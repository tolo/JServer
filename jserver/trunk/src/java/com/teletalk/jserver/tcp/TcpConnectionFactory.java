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

import java.net.Socket;

/**
 * Interface for classes responsible for handling new connections accepted by a TcpServer.
 * 
 * @see TcpServer
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 2
 */
public interface TcpConnectionFactory
{
	/**
	 * Called when a new connection is accepted by the associated TcpServer.
	 * 
	 * @param socket a Socket object represented the accepted connection.
    * @param acceptAddess the address of the server socket on which the socket was accepted.
	 */
	public void serverSideConnectionAccepted(Socket socket, TcpEndPointIdentifier acceptAddess);
}
