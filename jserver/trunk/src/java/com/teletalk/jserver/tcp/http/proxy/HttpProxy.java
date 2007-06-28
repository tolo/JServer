/*
 * Copyright 2007 the project originators.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.teletalk.jserver.tcp.http.proxy;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.SequenceInputStream;

import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.property.StringProperty;
import com.teletalk.jserver.tcp.TcpEndPointIdentifier;
import com.teletalk.jserver.tcp.http.HttpCommunicationManager;
import com.teletalk.jserver.tcp.http.HttpConstants;
import com.teletalk.jserver.tcp.http.HttpEndPoint;
import com.teletalk.jserver.tcp.http.HttpRequest;
import com.teletalk.jserver.tcp.http.HttpResponse;
import com.teletalk.jserver.tcp.messaging.Message;
import com.teletalk.jserver.tcp.messaging.MessageDispatchFailedException;
import com.teletalk.jserver.tcp.messaging.MessageHeader;
import com.teletalk.jserver.tcp.messaging.MessagingManager;
import com.teletalk.jserver.tcp.messaging.ResponseTimeOutException;

/**
 * Class that implements a HTTP proxy that can relay HTTP requests to connected servers (proxy clients). This class 
 * inherits {@link HttpCommunicationManager}, which means that it can listen on sever local addreses/ports for 
 * incomming HTTP requests. Requests are relayed to clients using a {@link MessagingManager} subsystem, which 
 * also is capable of listenling on several local addresses/ports.<br>
 * <br>
 * Proxy clients must use the class {@link HttpProxyClient} for communicating with the proxy.
 *  
 * @since 1.2
 * 
 * @author Tobias Löfstrand
 */
public class HttpProxy extends HttpCommunicationManager
{
	/** The default local port that the HttpProxy will listen for HTTP connections on. */
	public static final int DEFAULT_HTTP_PROXY_PORT = 9090;
	
	/** The default local port that the HttpProxy (or to be precise - it's child system "ProxyClientManager", which 
	 * is a {@link MessagingManager}) will listen fror proxy client connections. */
	public static final int DEFAULT_HTTP_PROXY_CLIENT_MANAGER_PORT = 9191;
	
	private final MessagingManager proxyClientManager;
   
   private final StringProperty namedReceiver;
	
	/**
	 * Creates a new HttpProxy that will listen for connections on the default localhost ports. 
	 * 
	 * @param parent the parent subsystem of this HttpProxy.
	 * 
	 * @see #DEFAULT_HTTP_PROXY_PORT
	 * @see #DEFAULT_HTTP_PROXY_CLIENT_MANAGER_PORT
	 */
	public HttpProxy(SubSystem parent)
	{
		this(parent, "HttpProxy");
	}
	
	/**
	 * Creates a new HttpProxy that will listen for connections on the default localhost ports. 
	 * 
	 * @param parent the parent subsystem of this HttpProxy.
	 * @param name the name of this HttpProxy.
	 * 
	 * @see #DEFAULT_HTTP_PROXY_PORT
	 * @see #DEFAULT_HTTP_PROXY_CLIENT_MANAGER_PORT
	 */
	public HttpProxy(SubSystem parent, String name)
	{
		this(parent, name, new TcpEndPointIdentifier(DEFAULT_HTTP_PROXY_PORT), new TcpEndPointIdentifier(DEFAULT_HTTP_PROXY_CLIENT_MANAGER_PORT));
	}
   
   /**
    * Creates a new HttpProxy that will listen for connections on the specified localhost ports. 
    * 
    * @param parent the parent subsystem of this HttpProxy.
    * @param httpPort the localhost port on which this HttpProxy will listen for HTTP connections.
    * @param clientManagerPort the localhost port on which this HttpProxy will listen for proxy client connections.
    * 
    * @see #DEFAULT_HTTP_PROXY_PORT
    * @see #DEFAULT_HTTP_PROXY_CLIENT_MANAGER_PORT
    */
   public HttpProxy(SubSystem parent, int httpPort, int clientManagerPort)
   {
      this(parent, "HttpProxy", httpPort, clientManagerPort);
   }
	
	/**
	 * Creates a new HttpProxy that will listen for connections on the specified localhost ports. 
	 * 
	 * @param parent the parent subsystem of this HttpProxy.
	 * @param name the name of this HttpProxy.
	 * @param httpPort the localhost port on which this HttpProxy will listen for HTTP connections.
	 * @param clientManagerPort the localhost port on which this HttpProxy will listen for proxy client connections.
	 */
	public HttpProxy(SubSystem parent, String name, int httpPort, int clientManagerPort)
	{
		this(parent, name, new TcpEndPointIdentifier(httpPort), new TcpEndPointIdentifier(clientManagerPort));
	}
	
	/**
	 * Creates a new HttpProxy that will listen for connections on the specified local addresses. 
	 * 
	 * @param parent the parent subsystem of this HttpProxy.
	 * @param name the name of this HttpProxy.
	 * @param httpaddress the local address on which this HttpProxy will listen for HTTP connections.
	 * @param clientManagerAddress the local address on which this HttpProxy will listen for proxy client connections.
	 */
	public HttpProxy(SubSystem parent, String name, TcpEndPointIdentifier httpaddress, TcpEndPointIdentifier clientManagerAddress)
	{
		this(parent, name, new TcpEndPointIdentifier[]{httpaddress}, new TcpEndPointIdentifier[]{clientManagerAddress});
	}
	
