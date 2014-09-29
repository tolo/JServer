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

import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.property.NumberProperty;
import com.teletalk.jserver.tcp.TcpEndPointIdentifier;
import com.teletalk.jserver.tcp.http.HttpRequest;
import com.teletalk.jserver.tcp.http.HttpResponse;
import com.teletalk.jserver.tcp.http.proxy.HttpProxyClient;

/**
 * Http proxy client manager implementation for XML-RPC over HTTP communication.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.1 build 690
 */
public class XmlRpcProxyClient extends HttpProxyClient
{
   private static final ThreadLocal XmlRpcWorkerThreadLocal = new ThreadLocal();
   
   
   private final NumberProperty defaultErrorCode;
   
   
   private final DefaultHandlerMapping handlerMapping;
         
   /**
    * Creates a new XmlRpcProxyClient.
    * 
    * @param parent the parent subsystem of this XmlRpcProxyClient.
    */
   public XmlRpcProxyClient(SubSystem parent)
   {
      this(parent, "XmlRpcProxyClient");
   }
   
   /**
    * Creates a new XmlRpcProxyClient.
    * 
    * @param parent the parent subsystem of this XmlRpcProxyClient.
    * @param messagingPort the port number of the HTTP proxy.
    */
   public XmlRpcProxyClient(SubSystem parent, int messagingPort)
   {
      this(parent, "XmlRpcProxyClient", messagingPort);
   }
   
   /**
    * Creates a new XmlRpcProxyClient.
    * 
    * @param parent the parent subsystem of this XmlRpcProxyClient.
    * @param name the name of this XmlRpcProxyClient.
    */
   public XmlRpcProxyClient(SubSystem parent, String name)
   {
      this(parent, name, (TcpEndPointIdentifier[])null);
   }
   
   /**
    * Creates a new XmlRpcProxyClient that will attempt to connect to a proxy at the specified address.
    * 
    * @param parent the parent subsystem of this XmlRpcProxyClient.
    * @param name the name of this HttpProxyClient.
    * @param messagingPort the port number of the HTTP proxy.
    */
   public XmlRpcProxyClient(SubSystem parent, String name, int messagingPort)
   {
      this(parent, name, new TcpEndPointIdentifier[]{new TcpEndPointIdentifier(messagingPort)});
   }
   
   /**
    * Creates a new XmlRpcProxyClient that will attempt to connect to a proxy at the specified address.
    * 
    * @param parent the parent subsystem of this XmlRpcProxyClient.
    * @param name the name of this XmlRpcProxyClient.
    * @param proxyAddress an address to a HTTP proxy.
    */
   public XmlRpcProxyClient(SubSystem parent, String name, TcpEndPointIdentifier proxyAddress)
   {
      this(parent, name, new TcpEndPointIdentifier[]{proxyAddress});
   }
   
   /**
    * Creates a new XmlRpcProxyClient that will attempt to connect to proxies at the specified addresses.
    * 
    * @param parent the parent subsystem of this XmlRpcProxyClient.
    * @param name the name of this XmlRpcProxyClient.
    * @param proxyAddresses addresses to HTTP proxies.
    */
   public XmlRpcProxyClient(SubSystem parent, String name, TcpEndPointIdentifier[] proxyAddresses)
   {
      super(parent, name, proxyAddresses);
      
      this.defaultErrorCode = new NumberProperty(this, "defaultErrorCode", 0, NumberProperty.MODIFIABLE_NO_RESTART);
      this.defaultErrorCode.setDescription("The default error code for to be used when errors occur. Default value is 0.");
      super.addProperty(this.defaultErrorCode); 
      
      this.handlerMapping = new DefaultHandlerMapping();
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
    * Parse the request and execute the handler method, if one is
    * found. Returns the result as XML.  The calling Java code
    * doesn't need to know whether the call was successful or not
    * since this is all packed into the response. No context information
    * is passed.
    */
   public byte[] execute(final HttpRequest request)
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
      
      return execute(request, new DefaultXmlRpcContext(user, password, getHandlerMapping()));
   }

   /**
    * Parse the request and execute the handler method, if one is
    * found. If the invoked handler is AuthenticatedXmlRpcHandler,
    * use the credentials to authenticate the user. No context information
    * is passed.
    */
   public byte[] execute(final HttpRequest request, final String user, final String password)
   {
      return execute(request, new DefaultXmlRpcContext(user, password, getHandlerMapping()));
   }
    
   /**
    * Parse the request and execute the handler method, if one is
    * found. If the invoked handler is AuthenticatedXmlRpcHandler,
    * use the credentials to authenticate the user. Context information
    * is passed to the worker, and may be passed to the request handler.
    */
   public byte[] execute(final HttpRequest request, final XmlRpcContext context)
   {
      return this.getXmlRpcWorker().execute(request.getRequestInputStream(), context);
   }
   
   /**
    */
   private XmlRpcProcessor getXmlRpcWorker()
   {
      XmlRpcProcessor xmlRpcWorker = (XmlRpcProcessor)XmlRpcWorkerThreadLocal.get();
      if( xmlRpcWorker == null )
      {
         xmlRpcWorker = new XmlRpcProcessor(this.getHandlerMapping());
         XmlRpcWorkerThreadLocal.set(xmlRpcWorker); 
      }
      
      return xmlRpcWorker;
   }
   
   /**
    * Method for handling incomming HTTP requests received from the proxy.
    * 
    * @param request a HTTP request received from a client.
    * 
    * @return the HTTP response that should be returned to the client (though the proxy) that sent the request.
    */
   public HttpResponse handleRequest(final HttpRequest request)
   {
      HttpResponse response;
      byte[] responseData;
      
      try
      {
         responseData = this.execute(request);
      }
      catch(Throwable t)
      {
         XmlRpcProcessor customXmlRpcWorker = getXmlRpcWorker();
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
    * Called to handle an exception that was thrown when executing the request (in {@link #handleRequest(HttpRequest)}). 
    * This implementation will return an XmlRpcException with a code specified through the property <code>defaultErrorCode</code>. The message 
    * will be generated though a call to <code>exception.toString()</code>.<br>
    * <br>
    * Subclasses may override this method to provide a customized exception processsing implementation. 
    * 
    * @param exception the exception that was thrown.
    */
   public XmlRpcException processException(Throwable exception)
   {
      return new XmlRpcException(this.defaultErrorCode.intValue(), exception.toString());
   }
}
