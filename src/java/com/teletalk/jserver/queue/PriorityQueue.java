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
import java.util.Collections;
import java.util.Comparator;
import java.util.Vector;

import com.teletalk.jserver.JServerUtilities;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.property.VectorProperty;

/**
 * This class implements a Queue with added behaviour for handling of QueueItems that have a priority. The QueueItems
 * contained in this PriorityQueue must contain data objects that implement the PriorityQueueItemData interface. This
 * makes it prossible for the PriorityQueue to get the priority from the data object.
 * 
 * @see com.teletalk.jserver.queue.PriorityQueueItemData
 * @see com.teletalk.jserver.queue.Queue
 * @author Tobias Löfstrand
 * @since Beta 1
 */
public final class PriorityQueue extends Queue
{

   /**
    * Comparator class for priority. This comparator is used when creating iterators for the associated PriorityQueue.
    * 
    * @see PriorityQueue
    * @see com.teletalk.jserver.property.VectorProperty.VectorPropertyIterator
    * @author Tobias Löfstrand
    * @since Beta 1
    */
   public static final class PriorityQueueItemComparator implements Comparator
   {

      private final boolean ascending;

      /**
       * Creates a new PriorityQueueItemComparator.
       * 
       * @param ascending indicates if the comparison order should be ascending (<code>true</code>) or descending (<code>false</code>).
       */
      public PriorityQueueItemComparator(boolean ascending)
      {
         this.ascending = ascending;

      }

      /**
       * Compares two objects for order according to priority. Returns a negative integer, zero, or a positive integer
       * as the first argument is less than, equal to, or greater than the second.
       * 
       * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or
       *         greater than the second.
       */
      public int compare(final Object o1, final Object o2)
      {
         if (o1 == null) return (o2 == null) ? 0 : (ascending ? -1 : 1);
         else if (o2 == null) return (o1 == null) ? 0 : (ascending ? 1 : -1);
         else
         {
            int prio1 = 0, prio2 = 0;
            boolean o1Null = false, o2Null = false;

            if (o1 instanceof QueueItem)
            {
               o1Null = (((QueueItem) o1).getItemData() == null);
               if (!o1Null) prio1 = ((PriorityQueueItemData) ((QueueItem) o1).getItemData()).getPriority();
            }
            else if (o1 instanceof PriorityQueueItemData) prio1 = ((PriorityQueueItemData) o1).getPriority();
            else if (o1 instanceof Number) prio1 = ((Number) o1).intValue();
            else prio1 = o1.hashCode();

            if (o2 instanceof QueueItem)
            {
               o2Null = (((QueueItem) o2).getItemData() == null);
               if (!o2Null) prio2 = ((PriorityQueueItemData) ((QueueItem) o2).getItemData()).getPriority();
            }
            else if (o2 instanceof PriorityQueueItemData) prio2 = ((PriorityQueueItemData) o2).getPriority();
            else if (o2 instanceof Number) prio2 = ((Number) o2).intValue();
            else prio2 = o2.hashCode();

            if (o1Null) return o2Null ? 0 : (ascending ? -1 : 1);
            else if (o2Null) return o1Null ? 0 : (ascending ? 1 : -1);
            else return (prio1 == prio2) ? 0 : ((prio1 < prio2) ? (ascending ? -1 : 1) : (ascending ? 1 : -1));
         }
      }

      /**
       * Compares this PriorityQueueItemComparator for equality.
       * 
       * @return true if the object specified by param <code>obj</code> is a PriorityQueueItemComparator and has the
       *         same ordering.
       */
      public boolean equals(Object obj)
      {
         if (obj instanceof PriorityQueueItemComparator)
         {
            return ((PriorityQueueItemComparator) obj).ascending == this.ascending;
         }
         else return false;
      }
   }

   /** Priority comparator for ascending priority. */
   public static final PriorityQueueItemComparator ascendingPriorityComparator = new PriorityQueueItemComparator(true);

   /** Priority comparator for descending priority. */
   public static final PriorityQueueItemComparator descendingPriorityComparator = new PriorityQueueItemComparator(false);

   private static final Comparator queuedItemPriorityListComparator = new Comparator()
   {

      public int compare(final Object o1, final Object o2)
      {
         return ((PriorityTuple) o1).getPriority() - ((PriorityTuple) o2).getPriority();
      }

      public boolean equals(Object obj)
      {
         return obj == this;
      }
   };

   private class PriorityTuple
   {

