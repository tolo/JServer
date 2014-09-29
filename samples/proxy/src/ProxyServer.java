
import com.teletalk.jserver.*;
import com.teletalk.jserver.tcp.http.*;
import com.teletalk.jserver.tcp.http.proxy.*;

public class ProxyServer
{
	public static JServer server = null;
	
	public static void main(String args[])
	{
		try
		{
			System.out.println("Creating ProxyServer! (" + JServer.getVersionString() + ")");
			
			createServer();

			System.out.println("ProxyServer started!");
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
			server = new JServer("ProxyServer");

			server.getMainHttpServer().setLocalPort(9901);
			
			server.addSubSystem(new HttpProxy(server));
			
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
