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

/**
 * Default implementation of the StatisticsSource interface which make use of the 
 * {@link CompositeStatisticsSupport} and {@link StatisticsEntrySupport} classes.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0.2
 */
public class DefaultStatisticsSource implements StatisticsSource
{
   static final long serialVersionUID = -846158580784400433L;

   private final CompositeStatisticsSupport compositeStatisticsSupport;
   
   private final StatisticsEntrySupport statisticsEntrySupport;
   
   /**
    * Creates a new DefaultStatisticsSource.
    */
   public DefaultStatisticsSource()
   {
      this.compositeStatisticsSupport = new CompositeStatisticsSupport();
      this.statisticsEntrySupport = new StatisticsEntrySupport();
   }
   
   
   /* ### STATISTICSSOURCE METHODS BEGIN ### */
   
   
   /**
    * Gets the names of the StatisticsSource object contained in this StatisticsSource object.
    */   
   public String[] getStatisticsSourceNames()
   {
      return compositeStatisticsSupport.getStatisticsSourceNames();
   }
   
   /**
    * Gets the StatisticsSource object with the specified name contained in this StatisticsSource object.
    */
   public StatisticsSource getStatisticsSource(String name)
   {
      return compositeStatisticsSupport.getStatisticsSource(name);
   }
   
   /**
    * Gets the names of the {@link StatisticsEntry} object contained in this StatisticsSource object.
    */
   public String[] getStatisticsEntryNames()
   {
      return this.statisticsEntrySupport.getStatisticsEntryNames();
   }
   
   /**
    * Gets the {@link StatisticsEntry} object with the specified name contained in this StatisticsSource object.
    */
   public StatisticsEntry getStatisticsEntry(String name)
   {
      return this.statisticsEntrySupport.getStatisticsEntry(name);
   }
   
   /**
    * Resets all the {@link StatisticsEntry} objects contained in this object. This call will cascade to all the 
    * StatisticsSource objects contained in this object.
    * 
    * @see StatisticsEntry#reset() 
    */
   public void reset()
   {
      this.compositeStatisticsSupport.reset();
      this.statisticsEntrySupport.reset();
   }
   
   
   /* ### STATISTICSSOURCE METHODS END ### */
   
   
   /**
    * Adds a StatisticsSource.
    */
   public void addSource(String name, StatisticsSource runtimeStatisticsSource)
   {
      compositeStatisticsSupport.addStatisticsSource(name, runtimeStatisticsSource);
   }
   
   /**
    * Removes a StatisticsSource.
    */   
   public StatisticsSource removeSource(String name)
   {
      return compositeStatisticsSupport.removeStatisticsSource(name);
   }
   
   /**
    * Adds a StatisticsEntry.
    */
   public void addEntry(String name, StatisticsEntry runtimeStatisticsEntry)
   {
      this.statisticsEntrySupport.addEntry(name, runtimeStatisticsEntry);
   }
   
   /**
    * Removes a StatisticsEntry.
    */
   public StatisticsEntry removeEntry(String name)
   {
      return this.statisticsEntrySupport.removeEntry(name);
   }
}
