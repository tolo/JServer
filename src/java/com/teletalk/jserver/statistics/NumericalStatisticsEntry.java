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
 * A StatisticsEntry implementation that support storage of an integer based statistics value.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0.2
 */
public class NumericalStatisticsEntry implements StatisticsEntry
{
   static final long serialVersionUID = -7623044656816954632L;

   private volatile long value;
   
   private String suffix;
   
   /**
    * Creates a new NumericalStatisticsEntry.
    */
   public NumericalStatisticsEntry()
   {
      this(0); 
   }
   
   /**
    * Creates a new NumericalStatisticsEntry.
    */
   public NumericalStatisticsEntry(long initialValue)
   {
      this(initialValue, null);
   }
   
   /**
    * Creates a new NumericalStatisticsEntry.
    */
   public NumericalStatisticsEntry(long initialValue, String suffix)
   {
      this.value = initialValue;
      this.suffix = suffix;
   }
   
   
   /* ### STATISTICSENTRY METHODS BEGIN ### */
   
   
   /**
    * Gets the value of this StatisticsEntry.
    */
   public String getValue()
   {
      if( this.suffix != null ) return String.valueOf(this.value) + this.suffix;
      else return String.valueOf(this.value);
   }
   
   /**
    * Resets the value of this StatisticsEntry.
    */
   public void reset()
   {
      this.value = 0;
   }
   
   
   /* ### STATISTICSENTRY METHODS BEGIN ### */
   
   
   /**
    * Gets the long value.
    */
   public long getLongValue()
   {
      return this.value;
   }
   
   /**
    * Sets the long value.
    */
   public void setLongValue(long value)
   {
      this.value = value;
   }
   
	/**
	 * Increments the value of this NumericalRuntimeStatisticsEntry with one(1).
	 */
	public void increment()
	{
		this.value++;
	}
	
	/**
	 * Increments the value of this NumericalRuntimeStatisticsEntry with i.
	 */
	public void increment(final long i)
	{
	   this.value += i;
	}
	
   /**
    * Gets the suffix for the value.
    */
   public String getSuffix()
   {
      return suffix;
   }
   
   /**
    * Sets the suffix for the value.
    */
   public void setSuffix(String suffix)
   {
      this.suffix = suffix;
   }	
}
