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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.FileAppender;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.log.AppenderComponent;
import com.teletalk.jserver.log.LogData;
import com.teletalk.jserver.log.LogManager;
import com.teletalk.jserver.net.admin.AdministrationManager;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.rmi.adapter.SubComponentRmiAdapter;
import com.teletalk.jserver.rmi.remote.RemoteSubComponentData;
import com.teletalk.jserver.tcp.messaging.Message;
import com.teletalk.jserver.tcp.messaging.MessageReceiver;
import com.teletalk.jserver.tcp.messaging.MessagingManager;
import com.teletalk.jserver.tcp.messaging.rpc.RpcException;
import com.teletalk.jserver.tcp.messaging.rpc.RpcHandler;
import com.teletalk.jserver.tcp.messaging.rpc.RpcInputStreamResponse;
import com.teletalk.jserver.util.Log4JUtils;
import com.teletalk.jserver.util.StringUtils;

/**
 * Default implementation of RPC based messaging remote administration of JServer.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0.1 (20040924)
 */
public class ServerAdministrationHandler implements MessageReceiver, ServerAdministrationInterface
{
   private static final class ServerAdministrationRpcHandler extends RpcHandler
   {
      public ServerAdministrationRpcHandler(MessagingManager messagingManager)
      {
         super(messagingManager);
      }

      public Object getHandler(final String name)
      {
         final Object administrationHandler = AdministrationManager.getAdministrationManager().getAdministrationHandler(name);
         
         if( administrationHandler != null ) return administrationHandler;
         else
         {
            JServer jServer = JServer.getJServer();
            if( jServer != null )
            {
               SubComponent subComponent = jServer.findSubComponent(name);
               if( subComponent != null ) return subComponent;
               else
               {
                  Property property = jServer.findProperty(name);
                  if( property != null ) return property;
               }
            }
            return super.getHandler(name);
         }
      }
   }
   
   
   /** Reference to the associated {@link MessagingManager}. */
   protected final MessagingManager messagingManager;   
   
   /** The RpcHandler for handling of RPC messages. */
   protected final RpcHandler rpcHandler;
   
   /**
    * Creates a new ServerAdministrationHandler.
    */
   public ServerAdministrationHandler(final MessagingManager messagingManager)
   {
      this.messagingManager = messagingManager;
      
      this.rpcHandler = new ServerAdministrationRpcHandler(messagingManager);
      this.rpcHandler.setDefaultHandler(this);
   }
   
   /**
    * Called when a server administration message is received in the associated {@link MessagingManager}. 
    * The implementation of this method uses an {@link RpcHandler} to attempt to handle the message as an RPC message.
    * The RpcHandler uses this object as it's default handler.
    * 
    * @param message the received {@link Message}.
    */
   public void messageReceived(final Message message)
   {
      RpcHandler.defaultHandleRpcMessage(message, this.rpcHandler, this.messagingManager, this.messagingManager);
   }
   
   private String getInvokerAddess()
   {
      Message msg = MessagingManager.getCurrentMessage();
      if( msg != null )
      {
         String serverName = msg.getDestination().getServerName();
         if( serverName != null ) return serverName + "(" + msg.getDestinationAddress() + ")";
         else return String.valueOf(msg.getDestinationAddress());
      }
      else return "<unknown>";
   }
   
   /**
    * Method to get the log origin used when logging.
    */
   protected String getLogOrigin()
   {
      return messagingManager.getNormalizedFullName() + ".ServerAdministrationHandler";
   }
   
   
   /* ### RPC METHODS ### */
   
   
   /**
    * Gets the implemented version of the administration interface.
    * 
    * @throws RpcException if an error occurrs during processing of the command.  
    */
   public int getInterfaceVersion() throws RpcException
   {
      if( messagingManager.isDebugMode() ) messagingManager.logDebug(this.getLogOrigin(), "Method getInterfaceVersion invoked from " + getInvokerAddess() + ".");
      return INTERFACE_VERSION;
   }
   
