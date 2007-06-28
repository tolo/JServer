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

/**
 * This class contains initialization data used when creating {@link MessagingEndPoint} objects.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.2
 */
public class MessagingEndPointInitData
{
	private Destination destination;
	private boolean firstEndPoint;
		
	/**
	 * Creates a new MessagingEndPointInitData that does not indicate the first connection for a certian address.
	 * 
	 * @param destination reference to a Destination object.
	 */
	public MessagingEndPointInitData(Destination destination)
	{
		this.destination = destination;
		this.firstEndPoint = false;
	}
	
	/**
	 * Creates a new MessagingEndPointInitData.
	 * 
	 * @param destination reference to a Destination object.
	 * @param firstEndPoint boolean flag indicating if this data object represents the first connection to a certain BLiP.
	 */
	public MessagingEndPointInitData(Destination destination, boolean firstEndPoint)
	{
		this.destination = destination;
		this.firstEndPoint = firstEndPoint;
	}
	
	/**
	 * Gets the Destination object contained in this MessagingEndPointInitData.
	 * 
	 * @return the Destination object contained in this MessagingEndPointInitData.
	 */
	public Destination getDestination()
	{
		return destination;
	}
	
	/**
	 * Gets the value of the flag indicating if this data object represents the first connection to a certain remote messaging system.
	 * 
	 * @return the value of the flag indicating if this data object represents the first connection to a certain remote messaging system.
	 */
	public boolean isFirstEndPoint()
	{
		return firstEndPoint;
	}
}
