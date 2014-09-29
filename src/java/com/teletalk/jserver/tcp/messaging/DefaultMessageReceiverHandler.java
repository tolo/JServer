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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.teletalk.jserver.SubComponent;

/**
 * Default implementation message receiver handling implementation.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20050517)
 */
public class DefaultMessageReceiverHandler implements MessageReceiverHandler
{
   private final HashMap namedMessageReceivers;
   
   private MessagingManager messagingManager;
   
   /**
    * Creates a new DefaultMessageReceiverHandler.
    */
   public DefaultMessageReceiverHandler()
   {
      this.namedMessageReceivers = new HashMap();
   }
   
   /**
    * Gets the MessagingManager associated with this handler.
    */
   public MessagingManager getMessagingManager()
   {
      return messagingManager;
   }
   
   /**
    * Sets the MessagingManager associated with this handler.
    */
   public void setMessagingManager(MessagingManager messagingManager)
   {
      this.messagingManager = messagingManager;
   }
   
   /**
    * Called by the associated MessagingManager to initialize this handler. This method is called when the MessagingManager 
    * is starting up, after {@link #setMessagingManager(MessagingManager)} has been called.
    */
   public void initialize()
   {
   }
   
   /**
    * Called by the associated MessagingManager to shut down this handler. This method is called when the MessagingManager 
    * is shutting down. 
    */
   public void shutDown()
   {
   }
   
   /**
    * Sets the message receiver components of the MessagingManager, by adding/replacing the 
    * current mappings with those in <code>messageReceiverComponents</code>. The method  
    * {@link #registerMessageReceiverComponent(MessageReceiverComponent)} will be used to register the components.
    * 
    * @param messageReceiverComponents List containing MessageReceiverComponent objects.   
    * 
    * @since 2.1 (20050426)
    */
   public void setMessageReceiverComponents(final List messageReceiverComponents)
   {
      if( messageReceiverComponents != null )
      {
         MessageReceiverComponent messageReceiverComponent;
         
         for(int i=0; i<messageReceiverComponents.size(); i++)
         {
            messageReceiverComponent = (MessageReceiverComponent)messageReceiverComponents.get(i);
            if( messageReceiverComponent != null )
            {
               registerMessageReceiverComponent(messageReceiverComponent);
            }
         }
      }
   }
   
   /**
    * Sets the message receivers of the MessagingManager, by adding/replacing the 
    * current mappings with those in <code>messageReceiverMap</code>. The method 
    * {@link #registerMessageReceiver(MessageReceiver, String)} to register the message receivers.
    * 
    * @param messageReceiverMap Map containing name to MessageReceiver mappings.   
    * 
    * @since 2.1 (20050422)
    */
   public void setMessageReceivers(final Map messageReceiverMap)
   {
      if( messageReceiverMap != null )
      {
         Iterator names = messageReceiverMap.keySet().iterator();
         String name;
         MessageReceiver messageReceiver;
            
         while(names.hasNext())
         {
            name = (String)names.next();
            if( name != null )
            {
               messageReceiver = (MessageReceiver)messageReceiverMap.get(name);
               if( messageReceiver != null )
               {
                  registerMessageReceiver(messageReceiver, name);
               }
            }
         }
      }
   }
   
   /**
    * Registers a {@link MessageReceiverComponent} that will receive incomming messages sent to the receiver name
    * specified by the property for that purpose. The MessagingManager will register the name in its meta data,  
    * under the key {@link MessagingManager#NAMED_MESSAGE_RECEIVER_METADATA_KEY}. If a MessageReceiver is
    * already registered for the specified name, it will be removed (and returned).<br>
    * <br>
    * The specified message receiver component will be added as a subcomponent to the MessagingManager and engaged.
    * 
    * @param messageReceiverComponent the MessageReceiverComponent to register.
    * 
    * @return the previously registered MessageReceiver under the specified name, if any.
    * 
    * @since 2.0 Build 757
    */
   public MessageReceiver registerMessageReceiverComponent(final MessageReceiverComponent messageReceiverComponent)
   {
      if (messageReceiverComponent != null)
      {
         MessageReceiver previousMessageReceiver = this.reRegisterMessageReceiverInternal(messageReceiverComponent,
               messageReceiverComponent.registeredReceiverName, null);
         
         return previousMessageReceiver;
      }
      else return null;
   }