   /**
    * Gets the names of all appender components (i.e. components capable of generating logs) in the server.
    */
   public String[] getAppenderComponentNames()
   {
      if( messagingManager.isDebugMode() ) messagingManager.logDebug(this.getLogOrigin(), "Method getAppenderComponentNames invoked from " + getInvokerAddess() + ".");
      JServer jServer = JServer.getJServer();
      if( jServer != null )
      {
	      LogManager logManager = jServer.getLogManager();
	      Enumeration appenders = logManager.getAllAppenders();
	      Object appender;
	      ArrayList logsList = new ArrayList();
	      
	      while(appenders.hasMoreElements())
	      {
	         appender = appenders.nextElement();
	         if( (appender != null) && (appender instanceof AppenderComponent) )
	         {
	            logsList.add( ((AppenderComponent)appender).getName() );
	         }
	      }
         
         // Get any Log4J FileAppenders
         logsList.addAll(Log4JUtils.getLog4JFileAppenders());

	      return (String[])logsList.toArray(new String[0]);
      }
      else return new String[0];
   }
   
   /**
    * Gets records of all available logs in the server.
    */
   public LogData[] getLogs()
   {
      if( messagingManager.isDebugMode() ) messagingManager.logDebug(this.getLogOrigin(), "Method getLogs invoked from " + getInvokerAddess() + ".");
      JServer jServer = JServer.getJServer();
      if( jServer != null )
      {
	      LogManager logManager = jServer.getLogManager();
	      Enumeration appenders = logManager.getAllAppenders();
	      Object appender;
	      ArrayList logDataList = new ArrayList();
	      
	      while(appenders.hasMoreElements())
	      {
	         appender = appenders.nextElement();
	         if( (appender != null) && (appender instanceof AppenderComponent) )
	         {
	            LogData[] tempLogData = ((AppenderComponent)appender).getLogs();
	            if( tempLogData != null ) logDataList.addAll(Arrays.asList( tempLogData ));
	         }
	      }

         // Get log data for any Log4J FileAppenders         
         ArrayList log4jappenders = Log4JUtils.getLog4JFileAppenders();
         FileAppender fileAppender;
         for (int i=0; i<log4jappenders.size(); i++)
         {
            fileAppender = (FileAppender)log4jappenders.get(i);
            if( fileAppender != null )
            {
               logDataList.addAll(Log4JUtils.getLog4JFileAppenderLogs(fileAppender));
            }
         }
	      
	      return (LogData[])logDataList.toArray(new LogData[0]);
      }
      else return new LogData[0];
   }
   
   /**
    * Gets records of all available logs in the specified appender component (as previously returned by {@link #getAppenderComponentNames()}).
    */
   public LogData[] getLogs(final String appenderName)
   {
      if( messagingManager.isDebugMode() ) messagingManager.logDebug(this.getLogOrigin(), "Method getLogs('" + appenderName +"') invoked from " + getInvokerAddess() + ".");
      JServer jServer = JServer.getJServer();
      if( jServer != null )
      {
	      LogManager logManager = jServer.getLogManager();
	      Object appender = logManager.getAppender(appenderName);
	      if( (appender != null) && (appender instanceof AppenderComponent) ) return ((AppenderComponent)appender).getLogs();
         
         // Else - attempt to get log data for any Log4J FileAppenders
         FileAppender fileAppender = Log4JUtils.findLog4JFileAppender(appenderName);
         if( fileAppender != null )
         {
            return (LogData[])Log4JUtils.getLog4JFileAppenderLogs((FileAppender)fileAppender).toArray(new LogData[0]);
         }
      }
      return new LogData[0];
   }
   
   /**
    * Gets an InputStream for reading the contents of the specified log from generated by the specified appender component.<br>
    * <br>
    * This implementation returns a {@link RpcInputStreamResponse}. 
    */
   public InputStream getLog(final String appenderName, final String log)
   {
      if( messagingManager.isDebugMode() ) messagingManager.logDebug(this.getLogOrigin(), "Method getLog('" + appenderName +"', '" + log + "') invoked from " + getInvokerAddess() + ".");
      
      return getLogInternal(appenderName, log, -1);
   }
   
   /**
    * Gets an InputStream for reading the contents of the specified log from generated by the specified appender component.<br>
    * <br>
    * Note: the server side implementation shoule return a {@link com.teletalk.jserver.tcp.messaging.rpc.RpcInputStreamResponse} object or object with similar functionality. 
    */
   public InputStream getLogTail(final String appenderName, final String log, final long tailSize)
   {
      if( messagingManager.isDebugMode() ) messagingManager.logDebug(this.getLogOrigin(), "Method getLogTail('" + appenderName +"', '" + log + ", " + tailSize + "') invoked from " + getInvokerAddess() + ".");
            
      return getLogInternal(appenderName, log, tailSize);
   }
   
