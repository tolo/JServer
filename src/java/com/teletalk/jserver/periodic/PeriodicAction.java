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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.property.BooleanProperty;
import com.teletalk.jserver.property.EnumProperty;
import com.teletalk.jserver.property.NumberProperty;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.property.StringProperty;

/**
 * Abstract base class of all periodic actions.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.2
 */
public abstract class PeriodicAction extends SubComponent
{
   /** The minimum allowed value of the interval propery (100 ms by default). @since 2.0 (20041022) */
   public static long MIN_INTERVAL = 100; 
   
   
	/** String describing the status "idle". */
	public static final String STATUS_IDLE = "Idle";
	
	/** String describing the status "executing". */
	public static final String STATUS_EXECUTING = "Executing";
   
   
   /**   Constant represeting interval periodicity. */
   public static final int PERIODICITY_INTERVAL    = 0;
   /**   Constant represeting hourly periodicity. */
   public static final int PERIODICITY_HOURLY      = 1;
   /**   Constant represeting daily periodicity. */
   public static final int PERIODICITY_DAILY          = 2; 
   /**   Constant represeting weekly periodicity. */
   public static final int PERIODICITY_WEEKLY        = 3;   
   /**   Constant represeting monthly periodicity. */
   public static final int PERIODICITY_MONTHLY    = 4;
   
   /**   String array containing readable names for the periodicity constants (constant can be used as index).  */
   public static final String[] PeriodicityTypeNames = {"INTERVAL", "HOURLY", "DAILY", "WEEKLY", "MONTHLY"};
   	
   
   /** Enumeration property for the periodicity type. */
   protected final EnumProperty periodicityType;
   
   /** Number property for the periodicity day offset. */
   protected final NumberProperty periodicityDayOffset;
   
   /** Number property for the periodicity hour offset. */
   protected final NumberProperty periodicityHourOffset;
   
   /** Number property for the periodicity minute offset. */
   protected final NumberProperty periodicityMinuteOffset;
   
   /** Number property for the periodicity interval in milliseconds (used for periodcity type {@link #PERIODICITY_INTERVAL}). */
   protected final NumberProperty interval;
   
   /** A string containing an action status description.  */
   protected final StringProperty actionStatus;
   
   /** String property for the last execution result (not persistent, display only). This property can be set through a call to {@link #setLastExecutionResult(String)}. */
   protected final StringProperty lastExecutionResult;
   
   /** String property for the time of the next execution (not persistent, display only). */
   protected final StringProperty timeOfNextExecutionProperty;
   
   /** String property for the time of the last execution (not persistent, display only). */
	protected final StringProperty lastExecutionStartTimeProperty;
   
   /** Boolean property used to force the execution of this action. */
   protected final BooleanProperty forcedExecution;


   /**   Reference to the parent PeriodicActionManager.  */
   protected PeriodicActionManager periodicActionManager;
	
	/**	The periodicity at which this action will execute. */
	protected Periodicity periodicity;
	
	/** The next time this action will execute. */
	private Calendar timeOfNextExecution;
	
	long executionStartTime = -1;
   
   private boolean executing = false;
   
