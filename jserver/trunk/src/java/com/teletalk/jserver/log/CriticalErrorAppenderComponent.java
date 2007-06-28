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

package com.teletalk.jserver.log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.StringTokenizer;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.JServerConstants;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.net.sns.client.ServiceListenerSupport;
import com.teletalk.jserver.property.BooleanProperty;
import com.teletalk.jserver.tcp.TcpEndPointIdentifier;
import com.teletalk.jserver.tcp.TcpEndPointIdentifierProperty;
import com.teletalk.jserver.util.UrlEncodingUtils;
import com.teletalk.jserver.util.exception.ErrorCodeException;

/**
 * An AppenderComponent class that only logs LoggingEvents where the log level is FATAL (critical error). This class attempts to dispatch 
 * log messages to a remote server if the <code>remote logging enabled</code> property is set to true and at least one address has 
 * been specified for the property <code>remote logging address(es)</code>. The request will be on the format :<br>
 * <br>
 * http://address:port/CriticalErrorLogger?logDate=2001-01-01%2001:01:01&serverName=TheServerName&logOrigin=TheOriginOfTheLog&logMessage=TheMessage&logMessageID=12345
 * <br>
 * <br>
 * This component will break after one successful remote log has been achieved (response code 200). If no addresses have been specified or no successful remote logs 
 * have been performed, a local error file will be written if the flag <code>write local error files</code> is set to true.
 * <br>
 * <br>
 * The name of the keys used in the queuey string are defined as constants in this class (LOG_DATE_KEY, SERVER_NAME_KEY, 
 * LOG_ORIGIN_KEY, LOG_MESSAGE_KEY and LOG_MESSAGE_ID_KEY). The (critical error) log message id field of log messages 
 * may be specified in the logging calls defined in class {@link com.teletalk.jserver.log.LoggableObject} or using an instance of 
 * {@link com.teletalk.jserver.util.exception.ErrorCodeException} when performing the logging.<br>
 * <br>
 * This class replaces the 1.X class CriticalErrorLogger.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0
 */
public class CriticalErrorAppenderComponent extends FileAppenderComponent
{
   /** Http request key for log date used in remote logging. */
   public static final String LOG_DATE_KEY = "logDate";
   /** Http request key for server name used in remote logging. */
   public static final String SERVER_NAME_KEY = "serverName";
   /** Http request key for log origin name used in remote logging. */
   public static final String LOG_ORIGIN_KEY = "logOrigin";
   /** Http request key for log message used in remote logging. */
   public static final String LOG_MESSAGE_KEY = "logMessage";
   /** Http request key for log message id used in remote logging. */
   public static final String LOG_MESSAGE_ID_KEY = "logMessageID";
   
   private static final String MESSAGE_LAYOUT_PATTERN = "%m %S";
   
   private final String newLine;
   
   /**   A object for formatting dates used by this Logger. */
   protected SimpleDateFormat dateFormat;
   
   
   private final BooleanProperty remoteLoggingEnabled;
   
   private final TcpEndPointIdentifierProperty remoteLoggingAddresses;
   
   private final BooleanProperty writeLocalErrorFiles;
      
