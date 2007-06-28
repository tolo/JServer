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

import java.util.ArrayList;

import com.teletalk.jserver.queue.command.QueueItemCancellationRequest;
import com.teletalk.jserver.queue.command.QueueItemCompletionResponse;
import com.teletalk.jserver.queue.command.QueueItemQuery;
import com.teletalk.jserver.queue.command.QueueItemRelocationRequest;
import com.teletalk.jserver.queue.command.QueueItemTransferRequest;
import com.teletalk.jserver.queue.command.QueueSystemSynchronizationRequest;
import com.teletalk.jserver.queue.command.QueueSystemSynchronizationResponse;

/**
 * Out queue mode implementation of methods in QueueManager.
 */
final class QueueManagerOutQueueImpl extends QueueManagerImplBase
{
	/**
	 * Creates a new QueueManagerOutQueueImpl.
	 */
	public QueueManagerOutQueueImpl(QueueManager queueManager, OutQueueController outController)
	{
		super(queueManager, outController);
	}
	
	/**
	 * performInQueueCheck
	 */
	public ArrayList performInQueueCheck()
	{
		return new ArrayList();
	}

	/**
	 * queueSystemSynchronizationRequestReceived
	 */
	public QueueSystemSynchronizationResponse queueSystemSynchronizationRequestReceived(RemoteQueueSystem rqs, QueueSystemSynchronizationRequest synchRequest)
	{
		return new QueueSystemSynchronizationResponse();
	}
	
	/**
	 * queueSystemSynchronizationResponseReceived
	 */
	public void queueSystemSynchronizationResponseReceived(RemoteQueueSystem rqs, QueueSystemSynchronizationResponse synchResponse)
	{
		super.queueSystemSynchronizationResponseReceived(rqs, synchResponse);
	}
		
	/**
	 * recoveredItemsInInQueue
	 */
	public void recoveredItemsInInQueue()
	{
		// NULL IMPLEMENTATION
	}
		
	/**
	 * isInItemCompleted
	 */
	public boolean isInItemCompleted(QueueItem inItem)
	{
		super.queueManager.logError("Method isInItemCompleted(QueueItem inItem) not available in out-queue mode!");
		return false;
	}
		
	/**
	 * inItemDone
	 */
	public void inItemDone(QueueItem inItem, byte responseType, Object responseData)
	{
		super.queueManager.logError("Method inItemDone(QueueItem inItem, byte responseType, Object responseData) not available in out-queue mode!");
	}
		
	/**
	 * cancelInItem
	 */
	public void cancelInItem(QueueItem inItem)
	{
		super.queueManager.logError("Method cancelInItem(QueueItem inItem) not available in out-queue mode!");
	}
		
	/**
	 * relocateInItem
	 */
	public void relocateInItem(QueueItem inItem)
	{
		super.queueManager.logError("Method relocateInItem(QueueItem inItem) not available in out-queue mode!");
	}
		
	/**
	 * queueItemTransferRequestReceived
	 */
	public void queueItemTransferRequestReceived(QueueItemTransferRequest request) 
	{
		super.queueManager.logError("Method relocateInItem(QueueItem inItem) not available in out-queue mode!");
	}
		
	/**
	 * queueItemTransferResponseAborted
	 */
	public void queueItemTransferResponseAborted(final String outItemId, final QueueItem item)
	{
		super.queueManager.logError("Method queueItemTransferResponseAborted(String outItemId, QueueItem item) not available in out-queue mode!");
	}
		
	/**
	 * Method to handle an incoming QueueItemQuery command.
	 * 
	 * @param query a QueueItemQuery.
	 */
	public void queueItemQueryReceived(QueueItemQuery query)
	{
		super.queueManager.logError("Method queueItemQueryReceived(QueueItemQuery query) not available in out-queue mode!");
	}

	/**
	 * queueItemCompletionResponseAborted
	 */
	public void queueItemCompletionResponseAborted(QueueItemCompletionResponse command)
	{
		super.queueManager.logError("Method queueItemCompletionResponseAborted(QueueItemCompletionResponse command) not available in out-queue mode!");
	}
	
	/**
	 * queueItemCancellationRequest
	 */
	public void queueItemCancellationRequest(QueueItemCancellationRequest request) 
	{
		super.queueManager.logError("Method queueItemCancellationRequest(QueueItemCancellationRequest request) not available in out-queue mode!");
	}
		
	/**
	 * queueItemRelocationRequest
	 */
	public void queueItemRelocationRequest(QueueItemRelocationRequest request)
	{
		super.queueManager.logError("Method queueItemRelocationRequest(QueueItemRelocationRequest request) not available in out-queue mode!");
	}
}