   /**
    * Registers a {@link MessageReceiver} that will receive incomming messages sent to the receiver name specified by
    * parameter <code>name</code>. The MessagingManager will register the name in its meta data,
    * under the key {@link MessagingManager#NAMED_MESSAGE_RECEIVER_METADATA_KEY}. If a MessageReceiver is already
    * registered for the specified name, it will be removed (and returned).
    * 
    * @param messageReceiver the MessageReceiver to register.
    * @param name the name that the MessageReceiver is to be associated with in the context of receiving messages.
    * 
    * @return the previously registered MessageReceiver under the specified name, if any.
    * 
    * @since 2.0 Build 757
    */
   public MessageReceiver registerMessageReceiver(final MessageReceiver messageReceiver, final String name)
   {
      if (messageReceiver instanceof MessageReceiverComponent)
      {
         return this.reRegisterMessageReceiverInternal(messageReceiver,
               ((MessageReceiverComponent) messageReceiver).registeredReceiverName, name);
      }
      else return this.reRegisterMessageReceiverInternal(messageReceiver, null, name);
   }

   /**
    * Re-registers a {@link MessageReceiver}that will receive incomming messages sent to the receiver name specified by
    * parameter <code>newName</code>. The MessageReceiver will at the same time be unregistered from the old name.
    * <br>
    * <br>
    * The MessagingManager will register the new name in its meta data, under the key
    * {@link MessagingManager#NAMED_MESSAGE_RECEIVER_METADATA_KEY}. If a MessageReceiver is already registered for the
    * specified name, it will be removed (and returned).
    * 
    * @param messageReceiver the MessageReceiver to register.
    * @param oldName the old name that the MessageReceiver was associated with in the context of receiving messages.
    * @param newName the name that the MessageReceiver is to be associated with in the context of receiving messages.
    *  
    * @since 2.0 Build 757
    */
   public MessageReceiver reRegisterMessageReceiver(final MessageReceiver messageReceiver, final String oldName,
         final String newName)
   {
      return this.reRegisterMessageReceiverInternal(messageReceiver, oldName, newName);
   }
   
