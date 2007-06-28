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

/**
 * Command class for response commands that are replys to QueueItemTransferRequests.
 * 
 * @see QueueItemTransferRequest
 *  
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public final class MultiQueueItemTransferResponse extends QueueItemResponse
{
	/** The serial version id of this class. */
	static final long serialVersionUID = 964102398498177618L;
	
	private String[] itemIds;
		
	/** Default no-arg constructor used during  deserialization.	 */
	public MultiQueueItemTransferResponse(){}
	
	/**
	 * Creates a new QueueItemTransferResponse.
	 * 
	 * @param address the destination address.
	 * @param items the ids of QueueItems that a response is to be sent for.
	 * @param responseType the type of this response.
	 */
	public MultiQueueItemTransferResponse(final EndPointIdentifier address, final QueueItem[] items, final byte responseType) 
	{
		super(address, responseType);
		
		this.itemIds = new String[items.length];
		
		for(int i=0; i<items.length; i++)
		{
			this.itemIds[i] = items[i].getId();
		}
		
		this.responseType = responseType;
	}
	
	public void execute(final QueueManager queueManager)
	{
		switch(super.responseType)
		{
			case QUEUE_ITEM_TRANSFER_SUCCESS :
         {
				queueManager.queueItemTransferredResponseReceived(this);
				break;
         }
			case QUEUE_ITEM_TRANSFER_FAILURE :
         {
				queueManager.queueItemTransferFailureResponseReceived(this);
				break;
         }
			case QUEUE_ITEM_TRANSFER_QUEUE_FULL :
         {
				queueManager.queueItemTransferFailureQueueFullResponseReceived(this);
				break;
         }
			default: throw new RuntimeException("Invalid response type (" + responseType + ") for QueueItemTransferResponse.");
		}
	}
	
	public void abort(QueueManager queueManager)
	{
		queueManager.queueItemTransferResponseAborted(this);
	}
	
	public String[] getItemIds()
	{
		return this.itemIds;
	}
	
	/**
	 * Gets a string representation of this MultiQueueItemTransferResponse.
	 * 
	 * @return a string representation of this MultiQueueItemTransferResponse.
	 */
	public String toString()
	{
		//Note that this method is used by equals(Object o) to test for equality.
		if(toStringString == null) 
		{
			toStringString = this.getClass().getName() + "(address: " + address + ", ids: " + QueueItem.concatIds(this.itemIds) + ")";
		}
		
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
		
		out.writeInt(this.itemIds.length); // Write number of item ids

		for(int i=0; i<this.itemIds.length; i++)
		{
			out.writeUTF(this.itemIds[i]); // Write item id
		}
	}
	
	/**
	 * Deserialization (Externalizable) method.
	 * 
	 * @param in the stream from which to deserialize objects of this class.
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		super.readExternal(in);		

		final int numberOfItemIds = in.readInt(); // Read number of item ids
		this.itemIds = new String[numberOfItemIds];

		for(int i=0; i<numberOfItemIds; i++)
		{
			this.itemIds[i] = in.readUTF(); // Read item id
		}
	}
}
