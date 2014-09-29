import com.teletalk.jserver.event.Event;
import com.teletalk.jserver.rmi.remote.RemoteEvent;

/**
 * 
 */
public final class TestEvent extends Event
{
	private final int id;
	
	public TestEvent(Object source, int id)
	{
		super(source);
		this.id = id;
	}
	
	public RemoteEvent createRemoteEvent()
	{
		return new RemoteTestEvent( ((super.source != null) ? super.source.toString() : ""), this.id);
	}
}
