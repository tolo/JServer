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

import org.apache.log4j.Level;

import com.teletalk.jserver.comm.EndPoint;
import com.teletalk.jserver.comm.EndPointIdentifier;
import com.teletalk.jserver.property.VectorPropertyItem;

/**
 * The TcpEndPoint class is an extension of TcpConnection and it's purpose is to function as a foundation for all 
 * classes that wish to implement an advanced form of communications endpoint. This class can be used as a client side 
 * communication endpoint, server side communication endpoint or both (but only one active implementation at a time).<br>
 * <br>
 * The TcpEndPoint class contains several flags that indicate its condition. The most important flags are the ones that 
 * indicate if the endpoint is connected and has established link. <br>
 * <br>
 * If the flag indicating if the endpoint is connected is set to <code>true</code> 
 * it means that the endpoint contains a socket object that is connected to some remote destination. The value of that flag can be checked by 
 * calling the mehtod {@link #isConnected()}.<br>
 * <br>
 * If the flag indicating if the endpoint has established link is set to <code>true</code> 
 * it means that the endpoint has successfully completed handshaking procedures. The value of that flag can be checked by 
 * calling the mehtod {@link #isLinkEstablished()}.<br>
 * <br>
 * This class can be customized by overriding the following methods:<br>
 *<ul>
 * <li>{@link #initClientSide(boolean)} - performs client side handshaking procedures.</li>
 * <li>{@link #initServerSide()} - performs server side handshaking procedures.</li>
 * <li>{@link #runClientSideImpl()} - contains the client side communication implementation.</li>
 * <li>{@link #runServerSideImpl()} - contains the server side communication implementation.</li>
 * </ul><br>
 * <br>
 * <i>Note: The lock of this object is used for serializing access to variables and methods dealing with the state of this object (i.e. disconnect etc). </i>
 * 
 * @see TcpCommunicationManager
 *  
 * @author Tobias Löfstrand
 * 
 * @since Beta 2
 */
public abstract class TcpEndPoint extends TcpConnection implements EndPoint, VectorPropertyItem
{
	/*
		Synchronization summary:
			* The lock of this object is used for serializing access to variables and methods dealing with the state of this object (i.e. disconnect etc). 
	 */
	
	private final TcpCommunicationManager commManager;
	
	private volatile TcpEndPointModeImpl modeImpl;
   private boolean endPointConnectedNotificationSuccess = false;
	
	volatile boolean endPointActive = false;  //Indicates if this end point is currently running.
	
	/** The base name of this TcpEndPoint. */
	private final String initialName;
	private String baseFullName;
	
	/** Flag indicating if this TcpEndPoint is connected or not. */
	private volatile boolean connected = false;

	/** Flag indicating if post connection negotiation (handshaking) procedures have been successfully completed. */
	private volatile boolean linkEstablished = false;
	
	private volatile int sequenceNumber = 0;
	
	private String key = null;
	private Object keyLock = new Object();
	
	volatile TcpEndPointIdentifier address;
   
   /** @since 2.0.1 */
   private TcpEndPointGroup endPointGroup;
	
	/**
	 * Creates  a new TcpEndPoint.
	 * 
	 * @param commManager reference to a parent TcpCommunicationManager.
	 */
	public TcpEndPoint(final TcpCommunicationManager commManager)
	{
		this(commManager, "TcpEndPoint");
	}
	
	/**
	 * Creates  a new TcpEndPoint.
	 * 
	 * @param commManager reference to a parent TcpCommunicationManager.
	 * @param name the name of this TcpEndPoint.
	 */
	public TcpEndPoint(final TcpCommunicationManager commManager, final String name)
	{
		super(name);
		
		this.commManager = commManager;
		this.initialName = name;
		this.baseFullName = this.getName();
	}
   
   /**
    * Gets the {@link TcpCommunicationManager} associated with this TcpEndPoint.
    * 
    * @since 2.0.1 (20041126)
    */
   public TcpCommunicationManager getTcpCommunicationManager()
   {
      return commManager;
   }
	
