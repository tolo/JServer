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
package com.teletalk.jserver.queue.legacy;

import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.JServerUtilities;
import com.teletalk.jserver.StatusTransitionException;
import com.teletalk.jserver.comm.EndPointIdentifier;
import com.teletalk.jserver.event.EventQueue;
import com.teletalk.jserver.event.StatusEvent;
import com.teletalk.jserver.event.StatusEventListener;
import com.teletalk.jserver.property.NumberProperty;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.queue.QueueManager;
import com.teletalk.jserver.queue.QueueSystemCollaborationManager;
import com.teletalk.jserver.queue.QueueSystemMetaData;
import com.teletalk.jserver.queue.RemoteQueueSystem;
import com.teletalk.jserver.queue.command.DefaultStatusCommand;
import com.teletalk.jserver.queue.command.QueueSystemCommand;
import com.teletalk.jserver.tcp.TcpCommunicationManager;
import com.teletalk.jserver.tcp.TcpEndPoint;
import com.teletalk.jserver.tcp.TcpEndPointIdentifier;
import com.teletalk.jserver.tcp.TcpServer;
import com.teletalk.jserver.util.MessageQueue;
import com.teletalk.jserver.util.MultiTimerListener;
import com.teletalk.jserver.util.Timer;

/**
 * The DefaultQueueSystemCollaborationManager class handles the network communication between queue systems. To accomplish this 
 * DefaultQueueSystemCollaborationManager inherits the class TcpCommunicationManager, which implements both client and server side 
 * tcp communication logic.<BR>
 * <BR>
 * Each connection to another queue system is symbolized by objects of the class <code>DefaultQueueSystemEndPointProxy</code>. An object of that 
 * class contains data about the remote queue system it symbolizes and, as the name indicates, is a proxy for an object of a class that 
 * handles the actual communication with the remote queue system - <code>DefaultQueueSystemEndPoint</code>.<BR>
 * <BR>
 * The DefaultQueueSystemCollaborationManager keeps track of all connections to other queue systems and regularly checks them for 
 * errors. In addition, it also regularly dispatches status messages, containing a information about this queue system, 
 * to other remote queue systems. The information that is dispatched is contained in a object of the class <code>QueueSystemMetaData</code>, 
 * which the DefaultQueueSystemCollaborationManager gets from the parent QueueManager using the method {@link QueueManager#getQueueSystemMetaData()}
 * 
 * @see DefaultQueueSystemEndPointProxy
 * @see DefaultQueueSystemEndPoint
 * @see QueueSystemMetaData
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1 
 */
public class DefaultQueueSystemCollaborationManager extends TcpCommunicationManager implements QueueSystemCollaborationManager, MultiTimerListener, StatusEventListener
{
	/** Constant for the default port used in communication between queuesystems. */
	public final static int DEFAULT_QUEUESYSTEM_COLLABORATION_PORT = 5829;
	
	/** Queue system metadata key for the status message interval setting of this DefaultQueueSystemCollaborationManager. */
	public final static String STATUSMESSAGE_INTERVAL_METADATA_KEY = "StatusMessageInterval";
	
	/** Queue system metadata key for the local ip address this DefaultQueueSystemCollaborationManager uses to listen for connections. */
	public final static String QUEUESYSTEM_ADDRESS_METADATA_KEY = "DefaultTcpQueueSystemAddress";
	
	/** Queue system metadata key for the local ip address this DefaultQueueSystemCollaborationManager uses to listen for connections. */
	public final static String SERVER_NAME_METADATA_KEY = "ServerName";
	
	/** A reference to the QueueManager. */
	private final QueueManager queueManager;
	
	private final HashMap queueSystemProxies;

	private final NumberProperty checkInteval;
	private final NumberProperty statusMessageInterval;
	
	//private TcpEndPointIdentifier address; //The address of this DefaultQueueSystemCollaborationManager, which actually is the address of the 
	
	private Timer statusMessageTimer;
	private Timer endPointCheckTimer;
	
	private int statusMessageTimerRestartCount = 0;
	private int endPointCheckTimerRestartCount = 0;
	
	private final MessageQueue commandQueue;
	
	private final TcpServer tcpServer;
	
