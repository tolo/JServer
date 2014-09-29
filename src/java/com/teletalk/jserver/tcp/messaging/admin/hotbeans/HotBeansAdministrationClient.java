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
package com.teletalk.jserver.tcp.messaging.admin.hotbeans;

import hotbeans.HotBeanModuleInfo;
import hotbeans.HotBeanModuleRepository;

import java.io.InputStream;

import com.teletalk.jserver.tcp.messaging.MessageHeader;
import com.teletalk.jserver.tcp.messaging.rpc.MessagingRpcInterface;
import com.teletalk.jserver.tcp.messaging.rpc.RpcInputStream;

/**
 * Adapter class that translates the HotBeanModuleRepository interface into the more messaging friendly interface {@link HotBeansAdministrationInterface}.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1.6 (20070508)
 */
public class HotBeansAdministrationClient implements HotBeanModuleRepository
{
   private MessagingRpcInterface messagingRpcInterface;
   
   private HotBeansAdministrationInterface hotBeansMessagingInterface;
   
   private int remoteVersion = -1; // Cache version
   
   private String name = null; // Cache name
   
   
   /**
    * Creates a new HotBeansMessagingClient.
    */
   public HotBeansAdministrationClient(final MessagingRpcInterface messagingRpcInterface)
   {
      this(messagingRpcInterface, null);
   }
   
   /**
    * Creates a new HotBeansMessagingClient.
    */
   public HotBeansAdministrationClient(final MessagingRpcInterface messagingRpcInterface, final String rpcHandlerName)
   {
      this.messagingRpcInterface = messagingRpcInterface;
      this.hotBeansMessagingInterface = (HotBeansAdministrationInterface)messagingRpcInterface.createProxy(HotBeansAdministrationInterface.class, rpcHandlerName);
   }
   
   /**
    * Gets the associated HotBeansMessagingInterface.
    */
   public HotBeansAdministrationInterface getHotBeansMessagingInterface()
   {
      return hotBeansMessagingInterface;
   }
   
   /**
    * Gets the associated MessagingRpcInterface.
    */
   public MessagingRpcInterface getMessagingRpcInterface()
   {
      return messagingRpcInterface;
   }
   
   
   /* ###### */

   
   /**
    * Gets the version of this HotBeansMessagingInterface supported by the remote side.
    */
   public int getInterfaceVersion()
   {
      if( remoteVersion < 0 )
      {
         remoteVersion = this.hotBeansMessagingInterface.getInterfaceVersion();
      }
      return remoteVersion;
   }

   public String getName()
   {
      if( this.name == null )
      {
         this.name = hotBeansMessagingInterface.getName();
      }
      return this.name;
   }
   
   
   public HotBeanModuleInfo addHotBeanModule(InputStream moduleFile)
   {
      return this.hotBeansMessagingInterface.addHotBeanModule((RpcInputStream)moduleFile);
   }
   
   public HotBeanModuleInfo updateHotBeanModule(String moduleName, InputStream moduleFile)
   {
      MessageHeader messageHeader = messagingRpcInterface.getRpcMessageHeader();
      messageHeader.setCustomHeaderField(HotBeansAdministrationInterface.MODULE_NAME_PARAM, moduleName);
      
      try
      {
         MessagingRpcInterface.setContextMessageHeader(messageHeader);
         return this.hotBeansMessagingInterface.updateHotBeanModule((RpcInputStream)moduleFile);
      }
      finally
      {
         MessagingRpcInterface.setContextMessageHeader(null); 
      }
   }
   
   public HotBeanModuleInfo revertHotBeanModule(String moduleName, long revision)
   {
      return this.hotBeansMessagingInterface.revertHotBeanModule(moduleName, revision);
   }
   
   public void removeHotBeanModule(String moduleName)
   {
      this.hotBeansMessagingInterface.removeHotBeanModule(moduleName);      
   }

   
   public String[] getHotBeanModuleNames()
   {
      return hotBeansMessagingInterface.getHotBeanModuleNames();
   }

   public HotBeanModuleInfo[] getHotBeanModuleInfo()
   {
      return hotBeansMessagingInterface.getHotBeanModuleInfo();
   }

   public HotBeanModuleInfo[] getHotBeanModuleInfo(String moduleName)
   {
      return hotBeansMessagingInterface.getHotBeanModuleInfo(moduleName);
   }
   
   public HotBeanModuleInfo getCurrentHotBeanModuleInfo(String moduleName)
   {
      return hotBeansMessagingInterface.getCurrentHotBeanModuleInfo(moduleName);
   }

   public boolean hasHotBeanModule(String moduleName)
   {
      return hotBeansMessagingInterface.hasHotBeanModule(moduleName);
   }
   
   public boolean hasHotBean(final String moduleName, final String beanName)
   {
      if( this.getInterfaceVersion() > 2 ) return hotBeansMessagingInterface.hasHotBean(moduleName, beanName);
      else return false;
   }
   
   public Object getHotBean(String moduleName, String beanName, Class interfaceClass)
   {
      return hotBeansMessagingInterface.getHotBean(moduleName, beanName, interfaceClass);
   }

   public Object getHotBean(String moduleName, String beanName, Class[] interfaceClasses)
   {
      return hotBeansMessagingInterface.getHotBean(moduleName, beanName, interfaceClasses);
   }
   
   public String getHotBeanClassName(String moduleName, String beanName)
   {
      if( this.getInterfaceVersion() > 1 ) return hotBeansMessagingInterface.getHotBeanClassName(moduleName, beanName);
      else return null;
   }

   public Class getHotBeanClass(String moduleName, String beanName)
   {
      if( this.getInterfaceVersion() > 1 ) return hotBeansMessagingInterface.getHotBeanClass(moduleName, beanName);
      else return null;
   }
}
