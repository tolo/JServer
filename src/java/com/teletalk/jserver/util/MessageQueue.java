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
package com.teletalk.jserver.util;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * The class MessageQueue represents a blocking message queue. This class can be useful as a message buffer for
 * asynchronous communication between threads.
 * 
 * @author Tobias Löfstrand
 * 
 * @since The beginning
 */
public class MessageQueue
{
   private final Object lock;
   
   protected final List queue;

   private boolean blockingModeEnabled;

   private int waitCounter;
   
   
   /**
    * Constructs a new MessageQueue.
    */
   public MessageQueue()
   {
      this(null, null);
   }
   
   /**
    * Constructs a new MessageQueue.
    * 
    * @since 2.1.5 (20070412)
    */
   public MessageQueue(final List queueList)
   {
      this(queueList, null);
   }
   
   /**
    * Constructs a new MessageQueue.
    * 
    * @since 2.1.2 (20060227)
    */
   public MessageQueue(final Object lockObject)
   {
      this(null, lockObject);
   }
   
   /**
    * Constructs a new MessageQueue.
    * 
    * @since 2.1.5 (20070412)
    */
   public MessageQueue(final List queueList, final Object lockObject)
   {
      if( lockObject != null ) this.lock = lockObject;
      else this.lock = this;
      
      this.queue = (queueList != null) ? queueList : new LinkedList();

      this.blockingModeEnabled = true;
   }
   
   /**
    * Gets the object that is used for synchronization of thread access in this object.
    * 
    * @since 2.1.2 (20060227)
    */
   public Object getLock()
   {
      return lock;
   }

   /**
    * Set the value of the flag indicating if methods in this object may block the calling thread.
    * 
    * @since 1.2
    */
   public void setBlockingModeEnabled(final boolean blockingModeEnabled)
   {
      synchronized(this.lock)
      {
         this.blockingModeEnabled = blockingModeEnabled;
         this.lock.notifyAll();
      }

   }

   /**
    * Get the value of the flag indicating if methods in this object may block the calling thread.
    * 
    * @since 1.2
    */
   public boolean isBlockingModeEnabled()
   {
      synchronized(this.lock)
      {
         return this.blockingModeEnabled;
      }
   }
   
   /**
    * Notifies waiting threads if needed. Note that a lock must be held on the {@link #getLock() queue lock} when this method is called.
    * 
    * @since 2.1.2 (20060227)
    */
   protected void notifyIfNeeded()
   {
      if (this.waitCounter > 0)
      {
         if (this.waitCounter == 1) this.lock.notify();
         else this.lock.notifyAll();
      }
   }
   
   /**
    * Puts a message in the MessageQueue.
    * 
    * @param msg message to be put in the MessageQueue.
    * 
    * @since 2.1.2 (20060227)
    */
   public void putMsg(final Object msg, final int position)
   {
      synchronized(this.lock)
      {
         this.queue.add(position, msg);
   
         this.notifyIfNeeded();
      }
   }

   /**
    * Puts a message in the MessageQueue.
    * 
    * @param msg message to be put in the MessageQueue.
    */
   public void putMsg(final Object msg)
   {
      synchronized(this.lock)
      {
         this.queue.add(msg);
   
         this.notifyIfNeeded();
      }
   }

   /**
    * Puts a message at the front the MessageQueue.
    * 
    * @param msg message to be put in the MessageQueue.
    */
   public void putUrgentMsg(final Object msg)
   {
      synchronized(this.lock)
      {
         this.queue.add(0, msg);
         
         this.notifyIfNeeded();
      }
   }

   /**
    * Gets (removes) the first message in the MessageQueue. If the MessageQueue is empty, the calling thread will block
    * until a message gets put into it.
    * 
    * @return a message.
    * 
    * @exception InterruptedException if the calling thread was interrupted while waiting.
    */
   public Object getMsg() throws InterruptedException
   {
      synchronized(this.lock)
      {
         try
         {
            this.waitCounter++;
   
            while (this.blockingModeEnabled && this.queue.isEmpty())
            {
               this.lock.wait();
            }
         }
         finally
         {
            this.waitCounter--;
         }
   
         if (this.blockingModeEnabled && !this.queue.isEmpty()) return this.queue.remove(0);
         else return null;
      }
   }

