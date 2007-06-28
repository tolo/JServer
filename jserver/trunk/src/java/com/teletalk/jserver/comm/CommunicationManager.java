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
package com.teletalk.jserver.comm;

import com.teletalk.jserver.SubSystem;

/**
 * Base class for subsystems implenting a manager for communication.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 2
 */
public abstract class CommunicationManager extends SubSystem
{
	/**
	 * Creates a new CommunicationManager.
	 * 
	 * @param parent the parent of this CommunicationManager.
	 * @param name the name of this CommunicationManager.
	 */
	public CommunicationManager(SubSystem parent, String name)
	{
		super(parent, name);
	}
	
	/**
	 * Gets an EndPoint object for the specified address. If this CommunicationManager currently 
	 * doesn't contain an endpoint matching the specified address, a new one will be created.
	 * 
	 * @param address an EndPointIdentifier object for which to get an endpoint.
	 * 
	 * @return an EndPoint object.
	 * 
	 * @see EndPoint
	 * @see EndPointIdentifier
	 */
	public abstract EndPoint getEndPoint(EndPointIdentifier address);
	
	/**
	 * Gets an EndPoint object for the specified address.
	 * 
	 * @param address an EndPointIdentifier object for which to get an endpoint.
	 * @param create boolean flag indicating if a new endpoint should be created if this CommunicationManager currently 
	 * doesn't contain one matching the specified address.
	 * 
	 * @return an EndPoint object.
	 * 
	 * @see EndPoint
	 * @see EndPointIdentifier
	 */
	public abstract EndPoint getEndPoint(EndPointIdentifier address, boolean create);
	
	/**
	 * Gets all the endpoints contained in this CommunicationManager.
	 * 
	 * @return an array of EndPoint objects.
	 */
	public abstract EndPoint[] getEndPoints();
}
