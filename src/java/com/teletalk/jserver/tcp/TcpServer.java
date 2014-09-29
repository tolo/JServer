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

/*
	TODO: NIO support.
*/
package com.teletalk.jserver.tcp;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

import com.teletalk.jserver.StatusTransitionException;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.pool.PoolWorkerFactory;
import com.teletalk.jserver.property.NumberProperty;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.property.StringProperty;

/**
 * The TcpServer class is a SubSystem used for TCP communication. It can be customized through the use of a requesthandler class which
 * implements the behavior for how incoming request should be interpreted. This requesthandler class must be a subclass of <code>TcpConnection</code>
 * which defines basic functionality for tcp requests. <br>
 * <br>
 * <i>Note that the request class creation parameters (constructor parameters) must be set for the request handler class using the 
 * appropriate constructor or the method {@link #setRequestClassCreationParams(Object[])}. If this is not done a default set of parameters, consisting of the a name of the 
 * request handler class, will be used.</i><br>
 * <br>
 * By default, the TcpServer uses the class <code>TcpConnectionPool</code> to create a pool filled with objects of the requesthandler class. From this
 * pool a TcpConnection is initialized every time a new connection request arrives to the TcpServer. It is, however, possible to specify another object to be 
 * responsible for instantiating new TcpConnection object for incomming connections. This is accomplished by specifiying a <code>TcpConnectionFactory</code> object 
 * when creating a new TcpServer object. 
 * <br>
 * 
 * @see TcpConnection
 * @see TcpConnectionPool
 * @see TcpConnectionFactory 
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public class TcpServer extends SubSystem //implements ChannelOwner
{
	/** NumberProperty for the local port this TcpServer listens to. */
	protected NumberProperty localPort;
	
	/** NumberProperty for the local IP-address this TcpServer is bound to. */
	protected StringProperty localIP;
	
	/** The backlog (queued incoming connections) length used when listening for connections. */
	protected NumberProperty backlog;
	
	
	/** Property for server socket factory class name. */
	protected StringProperty serverSocketFactoryClass;
	
		
	/** The serversocket used to listen for connections. */
	protected ServerSocket serverSocket;
		
	/** The actual factory instance used for creating a server socket for this TcpServer. */
	protected ServerSocketFactory serverSocketFactory;
	
	/** The class used for handling requests. */
	protected Class requestHandlerClass;
	
	/** Parameters used when creating a new requestHandlerClass. */
	private Object[] requestClassCreationParams = null;
	private boolean usingDefaultRequestClassCreationParams = false;
	
	private PoolWorkerFactory pooledConnectionFactory = null;
	
	/** The factory responsible for creating the connection objects. */
	protected TcpConnectionFactory connectionFactory;
	protected final boolean usingDefaultConnectionFactory;
	
	/** The initial pool size of the default TcpConnectionFactory(connection pool) (Defaultvalue = 50). */
	protected int poolSize = 50;
	
	/** The Socket timeout value used by the client Sockets created by this TcpServer. Default is no timeout (-1). */
	protected int clienSocketTimeOut = -1;
	
	/** The Socket receive buffer size value used by the client Sockets created by this TcpServer. Default value is 32k. */
	protected int socketReceiveBufferSize = 32*1024;
	
	/** The Socket receive buffer size value used by the client Sockets created by this TcpServer. Default value is 32k. */
	protected int socketSendBufferSize = 32*1024;
	
	/***/
	protected boolean keepAlive = true;
	
	
	private int consecutiveNewConnectionErrorCount = 0;
	
	private InetAddress localInetAddress = null;
	
	private int objectStreamResetInterval = 1;
	
	private boolean useAlternativeResetMethod = false; //For Java 2SE 1.3
	
	//private ChannelDescriptor channelDescriptor = null;
		
	private boolean slaveModeEnabled = false;
	
	private boolean usingDynamicServerSocketFactory = false;

	/**
	 * Constructs a new TcpServer named "TcpServer" and bound to all/any local addresses (0.0.0.0).
	 * 
	 * @param parent the parent SubSystem.
	 * @param requestHandlerClass the requesthandler class; must be a subclass of TcpConnection.
	 * @param localPort the port on which this TcpServer should listen for requests.
	 * 
	 * @see com.teletalk.jserver.tcp.TcpConnection
	 */
	public TcpServer(SubSystem parent, Class requestHandlerClass, int localPort) 
	{
		this(parent, requestHandlerClass, localPort, TcpEndPointIdentifier.anyLocalHostString);
	}
	
	/**
	 * Constructs a new TcpServer named "TcpServer" and bound to all/any local addresses (0.0.0.0).
	 * 
	 * @param parent the parent SubSystem.
	 * @param requestHandlerClass the requesthandler class; must be a subclass of TcpConnection.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 * @param localPort the port on which this TcpServer should listen for requests.
	 * 
	 * @see com.teletalk.jserver.tcp.TcpConnection
	 */
	public TcpServer(SubSystem parent, Class requestHandlerClass, Object[] requestClassCreationParams, int localPort) 
	{
		this(parent, requestHandlerClass, requestClassCreationParams, localPort, TcpEndPointIdentifier.anyLocalHostString);
	}
	
	/**
	 * Constructs a new TcpServer bound to all/any local addresses (0.0.0.0).
	 * 
	 * @param parent the parent SubSystem.
	 * @param name the name of the TcpServer.
	 * @param requestHandlerClass the requesthandler class; must be a subclass of TcpConnection.
	 * @param localPort the port on which this TcpServer should listen for requests.
	 * 
	 * @see com.teletalk.jserver.tcp.TcpConnection
	 */
	public TcpServer(SubSystem parent, String name, Class requestHandlerClass, int localPort) 
	{
		this(parent, name, requestHandlerClass, localPort, TcpEndPointIdentifier.anyLocalHostString);
	}
	
	/**
	 * Constructs a new TcpServer bound to all/any local addresses (0.0.0.0).
	 * 
	 * @param parent the parent SubSystem.
	 * @param name the name of the TcpServer.
	 * @param requestHandlerClass the requesthandler class; must be a subclass of TcpConnection.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 * @param localPort the port on which this TcpServer should listen for requests.
	 * 
	 * @see com.teletalk.jserver.tcp.TcpConnection
	 */
	public TcpServer(SubSystem parent, String name, Class requestHandlerClass, Object[] requestClassCreationParams, int localPort) 
	{
		this(parent, name, requestHandlerClass, requestClassCreationParams, localPort, TcpEndPointIdentifier.anyLocalHostString);
	}
	
	/**
	 * Constructs a new TcpServer named "TcpServer".
	 * 
	 * @param parent the parent SubSystem.
	 * @param requestHandlerClass the requesthandler class; must be a subclass of TcpConnection.
	 * @param localPort the port on which this TcpServer should listen for requests.
	 * @param localIP the local ip address this TcpServer should be bound to.
	 * 
	 * @see com.teletalk.jserver.tcp.TcpConnection
	 */
	public TcpServer(SubSystem parent, Class requestHandlerClass, int localPort, String localIP)
	{
		this(parent, "TcpServer", requestHandlerClass, localPort, localIP);
	}
	
	/**
	 * Constructs a new TcpServer named "TcpServer".
	 * 
	 * @param parent the parent SubSystem.
	 * @param requestHandlerClass the requesthandler class; must be a subclass of TcpConnection.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 * @param address the address to which this TcpServer should be bound.
	 * 
	 * @see com.teletalk.jserver.tcp.TcpConnection
	 */
	public TcpServer(SubSystem parent, Class requestHandlerClass, Object[] requestClassCreationParams, TcpEndPointIdentifier address)
	{
		this(parent, "TcpServer", requestHandlerClass, address.getPort(), address.getAddress());
	}
	
	/**
	 * Constructs a new TcpServer named "TcpServer".
	 * 
	 * @param parent the parent SubSystem.
	 * @param requestHandlerClass the requesthandler class; must be a subclass of TcpConnection.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 * @param localPort the port on which this TcpServer should listen for requests.
	 * @param localIP the local ip address this TcpServer should be bound to.
	 * 
	 * @see com.teletalk.jserver.tcp.TcpConnection
	 */
	public TcpServer(SubSystem parent, Class requestHandlerClass, Object[] requestClassCreationParams, int localPort, String localIP)
	{
		this(parent, "TcpServer", requestHandlerClass, requestClassCreationParams, localPort, localIP);
	}
	
	/**
	 * Constructs a new TcpServer.
	 * 
	 * @param parent the parent SubSystem.
	 * @param name the name of the TcpServer.
	 * @param requestHandlerClass the requesthandler class; must be a subclass of TcpConnection.
	 * @param localPort the port on which this TcpServer should listen for requests.
	 * @param localIP the local ip address this TcpServer should be bound to.
	 * 
	 * @see com.teletalk.jserver.tcp.TcpConnection
	 */
	public TcpServer(SubSystem parent, String name, Class requestHandlerClass, int localPort, String localIP)
	{
		this(parent, name, requestHandlerClass, null, localPort, localIP);
		usingDefaultRequestClassCreationParams = true;
	}
	
	/**
	 * Constructs a new TcpServer.
	 * 
	 * @param parent the parent SubSystem.
	 * @param name the name of the TcpServer.
	 * @param requestHandlerClass the requesthandler class; must be a subclass of TcpConnection.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 * @param address the address to which this TcpServer should be bound.
	 * 
	 * @see com.teletalk.jserver.tcp.TcpConnection
	 */
	public TcpServer(SubSystem parent, String name, Class requestHandlerClass, Object[] requestClassCreationParams, TcpEndPointIdentifier address)
	{
		this(parent, name, requestHandlerClass, requestClassCreationParams, address.getPort(), address.getAddress());
	}
	
	/**
	 * Constructs a new TcpServer.
	 * 
	 * @param parent the parent SubSystem.
	 * @param name the name of the TcpServer.
	 * @param requestHandlerClass the requesthandler class; must be a subclass of TcpConnection.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 * @param localPort the port on which this TcpServer should listen for requests.
	 * @param localIP the local ip address this TcpServer should be bound to.
	 * 
	 * @see com.teletalk.jserver.tcp.TcpConnection
	 */
	public TcpServer(SubSystem parent, String name, Class requestHandlerClass, Object[] requestClassCreationParams, int localPort, String localIP)
	{
		super(parent, name);
		
		this.serverSocketFactory = null;
		
		if(com.teletalk.jserver.util.JavaBugUtils.isUsingJava1_3_0())
		{
			this.setUseAlternativeResetMethod(true);
			logInfo("Defaulting flag for alternative object output stream reset method to true, because Java 2 version 1.3 is used.");
		}
				
		this.usingDefaultConnectionFactory = true;
		this.connectionFactory = null;
		
		this.requestHandlerClass = requestHandlerClass;
		this.requestClassCreationParams = requestClassCreationParams;
		
		this.createProperties(localPort, localIP);
	}
	
	/**
	 * Constructs a new TcpServer named "TcpServer" and bound to all/any local addresses (0.0.0.0).
	 * 
	 * @param parent the parent SubSystem.
	 * @param connectionFactory the factory used to create or initialize handlers for incomming connections.
	 * @param localPort the port on which this TcpServer should listen for requests.
	 * 
	 * @see com.teletalk.jserver.tcp.TcpConnection
	 */
	public TcpServer(SubSystem parent, TcpConnectionFactory connectionFactory, int localPort) //throws UnknownHostException
	{
		this(parent, connectionFactory, localPort, TcpEndPointIdentifier.anyLocalHostString);
	}
	
	/**
	 * Constructs a new TcpServer bound to all/any local addresses (0.0.0.0).
	 * 
	 * @param parent the parent SubSystem.
	 * @param name the name of the TcpServer.
	 * @param connectionFactory the factory used to create or initialize handlers for incomming connections.
	 * @param localPort the port on which this TcpServer should listen for requests.
	 * 
	 * @see com.teletalk.jserver.tcp.TcpConnection
	 */
	public TcpServer(SubSystem parent, String name, TcpConnectionFactory connectionFactory, int localPort) //throws UnknownHostException
	{
		this(parent, name, connectionFactory, localPort, TcpEndPointIdentifier.anyLocalHostString);
	}
	
	/**
	 * Constructs a new TcpServer named "TcpServer".
	 * 
	 * @param parent the parent SubSystem.
	 * @param connectionFactory the factory used to create or initialize handlers for incomming connections.
	 * @param address the address to which this TcpServer should be bound.
	 * 
	 * @see com.teletalk.jserver.tcp.TcpConnection
	 */
	public TcpServer(SubSystem parent, TcpConnectionFactory connectionFactory, TcpEndPointIdentifier address)
	{
		this(parent, "TcpServer", connectionFactory, address.getPort(), address.getAddress());
	}
	
	/**
	 * Constructs a new TcpServer named "TcpServer".
	 * 
	 * @param parent the parent SubSystem.
	 * @param connectionFactory the factory used to create or initialize handlers for incomming connections.
	 * @param localPort the port on which this TcpServer should listen for requests.
	 * @param localIP the local ip address this TcpServer should be bound to.
	 * 
	 * @see com.teletalk.jserver.tcp.TcpConnection
	 */
	public TcpServer(SubSystem parent, TcpConnectionFactory connectionFactory, int localPort, String localIP)
	{
		this(parent, "TcpServer", connectionFactory, localPort, localIP);
	}
	
	/**
	 * Constructs a new TcpServer.
	 * 
	 * @param parent the parent SubSystem.
	 * @param name the name of the TcpServer.
	 * @param connectionFactory the factory used to create or initialize handlers for incomming connections.
	 * @param address the address to which this TcpServer should be bound.
	 * 
	 * @see com.teletalk.jserver.tcp.TcpConnection
	 */
	public TcpServer(SubSystem parent, String name, TcpConnectionFactory connectionFactory, TcpEndPointIdentifier address)
	{
		this(parent, name, connectionFactory, address.getPort(), address.getAddress());
	}
	
	/**
	 * Constructs a new TcpServer.
	 * 
	 * @param parent the parent SubSystem.
	 * @param name the name of the TcpServer.
	 * @param connectionFactory the factory used to create or initialize handlers for incomming connections.
	 * @param localPort the port on which this TcpServer should listen for requests.
	 * @param localIP the local ip address this TcpServer should be bound to.
	 * 
	 * @see com.teletalk.jserver.tcp.TcpConnection
	 */
	public TcpServer(SubSystem parent, String name, TcpConnectionFactory connectionFactory, int localPort, String localIP)
	{
		super(parent, name);
		
		this.serverSocketFactory = null;
		
		if(com.teletalk.jserver.util.JavaBugUtils.isUsingJava1_3_0())
		{
			this.setUseAlternativeResetMethod(true);
			logInfo("Defaulting flag for alternative object output stream reset method to true, because Java 2 version 1.3 is used.");
		}
		
		this.usingDefaultConnectionFactory = false;
		this.connectionFactory = connectionFactory;
		
		this.requestHandlerClass = null;
		
		this.createProperties(localPort, localIP);
	}
	
	/**
	 * Constructs a new TcpServer.
	 * 
	 * @param parent the parent SubSystem.
	 * @param name the name of the TcpServer.
	 * @param pooledConnectionFactory the PoolWorkerFactory to be used with a TcpConnectionPool to create TcpConnection objects for this TcpServer. 
	 * @param address the address to which this TcpServer should be bound.
	 * 
	 * @see com.teletalk.jserver.tcp.TcpConnection
	 */
	public TcpServer(SubSystem parent, String name, PoolWorkerFactory pooledConnectionFactory, TcpEndPointIdentifier address)
	{
		this(parent, name, pooledConnectionFactory, address.getPort(), address.getAddress());
	}
	
	/**
	 * Constructs a new TcpServer.
	 * 
	 * @param parent the parent SubSystem.
	 * @param name the name of the TcpServer.
	 * @param pooledConnectionFactory the PoolWorkerFactory to be used with a TcpConnectionPool to create TcpConnection objects for this TcpServer.
	 * @param localPort the port on which this TcpServer should listen for requests.
	 * @param localIP the local ip address this TcpServer should be bound to.
	 * 
	 * @see com.teletalk.jserver.tcp.TcpConnection
	 */
	public TcpServer(SubSystem parent, String name, PoolWorkerFactory pooledConnectionFactory, int localPort, String localIP)
	{
		super(parent, name);
		
		this.serverSocketFactory = null;
		
		if(com.teletalk.jserver.util.JavaBugUtils.isUsingJava1_3_0())
		{
			this.setUseAlternativeResetMethod(true);
			logInfo("Defaulting flag for alternative object output stream reset method to true, because Java 2 version 1.3 is used.");
		}
		
		this.usingDefaultConnectionFactory = true;
		this.connectionFactory = null;
		this.pooledConnectionFactory = pooledConnectionFactory;
		
		this.requestHandlerClass = null;
		
		this.createProperties(localPort, localIP);
	}
	
	/**
	 */
	private void createProperties(int localPort, String localIP)
	{
		if(localIP == null) localIP = TcpEndPointIdentifier.anyLocalHostString;
		
		this.localPort = new NumberProperty(this, "localPort", localPort, Property.MODIFIABLE_OWNER_RESTART);
		this.localPort.setDescription("The the port on which this TcpServer should listen for requests. The port number must be between 0 and 65535, inclusive.");
		this.localIP = new StringProperty(this, "localAddress", localIP, Property.MODIFIABLE_OWNER_RESTART);
		this.localIP.setDescription("The local address to which this TcpServer is bound. If the value of this property is empty or '0.0.0.0' the server will accept connections on any/all local addresses ");
		this.backlog = new NumberProperty(this, "backlog", 50, Property.MODIFIABLE_OWNER_RESTART);
		this.backlog.setDescription("The maximum queue length for incoming connection indications (a request to connect). The backlog must be >= 0.");
		this.serverSocketFactoryClass = new StringProperty(this, "serverSocketFactoryClass", "", StringProperty.NOT_MODIFIABLE, true);
		this.serverSocketFactoryClass.setDescription("Class name of the server socket factory to be created for this system on start up. An empty string means that the default factory will be used.");
				
		addProperty(this.localPort);
		addProperty(this.localIP);
		addProperty(this.backlog);
		addProperty(this.serverSocketFactoryClass);
	}
	
	/**
	 * Engages this TcpServer.
	 */
	protected void doInitialize()
	{
		try
		{
			super.doInitialize();
         
         // Attempt to get old property "local port"
         super.initFromConfiguredProperty(this.localPort, "local port", false, true);
         // Attempt to get old property "local address"
         super.initFromConfiguredProperty(this.localIP, "local address", false, true);
         // Attempt to get old property "server socket factory class"
         super.initFromConfiguredProperty(this.serverSocketFactoryClass, "server socket factory class", false, true);
			
			if(this.isUsingAlternativeResetMethod())
         {
				logWarning("Note: this TcpServer uses an alternative method for resetting the object output streams used by it's TcpConnection objects. This alternative method may be inefficient.");
         }
			
			destroyServerSocket();
			
			consecutiveNewConnectionErrorCount = 0;
			
			if(usingDefaultConnectionFactory && (connectionFactory == null))
			{
			   TcpConnectionPool tcpConnectionPool;
				if( this.pooledConnectionFactory != null )
				{
				   tcpConnectionPool = new TcpConnectionPool(this, "ConnectionPool", poolSize, this.pooledConnectionFactory);
				}
				else
				{
				   if(!((Class)TcpConnection.class).isAssignableFrom(requestHandlerClass)) throw new InstantiationException("RequestHandlerClass must inherit class TcpConnection!");
				
					if(usingDefaultRequestClassCreationParams)
					{
						String reqClassName = requestHandlerClass.getName();
						
						if(reqClassName.lastIndexOf(".") > 0) reqClassName = reqClassName.substring(reqClassName.lastIndexOf(".") + 1);
				
						Object[] params = {getFullName() + "." + reqClassName};
									
						requestClassCreationParams = params;
					}
					
					tcpConnectionPool = new TcpConnectionPool(this, "ConnectionPool", poolSize, requestHandlerClass, requestClassCreationParams);
				}
				
				connectionFactory = tcpConnectionPool;
				
				addSubComponent(tcpConnectionPool, true);
			}
		}
		catch(Exception e)
		{
			throw new StatusTransitionException("Error while engaging!", e);
		}
		
		if( this.serverSocketFactory == null )
		{
			String serverSocketFactoryClassName = this.serverSocketFactoryClass.stringValue();
			if( (serverSocketFactoryClassName != null) && (serverSocketFactoryClassName.trim().length() > 0) )
			{
				serverSocketFactoryClassName = serverSocketFactoryClassName.trim();
				try
				{
					logInfo("Attempting to create ServerSocketFactory from class " + serverSocketFactoryClassName + ".");
					this.setServerSocketFactory( (ServerSocketFactory)Class.forName(serverSocketFactoryClassName).newInstance() );
					this.usingDynamicServerSocketFactory = true;
				}
				catch(Exception e)
				{
					logError("Failed create ServerSocketFactory from class " + serverSocketFactoryClassName + ". Using default.", e);
               this.setServerSocketFactory( new DefaultServerSocketFactory() );
				}
			}
			else
			{
            this.setServerSocketFactory( new DefaultServerSocketFactory() );
			}
		}
		
		try
		{
         localInetAddress = null;
         String localAddressString = localIP.getValueAsString();
         if( localAddressString != null ) localAddressString = localAddressString.trim();
         
         if( (localAddressString != null) && !"".equals(localAddressString) )
         {
            localInetAddress = InetAddress.getByName(localAddressString);
         }
			
			this.serverSocket = this.serverSocketFactory.createServerSocket(this.localPort.intValue(), this.backlog.intValue(), localInetAddress);
		}
		catch(Exception e)
		{
			throw new StatusTransitionException("Error while engaging! Failed to create server socket. Error is: " + e + ".");
		}
	}
	
	/**
	 * Shuts down this TcpServer.
	 */
	protected void doShutDown()
	{
		super.doShutDown();
		
		destroyServerSocket();
		if( !super.isReinitializing() )
		{
		   destroyConnectionPool();
		}
		   
		System.gc();
		System.runFinalization();
	 }
	
	/**
	 * Critical error handler for this TcpServer.
	 */
	protected void doCriticalError()
	{
		super.doCriticalError();
		
		destroyServerSocket();
		destroyConnectionPool();
		
		System.gc();
		System.runFinalization();
	}
	
	private final void destroyServerSocket()
	{
		if(serverSocket != null)
		{
			try
			{
				serverSocket.close();
			}catch(Exception e){}
			serverSocket = null;
			
			System.gc();
			System.runFinalization();
		}
	}
	
	private final void destroyConnectionPool()
	{
		if(usingDefaultConnectionFactory && (connectionFactory != null))
		{
			try
			{
				((TcpConnectionPool)connectionFactory).shutDown();
			}catch(Exception e){}
			removeSubComponent(((TcpConnectionPool)connectionFactory));
			connectionFactory = null; 
		}
	}
	
	/**
	 * Called when a property owned by this TcpServer has changed.
	 * 
	 * @param property the property that has changed.
	 */
	public void propertyModified(Property property)
	{
		if( (property == localPort) || (property == localIP) )
		{
			//this.assignChannelDescriptor(this.channelDescriptor.getType(), this.channelDescriptor.getDescription());
		}
		super.propertyModified(property);
	}
	
	/**
	 * Validates a modification of a property's value. Subclasses can override this
	 * method to provide more specialized behaviour. This implementation validates various TcpServer 
	 * properties like local port and backlog. For that reason it is recommended that subclasses that override this method 
	 * calls the superclass implementation in an appropriate manner.
	 * 
	 * @param property The property to be validated.
	 * 
	 * @return boolean value indicating if the property passed (true) validation or not (false).
	 * 
	 * @see com.teletalk.jserver.property.PropertyOwner
	 */
	public boolean validatePropertyModification(Property property)
	{
		if(property ==	localPort)
			return (localPort.intValue() >= 0) && (localPort.intValue() <= 65535);
		else if(property ==	backlog)
			return backlog.intValue() >= 0;
		else return super.validatePropertyModification(property);
	}
	
	/**
	 */
	/*public void assignChannelDescriptor(String type, String description)
	{
		this.assignChannelDescriptor(new ChannelDescriptor(type, description, this.getLocalAddress()));
	}*/
	
	/**
	 */
	/*public void assignChannelDescriptor(ChannelDescriptor channelDescriptor)
	{
		this.channelDescriptor = channelDescriptor;
		
		//if(JServer.getJServer() != null)
		//{
			//JServer.getJServer().getSnsClientManager().registerChannel(this);
		//}
	}*/
	
	/**
	 */
	/*public ChannelDescriptor getChannelDescriptor()
	{
		return this.channelDescriptor;
	}*/
	
	/**
	 * Enables or disables slave mode. If slave mode is enabled, the properties for 
	 * local address and port will be disabled, i.e. not modifiable through the administration tool. Slave mode also means 
	 * that socket parameters will not be set by the TcpServer on an accepted connection and the server socket factory will not be added as a 
    * sub component. 
	 * 
	 * @param slaveModeEnabled boolean flag indicating if slave mode should be enabled (<code>true</code>) 
	 * or not (<code>false</code>).
	 * 
	 * @since 1.2
	 */
	public void setSlaveMode(boolean slaveModeEnabled)
	{
		if( slaveModeEnabled )
		{
			this.localPort.setModificationMode(Property.NOT_MODIFIABLE);
			this.localPort.setPersistent(false);
			this.localIP.setModificationMode(Property.NOT_MODIFIABLE);
			this.localIP.setPersistent(false);
		}
		else
		{
			this.localPort.setModificationMode(Property.MODIFIABLE_NO_RESTART);
			this.localPort.setPersistent(true);
			this.localIP.setModificationMode(Property.MODIFIABLE_NO_RESTART);
			this.localIP.setPersistent(true);
		}
		
		this.slaveModeEnabled = slaveModeEnabled;
	}
	
	/**
	 * Checks if slave mode is enabled or not. If slave mode is enabled, the properties for 
	 * local address and port will be disabled, i.e. not modifiable through the administration tool. Slave mode also means 
	 * that socket parameters will not be set by the TcpServer on an accepted connection.
	 * 
	 * @return flag indicating if slave mode is enabled (<code>true</code>) or not (<code>false</code>).
	 * 
	 * @since 1.3 build 667
	 */
	public boolean isSlaveMode()
	{
		return this.slaveModeEnabled;
	}
	
	/**
	 * Gets the server socket factory class name specified dynamically for this system. Note that this is not necessarily the same as 
	 * the name of the class returned by method {@link #getServerSocketFactory()}, since the factory may have been 
	 * specified statically using the method {@link #setServerSocketFactory(ServerSocketFactory)}. 
	 * 
	 * @see SocketFactory
	 * 
	 * @since 1.3.1 build 690
	 */
	public String getServerSocketFactoryClassName()
	{
		return serverSocketFactoryClass.stringValue();
	}
	
	/**
	 * Checks if this system is using a server socket factory that has been specified dynamically (through property), i.e. {@link #getServerSocketFactoryClassName()}.
	 * 
	 * @since 1.3.1 build 690
	 */
	public boolean isUsingDynamicServerSocketFactory()
	{
		return usingDynamicServerSocketFactory;
	}
		
	/**
	 * Gets the socket factory responsible for creating a server socket object for this 
	 * TcpServer.
	 * 
	 * @return the current server socket factory.
	 * 
	 * @see ServerSocketFactory
	 * 
	 * @since 1.2
	 */
	public ServerSocketFactory getServerSocketFactory()
	{
		return this.serverSocketFactory;
	}
	
	/**
	 * Sets the socket factory responsible  for creating a server socket object for this 
	 * TcpServer.<br>
	 * <br>
	 * <i>Note:</i> If the tcp server is already started when calling this method, a restart ({@link SubSystem#reinitialize()}) 
	 * must be performed in order to make the new server socket implementation come into effect.
	 * 
	 * @param serverSocketFactory the new server socket factory.
	 * 
	 * @see ServerSocketFactory
	 * 
	 * @since 1.2
	 */
	public void setServerSocketFactory(final ServerSocketFactory serverSocketFactory)
	{
		if( this.serverSocketFactory != serverSocketFactory )
		{
			this.usingDynamicServerSocketFactory = false;
		}
		
		if( serverSocketFactory == null )
		{
			this.serverSocketFactory = new DefaultServerSocketFactory();
		}
		else
		{
			this.serverSocketFactory = serverSocketFactory;
		}
      
      if( !this.slaveModeEnabled )
      {
         if( (this.serverSocketFactory instanceof SubComponent) && (!this.hasSubComponent((SubComponent)this.serverSocketFactory)) )
         {
            super.addSubComponent((SubComponent)this.serverSocketFactory, true);
         }
      }
	}
   
   /**
    * Checks if connection activity logging is enabled. Connection classes should call this method 
    * before performing any information loggings.
    * 
    * @return the state of the connection activity logging flag, true if such loggings are enabled (allowed).
    * 
    * @see #connectionLogging
    * 
    * @deprecated as of 2.0.1, no replacement.
    */
   public final boolean sessionActivityLoggingEnabled()
   {
      return false;
   }
			
	/**
	 * Checks if connection activity logging is enabled. Connection classes should call this method 
	 * before performing any information loggings.
	 * 
	 * @return the state of the connection activity logging flag, true if such loggings are enabled (allowed).
	 * 
	 * @see #connectionLogging
    * 
    * @deprecated as of 2.1.5, no replacement.
	 */
	public final boolean isConnectionActivityLoggingEnabled()
	{
      return false;
	}
	
	/**
	 * Sets the parameters used when creating requesthandler objects. This method should be called before the TcpSever is engaged if 
	 * the parameters wasn't set in a constructor or the default parameters are to be used.
	 * <br><br>Note that this method is obsolete when an external TcpConnectionFactory is used.
	 * 
	 * @param params the parameters.
	 */
	public final void setRequestClassCreationParams(Object[] params)
	{
		requestClassCreationParams = params;
		usingDefaultRequestClassCreationParams = false;
	}
	
	/**
	 * Sets the size of the pool containig TcpConnection objects used by this TcpServer.
	 * <br><br>Note that this method is obsolete when an external TcpConnectionFactory is used.
	 * 
	 * @param poolSize the size.
	 */
	public final void setPoolSize(int poolSize)
	{
		this.poolSize = poolSize;	
		
		if(usingDefaultConnectionFactory && (connectionFactory != null)) // Null if the TcpServer hasn't started yet
		{
			((TcpConnectionPool)connectionFactory).setMinSize(poolSize);
		}
	}
	
	/**
	 * Gets the maximum number of objects contained in the pool.
	 * <br><br>Note that this method is obsolete when an external TcpConnectionFactory is used.
	 * 
	 * @return the maximum number of objects contained in the pool. -1 is returned if this TcpServer hasn't been started yet.
	 */
	public final int getPoolSize()
	{
		if(usingDefaultConnectionFactory && (connectionFactory != null)) // Null if the TcpServer hasn't started yet
		{
			return ((TcpConnectionPool)connectionFactory).getMinSize();
		}
		else return -1;
	}
	
	/**
	 * Sets the interval at which the object output stream of TcpConnection objects created by this TcpServer will be reset. The value 
	 * specified by parameter <code>objectStreamResetInterval</code> indicates how many object 
	 * writes ({@link TcpConnection#writeObject(Object)}) that have to be made before the streams are reset.
	 * <br><br>
	 * If the value of <code>resetInterval</code> is 0 (zero) stream reset is disabled.
	 * <br><br>
	 * The defaultvalue of this setting is 100.<br>
	 * <br>
	 * Note that if the parent of this TcpServer is a TcpCommunicationManager, calls to this method will be redirected there.
	 * 
	 * @param objectStreamResetInterval indicates how many object writes that have to be made before the object output stream is reset.
	 * 
	 * @see TcpConnection#setObjectStreamResetInterval(int)
	 */
	public final void setObjectStreamResetInterval(int objectStreamResetInterval)
	{
		this.objectStreamResetInterval = objectStreamResetInterval;
		if(this.parent instanceof TcpCommunicationManager)
		{
			((TcpCommunicationManager)parent).setObjectStreamResetInterval(objectStreamResetInterval);
		}
	}
	
	/**
	 * Gets the interval at which the object output stream of TcpConnection objects created by this TcpServer will be reset. The value 
	 * specified by parameter <code>objectStreamResetInterval</code> indicates how many object 
	 * writes ({@link TcpConnection#writeObject(Object)}) that have to be made before the streams are reset.
	 * <br><br>
	 * If the value of <code>resetInterval</code> is 0 (zero) stream reset is disabled.
	 * <br><br>
	 * The defaultvalue of this setting is 100.<br>
	 * <br>
	 * Note that if the parent of this TcpServer is a TcpCommunicationManager, calls to this method will be redirected there.
	 * 
	 * @return the number of object writes that have to be made before the object output stream is reset.
	 * 
	 * @see TcpConnection#getObjectStreamResetInterval()
	 */
	public final int getObjectStreamResetInterval()
	{
		if(this.parent instanceof TcpCommunicationManager) return ((TcpCommunicationManager)parent).getObjectStreamResetInterval();
		else return objectStreamResetInterval;
	}
	
	/**
	 * Sets the flag indicating if an alternative method should be used when resetting the object output stream 
	 * of all TcpConnection objects created by this TcpServer. This alternative reset method involvs creating a 
	 * new ObjectOutputStream object (specifically a {@link com.teletalk.jserver.util.NoHeadersObjectOutputStream} object) to reset 
	 * the stream.<br>
	 * <br>
	 * The foremost purpose of this alternative reset method is to correct a bug introduced in Java 2 version 1.3, which makes impossible 
	 * to reset an ObjectOutputStream object and consequently leads to a memory leak.<br>
	 * <br>
	 * Note that if the parent of this TcpServer is a TcpCommunicationManager, calls to this method will be redirected there.
	 * 
	 * @param useAlternativeResetMethod boolean flag indicating if the alternativ reset method should be used (<code>true</code>), or not (<code>false</code>).
	 * 
	 * @see TcpConnection#setUseAlternativeResetMethod(boolean)
	 */
	public final void setUseAlternativeResetMethod(boolean useAlternativeResetMethod)
	{
		if(this.parent instanceof TcpCommunicationManager) ((TcpCommunicationManager)parent).setUseAlternativeResetMethod(useAlternativeResetMethod);
		else this.useAlternativeResetMethod = useAlternativeResetMethod;
	}
	
	/**
	 * Checks if an alternative method will be used when resetting the object output stream 
	 * of all TcpConnection objects created by this TcpServer. This alternative reset method involvs creating a 
	 * new ObjectOutputStream object (specifically a {@link com.teletalk.jserver.util.NoHeadersObjectOutputStream} object) to reset 
	 * the stream.<br>
	 * <br>
	 * The foremost purpose of this alternative reset method is to correct a bug introduced in Java 2 version 1.3, which makes impossible 
	 * to reset an ObjectOutputStream object and consequently leads to a memory leak.<br>
	 * <br>
	 * Note that if the parent of this TcpServer is a TcpCommunicationManager, calls to this method will be redirected there.
	 * 
	 * @return <code>true</code> if the alternative reset method is currently used, otherwise <code>false</code>. 
	 * 
	 * @see TcpConnection#isUsingAlternativeResetMethod()
	 */
	public final boolean isUsingAlternativeResetMethod()
	{
		if(this.parent instanceof TcpCommunicationManager) return ((TcpCommunicationManager)parent).isUsingAlternativeResetMethod();
		else return useAlternativeResetMethod;
	}
	
	/**
	 * Sets the maximum queue length for connections requests on the ServerSocket used by this
	 * TcpServer. If a connection indication arrives when the queue is full, the connection is refused.
	 * The TcpServer uses a defaultvalue of 25 for this setting.
	 * 
	 * @param backlog the maximun queue length.
	 */
	public final void setBacklog(int backlog)
	{
		this.backlog.setValue(backlog);
	}
	
	/**
	 * Gets the maximum queue length for connections requests on the ServerSocket used by this
	 * TcpServer.
	 * 
	 * @return the current backlog value.
	 */
	public final int getBackLog()
	{
		return backlog.intValue();
	}
	
	/**
	 * Sets the Socket timeout value used by the client Sockets created by this TcpServer. With 
	 * this option set to a non-zero timeout, a read() call on the InputStream associated with the client 
	 * Socket will block for only this amount of time. By default there is no timeout.
	 * 
	 * @param timeout the timeout in milliseconds.
	 */
	public final void setSoTimeout(int timeout)
	{
		this.clienSocketTimeOut = timeout;
	}
	
	/**
	 * Gets the Socket timeout value used by the client Sockets created by this TcpCommunicationManager. With 
	 * this option set to a non-zero timeout, a read() call on the InputStream associated with the client 
	 * Socket will block for only this amount of time. By default there is no timeout.
	 * 
	 * @since 1.3 build 667
	 */
	public final int getSoTimeout()
	{
		return this.clienSocketTimeOut;
	}
	
	/**
	 * Sets the socket receive buffer size, used by all sockets created by this TcpServer. 
	 * The default value of this setting is 32k.
	 * 
	 * @param size the size of the socket receive buffer.
	 */
	public final void setSocketReceiveBufferSize(int size)
	{
		this.socketReceiveBufferSize = size;
	}
	
	/**
	 * Gets the socket receive buffer size, used by all sockets created by this TcpCommunicationManager. 
	 * The default value of this setting is 32k.
	 * 
	 * @since 1.3 build 667
	 */
	public final int getSocketReceiveBufferSize()
	{
		return this.socketReceiveBufferSize;
	}
	
	/**
	 * Sets the socket send buffer size, used by all sockets created by this TcpServer. 
	 * The default value of this setting is 32k.
	 * 
	 * @param size the size of the socket send buffer.
	 */
	public final void setSocketSendBufferSize(int size)
	{
		this.socketSendBufferSize = size;
	}
	
	/**
	 * Sets the socket send buffer size, used by all sockets created by this TcpCommunicationManager. 
	 * The default value of this setting is 32k.
	 * 
	 * @since 1.3 build 667
	 */
	public final int getSocketSendBufferSize()
	{
		return this.socketSendBufferSize;
	}
	
	/**
	 * Sets the value of the socket option SO_KEEPALIVE. This option will be set on all sockets created by this system.
    * 
	 * @since 1.3 build 667
	 */
	public final void setKeepAlive(boolean keepAlive)
	{
		this.keepAlive = keepAlive;
	}
	
	/**
    * Gets the value of the socket option SO_KEEPALIVE. This option will be set on all sockets created by this system.
	 *  
	 * @since 1.3 build 667
	 */
	public final boolean isKeepAlive()
	{
		return this.keepAlive;
	}
	
	/**
	 * Gets the local IP address which this TcpServer is bound to as an InetAddress object.
	 * 
	 * @return the local IP address as an InetAddress object.
	 */
	public final InetAddress getLocalInetAddress()
	{
		if(localInetAddress == null)
		{
			try
			{
            String localAddressString = localIP.getValueAsString();
            if( localAddressString != null ) localAddressString = localAddressString.trim();
            
            if( (localAddressString == null) || "".equals(localAddressString) )
				{
					localInetAddress = new InetSocketAddress(0).getAddress();
				}
				else
				{
					localInetAddress = InetAddress.getByName(localIP.getValueAsString());
				}
			}
			catch(Exception e)
			{
				return null;	
			}
		}
		
		return localInetAddress;
	}
	
	/**
	 * Gets the local IP address which this TcpServer is bound to as a String.
	 * 
	 * @return the local IP address as a String.
	 */
	public final String getLocalIPAddress()
	{
		return localIP.getValueAsString();
	}
	
	/**
	 * Sets the local address to which this TcpServer should be bound.
	 * 
	 * @param address an ip address.
	 */
	public final void setLocalIPAddress(String address)
	{
		this.localIP.setValue(address);
	}
	
	/**
	 * Get the local port used by this TcpServer to listen for connections.
	 * 
	 * @return the local port used by this TcpServer to listen for connections.
	 */
	public final int getLocalPort()
	{
		return localPort.intValue();
	}
	
	/**
	 * Sets the local port of this TcpServer.  
	 * 
	 * @param port the port on which this TcpServer should listen for requests.
	 */
	public final void setLocalPort(int port)
	{
		localPort.setValue(port);
	}
	
	/**
	 * Gets the local address to which this TcpServer is bound as a TcpEndPointIdentifier object.
	 * 
	 * @return a TcpEndPointIdentifier object.
	 */
	public TcpEndPointIdentifier getLocalAddress()
	{
		return new TcpEndPointIdentifier(this.getLocalInetAddress(), this.getLocalPort());
	}
	
	/**
	 * Gets the connection factory used by this TcpServer.
	 * 
	 * @return the connection factory used by this TcpServer.
	 */
	public TcpConnectionFactory getConnectionFactory()
	{
		return this.connectionFactory;
	}
	
	/**
	 * Gets the PoolWorkerFactory to be used with the default TcpConnectionFactory (TcpConnectionPool) to create TcpConnection objects. 
	 * 
	 * @since 2.0 Build 757
	 */
   public PoolWorkerFactory getPooledConnectionFactory()
   {
      return pooledConnectionFactory;
   }
   
   /**
    * Sets the PoolWorkerFactory to be used with the default TcpConnectionFactory (TcpConnectionPool) to create TcpConnection objects.
    * 
    * @since 2.0 Build 757
    */
   public void setPooledConnectionFactory(PoolWorkerFactory pooledConnectionFactory)
   {
      this.pooledConnectionFactory = pooledConnectionFactory;
   }
	
	/**
	 * The thread method of this TcpServer. Listens for incoming connections.
	 */
	public void run()
	{
		try
		{
			Socket s;
			
			while(canRun) 
			{
				s = serverSocket.accept();
				
				if(canRun) newConnection(s);
				else
				{
					if(s != null)
					{
						try{
						s.close();
						}catch(Throwable t){}
					}
				}
			}
		}
		catch(IOException ioe)
		{
			if(canRun)
			{
				error("Communication error while listening to port.");
				logError("Communication error while listening to port - " + ioe + ".");
			}
		}
		catch(Exception e)
		{
			if(canRun)
			{
				error("Error while listening to port.");
				logError("Error while listening to port", e);
			}
		}
	}
	
	/**
	 * Method for handling new connections. Initializes a TcpConnection from the TcpConnectionFactory used by this TcpServer.
	 * 
	 * @param socket the client socket for the new connection.
	 * 
	 * @see TcpConnection
	 * @see TcpConnectionFactory
	 * @see TcpConnectionPool
	 */
	public final void newConnection(final Socket socket)
	{
		try
		{
			if(isDebugMode()) logDebug("New connection from " + socket.getInetAddress().getHostAddress() + ".");
			
			if( !this.slaveModeEnabled )
			{
				if(clienSocketTimeOut > 0) socket.setSoTimeout(this.clienSocketTimeOut);
				socket.setReceiveBufferSize(this.getSocketReceiveBufferSize());
				socket.setSendBufferSize(this.getSocketSendBufferSize());
				socket.setKeepAlive(this.isKeepAlive());
			}
			
			this.connectionFactory.serverSideConnectionAccepted(socket, this.getLocalAddress());
			
			if(consecutiveNewConnectionErrorCount > 0) consecutiveNewConnectionErrorCount = 0;
		}
		catch(Exception e)
		{
			consecutiveNewConnectionErrorCount++;
			
			if(consecutiveNewConnectionErrorCount < 10)
			{
				logError("Error while while handling new connection!", e);
			}
			else
			{
				canRun = false;
				error("10 consecutive errors while handling new connections");
				logError("10 consecutive errors while accepting new connections", e);
			}
		}
	}
}
