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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

import com.teletalk.jserver.JServerUtilities;

/**
 * Property class for handling multiple values. When represented as a String the values of the MultiValueProperty are
 * separated by a delimiter character, that is specified at creation.
 * 
 * @see Property
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1 Build 545
 */
public abstract class MultiValueProperty extends Property
{
   /** The default delimiter characters (';'). @since 2.0 (20041011) */
   public static final String defaulDelimeterChars = ";";
   
   /** The default string used to separate the value id part from the actual value */
   public static final String DefaultValueIdSeparator = "=";
   
   
   /** The delimiter characters. */
   protected final String delimChars;

   /** The values in this MultiValueProperty. */
   protected ArrayList values;

   /** Maps values to ids. @since 2.0 */
   protected HashMap valueToId;
   
   
   /**
    * Creates a new MultiValueProperty object without delimeters. This constructor is primarily for internal use.
    * 
    * @param name the name of this MultiValueProperty.
    * @param initialValues the default value of this MultiValueProperty.
    * 
    * @since 2.0 (20041011)
    */
   protected MultiValueProperty(String name, Object[] initialValues)
   {
      super(name, (initialValues != null) ? initialValues : new Object[0]);

      this.delimChars = null; // Final

      this.init(initialValues, null, false);
   }

   /**
    * Creates a new MultiValueProperty object (using the default {@link #defaulDelimeterChars delimeter chars}when
    * representing the values of the property as a sigle string).
    * 
    * @param owner the owner of this MultiValueProperty.
    * @param name the name of this MultiValueProperty.
    * @param multiValueString the default value of this MultiValueProperty.
    * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values.
    *                If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
    * 
    * @since 2.0 (20041011)
    */
   public MultiValueProperty(PropertyOwner owner, String name, String multiValueString,
         int modificationMode)
   {
      super(owner, name, (multiValueString != null) ? (Object)multiValueString : new Object[0], modificationMode);

      this.delimChars = defaulDelimeterChars; // Final

      this.init(multiValueString, delimChars);
   }

   /**
    * Creates a new MultiValueProperty object (using the default {@link #defaulDelimeterChars delimeter chars}when
    * representing the values of the property as a sigle string).
    * 
    * @param owner the owner of this MultiValueProperty.
    * @param name the name of this MultiValueProperty.
    * @param multiValueString the default value of this MultiValueProperty.
    * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values.
    *                If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
    * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property
    *                file.
    * 
    * @since 2.0 (20041011)
    */
   public MultiValueProperty(PropertyOwner owner, String name, String multiValueString,
         int modificationMode, boolean persistent)
   {
      super(owner, name, (multiValueString != null) ? (Object)multiValueString : new Object[0], modificationMode, persistent);

      this.delimChars = defaulDelimeterChars; // Final

      this.init(multiValueString, delimChars);
   }

   /**
    * Creates a new MultiValueProperty object that isn't modifiable (using the default
    * {@link #defaulDelimeterChars delimeter chars}when representing the values of the property as a sigle string).
    * 
    * @param owner the owner of this MultiValueProperty.
    * @param name the name of this MultiValueProperty.
    * @param multiValueString the default value of this MultiValueProperty.
    * 
    * @since 2.0 (20041011)
    */
   public MultiValueProperty(PropertyOwner owner, String name, String multiValueString)
   {
      super(owner, name, (multiValueString != null) ? (Object)multiValueString : new Object[0]);

      this.delimChars = defaulDelimeterChars; // Final

      this.init(multiValueString, delimChars);
   }

   /**
    * Creates a new MultiValueProperty object (using the default {@link #defaulDelimeterChars delimeter chars}when
    * representing the values of the property as a sigle string).
    * 
    * @param owner the owner of this MultiValueProperty.
    * @param name the name of this MultiValueProperty.
    * @param initialValues the default value of this MultiValueProperty.
    * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values.
    *                If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
    * 
    * @since 2.0 (20041011)
    */
   public MultiValueProperty(PropertyOwner owner, String name, Object[] initialValues,
         int modificationMode)
   {
      super(owner, name, (initialValues != null) ? initialValues : new Object[0], modificationMode);

      this.delimChars = defaulDelimeterChars; // Final

      this.init(initialValues, delimChars);
   }

