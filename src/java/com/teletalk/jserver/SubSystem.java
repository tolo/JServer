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
package com.teletalk.jserver;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.teletalk.jserver.load.LoadManager;
import com.teletalk.jserver.property.BooleanProperty;
import com.teletalk.jserver.property.DateProperty;
import com.teletalk.jserver.property.NumberProperty;
import com.teletalk.jserver.property.Property;

/**
 * The SubSystem class is an abstract baseclass for all subsystems in the server and 
 * a fundamental component in the JServer architechture. This class plays a key role
 * in adding new functionality to the server.<br>
 * <br>
 * The difference between SubSystem and its super class SubComponent is the fact that 
 * SubSystem is an active component that contains its own thread which may be used by subclasses to 
 * execute component specific logic. The thread method is the standard <code>public void run()</code> method 
 * of <code>java.lang.Thread</code> and this mehthod must be implemented by the subclass.
 * If the implementation of this method contains a loop it is recomended that 
 * its loop condition depends on the state of the <b><code>{@link #canRun}</code></b> flag.<br>
 * <br>
 * Another important difference between SubSystem and SubComponent is that all status transitions 
 * are handled asynchronously in SubSystem. This means that when for instance the method {@link SubComponent#engage()} 
 * is invoked, the status transition will be handled in a separate thread. This behaviour may however be controlled by calling  
 * {@link SubComponent#setAsynchronousStatusChanges(boolean)}.<br>
 * <br>
 * SubSystems are constantly monitored by the JServer object of the server, and if found in a state of {@link JServerConstants#ERROR} 
 * the SubSystem will be restarted by the JServer, if {@link #restartable restartable}. The number of restarts that may be performed for 
 * a SubSystem, before it is put into the state {@link JServerConstants#CRITICAL_ERROR}, may be configured through the property 
 * <code>maxRestarts</code>, accessible through the methods {@link #getMaxRestarts()}/{@link #setMaxRestarts(int)}. The interval between 
 * restart may be set throug the property <code>restartInterval</code>, accessible through the methods 
 * {@link #getRestartInterval()}/{@link #setRestartInterval(int)}.<br>
 * <br>
 * A SubSystem can be tagged as a <code>keySystem</code> (using the property with the same name), in which case a 
 * a fatal error will be logged, using the log message id {@link JServerConstants#LOG_MESSAGE_ID_CRITICAL_SUBSYSTEM_ERROR}. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 * 
 * @see JServerConstants
 */
public abstract class SubSystem extends SubComponent implements Runnable
{
	/**	 A DateProperty containing the time when this SubComponent was started (engaged).*/
	protected DateProperty startTime;
	
	/**	 A NumberProperty containing the maximum allowed restarts before critical failure. */
	protected NumberProperty maxRestarts;
	
	/**	 A NumberProperty containing the time in milliseconds between restarts. */
	protected NumberProperty restartInterval;
	
	/**	 A NumberProperty keeping track of the number of times this SubSystem has been restarted. */
	protected NumberProperty restartCounter;
		
	/**	BooleanProperty used to indicate if this is a key system or not. */
	protected BooleanProperty keySystem;
   
	
	/**	Flag indicating if the thread in this SubSystem can run. */
	protected volatile boolean canRun = false;
	
	/**	Flag indicating if this SubSystem is restartable. */
	protected boolean canRestart = true;
		
	/**	A counter keeping track of the number of times this SubSystem has undergone a successful check. */
	protected volatile int successfulCheckCounter = 0;
	
	/** The maximum time a status transition is allowed to take in this subsystem. Defaultvalue is 300 seconds.*/
	protected volatile long statusTransitionTimeout = DEFAULT_STATUS_TRANSITION_TIMEOUT;
	
	/**	The thread object. */
	protected Thread thread = null;
	
	/** The priority of the thread of this SubSystem. */
	protected int threadPriority = Thread.NORM_PRIORITY;
	
	private boolean reinitializingSubSystemDueToError = false;
	
	private boolean errorStateDetectedInCheck = false; //Flag used when detecting state ERROR in checkStatus().
	
