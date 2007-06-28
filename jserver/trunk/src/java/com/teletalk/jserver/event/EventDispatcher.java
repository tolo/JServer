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
package com.teletalk.jserver.event;

import com.teletalk.jserver.pool.PoolWorker;

/**
 * This class represents an event dispatching thread, used in conjunction with a thread pool.
 * 
 * @author Tobias Löfstrand
 * 
 * @see EventQueue
 * 
 * @since 1.3
 */
public class EventDispatcher extends PoolWorker
{
	private final EventQueue eventQueue;
	
	private Object eventObject;
	
	/**
	 * Creates a new EventDispatcher
    * 
	 * @param eventQueue the associated EventQueue.
	 */
	public EventDispatcher(EventQueue eventQueue)
	{
		this.eventQueue = eventQueue;
	}
	
	/**
	 * Performs clean up of the event dispatcher. 
	 */
   protected void cleanUp()
   {
      super.cleanUp();
      
		this.eventObject = null;
   }

   /**
    * Destroys the event dispatcher.
    */
   protected void destroy()
   {
      super.destroy();
      
		this.eventObject = null;
   }

   /**
    * Sets the event object to be associated with this EventDispatcher. 
    * 
    * @param eventObject the event object to be associated with this EventDispatcher.
    */
   protected void setData(Object eventObject)
   {
		this.eventObject = eventObject;
   }

   /**
    * Perfoms the logic of this EventDispatcher, i.e. dispatches the event to event receivers or executes a Runnable object.  
    */
   protected void work()
   {
		if( this.eventObject instanceof Event )
		{
			this.eventQueue.dispatchEvent((Event)this.eventObject);
		}
		else if( this.eventObject instanceof Runnable )
		{
			((Runnable)this.eventObject).run();
		}
		else
		{
			this.eventQueue.logWarning("Unable to process object: " + this.eventObject + ".");
		}
   }
}
