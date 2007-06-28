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

import java.util.List;
import java.util.ListIterator;

/**
 * A priority based implementation of {@link MessageQueue}.
 *  
 * @author Tobias Löfstrand
 * 
 * @since 2.1.5 (20070413)
 */
public class PriorityMessageQueue extends MessageQueue
{
   /**
    * Constructs a new PriorityMessageQueue.
    */
   public PriorityMessageQueue()
   {
      this(null, null);
   }
      
   /**
    * Constructs a new PriorityMessageQueue.
    */
   public PriorityMessageQueue(final List queueList, final Object lockObject)
   {
      super(queueList, lockObject);
   }
   
   /**
    * Puts a {@link PriorityMessageQueueItem priority message} in the MessageQueue. The message will be added after all items in 
    * the queue with lower or equal priority to the message, but before any items with higher priority.   
    * 
    *  @see PriorityMessageQueueItem
    */
   public void putPriorityMsg(final PriorityMessageQueueItem priorityMessage)
   {
      synchronized(super.getLock())
      {
         final ListIterator it = super.queue.listIterator();
         boolean added = false;
         while(it.hasNext())
         {
            final PriorityMessageQueueItem queuedPriorityMessage = (PriorityMessageQueueItem) it.next();
            if (priorityMessage.getPriority() < queuedPriorityMessage.getPriority())
            {
               it.previous();
               it.add(priorityMessage);
               added = true;
               break;
            }
         }
         if( !added ) super.queue.add(priorityMessage);
         
         this.notifyIfNeeded();
      }
   }
   
   /**
    * Puts a {@link PriorityMessageQueueItem priority message} in the MessageQueue.
    * 
    * @see #putPriorityMsg(PriorityMessageQueueItem)
    */
   public void putMsg(final Object msg)
   {
      this.putPriorityMsg((PriorityMessageQueueItem)msg);
   }
   
   /**
    * Puts a {@link PriorityMessageQueueItem priority message} in the MessageQueue.
    * 
    * @see #putPriorityMsg(PriorityMessageQueueItem)
    */
   public void putMsg(final Object msg, int position)
   {
      this.putPriorityMsg((PriorityMessageQueueItem)msg);
   }

   /**
    * Puts a {@link PriorityMessageQueueItem priority message} in the MessageQueue.
    * 
    * @see #putPriorityMsg(PriorityMessageQueueItem)
    */
   public void putUrgentMsg(final Object msg)
   {
      this.putPriorityMsg((PriorityMessageQueueItem)msg);
   }
}