   private boolean criticalErrorDispatched = false;

	
	/**
	 * Creates a new PeriodicAction object.
	 * 
	 * @param name the name of this PeriodicAction. 
	 */
	public PeriodicAction(String name)
	{
      super(name);
      
		this.periodicActionManager = null;
      this.periodicity = new Periodicity();
      
      this.periodicityType = new EnumProperty(this, "periodicityType", Periodicity.HOURLY, Periodicity.PeriodicityTypeNames, Property.MODIFIABLE_NO_RESTART); //.MODIFIABLE_OWNER_RESTART);
      this.periodicityType.setDescription("Enumeration property for the periodicity type.");
      super.addProperty(this.periodicityType);
      
      this.periodicityDayOffset = new NumberProperty(this, "periodicityDayOffset", 0, Property.MODIFIABLE_NO_RESTART); //.MODIFIABLE_OWNER_RESTART);
      this.periodicityDayOffset.setDescription("The periodicity day offset.");
      super.addProperty(this.periodicityDayOffset);
      
      this.periodicityHourOffset = new NumberProperty(this, "periodicityHourOffset", 0, Property.MODIFIABLE_NO_RESTART); //.MODIFIABLE_OWNER_RESTART);
      this.periodicityHourOffset.setDescription("The periodicity hour offset.");
      super.addProperty(this.periodicityHourOffset);
            
      this.periodicityMinuteOffset = new NumberProperty(this, "periodicityMinuteOffset", 0, Property.MODIFIABLE_NO_RESTART); //.MODIFIABLE_OWNER_RESTART);
      this.periodicityMinuteOffset.setDescription("The periodicity minute offset.");
      super.addProperty(this.periodicityMinuteOffset);
      
      this.interval = new NumberProperty(this, "interval", 60000, Property.MODIFIABLE_NO_RESTART); //.MODIFIABLE_OWNER_RESTART);
      this.interval.setDescription("The interval in milliseconds when periodicityType is \"INTERVAL\".");
      super.addProperty(this.interval);
      
      this.actionStatus = new StringProperty(this, "actionStatus", STATUS_IDLE, StringProperty.NOT_MODIFIABLE, false);
      this.actionStatus.setDescription("A string containing an action status description.");
      super.addProperty(this.actionStatus);
      
      this.lastExecutionResult = new StringProperty(this, "lastExecutionResult", "", StringProperty.NOT_MODIFIABLE, false);
      this.lastExecutionResult.setDescription("A string representing the last execution result.");
      super.addProperty(this.lastExecutionResult);
      
      this.lastExecutionStartTimeProperty = new StringProperty(this, "lastExecutionStartTime", "", StringProperty.NOT_MODIFIABLE, false);
      this.lastExecutionStartTimeProperty.setDescription("The time of the last execution.");
		super.addProperty(this.lastExecutionStartTimeProperty);
      
      this.timeOfNextExecutionProperty = new StringProperty(this, "timeOfNextExecution", "", StringProperty.NOT_MODIFIABLE, false);
      this.timeOfNextExecutionProperty.setDescription("The time of the next execution.");
      super.addProperty(this.timeOfNextExecutionProperty);
      
      this.forcedExecution = new BooleanProperty(this, "forcedExecution", false, BooleanProperty.MODIFIABLE_NO_RESTART, false);
      this.forcedExecution.setDescription("Flag used to force the execution of this action.");
      super.addProperty(this.forcedExecution);
	}
   
   /**
    * Get the forced execution value.
    * 
    * @since 2.1 (20050825)
    */
   public boolean isForcedExecution()
   {
      return forcedExecution.booleanValue();
   }
   
   /**
    * Set the forced execution value.
    * 
    * @since 2.1 (20050825)
    */
   public void setForcedExecution(boolean forcedExecution)
   {
      this.forcedExecution.setValue(forcedExecution);
   }
   
   /**
    * Gets the interval setting for periodic action execution (relevant only if periodicity type is PERIODICITY_INTERVAL).
    * 
    * @since 2.1 (20050825)
    */
   public long getInterval()
   {
      return interval.longValue();
   }
   
   /**
    * Sets the interval setting for periodic action execution (relevant only if periodicity type is PERIODICITY_INTERVAL).
    * 
    * @since 2.1 (20050825)
    */
   public void setInterval(long interval)
   {
      this.interval.setValue(interval);
   }
   
   /**
    * Gets the day offset for periodic action execution.
    * 
    * @since 2.1 (20050825)
    */
   public int getPeriodicityDayOffset()
   {
      return periodicityDayOffset.intValue();
   }
   
   /**
    * Sets the day offset for periodic action execution.
    * 
    * @since 2.1 (20050825)
    */
   public void setPeriodicityDayOffset(int periodicityDayOffset)
   {
      this.periodicityDayOffset.setValue(periodicityDayOffset);
   }
   
   /**
    * Gets the hour offset for periodic action execution.
    * 
    * @since 2.1 (20050825)
    */
   public int getPeriodicityHourOffset()
   {
      return periodicityHourOffset.intValue();
   }
   
   /**
    * Sets the hour offset for periodic action execution.
    * 
    * @since 2.1 (20050825)
    */
   public void setPeriodicityHourOffset(int periodicityHourOffset)
   {
      this.periodicityHourOffset.setValue(periodicityHourOffset);
   }
   
   /**
    * Gets the minute offset for periodic action execution.
    * 
    * @since 2.1 (20050825)
    */
   public int getPeriodicityMinuteOffset()
   {
      return periodicityMinuteOffset.intValue();
   }
   
   /**
    * Sets the minute offset for periodic action execution.
    * 
    * @since 2.1 (20050825)
    */
   public void setPeriodicityMinuteOffset(int periodicityMinuteOffset)
   {
      this.periodicityMinuteOffset.setValue(periodicityMinuteOffset);
   }
   
