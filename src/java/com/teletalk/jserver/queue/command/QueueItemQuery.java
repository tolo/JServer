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
import com.teletalk.jserver.queue.QueueManager;

/**
 * Command class for dispatching querys on QueueItems in the in queue of another 
 * queue system.
 *  
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public final class QueueItemQuery extends QueueSystemCommand
{
	/** The serial version id of this class. */
	static final long serialVersionUID = 3258972624910341534L;
		
	private String itemId;
	
	/** Default no-arg constructor used during  deserialization.	 */
	public QueueItemQuery(){}
	
	/**
	 * Creates a new QueueItemQuery.
	 * 
	 * @param address the destination address.
	 * @param itemId the id of the QueueItem for which a query should be made.
	 */
	public QueueItemQuery(final EndPointIdentifier address, final String itemId)
	{
		super(address);
		
		this.itemId = itemId;
	}
	
	public void execute(final QueueManager queueManager)
	{
		queueManager.queueItemQueryReceived(this);//itemId, address);
	}
	
	public void abort(final QueueManager queueManager)
	{
	}

	/**
	 * Gets a string uniquely identifying the QueueItem for which a query should be made.
	 * 
	 * @return a string uniquely identifying the QueueItem for which a query should be made.
	 */
	public String getItemId()
	{
		return itemId;
	}
	
	/**
	 * Gets a string representation of this QueueItemQuery.
	 * 
	 * @return a string representation of this QueueItemQuery.
	 */
	public String toString()
	{
		if(toStringString == null) 
			toStringString = this.getClass().getName() + "(" + address + ", " + getItemId() + ")";
		
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
	}
}
