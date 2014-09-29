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
package com.teletalk.jserver.queue.messaging;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import com.teletalk.jserver.JServerConstants;
import com.teletalk.jserver.comm.EndPointIdentifier;
import com.teletalk.jserver.queue.QueueItem;
import com.teletalk.jserver.queue.QueueManager;
import com.teletalk.jserver.queue.QueueSystemCollaborationManager;
import com.teletalk.jserver.queue.QueueSystemMetaData;
import com.teletalk.jserver.queue.RemoteQueueSystem;
import com.teletalk.jserver.queue.command.MultiQueueItemTransferRequest;
import com.teletalk.jserver.queue.command.QueueItemTransferRequest;
import com.teletalk.jserver.queue.command.QueueSystemCommand;
import com.teletalk.jserver.queue.command.QueueSystemSynchronizationRequest;
import com.teletalk.jserver.queue.command.QueueSystemSynchronizationResponse;
import com.teletalk.jserver.tcp.TcpEndPointIdentifier;
import com.teletalk.jserver.tcp.messaging.Destination;
import com.teletalk.jserver.tcp.messaging.Message;
import com.teletalk.jserver.tcp.messaging.MessageHeader;
import com.teletalk.jserver.tcp.messaging.MessageReceiver;
import com.teletalk.jserver.tcp.messaging.MessageReceiverComponent;
import com.teletalk.jserver.tcp.messaging.MessagingEndPoint;
import com.teletalk.jserver.tcp.messaging.MessagingException;
import com.teletalk.jserver.tcp.messaging.MessagingManager;

/**
 * QueueSystemCollaborationManager implementation using messaging ({@link com.teletalk.jserver.tcp.messaging.MessagingManager}).
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1.2
 */
public class QueueMessagingManager extends MessagingManager implements QueueSystemCollaborationManager
{
   private static final String MULTI_QUEUE_ITEM_TRANSFER_REQUEST_COUNT_KEY = "count";
      
   public static final String QUEUE_SYSTEM_COMMAND_OBJECT_KEY = "queueSystemCommandObject";
   
   
   public static final String IN_QUEUE_SYSTEM_MESSAGE_RECEIVER_NAME_META_DATA_KEY = "InQueueSystemMessageReceiverName";
   
   public static final String QUEUE_SYSTEM_META_DATA_KEY = "queueSystemMetaData";
   
   public static final String QUEUE_SYSTEM_SYNCHRONIZATION_REQUEST_META_DATA_KEY = "QueueSystemSynchronizationRequest";
   
   public static final String QUEUE_SYSTEM_SYNCHRONIZATION_RESPONSE_META_DATA_KEY = "QueueSystemSynchronizationResponse";
   
   
   public static final long DEFAULT_STARTUP_SYNCHRONIZATION_TIMEOUT = 60*1000;
   
   
   
   private final QueueManager queueManager;
   
   private final QueueManagerImplWrapper queueManagerImplWrapper;
      

   private long startupSynchronizationTimeout = DEFAULT_STARTUP_SYNCHRONIZATION_TIMEOUT;
   
   
   /**
    * Creates a QueueMessagingManager using the server name as queue system message receiver name (only use this 
    * constructor for queue systems without an in queue).
    */
   public QueueMessagingManager(QueueManager parent)
   {
      this(parent, "QueueMessagingManager");
   }
   
   /**
    * Creates a QueueMessagingManager using the server name as queue system message receiver name (only use this 
    * constructor for queue systems without an in queue).
    */
   public QueueMessagingManager(QueueManager parent, String name)
   {
      this(parent, name, null);
   }