	/**
	 * Constructs a new SubSystem.
	 * 
	 * @param name the name of the SubSystem.
	 * @param parent the parent SubSystem.
	 * @param maxRestarts specifies the default value for maximum allowed restarts before critical failure in this SubSystem.
	 * @param restartInterval specifies the default time in milliseconds between restarts used by this SubSystem.
	 * @param keySystem flag indicating if this is a key system or not.
	 */
	protected SubSystem(SubSystem parent, String name, int maxRestarts, int restartInterval, boolean keySystem)
	{
		super(parent, name);
      
      // Subsystems use asynchronous state chages by default
      super.setAsynchronousStatusChanges(true);
      
		this.parent = parent;
      		
		status.setForceMode(true);
		status.setNotificationMode(false);
		status.setValue(CREATED);
		status.setForceMode(false);
		status.setNotificationMode(true);
		
		this.keySystem = new BooleanProperty(this, "keySystem", keySystem, Property.MODIFIABLE_NO_RESTART);
		this.keySystem.setDescription("Boolean flag indicating if this subsystem is a key system. If this flag is set to " +
																		"true, a critical error will be logged if an error occurs in this subsystem and it cannot be " +
																		"restarted (status CRITICAL_ERROR).");
		this.startTime = new DateProperty(this, "startTime", new Date());
		this.startTime.setDescription("The time when this subsystem was started.");
		this.maxRestarts = new NumberProperty(this, "maxRestarts", maxRestarts, Property.MODIFIABLE_NO_RESTART);
		this.maxRestarts.setDescription("The maximum number of attempts that should be made to restart this subsystem when an error occurs. The minimum value of this property is 0.");
		this.restartInterval = new NumberProperty(this, "restartInterval", restartInterval, Property.MODIFIABLE_NO_RESTART);
		this.restartInterval.setDescription("The interval in milliseconds between restarts. The minimun value of this property is 100 ms.");
		this.restartCounter = new NumberProperty(this, "restartCounter", 0);
		this.restartCounter.setDescription("The number of times this subsystem has been restarted.");
		
		addProperty(this.keySystem);
		addProperty(this.startTime);
		addProperty(this.maxRestarts);
		addProperty(this.restartInterval);
		addProperty(this.restartCounter);
	}
   
   /**
    * Statusmethod for the status INITIALIZING. Subclasses can override this method to provide code to be executed when
    * initializing (engaging) a subsystem. <br>
    * <br>
    * The thread of this subsystem will be started after the return of this method. <br>
    * <br>
    * The appropriate way for subclass implementations to signal that an error has occurred in this method is to throw a
    * {@link StatusTransitionException}.<br>
    * <br>
    * This implementation provides logic for maintaining backwards compatability with old property names.
    * 
    * @see #engage()
    */
   protected void doInitialize()
   {
      super.doInitialize();

      // Attempt to get old property "key system"
      this.initFromConfiguredProperty(this.keySystem, "key system", false, true);
      // Attempt to get old property "max restarts"
      this.initFromConfiguredProperty(this.maxRestarts, "max restarts", false, true);
      // Attempt to get old property "restart interval(ms)"
      this.initFromConfiguredProperty(this.restartInterval, "restart interval(ms)", false, true);
   }
	
	/**
    * Constructs a new SubSystem with the keySystem flag set to a default value of <code>false</code>. *
    * 
    * @param name the name of the SubSystem.
    * @param parent the parent SubSystem.
    * @param maxRestarts specifies the default value for maximum allowed restarts before critical failure in this
    *                SubSystem.
    * @param restartInterval specifies the default time in milliseconds between restarts used by this SubSystem.
    */
	protected SubSystem(SubSystem parent, String name, int maxRestarts, int restartInterval)
	{
		this(parent, name, maxRestarts, restartInterval, false);
	}
	
