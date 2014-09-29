import com.teletalk.jserver.*;
import com.teletalk.jserver.log.*;

public class PropertyServer
{
	public static void main(String args[])
	{
		try
		{
			System.out.println("Creating PropertyServer! (" + JServer.getVersionString() + ")");
			
			JServer server = new JServer("PropertyServer");
			
			server.addSubSystem(new PropertySystem(server), false);
			
			server.startJServer();
			System.out.println("TestServer started!");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
