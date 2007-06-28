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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;

import com.teletalk.jserver.StatusTransitionException;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.property.VectorProperty;
import com.teletalk.jserver.property.VectorPropertyOwner;

/**
 * This class implements a queue of QueueItem objects. The dynamic storage of items and the external view of the queue
 * is handled by a VectorProperty object, which also makes it possible to execute external operations on the queue.<br>
 * <br>
 * The Queue class is also capable of storing the items persistently through the use of a QueueStorage object. The
 * QueueStorage object to use is specified in the constructors of the Queue class. If none is specified, an instance of
 * the class {@link SimpleFileQueueStorage} is created. For applications that doesn't require any persistent storage,
 * the class {@link NullQueueStorage} is provided. <br>
 * <br>
 * The Queue class is used by the QueueManager to represent the in and out queues of a queue system, however it can also
 * be used stand alone.
 * 
 * @see com.teletalk.jserver.queue.QueueItem
 * @see com.teletalk.jserver.queue.QueueStorage
 * @see com.teletalk.jserver.property.VectorProperty
 * 
 * @author Tobias Löfstrand
 * @since Beta 1
 */
public class Queue extends SubComponent implements VectorPropertyOwner
{
   protected static final int GET_ALL_COMPLETED = -1;
   
   protected static final int GET_ALL_NOT_COMPLETED = -2;
   
   
   /**
    * @since 2.1.2 (20060207)
    */
   final static class QueueVectorProperty extends VectorProperty
   {
      private final Queue queue;
      
      public QueueVectorProperty(Queue parent, String name)
      {
         super(parent, name, parent.getLock());
         
         this.queue = parent;
      }

      public Object getLock()
      {
         return this.queue.getLock();
      }
   }
   
   
   /*
    * Synchronization summary: When aquiring multiple locks, lock in the following order: QueueItem, Queue
    * (VectorProperty), QueueStorage
    */

   private LinkedList queuedItemsList; // List for QueueItems with status QUEUED

   private boolean queueRestored = false; // Flag indicating if this queue has been restored from persistent storage.

   /** The associated VectorProperty object. */
   protected final QueueVectorProperty queueVector;

   /** The owner of this Queue. */
   protected QueueOwner owner;

   /** The associated QueueStorage object which is used for persistent storage of the QueueItems in this Queue. */
   protected QueueStorage queueStorage;
   
   /** The associated QueueManager. @since 2.1.2 (20060207) */
   protected QueueManager queueManager = null;
   

   /**
    * Creates a new Queue with no parent, using a SimpleFileQueueStorage object to store QueueItems.
    * 
    * @param name the name of this Queue.
    * @see com.teletalk.jserver.queue.SimpleFileQueueStorage
    */
   public Queue(String name)
   {
      this(null, name);
   }

   /**
    * Creates a new Queue with no parent.
    * 
    * @param name the name of this Queue.
    * @param queueStorage the queueStorage object which will be used to store QueueItems.
    */
   public Queue(String name, QueueStorage queueStorage)
   {
      this(null, name, queueStorage);
   }

   /**
    * Creates a new Queue using a SimpleFileQueueStorage object to store QueueItems.
    * 
    * @param parent the parent of this Queue.
    * @param name the name of this Queue.
    * @see com.teletalk.jserver.queue.SimpleFileQueueStorage
    */
   public Queue(SubComponent parent, String name)
   {
      this(parent, name, null);
   }

   /**
    * Creates a new Queue.
    * 
    * @param parent the parent of this Queue.
    * @param name the name of this Queue.
    * @param queueStorage the queueStorage object which will be used to store QueueItems.
    */
   public Queue(SubComponent parent, String name, QueueStorage queueStorage)
   {
      super(parent, name);

      queuedItemsList = new LinkedList();

      if (parent != null && parent instanceof QueueOwner)
      {
         owner = (QueueOwner) parent;
      }
      else
      {
         owner = null;
      }

      if (queueStorage == null)
      {
         this.queueStorage = new SimpleFileQueueStorage(this);
      }
      else
      {
         this.queueStorage = queueStorage;
      }

      queueVector = new QueueVectorProperty(this, "items");
      setVectorPropertyEventsEnabled(false);

      if (this.queueStorage instanceof SubComponent)
      {
         addSubComponent((SubComponent) this.queueStorage);
      }

      addProperty(queueVector);
   }
   
   /**
    * Gets the associated QueueManager.
    * 
    * @since 2.1.2 (20060207)
    */
   public QueueManager getQueueManager()
   {
      return queueManager;
   }

   /**
    * Sets the associated QueueManager.
    * 
    * @since 2.1.2 (20060207)
    */
   public void setQueueManager(QueueManager queueManager)
   {
      this.queueManager = queueManager;
   }

   /**
    * Gets the object used for synchronization of this Queue object.
    * 
    * @return the synchronization object.
    */
   public Object getLock()
   {
      if( this.getQueueManager() != null ) return getQueueManager().getQueueItemsLock();
      else return this;
   }

   /**
    * Performs enabling functionality for this Queue. The first time a Queue object is enabled attempts are made to
    * restore the queue from persistent storage.
    */
   protected void doInitialize()
   {
      if (parent != null && parent instanceof QueueOwner)
      {
         owner = (QueueOwner) parent;
      }
      else
      {
         owner = null;
      }

      if (queueStorage instanceof SubComponent)
      {
         if (!((SubComponent) queueStorage).engage()) throw new StatusTransitionException("Failed to engage queue storage!");
      }

      if (!queueRestored)
      {
         restoreQueueItemsFromPersistentStorage();
      }

      super.doInitialize();
   }

   private void restoreQueueItemsFromPersistentStorage()
   {
      synchronized (this.getLock())
      {
         List items = queueStorage.restoreQueueFromStorage();
   
         if (items.size() > 0)
         {
            QueueItem[] recItems = (QueueItem[]) items.toArray(new QueueItem[] {});
            queueVector.addAll(recItems);

            // Make sure that items with status QUEUED gets put in the queuedItemsList
            for (int i = 0; i < recItems.length; i++)
            {
               if (recItems[i].getStatus() == QueueItem.QUEUED) queuedItemsList.add(recItems[i]);

               queueItemAdded(recItems[i]);
            }

            this.getLock().notifyAll();
         }

         logInfo(items.size() + " QueueItems restored from persistent storage.");
      
         queueRestored = true;
      }
   }

