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
package com.teletalk.jserver.queue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.teletalk.jserver.comm.EndPointIdentifier;
import com.teletalk.jserver.queue.command.MultiQueueItemTransferRequest;
import com.teletalk.jserver.queue.command.QueueItemCancellationRequest;
import com.teletalk.jserver.queue.command.QueueItemCompletionResponse;
import com.teletalk.jserver.queue.command.QueueItemQuery;
import com.teletalk.jserver.queue.command.QueueItemRelocationRequest;
import com.teletalk.jserver.queue.command.QueueItemResponse;
import com.teletalk.jserver.queue.command.QueueItemTransferRequest;
import com.teletalk.jserver.queue.command.QueueItemTransferResponse;
import com.teletalk.jserver.queue.command.QueueSystemCommand;
import com.teletalk.jserver.queue.command.QueueSystemSynchronizationRequest;
import com.teletalk.jserver.queue.command.QueueSystemSynchronizationResponse;
import com.teletalk.jserver.util.StringUtils;

/**
 * Baseclass of classes that implement internal mode dependent implementation of methods in QueueManager. This abstract
 * base class implements certain methods that are common to at least two subclasses.
 */
abstract class QueueManagerImplBase implements QueueManagerImpl
{
   final QueueManager queueManager;

   final InQueueController inController;

   final OutQueueController outController;
   

   /**
    * Creates a new QueueManagerImplBase.
    */
   protected QueueManagerImplBase(QueueManager queueManager, InQueueController inController)
   {
      this.queueManager = queueManager;

      this.inController = inController;
      this.outController = null;
   }

   /**
    * Creates a new QueueManagerImplBase.
    */
   protected QueueManagerImplBase(QueueManager queueManager, OutQueueController outController)
   {
      this.queueManager = queueManager;

      this.inController = null;
      this.outController = outController;
   }

   /**
    * Creates a new QueueManagerImpl.
    */
   protected QueueManagerImplBase(QueueManager queueManager, InOutQueueController inOutController)
   {
      this.queueManager = queueManager;

      this.inController = inOutController;
      this.outController = inOutController;
   }

   /**
    * performInQueueCheck
    */
   public ArrayList performInQueueCheck()
   {
      if (this.queueManager.isDebugMode()) this.queueManager.logDebug("Performing in queue check.");

      // Get all items in in queue
      QueueItem[] inItems = (QueueItem[]) this.queueManager.inQueue.getAllAsList().toArray(new QueueItem[] {});
      
      ArrayList availableSenders = new ArrayList();
      EndPointIdentifier address;

      for (int i = 0; i < inItems.length; i++)
      {
         if (inItems[i] != null)
         {
            performInQueueCheckForEach(inItems[i]);
            address = inItems[i].getSenderReceiverAddress();

            // Add address to list of senders...
            if ((address != null) && !availableSenders.contains(address)) availableSenders.add(address);
         }
      }

      if (this.queueManager.isDebugMode()) this.queueManager.logDebug("In queue check done.");

      return availableSenders;
   }

   /**
    * performInQueueCheckForEach
    */
   void performInQueueCheckForEach(QueueItem item)
   {
      item.setStatus(QueueItem.QUEUED); // ...set status to QUEUED.
   }

   /**
    * performOutQueueCheck
    */
   public ArrayList performOutQueueCheck(ArrayList responsesToExecute)
   {
      if (this.queueManager.isDebugMode()) this.queueManager.logDebug("Performing out queue check.");

      // Get all items in out queue
      QueueItem[] outItems = (QueueItem[]) this.queueManager.outQueue.getAllAsList().toArray(new QueueItem[] {});
      
      EndPointIdentifier address;
      ArrayList availableReceivers = new ArrayList();
      QueueItemResponse response;

      for (int i = 0; i < outItems.length; i++)
      {
         if (outItems[i] != null)
         {
            address = outItems[i].getSenderReceiverAddress();

            // Add address to list of receivers...
            if ((address != null) && !availableReceivers.contains(address)) availableReceivers.add(address);

            response = this.performOutQueueCheckForEach(outItems[i]);

            if (response != null) responsesToExecute.add(response);
         }
      }

      if (this.queueManager.isDebugMode()) this.queueManager.logDebug("Out queue check done.");

      return availableReceivers;
   }

   /**
    * performOutQueueCheckForEach
    */
   QueueItemResponse performOutQueueCheckForEach(QueueItem item)
   {
      int status = item.getStatus();

      // If item is completed...
      if (status == QueueItem.DONE_SUCCESS)
      {
         // Use standard handling for completed queue items
         return new QueueItemCompletionResponse(item.getSenderReceiverAddress(), item, QueueItemCompletionResponse.QUEUE_ITEM_DONE_SUCCESS, null);
      }
      else if (status == QueueItem.DONE_FAILURE)
      {
         // Use standard handling for completed queue items
         return new QueueItemCompletionResponse(item.getSenderReceiverAddress(), item, QueueItemCompletionResponse.QUEUE_ITEM_DONE_FAILURE, null);
      }
      else if (status == QueueItem.DONE_CANCELLED)
      {
         // Use standard handling for completed queue items
         return new QueueItemCompletionResponse(item.getSenderReceiverAddress(), item, QueueItemCompletionResponse.QUEUE_ITEM_DONE_CANCELLED, null);
      }
      else if (status == QueueItem.RELOCATION_REQUIRED)
      {
         // Use standard handling for completed queue items
         return new QueueItemCompletionResponse(item.getSenderReceiverAddress(), item, QueueItemCompletionResponse.QUEUE_ITEM_RELOCATION_REQUIRED, null);
      }
      else if (status == QueueItem.DISPATCH_FAILED)
      {
         // Use standard handling for queue items that failed to dispatch
         return new QueueItemTransferResponse(item.getSenderReceiverAddress(), item, QueueItemTransferResponse.QUEUE_ITEM_TRANSFER_FAILURE);
      }
      else if (status == QueueItem.DISPATCH_FAILED_QUEUE_FULL)
      {
         // Use standard handling for queue items that failed to dispatch due to full queue
         return new QueueItemTransferResponse(item.getSenderReceiverAddress(), item, QueueItemTransferResponse.QUEUE_ITEM_TRANSFER_QUEUE_FULL);
      }
      else if ((status != QueueItem.QUEUED) && (status != QueueItem.DISPATCHING) && (status != QueueItem.DISPATCHED))
      {
         item.setStatus(QueueItem.QUEUED);
      }
      return null;
   }