	private final Object remoteQueueSystemLock = new Object();
      
		
	/**
	 * Creates a new DefaultQueueSystemCollaborationManager with a TcpServer bound to the default
	 * QueueManager port.
	 * 
	 * @param queueManager the parent of this DefaultQueueSystemCollaborationManager.
	 */
	public DefaultQueueSystemCollaborationManager(QueueManager queueManager)
	{
		this(queueManager, DEFAULT_QUEUESYSTEM_COLLABORATION_PORT);
	}
	
	/**
	 * Creates a new DefaultQueueSystemCollaborationManager with a TcpServer bound to the specified port.
	 * 
	 * @param queueManager the parent of this DefaultQueueSystemCollaborationManager.
	 * @param port the portnumber used to listen for connections from other queue systems.
	 */
	public DefaultQueueSystemCollaborationManager(QueueManager queueManager, int port)
	{
		this(queueManager, port, null);
	}
	
	/**
	 * Creates a new DefaultQueueSystemCollaborationManager with a TcpServer bound to the specified port.
	 * 
	 * @param queueManager the parent of this DefaultQueueSystemCollaborationManager.
	 * b@param port the portnumber used to listen for connections from other queue systems.
	 * @param localIP the local ip address that the TcpServer used by this DefaultQueueSystemCollaborationManager should be bound to.
	 */
	public DefaultQueueSystemCollaborationManager(QueueManager queueManager, int port, InetAddress localIP)
	{
		super(queueManager, "DefaultQueueSystemCollaborationManager", DefaultQueueSystemEndPoint.class); //, new TcpEndPointIdentifier(localIP, port));
		
		this.queueManager = queueManager;
		
		commandQueue = new MessageQueue();		
		
		queueSystemProxies = new HashMap();
   
		Object[] params = {this, "DefaultQueueSystemEndPoint"};
		super.setEndPointClassCreationParams(params);
		
		TcpEndPointIdentifier address = new TcpEndPointIdentifier(localIP, port);
		
		this.tcpServer = this.addTcpServer("TcpServer", address);
		this.tcpServer.initProperties();
		
		this.setPoolSize(5);  //Har du alla trådar i poolen? ;)
		
		checkInteval = new NumberProperty(this, "checkInterval", 2500, NumberProperty.MODIFIABLE_NO_RESTART);
		checkInteval.setDescription("The interval in milliseconds at which checks are performed on all connections to other queue systems.");
		statusMessageInterval = new NumberProperty(this, "statusMessageInterval", 5*1000, NumberProperty.MODIFIABLE_NO_RESTART);
		statusMessageInterval.setDescription("The interval in milliseconds at which statusmessages are dispatched to all remote queue systems connected to this queue system.");
				
		addProperty(checkInteval);
		addProperty(statusMessageInterval);
		
		queueManager.getQueueSystemMetaData().putExtraData(STATUSMESSAGE_INTERVAL_METADATA_KEY, statusMessageInterval.getValueAsNumber());
		queueManager.getQueueSystemMetaData().putExtraData(QUEUESYSTEM_ADDRESS_METADATA_KEY, address);
      
      JServer jserver = JServer.getJServer();
      String jServerName = "";
      if( jserver != null ) jServerName = jserver.getName();
      
		queueManager.getQueueSystemMetaData().putExtraData(SERVER_NAME_METADATA_KEY, jServerName);
	}
		
	/**
	 * Initialization method for this DefaultQueueSystemCollaborationManager.
	 */
	protected void doInitialize()
	{
		super.doInitialize();
      
      // Attempt to get old property "check interval(ms)" to be used if "check interval" property is missing
      super.initFromConfiguredProperty(this.checkInteval, "check interval(ms)", false, true);
      // Attempt to get old property "statusmessage interval" to be used if "statusmessage interval" property is missing
      super.initFromConfiguredProperty(this.statusMessageInterval, "statusmessage interval(ms)", false, true);
      
      EventQueue eventQueue = getEventQueue();
         
      if( eventQueue != null )
      {
         eventQueue.registerStatusEventListener(this);
      }
		
		statusMessageTimer = new Timer(this, statusMessageInterval.longValue(), this.getFullName() + ".StatusMessageTimer");
		endPointCheckTimer = new Timer(this, checkInteval.longValue(), this.getFullName() + ".EndPointCheckTimer");
		
		if( (tcpServer.getStatus() != ENABLED) && (tcpServer.getStatus() != INITIALIZING) )
		{
			tcpServer.engage();
		}
		try{
		this.tcpServer.waitForEnabled(30000); //Wait for the tcpserver to engage
		}catch(InterruptedException e){}
		
		if( tcpServer.getStatus() != ENABLED )
			throw new StatusTransitionException("Unable to start tcp server!");
		
		// Update metadata with address and port of tcp server, in case properties have changed
		TcpEndPointIdentifier address = new TcpEndPointIdentifier(tcpServer.getLocalInetAddress(), tcpServer.getLocalPort());
		queueManager.getQueueSystemMetaData().putExtraData(QUEUESYSTEM_ADDRESS_METADATA_KEY, address);
		queueManager.getQueueSystemMetaData().putExtraData(STATUSMESSAGE_INTERVAL_METADATA_KEY, statusMessageInterval.getValueAsNumber());
	}
	
