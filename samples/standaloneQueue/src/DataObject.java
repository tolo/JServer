import com.teletalk.jserver.queue.QueueItemData;

/**
 * 
 */
public class DataObject implements QueueItemData
{
   private final String string;
   
   public DataObject(String string)
   {
      this.string = string;
   }
   
   public String getDescription()
   {
      return this.string;
   }
}
