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
package com.teletalk.jserver.queue.command;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.teletalk.jserver.comm.EndPointIdentifier;
import com.teletalk.jserver.queue.QueueSystemMetaData;
import com.teletalk.jserver.queue.RemoteQueueSystem;

/**
 * Baseclass for response commands sent between queue systems.
 * 
 * @see QueueItemCompletionResponse
 * @see QueueItemTransferResponse
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public abstract class QueueItemResponse extends QueueSystemCommand
{
	static final long serialVersionUID = -5203839038505359162L;
	
	/* QueueItem transfer response codes */
	public static final byte QUEUE_ITEM_TRANSFER_SUCCESS				= 0;
	public static final byte QUEUE_ITEM_TRANSFER_FAILURE				= 1;
	public static final byte QUEUE_ITEM_TRANSFER_QUEUE_FULL		= 2;
	
	/* QueueItem completion response codes */
	public static final byte QUEUE_ITEM_DONE_SUCCESS							= 3;
	public static final byte QUEUE_ITEM_DONE_FAILURE							= 4;
	public static final byte QUEUE_ITEM_DONE_CANCELLED						= 5;
	public static final byte QUEUE_ITEM_RELOCATION_REQUIRED			= 6;
	
	private static final String[] codeStrings = { "transfer success", 
																				"transfer failure",
																				"transfer failed (queue full)",
																				
																				"done success", 
																				"done failure", 
																				"done cancelled", 
																				"relocation required"};
	
	/**
	 * Translates a response code (type) into a String.
	 * 
	 * @param code the code to translate.
	 * 
	 * @return the code as a String object.
	 */
	public static final String translateCodeToString(byte code)
	{
		if(code < codeStrings.length)
			return codeStrings[code];
		else
			return Byte.toString(code);
	}
	
	protected byte responseType;
	
	//private QueueSystemMetaData queueSystemMetaData = null; // Make it possible to piggyback meta data with all responses
	
	// Queue status data
	private boolean inQueueBlocked = false;
	private int inQueueLength = -1;
	private int inQueueMaxLength = -1;
	
	/** Default no-arg constructor used during  deserialization.	 */
	public QueueItemResponse(){}
	
	/**
	 * Creates a new QueueItemResponse.
	 * 
	 * @param address the destination address.
	 * @param responseType the type of this response.
	 */
	public QueueItemResponse(final EndPointIdentifier address, final byte responseType)
	{
		super(address);
									   
		this.responseType = responseType;
	}
	
	public void execute(RemoteQueueSystem remoteQueueSystem)
	{
		// Update the RemoteQueueSystem object with the piggybacked meta data
		if(this.inQueueLength != -1)
		{
			remoteQueueSystem.remoteQueueSystemMetaDataUpdated(this.getQueueStatus());
		}
		
		super.execute(remoteQueueSystem);
	}
	
	/**
	 * Gets the type of this response.
	 * 
	 * @return the type of this response.
	 */
	public final byte getResponseType()
	{
		return responseType;
	}
	
	/**
	 */
	public final QueueSystemMetaData getQueueStatus()
	{
		QueueSystemMetaData queueSystemMetaData = new QueueSystemMetaData();
		queueSystemMetaData.setInQueueBlocked(this.inQueueBlocked);
		queueSystemMetaData.setInQueueLength(this.inQueueLength);
		queueSystemMetaData.setinQueueMaxLength(this.inQueueMaxLength);
		
		return queueSystemMetaData;
		//return this.queueSystemMetaData;
	}
	
	/**
	 */
	public final void setQueueStatus(QueueSystemMetaData queueSystemMetaData)
	{
		this.inQueueBlocked = queueSystemMetaData.isInQueueBlocked();
		this.inQueueLength = queueSystemMetaData.getInQueueLength();
		this.inQueueMaxLength = queueSystemMetaData.getinQueueMaxLength();
		//this.queueSystemMetaData = queueSystemMetaData;
	}
		
	/**
	 * Serialization (Externalizable) method.
	 * 
	 * @param out the stream on which to serialize objects of this class. 
	 */
	public void writeExternal(ObjectOutput out) throws IOException
	{
		super.writeExternal(out);
		
		out.writeByte(this.responseType); // Write response type
		//out.writeObject(this.queueSystemMetaData);
		out.writeBoolean(this.inQueueBlocked);
		out.writeInt(this.inQueueLength);
		out.writeInt(this.inQueueMaxLength);
	}
	
	/**
	 * Deserialization (Externalizable) method.
	 * 
	 * @param in the stream from which to deserialize objects of this class.
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		super.readExternal(in);		

		this.responseType = in.readByte(); // Read response type
		//this.queueSystemMetaData = (QueueSystemMetaData)in.readObject();
		this.inQueueBlocked = in.readBoolean();
		this.inQueueLength = in.readInt();
		this.inQueueMaxLength = in.readInt();
	}
}
