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
package com.teletalk.jserver.tcp.messaging.admin;

import java.io.InputStream;
import java.util.Properties;

import com.teletalk.jserver.JServerConstants;
import com.teletalk.jserver.log.LogData;
import com.teletalk.jserver.net.admin.remote.RemoteJServer;
import com.teletalk.jserver.net.admin.remote.RemoteProperty;
import com.teletalk.jserver.net.admin.remote.RemoteSubComponent;
import com.teletalk.jserver.net.admin.remote.RemoteSubSystem;
import com.teletalk.jserver.net.admin.remote.RemoteVectorProperty;
import com.teletalk.jserver.rmi.remote.RemoteSubComponentData;
import com.teletalk.jserver.tcp.messaging.Destination;
import com.teletalk.jserver.tcp.messaging.MessageHeader;
import com.teletalk.jserver.tcp.messaging.MessagingManager;
import com.teletalk.jserver.tcp.messaging.rpc.MessagingRpcInterface;
import com.teletalk.jserver.tcp.messaging.rpc.RpcException;

/**
 * Client implementation of RPC based messaging remote administration of JServer.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0.1 (20040924)
 */
public class ServerAdministrationClient implements ServerAdministrationInterface
{
   protected final MessagingRpcInterface messagingRpcInterface;
   
   /** @since 2.1.4 (20060508) */
   protected final String rpcHandlerName;
   
   protected final ServerAdministrationInterface serverAdministrationInterfaceProxy;
   
   private int remoteVersion = -1;

   
   /**
    * Creates a new ServerAdministrationClient.
    */
   public ServerAdministrationClient(final MessagingManager messagingManager, final Destination remoteServer)
   {
      this(messagingManager.getMessagingRpcInterface(remoteServer));
   }
   
   /**
    * Creates a new ServerAdministrationClient.
    */
   public ServerAdministrationClient(final MessagingRpcInterface messagingRpcInterface)
   {
      this(messagingRpcInterface, null);
   }
   
   /**
    * Creates a new ServerAdministrationClient.
    * 
    * @since 2.1.2 (20060324)
    */
   public ServerAdministrationClient(final MessagingRpcInterface messagingRpcInterface, final String rpcHandlerName)
   {
      this.messagingRpcInterface = messagingRpcInterface;
      this.rpcHandlerName = rpcHandlerName;
      
      if( rpcHandlerName != null ) this.serverAdministrationInterfaceProxy = (ServerAdministrationInterface)this.messagingRpcInterface.createProxy(ServerAdministrationInterface.class, rpcHandlerName); 
      else this.serverAdministrationInterfaceProxy = (ServerAdministrationInterface)this.messagingRpcInterface.createProxy(ServerAdministrationInterface.class);
      
      MessageHeader prototypeHeader = this.messagingRpcInterface.getMessageDispatcher().getPrototypeMessageHeader();
      if( prototypeHeader == null ) prototypeHeader = new MessageHeader();
      prototypeHeader.setHeaderType(MessageHeader.SERVER_ADMINISTRATION_HEADER);
      this.messagingRpcInterface.getMessageDispatcher().setPrototypeMessageHeader(prototypeHeader);
   }
   
   /**
    * Gets the associated MessagingRpcInterface.
    * 
    * @since 2.1.2 (20060315)
    */
   public MessagingRpcInterface getMessagingRpcInterface()
   {
      return this.messagingRpcInterface;
   }
   
   /**
    * Gets the RPC handler name used when creating proxies via the associated {@link #getMessagingRpcInterface() MessagingRpcInterface}.
    * 
    * @since 2.1.4 (20060508)
    */
   public String getRpcHandlerName()
   {
      return rpcHandlerName;
   }

   /**
    * Gets a remote interface for interacting with the JServer object in the server.
    * 
    * @since 2.1.2 (20060315)
    */
   public RemoteJServer getRemoteJServer()
   {
      return (RemoteJServer)this.getRemoteObject(RemoteJServer.class, JServerConstants.JSERVER_TOP_SYSTEM_ALIAS);
   }
   