	/**
	 * Constructs a new SubSystem with maxRestarts set to 5 and restartInterval to 2000.
	 * 
	 * @param name the name of the SubSystem.
	 * @param parent the parent SubSystem.
	 * @param keySystem flag indicating if this is a key system or not.
	 */
	protected SubSystem(SubSystem parent, String name, boolean keySystem)
	{
		this(parent, name, 5, 2000, keySystem);
	}
	
	/**
	 * Constructs a new SubSystem with maxRestarts set to 5 and restartInterval to 2000.
	 * The keySystem flag will be set to a default value of <code>false</code>.
	 * 
	 * @param name the name of the SubSystem.
	 * @param parent the parent SubSystem.
	 */
	protected SubSystem(SubSystem parent, String name)
	{
		this(parent, name, 5, 2000);
	}
	
	/**
	 * Constructs a new SubSystem without a paren and with maxRestarts set to 5 and restartInterval to 2000. 
	 * The keySystem flag will be set to a default value of <code>false</code>.
	 * 
	 * @param name the name of the SubSystem.
	 */
	protected SubSystem(String name)
	{
		this(null, name, 5, 2000);
	}
	
	/**
	 * Destroys this SubSystem. This method is used by JServer to destroy this SubSystem when 
	 * shutting down the server. Applications should never call this method directly, however subclasses may 
	 * override this method but should always call the superclass implementation.
	 */
	protected void destroy()
	{
		synchronized(componentLock)
		{	
         stopThread();
         
         super.destroy();
		
			this.keySystem = null;
			this.startTime = null;
			this.maxRestarts = null;
			this.restartInterval = null;
			this.restartCounter = null;
		}
	}
	
	/**
	 * Called to check if a property can be modified. Subclasses can override this
	 * method to provide more specialized behaviour. This implementation only allows modification of properties in the 
    * following cases:<br>
    * <br>
    * * When the subsystem is in state CREATED, ENABLED, DOWN or CRITICAL_ERROR.<br>
    * * If the propery is one of the internal subsystem/subcomponent properties (status, logLevel, restartCounter, startTime and errorReason).<br>
    * * If the property has a modification mode of MODIFIABLE_NO_RESTART or NOT_MODIFIABLE.<br>
    * <br>
    * Subclasses can override this method to allow property modifications under other conditions. 
	 * 
	 * @param property The property to be checked.
	 * 
	 * @return true if the property can be modified, otherwise false.
	 *
	 * @see com.teletalk.jserver.property.PropertyOwner
	 */
	public boolean propertyModificationAllowed(final Property property)
	{
      if( property == null ) return false;
      
		if( (property == super.status) || 
		      (property == super.logLevel) || 
		      (property == this.restartCounter)||
		      (property == this.startTime) ||
		      (property == this.errorReason) || 
		      (property.getModificationMode() == Property.MODIFIABLE_NO_RESTART) || 
		      (property.getModificationMode() == Property.NOT_MODIFIABLE) ) return true;
		
		long currentStatus = getStatus();
		
		return (currentStatus == CREATED) || (currentStatus == ENABLED) || (currentStatus == DOWN) || (currentStatus == CRITICAL_ERROR);
	}
	
	/**
	 * Validates a modification of a property's value. The property specified by parameter <code>property</code> will 
	 * be given the value to be validated before this method is called, and will revert to the old one if this method returns <code>false</code>.
	 * <br><br>
	 * Subclasses can override this
	 * method to provide more specialized behaviour. This implementation validates various SubSystem 
	 * properties like restart interval. For that reason it is recommended that subclasses that override this method 
	 * calls the superclass implementation in an appropriate manner.
	 * 
	 * @param property The property to be validated.
	 * 
	 * @return boolean value indicating if the property passed (true) validation or not (false).
	 * 
	 * @see com.teletalk.jserver.property.PropertyOwner
	 */
	public boolean validatePropertyModification(Property property)
	{
		if(property == maxRestarts) return maxRestarts.longValue() >= 0;
		else if(property == restartInterval) return restartInterval.longValue() >= 100;
		else return super.validatePropertyModification(property);
	}
	