   /**
    * Performs disabling functionality for this Queue.
    */
   protected void doShutDown()
   {
      super.doShutDown();

      if (queueStorage instanceof SubComponent)
      {
         ((SubComponent) queueStorage).shutDown();
      }
   }

   private void queueItemAdded(QueueItem item)
   {
      item.setQueue(this);
      queueItemAddNotification(item);
   }

   /**
    * Notification method called when a QueueItem is added to this Queue.
    * 
    * @param item the QueueItem that was added.
    */
   protected void queueItemAddNotification(QueueItem item)
   {
   }

   private void queueItemRemoved(QueueItem item)
   {
      item.setQueue(null);
      queueItemRemoveNotification(item);
   }

   /**
    * Notification method called when a QueueItem is removed from this Queue.
    * 
    * @param item the QueueItem that was removed.
    */
   protected void queueItemRemoveNotification(QueueItem item)
   {
   }

   /**
    * Notification method called when a QueueItem in this Queue changes status.
    * 
    * @param item the QueueItem that was removed.
    * @param oldStatus the status the QueueItem had before the status change.
    * @param newStatus the new status of the QueueItem.
    */
   protected void queueItemStatusChangeNotification(QueueItem item, int oldStatus, int newStatus)
   {
   }

   /**
    * This method redirectes to the method with the same name in the associated VectorProperty object that sets a flag
    * indicating if VectorPropertyEvents should be dispatched.<br>
    * <br>
    * The Queue class sets the default value of this flag to <code>false</code> in it's constructors.
    * 
    * @param enabled the new value of the flag.
    * @see com.teletalk.jserver.property.VectorProperty#setVectorPropertyEventsEnabled(boolean)
    * @see com.teletalk.jserver.event.VectorPropertyEvent
    */
   public void setVectorPropertyEventsEnabled(boolean enabled)
   {
      this.queueVector.setVectorPropertyEventsEnabled(enabled);
   }

   /**
    * This method redirectes to the method with the same name in the associated VectorProperty object that gets the
    * value of a flag indicating if VectorPropertyEvents should be dispatched.<br>
    * <br>
    * The Queue class sets the default value of this flag to <code>false</code> in it's constructors.
    * 
    * @return the value of the flag.
    * @see com.teletalk.jserver.property.VectorProperty#isVectorPropertyEventsEnabled()
    * @see com.teletalk.jserver.event.VectorPropertyEvent
    */
   public boolean isVectorPropertyEventsEnabled()
   {
      return this.queueVector.isVectorPropertyEventsEnabled();
   }

   /**
    * Returns an iterator for this Queue.
    * 
    * @return a VectorProperty.VectorPropertyIterator object.
    */
   public VectorProperty.VectorPropertyIterator iterator()
   {
      if (isDebugMode()) logDebug("iterator().");
      return queueVector.iterator();
   }

   /**
    * Updates the persistent state of a QueueItem object stored in this queue.
    * 
    * @param item a QueueItem to be re-stored.
    * @return <code>true</code> if the persistent storage of the specified item was successfully updated, otherwise
    *         <code>false</code>.
    */
   public boolean updatePersistentStorage(final QueueItem item)
   {
      // Kanske lägga till en flagga som säger om bara själva QueueItem informationen måste uppdateras.....
      if (isDebugMode()) logDebug("updatePersistentStorage - " + item + ". ");

      synchronized(this.getLock())
      {
         if (!containsItemId(item.getId()))
         {
            logWarning("Error while trying to update persistent storage for item '" + item.toString() + "' - Item isn't in this Queue. No update of persistent state will be performed!");
            return false;
         }
         else
         {
            try
            {
               queueStorage.updateStoredQueueItem(item);
   
               return true;
            }
            catch (QueueStorageException qse)
            {
               logWarning("Error while trying to update persistent storage for item '" + item.toString() + "'. The following error occurred: " + qse + ".");
               return false;
            }
         }
      }
   }

   /**
    * Adds a QueueItem to the end of the Queue and notifies threads that are waiting for queued objects.
    * 
    * @param item a QueueItem to be added to the Queue.
    * @exception QueueStorageException if a QueueItem with the same id as that of the QueueItem specified by parameter
    *               <code>item</code> already exists in this Queue, or if an error occurs while adding the QueueItem
    *               to persistent storage in the associated QueueStorage object.
    * @return <code>true</code> if the item was successfully added, otherwise <code>false</code>.
    */
   public boolean add(final QueueItem item) throws QueueStorageException
   {
      return this.add(item, true);
   }

   /**
    * Adds a QueueItem to the end of the Queue.
    * 
    * @param item a QueueItem to be added to the Queue.
    * @param addToQueuedList boolean flag indicating if the item should be added to the list of queued items and 
    *                                           if threads that are waiting for queued objects should be notify after adding. If this flag is set to false, the method 
    *                                           {@link #addToQueuedList(QueueItem)} or {@link #addToQueuedList(QueueItem[])} MUST be called at some later point in 
    *                                           time to make sure the item are marked as queued correctly.
    * @exception QueueStorageException if a QueueItem with the same id as that of the QueueItem specified by parameter
    *               <code>item</code> already exists in this Queue, or if an error occurs while adding the QueueItem
    *               to persistent storage in the associated QueueStorage object.
    * @return <code>true</code> if the item was successfully added, otherwise <code>false</code>.
    */
   public boolean add(final QueueItem item, boolean addToQueuedList) throws QueueStorageException
   {
      if (isDebugMode()) logDebug("add - " + item);

      if (!isEnabled()) throw new RuntimeException("Unable to add item (" + item + ")! Queue isn't enabled!");

      if (item == null) return false;

      boolean itemWasAdded = false;

      boolean notifyNeeded = false;

      synchronized (this.getLock())
      {
         if (queueVector.add(item)) // If the item did not already exist...
         {
            itemWasAdded = true;
            if (item.getStatus() == QueueItem.QUEUED) // Only add the item to the queuedItemsList if status is QUEUED
            {
               notifyNeeded = queuedItemsList.isEmpty();

               if( addToQueuedList ) queuedItemsList.add(item);
               queueItemAdded(item);
            }
            else queueItemAdded(item);
         
            boolean storeSuccess = false;
            try
            {
               // Add item to persistent storage
               queueStorage.storeQueueItem(item);
               storeSuccess = true;
            }
            finally
            {
               if (!storeSuccess)
               {
                  // If there was an error adding the item to persistent storage, remove it from the queue
                  queueVector.remove(item);
                  queuedItemsList.remove(item);
               }
            }
         }
         else 
         {
            logWarning("Attempting to add item already in queue! Item: " + item + ".");
         }
   
         if (notifyNeeded && addToQueuedList)
         {
            this.getLock().notifyAll();
         }
      }

      return itemWasAdded;
   }
   
