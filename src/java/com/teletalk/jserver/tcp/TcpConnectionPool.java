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

import com.teletalk.jserver.pool.PoolWorkerFactory;
import com.teletalk.jserver.pool.ThreadPool;

/**
 * This class is a specialized ThreadPool to handle TcpConnections.
 * 
 * @see com.teletalk.jserver.pool.ThreadPool
 * @see TcpConnection
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0.1
 */
public class TcpConnectionPool extends ThreadPool implements TcpConnectionFactory
{
	private final TcpUtilities socketFactory;
	
	private final TcpServer tcpServer;
	
	/**
	 * Creates a fixed size TcpConnectionPool.
	 * 
	 * @param tcpServer the parent of this pool.
	 * @param name name of the pool.
	 * @param size the size of the pool.
	 * @param workerClass the Class of which PoolWorker objects will be created.
	 * @param workerClassConstructorParams the contructorparameters to be used when creating PoolWorker objects.
	 * 
	 * @exception IllegalArgumentException if workerClass isn't a subclass of PoolWorker.
	 * @exception NoSuchMethodException if there was an error finding a constructor in the specified worker class matching the sprecfied contructor parameters.
	 */
	public TcpConnectionPool(TcpServer tcpServer, String name, int size, Class workerClass, Object[] workerClassConstructorParams) throws IllegalArgumentException, NoSuchMethodException
	{
		super(tcpServer, name, size, workerClass, workerClassConstructorParams, 60*1000);
		
		this.tcpServer = tcpServer;
		
		if(!((Class)TcpConnection.class).isAssignableFrom(workerClass)) throw new IllegalArgumentException("Workerclass must be a subclass of class TcpConnection!");

		socketFactory = new TcpUtilities(this);
	}
	
	/**
	 * Creates a fixed size TcpConnectionPool. The defaultconstructor of workerClass will be used when creating TcpConnection objects.
	 * 
	 * @param tcpServer the parent of this pool.
	 * @param name name of the pool.
	 * @param size the size of the pool.
	 * @param workerClass the Class of which PoolWorker objects will be created.
	 * 
	 * @exception IllegalArgumentException if workerClass isn't a subclass of PoolWorker.
	 */
	public TcpConnectionPool(TcpServer tcpServer, String name, int size, Class workerClass) throws IllegalArgumentException
	{
		super(tcpServer, name, size, workerClass);
		
		this.tcpServer = tcpServer;
		
		if(!((Class)TcpConnection.class).isAssignableFrom(workerClass)) throw new IllegalArgumentException("Workerclass must be a subclass of class TcpConnection!");

		socketFactory = new TcpUtilities(this);
	}
	
	/**
	 * Creates a fixed size TcpConnectionPool.
	 * 
	 * @param tcpServer the parent of this pool.
	 * @param name name of the pool.
	 * @param size the size of the pool.
	 * @param pooledConnectionFactory the factory used to create TcpConnection objects for this pool.
	 * 
	 * @exception IllegalArgumentException if workerClass isn't a subclass of PoolWorker.
	 * 
	 * @since 2.0 Build 757
	 */
	public TcpConnectionPool(TcpServer tcpServer, String name, int size, PoolWorkerFactory pooledConnectionFactory) throws IllegalArgumentException
	{
		super(tcpServer, name, size, size, pooledConnectionFactory);
		
		this.tcpServer = tcpServer;

		socketFactory = new TcpUtilities(this);
	}
	
	/**
	 * Creates a new client Socket and initializes a TcpConnection from the pool with it. This method makes it possible to 
	 * use TcpConnection objects as client side communication handlers.
	 * 
	 * @param remoteAddress the address for which to create a socket.
	 * @param connectTimeOut the max time this method is allowed to block the calling thread in an attempt to 
	 * connect the Socket.
	 * @param maxAttempts the maximum number of attempts that will be made to create a socket.
	 * 
	 * @exception Exception if there was an error creating a socket.
	 */
	public TcpConnection openConnection(TcpEndPointIdentifier remoteAddress, long connectTimeOut, int maxAttempts) throws Exception
	{
		return openConnection(remoteAddress, connectTimeOut, maxAttempts, null);
	}
	
	/**
	 * Creates a new client Socket and initializes a TcpConnection from the pool with it. This method makes it possible to transfer
	 * a custom data object to the initialized TcpConnectionData object.  This method makes it possible to 
	 * use TcpConnection objects as client side communication handlers.
	 * 
	 * @param remoteAddress the address for which to create a socket.
	 * @param connectTimeOut the max time this method is allowed to block the calling thread in an attempt to 
	 * connect the Socket.
	 * @param maxAttempts the maximum number of attempts that will be made to create a socket.
	 * @param data a TcpConnectionData object to be transferred to the TcpConnection.
	 * 
	 * @exception Exception if there was an error creating a socket.
	 */
	public TcpConnection openConnection(TcpEndPointIdentifier remoteAddress, long connectTimeOut, int maxAttempts, Object data) throws Exception
	{
		TcpConnection connection;
		
		Socket socket = socketFactory.createSocket(remoteAddress, connectTimeOut);
				
		connection = (TcpConnection)(this.initializeThread(new TcpConnectionData(socket, data, TcpConnectionData.CLIENT_SIDE_TYPE))).getWorker();
		
		connection.setObjectStreamResetInterval(tcpServer.getObjectStreamResetInterval());
		connection.setUseAlternativeResetMethod(tcpServer.isUsingAlternativeResetMethod());
		
		return connection;
	}
	
	/**
	 * Initializes a TcpConnection from the pool.
	 * 
	 * @param socket the Socket to initialize a TcpConnection with.
	 */
	public void openConnection(Socket socket)
	{
		TcpConnection connection;
		
		connection = (TcpConnection)(this.initializeThread(new TcpConnectionData(socket))).getWorker();
		
		connection.setObjectStreamResetInterval(tcpServer.getObjectStreamResetInterval());
		connection.setUseAlternativeResetMethod(tcpServer.isUsingAlternativeResetMethod());
	}
	
	/**
	 * Initializes a TcpConnection from the pool.
	 * 
	 * @param connectionData the TcpConnectionData to initialize a TcpConnection with.
	 */
	public void openConnection(TcpConnectionData connectionData)
	{
		TcpConnection connection;
		
		connection = (TcpConnection)(this.initializeThread(connectionData)).getWorker();
		
		connection.setObjectStreamResetInterval(tcpServer.getObjectStreamResetInterval());
		connection.setUseAlternativeResetMethod(tcpServer.isUsingAlternativeResetMethod());
	}
	
	/**
	 * Called when a new connection is accepted by the associated TcpServer.
	 * 
	 * @param socket a Socket object represented the accepted connection.
	 */
	public void serverSideConnectionAccepted(final Socket socket, final TcpEndPointIdentifier serverSocketAddess)
	{
		openConnection(socket);
	}
}
