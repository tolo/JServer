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
package com.teletalk.jserver;

import java.io.PrintWriter;
import java.io.StringWriter;

import com.teletalk.jserver.load.LoadManager;

/**
 * The TopThreadGroup class is a support class to JServer that detects errors in SubSystems and other threaded 
 * parts of the server.<br>
 * <br>
 * All subclasses of {@link SubComponent} (which of course includes SubSystem subclasses) can use the method {@link SubComponent#getThreadGroup() getThreadGroup()} to get
 * a reference to the JServer's instance of this class.
 * 
 * @see JServer
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class TopThreadGroup extends ThreadGroup
{
	private final JServer topsystem;
	private final String fullName;
	
	private boolean outOfMemoryErrorAlreadyHandled = false;
	
	/**
	 * Constructs a new TopThreadGroup object.
	 * 
	 * @param topsystem a JServer object.
	 * @param name the name of this TopThreadGroup.
	 */
	public TopThreadGroup(JServer topsystem, String name)
	{
		super(name);
		
		this.topsystem = topsystem;
		this.fullName = topsystem.getName() + ".TopThreadGroup";
	}
	
	/**
	 * Called by the Java Virtual Machine when a thread in this thread group stops because of an uncaught exception.
	 * 
	 * @param thread the thread that is about to exit.
	 * @param exception the uncaught exception.
	 */
	public void uncaughtException(final Thread thread, final Throwable exception)
	{
		try
		{
		   // Unregister thread as load thread in case it was previously registered
		   LoadManager.unregisterLoadThread(thread);
		   
			if(exception instanceof ThreadDeath) // When Thread.stop is invoked
			{
			   super.uncaughtException(thread, exception);
			}
			else
			{
				SubComponent component;
				String threadName = thread.getName();
				StringWriter strWriter = new StringWriter();
				exception.printStackTrace(new PrintWriter(strWriter));
				
				// Handle out of memory error
				if(exception instanceof OutOfMemoryError)
				{
					if(topsystem.getKillProcessOnOutOfMemoryError())
					{
						boolean shutDownServer= true;
						
						try
						{
							synchronized(this)
							{
								if(!outOfMemoryErrorAlreadyHandled)
								{
									outOfMemoryErrorAlreadyHandled = true;
									JServerUtilities.logCriticalError(this.fullName, "OutOfMemoryError detected in " + threadName + "! Shutting down server! Stack trace: " + strWriter.toString() + ".", JServerConstants.LOG_MESSAGE_ID_OUT_OF_MEMORY_ERROR);
									System.out.println("OutOfMemoryError detected in " + threadName + "! Shutting down server! Stack trace: " + strWriter.toString() + ".");
								}
								else
								{
									shutDownServer = false;
									JServerUtilities.logError(this.fullName, "OutOfMemoryError detected in " + threadName + "! Server shut down in progress! Stack trace: " + strWriter.toString() + ".");
									System.out.println("OutOfMemoryError detected in " + threadName + "! Server shut down in progress! Stack trace: " + strWriter.toString() + ".");
								}
							}
												
							if(shutDownServer)
							{
								// Stop server
								this.topsystem.stopJServer();
							}
							else
							{
								// Don't proceed with handling of error since the server is going to shut down anyway. Instead, destroy component.
								component = topsystem.findSubComponent(threadName);
						
								if(component != null)
								{
									component.destroy();
								}
								return;
							}
						}
						finally
						{
							// Exit process
							if(shutDownServer) System.exit(1);
						}
					}
					else
					{
						JServerUtilities.logCriticalError(this.fullName, "OutOfMemoryError detected in " + threadName + "! Stack trace: " + strWriter.toString() + ".", JServerConstants.LOG_MESSAGE_ID_OUT_OF_MEMORY_ERROR);
						System.out.println("OutOfMemoryError detected in " + threadName + "! Stack trace: " + strWriter.toString() + ".");
					}
				}
				
				// Handle sub system error...
				if(thread.getName().equals(topsystem.getName()))
				{
					// ...main system error
					topsystem.handleMainThreadError(exception);
				}
				else
				{
					// ...sub system error
					
					if(threadName.endsWith("." + StatusTransitionTask.StatusTransitionTaskThreadName))
					{
						component = topsystem.findSubComponent(threadName.substring(0, threadName.length() - ("." + StatusTransitionTask.StatusTransitionTaskThreadName).length()));
						
						if(component != null)
						{
							JServerUtilities.logError(this.fullName, "Error(" + exception + ") detected in StateTransitionTask for component " + component.getFullName() + ". Stack trace: " + strWriter.toString() + ".");
							System.out.println("Error(" + exception + ") detected in StateTransitionTask for component " + component.getFullName() + ". Stack trace: " + strWriter.toString() + ".");
						}			
						else
						{
							JServerUtilities.logError(this.fullName, "Error(" + exception + ") detected in StateTransitionTask " + threadName + ", but no matching component found! Stack trace: " + strWriter.toString());
							System.out.println("Error(" + exception + ") detected in StateTransitionTask " + threadName + ", but no matching component found! Stack trace: " + strWriter.toString());
						}
					}
					else
					{
						component = topsystem.findSubSystem(threadName);
						
						if(component != null)
						{
							JServerUtilities.logError(this.fullName, "Error(" + exception + ") detected in component " + component.getFullName() + ". Stack trace: " + strWriter.toString() + ".");
							System.out.println("Error(" + exception + ") detected in component " + component.getFullName() + ". Stack trace: " + strWriter.toString() + ".");
							component.error(strWriter.toString());
						}
						else 
						{
							JServerUtilities.logError(this.fullName, "Error(" + exception + ") detected in thread " + threadName + ". Stack trace: " + strWriter.toString());
							System.out.println("Error(" + exception + ") detected in thread " + threadName + ". Stack trace: " + strWriter.toString());
						}
					}
				}
			}
		}
		catch(Throwable t)
		{
			if(t instanceof ThreadDeath)
			{
				super.uncaughtException(thread, exception);
			}
			else
			{
				try
				{
					String threadName = thread.getName();
					StringWriter strWriter = new StringWriter();
					exception.printStackTrace(new PrintWriter(strWriter));
					
					JServerUtilities.logError(this.fullName, "Error occurred while handling uncaught exception (thread: " + threadName + ", exception stack trace :" + strWriter.toString() + ").", t);
					System.out.println("Error occurred while handling uncaught exception (thread: " + threadName + ", exception stack trace :" + strWriter.toString() + ").");
					t.printStackTrace();
				}
				catch(Throwable t2){}
			}
		}
	}
}
