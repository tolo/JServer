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
package com.teletalk.jserver.tcp.messaging;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.teletalk.jserver.tcp.TcpEndPointIdentifier;
import com.teletalk.jserver.util.InputStreamer;
import com.teletalk.jserver.util.OutputStreamer;
import com.teletalk.jserver.util.Streamable;

/**
 * Message header class used when sending a message from one messaging system to another.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.2
 */
public class MessageHeader implements Externalizable, Streamable
{
   public static final int UNDEFINED = -1;
   
	/** Header type constant representing a standard message header. @since 1.3.1, build 670. */
   public static final byte STANDARD_HEADER = 0x00;
   /** Header type constant representing a header used for transporting an update of messaging system meta data for the remote system. @since 1.3.1, build 670. */
   public static final byte META_DATA_UPDATE_HEADER = 0x01;
   /** Header type constant representing a header used for checking the status of a endpoint connection. @since 1.3.1, build 670. */
   public static final byte ENDPOINT_CHECK_HEADER = 0x02;
   /** Header type constant representing a header used for checking the status of a endpoint connection. @since 1.3.1, build 694. */
   public static final byte DISCONNECT_HEADER = 0x03;
   /** Header type constant representing a header used for dispatching RPC messages. @since 2.0. */
   public static final byte RPC_HEADER = 0x04;
   /** Header type constant representing a header used to indicate that an error occurred when receiving/processing a message. @since 2.0 Build 762. */
   public static final byte MESSAGE_PROCESSING_ERROR_HEADER = 0x05;
   /** Header type constant representing a header used for server administration calls (to a {@link com.teletalk.jserver.tcp.messaging.admin.ServerAdministrationHandler}). @since 2.0.1 (20040924). */
   public static final byte SERVER_ADMINISTRATION_HEADER = 0x06;
   /** Header type constant representing an access denied response. @since 2.0.2 (20050331) */
   public static final byte ACCESS_DENIED_HEADER = 0x7F; 
      
   /** The serial version id of this class. */
	static final long serialVersionUID = 4232525261746359084L;
	
	/** The version number of the serialized data of this class. @deprecated as of 1.3.2 (Protocol 4) */
 	public static final byte SERIAL_DATA_VERSION = 0x06;
   
   
   /** @since 1.3.1 */
   protected transient byte protocolVersion = 1;
   
   /** @since 1.3.1, build 670. <b><i>Note:</i></b> Header type values of 0x1F and lower are reserved for JServer. */
   private byte headerType;
      
	// Message routing fields
	private long senderId;
	private long messageId;
	private long responseToId;
	
	// Message meta-data and body fields
	private int messageType;
	private long bodyLength;
	private String description;
	
	private long timeToLive = UNDEFINED; // Since serial version 4
	
	private boolean asynch;  
	
	private HashMap customHeaderFields;
	
	private HashMap messagingSystemMetaData; // Since serial version 6
	
	/**
	 * Public no-arg constructor (used during deserialization).
	 */
	public MessageHeader()
	{
		this(UNDEFINED, UNDEFINED, UNDEFINED, UNDEFINED, UNDEFINED, null);
	}
	
	/**
	 * Copy constructor.
	 * 
	 * @param otherHeader another MessageHeader object from which data will be copied.
	 */
	public MessageHeader(final MessageHeader otherHeader)
	{
		this(otherHeader.messageType, otherHeader.bodyLength, otherHeader.senderId, otherHeader.messageId, otherHeader.responseToId, otherHeader.description);
      
      this.protocolVersion = otherHeader.protocolVersion;
      this.headerType = otherHeader.headerType;
      this.timeToLive = otherHeader.timeToLive;
      this.asynch = otherHeader.asynch;
		
		this.customHeaderFields = otherHeader.customHeaderFields;
		if( this.customHeaderFields != null ) this.customHeaderFields = (HashMap)this.customHeaderFields.clone();
      
      this.messagingSystemMetaData = otherHeader.messagingSystemMetaData;
      if( this.messagingSystemMetaData != null ) this.messagingSystemMetaData = (HashMap)this.messagingSystemMetaData.clone();
	}
				
