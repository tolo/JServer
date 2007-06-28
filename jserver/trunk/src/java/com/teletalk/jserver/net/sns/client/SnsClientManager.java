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
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.net.sns.Service;
import com.teletalk.jserver.net.sns.SnsConstants;
import com.teletalk.jserver.tcp.messaging.Destination;
import com.teletalk.jserver.tcp.messaging.Message;
import com.teletalk.jserver.tcp.messaging.MessagingManager;
import com.teletalk.jserver.tcp.messaging.rpc.RpcMessageReceiver;
import com.teletalk.jserver.util.EqualsUtils;
import com.teletalk.jserver.util.StringUtils;

/**
 * This class implements an messaging based SNS client.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20050502)
 */
public class SnsClientManager extends MessagingManager implements SnsClient
{
   private static SnsClientManager singletonSnsClient = null; // ONLY used in standalone mode, i.e. no JServer main system
   
   
   /**
    * Singleton method for getting the SnsClient. This method will not create a new SnsClient if 
    * none exists.
    * 
    * @return the SnsClient or <code>null</code> if none has been created. 
    */
   public static SnsClientManager getSnsClientManager()
   {
      return getSnsClientManager(false, null);
   }
   
   /**
    * Singleton method for getting the SnsClient. This method will create a new SnsClient if 
    * none exists and the parameter <code>create</code> is <code>true</code>.
    * 
    * @param create indicating if a new SnsClient should be created if none exists.
    * 
    * @return the SnsClient or <code>null</code> if none has been created. 
    */
   public static SnsClientManager getSnsClientManager(boolean create)
   {
      return getSnsClientManager(create, null);
   }
   
   /**
    * Singleton method for getting the SnsClient. This method will create a new SnsClient if 
    * none exists and the parameter <code>create</code> is <code>true</code>.
    * 
    * @param create indicating if a new SnsClient should be created if none exists.
    * @param name the name of the new SnsClient, or null to use the default.
    * 
    * @return the SnsClient or <code>null</code> if none has been created. 
    */
   public static SnsClientManager getSnsClientManager(boolean create, final String name)
   {
      synchronized(SnsClientManager.class)
      {
         JServer jserver = JServer.getJServer();
         SnsClientManager snsClientManager = SnsClientManager.singletonSnsClient;
         if( jserver != null ) snsClientManager = jserver.getSnsClientManager();
         
         if( (snsClientManager == null) && create )
         {
            snsClientManager = new SnsClientManager(JServer.getJServer(), name);
            
            setSnsClientManager(snsClientManager);
         }
         
         return snsClientManager;
      }
   }
   
   /**
    * Sets the global SnsClientManager.
    * 
    * @since 2.1.1 (20060109)
    */
   public static void setSnsClientManager(SnsClientManager snsClientManager)
   {
      JServer jserver = JServer.getJServer();
      
      if( jserver != null ) jserver.setSnsClientManager(snsClientManager);
      else // Standalone mode
      {
         SnsClientManager.singletonSnsClient = snsClientManager;
         SnsClientManager.singletonSnsClient.engage();
      }
   }
   
   
   private final HashMap destinationServices; // String (destination address) -> HashMap ( String (name) -> Service )
   
   private final HashMap services; // String (name) -> Service
   
   
      
   private final ArrayList serviceProviders;
   
   private final ArrayList serviceListeners;
   
   
   private final Object mainLock;
   
   
   private final RpcMessageReceiver snsMessageReceiver;

   
   /**
    * Creates a new SnsClientManager.
    */
   public SnsClientManager()
   {
      this(null);
   }
   
   /**
    * Creates a new SnsClientManager.
    */
   public SnsClientManager(SubSystem parent)
   {
      this(parent, "SnsClientManager");
   }
   
   /**
    * Creates a new SnsClientManager.
    */
   public SnsClientManager(SubSystem parent, String name)
   {
      this(parent, name, true);
   }
   