	/**
	 * Shutdown method for this DefaultQueueSystemCollaborationManager.
	 */
	protected void doShutDown()
	{
		if(!isReinitializing())
		{
			//Disconnect existing proxies
			DefaultQueueSystemEndPointProxy proxy;
			final List endPointList = getRemoteQueueSystems();
		
			for(int i=0; i<endPointList.size(); i++)
			{
				proxy = (DefaultQueueSystemEndPointProxy)endPointList.get(i);
			
				if(proxy != null)
					proxy.destroy();
			}
		
			synchronized(this.remoteQueueSystemLock)
			{
				queueSystemProxies.clear();
			}
			
			if( (tcpServer.getStatus() != DOWN) && (tcpServer.getStatus() != SHUTTING_DOWN) && (tcpServer.getStatus() != CRITICAL_ERROR) )
			{
				tcpServer.shutDown();
			}
			try{
			tcpServer.waitForDown(2500);
			}catch(InterruptedException e){}
		}
		
		if( statusMessageTimer != null ) statusMessageTimer.kill();
		statusMessageTimer = null;
		if( endPointCheckTimer != null ) endPointCheckTimer.kill();
		endPointCheckTimer = null;
		
		super.doShutDown();
	}
	
	/**
	 * Called when the value of a property has been modified. This method checks the <code>statusMessageInterval</code>
	 * <code>checkInteval</code> properties have been modified and updated the appropriate timer.
	 * 
	 * @param property the Property that was changed.
	 */
	public void propertyModified(Property property)
	{
		if(getStatus() == ENABLED)
		{
			if(property == this.statusMessageInterval)
			{
				statusMessageTimer.setTimerTickInterval(statusMessageInterval.longValue());	
			}
			else if(property == this.checkInteval)
			{
				endPointCheckTimer.setTimerTickInterval(checkInteval.longValue());
			}
		}
				
		super.propertyModified(property);
	}
	
	/**
	 * Validates a modification of a property's value. This method validates various properties like 
	 * statusMessageInterval and checkInteval. 
	 *  
	 * @param property The property to be validated.
	 * 
	 * @return boolean value indicating if the property passed (true) validation or not (false).
	 * 
	 * @see com.teletalk.jserver.property.PropertyOwner
	 */
	public boolean validatePropertyModification(Property property)
	{
		if(property == statusMessageInterval)
			return statusMessageInterval.intValue() > 100;
		else if(property == checkInteval)
			return checkInteval.intValue() > 100;
		else return super.validatePropertyModification(property);
	}
   
   public void performStartupSynchronization(final List addresses)
   {
      // Synchronize queues with remote queue systems
      RemoteQueueSystem[] rqs = new RemoteQueueSystem[addresses.size()];
   
      // Create connections to all available queue systems (and synchronize queues)
      for(int i=0; i<addresses.size(); i++)
      {
         rqs[i] = this.getRemoteQueueSystem( (EndPointIdentifier)addresses.get(i) );
      }
   
      // Wait for links to be established
      for(int i=0; i<rqs.length; i++)
      {
         try
         {
            if( !rqs[i].waitForLinkEstablished( 20*60*1000 ) ) // Synchronization can take a very long time if the queues are large...
            {
               this.logWarning("Failed to establish link to queue system at " + rqs[i].getRemoteQueueSystemAddress() + " while performing queue check. Unable to synchronize queues at this time.");
            }
         }
         catch(InterruptedException ie)
         {
            this.logWarning("Got InterruptedException while waiting for link to be established to queue system at " + rqs[i].getRemoteQueueSystemAddress() + " while performing queue check. Unable to synchronize queues at this time.", ie);
         }
      }
   }
	
