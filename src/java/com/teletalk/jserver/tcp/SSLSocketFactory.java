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
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;

import com.teletalk.jserver.StatusTransitionException;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.property.BooleanProperty;
import com.teletalk.jserver.property.StringProperty;

/**
 * Socket factory implementation for creating SSL/TLS enabled sockets. This factory extends 
 * the sub component class {@link SSLComponent}, which provides a number of properties used for the setup 
 * of secure communication.
 * 
 * @see SocketFactory
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.1 Build 690
 */
public class SSLSocketFactory extends SSLComponent implements SocketFactory, HttpURLConnectionFactory
{
   private HostnameVerifier hostnameVerifier;
   
   
   private final BooleanProperty useNaiveHostnameVerifier;
   
   private final StringProperty hostnameVerifierClass;
   
   private final StringProperty wrappedSocketFactoryClass;
      
   
   private SocketFactory wrappedSocketFactory;
    
   
   /**
    * Creates a new SSLSocketFactory named "SSLSocketFactory".
    */
   public SSLSocketFactory()
   {
      this("SSLSocketFactory");
   }
   
   /**
    * Creates a new SSLSocketFactory.
    * 
    * @param name the name of this SSLSocketFactory
    */
   public SSLSocketFactory(String name)
   {
      this(null, name);
   }
   
   /**
    * Creates a new SSLSocketFactory.
    *  
    * @param parent the parent SSLSocketFactory
    * @param name the name of this SSLSocketFactory
    */
   public SSLSocketFactory(SubComponent parent, String name)
   {
      super(parent, name);
      
      this.useNaiveHostnameVerifier = new BooleanProperty(this, "useNaiveHostnameVerifier", false, BooleanProperty.MODIFIABLE_OWNER_RESTART);
      this.useNaiveHostnameVerifier.setDescription("Boolean value indicating if a naive host name verifier is to be used, i.e. a host name verifier that ignores host name mismatches.");
      super.addProperty(this.useNaiveHostnameVerifier);
      
      this.hostnameVerifierClass = new StringProperty(this, "hostnameVerifierClass", "", StringProperty.MODIFIABLE_OWNER_RESTART);
      this.hostnameVerifierClass.setDescription("The fully qualified name of a class to be used as a host name verifier.");
      super.addProperty(this.hostnameVerifierClass);
      
      this.wrappedSocketFactoryClass = new StringProperty(this, "wrappedSocketFactoryClass", "", StringProperty.MODIFIABLE_OWNER_RESTART);
      //this.wrappedSocketFactoryClass.setDescription("");
      super.addProperty(this.wrappedSocketFactoryClass);
   }
   
   /**
    * Enables this SSLSocketFactory.
    */
   protected void doInitialize()
   {
      super.doInitialize();
      
      String hostnameVerifierClassStr = this.hostnameVerifierClass.stringValue();
      if( hostnameVerifierClassStr != null ) hostnameVerifierClassStr = hostnameVerifierClassStr.trim();
      
      if( this.useNaiveHostnameVerifier.booleanValue() )
      {
         this.hostnameVerifier = new NaiveHostnameVerifier();
      }
      else if( (hostnameVerifierClassStr != null) && (hostnameVerifierClassStr.length() > 0) ) 
      {
         try
         {
            this.hostnameVerifier = (HostnameVerifier)Class.forName(hostnameVerifierClassStr).newInstance();
         }
         catch(Exception e)
         {
            throw new StatusTransitionException("Unable to instantiate HostnameVerifier (" + hostnameVerifierClassStr + ")!", e);
         }
      }
      
      String wrappedSocketFactoryClassStr = this.wrappedSocketFactoryClass.stringValue();
      if( wrappedSocketFactoryClassStr != null ) wrappedSocketFactoryClassStr = wrappedSocketFactoryClassStr.trim();
      
      if( (wrappedSocketFactoryClassStr != null) && (wrappedSocketFactoryClassStr.length() > 0) ) 
      {
         try
         {
            // Set socket factory and add as subcomponent 
            this.setWrappedSocketFactory((SocketFactory)Class.forName(wrappedSocketFactoryClassStr).newInstance(), true);
         }
         catch(Exception e)
         {
            throw new StatusTransitionException("Unable to instantiate wrapped SocketFactory (" + wrappedSocketFactoryClassStr + ")!", e);
         }
      }
   }
   
   /**
    * Gets the wrapped socket factory.
    * 
    * @since 2.1 (20050615)
    */
   public SocketFactory getWrappedSocketFactory()
   {
      return wrappedSocketFactory;
   }
   
   /**
    * Sets the wrapped socket factory.
    * 
    * @since 2.1 (20050615)
    */
   public void setWrappedSocketFactory(SocketFactory wrappedSocketFactory)
   {
      this.setWrappedSocketFactory(wrappedSocketFactory, false);
   }
   
