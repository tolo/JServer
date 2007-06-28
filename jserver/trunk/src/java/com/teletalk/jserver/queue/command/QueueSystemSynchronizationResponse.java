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

import com.teletalk.jserver.queue.QueueItem;
import com.teletalk.jserver.queue.QueueSystemMetaData;
import com.teletalk.jserver.queue.RemoteQueueSystem;
import com.teletalk.jserver.util.StringUtils;

/**
 * Response for a QueueSystemSynchronizationRequest.
 * 
 * @see QueueSystemSynchronizationRequest
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1
 */
public class QueueSystemSynchronizationResponse extends Command
{
	/** The serial version id of this class. */
	static final long serialVersionUID = -6748259875951382733L;
	
	private String[] queuedItemIds;
	private QueueItemCompletionResponse[] comletionResponses;
	
	private QueueSystemMetaData queueSystemMetaData = null;
   
	
	/**
	 * Creates a new QueueSystemSynchronizationResponse.
	 */
	public QueueSystemSynchronizationResponse()
	{
		this.queuedItemIds = new String[0];
		this.comletionResponses = new QueueItemCompletionResponse[0];
	}
   
   /**
    * Creates a new QueueSystemSynchronizationResponse.
    */
   public QueueSystemSynchronizationResponse(QueueSystemMetaData queueSystemMetaData)
   {
      this.queuedItemIds = new String[0];
      this.comletionResponses = new QueueItemCompletionResponse[0];
      this.queueSystemMetaData = queueSystemMetaData;
   }
	
	/**
	 * Creates a new QueueSystemSynchronizationResponse.
	 */
	public QueueSystemSynchronizationResponse(QueueItem[] queuedItems, QueueItemCompletionResponse[] comletionResponses, QueueSystemMetaData queueSystemMetaData)
	{
		this.queuedItemIds = new String[queuedItems.length];
		
		for(int i=0; i<queuedItems.length; i++)
		{
			this.queuedItemIds[i] = queuedItems[i].getId();
		}
		
		this.comletionResponses = comletionResponses;
		
		this.queueSystemMetaData = queueSystemMetaData;
	}
	
	public void execute(RemoteQueueSystem remoteQueueSystem)
	{
	}
	
	/**
	 * Gets the ids associated with this response. 
	 * 
	 * @return the ids associated with this response. 
	 */
	public String[] getIds()
	{
		return this.queuedItemIds;
	}

	/**
	 * Gets the completion responses associated with this response. 
	 * 
	 * @return the completion responses associated with this response. 
	 */
	public QueueItemCompletionResponse[] geCompletionResponse()
	{
		return this.comletionResponses;
	}
	
	/**
	 * Gets the QueueSystemMetaData associated with this response.
	 * 
	 * @return the QueueSystemMetaData associated with this response.
	 */
	public final QueueSystemMetaData getMetaData()
	{
		return this.queueSystemMetaData;
	}
	
	public void writeExternal(ObjectOutput out) throws IOException
	{
		super.writeExternal(out);
		
		out.writeInt(this.queuedItemIds.length); // Write number of ids

		// Write ids
		for(int i=0; i<this.queuedItemIds.length; i++)
		{
			out.writeUTF(this.queuedItemIds[i]);
		}
		
		out.writeInt(this.comletionResponses.length); // Write number of completion responses

		// Write completion responses
		for(int i=0; i<this.comletionResponses.length; i++)
		{
			out.writeObject(this.comletionResponses[i]);
		}
		
		out.writeObject(this.queueSystemMetaData);
	}
	
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		super.readExternal(in);
		
		int n = in.readInt(); // Read number of ids
		
		this.queuedItemIds = new String[n];

		// Read ids
		for(int i=0; i<n; i++)
		{
			this.queuedItemIds[i] = in.readUTF();
		}
		
		n = in.readInt(); // Read number of completion responses
		
		this.comletionResponses = new QueueItemCompletionResponse[n];

		// Read completion responses
		for(int i=0; i<n; i++)
		{
			this.comletionResponses[i] = (QueueItemCompletionResponse)in.readObject();
		}
		
		this.queueSystemMetaData = (QueueSystemMetaData)in.readObject();
	}
   
   /**
    * Gets a string representation of this object.
    */
   public String toString()
   {
      return "QueueSystemSynchronizationResponse(" + StringUtils.toString(queuedItemIds) + ")";
   }
}
