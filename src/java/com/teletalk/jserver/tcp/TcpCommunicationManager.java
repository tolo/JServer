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
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.teletalk.jserver.StatusTransitionException;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.comm.CommunicationManager;
import com.teletalk.jserver.comm.EndPoint;
import com.teletalk.jserver.comm.EndPointIdentifier;
import com.teletalk.jserver.pool.PoolWorkerFactory;
import com.teletalk.jserver.pool.ThreadPool;
import com.teletalk.jserver.property.NumberProperty;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.property.StringProperty;
import com.teletalk.jserver.property.VectorProperty;
import com.teletalk.jserver.property.VectorPropertyOwner;

/**
 * TcpCommunicationManager is a SubSystem class used to manage TCP communication, both server and client side, and it is an extension of the 
 * class TcpServer (which in fact is used by TcpCommunicationManager). TcpCommunicationManager keeps track on all active connections and makes 
 * them visible in the administration tool through the use of a VectorProperty object.<br>
 * <br>
 * TcpCommunicationManager will create TcpServer subsystems for listening for connections on the addresses specified by the property 
 * "<b>server addresses</b>". New addresses can also be added using the method {@link #addServerAddress(TcpEndPointIdentifier)}.<br>
 * <br>
 * A TcpCommunicationManager object can be customized through the use of a class that implements the behaviour of a communication endpoint. This 
 * class must be a subclass of <code>TcpEndPoint</code>, which defines basic functionality for tcp endpoints.<br>
 * <br>
 * The preferred way of creating TcpEndPoint objects is to specify a {@link com.teletalk.jserver.pool.PoolWorkerFactory}, either in a constructor or though 
 * the setter method {@link #setPooledEndPointFactory(PoolWorkerFactory)}. The alternative (old) way of doing this is to specify the endpoint class along 
 * with endpoint class creation parameters (constructor parameters). The latter can be specified for the endpoint class using the 
 * appropriate constructor or the method {@link #setEndPointClassCreationParams(Object[])}. If no PoolWorkerFactory or endpoint class constructor parameters 
 * are specified,  a default set of parameters, consisting of a reference to <code>this</code> and the a name of the endpoint class, will be used.</i><br>
 * <br>
 * The TcpCommunicationManager uses the class <code>ThreadPool</code> to create a pool filled with endpoint objects. From this
 * pool a TcpEndPoint is initialized every time a new connection request arrives to the TcpServer (and every time the user of this class wishes to initialize a client side 
 * TcpEndPoint). This is possible because TcpCommunicationManager implements the interface <code>TcpConnectionFactory</code>, which enables it to take responsible 
 * for instantiating new TcpEndPoint object for incomming connections.<br>
 * <br>
 * <i>Synchronization note:</i> The VectorProperty object {@link #endpointGroups} is used for synchronization of all endpoint group access.
 * <br>
 * <br>
 * <span style="font-family:courier;font-size:14px"><b>Properties:</b><br></span>
 * <ul> 
 * <span style="font-family:courier;font-size:11px">
 * <li><b>local addresses</b> - The server addresses on which to listen for connections.</li>
 * <li><b>connect timeout</b> - Connect timeout in milliseconds used when creating new client sockets.</li>
 * <li><b>connect attempts</b> - Maximum number of attempts that should be made to create a client socket connection before aborting.</li>
 * <li><b>socket factory class</b> - Class name of the socket factory to be created for this system on start up. An empty string means that the default factory will be used.</li>
 * <li><b>server socket factory class</b> - Class name of the server socket factory to be created for this system on start up. An empty string means that the default factory will be used.</li>
 * <li><b>client socket bind address</b> - The first port number to use when binding client sockets. Note that changing this property will not affect already created sockets.</li>
 * <li><b>client socket bind port begin</b> - The first port number to use when binding client sockets. Note that changing this property will not affect already created sockets.</li>
 * <li><b>client socket bind port end</b> - The last port number to use when binding client sockets. Note that changing this property will not affect already created sockets.</li>
 * </span>
 * </ul>
 * 
 * @see TcpEndPoint
 * @see TcpServer
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 2
 */
public class TcpCommunicationManager extends CommunicationManager implements TcpConnectionFactory, VectorPropertyOwner
{
	/** Name of external operation for VectorProperty <code>connections</code>, used for disconnecting active connections.	 */
	protected static final String disconnectOperationName = "disconnectTcpEndPoint";
	
	/** Vectorpropery containing connected endpoints (TcpEndPoint objects). */
	protected final VectorProperty endpoints;
	
	/** Vectorpropery containing the current endpoint groups (TcpEndPointGroup objects). Note that this object is used for internal synchronization. */
	protected final VectorProperty endpointGroups;
	
	/** TcpEndPointIdentifierProperty containing the local addresses on which this TcpCommunicationManager should listen for connections. */
	protected final TcpEndPointIdentifierProperty serverAddresses;
	
	/** Property for connect timeout used when creating sockets. */
	protected final NumberProperty connectTimeOut;
	
	/** Property for maximum attemtps that are to be made when creating a sockets. */
	protected final NumberProperty connectAttempts;
	
	/** Property for socket factory class name. */
	protected final StringProperty socketFactoryClass;
	
	/** Property for server socket factory class name. */
	protected final StringProperty serverSocketFactoryClass;
   
   /**The address used to bind client sockets to. @since 2.0 */
   protected final StringProperty clientSocketBindAddress;
   
   /** The first port number to use when binding client sockets. @since 2.0 */
   protected final NumberProperty clientSocketBindPortBegin;
   
   /** The last port number to use when binding client sockets. @since 2.0 */
   protected final NumberProperty clientSocketBindPortEnd;
	
	
	/** Flag indicating if empty TcpEndPointGroups should be removed. Default value is <code>true</code>. */
	protected boolean removeEmptyEndPointGroups = true;
	
	/** List containing the tcp servers used by this TcpCommunicationManager. */
	protected final List tcpServers;
	
	/** The factory used for creating a sockets for the TcpServers created by this TcpCommunicationManager. */
	protected SocketFactory socketFactory;
	
	/** The factory used for creating a server sockets for the TcpServers created by this TcpCommunicationManager. */
	protected ServerSocketFactory serverSocketFactory;
	
	/** The thread pool containing endpoints. */
	protected ThreadPool connectionPool;

	/** The class used for representing tcp endpoints. */
	protected Class tcpEndPointClass;
	
	/** Parameters used when creating a new tcpEndPointClass. */
	protected Object[] endPointClassCreationParams = null;
	/** Boolean flag inidcating if the default set of endpoint creation params are to be used.. */
	protected boolean usingDefaultEndPointClassCreationParams = false;
	/** The PoolWorkerFactory to be used with the endpoint pool (ThreadPool) to create TcpEndPoint objects. @since 2.0 Build 757*/
	protected PoolWorkerFactory pooledEndPointFactory = null;
	
	/** The initial size of the pool that holds the TcpEndPoint objects (Defaultvalue = 5). */
	protected int poolSize = 5;
	
	
	/** The Socket timeout value used by the client Sockets created by this TcpCommunicationManager. Default is infinite timeout (0). */
	protected int clienSocketTimeOut = 0;
	
	/** The Socket receive buffer size value used by the client Sockets created by this TcpCommunicationManager. Default value is 32k. */
	protected int socketReceiveBufferSize = 32*1024;
	
	/** The Socket receive buffer size value used by the client Sockets created by this TcpCommunicationManager. Default value is 32k. */
	protected int socketSendBufferSize = 32*1024;
	
	/** The value of the socket option SO_KEEPALIVE. This option will be set on all sockets created by this system. Default value is true.  */
	protected boolean keepAlive = true;
   
   /** The value of the socket option TCP_NODELAY. This option will be set on all sockets created by this system. Default value is true.  */
   protected boolean tcpNoDelay = true;
	
	
	private final TcpUtilities socketCreator;
	
	private int objectStreamResetInterval = 1;
	
	private boolean useAlternativeResetMethod = false; //For Java 2SE 1.3.0
	
	private boolean usingDynamicSocketFactory = false;
	
	private boolean usingDynamicServerSocketFactory = false;
   
   private final Object endPointLinkEstablishedMonitor = new Object();
   
	/**
	 * Creates a TcpCommunicationManager without any tcp servers listening for connections. 
	 * <br><br>The created TcpCommunicationManager will be named "TcpCommunicationManager".
	 * 
	 * @param parent the parent of this TcpCommunicationManager.
	 * @param tcpEndPointClass the class used to create endpoint objects for handling of connections. This class must inherit {@link TcpEndPoint}.
	 */
	public TcpCommunicationManager(SubSystem parent, Class tcpEndPointClass)
	{
		this(parent, "TcpCommunicationManager", tcpEndPointClass);
	}
	
	/**
	 * Creates a TcpCommunicationManager without any tcp servers listening for connections. 
	 * <br><br>The created TcpCommunicationManager will be named "TcpCommunicationManager".
	 * 
	 * @param parent the parent of this TcpCommunicationManager.
	 * @param tcpEndPointClass the class used to create endpoint objects for handling of connections. This class must inherit {@link TcpEndPoint}.
	 * @param endpointClassCreationParams the parameters that are to be used when creating instances of the class specified by parameter <code>tcpEndPointClass</code>.
	 */
	public TcpCommunicationManager(SubSystem parent, Class tcpEndPointClass, Object[] endpointClassCreationParams)
	{
		this(parent, "TcpCommunicationManager", tcpEndPointClass, endpointClassCreationParams);
	}
	
	/**
	 * Creates a TcpCommunicationManager without any tcp servers listening for connections.
	 * 
	 * @param parent the parent of this TcpCommunicationManager.
	 * @param name the name that will be given to this TcpCommunicationManager.
	 * @param tcpEndPointClass the class used to create endpoint objects for handling of connections. This class must inherit {@link TcpEndPoint}.
	 */
	public TcpCommunicationManager(SubSystem parent, String name, Class tcpEndPointClass)
	{
		this(parent, name, tcpEndPointClass, (Object[])null);
		this.usingDefaultEndPointClassCreationParams = true;
	}
	
	/**
	 * Creates a TcpCommunicationManager without any tcp servers listening for connections.
	 * 
	 * @param parent the parent of this TcpCommunicationManager.
	 * @param name the name that will be given to this TcpCommunicationManager.
	 * @param tcpEndPointClass the class used to create endpoint objects for handling of connections. This class must inherit {@link TcpEndPoint}.
	 * @param endPointClassCreationParams the parameters that are to be used when creating instances of the class specified by parameter <code>tcpEndPointClass</code>.
	 */
	public TcpCommunicationManager(SubSystem parent, String name, Class tcpEndPointClass, Object[] endPointClassCreationParams)
	{
		this(parent, name, tcpEndPointClass, endPointClassCreationParams, (TcpEndPointIdentifier[])null);
	}
	
