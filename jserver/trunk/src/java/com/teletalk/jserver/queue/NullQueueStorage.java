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
 * This implementation of the QueueStorage interface makes it possible to create Queue objects without persistent storage. 
 *   
 * @see com.teletalk.jserver.queue.Queue
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public final class NullQueueStorage implements QueueStorage
{
	/**
	 * Adds a QueueItem to persistant storage. This implementation does nothing.
	 * 
	 * @param item the QueueItem to be stored.
	 * 
	 * @exception QueueStorageException if an error occured during the store operation.
	 */
	public void storeQueueItem(QueueItem item) throws QueueStorageException
	{
	}
	
	/**
	 * Adds several QueueItems to persistent storage. This implementation does nothing.
	 * 
	 * @param items the QueueItems to be stored.
	 * 
	 * @exception QueueStorageException if an error occured during the store operation.
	 */
	public void storeQueueItems(QueueItem[] items) throws QueueStorageException
	{
	}
			
	/**
	 * Updates the persistent state of a previously stored QueueItem.
	 * 
	 * @param item the QueueItem to be updated.
	 * 
	 * @exception QueueStorageException if an error occured during the update operation.
	 */
	public void updateStoredQueueItem(QueueItem item)  throws QueueStorageException
	{
	}
	
	/**
	 * Method to reflect a change in the state (status value) of a QueueItem object on it's persistent counterpart. 
	 * This implementation does nothing.
	 * 
	 * @param item the QueueItem to be stored.
	 * 
	 * @exception QueueStorageException if an error occured during the store operation.
	 */
	public void updateQueueItemStatus(QueueItem item) throws QueueStorageException
	{
	}
		
	/**
	 * Removes a QueueItem from persistant storage. This implementation does nothing.
	 * 
	 * @param item the QueueItem to be removed from persistent storage.
	 */
	public void removeStoredQueueItem(QueueItem item)
	{
	}
			
	/**
	 * Restores all stored QueueItem objects. This implementation does nothing.
	 * 
	 * @return a vector containing QueueItem objects restored from persistent storage.
	 * 
	 * @exception QueueStorageException if an error occured during restoration of the stored QueueItem objects.
	 */
	public java.util.List restoreQueueFromStorage() throws QueueStorageException
	{
		return new java.util.Vector();
	}
}