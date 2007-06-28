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
package com.teletalk.jserver.rmi.remote;

import java.util.HashMap;

/**
 * Transfer structure class containing information about a property. This class is used to transfer 
 * property data to the administration tool 
 * 
 * @see com.teletalk.jserver.property.Property
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 2
 */
public final class RemotePropertyData implements RemotePropertyConstants, java.io.Serializable
{
	static final long serialVersionUID = 708299874945170364L;
	
	private final String name;
	private final HashMap metaData;
	private final String value;
	private final int modificationMode;
	private final int type;
	
	/**
	 * Creates a new RemotePropertyData object.
	 * 
	 * @param name the name of the property.
	 * @param value the current value of the property.
	 * @param metaData a HashMap containing metadata about the property.
	 * @param modificationMode the modificationMode of the property.
	 * @param type the type of property (BOOLEAN_TYPE, NUMBER_TYPE, etc.).
	 */
	public RemotePropertyData(String name, String value, HashMap metaData, int modificationMode, int type)
	{
		this.name = name;
		this.value = value;
		this.metaData = metaData;
		this.modificationMode = modificationMode;
		this.type = type;
	}
	
	/**
	 * Returns the name of the property this object represents.
	 * 
	 * @return the name.
	 */	
	public String getName()
	{
		return name;	
	}
	
	/**
	 * Gets all the metadata stored in the property this object represents.
	 * 
	 * @return a HashMap containing metadata.
	 */
	public HashMap getMetaData()
	{
		return metaData;	
	}
	
	/**
	 * Get the value of the property as a String.
	 * 
	 * @return the value of this Property as a String
	 */
	public String getValue()
	{
		return value;
	}
	
	/**
	 * Gets the modificationMode.
	 * 
	 * @return the value of modificationMode.
	 */
	public int getModificationMode()
	{
		return modificationMode;
	}
   
   /**
    * Gets the description field of this Property (using the meta data key {@link com.teletalk.jserver.property.PropertyConstants#PROPERTY_DESCRIPTION_KEY}).
    * 
    * @return the description of this property or null if none was set.
    * 
    * @since 2.1 (20050607)
    */
   public String getDescription()
   {
      return (String)metaData.get(PROPERTY_DESCRIPTION_KEY);
   }
	
	/**
	 * Get the type of this property (BOOLEAN_TYPE, NUMBER_TYPE, etc.).
	 * 
	 * @return type of this property as an integer value.
	 */
	public int getType()
	{
		return type;	
	}
   
   /**
    * Gets a string representation of this object.
    */
   public String toString()
   {
      return "RemotePropertyData[name: " + name + ", value: " + value + ", modificationMode: " + modificationMode + ", type: " + type + ", metaData: " + metaData + "]";
   }
}
