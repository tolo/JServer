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

import com.teletalk.jserver.comm.EndPointIdentifier;
import com.teletalk.jserver.queue.InOutQueueController;
import com.teletalk.jserver.queue.OutQueueController;
import com.teletalk.jserver.queue.Queue;
import com.teletalk.jserver.queue.QueueItem;
import com.teletalk.jserver.queue.QueueItemData;
import com.teletalk.jserver.queue.QueueManager;
import com.teletalk.jserver.queue.QueueManagerImpl;
import com.teletalk.jserver.queue.QueueStorageException;
import com.teletalk.jserver.queue.RemoteQueueSystem;
import com.teletalk.jserver.queue.command.MultiQueueItemTransferRequest;
import com.teletalk.jserver.queue.command.QueueItemCancellationRequest;
import com.teletalk.jserver.queue.command.QueueItemCompletionResponse;
import com.teletalk.jserver.queue.command.QueueItemQuery;
import com.teletalk.jserver.queue.command.QueueItemRelocationRequest;
import com.teletalk.jserver.queue.command.QueueItemTransferRequest;
import com.teletalk.jserver.queue.command.QueueSystemSynchronizationRequest;
import com.teletalk.jserver.queue.command.QueueSystemSynchronizationResponse;

/**
 * Wrapper class for the QueueManagerImpl of the QueueManager. This class overrides the logic 
 * used for dispatching queue items. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1.2 (20060224)
 */
class QueueManagerImplWrapper implements QueueManagerImpl 
{
   private final QueueManager queueManager;
      
   private final QueueManagerImpl queueManagerImpl;
   