	/**
	 * Gets the TcpServer used by this QueueSystemCollaborationManager.
	 * 
	 * @return a TcpServer object.
	 * 
	 * @see com.teletalk.jserver.tcp.TcpServer
	 */
	public final TcpServer getTcpServer()
	{
		return tcpServer;	
	}
	
	/**
	 * Gets a reference to the QueueManager associated with this QueueSystemCollaborationManager.
	 * 
	 * @return a reference to the QueueManager associated with this QueueSystemCollaborationManager.
	 */
	public final QueueManager getQueueManager()
	{
		return this.queueManager;
	}
	
	/**
	 * Performs a status check om this DefaultQueueSystemCollaborationManager.
	 * 
	 * @return <true> if the status values of this subsystem and its associated tcp server are not CRITICAL_ERROR, otherwise <false>.
	 */
	public boolean performCheck()
	{
		return ((getStatus() != CRITICAL_ERROR) && (tcpServer.getStatus() != CRITICAL_ERROR));
	}
	
	/**
	 * Gets the address used by this DefaultQueueSystemCollaborationManager to listen for connections.
	 * 
	 * @return a TcpEndPointIdentifier object.
	 */
	public TcpEndPointIdentifier getAddress()
	{
		return tcpServer.getLocalAddress();
	}
	
	/**
	 * Gets a DefaultQueueSystemEndPointProxy object representing a remote queue system at the
	 * specified address.<br> If there currently is no is no DefaultQueueSystemEndPointProxy object available for the address a new one will be created.
	 * <br>
	 * This method returns <code>null</code> if this DefaultQueueSystemCollaborationManager isn't ENABLED.
	 * 
	 * @param address the address of the remote queue system (Must be an instance of TcpEndPointIdentifier or one of its subclasses).
	 *  
	 * @return a DefaultQueueSystemEndPointProxy object, <code>null</code> if the address was invalid or there was an error creating a DefaultQueueSystemEndPointProxy object.
	 */
	public RemoteQueueSystem getRemoteQueueSystem(EndPointIdentifier address) 
	{
		return getRemoteQueueSystem(address, true);
	}
		
	/**
	 * Gets a DefaultQueueSystemEndPointProxy object representing a remote queue system at the
	 * specified address.<br>
	 * <br>
	 * This method returns <code>null</code> if this DefaultQueueSystemCollaborationManager isn't ENABLED.
	 * 
	 * @param address the address of the remote queue system (Must be an instance of TcpEndPointIdentifier or one of its subclasses).
	 * @param create boolean flag indicating if a new DefaultQueueSystemEndPointProxy object will be created if there currently is none available for the specified address.
	 *  
	 * @return a DefaultQueueSystemEndPointProxy object, <code>null</code> if the address was invalid, if there was an error creating a DefaultQueueSystemEndPointProxy object 
	 * or if the parameter create was set to false and there were no DefaultQueueSystemEndPointProxy object matching the specified address.
	 */
	public RemoteQueueSystem getRemoteQueueSystem(EndPointIdentifier address, boolean create) 
	{
		if(this.getStatus() != ENABLED)
		{
			logWarning("Method getRemoteQueueSystem(" + address + ", " +create + ") called in illegal state (" + getStatusName() + ")!");
			return null;
		}
		
		if(address == null)
		{
			logWarning("Error while trying to get reference to remote queue system proxy (getRemoteQueueSystem), null address specified! Stack trace: " + JServerUtilities.getStackTrace());
			return null;
		}
		
		if(!(address instanceof TcpEndPointIdentifier))
		{
			logWarning("Error while trying to get reference to remote queue system proxy (getRemoteQueueSystem), invalid address specified (Expected TcpEndPointIdentifier, got " + address.getClass().getName() + ")!");
			return null;
		}
		
		DefaultQueueSystemEndPointProxy proxy = null;
		String identifier = address.getAddressAsString();
		
		synchronized(this.remoteQueueSystemLock)
		{
			proxy = (DefaultQueueSystemEndPointProxy)queueSystemProxies.get(identifier);

			if( (proxy == null) && create)
			{
				try
				{
					if(queueSystemProxies.containsKey(identifier))
					{
						proxy = (DefaultQueueSystemEndPointProxy)queueSystemProxies.get(identifier);
					}
					else
					{
						proxy = new DefaultQueueSystemEndPointProxy(this, (TcpEndPointIdentifier)address);
						registerRemoteQueueSystem(proxy);
					}
				}
				catch(Exception e)
				{
					logError("Failed to get remote queue for " + identifier + ".", e);
					proxy = null;
				}
			}
		}
		
		return proxy;
	}
	
