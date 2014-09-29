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
package com.teletalk.jserver;

/**
 * Various constants used by the JServer core components.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta
 */
public interface JServerConstants
{
   public static final long DEFAULT_STATUS_TRANSITION_TIMEOUT = 300*1000;
   
   
   /** String used as global alias of the top system (i.e. instead of using the name of the server). The value of this field is 'JServer'. */
   public static final String JSERVER_TOP_SYSTEM_ALIAS = "JServer";
   
   /* ### CORE COMPONENT ALIASES ###*/
   
   /** Core component alias for the PropertyManager (value: ConfigurationManager). */
   public static final String CONFIGURATION_MANAGER_ALIAS = "ConfigurationManager";

   /** Core component alias for the EventQueue (value: EventQueue). */
   public static final String EVENT_QUEUE_ALIAS = "EventQueue";
            
   /** Core component alias for the LogManager (value: LogManager). */
   public static final String LOG_MANAGER_ALIAS = "LogManager";
   
   /** Core component alias for the DefaultCriticalErrorLogger (value: DefaultCriticalErrorLogger). */
   public static final String DEFAULT_CRITICAL_ERROR_LOGGER_ALIAS = "DefaultCriticalErrorLogger";

   /** Core component alias for the DefaultFileLogger (value: DefaultFileLogger). */
   public static final String DEFAULT_FILE_LOGGER_ALIAS = "DefaultFileLogger";

   /** Core component alias for the LoadManager (value: LoadManager). */
   public static final String LOAD_MANAGER_ALIAS = "LoadManager";

   /** Core component alias for the MainHttpServer (value: MainHttpServer). */
   public static final String MAIN_HTTP_SERVER_ALIAS = "MainHttpServer";

   /** Core component alias for the PeriodicActionManager (value: PeriodicActionManager). */
   public static final String PERIODIC_ACTION_MANAGER_ALIAS = "PeriodicActionManager";

   /** Core component alias for the RmiAdministration (value: RmiAdministration). */
   public static final String RMI_ADMINISTRATION_ALIAS = "RmiAdministration";

   /** Core component alias for the RmiManager (value: RmiManager). */
   public static final String RMI_MANAGER_ALIAS = "RmiManager";

   /** Core component alias for the SnsClientManager (value: SnsClientManager). */
   public static final String SNS_CLIENT_MANAGER_ALIAS = "SnsClientManager";

   /** Core component alias for the SnsManager (value: SnsManager). */
   public static final String SNS_MANAGER_ALIAS = "SnsManager";

   /** Core component alias for the SpringApplicationContext (value: SpringApplicationContext). */
   public static final String SPRING_APPLICATION_CONTEXT_ALIAS = "SpringApplicationContext";

   /** Core component alias for the StatisticsManager (value: StatisticsManager). */
   public static final String STATISTICS_MANAGER_ALIAS = "StatisticsManager";
   
   
   public static final String[] coreComponentNames = new String[] { 
      CONFIGURATION_MANAGER_ALIAS, EVENT_QUEUE_ALIAS, LOG_MANAGER_ALIAS, 
      DEFAULT_CRITICAL_ERROR_LOGGER_ALIAS, DEFAULT_FILE_LOGGER_ALIAS, LOAD_MANAGER_ALIAS, 
      MAIN_HTTP_SERVER_ALIAS, PERIODIC_ACTION_MANAGER_ALIAS, RMI_ADMINISTRATION_ALIAS, 
      RMI_MANAGER_ALIAS, SNS_CLIENT_MANAGER_ALIAS, SNS_MANAGER_ALIAS, 
      SPRING_APPLICATION_CONTEXT_ALIAS, STATISTICS_MANAGER_ALIAS
   };
   
   
   public static final int STATUS_TRANSITION_TYPE_ENGAGE = 0;
   
   public static final int STATUS_TRANSITION_TYPE_SHUT_DOWN = 1;
   
   public static final int STATUS_TRANSITION_TYPE_REINITIALIZE = 2;
   
   public static final int STATUS_TRANSITION_TYPE_ERROR = 3;
   