   /**
    * Gets (removes) the first message in the MessageQueue. If the MessageQueue is empty, the calling thread will block
    * until a message gets put into it or the specified time out ellapses. If, however, parameter timeOut is less or
    * equal to zero this method will block untill a message gets put in into the queue.
    * 
    * @param timeOut the maximum time in milliseconds to wait for data to be put in the queue.
    * 
    * @return a message.
    * 
    * @exception InterruptedException if the calling thread was interrupted while waiting.
    * 
    * @since 1.2
    */
   public Object getMsg(final long timeOut) throws InterruptedException
   {
      synchronized(this.lock)
      {
         final long beginWait = System.currentTimeMillis();
         long waitTime = timeOut;
   
         try
         {
            this.waitCounter++;
   
            while (this.blockingModeEnabled && this.queue.isEmpty() && (timeOut <= 0 || (waitTime > 0)))
            {
               if (timeOut <= 0) this.lock.wait();
               else this.lock.wait(waitTime);
   
               waitTime = timeOut - (System.currentTimeMillis() - beginWait);
            }
         }
         finally
         {
            this.waitCounter--;
         }
   
         if (this.blockingModeEnabled) return this.getMsgIfAny();
         else return null;
      }
   }

   /**
    * Gets a message from the MessageQueue. If the MessageQueue is empty this method returns <code>null</code> without
    * blocking the calling thread.
    * 
    * @return a message or null if the queue was empty.
    */
   public Object getMsgIfAny()
   {
      synchronized(this.lock)
      {
         if (!this.queue.isEmpty()) return this.queue.remove(0);
         else return null;
      }
   }

   /**
    * Looks at the first message in the queue without removing it.
    * 
    * @return a message or <code>null</code> if the queue was empty.
    */
   public Object peekMsg()
   {
      synchronized(this.lock)
      {
         if (!this.queue.isEmpty()) return queue.get(0);
         else return null;
      }
   }

   /**
    * Removes a message from this MessageQueue.
    * 
    * @param index the index of the message to be removed.
    * 
    * @return the removed message or null if none was found.
    */
   public Object remove(final int index)
   {
      synchronized(this.lock)
      {
         return queue.remove(index);
      }
   }

   /**
    * Removes the firest occurance of the message specified by parameter <code>msg</code>.
    * 
    * @param msg the message to be removed.
    * 
    * @return true if the message was found and removed, otherwise false.
    */
   public boolean remove(final Object msg)
   {
      synchronized(this.lock)
      {
         return queue.remove(msg);
      }
   }

   /**
    * Checkes whether or not this MessageQueue has any messages.
    * 
    * @return true if the MessageQueue has messages, otherwise false.
    */
   public boolean containsData()
   {
      synchronized(this.lock)
      {
         return !queue.isEmpty();
      }
   }

   /**
    * Checkes if this MessageQueue is empty.
    * 
    * @return true if the MessageQueue has no items, otherwise false.
    */
   public boolean isEmpty()
   {
      synchronized(this.lock)
      {
         return queue.isEmpty();
      }
   }

   /**
    * Checkes if the specified object is a contained in this MessageQueue.
    * 
    * @param o an object to check for.
    * 
    * @return true if the specified object is contained in this MessageQueue as determined by the equals method; false
    *                otherwise.
    */
   public boolean contains(final Object o)
   {
      synchronized(this.lock)
      {
         return queue.contains(o);
      }
   }

   /**
    * Returns the number of messages in this MessageQueue.
    * 
    * @return the number of messages in this MessageQueue.
    */
   public int size()
   {
      synchronized(this.lock)
      {
         return queue.size();
      }
   }

   /**
    * Blocks the calling thread until a message gets put into the MessageQueue or the specified waitTime elapses.
    * 
    * @param maxWait maximum time in milliseconds to block the calling thread.
    * 
    * @exception InterruptedException if the calling thread was interrupted while waiting.
    */
   public void waitForData(final long maxWait) throws InterruptedException
   {
      long waitStart = System.currentTimeMillis();
      long waitTime;

      synchronized(this.lock)
      {
         try
         {
            this.waitCounter++;

            while (this.blockingModeEnabled && this.queue.isEmpty())
            {
               waitTime = maxWait - (System.currentTimeMillis() - waitStart);

               if (waitTime > 0)
               {
                  this.lock.wait(waitTime);
               }
               else break;
            }
         }
         finally
         {
            this.waitCounter--;
         }
      }
   }

   /**
    * Blocks the calling thread until a message gets put into the MessageQueue.
    * 
    * @exception InterruptedException if the calling thread was interrupted while waiting.
    */
   public void waitForData() throws InterruptedException
   {
      synchronized(this.lock)
      {
         try
         {
            this.waitCounter++;
   
            while (this.blockingModeEnabled && !this.queue.isEmpty())
            {
               this.lock.wait();
            }
         }
         finally
         {
            this.waitCounter--;
         }
      }
   }

   /**
    * Clears this MessageQueue so that it contains no items.
    */
   public void clear()
   {
      synchronized(this.lock)
      {
         queue.clear();
   
         this.lock.notifyAll();
      }
   }
   
   /**
    * Gets all the objects in the queue as a list.
    * 
    * @since 2.1.2 (20060227)
    */
   public List getQueueAsList()
   {
      synchronized(this.lock)
      {
         return new ArrayList(this.queue);
      }
   }
}
