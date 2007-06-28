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
package com.teletalk.jserver.periodic;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * This class represents a periodicity and time offset used by a periodic action.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.2
 */
public class Periodicity
{
   public static final int INTERVAL    = 0;
	/**	Constant represeting hourly periodicity. */
	public static final int HOURLY		   = 1;
	/**	Constant represeting daily periodicity. */
	public static final int DAILY				= 2; 
	/**	Constant represeting weekly periodicity. */
	public static final int WEEKLY			= 3;	 
	/**	Constant represeting monthly periodicity. */
	public static final int MONTHLY	   = 4;
	
	/**	String array containing readable names for the periodicity constants (constant can be used as index).  */
	public static final String[] PeriodicityTypeNames = {"INTERVAL", "HOURLY", "DAILY", "WEEKLY", "MONTHLY"};
	
	private final int type;
   private final int dayOffset;
   private final int hourOffset;
   private final int minuteOffset;
	private final int interval;
		
   /**
    * Creates a new Periodicity object.
    */
   protected Periodicity() throws IllegalArgumentException
   {
      this.type = HOURLY;
      this.dayOffset = 0;
      this.hourOffset = 0;
      this.minuteOffset = 0;
      this.interval = 0;
   }
   
	/**
	 * Creates a new Periodicity object.
	 * 
	 * @param type the periodicity type (for instance {@link #HOURLY}).
	 * @param dayOffset the day offset of this periodicity.
	 * @param hourOffset the hour offset of this periodicity.
	 * @param minuteOffset the minute offset of this periodicity.
	 * @param interval the interval of this periodicity.
	 */
	public Periodicity(final int type, final int dayOffset, final int hourOffset, final int minuteOffset, final int interval) throws IllegalArgumentException
	{
		if( (type < 0) || (type > MONTHLY) ) throw new IllegalArgumentException("Invalid type: " + type + "!");
		
		this.type = type;
      this.dayOffset = dayOffset;
      this.hourOffset = hourOffset;
      this.minuteOffset = minuteOffset;
		this.interval = interval;
	}

	/**
	 * Gets a Calendar object representing the start of the next period for this Periodicity object.
	 * 
	 * @return a Calendar object representing the start of the next period for this Periodicity object.
	 */
	public Calendar getStartOfNextPeriod()
	{
		final Calendar currentTime = new GregorianCalendar();
		final Calendar startOfNextPeriod = new GregorianCalendar();
      
      if( this.type != INTERVAL )
      {
         startOfNextPeriod.set(Calendar.SECOND, 0);
         startOfNextPeriod.set(Calendar.MILLISECOND, 0);
      }
      
		switch(this.type)
		{
         case INTERVAL:
         {
            startOfNextPeriod.add(Calendar.MILLISECOND, this.interval);
            break;
         }
			case HOURLY:
			{
				startOfNextPeriod.set(Calendar.MINUTE, this.minuteOffset);
				
				if( startOfNextPeriod.getTimeInMillis() <= currentTime.getTimeInMillis() )
				{
					startOfNextPeriod.add(Calendar.HOUR_OF_DAY, 1);
				}
				
				break;
			}
			case DAILY:
			{
				startOfNextPeriod.set(Calendar.HOUR_OF_DAY, this.hourOffset);
				startOfNextPeriod.set(Calendar.MINUTE, this.minuteOffset);
				
            if( startOfNextPeriod.getTimeInMillis() <= currentTime.getTimeInMillis() )
				{
					startOfNextPeriod.add(Calendar.DATE, 1);
				}
				
				break;
			}
			case WEEKLY:
			{
				startOfNextPeriod.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
				startOfNextPeriod.add(Calendar.DAY_OF_WEEK, this.dayOffset);
				startOfNextPeriod.set(Calendar.HOUR_OF_DAY, this.hourOffset);
				startOfNextPeriod.set(Calendar.MINUTE, this.minuteOffset);
				
            if( startOfNextPeriod.getTimeInMillis() <= currentTime.getTimeInMillis() )
				{
					startOfNextPeriod.add(Calendar.WEEK_OF_YEAR, 1);
				}
				
				break;
			}
			case MONTHLY:
			{
				startOfNextPeriod.set(Calendar.DAY_OF_MONTH, this.dayOffset);
				startOfNextPeriod.set(Calendar.HOUR_OF_DAY, this.hourOffset);
				startOfNextPeriod.set(Calendar.MINUTE, this.minuteOffset);
				
            if( startOfNextPeriod.getTimeInMillis() <= currentTime.getTimeInMillis() )
				{
					startOfNextPeriod.add(Calendar.MONTH, 1);
				}
				
				break;
			}
			default:
			{
				throw new RuntimeException("Error - invalid periodicity type: " + this.type +"!");
			}
		}
		
		return startOfNextPeriod;
	}
	
	/**
	 * Gets a string representation of this Periodicity.
	 * 
	 * @return a string representation of this Periodicity.
	 */
	public String toString()
	{
      return PeriodicityTypeNames[this.type] + ", dayOffset: " + dayOffset + ", hourOffset: " + hourOffset + ", minuteOffset: " + minuteOffset + ", interval: " + interval;
	}
}