	/**
	 * Creates a new MessageHeader.
	 * 
	 * @param messageType the user defined message type.
	 */
	public MessageHeader(int messageType)
	{
		this(messageType, UNDEFINED, UNDEFINED, UNDEFINED, UNDEFINED, null);
	}
	
	/**
	 * Creates a new MessageHeader.
	 *
	 * @param messageType the user defined message type.
	 * @param description a textual description of the message.
	 */
	public MessageHeader(int messageType, String description)
	{
		this(messageType, UNDEFINED, UNDEFINED, UNDEFINED, UNDEFINED, description);
	}
	
	/**
	 * Creates a new MessageHeader.
	 * 
	 * @param messageType the user defined message type.
	 * @param bodyLength the length in bytes of the message body.
	 */
	public MessageHeader(int messageType, long bodyLength)
	{
		this(messageType, bodyLength, UNDEFINED, UNDEFINED, UNDEFINED, null);
	}
		
	/**
	 * Creates a new MessageHeader.
	 * 
	 * @param type the message type.
	 * @param bodyLength the length in bytes of the message body.
	 * @param description a textual description of the message.
	 */
	public MessageHeader(int type, long bodyLength, String description)
	{
		this(type, bodyLength, UNDEFINED, UNDEFINED, UNDEFINED, description);
	}
	
	/**
	 * Creates a new MessageHeader.
	 * 
	 * @param messageType the user defined message type.
	 * @param bodyLength the length in bytes of the message body.
	 * @param responseToId the id of the message that the message associated with this header is a response to.
	 * @param description a textual description of the message.
	 */
	public MessageHeader(int messageType, long bodyLength, long responseToId, String description)
	{
		this(messageType, bodyLength, UNDEFINED, UNDEFINED, responseToId, description);
	}
	
	/**
	 * Creates a new MessageHeader.
	 * 
	 * @param messageType the user defined message type.
	 * @param bodyLength the length in bytes of the message body.
	 * @param senderId the id of the sender of the message associated with this header. This parameter is normally set by {@link MessagingManager}.
	 * @param messageId the id (positive integer) of the message associated with this header. This parameter is normally set by {@link MessagingManager}.
	 * @param responseToId the id of the message that the message associated with this header is a response to.
	 * @param description a textual description of the message.
	 */
	public MessageHeader(int messageType, long bodyLength, long senderId, long messageId, long responseToId, String description)
	{
		this.headerType = STANDARD_HEADER;
      
      this.messageType = messageType;
		this.bodyLength = bodyLength;
		this.senderId = senderId;
		this.messageId = messageId;
		this.responseToId = responseToId;
		this.description = description;
		this.timeToLive = UNDEFINED;
		this.asynch = false;
		
		this.customHeaderFields = null;
		
		this.messagingSystemMetaData = null;
	}
   
   /**
    * Gets the value of the header type field. This field is primarily intended for internal use. Values 0x00 to 0x0F are reserved.
    * 
    * @since 1.3.1, build 670.
    */
   public byte getHeaderType()
   {
      return this.headerType;
   }

   /**
    * Sets the value of the header type field. This field is primarily intended for internal use. Values 0x00 to 0x0F are reserved.
    * 
    * @since 1.3.1, build 670.
    */
   public void setHeaderType(final byte headerType)
   {
      this.headerType = headerType;
   }
   
   /**
    * Convenience method to check if this header represents an RPC message. 
    * 
    * @since 2.0.2 (20050401)
    */
   public boolean isRpcHeader() { return this.headerType == MessageHeader.RPC_HEADER; }
   
   /**
    * Convenience method to check if this header indicates that an error occurred in the remote server while processing the message.  
    * 
    * @since 2.0.2 (20050401)
    */
   public boolean isMessageProcessingErrorHeader() { return this.headerType == MessageHeader.MESSAGE_PROCESSING_ERROR_HEADER; }
   
   /**
    * Convenience method to check if this header represents a server administration message. 
    * 
    * @since 2.0.2 (20050401)
    */
   public boolean isServerAdministrationHeader() { return this.headerType == MessageHeader.SERVER_ADMINISTRATION_HEADER; }   
   
