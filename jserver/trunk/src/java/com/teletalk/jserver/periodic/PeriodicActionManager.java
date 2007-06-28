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

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.LinkedList;

import com.teletalk.jserver.StatusTransitionException;
import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.pool.ThreadPool;
import com.teletalk.jserver.property.NumberProperty;

/**
 * This class implements the top system of the periodic action module. PeriodicActionManager is responsible 
 * for keeping track on all registered actions and executing them according to their periodiciy settings.  
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.2
 */
public class PeriodicActionManager extends SubSystem
{
	/**
	 * Comparator used for inserting actions in the action queue according to the next executing time.
	 */
	private static final Comparator PeriodicActionExecutionTimeComparator = new Comparator()
		{
		/**
		* Compares two PeriodicAction objects for order according to next execution time. Returns a negative integer, zero, or a 
		* positive integer as the first argument is less than, equal to, or greater than the second.
		* 
		* @param o1 the first object to be compared.
		* @param o2 the second object to be compared.
		* 
		* @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
		*/
		public int compare(final Object o1, final Object o2)
		{
         return (int)(((PeriodicAction)o1).getTimeOfNextExecution() - ((PeriodicAction)o2).getTimeOfNextExecution());
		}
   };
	
   
   long actionCheckInterval = 60000;
   
   long actionExecutionWaitTime = -1; // Only for testing
   
	ThreadPool actionThreadPool;
   
	
   //private final BooleanProperty actionsPanelEnabled;
   
   private final NumberProperty abnormalExecutionTime; 
   
	
   private final ArrayList actionList; // All actions
   
   private final LinkedList actionQueue; // Non-executing actions
   
   final Object actionsLock;
	
   
	/**
	 * Creates a new PeriodicActionManager, named "PeriodicActionManager".
	 * 
	 * @param parent the parent subsystem.
	 */
	public PeriodicActionManager(final SubSystem parent)
	{
		this(parent, "PeriodicActionManager");
	}
	
	/**
	 * Creates a new PeriodicActionManager.
	 * 
	 * @param parent the parent subsystem.
	 * @param name the name of this PeriodicActionManager subsystem.
	 */
	public PeriodicActionManager(final SubSystem parent, final String name)
	{
		super(parent, name);

      //this.actionsPanelEnabled = new BooleanProperty(this, "actionsPanelEnabled", false, BooleanProperty.MODIFIABLE_NO_RESTART);
      //this.actionsPanelEnabled.setDescription("");
      //this.addProperty(this.actionsPanelEnabled);
      this.abnormalExecutionTime = new NumberProperty(this, "abnormalExecutionTime", 60, NumberProperty.MODIFIABLE_NO_RESTART);
      this.abnormalExecutionTime.setDescription("The maximum time (in minutes) an action is allowed to execute before a critical error is logged.");
      this.addProperty(this.abnormalExecutionTime);
				
      this.actionList = new ArrayList();
		this.actionQueue = new LinkedList();
      this.actionsLock = this.actionQueue; 
   }
	
	/**
	 * Initializes this PeriodicActionManager.
	 */
	protected void doInitialize()
	{
		super.doInitialize();
		
		if( !super.isReinitializing() )
		{
         try
         {
            this.actionThreadPool = new ThreadPool(this, "PeriodicActionThreadPool", 1, PeriodicActionThread.class, new Object[]{this});
            this.addSubComponent(this.actionThreadPool, true);
            
            if( (this.actionQueue.size() > 1) && (this.actionThreadPool.getMaxSize() <= 1) )
            {
               this.actionThreadPool.setMaxSize(this.actionQueue.size());
            }
         }
         catch(Exception e)
         {
            throw new StatusTransitionException("Error while engaging - unable to create actionThreadPool!", e);
         }
         
         /*if( this.actionsPanelEnabled.booleanValue() )
         {
            try
            {
               this.setRmiAdapter(new PeriodicActionManagerRmiAdapter(this));
               JServer.getJServer().getAdministration().addCustomAdmPanel(ActionsInfoPanel.class, new Object[]{this.getRmiAdapter()});
            }
            catch(Exception e)
            {
               throw new StatusTransitionException("Error while engaging - unable to create administration panel!", e);
            }
         }*/
		}
	}
	
