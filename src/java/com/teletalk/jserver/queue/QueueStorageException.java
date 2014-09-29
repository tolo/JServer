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
 * Thrown when an error occurs while adding a QueueItem to a Queue or its associated QueueStorage object..
 * 
 * @see com.teletalk.jserver.queue.Queue
 * @see com.teletalk.jserver.queue.QueueStorage
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public final class QueueStorageException extends QueueException
{
   static final long serialVersionUID = -2776284226310609236L;

	/**
	 * Createas a new QueueStorageException without any message.
	 */
	public QueueStorageException()
	{
		super();
	}
	
	/**
	 * Createas a new QueueStorageException.
	 * 
	 * @param msg the message of the exception.
	 */
	public QueueStorageException(String msg)
	{
		super(msg);
	}
	
	/**
	 * Createas a new QueueStorageException.
	 * 
	 * @param msg the message of the exception.
	 * @param error an exception that should be included in the message of this QueueException.
	 */
	public QueueStorageException(String msg, Exception error)
	{
		super(msg, error);
	}
}
