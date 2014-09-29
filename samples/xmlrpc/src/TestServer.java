import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;

import org.apache.xmlrpc.*;
import org.apache.xmlrpc.secure.*;

import com.teletalk.jserver.*;

import com.teletalk.jserver.tcp.http.*;
import com.teletalk.jserver.tcp.http.xmlrpc.XmlRpcCommunicationManager;

public class TestServer implements AuthenticatedXmlRpcHandler
{
	public static JServer server = null;
	
	public static void main(String args[])
	{
		try
		{
			System.out.println("Creating TestServer! - " + JServer.getVersionString() + "");

         server = new JServer("TestServer");
         
         XmlRpcCommunicationManager xmlRpcCommunicationManager = new XmlRpcCommunicationManager(server, 12345);
         xmlRpcCommunicationManager.addHandler("test", new TestServer());
         server.addSubSystem(xmlRpcCommunicationManager);
                           
         server.startJServer();
						
			System.out.println("TestServer started!");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
   public java.lang.Object execute(java.lang.String method,
                                   java.util.Vector params,
                                   java.lang.String user,
                                   java.lang.String password)
                            throws java.lang.Exception
   {
      System.out.println("user: " + user + ", password: " + password);
      return new Invoker(this).execute(method, params);
   }
   
   public String testMethod(String param)
   {
      System.out.println("testMethod invoked - param: " + param);
      return "Success!";
   }
}
