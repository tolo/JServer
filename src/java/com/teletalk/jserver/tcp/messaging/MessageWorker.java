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
package com.teletalk.jserver.tcp.messaging;

import com.teletalk.jserver.pool.PoolWorker;

/**
 * Worker class used for handling incomming messages through a thread pool. 
 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.1
 */
public class MessageWorker extends PoolWorker
{
   private final MessagingManager messagingManager;
   
   private Message message;
   
   /**
    * Creates a new MessageWorker
    * 
    * @param messagingManager the associated MessagingManager.
    */
   public MessageWorker(MessagingManager messagingManager)
   {
      this.messagingManager = messagingManager;
   }
   
   /**
    * Performs clean up of the MessageWorker. 
    */
   protected void cleanUp()
   {
      super.cleanUp();
      
      this.message = null;
   }

   /**
    * Destroys the MessageWorker.
    */
   protected void destroy()
   {
      super.destroy();
      
      this.message = null;
   }

   /**
    * Sets the message object to be associated with this MessageWorker. 
    * 
    * @param message the message object to be associated with this MessageWorker.
    */
   protected void setData(Object message)
   {
      this.message = (Message)message;
   }

   /**
    * Perfoms the logic of this MessageWorker.  
    */
   protected void work()
   {
      try
      {
         this.messagingManager.messageReceivedImpl(this.message);
      }
      catch(Throwable t)
      {
         this.messagingManager.getMessageProcessor().handleMessageReceiverError(message, t);  
         if( t instanceof Error ) throw (Error)t;
      }
   }
}