   /**
    * Creates a new SnsClientManager.
    */
   public SnsClientManager(SubSystem parent, String name, boolean disableMessagingManagerSnsProperties)
   {
      super(parent, (name != null) ? name : "SnsClientManager");
      
      this.snsMessageReceiver = new RpcMessageReceiver(this);
      this.snsMessageReceiver.setDefaultHandler(this);
      
      super.registerMessageReceiver(snsMessageReceiver, SnsConstants.SNS_CLIENT_MESSAGE_HANDLER_NAME);
      super.setDefaultMessageReceiver(snsMessageReceiver); // Deprecated as of 2.1 (20050530)
      
      this.destinationServices = new HashMap();
      this.services = new HashMap();
      
      this.serviceProviders = new ArrayList();
      this.serviceListeners = new ArrayList();
      
      this.mainLock = this.destinationServices;
      
      // Make sure that the key SnsConstants.SNS_SERVICES_META_DATA_KEY exists in the meta data, 
      // since this is used by the SnsServer to determine if the client is an SNS client
      super.setMetaData(SnsConstants.SNS_SERVICES_META_DATA_KEY, null);
      
      if( disableMessagingManagerSnsProperties )
      {
         super.removeProperty(super.remoteServiceNames);
         super.removeProperty(super.localServiceNames);
         super.exposeMessageReceiversAsServices.setForceMode(true);
         super.exposeMessageReceiversAsServices.setNotificationMode(false);
         super.exposeMessageReceiversAsServices.setValue(false);
         super.removeProperty(super.exposeMessageReceiversAsServices);
      }
      
      
      // Set no of connections to 1 by default
      super.connectionsPerDestination.setForceMode(true);
      super.connectionsPerDestination.setNotificationMode(false);
      super.connectionsPerDestination.setDefaultValue(new Long(1));
      super.connectionsPerDestination.setValue(1);
      super.connectionsPerDestination.setForceMode(false);
      super.connectionsPerDestination.setNotificationMode(true);
   }
   
   /**
    * Registers a ServiceProvider.
    */
   public void addServiceProvider(final ServiceProvider serviceProvider)
   {
      if( (serviceProvider != null) && !(serviceProvider instanceof SnsClientManager) ) // Don't register SnsClientManagers as service providers
      {
         if( super.isDebugMode() ) super.logDebug("Adding service provider: " + serviceProvider + ".");
         
         synchronized(this.serviceProviders)
         {
            if( !serviceProviders.contains(serviceProvider) )
            {
               serviceProviders.add(serviceProvider);
            }
         
            rebuildServiceNames();
         }
      }
   }
   
   /**
    * Removes a registered ServiceProvider.
    */
   public void removeServiceProvider(final ServiceProvider serviceProvider)
   {
      if( super.isDebugMode() ) super.logDebug("Removing service provider: " + serviceProvider + ".");
      
      synchronized(this.serviceProviders)
      {
         serviceProviders.remove(serviceProvider);

         rebuildServiceNames();
      }
   }
   
   /**
    * Invoked by a ServiceProvider to indicate that the ServiceProvider is updated.
    */
   public void serviceProviderUpdated(final ServiceProvider serviceProvider)
   {
      if( super.isDebugMode() ) super.logDebug("Service provider updated: " + serviceProvider + ".");
      
      synchronized(this.serviceProviders)
      {
         rebuildServiceNames();
      }
   }
   
   /**
    * Registers a ServiceListener.
    */
   public void addServiceListener(final ServiceListener serviceListener)
   {
      if( super.isDebugMode() ) super.logDebug("Adding service listener: " + serviceListener + ".");
      
      if( serviceListener == null ) return;
      
      synchronized(this.mainLock)
      {
         // Notify listener of current services
         HashMap servicesClone = (HashMap)this.services.clone();
         serviceListener.servicesUpdated(servicesClone);
                  
         if( !this.serviceListeners.contains(serviceListener) )
         {
            this.serviceListeners.add(serviceListener);
         }
      }
   }
   
   /**
    * Removes a registered ServiceListener.
    */
   public void removeServiceListener(final ServiceListener serviceListener)
   {
      if( super.isDebugMode() ) super.logDebug("Removing service listener: " + serviceListener + ".");
      
      synchronized(this.mainLock)
      {
         this.serviceListeners.remove(serviceListener);
      }
   }
      
