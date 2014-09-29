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
import java.util.Map;

import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.property.BooleanProperty;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.property.StringProperty;
import com.teletalk.jserver.tcp.messaging.rpc.RemoteProcedureCall;
import com.teletalk.jserver.tcp.messaging.rpc.RpcHandler;
import com.teletalk.jserver.util.exception.InvocationExceptionTranslator;

/**
 * Abstract {@link MessageReceiver} implementation repesenting a message handler associated with a {@link MessagingManager}.  
 * This class extends {@link SubComponent} and contains a property for the receiver name that this component will be registered 
 * as a receiver of messages with. This name is specified through a constructor parameter, when creating a MessageReceiverComponent,   
 * but it may also be specified through the property at runtime or before starting the server.<br>
 * <br>
 * This class also contains an {@link com.teletalk.jserver.tcp.messaging.rpc.RpcHandler} and the default implementation of the method 
 * {@link #messageReceived(Message)} will attempt to handle incomming messages as RPC messages. The RpcHandler uses this object as it's 
 * default handler.<br>
 * <br>
 * To customize the message handling of this class, simply override the method {@link #messageReceived(Message)}.<br>
 * <br>
 * This class implements the inteface {@link AccessControlMessageReceiver}, which enables the use of an access control list. Access control checks 
 * can be switched on or off for this component using the property <code>accessControlCheckEnabled</code>.
 *
 * @author Tobias Löfstrand
 *  
 * @since 2.0 Build 757
 */
public class MessageReceiverComponent extends SubComponent implements AccessControlMessageReceiver, MessagingManagerAwareMessageReceiver
{
   /** Reference to the associated {@link MessagingManager}. */
   protected MessagingManager messagingManager;
   
   /** The last name that this MessageReceiverComponent was registered under. */
   protected String registeredReceiverName;
   
   /** The current name that this MessageReceiverComponent is registered as a named receiver under. */
   protected final StringProperty receiverName;
   
   /** Flag indicatinf if access control checks are enabled for this message receiver. @since 2.0.2 (20050331) */
   protected final BooleanProperty accessControlCheckEnabled;
   
   /** The RpcHandler for handling of RPC messages. */
   protected RpcHandler rpcHandler;
   
   
   /**
    * Creates a new MessageReceiverComponent.
    * 
    * @since 2.1 (20050422)
    */
   public MessageReceiverComponent()
   {
      this(null, "MessageReceiverComponent", null);
   }
   
   /**
    * Creates a new MessageReceiverComponent.
    * 
    * @param name the name which this message handler will be associated with. 
    */
   public MessageReceiverComponent(final String name)
   {
      this(null, name, name);
   }
   
   /**
    * Creates a new MessageReceiverComponent.
    * 
    * @param componentName the name of this component. 
    * @param receiverName the named receiver name which this MessageReceiverComponent will be associated with.
    */
   public MessageReceiverComponent(final String componentName, final String receiverName)
   {
      this(null, componentName, receiverName);
   }
   
   /**
    * Creates a new MessageReceiverComponent.
    * 
    * @param parent the parent component of this MessageReceiverComponent. This parameter may be null.
    * @param componentName the name of this component. 
    * @param receiverName the named receiver name which this MessageReceiverComponent will be associated with.
    */
   public MessageReceiverComponent(final SubComponent parent, final String componentName, final String receiverName)
   {
      this(parent, componentName, receiverName, false);
   }

   /**
    * Creates a new MessageReceiverComponent.
    * 
    * @param parent the parent component of this MessageReceiverComponent. This parameter may be null.
    * @param componentName the name of this component. 
    * @param receiverName the named receiver name which this MessageReceiverComponent will be associated with.
    * @param accessControlCheckEnabled default value of accessControlCheckEnabled property.
    * 
    * @since 2.0.2 (20050331)
    */
   public MessageReceiverComponent(final SubComponent parent, final String componentName, final String receiverName, final boolean accessControlCheckEnabled)
   {
      super(parent, componentName);
      
      if( parent instanceof MessagingManager ) this.messagingManager = (MessagingManager)parent;
      else this.messagingManager = null;
      
      this.registeredReceiverName = null; //receiverName;
      
      this.receiverName = new StringProperty(this, "receiverName", receiverName, StringProperty.MODIFIABLE_NO_RESTART);
      this.receiverName.setDescription("The name by which this component will be registered as a named receiver of messages.");
      super.addProperty(this.receiverName);
      
      this.accessControlCheckEnabled = new BooleanProperty(this, "accessControlCheckEnabled", accessControlCheckEnabled, BooleanProperty.MODIFIABLE_OWNER_RESTART);
      this.accessControlCheckEnabled.setDescription("Flag indicating if the access control check is enabled or not for this message receiver.");
      super.addProperty(this.accessControlCheckEnabled);
      
      this.initRpcHandler();
   }
   
