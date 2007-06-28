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
package com.teletalk.jserver.net.sns.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.net.sns.Service;
import com.teletalk.jserver.tcp.messaging.Destination;

/**
 * The repository used by SnsManager for storage of servers and service
 *
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20050502) 
 */
public class ServiceRepository
{
   private final SubComponent parentComponent;
   
   private final HashMap destinationIdToSnsClientDestination; // Long (destinationId) -> SnsClientDestination
   
   private final HashMap serverNameToSnsClientDestination; // String (unique server name) -> SnsClientDestination
   
   private final HashMap services; // String (name) -> Service
   

   /**
    * Creates a new ServiceRepository.
    */
   public ServiceRepository(SubComponent logger)
   {
      this.parentComponent = logger;
      this.destinationIdToSnsClientDestination = new HashMap();
      this.serverNameToSnsClientDestination = new HashMap();
      this.services = new HashMap();
   }
   
   /**
    * Gets all the remote SNS client representations.
    */
   public SnsClientDestination[] getSnsClientDestinations()
   {
      return (SnsClientDestination[])this.serverNameToSnsClientDestination.values().toArray(new SnsClientDestination[0]);
   }
   
   /**
    * Gets all the remote SNS client ids.
    */
   public String[] getSnsClientDestinationIds()
   {
      return (String[])this.serverNameToSnsClientDestination.keySet().toArray(new String[0]);
   }
   
   /**
    * Gets all the remote SNS client ids.
    */
   public Set getSnsClientDestinationIdsSet()
   {
      return this.serverNameToSnsClientDestination.keySet();
   }
   
   /**
    * Compiles a map of all service available in the remote SNS clients.
    */
   public HashMap getRemoteServices()
   {
      SnsClientDestination[] remoteSnsClientArray = (SnsClientDestination[])this.serverNameToSnsClientDestination.values().toArray(new SnsClientDestination[0]);
            
      HashMap newServices = new HashMap();
      List services;
      Service globalService;
      Service serverService;
      
      // Rebuild service map
      for (int i=0; i<remoteSnsClientArray.length; i++)
      {
         if( !remoteSnsClientArray[i].getServer().isLinkLost() ) // Don't include servers to which the link is lost
         {
            services = remoteSnsClientArray[i].getServer().getServices();
            for(int s=0; s<services.size(); s++)
            {
               serverService = (Service)services.get(s);
               if( serverService != null )
               {
                  globalService = (Service)newServices.get(serverService.getName());
                  if( globalService == null )
                  {
                     globalService = new Service(serverService.getName(), new HashSet());
                     newServices.put(globalService.getName(), globalService);
                  }
                  globalService.getAddresses().addAll(serverService.getAddresses());
               }
            }
         }
      }
      
      return newServices;
   }
   
   /**
    * Rebuilds the internal structure containing all services.
    */
   public ArrayList rebuildServices(final HashMap additionalServices)
   {
      // Get remote services from remote sns clients
      HashMap newServices = this.getRemoteServices();
      
      // Merge newServices and additionalServices
      Iterator additionalServicesIterator = additionalServices.keySet().iterator();
      String additionalServiceName;
      Service service;
      Service additionalService;
      while(additionalServicesIterator.hasNext())
      {
         additionalServiceName = (String)additionalServicesIterator.next();
         additionalService = (Service)additionalServices.get(additionalServiceName);
         if( additionalService != null )
         {
            service = (Service)newServices.get(additionalServiceName);
            if( service == null ) newServices.put(additionalServiceName, additionalService);
            else
            {
               service.getAddresses().addAll(additionalService.getAddresses());
            }
         }
      }
      
      Set allServiceNamesSet = new HashSet();
      allServiceNamesSet.addAll(newServices.keySet());
      allServiceNamesSet.addAll(this.services.keySet());
      String[] allServiceNames = (String[])allServiceNamesSet.toArray(new String[0]);
      
      Service newService;
      Service existingService;
      ArrayList modifiedServices = new ArrayList();
      
      // Compare with existing and find out which services are modified
      for (int i=0; i<allServiceNames.length; i++)
      {
         newService = (Service)newServices.get(allServiceNames[i]);
         existingService = (Service)this.services.get(allServiceNames[i]);
         
         if( (existingService != null) && (newService != null) ) // Modified
         {
            if( !existingService.equals(newService) )
            {
               if( parentComponent.isDebugMode() ) parentComponent.logDebug("Service modified - " + newService.toString(false) + ".");
               modifiedServices.add(newService);
            }
         }
         else if( (existingService == null) && (newService != null) ) // Added
         {
            if( parentComponent.isDebugMode() ) parentComponent.logDebug("Service added - " + newService.toString(false) + ".");
            modifiedServices.add(newService);
         }
         else if( (existingService != null) && (newService == null) ) // Removed
         {
            if( parentComponent.isDebugMode() ) parentComponent.logDebug("Service removed - " + existingService.toString(false) + ".");
            modifiedServices.add(new Service(existingService.getName())); // Add empty service to list
         }
      }
            
      this.services.clear();
      this.services.putAll(newServices);
      
      return modifiedServices;
   }
   
   /**
    * Registers a new SnsClientDestination.
    */
   public void addSnsClientDestination(final SnsClientDestination snsClientDestination)
   {
      this.destinationIdToSnsClientDestination.put(new Long(snsClientDestination.getDestination().getDestinationId()), snsClientDestination);
      this.serverNameToSnsClientDestination.put(snsClientDestination.getName(), snsClientDestination);
   }
   
   /**
    * Gets a SnsClientDestination.
    */
   public SnsClientDestination getSnsClientDestination(final Destination destination)
   {
      return (SnsClientDestination)this.destinationIdToSnsClientDestination.get(new Long(destination.getDestinationId()));
   }
      
   /**
    * Gets a SnsClientDestination.
    */
   public SnsClientDestination getSnsClientDestination(final String uniqueServerName)
   {
      return (SnsClientDestination)serverNameToSnsClientDestination.get(uniqueServerName);
   }
   
   /**
    * Removes a registered SnsClientDestination (the mapping between id/server name and SnsClientDestination).
    */
   public SnsClientDestination removeSnsClientDestination(final String uniqueServerName)
   {
      return (SnsClientDestination)serverNameToSnsClientDestination.remove(uniqueServerName);
   }
   
   /**
    * Removes a registered SnsClientDestination (the mapping between destination id and SnsClientDestination).
    */
   public SnsClientDestination removeActiveSnsClientDestination(final Destination destination)
   {
      if( destination != null ) return (SnsClientDestination)this.destinationIdToSnsClientDestination.remove(new Long(destination.getDestinationId()));
      else return null;
   }
   
   /**
    * Gets all services.
    */
   public HashMap getServices()
   {
      return services;
   }
   
   /**
    * Gets a service.
    */
   public Service getService(String key)
   {
      return (Service)services.get(key);
   }
}