	/**
	 * Returns the thread in this subsystem.
	 * 
	 * @return a Thread object.
	 */
	public final Thread getThread()
	{
		synchronized(componentLock)
		{
			return thread;
		}
	}
	
	/**
	 * Starts the thread in this subsystem.
	 */
	protected final void startThread()
	{
		synchronized(componentLock)
		{	
			if( ! (thread != null && thread.isAlive()) )
			{
				canRun = true;
				thread = new Thread(getThreadGroup(), this, getFullName());
				thread.setPriority(threadPriority);
				thread.start();
			}
		}
	}
	
	/**
	 * Stops the thread in this subsystem.
	 */
	protected final void stopThread()
	{
		Thread daThread = this.thread;
		
	   synchronized(componentLock)
		{	
			canRun = false;
			if(this.thread != null)
			{
			   this.thread.interrupt();
			   this.thread = null;
			}
		}
	   
	   // Unregister thread as load thread in case it was previously registered
	   LoadManager.unregisterLoadThread(daThread);
	}
	
	/**
	 * Returns the maximum allowed restarts before critical failure.
	 * 
	 * @return the maximum allowed restarts before critical failure.
	 */
	public final int getMaxRestarts()
	{
		return maxRestarts.getValueAsNumber().intValue();
	}
	
	/**
	 * Sets the maximum allowed restarts before critical failure.
	 * 
	 * @param maxRestarts the maximums allowed restarts.
	 */
	public final void setMaxRestarts(int maxRestarts)
	{
		this.maxRestarts.setValue(maxRestarts);
	}

	/**
	 * Returns the time in milliseconds between restarts.
	 * 
	 * @return the time in milliseconds between restarts.
	 */
	public final int getRestartInterval()
	{
		return restartInterval.getValueAsNumber().intValue();
	}
	
	/**
	 * Gets the error reason.
	 * 
	 * @return the error reason.
	 */
	public String getErrorReason()
	{
		return errorReason.stringValue();
	}
	
	/**
	 * Sets the time in milliseconds between restarts.
	 * 
	 * @param restartInterval the restart inteval in milliseconds.
	 */
	public final void setRestartInterval(int restartInterval)
	{
		this.restartInterval.setValue(restartInterval);
	}
	
	/**
	 * Gets the priority of the thread in this SubSystem.
	 * 
	 * @return the priority of the thread in this SubSystem.
	 * 
	 * @see java.lang.Thread#getPriority()
	 */
	public int getThreadPriority()
	{
		return threadPriority;
	}
	
	/**
	 * Sets the priority of the thread in this SubSystem.
	 * 
	 * @param threadPriority the new priority.
	 * 
	 * @see java.lang.Thread#setPriority(int)
	 */
	public void setThreadPriority(int threadPriority)
	{
		this.threadPriority = threadPriority;
		
		if(thread != null)
			thread.setPriority(threadPriority);
	}
	
	
	/**
	 * Checks if this SubSystem is allowed to restart.
	 * 
	 * @return boolean indicating if this SubSystem can restart.
	 */
	public final boolean restartable()
	{
      if( maxRestarts != null )
      {
   		if(getRestartCount() >= maxRestarts.intValue())
   		{
   			canRestart = false;
   		}
      }
		
		return canRestart;
	}
	
	/**
	 * Checks to see if this SubSystem is a key system or not.
	 * 
	 * @return boolean indicating if this SubSystem is a keysystem or not.
	 */
	public final boolean isKeySystem()
	{
		return keySystem.booleanValue();
	}
	
	/**
	 * Sets the key system flag. If a critical error should arise and this flag is set, a CRITICAL_ERROR log message 
	 * will be generated.
	 * 
	 * @param keySystem boolean indicating if this SubSystem should be a keysystem or not.
	 */
	public final void setKeySystem(boolean keySystem)
	{
		this.keySystem.setValue(keySystem);	
	}
	
	/**
	 * Checkes to see if this SubSystem is at the top of the system hierchy.
	 * 
	 * @return booleam indicating if this SubSystem is the topsystem.
	 */
	public final boolean isTopsystem()
	{
		return (this == JServer.getJServer());	
	}
	
