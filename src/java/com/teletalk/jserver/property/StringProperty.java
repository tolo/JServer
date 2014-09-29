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
 * Property class for handling String values. This class is used by PropertyManager for parsing properties from file.
 * 
 * @see com.teletalk.jserver.property.PropertyManager
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public class StringProperty extends Property
{
	private volatile String stringValue;
   
   /**
    * Creates a new StringProperty object.
    * 
    * @param name the name of this StringProperty.
    * @param defaultValue the default value of this StringProperty.
    */
   protected StringProperty(String name, String defaultValue) 
   {
      super(name, defaultValue);
      
      this.stringValue = defaultValue;
   }

	/**
	 * Creates a new StringProperty object.
	 * 
	 * @param owner the owner of this StringProperty.
	 * @param name the name of this StringProperty.
	 * @param defaultValue the default value of this StringProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 */
	public StringProperty(PropertyOwner owner, String name, String defaultValue, int modificationMode) 
	{
		super(owner, name, defaultValue, modificationMode);
		
		this.stringValue = defaultValue;
	}
	
	/**
	 * Creates a new StringProperty object.
	 * 
	 * @param owner the owner of this StringProperty.
	 * @param name the name of this StringProperty.
	 * @param defaultValue the default value of this StringProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
	 */
	public StringProperty(PropertyOwner owner, String name, String defaultValue, int modificationMode, boolean persistent) 
	{
		super(owner, name, defaultValue, modificationMode, persistent);
		
		this.stringValue = defaultValue;
	}
	
	/**
	 * Creates a new StringProperty object that isn't modifiable.
	 * 
	 * @param owner the owner of this StringProperty.
	 * @param name the name of this StringProperty.
	 * @param defaultValue the default value of this StringProperty.
	 */
	public StringProperty(PropertyOwner owner, String name, String defaultValue) 
	{
		super(owner, name, defaultValue);
		
		this.stringValue = defaultValue;
	}

	/**
	 * Method to get the value of this StringProperty as a String.
	 * 
	 * @return String representation of the value.
	 */
	public String stringValue()
	{
		return stringValue;
	}
		
	/**
	 * Method to get the value of this StringProperty as a String.
	 * 
	 * @return String representation of the value.
	 */
	public String getValueAsString()
	{
		return stringValue;
	}
	
	/**
	 * Gets the value of this StringProperty as an Object.
	 * 
	 * @return the value as an Object.
	 */
	public Object getValueAsObject()
	{
		return getValueAsString();
	}	
	
	/**
	 * Sets the value of this StringProperty as a String, after first checking if the owner allows modification
	 * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.
	 * 
	 * @param value the new value of this StringProperty.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValue(final String value)
	{
		if(canSet())
		{
			boolean validationResult = false;
			
			synchronized(this)
			{
				String oldValue = stringValue;
				
            if( value != null )
            {			
      			char[] newStr = value.toCharArray();
   				int charCount = 0;
   			
   				//Remove linebreaks from the new string value
   				for(int i=0; i<newStr.length; i++)
   				{
   					if(newStr[i] == '\r' || newStr[i] == '\n' || newStr[i] == '\t' || newStr[i] == '\f')
   					{
   						// Skip all following linebreaks...
   						for(; ((i+1)<newStr.length) && (newStr[i+1] == '\r' || newStr[i+1] == '\n' || newStr[i+1] == '\t' || newStr[i+1] == '\f'); i++);
   					
   						// Replace with space, but only if not first character
   						if( (charCount > 0) )
   						{
   							newStr[charCount] = ' ';
   							charCount++;
   						}
   					}
   					else
   					{
   						newStr[charCount] = newStr[i];
   						charCount++;
   					}
   				}
   				stringValue = new String(newStr, 0, charCount);
            }
            else 
            {
               stringValue = value;
            }
            
				validationResult = validate();
			
				if(!validationResult)
            {
					stringValue = oldValue;
            }
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
	 * Sets the value of this StringProperty as a String, after first checking if the owner allows modification
	 * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.
	 * 
	 * @param value the new value of this StringProperty.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValueAsString(String value)
	{
		return setValue(value);
	}
	
	/**
	 * Sets the value of this StringProperty as an Object, after first checking if the owner allows modification
	 * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.
	 * 
	 * @param value the new value of this StringProperty.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValueAsObject(Object value)
	{
		if(value instanceof String)
			return setValue((String)value);
		else
			return setValue(value.toString());
	}
}
