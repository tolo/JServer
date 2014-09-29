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
package com.teletalk.jserver.load;

import com.teletalk.jserver.property.VectorPropertyItem;

/**
 * Container class for theads registered as load values in a LoadManager.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0 Build 757
 */
public class LoadThreadContainer implements VectorPropertyItem
{
   private final Thread thread;
   
   private LoadValue loadValue;
   
   private final String key; // For VectorProperty

   /**
    * Creates a new LoadThreadContainer.
    * 
    * @param thread the thread.
    * @param loadValue the container for the load value. 
    */
   public LoadThreadContainer(final Thread thread, final LoadValue loadValue)
   {
      this.thread = thread;
      this.loadValue = loadValue;
      
      this.key = "ID: " + Long.toHexString(System.identityHashCode(this.thread));
   }
   
   /**
    * Destroys this LoadThreadContainer.
    */
   public void destroy()
   {
      this.loadValue = null;
   }

   /**
    * Gets the thread of this LoadThreadContainer.
    */
   public Thread getThread()
   {
      return this.thread;
   }
   
   /**
    * Gets the LoadValue of this LoadThreadContainer.
    */
   LoadValue getLoadValue()
   {
      return loadValue;
   }
   
   /**
    * Gets the load value of this LoadThreadContainer.
    */
   public int getLoad()
   {
      if( this.loadValue != null ) return this.loadValue.getLoad();
      else return -1;
   }
      
   /**
    * Gets the description of this LoadThreadContainer.
    */
   public String getDescription()
   {
      return this.thread.getName() + " - Load: " + this.getLoad();
   }
   
   /**
    * Gets the key (id) of this LoadThreadContainer for usage in a VectoryProperty.
    */
   public String getKey()
   {
      return this.key;
   }
}
