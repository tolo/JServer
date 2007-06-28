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
   TODO: Add lastMessageTime - the time when the last message was dispatched to this destination
   TODO: Flag indicating if the destination is blocked for receiving more requests
   TODO: Statistical & load balacing information information
   TODO: Inherit SubComponent to make it possible to view information in JAdmin? Or solve this in another way?
*/
package com.teletalk.jserver.tcp.messaging;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.teletalk.jserver.tcp.TcpEndPoint;
import com.teletalk.jserver.tcp.TcpEndPointGroup;
import com.teletalk.jserver.tcp.TcpEndPointIdentifier;
import com.teletalk.jserver.util.StringUtils;

/**
 * This class represents a remote messaging system to which messages can be sent. Destination is a subclass of 
 * {@link TcpEndPointGroup} and contains all endpoints associated with a certain destination. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.2
 */
public class Destination extends TcpEndPointGroup
{
	private long destinationId;
	private long clientId;
	/** @since 1.3.1 */
	private byte protocolVersion;
	
	private boolean error;
	private int errorCounter;
	private boolean connectingFirstEndPoint;
	private HashMap destinationMetaData;
	/** @since 1.3.1 Build 690 */
	private HashSet namedReceivers;
   
   private boolean allEndPointsDisconnected;
   
   /** @since 2.0 Build 757 */
   private int load;
	
	/**
	 * Creates a new Destination.
	 * 
	 * @param address the address of the remote remote messaging system this object is to represent.
	 */
	public Destination(TcpEndPointIdentifier address)
	{
		super(address);
		
		this.destinationId = -1;
		this.clientId = -1;
		this.protocolVersion = 1;
		
		this.error = false;
		this.errorCounter = 0;
		this.connectingFirstEndPoint = false;
		this.destinationMetaData = null;
		this.namedReceivers = new HashSet();
      
      this.allEndPointsDisconnected = false;
      
      this.load = -1;
	}

	/**
	 * Gets the unique id used to identify the local messaging system in the remote messaging manager.
	 * 
	 * @return the unique id used to identify the local messaging system client in the remote messaging manager.
	 */
	public synchronized long getClientId()
	{
		return this.clientId;
	}
	
	/**
	 * Sets the unique id used to identify the local messaging system in the remote messaging manager.
	 * 
	 * @param clientId the unique id used to identify the local messaging system in the remote messaging manager.
	 */
	protected synchronized void setClientId(long clientId)
	{
		this.clientId = clientId;
	}
	
	/**
	 * Gets the unique id of the remote messaging system represented by this destination object.
	 * 
	 * @return the unique id of the remote messaging system.
	 */
	public synchronized long getDestinationId()
	{
		return this.destinationId;
	}
	
	/**
	 * Sets the unique id of the remote messaging system represented by this destination object.
	 * 
	 * @param destinationId the unique id of the remote messaging system.
	 */
	protected synchronized void setDestinationId(long destinationId)
	{
		this.destinationId = destinationId;
	}
	
	/**
	 * Gets the protocol version to be used when communicating with the remote messaging system.
    * 
    * @since 1.3.1
	 */
	public byte getProtocolVersion()
	{
		return this.protocolVersion;
	}

	/**
	 * Sets the protocol version to be used when communicating with the remote messaging system.
    * 
    * @since 1.3.1
	 */
	public void setProtocolVersion(byte protocolVersion)
	{
		this.protocolVersion = protocolVersion;
	}
			
	/**
	 * Checks if an error has occurred creating the first connection to the remote messaging system this object represents.
	 * 
	 * @return true if an error has occurred creating the first connection to the remote messaging system this object represents, otherwise false. 
	 */
	public synchronized boolean isError()
	{
		return error;	
	}
	
	/**
	 * Sets the flag indicating if an error has occurred creating the first connection to the remote messaging system this object represents.
	 * 
	 * @param error boolean flag indicating if an error has occurred creating the first connection to the remote messaging system this object represents, otherwise false. 
	 */
	public void setError(boolean error)
	{
		synchronized(this)
		{
			this.error = error;
			if(!error)
			{
				errorCounter = 0;
			}
         
         this.notifyAll();
		}
	}
	
	/**
	 * Gets the number of consecutive errors that has occurred while trying to create the first connection to the remote messaging system this object represents.
	 * 
	 * @return the error count.
	 */
	public synchronized int getErrorCount()
	{
		return errorCounter;
	}
	
