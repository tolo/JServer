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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.StatusTransitionException;
import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.pool.PoolThread;
import com.teletalk.jserver.pool.ThreadPool;
import com.teletalk.jserver.property.NumberProperty;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.util.MessageQueue;
import com.teletalk.jserver.util.MessageQueueThread;

/**
 * A class for handling a queue of events. Classes can register themselves as listerners in this class
 * to receive nofification of events.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta
 */
public final class EventQueue extends SubSystem
{
	private final static int DEFAULT_POOL_SIZE = 2; 
	
	private ThreadPool eventDispatcherThreadPool;
	private MessageQueue queue;
	
	private ArrayList allEventListeners;
	private HashMap listeners;
	
	private int rejectedEvents = 0;
	private NumberProperty maxQueueSize;
   
   private SequentialEventDispatcher sequentialEventDispatcher;
   
   /**
    * Constructs a new EventQueue.
    * 
    * @param parent the parent SubSystem.
    */
   public EventQueue(SubSystem parent)
   {
      this(parent, "EventQueue");
   }
	
	/**
	 * Constructs a new EventQueue.
	 * 
	 * @param parent the parent SubSystem.
	 */
	public EventQueue(SubSystem parent, String name)
	{
		super(parent, name);
		
		init();
	}
		
	/**
	 * Constructs a new EventQueue.
	 * 
	 * @param parent the parent SubSystem.
	 * @param maxRestarts maximum allowed restarts before critical failure.
	 * @param restartInterval time in milliseconds between restarts.
	 */
	public EventQueue(SubSystem parent, int maxRestarts, int restartInterval)
	{
		super(parent, "EventQueue", maxRestarts, restartInterval);
		
		init();
	}
	
	/**
    * Initializes this EventQueue.
	 */
	private void init()
	{
		queue = new MessageQueue();
      
      sequentialEventDispatcher = new SequentialEventDispatcher(this);
		
		allEventListeners = new ArrayList();
		listeners = new HashMap();
						
		this.maxQueueSize = new NumberProperty(this, "maxQueueSize", 10000, Property.MODIFIABLE_NO_RESTART);
		this.maxQueueSize.setDescription("The maximum size of the queue that holds event objects that are waiting to be processed. A value of 0 or less means that no maximum limit will be imposed.");
		super.addProperty(this.maxQueueSize);
	}
	
   /**
    * Initializes this EventQueue.
    */
   protected void doInitialize()
   {
      super.doInitialize();
      
      // Attempt to get old property "max queuesize"
      super.initFromConfiguredProperty(this.maxQueueSize, "max queuesize", false, true);
      
      if(queue == null) queue = new MessageQueue();
      
      if(sequentialEventDispatcher == null) sequentialEventDispatcher = new SequentialEventDispatcher(this);
      
      if( !super.isReinitializing() )
      {
         try
         {
            this.eventDispatcherThreadPool = new ThreadPool(this, "EventDispatcherPool", DEFAULT_POOL_SIZE, EventDispatcher.class, new Object[]{this});
            super.addSubComponent(this.eventDispatcherThreadPool, true);
         }
         catch(Exception e)
         {
            throw new StatusTransitionException("Error while initializing!", e);
         }
      }
   }
   
   /**
    * Shuts down this EventQueue.
    */
   protected void doShutDown()
   {
      super.doShutDown();
      
      if( !super.isReinitializing() )
      {
         if( this.eventDispatcherThreadPool != null ) 
         {
            this.eventDispatcherThreadPool.shutDown();
            super.removeSubComponent(this.eventDispatcherThreadPool);
            this.eventDispatcherThreadPool = null;
         }
      }
      
		if(JServer.isServerShuttingDown())
		{
			queue = null;
         this.sequentialEventDispatcher.destroy();
		}
   }
	
	/**
	 * Sets the maximum number of events that can be waiting in the queue 
	 * of this EventQueue. A value of 0 or less means that no maximum limit will be imposed.
	 * 
	 * @param maxEventQueueSize the new maximum value.
	 */
	public void setMaxEventQueueSize(int maxEventQueueSize)
	{
		this.maxQueueSize.setValue(maxEventQueueSize);
	}
	
