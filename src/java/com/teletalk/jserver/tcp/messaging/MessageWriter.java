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
import java.io.OutputStream;

/**
 * Interface for different implementations for dispatching a message (body).
 * 
 * @author Tobias L�fstrand
 * 
 * @since 1.2
 */
public interface MessageWriter
{
	/**
	 * Called to write a message (header and body) to an endpoint.
	 * 
	 * @param header the header of the message that is to be dispatched.
    * @param endPoint the endpoint on which the message is to be dispatched on.
	 * @param endPointOutputStream the output stream of the endpoint on which the message is to be written to.
	 */
	public void writeMessage(MessageHeader header, MessagingEndPoint endPoint, OutputStream endPointOutputStream) throws IOException;
	
	/**
	 * Gets a description of the message body (for debug).
	 */
	public String getDescription();
}