   /**
    * Convenience method to check if this header indicates that access was denied to execute the requested operation. 
    * 
    * @since 2.0.2 (20050401)
    */
   public boolean isAccessDeniedHeader() { return this.headerType == MessageHeader.ACCESS_DENIED_HEADER; }
	
	/**
	 * Gets the id of the sender of this message. This field is normally set by {@link MessagingManager}.
	 * 
	 * @return a <code>long</code> identifying the sender of this message.
	 */
	public long getSenderId()
	{
		return this.senderId;
	}
	
	/**
	 * Sets the id of the sender of this message. This field is normally set by {@link MessagingManager}.
	 * 
	 * @param senderId a <code>long</code> identifying the sender of this message.
	 */
	public void setSenderId(final long senderId)
	{
		this.senderId = senderId;
	}
	
	/**
	 * Gets the id of this message.
	 * 
	 * @return a <code>long</code> identifying this message.
	 */
	public long getMessageId()
	{
		return this.messageId;
	}
	
	/**
	 * Sets the id of this message.
	 * 
	 * @param messageId a <code>long</code> (positive) identifying this message.
	 */
	public void setMessageId(final long messageId)
	{
		this.messageId = messageId;
	}
	
	/**
	 * Gets the id of the message for which this message is a reply to.
	 * 
	 * @return a <code>long</code> identifying the message which this message is a reply to.
	 */
	public long getResponseToId()
	{
		return this.responseToId;
	}
	
	/**
	 * Sets the id of the message for which this message is a reply to.
	 * 
	 * @param replyToId a <code>long</code> (positive) identifying the message which this message is a reply to.
	 */
	public void setResponseToId(final long replyToId)
	{
		this.responseToId = replyToId;
	}
	
	/**
	 * Gets the user defined type of the message associated with this header.
	 * 
	 * @return an integer representing the user defined type of the message associated with this header.
	 */
	public int getMessageType()
	{
		return this.messageType;
	}
	
	/**
	 * Sets the user defined type of the message associated with this header.
	 * 
	 * @param type an integer representing the user defined type of the message associated with this header.
	 */
	public void setMessageType(final int type)
	{
		this.messageType = type;
	}
	
	/**
	 * Gets the length in bytes of the body of the message associated with this header.
	 * 
	 * @return the length in bytes of the body of the message associated with this header.
	 */
	public long getBodyLength()
	{
		return this.bodyLength;
	}
	
	/**
	 * Sets the length in bytes of the body of the message associated with this header.
	 * 
	 * @param bodyLength the length in bytes of the body of the message associated with this header.
	 */
	public void setBodyLength(final long bodyLength)
	{
		this.bodyLength = bodyLength;
	}
			
	/**
	 * Gets the textual description of the message associated with this header. 
	 * 
	 * @return the textual description of the message associated with this header.
	 */
	public String getDescription()
	{
		return this.description;
	}
	
	/**
	 * Sets the textual description of the message associated with this header.
	 * 
	 * @param description the textual description of the message associated with this header.
	 */
	public void setDescription(final String description)
	{
		this.description = description;
	}
			
	/**
	 * Gets a custom header field from this header.
	 *
	 * @param key the key/name of the custom header field.
	 * 
	 * @return the value of the custom header field with the specified key, or
	 * <code>null</code> if no such value exists.
	 */
	public Object getCustomHeaderField(final String key)
	{
		if( this.customHeaderFields != null ) return this.customHeaderFields.get(key);
		else return null;
	}
   
   /**
    * Removes a custom header field from this header.
    *
    * @param key the key/name of the custom header field.
    * 
    * @return the value of the custom header field with the specified key, or
    * <code>null</code> if no such value exists.
    */
   public Object removeCustomHeaderField(final String key)
   {
      if( this.customHeaderFields != null ) return this.customHeaderFields.remove(key);
      else return null;
   }
	
	/**
	 * Gets all custom header fields from this header.
	 * 
	 * @return a map containing all the custom header fields contained in this
	 * header.
	 */
	public Map getCustomHeaderFields()
	{
		if( this.customHeaderFields != null ) return (Map)this.customHeaderFields.clone();
		else return new HashMap();
	}
	
