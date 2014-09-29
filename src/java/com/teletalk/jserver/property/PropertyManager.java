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

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.event.EventQueue;
import com.teletalk.jserver.event.PropertyEvent;
import com.teletalk.jserver.event.PropertyEventListener;

/**
 * The PropertyManager is a SubSystem that is responsible for loading properties from persistent storage (a PersistentPropertyStorage object
 * and periodically saving them back. This class is also responsible for making sure that properties that need to be
 * calculated gets calculated with a given interval (default 2 seconds). The PropertyManger does this by calling the method <tt>calculateProperties()</tt>
 * in every SubSystem and SubComponent in the system that implements the <tt>CalculatedPropertyOwner</tt> interface.<br>
 *
 * @see com.teletalk.jserver.SubSystem
 * @see PersistentPropertyStorage
 * @see CalculatedPropertyOwner
 *
 * @author Tobias Löfstrand
 *
 * @since Alpha
 */
public final class PropertyManager extends SubSystem implements PropertyEventListener
{
	private TreeMap persistentProperties; // The properties in this server that are marked as persistent

	private NumberProperty calculationInteval;
	private NumberProperty saveInteval;
	private StringProperty locale;
	
	private volatile boolean propertyManagerInitialized = false;
	private volatile boolean dirtyFlag = false;
	
	private boolean usingDefaultLocale = true;
	
	private final PersistentPropertyStorage persistentPropertyStorage;
	
	private final Object lock = new Object();
	
   private final HashMap dynamicDefaultSystems;
   
   private final Timer calculatedPropertiesTimer;
   
   private CalculatedPropertyTimerTask calculatedPropertyTimerTask = null;
   
	
	/**
	 * Creates a new PropertyManager and reads properties from persistent storage. This constructor will attempt 
	 * first to read the file <code>properties.jsr</code>, in which case a {@link SimpleTextFilePropertyStorage} will be 
	 * used with this PropertyManager. If that file is not found, a {@link XmlConfigurationFile} will be used instread, and 
	 * consequently attempts will be made to read the file <code>jserver.xml</code>.
	 *
	 * @param parent the parent SubSystem.
	 */
	public PropertyManager(SubSystem parent)
	{
		//this(parent, SimpleTextFilePropertyStorage.defaultPropertiesFileName);
	   this(parent, guessPersistentPropertyStorage());
	}

	/**
	 * Creates a new PropertyManager and reads properties from persistent storage. If the parameter 
	 * <code>propertiesFileName</code> ends with the string <code>".jsr"</code>, a {@link SimpleTextFilePropertyStorage} 
	 * will be used with this PropertyManager. Otherwise a {@link XmlConfigurationFile} will be used.
	 * 
	 * @param parent the parent SubSystem.
	 * @param propertiesFileName the filename for loading and saving properties.
	 */
	public PropertyManager(SubSystem parent, String propertiesFileName)
	{
		//this(parent, new SimpleTextFilePropertyStorage(propertiesFileName));
	   this(parent, guessPersistentPropertyStorage(propertiesFileName));
	}

	/**
	 * Creates a new PropertyManager and reads properties from persistent storage, using the specified {@link PersistentPropertyStorage}. 
	 * 
	 * @param parent the parent SubSystem.
	 * @param persistentPropertyStorage the PersistentPropertyStorage object used for reading and writing properties from/to persistent storage.
	 */
	public PropertyManager(SubSystem parent, PersistentPropertyStorage persistentPropertyStorage)
	{
		super(parent, "PropertyManager");
		
      /*JServer jServer = JServer.getJServer();
      if( jServer != null ) this.jServerName = jServer.getName();
      else this.jServerName = null;*/

		this.persistentPropertyStorage = persistentPropertyStorage;
		this.persistentPropertyStorage.init(this);

		this.persistentProperties = new TreeMap();
      
      this.dynamicDefaultSystems = new HashMap();
		
		this.calculationInteval = new NumberProperty(this, "calculationInteval", 2*1000, Property.MODIFIABLE_NO_RESTART);
		this.calculationInteval.setDescription("The interval in milliseconds at which properties owned by CalculatedPropertyOwners are recalculated.");
		this.saveInteval = new NumberProperty(this, "saveInterval", 15*1000, Property.MODIFIABLE_NO_RESTART);
		this.saveInteval.setDescription("The interval in milliseconds at which properties are saved to file (if needed).");
		this.locale = new StringProperty(this, "locale", Locale.getDefault().toString(), Property.NOT_MODIFIABLE, true);
		this.locale.setDescription("String representation of the locale to use as defaul for the whole JVM (for instance sv_SE, en_US, etc). Note that the default locale is essential when representing certain properties as strings.");

		addProperty(this.calculationInteval);
		addProperty(this.saveInteval);
		addProperty(this.locale);
		
		loadPersistentProperties(); //Load persistent properties
      
      this.calculatedPropertiesTimer = new Timer();
	}
   