	/**
	 * Creates a new HttpProxy that will listen for connections on the specified local addresses. 
	 * 
	 * @param parent the parent subsystem of this HttpProxy.
	 * @param name the name of this HttpProxy.
	 * @param httpaddresses the local addresses on which this HttpProxy will listen for HTTP connections.
	 * @param clientManagerAddresses the local addresses on which this HttpProxy will listen for proxy client connections.
	 */
	public HttpProxy(SubSystem parent, String name, TcpEndPointIdentifier[] httpaddresses, TcpEndPointIdentifier[] clientManagerAddresses)
	{
		super(parent, name, httpaddresses);
						
		this.proxyClientManager = new MessagingManager(this, "ProxyClientManager", null, clientManagerAddresses);
				
		this.addSubSystem(this.proxyClientManager);
      
      this.namedReceiver = new StringProperty(this, "named receiver", "", StringProperty.MODIFIABLE_NO_RESTART, true);
      this.namedReceiver.setDescription("The name of the named receiver in the proxy client, which will receive proxied messages. Leave empty if no named receiver is to be used.");
      
      super.addProperty(this.namedReceiver);
	}
	
	/**
	 * Initialization method for this HttpProxy.
	 */
	protected void doInitialize()
	{
		super.doInitialize();
		
		if( !super.isReinitializing() )
		{
			this.proxyClientManager.engage();
		}
	}
	
	/**
	 * Shut down method fot this HttpProxy.
	 */
	protected void doShutDown()
	{
		super.doShutDown();
		
		if( !super.isReinitializing() )
		{
			this.proxyClientManager.shutDown();
		}
	}
			
	/**
	 * Method for handling incomming HTTP requests.
	 * 
	 * @param request a HTTP request received from a client.
	 * @param endPoint the endpoint the request was received on.
	 * 
	 * @return the HTTP response that should be returned to the client that sent the request.
	 */
	public HttpResponse handleRequest(final HttpRequest request, final HttpEndPoint endPoint)
	{
		InputStream requestInputStream;
		Message response;
		
		try
		{
			if( super.isDebugMode() ) logDebug("Relaying request: " + request + ". Client address: " + endPoint.getRemoteAddress() + ". Local address: " + endPoint.getLocalAddress() + ".");
			
			TcpEndPointIdentifier remoteEPI= endPoint.getRemoteAddress();
			TcpEndPointIdentifier localEPI= endPoint.getLocalAddress();
			
			String clientAddress = remoteEPI.getAddress();
			if(clientAddress == null) clientAddress = "localhost";
			
			String serverAddress = localEPI.getAddress();
			if(serverAddress == null) serverAddress = "localhost";
			
			request.addHeader(HttpConstants.REMOTE_ADDRESS_HEADER_KEY, clientAddress);
			request.addHeader(HttpConstants.REMOTE_PORT_HEADER_KEY, String.valueOf(remoteEPI.getPort()));
									
			request.addHeader(HttpConstants.LOCAL_ADDRESS_HEADER_KEY, serverAddress);
			request.addHeader(HttpConstants.LOCAL_PORT_HEADER_KEY, String.valueOf(localEPI.getPort()));
			
			final byte[] requestMessageData = request.getRequestMessage().getBytes(HttpConstants.DEFAULT_CHARACTER_ENCODING);
			final ByteArrayInputStream requestMessageDataInputStream = new ByteArrayInputStream(requestMessageData);
		
			// If the request contains a body...
			if( request.getContentLength() > 0 )
			{
				// ...combine the (byte array) input stream for reading the request message with the stream used to read the request body
				requestInputStream = new SequenceInputStream(requestMessageDataInputStream, request.getRequestInputStream());
			}
			else
			{
				requestInputStream = requestMessageDataInputStream;
			}
         
         String namedReceiverName = this.namedReceiver.stringValue();
			
         // Relay the request
         if( (namedReceiverName != null) && (namedReceiverName.trim().length() > 0) )
         { 
            response = this.proxyClientManager.dispatchMessage(new MessageHeader(1), requestInputStream, (requestMessageData.length + request.getContentLength()), namedReceiverName.trim());
         }
         else
         {
			   response = this.proxyClientManager.dispatchMessage(new MessageHeader(1), requestInputStream, (requestMessageData.length + request.getContentLength()) );
         }
         			
			if( super.isDebugMode() ) logDebug("Received response (" + response + ") for request " + request + ".");
		
			return new HttpResponse(response.getBodyAsStream());
		}
		catch(MessageDispatchFailedException mdfe)
		{
			logWarning("Unable to relay request (" + request + ").", mdfe);
			return new HttpResponse(503, "Service offline");
		}
		catch(ResponseTimeOutException rtoe)
		{
			logWarning("Timeout occurred while waiting for response to request (" + request + ").", rtoe);
			return new HttpResponse(504, "Timeout occurred");
		}
		catch(Exception e)
		{
			logError("Error occurred while relaying request (" + request + ").", e);
			return new HttpResponse(500, "Internal server error");
		}
	}
}
