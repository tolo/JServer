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

import java.util.ArrayList;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.JServerUtilities;

/**
 * Class providing a timer function.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class Timer implements Runnable
{
	private static final TimerListener[] timerListenerArrayType = new TimerListener[]{};
	private static final MultiTimerListener[] multiTimerListenerArrayType = new MultiTimerListener[]{};
	
	private volatile boolean canRun = false;

	private final ArrayList timerListeners;
	private final ArrayList multiTimerListeners;
		
	private String name;
	private Thread thread;
	
	private volatile long timerInterval;
	
	private volatile boolean paused = false;
	
	/**
	 * Constructs a new Timer with a given interval between ticks and starts it.
	 * 
	 * @param timerInterval in milliseconds interval between ticks.
	 */
	public Timer(long timerInterval)
	{
		this(timerInterval, true);
	}
	
	/**
	 * Constructs a new Timer with a given interval between ticks.
	 * 
	 * @param timerInterval in milliseconds interval between ticks.
	 * @param startTimer boolean flag indicating if the timer should be started immediately.
	 */
	public Timer(long timerInterval, boolean startTimer)
	{
		this(timerInterval, null, startTimer);
	}
	
	/**
	 * Constructs a new Timer with a given interval between ticks and adds a TimerListener to it. 
	 * The Timer will also be started.
	 * 
	 * @param listener TimerListener which will receive notifications of timer ticks.
	 * @param timerInterval in milliseconds interval between ticks.
	 */
	public Timer(TimerListener listener, long timerInterval)
	{
		this(listener, timerInterval, true);
	}
	
	/**
	 * Constructs a new Timer with a given interval between ticks and adds a TimerListener to it. 
	 * 
	 * @param listener TimerListener which will receive notifications of timer ticks.
	 * @param timerInterval in milliseconds interval between ticks.
	 * @param startTimer boolean flag indicating if the timer should be started immediately.
	 */
	public Timer(TimerListener listener, long timerInterval, boolean startTimer)
	{
		this(timerInterval, startTimer);

		this.registerTimerListener(listener);
	}
	
	/**
	 * Constructs a new Timer with a given interval between ticks and adds a MultiTimerListener to it. 
	 * The Timer will also be started.
	 * 
	 * @param listener MultiTimerListener which will receive notifications of timer ticks.
	 * @param timerInterval in milliseconds interval between ticks.
	 */
	public Timer(MultiTimerListener listener, long timerInterval)
	{
		this(listener, timerInterval, true);
	}
	
	/**
	 * Constructs a new Timer with a given interval between ticks and adds a MultiTimerListener to it. 
	 * 
	 * @param listener MultiTimerListener which will receive notifications of timer ticks.
	 * @param timerInterval in milliseconds interval between ticks.
	 * @param startTimer boolean flag indicating if the timer should be started immediately.
	 */
	public Timer(MultiTimerListener listener, long timerInterval, boolean startTimer)
	{
		this(timerInterval, startTimer);

		this.registerTimerListener(listener);
	}
	
	/**
	 * Constructs a new Timer with a given interval between ticks and starts it.
	 * 
	 * @param timerInterval in milliseconds interval between ticks.
	 * @param name the name of this Timer.
	 */
	public Timer(long timerInterval, String name)
	{
		this(timerInterval, name, true);
	}
	
	/**
	 * Constructs a new Timer with a given interval between ticks.
	 * 
	 * @param timerInterval in milliseconds interval between ticks.
	 * @param name the name of this Timer.
	 * @param startTimer boolean flag indicating if the timer should be started immediately.
	 */
	public Timer(long timerInterval, String name, boolean startTimer)
	{
		this.timerInterval = timerInterval;

		timerListeners = new ArrayList();
		multiTimerListeners = new ArrayList();
		
		if(this.name != null)
			this.name = name;
		else
			this.name = "Timer" + Integer.toHexString(System.identityHashCode(this));
		
		canRun = true;
		if(startTimer) start();
	}
	
	/**
	 * Constructs a new Timer with a given interval between ticks and adds a TimerListener to it. 
	 * The Timer will also be started.
	 * 
	 * @param listener TimerListener which will receive notifications of timer ticks.
	 * @param timerInterval in milliseconds interval between ticks.
	 * @param name the name of this Timer.
	 */
	public Timer(TimerListener listener, long timerInterval, String name)
	{
		this(listener, timerInterval, name, true);
	}
	
	/**
	 * Constructs a new Timer with a given interval between ticks and adds a TimerListener to it. 

	 * @param listener TimerListener which will receive notifications of timer ticks.
	 * @param timerInterval in milliseconds interval between ticks.
	 * @param name the name of this Timer.
	 * @param startTimer boolean flag indicating if the timer should be started immediately.
	 */
	public Timer(TimerListener listener, long timerInterval, String name, boolean startTimer)
	{
		this(timerInterval, name, startTimer);

		this.registerTimerListener(listener);
	}
	
	/**
	 * Constructs a new Timer with a given interval between ticks and adds a MultiTimerListener to it. 
	 * The Timer will also be started.
	 * 
	 * @param listener MultiTimerListener which will receive notifications of timer ticks.
	 * @param timerInterval in milliseconds interval between ticks.
	 * @param name the name of this Timer.
	 */
	public Timer(MultiTimerListener listener, long timerInterval, String name)
	{
		this(listener, timerInterval, name, true);
	}
	
	/**
	 * Constructs a new Timer with a given interval between ticks and adds a MultiTimerListener to it. 
	 * 
	 * @param listener MultiTimerListener which will receive notifications of timer ticks.
	 * @param timerInterval in milliseconds interval between ticks.
	 * @param name the name of this Timer.
	 * @param startTimer boolean flag indicating if the timer should be started immediately.
	 */
	public Timer(MultiTimerListener listener, long timerInterval, String name, boolean startTimer)
	{
		this(timerInterval, name, startTimer);

		this.registerTimerListener(listener);
	}
	
	/**
	 * Gets the currentl interval at which this timer "ticks".
	 * 
	 * @return the interval in milliseconds.
	 */
	public long getTimerTickInterval()
	{
		return this.timerInterval;
	}
	
	/**
	 * Sets the currentl interval at which this timer is to "tick".
	 * 
	 * @param newInterval the new interval in milliseconds.
	 */
	public void setTimerTickInterval(long newInterval)
	{
		this.timerInterval = newInterval;
		if( (this.thread != null) && canRun ) this.thread.interrupt();
	}
	
	/**
	 */
	public void setName(String name)
	{
		this.name = name;
	}
	
	/**
	 * Gets the name of this Timer.
	 * 
	 * @return the name of this Timer.
	 */
	public String getName()
	{
		return this.name;
	}
	
	/**
	 * Returns a string representation of this Timer, which includes the name of the timer.
	 * 
	 * @return a string representation.
	 */	
	public String toString()
	{
		return this.getName();
	}
	
	/**
	 * Restarts this timer.
	 */
	public void restart()
	{
		kill();
		start();
	}
		
	/**
	 * Starts this timer.
	 */
	public void start()
	{
		if( (thread == null) ||  !thread.isAlive() )
		{
			canRun = true;
         JServer jServer = JServer.getJServer();
			
			if( (jServer != null) && (jServer.getThreadGroup() != null) )
         {
				thread = new Thread(jServer.getThreadGroup(), this, name);
         }
			else
         {
				thread = new Thread(this, name);
         }
			thread.setDaemon(true);
			thread.start();
		}
	}
		
	/**
	 * Stops this timer.
	 */
	public void kill()
	{
		canRun = false;
			
		if(thread != null) thread.interrupt();
		thread = null;

		synchronized(this)
		{
			notifyAll();
		}
	}
	
	/**
	 */
	public void pause()
	{
		paused = true;
	}
	
	/**
	 */
	public synchronized void resume()
	{
		paused = false;
		this.notify();
	}
		
	/**
	 * Checkes if this timer is alive.
	 * 
	 * @return true if this Timer is alive, otherwise false.
	 */
	public boolean check()
	{
		return canRun && (thread != null) && (thread.isAlive());
	}
	
	/**
	 * Registers a TimerListener which will receive notifications of timer ticks.
	 * 
	 * @param listener the TimerListener to be registered.
	 */
	public synchronized void registerTimerListener(TimerListener listener)
	{
		if(!timerListeners.contains(listener))
			timerListeners.add(listener);
	}
	
	/**
	 * Registers a MultiTimerListener which will receive notifications of timer ticks.
	 * 
	 * @param listener the MultiTimerListener to be registered.
	 */
	public synchronized void registerTimerListener(MultiTimerListener listener)
	{
		if(!multiTimerListeners.contains(listener))
			multiTimerListeners.add(listener);
	}
	
	/**
	 * Unregisters a TimerListener.
	 * 
	 * @param listener the TimerListener to be unregistered.
	 */
	public synchronized void unregisterTimerListener(TimerListener listener)
	{
		timerListeners.remove(listener);
	}
	
	/**
	 * Unregisters a MultiTimerListener.
	 * 
	 * @param listener the TimerListener to be unregistered.
	 */
	public synchronized void unregisterTimerListener(MultiTimerListener listener)
	{
		multiTimerListeners.remove(listener);
	}
	
	/**
	 * The thread method of this Timer. Notifies listeners of timer ticks.
	 */
	public void run()
	{
		long nextTimeTick = System.currentTimeMillis() + timerInterval;
		long sleepTime = 1;
		
		while(canRun)
		{
			try
			{
				while(paused)
				{
					synchronized(this)
					{
						this.wait();
					}
				}
				
				sleepTime = nextTimeTick - System.currentTimeMillis();
				if(sleepTime <= 0) sleepTime = 1;
				Thread.sleep(sleepTime);
			}
			catch(InterruptedException e)
			{
				if(!canRun) break;
				else 
				{
					// Calculate the next time the timer should "tick"
					nextTimeTick = System.currentTimeMillis() + timerInterval;
					continue;
				}
			}
			
			// Calculate the next time the timer should "tick"
			nextTimeTick = System.currentTimeMillis() + timerInterval;
			
			TimerListener[] timerListenerArray;
			MultiTimerListener[] multiTimerListenerArray;
			
			synchronized(this)
			{
				timerListenerArray = (TimerListener[])timerListeners.toArray(timerListenerArrayType);
				multiTimerListenerArray = (MultiTimerListener[])multiTimerListeners.toArray(multiTimerListenerArrayType);
			}
									
			for(int i=0; canRun && (i<timerListenerArray.length); i++)
			{
				try
				{
					timerListenerArray[i].timerTick();
				}
				catch(Exception e)
				{
					JServerUtilities.logError(name, "Got exception while notifying TimerListener (" + timerListenerArray[i] + ")" , e);
				}
			}
			
			for(int i=0; canRun && (i<multiTimerListenerArray.length); i++)
			{
				try
				{
					multiTimerListenerArray[i].timerTick(this);
				}
				catch(Exception e)
				{
					JServerUtilities.logError(name, "Got exception while notifying TimerListener (" + multiTimerListenerArray[i] + ")" , e);
				}
			}
		}
	}
}
