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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * PersistentPropertyStorage implementation that uses a simple text file for persistent storage of properties. The 
 * file is divided into blocks, separated by emply lines (CRLR), where each block contains information about one property group. 
 * The first line of the block contains the name of the property group surrounded by brackets. The following lines contains 
 * information about the properties in the group. Each line contains information about a single property, starting with an 
 * integer representing the modification mode followed by a tab character that precedes the key=value mapping of the property. 
 * Below is an example of a block (property group) in a property file:<br>
 * <br> 
 * ...<br>
 * [JServer.LogManager.FileLogger]<br>
 * 0	debugMode=false<br>
 * 1	periodicity=Daily (weekly cyclicity)<br>
 * 1	log prefix=KahunaServer_<br>
 * 1	log suffix=.log<br>
 * 0	flush interval=5000<br>
 * 1	logFilePath=../logs/<br>
 * ...<br>
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1 Build 545
 */
public final class SimpleTextFilePropertyStorage implements PersistentPropertyStorage
{
	private static final String propertyBackupFileExtension = ".bak";
	
	/**	The default filename for properties. */
	public static String defaultPropertiesFileName = "properties.jsr";
	
	private String propertiesFileName;
	
	private PropertyManager pm;
	private String fullName;
   
   /**
    * Creates a new SimpleTextFilePropertyStorage.
    */
   public SimpleTextFilePropertyStorage()
   {
   }
	
   /**
    * Creates a new SimpleTextFilePropertyStorage.
    */
	public SimpleTextFilePropertyStorage(String propertiesFileName)
	{
		this.propertiesFileName = propertiesFileName;
	}
   
   /**
    * Sets the name of the file used for persistent storage of properties.
    * 
    * @param propertiesFileName
    */
   public void setPersistentPropertyStorageFile(String propertiesFileName)
   {
      this.propertiesFileName = propertiesFileName;
   }

   /**
    * Initializes this PersistentPropertyStorage.
    * 
    * @param pm the PropertyManager object to which this PersistentPropertyStorage is to belong to.
    */
	public void init(PropertyManager pm)
	{
		this.pm = pm;
            
		this.fullName = pm.getFullName() + ".TextFilePropertyStorage";
		
		if( this.propertiesFileName == null ) this.propertiesFileName = SimpleTextFilePropertyStorage.defaultPropertiesFileName;
	}

   /**
    * Reads all the properties stored persistently in the persistent storage represented by this PersistentPropertyStorage. 
    */
	public void readProperties()
	{
		try
		{
			File propertyFile = new File(propertiesFileName + propertyBackupFileExtension); // Check if  backup propertyfile exists (and if so, use it)

			if( (!propertyFile.exists() || propertyFile.length() < 3) ) // If temporary property file didn't exist, try to use the standard one
			{
				propertyFile = new File(propertiesFileName);
			}

			if(propertyFile.exists())
			{
				pm.logInfo(fullName, "Attempting to read properties from '" + propertyFile.getAbsolutePath() + "'.");

				BufferedReader in = new BufferedReader(new FileReader(propertyFile));

				String line;
				String currentGroup = null;
				boolean groupNameParseError = true;
				//HashMap currentGroupProperties = null;
				//List currentGroupProperties = null;

				//Read file, line by line
				while((line = in.readLine()) != null)
				{
					//Group
					if(line.trim().startsWith("[") && line.trim().endsWith("]"))
					{
						line = line.trim();
						currentGroup = line.substring(1 , line.length() - 1);

						if(currentGroup != null)
						{
							currentGroup = pm.convertFullName(currentGroup);

							//currentGroupProperties = new HashMap();
							//currentGroupProperties = new ArrayList();
							//loadedProperties.put(currentGroup, currentGroupProperties);

							groupNameParseError = false;
						}
						else
						{
							pm.logError(fullName, "Error parsing property group name (" + line + ")");
							groupNameParseError = true;
						}
					}
					//Property
					else if(!groupNameParseError && !(line.trim()).equals(""))
					{
						try
						{
							Property p = parseProperty(line);
							
							if(p != null)
							{
								pm.addPersistentProperty(p, currentGroup);
								//currentGroupProperties.add(p);
								//currentGroupProperties.put(p.getName(), p);
							}
						}
						catch(Exception e)
						{
							pm.logError(fullName, "Error parsing property (" + line + ") - " + e);
						}
					}
					else if(!(line.trim()).equals(""))
					{
						pm.logWarning(fullName, "Skipping property (" + line + ") - ");
					}
				}

				in.close();
				pm.logInfo(fullName, "Properties loaded!");
			}
			else
         {
				pm.logInfo(fullName, "Property file not found!");
         }
		}
		catch(FileNotFoundException ex)
		{
			pm.logInfo(fullName, "Property file not found!");
         System.out.println("Property file not found!");
         ex.printStackTrace();
		}
		catch(IOException ex)
		{
			pm.logError(fullName, "Error reading from property file!", ex);
         System.out.println("Error reading from property file!");
         ex.printStackTrace();
		}
	}