	/**
	 * Returns the amount of time that has elapsed since this SubSystem was started.
	 * 
	 * @return long indicating the time in milliseconds.
	 */
	public final long getUpTime()
	{
		return (System.currentTimeMillis() - startTime.dateValueAsMilliseconds());
	}
	
	/**
	 * Returns the number of times this SubSystem has been restarted.
	 * 
	 * @return int indicating the number of restarts.
	 */
	public final int getRestartCount()
	{
		return restartCounter.intValue();	
	}
		
	/**
	 * Sets the restartCounter to the given value.
	 * 
	 * @param value the new value for restartCounter.
	 */
	public final void setRestartCount(int value)
	{
		restartCounter.setValue(value);
	}
	
	/**
	 * Increments the restartCounter.
	 */
	public final void incrementRestartCount()
	{
		restartCounter.increment();
	}
   
   /**
    * Gets the maximum state transition time.
    * 
    * @since 2.0
    */
   protected long getMaximumStatusTransitionTime()
   {
      //Calculate the definition of an maximum status transition time from the maximum of statusTransitionTimeout and the restartinterval*1.5
      return (long)Math.max(super.statusTransitionTimeout, (restartInterval.longValue() * 1.5));
   }
	
	/**
	 * Checks the status of this subsystem. Returns false if the subsystem is in an erroneous 
	 * status, otherwise true.
	 * 
	 * @return boolean - false if the status of this subsystem is ERROR or ENABLED and the 
	 * subsystem thread(if there is one) is dead. Otherwise this method returns true.
	 */
	protected final boolean checkStatus()
	{
     boolean result = true;
      
      synchronized(componentLock)
      {
         if(getStatus() == ENABLED)
         {
            if(thread == null || !thread.isAlive())
            {
               if( this.isDebugMode() ) logDebug("checkStatus failed - thread dead.");
               result = false;
            }
         }
         else if(getStatus() == ERROR)
         {
            if( errorStateDetectedInCheck )
            {
               result = false;
            }
            else
            {
               errorStateDetectedInCheck = true;
               result = true;
            }
         }
      }
            
      if( result )
      {
         result = super.checkStatus();
         
         if(result) successfulCheckCounter++;
                     
         //Reset the restart counter after two successful checks
         if((getRestartCount() > 0) && (successfulCheckCounter > 1) && result)
         {
            restartCounter.setValue(0);
            successfulCheckCounter = 0;
         }
      }
      
      return result;
	}
   
	/**
	 * Method to check if this SubSystem is currently reinitializing due to an error.
	 * 
	 * @return true if this SubSystem is currently reinitializing due to an error, false if the reinitialization was initiated 
	 * manually (possibly through the administration tool).
	 */
	public boolean isReinitializingDueToError()
	{
		return reinitializingSubSystemDueToError;	
	}
   
   /**
    * Method for subclass handling of status transition initialization.
    * 
    * @since 2.0
    */
   protected void doInitiateStatusTransition(final StatusTransitionTask statusTransitionTask)
   {
      int statusTransitionType = statusTransitionTask.getStatusTransitionType();
      
      switch(statusTransitionType)
      {
         case JServerConstants.STATUS_TRANSITION_TYPE_ENGAGE: 
         case JServerConstants.STATUS_TRANSITION_TYPE_REINITIALIZE:
         {
            errorStateDetectedInCheck = false; //Reset the private flag inicating if ERROR state was detected in checkStatus()
            
            if(this.getStatus() == CRITICAL_ERROR) //If current status is CRITICAL_ERROR...
            {
               restartCounter.setValue(0); //...reset the restart counter
            }
            
            break;
         }
         case JServerConstants.STATUS_TRANSITION_TYPE_ERROR:
         case JServerConstants.STATUS_TRANSITION_TYPE_CRITICAL_ERROR:
         {
            synchronized(super.statusTransitionExecutionMonitor)
            {
               if( super.currentStatusTransitionExecutionThread  == Thread.currentThread() )
               {
                  throw new StatusTransitionException(this.errorReason.stringValue());
               }
            }
            break;
         }
      }
   }
   