   /**
    * Adds a QueueItem to the end of the Queue and notifies threads that are waiting for queued objects.
    * 
    * @param items a QueueItem to be added to the Queue.
    * @exception QueueStorageException if a QueueItem with the same id as that of the QueueItem specified by parameter
    *               <code>item</code> already exists in this Queue, or if an error occurs while adding the QueueItem
    *               to persistent storage in the associated QueueStorage object.
    */
   public void add(final QueueItem[] items) throws QueueStorageException
   {
      this.add(items, true);
   }

   /**
    * Adds a QueueItem to the end of the Queue.
    * 
    * @param items queueItems to be added to the Queue.
    * @param addToQueuedList boolean flag indicating if the items should be added to the list of queued items and 
    *                                           if threads that are waiting for queued objects should be notify after adding. If this flag is set to false, the method 
    *                                           {@link #addToQueuedList(QueueItem)} or {@link #addToQueuedList(QueueItem[])} MUST be called at some later point in 
    *                                           time to make sure the items are marked as queued correctly.
    * @exception QueueStorageException if a QueueItem with the same id as that of the QueueItem specified by parameter
    *               <code>item</code> already exists in this Queue, or if an error occurs while adding the QueueItem
    *               to persistent storage in the associated QueueStorage object.
    */
   public boolean[] add(final QueueItem[] items, boolean addToQueuedList) throws QueueStorageException
   {
      if (isDebugMode()) logDebug("add - QueueItem[]. ");

      if (!isEnabled())
      {
         throw new RuntimeException("Unable to add items (" + QueueItem.concatIds(items) + ")! Queue isn't enabled!");
      }

      QueueItem[] itemsToAdd = items;
      boolean[] addResult = new boolean[items.length];
      int itemsToAddIndex = 0;
      
      // Add items to the list for items with status QUEUED...
      synchronized (this.getLock())
      {
         boolean notifyNeeded = queuedItemsList.isEmpty();
         
         for (int i = 0; i < items.length; i++)
         {
            if (items[i] != null)
            {
               // ...but only if the item didn't already exist in this queue
               if (queueVector.add(items[i])) // If the item did not already exist...
               {
                  addResult[i] = true;
                  itemsToAddIndex++;

                  if (items[i].getStatus() == QueueItem.QUEUED) // Only add the item to the queuedItemsList if
                                                                  // status is QUEUED
                  {
                     if( addToQueuedList ) queuedItemsList.add(items[i]);
                     queueItemAdded(items[i]);
                  }
                  else queueItemAdded(items[i]);
               }
               else
               {
                  addResult[i] = false;
                  logWarning("Attempting to add item already in queue! Item: " + items[i] + ".");
                  
                  // "Remove" item from itemsToAdd array
                  QueueItem[] newItemsToAdd = new QueueItem[itemsToAdd.length - 1];

                  System.arraycopy(itemsToAdd, 0, newItemsToAdd, 0, itemsToAddIndex);
                  System.arraycopy(itemsToAdd, itemsToAddIndex + 1, newItemsToAdd, itemsToAddIndex, (itemsToAdd.length - itemsToAddIndex - 1));

                  itemsToAdd = newItemsToAdd;
               }
            }
         }
   
         if (itemsToAdd.length > 0)
         {
            boolean storeSuccess = false;
            try
            {
               // Add items to persistent storage
               queueStorage.storeQueueItems(itemsToAdd);
               storeSuccess = true;
            }
            finally
            {
               if (!storeSuccess)
               {
                  // If there was an error adding the item to persistent storage, remove it from the queue
                  for (int i = 0; i < itemsToAdd.length; i++)
                  {
                     queueVector.remove(itemsToAdd[i]);
                     queuedItemsList.remove(itemsToAdd[i]);
                  }
               }
            }
         }
   
         if (notifyNeeded && addToQueuedList)
         {
            this.getLock().notifyAll();
         }
      }
      
      return addResult;
   }
   
   /**
    * Adds a queue item to the list containing items with status QUEUED.
    * 
    * @since 2.1.2 (20060224)
    */
   public void addToQueuedList(final QueueItem item)
   {
      if (isDebugMode()) logDebug("addToQueuedList(QueueItem).");
      
      synchronized (this.getLock())
      {
         // Only add the item to the queuedItemsList if status is QUEUED and it exists in this queue
         if ( this.queueVector.contains(item) && (item.getStatus() == QueueItem.QUEUED) && !queuedItemsList.contains(item) )
         {
            queuedItemsList.add(item);
         }
         
         this.getLock().notifyAll();
      }
   }
   
   /**
    * Adds queue items to the list containing items with status QUEUED.
    * 
    * @since 2.1.2 (20060224)
    */
   public void addToQueuedList(final QueueItem[] items)
   {
      if (isDebugMode()) logDebug("addToQueuedList(QueueItem[]).");
      
      synchronized (this.getLock())
      {
         for(int i=0; i<items.length; i++)
         {
            // Only add the item to the queuedItemsList if status is QUEUED and it exists in this queue
            if ( this.queueVector.contains(items[i]) && (items[i].getStatus() == QueueItem.QUEUED) && !queuedItemsList.contains(items[i]) )
            {
               queuedItemsList.add(items[i]);
            }
         }
         
         this.getLock().notifyAll();
      }
   }   

