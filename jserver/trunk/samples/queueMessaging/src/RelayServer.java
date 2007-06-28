import com.teletalk.jserver.JServer;

public class RelayServer
{
	public static JServer server = null;
	
	public static void main(String args[])
	{
		try
		{
			System.out.println("Creating RelayServer! (" + JServer.getVersionString() + ")");
			
			createServer();

			System.out.println("RelayServer started!");
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
			server = new JServer("RelayServer");
			
         RelayController relayController = new RelayController(server);
			server.addSubSystem(relayController);
						
			server.startJServer();
         
         // Be mean!
         /*while(true)
         {
            Thread.sleep(20000 + (long)(40000*Math.random()));
            
            System.out.println("Killing RelayController");
            relayController.shutDown();
            relayController.waitForDown(60000);
            Thread.sleep(10000);
            System.out.println("Starting RelayController");
            relayController.engage();
            relayController.waitForEnabled(60000);
            System.out.println("RelayController running");
         }*/
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
