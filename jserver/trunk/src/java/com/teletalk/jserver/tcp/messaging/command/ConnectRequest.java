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
package com.teletalk.jserver.tcp.messaging.command;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;

import com.teletalk.jserver.tcp.messaging.MessagingManager;

/**
 * Request class used during handshaking procedures between two messaging systems. 
 * 
 * @see com.teletalk.jserver.tcp.messaging.MessagingEndPoint
 *  
 * @author Tobias Löfstrand
 * 
 * @since 1.2
 */
public class ConnectRequest implements Externalizable
{
	/** The serial version id of this class. */
	static final long serialVersionUID = 3873053509107173356L;
	
	/** The version number of the serialized data of this class. */
 	public static final byte SERIAL_DATA_VERSION = 0x02;
	
   /** @since 1.3.1 */
   private byte protocolVersion;	
	private long clientId;
	private HashMap messagingSystemMetaData;
	private boolean firstConnectRequest;
	
	/**
	 * Default no arg constructor for serialization (Externalizable).
	 */
	public ConnectRequest()
	{
      this.protocolVersion = 1;
      this.clientId = -1;
		this.messagingSystemMetaData = null;
		this.firstConnectRequest = false;
	}
	
	/**
	 * Creates a new ConnectRequest, using the protocol version specified by {@link MessagingManager#MESSAGING_PROTOCOL_VERSION}.
	 * 
	 * @param clientId if parameter <code>firstConnectRequest</code> is <code>true</code> this parameter will be 
	 * the id by which the receiving messaging system will be known in the sending messaging system. Otherwise, this parameter will be the 
	 * id by which the sending messaging system is known in the receiving messaging system.
	 * @param messagingSystemMetaData the meta data of the sending messaging system.
	 * @param firstConnectRequest boolean flag indicating if this is the first connect request for a certain destination (<code>true</code>) 
	 * or not (<code>false</code>) .
	 */
	public ConnectRequest(long clientId, HashMap messagingSystemMetaData, boolean firstConnectRequest)
	{
      this.clientId = clientId;
		this.messagingSystemMetaData = messagingSystemMetaData;
		this.firstConnectRequest = firstConnectRequest;
		this.protocolVersion = MessagingManager.MESSAGING_PROTOCOL_VERSION;
	}
	
	/**
	 * Creates a new ConnectRequest.
	 * 
	 * @param clientId if parameter <code>firstConnectRequest</code> is <code>true</code> this parameter will be 
	 * the id by which the receiving messaging system will be known in the sending messaging system. Otherwise, this parameter will be the 
	 * id by which the sending messaging system is known in the receiving messaging system.
	 * @param messagingSystemMetaData the meta data of the sending messaging system.
	 * @param firstConnectRequest boolean flag indicating if this is the first connect request for a certain destination (<code>true</code>) 
	 * or not (<code>false</code>) .
	 * @param protocolVersion the highest protocol version the sending messaging system can use.
	 */
	public ConnectRequest(long clientId, HashMap messagingSystemMetaData, boolean firstConnectRequest, byte protocolVersion)
	{
		this.clientId = clientId;
		this.messagingSystemMetaData = messagingSystemMetaData;
		this.firstConnectRequest = firstConnectRequest;
		this.protocolVersion = protocolVersion;
	}
				
	/**
	 * If the method {@link #isFirstConnectRequest()} returns <code>true</code>, the id returned by this 
	 * method will be the id by which the receiving messaging system will be known in the sending messaging system. 
	 * Otherwise, this parameter will be the id by which the sending messaging system is known in the receiving messaging system.
	 * 
	 * @return the unique id identifying a messaging system.
	 */
	public long getClientId()
	{
		return this.clientId;
	}
	
	/**
	 * Get meta data about the sending messaging system.
	 * 
	 * @return a HashMap containing meta data.
	 */
	public HashMap getMessagingSystemMetaData()
	{
		return messagingSystemMetaData;
	}
	
	/**
	 * Checks if this request is the first from a certain destination.
	 * 
	 * @return <code>true</code> if this request is the first from a certain destination, otherwise <code>false</code>.
	 */
	public boolean isFirstConnectRequest()
	{
		return this.firstConnectRequest;
	}
   
   /**
    * Gets the highest protocol version the sending messaging system can use.
    *
    * @since 1.3.1
    */
   public byte getProtocolVersion()
   {
      return this.protocolVersion;
   }
	
	/**
	 * Serialization (Externalizable) method.
	 * 
	 * @param out the stream on which to serialize objects of this class. 
	 */
	public void writeExternal(final ObjectOutput out) throws IOException
	{
		// Write command version
		out.writeByte(SERIAL_DATA_VERSION);
		
		out.writeLong(this.clientId);
		
		out.writeObject(this.messagingSystemMetaData);
		
		out.writeBoolean(this.firstConnectRequest);
      
      // Write version 2 data
      out.writeByte(this.protocolVersion);
	}
	
	/**
	 * Deserialization (Externalizable) method.
	 * 
	 * @param in the stream from which to deserialize objects of this class.
	 */
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException
	{
		// Read command version
      byte serialDataVersion = in.readByte();
		
		this.clientId = in.readLong();
		
		this.messagingSystemMetaData = (HashMap)in.readObject();
		
		this.firstConnectRequest = in.readBoolean();
      
      // Read version 2 data
      if( serialDataVersion >= 2 )
      {
         this.protocolVersion = in.readByte();
      }
	}
}
