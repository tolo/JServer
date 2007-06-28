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
import java.net.Socket;

/**
 * Interface for classes implementing a customized way of creating client sockets (<code>Socket</code> objects). This interface 
 * is used by classes such as <code>TcpCommunicationManager</code> to make it possible to create custom client sockets.
 * 
 * @see TcpUtilities
 * @see TcpCommunicationManager
 * @see ServerSocketFactory
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.2
 */
public interface SocketFactory
{
	/**
	 * Creates an unconnected socket.
	 * 
	 * @throws IOException if a connection cannot be established.
	 */
	public Socket createSocket() throws IOException;
	
	/**
	 * Creates a socket and connects it to the specified host and port.
	 * 
	 * @param host the server host.
	 * @param port the server port.
	 * 
	 * @throws IOException if a connection cannot be established.
	 */
	public Socket createSocket(InetAddress host, int port) throws IOException;
	
	/**
	 * Creates a socket and connects it to the specified host and port, using the specified timeout value. 
	 * A timeout of zero is interpreted as an infinite timeout. The connection will then block until established or an error occurs. 
	 * 
	 * @param host the server host.
	 * @param port the server port.
	 * @param connectTimeOut the timeout value to be used in milliseconds. 
	 * 
	 * @throws IOException if a connection cannot be established.
	 */
	public Socket createSocket(InetAddress host, int port, int connectTimeOut) throws IOException;
   
	/**
    * Creates a socket and connects it to the specified host and port, blocking until a connection has been established or an error occurs. 
    * The created socket will be bound to the specified local address and port.
    * 
    * @param host the server host.
    * @param port the server port.
    * @param localAddress the local address to bind to.
    * @param localPort the local port to bind to.
    * 
    * @throws IOException if a connection cannot be established.
    */
   public Socket createSocket(InetAddress host, int port, InetAddress localAddress, int localPort) throws IOException;
   
   /**
    * Creates a socket and connects it to the specified host and port, using the specified timeout value. 
    * The created socket will be bound to the specified local address and port.
    * A timeout of zero is interpreted as an infinite timeout. The connection will then block until established or an error occurs. 
    * 
    * @param host the server host.
    * @param port the server port.
    * @param localAddress the local address to bind to.
    * @param localPort the local port to bind to.
    * @param connectTimeOut the timeout value to be used in milliseconds. 
    * 
    * @throws IOException if a connection cannot be established.
    */
   public Socket createSocket(InetAddress host, int port, InetAddress localAddress, int localPort, int connectTimeOut) throws IOException;
}
