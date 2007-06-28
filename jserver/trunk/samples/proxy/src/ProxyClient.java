
import com.teletalk.jserver.*;
import com.teletalk.jserver.tcp.http.*;
import com.teletalk.jserver.tcp.http.proxy.*;

public class ProxyClient extends HttpProxyClient
{
	public ProxyClient(SubSystem parent)
	{
		super(parent, "ProxyClient");
		
		super.addDestination("localhost", 9191);
	}
			
	/**
	 */
	public HttpResponse handleRequest(final HttpRequest request)
	{
		System.out.println("Received request: " + request);
		System.out.println("Client address is: " + request.getHeaderSingleValue(HttpConstants.REMOTE_ADDRESS_HEADER_KEY));
		
		System.out.println("Request data: " + request.getRequestData());
		
		return new HttpResponse(200, "OK", "Kaffekanna!!!!!!!!!");
	}
}
