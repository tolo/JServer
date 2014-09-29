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
package com.teletalk.jserver.tcp;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.property.StringProperty;
import com.teletalk.jserver.tcp.http.HttpConstants;
import com.teletalk.jserver.tcp.http.HttpRequest;
import com.teletalk.jserver.tcp.http.HttpResponse;

/**
 * Socket factory implementation for sockets capable of tunneling through an HTTP proxy.
 *  
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20050616)
 */
public class HttpProxySocketFactory extends SubComponent implements SocketFactory
{
   private final DefaultSocketFactory defaultSocketFactory;
   
   private StringProperty proxyHost;
   
   private StringProperty proxyPort;
   
   private StringProperty proxyAuthorization;
  
   /**
    * Creates a new HttpProxySocketFactory named "HttpProxySocketFactory".
    */
   public HttpProxySocketFactory()
   {
      this("HttpProxySocketFactory");
   }
   
   /**
    * Creates a new HttpProxySocketFactory.
    * 
    * @param name the name of this HttpProxySocketFactory
    */
   public HttpProxySocketFactory(String name)
   {
      this(null, name);
   }
   
   /**
    * Creates a new HttpProxySocketFactory.
    *  
    * @param parent the parent HttpProxySocketFactory
    * @param name the name of this HttpProxySocketFactory
    */
   public HttpProxySocketFactory(SubComponent parent, String name)
   {
      super(parent, name);
      
      this.defaultSocketFactory = new DefaultSocketFactory();
      
      this.proxyHost = new StringProperty(this, "proxyHost", "", StringProperty.MODIFIABLE_NO_RESTART);
      super.addProperty(this.proxyHost);
      
      this.proxyPort = new StringProperty(this, "proxyPort", "", StringProperty.MODIFIABLE_NO_RESTART);
      super.addProperty(this.proxyPort);
      
      this.proxyAuthorization = new StringProperty(this, "proxyAuthorization", "", StringProperty.MODIFIABLE_NO_RESTART);
      super.addProperty(this.proxyAuthorization);
   }
   
   /**
    * Initializes a tunneled socket via a proxy.
    */
   public static void initProxySocket(Socket proxySocket, String proxyAuthorization, InetAddress host, int port) throws IOException
   {
      // Send proxy connect request
      HttpRequest request = new HttpRequest("CONNECT", host.getHostAddress() + ":" + port, "HTTP/1.1");
      request.setHeader("Host", host.getHostAddress() + ":" + port);
      request.setHeader(HttpConstants.USER_AGENT_HEADER_KEY, JServer.getVersionString());
      if( (proxyAuthorization != null) && (proxyAuthorization.trim().length() > 0) )
      {
         request.setHeader("Proxy-Authorization", proxyAuthorization);
      }
      
      PrintWriter output = new PrintWriter(proxySocket.getOutputStream());
      output.print(request.getRequestMessage());
      output.flush();
      output = null;
      
      // Read proxy response
      HttpResponse httpResponse = new HttpResponse(proxySocket.getInputStream(), false);
            
      if( (httpResponse.getCode() < 200) || (httpResponse.getCode() >= 300) )
      {
         throw new IOException("Error creating tunneled socket via proxy at " + 
            proxySocket.getInetAddress().getHostAddress() + ":" + proxySocket.getPort() + " - response code: " + httpResponse.getCode() + ". Response: " + httpResponse + ".");
      }
      
      httpResponse = null;
   }
   
   /**
    */
   private String getProxyPortInternal()
   {
      String proxyPort = this.proxyPort.stringValue();
      if( proxyPort == null ) proxyPort = "8080";
      return proxyPort;
   }
   
   /**
    */
   private boolean useProxy()
   {
      if( (this.proxyHost.stringValue() != null) && (this.proxyHost.stringValue().trim().length() > 0) ) return true;
      else return false;
   }
      
   
   /* ### SOCKETFACTORY METHODS ### */
      
   
   /**
    * Creates an unconnected socket.
    * 
    * @throws IOException if a connection cannot be established.
    */
   public Socket createSocket() throws IOException
   {
      return defaultSocketFactory.createSocket();
   }
   
