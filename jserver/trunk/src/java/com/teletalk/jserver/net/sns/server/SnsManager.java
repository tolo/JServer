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
import java.util.StringTokenizer;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.JServerConstants;
import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.net.acl.AccessControlListHandler;
import com.teletalk.jserver.net.sns.Server;
import com.teletalk.jserver.net.sns.Service;
import com.teletalk.jserver.net.sns.SnsConstants;
import com.teletalk.jserver.net.sns.client.SnsClient;
import com.teletalk.jserver.property.MapProperty;
import com.teletalk.jserver.property.NumberProperty;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.tcp.messaging.DefaultMessageProcessor;
import com.teletalk.jserver.tcp.messaging.Destination;
import com.teletalk.jserver.tcp.messaging.Message;
import com.teletalk.jserver.tcp.messaging.MessageHeader;
import com.teletalk.jserver.tcp.messaging.MessagingManager;
import com.teletalk.jserver.tcp.messaging.ObjectMessageWriter;
import com.teletalk.jserver.tcp.messaging.rpc.MessagingRpcInterface;
import com.teletalk.jserver.tcp.messaging.rpc.RemoteProcedureCall;
import com.teletalk.jserver.tcp.messaging.rpc.RpcInputStream;
import com.teletalk.jserver.tcp.messaging.rpc.RpcMessageReceiver;
import com.teletalk.jserver.util.EqualsUtils;
import com.teletalk.jserver.util.MessageQueueThread;

/**
 * This class implements an messaging based SNS server.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20050502)
 */
public class SnsManager extends MessagingManager implements SnsServer
{
   /**
    * InternalMessageProcessor
    */
   private static final class InternalMessageProcessor extends DefaultMessageProcessor
   {
      private final SnsManager snsManager;
      
      /**
       */
      public InternalMessageProcessor(SnsManager snsManager)
      {
         this.snsManager = snsManager;
      }
      
      /**
       * Method intercepted to enable forwarding of server administration RPC calls to a connected server.
       */
      public boolean handleServerAdministrationMessage(final Message message, final AccessControlListHandler accessControlListHandler) throws Exception
      {
         String methodName = RemoteProcedureCall.getRPCMethodName(message);
                  
         if( (methodName != null) && (methodName.indexOf(SnsConstants.SNS_ADMINISTRATION_PROXY_RPC_HANDLER_PREFIX) > -1 ) )
         {
            return handleSnsSeverCall(message, methodName);
         }
         else return super.handleServerAdministrationMessage(message, accessControlListHandler);
      }
      
      /**
       * Forwards of an RPC calls to a connected server.
       */
      public boolean handleSnsSeverCall(final Message message, String methodName) throws Exception
      {
         final MessageHeader header = message.getHeader();
         int indexOfNextDot = methodName.indexOf('.', SnsConstants.SNS_ADMINISTRATION_PROXY_RPC_HANDLER_PREFIX.length());
         
         if( indexOfNextDot > -1 )
         {
            String serverName = methodName.substring(SnsConstants.SNS_ADMINISTRATION_PROXY_RPC_HANDLER_PREFIX.length(), indexOfNextDot);
            methodName = methodName.substring(indexOfNextDot + 1);

            SnsClientDestination snsClientDestination = this.snsManager.serviceRepository.getSnsClientDestination(serverName);
            
            if( snsClientDestination != null ) 
            {
               if( header.hasCustomHeaderField(RpcInputStream.RPC_INPUTSTREAM_HEADER_KEY) )
               {
                  // Update method name
                  header.setCustomHeaderField(MessagingRpcInterface.RPC_METHOD_NAME, methodName);
                  
                  return this.snsManager.dispatchProxiedMessage(message, snsClientDestination.getDestination(), null, true);
               }
               else
               {
                  RemoteProcedureCall remoteProcedureCall = (RemoteProcedureCall)message.getBodyAsObject();
                                    
                  // Update method name
                  remoteProcedureCall.setMethodName(methodName);
                  header.setCustomHeaderField(MessagingRpcInterface.RPC_METHOD_NAME, methodName);
   
                  return this.snsManager.dispatchProxiedMessage(message, snsClientDestination.getDestination(), null, true, 
                        new ObjectMessageWriter(remoteProcedureCall));
               }
            }
         }
         
         return false;
      }
   }
   
   
   private static SnsManager singletonSnsServer = null; // ONLY used in standalone mode, i.e. no JServer main system
    
   
   /**
    * Singleton method for getting the SnsServer. This method will not create a new SnsServer if 
    * none exists.
    * 
    * @return the SnsServer or <code>null</code> if none has been created. 
    */
   public static SnsManager getSnsManager()
   {
      return getSnsManager(false, null);
   }
   
