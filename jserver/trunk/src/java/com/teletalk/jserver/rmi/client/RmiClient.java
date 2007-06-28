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
package com.teletalk.jserver.rmi.client;

import java.net.InetAddress;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.RMIClientSocketFactory;
import java.rmi.server.RMIServerSocketFactory;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.spi.Filter;

import com.teletalk.jserver.rmi.remote.RemoteEvent;
import com.teletalk.jserver.rmi.remote.RemoteEventListener;
import com.teletalk.jserver.rmi.remote.RemoteJServer;
import com.teletalk.jserver.rmi.remote.RemoteJServerRmiHost;
import com.teletalk.jserver.rmi.remote.RemoteJServerRmiInterface;
import com.teletalk.jserver.rmi.remote.RemoteRmiClient;

/**
 * The abstract RmiClient class is a class for clientside RMI communication with a server.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public class RmiClient extends UnicastRemoteObject implements RemoteRmiClient
{
   static final long serialVersionUID = -1557167346205147353L;

   private static final int PROTOCOL_VERSION = 2;
   
   
   private final ArrayList remoteEventListeners;
	
	/** Flag indicating whether or not this client is alive. */
	protected boolean alive = false;
	
	/** Reference to a RemoteJServerRmiHost object. */
	protected RemoteJServerRmiHost remoteHost = null;
	
   protected RemoteJServerRmiInterface jServerRmiInterface = null;
   
	/** Reference to a RemoteJServer object. */
	protected RemoteJServer remoteServer = null;
	
	/** The id/name of this RmiClient. */
	protected final String id;
   
   protected int currentProtocolVersion = PROTOCOL_VERSION;
	
	/**
	 * Constructs a new RmiClient.
	 * 
	 * @exception RemoteException if there was an error creating this RmiClient.
	 */
	public RmiClient() throws RemoteException
	{
		super();
		String newId;
		try
		{
			newId = "RmiClient@" + InetAddress.getLocalHost().getHostAddress();
		}
		catch(Exception e)
		{
			newId = "RmiClient@" + hashCode();	
		}
		
		this.id = newId;
		this.remoteEventListeners = new ArrayList();
	}
	
	/**
	 * Constructs a new RmiClient.
	 * 
	 * @exception RemoteException if there was an error creating this RmiClient.
	 * 
	 * @since 1.13 Build 600
	 */
	public RmiClient(final String id) throws RemoteException
	{
		super();
		this.id = id;
		this.remoteEventListeners = new ArrayList();
	}
	
	/**
	 * Constructs a new RmiClient.
	 * 
	 * @exception RemoteException if there was an error creating this RmiClient.
	 * 
	 * @since 1.13 Build 600
	 */
	public RmiClient(final String id, final int port) throws RemoteException
	{
		super(port);
		this.id = id;
		this.remoteEventListeners = new ArrayList();
	}
	
	/**
	 * Constructs a new RmiClient.
	 * 
	 * @exception RemoteException if there was an error creating this RmiClient.
	 * 
	 * @since 1.13 Build 600
	 */
	public RmiClient(final String id, final int port, final RMIClientSocketFactory csf, final RMIServerSocketFactory ssf) throws RemoteException
	{
		super(port, csf, ssf);
		this.id = id;
		this.remoteEventListeners = new ArrayList();
	}
	
	/**
	 * Sets the value of the alive flag.
	 * 
	 * @param alive the value for the alive flag.
	 */
	public void setAlive(boolean alive)
	{
		this.alive = alive;	
	}
		
	/**
	 * Connects to a RMI enabled server.
	 * 
	 * @param host the hostname where the RMI registry is located.
	 * @param port the portnumber to the RMI registry.
	 * @param serverName the name which the server object is bound to in the RMI registry.
	 * 
	 * @exception Exception if there was an error connecting to the remote server.
	 */
	public final void connect(String host, int port, String serverName) throws Exception
	{
      if( (host == null) || (host.trim().length() == 0) )
      {
         this.connect(port, serverName);
      }
      else
      {
         this.remoteHost = (RemoteJServerRmiHost)Naming.lookup("//" + host + ":" + port + "/" + serverName + "." + RemoteJServerRmiHost.remoteObjectName);
         doConnect();
      }
	}
	
	/**
	 * Connects to a RMI enabled server on the local host.
	 * 
	 * @param port the portnumber to the RMI registry.
	 * @param serverName the name which the server object is bound to in the RMI registry.
	 * 
	 * @exception Exception if there was an error connecting to the remote server.
	 */
	public final void connect(int port, String serverName) throws Exception
	{
      try
      {
         this.remoteHost = (RemoteJServerRmiHost)Naming.lookup("//:" + port + "/" + serverName + "." + RemoteJServerRmiHost.remoteObjectName);
      }
      catch(Exception e)
      {
         try{
         // Try localhost if above call fails
         this.remoteHost = (RemoteJServerRmiHost)Naming.lookup("//localhost:" + port + "/" + serverName + "." + RemoteJServerRmiHost.remoteObjectName);
         }catch(Exception e2){throw e;}
      }
      doConnect();
	}
	
	/**
	 * Connects to a RMI enabled server on the "well known" RMI port.
	 * 
	 * @param host the hostname where the RMI registry is located.
	 * @param serverName the name which the server object is bound to in the RMI registry.
	 * 
	 * @exception Exception if there was an error connecting to the remote server.
	 */
	public final void connect(String host, String serverName) throws Exception
	{
      try
      {
         this.remoteHost = (RemoteJServerRmiHost)Naming.lookup("//" + host + "/" + serverName + "." + RemoteJServerRmiHost.remoteObjectName);
      }
      catch(Exception e)
      {
         try{
         // Try localhost if above call fails
         this.remoteHost = (RemoteJServerRmiHost)Naming.lookup("//localhost/" + serverName + "." + RemoteJServerRmiHost.remoteObjectName);
         }catch(Exception e2){throw e;}
      }
      
      doConnect();
	}
	
	/**
	 * Connects to a RMI enabled server on the local host and on the "well known" RMI port.
	 * 
	 * @param serverName the name which the server object is bound to in the RMI registry.
	 * 
	 * @exception Exception if there was an error connecting to the remote server.
	 */
	public final void connect(String serverName) throws Exception
	{
      try
      {
         this.remoteHost = (RemoteJServerRmiHost)Naming.lookup(serverName + "." + RemoteJServerRmiHost.remoteObjectName);
      }
      catch(Exception e)
      {
         try{
         // Try localhost if above call fails
         this.remoteHost = (RemoteJServerRmiHost)Naming.lookup("//localhost/" + serverName + "." + RemoteJServerRmiHost.remoteObjectName);
         }catch(Exception e2){throw e;}
      }
		doConnect();
	}
	
	/**
	 * Performs the actual connection to the server.
	 * 
	 * @exception Exception if there was an error connecting to the remote server.
	 */
	protected void doConnect() throws Exception
	{
      try
      {
         this.currentProtocolVersion = remoteHost.getProtocolVersion();
      }
      catch(Exception e)
      {
         this.currentProtocolVersion = 1;
      }
      
      this.currentProtocolVersion = Math.min(this.currentProtocolVersion, PROTOCOL_VERSION);
      
      setAlive(true);
		
      if( this.currentProtocolVersion > 1 )
      {
         this.jServerRmiInterface = remoteHost.connect(this, null);
         this.remoteServer = this.jServerRmiInterface.getRemoteJServer();
      }
      else
      {
         remoteServer = remoteHost.connect(this);
      }
	}
	
	private final List parseServers(String[] rmiUrls)
	{
		String server;
		List serverList = new ArrayList();
		
		for(int i=0; i < rmiUrls.length; i++)
		{
			server = rmiUrls[i];

			if(server.endsWith(RemoteJServerRmiHost.remoteObjectName))
			{
				int index1 = server.lastIndexOf("/");
				int index2 = server.indexOf("." + RemoteJServerRmiHost.remoteObjectName);
				
				if(index1 >= 0 && index2 >=0)
				{
					serverList.add(server.substring(index1 + 1, index2));
				}
			}
		}
      
      Collections.sort(serverList);
		
		return serverList;
	}
	
	/**
	 * Lists registered RMI objects.
	 * 
	 * @param host the hostname where the RMI registry is located.
	 * @param port the portnumber to the RMI registry.
	 * 
	 * @exception Exception if there was an error accessing the remote registry.
	 */
	public final List list(String host, int port) throws Exception
	{
      if( (host == null) || (host.trim().length() == 0) )
      {
         return list(port);
      }
      else
      {
         return parseServers(Naming.list("//" + host + ":" + port));
      }
	}
	
	/**
	 * Lists registered RMI objects.
	 * 
	 * @param port the portnumber to the RMI registry.
	 * 
	 * @exception Exception if there was an error accessing the remote registry.
	 */
	public final List list(int port) throws Exception
	{
      try
      {
         return parseServers(Naming.list("//:" + port));
      }
      catch(Exception e)
      {
         try{
         // Try localhost if above call fails
         return parseServers(Naming.list("//localhost:" + port));
         }catch(Exception e2){throw e;}
      }
	}
	
	/**
	 * Lists registered RMI objects.
	 * 
	 * @param host the hostname where the RMI registry is located.
	 * 
	 * @exception Exception if there was an error accessing the remote registry.
	 */
	public final List list(String host) throws Exception
	{
      /*try
      {*/
         return parseServers(Naming.list("//" + host));
      /*}
      catch(Exception e)
      {
         try{
         if( InetAddress.
         // Try localhost if above call fails
         return parseServers(Naming.list("//localhost"));
         }catch(Exception e2){throw e;}
      }*/
	}
	
	/**
	 * Lists registered RMI objects.
	 * 
	 * @exception Exception if there was an error accessing the remote registry.
	 */
	public final List list() throws Exception
	{
      try
      {
         return parseServers(Naming.list(""));
      }
      catch(Exception e)
      {
         try{
         // Try localhost if above call fails
         return parseServers(Naming.list("//localhost"));
         }catch(Exception e2){throw e;}
      }
	}
	
	/**
	 * Disconnects from the server.
	 */
	public void disconnect()
	{
		setAlive(false);
		try
		{
         if( this.currentProtocolVersion > 1 )
         {
            if(this.jServerRmiInterface != null) this.jServerRmiInterface.disconnect();
         }
         else
         {
            if(this.remoteHost != null) this.remoteHost.disconnect(this);
         }
		}
		catch(Exception e){}
		
      this.jServerRmiInterface = null;
		this.remoteServer = null;
      this.remoteHost = null;
		
		System.gc();
	}
	
	/**
	 * Registers a listener that will receive a notification when a remote event is received from the server.
	 * 
	 * @param listener the listener to register.
	 * 
	 * @since 1.13 Build 600
	 */
	public void registerRemoteEventListener(final RemoteEventListener listener)
	{
		synchronized(this.remoteEventListeners)
		{
			this.remoteEventListeners.add(listener);
		}
	}
	
	/**
	 * Unregisters a remote event listener.
	 * 
	 * @param listener the listener to unregister.
	 * 
	 * @since 1.13 Build 600
	 */
	public void ungisterRemoteEventListener(final RemoteEventListener listener)
	{
		synchronized(this.remoteEventListeners)
		{
			this.remoteEventListeners.remove(listener);
		}
	}
   
   /**
    * @return Returns the currentProtocolVersion.
    * 
    * @since 2.0
    */
   public int getCurrentProtocolVersion()
   {
      return currentProtocolVersion;
   }
   
   /**
    * 
    */
   public String[] listLoggers() throws RemoteException
   {
      if( this.currentProtocolVersion > 1 )
      {
         return this.jServerRmiInterface.listLoggers();
      }
      else return new String[0];
   }
  
   /**
    * 
    * @param loggerName
    * @throws RemoteException
    */
   public void addAppender(String loggerName) throws RemoteException
   {
      if( this.currentProtocolVersion > 1 )
      {
         this.jServerRmiInterface.addAppender(loggerName);
      }
   }
   
   /**
    * 
    * @param loggerName
    * @throws RemoteException
    */
   public void removeAppender(String loggerName) throws RemoteException
   {
      if( this.currentProtocolVersion > 1 )
      {
         this.jServerRmiInterface.removeAppender(loggerName);
      }
   }
   
   /**
    * 
    * @throws RemoteException
    */
   public void removeAllAppenders() throws RemoteException
   {
      if( this.currentProtocolVersion > 1 )
      {
         this.jServerRmiInterface.removeAllAppenders();
      }
   }
   
   /**
    * 
    * @param loggerName
    * @param filter
    * @throws RemoteException
    */
   public void addAppenderFilter(String loggerName, Filter filter) throws RemoteException
   {
      if( this.currentProtocolVersion > 1 )
      {
         this.jServerRmiInterface.addAppenderFilter(loggerName, filter);
      }
   }
   
   /**
    * 
    * @param loggerName
    * @param filter
    * @throws RemoteException
    */
   /*public void removeAppenderFilter(String loggerName, Filter filter) throws RemoteException
   {
      if( this.currentProtocolVersion > 1 )
      {
         this.jServerRmiInterface.removeAppenderFilter(loggerName, filter);
      }
   }*/
   
   /**
    * 
    * @param loggerName
    * @throws RemoteException
    */
   public void clearAppenderFilters(String loggerName) throws RemoteException
   {
      if( this.currentProtocolVersion > 1 )
      {
         this.jServerRmiInterface.clearAppenderFilters(loggerName);
      }
   }
	
	/*##############################
	######## REMOTE METHODS ##########
	###############################*/
	
	/**
	 * Returns true if this client is alive.
	 * 
	 * @return true if this client is alive, otherwise false.
	 */
	public boolean isAlive()
	{
		return alive;
	}
	
	/**
	 * Called by the server to disconnect this RmiClient.
	 */
	public void disconnectedFromServer()
	{
		setAlive(false);
      this.jServerRmiInterface = null;
		this.remoteServer = null;
      this.remoteHost = null;
	}
	
	/**
	 * Identifies this Administrator by returning its id String.
	 * 
	 * @return String object indentifying this Administrator.
	 */	
	public String identify() throws RemoteException
	{
		return id;
	}
			
	/**
	 * Checkes whether or not this RmiClient wants to receive events.
	 * This implementation always returns true.
	 * 
	 * @return true.
	 */
	public boolean receiveEvents()
	{
		return true;
	}
		
	/**
	 * Method for receiving events from the server.
	 * 
	 * @param event the event.
	 */
	public void receiveEvent(final RemoteEvent event)
	{
		RemoteEventListener listener;
		
		synchronized(this.remoteEventListeners)
		{
			for(int i=0; i<this.remoteEventListeners.size(); i++)
			{
				listener = (RemoteEventListener)this.remoteEventListeners.get(i);
				
				if(listener != null)
				{
					try
					{
						listener.remoteEventReceived(event);
					}
					catch(Exception e)
					{
						e.printStackTrace();
					}
				}
			}	
		}
	}
   
   /**
    * 
    * @since 2.0
    */
   public int getProtocolVersion()
   {
      return PROTOCOL_VERSION;
   }
}