   /**
    * Creates a QueueMessagingManager.
    */
   public QueueMessagingManager(QueueManager parent, String name, String queueSystemMessageReceiverName)
   {
      super(parent, name);
      
      this.queueManager = parent;
      
      // Provide a custom QueueManagerImpl implementation that overrides dispatchQueueItem and dispatchQueueItems to ensure correct behaviour
      this.queueManagerImplWrapper = new QueueManagerImplWrapper(this.queueManager);
      this.queueManager.setQueueManagerImpl(this.queueManagerImplWrapper);
      
      // Don't use a message handler pool
      super.setUseMessageHandlerPool(false);
      
      // Set default asych message dispatch timeout
      super.asynchMessageDispatchTimeOut.setValue(10000);
      super.asynchMessageDispatchTimeOut.setDefaultValueToCurrentValue();
                        
      // IMPORTANT - make sure that the connections per destination setting is fixed to 1 (one) to ensure correct behaviour in QueueManager!  
      super.setConnectionsPerDestination(1);
      super.removeProperty(super.connectionsPerDestination);
      
      if (queueSystemMessageReceiverName != null)
      {
         InQueueSystemMessageReceiver inQueueSystemMessageReceiver = new InQueueSystemMessageReceiver(this, queueSystemMessageReceiverName);
         super.registerMessageReceiverComponent(inQueueSystemMessageReceiver);
      }
      super.setDefaultMessageReceiver(new DefaultQueueSystemMessageReceiver(this));
      
      super.setMetaData(IN_QUEUE_SYSTEM_MESSAGE_RECEIVER_NAME_META_DATA_KEY, queueSystemMessageReceiverName);
   }
   
   /**
    * Shuts down this system.
    */
   protected void doShutDown()
   {
      super.doShutDown();
   }
   
   
   /* ### QUEUEMESSAGINGMANAGER METHODS BEGIN ### */
   
   
   /**
    * Gets the associated QueueManager.
    */
   public QueueManager getQueueManager()
   {
      return queueManager;
   }
   
   /**
    * Called when a message is received. This method invokes an interna sequential message handler to handle the message in
    * {@link #doQueueSystemMessageReceived(Message)}.
    */
   protected void queueSystemMessageReceived(final Message message)
   {
      try
      {
         // Read and cache the message body here to prevent an input stream read timeout if message handling is slow/stalled
         message.readAndCacheBody();
         ((RemoteQueueSystemDestination)message.getDestination()).queueSystemMessageReceived(message);
      }
      catch (Exception e) 
      {
         logError("Error occurred while receiving queue system message: " + message + ".", e);
      }
   }

   /**
    * Called by an internal sequential message handler when a message is received.
    */
   protected void doQueueSystemMessageReceived(final Message message)
   {
      try
      {
         QueueSystemCommand queueSystemCommand = (QueueSystemCommand)message.getBodyAsObject();
         
         RemoteQueueSystemDestination remoteQueueSystemDestination = ((RemoteQueueSystemDestination)message.getDestination());
         
         // Execute command in RemoteQueueSystemDestination (which will result in a call to handleQueueSystemCommand())
         queueSystemCommand.execute( remoteQueueSystemDestination );
      }
      catch (Exception e) 
      {
         logError("Error occurred while handling queue system message: " + message + ".", e);
      }
   }
   
   /**
    * Gets the startup synchronization timeout.
    */
   public long getStartupSynchronizationTimeout()
   {
      return startupSynchronizationTimeout;
   }

   /**
    * Sets the startup synchronization timeout.
    */
   public void setStartupSynchronizationTimeout(long startupSynchronizationTimeout)
   {
      this.startupSynchronizationTimeout = startupSynchronizationTimeout;
   }
   
   /**
    * Method to dispatch a command to a remote queue system at the address specified in the command, or to the specified destination (if set).
    */
   public void dispatchCommand(final QueueSystemCommand command, final RemoteQueueSystemDestination destination)
   {
      try
      {
         MessagingQueueSystemEndPointIdentifier address;
         if( destination != null )
         {
            address = (MessagingQueueSystemEndPointIdentifier)destination.getRemoteQueueSystemAddress();
         }
         else
         {
            address = (MessagingQueueSystemEndPointIdentifier)command.getAddress();
         }
         
         if( (address != null) && (address.getServerName() != null) ) // Address and server name specified - typically a response/receipt message
         {
            final RemoteQueueSystemDestination destinationForMessageDispatch = this.getRemoteQueueSystemDestination(address);
            
            if( (destinationForMessageDispatch != null) && destinationForMessageDispatch.isLinkEstablished() )
            {
               // Dispatch in sequential thread for RemoteQueueSystemDestination
               destinationForMessageDispatch.dispatchCommandToRemoteQueueSystemDestination(command);
            }
            else
            {
               if( destinationForMessageDispatch != null ) logWarning("Destination for " + address + " has not established a link yet! Unable to dispatch command (" + command + ")!");
               else logWarning("Unable to find destination for " + address + "! Unable to dispatch command (" + command + ")!");
               
               this.queueManager.commandDeliveryReport(command, false);
            }
         }
         else // Send to server with named receiver...
         {
            final MessageHeader header = this.createMessageHeader(command);
            
            if( (address != null) && (address.getReceiverName() != null) )
            {
               super.dispatchMessageAsync(header, command, address.getReceiverName());
            }
            else // ...or send to any server
            {
               super.dispatchMessageAsync(header, command);
            }
            
            this.queueManager.commandDeliveryReport(command, true);
         }
      }
      catch(Exception e)
      {
         logError("Error dispatching queue system command " + command + "!", e);
         
         this.queueManager.commandDeliveryReport(command, false);
      }
   }
   
