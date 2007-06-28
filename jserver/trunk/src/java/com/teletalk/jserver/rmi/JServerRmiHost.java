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

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.registry.Registry;
import java.rmi.server.RMISocketFactory;
import java.rmi.server.RemoteServer;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Map;

import org.apache.log4j.Level;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.rmi.adapter.JServerRmiAdapter;
import com.teletalk.jserver.rmi.remote.RemoteJServer;
import com.teletalk.jserver.rmi.remote.RemoteJServerRmiHost;
import com.teletalk.jserver.rmi.remote.RemoteJServerRmiInterface;
import com.teletalk.jserver.rmi.remote.RemoteRmiClient;
import com.teletalk.jserver.tcp.TcpEndPointIdentifier;

/**
 * The JServerRmiHost class is the portal for all RMI communication with the server. This class
 * is the actual remote object that clients look up when they wish to communicate with the server. 
 * JServerRmiHost also handles the creation or connection to a local RMI registry at the address specified 
 * when creating an instance of the class.<br>
 * <br>
 * The JServerRmiHost is responsible for creating a JServerRmiAdapter connected to the top object in the
 * JServer architecture, the JServer class, from which it is possible to build a tree of adapters to 
 * all it's SubSystems and SubComponents and their Properties. This makes it possbile for clients to
 * inspect, and possibly modify, almoste every part of the server.
 * 
 * @see JServerRmiAdapter
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class JServerRmiHost extends UnicastRemoteObject implements RemoteJServerRmiHost
{
   static final long serialVersionUID = 3040842337084930998L;

   /** @since 2.0 */
   public static final int PROTOCOL_VERSION = 2;
   

	private final ArrayList clients;
	private Registry registry;

	private final RmiManager parent;
	private final String fullName;
	
	private final String registryHost;
	private final int registryPort;
	
	private boolean rmiHostFailure = false;
	private int failureCount = 0;
	
	private boolean registryOwner = false;
	
	private JServerRmiAdapter topsystemAdapter;
  	
	/**
	 * Constructs a new JServerRmiHost object for communicating with a local registry at the default RMI registry port. 
	 * 
	 * @exception RemoteException if there was an error creating this JServerRmiHost.
	 */
	public JServerRmiHost(final RmiManager parent) throws RemoteException
	{
		this(parent, Registry.REGISTRY_PORT);
	}
	
	/**
	 * Constructs a new JServerRmiHost object for communicating with a local registry. 
	 * 
	 * @param registryPort the port for the RMI registry.
	 * 
	 * @exception RemoteException if there was an error creating this JServerRmiHost.
	 */
	public JServerRmiHost(final RmiManager parent, final int registryPort) throws RemoteException
	{
		this(parent, TcpEndPointIdentifier.anyLocalHostString, registryPort);
	}
	
	/**
	 * Constructs a new JServerRmiHost object for communicating with a registry at the default RMI registry port. 
	 * 
	 * @param registryHost the local ip (host) address of a remote RMI registry.
	 * 
	 * @exception RemoteException if there was an error creating this JServerRmiHost.
	 */
	public JServerRmiHost(final RmiManager parent, final String registryHost) throws RemoteException
	{
		this(parent, registryHost, Registry.REGISTRY_PORT);
	}
	
	/**
	 * Constructs a new JServerRmiHost object for communicating with a registry at the specified address. 
	 * 
	 * @param registryAddress the local address (ip address and port number) of a remote RMI registry.
	 * 
	 * @exception RemoteException if there was an error creating this JServerRmiHost.
	 */
	public JServerRmiHost(final RmiManager parent, final TcpEndPointIdentifier registryAddress) throws RemoteException
	{
		this(parent, registryAddress.getAddress(), registryAddress.getPort());
	}
	
	/**
	 * Constructs a new JServerRmiHost object. 
	 * 
	 * @param registryHost the ip (host) address of the RMI registry.
	 * @param registryPort the port for the RMI registry.
	 * 
	 * @exception RemoteException if there was an error creating this JServerRmiHost.
	 */
	public JServerRmiHost(final RmiManager parent, final String registryHost, final int registryPort) throws RemoteException
	{
		super(parent.getExportAddresses()[0].getPort());
		
		this.parent = parent;
		this.fullName = parent.getFullName() + ".JServerRmiHost(" + ((registryHost == null) ? TcpEndPointIdentifier.anyLocalHostString : registryHost) + ":" + registryPort + ")";
		this.clients = new ArrayList();
		
		this.registryHost = (registryHost != null) ? registryHost : TcpEndPointIdentifier.anyLocalHostString;
		this.registryPort = registryPort;
	}
   
   /**
    * @return Returns the topsystemAdapter.
    */
   JServerRmiAdapter getTopsystemAdapter()
   {
      return this.topsystemAdapter;
   }
   
   /**
    * @return Returns the fullName.
    * 
    * @since 2.0
    */
   public String getFullName()
   {
      return this.fullName;
   }
   
   /**
    * @return Returns the parent.
    * 
    * @since 2.0
    */
   public RmiManager getParent()
   {
      return parent;
   }
	
	/**
	 * Initializes this this JServerRmiHost by trying to reconnect to the registry or create a new one if 
	 * that should fail.
	 * 
	 * @return <code>true</code> if initialization was successfull, otherwise <code>false</code>.
	 */
	public boolean initialize()
	{
		return this.initialize(true, false);
	}
	
	/**
	 * Reinitializes this JServerRmiHost by trying to reconnect to the registry or create a new one if 
	 * that should fail.
	 * 
	 * @param silent boolean flag inicating if no errors should be logged while attempting to reinitialize.
	 * 
	 * @return <code>true</code> if reinitialization was successfull, otherwise <code>false</code>.
	 */
	public boolean reinitialize(boolean silent)
	{
		return this.initialize(false, silent);
	}
	
	/**
	 * Reinitializes this JServerRmiHost by trying to reconnect to the registry or create a new one if 
	 * that should fail.
	 * 
	 * @param isInitialization boolean flag if the call to this method is due to an initialization or a reinitialization.
	 * @param silent boolean flag inicating if no errors should be logged while attempting to reinitialize.
	 */
	private boolean initialize(final boolean isInitialization, final boolean silent)
	{
		try
		{
			try{
			Thread.sleep((long)(Math.random()*1000));
			}catch(InterruptedException ie){}
			
			destroy();
			
			getRegistryAndBind(silent);
			
			this.topsystemAdapter = (JServerRmiAdapter) ((JServer.getJServer() != null) ? JServer.getJServer().getRmiAdapter() : null); 
		
			rmiHostFailure = false;
			failureCount = 0;
			
			return true;
		}
		catch(Exception e)
		{
			if(isInitialization && !silent) parent.logError(fullName, "Unable to initialize registry!", e);
			else if(!isInitialization && !silent) parent.logError(fullName, "Unable to reinitialize registry!", e);
			rmiHostFailure = true;
			failureCount++;
			
			return false;
		}
	}
	
	/**
	 * Destroys this JServerRmiHost.
	 */
	public final void destroy()
	{
		if(registry != null)
		{
			try{
			registry.unbind(getBindName());
			}catch(Exception e){}
		}
			
		for(int i=0; i<clients.size(); i++)
		{
			try{
			disconnectClient((RemoteRmiClient)clients.get(i));	
			}catch(Exception e){}
		}
			
		registry = null;
		topsystemAdapter = null;
			
		synchronized(this)
		{
			notifyAll();	
		}
	}
	
	/**
	 * Binds to a registry.
	 */
	private void getRegistryAndBind(final boolean silent) throws Exception
	{
		try
		{
			// Create a custom socket factory for the registry
			final RMISocketFactory socketFactory = new RMISocketFactory()
				{
					public ServerSocket createServerSocket(final int port) throws IOException
					{
						return new java.net.ServerSocket(registryPort, 50, InetAddress.getByName(registryHost));
					}
						
					public Socket createSocket(final String host, final int port) throws IOException
					{
						return RMISocketFactory.getDefaultSocketFactory().createSocket(host, port);
					}
				};
				
			registry = LocateRegistry.createRegistry(registryPort, socketFactory, socketFactory);
			registry.rebind(getBindName(), this);
			if(!silent) parent.logInfo(fullName, "Created registry at " + registryHost + ":" + registryPort + ".");
			registryOwner = true;
		}
		catch(Exception e)
		{
			if( !silent && parent.isDebugMode() ) parent.log(Level.DEBUG, "Error creating a registry at " + registryHost + ":" + registryPort + ". Attempting to connect.", e);
				
         if( TcpEndPointIdentifier.anyLocalHostString.equals(registryHost) ) registry = LocateRegistry.getRegistry(registryPort);
			else registry = LocateRegistry.getRegistry(registryHost, registryPort);
			registry.rebind(getBindName(), this);
			if(!silent) parent.logInfo(fullName, "Connected to registry at " + registryHost + ":" + registryPort + ".");
			registryOwner = false;
		}
	}
	
	/**
	 * Gets the address of the RMI registry used by this JServerRmiHost.
	 * 
	 * @return a TcpEndPointIdentifier object.
	 */
	public TcpEndPointIdentifier getAddress()
	{
		return new TcpEndPointIdentifier(this.registryHost, this.registryPort);	
	}
			
	/**
	 * Checks if this JServerRmiHost owns (is the creator of) the registry it is associated with.
	 * 
	 * @return <code>true</code> this JServerRmiHost owns (is the creator of) the registry it is associated with, 
	 * otherwise <code>false</code>.
	 */
	public boolean isRegistryOwner()
	{
		return registryOwner;
	}
	
	/**
	 * If this JServerRmiHost owns (is the creator of) the registry it is associated with, this method 
	 * cleans up the registry by unbinding servers that are no longer running.
	 * 
	 * @exception RemoteException if an error occurs while contacting the registry.
	 */
	public void cleanUpRegistry() throws RemoteException
	{
		if(registryOwner)
		{
			String[] boundServers = registry.list();
			String serverName;
		
			for(int i=0; i<boundServers.length; i++)
			{
				int index1 = boundServers[i].lastIndexOf("/");
				int index2 = boundServers[i].indexOf("." + RemoteJServerRmiHost.remoteObjectName);
				serverName = boundServers[i].substring(index1 + 1, index2);
				
				try
				{
               JServer jserver = JServer.getJServer();
               String jServerName = "";
               if( jserver != null ) jServerName = jserver.getName();
               
					if( (boundServers[i] != null) && (!serverName.equals( jServerName )) )
               {
                  if( this.parent.isDebugMode() ) parent.logDebug(fullName, "CleanUpRegistry - looking up '" + boundServers[i] + "'.");
                  RemoteJServerRmiHost remoteJServerRmiHost = (RemoteJServerRmiHost)this.registry.lookup(boundServers[i]);

                  if( this.parent.isDebugMode() ) parent.logDebug(fullName, "CleanUpRegistry - checking " + remoteJServerRmiHost + ".");
                  remoteJServerRmiHost.aliveCheck();  //Call a method to check if the server is alive
               }
				}
				catch(Exception e)
				{
               if( this.parent.isDebugMode() ) parent.logError("Error contacting server '" + serverName  + "'! Unbinding server from registry.", e);
					else parent.logWarning(fullName, "Error contacting server '" + serverName  + "'! Unbinding server from registry. Error is: " + e + ".");
               
					try
					{
						registry.unbind(boundServers[i]);
					}catch(Exception ex){}
				}
			}
		}
	}
	
	/**
	 * Checkes if this JServerRmiHost has failed (lost contact with the associated registry).
	 * 
	 * @return <code>true</code> if this JServerRmiHost has failed, otherwise <code>false</code>.
	 */
	public boolean hasRmiHostFailed()
	{
		return rmiHostFailure;
	}
	
	/**
	 * Gets the number of times this JServerRmiHost has failed to reinitialize or rebind.
	 * 
	 * @return the number of times this JServerRmiHost has failed to reinitialize or rebind.
	 */
	public int getFailureCounter()
	{
		return failureCount;
	}
		
	/**
	 * Rebinds this object in the RMI registry.
	 * 
	 * @return <code>true</code> if the rebind was successful, otherwise <code>false</code>.
	 */
	public boolean rebind()
	{
		try
		{
			registry.rebind(getBindName(), this);
			
			rmiHostFailure = false;
			failureCount = 0;
			
			return true;
		}
		catch(Exception e)
		{
			rmiHostFailure = true;
			failureCount++;
			
			return false;
		}
	}
	
	/**
	 * Gets the name to which this object is or is to be bound in the RMI registry.
	 * 
	 * @return the bind name.
	 */
	public final String getBindName()
	{
      JServer jServer = JServer.getJServer();
      
		if( jServer != null ) return JServer.getJServer().getName() + "." + remoteObjectName;
      else return remoteObjectName;
	}
	
	/**
	 * Gets a refetence to the RMI registry.
	 * 
	 * @return a Registry object.
	 */
	public final Registry getRegistry()
	{
		return registry;
	}
   
   /**
    * Gets all clients connected to this JServerRmiHost.
    * 
    * @return a list containing RemoteRmiClient objects.
    */
   protected synchronized ArrayList getClients()
   {
      return new ArrayList(clients);
   }
   
   /**
    * Disconnects a client from this JServerRmiHost.
    * 
    * @param remoteRmiClient a RemoteRmiClient object.
    */
   public void disconnectClient(final RemoteRmiClient remoteRmiClient)
   {
      RemoteRmiClientProxy proxy = null;
      
      if( remoteRmiClient instanceof RemoteRmiClientProxy )
      {
         proxy = (RemoteRmiClientProxy)remoteRmiClient;
      }
      else if( remoteRmiClient != null )
      {
         synchronized(this)
         {
            RemoteRmiClientProxy itProxy = null;
            
            for(int i=0; i<clients.size(); i++)
            {
               itProxy = (RemoteRmiClientProxy)clients.get(i);
               if( itProxy != null )
               {
                  if( remoteRmiClient.equals( itProxy.getRemoteRmiClient() ) )
                  {
                     proxy = itProxy;
                     break;
                  }
               }
            }
         }
      }
      
      if( proxy != null) disconnectClient(proxy);
   }
   
   /**
    * Disconnects a client from this JServerRmiHost.
    * 
    * @param proxy a Client object.
    */
   private void disconnectClient(final RemoteRmiClientProxy proxy)
   {
      synchronized(this)
      {
         clients.remove(proxy);
      }
      try
      {
         proxy.disconnectedFromServer();
      }
      catch(Exception e)
      {
         String clientID = null;
         try
         {
            clientID = proxy.identify();
         }
         catch(Exception e2){}
         
         if(clientID != null) parent.logError(fullName, "Got exception while trying to disconnect client (" + clientID + ")!", e);
         else parent.logError(fullName, "Got exception while trying to disconnect client!", e);
      }
      finally
      {
         proxy.destroy();
      }
   }
	
	/*#######################
	#####	Remote methods ######
	 ########################*/
	
   /**
    * Method to connect a RmiClient to a JServer.
    * 
    * @param remoteRmiClient a RemoteRmiClient object.
    * @param credentials the credentials needed to authenticate the client in the server (may be null). 
    * 
    * @return a RemoteJServerRmiInterface.
    * 
    * @exception RemoteException if there was an error during remote access of this method.
    * 
    * @since 2.0
    */
   public RemoteJServerRmiInterface connect(RemoteRmiClient remoteRmiClient, Map credentials) throws RemoteException
   {
      String clientId = null;
      
      try
      {
         parent.logInfo(fullName, "Remote rmi client at " + RemoteServer.getClientHost() + " connected.");
         
         int currentProtocolVersion;
         
         try
         {
            currentProtocolVersion = remoteRmiClient.getProtocolVersion();
         }
         catch(Exception e)
         {
//  TODO: Check which exception is thrown when method doesn't exist.         
            currentProtocolVersion = 1;
         }
         
         currentProtocolVersion = Math.min(currentProtocolVersion, PROTOCOL_VERSION);
         
         RemoteRmiClientProxy proxy = null;
                  
         // Make sure a proxy doesn't already exists
         synchronized(this)
         {
            RemoteRmiClientProxy itProxy = null;
            
            for(int i=0; i<clients.size(); i++)
            {
               itProxy = (RemoteRmiClientProxy)clients.get(i);
               if( itProxy != null )
               {
                  if( remoteRmiClient.equals( itProxy.getRemoteRmiClient() ) )
                  {
                     proxy = itProxy;
                     break;
                  }
               }
            }
         }
         
         if( proxy == null )
         {
            proxy = new RemoteRmiClientProxy(this, remoteRmiClient, currentProtocolVersion);
            clients.add(proxy);
         }
         
         return proxy.getJServerRmiInterface();
      }
      catch(Exception e)
      {
         if( clientId != null) parent.logError(fullName, "Error while connecting remote rmi client ('" + clientId + "')!", e);
         else parent.logError(fullName, "Error while connecting remote rmi client!", e);
         
         if( e instanceof RemoteException ) throw (RemoteException)e;
         else throw new RemoteException("Error while connecting remote rmi client!", e);
      }
   }
   
	/**
	 * Method to connect a RmiClient to a JServerRmiHost.
	 * 
	 * @param remoteRmiClient a RemoteRmiClient object.
    *
    * @deprecated as of 2.0, replaced by {@link #connect(RemoteRmiClient, Map)}. 
	 */
	public synchronized RemoteJServer connect(final RemoteRmiClient remoteRmiClient) throws RemoteException
	{
      String clientId = null;
      
		try
		{
			parent.logInfo(fullName, "Remote rmi client at " + RemoteServer.getClientHost() + " connected.");
         
         int currentProtocolVersion;
         
         try
         {
            currentProtocolVersion = remoteRmiClient.getProtocolVersion();
         }
         catch(Exception e)
         {
//  TODO: Check which exception is thrown when method doesn't exist.         
            currentProtocolVersion = 1;
         }
         
         currentProtocolVersion = Math.min(currentProtocolVersion, PROTOCOL_VERSION);
         
         RemoteRmiClientProxy proxy = null;
                  
         // Make sure a proxy doesn't already exists
         synchronized(this)
         {
            RemoteRmiClientProxy itProxy = null;
            
            for(int i=0; i<clients.size(); i++)
            {
               itProxy = (RemoteRmiClientProxy)clients.get(i);
               if( itProxy != null )
               {
                  if( remoteRmiClient.equals( itProxy.getRemoteRmiClient() ) )
                  {
                     proxy = itProxy;
                     break;
                  }
               }
            }
         }
         
         if( proxy == null )
         {
            proxy = new RemoteRmiClientProxy(this, remoteRmiClient, currentProtocolVersion);
            clients.add(proxy);
         }
		
   		return topsystemAdapter;
      }
      catch(Exception e)
      {
         if( clientId != null) parent.logError(fullName, "Error while connecting remote rmi client ('" + clientId + "')!", e);
         else parent.logError(fullName, "Error while connecting remote rmi client!", e);
         
         if( e instanceof RemoteException ) throw (RemoteException)e;
         else throw new RemoteException("Error while connecting remote rmi client!", e);
      }
	}
	
	/**
	 * Method to disconnect a RmiClient from a JServerRmiHost.
	 * 
	 * @param remoteRmiClient a RemoteRmiClient object.
    * 
    * @deprecated as of 2.0, replaced by {@link JServerRmiInterface}.
	 */
	public void disconnect(final RemoteRmiClient remoteRmiClient) throws RemoteException
	{ 
      if( remoteRmiClient != null )
      {
         RemoteRmiClientProxy proxy = null;
         
         synchronized(this)
         {
            RemoteRmiClientProxy itProxy = null;
            
            for(int i=0; i<clients.size(); i++)
            {
               itProxy = (RemoteRmiClientProxy)clients.get(i);
               if( itProxy != null )
               {
                  if( remoteRmiClient.equals( itProxy.getRemoteRmiClient() ) )
                  {
                     clients.remove(i);
                     proxy = itProxy;
                     break;
                  }
               }
            }
         }
         
         if( proxy != null ) proxy.destroy();
      }
	}

	/**
	 * Method to get the <code>java.rmi.server.codebase</code> property used by this server.
	 * 
	 * @return the value of the <code>java.rmi.server.codebase property</code>.
    * 
    * @deprecated as of 2.0, replaced by {@link JServerRmiInterface} (where the method even is spelled right! :)).
	 */ 
	public String gerRmiCodeBase() throws RemoteException
	{
		return System.getProperty("java.rmi.server.codebase");
	}

	/**
	 * Performs an alive check.
    * 
    * @deprecated as of 2.0, replaced by {@link JServerRmiInterface}.
	 */
	public void aliveCheck() throws RemoteException
	{
		try
		{
			synchronized(this)
			{
				wait(5*1000);	// Wait 5 seconds per alive check
			}
		}
		catch(Exception e){}
	}
   
   /**
    * Gets the protocol version.
    *  
    * @since 2.0
    */
   public int getProtocolVersion()
   {
      return PROTOCOL_VERSION;
   }
}