   /**
    * Creates a socket and connects it to the specified host and port. If the proxyHost property of this HttpProxySocketFactory 
    * has been set, an attempt will be made to connect the socket through an HTTP proxy. 
    * 
    * @param host the server host.
    * @param port the server port.
    * 
    * @throws IOException if a connection cannot be established.
    */
   public Socket createSocket(InetAddress host, int port) throws IOException
   {
      if( this.useProxy() )
      {
         if( super.isDebugMode() ) logDebug("Creating tunneled socket via proxy at " + proxyHost.stringValue() + ":" + this.getProxyPortInternal() + ".");
         
         Socket socket = this.defaultSocketFactory.createSocket(InetAddress.getByName(this.proxyHost.stringValue()), Integer.parseInt(this.getProxyPortInternal()));
         HttpProxySocketFactory.initProxySocket(socket, this.proxyAuthorization.stringValue(), host, port);
         return socket;
      }
      else
      {
         if( super.isDebugMode() ) logDebug("Creating socket for " + host + ":" + port + ".");
         
         return this.defaultSocketFactory.createSocket(host, port);
      }
   }
   
   /**
    * Creates a socket and connects it to the specified host and port, blocking until a connection has been established or an error occurs. 
    * The created socket will be bound to the specified local address and port. If the proxyHost property of this HttpProxySocketFactory 
    * has been set, an attempt will be made to connect the socket through an HTTP proxy. 
    * 
    * @param host the server host.
    * @param port the server port.
    * @param localAddress the local address to bind to.
    * @param localPort the local port to bind to.
    * 
    * @throws IOException if a connection cannot be established.
    */
   public Socket createSocket(InetAddress host, int port, InetAddress localAddress, int localPort) throws IOException
   {
      if( this.useProxy() )
      {
         if( super.isDebugMode() ) logDebug("Creating tunneled socket via proxy at " + proxyHost.stringValue() + ":" + this.getProxyPortInternal() + ", local address: " + localAddress + ", local port: " + localPort + ".");
         
         Socket socket = this.defaultSocketFactory.createSocket(InetAddress.getByName(this.proxyHost.stringValue()), Integer.parseInt(this.getProxyPortInternal()));
         HttpProxySocketFactory.initProxySocket(socket, this.proxyAuthorization.stringValue(), host, port);
         return socket;
      }
      else
      {
         if( super.isDebugMode() ) logDebug("Creating socket for " + host + ":" + port + ", local address: " + localAddress + ", local port: " + localPort + ".");
         
         return this.defaultSocketFactory.createSocket(host, port);
      }
   }
      
   /* ### METHODS USING DEFAULTSOCKETFACTORY FOR CONNECT WITH TIMEOUT BEGIN ### */
   
   /**
    * Creates a socket and connects it to the specified host and port, using the specified timeout value. 
    * A timeout of zero is interpreted as an infinite timeout. The connection will then block until established or an error occurs. 
    * If the proxyHost property of this HttpProxySocketFactory has been set, an attempt will be made to connect the socket through an HTTP proxy. 
    * 
    * @param host the server host.
    * @param port the server port.
    * @param connectTimeOut the timeout value to be used in milliseconds. 
    * 
    * @throws IOException if a connection cannot be established.
    */
   public Socket createSocket(final InetAddress host, final int port, final int connectTimeOut) throws IOException
   {
      // Use DefaultSocketFactory to create a socket with a timeout, using this socket factory.
      // This will result in a call to createSocket(InetAddress, int)
      return DefaultSocketFactory.createSocket(host, port, connectTimeOut, this);
   }
   
   /**
    * Creates a socket and connects it to the specified host and port, using the specified timeout value. 
    * The created socket will be bound to the specified local address and port.
    * A timeout of zero is interpreted as an infinite timeout. The connection will then block until established or an error occurs. 
    * If the proxyHost property of this HttpProxySocketFactory has been set, an attempt will be made to connect the socket through an HTTP proxy.
    * 
    * @param host the server host.
    * @param port the server port.
    * @param localAddress the local address to bind to.
    * @param localPort the local port to bind to.
    * @param connectTimeOut the timeout value to be used in milliseconds. 
    * 
    * @throws IOException if a connection cannot be established.
    */
   public Socket createSocket(final InetAddress host, final int port, final InetAddress localAddress, final int localPort, final int connectTimeOut) throws IOException
   {
      // Use DefaultSocketFactory to create a socket with a timeout, using this socket factory
      // This will result in a call to createSocket(InetAddress, int, InetAddress, int)
      return DefaultSocketFactory.createSocket(host, port, localAddress, localPort, connectTimeOut, this);
   }   
   
   /* ### METHODS USING DEFAULTSOCKETFACTORY FOR CONNECT WITH TIMEOUT END ### */
}
