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
package com.teletalk.jserver.queue;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;

import com.teletalk.jserver.util.StringUtils;

/**
 * Class for handling meta data for queue systems. This class is used by queue systems for exchanging information about themselves. 
 * It contains the following information:<BR>
 * <BR>
 * * the length of the in queue. <BR>
 * * the blocking state of the in queue. <BR>
 * * a hashtable containing user defined information. <BR>
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public final class QueueSystemMetaData implements Externalizable, Cloneable
{
	/** The serial version id of this class. */
	static final long serialVersionUID = -1285270295680466438L;	
	
	private static final byte VERSION = 1;
	
	private volatile boolean inQueueBlocked;
	private volatile int inQueueLength;
	private volatile int inQueueMaxLength;
	
	private volatile HashMap extraData;
	
	private volatile boolean dirty;
	
	/**
	 * Creates a new QueueSystemMetaData object.
	 */
	public QueueSystemMetaData()
	{
		inQueueBlocked = false;
		inQueueLength = -1;
		inQueueMaxLength = -1;
		
		dirty = true;
		
		extraData = null;
	}
	
	public String toString()
	{
		if(extraData == null)
		{
			return "QueueSystemMetaData(inQueueBlocked: "  + inQueueBlocked + 
						", inQueueLength: " + inQueueLength + 
						", inQueueMaxLength: " + inQueueMaxLength + ")";
		}
		else
		{
			return "QueueSystemMetaData(inQueueBlocked: "  + inQueueBlocked + 
						", inQueueLength: " + inQueueLength + 
					 ", inQueueMaxLength: " + inQueueMaxLength + ", extraData: " + StringUtils.toString(extraData) + ")";
		}
	}
	
	/**
	 * Sets the length of the in queue in the queue system.
	 * 
	 * @param inQueueLength the length of the in queue.
	 */
	public void setInQueueLength(int inQueueLength)
	{
		this.inQueueLength = inQueueLength;
	}
	
	/**
	 * Gets the length of the in queue in the queue system.
	 * 
	 * @return the length of the in queue.
	 */
	public int getInQueueLength()
	{
		return inQueueLength;
	}
	
	/**
	 * Sets the maximum length of the in queue in the queue system.
	 * 
	 * @param inQueueMaxLength the maximum length of the in queue.
	 */
	public void setinQueueMaxLength(int inQueueMaxLength)
	{
		this.inQueueMaxLength = inQueueMaxLength;
	}
	
	/**
	 * Gets the maximum length of the in queue in the queue system.
	 * 
	 * @return the maximum length of the in queue.
	 */
	public int getinQueueMaxLength()
	{
		return inQueueMaxLength;
	}
	
	/**
	 * Sets the block status of the in queue in the queue system.
	 * 
	 * @param blocked the block status of the in queue.
	 */
	public void setInQueueBlocked(boolean blocked)
	{
		this.inQueueBlocked = blocked;
	}
	
	/**
	 * Gets the block status of the in queue in the queue system.
	 * 
	 * @return the block status of the in queue.
	 */
	public boolean isInQueueBlocked()
	{
		return inQueueBlocked;
	}
	
	/**
	 * Stores extra metadata in this object.
	 * 
	 * @param key metadata key.
	 * @param value metadata value.
	 */
	public synchronized void putExtraData(Object key, Object value)
	{
      if( extraData == null ) extraData = new HashMap();
		extraData.put(key, value);
		setDirtyFlag(true);
	}
	
	/**
	 * Gets extra metadata.
	 * 
	 * @param key metadata key.
	 * 
	 * @return the metadata value.
	 */
	public synchronized Object getExtraData(Object key)
	{
		if( extraData != null ) return extraData.get(key);
      else return null;
	}
	
	/**
	 * Gets a hashtable containing all extra metadata.
	 * 
	 * @return hashtable containing all extra metadata.
	 */
	public synchronized HashMap getExtraData()
	{
		return extraData;
	}
	
	/**
	 * Sets hashtable for extra metadata, replacing the old one.
	 * 
	 * @param extraData a hashtable containing extradata.
	 */
	public synchronized void setExtraData(HashMap extraData)
	{
		this.extraData = extraData;
	}
	
	/**
	 * Sets the value of the flag indicating if this object has changed.
	 * 
	 * @param dirty boolean value indicating if the flag should be set or not.
	 */
	public void setDirtyFlag(boolean dirty)
	{
		this.dirty = dirty;
	}
	
	/**
	 * Gets the value of the flag indicating if this object has changed.
	 * 
	 * @return the value of the flag indicating if this object has changed.
	 */
	public boolean isDirty()
	{
		return dirty;
	}
	
	/**
	 * Resets (unsets) the value of the flag indicating if this object has changed.
	 */
	public void resetDirtyFlag()
	{
		dirty = false;
	}
	
	/**
	 * Creates and returns a copy of this object. 
	 * 
	 * @return a clone of this object.
	 */
	public Object clone()
	{
		QueueSystemMetaData clonedData = new QueueSystemMetaData();
		clonedData.inQueueBlocked = this.inQueueBlocked;
		clonedData.inQueueLength = this.inQueueLength;
		clonedData.inQueueMaxLength = this.inQueueMaxLength;
		synchronized(extraData)
		{
			clonedData.extraData = (HashMap)this.extraData.clone();
		}
		
		return clonedData;
	}
	
	public void writeExternal(ObjectOutput out) throws IOException
	{
		// Write command version
		out.writeByte(VERSION);
		out.writeBoolean(this.inQueueBlocked);
		out.writeInt(this.inQueueLength);
		out.writeInt(this.inQueueMaxLength);
		out.writeObject(this.extraData);
	}
	
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		// Read (consume) command version
		in.readByte();
		this.inQueueBlocked = in.readBoolean();
		this.inQueueLength = in.readInt();
		this.inQueueMaxLength = in.readInt();
		this.extraData = (HashMap)in.readObject();
	}
}
