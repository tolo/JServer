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

/**
 * Exception class used to indicate that a message couldn't be dispatched or handled by the receiving side.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.2
 */
public class MessageDispatchFailedException extends MessagingException
{
   static final long serialVersionUID = -4116545114660253625L;

	private final boolean canRedispatch;
   
   private final MessageHeader responseHeader;

	/**
	 * Constructs a new MessageDispatchFailedException.
	 * 
	 * @param message the detail message.
	 */
	public MessageDispatchFailedException(String message)
	{
		this(message, false, null);
	}
	
	/**
	 * Constructs a new MessageDispatchFailedException.
	 * 
	 * @param message the detail message.
	 * @param cause the cause.
	 */
	public MessageDispatchFailedException(String message, Throwable cause)
	{
		this(message, true, cause);
	}
	
	/**
	 * Constructs a new MessageDispatchFailedException.
	 * 
	 * @param message the detail message.
	 * @param canRedispatch boolean flag indicating if the MessagingManager can redispatch the message (<code>true</code>) or not (<code>false</code>).
	 */
	protected MessageDispatchFailedException(String message, boolean canRedispatch)
	{
		this(message, canRedispatch, null);
	}
   
   /**
    * Constructs a new MessageDispatchFailedException representing a response indicating that the remote server was unable to process the message. 
    * 
    * @param message the detail message.
    * @param responseHeader the header of the response received from the remote server. 
    */
   protected MessageDispatchFailedException(String message, MessageHeader responseHeader)
   {
      this(message, responseHeader, null);
   }
	
	/**
	 * Constructs a new MessageDispatchFailedException.
	 * 
	 * @param message the detail message.
	 * @param canRedispatch boolean flag indicating if the MessagingManager can redispatch the message (<code>true</code>) or not (<code>false</code>).
    * @param cause the cause.
	 */
	protected MessageDispatchFailedException(String message, boolean canRedispatch, Throwable cause)
	{
		super(message, cause);
		this.canRedispatch = canRedispatch;
      this.responseHeader = null;
	}
   
   /**
    * Constructs a new MessageDispatchFailedException representing a response indicating that the remote server was unable to process the message. 
    * 
    * @param message the detail message.
    * @param responseHeader the header of the response received from the remote server.
    * @param cause the cause.
    */
   protected MessageDispatchFailedException(String message, MessageHeader responseHeader, Throwable cause)
   {
      super(message, cause);
      this.canRedispatch = false;
      this.responseHeader = responseHeader;
   }
	
	/**
	 * Gets the flag indicating if the MessagingManager can redispatch the message (<code>true</code>) or not (<code>false</code>). 
    * Note: This flag is only used by MessagingManager internally. 
	 * 
	 * @return the flag indicating if the MessagingManager can redispatch the message (<code>true</code>) or not (<code>false</code>).
	 */
	protected boolean reDispatchPossible()
	{
		return this.canRedispatch;
	}
   
   /**
    * Gets the associated response message header, if any. The response header is only set if this exception was thrown 
    * due to the fact that the remote sever was unable to process the message. 
    * 
    * @since 2.0.2 (20050401)
    */
   public MessageHeader getResponseHeader()
   {
      return this.responseHeader;
   }
}
