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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;

import com.teletalk.jserver.JServer;

/**
 * A class providing basic http requests handling functionality. This class can fetch files from the 
 * current directory of the local filesystem and the directories, and make it possible to create a simple 
 * web server. This class also handles the following request paths:<br>
 * <br>
 * &middot; <b>/alive</b> - Sends an alive response using the method {@link #sendAliveResponse()}.<br>
 * &middot; <b>/version</b> - Sends a response containing the version of the server using the method {@link #sendVersionResponse()}.<br>
 * &middot; <b>/versionlong</b> - Sends a response containing extend server version information using the method {@link #sendVersionResponse(boolean)}.<br>
 * &middot; <b>/name</b> - Sends a response containing the name of the server using the method {@link #sendNameResponse()}.<br>
 * &middot; <b>/getdiskfreespace</b> - Sends a response containing the free disk space in bytes on the disk indicated by the request. 
 * The mehtod {@link #sendNameResponse()} will be used to handle this request. The request must contain the parameter <code>path</code>, 
 * for instance <code>http://myserver:myport/getdiskfreespace?path=C</code>.
 *  
 * @author Tobias Löfstrand
 * 
 * @since 2.0.1
 */
public class DefaultHttpConnection extends HttpConnection
{
	/**
	 * Interface used to for mapping a request path to a action.
	 * 
	 * @author Tobias Löfstrand
 	 * 
 	 * @since 1.13 Build 600
	 */
	protected interface RequestAction
	{
		/**
		 * Executes this action.
		 */
		public void execute(final HttpRequest req) throws IOException;
	}
	
	/** Request path constant for an alive request. */
	public static final String ALIVE_REQUEST_PATH = "/alive";
	/** Request path constant for a version request. */
	public static final String VERSION_REQUEST_PATH = "/version";
	/** Request path constant for an extended version request. */
	public static final String VERSIONLONG_REQUEST_PATH = "/versionlong";
	/** Request path constant for a name request. */
	public static final String NAME_REQUEST_PATH = "/name";
	/** Request path constant for disk free space request. */
	public static final String GETDISKFREESPACE_REQUEST_PATH = "/getdiskfreespace";

	/** A reference to a HttpServer. */
	protected final HttpServer server;
	
	/** 
	 * HashMap containing mappings between request paths and <code>RequestAction</code> 
	 * objects. The {@link #request(HttpRequest)} method of this class will attempt to get such an object 
	 * for current request path, and execute it if found. <br>
	 * Subclasses may add additional mappings in the following way:<br>
	 * <br>
	 * <code>super.requestPathToActionMap.put("/myrequest", new RequestAction()<br>
	 * {<br>
	 *	&nbsp;&nbsp;&nbsp;public void execute(HttpRequest req) throws IOException<br>
	 *	&nbsp;&nbsp;&nbsp;{<br>
	 *	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;myRequestHanderMethod(req);<br>
	 *	&nbsp;&nbsp;&nbsp;}<br>
	 * });<br></code>
	  */
	protected final HashMap requestPathToActionMap = new HashMap();

	/**
	 * Creates a new DefaultHttpConnection.
	 */
	public DefaultHttpConnection()
	{
		this(null, "DefaultHttpConnection");
	}
	
	/**
	 * Creates a new DefaultHttpConnection.
	 * 
	 * @param server reference to a HttpServer.
	 */
	public DefaultHttpConnection(HttpServer server)
	{
		this(server, "DefaultHttpConnection");
	}
	
	/**
	 * Creates a new DefaultHttpConnection.
	 * 
	 * @param name the name of the DefaultHttpConnection.
	 */
	public DefaultHttpConnection(String name)
	{
		super(name);
		this.server = null;
		
		this.initRequestPathToActionMappings();
	}
			
	/**
	 * Creates a new DefaultHttpConnection.
	 * 
	 * @param server reference to a HttpServer.
	 * @param name the name of the DefaultHttpConnection.
	 */
	public DefaultHttpConnection(HttpServer server, String name)
	{
		super(server.getFullName() + "." + name);
		this.server = server;
		
		this.initRequestPathToActionMappings();
	}
	
