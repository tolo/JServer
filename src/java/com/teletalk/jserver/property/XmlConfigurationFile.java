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

/*
 TODO: Make it possible to link to external xml files containing module configurations
 TODO: Reading of modification mode - is there any use for configuration of modification mode?
 TODO: XML Schema.
*/

package com.teletalk.jserver.property;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import org.xml.sax.SAXException;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.JServerConstants;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.load.LoadManager;
import com.teletalk.jserver.log.CriticalErrorAppenderComponent;
import com.teletalk.jserver.log.FileAppenderComponent;
import com.teletalk.jserver.net.sns.client.SnsClientManager;
import com.teletalk.jserver.net.sns.server.SnsManager;
import com.teletalk.jserver.periodic.PeriodicActionManager;
import com.teletalk.jserver.rmi.RmiManager;
import com.teletalk.jserver.spring.SpringApplicationContext;
import com.teletalk.jserver.statistics.StatisticsManager;
import com.teletalk.jserver.tcp.http.HttpServer;

/**
 * PersistentPropertyStorage implementation that stores configuration to an XML-file.
 * 
 * @since 1.3.2
 * 
 * @author Tobias Löfstrand
 */
public class XmlConfigurationFile implements PersistentPropertyStorage
{
   private static final String JSERVER_ROOT_TAG = "JServer";
   
   
   private static final String CONFIGURATION_MANAGER_TAG = JServerConstants.CONFIGURATION_MANAGER_ALIAS;

   private static final String EVENT_QUEUE_TAG = JServerConstants.EVENT_QUEUE_ALIAS;
            
   private static final String LOG_MANAGER_TAG = JServerConstants.LOG_MANAGER_ALIAS;
   
   private static final String DEFAULT_FILE_LOGGER_TAG = JServerConstants.DEFAULT_FILE_LOGGER_ALIAS; // Since 2.1
   
   private static final String DEFAULT_CRITICAL_ERROR_LOGGER_TAG = JServerConstants.DEFAULT_CRITICAL_ERROR_LOGGER_ALIAS; // Since 2.1
      
   private static final String MAIN_HTTP_SERVER_TAG = JServerConstants.MAIN_HTTP_SERVER_ALIAS;
   
   private static final String RMI_MANAGER_TAG = JServerConstants.RMI_MANAGER_ALIAS; 
   
   private static final String PERIODIC_ACTION_MANAGER_TAG = JServerConstants.PERIODIC_ACTION_MANAGER_ALIAS;
   
   private static final String LOAD_MANAGER_TAG = JServerConstants.LOAD_MANAGER_ALIAS;
   
   private static final String STATISTICS_MANAGER_TAG = JServerConstants.STATISTICS_MANAGER_ALIAS; // Since 2.1
   
   private static final String SPRING_APPLICATION_CONTEXT_TAG = JServerConstants.SPRING_APPLICATION_CONTEXT_ALIAS; // Since 2.1
   
   private static final String SNS_CLIENT_TAG = JServerConstants.SNS_CLIENT_MANAGER_ALIAS; // Since 2.1 (20050503)
   
   private static final String SNS_SERVER_TAG = JServerConstants.SNS_MANAGER_ALIAS; // Since 2.1 (20050503)
   
   private static final String RMI_ADMINISTRATION_TAG = JServerConstants.RMI_ADMINISTRATION_ALIAS; // Administration
   
   
   private static final HashSet ComponentTagNames = new HashSet();  
   
   static
   {
      ComponentTagNames.add(CONFIGURATION_MANAGER_TAG);
      ComponentTagNames.add(LOG_MANAGER_TAG);
      ComponentTagNames.add(DEFAULT_FILE_LOGGER_TAG);
      ComponentTagNames.add(DEFAULT_CRITICAL_ERROR_LOGGER_TAG);
      ComponentTagNames.add(EVENT_QUEUE_TAG);
      ComponentTagNames.add(MAIN_HTTP_SERVER_TAG);
      ComponentTagNames.add(RMI_MANAGER_TAG);
      ComponentTagNames.add(PERIODIC_ACTION_MANAGER_TAG);
      ComponentTagNames.add(LOAD_MANAGER_TAG);
      ComponentTagNames.add(STATISTICS_MANAGER_TAG);
      ComponentTagNames.add(SPRING_APPLICATION_CONTEXT_TAG);
      ComponentTagNames.add(SNS_CLIENT_TAG);
      ComponentTagNames.add(SNS_SERVER_TAG);
      ComponentTagNames.add(RMI_ADMINISTRATION_TAG);
   }
      
   
   private static final String SUB_SYSTEM_TAG = "SubSystem"; // REMOVE?
   
   private static final String SUB_COMPONENT_TAG = "SubComponent"; // REMOVE?
   
   private static final String COMPONENT_TAG = "Component";
   
   private static final String PROPERTY_TAG = "property";
   
   private static final String PROPERTY_META_DATA_TAG = "metaData";
   
   
   private static final String COMPONENT_NAME_ATTRIBUTE = "name";
   
