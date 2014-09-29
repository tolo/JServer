import com.teletalk.jserver.*;

/**
 * Client main class.
 */
public class Client
{
	public static JServer server = null;
	
	public static void main(String args[])
	{
		try
		{
			System.out.println("Creating SprintTestClient! - " + JServer.getVersionString());
			
			createServer();

			System.out.println("SprintTestClient started!");
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
			server = new JServer("SprintTestClient");
			
			server.addSubSystem(new ClientCommunicationManager(server));
			
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
