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
package com.teletalk.jserver.log;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.property.DateProperty;
import com.teletalk.jserver.property.EnumProperty;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.property.StringProperty;

/**
 * The class PeriodicityAppenderComponent serves as an abstract baseclass for all AppenderComponents that 
 * require the ability to periodically change logs.<br>
 * <br>
 * This class replaces the 1.X class PeriodicityLogger.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0
 */
public abstract class PeriodicityAppenderComponent extends AppenderComponent
{
   /** Periodicity constant for daily logchange.  */
   public static final int DAY_LOG_NO_CYCLICITY = 0; //"Daily (no cyclicity)";
   /** Periodicity constant for daily logchange and a cycle of a week. */
   public static final int DAY_LOG_WEEK_CYCLICITY = 1; //"Daily (weekly cyclicity)";
   /** Periodicity constant for daily logchange and a cycle of a month.  */
   public static final int DAY_LOG_MONTH_CYCLICITY = 2; //"Daily (monthly cyclicity)";
   
   /** Periodicity constant for weekly logchange.  */
   public static final int WEEK_LOG_NO_CYCLICITY = 3; //"Weekly (no cyclicity)";
   /** Periodicity constant for weekly logchange and a cycle of a month.  */
   public static final int WEEK_LOG_MONTH_CYCLICITY = 4; //"Weekly (monthly cyclicity)";
   /** Periodicity constant for weekly logchange and a cycle of a year.  */
   public static final int WEEK_LOG_YEAR_CYCLICITY = 5; //"Weekly (yearly cyclicity)";
   
   /** Periodicity constant for monthly logchange.  */
   public static final int MONTH_LOG_NO_CYCLICITY = 6; //"Monthly (no cyclicity)";
   /** Periodicity constant for monthly logchange and a cycle of a year.  */
   public static final int MONTH_LOG_YEAR_CYCLICITY = 7; //"Monthly (yearly cyclicity)";
   
   /** Array containing the names for the different periodicities. */
   public static final String[] periodicityValues = { "Daily (no cyclicity)", "Daily (weekly cyclicity)", "Daily (monthly cyclicity)", 
                                                                                              "Weekly (no cyclicity)", "Weekly (monthly cyclicity)", "Weekly (yearly cyclicity)", 
                                                                                              "Monthly (no cyclicity)", "Monthly (yearly cyclicity)"};
   
   /**   EnumProperty for the periodicity. */
   protected EnumProperty periodicity;
   
   /**   Time for the last logchange. */
   protected DateProperty lastLogChange;
   
   /**   Time for the next logchange. */
   protected DateProperty nextLogChange;
   
   /** Name of the current log. */
   protected StringProperty currentLog;
   
   /**   Prefix for logs. Default = "". */
   protected StringProperty logPrefix;
   
   /**   Suffix for logs. Default = "". */
   protected StringProperty logSuffix;
   
   //private boolean lastLogChangeCompletedCycle = false;
   
