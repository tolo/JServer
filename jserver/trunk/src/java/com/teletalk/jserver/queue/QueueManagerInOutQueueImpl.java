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

import com.teletalk.jserver.comm.EndPointIdentifier;
import com.teletalk.jserver.queue.command.QueueItemCompletionResponse;
import com.teletalk.jserver.queue.command.QueueItemResponse;

/**
 * In/out queue mode implementation of methods in QueueManager.
 */
final class QueueManagerInOutQueueImpl extends QueueManagerImplBase
{

   private InOutQueueController inOutController;

   /**
    * Creates a new QueueManagerInOutQueueImpl.
    */
   public QueueManagerInOutQueueImpl(QueueManager queueManager, InOutQueueController inOutController)
   {
      super(queueManager, inOutController);
      this.inOutController = inOutController;
   }

   /**
    * performInQueueCheckForEach
    */
   void performInQueueCheckForEach(QueueItem item)
   {
      QueueItem[] children = this.queueManager.outQueue.getWithParentId(item.getId());

      // if(inOutQueueAutoModeEnabled)
      item.setChildCount(children.length);

      if ((item.getStatus() == QueueItem.QUEUED) && (children != null) && (children.length > 0))
      {
         item.setStatus(QueueItem.CHECKED_OUT); // ...set status to CHECKED_OUT.
      }
      else if (item.isCompleted())
      {
         this.queueManager.inItemDone(item);
      }
      // If in queue item has no children in out queue...
      else if ((children != null) && (children.length == 0))
      {
         item.setStatus(QueueItem.QUEUED); // ...set status to QUEUED.
      }
   }

   /**
    * performOutQueueCheckForEach
    */
   QueueItemResponse performOutQueueCheckForEach(QueueItem item)
   {
      String parentId = item.getParentId();

      // If item has no parent in in queue...
      if ((parentId == null) || (!this.queueManager.inQueue.containsItemId(parentId)))
      {
         this.queueManager.outQueue.remove(item); // ...remove item
         return null;
      }
      else return super.performOutQueueCheckForEach(item);
   }

   /**
    * queueSystemSynchronizationRequestReceived_RemoveChildren
    */
   void queueSystemSynchronizationRequestReceived_RemoveChildren(String id)
   {
      // Remove children, if any
      QueueItem[] children = this.queueManager.getOutQueue().getWithParentId(id);
      for (int q = 0; q < children.length; q++)
      {
         this.queueManager.getOutQueue().remove(children[q]);
      }
   }

   /**
    * dispatchQueueItem
    */
   public void dispatchQueueItem(QueueItem qItem, final EndPointIdentifier address) throws QueueStorageException
   {
      if (this.queueManager.inQueue.contains(qItem)) // If the specified qItem was in the in queue...
      {
         QueueItem parentItem = qItem;

         qItem = new QueueItem(parentItem.getItemData(), super.queueManager.getUniqueId(), parentItem.getId());
      }

      super.dispatchQueueItem(qItem, address);
   }

   /**
    * isInItemCompleted
    */
   public boolean isInItemCompleted(final QueueItem inItem)
   {
      final QueueItem[] childItems = super.queueManager.outQueue.getWithParentId(inItem.getId());
      boolean allOutItemsCompleted = true;
      QueueItem child;

      for (int i = 0; i < childItems.length; i++)
      {
         child = childItems[i];

         if (!child.isCompleted())
         {
            allOutItemsCompleted = false;
            break;
         }
      }

      return allOutItemsCompleted;
   }

   /**
    * inItemDone
    */
   public void inItemDone(final QueueItem inItem, final byte responseType, final Object responseData)
   {
      final QueueItem[] childItems = super.queueManager.outQueue.getWithParentId(inItem.getId());

      for (int i = 0; i < childItems.length; i++)
         super.queueManager.outQueue.remove(childItems[i]);

      super.inItemDone(inItem, responseType, responseData);
   }