	/**
	 * Shuts down this PeriodicActionManager.
	 */
	protected void doShutDown()
	{
		super.doShutDown();

		if( !super.isReinitializing() )
		{
         this.destroyThreadPool();
      }
	}
   
   /**
    * Destroys the thread pool.
    */
   private final void destroyThreadPool()
   {
      if(this.actionThreadPool != null)
      {
         try{
         this.actionThreadPool.shutDown();
         removeSubComponent(this.actionThreadPool);
         this.actionThreadPool = null; 
         }catch(Exception e){}
      }
   }
   
   /**
    * Adds a PeriodicAction.
    */
   public void addPeriodicAction(final PeriodicAction action)
   {
      addPeriodicActionInternal(action, true);
   }
   
   /**
    */
   void addPeriodicActionInternal(final PeriodicAction action, boolean enable)
   {
      boolean added = false;
      synchronized(this.actionsLock)
      {
         if( (action != null) && !this.actionList.contains(action) )
         {
            added = true;
            this.actionList.add(action);
            this.addActionToQueue(action);
            
            action.periodicActionManager = this; // Just in case...
         }
      }
      
      if( added )
      {
         if( !super.hasSubComponent(action) )
         {
            super.addSubComponent(action);
         }
         
         if( enable && !action.isEnabled() ) action.engage();
         
         this.logInfo("Action " + action + " added.");
      }
   }
   
   /**
    * Removes a periodic action.
    */
   public void removePeriodicAction(final PeriodicAction action)
   {
      removePeriodicActionInternal(action, true);
   }
   
   /**
    * Removes a periodic action.
    * 
    * @since 2.1 (20050825)
    */
   public void removePeriodicAction(final String name)
   {
      PeriodicAction action = this.getPeriodicAction(name);
      if( action != null ) removePeriodicActionInternal(action, true);
   }
   
   /**
    */
   void removePeriodicActionInternal(final PeriodicAction action, boolean disable)
   {
      boolean removed = false;
      synchronized(this.actionsLock)
      {
         if( (action != null) && this.actionList.contains(action) )
         {
            removed = true;
            this.actionList.remove(action);
            this.removeActionFromQueue(action);
         }
      }
      
      if( removed )
      {
         if( super.hasSubComponent(action) )
         {
            super.removeSubComponent(action);
         }
         
         if( disable && action.isEnabled() ) action.shutDown();
         
         this.logInfo("Action " + action + " removed.");
      }
   }
   
   /**
    * Gets all registered periodic actions.
    * 
    * @since 2.1 (20050825)
    */
   public PeriodicAction[] getPeriodicActions()
   {
      synchronized(this.actionsLock)
      {
         return (PeriodicAction[])this.actionList.toArray(new PeriodicAction[0]);
      }
   }
   
   /**
    * Gets a registered periodic action.
    * 
    * @since 2.1 (20050825)
    */
   public PeriodicAction getPeriodicAction(final String name)
   {
      PeriodicAction action = null;
      
      synchronized(this.actionsLock)
      {
         for(int i=0; i<this.actionList.size(); i++)
         {
            action = (PeriodicAction)this.actionList.get(i);
            if( (action != null) && action.getName().equals(name) )
            {
               return action;
            }
         }
      }
      
      return null;
   }
	
	/**
	 * Gets a String matrix containing name, status and description for each registered action.
	 * 
	 * @return a String matrix containing name, status and description for each registered action.
	 */
	public String[][] getActionDescriptions()
	{
      PeriodicAction[] actions = null;
      synchronized(this.actionsLock)
      {
         actions = (PeriodicAction[])this.actionList.toArray(new PeriodicAction[0]);
      }
      
      String[][] actionDescriptions = new String[actions.length][3];
		
		for(int i=0; i<actions.length; i++)
		{
			actionDescriptions[i][0] = actions[i].getName();
			actionDescriptions[i][1] = actions[i].getActionStatus();
			actionDescriptions[i][2] = actions[i].getLongDescription();
		}
		
		return actionDescriptions;
	}
   
