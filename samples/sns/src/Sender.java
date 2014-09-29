import com.teletalk.jserver.JServer;
import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.tcp.messaging.MessagingManager;
import com.teletalk.jserver.tcp.messaging.rpc.MessagingRpcInterface;
import com.teletalk.jserver.tcp.messaging.rpc.RpcExceptionWrapper;

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
		
      // Add the name of the service that the MessagingManager is to connect to
		this.messagingManager.addRemoteServiceName("Receiver");
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
         System.out.println("Waiting for available connections...");
         this.messagingManager.waitForLinkEstablished();
         System.out.println("Connections available!");

         //MessagingRpcInterface messagingRpcInterface = this.messagingManager.getMessagingRpcInterface("test.sns");
         MessagingRpcInterface messagingRpcInterface = this.messagingManager.getMessagingRpcInterface("Receiver");
         TestInterface testInterface = (TestInterface)messagingRpcInterface.createProxy(TestInterface.class);
         String response;
            
         /*while(true)
         {
            System.out.println(new Date() + " - remote receiver names:");
            System.out.println(StringUtils.toString(messagingManager.getRemoteMessageReceiverNames()));
            Thread.sleep(2000);
         }*/
         
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
                  response = testInterface.testMethod("I'm a little coffepot!");
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
