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

import com.teletalk.jserver.util.MessageQueue;
import com.teletalk.jserver.util.PriorityMessageQueue;
import com.teletalk.jserver.util.PriorityMessageQueueItem;

/**
 * 
 */
public class PriorityMessageQueueTest extends MessageQueueTest
{
   private static final class TestPriorityMessageQueueItem implements PriorityMessageQueueItem
   {
      final long priority;
      
      public TestPriorityMessageQueueItem()
      {
         this.priority = System.identityHashCode(this);
      }
      
      public TestPriorityMessageQueueItem(long priority)
      {
         this.priority = priority;
      }
      
      public long getPriority()
      {
         return this.priority;
      }
   }
   
   protected MessageQueue createMessageQueue()
   {
      return new PriorityMessageQueue();
   }
   
   protected Object createMessageQueueItem()
   {
      return new TestPriorityMessageQueueItem();
   }
   
   
   public void testPriority() throws Exception
   { 
      MessageQueue messageQueue = createMessageQueue();
      
      messageQueue.putMsg(new TestPriorityMessageQueueItem(2));
      messageQueue.putMsg(new TestPriorityMessageQueueItem(3));
      messageQueue.putMsg(new TestPriorityMessageQueueItem(1));
      
      TestPriorityMessageQueueItem testPriorityMessageQueueItem = (TestPriorityMessageQueueItem)messageQueue.getMsg(); 
      assertEquals(1, testPriorityMessageQueueItem.priority);
      testPriorityMessageQueueItem = (TestPriorityMessageQueueItem)messageQueue.getMsg(); 
      assertEquals(2, testPriorityMessageQueueItem.priority);
      testPriorityMessageQueueItem = (TestPriorityMessageQueueItem)messageQueue.getMsg(); 
      assertEquals(3, testPriorityMessageQueueItem.priority);
   }
}