   /**
    * initiateQueueSystemSynchronization
    */
   public QueueSystemSynchronizationRequest initiateQueueSystemSynchronization(final RemoteQueueSystem rqs)
   {
      if (this.queueManager.isDebugMode()) 
      {
         if( rqs != null ) this.queueManager.logDebug("Initiating out queue synchronization with remote queue system at " + rqs.getRemoteQueueSystemAddress() + ".");
         else this.queueManager.logDebug("Initiating out queue synchronization with remote queue system.");
      }
      
      ArrayList items;
      if( rqs != null )
      {
         // Get all items that were dispatched to the specified RemoteQueueSystem
         items = this.queueManager.getOutItemsWithAddressAsList(rqs.getRemoteQueueSystemAddress());
   
         // Include all items with unknown receiver
         items.addAll(this.queueManager.getOutItemsWithAddressAsList(null));
      }
      else // Get all
      {
         items = new ArrayList();
         if( this.queueManager.getOutQueue() != null )
         {
            items.addAll(this.queueManager.getOutQueue().getAll());
         }
      }
      
      QueueSystemSynchronizationRequest queueSystemSynchronizationRequest = new QueueSystemSynchronizationRequest((QueueItem[]) items.toArray(new QueueItem[] {}));

      if (this.queueManager.isDebugMode()) 
      {
         if( rqs != null ) this.queueManager.logDebug("Initiating out queue synchronization with remote queue system at " + rqs.getRemoteQueueSystemAddress() + "  - returning request object: " + queueSystemSynchronizationRequest + ".");
         else this.queueManager.logDebug("Initiating out queue synchronization with remote queue system - returning request object: " + queueSystemSynchronizationRequest + ".");
      }
      return queueSystemSynchronizationRequest;
   }