   /**
    * Creates a MessageHeader.
    */
   private MessageHeader createMessageHeader(final QueueSystemCommand command)
   {
      final MessageHeader header = new MessageHeader();
            
      header.setMessageType(QueueSystemCommandConstants.getQueueSystemCommandMessageType(command.getClass()));
      
      if( header.getMessageType() == QueueSystemCommandConstants.MULTI_QUEUE_ITEM_TRANSFER_REQUEST_MESSAGE_TYPE )
      {
         header.setCustomHeaderField(MULTI_QUEUE_ITEM_TRANSFER_REQUEST_COUNT_KEY, new Integer( ((MultiQueueItemTransferRequest)command).getQueueItems().length ));
      }
      
      // Set temporary header field containing command object (will be remove just before the request is to be dispathched)
      header.setCustomHeaderField(QUEUE_SYSTEM_COMMAND_OBJECT_KEY, command);
      
      if( this.queueManager.getInQueue() != null )
      {
         // Piggy back queue system meta data (but only if QueueManager has in queue)
         final QueueSystemMetaData queueSystemMetaData = this.queueManager.getQueueSystemMetaData();
         
         final QueueSystemMetaData partialQueueStatusData = new QueueSystemMetaData();
         partialQueueStatusData.setInQueueBlocked(queueSystemMetaData.isInQueueBlocked());
         partialQueueStatusData.setInQueueLength(queueSystemMetaData.getInQueueLength());
         partialQueueStatusData.setinQueueMaxLength(queueSystemMetaData.getinQueueMaxLength());
         
         HashMap partialMetaData = new HashMap();
         partialMetaData.put(QUEUE_SYSTEM_META_DATA_KEY, partialQueueStatusData);
         header.setMessagingSystemMetaData(partialMetaData);
      }
      
      return header;
   }
   
   /**
    * Internal method to dispatch a command to a remote queue system at the address specified in the command, or to the specified destination (if set).
    */
   protected void dispatchCommandToRemoteQueueSystemDestination(final QueueSystemCommand command, final RemoteQueueSystemDestination destination)
   {
      boolean result = false;
      
      final MessageHeader header = this.createMessageHeader(command);
      
      try
      {
         int dispatchAttempts = 0;
         
         if( destination.isLinkEstablished() )
         {
            do
            {
               try
               {
                  super.dispatchMessageAsync(header, command, destination);
                  result = true;
                  break;
               }
               catch(MessagingException me)
               {
                  dispatchAttempts++;
                  
                  if( !destination.isLinkEstablished() )
                  {
                     logWarning("Error (" + me + ") dispatching queue system command " + command + " to " + destination + "!", me);
                     break;
                  }
                  //If attempting to dispatch (and link is established) for more than one minute (asynch timeout=10000ms)...
                  else if( (dispatchAttempts % 6) == 0 )
                  {
                     // ...something is seriously wrong - log critical error
                     logCriticalError("Error dispatching queue system command " + command + " to " + destination + "! Error count: " + dispatchAttempts + ".", me, JServerConstants.LOG_MESSAGE_ID_CRITICAL_SUBSYSTEM_ERROR);
                  }
               }
            }
            while( destination.isLinkEstablished() );
         }
      }
      catch(Exception e)
      {
         result = false;
         logError("Error dispatching queue system command " + command + "!", e);
      }
      
      this.queueManager.commandDeliveryReport(command, result);
   }
   
   
   /**
    */
   private RemoteQueueSystemDestination getRemoteQueueSystemDestination(final MessagingQueueSystemEndPointIdentifier messagingQueueSystemEndPointIdentifier)
   {
      // Attempt to find destination matching server name
      Destination[] destinations = super.getDestinations();
      RemoteQueueSystemDestination remoteQueueSystemDestination;
      for(int i=0; i<destinations.length; i++)
      {
         remoteQueueSystemDestination = (RemoteQueueSystemDestination)destinations[i];
         //if( serverName.equals(destinations[i].getServerName()) )
         if( messagingQueueSystemEndPointIdentifier.equals(remoteQueueSystemDestination.getRemoteQueueSystemAddress()) )
         {
            return remoteQueueSystemDestination;
         }
      }
      
      return null;
   }
   
   
   /* ### QUEUEMESSAGINGMANAGER METHODS END### */
      
   
   /* ##### OVERRIDDEN METHODS FROM QUEUESYSTEMCOLLABORATIONMANAGER: ##### */
   