      private final String key;

      private final short priority;

      private final boolean prioritySearch;

      public PriorityTuple(String key, short priority)
      {
         this.key = key;
         this.priority = priority;
         prioritySearch = false;
      }

      public PriorityTuple(String key, short priority, boolean prioritySearch)
      {
         this.key = key;
         this.priority = priority;
         this.prioritySearch = prioritySearch;
      }

      protected String getKey()
      {
         return key;
      }

      protected short getPriority()
      {
         return priority;
      }

      public boolean equals(Object o)
      {
         if (o instanceof PriorityTuple)
         {
            PriorityTuple pt = (PriorityTuple) o;

            if (prioritySearch) return (this.priority == pt.priority);
            else return this.key.equals(pt.key) && (this.priority == pt.priority);
         }
         else if (o instanceof QueueItem)
         {
            QueueItem qi = (QueueItem) o;
            PriorityQueueItemData itemData = (PriorityQueueItemData) qi.getItemData();

            if (prioritySearch) return (this.priority == itemData.getPriority());
            else return this.key.equals(qi.getKey()) && (this.priority == itemData.getPriority());
         }
         else return false;
      }
   }

   private final ArrayList queuedItemPriorityList;

   /**
    * Creates a new PriorityQueue with no parent, using a SimpleFileQueueStorage object to store QueueItems.
    * 
    * @param name the name of this PriorityQueue.
    * @see com.teletalk.jserver.queue.SimpleFileQueueStorage
    */
   public PriorityQueue(String name)
   {
      super(name);

      this.queuedItemPriorityList = new ArrayList();
   }

   /**
    * Creates a new PriorityQueue with no parent.
    * 
    * @param name the name of this PriorityQueue.
    * @param queueStorage the queueStorage object which will be used to store QueueItems.
    */
   public PriorityQueue(String name, QueueStorage queueStorage)
   {
      super(name, queueStorage);

      this.queuedItemPriorityList = new ArrayList();
   }

   /**
    * Creates a new PriorityQueue.
    * 
    * @param parent the parent of this PriorityQueue.
    * @param name the name of this PriorityQueue.
    * @param queueStorage the queueStorage object which will be used to store QueueItems.
    */
   public PriorityQueue(SubComponent parent, String name, QueueStorage queueStorage)
   {
      super(parent, name, queueStorage);

      this.queuedItemPriorityList = new ArrayList();
   }

   /**
    * Creates a new PriorityQueue using a SimpleFileQueueStorage object to store QueueItems.
    * 
    * @param parent the parent of this PriorityQueue.
    * @param name the name of this PriorityQueue.
    * @see com.teletalk.jserver.queue.SimpleFileQueueStorage
    */
   public PriorityQueue(SubComponent parent, String name)
   {
      super(parent, name);

      this.queuedItemPriorityList = new ArrayList();
   }

   protected void queueItemAddNotification(QueueItem item)
   {
      if (item.getStatus() != QueueItem.QUEUED) return; // Only add the item to the queuedItemPriorityList if status is
                                                         // QUEUED

      PriorityQueueItemData itemData = (PriorityQueueItemData) item.getItemData();
      PriorityTuple tuple = new PriorityTuple(item.getKey(), itemData.getPriority());

      int addIndex = Collections.binarySearch(queuedItemPriorityList, tuple, queuedItemPriorityListComparator);
      if (addIndex < 0) addIndex = (-addIndex) - 1; // If key not found, return value of Collections.binarySearch is
                                                      // (-(insertion point) - 1)

      if ((addIndex < 0) || (addIndex >= queuedItemPriorityList.size())) queuedItemPriorityList.add(tuple);
      else queuedItemPriorityList.add(addIndex, tuple);
   }

   protected void queueItemRemoveNotification(QueueItem item)
   {
      PriorityQueueItemData itemData = (PriorityQueueItemData) item.getItemData();

      queuedItemPriorityList.remove(new PriorityTuple(item.getKey(), itemData.getPriority()));
   }

   protected void queueItemStatusChangeNotification(QueueItem item, int oldStatus, int newStatus)
   {
      if ((oldStatus == QueueItem.QUEUED) && (newStatus != QueueItem.QUEUED)) // If item is currently queued and the new
                                                                              // status is not QUEUED...
      {
         queueItemRemoveNotification(item);
      }
      else if ((oldStatus != QueueItem.QUEUED) && (newStatus == QueueItem.QUEUED)) // If item is not currently queued,
                                                                                    // but the new status is QUEUED...
      {
         queueItemAddNotification(item);
      }
   }

