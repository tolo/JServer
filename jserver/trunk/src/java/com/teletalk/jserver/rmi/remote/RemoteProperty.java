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

import java.rmi.RemoteException;
import java.util.HashMap;

/**
 * Remote interface for interacting with a PropertyRmiAdapter object.
 * 
 * @see com.teletalk.jserver.rmi.adapter.PropertyRmiAdapter
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public interface RemoteProperty extends RemoteObject, RemotePropertyConstants
{
	/**
	 * Get the type of this property (BOOLEAN_TYPE, NUMBER_TYPE, etc.).
	 * 
	 * @return type of this property as an integer value.
	 */
	public int getType() throws RemoteException;
	
	/**
	 * Returns the name of the Property.
	 * 
	 * @return the name.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public String getName() throws RemoteException;
	
	/**
	 * Returns the full name of the Property by concatenating the name of the Property to the full name
	 * of it's parent, separated by a dot.
	 * 
	 * @return the full name.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */	
	public String getFullName() throws RemoteException;
	
	/**
	 * Returns the value of the Property.
	 * 
	 * @return the value.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public Object getValueAsObject() throws RemoteException;
		
	/**
	 * Gets the value of the Property as a String.
	 * 
	 * @return String representation of the value.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public String getValueAsString() throws RemoteException;
		
	/**
	 * Method to parse a new value for the Property from a String.
	 * 
	 * @param strVal the String from which the value will be parsed.
	 * 
	 * @return true if the parsing succeded, otherwise false.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public boolean setValue(String strVal) throws RemoteException;
	
	/**
	 * Sets the value of the Property if the class of the defaultvalue is assignment-compatible 
	 * with the class of the new value. If the value was successfully set the parent will be
	 * notified.
	 * 
	 * @param value the new value.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public boolean setValue(Object value) throws RemoteException;
		
	/**
	 * Gets all the metadata stored in the Property.
	 * 
	 * @return a HashMap containing metadata.
	 */
	public HashMap getMetaData() throws RemoteException;
	
	/**
	 * Gets metadata.
	 * 
	 * @param key metadata key.
	 * 
	 * @return the metadata value.
	 */
	public Object getMetaData(Object key) throws RemoteException;
	
	/**
	 * Returns a String for use in persistent storage.
	 * 
	 * @return a string representation of the property.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public String toPersistentStorageString() throws RemoteException;
											   
	/**
	 * Sets the modification mode.
	 * 
	 * @param mode the new value for the modification mode.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public void setModificationMode(int mode) throws RemoteException;

	/**
	 * Returns the value of the modifiable flag.
	 * 
	 * @return true if the Property is modifiable, otherwise false.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public boolean isModifiable() throws RemoteException;
	
	/**
	 * Gets the modificationMode.
	 * 
	 * @return the value of modificationMode.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public int getModificationMode() throws RemoteException;
	
	/**
	 * Checkes if modification of this property will trigger some sort of restart.
	 * 
	 * @return true if the property triggers some sort of restart on modification.
	 */
	public boolean restartNeeded() throws RemoteException;
}
