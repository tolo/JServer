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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.log4j.spi.LoggingEvent;

import com.teletalk.jserver.JServerConstants;

/**
 * Base class for all classes requiring logging convinience methods.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0 
 */
public abstract class LoggableObject
{
   protected final String fqcn;
   
   private boolean generateLocationInformation = false;
   
   private ComponentLogger defaultComponentLogger = null;
   
   /**
    * Creates a new LoggableObject.
    */
   protected LoggableObject()
   {
      this.fqcn = this.getClass().getName();
   }
   
   /**
    * Creates a new LoggableObject.
    * 
    * @param fqcn fully qualified class name of the class. 
    */
   protected LoggableObject(final String fqcn)
   {
      this.fqcn = fqcn;
   }
  
   /**
    * Gets the value of the flag indicating if location information is to be generated.
    */
   public boolean isGenerateLocationInformation()
   {
      return this.generateLocationInformation;
   }
   
   /**
    * Sets the value of the flag indicating if location information is to be generated.
    */
   public void setGenerateLocationInformation(boolean generateLocationInformation)
   {
      this.generateLocationInformation = generateLocationInformation;
   }
   
   /**
    * Gets the logger that will be used by this object to dispatch logging events. Subclasses may override this method to provide 
    * an alternate way of getting the logger.
    */
   protected ComponentLogger getLogger()
   {
      synchronized(this)
      {
         if( this.defaultComponentLogger == null )
         {
            this.defaultComponentLogger = ComponentLogger.getTopSystemLogger();
         }
         
         return this.defaultComponentLogger;
      }
   }
   
   /**
    * Convenience method to check if log events with trace level can be logged.
    * 
    * @return <code>true</code> if log events with trace level can be logged, otherwise <code>false</code>.
    * 
    * @since 2.1.4 (20060512)
    * 
    * @see #isLogLevelEnabled(Level)
    */
   public boolean isTraceEnabled()
   {
      return this.getLogger().isTraceEnabled();
   }
   
   /**
    * Convenience method to check if log events with debug level can be logged.
    * 
    * @return <code>true</code> if log events with debug level can be logged, otherwise <code>false</code>.
    * 
    * @since 2.1.4 (20060512)
    * 
    *  @see #isLogLevelEnabled(Level)
    */
   public boolean isDebugEnabled()
   {
      return this.getLogger().isDebugEnabled();
   }

   /**
    * Convenience method to check if log events with info level can be logged.
    * 
    * @return <code>true</code> if log events with info level can be logged, otherwise <code>false</code>.
    * 
    * @since 2.1.4 (20060512)
    * 
    * @see #isLogLevelEnabled(Level)
    */
   public boolean isInfoEnabled()
   {
      return this.getLogger().isInfoEnabled();
   }
   
   /**
    * Checks if log events with the specified level can be logged.
    *  
    * @return <code>true</code> if log events with the specified level can be logged, otherwise <code>false</code>.
    * 
    * @since 2.1.4 (20060512)
    */
   public boolean isLogLevelEnabled(final Level logLevel)
   {
      return this.getLogger().isEnabledFor(logLevel);
   }
   
   /**
    * Gets the current log level of the associated logger.
    * 
    * @return the log level.
    * 
    * @since 2.1.4 (20060512)
    */
   public Level getLevel()
   {
      return this.getLogger().getLevel();
   }
   
   
   /* ##### LOGGING METHODS BEGINS #####*/
   

   /**
    * Logs a message with the specified level.
    * 
    * @param level the level of the logging event.
    * @param msg the message to log.
    * 
    * @since 2.0
    */
   public void log(Level level, String msg)
   {
      this.log(fqcn, level, null, msg, null);
   }
   
   /**
    * Logs a message with the specified level.
    * 
    * @param level the level of the logging event.
    * @param msg the message to log.
    * @param logMessageId the id associated with the log message. This id will be stored in the MDC as an java.lang.Integer object 
    * under the key {@link JServerConstants#LOG_MESSAGE_ID_KEY}. The range 4095-65535 is reserved for JServer.
    * 
    * @since 2.0
    */
   public void log(Level level, String msg, int logMessageId)
   {
      this.log(fqcn, level, null, msg, null, logMessageId);
   }
   
   /**
    * Logs a message with the specified level.
    * 
    * @param level the level of the logging event.
    * @param origin the origin of the logging event. The thread name will be set to this parameter when creating the logging event.
    * @param msg the message to log.
    * 
    * @since 2.0
    */
   public void log(Level level, String origin, String msg)
   {
      this.log(fqcn, level, origin, msg, null);
   }
   
