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
package com.teletalk.jserver.tcp.http.xmlrpc;

import org.apache.commons.codec.binary.Base64;
import org.apache.xmlrpc.DefaultHandlerMapping;
import org.apache.xmlrpc.DefaultXmlRpcContext;
import org.apache.xmlrpc.XmlRpcContext;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.XmlRpcHandlerMapping;
import org.apache.xmlrpc.XmlRpcRequestProcessor;
import org.apache.xmlrpc.XmlRpcResponseProcessor;
import org.apache.xmlrpc.XmlRpcWorker;

import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.property.NumberProperty;
import com.teletalk.jserver.tcp.TcpEndPointIdentifier;
import com.teletalk.jserver.tcp.http.HttpCommunicationManager;
import com.teletalk.jserver.tcp.http.HttpEndPoint;
import com.teletalk.jserver.tcp.http.HttpRequest;
import com.teletalk.jserver.tcp.http.HttpResponse;

/**
 * Communication manager implementation for XML-RPC over HTTP communication.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.1 build 690
 */
public class XmlRpcCommunicationManager extends HttpCommunicationManager
{
   private final NumberProperty defaultErrorCode;
   
   
   private final DefaultHandlerMapping handlerMapping;
	
	/**
	 * Creates a new XmlRpcCommunicationManager that will listen for connections on the specified localhost ports. 
	 * 
	 * @param parent the parent subsystem of this XmlRpcCommunicationManager.
	 * @param httpPort the localhost port on which this XmlRpcCommunicationManager will listen for HTTP connections.
	 */
	public XmlRpcCommunicationManager(SubSystem parent, int httpPort)
	{
		this(parent, "XmlRpcCommunicationManager", new TcpEndPointIdentifier(httpPort));
	}
	
	/**
	 * Creates a new XmlRpcCommunicationManager that will listen for connections on the specified localhost ports. 
	 * 
	 * @param parent the parent subsystem of this XmlRpcCommunicationManager.
	 * @param name the name of this XmlRpcCommunicationManager.
	 * @param httpPort the localhost port on which this XmlRpcCommunicationManager will listen for HTTP connections.
	 */
	public XmlRpcCommunicationManager(SubSystem parent, String name, int httpPort)
	{
		this(parent, name, new TcpEndPointIdentifier(httpPort));
	}
	
	/**
	 * Creates a new XmlRpcCommunicationManager that will listen for connections on the specified local addresses. 
	 * 
	 * @param parent the parent subsystem of this XmlRpcCommunicationManager.
	 * @param name the name of this XmlRpcCommunicationManager.
	 * @param httpaddress the local address on which this XmlRpcCommunicationManager will listen for HTTP connections.
	 */
	public XmlRpcCommunicationManager(SubSystem parent, String name, TcpEndPointIdentifier httpaddress)
	{
		super(parent, name, httpaddress, XmlRpcEndPoint.class);
		
      this.defaultErrorCode = new NumberProperty(this, "defaultErrorCode", 0, NumberProperty.MODIFIABLE_NO_RESTART);
      this.defaultErrorCode.setDescription("The default error code for to be used when errors occur. Default value is 0.");
      super.addProperty(this.defaultErrorCode);
      
		this.handlerMapping = new DefaultHandlerMapping();
	}
	
	/**
	 * Creates a new XmlRpcCommunicationManager that will listen for connections on the specified local addresses. 
	 * 
	 * @param parent the parent subsystem of this XmlRpcCommunicationManager.
	 * @param name the name of this XmlRpcCommunicationManager.
	 * @param httpaddresses the local addresses on which this XmlRpcCommunicationManager will listen for HTTP connections.
	 */
	public XmlRpcCommunicationManager(SubSystem parent, String name, TcpEndPointIdentifier[] httpaddresses)
	{
		super(parent, name, httpaddresses, XmlRpcEndPoint.class);
		
      this.defaultErrorCode = new NumberProperty(this, "defaultErrorCode", 0, NumberProperty.MODIFIABLE_NO_RESTART);
      this.defaultErrorCode.setDescription("The default error code for to be used when errors occur. Default value is 0.");
      super.addProperty(this.defaultErrorCode);
      
		this.handlerMapping = new DefaultHandlerMapping();
	}
	
	/**
	 * Initialization method for this XmlRpcCommunicationManager.
	 */
	protected void doInitialize()
	{
		super.doInitialize();
	}
	
	/**
	 * Shut down method fot this XmlRpcCommunicationManager.
	 */
	protected void doShutDown()
	{
		super.doShutDown();
	}
	
   /**
    * Register a handler object with this name. Methods of this
    * objects will be callable over XML-RPC as
    * "handlername.methodname". For more information about XML-RPC
    * handlers see the XML-RPC main documentation.
    *
    * @param handlerName The name to identify the handler by.
    * @param handler The handler itself.
    */
   public void addHandler(final String handlerName, final Object handler)
   {
      this.handlerMapping.addHandler(handlerName, handler);
   }
   
   /**
    * Find the handler and its method name for a given method.
    * Implements the <code>XmlRpcHandlerMapping</code> interface.
    *
    * @param methodName The name of the XML-RPC method to find a
    * handler for (this is <i>not</i> the Java method name).
    * @return A handler object and method name.
    * @see org.apache.xmlrpc.XmlRpcHandlerMapping#getHandler(String)
    */
   public Object getHandler(final String methodName) throws Exception
   {
      return this.handlerMapping.getHandler(methodName);
   }

