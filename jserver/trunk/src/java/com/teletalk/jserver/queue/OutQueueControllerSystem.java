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

import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.queue.command.QueueControllerCommand;

/**
 * An abstract SubSystem class that implements the OutQueueController interface. This class creates a QueueManager 
 * object that has only an out queue and specifies itself as the controller. The created QueueManager is then added as a subsystem to 
 * this object.<BR><BR>
 * This class provides dummy implementations of all the methods in the OutQueueController interface and subclasses need only 
 * implement the ones that are actually needed for a specific purpose.
 *  
 * @see OutQueueController
 * @see QueueManager
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public abstract class OutQueueControllerSystem extends QueueControllerSystem implements OutQueueController
{
	/**
	 * Creates a OutQueueControllerSystem with a QueueManager equipped with an out-queue.
	 * 
	 * @param parent the parent of this OutQueueControllerSystem.
	 * @param name the name of this OutQueueControllerSystem.
	 */
	protected OutQueueControllerSystem(SubSystem parent, String name)
	{
		super(parent, name);
		
      queueManager.createOutQueue();
	}
	
	/**
	 * Creates a OutQueueControllerSystem with a QueueManager equipped with the specified out-queue. The QueueManager will assume ownership of the specified queue, 
	 * and for that reason it is recommended that it is created using a constructor that doensn't take a parent as a parameter.
	 * 
	 * @param parent the parent of this OutQueueControllerSystem.
	 * @param name the name of this OutQueueControllerSystem.
	 * @param outQueue the out queue to be used in the QueueManager.
	 * @param addDefaultExternalQueueOperations flag indicating if the default external operation (cancel) should be added to the out queue.
	 */
	protected OutQueueControllerSystem(SubSystem parent, String name, Queue outQueue, boolean addDefaultExternalQueueOperations)
	{
		super(parent, name);
		
      if( outQueue != null ) queueManager.setOutQueue(outQueue, addDefaultExternalQueueOperations);
      else queueManager.createOutQueue(addDefaultExternalQueueOperations);
	}
	
	
	public void recoveredItemsInOutQueue()
	{
	}
	
	public void outItemRelocationRequestReceived(QueueItem outItem, Object responseData)
	{
	}
	
	public void outItemDoneCancelled(QueueItem item, Object responseData)
	{
	}
	
	public void outItemDoneFailure(QueueItem item, Object responseData)
	{
	}
	
	public void outItemDoneSuccess(QueueItem item, Object responseData)
	{
	}
	
	public void outItemDispatched(QueueItem item)
	{
	}
	
	public void unableToDispatchOutItem(QueueItem item)
	{
	}
	
	public void unableToDispatchOutItemQueueFull(QueueItem item)
	{
	}
	
	public void outItemAgeWarning(QueueItem item)
	{
	}
	
	public void queueControllerCommandReceived(QueueControllerCommand command)
	{
	}
	
	public void unableToDeliverControllerCommand(QueueControllerCommand command)
	{
	}
	
	public void customExternalQueueOperationCalled(String operationName, String[] ids)
	{
	}
	
	public void linkToRemoteQueueSystemLost(RemoteQueueSystem remoteQueueSystem)
	{
	}
}
