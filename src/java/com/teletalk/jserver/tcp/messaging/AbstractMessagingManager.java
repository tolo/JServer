/*
 * Copyright 2007 the project originators.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.teletalk.jserver.tcp.messaging;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.event.EventQueue;
import com.teletalk.jserver.event.PropertyEvent;
import com.teletalk.jserver.event.PropertyEventListener;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.tcp.TcpCommunicationManager;
import com.teletalk.jserver.tcp.TcpEndPointGroup;
import com.teletalk.jserver.tcp.TcpEndPointIdentifier;
import com.teletalk.jserver.tcp.TcpEndPointIdentifierProperty;
import com.teletalk.jserver.tcp.messaging.rpc.MessagingRpcInterface;
import com.teletalk.jserver.util.EqualsUtils;

/**
 * Abstract base class for messaging implementations. The main purpose of this class is simply to reduce the size and 
 * complexity of MessagingManager by refactoring out some functionality, however it may also be used as base class 
 * for alternative messaging implementations.<br>
 * <br>
 * This class extends TcpCommunicationManager and provides extended support for client side connection.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20050516) 
 */
public abstract class AbstractMessagingManager extends TcpCommunicationManager implements PropertyEventListener
{
   /** 
    * The version of the messaging protocol.<br>
    * <br>
    * Version 4 - Major upgrade of protocol, using {@link com.teletalk.jserver.util.Streamable} headers.<br>
    * Version 5 - New header type: {@link MessageHeader#MESSAGE_PROCESSING_ERROR_HEADER}.<br>
    * Version 6 - New field in ConnectResponse - secondaryResponseSuccess.<br>
    */
   public static final byte MESSAGING_PROTOCOL_VERSION = 0x06;
   
   /** Meta data key for server name. The value of this field is <code>ServerName</code>. */
   public static final String SERVER_NAME_METADATA_KEY = "ServerName";

   /**
    * Meta data key for server version (not JServer version). The value of this field is <code>ServerVersion</code>.
    * 
    * @since 1.3.1, build 675.
    */
   public static final String SERVER_VERSION_METADATA_KEY = "ServerVersion";
   
   
   
   /** Client side {@link Destination} objects. */
   protected final ArrayList clientSideDestinations;
   
   /** The {@link MetaDataValueMatcher} used for matching meta data in remote destinations.  */
   private MetaDataValueMatcher metaDataMatcher;
   
   
   /**
    * TcpEndPointIdentifierProperty containing the remote destination addresses to which this MessagingManager should
    * connect.
    */
   protected final TcpEndPointIdentifierProperty remoteDestinationAddresses;

   /**
    * Flag indicating if client side destinations should be created automatically for requested message dispatch addresses 
    * (in method {@link #getDestinationForMessageDispatch(MessageHeader, TcpEndPointIdentifier)}. @since 2.1.2 (20060307)
    */
   protected boolean destinationAutoCreationEnabled = false;

   
   /**
    * Creates a new AbstractMessagingManager.
    */
   public AbstractMessagingManager(SubSystem parent, String name)
   {
      this(parent, name, null, null);
   }
   
   /**
    * Creates a new AbstractMessagingManager.
    */
   public AbstractMessagingManager(SubSystem parent, String name, TcpEndPointIdentifier[] clientAddresses, TcpEndPointIdentifier[] localAddress)
   {
      super(parent, name, MessagingEndPoint.class, localAddress);
      
      this.clientSideDestinations = new ArrayList();
      this.metaDataMatcher = new ReceiverNameMetaDataValueMatcher();
     
      this.remoteDestinationAddresses = new TcpEndPointIdentifierProperty(this, "remoteAddresses", clientAddresses,
            Property.MODIFIABLE_NO_RESTART);
      this.remoteDestinationAddresses.setDescription("Addresses for the destinations that this " + name
            + " should attempt to create connections to.");
      addProperty(this.remoteDestinationAddresses);
      
      super.endpointGroups.ownerShipAssumed(this, "destinations");
   }
   
   /**
    * Initialization method for this AbstractMessagingManager.
    */
   protected void doInitialize()
   {
      super.doInitialize();
      
      JServer jServer = JServer.getJServer();

      // Add default meta data
      if (jServer != null)
      {
         this.setMetaData(SERVER_NAME_METADATA_KEY, jServer.getName());
         this.setMetaData(SERVER_VERSION_METADATA_KEY, jServer.getServerVersion());
         
         EventQueue eventQueue = jServer.getEventQueue();
         if( eventQueue != null )eventQueue.registerPropertyEventListener(this);
      }
      
      // Attempt to get old property "remote addresses"
      super.initFromConfiguredProperty(this.remoteDestinationAddresses, "remote addresses", false, true);
      
      if( !super.isReinitializing() )
      {
         this.updateRemoteDestinations();
      }
   }
   
   /**
    * Shuts down this AbstractMessagingManager. 
    */
   protected void doShutDown()
   {
      super.doShutDown();
      
      JServer jServer = JServer.getJServer();

      if (jServer != null)
      {
         EventQueue eventQueue = jServer.getEventQueue();
         if( eventQueue != null )eventQueue.unregisterPropertyEventListener(this);
      }
   }   
   
   /**
    * Called when a property owne by this AbstractMessagingManager is modified.
    * 
    * @param property the property that was modified.
    */
   public void propertyModified(final Property property)
   {
      if (property == this.remoteDestinationAddresses)
      {
         // Reset destinations (but only if enabled)
         if( this.isEnabled() ) this.updateRemoteDestinations();
      }
      
      super.propertyModified(property);
   }
   
   /**
    * Called to check if a property can be modified. This method makes it possible to modify
    * remoteDestinationAddresses regardless of the state of this system.
    * 
    * @param property The property to be checked.
    * 
    * @return true if the property can be modified, otherwise false.
    */
   public boolean propertyModificationAllowed(Property property)
   {
      if (property == remoteDestinationAddresses) return true;
      else return super.propertyModificationAllowed(property);
   }
   
   /**
    * Notification of a property modificaion.
    * 
    * @param e a PropertyEvent object.
    */
   public void propertyModified(final PropertyEvent e)
   {
      JServer jServer = JServer.getJServer();
      
      if( (jServer != null) && (e.getSource() == jServer) )
      {
         String newServerName = jServer.getName();
         String newServerVersion = jServer.getServerVersion();
         String oldServerName = (String)this.getMetaData(SERVER_NAME_METADATA_KEY); 
         String oldServerVersion = (String)this.getMetaData(SERVER_VERSION_METADATA_KEY);
         
         if( !EqualsUtils.equals(newServerName, oldServerName) ) this.setMetaData(SERVER_NAME_METADATA_KEY, newServerName);
         if( !EqualsUtils.equals(newServerVersion, oldServerVersion) ) this.setMetaData(SERVER_VERSION_METADATA_KEY, newServerVersion);
      }
   }
   
   
   /* ### MISC GETTERS SETTERS BEGIN ### */
   
   
   /**
    * Gets the timeout value in milliseconds for reading of the body of a received message.
    */
   public abstract long getMessageReadTimeout();
   
   /**
    * Gets the default timeout used when waiting on a reponse for a specific message to be received (used in the
    * <code>dispatchXXXMessage</code> methods).
    * 
    * @return the current timeout.
    */
   public abstract long getResponseMessageTimeOut();
   
   /**
    * Gets the default timeout used when dispatching asynchronous messages (used in the
    * <code>dispatchXXXMessageAsynch</code> methods).
    * 
    * @return the current timeout.
    */
   public abstract long getAsynchMessageDispatchTimeOut();
   
   /**
    * Gets the current value of the setting for number of (client) connections that are to be created for each remote
    * messaging system.
    * 
    * @return the current value of the setting for number of connections that are to be created for each BLiP.
    */
   public abstract int getConnectionsPerDestination();
   
   /**
    * Stores a meta data value associated with the specified key. Meta data will be shared will all associated messaging
    * systems, in which it can be accessed through the Destination object (through the method
    * {@link Destination#getDestinationMetaData(String)}representing this messaging system. <br>
    * <br>
    * The specified value must be serializable, or a RuntimeException will be thrown.
    * 
    * @param key the key of the meta data.
    * @param value the meta data value. Must be serializable.
    */
   public abstract void setMetaData(final String key, final Object value);

   /**
    * Gets the meta data value with the specified key.
    * 
    * @param key a meta data key.
    * 
    * @return the meta data value matching the specified key, or <code>null</code> if none was found.
    */
   public abstract Object getMetaData(final String key);

   /**
    * Gets all the meta data contained in this MessagingManager.
    * 
    * @return all the meta data contained in this MessagingManager.
    */
   public abstract HashMap getMetaData();
   
   /**
    * Called to get the meta data that will be shared with the specified destination.
    * 
    * @return the meta data that will be shared with the specified destination. 
    * 
    * @since 2.1.2 (20060206)
    */
   public HashMap getMetaData(final Destination destination)
   {
      HashMap destinationMetaData = new HashMap();
      
      // Add global meta data
      HashMap metaData = this.getMetaData();
      if( metaData != null ) destinationMetaData.putAll(metaData);
       
      // Add contextual meta data
      HashMap contextualMetaData = this.getContextualMetaData(destination);
      if( contextualMetaData != null ) destinationMetaData.putAll(contextualMetaData);
         
      return destinationMetaData;
   }
   
   /**
    * Called to get any contextual meta data for the specified destination. Subclasses may override this method to provide 
    * additional meta data will that only be shared with the specified destination. Mappings in the returned HashMap will override 
    * mappings in the global meta data ({@link #getMetaData()}).<br>
    * <br>
    * This implementation returns null.
    * 
    * @return the additional meta data that will be shared with the specified destination. 
    * 
    * @since 2.1.2 (20060206)
    */
   public HashMap getContextualMetaData(final Destination destination)
   {
      return null;
   }
   
   /**
    * Returns the metaDataMatcher.
    * 
    * @return MetaDataValueMatcher
    */
   public MetaDataValueMatcher getMetaDataMatcher()
   {
      return metaDataMatcher;
   }

   /**
    * Sets the metaDataMatcher.
    * 
    * @param metaDataMatcher The metaDataMatcher to set
    */
   public void setMetaDataMatcher(MetaDataValueMatcher metaDataMatcher)
   {
      if (metaDataMatcher == null) return;
      this.metaDataMatcher = metaDataMatcher;
   }
      
   
   /* ### MISC GETTERS SETTERS END ### */
   
   
   /* ### MESSAGE DISPATCHER/RPC INTERFACE METHODS BEGIN ### */
   
   
   /**
    * Factory method for creating a new MessagingRpcInterface. Subclasses may override this method to create custom
    * implementations instead of having to override each of the getMessagingRpcInterface methods.
    * 
    * @param messageDispatcher the MessageDispatcher to be associated with the MessagingRpcInterface.
    * 
    * @since 2.0
    */
   protected abstract MessagingRpcInterface createMessagingRpcInterface(final MessageDispatcher messageDispatcher);