   /**
    * Creates a new MultiValueProperty object (using the default {@link #defaulDelimeterChars delimeter chars}when
    * representing the values of the property as a sigle string).
    * 
    * @param owner the owner of this MultiValueProperty.
    * @param name the name of this MultiValueProperty.
    * @param initialValues the default value of this MultiValueProperty.
    * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values.
    *                If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
    * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property
    *                file.
    * 
    * @since 2.0 (20041011)
    */
   public MultiValueProperty(PropertyOwner owner, String name, Object[] initialValues,
         int modificationMode, boolean persistent)
   {
      super(owner, name, (initialValues != null) ? initialValues : new Object[0], modificationMode, persistent);

      this.delimChars = defaulDelimeterChars; // Final

      this.init(initialValues, delimChars);
   }

   /**
    * Creates a new MultiValueProperty object that isn't modifiable (using the default
    * {@link #defaulDelimeterChars delimeter chars}when representing the values of the property as a sigle string).
    * 
    * @param owner the owner of this MultiValueProperty.
    * @param name the name of this MultiValueProperty.
    * @param initialValues the default value of this MultiValueProperty.
    * 
    * @since 2.0 (20041011)
    */
   public MultiValueProperty(PropertyOwner owner, String name, Object[] initialValues)
   {
      super(owner, name, (initialValues != null) ? initialValues : new Object[0]);

      this.delimChars = defaulDelimeterChars; // Final

      this.init(initialValues, delimChars);
   }

   /**
    * Creates a new MultiValueProperty object.
    * 
    * @param owner the owner of this MultiValueProperty.
    * @param name the name of this MultiValueProperty.
    * @param delimChars the set of value delimeter chars (for use when representing the values of the property as a
    *                sigle string).
    * @param multiValueString the default value of this MultiValueProperty.
    * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values.
    *                If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
    */
   public MultiValueProperty(PropertyOwner owner, String name, String delimChars, String multiValueString,
         int modificationMode)
   {
      super(owner, name, (multiValueString != null) ? (Object)multiValueString : new Object[0], modificationMode);

      this.delimChars = delimChars; // Final

      this.init(multiValueString, delimChars);
   }

   /**
    * Creates a new MultiValueProperty object.
    * 
    * @param owner the owner of this MultiValueProperty.
    * @param name the name of this MultiValueProperty.
    * @param delimChars the set of value delimeter chars (for use when representing the values of the property as a
    *                sigle string).
    * @param multiValueString the default value of this MultiValueProperty.
    * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values.
    *                If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
    * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property
    *                file.
    */
   public MultiValueProperty(PropertyOwner owner, String name, String delimChars, String multiValueString,
         int modificationMode, boolean persistent)
   {
      super(owner, name, (multiValueString != null) ? (Object)multiValueString : new Object[0], modificationMode, persistent);

      this.delimChars = delimChars; // Final

      this.init(multiValueString, delimChars);
   }

   /**
    * Creates a new MultiValueProperty object that isn't modifiable.
    * 
    * @param owner the owner of this MultiValueProperty.
    * @param name the name of this MultiValueProperty.
    * @param delimChars the set of value delimeter chars (for use when representing the values of the property as a
    *                sigle string).
    * @param multiValueString the default value of this MultiValueProperty.
    */
   public MultiValueProperty(PropertyOwner owner, String name, String delimChars, String multiValueString)
   {
      super(owner, name, (multiValueString != null) ? (Object)multiValueString : new Object[0]);

      this.delimChars = delimChars; // Final

      this.init(multiValueString, delimChars);
   }

   /**
    * Creates a new MultiValueProperty object.
    * 
    * @param owner the owner of this MultiValueProperty.
    * @param name the name of this MultiValueProperty.
    * @param delimChars the set of value delimeter chars (for use when representing the values of the property as a
    *                sigle string).
    * @param initialValues the default value of this MultiValueProperty.
    * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values.
    *                If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
    */
   public MultiValueProperty(PropertyOwner owner, String name, String delimChars, Object[] initialValues,
         int modificationMode)
   {
      super(owner, name, (initialValues != null) ? initialValues : new Object[0], modificationMode);

      this.delimChars = delimChars; // Final

      this.init(initialValues, delimChars);
   }