	/**
	 * Initializes request path to action mappings.
	 */
	private void initRequestPathToActionMappings()
	{
		requestPathToActionMap.put(ALIVE_REQUEST_PATH, new RequestAction()
		{
			public void execute(HttpRequest req) throws IOException
			{
            sendAliveResponse();
			}
		});
		requestPathToActionMap.put(VERSION_REQUEST_PATH, new RequestAction()
		{
			public void execute(HttpRequest req) throws IOException
			{
				sendVersionResponse();
			}
		});
		requestPathToActionMap.put(VERSIONLONG_REQUEST_PATH, new RequestAction()
		{
			public void execute(HttpRequest req) throws IOException
			{
            sendVersionResponse(true);
			}
		});
		requestPathToActionMap.put(NAME_REQUEST_PATH, new RequestAction()
		{
			public void execute(HttpRequest req) throws IOException
			{
            sendNameResponse();
			}
		});
		requestPathToActionMap.put(GETDISKFREESPACE_REQUEST_PATH, new RequestAction()
		{
			public void execute(HttpRequest req) throws IOException
			{
            sendDiskFreeSpaceResponse(req);
			}
		});
	}
   
   /**
    * Checks if connection activity logging is enabled (i.e. returns {@link #isDebugEnabled()}).
    * 
    * @since 2.1 (20050525)
    * 
    * @deprecated as of 2.1.5 (20070419), replaced by {@link #isDebugEnabled()}.
    */
   public boolean isConnectionActivityLoggingEnabled()
   {
      return super.isDebugEnabled();
   }
	
	/**	
	 * Request handler method.
	 *   
	 * @param req The request from the client
	 * 
	 * @exception IOException if there was an error opening or reading a file or reading/writing from/to the socket.
	 */
	public void request(final HttpRequest req) throws java.io.IOException
	{
		String reqPath = req.getPath();

		if( reqPath == null) reqPath = "";
		else reqPath = reqPath.trim();
		
		final RequestAction requestAction = (RequestAction)this.requestPathToActionMap.get(reqPath.toLowerCase());
		
		if( requestAction != null )
		{
			requestAction.execute(req);
		}
		else
		{
			String vDir = null;
			String redirectToDir = null; 
			File file;
			
			if(reqPath.equals("/") || reqPath.equals(""))
			{
				file = new File(server.getDocumentRoot() + "/" + "index.html");
				sendFileResponse(file);
			}
			else
			{
				if(reqPath.startsWith("/")) 
					reqPath = reqPath.substring(1);	
				
				if(reqPath.endsWith("/")) 
				{
					reqPath = reqPath.substring(0, reqPath.length()-1);
					vDir = server.convertPath(reqPath);
					
					if(vDir.equals(reqPath))
					{
						file = new File(server.getDocumentRoot() + "/" + reqPath + "/" + "index.html");
						sendFileResponse(file);
					}
					else
					{
						file = new File(vDir + "/" + "index.html");
						sendFileResponse(file);
					}
				}
				else
				{
					vDir = server.convertPath(reqPath);
					
					if(vDir.equals(reqPath)) //If vDir was unchanged, there was no virtual directories in HttpServer
					{
						file = new File(server.getDocumentRoot() + "/" + reqPath);
						
						if(file.exists())
						{
							if(file.isDirectory())
							{
								redirectToDir = "/" + reqPath + "/";
								sendRedirectResponse(redirectToDir);
							}
							else
								sendFileResponse(file);
						} //Attempt to get requested path as resource
						else
							sendResourceResponse(reqPath);
					}
					else
					{
						file = new File(vDir);

						if(file.exists())
						{
							if(file.isDirectory() || server.getVirtualDirectory(reqPath) != null)
							{
								redirectToDir = "/" + reqPath + "/";
								sendRedirectResponse(redirectToDir);
							}
							else
								sendFileResponse(file);
						} //Attempt to get requested path as resource
						else
							sendResourceResponse(reqPath);
					}
				}
			}
		}
	}
	
	/**
	 * Gets the content type for the specified file. This method uses the static FileNameMap member (@see #fileNameMap) to 
	 * get the conten type for a certain file. If no type could be found, "application/octet-stream" will be returned. <br>
	 * <br>
	 * Subclasses can override this method to provide a better implementation.
	 * 
	 * @param file the name of the file to get the contenttype for.
	 * 
	 * @return the content type.
	 */
	public String getContentType(String file)
	{
      return HttpResponseUtils.getContentType(file);
	}
	
	/**
	 * Sends a response to an "/alive" request. The response will be "200 OK" if all subsystems are ok (no subsystem has the status CRITICAL_ERROR), otherwise 
	 * a 500 response will be returned.
	 * 
	 * @exception IOException if there was an error writing to the socket.
	 */
	public final void sendAliveResponse() throws IOException
	{
		HttpResponse rsp = HttpResponseUtils.createAliveResponse(DefaultHttpConnection.this);
				
		sendResponse(rsp);
	}
	
