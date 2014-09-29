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

import java.io.IOException;

import com.teletalk.jserver.tcp.TcpEndPoint;

/**
 * This class implements an endpoint for HTTP communication. HttpEndPoint is meant to be used 
 * in conjunction with an HttpCommunicationManager, which will perform the actual request handling logic.<br>
 * <br>
 * For now, this class only implements server side behaviour.
 * 
 * @since 1.2 Build 635
 * 
 * @author Tobias Löfstrand
 */
public class HttpEndPoint extends TcpEndPoint
{
	private String lastConnectionType = null;
	
   /** Reference to the HttpCommunicationManager. */
	protected final HttpCommunicationManager commManager;
	
	/**
	 * Creates  a new HttpEndPoint.
	 * 
	 * @param commManager reference to a parent HttpCommunicationManager.
	 */
	public HttpEndPoint(final HttpCommunicationManager commManager)
	{
		this(commManager, "HttpEndPoint");
	}
	
	/**
	 * Creates  a new HttpEndPoint.
	 * 
	 * @param commManager reference to a parent HttpCommunicationManager.
	 * @param name the name of this HttpEndPoint.
	 */
	public HttpEndPoint(final HttpCommunicationManager commManager, final String name)
	{
		super(commManager, name);
		this.commManager = commManager;
	}
	
	/**
	 * Server side HTTP request handling logic.
	 */
	protected final void runServerSideImpl()
	{
		HttpRequest req;
		this.lastConnectionType = null;
			      
		try 
		{
			// Initialize i/o streams
			super.initInputStream();
			super.initOutputStream();
			
			if(super.isDebugMode()) logDebug("Connection from client at " + super.getRemoteAddress() +  " opened.");
			
			do
			{
				try
				{
					req = new HttpRequest(super.inputStream);
				}
				catch(IOException se)
				{
					logError("Got exception while reading/waiting for request - " + se);
					break;
				}
				
				if( (req.isClosed()) && (lastConnectionType != null) ) // If connection closed and this is an using "keep-alive" connection.
				{
					if(super.isDebugMode()) logDebug("Connection closed by client.");
					break;
				}
				else if(!req.isValid())
				{
					logWarning("Exiting due to invalid request.");
					break;
				}
				
				if(super.isDebugMode()) logDebug("Handling request - " + req.getMethod() + " " + req.getPath() + ".");
				
				// Get the type marked in the last message
				this.lastConnectionType = req.getHeaderSingleValue(HttpConstants.CONNECTION_HEADER_KEY);
				
				this.handleRequest(req);
			}
			while( (this.lastConnectionType!= null) && this.lastConnectionType.trim().equalsIgnoreCase(HttpConstants.CONNECTION_KEEP_ALIVE_HEADER_VALUE) );
		}
		catch(Exception e) 
		{
			HttpResponse rsp;
			String msg;
				      
			msg = "<h1><CENTER>Server error</CENTER></h1><BR>";
			rsp = new HttpResponse(500, "Server error", msg);
	
			rsp.setContentType("text/html");
			
			try{
			super.inputStream.skip(super.inputStream.available());
			}catch(IOException ex1){}
			
			try{
			this.sendResponse(rsp);
			} catch(IOException ex2){}
			
			logError("Got exception during request (" + getSocket().getInetAddress().toString() + ":" + getSocket().getPort() + ")", e);
		}
	}
	
	/**
	 * Called when a HTTP request is received. This method only relays the request to the associated 
	 * {@link HttpCommunicationManager}, using the method {@link HttpCommunicationManager#handleRequest(HttpRequest, HttpEndPoint)}, and 
	 * sends the response back to the client.
	 * 
	 * @param request the received HTTP request.
	 * 
	 * @throws IOException if an i/o error occurred while handling the request.
	 */
	protected void handleRequest(final HttpRequest request) throws IOException
	{
		this.sendResponse( this.commManager.handleRequest(request, this) );
	}
	
	/**
	 * Sends a HTTP response to the client at the other end of this endpoint. This method will also set 
	 * the <code>Connection</code> header if it was set in the request.
	 * 
	 * @exception IOException if there was an error writing to the socket
	 */
	public void sendResponse(final HttpResponse response) throws java.io.IOException
	{
		if( (this.lastConnectionType != null) && (this.lastConnectionType.trim().length() > 0) )
		{
			response.setHeader(HttpConstants.CONNECTION_HEADER_KEY, this.lastConnectionType);
		}
		
		response.send(super.outputStream);
		super.outputStream.flush();
	}
}