   /**
    * 
    */
   private MessageReceiver reRegisterMessageReceiverInternal(final MessageReceiver messageReceiver,
         final String oldName, String newName)
   {
      if( messageReceiver == null ) throw new NullPointerException("MessageReceiver cannot be null!");
      
      MessageReceiver existingMessageReceiverForName = null;
      SubComponent componentToRemove = null;
      MessageReceiverComponent messageReceiverComponent = null;
      
      if (messageReceiver instanceof MessageReceiverComponent)
      {
         messageReceiverComponent = (MessageReceiverComponent) messageReceiver;
         
         // Assume ownership over message receiver component and make sure properties are reinitialized
         if( !this.messagingManager.hasSubComponent(messageReceiverComponent) )
         {
            messagingManager.addSubComponent(messageReceiverComponent);
            
            // Engage messageReceiverComponent only if messaging manager is running
            if (  messagingManager.isEnabled() && 
                  !messageReceiverComponent.isEnabled() && !messageReceiverComponent.isInitializing() ) messageReceiverComponent.engage();
         }

         if( newName == null ) newName = messageReceiverComponent.getMessageReceiverName();
      }
      
      if (messageReceiver != null)
      {
         // If name is null - generate unique name (which most likely will be modified later.....when the components are in their correct locations.....)
         if( newName == null ) newName = messageReceiver.getClass().getName() + "@" + System.identityHashCode(messageReceiver);
         
         messagingManager.logInfo("(Re)registering message receiver " + messageReceiver + " under name '" + newName + "'.");
         
         boolean removeOld = false;

         // Remove old mapping, if mapping was with specified message receiver
         if (messageReceiver == this.namedMessageReceivers.get(oldName))
         {
            this.namedMessageReceivers.remove(oldName);
            removeOld = true;
         }
         existingMessageReceiverForName = (MessageReceiver) namedMessageReceivers.put(newName, messageReceiver);

         // Remove existingMessageReceiverForName as SubComponent, if not null...
         if (existingMessageReceiverForName instanceof SubComponent)
         {
            componentToRemove = (SubComponent) existingMessageReceiverForName;
         }

         if (messageReceiver instanceof MessageReceiverComponent)
         {
            messageReceiverComponent.updateMessageReceiverName(newName);
         }
         if (messageReceiver instanceof MessagingManagerAwareMessageReceiver)
         {
            ((MessagingManagerAwareMessageReceiver)messageReceiver).setMessagingManager(this.messagingManager);
         }

         // Register named message reveiver name i meta data
         ArrayList list = (ArrayList)messagingManager.getMetaData(MessagingManager.NAMED_MESSAGE_RECEIVER_METADATA_KEY);

         if (list == null)
         {
            list = new ArrayList();
         }
         else
         {
            if (removeOld) list.remove(oldName);
         }

         if (!list.contains(newName)) list.add(newName);
         
         messagingManager.setMetaData(MessagingManager.NAMED_MESSAGE_RECEIVER_METADATA_KEY, list);
      }

      if( componentToRemove != null ) this.messagingManager.removeSubComponent(componentToRemove);
      
      return existingMessageReceiverForName;
   }
   
   /**
    * Gets {@link MessageReceiver}objects.
    */
   public Map getMessageReceivers()
   {
      return (Map)this.namedMessageReceivers.clone();
   }
   
   /**
    * Gets the names of all message receivers.
    */
   public String[] getMessageReceiverNames()
   {
      return (String[])this.namedMessageReceivers.keySet().toArray(new String[0]);
   }

   /**
    * Gets the {@link MessageReceiver}that is registered with the specified name.
    * 
    * @param receiverName the name associated with a registered MessageReceiver.
    * 
    * @return a MessageReceiver registered with the specified name, or <code>null</code> if none was found.
    */
   public MessageReceiver getMessageReceiver(final String receiverName)
   {
      return (MessageReceiver) this.namedMessageReceivers.get(receiverName);
   }

   /**
    * Unregisters a MessageReceiver from the MessagingManager so that it will no longer receive messages.
    * 
    * @param receiverName the name of the MessageReceiver to remove.
    * 
    * @since 2.0 Build 757
    */
   public MessageReceiver unregisterMessageReceiver(final String receiverName)
   {
      MessageReceiver existingMessageReceiverForName = null;
      SubComponent componentToRemove = null;

      // Remove a named message reveiver mapping
      existingMessageReceiverForName = (MessageReceiver) namedMessageReceivers.remove(receiverName);

      if (existingMessageReceiverForName instanceof SubComponent)
      {
         componentToRemove = (SubComponent) existingMessageReceiverForName;
      }

      // Unregister named message reveiver name i meta data
      if (receiverName != null)
      {
         ArrayList list = (ArrayList) this.messagingManager.getMetaData(MessagingManager.NAMED_MESSAGE_RECEIVER_METADATA_KEY);

         if (list != null)
         {
            list.remove(receiverName);
            messagingManager.setMetaData(MessagingManager.NAMED_MESSAGE_RECEIVER_METADATA_KEY, list);
         }
      }
      
      if( componentToRemove != null ) this.messagingManager.removeSubComponent(componentToRemove);
      
      return existingMessageReceiverForName;
   }
}
