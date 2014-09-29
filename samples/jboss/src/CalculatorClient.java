import com.teletalk.jserver.JServer;
import com.teletalk.jserver.tcp.messaging.MessagingManager;
import com.teletalk.jserver.tcp.messaging.rpc.MessagingRpcInterface;


public class CalculatorClient
{

   /**
    * @param args
    */
   public static void main(String[] args)
   {
      try
      {
         JServer jserver = new JServer();
         
         MessagingManager messagingManager = new MessagingManager();
         jserver.addSubSystem(messagingManager);
         jserver.startJServer();
         jserver.waitForEnabled(10000);
         
         System.out.println("Waiting for link...");
         messagingManager.waitForLinkEstablished();
         System.out.println("Link established");
         
         MessagingRpcInterface messagingRpcInterface = messagingManager.getMessagingRpcInterface("calculator");
         RemoteCalculator calc = (RemoteCalculator)messagingRpcInterface.createProxy(RemoteCalculator.class);
         
         System.out.println("Result: " + calc.calculate(1, 2, 0.5, 1000));
      }
      catch (Exception e) 
      {
         e.printStackTrace();
      }
   }
}
