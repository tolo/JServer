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
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import com.teletalk.jserver.tcp.TcpConnection;

/**
 * The class HttpConnection is an abstract class which primary function is to serve as a simple  
 * serverside HTTP connection handler. It can be used as with the class HttpServer (or TcpServer) to 
 * handle incomming HTTP requests.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0.1
 * 
 * @see HttpServer
 */
public abstract class HttpConnection extends TcpConnection
{
	/**   The stream from where to read client data. **/
	protected InputStream  readStream;
	   
	/**   The stream to which data for client should be written. **/
	protected OutputStream writeStream;
		
	private final StringBuffer baseName;
	private final int appendIndex;  //Only for optimization
	
	private String lastConnectionType = null;
	
	/**
	 * Creates a new HttpConnection.
	 */
	public HttpConnection()
	{
		super();
		
		baseName = new StringBuffer(getName());
		appendIndex = baseName.length();
	}
	
	/**
	 * Creates a new HttpConnection.
	 * 
	 * @param name the name of the HttpConnection.
	 */
	public HttpConnection(String name)
	{
		super(name);
		
		baseName = new StringBuffer(name);
		appendIndex = baseName.length();
	}
	
	/**	
	 * The core of the request handling process. The thread
	 * initialized by the HttpServer (or TcpServer) begins its execution here.
	 * <p>
	 * A new HttpRequest instance is created upon the input stream
	 * connected to the client and this request is then sent to the
	 * method <tt>request</tt> for handling.
	 *   
	 */
	public final void work()
	{
		HttpRequest req;
		this.lastConnectionType = null;
			      
		try 
		{
			final Socket socket = getSocket();

			readStream = socket.getInputStream();
			writeStream = socket.getOutputStream();
			
			if(super.isDebugMode())
			{
				baseName.delete(appendIndex, baseName.length());
				baseName.append("(");
				baseName.append(socket.getInetAddress().toString() + ":" +  getSocket().getPort());
				baseName.append(")");
			}
			
			if(super.isDebugMode()) logDebug("Connection from client at " + super.getRemoteAddress() +  " opened.");
			
			do
			{
				try
				{
					req = new HttpRequest(readStream);
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
				
				request(req);
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
			readStream.skip(readStream.available());
			}catch(IOException ex1){}
			
			try{
			this.sendResponse(rsp);
			} catch(IOException ex2){}
			
			logError("Got exception during request (" + getSocket().getInetAddress().toString() + ":" + getSocket().getPort() + ")", e);
		}
	}
	   
	/**	
	 * Default request handler method. This method should be
	 * overridden by the actual subclass that handles the requests
	 * appropriate to the application function.
	 * <p>
	 * If this method is not overridden, all requests to the server
	 * will result in a response of type <tt>500 Server Error</tt>.
	 *   
	 * @param req The request from the client
	 * 
	 * @exception IOException if there was an error reading/writing from/to the socket
	 */
	protected void request(HttpRequest req) throws java.io.IOException
	{
		HttpResponse rsp;
		String msg;
			      
		msg = "<h1>500 Server Error</h1>";
		msg += "Bad configuration, no request handler found";
		rsp = new HttpResponse(500, "Server Error", msg);
			      
		rsp.setContentType("text/html");
		rsp.send(this.writeStream);
			      
		this.logError("No requesthandler found. Request: " + req + ".");
	}
	
	/**
	 * Convenience method to send a http response. This method will also set the <code>Connection</code> header if 
	 * it was set in the request.
	 * 
	 * @exception IOException if there was an error writing to the socket
	 */
	public void sendResponse(final HttpResponse response) throws java.io.IOException
	{
		if( (this.lastConnectionType != null) && (this.lastConnectionType.trim().length() > 0) )
		{
			response.setHeader(HttpConstants.CONNECTION_HEADER_KEY, this.lastConnectionType);
		}
		
		response.send(this.writeStream);
		this.writeStream.flush();
	}
}