   /**
    * Singleton method for getting the SnsServer. This method will create a new SnsServer if 
    * none exists and the parameter <code>create</code> is <code>true</code>.
    * 
    * @param create indicating if a new SnsServer should be created if none exists.
    * 
    * @return the SnsServer or <code>null</code> if none has been created. 
    */
   public static SnsManager getSnsManager(boolean create)
   {
      return getSnsManager(create, null);
   }
   
   /**
    * Singleton method for getting the SnsServer. This method will create a new SnsServer if 
    * none exists and the parameter <code>create</code> is <code>true</code>.
    * 
    * @param create indicating if a new SnsServer should be created if none exists.
    * @param name the name of the new SnsServer, or null to use the default.
    * 
    * @return the SnsServer or <code>null</code> if none has been created. 
    */
   public static SnsManager getSnsManager(boolean create, final String name)
   {
      synchronized(SnsManager.class)
      {
         JServer jserver = JServer.getJServer();
         SnsManager snsManager = SnsManager.singletonSnsServer;
         if( jserver != null ) snsManager = jserver.getSnsManager();
         
         if( (snsManager == null) && create )
         {
            snsManager = new SnsManager(JServer.getJServer(), name);
            
            if( jserver != null ) jserver.setSnsManager(snsManager);
            else
            {
               SnsManager.singletonSnsServer = snsManager;
               SnsManager.singletonSnsServer.engage();
            }
         }
         
         return snsManager;
      }
   }
   
   
   private final RpcMessageReceiver snsMessageReceiver;
   
   private final ServiceRepository serviceRepository;
   
   private final Object mainLock;
   
   private final MessageQueueThread sequentialNotificationThread;
   
   
   private final NumberProperty serverEntryLingerTime; // Minutes
   
   private final MapProperty manualServices;
   
      
   
   /**
    * Creates a new SnsManager.
    */
   public SnsManager()
   {
      this(null, "SnsManager");
   }
   
   /**
    * Creates a new SnsManager.
    */
   public SnsManager(SubSystem parent)
   {
      this(parent, "SnsManager");
   }
   
   /**
    * Creates a new SnsManager.
    */
   public SnsManager(SubSystem parent, String name)
   {
      super(parent, (name != null) ? name : "SnsManager");
      
      super.setResponseMessageTimeOut(5000); // Set a low response message timeout as default
      super.setCheckInteval(60*1000); // Set a longer check interval
      
      this.serviceRepository = new ServiceRepository(this);
      this.mainLock = this.serviceRepository;
      
      
      // Register RPC message receiver for SnsServer interface
      this.snsMessageReceiver = new RpcMessageReceiver(this);
      this.snsMessageReceiver.setDefaultHandler(this);
      super.registerMessageReceiver(this.snsMessageReceiver, SnsConstants.SNS_SERVER_MESSAGE_HANDLER_NAME);
      
      // Register custom MessageProcessor that relays administration messages to the correct server
      super.setMessageProcessor(new InternalMessageProcessor(this));
      
      
      this.serverEntryLingerTime = new NumberProperty(this, "serverEntryLingerTime", 60, NumberProperty.MODIFIABLE_NO_RESTART);
      this.serverEntryLingerTime.setDescription("The maximum time in minutes that a server entry will be kept in this SNS after the connection has been lost.");
      addProperty(this.serverEntryLingerTime);
      
      this.manualServices = new MapProperty(this, "manualServices", new HashMap(), MapProperty.MODIFIABLE_NO_RESTART);
      this.manualServices.setDescription("Storage for manually configured services. Format: <serviceName>=<address:port>,<address:port>.");
      addProperty(this.manualServices);
            
      this.sequentialNotificationThread = new MessageQueueThread(this.getFullName() + ".SequentialNotificationThread");
   }
   