   /**
    * Sets the wrapped socket factory.
    * 
    * @since 2.1 (20050615)
    */
   private void setWrappedSocketFactory(SocketFactory wrappedSocketFactory, boolean reqister)
   {
      if( reqister )
      {
         SocketFactory oldWrappedSocketFactory = this.wrappedSocketFactory;
         
         if( this.wrappedSocketFactory != wrappedSocketFactory)
         {
            if( (oldWrappedSocketFactory != null) && (oldWrappedSocketFactory instanceof SubComponent) )
            {
               super.removeSubComponent((SubComponent)oldWrappedSocketFactory);
            }
         }
                     
         if( (wrappedSocketFactory instanceof SubComponent) && (!this.hasSubComponent((SubComponent)wrappedSocketFactory)) )
         {
            super.addSubComponent((SubComponent)wrappedSocketFactory, true);
         }
      }
      
      this.wrappedSocketFactory = wrappedSocketFactory;
   }
   
   /**
    * Gets the value of the property indicating if a {@link NaiveHostnameVerifier} object is to be used 
    * as HostnameVerifier for this object.
    */
   public boolean getUseNaiveHostnameVerifier()
   {
      return useNaiveHostnameVerifier.booleanValue();
   }

   /**
    * Sets the value of the property indicating if a {@link NaiveHostnameVerifier} object is to be used 
    * as HostnameVerifier for this object.
    */
   public void setUseNaiveHostnameVerifier(boolean useNaiveHostnameVerifier)
   {
      this.useNaiveHostnameVerifier.setValue(useNaiveHostnameVerifier);
   }
   
   public Socket createSocket() throws IOException
	{
      if( super.isEnabled() && (super.getSSLContext() != null) )
      {
         if( super.isDebugMode() ) logDebug("Creating unconnected socket.");
         
         return super.getSSLContext().getSocketFactory().createSocket();
      }
      else if( super.isEnabled() && (super.getSSLContext() == null) )
      {
         if( super.isDebugMode() ) logDebug("Creating unconnected socket. Using default javax.net.ssl.SSLSocketFactory.");
         
         return javax.net.ssl.SSLSocketFactory.getDefault().createSocket();
      }
      else
      {
         logError("Unable to create unconnected socket - socket factory not initialized!");
         
         throw new IOException("SSLSocketFactory not initialized!"); 
      }
	}
   
   /**
    * Creates a socket and connects it to the specified host and port. If a wrappedSocketFactory is set, it will be used 
    * to create the a standard socket, which will be passed to the method SSLSocketFactory.createSocket(Socket, String, int, boolean) 
    * to create a SSL socket. 
    * 
    * @param host the server host.
    * @param port the server port.
    * 
    * @throws IOException if a connection cannot be established.
    */
	public Socket createSocket(InetAddress host, int port) throws IOException
	{
      Socket wrappedSocket = null;
      if( this.wrappedSocketFactory != null ) wrappedSocket = this.wrappedSocketFactory.createSocket(host, port);
      String hostString = null;
      if( host != null ) hostString = host.getHostAddress();
      
      if( super.isEnabled() && (super.getSSLContext() != null) )
      {
         if( super.isDebugMode() ) logDebug("Creating socket for address: " + host + ", port: " + port + ".");
         
         if( wrappedSocket != null ) return super.getSSLContext().getSocketFactory().createSocket(wrappedSocket, hostString, port, true);
         else return super.getSSLContext().getSocketFactory().createSocket(host, port);
      }
      else if( super.isEnabled() && (super.getSSLContext() == null) )
      {
         if( super.isDebugMode() ) logDebug("Creating socket for address: " + host + ", port: " + port + ". Using default javax.net.ssl.SSLSocketFactory.");
         
         if( wrappedSocket != null ) return ((javax.net.ssl.SSLSocketFactory)javax.net.ssl.SSLSocketFactory.getDefault()).createSocket(wrappedSocket, hostString, port, true);
         else return javax.net.ssl.SSLSocketFactory.getDefault().createSocket(host, port);
      }
      else 
      {
         logError("Unable to create socket for address: " + host + ", port: " + port + " - socket factory not initialized!");
         
         throw new IOException("SSLSocketFactory not initialized!");
      }
	}
   
