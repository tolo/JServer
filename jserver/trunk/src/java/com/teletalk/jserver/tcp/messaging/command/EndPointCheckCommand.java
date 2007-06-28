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
package com.teletalk.jserver.tcp.messaging.command;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.teletalk.jserver.tcp.messaging.MessageHeader;

/**
 * Command class used to check the status of the connection of an endpoint. Instances if this class is used in the method 
 * {@link com.teletalk.jserver.tcp.messaging.MessagingEndPoint#check()}.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.2
 */
public class EndPointCheckCommand extends MessageHeader
{
	/** The serial version id of this class. */
	static final long serialVersionUID = -4361360951069638295L;
	
	/** The version number of the serialized data of this class. */
 	public static final byte SERIAL_DATA_VERSION = 0x01;
			
	/**
	 * No-arg constructor for serialization (Externalizable).
	 */
	public EndPointCheckCommand()
	{
		super();
      
      super.setHeaderType(ENDPOINT_CHECK_HEADER);
	}
	
	/**
	 * Serialization (Externalizable) method.
	 * 
	 * @param out the stream on which to serialize objects of this class. 
	 */
	public void writeExternal(final ObjectOutput out) throws IOException
	{
		super.writeExternal(out);
		
		// Write serial data version
		out.writeByte(SERIAL_DATA_VERSION);
	}
	
	/**
	 * Deserialization (Externalizable) method.
	 * 
	 * @param in the stream from which to deserialize objects of this class.
	 */
	public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException
	{
		super.readExternal(in);
		
		// Read serial data version
		in.readByte();
	}
}