   /**
    * Statusmethod for the status INITIALIZING.
    */
   protected void doInitialize()
   {
      super.doInitialize();
      
      if( (this.registeredReceiverName == null) || (!this.receiverName.stringValue().equals(this.registeredReceiverName)) ) // i.e. not registered or registered under different name
      {
         this.performRegistration(); // Auto register
      }
   }
      
	/**
	 * Called when a property owne by this MessageReceiverComponent is modified. This implementation handles 
    * updates to the receiverName property.
	 * 
	 * @param property the property that was modified.
	 */
   public void propertyModified(final Property property)
   {
      if( property == this.receiverName )
      {
         if( this.messagingManager != null )
         {
            this.performRegistration();
         }
      }
      super.propertyModified(property);
   }
   
   private void performRegistration()
   {
      MessagingManager msgMan = this.messagingManager;
      if( (msgMan == null) && (super.parent instanceof MessagingManager) ) msgMan = (MessagingManager)parent; 
      
      if( msgMan != null )
      {
         if( (this.receiverName.stringValue() == null) || !this.receiverName.stringValue().equals(this.registeredReceiverName) ) // Only re-register if name is different
         {
            msgMan.reRegisterMessageReceiver(this, this.registeredReceiverName, this.receiverName.stringValue());
            this.registeredReceiverName = this.receiverName.stringValue();
         }
      }
   }
   
   /**
    * Initializes the RpcHandler.
    * 
    * @since 2.1 (20050422)
    */
   protected void initRpcHandler()
   {
      this.rpcHandler = new RpcHandler(this.messagingManager);
      this.rpcHandler.setDefaultHandler(this);
   }
   
   /**
    * Gets the {@link MessagingManager} associated with this MessageReceiverComponent.
    * 
    * @since 2.0 Build 757.
    */
   public MessagingManager getMessagingManager()
   {
      return this.messagingManager;
   }
   
   /**
    * Sets the {@link MessagingManager} to be associated with this MessageReceiverComponent and 
    * creates an {@link RpcHandler}. This method is called when this MessageReceiverComponent has been registered in 
    * the specified MessagingManager.
    * 
    * @since 2.0 Build 757.
    */
   public void setMessagingManager(final MessagingManager messagingManager)
   {
      this.messagingManager = messagingManager;
      if( this.messagingManager != null )
      {
         if( this.rpcHandler == null )
         {
            this.initRpcHandler();
         }
         else
         {
            this.rpcHandler.setMessagingManager(this.messagingManager);
         }
      }
   }

	/**
	 * Gets the name by which this MessageHandler is currently registered as a receiver of messages in the associated {@link MessagingManager}.
	 */
	public String getMessageReceiverName()
	{
		return this.receiverName.stringValue();
	}
	
	/**
	 * Sets the name by which this MessageHandler is to be registered as a receiver of messages in the associated {@link MessagingManager}.
	 */
	public void setMessageReceiverName(String receiverName)
	{
		this.receiverName.setValue(receiverName);
	}
   
   /**
    * Sets the name by which this MessageHandler is to be registered as a receiver of messages in the associated {@link MessagingManager}. 
    * 
    * @since 2.1 (20050422)
    */
   void updateMessageReceiverName(String receiverName)
   {
      this.receiverName.setForceMode(true);
      this.receiverName.setNotificationMode(false);
      this.receiverName.setValue(receiverName);
      this.receiverName.setNotificationMode(true);
      this.receiverName.setForceMode(false);
      this.registeredReceiverName = this.receiverName.stringValue();
   }
   
   /**
    * Gets the flag indicating if access control checks are enabled for this message receiver (default false). This flag will be checked by 
    * MessagingManager automatically before access control list checks are performed.
    * 
    * @since 2.0.2 (20050331)
    */
   public boolean isAccessControlCheckEnabled()
   {
      return accessControlCheckEnabled.booleanValue();
   }
   
   /**
    * Sets the flag indicating if access control checks are enabled for this message receiver (default false). This flag will be checked by 
    * MessagingManager automatically before access control list checks are performed.
    * 
    * @since 2.0.2 (20050331)
    */
   public void setAccessControlCheckEnabled(boolean accessControlCheckEnabled)
   {
      this.accessControlCheckEnabled.setValue(accessControlCheckEnabled);
   }
   
   /**
    * Gets the {@link RpcHandler} to be used by this MessageHandler to handle processing of RPC messages.<br>
    * <br>
    * <b>Note:</b> This method may return null if this message receiver component was created without a reference to a 
    * {@link MessagingManager} and if the method {@link #setMessagingManager(MessagingManager)} hasn't been called yet. 
    */
   public RpcHandler getRpcHandler()
   {
      return this.rpcHandler;
   }
   
   /**
    * Sets the {@link RpcHandler} to be used by this MessageHandler to handle processing of RPC messages.
    */
   public void setRpcHandler(RpcHandler rpcHandler)
   {
      this.rpcHandler = rpcHandler;
   }
   
