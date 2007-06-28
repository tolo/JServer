import java.util.ArrayList;

import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.queue.FileDBQueueStorage;
import com.teletalk.jserver.queue.OutQueueControllerSystem;
import com.teletalk.jserver.queue.QueueItem;
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
   private long totalTime = 0;
   
   private int dispatchCount = 0;
   
   private final MessageQueue redispatchQueue = new MessageQueue();
   
   
   /**
    */
	public SendController(SubSystem parent)
	{
		super(parent, "SendController");
      
      super.queueManager.setQueueCollaborationManager(new QueueMessagingManager(super.queueManager, "QueueMessagingManager"));
      
      super.queueManager.getOutQueue().setQueueStorage(new FileDBQueueStorage(queueManager.getOutQueue()));
      
      //super.queueManager.setRemoveCompletedOutQueueItems(false);
	}
   
   protected void doInitialize()
   {
      redispatchQueue.clear();
      
      super.doInitialize();
   }
   
   public void outItemDispatched(QueueItem item)
   {
      super.outItemDispatched(item);
      
      dispatchCount++;
   }
	
	public void unableToDispatchOutItem(QueueItem item)
   {
      super.unableToDispatchOutItem(item);
      
      redispatchQueue.putMsg(item);
   }

   public void unableToDispatchOutItemQueueFull(QueueItem item)
   {
      super.unableToDispatchOutItemQueueFull(item);
      
      redispatchQueue.putMsg(item);
   }

   public void outItemDoneCancelled(QueueItem item, Object responseData)
	{
		//System.out.println(item.getItemData().getDescription() + " done (cancelled)) - " + item.getSenderReceiverAddress());
	}
	
	public void outItemDoneFailure(QueueItem item, Object responseData)
	{
		//System.out.println(item.getItemData().getDescription() + " done (failure) - " + item.getSenderReceiverAddress());
	}
	
	public void outItemDoneSuccess(QueueItem item, Object responseData)
	{
		//System.out.println(item.getItemData().getDescription() + " done (success))  - " + item.getSenderReceiverAddress());
	}
	
	public void run()
	{
      long startTime = -1;
      
      try
      {
         logInfo("SendController: waiting for connections...");
         System.out.println("SendController: waiting for connections...");
         
         super.queueManager.waitForEnabled(10000);
         
         //int existingQueueSize = super.queueManager.getOutQueue().size();
         
         QueueMessagingManager queueMessagingManager = ((QueueMessagingManager)super.queueManager.getCollaborationManager());  
         queueMessagingManager.waitForLinkEstablished(60*1000);
         
         /*logInfo("SendController: existingQueueSize: " + existingQueueSize);
         System.out.println("SendController: existingQueueSize: " + existingQueueSize);*/
         
         ArrayList allQueued = super.queueManager.getOutQueue().getAllQueuedAsList();
         
         Object iterationObj;
         for (int i = 0; i < allQueued.size(); i++)
         {
            iterationObj = allQueued.get(i);
            if (iterationObj != null) this.redispatchQueue.putMsg(iterationObj);
         }         
         
         logInfo("SendController - redispatch queue size: " + this.redispatchQueue.size());
         System.out.println("SendController - redispatch queue size: " + this.redispatchQueue.size());
         
         logInfo("SendController: sending items...");
         System.out.println("SendController: sending items...");
         
         startTime = System.currentTimeMillis();
         
   		ItemData data = null;
         
         int n = 10000;
         int burstSize = 100;
         int availableBurst;
         int maxOutQueueSize = 500;
         int outQueueRefillThreshold = maxOutQueueSize - 100;
         //int pauseAtEvery = 100;
         
   		//MessagingQueueSystemEndPointIdentifier endPointIdentifier = new MessagingQueueSystemEndPointIdentifier("queueTestReceiver");
         MessagingQueueSystemEndPointIdentifier endPointIdentifier = new MessagingQueueSystemEndPointIdentifier("queueTestRelayer");
         
         
         RemoteQueueSystemDestination serverDestination = null;
         Destination[] destinations = queueMessagingManager.getDestinations();
         if( destinations.length > 0 ) serverDestination = (RemoteQueueSystemDestination)destinations[0];
                  
         
         //for(int i=existingQueueSize; (i<n) && super.canRun;)
         for(int i=(dispatchCount + this.redispatchQueue.size()); (i<n) && super.canRun;)
         {
            while( !queueMessagingManager.isLinkEstablished() )
            {
               logInfo("SendController: waiting for connections...");
               System.out.println("SendController: waiting for connections...");
               queueMessagingManager.waitForLinkEstablished(60*1000);
            }
            
            destinations = queueMessagingManager.getDestinations();
            if( destinations.length > 0 ) serverDestination = (RemoteQueueSystemDestination)destinations[0];
            
            if( serverDestination != null)
            {
               maxOutQueueSize = serverDestination.getRemoteInQueueMaxLength();
               outQueueRefillThreshold = maxOutQueueSize - 100;
            }
            
            if( super.queueManager.getOutQueue().size() >= maxOutQueueSize )
            {
               for(int q=0; (super.queueManager.getOutQueue().size() >= outQueueRefillThreshold); q++)
               {
                  if( (q % 10) ==0 )
                  {
                     logInfo("SendController: waiting for out queue space (out queue size: " + super.queueManager.getOutQueue().size() + ")...");
                     System.out.println("SendController: waiting for out queue space (out queue size: " + super.queueManager.getOutQueue().size() + ")...");
                      
                    maxOutQueueSize = serverDestination.getRemoteInQueueMaxLength();
                    outQueueRefillThreshold = maxOutQueueSize - 100;
                  }
                  Thread.sleep(250);
               }
            }
            
            if( this.redispatchQueue.containsData() )
            {
               logInfo("SendController: redispatching " + this.redispatchQueue.size() + " items...");
               System.out.println("SendController: redispatching " + this.redispatchQueue.size() + " items...");
               
               // Redispatch
               while( this.redispatchQueue.containsData() )
               {
                  Object item = this.redispatchQueue.getMsg(1000);
                  if( item != null ) queueManager.dispatchQueueItem((QueueItem)item);
               }
            }
            else
            {
               availableBurst = Math.min(burstSize, (n-i+1));
               if( serverDestination != null )
               {
                  if( serverDestination.getRemoteInQueueMaxLength() > serverDestination.getExpectedRemoteInQueueLength())
                  {
                     availableBurst = Math.min(availableBurst, serverDestination.getRemoteInQueueMaxLength() - 
                           serverDestination.getExpectedRemoteInQueueLength());
                  }
               }
               
               logInfo("SendController: dispatching " + availableBurst + " new items...");
               System.out.println("SendController: dispatching " + availableBurst + " new items...");
               
               for(int b=0; b<availableBurst; b++)
               {
                  data = new ItemData("Test " + i);
                  
                  //logInfo("Dispatching " + data);
                  //System.out.println("Dispatching " + data);
                  queueManager.dispatchQueueItem(data, endPointIdentifier);
                  
                  i++;
               }
               
               /*logInfo("Pausing - item index: " + i);
               System.out.println("Pausing - item index: " + i);
               Thread.sleep(1000);*/
            }
               				
            /*if( (i % pauseAtEvery) == 0 )
            {
               logInfo("Pausing - item index: " + i);
       			System.out.println("Pausing - item index: " + i);
               	
      			try
      			{
      				Thread.sleep(1000 + (long)(Math.random() * 500));
      			}
      			catch(InterruptedException e)
      			{
      				if(!canRun) break;
      			}
            }*/
         }
         
         
         if( super.canRun )
         {
            logInfo("Done dispatching items");
            System.out.println("Done dispatching items");
            
            
            int i=0;
            // Redispatch
            while( super.canRun && ((super.queueManager.getOutQueue().getAllQueuedAsList().size() > 0) || 
                  (super.queueManager.getOutQueue().getAllWithStatusAsList(QueueItem.DISPATCHING).size() > 0) ||
                  (super.queueManager.getOutQueue().getAllWithStatusAsList(QueueItem.DISPATCHED).size() > 0)) )
            {
               try
               {
                  while( this.redispatchQueue.containsData() )
                  {
                     Object item = this.redispatchQueue.getMsg(1000);
                     if( item != null ) queueManager.dispatchQueueItem((QueueItem)item);
                  }
                                                      
                  Thread.sleep(500);
                  
                  if( (i % 20) == 0 )
                  {
                     System.out.println("Queued items: " + super.queueManager.getOutQueue().getAllQueuedAsList().size());
                     System.out.println("Dispatching items: " + super.queueManager.getOutQueue().getAllWithStatusAsList(QueueItem.DISPATCHING).size());
                     System.out.println("Dispatched items: " + super.queueManager.getOutQueue().getAllWithStatusAsList(QueueItem.DISPATCHED).size());
                  }
               }
               catch(InterruptedException e){if(!canRun) break;}
               
               i++;
            }
           
            if( super.canRun )
            {
               SendServer.testComplete = true;
               
               long testTime = totalTime + (System.currentTimeMillis() - startTime);
      
               logInfo("### All items done (time: " + (long)(testTime/1000L) + "s) ###");
               System.out.println("### All items done (time: " + (long)(testTime/1000L) + "s) ###");
               
               super.queueManager.getOutQueue().clear();
            }
         }
         
            
         while(canRun)
         {
            try
            {
               Thread.sleep(1000000);
            }
            catch(InterruptedException e){if(!canRun) break;}
         }
      }
      catch (Throwable e) 
      {
         e.printStackTrace();
      }
      finally
      {
         if( startTime > 0 ) totalTime += (System.currentTimeMillis() - startTime); 
      }
	}
}
