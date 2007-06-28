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
package com.teletalk.jserver.tcp.http;

import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.tcp.TcpCommunicationManager;
import com.teletalk.jserver.tcp.TcpEndPointIdentifier;

/**
 * This class implements a manager for HTTP communication. HttpCommunicationManager is a subclass of 
 * {@link TcpCommunicationManager} and is capable of receiving HTTP requests on multiple addresses.<br>
 * <br>
 * The actual HTTP communication logic is handled by the class {@link HttpEndPoint}.<br>
 * <br>
 * To handle incomming requests, subclasses must implement the method {@link #handleRequest(HttpRequest, HttpEndPoint)}.
 * 
 * @since 1.2 Build 635
 * 
 * @author Tobias Löfstrand
 */
public class HttpCommunicationManager extends TcpCommunicationManager
{
	/**
	 * Creates a HttpCommunicationManager without any tcp servers listening for connections. 
	 * <br><br>The created HttpCommunicationManager will be named "HttpCommunicationManager".
	 * 
	 * @param parent the parent of this HttpCommunicationManager.
	 */
	public HttpCommunicationManager(SubSystem parent)
	{
		super(parent, "HttpCommunicationManager", HttpEndPoint.class);
	}
	
	/**
	 * Creates a HttpCommunicationManager without any tcp servers listening for connections. 
	 * <br><br>The created HttpCommunicationManager will be named "HttpCommunicationManager".
	 * 
	 * @param parent the parent of this HttpCommunicationManager.
	 */
	public HttpCommunicationManager(SubSystem parent, Class tcpEndPointClass)
	{
		super(parent, "HttpCommunicationManager", tcpEndPointClass);
	}
			
	/**
	 * Creates a HttpCommunicationManager without any tcp servers listening for connections.
	 * 
	 * @param parent the parent of this HttpCommunicationManager.
	 * @param name the name that will be given to this HttpCommunicationManager.
	 */
	public HttpCommunicationManager(SubSystem parent, String name)
	{
		super(parent, name, HttpEndPoint.class);
	}
	
	/**
	 * Creates a HttpCommunicationManager without any tcp servers listening for connections.
	 * 
	 * @param parent the parent of this HttpCommunicationManager.
	 * @param name the name that will be given to this HttpCommunicationManager.
    * @param tcpEndPointClass the endpoint class.
	 */
	public HttpCommunicationManager(SubSystem parent, String name, Class tcpEndPointClass)
	{
		super(parent, name, tcpEndPointClass);
	}
			
	/**
	 * Creates a HttpCommunicationManager with a single tcp server, named "TcpServer", listening for connections. 
	 * <br><br>The created HttpCommunicationManager will be named "HttpCommunicationManager".
	 * 
	 * @param parent the parent of this HttpCommunicationManager.
	 * @param address the address to which the tcp server should be bound.
	 */
	public HttpCommunicationManager(SubSystem parent, TcpEndPointIdentifier address)
	{
		super(parent, "HttpCommunicationManager", HttpEndPoint.class, address);
	}
	
	/**
	 * Creates a HttpCommunicationManager with a single tcp server, named "TcpServer", listening for connections. 
	 * <br><br>The created HttpCommunicationManager will be named "HttpCommunicationManager".
	 * 
	 * @param parent the parent of this HttpCommunicationManager.
	 * @param address the address to which the tcp server should be bound.
    * @param tcpEndPointClass the endpoint class.
	 */
	public HttpCommunicationManager(SubSystem parent, TcpEndPointIdentifier address, Class tcpEndPointClass)
	{
		super(parent, "HttpCommunicationManager", tcpEndPointClass, address);
	}
	
	/**
	 * Creates a HttpCommunicationManager with a single tcp server, named "TcpServer", listening for connections. 
	 * <br><br>The created HttpCommunicationManager will be named "HttpCommunicationManager".
	 * 
	 * @param parent the parent of this HttpCommunicationManager.
	 * @param name the name that will be given to this HttpCommunicationManager.
	 * @param address the address to which the tcp server should be bound.
	 */
	public HttpCommunicationManager(SubSystem parent, String name, TcpEndPointIdentifier address)
	{
		super(parent, name, HttpEndPoint.class, address);
	}
	
	/**
	 * Creates a HttpCommunicationManager with a single tcp server, named "TcpServer", listening for connections. 
	 * <br><br>The created HttpCommunicationManager will be named "HttpCommunicationManager".
	 * 
	 * @param parent the parent of this HttpCommunicationManager.
	 * @param name the name that will be given to this HttpCommunicationManager.
	 * @param address the address to which the tcp server should be bound.
    * @param tcpEndPointClass the endpoint class.
	 */
	public HttpCommunicationManager(SubSystem parent, String name, TcpEndPointIdentifier address, Class tcpEndPointClass)
	{
		super(parent, name, tcpEndPointClass, address);
	}
			
	/**
	 * Creates a HttpCommunicationManager with multiple tcp servers listening for connections. The number of tcp servers that will be created 
	 * depends on the sizes of the arrays specified by parameters <code>tcpServerNames</code> and <code>addresses</code>.
	 * 
	 * @param parent the parent of this HttpCommunicationManager.
	 * @param name the name that will be given to this HttpCommunicationManager.
	 * @param addresses array of addresses to which the created tcp servers should be bound.
	 */
	public HttpCommunicationManager(SubSystem parent, String name, TcpEndPointIdentifier[] addresses)
	{
		super(parent, name, HttpEndPoint.class, addresses);
	}
	
	/**
	 * Creates a HttpCommunicationManager with multiple tcp servers listening for connections. The number of tcp servers that will be created 
	 * depends on the sizes of the arrays specified by parameters <code>tcpServerNames</code> and <code>addresses</code>.
	 * 
	 * @param parent the parent of this HttpCommunicationManager.
	 * @param name the name that will be given to this HttpCommunicationManager.
	 * @param addresses array of addresses to which the created tcp servers should be bound.
    * @param tcpEndPointClass the endpoint class.
	 */
	public HttpCommunicationManager(SubSystem parent, String name, TcpEndPointIdentifier[] addresses, Class tcpEndPointClass)
	{
		super(parent, name, tcpEndPointClass, addresses);
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
		HttpResponse rsp;
		String msg;
					      
		msg = "<h1>500 Server Error</h1>";
		msg += "Bad configuration, no request handler found";
		rsp = new HttpResponse(500, "Server Error", msg);
		rsp.setContentType("text/html");
		
		return rsp;
	}
}
