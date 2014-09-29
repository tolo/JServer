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
import com.teletalk.jserver.queue.QueueItem;
import com.teletalk.jserver.queue.QueueManager;
import com.teletalk.jserver.queue.RemoteQueueSystem;

/**
 * Command class for response commands that indicate that a certain QueueItem is completed.
 *  
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public final class QueueItemCompletionResponse extends QueueItemResponse
{
	/** The serial version id of this class. */
	static final long serialVersionUID = 6034743691346110090L;
	
	private transient QueueItem item;
	
	private String itemId;
	private Object responseData;
		
	/** Default no-arg constructor used during  deserialization.	 */
	public QueueItemCompletionResponse(){}
	
	/**
	 * Creates a new QueueItemCompletionResponse.
	 * 
	 * @param address the destination address.
	 * @param item the QueueItem that a response is to be sent for.
	 * @param responseType the type of this response.
	 */
	public QueueItemCompletionResponse(final EndPointIdentifier address, final QueueItem item, final byte responseType)
	{
		super(address, responseType);
		
		this.item = item;
		this.itemId = item.getId();
		this.responseData = null;
		//this.responseType = responseType;
	}
	
	/**
	 * Creates a new QueueItemCompletionResponse.
	 * 
	 * @param address the destination address.
	 * @param item the QueueItem that a response is to be sent for.
	 * @param responseType the type of this response.
	 * @param responseData an arbitrary data object to be transferred with this response.
	 */
	public QueueItemCompletionResponse(final EndPointIdentifier address, final QueueItem item, final byte responseType, final Object responseData)
	{
		super(address, responseType);
		
		this.item = item;
		this.itemId = item.getId();
		this.responseData = responseData;
		//this.responseType = responseType;
	}
	
	public void execute(RemoteQueueSystem remoteQueueSystem)
	{
		super.execute(remoteQueueSystem);
	}
	
	public final void execute(final QueueManager queueManager)
	{
		switch(super.responseType)
		{
			case QUEUE_ITEM_DONE_SUCCESS :
				queueManager.queueItemSuccessResponseReceived(this);
			break;
			
			case QUEUE_ITEM_DONE_FAILURE :
				queueManager.queueItemFailureResponseReceived(this);
			break;
			
			case QUEUE_ITEM_DONE_CANCELLED :
				queueManager.queueItemCancelledResponseReceived(this);
			break;
			
			case QUEUE_ITEM_RELOCATION_REQUIRED :
				queueManager.queueItemRelocationRequiredResponseReceived(this);
			break;
			
			default: throw new RuntimeException("Invalid response type (" + responseType + ") for QueueItemCompletionResponse.");
		}
	}
	
	public final void abort(QueueManager queueManager)
	{
		queueManager.queueItemCompletionResponseAborted(this);
	}
	
	/**
	 * Gets the arbitrary data object transferred with this response.
	 * 
	 * @return the arbitrary data object transferred with this response.
	 */
	public final Object getResponseData()
	{
		return this.responseData;	
	}
	
	/**
	 * Gets the QueueItem that this response is for. Note that this method only will return a valid QueueItem when called by the sender of the response.
	 * 
	 * @return the QueueItem that this response is for.
	 */
	public final QueueItem getItem()
	{
		return this.item;	
	}
	
	/**
	 * Gets a string uniquely identifying the QueueItem that this response is for.
	 * 
	 * @return a string uniquely identifying the QueueItem that this response is for.
	 */
	public final String getItemId()
	{
		return this.itemId;	
	}
	
	/**
	 * Gets a string representation of this QueueItemResponse.
	 * 
	 * @return a string representation of this QueueItemResponse.
	 */
	public String toString()
	{
		if(toStringString == null) 
			toStringString = this.getClass().getName() + "(" + this.address + ", item id: " + this.itemId + ", type: "  + super.translateCodeToString(super.responseType) + ")";
		
		return toStringString;
	}
	
	/**
	 * Serialization (Externalizable) method.
	 * 
	 * @param out the stream on which to serialize objects of this class. 
	 */
	public void writeExternal(ObjectOutput out) throws IOException
	{
		super.writeExternal(out);
		
		out.writeUTF(this.itemId); // Write item id
		out.writeObject(this.responseData); // Write response data
	}
	
	/**
	 * Deserialization (Externalizable) method.
	 * 
	 * @param in the stream from which to deserialize objects of this class.
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		super.readExternal(in);		

		this.itemId = in.readUTF(); // Read item id
		this.responseData = in.readObject(); // Read response data
	}
}