   /**
    * Gets the first QueueItem in the queue that hasn't already been checked out(status = QUEUED). If the queue is
    * empty, null is returned.
    * 
    * @return a QueueItem or null if there were no items in this Queue.
    */
   public QueueItem getFirstIfAny()
   {
      if (isDebugMode()) logDebug("getFirstIfAny().");

      synchronized (this.getLock())
      {
         if( queuedItemsList.size() > 0 ) return (QueueItem) queuedItemsList.getFirst();
         else return null;
      }
   }

   /**
    * Gets the first QueueItem in the queue that hasn't already been checked out(status = QUEUED). If the queue is
    * empty, the calling thread will block until a QueueItem gets put into the queue.
    * 
    * @return a QueueItem or null if an error occured.
    * @exception InterruptedException if the calling thread was interrupted while waiting.
    */
   public QueueItem getFirst() throws InterruptedException
   {
      if (isDebugMode()) logDebug("getFirst().");

      synchronized (this.getLock())
      {
         while (!hasQueuedItems())
         {
            this.getLock().wait(30 * 1000); // Use a time out in case conditions have changed without notifying the current thread...
         }

         return (QueueItem) queuedItemsList.getFirst();
      }
   }

   /**
    * Checkes out the first QueueItem in the Queue that hasn't already been checked out(status = QUEUED). If the queue
    * is empty, the calling thread will block until a QueueItem gets put into the queue.
    * 
    * @return a QueueItem or null if there isn't any QueueItems in the Queue that hasn't already been checked out (e.g.
    *         status!=QueueItem.QUEUED).
    * @exception InterruptedException if the calling thread was interrupted while waiting.
    */
   public QueueItem checkOutFirst() throws InterruptedException
   {
      QueueItem item = null;

      synchronized (this.getLock())
      {
         while (!hasQueuedItems())
         {
            this.getLock().wait(30 * 1000); // Use a time out in case conditions have changed without notifying the current thread...
         }

         item = (QueueItem) queuedItemsList.removeFirst();
      }

      if (item != null)
      {
         if (item.getStatus() == QueueItem.QUEUED) changeStatus(item, QueueItem.CHECKED_OUT);
      }

      if (isDebugMode()) logDebug("checkOutFirst() - " + item + ".");

      return item;
   }

   /**
    * Gets the first n QueueItems in the Queue that hasn't already been checked out(status = QUEUED). If the queue is
    * empty, the calling thread will block until a QueueItem gets put into the queue.
    * 
    * @param n the number of items to get.
    * @return a vector of QueueItems. The vector will be empty if there isn't any QueueItems in the Queue that hasn't
    *         already been checked out (e.g. status!=QueueItem.QUEUED).
    * @exception InterruptedException if the calling thread was interrupted while waiting.
    */
   public Vector getFirstN(int n) throws InterruptedException
   {
      if (isDebugMode()) logDebug("getFirstN - " + n + ".");

      synchronized (this.getLock())
      {
         while (!hasQueuedItems())
         {
            this.getLock().wait(30 * 1000); // Use a time out in case conditions have changed without notifying the current thread...
         }

         if (queuedItemsList.size() < n) n = queuedItemsList.size();

         Vector firstN = new Vector();
         QueueItem item;

         for (int i = 0; i < n; i++)
         {
            item = (QueueItem) queuedItemsList.get(i);
            firstN.add(item);
         }

         return firstN;
      }
   }

   /**
    * Checks out the first n QueueItems in the Queue that hasn't already been checked out(status = QUEUED). If the queue
    * is empty, the calling thread will block until a QueueItem gets put into the queue. This method will return if
    * there is at least one item with status QUEUED in the queue, which means that the number of items in the returned
    * vector will be between one and <code>n</code>.
    * 
    * @param n the number of items to get.
    * @return a vector of QueueItems. This vector will contain between one and <code>n</code> QueueItems.
    * @exception InterruptedException if the calling thread was interrupted while waiting.
    */
   public Vector checkOutFirstN(int n) throws InterruptedException
   {
      if (isDebugMode()) logDebug("checkOutFirstN - " + n + ".");

      QueueItem item;
      Vector firstN = new Vector();

      synchronized (this.getLock())
      {
         while (!hasQueuedItems())
         {
            this.getLock().wait(30 * 1000); // Use a time out in case conditions have changed without notifying the current thread...
         }

         if (queuedItemsList.size() < n) n = queuedItemsList.size();

         for (int i = 0; i < n; i++)
         {
            item = (QueueItem) queuedItemsList.removeFirst(); // remove(i);
            firstN.add(item);
         }
      }

      for (int i = 0; i < firstN.size(); i++)
      {
         item = (QueueItem) firstN.get(i);
         changeStatus(item, QueueItem.CHECKED_OUT);
      }

      return firstN;
   }

   /**
    * Gets the QueueItem with the specified item id.
    * 
    * @param itemId a string identifying a QueueItem.
    * @return a QueueItem object.
    */
   public final QueueItem get(String itemId)
   {
      if (isDebugMode()) logDebug("get - " + itemId + ".");

      return (QueueItem) queueVector.get(itemId);
   }

   /**
    * Gets the QueueItems with the specified item ids.
    * 
    * @param itemIds an array of strings identifying QueueItems.
    * @return an array of QueueItem objects.
    */
   public final QueueItem[] get(String[] itemIds)
   {
      if (isDebugMode()) logDebug("get - " + QueueItem.concatIds(itemIds) + ".");

      return (QueueItem[]) queueVector.get(itemIds, new QueueItem[itemIds.length]);
   }

