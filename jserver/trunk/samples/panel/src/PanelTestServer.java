import com.teletalk.jserver.*;

import com.teletalk.jserver.tcp.*;

public class PanelTestServer
{
	public static JServer server = null;
	
	public static void main(String args[])
	{
		try
		{
			System.out.println("Creating PanelTestServer! - " + JServer.getVersionString() + "");
			java.util.Locale.setDefault(new java.util.Locale("en", "US"));
			createServer();
						
			System.out.println("PanelTestServer started!");
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
			server = new JServer("PanelTestServer");
			
			TestSystem testSystem = new TestSystem(server);
			server.addSubSystem(testSystem);
			
			server.getAdministration().addCustomAdmPanel(TestPanel.class, new Object[]{testSystem.getRmiAdapter(), "Tjena"});
												
			server.startJServer();
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
