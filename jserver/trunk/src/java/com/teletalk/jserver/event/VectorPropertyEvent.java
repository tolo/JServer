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
package com.teletalk.jserver.event;

import com.teletalk.jserver.property.PropertyConstants;
import com.teletalk.jserver.property.VectorProperty;
import com.teletalk.jserver.rmi.remote.RemoteEvent;
import com.teletalk.jserver.rmi.remote.RemoteVectorPropertyEvent;

/**
 * An eventclass for modificatios of the content of a VectorProperty object.
 * 
 * @see com.teletalk.jserver.property.VectorProperty
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1
 */
public class VectorPropertyEvent extends PropertyEvent implements PropertyConstants
{
	static final long serialVersionUID = -4659292903837761132L;
	
	private final byte eventType;
	
	private final String[] itemKeys;
	private final String[] itemDescriptions;
	
	/**
	 * Constructs a new VectorPropertyEvent.
	 * 
	 * @param source the source of the event.
	 * @param property an associated Property object.
    * @param eventType the event type.
	 */
	public VectorPropertyEvent(Object source, VectorProperty property, byte eventType)
	{
		super(source, property);
		
		this.eventType = eventType;
		this.itemKeys = null;
		this.itemDescriptions = null;
	}
	
	/**
	 * Constructs a new VectorPropertyEvent.
	 * 
	 * @param source the source of the event.
	 * @param property an associated Property object.
    * @param eventType the event type.
	 * @param itemKey the key of the item that is associated with this event.
	 * @param itemDescription description of the item that is associated with this event.
	 */
	public VectorPropertyEvent(Object source, VectorProperty property, byte eventType, String itemKey, String itemDescription)
	{
		super(source, property);
		
		this.eventType = eventType;
		this.itemKeys = new String[]{itemKey};
		this.itemDescriptions = new String[]{itemDescription};
	}
	
	/**
	 * Constructs a new VectorPropertyEvent.
	 * 
	 * @param source the source of the event.
	 * @param property an associated Property object.
    * @param eventType the event type.
	 * @param itemKeys the keys of the items that are associated with this event.
	 * @param itemDescriptions descriptions of the items that are associated with this event.
	 */
	public VectorPropertyEvent(Object source, VectorProperty property, byte eventType, String[] itemKeys, String[] itemDescriptions)
	{
		super(source, property);
		
		this.eventType = eventType;
		this.itemKeys = itemKeys;
		this.itemDescriptions = itemDescriptions;
	}
	
	/**
	 * Gets the class object representing the event class / group this event object belong to. 
	 * This is used to determine which type of listeners that should be notified of the event.<br>
	 * <br>
	 * This implementation returns the class object of {@link PropertyEvent}, which enables 
	 * {@link PropertyEventListener PropertyEventListeners} to receive VectorPropertyEvents.
	 * 
	 * @return Class object representing the event class / group of this event object.
	 */
	public Class getEventClass()
	{
		return PropertyEvent.class;
	}
	
	/**
	 * Gets the type of this event. See class {@link PropertyConstants} for valid values.
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
	
	public RemoteEvent createRemoteEvent()
	{
		return new RemoteVectorPropertyEvent(super.property.getFullName(), super.property.getValueAsString(), this.getEventType(), this.getItemKeys(), this.getItemDescriptions());
	}
}