	/**
	 * Gets the maximum number of events that can be waiting in the queue 
	 * of this EventQueue. A value of 0 or less means that no maximum limit will be imposed.
	 * 
	 * @return the maximum value.
	 */
	public int getMaxEventQueueSize()
	{
		return this.maxQueueSize.intValue();
	}
	
	/**
	 * Registers an EventListener that will receive all types of events.
	 * 
	 * @param listener an EventListener to be registered.
	 */
	public void registerEventListener(final EventListener listener)
	{
		synchronized(allEventListeners)
		{
			if((allEventListeners != null) && !allEventListeners.contains(listener))
         {
				allEventListeners.add(listener);
         }
		}
	}
	
	/**
	 * Registers a PropertyEventListener with this EventQueue.
	 * 
	 * @param listener a PropertyEventListener to be registered.
	 */
	public void registerPropertyEventListener(final PropertyEventListener listener)
	{
		registerEventListener(listener, PropertyEvent.class);
	}
	
	/**
	 * Registers a StatusEventListener with this EventQueue.
	 * 
	 * @param listener a StatusEventListener to be registered.
	 */
	public void registerStatusEventListener(final StatusEventListener listener)
	{
		registerEventListener(listener, StatusEvent.class);
	}
	
	/**
	 * Registers a StructureEventListener with this EventQueue.
	 * 
	 * @param listener a StructureEventListener to be registered.
	 */
	public void registerStructureEventListener(StructureEventListener listener)
	{
		registerEventListener(listener, StructureEvent.class);
	}
	
	/**
	 * Registers an event listener with this EventQueue.
	 * 
	 * @param listener an event listener to be registered.
	 * @param eventClass the event type that the event listener is to be registered for.
	 */
	public void registerEventListener(Object listener, Class eventClass)
	{
		List customEventListeners = null;
		
		synchronized(this.listeners)
		{
			if(listener != null)
			{
				customEventListeners = (List)this.listeners.get(eventClass);
		
				if(customEventListeners == null)
				{
					customEventListeners = new ArrayList();
					this.listeners.put(eventClass, customEventListeners);
				}
			}
		}
      
      synchronized(customEventListeners)
      {
         if(!customEventListeners.contains(listener))
         {
            customEventListeners.add(listener);
         }
      }
	}
	
	/**
	 * Unregisters a PropertyEventListener with this EventQueue.
	 * 
	 * @param listener a PropertyEventListener to be unregistered.
	 */
	public void unregisterPropertyEventListener(PropertyEventListener listener)
	{
		unregisterEventListener(listener, PropertyEvent.class);
	}
	
	/**
	 * Unregisters a StatusEventListener with this EventQueue.
	 * 
	 * @param listener a StatusEventListener to be unregistered.
	 */
	public void unregisterStatusEventListener(StatusEventListener listener)
	{
		unregisterEventListener(listener, StatusEvent.class);
	}
	
	/**
	 * Unregisters a StructureEventListener with this EventQueue.
	 * 
	 * @param listener a StructureEventListener to be unregistered.
	 */
	public void unregisterStructureEventListener(StructureEventListener listener)
	{
		unregisterEventListener(listener, StructureEvent.class);
	}
	
	/**
	 * Unregisters an event listener from a specific type of events.
	 * 
	 * @param listener an event listener to be unregistered.
	 * @param eventClass the event type that the event listener is to be registered for.
	 */
	public void unregisterEventListener(Object listener, Class eventClass)
	{
		List eventClassListeners = null;
		
		synchronized(this.listeners)
		{
			if(listener != null)
			{
				eventClassListeners = (List)this.listeners.get(eventClass);
			}
		}
      
      if(eventClassListeners != null)
      {
         synchronized(eventClassListeners)
         {
            eventClassListeners.remove(listener);
         }
      }
	}
	
	/**
	 * Unregisters an event listener from all the event types it is registered for.
	 * 
	 * @param listener an event listener to be unregistered.
	 */
	public void unregisterEventListener(Object listener)
	{
		if(listener != null)
		{
			Class eventClasses[];
			
			synchronized(this.listeners)
			{
				eventClasses = (Class[])this.listeners.keySet().toArray(new Class[]{});
			}
			
			for(int i=0; i<eventClasses.length; i++)
			{
				unregisterEventListener(listener, eventClasses[i]);
			}
		}
	}
	
