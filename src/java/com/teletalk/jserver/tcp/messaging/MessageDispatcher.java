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

import java.io.InputStream;

/**
 * Class providing an alternative, and possibly easier, approach to dispatching messages through a {@link MessagingManager}. 
 * This class uses a {@link MessageDispatcherProperties} object as configuration for how messages are to be dispatched. A 
 * prototype header to be used for all messages may also be specified through the method {@link #setPrototypeMessageHeader(MessageHeader)}.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.1 Build 690
 */
public class MessageDispatcher
{
	private final MessagingManager messagingManager;
   
   private final MessageDispatcherProperties messageDispatcherProperties; 
	
   private MessageHeader prototypeMessageHeader;
   
	/**
    * Creates a new MessageDispatcher associated with the specified {@link MessagingManager}.
    * 
    * @param messagingManager the associated {@link MessagingManager}.
    * @param messageDispatcherProperties the settings for message dispatch. 
	 */
	public MessageDispatcher(final MessagingManager messagingManager, final MessageDispatcherProperties messageDispatcherProperties)
	{
		this.messagingManager = messagingManager;
      this.messageDispatcherProperties = messageDispatcherProperties;
      this.prototypeMessageHeader = null;
	}
   
   /**
    * Gets the associated {@link MessagingManager}.
    */
   public MessagingManager getMessagingManager()
   {
      return messagingManager;
   }
   
   /**
    * Gets the prototype message header.<br>
    * <br>
    * <b><i>Note: </i></b> Do not use this header for message dispatch directly, since this may result in undesired side effects! 
    * Use {@link #createPrototypeMessageHeaderInstance()} insted.
    * 
    * @see #createPrototypeMessageHeaderInstance()
    */
   public MessageHeader getPrototypeMessageHeader()
   {
      return this.prototypeMessageHeader;
   }
   
   /**
    * Creates a new MessageHeader instance based on the data in the prototype header ({@link #getPrototypeMessageHeader()}). If no 
    * message header exists, this method creates a new "empty" header. 
    * 
    * @since 2.1.2 (20060215)
    */
   public MessageHeader createPrototypeMessageHeaderInstance()
   {
      if( this.prototypeMessageHeader != null ) return new MessageHeader(this.prototypeMessageHeader);
      else return new MessageHeader();
   }

   /**
    * Sets the prototype message header.
    */
   public void setPrototypeMessageHeader(MessageHeader header)
   {
      this.prototypeMessageHeader = header;
   }

   /**
    * Get the {@link MessageDispatcherProperties}. The actual object used by this MessageDispatcher is returned though this method, 
    * which means that any modifications in the returned object will have an affect on this object.
    */
   public MessageDispatcherProperties getMessageDispatcherProperties()
   {
      return messageDispatcherProperties;
   }
   
   /**
    * Dispatches an object message using the prototype message header if specifed, if not a default header will be created.<br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown.<br>
    * <br>
    * This method will automatically set the "<b>response to</b>" field of the header to the value of the 
    * "<b>message id</b>" field, if set, before assigning the message a new id.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message (the asynch field in the {@link MessageDispatcherProperties} must be set to <code>true</code>), 
    * by using the message header of the incomming message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param body the object body of the message. Must be serializable.
    * 
    * @return the response message or <code>null</code> if parameter asynch in the {@link MessageDispatcherProperties} was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final Object body) throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(this.createPrototypeMessageHeaderInstance(), body);
   }
   
   /**
    * Dispatches an object message.<br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown.<br>
    * <br>
    * This method will automatically set the "<b>response to</b>" field of the header to the value of the 
    * "<b>message id</b>" field, if set, before assigning the message a new id.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message (the asynch field in the {@link MessageDispatcherProperties} must be set to <code>true</code>), 
    * by using the message header of the incomming message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param header the header of the message to be dispatched.
    * @param body the object body of the message. Must be serializable.
    * 
    * @return the response message or <code>null</code> if parameter asynch in the {@link MessageDispatcherProperties} was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final Object body) throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.messagingManager.dispatchMessage(header, new ObjectMessageWriter(body), this.messageDispatcherProperties);
   }
   
   /**
    * Dispatches a byte array message using the prototype message header if specifed, if not a default header will be created.<br>
    * <br>
    * This method will automatically set the "<b>response to</b>" field of the header to the value of the 
    * "<b>message id</b>" field, if set, before assigning the message a new id.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message (the asynch field in the {@link MessageDispatcherProperties} must be set to <code>true</code>), 
    * by using the message header of the incomming message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param body the byte array body of the message.
    * 
    * @return the response message or <code>null</code> if parameter asynch in the {@link MessageDispatcherProperties} was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final byte[] body) throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(this.createPrototypeMessageHeaderInstance(), body);
   }
   
   /**
    * Dispatches a byte array message.<br>
    * <br>
    * This method will automatically set the "<b>response to</b>" field of the header to the value of the 
    * "<b>message id</b>" field, if set, before assigning the message a new id.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message (the asynch field in the {@link MessageDispatcherProperties} must be set to <code>true</code>), 
    * by using the message header of the incomming message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param header the header of the message to be dispatched.
    * @param body the byte array body of the message.
    * 
    * @return the response message or <code>null</code> if parameter asynch in the {@link MessageDispatcherProperties} was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final byte[] body) throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.messagingManager.dispatchMessage(header, new ByteArrayMessageWriter(body), this.messageDispatcherProperties);
   }
   
   /**
    * Dispatches an inputstream message message using the prototype message header if specifed, if not a default header will be created.<br>
    * <br>
    * This method will automatically set the "<b>response to</b>" field of the header to the value of the 
    * "<b>message id</b>" field, if set, before assigning the message a new id.<br>
    * <br>
    * <b><i>Note</i></b>: This method will not close the stream when the message has been dispatched - this is the 
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as it is guaranteed 
    * that the message has been fully dispatched by then.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message (the asynch field in the {@link MessageDispatcherProperties} must be set to <code>true</code>), 
    * by using the message header of the incomming message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param body the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * 
    * @return the response message or <code>null</code> if parameter asynch in the {@link MessageDispatcherProperties} was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final InputStream body, final long bodyLength) throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.dispatchMessage(this.createPrototypeMessageHeaderInstance(), body, bodyLength);
   }
   
   /**
    * Dispatches an inputstream message.<br>
    * <br>
    * This method will automatically set the "<b>response to</b>" field of the header to the value of the 
    * "<b>message id</b>" field, if set, before assigning the message a new id.<br>
    * <br>
    * <b><i>Note</i></b>: This method will not close the stream when the message has been dispatched - this is the 
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as it is guaranteed 
    * that the message has been fully dispatched by then.<br>
    * <br>
    * <b>Note: </b> This method can be used for sending a response message (the asynch field in the {@link MessageDispatcherProperties} must be set to <code>true</code>), 
    * by using the message header of the incomming message. When doing so the sender id field of that header will be used to figure out where to send the message. The response 
    * to field will also be set to the value of the message id field.
    * 
    * @param header the header of the message to be dispatched.
    * @param body the inputstream from which the body of the message will be read.
    * @param bodyLength the number of bytes that will be read from the body input stream.
    * 
    * @return the response message or <code>null</code> if parameter asynch in the {@link MessageDispatcherProperties} was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(final MessageHeader header, final InputStream body, final long bodyLength) throws MessageDispatchFailedException, ResponseTimeOutException
   {
      return this.messagingManager.dispatchMessage(header, new InputStreamMessageWriter(body, bodyLength), this.messageDispatcherProperties);
   }
}
