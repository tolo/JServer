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

import java.util.HashMap;
import java.util.HashSet;

import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.property.BooleanProperty;
import com.teletalk.jserver.property.StringProperty;

/**
 * This class handles (network)interface based access checks. Interfaces for which access control checks are to be peformed on are 
 * represented as simple strings. These strings may for instance be associated with ip-adresses as value ids (see 
 * {@link com.teletalk.jserver.property.IpAndPortProperty} or {@link com.teletalk.jserver.property.MultiValueProperty}<br>
 * <br>
 * <span style="font-family:courier;font-size:14px"><b>Properties:</b><br></span>
 * <ul> 
 * <span style="font-family:courier;font-size:11px">
 * <li><b>accessControlCheckEnabled</b> - Flag indicating if the access control check is enabled or not.</li>
 * <li><b>configurationFile</b> - The path of the access control configuration file.</li>
 * </span>
 * </ul>
 *  
 * @since 2.0.2 (20050331)
 * 
 * @see DefaultAccessControlListConfigurationFile
 */
public class DefaultAccessControlListHandler extends SubComponent implements AccessControlListHandler
{
   private final BooleanProperty accessControlCheckEnabled;
   
   private final StringProperty configurationFile;
   
   private final HashMap interfacePermissions = new HashMap();
   
   private final HashSet serverAdministrationAccess = new HashSet();
   
   /**
    * Creates a new AccessControlListHandler.
    */
   public DefaultAccessControlListHandler()
   {
      this(null);
   }
   
   /**
    * Creates a new AccessControlListHandler.
    * 
    * @param parent the parent of this AccessControlListHandler.
    */
   public DefaultAccessControlListHandler(SubComponent parent)
   {
      this(parent, true);
   }
   
   /**
    * Creates a new AccessControlListHandler.
    * 
    * @param parent the parent of this AccessControlListHandler.
    */
   public DefaultAccessControlListHandler(SubComponent parent, String name)
   {
      this(parent, name, true);
   }
   
   /**
    * Creates a new AccessControlListHandler.
    * 
    * @param parent the parent of this AccessControlListHandler.
    * @param accessControlCheckEnabled flag indicating if accessControlCheckEnabled should be enabled.
    */
   public DefaultAccessControlListHandler(SubComponent parent, boolean accessControlCheckEnabled)
   {
      this(parent, "AccessControlListHandler", accessControlCheckEnabled);
   }
   
   /**
    * Creates a new AccessControlListHandler.
    * 
    * @param parent the parent of this AccessControlListHandler.
    * @param accessControlCheckEnabled flag indicating if accessControlCheckEnabled should be enabled.
    */
   public DefaultAccessControlListHandler(SubComponent parent, String name, boolean accessControlCheckEnabled)
   {
      super(parent, name);
      
      this.accessControlCheckEnabled = new BooleanProperty(this, "accessControlCheckEnabled", accessControlCheckEnabled, BooleanProperty.MODIFIABLE_OWNER_RESTART);
      this.accessControlCheckEnabled.setDescription("Flag indicating if the access control check is enabled or not.");
      super.addProperty(this.accessControlCheckEnabled);
      
      this.configurationFile = new StringProperty(this, "configurationFile", "acl.xml", StringProperty.MODIFIABLE_OWNER_RESTART);
      this.configurationFile.setDescription("The path of the access control configuration file.");
      super.addProperty(this.configurationFile);
   }
   
   /**
    * Initializes this AccessControlListHandler. 
    */
   protected void doInitialize()
   {
      super.doInitialize();
      
      this.interfacePermissions.clear();
      
      if( this.accessControlCheckEnabled.booleanValue() )
      {
         try
         {
            DefaultAccessControlListConfigurationFile.readAccessControlList(this.configurationFile.stringValue(), this);
            
            super.logInfo("Access control configuration parsed: " + this.interfacePermissions + ".");
         }
         catch(Exception e)
         {
            super.logError("Failed to parse access control configuration file (" + this.configurationFile.stringValue() + ")!", e);
            //super.logCriticalError("Failed to parse access control configuration file (" + this.configurationFile.stringValue() + ")!", e, LOG_MESSAGE_ID_CRITICAL_JSERVER_ERROR);
         }
         
         if( interfacePermissions.isEmpty() )
         {
            super.logInfo("Access control configuration is empty - setting accessControlCheckEnabled to false.");
            this.accessControlCheckEnabled.setValue(false);
         }
      }
   }
   
   /**
    * Checks if access control is enabled or not.
    */
   public boolean isAccessControlCheckEnabled()
   {
      return accessControlCheckEnabled.booleanValue();
   }
   
   /**
    * Sets the flag indicating if access control is enabled or not.
    */
   public void setAccessControlCheckEnabled(final boolean accessControlCheckEnabled)
   {
      this.accessControlCheckEnabled.setValue(accessControlCheckEnabled);
   }
   
   /**
    * Registers a set of operation permissions for a specific interface.
    *  
    * @param interfaceName the name of the interface.
    * @param permissions the set of permissions.
    */
   public void registerInterfacePermissions(final String interfaceName, final HashSet permissions)
   {
      this.interfacePermissions.put(interfaceName, permissions);  
   }
   
   /**
    * Registers server administration access for the specified interface.
    *  
    * @param interfaceName the name of the interface.
    * 
    * @since 2.1 (20050427)
    */
   public void registerServerAdministrationAccess(final String interfaceName)
   {
      this.serverAdministrationAccess.add(interfaceName);
   }
   
   /**
    * Checks if the specified interfaces has a specific operation permission. If the access control 
    * is disabled (@see {@link #isAccessControlCheckEnabled()}) this method always returns <code>true</code>. 
    * 
    * @param interfaceName the name of the interface. 
    * @param operationId the id of a permission.
    * 
    * @return <code>true</code> if the interface has permission, otherwise <code>false</code>. This method also 
    * returns <code>true</code> is access control check is disabled.
    */
   public boolean checkAccess(final String interfaceName, final String operationId)
   {
      if( this.accessControlCheckEnabled.booleanValue() && this.isEnabled() )
      {
         HashSet operations = null;

         operations = (HashSet)this.interfacePermissions.get(interfaceName);  
         
         if( operations != null ) return operations.contains(operationId);
         else return false;
      }
      else return true; // If access control check is disabled, always return true
   }
   
   /**
    * Checks if the specified interface has administration permission.
    * 
    * @since 2.1 (20050427)
    * 
    * @return <code>true</code> if the interface has permission, otherwise <code>false</code>. This method also 
    * returns <code>true</code> is access control check is disabled.
    */
   public boolean hasServerAdministrationAccess(final String interfaceName)
   {
      if( this.accessControlCheckEnabled.booleanValue() && this.isEnabled() )
      { 
         return this.serverAdministrationAccess.contains(interfaceName);
      }
      else return true; // If access control check is disabled, always return true
   }
}