   /**
    * Protected constructor to create a new PeriodicityAppenderComponent.
    * 
    * @param parent the parent of this PeriodicityAppenderComponent.
    * @param name the name of this PeriodicityAppenderComponent.
    * @param logNamePrefix prefix for logs.
    * @param logNameSuffix suffix for logs.
    * @param periodicity the default periodicity of this PeriodicityAppenderComponent.
    * 
    * @see LogManager
    */
   protected PeriodicityAppenderComponent(SubComponent parent, String name, String logNamePrefix, String logNameSuffix, int periodicity)
   {
      super(parent, name);
      
      if( logNamePrefix == null )
      {
         JServer jserver = JServer.getJServer();
         String jServerName = name;
         if( jserver != null ) jServerName = jserver.getName();
         
         logNamePrefix =jServerName + "_";
      }
   
      if( (periodicity < DAY_LOG_NO_CYCLICITY) || (periodicity > MONTH_LOG_YEAR_CYCLICITY) )
      {
         logWarning("Invalid periodicity: " + periodicity + ". Setting to default.");  
         periodicity = DAY_LOG_NO_CYCLICITY;
      }
      
      this.periodicity = new EnumProperty(this, "periodicity", periodicity, periodicityValues, Property.MODIFIABLE_OWNER_RESTART);
      this.periodicity.setDescription("The periodicity and cyclicity at which the current log will be exchanged.");

      lastLogChange = new DateProperty(this, "lastLogChange", new Date());
      lastLogChange.setDescription("The time of the last logchange.");
      nextLogChange = new DateProperty(this, "nextLogChange", new Date());
      nextLogChange.setDescription("The time of the next logchange.");
      
      currentLog = new StringProperty(this, "currentLog", "");
      currentLog.setDescription("The current log.");
      
      logPrefix = new StringProperty(this, "logPrefix", logNamePrefix , Property.MODIFIABLE_OWNER_RESTART);
      logPrefix.setDescription("The prefix of logs generated by this logger.");
      logSuffix = new StringProperty(this, "logSuffix", logNameSuffix , Property.MODIFIABLE_OWNER_RESTART);
      logSuffix.setDescription("The suffix of logs generated by this logger.");
      
      calculateNextLogChange();
      
      addProperty(this.periodicity);
      addProperty(this.lastLogChange);
      addProperty(this.nextLogChange);
      addProperty(this.currentLog);
      addProperty(this.logPrefix);
      addProperty(this.logSuffix);
   }
   
   /**
    * Protected constructor to create a new PeriodicityAppenderComponent.
    * 
    * @param parent the parent of this PeriodicityAppenderComponent.
    * @param name the name of this PeriodicityAppenderComponent.
    * @param logNamePrefix prefix for logs.
    * @param logNameSuffix suffix for logs.
    * 
    * @see LogManager
    */
   protected PeriodicityAppenderComponent(SubComponent parent, String name, String logNamePrefix, String logNameSuffix)
   {
      this(parent, name, logNamePrefix, logNameSuffix, DAY_LOG_NO_CYCLICITY);
   }
   
   /**
    * Protected constructor to create a new PeriodicityAppenderComponent.
    * 
    * @param parent the parent of this PeriodicityAppenderComponent.
    * @param name the name of this PeriodicityAppenderComponent.
    * 
    * @see LogManager
    */
   protected PeriodicityAppenderComponent(SubComponent parent, String name)
   {
      this(parent, name, null, "", DAY_LOG_NO_CYCLICITY);
   }
   
   /**
    * Enables this PeriodicityAppenderComponent.
    */
   public void doInitialize()
   {
      super.doInitialize();
      
      // Attempt to get old property "log prefix"
      this.initFromConfiguredProperty(this.logPrefix, "log prefix", false, true);
      // Attempt to get old property "log prefix"
      this.initFromConfiguredProperty(this.logSuffix, "log suffix", false, true);      
      
      changeLog();
   }

   /**
    * Validates a modification of a property's value. 
    *  
    * @param property The property to be validated.
    * 
    * @return boolean value indicating if the property passed (true) validation or not (false).
    */
   public boolean validatePropertyModification(Property property)
   {
      if(property == periodicity)
      {
         if( (periodicity.intValue() < DAY_LOG_NO_CYCLICITY) || (periodicity.intValue() > MONTH_LOG_YEAR_CYCLICITY) ) return false;
         else return true;
      }
      else return super.validatePropertyModification(property);
   }
   
   /**
    * Convenience method for setting log prefix.
    * 
    * @param prefix the log prefix.
    */
   public final void setLogPrefix(String prefix)
   {
      logPrefix.setValue(prefix);
   }
   
   /**
    * Convenience method for getting log prefix.
    * 
    * @return the log prefix.
    */
   public final String getLogPrefix()
   {
      return logPrefix.getValueAsString();
   }
   
   /**
    * Convenience method for setting log suffix.
    * 
    * @param suffix the log suffix.
    */
   public final void setLogSuffix(String suffix)
   {
      logSuffix.setValue(suffix);
   }
   
   /**
    * Convenience method for getting log suffix.
    * 
    * @return the log suffix.
    */
   public final String getLogSuffix()
   {
      return logSuffix.getValueAsString();
   }
   
   /**
    * Gets the current logname.
    * 
    * @return current logname.
    */
   public final String getCurrentLog()
   {
      return currentLog.getValueAsString();
   }

