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
 * Interface for classes implementing message dispatching logic for a {@link MessagingManager}. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20050517)
 */
public interface MessageDispatchHandler extends MessagingManagerHandler
{
   /**
    * Checks if the specified message is a response to a message previously dispatched through 
    * {@link #dispatchMessage(MessageHeader, MessageWriter, MessageDispatcherProperties, boolean)}, 
    * and if so notify the thread that is waiting for the response.
    * 
    * @return <code>true</code> if the specified message was a response to a message send through this object, otherwise <code>false</code>.
    */
   public boolean responseReceived(final Message message) throws Exception;
   
   /**
    * Method for performing the actual dispatching of a message. This method handles the dispatching of both synchronous and 
    * asynchronous message. For synchronous messages, this methods will block while waiting for a response message. 
    * The maximum time this method will block is determined by the timeout value of the messageDispatcherProperties  
    * object or the timeouts defined in MessagingManager by {@link MessagingManager#getResponseMessageTimeOut()} or 
    * {@link MessagingManager#getThreadResponseMessageTimeOut()}. When a response message is received, the MessagingManager 
    * will call the {@link #responseReceived(Message)} to notify this object that a response is received.<br>
    * <br>
    * This method automatically sets the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageDispatchImpl the implementation to be used for dispatching the message body.
    * @param messageDispatcherProperties a {@link MessageDispatcherProperties} object containing properties for message dispatch.
    * @param proxyMessage flag indicating if the message to be dispatched is a proxied message, which means that this method should never wait for a 
    * response even if the message isn't asych.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(MessageHeader header, MessageWriter messageDispatchImpl, 
         MessageDispatcherProperties messageDispatcherProperties, boolean proxyMessage)
         throws MessageDispatchFailedException, ResponseTimeOutException;
}
