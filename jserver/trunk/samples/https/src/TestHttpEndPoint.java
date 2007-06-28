import com.teletalk.jserver.tcp.*;
import com.teletalk.jserver.tcp.http.*;
import com.teletalk.jserver.*;
import com.teletalk.jserver.log.*;

import java.io.*;
import java.util.*;

public class TestHttpEndPoint extends DefaultHttpEndPoint
{
	public TestHttpEndPoint(HttpCommunicationManager server)
	{
		super(server);
	}
	
	public TestHttpEndPoint(HttpCommunicationManager server, String name)
	{
		super(server, name);
	}
	
   protected void handleRequest(final HttpRequest req) throws IOException
	{
		StringBuffer responseString = new StringBuffer();  //Create a string buffer for the html-response
      
         try
         {
            File f = new File("test.html");
            FileInputStream fi = new FileInputStream(f);
            byte[] b = new byte[(int)f.length()];
            fi.read(b);
            
            responseString.append(new String(b) + "\r\n<br>\r\n<br>\r\n");
         }
         catch(Exception e)
         {
            e.printStackTrace();
         }
        
        System.out.println("\r\n=========================================================");
		System.out.println(toString() + " - Receiving request");
		
		try
		{
			responseString.append("Request:<br><br>");
			responseString.append(req.toString());
			responseString.append("<br><br>Request data/querystring data:");
			HttpRequestData data = req.getRequestData();  //Extract data from the request
						
			{
				responseString.append("<br><br>---<br><br>");
				responseString.append(key);
				Object [] vals = data.getValues(key);
				if(vals.length == 1)
				{
					if(vals[0] == null) continue;
					
					if(vals[0] instanceof MessageBodyPart)
					{
						MessageBodyPart mbp = (MessageBodyPart)vals[0];
						//If a file has been transferred with the request, attempt to save it
							String fileName = mbp.getFileName();
							if(mbp.getFileName().lastIndexOf("\\") > 0)
							System.out.println("Saving " + f.getAbsolutePath());
					}
					{
						responseString.append("<br><strong>Value " +i + ":</strong><br>");
						if(vals[i] == null) continue;
					}	
			}
			super.sendOkResponse(responseString.toString());
		catch(Exception e)
		{
			e.printStackTrace();	
		}
		
		System.out.println(toString() + " - Finished processing request");
	}
}