   /**
    * Gets the date for the last logchange.
    * 
    * @return date for the last logchange.
    */
   public final Date getLastLogChange()
   {
      return lastLogChange.dateValue();   
   }
   
   /**
    * Gets the date for the next logchange.
    * 
    * @return date for the next logchange.
    */
   public final Date getNextLogChange()
   {
      return nextLogChange.dateValue();   
   }
   
   /**
    * Checkes if it is time to change log.
    * 
    * @return true if it is time to change log, otherwise false.
    */
   public final boolean checkNextLogChange()
   {
      if(System.currentTimeMillis() > nextLogChange.dateValueAsMilliseconds())
      {
         changeLog();
         return true;
      }
      else return false;
   }
   
   /**
    * Changes current log.
    */
   protected final void changeLog()
   {
      lastLogChange.setValue(new Date());
      calculateNextLogChange();
      calculateLogName(lastLogChange.dateValue());
   }
   
   /**
    * Calculates the time for next logchange.
    */
   public final void calculateNextLogChange()
   {
      GregorianCalendar nextLogChangeDate = null;
      GregorianCalendar lastLogChangeDate = new GregorianCalendar();
      lastLogChangeDate.setTime(lastLogChange.dateValue());
      
      if( (periodicity.intValue() < DAY_LOG_NO_CYCLICITY) || (periodicity.intValue() > MONTH_LOG_YEAR_CYCLICITY) )
      {
         logWarning("Invalid periodicity: " + periodicity + ". Setting to default.");  
         periodicity.setValue(DAY_LOG_NO_CYCLICITY);
      }
      
      int currentPeriodicity = periodicity.intValue();
      
      if( (currentPeriodicity >= DAY_LOG_NO_CYCLICITY) && (currentPeriodicity <= DAY_LOG_MONTH_CYCLICITY) )
      {
         nextLogChangeDate = new GregorianCalendar(lastLogChangeDate.get(Calendar.YEAR), lastLogChangeDate.get(Calendar.MONTH), lastLogChangeDate.get(Calendar.DAY_OF_MONTH));
         nextLogChangeDate.add(Calendar.DAY_OF_MONTH, 1);
         nextLogChangeDate.add(Calendar.MILLISECOND, -1);
      }
      else if( (currentPeriodicity >= WEEK_LOG_NO_CYCLICITY) && (currentPeriodicity <= WEEK_LOG_YEAR_CYCLICITY) )
      {
         nextLogChangeDate = new GregorianCalendar(lastLogChangeDate.get(Calendar.YEAR), lastLogChangeDate.get(Calendar.MONTH), lastLogChangeDate.get(Calendar.DAY_OF_MONTH));
         nextLogChangeDate.add(Calendar.DAY_OF_MONTH, -(nextLogChangeDate.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY) + 1);
         nextLogChangeDate.add(Calendar.WEEK_OF_YEAR, 1);
         nextLogChangeDate.add(Calendar.MILLISECOND, -1);
      }
      else if( (currentPeriodicity == MONTH_LOG_NO_CYCLICITY) || (currentPeriodicity == MONTH_LOG_YEAR_CYCLICITY) )
      {
         nextLogChangeDate = new GregorianCalendar(lastLogChangeDate.get(Calendar.YEAR), lastLogChangeDate.get(Calendar.MONTH), 1);//lastLogChangeDate.get(Calendar.DAY_OF_MONTH));
         //nextLogChangeDate.add(Calendar.DAY_OF_MONTH, -(nextLogChangeDate.get(Calendar.DAY_OF_WEEK) - Calendar.SUNDAY) + 1);
         nextLogChangeDate.add(Calendar.MONTH, 1);
         nextLogChangeDate.add(Calendar.MILLISECOND, -1);
      }

      nextLogChange.setValue(nextLogChangeDate.getTime());
   }
   
