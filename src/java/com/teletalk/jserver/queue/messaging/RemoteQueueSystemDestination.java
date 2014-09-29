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

import java.util.ArrayList;
import java.util.List;

import com.teletalk.jserver.comm.EndPointIdentifier;
import com.teletalk.jserver.queue.QueueSystemCollaborationManager;
import com.teletalk.jserver.queue.QueueSystemMetaData;
import com.teletalk.jserver.queue.RemoteQueueSystem;
import com.teletalk.jserver.queue.command.QueueSystemCommand;
import com.teletalk.jserver.queue.command.QueueSystemSynchronizationRequest;
import com.teletalk.jserver.queue.command.QueueSystemSynchronizationResponse;
import com.teletalk.jserver.tcp.TcpEndPointIdentifier;
import com.teletalk.jserver.tcp.messaging.Destination;
import com.teletalk.jserver.tcp.messaging.Message;
import com.teletalk.jserver.util.MessageQueueThread;

/**
 * Class representing a remote queue system destination.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1.2
 */
public class RemoteQueueSystemDestination extends Destination implements RemoteQueueSystem
{
   private final QueueMessagingManager queueMessagingManager;
   
   private MessagingQueueSystemEndPointIdentifier messagingQueueSystemEndPointIdentifier;
      
   
   private int expectedRemoteInQueueLength = 0;
   
   private volatile QueueSystemMetaData remoteQueueSystemMetaData;
   
   private boolean synchronizationComplete = false;
   
   
   private QueueSystemSynchronizationRequest queueSystemSynchronizationRequest = null;
   
   private QueueSystemSynchronizationResponse queueSystemSynchronizationResponse = null;
   
   
   private SequentialMessageHandler sequentialMessageHandler;
   
   private SequentialMessageDispatcher sequentialMessageDispatcher;
   

   /**
    * Creates a new RemoteQueueSystemDestination.
    */
   public RemoteQueueSystemDestination(final QueueMessagingManager queueMessagingManager, final TcpEndPointIdentifier address)
   {
      super(address);
      
      this.queueMessagingManager = queueMessagingManager;
      
      this.sequentialMessageHandler = new SequentialMessageHandler(this, queueMessagingManager);
      this.sequentialMessageDispatcher = new SequentialMessageDispatcher(this, queueMessagingManager);
   }
   
   /**
    * Called when the destination link is lost.
    * 
    * @since 2.1.2 (20060309)
    */
   public void destinationLinkLost()
   {
      if( this.sequentialMessageDispatcher != null ) this.sequentialMessageDispatcher.cancelCommands();
      this.setSynchronizationComplete(false); // Reset synchronizationComplete flag
   }
      
   /**
    * Called when the destination is destroyed.
    * 
    * @since 2.1.2 (20060309)
    */
   public void destinationDestroyed()
   {
      this.sequentialMessageHandler.destroy();
      this.sequentialMessageHandler = null;
      this.sequentialMessageDispatcher.destroy();
      this.sequentialMessageDispatcher = null;
   }
   
   /**
    * Called when a message is received. This method invokes an internal sequential message handler thread to handle the message in
    * {@link QueueMessagingManager#doQueueSystemMessageReceived(Message)}.
    * 
    * @since 2.1.2 (20060309)
    */
   protected void queueSystemMessageReceived(final Message message)
   {
      if( !this.sequentialMessageHandler.queueMessage(message) )
      {
         queueMessagingManager.logWarning("Unable to queue message " + message + " in sequentialMessageHandler! Handling message synchronously.");
         this.sequentialMessageHandler.handleMessage(message);
      }
   }
   
   /**
    * Called to dispatch a command to this remote queue system destination. This method invokes an internal sequential message 
    * dispatcher thread to dispatch the message in {@link QueueMessagingManager#dispatchCommandToRemoteQueueSystemDestination(QueueSystemCommand, RemoteQueueSystemDestination)}.
    * 
    * @since 2.1.2 (20060310)
    */
   protected void dispatchCommandToRemoteQueueSystemDestination(final QueueSystemCommand command)
   {
      if( !this.sequentialMessageDispatcher.queueMessage(command) )
      {
         queueMessagingManager.logWarning("Unable to queue command " + command + " in sequentialMessageDispatcher! Handling message synchronously.");
         this.sequentialMessageDispatcher.handleMessage(command);
      }
   }