   /**
    * Performs start up synchronization.
    */
   public void performStartupSynchronization(List addresses)
   {
      try
      {
         long beginWait = System.currentTimeMillis();
         long waitTime; 
         long timeout = this.startupSynchronizationTimeout;
         
         Destination[] destinations = super.getDestinations();
         for(int i=0; i<destinations.length; i++)
         {
            waitTime = timeout - (System.currentTimeMillis() - beginWait);
            
            if(waitTime > 0)
            {
               if( destinations[i] != null )
               {
                  destinations[i].waitForLinkEstablished(waitTime);
               }
            }
            else
            {
               logWarning("Timeout occurred while waiting for destinations to establish link during start up synchronization!");
               break;
            }
         }
      }
      catch(InterruptedException ie)
      {
         logWarning("Interrupted while waiting for destinations to establish link during start up synchronization!");
      }
   }
   

   /**
    * Gets a RemoteQueueSystem object representing a remote queue system at the specified address.<br>
    * <br>
    * Not that QueueMessagingManager never creates new RemoteQueueSystems(RemoteQueueSystemDestinations) 
    * when this method is invoked.
    */
   public RemoteQueueSystem getRemoteQueueSystem(final EndPointIdentifier address, final boolean create)
   {
      return this.getRemoteQueueSystem(address);
   }
   
   /**
    * Gets a RemoteQueueSystem object representing a remote queue system at the specified address.
    */
   public RemoteQueueSystem getRemoteQueueSystem(final EndPointIdentifier address)
   {
      if( address instanceof MessagingQueueSystemEndPointIdentifier )
      {
         MessagingQueueSystemEndPointIdentifier messagingQueueSystemEndPointIdentifier = (MessagingQueueSystemEndPointIdentifier)address;
         
         return getRemoteQueueSystemDestination(messagingQueueSystemEndPointIdentifier);
      }
      return null;
   }
   
   /**
    * Gets a list of all remote queue systems connected to this queue system.
    */
   public List getRemoteQueueSystems()
   {
      return Arrays.asList(super.getDestinations());
   }
   
   /**
    * Method to dispatch a command to a remote queue system at the address specified in the command.
    * 
    * @param command the command to dispatch.
    * 
    * @since 2.1 (20051221)
    */
   public void dispatchCommand(final QueueSystemCommand command)
   {
      dispatchCommand(command, null);
   }
   
   /**
    * Called to handle an incoming command from a remote queue system. 
    */
   public void handleQueueSystemCommand(final QueueSystemCommand command)
   {
      // Execute command in QueueManager
      command.execute(this.queueManager);
   }
   
