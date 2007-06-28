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

import org.apache.log4j.Level;

import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.queue.FileDBQueueStorage;
import com.teletalk.jserver.queue.InQueueControllerSystem;
import com.teletalk.jserver.queue.QueueItem;
import com.teletalk.jserver.queue.messaging.QueueMessagingManager;

/**
 * @author Tobias Löfstrand
 */
public class ReceiverController extends InQueueControllerSystem
{
   public static final String QUEUE_ITEM_IN_QUEUE_PATH = "./receiver/in";
   
   public static final int RECEIVER_CONTROLLER_PORT = 11233;

   
   private int i = 0;
   

   /**
    */
   public ReceiverController(SubSystem parent)
   {
      super(parent, "ReceiverController");
      
      super.queueManager.setRecentCompletionResponseCacheSize(QueueManagerTest.N_ITEMS/2);

      QueueMessagingManager queueMessagingManager = new QueueMessagingManager(super.queueManager, "QueueMessagingManager", "queueTestReceiver");
      queueMessagingManager.setCheckInteval(1000);
      queueMessagingManager.addServerAddress("localhost", RECEIVER_CONTROLLER_PORT);
      super.queueManager.setQueueCollaborationManager(queueMessagingManager);
                  
      FileDBQueueStorage fileDBQueueStorage = new FileDBQueueStorage(queueManager.getInQueue());
      fileDBQueueStorage.setDbFilePath(QUEUE_ITEM_IN_QUEUE_PATH);
      
      super.queueManager.getInQueue().setQueueStorage(fileDBQueueStorage);
      
      //super.queueManager.setLogLevel(Level.DEBUG);
   }
   
   /**
    */
   protected void doShutDown()
   {
      super.doShutDown();
      
      try{
         super.queueManager.waitForDown(10000);
      }catch(InterruptedException ie){}
   }
   
   /**
    */
   public int getProcessedItemsCount()
   {
      return this.i;
   }
   
   /**
    */
   public boolean checkCompletionResponseCache()
   {
      return (super.queueManager.getRecentCompletionResponses().size() <= super.queueManager.getRecentCompletionResponseCacheSize() );
   }
   
   /**
    */
   public int getActualCompletionResponseCacheSize()
   {
      return super.queueManager.getRecentCompletionResponses().size();
   }

   /**
    */
   public void run()
   {
      try
      {
         super.queueManager.waitForEnabled(10000);
                  
         QueueItem item = null;

         for(; super.canRun;)
         {
            item = this.queueManager.getInQueue().checkOutFirst();
            
            if( Math.random() >= 0.8) this.queueManager.inItemDoneFailure(item);
            else this.queueManager.inItemDoneSuccess(item);
            
            i++;
            
            if( (i % 100) == 0 )
            {
               super.logInfo(i + " items processed.");
               Thread.sleep(250);
            }
         }
      }
      catch (Throwable e)
      {
         if( super.canRun )
         {
            logError("Error in RelayController!", e);
            QueueManagerTest.incrementFailCount();
         }
      }
   }
}