   /**
    */
   QueueSystemSynchronizationRequest getQueueSystemSynchronizationRequest()
   {
      return queueSystemSynchronizationRequest;
   }
   
   /**
    */
   void setQueueSystemSynchronizationRequest(QueueSystemSynchronizationRequest queueSystemSynchronizationRequest)
   {
      this.queueSystemSynchronizationRequest = queueSystemSynchronizationRequest;
   }
   
   /**
    */
   QueueSystemSynchronizationResponse getQueueSystemSynchronizationResponse()
   {
      return queueSystemSynchronizationResponse;
   }
   
   /**
    */
   void setQueueSystemSynchronizationResponse(QueueSystemSynchronizationResponse queueSystemSynchronizationResponse)
   {
      this.queueSystemSynchronizationResponse = queueSystemSynchronizationResponse;
   }
   
   
   /**
    * Gets the flag indicating that queue synchronization is complete.
    */
   public synchronized boolean isSynchronizationComplete()
   {
      return synchronizationComplete;
   }


   /**
    * Sets the flag indicating that queue synchronization is complete.
    */
   public synchronized void setSynchronizationComplete(boolean synchronizationComplete)
   {
      this.synchronizationComplete = synchronizationComplete;
      
      this.notifyAll();
   }
   
   
   /**
    * Called when destination meta data is updated.
    */
   protected synchronized void remoteQueueSystemMetaDataUpdated()
   {
      QueueSystemMetaData metaData = (QueueSystemMetaData)super.getDestinationMetaData(QueueMessagingManager.QUEUE_SYSTEM_META_DATA_KEY);

      this.remoteQueueSystemMetaDataUpdated(metaData);
   }
   
   
   /* ### IMPLEMENTED METHODS FROM REMOTEQUEUESYSTEM: ### */
   
   
   /**
    * Called when a status command, containing metadata, is received from the remote queue system this 
    * object represents. This method should also be called when a link is first established with the queue system.
    */
   public synchronized void remoteQueueSystemMetaDataUpdated(QueueSystemMetaData metaData)
   {
      if(metaData != null)
      {
         if( (this.remoteQueueSystemMetaData != null) && (metaData.getExtraData() == null) ) // If piggybacked meta data (which only contains queue status...)
         {
            this.remoteQueueSystemMetaData.setInQueueBlocked(metaData.isInQueueBlocked());
            this.remoteQueueSystemMetaData.setInQueueLength(metaData.getInQueueLength());
            this.remoteQueueSystemMetaData.setinQueueMaxLength(metaData.getinQueueMaxLength());
         }
         else
         {
            this.remoteQueueSystemMetaData = metaData;
         }

         this.expectedRemoteInQueueLength = metaData.getInQueueLength();
      }      
   }
   
   
   /**
    * Cancels a command which that is to be delivered.<br>
    * <br>
    * This is an empty implementation that always returns false. 
    */
   public boolean cancelCommand(QueueSystemCommand command, boolean markAsSuccessful)
   {
      // Cancelling of dispatched commands not possible when using messaging
      return false;
   }

   /**
    * Cancels a command which that is to be delivered.<br>
    * <br>
    * This is an empty implementation that always returns false.
    */
   public boolean cancelCommand(QueueSystemCommand command)
   {
      // Cancelling of dispatched commands not possible when using messaging
      return false;
   }
   
   /**
    * Gets all the pending commands to the remote queue system.
    * 
    * @since 2.1.2 (20060227)
    */
   public List getPendningCommands()
   {
      return new ArrayList();
   }
   

   /**
    * Not implemented.
    */
   public void destroy()
   {
   }

   /**
    * Method to dispatch a command to the remote queue system this object represents.
    */
   public void dispatchCommand(QueueSystemCommand command)
   {
      this.queueMessagingManager.dispatchCommand(command, this);
   }

   /**
    * Gets a reference to the QueueSystemCollaborationManager associated with this RemoteQueueSystem.
    */
   public QueueSystemCollaborationManager getQueueSystemCollaborationManager()
   {
      return this.queueMessagingManager;
   }

