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
import java.util.List;

import org.apache.log4j.Level;

import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.queue.FileDBQueueStorage;
import com.teletalk.jserver.queue.OutQueueControllerSystem;
import com.teletalk.jserver.queue.QueueItem;
import com.teletalk.jserver.queue.QueueItemData;
import com.teletalk.jserver.queue.messaging.MessagingQueueSystemEndPointIdentifier;
import com.teletalk.jserver.queue.messaging.QueueMessagingManager;
import com.teletalk.jserver.queue.messaging.RemoteQueueSystemDestination;
import com.teletalk.jserver.tcp.messaging.Destination;
import com.teletalk.jserver.util.MessageQueue;

/**
 * 
 * @author Tobias Löfstrand
 */
public class SendController extends OutQueueControllerSystem
{
   public static final String QUEUE_ITEM_OUT_QUEUE_PATH = "./sender/out";
   
   
   private int dispatchCount = 0;
   
   private int completeCount = 0;
   
   
   private final MessageQueue dispatchQueue = new MessageQueue();
   
   private final MessageQueue redispatchQueue = new MessageQueue();
         
   
   /**
    */
	public SendController(SubSystem parent)
	{
		super(parent, "SendController");
            
      QueueMessagingManager queueMessagingManager = new QueueMessagingManager(super.queueManager, "QueueMessagingManager");
      queueMessagingManager.setCheckInteval(1000);
      super.queueManager.setQueueCollaborationManager(queueMessagingManager);
      queueMessagingManager.addDestination("localhost", RelayController.RELAY_CONTROLLER_PORT);
            
      FileDBQueueStorage fileDBQueueStorage = new FileDBQueueStorage(queueManager.getOutQueue());
      fileDBQueueStorage.setDbFilePath(QUEUE_ITEM_OUT_QUEUE_PATH);
      super.queueManager.getOutQueue().setQueueStorage(fileDBQueueStorage);
      
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
   
   public void queueJob(Object job)
   {
      this.dispatchQueue.putMsg(job);
   }
   
   public boolean waitForCompletionEvent(final int numberOfItems, final long waitTime)
   {
      long beginWait = System.currentTimeMillis();
      long timeLeft = waitTime - (System.currentTimeMillis() - beginWait);
      
      synchronized(this)
      {
         while( (numberOfItems > this.completeCount) && (timeLeft > 0) )
         {
            timeLeft = waitTime - (System.currentTimeMillis() - beginWait);
            if( timeLeft > 0 )
            {
               try{
               this.wait(timeLeft);
               }catch(InterruptedException ie){}
            }
            else break;
         }
         
         logInfo("Complete count: " + this.completeCount);
         
         return this.completeCount >= numberOfItems;
      }
   }
   
   public synchronized int getCompleteCount()
   {
      return completeCount;
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
      this.notify();
	}
	
	public synchronized void outItemDoneFailure(QueueItem item, Object responseData)
	{
      completeCount++;
      this.notify();
	}
	
	public synchronized void outItemDoneSuccess(QueueItem item, Object responseData)
	{
      completeCount++;
      this.notify();
	}
	
	public void run()
	{
      MessagingQueueSystemEndPointIdentifier endPointIdentifier = new MessagingQueueSystemEndPointIdentifier("queueTestRelayer");
      
      try
      {
         super.queueManager.waitForEnabled(10000);
         
         QueueMessagingManager queueMessagingManager = ((QueueMessagingManager)super.queueManager.getCollaborationManager());  
         queueMessagingManager.waitForLinkEstablished(60*1000);
         
         int maxOutQueueSize = RelayController.MAX_IN_QUEUE_SIZE;
         int refillDelta = maxOutQueueSize / 2;
         int burstSize = maxOutQueueSize;
         int availableBurst;
         int outQueueRefillThreshold = maxOutQueueSize - refillDelta;
         
         TestQueueItemData data = null;
         RemoteQueueSystemDestination serverDestination = null;
         
         logInfo("Starting message dispatch");
         
         for(int i=0; canRun; i++)
         {
            while( !queueMessagingManager.isLinkEstablished() )
            {
               queueMessagingManager.waitForLinkEstablished(60*1000);
            }
            
            Destination[] destinations = queueMessagingManager.getDestinations();
            if( destinations.length > 0 ) serverDestination = (RemoteQueueSystemDestination)destinations[0];
            
            if( serverDestination != null )
            {
               // If out queue is bigger than maxOutQueueSize...
               if( (super.queueManager.getOutQueue().size() >= maxOutQueueSize) && (serverDestination.getRemoteInQueueMaxLength() != -1) )
               {
                  // Wait until space is available in the remote in queue
                  for(int q=0; (serverDestination.getExpectedRemoteInQueueLength() >= outQueueRefillThreshold); q++)
                  {
                     if( (q % 10) ==0 )
                     {
                        logInfo("Waiting for remote in queue space (remote in queue size: " + serverDestination.getExpectedRemoteInQueueLength() + ")...");
                     }
                     Thread.sleep(250);
                  }
               }
               
               if( this.redispatchQueue.containsData() )
               {
                  List redispatchItems;
                  synchronized(this.redispatchQueue.getLock())
                  {
                     redispatchItems = this.redispatchQueue.getQueueAsList();
                     this.redispatchQueue.clear();
                  }
                                    
                  logInfo("Redispatching " + redispatchItems.size() + " items...");
                                    
                  // Redispatch
                  for(int r=0; r<redispatchItems.size(); r++)
                  {
                     Object item = redispatchItems.get(r);
                     if( item != null ) queueManager.dispatchQueueItem((QueueItem)item);
                  }
                  
                  Thread.sleep(250);
               }
               else if( this.dispatchQueue.size() > 0)
               {
                  availableBurst = Math.min(burstSize, this.dispatchQueue.size());
                  
                  /*if( serverDestination.getRemoteInQueueMaxLength() > serverDestination.getExpectedRemoteInQueueLength())
                  {
                     availableBurst = Math.min(availableBurst, serverDestination.getRemoteInQueueMaxLength() - 
                           serverDestination.getExpectedRemoteInQueueLength());
                  }*/
                  
                  availableBurst = Math.min(availableBurst, maxOutQueueSize - serverDestination.getExpectedRemoteInQueueLength());
                  
                  if( availableBurst > 0 )
                  {
                     logInfo("Dispatching " + availableBurst + " new items...");
                     
                     boolean dispatchMultipleItems = Math.random() < 0.5;
                     ArrayList multiItemDispatchList = dispatchMultipleItems ? new ArrayList() : null;
                     
                     for(int b=0; b<availableBurst; b++)
                     {
                        data = (TestQueueItemData)this.dispatchQueue.getMsg();
                        
                        if( dispatchMultipleItems ) multiItemDispatchList.add(data);
                        else queueManager.dispatchQueueItem(data, endPointIdentifier);
                        
                        if( this.redispatchQueue.size() > 0 ) break;
                     }
                     
                     if( dispatchMultipleItems ) queueManager.dispatchQueueItems((QueueItemData[])multiItemDispatchList.toArray(new QueueItemData[multiItemDispatchList.size()]), endPointIdentifier);
                  }
               }
               else
               {
                  logInfo("Waiting for data.");
                  this.dispatchQueue.waitForData(1000);
                  
                  if( super.queueManager.getOutQueue().size() > 0 )
                  {
                     logInfo("Queued items: " + super.queueManager.getOutQueue().getAllQueuedAsList().size());
                     logInfo("Dispatching items: " + super.queueManager.getOutQueue().getAllWithStatusAsList(QueueItem.DISPATCHING).size());
                     logInfo("Dispatched items: " + super.queueManager.getOutQueue().getAllWithStatusAsList(QueueItem.DISPATCHED).size());
                  }
               }
            }
         }
      }
      catch (Throwable e) 
      {
         if( super.canRun )
         {
            logError("Error in SendController!", e);
            QueueManagerTest.incrementFailCount();
         }
      }
	}
}
