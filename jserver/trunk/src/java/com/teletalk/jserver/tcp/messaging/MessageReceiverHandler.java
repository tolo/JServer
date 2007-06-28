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

import java.util.List;
import java.util.Map;

/**
 * Interface for classes handling a repository of message receivers for a {@link MessagingManager}. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20050517)
 */
public interface MessageReceiverHandler extends MessagingManagerHandler
{
   /**
    * Registers a {@link MessageReceiverComponent} that will receive incomming messages sent to the receiver name
    * specified by the property for that purpose. This MessagingManager will register the name in the meta data of this
    * system, under the key {@link MessagingManager#NAMED_MESSAGE_RECEIVER_METADATA_KEY}. If a MessageReceiver is
    * already registered for the specified name, it will be removed (and returned).<br>
    * <br>
    * The specified message receiver component will be added as a subcomponent to this MessagingManager and engaged.
    * 
    * @param messageReceiverComponent the MessageReceiverComponent to register.
    * 
    * @return the previously registered MessageReceiver under the specified name, if any.
    */
   public MessageReceiver registerMessageReceiverComponent(final MessageReceiverComponent messageReceiverComponent);

   /**
    * Registers a {@link MessageReceiver} that will receive incomming messages sent to the receiver name specified by
    * parameter <code>name</code>. This MessagingManager will register the name in the meta data of this system,
    * under the key {@link MessagingManager#NAMED_MESSAGE_RECEIVER_METADATA_KEY}. If a MessageReceiver is already
    * registered for the specified name, it will be removed (and returned).
    * 
    * @param messageReceiver the MessageReceiver to register.
    * @param name the name that the MessageReceiver is to be associated with in the context of receiving messages.
    * 
    * @return the previously registered MessageReceiver under the specified name, if any.
    */
   public MessageReceiver registerMessageReceiver(final MessageReceiver messageReceiver, final String name);

   /**
    * Re-registers a {@link MessageReceiver}that will receive incomming messages sent to the receiver name specified by
    * parameter <code>newName</code>. The MessageReceiver will at the same time be unregistered from the old name.
    * <br>
    * <br>
    * This MessagingManager will register the new name in the meta data of this system, under the key
    * {@link MessagingManager#NAMED_MESSAGE_RECEIVER_METADATA_KEY}. If a MessageReceiver is already registered for the
    * specified name, it will be removed (and returned).
    * 
    * @param messageReceiver the MessageReceiver to register.
    * @param oldName the old name that the MessageReceiver was associated with in the context of receiving messages.
    * @param newName the name that the MessageReceiver is to be associated with in the context of receiving messages.
    */
   public MessageReceiver reRegisterMessageReceiver(final MessageReceiver messageReceiver, final String oldName,
         final String newName);
   
   /**
    * Sets the message receiver components of this MessagingManager, by adding/replacing the 
    * current mappings with those in <code>messageReceiverComponents</code>. The method  
    * {@link #registerMessageReceiverComponent(MessageReceiverComponent)} will be used to register the components.
    * 
    * @param messageReceiverComponents List containing MessageReceiverComponent objects.   
    */
   public void setMessageReceiverComponents(final List messageReceiverComponents);
   
   /**
    * Sets the message receivers of this MessagingManager, by adding/replacing the 
    * current mappings with those in <code>messageReceiverMap</code>. The method 
    * {@link #registerMessageReceiver(MessageReceiver, String)} to register the message receivers.
    * 
    * @param messageReceiverMap Map containing name to MessageReceiver mappings.   
    */
   public void setMessageReceivers(final Map messageReceiverMap);
   
   /**
    * Gets {@link MessageReceiver}objects registered with names in this MessagingManager.
    */
   public Map getMessageReceivers();
   
   /**
    * Gets the names of all message receivers registered in this MessagingManager.
    */
   public String[] getMessageReceiverNames();

   /**
    * Gets the {@link MessageReceiver}that is registered with the specified name in this MessagingManager.
    * 
    * @param receiverName the name associated with a registered MessageReceiver.
    * 
    * @return a MessageReceiver registered with the specified name, or <code>null</code> if none was found.
    */
   public MessageReceiver getMessageReceiver(final String receiverName);

   /**
    * Unregisters a MessageReceiver from this MessagingManager so that it will no longer receive messages.
    * 
    * @param receiverName the name of the MessageReceiver to remove.
    */
   public MessageReceiver unregisterMessageReceiver(final String receiverName);
}
