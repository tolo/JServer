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

/**
 * Class used by ObjectPool to do clean up.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class PoolCleaner extends ThreadGroup implements Runnable
{
	private Thread cleanUpThread;
	
	private boolean canRun = true;
	private int restartCounter = 0;
	private long lastRestart = -1;
	
	private final ObjectPool pool;
	private long cleanUpInterval;
		
	/**
	 * Creates a new PoolCleaner.
	 * 
	 * @param pool the associated pool.
	 * @param cleanUpInterval the interval between cleanups (in milliseconds).
	 */
	public PoolCleaner(ObjectPool pool, long cleanUpInterval)
	{
		super((pool.getThreadGroup() != null) ? pool.getThreadGroup() : Thread.currentThread().getThreadGroup(), pool.getFullName() + ".PoolCleaner");
			
		this.pool = pool;
		this.cleanUpInterval = cleanUpInterval;
	}
	
	/**
	 * Start cleaning up.
	 */
	public void start()
	{
		if(pool.isDebugMode()) pool.logDebug(getName(), "(start) Starting PoolCleaner.");
		canRun = true;
		cleanUpThread = new Thread(this, this, "PoolCleaner for " + pool.getName());
		cleanUpThread.setDaemon(true);
		//cleanUpThread.setPriority(Thread.NORM_PRIORITY-1);
		cleanUpThread.start();
	}
	
	/**
	 * Stop cleaning up.
	 */
	public void kill()
	{
		if(pool.isDebugMode()) pool.logDebug(getName(), "(kill) Killing PoolCleaner.");
		canRun = false;
		cleanUpThread.interrupt();
		cleanUpThread = null;
		try
		{
			Thread.sleep(500);
			destroy();
		}catch(Exception e){}
	}
	
	/**
	 * The thread method of this PoolCleaner. Calls the method <tt>cleanUp()</tt> in the associated pool.
	 * 
	 * @see ObjectPool#cleanUp()
	 */
	public void run()
	{
		long sleepTime = cleanUpInterval;
		
		if(pool.isDebugMode()) pool.logDebug(getName(), "(run) PoolCleaner started.");
		
		while(canRun)
		{
			sleepTime = cleanUpInterval;
			try
			{
				Thread.sleep(sleepTime);
			}
			catch(InterruptedException e)
			{
				continue;
			}
			
			try
			{
				if(!canRun) continue;
				pool.cleanUp();
			}
			catch(Exception e)
			{
				pool.logWarning(getName(), "Error during cleanup", e);
			}
			
			if(lastRestart > 0 && (System.currentTimeMillis() - lastRestart) > (2*cleanUpInterval))
			{
				if(pool.isDebugMode()) pool.logDebug(getName(), "(run) Resetting restartCounter. Previous value: " + restartCounter + ".");
				restartCounter = 0;
			}
		}

		if(pool.isDebugMode()) pool.logDebug(getName(), "(run) PoolCleaner terminated.");
	}
	
	/**
	 * Handles uncaught exceptions in this class.
	 */
	public void uncaughtException(Thread thread, Throwable exception)
	{
		if(exception instanceof ThreadDeath)
		{
			super.uncaughtException(thread, exception);
		}
		else
		{
			if(exception instanceof OutOfMemoryError)
			{
				// Relay OutOfMemoryError to parent thread group (TopThreadGroup)
				ThreadGroup parentGroup = this.getParent();				if(parentGroup != null) 				{
					parentGroup.uncaughtException(thread, exception);
					return;
				}
			}
			
			if(thread == cleanUpThread)
			{
				pool.logWarning(getName(), "PoolCleaner died! Restarting.", exception);
				
				kill();
				if(restartCounter < 10)
				{
					lastRestart = System.currentTimeMillis();
					restartCounter++;
					start();
				}
				else pool.logWarning(getName(), "PoolCleaner died! PoolCleaner disabled!", exception);
					
			}
			else // If threadPool...
			{
				pool.logError("Uncaught exception in thread " + thread.getName() + "!", exception);
			
				if(thread instanceof PoolThread)
				{
					((PoolThread)thread).error();
				}
			}
		}
	}
	
	/**
	 * Gets the cleanUpInterval.
	 * 
	 * @since 2.0
	 */
	public final long getCleanUpInterval()
	{
		return this.cleanUpInterval;
	}
	
	/**
	 * Sets the cleanUpInterval.
	 * 
	 * @param cleanUpInterval the cleanUpInterval.
	 */
	public final void setCleanUpInterval(int cleanUpInterval)
	{
		this.cleanUpInterval = cleanUpInterval;
	}
}
