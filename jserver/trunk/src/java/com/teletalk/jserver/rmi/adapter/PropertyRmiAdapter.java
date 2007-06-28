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
import java.rmi.server.UnicastRemoteObject;
import java.util.HashMap;

import com.teletalk.jserver.JServerUtilities;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.rmi.RmiManager;
import com.teletalk.jserver.rmi.remote.RemoteProperty;

/**
 * Adapter for remote interaction with a Property object.
 *
 * @see com.teletalk.jserver.property.Property
 *
 * @author Tobias Löfstrand
 *
 * @since Alpha
 */
public class PropertyRmiAdapter extends UnicastRemoteObject implements RemoteProperty, RmiAdapter, Cloneable
{
   static final long serialVersionUID = 3133852410111003798L;
   
	private final Property adaptee;

	/**
	 * Constructs a new PropertyRmiAdapter object.
	 *
	 * @param adaptee a Property adaptee object.
	 *
	 * @exception RemoteException if there was an error creating this PropertyRmiAdapter.
	 */
	public PropertyRmiAdapter(Property adaptee) throws RemoteException
	{
		super(RmiManager.getRmiManager().getExportAddresses()[0].getPort());
		
		this.adaptee = adaptee;
	}
	
	public Object clone()
	{
		try{
		return super.clone();
		}catch(Exception e){e.printStackTrace();return null;}
	}
	
	/**
	 * Get the type (BOOLEAN_TYPE, NUMBER_TYPE, etc.) of the property specified by parameter <code>p</code>.
	 *
	 * @param p a property to get the type for.
	 *
	 * @return type of this property as an integer value.
	 */
	public static int getType(Property p)
	{
		if(p instanceof com.teletalk.jserver.property.BooleanProperty)
			return BOOLEAN_TYPE;
		else if(p instanceof com.teletalk.jserver.property.DateProperty)
			return DATE_TYPE;
		else if(p instanceof com.teletalk.jserver.property.EnumProperty)
			return ENUM_TYPE;
		else if(p instanceof com.teletalk.jserver.property.NumberProperty)
			return NUMBER_TYPE;
		else if(p instanceof com.teletalk.jserver.property.StringProperty)
			return STRING_TYPE;
		else if(p instanceof com.teletalk.jserver.property.VectorProperty)
			return VECTOR_TYPE;
		else if(p instanceof com.teletalk.jserver.property.MultiValueProperty)
			return MULTIVALUE_TYPE;
		else
			return CUSTOM_TYPE;
	}

	/**
	 * Get the type of this property (BOOLEAN_TYPE, NUMBER_TYPE, etc.).
	 *
	 * @return type of this property as an integer value.
	 */
	public int getType() throws RemoteException
	{
		try	
		{		
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".PropertyRmiAdapter", "getType()");
						
			return getType(adaptee);
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
	 * Returns the name of the Property.
	 *
	 * @return the name.
	 *
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final String getName() throws RemoteException
	{
		try	
		{		
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".PropertyRmiAdapter", "getName()");

			return adaptee.getName();
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
	 * Returns the full name of the Property by concatenating the name of the Property to the full name
	 * of it's parent, separated by a dot.
	 *
	 * @return the full name.
	 *
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final String getFullName() throws RemoteException
	{
		try	
		{		
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".PropertyRmiAdapter", "getFullName()");
						
			return adaptee.getFullName();
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
	 * Returns the value of the Property.
	 *
	 * @return the value.
	 *
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final Object getValueAsObject() throws RemoteException
	{
		try	
		{		
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".PropertyRmiAdapter", "getValueAsObject()");
						
			return adaptee.getValueAsObject();
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
	 * Gets the value of the Property as a String.
	 *
	 * @return String representation of the value.
	 *
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final String getValueAsString() throws RemoteException
	{
		try	
		{		
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".PropertyRmiAdapter", "getValueAsString()");
						
			return adaptee.getValueAsString();
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
	 * Method to parse a new value for the Property from a String.
	 *
	 * @param strVal the String from which the value will be parsed.
	 *
	 * @return true if the parsing succeded, otherwise false.
	 *
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public boolean setValue(String strVal) throws RemoteException
	{
		try	
		{		
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".PropertyRmiAdapter", "setValue(" + strVal + ")");

			return adaptee.setValueAsString(strVal);
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
	public boolean setValue(Object value) throws RemoteException
	{
		try	
		{		
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".PropertyRmiAdapter", "setValue(Object value)");
			
			return adaptee.setValueAsObject(value);
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
	 * Gets all the metadata stored in this Property.
	 *
	 * @return a HashMap containing metadata.
	 */
	public HashMap getMetaData()  throws RemoteException
	{
		try	
		{		
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".PropertyRmiAdapter", "getMetaData()");
			
			return adaptee.getMetaData();
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
	 * Gets metadata.
	 *
	 * @param key metadata key.
	 *
	 * @return the metadata value.
	 */
	public Object getMetaData(Object key) throws RemoteException
	{
		try	
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".PropertyRmiAdapter", "getMetaData(Object key)");
			
			return adaptee.getMetaData( (key != null) ? key.toString() : null );
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
	 * Sets the modification mode.
	 *
	 * @param mode the new value for the modification mode.
	 *
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final void setModificationMode(int mode) throws RemoteException
	{
		try	
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".PropertyRmiAdapter", "setModificationMode( + mode + )");
			
			adaptee.setModificationMode(mode);
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
	 * Returns the value of the modifiable flag.
	 *
	 * @return true if the Property is modifiable, otherwise false.
	 *
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final boolean isModifiable() throws RemoteException
	{
		try	
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".PropertyRmiAdapter", "isModifiable()");
			
			return adaptee.isModifiable();
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
	 * Gets the modificationMode.
	 *
	 * @return the value of modificationMode.
	 *
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final int getModificationMode() throws RemoteException
	{
		try	
		{		
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".PropertyRmiAdapter", "getModificationMode()");
			
			return adaptee.getModificationMode();
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
	 * Checkes if modification of this property will trigger some sort of restart.
	 *
	 * @return true if the property triggers some sort of restart on modification.
	 */
	public final boolean restartNeeded() throws RemoteException
	{
		try	
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".PropertyRmiAdapter", "restartNeeded()");
			
			int mode = getModificationMode();
			return mode == Property.MODIFIABLE_OWNER_RESTART || mode == Property.MODIFIABLE_SERVER_RESTART;
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
    * Returns a String for use in persistent storage.
    * 
    * @return a string representation of the property.
    * 
    * @exception RemoteException if there was an error during remote access of this method.
    * 
    * @deprecated
    */
   public String toPersistentStorageString() throws RemoteException{return null;}
}
