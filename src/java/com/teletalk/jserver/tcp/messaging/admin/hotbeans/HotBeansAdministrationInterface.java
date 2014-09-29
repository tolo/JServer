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

import hotbeans.BeanNotFoundException;
import hotbeans.HotBeanModuleInfo;
import hotbeans.ModuleNotFoundException;

import com.teletalk.jserver.tcp.messaging.rpc.RpcInputStream;

/**
 * Interface for RPC based messaging communication with a HotBeanModuleRepository.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1.6 (20070508)
 */
public interface HotBeansAdministrationInterface 
{
   /** 
    * The handler name prefix used for registration of a HotBeansRepository as a JServer administration handler. 
    * The administration handler name will consist of the prefix followed by a dot followed by the name of the repository. 
    */
   public static final String HOT_BEANS_ADMINISTRATION_HANDLER_PREFIX = "hotbeans";
      
   /** The name of parameter containing the module name. Used in the method updateHotBeanModule. */
   public static final String MODULE_NAME_PARAM = "moduleName";
   
   
   /**
    * The version of the interface.
    */
   public static final int INTERFACE_VERSION = 3;
   
   
   /**
    * Gets the version of this HotBeansMessagingInterface supported by the remote side.
    */
   public int getInterfaceVersion();
   
   
   /**
    * Gets the name of this HotBeanModuleRepository.
    */
   public String getName();
   
   
   /**
    * Adds (installs) a new hot bean module.
    */
   public HotBeanModuleInfo addHotBeanModule(RpcInputStream moduleFile);
   
   /**
    * Updates an existing hot beans module with a new revision.
    */
   public HotBeanModuleInfo updateHotBeanModule(RpcInputStream moduleFile);
   
   /**
    * Reverts an hot beans module to a previous revision (which becomes a new revision).
    */
   public HotBeanModuleInfo revertHotBeanModule(String moduleName, long revision);
   
   /**
    * Removes all revisions of a hot bean module.
    */
   public void removeHotBeanModule(String moduleName);
   
   
   /**
    * Gets the names of all existing hot bean modules.
    */
   public String[] getHotBeanModuleNames();
   
   /**
    * Gets information about the current revisions of all installed hot bean modules.
    */
   public HotBeanModuleInfo[] getHotBeanModuleInfo(); 
   
   /**
    * Gets information about all revisions of a specific hot bean module.
    */
   public HotBeanModuleInfo[] getHotBeanModuleInfo(String moduleName);
   
   /**
    * Gets information about the current revisions of a specific hot bean module.
    */
   public HotBeanModuleInfo getCurrentHotBeanModuleInfo(String moduleName);
   
   /**
    * Checks if a module with the specified name exists. 
    */
   public boolean hasHotBeanModule(final String moduleName);
   

   /**
    * Checks if the specified module contains a bean with the specified name.
    * 
    * @since 1.0.1 (20070302)
    * @since Interface version 3
    */
   public boolean hasHotBean(final String moduleName, final String beanName);
   
   /**
    * Gets a reference, via a proxy, to a the hot bean with the specified name in the specified module. This method will return 
    * an object even if the hot bean module doesn't exist (yet). When invoking methods on the proxy, exceptions of the types 
    * {@link ModuleNotFoundException} and {@link BeanNotFoundException} will be thrown to indicate that the module or bean 
    * wasn't found.
    */
   public Object getHotBean(String moduleName, String beanName, Class interfaceClass);
   
   /**
    * Gets a reference, via a proxy, to a the hot bean with the specified name in the specified module. This method will return 
    * an object even if the hot bean module doesn't exist (yet). When invoking methods on the proxy, exceptions of the types 
    * {@link ModuleNotFoundException} and {@link BeanNotFoundException} will be thrown to indicate that the module or bean 
    * wasn't found.
    */
   public Object getHotBean(String moduleName, String beanName, Class[] interfaceClasses);
   
   /**
    * Gets the class name of the bean with the specified name in the specified module.
    * 
    * @since 1.0.1 (20070212)
    * @since Interface version 2
    */
   public String getHotBeanClassName(String moduleName, String beanName);
   
   /**
    * Gets the class of the bean with the specified name in the specified module.
    * 
    * @since 1.0.1 (20070212)
    * @since Interface version 2
    */
   public Class getHotBeanClass(String moduleName, String beanName);
}
