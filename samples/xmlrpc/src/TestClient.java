import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Vector;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import javax.net.ssl.SSLSession;

import org.apache.xmlrpc.*;

/**
 */
public class TestClient
{
	public static void main(String[] args)
	{
		try
		{
         // Uncomment if all certificates are to be trusted
         TrustManager[] trustAllCerts = new TrustManager[]{
             new X509TrustManager() 
             {
                 public java.security.cert.X509Certificate[] getAcceptedIssuers() 
                 {
                     return null;
                 }
                 public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) 
                 {
                 }
                 public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) 
                 {
                 }
             }
         };
         try 
         {
             SSLContext sc = SSLContext.getInstance("SSL");
             sc.init(null, trustAllCerts, new java.security.SecureRandom());
             HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
         } catch (Exception e){}
         

         
         // Uncomment if certificate host name mismatches are to be accepted 
         HostnameVerifier hv = new HostnameVerifier()
         {
            public boolean verify(String s1, SSLSession session)
            {
               return true;
            }
         };
         
         //XmlRpc.debug = true; 
         XmlRpcClient xmlrpc = new XmlRpcClient("https://localhost:12345/RPC");
         xmlrpc.setBasicAuthentication("testuser", "testpass");
         
			Vector params = new Vector ();
			params.addElement ("param1");
			String result = (String) xmlrpc.execute("test.testMethod", params);
			
			System.out.println("Result: " + result);
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
	}
}
