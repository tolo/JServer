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
package com.teletalk.jserver.queue.command;

import com.teletalk.jserver.comm.EndPointIdentifier;
import com.teletalk.jserver.queue.QueueManager;

/**
 * Base command class for custom commands that are to be used for communication between coltrollers of 
 * queue systems. Custom commands are dispatched using the method {@link QueueManager#dispatchCommand(QueueSystemCommand)} 
 * in the QueueManager class. When a custom command is received by the queue system it is handled by calling the method 
 * {@link com.teletalk.jserver.queue.QueueController#queueControllerCommandReceived(QueueControllerCommand)} in the controller.
 *    
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public abstract class QueueControllerCommand extends QueueSystemCommand
{
	/** Default no-arg constructor used during  deserialization.	 */
	public QueueControllerCommand(){}
	
	/**
	 * Creates a new QueueControllerCommand.
	 * 
	 * @param address the address of the received of this command.
	 */
	public QueueControllerCommand(final EndPointIdentifier address)
	{
		super(address);	
	}
	
	public final void execute(QueueManager queueManager)
	{
		queueManager.queueControllerCommandReceived(this);
	}
	
	public final void abort(QueueManager queueManager)
	{
		queueManager.queueControllerCommandAborted(this);
	}
}
