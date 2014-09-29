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
public final class QueueItemTransferResponse extends QueueItemResponse
{
	/** The serial version id of this class. */
	static final long serialVersionUID = 2125117002045746807L;

	//private transient QueueItem item;
	
	private String itemId;
	
	/** Default no-arg constructor used during  deserialization.	 */
	public QueueItemTransferResponse(){}
			
	/**
	 * Creates a new QueueItemTransferResponse.
	 * 
	 * @param address the destination address.
	 * @param item the QueueItem that a response is to be sent for.
	 * @param responseType the type of this response.
	 */
	public QueueItemTransferResponse(final EndPointIdentifier address, final QueueItem item, final byte responseType)
	{
		super(address, responseType);
				
		//this.item = item;
		this.itemId = item.getId();
	}
	
	public final void execute(final QueueManager queueManager)
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
	
	public final void abort(QueueManager queueManager)
	{
		queueManager.queueItemTransferResponseAborted(this);
	}
		
	/**
	 * Gets the QueueItem that this response is for. Note that this method only will return a valid QueueItem when called by the sender of the response.
	 * 
	 * @return the QueueItem that this response is for.
	 */
	/*public final QueueItem getItem()
	{
		return this.item;	
	}*/
	
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
      {
			toStringString = this.getClass().getName() + "(" + address + ", item id: " + itemId + ", type: "  + super.translateCodeToString(super.responseType) + ")";
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
		
		//out.writeByte(this.responseType); // Write response type
		out.writeUTF(this.itemId); // Write item id
	}
	
	/**
	 * Deserialization (Externalizable) method.
	 * 
	 * @param in the stream from which to deserialize objects of this class.
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		super.readExternal(in);		

		//this.responseType = in.readByte(); // Read response type
		this.itemId = in.readUTF(); // Read item id
	}
}
