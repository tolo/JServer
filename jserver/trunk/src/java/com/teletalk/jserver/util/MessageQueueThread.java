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

import com.teletalk.jserver.JServerUtilities;

/**
 * Threaded message queue implementation that handles queueing and execution of messages. Message execution is 
 * performed in the method {@link #handleMessage(Object)}. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0 
 */
public class MessageQueueThread extends Thread
{
   /** The message queue. */
   protected final MessageQueue queue;
      
   /** Flag indicating if this thread may run. */
   protected volatile boolean canRun = true;
   
   /**
    * Creates a new running MessageQueueThread.
    */
   public MessageQueueThread()
   {
      this("MessageQueueThread", true);
   }
   
   /**
    * Creates a new MessageQueueThread.
    * 
    * @param startThread flag indicating if the thread should be started.
    */
   public MessageQueueThread(boolean startThread)
   {
      this("MessageQueueThread", startThread);
   }
   
   /**
    * Creates a new running MessageQueueThread.
    * 
    * @param name the name of this MessageQueueThread.
    */
   public MessageQueueThread(String name)
   {
      this(name, true);
   }
    
   /**
    * Creates a new MessageQueueThread.
    * 
    * @param name the name of this MessageQueueThread.
    * @param startThread flag indicating if the thread should be started.
    */
   public MessageQueueThread(String name, boolean startThread)
   {
      super(name);
      
      this.queue = new MessageQueue();
      
      super.setDaemon(true);
      if( startThread )super.start();
   }
   
   /**
    * Gets the MessageQueue used by this MessageQueueThread.
    * 
    * @since 2.1
    */
   public MessageQueue getMessageQueue()
   {
      return queue;
   }
   
   /**
    * Checkes if this MessageQueueThread is alive (not destroyed).
    * 
    * @return true if this MessageQueueThread is alive, otherwise false.
    */
   public boolean check()
   {
      return (canRun && this.isAlive());
   }
         
   /**
    * Places a message in the queue.
    */
   public boolean queueMessage(final Object message)
   {
      if( this.canRun )
      {
         this.queue.putMsg(message);
         return true;
      }
      else return false;
   }
   
   /**
    * Handles a message places in the queue. This method will be invoked by the thread method of this MessageQueueThread.<br>
    * <br>
    * This default implementation handles execution of messages of the type java.lang.Runnable.
    */
   protected void handleMessage(final Object message)
   {
      ((Runnable)message).run();
   }
   
   /**
    * Starts this MessageQueueThread.
    */
   public void start()
   {
      canRun = true;
      super.start();
   }
   
   /**
    * Stops and destroys this MessageQueueThread.
    */
   public void destroy()
   {
      this.doDestroy();
      this.interrupt();
   }
   
   /**
    * Internal destroy method.
    */
   protected void doDestroy()
   {
      this.canRun = false;
      this.queue.clear();
   }
    
   /**
    * The thread method.
    */
   public void run()
   {
      try
      {
         while(canRun)
         {
            Object message = null;
            try
            {
               message = queue.getMsg();
               this.handleMessage(message);
            }
            catch(InterruptedException ie)
            {
               continue;
            }
            catch(Throwable e)
            {
               JServerUtilities.logError("Error while handling message ('" + message + "')!", e);
            }
         }
      }
      finally
      {
         this.doDestroy();
      }
   }
}
