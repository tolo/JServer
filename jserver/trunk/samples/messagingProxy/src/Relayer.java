import com.teletalk.jserver.*;
import com.teletalk.jserver.tcp.messaging.*;
import com.teletalk.jserver.tcp.messaging.rpc.*;

/**
 * Relayer
 */
public class Relayer
{
	public static JServer server = null;
	
	public static void main(String args[])
	{
		try
		{
			System.out.println("Creating Relayer! - " + JServer.getVersionString());

			createServer();
						
			System.out.println("Relayer started!");
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
         server = new JServer("Relayer");
			
         MessagingManager msgMan = new MessagingManager(server, "MessagingManager");
         msgMan.setProxyingEnabled(true);
         
         msgMan.addServerAddress("localhost", 12121);
         
         server.addSubSystem(msgMan);
			
			server.startJServer();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
