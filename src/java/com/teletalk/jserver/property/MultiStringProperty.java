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
 * Property class for handling one or more string values.
 * 
 * @see MultiValueProperty
 * @see Property
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1 Build 545
 */
public class MultiStringProperty extends MultiValueProperty
{
	private static final String[] StringArrayType = new String[0]; // Sting array type used in the method getStringValues
	
   /**
    * Internal method to create a MultiStringProperty.
    * 
    * @param name the name of this MultiStringProperty.
    * @param initialValues the default values of this MultiStringProperty.
    */
   protected MultiStringProperty(String name, String[] initialValues)
   {
      super(name, initialValues);
   }
      
   /**
    * Creates a new MultiStringProperty object (using the default {@link MultiValueProperty#defaulDelimeterChars delimeter chars} when representing the values of the property as a sigle string).
    * 
    * @param owner the owner of this MultiStringProperty.
    * @param name the name of this MultiStringProperty.
    * @param multiValueString the default value of this MultiStringProperty.
    * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
    * 
    * @since 2.0 (20041011)
    */
   public MultiStringProperty(PropertyOwner owner, String name, String multiValueString, int modificationMode)
   {
      super(owner, name, defaulDelimeterChars, multiValueString, modificationMode);
   }
   
   /**
    * Creates a new MultiStringProperty object (using the default {@link MultiValueProperty#defaulDelimeterChars delimeter chars} when representing the values of the property as a sigle string).
    * 
    * @param owner the owner of this MultiStringProperty.
    * @param name the name of this MultiStringProperty.
    * @param multiValueString the default value of this MultiStringProperty.
    * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
    * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
    * 
    * @since 2.0 (20041011)
    */
   public MultiStringProperty(PropertyOwner owner, String name, String multiValueString, int modificationMode, boolean persistent)
   {
      super(owner, name, defaulDelimeterChars, multiValueString, modificationMode, persistent);
   }
   
   /**
    * Creates a new MultiStringProperty object that isn't modifiable (using the default {@link MultiValueProperty#defaulDelimeterChars delimeter chars} when representing the values of the property as a sigle string). 
    * 
    * @param owner the owner of this MultiStringProperty.
    * @param name the name of this MultiStringProperty.
    * @param multiValueString the default value of this MultiStringProperty.
    * 
    * @since 2.0 (20041011)
    */
   public MultiStringProperty(PropertyOwner owner, String name, String multiValueString)
   {
      super(owner, name, defaulDelimeterChars, multiValueString);
   }
   
   /**
    * Creates a new MultiStringProperty object (using the default {@link MultiValueProperty#defaulDelimeterChars delimeter chars} when representing the values of the property as a sigle string).
    * 
    * @param owner the owner of this MultiStringProperty.
    * @param name the name of this MultiStringProperty.
    * @param initialValues the default values of this MultiStringProperty.
    * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
    * 
    * @since 2.0 (20041011)
    */
   public MultiStringProperty(PropertyOwner owner, String name, String[] initialValues, int modificationMode)
   {
      super(owner, name, defaulDelimeterChars, initialValues, modificationMode);
   }
   
   /**
    * Creates a new MultiStringProperty object (using the default {@link MultiValueProperty#defaulDelimeterChars delimeter chars} when representing the values of the property as a sigle string).
    * 
    * @param owner the owner of this MultiStringProperty.
    * @param name the name of this MultiStringProperty.
    * @param initialValues the default values of this MultiStringProperty.
    * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
    * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
    * 
    * @since 2.0 (20041011)
    */
   public MultiStringProperty(PropertyOwner owner, String name, String[] initialValues, int modificationMode, boolean persistent)
   {
      super(owner, name, defaulDelimeterChars, initialValues, modificationMode, persistent);
   }
   
   /**
    * Creates a new MultiStringProperty object that isn't modifiable (using the default {@link MultiValueProperty#defaulDelimeterChars delimeter chars} when representing the values of the property as a sigle string). 
    * 
    * @param owner the owner of this MultiStringProperty.
    * @param name the name of this MultiStringProperty.
    * @param initialValues the default values of this MultiStringProperty.
    * 
    * @since 2.0 (20041011)
    */
   public MultiStringProperty(PropertyOwner owner, String name, String[] initialValues)
   {
      super(owner, name, defaulDelimeterChars, initialValues);
   }
      
