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

/**
 * Interface for controlling a queue system with both an in and an out queue.
 * 
 * @see QueueManager
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public interface InOutQueueController extends InQueueController, OutQueueController
{
	/**
	 * Called when all outgoing items belonging to a certain incoming item are done. Depending on the value of the flag 
	 * autoSendInItemResponses in QueueManager a response will automatically be sent to the indicated in queue item after the 
	 * return of this method. The value of the flag is set through a constructor parameter when 
	 * {@link QueueManager#QueueManager(SubSystem, InOutQueueController, boolean) creating a QueueManager}. If InOutQueueControllerSystem 
	 * is used the flag can also be set from the {@link InOutQueueControllerSystem#InOutQueueControllerSystem(SubSystem, String, boolean) constructor} 
	 * of that class.
	 * 
	 * @param inItem a QueueItem in the in queue.
	 * 
	 * @return the return value of this method is interpreted as optional data to be returned in an response (receipt) to 
	 * the sender of the in-queue item. If no response data is available or if the autoSendInItemResponses flag is set to <code>false</code> 
	 * this method should return null.
	 * 
	 * @see QueueManager
	 * @see InOutQueueControllerSystem
	 */
	public Object allChildrenCompleted(QueueItem inItem);
}