   /**
    * Checkes out the QueueItem with the specified item id if it hasn't already been checked out(status = QUEUED).
    * 
    * @param itemId a string identifying a QueueItem.
    * @return a QueueItem object, null if no item with the specified item id was found or if the item was already
    *         checked out.
    */
   public final QueueItem checkOut(String itemId)
   {
      if (isDebugMode()) logDebug("checkOut - " + itemId + ".");

      QueueItem item;

      synchronized (this.getLock())
      {
         item = (QueueItem) queueVector.get(itemId);

         if (item != null)
         {
            queuedItemsList.remove(item);
            changeStatus(item, QueueItem.CHECKED_OUT);
         }
      }
      return item;
   }

   /**
    * Checkes out the specified QueueItem if it hasn't already been checked out(status = QUEUED).
    * 
    * @param item a QueueItem object.
    * @return true if the item was successfully checked out, otherwise false.
    */
   public final boolean checkOut(QueueItem item)
   {
      if (isDebugMode()) logDebug("checkOut - " + item + ".");

      boolean existed = false;

      if (item != null)
      {
         synchronized (this.getLock())
         {
            existed = queuedItemsList.remove(item);
            
            if (existed) changeStatus(item, QueueItem.CHECKED_OUT);
         }
      }
      return existed;
   }

   /**
    * Gets all QueueItems with the specidied parent id.
    * 
    * @param parentItemId a string identifying a parent QueueItem.
    * @return an array of QueueItems. The array will have the size 0 if this queue contained no items with the specified
    *         parentItemId.
    */
   public final QueueItem[] getWithParentId(String parentItemId)
   {
      if (isDebugMode()) logDebug("getWithParentId - " + parentItemId + ".");

      ArrayList v = new ArrayList();

      synchronized (this.getLock())
      {
         VectorProperty.VectorPropertyIterator it = iterator();
         QueueItem item;
         String parentId;
   
         while (it.hasNext())
         {
            item = (QueueItem) it.next();
   
            parentId = item.getParentId();
   
            if (parentId != null)
            {
               if (parentId.equals(parentItemId)) v.add(item);
            }
         }
   
         it = null;
      }

      return (QueueItem[]) v.toArray(new QueueItem[v.size()]);
   }

   /**
    * Gets the QueueItem thats wraps around the specified object.
    * 
    * @param itemData an object that is contained inside a QueueItem.
    * @return the QueueItem that wraps around the specified object or null if no such item was found.
    */
   public final QueueItem getQueueItemFor(QueueItemData itemData)
   {
      if (isDebugMode()) logDebug("getQueueItemFor - QueueItemData itemData.");

      QueueItem result = null;
      QueueItem qItem;

      synchronized (this.getLock())
      {
         VectorProperty.VectorPropertyIterator it = iterator();
      
         while (it.hasNext())
         {
            qItem = (QueueItem) it.next();
      
            if (qItem.getItemData().equals(itemData))
            {
               result = qItem;
               break;
            }
         }
      
         it = null;
      }

      return result;
   }

   /**
    * Gets all the QueueItems stored in this Queue.
    * 
    * @return a Vector containing all QueueItem objects in this Queue.
    */
   public Vector getAll()
   {
      if (isDebugMode()) logDebug("getAll().");

      final Vector all = new Vector();

      synchronized (this.getLock())
      {
         VectorProperty.VectorPropertyIterator it = queueVector.iterator();
   
         while (it.hasNext())
         {
            all.add(it.next());
         }
      
         it = null;
      }

      return all;
   }
   
   /**
    * Gets all the QueueItems stored in this Queue.
    * 
    * @since 2.1.2 (20060224)
    */
   public ArrayList getAllAsList()
   {
      if (isDebugMode()) logDebug("getAllAsList().");

      return queueVector.getItemsAsList();
   }

   /**
    * Gets all the QueueItems stored in this Queue that hasn't been checked out yet (status = QUEUED).
    * 
    * @return a Vector containing all QueueItem objects in this Queue with status = QUEUED.
    */
   public Vector getAllQueued()
   {
      if (isDebugMode()) logDebug("getAllQueued().");

      synchronized (this.getLock())
      {
         return new Vector(queuedItemsList);
      }
   }
   
   /**
    * Gets all the QueueItems stored in this Queue that hasn't been checked out yet (status = QUEUED).
    * 
    * @since 2.1.2 (20060224)
    */
   public ArrayList getAllQueuedAsList()
   {
      if (isDebugMode()) logDebug("getAllQueuedAsList().");

      synchronized (this.getLock())
      {
         return new ArrayList(queuedItemsList);
      }
   }

   /**
    * Gets all the QueueItems stored in this Queue with the specified status.
    * 
    * @param status the status value to search for.
    * @return a Vector containing all QueueItem objects in this Queue with the specified status.
    */
   public Vector getAllWithStatus(final int status)
   {
      if (isDebugMode()) logDebug("getAllWithStatus - " + status + ". ");

      final Vector allWithStatus = new Vector();
      this.getAllWithStatusAsList(status, allWithStatus);
            
      return allWithStatus;
   }
   
   /**
    * Gets all the QueueItems stored in this Queue with the specified status.
    * 
    * @since 2.1.2 (20060224)
    */
   public ArrayList getAllWithStatusAsList(final int status)
   {
      if (isDebugMode()) logDebug("getAllWithStatusAsList - " + status + ". ");

      final ArrayList allWithStatus = new ArrayList();
      this.getAllWithStatusAsList(status, allWithStatus);
      
      return allWithStatus;
   }
      
   /**
    * Gets all completed QueueItems stored in this Queue.
    * 
    * @since 2.1.5 (20070417)
    */
   public ArrayList getAllCompletedAsList()
   {
      if (isDebugMode()) logDebug("getAllCompletedAsList ");

      final ArrayList allWithStatus = new ArrayList();
      this.getAllWithStatusAsList(GET_ALL_COMPLETED, allWithStatus);
      
      return allWithStatus;
   }
   