   /**
    * Gets the address of the remote queue system represented by this object.
    */
   public synchronized EndPointIdentifier getRemoteQueueSystemAddress()
   {
      return this.messagingQueueSystemEndPointIdentifier;
   }
   
   /**
    * Sets the address of the remote queue system represented by this object.
    */
   public synchronized void setRemoteQueueSystemAddress(final MessagingQueueSystemEndPointIdentifier messagingQueueSystemEndPointIdentifier)
   {
      this.messagingQueueSystemEndPointIdentifier = messagingQueueSystemEndPointIdentifier;
   }

   /**
    * Gets metadata about this remote queue system.
    */
   public synchronized QueueSystemMetaData getRemoteQueueSystemMetaData()
   {
      return this.remoteQueueSystemMetaData;
   }
   
   
   /**
    * Gets the expected length of the in queue of the remote queue system represented by this object.<br>
    * <br>
    * This method uses an internal counter of how many items have been dispatched to the remote queue system to determine the 
    * expected size of the remote in queue. This counter will be updated with the actual size every time a status message is received.
    * 
    * @return the length of the remote in queue or -1 if no link has been established with the remote queue system this object represents.
    */
   public synchronized int getExpectedRemoteInQueueLength()
   {
      if( this.isLinkEstablished() )
      {
         return this.expectedRemoteInQueueLength;
      }
      else return -1;
   }
   
   /**
    * Increments the counter for expected remote in queue length.<br>
    * <br>
    * This method uses an internal counter of how many items have been dispatched to the remote queue system to determine the 
    * expected size of the remote in queue. This counter will be updated with the actual size every time a status message is received.
    */
   public synchronized void incrementExpectedRemoteInQueueLength()
   {
      this.expectedRemoteInQueueLength++;
   }
      
   /**
    * Sets the value of the counter for expected remote in queue length.<br>
    * <br>
    * This method uses an internal counter of how many items have been dispatched to the remote queue system to determine the 
    * expected size of the remote in queue. This counter will be updated with the actual size every time a status message is received.
    * 
    * @param length the new value.
    */
   public synchronized void setExpectedRemoteInQueueLength(int length)
   {
      this.expectedRemoteInQueueLength = length;
   }
   
   /**
    * Gets the length of the in queue of the remote queue system this DefaultQueueSystemEndPointProxy is connected to.
    * 
    * @return the length of the remote in queue or -1 if there is no link established with the remote queue system.
    */
   public synchronized int getRemoteInQueueLength()
   {
      if( this.isLinkEstablished() )
      {
         return getRemoteQueueSystemMetaData().getInQueueLength();
      }
      else return -1;
   }
   
   /**
    * Gets the maximum length of the in queue of the remote queue system represented by this object.
    * 
    * @return the maximum length of the remote in queue or -1 if there is no link established with the remote queue system.
    */
   public synchronized int getRemoteInQueueMaxLength()
   {
      if( this.isLinkEstablished() )
      {
         return getRemoteQueueSystemMetaData().getinQueueMaxLength();
      }
      else return -1;
   }
   
   /**
    * Checks if the in queue of the remote queue system represented by this object is expected to be full or not.<br>
    * <br>
    * This method uses an internal counter of how many items have been dispatched to the remote queue system to determine the 
    * expected size of the remote in queue. This counter will be updated with the actual size every time a status message is received.
    * 
    * @return <code>true</code> if the remote in queue is full, otherwise <code>false</code>. Note that the return value of this method 
    * will be <code>true</code> if there is no link established with the remote queue system.
    */
   public synchronized boolean isExpectedRemoteInQueueFull()
   {
      int max = this.getRemoteInQueueMaxLength();
      
      if( this.isLinkEstablished() )
      {
         if(max >= 0) return (this.getExpectedRemoteInQueueLength() >= max);
         else return false;
      }
      else return true;
   }
   
