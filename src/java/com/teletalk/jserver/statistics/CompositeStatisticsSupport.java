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
 * Statistics source class with support for child StatisticsSource objects. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0.2
 */
public class CompositeStatisticsSupport implements StatisticsSource
{
   static final long serialVersionUID = 717813876174418884L;

   static final String[] StringArrayType = new String[0];
   
   
   private final HashMap sources;
   
   /**
    * Creates a new CompositeStatisticsSupport.
    */
   public CompositeStatisticsSupport()
   {
      this.sources = new HashMap();
   }
   
   
   /* ### STATISTICSSOURCE METHODS BEGIN ### */
   
      
   /**
    * Gets the names of the StatisticsSource object contained in this StatisticsSource object.
    */
   public String[] getStatisticsSourceNames()
   {
     synchronized(this.sources)
     {
        return (String[])sources.keySet().toArray(StringArrayType);
     }
   } 
   
   /**
    * Gets the StatisticsSource object with the specified name contained in this StatisticsSource object.
    */
   public StatisticsSource getStatisticsSource(String name)
   {
      synchronized(this.sources)
      { 
         return (StatisticsSource)this.sources.get(name);
      }
   }
   
   /**
    * Gets the {@link StatisticsEntry} object with the specified name contained in this StatisticsSource object.
    */
   public StatisticsEntry getStatisticsEntry(String name)
   {
      return null;
   }
   
   /**
    * Gets the {@link StatisticsEntry} object with the specified name contained in this StatisticsSource object.
    */
   public String[] getStatisticsEntryNames()
   {
      return null;
   }
   
   /**
    * Resets all the {@link StatisticsEntry} objects contained in this object. This call will cascade to all the 
    * StatisticsSource objects contained in this object.
    * 
    * @see StatisticsEntry#reset() 
    */
   public void reset()
   {
      synchronized(this.sources)
      { 
         Iterator it = this.sources.values().iterator();
         StatisticsSource runtimeStatisticsSource;
         
         while(it.hasNext())
         {
            runtimeStatisticsSource = (StatisticsSource)it.next();
            if( runtimeStatisticsSource != null )
            {
               runtimeStatisticsSource.reset();
            }
         }
      }
   }
   
   
   /* ### STATISTICSSOURCE METHODS END ### */
   
   
   /**
    * Adds a {@link StatisticsSource} object.
    */
   public void addStatisticsSource(String name, StatisticsSource statisticsSource)
   {
      synchronized(this.sources)
      {
         this.sources.put(name, statisticsSource);
      }
   }
   
   /**
    * Removes a {@link StatisticsSource} object.
    */
   public StatisticsSource removeStatisticsSource(String name)
   {
      synchronized(this.sources)
      { 
         return (StatisticsSource)this.sources.remove(name);
      }
   }
}
