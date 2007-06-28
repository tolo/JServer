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

/**
 * Interface for classes implementing a customized way of creating server sockets (<code>ServerSocket</code> objects). This interface 
 * is used by classes such as <code>TcpCommunicationManager</code> to make it possible to specify the server socket implementation 
 * that will be used to listen for connections.
 * 
 * @see TcpServer
 * @see SocketFactory
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.2
 */
public interface ServerSocketFactory
{
	/**
	 * Creates a server socket bound to the specified port and local address and with the specified backlog 
	 * (maximum connection request queue length).
	 * 
	 * @param port the port number to listen for connections on.
	 * @param backlog the maximum queue length for connections requests on the created ServerSocket.
	 * @param localAddress the local address to bind to.
	 * 
	 * @throws IOException if a network error occurs.
	 */
	public ServerSocket createServerSocket(int port, int backlog, InetAddress localAddress) throws IOException;
}