	/**
	 * Creates a TcpCommunicationManager with a single tcp server, named "TcpServer", listening for connections. 
	 * <br><br>The created TcpCommunicationManager will be named "TcpCommunicationManager".
	 * 
	 * @param parent the parent of this TcpCommunicationManager.
	 * @param tcpEndPointClass the class used to create endpoint objects for handling of connections. This class must inherit {@link TcpEndPoint}.
	 * @param localAddress the address to which the tcp server should be bound.
	 */
	public TcpCommunicationManager(SubSystem parent, Class tcpEndPointClass, TcpEndPointIdentifier localAddress)
	{
		this(parent, "TcpCommunicationManager", tcpEndPointClass, (Object[])null, localAddress);
		this.usingDefaultEndPointClassCreationParams = true;
	}
	
	/**
	 * Creates a TcpCommunicationManager with a single tcp server, named "TcpServer", listening for connections. 
	 * <br><br>The created TcpCommunicationManager will be named "TcpCommunicationManager".
	 * 
	 * @param parent the parent of this TcpCommunicationManager.
	 * @param name the name that will be given to this TcpCommunicationManager.
	 * @param tcpEndPointClass the class used to create endpoint objects for handling of connections. This class must inherit {@link TcpEndPoint}.
	 * @param localAddress the address to which the tcp server should be bound.
	 */
	public TcpCommunicationManager(SubSystem parent, String name, Class tcpEndPointClass, TcpEndPointIdentifier localAddress)
	{
		this(parent, name, tcpEndPointClass, (Object[])null, localAddress);
		this.usingDefaultEndPointClassCreationParams = true;
	}
	
	/**
	 * Creates a TcpCommunicationManager with a single tcp server, named "TcpServer", listening for connections. 
	 * <br><br>The created TcpCommunicationManager will be named "TcpCommunicationManager".
	 * 
	 * @param parent the parent of this TcpCommunicationManager.
	 * @param tcpEndPointClass the class used to create endpoint objects for handling of connections. This class must inherit {@link TcpEndPoint}.
	 * @param endPointClassCreationParams the parameters that are to be used when creating instances of the class specified by parameter <code>tcpEndPointClass</code>.
	 * @param localAddress the address to which the tcp server should be bound.
	 */
	public TcpCommunicationManager(SubSystem parent, Class tcpEndPointClass, Object[] endPointClassCreationParams, TcpEndPointIdentifier localAddress)
	{
		this(parent, "TcpCommunicationManager", tcpEndPointClass, endPointClassCreationParams, localAddress);
	}
	
	/**
	 * Creates a TcpCommunicationManager with a single tcp server, named "TcpServer", listening for connections. 
	 * 
	 * @param parent the parent of this TcpCommunicationManager. 
	 * @param name the name that will be given to this TcpCommunicationManager. 
	 * @param tcpEndPointClass the class used to create endpoint objects for handling of connections. This class must inherit {@link TcpEndPoint}. 
	 * @param endPointClassCreationParams the parameters that are to be used when creating instances of the class specified by parameter <code>tcpEndPointClass</code>.
	 * @param localAddress the address to which the tcp server should be bound.
	 */
	public TcpCommunicationManager(SubSystem parent, String name, Class tcpEndPointClass, Object[] endPointClassCreationParams, TcpEndPointIdentifier localAddress)
	{
		this(parent, name, tcpEndPointClass, endPointClassCreationParams, ((localAddress != null) ? new TcpEndPointIdentifier[]{localAddress} : null));
	}
	
	/**
	 * Creates a TcpCommunicationManager with multiple tcp servers listening for connections. The number of tcp servers that will be created 
	 * depends on the sizes of the arrays specified by parameters <code>tcpServerNames</code> and <code>localAddress</code>.
	 * 
	 * @param parent the parent of this TcpCommunicationManager.
	 * @param name the name that will be given to this TcpCommunicationManager.
	 * @param tcpEndPointClass the class used to create endpoint objects for handling of connections. This class must inherit {@link TcpEndPoint}.
	 * @param localAddress array of addresses to which the created tcp servers should be bound.
	 */
	public TcpCommunicationManager(SubSystem parent, String name, Class tcpEndPointClass, TcpEndPointIdentifier[] localAddress)
	{
		this(parent, name, tcpEndPointClass, (Object[])null, localAddress);
		this.usingDefaultEndPointClassCreationParams = true;
	}
	
	/**
	 * Creates a TcpCommunicationManager with multiple tcp servers listening for connections. The number of tcp servers that will be created 
	 * depends on the sizes of the arrays specified by parameters <code>tcpServerNames</code> and <code>addresses</code>.
	 * 
	 * @param parent the parent of this TcpCommunicationManager.
	 * @param name the name that will be given to this TcpCommunicationManager. 
	 * @param tcpEndPointClass the class used to create endpoint objects for handling of connections. This class must inherit {@link TcpEndPoint}.
	 * @param endPointClassCreationParams the parameters that are to be used when creating instances of the class specified by parameter <code>tcpEndPointClass</code>.
	 * @param localAddress array of addresses to which the created tcp servers should be bound.
	 */
	public TcpCommunicationManager(SubSystem parent, String name, Class tcpEndPointClass, Object[] endPointClassCreationParams, TcpEndPointIdentifier[] localAddress)
	{
		super(parent, name);
		
		this.tcpEndPointClass = tcpEndPointClass;
		this.endPointClassCreationParams = endPointClassCreationParams;
		
		this.tcpServers = new ArrayList();
		this.socketFactory = null;
		this.serverSocketFactory = null;
				
		this.endpoints = new VectorProperty(this, "activeEndpoints");
      addProperty(endpoints);
		this.endpointGroups = new VectorProperty(this, "endpointGroups");
      addProperty(endpointGroups);
      
		this.serverAddresses = new TcpEndPointIdentifierProperty(this, "localAddresses", localAddress, Property.MODIFIABLE_NO_RESTART);
		this.serverAddresses.setDescription("The server (local) addresses on which to listen for connections.");
      addProperty(serverAddresses);
      
		this.connectTimeOut = new NumberProperty(this, "connectTimeout", 2500, NumberProperty.MODIFIABLE_NO_RESTART);
		this.connectTimeOut.setDescription("Connect timeout in milliseconds used when creating new client sockets.");
      addProperty(connectTimeOut);
      this.connectAttempts = new NumberProperty(this, "connectAttempts", 3, NumberProperty.MODIFIABLE_NO_RESTART);
		this.connectAttempts.setDescription("Maximum number of attempts that should be made to create a client socket connection before aborting.");
      addProperty(connectAttempts);
      
		this.socketFactoryClass = new StringProperty(this, "socketFactoryClass", "", StringProperty.NOT_MODIFIABLE, true);
		this.socketFactoryClass.setDescription("Class name of the socket factory to be created for this system on start up. An empty string means that the default factory will be used.");
      addProperty(socketFactoryClass);
		this.serverSocketFactoryClass = new StringProperty(this, "serverSocketFactoryClass", "", StringProperty.NOT_MODIFIABLE, true);
		this.serverSocketFactoryClass.setDescription("Class name of the server socket factory to be created for this system on start up. An empty string means that the default factory will be used.");
      addProperty(serverSocketFactoryClass);
		
      this.clientSocketBindAddress = new StringProperty(this, "clientSocketBindAddress", null, NumberProperty.MODIFIABLE_NO_RESTART);
      this.clientSocketBindAddress.setDescription("The local address to be used when binding client sockets. Note that changing this property will not affect already created sockets. An empty value means that the default address will be used.");
      addProperty(this.clientSocketBindAddress);
      
      this.clientSocketBindPortBegin = new NumberProperty(this, "clientSocketBindPortBegin", -1, NumberProperty.MODIFIABLE_NO_RESTART);
      this.clientSocketBindPortBegin.setDescription("The first port number to use when binding client sockets. Note that changing this property will not affect already created sockets.");
      addProperty(this.clientSocketBindPortBegin);
      
      this.clientSocketBindPortEnd = new NumberProperty(this, "clientSocketBindPortEnd", -1, NumberProperty.MODIFIABLE_NO_RESTART);
      this.clientSocketBindPortBegin.setDescription("The last port number to use when binding client sockets. Note that changing this property will not affect already created sockets.");
      addProperty(this.clientSocketBindPortEnd);
		
		
		this.socketCreator = new TcpUtilities(this);
		
		if(com.teletalk.jserver.util.JavaBugUtils.isUsingJava1_3_0())
		{
			this.setUseAlternativeResetMethod(true);
			logInfo("Defaulting flag for alternative object output stream reset method to true, because Java 2 version 1.3 is used.");
		}
		
		// Add external operations to vectorpropterty
		endpoints.addExternalOperation( TcpCommunicationManager.disconnectOperationName, "Disconnect" );
	}
	
	/**
	 * Creates a TcpCommunicationManager with a single tcp server, named "TcpServer", listening for connections.
	 * 
	 * @param parent the parent of this TcpCommunicationManager.
	 * @param name the name that will be given to this TcpCommunicationManager. 
	 * @param pooledEndPointFactory the PoolWorkerFactory to be used with a ThreadPool to create TcpEndPoint objects for this TcpCommunicationManager.
	 * @param localAddress the address to which the tcp server should be bound.
	 */
	public TcpCommunicationManager(SubSystem parent, String name, PoolWorkerFactory pooledEndPointFactory, TcpEndPointIdentifier localAddress)
	{
	   this(parent, name, null, null, ((localAddress != null) ? new TcpEndPointIdentifier[]{localAddress} : null));
	   
	   this.pooledEndPointFactory = pooledEndPointFactory;
	}
	
	/**
	 * Creates a TcpCommunicationManager with multiple tcp servers listening for connections. The number of tcp servers that will be created 
	 * depends on the sizes of the array specified by parameter <code>addresses</code>.
	 * 
	 * @param parent the parent of this TcpCommunicationManager.
	 * @param name the name that will be given to this TcpCommunicationManager. 
	 * @param pooledEndPointFactory the PoolWorkerFactory to be used with a ThreadPool to create TcpEndPoint objects for this TcpCommunicationManager.
	 * @param localAddresses array of addresses to which the created tcp servers should be bound.
	 */
	public TcpCommunicationManager(SubSystem parent, String name, PoolWorkerFactory pooledEndPointFactory, TcpEndPointIdentifier[] localAddresses)
	{
	   this(parent, name, null, null, localAddresses);
	   
	   this.pooledEndPointFactory = pooledEndPointFactory;
	}
	
