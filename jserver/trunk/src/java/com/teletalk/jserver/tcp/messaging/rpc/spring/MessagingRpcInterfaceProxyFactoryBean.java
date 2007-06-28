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
package com.teletalk.jserver.tcp.messaging.rpc.spring;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.FactoryBean;
import org.springframework.beans.factory.InitializingBean;

import com.teletalk.jserver.tcp.messaging.MessageDispatcherProperties;
import com.teletalk.jserver.tcp.messaging.MessagingManager;
import com.teletalk.jserver.tcp.messaging.rpc.MessagingRpcInterface;

/**
 * Factory bean implementation that enables the creation of an Messaging RPC-enabled dynamic proxy.  
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1.4 (20060601)
 * 
 * @see com.teletalk.jserver.tcp.messaging.rpc.MessagingRpcInterface
 */
public class MessagingRpcInterfaceProxyFactoryBean implements InitializingBean, FactoryBean
{
   private MessagingManager messagingManager;
   
   private Class[] interfaces;
   
   private String receiverName;
   
   private String rpcHandlerName;
   
   private boolean registerReceiverNameAsServiceName;
   
   
   private Object rpcInterface;
      
   private MessagingRpcInterface messagingRpcInterface;
   
   
   
   /**
    * Invoked by a BeanFactory after it has set all bean properties.
    */
   public final void afterPropertiesSet() throws Exception 
   {
      this.messagingRpcInterface = this.messagingManager.getMessagingRpcInterface(new MessageDispatcherProperties(this.receiverName));
      
      if( rpcHandlerName != null ) this.rpcInterface = this.messagingRpcInterface.createProxy(this.interfaces, this.rpcHandlerName);
      else this.rpcInterface = this.messagingRpcInterface.createProxy(this.interfaces);
      
      if( this.registerReceiverNameAsServiceName )
      {
         // Initialize properties of MessagingManager from persistent storage, before setting remote service name 
         if( !this.messagingManager.propertiesInitializedFromPersistentStorage() ) this.messagingManager.initProperties();
         
         this.messagingManager.addRemoteServiceName(this.receiverName);
      }
   }
   
   /**
    * Checks if the bean managed by this factory is a singleton.
    */
   public final boolean isSingleton() 
   {
      return true;
   }
   
   /**
    * Gets a proxy for a bean in a hot bean module. 
    */
   public synchronized final Object getObject() throws Exception 
   {
      return this.rpcInterface;
   }

   /**
    * Return the type of object that this FactoryBean creates.
    */
   public Class getObjectType()
   {
      return null;
   }

   
   /* ### PROPERTIEs ### */
   
   
   /**
    */
   public MessagingManager getMessagingManager()
   {
      return messagingManager;
   }

   /**
    */
   public void setMessagingManager(MessagingManager messagingManager)
   {
      this.messagingManager = messagingManager;
   }
   
   
   /**
    */
   public Class[] getInterfaces()
   {
      return interfaces;
   }

   /**
    */
   public void setInterfaces(Class[] interfaces)
   {
      this.interfaces = interfaces;
   }
   
   /**
    */
   public List getInterfaceClassNames()
   {
      if( this.interfaces != null )
      {
         List classNames = new ArrayList(this.interfaces.length);
         for (int i=0; i<this.interfaces.length; i++)
         {
            classNames.add(this.interfaces[i].getName());
         }
         return classNames;
      }
      else return null;
   }

   /**
    */
   public void setInterfaceClassNames(final List interfaceClassNames) throws ClassNotFoundException
   {
      if( interfaceClassNames != null )
      {
         this.interfaces = new Class[interfaceClassNames.size()];
         
         for (int i=0; i<interfaceClassNames.size(); i++) 
         {
            this.interfaces[i] = Class.forName( ((String)interfaceClassNames.get(i)).trim() );
         }
      }
      else this.interfaces = null;
   }

   
   /**
    */
   public String getReceiverName()
   {
      return receiverName;
   }

   /**
    */
   public void setReceiverName(String receiverName)
   {
      this.receiverName = receiverName;
   }

   
   /**
    */
   public String getRpcHandlerName()
   {
      return rpcHandlerName;
   }

   /**
    */
   public void setRpcHandlerName(String rpcHandlerName)
   {
      this.rpcHandlerName = rpcHandlerName;
   }
   
   /**
    */
   public boolean isRegisterReceiverNameAsServiceName()
   {
      return registerReceiverNameAsServiceName;
   }

   /**
    */
   public void setRegisterReceiverNameAsServiceName(boolean registerReceiverNameAsServiceName)
   {
      this.registerReceiverNameAsServiceName = registerReceiverNameAsServiceName;
   }
}