	/**
	 * Gets the base (initial) name of this TcpEndPoint (the name that was specified in the constructor).
	 * 
	 * @return the base name of this TcpEndPoint.
	 */
	public String getBaseName()
	{
		return this.initialName;
	}
		
	/**
	 * Sets the data stored in this TcpEndPoint. This method also determines if the endpoint should run the 
	 * client- or server side implementaion.
	 * 
	 * @param data a TcpConnectionData object.
	 * 
	 * @exception ClassCastException if there was an error casting data to TcpConnectionData.
	 */
	protected void setData(final Object data) throws ClassCastException
	{
		super.setData(data);
		
		TcpConnectionData connectionData = this.getTcpConnectionData();
		
		this.baseFullName = this.getName(); //Initialize baseName again, to make sure that the name includes the thread id.
		this.rename(this.baseFullName + "(" + connectionData.getRemoteAddress() + ")");
				
      this.modeImpl = createTcpEndPointModeImpl(connectionData.isServerSide());
		
		endPointActive = true;
	}
   
   /**
    * Creates the actual mode implementation, handling the setup and execution of the client or server side 
    * endpoint logic.
    * 
    * @since 2.0.1 (20041126)
    */
   protected TcpEndPointModeImpl createTcpEndPointModeImpl(final boolean serverSide)
   {
      if( serverSide ) return new TcpEndPointServerSideModeImpl();
      else return new TcpEndPointClientSideModeImpl();
   }
   
   /**
    * Gets the {@link TcpEndPointGroup} this endpoint belongs to. For server side endpoints, this endpoint group isn't available through this 
    * method until after {@link #initServerSide()} is called.
    * 
    * @since 2.0.1
    */
   public TcpEndPointGroup getEndPointGroup()
   {
      return endPointGroup;
   }
   
   /**
    * Sets the {@link TcpEndPointGroup} this endpoint belongs to.
    * 
    * @since 2.0.1
    */
   protected void setEndPointGroup(TcpEndPointGroup endPointGroup)
   {
      this.endPointGroup = endPointGroup;
   }
	
	/**
	 * Returns an EndPointIdentifier object identifying an address which this end point is to represent. For client 
	 * side endpoints this method returns the value returned by calling  {@link TcpConnectionData#getRemoteAddress()}. For server side 
	 * endpoints that value is returned by default (which means that it will be a different value for each endpoint, i.e. they will be placed into different 
    * {@link TcpEndPointGroup} objects), however the address value can be set by calling the method {@link #setEndPointIdentifier(TcpEndPointIdentifier address)}.
	 *  
	 * @return an EndPointIdentifier object identifying this end point.
	 * 
	 * @see TcpConnection#getRemoteAddress()
	 */
	public final EndPointIdentifier getEndPointIdentifier()
	{
		return (modeImpl != null) ? modeImpl.getEndPointIdentifier() : null;
	}
	
	/**
	 * Sets the EndPointIdentifier object identifying an address which this end point is to represent. This method is provied exclusively to server side 
	 * endpoints that may wish to represent a different address than that which is returned by calling  {@link TcpConnectionData#getRemoteAddress()}. For instance, 
	 * endpoints that originated from the same remote sever may want to represent the same address so that they can be grouped together.<br>
	 * <br>
	 * Note that the address cannot be set after the endpoint has established link. The preferred place to set the address is when handshaking procedures are 
	 * performed, in the method {@link #initServerSide()}.
	 *  
	 * @param address an TcpEndPointIdentifier object identifying this end point.
	 */
	public final void setEndPointIdentifier(TcpEndPointIdentifier address)
	{
		if((getTcpConnectionData() != null) && getTcpConnectionData().isServerSide() && !isLinkEstablished())
		{
			this.address = address;	
			this.key = null;
		}
	}
	
	/**
	 * Gets a number uniquely identifying this TcpEndPoint. This number is meant to be used when handling 
	 * connection collisions between two TcpCommunicationManager systems. The number returned by this method 
	 * is guaranteed to be the same for the TcpEndPoint objects on each side of the connection.
	 * 
	 * @return a number uniquely identifying this TcpEndPoint, -1 if an error occurs.
	 */
	public final long getMagicNumber()
	{
		return (modeImpl != null) ? modeImpl.getMagicNumber() : -1;
	}
	