   /**
    * Registers a dynamic core system with the specified component name.
    * 
    * @since 2.1
    */
   public void registerDynamicCoreSystem(final String dynamicCoreComponentId, final String componentName)
   {
      synchronized(this.dynamicDefaultSystems)
      {
         this.dynamicDefaultSystems.put(dynamicCoreComponentId, componentName);
      }
   }
   
   /**
    * Checks if a dynamic core system is registered.
    * 
    * @since 2.1
    */
   public boolean isDynamicCoreSystemRegistered(final String dynamicDefaultSystemId)
   {
      synchronized(this.dynamicDefaultSystems)
      {
         return this.dynamicDefaultSystems.containsKey(dynamicDefaultSystemId);
      }
   }
   
   /**
    * Gets the registered component name for the dynamic core system with the specified id.
    * 
    * @since 2.1
    */
   public String getDynamicCoreSystemName(final String dynamicDefaultSystemId)
   {
      synchronized(this.dynamicDefaultSystems)
      {
         return (String)this.dynamicDefaultSystems.get(dynamicDefaultSystemId);
      }
   }
   
   /**
    * Checks if the component with the specified name is a dynamic core system.
    * 
    * @since 2.1
    */
   public boolean isDynamicCoreSystem(final String componentName)
   {
      synchronized(this.dynamicDefaultSystems)
      {
         return this.dynamicDefaultSystems.containsValue(componentName);
      }
   }
	
	/**
	 * Guesses the persistent property storage.
	 */
	private static PersistentPropertyStorage guessPersistentPropertyStorage()
	{
	   if( new File(SimpleTextFilePropertyStorage.defaultPropertiesFileName).exists() ) return new SimpleTextFilePropertyStorage(SimpleTextFilePropertyStorage.defaultPropertiesFileName);
	   else return new XmlConfigurationFile();
	}
	
	/**
	 * Returns the appropiate {@link PersistentPropertyStorage} for the specified file name.
	 */
	public static PersistentPropertyStorage guessPersistentPropertyStorage(final String fileName)
	{
	   if( fileName == null ) return new XmlConfigurationFile();
	   else if( fileName.trim().toLowerCase().endsWith(".xml") ) return new XmlConfigurationFile(fileName);
	   else return new SimpleTextFilePropertyStorage(fileName);
	   /*if( (fileName != null) && fileName.endsWith(".jsr") ) return new SimpleTextFilePropertyStorage();
	   else return new XmlConfigurationFile();*/
	}

	/**
	 * Initializes this PropertyManager.
	 */
	protected void doInitialize()
	{
		propertyManagerInitialized = false;
		
		EventQueue eq = getEventQueue();

		if(eq != null) eq.registerPropertyEventListener(this);
            
      this.reScheduleCalculatedPropertyTimerTask();

		super.doInitialize();
	}

	/**
	 * Shuts down this PropertyManager.
	 */
	protected void doShutDown()
	{
		if(!isReinitializing()) savePersistentProperties();

		EventQueue eq = getEventQueue();

		if(eq != null) eq.unregisterPropertyEventListener(this);
      
      if( this.calculatedPropertyTimerTask != null )
      {
         this.calculatedPropertyTimerTask.cancel();
         this.calculatedPropertyTimerTask = null;
      }

		super.doInitialize();
	}
	
	/**
	 * Validates a modification of a value of property owned by this subsystem. 
	 * 
	 * @param property The property to be validated.
	 * 
	 * @return boolean value indicating if the property passed (true) validation or not (false).
	 * 
	 * @see com.teletalk.jserver.property.PropertyOwner
	 */
	public boolean validatePropertyModification(final Property property)
	{
		if(property ==	this.saveInteval)
      {
			return (this.saveInteval.intValue() >= 1000) && (this.saveInteval.intValue() <= (60*60*1000) );
      }
		else if(property ==	this.calculationInteval)
      {
			return (this.calculationInteval.intValue() >= 1000) && (this.calculationInteval.intValue() <= (60*60*1000) );
      }
		else return super.validatePropertyModification(property);
	}
   