	/**
	 * Gets a list of all remote queue systems connected to this queue system.
	 * 
	 * @return a list of DefaultQueueSystemEndPointProxy objects.
	 */
	public List getRemoteQueueSystems()
	{
		final List endPointList = new ArrayList();
		
		synchronized(this.remoteQueueSystemLock)
		{
			final Iterator iterator = queueSystemProxies.keySet().iterator();
			
			DefaultQueueSystemEndPointProxy proxy;
			String key;

			while(iterator.hasNext())
			{
				key = (String)iterator.next();
				if(key == null) continue;
				
				proxy = (DefaultQueueSystemEndPointProxy)queueSystemProxies.get(key);
				
				if(proxy != null)
					endPointList.add(proxy);
			}
		}
		
		return endPointList;
	}

	/**
	 * Initializes a new client side endpoint for communication with another queue system.
	 * 
	 * @param remoteAddress the address to create an endpoint for.
	 * @param proxy the DefaultQueueSystemEndPointProxy object to be associated with the endpoint.
	 * 
	 * @return a DefaultQueueSystemEndPoint object.
	 */
	protected DefaultQueueSystemEndPoint initClientSideQueueSystemEndPoint(TcpEndPointIdentifier remoteAddress, DefaultQueueSystemEndPointProxy proxy)
	{
		return (DefaultQueueSystemEndPoint)initializeClientSideTcpEndPoint(remoteAddress, proxy);	
	}

	/**
	 * Initializes a new server side endpoint for communication with another queue system.
	 * 
	 * @param socket a newly created server side socket object.
	 * @param proxy the DefaultQueueSystemEndPointProxy object to be associated with the endpoint.
	 * 
	 * @return a DefaultQueueSystemEndPoint object.
	 */
	protected DefaultQueueSystemEndPoint initServerSideQueueSystemEndPoint(final Socket socket, final TcpEndPointIdentifier acceptAddess, final DefaultQueueSystemEndPointProxy proxy)
	{
		return (DefaultQueueSystemEndPoint)super.initializeServerSideTcpEndPoint(socket, acceptAddess, proxy);
	}
	
	/**
	 * Method called when a new connection from another queue system is accepted. This method creates a new 
	 * DefaultQueueSystemEndPointProxy which in turn calls the method {@link #initServerSideQueueSystemEndPoint(Socket, TcpEndPointIdentifier, DefaultQueueSystemEndPointProxy)} 
	 * to create the actual endpoint object (DefaultQueueSystemEndPoint).
	 * 
	 * @param socket a newly created server side socket object.
	 * 
	 * @return a TcpEndPoint(DefaultQueueSystemEndPoint) object.
	 */
   protected TcpEndPoint initializeServerSideTcpEndPoint(final Socket socket, final TcpEndPointIdentifier acceptAddess, final Object customData)
	{
		try
		{
			return (new DefaultQueueSystemEndPointProxy(this, socket, acceptAddess)).getEndPoint();
		}
		catch(Exception e)
		{
			logError("Failed to create serverside endpoint for remote queue system at " + socket.getInetAddress() +  ".", e);
			return null;
		}
	}
	
	/**
	 * Method called when a QueueMessage is delivered to a remote queue system. This method simply relays the
	 * information to the parent(QueueManager).
	 * 
	 * @param command a message delivered to a remote QueueManager.
	 * @param success flag indicating if the message was successfully delivered or not.
	 */
	protected void commandDeliveryReport(QueueSystemCommand command, boolean success)
	{
		queueManager.commandDeliveryReport(command, success);
	}