   /**
    */
   private InputStream getLogInternal(final String appenderName, final String log, final long tailSize)
   {
      long skipSize = 0;
      long logSize = 0;
      
      JServer jServer = JServer.getJServer();
      if( (jServer != null) && (log != null) )
      {
	      LogManager logManager = jServer.getLogManager();
	      Object appender = logManager.getAppender(appenderName);
	      if( appender instanceof AppenderComponent ) 
	      {
	         LogData logData = null;
	         LogData[] logDataArray = ((AppenderComponent)appender).getLogs();
	         
	         if( logDataArray != null )
	         {
		         for(int i=0; i<logDataArray.length; i++)
		         {
		            if( log.equals( logDataArray[i].getLogName() ) )
		            {
		               logData = logDataArray[i];
		            }
		         }
		         
		         if( logData != null )
               {
                  InputStream logStream = ((AppenderComponent)appender).getLogAsStream(log);
                  logSize = logData.getLogSize();
                  
                  if( (tailSize > 0) && (tailSize < logSize) )
                  {
                     skipSize = logSize - tailSize;
                     logSize = tailSize;
                     try{
                     logStream.skip(skipSize);
                     }catch(IOException ioe){}
                  }
                  return new RpcInputStreamResponse(logStream, logSize);
               }
	         }
	      }
         
         // Else - attempt to get log for any Log4J FileAppenders
         FileAppender fileAppender = Log4JUtils.findLog4JFileAppender(appenderName);

         if( fileAppender != null )
         {
            String basePath = Log4JUtils.getLog4JFileAppenderBasePath((FileAppender)fileAppender);
            File file = new File(basePath, log);
            if( file.exists() )
            {
               try
               {
                  FileInputStream fileIn = new FileInputStream(file);
                  logSize = file.length();
                  if( (tailSize > 0) && (tailSize < logSize) )
                  {
                     skipSize = logSize - tailSize;
                     logSize = tailSize;
                     try{
                     fileIn.skip(skipSize);
                     }catch(IOException ioe){}
                  }
                  return new RpcInputStreamResponse(fileIn, logSize);
               }
               catch(FileNotFoundException fnfe){}
            }
         }
      }
      return null;
   }
   
   /**
    * Gets administraion data (recursively) of the root component in the server. 
    */
   public RemoteSubComponentData getRemoteSubComponentData()
   {
      if( messagingManager.isDebugMode() ) messagingManager.logDebug(this.getLogOrigin(), "Method getRemoteSubComponentData() invoked from " + getInvokerAddess() + ".");
      return SubComponentRmiAdapter.getRemoteSubComponentData(JServer.getJServer());
   }

   /**
    * Gets administraion data (recursively) of the specified component in the server. 
    */
   public RemoteSubComponentData getRemoteSubComponentData(final String componentFullName)
   {
      if( messagingManager.isDebugMode() ) messagingManager.logDebug(this.getLogOrigin(), "Method getRemoteSubComponentData('" + componentFullName+ "') invoked from " + getInvokerAddess() + ".");
      JServer jServer = JServer.getJServer();
      if( jServer != null )
      {
         SubComponent subComponent = jServer.findSubComponent(componentFullName);
         if( subComponent != null ) return SubComponentRmiAdapter.getRemoteSubComponentData(subComponent);
      }
      return null;
   }
   
   /**
    * Starts the specified component.
    */
   public boolean engageComponent(final String componentFullName)
   {
      if( messagingManager.isDebugMode() ) messagingManager.logDebug(this.getLogOrigin(), "Method engageComponent('" + componentFullName+ "') invoked from " + getInvokerAddess() + ".");
      JServer jServer = JServer.getJServer();
      if( jServer != null )
      {
         SubComponent subComponent = jServer.findSubComponent(componentFullName);
         if( subComponent != null ) return subComponent.engage();
      }
      return false;
   }
   
