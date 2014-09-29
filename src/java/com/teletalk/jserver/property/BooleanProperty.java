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
 * Property class for handling boolean values.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public class BooleanProperty extends Property
{
	/** String constant for the value <code>true</code>. */
	public static final String TRUE = "true";
	
	/** String constant for the value <code>false</code>. */
	public static final String FALSE = "false";
	
	private volatile boolean boolVal;
	
	/**
	 * Creates a new BooleanProperty object.
	 * 
	 * @param owner the owner of this BooleanProperty.
	 * @param name the name of this BooleanProperty.
	 * @param defaultValue the default value of this BooleanProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 */
	public BooleanProperty(PropertyOwner owner, String name, boolean defaultValue, int modificationMode)
	{
		super(owner, name, new Boolean(defaultValue), modificationMode);
		
		boolVal = defaultValue;
	}
	
	/**
	 * Creates a new BooleanProperty object.
	 * 
	 * @param owner the owner of this BooleanProperty.
	 * @param name the name of this BooleanProperty.
	 * @param defaultValue the default value of this BooleanProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
	 */
	public BooleanProperty(PropertyOwner owner, String name, boolean defaultValue, int modificationMode, boolean persistent)
	{
		super(owner, name, new Boolean(defaultValue), modificationMode, persistent);
		
		boolVal = defaultValue;
	}
	
	/**
	 * Creates a new BooleanProperty object that isn't modifiable (externally).
	 * 
	 * @param owner the owner of this BooleanProperty.
	 * @param name the name of this BooleanProperty.
	 * @param defaultValue the default value of this BooleanProperty.
	 */	
	public BooleanProperty(PropertyOwner owner, String name, boolean defaultValue) 
	{
		super(owner, name, new Boolean(defaultValue));
		
		boolVal = defaultValue;
	}
	
	/**
	 * Gets the value of this BooleanProperty as a String.
	 * 
	 * @return String representation of the value.
	 */
	public synchronized String getValueAsString()
	{
		if(boolVal) return TRUE;
		else return FALSE;
	}
	
	/**
	 * Gets the value of this BooleanProperty as an Object.
	 * 
	 * @return the value as an Object.
	 */
	public Object getValueAsObject()
	{
		return getValueAsBoolean();
	}	
	
	/**
	 * Gets the value of this BooleanProperty as a Boolean object.
	 * 
	 * @return a Boolean object.
	 */
	public Boolean getValueAsBoolean()
	{
		return new Boolean(boolVal);
	}
	
	/**
	 * Gets the value of this BooleanProperty as a boolean.
	 * 
	 * @return the boolean value.
	 */
	public boolean booleanValue()
	{
		return boolVal;
	}
	
	/**
	 * Sets the value of this BooleanProperty as a String, after first checking if the owner allows modification
	 * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.
	 * 
	 * @param strVal the new value of this BooleanProperty.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValueAsString(String strVal)
	{
		if(strVal.equalsIgnoreCase(TRUE))
		{
			return this.setValue(true);
		}
		else if(strVal.equalsIgnoreCase(FALSE))
		{
			return this.setValue(false);			
		}
		else return false;
	}
	
	/**
	 * Sets the value of this BooleanProperty as an Object, after first checking if the owner allows modification
	 * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.
	 * 
	 * @param value the new value of this BooleanProperty.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValueAsObject(Object value)
	{
		if(value instanceof Boolean)
			return setValue((Boolean)value);
		else
			return false;
	}
	
	/**
	 * Sets the value of this Property after first checking if the owner allows modification
	 * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.
	 * 
	 * @param booleanValue the new boolean value.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValue(boolean booleanValue)
	{
		if(canSet())
		{
			boolean validationResult = false;
			
			synchronized(this)
			{
				final boolean oldBoolVal = this.boolVal;
					
				this.boolVal = booleanValue;
				validationResult = validate();
						
				if(!validationResult)
					boolVal = oldBoolVal;
			}
			if(validationResult)
			{
				modified();
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Sets the value of this Property after first checking if the owner allows modification
	 * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.
	 * 
	 * @param booleanValue the new Boolean value.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValue(Boolean booleanValue)
	{
		return setValue(booleanValue.booleanValue());
	}
}
