import com.teletalk.jserver.tcp.*;
import com.teletalk.jserver.tcp.http.*;
import com.teletalk.jserver.*;
import com.teletalk.jserver.log.*;

import java.io.*;
import java.util.*;

public class TestHttpSession extends DefaultHttpConnection
{
	public TestHttpSession(HttpServer server)
	{
		super(server);
	}
	
	public TestHttpSession(HttpServer server, String name)
	{
		super(server, name);
	}
	
	public void request(HttpRequest req) throws java.io.IOException
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
   			HttpRequestData data = req.getRequestData();  // Extract data from the request
   						
            if( data != null )
            {
      			{
      				responseString.append("<br><br>---<br><br>");
      				responseString.append(key);
						System.out.println("Key:");
						System.out.println(key);
      				Object [] vals = data.getValues(key);
      				if(vals.length == 1)
      				{
      					if(vals[0] == null) continue;
      					
      					
						System.out.println("Value:");
						System.out.println(vals[0].toString());
      					
      					if(vals[0] instanceof MessageBodyPart)
      					{
      						MessageBodyPart mbp = (MessageBodyPart)vals[0];
      						//If a file has been transferred with the request, attempt to save it
      							String fileName = mbp.getFileName();
      							if(mbp.getFileName().lastIndexOf("\\") > 0) fileName = fileName.substring(mbp.getFileName().lastIndexOf("\\")+1);
      							System.out.println("Saving " + f.getAbsolutePath());
      					}
      					{
      						responseString.append("<br><strong>Value " +i + ":</strong><br>");
      						if(vals[i] == null) continue;
								System.out.println("Value:");
								System.out.println(vals[i].toString());
      					}	
      			}
            }
            HttpResponse rsp = new HttpResponse(200, "OK", responseString.toString());
            rsp.setContentType("text/html");
            
            super.sendResponse(rsp);
   			//super.sendOkResponse(responseString.toString());
         }
   		{
   			e.printStackTrace();	
   		}
		
		System.out.println(toString() + " - Finished processing request");
	}
}