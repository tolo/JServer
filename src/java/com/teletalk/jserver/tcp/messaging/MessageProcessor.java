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

import com.teletalk.jserver.net.acl.AccessControlListHandler;
import com.teletalk.jserver.tcp.messaging.admin.ServerAdministrationHandler;

/**
 * Interface for message processor classes, resposible for handling of incomming messages for a {@link MessagingManager}. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20050517)
 */
public interface MessageProcessor extends MessagingManagerHandler
{
   /**
    * Handles a response message, with the use of the specified message dispatch handler.<br>
    * <br>
    * Note: This method will by default be called synchronously, by the thread reading from the endpoint.
    * 
    * @return <code>true</code> if the specified message was a response to a message send through this object, otherwise <code>false</code>.
    */
   public boolean handleResponseMessage(Message message, MessageDispatchHandler messageDispatchHandler) throws Exception;
   
   /**
    * Called to notify a {@link MessageReceiver} of an incomming message, possibly with an access control list check.<br>
    * <br>
    * Note: This method will by default be called from a different thread than the thread reading from the endpoint.
    * 
    * @param messageReceiver the {@link MessageReceiver}that is to be notified of an incomming message.
    * @param message the received message.
    * @param accessControlListHandler the {@link AccessControlListHandler} used by the associated MessagingManager. May be null.
    * 
    * @return <code>true</code> if the specified message receiver handled the message, otherwise <code>false</code>.
    */
   public boolean handleInMessageReceiver(Message message, MessageReceiver messageReceiver, AccessControlListHandler accessControlListHandler) throws Exception;
   
   /**
    * Method for handling messages (calls) to the {@link ServerAdministrationHandler} of this MessagingManager. This method is 
    * invoked by the method {@link MessagingManager#messageReceivedImpl(Message)} if the header type is {@link MessageHeader#SERVER_ADMINISTRATION_HEADER}.<br>
    * <br>
    * Note: This method will by default be called from a different thread than the thread reading from the endpoint.
    * 
    * @param message the received message.
    */
   public boolean handleServerAdministrationMessage(Message message, AccessControlListHandler accessControlListHandler) throws Exception;
   
   /**
    * Handles a proxied message.<br>
    * <br>
    * Note: This method will by default be called from a different thread than the thread reading from the endpoint.
    * 
    * @param message the message to relay to another messaging system.
    * @param namedReceiver the receiver name to which the message should be relayed to. May be null if the message is a response. 
    * 
    * @return <code>true</code> if the specified message could be proxied, otherwise <code>false</code>.
    */
   public boolean handleProxyMessage(Message message, String namedReceiver) throws Exception;

   /**
    * Called to handle an unhandle message.<br>
    * <br>
    * Note: This method will by default be called from a different thread than the thread reading from the endpoint.
    * 
    * @param message the unhandled message.
    */
   public void handleUnhandledMessage(Message message) throws Exception;
   
   /**
    * Method for handling errors that occur while {@link MessageWorker} threads executes {@link MessagingManager#messageReceivedImpl(Message)}, 
    * i.e. when a message receiver handles a message.<br>
    * <br>
    * Note: This method will by default be called from a different thread than the thread reading from the endpoint.
    * 
    * @param message the received message.
    */
   public void handleMessageReceiverError(Message message, Throwable error);
}
