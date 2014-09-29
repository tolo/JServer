import com.teletalk.jserver.*;

public class ReceiverServer
{
	public static JServer server = null;
	
	public static void main(String args[])
	{
		try
		{
			System.out.println("Creating ReceiverServer! - " + JServer.getVersionString() + "");

			createServer();
						
			System.out.println("ReceiverServer started!");
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
			server = new JServer("ReceiverServer");
			
         ReceiverController receiverController = new ReceiverController(server);
			server.addSubSystem(receiverController);
						
			server.startJServer();
         
         // Be mean!
         /*while(true)
         {
            Thread.sleep(20000 + (long)(60000*Math.random()));
            
            System.out.println("Killing ReceiverController");
            receiverController.shutDown();
            receiverController.waitForDown(60000);
            Thread.sleep(10000);
            System.out.println("Starting ReceiverController");
            receiverController.engage();
            receiverController.waitForEnabled(60000);
            System.out.println("ReceiverController running");
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
		if(server != null) server.stopJServer();		   
	}
}
