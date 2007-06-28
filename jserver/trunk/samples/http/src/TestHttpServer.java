import java.net.HttpURLConnection;
import java.net.URL;

import com.teletalk.jserver.*;

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
			
			//server.setServerVersion("1.01 (2000-10-20 12:31)");
			
			HttpServer httpServer = server.createHttpServer(8765, TestHttpSession.class);			
			httpServer.setPoolSize(3);
									
			server.startJServer();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
