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
 * Command class used to transfer a QueueItem to another queue system.
 *  
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public final class QueueItemTransferRequest extends QueueSystemCommand
{
	/** The serial version id of this class. */
	static final long serialVersionUID = 1266133650221823983L;
	
	private transient QueueItem queueItem;
	
	/** Default no-arg constructor used during  deserialization.	 */
	public QueueItemTransferRequest(){}
	
	/**
	 * Creates a new QueueItemTransferRequest.
	 * 
	 * @param address the destination address.
	 * @param queueItem the QueueItem to be transferred.
	 */
	public QueueItemTransferRequest(final EndPointIdentifier address, final QueueItem queueItem)
	{
		super(address);
		
		this.queueItem = queueItem;
	}
	
	public void execute(RemoteQueueSystem remoteQueueSystem)
	{
		// Initialize address field of queue item with the address of the remote queue system object.
		this.queueItem.setSenderReceiverAddress(remoteQueueSystem.getRemoteQueueSystemAddress());
		
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
	 * Gets the actual object (QueueItemData that was wrapped in the QueueItem that was dispatched.
	 * 
	 * @return the actual object (QueueItemData that was wrapped in the QueueItem that was dispatched.
	 */
	public QueueItemData getItemData()
	{
		return this.queueItem.getItemData(); //itemData;
	}
	
	/**
	 * Gets a string uniquely identifying the QueueItem that was sent with this command.
	 * 
	 * @return a string uniquely identifying the QueueItem that was sent with this command.
	 */
	public String getItemId()
	{
		return this.queueItem.getId(); //itemId;
	}
	
	/**
	 * Gets the QueueItem that was sent with this command.
	 * 
	 * @return the QueueItem that was sent with this command.
	 */
	public QueueItem getQueueItem()
	{
		return queueItem;
	}
	
	/**
	 * Gets a string representation of this QueueItemTransferRequest.
	 * 
	 * @return a string representation of this QueueItemTransferRequest.
	 */
	public String toString()
	{
		if(toStringString == null) 
      {
			toStringString = this.getClass().getName() + "(" + address + ", " + this.queueItem + ")";
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

		out.writeUTF(this.queueItem.getId()); // Write item id
		out.writeObject(this.getItemData()); // Write item data
	}
	
	/**
	 * Deserialization (Externalizable) method.
	 * 
	 * @param in the stream from which to deserialize objects of this class.
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		super.readExternal(in);		

		final String itemId = in.readUTF(); // Read item id
		final QueueItemData itemData = (QueueItemData)in.readObject(); // Read item data
		
		// Construct and initialize a new QueueItem for the transferred data
		this.queueItem = new QueueItem(itemData, itemId);
		this.queueItem.setSendReceiveTime(System.currentTimeMillis());
		// QueueItem address must be initialized in execute(RemoteQueueSystem)
	}
}
