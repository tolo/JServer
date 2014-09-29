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
   public String testMethod(String string)
   {
      System.out.println("testMethod - " + string);
      
      return new StringBuffer(string).reverse().toString();
   }
}