	/**
	 * Unregisters an EventListener that is listening to all types of events.
	 * 
	 * @param listener an EventListener to be unregistered.
	 */
	public void unregisterEventListener(EventListener listener)
	{
		synchronized(allEventListeners)
		{
			if(allEventListeners != null)
			{
				allEventListeners.remove(listener);
			}
		}
	}
	
	/**
	 * Places an event in the queue of this EventQueue.
	 * 
	 * @param event an {@link Event} or <code>java.lang.Runnable<code> object to be queued.
    * 
    * @return <code>true</code> if the event was successfully queued, otherwise <code>false</code>.
	 */
	public boolean queueEvent(final Object event)
	{
		final int currentStatus = getStatus();
		
		if( (currentStatus != SHUTTING_DOWN) && (currentStatus != DOWN) && (currentStatus != CRITICAL_ERROR) && (queue != null) )
		{
         int maxQueueSizeTmp = maxQueueSize.intValue();
			if( (maxQueueSizeTmp <= 0) || (queue.size() < maxQueueSizeTmp) )
			{
				if(rejectedEvents > 0)
				{
					logError("Messagequeue overflow! " + rejectedEvents + " events rejected!");
					rejectedEvents = 0;
				}
            				
				queue.putMsg(event);
            
            return true;
			}
			else
			{
				rejectedEvents++;
			}
		}
      
      return false;
	}
	
	/**
	 * Dispatches an event to registered listeners.
	 * 
	 * @param event the event to be dispatched.
	 */
	protected void dispatchEvent(Event event)
	{
		Object eventListener;

		try
		{
			List eventClassListenersList;
			
			synchronized(this.listeners)
			{
				eventClassListenersList = (List)listeners.get(event.getEventClass());
			}
			
			if(eventClassListenersList != null)
			{
				final Object[] eventClassListeners;
				
				synchronized(eventClassListenersList)
				{
					eventClassListeners= eventClassListenersList.toArray();
				}
				
				for(int i=0; i<eventClassListeners.length; i++)
				{
					eventListener = eventClassListeners[i]; 
					
					event.notifyListener(eventListener);
				}
			}
			
			final Object[] allEventClassListeners;
			
			synchronized(allEventListeners)
			{
				allEventClassListeners = allEventListeners.toArray();
			}
			
			for(int i=0; i<allEventClassListeners.length; i++)
			{
				eventListener = allEventClassListeners[i]; 
				
				if(eventListener instanceof EventListener)
				{
					((EventListener)eventListener).handleEvent(event);
				}
			}
		}
		catch(Exception e)
		{
			logError("Error while notifying listeners (event: " + event + ")" , e);
		}
		finally
		{
			event = null;	
		}
	}
	
   /**
    * The thread method of this EventQueue. Dispatches events from the queue.
    */
   public void run()
   {
      Object event = null;
      PoolThread poolThread = null;
      
      while( canRun )
      {
         try
         {
            event = queue.getMsg();
            
            if( event != null )
            {
               if( (event instanceof Event) && ((Event)event).isSequential() )
               {
                  this.sequentialEventDispatcher.queueMessage((Event)event);
               }
               else
               {
                  // Wait no more than one second for an event dispatcher thread to become available...
                  poolThread = (PoolThread)this.eventDispatcherThreadPool.checkOutWait(1000);
                  
                  if( poolThread != null )
                  {
                     poolThread.initialize(event);
                  }
                  else // ...but if no thread is available - execute in a new one
                  {
                     this.eventDispatcherThreadPool.initializeThread(event);
                  }
               }
            }
         }
         catch(InterruptedException e){ continue; }
      }
   }
   
   /**
    * Internal thread class for handling sequential events.
    */
   private static final class SequentialEventDispatcher extends MessageQueueThread
   {
      private final EventQueue eventQueue;
      
      /**
       * Creates a new SequentialEventDispatcher.
       */
      public SequentialEventDispatcher(final EventQueue eventQueue)
      {
         super(eventQueue.getFullName() + ".InternalEventDispatcher");
         
         this.eventQueue = eventQueue;
      }
      
      /**
       * Handles a queued event. 
       */
      protected void handleMessage(Object message)
      {
         this.eventQueue.dispatchEvent((Event)message);
      }
   }
}