   /**
    * Gets a remote interface for interacting with a SubSystem object in the server.
    * 
    * @param fullName the full name of the remote object.
    * 
    * @since 2.1.2 (20060315)
    */
   public RemoteSubSystem getRemoteSubSystem(final String fullName)
   {
      return (RemoteSubSystem)this.getRemoteObject(RemoteSubSystem.class, fullName);
   }
   
   /**
    * Gets a remote interface for interacting with a SubComponent object in the server.
    * 
    * @param fullName the full name of the remote object.
    * 
    * @since 2.1.2 (20060315)
    */
   public RemoteSubComponent getRemoteSubComponent(final String fullName)
   {
      return (RemoteSubComponent)this.getRemoteObject(RemoteSubComponent.class, fullName);
   }
   
   /**
    * Gets a remote interface for interacting with a Property object in the server.
    * 
    * @param fullName the full name of the remote object.
    * 
    * @since 2.1.2 (20060315)
    */
   public RemoteProperty getRemoteProperty(final String fullName)
   {
      return (RemoteProperty)this.getRemoteObject(RemoteProperty.class, fullName);
   }
   
   /**
    * Gets a remote interface for interacting with a VectorProperty object in the server.
    * 
    * @param fullName the full name of the remote object.
    * 
    * @since 2.1.2 (20060315)
    */
   public RemoteVectorProperty getRemoteVectorProperty(final String fullName)
   {
      return (RemoteVectorProperty)this.getRemoteObject(RemoteVectorProperty.class, fullName);
   }
   
   /**
    * Gets a remote interface for interacting with an object (subcomponent/subsystem or property) in the server.
    * 
    * @param fullName the full name of the remote object.
    * 
    * @since 2.1.2 (20060315)
    */
   public Object getRemoteObject(final Class interfaceClass, String fullName)
   {
      if( this.rpcHandlerName != null ) fullName = this.rpcHandlerName + "." + fullName;
      return this.messagingRpcInterface.createProxy(interfaceClass, fullName);
   }
   
   /**
    * Gets a remote interface for interacting with an administration handler registered in the server.
    * 
    * @param handlerName the full name of the administration handler.
    * 
    * @since 2.1.4 (20060504)
    * 
    * @see #getAdministrationHandlerNames()
    */
   public Object getRemoteAdministrationHandler(final Class interfaceClass, final String handlerName)
   {
      return this.getRemoteObject(interfaceClass, handlerName);
   }
   
   private void checkVersion(final int requiredVersion) throws RpcException
   {
      if( this.getInterfaceVersion() < requiredVersion )
      {
         throw new RpcException(RpcException.METHOD_NOT_FOUND, "Requested method is not available in remote server " +
               "(remote version: " + this.getInterfaceVersion() + ", required version: " + requiredVersion + ")!");
      }
   }
   
   
   /* ### RPC ADMINISTRATION METHODS ### */


   /**
    * Gets the version of this interface.
    * 
    * @throws RpcException if an error occurrs during remote processing of the command.  
    */
   public int getInterfaceVersion() throws RpcException
   {
      if( remoteVersion < 0 )
      {
         remoteVersion = this.serverAdministrationInterfaceProxy.getInterfaceVersion();
      }
      return remoteVersion;
   }
   
   /**
    * Gets the names of all appender components (i.e. components capable of generating logs) in the server.
    * 
    * @throws RpcException if an error occurrs during remote processing of the command.
    */
   public String[] getAppenderComponentNames() throws RpcException
   {
      return this.serverAdministrationInterfaceProxy.getAppenderComponentNames();
   }
   
   /**
    * Gets records of all available logs in the server.
    * 
    * @throws RpcException if an error occurrs during remote processing of the command.
    */
   public LogData[] getLogs() throws RpcException
   {
      return this.serverAdministrationInterfaceProxy.getLogs();
   }