   /**
    * Synchronize in queue with remote out queue
    */
   public QueueSystemSynchronizationResponse queueSystemSynchronizationRequestReceived(final RemoteQueueSystem rqs, final QueueSystemSynchronizationRequest synchRequest)
   {
      try
      {
         this.queueManager.trustCurrentThread();

         String[] ids = synchRequest.getIds();

         ArrayList idsList = new ArrayList(Arrays.asList(ids));
         
         if (this.queueManager.isDebugMode())
         {
            this.queueManager.logInfo("Synchronizing in queue with out queue in remote queue system at " + rqs.getRemoteQueueSystemAddress() + 
                  ". No of id:s to synchronize: " + ids.length + ". SynchRequest: " + synchRequest + ".");
         }
         else
         {
            this.queueManager.logInfo("Synchronizing in queue with out queue in remote queue system at " + rqs.getRemoteQueueSystemAddress() + 
                  ". No of id:s to synchronize: " + ids.length + ". ");
         }
                  
         short[] statuses = synchRequest.getStatuses();
         Queue inQueue = this.queueManager.getInQueue();
         QueueItem[] itemsFromAddress = this.queueManager.getInItemsWithAddress(rqs.getRemoteQueueSystemAddress());
         QueueItem[] unknownSenderItems = this.queueManager.getInItemsWithAddress(null);
         String id;
         int index;
         int removeCount = 0;
         int completionResponseCount = 0;

         // REMOVE ALL QUEUE ITEMS THAT WERE NOT INCLUDED IN THE TRANSFERRED IDS ARRAY
         if (this.queueManager.isDebugMode())
         {
            this.queueManager.logDebug("Synchronizing in queue with out queue in remote queue system at " + rqs.getRemoteQueueSystemAddress()
                  + " - removing items that have not been dispatched to local in queue.");
         }
         
         for (int i = 0; i < itemsFromAddress.length; i++)
         {
            if (itemsFromAddress[i] != null)
            {
               id = itemsFromAddress[i].getId();
               // Find and remove (just to make it quicker to search...) id from remote out queue id list:
               index = idsList.indexOf(id);

               if ((index < 0) || // If not found or if remote queue item was not dispatched
                     (statuses[index] != QueueItem.QUEUED) && (statuses[index] != QueueItem.DISPATCHING) && (statuses[index] != QueueItem.DISPATCHED))
               {
                  inQueue.remove(id);
                  removeCount++;

                  queueSystemSynchronizationRequestReceived_RemoveChildren(id);
               }
               else idsList.remove(index);
            }
         }

         if (removeCount > 0) // If at least one was removed...
         {
            // ...get items for address again
            itemsFromAddress = this.queueManager.getInItemsWithAddress(rqs.getRemoteQueueSystemAddress()); 
         }

         // CHECK IF SOME OF THE ITEMS WITH UNKNOWN SENDER ORIGINATED FROM THE SPECIFIED QUEUE SYSTEM
         if (this.queueManager.isDebugMode())
         {
            this.queueManager.logDebug("Synchronizing in queue with out queue in remote queue system at " + rqs.getRemoteQueueSystemAddress() + 
                  " - checking items with unknown sender.");
         }
         
         for (int i = 0; i<unknownSenderItems.length; i++)
         {
            if (unknownSenderItems[i] != null)
            {
               id = unknownSenderItems[i].getId();

               index = idsList.indexOf(id);

               if (index >= 0) // If the item with unknown sender was found...
               {
                  // ...and if remote queue item was not dispatched
                  if ((statuses[index] != QueueItem.QUEUED) && (statuses[index] != QueueItem.DISPATCHING) && (statuses[index] != QueueItem.DISPATCHED))
                  {
                     // ...remove it
                     inQueue.remove(id);
                     removeCount++;

                     queueSystemSynchronizationRequestReceived_RemoveChildren(id);
                  }
                  else
                  // ...else, set the sender address of the item to the address of the specified remote queue system
                  {
                     unknownSenderItems[i].setSenderReceiverAddress(rqs.getRemoteQueueSystemAddress());
                  }
               }
            }
         }

         
         // CHECK UNSENT COMPLETION RESPONSES...
         
         List completionResponses = this.queueManager.getUnsentCompletionResponsesForAddress(rqs.getRemoteQueueSystemAddress());
         
         if (this.queueManager.isDebugMode())
         {
            this.queueManager.logDebug("Synchronizing in queue with out queue in remote queue system at " + rqs.getRemoteQueueSystemAddress() + 
                  " - unsent responses for remote queue system: (" + StringUtils.toString(completionResponses) + ").");
         }
         
         
         // CHECK COMPLETION RESPONSE CACHE
         List recentCompletionsForAddress = this.queueManager.getRecentCompletionResponsesForAddress(rqs.getRemoteQueueSystemAddress());
         
         if (this.queueManager.isDebugMode())
         {
            this.queueManager.logDebug("Synchronizing in queue with out queue in remote queue system at " + rqs.getRemoteQueueSystemAddress() + 
                  " - recent completion responses for remote queue system: (" + StringUtils.toString(recentCompletionsForAddress) + ").");
         }
         
         
         // CHECK PENDING COMMANDS
         List pendingCommandsForAddress = rqs.getPendningCommands();
         ArrayList pendingCompletionResponsesForAddress = new ArrayList();
         if (pendingCommandsForAddress != null)
         {
            QueueItemCompletionResponse response;
            Object command;

            for (int i = 0; i < pendingCommandsForAddress.size(); i++)
            {
               command = pendingCommandsForAddress.get(i);
               if ((command != null) && (command instanceof QueueItemCompletionResponse))
               {
                  response = (QueueItemCompletionResponse) command;
                  pendingCompletionResponsesForAddress.add(response);
                  rqs.cancelCommand(response, true); // Cancel the pending response (but mark it as successful)
                  //unsentResponseCount++;
               }
            }
         }
         
         if (this.queueManager.isDebugMode())
         {
            this.queueManager.logDebug("Synchronizing in queue with out queue in remote queue system at " + rqs.getRemoteQueueSystemAddress() + 
                  " - pending commands for remote queue system: (" + StringUtils.toString(pendingCompletionResponsesForAddress) + ").");
         }
         
         
         // ONLY RETURN RESPONSES FOR THE SPECIFIED ID:s
         completionResponses.addAll(recentCompletionsForAddress);
         completionResponses.addAll(pendingCompletionResponsesForAddress);
                  
         ArrayList completionResponsesForAddress = new ArrayList();
         QueueItemCompletionResponse response;
         
         for(int i=0; i<completionResponses.size(); i++)
         {
            response = (QueueItemCompletionResponse)completionResponses.get(i);
            if( idsList.contains(response.getItemId()) )
            {
               completionResponsesForAddress.add(response);
               completionResponseCount++;
            }
         }

         this.queueManager.logInfo("Synchronization of in queue with out queue in remote queue system at " + rqs.getRemoteQueueSystemAddress() + " complete. " + itemsFromAddress.length
               + " items in the in queue originated from that queue system, " + removeCount + " items were removed from the in queue during synchronization and " + completionResponseCount
               + " unsent/pending/lost completion responses were found.");

         return new QueueSystemSynchronizationResponse(itemsFromAddress, (QueueItemCompletionResponse[]) completionResponsesForAddress.toArray(new QueueItemCompletionResponse[] {}),
               this.queueManager.getQueueSystemMetaData());
      }
      finally
      {
         this.queueManager.distrustCurrentThread();
      }
   }

   /**
    * queueSystemSynchronizationRequestReceived_RemoveChildren
    */
   void queueSystemSynchronizationRequestReceived_RemoveChildren(String id)
   {
   }

