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

/**
 * Interface used by owners of queues.
 * 
 * @see Queue
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public interface QueueOwner
{
	/**
	 * This methd is called when a external operation is called in the associated queue.
	 * 
	 * @param operationName the name of the operation that was called.
	 * @param keys indices of items in a queue for which the operation was called.
	 */
	public void externalQueueOperationCalled(String operationName, String[] keys);
}