   public static final int STATUS_TRANSITION_TYPE_CRITICAL_ERROR = 4;
   
   public static final String[] statusTransitionTypeNames = {"engage", "shut down", "reinitialize", "error", "critical error"};
   
   
   /** Constant for the status ENABLED. */
	public static final int ENABLED							= 0;
	
	/** Constant for the status DISABLED. @deprecated as of 2.0, replaced by {@link #DOWN} */
	public static final int DISABLED							= 6; // 1 before
	
	/** Constant for the status INITIALIZING. */
	public static final int INITIALIZING					= 2;
	
	/** Constant for the status CREATED. */
	public static final int CREATED							= 3;  // The initial status
	
	/** Constant for the status REINITIALIZING. */
	public static final int REINITIALIZING				= 4;
	
	/** Constant for the status SHUTTING_DOWN. */
	public static final int SHUTTING_DOWN			= 5;
	
	/** Constant for the status DOWN. */
	public static final int DOWN								= 6;
	
	/** Constant for the status ERROR. */
	public static final int ERROR								= 7;
	
	/** Constant for the status CRITICAL_ERROR. */
	public static final int CRITICAL_ERROR			= 8;
   
   /** Constant for the status CRITICAL_ERROR. */
   public static final int DESTROYED       = 9;
	
   
	/** Array containing the names for the different statuslevels. */
	public static final String[] statusNames = {"ENABLED", "DOWN", "INITIALIZING", "CREATED", 
																					"REINITIALIZING", "SHUTTING_DOWN", "DOWN", "ERROR", "CRITICAL ERROR", "DESTROYED"};
																					
   
	
   
	/** Critical log message error constant (5000) indicating that the main thread of the JServer has failed. */
	public static final int LOG_MESSAGE_ID_CRITICAL_JSERVER_ERROR = 5000;
	
	/** Critical log message error constant (6000) indicating that a subsystem has failed. */
	public static final int LOG_MESSAGE_ID_CRITICAL_SUBSYSTEM_ERROR = 6000;
   
   /** Critical log message error constant (6001) indicating that an error in a log appender has occurred. */
   public static final int LOG_MESSAGE_ID_LOG_APPENDER_ERROR = 6001;
	
	/** Critical log message error constant (7000) indicating that an out of memory error has occurred. */
	public static final int LOG_MESSAGE_ID_OUT_OF_MEMORY_ERROR = 7000;
	
	/** Critical log message error constant (7001) indicating that an out of memory error has occurred. */
	//public static final int LOG_MESSAGE_ID_LOW_MEMORY_WARNING = 7001;
	
	/** Critical log message error constant (8000) indicating that an rmi registry failure has occurred and it will not be possible to connecto to the server through JAdmin. */
	public static final int LOG_MESSAGE_ID_RMI_REGISTRY_FAILURE = 8000;
   
   /** Critical log message error constant (9000) indicating low disk space {@link com.teletalk.jserver.util.DiskSpaceCheckAction}. */
   public static final int LOG_MESSAGE_ID_LOW_DISK_SPACE = 9000;
   
   /** Critical log message error constant (9001) indicating that an error occurred while checking disk space {@link com.teletalk.jserver.util.DiskSpaceCheckAction}. */
   public static final int LOG_MESSAGE_ID_DISK_SPACE_CHECK_ERROR = 9001;
   
   /** Critical log message error constant (9100) indicating that a periodic action exceeded the maximum execution time. @since 2.1 (20050524) */
   public static final int LOG_MESSAGE_ID_ACTION_EXECUTION_TIME_EXCEEDED = 9100;
         
   
   /** Key for log message id (java.lang.Integer) objects stored in MDC. The value of this key is "logMessageId". The range 4095-65535 is reserved for JServer. */
   public static final String LOG_MESSAGE_ID_KEY = "logMessageId";
   
   /** The value is used to indicate that a log message id has not been specified. This constant has the value of Integer.MIN_VALUE, which is to be considered a reserved value.  */
   public static final int LOG_MESSAGE_ID_UNSPECIFIED = Integer.MIN_VALUE;
}