   /**
    * cancelInItem
    */
   public void cancelInItem(final QueueItem inItem)
   {
      if (inItem != null)
      {
         if (this.inOutController.canCancelInItem(inItem))
         {
            super.queueManager.inQueue.changeStatus(inItem, QueueItem.DONE_CANCELLED);
            this.inOutController.inItemCancellationRequested(inItem);
            Object responseData;

            // If auto mode enabled:
            if ((inItem.getChildCount() == 0) && super.queueManager.inOutQueueAutoModeEnabled)
            {
               responseData = this.inOutController.allChildrenCompleted(inItem);

               if (responseData == null) super.queueManager.inItemDoneCancelled(inItem);
               else super.queueManager.inItemDoneCancelled(inItem, responseData);
            }
            else if (super.queueManager.inOutQueueAutoModeEnabled)
            {
               // Cancel any outgoing items
               QueueItem[] v = this.queueManager.outQueue.getWithParentId(inItem.getId());

               for (int i = 0; i < v.length; i++)
                  super.queueManager.cancelOutItem(v[i]);
            }
         }
      }
   }

   /**
    * relocateInItemrelocateInItem
    */
   public void relocateInItem(final QueueItem inItem)
   {
      if (inItem != null)
      {
         if (this.inOutController.canRelocateInItem(inItem))
         {
            super.queueManager.inQueue.changeStatus(inItem, QueueItem.RELOCATION_REQUIRED);
            this.inOutController.inItemRelocationRequested(inItem);
            Object responseData;

            // If auto mode enabled:
            if (inItem.getChildCount() == 0 && super.queueManager.inOutQueueAutoModeEnabled)
            {
               responseData = this.inOutController.allChildrenCompleted(inItem);

               if (responseData == null) super.queueManager.inItemRelocationRequired(inItem);
               else super.queueManager.inItemRelocationRequired(inItem, responseData);
            }
            else if (super.queueManager.inOutQueueAutoModeEnabled)
            {
               // Cancel any outgoing items
               QueueItem[] v = super.queueManager.outQueue.getWithParentId(inItem.getId());

               for (int i = 0; i < v.length; i++)
                  super.queueManager.cancelOutItem(v[i]);
            }
         }
      }
   }

   /**
    * queueItemTransferResponseAborted
    */
   public void queueItemTransferResponseAborted(final String outItemId, final QueueItem item)
   {
      super.queueItemTransferResponseAborted(outItemId, item);

      if (super.queueManager.removeAbortedItems)
      {
         // Remove child items, if any, from out-queue
         QueueItem[] childItems = super.queueManager.outQueue.getWithParentId(outItemId);

         for (int q = 0; q < childItems.length; q++)
         {
            super.queueManager.outQueue.remove(childItems[q]);
         }
      }
   }

   /**
    * queueItemCompletedSuccess
    */
   public QueueItem queueItemCompletedSuccess(final QueueItemCompletionResponse response)
   {
      final QueueItem item = super.queueItemCompletedSuccess(response);

      // If auto mode handling enabled
      if (super.queueManager.inOutQueueAutoModeEnabled && (item != null))
      {
         /*synchronized (this.queueManager.getQueueItemsLock())
         {*/
            String parentId = item.getParentId();

            if (parentId != null)
            {
               QueueItem parentItem = super.queueManager.inQueue.get(parentId);

               if (parentItem != null)
               {
                  parentItem.incrementCompletedChildCount();

                  if (parentItem.getCompletedChildCount() >= parentItem.getChildCount())
                  {
                     Object extraData = this.inOutController.allChildrenCompleted(parentItem);

                     if ((parentItem.getStatus() != QueueItem.DONE_CANCELLED) && (parentItem.getStatus() != QueueItem.RELOCATION_REQUIRED))
                     {
                        super.queueManager.inQueue.changeStatus(parentItem, QueueItem.DONE_SUCCESS);
                     }

                     if (extraData == null) super.queueManager.inItemDone(parentItem);
                     else super.queueManager.inItemDone(parentItem, extraData);
                  }
               }
            }
         /*}*/
      }

      return item;
   }