	/**
	 * Set a custom header field in this header. If the value already exists for
	 * the specified key, it will be replaced.
	 * 
	 * @param key the key/name of the custom header field.
	 * @param value the value of the custom header field.
	 */
	public void setCustomHeaderField(final String key, final Serializable value)
	{
      if( key == null ) return;
      
      if( this.customHeaderFields == null ) this.customHeaderFields = new HashMap();
      this.customHeaderFields.put(key, value);
   }
	
	/**
	 * Copies all the custom header fields mappings from the map specified by parameter <code>fields</code>, 
	 * into this MessageHeader. These mappings will replace any already existing mappings with identical key names.<br>
	 * <br>
	 * <i>Note:</i> Only string keys are allowed!
	 * 
	 * @param fields a java.util.Map containing custom header mappings.
	 */
	public void setCustomHeaderFields(final Map fields)
	{
		if( fields == null ) return;
		
		if( this.customHeaderFields == null ) this.customHeaderFields = new HashMap();
		
		this.customHeaderFields.putAll(fields);
	}
   
   /**
    * Checks if a custom header field exists in this header.
    *
    * @param key the key/name of the custom header field.
    * 
    * @return <code>true</code> if the key exists, otherwise <code>false</code>.
    * 
    * @since 2.0.1 (20040924)
    */
   public boolean hasCustomHeaderField(final String key)
   {
      if( this.customHeaderFields != null ) return this.customHeaderFields.containsKey(key);
      else return false;
   }
	
	/**
	 * Get meta data about the sending messaging system.
	 * 
	 * @return a HashMap containing meta data.
	 * 
	 * @since 1.3.1, build 670.
	 */
	public HashMap getMessagingSystemMetaData()
	{
		return messagingSystemMetaData;
	}
	
	/**
	 * Get meta data about the sending messaging system.
	 * 
	 * @since 1.3.1, build 670.
	 */
	public void setMessagingSystemMetaData(final HashMap messagingSystemMetaData)
	{
		this.messagingSystemMetaData = messagingSystemMetaData;
	}
	
	/**
	 * Gets the amount of time (ms) before this message is considered to be obsolete. 
	 */
	public long getTimeToLive()
	{
		return this.timeToLive;
	}

	/**
	 * Sets the amount of time (ms) before this message is considered to be obsolete.
	 */
	public void setTimeToLive(final long timeToLive)
	{
		this.timeToLive = timeToLive;
	}
	
	/**
	 * Gets the flag indicating if the message associated with this header is an asynchronous message.
    * 
    * @since 1.3.1, build 670.
	 */
	public boolean isAsynch()
	{
      return this.asynch;
	}

	/**
	 * Sets the flag indicating if the message associated with this header is an asynchronous message.
    * 
    * @since 1.3.1, build 670.
	 */
	public void setAsynch(final boolean asynch)
	{
		this.asynch = asynch;
	}
   
   /**
    * Checks if the sender of this message header expects a response. If protocol version > 3 this method checks if the 
    * message was sent asynchronously, otherwise the value of the "response to id" field is checked to see 
    * if this message is a response message itself. 
    *  
    * @return <code>true</code> if sender expects a response, otherwise <code>false</code>.
    */
   public boolean expectingResponse()
   {
      if( this.protocolVersion > 3)
      {
         return !this.asynch;
      }
      else
      {
         return (this.responseToId != UNDEFINED);
      }
   }
   
   /**
    * Sets the protocol version used by the endpoint on which this header is to be sent or was received on.
    * 
    * @since 1.3.1, build 670. 
    */
   public void setProtocolVersion(final byte protocolVersion)
   {
      this.protocolVersion = protocolVersion;
   }
   
   /**
    * Sets the protocol version used by the endpoint on which this header is to be sent or was received on.
    * 
    * @since 2.0, build 762. 
    */
   public byte getProtocolVersion()
   {
      return this.protocolVersion;
   }
   
