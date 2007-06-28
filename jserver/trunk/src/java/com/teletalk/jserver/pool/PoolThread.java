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

import com.teletalk.jserver.load.LoadManager;
import com.teletalk.jserver.util.IdentifiableThread;

/**
 * This class is used by ThreadPool as a thread wrapper for custom implementations of the class
 * PoolWorker.
 * 
 * @see ThreadPool
 * @see PoolWorker
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class PoolThread extends Thread implements IdentifiableThread
{
   private boolean canRun = true;
   
   private boolean destroyed = false;
	
	private final ThreadPool parent;
	private final PoolWorker worker;
	private final int id;
	
	private boolean initialized = false;
			
	/**
	 * Created a new PoolThread object.
	 * 
	 * @param parent the parent ThreadPool.
	 * @param id the id of this PoolThread.
	 * @param thrGroup the ThreadGroup object responsible for detecting errors in this PoolThread.
	 * @param worker the PoolWorker object which performs the actual work of the thread.
	 * 
	 * @see PoolWorker
	 */
	public PoolThread(ThreadPool parent, int id, ThreadGroup thrGroup, PoolWorker worker)
	{
	   super(thrGroup, "PoolThread" + id + "-(" + worker.getName() + ")");
				
		this.parent = parent;
		this.id = id;
		this.worker = worker;
				
		this.setDaemon(true);
		this.worker.init(this);
	}
		
	/**
	 * Renames this PoolThread by getting an updated name from the associated pool worker.
	 *
	 * @since 2.0, build 761.
	 */
	protected void rename()
	{
	   super.setName("PoolThread" + id + "-(" + worker.getName() + ")");
	}
	
	/**
	 * Initializes (wakes up) this PoolThread with debugmode disabled.
	 * 
	 * @param data optional data to be sent to the PoolWorker contained in this PoolThread.
	 */
	public synchronized void initialize(Object data)
	{
		this.worker.setData(data);
		this.setInitialized(true);
	}
	
	/**
	 * Return the PoolWorker object contained in this PoolThread.
	 * 
	 * @return a PoolWorker object.
	 */
	public final PoolWorker getWorker()
	{
		return worker;
	}
	
	/**
	 * Returns the id of this PoolThread.
	 * 
	 * @return the id.
	 */
	public final String getThreadId()
	{
		return parent.getName() + "-t" + id;
	}
	
	/**
	 * Gets the parent ThreadPool object.
	 * 
	 * @return the parent ThreadPool.
	 */
	public ThreadPool getParent()
	{
		return this.parent;
	}
	
	/**
	 * Makes the calling thread wait if there is no data.
	 */
	public synchronized void waitForInitialization()
	{
		try
		{
			while(!initialized) wait();
		}
		catch(InterruptedException e){}
	}
	
	/**
	 */
	private synchronized void setInitialized(boolean poolThreadInitialized)
	{
		this.initialized = poolThreadInitialized;
		notify();
	}
	
	/**
	 * The thread method of this class. It waits for data and when it arrives 
	 * the method work in the PoolWorker object contained in this PoolThread is
	 * called.
	 * 
	 * @see PoolWorker#work
	 */
	public void run()
	{
	   while(canRun)
		{
			this.waitForInitialization();
				
			if( this.initialized && this.canRun )
			{
			   worker.work();
			   
			   //LoadValue.setThreadLoad(0);

				worker.cleanUp();
				this.setInitialized(false);
				parent.returnToPool(this);
			}
			else this.setInitialized(false);
		}
	}
	
	/**
	 * Destroys this PoolThread.
	 */
	public void kill()
	{
	   if( !destroyed )
	   {
		   canRun = false;
			setInitialized(false);
			
			worker.destroy();
	
			try
			{
				this.interrupt();
			}
			catch(Exception e){}
			
		   // Unregister thread as load thread in case it was previously registered
		   LoadManager.unregisterLoadThread(this);
		   
		   destroyed = true;
	   }
	}
	
	/**
	 * Signals that an error has occured in this PoolThread. Calling this method will kill the PoolThread and report 
	 * it as "bad" to the ThreadPool.
	 */
	public void error()
	{
		kill();
		parent.badObject(this);
	}
	
	/**
	 * Checks this PoolThread and it's associated PoolWorker object for errors.
	 * 
	 * @return <code>true</code> if there are no errors, otherwise <code>false</code>.
	 */
	public boolean validate()
	{
		return (this.isAlive() && worker.validate());
	}
}
