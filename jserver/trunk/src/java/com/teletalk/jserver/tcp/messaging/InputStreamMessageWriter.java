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
package com.teletalk.jserver.tcp.messaging;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Message writer implementation for sending a streamed message body.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.2
 */
public final class InputStreamMessageWriter implements MessageWriter
{
	private final InputStream messageBodyInputStream;
	private final long bodyLength;
	
	/**
	 * Creates a new InputStreamMessageDispatchImpl for writing the specified object input stream body.
	 * 
	 * @param messageBodyInputStream the input stream to read the message body from.
	 * @param bodyLength the number of bytes to read from the input stream.
	 */
	public InputStreamMessageWriter(final InputStream messageBodyInputStream, final long bodyLength)
	{
		this.messageBodyInputStream = messageBodyInputStream;
		this.bodyLength = bodyLength;
	}
	
	/**
	 * Called to write a message (header and body) to an endpoint.
	 * 
	 * @param header the header of the message that is to be dispatched.
    * @param endPoint the endpoint on which the message is to be dispatched on.
    * @param endPointOutputStream the output stream of the endpoint on which the message is to be written to.
	 */
   public void writeMessage(MessageHeader header, MessagingEndPoint endPoint, OutputStream endPointOutputStream) throws IOException
	{
		header.setBodyLength(this.bodyLength);
					
		if( endPoint.getDestination().getProtocolVersion() >= 4 )
		{
			// Write header
			endPoint.dispatchHeader(header);
         
         if(endPoint.isDebugMode()) endPoint.logDebug("Sending streamed message with header " + header + ".");
		}
		else
		{									
			// Write header
			endPoint.dispatchHeader(header);
         
         if(endPoint.isDebugMode()) endPoint.logDebug("Sending streamed message with header " + header + ".");
         
         endPoint.resetObjectSerializer(true, true); 
		}
		
		// Stream body
		endPoint.writeStreamBody(header, (InputStream)messageBodyInputStream);
		
		if(endPoint.isDebugMode()) endPoint.logDebug("Done sending streamed message with header " + header + ".");
	}
	
	/**
	 * Gets a description of the message body (for debug).
	 */
	public String getDescription()
	{
		return null;
	}
}