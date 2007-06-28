import com.teletalk.jserver.*;
import com.teletalk.jserver.util.*;

public class Receiver extends SubSystem
{
	private MessageQueue msgQueue;
	
	public Receiver(SubSystem parent)
	{
		super(parent, "Receiver");
		
		msgQueue = new MessageQueue();
	}
	
	public void putMsg(Object msg)
	{
		msgQueue.putMsg(msg);	
	}
	
	public void run()
	{
		while(canRun)
		{
			try
			{
				System.out.println("Receiver: Receiving '" + msgQueue.getMsg() + "'");
			}
			catch(InterruptedException e)
			{
				if(!canRun) break;
			}
		}
	}
}
