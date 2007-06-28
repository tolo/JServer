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
 * Property class for handling String enumeration values associated with integer indices. Note that enumeration values are treated as 
 * case insensitive.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta
 */
public class EnumProperty extends Property
{
	private final String[] enumerationValues;	
	private volatile int iVal = 0;
	
   /**
    * Creates a new EnumProperty object that isn't modifiable (externally).
    * 
    * @param owner the owner of this EnumProperty.
    * @param name the name of this EnumProperty.
    * @param initialValue the initial value of this EnumProperty.
    * @param enumerationValues the enumeration objects of this EnumProperty.
    * 
    * @throws NullPointerException if enumerationValues or an element in enumerationValues is null.
    */
   public EnumProperty(PropertyOwner owner, String name, int initialValue, String[] enumerationValues)
   {
      this(owner, name, initialValue, enumerationValues, NOT_MODIFIABLE, false);
   }
   
	/**
	 * Creates a new EnumProperty object.
	 * 
	 * @param owner the owner of this EnumProperty.
	 * @param name the name of this EnumProperty.
	 * @param initialValue the initial value of this EnumProperty.
	 * @param enumerationValues the enumeration objects of this EnumProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
    * 
    * @throws NullPointerException if enumerationValues or an element in enumerationValues is null.
    * @throws ArrayIndexOutOfBoundsException if <code>initialValue</code> is outside the bounds of <code>enumerationValues</code>
	 */
	public EnumProperty(PropertyOwner owner, String name, int initialValue, String[] enumerationValues, int modificationMode)
	{
      this(owner, name, initialValue, enumerationValues, modificationMode, true);
	}
   
   /**
    * Creates a new EnumProperty object.
    * 
    * @param owner the owner of this EnumProperty.
    * @param name the name of this EnumProperty.
    * @param initialValue the initial value of this EnumProperty.
    * @param enumerationValues the enumeration objects of this EnumProperty.
    * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
    * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
    * 
    * @throws NullPointerException if enumerationValues or an element in enumerationValues is null.
    */
   public EnumProperty(PropertyOwner owner, String name, int initialValue, String[] enumerationValues, int modificationMode, boolean persistent)
   {
      super(owner, name, null, modificationMode, persistent);
      
      iVal = initialValue;
      
      this.enumerationValues = enumerationValues;
      for(int i=0; i<this.enumerationValues.length; i++)
      {
         this.enumerationValues[i].intern();
         if( i == initialValue ) super.setDefaultValue(this.enumerationValues[i]);
      }
      
      super.putMetaData(ENUMERATIONS_KEY, enumerationValues);
   }
   
   /* ##### METHODS FROM PROPERTY BEGINS #####*/
	
	/**
	 * Gets the enumeration value of this EnumProperty.
	 * 
	 * @return the current enumeration value as a String object.
	 */
	public String getValueAsString()
	{
		return enumerationValues[iVal];
	}
	
	/**
	 * Gets the enumeration value of this EnumProperty.
	 * 
	 * @return the current enumeration value as a String object.
	 */
	public Object getValueAsObject()
	{
		return enumerationValues[iVal];
	}	
	
	/**
	 * Sets the value of this EnumProperty as a String, after first checking if the owner allows modification
	 * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.
	 * 
	 * @param value the string to be parsed.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValueAsString(final String value)
	{
		if(canSet())
		{
			int index = -1;
		
			for(int i=0; i<enumerationValues.length; i++)
			{
				if(enumerationValues[i].equalsIgnoreCase(value)) index = i;
			}
			
			if(index >= 0) return setValue(index);
			else
			{
				try
				{
					index = Integer.parseInt(value);
					return setValue(index);
				}
				catch(Exception e)
				{
					return false;	
				}
			}
		}
		
		return false;
	}
	
	/**
	 * Sets the value of this EnumProperty as an Object, after first checking if the owner allows modification
	 * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.
	 * 
	 * @param value the new value of this EnumProperty.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValueAsObject(final Object value)
	{
		if(canSet())
		{
			if( value != null )
         {
			   return this.setValueAsString(value.toString());
         }
		}
		
		return false;
	}
   
   /* ##### METHODS FROM PROPERTY ENDS #####*/
	
	/**
	 * Returns a the value of this EnumProperty as an int value, i.e. the enumeration value index (which means that this method returns  
    * the same value as {@link #getIndex()}).
	 * 
	 * @return the int value.
	 */
	public int intValue()
	{
		return iVal;
	}
   
   /**
    * Gets the index of the current enumeration value.
    * 
    * @since 2.0
    */
   public int getIndex()
   {
      return iVal;
   }
   
   /**
    * Gets the enumeration value of this EnumProperty. This method is identical to {@link #getValueAsString()}.
    * 
    * @return the current enumeration value as a String object.
    * 
    * @since 2.0
    */
   public String getValue()
   {
      return enumerationValues[iVal];
   }

	/**
	 * Sets the enumeration value of this EnumProperty, after first checking if the owner allows modification of it and validates 
    * it ok. If the value was succesfully set the owner will be notified of the change.<br>
    * <br>
    * This method will attempt to convert object passed in parameter enumValue to a string value, by calling  
    * the method toString on the object.
	 * 
	 * @param enumValue the new value.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValue(Object enumValue)
	{
		return this.setValueAsObject(enumValue);
	}
	
	/**
	 * Sets the enumeration value (index) of this EnumProperty after first checking if the owner allows modification
	 * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.
	 * 
	 * @param index the index of the new enumeration value.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValue(Integer index)
	{
		return setValue(index.intValue());
	}
	
	/**
	 * Sets the enumeration value (index) of this EnumProperty after first checking if the owner allows modification
	 * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.
	 * 
	 * @param index the index of the new enumeration value.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValue(final int index)
	{
		if(canSet())
		{
			boolean validationResult = false;
			
         if( index < this.enumerationValues.length )
         {
   			synchronized(this)
   			{
   				final int oldValue = iVal;
   					
   				iVal = index;
   				validationResult = validate();
   					
   				if(!validationResult) iVal = oldValue;
   			}
   			if(validationResult)
   			{
   				modified();
   				return true;
   			}
         }
		}
		
		return false;
	}
	
	/**
	 * Increments the enumeration value index of this EnumProperty with one(1).
	 * 
	 * @return true if the value was successfully incremented.
	 */
	public boolean increment()
	{
		return this.valueAdd(1);
	}
	
	/**
	 * Increments the enumeration value index of this EnumProperty with i.
	 * 
	 * @return true if the value was successfully incremented.
	 */
	public boolean increment(int i)
	{
		return this.valueAdd(i);
	}
	
	/**
	 * Decrements the enumeration value index of this EnumProperty with one(1).
	 * 
	 * @return true if the value was successfully decremented.
	 */
	public boolean decrement()
	{
		return this.valueAdd(-1);
	}
	
	/**
	 * Decrements the enumeration value index of this EnumProperty with i.
	 * 
	 * @return true if the value was successfully decremented.
	 */
	public boolean decrement(int i)
	{
		return this.valueAdd(-i);
	}
   
   /**
    */
   private boolean valueAdd(int change)
   {
      if(canSet())
      {
         boolean validationResult = false;
         
         synchronized(this)
         {
            final int oldValue = iVal;
               
            iVal = iVal + change;
            validationResult = validate();
               
            if(!validationResult) iVal = oldValue;
         }
         if(validationResult)
         {
            modified();
            return true;
         }
      }
      
      return false;
   }
}
