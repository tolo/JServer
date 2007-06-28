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
 * Interface for controlling a queue system with only an out queue.
 * 
 * @see QueueManager
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public interface OutQueueController extends QueueController
{
	/**
	 * This method is called during initailization of the QueueManager to notify the QueueController that there are recovered 
	 * QueueItems in the out-queue.
	 */
	public void recoveredItemsInOutQueue();
	
	/**
	 * Called when a request to relocate an item in the out queue is received.
	 * 
	 * @param outItem a QueueItem in the out queue.
	 * @param responseData a custom response data object returned with the response, null if there was no such object.
	 */
	public void outItemRelocationRequestReceived(QueueItem outItem, Object responseData);
	
	/**
	 * This method is called by the QueueManager when a cancellation response for a QueueItem in the out queue is received.
	 * 
	 * @param item a QueueItem in the out queue.
	 * @param responseData a custom response data object returned with the response, null if there was no such object.
	 */
	public void outItemDoneCancelled(QueueItem item, Object responseData);
	
	/**
	 * This method is called by the QueueManager when a failure response for a QueueItem in the out queue is received.
	 * 
	 * @param item a QueueItem in the out queue.
	 * @param responseData a custom response data object returned with the response, null if there was no such object.
	 */
	public void outItemDoneFailure(QueueItem item, Object responseData);
	
	/**
	 * This method is called by the QueueManager when a success response for a QueueItem in the out queue is received.
	 *  
	 * @param item a QueueItem in the out queue.
	 * @param responseData a custom response data object returned with the response, null if there was no such object.
	 */
	public void outItemDoneSuccess(QueueItem item, Object responseData);
	
	/**
	 * Method to notify the QueueController that a QueueItem in the out queue has been dispatched.
	 * 
	 * @param item a QueueItem in the out queue.
	 */
	public void outItemDispatched(QueueItem item);
	
	/**
	 * Method to notify the QueueController that an outgoing QueueItem failed to dispatch.
	 * 
	 * @param item a QueueItem in the out queue.
	 */
	public void unableToDispatchOutItem(QueueItem item);
	
	/**
	 * Method to notify the QueueController that an outgoing QueueItem failed to dispatch because the in queue of the 
	 * receiving queue system was full.
	 * 
	 * @param item a QueueItem in the out queue.
	 */
	public void unableToDispatchOutItemQueueFull(QueueItem item);
	
	/**
	 * This method is called when a reply hasn't been received for a dispatched outgoing QueueItem in a certain time. This time is a property i QueueManager and
	 * can be set through the method {@link QueueManager#setOutQueueItemAgeLimit(long) setOutQueueItemAgeLimit(long ageLimit)}.
	 * 
	 * @param item a QueueItem in the out queue.
	 */
	public void outItemAgeWarning(QueueItem item);
}