   /**
    * Adds a QueueItem to the Queue. Note that QueueItems containing item data objects of other types than
    * PriorityQueueItemData will cause this method to throw a ClassCastException.
    * 
    * @param item a QueueItem to be added to the Queue.
    * @exception QueueStorageException if an error occurs when the QueueStorage object associated with this Queue tries
    *               to store the QueueItem.
    * @exception ClassCastException if the itemdata object in the specified QueueItem object was not an instance of
    *               PriorityQueueItemData.
    * @see PriorityQueueItemData
    * @return <code>true</code> if the item was successfully added, otherwise <code>false</code>.
    */
   public boolean add(QueueItem item) throws QueueStorageException, ClassCastException
   {
      return super.add(item);
   }

   /**
    * Returns an iterator for this Queue that will iterate over the elements in ascending priority order.
    * 
    * @return a VectorProperty.VectorPropertyIterator object.
    */
   public VectorProperty.VectorPropertyIterator priorityIterator()
   {
      return priorityIterator(true);
   }

   /**
    * Returns an iterator for this Queue that will iterate over the elements in ascending or descending priority order,
    * as specified by parameter <code>ascending</code>.
    * 
    * @param ascending boolean flag indicating if ascending (<code>true</code>) or descending (<code>false</code>)
    *           priority order should be used.
    * @return a VectorProperty.VectorPropertyIterator object.
    */
   public VectorProperty.VectorPropertyIterator priorityIterator(boolean ascending)
   {
      if (ascending) return queueVector.iterator(ascendingPriorityComparator);
      else return queueVector.iterator(descendingPriorityComparator);
   }

   /**
    * Gets the QueueItem with the lowest priority value and which hasn't already been checked out(status = QUEUED). If
    * the queue is empty, the calling thread will block until a QueueItem gets put into the queue.
    * 
    * @return a QueueItem or null if an error occured.
    * @exception InterruptedException if the calling thread was interrupted while waiting.
    */
   public QueueItem getWithLowestPriority() throws InterruptedException
   {
      synchronized (super.getLock())
      {
         while (!hasQueuedItems())
         {
            super.getLock().wait();
         }

         return super.get(((PriorityTuple) queuedItemPriorityList.get(0)).getKey());
      }
   }

   /**
    * Checkes out the QueueItem with the lowest priority value and which hasn't already been checked out(status =
    * QUEUED). If the queue is empty, the calling thread will block until a QueueItem gets put into the queue.
    * 
    * @return a QueueItem or null if an error occured.
    * @exception InterruptedException if the calling thread was interrupted while waiting.
    */
   public QueueItem checkOutWithLowestPriority() throws InterruptedException
   {
      synchronized (super.getLock())
      {
         while (!hasQueuedItems())
         {
            super.getLock().wait();
         }

         return super.checkOut(((PriorityTuple) queuedItemPriorityList.get(0)).getKey());
      }
   }

   /**
    * Gets the QueueItem with the highest priority value and which hasn't already been checked out(status = QUEUED). If
    * the queue is empty, the calling thread will block until a QueueItem gets put into the queue.
    * 
    * @return a QueueItem or null if an error occured.
    * @exception InterruptedException if the calling thread was interrupted while waiting.
    */
   public QueueItem getWithHighestPriority() throws InterruptedException
   {
      synchronized (super.getLock())
      {
         while (!hasQueuedItems())
         {
            super.getLock().wait();
         }

         return super.get(((PriorityTuple) queuedItemPriorityList.get(queuedItemPriorityList.size() - 1)).getKey());
      }
   }

   /**
    * Checkes out the QueueItem with the highest priority value and which hasn't already been checked out(status =
    * QUEUED). If the queue is empty, the calling thread will block until a QueueItem gets put into the queue.
    * 
    * @return a QueueItem or null if an error occured.
    * @exception InterruptedException if the calling thread was interrupted while waiting.
    */
   public QueueItem checkOutWithHighestPriority() throws InterruptedException
   {
      synchronized (super.getLock())
      {
         while (!hasQueuedItems())
         {
            super.getLock().wait();
         }

         return super.checkOut(((PriorityTuple) queuedItemPriorityList.get(queuedItemPriorityList.size() - 1)).getKey());
      }
   }

