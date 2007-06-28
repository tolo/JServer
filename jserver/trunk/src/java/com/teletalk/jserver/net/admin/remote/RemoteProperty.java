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
package com.teletalk.jserver.net.admin.remote;

import java.util.HashMap;

/**
 * Remote interface for interacting with a Property object.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1.4 (20060503)
 */
public interface RemoteProperty
{
	/**
	 * Returns the name of the Property.
	 */
	public String getName();
	
	/**
	 * Returns the full name of the Property by concatenating the name of the Property to the full name
	 * of it's parent, separated by a dot.
	 */	
	public String getFullName();
	
	/**
	 * Returns the value of the Property.
	 */
	public Object getValueAsObject();
		
	/**
	 * Gets the value of the Property as a String.
	 */
	public String getValueAsString();
   
   /**
    * Gets the value of this property as an array of strings.
    */
   public String[] getValuesAsStrings();
		
	/**
	 * Method to parse a new value for the Property from a String.
	 * 
	 * @param strVal the String from which the value will be parsed.
	 * 
	 * @return true if the parsing succeded, otherwise false.
	 */
	public boolean setValue(String strVal);
   
   /**
    * Sets the value of this property as an array of strings. 
    * 
    * @param values the Strings from which the values will be parsed.
    * 
    * @return true if the parsing succeded, otherwise false.
    */
   public boolean setValuesAsStrings(String[] values);
	
	/**
	 * Sets the value of the Property if the class of the defaultvalue is assignment-compatible 
	 * with the class of the new value. If the value was successfully set the parent will be
	 * notified.
	 * 
	 * @param value the new value.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValue(Object value);
   
   /**
    * Sets the value of this property as an array of objects.
    * 
    * @param values the new values.
    * 
    * @return true if the values were successfully set, otherwise false.
    */
   public boolean setValuesAsObjects(Object[] values);
		
	/**
	 * Gets all the metadata stored in the Property.
	 */
	public HashMap getMetaData();
	
	/**
	 * Gets metadata.
	 * 
	 * @param key metadata key.
	 * 
	 * @return the metadata value.
	 */
	public Object getMetaData(Object key);
											   
	/**
	 * Sets the modification mode.
	 * 
	 * @param mode the new value for the modification mode.
	 */
	public void setModificationMode(int mode);

	/**
	 * Returns the value of the modifiable flag.
	 * 
	 * @return true if the Property is modifiable, otherwise false.
	 */
	public boolean isModifiable();
	
	/**
	 * Gets the modificationMode.
	 */
	public int getModificationMode();
	
	/**
	 * Checkes if modification of this property will trigger some sort of restart.
	 * 
	 * @return true if the property triggers some sort of restart on modification.
	 */
	public boolean restartNeeded();
}
