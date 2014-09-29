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
 * Interface for controlling a queue system with only an in queue.
 * 
 * @see QueueManager
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public interface InQueueController extends QueueController
{
	/**
	 * This method is called during initailization of the QueueManager to notify the QueueController that there are recovered 
	 * QueueItems in the in-queue.
	 */
	public void recoveredItemsInInQueue();
	
	/**
	 * Called to notify the QueueController that the QueueManager was unable to dispatch a response (receipt) for a 
	 * completed QueueItem. 
	 * 
	 * @param inItem a QueueItem that has been put in the in queue.
	 */
	public void unableToDispatchCompletionResponse(QueueItem inItem);
	
	/**
	 * Method to indicate that a new QueueItem has been put in the in-queue of the QueueManager.
	 * 
	 * @param inItem a QueueItem that has been put in the in queue.
	 */
	public void newInItem(QueueItem inItem);
	
	/**
	 * Called when a request to cancel an item in the in-queue is received. This method is called before 
	 * {@link #inItemCancellationRequested(QueueItem)} to indicate if the specified QueueItem can be cancelled. If this 
	 * method returns <code>false</code>, <code>inItemCancellationRequested</code> will not be called and the 
	 * QueueItem will not be cancelled.
	 * 
	 * @return <code>true</code> if the item may be cancelled, otherwise <code>false</code>.
	 */
	public boolean canCancelInItem(QueueItem inItem);
			
	/**
	 * Called when an item in the in-queue must be cancelled due to some external event (cancellation operation called from 
	 * the administration tool for instance). If this QueueController is working with the indicated item it may continue to do 
	 * so, otherwise it should not not attempt to check out the item from the in queue.
	 * 
	 * @param inItem a QueueItem in the in queue.
	 *
	 * @return an object to be returned in the response (receipt) to the sender of the QueueItem. If no such object is available 
	 * this method should return null.
	 */
	public Object inItemCancellationRequested(QueueItem inItem);
	
	/**
	 * Called when a request to relocate an item in the in-queue is received. This method is called before 
	 * {@link #inItemRelocationRequested(QueueItem)} to indicate if the specified QueueItem can be relocated. If this 
	 * method returns <code>false</code>, <code>inItemRelocationRequested</code> will not be called and the 
	 * QueueItem will not be relocated.
	 * 
	 * @return <code>true</code> if the item may be relocated, otherwise <code>false</code>.
	 * 
	 * @since 1.13 Build 603
	 */
	public boolean canRelocateInItem(QueueItem inItem);
	
	/**
	 * Called when an item in the in-queue must be relocated due to some external event (relocate operation called from 
	 * the administration tool for instance). If this QueueController is working with the indicated item it may continue to do 
	 * so, otherwise it should not not attempt to check out the item from the in queue.
	 * 
	 * @param inItem a QueueItem in the in queue.
	 *
	 * @return an object to be returned in the response (receipt) to the sender of the QueueItem. If no such object is available 
	 * this method should return null.
	 */
	public Object inItemRelocationRequested(QueueItem inItem);
	
	/**
	 * This method is called when the QueueManager is unable to send an acknowledgement to the sender of a QueueItem that
	 * the item has been received by this queue system. If this QueueController is working with the indicated item it should discontinue 
	 * immediately.<BR><BR>
	 * Depending on the value of the flag removeAbortedItems in QueueManager the item will be removed 
	 * after the return of this method. The get/set methods for this flag are {@link QueueManager#canRemoveAbortedItems() canRemoveAbortedItems()} 
	 * and {@link QueueManager#setRemoveAbortedItems(boolean removeAbortedItems) setRemoveAbortedItems(boolean removeAbortedItems)}.
	 * 
	 * @param inItem a QueueItem in the in queue.
	 */
	public void inItemAborted(QueueItem inItem);
}