   /**
    * Synchronize out queue with remote in queue
    */
   public void queueSystemSynchronizationResponseReceived(RemoteQueueSystem rqs, QueueSystemSynchronizationResponse synchResponse)
   {
      try
      {
         this.queueManager.trustCurrentThread();

         if (this.queueManager.isDebugMode())
         {
            this.queueManager.logInfo("Synchronizing out queue with in queue in remote queue system at " + rqs.getRemoteQueueSystemAddress() + 
                  " - " + synchResponse + ".");
         }
         else
         {
            this.queueManager.logInfo("Synchronizing out queue with in queue in remote queue system at " + rqs.getRemoteQueueSystemAddress() + ".");
         }
         
         int dispatchedCount = 0;
         int queuedCount = 0;
         int removedCount = 0;
         String[] queuedItemIds = synchResponse.getIds();
         QueueItemCompletionResponse[] completionResponses = synchResponse.geCompletionResponse();
         final ArrayList responsesToExecute = new ArrayList();

         ArrayList idsList = new ArrayList(Arrays.asList(queuedItemIds));
         Queue outQueue = this.queueManager.getOutQueue();
         
         QueueItem[] itemsToAddress = this.queueManager.getOutItemsWithAddress(rqs.getRemoteQueueSystemAddress());
         QueueItem[] unknownReceiverItems = this.queueManager.getOutItemsWithAddress(null);
         List pendingCommandsForAddress = rqs.getPendningCommands();
         String id;
         int index;
         QueueItemTransferRequest transferRequest;
         boolean pendingTransferRequest;

         // Add completion response ids to list of ids
         for (int i = 0; i < completionResponses.length; i++)
         {
            idsList.add(completionResponses[i].getItemId());

            // Add to list of completion responses to execute
            responsesToExecute.add(completionResponses[i]);
         }

         // MATCH ITEMS IN REMOTE IN QUEUE WITH ITEMS IN OUT QUEUE
         if (this.queueManager.isDebugMode())
         {
            this.queueManager.logDebug("Synchronizing out queue with in queue in remote queue system at " + rqs.getRemoteQueueSystemAddress()
                  + " - matching items in remote in queue with items in local out queue.");
         }
         
         for (int i = 0; i<itemsToAddress.length; i++)
         {
            if (itemsToAddress[i] != null)
            {
               id = itemsToAddress[i].getId();
               index = idsList.indexOf(id);

               // CHECK PENDING COMMANDS TO SEE IF A REQUEST TO TRANSFER THE VERY SAME ITEM IS PENDING
               transferRequest = checkForPendingTransferRequestForId(pendingCommandsForAddress, id);
               pendingTransferRequest = (transferRequest != null);

               if (index >= 0) // Item has been dispatched to remote queue system...
               {
                  itemsToAddress[i].setStatus(QueueItem.DISPATCHED); // ...set status to DISPATCHED
                  dispatchedCount++;

                  // If pending transfer request...
                  if (pendingTransferRequest)
                  {
                     rqs.cancelCommand(transferRequest, true); // Cancel the pending request (but mark it as successful)
                  }

                  idsList.remove(index);
               }
               else if (!pendingTransferRequest) // Else if not dispatched and no transfer request is pending
               {
                  QueueItem item = outQueue.get(id);
                  // Only handle as failed if dispatched or dispatching!
                  if ( (item != null) && ((item.getStatus() == QueueItem.DISPATCHING) || (item.getStatus() == QueueItem.DISPATCHED)) )
                  {
                     // Use standard handling for queue items that failed to dispatch
                     responsesToExecute.add(new QueueItemTransferResponse(item.getSenderReceiverAddress(), item, QueueItemTransferResponse.QUEUE_ITEM_TRANSFER_FAILURE));
                  }
               }
            }
         }

         // CHECK IF SOME OF THE ITEMS WITH UNKNOWN RECEIVER WERE DISPATCHED TO THE SPECIFIED QUEUE SYSTEM
         if (this.queueManager.isDebugMode())
         {            
            this.queueManager.logDebug("Synchronizing out queue with in queue in remote queue system at " + rqs.getRemoteQueueSystemAddress() + 
                  " - checking items with unknown receiver.");
         }
         
         for (int i = 0; i<unknownReceiverItems.length; i++)
         {
            if (unknownReceiverItems[i] != null)
            {
               id = unknownReceiverItems[i].getId();
               index = idsList.indexOf(id);

               if (index >= 0) // Item has been dispatched to remote queue system...
               {
                  // CHECK PENDING COMMANDS TO SEE IF A REQUEST TO TRANSFER THE VERY SAME ITEM IS PENDING
                  transferRequest = checkForPendingTransferRequestForId(pendingCommandsForAddress, id);
                  pendingTransferRequest = (transferRequest != null);

                  unknownReceiverItems[i].setSenderReceiverAddress(rqs.getRemoteQueueSystemAddress()); // Set the address to
                  // which the QueueItem was dispatched
                  unknownReceiverItems[i].setStatus(QueueItem.DISPATCHED); // ...set status to DISPATCHED
                  dispatchedCount++;

                  // If pending transfer request...
                  if (pendingTransferRequest)
                  {
                     rqs.cancelCommand(transferRequest, true); // Cancel the pending request (but mark it as successful)
                  }
               }
            }
         }

         // EXECUTE ALL TRANSFERRED COMPLETION RESPONSES (IN A SEPARATE THREAD THAT WAITS FOR THE QUEUEMANAGER TO GET
         // ENGAGED)
         if (this.queueManager.isDebugMode())
         {
            this.queueManager.logDebug("Synchronizing out queue with in queue in remote queue system at " + rqs.getRemoteQueueSystemAddress() + " - executing transferred completion responses.");
         }
         
         final Thread fireAndForget = new Thread("queueSystemSynchronizationResponseReceived command execution thread")
         {
            public void run()
            {
               try
               {
                  queueManager.trustCurrentThread();

                  // Wait for queuemanager to get enabled first (if it isn't already)
                  try
                  {
                     queueManager.waitForEnabled(60 * 60 * 1000);
                  }
                  catch (InterruptedException ie)
                  {
                  }

                  for (int i = 0; i < responsesToExecute.size(); i++)
                  {
                     ((QueueSystemCommand) responsesToExecute.get(i)).execute(queueManager);
                  }
               }
               catch (Exception e)
               {
                  queueManager.logError("Error occurred while executing responses during queue system synchronization.", e);
               }
               finally
               {
                  queueManager.distrustCurrentThread();
               }
            }
         };

         fireAndForget.setDaemon(true);
         fireAndForget.start();

         this.queueManager.logInfo("Synchronization of out queue with in queue in remote queue system at " + rqs.getRemoteQueueSystemAddress() + " complete. "
               + (dispatchedCount - completionResponses.length) + " items in the out queue were marked as dispatched, " + completionResponses.length + " as completed, " + queuedCount
               + " as queued and " + removedCount + " items were removed.");
      }
      finally
      {
         this.queueManager.distrustCurrentThread();
      }
   }

   /**
    * checkForPendingTransferRequestForId
    */
   QueueItemTransferRequest checkForPendingTransferRequestForId(List pendingCommands, String id)
   {
      QueueItemTransferRequest transferRequest;
      Object command;

      if (pendingCommands != null)
      {
         for (int i = 0; i < pendingCommands.size(); i++)
         {
            command = pendingCommands.get(i);
            if ((command != null) && (command instanceof QueueItemTransferRequest))
            {
               transferRequest = (QueueItemTransferRequest) command;
               if (transferRequest.getItemId().equals(id)) // Transfer request pending
               {
                  return transferRequest;
               }
            }
         }
      }
      return null;
   }

