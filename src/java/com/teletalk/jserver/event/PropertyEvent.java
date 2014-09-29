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

import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.rmi.remote.RemoteEvent;
import com.teletalk.jserver.rmi.remote.RemotePropertyEvent;

/**
 * An eventclass for properties modifications.
 * 
 * @see com.teletalk.jserver.property.Property
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta
 */
public class PropertyEvent extends Event
{
	final Property property;
	
	private final boolean initializationModification;
	
	/**
	 * Constructs a new PropertyEvent.
	 * 
	 * @param source the source of the event.
	 * @param property an associated Property object.
	 */
	public PropertyEvent(Object source, Property property)
	{
		this(source, property, false);
	}
	
	/**
	 * Constructs a new PropertyEvent.
	 * 
	 * @param source the source of the event.
	 * @param property an associated Property object.
	 * @param initializationModification boolean flag indicating if this modification event was triggered as 
	 * a result of a property beeing initialized (<code>true</code>).
	 */
	public PropertyEvent(Object source, Property property, boolean initializationModification)
	{
		super(source, true);
		
		this.property = property;
		this.initializationModification = initializationModification;
	}
	
	/**
	 * Gets the Property object associated with this PropertyEvent.
	 * 
	 * @return the associated Property object.
	 */
	public Property getProperty()
	{
		return property;
	}
	
	/**
	 * Checks if this property modification event was triggered as 
	 * a result of a property beeing initialized.
	 * 
	 * @return <code>true</code> if this modification event was triggered as 
	 * a result of a property beeing initialized, otherwise <code>false</code>.
	 */
	public boolean isInitializationModification()
	{
		return initializationModification;
	}
	
	public void notifyListener(final Object listener)
	{
		if(listener instanceof PropertyEventListener)
      {
			((PropertyEventListener)listener).propertyModified(this);
      }
		else super.notifyListener(listener);
	}
	
	public RemoteEvent createRemoteEvent()
	{
		return new RemotePropertyEvent(property.getFullName(), property.getValueAsString());
	}
}