   /**
    * Called when the value of a property has been modified. 
    */   
   public void propertyModified(final Property property)
   {
      if( property == this.calculationInteval )
      {
         reScheduleCalculatedPropertyTimerTask();
      }
      super.propertyModified(property);
   }

   /**
    * Returns the PropertyManager, e.g. this object.
    *
    * @return a PropertyManager object.
    */
   public final PropertyManager getPropertyManager()
   {
      return this;
   }

   /**
    * Notification of a property modificaion.
    *
    * @param e a PropertyEvent object.
    *
    * @see com.teletalk.jserver.event.PropertyEvent
    */
   public void propertyModified(final PropertyEvent e)
   {
      if( !e.isInitializationModification() ) //If not property initialization
      {
         Property p = e.getProperty();

         if(p.isPersistent() && propertyManagerInitialized)
         {
            if(isDebugMode()) logDebug("Persistent property "  + p.getFullName() + " modified.");
            
            addPersistentProperty(p);
            dirtyFlag = true;
         }
      }
   }
   
   /**
    * (Re)schedules the calculated property timer.
    */
   private void reScheduleCalculatedPropertyTimerTask()
   {
      if( this.calculatedPropertyTimerTask != null ) this.calculatedPropertyTimerTask.cancel();
      this.calculatedPropertyTimerTask = new CalculatedPropertyTimerTask(this);
      this.calculatedPropertiesTimer.schedule(this.calculatedPropertyTimerTask, this.calculationInteval.longValue(), this.calculationInteval.longValue());
   }
   
   
   /* ### ### */
   

	/**
	 * Reads properties from the propertyfile.
	 */
	private void loadPersistentProperties()
	{
      logInfo("Loading persistent properties.");
		
		// Read properties from PersistentPropertyStorage
		//this.persistentProperties = new TreeMap(persistentPropertyStorage.readProperties());
		this.persistentPropertyStorage.readProperties();

		initLocale();
		
      logInfo("Persistent properties loaded.");
	}

	/**
    * Initializes the locale.
	 */
	private void initLocale()
	{
		//Get the hashmap containing the persistent properties belonging to this PropertyManager. (Method getLoadedProperties cannot be used, since it clones the hashmap...)
		List propertyManagerProperties = (List)this.persistentProperties.get(convertFullName(this.getFullName()));
		
		//Get persistent counterpart of the locale property:
		Property persistenLocaleProperty = null;
		Property p = null;
		
		if(propertyManagerProperties != null)
		{
			for(int i=0; i<propertyManagerProperties.size(); i++)
			{
				p = ((Property)propertyManagerProperties.get(i));
				if( p.getName().equalsIgnoreCase(locale.getName()) )
				{
					persistenLocaleProperty = p;
					break;
				}
			}
		}

		if(persistenLocaleProperty != null)
		{
			locale.initProperty(persistenLocaleProperty); //Init locale property from persistent counterpart
			//Remove persistenLocaleProperty from propertyManagerProperties, so that it doesn't overwrite a new value given to the property.

			//propertyManagerProperties.remove(locale.getName());
			propertyManagerProperties.remove(locale);

			Locale defaultLocale = Locale.getDefault();
			logInfo("Default locale is " + defaultLocale + ".");

			Locale loc = getLocale();

			if(loc != null && !defaultLocale.equals(loc)) //If the locale is different from the default...
			{
				Locale.setDefault(loc);
				usingDefaultLocale = false;
				logInfo("Setting locale to " + loc +".");
			}
			else
			{
				locale.setValue(defaultLocale.toString());
				logInfo("Using default locale (" + defaultLocale +").");
			}
		}
	}

