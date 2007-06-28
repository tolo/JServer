import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.queue.FileDBQueueStorage;
import com.teletalk.jserver.queue.InOutQueueControllerSystem;
import com.teletalk.jserver.queue.QueueItem;
import com.teletalk.jserver.queue.messaging.MessagingQueueSystemEndPointIdentifier;
import com.teletalk.jserver.queue.messaging.QueueMessagingManager;

/**
 * 
 * @author Tobias Löfstrand
 */
public class RelayController extends InOutQueueControllerSystem
{
	public RelayController(SubSystem parent)
	{
		super(parent, "RelayController", true);
      
      super.queueManager.setQueueCollaborationManager(new QueueMessagingManager(super.queueManager, "QueueMessagingManager", "queueTestRelayer"));

      super.queueManager.getInQueue().setQueueStorage(new FileDBQueueStorage(queueManager.getInQueue()));
      super.queueManager.getOutQueue().setQueueStorage(new FileDBQueueStorage(queueManager.getOutQueue()));
	}
	
	protected void doInitialize()
	{
		super.doInitialize();
	}
	
	public void outItemDoneCancelled(QueueItem item, Object responseData)
	{
		System.out.println(item.getItemData().getDescription() + " done (cancelled))");
	}
	
	public void outItemDoneFailure(QueueItem item, Object responseData)
	{
		System.out.println(item.getItemData().getDescription() + " done (failure))");
	}
	
	public void outItemDoneSuccess(QueueItem item, Object responseData)
	{
		System.out.println(item.getItemData().getDescription() + " done (success))");
	}
	
	public void run()
	{
		System.out.println("ReceiverController: waiting for incoming items...");

		QueueItem item = null; 
					
		try
		{
         super.queueManager.waitForEnabled(10000);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
      MessagingQueueSystemEndPointIdentifier endPointIdentifier = new MessagingQueueSystemEndPointIdentifier("queueTestReceiver");	
      
		while(canRun)
		{
			try
			{
				item = queueManager.getInQueue().checkOutFirst();
			}
			catch(InterruptedException ie)
			{
				if(!canRun) break;	
			}
				
			System.out.println("Relaying item - " + item.getItemData().getDescription());
				
			queueManager.dispatchQueueItem(item.getItemData(), item, endPointIdentifier);
		}
	}
}
