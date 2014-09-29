import com.teletalk.jserver.*;

public class Sender extends SubSystem
{
	private final Receiver receiver;
	
	public Sender(SubSystem parent, Receiver receiver)
	{
		super(parent, "Sender");
	
		this.receiver = receiver;
	}
	
	public void run()
	{
		int counter = 0;
		
		while(canRun)
		{
			System.out.println("Sender: Sending messages to Receiver...");
			
			for(int m=0; m<10; m++)
			{
				receiver.putMsg("Msg nr " + (counter++));
			}
			
			System.out.println("Sender: Sleeping...");
			
			try
			{
				this.getThread().sleep(10*1000 + (long)(10*1000*Math.random()));
			}
			catch(InterruptedException e)
			{
				if(!canRun) break;
			}
			
			System.out.println("Sender: Waking up...");
		}
	}
}
