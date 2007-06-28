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
   			HttpRequestData data = req.getRequestData();  // Extract data from the request   			System.out.println("Request : \r\n\r\n" + req + "\r\n");   			   			System.out.println("Testfield1 is : " + data.getSingleStringValue("Testfield1") + "\r\n");   			System.out.println("Testfield2 is : " + data.getSingleStringValue("Testfield2") + "\r\n");   			System.out.println("Testfield3 is : " + data.getSingleStringValue("Testfield3") + "\r\n");
   						
            if( data != null )
            {      			HashMap mappings = data.getMappings();  //Get the key/value mappings for the data in the request      			      			//Iterate through all key/value mappings and generate a response describing the data transferred in the request.      			for(Enumeration e = Collections.enumeration(mappings.keySet()); e.hasMoreElements(); )
      			{
      				responseString.append("<br><br>---<br><br>");      				String key = (String)e.nextElement();	      				responseString.append("<strong>Key:</strong> <br>");
      				responseString.append(key);
						System.out.println("Key:");
						System.out.println(key);
      				Object [] vals = data.getValues(key);      				if(vals == null) continue;
      				if(vals.length == 1)
      				{      					responseString.append("<br><strong>Value:</strong><br>");
      					if(vals[0] == null) continue;
      					      					responseString.append(vals[0].toString());
      					
						System.out.println("Value:");
						System.out.println(vals[0].toString());
      					
      					if(vals[0] instanceof MessageBodyPart)
      					{
      						MessageBodyPart mbp = (MessageBodyPart)vals[0];
      						//If a file has been transferred with the request, attempt to save it      						if( (mbp.getFileName() != null) && (mbp.getFileName().length() > 0) )      						{
      							String fileName = mbp.getFileName();      							
      							if(mbp.getFileName().lastIndexOf("\\") > 0) fileName = fileName.substring(mbp.getFileName().lastIndexOf("\\")+1);      							      							File f = new File(fileName);
      							System.out.println("Saving " + f.getAbsolutePath());      							FileOutputStream fo = new FileOutputStream(f);      							fo.write(mbp.getData());      							fo.close();      						}
      					}      				}      				else      				{      					for(int i=0; i<vals.length; i++)
      					{
      						responseString.append("<br><strong>Value " +i + ":</strong><br>");
      						if(vals[i] == null) continue;      						responseString.append(vals[i].toString());
								System.out.println("Value:");
								System.out.println(vals[i].toString());
      					}	      				}
      			}
            }
            HttpResponse rsp = new HttpResponse(200, "OK", responseString.toString());
            rsp.setContentType("text/html");
            
            super.sendResponse(rsp);
   			//super.sendOkResponse(responseString.toString());
         }   		catch(Exception e)
   		{
   			e.printStackTrace();	
   		}
		
		System.out.println(toString() + " - Finished processing request");
	}
}
