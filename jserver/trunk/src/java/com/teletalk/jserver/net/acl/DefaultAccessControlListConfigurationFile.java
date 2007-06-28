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
package com.teletalk.jserver.net.acl;

import java.io.FileInputStream;
import java.util.HashSet;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Handles parsing of the access control configuration file.<br>
 * <br>
 * Configuration file example:<br>
 * <br>
 * <pre>
 * <code>
 * &lt;accessControlList&gt;
 * 
 *   &lt;interface name="internet"&gt;
 *     &lt;operation id="testMethod_0"/&gt;
 *     &lt;operation id="testMethod_1"/&gt;
 *   &lt;/interface&gt;
 *   
 *   &lt;interface name="intranet"&gt;
 *      &lt;operation id="testMethod_0"/&gt;
 *      &lt;operation id="testMethod_1"/&gt;
 *      &lt;operation id="testMethod_2"/&gt;
 *  &lt;/interface&gt;
 * 
 *  &lt;/accessControlList&gt;
 * </code>
 * </pre>
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0.2 (20050331)
 */
public class DefaultAccessControlListConfigurationFile
{
   private static final String ID_ATTRIBUTE_NAME = "id";
   
   private static final String NAME_ATTRIBUTE_NAME = "name";
   
   
   private static final String ACCESS_CONTROL_ROOT_TAG_NAME = "accessControlList";
   
   private static final String INTERFACE_TAG_NAME = "interface";
   
   private static final String OPERATION_TAG_NAME = "operation";
   
   private static final String ADMIN_TAG_NAME = "admin";
   
   
   /**
    * Reads and parses the access control configuration file with the specified name.
    *  
    * @param fileName the path of the access control configuration file.
    * @param accessControlListHandler the associated {@link AccessControlListHandler}. 
    * 
    * @throws Exception if an error occurs during reading/parsing.
    */
   public static void readAccessControlList(final String fileName, final DefaultAccessControlListHandler accessControlListHandler) throws Exception
   {
      DocumentBuilderFactory docFactory = null;
      
      // Get document builder factory
      docFactory = DocumentBuilderFactory.newInstance();
      docFactory.setValidating(false);
      docFactory.setIgnoringComments(false);
      docFactory.setIgnoringElementContentWhitespace(false);
      
      DocumentBuilder xmlDocumentBuilder = docFactory.newDocumentBuilder();
      
      Document configurationFileDocument = xmlDocumentBuilder.parse(new FileInputStream(fileName)); //openXMLDocument(new FileInputStream(fileName));
      
      // Get root tag
      final Element rootTag = (configurationFileDocument != null) ? (Element)configurationFileDocument.getElementsByTagName(ACCESS_CONTROL_ROOT_TAG_NAME).item(0) : null;
    
      if( rootTag != null )
      {
         // Iterate though level one tags
         final NodeList childNodes = rootTag.getChildNodes();
         Node node;
         
         for(int i=0; i<childNodes.getLength(); i++)
         {
            node = childNodes.item(i);
            
            if( (node != null) && (node instanceof Element) && (node.getNodeName() != null) )
            {
               if( node.getNodeName().trim().equalsIgnoreCase(INTERFACE_TAG_NAME) )
               {
                  parseInterface((Element)node, accessControlListHandler);
               }
            }
         }  
      }
      else
      {
         throw new RuntimeException("Root node (" + ACCESS_CONTROL_ROOT_TAG_NAME + ") not found!");
      }
   }
   
   /**
    * Parses an interface tag.
    */
   private static void parseInterface(final Element interfaceElement, final DefaultAccessControlListHandler accessControlListHandler)
   {
      final NodeList childNodes = interfaceElement.getChildNodes();
      Node node;
      Element element;
      String interfaceName = interfaceElement.getAttribute(NAME_ATTRIBUTE_NAME);
      HashSet operations = new HashSet();
      
      for(int i=0; i<childNodes.getLength(); i++)
      {
         node = childNodes.item(i);
         
         if( (node != null) && (node instanceof Element) && (node.getNodeName() != null) )
         {
            // operation tag
            if( node.getNodeName().trim().equalsIgnoreCase(OPERATION_TAG_NAME) )
            {
               element = (Element)node;
               
               operations.add( element.getAttribute(ID_ATTRIBUTE_NAME) );
            }
            // admin tag
            else if( node.getNodeName().trim().equalsIgnoreCase(ADMIN_TAG_NAME) )
            {
               accessControlListHandler.registerServerAdministrationAccess(interfaceName);
            }
         }
      }
      
      accessControlListHandler.registerInterfacePermissions(interfaceName, operations);
   }
}
