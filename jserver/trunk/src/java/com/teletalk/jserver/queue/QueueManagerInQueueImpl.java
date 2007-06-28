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

import com.teletalk.jserver.comm.EndPointIdentifier;
import com.teletalk.jserver.queue.command.QueueItemCompletionResponse;
import com.teletalk.jserver.queue.command.QueueSystemSynchronizationRequest;
import com.teletalk.jserver.queue.command.QueueSystemSynchronizationResponse;

/**
 * In queue mode implementation of methods in QueueManager.
 */
final class QueueManagerInQueueImpl extends QueueManagerImplBase
{

   /**
    * Creates a new QueueManagerInQueueImpl.
    */
   public QueueManagerInQueueImpl(QueueManager queueManager, InQueueController inController)
   {
      super(queueManager, inController);
   }

   /**
    * performOutQueueCheck
    */
   public ArrayList performOutQueueCheck(ArrayList responsesToExecute)
   {
      return new ArrayList();
   }

   /**
    * initiateQueueSystemSynchronization
    */
   public QueueSystemSynchronizationRequest initiateQueueSystemSynchronization(RemoteQueueSystem rqs)
   {
      return new QueueSystemSynchronizationRequest(); // Retun empty request
   }

   /**
    * queueSystemSynchronizationResponseReceived
    */
   public void queueSystemSynchronizationResponseReceived(RemoteQueueSystem rqs, QueueSystemSynchronizationResponse synchResponse)
   {
   }

   /**
    * recoveredItemsInOutQueue
    */
   public void recoveredItemsInOutQueue()
   {
   }

   /**
    * createOutgoingQueueItems
    */
   public QueueItem[] createOutgoingQueueItems(QueueItemData[] itemData, QueueItem parentItem, boolean add) throws QueueStorageException
   {
      super.queueManager.logError("Method createOutgoingQueueItems(QueueItemData[] itemData, QueueItem parentItem, boolean add) not available in in-queue mode!");
      return null;
   }

   /**
    * createOutgoingQueueItem
    */
   public QueueItem createOutgoingQueueItem(QueueItemData itemData, QueueItem parentItem, boolean add) throws QueueStorageException
   {
      super.queueManager.logError("Method createOutgoingQueueItem(QueueItemData itemData, QueueItem parentItem, boolean add) not available in in-queue mode!");
      return null;
   }

   /**
    * dispatchQueueItem
    */
   public void dispatchQueueItem(QueueItem qItem, EndPointIdentifier address) throws QueueStorageException
   {
      super.queueManager.logError("Method dispatchQueueItem(QueueItem qItem, EndPointIdentifier address) not available in in-queue mode!");
   }

   /**
    * dispatchQueueItemQuery
    */
   public void dispatchQueueItemQuery(QueueItem outQueueItem)
   {
      super.queueManager.logError("Method dispatchQueueItemQuery(QueueItem outQueueItem) not available in in-queue mode!");
   }

   /**
    * isInItemCompleted
    */
   public boolean isInItemCompleted(QueueItem inItem)
   {
      return inItem.isCompleted();
   }

   /**
    * cancelInItem
    */
   public void cancelInItem(final QueueItem inItem)
   {
      if (inItem != null)
      {
         synchronized (this.queueManager.getQueueItemsLock())
         {
            if (super.inController.canCancelInItem(inItem))
            {
               this.queueManager.inQueue.changeStatus(inItem, QueueItem.DONE_CANCELLED);

               final Object responseData = super.inController.inItemCancellationRequested(inItem);

               if (responseData == null) super.queueManager.inItemDoneCancelled(inItem);
               else super.queueManager.inItemDoneCancelled(inItem, responseData);
            }
         }
      }
   }

   /**
    * cancelOutItem
    */
   public void cancelOutItem(QueueItem outItem)
   {
      super.queueManager.logError("Method cancelOutItem(QueueItem outItem) not available in in-queue mode!");
   }

   /**
    * relocateInItem
    */
   public void relocateInItem(final QueueItem inItem)
   {
      if (inItem != null)
      {
         synchronized (this.queueManager.getQueueItemsLock())
         {
            if (super.inController.canRelocateInItem(inItem))
            {
               super.queueManager.inQueue.changeStatus(inItem, QueueItem.RELOCATION_REQUIRED);
               final Object responseData = super.inController.inItemRelocationRequested(inItem);

               if (responseData == null) super.queueManager.inItemRelocationRequired(inItem);
               else super.queueManager.inItemRelocationRequired(inItem, responseData);
            }
         }
      }
   }

   /**
    * relocateOutItem
    */
   public void relocateOutItem(QueueItem outItem)
   {
      super.queueManager.logError("Method relocateOutItem(QueueItem outItem) not available in in-queue mode!");
   }

   /**
    * forceOutItemDone
    */
   public void forceOutItemDone(QueueItem outItem, byte responseType, Object responseData)
   {
      super.queueManager.logError("Method forceOutItemDone(QueueItem outItem, byte responseType, Object responseData) not available in in-queue mode!");
   }

   /**
    * queueItemTransferRequestAborted
    */
   public void queueItemTransferRequestAborted(final QueueItem item)
   {
      super.queueManager.logError("Method queueItemTransferRequestAborted(QueueItem item) not available in in-queue mode!");
   }

   /**
    * queueItemTransferred
    */
   public void queueItemTransferred(final String outItemId, final QueueItem item)
   {
      super.queueManager.logError("Method queueItemTransferred(String outItemId, QueueItem item) not available in in-queue mode!");
   }

   /**
    * queueItemTransferFailure
    */
   public void queueItemTransferFailure(final String outItemId, final QueueItem item)
   {
      super.queueManager.logError("Method queueItemTransferFailure(String outItemId, QueueItem item) not available in in-queue mode!");
   }

   /**
    * queueItemTransferFailureQueueFull
    */
   public void queueItemTransferFailureQueueFull(final String outItemId, final QueueItem item)
   {
      super.queueManager.logError("Method queueItemTransferFailureQueueFull(String outItemId, QueueItem item) not available in in-queue mode!");
   }

   /**
    * queueItemCompletedSuccess
    */
   public QueueItem queueItemCompletedSuccess(QueueItemCompletionResponse response)
   {
      super.queueManager.logError("Method queueItemCompletedSuccess(QueueItemCompletionResponse response) not available in in-queue mode!");
      return null;
   }

   /**
    * queueItemCompletedFailure
    */
   public QueueItem queueItemCompletedFailure(QueueItemCompletionResponse response)
   {
      super.queueManager.logError("Method queueItemCompletedFailure(QueueItemCompletionResponse response) not available in in-queue mode!");
      return null;
   }

   /**
    * queueItemCompletedCancelled
    */
   public QueueItem queueItemCompletedCancelled(QueueItemCompletionResponse response)
   {
      super.queueManager.logError("Method queueItemCompletedCancelled(QueueItemCompletionResponse response) not available in in-queue mode!");
      return null;
   }

   /**
    * queueItemRelocationRequiredResponseReceived
    */
   public void queueItemRelocationRequiredResponseReceived(QueueItemCompletionResponse response)
   {
      super.queueManager.logError("Method queueItemRelocationRequiredResponseReceived(QueueItemCompletionResponse response) not available in in-queue mode!");
   }

   /**
    * outItemAgeWarning
    */
   public void outItemAgeWarning(QueueItem outItem)
   {
      // NULL IMPLEMENTATION
   }

   /**
    * performOutItemCheck
    */
   public void performOutItemCheck()
   {
      // NULL IMPLEMENTATION
   }
}