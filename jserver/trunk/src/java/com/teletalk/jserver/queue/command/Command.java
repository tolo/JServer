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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.teletalk.jserver.queue.RemoteQueueSystem;

/**
 * Base interface for commands.
 *  
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public abstract class Command implements Externalizable
{
	public static final byte QUEUE_COMMAND_VERSION = 0x01;
	
	protected byte queue_command_stream_version;
	
	/**
	 * Method to exectue this command at the receiving end.
	 * 
	 * @param remoteQueueSystem reference to a object representing the remote queue system from which this command was received.
	 */
	public abstract void execute(RemoteQueueSystem remoteQueueSystem);
	
	/**
	 * Serialization (Externalizable) method.
	 * 
	 * @param out the stream on which to serialize objects of this class. 
	 */
	public void writeExternal(ObjectOutput out) throws IOException
	{
		// Write command version
		out.writeByte(QUEUE_COMMAND_VERSION);
	}
	
	/**
	 * Deserialization (Externalizable) method.
	 * 
	 * @param in the stream from which to deserialize objects of this class.
	 */
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		// Read command version
		this.queue_command_stream_version = in.readByte();
	}
}