   /**
    * Resets all message routing fields (senderId, messageId and responseToId) in this header.
    * 
    * @since 2.1.6 (20070507)
    */
   public void resetMessageRoutingFields()
   {
      this.senderId = UNDEFINED;
      this.messageId = UNDEFINED;
      this.responseToId = UNDEFINED;
   }
	
	/**
	 * Gets a string representation of this MessageHeader.
	 * 
	 * @return a string representation of this MessageHeader.
	 */
	public String toString()
	{
		return "MessageHeader[" +
											"header type: " + this.headerType +
				 							", message type: " + this.messageType + 
											", sender id: " + this.senderId + ((this.senderId != UNDEFINED) ? " (" + TcpEndPointIdentifier.parseTcpEndPointIdentifier(this.senderId) + ")" : "") +  
											", message id: " + this.messageId + 
											", reply to message id: " + this.responseToId +
											", body length: " + this.bodyLength + 
											", TTL: " + this.timeToLive +
											", asynch: " + this.asynch +
											", description: " + ((this.description != null) ? this.description : "") + 
											", custom header fields: " + ((this.customHeaderFields != null) ? this.customHeaderFields.toString() : "") +
                                 ", protocol version: " + this.protocolVersion + "]";
	}
	
   /**
    * Deserializates the state of this object from the specified {@link InputStreamer}.
    * 
    * @param input the {@link InputStreamer} to read data from.
    * 
    * @throws IOException if an I/O error occurs.
    */
	public void read(final InputStreamer input) throws IOException
	{
		// No version is read - protocol version is to be used instread
		
      this.headerType = input.readByte();
     
		this.senderId = input.readLong();
		this.messageId = input.readLong();
		this.responseToId = input.readLong();
		this.messageType = input.readInt();
		this.bodyLength = input.readLong();
		this.description = input.readUTF();
		this.timeToLive = input.readLong();
		this.asynch = input.readBoolean();

		this.customHeaderFields = readHashMap(input);
      
      this.messagingSystemMetaData = readHashMap(input);
	}
	
   /**
    * Serializates the state of this object to the specified {@link OutputStreamer}.
    * 
    * @param output the {@link OutputStreamer} to write data to.
    * 
    * @throws IOException if an I/O error occurs.
    */
	public void write(final OutputStreamer output) throws IOException
	{
		// No version is written - protocol version is to be used instread
		
      output.writeByte(this.headerType);
      
		output.writeLong(this.senderId);
		output.writeLong(this.messageId);
		output.writeLong(this.responseToId);
		output.writeInt(this.messageType);
		output.writeLong(this.bodyLength);
		output.writeUTF(this.description != null ? this.description : "");
		output.writeLong(this.timeToLive);
		output.writeBoolean(this.asynch);
		
		writeHashMap(this.customHeaderFields, output);
      
      writeHashMap(this.messagingSystemMetaData, output);
	}
	
	/**
    * Internal method for deserializing a HashMap from an InputStreamer.
	 */
	private static HashMap readHashMap(final InputStreamer input) throws IOException
	{
		HashMap map = null;
		
		int numberOfFields = input.readInt();
      if( numberOfFields > 0 ) map = new HashMap();
      
      String key;
      Object value;
      int valueType;
            
		for(int i=0; i<numberOfFields; i++)
		{
         key = input.readUTF();
         valueType = input.readByte();         

         if( valueType == 1 ) // UTF String
         {
            value = input.readUTF();
         }
         else if( valueType == 2 ) // Object
         {
            try{
            value = input.getContextObjectInputStream().readObject();
            }catch(ClassNotFoundException e){throw new IOException("Caught ClassNotFoundException: " + e);}
         }
         else // Null
         {
            value = null;
         }
         
         map.put(key, value);         
		}
		
		return map;
	}
	