   /**
    * Remove a handler object that was previously registered with
    * this server.
    *
    * @param handlerName The name identifying the handler to remove.
    */
	public void removeHandler(final String handlerName)
	{
      this.handlerMapping.removeHandler(handlerName);
	}

	/**
	 * Return the current XmlRpcHandlerMapping.
	 */
	public XmlRpcHandlerMapping getHandlerMapping()
	{
		 return this.handlerMapping;
	}

	/**
	 * Executes the method specified in the XML-RPC formatted body of the 
    * http request. This method will attempt to parse user and password from 
    * a basic authorization header in the request. 
    * 
    * @param request the current HTTP request object.
    * @param endPoint the end point on which the request was received.
	 */
	public byte[] execute(final HttpRequest request, final XmlRpcEndPoint endPoint)
	{
      String authorization = request.getHeaderSingleValue("Authorization");
      String user = null;
      String password = null;
      int basicIndex = -1;
      
      if( (authorization != null) && ((basicIndex = authorization.toLowerCase().indexOf("basic")) >= 0) )
      {
         try
         {
            authorization = authorization.substring(basicIndex + "basic".length() + 1); // One space
            byte[] authBytes = Base64.decodeBase64(authorization.getBytes("US-ASCII"));
            String authString = new String(authBytes);
            int speparatorIndex = authString.indexOf(':');
            
            user = authString.substring(0, speparatorIndex);
            password = authString.substring(speparatorIndex + 1);
         }
         catch(Exception e)
         {
            logError("Failed to parse authorization!", e);
         }
      }
      
      return execute(request, endPoint, new DefaultXmlRpcContext(user, password, getHandlerMapping()));
	}

   /**
    * Executes the method specified in the XML-RPC formatted body of the 
    * http request.
    * 
    * @param request the current HTTP request object.
    * @param endPoint the end point on which the request was received.
    */
	public byte[] execute(final HttpRequest request, final XmlRpcEndPoint endPoint, final String user, final String password)
	{
      return execute(request, endPoint, new DefaultXmlRpcContext(user, password, getHandlerMapping()));
	}
    
   /**
    * Executes the method specified in the XML-RPC formatted body of the 
    * http request.
    * 
    * @param request the current HTTP request object.
    * @param endPoint the end point on which the request was received.
    * @param context the context for the request.
    */
	public byte[] execute(final HttpRequest request, final XmlRpcEndPoint endPoint, final XmlRpcContext context)
	{
      XmlRpcWorker worker = endPoint.getXmlRpcProcessor();
      return worker.execute(request.getRequestInputStream(), context);
	}
	
	/**
	 * Method for handling incomming HTTP requests. This method only calls {@link #handleRequest(HttpRequest, XmlRpcEndPoint)}.
	 * 
	 * @param request a HTTP request received from a client.
	 * @param endPoint the endpoint the request was received on.
	 * 
	 * @return the HTTP response that should be returned to the client that sent the request.
	 */
	public HttpResponse handleRequest(final HttpRequest request, final HttpEndPoint endPoint)
	{
      return handleRequest(request, (XmlRpcEndPoint)endPoint);
	}
			
	/**
	 * Method for handling incomming XmlRpc HTTP requests. This method calls the {@link #execute(HttpRequest, XmlRpcEndPoint)} 
	 * to execute and incomming XmpRpc method call.
	 * 
	 * @param request a HTTP request received from a client.
	 * @param endPoint the endpoint the request was received on.
	 * 
	 * @return the HTTP response that should be returned to the client that sent the request.
	 */
	public HttpResponse handleRequest(final HttpRequest request, final XmlRpcEndPoint endPoint)
	{
      HttpResponse response;
      byte[] responseData;
      
      try
      {
         responseData = this.execute(request, endPoint);
      }
      catch(Throwable t)
      {
         XmlRpcProcessor customXmlRpcWorker = endPoint.getXmlRpcProcessor();
         XmlRpcRequestProcessor requestProcessor = customXmlRpcWorker.getXmlRpcRequestProcessor();
         XmlRpcResponseProcessor responseProcessor = customXmlRpcWorker.getXmlRpcResponseProcessor();
                  
         XmlRpcException e;
         
         if( t instanceof XmlRpcException )
         {
            e = (XmlRpcException)t;
         }
         else
         {
            e = this.processException(t);
         }
         
         responseData = responseProcessor.encodeException(e, requestProcessor.getEncoding());
      }
      
      response = new HttpResponse(200, "OK", responseData);
      response.setContentType ("text/xml");
      
      return response;
   }
   
   /**
    * Called to handle an exception that was thrown when executing the request (in {@link #handleRequest(HttpRequest, XmlRpcEndPoint)}). 
    * This implementation will return an XmlRpcException with a code specified through the property <code>defaultErrorCode</code>. The message 
    * will be generated though a call to <code>exception.toString()</code>.<br>
    * <br>
    * Subclasses may override this method to provide a customized exception processsing implementation. 
    * 
    * @param exception the exception that was thrown.
    */
   public XmlRpcException processException(final Throwable exception)
   {
      return new XmlRpcException(this.defaultErrorCode.intValue(), exception.toString());
   }
}