   /**
    * Gets all QueueItems stored in this Queue that are not completed.
    * 
    * @since 2.1.5 (20070417)
    */
   public ArrayList getAllNotCompletedAsList()
   {
      if (isDebugMode()) logDebug("getAllNotCompletedAsList. ");

      final ArrayList allWithStatus = new ArrayList();
      this.getAllWithStatusAsList(GET_ALL_NOT_COMPLETED, allWithStatus);
      
      return allWithStatus;
   }
   
   /**
    * Gets all the QueueItems stored in this Queue with the specified status (or all completed or all not completed).
    * 
    * @since 2.1.5 (20070417)
    */
   protected void getAllWithStatusAsList(final int status, final List resultList)
   {
      QueueItem item;

      synchronized (this.getLock())
      {
         VectorProperty.VectorPropertyIterator it = queueVector.iterator();
   
         while (it.hasNext())
         {
            item = (QueueItem) it.next();
            switch(status)
            {
               case GET_ALL_COMPLETED: 
                  if ( item.isCompleted() ) resultList.add(item);
                  break;
               case GET_ALL_NOT_COMPLETED: 
                  if ( !item.isCompleted() ) resultList.add(item);
                  break;
               default:
                  if ( item.getStatus() == status) resultList.add(item);
               break;
            }
         }
   
         it = null;
      }
   }

   /**
    * Gets all the QueueItems in this Queue that was restored from persistent storage.
    * 
    * @return a Vector containing all QueueItem objects in this Queue that was restored from persistent storage.
    */
   public Vector getAllRecovered()
   {
      if (isDebugMode()) logDebug("getAllRecovered().");

      QueueItem item;
      final Vector allRestored = new Vector();

      synchronized (this.getLock())
      {
         VectorProperty.VectorPropertyIterator it = queueVector.iterator();
   
         while (it.hasNext())
         {
            item = (QueueItem) it.next();
            if (item.isRecoveredFromPersistentStorage()) allRestored.add(item);
         }
   
         it = null;
      }

      return allRestored;
   }
   
   /**
    * Gets all the QueueItems in this Queue that was restored from persistent storage.
    * 
    * @since 2.1.2 (20060224)
    */
   public ArrayList getAllRecoveredAsList()
   {
      if (isDebugMode()) logDebug("getAllRecoveredAsList().");

      QueueItem item;
      final ArrayList allRestored = new ArrayList();

      synchronized (this.getLock())
      {
         VectorProperty.VectorPropertyIterator it = queueVector.iterator();
   
         while (it.hasNext())
         {
            item = (QueueItem) it.next();
            if (item.isRecoveredFromPersistentStorage()) allRestored.add(item);
         }
   
         it = null;
      }

      return allRestored;
   }
   

   /**
    * Gets all QueueItems with the specidied parent id.
    * 
    * @param parentItemId a string identifying a parent QueueItem.
    * @return an array of QueueItems. The array will have the size 0 if this queue contained no items with the specified
    *         parentItemId.
    */
   public final QueueItem[] getAllRecoveredWithParentId(String parentItemId)
   {
      if (isDebugMode()) logDebug("getAllRecoveredWithParentId - " + parentItemId + ".");

      ArrayList v = new ArrayList();

      synchronized (this.getLock())
      {
         VectorProperty.VectorPropertyIterator it = iterator();
         QueueItem item;
         String parentId;
   
         while (it.hasNext())
         {
            item = (QueueItem) it.next();
   
            parentId = item.getParentId();
   
            if (parentId != null)
            {
               if ((parentId.equals(parentItemId)) && (item.isRecoveredFromPersistentStorage())) v.add(item);
            }
         }
         
         it = null;
      }

      return (QueueItem[]) v.toArray(new QueueItem[v.size()]);
   }

   /**
    * Gets the QueueItem at the specified index.
    * 
    * @param index the specified index.
    * @return the QueueItem at the specified index.
    */
   public final QueueItem get(int index)
   {
      if (isDebugMode()) logDebug("get - " + index + ".");

      return (QueueItem) queueVector.get(index);
   }

   /**
    * Gets the number of QueueItems in this Queue.
    * 
    * @return the number of QueueItems in this Queue.
    */
   public final int size()
   {
      if (isDebugMode()) logDebug("size().");

      return queueVector.size();
   }

   /**
    * Removes all the elements in this Queue. The elements will also be removed from persistent storage.
    */
   public void clear()
   {
      if (isDebugMode()) logDebug("clear().");

      synchronized (this.getLock())
      {
         VectorProperty.VectorPropertyIterator it = iterator();

         while (it.hasNext())
         {
            remove((QueueItem) it.next());
         }
      }
      /*
       * synchronized(this.queueVector) { this.queueVector.clear(); this.queuedItemsList.clear(); }
       */
   }

   /**
    * Removes the first QueueItem in this Queue and returns it.
    * 
    * @return the removed QueueItem or null if the Queue was empty.
    */
   public QueueItem removeFirst()
   {
      if (isDebugMode()) logDebug("removeFirst().");

      QueueItem item = null;

      synchronized (this.getLock())
      {
         item = (QueueItem) queueVector.removeFirst();
   
         if (item != null)
         {
            queuedItemsList.remove(item);
            queueItemRemoved(item);
         
            queueStorage.removeStoredQueueItem(item);
         }
      }
      return item;
   }

   /**
    * Removes a QueueItem from this Queue.
    * 
    * @param itemId the item id of the object to be removed.
    * @return the removed QueueItem or null if none was found.
    */
   public final QueueItem remove(final String itemId)
   {
      if (isDebugMode()) logDebug("remove  - " + itemId + ". ");

      QueueItem item = null;
      
      synchronized (this.getLock())
      {
         item = (QueueItem) queueVector.remove(itemId);
   
         if (item != null)
         {
            queuedItemsList.remove(item);
            queueItemRemoved(item);
            
            queueStorage.removeStoredQueueItem(item);
         }
         else queueStorage.removeStoredQueueItem(new QueueItem(null, itemId));
      }

      return item;
   }