	/**
	 * Parses a Locale object from the property <code>locale</code> owned by this PropertyManager. If no locale
	 * matching the value of that property is found, <code>null</code> is returned.
	 *
	 * @return a Locale object or <code>null</code> if no locale matching the property was found.
	 */
	public Locale getLocale()
	{
		String localeString =  locale.getValueAsString();

		Locale matchingLocale = null;

		if(localeString != null)
		{
			Locale[] locs = Locale.getAvailableLocales();

			for(int i=0; i<locs.length; i++)
			{
				if(locs[i].toString().equals(localeString))
				{
					matchingLocale = locs[i];
					break;
				}
			}

			if(matchingLocale == null)
				logWarning("No available locale found matching '" + localeString + "'.");
		}

		return matchingLocale;
	}

	/**
	 * Checks if the default locale is used or a different locale has been set in the property file.
	 *
	 * @return <code>true</code> if the default locale is used, otherwise <code>false</code>.
	 */
	public boolean isUsingDefaultLocale()
	{
		return usingDefaultLocale;
	}
	
	/**
	 * Saves properties to the propertyfile. This method is used by PropertyManager to save
	 * properties and it is not nessecary to call this method directly.
	 *
	 * @return true if the save was successful, otherwise false.
	 */
	public final boolean savePersistentProperties()
	{
      if( super.isDebugMode() ) logDebug("Saving persistent properties.");
      
      boolean success = false;
		synchronized(lock)
		{
			// Write properties to PersistentPropertyStorage
         success = persistentPropertyStorage.writeProperies(this.persistentProperties);
		}
      
      if( success )
      {
         if( super.isDebugMode() ) logDebug("Persistent properties saved.");
      }
      else logWarning("Failed to save persistent properties!");
      
      return success;
	}
	
	/**
	 * Gets properties restored from persistent storage belonging to the specified group (SubSystem or SubComponent) as a <code>Map</code>.
	 *
	 * @param group the group (full name of subsystem or subcomponent, i.e. "MyServer.MySubsystem") to get properies for.
	 *
	 * @return a Map containing names of properties mapped with Property objects.
	 */
	public final Map getLoadedPropertiesAsMap(String group)
	{
		synchronized (lock)
      {
         group = convertFullName(group);

         final List groupPropertiesList = (List) this.persistentProperties.get(group);
         final HashMap groupPropertiesMap = new HashMap();
         Property p;

         if (groupPropertiesList != null)
         {
            for (int i = 0; i < groupPropertiesList.size(); i++)
            {
               p = (Property) groupPropertiesList.get(i);
               if (p != null)
               {
                  groupPropertiesMap.put(p.getName(), p);
               }
            }
         }

         return groupPropertiesMap;
      }
	}

	/**
	 * Gets properties restored from persistent storage belonging to the specified group (SubSystem or SubComponent) as a <code>List</code>.
	 *
	 * @param group the group (full name of subsystem or subcomponent, i.e. "MyServer.MySubsystem") to get properies for.
	 *
	 * @return a List containing Property objects.
	 */
	public final List getLoadedPropertiesAsList(String group)
	{
		synchronized (lock)
      {
         if (this.persistentProperties != null)
         {
            group = convertFullName(group);

            final List l = (List) this.persistentProperties.get(group);

            if (l != null) return new ArrayList(l);
            else return new ArrayList();
         }
         else return new ArrayList();
      }
	}
	
	/**
	 * Gets all properties loaded from persistent storage. The returned Map will contain names of property groups 
	 * mapped with Maps containing names of properties mapped with Property objects.
	 *
	 * @return a Map containing names of property groups mapped with Maps containing names of properties mapped with Property objects.
	 */
	public final Map getAllLoadedPropertiesAsMaps()
	{
		synchronized(lock)
		{
			final HashMap allProperties = new HashMap();
		
			if(this.persistentProperties != null)
			{
				final Iterator it = this.persistentProperties.keySet().iterator();
				
				String groupName;
				
				while(it.hasNext())
				{
					groupName = (String)it.next();
									
					if(groupName != null)
					{
						allProperties.put(convertFullName(groupName), getLoadedPropertiesAsMap(groupName));
					}
				}
			}
		
			return allProperties;
		}
	}
		
	/**
	 * Gets all properties loaded from persistent storage. The returned Map will contain names of property groups 
	 * mapped with Lists containing Property objects.
	 *
	 * @return a Map containing names of property groups mapped with Lists containing Property objects.
	 */
	public final Map getAllLoadedPropertiesAsLists()
	{
		synchronized(lock)
		{
			final HashMap allProperties = new HashMap();
		
			if(this.persistentProperties != null)
			{
				final Iterator it = this.persistentProperties.keySet().iterator();
				String groupName;
				
				while(it.hasNext())
				{
					groupName = (String)it.next();
									
					if(groupName != null)
					{
						allProperties.put(convertFullName(groupName), getLoadedPropertiesAsList(groupName));
					}
				}
			}
		
			return allProperties;
		}
	}

