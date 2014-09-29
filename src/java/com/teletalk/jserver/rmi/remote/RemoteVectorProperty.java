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
 * Remote interface for interacting with a VectorPropertyRmiAdapter object.
 * 
 * @see com.teletalk.jserver.rmi.adapter.VectorPropertyRmiAdapter
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public interface RemoteVectorProperty extends RemoteProperty
{
	/**
	 * Returns the number of objects stored in the vector of this VectorProperty.
	 * 
	 * @return the size of the vector.
	 */
	public int size() throws RemoteException;
	
	/**
	 * Checkes if this VectorProperty is empty.
	 * 
	 * @return true if the VectorProperty has no items, otherwise false.
	 */
	public boolean isEmpty() throws RemoteException;
	
	/**
	 * Returns the object at the given index.
	 * 
	 * @param i index for an object.
	 * 
	 * @return the object at given index.
	 */
	public Object get(int i) throws RemoteException;
	
	/**
	 * Returns the item with the specified key.
	 * 
	 * @param key a key uniquely identifying an item in this VectorProperty.
	 * 
	 * @return the item with the specified key.
	 */
	public Object get(String key) throws RemoteException;
	
	/**
	 * Checks if an object with the specified unique index is contained in this vector.
	 * 
	 * @param key unique index for an object.
	 * 
	 * @return true if an object with the specified unique index is contained in this vector, otherwise false.
	 */
	public boolean containsKey(String key) throws RemoteException;
	
	/**
	 * Checkes if the specified item is a contained in this VectorProperty.
	 * 
	 * @param item an item to check for.
	 * 
	 * @return true if the specified item is contained in this VectorProperty as determined by the equals method; false otherwise.
	 */
	public boolean contains(Object item) throws RemoteException;
	
	/**
	 * Returns a String matrix (2*x) containing unique indices matched with
	 * string representaions of the actual objects stored in the vecor.
	 * 
	 * @return String matrix containing unique indices matched with
	 * string representaions of the actual objects stored in the vecor.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public String[][] getItemsAsStrings() throws RemoteException;
	
	/**
	 * Gets all external operations for the associated VectorProperty.
	 * 
	 * @return HashMap containing internal name/display name mappings of the operations.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public HashMap getExternalOperations() throws RemoteException;
	
	/**
	 * This method is called when an external operation is called on the associated VectorProperty object.
	 * 
	 * @param operationName the internal name of the operation that was called.
	 * @param keys an array containing unique indices that was selected for the operation call.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public void externalOperationCalled(String operationName, String[] keys) throws RemoteException;
}