   /**
    * Creates a socket and connects it to the specified host and port, blocking until a connection has been established or an error occurs. 
    * The created socket will be bound to the specified local address and port. If a wrappedSocketFactory is set, it will be used 
    * to create the a standard socket, which will be passed to the method SSLSocketFactory.createSocket(Socket, String, int, boolean) 
    * to create a SSL socket. 
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
      Socket wrappedSocket = null;
      if( this.wrappedSocketFactory != null ) wrappedSocket = this.wrappedSocketFactory.createSocket(host, port, localAddress, localPort);
      String hostString = null;
      if( host != null ) hostString = host.getHostAddress();
      
      if( super.isEnabled() && (super.getSSLContext() != null) )
      {
         if( super.isDebugMode() ) logDebug("Creating socket for address: " + host + ", port: " + port + ", local address: " + localAddress + ", local port: " + localPort + ".");
         
         if( wrappedSocket != null ) return super.getSSLContext().getSocketFactory().createSocket(wrappedSocket, hostString, port, true);
         else return super.getSSLContext().getSocketFactory().createSocket(host, port, localAddress, localPort);
      }
      else if( super.isEnabled() && (super.getSSLContext() == null) )
      {
         if( super.isDebugMode() ) logDebug("Creating socket for address: " + host + ", port: " + port + ", local address: " + localAddress + ", local port: " + localPort + ". Using default javax.net.ssl.SSLSocketFactory.");
         
         if( wrappedSocket != null ) return ((javax.net.ssl.SSLSocketFactory)javax.net.ssl.SSLSocketFactory.getDefault()).createSocket(wrappedSocket, hostString, port, true);
         else return javax.net.ssl.SSLSocketFactory.getDefault().createSocket(host, port, localAddress, localPort);
      }
      else 
      {
         logError("Unable to create socket for address: " + host + ", port: " + port + ", local address: " + localAddress + ", local port: " + localPort +  " - socket factory not initialized!");
         
         throw new IOException("SSLSocketFactory not initialized!");
      }
   }
   
   /**
    * Returns a socket layered over an existing socket connected to the named host, at the given port.
    * 
    * @since 2.1 (20050615)
    */
   public Socket createSocket(Socket socket, String host, int port, boolean autoClose) throws IOException
   {
      if( super.isEnabled() && (super.getSSLContext() != null) )
      {
         if( super.isDebugMode() ) logDebug("Creating wrapped socket for address: " + host + ", port: " + port + ".");
         
         return super.getSSLContext().getSocketFactory().createSocket(socket, host, port, autoClose);
      }
      else if( super.isEnabled() && (super.getSSLContext() == null) )
      {
         if( super.isDebugMode() ) logDebug("Creating wrapped socket for address: " + host + ", port: " + port + ". Using default javax.net.ssl.SSLSocketFactory.");
         
         return ((javax.net.ssl.SSLSocketFactory)javax.net.ssl.SSLSocketFactory.getDefault()).createSocket(socket, host, port, autoClose);
      }
      else 
      {
         logError("Unable to create wrapped socket for address: " + host + ", port: " + port + " - socket factory not initialized!");
         
         throw new IOException("SSLSocketFactory not initialized!");
      }
   }
   
   
   /* ### METHODS USING DEFAULTSOCKETFACTORY FOR CONNECT WITH TIMEOUT BEGIN ### */
   
   /**
    * Creates a socket and connects it to the specified host and port, using the specified timeout value. 
    * A timeout of zero is interpreted as an infinite timeout. The connection will then block until established or an error occurs. 
    * If a wrappedSocketFactory is set, it will be used to create the a standard socket, which will be passed to the method 
    * SSLSocketFactory.createSocket(Socket, String, int, boolean) to create a SSL socket. 
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
    * If a wrappedSocketFactory is set, it will be used to create the a standard socket, which will be passed to the method 
    * SSLSocketFactory.createSocket(Socket, String, int, boolean) to create a SSL socket.
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
   
   
   /**
    * Creates an HttpURLConnection object for the specified URL.
    * 
    * @param url the URL to create an HttpURLConnection.
    */
   public HttpURLConnection getHttpURLConnection(final URL url) throws IOException
   {
     URLConnection urlConn = url.openConnection();
      if(urlConn instanceof HttpsURLConnection )
      {
         HttpsURLConnection httpsUrlConn = (HttpsURLConnection)urlConn;
         
         if( this.hostnameVerifier != null ) httpsUrlConn.setHostnameVerifier(this.hostnameVerifier);
         httpsUrlConn.setSSLSocketFactory(super.getSSLContext().getSocketFactory());
         
         return httpsUrlConn;
      }
      else if(urlConn instanceof HttpURLConnection )
      {
         return (HttpURLConnection)urlConn;
      }
      else return null;
   }
   
   /**
    * Gets the HostnameVerifier to be used when creating HttpsURLConnection objects in the method {@link #getHttpURLConnection(URL)}.
    */
   public HostnameVerifier getHostnameVerifier()
   {
      return hostnameVerifier;
   }

   /**
    * Sets the HostnameVerifier to be used when creating HttpsURLConnection objects in the method {@link #getHttpURLConnection(URL)}.
    */
   public void setHostnameVerifier(HostnameVerifier hostnameVerifier)
   {
      this.hostnameVerifier = hostnameVerifier;
   }
}