   /**
    * recoveredItemsInInQueue
    */
   public void recoveredItemsInInQueue()
   {
      inController.recoveredItemsInInQueue();
   }

   /**
    * recoveredItemsInOutQueue
    */
   public void recoveredItemsInOutQueue()
   {
      outController.recoveredItemsInOutQueue();
   }

   /**
    * createOutgoingQueueItems
    */
   public QueueItem[] createOutgoingQueueItems(final QueueItemData[] itemData, final QueueItem parentItem, boolean add) throws QueueStorageException
   {
      final QueueItem[] qItems = new QueueItem[itemData.length];

      for (int i = 0; i < itemData.length; i++)
      {
         QueueItem qItem;

         if (parentItem != null) qItem = new QueueItem(itemData[i], this.queueManager.getUniqueId(), parentItem.getId());
         else qItem = new QueueItem(itemData[i], this.queueManager.getUniqueId());

         qItems[i] = qItem;
      }

      synchronized (this.queueManager.getQueueItemsLock())
      {
         if (parentItem != null)
         {
            parentItem.setChildCount(itemData.length + parentItem.getChildCount());
         }

         if (add) this.queueManager.outQueue.add(qItems);
      }

      return qItems;
   }

   /**
    * createOutgoingQueueItem
    */
   public QueueItem createOutgoingQueueItem(final QueueItemData itemData, final QueueItem parentItem, boolean add) throws QueueStorageException
   {
      QueueItem qItem;

      if (parentItem != null)
      {
         qItem = new QueueItem(itemData, this.queueManager.getUniqueId(), parentItem.getId());
         parentItem.incrementChildCount();
      }
      else qItem = new QueueItem(itemData, this.queueManager.getUniqueId());

      if (add) this.queueManager.outQueue.add(qItem);

      return qItem;
   }

   /**
    * dispatchQueueItem
    */
   public void dispatchQueueItem(QueueItem qItem, final EndPointIdentifier address) throws QueueStorageException
   {
      QueueItemTransferRequest command = new QueueItemTransferRequest(address, qItem);

      synchronized (this.queueManager.getQueueItemsLock())
      {
         qItem.incrementDispatchCount();
         qItem.setSenderReceiverAddress(address);
         qItem.setSendReceiveTime(System.currentTimeMillis());
         qItem.forceStatus(QueueItem.DISPATCHING);
         
         if (!this.queueManager.outQueue.contains(qItem)) this.queueManager.outQueue.add(qItem);
         else this.queueManager.outQueue.updatePersistentStorage(qItem);
      }

      this.queueManager.dispatchCommand(command);
   }

   /**
    * dispatchQueueItems
    */
   public void dispatchQueueItems(QueueItem[] qItems, final EndPointIdentifier address) throws QueueStorageException
   {
      MultiQueueItemTransferRequest command = new MultiQueueItemTransferRequest(address, qItems);

      long currentTime = System.currentTimeMillis();

      synchronized (this.queueManager.getQueueItemsLock())
      {
         for (int i = 0; i < qItems.length; i++)
         {
            qItems[i].incrementDispatchCount();
            qItems[i].setSenderReceiverAddress(address);
            qItems[i].setSendReceiveTime(currentTime);
            qItems[i].forceStatus(QueueItem.DISPATCHING);

            // This could perhaps be done a bit better (given that Queue/QueueStorage supports methods that operate on
            // multiple QueueItems)
            if (!this.queueManager.outQueue.contains(qItems[i])) this.queueManager.outQueue.add(qItems[i]);
            else this.queueManager.outQueue.updatePersistentStorage(qItems[i]);
         }
      }

      this.queueManager.dispatchCommand(command);
   }

   /**
    * dispatchQueueItemQuery
    */
   public void dispatchQueueItemQuery(QueueItem outQueueItem)
   {
      if (outQueueItem.getSenderReceiverAddress() != null) this.queueManager.dispatchCommand(new QueueItemQuery(outQueueItem.getSenderReceiverAddress(), outQueueItem.getId()));
      else this.queueManager.logWarning("Unable to dispatch QueueItemQuery for item " + outQueueItem + " - sender/receiver address of QueueItem is null! The QueueItem may not have been dispatched.");
   }

   /**
    * isInItemCompleted
    */
   public abstract boolean isInItemCompleted(final QueueItem inItem);

   /**
    * inItemDone
    */
   public void inItemDone(final QueueItem inItem, final byte responseType, final Object responseData)
   {
      this.queueManager.inQueue.remove(inItem);

      if (responseData != null) this.queueManager.dispatchQueueItemCompletionResponse(inItem, responseType, responseData);
      else this.queueManager.dispatchQueueItemCompletionResponse(inItem, responseType);
   }

   /**
    * cancelInItem
    */
   public abstract void cancelInItem(final QueueItem inItem);

   /**
    * cancelOutItem
    */
   public void cancelOutItem(final QueueItem outItem)
   {
      if (outItem != null)
      {
         if (outItem == null) return;

         final int status = outItem.getStatus();

         if ((status != QueueItem.DISPATCHING) && (status != QueueItem.DISPATCHED))
         {
            // Use standard completion handling
            new QueueItemCompletionResponse(outItem.getSenderReceiverAddress(), outItem, QueueItemCompletionResponse.QUEUE_ITEM_DONE_CANCELLED, null).execute(this.queueManager);
         }
         else
         {
            this.queueManager.dispatchCommand(new QueueItemCancellationRequest(outItem.getSenderReceiverAddress(), outItem.getId()));
         }
      }
   }

   /**
    * relocateInItem
    */
   public abstract void relocateInItem(final QueueItem inItem);

