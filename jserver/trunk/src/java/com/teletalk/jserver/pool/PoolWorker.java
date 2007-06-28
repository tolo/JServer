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
package com.teletalk.jserver.pool;

import org.apache.log4j.Level;

import com.teletalk.jserver.log.ComponentLogger;
import com.teletalk.jserver.log.LoggableObject;

/**
 * Abstract baseclass for all classes that are to be used to perform work in a ThreadPool.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public abstract class PoolWorker extends LoggableObject
{
	/** The thread. */
	protected PoolThread thread = null;

	/** The name of this PoolWorker. */
	protected String name;
	
	/**
	 * Constructs a new PoolWorker object with the name field set to the last part of the class name.
	 */
	public PoolWorker()
	{
		String clsName = getClass().getName();
		if(clsName.lastIndexOf(".") > 0) name = clsName.substring(clsName.lastIndexOf("."));
		else name = clsName;
	}
	
	/**
	 * Constructs a new PoolWorker object.
	 * 
	 * @param nameBase the base name of this PoolWorker.
	 */
	public PoolWorker(String nameBase)
	{
		this.name = nameBase;
	}
	
	/**
	 * Called by PoolThread to perform post creation initialization of this PoolWorker.
	 * 
	 * @param thread the associated PoolThread.
	 */
	protected final void init(PoolThread thread)
	{
		this.name = name + "[" + thread.getThreadId() + "]";
		this.thread = thread;
		this.thread.rename();
	}
   
   /**
    * Gets the logger that will be used by this object to dispatch logging events. Subclasses may override this method to provide 
    * an alternate way of getting the logger.
    */
   protected ComponentLogger getLogger()
   {
      if( this.thread != null )
      {
         return this.thread.getParent().getLogger();
      }
      else
      {
         return super.getLogger();
      }
   }
   
   /**
    * Creates a logging event. This method overrides the super class implementation and adds the name of this pool worker to the log message.
    * 
    * @since 2.1.1 (20060118)
    */
   protected void doLog(final String callerFQN, final Level level, final String origin, final Object msg, final Throwable throwable, final boolean useLogMessageId, final int logMessageId)
   {
      super.doLog(callerFQN, level, origin, this.getName() + " - " + msg, throwable, useLogMessageId, logMessageId);
   }
   
	/**
	 * Gets the PoolThread used by this PoolWorker.
	 * 
	 * @return the PoolThread used by this PoolWorker.
	 */
	public final PoolThread getThread()
	{
		return thread;
	}
	
	/**
	 * Gets the name of this PoolWorker.
	 * 
	 * @return the name of this PoolWorker, null if there is none.
	 */
	public final String getName()
	{
		return name;
	}
	
	/**
	 * Gets a string representation of this PoolWorker.
	 * 
	 * @return a string representation of this PoolWorker.
	 */
	public String toString()
	{
		return this.getName();
	}
	
	/**
	 * Sets a new name for this PoolWorker.
	 * 
	 * @param newName the new name for this PoolWorker.
	 */
	public final void rename(String newName)
	{
		this.name = newName;
	}
	
	/**
	 * Method to set an optional data object for this PoolWorker. Subclasses should override this method 
	 * to take care of data that is to be transferred to the PoolWorker before the work method is called. This implementation does nothing.
	 * 
	 * @param data the data.
	 */
	protected void setData(Object data)
	{
	}
	
	/**
	 * Gets the debug mode flag from the associated ThreadPool.
	 * 
	 * @return boolean indicating if debugmode is enabled or not.
	 */
	public final boolean isDebugMode()
	{
      return (thread != null) ? thread.getParent().isDebugMode() : false;
	}
	
	/**
	 * Abstract method to perform the actual work this PoolWorker is supposed to perform.
	 */
	protected abstract void work();
	
	/**
	 * Performs optional clean up. This method is called after this PoolWorker has returned from the work method. 
	 * This implementation does nothing.
	 */
	protected void cleanUp()
	{
	}
	
	/**
	 * This method is called when the PoolWorker can no longer be reused and is to be killed. Subclasses should override this method to do cleanup. 
	 */
	protected void destroy()
	{
	}
	
	/**
	 * Checks this PoolWorker object for errors.
	 * 
	 * @return <code>true</code> if there are no errors, otherwise <code>false</code>.
	 */
	protected boolean validate()
	{
		return true;
	}
}