   /**
    * Gets the periodicity type.
    * 
    * @since 2.1 (20050825)
    */
   public int getPeriodicityType()
   {
      return periodicityType.intValue();
   }
   
   /**
    * Sets the periodicity type.
    * 
    * @since 2.1 (20050825)
    */
   public void setPeriodicityType(int periodicityType)
   {
      this.periodicityType.setValue(periodicityType);
   }
   
   /**
    * Gets the periodicity object.
    * 
    * @since 2.1 (20050825)
    */
   public Periodicity getPeriodicity()
   {
      return periodicity;
   }
   
   /**
    * Checks if an critical error has been dispatched for this action. This flag will be reset the next time the action executes.
    */
   boolean isCriticalErrorDispatched()
   {
      return criticalErrorDispatched;
   }
   
   /**
    * Sets the flag indicating if an critical error has been dispatched for this action. This flag will be reset the next time the action executes.
    */
   void setCriticalErrorDispatched(boolean criticalErrorDispatched)
   {
      this.criticalErrorDispatched = criticalErrorDispatched;
   }
   
   /**
    * Checks if this action is currently executing.
    * 
    * @since 2.1 (20050524)
    */
	public boolean isExecuting()
   {
      return this.executing;
   }
   
   /**
    * Gets a string representaion of this PeriodicTime.
    * 
    * @return a string representaion of this PeriodicTime.
    */
   public String getDescription()
   {
      return this.name + " (" + this.periodicity.toString() + ")";
   }
	
	/**
	 * Gets a description on this action.
	 * 
	 * @return a description on this action.
	 */
	public String getLongDescription()
	{
		String lineSep = System.getProperty("line.separator");
		if( lineSep == null ) lineSep = "\r\n";
		
		return "Periodicity: " + this.periodicity.toString() + lineSep + "Next execution: " + SimpleDateFormat.getDateTimeInstance().format( this.timeOfNextExecution.getTime()) + lineSep;
	}
	
	/**
	 * Gets a string describing the status of this action.
	 * 
	 * @return a string describing the status of this action.
	 */
	public String getActionStatus()
	{
		return this.actionStatus.stringValue();
	}
	
	/**
	 * Sets the string describing the status of this action.
	 * 
	 * @param actionStatus a string describing the status of this action.
	 */
	public void setActionStatus(String actionStatus)
	{
		this.actionStatus.setValue(actionStatus);
	}
	
	/**
	 * Gets a message describing the result of the last execution of this action.
	 * 
	 * @return a message describing the result of the last execution of this action.
	 */
	public String getLastExecutionResult()
	{
		return this.lastExecutionResult.stringValue();
	}
	
	/**
	 * Sets a message describing the result of the last execution of this action.
	 * 
	 * @param executionResultMessage a message describing the result of the last execution of this action.
	 */
	public void setLastExecutionResult(String executionResultMessage)
	{
		this.lastExecutionResult.setValue(executionResultMessage);
	}
	
	/**
	 * Gets millisecond value (since January 1, 1970, 00:00:00 GMT) indicating the next execution time of this action.
	 * 
	 * @return a millisecond value indicating the next execution time of this action.
	 */
	public long getTimeOfNextExecution()
	{
		return this.timeOfNextExecution.getTime().getTime();
	}
	
	/**
	 * Gets the execution start time of this action.
	 */
	public long getExecutionStartTime()
	{
		return this.executionStartTime;
	}
	
	/**
	 * Calculates the next execution time of this action, based on its periodicity.
	 */
	public void calculateTimeOfNextExecution()
	{
      // If action hasn't executed yet executionStartTime hasn't been set yet...  
      if( this.executionStartTime < 0 ) this.executionStartTime = System.currentTimeMillis();
		this.timeOfNextExecution = periodicity.getStartOfNextPeriod();
      
      if( this.periodicityType.intValue() == Periodicity.INTERVAL )
      {
			// Reevaluate timeOfNextExecution to set to to be exactly the interval value in milliseconds later than executionStartTime
         this.timeOfNextExecution.setTimeInMillis(this.timeOfNextExecution.getTime().getTime() - (System.currentTimeMillis() - this.executionStartTime));
      }
      
      SimpleDateFormat dateFormat = (SimpleDateFormat)DateFormat.getDateTimeInstance();
      dateFormat.applyPattern("yyyy-MM-dd HH:mm:ss");
      this.timeOfNextExecutionProperty.setValue(dateFormat.format(this.timeOfNextExecution.getTime()));
	}
	