   /**
    * Formats the date name fragment of the log.
    * 
    * @since 2.1.2 (20060224)
    */
   public final String calculateLogDateFormatted(final Date date)
   {
      SimpleDateFormat sdf = (SimpleDateFormat)DateFormat.getDateTimeInstance();
      
      if(periodicity.intValue() == DAY_LOG_NO_CYCLICITY) sdf.applyPattern("yyyyMMdd");
      else if(periodicity.intValue() == DAY_LOG_WEEK_CYCLICITY) sdf.applyPattern("EEEE");
      else if(periodicity.intValue() == DAY_LOG_MONTH_CYCLICITY) sdf.applyPattern("'day'dd");
      else if(periodicity.intValue() == WEEK_LOG_NO_CYCLICITY) sdf.applyPattern("'week'ww'@'yyyy");
      else if(periodicity.intValue() == WEEK_LOG_MONTH_CYCLICITY) sdf.applyPattern("'week_in_month'WW");
      else if(periodicity.intValue() == WEEK_LOG_YEAR_CYCLICITY) sdf.applyPattern("'week'ww");
      else if(periodicity.intValue() == MONTH_LOG_NO_CYCLICITY) sdf.applyPattern("MMMM'@'yyyy");
      else if(periodicity.intValue() == MONTH_LOG_YEAR_CYCLICITY) sdf.applyPattern("MMMM");
      else
      {
         logWarning("Unknown periodicity: " + periodicity.intValue() + ". Restoring to default."); 
         sdf.applyPattern("yyyyMMdd");
         periodicity.setValue(DAY_LOG_NO_CYCLICITY);
      }
      
      return sdf.format(date);
   }
   
   /**
    * Calculates a logname based on current date and periodicity.
    * 
    * @return a log name.
    */
   public final String calculateLogName(final Date date)
   {
      String logName = getLogPrefix() + this.calculateLogDateFormatted(date) + getLogSuffix();
      currentLog.setValue(logName);
      
      return logName;
   }
   
   /**
    * Sets the periodicity and calculates next log change. If the specified periodicity it invalid 
    * this method returns <code>false</code>, otherwise <code>true</code>.
    * 
    * @param periodicity the new periodicity value.
    */
   public final boolean setPeriodicity(int periodicity)
   {
      if(this.periodicity.setValue(periodicity))
      {
         calculateNextLogChange();
         return true;
      }
      else return false;
   }
   
   /**
    * Checks if the current periodicity is of a cyclic type.
    * 
    * @since 2.1.2 (20060307)
    */
   public boolean isCyclicPeriodicity()
   {
      int periodicityValue = this.periodicity.intValue();
      if( (periodicityValue == DAY_LOG_NO_CYCLICITY) || 
           (periodicityValue == WEEK_LOG_NO_CYCLICITY) || 
           (periodicityValue == MONTH_LOG_NO_CYCLICITY))
      {
         return false;
      }
      else return true;
   }
   
   /**
    * Gets the current periodicity time span, i.e. time in milliseconds between two log changes.
    * 
    * @since 2.1.2 (20060307)
    */
   public long getPeriodicityTimeSpan()
   {
      GregorianCalendar nextLogChangeCalendar = new GregorianCalendar();
      nextLogChangeCalendar.setTime(nextLogChange.dateValue());
      GregorianCalendar periodBeginCalendar = new GregorianCalendar();
      periodBeginCalendar.setTime(nextLogChangeCalendar.getTime());
      
      int periodicityValue = this.periodicity.intValue();
      if( (periodicityValue == DAY_LOG_NO_CYCLICITY) || 
           (periodicityValue == DAY_LOG_WEEK_CYCLICITY) || 
           (periodicityValue == DAY_LOG_MONTH_CYCLICITY))
      {
         periodBeginCalendar.add(Calendar.DATE, -1);
      }
      else if( (periodicityValue == WEEK_LOG_NO_CYCLICITY) || 
            (periodicityValue == WEEK_LOG_MONTH_CYCLICITY) || 
            (periodicityValue == WEEK_LOG_YEAR_CYCLICITY))
      {
         periodBeginCalendar.add(Calendar.WEEK_OF_YEAR, -1);
      }
      else // Month
      {
         periodBeginCalendar.add(Calendar.MONTH, -1);
      }
      
      return nextLogChangeCalendar.getTimeInMillis() - periodBeginCalendar.getTimeInMillis();
   }
}