   /**
    * Subclass method for handling a status transition.
    * 
    * @since 2.0
    */
   protected boolean doExecuteStatusTransition(final StatusTransitionTask statusTransitionTask)
   {
      int statusTransitionType = statusTransitionTask.getStatusTransitionType();
      
      switch(statusTransitionType)
      {
         case STATUS_TRANSITION_TYPE_ENGAGE:
         {
            // Reset flags/settings
            this.errorStateDetectedInCheck = false;
            this.canRestart = true;
            this.successfulCheckCounter = 0;
            
            if( super.doExecuteStatusTransition(statusTransitionTask) )
            {
               this.startTime.setValue(new Date());
               this.startThread();  // Start the subsystem thread
               
               return true;            
            }
         }
         case STATUS_TRANSITION_TYPE_REINITIALIZE:
         {
            int oldStatus = this.getStatus();
            
            this.setStatus(REINITIALIZING);
            
            try
            {
               this.errorStateDetectedInCheck = false;

               if( oldStatus == ERROR ) this.reinitializingSubSystemDueToError = true;
               
               this.stopThread(); //Stop the subsystem thread
               
               if( super.statusTransitionInterrupted || Thread.interrupted() ) return false;
               
               try
               {
                  this.doShutDown();
               }
               catch(Exception e)
               {
                  logError("Got exception while shutting down during reinitialization", e);
               }
               
               if( super.statusTransitionInterrupted || Thread.interrupted() ) return false;
         
               try
               {
                  long waitStart = System.currentTimeMillis();
                  long waitTime = 0;
                  long maxWait = getRestartInterval();
                  
                  while(waitTime < maxWait)
                  {
                     waitTime = maxWait - (System.currentTimeMillis() - waitStart);
                     
                     if(waitTime > 0) Thread.sleep(waitTime);
                     else break;
                  }
               }
               catch(InterruptedException e)
               {
                  if( this.statusTransitionInterrupted ) return false;
               }

               this.incrementRestartCount();
               
               //Reset flags/settings
               this.errorStateDetectedInCheck = false;
               super.setErrorReason(null);
               this.canRestart = true;
               this.successfulCheckCounter = 0;
               
               if( this.statusTransitionInterrupted || Thread.interrupted() ) return false;
               
               this.doInitialize();
            }
            finally
            {
               this.reinitializingSubSystemDueToError = false;
            }
            
            if( this.statusTransitionInterrupted || Thread.interrupted() ) return false;
            
            this.setStatus(ENABLED);
            this.startThread();  //Start the subsystem thread
            
            return true;
         }
         case STATUS_TRANSITION_TYPE_SHUT_DOWN:
         {
            SubSystem.this.stopThread(); //Stop the subsystem thread
            
            return super.doExecuteStatusTransition(statusTransitionTask);
         }
         case STATUS_TRANSITION_TYPE_ERROR:
         {
            this.stopThread();  //Stop the subsystem thread
            
            boolean result = super.doExecuteStatusTransition(statusTransitionTask);
            
            // Send a message to the top system, letting it know that an error has occurred in this subsystem
            super.fireGlobalEvent(new SubSystemErrorTask(SubSystem.this));
         
            return result;
         }
         case STATUS_TRANSITION_TYPE_CRITICAL_ERROR:
         {
            if( this.isKeySystem() ) logCriticalError("Critical error! Unable to restart subsystem!", JServerConstants.LOG_MESSAGE_ID_CRITICAL_SUBSYSTEM_ERROR);
            else logError("Critical error! Unable to restart subsystem!");
            
            this.stopThread(); //Stop the subsystem thread
            this.canRestart = false;
            
            return super.doExecuteStatusTransition(statusTransitionTask);
         }
      }
      
      return false;
   }
   
