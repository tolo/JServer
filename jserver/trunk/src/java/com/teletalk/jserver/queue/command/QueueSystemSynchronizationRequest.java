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
import com.teletalk.jserver.queue.RemoteQueueSystem;
import com.teletalk.jserver.util.StringUtils;

/**
 * Request to synchronized the out queue of a queue system with the in queue of another.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1
 */
public class QueueSystemSynchronizationRequest extends Command
{
	/** The serial version id of this class. */
	static final long serialVersionUID = -7373931230536421535L;
	
	private String[] ids;
	private short[] statusValues;
   
	
	/**
	 * Creates a new QueueSystemSynchronizationRequest.
	 */
	public QueueSystemSynchronizationRequest()
	{
		this.ids = new String[0];
		this.statusValues = new short[0];
	}
	
	/**
	 * Creates a new QueueSystemSynchronizationRequest.
	 */
	public QueueSystemSynchronizationRequest(final QueueItem[] items)
	{
		this.ids = new String[items.length];
		this.statusValues = new short[items.length];
		
		for(int i=0; i<items.length; i++)
		{
			this.ids[i] = items[i].getId();
			this.statusValues[i] = items[i].getStatus();
		}
	}
	
	public void execute(RemoteQueueSystem remoteQueueSystem)
	{
	}
	
	/**
	 * Gets the ids associated with this request. 
	 * 
	 * @return the ids associated with this request. 
	 */
	public String[] getIds()
	{
		return this.ids;
	}
	
	/**
	 * Gets the status values associated with this request. 
	 * 
	 * @return the status values associated with this request. 
	 */
	public short[] getStatuses()
	{
		return this.statusValues;
	}
	
	public void writeExternal(ObjectOutput out) throws IOException
	{
		super.writeExternal(out);
		
		out.writeInt(this.ids.length); // Write number of ids/status values

		// Write ids and status values
		for(int i=0; i<this.ids.length; i++)
		{
			out.writeUTF(this.ids[i]);
			out.writeShort(this.statusValues[i]);
		}
	}
	
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		super.readExternal(in);
		
		int n = in.readInt(); // Read number of ids/status values
		
		this.ids = new String[n];
		this.statusValues = new short[n];

		// Read ids and status values
		for(int i=0; i<n; i++)
		{
			this.ids[i] = in.readUTF();
			this.statusValues[i] = in.readShort();
		}
	}
   
   /**
    * Gets a string representation of this object.
    */
   public String toString()
   {
      return "QueueSystemSynchronizationRequest(" + StringUtils.toString(ids) + ")";
   }
}