   /**
    * Called when a property owne by this MessagingManager is modified.
    * 
    * @param property the property that was modified.
    */
   public void propertyModified(final Property property)
   {
      if (property == this.manualServices)
      {
         synchronized (this.mainLock)
         {
            this.rebuildServices();
         }
      }
      
      super.propertyModified(property);
   }
   
   /**
    * Gets a map containing the manually configured services. 
    */
   public HashMap getManualServices()
   {
      HashMap manualServiceMap = new HashMap();
      HashMap manualServiceValueMap = this.manualServices.getMappings();
      
      if( manualServiceValueMap != null )
      {
         Iterator it = manualServiceValueMap.keySet().iterator();
         String name;
         String value;
         Service service;
         while (it.hasNext())
         {
            name = (String)it.next();
            if( name != null )
            {
               value = (String)manualServiceValueMap.get(name);
               if( value != null )
               {
                  service = new Service(name);
                  HashSet addresses = new HashSet();
                  try
                  {
                     StringTokenizer tokenizer = new StringTokenizer(value, ",;");
                     while(tokenizer.hasMoreTokens()) addresses.add(tokenizer.nextToken());
                  }
                  catch(Exception e)
                  {
                     logError("Error parsing values for manual service " + name + ".", e);
                  }
                  
                  service.setAddresses(addresses);
                  manualServiceMap.put(service.getName(), service);
               }
            }
         }
      }
      
      return manualServiceMap;
   }
   
   /**
    * Performs a periodic check of this SnsManager, which involves checking for lingering servers as well as dispatching 
    * complete service updates.  
    */
   protected void performPeriodicCheck() throws Exception
   {
      super.performPeriodicCheck();
      
      synchronized(this.mainLock)
      {
         //RemoteSnsClient[] serverEntries = (RemoteSnsClient[])this.remoteSnsClients.values().toArray(new RemoteSnsClient[0]);
         SnsClientDestination[] remoteSnsClients = this.serviceRepository.getSnsClientDestinations();
         long serverEntryLingerTimeMs = this.serverEntryLingerTime.longValue() * 60 * 1000;
         
         // Remove lingering servers
         for (int i=0; i<remoteSnsClients.length; i++)
         {
            if ( (remoteSnsClients[i] != null) && (remoteSnsClients[i].getServer() != null) )
            {
               // If link lost - check linger time - 
               if( (remoteSnsClients[i].isLinkLost()) && (System.currentTimeMillis() - remoteSnsClients[i].getServer().getLinkLostTime()) > serverEntryLingerTimeMs )
               {
                  // Remove server entry
                  if( this.isDebugMode() ) logDebug("Linger time expired - removing server entry " + remoteSnsClients[i].getServer().toString(false) + ".");
                  this.serviceRepository.removeSnsClientDestination(remoteSnsClients[i].getName());
                  remoteSnsClients[i].destroy();
               }
            }
         }
         
         // Send services refresh
// TODO: Send services refresh - is this necessary?
         for (int i=0; i<remoteSnsClients.length; i++)
         {
            if ( (remoteSnsClients[i] != null) && (remoteSnsClients[i].getServer() != null) )
            {
               if( !remoteSnsClients[i].isLinkLost() )
               {
                  this.serviceRegistryRefresh(remoteSnsClients[i], this.serviceRepository.getServices());
               }
            }
         }
      }
   }
   
   /**
    * Rebuilds the services and sends service refreshes/updates to clients. Must be invoked with a lock on mainLock.
    */
   private void rebuildServices()
   {
      rebuildServices(null);
   }
   
   /**
    * Rebuilds the services and sends service refreshes/updates to clients. Must be invoked with a lock on mainLock.
    * 
    * @param excludeSnsClientDestination a SnsClientDestination which should be excluded from a service refresh/update.
    */
   private void rebuildServices(final SnsClientDestination excludeSnsClientDestination)
   {
      if( this.isDebugMode() ) logDebug("Rebuilding services.");
      
      ArrayList modifiedServices = this.serviceRepository.rebuildServices(this.getManualServices());
      SnsClientDestination[] remoteSnsClientArray = this.serviceRepository.getSnsClientDestinations();
      
      boolean performRegistryRefresh = (modifiedServices != null) && (modifiedServices.size() > 1);
            
      if( (modifiedServices != null) && (modifiedServices.size() > 0) )
      {
         // Notify clients of modified services
         for (int i=0; i<remoteSnsClientArray.length; i++)
         {
            if( (remoteSnsClientArray[i] != null) && !remoteSnsClientArray[i].isLinkLost() && 
                  (remoteSnsClientArray[i] != excludeSnsClientDestination) )
            {
               if( performRegistryRefresh )
               {
                  serviceRegistryRefresh(remoteSnsClientArray[i], this.serviceRepository.getServices());
               }
               else
               {
                  serviceUpdated(remoteSnsClientArray[i], (Service)modifiedServices.get(0));
               }
            }
         }
      }
   }
         
