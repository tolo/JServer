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
package com.teletalk.jserver.tcp.messaging;

import java.util.Map;

/**
 * Interface representing a strategy for selecting destinations and endpoints for dispatching of messages. 
 * 
 * @since 1.3
 * 
 * @author Tobias Löfstrand
 */
public interface EndPointSelectionStrategy extends MessagingManagerHandler
{
	/**
    * Called when an endpoint is marked as available for message dispapatch.<br>
    * <br>
    * Note: This method must never be called with a lock held on {@link MessagingManager#getEndpointGroupsLock()}.
	 */
	public void endPointReady(MessagingEndPoint endPoint);
	
	/**
    * Called when an endpoint is destroyed and can no longer be used for dispatching messages.<br>
    * <br>
    * Note: This method must never be called with a lock held on {@link MessagingManager#getEndpointGroupsLock()}.
	 */
	public void endPointDestroyed(MessagingEndPoint endPoint);
	
	/**
    * Gets the first available endpoint. This method will wait a maximum of <code>timeOut</code> milliseconds for an available endpoint. <br>
    * <br>
    * Note: This method must never be called with a lock held on {@link MessagingManager#getEndpointGroupsLock()}.
	 */
	public MessagingEndPoint getEndPoint(long timeOut) throws InterruptedException;
	
	/**
    * Gets the first available endpoint for the specified destination. This method will wait a maximum of <code>timeOut</code> milliseconds for an available endpoint.<br>
    * <br>
    * Note: This method must never be called with a lock held on {@link MessagingManager#getEndpointGroupsLock()}.
	 */
	public MessagingEndPoint getEndPoint(Destination destination, long timeOut) throws InterruptedException;
	
	/**
    * Gets the first available endpoint for the specified named receiver. This method will wait a maximum of <code>timeOut</code> milliseconds for an available endpoint.<br>
    * <br>
    * Note: This method must never be called with a lock held on {@link MessagingManager#getEndpointGroupsLock()}.
	 */
	public MessagingEndPoint getEndPoint(String namedReceiver, long timeOut) throws InterruptedException;
	
	/**
    * Gets the first available endpoint for the specified metadata. This method will wait a maximum of <code>timeOut</code> milliseconds for an available endpoint.<br>
    * <br>
    * Note: This method must never be called with a lock held on {@link MessagingManager#getEndpointGroupsLock()}.
	 */
	public MessagingEndPoint getEndPoint(Map metaData, long timeOut) throws InterruptedException;
}