   /**
    * relocateOutItem
    */
   public void relocateOutItem(QueueItem outItem)
   {
      if (outItem != null)
      {
         if (outItem == null) return;

         final int status = outItem.getStatus();

         if ((status != QueueItem.DISPATCHING) && (status != QueueItem.DISPATCHED))
         {
            // Use standard completion handling
            new QueueItemCompletionResponse(outItem.getSenderReceiverAddress(), outItem, QueueItemCompletionResponse.QUEUE_ITEM_RELOCATION_REQUIRED, null).execute(this.queueManager);
         }
         else
         {
            this.queueManager.dispatchCommand(new QueueItemRelocationRequest(outItem.getSenderReceiverAddress(), outItem.getId()));
         }
      }
   }

   /**
    * forceOutItemDone
    */
   public void forceOutItemDone(QueueItem outItem, byte responseType, Object responseData)
   {
      // Use standard handling for completed queue items
      new QueueItemCompletionResponse(outItem.getSenderReceiverAddress(), outItem, responseType, responseData).execute(this.queueManager);
   }

   /**
    * queueItemTransferRequestReceived
    */
   public void queueItemTransferRequestReceived(final QueueItemTransferRequest request)
   {
      final QueueItem item = request.getQueueItem();

      try
      {
         /* This won't work very well if queue items are reused...
         QueueItemCompletionResponse completionResponse = this.queueManager.getRecentCompletionResponse(item.getId());
         if( completionResponse != null )
         {
            this.queueManager.logInfo("Found completion response when attempting to add queue item (" + item + "). Returning response (" + completionResponse + ").");
            this.queueManager.dispatchCommand(completionResponse);
         }
         else
         {*/
            if (this.queueManager.inQueue.isEnabled())
            {
               boolean canAdd = false;
               boolean added = false;
               
               synchronized (this.queueManager.getQueueItemsLock())
               {
                  int maxSize = this.queueManager.inQueueMaxSize.intValue();
   
                  canAdd = (maxSize < 0) || (maxSize > 0 && (maxSize > this.queueManager.inQueue.size()));
   
                  if (canAdd)
                  {
                     added = this.queueManager.inQueue.add(item, false);
                  }
               }
                              
               if ( canAdd )
               {
                  this.queueManager.dispatchQueueItemTransferResponse(item, QueueItemTransferResponse.QUEUE_ITEM_TRANSFER_SUCCESS);
                  
                  if( added )
                  {
                     // Add the item to the list of queued items and notify threads that are waiting for queued objects. 
                     // Note that this is performed after the transfer response has been dispatched.
                     this.queueManager.inQueue.addToQueuedList(item);
                     
                     this.inController.newInItem(item);
                  }
               }
               else // If queue full
               {
                  if (this.queueManager.isDebugMode()) this.queueManager.logDebug("Unable to add item " + item.getId() + " to in queue - in queue full!");
                  this.queueManager.dispatchQueueItemTransferResponse(item, QueueItemTransferResponse.QUEUE_ITEM_TRANSFER_QUEUE_FULL);
               }
            }
            else
            {
               this.queueManager.logWarning("Unable to add item " + item.getId() + " to in queue - in queue disabled.");
               this.queueManager.dispatchQueueItemTransferResponse(item, QueueItemTransferResponse.QUEUE_ITEM_TRANSFER_FAILURE);
            }
         /*}*/
      }
      catch (Throwable e)
      {
         this.queueManager.inQueue.notifyWaitingThreads();
         this.queueManager.logError("Unable to add item " + item.getId() + " to in queue.", e);
         this.queueManager.dispatchQueueItemTransferResponse(item, QueueItemTransferResponse.QUEUE_ITEM_TRANSFER_FAILURE);
         if (e instanceof Error) throw (Error) e;
      }
   }

   /**
    * multiQueueItemTransferRequestReceived
    */
   public void multiQueueItemTransferRequestReceived(final MultiQueueItemTransferRequest request)
   {
      QueueItem[] items = request.getQueueItems();

      try
      {
         /* This won't work very well if queue items are reused...
         QueueItemCompletionResponse completionResponse;
         ArrayList newItemsList = new ArrayList(); 
         for (int i = 0; i<items.length; i++)
         {
            completionResponse = this.queueManager.getRecentCompletionResponse(items[i].getId());
            if( completionResponse != null )
            {
               this.queueManager.logInfo("Found completion response when attempting to add queue items. Returning response (" + completionResponse + ").");
               this.queueManager.dispatchCommand(completionResponse);
            }
            else newItemsList.add(items[i]);
         }
         
         if( newItemsList.size() != items.length )
         {
            items = (QueueItem[])newItemsList.toArray(new QueueItem[newItemsList.size()]); 
         }
         
         if( items.length > 0 )
         {*/
            if (this.queueManager.inQueue.isEnabled())
            {
               int numberOfItemsToAdd = 0;
               QueueItem[] itemsToAdd = null;
               boolean[] addResult = null;
               
               // Calculate number of items that can be added
               synchronized (this.queueManager.getQueueItemsLock())
               {
                  int maxSize = this.queueManager.inQueueMaxSize.intValue();
                  int size = this.queueManager.inQueue.size();
   
                  if (maxSize < 0)
                  {
                     numberOfItemsToAdd = items.length;
                  }
                  else if (maxSize > 0 && (maxSize > size))
                  {
                     numberOfItemsToAdd = Math.min((maxSize - size), items.length);
                  }
   
                  if ((numberOfItemsToAdd > 0) && (numberOfItemsToAdd < items.length))
                  {
                     itemsToAdd = new QueueItem[numberOfItemsToAdd];
                     System.arraycopy(items, 0, itemsToAdd, 0, numberOfItemsToAdd);
                  }
                  else if (numberOfItemsToAdd > 0)
                  {
                     numberOfItemsToAdd = items.length;
                     itemsToAdd = items;
                  }
   
                  // Add items...
                  if (numberOfItemsToAdd > 0)
                  {
                     addResult = this.queueManager.inQueue.add(itemsToAdd, false); // Add, but don't notify
                  }
               }
               
               if (numberOfItemsToAdd > 0)
               {
                  this.queueManager.dispatchQueueItemTransferResponse(itemsToAdd, QueueItemTransferResponse.QUEUE_ITEM_TRANSFER_SUCCESS);
                  
                  ArrayList addedItems = new ArrayList();
                  for(int i=0; i<addResult.length; i++)
                  {
                     if( addResult[i] ) addedItems.add(itemsToAdd[i]);
                  }
                  
                  // Add the items to the list of queued items and notify threads that are waiting for queued objects. 
                  // Note that this is performed after the transfer response has been dispatched.
                  this.queueManager.inQueue.addToQueuedList((QueueItem[])addedItems.toArray(new QueueItem[addedItems.size()]));
                  
                  for (int i = 0; i < itemsToAdd.length; i++) this.inController.newInItem(itemsToAdd[i]);
               }
   
               // If there where items that cound no be added (didn't fit in the in queue)...
               if (numberOfItemsToAdd < items.length)
               {
                  QueueItem[] itemsNotAdded = new QueueItem[items.length - numberOfItemsToAdd];
                  System.arraycopy(items, numberOfItemsToAdd, itemsNotAdded, 0, itemsNotAdded.length);
   
                  if (this.queueManager.isDebugMode()) this.queueManager.logDebug("Unable to add items (" + QueueItem.concatIds(itemsNotAdded) + ") to in queue - in queue full!");
                  this.queueManager.dispatchQueueItemTransferResponse(itemsNotAdded, QueueItemTransferResponse.QUEUE_ITEM_TRANSFER_QUEUE_FULL);
               }
            }
            else
            {
               this.queueManager.logWarning("Unable to add items " + QueueItem.concatIds(items) + " to in queue - in queue disabled.");
               this.queueManager.dispatchQueueItemTransferResponse(items, QueueItemResponse.QUEUE_ITEM_TRANSFER_FAILURE);
            }
         /*}*/
      }
      catch (Throwable e)
      {
         this.queueManager.inQueue.notifyWaitingThreads();
         this.queueManager.logError("Unable to add items " + QueueItem.concatIds(items) + " to in queue", e);
         this.queueManager.dispatchQueueItemTransferResponse(items, QueueItemResponse.QUEUE_ITEM_TRANSFER_FAILURE);
         if (e instanceof Error) throw (Error) e;
      }
   }