	/**
	 * Called when a remote queue system has connected (serverside connection) to this queue system and is attempting to establish a link. 
	 * This method will detect and handle possible collisions between two connections between the same queue systems.
	 * 
	 * @return If a link was successfully established, an integer value > 0 will be returned.<br>
	 *	If there was a connection collision and the endpoint of the specified proxy won the "duel", an integer value of 0 (zero) will be retured.<br>
	 * If there was a connection collision and the endpoint of the specified proxy lost the "duel", an integer value < 0 will be returned.
	 */
	protected int remoteQueueSystemLinkRequest(DefaultQueueSystemEndPointProxy proxy)
	{
		synchronized(this.remoteQueueSystemLock)
		{
			String identifier = proxy.getRemoteQueueSystemAddress().getAddressAsString();
			DefaultQueueSystemEndPointProxy alreadyExistingProxy = (DefaultQueueSystemEndPointProxy)queueSystemProxies.get(identifier);
			if(isDebugMode()) logDebug("Request to establish link with remote queue system received.");

			if(alreadyExistingProxy == null)
			{
				registerRemoteQueueSystem(proxy);
				return 1;
			}
			else
			{
				synchronized(alreadyExistingProxy) // Acquire  a lock over the existing proxy (state lock)
				{
					if( alreadyExistingProxy.isDestroyed() ) //Check if it is destroyed and if so, unregister it and create a new one.
					{
						if(isDebugMode()) logDebug("Existing proxy found, but destroyed. Unregistering existing proxy.");
						unregisterRemoteQueueSystem(alreadyExistingProxy);
						alreadyExistingProxy = null;				

						registerRemoteQueueSystem(proxy);

						return 1;
					}
					else if(alreadyExistingProxy.isNegotiationCompleted())
					{
						logWarning("A valid link to " + alreadyExistingProxy.getRemoteQueueSystemName() + " already exists. Rejecting new connection!");
						return -1;	
					}
					else //Collision
					{
						return handleCollision(alreadyExistingProxy, proxy);
					}
				}
			}
		}
	}

	// Handles connection collision
	private int handleCollision(DefaultQueueSystemEndPointProxy alreadyExistingProxy, DefaultQueueSystemEndPointProxy proxy)
	{
		logInfo("Remote queue system connection collision - existing connection: " + alreadyExistingProxy + ", new connection: " + proxy + ".");

		if( (alreadyExistingProxy.getCurrentState() != DefaultQueueSystemEndPointProxy.NEGOTIATING) || // If already existing proxy isn't connected yet...
			 (alreadyExistingProxy.getCurrentState() != DefaultQueueSystemEndPointProxy.SYNCHRONIZING) ||
			 (alreadyExistingProxy.getCurrentState() != DefaultQueueSystemEndPointProxy.LINK_ESTABLISHED) ||
			 (alreadyExistingProxy.getEndPoint() == null) ||
			 (alreadyExistingProxy.getEndPoint().getMagicNumber() < proxy.getEndPoint().getMagicNumber()) ) // ...or if the endpoint that was initiated from this server has a lesser magic number, and loses the collision duel
		{
			alreadyExistingProxy.setEndPoint(proxy.getEndPoint());
			if(isDebugMode()) logDebug("Existing proxy lost the duel.");
			return 0;
		}
		else if(alreadyExistingProxy.getEndPoint().getMagicNumber() > proxy.getEndPoint().getMagicNumber())  //The endpoint that was initiated from this server has a greater magic number, and wins the collison duel
		{
			if(isDebugMode()) logDebug("Existing proxy won the duel.");
			return -1; //Return null because the connecting endpoint lost the collision duel
		}
		else //This is extremely unlikely, but still...
		{
			int waitTime = (int)(1234 * Math.random());
				
			try{Thread.sleep(waitTime);
			}catch(InterruptedException e){}
				
			if(alreadyExistingProxy.isNegotiationCompleted())
				return -1;	
			else
			{
				if(alreadyExistingProxy.check())
				{
					alreadyExistingProxy.setEndPoint(proxy.getEndPoint());
					return 0;
				}
				else
				{
					alreadyExistingProxy.destroy();
					unregisterRemoteQueueSystem(alreadyExistingProxy);
					alreadyExistingProxy = null;				

					registerRemoteQueueSystem(proxy);
							
					return 1;
				}
			}
		}
	}
	
	/**
	 * Registers a new DefaultQueueSystemEndPointProxy with this DefaultQueueSystemCollaborationManager.
	 * 
	 * @param proxy a DefaultQueueSystemEndPointProxy object to be registered.
	 */
	protected void registerRemoteQueueSystem(DefaultQueueSystemEndPointProxy proxy)
	{
		synchronized(this.remoteQueueSystemLock)
		{
			String identifier = proxy.getRemoteQueueSystemAddress().getAddressAsString();

			if(!queueSystemProxies.containsKey(identifier))
			{
				queueSystemProxies.put(identifier, proxy);
			}
		}
	}
	
