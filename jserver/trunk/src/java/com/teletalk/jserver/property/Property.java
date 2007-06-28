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

import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Locale;

import com.teletalk.jserver.JServerConstants;
import com.teletalk.jserver.JServerUtilities;
import com.teletalk.jserver.rmi.adapter.PropertyRmiAdapter;
import com.teletalk.jserver.rmi.adapter.RmiAdapter;
import com.teletalk.jserver.rmi.adapter.VectorPropertyRmiAdapter;

/**
 * Abstract class for representing a property that allows for interaction both inside and
 * outside the server in a flexible way. One of the primary resons for the existence of this class is
 * that it facilitates administration of the server.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public abstract class Property implements PropertyConstants
{
	/** The name of this Property. */
	private volatile String name;
	
	/**  @since 2.0 Build 757 */
	protected Object defaultValue;
	
	/** Hash table containing optional metadata for this Property. */
	protected final HashMap metaData;
	
	/**  @since 2.0 Build 757 */
	protected boolean persistentMetaData;
		
	/** Flag indicating whether or not this Property is modifiable and how it should react to modifications. @see PropertyConstants */
	protected volatile int modificationMode;
	
	/** Flag indicating whether or not this Property will be saved to persistent storage. This flag is set to <code>true</code> by default. */
	protected volatile boolean persistent;
	
	/** The owner of this Property. */
	protected volatile PropertyOwner owner;
	
	/** The associated adapter for RMI communication. */
	protected RmiAdapter rmiAdapter;
	
	/** Flag indicating if the owner of this Property will be notified of changes. */
	protected volatile boolean notificationModeEnabled;
		
	protected Locale currentLocale;
		
	private volatile boolean initializing = false;
	
	private volatile boolean initialized = false;
	
	private volatile boolean forceModeEnabled = false;
   
   private volatile long lastModified = System.currentTimeMillis();
	
	/** Boolean flag indicating if this Property has been removed. */
	protected boolean removed = false; 
   	
	/** Internal lock. */
	protected final Object componentLock = new Object();
   
	
   /**
    * Internal method to create a new Property object.
    * 
    * @param name the name of this Property.
    * 
    * @see PropertyConstants
    */
   protected Property(String name, Object defaultValue)
   {
      notificationModeEnabled = true;
            
      this.owner = null;
      this.name = validateName(name).intern();
      this.defaultValue = defaultValue;
      
      this.metaData = new HashMap();
      
      this.modificationMode = MODIFICATION_MODE_NOT_SET;
      
      this.persistent = true;
   }
   
	/**
	 * Creates a new Property object.
	 * 
	 * @param owner the owner of this Property.
	 * @param name the name of this Property.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
	 * 
	 * @see PropertyConstants
	 */
	protected Property(PropertyOwner owner, String name, Object defaultValue, int modificationMode, boolean persistent)
	{
		notificationModeEnabled = true;
				
		this.owner = owner;
		this.name = validateName(name).intern();
		this.defaultValue = defaultValue;
		
		this.metaData = new HashMap();
		
		if((modificationMode >= MODIFIABLE_NO_RESTART) && (modificationMode <= NOT_MODIFIABLE))
      {
			this.modificationMode = modificationMode;
      }
		else
      {
			this.modificationMode = NOT_MODIFIABLE;
      }
		
		this.persistent = persistent;
	}
		
	/**
	 * Creates a new persistent Property object.
	 * 
	 * @param owner the owner of this Property.
	 * @param name the name of this Property.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * 
	 * @see PropertyConstants
	 */
	protected Property(PropertyOwner owner, String name, Object defaultValue, int modificationMode)
	{
		this(owner, name, defaultValue, modificationMode, true);
	}
			
	/**
	 * Creates a new non-persistent Property object that isn't modifiable (externally).
	 * 
	 * @param owner the owner of this Property.
	 * @param name the name of this Property.
	 */
	protected Property(PropertyOwner owner, String name, Object defaultValue)
	{
		this(owner, name, defaultValue, NOT_MODIFIABLE, false);
	}
	
	/**
	 * Validates the name of this property.
	 */
	private String validateName(String requestedName)
	{
		String newName = requestedName;
		
		if( (newName == null) || (newName.equals("")) )
		{
			String clsName = this.getClass().getName(); 
			if(clsName.lastIndexOf(".") > 0) newName = clsName.substring(clsName.lastIndexOf("."));
			else newName = clsName;
			newName = newName + System.identityHashCode(this);
			
			JServerUtilities.logWarning(JServerConstants.JSERVER_TOP_SYSTEM_ALIAS, "No name specified for property. Property is now named '" + newName + "'.");
		}
		else
		{
			newName = newName.trim();
			boolean invalidName = false;
			
			if(newName.length() > 100)
			{
				newName = newName.substring(0, 100);
				invalidName = true;
			}
			
			char[] illegalChars = {'.', '\r', '\n', '\t', '\f', '='};
			
			for(int i=0; i<illegalChars.length; i++)
			{
				if(newName.indexOf(illegalChars[i]) >= 0)
				{
					newName = newName.replace(illegalChars[i], '_');
					invalidName = true;
				}
			}
			
			String fName;
			if(owner != null) fName = owner.getFullName() + "." + newName;
			else fName = newName;
			
			if(invalidName)
			{
				JServerUtilities.logWarning(fName, "Name validation failed (requested name contained illegal characters)! Requested name: " + requestedName + ", new name: " + newName + ".");
			}
		}
		
		return newName;
	}
	
	
	/* ### OWNER RELATED METHODS BEGIN  ### */
	
	
	/**
	 * Gets the owner of this property.
	 * 
	 * @return the owner of this property.
	 */
	public final PropertyOwner getPropertyOwner()
	{
		return owner;
	}
	
	/**
	 * Method to assume ownership of a Property. If a parent already exists it will be replaced by parameter <code>newOwner</code>. This method is 
	 * used by {@link com.teletalk.jserver.SubComponent#addProperty(Property)} and {@link com.teletalk.jserver.SubComponent#addProperty(Property, String)}, 
	 * which also are the preferred ways for SubComponets to assume ownership of properties.
	 * 
	 * @param newOwner the new owner.
	 */
	public final void ownerShipAssumed(PropertyOwner newOwner)
	{
		this.owner = newOwner;	
	}
	
	/**
	 * Method to assume ownership of a Property and rename it. If a parent already exists it will be replaced by parameter <code>newOwner</code>. This method is 
	 * used by {@link com.teletalk.jserver.SubComponent#addProperty(Property)} and {@link com.teletalk.jserver.SubComponent#addProperty(Property, String)}, 
	 * which also are the preferred ways for SubComponets to assume ownership of properties.
	 * 
	 * @param newOwner the Property to assume ownership of.
	 * @param newName a new name for the target Property.
	 */
	public final void ownerShipAssumed(PropertyOwner newOwner, String newName)
	{
		synchronized(this.componentLock)
		{
			this.owner = newOwner;	
			this.name = validateName(newName);
		}
	}
	
	
	/* ### OWNER RELATED METHODS END ### */	
	
	/* ### GETTERS/SETTERS BEGIN  ### */
	
   /**
    * Sets the value representing the default value of the this property. This value is used for checking 
    * if the property needs to be saved to persistent storage.
    * 
    * @see #isUsingDefaultValue()
    * 
    * @since 2.0 Build 757 
    */
	public void setDefaultValue(Object defaultValue)
   {
      this.defaultValue = defaultValue;
   }
   
   /**
    * Sets the default value of the this property to the current value of thei property. This default value is used for checking 
    * if the property needs to be saved to persistent storage.
    * 
    * @see #setDefaultValue(Object)
    * @see #isUsingDefaultValue()
    * 
    * @since 2.1.2 (20060227) 
    */
   public void setDefaultValueToCurrentValue()
   {
      this.setDefaultValue(this.getValueAsObject());
   }
   
	/**
	 * Gets the value representing the default value of the this property. This value is used for checking 
    * if the property needs to be saved to persistent storage.
	 * 
	 * @since 2.0 Build 757
	 * 
	 * @see #isUsingDefaultValue()
	 */
   public Object getDefaultValue()
   {
      return defaultValue;
   }
	
	/**
	 * Returns the name of this Property.
	 * 
	 * @return the name.
	 */
	public final String getName()
	{
		return name;
	}
	
	/**
	 * Sets the name of this Property.<br>
	 * <br>
	 * <i><b>Note: </b></i> 
	 * 
	 * @return the name.
	 * 
	 * @since 2.0 Build 757
	 */
	/*public void setName(String name)
	{
		this.name = this.validateName(name);
	}*/
	
	/**
	 * Returns the full name of this Property by concatenating the name of this Property to the full name
	 * of the owner, separated by a dot.
	 * 
	 * @return the full name.
	 */
	public final String getFullName()
	{
		if(owner != null) return owner.getFullName() + "." + getName();
		else return getName();
	}
	
	/**
	 * Marks this Property as removed.
	 */
	public void remove()
	{
		synchronized(componentLock)
		{
			owner = null;
			removed = false; 
		}
	}
	
	/**
	 * Checks if this Property is removed.
	 * 
	 * @return true if this Property is removed, otherwise false.
	 */
	public boolean isRemoved()
	{
		synchronized(componentLock)
		{
			return removed;
		}
	}
	
	/**
	 * Checks if this propery is currently initializing (with data from persistent storage).
	 * 
	 * @return <code>true</code> if this property is currently initializing ,otherwise <code>false</code>.
	 */
	public boolean isInitializing()
	{
		return this.initializing;
	}
	
	/**
	 * Checks if this propery has been initialized with data from persistent storage (i.e. not using the default value).
	 * 
	 * @return <code>true</code> if this property has been initialized ,otherwise <code>false</code>.
	 */
	public boolean isInitialized()
	{
		return this.initialized;
	}
	
	/**
	 * Stores metadata in this object.
	 * 
	 * @param key metadata key.
	 * @param value metadata value.
	 */
	public void putMetaData(String key, Object value)
	{
		synchronized(this.metaData)
		{
         this.metaData.put(key, value);
		}
	}
   
   /**
    * Stores metadata in this object.
    * 
    * @param metaData metadata to add.
    */
   public void putMetaData(HashMap metaData)
   {
      synchronized(this.metaData)
      {
         this.metaData.putAll(metaData);
      }
   }
	
	/**
	 * Gets metadata.
	 * 
	 * @param key metadata key.
	 * 
	 * @return the metadata value.
	 */
	public Object getMetaData(String key)
	{
		synchronized(this.metaData)
		{
			return this.metaData.get(key);
		}
	}
	
	/**
	 * Gets all the metadata stored in this Property.
	 * 
	 * @return a HashMap containing metadata.
	 */
	public HashMap getMetaData()
	{
		synchronized(this.metaData)
		{
			return (HashMap)metaData.clone();
		}
	}
	
	/**
	 * Gets the flag indicating if the meta data of this propery is to be persistent.
	 * 
	 * @since 2.0 Build 757
	 */
   public boolean isPersistentMetaData()
   {
      return persistentMetaData;
   }
   
   /**
    * Sets the flag indicating if the meta data of this propery is to be persistent.
    * 
    * @since 2.0 Build 757
    */
   public void setPersistentMetaData(boolean metaDataModified)
   {
      this.persistentMetaData = metaDataModified;
   }
	
	/**
	 * Sets the description field of this Property.
	 * 
	 * @param description the description of this Property.
	 */
	public void setDescription(String description)
	{
		synchronized(this.metaData)
		{
			metaData.put(PROPERTY_DESCRIPTION_KEY, description);
		}
	}
	
	/**
	 * Gets the description field of this Property.
	 * 
	 * @return the description of this property or null if none was set.
	 */
	public String getDescription()
	{
		synchronized(this.metaData)
		{
			return (String)metaData.get(PROPERTY_DESCRIPTION_KEY);
		}
	}
	
	/**
	 * Enables or disables <code>force mode</code> of this property. If force mode is enabled no validation or access control 
	 * (see methods {@link #validate()} and {@link #canSet()}) will be performed when setting a new value for this 
	 * property. <code>Force mode</code> is disabled by default.
	 * 
	 * @param enabled <code>true</code> if <code>force mode</code> is to be enabled, otherwise <code>false</code>.
	 */
	public final void setForceMode(boolean enabled)
	{
		this.forceModeEnabled = enabled;
	}
	
	/**
	 * Checks whether or not <code>force mode</code> is active for this property. If force mode is enabled no validation or access control 
	 * (see methods {@link #validate()} and {@link #canSet()}) will be performed when setting a new value for this 
	 * property. <code>Force mode</code> is disabled by default.
	 * 
	 * @return <code>true</code> if <code>force mode</code> is enabled, otherwise <code>false</code>.
	 */
	public final boolean getForceMode()
	{
		return forceModeEnabled;
	}
	
	/**
	 * Enables or disables modification notifications sent to the owner of this property.
	 * 
	 * @param enabled true if modification notifications are to be enabled, otherwise false.
	 */
	public final void setNotificationMode(boolean enabled)
	{
		this.notificationModeEnabled = enabled;
	}
	
	/**
	 * Checks whether or not modification notifications are to be sent to the owner of this property.
	 * 
	 * @return true if modification notifications are to be sent, otherwise false.
	 */
	public final boolean getNotificationMode()
	{
		return notificationModeEnabled;
	}
	
	/**
	 * Sets the modification mode.
	 * 
	 * @param mode the new value for the modification mode.
	 */
	public final void setModificationMode(int mode)
	{
		if( (mode >= MODIFIABLE_NO_RESTART) && (mode <= NOT_MODIFIABLE) )
		{
			this.modificationMode = mode;
		}
	}
	
	/**
	 * Checks if this property is modifiable.
	 * 
	 * @return true if this Property is modifiable, otherwise false.
	 */
	public final boolean isModifiable()
	{
		return this.modificationMode != NOT_MODIFIABLE;	
	}
	
	/**
	 * Gets the modificationMode.
	 * 
	 * @return the value of modificationMode.
	 */
	public final int getModificationMode()
	{
		return modificationMode;	
	}
	
	/**
	 * Sets the persistent flag.
	 * 
	 * @param persistent the new value for the persistent flag.
	 */
	public final void setPersistent(boolean persistent)
	{
		this.persistent = persistent;
	}
	
	/**
	 * Returns the value of the persistent flag. 
	 * 
	 * @return true if this Property is persistent, otherwise false.
	 */
	public final boolean isPersistent()
	{
		return persistent;
	}
   
   /**
    * Gets the time when this property was last modified.
    * 
    * @since 2.1 (20050815)
    */
   public long getLastModified()
   {
      return lastModified;
   }
   
   /**
    * Sets the time when this property was last modified.
    * 
    * @since 2.1 (20050815)
    */
   protected void setLastModified(long lastModified)
   {
      this.lastModified = lastModified;
   }
	
	
	/* ### GETTERS/SETTERS END ### */
	
	/* ### INITIALIZATION AND VALIDATION METHODS BEGIN ### */
		

	/**
	 * Initializes this property and extracts data from its persistent counterpart, if any, restored 
	 * by the PropertyManager. If this isn't a persistent property the parameter <code>persistentProperty</code> will 
	 * be <code>null</code>. This method makes it possible for subclasses to check if the default locale has changed 
	 * since this property was created, by calling the method {@link PropertyManager#isUsingDefaultLocale()}.
	 * 
	 * @param persistentProperty a property read from persistent storage, <code>null</code> if there is none matching this property.
	 * 
	 * @see com.teletalk.jserver.property.PropertyManager
	 */
	public synchronized void initProperty(final Property persistentProperty)
	{
		try
		{
			if( (persistentProperty != null) && this.isPersistent() )
			{
				this.initializing = true;
				
				if(persistentProperty.getModificationMode() >= 0)
            {
					setModificationMode(persistentProperty.getModificationMode());
            }
            
				if( persistentProperty.isPersistentMetaData() )
				{
				   this.metaData.putAll(persistentProperty.metaData);
				   this.setPersistentMetaData(true);
				}
				
            String[] values = persistentProperty.getValuesAsStrings();
            if( (values != null) && (values.length > 1) )
            {
               setValuesAsStrings(values);
            }
            else
            {
   				setValueAsString(persistentProperty.getValueAsString());
            }
			}
		}
		finally
		{
			this.initializing = false;
			this.initialized = true;
		}
	}
	
	/**
	 * Convenience method to check with the owner if a new value can be set.
	 * 
	 * @return true if a new value can be set, otherwise false.
	 */
	protected final boolean canSet()
	{
		if(initializing || forceModeEnabled) 
			return true;
		else if(owner != null)
			return owner.propertyModificationAllowed(this);
		else
			return true;
	}
	
	/**
	 * Convenience method to validate a new value with the owner. If validation fails, a warning will be logged. 
	 * 
	 * @return true if validation succeeded, otherwise false.
	 */
	protected final boolean validate()
	{
		if( (owner != null) && !forceModeEnabled )
		{
			if(!owner.validatePropertyModification(this))
			{
				JServerUtilities.logWarning(this.getFullName(), "Validation failed (the following value was invalid : " + getValueAsString() + ")!");
				return false;
			}
		}
		return true;
	}
	
	/**
	 * Called to indicate that the value of this property has changed.
	 */
	protected void modified()
	{
      this.setLastModified(System.currentTimeMillis());
		if( (owner != null) && notificationModeEnabled ) // && !initializing)
		{
			owner.propertyModified(this);
		}
	}
	
	
	/* ### INITIALIZATION AND VALIDATION METHODS END ### */
	
   /* ### VALUE SETTERS, ETC BEGIN ### */
	
   
	/**
	 * Checks if this property is using its default value.
	 * 
	 * @since 2.0 Build 757
	 */
   public boolean isUsingDefaultValue()
   {
      Object currentValue = this.getValueAsObject();
      if( this.defaultValue == null ) return (currentValue == null);
      else return this.defaultValue.equals(currentValue);
   }
	
	/**
	 * Abstract method to get the value of this Property as a String.
	 * 
	 * @return String representation of the value.
	 */
	public abstract String getValueAsString();
   
   /**
    * Gets the value of this property as an array of strings. This methods is provided for properties that contain more than one value.
    * This implementation returns a string array with one element, containing the return value of {@link #getValueAsString()}.
    * 
    * @since 1.3.2
    */
   public String[] getValuesAsStrings()
   {
      return new String[]{this.getValueAsString()};
   }
	
	/**
	 * Abstract method to get the value of this Property as an Object.
	 * 
	 * @return the value as an Object.
	 */
	public abstract Object getValueAsObject();
   
   /**
    * Gets the value of this property as an array of objects. This methods is provided for properties that contain more than one value.
    * This implementation returns an object array with one element, containing the return value of {@link #getValueAsObject()}.
    * 
    * @since 2.1.4 (20060503)
    */
   public Object[] getValuesAsObjects()
   {
      return new Object[]{this.getValueAsObject()};
   }
	
	/**
	 * Abstract method to parse a new value for this Property from a String.
	 * 
	 * @param value the String from which the value will be parsed.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public abstract boolean setValueAsString(String value);
   
   /**
    * Sets the value of this property as an array of strings. This methods is provided for properties that contain more than one value.
    * This implementation calls the method {@link #setValueAsString(String)} with the first element (if present, otherwise null) of the array as parameter.
    * 
    * @since 1.3.2
    */
   public boolean setValuesAsStrings(String[] values)
   {
      return this.setValueAsString( (values != null) ? values[0] : null );
   }
   
	/**
	 * Abstract method to set a new value for this Property as an Object.
	 * 
	 * @param value the new value of the Property.
	 * 
	 * @return true if the value was successfully set, otherwise false.
	 */
	public abstract boolean setValueAsObject(Object value);
   
   /**
    * Sets the value of this property as an array of objects. This methods is provided for properties that contain more than one value.
    * This implementation calls the method {@link #setValueAsObject(String)} with the first element (if present, otherwise null) of the array as parameter.
    * 
    * @param values the new values.
    * 
    * @return true if the values were successfully set, otherwise false.
    * 
    * @since 2.1.4 (20060503)
    */
   public boolean setValuesAsObjects(Object[] values)
   {
      return this.setValueAsObject( (values != null) ? values[0] : null );
   }
	
	public String getParsingDescription()
	{
		return null;
	}
	
	
	/* ### VALUE SETTERS, ETC END ### */
	
	/* ### TOSTRING, EQUALS ETC BEGIN  ### */
	
	
	/**
	 * Compares two properties. Two properties are equal if the are of the same class, have the same name and same owners.
	 * 
	 * @return true if the classes, names and owners of the properties are equal.
	 */
	public boolean equals(Object obj)
	{
		if(obj instanceof Property)
		{
			Property p = ((Property)obj);

			PropertyOwner po = p.getPropertyOwner();
			
			if(po != null)
         {
				return p.getClass().equals(this.getClass()) && 
            p.getName().equals(this.getName()) && 
            (po.equals(this.getPropertyOwner()));
         }
			else
         {
				return p.getClass().equals(this.getClass()) && 
            p.getName().equals(this.getName()) && 
            (this.getPropertyOwner() == null);
         }
		}
		else return false;
	}

	/**
	 * Returns a hash code value for this property. 
	 * 
	 * @return a hash code value for this property. 
	 */
	public int hashCode()
	{
		return this.name.hashCode();
	}
	
	/**
	 * Returns a String describing this property, containing the name and value.
	 * 
	 * @return a string representation of the property.
	 */
	public String toString()
	{
		return this.getName() + ": " + this.getValueAsString();
	}
	
	
	/* ### TOSTRING, EQUALS ETC END  ### */
	
	/* ### RMI ADAPTER METHODS BEGIN  ### */
	

	/**
	 * Sets the RmiAdapter for this Property.
	 * 
	 * @param rmiAdapter a RmiAdapter object.
	 */
	public void setRmiAdapter(RmiAdapter rmiAdapter)
	{
		synchronized(this.componentLock)
		{
			if( (this instanceof VectorProperty) && !(rmiAdapter instanceof VectorPropertyRmiAdapter) ) 
			{
				throw new RuntimeException("Unable to set rmi adapter! Specified adapter object is not an instance of VectorPropertyRmiAdapter");
			}
			else if( (this instanceof Property) && !(rmiAdapter instanceof PropertyRmiAdapter) ) 
			{
				throw new RuntimeException("Unable to set rmi adapter! Specified adapter object is not an instance of PropertyRmiAdapter");
			}
			else
				this.rmiAdapter = rmiAdapter;
		}
	}
	
	/**
	 * Gets the RmiAdapter associated with this Property.
	 * 
	 * @return a RmiAdapter (PropertyRmiAdapter) object.
	 * 
	 * @see com.teletalk.jserver.rmi.adapter.PropertyRmiAdapter
	 */
	public RmiAdapter getRmiAdapter()
	{
		synchronized(this.componentLock)
		{
			if(rmiAdapter == null)
			{
				try
				{
					setRmiAdapter(new PropertyRmiAdapter(this));
				}
				catch(RemoteException e)
				{
					JServerUtilities.logError(getFullName(), "Unable to create PropertyRmiAdapter", e);
				}
			}
					
			return rmiAdapter;
		}
	}
}
