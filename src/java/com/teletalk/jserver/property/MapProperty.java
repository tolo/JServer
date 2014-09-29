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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.StringTokenizer;

/**
 * Property class for handling a list of key/value pairs.
 * 
 * @see MultiValueProperty
 * @see Property
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.2 Build 695
 */
public class MapProperty extends MultiStringProperty
{
   /** The default key value divider ('='). */
   public static final String defaultKeyValueDivider = "=";

   /** The delimiter characters. */
   protected String keyValueDivider;

   private final HashMap mapBritt = new HashMap();

   /**
    * Creates a new MapProperty object with default delimeters chars and key value divider.
    * 
    * @param owner the owner of this MultiStringProperty.
    * @param name the name of this MultiStringProperty.
    * @param initialValues the default values of this MapProperty.
    * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. 
    * If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
    */
   public MapProperty(PropertyOwner owner, String name, HashMap initialValues, int modificationMode)
   {
      this(owner, name, defaulDelimeterChars, defaultKeyValueDivider, initialValues, modificationMode);
   }

   /**
    * Creates a new MapProperty object with default delimeters chars and key value divider.
    * 
    * @param owner the owner of this MultiStringProperty.
    * @param name the name of this MultiStringProperty.
    * @param initialValues the default values of this MapProperty.
    * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. 
    * If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
    * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
    */
   public MapProperty(PropertyOwner owner, String name, HashMap initialValues, int modificationMode, boolean persistent)
   {
      this(owner, name, defaulDelimeterChars, defaultKeyValueDivider, initialValues, modificationMode, persistent);
   }

   /**
    * Creates a new MapProperty object with default delimeters chars and key value divider.
    * 
    * @param owner the owner of this MultiStringProperty.
    * @param name the name of this MultiStringProperty.
    * @param initialValues the default values of this MapProperty.
    */
   public MapProperty(PropertyOwner owner, String name, HashMap initialValues)
   {
      this(owner, name, defaulDelimeterChars, defaultKeyValueDivider, initialValues);
   }

   /**
    * Creates a new MapProperty object.
    * 
    * @param owner the owner of this MultiStringProperty.
    * @param name the name of this MultiStringProperty.
    * @param delimChars the set of value delimeter chars.
    * @param initialValues the default values of this MapProperty.
    * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. 
    * If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
    */
   public MapProperty(PropertyOwner owner, String name, String delimChars, String keyValueDivider,
         HashMap initialValues, int modificationMode)
   {
      super(owner, name, delimChars, (String[]) null, modificationMode);

      this.keyValueDivider = keyValueDivider;
      this.initMappings(initialValues);
      super.defaultValue = initialValues;
   }

   /**
    * Creates a new MapProperty object.
    * 
    * @param owner the owner of this MultiStringProperty.
    * @param name the name of this MultiStringProperty.
    * @param delimChars the set of value delimeter chars.
    * @param initialValues the default values of this MapProperty.
    * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. 
    * If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
    * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
    */
   public MapProperty(PropertyOwner owner, String name, String delimChars, String keyValueDivider,
         HashMap initialValues, int modificationMode, boolean persistent)
   {
      super(owner, name, delimChars, (String[]) null, modificationMode, persistent);

      this.keyValueDivider = keyValueDivider;
      this.initMappings(initialValues);
      super.defaultValue = initialValues;
   }

   /**
    * Creates a new MapProperty object.
    * 
    * @param owner the owner of this MultiStringProperty.
    * @param name the name of this MultiStringProperty.
    * @param delimChars the set of value delimeter chars.
    * @param initialValues the default values of this MapProperty.
    */
   public MapProperty(PropertyOwner owner, String name, String delimChars, String keyValueDivider, HashMap initialValues)
   {
      super(owner, name, delimChars, (String[]) null);

      this.keyValueDivider = keyValueDivider;
      this.initMappings(initialValues);
      super.defaultValue = initialValues;
   }
   
   /**
    * Internal method to initialize mappings.
    */
   private void initMappings(final HashMap initialValues)
   {
      if( initialValues != null )
      {
         Object[] initialValueArray = new Object[initialValues.size()];
   
         Iterator keys = initialValues.keySet().iterator();
         String key;
   
         for (int i = 0; keys.hasNext(); i++)
         {
            key = (String) keys.next();
            initialValueArray[i] = new String[] { key, (String) initialValues.get(key)};
         }
         
         super.init(initialValueArray, delimChars);
         this.mapBritt.putAll(initialValues);
      }
   }
   