   /**
    * Logs a message with the specified level.
    * 
    * @param level the level of the logging event.
    * @param origin the origin of the logging event. The thread name will be set to this parameter when creating the logging event.
    * @param msg the message to log.
    * @param logMessageId the id associated with the log message. This id will be stored in the MDC as an java.lang.Integer object 
    * under the key {@link JServerConstants#LOG_MESSAGE_ID_KEY}. The range 4095-65535 is reserved for JServer.
    * 
    * @since 2.0
    */
   public void log(Level level, String origin, String msg, int logMessageId)
   {
      this.log(fqcn, level, origin, msg, null, logMessageId);
   }
   
   /**
    * Logs a message with the specified level.
    * 
    * @param level the level of the logging event.
    * @param msg the message to log.
    * @param throwable a throwable to be associated with the logging event. This parameter may be <code>null</code>.
    * 
    * @since 2.0
    */
   public void log(Level level, String msg, Throwable throwable)
   {
      this.log(fqcn, level, null, msg, throwable);
   }
   
   /**
    * Logs a message with the specified level. This method supports messages of other types than String.
    * 
    * @param level the level of the logging event.
    * @param msg the message to log.
    * @param throwable a throwable to be associated with the logging event. This parameter may be <code>null</code>.
    * @param logMessageId the id associated with the log message. This id will be stored in the MDC as an java.lang.Integer object 
    * under the key {@link JServerConstants#LOG_MESSAGE_ID_KEY}. The range 4095-65535 is reserved for JServer.
    * 
    * @since 2.0
    */
   public void log(Level level, Object msg, Throwable throwable, int logMessageId)
   {
      this.log(fqcn, level, null, msg, throwable, logMessageId);
   }
   
   /**
    * Logs a message with the specified level. This method supports messages of other types than String.
    * 
    * @param level the level of the logging event.
    * @param origin the origin of the logging event. The thread name will be set to this parameter when creating the logging event.
    * @param msg the message to log.
    * @param throwable a throwable to be associated with the logging event. This parameter may be <code>null</code>.
    * 
    * @since 2.0
    */
   public void log(Level level, String origin, Object msg, Throwable throwable)
   {
      this.log(fqcn, level, origin, msg, throwable);
   }
   
   /**
    * Logs a message with the specified level. This method supports messages of other types than String.
    * 
    * @param level the level of the logging event.
    * @param origin the origin of the logging event. The thread name will be set to this parameter when creating the logging event.
    * @param msg the message to log.
    * @param throwable a throwable to be associated with the logging event. This parameter may be <code>null</code>.
    * @param logMessageId the id associated with the log message. This id will be stored in the MDC as an java.lang.Integer object 
    * under the key {@link JServerConstants#LOG_MESSAGE_ID_KEY}. The range 4095-65535 is reserved for JServer.
    * 
    * @since 2.0
    */
   public void log(Level level, String origin, Object msg, Throwable throwable, int logMessageId)
   {
      this.log(fqcn, level, origin, msg, throwable, logMessageId);
   }
   
   /**
    * Logs a message with the specified level. This method supports messages of other types than String.
    * 
    * @param callerFQN the fully qualified name of the calling class. This is only used for generating <code>org.apache.log4j.spi.LocationInfo</code>.
    * @param level the level of the logging event.
    * @param msg the message to log.
    * @param throwable a throwable to be associated with the logging event. This parameter may be <code>null</code>.
    * 
    * @since 2.0
    */
   public void log(String callerFQN, Level level, Object msg, Throwable throwable)
   {
      this.log(callerFQN, level, null, msg, throwable);
   }
   
   /**
    * Logs a message with the specified level. This method supports messages of other types than String.
    * 
    * @param callerFQN the fully qualified name of the calling class. This is only used for generating <code>org.apache.log4j.spi.LocationInfo</code>.
    * @param level the level of the logging event.
    * @param msg the message to log.
    * @param throwable a throwable to be associated with the logging event. This parameter may be <code>null</code>.
    * @param logMessageId the id associated with the log message. This id will be stored in the MDC as an java.lang.Integer object 
    * under the key {@link JServerConstants#LOG_MESSAGE_ID_KEY}. The range 4095-65535 is reserved for JServer.
    * 
    * @since 2.0
    */
   public void log(String callerFQN, Level level, Object msg, Throwable throwable, int logMessageId)
   {
      this.log(callerFQN, level, null, msg, throwable, logMessageId);
   }
   
