import com.teletalk.jserver.tcp.messaging.Message;
import com.teletalk.jserver.tcp.messaging.MessageReceiverComponent;
import com.teletalk.jserver.tcp.messaging.MessagingManager;
import com.teletalk.jserver.tcp.messaging.rpc.RemoteProcedureCall;

/**
 * TestImpl
 */
public class TestImpl implements TestInterface
{
   /**
    * RPC method
    */ 
   public String testMethod(String string, int op)
   {
      try
      {
         if( op == 2 )
         {
            System.out.println("testMethod " + op + " - string length: " + string.length());
            
            Thread.sleep(1000);
            
            return "String length: " + string.length();
         }
         else
         {
            System.out.println("testMethod " + op + " - " + string);
            
            Thread.sleep(1000);
            
            return new StringBuffer(string).reverse().toString();
         }
      }
      catch (Exception e) 
      {
         throw new RuntimeException(e);
      }
   }
}