   /**
    * Gets records of all available logs in the specified appender component (as previously returned by {@link #getAppenderComponentNames()}).
    * 
    * @throws RpcException if an error occurrs during remote processing of the command.
    */
   public LogData[] getLogs(final String appenderName) throws RpcException
   {
      return this.serverAdministrationInterfaceProxy.getLogs(appenderName);
   }
   
   /**
    * Gets an InputStream for reading the contents of the specified log from generated by the specified appender component.<br>
    * <br>
    * Note: the server side implementation shoule return a {@link com.teletalk.jserver.tcp.messaging.rpc.RpcInputStream} object or object with similar functionality. 
    * 
    * @throws RpcException if an error occurrs during remote processing of the command.
    */
   public InputStream getLog(final String appenderName, final String log) throws RpcException
   {
      return this.serverAdministrationInterfaceProxy.getLog(appenderName, log);
   }
   
   /**
    * Gets an InputStream for reading the contents of the specified log from generated by the specified appender component.<br>
    * <br>
    * Note: the server side implementation shoule return a {@link com.teletalk.jserver.tcp.messaging.rpc.RpcInputStream} object or object with similar functionality. 
    * 
    * @throws RpcException if an error occurrs during remote processing of the command.
    */
   public InputStream getLogTail(final String appenderName, final String log, final long tailSize) throws RpcException
   {
      this.checkVersion(4);
      return this.serverAdministrationInterfaceProxy.getLogTail(appenderName, log, tailSize);
   }
   
   /**
    * Gets administraion data (recursively) of the root component in the remote server. 
    * 
    * @throws RpcException if an error occurrs during remote processing of the command.
    */
   public RemoteSubComponentData getRemoteSubComponentData() throws RpcException
   {
      return this.serverAdministrationInterfaceProxy.getRemoteSubComponentData();
   }
   
   /**
    * Gets administraion data (recursively) of the specified component in the remote server. 
    * 
    * @throws RpcException if an error occurrs during remote processing of the command.
    */
   public RemoteSubComponentData getRemoteSubComponentData(final String componentFullName) throws RpcException
   {
      return this.serverAdministrationInterfaceProxy.getRemoteSubComponentData(componentFullName);
   }
   
   /**
    * Starts the specified component.
    * 
    * @throws RpcException if an error occurrs during remote processing of the command.
    */
   public boolean engageComponent(final String componentFullName) throws RpcException
   {
      return this.serverAdministrationInterfaceProxy.engageComponent(componentFullName);
   }
   
   /**
    * Stops the specified component.
    * 
    * @throws RpcException if an error occurrs during remote processing of the command.
    */
   public boolean shutDownComponent(final String componentFullName) throws RpcException
   {
      return this.serverAdministrationInterfaceProxy.shutDownComponent(componentFullName);
   }

   /**
    * Restarts the specified component.
    * 
    * @throws RpcException if an error occurrs during remote processing of the command.
    */
   public boolean reinitializeComponent(final String componentFullName) throws RpcException
   {
      return this.serverAdministrationInterfaceProxy.reinitializeComponent(componentFullName);
   }
   
   /**
    * Sets the value of the specified property as a string. The format of the propertyFullName parameter is a the "path" of the property, 
    * i.e. <componentName>.<componentName>.<propertyName>, for instance MyServer.MyComponent.MyProperty.  
    * 
    * @throws RpcException if an error occurrs during remote processing of the command.
    */
   public boolean setPropertyValue(final String propertyFullName, final String value) throws RpcException
   {
      return this.serverAdministrationInterfaceProxy.setPropertyValue(propertyFullName, value);
   }
   
