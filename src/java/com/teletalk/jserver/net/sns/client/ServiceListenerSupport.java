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
package com.teletalk.jserver.net.sns.client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.teletalk.jserver.JServerConstants;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.event.EventQueue;
import com.teletalk.jserver.event.PropertyEvent;
import com.teletalk.jserver.event.PropertyEventListener;
import com.teletalk.jserver.event.StatusEvent;
import com.teletalk.jserver.event.StatusEventListener;
import com.teletalk.jserver.net.sns.Service;
import com.teletalk.jserver.property.MultiStringProperty;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.tcp.TcpEndPointIdentifier;
import com.teletalk.jserver.util.StringUtils;

/**
 * This service lister implementation is provided to facilitate the use of a SNS to obtain addresses for remote services.  
 * ServiceListenerSupport may be used in conjunction with a parent SubComponent object, to wich a single propery is added to 
 * hold the names of the services for which addresses are to be obtained.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20050613)
 */
public class ServiceListenerSupport implements ServiceListener, PropertyEventListener, StatusEventListener
{
   private final SubComponent parentComponent;
   
   private final MultiStringProperty remoteServiceNames;
   
   private final ArrayList remoteServiceAddresses;
   
   
   /**
    * Creates a new ServiceListenerSupport.
    */
   public ServiceListenerSupport(SubComponent parentComponent)
   {
      this(parentComponent, "remoteServiceNames");
   }
   
   /**
    * Creates a new ServiceListenerSupport.
    */
   public ServiceListenerSupport(SubComponent parentComponent, String remoteServicesPropertyName)
   {
      this(parentComponent, "remoteServiceNames", "The names of the services to connect to. " +
         "Addresses for the service names will be fetched from an SNS.");
   }
      
   /**
    * Creates a new ServiceListenerSupport.
    */
   public ServiceListenerSupport(SubComponent parentComponent, String remoteServicesPropertyName, String remoteServicesPropertyDescription)
   {
      this.parentComponent = parentComponent;
      
      this.remoteServiceNames = new MultiStringProperty(parentComponent, remoteServicesPropertyName, "", MultiStringProperty.MODIFIABLE_NO_RESTART);
      this.remoteServiceNames.setDescription(remoteServicesPropertyDescription);
      parentComponent.addProperty(this.remoteServiceNames);
      
      this.remoteServiceAddresses = new ArrayList();
      
      EventQueue eventQueue = parentComponent.getEventQueue();
      if( eventQueue != null ) 
      {
         eventQueue.registerPropertyEventListener(this);
         eventQueue.registerStatusEventListener(this);
      }
   }
   
   /**
    * Called to initialize this ServiceListenerSupport. Normally this is handled automatically by the method {@link #statusChanged(StatusEvent)}. 
    */
   public void initialize()
   {
      if( this.parentComponent.isDebugMode() ) this.parentComponent.logDebug("Initializing ServiceListenerSupport.");
      
      if( this.remoteServiceNames.size() > 0 )
      {
         // Register in SNS client
         SnsClientManager snsClient = SnsClientManager.getSnsClientManager(true);
         snsClient.addServiceListener(this); // This method call will result in a call to servicesUpdated, i.e. initialization of SNS service addresses
      }
   }
   
   /**
    * Notification of a property modificaion.
    * 
    * @param e a PropertyEvent object.
    */
   public void propertyModified(final PropertyEvent e)
   {
      Property property = e.getProperty();
      
      if( property == this.remoteServiceNames )
      {
         if( this.parentComponent.isEnabled() ) 
         {
            if( this.remoteServiceNames.size() > 0 )
            {
               SnsClientManager snsClient = SnsClientManager.getSnsClientManager(true);
               snsClient.addServiceListener(this);
            }
            else
            {
               SnsClientManager snsClient = SnsClientManager.getSnsClientManager();
               if( snsClient != null ) snsClient.removeServiceListener(this);
            }
         }
      }
   }
   
   /**
    * Notification of a status modification.
    * 
    * @param e a StatusEvent object.
    */
   public void statusChanged(StatusEvent e)
   {
      if( e.getSourceComponent() == this.parentComponent )
      {
         if( e.getStatus() == JServerConstants.ENABLED )
         {
            initialize();
         }
      }
   }
   
   /**
    * SNS listener method called when SNS services have been updated.
    */
   public void servicesUpdated(final HashMap services)
   {
      List remoteServiceNameList = this.remoteServiceNames.getValuesAsList();
      Service service;
      ArrayList newRemoteServiceAddresses = new ArrayList();
      int addCount = 0; 
      
      // Iterate through service used by this SnsSupport
      for(int i=0; i<remoteServiceNameList.size(); i++)
      {
         service = (Service)services.get(remoteServiceNameList.get(i));
         if( (service != null) && (service.getAddresses() != null) )
         {
            // Add addresses for service 
            Set serviceAddresses = service.getAddresses();
            Iterator it = serviceAddresses.iterator();
            
            while (it.hasNext())
            {
               String element = (String)it.next();
               newRemoteServiceAddresses.add(TcpEndPointIdentifier.parseTcpEndPointIdentifier(element));
               addCount++;
            }
         }
      }
      
      this.remoteServiceAddressesUpdated(newRemoteServiceAddresses);
      
      if( this.parentComponent.isDebugMode() && (remoteServiceNameList.size() > 0) )
      {
         if( remoteServiceNameList.size() == 1 ) this.parentComponent.logDebug("Addresses for service " + remoteServiceNameList.get(0) + " updated: " + StringUtils.toString(newRemoteServiceAddresses) + ".");
         else this.parentComponent.logDebug("Addresses for services " + StringUtils.toString(remoteServiceNameList) + " updated: " + StringUtils.toString(newRemoteServiceAddresses) + ".");
      }
   }
   
   /**
    * Called when addresses for remote service names have been updated. 
    */
   protected void remoteServiceAddressesUpdated(ArrayList remoteServiceAddresses)
   {
      synchronized(this.remoteServiceAddresses)
      {
         this.remoteServiceAddresses.clear();
         this.remoteServiceAddresses.addAll(remoteServiceAddresses);
      }
   }
   
   /**
    * Gets the addresses for the remote services that were set in the last update from the SNS. 
    */
   public ArrayList getRemoteServiceAddresses()
   {
      synchronized(this.remoteServiceAddresses)
      {
         return (ArrayList)remoteServiceAddresses.clone();
      }
   }
   
   /**
    * Gets the remote service names.
    */
   public String[] getRemoteServiceNames()
   {
      return (String[])remoteServiceNames.getValues(new String[0]);
   }
   
   /**
    * Sets the remote service names.
    */
   public void setRemoteServiceNames(String[] remoteServiceNames)
   {
      this.remoteServiceNames.setValue(remoteServiceNames);
   }
   
   /**
    * Adds a remote service name.
    */
   public void addRemoteServiceName(String remoteServiceName)
   {
      this.remoteServiceNames.addValue(remoteServiceName);
   }
}