   /**
    * Gets a RemoteSnsClient for the specified destination. Must be invoked with a lock on mainLock.
    */
   private SnsClientDestination getRemoteSnsClient(final Destination destination, boolean create)
   {
      SnsClientDestination snsClientDestination = null;
      
      HashMap metaData = destination.getDestinationMetaData();
      if( (metaData != null) && (metaData.containsKey(SnsConstants.SNS_SERVICES_META_DATA_KEY)) ) // Check if the destination is an SNS client
      {
         String serverName = destination.getServerName();
                  
         if( serverName != null )
         {
            if( create )
            {
               String uniqueServerName = serverName;
               snsClientDestination = this.serviceRepository.getSnsClientDestination(uniqueServerName);
               
               if( snsClientDestination != null ) // Server name already existed - this is possibly a reconnect 
               {
                  // Destroy old snsClientDestination
                  this.destroySnsClientDestination(snsClientDestination.getDestination(), snsClientDestination);
                  
                  if( !snsClientDestination.isLinkLost() ) // Still active connection to existing server - server name conflict - remove existing server!
                  {
                     logCriticalError("Server name collision ('" + serverName + "') while registering new SnsClient. New server will override existing server. Address of existing server: " + snsClientDestination.getDestination().getAddress() + 
                           ", address of new server: " + destination.getAddress() + ". Existing server is removed.", JServerConstants.LOG_MESSAGE_ID_CRITICAL_SUBSYSTEM_ERROR);
                  }
                  snsClientDestination = null; // Reset to force creation of new SnsClientDestination
               }
               
               if( snsClientDestination == null ) snsClientDestination = new SnsClientDestination(uniqueServerName);
               
               // Initialize SnsClientDestination
               this.initSnsClientDestination(destination, snsClientDestination);
               
               // Register SnsClientDestination (and map it to destination id and server name)
               this.serviceRepository.addSnsClientDestination(snsClientDestination);
            }
            else
            {
               // Get SnsClientDestination by Destination to ensure correct SnsClientDestination is returned (in case there are name collisions)
               snsClientDestination = this.serviceRepository.getSnsClientDestination(destination);
            }
         }
         else
         {
// TODO: Log critical?            
            logError("Unable to obtain server name for SNS client (" + destination + ")! Client registration cannot proceed!");
         }
      }
      
      return snsClientDestination;
   }
   
   /**
    * Initializes a SnsClientDestination. Must be invoked with a lock on mainLock.
    */
   private void initSnsClientDestination(final Destination destination, final SnsClientDestination snsClientDestination)
   {
      final String serverAddress = destination.getAddress().toString();
                  
      List destinationServices = null;
      HashMap metaData = destination.getDestinationMetaData();
      if( metaData != null )
      {
         destinationServices = (List)metaData.get(SnsConstants.SNS_SERVICES_META_DATA_KEY);
      }
      if( destinationServices == null ) destinationServices = new ArrayList();
      
      // Init Server object, representing the server.
      Server server = snsClientDestination.getServer();
      server.setSnsClientAddress(serverAddress);
      server.setServices(destinationServices);
      server.setMetaData(destination.getDestinationMetaData());
      server.resetLinkLostTime();
      
      MessagingRpcInterface rpcInterface = super.getMessagingRpcInterface(destination, SnsConstants.SNS_CLIENT_MESSAGE_HANDLER_NAME);
      SnsClient snsClient = (SnsClient)rpcInterface.createProxy(SnsClient.class);
      
      snsClientDestination.setDestination(destination);
      snsClientDestination.setSnsClient(snsClient);
      
      // Enable remote administration by adding a ServerAdministrationClient to the rpc handler of the message receiver of this system
/*      this.snsMessageReceiver.addHandler(server.getAdministrationProxyRpcHandlerName(), 
            new ServerAdministrationClient(this, destination));
*/            
   }
   
