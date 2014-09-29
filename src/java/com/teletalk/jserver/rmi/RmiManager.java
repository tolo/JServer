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
package com.teletalk.jserver.rmi;

import java.net.URL;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.registry.Registry;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.JServerConstants;
import com.teletalk.jserver.StatusTransitionException;
import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.event.Event;
import com.teletalk.jserver.event.EventListener;
import com.teletalk.jserver.event.EventQueue;
import com.teletalk.jserver.property.BooleanProperty;
import com.teletalk.jserver.property.NumberProperty;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.property.StringProperty;
import com.teletalk.jserver.rmi.remote.RemoteEvent;
import com.teletalk.jserver.rmi.remote.RemoteRmiClient;
import com.teletalk.jserver.tcp.TcpEndPointIdentifier;
import com.teletalk.jserver.tcp.TcpEndPointIdentifierProperty;
import com.teletalk.jserver.tcp.http.HttpServer;

/**
 * The class RmiManager is a SubSystem responsible for managing RMI communication. It does this with
 * the help of the class <tt>JServerRmiHost</tt>, which is the actual remote object that clients look up
 * when the wish to communicate with the server.<br>
 * <br>
 * This class makes sure that RMI clients connected to the server gets notified of events in it, such as
 * logevents and propertyevents.<br>
 * <br>
 * When creating an RmiManager it is imperative that the method {@link #initRmiProperties()} is called as soon as posible to 
 * initialize important JServer and system properties. The method must however be called after the PropertyManager has 
 * loaded properties from persistent storage.
 * 
 * @see com.teletalk.jserver.SubSystem
 * @see JServerRmiHost
 * @see com.teletalk.jserver.rmi.remote.RemotePropertyEvent
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class RmiManager extends SubSystem implements EventListener
{
	private static final String rmiCodeBaseURLDelim = " ";
	
	private final JServer jServer;
	
	private final List rmiHosts;
	
	private final NumberProperty checkInterval;
	private final TcpEndPointIdentifierProperty registryAddresses;
	private final StringProperty rmiCodeBase;
	private final BooleanProperty rmiMethodCallLoggingEnabled;
	private final TcpEndPointIdentifierProperty exportAddresses;
	
	private boolean rmiPropertiesInitialized = false;  //Flag indicating if the java.rmi.server.codebase system property has been set
	
	//private RMIServerSocketFactory[] customRmiSocketFactories = null;
   
   /**
    * Constructs a new RmiManager. Sets interval in milliseconds between checks to 5 seconds.
    * 
    * @param parent the JServer parent of this RmiManager.
    */
   public RmiManager(JServer parent)
   {
      this(parent, "RmiManager");
   }
		
	/**
	 * Constructs a new RmiManager. Sets interval in milliseconds between checks to 5 seconds.
	 * 
	 * @param parent the JServer parent of this RmiManager.
	 */
	public RmiManager(JServer parent, String name)
	{
		this(parent, 5*1000);
	}
   
   /**
    * Constructs a new RmiManager.
    * 
    * @param parent the JServer parent of this RmiManager.
    * @param checkInterval interval in milliseconds between checks.
    */
   public RmiManager(JServer parent, long checkInterval)
   {
      this(parent, "RmiManager", checkInterval);
   }
	
	/**
	 * Constructs a new RmiManager.
	 * 
	 * @param parent the JServer parent of this RmiManager.
	 * @param checkInterval interval in milliseconds between checks.
	 */
	public RmiManager(JServer parent, String name, long checkInterval)
	{
		super(parent, name, true);
		
		this.jServer = parent;
		
		this.rmiHosts = new ArrayList();
		
		/*if(System.getSecurityManager() == null)
		{
			System.setSecurityManager(new com.teletalk.jserver.util.AFreeWorld());
		}*/
		
		String rmiCodeBaseString = System.getProperty("java.rmi.server.codebase");
		if(rmiCodeBaseString == null) rmiCodeBaseString = "";
				
		this.checkInterval = new NumberProperty(this, "checkInterval", checkInterval, Property.MODIFIABLE_NO_RESTART);
		this.checkInterval.setDescription("The interval in milliseconds at which this RmiManager should perform various checks to determine the status of, among other things, the connection to the Rmi registry.");
		this.registryAddresses = new TcpEndPointIdentifierProperty(this, "registryAddresses", TcpEndPointIdentifier.anyLocalHostString + ":" + Registry.REGISTRY_PORT, Property.MODIFIABLE_NO_RESTART);
		this.registryAddresses.setDescription("Addresses (host:port) to RMI registries to which this RmiManager should connect and register the server.");
		this.rmiCodeBase = new StringProperty(this, "rmiCodeBase", rmiCodeBaseString, Property.NOT_MODIFIABLE, true);//, Property.MODIFIABLE_NO_RESTART);
		this.rmiCodeBase.setDescription("The value for the system property java.rmi.server.codebase, used when loading resources like custom administration panels from this server via RMI. " + 
																		"If multiple codebase urls are to be specified, they must be delimited by spaces. " + 
																		"If a codebase url denotes a directory it must have a trailing '/'.");
		this.rmiMethodCallLoggingEnabled = new BooleanProperty(this, "rmiMethodCallLoggingEnabled", false, Property.MODIFIABLE_NO_RESTART);
		this.rmiMethodCallLoggingEnabled.setDescription("Flag indicating if rmi method calls should be logged. If this flag is set to true all method calls in "+
																		" the rmi adapter classes (package com.teletalk.jserver.rmi.adapter) will be logged.");
		this.exportAddresses = new TcpEndPointIdentifierProperty(this, "rmiExportAddresses", "", Property.NOT_MODIFIABLE);
		this.exportAddresses.setDescription("The local addresses on which RMI objects (such as subsystem RMI adapter classes etc.) should be exported (listen) on. Note that only one address is possible at the moment.");
		
		addProperty(this.checkInterval);
		addProperty(this.registryAddresses);
		addProperty(this.rmiCodeBase);
		addProperty(this.rmiMethodCallLoggingEnabled);
		addProperty(this.exportAddresses);
	}
	
	/**
	 * Initializes the RMI properties for RMI codebase and hostname and creates the custom socket factories used to export 
	 * remote objects on the configured addresses.
	 */
	public synchronized void initRmiProperties()
	{
		if(!rmiPropertiesInitialized)
		{
			this.initProperties();  //Read properties from persistent storage
         
         // Attempt to get old property "check interval"
         super.initFromConfiguredProperty(this.checkInterval, "check interval", false, true);
         // Attempt to get old property "registry addresses"
         super.initFromConfiguredProperty(this.registryAddresses, "registry addresses", false, true);
         // Attempt to get old property "rmi codebase"
         super.initFromConfiguredProperty(this.rmiCodeBase, "rmi codebase", false, true);
         // Attempt to get old property "rmi method call logging enabled"
         super.initFromConfiguredProperty(this.rmiMethodCallLoggingEnabled, "rmi method call logging enabled", false, true);
         // Attempt to get old property "rmi export addresses"
         super.initFromConfiguredProperty(this.exportAddresses, "rmi export addresses", false, true);
			
			this.initRmiCodeBase();  // Init rmi codebase
						
			this.initRmiExportAddresses(); // Init export address and rmi host name
						
			rmiPropertiesInitialized = true;
		}
	}
	
	/**
	 * Initializes the RMI codebase property.
	 */
	private void initRmiCodeBase()
	{
		//Create string containing codebase information from the property rmiCodeBase AND the syststem java.rmi.server.codebase property.
		String rmiCodeBaseString = rmiCodeBase.stringValue();
		String javaRmiCodeBaseSystemProperty = System.getProperty("java.rmi.server.codebase");
					
		if(rmiCodeBaseString == null) rmiCodeBaseString = "";
					
		//List for code base urls
		List rmiCodeBasePropertyList = new ArrayList();
					
		StringTokenizer tokenizer;
		String codeBaseUrl;
														
		//Parse urls from the rmiCodeBase property of this RmiManager
		tokenizer = new StringTokenizer(rmiCodeBaseString, rmiCodeBaseURLDelim, false);
							
		try
		{
			while(tokenizer.hasMoreTokens())
			{
				rmiCodeBasePropertyList.add(tokenizer.nextToken().trim());
			}
		}
		catch(Exception e)
		{
			logWarning("Error while parsing the rmiCodeBase property: " + e + ".");
		}
															
		//Check if the system property java.rmi.server.codebase has been set
		if( (javaRmiCodeBaseSystemProperty != null) && !javaRmiCodeBaseSystemProperty.equals("") )
		{
			List javaRmiCodeBaseSystemPropertyList = new ArrayList();	
							
			//Parse java.rmi.server.codebase urls
			tokenizer = new StringTokenizer(javaRmiCodeBaseSystemProperty, rmiCodeBaseURLDelim, false);
							
			try
			{
				while(tokenizer.hasMoreTokens())
				{
					javaRmiCodeBaseSystemPropertyList.add(tokenizer.nextToken().trim());
				}
			}
			catch(Exception e)
			{
				logWarning("Error while parsing java.rmi.server.codebase system property: " + e + ".");
			}
							
			//Add urls from java.rmi.server.codebase that doesn't exist in the rmiCodeBase property of this RmiManager
			for(int i=0; i<javaRmiCodeBaseSystemPropertyList.size(); i++)
			{
				codeBaseUrl = (String)javaRmiCodeBaseSystemPropertyList.get(i);
				if(!rmiCodeBasePropertyList.contains(codeBaseUrl)) rmiCodeBasePropertyList.add(codeBaseUrl);
			}
		}
		
		HttpServer http = jServer.getMainHttpServer();
		
		//If no addresses have been specified in either the rmiCodeBase property or the java.server.rmi.codebase system propery, 
		//then add the address of the main http server of this server
		if( (rmiCodeBasePropertyList.size() == 0) && (http != null) )
		{
			rmiCodeBasePropertyList.add("http://" + http.getLocalAddress() + "/" );
		}
					
		StringBuffer rmiCodeBaseBuffer = new StringBuffer();
					
		//Construct a new codebase string from the elements in rmiCodeBasePropertyList and validate the code base urls
		for(int i=0; i<rmiCodeBasePropertyList.size(); i++)
		{
			codeBaseUrl = (String)rmiCodeBasePropertyList.get(i);

			if( (codeBaseUrl == null) || (codeBaseUrl.equals("")) ) continue;
								
			try
			{
				new URL(codeBaseUrl);
			}
			catch(Exception e)
			{
				logWarning("Invalid url in rmi codebase property: " +codeBaseUrl + ". Codebase url will not be added to rmi codebase property. Exception is: " + e + ".");
				continue;
			}
						
			if(i > 0) rmiCodeBaseBuffer.append(rmiCodeBaseURLDelim);
																
			rmiCodeBaseBuffer.append(codeBaseUrl);
		}
					
		rmiCodeBase.setNotificationMode(false);
		rmiCodeBase.setForceMode(true);
		rmiCodeBase.setValue(rmiCodeBaseBuffer.toString());
		rmiCodeBase.setForceMode(false);
		rmiCodeBase.setNotificationMode(true);
		
		// Prevent new setting from beeing saved
		rmiCodeBase.setDefaultValue(rmiCodeBase.getValueAsObject());
				
		//Set the java.rmi.server.codebase once and for all...
		System.setProperty("java.rmi.server.codebase", rmiCodeBase.stringValue());
		logInfo("java.rmi.server.codebase property is set to '" + System.getProperty("java.rmi.server.codebase") + "'.");
	}
	
	/**
	 * Initializes the RMI export address (only one for now) and the java.rmi.server.hostname system property.
	 */
	private void initRmiExportAddresses()
	{
		String rmiHostName = null;
			
		// Attempt to get old property "rmi server hostname" to be used if export addresses property is missing
		Property rmiServerHostName = super.getConfiguredProperty("rmi server hostname");
			
		if(rmiServerHostName != null) 
		{
			rmiHostName = rmiServerHostName.getValueAsString();
			if( (rmiHostName != null) && (rmiHostName.trim().length() == 0) ) rmiHostName = null;
		}

		if( this.exportAddresses.size() == 0 )
		{
			this.exportAddresses.setForceMode(true);
			
			if( rmiHostName != null ) this.exportAddresses.addAddress(new TcpEndPointIdentifier(rmiHostName, 0));
			else this.exportAddresses.addAddress(new TcpEndPointIdentifier(TcpEndPointIdentifier.anyLocalHostString, 0));
			
			this.exportAddresses.setForceMode(false);
			
			// Prevent auto assigned value from beeing saved
			this.exportAddresses.setDefaultValue(this.exportAddresses.getValues());
		}
		else if( this.exportAddresses.size() > 1 )
		{
			logWarning("Only one RMI export address is possible at the moment!");
			
			this.exportAddresses.setForceMode(true);
			
			final TcpEndPointIdentifier firstAddress = this.exportAddresses.getAddress(0);
			this.exportAddresses.setValue(new TcpEndPointIdentifier[]{firstAddress});
			
			this.exportAddresses.setForceMode(false);
		}

		logInfo("RMI export address is set to " + this.exportAddresses.get(0) + ".");
		
		// Set java.rmi.server.hostname system property to first (and only) export address
		final TcpEndPointIdentifier firstAddress = this.exportAddresses.getAddress(0);
		if( (firstAddress.getAddress() != null) &&
			 (!firstAddress.getAddress().trim().equalsIgnoreCase(TcpEndPointIdentifier.localHostString)) &&
			 (!firstAddress.getAddress().trim().equalsIgnoreCase(TcpEndPointIdentifier.localHostAddressString)) )
		{
			System.setProperty("java.rmi.server.hostname", firstAddress.getAddress());
			logInfo("java.rmi.server.hostname is set to " + firstAddress.getAddress() + ".");
		}
	}
			
	/**
	 * Gets the local adresses on which RMI objects are to receive incoming calls (be exported) on.
	 * The returned addresses are those specified by the addresses specified by the property
	 * <code>rmi export addresses</code> in this subsystem. <br>
	 * <br>
	 * This method will only return one single address for now, since the current RMI implementation 
	 * only allows an object to be exported on one single address (UnicastRemoteObject).
	 * 
	 * @return an array of local addresses.
	 * 
	 * @since 1.13 build 600.
	 */
	public TcpEndPointIdentifier[] getExportAddresses()
	{
		return this.exportAddresses.getAddresses();
	}
   
   /**
    * Gets a reference to the RmiManager of this server.
    */
   public static RmiManager getRmiManager()
   {
      JServer jServer = JServer.getJServer();
      RmiManager rmiManager = null;
      
      if( jServer != null ) 
      {
         rmiManager = jServer.getRmiManager();
      }
      
      if( rmiManager == null ) 
      {
         rmiManager = new RmiManager(null);
      }
      
      return rmiManager;
   }

	/**
	 * Engages this RmiManager.
	 */
	protected void doInitialize()
	{
		try
		{
			super.doInitialize();

			if(!this.isReinitializing())
			{
				initRmiProperties(); //Initialize rmi properties (if they haven't already been initialized)

				JServerRmiHost rmiHost;
				TcpEndPointIdentifier[] addresses = registryAddresses.getAddresses();
				
				// If no local registry addresses are specified...
				if(addresses.length == 0)
				{
					// ...add the default
					final TcpEndPointIdentifier defaultRegistryAddress = new TcpEndPointIdentifier(TcpEndPointIdentifier.anyLocalHostString, Registry.REGISTRY_PORT);
					
					registryAddresses.setNotificationMode(false);
					registryAddresses.setForceMode(true);
					registryAddresses.addAddress(defaultRegistryAddress);
					registryAddresses.setForceMode(false);
					registryAddresses.setNotificationMode(true);
					// Set defaultRegistryAddress as default value, to prevent it from beeing saved
					registryAddresses.setDefaultValue(defaultRegistryAddress);
					
					addresses = registryAddresses.getAddresses();
					
					logWarning("No local registry found! Registry at " + defaultRegistryAddress.toString() + " added.");
				}
				
				boolean atLeastOneRmiHostFailed = false;
				
				// Create rmi host object (registries)
				for(int i=0; i<addresses.length; i++)
				{
					try
					{
						rmiHost = new JServerRmiHost(this, addresses[i]);
						
						synchronized(rmiHosts)
						{
							rmiHosts.add(rmiHost);
						}
						
						if( !rmiHost.initialize() ) atLeastOneRmiHostFailed = true;
					}
					catch(Exception e)
					{
						logWarning("Unable to create JServerRmiHost object for address " + addresses[i] + ". Exception is : " + e.toString() + ".");
					}
				}
				
				// If more that one registry address is used and creation of at least on rmi host failed...
				if( (addresses.length > 1) && atLeastOneRmiHostFailed )
				{
					logWarning("Failed to create at least one RMI host. If a JVM with a version less than 1.4 is used, mutiple RMI hosts (registries) are not supported.");
				}
						
				//LogManager l = getLogManager();
				EventQueue eq = getEventQueue();
			
				if(eq != null)
				{
					eq.registerEventListener(this);
				}
			
				/*if(l != null)
				{
					if(rmiLogger == null) rmiLogger = new RmiLogger(this, l);
					
					rmiLogger.enable();
					l.attachLogger(rmiLogger);
				}*/
			}
		}
		catch(Exception e)
		{
			throw new StatusTransitionException("Error while engaging", e);
		}
	}
			
	/**
	 * Shuts down this RmiManager.
	 */
	protected void doShutDown()
	{
		super.doShutDown();
		
		if(!this.isReinitializing())
		{
			EventQueue eq = getEventQueue();
			//LogManager l = getLogManager();

			if(eq != null) eq.unregisterEventListener(this);
					
			/*if(l != null && rmiLogger != null)
			{
				l.detachLogger(rmiLogger);
				rmiLogger = null;
			}*/
			
			JServerRmiHost rmiHost;
			
			synchronized(rmiHosts)
			{
				for(int i=0; i<rmiHosts.size(); i++)
				{
					rmiHost = (JServerRmiHost)rmiHosts.get(i);
					rmiHost.destroy();
					try
					{
						java.rmi.server.UnicastRemoteObject.unexportObject(rmiHost, true);
					}catch(Exception e){}
				}
				
				rmiHosts.clear();
			}
		}
	}
	
	/**
	 * Called when the value of a property owned by this RmiManager has been modified. 
	 * 
	 * @param property the Property that was changed.
	 */
	public void propertyModified(Property property)
	{
		if(property == this.registryAddresses)
		{
			List addresses = registryAddresses.getAddressList();
			JServerRmiHost rmiHost;
			ArrayList rmiHostsCopy;
			
			synchronized(rmiHosts)
			{
				rmiHostsCopy = new ArrayList(rmiHosts);

				//Remove old
				for(int i=0; i<rmiHostsCopy.size(); i++)
				{
					rmiHost = (JServerRmiHost)rmiHostsCopy.get(i);
						
					if( (rmiHost != null) && !addresses.contains( rmiHost.getAddress() ))
					{
						logInfo("Removing JServerRmiHost object for address " + rmiHost.getAddress() + ".");

						rmiHosts.remove(rmiHost);
							
						rmiHost.destroy();
						rmiHost = null;
					}
				}
			
				//Add new
				for(int i=0; i<addresses.size(); i++)
				{
					try
					{
						rmiHost = new JServerRmiHost(this, (TcpEndPointIdentifier)addresses.get(i));

						rmiHosts.add(rmiHost);
						
						rmiHost.initialize();
					}
					catch(Exception e)
					{
						logWarning("Unable to create JServerRmiHost object for address " +addresses.get(i) + ". Exception is : " + e.toString() + ".");
					}
				}
			}
		}
						
		super.propertyModified(property);
	}
	
	/**
	 * Validates a modification of a property's value. 
	 * 
	 * @param property The property to be validated.
	 * 
	 * @return boolean value indicating if the property passed (true) validation or not (false).
	 * 
	 * @see com.teletalk.jserver.property.PropertyOwner
	 */
	public boolean validatePropertyModification(final Property property)
	{
		if(property == this.checkInterval) return this.checkInterval.intValue() > 100;
		else 
		{
			return super.validatePropertyModification(property);
		}
	}
	
	/**
	 * Checks if rmi method calls should be logged.
	 * 
	 * @return <code>true</code> if rmi method calls should be logged, otherwise <code>false</code>.
	 */
	public boolean isRmiMethodCallLoggingEnabled()
	{
		return rmiMethodCallLoggingEnabled.booleanValue();
	}
	
	/**
	 * Gets a list of all rmi clients connected to this server.
	 * 
	 * @return a list of RmiClient objects.
	 */
	public List getAllClients()
	{
		ArrayList rmiHostsCopy;
		ArrayList allClients = new ArrayList();
		JServerRmiHost rmiHost;
			
		synchronized(rmiHosts)
		{
			rmiHostsCopy = new ArrayList(rmiHosts);
		}
			
		for(int i=0; i<rmiHostsCopy.size(); i++)
		{
			rmiHost = (JServerRmiHost)rmiHostsCopy.get(i);
					
			if(rmiHost != null)
			{
				allClients.addAll(rmiHost.getClients());
			}
		}
		
		return allClients;
	}
	
	/**
	 * Checks if there currently are any remote rmi clients connected to this server.
	 * 
	 * @return <code>true</code> if there currently are any remote rmi clients connected to this server, otherwise <code>false</code>.
	 */
	public boolean isRmiClientConnected()
	{
		return (getAllClients().size() > 0);
	}
	
	/**
	 * Called when a new LogMessage is received by the LogManager.
	 * 
	 * @param logMsg a LogMessage.
	 * 
	 * @see com.teletalk.jserver.log.LogManager
	 * @see com.teletalk.jserver.log.LogMessage
	 */
	/*public void newLogMessage(final LogMessage logMsg)
	{
		if( (rmiHosts.size() > 0) && (this.getStatus() == ENABLED) )
		{
			sendEventToClients(new RemoteLogEvent(logMsg));
		}
	}*/
	
	/**
	 * Event listener method that dispatches remote events to connected RMI clients.
	 * 
	 * @param e an event that has occurred.
	 */
	public void handleEvent(final Event e)
	{
		if( (rmiHosts.size() > 0) && (this.getStatus() == ENABLED) )
		{
			this.sendEventToClients(e.createRemoteEvent());
		}
	}
	
	/**
	 * Sends an event to all RMI clients connected to this server.
	 * 
	 * @param remoteEvent the event that is to be sent to clients.
	 */
	public void sendEventToClients(RemoteEvent remoteEvent)
	{
		ArrayList rmiHostsCopy;
		JServerRmiHost rmiHost;
		
		if( (remoteEvent != null) && (rmiHosts.size() > 0) )
		{
			RemoteRmiClient client = null;
		
			synchronized(rmiHosts)
			{
				rmiHostsCopy = new ArrayList(rmiHosts);
			}
			
			for(int q=0; q<rmiHostsCopy.size(); q++)
			{
				rmiHost = (JServerRmiHost)rmiHostsCopy.get(q);
				if(rmiHost == null) continue;
				
				List clients = rmiHost.getClients();
				
				for(int i=0; i<clients.size(); i++)
				{
					try
					{
						client = (RemoteRmiClient)clients.get(i);
						
						if(client != null)
						{
							client.receiveEvent(remoteEvent);
						}
					}
					catch(Exception e)
					{
                  // This should not happen under normal cicumstances since RemoteRmiClientProxy is used!
					   logError("Got exception while sending event (" + remoteEvent.toString() + ") to client..", e);
					}
				}
			}
		}
		
		remoteEvent = null;
	}
	
	/**
	 * The thread method of this RmiManager. With a given interval it run a number of checkes like for
	 * instance checking to see if all the RMI clients connected to the server are alive.
	 */
	public void run()
	{
		int cyclesWithoutRmiHosts = 0;
		long firstErrorTime = 0;
		boolean criticalErrorSignalled = false;
		
		while(canRun)
		{
			try
			{
				Thread.sleep(checkInterval.intValue());
			}
			catch(InterruptedException e)
			{
				continue;	
			}
						
			synchronized(rmiHosts)
			{
				if(!checkRmiHosts())
				{
					if(cyclesWithoutRmiHosts == 0)
					{
						firstErrorTime = System.currentTimeMillis();
					}
					else if( (!criticalErrorSignalled) && ((System.currentTimeMillis() - firstErrorTime) > 120*1000) )  // If the RmiManager cannot establish contact/create a registry in 2 minutes - log a critical error!
					{
						if(this.isKeySystem()) logCriticalError("No valid RMI registries avalible! Check property for registry addresses! Remote administration of this server will not be possible!", JServerConstants.LOG_MESSAGE_ID_RMI_REGISTRY_FAILURE);
						else logError("No valid RMI registries avalible! Check property for registry addresses!");
						criticalErrorSignalled = true;
					}
					cyclesWithoutRmiHosts++;
				}
				else
					cyclesWithoutRmiHosts = 0;
			}
		}
	}
	
	/**
	 * Performs a check on all registered RMI hosts.
	 */
	private boolean checkRmiHosts()
	{
		if(!canRun) return true;
		
		JServerRmiHost rmiHost = null;
		boolean remove = false;
		RemoteRmiClient client = null;
		boolean rmiHostFailure = false;
		int numberOfOkRmiHosts = 0;
		
		for(int i=0; i<rmiHosts.size(); i++)
		{
			rmiHost = (JServerRmiHost)rmiHosts.get(i);
			rmiHostFailure = false;
			
			if(rmiHost == null) continue;
				
			if(rmiHost.hasRmiHostFailed() && (rmiHost.getFailureCounter() < 5))  //RmiHost failure - reinitialize.
				rmiHostFailure = !rmiHost.reinitialize(false);
			else if(rmiHost.getFailureCounter() >= 5)  
			{
				//RmiHost has failed to many times - reinitialize silently
				rmiHostFailure = !rmiHost.reinitialize(true);
			}
			else
			{
				try
				{
					Registry reg = rmiHost.getRegistry();
					
					if(reg == null)
					{
						logWarning("No registry associated with " + rmiHost + ". Reinitializing rmi host!");
						rmiHostFailure = !rmiHost.reinitialize(true);
					}
					else
					{
						//Check if this server is registered ok in registry.
						Remote r = reg.lookup(rmiHost.getBindName());
							
						if(r == null)
						{
							logError("Incorrect binding! Rebinding!");
							rmiHostFailure = !rmiHost.rebind();
						}
					
						rmiHost.cleanUpRegistry(); //Performs registry clean up
					}
				}
				catch(RemoteException re)
				{
					logWarning("Error contacting registry - reinitializing JServerRmiHost. Nested exception is: " + re + ". Reinitializing rmi host!");

					try
					{
						Thread.sleep((long)(Math.random()*1234));
					}
					catch(InterruptedException ie)
					{
						if(!canRun) break;
					}
					rmiHostFailure = !rmiHost.reinitialize(true);
				}
				catch(NotBoundException re)
				{
					logWarning("JServerRmiHost not bound! Rebinding!");
					rmiHostFailure = !rmiHost.rebind();
				}
			}
			
			if(!rmiHostFailure && (rmiHost != null))  //Check clients
			{
				remove = false;
				List clients = rmiHost.getClients();
					
				for(int q=0; q<clients.size(); q++)
				{
					remove = false;
					client = null;
						
					try
					{
						client = (RemoteRmiClient)clients.get(q);
						//client = clients[i];
						if(!client.isAlive()) remove = true;
					}
					catch(Exception e)
					{
						remove = true;
					}
										
					if(remove && (client != null))
					{
						rmiHost.disconnectClient(client);
					}
				}
				
				numberOfOkRmiHosts++;	
			}
		}
		
		return (numberOfOkRmiHosts > 0);
	}
}