   /**
    * Gets the service with the specified name, available via the connected SNS(es).
    */
   public Service getRemoteService(final String serviceName)
   {
      synchronized(this.mainLock)
      {
         Service service = (Service)this.services.get(serviceName);
         if( service != null ) return service;
      }
      return null;
   }
   
   /**
    * Gets the services available via the connected SNS(es).
    */
   public List getRemoteServices()
   {
      synchronized(this.mainLock)
      {
         return new ArrayList(this.services.values());
      }
   }
   
   
   /* ### RPC METHODS ### */
   
   
   /**
    * Gets the version of the SnsClient interface.
    *  
    * @since 2.1.3 (20060324)
    */
   public int getInterfaceVersion()
   {
      return SnsClient.INTERFACE_VERSION;
   }
   
   
   /**
    * Called by an SNS to perform a complete update of the available services.
    */
   public void serviceRegistryRefresh(final HashMap services)
   {
      Message message = MessagingManager.getCurrentMessage();
      
      if( this.isDebugMode() ) logDebug("Service registry refresh received from SNS (" +message.getDestination() + ") - " + StringUtils.toString(services) + ".");
      
      synchronized(this.mainLock)
      {
         Iterator it = services.values().iterator();
         boolean dirtyFlag = false;
         Service service;
         while (it.hasNext())
         {
            service = (Service) it.next();
            if( this.destinationServiceUpdated(service, message.getDestination()) ) dirtyFlag = true;
         }
         
         if( dirtyFlag ) this.merge();
      }
   }
   
   /**
    * Called by an SNS when a service is updated.
    */
   public void serviceUpdated(final Service service)
   {
      Message message = MessagingManager.getCurrentMessage();
      
      if( this.isDebugMode() ) logDebug("Service update received from SNS (" +message.getDestination() + ") - " + service + ".");

      synchronized(this.mainLock)
      {
         if( this.destinationServiceUpdated(service, message.getDestination()) )
         {
            this.merge();
         }
      }
   }
   
   
   /* ### INTERNAL METHODS ### */
   
   
   /**
    * Rebuilds the service names.
    */
   private void rebuildServiceNames()
   {
      ServiceProvider[] serviceProviderArray = (ServiceProvider[])serviceProviders.toArray(new ServiceProvider[0]);
      HashSet serviceNames = new HashSet();
      List providerServiceNames;
      
      for(int i=0; i<serviceProviderArray.length; i++)
      {
         if( serviceProviderArray[i] != null )
         {
            providerServiceNames = serviceProviderArray[i].getProvidedServices();
            if( providerServiceNames != null )
            {
               Object serviceName;
               for (int j = 0; j<providerServiceNames.size(); j++)
               {
                  serviceName = providerServiceNames.get(j);
                  if( serviceName != null ) serviceNames.add(serviceName.toString());
               }
            }
         }
      }
      
      String[] serviceArray = (String[])serviceNames.toArray(new String[0]);
      List providerServices;
      //HashMap serviceMap = new HashMap();
      Service service;
      List services = new ArrayList();
      HashSet serviceAddresses; 
      
      for (int i=0; i<serviceArray.length; i++) // serviceArray - i 
      {
         if( serviceArray[i] != null )
         {
            serviceAddresses = new HashSet();
            
            for (int p = 0; p<serviceProviderArray.length; p++) // serviceProviderArray - p
            {
               if( serviceProviderArray[p] != null )
               {
                  providerServices = serviceProviderArray[p].getProvidedServiceAddresses(serviceArray[i]);
                  if( providerServices != null )
                  {
                     serviceAddresses.addAll(providerServices);
                  }
               }
            }
            
            service = new Service(serviceArray[i], serviceAddresses);
            services.add(service);
            //serviceMap.put(serviceArray[i], (String[])serviceAddresses.toArray(new String[0]));
         }
      }
      
      super.setMetaData(SnsConstants.SNS_SERVICES_META_DATA_KEY, services);
      
      if( this.isDebugMode() ) logDebug("Local services updated - " + StringUtils.toString(services) + ".");
   }
   
