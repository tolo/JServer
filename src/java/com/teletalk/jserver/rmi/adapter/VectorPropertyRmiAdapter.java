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
package com.teletalk.jserver.rmi.adapter;

import java.rmi.RemoteException;
import java.util.HashMap;

import com.teletalk.jserver.JServerUtilities;
import com.teletalk.jserver.property.VectorProperty;
import com.teletalk.jserver.property.VectorPropertyItem;
import com.teletalk.jserver.rmi.remote.RemoteVectorProperty;

/**
 * Adapter for remote interaction with a VectorProperty object.
 * 
 * @see com.teletalk.jserver.property.VectorProperty
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public final class VectorPropertyRmiAdapter extends PropertyRmiAdapter implements RemoteVectorProperty
{
   static final long serialVersionUID = 8854359751729397497L;
   
	private final VectorProperty adaptee;
	
	/**
	 * Constructs a new VectorPropertyRmiAdapter object.
	 * 
	 * @param adaptee a Property adaptee object.
	 * 
	 * @exception RemoteException if there was an error creating this VectorPropertyRmiAdapter.
	 */
	public VectorPropertyRmiAdapter(VectorProperty adaptee) throws RemoteException
	{
		super(adaptee);
		this.adaptee = adaptee;
	}
	
	/**
	 * Returns the number of objects stored in the vector of this VectorProperty.
	 * 
	 * @return the size of the vector.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public int size() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".VectorPropertyRmiAdapter", "size()");
			
			return adaptee.size();	
		}
		catch(Throwable t)
		{
			JServerUtilities.logError(adaptee.getFullName(), "Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Checkes if this VectorProperty is empty.
	 * 
	 * @return true if the VectorProperty has no items, otherwise false.
	 */
	public boolean isEmpty() throws RemoteException
	{
		try
		{		
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".VectorPropertyRmiAdapter", "isEmpty()");			

			return adaptee.isEmpty();	
		}
		catch(Throwable t)
		{
			JServerUtilities.logError(adaptee.getFullName(), "Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Returns the object at the given index.
	 * 
	 * @param i index for an object.
	 * 
	 * @return the object at given index.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final Object get(int i) throws RemoteException
	{
		try
		{		
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".VectorPropertyRmiAdapter", "get(" + i + ")");			
			
			return adaptee.get(i);
		}
		catch(Throwable t)
		{
			JServerUtilities.logError(adaptee.getFullName(), "Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}

	/**
	 * Returns the item with the specified key.
	 * 
	 * @param key a key uniquely identifying an item in this VectorProperty.
	 * 
	 * @return the item with the specified key.
	 */
	public Object get(String key) throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".VectorPropertyRmiAdapter", "get(" +key + ")");			
			
			return adaptee.get(key);
		}
		catch(Throwable t)
		{
			JServerUtilities.logError(adaptee.getFullName(), "Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Checks if an object with the specified key is contained in this VectorProperty.
	 * 
	 * @param key a key uniquely identifying an item in this VectorProperty.
	 * 
	 * @return true if an object with the specified key is contained in this vector, otherwise false.
	 */
	public boolean containsKey(String key) throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".VectorPropertyRmiAdapter", "containsKey(" +key + ")");			
			
			return adaptee.containsKey(key);
		}
		catch(Throwable t)
		{
			JServerUtilities.logError(adaptee.getFullName(), "Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Checkes if the specified item is a contained in this VectorProperty.
	 * 
	 * @param item an item to check for.
	 * 
	 * @return true if the specified item is contained in this VectorProperty as determined by the equals method; false otherwise.
	 */
	public boolean contains(Object item) throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".VectorPropertyRmiAdapter", "contains(Object item)");
			
			if( (item != null) && (item instanceof VectorPropertyItem) )
			     return adaptee.contains((VectorPropertyItem)item);
			 else return false;
		}
		catch(Throwable t)
		{
			JServerUtilities.logError(adaptee.getFullName(), "Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Returns a Hashtable containing unique indices matched with
	 * string representaions of the actual objects stored in the vecor.
	 * 
	 * @return Hashtable containing unique indices matched with
	 * string representaions of the actual objects stored in the vecor.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public String[][] getItemsAsStrings() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".VectorPropertyRmiAdapter", "getItemsAsStrings()");
			
			return adaptee.getItemsAsStrings();
		}
		catch(Throwable t)
		{
			JServerUtilities.logError(adaptee.getFullName(), "Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Gets all external operations for the associated VectorProperty.
	 * 
	 * @return HashMap containing internal name/display name mappings of the operations.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public HashMap getExternalOperations() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".VectorPropertyRmiAdapter", "getExternalOperations()");
			
			return adaptee.getExternalOperations();
		}
		catch(Throwable t)
		{
			JServerUtilities.logError(adaptee.getFullName(), "Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * This method is called when an external operation is called on the associated VectorProperty object.
	 * 
	 * @param operationName the internal name of the operation that was called.
	 * @param keys an array containing unique indices that was selected for the operation call.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public void externalOperationCalled(String operationName, String[] keys) throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".VectorPropertyRmiAdapter", "externalOperationCalled(" +operationName + ", String[] keys)");
			
			adaptee.externalOperationCalled(operationName, keys);
		}
		catch(Throwable t)
		{
			JServerUtilities.logError(adaptee.getFullName(), "Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
}