   /**
    * Gets an interface for RPC-based communication with a remote messaging system.
    * 
    * @param messageDispatcherProperties the properties to set for the message dispatcher.
    * 
    * @since 2.0
    */
   public MessagingRpcInterface getMessagingRpcInterface(final MessageDispatcherProperties messageDispatcherProperties)
   {
      return this.createMessagingRpcInterface(this.getMessageDispatcher(messageDispatcherProperties));
   }

   /**
    * Gets an interface for RPC-based communication with multiple remote messaging systems.
    * 
    * @param desinations remote messaging systems to create MessagingRpcInterface objects for.
    * 
    * @since 2.0
    */
   public MessagingRpcInterface[] getMessagingRpcInterfaces(final Destination[] desinations)
   {
      return this.getMessagingRpcInterfaces(desinations, null);
   }

   /**
    * Gets an interface for RPC-based communication with multiple remote messaging systems.
    * 
    * @param desinations remote messaging systems to create MessagingRpcInterface objects for.
    * @param messageDispatcherPropertiesTemplate a template MessageDispatcherProperties object.
    * 
    * @since 2.0
    */
   public MessagingRpcInterface[] getMessagingRpcInterfaces(final Destination[] desinations,
         final MessageDispatcherProperties messageDispatcherPropertiesTemplate)
   {
      MessagingRpcInterface[] messagingRpcInterfaces = new MessagingRpcInterface[desinations.length];
      MessageDispatcherProperties messageDispatcherProperties;

      for (int i = 0; i < messagingRpcInterfaces.length; i++)
      {
         messageDispatcherProperties = new MessageDispatcherProperties(messageDispatcherPropertiesTemplate);
         messageDispatcherProperties.setDestination(desinations[i]);
         messagingRpcInterfaces[i] = this.createMessagingRpcInterface(this.getMessageDispatcher(messageDispatcherProperties));
      }

      return messagingRpcInterfaces;
   }

   /**
    * Gets an interface for RPC-based communication with a (any) remote messaging system that has the specified named
    * receiver.
    * 
    * @param namedReceiver the name of a named receiver in a remote destination.
    * 
    * @since 2.0
    */
   public MessagingRpcInterface getMessagingRpcInterface(final String namedReceiver)
   {
      return this.createMessagingRpcInterface(this.getMessageDispatcher(new MessageDispatcherProperties(namedReceiver)));
   }

   /**
    * Gets an interface for RPC-based communication with a specific remote messaging system.
    * 
    * @param destination a remote messaging system.
    * 
    * @since 2.0, Build 757
    */
   public MessagingRpcInterface getMessagingRpcInterface(final Destination destination)
   {
      return this.createMessagingRpcInterface(this.getMessageDispatcher(new MessageDispatcherProperties(destination)));
   }

   /**
    * Gets an interface for RPC-based communication with a specific remote messaging system that has the specified named
    * receiver.
    * 
    * @param destination a remote messaging system.
    * @param namedReceiver the name of a named receiver in a remote destination.
    * 
    * @since 2.0, Build 757
    */
   public MessagingRpcInterface getMessagingRpcInterface(final Destination destination, final String namedReceiver)
   {
      return this.createMessagingRpcInterface(this.getMessageDispatcher(new MessageDispatcherProperties(destination,
            namedReceiver)));
   }

   /**
    * Gets interfaces for RPC-based communication with multiple remote messaging systems that all have the specified
    * named receiver.
    * 
    * @param namedReceiver the name of a named receiver in a remote destination.
    * 
    * @since 2.0
    */
   public MessagingRpcInterface[] getMessagingRpcInterfaces(final String namedReceiver)
   {
      return this.getMessagingRpcInterfaces(this.getDestinations(namedReceiver), new MessageDispatcherProperties(
            namedReceiver));
   }

   /**
    * Factory method for creating a new MessageDispatcher. Subclasses may override this method to create custom
    * implementations instead of having to override each of the getMessagingRpcInterfaces methods.
    * 
    * @param messageDispatcherProperties the properties to set for the message dispatcher.
    * 
    * @since 2.0
    */
   protected abstract MessageDispatcher createMessageDispatcher(final MessageDispatcherProperties messageDispatcherProperties);

   /**
    * Creates a {@link MessageDispatcher}interface to be used for dispatching messages. MessageDispatcher provides an
    * alternative, and possibly easier, approach to dispatching messages through a MessagingManager.
    */
   public MessageDispatcher getMessageDispatcher(final MessageDispatcherProperties messageDispatcherProperties)
   {
      return this.createMessageDispatcher(messageDispatcherProperties);
   }

   /**
    * Creates an array of {@link MessageDispatcher}interface to be used for dispatching messages to the specified
    * destinations. MessageDispatcher provides an alternative, and possibly easier, approach to dispatching messages
    * through a MessagingManager.
    * 
    * @param desinations remote messaging systems to create MessageDispatcher objects for.
    * 
    * @since 2.0
    */
   public MessageDispatcher[] getMessageDispatchers(final Destination[] desinations)
   {
      return this.getMessageDispatchers(desinations, null);
   }

   /**
    * Creates an array of {@link MessageDispatcher}interface to be used for dispatching messages to the specified
    * destinations. MessageDispatcher provides an alternative, and possibly easier, approach to dispatching messages
    * through a MessagingManager.
    * 
    * @param desinations remote messaging systems to create MessageDispatcher objects for.
    * @param messageDispatcherPropertiesTemplate a template MessageDispatcherProperties object.
    * 
    * @since 2.0
    */
   public MessageDispatcher[] getMessageDispatchers(final Destination[] desinations,
         final MessageDispatcherProperties messageDispatcherPropertiesTemplate)
   {
      MessageDispatcher[] messageDispatchers = new MessageDispatcher[desinations.length];
      MessageDispatcherProperties messageDispatcherProperties;

      for (int i = 0; i < messageDispatchers.length; i++)
      {
         messageDispatcherProperties = new MessageDispatcherProperties(messageDispatcherPropertiesTemplate);
         messageDispatcherProperties.setDestination(desinations[i]);
         messageDispatchers[i] = this.createMessageDispatcher(messageDispatcherProperties);
      }

      return messageDispatchers;
   }

   /**
    * Creates a {@link MessageDispatcher}interface to be used for dispatching messages to a named receiver in a single
    * remote messaging system. MessageDispatcher provides an alternative, and possibly easier, approach to dispatching
    * messages through a MessagingManager.
    * 
    * @param namedReceiver the name of a named receiver in a remote destination.
    * 
    * @since 2.0
    */
   public MessageDispatcher getMessageDispatcher(final String namedReceiver)
   {
      return this.getMessageDispatcher(new MessageDispatcherProperties(namedReceiver));
   }

   /**
    * Creates a {@link MessageDispatcher}interface to be used for dispatching messages to a specific remote messaging
    * system. MessageDispatcher provides an alternative, and possibly easier, approach to dispatching messages through a
    * MessagingManager.
    * 
    * @param destination a remote messaging system.
    * 
    * @since 2.0, Build 757
    */
   public MessageDispatcher getMessageDispatcher(final Destination destination)
   {
      return this.getMessageDispatcher(new MessageDispatcherProperties(destination));
   }

   /**
    * Creates a {@link MessageDispatcher}interface to be used for dispatching messages to a named receiver in a
    * specific remote messaging system. MessageDispatcher provides an alternative, and possibly easier, approach to
    * dispatching messages through a MessagingManager.
    * 
    * @param destination a remote messaging system.
    * @param namedReceiver the name of a named receiver in a remote destination.
    * 
    * @since 2.0, Build 757
    */
   public MessageDispatcher getMessageDispatcher(final Destination destination, final String namedReceiver)
   {
      return this.getMessageDispatcher(new MessageDispatcherProperties(destination, namedReceiver));
   }

   /**
    * Creates an array of {@link MessageDispatcher}interface to be used for dispatching messages to named receivers in
    * multiple destinations. MessageDispatcher provides an alternative, and possibly easier, approach to dispatching
    * messages through a MessagingManager.
    * 
    * @param namedReceiver the name of a named receiver in a remote destination.
    * 
    * @since 2.0
    */
   public MessageDispatcher[] getMessageDispatchers(final String namedReceiver)
   {
      return this.getMessageDispatchers(this.getDestinations(namedReceiver), new MessageDispatcherProperties(
            namedReceiver));
   }
   
   
   /* ### MESSAGE DISPATCHER/RPC INTERFACE METHODS END ### */
   
   
   /* ### MESSAGE DISPATCH METHODS BEGIN ### */
   
   
   /**
    * Gets a Destination object for the specified address, or creates one if none exists and the flag {@link #destinationAutoCreationEnabled} is true.
    */
   protected Destination getDestinationForMessageDispatch(final MessageHeader header, final TcpEndPointIdentifier destinationAddress) 
      throws MessageDispatchFailedException
   {
      Destination destination = this.getDestination(destinationAddress);

      if (destination == null)
      {
         if( this.destinationAutoCreationEnabled )
         {
            // Create a new destination for the specified address
            this.addDestination(destinationAddress);
            destination = this.getDestination(destinationAddress);
   
            if (destination == null) // This should never happen...
            {
               logError("Unable to create destination for address " + destinationAddress
                     + " when attempting to dispatch object message with header " + header + "!");
               throw new MessageDispatchFailedException("Unable to create destination for address " + destinationAddress
                     + " when attempting to dispatch object message with header " + header + "!");
            }
         }
         else throw new MessageDispatchFailedException("Unable to find destination for address " + destinationAddress
               + " when attempting to dispatch object message with header " + header + "!");
      }

      return destination;
   }
      
