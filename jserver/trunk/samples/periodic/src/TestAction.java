import java.text.DateFormat;
import java.util.Date;

import com.teletalk.jserver.periodic.PeriodicAction;

/**
 * 
 */
public class TestAction extends PeriodicAction
{
   /**
    * 
    */
   public TestAction(String name)
   {
      super(name);
   }
   
   /**
    * 
    */
   public boolean execute() throws Exception
   {
      DateFormat df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.MEDIUM);
      System.out.println(df.format(new Date()) + " - " + Thread.currentThread().getName() + " - Starting....");
      
      for(int i=0; i<10; i++)
      {
         super.setActionStatus("Status " + i);
          System.out.println(df.format(new Date()) + " - Status " + i);
         try
         {
            Thread.sleep(1000);
         }
         catch(Exception e)
         {
            e.printStackTrace();
         }
      }
      
      System.out.println(df.format(new Date()) + " - " + Thread.currentThread().getName() + " - DONE!");
      super.setLastExecutionResult("Successbritt - " + System.currentTimeMillis());
      
      return true;
   }
}
