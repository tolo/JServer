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
 * Default implementation of the {@link ServerSocketFactory} interface for creation of standard server sockets.
 * 
 * @see ServerSocketFactory
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.1 build 690
 */
public class DefaultServerSocketFactory implements ServerSocketFactory
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
	public ServerSocket createServerSocket(final int port, final int backlog, InetAddress localAddress) throws IOException
	{
		return new ServerSocket(port, backlog, localAddress);
	}
}