   private final ServiceListenerSupport serviceListenerSupport;
   
   
   /**
    * Creates a new CriticalErrorAppenderComponent.
    * 
    * @param parent the parent of this CriticalErrorAppenderComponent.
    * @param name the name of this CriticalErrorAppenderComponent.
    * @param logFileNamePrefix the filename prefix for logfiles.
    * @param logFileNameSuffix the filename suffix for logfiles.
    * @param basePath the base path of log file names.
    * 
    * @see LogManager
    */
   public CriticalErrorAppenderComponent(SubComponent parent, String name, String logFileNamePrefix, String logFileNameSuffix, String basePath)
   {
      super(parent, name, logFileNamePrefix, logFileNameSuffix, basePath);
      super.setLogLevelThreshold(Level.FATAL);

      newLine = System.getProperty("line.separator");
      
      remoteLoggingEnabled = new BooleanProperty(this, "remoteLoggingEnabled", true, BooleanProperty.MODIFIABLE_NO_RESTART);
      remoteLoggingEnabled.setDescription("Flag indicating if remote logging over HTTP is enabled.");
      remoteLoggingAddresses = new TcpEndPointIdentifierProperty(this, "remoteLoggingAddresses", "", TcpEndPointIdentifierProperty.MODIFIABLE_NO_RESTART);
      remoteLoggingAddresses.setDescription("The addresses (ip:port) to be used for logging over HTTP. Note that only one of the addresses will receive a log message.");
      writeLocalErrorFiles = new BooleanProperty(this, "writeLocalErrorFiles", false, BooleanProperty.MODIFIABLE_NO_RESTART);
      writeLocalErrorFiles.setDescription("Flag indicating if local error files should be generated as a backup to remote logging (if remote logging is successful, " +
            "no file will be generated event if this property is true).");
      
      super.addProperty(remoteLoggingEnabled);
      super.addProperty(remoteLoggingAddresses);
      super.addProperty(writeLocalErrorFiles);
      
      // Remove obsolete super class properties 
      super.removeProperty(super.flushInterval);
      super.removeProperty(super.periodicity);
      super.removeProperty(super.lastLogChange);
      super.removeProperty(super.nextLogChange);
      super.removeProperty(super.currentLog);
      super.removeProperty(super.defaultLayoutPattern);
      super.removeProperty(super.logLevelThreshold); // We don't want it to be possible to change this property!
      
      dateFormat = (SimpleDateFormat)DateFormat.getDateTimeInstance();
      dateFormat.applyPattern("yyyy-MM-dd HH:mm:ss");
      
      super.setLayout(new ExtendedPatternLayout(MESSAGE_LAYOUT_PATTERN));
      
      this.serviceListenerSupport = new ServiceListenerSupport(this, "remoteLoggingServiceNames", "The names of the remote critical error log manager services to connect to. " +
            "Addresses for the service names will be fetched from an SNS.");
   }
   
   /**
    * Creates a new CriticalErrorAppenderComponent.
    * 
    * @param parent the parent of this CriticalErrorAppenderComponent.
    * @param name the name of this CriticalErrorAppenderComponent.
    * @param basePath the base filename (path) for logging.
    * 
    * @see LogManager
    */
   public CriticalErrorAppenderComponent(SubComponent parent, String name, String basePath)
   {
      this(parent, name, null, ".err", basePath);
   }
   
   /**
    * Creates a new CriticalErrorAppenderComponent.
    * 
    * @param parent the parent of this CriticalErrorAppenderComponent.
    * @param name the name of this CriticalErrorAppenderComponent.
    * 
    * @see LogManager
    */
   public CriticalErrorAppenderComponent(SubComponent parent, String name)
   {
      this(parent, name, null, ".err", "");
   }
   
   /**
    * Creates a new CriticalErrorAppenderComponent.
    * 
    * @param parent the parent of this CriticalErrorAppenderComponent.
    * 
    * @see LogManager
    */
   public CriticalErrorAppenderComponent(SubComponent parent)
   {
      this(parent, "CriticalErrorAppender", "");
   }
   
   /**
    * Enables this CriticalErrorAppenderComponent.
    */
   public synchronized void doInitialize()
   {
      if(!propertiesInitialized) initProperties();
      
      // Attempt to get old property "remote logging enabled"
      super.initFromConfiguredProperty(this.remoteLoggingEnabled, "remote logging enabled", false, true);
      // Attempt to get old property "remote logging address(es)"
      super.initFromConfiguredProperty(this.remoteLoggingAddresses, "remote logging address(es)", false, true);
      // Attempt to get old property "write local error files"
      super.initFromConfiguredProperty(this.writeLocalErrorFiles, "write local error files", false, true);
      
      // Attempt to get old PeriodicityAppenderComponent properties:
      // Attempt to get old property "log prefix"
      this.initFromConfiguredProperty(this.logPrefix, "log prefix", false, true);
      // Attempt to get old property "log prefix"
      this.initFromConfiguredProperty(this.logSuffix, "log suffix", false, true);
      
      new File(super.logFilePath.stringValue()).mkdirs();
      
      super.logFileInitialized = true;
   }
   