	/**
	 * Assigns a sequence number to this TcpEndPoint. This method is used when several endpoints are 
	 * connected to the same address.
	 * 
	 * @param sequenceNumber a sequence number.
	 */
	protected final void assignSequenceNumber(int sequenceNumber)
	{
		this.sequenceNumber = sequenceNumber;
		this.key = null;
		this.rename(this.baseFullName + "(" + getKey() + ")");
	}
	
	/**
	 * Gets the key uniquely identifying this TcpEndPoint.
	 * 
	 * @return the key uniquely identifying this TcpEndPoint.
	 */
	public final String getKey()
	{
		synchronized(keyLock)
		{
			if(key == null) key = getEndPointIdentifier() + "[" + sequenceNumber + "]";
		}
		
		return key;
	}
	
	/**
	 * Gets a String describing this TcpEndPoint.
	 * 
	 * @return a String describing this TcpEndPoint.
	 */	
	public String getDescription()
	{
		String description = initialName + ": ";
		
		if(this.isActive())
		{
			final EndPointIdentifier addr = this.getEndPointIdentifier();
						
			if( !isConnected() ) description = "Connecting to " + addr + ".";
			else if( isConnected() && !isLinkEstablished() ) description = "Connected to " + addr + ".";
			else description = "Link established with " + addr + ".";
		}
		else
		{
			description = "Inactive";
		}
		
		return description + " (Address: " + this.getEndPointIdentifier() + ", thread id: " + this.thread.getThreadId() + ")";
	}
	
	/**
	 * Checks if this TcpEndPoint currently is active.
	 * 
	 * @return true if this TcpEndPoint currently is active, otherwise false.
	 */
	public final boolean isActive()
	{
		return endPointActive;
	}
	
	/**
	 * Default disconnection method. Disconnects this TcpEndPoint by closing the socket (and streams) and setting the 
	 * <code>connected</code> flag to <code>false</code>.
	 */
	public synchronized void disconnect()
	{
		endPointActive = false;  //Important to set this flag to false BEFORE calling the methods below
		setConnected(false);
		setLinkEstablished(false);
		
		if(Thread.currentThread() != thread)
      {
			thread.interrupt();
      }
		closeConnection();
		
		modeImpl = null;
	}
	
	/**
	 * Checks if this TcpEndPoint is connected.
	 * <br><br>
	 * The <code>connected</code> flag is set and reset automatically by TcpEndPoint.
	 * 
	 * @return true if this TcpEndPoint is connected.
	 * 
	 * @see #setConnected(boolean connected)
	 * @see #waitForConnected()
	 */
	public final boolean isConnected()
	{
		return connected;	
	}
	
	/**
	 * Sets the connected flag and notifies waiting threads.
	 * <br><br>
	 * The <code>connected</code> flag is set and reset automatically by TcpEndPoint.
	 * 
	 * @see #isConnected()
	 * @see #waitForConnected()
	 */
	protected final void setConnected(boolean connected)
	{
		this.connected = connected;
		synchronized(this)
		{
			notify();
		}
	}
	
	/**
	 * Blocks the calling thread until this TcpEndPoint gets connected or 
	 * attempts to connect it has failed. 
	 * <br><br>
	 * The <code>connected</code> flag is set and reset automatically by TcpEndPoint.
	 * 
	 * @return true if this TcpEndPoint is connected, otherwise false. 
	 * 
	 * @see #setConnected(boolean connected)
	 * @see #isConnected()
	 * 
	 * @exception InterruptedException if the calling thread was interrupted.
	 */
	public final boolean waitForConnected() throws InterruptedException
	{
		while(!connected && endPointActive)
		{
			synchronized(this)
			{
				wait(10000);  //Timeout after 10 seconds to check if  the flags have changed
			}
		}
		return connected;	
	}
	