   /**
    * queueItemTransferRequestAborted
    */
   public void queueItemTransferRequestAborted(final QueueItem item)
   {
      this.queueManager.outQueue.changeStatus(item, QueueItem.DISPATCH_FAILED);
      this.outController.unableToDispatchOutItem(item);
   }

   /**
    * queueItemTransferred
    */
   public void queueItemTransferred(final String outItemId, final QueueItem item)
   {
      if (item != null)
      {
         this.queueManager.outQueue.changeStatus(item, QueueItem.DISPATCHED);
         this.outController.outItemDispatched(item);
      }
      else this.queueManager.logWarning("Error while receiving item transfer success response - item " + outItemId + " not found in out queue.");
   }

   /**
    * queueItemTransferFailure
    */
   public void queueItemTransferFailure(final String outItemId, final QueueItem item)
   {
      if (item != null)
      {
         this.queueManager.outQueue.changeStatus(item, QueueItem.DISPATCH_FAILED);

         item.decrementDispatchCount();

         this.outController.unableToDispatchOutItem(item);
      }
      else this.queueManager.logWarning("Error while receiving item transfer failure response - item " + outItemId + " not found in out queue.");
   }

   /**
    * queueItemTransferFailureQueueFull
    */
   public void queueItemTransferFailureQueueFull(final String outItemId, final QueueItem item)
   {
      if (item != null)
      {
         this.queueManager.outQueue.changeStatus(item, QueueItem.DISPATCH_FAILED_QUEUE_FULL);

         item.decrementDispatchCount();

         this.outController.unableToDispatchOutItemQueueFull(item);
      }
      else this.queueManager.logWarning("Error while receiving item transfer queue full response - item " + outItemId + " not found in out queue.");
   }

   /**
    * queueItemTransferResponseAborted
    */
   public void queueItemTransferResponseAborted(final String outItemId, final QueueItem item)
   {
      this.inController.inItemAborted(item);

      if (this.queueManager.removeAbortedItems) this.queueManager.inQueue.remove(item);
   }

   /**
    */
   public void queueItemQueryReceived(QueueItemQuery query)
   {
      /*
       * final String itemId = query.getItemId(); final EndPointIdentifier sender = query.getAddress();
       */

      // NOT IMPLEMENTED
      // ...Do something else - return status... (QueueItemQueryResponse...)
   }

   /**
    * queueItemCompletedSuccess
    */
   public QueueItem queueItemCompletedSuccess(final QueueItemCompletionResponse response)
   {
      final String outItemId = response.getItemId();
      final Object responseData = response.getResponseData();

      final QueueItem item = this.queueManager.outQueue.get(outItemId);

      if (item != null)
      {
         this.queueManager.outQueue.changeStatus(item, QueueItem.DONE_SUCCESS);
         this.outController.outItemDoneSuccess(item, responseData);

         if (this.queueManager.removeCompletedOutQueueItems) this.queueManager.outQueue.remove(outItemId);

         return item;
      }
      else
      {
         this.queueManager.logWarning("Error while receiving item completed (success) response - item " + outItemId + " not found in out queue!");

         return null;
      }
   }

   /**
    * queueItemCompletedFailure
    */
   public QueueItem queueItemCompletedFailure(final QueueItemCompletionResponse response)
   {
      final String outItemId = response.getItemId();
      final Object responseData = response.getResponseData();

      final QueueItem item = this.queueManager.outQueue.get(outItemId);

      if (item != null)
      {
         this.queueManager.outQueue.changeStatus(item, QueueItem.DONE_FAILURE);
         this.outController.outItemDoneFailure(item, responseData);

         if (this.queueManager.removeCompletedOutQueueItems) this.queueManager.outQueue.remove(outItemId);

         return item;
      }
      else
      {
         this.queueManager.logWarning("Error while receiving item completed (failure) response - item " + outItemId + " not found in out queue!");

         return null;
      }
   }

