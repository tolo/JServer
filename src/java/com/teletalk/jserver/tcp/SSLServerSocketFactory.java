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
import java.net.InetAddress;
import java.net.ServerSocket;

import com.teletalk.jserver.SubComponent;

/**
 * Server socket factory implementation for creating SSL/TLS enabled server sockets. This factory extends 
 * the sub component class {@link SSLComponent}, which provides a number of properties used for the setup 
 * of secure communication.
 * 
 * @see ServerSocketFactory
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.1 Build 690
 */
public class SSLServerSocketFactory extends SSLComponent implements ServerSocketFactory
{
   /**
    * Creates a new SSLServerSocketFactory named "SSLServerSocketFactory".
    */
   public SSLServerSocketFactory()
   {
      this("SSLServerSocketFactory");
   }
   
   /**
    * Creates a new SSLServerSocketFactory.
    * 
    * @param name the name of this SSLServerSocketFactory
    */
   public SSLServerSocketFactory(String name)
   {
      this(null, name);
   }
   
   /**
    * Creates a new SSLServerSocketFactory.
    * 
    * @param parent the parent SSLServerSocketFactory
    * @param name the name of this SSLServerSocketFactory
    */
   public SSLServerSocketFactory(SubComponent parent, String name)
   {
      super(parent, name);
   }
   
	/**
	 * Creates a server socket bound to the specified port and local address and with the specified backlog 
	 * (maximum connection request queue length). If localAddress is null, it will default accepting connections on any/all local addresses.
	 * 
	 * @param port the port number to listen for connections on.
	 * @param backlog the maximum queue length for connections requests on the created ServerSocket.
	 * @param localAddress the local server address to bind the server socket to.
	 * 
	 * @throws IOException if a network error occurs.
	 */
	public ServerSocket createServerSocket(int port, int backlog, InetAddress localAddress) throws IOException
	{
		if( super.isEnabled() && (super.getSSLContext() != null) )
      {
         if( super.isDebugMode() ) logDebug("Creating server socket for address: " + localAddress + ", port: " + port + ".");
         
         return super.getSSLContext().getServerSocketFactory().createServerSocket(port, backlog, localAddress);
      }
      else if( super.isEnabled() && (super.getSSLContext() == null) )
      {
         if( super.isDebugMode() ) logDebug("Creating server socket for address: " + localAddress + ", port: " + port + ". Using default javax.net.ssl.SSLServerSocketFactory.");
         
         return javax.net.ssl.SSLServerSocketFactory.getDefault().createServerSocket(port, backlog, localAddress);
      }
      else 
      {
         logError("Unable to create server socket for address: " + localAddress + ", port: " + port + " - server socket factory not initialized!");
         
         throw new IOException("SSLServerSocketFactory not initialized!");
      }
	}
}