   /**
    * Disables this CriticalErrorAppenderComponent.
    */
   public synchronized void doShutDown()
   {
   }
   
   /**
    * Called to record a LoggingEvent to this AppenderComponent.
    * 
    * @param event the LoggingEvent to record.
    * 
    * @throws Exception if an error occurred while attempting to record the event.
    */
   public void append(final LoggingEvent event) throws Exception
   {
      if(remoteLoggingEnabled.booleanValue())
      {
         if(!sendLogToRemoteServer(event))  // If no successful - write to local log file
         {
            if( writeLocalErrorFiles.booleanValue() ) writeLogToFile(event);
         }
      }
      else if( writeLocalErrorFiles.booleanValue() )
      {
         writeLogToFile(event);
      }
   }
   
   /**
    * Gets the log message id from the specified logging event.
    */
   private String getLogMessageId(final LoggingEvent event)
   {
      Object o = event.getMDC(JServerConstants.LOG_MESSAGE_ID_KEY);
      if( o != null )
      {
         return o.toString();
      }
      else if( event.getThrowableInformation() != null )
      {
         Throwable throwable = event.getThrowableInformation().getThrowable();
         if( throwable instanceof ErrorCodeException )
         {
            return String.valueOf( ((ErrorCodeException)throwable).getErrorCode() );
         }
      }
      return null;
   }
   
   /**
    * Sets a log to a remote server (CELMa).
    */
   private boolean sendLogToRemoteServer(final LoggingEvent event) throws java.io.IOException
   {
      boolean logMessageDispatched = false;
      
      ArrayList allRemoteAddresses = new ArrayList();
      allRemoteAddresses.addAll(this.remoteLoggingAddresses.getAddressList());
      allRemoteAddresses.addAll(this.serviceListenerSupport.getRemoteServiceAddresses());

      if(allRemoteAddresses.size() > 0)
      {
         StringBuffer queryString = new StringBuffer();
         queryString.append(LOG_DATE_KEY);
         queryString.append("=");
         queryString.append(UrlEncodingUtils.encode(dateFormat.format( new Date(event.timeStamp) )));
         queryString.append("&");
         if( JServer.getJServer() != null )
         {         
            queryString.append(SERVER_NAME_KEY);
            queryString.append("=");
            queryString.append(UrlEncodingUtils.encode(JServer.getJServer().getName()));
            queryString.append("&");
         }
         queryString.append(LOG_ORIGIN_KEY);
         queryString.append("=");
         queryString.append(UrlEncodingUtils.encode( event.getThreadName() ));
         queryString.append("&");
         queryString.append(LOG_MESSAGE_KEY);
         queryString.append("=");
         queryString.append(UrlEncodingUtils.encode(super.getLayout().format(event)));
         
         String logMessageId = getLogMessageId(event);
         
         if( logMessageId != null )
         {
            queryString.append("&");
            queryString.append(LOG_MESSAGE_ID_KEY);
            queryString.append("=");
            queryString.append( logMessageId );
         }
         
         TcpEndPointIdentifier address;
         
         for(int i=0;(!logMessageDispatched && (i<allRemoteAddresses.size())); i++)
         {
            address = (TcpEndPointIdentifier)allRemoteAddresses.get(i);

            try
            {
               StringBuffer urlString = new StringBuffer();
               urlString.append("http://");
               urlString.append(address.getAddressAsString());
               urlString.append("/CriticalErrorLogger?");
               urlString.append(queryString.toString());

               URL url = new URL(urlString.toString());
            
               HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                           
               if(conn.getResponseCode() == 200)
               {
                  logMessageDispatched = true;
               }
               else 
               {
                  logError("Failed to dispatch critical error message to " + address + "! Response was: " + conn.getResponseCode() + " - " + conn.getResponseMessage() + ".");
               }
               
               try{
               conn.disconnect();
               }catch(Exception e){}
            }
            catch(Exception e)
            {
               logError("Error while dispatching critical error message to " + address + "!", e);
            }
         }
         
         return logMessageDispatched;
      }
      else
      {
         return false;
      }
   }
   