   /**
    * Stops the specified component.
    */
   public boolean shutDownComponent(final String componentFullName)
   {
      if( messagingManager.isDebugMode() ) messagingManager.logDebug(this.getLogOrigin(), "Method shutDownComponent('" + componentFullName+ "') invoked from " + getInvokerAddess() + ".");
      JServer jServer = JServer.getJServer();
      if( jServer != null )
      {
         SubComponent subComponent = jServer.findSubComponent(componentFullName);
         if( subComponent != null ) return subComponent.shutDown();
      }
      return false;
   }
   
   /**
    * Restarts the specified component.
    */
   public boolean reinitializeComponent(final String componentFullName)
   {
      if( messagingManager.isDebugMode() ) messagingManager.logDebug(this.getLogOrigin(), "Method reinitializeComponent('" + componentFullName+ "') invoked from " + getInvokerAddess() + ".");
      JServer jServer = JServer.getJServer();
      if( jServer != null )
      {
         SubComponent subComponent = jServer.findSubComponent(componentFullName);
         if( subComponent != null ) return subComponent.reinitialize();
      }
      return false;
   }
   
   /**
    * Sets the value of the specified property as a string. The format of the propertyFullName parameter is a the "path" of the property, 
    * i.e. <componentName>.<componentName>.<propertyName>, for instance MyServer.MyComponent.MyProperty.  
    */
   public boolean setPropertyValue(final String propertyFullName, final String value)
   {
      if( messagingManager.isDebugMode() ) messagingManager.logDebug(this.getLogOrigin(), "Method setPropertyValue('" + propertyFullName +"', '" + value + "') invoked from " + getInvokerAddess() + ".");
      JServer jServer = JServer.getJServer();
      if( jServer != null )
      {
         Property property = jServer.findProperty(propertyFullName);
         if( property != null ) return property.setValueAsString(value);
      }
      return false;
   }
   
   /**
    * Sets the value of the specified property as a array of strings. The format of the propertyFullName parameter is a the "path" of the property, 
    * i.e. <componentName>.<componentName>.<propertyName>, for instance MyServer.MyComponent.MyProperty.  
    */
   public boolean setPropertyValue(final String propertyFullName, final String[] value)
   {
      if( messagingManager.isDebugMode() ) messagingManager.logDebug(this.getLogOrigin(), "Method setPropertyValue('" + propertyFullName +"', '" + StringUtils.toString(value) + "') invoked from " + getInvokerAddess() + ".");
      JServer jServer = JServer.getJServer();
      if( jServer != null )
      {
         Property property = jServer.findProperty(propertyFullName);
         if( property != null ) return property.setValuesAsStrings(value);
      }
      return false;
   }
   
   /**
    * Gets the value of the specified property. The format of the propertyFullName parameter is a the "path" of the property, 
    * i.e. <componentName>.<componentName>.<propertyName>, for instance MyServer.MyComponent.MyProperty.
    */
   public String getPropertyValue(final String propertyFullName)
   {
      if( messagingManager.isDebugMode() ) messagingManager.logDebug(this.getLogOrigin(), "Method getPropertyValue() invoked from " + getInvokerAddess() + ".");
      JServer jServer = JServer.getJServer();
      if( jServer != null )
      {
         Property property = jServer.findProperty(propertyFullName);
         if( property != null ) return property.getValueAsString();
      }
      return null;
   }
   
   /**
    * Gets the values of the specified property. The format of the propertyFullName parameter is a the "path" of the property, 
    * i.e. <componentName>.<componentName>.<propertyName>, for instance MyServer.MyComponent.MyProperty.
    * 
    * @since 2.0 (20041222), interface version 3. 
    */
   public String[] getPropertyValues(final String propertyFullName)
   {
      if( messagingManager.isDebugMode() ) messagingManager.logDebug(this.getLogOrigin(), "Method getPropertyValues() invoked from " + getInvokerAddess() + ".");
      JServer jServer = JServer.getJServer();
      if( jServer != null )
      {
         Property property = jServer.findProperty(propertyFullName);
         if( property != null ) return property.getValuesAsStrings();
      }
      return null;
   }
   
   /**
    * Gets the system properties (as returned by a call to <code>System.getProperties()</code>) of the server.
    * 
    * @throws RpcException if an error occurrs during processing of the command.
    * 
    * @since interface version 2.
    */
   public Properties getSystemProperties() throws RpcException
   {
      if( messagingManager.isDebugMode() ) messagingManager.logDebug(this.getLogOrigin(), "Method getSystemProperties() invoked from " + getInvokerAddess() + ".");
      return System.getProperties();
   }
   
