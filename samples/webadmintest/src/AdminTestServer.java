import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.InputStream;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.net.admin.AdministrationManager;
import com.teletalk.jserver.tcp.messaging.Message;
import com.teletalk.jserver.tcp.messaging.MessageDispatcher;
import com.teletalk.jserver.tcp.messaging.MessageHeader;
import com.teletalk.jserver.tcp.messaging.MessagingManager;
import com.teletalk.jserver.tcp.messaging.admin.ServerAdministrationClient;
import com.teletalk.jserver.tcp.messaging.admin.ServerFile;
import com.teletalk.jserver.tcp.messaging.admin.ServerFileInfo;
import com.teletalk.jserver.tcp.messaging.admin.web.CustomAdministrationWebAppInterface;
import com.teletalk.jserver.tcp.messaging.admin.web.jetty.CustomAdministrationWebAppHandler;
import com.teletalk.jserver.tcp.messaging.rpc.MessagingRpcInterface;
import com.teletalk.jserver.tcp.messaging.rpc.RpcInputStream;
import com.teletalk.jserver.util.StringUtils;

/**
 * 
 */
public class AdminTestServer
{
	public static JServer server = null;
	
	public static void main(String args[])
	{
		try
		{
			System.out.println("Creating AdminTestServer! (" + JServer.getVersionString() + ")");
			
			createServer();

			System.out.println("AdminTestServer started!");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
	
	public static void createServer()
	{
      String tmpFileName = "tmp.txt";
      
		try
		{
			server = new JServer("AdminTestServer");
         server.getLogManager().setAddToRootLogger(true);
			
         server.addSubComponent(new CustomAdministrationWebAppHandler(), true);
         
         System.out.println("Creating MessagingManagers");
         
         MessagingManager messagingManager1 = new MessagingManager(server, "MessagingManager1");
         messagingManager1.addDestination(44001);
         server.addSubSystem(messagingManager1);
         
         MessagingManager messagingManager2 = new MessagingManager(server, "MessagingManager2");
         messagingManager2.addServerAddress(44001);
         server.addSubSystem(messagingManager2);
         
			server.getMainHttpServer().setLocalPort(9901);
			
			server.startJServer();
         
         System.out.println("Waiting for link...");
         
         messagingManager1.waitForLinkEstablished();
         
         System.out.println("Dispatching message");
         
         ServerAdministrationClient adminClient = new ServerAdministrationClient(messagingManager1.getMessagingRpcInterface(messagingManager1.getDestinations()[0]));
         
         CustomAdministrationWebAppInterface administrationWebAppInterface = (CustomAdministrationWebAppInterface)adminClient.getRemoteAdministrationHandler(CustomAdministrationWebAppInterface.class, CustomAdministrationWebAppInterface.ADMINISTRATION_HANDLER_NAME);
         
         //dispatchRequest("GET /virtual/contextPath/styles.css HTTP/1.1\r\nhost:localhost\r\n\r\n\r\n", administrationWebAppInterface);
         
         dispatchRequest("GET /virtual/contextPath/ HTTP/1.1\r\nhost:localhost\r\n\r\n\r\n", adminClient, administrationWebAppInterface);
         dispatchRequest("GET /virtual/contextPath/ HTTP/1.1\r\nhost:localhost\r\n\r\n\r\n", adminClient, administrationWebAppInterface);
         
         adminClient.createServerFile(new ServerFile(new ServerFileInfo("tmp.txt", false, System.currentTimeMillis()), ("Test" + System.currentTimeMillis()).getBytes()));
         System.out.println("Server files: " + StringUtils.toString(adminClient.listServerFiles()));
         System.out.println("Server files in bin: " + StringUtils.toString(adminClient.listServerFiles("bin")));
         System.out.println("Contents of tmp.txt: " + new String(adminClient.getServerFile("tmp.txt").getFileData()));
         adminClient.deleteServerFile("tmp.txt");
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
      
      server.stopJServer(true);
	}
   
   public static void dispatchRequest(String request, ServerAdministrationClient adminClient, CustomAdministrationWebAppInterface administrationWebAppInterface) throws Exception
   {
      MessageHeader messageHeader = adminClient.getMessagingRpcInterface().getRpcMessageHeader();
      messageHeader.setCustomHeaderField(CustomAdministrationWebAppInterface.VIRTUAL_CONTEXT_PATH_HEADER_KEY, "/virtual/contextPath");
      MessagingRpcInterface.setContextMessageHeader(messageHeader);
      
      System.out.println("Request: " + request);
      
      byte[] requestBytes = request.getBytes();
      RpcInputStream responseInputStream = (RpcInputStream)administrationWebAppInterface.handle(new RpcInputStream(new ByteArrayInputStream(requestBytes), requestBytes.length));

      DataInputStream dataInputStream = new DataInputStream(responseInputStream);
      byte[] responseBytes = new byte[(int)responseInputStream.getDataLength()];
      dataInputStream.readFully(responseBytes);
      
      System.out.println("Response: " + new String(responseBytes));
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