   /**
    * Performs an internal check of this QueueSystemCollaborationManager.
    */
   public boolean performCheck()
   {
      return (getStatus() != CRITICAL_ERROR);
   }
   
   
   /* ##### OVERRIDDEN METHODS FROM MESSAGINGMANAGER: ##### */
   
   
   /**
    * Called just before a message is to be dispatched.
    */
   protected void beforeMessageDispatch(final MessageHeader header, final MessagingEndPoint endPoint)
   {
      super.beforeMessageDispatch(header, endPoint);
      
      QueueSystemCommand command = (QueueSystemCommand)header.getCustomHeaderField(QUEUE_SYSTEM_COMMAND_OBJECT_KEY);
      if( command != null )
      {
         RemoteQueueSystemDestination remoteQueueSystemDestination = (RemoteQueueSystemDestination)endPoint.getDestination();
         
         // Update address in command (server name)
         command.setAddress(remoteQueueSystemDestination.getRemoteQueueSystemAddress());
         
         // Set address in queue item if transfer request
         if( command instanceof QueueItemTransferRequest )
         {
            QueueItemTransferRequest queueItemTransferRequest = (QueueItemTransferRequest)command;
            QueueItem item = queueItemTransferRequest.getQueueItem(); 
            if( item != null ) item.setSenderReceiverAddress(command.getAddress());
            
            this.queueManagerImplWrapper.onQueueItemTransferRequestDispatch(queueItemTransferRequest);
         }
         else if( command instanceof MultiQueueItemTransferRequest )
         {
            MultiQueueItemTransferRequest multiQueueItemTransferRequest = (MultiQueueItemTransferRequest)command;
            QueueItem[] items = multiQueueItemTransferRequest.getQueueItems();
            if( items != null )
            {
               for (int i = 0; i<items.length; i++)
               {
                  if( items[i] != null ) items[i].setSenderReceiverAddress(command.getAddress());
               }
            }
            
            this.queueManagerImplWrapper.onMultiQueueItemTransferRequestDispatch(multiQueueItemTransferRequest);
         }
      }
      
      // Remove temporary header
      header.removeCustomHeaderField(QUEUE_SYSTEM_COMMAND_OBJECT_KEY);
   }
      
   
   /**
    * Called when a message has been successfully dispatched.
    */
   protected void messageDispatched(final MessageHeader header, final MessagingEndPoint endPoint)
   {
      super.messageDispatched(header, endPoint);
      
      RemoteQueueSystemDestination remoteQueueSystemDestination = (RemoteQueueSystemDestination)endPoint.getDestination();
      
      // Increment expected in queue count
      if( header.getMessageType() == QueueSystemCommandConstants.QUEUE_ITEM_TRANSFER_REQUEST_MESSAGE_TYPE ) remoteQueueSystemDestination.incrementExpectedRemoteInQueueLength();
      else if( header.getMessageType() == QueueSystemCommandConstants.MULTI_QUEUE_ITEM_TRANSFER_REQUEST_MESSAGE_TYPE )
      {
         try{
         int count = ((Integer)header.getCustomHeaderField(MULTI_QUEUE_ITEM_TRANSFER_REQUEST_COUNT_KEY)).intValue();
         for(int i=0; i<count; i++) remoteQueueSystemDestination.incrementExpectedRemoteInQueueLength();
         }catch(Exception e){}
         header.removeCustomHeaderField(MULTI_QUEUE_ITEM_TRANSFER_REQUEST_COUNT_KEY);
      }
   }
      
   /**
    * Creates the actual Destination (TcpEndPointGroup) implementation.
    */
   protected Destination createDestinationImpl(final TcpEndPointIdentifier address) 
   {
      // Get updated meta data from QueueManager every time a new destination is created 
      super.setMetaData(QUEUE_SYSTEM_META_DATA_KEY, this.queueManager.getQueueSystemMetaData());
      
      return new RemoteQueueSystemDestination(this, address);
   }
   
   /**
    * Dispatches a meta data update command to all associated messaging systems.
    */
   protected void dispatchMetaDataUpdateCommand()
   {
      // Get updated meta data from QueueManager every time meta data update command is to be dispatched 
      super.setMetaData(QUEUE_SYSTEM_META_DATA_KEY, this.queueManager.getQueueSystemMetaData());
      
      super.dispatchMetaDataUpdateCommand();
   }
   