	/**
	 * Unregisters a DefaultQueueSystemEndPointProxy with this DefaultQueueSystemCollaborationManager. If there are any unsent messages in the MessageQueue of the 
	 * DefaultQueueSystemEndPointProxy, they will be cancelled. 
	 * 
	 * @param proxy a DefaultQueueSystemEndPointProxy object to be unregistered.
	 */
	protected void unregisterRemoteQueueSystem(DefaultQueueSystemEndPointProxy proxy)
	{
		synchronized(this.remoteQueueSystemLock)
		{
			String identifier = proxy.getRemoteQueueSystemAddress().getAddressAsString();
	
			queueSystemProxies.remove(identifier);
		}
	}

	/**
	 * Gets the interval in milliseconds for sending status messages to all connected remote QueueSystems.
	 * 
	 * @return the status message interval in milliseconds.
	 */
	public long getStatusMessageInterval()
	{
		return statusMessageInterval.longValue();	
	}
	
	/**
	 * Gets the metadata object associated with the queuesystem. This is a convenience method that 
	 * asks the QueueManager for the meta data object.
	 * 
	 * @return a QueueSystemMetaData object.
	 */
	protected QueueSystemMetaData getQueueSystemMetaData()
	{
		return queueManager.getQueueSystemMetaData();
	}
   
   /**
    * Method to dispatch a command to a remote queue system at the address specified in the command.
    * 
    * @param command the command to dispatch.
    * 
    * @since 2.1 (20051221)
    */
   public void dispatchCommand(QueueSystemCommand command)
   {
      RemoteQueueSystem remoteQueueSystem = this.getRemoteQueueSystem(command.getAddress());
      
      if(remoteQueueSystem != null)
      {
         ((DefaultQueueSystemEndPointProxy)remoteQueueSystem).dispatchCommand(command);
      }
      else
      {
         logError("Unable to dispatch command (" + command + ")! Couldn't get reference to remote queue system!");
      }
   }
	
	/**
	 * Called to handle an incoming command from a remote queue system. This method places the command in the  
	 * messagequeue of this DefaultQueueSystemCollaborationManager.
	 * 
	 * @param command a command to be handled.
	 */
	public void handleQueueSystemCommand(QueueSystemCommand command)
	{
		commandQueue.putMsg(command);
	}
	
	/**
	 * Notification of a status modificaion.
	 * 
	 * @param e a StatusEvent object.
	 */
	public void statusChanged(StatusEvent e)
	{
		if(e.getSourceComponent() == this.tcpServer)
		{
			// If the tcp server entered status ENABLED...
			if(e.getStatus() == ENABLED)
			{
				TcpEndPointIdentifier address = new TcpEndPointIdentifier(tcpServer.getLocalInetAddress(), tcpServer.getLocalPort());
				QueueSystemMetaData qSystemMetaData = getQueueSystemMetaData();

				qSystemMetaData.putExtraData(QUEUESYSTEM_ADDRESS_METADATA_KEY, address); // ...update the address field of the metadata object of this queue system
			}
		}
	}
	
	/**
	 * Called when a timer associated with this DefaultQueueSystemCollaborationManager "ticks".
	 * 
	 * @param t the timer that "ticked".
	 */
	public void timerTick(Timer t)
	{
		if(t == this.statusMessageTimer)
		{
			dispatchStatusCommand();
		}
		else if(t == this.endPointCheckTimer)
		{
			performEndPointCheck();
		}
	}
	
	//Dispatches a status command to all queue systems connected to this.
	private void dispatchStatusCommand()
	{
		QueueSystemMetaData qSystemMetaData = getQueueSystemMetaData();
		
		final DefaultStatusCommand statusCommand = new DefaultStatusCommand((QueueSystemMetaData)qSystemMetaData.clone());
		
		final List endPointList = getRemoteQueueSystems();
		DefaultQueueSystemEndPointProxy proxy;

		for(int i=0; i<endPointList.size(); i++)
		{
			proxy = (DefaultQueueSystemEndPointProxy)endPointList.get(i);
				
			if(proxy != null)
			{
				if(isDebugMode()) logDebug("Dispatching status command (" + qSystemMetaData.toString() + ") to " + proxy.toString() + ".");
				proxy.sendStatusCommand(statusCommand);
			}
		}
	}
	