   /**
    * Creates a new MultiValueProperty object.
    * 
    * @param owner the owner of this MultiValueProperty.
    * @param name the name of this MultiValueProperty.
    * @param delimChars the set of value delimeter chars (for use when representing the values of the property as a
    *                sigle string).
    * @param initialValues the default value of this MultiValueProperty.
    * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values.
    *                If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
    * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property
    *                file.
    */
   public MultiValueProperty(PropertyOwner owner, String name, String delimChars, Object[] initialValues,
         int modificationMode, boolean persistent)
   {
      super(owner, name, (initialValues != null) ? initialValues : new Object[0], modificationMode, persistent);

      this.delimChars = delimChars; // Final

      this.init(initialValues, delimChars);
   }

   /**
    * Creates a new MultiValueProperty object that isn't modifiable.
    * 
    * @param owner the owner of this MultiValueProperty.
    * @param name the name of this MultiValueProperty.
    * @param delimChars the set of value delimeter chars (for use when representing the values of the property as a
    *                sigle string).
    * @param initialValues the default value of this MultiValueProperty.
    */
   public MultiValueProperty(PropertyOwner owner, String name, String delimChars, Object[] initialValues)
   {
      super(owner, name, (initialValues != null) ? initialValues : new Object[0]);

      this.delimChars = delimChars; // Final

      this.init(initialValues, delimChars);
   }

   /**
    * Internal initialization method.
    */
   private void init(final String multiValueString, final String delimChars) throws IllegalArgumentException
   {
      this.init(multiValueString, delimChars, true);
   }

   /**
    * Internal initialization method.
    */
   private void init(final String multiValueString, final String delimChars, boolean useDelimChars)
         throws IllegalArgumentException
   {
      if (useDelimChars && (delimChars.length() == 0)) throw new IllegalArgumentException("No delimeter chars specified!");

      values = new ArrayList();

      if (multiValueString != null)
      {
         this.setForceMode(true);
         this.setNotificationMode(false);
         setValueAsString(multiValueString);
         this.setNotificationMode(true);
         this.setForceMode(false);
      }

      if (useDelimChars)
      {
         super.putMetaData(PropertyConstants.DELIMETER_CHARS_KEY, delimChars);
      }
   }

   /**
    * Internal initialization method.
    */
   protected void init(final Object[] initialValues, final String delimChars) throws IllegalArgumentException
   {
      this.init(initialValues, delimChars, true);
   }

   /**
    * Internal initialization method.
    */
   protected void init(final Object[] initialValues, final String delimChars, boolean useDelimChars)
         throws IllegalArgumentException
   {
      if (useDelimChars && (delimChars.length() == 0))
            throw new IllegalArgumentException("No delimeter chars specified!");

      this.values = new ArrayList();

      if (initialValues != null)
      {
         // Don't add null values
         for (int i = 0; i < initialValues.length; i++)
         {
            if (initialValues[i] != null)
            {
               this.values.add(initialValues[i]);
            }
         }
      }

      if (useDelimChars)
      {
         super.putMetaData(PropertyConstants.DELIMETER_CHARS_KEY, delimChars);
      }
   }
   
   /**
    * Removes all the values in this MultiValueProperty.
    * 
    * @since 1.3.2
    */
   public synchronized boolean clear()
   {
      return this.setValue(new Object[0]);
   }

   /**
    * Returns the number of values in this MultiValueProperty.
    * 
    * @return the number of values in this MultiValueProperty.
    */
   public synchronized int size()
   {
      return this.values.size();
   }

   /**
    * Gets all the values in this MultiValueProperty.
    * 
    * @return an array containing the values of this MultiValueProperty.
    */
   public synchronized Object[] getValues()
   {
      return this.values.toArray();
   }

   /**
    * Gets all the values in this MultiValueProperty. The runtime type of the returned array will be that of the
    * specified array. If the values fits in the specified array, it is returned therein. Otherwise, a new array is
    * allocated with the runtime type of the specified array.
    * 
    * @param array the array into which the values of the MultiValueProperty are to be stored, if it is big enough.
    * Otherwise, a new array of the same runtime type is allocated for this purpose.
    * 
    * @return an array containing the values of this MultiValueProperty.
    * 
    * @since 1.2 Build 645
    */
   public synchronized Object[] getValues(final Object[] array)
   {
      return this.values.toArray(array);
   }