   /**
    * Internal method for performing the actual dispatching of a message. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageDispatchImpl the implementation to be used for dispatching the message body.
    * @param messageDispatcherProperties a {@link MessageDispatcherProperties} object containing properties for message dispatch.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   protected Message dispatchMessage(final MessageHeader header,
         final MessageWriter messageDispatchImpl,
         final MessageDispatcherProperties messageDispatcherProperties) throws MessageDispatchFailedException,
         ResponseTimeOutException
   {
      return this.dispatchMessage(header, messageDispatchImpl, messageDispatcherProperties, false);
   }
   
   /**
    * Internal method for performing the actual dispatching of a message. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageDispatchImpl the implementation to be used for dispatching the message body.
    * @param destination the destination to dispatch the message to, or <code>null</code> if the message is to be dispatched to any destination.
    * @param timeOut the timeout to be used when waiting for a response and the maximum time to wait for an available endpoint.
    * @param asynch boolean flag indicating if the message is asynchronous (<code>true</code>) or not (<code>false</code>).
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   protected Message dispatchMessage(MessageHeader header,
         final MessageWriter messageDispatchImpl, Destination destination,
         String namedReceiver, final Map destinationMetaDataConstraints, long timeOut, final boolean asynch)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      MessageDispatcherProperties messageDispatcherProperties = new MessageDispatcherProperties(destination, namedReceiver, timeOut, asynch);
      messageDispatcherProperties.setDestinationMetaDataConstraints(destinationMetaDataConstraints);
      
      return dispatchMessage(header, messageDispatchImpl, messageDispatcherProperties, false);
   }
   
   /**
    * Internal method for performing the actual dispatching of a message. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageDispatchImpl the implementation to be used for dispatching the message body.
    * @param messageDispatcherProperties a {@link MessageDispatcherProperties} object containing properties for message dispatch.
    * @param proxyMessage flag indicating if the message to be dispatched is a proxied message, which means that this method should never wait for a 
    * response even if the message isn't asych.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    * 
    * @since 2.0.3 (20050413)
    */
   protected abstract Message dispatchMessage(MessageHeader header, MessageWriter messageDispatchImpl, 
         MessageDispatcherProperties messageDispatcherProperties, boolean proxyMessage)
         throws MessageDispatchFailedException, ResponseTimeOutException;
      
   
   
   // ----------------------
   // NULL/HEADER-ONLY MESSAGE
   // ----------------------
   