	/**
	 * Engages this TcpCommunicationManager and all TcpServers under it.
	 */
	protected void doInitialize()
	{
		super.doInitialize();
		
		// Attempt to get old property "server addresses" to be used if "local addresses" property is missing
      super.initFromConfiguredProperty(this.serverAddresses, "server addresses", false, true);
      // Attempt to get old property "local addresses"
      super.initFromConfiguredProperty(this.serverAddresses, "local addresses", false, true);
      // Attempt to get old property "connect timeout(ms)" to be used if "connect timeout" property is missing
      super.initFromConfiguredProperty(this.connectTimeOut, "connect timeout(ms)", false, true);
      // Attempt to get old property "connect attempts"
      super.initFromConfiguredProperty(this.connectAttempts, "connect attempts", false, true);
      // Attempt to get old property "socket factory class"
      super.initFromConfiguredProperty(this.socketFactoryClass, "socket factory class", false, true);
      // Attempt to get old property "server socket factory class"
      super.initFromConfiguredProperty(this.serverSocketFactoryClass, "server socket factory class", false, true);
      // Attempt to get old property "client socket bind address"
      super.initFromConfiguredProperty(this.clientSocketBindAddress, "client socket bind address", false, true);
      // Attempt to get old property "client socket bind port begin"
      super.initFromConfiguredProperty(this.clientSocketBindPortBegin, "client socket bind port begin", false, true);
      // Attempt to get old property "client socket bind port end"
      super.initFromConfiguredProperty(this.clientSocketBindPortEnd, "client socket bind port end", false, true);
      		
		if(this.isUsingAlternativeResetMethod())
		{
			logWarning("Note: this TcpCommunicationManager uses an alternative method for resetting the object output streams used by it's TcpEndPoint objects. This alternative method may be inefficient.");
		}
		
		try
		{
			if(connectionPool == null)
			{
				if( this.pooledEndPointFactory != null )
				{
				   connectionPool = new ThreadPool(this, "EndpointPool", poolSize, poolSize, this.pooledEndPointFactory);
				}
				else
				{
				   if(!((Class)TcpEndPoint.class).isAssignableFrom(tcpEndPointClass))
						throw new InstantiationException("RequestHandlerClass must inherit class TcpEndPoint!");
					
					if(usingDefaultEndPointClassCreationParams)
					{
						String reqClassName = tcpEndPointClass.getName();
								
						if(reqClassName.lastIndexOf(".") > 0) reqClassName = reqClassName.substring(reqClassName.lastIndexOf(".") + 1);
				
						Object[] params = {this, reqClassName};
									
						 endPointClassCreationParams = params;
					}
	
					connectionPool = new ThreadPool(this, "EndpointPool", poolSize, tcpEndPointClass, endPointClassCreationParams);
				}
            addSubComponent(connectionPool, true);
			}
			
			if(!isReinitializing())
			{
            String socketFactoryClassName = this.socketFactoryClass.stringValue();
            
				if( (this.socketFactory == null) || (!this.socketFactory.getClass().getName().equals(socketFactoryClassName)) )
				{
					if( (socketFactoryClassName != null) && (socketFactoryClassName.trim().length() > 0) )
					{
						socketFactoryClassName = socketFactoryClassName.trim();
						try
						{
							logInfo("Attempting to create SocketFactory from class " + socketFactoryClassName + ".");
							this.setSocketFactory( (SocketFactory)Class.forName(socketFactoryClassName).newInstance() );
							this.usingDynamicSocketFactory = true;
						}
						catch(Exception e)
						{
							logError("Failed create SocketFactory from class " + socketFactoryClassName + ". Using default.", e);
                     this.setSocketFactory( new DefaultSocketFactory() );
						}
					}
					else
					{
                  this.setSocketFactory( new DefaultSocketFactory() );
					}
				}
				
				this.socketCreator.setSocketFactory(this.socketFactory);
				
            String serverSocketFactoryClassName = this.serverSocketFactoryClass.stringValue();
            
				if( (this.serverSocketFactory == null) || (!this.serverSocketFactory.getClass().getName().equals(serverSocketFactoryClassName)) )
				{
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
				
				// Create tcp servers
				this.setServerAddressesInternal( this.serverAddresses.getAddresses() );
				
				synchronized(tcpServers)
				{
					TcpServer tcpServer;
								
					// Engage tcp servers (those not already engaged)
					for(int i=0; i<tcpServers.size(); i++)
					{
						tcpServer = (TcpServer)tcpServers.get(i);
						
						if(tcpServer != null)
						{
							if( (tcpServer.getStatus() != INITIALIZING) && (tcpServer.getStatus() != ENABLED) )
                     {
								tcpServer.engage();
                     }
						}
					}
					// Wait for tcp servers to start
					for(int i=0; i<tcpServers.size(); i++)
					{
						tcpServer = (TcpServer)tcpServers.get(i);
						
						if(tcpServer != null)
						{
							try{
							tcpServer.waitForEnabled(2500);
							}catch(InterruptedException e){}
						}
					}
				}
            
            // Init socket creator
            this.socketCreator.setClientSocketBindAddress(this.clientSocketBindAddress.stringValue());
            this.socketCreator.setClientSocketBindPortBegin(this.clientSocketBindPortBegin.intValue());
            this.socketCreator.setClientSocketBindPortEnd(this.clientSocketBindPortEnd.intValue());
			}
		}
		catch(Exception e)
		{
			throw new StatusTransitionException("Error while engaging", e);
		}
	}
	
	/**
	 * Shuts down this TcpCommunicationManager and all TcpServers under it.
	 */
	protected void doShutDown()
	{
		super.doShutDown();
		
		if(!isReinitializing())
		{
			synchronized(tcpServers)
			{
				TcpServer tcpServer;
								
				for(int i=0; i<tcpServers.size(); i++)
				{
					tcpServer = (TcpServer)tcpServers.get(i);
						
					if(tcpServer != null)
					{
						if( (tcpServer.getStatus() != SHUTTING_DOWN) && (tcpServer.getStatus() != DOWN) && (tcpServer.getStatus() != CRITICAL_ERROR) ) tcpServer.shutDown();
					}
				}
			}
			
         synchronized(this.getEndpointGroupsLock())
         {
            this.destroyAllEndPoints();
            this.endpoints.clear();
            if( removeEmptyEndPointGroups ) this.endpointGroups.clear();
         }
			this.destroyConnectionPool();
         
         this.setSocketFactory(null);
         this.setServerSocketFactory(null);
		}
	}
	
	/**
	 */
	private final void destroyConnectionPool()
	{
		if(connectionPool != null)
		{
			try
			{
				connectionPool.shutDown();
            connectionPool.waitForDown(10000);
			}catch(Exception e){}
         try
         {
            removeSubComponent(connectionPool);
            connectionPool = null; 
         }catch(Exception e){}
		}
	}
	
	/**
	 * Called when a property owned by this TcpCommunicationManager is modified.
	 * 
	 * @param property the property that was modified.
	 */
	public void propertyModified(final Property property)
	{
		if(property == this.serverAddresses)
		{
			// Reset server addresses
			this.setServerAddressesInternal( this.serverAddresses.getAddresses() );
		}
      else if( property == this.clientSocketBindAddress )
      {
         this.socketCreator.setClientSocketBindAddress(this.clientSocketBindAddress.stringValue());
      }
      else if( property == this.clientSocketBindPortBegin )
      {
         this.socketCreator.setClientSocketBindPortBegin(this.clientSocketBindPortBegin.intValue());   
      }
      else if( property == this.clientSocketBindPortEnd )
      {
         this.socketCreator.setClientSocketBindPortEnd(this.clientSocketBindPortEnd.intValue());
      }
				
		super.propertyModified(property);
	}
	
	/**
	 * Validates a modification of a property's value. Subclasses can override this
	 * method to provide more specialized behaviour. This implementation validates various TcpCommunicationManager 
	 * properties like connectTimeOut and connectAttempts. For that reason it is recommended that subclasses that override this method 
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
		if(property == connectTimeOut) return connectTimeOut.intValue() > 100;
		else if(property == connectAttempts) return connectAttempts.intValue() > 0;
		else return super.validatePropertyModification(property);
	}
	
	/**
	 * Sets the parameters used when creating endpoint objects. This method should be called before the TcpCommunicationManager is engaged if 
	 * the parameters wasn't set in a constructor or the default parameters are to be used. 
	 *  
	 * @param params the parameters.
	 */
	public final void setEndPointClassCreationParams(Object[] params)
	{
		endPointClassCreationParams = params;
		usingDefaultEndPointClassCreationParams = false;
	}
	
	/**
	 * Gets the PoolWorkerFactory to be used with the endpoint pool (ThreadPool) to create TcpEndPoint objects. 
	 * 
	 * @since 2.0 Build 757
	 */
   public PoolWorkerFactory getPooledEndPointFactory()
   {
      return pooledEndPointFactory;
   }
   
   /**
    * Sets the PoolWorkerFactory to be used with the endpoint pool (ThreadPool) to create TcpEndPoint objects.
    * 
    * @since 2.0 Build 757
    */
   public void setPooledEndPointFactory(PoolWorkerFactory pooledEndPointFactory)
   {
      this.pooledEndPointFactory = pooledEndPointFactory;
   }
	
	/**
	 * Sets the size of the pool containig TcpEndPoint objects used by this TcpCommunicationManager.
	 *  
	 * @param poolSize the size.
	 */
	public final void setPoolSize(int poolSize)
	{
		this.poolSize = poolSize;	
		
		if(connectionPool != null) //If connectionPool is null, it is because the TcpCommunicationManager hasn't been started yet
		{
		   connectionPool.setMinSize(poolSize);
		}
	}
	
	/**
	 * Gets the maximum number of objects contained in the pool.
	 * 
	 * @return the maximum number of objects contained in the pool. -1 is returned if this TcpCommunicationManager hasn't been started yet.
	 */
	public final int getPoolSize()
	{
		if(connectionPool != null) return connectionPool.getMinSize();
		else return -1;
	}
	
	/**
	 * This method is called when an external operation is called on VectorProperty object <code>connections</code>.
	 * 
	 * @param operationName the internal name of the operation that was called.
	 * @param keys an array containing keys that was selected for the operation call.
	 */
	public void externalOperationCalled(final String operationName, final String[] keys)
	{
		TcpEndPoint endPoint;
		
		if( operationName.equals(disconnectOperationName) )
		{
			for(int i=0; i<keys.length; i++)
			{
				if( keys[i] != null )
				{
					endPoint = (TcpEndPoint)this.endpoints.get(keys[i]);
					
					if(endPoint != null)
					{
						if(this.isDebugMode()) logDebug("Disconnecting endpoint " + endPoint.toString() + ".");
						endPoint.disconnect();
					}
				}
			}
		}
	}
	
	/**
	 * Adds a local host server (listen) address on which this TcpCommunicationManager will listen for connections (via a {@link TcpServer}).
	 * 
	 * @param port the port on which to listen for connections.
	 * 
	 * @since 1.2
	 */
	public void addServerAddress(int port)
	{
		this.addServerAddress(new TcpEndPointIdentifier(port));
	}
		
	/**
	 * Adds a server (listen) address on which this TcpCommunicationManager will listen for connections (via a {@link TcpServer}).
	 * 
	 * @param address the local ip address to bind to.
	 * @param port the port on which to listen for connections.
	 * 
	 * @since 1.2
	 */
	public void addServerAddress(String address, int port)
	{
		this.addServerAddress(new TcpEndPointIdentifier(address, port));
	}
	
	/**
	 * Adds a server (listen) address on which this TcpCommunicationManager will listen for connections (via a {@link TcpServer}).
	 * 
	 * @param address the server address (local ip address and port) to add.
	 * 
	 * @since 1.2
	 */
	public void addServerAddress(final TcpEndPointIdentifier address)
	{
		if(address == null) return;
		
		if( (this.getStatus() == ENABLED) || (this.getStatus() == INITIALIZING) )
		{
			this.serverAddresses.addAddress(address);
		}
		else
		{
			this.serverAddresses.setNotificationMode(false);
			this.serverAddresses.addAddress(address);
			this.serverAddresses.setNotificationMode(true);
		}
	}
	
	/**
	 * Removes a server (listen) address on which this TcpCommunicationManager will listen for connections (via a {@link TcpServer}).
	 * 
	 * @param address the server address to remove.
	 * 
	 * @since 1.2
	 */
	public void removeServerAddress(final TcpEndPointIdentifier address)
	{
		if(address == null)
			return;		

		if( (this.getStatus() == ENABLED) || (this.getStatus() == INITIALIZING) )
		{
			this.serverAddresses.removeAddress(address);
		}
		else
		{
			this.serverAddresses.setNotificationMode(false);
			this.serverAddresses.removeAddress(address);
			this.serverAddresses.setNotificationMode(true);
		}
	}
	
	/**
	 * Sets the server addresses on which this TcpCommunicationManager will listen for connections (via a {@link TcpServer}).
	 * 
	 * @param addresses the addresses on which this TcpCommunicationManager is to listen for connections.
	 * 
	 * @return <code>true</code> if the addresses were set successfully, otherwise <code>false</code>.
	 * 
	 * @since 1.2
	 */
	public boolean setServerAddresses(final TcpEndPointIdentifier[] addresses)
	{
		return this.serverAddresses.setValue(addresses);
	}
		
	/**
	 * Internal method to create TcpServers corresponding to the property "server addresses".
	 */
	private final void setServerAddressesInternal(final TcpEndPointIdentifier[] newAddresses)
	{
		final ArrayList existingAddresses = new ArrayList();
		final ArrayList serversToRemove = new ArrayList();
		final ArrayList serversToAdd = new ArrayList();
		TcpServer tcpServer;
		TcpEndPointIdentifier serverAddress;
		List newAddressList = Arrays.asList( (newAddresses != null) ? newAddresses : new TcpEndPointIdentifier[0] );
						
		synchronized(this.tcpServers)
		{
			// Find addresses to remove
			for(int i=0; i<this.tcpServers.size(); i++)
			{
				tcpServer = (TcpServer)this.tcpServers.get(i);
				serverAddress = (tcpServer != null) ? tcpServer.getLocalAddress() : null;
				
				if( (newAddresses == null) || ((serverAddress != null) && !(newAddressList.contains(serverAddress))) )
				{
					serversToRemove.add(tcpServer);
				}
				else if( serverAddress != null ) existingAddresses.add(serverAddress);
			}
											
			// Find addresses to add
			if(newAddresses != null)
			{
				for(int i=0; i<newAddresses.length; i++)
				{
					serverAddress = (TcpEndPointIdentifier)newAddresses[i];
					
					// If server address is not null and didn't already exist
					if( (serverAddress != null) && !existingAddresses.contains(serverAddress) )
				    {
					   serversToAdd.add(serverAddress);
					}
				}
			}
		}
		
		// Remove
		for(int i=0; i<serversToRemove.size(); i++)
		{
			this.destroyTcpServer( (TcpServer)serversToRemove.get(i) );
		}
		
		// Add
		for(int i=0; i<serversToAdd.size(); i++)
		{
			this.createTcpServer( (TcpEndPointIdentifier)serversToAdd.get(i) );
		}
	}
	
	/**
	 * Gets the server addresses on which this TcpCommunicationManager currently listens for connections (via a {@link TcpServer}).
	 * 
	 * @return the addresses on which this TcpCommunicationManager currently listens for connections.
	 * 
	 * @since 1.2
	 */
	public TcpEndPointIdentifier[] getServerAddresses()
	{
		return this.serverAddresses.getAddresses();
	}
   
   /**
    * Convenience method to create and add a new TcpServer to this TcpCommunicationManager as a child subsystem. 
    * Note that this is not the same thing as calling the method {@link #addServerAddress(TcpEndPointIdentifier)}, which 
    * is the preferred way of adding new a new TcpServer. <br>
    * <br>
    * This method is only included for compatability reasons.
    * 
    * @param name the name of the TcpServer to add.
    * @param address the default address of the TcpServer.
    * 
    * @return the newly created TcpServer.
    * 
    * @deprecated since 1.2, replaced by {@link #addServerAddress(TcpEndPointIdentifier)}.
    */
   public TcpServer addTcpServer(final String name, final TcpEndPointIdentifier address)
   {
      TcpServer tcpServer = null;
      
      tcpServer = new TcpServer(this, name, this, address);
      this.addSubSystem(tcpServer);
      
      return tcpServer;
   }
   
   /**
    * This method only removes the tcp server with the specified name, if found, as 
    * as SubSystem (using the method {@link SubSystem#removeSubSystem(SubSystem)}. <br>
    * <br>
    * This method is only included for compatability reasons.
    * 
    * @param name the name of the tcp server to remove.
    * 
    * @return the newly created tcp server.
    * 
    * @deprecated since 1.2, replaced by {@link #removeServerAddress(TcpEndPointIdentifier)}.
    */
   public final TcpServer removeTcpServer(final String name)
   {
      SubSystem matchingTcpServer = super.getSubSystem(name);
            
      if( (matchingTcpServer != null) && (matchingTcpServer instanceof TcpServer) )
      {
         super.removeSubSystem(matchingTcpServer);
         
         return (TcpServer)matchingTcpServer;
      }
      else return null;
   }
	
	/**
	 * Internal method to create a new TcpServer.
	 */
	private void createTcpServer(final TcpEndPointIdentifier address)
	{
		TcpServer server;
		synchronized(tcpServers)
		{
			String serverAddress = address.getAddressAsString();
			if( serverAddress != null ) serverAddress = serverAddress.replace('.', '_');
			
			server = new TcpServer(this, ("TcpServer@" + serverAddress) , this, address);
			server.setSlaveMode(true); // Enable slave mode on the tcp server (disable modification of local address and port).
			server.setServerSocketFactory(this.serverSocketFactory);
			this.tcpServers.add(server);
		}
		
		this.addSubSystem(server, true);
	}
	
	/**
	 * Internal method to destroy a TcpServer.
	 */
	private void destroyTcpServer(final TcpServer server)
	{
		synchronized(this.tcpServers)
		{
			this.tcpServers.remove(server);
		}
		
		super.removeSubSystem(server, true);
	}
	
	/**
	 * Gets the TcpServer objects used by this TcpCommunicationManager to listen on the addresses 
	 * specified by the "server addresses" property.
	 * 
	 * @return a list containing TcpServer objects.
	 */
	public final List getTcpServers()
	{
		synchronized(tcpServers)
		{
			return new ArrayList(this.tcpServers);
		}
	}
	
	/**
	 * Gets the socket factory class name specified dynamically for this system. Note that this is not necessarily the same as 
	 * the name of the class returned by method {@link #getSocketFactory()}, since the factory may have been 
	 * specified statically using the method {@link #setSocketFactory(SocketFactory)}. 
	 * 
	 * @see SocketFactory
	 * 
	 * @since 1.3.1 build 690
	 */
	public String getSocketFactoryClassName()
	{
		return this.socketFactoryClass.stringValue();
	}

	/**
	 * Checks if this system is using a socket factory that has been specified dynamically (through property), i.e. {@link #getSocketFactoryClassName()}.
	 * 
	 * @since 1.3.1 build 690
	 */
	public boolean isUsingDynamicSocketFactory()
	{
		return this.usingDynamicSocketFactory;
	}
		
	/**
	 * Gets the socket factory responsible for creating client socket objects for this 
	 * TcpCommunicationManager.
	 * 
	 * @return the current client socket factory.
	 * 
	 * @see SocketFactory
	 * 
	 * @since 1.2
	 */
	public SocketFactory getSocketFactory()
	{
		return this.socketFactory;
	}
		
	/**
	 * Sets the socket factory responsible for creating client socket objects for this 
	 * TcpCommunicationManager.
	 * 
	 * @param socketFactory the new client socket factory.
	 * 
	 * @see SocketFactory
	 * 
	 * @since 1.2
	 */
	public void setSocketFactory(final SocketFactory socketFactory)
	{
      SocketFactory oldSocketFactory = this.socketFactory;
      
		if( this.socketFactory != socketFactory)
		{
			this.usingDynamicSocketFactory = false;
         if( (oldSocketFactory != null) && (oldSocketFactory instanceof SubComponent) )
         {
            super.removeSubComponent((SubComponent)oldSocketFactory);
         }
		}
		
      if( socketFactory == null )
      {
         this.socketFactory = new DefaultSocketFactory();
      }
      else
      {
   		this.socketFactory = socketFactory;
      }
		
		this.socketCreator.setSocketFactory(this.socketFactory);
      
      if( (this.socketFactory instanceof SubComponent) && (!this.hasSubComponent((SubComponent)this.socketFactory)) )
      {
         super.addSubComponent((SubComponent)this.socketFactory, true);
      }
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
	 * Gets the socket factory responsible for creating server socket objects for 
	 * TcpServers created by this TcpCommunicationManager.
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
	 * Sets the socket factory responsible  for creating server socket objects for 
	 * TcpServers created by this TcpCommunicationManager.
	 * 
	 * @param serverSocketFactory the new server socket factory.
	 * 
	 * @see ServerSocketFactory
	 * 
	 * @since 1.2
	 */
	public void setServerSocketFactory(final ServerSocketFactory serverSocketFactory)
	{
      ServerSocketFactory oldServerSocketFactory = this.serverSocketFactory;
      
		if( this.serverSocketFactory != serverSocketFactory )
		{
			this.usingDynamicServerSocketFactory = false;
         if( (oldServerSocketFactory != null) && (oldServerSocketFactory instanceof SubComponent) )
         {
            super.removeSubComponent((SubComponent)oldServerSocketFactory);
         }
		}
		
      if( serverSocketFactory == null )
      {
         this.serverSocketFactory = new DefaultServerSocketFactory();
      }
      else
      {
         this.serverSocketFactory = serverSocketFactory;
      }
      
      if( (this.serverSocketFactory instanceof SubComponent) && (!this.hasSubComponent((SubComponent)this.serverSocketFactory)) )
      {
         super.addSubComponent((SubComponent)this.serverSocketFactory, true);
      }
      
      List tcpServers = this.getTcpServers();
      TcpServer server;
      for(int i=0; i<tcpServers.size(); i++)
      {
         server = (TcpServer)tcpServers.get(i);
         if( server != null )
         {
            server.setServerSocketFactory(serverSocketFactory);
         }
      }
	}
	
	/**
	 * Gets the timeout value used when connecting sockets created by this TcpCommunicationManager. The value of this setting is the maximum time 
	 * each attempt to connect a socket may take.
	 * 
	 * @return the connect timeout value (in milliseconds).
	 * 
	 * @see #createSocket(TcpEndPointIdentifier)
	 */
	public final long getConnectTimeOut()
	{
		return connectTimeOut.longValue();	
	}
	
	/**
	 * Sets the timeout value used when connecting sockets created by this TcpCommunicationManager. The value of this setting is the maximum time 
	 * each attempt to connect a socket may take.
	 * 
	 * @param connectTimeOut the connect timeout value (in milliseconds).
	 * 
	 * @see #createSocket(TcpEndPointIdentifier)
	 */
	public final void setConnectTimeOut(long connectTimeOut)
	{
		this.connectTimeOut.setValue(connectTimeOut);
	}
	
	/**
	 * Gets the value of the setting for the number of attempts that should be made to connect a socket created by this TcpCommunicationManager. 
	 * 
	 * @return the current number of attempts that will be made to connect a socket.
	 * 
	 * @see #createSocket(TcpEndPointIdentifier)
	 */
	public final int getConnectAttempts()
	{
		return connectAttempts.intValue();
	}
	
	/**
	 * Sets the value of the setting for the number of attempts that should be made to connect a socket created by this TcpCommunicationManager. 
	 * 
	 * @param connectAttempts the number of attempts that will be made to connect a socket.
	 * 
	 * @see #createSocket(TcpEndPointIdentifier)
	 */
	public final void setConnectAttempts(int connectAttempts)
	{
		this.connectAttempts.setValue(connectAttempts);
	}
	
	/**
	 * Sets the interval at which the object output stream of TcpConnection objects created by this TcpServer will be reset. The value 
	 * specified by parameter <code>objectStreamResetInterval</code> indicates how many object 
	 * writes ({@link TcpConnection#writeObject(Object)}) that have to be made before the streams are reset.
	 * <br><br>
	 * If the value of <code>resetInterval</code> is 0 (zero) stream reset is disabled.
	 * <br><br>
	 * The defaultvalue of this setting is 1.
	 * 
	 * @param objectStreamResetInterval indicates how many object writes that have to be made before the object output stream is reset.
	 * 
	 * @see TcpConnection#setObjectStreamResetInterval(int)
	 */
	public final void setObjectStreamResetInterval(int objectStreamResetInterval)
	{
		this.objectStreamResetInterval = objectStreamResetInterval;
	}
	
	/**
	 * Gets the interval at which the object output stream of TcpConnection objects created by this TcpServer will be reset. The value 
	 * specified by parameter <code>objectStreamResetInterval</code> indicates how many object 
	 * writes ({@link TcpConnection#writeObject(Object)}) that have to be made before the streams are reset.
	 * <br><br>
	 * If the value of <code>resetInterval</code> is 0 (zero) stream reset is disabled.
	 * <br><br>
	 * The defaultvalue of this setting is 1.
	 * 
	 * @return the number of object writes that have to be made before the object output stream is reset.
	 * 
	 * @see TcpConnection#getObjectStreamResetInterval()
	 */
	public final int getObjectStreamResetInterval()
	{
		return objectStreamResetInterval;
	}
	
	/**
	 * Sets the flag indicating if an alternative method should be used when resetting the object output stream 
	 * of all TcpEndPoint objects created by this TcpCommunicationManager. This alternative reset method involvs creating a 
	 * new ObjectOutputStream object (specifically a {@link com.teletalk.jserver.util.NoHeadersObjectOutputStream} object) to reset 
	 * the stream.<br>
	 * <br>
	 * The foremost purpose of this alternative reset method is to correct a bug introduced in Java 2 version 1.3, which makes impossible 
	 * to reset an ObjectOutputStream object and consequently leads to a memory leak.
	 * 
	 * @param useAlternativeResetMethod boolean flag indicating if the alternativ reset method should be used (<code>true</code>), or not (<code>false</code>).
	 * 
	 * @see TcpConnection#setUseAlternativeResetMethod(boolean)
	 */
	public final void setUseAlternativeResetMethod(boolean useAlternativeResetMethod)
	{
		this.useAlternativeResetMethod = useAlternativeResetMethod;
	}
	
	/**
	 * Checks if an alternative method will be used when resetting the object output stream 
	 * of all TcpEndPoint objects created by this TcpCommunicationManager. This alternative reset method involvs creating a 
	 * new ObjectOutputStream object (specifically a {@link com.teletalk.jserver.util.NoHeadersObjectOutputStream} object) to reset 
	 * the stream.<br>
	 * <br>
	 * The foremost purpose of this alternative reset method is to correct a bug introduced in Java 2 version 1.3, which makes impossible 
	 * to reset an ObjectOutputStream object and consequently leads to a memory leak.
	 * 
	 * @return <code>true</code> if the alternative reset method is currently used, otherwise <code>false</code>. 
	 * 
	 * @see TcpConnection#isUsingAlternativeResetMethod()
	 */
	public final boolean isUsingAlternativeResetMethod()
	{
		return useAlternativeResetMethod;
	}
	
	/**
	 * Sets the Socket timeout value used by the client Sockets created by this TcpCommunicationManager. With 
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
	 * Sets the socket receive buffer size, used by all sockets created by this TcpCommunicationManager. 
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
	 * Sets the socket send buffer size, used by all sockets created by this TcpCommunicationManager. 
	 * The default value of this setting is 32k.
	 * 
	 * @param size the size of the socket send buffer.
	 */
	public final void setSocketSendBufferSize(int size)
	{
		this.socketSendBufferSize = size;
	}
	
	/**
	 * Gets the socket send buffer size, used by all sockets created by this TcpCommunicationManager. 
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
    * Gets the value of the socket option TCP_NODELAY. This option will be set on all sockets created by this system.
    * 
    * @since 1.3.1 build 670
    */
   public boolean isTcpNoDelay()
   {
      return this.tcpNoDelay;
   }

   /**
    * Sets the value of the socket option TCP_NODELAY. This option will be set on all sockets created by this system.
    * 
    * @since 1.3.1 build 670
    */
   public void setTcpNoDelay(boolean tcpNoDelay)
   {
      this.tcpNoDelay = tcpNoDelay;
   }
   
   /**
    * @param clienSocketTimeOut The clienSocketTimeOut to set.
    * 
    * @since 2.0
    */
   public void setClienSocketTimeOut(int clienSocketTimeOut)
   {
      this.clienSocketTimeOut = clienSocketTimeOut;
   }
   
   /**
    * @return Returns the clientSocketBindAddress.
    * 
    * @since 2.0
    */
   public StringProperty getClientSocketBindAddress()
   {
      return this.clientSocketBindAddress;
   }
   
   /**
    * @param clientSocketBindAddress The clientSocketBindAddress to set.
    * 
    * @since 2.0
    */
   public void setClientSocketBindAddress(String clientSocketBindAddress)
   {
      this.clientSocketBindAddress.setValue(clientSocketBindAddress);
   }
   
   /**
    * @return Returns the clientSocketBindPortBegin.
    * 
    * @since 2.0
    */
   public int getClientSocketBindPortBegin()
   {
      return this.clientSocketBindPortBegin.intValue();
   }
   
   /**
    * @param clientSocketBindPortBegin The clientSocketBindPortBegin to set.
    * 
    * @since 2.0
    */
   public void setClientSocketBindPortBegin(int clientSocketBindPortBegin)
   {
      this.clientSocketBindPortBegin.setValue(clientSocketBindPortBegin);
   }
   
   /**
    * @return Returns the clientSocketBindPortEnd.
    * 
    * @since 2.0
    */
   public int getClientSocketBindPortEnd()
   {
      return this.clientSocketBindPortEnd.intValue();
   }
   
   /**
    * @param clientSocketBindPortEnd The clientSocketBindPortEnd to set.
    * 
    * @since 2.0
    */
   public void setClientSocketBindPortEnd(int clientSocketBindPortEnd)
   {
      this.clientSocketBindPortEnd.setValue(clientSocketBindPortEnd);
   }
	
	/**
	 * Gets the current number of active endpoints associated with the address specified by parameter <code>address</code>.
	 * 
	 * @param address a TcpEndPointIdentifier.
	 * 
	 * @return the number of active endpoints for the specified address.
	 */
	public final int getNumberOfEndPoints(final TcpEndPointIdentifier address)
	{
		TcpEndPointGroup group = getTcpEndPointGroup(address);
		
		if(group != null)
		{
			return group.size();
		}
		else 
			return -1;
	}
   
   /**
    * Gets the object used for maintaining serialized access to the endpoints groups. 
    * 
    * @since 2.0.1 (20041126)
    */
   public Object getEndpointGroupsLock()
   {
      return this.endpointGroups;
   }
			
	/**
	 * Gets the TcpEndPointGroup for the specified address. 
	 * 
	 * @param address the address of the TcpEndPointGroup.
	 * 
	 * @return a TcpEndPointGroup or <code>null</code> if none was found for the specified address.
	 */
	public final TcpEndPointGroup getTcpEndPointGroup(final TcpEndPointIdentifier address)
	{
		return this.getTcpEndPointGroup(address, false, true);
	}
	
	/**
	 * Gets the TcpEndPointGroup for the specified address. If no group was found matching the specified address, 
	 * and parameter <code>create</code> is <code>true</code>, a new group will be created using the 
	 * method {@link #createTcpEndPointGroup(TcpEndPointIdentifier)}. The created group will also be added to this 
	 * TcpCommunicationManager.
	 * 
	 * @param address the address of the TcpEndPointGroup.
	 * @param create flag indicating if a new group should be created if none was found matching the specified address.
	 * @param clientSide flag indicating if a TcpEndPointGroup created by this method should be client side (<code>true</code>) or server side (<code>false</code>).
	 * 
	 * @return a TcpEndPointGroup for the specified address.
	 */
	protected final TcpEndPointGroup getTcpEndPointGroup(final TcpEndPointIdentifier address, final boolean create, final boolean clientSide)
	{
		TcpEndPointGroup group = null;
		final String identifierString = address.getAddressAsString();
		
		synchronized(this.getEndpointGroupsLock())
		{
			group = (TcpEndPointGroup)endpointGroups.get(identifierString);
			
			if( (group == null) && create )
			{
				group = this.createTcpEndPointGroup(address);
				group.setClientSide(clientSide);
				this.registerGroup(group);
			}
		}
		
		return group;
	}
   
   /**
    * Gets all {@link TcpEndPointGroup TcpEndPointGroups}.
    * 
    * @since 2.0.1 (20041126)
    */
   public TcpEndPointGroup[] getTcpEndPointGroups()
   {
      synchronized(this.getEndpointGroupsLock())
      {
         return (TcpEndPointGroup[])this.endpointGroups.getItems(new TcpEndPointGroup[0]);
      }
   }
	
	/**
	 * Gets an iterator that iterates over all currently active endpoints (connections).
	 * 
	 * @return a VectorProperty.VectorPropertyIterator object.
	 */
	public final VectorProperty.VectorPropertyIterator getEndPointIterator()
	{
		return endpoints.iterator();
	}
		
	/**
	 * Removes the endpoint identified by the parameter <code>key</code>.
	 * 
	 * @param key a String uniquely identifying an endpoint.
	 */
	public final void removeTcpEndPoint(final String key)
	{
		TcpEndPoint ep = (TcpEndPoint)endpoints.remove(key);
		
		if(ep != null)
		{
			ep.disconnect();
			unregisterEndPoint(ep);
		}
	}
	
	/**
	 * Destroys (disconnectes) all active endpoints in this TcpCommunicationManager.
	 */
	public final void destroyAllEndPoints()
	{
      synchronized(this.getEndpointGroupsLock())
      {
         TcpEndPointGroup[] tcpEndPointGroupsArray = this.getTcpEndPointGroups();
         
         for(int i=0;i<tcpEndPointGroupsArray.length; i++)
         {
            if(tcpEndPointGroupsArray[i] != null)
            {
               destroyEndPointGroup(tcpEndPointGroupsArray[i]);
            }
         }
      }
	}
	
	/**
	 * Destroys (disconnectes) all active endpoints associated with the specified address in this TcpCommunicationManager.
	 * 
	 * @param address the address for which to remove endpoints.
	 */
	public final void destroyEndPoints(final TcpEndPointIdentifier address)
	{
		this.destroyEndPointGroup(address);
	}
	
	/**
	 * Destroys (disconnectes) all active endpoints associated with the specified group in this TcpCommunicationManager.
	 * 
	 * @param address the address for which to remove endpoints.
	 * 
	 * @since 1.3
	 */
	public final void destroyEndPointGroup(final TcpEndPointIdentifier address)
	{
		final String identifierString = address.getAddressAsString();
		
		TcpEndPointGroup epg;
		
		synchronized(this.getEndpointGroupsLock())
		{
			epg = (TcpEndPointGroup)endpointGroups.get(identifierString);
		
			this.destroyEndPointGroup(epg);
      }
	}
		
	/**
	 * Destroys (disconnects) all active endpoints associated with the specified group in this TcpCommunicationManager.
	 * 
	 * @param group the group for which to remove endpoints.
	 * 
	 * @since 1.3 (public version)
	 */
	public final void destroyEndPointGroup(final TcpEndPointGroup group)
	{
		if(group != null)
		{
			List eps = group.getEndPoints();
			
			for(int i=0; i<eps.size(); i++)
			{
				((TcpEndPoint)eps.get(i)).disconnect();
			}
		}
	}
	
	/**
	 * Disconnectes and removes the specified endpoint from active connections.
	 * 
	 * @param endPoint the object representing the endpoint.
	 */
	public final void disconnectEndPoint(final TcpEndPoint endPoint)
	{
		endPoint.disconnect();
		unregisterEndPoint(endPoint);
	}
	
	/**
	 * Gets all the active endpoints in this TcpCommunicationManager as an array.
	 * 
	 * @return an array of EndPoint objects.
	 */
	public final EndPoint[] getEndPoints()
	{
		return (TcpEndPoint[])endpoints.getItems(new TcpEndPoint[]{});
	}
	
	/**
	 * Convenience method to get the endpoints contained in this TcpCommunicationManager as an TcpEndPoint array.
	 * 
	 * @return an array of TcpEndPoint objects.
	 */
	public final TcpEndPoint[] getTcpEndPoints()
	{
		return (TcpEndPoint[])endpoints.getItems(new TcpEndPoint[]{});
	}
	
	/**
	 * Convenience method to get the endpoints contained in this TcpCommunicationManager associated with the specified address.
	 * 
	 * @param address the address to get endpoints for.
	 * 
	 * @return an array of TcpEndPoint objects.
	 */
	public final TcpEndPoint[] getTcpEndPoints(final TcpEndPointIdentifier address)
	{
		final String identifierString = address.getAddressAsString();
		
		final TcpEndPointGroup group;
		
		synchronized(this.getEndpointGroupsLock())
		{
			group = (TcpEndPointGroup)endpointGroups.get(identifierString);
		}
		
		if(group != null)
		{
			final List groupEndPoints = group.getEndPoints();
			return (TcpEndPoint[])groupEndPoints.toArray(new TcpEndPoint[]{});
		}
		else
			return new TcpEndPoint[0];
	}
	
	/**
	 * Gets an endpoint matching the specified address.
	 * 
	 * @param address the address to get an endpoint for.
	 * 
	 * @return an EndPoint object, or <code>null</code> if no endpoint matching the specified address was found.
	 */
	public final EndPoint getEndPoint(final EndPointIdentifier address)
	{
		if(address instanceof TcpEndPointIdentifier)
		{
			return getTcpEndPoint((TcpEndPointIdentifier)address);
		}
		else
			return null;
	}
	
	/**
	 * Gets an endpoint matching the specified address. If there is no endpoint for 
	 * the specified address and the value of parameter <code>create</code> is <code>true</code>, 
	 * a new one will be created.
	 * 
	 * @param address the address to get an endpoint for.
	 * @param create boolean flag inidcating if a new endpoint should be created if there is none for the specified address.
	 * 
	 * @return an EndPoint object.
	 */
	public final EndPoint getEndPoint(final EndPointIdentifier address, final boolean create)
	{
		if(address instanceof TcpEndPointIdentifier)
		{
			return getTcpEndPoint((TcpEndPointIdentifier)address, create);
		}
		else
			return null;
	}
	
	/**
	 * Gets an TcpEndPoint matching the specified address. 
	 * 
	 * @param address the address to get an endpoint for.
	 * 
	 * @return an TcpEndPoint object, or <code>null</code> if no endpoint matching the specified address was found.
	 */
	public final TcpEndPoint getTcpEndPoint(final TcpEndPointIdentifier address)
	{
		return getTcpEndPoint(address, false);
	}
	
	/**
	 * Gets an TcpEndPoint matching the specified address. If there is no endpoint for 
	 * the specified address and the value of parameter <code>create</code> is <code>true</code>, 
	 * a new one will be created.
	 * 
	 * @param address the address to get an endpoint for.
	 * @param create boolean flag inidcating if a new endpoint should be created if there is none for the specified address.
	 * 
	 * @return an TcpEndPoint object.
	 */
	public final TcpEndPoint getTcpEndPoint(final TcpEndPointIdentifier address, final boolean create)
	{
		TcpEndPoint endPoint = null;
		TcpEndPointGroup group = null;
		final String identifierString = address.getAddressAsString();
		
		synchronized(this.getEndpointGroupsLock())
		{
			group = (TcpEndPointGroup)endpointGroups.get(identifierString);
		
			if(group != null) endPoint = group.getEndPoint();
		
			if((endPoint == null) && create)
			{
				endPoint = initializeClientSideTcpEndPoint(address);
			}
		}
		
		return endPoint;
	}
	
	/**
	 * Gets the endpoint identified by the parameter <code>key</code>.
	 * 
	 * @param key a String uniquely identifying an endpoint.
	 * 
	 * @return a TcpEndPoint object.
	 */
	public final TcpEndPoint getTcpEndPoint(final String key)
	{
		return (TcpEndPoint)endpoints.get(key);
	}
	
	/**
	 * Initializes and registers a new tcp endpoint. This method calls 
	 * {@link #initializeClientSideTcpEndPoint(TcpEndPointIdentifier)} to initialize a new endpoint.
	 * 
	 * @param address the address for which to create an endpoint.
	 * 
	 * @return a newly created TcpEndPoint object.
	 */
	public final TcpEndPoint createTcpEndPoint(final TcpEndPointIdentifier address)
	{
		return initializeClientSideTcpEndPoint(address);
	}
	
	/**
	 * Initializes and registers a new tcp endpoint. This method calls 
	 * {@link #initializeClientSideTcpEndPoint(TcpEndPointIdentifier, Object)} to initialize a new endpoint.
	 * 
	 * @param address the address for which to create an endpoint.
	 * @param customData a custom data object to be transferred to the created TcpEndPoint object.
	 * 
	 * @return a newly created TcpEndPoint object.
	 */
	public final TcpEndPoint createTcpEndPoint(final TcpEndPointIdentifier address, final Object customData)
	{
		return initializeClientSideTcpEndPoint(address, customData);
	}

	/**
	 * Internal method to initialize and register a new client side tcp endpoint. Subclasses can overridde this method to 
	 * initialize a tcp endpoint in another fashion and provide custom data for the endpoint. This method is called by for 
	 * instance the <code>getTcpEndPoint</code> and <code>getEndPoint</code> methods when a new endpoint 
	 * needs to be created.<br>
	 * <br>
	 * This implementation only calls {@link #initializeClientSideTcpEndPoint(TcpEndPointIdentifier, Object)} with a <code>null</code> 
	 * custom data object.
	 * 
	 * @param address the address to create an endpoint for.
	 * 
	 * @return a newly initialized TcpEndPoint object or <code>null</code> if this TcpComminicationManager hasn't created its connection pool yet, which is done during initialization.
	 */
	protected TcpEndPoint initializeClientSideTcpEndPoint(final TcpEndPointIdentifier address)
	{
		return initializeClientSideTcpEndPoint(address, null);
	}
	
	/**
	 * Internal method to initialize and register a new client side tcp endpoint. Subclasses can overridde this method to 
	 * initialize a tcp endpoint in another fashion.
	 * 
	 * @param address the address to create an endpoint for.
	 * @param customData a custom data object.
	 * 
	 * @return a newly initialized TcpEndPoint object or <code>null</code> if this TcpComminicationManager hasn't created its connection pool yet, which is done during initialization.
	 */
	protected TcpEndPoint initializeClientSideTcpEndPoint(final TcpEndPointIdentifier address, final Object customData)
	{
		if(connectionPool == null) return null;
			
		TcpEndPoint endPoint;
		
		endPoint = (TcpEndPoint)(connectionPool.initializeThread(new TcpConnectionData(address, customData))).getWorker();
		
		if(endPoint != null)
		{
			registerEndPoint(endPoint, true);  //Register the endpoint
		}
		return endPoint;
	}
   
   /**
    * Internal method to initialize and register a new server side tcp endpoint. Subclasses can overridde this method to 
    * initialize a tcp endpoint in another fashion. Note that this method 
    * is called by {@link #serverSideConnectionAccepted(Socket, TcpEndPointIdentifier)} when a new server side connection is accepted.
    * 
    * @param socket the socket to create an endpoint for.
    * @param customData a custom data object.
    * @param acceptAddess the address of the server socket on which the socket was accepted.
    * 
    * @return a newly initialized TcpEndPoint object or <code>null</code> if this TcpComminicationManager hasn't created its connection pool yet, which is done during initialization.
    */
   protected TcpEndPoint initializeServerSideTcpEndPoint(final Socket socket, final TcpEndPointIdentifier acceptAddess, final Object customData)
   {
      if( connectionPool == null ) return null;
      
      return (TcpEndPoint)(connectionPool.initializeThread(new TcpConnectionData(socket, customData, acceptAddess))).getWorker();
   }
	
	/**
	 * Called when the tcp server accepts a new connection. This method calls {@link #initializeServerSideTcpEndPoint(Socket, TcpEndPointIdentifier, Object)}, which can 
	 * be overridden by subclasses, to initialize a TcpEndPoint for the new connection.
	 * 
	 * @param socket the socket object of a newly accepted tcp connection.
    * @param acceptAddess the address of the server socket on which the socket was accepted. 
	 */
	public final void serverSideConnectionAccepted(final Socket socket, final TcpEndPointIdentifier acceptAddess)
	{
		if(socket != null)
		{
			try
			{
				if(clienSocketTimeOut > 0) socket.setSoTimeout(this.clienSocketTimeOut);
				socket.setReceiveBufferSize(this.getSocketReceiveBufferSize());
				socket.setSendBufferSize(this.getSocketSendBufferSize());
				socket.setKeepAlive(this.isKeepAlive());
            socket.setTcpNoDelay(this.isTcpNoDelay());            
			}
			catch(SocketException se)	
			{
				logWarning("Unable to set socket parameters for accepted socket.", se);
			}
		}
      
		initializeServerSideTcpEndPoint(socket, acceptAddess, null);
	}
	
	/**
	 * Convenience method that makes it possible to make a certain number of attempts at a certail interval to create a socket for 
	 * the specified address. The number of attempts that will be made is determined by the property <code>connectAttempts</code> and 
	 * the interval between the attempts (the maximum time a connect attempt is allowed to take) is determined by the property <code>connectTimeOut</code>.
	 * 
	 * @param remoteAddress the address for which to create a socket.
	 * 
	 * @return a newly created Socket object, or <code>null</code> if there was an error creating one.
	 */
	public final Socket createSocket(final TcpEndPointIdentifier remoteAddress)
	{
		return createSocket(remoteAddress, connectTimeOut.longValue(), connectAttempts.intValue());
	}
   
   /**
    * Convenience method that makes it possible to make a certain number of attempts at a certail interval to create a socket for 
    * the specified address. The number of attempts that will be made is determined by the property <code>connectAttempts</code> and 
    * the interval between the attempts (the maximum time a connect attempt is allowed to take) is determined by the property <code>connectTimeOut</code>.
    * 
    * @param remoteAddress the address for which to create a socket.
    * 
    * @return a newly created Socket object, or <code>null</code> if there was an error creating one.
    * 
    * @since 2.0
    */
   public final Socket createSocket(final TcpEndPointIdentifier remoteAddress, final TcpEndPointIdentifier localAddress)
   {
      return createSocket(remoteAddress, localAddress, connectTimeOut.longValue(), connectAttempts.intValue());
   }
	
	/**
	 * Convenience method that makes it possible to make a certain number of attempts at a certail interval to create a socket for 
	 * the specified address.
	 *  
	 * @param remoteAddress the address for which to create a socket.
	 * @param connectTimeOut the the interval between the connect attempts (the maximum time a connect attempt is allowed to take).
	 * @param maxAttempts the maximum number of attempts that will be made to create a socket.
	 * 
	 * @return a newly created Socket object, or <code>null</code> if there was an error creating one.
	 */
	public final Socket createSocket(final TcpEndPointIdentifier remoteAddress, final long connectTimeOut, final int maxAttempts)
	{
		return createSocket(remoteAddress, connectTimeOut, maxAttempts, false);
	}
   
   /**
    * Convenience method that makes it possible to make a certain number of attempts at a certail interval to create a socket for 
    * the specified address.
    *  
    * @param remoteAddress the address for which to create a socket.
    * @param connectTimeOut the the interval between the connect attempts (the maximum time a connect attempt is allowed to take).
    * @param maxAttempts the maximum number of attempts that will be made to create a socket.
    * 
    * @return a newly created Socket object, or <code>null</code> if there was an error creating one.
    * 
    * @since 2.0
    */
   public final Socket createSocket(final TcpEndPointIdentifier remoteAddress, final TcpEndPointIdentifier localAddress, final long connectTimeOut, final int maxAttempts)
   {
      return createSocket(remoteAddress, localAddress, connectTimeOut, maxAttempts, false);
   }
   
   /**
    * Convenience method that makes it possible to make a certain number of attempts at a certail interval to create a socket for 
    * the specified address.
    *  
    * @param remoteAddress the address for which to create a socket.
    * @param connectTimeOut the the interval between the connect attempts (the maximum time a connect attempt is allowed to take).
    * @param maxAttempts the maximum number of attempts that will be made to create a socket.
    * @param silent boolean flag indicating if errors/warning should be logged while trying to connect.
    * 
    * @return a newly created Socket object, or <code>null</code> if there was an error creating one.
    */
   public final Socket createSocket(final TcpEndPointIdentifier remoteAddress, final long connectTimeOut, final int maxAttempts, boolean silent)
   {
      return createSocket(remoteAddress, null, connectTimeOut, maxAttempts, silent);
   }
	
	/**
	 * Convenience method that makes it possible to make a certain number of attempts at a certail interval to create a socket for 
	 * the specified address.
	 *  
	 * @param remoteAddress the address for which to create a socket.
	 * @param connectTimeOut the the interval between the connect attempts (the maximum time a connect attempt is allowed to take).
	 * @param maxAttempts the maximum number of attempts that will be made to create a socket.
	 * @param silent boolean flag indicating if errors/warning should be logged while trying to connect.
	 * 
	 * @return a newly created Socket object, or <code>null</code> if there was an error creating one.
    * 
    * @since 2.0
	 */
	public final Socket createSocket(final TcpEndPointIdentifier remoteAddress, final TcpEndPointIdentifier localAddress, final long connectTimeOut, final int maxAttempts, boolean silent)
	{
		Socket socket = null;
      
      if( localAddress != null ) socket = socketCreator.createSocket(remoteAddress, localAddress, connectTimeOut, maxAttempts, silent);
      else socket = socketCreator.createSocket(remoteAddress, connectTimeOut, maxAttempts, silent);
		
		if(socket != null)
		{
			try
			{
				if(clienSocketTimeOut > 0) socket.setSoTimeout(this.getSoTimeout());
				socket.setReceiveBufferSize(this.getSocketReceiveBufferSize());
				socket.setSendBufferSize(this.getSocketSendBufferSize());
				socket.setKeepAlive(this.isKeepAlive());
            socket.setTcpNoDelay(this.isTcpNoDelay());
			}
			catch(SocketException se)	
			{
				logWarning("Unable to set socket parameters for created socket.", se);
			}
		}
		
		return socket;
	}
	
	/**
	 * Convenience method to get a TcpEndPointIdentifier representing the remote address of the specified socket.
	 * 
	 * @param socket the socket to get the remote address for.
	 * 
	 * @return a TcpEndPointIdentifier object representing the remote address.
	 */
	public final TcpEndPointIdentifier getRemoteAddress(final Socket socket)
	{
		return new TcpEndPointIdentifier(socket.getInetAddress(), socket.getPort());	
	}
	
	/**
	 * Method to register the specified endpoint group.<br>
	 * <br>
	 * Sublasses may override this method to receive notifications when a group is to be added.<br>
	 * <br>
	 * <i>Note: </i> a lock on {@link #endpointGroups} will be held when this method is called.
	 * 
	 * @param group a TcpEndPointGroup.
	 * 
	 * @since 1.3
	 */
	protected void registerGroup(final TcpEndPointGroup group)
	{
      synchronized(this.getEndpointGroupsLock())
      {
         this.endpointGroups.add(group);
      }
	}
	
	/**
	 * Method to unregister the specified endpoint group. Note that this method doesn't disconnect the 
	 * endpoints of the group. If this functionality is requested - use the method {@link #destroyEndPointGroup(TcpEndPointGroup)} 
	 * instead.<br>
	 * <br>
	 * Sublasses may override this method to receive notifications when a group is to be removed, or even to prevent the group from 
	 * beeing removed.<br>
	 * <i>Note: </i> a lock on {@link #getEndpointGroupsLock()} will be held when this method is called.
	 * 
	 * @param group a TcpEndPointGroup.
	 * 
	 * @since 1.3
	 */
	protected void unregisterGroup(final TcpEndPointGroup group)
	{
      synchronized(this.getEndpointGroupsLock())
      {
         this.endpointGroups.remove(group.getAddress().getAddressAsString());
      }
	}
			
	/**
	 * Method to register a TcpEndPoint in this TcpCommunicationManager.
	 * 
	 * @param endPoint the endpoint to register.
	 * @param clientSide indicating if the specified endpoint is a client side (<code>true</code>) or server side (<code>false</code>) endpoint.
	 * 
	 * @return <code>true</code> if the endpoint was successfully registered, otherwise <code>false</code>.
	 */
	protected boolean registerEndPoint(final TcpEndPoint endPoint, final boolean clientSide)
	{
		TcpEndPointIdentifier identifier = (TcpEndPointIdentifier)endPoint.getEndPointIdentifier();
		
		if(identifier != null)
		{
         int sequenceNumber;
         
         synchronized(this.getEndpointGroupsLock())
         {
   			TcpEndPointGroup group = this.getTcpEndPointGroup(identifier, true, clientSide);
   			
   			sequenceNumber = group.addEndPoint(endPoint);
         }
         			
			// Assign a sequence number to the endpoint. 
			// The sequence number is the index in the TcpEndPointGroup at which the endpoints was added
			endPoint.assignSequenceNumber(sequenceNumber); 

			endpoints.add(endPoint);
			return true;
		}

		return false;
	}
						
	/**
	 * Method to unregister a TcpEndPoint with this TcpCommunicationManager. Note that this method doesn't disconnect the 
	 * endpoint. If this functionality is requested - use the method {@link #disconnectEndPoint(TcpEndPoint)} 
	 * instead.
	 * 
	 * @param endPoint the endpoint to unregister.
	 * 
	 * @return <code>true</code> if the endpoint was successfully ungistered, otherwise <code>false</code>.
	 */
	protected boolean unregisterEndPoint(final TcpEndPoint endPoint)
	{
		EndPointIdentifier identifier = endPoint.getEndPointIdentifier();
		TcpEndPointGroup group = null;

      synchronized(this.getEndpointGroupsLock())
      {
   		if(identifier != null)
   		{
  				group = (TcpEndPointGroup)endpointGroups.get(identifier.getAddressAsString());
   		}
   		else // Fall back
   		{
            TcpEndPointGroup[] tcpEndPointGroupsArray = this.getTcpEndPointGroups();
            
            for (int i=0; i<tcpEndPointGroupsArray.length; i++)
            {
               if(tcpEndPointGroupsArray[i] != null)
               {
                  List endPoins = tcpEndPointGroupsArray[i].getEndPoints();
                  TcpEndPoint ep;
                  
                  for(int e=0; e<endPoins.size(); e++)
                  {
                     ep = (TcpEndPoint)endPoins.get(e);
                     if(ep != null && (ep == endPoint))
                     {
                        group = tcpEndPointGroupsArray[i];
                        break;
                     }
                  }
               }
            }
   		}
   		
   		return unregisterEndPoint(endPoint, group);
      }
	}
	
	/**
	 * Internal method to unregister a TcpEndPoint with this TcpCommunicationManager. 
	 * 
	 * @param endPoint the endpoint to unregister.
	 * @param group the group to which the endpoint belongs.
	 * 
	 * @return <code>true</code> if the endpoint was successfully ungistered, otherwise <code>false</code>.
	 */
	private final boolean unregisterEndPoint(final TcpEndPoint endPoint, final TcpEndPointGroup group)
	{
		if(group != null)
		{
			if(group.removeEndPoint(endPoint))  // If endpoint was in group...
			{
				if(group.size() == 0)
				{
					// Unregister enpoint group if empty and flag removeEmptyEndPointGroups is true
					if( (group.size() == 0) && this.removeEmptyEndPointGroups )
					{
						this.unregisterGroup(group);
					}
				}
			}
		}
		return endpoints.remove(endPoint);
	}
	
	/**
	 * Creates a new TcpEndPointGroup for the specified address. Subclasses can override this method to 
	 * create their own implementation of a TcpEndPointGroup.
	 * 
	 * @param address the address of the TcpEndPointGroup.
	 * 
	 * @return the newly created TcpEndPointGroup.
	 */
	protected TcpEndPointGroup createTcpEndPointGroup(final TcpEndPointIdentifier address)
	{
		return new TcpEndPointGroup(address);
	}
   
   
   /* ### NOTIFICATION METHODS BEGIN ### */
   
   
   /**
    * Internal version of {@link #endPointConnected(TcpEndPoint)}.
    */
   boolean endPointConnectedInternal(final TcpEndPoint endPoint)
   {
      if( endPoint.getEndPointGroup() != null )
      {
         endPoint.getEndPointGroup().endPointConnected(endPoint);
         endPointConnected(endPoint);
         return true;
      }
      else 
      {
         return false;
      }
   }

	/**
	 * Called when an endpoint gets connected. This method is provided exclusively for subclasses that wich to 
	 * be notified when an endpoint gets connected.<br>
    * <br>
    * <b>NOTE:<b> Since this is a notification method called by an internal thread, any overriding subclass implementations 
    * should make sure that this method executes and returns rather swiftly, preferably without any blocking wait.<br>
	 * <br>
	 * This implementaion is empty.
	 * 
	 * @param endPoint the object representing the endpoint.
	 */
	protected void endPointConnected(final TcpEndPoint endPoint)
	{
	}
   
   /**
    * Internal version of {@link #endPointLinkEstablished(TcpEndPoint)}.
    */
   void endPointLinkEstablishedInternal(final TcpEndPoint endPoint)
   {
      if( endPoint.getEndPointGroup() != null ) endPoint.getEndPointGroup().endPointLinkEstablished(endPoint);
      synchronized(this.endPointLinkEstablishedMonitor) // Wake up threads in waitForLinkEstablished
      {
         this.endPointLinkEstablishedMonitor.notifyAll();
      }
      
      endPointLinkEstablished(endPoint);
   }
   	
	/**
	 * Called when an endpoint establishes a link. This method is provided exclusively for subclasses that wich to 
	 * be notified when an endpoint establishes a link.<br>
    * <br>
    * <b>NOTE:<b> Since this is a notification method called by an internal thread, any overriding subclass implementations 
    * should make sure that this method executes and returns rather swiftly, preferably without any blocking wait.<br>
	 * <br>
	 * This implementaion is empty.
	 * 
	 * @param endPoint the object representing the endpoint.
	 */
	protected void endPointLinkEstablished(final TcpEndPoint endPoint)
	{
	}
   
   /**
    * Internal version of {@link #endPointDisconnected(TcpEndPoint)}.
    */
   void endPointDisconnectedInternal(final TcpEndPoint endPoint)
   {
      if( endPoint.getEndPointGroup() != null ) endPoint.getEndPointGroup().endPointDisconnected(endPoint);
      endPointDisconnected(endPoint);
   }
	
	/**
	 * Called when an endpoint gets disconnected. This method is provided exclusively for subclasses that wich to 
	 * be notified when an endpoint gets disconnected.<br>
    * <br>
    * <b>NOTE:<b> Since this is a notification method called by an internal thread, any overriding subclass implementations 
    * should make sure that this method executes and returns rather swiftly, preferably without any blocking wait.<br>
	 * <br>
	 * This implementaion is empty.
	 * 
	 * @param endPoint the object representing the endpoint.
	 */
	protected void endPointDisconnected(final TcpEndPoint endPoint)
	{
	}
   
   
   /* ### NOTIFICATION METHODS END ### */
   
   
   /**
    * Checks if this TcpCommunicationManager has at least one endpoint that has an established link.
    * 
    * @return <code>true</code> if at least one endpoint has an established link, otherwise <code>false</code>. 
    * 
    * @since 2.0.1 (20040915)
    */
   public boolean isLinkEstablished()
   {
      TcpEndPoint[] endPointArray = this.getTcpEndPoints();
      
      for(int i=0; i<endPointArray.length; i++)
      {
         if( (endPointArray[i] != null) && (endPointArray[i].isLinkEstablished()) ) return true;
      }
            
      return false;
   }
   
   /**
    * Blocks the calling thread until this TcpCommunicationManager has at least one endpoint that has an established link.
    * 
    * @return <code>true</code> if at least one endpoint has an established link, otherwise <code>false</code>. 
    * 
    * @since 2.0.1 (20040915)
    * 
    * @exception InterruptedException if the calling thread was interrupted.
    */
   public boolean waitForLinkEstablished() throws InterruptedException
   {
      boolean linkEstablished = isLinkEstablished();
      
      while(!linkEstablished)
      {
         synchronized(this.endPointLinkEstablishedMonitor)
         {
            this.endPointLinkEstablishedMonitor.wait(10000);  //Timeout after 10 seconds to check if  the flags have changed
         }
         
         linkEstablished = isLinkEstablished();
      }
      return linkEstablished;
   }
   
   /**
    * Blocks the calling thread until this TcpCommunicationManager has at least one endpoint that has an established link, but a maximum of <code>timeout</code> milliseconds.
    * 
    * @param timeout maximum time in milliseconds to wait for an endpoint to establish link.
    * 
    * @return <code>true</code> if at least one endpoint has an established link, otherwise <code>false</code>. 
    * 
    * @since 2.0.1 (20040915)
    * 
    * @exception InterruptedException if the calling thread was interrupted.
    */
   public boolean waitForLinkEstablished(final long timeout) throws InterruptedException
   {
      long beginWait = System.currentTimeMillis();
      long waitTime;
      boolean linkEstablished = false;

      synchronized(this.endPointLinkEstablishedMonitor)
      {
         linkEstablished = this.isLinkEstablished();
         
         while( !linkEstablished )
         {
            waitTime = timeout - (System.currentTimeMillis() - beginWait);
            
            if(waitTime > 0)
            {
               this.endPointLinkEstablishedMonitor.wait(waitTime);
               
               linkEstablished = this.isLinkEstablished();
            }
            else break;
         }
      }
      return linkEstablished;
   }
			
	/**
	 * Performs a check on all active endpoints. 
	 */
	protected void performConnectionCheck()
	{
		//Check connections for errors...
		VectorProperty.VectorPropertyIterator it = endpoints.iterator();
		TcpEndPoint ep;
		
		while(it.hasNext())
		{
			ep = (TcpEndPoint)it.next();
			
			if(ep != null)
			{
				if( !ep.check() && ep.isConnected() )
				{
					logWarning("Endpoint (" + ep + ") failed check!");
					disconnectEndPoint(ep);
				}
			}
		}
	}
	
	/**
	 * Implementation of the thread method for this TcpCommunicationManager, which calls the method 
	 * {@link #performConnectionCheck()} on a regular basis to check active connections for errors. Subclasses may 
	 * override this thread method but should make sure that <code>performConnectionCheck()</code> is called periodically, or 
	 * check the connections in some other way.
	 */
	public void run()
	{
		while(canRun)
		{
			try
			{
				Thread.sleep(30*1000);
			}
			catch(InterruptedException e)
			{
				if(!canRun) break;
			}
			
			try
			{
				performConnectionCheck();
			}
			catch(Exception e)
			{
				logWarning("Got exception while checking connections.", e);
			}
		}
	}
}
