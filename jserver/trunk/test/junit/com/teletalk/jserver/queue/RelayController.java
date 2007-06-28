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

import org.apache.log4j.Level;

import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.queue.FileDBQueueStorage;
import com.teletalk.jserver.queue.InOutQueueControllerSystem;
import com.teletalk.jserver.queue.QueueItem;
import com.teletalk.jserver.queue.QueueManager;
import com.teletalk.jserver.queue.messaging.MessagingQueueSystemEndPointIdentifier;
import com.teletalk.jserver.queue.messaging.QueueMessagingManager;
import com.teletalk.jserver.util.MessageQueue;

/**
 * 
 * @author Tobias Löfstrand
 */
public class RelayController extends InOutQueueControllerSystem
{
   public static final String QUEUE_ITEM_IN_QUEUE_PATH = "./relayer/in";
   
   public static final String QUEUE_ITEM_OUT_QUEUE_PATH = "./relayer/out";
   
   public static final int RELAY_CONTROLLER_PORT = 11232;
   
   public static final int MAX_IN_QUEUE_SIZE = 100;
   
   
   private int dispatchCount = 0;
   
   private int completeCount = 0;
   
   private final MessageQueue redispatchQueue = new MessageQueue();
   
   
   QueueMessagingManager queueMessagingManager;
      
   
	public RelayController(SubSystem parent)
	{
		super(parent, "RelayController", true);
      
      super.queueManager.setInQueueMaxSize(MAX_IN_QUEUE_SIZE);
      
      queueMessagingManager = new QueueMessagingManager(super.queueManager, "QueueMessagingManager", "queueTestRelayer");
      queueMessagingManager.setCheckInteval(1000);
      queueMessagingManager.addServerAddress("localhost", RELAY_CONTROLLER_PORT);
      queueMessagingManager.addDestination("localhost", ReceiverController.RECEIVER_CONTROLLER_PORT);
      super.queueManager.setQueueCollaborationManager(queueMessagingManager);
            
      FileDBQueueStorage fileDBQueueStorage = new FileDBQueueStorage(queueManager.getInQueue());
      fileDBQueueStorage.setDbFilePath(QUEUE_ITEM_IN_QUEUE_PATH);
      super.queueManager.getInQueue().setQueueStorage(fileDBQueueStorage);
      
      fileDBQueueStorage = new FileDBQueueStorage(queueManager.getOutQueue());
      fileDBQueueStorage.setDbFilePath(QUEUE_ITEM_OUT_QUEUE_PATH);
      super.queueManager.getOutQueue().setQueueStorage(fileDBQueueStorage);
      
      //super.queueManager.setLogLevel(Level.DEBUG);
	}
   
   /**
    */
   protected void doShutDown()
   {
      // Do nothing here
   }
   
   public QueueManager getQueueManager()
   {
      return super.queueManager;
   }
   
   public void outItemDispatched(QueueItem item)
   {
      synchronized(this)
      {
         dispatchCount++;
      }
   }
   
   public void unableToDispatchOutItem(QueueItem item)
   {
      redispatchQueue.putMsg(item);
   }

   public void unableToDispatchOutItemQueueFull(QueueItem item)
   {
      redispatchQueue.putMsg(item);
   }
   
   public synchronized void outItemDoneCancelled(QueueItem item, Object responseData)
   {
      completeCount++;
   }
   
   public synchronized void outItemDoneFailure(QueueItem item, Object responseData)
   {
      completeCount++;
   }
   
   public synchronized void outItemDoneSuccess(QueueItem item, Object responseData)
   {
      completeCount++;
   }
		
   /**
    */
	public void run()
	{
		QueueItem item = null;
      MessagingQueueSystemEndPointIdentifier endPointIdentifier = new MessagingQueueSystemEndPointIdentifier("queueTestReceiver");
      					
		try
		{
         super.queueManager.waitForEnabled(30000);
         
         ArrayList allQueued = super.queueManager.getOutQueue().getAllQueuedAsList();
         
         Object iterationObj;
         for (int i = 0; i < allQueued.size(); i++)
         {
            iterationObj = allQueued.get(i);
            if (iterationObj != null) this.redispatchQueue.putMsg(iterationObj);
         }
         
         logInfo("Redispatch queue size: " + this.redispatchQueue.size());
      
         for(int i=0; canRun;)
         {
            if( this.redispatchQueue.containsData() )
            {
               logInfo("Redispatching " + this.redispatchQueue.size() + " items...");
               
               // Redispatch
               while( this.redispatchQueue.containsData() )
               {
                  item = (QueueItem)this.redispatchQueue.getMsg(1000);
                  if( item != null ) queueManager.dispatchQueueItem(item);
               }
            }
            
            try
            {
               item = queueManager.getInQueue().checkOutFirst();
            }
            catch(InterruptedException ie)
            {
               if(!canRun) break;   
            }
               
            queueManager.dispatchQueueItem(item.getItemData(), item, endPointIdentifier);
            i++;
            
            if( ((i+1) % 100) == 0 )
            {
               super.logInfo((i+1) + " items relayed.");
            }
         }
		}
		catch(Throwable e)
		{
         if( super.canRun )
         {
            logError("Error in RelayController!", e);
            QueueManagerTest.incrementFailCount();
         }
		}
	}
}