   private static final String COMPONENT_CLASS_ATTRIBUTE = "class";
   
   
   private static final String PROPERTY_NAME_ATTRIBUTE = "name";
   
   private static final String PROPERTY_VALUE_ATTRIBUTE = "value";
   
   
   private static final String PROPERTY_META_DATA_NAME_ATTRIBUTE = "name";
   
   //private static final String PROPERTY_MODIFICATION_MODE_ATTRIBUTE = "mode";

   
   
   private static final String CONFIGURATION_BACKUP_FILE_EXTENSION = ".bak";
   
   private static final String CONFIGURATION_ERROR_BACKUP_FILE_EXTENSION = ".err";
   
   /**   The default filename for properties. */
   public static String DEFAULT_CONFIGURATION_FILE_NAME = "jserver.xml";
   
   private String configurationFileName;
   
   private PropertyManager propertyManager;
   private String fullName;
   
   private DocumentBuilder xmlDocumentBuilder = null;
   
   private Transformer xmlTransformer = null;
   
   private Document configurationFileDocument;
   	
   
   /**
    * Creates a new XmlConfigurationFile.
    */
   public XmlConfigurationFile()
   {
      this(null);
   }
   
   /**
    * Creates a new XmlConfigurationFile.
    */
   public XmlConfigurationFile(final String propertiesFileName)
   {
      this.configurationFileName = propertiesFileName;
      if( this.configurationFileName == null ) this.configurationFileName = DEFAULT_CONFIGURATION_FILE_NAME;
   }
   
   /**
    * Sets the name of the file used for persistent storage of properties.
    * 
    * @param propertiesFileName
    */
   public void setPersistentPropertyStorageFile(final String propertiesFileName)
   {
      this.configurationFileName = propertiesFileName;
      if( this.configurationFileName == null ) this.configurationFileName = DEFAULT_CONFIGURATION_FILE_NAME;
   }
   
