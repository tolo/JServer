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

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.teletalk.jserver.queue.NullQueueStorage;
import com.teletalk.jserver.queue.Queue;
import com.teletalk.jserver.queue.QueueItem;
import com.teletalk.jserver.queue.QueueItemData;

/**
 * 
 * @author Tobias Löfstrand
 */
public class QueueTest extends TestCase
{
   public static final int N_ITEMS = 1000;
   
   private static final Log logger = LogFactory.getLog(QueueTest.class);
   
   private static int failCount = 0;
   
   
   private static void resetFailCount()
   {
      synchronized (QueueTest.class)
      {
         failCount = 0;
      }
   }
   
   private static void incrementFailCount()
   {
      synchronized (QueueTest.class)
      {
         failCount++;
      }
   }
   
   private static int getFailCount()
   {
      synchronized (QueueTest.class)
      {
         return failCount;
      }
   }
   
   
   /**
    */
   public void testQueue()
   {
      logger.info("BEGIN testQueue.");
      
      Queue queue = new Queue("queue", new NullQueueStorage());
      queue.engage();
      
      int n = 10;
      
      ProducerThread producerThreads[] = new ProducerThread[n];
      ConsumerThread consumerThreads[] = new ConsumerThread[n];
            
      int counter = 0;
      resetFailCount();
         
      for(int i=0; i<n; i++)
      {
         producerThreads[i] = new ProducerThread(i, queue);
         producerThreads[i].setDaemon(true);
         consumerThreads[i] = new ConsumerThread(i, queue);
         consumerThreads[i].setDaemon(true);
         counter++;
      }
      
      for(int i=0; i<n; i++)
      {
         producerThreads[i].start();
         consumerThreads[i].start();
      }
      
      long beginWait = System.currentTimeMillis();
      long waitTime;
      for(int i=0; i<n; i++)
      {
         waitTime = 10000 - (System.currentTimeMillis() - beginWait);
         if( waitTime > 0 )
         {
            try{
            producerThreads[i].join(waitTime);
            }catch (Exception e){}
         }
         waitTime = 10000 - (System.currentTimeMillis() - beginWait);
         if( waitTime > 0 )
         {
            try{
            consumerThreads[i].join(waitTime);
            }catch (Exception e){}
         }
         producerThreads[i].interrupt();
         consumerThreads[i].interrupt();
      }
         
      if( getFailCount() > 0 )
      {
         super.fail("Error in testQueue - failCount: " + getFailCount() + ".");
      }
      if( queue.size() > 0 )
      {
         super.fail("There are still items in the queue (" + queue.size() + ")!");
      }
      
      logger.info("END testQueue.");
   }
   
   
   /* ### INTERNALS ### */
   
   
   private static class StringQueueItemData implements QueueItemData
   {
      private static final long serialVersionUID = 1L;
      
      private String data;
      
      public StringQueueItemData(String data)
      {
         this.data = data;
      }

      public String getDescription()
      {
         return data;
      }
      
   }
   
   
   /**
    */
   private static class ProducerThread extends Thread
   {
      private int threadId;
      
      private Queue queue;
      
      public ProducerThread(int threadId, Queue queue)
      {
         this.threadId = threadId;
         this.queue = queue;
      }
      
      public void run()
      {
         try
         {
            QueueItem item;
            String id;
            for(int i=0; i<N_ITEMS; i++)
            {
               id = threadId + "(" + i + ")";
               item = new QueueItem(new StringQueueItemData("data" + id), id);
               
               if( (i%3) == 0 ) queue.add(item);
               else if( (i%3) == 1 ) queue.add(new QueueItem[]{item}); 
               else
               {
                  queue.add(item, false);
                  queue.addToQueuedList(item);
               }
            }
         }
         catch(Exception e)
         {
            incrementFailCount();
         }
      }
   }
   
   /**
    */
   private static class ConsumerThread extends Thread
   {
      private int threadId;
      
      private Queue queue;
      
      public ConsumerThread(int threadId, Queue queue)
      {
         this.threadId = threadId;
         this.queue = queue;
      }
      
      public void run()
      {
         int i = 0;
         ArrayList checkedOutItems = new ArrayList();
         QueueItem item = null;
         
         
         // Check out items
         for(; (i<N_ITEMS) && (!Thread.currentThread().isInterrupted()); i++)
         {
            try
            {
               if( (i%4) == 0 ) // Check out and set status to DONE_SUCCESS
               {
                  item = queue.checkOutFirst();
                  if( item == null )
                  {
                     if( QueueTest.logger.isDebugEnabled() ) QueueTest.logger.debug("ConsumerThread" + threadId + ": checkOutFirst() returned null!");
                     incrementFailCount();
                  }
                  else
                  {
                     item.setStatus(QueueItem.DONE_SUCCESS);
                     
                     checkedOutItems.add(item);
                  }
               }
               else if( (i%4) == 1 ) // Wait for queued items, get first from list of queue and remove
               {
                  synchronized(queue.getLock())
                  {
                     queue.waitForQueuedItems(1000);
                     ArrayList queued = queue.getAllQueuedAsList();
                     if( queued.size() == 0 )
                     {
                        if( QueueTest.logger.isDebugEnabled() ) QueueTest.logger.debug("ConsumerThread" + threadId + ": getAllQueuedAsList() returned 0 items!");
                        incrementFailCount();
                     }
                     else
                     {
                        item = (QueueItem)queued.get(0);
                        
                        if( queue.checkOut(item) == false )
                        {
                           if( QueueTest.logger.isDebugEnabled() ) QueueTest.logger.debug("ConsumerThread" + threadId + ": checkOut(QueueItem) returned false!");
                           incrementFailCount();
                        }
                        
                        queue.remove(item);
                     }
                  }
               }
               else if( (i%4) == 2 ) // Get (wait for) first queued and remove 
               {
                  synchronized(queue.getLock())
                  {
                     item = queue.getFirst();
                     
                     if( queue.getFirst() == null )
                     {
                        if( QueueTest.logger.isDebugEnabled() ) QueueTest.logger.debug("ConsumerThread" + threadId + ": removeFirst() returned null!");
                        incrementFailCount();
                     }
                     
                     queue.remove(item);
                  }
               }
               else // Wait for queued items, get first if any and remove 
               {
                  synchronized(queue.getLock())
                  {
                     queue.waitForQueuedItems(1000);
                     item = queue.getFirstIfAny();
                     
                     if( item == null )
                     {
                        if( QueueTest.logger.isDebugEnabled() ) QueueTest.logger.debug("ConsumerThread" + threadId + ": getFirstIfAny() returned null!");
                        incrementFailCount();
                     }
                     
                     queue.remove(item);
                  }
               }
            }
            catch(Exception e)
            {
               QueueTest.logger.error("ConsumerThread" + threadId + ": error (" + i + ")!", e);
               incrementFailCount();
            }
         }
         
         
         // Remove all items completed by this thread
         ArrayList completedItems = queue.getAllWithStatusAsList(QueueItem.DONE_SUCCESS);
         
         for(int q=0; q<checkedOutItems.size(); q++)
         {
            item = (QueueItem)checkedOutItems.get(q);
            if( !completedItems.contains(item) )
            {
               if( QueueTest.logger.isDebugEnabled() ) QueueTest.logger.debug("ConsumerThread" + threadId + ": getAllWithStatusAsList() didn't return the expected item!");
               incrementFailCount();
            }
            
            queue.remove(item);
         }
         
         
         if( i < N_ITEMS )
         {
            QueueTest.logger.error("ConsumerThread" + threadId + " interrupted before completion!");
            incrementFailCount();
         }
      }
   }
}
