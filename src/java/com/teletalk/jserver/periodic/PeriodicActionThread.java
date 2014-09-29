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

import com.teletalk.jserver.pool.PoolWorker;

/**
 * Thread pool worker class for execution of actions.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.2
 */
public final class PeriodicActionThread extends PoolWorker
{
	private final PeriodicActionManager periodicActionManager;
	
	private PeriodicAction action;
	
	/**
	 * Creates a new PeriodicActionThread.
	 * 
	 * @param periodicActionManager reference to a parent PeriodicActionManager.
	 */
	public PeriodicActionThread(final PeriodicActionManager periodicActionManager)
	{
		this.periodicActionManager = periodicActionManager;
	}
	
	/**
	 * Sets the PeriodicAction object that is to be executed in this thread.
	 * 
	 * @param data a PeriodicAction object.
	 */
	public void setData(final Object data)
	{
		this.action = (PeriodicAction)data;
	}
	
	/**
	 * Executes the current action.
	 */
	public void work()
	{
		boolean result = false;
		
		try
		{
			action.setLastExecutionResult(""); // Reset action execution message
			result = action.execute();
		}
		catch(Throwable t)
		{
         action.actionFatalError(t);
         			
			if( (action.getLastExecutionResult() == null) || (action.getLastExecutionResult().trim().equals("")) )
			{
				action.setLastExecutionResult("Fatal error while executing action - " + t + ".");
			}
			
			if(t instanceof Error) throw (Error)t;
		}
		finally
		{
			this.periodicActionManager.actionExecutionCompleted(action, result);
		}
	}
	
	/**
	 * Clean up method.
	 */
	public void cleanUp()
	{
		action = null;
	}
	
	/**
	 * Destroy method.
	 */
	public void destroy()
	{
		action = null;
	}
}
