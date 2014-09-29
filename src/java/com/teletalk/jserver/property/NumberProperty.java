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

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.JServerUtilities;

/**
 * Property class for handling numeric values.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public class NumberProperty extends Property
{
	/**
	 * String constant used as the default pattern when creating the DecimalFormat used for parsing numbers. The value of this 
	 * constant is "#,##0.###".
	 */
	private static final String defaultParsePattern = "#,##0.###";
	
	/**
	 * String constant used as the default pattern when creating the DecimalFormat used for formatting numbers. The value of this 
	 * constant is "0.#####".
	 */
	private static final String defaultFormatPattern = "0.#####";
	
	private static final char decimalSeparator = ',';
	
	private final boolean useLocaleSpecificNumbers;
	
	private String parsingDescription;
	
	private DecimalFormat numberParser;
	private DecimalFormat numberFormatter;

	private volatile double dVal = 0;
	private volatile long lVal = 0;
	
	private volatile boolean wasDoubleSetLast = false;
	
	/**
	 * Creates a new NumberProperty object that isn't modifiable (externally).
	 * 
	 * @param owner the owner of this NumberProperty.
	 * @param name the name of this NumberProperty.
	 * @param defaultValue the default value of this NumberProperty.
	 */
	public NumberProperty(PropertyOwner owner, String name, double defaultValue) 
	{
		this(owner, false, name, defaultValue);
	}
	
	/**
	 * Creates a new NumberProperty object that isn't modifiable (externally).
	 * 
	 * @param owner the owner of this NumberProperty.
	 * @param name the name of this NumberProperty.
	 * @param defaultValue the default value of this NumberProperty.
	 */
	public NumberProperty(PropertyOwner owner, boolean useLocaleSpecificNumbers, String name, double defaultValue) 
	{
		super(owner, name, new Double(defaultValue));
		
		this.useLocaleSpecificNumbers = useLocaleSpecificNumbers;
		
		this.createDecimalFormatObjects();
		
		this.dVal = defaultValue;
		this.lVal = (long)defaultValue;
		wasDoubleSetLast = true;
	}
	
	/**
	 * Creates a new NumberProperty object.
	 * 
	 * @param owner the owner of this NumberProperty.
	 * @param name the name of this NumberProperty.
	 * @param defaultValue the default value of this NumberProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 */
	public NumberProperty(PropertyOwner owner, String name, double defaultValue, int modificationMode) 
	{
		this(owner, false, name, defaultValue, modificationMode);
	}
	
	/**
	 * Creates a new NumberProperty object.
	 * 
	 * @param owner the owner of this NumberProperty.
	 * @param name the name of this NumberProperty.
	 * @param defaultValue the default value of this NumberProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 */
	public NumberProperty(PropertyOwner owner, boolean useLocaleSpecificNumbers, String name, double defaultValue, int modificationMode) 
	{
		super(owner, name, new Double(defaultValue), modificationMode);
		
		this.useLocaleSpecificNumbers = useLocaleSpecificNumbers;
		
		this.createDecimalFormatObjects();
		
		this.dVal = defaultValue;
		this.lVal = (long)defaultValue;
		wasDoubleSetLast = true;
	}
	
	/**
	 * Creates a new NumberProperty object.
	 * 
	 * @param owner the owner of this NumberProperty.
	 * @param name the name of this NumberProperty.
	 * @param defaultValue the default value of this NumberProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
	 */
	public NumberProperty(PropertyOwner owner, String name, double defaultValue, int modificationMode, boolean persistent) 
	{
		this(owner, false, name, defaultValue, modificationMode, persistent);
	}
	
	/**
	 * Creates a new NumberProperty object.
	 * 
	 * @param owner the owner of this NumberProperty.
	 * @param name the name of this NumberProperty.
	 * @param defaultValue the default value of this NumberProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
	 */
	public NumberProperty(PropertyOwner owner, boolean useLocaleSpecificNumbers, String name, double defaultValue, int modificationMode, boolean persistent) 
	{
		super(owner, name, new Double(defaultValue), modificationMode, persistent);
		
		this.useLocaleSpecificNumbers = useLocaleSpecificNumbers;
		
		this.createDecimalFormatObjects();
		
		this.dVal = defaultValue;
		this.lVal = (long)defaultValue;
		wasDoubleSetLast = true;
	}
	
	/**
	 * Creates a new NumberProperty object that isn't modifiable (externally).
	 * 
	 * @param owner the owner of this NumberProperty.
	 * @param name the name of this NumberProperty.
	 * @param defaultValue the default value of this NumberProperty.
	 */
	public NumberProperty(PropertyOwner owner, String name, long defaultValue) 
	{
		this(owner, false, name, defaultValue);
	}
	
	/**
	 * Creates a new NumberProperty object that isn't modifiable (externally).
	 * 
	 * @param owner the owner of this NumberProperty.
	 * @param name the name of this NumberProperty.
	 * @param defaultValue the default value of this NumberProperty.
	 */
	public NumberProperty(PropertyOwner owner, boolean useLocaleSpecificNumbers, String name, long defaultValue) 
	{
		super(owner, name, new Long(defaultValue));
		
		this.useLocaleSpecificNumbers = useLocaleSpecificNumbers;
		
		this.createDecimalFormatObjects();
		
		this.dVal = defaultValue;
		this.lVal = defaultValue;
		wasDoubleSetLast = false;
	}
	
	/**
	 * Creates a new NumberProperty object.
	 * 
	 * @param owner the owner of this NumberProperty.
	 * @param name the name of this NumberProperty.
	 * @param defaultValue the default value of this NumberProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 */
	public NumberProperty(PropertyOwner owner, String name, long defaultValue, int modificationMode) 
	{
		this(owner, false, name, defaultValue, modificationMode);
	}
	
	/**
	 * Creates a new NumberProperty object.
	 * 
	 * @param owner the owner of this NumberProperty.
	 * @param name the name of this NumberProperty.
	 * @param defaultValue the default value of this NumberProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 */
	public NumberProperty(PropertyOwner owner, boolean useLocaleSpecificNumbers, String name, long defaultValue, int modificationMode) 
	{
		super(owner, name, new Long(defaultValue), modificationMode);
		
		this.useLocaleSpecificNumbers = useLocaleSpecificNumbers;
		
		this.createDecimalFormatObjects();
		
		this.dVal = defaultValue;
		this.lVal = defaultValue;
		wasDoubleSetLast = false;
	}
	
	/**
	 * Creates a new NumberProperty object.
	 * 
	 * @param owner the owner of this NumberProperty.
	 * @param name the name of this NumberProperty.
	 * @param defaultValue the default value of this NumberProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
	 */
	public NumberProperty(PropertyOwner owner, String name, long defaultValue, int modificationMode, boolean persistent) 
	{
		this(owner, false, name, defaultValue, modificationMode, persistent);
	}
	
	/**
	 * Creates a new NumberProperty object.
	 * 
	 * @param owner the owner of this NumberProperty.
	 * @param name the name of this NumberProperty.
	 * @param defaultValue the default value of this NumberProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
	 */
	public NumberProperty(PropertyOwner owner, boolean useLocaleSpecificNumbers, String name, long defaultValue, int modificationMode, boolean persistent) 
	{
		super(owner, name, new Long(defaultValue), modificationMode, persistent);
		
		this.useLocaleSpecificNumbers = useLocaleSpecificNumbers;
		
		this.createDecimalFormatObjects();
		
		this.dVal = defaultValue;
		this.lVal = defaultValue;
		wasDoubleSetLast = false;
	}
			
	private void createDecimalFormatObjects()
	{
		if(this.useLocaleSpecificNumbers)
		{
			numberParser = new DecimalFormat();
			numberFormatter = new DecimalFormat();
		}
		else
		{
			DecimalFormatSymbols decFormatSymbols;
			
			decFormatSymbols = new DecimalFormatSymbols();
			decFormatSymbols.setDigit('#');
			decFormatSymbols.setZeroDigit('0');
			decFormatSymbols.setDecimalSeparator(NumberProperty.decimalSeparator);
			decFormatSymbols.setGroupingSeparator((char)0xA0); // Non-breaking space
			
			numberParser = new DecimalFormat(defaultParsePattern, decFormatSymbols);
			numberFormatter = new DecimalFormat(defaultFormatPattern, decFormatSymbols);
		}
		parsingDescription = "Format: " + numberParser.toLocalizedPattern();
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
				 (!JServer.getJServer().getPropertyManager().isUsingDefaultLocale() && 
				 this.useLocaleSpecificNumbers))
			{
				//If not using the default locale - reinitialize the decimal format object.
				this.createDecimalFormatObjects();
			}
		}
		
		super.initProperty(persistentProperty);
	}
	
	public String getParsingDescription()
	{
		return this.parsingDescription;
	}
	
	/**
	 * Method to get the value of this NumberProperty as a String.
	 * 
	 * @return String representation of the value.
	 */
	public synchronized String getValueAsString()
	{
		return numberFormatter.format(dVal);
	}
	
	/**
	 * Gets the value of this NumberProperty as an Object.
	 * 
	 * @return the value as an Object.
	 */
	public Object getValueAsObject()
	{
		return getValueAsNumber();
	}
	
	/**
	 * Returns a Number value.
	 * 
	 * @return a Number object.
	 */
	public Number getValueAsNumber()
	{
		if(wasDoubleSetLast)
			return new Double(dVal);
		else
			return new Long(lVal);
	}
	
	/**
	 * Returns a the value of this NumberProperty as an int value.
	 * 
	 * @return the int value.
	 */
	public int intValue()
	{
		return (int)this.lVal;
	}
	
	/**
	 * Returns a the value of this NumberProperty as an long value.
	 * 
	 * @return the long value.
	 */
	public long longValue()
	{
		return this.lVal;
	}
	
	/**
	 * Returns a the value of this NumberProperty as an float value.
	 * 
	 * @return the float value.
	 */
	public float floatValue()
	{
		return (float)this.dVal;
	}
	
	/**
	 * Returns a the value of this NumberProperty as an double value.
	 * 
	 * @return the double value.
	 */
	public double doubleValue()
	{
		return this.dVal;
	}
	
	/**
	 * Sets the value of this NumberProperty as a String, after first checking if the owner allows modification
	 * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.
	 * 
	 * @param value the new value of this NumberProperty.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValueAsString(String value)
	{
		//Replace spaces with non-braking spaces. This is primarily done to make it possible to specify number strings that contain 
		//spaces (and not just non-braking spaces), which for instance is necessary for the swedish locale where nbsp is used as a grouping 
		//separator for thousands.
		value = value.replace((char)0x20, (char)0xA0);  
		
		//Replace . with ,
		if(useLocaleSpecificNumbers) value = value.replace('.', NumberProperty.decimalSeparator);  
			
		//double newVal;
		Number newVal;
		
		try
		{
			newVal = numberParser.parse(value); //.doubleValue();
		}
		catch(Exception e)
		{
			JServerUtilities.logWarning(this.getFullName(), "Failed to parse number from '" + value + "'!");
			return false;
		}
		
		if ( (newVal instanceof Long) || (newVal instanceof Integer) || (newVal instanceof Short) || (newVal instanceof Byte) )
			return this.setValue(newVal.longValue());
		else
			return this.setValue(newVal.doubleValue());
	}
		
	/**
	 * Sets the value of this NumberProperty as an Object, after first checking if the owner allows modification
	 * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.
	 * 
	 * @param value the new value of this NumberProperty.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValueAsObject(Object value)
	{
		if(value instanceof Number)
			return setValue((Number)value);
		else
			return false;
	}
	
	/**
	 * Sets the value of this NumberProperty as a Number object, after first checking if the owner allows modification
	 * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.
	 * 
	 * @param num the new value of this NumberProperty.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValue(Number num)
	{
		if ( (num instanceof Long) || (num instanceof Integer) || (num instanceof Short) || (num instanceof Byte) )
			return this.setValue(num.longValue());
		else
			return this.setValue(num.doubleValue());
	}
		
	/**
	 * Sets the value of this NumberProperty as a long, after first checking if the owner allows modification
	 * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.
	 * 
	 * @param value the new long value.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValue(final long value)
	{
		if(canSet())
		{
			boolean validationResult = false;
			
			synchronized(this)
			{
				final double oldDVal = dVal;
				final long oldLVal = lVal;
				final boolean oldWasDoubleSetLast = wasDoubleSetLast;
					
				dVal = value;
				lVal = value;
				wasDoubleSetLast = false;
				validationResult = validate();
					
				if(!validationResult)
				{
					dVal = oldDVal;
					lVal = oldLVal;
					wasDoubleSetLast = oldWasDoubleSetLast;
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
	 * Sets the value of this NumberProperty as a double, after first checking if the owner allows modification
	 * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.
	 * 
	 * @param value the new double value.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public boolean setValue(final double value)
	{
		if(canSet())
		{
			boolean validationResult = false;
			
			synchronized(this)
			{
				final double oldDVal = dVal;
				final long oldLVal = lVal;
				final boolean oldWasDoubleSetLast = wasDoubleSetLast;
					
				dVal = value;
				lVal = (long)value;
				wasDoubleSetLast = true;
				validationResult = validate();
					
				if(!validationResult)
				{
					dVal = oldDVal;
					lVal = oldLVal;
					wasDoubleSetLast = oldWasDoubleSetLast;
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
	 */
	private boolean changeValue(long change)
	{
		if(canSet())
		{
			boolean validationResult = false;
			
			synchronized(this)
			{
				final double oldDVal = dVal;
				final long oldLVal = lVal;
				final boolean oldWasDoubleSetLast = wasDoubleSetLast;
					
				dVal = dVal + change;
				lVal = lVal + change;
				wasDoubleSetLast = false;
				validationResult = validate();
					
				if(!validationResult)
				{
					dVal = oldDVal;
					lVal = oldLVal;
					wasDoubleSetLast = oldWasDoubleSetLast;
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
	 */
	private boolean changeValue(double change)
	{
		if(canSet())
		{
			boolean validationResult = false;
			
			synchronized(this)
			{
				final double oldDVal = dVal;
				final long oldLVal = lVal;
				final boolean oldWasDoubleSetLast = wasDoubleSetLast;
					
				dVal = dVal + change;
				lVal = lVal + (long)change;
				wasDoubleSetLast = true;
				validationResult = validate();
					
				if(!validationResult)
				{
					dVal = oldDVal;
					lVal = oldLVal;
					wasDoubleSetLast = oldWasDoubleSetLast;
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
	 * Increments the value of this NumberProperty with one(1).
	 * 
	 * @return true if the value was successfully incremented.
	 */
	public boolean increment()
	{
		return this.changeValue(1);
	}
	
	/**
	 * Increments the value of this NumberProperty with i.
	 * 
	 * @return true if the value was successfully incremented.
	 */
	public boolean increment(final double i)
	{
		return this.changeValue(i);
	}
	
	/**
	 * Increments the value of this NumberProperty with i.
	 * 
	 * @return true if the value was successfully incremented.
	 */
	public boolean increment(final long i)
	{
		return this.changeValue(i);
	}
	
	/**
	 * Decrements the value of this NumberProperty with one(1).
	 * 
	 * @return true if the value was successfully decremented.
	 */
	public boolean decrement()
	{
		return this.changeValue(-1);
	}
	
	/**
	 * Decrements the value of this NumberProperty with i.
	 * 
	 * @return true if the value was successfully decremented.
	 */
	public boolean decrement(final double i)
	{
		return this.changeValue(-i);
	}
	
	/**
	 * Decrements the value of this NumberProperty with i.
	 * 
	 * @return true if the value was successfully decremented.
	 */
	public boolean decrement(final long i)
	{
		return this.changeValue(-i);
	}
}