   /**
    * Gets all the values in this MultiValueProperty.
    * 
    * @return a list containing the values of this MultiValueProperty.
    */
   public synchronized List getValuesAsList()
   {
      return new ArrayList(this.values);
   }

   /**
    * Method to get the value of this MultiValueProperty as a string. The returned string will contain all the values in
    * this MultiValueProperty separated by the first delimeter char.
    * 
    * @return String representation of the values.
    */
   public String getValueAsString()
   {
      final Object[] valueArray = getValues();
      final StringBuffer result = new StringBuffer();

      for (int i = 0; i < valueArray.length; i++)
      {
         result.append(defaultFormatValue(valueArray[i]));

         if (i < (valueArray.length - 1))
         {
            if (delimChars != null) // delimChars should never be null unless when this object was
                                                    // created with protected constructor
            {
               result.append(this.delimChars.charAt(0));
            }
         }
      }

      return result.toString();
   }

   /**
    * Method to get the values of this MultiValueProperty as an array of strings.
    * 
    * @return String array representation of the values.
    */
   public String[] getValuesAsStrings()
   {
      final Object[] valueArray = getValues();
      final String[] result = new String[(valueArray != null) ? valueArray.length : 0];

      for (int i = 0; i < valueArray.length; i++)
      {
         result[i] = defaultFormatValue(valueArray[i]);
      }

      return result;
   }

   /**
    * Gets the value of this MultiValueProperty as an Object, or to be specific, an array of objects (Object[]).
    * 
    * @return the value as an Object.
    */
   public synchronized Object getValueAsObject()
   {
      return this.values.toArray();
   }
   
   /**
    * Gets the values of this MultiValueProperty as an Object array.
    * 
    * @since 2.1.4 (20060503)
    */
   public synchronized Object[] getValuesAsObjects()
   {
      return this.values.toArray();
   }

   /**
    * Sets the value of this MultiValueProperty as a String, after first checking if the owner allows modification of it
    * and validates it ok. If the value was succesfully set the owner will be notified of the change. Tokens will be
    * parsed from the specified string using the delimeter chars.
    * 
    * @param value the new value of this MultiValueProperty.
    * 
    * @return true if the value was successfully set, otherwise false.
    */
   public boolean setValueAsString(final String value)
   {
      if (canSet())
      {
         Object objValue;
         boolean validationResult = false;

         synchronized (this)
         {
            ArrayList oldValues = this.values;
            ArrayList newValues = new ArrayList();

            if (value != null)
            {
               try
               {
                  final StringTokenizer tokenizer = (this.delimChars != null) ? new StringTokenizer(value, this.delimChars) : new StringTokenizer(value, "");

                  for (int i = 0; (tokenizer.hasMoreTokens()); i++)
                  {
                     objValue = this.defaultParseValue(tokenizer.nextToken());

                     if (objValue != null)
                     {
                        newValues.add(objValue);
                     }
                     else
                     {
                        JServerUtilities.logWarning(super.getFullName(), "Failed to parse values from string '" + value
                              + "'. Element " + i + " failed.");
                        return false;
                     }
                  }
               }
               catch (Exception e)
               {
                  JServerUtilities.logWarning(super.getFullName(),
                        "Failed to parse values from string '" + value + "'", e);
                  return false;
               }
            }

            this.values = newValues;
            validationResult = validate();

            if (!validationResult)
            {
               this.values = oldValues;
               newValues.clear();
               newValues = null;
            }
            else
            {
               oldValues.clear();
               oldValues = null;
            }
         }
         if (validationResult)
         {
            modified();
            return true;
         }
      }

      return false;
   }

   /**
    * Sets the value of this property as an array of strings.
    * 
    * @since 1.3.2
    */
   public boolean setValuesAsStrings(final String[] values)
   {
      if (canSet())
      {
         Object objValue;
         boolean validationResult = false;

         synchronized (this)
         {
            ArrayList oldValues = this.values;
            ArrayList newValues = new ArrayList();

            if (values != null)
            {
               try
               {
                  for (int i = 0; i < values.length; i++)
                  {
                     objValue = this.defaultParseValue(values[i]);

                     if (objValue != null)
                     {
                        newValues.add(objValue);
                     }
                     else
                     {
                        JServerUtilities.logWarning(super.getFullName(), "Failed to parse values! Element " + i
                              + " failed.");
                        return false;
                     }
                  }
               }
               catch (Exception e)
               {
                  JServerUtilities.logWarning(super.getFullName(), "Failed to parse values!", e);
                  return false;
               }
            }

            this.values = newValues;
            validationResult = validate();

            if (!validationResult)
            {
               this.values = oldValues;
               newValues.clear();
               newValues = null;
            }
            else
            {
               oldValues.clear();
               oldValues = null;
            }
         }
         if (validationResult)
         {
            modified();
            return true;
         }
      }

      return false;
   }