   /**
    * Called to get the meta data to be used for handshaking (queue synchronization) with a remote messaging manager.
    */
   protected HashMap initializeDestinationMetaData(final Destination destination)
   {
      HashMap destinationMetaData = super.initializeDestinationMetaData(destination);
      
      final RemoteQueueSystemDestination remoteQueueSystemDestination = (RemoteQueueSystemDestination)destination;
      
      if( destinationMetaData == null ) destinationMetaData = new HashMap();
      
      if( remoteQueueSystemDestination.isClientSide() ) // Client side
      {
         super.logInfo("Initiating client side handshaking/synchronization with " + remoteQueueSystemDestination + ".");
         
         // Generate synchronization request - but only if this queue manager has an out queue
         if( this.queueManager.getOutQueue() != null )
         {
            QueueSystemSynchronizationRequest currentQueueSystemSynchronizationRequest = this.queueManager.initiateQueueSystemSynchronization(null); // Get all //remoteQueueSystemDestination);
            remoteQueueSystemDestination.setQueueSystemSynchronizationRequest(currentQueueSystemSynchronizationRequest);
            destinationMetaData.put(QUEUE_SYSTEM_SYNCHRONIZATION_REQUEST_META_DATA_KEY, currentQueueSystemSynchronizationRequest);
         }
         
         if( super.isDebugMode() ) super.logDebug("Client side handshaking - getting contextual meta data (" + remoteQueueSystemDestination.getQueueSystemSynchronizationRequest() + "). " + remoteQueueSystemDestination + ".");
      }
      else // Server side
      {
         super.logInfo("Completing server side handshaking/synchronization with " + remoteQueueSystemDestination + ".");
         
         if( super.isDebugMode() ) super.logDebug("Server side handshaking - getting contextual meta data (" + remoteQueueSystemDestination.getQueueSystemSynchronizationResponse() + "). " + remoteQueueSystemDestination + ".");
         
         // Set synchronization response...
         destinationMetaData.put(QUEUE_SYSTEM_SYNCHRONIZATION_RESPONSE_META_DATA_KEY, remoteQueueSystemDestination.getQueueSystemSynchronizationResponse());
         
         // Mark queue system synchronization as complete
         remoteQueueSystemDestination.setSynchronizationComplete(true);
      }
      
      return destinationMetaData;
   }

   /**
    * Called when destination meta data is initialized.<br>
    * <br>
    * This method is is responsible for performing queue system synchronization during handshaking.
    */
   protected void destinationMetaDataInitialized(final Destination destination)
   {
      if( super.isDebugMode() ) super.logDebug("Destination meta data initialized for destination " + destination + ".");
      
      // Call super class implementation which invokes destinationMetaDataUpdated (which in turn
      // calls RemoteQueueSystemDestination.remoteQueueSystemMetaDataUpdated() )
      super.destinationMetaDataInitialized(destination);
      
      
      // Create queue system address
      String inQueueReceiverName = (String)destination.getDestinationMetaData(IN_QUEUE_SYSTEM_MESSAGE_RECEIVER_NAME_META_DATA_KEY);
      MessagingQueueSystemEndPointIdentifier messagingQueueSystemEndPointIdentifier = 
         new MessagingQueueSystemEndPointIdentifier(inQueueReceiverName, destination.getServerName());
      
      // Set remote queue system addesss
      RemoteQueueSystemDestination remoteQueueSystemDestination = (RemoteQueueSystemDestination)destination;
      remoteQueueSystemDestination.setRemoteQueueSystemAddress(messagingQueueSystemEndPointIdentifier);
      
          
      if( remoteQueueSystemDestination.isClientSide() ) // Client side
      {
         super.logInfo("Completing client side handshaking/synchronization with " + remoteQueueSystemDestination + ".");
         
         if( this.queueManager.getOutQueue() != null )
         {
            QueueSystemSynchronizationResponse queueSystemSynchronizationResponse = (QueueSystemSynchronizationResponse)remoteQueueSystemDestination.getDestinationMetaData(QUEUE_SYSTEM_SYNCHRONIZATION_RESPONSE_META_DATA_KEY);
            
            if( queueSystemSynchronizationResponse != null )
            {
               this.queueManager.queueSystemSynchronizationResponseReceived(remoteQueueSystemDestination, queueSystemSynchronizationResponse);
            }
            else // Error! QueueSystemSynchronizationResponse expected!
            {
               logCriticalError("Client side handshaking - meta data update - no QueueSystemSynchronizationResponse found in remote meta data - " + remoteQueueSystemDestination + "!", JServerConstants.LOG_MESSAGE_ID_CRITICAL_SUBSYSTEM_ERROR);
               // Throw exception to make the connection fail
               throw new RuntimeException("No QueueSystemSynchronizationResponse found in remote meta data!");
            }
         }
         
         // Mark queue system synchronization as complete
         remoteQueueSystemDestination.setSynchronizationComplete(true);
      }
      else // Server side
      {
         super.logInfo("Initiating server side handshaking/synchronization with " + remoteQueueSystemDestination + ".");
         
         QueueSystemSynchronizationResponse queueSystemSynchronizationResponse; 
         
         // Get synchronization request from destination meta data - but only if this queue manager has an in queue
         if( this.queueManager.getInQueue() != null )
         {
            QueueSystemSynchronizationRequest queueSystemSynchronizationRequest = (QueueSystemSynchronizationRequest)remoteQueueSystemDestination.getDestinationMetaData(QUEUE_SYSTEM_SYNCHRONIZATION_REQUEST_META_DATA_KEY);
            
            if( queueSystemSynchronizationRequest != null ) // If queueSystemSynchronizationRequest == null, no out queue exists in the remote queue system
            {
               // Perform synchronization and generate QueueSystemSynchronizationResponse
               queueSystemSynchronizationResponse = this.queueManager.queueSystemSynchronizationRequestReceived(remoteQueueSystemDestination, queueSystemSynchronizationRequest);
            }
            else // No QueueSystemSynchronizationRequest means that the client server had no out queue
            {
               if( super.isDebugMode() ) super.logDebug("Server side handshaking - meta data update - no QueueSystemSynchronizationRequest found in remote meta data." + remoteQueueSystemDestination + ".");
               queueSystemSynchronizationResponse = new QueueSystemSynchronizationResponse(this.queueManager.getQueueSystemMetaData());
            }
         }
         else queueSystemSynchronizationResponse = new QueueSystemSynchronizationResponse(this.queueManager.getQueueSystemMetaData());
         
         remoteQueueSystemDestination.setQueueSystemSynchronizationResponse(queueSystemSynchronizationResponse);
      }
      
      if( remoteQueueSystemDestination.getRemoteQueueSystemMetaData() != null ) remoteQueueSystemDestination.setExpectedRemoteInQueueLength( remoteQueueSystemDestination.getRemoteQueueSystemMetaData().getInQueueLength() );
   }
   
   
   /**
    * Called when destination meta data is updated.
    */
   protected void destinationMetaDataUpdated(final Destination destination, final HashMap previousDestinationMetaData)
   {
      super.destinationMetaDataUpdated(destination, previousDestinationMetaData);
      
      RemoteQueueSystemDestination remoteQueueSystemDestination = (RemoteQueueSystemDestination)destination;
      // Update queue system meta data
      remoteQueueSystemDestination.remoteQueueSystemMetaDataUpdated();
   }
   
