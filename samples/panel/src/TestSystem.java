import com.teletalk.jserver.*;
import com.teletalk.jserver.log.*;
import com.teletalk.jserver.property.*;

public class TestSystem extends SubSystem
{
	private NumberProperty counter;
	private VectorProperty v;
	
	public TestSystem(SubSystem parent)
	{
		super(parent, "TestSystem");
		
		try
		{
			setRmiAdapter(new TestSystemRmiAdapter(this));
		}
		catch(Exception e)
		{
			logError("Error creating TestSystemRmiAdapter"	, e);
		}
	}
	
	public String getTestMessage()
	{
		return "Jag är en liten kaffekanna!";	
	}
	
	public void run()
	{
		int i=0;
		
		while(canRun)
		{
			try
			{
				this.getThread().sleep(5*1000 + (long)(5*1000*Math.random()));
			}
			catch(InterruptedException e)
			{
				if(!canRun) break;
			}

			// Send event
			super.fireGlobalEvent(new TestEvent(this, i++));
		}
	}
}