   /**
    * Sets the value of this MultiValueProperty as an Object, after first checking if the owner allows modification of
    * it and validates it ok. If the value was succesfully set the owner will be notified of the change.
    * 
    * @param value
    *                   the new value of this MultiValueProperty.
    * 
    * @return <code>true</code> if the value was successfully set, otherwise <code>false</code>.
    */
   public boolean setValueAsObject(final Object value)
   {
      if (value instanceof String)
      {
         return setValueAsString((String) value);
      }
      else if (value instanceof Object[])
      {
         return setValue((Object[]) value);
      }
      else
      {
         return false;
      }
   }
   
   /**
    * Sets a new value, consisting of an array of objects, for this MultiValueProperty.
    * 
    * @param values an array of MultiValueProperty objects.
    * 
    * @return <code>true</code> if the value was successfully set, otherwise <code>false</code>.
    * 
    * @since 2.1.4 (20060503)
    */
   public boolean setValuesAsObjects(final Object[] values)
   {
      return setValue(values);
   }

   /**
    * Sets a new value, consisting of a list of objects, for this MultiValueProperty.
    * 
    * @param valueList
    *                   a list of MultiValueProperty objects.
    * 
    * @return <code>true</code> if the value was successfully set, otherwise <code>false</code>.
    */
   public boolean setValue(final List valueList)
   {
      return setValue(valueList.toArray());
   }

   /**
    * Sets a new value, consisting of an array of objects, for this MultiValueProperty.
    * 
    * @param valueArray
    *                   an array of MultiValueProperty objects.
    * 
    * @return <code>true</code> if the value was successfully set, otherwise <code>false</code>.
    */
   public synchronized boolean setValue(final Object[] valueArray)
   {
      if (canSet())
      {
         boolean validationResult = false;

         synchronized (this)
         {
            ArrayList oldValues = this.values;
            ArrayList newValues;

            if (valueArray != null) newValues = new ArrayList(Arrays.asList(valueArray));
            else newValues = new ArrayList();

            this.values = newValues;
            validationResult = validate();

            if (!validationResult) this.values = oldValues;
         }
         if (validationResult)
         {
            modified();
            return true;
         }
      }
      return false;
   }

   /**
    * Gets the value in this MultiValueProperty at the specified index.
    * 
    * @return the value in this MultiValueProperty at the specified index.
    */
   public synchronized Object getValue(final int index)
   {
      if (index < this.values.size())
         return this.values.get(index);
      else
         return null;
   }

   /**
    * Adds a value to this MultiValueProperty.
    *  
    * @return <code>true</code> if the value was successfully added, otherwise <code>false</code>.
    */
   public boolean addValue(final Object value)
   {
      if (canSet())
      {
         if (value == null) return false;
         boolean validationResult = false;

         synchronized (this)
         {
            ArrayList oldValues = new ArrayList(this.values);

            this.values.add(value);
            validationResult = validate();

            if (!validationResult) this.values = oldValues;
         }
         if (validationResult)
         {
            modified();
            return true;
         }
      }

      return false;
   }

   /**
    * Removes a value from this MultiValueProperty.
    * 
    * @param value a value to remove.
    * 
    * @return <code>true</code> if the value was successfully removed, otherwise <code>false</code>.
    */
   public boolean removeValue(final Object value)
   {
      if (canSet())
      {
         if (value == null) return false;
         boolean validationResult = false;

         synchronized (this)
         {
            ArrayList oldValues = new ArrayList(this.values);

            this.values.remove(value);
            validationResult = validate();

            if (!validationResult) this.values = oldValues;
         }
         if (validationResult)
         {
            modified();
            return true;
         }
      }

      return false;
   }
   
