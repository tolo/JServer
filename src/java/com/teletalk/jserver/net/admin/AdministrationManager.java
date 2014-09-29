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
package com.teletalk.jserver.net.admin;

import java.util.HashMap;

/**
 * Manager for remote administration the server.<br>
 * <br>
 * Note: Currently this class only offers functionality for registration of custom administration handlers. In the future however, this 
 * class will become the main class of server administration.  
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1.4 (20060503)
 */
public class AdministrationManager
{
   private static AdministrationManager singleton = null;
   
   /**
    * Gets the global AdministrationManager instance.
    */
   public static AdministrationManager getAdministrationManager()
   {
      synchronized(AdministrationManager.class)
      {
         if( singleton == null ) singleton = new AdministrationManager();
         
         return singleton;
      }
   }
   
   
   private final HashMap handlerMap;
   
   
   private AdministrationManager()
   {
      this.handlerMap = new HashMap(); 
   }
   
   /**
    * Registers an administration handler.
    * 
    * @param name the name of the handler.
    * @param handler the handler.
    * 
    * @throws NullPointerException if name is null.
    */
   public void addAdministrationHandler(final String name, final Object handler)
   {
      if( name == null ) throw new NullPointerException("Handler name may not be null!");
      
      synchronized(this.handlerMap)
      {
         this.handlerMap.put(name, handler);
      }
   }
   
   /**
    * Removes an administration handler.
    * 
    * @param name the name of the handler to remove.
    */
   public Object removeAdministrationHandler(final String name)
   {
      synchronized(this.handlerMap)
      {
         return this.handlerMap.remove(name);
      }
   }
   
   /**
    * Gets an administration handler.
    * 
    * @param name the name of the handler to get.
    */
   public Object getAdministrationHandler(final String name)
   {
      synchronized(this.handlerMap)
      {
         return this.handlerMap.get(name);
      }
   }
   
   /**
    * Gets the names of all registered administration handlers.
    */
   public String[] getAdministrationHandlerNames()
   {
      synchronized(this.handlerMap)
      {
         return (String[])this.handlerMap.keySet().toArray(new String[0]);
      }
   }
}