   /**
    * Exceutes the action with the specified name if it is not already executing.
    * 
    * @param action the action to execute.
    */
   public void executeAction(final PeriodicAction action)
   {
      boolean success = false;
         
      synchronized(this.actionsLock)
      {
         if( this.actionQueue.contains(action) )
         {
            this.actionQueue.remove(action);
               
            this.executeActionInternal(action);
            success = true;
         }
      }
      
      if( !success )
      {
         logWarning("Unable to execute action '" + action + "'. It may already be running.");
      }
   }
	
	/**
	 * Exceutes the action with the specified name if it is not already executing.
	 * 
	 * @param actionName the name of the action to execute.
	 */
	public void executeAction(final String actionName)
	{
		PeriodicAction action;
		boolean success = false;
			
		synchronized(this.actionsLock)
		{
			// Search for action in action queue
			for(int i=0; i<this.actionQueue.size(); i++)
			{
				action = (PeriodicAction)this.actionQueue.get(i);
				if( (action != null) && action.getName().equals(actionName) )
				{
					this.actionQueue.remove(i);
					
					this.executeActionInternal(action);
					success = true;
					
					break;
				}
			}
		}
		
		if( !success )
		{
			logWarning("Unable to execute action '" + actionName + "'. It may already be running.");
		}
	}
	
	/**
	 */
	private void executeActionInternal(final PeriodicAction action)
	{
		if( super.isDebugMode() ) logDebug("Executing action " + action.getName() + ".");
		
		// Execute action (in separate thread)
		action.actionPreExecuteInternal();
		this.actionThreadPool.initializeThread(action);
	}
   
   /**
    * Must be called with lock on actionsLock.
    */
   private void addActionToQueue(final PeriodicAction action)
   {
      if( !this.actionQueue.contains(action) ) // Don't add if action already exists in queue
      {
         action.setActionStatus(PeriodicAction.STATUS_IDLE);
         
         this.doAddActionToQueue(action);
      }
   }
   
   /**
    * Must be called with lock on actionsLock.
    * 
    * @since (20041022)
    */
   private void doAddActionToQueue(final PeriodicAction action)
   {
      // Calculate next execution time of action
      action.calculateTimeOfNextExecution();
      
      int index = Collections.binarySearch(this.actionQueue, action, PeriodicActionExecutionTimeComparator);
      if(index < 0) index = (-index) -1; //If key not found, return value of Collections.binarySearch is (-(insertion point) - 1)
      
      if( (index < 0) || (index >= this.actionQueue.size()) )
      {
         this.actionQueue.add(action);
      }
      else
      {
         this.actionQueue.add(index, action);
      }
      
      this.actionsLock.notify();
   }
   
   /**
    */
   private void removeActionFromQueue(final PeriodicAction action)
   {
      this.actionQueue.remove(action);
      this.actionsLock.notify();
   }
   
   /**
    * Called to indicate that action periodicity parameters have been modified.
    * 
    * @since (20041022)
    */
   public void actionPeriodicityModified(final PeriodicAction action)
   {
      synchronized(this.actionsLock)
      {      
         if( this.actionQueue.remove(action) ) // If not executing (i.e. if present in queue)...
         {
            this.doAddActionToQueue(action); // ...add with updated execution time
         }
      }
   }
	
	/**
	 * Called by PeriodicActionThread objects when an action has finished it's execution cycle.
	 */
	void actionExecutionCompleted(final PeriodicAction action, final boolean success)
	{
		if( super.isDebugMode() ) 
		{
			if(success) logDebug("Execution of action " + action.getName() + " completed successfully (result message: " + action.getLastExecutionResult() + ").");
			else logDebug("Execution of action " + action.getName() + " failed (result message: " + action.getLastExecutionResult() + ")!");
		}
      
      action.actionIdleInternal();
		
      synchronized(this.actionsLock)
      {
         if( this.actionList.contains(action) ) // Only place in queue if action still exists in this PeriodicActionManager 
         {
            this.addActionToQueue(action);
         }
      }
	}
	