   /**
    * Destroys (deinitializes) a SnsClientDestination. Must be invoked with a lock on mainLock.
    */
   private void destroySnsClientDestination(final Destination destination, final SnsClientDestination snsClientDestination)
   {
      // Reset destination and SnsClient rpc interface
/*      this.snsMessageReceiver.removeHandler(SnsConstants.SNS_ADMINISTRATION_PROXY_RPC_HANDLER_PREFIX + 
            snsClientDestination.getServer().getName()); // Remove admin client
*/      
      this.serviceRepository.removeActiveSnsClientDestination(destination);
   }
   
   
   /* ### NOTIFICATION METHODS BEGIN ### */
   
   
   /**
    * Called when a link has been established with a new destination.
    */
   protected void destinationLinkEstablished(final Destination destination)
   {
      this.sequentialNotificationThread.queueMessage(new Runnable(){
         public void run()
         {
            doDestinationLinkEstablished(destination);
         }
      });
   }
   
   /**
    * Called when a link has been established with a new destination.
    */
   void doDestinationLinkEstablished(final Destination destination)
   {
      synchronized(this.mainLock)
      {
         SnsClientDestination remoteSnsClient = getRemoteSnsClient(destination, true);
         
         if( remoteSnsClient != null )
         {
            super.logInfo("SNS client established link - " + destination + ".");
            
            this.rebuildServices(remoteSnsClient); // Excluded remoteSnsClient from the potential service refresh/update...
            
            // ... and refresh manually
            this.serviceRegistryRefresh(remoteSnsClient, this.serviceRepository.getServices());
         }
      }
   }
   
   /**
    * Called when a link with a destination has been lost.
    */
   protected void destinationLinkLost(final Destination destination)
   {
      this.sequentialNotificationThread.queueMessage(new Runnable(){
         public void run()
         {
            doDestinationLinkLost(destination);
         }
      });
   }
   
   /**
    * Called when a link with a destination has been lost.
    */
   void doDestinationLinkLost(final Destination destination)
   {
      synchronized(this.mainLock)
      {
         // Remove/mark as bad
         SnsClientDestination snsClientDestination = getRemoteSnsClient(destination, false);
         if( snsClientDestination != null )
         {
            snsClientDestination.setLinkLost(System.currentTimeMillis()); // Mark link lost
            
            // Destroy (deinit)
            this.destroySnsClientDestination(destination, snsClientDestination);
                        
            this.rebuildServices(); // ...rebuild services
         }
      }
   }
   
   /**
    * Called when the meta data of a remote messaging manager has been updated.
    */
   protected void destinationMetaDataUpdated(final Destination destination, final HashMap previousDestinationMetaData)
   {
      this.sequentialNotificationThread.queueMessage(new Runnable(){
         public void run()
         {
            doDestinationMetaDataUpdated(destination, previousDestinationMetaData);
         }
      });
   }
   
   /**
    * Called when the meta data of a remote messaging manager has been updated.
    */
   void doDestinationMetaDataUpdated(final Destination destination, final HashMap previousDestinationMetaData)
   {
      super.destinationMetaDataUpdated(destination, previousDestinationMetaData);
      
      HashMap metaData = destination.getDestinationMetaData();
      
      List destinationServices = null;
      if( metaData != null ) destinationServices = (List)metaData.get(SnsConstants.SNS_SERVICES_META_DATA_KEY);
      
      List previousDestinationServices = null;
      if( previousDestinationMetaData != null ) previousDestinationServices = (List)previousDestinationMetaData.get(SnsConstants.SNS_SERVICES_META_DATA_KEY);
         
      // If sns services meta data modified...
      if( !EqualsUtils.equals(destinationServices, previousDestinationServices) )
      {
         synchronized(this.mainLock)
         {
            // Update...
            SnsClientDestination remoteSnsClient = getRemoteSnsClient(destination, false);
            if( remoteSnsClient != null )
            {
               Server server = remoteSnsClient.getServer();
               server.setMetaData(metaData);
               remoteSnsClient.getServer().setServices(destinationServices);
               
               rebuildServices(); // ...rebuild services
            }
         }
      }
   }
   
   
   /* ### NOTIFICATION METHODS END ### */
   
   
   /**
    * Invokes the serviceRegistryRefresh method of the specified SnsClientDestination, in the sequential client remote 
    * call thread of the SnsClientDestination.
    */
   private void serviceRegistryRefresh(final SnsClientDestination remoteSnsClient, final HashMap services)
   {
      MessageQueueThread sequentialClientRemoteCallThread = remoteSnsClient.getSequentialClientRemoteCallThread();
      if( sequentialClientRemoteCallThread != null )
      {
         sequentialClientRemoteCallThread.queueMessage(new Runnable(){
            public void run()
            {
               doServiceRegistryRefresh(remoteSnsClient, services);
            }
         });
      }
   }
   
