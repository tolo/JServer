import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.tcp.messaging.MessagingManager;
import com.teletalk.jserver.tcp.messaging.rpc.MessagingRpcInterface;
import com.teletalk.jserver.tcp.messaging.rpc.RpcExceptionWrapper;

/**
 * Client class handling the actual communication.
 */
public class ClientCommunicationManager extends SubSystem
{
   private final MessagingManager messagingManager;
	
	public ClientCommunicationManager(SubSystem parent)
	{
		super(parent, "ClientCommunicationManager", false);
		
      this.messagingManager = new MessagingManager(this, "MessagingManager");
		this.addSubSystem(this.messagingManager);
		
		this.messagingManager.addDestination("localhost", 11233);
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

         MessagingRpcInterface messagingRpcInterface = this.messagingManager.getMessagingRpcInterface("examples.springTest");
         TestDao testInterface = (TestDao)messagingRpcInterface.createProxy(TestDao.class, "test");
         String response;
            
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
                  System.out.println("Invoking dynamic proxy RPC method getUserByName1()...");
                  response = testInterface.getUserByName1("Mupp").getName();
                  System.out.println("Received response 1 : " + response);
                  System.out.println("Invoking dynamic proxy RPC method getUserByName2()...");
                  response = testInterface.getUserByName2("Mupp").getName();
                  System.out.println("Received response 2 : " + response);
                  System.out.println("Invoking dynamic proxy RPC method getUserByNameBroken()...");
                  response = testInterface.getUserByNameBroken("Mupp").getName();
                  System.out.println("Received response 3 : " + response);
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