   /**
    * Registeres a new or updated service.
    */
   private boolean destinationServiceUpdated(final Service service, final Destination destination)
   {
      if( service == null ) return false;
            
      HashMap destinationServiceMap = (HashMap)this.destinationServices.get(destination.getAddress());
      if( destinationServiceMap == null )
      {
         destinationServiceMap = new HashMap();
         this.destinationServices.put(destination.getAddress(), destinationServiceMap);
      }
      
      Service existingService = null;
      
      if( (service.getAddresses() != null) && (service.getAddresses().size() > 0) ) // Don't add empty services...
      {
         // Replace old service in destination services map
         existingService = (Service)destinationServiceMap.put(service.getName(), service);
      }
      else
      {
         // Otherwise remove service
         existingService = (Service)destinationServiceMap.remove(service.getName());
      }
      
      return !EqualsUtils.equals(service, existingService); // Dirty flag 
   }
   
   /**
    * Merges services from different SNS:s into a single Map. 
    */
   private void merge()
   {
      if( this.isDebugMode() ) logDebug("Merging remote services.");
      
      HashMap[] destinationServicesMaps = (HashMap[])this.destinationServices.values().toArray(new HashMap[0]); // Service maps of all destinations (sns:s)
      HashMap newMergedServices = new HashMap();
      
      Service newMergedService;
      Service destinationService;
            
      // Merge destination services
      for (int i=0; i<destinationServicesMaps.length; i++) // Iterate over all destination service maps
      {
         if (destinationServicesMaps[i] != null)
         {
            Iterator it = destinationServicesMaps[i].keySet().iterator();
            while (it.hasNext()) // Iterate over service names in destination service map
            {
               String name = (String) it.next();
               if( name != null )
               {
                  destinationService = (Service)destinationServicesMaps[i].get(name);
                  if( destinationService != null )
                  {
                     newMergedService = (Service)newMergedServices.get(name);
                     if( newMergedService != null )
                     {
                        newMergedService.addAddresses(destinationService.getAddresses());
                     }
                     else
                     {
                        newMergedService = new Service(name);
                        newMergedService.addAddresses(destinationService.getAddresses());
                        newMergedServices.put(name, newMergedService);
                     }
                  }
               }
            }
         }
      }
      
      // Merge service names
      Set allServiceNamesSet = new HashSet();
      allServiceNamesSet.addAll(newMergedServices.keySet());
      allServiceNamesSet.addAll(this.services.keySet());
      String[] allServiceNames = (String[])allServiceNamesSet.toArray(new String[0]);
      
      Service existingService;
      Service newService;
      //ArrayList modifiedServices = new ArrayList();
      int modCount = 0;
      
      // Compare with existing and find out which services are modified
      for (int i=0; i<allServiceNames.length; i++)
      {
         newService = (Service)newMergedServices.get(allServiceNames[i]);
         existingService = (Service)this.services.get(allServiceNames[i]);
         
         if( (existingService != null) && (newService != null) ) // Modified
         {
            //if( !existingService.equals(newService) ) modifiedServices.add(newService);
            if( !existingService.equals(newService) ) modCount++;
         }
         else if( (existingService == null) && (newService != null) ) // Added
         {
            //modifiedServices.add(newService);
            modCount++;
         }
         else if( (existingService != null) && (newService == null) ) // Removed
         {
            //modifiedServices.add(new Service(existingService.getName())); // Add empty service to list
            modCount++;
         }
      }
      
      // Notify service listeners
      /*Service modifiedService;
      for(int s=0; s<modifiedServices.size(); s++)
      {
         modifiedService = (Service)modifiedServices.get(s);
         for(int i=0; i<this.serviceListeners.size(); i++)
         {
            ((ServiceListener)this.serviceListeners.get(i)).serviceUpdated(modifiedService);
         }
      }*/
      
      HashMap newMergedServicesClone = (HashMap)newMergedServices.clone();

      if( modCount > 0 )
      {
         // Notify service listeners
         for(int i=0; i<this.serviceListeners.size(); i++)
         {
            ((ServiceListener)this.serviceListeners.get(i)).servicesUpdated(newMergedServicesClone);
         }
      }
                  
      this.services.clear();
      this.services.putAll(newMergedServices);
   }
}
