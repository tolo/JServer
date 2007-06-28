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

/**
 * This class functions as a container for a thread load value.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0 Build 757
 */
public class LoadValue
{
   private static final ThreadLocal loadStorage = new ThreadLocal()
   {
		protected synchronized Object initialValue() 
		{
		   return new LoadValue();
		}
   };
   
   static LoadValue getThreadLoadValue(final boolean register)
   {
      LoadValue loadValue = ((LoadValue)loadStorage.get());
      if( register && !loadValue.registered ) loadValue.registered = LoadManager.registerLoadThread();
      return loadValue;
   }

   /**
    * Gets the LoadValue for the current thread.
    */
   public static LoadValue getThreadLoadValue()
   {
      return getThreadLoadValue(true);
   }
   
   /**
    * Gets the load value for the current thread.
    */
   public static int getThreadLoad()
   {
      return getThreadLoadValue().getLoad();
   }
   
   /**
    * Sets the load value for the current thread.
    */
   public static void setThreadLoad(int load)
   {
      getThreadLoadValue().setLoad(load);
   }
   
   /**
    * Resets the load value for the current thread.
    */
   public static void resetThreadLoad()
   {
      getThreadLoadValue().setLoad(0);
   }
   
   
   private volatile int load;

   private boolean registered = false;
   
   /**
    * Creates a new load value.
    */
   protected LoadValue()
   {
      this.load = 0;
   }
   
   /**
    * Gets the load value.
    */
   public int getLoad()
   {
      return load;
   }
    
   /**
    * Sets the load value.
    */
   public void setLoad(int newLoad)
   {
      int oldLoad = this.load;
      if( this.registered )
      {
         LoadManager.loadChanged(newLoad - oldLoad);
      }
      else
      {
         this.registered = LoadManager.registerLoadThread();
         if( this.registered ) LoadManager.loadChanged(newLoad);
      }
      this.load = newLoad;
   }
}