	/**
	 * Executes this periodic action.
	 * 
	 * @return <code>true</code> if the action execution was successful, otherwise <code>false</code>.
	 */
	public abstract boolean execute() throws Exception;
   
   /**
    * Internal method to prepare the action for execution.
    */
   void actionPreExecuteInternal()
   {
      this.executionStartTime = System.currentTimeMillis();
      
      SimpleDateFormat dateFormat = (SimpleDateFormat)DateFormat.getDateTimeInstance();
      dateFormat.applyPattern("yyyy-MM-dd HH:mm:ss");
      this.lastExecutionStartTimeProperty.setValue(dateFormat.format(new Date(this.executionStartTime)));
      
      // Reset timeOfNextExecutionProperty while executing
      this.timeOfNextExecutionProperty.setValue("");
      
      
      this.setActionStatus(PeriodicAction.STATUS_EXECUTING);
      this.executing = true;
      this.criticalErrorDispatched = false;
      
      this.actionPreExecute();
   }
   
   /**
    * Prepares the action for execution.
    */
   protected void actionPreExecute()
   {
   }
   
   /**
    * Internal method called to notify the action that it has been placed in an idle state.
    */
   void actionIdleInternal()
   {
      this.forcedExecution.setValue(false);
      
      this.executing = false;
      
      this.actionIdle();
   }
   
   /**
    * Called to notify the action that it has been placed in an idle state. This method is always called when an 
    * action has completed execution, even if an error occurs.
    */
   protected void actionIdle()
   {
   }
   
   /**
    * Called to notify the action that a fatal error during execution occurred.
    */
   protected void actionFatalError(final Throwable t)
   {
      this.logError("Fatal error occurred during execution!", t);
   }
   
   
   /**
    * Removes this action from the PeriodicaActionManager, without attempting to shutting it down.
    */
   protected void removePeriodicAction()
   {
      if( this.periodicActionManager != null )
      {
         this.periodicActionManager.removePeriodicActionInternal(this, false);
      }
   }
   
   /**
    * Adds this action from the PeriodicaActionManager, without attempting to engage it.
    */
   protected void addPeriodicAction()
   {
      if( this.periodicActionManager != null )
      {
         this.periodicActionManager.addPeriodicActionInternal(this, false);
      }
   }
   
   /**
    * Disables this PeriodicAction.
    */
   protected void doShutDown()
   {
      super.doShutDown();
      
      this.removePeriodicAction();
   }

   /**
    * Enables this PeriodicAction.
    */
   protected void doInitialize()
   {
      super.doInitialize();
      
      if( (this.periodicActionManager == null) && ((super.parent != null) && (parent instanceof PeriodicActionManager)) )
      {
         this.periodicActionManager = (PeriodicActionManager)parent;
      }
      
      this.periodicity = new Periodicity(this.periodicityType.intValue(), this.periodicityDayOffset.intValue(), 
            this.periodicityHourOffset.intValue(), this.periodicityMinuteOffset.intValue(), this.interval.intValue());
      
      this.addPeriodicAction(); // This method call will result in a call to calculateTimeOfNextExecution() (in doAddActionToQueue of PeriodicActionManager)
   }
   
   /**
    * Called when the value of a property has been modified. 
    * 
    * @param property the Property that was changed.
    */
   public void propertyModified(final Property property)
   {
      super.propertyModified(property);
      
      if( (property == this.forcedExecution) && this.forcedExecution.booleanValue() )
      {
         if( this.periodicActionManager != null )
         {
            this.periodicActionManager.executeAction(this);
         }
      }
      else if( (property == this.periodicityType) ||
                  (property == this.periodicityDayOffset) || 
                  (property == this.periodicityHourOffset) || 
                  (property == this.periodicityMinuteOffset) || 
                  (property == this.interval) ||
                  (property == this.forcedExecution) )
      {
         this.periodicity = new Periodicity(this.periodicityType.intValue(), this.periodicityDayOffset.intValue(), 
               this.periodicityHourOffset.intValue(), this.periodicityMinuteOffset.intValue(), this.interval.intValue());
         
         if( this.periodicActionManager != null )
         {
            this.periodicActionManager.actionPeriodicityModified(this);
         }
      }
   }
   
   /**
    * Validates a modification of a property's value.
    * 
    * @since (20041022)
    */
   public boolean validatePropertyModification(final Property property)
   {
      if( property == this.interval )
      {
         return this.interval.longValue() >= MIN_INTERVAL;
      }
      else return super.validatePropertyModification(property);
   }
}