   /**
    * Removes a QueueItem from this Queue.
    * 
    * @param item the QueueItem to be removed.
    * @return true if the QueueItem was found in this Queue and removed, otherwise false.
    */
   public final boolean remove(final QueueItem item)
   {
      if (isDebugMode()) logDebug("remove  - " + item + ". ");

      boolean result = false;

      if (item != null)
      {
         synchronized (this.getLock())
         {
            queuedItemsList.remove(item);
            queueItemRemoved(item);

            result = queueVector.remove(item);
            
            queueStorage.removeStoredQueueItem(item);
         }

         return result;
      }

      return false;
   }

   /**
    * Changes the status of a QueueItem. This is the preferred way to change the status of a QueueItem to ensure that
    * the status change is properly reflected in the persistent state of the item.
    * 
    * @param item the QueueItem to change status for.
    * @param newStatus the new status value for the QueueItem.
    * @return <true> if the new status was successfully reflected on the persistent counterpart of the QueueItem,
    *         otherwise <false>.
    */
   public final boolean changeStatus(final QueueItem item, final short newStatus)
   {
      if (isDebugMode()) logDebug("changeStatus  - " + item + ", newStatus: " + QueueItem.statusNames[newStatus] + ". ");
      boolean doNotify = false;

      synchronized (this.getLock())
      {
         short oldStatus = item.getStatus();

         if (!containsItemId(item.getId()))
         {
            logWarning("Warning! Unable to reflect status change (new status: " + QueueItem.statusNames[newStatus] + ") of item  '" + item.toString()
                  + "' on persistet counterpart. The specified Item isn't in this Queue!", new Throwable());
            item.forceStatus(newStatus);

            return false;
         }
         else
         {
            if ((oldStatus == QueueItem.QUEUED) && (newStatus != QueueItem.QUEUED)) // If item is currently queued and
                                                                                    // the new status is not QUEUED...
            {
               this.queuedItemsList.remove(item); // ...remove the item from the list for queued items
               doNotify = true;
            }
            else if ((oldStatus != QueueItem.QUEUED) && (newStatus == QueueItem.QUEUED)) // If item is not currently
                                                                                          // queued, but the new status
                                                                                          // is QUEUED...
            {
               this.queuedItemsList.add(item); // ...add the item to the list for queued items
               doNotify = true;
            }

            try
            {
               item.forceStatus(newStatus);
               queueStorage.updateQueueItemStatus(item);

               queueItemStatusChangeNotification(item, oldStatus, newStatus);

               if (doNotify)
               {
                  this.getLock().notifyAll();
               }
               return true;
            }
            catch (QueueStorageException qse)
            {
               logWarning("Warning! Unable to reflect status change (new status: " + QueueItem.statusNames[newStatus] + ") of item  '" + item.toString()
                     + "' on persistet counterpart. The following error occurred: " + qse + ".");
               if (doNotify)
               {
                  this.getLock().notifyAll();
               }
               return false;
            }
         }
      }
   }

   /**
    * Checkes if the Queue contains any items that with status QUEUED.
    * 
    * @return true if the contains at least one item with status QUEUED, otherwise false.
    */
   public final boolean hasQueuedItems()
   {
      if (isDebugMode()) logDebug("hasQueuedItems().");

      synchronized (this.getLock())
      {
         return !queuedItemsList.isEmpty();
      }
   }

   /**
    * Checkes if the specified QueueItem is a contained in this Queue.
    * 
    * @param itemId the id of a QueueItem object check for.
    * @return true if the specified QueueItem is contained in this Queue as determined by the equals method; false
    *         otherwise.
    */
   public final boolean containsItemId(String itemId)
   {
      if (isDebugMode()) logDebug("containsItemId  - " + itemId + ".");

      return queueVector.containsKey(itemId);
   }

   /**
    * Checkes if the specified QueueItem is a contained in this Queue.
    * 
    * @param item a QueueItem object check for.
    * @return true if the specified QueueItem is contained in this Queue as determined by the equals method; false
    *         otherwise.
    */
   public final boolean contains(QueueItem item)
   {
      if (isDebugMode()) logDebug("contains  - " + item + ".");

      return queueVector.contains(item);
   }

   /**
    * Checkes if the specified object is contained in a QueueItem that is contained in this Queue.
    * 
    * @param itemData a object to check for.
    * @return true if the specified itemData object is contained in a QueueItem that is contained in this Queue as
    *         determined by the equals method; false otherwise.
    */
   public final boolean containsItemData(QueueItemData itemData)
   {
      if (isDebugMode()) logDebug("containsItemData  - QueueItemData itemData.");

      boolean result = false;
      QueueItem qItem;

      synchronized (this.getLock())
      {
         VectorProperty.VectorPropertyIterator it = queueVector.iterator();
   
         while (it.hasNext())
         {
            qItem = (QueueItem) it.next();
   
            if (qItem.getItemData().equals(itemData))
            {
               result = true;
               break;
            }
         }
   
         it = null;
      }

      return result;
   }

   /**
    * Checkes whether or not this queue has any QueueItems.
    * 
    * @return true if the queue has QueueItems, otherwise false.
    */
   public final boolean containsItems()
   {
      if (isDebugMode()) logDebug("containsItems().");

      return !queueVector.isEmpty();
   }

   /**
    * Blocks the calling thread until a QueueItem gets put into the queue or the specified waitTime elapses.<br>
    * <br>
    * Note: This method may return before a queue item with the status QUEUED is available. To wait for QUEUED items, use 
    * the method {@link #waitForQueuedItems(long)} instead.
    * 
    * @param maxWait maximum time in milliseconds to block the calling thread.
    * 
    * @see #waitForQueuedItems(long)
    */
   public final void waitForData(long maxWait) throws InterruptedException
   {
      if (isDebugMode()) logDebug("waitForData - " + maxWait + ".");

      long waitStart = System.currentTimeMillis();
      long waitTime;

      synchronized (this.getLock())
      {
         while (!containsItems())
         {
            waitTime = maxWait - (System.currentTimeMillis() - waitStart);
   
            if (waitTime > 0)
            {
               this.getLock().wait(waitTime);
            }
            else break;
         }
      }
   }