	/**
	 * Checks if a link has been established, i.e. if post connection negotiation (handshaking) procedures have been successfully completed.
	 * 
	 * @return true if a link has been established, i.e. if post connection negotiation (handshaking) procedures have been successfully completed.
	 * 
	 * @see #setLinkEstablished(boolean linkEstablished)
	 * @see #waitForLinkEstablished()
	 */
	public final boolean isLinkEstablished()
	{
		return linkEstablished;	
	}
	
	/**
	 * Sets the value for the flag for indication of established link, i.e. if post connection negotiation (handshaking) procedures have been successfully completed.
	 * 
	 * @param linkEstablished the new value for the flag.
	 * 
	 * @see #isLinkEstablished()
	 * @see #waitForLinkEstablished()
	 */
	protected final void setLinkEstablished(boolean linkEstablished)
	{
		this.linkEstablished = linkEstablished;
		synchronized(this)
		{
			notify();
		}
	}
	
	/**
	 * Waits for a link to be established. This method blocks the calling thread until the link has been 
	 * established or the TcpEndPoint gets disconnected.
	 * <br><br>
	 * The <code>linkEstablished</code> flag is reserved for subclasses of TcpEndPoint who wish to indicate that post connection 
	 * negotiation (handshaking) procedures have been successfully completed. The flag is set to <code>false</code> by default.
	 * 
	 * @return true if this TcpEndPoint has established a link, otherwise false. 
	 * 
	 * @see #setLinkEstablished(boolean linkEstablished)
	 * @see #isLinkEstablished()
	 * 
	 * @exception InterruptedException if the calling thread was interrupted.
	 */
	public final boolean waitForLinkEstablished() throws InterruptedException
	{
		while(!linkEstablished && endPointActive)
		{
			synchronized(this)
			{
				wait(10000);  //Timeout after 10 seconds to check if  the flags have changed
			}
		}
		return linkEstablished;
	}
	
	/**
	 * Method to signal that a fatal error has occurred in this TcpEndPoint.
	 */
	protected final void fatalError()
	{
		this.thread.error();
	}
	
	/**
	 * Performs default clean up by closing the connection and unregistering this endpoint with the associated TcpCommunicationManager. 
	 * If subclasses override this method they <strong>MUST</strong> call the superclass implementation.
	 */
	protected void cleanUp()
	{
		commManager.unregisterEndPoint(this);  //Unregister this endpoint in the TcpCommunicationManager
		commManager.endPointDisconnectedInternal(this);  //Notify the TcpCommunicationManager that this TcpEndPoint was disconnected.
		
		endPointActive = false;  //Important to set this flag to false BEFORE calling the methods below
		setConnected(false); //Notifies waiting threads...
		setLinkEstablished(false); //Notifies waiting threads...

		super.cleanUp();
		
		this.rename(this.baseFullName);
				
		key = null;
		
		modeImpl = null;

	}
	
	/**
	 * Destroys this TcpEndPoint and unregisters it with the associated TcpCommunicationManager.
	 * If subclasses override this method they <strong>MUST</strong> call the superclass implementation.
	 */
	protected void destroy()
	{
		if(endPointActive)  // If endPointActive is false, the destroy method was called as a result of a draining of the pool
		{
			commManager.unregisterEndPoint(this);  //Unregister this endpoint in the TcpCommunicationManager
			commManager.endPointDisconnectedInternal(this);  //Notify the TcpCommunicationManager that this TcpEndPoint was disconnected.
		}
		
		endPointActive = false; //Important to set this flag to false BEFORE calling the methods below
		setConnected(false); //Notifies waiting threads...
		setLinkEstablished(false); //Notifies waiting threads...

		super.destroy();
		
		this.rename(this.baseFullName);
						
		key = null;
		
		modeImpl = null;
	}
	
	/**
	 * Checks this TcpEndPoint for errors.
	 * 
	 * @return <code>true</code> if everything is OK, otherwise <code>false</code>.
	 */
	public boolean check()
	{
		return (modeImpl != null) ? modeImpl.check() : false;
	}
	