   /**
    * Checks if the in queue of the remote queue system represented by this object is full or not.
    * 
    * @return <code>true</code> if the remote in queue is full, otherwise <code>false</code>. Note that the return value of this method 
    * will be <code>true</code> if there is no link established with the remote queue system.
    */
   public synchronized boolean isRemoteInQueueFull()
   {
      int max = this.getRemoteInQueueMaxLength();
      
      if( this.isLinkEstablished() )
      {
         if(max >= 0) return (this.getRemoteInQueueLength() >= max);
         else return false;
      }
      else return true;
   }
   
   /**
    * Checks if the in queue of the remote queue system represented by this object is blocked or not.
    * 
    * @return <code>true</code> if the remote in queue is blocked, otherwise <code>false</code>. Note that the return value of this method 
    * will be <code>false</code> if there is no link established with the remote queue system.
    */
   public synchronized boolean isRemoteInQueueBlocked()
   {
      if( this.isLinkEstablished() )
      {
         return getRemoteQueueSystemMetaData().isInQueueBlocked();
      }
      else return false;
   }
   
   
   /* ### OVERRIDDEN METHODS FROM DESTINATION: ### */

   
   /**
    * Gets the load, which is equal to the expected in queue length.
    */
   public synchronized int getLoad()
   {
      return this.expectedRemoteInQueueLength;
   }
   
   
   /**
    * Checks if this endpoint group has at least one endpoint that has an established link.
    * 
    * @return <code>true</code> if at least one endpoint has an established link, otherwise <code>false</code>.
    */
   public synchronized boolean isLinkEstablished()
   {
      return super.isLinkEstablished() && this.synchronizationComplete;
   }
   
   /**
    * Checks whether or not a connection exists with the remote queue system this object represents. This method should 
    * return true if a connection exists even though final handshaking and synchronization may not be completed. 
    * 
    * @return true if a connection exists, otherwise false.
    */
   public boolean isConnected()
   {
      return super.isConnected();
   }
   
   
   /* ### ### */
      
   
   /**
    * Internal thread class for sequential handling of incomming messages.
    */
   private static final class SequentialMessageHandler extends MessageQueueThread
   {
      private final QueueMessagingManager queueMessagingManager;
      
      /**
       * Creates a new SequentialMessageHandler.
       */
      public SequentialMessageHandler(final RemoteQueueSystemDestination destination, final QueueMessagingManager queueMessagingManager)
      {
         super(queueMessagingManager.getFullName() + ".SequentialMessageHandler." + System.identityHashCode(destination));
         
         this.queueMessagingManager = queueMessagingManager;
      }
      
      /**
       * Handles a queued message. 
       */
      protected void handleMessage(final Object message)
      {
         this.queueMessagingManager.doQueueSystemMessageReceived((Message)message);
      }
   }
   
   
   /**
    * Internal thread class for sequential dispatching of outgoing messages.
    */
   private static final class SequentialMessageDispatcher extends MessageQueueThread
   {
      private final RemoteQueueSystemDestination destination;
      
      private final QueueMessagingManager queueMessagingManager;
      
      /**
       * Creates a new SequentialMessageDispatcher.
       */
      public SequentialMessageDispatcher(final RemoteQueueSystemDestination destination, final QueueMessagingManager queueMessagingManager)
      {
         super(queueMessagingManager.getFullName() + ".SequentialMessageDispatcher." + System.identityHashCode(destination));
         
         this.destination = destination;
         this.queueMessagingManager = queueMessagingManager;
      }
      
      /**
       */
      void cancelCommands()
      {
         QueueSystemCommand command;
         synchronized (super.queue.getLock())
         {
            while(super.queue.containsData())
            {
               command = (QueueSystemCommand)super.queue.getMsgIfAny();
               if( command != null )
               {
                  this.queueMessagingManager.getQueueManager().commandDeliveryReport(command, false);
               }
            }
         }
      }
      
      /**
       * Internal destroy method.
       */
      protected void doDestroy()
      {
         super.canRun = false;
         
         this.cancelCommands();
                  
         super.doDestroy();
      }
      
      /**
       * Handles a queued message. 
       */
      protected void handleMessage(final Object message)
      {
         this.queueMessagingManager.dispatchCommandToRemoteQueueSystemDestination((QueueSystemCommand)message, this.destination);
      }
   }
}
