import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.queue.FileDBQueueStorage;
import com.teletalk.jserver.queue.Queue;
import com.teletalk.jserver.queue.QueueItem;

/**
 * 
 */
public class StandaloneQueueComponent extends SubComponent
{
   final Queue queue;
   
   private final Timer timer;
   
	
   /**
    */
	public StandaloneQueueComponent(SubSystem parent)
	{
		super(parent, "StandaloneQueueComponent");
      
      this.queue = new Queue(this, "queue");
      this.queue.setQueueStorage(new FileDBQueueStorage(this.queue));
      
      this.timer = new Timer(true);
	}
	
	protected void doInitialize()
	{
		super.doInitialize();
      
      this.queue.engage();
      
      this.timer.schedule(new QueueTimerTask(), 10000);
	}
   
   class QueueTimerTask extends TimerTask
   {
      public void run()
      {
         try
         {
            ArrayList allItems = queue.getAllAsList();
            
            for(int i=0; i<allItems.size(); i++)
            {
               QueueItem item = (QueueItem)allItems.get(i);
               System.out.println("Removing " + item);
               queue.remove(item);
            }
            
            for(int i=0; i<10; i++)
            {
               String id = Integer.toString((int)(Math.random() * 10000));
               QueueItem item = new QueueItem(new DataObject(id), id);
               System.out.println("Adding " + item);
               queue.add(item);
            }
         }
         catch (Exception e) 
         {
            e.printStackTrace();
         }
      }
   }
}
