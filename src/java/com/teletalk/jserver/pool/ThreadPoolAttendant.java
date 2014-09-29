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
 * A class used by ThreadPool for detecting errors in PoolThread objects.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class ThreadPoolAttendant extends ThreadGroup
{
	private final ThreadPool threadPool;
	
	/**
	 * Creates a new ThreadPoolAttendant for the specified ThreadPool.
	 * 
	 * @param threadPool the associated ThreadPool object.
	 */
	public ThreadPoolAttendant(ThreadPool threadPool)
	{
		super("ThreadPoolAttendant for " + threadPool.getName());
		
		this.threadPool = threadPool;
	}
	
	/**
	 * Creates a new ThreadPoolAttendant for the specified ThreadPool.
	 * 
	 * @param threadPool the associated ThreadPool object.
	 */
	public ThreadPoolAttendant(ThreadPool threadPool, ThreadGroup parentGroup)
	{
		super(parentGroup ,"ThreadPoolAttendant for " + threadPool.getName());
		
		this.threadPool = threadPool;
	}
	
	/**
	 * Called by the Java Virtual Machine when a thread in this thread group stops because of an uncaught exception.
	 * 
	 * @param thread the thread that is about to exit.
	 * @param exception the uncaught exception.
	 */
	public void uncaughtException(final Thread thread, final Throwable exception)
	{
		if(exception instanceof ThreadDeath) // When Thread.stop is invoked (should never be performed on PoolThreads...)
		{
		   if(thread instanceof PoolThread)
			{
		      ((PoolThread)thread).kill();
			}
		   
		   super.uncaughtException(thread, exception);
		}
		else
		{
			boolean outOfMemoryError = false;
			
		   if(exception instanceof OutOfMemoryError)
			{
		      outOfMemoryError = true;
		      
		      // Relay OutOfMemoryError to parent thread group (TopThreadGroup)
		      ThreadGroup parentGroup = this.getParent();
				if(parentGroup != null) 				{
					parentGroup.uncaughtException(thread, exception);
					//return;
				}
			}
			
			if( !outOfMemoryError ) threadPool.logError("Uncaught exception in thread " + thread.getName() + "!", exception);
			
			if(thread instanceof PoolThread)
			{
				((PoolThread)thread).error();
			}
		}
	}
}
