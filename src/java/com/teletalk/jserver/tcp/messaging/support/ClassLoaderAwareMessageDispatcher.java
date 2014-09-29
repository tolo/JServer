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
package com.teletalk.jserver.tcp.messaging.support;

import java.io.InputStream;

import com.teletalk.jserver.tcp.messaging.Message;
import com.teletalk.jserver.tcp.messaging.MessageDispatchFailedException;
import com.teletalk.jserver.tcp.messaging.MessageDispatcher;
import com.teletalk.jserver.tcp.messaging.MessageDispatcherProperties;
import com.teletalk.jserver.tcp.messaging.MessageHeader;
import com.teletalk.jserver.tcp.messaging.MessagingManager;
import com.teletalk.jserver.tcp.messaging.ResponseTimeOutException;

/**
 * Message dispatcher implementation to be used in environments where a messaging manager is to be used from different class loaders.
 *  
 * @author Tobias Löfstrand
 * 
 * @since 2.1.2 (20060303)
 * 
 * @see com.teletalk.jserver.tcp.messaging.support.ClassLoaderAwareMessage
 */
public class ClassLoaderAwareMessageDispatcher extends MessageDispatcher
{
   private ClassLoader classLoader = null;
   
   /**
    * Creates a new ClassLoaderAwareMessageDispatcher associated with the specified {@link MessagingManager}.
    */
   public ClassLoaderAwareMessageDispatcher(final MessagingManager messagingManager, final MessageDispatcherProperties messageDispatcherProperties)
   {
      super(messagingManager, messageDispatcherProperties);
   }
   
   /**
    * Sets the class loader that should be used for resolving classes when reading object message bodies, 
    * using the method {@link Message#getBodyAsObject()}. 
    * 
    * @param classLoader the class loader to be used for resolving classes.
    */
   public void setClassLoader(ClassLoader classLoader)
   {
      this.classLoader = classLoader;
   }
   
   /**
    * Method initializing a response message. This method only sets the configured class loader on the received message.
    */
   protected Message initMessage(final ClassLoaderAwareMessage message)
   {
      if( message != null )
      {
         message.setClassLoader(this.classLoader);
      }
      return message;
   }
   
   /**
    * Dispatches an object message using the prototype message header if specifed, if not a default header will be created.<br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown.<br>
    * <br>
    * This method will automatically set the "<b>response to</b>" field of the header to the value of the 
    * "<b>message id</b>" field, if set, before assigning the message a new id.
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
      return this.initMessage((ClassLoaderAwareMessage)super.dispatchMessage(body));
   }
   
   /**
    * Dispatches an object message.<br>
    * <br>
    * The specified message body object must be serializable, or a MessageDispatchFailedException will be thrown.<br>
    * <br>
    * This method will automatically set the "<b>response to</b>" field of the header to the value of the 
    * "<b>message id</b>" field, if set, before assigning the message a new id.
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
      return this.initMessage((ClassLoaderAwareMessage)super.dispatchMessage(header, body));
   }
   
   /**
    * Dispatches a byte array message using the prototype message header if specifed, if not a default header will be created.<br>
    * <br>
    * This method will automatically set the "<b>response to</b>" field of the header to the value of the 
    * "<b>message id</b>" field, if set, before assigning the message a new id.
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
      return this.initMessage((ClassLoaderAwareMessage)super.dispatchMessage(body));
   }
   
   /**
    * Dispatches a byte array message.<br>
    * <br>
    * This method will automatically set the "<b>response to</b>" field of the header to the value of the 
    * "<b>message id</b>" field, if set, before assigning the message a new id.
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
      return this.initMessage((ClassLoaderAwareMessage)super.dispatchMessage(header, body));
   }
   
   /**
    * Dispatches an inputstream message message using the prototype message header if specifed, if not a default header will be created.<br>
    * <br>
    * This method will automatically set the "<b>response to</b>" field of the header to the value of the 
    * "<b>message id</b>" field, if set, before assigning the message a new id.<br>
    * <br>
    * <b><i>Note</i></b>: This method will not close the stream when the message has been dispatched - this is the 
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as it is guaranteed 
    * that the message has been fully dispatched by then.
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
      return this.initMessage((ClassLoaderAwareMessage)super.dispatchMessage(body, bodyLength));
   }
   
   /**
    * Dispatches an inputstream message.<br>
    * <br>
    * This method will automatically set the "<b>response to</b>" field of the header to the value of the 
    * "<b>message id</b>" field, if set, before assigning the message a new id.<br>
    * <br>
    * <b><i>Note</i></b>: This method will not close the stream when the message has been dispatched - this is the 
    * responsibility of the caller. It is however perfeclty safe to close the stream after the return of this method, as it is guaranteed 
    * that the message has been fully dispatched by then.
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
      return this.initMessage((ClassLoaderAwareMessage)super.dispatchMessage(header, body, bodyLength));
   }   
}
