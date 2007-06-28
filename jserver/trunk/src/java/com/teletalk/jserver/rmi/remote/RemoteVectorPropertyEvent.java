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
 * An remote event class for modificatios of the content of a VectorProperty object.
 * 
 * @see com.teletalk.jserver.event.VectorPropertyEvent
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1
 */
public class RemoteVectorPropertyEvent extends RemotePropertyEvent
{
   static final long serialVersionUID = 7134056187885853525L;

	private final byte eventType;
	private final String[] itemKeys;
	private final String[] itemDescriptions;
	
	/**
	 * Constructs a new RemoteVectorPropertyEvent.
	 * 
	 * @param propertyFullName the full name of the Property that is to be contained in this RemoteVectorPropertyEvent.
	 * @param value the value that is to be contained in this RemoteVectorPropertyEvent.
	 * @param itemKeys the keys of the items that are associated with this event.
	 * @param itemDescriptions descriptions of the items that are associated with this event.
	 */
	public RemoteVectorPropertyEvent(String propertyFullName, String value, byte eventType, String[] itemKeys, String[] itemDescriptions)
	{
		super(propertyFullName, value);
		
		this.eventType = eventType;
		this.itemKeys = itemKeys;
		this.itemDescriptions = itemDescriptions;
	}
	
	/**
	 * Gets the type of this event.
	 * 
	 * @return the type of this event.
	 */
	public byte getEventType()
	{
		return this.eventType;
	}
	
	/**
	 * Checks if the type of this event indicates that an item was added to the associated VectorProperty object.
	 * 
	 * @return <code>true</code> if the type of this event indicates that an item was added to the associated VectorProperty object, otherwise <code>false</code>.
	 */
	public boolean isAddEvent()
	{
		return this.eventType == VECTORPROPERTY_VALUE_ADDED;
	}
	
	/**
	 * Checks if the type of this event indicates that an item was removed from the associated VectorProperty object.
	 * 
	 * @return <code>true</code> if the type of this event indicates that an item was removed from the associated VectorProperty object, otherwise <code>false</code>.
	 */
	public boolean isRemoveEvent()
	{
		return this.eventType == VECTORPROPERTY_VALUE_REMOVED;
	}
	
	/**
	 * Checks if the type of this event indicates that an item was modified in the associated VectorProperty object.
	 * 
	 * @return <code>true</code> if the type of this event indicates that an item was modified in the associated VectorProperty object, otherwise <code>false</code>.
	 */
	public boolean isModificationEvent()
	{
		return this.eventType == VECTORPROPERTY_VALUE_MODIFIED;
	}
	
	/**
	 * Checks if the type of this event indicates that all items were removed from the associated VectorProperty object.
	 * 
	 * @return <code>true</code> if the type of this event indicates that all items were removed from the associated VectorProperty object, otherwise <code>false</code>.
	 */
	public boolean isClearEvent()
	{
		return this.eventType == VECTORPROPERTY_CLEAR;
	}
	
	/**
	 * Gets the keys of the items that are associated with this event.
	 * 
	 * @return an array of String objects.
	 */
	public String[] getItemKeys()
	{
		return this.itemKeys;
	}
	
	/**
	 * Gets the descriptions of the items that are associated with this event.
	 * 
	 * @return an array of String objects.
	 */
	public String[] getItemDescriptions()
	{
		return this.itemDescriptions;
	}
}