	/**
	 * Converts a name on the form "MyServer.MySubsystem" to "JServer.MySubsystem".
	 */
	public String convertFullName(final String fullName)
	{
      JServer jServer = JServer.getJServer();
      if( jServer != null ) return jServer.normalizeFullName(fullName);
      else return fullName;
	}

	/**
    * Rebuilds properties.
	 */
	private void rebuildProperties()
	{
      JServer jServer = JServer.getJServer();
      if( jServer != null )
      {
   		List systemTree = jServer.getSystemTree();
   		
   		if(isDebugMode()) logDebug("Rebuilding list of persistent properties");
   
   		List subComponentProperties;
   		SubComponent subComponent;
   		Property property;
   		   			
   		for(int i=0; i<systemTree.size(); i++)
   		{
   		   subComponent = (SubComponent)systemTree.get(i);
   			
   			if(isDebugMode()) logDebug("Rebuilding persistent properties of " + subComponent.getFullName() + ".");
   			
   			subComponentProperties = subComponent.getProperties();
   
   			for(int q=0; q<subComponentProperties.size(); q++)
   			{
   				property = (Property)subComponentProperties.get(q);
   
   				if( property.isPersistent() )
   				{
   					addPersistentProperty(property, true);
   				}
   			}
            
            if(isDebugMode()) logDebug("Done rebuilding persistent properties of " + subComponent.getFullName() + ".");
         }
         
         if(isDebugMode()) logDebug("Done rebuilding list of persistent properties");
      }
	}

   /**
    * Calculates properties owned by CalculatedPropertyOwners.
    *
    * @see CalculatedPropertyOwner
    */
   final void calculateProperties()
   {
      JServer jServer = JServer.getJServer();
      if( jServer != null )
      {
         List systemTree = jServer.getSystemTree();
         
         synchronized(lock)
         {
            SubComponent subComponent;
            
            for(int i=0; i<systemTree.size(); i++)
            {
               subComponent = (SubComponent)systemTree.get(i);
               
               if( (subComponent != null) && subComponent.isEnabled() && (subComponent instanceof CalculatedPropertyOwner) )
               {
                  ((CalculatedPropertyOwner)subComponent).calculateProperties();
               }
            }
         }
      }
   }
   
   /**
    * Registers an invisible implementation class property for a dynamic component.
    * 
    * @since 2.1 (20050825)
    */
   public void addImplementationClassProperty(final String componentName, final String implementationClass)
   {
      StringProperty property = new StringProperty(null, SubComponent.IMPLEMENTATION_CLASS_NAME_PROPERTY, implementationClass, StringProperty.NOT_MODIFIABLE, false);
      this.addPersistentProperty(property, componentName);
   }

	/**
	 * Method to register a persistent property. SubComponent and SubSystem objects should NOT call this method directly, as this
	 * is done automatically for all components that are part of the server. Only PropertyOwner (non SubComponent and SubSystem objects
	 * that is) should call this method to register persistent properties.
	 *
	 * @param property the persistent property to be registered in this PropertyManager.
	 * @param owner the name of the owner.
	 */
	public void addPersistentProperty(final Property property, String owner)
	{
		addPersistentProperty(property, owner, false);
	}
	