	/**
	 * Sends a response to an "/version" request. The response contains information about the version of this server, as 
	 * returned by the {@link JServer#getServerVersion()}.
	 * 
	 * @exception IOException if there was an error writing to the socket.
	 */
	public final void sendVersionResponse() throws IOException
	{
		sendVersionResponse(false);
	}
		
	/**
	 * Sends a response to an "/version" request. The response contains information about the version of this server, as 
	 * returned by the {@link JServer#getServerVersion()}. If parameter <code>longVersion</code> is <code>true</code> the 
	 * version of the JServer and information about the current Java VM is also returned.
	 * 
	 * @param longVersion flag indicating if the version of the JServer and information about the current Java VM should also be returned.
	 * 
	 * @exception IOException if there was an error writing to the socket.
	 */
	public final void sendVersionResponse(boolean longVersion) throws IOException
	{
		HttpResponse rsp = HttpResponseUtils.createVersionResponse(longVersion, DefaultHttpConnection.this);
				
		sendResponse(rsp);
	}
	
	/**
	 * Sends a response to an "/name" request. The response contains information about the name of this server.
	 * 
	 * @exception IOException if there was an error writing to the socket.
	 */
	public final void sendNameResponse() throws IOException
	{
      HttpResponse rsp = HttpResponseUtils.createNameResponse(DefaultHttpConnection.this);
				
		sendResponse(rsp);
	}
	
	/**
	 * Sends a response containing information on free disk space for the path specified in 
	 * the request. This means that the request must contain the parameter <code>path</code>, 
	 * for instance: <br><br>
	 * <code>http://myserver:myport/getdiskfreespace?path=C</code>.<br><br>
	 * The path must be a valid path that identifies the disk that
	 * 
	 * @param req the current http request object.
	 */
	public final void sendDiskFreeSpaceResponse(final HttpRequest req) throws IOException
	{
      HttpResponse rsp = HttpResponseUtils.createDiskFreeSpaceResponse(req, DefaultHttpConnection.this);
      				
		sendResponse(rsp);
	}
	
	/**
	 * Sends a redirection response (302 - "See Other").
	 * 
	 * @param path the redirect path.
	 * 
	 * @exception IOException if there was an error writing to the socket.
	 */
	public final void sendRedirectResponse(String path) throws IOException
	{
      HttpResponse rsp = HttpResponseUtils.createRedirectResponse(path, DefaultHttpConnection.this);
      
		sendResponse(rsp);
	}
	
	/**
	 * Sends a 200 - "OK" response with the specified response body and a content type of "text/html".
	 * 
	 * @param body the body of the respose.
	 * 
	 * @exception IOException if there was an error writing to the socket.
	 */
	public final void sendOkResponse(String body) throws IOException
	{
      HttpResponse rsp = HttpResponseUtils.createOkResponse(body, DefaultHttpConnection.this);
      				
		sendResponse(rsp);
	}
	
	/**
	 * Sends a 200 - "OK" response with the specified response body and content type.
	 * 
	 * @param body the body of the respose.
	 * @param contentType the content type of the body ("text/html" for instance).
	 * 
	 * @exception IOException if there was an error writing to the socket.
	 */
	public final void sendOkResponse(String body, String contentType) throws IOException
	{
      HttpResponse rsp = HttpResponseUtils.createOkResponse(body, contentType, DefaultHttpConnection.this);
      				
		sendResponse(rsp);
	}
	
	/**
	 * Sends a response with the specified response code, response description, response body and a content type of "text/html".
	 * 
	 * @param responseCode the code of the response.
	 * @param responseDescription the description of the response.
	 * @param body the body of the respose.
	 * 
	 * @exception IOException if there was an error writing to the socket.
	 */
	public final void sendResponse(int responseCode, String responseDescription, String body) throws IOException
	{
      HttpResponse rsp = HttpResponseUtils.createResponse(responseCode, responseDescription, body, DefaultHttpConnection.this);
      				
		sendResponse(rsp);
	}
	