   /**
    * queueItemCompletedFailure
    */
   public QueueItem queueItemCompletedFailure(final QueueItemCompletionResponse response)
   {
      final QueueItem item = super.queueItemCompletedFailure(response);

      // If auto mode handling enabled
      if (super.queueManager.inOutQueueAutoModeEnabled && (item != null))
      {
         /*synchronized (this.queueManager.getQueueItemsLock())
         {*/
            String parentId = item.getParentId();

            if (parentId != null)
            {
               QueueItem parentItem = super.queueManager.inQueue.get(parentId);

               if (parentItem != null)
               {
                  parentItem.incrementCompletedChildCount();

                  if (parentItem.getCompletedChildCount() >= parentItem.getChildCount())
                  {
                     Object extraData = this.inOutController.allChildrenCompleted(parentItem);

                     if (parentItem.getStatus() != QueueItem.DONE_CANCELLED && parentItem.getStatus() != QueueItem.RELOCATION_REQUIRED)
                     {
                        boolean parentItemSuccess = false;

                        QueueItem[] v = super.queueManager.outQueue.getWithParentId(parentId);
                        QueueItem child;

                        // If all children have status FAILED, that status is given to the parent as well
                        for (int i = 0; i < v.length; i++)
                        {
                           child = v[i];

                           if (child.getStatus() == QueueItem.DONE_SUCCESS)
                           {
                              parentItemSuccess = true;
                              break;
                           }
                        }

                        if (!parentItemSuccess) super.queueManager.inQueue.changeStatus(parentItem, QueueItem.DONE_FAILURE);
                        else super.queueManager.inQueue.changeStatus(parentItem, QueueItem.DONE_SUCCESS);
                     }

                     if (extraData == null) super.queueManager.inItemDone(parentItem);
                     else super.queueManager.inItemDone(parentItem, extraData);
                  }
               }
            }
         /*}*/
      }

      return item;
   }

   /**
    * queueItemCompletedCancelled
    */
   public QueueItem queueItemCompletedCancelled(final QueueItemCompletionResponse response)
   {
      final QueueItem item = super.queueItemCompletedCancelled(response);

      // If auto mode handling enabled
      if (super.queueManager.inOutQueueAutoModeEnabled && (item != null))
      {
         /*synchronized (this.queueManager.getQueueItemsLock())
         {*/
            String parentId = item.getParentId();

            if (parentId != null)
            {
               QueueItem parentItem = super.queueManager.inQueue.get(parentId);

               if (parentItem != null)
               {
                  parentItem.incrementCompletedChildCount();

                  if (parentItem.getCompletedChildCount() >= parentItem.getChildCount())
                  {
                     Object extraData = this.inOutController.allChildrenCompleted(parentItem);

                     if (parentItem.getStatus() != QueueItem.DONE_CANCELLED && parentItem.getStatus() != QueueItem.RELOCATION_REQUIRED)
                     {
                        boolean successFound = false;
                        boolean failureFound = false;

                        QueueItem[] v = super.queueManager.outQueue.getWithParentId(parentId);
                        QueueItem child;

                        for (int i = 0; i < v.length; i++)
                        {
                           child = v[i];

                           if (child.getStatus() == QueueItem.DONE_SUCCESS)
                           {
                              successFound = true;
                              break;
                           }
                           else if (child.getStatus() == QueueItem.DONE_FAILURE)
                           {
                              failureFound = true;
                           }
                        }

                        if (!successFound)
                        {
                           if (!failureFound) super.queueManager.inQueue.changeStatus(parentItem, QueueItem.DONE_FAILURE);
                           else super.queueManager.inQueue.changeStatus(parentItem, QueueItem.DONE_CANCELLED);
                        }
                        else super.queueManager.inQueue.changeStatus(parentItem, QueueItem.DONE_SUCCESS);
                     }

                     if (extraData == null) super.queueManager.inItemDone(parentItem);
                     else super.queueManager.inItemDone(parentItem, extraData);
                  }
               }
            }
         /*}*/
      }

      return item;
   }
}