	/**
	 * Performs client side handshaking procedures. This method should be overridden by subclasses that wish to provide an implementation for client side handshaking. <br>
	 * <br>
	 * This implementation only returns <code>true</code>.
	 * 
	 * @param connectionSuccessful convenience flag indicating if this TcpEndPoint was successfully connected (<code>true</code>) or not (<code>false</code>).
	 * 
	 * @return <code>true</code> if handshaking procedures were successfully completed, otherwise <code>false</code>.
	 */
	protected boolean initClientSide(boolean connectionSuccessful)
	{
		return true;
	}
	
	/**
	 * Performs server side handshaking procedures. This method should be overridden by subclasses that wish to provide an implementation for server side handshaking. 
	 * If the server side enpoint is to belong to a logical group of endpoints, the subclass implementation of this method may call {@link #setEndPointIdentifier(TcpEndPointIdentifier)} to 
	 * set the (group) address this enpoint is to represent.<br>
	 * <br>
	 * This implementation only returns <code>true</code>.
	 * 
	 * @return <code>true</code> if handshaking procedures were successfully completed, otherwise <code>false</code>.
	 */
	protected boolean initServerSide()
	{
		return true;
	}
	
	/**
	 * End point communication implementation method that runs in the thread of a client side TcpEndPoint. This method should be overriden by subclasses that 
	 * wish to implement client side handling. This implementation does nothing.
	 */
	protected void runClientSideImpl()
	{
	}
	
	/**
	 * End point communication implementation method that runs in the thread of a server side TcpEndPoint. This method should be overriden by subclasses that 
	 * wish to implement server side handling. This implementation does nothing.
	 */
	protected void runServerSideImpl()
	{
	}
	
	/**
	 * Method called by this TcpEndPoint to create a client socket, connected to the address specified by parameter <code>address</code>. 
	 * This implementation calls the method {@link TcpCommunicationManager#createSocket(TcpEndPointIdentifier)} to create a Socket object. <br>
	 * <br>
	 * Subclasses may override this method to provide a different implementation for creating Socket objects.
	 * 
	 * @param address the remote address to which the created socket should be connected.
	 * 
	 * @return a newly created Socket object.
	 */
	protected Socket createClientSocket(TcpEndPointIdentifier address)
	{
		return commManager.createSocket(address);
	}
	
	/**
	 * ThreadPool worker method. This method detectes if this TcpEndPoint should be server or client side and executes the appropriate 
	 * handling method, {@link #runClientSideImpl()} for client side and {@link #runServerSideImpl()} for server side.  If the TcpEndPoint should 
	 * be client side it creates a socket before executing the handling method.
	 */
	protected final void work()
	{
		// Get settings for object stream reset interval and alternative reset method from the associated TcpCommunicationManager
		this.setObjectStreamResetInterval(commManager.getObjectStreamResetInterval());
		this.setUseAlternativeResetMethod(commManager.isUsingAlternativeResetMethod());
						
		modeImpl.work();
	}
   
   /**
    * Called to notify the {@link TcpCommunicationManager} that the endpoint has been connected. This is normally done by the {@link TcpEndPointModeImpl}.
    * 
    * @since 2.0.1 (20041126)
    */
   protected void notifyEndPointConnected()
   {
      endPointConnectedNotificationSuccess = commManager.endPointConnectedInternal(TcpEndPoint.this); //Notify the TcpCommunicationManager that this endpoint is connected
   }
   
   /**
    * Called to notify the {@link TcpCommunicationManager} that the endpoint has been connected. This is normally done by the {@link TcpEndPointModeImpl}.
    * 
    * @since 2.0.1 (20041126)
    */
   protected void notifyEndPointLinkEstablished()
   {
      if( !endPointConnectedNotificationSuccess ) commManager.endPointConnectedInternal(TcpEndPoint.this); // Notify the TcpCommunicationManager that this endpoint is connected
      commManager.endPointLinkEstablishedInternal(TcpEndPoint.this); // Notify the TcpCommunicationManager that this endpoint has established a link
   }
   
