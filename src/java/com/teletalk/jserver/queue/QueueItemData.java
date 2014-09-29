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

import java.io.Serializable;

/**
 * Interface that must be implemented by data objects contained in QueueItems..
 * 
 * @see QueueItem
 * @see Queue
 *   
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public interface QueueItemData extends Serializable
{
	/**
	 * Gets a description of this QueueItemData.
	 * 
	 * @return a description of this QueueItemData.
	 */
	public String getDescription();
}
