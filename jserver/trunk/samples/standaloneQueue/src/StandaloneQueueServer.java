import com.teletalk.jserver.JServer;

/**
 * 
 */
public class StandaloneQueueServer
{
	public static JServer server = null;
	
	public static void main(String args[])
	{
		try
		{
			System.out.println("Creating StandaloneQueueServer! (" + JServer.getVersionString() + ")");
			
			createServer();

			System.out.println("StandaloneQueueServer started!");
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
			server = new JServer("StandaloneQueueServer");
			
			server.addSubComponent(new StandaloneQueueComponent(server), true);
			
			server.getMainHttpServer().setLocalPort(9901);
			
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
