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

import java.util.Enumeration;
import java.util.Hashtable;

import com.teletalk.jserver.StatusTransitionException;
import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.property.StringProperty;
import com.teletalk.jserver.tcp.TcpEndPointIdentifier;
import com.teletalk.jserver.tcp.TcpServer;

/**
 * The HttpServer is a direct subclass of TcpServer and is used for handling HTTP communication. It can be customized through the use of a requesthandler class which
 * implements the behavior for how incoming request should be interpreted. This requesthandler class must be a subclass of <tt>TcpConnection</tt>
 * which defines basic functionality for tcp requests. <br>
 * <br>
 * This class provider serveral methods which makes is easy to create a simple http server using the class DefaultHttpConnection to handle 
 * incoming requests. DefaultHttpConnection provides basic http requests handling functionality, such as fetching files from the local file system. 
 * <br><br>
 * <i>Note that the request class creation parameters (constructor parameters) must be set for the request handler class using the 
 * method {@link #setRequestClassCreationParams(Object[])}. If this is not done a default set of parameters, consisting of the name of a reference to this 
 * and the name of the request handler class, will be used.</i><br>
 * 
 * 
 * @see DefaultHttpConnection
 * @see com.teletalk.jserver.tcp.TcpServer
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta
 */
public class HttpServer extends TcpServer
{
	private StringProperty documentRoot;
	
	private final Hashtable virtualDirectories;
   
   /**
    * Constructs a new default HttpServer (using DefaultHttpConnection as request handler) named "HttpServer" and bound to the local host on port 80. 
    * The request handler creation params will be defaulted to a reference to this object and the name of the request handler class. 
    * 
    * @param parent the parent SubSystem.
    * 
    * @see com.teletalk.jserver.tcp.http.HttpConnection
    */
   public HttpServer(SubSystem parent) 
   {
      this(parent, "HttpServer");
   }
   
   /**
    * Constructs a new default HttpServer (using DefaultHttpConnection as request handler) and bound to the local host on port 80. 
    * The request handler creation params will be defaulted to a reference to this object and the name of the request handler class. 
    * 
    * @param parent the parent SubSystem.
    * @param name the name of the HttpServer.
    * 
    * @see com.teletalk.jserver.tcp.http.HttpConnection
    */
   public HttpServer(SubSystem parent, String name) 
   {
      this(parent, name, DefaultHttpConnection.class, 80);
   }

	/**
	 * Constructs a new HttpServer named "HttpServer" and bound to all/any local addresses (0.0.0.0). The request handler creation params 
	 * will be defaulted to a reference to this object and the name of the request handler class. 
	 * 
	 * @param parent the parent SubSystem.
	 * @param requestHandlerClass the requesthandler class; must be a subclass of HttpConnection.
	 * @param localPort the port on which this HttpServer should listen for requests.
	 * 
	 * @see com.teletalk.jserver.tcp.http.HttpConnection
	 */
	public HttpServer(SubSystem parent, Class requestHandlerClass, int localPort) 
	{
		this(parent, "HttpServer", requestHandlerClass, localPort, TcpEndPointIdentifier.anyLocalHostString);
	}
	
	/**
	 * Constructs a new HttpServer named "HttpServer" and bound to all/any local addresses (0.0.0.0). 
	 * 
	 * @param parent the parent SubSystem.
	 * @param requestHandlerClass the requesthandler class; must be a subclass of HttpConnection.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 * @param localPort the port on which this HttpServer should listen for requests.
	 * 
	 * @see com.teletalk.jserver.tcp.http.HttpConnection
	 */
	public HttpServer(SubSystem parent, Class requestHandlerClass, Object[] requestClassCreationParams, int localPort) 
	{
		this(parent, "HttpServer", requestHandlerClass, requestClassCreationParams, localPort, TcpEndPointIdentifier.anyLocalHostString);
	}
	
	/**
	 * Constructs a new HttpServer bound to all/any local addresses (0.0.0.0) with the default set of request handler 
	 * creation params (a reference to this object and the name of the request handler class).
	 * 
	 * @param parent the parent SubSystem.
	 * @param name the name of the HttpServer.
	 * @param requestHandlerClass the requesthandler class; must be a subclass of HttpConnection.
	 * @param localPort the port on which this HttpServer should listen for requests.
	 * 
	 * @see com.teletalk.jserver.tcp.http.HttpConnection
	 */
	public HttpServer(SubSystem parent, String name, Class requestHandlerClass, int localPort) 
	{
		this(parent, name, requestHandlerClass, localPort, TcpEndPointIdentifier.anyLocalHostString);
	}
	