   /**
    * Statusmethod for the status ERROR. Subclasses can override this method to provide code to 
    * be executed  when an error has occurred in this subsystem.<br>
    * <br>
    * This implementation exists to override the SubComponent implementation with an empty one, i.e. one that does not call 
    * {@link #doShutDown()}, since this will be done before auto reinitialization anyway. <br>
    * <br>
    * The appropriate way for subclass implementations to signal that an error has occurred in this method is to throw a 
    * {@link StatusTransitionException}.
    * 
    * @see #error(String)
    */
    protected void doError()
    {
    }
   
	
	/**
	 * Engages all child subsystems of this subsystem.
	 * 
	 * @since 1.2
	 */
	/*public final void engageSubSystems()
	{
		List s = this.getSubSystems();
		SubSystem subSys;
		
		for(int i=0; i<s.size(); i++)
		{
			subSys = (SubSystem)s.get(i);
			if( (subSys != null) && (subSys.getStatus() != SubSystem.INITIALIZING) && (subSys.getStatus() != SubSystem.ENABLED) )
			{
				try
				{
					subSys.engage();
				}
				catch(Exception e)
				{
					logError("Error occurred while engaging subsystem + " + subSys.getFullName() + "!", e);
				}
			}
		}
	}*/
	
	/**
	 * Shuts down all child subsystems of this subsystem.
	 * 
	 * @since 1.2
	 */
	/*public final void shutDownSubSystems()
	{
		List s = this.getSubSystems();
		SubSystem subSys;
		
		for(int i=0; i<s.size(); i++)
		{
			subSys = (SubSystem)s.get(i);
			if( (subSys != null) && (subSys.getStatus() != SubSystem.SHUTTING_DOWN) && (subSys.getStatus() != SubSystem.DOWN) )
			{
				try
				{
					subSys.shutDown();
				}
				catch(Exception e)
				{
					logError("Error occurred while shutting down subsystem + " + subSys.getFullName() + "!", e);
				}
			}
		}
	}*/

	/**
	 * Adds a child SubSystem (but doesn't engage it).<br>
	 * <br>
	 * This method will assume ownership of the SubSystem. If a parent already exists it will be 
	 * replaced by <code>this</code> and the target SubSystem will be removed from the original parent.<br>
	 * <br>
	 * <i>Note:</i> Since version 2.0, calling this method has the same effect as calling {@link SubComponent#addSubComponent(SubComponent)} .
	 * 
	 * @param subsys the SubSystem to be added.
	 */
	public void addSubSystem(SubSystem subsys)
	{
		super.addSubComponent(subsys);
	}
	
	/**
	 * Adds a child SubSystem (but doesn't engage it).<br>
	 * <br>
	 * This method will assume ownership of the SubSystem. If a parent already exists it will be 
	 * replaced by <code>this</code> and the target SubSystem will be removed from the original parent.<br>
	 * <br>
	 * <i>Note:</i> Since version 2.0, calling this method has the same effect as calling {@link SubComponent#addSubComponent(SubComponent, String)} .
	 * 
	 * @param subsys the SubSystem to be added.
	 * @param newName a new name for the target SubSystem.
	 */
	public void addSubSystem(SubSystem subsys, String newName)
	{
      super.addSubComponent(subsys, newName);
	}
	
	/**
	 * Adds a child SubSystem. If the parameter <code>engage</code> is true the 
	 * subsystem will be engaged.<br>
	 * <br>
	 * This method will assume ownership of the SubSystem and rename it. If a parent already exists it will be 
	 * replaced by <code>this</code> and the target SubSystem will be removed from the original parent.<br>
	 * <br>
	 * <i>Note:</i> Since version 2.0, calling this method has the same effect as calling {@link SubComponent#addSubComponent(SubComponent, boolean)} .
	 * 
	 * @param subsys the SubSystem to be added.
	 * @param engage boolean value indicating if the SubSystem should be engaged. 
	 * 
	 * @see #engage()
	 */
	public void addSubSystem(SubSystem subsys, boolean engage)
	{
      super.addSubComponent(subsys, engage);
	}
	