   /**
    * Creates a new QueueManagerImplWrapper.
    */
   public QueueManagerImplWrapper(final QueueManager queueManager)
   {
      this.queueManager = queueManager;
      this.queueManagerImpl = this.queueManager.getQueueManagerImpl();
   }
   
   
   /**
    */
   public void onQueueItemTransferRequestDispatch(final QueueItemTransferRequest queueItemTransferRequest)
   {
      QueueItem qItem = queueItemTransferRequest.getQueueItem();
      
      synchronized (this.queueManager.getQueueItemsLock())
      {
         Queue outQueue = this.queueManager.getOutQueue();
         
         if( outQueue != null )
         {
            if (!outQueue.contains(qItem)) outQueue.add(qItem);
            else outQueue.updatePersistentStorage(qItem);
         }
      }
   }
   
   
   /**
    */
   public void onMultiQueueItemTransferRequestDispatch(final MultiQueueItemTransferRequest multiQueueItemTransferRequest)
   {
      QueueItem[] qItems = multiQueueItemTransferRequest.getQueueItems();
      
      synchronized (this.queueManager.getQueueItemsLock())
      {
         Queue outQueue = this.queueManager.getOutQueue();
         
         if( outQueue != null )
         {
            for (int i = 0; i<qItems.length; i++)
            {
               if (!outQueue.contains(qItems[i])) outQueue.add(qItems[i]);
               else outQueue.updatePersistentStorage(qItems[i]);
            }
         }
      }
   }
   
   
   /* ### OVERRIDDEN METHODS ### */
   
   
   public void dispatchQueueItem(QueueItem qItem, final EndPointIdentifier address) throws QueueStorageException
   {
      if( this.queueManager.getQueueController() instanceof InOutQueueController )
      {
         if (this.queueManager.getInQueue().contains(qItem)) // If the specified qItem was in the in queue...
         {
            QueueItem parentItem = qItem;
   
            qItem = new QueueItem(parentItem.getItemData(), this.queueManager.getUniqueId(), parentItem.getId());
         }
      }
      
      if( (this.queueManager.getQueueController() instanceof OutQueueController) || 
            (this.queueManager.getQueueController() instanceof InOutQueueController) )
      {
         QueueItemTransferRequest command = new QueueItemTransferRequest(address, qItem);
         
         synchronized (this.queueManager.getQueueItemsLock())
         {
            qItem.incrementDispatchCount();
            qItem.setSenderReceiverAddress(address);
            qItem.setSendReceiveTime(System.currentTimeMillis());
            qItem.forceStatus(QueueItem.DISPATCHING);
            // Defer adding the queue item to onQueueItemTransferRequestDispatch
         }
         
         this.queueManager.dispatchCommand(command);
      }
   }
   
   
   public void dispatchQueueItems(final QueueItem[] qItems, final EndPointIdentifier address) throws QueueStorageException
   {
      if( (this.queueManager.getQueueController() instanceof OutQueueController) || 
            (this.queueManager.getQueueController() instanceof InOutQueueController) )
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
               // Defer adding the queue items to onMultiQueueItemTransferRequestDispatch
            }
         }

         this.queueManager.dispatchCommand(command);
      }
   }
   
   
   /* ### DELEGATE METHODS ### */
   

   public void cancelInItem(QueueItem inItem)
   {
      queueManagerImpl.cancelInItem(inItem);
   }

   public void cancelOutItem(QueueItem outItem)
   {
      queueManagerImpl.cancelOutItem(outItem);
   }

   public QueueItem createOutgoingQueueItem(QueueItemData itemData, QueueItem parentItem, boolean add) throws QueueStorageException
   {
      return queueManagerImpl.createOutgoingQueueItem(itemData, parentItem, add);
   }

   public QueueItem[] createOutgoingQueueItems(QueueItemData[] itemData, QueueItem parentItem, boolean add) throws QueueStorageException
   {
      return queueManagerImpl.createOutgoingQueueItems(itemData, parentItem, add);
   }

   public void dispatchQueueItemQuery(QueueItem outQueueItem)
   {
      queueManagerImpl.dispatchQueueItemQuery(outQueueItem);
   }
   
   public void forceOutItemDone(QueueItem outItem, byte responseType, Object responseData)
   {
      queueManagerImpl.forceOutItemDone(outItem, responseType, responseData);
   }

   public void inItemDone(QueueItem inItem, byte responseType, Object responseData)
   {
      queueManagerImpl.inItemDone(inItem, responseType, responseData);
   }

   public QueueSystemSynchronizationRequest initiateQueueSystemSynchronization(RemoteQueueSystem rqs)
   {
      return queueManagerImpl.initiateQueueSystemSynchronization(rqs);
   }

   public boolean isInItemCompleted(QueueItem inItem)
   {
      return queueManagerImpl.isInItemCompleted(inItem);
   }

   public void multiQueueItemTransferRequestReceived(MultiQueueItemTransferRequest request)
   {
      queueManagerImpl.multiQueueItemTransferRequestReceived(request);
   }

   public void outItemAgeWarning(QueueItem outItem)
   {
      queueManagerImpl.outItemAgeWarning(outItem);
   }

   public ArrayList performInQueueCheck()
   {
      return queueManagerImpl.performInQueueCheck();
   }

   public void performOutItemCheck()
   {
      queueManagerImpl.performOutItemCheck();
   }

   public ArrayList performOutQueueCheck(ArrayList responsesToExecute)
   {
      return queueManagerImpl.performOutQueueCheck(responsesToExecute);
   }

   public boolean performQueueSystemCheck()
   {
      return queueManagerImpl.performQueueSystemCheck();
   }

   public void queueItemCancellationRequest(QueueItemCancellationRequest request)
   {
      queueManagerImpl.queueItemCancellationRequest(request);
   }

   public QueueItem queueItemCompletedCancelled(QueueItemCompletionResponse response)
   {
      return queueManagerImpl.queueItemCompletedCancelled(response);
   }

   public QueueItem queueItemCompletedFailure(QueueItemCompletionResponse response)
   {
      return queueManagerImpl.queueItemCompletedFailure(response);
   }

   public QueueItem queueItemCompletedSuccess(QueueItemCompletionResponse response)
   {
      return queueManagerImpl.queueItemCompletedSuccess(response);
   }

   public void queueItemCompletionResponseAborted(QueueItemCompletionResponse command)
   {
      queueManagerImpl.queueItemCompletionResponseAborted(command);
   }

   public void queueItemQueryReceived(QueueItemQuery query)
   {
      queueManagerImpl.queueItemQueryReceived(query);
   }

   public void queueItemRelocationRequest(QueueItemRelocationRequest request)
   {
      queueManagerImpl.queueItemRelocationRequest(request);
   }

   public void queueItemRelocationRequiredResponseReceived(QueueItemCompletionResponse response)
   {
      queueManagerImpl.queueItemRelocationRequiredResponseReceived(response);
   }

   public void queueItemTransferFailure(String outItemId, QueueItem item)
   {
      queueManagerImpl.queueItemTransferFailure(outItemId, item);
   }

   public void queueItemTransferFailureQueueFull(String outItemId, QueueItem item)
   {
      queueManagerImpl.queueItemTransferFailureQueueFull(outItemId, item);
   }

   public void queueItemTransferred(String outItemId, QueueItem item)
   {
      queueManagerImpl.queueItemTransferred(outItemId, item);
   }

   public void queueItemTransferRequestAborted(QueueItem item)
   {
      queueManagerImpl.queueItemTransferRequestAborted(item);
   }

   public void queueItemTransferRequestReceived(QueueItemTransferRequest request)
   {
      queueManagerImpl.queueItemTransferRequestReceived(request);
   }

   public void queueItemTransferResponseAborted(String outItemId, QueueItem item)
   {
      queueManagerImpl.queueItemTransferResponseAborted(outItemId, item);
   }

   public QueueSystemSynchronizationResponse queueSystemSynchronizationRequestReceived(RemoteQueueSystem rqs, QueueSystemSynchronizationRequest synchRequest)
   {
      return queueManagerImpl.queueSystemSynchronizationRequestReceived(rqs, synchRequest);
   }

   public void queueSystemSynchronizationResponseReceived(RemoteQueueSystem rqs, QueueSystemSynchronizationResponse synchResponse)
   {
      queueManagerImpl.queueSystemSynchronizationResponseReceived(rqs, synchResponse);
   }

   public void recoveredItemsInInQueue()
   {
      queueManagerImpl.recoveredItemsInInQueue();
   }

   public void recoveredItemsInOutQueue()
   {
      queueManagerImpl.recoveredItemsInOutQueue();
   }

   public void relocateInItem(QueueItem inItem)
   {
      queueManagerImpl.relocateInItem(inItem);
   }

   public void relocateOutItem(QueueItem outItem)
   {
      queueManagerImpl.relocateOutItem(outItem);
   }
}