   /**
    * Blocks the calling thread until a QueueItem gets put into the queue.<br>
    * <br>
    * Note: This method may return before a queue item with the status QUEUED is available. To wait for QUEUED items, use 
    * the method {@link #waitForQueuedItems()} instead.
    * 
    * @see #waitForQueuedItems()
    */
   public final void waitForData() throws InterruptedException
   {
      if (isDebugMode()) logDebug("waitForData().");

      synchronized (this.getLock())
      {
         while (!containsItems())
         {
            this.getLock().wait();
         }
      }
   }

   /**
    * Blocks the calling thread until there is at least one QueueItem in the queue with status QUEUED, or the specified
    * waitTime elapses.
    * 
    * @param maxWait maximum time in milliseconds to block the calling thread.
    */
   public final void waitForQueuedItems(long maxWait) throws InterruptedException
   {
      if (isDebugMode()) logDebug("waitForQueuedItems - " + maxWait + ".");

      long waitStart = System.currentTimeMillis();
      long waitTime;

      synchronized (this.getLock())
      {
         while (!hasQueuedItems())
         {
            waitTime = maxWait - (System.currentTimeMillis() - waitStart);
   
            if (waitTime > 0)
            {
               this.getLock().wait(waitTime);
            }
            else break;
         }
      }
   }

   /**
    * Wakes up all threads that are locked in a waiting state in this Queue object.
    */
   public final void notifyWaitingThreads()
   {
      synchronized (this.getLock())
      {
         this.getLock().notifyAll();
      }
   }

   /**
    * Blocks the calling thread until there is at least one QueueItem in the queue with status QUEUED.
    */
   public final void waitForQueuedItems() throws InterruptedException
   {
      if (isDebugMode()) logDebug("waitForQueuedItems().");

      synchronized (this.getLock())
      {
         while (!hasQueuedItems())
         {
            this.getLock().wait();
         }
      }
   }

   /**
    * Fires an event (VectorPropertyEvent) indicating that the specified item was modified.
    * 
    * @param itemId the id of the item to fire and event for.
    * @see com.teletalk.jserver.event.VectorPropertyEvent
    */
   public void fireItemModified(String itemId)
   {
      this.fireItemModified(this.get(itemId));
   }

   /**
    * Fires an event (VectorPropertyEvent) indicating that the specified item was modified.
    * 
    * @param item the item to fire and event for.
    * @see com.teletalk.jserver.event.VectorPropertyEvent
    */
   public void fireItemModified(QueueItem item)
   {
      if (item != null) this.queueVector.fireItemModified(item);
   }

   /**
    * This method is called when an external operation is called on the VectorProperty object associated with this
    * Queue.
    * 
    * @param operationName the internal name of the operation that was called.
    * @param keys an array containing unique indices that was selected for the operation call.
    */
   public void externalOperationCalled(String operationName, String[] keys)
   {
      if (isDebugMode()) logDebug("externalOperationCalled - " + operationName + ", String[] keys.");

      if (owner != null) owner.externalQueueOperationCalled(operationName, keys);
   }

   /**
    * Adds an external operation to this Queue. <BR>
    * <BR>
    * This method simply relays the call to the same method in the VectorProperty object associated with this Queue.
    * 
    * @param internalName the internal name of the operation.
    * @param displayName the name of the operation used for displaying purposes.
    */
   public final void addExternalOperation(String internalName, String displayName)
   {
      if (isDebugMode()) logDebug("addExternalOperation - " + internalName + ", " + displayName + ".");

      queueVector.addExternalOperation(internalName, displayName);
   }

   /**
    * Adds an external operation to this Queue. The displayname will be the same as the internal name. <BR>
    * <BR>
    * This method simply relays the call to the same method in the VectorProperty object associated with this Queue.
    * 
    * @param name the internal name of the operation.
    */
   public final void addExternalOperation(String name)
   {
      if (isDebugMode()) logDebug("addExternalOperation - " + name + ".");

      queueVector.addExternalOperation(name);
   }

   /**
    * Removes an external operation from this Queue. <BR>
    * <BR>
    * This method simply relays the call to the same method in the VectorProperty object associated with this Queue.
    * 
    * @param name the internal name of the operation.
    */
   public final void removeExternalOperation(String name)
   {
      if (isDebugMode()) logDebug("removeExternalOperation - " + name + ".");

      queueVector.removeExternalOperation(name);
   }

   /**
    * Gets all external operations for this Queue. <BR>
    * <BR>
    * This method simply relays the call to the same method in the VectorProperty object associated with this Queue.
    * 
    * @return hashtable containing internal name/display name mappings of the operations.
    */
   public final HashMap getExternalOperations()
   {
      if (isDebugMode()) logDebug("getExternalOperations().");

      return queueVector.getExternalOperations();
   }

   /**
    * Gets the QueueStorage object associated with this Queue.
    * 
    * @return a QueueStorage object.
    */
   public final QueueStorage getQueueStorage()
   {
      if (isDebugMode()) logDebug("getQueueStorage().");

      return queueStorage;
   }

   /**
    * Sets the QueueStorage object for this Queue. If the old QueueStorage object, if any, is a SubComponent it is first
    * removed befor setting the new QueueStorage object. Also, if the new QueueStorage object is a SubComponent this
    * will be added as such an object.
    * 
    * @param queueStorage a new QueueStorage object to be set for this Queue.
    */
   public final void setQueueStorage(QueueStorage queueStorage)
   {
      if (isDebugMode()) logDebug("setQueueStorage - QueueStorage queueStorage.");

      if (this.queueStorage != null)
      {
         if (this.queueStorage instanceof SubComponent)
         {
            removeSubComponent((SubComponent) this.queueStorage);
            this.queueStorage = null;
         }
      }

      this.queueStorage = queueStorage;

      if (queueStorage instanceof SubComponent)
      {
         addSubComponent((SubComponent) queueStorage);
      }

      if (this.isEnabled())
      {
         if (queueStorage instanceof SubComponent) ((SubComponent) queueStorage).engage();

         restoreQueueItemsFromPersistentStorage();
      }
   }
}