	/**
	 * Creates a new MultiStringProperty object.
	 * 
	 * @param owner the owner of this MultiStringProperty.
	 * @param name the name of this MultiStringProperty.
    * @param delimChars the set of value delimeter chars (for use when representing the values of the property as a sigle string).
	 * @param multiValueString the default value of this MultiStringProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 */
	public MultiStringProperty(PropertyOwner owner, String name, String delimChars, String multiValueString, int modificationMode)
	{
		super(owner, name, delimChars, multiValueString, modificationMode);
	}
	
	/**
	 * Creates a new MultiStringProperty object.
	 * 
	 * @param owner the owner of this MultiStringProperty.
	 * @param name the name of this MultiStringProperty.
    * @param delimChars the set of value delimeter chars (for use when representing the values of the property as a sigle string).
	 * @param multiValueString the default value of this MultiStringProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
	 */
	public MultiStringProperty(PropertyOwner owner, String name, String delimChars, String multiValueString, int modificationMode, boolean persistent)
	{
		super(owner, name, delimChars, multiValueString, modificationMode, persistent);
	}
	
	/**
	 * Creates a new MultiStringProperty object that isn't modifiable.
	 * 
	 * @param owner the owner of this MultiStringProperty.
	 * @param name the name of this MultiStringProperty.
    * @param delimChars the set of value delimeter chars (for use when representing the values of the property as a sigle string).
	 * @param multiValueString the default value of this MultiStringProperty.
	 */
	public MultiStringProperty(PropertyOwner owner, String name, String delimChars, String multiValueString)
	{
		super(owner, name, delimChars, multiValueString);
	}
	
	/**
	 * Creates a new MultiStringProperty object.
	 * 
	 * @param owner the owner of this MultiStringProperty.
	 * @param name the name of this MultiStringProperty.
    * @param delimChars the set of value delimeter chars (for use when representing the values of the property as a sigle string).
	 * @param initialValues the default values of this MultiStringProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 */
	public MultiStringProperty(PropertyOwner owner, String name, String delimChars, String[] initialValues, int modificationMode)
	{
		super(owner, name, delimChars, initialValues, modificationMode);
	}
	
	/**
	 * Creates a new MultiStringProperty object.
	 * 
	 * @param owner the owner of this MultiStringProperty.
	 * @param name the name of this MultiStringProperty.
    * @param delimChars the set of value delimeter chars (for use when representing the values of the property as a sigle string).
	 * @param initialValues the default values of this MultiStringProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
	 */
	public MultiStringProperty(PropertyOwner owner, String name, String delimChars, String[] initialValues, int modificationMode, boolean persistent)
	{
		super(owner, name, delimChars, initialValues, modificationMode, persistent);
	}
	
	/**
	 * Creates a new MultiStringProperty object that isn't modifiable.
	 * 
	 * @param owner the owner of this MultiStringProperty.
	 * @param name the name of this MultiStringProperty.
    * @param delimChars the set of value delimeter chars (for use when representing the values of the property as a sigle string).
	 * @param initialValues the default values of this MultiStringProperty.
	 */
	public MultiStringProperty(PropertyOwner owner, String name, String delimChars, String[] initialValues)
	{
		super(owner, name, delimChars, initialValues);
	}
	
	/**
	 * Gets all the <code>String</code> values in this MultiStringProperty.
	 * 
	 * @return an <code>String</code> array containing the values of this MultiValueProperty.
	 * 
	 * @since 1.2 Build 645
	 */
	public String[] getStringValues()
	{
		return (String[])super.getValues(StringArrayType);
	}
	
   /**
    * Parses a string value into an object that is to be inserted in this MultiStringProperty. This implementation simply 
    * returns the <code>valueAsString</code> parameter.
    * 
    * @param valueAsString the value to be parsed into an object.
    * 
    * @return the parsed object or <code>null</code> if an object couldn't be parsed from the specified string.
    */
	protected Object parseValue(final String valueAsString)
	{
		return valueAsString;
	}
	
   /**
    * Formats an object value into a string that is to be used when generating a string representation of the value of this 
    * MultiValueProperty. This method simply calls <code>toString()</code> on the object passed in parameter <code>value</code>.
    * 
    * @param value the value to be formatted.
    * 
    * @return the object value as a string.
    */
	protected String formatValue(final Object value)
	{
      if( value != null )
      {
         return value.toString();
      }
      else
      {
         return "";
      }
	}
}
