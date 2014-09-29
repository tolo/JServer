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
import com.teletalk.jserver.queue.QueueItemData;
import com.teletalk.jserver.queue.QueueManager;
import com.teletalk.jserver.queue.RemoteQueueSystem;

/**
 * Command class used to transfer multiple QueueItems to another queue system.
 *  
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public final class MultiQueueItemTransferRequest extends QueueSystemCommand
{
	/** The serial version id of this class. */
	static final long serialVersionUID = -8331269521077193938L;
	
	private transient QueueItem[] queueItems;
	
	/** Default no-arg constructor used during  deserialization.	 */
	public MultiQueueItemTransferRequest(){}
	
	/**
	 * Creates a new MultiQueueItemTransferRequest.
	 * 
	 * @param address the destination address.
	 * @param queueItems the QueueItems to be transferred.
	 */
	public MultiQueueItemTransferRequest(final EndPointIdentifier address, final QueueItem[] queueItems)
	{
		super(address);
		
		this.queueItems = queueItems;
	}
	
	public void execute(RemoteQueueSystem remoteQueueSystem)
	{
		// Initialize address field of queue item with the address of the remote queue system object.
		for(int i=0; i<this.queueItems.length; i++)
		{
			this.queueItems[i].setSenderReceiverAddress(remoteQueueSystem.getRemoteQueueSystemAddress());
		}
		
		super.execute(remoteQueueSystem);
	}
	
	public void execute(final QueueManager queueManager)
	{
		queueManager.queueItemTransferRequestReceived(this);
	}
	
	public void abort(final QueueManager queueManager)
	{
		queueManager.queueItemTransferRequestAborted(this);
	}
	
	/**
	 * Gets the actual objects (QueueItemData) that were wrapped in the QueueItems that were dispatched.
	 * 
	 * @return the actual objects (QueueItemData) that were wrapped in the QueueItem that were dispatched.
	 */
	public QueueItemData[] getItemData()
	{
		QueueItemData[] itemData = new QueueItemData[this.queueItems.length];
		for(int i=0; i<itemData.length; i++)
		{
			itemData[i] = this.queueItems[i].getItemData();
		}
		
		return itemData;
	}
	
	/**
	 * Gets the strings uniquely identifying the QueueItems that were sent with this command.
	 * 
	 * @return the strings uniquely identifying the QueueItems that were sent with this command.
	 */
	public String[] getItemIds()
	{
		String[] itemIds = new String[this.queueItems.length];
		for(int i=0; i<itemIds.length; i++)
		{
			itemIds[i] = this.queueItems[i].getId();
		}
		
		return itemIds;
	}
	
	/**
	 * Gets the QueueItems that were sent with this command.
	 * 
	 * @return the QueueItems that were sent with this command.
	 */
	public QueueItem[] getQueueItems()
	{
		return queueItems;
	}
	
	/**
	 * Gets a string representation of this MultiQueueItemTransferRequest.
	 * 
	 * @return a string representation of this MultiQueueItemTransferRequest.
	 */
	public String toString()
	{
		//Note that this method is used by equals(Object o) to test for equality.
		if(toStringString == null) 
		{
			toStringString = this.getClass().getName() + "(address: " + address + ", ids: " + QueueItem.concatIds(this.queueItems) + ")";
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
		
		out.writeInt(this.queueItems.length); // Write number of items
		
		for(int i=0; i<this.queueItems.length; i++) // Write item ids and data
		{
			out.writeUTF(this.queueItems[i].getId()); // Write item id
			out.writeObject(this.queueItems[i].getItemData()); // Write item data
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
		
		final long receiveTime = System.currentTimeMillis();

		final int numberOfItemIds = in.readInt(); // Read number of item ids
		
		String itemId;
		QueueItemData itemData;
		this.queueItems = new QueueItem[numberOfItemIds];
		
		for(int i=0; i<numberOfItemIds; i++)
		{
			itemId = in.readUTF(); // Read item id
			itemData = (QueueItemData)in.readObject(); // Read item data
			
			// Construct and initialize a new QueueItem for the transferred data
			this.queueItems[i] = new QueueItem(itemData, itemId);
			this.queueItems[i].setSendReceiveTime(receiveTime);
			// QueueItem address must be initialized in execute(RemoteQueueSystem)
		}
	}
}