   /**
    * Logs a message with the specified level. This method supports messages of other types than String.
    * 
    * @param callerFQN the fully qualified name of the calling class. This is only used for generating <code>org.apache.log4j.spi.LocationInfo</code>.
    * @param level the level of the logging event.
    * @param origin the origin of the logging event. The thread name will be set to this parameter when creating the logging event.
    * @param msg the message to log.
    * @param throwable a throwable to be associated with the logging event. This parameter may be <code>null</code>.
    * 
    * @since 2.0
    */
   public void log(final String callerFQN, final Level level, final String origin, final Object msg, final Throwable throwable)
   {
      this.doLog(callerFQN, level, origin, msg, throwable, false, -1);
   }
   
   /**
    * Logs a message with the specified level. This method supports messages of other types than String.
    * 
    * @param callerFQN the fully qualified name of the calling class. This is only used for generating <code>org.apache.log4j.spi.LocationInfo</code>.
    * @param level the level of the logging event.
    * @param origin the origin of the logging event. The thread name will be set to this parameter when creating the logging event.
    * @param msg the message to log.
    * @param throwable a throwable to be associated with the logging event. This parameter may be <code>null</code>.
    * @param logMessageId the id associated with the log message. This id will be stored in the MDC as an java.lang.Integer object 
    * under the key {@link JServerConstants#LOG_MESSAGE_ID_KEY}. The range 4095-65535 is reserved for JServer.
    * 
    * @since 2.0
    */
   public void log(final String callerFQN, final Level level, final String origin, final Object msg, final Throwable throwable, final int logMessageId)
   {
      this.doLog(callerFQN, level, origin, msg, throwable, true, logMessageId);
   }
   
   /**
    * Creates a logging event. All logging methods in this class calls this method 
    * to create a logging event, which means this method can be overridden by subclasses to intercept (and possibly modify) log calls.
    * 
    * @param callerFQN the fully qualified name of the calling class. This is only used for generating <code>org.apache.log4j.spi.LocationInfo</code>.
    * @param level the level of the logging event.
    * @param origin the origin of the logging event. The thread name will be set to this parameter when creating the logging event.
    * @param msg the message to log.
    * @param throwable a throwable to be associated with the logging event. This parameter may be <code>null</code>.
    * @param useLogMessageId flag indicating id the logMessageId parameter should be included in the context of the log message. 
    * @param logMessageId the id associated with the log message. This id will be stored in the MDC as an java.lang.Integer object 
    * under the key {@link JServerConstants#LOG_MESSAGE_ID_KEY}. The range 4095-65535 is reserved for JServer.
    * 
    * @since 2.1.1 (20060118)
    */
   protected void doLog(final String callerFQN, final Level level, String origin, final Object msg, final Throwable throwable, final boolean useLogMessageId, final int logMessageId)
   {
      final ComponentLogger logger = this.getLogger();
      
      synchronized(logger)
      {
         if( useLogMessageId ) MDC.put(JServerConstants.LOG_MESSAGE_ID_KEY, new Integer(logMessageId));
         
         // Get the logger that will be used to generate the category name field (origin) of the logging event 
         Logger originLogger = null;
         if( origin != null )
         {
            if( !origin.startsWith(JServerConstants.JSERVER_TOP_SYSTEM_ALIAS + ".") ) origin = JServerConstants.JSERVER_TOP_SYSTEM_ALIAS + "." + origin;
            originLogger = Logger.getLogger(origin);
         }
         else originLogger = logger;
         
         LoggingEvent loggingEvent = new LoggingEvent(callerFQN, originLogger, level, msg, throwable);
         if( this.generateLocationInformation ) loggingEvent.getLocationInformation();
         logger.log(loggingEvent);
         
         // We assume that the log messsage id has been copied when the above call is completed...         
         if( useLogMessageId ) MDC.remove(JServerConstants.LOG_MESSAGE_ID_KEY);
      }
   }
      
   /**
    * Convenience method to log a trace (finer-grained than debug) message (org.apache.log4j.Level.TRACE).
    * 
    * @param msg the message to log.
    * 
    * @since 2.1.4 (20060511)
    */
   public void logTrace(String msg)
   {
      this.log(Level.TRACE, msg);
   }
   