   /**
    * Sets the value of the specified property as a array of strings. The format of the propertyFullName parameter is a the "path" of the property, 
    * i.e. <componentName>.<componentName>.<propertyName>, for instance MyServer.MyComponent.MyProperty.  
    * 
    * @throws RpcException if an error occurrs during remote processing of the command.
    */
   public boolean setPropertyValue(final String propertyFullName, final String[] value) throws RpcException
   {
      return this.serverAdministrationInterfaceProxy.setPropertyValue(propertyFullName, value);
   }
   
   /**
    * Gets the value of the specified property. The format of the propertyFullName parameter is a the "path" of the property, 
    * i.e. <componentName>.<componentName>.<propertyName>, for instance MyServer.MyComponent.MyProperty.
    * 
    * @throws RpcException if an error occurrs during remote processing of the command.
    */
   public String getPropertyValue(final String propertyFullName) throws RpcException
   {
      return this.serverAdministrationInterfaceProxy.getPropertyValue(propertyFullName);
   }
   
   /**
    * Gets the values of the specified property. The format of the propertyFullName parameter is a the "path" of the property, 
    * i.e. <componentName>.<componentName>.<propertyName>, for instance MyServer.MyComponent.MyProperty.
    * 
    * @throws RpcException if an error occurrs during remote processing of the command.
    * 
    * @since 2.0 (20041222), interface version 3. 
    */
   public String[] getPropertyValues(final String propertyFullName) throws RpcException
   {
      this.checkVersion(3);
      return this.serverAdministrationInterfaceProxy.getPropertyValues(propertyFullName);
   }
   
   /**
    * Gets the system properties (as returned by a call to <code>System.getProperties()</code>) of the remote server.
    * 
    * @throws RpcException if an error occurrs during remote processing of the command.
    * 
    * @since interface version 2.
    */
   public Properties getSystemProperties() throws RpcException
   {
      this.checkVersion(2);
      return this.serverAdministrationInterfaceProxy.getSystemProperties();
   }
   
   /**
    * Gets the maximum configured memory in bytes of the remote server. 
    * 
    * @throws RpcException if an error occurrs during remote processing of the command.
    * 
    * @since interface version 2.
    */
   public long getMaxMemory() throws RpcException
   {
      this.checkVersion(2);
      return this.serverAdministrationInterfaceProxy.getMaxMemory();
   }
   
   /**
    * Gets the currently used memory in bytes of the remote server.
    * 
    * @throws RpcException if an error occurrs during remote processing of the command.
    * 
    * @since interface version 2.
    */
   public long getUsedMemory() throws RpcException
   {
      this.checkVersion(2);
      return this.serverAdministrationInterfaceProxy.getUsedMemory();
   }
   
   /**
    * Gets the names of all registered (custom) administration handlers.
    * 
    * @since 2.1.4 (20060504), interface version 7.
    */
   public String[] getAdministrationHandlerNames() throws RpcException
   {
      this.checkVersion(7);
      return this.serverAdministrationInterfaceProxy.getAdministrationHandlerNames();
   }

   public ServerFileInfo[] listServerFiles() throws RpcException
   {
      this.checkVersion(8);
      return this.serverAdministrationInterfaceProxy.listServerFiles();
   }

   public ServerFileInfo[] listServerFiles(final String subPath) throws RpcException
   {
      this.checkVersion(8);
      return this.serverAdministrationInterfaceProxy.listServerFiles(subPath);
   }

   public ServerFile getServerFile(final String fileName) throws RpcException
   {
      this.checkVersion(8);
      return this.serverAdministrationInterfaceProxy.getServerFile(fileName);
   }
   
   public void createServerFile(final ServerFile serverFileData) throws RpcException
   {
      this.checkVersion(8);
      this.serverAdministrationInterfaceProxy.createServerFile(serverFileData);
   }
   
   public boolean deleteServerFile(String fileName) throws RpcException
   {
      this.checkVersion(8);
      return this.serverAdministrationInterfaceProxy.deleteServerFile(fileName);
   }
}
