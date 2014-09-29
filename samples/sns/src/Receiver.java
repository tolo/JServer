import com.teletalk.jserver.JServer;
import com.teletalk.jserver.tcp.messaging.MessageReceiverComponent;
import com.teletalk.jserver.tcp.messaging.MessagingManager;

/**
 * Receiver
 */
public class Receiver
{
	public static JServer server = null;
	
	public static void main(String args[])
	{
		try
		{
			System.out.println("Creating Receiver! - " + JServer.getVersionString());

			createServer();
						
			System.out.println("Receiver started!");
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
         server = new JServer("Receiver");
         server.setServerVersion("1.2.3");
			
         MessagingManager msgMan = new MessagingManager(server, "MessagingManager");
         msgMan.setExposeMessageReceiversAsServices(true);
                  
         /*msgMan.addServerAddress("localhost", 13131);*/
         /*msgMan.addLocalServiceName("Receiver");*/
                  
         server.addSubSystem(msgMan);
                  
         //MessageReceiverComponent messageReceiverComponent = new MessageReceiverComponent(msgMan, "MessageReceiverComponent", "test.sns");
         MessageReceiverComponent messageReceiverComponent = new MessageReceiverComponent(msgMan, "MessageReceiverComponent", "Receiver");
         msgMan.registerMessageReceiverComponent(messageReceiverComponent);
         messageReceiverComponent.getRpcHandler().setDefaultHandler(new TestImpl());
         			
			server.startJServer();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void startService(String args[])
	{
		createServer();
	}
	
	public static void stopService()
	{
		if(server != null) server.shutDown();		   
	}
}
