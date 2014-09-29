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

import java.util.Map;

/**
 * Interface that represents a persistent storage mechanism for properties. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1 Build 545
 */
public interface PersistentPropertyStorage
{
   /**
    * Sets the name of the file used for persistent storage of properties.
    * 
    * @param propertiesFileName
    */
   public void setPersistentPropertyStorageFile(String propertiesFileName);
   
	/**
	 * Initializes this PersistentPropertyStorage.
	 * 
	 * @param pm the PropertyManager object to which this PersistentPropertyStorage is to belong to.
	 */
	public void init(PropertyManager pm);
	
	/**
	 * Reads all the properties stored persistently in the persistent storage represented by this PersistentPropertyStorage. 
	 * This method must used the method PropertyManager.addPersistentProperty() to add properties that are read from persistent storage.
	 */
	public void readProperties();
	
	/* 
	 The returned map will contain names of property groups (SubComponents and SubSystems) mapped with lists (java.lang.List objects) of properties.
	 @return a Map containing mappings  between propery group names and lists of properties.
	*/
	
	/**
	 * Writes properties to the persistent storage represented by this PersistentPropertyStorage. The map specified 
	 * by parameter <code>properties</code> contains names of property groups (SubComponents and SubSystems) mapped 
	 * with lists (java.lang.List objects) of properties.
	 * 
	 * @param properties the properties to be save to persistent storage.
	 */
	public boolean writeProperies(Map properties);
}