	//Performs a check on all endpoints
	private void performEndPointCheck()
	{
		final List endPointList = getRemoteQueueSystems();
		DefaultQueueSystemEndPointProxy proxy;
		
		for(int i=0; i<endPointList.size(); i++)
		{
			proxy = (DefaultQueueSystemEndPointProxy)endPointList.get(i);
			
			if(proxy != null)
			{
				if(proxy.isDestroyed())
				{
					// If a proxy failed to reconnect (and consequently is destroyed) it will be blocked a period of time,  before it is 
					// finally unregistered and it is again possible to try to reconnect to the address of the proxy.
					if((System.currentTimeMillis() - proxy.getLastConnectAttemptTime()) > 30*1000) //postConnectionFailureLingerTime
					{
						logInfo("Unregistering " + proxy + "!");
						unregisterRemoteQueueSystem(proxy);
						proxy.cancelAllCommands();  //Cancel any commands in the message queue of the proxy...if any (there shouldn't be any)
					}
				}
				else if(!proxy.check())
				{
					logInfo(proxy + " failed check! Trying to reconnect!");
					//Try to reconnect...
					if(!proxy.reConnect())
					{
						logInfo("Reconnection of " + proxy + " not possible!");
						proxy.destroy();

						queueManager.linkToRemoteQueueSystemLost(proxy);  //Notify the controller that the link is lost, and cancel pending commands
					}
				}
				else //...if the check was successful
				{
					if(!proxy.statusMessageCheck())
					{
						//Do nothing with the status message check for now...
					}
					
					DefaultQueueSystemEndPoint ep = proxy.getEndPoint();
					if(ep != null) 
					{	
						try
						{
							ep.resetObjectOutputStream();
						}
						catch(Exception e){	}
					}
				}
			}
		}
	}

	//Performs a check on the timers 
	private boolean performTimerCheck()
	{
		if(!statusMessageTimer.check())
		{
			statusMessageTimer.restart();
			statusMessageTimerRestartCount++;
			
			if(statusMessageTimerRestartCount >= 5)
			{
				error("Error in status message timer!");
				return false;
			}
		}
		else
		{
			if(statusMessageTimerRestartCount > 0)
				statusMessageTimerRestartCount = 0;
		}
		
		if(!endPointCheckTimer.check())
		{
			endPointCheckTimer.restart();
			endPointCheckTimerRestartCount++;
			
			if(endPointCheckTimerRestartCount >= 5)
			{
				error("Error in endpoint checker timer!");
				return false;
			}
		}
		else
		{
			if(endPointCheckTimerRestartCount > 0)
				endPointCheckTimerRestartCount = 0;
		}
		
		return true;
	}
	
	/**
	 * The thread method of this DefaultQueueSystemCollaborationManager. This method regularly dispathces status messages, containing a metadata object with information about this queue system, 
	 * to other remote queue systems and performs error checking on RemoteQueueSystemProxies.
	 * 
	 * @see QueueSystemMetaData
	 */
	public void run()
	{
		while(canRun)
		{
			try
			{
				if(this.queueManager.waitForEnabled(60*60*1000))
					break; // Success
				else
				{
					logError("Timeout occurred while waiting for QueueManager become enabled.");
					return; // Failure
				}
			}
			catch(InterruptedException ie){}
		}
		
		QueueSystemCommand command;
		long lastCheck = System.currentTimeMillis();
		final long timerCheckInterval = 30*1000;
		long remainingWaitTime = timerCheckInterval;
			
		while(canRun)
		{
			try
			{
				commandQueue.waitForData(remainingWaitTime);
				command = (QueueSystemCommand)commandQueue.getMsgIfAny(); //Get a new incomming command from the queue
				
				if(command != null)
				{
					try
					{
						command.execute(queueManager);
					}
					catch(Exception e)
					{
						logError("Got exception while executing command.", e);
					}
				}
				
				if((System.currentTimeMillis() - lastCheck) > remainingWaitTime)
				{
					if(!performTimerCheck())
					{
						break;	
					}
						
					remainingWaitTime = timerCheckInterval;
					lastCheck = System.currentTimeMillis() - 33;
				}
				else
				{
					remainingWaitTime = timerCheckInterval - (System.currentTimeMillis() - lastCheck);
					if(remainingWaitTime <= 0) remainingWaitTime = 1;
				}
			}
			catch(InterruptedException ie)
			{
				if(!canRun)
					break;
			}
		}
	}
}