	/**
	 * Adds a child SubSystem. If the parameter <code>engage</code> is true the 
	 * subsystem will be engaged.<br>
	 * <br>
	 * This method will assume ownership of the SubSystem and rename it. If a parent already exists it will be 
	 * replaced by <code>this</code> and the target SubSystem will be removed from the original parent.<br>
	 * <br>
	 * <i>Note:</i> Since version 2.0, calling this method has the same effect as calling {@link SubComponent#addSubComponent(SubComponent, String, boolean)} .
	 * 
	 * @param subsys the SubSystem to be added.
	 * @param newName a new name for the target SubSystem.
	 * @param engage boolean value indicating if the SubSystem should be engaged. 
	 * 
	 * @see #engage()
	 */
	public void addSubSystem(SubSystem subsys, String newName, boolean engage)
	{
      super.addSubComponent(subsys, newName, engage);
	}
	
	/**
	 * Removes a child SubSystem (but doesn't shut it down).<br>
	 * <br>
	 * <i>Note:</i> Since version 2.0, calling this method has the same effect as calling {@link SubComponent#removeSubComponent(SubComponent)} .
	 * 
	 * @param subsys the SubSystem to be removed.
	 */
	public boolean removeSubSystem(SubSystem subsys)
	{
		return super.removeSubComponent(subsys);
	}
	
	/**
	 * Removes a child SubSystem.<br>
	 * <br>
	 * <i>Note:</i> Since version 2.0, calling this method has the same effect as calling {@link SubComponent#removeSubComponent(SubComponent, boolean)} .
	 * 
	 * @param subsys the SubSystem to be removed.
	 * @param shutDown boolean value indicating if the SubSystem should be shut down.
	 * 
	 * @see #shutDown()
	 */
	public boolean removeSubSystem(SubSystem subsys, boolean shutDown)
	{
      return super.removeSubComponent(subsys, shutDown);
	}
	
	/**
	 * Checks if this SubSystem owns (is the parent of) the SubSystem specifieid by parameter <code>subsys.</code>.<br>
	 * <br>
	 * <i>Note:</i> Since version 2.0, calling this method has the same effect as calling {@link SubComponent#hasSubComponent(SubComponent)} .
	 * 
	 * @return <code>true</code> if this SubSystem owns the specified SubSystem, otherwise <code>false</code>.
	 */
	public final boolean hasSubSystem(final SubSystem subsys)
	{
      return super.hasSubComponent(subsys);
	}
	
	/**
	 * Returns the child components of this SubSystems that are SubSystem instances (not SubComponents).
	 * 
	 * @return List containing SubSystems.
	 */
	public final List getSubSystems()
	{
		List subComponentList = super.getSubComponents();
      ArrayList subSystemList = new ArrayList();
      Object component;
      
      for(int i=0; i<subComponentList.size(); i++)
      {
         component = subComponentList.get(i);
         if( component instanceof SubSystem )
         {
            subSystemList.add(component);
         }
      }
      
      return subSystemList;
	}
	
	/**
	 * Gets the SubSystem (not SubComponent) with the specified name contained in this SubSystems.
	 * 
	 * @param name the name of the SubSystem.
	 * 
	 * @return a SubSystem or null if none was found.
	 */
	public SubSystem getSubSystem(final String name)
	{
      SubComponent s = super.getSubComponent(name);
      
      if( s instanceof SubSystem ) return (SubSystem)s;
		else return null;
	}
	
	/**
	 * Returns a recursive listing of all child SubSystem of this SubSystem (not SubComponents).
	 * 
	 * @return a List containing SubSystem.
    * 
    * @since 2.0
    * 
    * @see SubComponent#getSubComponentTree()
	 */
	public final List getSubSystemTree()
	{
      List subComponentList = super.getSubComponentTree();
      ArrayList subSystemList = new ArrayList();
      Object component;
      
      for(int i=0; i<subComponentList.size(); i++)
      {
         component = subComponentList.get(i);
         if( component instanceof SubSystem )
         {
            subSystemList.add(component);
         }
      }
      
      return subSystemList;
	}
}