   /**
    * Gets the string used for key value divider.
    */
   public synchronized String getKeyValueDivider()
   {
      return this.keyValueDivider;
   }
   
   /**
    * Sets the string used for key value divider.
    */
   public synchronized void setKeyValueDivider(String keyValueDivider)
   {
      this.keyValueDivider = keyValueDivider;
   }
   
   /**
    * Calls {@link #parseValue(String)} to parse a key/value pair. This implementation overrides that of MultiValueProperty 
    * to disable parsing of value id, which interferes with the key/value parsing of this property.
    * 
    * @since 2.0.1 (20051216)
    */
   protected Object defaultParseValue(String valueAsString)
   {
      return parseValue(valueAsString);
   }
      
   /**
    * Parses a string value into an key/value pair (String[] object) that is to be inserted in this
    * MapProperty. This method is used by the methods {@link MultiValueProperty#setValueAsString(String)}and
    * {@link MultiValueProperty#setValuesAsStrings(String[])}.
    * 
    * @param valueAsString the value to be parsed into an IpAndPortEndPointIdentifier object.
    * 
    * @return the parsed String[] object or <code>null</code> if an object couldn't be parsed from
    *                the specified string.
    */
   protected Object parseValue(final String valueAsString) 
   {
      if (valueAsString != null)
      {
         StringTokenizer tokenizer = new StringTokenizer(valueAsString, this.keyValueDivider);
         String key = "";
         String value = "";

         if (tokenizer.hasMoreTokens())
         {
            key = tokenizer.nextToken().trim();

            if (tokenizer.hasMoreTokens())
            {
               value = "";
               while (tokenizer.hasMoreTokens())
               {
                  value += tokenizer.nextToken().trim();
               }
            }
         }

         //this.mapBritt.put(key, value);
         return new String[] {key, value};
      }
      else return null;
   }

   /**
    * Formats an key/value pair (String[] object) value into a string that is to be used when generating a string
    * representation of the value of this MapProperty. This method is used by the methods
    * {@link MultiValueProperty#getValueAsString()}and {@link MultiValueProperty#getValuesAsStrings()}.
    * 
    * @param value the value to be formatted.
    * 
    * @return the String[] object value as a string.
    */
   protected String formatValue(final Object value)
   {
      String[] keyValuePair = (String[]) value;

      return keyValuePair[0] + this.keyValueDivider + keyValuePair[1];
   }

   /**
    * Returns a java.util.Set object containing all the keys of this MapProperty.
    */
   public synchronized Set keySet() 
   {
      return this.mapBritt.keySet();
   }
   
   /**
    * Gets a HashMap containing all the mappings in this MapProperty.
    */
   public synchronized HashMap getMappings()
   {
      return (HashMap)this.mapBritt.clone();
   }

   /**
    * Gets the value for the specified key.
    *  
    * @param key the key whose associated value is to be returned.
     * @return the value to which this MapProperty maps the specified key, or
     *          <code>null</code> if there is no mapping for the specified key.
    */
   public synchronized String get(final String key)
   {
      return (String) this.mapBritt.get(key);
   }

   /**
    * Associates the specified value with the specified key in this MapProperty.
     * If this MapProperty previously contained a mapping for the specified key, the old value is replaced.
    * 
    * @param key key with which the specified value is to be associated.
    * @param value value to be associated with the specified key.
    * @return previous value associated with specified key, or <code>null</code>
    *          if there was no mapping for key.
    */
   public synchronized Object put(final String key, final String value)
   {
      Object oldValue = this.mapBritt.get(key); 
      if( super.addValue(new String[] {key, value}) )
      {
         return oldValue;
      }
      else return null;
   }
   
   /**
    * Called to indicate that the value of this property has changed.
    */
   protected void modified()
   {
      // Repopulate mappings
      this.mapBritt.clear();
      Object[] values = super.getValuesAsObjects();
      if( values != null )
      {
         for (int i=0; i<values.length; i++)
         {
            String[] keyValuePair = (String[])values[i];
            this.mapBritt.put(keyValuePair[0], keyValuePair[1]);
         }
      }
      
      super.modified();
   }
}
