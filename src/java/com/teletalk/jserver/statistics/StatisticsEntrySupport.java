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
package com.teletalk.jserver.statistics;

import java.util.HashMap;
import java.util.Iterator;

/**
 * Statistics source class with support for storage of {@link StatisticsEntry} objects.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0.2
 */
public class StatisticsEntrySupport implements StatisticsSource
{
   static final long serialVersionUID = 2819407830793952407L;

   private final HashMap entries;
   
   /**
    * Creates a StatisticsEntrySupport.
    */
   public StatisticsEntrySupport()
   {
      this.entries = new HashMap();
   }
   
   
   /* ### STATISTICSSOURCE METHODS BEGIN ### */
   
   
   /**
    * Gets the names of the StatisticsSource object contained in this StatisticsSource object.
    */
   public String[] getStatisticsSourceNames()
   {
      return null;
   }
   
   /**
    * Gets the StatisticsSource object with the specified name contained in this StatisticsSource object.
    */
   public StatisticsSource getStatisticsSource(String name)
   {
      return null;
   }
   
   /**
    * Gets the names of the {@link StatisticsEntry} object contained in this StatisticsSource object.
    */
   public String[] getStatisticsEntryNames()
   {
     synchronized(this.entries)
     {
        return (String[])entries.keySet().toArray(CompositeStatisticsSupport.StringArrayType);
     }
   }
   
   /**
    * Gets the {@link StatisticsEntry} object with the specified name contained in this StatisticsSource object.
    */
   public StatisticsEntry getStatisticsEntry(String name)
   {
      synchronized(this.entries)
      { 
         return (StatisticsEntry)this.entries.get(name);
      }
   }
   
   /**
    * Resets all the {@link StatisticsEntry} objects contained in this object. This call will cascade to all the 
    * StatisticsSource objects contained in this object.
    * 
    * @see StatisticsEntry#reset() 
    */
   public void reset()
   {
      synchronized(this.entries)
      { 
         Iterator it = this.entries.values().iterator();
         StatisticsEntry runtimeStatisticsEntry;
         
         while(it.hasNext())
         {
            runtimeStatisticsEntry = (StatisticsEntry)it.next();
            if( runtimeStatisticsEntry != null )
            {
               runtimeStatisticsEntry.reset();
            }
         }
      }
   }
   
   
   /* ### STATISTICSSOURCE METHODS END ### */
   
   
   /**
    * Adds an entry.
    */
   public void addEntry(String name, StatisticsEntry runtimeStatisticsEntry)
   {
      synchronized(this.entries)
      {
         this.entries.put(name, runtimeStatisticsEntry);
      }
   }
   
   /**
    * Removes an entry.
    */
   public StatisticsEntry removeEntry(String name)
   {
      synchronized(this.entries)
      { 
         return (StatisticsEntry)this.entries.remove(name);
      }
   }
}
