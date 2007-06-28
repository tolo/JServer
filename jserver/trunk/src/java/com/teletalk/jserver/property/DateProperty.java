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

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.JServerUtilities;

/**
 * Property class for handling Date values.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public class DateProperty extends Property
{
	public static final String defaultDatePattern = "yyyy-MM-dd HH:mm:ss";
	
	private SimpleDateFormat dateFormat;
	private Date date;
	private String datePattern;
	
	private String parsingDescription;
	
	/**
	 * Creates a new DateProperty object with a default date pattern ("yyyy-MM-dd HH:mm:ss").
	 * 
	 * @param parent the owner of this DateProperty.
	 * @param name the name of this DateProperty.
	 * @param defaultValue the default value of this DateProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 */
	public DateProperty(PropertyOwner parent, String name, Date defaultValue, int modificationMode)
	{
		this(parent, name, defaultValue, modificationMode, DateProperty.defaultDatePattern);
	}
	
	/**
	 * Creates a new DateProperty object with a default date pattern ("yyyy-MM-dd HH:mm:ss").
	 * 
	 * @param parent the owner of this DateProperty.
	 * @param name the name of this DateProperty.
	 * @param defaultValue the default value of this DateProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
	 */
	public DateProperty(PropertyOwner parent, String name, Date defaultValue, int modificationMode, boolean persistent)
	{
		this(parent, name, defaultValue, modificationMode, persistent, DateProperty.defaultDatePattern);
	}
	
	/**
	 * Creates a new (externally) unmodifiable DateProperty object with a default date pattern ("yyyy-MM-dd HH:mm:ss").
	 * 
	 * @param parent the owner of this DateProperty.
	 * @param name the name of this DateProperty.
	 * @param defaultValue the default value of this DateProperty.
	 */
	public DateProperty(PropertyOwner parent, String name, Date defaultValue)
	{
		this(parent, name, defaultValue, DateProperty.defaultDatePattern);
	}
	
	/**
	 * Creates a new DateProperty object.
	 * 
	 * @param parent the owner of this DateProperty.
	 * @param name the name of this DateProperty.
	 * @param defaultValue the default value of this DateProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
	 * @param datePattern the datepattern for parsing and formatting. See the class java.text.SimpleDateFormat for valid date pattern values.
	 */
	public DateProperty(PropertyOwner parent, String name, Date defaultValue, int modificationMode, boolean persistent, String datePattern)
	{
		super(parent, name, defaultValue, modificationMode, persistent);
		
		this.date = new Date(defaultValue.getTime());
		this.datePattern = datePattern;
		
		dateFormat = (SimpleDateFormat)DateFormat.getDateTimeInstance();
		dateFormat.applyPattern(datePattern);
		parsingDescription = "Format: " + dateFormat.toLocalizedPattern();
	}
	
	/**
	 * Creates a new DateProperty object.
	 * 
	 * @param parent the owner of this DateProperty.
	 * @param name the name of this DateProperty.
	 * @param defaultValue the default value of this DateProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * @param datePattern the datepattern for parsing and formatting. See the class java.text.SimpleDateFormat for valid date pattern values.
	 */
	public DateProperty(PropertyOwner parent, String name, Date defaultValue, int modificationMode, String datePattern)
	{
		super(parent, name, defaultValue, modificationMode);
		
      this.date = new Date(defaultValue.getTime());
		this.datePattern = datePattern;
		
		dateFormat = (SimpleDateFormat)DateFormat.getDateTimeInstance();
		dateFormat.applyPattern(datePattern);
		parsingDescription = "Format: " + dateFormat.toLocalizedPattern();
	}
	
	/**
	 * Creates a new (externally) unmodifiable DateProperty object.
	 * 
	 * @param parent the owner of this DateProperty.
	 * @param name the name of this DateProperty.
	 * @param defaultValue the default value of this DateProperty.
	 * @param datePattern the datepattern for parsing and formatting. See the class java.text.SimpleDateFormat for valid date pattern values.
	 */
	public DateProperty(PropertyOwner parent, String name, Date defaultValue, String datePattern)
	{
		super(parent, name, defaultValue);
		
      this.date = new Date(defaultValue.getTime());
		this.datePattern = datePattern;
		
		dateFormat = (SimpleDateFormat)DateFormat.getDateTimeInstance();
		dateFormat.applyPattern(datePattern);
		parsingDescription = "Format: " + dateFormat.toLocalizedPattern();
	}
	
	/**
	 * Sets the date pattern used for parsing date values from strings. See the class java.text.SimpleDateFormat for valid date pattern values.
	 * 
	 * @param datePattern the new datepattern.
	 */
	public synchronized void setDatePattern(String datePattern)
	{
		dateFormat.applyPattern(datePattern);
	}
	
	/**
	 * Initializes this property and extracts data from its persistent counterpart, if any, restored 
	 * by the PropertyManager. This method also checks if the default locale has changed and, if so, takes appropriate actions.
	 * 
	 * @param persistentProperty a property read from persistent storage, <code>null</code> if there is none matching this property.
	 * 
	 * @see com.teletalk.jserver.property.PropertyManager
	 */
	public void initProperty(Property persistentProperty)
	{
		if(persistentProperty != null)
		{
			if( (JServer.getJServer() != null) && 
				(JServer.getJServer().getPropertyManager() != null) && 
				(!JServer.getJServer().getPropertyManager().isUsingDefaultLocale()) )
			{
				//If not using the default locale - reinitialize the date format object.
				dateFormat = (SimpleDateFormat)DateFormat.getDateTimeInstance();
				dateFormat.applyPattern(datePattern);		
				parsingDescription = "Format: " + dateFormat.toLocalizedPattern();
			}
		}
		
		super.initProperty(persistentProperty);
	}
	
	public String getParsingDescription()
	{
// TODO: Document!      
// TODO: Set this in metadata?      
		return this.parsingDescription;
	}
	
	/**
	 * Method to get the value of this DateProperty as a String.
	 * 
	 * @return String representation of the value.
	 */
	public synchronized String getValueAsString()
	{
		return dateFormat.format(date);
	}
	
	/**
	 * Gets the value of this DateProperty as an Object.
	 * 
	 * @return the value as an Object.
	 */
	public Object getValueAsObject()
	{
		return dateValue();
	}	
	
	/**
	 * Returns the Date value of this DateProperty.
	 * 
	 * @return a Date object.
	 */
	public synchronized Date dateValue()
	{
		return date;
	}
	
	/**
	 * Returns the date value of this DateProperty in milliseconds since January 1, 1970, 00:00:00 GMT.
	 * 
	 * @return the date in milliseconds since January 1, 1970, 00:00:00 GMT.
	 */
	public synchronized long dateValueAsMilliseconds()
	{
		return date.getTime();
	}
	
	/**
	 * Sets the value of this DateProperty as a String, after first checking if the owner allows modification
	 * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.
	 * 
	 * @param strVal the new value of this BooleanProperty.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValueAsString(String strVal)
	{
		Date newDate = null;
		
		try
		{
			newDate = dateFormat.parse(strVal);
		}
		catch(Exception e)
		{
			JServerUtilities.logWarning(this.getFullName(), "Failed to parse date from '" + strVal + "'!");
			return false;
		}
		return this.setValue(newDate);
	}
	
	/**
	 * Sets the value of this DateProperty as an Object, after first checking if the owner allows modification
	 * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.
	 * 
	 * @param value the new value of this DateProperty.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValueAsObject(Object value)
	{
		if(value instanceof Date)
      {
			return setValue((Date)value);
      }
		else
      {
			return false;
      }
	}
	
	/**
	 * Sets the value of this DateProperty as a Date object, after first checking if the owner allows modification
	 * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.
	 * 
	 * @param newDate the new value of this DateProperty.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValue(Date newDate)
	{
		return this.setValue(newDate.getTime());
	}
	
	/**
	 * Sets the value of this DateProperty as milliseconds since January 1, 1970, 00:00:00 GMT, after first checking if the owner allows modification
	 * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.
	 * 
	 * @param milliseconds the new value of this DateProperty as milliseconds since January 1, 1970, 00:00:00 GMT.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValue(long milliseconds)
	{
		if(canSet())
		{
			boolean validationResult = false;
			
			synchronized(this)
			{
				final long oldMilliseconds = date.getTime();
					
				date.setTime(milliseconds);
				validationResult = validate();
					
				if(!validationResult) date.setTime(oldMilliseconds);
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
