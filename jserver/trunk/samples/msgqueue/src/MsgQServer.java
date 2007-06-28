import com.teletalk.jserver.*;

public class MsgQServer
{
	public static JServer server = null;
	
	public static void main(String args[])
	{
		try
		{
			System.out.println("Creating MsgQServer! (" + JServer.getVersionString() + ")");
			
			createServer();

			System.out.println("MsgQServer started!");
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
			server = new JServer("MsgQServer");
			
			Receiver r = new Receiver(server);
			
			server.addSubSystem(r);
			
			server.addSubSystem(new Sender(server, r));
			
			server.startJServer();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