	/**
	 * Constructs a new HttpServer bound to all/any local addresses (0.0.0.0).
	 * 
	 * @param parent the parent SubSystem.
	 * @param name the name of the HttpServer.
	 * @param requestHandlerClass the requesthandler class; must be a subclass of HttpConnection.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 * @param localPort the port on which this HttpServer should listen for requests.
	 * 
	 * @see com.teletalk.jserver.tcp.http.HttpConnection
	 */
	public HttpServer(SubSystem parent, String name, Class requestHandlerClass, Object[] requestClassCreationParams, int localPort) 
	{
		this(parent, name, requestHandlerClass, requestClassCreationParams, localPort, TcpEndPointIdentifier.anyLocalHostString);
	}
	
	/**
	 * Constructs a new HttpServer named "HttpServer" with the default set of request handler 
	 * creation params (a reference to this object and the name of the request handler class).
	 * 
	 * @param parent the parent SubSystem.
	 * @param requestHandlerClass the requesthandler class; must be a subclass of HttpConnection.
	 * @param localPort the port on which this HttpServer should listen for requests.
	 * @param localIP the local ip address this HttpServer should be bound to.
	 * 
	 * @see com.teletalk.jserver.tcp.http.HttpConnection
	 */
	public HttpServer(SubSystem parent, Class requestHandlerClass, int localPort, String localIP) 
	{
		this(parent, "HttpServer", requestHandlerClass, localPort, localIP);
	}
	
	/**
	 * Constructs a new HttpServer named "HttpServer".
	 * 
	 * @param parent the parent SubSystem.
	 * @param requestHandlerClass the requesthandler class; must be a subclass of HttpConnection.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 * @param localPort the port on which this HttpServer should listen for requests.
	 * @param localIP the local ip address this HttpServer should be bound to.
	 * 
	 * @see com.teletalk.jserver.tcp.http.HttpConnection
	 */
	public HttpServer(SubSystem parent, Class requestHandlerClass, Object[] requestClassCreationParams, int localPort, String localIP) 
	{
		this(parent, "HttpServer", requestHandlerClass, requestClassCreationParams, localPort, localIP);
	}
	
	/**
	 * Constructs a new HttpServer with the default set of request handler creation params (a reference to this object and the name of the request handler class).
	 * 
	 * @param parent the parent SubSystem.
	 * @param name the name of the HttpServer.
	 * @param requestHandlerClass the requesthandler class; must be a subclass of HttpConnection.
	 * @param localPort the port on which this HttpServer should listen for requests.
	 * @param localIP the local ip address this HttpServer should be bound to.
	 * 
	 * @see com.teletalk.jserver.tcp.http.HttpConnection
	 */
	public HttpServer(SubSystem parent, String name, Class requestHandlerClass, int localPort, String localIP) 
	{
		this(parent, name, requestHandlerClass, null, localPort, localIP);
		
		String reqClassName = requestHandlerClass.getName();
		
		setRequestClassCreationParams(new Object[]{this,  
			 (reqClassName.lastIndexOf(".") > 0) ? reqClassName.substring(reqClassName.lastIndexOf(".") + 1) :  reqClassName});
	}
	
	/**
	 * Constructs a new HttpServer.
	 * 
	 * @param parent the parent SubSystem.
	 * @param name the name of the HttpServer.
	 * @param requestHandlerClass the requesthandler class; must be a subclass of HttpConnection.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 * @param localPort the port on which this HttpServer should listen for requests.
	 * @param localIP the local ip address this HttpServer should be bound to.
	 * 
	 * @see com.teletalk.jserver.tcp.http.HttpConnection
	 */
	public HttpServer(SubSystem parent, String name, Class requestHandlerClass, Object[] requestClassCreationParams, int localPort, String localIP) 
	{
		super(parent, name, requestHandlerClass, requestClassCreationParams, localPort, localIP);
				
		virtualDirectories = new Hashtable();
		
		documentRoot = new StringProperty(this, "documentRoot", ".", StringProperty.MODIFIABLE_NO_RESTART);
		documentRoot.setDescription("The root directory used by this HttpServer when translating a http path to a path on the local filesystem. The default value is the current directory.");
		
		addProperty(documentRoot);
	}

	/**
	 * Constructs a HttpServer named "HttpServer" and bound to all/any local addresses (0.0.0.0), using DefaultHttpConnection as request handler.
	 * 
	 * @param parent the parent SubSystem.
	 * @param localPort the port on which this HttpServer should listen for requests.
	 * 
	 * @see com.teletalk.jserver.tcp.http.DefaultHttpConnection
	 */ 
	public static HttpServer createDefaultHttpServer(SubSystem parent, int localPort)
	{
		return createDefaultHttpServer(parent, localPort, TcpEndPointIdentifier.anyLocalHostString);
	}
	
