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
 * @since Beta
 */
public class RemotePropertyEvent extends RemoteEvent implements RemotePropertyConstants
{
	static final long serialVersionUID = -7384655745192792263L;

	private final String propertyFullName;
	
	/**
	 * Constructs a new RemotePropertyEvent.
	 * 
	 * @param propertyFullName the full name of the Property that is to be contained in this RemotePropertyEvent.
	 * @param value the value that is to be contained in this RemotePropertyEvent.
	 * 
	 * @see com.teletalk.jserver.property.Property
	 */
	public RemotePropertyEvent(String propertyFullName, String value)
	{
		super(value);
		
		this.propertyFullName = propertyFullName;
	}
	
	/**
	 * Returns the name of the Property associated with this RemotePropertyEvent.
	 * 
	 * @return String representation of the name.
	 */
	public String getPropertyName()
	{
		return propertyFullName;
	}
	
	/**
	 * Returns the value of the Property associated with this RemotePropertyEvent.
	 * 
	 * @return String representation of the value.
	 */
	public String getValue()
	{
		return (String)param;
	}

	/**
	 * Returns a String object representing of this RemotePropertyEvent.
	 * 
	 * @return a String representation of this RemotePropertyEvent.
	 */	
	public String toString()
	{
		return "RemotePropertyEvent(" + getPropertyName() + ", " + getValue() + ")";	
	}
}
