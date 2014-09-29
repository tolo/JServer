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
   private int i=0;

   protected void doShutDown()
   {
      super.doShutDown();
      /*
       * System.out.println("ReceiverController - shutting down"); try{ Thread.sleep(3*1000); } catch (Exception e) { }
       * System.out.println("ReceiverController - down");
       */
   }

   /**
    */
   public ReceiverController(SubSystem parent)
   {
      super(parent, "ReceiverController");

      super.queueManager.setQueueCollaborationManager(new QueueMessagingManager(super.queueManager, "QueueMessagingManager", "queueTestReceiver"));
      
      super.queueManager.getInQueue().setQueueStorage(new FileDBQueueStorage(queueManager.getInQueue()));
   }

   /**
    */
   public void run()
   {
      try
      {
         System.out.println("ReceiverController: waiting for incoming items...");

         super.queueManager.waitForEnabled(10000);

         QueueItem item = null;
         int i = 0;
         
         Thread.sleep(5000);

         System.out.println("ReceiverController: processing incoming items...");
         
         long startTime = -1;
         
         for(; canRun; i++)
         {
            try
            {
               item = queueManager.getInQueue().checkOutFirst();
               if( startTime < 0 ) startTime = System.currentTimeMillis(); // Set startTime
               //System.out.println("Processing item (" + i + ") - " + item.getItemData().getDescription());

               //Thread.sleep(1000);
            }
            catch (InterruptedException ie)
            {
               if (!canRun) break;
            }

            // System.out.println("Item (" + (i++) + ") - " + item.getItemData().getDescription() + " completed");
            //System.out.println("Item  " + ((JobRequest)item.getItemData()).getJobID() +  " / " + item.getId() + " completed");
            
            if( ((i+1) % 100) == 0 )
            {
               System.out.println("T+" + (long)((System.currentTimeMillis() - startTime)/1000L) + "s - " +  i + " items completed.");               
            }

            if( Math.random() >= 0.8) queueManager.inItemDoneFailure(item);
            else queueManager.inItemDoneSuccess(item);
         }
      }
      catch (Throwable e)
      {
         e.printStackTrace();
      }
   }
}