   /**
    * queueItemCompletedCancelled
    */
   public QueueItem queueItemCompletedCancelled(final QueueItemCompletionResponse response)
   {
      final String outItemId = response.getItemId();
      final Object responseData = response.getResponseData();

      final QueueItem item = this.queueManager.outQueue.get(outItemId);

      if (item != null)
      {
         this.queueManager.outQueue.changeStatus(item, QueueItem.DONE_CANCELLED);
         this.outController.outItemDoneCancelled(item, responseData);

         if (this.queueManager.removeCompletedOutQueueItems) this.queueManager.outQueue.remove(outItemId);

         return item;
      }
      else
      {
         this.queueManager.logWarning("Error while receiving item completed (cancelled) response - item " + outItemId + " not found in out queue!");

         return null;
      }
   }

   /**
    * queueItemRelocationRequiredResponseReceived
    */
   public void queueItemRelocationRequiredResponseReceived(final QueueItemCompletionResponse response)
   {
      final String outItemId = response.getItemId();
      final Object responseData = response.getResponseData();

      QueueItem item = this.queueManager.outQueue.get(outItemId);

      if (item != null)
      {
         this.queueManager.outQueue.changeStatus(item, QueueItem.RELOCATION_REQUIRED);
         this.outController.outItemRelocationRequestReceived(item, responseData);
      }
      else this.queueManager.logWarning("Error while receiving item relocation request - item " + outItemId + " not found in out queue.");
   }

   /**
    * queueItemCompletionResponseAborted
    */
   public void queueItemCompletionResponseAborted(final QueueItemCompletionResponse command)
   {
      this.queueManager.logWarning("Unable to send QueueItemCompletionResponse: " + command.toString() + ".");

      this.queueManager.addUnsentCompletionResponse(command);

      final QueueItem inItem = command.getItem();

      this.inController.unableToDispatchCompletionResponse(inItem);
   }

   /**
    * queueItemCancellationRequest
    */
   public void queueItemCancellationRequest(final QueueItemCancellationRequest request)
   {
      final String inItemId = request.getItemId();
      final QueueItem inItem = this.queueManager.inQueue.get(inItemId);

      if (inItem != null)
      {
         this.queueManager.cancelInItem(inItem);
      }
      else this.queueManager.logWarning("Error while receiving item cancellation request - item " + inItemId + " not found in in queue.");
   }

   /**
    * queueItemRelocationRequest
    */
   public void queueItemRelocationRequest(QueueItemRelocationRequest request)
   {
      final String inItemId = request.getItemId();
      final QueueItem inItem = this.queueManager.inQueue.get(inItemId);

      if (inItem != null)
      {
         this.queueManager.relocateInItem(inItem);
      }
      else this.queueManager.logWarning("Error while receiving item relocation request - item " + inItemId + " not found in in queue.");
   }

   /**
    * outItemAgeWarning
    */
   public void outItemAgeWarning(final QueueItem outItem)
   {
      if (outItem.getAgeLimitWarningCount() == 0) // Only notify controller on the first warning
         this.outController.outItemAgeWarning(outItem);
   }

   /**
    * performOutItemCheck
    */
   public void performOutItemCheck()
   {
      Iterator it = this.queueManager.outQueue.iterator();
      QueueItem item;
      long ageLimit = this.queueManager.outItemWarningAgeLimit.longValue();
      boolean checkAge = this.queueManager.checkOutQueueItemAge;

      while (it.hasNext())
      {
         item = (QueueItem) it.next();

         if (item != null)
         {
            if (item.getStatus() == QueueItem.DISPATCHING)
            {
               EndPointIdentifier ep = item.getSenderReceiverAddress();
               boolean checkOk = true;
               
               RemoteQueueSystem remoteQueueSystem = this.queueManager.getRemoteQueueSystem(ep, false);
               
               // Check if the item has been dispatched to a remote queue system to which the connection is down, 
               // and connection hasn't been reestablished withing a reasonable time (one minute)
               if ( ( (remoteQueueSystem == null) || !remoteQueueSystem.isConnected() ) && 
                     ( (System.currentTimeMillis() - item.getSendReceiveTime()) > (60 * 1000) ) )
               {
                  this.queueManager.logInfo("QueueItem (" + item.toString() + ") in state DISPATCHING and dispatched to a non existent remote queue system. Item will be marked as DISPATCH_FAILED.");
                  checkOk = false;
               }
               // Check if an item has been in the state DISPATCHING abnormally long (30 minutes)
               // (queue system synchronization can take a very long time sometimes....)
               else if ((System.currentTimeMillis() - item.getSendReceiveTime()) > (30 * 60 * 1000))
               {
                  this.queueManager.logWarning("QueueItem (" + item.toString() + ") stuck in state DISPATCHING. Item will be marked as DISPATCH_FAILED.");
                  checkOk = false;
               }

               if (!checkOk)
               {
                  // Use standard handling for queue items that failed to dispatch
                  new QueueItemTransferResponse(item.getSenderReceiverAddress(), item, QueueItemTransferResponse.QUEUE_ITEM_TRANSFER_FAILURE).execute(this.queueManager);
               }
            }
            else if (checkAge && (item.getStatus() == QueueItem.DISPATCHED))
            {
               if ((System.currentTimeMillis() - item.getSendReceiveTime()) > (ageLimit * (item.getAgeLimitWarningCount() + 1)))
               {
                  this.outItemAgeWarning(item);
                  item.incrementAgeLimitWarningCount();
               }
            }
         }
      }
   }

   /**
    * performQueueSystemCheck
    */
   public boolean performQueueSystemCheck()
   {
      return this.queueManager.collaborationManager.performCheck();
   }
}