	/**
    * Adds a persistent property.
	 */
	private void addPersistentProperty(final Property property, String owner, boolean onlyLogNewPersistentProperties)
	{
		synchronized(lock)
		{
			ArrayList parentPropertyList = null;

			if(owner != null)
			{
				owner = convertFullName(owner);
				
				parentPropertyList = (ArrayList)persistentProperties.get(owner);

				if(parentPropertyList == null)
				{
					parentPropertyList = new ArrayList();
					persistentProperties.put(owner, parentPropertyList);
				}

				if( !parentPropertyList.contains(property) )
				{
					if(isDebugMode()) logDebug("Registering persistent property '" + property.getName() + "' owned by '" + owner + "'.");
					
					Property p;
					boolean loadedPropertyFound = false;
					
					for(int i=0; i<parentPropertyList.size(); i++) // Remove and replace loaded property (StringProperty)...
					{
						p = (Property)parentPropertyList.get(i);

						if(p.getName().equals(property.getName()))
						{
							loadedPropertyFound = true;
							if( property.isInitialized() ) // Only replace the loaded property with the real one if it has been initialized...(i.e. not using default value).
							{
								parentPropertyList.remove(p);
							}
							break;
						}
					}

					// Only add the property if it has been initialized (i.e. not using default value) or if any loaded property doesn't exist...
					if( property.isInitialized() || !loadedPropertyFound )
					{
						parentPropertyList.add(property);
						dirtyFlag = true;
					}
				}
				else
				{
					if( (isDebugMode() && !onlyLogNewPersistentProperties) ) logDebug("Persistent property '" + property.getName() + "' owned by '" + owner + "' changed value.");
				}
			}
		}
	}

	/**
	 * Method to register a persistent property. SubComponent and SubSystem objects should NOT call this method directly, as this
	 * is done automatically for all components that are part of the server. Only PropertyOwner (non SubComponent and SubSystem objects
	 * that is) should call this method to register persistent properties.
	 *
	 * @param property the persistent property to be registered in this PropertyManager.
	 */
	public void addPersistentProperty(final Property property)
	{
		addPersistentProperty(property, false);
	}
	
	/**
    * Adds a persistent property.
	 */
	private void addPersistentProperty(final Property property, boolean onlyLogNewPersistentProperties)
	{
		PropertyOwner owner = property.getPropertyOwner();
		boolean ownerIsRemoved = false;
		
		if( (owner instanceof SubComponent) && (owner != JServer.getJServer()) )
		{
			ownerIsRemoved = !((SubComponent)owner).hasParent();
		}
		
      if( (owner != null) && !ownerIsRemoved )
      {
   		addPersistentProperty(property, owner.getFullName(), onlyLogNewPersistentProperties);
      }
	}
	
	/**
	 * Removes a persistent property registered under the owner with the specified name.
	 * 
	 * @param property the property to remove from the the persisten property record of this PropertyManager.
	 * @param ownerName the name of the owner of the property.
	 */
	public final void removePersistentProperty(final Property property, String ownerName)
	{
		synchronized(lock)
		{
			if(ownerName != null)
			{
				ownerName = convertFullName(ownerName);
	
				ArrayList parentPropertyVector = (ArrayList)persistentProperties.get(ownerName);

				if(parentPropertyVector != null)
				{
					parentPropertyVector.remove(property);
				}
			}
		}
	}

	/**
	 * The thread method of this PropertyManager. Updates properties and saves to file with a given
	 * interval.
	 */
	public void run()
	{
		this.propertyManagerInitialized = true;
		
		boolean save = false;
		this.dirtyFlag = false;
		boolean firstCycle = true;
		
		long saveT = saveInteval.longValue();
		long lastSave = System.currentTimeMillis();
		long saveWaitTime = saveT;
		
		while(canRun)
		{
			try
			{
				if( saveWaitTime > 0 ) Thread.sleep(saveWaitTime);
			}
			catch(InterruptedException e)
			{
				continue;
			}

			saveT = saveInteval.longValue();
			

			//Check if it' s time to save properties
			if(( System.currentTimeMillis() - lastSave) > saveT )
			{
				// If first cycle...
				if( firstCycle ) 
				{
					// ...rebuild properties
					rebuildProperties();
					firstCycle = false;
				}
				saveWaitTime = saveT;
				lastSave = System.currentTimeMillis() - 33;

				synchronized(lock)
				{
					if(dirtyFlag)
					{
						dirtyFlag = false;
						save = true;
					}
				}

				if( save )
				{
					savePersistentProperties();
					save = false;
				}
			}
			else
			{
				saveWaitTime = saveT - (System.currentTimeMillis() - lastSave);
				if(saveWaitTime <= 0) saveWaitTime = 1;
			}
		}
	}
   
   
   /* ### ### */
   
   
   /**
    */
   private static class CalculatedPropertyTimerTask extends TimerTask
   {
      private final PropertyManager propertyManager;
      
      public CalculatedPropertyTimerTask(PropertyManager propertyManager)
      {
         this.propertyManager = propertyManager;
      }
      
      public void run()
      {
         this.propertyManager.calculateProperties();
      }
   }
}