	/**
	 * Checks if this property is using its default value.
	 * 
	 * @since 2.0 Build 757
	 */
   public boolean isUsingDefaultValue()
   {
      if( super.defaultValue instanceof String ) return super.defaultValue.equals(this.getValueAsString());
      else if( super.defaultValue instanceof Object[]) 
      {
         Object[] defaultValues = (Object[])super.defaultValue;
         Object[] existingValues = this.getValues();
         
         if( (defaultValues == null) && (existingValues != null) ) return false;
         if( (defaultValues != null) && (existingValues == null) ) return false;
         if( (defaultValues != null) && (existingValues != null) )
         {
            if( defaultValues.length != existingValues.length ) return false;
            
            for(int i=0; i<defaultValues.length; i++)
            {
               if( (defaultValues[i] == null) && (existingValues[i] != null) ) return false;
               else if( (defaultValues[i] != null) && (existingValues[i] == null) ) return false;
               else if( (defaultValues[i] != null) && (existingValues[i] != null) && (!defaultValues[i].equals(existingValues[i])) ) return false;
            }
         }
         
         return true;
      }
      else return super.isUsingDefaultValue();
   }
   
   /**
    * Gets the map containing value-to-id mappings. The returned map is not cloned.
    * 
    * @since 2.0.1 (20040913)
    */
   public HashMap getValueIds()
   {
      synchronized(this.valueToId)
      {
         return this.valueToId;
      }
   }
   
   /**
    * Gets the configured id for a specific value in this multi value property.<br>
    * <br>
    * Ids for values are by default specified using the '=' character as a separator in the configuration file.
    *  
    * @param value the value to get the id for.
    * 
    * @since 2.0.1 (20040913)
    */
   public String getValueId(final Object value)
   {
      synchronized(this)
      {
         if( this.valueToId != null )
         {
            return (String)this.valueToId.get(value);
         }
         else return null;
      }
   }
   
   /**
    * Sets the id for a specific value in this multi value property.<br>
    * <br>
    * Ids for values are by default specified using the '=' character as a separator in the configuration file.
    * 
    * @param value the value to set the id for.
    * @param id the id with which the value should be associated with.
    * 
    * @since 2.0.1 (20040913)
    */
   public void setValueId(final Object value, final String id)
   {
      synchronized(this)
      {
         if( this.valueToId == null ) this.valueToId = new HashMap();
         this.valueToId.put(value, id);
      }
   }
   
   /**
    * Default value parsing implementation that attempts to parse a value id from the specified 
    * valueAsString parameter, before calling {@link #parseValue(String)} to parse the actual value.
    * 
    * @since 2.0.1 (20040913)
    */
   protected Object defaultParseValue(String valueAsString)
   {
      int separatorIndex = valueAsString.indexOf(DefaultValueIdSeparator);
      String valueId = null;
      
      if( separatorIndex > 0 )
      {
         valueId = valueAsString.substring(0, separatorIndex);
         valueAsString = valueAsString.substring(separatorIndex + 1);
      }
            
      Object value = parseValue(valueAsString);
      
      this.setValueId(value, valueId);
      
      return value;
   }
   
   /**
    * Default value formatting implementation that attempts to get a matching id for the specified value and add it 
    * to the returned string along with the result of {@link #formatValue(Object)}.
    * 
    * @since 2.0.1 (20040913)
    */
   protected String defaultFormatValue(Object value)
   {
      String valueId = this.getValueId(value);
      if( valueId != null ) return valueId + DefaultValueIdSeparator + this.formatValue(value);
      else return this.formatValue(value);
   }

   /**
    * Parses a string value into an object that is to be inserted in this MultiValueProperty. This method is used by the methods 
    * {@link #setValueAsString(String)} and {@link #setValuesAsStrings(String[])}.
    * 
    * @param valueAsString the value to be parsed into an object.
    * 
    * @return the parsed object or <code>null</code> if an object couldn't be parsed from the specified string.
    */
   protected abstract Object parseValue(String valueAsString);

   /**
    * Formats an object value into a string that is to be used when generating a string representation of the value of this 
    * MultiValueProperty. This method is used by the methods {@link #getValueAsString()} and {@link #getValuesAsStrings()}.
    * 
    * @param value the value to be formatted.
    * 
    * @return the object value as a string.
    */
   protected abstract String formatValue(Object value);
}