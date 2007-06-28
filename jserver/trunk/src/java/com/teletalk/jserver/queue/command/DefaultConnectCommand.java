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

import com.teletalk.jserver.queue.QueueSystemMetaData;
import com.teletalk.jserver.queue.RemoteQueueSystem;

/**
 * Command used for post connection negotiation.<br>
 * <br>
 * Note: This class is part of the legacy queue system collaboration implementation (DefaultQueueSystemCollaborationManager).
 *   
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public class DefaultConnectCommand extends Command
{
	/** The serial version id of this class. */
	static final long serialVersionUID = -1649883241498905046L;
	
	private boolean queueSystemHasInQueue;
	private boolean queueSystemHasOutQueue;
	
	private QueueSystemMetaData metaData;
	
	/** Default no-arg constructor used during  deserialization.	 */
	public DefaultConnectCommand(){}
	
	/**
	 * Creates a new DefaultConnectCommand.
	 * 
	 * @param metaData a QueueSystemMetaData object containing information about the originating queue system to be sent to a remote queue system.
	 */
	public DefaultConnectCommand(QueueSystemMetaData metaData, boolean queueSystemHasInQueue, boolean queueSystemHasOutQueue)
	{
		this.metaData = metaData;
		this.queueSystemHasInQueue = queueSystemHasInQueue;
		this.queueSystemHasOutQueue = queueSystemHasOutQueue;
	}
	
	/**
	 * Gets the QueueSystemMetaData object stored in this DefaultConnectCommand. 
	 * 
	 * @return a QueueSystemMetaData object.
	 */
	public QueueSystemMetaData getQueueSystemMetaData()
	{
		return metaData;	
	}
	
	/**
	 * Checks if the queue system that sent this connect command has an in queue.
	 * 
	 * @return <code>true</code> if the queue system that sent this connect command has an in queue, otherwise <code>false</code>.
	 */
	public boolean hasInQueue()
	{
		return this.queueSystemHasInQueue;
	}
	
	/**
	 * Checks if the queue system that sent this connect command has an out queue.
	 * 
	 * @return <code>true</code> if the queue system that sent this connect command has an out queue, otherwise <code>false</code>.
	 */
	public boolean hasOutQueue()
	{
		return this.queueSystemHasOutQueue;
	}
	
	public void execute(RemoteQueueSystem remoteQueueSystem)
	{
		throw new RuntimeException("Method execute not available in class " + this.getClass().getName() + ".");
	}
	
	/**
	 * Gets a string representation of this DefaultConnectCommand.
	 * 
	 * @return a string representation of this DefaultConnectCommand.
	 */
	public String toString()
	{
		return this.getClass().getName();
	}
	
	/**
	 * Serialization (Externalizable) method.
	 * 
	 * @param out the stream on which to serialize objects of this class. 
	 */
	public void writeExternal(ObjectOutput out) throws IOException
	{
		super.writeExternal(out);
		
		out.writeObject(this.metaData); // Write queue system meta data
		out.writeBoolean(this.queueSystemHasInQueue);
		out.writeBoolean(this.queueSystemHasOutQueue);
	}

	/**
	 * Deserialization (Externalizable) method.
	 * 
	 * @param in the stream from which to deserialize objects of this class.
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		super.readExternal(in);		

		this.metaData = (QueueSystemMetaData)in.readObject(); // Read queue system meta data
		this.queueSystemHasInQueue = in.readBoolean();
		this.queueSystemHasOutQueue = in.readBoolean();
	}
}