   /**
    * Gets the maximum configured memory in bytes of the server. 
    * 
    * @throws RpcException if an error occurrs during processing of the command.
    * 
    * @since interface version 2.
    */
   public long getMaxMemory() throws RpcException
   {
      if( messagingManager.isDebugMode() ) messagingManager.logDebug(this.getLogOrigin(), "Method getMaxMemory() invoked from " + getInvokerAddess() + ".");
      return Runtime.getRuntime().maxMemory();
   }
   
   /**
    * Gets the currently used memory in bytes of the server.
    * 
    * @throws RpcException if an error occurrs during remote processing of the command.
    * 
    * @since interface version 2.
    */
   public long getUsedMemory() throws RpcException
   {
      if( messagingManager.isDebugMode() ) messagingManager.logDebug(this.getLogOrigin(), "Method getUsedMemory() invoked from " + getInvokerAddess() + ".");
      return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();
   }
   
   /**
    * Gets the names of all registered (custom) administration handlers.
    * 
    * @since 2.1.4 (20060504), interface version 7.
    */
   public String[] getAdministrationHandlerNames() throws RpcException
   {
      if( messagingManager.isDebugMode() ) messagingManager.logDebug(this.getLogOrigin(), "Method getAdministrationHandlerNames() invoked from " + getInvokerAddess() + ".");
      return AdministrationManager.getAdministrationManager().getAdministrationHandlerNames();
   }
   
   public ServerFileInfo[] listServerFiles() throws RpcException
   {
      return listServerFiles(null);
   }
   
   public ServerFileInfo[] listServerFiles(final String subPath) throws RpcException
   {
      final File serverDir; 
      if( subPath != null ) serverDir= new File(new File(".").getAbsoluteFile(), subPath);
      else serverDir = new File(".").getAbsoluteFile();
            
      File[] files = serverDir.listFiles();
      ServerFileInfo[] serverFileInfo = new ServerFileInfo[files.length];
      
      for (int i = 0; i < serverFileInfo.length; i++)
      {
         serverFileInfo[i] = new ServerFileInfo(files[i].getName(), files[i].isDirectory(), files[i].lastModified());
      }
      
      return serverFileInfo;
   }
   
   public ServerFile getServerFile(final String fileName) throws RpcException
   {
      final File serverFile = new File(new File(".").getAbsoluteFile(), fileName);
      
      try
      {
         final byte[] fileBytes = new byte[(int)serverFile.length()];
         DataInputStream dataInputStream = new DataInputStream(new BufferedInputStream(new FileInputStream(serverFile)));
         dataInputStream.readFully(fileBytes);
         dataInputStream.close();
         
         return new ServerFile(new ServerFileInfo(serverFile.getName(), false, serverFile.lastModified()), fileBytes);
      }
      catch (Exception e) 
      {
         throw new RpcException(RpcException.MISC_INTERNAL_ERROR, "Error reading file " + serverFile + " when executing getServerFile!", e);
      }
   }
   
   public void createServerFile(final ServerFile serverFileData) throws RpcException
   {
      final ServerFileInfo fileInfo = serverFileData.getServerFileInfo();
      final File serverFile = new File(new File(".").getAbsoluteFile(), fileInfo.getFileName());
            
      try
      {
         if( fileInfo.isDirectory() )
         {
            if( !serverFile.mkdirs() )
            {
               throw new RpcException(RpcException.MISC_INTERNAL_ERROR, "Error creating directory " + serverFile + " when executing getServerFile!");
            }
         }
         else
         {
            BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(serverFile));
            fileOutputStream.write(serverFileData.getFileData());
            fileOutputStream.close();
            if( fileInfo.getLastModified() > -1 ) serverFile.setLastModified(fileInfo.getLastModified());
         }
      }
      catch (Exception e) 
      {
         throw new RpcException(RpcException.MISC_INTERNAL_ERROR, "Error writing file " + serverFile + " when executing getServerFile!", e);
      }
   }
   
   public boolean deleteServerFile(final String fileName) throws RpcException
   {
      final File serverFile = new File(new File(".").getAbsoluteFile(), fileName);
      return serverFile.delete();
   }
}