   /**
    * Called when a link has been established with a new destination.
    */
   protected void destinationLinkEstablished(final Destination destination)
   {
      super.destinationLinkEstablished(destination);
      
      RemoteQueueSystemDestination remoteQueueSystemDestination = (RemoteQueueSystemDestination)destination;
      
      // Reset obsolete data
      remoteQueueSystemDestination.setQueueSystemSynchronizationRequest(null);
      remoteQueueSystemDestination.setQueueSystemSynchronizationResponse(null);
   }
   
   /**
    * Called when a link to a destination is lost.
    */
   protected void destinationLinkLost(Destination destination)
   {
      super.destinationLinkLost(destination);
      
      RemoteQueueSystemDestination remoteQueueSystemDestination = (RemoteQueueSystemDestination)destination;
      
      remoteQueueSystemDestination.destinationLinkLost();
   }
   
   /**
    * Called when a destination object is destroyed.
    *  
    * @since 2.1.2 (20060309)
    */
   protected void destinationDestroyed(final Destination destination)
   {
      ((RemoteQueueSystemDestination)destination).destinationDestroyed();
   }
   
   
   /* ### NESTED CLASSES ### */

   
   /**
    * Internal message receiver.
    */
   private static class DefaultQueueSystemMessageReceiver implements MessageReceiver
   {
      private final QueueMessagingManager queueMessagingManager;
      
      /**
       */
      public DefaultQueueSystemMessageReceiver(QueueMessagingManager queueMessagingManager)
      {
         this.queueMessagingManager = queueMessagingManager;
      }

      /**
       */
      public void messageReceived(final Message message)
      {
         this.queueMessagingManager.queueSystemMessageReceived(message);
      }
   }
   
   
   /**
    * Internal in queue message receiver.
    */
   private static class InQueueSystemMessageReceiver extends MessageReceiverComponent
   {
      private final QueueMessagingManager queueMessagingManager;
      
      /**
       */
      public InQueueSystemMessageReceiver(QueueMessagingManager queueMessagingManager, String receiverName)
      {
         super("QueueSystemMessageReceiver", receiverName);
         
         this.queueMessagingManager = queueMessagingManager;
      }

      /**
       */
      public void messageReceived(final Message message)
      {
         this.queueMessagingManager.queueSystemMessageReceived(message);
      }
   }
}