	/**
	 * Increments the counter for consecutive errors that has occurred while trying to create the first connection to the remote messaging system this object represents.
	 */
	public synchronized void incrementErrorCounter()
	{
		error = true;
		errorCounter++;
	}
	
	/**
	 * Checks if the MessagingManager is currently trying to create the first connection to the remote messaging system this object represents.
	 * 
	 * @return true if the MessagingManager is currently trying to create the first connection to the remote messaging system this object represents, otherwise false.
	 */
	public synchronized boolean isConnectingFirstEndPoint()
	{
		return connectingFirstEndPoint;
	}
	
	/**
	 * Sets the flag indicating if the MessagingManager is currently trying to create the first connection to the remote messaging system this object represents.
	 * 
	 * @param connectingFirstEndPoint the flag inidcating if the first endpoint is currently trying to connect.
	 */
	public synchronized void setConnectingFirstEndPoint(boolean connectingFirstEndPoint)
	{
		this.connectingFirstEndPoint = connectingFirstEndPoint;
	}
   
   /**
    * Gets a string containing server name and address.
    * 
    * @since 2.0.1
    */
   public String getName()
   {
      String remoteServerName = (String)this.getDestinationMetaData( MessagingManager.SERVER_NAME_METADATA_KEY );
            
      String addressAsString = (this.address != null) ? this.address.getAddressAsString() : "<n/a>";
      
      if( remoteServerName != null )
      {
         return remoteServerName + " (" + addressAsString + ")";
      }
      else
      {
         return addressAsString;
      }
   }
   
   /**
    * Gets the server name value set in the meta data of this destination.  
    * 
    * @since 2.0.1
    */
   public String getServerName()
   {
      return (String)this.getDestinationMetaData(MessagingManager.SERVER_NAME_METADATA_KEY);
   }
   
   /**
    * Gets the server name value set in the meta data of this destination.  
    * 
    * @since 2.0.1
    */
   public String getServerVersion()
   {
      return (String)this.getDestinationMetaData(MessagingManager.SERVER_VERSION_METADATA_KEY);
   }
	
	/**
	 * Gets the load of the remote messaging system. 
	 * 
	 * @since 2.0 Build 757
	 */
   public int getLoad()
   {
      return load;
   }
   
   /**
    * 
    * @since 2.1.1 (20060109)
    */
   public boolean isProxy()
   {
      Boolean proxyingEnabled = (Boolean)this.getDestinationMetaData(MessagingManager.PROXYING_ENABLED_METADATA_KEY);
      if( proxyingEnabled != null ) return proxyingEnabled.booleanValue();
      else return false;
   }
	
	private void metaDataUpdated(final boolean updateNamedReceivers)
	{
	   if( updateNamedReceivers )
	   {
		   if( this.destinationMetaData != null )
		   {
			   List namedReceiverList = (List)this.destinationMetaData.get(MessagingManager.NAMED_MESSAGE_RECEIVER_METADATA_KEY);
		      if( namedReceiverList != null )
		      {
		   		this.namedReceivers.clear();
		   		this.namedReceivers.addAll(namedReceiverList);
		      }
		   }
		   else this.namedReceivers.clear();
	   }
	   
	   if( this.destinationMetaData != null )
	   {
	      Integer loadValue = (Integer)this.destinationMetaData.get(MessagingManager.SERVER_LOAD_METADATA_KEY);
	      if( loadValue != null ) this.load = loadValue.intValue();
	   }
	}
	
	/**
	 * Sets the meta data (and replaces the old) of the remote messaging system this object represents.
	 * 
	 * @param destinationMetaData the new meta data.
	 */
	public synchronized void setDestinationMetaData(final HashMap destinationMetaData)
	{
		this.destinationMetaData = destinationMetaData;
		this.metaDataUpdated(true);
	}
	
	/**
	 * Updates, possibly partially, the meta data of the remote messaging system this object represents.
	 * 
	 * @param destinationMetaData the new meta data.
	 * 
	 * @since 2.0 Build 757
	 */
	public synchronized void updateDestinationMetaData(final HashMap destinationMetaData)
	{
	   if( destinationMetaData == null ) return;
	   
	   if( this.destinationMetaData != null ) this.destinationMetaData.putAll(destinationMetaData);
	   else this.destinationMetaData = destinationMetaData;
	   this.metaDataUpdated(destinationMetaData.containsKey(MessagingManager.NAMED_MESSAGE_RECEIVER_METADATA_KEY));
	}
	
