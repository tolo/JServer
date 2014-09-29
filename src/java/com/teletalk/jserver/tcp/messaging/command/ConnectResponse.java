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

/**
 * Response class used during handshaking procedures between two messaging systems. 
 * 
 * @see com.teletalk.jserver.tcp.messaging.MessagingEndPoint
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.2
 */
public class ConnectResponse implements Externalizable
{
	/** The serial version id of this class. */
	static final long serialVersionUID = 8924229249086348373L;
	
	/** The version number of the serialized data of this class. */
 	public static final byte SERIAL_DATA_VERSION = 0x03;	
	
   /** @since 1.3.1 */
   private byte protocolVersion;
	private long clientId;
	private HashMap messagingSystemMetaData;
   /** @since 2.0.1 (20041125) */
   private boolean secondaryResponseSuccess;
	
	/**
	 * Default no arg constructor.
	 */
	public ConnectResponse()
	{
      this.protocolVersion = 1;
      this.clientId = -1;
		this.messagingSystemMetaData = null;
      this.secondaryResponseSuccess = false;
	}
   
   /**
    * Creates a new ConnectResponse used as response for secondary connections (all but the first).
    * 
    * @since 2.0.1 (20041125)
    */
   public ConnectResponse(boolean success)
   {
      this.secondaryResponseSuccess = success;
   }
	
	/**
	 * Creates a new ConnectResponse used as response for primary connections (only the first).
	 * 
	 * @param clientId the id by which the receiving messaging system will be known in the sending messaging system. 
	 * @param messagingSystemMetaData the meta data of the sending messaging system.
	 */
	public ConnectResponse(long clientId, HashMap messagingSystemMetaData, byte protocolVersion)
	{
      this.clientId = clientId;
		this.messagingSystemMetaData = messagingSystemMetaData;
		this.protocolVersion = protocolVersion;
      this.secondaryResponseSuccess = false;
	}
	
	/**
	 * Gets the id by which the receiving messaging system will be known in the sending messaging system. 
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
    * Gets the highest protocol version the sending messaging system can use.
    * 
    * @since 1.3.1
    */
   public byte getProtocolVersion()
   {
      return this.protocolVersion;
   }
   
   /**
    * Gets the flag indicating if a secondary endpoint failed to connect. 
    */
   public boolean isSecondaryResponseSuccess()
   {
      return secondaryResponseSuccess;
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
      
      // Write version 2 data
      out.writeByte(this.protocolVersion);
      
      // Write version 3 data
      out.writeBoolean(this.secondaryResponseSuccess);
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
      
      // Read version 2 data
      if( serialDataVersion >= 2 )
      {
         this.protocolVersion = in.readByte();
         // Read version 3 data
         if( serialDataVersion >= 3 )
         {
            this.secondaryResponseSuccess = in.readBoolean();
         }
      }
	}
}