	/**
	 * Constructs a HttpServer bound to all/any local addresses (0.0.0.0), using DefaultHttpConnection as request handler.
	 * 
	 * @param parent the parent SubSystem.
	 * @param name the name of the HttpServer.
	 * @param localPort the port on which this HttpServer should listen for requests.
	 * 
	 * @see com.teletalk.jserver.tcp.http.DefaultHttpConnection
	 */ 
	public static HttpServer createDefaultHttpServer(SubSystem parent, String name, int localPort)
	{
		return createDefaultHttpServer(parent, name, localPort, TcpEndPointIdentifier.anyLocalHostString);
	}
	
	/**
	 * Constructs a HttpServer named "HttpServer", using DefaultHttpConnection as request handler.
	 * 
	 * @param parent the parent SubSystem.
	 * @param localPort the port on which this HttpServer should listen for requests.
	 * @param localIP the local ip address this HttpServer should be bound to.
	 * 
	 * @see com.teletalk.jserver.tcp.http.DefaultHttpConnection
	 */
	public static HttpServer createDefaultHttpServer(SubSystem parent, int localPort, String localIP)
	{
		return createDefaultHttpServer(parent, "HttpServer", localPort, localIP);
	}
	
	/**
	 * Constructs a HttpServer using DefaultHttpConnection as request handler.
	 * 
	 * @param name the name of the HttpServer.
	 * @param parent the parent SubSystem.
	 * @param localPort the port on which this HttpServer should listen for requests.
	 * @param localIP the local ip address this HttpServer should be bound to.
	 * 
	 * @see com.teletalk.jserver.tcp.http.DefaultHttpConnection
	 */
	public static HttpServer createDefaultHttpServer(SubSystem parent, String name, int localPort, String localIP)
	{
		return new HttpServer(parent, name, DefaultHttpConnection.class, localPort, localIP);
	}

	/**
	 * Engages this HttpServer.
	 */
	protected void doInitialize()
	{
		super.doInitialize();
      
      // Attempt to get old property "document root"
      super.initFromConfiguredProperty(this.documentRoot, "document root", false, true);
      
		try
		{
			if(!((Class)HttpConnection.class).isAssignableFrom(requestHandlerClass))
				throw new InstantiationException("RequestHandlerClass must inherit class HttpConnection!");
		}
		catch(Exception e)
		{
			throw new StatusTransitionException("Error while engaging", e);
		}
	}
	
	/**
	 * Sets the root path used when fetching documents.
	 * 
	 * @param documentRoot root path for documents.
	 */
	public final void setDocumentRoot(String documentRoot)
	{
		this.documentRoot.setValue(documentRoot);
	}
	
	/**
	 * Gets the root path used when fetching documents.
	 * 
	 * @return the root path for documents.
	 */
	public final String getDocumentRoot()
	{
		return documentRoot.stringValue();
	}
	
	/**
	 * Adds a virtual directory which is associated with an actual directory.
	 * 
	 * @param dirName the name of the virtual directory, e.g. "doc".
	 * @param path absolute path to the actual directory, e.g. "d:\kahuna\docs".
	 */
	public void addVirtualDirectory(String dirName, String path)
	{
		if(dirName.startsWith("/") || dirName.startsWith("\\")) dirName = dirName.substring(1);
		if(dirName.endsWith("/") || dirName.endsWith("\\")) dirName = dirName.substring(0, dirName.length()-1);
		if(path.endsWith("/") || path.endsWith("\\")) path = path.substring(0, dirName.length()-1);
		
		virtualDirectories.put(dirName, path);
	}
	
	/**
	 * Gets the actual directory associated with an virtual directory.
	 * 
	 * @param dirName the name of the virtual directory, e.g. "doc".
	 * 
	 * @return the actual directory
	 */
	public String getVirtualDirectory(String dirName)
	{
		return (String)virtualDirectories.get(dirName);
	}
	
	/**
	 * Checkes if a given path contains a virtual directory.
	 * 
	 * @param path a path to be searched for a virtual directory.
	 * 
	 * @return true if a virtual directory was found in the given path, otherwise false.
	 */
	public boolean containsVirtualDirectory(String path)
	{
		Enumeration e;
		String vDir;
		boolean result = false;
		
		for(e = virtualDirectories.keys(); e.hasMoreElements();)
		{
			vDir = (String)e.nextElement();
			
			if(path.startsWith(vDir))
			{
				result = true;
				break;
			}
		}
		
		return result;
	}
	
	/**
	 * Converts a path by exchanging a virtual directory name contained in it for the name of the actual directory.
	 * 
	 * @param path a path to be converted.
	 * 
	 * @return the converted path. If no virtual directory was found in the path the unconverted path will be returned.
	 */
	public String convertPath(String path)
	{
		Enumeration e;
		String vDir;
		String result = path;
		
		for(e = virtualDirectories.keys(); e.hasMoreElements();)
		{
			vDir = (String)e.nextElement();
			
			if(path.startsWith(vDir))
			{
				result = ((String)virtualDirectories.get(vDir)) + path.substring(vDir.length());
				break;
			}
		}
		
		return result;
	}
}
