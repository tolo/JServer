import java.util.Date;

import com.teletalk.jserver.*;
import com.teletalk.jserver.tcp.messaging.*;
import com.teletalk.jserver.tcp.messaging.rpc.*;
import com.teletalk.jserver.util.StringUtils;

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
			
         MessagingManager msgMan = new MessagingManager(server, "MessagingManager");
         
         msgMan.addDestination("localhost", 12121); 
                  
         MessageReceiverComponent messageReceiverComponent = new MessageReceiverComponent(msgMan, "MessageReceiverComponent", "test.messagingProxy");
         msgMan.registerMessageReceiverComponent(messageReceiverComponent);
         messageReceiverComponent.getRpcHandler().setDefaultHandler(new TestImpl());
                  
         server.addSubSystem(msgMan);
			
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
