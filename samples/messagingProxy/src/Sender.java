import java.util.Date;

import com.teletalk.jserver.*;
import com.teletalk.jserver.tcp.messaging.*;
import com.teletalk.jserver.tcp.messaging.rpc.*;
import com.teletalk.jserver.util.StringUtils;

/**
 * Sender
 */
public class Sender extends SubSystem
{
   public static JServer server = null;
   
   public static void main(String args[])
   {
      try
      {
         System.out.println("Creating Sender! - " + JServer.getVersionString());
         
         createServer();

         System.out.println("Sender started!");
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }
   
   public static void createServer()
   {
      try
      {
         server = new JServer("Sender");
         
         server.addSubSystem(new Sender(server));
         
         server.startJServer();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }
   
   
   
   private final MessagingManager messagingManager;
	
	public Sender(SubSystem parent)
	{
		super(parent, "SenderSystem", false);
		
      this.messagingManager = new MessagingManager(this, "MessagingManager");
		this.addSubSystem(this.messagingManager);
		
		this.messagingManager.addDestination("localhost", 12121);
	}
	
	protected void doInitialize()
	{
		super.doInitialize();
		
		this.messagingManager.engage();
	}
	
	protected void doShutDown()
	{
		super.doShutDown();
 
		this.messagingManager.shutDown();
	}

	public void run()
	{
      try
      {
         this.messagingManager.waitForLinkEstablished();

         MessagingRpcInterface messagingRpcInterface = this.messagingManager.getMessagingRpcInterface("test.messagingProxy");
         TestInterface testInterface = (TestInterface)messagingRpcInterface.createProxy(TestInterface.class);
         String response;
            
         /*while(true)
         {
            System.out.println(new Date() + " - remote receiver names:");
            System.out.println(StringUtils.toString(messagingManager.getRemoteMessageReceiverNames()));
            Thread.sleep(2000);
         }*/
         String giganticString = "Mu"; //new String(new byte[20*1024*1024]);
         
         
   		for(int i=0; canRun; i++)
   		{
            System.out.println("");
            System.out.println("####");
            System.out.println("");
            
            for(int op=0; op<3; op++)
            {
               try
      			{
                  System.out.println("-----");
                  System.out.println("Invoking dynamic proxy RPC method (testMethod, operation " + op +")...");
                  
                  if( op == 2 ) response = testInterface.testMethod(giganticString, op);
                  else response = testInterface.testMethod("I'm a little coffepot!", op);
                  
                  System.out.println("Received response : " + response);
      			}
               // Note: Since RpcException is a checked exception it is wrapped in a RpcExceptionWrapper 
               // when a dynamic proxy is used.               
               catch(RpcExceptionWrapper e)
               {
                  System.out.println("Remote error - " + e);
               }
               catch(Exception e)
               {
                  System.out.println("ERROR - " + e);
               }
            }
            
            try
            {
               Thread.sleep(1000);
            }
            catch(Exception e){}
   		}
      }
      catch(Throwable e)
      {
         System.out.println("Fatal error in ClientController!");
         e.printStackTrace();
      }
	}
}