	/**
    * Internal method for serializing a HashMap to an OutputStreamer.
	 */
	private static void writeHashMap(final HashMap map, final OutputStreamer output) throws IOException
	{
		if( map != null )
		{
			output.writeInt(map.size());
			Iterator it = map.keySet().iterator();
			Object key;
			Object value;
         
			while(it.hasNext())
			{
				key = it.next();
            if( !(key instanceof String) ) continue;
            
				output.writeUTF((String)key);
				value = map.get(key);
            
				if( value == null )
            {
               output.writeByte(0); // Write type
            }
            else if( value instanceof String )
            { 
               output.writeByte(1); // Write type
               output.writeUTF((String)value);
            }
				else // Object
            {
               output.writeByte(2); // Write type
               output.getContextObjectOutputStream().writeObject(value);
            } 
			}
		}
		else
		{
			output.writeInt(0);
		}
	}
   
   
   /* ### EXTERNALIZABLE METHODS (FOR BACKWARDS COMPATABILITY) ### */
   
   
   /**
    * Serialization (Externalizable) method.
    * 
    * @param out the stream on which to serialize objects of this class. 
    */
   public void writeExternal(final ObjectOutput out) throws IOException
   {
      switch(this.protocolVersion)
      {
         case 1:
         {
            this.writeExternalV1(out);
            break;
         }
         case 2:
         {
            this.writeExternalV2(out);
            break;
         }
         default:
         {
            this.writeExternalV3(out);
            break;
         }
      }
   }
   
   /**
    * Serialization (Externalizable) method.
    * 
    * @param out the stream on which to serialize objects of this class. 
    */
   private void writeExternalV1(final ObjectOutput out) throws IOException
   {
      // Write serial data version
      out.writeByte(SERIAL_DATA_VERSION);
      
      out.writeLong(this.senderId);
      out.writeLong(this.messageId);
      out.writeLong(this.responseToId);
      out.writeInt(this.messageType);
      out.writeLong(this.bodyLength);
      out.writeObject(this.description);
      
      // Write version 3 data
      if( (this.customHeaderFields != null) && (this.customHeaderFields.size() == 0) ) out.writeObject(null);
      else out.writeObject(this.customHeaderFields);
   }
   
   /**
    * Serialization (Externalizable) method.
    * 
    * @param out the stream on which to serialize objects of this class. 
    */
   private void writeExternalV2(final ObjectOutput out) throws IOException
   {
      this.writeExternalV1(out);
      
      // Write version 4 data
      out.writeLong(this.timeToLive);
   }
   
   /**
    * Serialization (Externalizable) method.
    * 
    * @param out the stream on which to serialize objects of this class. 
    */
   private void writeExternalV3(final ObjectOutput out) throws IOException
   {
      // Remove NamedMessageReceiver.NamedMessageReceiverMetaDataKey from customHeaderFields, and write it separately...
      Object namedReceiversValue = null;  
      if( this.customHeaderFields != null ) namedReceiversValue = this.customHeaderFields.remove(MessagingManager.NAMED_MESSAGE_RECEIVER_METADATA_KEY); 
      
      this.writeExternalV1(out);
      
      if( namedReceiversValue != null ) this.customHeaderFields.put(MessagingManager.NAMED_MESSAGE_RECEIVER_METADATA_KEY, namedReceiversValue);
      
      // Write version 4 data
      out.writeLong(this.timeToLive);
      
      // Write version 5 data
      out.writeObject(namedReceiversValue);
   }
   
   /**
    * Deserialization (Externalizable) method.
    * 
    * @param in the stream from which to deserialize objects of this class.
    */
   public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException
   {
      // Read serial data version
      byte serialDataVersion = in.readByte();
      
      this.senderId = in.readLong();
      this.messageId = in.readLong();
      this.responseToId = in.readLong();
      this.messageType = in.readInt();
      this.bodyLength = in.readLong();
      this.description = (String)in.readObject();
      
      if( serialDataVersion >= 0x03 ) // Read version 3 data
      {
         this.customHeaderFields = (HashMap)in.readObject();
      }
      if( serialDataVersion >= 0x04 ) // Read version 4 (protocol version 2) data
      {
         this.timeToLive = in.readLong();
      }
      if( serialDataVersion >= 0x05 ) // Read version 5 (protocol version 3) data
      {
         Object namedReceiversValue = in.readObject(); 
         if( namedReceiversValue != null ) this.setCustomHeaderField(MessagingManager.NAMED_MESSAGE_RECEIVER_METADATA_KEY, (Serializable)namedReceiversValue);
      }
   }
}
