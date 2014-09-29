import com.teletalk.jserver.queue.*;

public class ItemData implements  QueueItemData
{
	private final String s;
	
	public ItemData(String str)
	{
		s = str;
	}
		
	public String getDescription()
	{
		return s;	
	}
	
	public String toString()
	{
		return getDescription();	
	}
}