   /**
    * Called to identify a operation (message) prior to access control check. The returned string will be used to perform a lookup in the 
    * access control list to se if the operation is allowed to be executed from the address it was send from. If the message cannot be 
    * identified, this implementation should return <code>null</code>.<br> 
    * <br>
    * <i>Note:</i>If the message body needs to be parsed to identify the message, the method Message.setMessageBodyCachingEnabled(boolean) 
    * should be called to cache the message, i.e. to make it available when {@link #messageReceived(Message)} is called.<br>
    * <br>
    * This default implementation returns the method name for RPC messages, otherwise it returns <code>null</code> 
    * (i.e. indicating that access control check is not to be performed).<br>
    * <br>
    * Subclasses that wish to provide a custom implementation for identification of messages (requried for all non-rpc messages), should 
    * override this method.
    * 
    * @since 2.0.2 (20050331)
    */
   public String identify(final Message message) throws Exception
   {
      if( RemoteProcedureCall.isRemoteProcedureCall(message) )
      {
         return RemoteProcedureCall.getRPCMethodName(message);
      }
      else return null;
   }
      
   /**
    * Called when a message, that has a resource name that matches the name of this object, 
    * is received in the associated {@link MessagingManager}. The default implementation of this 
    * method uses an {@link RpcHandler} to attempt to handle the message as an RPC message.
    * The RpcHandler uses this object as it's default handler.
    * 
    * @param message the received {@link Message}.
    */
   public void messageReceived(final Message message)
   {
      RpcHandler.defaultHandleRpcMessage(message, this.rpcHandler, this, this.messagingManager);
   }
   
   /**
    * Convenience method to dispatche a response message (asynch). 
    * 
    * @param header the header of the message to be dispatched.
    * @param body the object body of the message. Must be serializable.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * 
    * @since 2.0.2 (20050404)
    */
   public void dispatchResponse(final MessageHeader header, final Object body) throws MessageDispatchFailedException
   {
      this.messagingManager.dispatchMessageAsync(header, body);
   }
   
   /**
    * Convenience method to dispatche a response message (asynch).
    * 
    * @param header the header of the message to be dispatched.
    * @param body the byte array body of the message.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * 
    * @since 2.0.2 (20050404)
    */
   public void dispatchResponse(final MessageHeader header, final byte[] body) throws MessageDispatchFailedException
   {
      this.messagingManager.dispatchMessageAsync(header, body);
   }
   
   /**
    * Convenience method to dispatche a response message (asynch).
    * 
    * @param header the header of the message to be dispatched.
    * @param body the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to
    *                    lack of connected endpoints).
    * 
    * @since 2.0.2 (20050404)
    */
   public void dispatchResponse(final MessageHeader header, final InputStream body, final long bodyLength) throws MessageDispatchFailedException
   {
      this.messagingManager.dispatchMessageAsync(header, body, bodyLength);
   }
   
   /**
    * Convenience method to set the default RPC method handler of the {@link RpcHandler} of this component. One of the 
    * reasons why this method exists here is to facilitate configuration when using Spring. 
    * 
    * @since 2.1 (20050422)
    * 
    * @see RpcHandler#setDefaultHandler(Object)
    */
   public void setRpcDefaultHandler(final Object defaultHandler)
   {
      if( this.rpcHandler != null ) this.rpcHandler.setDefaultHandler(defaultHandler);
   }
   
   /**
    * Convenience to set the RPC handler mappings in the {@link RpcHandler} of this component, removing the previous handler mappings.
    * One of the reasons why this method exists here is to facilitate configuration when using Spring.
    * 
    * @param handlers the new handler mappings (i.e. String to Object mappings). 
    * 
    * @since 2.1 (20050422)
    * 
    * @see RpcHandler#setHandlers(Map)
    */
   public void setRpcHandlerMappings(final Map handlers)
   {
      if( this.rpcHandler != null ) this.rpcHandler.setHandlers(handlers); 
   }
   
   /**
    * Convenience to get the RPC handler mappings in the {@link RpcHandler} of this component.
    * One of the reasons why this method exists here is to facilitate configuration when using Spring.
    * 
    * @since 2.1.5 (20070301)
    * 
    * @see RpcHandler#setHandlers(Map)
    */
   public Map getRpcHandlerMappings()
   {
      if( this.rpcHandler != null ) return this.rpcHandler.getHandlers();
      else return null;
   }
   
   /**
    * Convenience method to set the {@link InvocationExceptionTranslator} for the associated {@link RpcHandler} of this component.
    * 
    * @param invocationExceptionTranslator the exception translator.  
    * 
    * @since 2.1 (20050426)
    * 
    * @see RpcHandler#setMethodInvocationExceptionTranslator(InvocationExceptionTranslator)
    */
   public void setRpcHandlerExceptionTranslator(final InvocationExceptionTranslator invocationExceptionTranslator)
   {
      if( this.rpcHandler != null ) this.rpcHandler.setMethodInvocationExceptionTranslator(invocationExceptionTranslator); 
   }
}