   /**
    * Called to register the endpoint in the {@link TcpCommunicationManager}. This is normally done by the {@link TcpEndPointModeImpl}.
    * 
    * @since 2.0.1 (20041126)
    */
   protected boolean registerEndPoint()
   {
      if( super.getTcpConnectionData().isServerSide() )
      {
         return commManager.registerEndPoint(TcpEndPoint.this, false); // Register this (server side) endpoint in the TcpCommunicationManager and in endpoint group
      }
      return true;
   }
	
	/**
	 * Mode implementation interface.
	 */
	public interface TcpEndPointModeImpl
	{
      /**
       * Returns an EndPointIdentifier object identifying an address which the end point is to represent.
       *  
       * @return an EndPointIdentifier object identifying this end point.
       * 
       * @see TcpConnection#getRemoteAddress()
       */
		public EndPointIdentifier getEndPointIdentifier();
			
      /**
       * Gets a number uniquely identifying the TcpEndPoint. This number is meant to be used when handling 
       * connection collisions between two TcpCommunicationManager systems. The number returned by this method 
       * is guaranteed to be the same for the TcpEndPoint objects on each side of the connection.
       *       
       * @return a number uniquely identifying this TcpEndPoint, -1 if an error occurs. 
       */
		public long getMagicNumber();
		
      /**
       * Mode implementation worker method that handles setup (including connect for client side endpoints) and 
       * execution of the endpoint logic. 
       */
		public void work();
		
      /**
       * Checks the TcpEndPoint for errors.
       * 
       * @return <code>true</code> if everything is OK, otherwise <code>false</code>.
       */
		public boolean check();
	}
	
	/**
	 * Default client side mode implementation.
	 */
	public class TcpEndPointClientSideModeImpl implements TcpEndPointModeImpl
	{
      /**
       * Returns an EndPointIdentifier object identifying an address which the end point is to represent.
       *  
       * @return an EndPointIdentifier object identifying this end point.
       * 
       * @see TcpConnection#getRemoteAddress()
       */
		public EndPointIdentifier getEndPointIdentifier()
		{
			if(connectionData != null) return connectionData.getRemoteAddress();
			else return null;
		}
		
		/**
       * Gets a number uniquely identifying the TcpEndPoint. This number is meant to be used when handling 
       * connection collisions between two TcpCommunicationManager systems. The number returned by this method 
       * is guaranteed to be the same for the TcpEndPoint objects on each side of the connection.
       *       
       * @return a number uniquely identifying this TcpEndPoint, -1 if an error occurs. 
       */
		public long getMagicNumber()
		{
			return getRemoteAddress().getAddressAsLong();
		}	
		
		/**
		 * Client side mode implementation worker method. This method handles setup, including socket connect and 
       * execution of the endpoint logic. 
		 */
		public void work()
		{
			if( isDebugMode() ) logDebug("Connecting to " + this.getEndPointIdentifier()  + "...");
			boolean connectSuccessful = connect();
			
			if(connectSuccessful)
			{
            TcpEndPoint.this.notifyEndPointConnected(); // Notify the TcpCommunicationManager that this endpoint is connected
			}
			
			if( isDebugMode() ) logDebug("Initializing...");
			
			if( initClientSide(connectSuccessful) )
			{
				setLinkEstablished(true);  //Set "linkEstablished" to true if post connection negotiation was successful
				TcpEndPoint.this.notifyEndPointLinkEstablished(); // Notify the TcpCommunicationManager that this endpoint has established a link
				if( isDebugMode() ) logDebug("Link established.");
				runClientSideImpl();
			}
			else
			{
				if( isDebugMode() ) logDebug("Failed to establish link.");
				setLinkEstablished(false);
			}
		}
		
		/**
       * Perfors socket connect.
		 */
		protected boolean connect()
		{
			final Socket socket = createClientSocket(connectionData.getRemoteAddress());
			
			if(socket != null)
			{
				setSocket(socket);
				setConnected(true);
				return true;
			}
			else
			{
				setSocket(null);
				setConnected(false);
				return false;
			}
		}
		