   /**
    * Writes properties to the persistent storage represented by this PersistentPropertyStorage. The map specified 
    * by parameter <code>properties</code> contains names of property groups (SubComponents and SubSystems) mapped 
    * with lists (java.lang.List objects) of properties.
    * 
    * @param persistentProperties the properties to be save to persistent storage.
    */
	public boolean writeProperies(final Map persistentProperties)
	{
		String groupName;

      File propertyFile = new File(this.propertiesFileName);
      File backupPropertyFile = new File(this.propertiesFileName + propertyBackupFileExtension);
      if( backupPropertyFile.exists() ) backupPropertyFile.delete();
      propertyFile.renameTo(backupPropertyFile);

		try
		{
			PrintWriter writer = new PrintWriter(new BufferedWriter(new FileWriter(propertyFile)));

			List groupProperties = null;

			for(Iterator it=persistentProperties.keySet().iterator(); it.hasNext();)
			{
				groupName = (String)it.next();

				if(groupName != null)
				{
					groupProperties = (List)persistentProperties.get(groupName);
					
					if(groupProperties != null)
					{
						writer.println("[" + groupName + "]");

						for(int q=0; q<groupProperties.size(); q++)
						{
							Property property = (Property)groupProperties.get(q);
							String propertyString = formatProperty(property);

							writer.println(propertyString);
							if(pm.isDebugMode()) pm.logDebug(fullName, "Writing property for " + groupName + ": " + propertyString);
						}

						groupProperties = null;

						writer.println();
					}
				}
			}

			writer.flush();
			writer.close();

         backupPropertyFile.delete();

			return true;
		}
		catch(Exception e)
		{
			pm.logError(fullName, "Error writing to property file!" , e);
			return false;
		}
	}

	/**
	 * Constructor to create a StringProperty from a string. This method is used by PropertyManager to parse properties from file.
	 *
	 * @param propertyString a String from which a StringProperty object will be constructed.
	 */
	private Property parseProperty(String propertyString)
	{
		String parsedValueString = "";
		StringTokenizer prefix = new StringTokenizer(propertyString);
		Exception restartRequirementModeParsingException = null;
		boolean restartRequirementModeParsingFailed = false;
		int modificationMode;
		//PropertyOwner owner;

		try
		{
			String firstToken = (String)prefix.nextToken();

			modificationMode = Integer.parseInt(firstToken);

			if(modificationMode < Property.MODIFIABLE_NO_RESTART || modificationMode > Property.NOT_MODIFIABLE)
			{
				restartRequirementModeParsingFailed = true;
				modificationMode = -1;
			}

			propertyString = (propertyString.substring(firstToken.length())).trim();
		}
		catch(Exception e)
		{
			restartRequirementModeParsingFailed = true;
			restartRequirementModeParsingException = e;
			modificationMode = -1;
		}

		prefix = null;

		StringTokenizer tokens = new StringTokenizer(propertyString, "=", true);

		String name = (String)tokens.nextToken();
		name = name.trim();

		tokens.nextToken();

		while(tokens.hasMoreTokens())
			parsedValueString += (String)tokens.nextToken();

		parsedValueString.trim();

		//owner = null;

		StringProperty s;

		s = new StringProperty(null, name, parsedValueString, modificationMode);

		if(restartRequirementModeParsingFailed)
		{
			if(restartRequirementModeParsingException != null)
         {
				pm.logWarning(fullName, "Exception (" + restartRequirementModeParsingException + ") while parsing modificationMode from property. PropertyString: '" + propertyString + "'. Using defaultvalue!");
         }
			else
         {
				pm.logWarning(fullName, "Error while parsing modificationMode from property. PropertyString: '" + propertyString + "'. Using defaultvalue!");
         }
		}

		return s;
	}

	/**
	 * Gets a String for use in persistent storage.
	 *
	 * @return a string representation of the property.
	 */
	private String formatProperty(final Property p)
	{
		final String val = p.getValueAsString();
		return p.getModificationMode() + "\t" + p.getName().trim() + "=" + ((val != null) ? val.trim() : "");
	}
}