	/**
	 * Gets the meta data value matching the specified key.
	 * 
	 * @param key the name of the meta data value to get.
	 * 
	 * @return the meta data value matching the specified key.
	 */
	public synchronized Object getDestinationMetaData(final String key)
	{
		if( this.destinationMetaData != null )
		{
			return destinationMetaData.get(key);
		}
		else return null;
	}
	
	/**
	 * Gets all meta data for the remote messaging system
	 * 
	 * @return a HashMap containing key/value mapped meta data.
	 */
	public synchronized HashMap getDestinationMetaData()
	{
		return (this.destinationMetaData != null) ? (HashMap)this.destinationMetaData.clone() : null;
	}
	
	/**
	 * Checks if this destination has the specified message receiver.
    * 
	 * @param messageReceiverName a message receiver name.
	 * 
	 * @since 1.3.1 Build 690
	 */
	public synchronized boolean hasNamedReceiver(final String messageReceiverName)
	{
		return this.namedReceivers.contains(messageReceiverName);
	}
	
	/**
	 * Gets the names of the message receivers in the remote messaging system this destination object represents.
    * 	 * 
	 * @since 1.3.1 Build 690
	 */
	public synchronized String[] getNamedReceivers()
	{
		return (String[])this.namedReceivers.toArray(new String[0]);
	}
   
   /**
    * Gets the flag indicating if all endpoints in this destination has been disconnected since the last endpoint check.
    * 
    * @since 1.3.1 Build 694.
    */
   public boolean isAllEndPointsDisconnected()
   {
      return allEndPointsDisconnected;
   }

   /**
    * Sets the flag indicating if all endpoints in this destination has been disconnected since the last endpoint check.
    * 
    * @since 1.3.1 Build 694.
    */
   protected void setAllEndPointsDisconnected(boolean disconnectHeaderReceived)
   {
      this.allEndPointsDisconnected = disconnectHeaderReceived;
   }
   
   /**
    * Gets a String describing this Destination.
    * 
    * @return a String describing this Destination.
    */
   public String getDescription()
   {
      return this.getDescription(false);
   }
	
	/**
	 * Gets a String describing this Destination.
	 * 
	 * @return a String describing this Destination.
	 */
   protected String getDescription(boolean includeAddress)
	{
		StringBuffer description = new StringBuffer(50);
	   String remoteServerName = (String)this.getDestinationMetaData( MessagingManager.SERVER_NAME_METADATA_KEY );
		
		if( this.clientSide ) description.append("Client side destination ");
		else description.append("Server side destination ");
      
      String addressAsString = "";
      if( includeAddress ) addressAsString = (this.address != null) ? this.address.getAddressAsString() : "<n/a>";
		
		if( remoteServerName != null )
		{
         if( includeAddress ) description.append(remoteServerName).append(" (").append(addressAsString).append(") - ");
         else description.append(remoteServerName).append(" - ");
		}
		else
		{
		   description.append(addressAsString).append(" - ");
		}
		
		description.append(String.valueOf(this.size()));
		description.append(" endpoints");
		
		if( this.error )
		{
		   String errorCountString = ", error count: " + this.errorCounter + ""; 
		   description.append(String.valueOf(errorCountString));
		}
		
		if( !this.error )
		{
			if( this.load >= 0 )
			{
			   description.append(", load: ");
			   description.append(String.valueOf(this.load));
			}
		   
			if( !this.connectingFirstEndPoint )
			{
			   description.append(", protocol version: ");
			   description.append(String.valueOf(this.protocolVersion));
			}
		}
      
      String[] namedReceivers = this.getNamedReceivers(); 
      if( (namedReceivers != null) && (namedReceivers.length > 0) )
      {
         description.append(", named receivers: ");
         description.append(StringUtils.toString(namedReceivers));
      }
		
		return description.toString();
	}
   
   /**
    * Gets a String describing this Destination.
    * 
    * @return a String describing this Destination.
    */   
   public String toString()
   {
      return this.getDescription(true);
   }
   
   /**
    * Called when an endpoint gets disconnected.<br>
    * <br>
    * <b><i>Note:</i></b> If this method is overridden by a subclass, it is imperative that the super class implementation is called.
    * 
    * @param endPoint the object representing the endpoint.
    */
   protected void endPointDisconnected(final TcpEndPoint endPoint)
   {
      super.endPointDisconnected(endPoint);
      
      synchronized(this) // For MessagingManager.performClientSideDestinationsCheck()...
      {
         this.notifyAll();
      }
   }
}