   /**
    * Generates a log file name.
    */
   private String generateLogFileName(Date logDate, String extra)
   {
      dateFormat.applyPattern("yyyyMMdd_HH-mm-ss");
      return super.logFilePath.stringValue() + super.getLogPrefix() + dateFormat.format(logDate) + extra + super.getLogSuffix();
   }

   /**
    * Writes to local file if remote logging fails.
    */   
   private void writeLogToFile(final LoggingEvent event) throws java.io.IOException
   {
      String logFileName;
      PrintWriter writer;
      
      logFileName = this.generateLogFileName(new Date(event.timeStamp), "");
      
      dateFormat.applyPattern("yyyy-MM-dd HH:mm:ss");
      
      StringBuffer result = new StringBuffer();
      String logMessageId = getLogMessageId(event);
      
      if( logMessageId != null )
      {
         result.append("Error type id:\t");
         result.append(logMessageId);
         result.append(newLine);
      }
      result.append("Level:\t");
      result.append(event.getLevel().toString());
      result.append(newLine);
      result.append("Time:\t");

      result.append(dateFormat.format(new Date(event.timeStamp)));

      result.append(newLine);
      result.append("Origin:\t");

      result.append(event.getThreadName());

      result.append(newLine);
      result.append("Message:\t");
       
      String msg = super.getLayout().format(event);
      if(msg != null)
      {
         StringTokenizer t = new StringTokenizer(msg);
      
         result.append(t.nextToken());
      
         while(t.hasMoreTokens())
         {
            result.append(" ");
            result.append(t.nextToken());
         }
      }
      else
      {
         result.append("<No message>");
      }

      try
      {
         if(!(new File(logFileName)).exists())
         {
            writer = new PrintWriter(new BufferedWriter(new FileWriter(logFileName, false)));

            writer.println(result.toString());
                  
            writer.flush();
            writer.close();
            writer = null;
         }
         else
         {
            short random = (short)(Math.random() * Short.MAX_VALUE);
            //dateFormat.applyPattern("yyyyMMdd_HH-mm-ss");
            //logFileName = this.logFilePath.stringValue() + dateFormat.format(logMsg.time) + "-" + random + ".err";;
            logFileName = this.generateLogFileName(new Date(event.timeStamp), "-" + random);
            
            writer = new PrintWriter(new BufferedWriter(new FileWriter(logFileName, false)));
            
            writer.println(result.toString());
      
            writer.flush();
            writer.close();
            writer = null;
         }
      }
      catch(IOException e)
      {
         short random = (short)(Math.random() * Short.MAX_VALUE);
         //dateFormat.applyPattern("yyyyMMdd_HH-mm-ss");
         //logFileName = this.logFilePath.stringValue() + dateFormat.format(logMsg.time) + "-" + random + ".err";;
         logFileName = this.generateLogFileName(new Date(event.timeStamp), "-" + random);
            
         writer = new PrintWriter(new BufferedWriter(new FileWriter(logFileName, false)));
            
         writer.println(result.toString());
      
         writer.flush();
         writer.close();
         writer = null;
      }
   }
   
   /**
    * This implementation only returns an empty array.
    * 
    * @return String array containing filenames.
    */
   public LogData[] getLogs()
   {
      return new LogData[0];
   }
}