	/**
	 * Gets the first action in the queue and checks if it is time to execute it. If so, the action 
	 * is executed and removed from the queue and this mehtod will be called again.
	 * 
	 * @param currentTimeMs the execution time of the next action or -1 if there are no more actions in the queue.
	 */
	private long executeNextAction(final long currentTimeMs)
	{
		if( !this.actionQueue.isEmpty() )
		{
			final PeriodicAction nextAction = ((PeriodicAction)this.actionQueue.getFirst());
			final long timeOfNextActionExecution = nextAction.getTimeOfNextExecution();
			
			if( timeOfNextActionExecution <= currentTimeMs )
			{
				this.actionQueue.removeFirst();
				
				this.executeActionInternal(nextAction);
				
				return this.executeNextAction(currentTimeMs);
			}
			else return timeOfNextActionExecution;
		}
		else return -1;
	}
	
	/**
	 * The famous thread method.
	 */
	public void run()
	{
		//long timeOfNextHour;
	   long timeOfNextMinute;
		long timeOfNextActionExecution;
		Calendar currentTime;
		long currentTimeMs;
      long lastActionCheck = System.currentTimeMillis();
		
		while(canRun)
		{
			currentTime = new GregorianCalendar();
			currentTimeMs = currentTime.getTime().getTime();
			
			synchronized(this.actionsLock)
			{
				// Execure the first action(s) in the queue and/or get execution time of next action.
				timeOfNextActionExecution = this.executeNextAction(currentTimeMs);
			
   			// Reevaluate action execution times at least every minute to detect time changes and check for hung action.
            currentTime.add(Calendar.MINUTE, 1);
            timeOfNextMinute = currentTime.getTime().getTime();

            if( super.isDebugMode() ) logDebug("Next action execution: " + new Date(timeOfNextActionExecution) + ".");

   			try
   			{
				   if( timeOfNextActionExecution > currentTimeMs )
               {
                  this.actionsLock.wait(Math.min((timeOfNextMinute-currentTimeMs), (timeOfNextActionExecution-currentTimeMs)));
               }
               else if( this.actionExecutionWaitTime > 0 ) // Only for testing
               {
                  this.actionsLock.wait(this.actionExecutionWaitTime);
               }
               else
               {
                  this.actionsLock.wait(timeOfNextMinute-currentTimeMs);
               }
   			}
   			catch(InterruptedException ie)
   			{
   				continue;
   			}
         
            
            if( (System.currentTimeMillis() - lastActionCheck) >= actionCheckInterval )
            {
               lastActionCheck = System.currentTimeMillis();
               
      			// Check for actions that have exceeded max execution time
               PeriodicAction[] actions = (PeriodicAction[])this.actionList.toArray(new PeriodicAction[0]);
               long executionStartTime;
               long maxExecutionTimeMinutes = this.abnormalExecutionTime.longValue();
               long maxExecutionTimeMs = maxExecutionTimeMinutes * 60 * 1000;
               
               for (int i=0; i<actions.length; i++)
               {
                  if (actions[i] != null)
                  {
                     executionStartTime = actions[i].getExecutionStartTime();
                     if( actions[i].isExecuting() && ((System.currentTimeMillis() - executionStartTime) > maxExecutionTimeMs) )
                     {
                        if( !actions[i].isCriticalErrorDispatched() )
                        {
                           logCriticalError("Maximum execution time (" + maxExecutionTimeMinutes + " min) exceeded for action " + actions[i].getName() + "!", LOG_MESSAGE_ID_ACTION_EXECUTION_TIME_EXCEEDED);
                           actions[i].setCriticalErrorDispatched(true);
                        }
                     }
                  }
               } 
            }
            
         }// End synchronized block
		}
	}
}