   /**
    * Initializes this PersistentPropertyStorage.
    * 
    * @param propertyManager the PropertyManager object to which this PersistentPropertyStorage is to belong to.
    */
   public void init(final PropertyManager propertyManager)
   {
      this.propertyManager = propertyManager;
            
      this.fullName = propertyManager.getFullName() + ".XmlConfigurationFile";
      
      DocumentBuilderFactory docFactory = null;
      
      // Get document builder factory
      try
      {
         docFactory = DocumentBuilderFactory.newInstance();
         docFactory.setValidating(false);
         docFactory.setIgnoringComments(false);
         docFactory.setIgnoringElementContentWhitespace(false);
         //docFactory.setExpandEntityReferences(false);
         
         xmlDocumentBuilder = null;
      
         // Get document builder
         try 
         {
            xmlDocumentBuilder = docFactory.newDocumentBuilder();
         }
         catch (ParserConfigurationException pce) 
         {
            this.propertyManager.logError(this.fullName, "Initialization error - failed to create DocumentBuilder.", pce);
            System.out.println("Initialization error - failed to create DocumentBuilder.");
            pce.printStackTrace();
         }
      }
      catch(Exception e)
      {
         this.propertyManager.logError(this.fullName, "Initialization error - failed to create DocumentBuilder/DocumentBuilderFactory.", e);
         System.out.println("Initialization error - failed to create DocumentBuilder/DocumentBuilderFactory.");
         e.printStackTrace();
      }
      
      TransformerFactory transFactory = null;
      
      // Get transformer factory
      try
      {
         transFactory = TransformerFactory.newInstance();
         
         // Get transformer
         try 
         {
            xmlTransformer = transFactory.newTransformer();
            xmlTransformer.setOutputProperty(OutputKeys.DOCTYPE_PUBLIC, JSERVER_ROOT_TAG);
            xmlTransformer.setOutputProperty(OutputKeys.ENCODING, "ISO-8859-1");
            xmlTransformer.setOutputProperty(OutputKeys.INDENT, "yes");
            xmlTransformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "3");
         }
         catch (TransformerConfigurationException tce) 
         {
            this.propertyManager.logError("Initialization error - failed to create Transformer!", tce);
            System.out.println("Initialization error - failed to create Transformer!");
            tce.printStackTrace();
         }
      }
      catch(Exception e)
      {
         this.propertyManager.logError(this.fullName, "Initialization error - failed to create Transformer/TransformerFactory!", e);
         System.out.println("Initialization error - failed to create Transformer/TransformerFactory!");
         e.printStackTrace();
      }
   }
   
   /**
    * Parses a component element.
    */
   private void parseComponentElement(final Element element, final String componentName)
   {
      final NodeList childNodes = element.getChildNodes();
      Element childElement;
      String childElementName;
      String childComponentName;
      String childComponentClass;
      boolean predefinedComponent;
      String predefinedComponentName;
            
      if( childNodes != null )
      {
         for(int i=0; i<childNodes.getLength(); i++)
         {
            if( childNodes.item(i) instanceof Element )
            {
               childElement = (Element)childNodes.item(i);
               childElementName = childElement.getNodeName();
               
               if( PROPERTY_TAG.equalsIgnoreCase(childElementName) )
               {
                  this.parsePropertyElement(childElement, componentName);
               }
               else
               {
                  predefinedComponentName = null;
                  
                  if( COMPONENT_TAG.equalsIgnoreCase(childElementName) ||
                        SUB_SYSTEM_TAG.equalsIgnoreCase(childElementName) || 
                        SUB_COMPONENT_TAG.equalsIgnoreCase(childElementName) )
                  {
                     predefinedComponent = false;
                  }
                  else
                  {
                     predefinedComponent = true;
                     if( this.isCoreComponentTagName(childElementName) ) predefinedComponentName = childElementName; 
                  }
                   
                  childComponentName = childElement.hasAttribute(COMPONENT_NAME_ATTRIBUTE) ? childElement.getAttribute(COMPONENT_NAME_ATTRIBUTE) : predefinedComponentName;
                  if( childComponentName != null )
                  {
                     // Get component class (by "class" attribute)
                     if( !childElement.hasAttribute(COMPONENT_CLASS_ATTRIBUTE) ) childComponentClass = null;
                     else childComponentClass = childElement.getAttribute(COMPONENT_CLASS_ATTRIBUTE);
                     
                     // Attempt to get implementation class attribute by SubComponent.IMPLEMENTATION_CLASS_NAME_PROPERTY name
                     if( (childComponentClass == null) && childElement.hasAttribute(SubComponent.IMPLEMENTATION_CLASS_NAME_PROPERTY) )
                     {
                        childComponentClass = childElement.getAttribute(SubComponent.IMPLEMENTATION_CLASS_NAME_PROPERTY);
                     }
                     
                     childComponentName = SubComponent.validateName(childComponentClass, childComponentName);
                     
                     // Make childComponentName in to full component name
                     childComponentName = componentName + "." + childComponentName;
                                         
                     if( predefinedComponent )
                     {
                        if( DEFAULT_FILE_LOGGER_TAG.equalsIgnoreCase(childElementName) )
                        {
                           if (childComponentClass == null) childComponentClass = FileAppenderComponent.class.getName();
                           propertyManager.registerDynamicCoreSystem(PropertyManager.DEFAULT_FILE_LOGGER_ALIAS, childComponentName);
                        }
                        if( DEFAULT_CRITICAL_ERROR_LOGGER_TAG.equalsIgnoreCase(childElementName) )
                        {
                           if( childComponentClass == null ) childComponentClass = CriticalErrorAppenderComponent.class.getName();
                           propertyManager.registerDynamicCoreSystem(PropertyManager.DEFAULT_CRITICAL_ERROR_LOGGER_ALIAS, childComponentName);
                        }
                        else if( MAIN_HTTP_SERVER_TAG.equalsIgnoreCase(childElementName) )
                        {
                           if( childComponentClass == null ) childComponentClass = HttpServer.class.getName();
                           propertyManager.registerDynamicCoreSystem(PropertyManager.MAIN_HTTP_SERVER_ALIAS, childComponentName);
                        }
                        else if( RMI_MANAGER_TAG.equalsIgnoreCase(childElementName) )
                        {
                           if( childComponentClass == null ) childComponentClass = RmiManager.class.getName();
                           propertyManager.registerDynamicCoreSystem(PropertyManager.RMI_MANAGER_ALIAS, childComponentName);
                        }
                        else if( PERIODIC_ACTION_MANAGER_TAG.equalsIgnoreCase(childElementName) )
                        {
                           if( childComponentClass == null ) childComponentClass = PeriodicActionManager.class.getName();
                           propertyManager.registerDynamicCoreSystem(PropertyManager.PERIODIC_ACTION_MANAGER_ALIAS, childComponentName);
                        }
                        else if( LOAD_MANAGER_TAG.equalsIgnoreCase(childElementName) )
                        {
                           if( childComponentClass == null ) childComponentClass = LoadManager.class.getName();
                           propertyManager.registerDynamicCoreSystem(PropertyManager.LOAD_MANAGER_ALIAS, childComponentName);
                        }
                        else if( STATISTICS_MANAGER_TAG.equalsIgnoreCase(childElementName) )
                        {
                           if( childComponentClass == null ) childComponentClass = StatisticsManager.class.getName();
                           propertyManager.registerDynamicCoreSystem(PropertyManager.STATISTICS_MANAGER_ALIAS, childComponentName);
                        }                        
                        else if( SPRING_APPLICATION_CONTEXT_TAG.equalsIgnoreCase(childElementName) )
                        {
                           if( childComponentClass == null ) childComponentClass = SpringApplicationContext.class.getName();
                           propertyManager.registerDynamicCoreSystem(PropertyManager.SPRING_APPLICATION_CONTEXT_ALIAS, childComponentName);
                        }                        
                        else if( SNS_CLIENT_TAG.equalsIgnoreCase(childElementName) )
                        {
                           if( childComponentClass == null ) childComponentClass = SnsClientManager.class.getName();
                           propertyManager.registerDynamicCoreSystem(PropertyManager.SNS_CLIENT_MANAGER_ALIAS, childComponentName);
                        }
                        else if( SNS_SERVER_TAG.equalsIgnoreCase(childElementName) )
                        {
                           if( childComponentClass == null ) childComponentClass = SnsManager.class.getName();
                           propertyManager.registerDynamicCoreSystem(PropertyManager.SNS_MANAGER_ALIAS, childComponentName);
                        }
                        else if( RMI_ADMINISTRATION_TAG.equalsIgnoreCase(childElementName) )
                        {
                           propertyManager.registerDynamicCoreSystem(PropertyManager.RMI_ADMINISTRATION_ALIAS, childComponentName);
                        }
                        /*else
                        {
                           // Unknown
                        }*/
                     }
                                          
                     if( childComponentClass != null )
                     {
                        this.propertyManager.addImplementationClassProperty(childComponentName, childComponentClass);
                     }
                                       
                     this.parseComponentElement(childElement, childComponentName);
                  }
                  else
                  {
                     this.propertyManager.logError("No name specified for " + childElementName + " tag under " + element.getTagName() + ". Configuration data will not be read for this component.");
                  }
               }
            }
         }
      }
   }
   
   /**
    */
   private void parsePropertyElement(final Element element, final String ownerName)
   {
      String name = element.getAttribute(PROPERTY_NAME_ATTRIBUTE);
      if( element.hasAttribute(PROPERTY_NAME_ATTRIBUTE) && (name != null) )
      {
         ArrayList values = new ArrayList();
         String bodyTextValue = null;
         HashMap metaData = new HashMap();
         boolean persistentMetaData = false;
         
         String attrValue = element.getAttribute(PROPERTY_VALUE_ATTRIBUTE);
         if( element.hasAttribute(PROPERTY_VALUE_ATTRIBUTE) )
         {
            values.add(attrValue);
         }

         NodeList childNodes = element.getChildNodes();
         Element childElement;
         String childElementName;
         String childElementBodyValue;
         String metaDataName;
         String metaDataValue;
         
         if( childNodes != null )
         {
            for(int i=0; i<childNodes.getLength(); i++)
            {
               if( childNodes.item(i) instanceof Element )
               {
                  childElement = (Element)childNodes.item(i);
                  childElementName = childElement.getNodeName();
                  childElementBodyValue = null;
                  
                  NodeList childChildNodes = childElement.getChildNodes();
                  if( (childChildNodes != null) && (childChildNodes.getLength() > 0) && (childChildNodes.item(0) != null) )
                  {
                     childElementBodyValue = childChildNodes.item(0).getNodeValue();
                  }
                  
                  if( PROPERTY_VALUE_ATTRIBUTE.equalsIgnoreCase(childElementName) && (childElementBodyValue != null) )
                  {
                     values.add(childElementBodyValue);
                  }
                  else if( PROPERTY_META_DATA_TAG.equalsIgnoreCase(childElementName) )
                  {
                     persistentMetaData = true;
                     metaDataName = null;
                     if( childElement.hasAttribute(PROPERTY_META_DATA_NAME_ATTRIBUTE) )
                     {
                        metaDataName = childElement.getAttribute(PROPERTY_META_DATA_NAME_ATTRIBUTE);
                     }
                     
                     metaDataValue = childElementBodyValue; //childElement.getAttribute(PROPERTY_VALUE_ATTRIBUTE);
                    
                     if( metaDataName != null )
                     {
                        if( (metaDataValue == null) && childElement.hasAttribute(PROPERTY_VALUE_ATTRIBUTE) )
                        {
                           metaDataValue = childElement.getAttribute(PROPERTY_VALUE_ATTRIBUTE);
                        }

                        metaData.put(metaDataName, metaDataValue);
                     }
                  }
               }
               else if( (childNodes.item(i) instanceof Text) && (i==0) )
               {
                  bodyTextValue = childNodes.item(i).getNodeValue();
               }
            }
         }
         
         Property property;
         
         if( values.size() > 1 )
         {
            property = new MultiStringProperty(name, (String[])values.toArray(new String[0]));
         }
         else
         {
            String value = null;
            if( values.size() > 0 )
            { 
               value = (String)values.get(0); 
            }
            else
            {
               value = bodyTextValue;               
            }
            
            property = new StringProperty(name, value);
         }
         
         if( persistentMetaData )
         {
            property.setPersistentMetaData(true);
            property.putMetaData(metaData);
         }
                 
         this.propertyManager.addPersistentProperty(property, ownerName);
      }
   }

   /**
    * Reads all the properties stored persistently in the persistent storage represented by this PersistentPropertyStorage. 
    * This method must used the method PropertyManager.addPersistentProperty() to add properties that are read from persistent storage.
    */
   public void readProperties()
   {
      File propertyFile = null;
      
      try
      {
         propertyFile = new File(configurationFileName + CONFIGURATION_BACKUP_FILE_EXTENSION); // Check if  backup propertyfile exists (and if so, use it)

         if( !propertyFile.exists() ) // If temporary property file didn't exist, try to use the standard one
         {
            propertyFile = new File(configurationFileName);
         }

         if( propertyFile.exists() )
         {
            this.propertyManager.logInfo(fullName, "Attempting to read configuration from '" + propertyFile.getAbsolutePath() + "'.");

            this.configurationFileDocument= this.openXMLDocument(propertyFile);
            
            // Get root tag
            final Element jserverRootTag = (this.configurationFileDocument != null) ? (Element)this.configurationFileDocument.getElementsByTagName(JSERVER_ROOT_TAG).item(0) : null;
            
            if( (this.configurationFileDocument != null) && (jserverRootTag != null) )
            {
               JServer jserver = JServer.getJServer();
               if( jserver != null )
               {
                  String jserverName = jserverRootTag.getAttribute(COMPONENT_NAME_ATTRIBUTE);
                  if( (jserverName != null) && (jserverName.trim().length() > 0) )
                  {
                     jserver.rename(jserverName);
                  }
               }
               this.parseComponentElement(jserverRootTag, JServerConstants.JSERVER_TOP_SYSTEM_ALIAS);
            }
            else
            {
               this.propertyManager.logError("Root tag '" + JSERVER_ROOT_TAG + "' not found!");
               System.out.println("Root tag '" + JSERVER_ROOT_TAG + "' not found!");
            }
         }
         else
         {
				this.propertyManager.logWarning(fullName, "Configuration file (" + configurationFileName + ") not found!");
            System.out.println("Configuration file (" + configurationFileName + ") not found!");
         }
      }
      catch(Exception e)
      {
         if( propertyFile != null )
         {
            File errPropertyFile = new File(configurationFileName + CONFIGURATION_ERROR_BACKUP_FILE_EXTENSION);
            if( errPropertyFile.exists() ) errPropertyFile = new File(configurationFileName + CONFIGURATION_ERROR_BACKUP_FILE_EXTENSION + System.currentTimeMillis());
            propertyFile.renameTo(errPropertyFile);
         }
         this.propertyManager.logError("Configuration document parsing failed - Exception while parsing.", e);
         System.out.println("Configuration document parsing failed - Exception while parsing.");
         e.printStackTrace();
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
      File propertyFile = new File(this.configurationFileName);
      File backupPropertyFile = new File(this.configurationFileName + CONFIGURATION_BACKUP_FILE_EXTENSION);
      if( backupPropertyFile.exists() ) backupPropertyFile.delete();
      propertyFile.renameTo(backupPropertyFile);

		try
		{
         Element jserverRootTag;
			if( this.configurationFileDocument == null )
         {
            this.configurationFileDocument = this.xmlDocumentBuilder.newDocument();
            jserverRootTag = this.configurationFileDocument.createElement(JSERVER_ROOT_TAG);
            this.configurationFileDocument.appendChild(jserverRootTag);
         }
         else
         {
            //  Get root tag
            jserverRootTag = (this.configurationFileDocument != null) ? (Element)this.configurationFileDocument.getElementsByTagName(JSERVER_ROOT_TAG).item(0) : null;
         }
            
			if( (this.configurationFileDocument != null) && (jserverRootTag != null) )
			{
            JServer jserver = JServer.getJServer();
            if( jserver != null )
            {
               jserverRootTag.setAttribute(COMPONENT_NAME_ATTRIBUTE, jserver.getName());
            }
            
				this.updateComponentElement(jserverRootTag, JServerConstants.JSERVER_TOP_SYSTEM_ALIAS, persistentProperties, true, 0);
            
            FileOutputStream fout = new FileOutputStream(propertyFile); 
            StreamResult streamResult = new StreamResult(fout); // StreamResult(OutputStream) is used instead of StreamResult(File) due to strange errors in some environments... 
            xmlTransformer.transform(new DOMSource(this.configurationFileDocument), streamResult);
            fout.flush();
            try{
            fout.close();
            }catch(Exception e){}
            streamResult = null;
            System.runFinalization();
            Thread.yield();
            
            backupPropertyFile.delete();
            
            return true;
			}
         
         return false;
		}
		catch(Exception e)
		{
         this.propertyManager.logError(this.fullName, "Error while storing configuration!", e);
         
         return false;
		}
   }
   
	/**
	 */
	private boolean updateComponentElement(final Element element, final String componentName, final Map persistentProperties, final int indentLevel)
	{
	   return updateComponentElement(element, componentName, persistentProperties, false, indentLevel);
	}
   
	/**
	 */
	private boolean updateComponentElement(final Element element, final String componentName, final Map persistentProperties, final boolean isRoot, final int indentLevel)
	{
   	final NodeList childNodes = element.getChildNodes();
		Element childElement;
		String childElementName;
		String childComponentName;
      String predefinedComponentName;
		Property property;
		boolean propertyElementsExists = false;
		List componentProperties = (List)persistentProperties.get(componentName);
		//boolean addJServerPropertiesComment = isRoot && ((childNodes == null) || (childNodes.getLength() == 0)) && (componentProperties != null) && (componentProperties.size() > 0); 
           
      HashSet existingProperyChildElements = new HashSet();
      HashSet existingComponentChildElements = new HashSet();
      
      // Check if property implementationClass is set
      property = this.getPropertyByName(componentProperties, SubComponent.IMPLEMENTATION_CLASS_NAME_PROPERTY);
      if( (property != null) && !this.isCoreComponentTagName(element.getNodeName()) )
      {
         // Check if "implementationClass" or "class" is used as attribute name...
         if( element.hasAttribute(SubComponent.IMPLEMENTATION_CLASS_NAME_PROPERTY) )
         {
            element.removeAttribute(SubComponent.IMPLEMENTATION_CLASS_NAME_PROPERTY);
         }
         element.setAttribute(COMPONENT_CLASS_ATTRIBUTE, property.getValueAsString());
      }
      
		if( childNodes != null )
		{
			// Update existing elements
         for(int i=0; i<childNodes.getLength(); i++)
			{
				if( childNodes.item(i) instanceof Element )
				{
					childElement = (Element)childNodes.item(i);
					childElementName = childElement.getNodeName();
               
					if( PROPERTY_TAG.equalsIgnoreCase(childElementName) )
					{
                  property = this.getPropertyByName(componentProperties, childElement.getAttribute(PROPERTY_NAME_ATTRIBUTE));
                  if( !childElement.hasAttribute(PROPERTY_NAME_ATTRIBUTE) ) property = null;
                  if( (property != null) && property.isPersistent() )
                  {
                     try
                     {
                        existingProperyChildElements.add(property.getName());
                        this.updatePropertyElement(childElement, property);
                        
                        propertyElementsExists = true;
                     }
                     catch(Exception e)
                     {
                        this.propertyManager.logError(this.fullName, "Unable to update property " + property + ".", e);
                     }
                  }
                  else if( property == null ) // If persistent property has been removed (by for instance a call to SubComponent.initFromConfiguredProperty or to PropertyManager.removePersistentProperty directly).
                  {
                     element.removeChild(childElement);
                  }
					}
					else
					{
                  predefinedComponentName = null;
                  
                  if( !COMPONENT_TAG.equalsIgnoreCase(childElementName) &&
                        !SUB_SYSTEM_TAG.equalsIgnoreCase(childElementName) && 
                        !SUB_COMPONENT_TAG.equalsIgnoreCase(childElementName) )
                  {
                     if( this.isCoreComponentTagName(childElementName) ) predefinedComponentName = childElementName;
                  }
                   
                  childComponentName = childElement.hasAttribute(COMPONENT_NAME_ATTRIBUTE) ? childElement.getAttribute(COMPONENT_NAME_ATTRIBUTE) : predefinedComponentName;

                  if( childComponentName != null )
						{
                     existingComponentChildElements.add(childComponentName);
                     
                     //  Make childComponentName in to full component name
                     childComponentName = componentName + "." + childComponentName;
                     
                     try
                     {
   							this.updateComponentElement(childElement, childComponentName, persistentProperties, indentLevel+1);
                     }
                     catch(Exception e)
                     {
                        this.propertyManager.logError(this.fullName, "Unable to update component " + childComponentName + ".", e);
                     }
						}
					}
				}
			}
      }
         
      String name;
      Element newElement;
      
      Node node = null;
      Node addBeforeNode = null;
      if( (childNodes != null) && (childNodes.getLength() > 0) )
      {
         for(int i=0; i<childNodes.getLength(); i++)
         {
            node = childNodes.item(i);
               
            if( !(node instanceof Text) && !(PROPERTY_TAG.equalsIgnoreCase(node.getNodeName())) )
            {
               addBeforeNode = node;
               break;
            }
         }
      }
      
      /*if( addJServerPropertiesComment )
      {
         Comment comment = this.configurationFileDocument.createComment("JServer top system properties begin");
         element.appendChild(this.configurationFileDocument.createTextNode("\n   ")); // Create empty line
         element.appendChild(comment);
         element.appendChild(this.configurationFileDocument.createTextNode("\n   ")); // Create empty line
      }*/
      
      // Add new properties
      if( componentProperties != null )
      {
         for(int i=0; i<componentProperties.size(); i++)
         {
            property = (Property)componentProperties.get(i);
            
            try
            {
               if( (property != null) && property.isPersistent() && !property.isUsingDefaultValue() 
                     && (!existingProperyChildElements.contains(property.getName())) )
               {
                  propertyElementsExists = true;
                  
                  newElement = this.configurationFileDocument.createElement(PROPERTY_TAG);
                  newElement.setAttribute(PROPERTY_NAME_ATTRIBUTE, property.getName());
                  if( addBeforeNode != null )
                  {
                     element.insertBefore(newElement, addBeforeNode);
                  }
                  else
                  {
                     element.appendChild(newElement);
                  }
                  
                  this.updatePropertyElement(newElement, property);
               }
            }
            catch(Exception e)
            {
               this.propertyManager.logError(this.fullName, "Unable to store property " + property + ".", e);
            }
         }
      }
      
      /*if( addJServerPropertiesComment )
      {
         Comment comment = this.configurationFileDocument.createComment("JServer top system properties end");
         element.appendChild(this.configurationFileDocument.createTextNode("\n   ")); // Create empty line
         element.appendChild(comment);
         element.appendChild(this.configurationFileDocument.createTextNode("\n   ")); // Create empty line
         element.appendChild(this.configurationFileDocument.createTextNode("\n   ")); // Create empty line
      }*/
      
      String indent = "";
      for(int i=0; i<indentLevel; i++) indent += "   ";
      
      // Add new components
      ArrayList subComponentNames = getSubComponentNames(persistentProperties, componentName);
      String fullName;
      
      for(int i=0; i<subComponentNames.size(); i++)
      {
         name = (String)subComponentNames.get(i);
         
         if( (name != null) && (!existingComponentChildElements.contains(name)) )
         {
            fullName = componentName + "." + name;
            
            try
            {
               newElement = this.configurationFileDocument.createElement(this.getTagNameForComponent(fullName));
               newElement.setAttribute(COMPONENT_NAME_ATTRIBUTE, name);
                              
               if( this.updateComponentElement(newElement, fullName, persistentProperties, indentLevel+1) ) 
               {
                  element.appendChild(this.configurationFileDocument.createTextNode("\n   " + indent)); // Create empty line
                  element.appendChild(this.configurationFileDocument.createTextNode("\n   " + indent)); // Create empty line
                  element.appendChild(newElement);
                  /*element.appendChild(this.configurationFileDocument.createTextNode("\n" + indent)); // Create empty line
                  element.appendChild(this.configurationFileDocument.createTextNode("\n" + indent)); // Create empty line*/
                  propertyElementsExists = true;
               }
            }
            catch(Exception e)
            {
               this.propertyManager.logError(this.fullName, "Unable to store component " + fullName + ".", e);
            }
         }
      }
      
      return propertyElementsExists;
	}
   
   /**
    */
   private Property getPropertyByName(final List properties, final String propertyName)
   {
      if( properties != null )
      {
         Property property;
         
         for(int i=0; i<properties.size(); i++)
         {
            property = (Property)properties.get(i);
            if( (property != null) && (property.getName().equals(propertyName)) )
            {
               return property;
            }
         }
      }
      
      return null;
   }
   
   /**
    */
   private ArrayList getSubComponentNames(final Map persistentProperties, final String parentComponentName)
   {
      Iterator names = persistentProperties.keySet().iterator();
      String name;
      ArrayList subComponentNames = new ArrayList();
      
      while(names.hasNext())
      {
         name = (String)names.next();
         if( name != null )
         {
            if( (name.startsWith(parentComponentName)) && (name.length() > parentComponentName.length()) )
            {
               name = name.substring(parentComponentName.length() + 1, name.length());
               if( name.indexOf('.') < 0 ) // If direct sub component
               {
                  subComponentNames.add(name);
               }
            }
         }
      }
      
      return subComponentNames;
   }
   
   /**
    */
   private boolean isCoreComponentTagName(final String tagName)
   {
      return ComponentTagNames.contains( tagName );
   }
   
   /**
    */
   private String getTagNameForComponent(final String componentFullName)
   {
      JServer jserver = JServer.getJServer();
      SubComponent subComponent;
      if( jserver != null )
      {
         subComponent = jserver.findSubComponent(componentFullName);
         if( subComponent != null )
         {
            if( subComponent == jserver.getPropertyManager() ) return CONFIGURATION_MANAGER_TAG;
            else if( subComponent == jserver.getLogManager() ) return LOG_MANAGER_TAG;
            else if( subComponent == jserver.getDefaultFileLogger() ) return DEFAULT_FILE_LOGGER_TAG;
            else if( subComponent == jserver.getDefaultCriticalErrorLogger() ) return DEFAULT_CRITICAL_ERROR_LOGGER_TAG;
            else if( subComponent == jserver.getEventQueue() ) return EVENT_QUEUE_TAG; 
            else if( subComponent == jserver.getMainHttpServer() ) return MAIN_HTTP_SERVER_TAG;
            else if( subComponent == jserver.getRmiManager() ) return RMI_MANAGER_TAG;
            else if( subComponent == jserver.getPeriodicActionManager() ) return PERIODIC_ACTION_MANAGER_TAG;
            else if( subComponent == jserver.getLoadManager() ) return LOAD_MANAGER_TAG;
            else if( subComponent == jserver.getStatisticsManager() ) return STATISTICS_MANAGER_TAG;
            else if( subComponent == jserver.getSpringApplicationContext() ) return SPRING_APPLICATION_CONTEXT_TAG;
            else if( subComponent == jserver.getSnsClientManager() ) return SNS_CLIENT_TAG;
            else if( subComponent == jserver.getSnsManager() ) return SNS_SERVER_TAG;
         }
      }
      
      return COMPONENT_TAG;
   }
   
   /**
    */
   private void updatePropertyElement(final Element element, final Property property)
   {
      String values[] = property.getValuesAsStrings();
      if( values == null ) values = new String[0];
            
      HashMap propertyMetaData = property.getMetaData();
      
      NodeList childNodes = element.getChildNodes();
      Node previousNode = null;
      Element childElement;
      String childElementName;
      String metaDataName;
      String newMetaDataValue;
      
      ArrayList elementsToRemove = new ArrayList();
      
      Element newElement;
      
      boolean hasValueNodes = false;
      
      // Remove all existing sub elements (value and meta data) to assure complete update 
      if( childNodes != null )
      {
         for(int i=0; i<childNodes.getLength(); i++)
         {
            if( childNodes.item(i) instanceof Element )
            {
               childElement = (Element)childNodes.item(i);
               childElementName = childElement.getNodeName();
                     
               // Value or Meta data
               if( PROPERTY_VALUE_ATTRIBUTE.equalsIgnoreCase(childElementName) || PROPERTY_META_DATA_TAG.equalsIgnoreCase(childElementName) )
               {
                  if( PROPERTY_VALUE_ATTRIBUTE.equalsIgnoreCase(childElementName) ) hasValueNodes = true;
                  
                  // Remove white space before
                  if( (previousNode != null) && (previousNode instanceof Text) ) elementsToRemove.add(previousNode);
                  
                  // Mark element for removal
                  elementsToRemove.add(childElement);
               }
            }
            
            previousNode = childNodes.item(i);
         }
      }
      
      // Remove elements marked for removal
      for(int i=0; i<elementsToRemove.size(); i++)
      {
         element.removeChild((Node)elementsToRemove.get(i));
      }
            
      // Add new value elements
      if( (values.length <= 1) && !hasValueNodes ) // Single value and no previous value elements...
      {
         String value = (values.length > 0) ? values[0] : "";
         if( value == null ) value = "";
         
         try
         {
            element.setAttribute(PROPERTY_VALUE_ATTRIBUTE, value);
         }
         catch(Exception e)
         {
            // Attempt to create subnode instead
            newElement = this.configurationFileDocument.createElement(PROPERTY_VALUE_ATTRIBUTE);
            newElement.appendChild(configurationFileDocument.createTextNode(value));
            
            element.appendChild(newElement);
         }
      }
      else // Multiple values (or single value that was previously specified through a value sub element) 
      {
         if( element.hasAttribute(PROPERTY_VALUE_ATTRIBUTE) )
         {
            element.removeAttribute(PROPERTY_VALUE_ATTRIBUTE);
         }
         
         for(int i=0; i<values.length; i++)
         {
            newElement = this.configurationFileDocument.createElement(PROPERTY_VALUE_ATTRIBUTE);
            newElement.appendChild(configurationFileDocument.createTextNode(values[i]));
            element.appendChild(newElement);
         }
      }

      // Add meta data
      if( property.isPersistentMetaData() )
      {
	      Iterator it = propertyMetaData.keySet().iterator();
	      
	      // Add new meta data elements
	      while(it.hasNext())
	      {
	         metaDataName = (String)it.next();
	         
	         try
	         {
	            //if( (metaDataName != null) && (!existingMetaDataElements.contains(metaDataName)) && !(PropertyConstants.PROPERTY_DESCRIPTION_KEY.equals(metaDataName)) )
               if( (metaDataName != null) && !(PropertyConstants.PROPERTY_DESCRIPTION_KEY.equals(metaDataName)) )
	            {
	               if( propertyMetaData.get(metaDataName) instanceof String )
	               {
	                  newMetaDataValue = (String)propertyMetaData.get(metaDataName);
	                  
	                  newElement = this.configurationFileDocument.createElement(PROPERTY_META_DATA_TAG);
	                  newElement.setAttribute(PROPERTY_META_DATA_NAME_ATTRIBUTE, metaDataName);
	                  /*try
	                  {
	                     newElement.setAttribute(PROPERTY_VALUE_ATTRIBUTE, newMetaDataValue);
	                  }
	                  catch(Exception e)
	                  {*/
	                     newElement.appendChild(configurationFileDocument.createTextNode((newMetaDataValue != null) ? newMetaDataValue : ""));
	                  //}
	                  
	                  element.appendChild(newElement);
	               }
	            }
	         }
	         catch(Exception e)
	         {
	            this.propertyManager.logError(this.fullName, "Unable to add meta data element ('" + metaDataName + "') to property " + property + ".", e);
	         }
	      }
      }
   }
      
   /**
    * Opens the XML-document at the specified path.
    */
   private Document openXMLDocument(final File xmlFile)
   {
      // Get document
      Document doc = null;
      try 
      {
         doc = xmlDocumentBuilder.parse(xmlFile);
      } 
      catch (SAXException se) 
      {
         this.propertyManager.logError(this.fullName, "Configuration file parsing failed - failed to create Document.", se);
         System.out.println("Configuration file parsing failed - failed to create Document.");
         se.printStackTrace();
         return null;
      }
      catch (IOException ioe) 
      {
         this.propertyManager.logError(this.fullName, "Configuration file parsing failed - failed to create Document.", ioe);
         System.out.println("Configuration file parsing failed - failed to create Document.");
         ioe.printStackTrace();
         return null;
      }
      
      return doc;
   }  
}