		/**
       * Checks the TcpEndPoint for errors.
       * 
       * @return <code>true</code> if everything is OK, otherwise <code>false</code>.
       */
		public boolean check()
		{
			final boolean debug = isDebugMode();
			
			if( debug && !endPointActive ) logDebug("Check failed - endpoint no longer active.");
									
			if(endPointActive && connected) //If connected is false, then this TcpEndPoint is currently connecting
			{
				final boolean threadAlive = (TcpEndPoint.this).getThread().isAlive();
				boolean inputStreamOk;
			
				try
				{
					getSocket().getInputStream().available();
					inputStreamOk = true;
				}
				catch(Exception e)
				{
					if( debug ) log(Level.DEBUG, "Check failed (while checking stream)", e);
					inputStreamOk = false;
				}
				
				if(!threadAlive)
				{
					if( debug ) logDebug("Check failed - thread dead!");
					(TcpEndPoint.this).getThread().error();
				}
				
				return endPointActive && threadAlive && inputStreamOk;
			}
			else return endPointActive;
		}
	}
	
	/**
	 * Default server side mode implementation.
	 */
	public class TcpEndPointServerSideModeImpl implements TcpEndPointModeImpl
	{
      /**
       * Returns an EndPointIdentifier object identifying an address which the end point is to represent.
       *  
       * @return an EndPointIdentifier object identifying this end point.
       * 
       * @see TcpConnection#getRemoteAddress()
       */
		public EndPointIdentifier getEndPointIdentifier()
		{
			if((address == null) && (connectionData != null)) return connectionData.getRemoteAddress();
			else return address;
		}
		
		/**
       * Gets a number uniquely identifying the TcpEndPoint. This number is meant to be used when handling 
       * connection collisions between two TcpCommunicationManager systems. The number returned by this method 
       * is guaranteed to be the same for the TcpEndPoint objects on each side of the connection.
       *       
       * @return a number uniquely identifying this TcpEndPoint, -1 if an error occurs. 
       */
		public long getMagicNumber()
		{
			return getLocalAddress().getAddressAsLong();
		}
		
		/**
       * Server side mode implementation worker method. This method handles setup and 
       * execution of the endpoint logic.
		 */
		public void work()
		{
			if( isDebugMode() ) logDebug("Initializing connection from " + this.getEndPointIdentifier()  + "...");
			setConnected(true);
         
			TcpEndPoint.this.notifyEndPointConnected(); // Notify the TcpCommunicationManager that this endpoint is connected
			
			if( initServerSide() )
			{
				TcpEndPoint.this.registerEndPoint(); // Register this (server side) endpoint in the TcpCommunicationManager and in endpoint group
            setLinkEstablished(true);  //Set "linkEstablished" to true if post connection negotiation was successful
            TcpEndPoint.this.notifyEndPointLinkEstablished(); // Notify the TcpCommunicationManager that this endpoint has established a link
				if( isDebugMode() ) logDebug("Link established.");
				runServerSideImpl();
			}
			else
			{
				if( isDebugMode() ) logDebug("Failed to establish link.");
				setLinkEstablished(false);
			}
		}
		
		/**
       * Checks the TcpEndPoint for errors.
       * 
       * @return <code>true</code> if everything is OK, otherwise <code>false</code>.
       */
		public boolean check()
		{
			final boolean debug = isDebugMode();

			if( debug && !endPointActive ) logDebug("Check failed - endpoint no longer active.");			

			final boolean threadAlive = (TcpEndPoint.this).getThread().isAlive();
			boolean inputStreamOk;
			
			try
			{
				getSocket().getInputStream().available();
				inputStreamOk = true;
			}
			catch(Exception e)
			{
				if( debug ) log(Level.DEBUG, "Check failed (while checking stream)", e);
				inputStreamOk = false;
			}
						
			if(!threadAlive)
			{
				if( debug ) logWarning("Check failed - thread dead!");
				(TcpEndPoint.this).getThread().error();
			}

			return endPointActive && threadAlive && inputStreamOk;
		}
	}
}