   /**
    * Invokes the serviceRegistryRefresh method of the specified SnsClientDestination.
    */
   void doServiceRegistryRefresh(final SnsClientDestination remoteSnsClient, final HashMap services)
   {
      try
      {
         remoteSnsClient.getSnsClient().serviceRegistryRefresh(services);
      }
      catch(Exception e)
      {
         this.logError("Error performing server registry refresh for server " + remoteSnsClient.getName() + "/" + remoteSnsClient.getSnsClientAddress() +"!", e);
      }
   }
   
   /**
    * Invokes the serviceUpdated method of the specified SnsClientDestination, in the sequential client remote 
    * call thread of the SnsClientDestination.
    */
   private void serviceUpdated(final SnsClientDestination remoteSnsClient, final Service service)
   {
      MessageQueueThread sequentialClientRemoteCallThread = remoteSnsClient.getSequentialClientRemoteCallThread();
      if( sequentialClientRemoteCallThread != null )
      {
         sequentialClientRemoteCallThread.queueMessage(new Runnable(){
            public void run()
            {
               doServiceUpdated(remoteSnsClient, service);
            }
         });
      }
   }
   
   /**
    * Invokes the serviceUpdated method of the specified SnsClientDestination.
    */
   void doServiceUpdated(final SnsClientDestination remoteSnsClient, final Service service)
   {
      try
      {
         remoteSnsClient.getSnsClient().serviceUpdated(service);
      }
      catch(Exception e)
      {
         this.logError("Error performing service update for server " + remoteSnsClient.getName() + "/" + remoteSnsClient.getSnsClientAddress() +"!", e);
      }
   }
   
   
   /* ### EXTERNAL INTERFACE ### */
   
   
   /**
    * Gets the version of the SnsServer interface.
    *  
    * @since 2.1.3 (20060324)
    */
   public int getInterfaceVersion()
   {
      return SnsServer.INTERFACE_VERSION;
   }
   
   /**
    * Gets a list of all servers connected to the SNS server.
    */
   public List getServers()
   {
      ArrayList servers = new ArrayList();
      
      synchronized(this.mainLock)
      {
         SnsClientDestination[] serverEntries = this.serviceRepository.getSnsClientDestinations(); //(RemoteSnsClient[])this.remoteSnsClients.values().toArray(new RemoteSnsClient[0]);
         for (int i=0; i<serverEntries.length; i++)
         {
            servers.add(serverEntries[i].getServer());
         }
      }
      
      return servers;
   }
   
   /**
    * Gets a list of all servers connected to the SNS server, containing the service with the specified name.
    */
   public List getServers(final String serviceName)
   {
      ArrayList servers = new ArrayList();
      
      synchronized(this.mainLock)
      {
         SnsClientDestination[] serverEntries = this.serviceRepository.getSnsClientDestinations(); //(RemoteSnsClient[])this.remoteSnsClients.values().toArray(new RemoteSnsClient[0]);
         for (int i=0; i<serverEntries.length; i++)
         {
            if( serverEntries[i].getServer().hasService(serviceName) )
            {
               servers.add(serverEntries[i].getServer());
            }
         }
      }
      
      return servers;
   }
   
   /**
    * Gets a list of all available services in the SNS server.
    */
   public List getServices()
   {
      synchronized(this.mainLock)
      {
         return new ArrayList(this.serviceRepository.getServices().values());
      }
   }
   
   /**
    * Gets the service with the specified name from the SNS server.
    */
   public Service getService(final String serviceName)
   {
      synchronized(this.mainLock)
      {
         Service service = (Service)this.serviceRepository.getService(serviceName);
         if( service != null ) return service;
      }
      return null;
   }
}
