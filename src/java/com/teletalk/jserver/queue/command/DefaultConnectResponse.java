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

import com.teletalk.jserver.queue.QueueSystemMetaData;

/**
 * Reply command class used for post connection negotiation.<br>
 * <br>
 * Note: This class is part of the legacy queue system collaboration implementation (DefaultQueueSystemCollaborationManager).
 *   
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public final class DefaultConnectResponse extends DefaultConnectCommand //Command
{
	/** The serial version id of this class. */
	static final long serialVersionUID = 7700811304680796184L;

	//private QueueSystemMetaData metaData;
	
	/**
	 * Creates a DefaultConnectResponse for a failed connection attempt. 
	 */
	public DefaultConnectResponse()
	{
		super(null, false, false);
		//metaData = null;
	}
	
	/**
	 * Creates a ConnectReplyMessage for a succeeded connection attempt.
	 * 
	 * @param metaData a QueueSystemMetaData object containing information about the originating queue system to be sent to a remote queue system.
	 */
	public DefaultConnectResponse(QueueSystemMetaData metaData, boolean queueSystemHasInQueue, boolean queueSystemHasOutQueue)
	{
		super(metaData, queueSystemHasInQueue, queueSystemHasOutQueue);
	}
	/*public DefaultConnectResponse(QueueSystemMetaData metaData)
	{
		this.metaData = metaData;
	}*/
	
	/**
	 * Gets the QueueSystemMetaData object stored in this DefaultConnectCommand. 
	 * 
	 * @return a QueueSystemMetaData object.
	 */
	/*public QueueSystemMetaData getQueueSystemMetaData()
	{
		return metaData;	
	}*/
	
	/*public void execute(RemoteQueueSystem remoteQueueSystem)
	{
		throw new RuntimeException("Method execute not available in class " + this.getClass().getName() + ".");
	}*/
	
	/**
	 * Gets a string representation of this DefaultConnectResponse.
	 * 
	 * @return a string representation of this DefaultConnectResponse.
	 */
	/*public String toString()
	{
		return this.getClass().getName();
	}*/
	
	/**
	 * Serialization (Externalizable) method.
	 * 
	 * @param out the stream on which to serialize objects of this class. 
	 */
	/*public void writeExternal(ObjectOutput out) throws IOException
	{
		super.writeExternal(out);
		
		out.writeObject(this.metaData); // Write queue system meta data
	}*/
	
	/**
	 * Deserialization (Externalizable) method.
	 * 
	 * @param in the stream from which to deserialize objects of this class.
	 */
	/*public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		super.readExternal(in);		

		this.metaData = (QueueSystemMetaData)in.readObject(); // Read queue system meta data
	}*/
}
