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
package com.teletalk.jserver.util;

/**
 * The Future class is a simple thread synchronization device that basically 
 * is a placeholder for a value that is to be set by another thread than that which 
 * created it. When the future is created it can be given an initial value (<code>null</code> 
 * is used by default) and after that its value can only be assigned 
 * once. The thread that created the future can wait for the value to get set by calling one of 
 * the <code>getValue</code>-methods, which will block the calling thread until a 
 * value is assigned or a timeout occurs.
 *  
 * @author Tobias Löfstrand
 * 
 * @since 1.1 Build 545
 */
public class Future
{
	private boolean valueSet = false;
   private boolean cancelled = false;
	private Object value;
	
	/**
	 * Creates a new Future object. The initial value of the Future will be a reference to itself.
	 */
	public Future()
	{
		this(null);
	}
	
	/**
	 * Creates a new Future object.
	 * 
	 * @param initialValue the initial value of this future.
	 */
	public Future(final Object initialValue)
	{
		this.value = initialValue;
		this.valueSet = false;
	}
	
	/**
	 * Gets the value of the future. If a value hasn't been assigned yet this method 
	 * will block the calling thread until another thread calls the method {@link #setValue(Object)}.
	 * 
	 * @return the value of the future.
	 */
	public synchronized Object getValue() throws InterruptedException
	{
		while(!this.valueSet) wait();
		return this.value;
	}
	
	/**
	 * Gets the value of the future. If a value hasn't been assigned yet this method 
	 * will block the calling thread until another thread calls the method {@link #setValue(Object)} or the 
	 * number of millseconds specified by parameter <code>timeOut</code> have elapsed.
	 * 
	 * @param timeOut the maximum time in milliseconds to wait for the value to get set.
	 * 
	 * @return the value of the future.
	 * 
	 * @throws RuntimeException if a value hasn't been set in <code>timeOut</code> milliseconds.
	 */
	public synchronized Object getValue(final long timeOut) throws InterruptedException
	{
		if( !this.valueSet )
		{
			long beginWait = System.currentTimeMillis();
			long waitTime; 
			
			while(!this.valueSet)
			{
				waitTime = timeOut - (System.currentTimeMillis() - beginWait);
				if(waitTime > 0)
				{
					wait(waitTime);
				}
				else break;
			}
		}
		return this.value;
	}
	
	/**
	 * Sets the value of this future. When this method is called, all threads that are waiting in 
	 * one of the <code>getValue</code>-methods are waked up.
	 * 
	 * @param value the value that is to be assigned to the future.
	 * 
	 * @return <code>true</code> if the value of this future was not set prior to the call to this method, otherwise <code>false</code>.
	 */
	public synchronized boolean setValue(Object value)
	{
		if(!this.valueSet)
		{
			this.value = value;
			this.valueSet = true;
			notifyAll();
			return true;
		}
		else return false;
	}
	
	/**
	 * Checks if the value of this future has been assigned.
	 * 
	 * @return <code>true</code> if the value of this future has been assigned, otherwise <code>false</code>.
	 */
	public synchronized boolean isSet()
	{
		return this.valueSet;
	}

   /**
    * Marks this future as cancelled.
    * 
    * @since 2.1.2 (20060215)
    */
   public synchronized void setCancelled(boolean cancelled)
   {
      this.cancelled = cancelled;
   }
   
   /**
    * Checks if this future is cancelled.
    * 
    * @since 2.1.2 (20060215)
    */
   public synchronized boolean isCancelled()
   {
      return cancelled;
   }
}
