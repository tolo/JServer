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

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.teletalk.jserver.util.MessageQueue;

/**
 * 
 * @author Tobias Löfstrand
 */
public class MessageQueueTest extends TestCase
{
   public static final int N_ITEMS = 1000;
   
   private static final Log logger = LogFactory.getLog(MessageQueueTest.class);
   
   private static int failCount = 0;
   
   
   private static void resetFailCount()
   {
      synchronized (MessageQueueTest.class)
      {
         failCount = 0;
      }
   }
   
   private static void incrementFailCount()
   {
      synchronized (MessageQueueTest.class)
      {
         failCount++;
      }
   }
   
   private static int getFailCount()
   {
      synchronized (MessageQueueTest.class)
      {
         return failCount;
      }
   }
   
   
   protected MessageQueue createMessageQueue()
   {
      return new MessageQueue();
   }
   
   protected Object createMessageQueueItem()
   {
      return new Object();
   }
   
   
   /* ### TEST METHODS ### */

   
   /**
    */
   public void testMessageQueue()
   {
      int n = 10;
      
      ProducerThread producerThreads[] = new ProducerThread[n];
      ConsumerThread consumerThreads[] = new ConsumerThread[n];
      
      MessageQueue messageQueue = createMessageQueue();
      
      int counter = 0;
      resetFailCount();
         
      for(int i=0; i<n; i++)
      {
         producerThreads[i] = new ProducerThread(this, messageQueue);
         producerThreads[i].setDaemon(true);
         consumerThreads[i] = new ConsumerThread(i, messageQueue);
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
         super.fail("Error in testMessageQueue - failCount: " + getFailCount() + ".");
      }
      if( messageQueue.size() > 0 )
      {
         super.fail("There are still items in the queue (" + messageQueue.size() + ")!");
      }
   }
   
   
   /* ### INTERNALS ### */
   
   
   /**
    */
   private static class ProducerThread extends Thread
   {
      private MessageQueueTest messageQueueTest;
      
      private MessageQueue messageQueue;
      
      public ProducerThread(MessageQueueTest messageQueueTest, MessageQueue messageQueue)
      {
         this.messageQueueTest = messageQueueTest;
         this.messageQueue = messageQueue;
      }
      
      public void run()
      {
         try
         {
            for(int i=0; i<N_ITEMS; i++)
            {
               if( (i%2) == 0 ) messageQueue.putMsg(this.messageQueueTest.createMessageQueueItem());
               else messageQueue.putUrgentMsg(this.messageQueueTest.createMessageQueueItem());
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
      
      private MessageQueue messageQueue;
      
      public ConsumerThread(int threadId, MessageQueue messageQueue)
      {
         this.threadId = threadId;
         this.messageQueue = messageQueue;
      }
      
      public void run()
      {
         int i = 0;
         
         for(; (i<N_ITEMS) && (!Thread.currentThread().isInterrupted()); i++)
         {
            try
            {
               if( (i%4) == 0 )
               {
                  if( messageQueue.getMsg() == null )
                  {
                     if( MessageQueueTest.logger.isDebugEnabled() ) MessageQueueTest.logger.debug("ConsumerThread" + threadId + ": messageQueue.getMsg() returned null!");
                     incrementFailCount();
                  }
               }
               else if( (i%4) == 1 )
               {
                  if( messageQueue.getMsg(1000) == null )
                  {
                     if( MessageQueueTest.logger.isDebugEnabled() ) MessageQueueTest.logger.debug("ConsumerThread" + threadId + ": messageQueue.getMsg(1000) returned null!");
                     incrementFailCount();
                  }
               }
               else if( (i%4) == 2 )
               {
                  synchronized(messageQueue.getLock())
                  {
                     messageQueue.waitForData(1000);
                     if( messageQueue.remove(0) == null )
                     {
                        if( MessageQueueTest.logger.isDebugEnabled() ) MessageQueueTest.logger.debug("ConsumerThread" + threadId + ": messageQueue.remove(0) returned null!");
                        incrementFailCount();
                     }
                  }
               }
               else
               {
                  synchronized(messageQueue.getLock())
                  {
                     messageQueue.waitForData(1000);
                     messageQueue.peekMsg();
                     if( messageQueue.getMsgIfAny() == null )
                     {
                        if( MessageQueueTest.logger.isDebugEnabled() ) MessageQueueTest.logger.debug("ConsumerThread" + threadId + ": messageQueue.getMsgIfAny() returned null!");
                        incrementFailCount();
                     }
                  }
               }
            }
            catch(Exception e)
            {
               MessageQueueTest.logger.error("ConsumerThread" + threadId + ": error (" + i + ")!", e);
               incrementFailCount();
            }
         }
         
         if( i < N_ITEMS )
         {
            MessageQueueTest.logger.error("ConsumerThread" + threadId + " interrupted before completion!");
            incrementFailCount();
         }
      }
   }
}
