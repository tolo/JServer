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

import java.io.Serializable;

/**
 * Interface representing a source of statistics information. A statistics source may contain
 * statistics entry objects as well as other statistics sources. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0.2
 */
public interface StatisticsSource extends Serializable
{
   /**
    * Gets the names of the StatisticsSource object contained in this StatisticsSource object.
    */
   public String[] getStatisticsSourceNames();
   
   /**
    * Gets the StatisticsSource object with the specified name contained in this StatisticsSource object.
    */
   public StatisticsSource getStatisticsSource(String name);
   
   /**
    * Gets the names of the {@link StatisticsEntry} object contained in this StatisticsSource object.
    */
   public String[] getStatisticsEntryNames();
   
   /**
    * Gets the {@link StatisticsEntry} object with the specified name contained in this StatisticsSource object.
    */
   public StatisticsEntry getStatisticsEntry(String name);
   
   /**
    * Resets all the {@link StatisticsEntry} objects contained in this object. This call will cascade to all the 
    * StatisticsSource objects contained in this object.
    * 
    * @see StatisticsEntry#reset() 
    */
   public void reset();
}
