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
import com.teletalk.jserver.queue.RemoteQueueSystem;

/**
 * Baseclass for commands sent between queue systems.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public abstract class QueueSystemCommand extends Command
{
	/** The destination address. */
	protected transient EndPointIdentifier address;
	
	/** The time at which this command was sent. */
	protected transient long sendTime = -1;
	
	/** Reference to a string description of this command. */
	protected transient String toStringString = null;
		
	/** Default no-arg constructor used during  deserialization.	 */
	public QueueSystemCommand(){}
	
	/**
	 * Creates a new QueueSystemCommand which is to be sent to the specified address.
	 * 
	 * @param address the destination address.
	 */
	public QueueSystemCommand(EndPointIdentifier address)
	{
		this.address = address;
		
		this.sendTime = System.currentTimeMillis();
	}
						
	public void execute(RemoteQueueSystem remoteQueueSystem)
	{
		this.address = remoteQueueSystem.getRemoteQueueSystemAddress();
		
		remoteQueueSystem.getQueueSystemCollaborationManager().handleQueueSystemCommand(this);
	}
	
	/**
	 * Method to exectue this command in the receiving QueueManager.
	 * 
	 * @param queueManager the QueueManager in the receiving queue system.
	 */
	public abstract void execute(QueueManager queueManager);
	
	/**
	 * Method to abort this command in the sending QueueManager.
	 * 
	 * @param queueManager the QueueManager in the sending queue system.
	 */
	public abstract void abort(QueueManager queueManager);
	
	/**
	 * Gets the address to which this command is to be sent.
	 * 
	 * @return the address to which this command is to be sent.
	 */
	public EndPointIdentifier getAddress()
	{
		return this.address;	
	}
	
	/**
	 * Gets the time at which this command was sent.
	 * 
	 * @return the time at which this command was sent.
	 */
	public long getSendTime()
	{
		return sendTime;
	}
	
	/**
	 * Sets the address to which this command is to be sent.
	 * 
	 * @param address the address to which this command is to be sent.
	 */
	public final void setAddress(EndPointIdentifier address)
	{
		toStringString = null;
		this.address = address;
	}
	
	/**
	 * Sets the time at which this command was sent.
	 * 
	 * @param sendTime the time at which this command was sent.
	 */
	public final void setSendTime(long sendTime)
	{
		this.sendTime = sendTime;	
	}
	
	/**
	 * Gets a string representation of this QueueSystemCommand. 
	 * 
	 * @return a string representation of this QueueSystemCommand.
	 */
	public String toString()
	{
		//Note that this method is used by equals(Object o) to test for equality.
		
		if(toStringString == null) 
			toStringString = this.getClass().getName() + "@" + System.identityHashCode(this) + "(" + address + ")";
		
		return toStringString;
	}
	
	/**
	 * Compares this object with another.
	 * 
	 * @param o an object to be compared with this.
	 * 
	 * @return true if the object specified by parameter o is an instance of QueueSystemCommand and has the same toString representation.
	 */
	/*public boolean equals(Object o)
	{
		if(o instanceof QueueSystemCommand)
		{
			QueueSystemCommand q = (QueueSystemCommand)o;
			//if( q.getClass().equals(this.getClass()) && q.address.equals(this.address) ) return true;
			return this.toString().equals(q.toString()
		}
			
		return false;
	}*/
	
	/**
	 * Serialization (Externalizable) method.
	 * 
	 * @param out the stream on which to serialize objects of this class. 
	 */
	public void writeExternal(ObjectOutput out) throws IOException
	{
		super.writeExternal(out);
		
		out.writeObject(this.address); // Write address
	}
	
	/**
	 * Deserialization (Externalizable) method.
	 * 
	 * @param in the stream from which to deserialize objects of this class.
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		super.readExternal(in);		

		address = (EndPointIdentifier)in.readObject(); // Read address
	}
}