   /**
    * Convenience method to log a trace (finer-grained than debug) message (org.apache.log4j.Level.TRACE).
    * 
    * @param origin the origin of the logging event. The thread name will be set to this parameter when creating the logging event.
    * @param msg the message to log.
    * 
    * @since 2.1.4 (20060511)
    */
   public void logTrace(String origin, String msg)
   {
      this.log(Level.TRACE, origin, msg);
   }
   
   /**
    * Convenience method to log a debug message (org.apache.log4j.Level.DEBUG).
    * 
    * @param msg the message to log.
    */
   public void logDebug(String msg)
   {
      this.log(Level.DEBUG, msg);
   }
   
   /**
    * Convenience method to log a debug message (org.apache.log4j.Level.DEBUG).
    * 
    * @param origin the origin of the logging event. The thread name will be set to this parameter when creating the logging event.
    * @param msg the message to log.
    */
   public void logDebug(String origin, String msg)
   {
      this.log(Level.DEBUG, origin, msg);
   }
   
   /**
    * Convenience method to log an information message (org.apache.log4j.Level.INFO).
    * 
    * @param msg the message to log.
    */
   public void logInfo(String msg)
   {
      this.log(Level.INFO, msg);
   }
   
   /**
    * Convenience method to log an information message (org.apache.log4j.Level.INFO).
    * 
    * @param origin the origin of the logging event. The thread name will be set to this parameter when creating the logging event.
    * @param msg the message to log.
    */
   public void logInfo(String origin, String msg)
   {
      this.log(Level.INFO, origin, msg);
   }
   
   /**
    * Convenience method to log a warning message (org.apache.log4j.Level.WARNING).
    * 
    * @param msg the message to log.
    */
   public void logWarning(String msg)
   {
      this.log(Level.WARN, msg);
   }
   
   /**
    * Convenience method to log a warning message (org.apache.log4j.Level.WARNING).
    * 
    * @param origin the origin of the logging event. The thread name will be set to this parameter when creating the logging event.
    * @param msg the message to log.
    */
   public void logWarning(String origin, String msg)
   {
      this.log(Level.WARN, origin, msg);
   }
   
   /**
    * Convenience method to log a warning message (org.apache.log4j.Level.WARNING).
    * 
    * @param msg the message to log.
    * @param throwable a throwable to be associated with the logging event. This parameter may be <code>null</code>.
    */
   public void logWarning(String msg, Throwable throwable)
   {
      this.log(Level.WARN, msg, (Throwable)throwable);
   }
   
   /**
    * Convenience method to log a warning message (org.apache.log4j.Level.WARNING).
    * 
    * @param origin the origin of the logging event. The thread name will be set to this parameter when creating the logging event.
    * @param msg the message to log.
    * @param throwable a throwable to be associated with the logging event. This parameter may be <code>null</code>.
    */
   public void logWarning(String origin, String msg, Throwable throwable)
   {
      this.log(Level.WARN, origin, msg, throwable);
   }
   
   /**
    * Convenience method to log an error message (org.apache.log4j.Level.ERROR).
    * 
    * @param msg a description of the error.
    * 
    * @see com.teletalk.jserver.log.LogManager
    */
   public void logError(String msg)
   {
      this.log(Level.ERROR, msg);
   }
   
   /**
    * Convenience method to log an error message (org.apache.log4j.Level.ERROR).
    * 
    * @param origin the origin of the logging event. The thread name will be set to this parameter when creating the logging event.
    * @param msg a description of the error.
    * 
    * @see com.teletalk.jserver.log.LogManager
    */
   public void logError(String origin, String msg)
   {
      this.log(Level.ERROR, origin, msg);
   }
   
   /**
    * Convenience method to log an error message (org.apache.log4j.Level.ERROR).
    * 
    * @param msg a msg of the error.
    * @param throwable a throwable to be associated with the logging event. This parameter may be <code>null</code>.
    * 
    * @see com.teletalk.jserver.log.LogManager
    */
   public void logError(String msg, Throwable throwable)
   {
      this.log(Level.ERROR, msg, throwable);
   }
   
   /**
    * Convenience method to log an error message (org.apache.log4j.Level.ERROR).
    * 
    * @param origin the origin of the logging event. The thread name will be set to this parameter when creating the logging event.
    * @param msg a description of the error.
    * @param throwable a throwable to be associated with the logging event. This parameter may be <code>null</code>.
    * 
    * @see com.teletalk.jserver.log.LogManager
    */
   public void logError(String origin, String msg, Throwable throwable)
   {
      this.log(Level.ERROR, origin, msg, throwable);
   }
      
