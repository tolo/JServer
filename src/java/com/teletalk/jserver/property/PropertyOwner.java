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
package com.teletalk.jserver.property;

/**
 * Interface for objects that own properties.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 * 
 * @see Property
 */
public interface PropertyOwner
{
	/**
	 * Called when the value of a property has been modified.
	 * 
	 * @param property the Property that was changed.
	 * 
	 * @see Property
	 */
	public void propertyModified(Property property);
	
	/**
	 * Called when the value of a property has been modified.
	 * 
	 * @param property the Property that was changed.
	 * @param event the specific event that is to be dispatched as a result of the modification.
	 * 
	 * @see Property
	 */
	//public void propertyModified(Property property, PropertyEvent event);
	
	/**
	 * Called to check if a property can be modified.
	 * 
	 * @param property The property to be checked.
	 * 
	 * @return true if the property can be modified, otherwise false.
	 */
	public boolean propertyModificationAllowed(Property property);
	
	/**
	 * Validates a modification of a property's value. The property specified by parameter <code>property</code> will 
	 * be given the value to be validated before this method is called, and will revert to the old one if this method returns <code>false</code>.
	 * 
	 * @param property The property to be validated.
	 * 
	 * @return boolean value indicating if the property passed (true) validation or not (false).
	 */
	public boolean validatePropertyModification(Property property);
	
	/**
	 * Returns the full name of this PropertyOwner.
	 * 
	 * @return the full name.
	 */
	public String getFullName();
}
