import com.teletalk.jserver.*;

import com.teletalk.jserver.tcp.*;
import com.teletalk.jserver.tcp.http.*;

public class TestHttpServer
{
	public static JServer server = null;
	
	public static void main(String args[])
	{
		try
		{
			System.out.println("Creating TestHttpServer! - " + JServer.getVersionString() + "");

			createServer();
						
			System.out.println("TestHttpServer started!");
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
			server = new JServer("TestHttpServer");
			
			server.setServerVersion("1.2.3");
			
         HttpCommunicationManager httpCommunicationManager = new HttpCommunicationManager(server, "HttpCommunicationManager", new TcpEndPointIdentifier(8765), TestHttpEndPoint.class);
         server.addSubSystem(httpCommunicationManager);
									
			server.startJServer();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