   /**
    * Convenience method to log a critical error message (org.apache.log4j.Level.FATAL).
    * 
    * @param msg the message to log.
    * 
    * @deprecated replaced by {@link #logCriticalError(String, int)}
    */
   public void logCriticalError(String msg)
   {
      this.log(Level.FATAL, msg);
   }
   
   /**
    * Convenience method to log a critical error message (org.apache.log4j.Level.FATAL).
    * 
    * @param msg the message to log.
    * @param logMessageId the id associated with the log message. This id will be stored in the MDC as an java.lang.Integer object 
    * under the key {@link JServerConstants#LOG_MESSAGE_ID_KEY}. The range 4095-65535 is reserved for JServer.
    */
   public void logCriticalError(String msg, int logMessageId)
   {
      this.log(Level.FATAL, msg, logMessageId);
   }
   
   /**
    * Convenience method to log a critical error message (org.apache.log4j.Level.FATAL).
    * 
    * @param origin the origin of the logging event. The thread name will be set to this parameter when creating the logging event.
    * @param msg the message to log.
    * 
    * @deprecated replaced by {@link #logCriticalError(String, String, int)}
    */
   public void logCriticalError(String origin, String msg)
   {
      this.log(Level.FATAL, origin, msg);
   }
   
   /**
    * Convenience method to log a critical error message (org.apache.log4j.Level.FATAL).
    * 
    * @param origin the origin of the logging event. The thread name will be set to this parameter when creating the logging event.
    * @param msg the message to log.
    * @param logMessageId the id associated with the log message. This id will be stored in the MDC as an java.lang.Integer object 
    * under the key {@link JServerConstants#LOG_MESSAGE_ID_KEY}. The range 4095-65535 is reserved for JServer.
    */
   public void logCriticalError(String origin, String msg, int logMessageId)
   {
      this.log(Level.FATAL, origin, msg, logMessageId);
   }
   
   /**
    * Convenience method to log a critical error message (org.apache.log4j.Level.FATAL).
    * 
    * @param msg the message to log.
    * @param throwable a throwable to be associated with the logging event. This parameter may be <code>null</code>.
    * 
    * @deprecated replaced by {@link #logCriticalError(String, Throwable, int)}
    */
   public void logCriticalError(String msg, Throwable throwable)
   {
      this.log(Level.FATAL, msg, throwable);
   }
   
   /**
    * Convenience method to log a critical error message (org.apache.log4j.Level.FATAL).
    * 
    * @param msg the message to log.
    * @param throwable a throwable to be associated with the logging event. This parameter may be <code>null</code>.
    * @param logMessageId the id associated with the log message. This id will be stored in the MDC as an java.lang.Integer object 
    * under the key {@link JServerConstants#LOG_MESSAGE_ID_KEY}. The range 4095-65535 is reserved for JServer.
    */
   public void logCriticalError(String msg, Throwable throwable, int logMessageId)
   {
      this.log(Level.FATAL, msg, throwable, logMessageId);
   }
   
   /**
    * Convenience method to log a critical error message (org.apache.log4j.Level.FATAL).
    * 
    * @param origin the origin of the logging event. The thread name will be set to this parameter when creating the logging event.
    * @param msg the message to log.
    * @param throwable a throwable to be associated with the logging event. This parameter may be <code>null</code>.
    * 
    * @deprecated replaced by {@link #logCriticalError(String, String, Throwable, int)}
    */
   public void logCriticalError(String origin, String msg, Throwable throwable)
   {
      this.log(Level.FATAL, origin, msg, throwable);
   }
   
   /**
    * Convenience method to log a critical error message (org.apache.log4j.Level.FATAL).
    * 
    * @param origin the origin of the logging event. The thread name will be set to this parameter when creating the logging event.
    * @param msg the message to log.
    * @param throwable a throwable to be associated with the logging event. This parameter may be <code>null</code>.
    * @param logMessageId the id associated with the log message. This id will be stored in the MDC as an java.lang.Integer object 
    * under the key {@link JServerConstants#LOG_MESSAGE_ID_KEY}. The range 4095-65535 is reserved for JServer.
    */
   public void logCriticalError(String origin, String msg, Throwable throwable, int logMessageId)
   {
      this.log(Level.FATAL, origin, msg, throwable, logMessageId);
   }
   
   /* ##### LOGGING METHODS ENDS #####*/
}
