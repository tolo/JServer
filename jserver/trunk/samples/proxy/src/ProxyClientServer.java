
import com.teletalk.jserver.*;
import com.teletalk.jserver.tcp.http.*;
import com.teletalk.jserver.tcp.http.proxy.*;
import com.teletalk.jserver.util.*;

public class ProxyClientServer
{
	public static JServer server = null;
	
	public static void main(String args[])
	{
		try
		{
			System.out.println("Creating ProxyClientServer! - " + JServer.getVersionString() + "");

			createServer();
						
			System.out.println("ProxyClientServer started!");
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
			server = new JServer("ProxyClientServer");
			
			server.getMainHttpServer().setLocalPort(9902);
			
			server.addSubSystem(new ProxyClient(server));
			
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
