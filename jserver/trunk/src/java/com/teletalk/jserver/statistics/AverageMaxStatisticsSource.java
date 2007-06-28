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
 * Statistics source class for keeping track of the average and max values associated with a value. 
 * For this purpose, this class contains three nested statistics entry object ({@link NumericalStatisticsEntry}) 
 * for the total count (i.e. the number of times the value has changed), the average value and the maximum value. 
 * The statisistics are updated using the method {@link #update(long)}.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0.2
 */
public class AverageMaxStatisticsSource implements StatisticsSource
{
   static final long serialVersionUID = -5613226337460308366L;

   public static final String COUNT_DEFAULT_ENTRY_NAME = "count";
   
   public static final String AVERAGE_DEFAULT_ENTRY_NAME = "average";
   
   public static final String MAX_DEFAULT_ENTRY_NAME = "max";
      
   
   protected final CompositeStatisticsSupport compositeStatisticsSupport;
   
   private final String countEntryName;
   
   private final String averageEntryName;
   
   private final String maxEntryName;
   
   private final String[] EntryNames;
   
   private final NumericalStatisticsEntry count;
   
   private long total;
   
   private final NumericalStatisticsEntry average;
   
   private final NumericalStatisticsEntry max;
   
   /**
    * Creates a new AverageMaxStatisticsSource.
    */
   public AverageMaxStatisticsSource(final boolean hasSources)
   {
      this(hasSources, COUNT_DEFAULT_ENTRY_NAME, AVERAGE_DEFAULT_ENTRY_NAME, MAX_DEFAULT_ENTRY_NAME);
   }
   
   /**
    * Creates a new AverageMaxStatisticsSource.
    */
   public AverageMaxStatisticsSource(final boolean hasSources, String averageEntryName, String maxEntryName)
   {
      this(hasSources, COUNT_DEFAULT_ENTRY_NAME, averageEntryName, maxEntryName);
   }
   
   /**
    * Creates a new AverageMaxStatisticsSource.
    */
   public AverageMaxStatisticsSource(final boolean hasSources, String countEntryName, String averageEntryName, String maxEntryName)
   {
      if( hasSources ) this.compositeStatisticsSupport = new CompositeStatisticsSupport();
      else this.compositeStatisticsSupport = null;
      
      this.countEntryName = countEntryName;
      this.averageEntryName = averageEntryName;
      this.maxEntryName = maxEntryName;
      this.EntryNames = new String[]{this.countEntryName, this.averageEntryName, this.maxEntryName};
      
      this.count = new NumericalStatisticsEntry();
      this.total = 0;
      this.average = new NumericalStatisticsEntry();
      this.max = new NumericalStatisticsEntry();
   }
   
   
   /* ### STATISTICSSOURCE METHODS BEGIN ### */
   
   
   /**
    * Gets the names of the StatisticsSource object contained in this StatisticsSource object.
    */
   public String[] getStatisticsSourceNames()
   {
      if( this.compositeStatisticsSupport != null ) return this.compositeStatisticsSupport.getStatisticsSourceNames();
      else return null;
   }
   
   /**
    * Gets the StatisticsSource object with the specified name contained in this StatisticsSource object.
    */
   public StatisticsSource getStatisticsSource(String name)
   {
      if( this.compositeStatisticsSupport != null ) return this.compositeStatisticsSupport.getStatisticsSource(name);
      else return null;
   }
   
   /**
    * Gets the names of the {@link StatisticsEntry} object contained in this StatisticsSource object.
    */
   public String[] getStatisticsEntryNames()
   {
      return EntryNames;
   }
   
   /**
    * Gets the {@link StatisticsEntry} object with the specified name contained in this StatisticsSource object.
    */
   public StatisticsEntry getStatisticsEntry(String name)
   {
      if( this.countEntryName.equals(name) ) return count;
      else if( this.averageEntryName.equals(name) ) return average;
      else if( this.maxEntryName.equals(name) ) return max;
      else return null;
   }
   
   /**
    * Resets all the {@link StatisticsEntry} objects contained in this object. This call will cascade to all the 
    * StatisticsSource objects contained in this object.
    * 
    * @see StatisticsEntry#reset() 
    */
   public synchronized void reset()
   {
      if( this.compositeStatisticsSupport != null ) this.compositeStatisticsSupport.reset();
      this.count.reset();
      this.total = 0;
      this.average.reset();
      this.max.reset();
   }
   
   
   /* ### STATISTICSSOURCE METHODS END ### */
   
   /**
    * Gets the {@link NumericalStatisticsEntry} for the count.
    */
   public NumericalStatisticsEntry getCountStatisticsEntry()
   {
      return this.count;
   }
   
   /**
    * Gets the {@link NumericalStatisticsEntry} for the average value.
    */
   public NumericalStatisticsEntry getAverageStatisticsEntry()
   {
      return this.average;
   }
   
   /**
    * Gets the {@link NumericalStatisticsEntry} for the max value.
    */
   public NumericalStatisticsEntry getMaxStatisticsEntry()
   {
      return this.max;
   }
   
   /**
    * Sets the suffix to be used for the average and max values.
    */
   public void setSuffix(String suffix)
   {
      this.average.setSuffix(suffix);
      this.max.setSuffix(suffix);
   }
   
   /**
    * Gets the suffix to be used for the average and max values.
    */
   public String getSuffix()
   {
      return this.average.getSuffix();
   }
   
   /**
    * Adds a {@link StatisticsSource} object this object.
    */
   public void addSource(final String name, final StatisticsSource statisticsSource)
   {
      if( compositeStatisticsSupport != null ) compositeStatisticsSupport.addStatisticsSource(name, statisticsSource);
   }
   
   /**
    * Removes a {@link StatisticsSource} object from this object.
    */   
   public StatisticsSource removeSource(final String name)
   {
      if( compositeStatisticsSupport != null ) return compositeStatisticsSupport.removeStatisticsSource(name);
      else return null;
   }
   
   /**
    * Gets the total value.
    */
   public long getTotal()
   {
      return total;
   }
   
   /**
    * Sets the total value.
    */
   public synchronized void setTotal(long total)
   {
      this.total = total;
      
      this.count.increment(-1);
      this.update(0);
   }
   
   /**
    * Gets the count.
    */
   public long getCount()
   {
      return count.getLongValue();
   }
   
   /**
    * Sets the count.
    */
   public synchronized void setCount(long count)
   {
      this.count.setLongValue(count);
      
      this.count.increment(-1);
      this.update(0);
   }
   
   /**
    * Gets the average value.
    */   
   public long getAverage()
   {
      return average.getLongValue();
   }
   
   /**
    * Gets the max value.
    */   
   public long getMax()
   {
      return average.getLongValue();
   }   
   
   /**
    * Updates the statistics.
    */   
   public synchronized void update(long value)
   {
      if( (Long.MAX_VALUE - this.total) < value )
      {
         this.total = this.average.getLongValue();
         this.count.setLongValue(1);
      }
      
      this.count.increment();
      this.total += value;
      if( this.total > 0 ) this.average.setLongValue( this.total / this.count.getLongValue() );
      else this.average.setLongValue(0);
      if( value > this.max.getLongValue() ) this.max.setLongValue(value);
   }
   
   /**
    * Gets the name of the average statistics entry.
    */
   public String getAverageEntryName()
   {
      return averageEntryName;
   }
   
   /**
    * Gets the name of the count statistics entry.
    */
   public String getCountEntryName()
   {
      return countEntryName;
   }
   
   /**
    * Gets the name of the max statistics entry.
    */
   public String getMaxEntryName()
   {
      return maxEntryName;
   }
}
