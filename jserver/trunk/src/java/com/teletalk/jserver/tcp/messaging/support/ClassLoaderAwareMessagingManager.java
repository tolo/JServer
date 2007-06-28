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

import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.tcp.TcpEndPointIdentifier;
import com.teletalk.jserver.tcp.messaging.Message;
import com.teletalk.jserver.tcp.messaging.MessageDispatcher;
import com.teletalk.jserver.tcp.messaging.MessageDispatcherProperties;
import com.teletalk.jserver.tcp.messaging.MessageHeader;
import com.teletalk.jserver.tcp.messaging.MessagingEndPoint;
import com.teletalk.jserver.tcp.messaging.MessagingManager;

/**
 * MessagingManager implementation to be used in environments where code running in different class loaders are using the MessagingManager.
 *  
 * @author Tobias Löfstrand
 * 
 * @since 2.1.2 (20060306) 
 */
public class ClassLoaderAwareMessagingManager extends MessagingManager
{
   /**
    * Creates a ClassLoaderAwareMessagingManager without any client side or server side addresses specified.
    */
   public ClassLoaderAwareMessagingManager()
   {
      super();
   }   
   
	/**
	 * Creates a ClassLoaderAwareMessagingManager without any client side or server side addresses specified.
	 * 
	 * @param parent the parent of this ClassLoaderAwareMessagingManager.
	 */
   public ClassLoaderAwareMessagingManager(SubSystem parent)
   {
      super(parent);
   }

	/**
	 * Creates a ClassLoaderAwareMessagingManager without any client side or server side addresses specified.
	 * 
	 * @param parent the parent of this ClassLoaderAwareMessagingManager.
	 * @param name the name that will be given to this ClassLoaderAwareMessagingManager.
	 */
   public ClassLoaderAwareMessagingManager(SubSystem parent, String name)
   {
      super(parent, name);
   }

	/**
	 * Creates a ClassLoaderAwareMessagingManager with a single client and server address specified.
	 * 
	 * @param parent the parent of this ClassLoaderAwareMessagingManager.
	 * @param name the name that will be given to this ClassLoaderAwareMessagingManager.
	 * @param remoteAddress remote address (address to connect to). May be <code>null</code>.
	 * @param localAddress local address (local address to listen on). May be <code>null</code>.
	 */
   public ClassLoaderAwareMessagingManager(SubSystem parent, String name, TcpEndPointIdentifier remoteAddress, TcpEndPointIdentifier localAddress)
   {
      super(parent, name, remoteAddress, localAddress);
   }

	/**
	 * Creates a ClassLoaderAwareMessagingManager with a several client and server address specified.
	 * 
	 * @param parent the parent of this ClassLoaderAwareMessagingManager.
	 * @param name the name that will be given to this ClassLoaderAwareMessagingManager.
	 * @param remoteAddresses remote addresses (addresses to connect to). May be <code>null</code>.
	 * @param localAddresses local addresses (local addresses to listen on). May be <code>null</code>.
	 */
   public ClassLoaderAwareMessagingManager(SubSystem parent, String name, TcpEndPointIdentifier[] remoteAddresses, TcpEndPointIdentifier[] localAddresses)
   {
      super(parent, name, remoteAddresses, localAddresses);
   }
   
   /**
    * Method for creating a {@link Message} object, for representation of an incomming message. This implementation creates a 
    * {@link ClassLoaderAwareMessage} object.
    * 
    * @param header the header of the received message.
	 * @param endPoint the endpoint on which the message was received.
    * 
    * @see com.teletalk.jserver.tcp.messaging.MessagingManager#createMessage(MessageHeader, MessagingEndPoint)
    */
   protected Message createMessage(MessageHeader header, MessagingEndPoint endPoint)
   {
      return new ClassLoaderAwareMessage(header, endPoint);
   }
   
   /**
    * Factory method for creating a new MessageDispatcher. 
    * 
    * @param messageDispatcherProperties the properties to set for the message dispatcher.
    */
   protected MessageDispatcher createMessageDispatcher(final MessageDispatcherProperties messageDispatcherProperties)
   {
      return new ClassLoaderAwareMessageDispatcher(this, messageDispatcherProperties);
   }
}
