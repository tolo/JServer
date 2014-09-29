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
package com.teletalk.jserver.rmi.remote;

/**
 * RemoteEvent for properties.
 * 
 * @see com.teletalk.jserver.rmi.remote.RemoteEvent
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public final class RemoteStatusEvent extends RemoteEvent
{
	static final long serialVersionUID = -5444697201812139710L;
	
	private final String sourceName;
	private final int status;
	private final String statusString;
	
	/**
	 * Constructs a new RemoteStatusEvent.
	 * 
	 * @param sourceName the name of the source component that generated this event.
	 * @param statusString a status value as a String.
	 */
	public RemoteStatusEvent(String sourceName, int status, String statusString)
	{
		super(sourceName);
		
		this.sourceName = sourceName;
		this.status = status;
		this.statusString = statusString;
	}
	
	/**
	 * Returns the name of the Property associated with this RemotePropertyEvent.
	 * 
	 * @return String representation of the name.
	 */
	public String getSourceName()
	{
		return sourceName;
	}
	
	/**
	 * Gets the statusvalue associated with the source of this event.
	 * 
	 * @return a statusvalue.
	 */
	public int getStatus()
	{
		return status;
	}

	/**
	 * Gets the statusvalue as a string.
	 * 
	 * @return a statusvalue.
	 */
	public String getStatusAsString()
	{
		return statusString;
	}

	
	/**
	 * Returns a String object representing of this RemotePropertyEvent.
	 * 
	 * @return a String representation of this RemotePropertyEvent.
	 */	
	public String toString()
	{
		return "RemoteStatusEvent(" + getSourceName() + ", " + getStatus() + ")";	
	}
}
