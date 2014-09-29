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
 * Remote interface for interacting with a VectorProperty object.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1.4 (20060503)
 */
public interface RemoteVectorProperty extends RemoteProperty
{
	/**
	 * Returns the number of objects stored in the vector of this VectorProperty.
	 * 
	 * @return the size of the vector.
	 */
	public int size();
	
	/**
	 * Checkes if this VectorProperty is empty.
	 * 
	 * @return true if the VectorProperty has no items, otherwise false.
	 */
	public boolean isEmpty();
	
	/**
	 * Returns the object at the given index.
	 * 
	 * @param i index for an object.
	 * 
	 * @return the object at given index.
	 */
	public Object get(int i);
	
	/**
	 * Returns the item with the specified key.
	 * 
	 * @param key a key uniquely identifying an item in this VectorProperty.
	 * 
	 * @return the item with the specified key.
	 */
	public Object get(String key);
	
	/**
	 * Checks if an object with the specified unique index is contained in this vector.
	 * 
	 * @param key unique index for an object.
	 * 
	 * @return true if an object with the specified unique index is contained in this vector, otherwise false.
	 */
	public boolean containsKey(String key);
	
	/**
	 * Checkes if the specified item is a contained in this VectorProperty.
	 * 
	 * @param item an item to check for.
	 * 
	 * @return true if the specified item is contained in this VectorProperty as determined by the equals method; false otherwise.
	 */
	public boolean contains(Object item);
	
	/**
	 * Returns a String matrix (2*x) containing unique indices matched with
	 * string representaions of the actual objects stored in the vecor.
	 * 
	 * @return String matrix containing unique indices matched with
	 * string representaions of the actual objects stored in the vecor.
	 */
	public String[][] getItemsAsStrings();
	
	/**
	 * Gets all external operations for the associated VectorProperty.
	 * 
	 * @return HashMap containing internal name/display name mappings of the operations.
	 */
	public HashMap getExternalOperations();
	
	/**
	 * This method is called when an external operation is called on the associated VectorProperty object.
	 * 
	 * @param operationName the internal name of the operation that was called.
	 * @param keys an array containing unique indices that was selected for the operation call.
	 */
	public void externalOperationCalled(String operationName, String[] keys);
}