	/**
	 * Sends a response with the specified response code, response description, response body and content type.
	 * 
	 * @param responseCode the code of the response.
	 * @param responseDescription the description of the response.
	 * @param body the body of the respose.
	 * @param contentType the content type of the body ("text/html" for instance).
	 * 
	 * @exception IOException if there was an error writing to the socket.
	 */
	public final void sendResponse(int responseCode, String responseDescription, String body, String contentType) throws IOException
	{
      HttpResponse rsp = HttpResponseUtils.createResponse(responseCode, responseDescription, body, contentType, DefaultHttpConnection.this);
      				
		sendResponse(rsp);
	}
	
	/**
	 * Sends a 404 - "File not found" response, indicating that the file specified by parameter <code>fileName</code> could not be found.
	 * 
	 * @param fileName the name of the file that couldn't be found.
	 * 
	 * @exception IOException if there was an error writing to the socket.
	 */
	public final void sendFileNotFoundResponse(String fileName) throws IOException
	{
      HttpResponse rsp = HttpResponseUtils.createFileNotFoundResponse(fileName, DefaultHttpConnection.this);
      
		sendResponse(rsp);
	}
	
	/**
	 * Sends a 200 - "OK" response with the specified file in the response body. If parameter <code>file</code> is 
	 * null, doesn't exist or if it is a directory a 404 - "File not found" response will be sent (if parameter <code>sendFileNotFound</code> 
	 * is set to <code>true</code>).
	 * 
	 * @param file a file to be sent in a response.
	 * @param sendFileNotFound flag indicating if a 404 - "File not found" response should be sent if the specified file does not exist. 
	 * 
	 * @return <code>true</code> if the file existed, otherwise <code>false</code>.
	 * 
	 * @exception IOException if there was an error writing to the socket.
	 */
	public final boolean sendFileResponse(File file, boolean sendFileNotFound) throws IOException
	{
      HttpResponse rsp = HttpResponseUtils.createFileResponse(file, sendFileNotFound, DefaultHttpConnection.this);
      
      if( rsp != null ) sendResponse(rsp);
      
      return rsp != null;
	}
	
	/**
	 * Sends a 200 - "OK" response with the specified file in the response body. If parameter <code>file</code> is 
	 * null, doesn't exist or if it is a directory a 404 - "File not found" response will be sent.
	 * 
	 * @param file a file to be sent in a response.
	 * 
	 * @exception IOException if there was an error writing to the socket.
	 */
	public final void sendFileResponse(File file) throws IOException
	{
		sendFileResponse(file, true);
	}
	
	/**
	 * Sends a 200 - "OK" response with the specified resource in the response body. If parameter <code>resourceName</code> is 
	 * null, doesn't exist or if it is a directory a 404 - "File not found" response will be sent (if parameter <code>sendFileNotFound</code> 
	 * is set to <code>true</code>). This method uses Class.getResource(String) to serach for resources.
	 * 
	 * @param resourceName name of a resource to be sent in a response.
	 * @param sendFileNotFound flag indicating if a 404 - "File not found" response should be sent if the specified resource does not exist. 
	 * 
	 * @return <code>true</code> if the resource existed, otherwise <code>false</code>.
	 * 
	 * @exception IOException if there was an error writing to the socket.
	 */
	public final boolean sendResourceResponse(String resourceName, boolean sendFileNotFound) throws IOException
	{
      HttpResponse rsp = HttpResponseUtils.createResourceResponse(resourceName, sendFileNotFound, DefaultHttpConnection.this);
      
      if( rsp != null ) sendResponse(rsp);
      
      return rsp != null;
	}
	
	/**
	 * Sends a 200 - "OK" response with the specified resource in the response body. If parameter <code>resourceName</code> is 
	 * null, doesn't exist or if it is a directory a 404 - "File not found" response will be sent. This method uses Class.getResource(String) to serach 
	 * for resources.
	 * 
	 * @param resourceName name of a resource to be sent in a response.
	 * 
	 * @exception IOException if there was an error writing to the socket.
	 */
	public final void sendResourceResponse(String resourceName) throws IOException
	{
		sendResourceResponse(resourceName, true);
	}
	
	/**
	 * Sends a 200 - "OK" response with the specified url in the response body. 
	 * 
	 * @param url an URL to be sent in a response.
	 * 
	 * @exception IOException if there was an error writing to the socket.
	 * @exception NullPointerException if parameter url was null.
	 */
	public final void sendUrlResponse(URL url) throws IOException, NullPointerException
	{
      HttpResponse rsp = HttpResponseUtils.createUrlResponse(url, DefaultHttpConnection.this);
      
      sendResponse(rsp);
	}
}