   /**
    * Gets all (not just queue) QueueItems in this PriorityQueue which has the specidied priority.
    * 
    * @param priority a priority level.
    */
   public QueueItem[] getAllWithPriority(short priority)
   {
      ArrayList v = new ArrayList();
      VectorProperty.VectorPropertyIterator it = this.priorityIterator(); // iterator();
      QueueItem item;
      PriorityQueueItemData data;

      while (it.hasNext())
      {
         item = (QueueItem) it.next();
         if (item == null) continue;

         data = (PriorityQueueItemData) item.getItemData();
         if (data == null) continue;

         if (data.getPriority() == priority) v.add(item);
         else if (priority < data.getPriority()) break;
      }

      return (QueueItem[]) v.toArray(new QueueItem[v.size()]);
   }

   /**
    * Gets all the QueueItems stored in this Queue that hasn't been checked out yet (status = QUEUED). The QueueItems in
    * the returned list will be sorted according to priority.
    * 
    * @return a Vector containing all QueueItem objects in this Queue with status = QUEUED.
    */
   public Vector getAllQueued()
   {
      if (isDebugMode()) logDebug("getAllQueued(). " + JServerUtilities.getStackTrace());

      synchronized (super.getLock())
      {
         return new Vector(queuedItemPriorityList);
      }
   }

   /**
    * Gets all QueueItems in this PriorityQueue that has the specidied priority and hasn't already been checked out
    * (status = QUEUED).
    * 
    * @param priority a priority level.
    */
   public QueueItem[] getAllQueuedWithPriority(short priority)
   {
      ArrayList v = new ArrayList();
      VectorProperty.VectorPropertyIterator it = this.priorityIterator();// iterator();
      QueueItem item;
      PriorityQueueItemData data;

      while (it.hasNext())
      {
         item = (QueueItem) it.next();
         if (item == null) continue;

         data = (PriorityQueueItemData) item.getItemData();
         if (data == null) continue;

         if ((data.getPriority() == priority) && (item.getStatus() == QueueItem.QUEUED)) v.add(item);
         else if (priority < data.getPriority()) break;
      }

      return (QueueItem[]) v.toArray(new QueueItem[v.size()]);
   }

   /**
    * Checks out all QueueItems in this PriorityQueue which has the specidied priority.
    * 
    * @param priority a priority level.
    */
   public QueueItem[] checkOutAllWithPriority(short priority)
   {
      ArrayList v = new ArrayList();
      VectorProperty.VectorPropertyIterator it = this.priorityIterator();// iterator();
      QueueItem item;
      PriorityQueueItemData data;

      while (it.hasNext())
      {
         item = (QueueItem) it.next();
         if (item == null) continue;

         data = (PriorityQueueItemData) item.getItemData();
         if (data == null) continue;

         if ((data.getPriority() == priority) && (item.getStatus() == QueueItem.QUEUED)) if (checkOut(item)) v.add(item);
         else if (priority < data.getPriority()) break;
      }

      return (QueueItem[]) v.toArray(new QueueItem[v.size()]);
   }

   /**
    * Gets the first QueueItem in the queue with the specified priority and that hasn't already been checked out(status =
    * QUEUED).
    * 
    * @param priority the priority.
    * @return a QueueItem or null if no QueueItem with the specified priority was found.
    */
   public QueueItem getFirstWithPriority(short priority)
   {
      synchronized (super.getLock())
      {
         PriorityTuple tuple = new PriorityTuple("", priority, true);

         int index = Collections.binarySearch(queuedItemPriorityList, tuple, queuedItemPriorityListComparator);

         if (index >= 0)
         {
            tuple = (PriorityTuple) queuedItemPriorityList.get(index);
            if (tuple != null) return super.get(tuple.getKey());
         }
         return null;
      }
   }

   /**
    * Checkes out the first QueueItem in the queue with the specified priority and that hasn't already been checked
    * out(status = QUEUED).
    * 
    * @param priority the priority.
    * @return a QueueItem or null if an error occured.
    */
   public QueueItem checkOutFirstWithPriority(short priority)
   {
      synchronized (super.getLock())
      {
         PriorityTuple tuple = new PriorityTuple("", priority, true);

         int index = Collections.binarySearch(queuedItemPriorityList, tuple, queuedItemPriorityListComparator);

         if (index >= 0)
         {
            tuple = (PriorityTuple) queuedItemPriorityList.get(index);
            if (tuple != null) return super.checkOut(tuple.getKey());
         }
         return null;
      }
   }
}
