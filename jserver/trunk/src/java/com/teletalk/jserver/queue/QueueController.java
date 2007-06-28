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
package com.teletalk.jserver.queue;

import com.teletalk.jserver.queue.command.QueueControllerCommand;

/**
 * Base interface for all interfaces used to operate a queue system and receive notifications from it. The subinterfaces of this interface hooks 
 * into the QueueManager class, which is the main class of the queue system.  
 * 
 * @see InQueueController
 * @see OutQueueController
 * @see InOutQueueController
 * @see QueueManager
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public interface QueueController
{
	/**
	 * This method is called when a custom command is received by the queue system.
	 * 
	 * @param command the received command.
	 */
	public void queueControllerCommandReceived(QueueControllerCommand command);
	
	/**
	 * This method is called when the QueueManager is unable to deliver a custom message.
	 * 
	 * @param command the command that the QueueManager was unable to deliver.
	 */
	public void unableToDeliverControllerCommand(QueueControllerCommand command);
	
	/**
	 * This method is called when a custom (user defined) operation is called on one of the queues (the in- or outqueue).
	 * 
	 * @param operationName the name of the operation that was called.
	 * @param ids indices of items in a queue for which the operation was called.
	 */
	public void customExternalQueueOperationCalled(String operationName, String[] ids);
	
	/**
	 * Signals that the link to a remote queue system has been lost. This method will be called only after attempts to reconnect have failed.
	 * 
	 * @param remoteQueueSystem a remote queue system.
	 */
	public void linkToRemoteQueueSystemLost(RemoteQueueSystem remoteQueueSystem);
}
