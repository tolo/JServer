import com.teletalk.jserver.rmi.remote.RemoteEvent;

/**
 * 
 */
public class RemoteTestEvent extends RemoteEvent
{
	private final int id;
	
	public RemoteTestEvent(String source, int id)
	{
		super(source);
		this.id = id;
	}
	
	public String toString()
	{
		return "RemoteTestEvent( id: " + this.id + " )";
	}
}