   /**
    * Sends a synchronous a message without body to any destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * 
    * @return the response message.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    * 
    * @since 2.0 Build 762 
    */
   public Message dispatchMessage(final MessageHeader header)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ByteArrayMessageWriter(null), null, null, 
            null, this.getResponseMessageTimeOut(), false);
   }
   
   /**
    * Sends an asynchronous a message without body to any destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message, by using the message header of the incomming 
    * message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param header the header of the message to be dispatched.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * 
    * @since 2.0 Build 762
    */
   public void dispatchMessageAsync(final MessageHeader header)
         throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new ByteArrayMessageWriter(null), null, null,
            null, this.getAsynchMessageDispatchTimeOut(), true);
   }

   // ----------------------
   // OBJECT MESSAGES
   // ----------------------

   /**
    * Sends a synchronous object message to any destination. <br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the object body of the message. Must be serializable.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final Object messageBody)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      /*if (!(messageBody instanceof Serializable))
            throw new MessageDispatchFailedException("Body object must be serializable!");*/

      return this.dispatchMessage(header, new ObjectMessageWriter(messageBody), null,
            null, null, this.getResponseMessageTimeOut(), false);
   }

   /**
    * Sends a synchronous object message to any destination. <br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the object body of the message. Must be serializable.
    * @param timeOut the timeout to be used when waiting for a response or and endpoint to become available.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final Object messageBody, final long timeOut)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ObjectMessageWriter(messageBody), null,
            null, null, ((timeOut > 0) ? timeOut : this.getResponseMessageTimeOut()), false);
   }

   /**
    * Sends an asynchronous object message to any destination. <br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message, by using the message header of the incomming 
    * message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the object body of the message. Must be serializable.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    */
   public void dispatchMessageAsync(final MessageHeader header, final Object messageBody)
         throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new ObjectMessageWriter(messageBody), null, null,
            null, this.getAsynchMessageDispatchTimeOut(), true);
   }
   
   /**
    * Sends a synchronous object message to a specific destination. <br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the object body of the message. Must be serializable.
    * @param destination the destination to dispatch the message to.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    * 
    * @since 2.1.2 (20060307)
    */
   public Message dispatchMessage(final MessageHeader header, final Object messageBody,
         final Destination destination) throws MessageDispatchFailedException,
         ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ObjectMessageWriter(messageBody),
            destination, null, null,
            this.getResponseMessageTimeOut(), false);
   }

   /**
    * Sends a synchronous object message to a specific destination. <br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the object body of the message. Must be serializable.
    * @param destinationAddress the address to dispatch the message to.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final Object messageBody,
         final TcpEndPointIdentifier destinationAddress) throws MessageDispatchFailedException,
         ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ObjectMessageWriter(messageBody),
            this.getDestinationForMessageDispatch(header, destinationAddress), null, null,
            this.getResponseMessageTimeOut(), false);
   }
   
   /**
    * Sends a synchronous object message to a specific destination. <br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the object body of the message. Must be serializable.
    * @param destination the destination to dispatch the message to.
    * @param timeOut the timeout to be used when waiting for a response or and endpoint to become available.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    * 
    * @since 2.1.2 (20060307)
    */
   public Message dispatchMessage(final MessageHeader header, final Object messageBody,
         final Destination destination, final long timeOut) throws MessageDispatchFailedException,
         ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ObjectMessageWriter(messageBody),
            destination, null, null, ((timeOut > 0) ? timeOut
                  : this.getResponseMessageTimeOut()), false);
   }
   
   /**
    * Sends a synchronous object message to a specific destination. <br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the object body of the message. Must be serializable.
    * @param destinationAddress the address to dispatch the message to.
    * @param timeOut the timeout to be used when waiting for a response or and endpoint to become available.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final Object messageBody,
         final TcpEndPointIdentifier destinationAddress, final long timeOut) throws MessageDispatchFailedException,
         ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ObjectMessageWriter(messageBody),
            this.getDestinationForMessageDispatch(header, destinationAddress), null, null, ((timeOut > 0) ? timeOut
                  : this.getResponseMessageTimeOut()), false);
   }
   
   /**
    * Sends an asynchronous object message to a specific destination. <br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the object body of the message. Must be serializable.
    * @param destination the destination to dispatch the message to.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    *                    
    * @since 2.1.2 (20060307)
    */
   public void dispatchMessageAsync(final MessageHeader header, final Object messageBody,
         final Destination destination) throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new ObjectMessageWriter(messageBody),
            destination, null, null,
            this.getAsynchMessageDispatchTimeOut(), true);
   }

   /**
    * Sends an asynchronous object message to a specific destination. <br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the object body of the message. Must be serializable.
    * @param destinationAddress the address to dispatch the message to.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    */
   public void dispatchMessageAsync(final MessageHeader header, final Object messageBody,
         final TcpEndPointIdentifier destinationAddress) throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new ObjectMessageWriter(messageBody),
            this.getDestinationForMessageDispatch(header, destinationAddress), null, null,
            this.getAsynchMessageDispatchTimeOut(), true);
   }

   /**
    * Sends a synchronous object message to a destination with the specified meta data constraints. <br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the object body of the message. Must be serializable.
    * @param destinationMetaDataConstraints a java.util.Map object containing mappings for matching against the meta
    *                   data of a destination object.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final Object messageBody,
         final Map destinationMetaDataConstraints) throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ObjectMessageWriter(messageBody), null,
            null, destinationMetaDataConstraints, this.getResponseMessageTimeOut(), false);
   }

   /**
    * Sends a synchronous object message to a destination with the specified meta data constraints. <br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the object body of the message. Must be serializable.
    * @param destinationMetaDataConstraints a java.util.Map object containing mappings for matching against the meta
    *                   data of a destination object.
    * @param timeOut the timeout to be used when waiting for a response or and endpoint to become available.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final Object messageBody,
         final Map destinationMetaDataConstraints, final long timeOut) throws MessageDispatchFailedException,
         ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ObjectMessageWriter(messageBody), null,
            null, destinationMetaDataConstraints, ((timeOut > 0) ? timeOut : this.getResponseMessageTimeOut()), false);
   }

   /**
    * Sends an asynchronous object message to a destination with the specified meta data constraints. <br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the object body of the message. Must be serializable.
    * @param destinationMetaDataConstraints a java.util.Map object containing mappings for matching against the meta
    *                   data of a destination object.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    */
   public void dispatchMessageAsync(final MessageHeader header, final Object messageBody,
         final Map destinationMetaDataConstraints) throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new ObjectMessageWriter(messageBody), null, null,
            destinationMetaDataConstraints, this.getAsynchMessageDispatchTimeOut(), true);
   }

   /**
    * Method to dispatch a synchronous object message to a destination with the specified named receiver. Calling this
    * method has the same effect as calling the method {@link #dispatchMessage(MessageHeader, Object, Map, long)}, with
    * a map containing parameter <code>namedReceiver</code> mapped to the key
    * {@link MessagingManager#NAMED_MESSAGE_RECEIVER_METADATA_KEY}.<br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the object body of the message. Must be serializable.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final Object messageBody, final String namedReceiver)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ObjectMessageWriter(messageBody), null,
            namedReceiver, null, this.getResponseMessageTimeOut(), false);
   }

   /**
    * Method to dispatch a synchronous object message to a destination with the specified named receiver. Calling this
    * method has the same effect as calling the method {@link #dispatchMessage(MessageHeader, Object, Map, long)}, with
    * a map containing parameter <code>namedReceiver</code> mapped to the key
    * {@link MessagingManager#NAMED_MESSAGE_RECEIVER_METADATA_KEY}.<br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the object body of the message. Must be serializable.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * @param timeOut the timeout to be used when waiting for a response or and endpoint to become available.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final Object messageBody, final String namedReceiver,
         final long timeOut) throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ObjectMessageWriter(messageBody), null,
            namedReceiver, null, ((timeOut > 0) ? timeOut : this.getResponseMessageTimeOut()), false);
   }

   /**
    * Method to dispatch a synchronous object message to a destination with the specified named receiver. Calling this
    * method has the same effect as calling the method {@link #dispatchMessage(MessageHeader, Object, Map)}, with a map
    * containing parameter <code>namedReceiver</code> mapped to the key
    * {@link MessagingManager#NAMED_MESSAGE_RECEIVER_METADATA_KEY}.<br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the object body of the message. Must be serializable.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    */
   public void dispatchMessageAsync(final MessageHeader header, final Object messageBody, final String namedReceiver)
         throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new ObjectMessageWriter(messageBody), null,
            namedReceiver, null, this.getAsynchMessageDispatchTimeOut(), true);
   }

   /**
    * Sends a synchronous object message to a specific destination. <br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the object body of the message. Must be serializable.
    * @param destination the destination to dispatch the message to.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    * 
    * @since 2.1.2 (20060307)
    */
   public Message dispatchMessage(final MessageHeader header, final Object messageBody,
         final Destination destination, final String namedReceiver)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ObjectMessageWriter(messageBody),
            destination, namedReceiver, null,
            this.getResponseMessageTimeOut(), false);
   }
   
   /**
    * Sends a synchronous object message to a specific destination. <br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the object body of the message. Must be serializable.
    * @param destinationAddress the address to dispatch the message to.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final Object messageBody,
         final TcpEndPointIdentifier destinationAddress, final String namedReceiver)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ObjectMessageWriter(messageBody),
            this.getDestinationForMessageDispatch(header, destinationAddress), namedReceiver, null,
            this.getResponseMessageTimeOut(), false);
   }
   
   /**
    * Sends a synchronous object message to a specific destination. <br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the object body of the message. Must be serializable.
    * @param destination the destination to dispatch the message to.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * @param timeOut the timeout to be used when waiting for a response or and endpoint to become available.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    * 
    * @since 2.1.2 (20060307)
    */
   public Message dispatchMessage(final MessageHeader header, final Object messageBody,
         final Destination destination, final String namedReceiver, final long timeOut)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ObjectMessageWriter(messageBody),
            destination, namedReceiver, null,
            ((timeOut > 0) ? timeOut : this.getResponseMessageTimeOut()), false);
   }

   /**
    * Sends a synchronous object message to a specific destination. <br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the object body of the message. Must be serializable.
    * @param destinationAddress the address to dispatch the message to.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * @param timeOut the timeout to be used when waiting for a response or and endpoint to become available.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final Object messageBody,
         final TcpEndPointIdentifier destinationAddress, final String namedReceiver, final long timeOut)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ObjectMessageWriter(messageBody),
            this.getDestinationForMessageDispatch(header, destinationAddress), namedReceiver, null,
            ((timeOut > 0) ? timeOut : this.getResponseMessageTimeOut()), false);
   }
   
   /**
    * Sends an asynchronous object message to a specific destination. <br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message, by using the message header of the incomming 
    * message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the object body of the message. Must be serializable.
    * @param destination the destination to dispatch the message to.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    *                    
    * @since 2.1.2 (20060307)
    */
   public void dispatchMessageAsync(final MessageHeader header, final Object messageBody,
         final Destination destination, final String namedReceiver)
         throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new ObjectMessageWriter(messageBody),
            destination, namedReceiver, null,
            this.getAsynchMessageDispatchTimeOut(), true);
   }

   /**
    * Sends an asynchronous object message to a specific destination. <br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message, by using the message header of the incomming 
    * message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the object body of the message. Must be serializable.
    * @param destinationAddress the address to dispatch the message to.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    */
   public void dispatchMessageAsync(final MessageHeader header, final Object messageBody,
         final TcpEndPointIdentifier destinationAddress, final String namedReceiver)
         throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new ObjectMessageWriter(messageBody),
            this.getDestinationForMessageDispatch(header, destinationAddress), namedReceiver, null,
            this.getAsynchMessageDispatchTimeOut(), true);
   }

   // ----------------------
   // BYTE ARRAY MESSAGES
   // ----------------------

   /**
    * Sends a synchronous byte array message to any destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the byte array body of the message.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final byte[] messageBody)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ByteArrayMessageWriter(messageBody),
            null, null, null, this.getResponseMessageTimeOut(), false);
   }

   /**
    * Sends a synchronous byte array message to any destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the byte array body of the message.
    * @param timeOut the timeout to be used when waiting for a response or and endpoint to become available.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final byte[] messageBody, final long timeOut)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ByteArrayMessageWriter(messageBody),
            null, null, null, ((timeOut > 0) ? timeOut : this.getResponseMessageTimeOut()), false);
   }

   /**
    * Sends an asynchronous byte array message to any destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message, by using the message header of the incomming 
    * message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the byte array body of the message.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    */
   public void dispatchMessageAsync(final MessageHeader header, final byte[] messageBody)
         throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new ByteArrayMessageWriter(messageBody), null, null,
            null, this.getAsynchMessageDispatchTimeOut(), true);
   }
   
   /**
    * Sends a synchronous byte array message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the byte array body of the message.
    * @param destination the destination to dispatch the message to.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    * 
    * @since 2.1.2 (20060307)
    */
   public Message dispatchMessage(final MessageHeader header, final byte[] messageBody,
         final Destination destination) throws MessageDispatchFailedException,
         ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ByteArrayMessageWriter(messageBody),
            destination, null, null,
            this.getResponseMessageTimeOut(), false);
   }

   /**
    * Sends a synchronous byte array message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the byte array body of the message.
    * @param destinationAddress the address to dispatch the message to.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final byte[] messageBody,
         final TcpEndPointIdentifier destinationAddress) throws MessageDispatchFailedException,
         ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ByteArrayMessageWriter(messageBody),
            this.getDestinationForMessageDispatch(header, destinationAddress), null, null,
            this.getResponseMessageTimeOut(), false);
   }
   
   /**
    * Sends a synchronous byte array message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the byte array body of the message.
    * @param destination the destination to dispatch the message to.
    * @param timeOut the timeout to be used when waiting for a response or and endpoint to become available.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    * 
    * @since 2.1.2 (20060307)
    */
   public Message dispatchMessage(final MessageHeader header, final byte[] messageBody,
         final Destination destination, final long timeOut) throws MessageDispatchFailedException,
         ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ByteArrayMessageWriter(messageBody),
            destination, null, null, ((timeOut > 0) ? timeOut
                  : this.getResponseMessageTimeOut()), false);
   }

   /**
    * Sends a synchronous byte array message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the byte array body of the message.
    * @param destinationAddress the address to dispatch the message to.
    * @param timeOut the timeout to be used when waiting for a response or and endpoint to become available.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final byte[] messageBody,
         final TcpEndPointIdentifier destinationAddress, final long timeOut) throws MessageDispatchFailedException,
         ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ByteArrayMessageWriter(messageBody),
            this.getDestinationForMessageDispatch(header, destinationAddress), null, null, ((timeOut > 0) ? timeOut
                  : this.getResponseMessageTimeOut()), false);
   }
   
   /**
    * Sends a synchronous byte array message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message, by using the message header of the incomming 
    * message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the byte array body of the message.
    * @param destination the destination to dispatch the message to.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    *                    
    * @since 2.1.2 (20060307)
    */
   public void dispatchMessageAsync(final MessageHeader header, final byte[] messageBody,
         final Destination destination) throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new ByteArrayMessageWriter(messageBody),
            destination, null, null,
            this.getAsynchMessageDispatchTimeOut(), true);
   }

   /**
    * Sends a synchronous byte array message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message, by using the message header of the incomming 
    * message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the byte array body of the message.
    * @param destinationAddress the address to dispatch the message to.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    */
   public void dispatchMessageAsync(final MessageHeader header, final byte[] messageBody,
         final TcpEndPointIdentifier destinationAddress) throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new ByteArrayMessageWriter(messageBody),
            this.getDestinationForMessageDispatch(header, destinationAddress), null, null,
            this.getAsynchMessageDispatchTimeOut(), true);
   }

   /**
    * Sends a synchronous byte array message to a destination with the specified meta data constraints. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the byte array body of the message.
    * @param destinationMetaDataConstraints a java.util.Map object containing mappings for matching against the meta
    *                   data of a destination object.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final byte[] messageBody,
         final Map destinationMetaDataConstraints) throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ByteArrayMessageWriter(messageBody),
            null, null, destinationMetaDataConstraints, this.getResponseMessageTimeOut(), false);
   }

   /**
    * Sends a synchronous byte array message to a destination with the specified meta data constraints. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the byte array body of the message.
    * @param destinationMetaDataConstraints a java.util.Map object containing mappings for matching against the meta
    *                   data of a destination object.
    * @param timeOut the timeout to be used when waiting for a response or and endpoint to become available.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final byte[] messageBody,
         final Map destinationMetaDataConstraints, final long timeOut) throws MessageDispatchFailedException,
         ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ByteArrayMessageWriter(messageBody),
            null, null, destinationMetaDataConstraints, ((timeOut > 0) ? timeOut : this.getResponseMessageTimeOut()),
            false);
   }

   /**
    * Sends an asynchronous byte array message to a destination with the specified meta data constraints. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message, by using the message header of the incomming 
    * message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the byte array body of the message.
    * @param destinationMetaDataConstraints a java.util.Map object containing mappings for matching against the meta
    *                   data of a destination object.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    */
   public void dispatchMessageAsync(final MessageHeader header, final byte[] messageBody,
         final Map destinationMetaDataConstraints) throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new ByteArrayMessageWriter(messageBody), null, null,
            destinationMetaDataConstraints, this.getAsynchMessageDispatchTimeOut(), true);
   }

   /**
    * Method to dispatch an asynchronous byte array message to a destination with the specified named receiver. Calling
    * this method has the same effect as calling the method {@link #dispatchMessage(MessageHeader, Object, Map, long)},
    * with a map containing parameter <code>namedReceiver</code> mapped to the key
    * {@link MessagingManager#NAMED_MESSAGE_RECEIVER_METADATA_KEY}.<br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the byte array body of the message.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final byte[] messageBody, final String namedReceiver)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ByteArrayMessageWriter(messageBody),
            null, namedReceiver, null, this.getResponseMessageTimeOut(), false);
   }

   /**
    * Method to dispatch an asynchronous byte array message to a destination with the specified named receiver. Calling
    * this method has the same effect as calling the method {@link #dispatchMessage(MessageHeader, Object, Map, long)},
    * with a map containing parameter <code>namedReceiver</code> mapped to the key
    * {@link MessagingManager#NAMED_MESSAGE_RECEIVER_METADATA_KEY}.<br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the byte array body of the message.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * @param timeOut the timeout to be used when waiting for a response or and endpoint to become available.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final byte[] messageBody, final String namedReceiver,
         final long timeOut) throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ByteArrayMessageWriter(messageBody),
            null, namedReceiver, null, ((timeOut > 0) ? timeOut : this.getResponseMessageTimeOut()), false);
   }

   /**
    * Method to dispatch an asynchronous byte array message to a destination with the specified named receiver. Calling
    * this method has the same effect as calling the method {@link #dispatchMessage(MessageHeader, Object, Map, long)},
    * with a map containing parameter <code>namedReceiver</code> mapped to the key
    * {@link MessagingManager#NAMED_MESSAGE_RECEIVER_METADATA_KEY}.<br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message, by using the message header of the incomming 
    * message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the byte array body of the message.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    */
   public void dispatchMessageAsync(final MessageHeader header, final byte[] messageBody, final String namedReceiver)
         throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new ByteArrayMessageWriter(messageBody), null,
            namedReceiver, null, this.getAsynchMessageDispatchTimeOut(), true);
   }
   
   /**
    * Sends a synchronous byte array message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the byte array body of the message.
    * @param destination the destination to dispatch the message to.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    * 
    * @since 2.1.2 (20060307)
    */
   public Message dispatchMessage(final MessageHeader header, final byte[] messageBody,
         final Destination destination, final String namedReceiver)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ByteArrayMessageWriter(messageBody),
            destination, namedReceiver, null,
            this.getResponseMessageTimeOut(), false);
   }

   /**
    * Sends a synchronous byte array message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the byte array body of the message.
    * @param destinationAddress the address to dispatch the message to.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final byte[] messageBody,
         final TcpEndPointIdentifier destinationAddress, final String namedReceiver)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ByteArrayMessageWriter(messageBody),
            this.getDestinationForMessageDispatch(header, destinationAddress), namedReceiver, null,
            this.getResponseMessageTimeOut(), false);
   }
   
   /**
    * Sends a synchronous byte array message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the byte array body of the message.
    * @param destination the destination to dispatch the message to.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * @param timeOut the timeout to be used when waiting for a response or and endpoint to become available.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    * 
    * @since 2.1.2 (20060307)
    */
   public Message dispatchMessage(final MessageHeader header, final byte[] messageBody,
         final Destination destination, final String namedReceiver, final long timeOut)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ByteArrayMessageWriter(messageBody),
            destination, namedReceiver, null,
            ((timeOut > 0) ? timeOut : this.getResponseMessageTimeOut()), false);
   }

   /**
    * Sends a synchronous byte array message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the byte array body of the message.
    * @param destinationAddress the address to dispatch the message to.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * @param timeOut the timeout to be used when waiting for a response or and endpoint to become available.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final byte[] messageBody,
         final TcpEndPointIdentifier destinationAddress, final String namedReceiver, final long timeOut)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new ByteArrayMessageWriter(messageBody),
            this.getDestinationForMessageDispatch(header, destinationAddress), namedReceiver, null,
            ((timeOut > 0) ? timeOut : this.getResponseMessageTimeOut()), false);
   }
   
   /**
    * Sends a synchronous byte array message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message, by using the message header of the incomming 
    * message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the byte array body of the message.
    * @param destination the destination to dispatch the message to.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * 
    * @since 2.1.2 (20060307)
    */
   public void dispatchMessageAsync(final MessageHeader header, final byte[] messageBody,
         final Destination destination, final String namedReceiver)
         throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new ByteArrayMessageWriter(messageBody),
            destination, namedReceiver, null,
            this.getAsynchMessageDispatchTimeOut(), true);
   }

   /**
    * Sends a synchronous byte array message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message, by using the message header of the incomming 
    * message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBody the byte array body of the message.
    * @param destinationAddress the address to dispatch the message to.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    */
   public void dispatchMessageAsync(final MessageHeader header, final byte[] messageBody,
         final TcpEndPointIdentifier destinationAddress, final String namedReceiver)
         throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new ByteArrayMessageWriter(messageBody),
            this.getDestinationForMessageDispatch(header, destinationAddress), namedReceiver, null,
            this.getAsynchMessageDispatchTimeOut(), true);
   }

   // ----------------------
   // INPUTSTREAM MESSAGES
   // ----------------------

   /**
    * Sends a synchronous inputstream message to any destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id. <br>
    * <br>
    * <b><i>Note </i> </b>: This method will not close the stream when the message has been dispatched - this is the
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as
    * it is guaranteed that the message has been fully dispatched by then.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBodyReader the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final InputStream messageBodyReader, final long bodyLength)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new InputStreamMessageWriter(
            messageBodyReader, bodyLength), null, null, null, this.getResponseMessageTimeOut(), false);
   }

   /**
    * Sends a synchronous inputstream message to any destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id. <br>
    * <br>
    * <b><i>Note </i> </b>: This method will not close the stream when the message has been dispatched - this is the
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as
    * it is guaranteed that the message has been fully dispatched by then.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBodyReader the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * @param timeOut the timeout to be used when waiting for a response or and endpoint to become available.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final InputStream messageBodyReader,
         final long bodyLength, final long timeOut) throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new InputStreamMessageWriter(
            messageBodyReader, bodyLength), null, null, null, ((timeOut > 0) ? timeOut
            : this.getResponseMessageTimeOut()), false);
   }

   /**
    * Sends an asynchronous inputstream message to any destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id. <br>
    * <br>
    * <b><i>Note </i> </b>: This method will not close the stream when the message has been dispatched - this is the
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as
    * it is guaranteed that the message has been fully dispatched by then.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message, by using the message header of the incomming 
    * message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBodyReader the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    */
   public void dispatchMessageAsync(final MessageHeader header, final InputStream messageBodyReader,
         final long bodyLength) throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new InputStreamMessageWriter(messageBodyReader,
            bodyLength), null, null, null, this.getAsynchMessageDispatchTimeOut(), true);
   }
   
   /**
    * Sends a synchronous inputstream message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id. <br>
    * <br>
    * <b><i>Note </i> </b>: This method will not close the stream when the message has been dispatched - this is the
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as
    * it is guaranteed that the message has been fully dispatched by then.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBodyReader the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * @param destination the destination to dispatch the message to.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    * 
    * @since 2.1.2 (20060307)
    */
   public Message dispatchMessage(final MessageHeader header, final InputStream messageBodyReader,
         final long bodyLength, final Destination destination) throws MessageDispatchFailedException,
         ResponseTimeOutException
   {
      return this.dispatchMessage(header, new InputStreamMessageWriter(
            messageBodyReader, bodyLength), destination, null,
            null, this.getResponseMessageTimeOut(), false);
   }

   /**
    * Sends a synchronous inputstream message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id. <br>
    * <br>
    * <b><i>Note </i> </b>: This method will not close the stream when the message has been dispatched - this is the
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as
    * it is guaranteed that the message has been fully dispatched by then.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBodyReader the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * @param destinationAddress the address to dispatch the message to.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final InputStream messageBodyReader,
         final long bodyLength, final TcpEndPointIdentifier destinationAddress) throws MessageDispatchFailedException,
         ResponseTimeOutException
   {
      return this.dispatchMessage(header, new InputStreamMessageWriter(
            messageBodyReader, bodyLength), this.getDestinationForMessageDispatch(header, destinationAddress), null,
            null, this.getResponseMessageTimeOut(), false);
   }

   /**
    * Sends a synchronous inputstream message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id. <br>
    * <br>
    * <b><i>Note </i> </b>: This method will not close the stream when the message has been dispatched - this is the
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as
    * it is guaranteed that the message has been fully dispatched by then.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBodyReader the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * @param destination the destination to dispatch the message to.
    * @param timeOut the timeout to be used when waiting for a response or and endpoint to become available.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    * 
    * @since 2.1.2 (20060307)
    */
   public Message dispatchMessage(final MessageHeader header, final InputStream messageBodyReader,
         final long bodyLength, final Destination destination, final long timeOut)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new InputStreamMessageWriter(
            messageBodyReader, bodyLength), destination, null,
            null, ((timeOut > 0) ? timeOut : this.getResponseMessageTimeOut()), false);
   }
   
   /**
    * Sends a synchronous inputstream message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id. <br>
    * <br>
    * <b><i>Note </i> </b>: This method will not close the stream when the message has been dispatched - this is the
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as
    * it is guaranteed that the message has been fully dispatched by then.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBodyReader the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * @param destinationAddress the address to dispatch the message to.
    * @param timeOut the timeout to be used when waiting for a response or and endpoint to become available.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final InputStream messageBodyReader,
         final long bodyLength, final TcpEndPointIdentifier destinationAddress, final long timeOut)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new InputStreamMessageWriter(
            messageBodyReader, bodyLength), this.getDestinationForMessageDispatch(header, destinationAddress), null,
            null, ((timeOut > 0) ? timeOut : this.getResponseMessageTimeOut()), false);
   }
   
   /**
    * Sends an asynchronous inputstream message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id. <br>
    * <br>
    * <b><i>Note </i> </b>: This method will not close the stream when the message has been dispatched - this is the
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as
    * it is guaranteed that the message has been fully dispatched by then.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message, by using the message header of the incomming 
    * message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBodyReader the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * @param destination the destination to dispatch the message to.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    *                    
    * @since 2.1.2 (20060307)
    */
   public void dispatchMessageAsync(final MessageHeader header, final InputStream messageBodyReader,
         final long bodyLength, final Destination destination) throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new InputStreamMessageWriter(messageBodyReader,
            bodyLength), destination, null, null,
            this.getAsynchMessageDispatchTimeOut(), true);
   }

   /**
    * Sends an asynchronous inputstream message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id. <br>
    * <br>
    * <b><i>Note </i> </b>: This method will not close the stream when the message has been dispatched - this is the
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as
    * it is guaranteed that the message has been fully dispatched by then.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message, by using the message header of the incomming 
    * message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBodyReader the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * @param destinationAddress the address to dispatch the message to.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    */
   public void dispatchMessageAsync(final MessageHeader header, final InputStream messageBodyReader,
         final long bodyLength, final TcpEndPointIdentifier destinationAddress) throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new InputStreamMessageWriter(messageBodyReader,
            bodyLength), this.getDestinationForMessageDispatch(header, destinationAddress), null, null,
            this.getAsynchMessageDispatchTimeOut(), true);
   }

   /**
    * Sends a synchronous inputstream message to a destination with the specified meta data constraints. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id. <br>
    * <br>
    * <b><i>Note </i> </b>: This method will not close the stream when the message has been dispatched - this is the
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as
    * it is guaranteed that the message has been fully dispatched by then.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBodyReader the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * @param destinationMetaDataConstraints a java.util.Map object containing mappings for matching against the meta
    *                   data of a destination object.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final InputStream messageBodyReader,
         final long bodyLength, final Map destinationMetaDataConstraints) throws MessageDispatchFailedException,
         ResponseTimeOutException
   {
      return this.dispatchMessage(header, new InputStreamMessageWriter(
            messageBodyReader, bodyLength), null, null, destinationMetaDataConstraints,
            this.getResponseMessageTimeOut(), false);
   }

   /**
    * Sends a synchronous inputstream message to a destination with the specified meta data constraints. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id. <br>
    * <br>
    * <b><i>Note </i> </b>: This method will not close the stream when the message has been dispatched - this is the
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as
    * it is guaranteed that the message has been fully dispatched by then.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBodyReader the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * @param destinationMetaDataConstraints a java.util.Map object containing mappings for matching against the meta
    *                   data of a destination object.
    * @param timeOut the timeout to be used when waiting for a response or and endpoint to become available.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final InputStream messageBodyReader,
         final long bodyLength, final Map destinationMetaDataConstraints, final long timeOut)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new InputStreamMessageWriter(
            messageBodyReader, bodyLength), null, null, destinationMetaDataConstraints, ((timeOut > 0) ? timeOut
            : this.getResponseMessageTimeOut()), false);
   }

   /**
    * Sends an asynchronous inputstream message to a destination with the specified meta data constraints. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id. <br>
    * <br>
    * <b><i>Note </i> </b>: This method will not close the stream when the message has been dispatched - this is the
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as
    * it is guaranteed that the message has been fully dispatched by then.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message, by using the message header of the incomming 
    * message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBodyReader the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * @param destinationMetaDataConstraints a java.util.Map object containing mappings for matching against the meta
    *                   data of a destination object.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    */
   public void dispatchMessageAsync(final MessageHeader header, final InputStream messageBodyReader,
         final long bodyLength, final Map destinationMetaDataConstraints) throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new InputStreamMessageWriter(messageBodyReader,
            bodyLength), null, null, destinationMetaDataConstraints, this.getAsynchMessageDispatchTimeOut(),
            true);
   }

   /**
    * Method to dispatch a synchronous inputstream message to a destination with the specified named receiver. Calling
    * this method has the same effect as calling the method {@link #dispatchMessage(MessageHeader, Object, Map, long)},
    * with a map containing parameter <code>namedReceiver</code> mapped to the key
    * {@link MessagingManager#NAMED_MESSAGE_RECEIVER_METADATA_KEY}.<br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id. <br>
    * <br>
    * <b><i>Note </i> </b>: This method will not close the stream when the message has been dispatched - this is the
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as
    * it is guaranteed that the message has been fully dispatched by then.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBodyReader the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final InputStream messageBodyReader,
         final long bodyLength, final String namedReceiver) throws MessageDispatchFailedException,
         ResponseTimeOutException
   {
      return this.dispatchMessage(header, new InputStreamMessageWriter(
            messageBodyReader, bodyLength), null, namedReceiver, null, this.getResponseMessageTimeOut(), false);
   }

   /**
    * Method to dispatch a synchronous inputstream message to a destination with the specified named receiver. Calling
    * this method has the same effect as calling the method {@link #dispatchMessage(MessageHeader, Object, Map, long)},
    * with a map containing parameter <code>namedReceiver</code> mapped to the key
    * {@link MessagingManager#NAMED_MESSAGE_RECEIVER_METADATA_KEY}.<br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id. <br>
    * <br>
    * <b><i>Note </i> </b>: This method will not close the stream when the message has been dispatched - this is the
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as
    * it is guaranteed that the message has been fully dispatched by then.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBodyReader the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * @param timeOut the timeout to be used when waiting for a response or and endpoint to become available.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final InputStream messageBodyReader,
         final long bodyLength, final String namedReceiver, final long timeOut) throws MessageDispatchFailedException,
         ResponseTimeOutException
   {
      return this.dispatchMessage(header, new InputStreamMessageWriter(
            messageBodyReader, bodyLength), null, namedReceiver, null, ((timeOut > 0) ? timeOut
            : this.getResponseMessageTimeOut()), false);
   }

   /**
    * Method to dispatch a synchronous inputstream message to a destination with the specified named receiver. Calling
    * this method has the same effect as calling the method {@link #dispatchMessage(MessageHeader, Object, Map, long)},
    * with a map containing parameter <code>namedReceiver</code> mapped to the key
    * {@link MessagingManager#NAMED_MESSAGE_RECEIVER_METADATA_KEY}.<br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id. <br>
    * <br>
    * <b><i>Note </i> </b>: This method will not close the stream when the message has been dispatched - this is the
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as
    * it is guaranteed that the message has been fully dispatched by then.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message, by using the message header of the incomming 
    * message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBodyReader the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    */
   public void dispatchMessageAsync(final MessageHeader header, final InputStream messageBodyReader,
         final long bodyLength, final String namedReceiver) throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new InputStreamMessageWriter(messageBodyReader,
            bodyLength), null, namedReceiver, null, this.getAsynchMessageDispatchTimeOut(), true);
   }
   
   /**
    * Sends a synchronous inputstream message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id. <br>
    * <br>
    * <b><i>Note </i> </b>: This method will not close the stream when the message has been dispatched - this is the
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as
    * it is guaranteed that the message has been fully dispatched by then.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBodyReader the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * @param destination the destination to dispatch the message to.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    * 
    * @since 2.1.2 (20060307)
    */
   public Message dispatchMessage(final MessageHeader header, final InputStream messageBodyReader,
         final long bodyLength, final Destination destination, final String namedReceiver)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new InputStreamMessageWriter(
            messageBodyReader, bodyLength), destination,
            namedReceiver, null, this.getResponseMessageTimeOut(), false);
   }

   /**
    * Sends a synchronous inputstream message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id. <br>
    * <br>
    * <b><i>Note </i> </b>: This method will not close the stream when the message has been dispatched - this is the
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as
    * it is guaranteed that the message has been fully dispatched by then.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBodyReader the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * @param destinationAddress the address to dispatch the message to.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final InputStream messageBodyReader,
         final long bodyLength, final TcpEndPointIdentifier destinationAddress, final String namedReceiver)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new InputStreamMessageWriter(
            messageBodyReader, bodyLength), this.getDestinationForMessageDispatch(header, destinationAddress),
            namedReceiver, null, this.getResponseMessageTimeOut(), false);
   }
   
   /**
    * Sends a synchronous inputstream message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id. <br>
    * <br>
    * <b><i>Note </i> </b>: This method will not close the stream when the message has been dispatched - this is the
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as
    * it is guaranteed that the message has been fully dispatched by then.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBodyReader the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * @param destination the destination to dispatch the message to.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * @param timeOut the timeout to be used when waiting for a response or and endpoint to become available.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    * 
    * @since 2.1.2 (20060307)
    */
   public Message dispatchMessage(final MessageHeader header, final InputStream messageBodyReader,
         final long bodyLength, final Destination destination, final String namedReceiver,
         final long timeOut) throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new InputStreamMessageWriter(
            messageBodyReader, bodyLength), destination,
            namedReceiver, null, ((timeOut > 0) ? timeOut : this.getResponseMessageTimeOut()), false);
   }

   /**
    * Sends a synchronous inputstream message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id. <br>
    * <br>
    * <b><i>Note </i> </b>: This method will not close the stream when the message has been dispatched - this is the
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as
    * it is guaranteed that the message has been fully dispatched by then.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBodyReader the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * @param destinationAddress the address to dispatch the message to.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * @param timeOut the timeout to be used when waiting for a response or and endpoint to become available.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final InputStream messageBodyReader,
         final long bodyLength, final TcpEndPointIdentifier destinationAddress, final String namedReceiver,
         final long timeOut) throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(header, new InputStreamMessageWriter(
            messageBodyReader, bodyLength), this.getDestinationForMessageDispatch(header, destinationAddress),
            namedReceiver, null, ((timeOut > 0) ? timeOut : this.getResponseMessageTimeOut()), false);
   }
   
   /**
    * Sends an asynchronous inputstream message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id. <br>
    * <br>
    * <b><i>Note </i> </b>: This method will not close the stream when the message has been dispatched - this is the
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as
    * it is guaranteed that the message has been fully dispatched by then.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message, by using the message header of the incomming 
    * message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBodyReader the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * @param destination the destination to dispatch the message to.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    *                    
    * @since 2.1.2 (20060307)
    */
   public void dispatchMessageAsync(final MessageHeader header, final InputStream messageBodyReader,
         final long bodyLength, final Destination destination, final String namedReceiver)
         throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new InputStreamMessageWriter(messageBodyReader,
            bodyLength), destination, namedReceiver, null,
            this.getAsynchMessageDispatchTimeOut(), true);
   }

   /**
    * Sends an asynchronous inputstream message to a specific destination. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id. <br>
    * <br>
    * <b><i>Note </i> </b>: This method will not close the stream when the message has been dispatched - this is the
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as
    * it is guaranteed that the message has been fully dispatched by then.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message, by using the message header of the incomming 
    * message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageBodyReader the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * @param namedReceiver the name of a message receiver in a remote destination.
    * @param destinationAddress the address to dispatch the message to.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    */
   public void dispatchMessageAsync(final MessageHeader header, final InputStream messageBodyReader,
         final long bodyLength, final TcpEndPointIdentifier destinationAddress, final String namedReceiver)
         throws MessageDispatchFailedException
   {
      this.dispatchMessage(header, new InputStreamMessageWriter(messageBodyReader,
            bodyLength), this.getDestinationForMessageDispatch(header, destinationAddress), namedReceiver, null,
            this.getAsynchMessageDispatchTimeOut(), true);
   }

      
   /* ### MESSAGE DISPATCH METHODS END ###  */
   
   
   /* ### DESTINATION/ADDRESS METHODS BEGIN ###  */
   
      
   /**
    * Adds a local host remote messaging system that this MessagingManager is to communicate with.
    * 
    * @param port the portnumber on which the BLiP at the specified address is listening for connections.
    */
   public void addDestination(int port)
   {
      addDestination(new TcpEndPointIdentifier(port));
   }

   /**
    * Adds an address to a remote messaging systen that this MessagingManager is to communicate with.
    * 
    * @param address the ip address of a remote messaging system to add.
    * @param port the portnumber on which the remote messaging at the specified address is listening for connections.
    */
   public void addDestination(String address, int port)
   {
      addDestination(new TcpEndPointIdentifier(address, port));
   }

   /**
    * Adds an address to a remote messaging systen that this MessagingManager is to communicate with.
    * 
    * @param address the address of a remote messaging system to add.
    */
   public void addDestination(TcpEndPointIdentifier address)
   {
      if (address == null) return;

      if ((this.getStatus() == ENABLED) || (this.getStatus() == INITIALIZING))
      {
         remoteDestinationAddresses.addAddress(address);
      }
      else
      {
         remoteDestinationAddresses.setNotificationMode(false);
         remoteDestinationAddresses.addAddress(address);
         remoteDestinationAddresses.setNotificationMode(true);
      }
   }
   
   /**
    * Gets the Destination object representing the remote messaging system at the specified address.
    * 
    * @param address the address of a remote messaging system.
    * 
    * @return a Destination object representing the remote messaging system or <code>null</code> if none was found.
    */
   public Destination getDestination(final TcpEndPointIdentifier address)
   {
      return (Destination) super.getTcpEndPointGroup(address);
   }

   /**
    * Gets the Destination for the specified address. If no Destination was found matching the specified address, and
    * parameter <code>create</code> is <code>true</code>, a new Destination will be created using the method
    * {@link #createTcpEndPointGroup(TcpEndPointIdentifier)}. The created Destination will also be added to this
    * MessagingManager.
    * 
    * @param address the address of the Destination.
    * @param create flag indicating if a new Destination should be created if none was found matching the specified
    *                   address.
    * @param clientSide flag indicating if a Destination created by this method should be client side (<code>true</code>) or
    *                   server side (<code>false</code>).
    * 
    * @return a Destination for the specified address.
    */
   protected Destination getDestination(final TcpEndPointIdentifier address, final boolean create, final boolean clientSide)
   {
      return (Destination) super.getTcpEndPointGroup(address, create, clientSide);
   }

   /**
    * Gets the Destination object representing the remote messaging system with the specified id. This id can be
    * obtained by for instance calling the method {@link MessageHeader#getSenderId()}for an incomming message.
    * 
    * @param destinationId the id of a destination (remote messaging system).
    * 
    * @return a Destination object representing the remote messaging system or <code>null</code> if none was found.
    */
   public Destination getDestination(final long destinationId)
   {
      Destination[] destArray = this.getDestinations();

      for (int i = 0; i < destArray.length; i++)
      {
         if (destArray[i].getDestinationId() == destinationId) return destArray[i];
      }

      return null;
   }

   /**
    * Removes the connections to the remote messaging system at the specified address.
    * 
    * @param address the address of a remote messaging system to remove connections to.
    */
   public void removeDestination(final TcpEndPointIdentifier address)
   {
      if (address == null) return;

      if ((this.getStatus() == ENABLED) || (this.getStatus() == INITIALIZING))
      {
         remoteDestinationAddresses.removeAddress(address);
      }
      else
      {
         remoteDestinationAddresses.setNotificationMode(false);
         remoteDestinationAddresses.removeAddress(address);
         remoteDestinationAddresses.setNotificationMode(true);
      }
   }

   /**
    * Sets the addresses of the remote messaging systems that this MessagingManager should connect to.
    * 
    * @param newDestinationAddresses an array of TcpEndPointIdentifier object representing the new client side addresses of this MessagingManager.
    * 
    * @return <code>true</code> if the addresses were set successfully, otherwise <code>false</code>.
    */
   public boolean setRemoteDestinationAddresses(final TcpEndPointIdentifier[] newDestinationAddresses)
   {
      return remoteDestinationAddresses.setValue(newDestinationAddresses);
   }
   
   /**
    * Gets the addresses of the remote messaging systems that this MessagingManager should connect to.
    */
   public TcpEndPointIdentifier[] getRemoteDestinationAddresses()
   {
      return remoteDestinationAddresses.getAddresses();
   }
   
   /**
    * Gets the address that is configured to be used as bind address for the specified address.
    * 
    * @since 2.0
    */
   public TcpEndPointIdentifier getBindAddressForRemoteAddress(final TcpEndPointIdentifier remoteAddress)
   {
      return (TcpEndPointIdentifier) this.remoteDestinationAddresses.getAssociatedAddress(remoteAddress);
   }

   /**
    * Gets the addresses of the client side and server side remote messaging systems (destinations) that this
    * MessagingManager has connections to.
    * 
    * @return a List of {@link TcpEndPointIdentifier}addresses.
    */
   public final List getDestinationAddresses()
   {
      final Destination[] destArray = this.getDestinations();
      final ArrayList destAddrList = new ArrayList();

      for (int i = 0; i < destArray.length; i++)
      {
         if (destArray[i] != null)
         {
            destAddrList.add(destArray[i].getAddress());
         }
      }

      return destAddrList;
   }

   /**
    * Gets a list of client side (connections created from this MessagingManager) Destination objects, each representing
    * a remote messaging system.
    * 
    * @return a list of Destination objects, each representing a remote messaging system.
    * 
    * @see Destination
    */
   public Destination[] getClientSideDestinations()
   {
      return (Destination[]) this.clientSideDestinations.toArray(new Destination[0]);
   }

   /**
    * Gets a list of active Destination objects, each representing a remote messaging system.
    * 
    * @return a list of Destination objects, each representing a remote messaging system.
    * 
    * @see Destination
    */
   public Destination[] getDestinations()
   {
      synchronized(super.getEndpointGroupsLock())
      {
         return (Destination[]) super.endpointGroups.getItems(new Destination[0]);
      }
   }

   /**
    * Gets the destinations that matches the specified named receiver.
    * 
    * @param namedReceiver the name of a message receiver in a remote destination.
    * 
    * @return an array of Destination objects representing the remote messaging systems matching the specified name
    *                receiver.
    * 
    * @since 1.3.1 Build 676
    */
   public Destination[] getDestinations(final String namedReceiver)
   {
      final Destination[] destinations = this.getDestinations();
      final ArrayList resultingDestinations = new ArrayList();

      // Interate through destinations
      for (int i = 0; i < destinations.length; i++)
      {
         if (destinations[i].hasNamedReceiver(namedReceiver))
         {
            resultingDestinations.add(destinations[i]);
         }
      }

      return (Destination[]) resultingDestinations.toArray(new Destination[0]);
   }
   
   /**
    * Gets the destinations that matches the specified map of meta data. <br>
    * <br>
    * This method will use the current {@link MetaDataValueMatcher}, which can be set using the method
    * {@link #setMetaDataMatcher(MetaDataValueMatcher)}. The default meta data value matcher is an object of the class
    * {@link MessagingManager.ReceiverNameMetaDataValueMatcher}.
    * 
    * @return an array of Destination objects representing the remote messaging systems matching the specified meta
    *                data.
    */
   public Destination[] getDestinations(final Map metaData)
   {
      final Destination[] destinations = this.getDestinations();
      Map destinationMetaData;
      ArrayList resultingDestinations = new ArrayList();
      Iterator metaDataKeyIterator;
      Object key;
      boolean match;

      if (metaData != null)
      {
         // Interate through destinations
         for (int i = 0; i < destinations.length; i++)
         {
            destinationMetaData = destinations[i].getDestinationMetaData();

            if (destinationMetaData != null)
            {
               metaDataKeyIterator = metaData.keySet().iterator();

               match = true;

               // Compare specified meta data with meta data of destination
               while (metaDataKeyIterator.hasNext())
               {
                  key = metaDataKeyIterator.next();

                  if (key != null)
                  {
                     if (this.metaDataMatcher.match(key, destinationMetaData.get(key), metaData.get(key)))
                     {
                        continue;
                     }
                  }

                  match = false;
                  break;
               }

               if (match)
               {
                  resultingDestinations.add(destinations[i]);
               }
            }
         }

         return (Destination[]) resultingDestinations.toArray(new Destination[0]);
      }
      else
      {
         return destinations;
      }
   }
   
   /**
    * Internal method that sets the addresses of the remote messaging systems that this  
    * MessagingManager should connect to. This method calls {@link #doUpdateRemoteDestinations(List)} 
    * using the addresses of the <code>remoteDestinationAddresses</code> property as parameter. 
    */
   protected void updateRemoteDestinations()
   {
      this.doUpdateRemoteDestinations(this.remoteDestinationAddresses.getAddressList());
   }
   
   /**
    * Internal method that sets the addresses of the remote messaging systems that this  
    * MessagingManager should connect to. This method is always called by {@link #updateRemoteDestinations()}, which specifies  
    * the list of addresses that this messaging system currently should be connected to. 
    * 
    * @param newDestinationAddresses a List containing the addresses of the remote destination that this system currently 
    * should have connections to. Any existing client side destination not present in the specified list will be removed.
    */
   protected void doUpdateRemoteDestinations(final List newDestinationAddresses)
   {
      final ArrayList destinationsToRemove = new ArrayList();
      final ArrayList destinationsToAdd = new ArrayList();
      TcpEndPointIdentifier destinationAddress;
      Destination destination;

      synchronized(super.getEndpointGroupsLock())
      {
         // Find addresses to remove
         for (int i = 0; i < this.clientSideDestinations.size(); i++)
         {
            destination = (Destination) this.clientSideDestinations.get(i);
            destinationAddress = (destination != null) ? destination.getAddress() : null;

            if (newDestinationAddresses != null)
            {
               if ((destinationAddress != null) && !(newDestinationAddresses.contains(destinationAddress)))
               {
                  destinationsToRemove.add(destinationAddress);
               }
            }
            else
            {
               destinationsToRemove.add(destinationAddress);
            }
         }

         // Find addresses to add
         if (newDestinationAddresses != null)
         {
            for (int i = 0; i < newDestinationAddresses.size(); i++)
            {
               destinationAddress = (TcpEndPointIdentifier) newDestinationAddresses.get(i);

               // If destination is not null and the destination (TcpEndPointGroup) did not already exist
               if ((destinationAddress != null) && (super.getTcpEndPointGroup(destinationAddress) == null))
               {
                  destinationsToAdd.add(destinationAddress);
               }
            }
         }
      
         // Remove
         for (int i = 0; i < destinationsToRemove.size(); i++)
         {
            this.destroyDestination((TcpEndPointIdentifier) destinationsToRemove.get(i));
         }
   
         // Add
         for (int i = 0; i < destinationsToAdd.size(); i++)
         {
            this.createClientSideDestination((TcpEndPointIdentifier) destinationsToAdd.get(i));
         }
      }
   }

   /**
    * Internal method to add a new client side destination.
    */
   protected void createClientSideDestination(final TcpEndPointIdentifier address)
   {
      //MessagingEndPoint ep = null;

      if (address == null)
      {
         logWarning("Cannot add destination with null address!");
         //return null;
      }

      // Create new Destination (TcpEndPointGroup)
      final Destination destination = (Destination) super.getTcpEndPointGroup(address, true, true);

      synchronized(super.getEndpointGroupsLock())
      {
         //if(this.clientSideDestinations.contains(destination)) return destination;
         if (!this.clientSideDestinations.contains(destination))
         {
            this.clientSideDestinations.add(destination);
         }
      }

      logInfo("Attempting to connect to remote messaging system at address " + address);
      //ep = this.createFirstMessagingEndPoint(address, destination);
      this.createFirstClientSideMessagingEndPoint(address, destination);

      /*
       * This slows up the process of setting the local/remote addresses properties. Neccesary? try {
       * ep.waitForLinkEstablished(); } catch(InterruptedException ie){}
       * 
       * return destination;
       */
   }

   /**
    * Internal method to remove a destination address.
    */
   protected final void destroyDestination(final TcpEndPointIdentifier address)
   {
      if (address == null) return;
      
      Destination destination = null;

      synchronized(super.getEndpointGroupsLock())
      {
         for (int i = 0; i < this.clientSideDestinations.size(); i++)
         {
            if (((Destination) this.clientSideDestinations.get(i)).getAddress().equals(address))
            {
               destination = (Destination) this.clientSideDestinations.get(i);
               break;
            }
         }

         if (destination != null)
         {
            logInfo("Destroying destination: " + destination + ".");

            super.destroyEndPoints(address);
            
            this.clientSideDestinations.remove(destination);
            
            //super.unregisterGroup(destination);
            this.unregisterGroup(destination, true);
         }
      }
   }
   
   /**
    * Creates a new TcpEndPointGroup for the specified address. Subclasses can override this method to create their own
    * implementation of a TcpEndPointGroup.
    * 
    * @param address the address of the TcpEndPointGroup.
    * 
    * @return the newly created TcpEndPointGroup.
    */
   protected TcpEndPointGroup createTcpEndPointGroup(final TcpEndPointIdentifier address)
   {
      // For prevention with inconsistencys between this.clientSideDestinations and super.endpointGroups
      synchronized(super.getEndpointGroupsLock())
      {
         final Destination[] destArray = this.getClientSideDestinations();
         for (int i = 0; i < destArray.length; i++)
         {
            if ((destArray[i] != null) && (destArray[i].getAddress().equals(address)))
            {
               return destArray[i];
            }
         }
      }
      
      Destination destination = this.createDestinationImpl(address);

      return destination;
   }
   
   /**
    * Creates the actual Destination (TcpEndPointGroup) implementation. Subclasses may override this 
    * method to create alternative Destination implementations.
    * 
    * @since 2.1 (20051118)
    */
   protected Destination createDestinationImpl(final TcpEndPointIdentifier address) 
   {
      return new Destination(address);
   }
   
      
   /* ### DESTINATION/ADDRESS METHODS END ###  */
   
   
   /* ### ENDPOINT/ENDPOINTGROUP METHODS BEGIN ###  */
      
   
   /**
    * Method to create the first connection to a certain remote messaging system.
    * 
    * @param address address of the messagin system to connect to.
    * @param destination a Destination object representing the messagin system to connect to.
    * 
    * @see Destination
    */
   protected MessagingEndPoint createFirstClientSideMessagingEndPoint(final TcpEndPointIdentifier address,
         final Destination destination)
   {
      synchronized (super.getEndpointGroupsLock())
      {
         destination.setConnectingFirstEndPoint(true);

         return (MessagingEndPoint) createTcpEndPoint(address, new MessagingEndPointInitData(destination, true));
      }
   }
   
   /**
    * Method called when the first endpoint created for a certain messagin system has successfully or unsuccessfully (as indicated
    * by parameter <code>success</code>) concluded its attempts to connect to a remote messaging system. If the
    * connect attempt was successful this method will try to create additional connections (see
    * {@link #getConnectionsPerDestination()}).
    * 
    * @param endPoint the MessagingEndPoint object that tried to connect to the remote messaging system.
    * @param destination a Destination object representing the remote messaging system that attempts were made to connect to.
    * @param success boolean flag indicating if the connect attempt was successful (<code>true</code>) or
    *                   unsuccessful (<code>false</code>).
    * 
    * @see MessagingEndPoint
    * @see Destination
    */
   protected void firstClientSideEndPointEstablishedLink(final MessagingEndPoint endPoint,
         final Destination destination, boolean success)
   {
      String remoteServerName;

      synchronized (super.getEndpointGroupsLock())
      {
         remoteServerName = (String) destination.getDestinationMetaData(SERVER_NAME_METADATA_KEY);
         if (remoteServerName == null) remoteServerName = "messaging system";

         destination.setConnectingFirstEndPoint(false);
         if (success)
         {
            destination.setError(false);
            destination.setAllEndPointsDisconnected(false);
         }
         else if (destination.getErrorCount() == 0)
         {
            destination.setError(true);
         }
         
         if( this.getDestination(destination.getAddress()) != null ) // Make sure that the destination still exists (i.e. hasn't been destroyed)
         {   
            if (success)
            {
               // Create more connections
               int maxConnections = getConnectionsPerDestination();
      
               if( this.isDebugMode() ) 
               {
                  logDebug("Initializing destination " + destination + "."
                     + ". Creating " + (maxConnections - 1) + " additional endpoint"
                     + (((maxConnections - 1) != 1) ? "s" : "") + ".");
               }
      
               for (int i = 0; i < (maxConnections - 1); i++)
               {
                  super.createTcpEndPoint((TcpEndPointIdentifier) endPoint.getEndPointIdentifier(),
                        new MessagingEndPointInitData(destination));
               }
            }
            else if (destination.getErrorCount() < 10)
            {
               logWarning("Unable to contact " + remoteServerName + " at address " + destination.getAddress()
                     + ". Attempts to establish contact are underway (" + destination.getErrorCount() + ").");
            }
         }
         else
         {
            if( this.isDebugMode() ) logDebug("Skipping creation of additional endpoints - destination destroyed - " + destination + ".");
         }
      }
   }
   
   
   /**
    * Method to unregister the specified endpoint group.
    * 
    * @param destination a Destination.
    * 
    * @since 1.3
    */
   protected void unregisterGroup(final TcpEndPointGroup destination)
   {
      this.unregisterGroup(destination, false);
   }

   /**
    * Method to unregister the specified endpoint group.
    * 
    * @param destination a Destination.
    * 
    * @since 2.1 (20050429)
    */
   protected void unregisterGroup(final TcpEndPointGroup destination, final boolean forceUnregister)
   {
      // Check if the specified destination is a client side destination, in which case it shouldn't be removed...
      if ( forceUnregister || !this.clientSideDestinations.contains(destination) )
      {
         // ...otherwise, if server side destination, remove it
         super.unregisterGroup(destination);
         
         this.destinationDestroyed((Destination)destination);
         
         if( this.isDebugMode() ) logDebug("Destination destroyed: " + destination + ".");
      }
   }
   
   /**
    * Called when a destination object is destroyed.<br>
    * <br>
    * <b>NOTE:<b> Since this is a notification method called by an internal thread, any overriding subclass implementations 
    * should make sure that this method executes and returns rather swiftly, without any kind of blocking wait. 
    * 
    * @since 2.1.2 (20060309)
    */
   protected void destinationDestroyed(final Destination destination)
   {
      
   }
      
   
   /* ### ENDPOINT/ENDPOINTGROUP METHODS END ###  */
   
   
   /* ### MESSAGE RECEIVE RELATED METHODS BEGIN ###  */
      
   
   /**
    * Called to get the meta data to be used for handshaking with a remote messaging manager.<br>
    * <br>
    * This implementation only calls {@link #getMetaData(Destination)}.<br>
    * <br>
    * <b>NOTE:<b> Since this is a notification method called by an internal thread, any overriding subclass implementations 
    * should make sure that this method executes and returns rather swiftly, preferably without any blocking wait.<br>
    * 
    * @since 2.1.2 (20060301)
    */
   protected HashMap initializeDestinationMetaData(final Destination destination)
   {
      return this.getMetaData(destination);
   }
   
   /**
    * Called when the meta data of a remote messaging manager has initilized for the first time during handshaking.<br>
    * <br>
    * This implementation only calls {@link #destinationMetaDataUpdated(Destination, HashMap)}.<br>
    * <br>
    * <b>NOTE:<b> Since this is a notification method called by an internal thread, any overriding subclass implementations 
    * should make sure that this method executes and returns rather swiftly, preferably without any blocking wait.<br>
    * 
    * @since 2.1.2 (20060301)
    */
   protected void destinationMetaDataInitialized(final Destination destination)
   {
      this.destinationMetaDataUpdated(destination, null);
   }
   
   /**
    * Called when the meta data of a remote messaging manager has been updated.<br>
    * <br>
    * <b>NOTE:<b> Since this is a notification method called by an internal thread, any overriding subclass implementations 
    * should make sure that this method executes and returns rather swiftly, preferably without any blocking wait.<br>
    * 
    * @since 2.0.3 (20050412)
    */
   protected abstract void destinationMetaDataUpdated(final Destination destination, final HashMap previousDestinationMetaData);

   /**
    * Method for creating a {@link Message}object, for representation of an incomming message. Subclasses may override
    * this method to create custom implementations. Note that this method will be invoked from an internal thread (MessagingEnPoint), 
    * which means that this method cannot be used for throwing exceptions to the client.
    * 
    * @param header the header of the received message.
    * @param endPoint the endpoint on which the message was received.
    * 
    * @since 1.3
    */
   protected Message createMessage(final MessageHeader header, final MessagingEndPoint endPoint)
   {
      return new Message(header, endPoint);
   }   
   
   /**
    * Called when a message is received from a remote messaging system.  
    * 
    * @param message the received message.
    */
   protected abstract void messageReceived(final Message message);
      
   
   /* ### MESSAGE RECEIVE RELATED METHODS END ###  */
   
   
   /* ### NESTED/INTERNAL CLASSES ### */
      
   
   /**
    * Interface for matching messaging system meta data from different destinations.
    * 
    * @since 1.3
    */
   public interface MetaDataValueMatcher
   {

      /**
       * Matches a key with meta data in avaiable destinations.
       */
      public boolean match(Object metaDataKey, Object metaDataValue, Object metaDataMatchValue);
   }

   /**
    * Class for matching receiver names in different destinations.
    * 
    * @since 1.3
    */
   public class ReceiverNameMetaDataValueMatcher implements MetaDataValueMatcher
   {
      /**
       * Matches a receiver name with remote receiver names in avaiable destinations.
       */
      public boolean match(Object metaDataKey, Object metaDataValue, Object metaDataMatchValue)
      {
         if ((metaDataValue == null) && (metaDataMatchValue == null))
         {
            return true;
         }
         else if ((metaDataValue != null) && (metaDataMatchValue != null))
         {
            if (metaDataKey.equals(MessagingManager.NAMED_MESSAGE_RECEIVER_METADATA_KEY))
            {
               if (metaDataMatchValue instanceof String)
               {
                  return ((List) metaDataValue).contains(metaDataMatchValue);
               }
               else if (metaDataMatchValue instanceof Collection)
               {
                  return ((List) metaDataValue).containsAll((Collection) metaDataMatchValue);
               }
               else return false;
            }
            else return metaDataValue.equals(metaDataMatchValue);
         }
         else return false;
      }
   }
}
