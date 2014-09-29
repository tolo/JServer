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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;

import com.teletalk.jserver.event.EventQueue;
import com.teletalk.jserver.load.LoadManager;
import com.teletalk.jserver.log.CriticalErrorAppenderComponent;
import com.teletalk.jserver.log.FileAppenderComponent;
import com.teletalk.jserver.log.LogManager;
import com.teletalk.jserver.net.sns.client.SnsClientManager;
import com.teletalk.jserver.net.sns.server.SnsManager;
import com.teletalk.jserver.periodic.PeriodicActionManager;
import com.teletalk.jserver.property.CalculatedPropertyOwner;
import com.teletalk.jserver.property.NumberProperty;
import com.teletalk.jserver.property.PersistentPropertyStorage;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.property.PropertyManager;
import com.teletalk.jserver.property.StringProperty;
import com.teletalk.jserver.rmi.Administration;
import com.teletalk.jserver.rmi.RmiManager;
import com.teletalk.jserver.rmi.adapter.JServerRmiAdapter;
import com.teletalk.jserver.rmi.adapter.RmiAdapter;
import com.teletalk.jserver.spring.SpringApplicationContext;
import com.teletalk.jserver.statistics.StatisticsManager;
import com.teletalk.jserver.tcp.TcpServer;
import com.teletalk.jserver.tcp.http.HttpServer;
import com.teletalk.jserver.util.ReflectionUtils;

/**
 * The JServer class is the main class of the JSever architecture, and also the topmost 
 * subsystem in the system heirarchy. The primary function of this class is to manage 
 * all core systems of the server and to detect and handle errors in subsystems (with a little help 
 * from the class {@link TopThreadGroup}).<br>
 * <br>
 * JServer also provides a number of convenience methods to create various common subsystems, such 
 * as tcp servers etc.<br>
 * <br>
 * When building a server with the JServer package one should go through the following steps:<br>
 * <br><br>
 * <b>1</b>. Create the JServer object. The following row creates a JServer object using one of its simplest constructors.
 * <br><code>JServer server = new JServer("TestServer");</code><br><br>
 *
 * <b>2</b>. Add custom Loggers (if any). For instance:
 * <br><code>server.getLogManager().attachLogger(new DBLogger(server.getLogManager(), "DBLogger", "sun.jdbc.odbc.JdbcOdbcDriver", "jdbc:odbc:alpha"));</code><br><br>
 *  
 * <b>3</b>. Add custom SubSystems. For instance:
 * <br><code>server.addSubSystem(new KahunaSystem(server), false);</code><br><br>
 *
 * <b>4</b>. Start the server with the following line:
 * <br><code>server.startJServer();</code><br><br>
 * 
 * @author Tobias Löfstrand
 *
 * @see TopThreadGroup
 *
 * @since The very beginning
 */
public final class JServer extends SubSystem implements CalculatedPropertyOwner
{
	/** The name of this application. */
	final static String appName = "JServer";
	/** The major version number. */
	final static short appVersionMajor = 2;
	/** The minor version number. */
	final static short appVersionMinor = 2;
	/** The micro version number. */
	final static short appVersionMicro = 0;
	/** The type of this build (e.g. Debug or Release). */
   final static String buildType = (JServer.class.getPackage().getImplementationTitle() != null) ? JServer.class.getPackage().getImplementationTitle().trim() : "";
	//final static String buildType = (JServer.class.getPackage().getImplementationVersion() != null) ? JServer.class.getPackage().getImplementationVersion() : "";
   /** The build id. */
   final static String buildId = (JServer.class.getPackage().getImplementationVersion() != null) ? JServer.class.getPackage().getImplementationVersion().trim() : "";
   /** Sub build name. */
	final static String buildExtra = null; // "RC" "Beta"

	/** A string containing application and version information. */
   final static String versionString;
   
   /** A string containing version information. */
   final static String versionOnlyString;

   /** The default name of the main http server. */
	public static final String MainHttpServerName = "MainHttpServer";
   
   private static final long DEFAULT_MAX_SHUT_DOWN_WAIT_TIME = 60*1000; // 60 seconds
      

	/** Flag indicated if a static call to the method {@link #getJServer()} is allowed to create a new JServer object if none exists. The defaultvalue is <code>false</code>.*/
	public static boolean canCreateDefaultJServer = false;

	/** A static reference to a JServer object (singleton). */
	private static JServer topSystem = null;
	
   static
   {
      String buildString = null;
      if( buildId != null ) buildString = "Build " + buildId;
      
      String extraString = "";
      if( (buildString != null) || (buildExtra != null) || (buildType != null) )
      {
         extraString = " (";
         
         if( buildString != null ) extraString += buildString;
         
         if( buildExtra != null ) extraString += ((buildString != null) ? " " : "") + buildExtra;
         
         if( buildType != null ) extraString += (((buildString != null)||(buildExtra != null)) ? " " : "") + buildType;
         
         extraString += ")";
      }
      
      versionOnlyString = appVersionMajor + "." + appVersionMinor + "." + appVersionMicro + extraString;
      versionString = appName + " " + versionOnlyString; 
   }
   
	/** Flag indicating if the server process should be killed when it runs out of memory.  */
	private boolean killProcessOnOutOfMemoryError = true;

	/** Value indicating when this JServer was last restarted. */
	private long lastRestart = -1;

	/** The top theadgroup of this server. */
	private TopThreadGroup threadGroup;
	   

	/** Reference to a PropertyManager. */
	private final PropertyManager propertyManager;

	/** Reference to the logmanager in the server. */
	private final LogManager logManager;

	/** Reference to the eventQueue in the server. */
	private final EventQueue eventQueue;

	/** Reference to a RmiManager. */
	private RmiManager rmiManager = null;

	/** Reference to the main http server. */
	private HttpServer mainHttpServer = null;
   
   private PeriodicActionManager periodicActionManager = null;
   
   private LoadManager loadManager = null;
   
   private StatisticsManager statisticsManager = null;
   
   private SpringApplicationContext springApplicationContext = null;
   
   private SnsClientManager snsClientManager = null;
   
   private SnsManager snsManager = null;
     
   /** Reference to an Administration object. */
   private Administration administration;
	
	private FileAppenderComponent defaultFileLogger;
	
	private CriticalErrorAppenderComponent defaultCriticalErrorLogger;
	
	
	/** NumberProperty containing the number of active threads in the server. */
	private NumberProperty activeThreadCount;

	/** StringProperty containing the version of this JServer. */
	private StringProperty jServerVersion;

	/** StringProperty containing the version of this Server. */
	private StringProperty serverVersion;
   
	/** The interval in milliseconds between two consecutive SubSystem checks. */
	private NumberProperty checkInterval;

	/** StringProperty indicating the current directory. */
	private StringProperty currentDirectory;
	
	/** StringProperty presenting info on the current java vm. */
	private StringProperty javaVMInfo;
	
	/** The threshold for minimum amount of free memory that will trigger a critical error. */
	//private NumberProperty memoryWarningThreshold; // TODO
	   
		
	/** Flag used to signal that the whole server is shutting down. */
	private static volatile boolean serverIsShuttingDown = false;
	
	// Reference to callback server restart method - Used by JServerLauncher.
	static Method launcherRestartMethod = null;
	
	// Method to register callback server restart method - Used by JServerLauncher.
	public static void registerLauncherRestartMethod(final Method launcherRestartMethod)
	{
		JServer.launcherRestartMethod = launcherRestartMethod;
	}
	
	long maxShutDownWaitTime = DEFAULT_MAX_SHUT_DOWN_WAIT_TIME;
      
   
	
	/**
	 * Constructs a new JServer object and optionally creates and adds default SubSystems such as PropertyManager,
	 * LogManager, RmiManager, EventQueue and a main HttpServer to the that object. This constructor makes it possible to specify the 
	 * implementations of PersistentPropertyStorage that will be used by the PropertyManager and the main http server implementation.
	 *
	 * @param name the name of the server.
	 * @param checkInterval the interval in milliseconds between two consecutive SubSystem checks.
	 * @param persistentPropertyStorage The PersistentPropertyStorage implementation to be used with the PropertyManager.
	 * @param createDefaultSubSystems boolean value indicating if default SubSystems such as PropertyManager,
	 * LogManager, RmiManager, EventQueue and main HttpServer should be created and added to the JServer object (strongly recommended!).
	 * @param createDefaultLoggers boolean value indicating if default loggers (a FileLogger and a CriticalErrorLogger)
	 * should be created.
	 */
	public JServer(String name, long checkInterval, PersistentPropertyStorage persistentPropertyStorage, boolean createDefaultSubSystems, boolean createDefaultLoggers)
	{
		super(null, name);
		JServer.topSystem = this;
      JServer.serverIsShuttingDown = false;

		this.activeThreadCount = new NumberProperty(this, "active threads", 0);
		this.activeThreadCount.setDescription("The number of active threads in this server.");
		this.jServerVersion = new StringProperty(this, "JServer version", versionOnlyString);
		this.jServerVersion.setDescription("The JServer version.");
		this.serverVersion = new StringProperty(this, "server version", versionString); // Set server version to JServer version as default
		this.serverVersion.setDescription("The version of this server implementation.");
		this.checkInterval = new NumberProperty(this, "check interval", checkInterval, Property.MODIFIABLE_NO_RESTART);
		this.checkInterval.setDescription("The interval in milliseconds between two consecutive SubSystem checks. The minimum value of this property is 1000 ms (although it is advisable to use a considerably higher value).");
		this.currentDirectory = new StringProperty(this, "current directory", new java.io.File("").getAbsolutePath());
		this.currentDirectory.setDescription("The current directory of the server process.");
		//this.memoryWarningThreshold = new NumberProperty(this, "memory warning threshold", -1, Property.MODIFIABLE_NO_RESTART);
		//this.memoryWarningThreshold.setDescription("");
      		
		
		// Build a string containing information on the current java vm...
		String javaVMString = "";		
		
		String str = System.getProperty("java.vm.name");
		javaVMString += (str != null) ? str : "";
		
		str = System.getProperty("java.vm.version");
		javaVMString += (str != null) ? (" " + str) : "";
		
		str = System.getProperty("java.vm.info");
		javaVMString += (str != null) ? (" " + str) : "";
		
		if(javaVMString.length() == 0) javaVMString = "Unknown";
				
		this.javaVMInfo = new StringProperty(this, "Java VM", javaVMString);
		this.javaVMInfo.setDescription("The Java VM used by the server process.");

		addProperty(this.activeThreadCount);
		addProperty(this.jServerVersion);
		addProperty(this.serverVersion);
		addProperty(this.checkInterval);
		addProperty(this.currentDirectory);
		addProperty(this.javaVMInfo);

		this.threadGroup = new TopThreadGroup(this, "TopThreadGroup");

      
      SubComponent dynamicComponent;
      
      
      /* ### CREATE FIXED SUBSYSTEMS BEGIN ## */ 
      

      // CREATE LOGMANAGER
      this.logManager = new LogManager(this);
      addSubSystem(logManager, false);
            
      logInfo("Initializing " + getVersionString());
      
      // CREATE PROPERTYMANAGER
      if(persistentPropertyStorage != null)
      {
         this.propertyManager = new PropertyManager(null, persistentPropertyStorage);
      }
      else
      {
         this.propertyManager = new PropertyManager(null);
      }
      addSubSystem(propertyManager, false);
      
      // Init properties of LogManager (addToRootLogger)...
      this.logManager.initProperties(); 
      
		// CREATE EVENTQUEUE
      this.eventQueue = new EventQueue(this);
      addSubSystem(eventQueue, true);
      
      
      /* ### CREATE FIXED SUBSYSTEMS END ## */
      
      
      // CHECK IF DEFAULT LOGGERS (APPENDERS) SHOULD BE CREATED
      if(createDefaultLoggers)
      {
         // Attempt to find dynamically created default file logger...
         dynamicComponent = instantiateDynamicCoreComponent(PropertyManager.DEFAULT_FILE_LOGGER_ALIAS);
         if( (dynamicComponent != null) && (dynamicComponent instanceof FileAppenderComponent) )
         {
            this.defaultFileLogger = (FileAppenderComponent)dynamicComponent;
         }
         else this.defaultFileLogger = new FileAppenderComponent(this.logManager); // ...or create
         
         // Attempt to find dynamically created default critical error logger...         
         dynamicComponent = instantiateDynamicCoreComponent(PropertyManager.DEFAULT_CRITICAL_ERROR_LOGGER_ALIAS);
         if( (dynamicComponent != null) && (dynamicComponent instanceof CriticalErrorAppenderComponent) )
         {
            this.defaultCriticalErrorLogger = (CriticalErrorAppenderComponent)dynamicComponent;
         }
         else this.defaultCriticalErrorLogger = new CriticalErrorAppenderComponent(this.logManager); // ...or create
         
         logManager.addAppender(this.defaultFileLogger);
         logManager.addAppender(this.defaultCriticalErrorLogger);
      }
      

      // CHECK IF DEFAULT SUBSYSTEMS SHOULD BE CREATED
		if(createDefaultSubSystems)
		{
         // CREATE MAIN HTTP SERVER
		   // Attempt to find dynamically created http server...
         dynamicComponent = instantiateDynamicCoreComponent(PropertyManager.MAIN_HTTP_SERVER_ALIAS);
         if( (dynamicComponent != null) && (dynamicComponent instanceof HttpServer) )
         {
            this.mainHttpServer = (HttpServer)dynamicComponent;
         }
         else // ...or create MainHttpServer
         {
            this.mainHttpServer = HttpServer.createDefaultHttpServer(this, MainHttpServerName, 9901);
            this.mainHttpServer.setPoolSize(2);  //Set the default poolsize of the main http server to 2
         }
         this.mainHttpServer.initProperties();
         addSubSystem(this.mainHttpServer, false);
			
         // CREATE RMI MANAGER
         dynamicComponent = instantiateDynamicCoreComponent(PropertyManager.RMI_MANAGER_ALIAS);
         if( (dynamicComponent != null) && (dynamicComponent instanceof RmiManager) )
         {
            this.rmiManager = (RmiManager)dynamicComponent; 
         }
         else
         {
            this.rmiManager = new RmiManager(this);
         }
         addSubSystem(rmiManager, false);
			//Initialize the rmi properties for rmi codebase and host. It is imperative that these properties gets initialized as soon as possible!
			this.rmiManager.initRmiProperties();

         // CREATE PERIODIC ACTION MANAGER
         dynamicComponent = instantiateDynamicCoreComponent(PropertyManager.PERIODIC_ACTION_MANAGER_ALIAS);
         if( (dynamicComponent != null) && (dynamicComponent instanceof PeriodicActionManager) )
         {
            this.periodicActionManager = (PeriodicActionManager)dynamicComponent; 
         }
         else
         {
            this.periodicActionManager = new PeriodicActionManager(this);
         }
         addSubSystem(this.periodicActionManager, false);
         
         // CREATE RMI ADMINISTRATION
			try
			{
				this.administration = new Administration();
				logInfo("Administration created");
			}
			catch(Exception e)
			{
				e.printStackTrace();
				logError("Error while creating Administration - " + e);
			}
		}
            
		// Initialize any dynamic core components that hasn't already been created...
      this.instantiateDynamicCoreComponents();
	}
	
	/**
	 * Constructs a new JServer object and optionally creates and adds default SubSystems such as PropertyManager,
	 * LogManager, RmiManager, EventQueue and a main HttpServer to the that object. This constructor makes it possible to specify the 
	 * main http server implementation.
	 *
	 * @param name the name of the server.
	 * @param checkInterval the interval in milliseconds between two consecutive SubSystem checks.
	 * @param createDefaultSubSystems boolean value indicating if default SubSystems such as PropertyManager,
	 * LogManager, RmiManager, EventQueue and main HttpServer should be created and added to the JServer object (strongly recommended!).
	 * @param createDefaultLoggers boolean value indicating if default loggers (a FileLogger and a CriticalErrorLogger)
	 * should be created.
	 */
	public JServer(String name, long checkInterval, boolean createDefaultSubSystems, boolean createDefaultLoggers)
	{
		this(name, checkInterval, null, createDefaultSubSystems, createDefaultLoggers);
	}
	
	/**
	 * Constructs a new JServer object and optionally creates and adds default SubSystems such as PropertyManager,
	 * LogManager, RmiManager, EventQueue and a main HttpServer to the that object. This constructor makes it possible to specify the 
	 * implementation of PersistentPropertyStorage that will be used by the PropertyManager.
	 * A check interval of 30 seconds will be used.
	 *
	 * @param name the name of the server.
	 * @param persistentPropertyStorage The PersistentPropertyStorage implementation to be used with the PropertyManager.
	 * @param createDefaultSubSystems boolean value indicating if default SubSystems such as PropertyManager,
	 * LogManager, RmiManager, EventQueue and main HttpServer should be created and added to the JServer object (strongly recommended!).
	 * @param createDefaultLoggers boolean value indicating if default loggers (a FileLogger and a CriticalErrorLogger)
	 * should be created.
	 */
	public JServer(String name, PersistentPropertyStorage persistentPropertyStorage, boolean createDefaultSubSystems, boolean createDefaultLoggers)
	{
		this(name, 30*1000, persistentPropertyStorage, createDefaultSubSystems, createDefaultLoggers);
	}
	
	/**
	 * Constructs a new JServer object and optionally creates and adds default SubSystems such as PropertyManager,
	 * LogManager, RmiManager, EventQueue and a main HttpServer to the that object. This constructor makes it possible to specify the 
	 * main http server implementation.
	 * A check interval of 30 seconds will be used.
	 *
	 * @param name the name of the server.
	 * @param createDefaultSubSystems boolean value indicating if default SubSystems such as PropertyManager,
	 * LogManager, RmiManager, EventQueue and main HttpServer should be created and added to the JServer object (strongly recommended!).
	 * @param createDefaultLoggers boolean value indicating if default loggers (a FileLogger and a CriticalErrorLogger)
	 * should be created.
	 */
	public JServer(String name, boolean createDefaultSubSystems, boolean createDefaultLoggers)
	{
		this(name, 30*1000, null, createDefaultSubSystems, createDefaultLoggers);
	}
	
	/**
	 * Constructs a new JServer object and creates and adds default SubSystems such as PropertyManager,
	 * LogManager, RmiManager, EventQueue and a main HttpServer to the that object. This constructor makes it possible to specify the 
	 * implementation of PersistentPropertyStorage that will be used by the PropertyManager.
	 * A check interval of 30 seconds will be used.
	 *
	 * @param name the name of the server.
	 * @param persistentPropertyStorage The PersistentPropertyStorage implementation to be used with the PropertyManager.
	 */
	public JServer(String name, PersistentPropertyStorage persistentPropertyStorage)
	{
		this(name, 30*1000, persistentPropertyStorage, true, true);
	}

	/**
	 * Constructs a new JServer object with a 30 second checkInterval.
	 *
	 * @param name the name of the server.
	 */
	public JServer(String name)
	{
		this(name, 30*1000, true, true);
	}

	/**
	 * Constructs a new JServer object named "JServer" with a 30 second checkInterval and default SubSystems and loggers created.
	 */
	public JServer()
	{
		this(JServerConstants.JSERVER_TOP_SYSTEM_ALIAS);
	}
   
   /**
    * Static method to create and start an empty JServer. Note that this method sets the flag 
    * {@link #setKillProcessOnOutOfMemoryError(boolean) killProcessOnOutOfMemoryError} to false on the created JServer.
    *
    * @return a newly created JServer.
    */
   public static JServer createEmptyJServer()
   {
      return createEmptyJServer(JServerConstants.JSERVER_TOP_SYSTEM_ALIAS, true);
   }

	/**
	 * Static method to create an empty JServer. Note that this method sets the flag 
    * {@link #setKillProcessOnOutOfMemoryError(boolean) killProcessOnOutOfMemoryError} to false on the created JServer.
    * 
    * @param start flag indicating if the server should be started with in this method call.
	 *
	 * @return a newly created JServer.
    * 
    * @since 2.0
	 */
	public static JServer createEmptyJServer(boolean start)
	{
		return createEmptyJServer(JServerConstants.JSERVER_TOP_SYSTEM_ALIAS, start);
	}
   
   /**
    * Static method to create and start an empty JServer. Note that this method sets the flag 
    * {@link #setKillProcessOnOutOfMemoryError(boolean) killProcessOnOutOfMemoryError} to false on the created JServer.
    *
    * @param name the name of the server.
    *
    * @return a newly created JServer.
    */
   public synchronized static JServer createEmptyJServer(String name)
   {
      return createEmptyJServer(name, true);
   }

	/**
	 * Static method to create an empty JServer. Note that this method sets the flag 
    * {@link #setKillProcessOnOutOfMemoryError(boolean) killProcessOnOutOfMemoryError} to false on the created JServer.
	 *
	 * @param name the name of the server.
    * @param start flag indicating if the server should be started with in this method call.
	 *
	 * @return a newly created JServer.
    * 
    * @since 2.0
	 */
	public synchronized static JServer createEmptyJServer(String name, boolean start)
	{
		/*if( topSystem == null )
		{*/
			topSystem = new JServer(name, 30*1000, false, false);
         topSystem.killProcessOnOutOfMemoryError = false;
			if( start ) topSystem.startJServer();
		/*}*/
		return topSystem;
	}

	/**
	 * Singleton method to get a JServer instance.
	 *
	 * @return JServer object.
	 */
	public synchronized static JServer getJServer()
	{
		if((topSystem == null) && canCreateDefaultJServer)
		{
			topSystem = new JServer(JServerConstants.JSERVER_TOP_SYSTEM_ALIAS);
			topSystem.startJServer();
		}
		return topSystem;
	}

	/**
	 * Gets the name of this application.
	 *
	 * @return the name of this application.
	 */
	public static String getAppName()
	{
		return appName;
	}

	/**
	 * Gets the major version number. If the version is 1.23 then major version is 1.
	 *
	 * @return the major version number.
	 */
	public static short getAppVersionMajor()
	{
		return appVersionMajor;
	}

	/**
	 * Gets the minor version number. If the version is 1.23 then minor version is 2.
	 *
	 * @return the minor version number.
	 */
	public static short getAppVersionMinor()
	{
		return appVersionMinor;
	}

	/**
	 * Gets the micro version number. If the version is 1.23 then micro version is 3.
	 *
	 * @return the micro version number.
	 */
	public static short getAppVersionMicro()
	{
		return appVersionMicro;
	}

	/**
	 * Gets the type of this build (e.g. Debug or Release).
	 *
	 * @return the type of this build.
	 */
	public static String getBuildType()
	{
		return buildType;
	}
   
   /**
    * Gets the build id (a string on the format 200409090816).
    *
    * @return the build number.
    * 
    * @since 2.0.1
    */
   public static String getBuildId()
   {
      return buildId;
   }

	/**
	 * Gets a string containing application (JServer) and version information.
	 *
	 * @return a string containing version information.
	 */
	public static String getVersionString()
	{
		return versionString;
	}
   
   /**
    * Gets a string containing (JServer) version information.
    *
    * @return a string containing version information.
    * 
    * @since 2.1 (20050610)
    */
   public static String getVersionOnlyString()
   {
      return versionString;
   }

	/**
	 * Gets version information about this server (not the JServer version!).
	 *
	 * @return a string containing version information about this server.
	 */
	public String getServerVersion()
	{
		return serverVersion.stringValue();
	}

	/**
	 * Sets version information about this server (not the JServer version!).
	 *
	 * @param serverVer a string containing version information about this server.
	 */
	public void setServerVersion(String serverVer)
	{
		serverVersion.setValue(serverVer);
      logInfo("Server version: " + serverVer + ".");
	}
	
	/**
	 * Gets the value of the property <code>javaVMInfo</code>, which contains information 
	 * on the current Java VM.
	 * 
	 * @return a string containing information on the current Java VM.
	 */
	public String getJavaVMInfo()
	{
		return this.javaVMInfo.stringValue();
	}

	/**
	 * Validates a modification of a properties owned by this JServer object.
	 *
	 * @param property The property to be validated.
	 *
	 * @return boolean value indicating if the property passed (true) validation or not (false).
	 *
	 * @see com.teletalk.jserver.property.PropertyOwner
	 */
	public boolean validatePropertyModification(Property property)
	{
		if(property ==	checkInterval)
      {
			return checkInterval.longValue() > 1000;
      }
		else return super.validatePropertyModification(property);
	}
   
   /**
    * Sets the name of this JServer. This name may only be set before the JServer has started.
    */
   public void setName(final String newName)
   {
      super.rename(newName);
   }

	/**
	 * Returns every SubSystem and SubComponent in the server including this
	 * object.
	 *
	 * @return List containing SubSystems and SubComponents.
	 */
	public final List getSystemTree()
	{
		ArrayList tree = new ArrayList();

		tree.add(this);

      tree.addAll(super.getSubComponentTree());

		return tree;
	}

	/**
	 * Method to check if the whole server is currently shutting down.
	 *
	 * @return true if the whole server is shutting down, otherwise false.
	 */
	public static boolean isServerShuttingDown()
	{
		return serverIsShuttingDown;
	}

	/**
	 * Convenience method to start this JServer and engage all its subsystems that need engaging.<br>
	 * <br>
	 * <b>Note:</b> This method is asynchronous - It only initiates the server start and returns before it is completed (i.e. 
	 * it is executed in another thread).
	 */
	public void startJServer()
	{
		engage();
	}

   /**
    * Stop (and destroys) this JServer and all its subsystems. This method is asynchronous - it only initiates the shut down and returns 
    * before it is completed (i.e. it is executed in another thread).<br>
    * <br>
    * This method uses the default maximum shut down time ({@link #getMaxShutDownWaitTime()}).
    */
   public void stopJServer()
   {
      stopJServer(false, this.maxShutDownWaitTime);
   }
   
	/**
	 * Stop (and destroys) this JServer and all its subsystems. This method is asynchronous - it only initiates the shut down and returns 
    * before it is completed (i.e. it is executed in another thread).
    * 
    * @since 2.1.2
	 */
	public void stopJServer(final long maximumShutDownWaitTimeout)
	{
		stopJServer(false, maximumShutDownWaitTimeout);
	}
		
	/**
	 * Stop (and destroys) this JServer and all its subsystems. This method is asynchronous - it only initiates the shut down and returns 
    * before it is completed (i.e. it is executed in another thread). When the shut down process if complete,
    * the server process is terminated using System.exit(0), if the parameter <code>exitProcess</code> is set to <code>true</code>.<br>
    * <br>
    * This method uses the default maximum shut down time ({@link #getMaxShutDownWaitTime()}).
	 */
   public void stopJServer(final boolean exitProcess)
   {
      stopJServer(exitProcess, this.maxShutDownWaitTime);
   }
   
   /**
    * Stop (and destroys) this JServer and all its subsystems. This method is asynchronous - it only initiates the shut down and returns 
    * before it is completed (i.e. it is executed in another thread). When the shut down process if complete,
    * the server process is terminated using System.exit(0), if the parameter <code>exitProcess</code> is set to <code>true</code>.
    * 
    * @since 2.1.2
    */
	public void stopJServer(final boolean exitProcess, final long maximumShutDownWaitTimeout)
	{
		logInfo("Stopping server!");
      
		serverIsShuttingDown = true;

		final Thread shutDownThread = new Thread("JServer shutdown thread")
		{
			public void run()
			{
            destroyJServerInternal(exitProcess, maximumShutDownWaitTimeout);
			}
		};
		
      shutDownThread.start();
	}
   
   /**
    * Internal method to stop and destroy JServer.
    */
   void destroyJServerInternal(final boolean exitProcess, final long maximumShutDownWaitTimeout)
   {
      shutDown();
      try
      {
         // Wait for shut down

         waitForDown(maximumShutDownWaitTimeout);
      }
      catch(InterruptedException e){}

      JServer.this.destroy();
      System.runFinalization();
      System.gc();
      if(exitProcess) System.exit(0);
   }
   
   /**
    * Stop (and destroys) this JServer and all its subsystems. This method is a synchronous alternative to {@link #stopJServer()}.
    * 
    * @since 2.1.2 (20060310)
    */
   public void destroyJServer()
   {
      this.destroyJServer(false, this.maxShutDownWaitTime);
   }
   
   /**
    * Stop (and destroys) this JServer and all its subsystems. This method is a synchronous alternative to {@link #stopJServer(long)}.
    * 
    * @since 2.1.2 (20060310)
    */
   public void destroyJServer(final long maximumShutDownWaitTimeout)
   {
      this.destroyJServer(false, maximumShutDownWaitTimeout);
   }
   
   /**
    * Stop (and destroys) this JServer and all its subsystems. This method is a synchronous alternative to {@link #stopJServer(boolean)}.
    * 
    * @since 2.1.2 (20060310)
    */
   public void destroyJServer(final boolean exitProcess)
   {
      this.destroyJServer(exitProcess, this.maxShutDownWaitTime);
   }
   
   /**
    * Stop (and destroys) this JServer and all its subsystems. This method is a synchronous alternative to {@link #stopJServer(boolean, long)}.
    * 
    * @since 2.1.2 (20060310)
    */
   public void destroyJServer(final boolean exitProcess, final long maximumShutDownWaitTimeout)
   {
      logInfo("Stopping server!");

      serverIsShuttingDown = true;
      
      this.destroyJServerInternal(exitProcess, maximumShutDownWaitTimeout);
   }
		
	/**
	 * Method to restart this JServer. If a JServerLauncher has been used to start this server, this method will invoke the server restart 
	 * method in that object. If a JServerLauncher is not used the server is restarted using the reinitialize method.
	 */
	public void restartJServer()
	{
		if(JServer.launcherRestartMethod != null)
		{
			logInfo("Restarting and reloading server using JServerLauncher!");
			
			final Thread fireAndForget = new Thread("JServer shutdown thread")
			{
				public void run()
				{
					shutDown();

					try
					{
						// Wait for shut down
						waitForDown(JServer.this.maxShutDownWaitTime);
					}
					catch(InterruptedException e){}

					JServer.this.destroy();
					System.runFinalization();
					System.gc();

					try
					{
						launcherRestartMethod.invoke(null, null);
					}
					catch(Exception e) 
					{
						logError("Error occurred during restart/reload! Aborting restart - terminating server.", e);
						System.out.println("Error occurred during restart/reload! Aborting restart - terminating server.");
						e.printStackTrace();
					}
				}
			};

			fireAndForget.setDaemon(true);
			fireAndForget.start();
		}
		else
		{
			logInfo("Restarting server!");
			reinitialize();
			System.runFinalization();
			System.gc();
			logInfo("Server restarted!");
		}
	}
	
	/**
	 * Method to kill this JServer without performing any shutdown logic.
	 */
	public void killJServer()
	{
		System.exit(0);
	}
   
   /**
    * Gets the maximum shut down wait time.
    * 
    * @since 2.1.2 (20060221)
    */
   public long getMaxShutDownWaitTime()
   {
      return maxShutDownWaitTime;
   }

   /**
    * Sets the maximum shut down wait time.
    * 
    * @since 2.1.2 (20060221)
    */
   public void setMaxShutDownWaitTime(long maxShutDownWaitTime)
   {
      this.maxShutDownWaitTime = maxShutDownWaitTime;
   }

	/**
	 * Destroys this JServer. This method is used to destroy this JServer when
	 * shutting down the server. Applications should never call this method directly.
	 */
	protected void destroy()
	{
		synchronized(componentLock)
		{
			super.destroy();

			try
			{
				Thread.sleep(500);
				threadGroup.destroy();
			}catch(Exception e){}

			topSystem = null;

			this.rmiManager = null;
         this.periodicActionManager = null;
         this.loadManager = null;
         this.statisticsManager = null;
         this.defaultFileLogger = null;
         this.defaultCriticalErrorLogger = null;

			try
			{
				java.rmi.server.UnicastRemoteObject.unexportObject(this.administration, true);
			}
         catch(Exception e){}
		
			this.administration = null;
			this.mainHttpServer = null;

			this.threadGroup = null;

			this.activeThreadCount = null;
			this.jServerVersion = null;
			this.checkInterval = null;
			this.currentDirectory = null;
		}
	}

	/**
	 * Initializes this JServer and all its child SubSystems.
	 */
	protected void doInitialize()
	{
      super.doInitialize();
      
		if(!this.isReinitializingDueToError()) // Only start everything up if not reinitializing due to an error
		{
			List subSysCopy = this.getSubSystems();
			
			// Engage subsystems
			for(int i=0; i<subSysCopy.size(); i++)
			{
				SubSystem subsys = (SubSystem)subSysCopy.get(i);
				if( (subsys != null) && (subsys.getStatus() != SubSystem.INITIALIZING) && (subsys.getStatus() != SubSystem.ENABLED) )
				{
					subsys.engage();
					if(super.isDebugMode()) logDebug("Engaging " + subsys);
				}
			}
			
			// Wait for subsystems to engage
			for(int i=0; i<subSysCopy.size(); i++)
			{
				SubSystem subsys = (SubSystem)subSysCopy.get(i);

				if( (subsys != null) && ((subsys.getStatus() == SubSystem.CREATED) || (subsys.getStatus() == SubSystem.INITIALIZING)) )
				{
					try
					{
						// Wait max 5 seconds
						subsys.waitForEnabled(5*1000);
					}
					catch(InterruptedException e){}
				}
			}

			if(super.isDebugMode()) logDebug("JServer engaged!");
		}
      
      this.instantiateDynamicComponents();
	}

	/**
	 * Shuts down this JServer and all its children (first level) SubSystems.
	 */
	protected void doShutDown()
	{
		if(!this.isReinitializingDueToError()) // Only shut down everything if not reinitializing due to an error
		{
			final long shutDownBeginTime = System.currentTimeMillis();
			
			List subComponentsCopy = new ArrayList(this.getSubComponents());
			
			if( this.isDebugMode() ) this.logDebug("Shutting down top level components.");

         SubComponent component;
         
			// Shut down top level components
			for(int i=0; i<subComponentsCopy.size(); i++)
			{
            component = (SubComponent)subComponentsCopy.get(i);

				if( (component != null) && (component.getStatus() != JServerConstants.SHUTTING_DOWN) && (component.getStatus() != JServerConstants.DOWN) && (component != logManager) )
				{
					try
					{
                  component.shutDown();
					}
					catch(Throwable t)
					{
						logWarning("Error occurred while shutting down component + " + component.getFullName() + " during server shut down!", t);
					}
				}
			}
			boolean abortWaitForDown = false;
			
         if( this.isDebugMode() ) this.logDebug("Waiting for top level components to shut down.");
         
			// Wait for top level components to shut down
			for(int i=0; !abortWaitForDown && (i<subComponentsCopy.size()); i++)
			{
            component = (SubComponent)subComponentsCopy.get(i);
            if( this.isDebugMode() ) this.logDebug("Waiting for " + component + " to shut down (current status: " + JServerConstants.statusNames[component.getStatus()] + ").");

				if( (component != null) && (component.getStatus() != JServerConstants.DOWN) && (component != logManager) )
				{
					try
					{
						while( !abortWaitForDown && !component.waitForDown(30*1000) )
						{
							if( (System.currentTimeMillis() - shutDownBeginTime) > (this.maxShutDownWaitTime - 10*1000) )
							{
                        if( this.isDebugMode() ) this.logDebug("Aborting top level components shutdown wait due to timeout.");
								abortWaitForDown = true;
								break;
							}
						}
						
						if(abortWaitForDown) break;
					}
               catch(Throwable t)
               {
                  logWarning("Error occurred while waining for component + " + component.getFullName() + " to shut down during server shut down!", t);
               }
				}
			}
         
         if( this.isDebugMode() ) this.logDebug("Done waiting for top level components to shut down.");
			
         try{
         Thread.sleep(100);
         }catch (Exception e){}
         
			// Shut down logmanager last
			if((logManager != null) && (logManager.getStatus() != DOWN))
			{
				logManager.shutDown();
				try
				{
					// Wait a maximum 60 seconds for the log manager to shut down
					logManager.waitForDown(60*1000);
				}
				catch(InterruptedException e){}
			}
		}
		
		super.doShutDown();
	}

	/**
	 * Sets the flag indicating if the server process should be terminated when it runs out of memory. 
	 * The default value of this flag is <code>true</code>. 
	 * 
	 * @param killProcessOnOutOfMemoryError the new value of the flag.
	 */
	public void setKillProcessOnOutOfMemoryError(boolean killProcessOnOutOfMemoryError)
	{
		this.killProcessOnOutOfMemoryError = killProcessOnOutOfMemoryError;
	}
	
	/**
	 * Gets the flag indicating if the server process should be terminated when it runs out of memory. 
	 * The default value of this flag is <code>true</code>. 
	 * 
	 * @return the value of the flag.
	 */
	public boolean getKillProcessOnOutOfMemoryError()
	{
		return this.killProcessOnOutOfMemoryError;
	}

	/**
	 * Sets the time for the last restart.
	 *
	 * @param lastRestart the last restart.
	 */
	final void setLastRestart(long lastRestart)
	{
		this.lastRestart = lastRestart;
	}

	/**
	 * Sets the interval in milliseconds between two consecutive SubSystem checks.
	 *
	 * @param checkInterval the interval.
	 */
	public void setCheckInterval(long checkInterval)
	{
		this.checkInterval.setValue(checkInterval);
	}
	
	/**
	 * Gets the default file logger, which was created in the constructor of this JServer 
	 * object (if the appropriate constructor and parameters was used).
	 * 
	 * @return the default file logger or <code>null</code> if none existed.
	 */
	public FileAppenderComponent getDefaultFileLogger()
	{
		return this.defaultFileLogger;
	}
   
   /**
    * Sets the default file logger.
    * 
    * @since 2.1
    */
   public void setDefaultFileLogger(FileAppenderComponent fileAppenderComponent)
   {
      if( this.defaultFileLogger != null )
      {
         this.logManager.removeAppender(this.defaultFileLogger);
      }
         
      this.defaultFileLogger = fileAppenderComponent;
      logManager.addAppender(this.defaultFileLogger);
   }
   
   /**
    * Creates and sets the default file logger.
    * 
    * @since 2.1
    */
   public FileAppenderComponent createDefaultFileLogger()
   {
      this.setDefaultFileLogger(new FileAppenderComponent(this.logManager));
      return this.getDefaultFileLogger();
   }
   
   /**
    * Creates and sets the default file logger.
    * 
    * @since 2.1
    */
   public FileAppenderComponent createDefaultFileLogger(final String name)
   {
      this.setDefaultFileLogger(new FileAppenderComponent(this.logManager, name));
      return this.getDefaultFileLogger();
   }
   	
	/**
	 * Gets the default critical error logger, which was created in the constructor of this JServer 
	 * object (if the appropriate constructor and parameters was used).
	 * 
	 * @return the default critical error logger or <code>null</code> if none existed.
	 */
	public CriticalErrorAppenderComponent getDefaultCriticalErrorLogger()
	{
		return this.defaultCriticalErrorLogger;
	}
   
   /**
    * Sets the default critical error logger.
    * 
    * @since 2.1
    */
   public void setDefaultCriticalErrorLogger(CriticalErrorAppenderComponent criticalErrorAppenderComponent)
   {
      if( this.defaultCriticalErrorLogger != null )
      {
         this.logManager.removeAppender(this.defaultCriticalErrorLogger);
      }
      
      this.defaultCriticalErrorLogger = criticalErrorAppenderComponent;
      logManager.addAppender(this.defaultCriticalErrorLogger);
   }
   
   /**
    * Creates and sets the default critical error logger.
    * 
    * @since 2.1
    */
   public CriticalErrorAppenderComponent createDefaultCriticalErrorLogger()
   {
      this.setDefaultCriticalErrorLogger(new CriticalErrorAppenderComponent(this.logManager));
      return this.getDefaultCriticalErrorLogger();
   }
   
   /**
    * Creates and sets the default critical error logger.
    * 
    * @since 2.1
    */
   public CriticalErrorAppenderComponent createDefaultCriticalErrorLogger(final String name)
   {
      this.setDefaultCriticalErrorLogger(new CriticalErrorAppenderComponent(this.logManager, name));
      return this.getDefaultCriticalErrorLogger();
   }

	/**
	 * Returns the TopThreadGroup.
	 *
	 * @return a ThreadGroup object.
	 */
	public ThreadGroup getThreadGroup()
	{
		return threadGroup;
	}

	/**
    * Gets the name components from a full name.
	 *
    * @since 2.1 
	 */
	public static String[] parseNames(final String fullName)
	{
		StringTokenizer t = new StringTokenizer(fullName, ".");
		String[] names = new String[t.countTokens()];

		for(int i=0; t.hasMoreTokens(); i++)
		{
			names[i] = t.nextToken();
		}

		return names;
	}
   
   /**
    * Converts a full name of a component to a normalized form, i.e. replaces the server name with "JServer".
    * For example the full name "MyServer.MySubsystem" would be converted to  "JServer.MySubsystem".
    * 
    * @since 2.0
    */
   public String normalizeFullName(final String fullName)
   {
      return normalizeFullName(parseNames(fullName));
   }
   
   /**
    * Converts a full name of a component to a normalized form, i.e. replaces the server name with "JServer".
    * For example the full name "MyServer.MySubsystem" would be converted to  "JServer.MySubsystem".
    * 
    * @since 2.1
    */
   public String normalizeFullName(final String[] nameComponents)
   {
      if(nameComponents.length > 0)
      {
         if(nameComponents[0].equals(this.getName()) )  // If top system...
         {
            nameComponents[0] = JServerConstants.JSERVER_TOP_SYSTEM_ALIAS; //...replace the servername with the standard top system name (JServer)
         }

         StringBuffer convertedName = new StringBuffer();
         for(int i=0; i<nameComponents.length; i++)
         {
            convertedName.append(nameComponents[i]);
            if( i < (nameComponents.length - 1) )
            {
               convertedName.append(".");
            }
         }

         return convertedName.toString();
      }
      else
         return null;
   }

	/**
	 * Performs a search for a SubSystem.
	 *
	 * @param fullName the name of the SubSystem.
	 *
	 * @return a SubSystem or null if none was found.
	 */
	public SubSystem findSubSystem(final String fullName)
	{
		SubSystem subsys = null;
		SubSystem parentSystem = this;
		String[] names = parseNames(fullName);

		if(names.length > 1)
		{
			for(int i=1; i<names.length; i++)
			{
				subsys = parentSystem.getSubSystem(names[i]);

				if(subsys == null) break;

				parentSystem = subsys;
			}
		}
		else if( (names.length > 0) && (fullName.equals(getName())) )
      {
			subsys = this;
      }

		return subsys;
	}
   
   /**
    * Performs a search for a SubComponent.
    *
    * @param fullName the name of the SubComponent.
    *
    * @return a SubComponent or null if none was found.
    */
   public SubComponent findSubComponent(final String fullName)
   {
      return findSubComponent(parseNames(fullName));
   }
      
   /**
    * Performs a search for a SubComponent.
    * 
    * @return a SubComponent or null if none was found.
    * 
    * @since 2.1
    */
   public SubComponent findSubComponent(final String[] nameComponents)
   {
      return findSubComponent(nameComponents, nameComponents.length);
   }

	/**
	 * Performs a search for a SubComponent.
	 *
	 * @return a SubComponent or null if none was found.
    * 
    * @since 2.1
	 */
	public SubComponent findSubComponent(final String[] nameComponents, final int length)
	{
		SubComponent subC = null;
		SubComponent parentComponent = this;
		
      if( length > 1 )
      {
   		for(int i=1; i<length; i++)
   		{
   			subC = parentComponent.getSubComponent(nameComponents[i]);
            
   			if(subC == null) break;
   
   			parentComponent = subC;
   		}
      }
      else if( (length == 1) && (JSERVER_TOP_SYSTEM_ALIAS.equals(nameComponents[0]) || this.getName().equals(nameComponents[0])) ) subC = this;

		return subC;
	}

	/**
	 * Performs a search for a Property.
	 *
	 * @param fullName the name of the Property.
	 *
	 * @return a Property or null if none was found.
	 */
	public Property findProperty(final String fullName)
	{
		Property property = null;
      SubComponent subC = null;
      SubComponent parentComponent = this;
      String[] names = parseNames(fullName);

      if (names.length > 1)
      {
         int i = 1;
         for (; i < (names.length - 1); i++)
         {
            subC = null;

            if (parentComponent instanceof SubSystem) subC = ((SubSystem) parentComponent).getSubSystem(names[i]);
            if (subC == null) subC = parentComponent.getSubComponent(names[i]);

            parentComponent = subC;

            if (subC == null) break;
         }

         if ((parentComponent != null) && (i == (names.length - 1))) property = parentComponent.getProperty(names[names.length - 1]);
      }

      return property;
	}

	/**
	 * Returns the EventQueue of this server.
	 *
	 * @return a EventQueue object.
	 *
	 * @see com.teletalk.jserver.event.EventQueue
	 */
	public EventQueue getEventQueue()
	{
		return this.eventQueue;
	}

	/**
	 * Returns the LogManager.
	 *
	 * @return a LogManager object.
	 *
	 * @see com.teletalk.jserver.log.LogManager
	 */
	public LogManager getLogManager()
	{
		return logManager;
	}

	/**
	 * Returns the PropertyManager.
	 *
	 * @return a PropertyManager object.
	 *
	 * @see com.teletalk.jserver.property.PropertyManager
	 */
	public PropertyManager getPropertyManager()
	{
		return propertyManager;
	}

	/**
	 * Returns the RmiManager.
	 *
	 * @return a RmiManager object.
	 *
	 * @see com.teletalk.jserver.rmi.RmiManager
	 */
	public RmiManager getRmiManager()
	{
		return rmiManager;
	}

	/**
	 * Sets the RmiManager. If there already is a RmiManager, it will be removed.
	 *
	 * @param rmiManager a RmiManager object.
	 *
	 * @see com.teletalk.jserver.rmi.RmiManager
	 */
	public void setRmiManager(RmiManager rmiManager)
	{
      if( this.isDebugMode() ) this.logDebug("Setting new RmiManager - " + rmiManager + ".");
      
      if( this.rmiManager == rmiManager ) return;
      
      if( this.rmiManager != null )
      {
			this.removeSubComponent(this.rmiManager, true);
      }

		this.rmiManager = rmiManager;

		addSubComponent(rmiManager, true);
	}

	/**
	 * Returns the Administration object.
	 *
	 * @return an Administration object.
	 *
	 * @see com.teletalk.jserver.rmi.Administration
	 */
	public Administration getAdministration()
	{
		return administration;
	}

	/**
	 * Sets the Administration object.
	 *
	 * @param administration an Administration object.
	 *
	 * @see com.teletalk.jserver.rmi.Administration
	 */
	public void setAdministration(final Administration administration)
	{
		this.administration = administration;
	}

	/**
	 * Sets the main Http Server, which among other things is responsible for loading files over the network to
	 * the administration tool. This JServer will assume ownership of the specified HttpServer, rename it and finally
	 * ininialize its properties from persistent storage. If there already is a main Http server, it will be removed.
	 *
	 * @param mainHttpServer a new main Http Server.
	 *
	 * @see com.teletalk.jserver.tcp.http.HttpServer
	 */
	public void setMainHttpServer(final HttpServer mainHttpServer)
	{
      if( this.isDebugMode() ) this.logDebug("Setting new MainHttpServer - " + mainHttpServer + ".");
      
      if( this.mainHttpServer == mainHttpServer ) return;
      
		if( this.mainHttpServer != null )
      {
         super.removeSubComponent(this.mainHttpServer, true);
      }

		this.mainHttpServer = mainHttpServer;

      super.addSubComponent(mainHttpServer);
		mainHttpServer.initProperties();
		//addJServerMetaData(HttpServer.JServerMetaDataKey_MainHttpServerPort, new Integer(mainHttpServer.getLocalPort()));
	}

	/**
	 * Sets the main Http Server, which among other things is responsible for loading files over the network to
	 * the administration tool.
	 *
	 * @return the main Http Server.
	 *
	 * @see com.teletalk.jserver.tcp.http.HttpServer
	 */
	public HttpServer getMainHttpServer()
	{
		return this.mainHttpServer;
	}
   
   /**
    * Returns the PeriodicActionManager.
    *
    * @return a PeriodicActionManager object.
    */
   public PeriodicActionManager getPeriodicActionManager()
   {
      return this.periodicActionManager;
   }

   /**
    * Sets the PeriodicActionManager. If there already is a PeriodicActionManager, it will be removed.
    *
    * @param periodicActionManager a PeriodicActionManager object.
    */
   public void setPeriodicActionManager(final PeriodicActionManager periodicActionManager)
   {
      if( this.isDebugMode() ) this.logDebug("Setting new PeriodicActionManager - " + periodicActionManager + ".");
      
      if( this.periodicActionManager == periodicActionManager ) return;
      
      if( this.periodicActionManager != null )
      {
         super.removeSubComponent(this.periodicActionManager, true);
      }
      
      this.periodicActionManager = periodicActionManager;

      super.addSubComponent(periodicActionManager, true);
   }
   
   /**
    * Returns the LoadManager.
    *
    * @return a LoadManager object.
    * 
    * @since 2.1
    */
   public LoadManager getLoadManager()
   {
      return this.loadManager;
   }
   
   /**
    * Sets the LoadManager. If there already is a LoadManager, it will be removed.
    *
    * @param loadManager a PeriodicActionManager object.
    * 
    * @since 2.1
    */
   public void setLoadManager(final LoadManager loadManager)
   {
      if( this.isDebugMode() ) this.logDebug("Setting new LoadManager - " + loadManager + ".");
      
      if( this.loadManager == loadManager ) return;
      
      if( this.loadManager != null )
      {
         super.removeSubComponent(this.loadManager, true);
      }
      
      this.loadManager = loadManager;

      super.addSubComponent(loadManager, true);
   }
   
   /**
    * Returns the StatisticsManager.
    *
    * @return a StatisticsManager object.
    * 
    * @since 2.1
    */
   public StatisticsManager getStatisticsManager()
   {
      return this.statisticsManager;
   }
   
   /**
    * Sets the StatisticsManager. If there already is a StatisticsManager, it will be removed.
    *
    * @param statisticsManager a StatisticsManager object.
    * 
    * @since 2.1
    */
   public void setStatisticsManager(final StatisticsManager statisticsManager)
   {
      if( this.isDebugMode() ) this.logDebug("Setting new StatisticsManager - " + statisticsManager + ".");
      
      if( this.statisticsManager == statisticsManager ) return;
      
      if( this.statisticsManager != null )
      {
         super.removeSubComponent(this.statisticsManager, true);
      }
      
      this.statisticsManager = statisticsManager;

      super.addSubComponent(statisticsManager, true);
   }
   
   /**
    * Returns the SpringApplicationContext.
    *
    * @return a SpringApplicationContext object.
    * 
    * @since 2.1
    */
   public SpringApplicationContext getSpringApplicationContext()
   {
      return this.springApplicationContext;
   }
   
   /**
    * Sets the SpringApplicationContext. If there already is a SpringApplicationContext, it will be removed.
    *
    * @param springApplicationContext a SpringApplicationContext object.
    * 
    * @since 2.1
    */
   public void setSpringApplicationContext(final SpringApplicationContext springApplicationContext)
   {
      if( this.isDebugMode() ) this.logDebug("Setting new SpringApplicationContext - " + springApplicationContext + ".");
      
      if( this.springApplicationContext == springApplicationContext ) return;
      
      if( this.springApplicationContext != null )
      {
         super.removeSubComponent(this.springApplicationContext, true);
      }
      
      this.springApplicationContext = springApplicationContext;

      super.addSubComponent(springApplicationContext, true);
   }
   
   /**
    * Returns the SnsClient.
    *
    * @return a SnsClient object.
    * 
    * @since 2.1 (20050503)
    */
   public SnsClientManager getSnsClientManager()
   {
      return this.snsClientManager;
   }
   
   /**
    * Sets the SnsClient. If there already is a SnsClient, it will be removed.
    *
    * @param snsClient a SnsClient object.
    * 
    * @since 2.1 (20050503)
    */
   public void setSnsClientManager(final SnsClientManager snsClient)
   {
      if( this.isDebugMode() ) this.logDebug("Setting new SnsClientManager - " + snsClient + ".");
      
      if( this.snsClientManager == snsClient ) return;
      
      if( this.snsClientManager != null )
      {
         super.removeSubComponent(this.snsClientManager, true);
      }
      
      this.snsClientManager = snsClient;

      super.addSubComponent(snsClient, true);
   }
   
   /**
    * Returns the SnsServer.
    *
    * @return a SnsServer object.
    * 
    * @since 2.1 (20050503)
    */
   public SnsManager getSnsManager()
   {
      return this.snsManager;
   }
   
   /**
    * Sets the SnsServer. If there already is a SnsServer, it will be removed.
    *
    * @param snsServer a SnsServer object.
    * 
    * @since 2.1 (20050503)
    */
   public void setSnsManager(final SnsManager snsServer)
   {
      if( this.isDebugMode() ) this.logDebug("Setting new SnsManager - " + snsServer + ".");
      
      if( this.snsManager == snsServer ) return;
      
      if( this.snsManager != null )
      {
         super.removeSubComponent(this.snsManager, true);
      }
      
      this.snsManager = snsServer;

      super.addSubComponent(snsServer, true);
   }

	/**
	 * Convenience method to create and add a HTTP server bound to the local host, using DefaultHttpConnection as request handler. The created HTTP server will NOT
	 * be engaged as a result of this method call.
	 *
	 * @param port the port on which the HTTP server will listen to.
	 *
	 * @return the created HTTP server.
	 *
	 * @see com.teletalk.jserver.tcp.http.HttpServer
	 * @see com.teletalk.jserver.tcp.http.DefaultHttpConnection
	 */
	public HttpServer createDefaultHttpServer(int port)
	{
		return createDefaultHttpServer(port, false);
	}

	/**
	 * Convenience method to create and add a HTTP server bound to the local host, using DefaultHttpConnection as request handler.
	 *
	 * @param port the port on which the HTTP server will listen to.
	 * @param engage boolean value indicating if the created HTTP server should be engaged.
	 *
	 * @return the created HTTP server.
	 *
	 * @see com.teletalk.jserver.tcp.http.HttpServer
	 * @see com.teletalk.jserver.tcp.http.DefaultHttpConnection
	 */
	public HttpServer createDefaultHttpServer(int port, boolean engage)
	{
		HttpServer httpServer = HttpServer.createDefaultHttpServer(this, port);

		addSubSystem(httpServer, engage);

		return httpServer;
	}

	/**
	 * Convenience method to create and add a HTTP server using DefaultHttpConnection as request handler. The created HTTP server will NOT
	 * be engaged as a result of this method call.
	 *
	 * @param port the port on which the HTTP server will listen to.
	 * @param localIPAddress the local address to which the HTTP server will be bound.
	 *
	 * @return the created HTTP server.
	 *
	 * @see com.teletalk.jserver.tcp.http.HttpServer
	 * @see com.teletalk.jserver.tcp.http.DefaultHttpConnection
	 */
	public HttpServer createDefaultHttpServer(int port, String localIPAddress)
	{
		return createDefaultHttpServer(port, localIPAddress, false);
	}

	/**
	 * Convenience method to create and add a HTTP server using DefaultHttpConnection as request handler.
	 *
	 * @param port the port on which the HTTP server will listen to.
	 * @param localIPAddress the local address to which the HTTP server will be bound.
	 * @param engage boolean value indicating if the created HTTP server should be engaged.
	 *
	 * @return the created HTTP server.
	 *
	 * @see com.teletalk.jserver.tcp.http.HttpServer
	 * @see com.teletalk.jserver.tcp.http.DefaultHttpConnection
	 */
	public HttpServer createDefaultHttpServer(int port, String localIPAddress, boolean engage)
	{
		HttpServer httpServer = HttpServer.createDefaultHttpServer(this, port, localIPAddress);

		addSubSystem(httpServer, engage);

		return httpServer;
	}

	/**
	 * Convenience method to create and add a HTTP server using DefaultHttpConnection as request handler. The created HTTP server will NOT
	 * be engaged as a result of this method call.
	 *
	 * @param name the name of the HTTP server.
	 * @param port the port on which the HTTP server will listen to.
	 * @param localIPAddress the local address to which the HTTP server will be bound.
	 *
	 * @return the created HTTP server.
	 *
	 * @see com.teletalk.jserver.tcp.http.HttpServer
	 * @see com.teletalk.jserver.tcp.http.DefaultHttpConnection
	 */
	public HttpServer createDefaultHttpServer(String name, int port, String localIPAddress)
	{
		return createDefaultHttpServer(name, port, localIPAddress, false);
	}

	/**
	 * Convenience method to create and add a HTTP server using DefaultHttpConnection as request handler.
	 *
	 * @param name the name of the HTTP server.
	 * @param port the port on which the HTTP server will listen to.
	 * @param localIPAddress the local address to which the HTTP server will be bound.
	 * @param engage boolean value indicating if the created HTTP server should be engaged.
	 *
	 * @return the created HTTP server.
	 *
	 * @see com.teletalk.jserver.tcp.http.HttpServer
	 * @see com.teletalk.jserver.tcp.http.DefaultHttpConnection
	 */
	public HttpServer createDefaultHttpServer(String name, int port, String localIPAddress, boolean engage)
	{
		HttpServer httpServer = HttpServer.createDefaultHttpServer(this, name, port, localIPAddress); 

		addSubSystem(httpServer, engage);

		return httpServer;
	}

	/**
	 * Convenience method to create and add a HTTP server bound to the local host. The created HTTP server will NOT
	 * be engaged as a result of this method call.
	 *
	 * @param port the port on which the HTTP server will listen to.
	 * @param requestClass the requesthandler class.
	 *
	 * @return the created HTTP server.
	 *
	 * @see com.teletalk.jserver.tcp.http.HttpServer
	 */
	public HttpServer createHttpServer(int port, Class requestClass)
	{
		return createHttpServer(port, requestClass, false);
	}

	/**
	 * Convenience method to create and add a HTTP server bound to the local host. The created HTTP server will NOT
	 * be engaged as a result of this method call.
	 *
	 * @param port the port on which the HTTP server will listen to.
	 * @param requestClass the requesthandler class.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 *
	 * @return the created HTTP server.
	 *
	 * @see com.teletalk.jserver.tcp.http.HttpServer
	 */
	public HttpServer createHttpServer(int port, Class requestClass, Object[] requestClassCreationParams)
	{
		return createHttpServer(port, requestClass, requestClassCreationParams, false);
	}

	/**
	 * Convenience method to create and add a HTTP server bound to the local host.
	 *
	 * @param port the port on which the HTTP server will listen to.
	 * @param requestClass the requesthandler class.
	 * @param engage boolean value indicating if the created HTTP server should be engaged.
	 *
	 * @return the created HTTP server.
	 *
	 * @see com.teletalk.jserver.tcp.http.HttpServer
	 */
	public HttpServer createHttpServer(int port, Class requestClass, boolean engage)
	{
		HttpServer httpServer = new HttpServer(this, requestClass, port);

		addSubSystem(httpServer, engage);

		return httpServer;
	}

	/**
	 * Convenience method to create and add a HTTP server bound to the local host.
	 *
	 * @param port the port on which the HTTP server will listen to.
	 * @param requestClass the requesthandler class.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 * @param engage boolean value indicating if the created HTTP server should be engaged.
	 *
	 * @return the created HTTP server.
	 *
	 * @see com.teletalk.jserver.tcp.http.HttpServer
	 */
	public HttpServer createHttpServer(int port, Class requestClass, Object[] requestClassCreationParams, boolean engage)
	{
		HttpServer httpServer = new HttpServer(this, requestClass, requestClassCreationParams, port);

		addSubSystem(httpServer, engage);

		return httpServer;
	}

	/**
	 * Convenience method to create and add  a HTTP server. The created HTTP server will NOT
	 * be engaged as a result of this method call.
	 *
	 * @param port the port on which the HTTP server will listen to.
	 * @param localIPAddress the local address to which the HTTP server will be bound.
	 * @param requestClass the requesthandler class.
	 *
	 * @return the created HTTP server.
	 *
	 * @see com.teletalk.jserver.tcp.http.HttpServer
	 */
	public HttpServer createHttpServer(int port, String localIPAddress, Class requestClass)
	{
		return createHttpServer(port, localIPAddress, requestClass, false);
	}

	/**
	 * Convenience method to create and add  a HTTP server. The created HTTP server will NOT
	 * be engaged as a result of this method call.
	 *
	 * @param port the port on which the HTTP server will listen to.
	 * @param localIPAddress the local address to which the HTTP server will be bound.
	 * @param requestClass the requesthandler class.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 *
	 * @return the created HTTP server.
	 *
	 * @see com.teletalk.jserver.tcp.http.HttpServer
	 */
	public HttpServer createHttpServer(int port, String localIPAddress, Class requestClass, Object[] requestClassCreationParams)
	{
		return createHttpServer(port, localIPAddress, requestClass, requestClassCreationParams, false);
	}

	/**
	 * Convenience method to create and add  a HTTP server.
	 *
	 * @param port the port on which the HTTP server will listen to.
	 * @param localIPAddress the local address to which the HTTP server will be bound.
	 * @param requestClass the requesthandler class.
	 * @param engage boolean value indicating if the created HTTP server should be engaged.
	 *
	 * @return the created HTTP server.
	 *
	 * @see com.teletalk.jserver.tcp.http.HttpServer
	 */
	public HttpServer createHttpServer(int port, String localIPAddress, Class requestClass, boolean engage)
	{
		HttpServer httpServer = new HttpServer(this, requestClass, port, localIPAddress);

		addSubSystem(httpServer, engage);

		return httpServer;
	}

	/**
	 * Convenience method to create and add  a HTTP server.
	 *
	 * @param port the port on which the HTTP server will listen to.
	 * @param localIPAddress the local address to which the HTTP server will be bound.
	 * @param requestClass the requesthandler class.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 * @param engage boolean value indicating if the created HTTP server should be engaged.
	 *
	 * @return the created HTTP server.
	 *
	 * @see com.teletalk.jserver.tcp.http.HttpServer
	 */
	public HttpServer createHttpServer(int port, String localIPAddress, Class requestClass, Object[] requestClassCreationParams, boolean engage)
	{
		HttpServer httpServer = new HttpServer(this, requestClass, requestClassCreationParams, port, localIPAddress);

		addSubSystem(httpServer, engage);

		return httpServer;
	}

	/**
	 * Convenience method to create and add  a HTTP server. The created HTTP server will NOT
	 * be engaged as a result of this method call.
	 *
	 * @param name the name of the HTTP server.
	 * @param port the port on which the HTTP server will listen to.
	 * @param localIPAddress the local address to which the HTTP server will be bound.
	 * @param requestClass the requesthandler class.
	 *
	 * @return the created HTTP server.
	 *
	 * @see com.teletalk.jserver.tcp.http.HttpServer
	 */
	public HttpServer createHttpServer(String name, int port, String localIPAddress, Class requestClass)
	{
		return createHttpServer(name, port, localIPAddress, requestClass, false);
	}

	/**
	 * Convenience method to create and add  a HTTP server. The created HTTP server will NOT
	 * be engaged as a result of this method call.
	 *
	 * @param name the name of the HTTP server.
	 * @param port the port on which the HTTP server will listen to.
	 * @param localIPAddress the local address to which the HTTP server will be bound.
	 * @param requestClass the requesthandler class.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 *
	 * @return the created HTTP server.
	 *
	 * @see com.teletalk.jserver.tcp.http.HttpServer
	 */
	public HttpServer createHttpServer(String name, int port, String localIPAddress, Class requestClass, Object[] requestClassCreationParams)
	{
		return createHttpServer(name, port, localIPAddress, requestClass, requestClassCreationParams, false);
	}

	/**
	 * Convenience method to create and add  a HTTP server.
	 *
	 * @param name the name of the HTTP server.
	 * @param port the port on which the HTTP server will listen to.
	 * @param localIPAddress the local address to which the HTTP server will be bound.
	 * @param requestClass the requesthandler class.
	 * @param engage boolean value indicating if the created HTTP server should be engaged.
	 *
	 * @return the created HTTP server.
	 *
	 * @see com.teletalk.jserver.tcp.http.HttpServer
	 */
	public HttpServer createHttpServer(String name, int port, String localIPAddress, Class requestClass, boolean engage)
	{
		HttpServer httpServer = new HttpServer(this, name, requestClass, port, localIPAddress);

		addSubSystem(httpServer, engage);

		return httpServer;
	}

	/**
	 * Convenience method to create and add  a HTTP server.
	 *
	 * @param name the name of the HTTP server.
	 * @param port the port on which the HTTP server will listen to.
	 * @param localIPAddress the local address to which the HTTP server will be bound.
	 * @param requestClass the requesthandler class.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 * @param engage boolean value indicating if the created HTTP server should be engaged.
	 *
	 * @return the created HTTP server.
	 *
	 * @see com.teletalk.jserver.tcp.http.HttpServer
	 */
	public HttpServer createHttpServer(String name, int port, String localIPAddress, Class requestClass, Object[] requestClassCreationParams, boolean engage)
	{
		HttpServer httpServer = new HttpServer(this, name, requestClass, requestClassCreationParams, port, localIPAddress);

		addSubSystem(httpServer, engage);

		return httpServer;
	}

	/**
	 * Convenience method to create and add a TCP server bound to the local host. The created TCP server will NOT
	 * be engaged as a result of this method call.
	 *
	 * @param port the port on which the TCP server will listen to.
	 * @param requestClass the requesthandler class.
	 *
	 * @return the created TCP server.
	 *
	 * @see com.teletalk.jserver.tcp.TcpServer
	 */
	public TcpServer createTcpServer(int port, Class requestClass)
	{
		return createTcpServer(port, requestClass, false);
	}

	/**
	 * Convenience method to create and add a TCP server bound to the local host. The created TCP server will NOT
	 * be engaged as a result of this method call.
	 *
	 * @param port the port on which the TCP server will listen to.
	 * @param requestClass the requesthandler class.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 *
	 * @return the created TCP server.
	 *
	 * @see com.teletalk.jserver.tcp.TcpServer
	 */
	public TcpServer createTcpServer(int port, Class requestClass,  Object[] requestClassCreationParams)
	{
		return createTcpServer(port, requestClass, requestClassCreationParams, false);
	}

	/**
	 * Convenience method to create and add a TCP server bound to the local host.
	 *
	 * @param port the port on which the TCP server will listen to.
	 * @param requestClass the requesthandler class.
	 * @param engage boolean value indicating if the created TCP server should be engaged.
	 *
	 * @return the created TCP server.
	 *
	 * @see com.teletalk.jserver.tcp.TcpServer
	 */
	public TcpServer createTcpServer(int port, Class requestClass, boolean engage)
	{
		TcpServer tcpServer = new TcpServer(this, requestClass, port);

		addSubSystem(tcpServer, engage);

		return tcpServer;
	}

	/**
	 * Convenience method to create and add a TCP server bound to the local host.
	 *
	 * @param port the port on which the TCP server will listen to.
	 * @param requestClass the requesthandler class.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 * @param engage boolean value indicating if the created TCP server should be engaged.
	 *
	 * @return the created TCP server.
	 *
	 * @see com.teletalk.jserver.tcp.TcpServer
	 */
	public TcpServer createTcpServer(int port, Class requestClass,  Object[] requestClassCreationParams, boolean engage)
	{
		TcpServer tcpServer = new TcpServer(this, requestClass, requestClassCreationParams, port);

		addSubSystem(tcpServer, engage);

		return tcpServer;
	}

	/**
	 * Convenience method to create and add a TCP server. The created TCP server will NOT
	 * be engaged as a result of this method call.
	 *
	 * @param port the port on which the TCP server will listen to.
	 * @param localIPAddress the local address to which the TCP server will be bound.
	 * @param requestClass the requesthandler class.
	 *
	 * @return the created TCP server.
	 *
	 * @see com.teletalk.jserver.tcp.TcpServer
	 */
	public TcpServer createTcpServer(int port, String localIPAddress, Class requestClass)
	{
		return createTcpServer(port, localIPAddress, requestClass, false);
	}

	/**
	 * Convenience method to create and add a TCP server. The created TCP server will NOT
	 * be engaged as a result of this method call.
	 *
	 * @param port the port on which the TCP server will listen to.
	 * @param localIPAddress the local address to which the TCP server will be bound.
	 * @param requestClass the requesthandler class.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 *
	 * @return the created TCP server.
	 *
	 * @see com.teletalk.jserver.tcp.TcpServer
	 */
	public TcpServer createTcpServer(int port, String localIPAddress, Class requestClass, Object[] requestClassCreationParams)
	{
		return createTcpServer(port, localIPAddress, requestClass, requestClassCreationParams, false);
	}

	/**
	 * Convenience method to create and add a TCP server .
	 *
	 * @param port the port on which the TCP server will listen to.
	 * @param localIPAddress the local address to which the TCP server will be bound.
	 * @param requestClass the requesthandler class.
	 * @param engage boolean value indicating if the created TCP server should be engaged.
	 *
	 * @return the created TCP server.
	 *
	 * @see com.teletalk.jserver.tcp.TcpServer
	 */
	public TcpServer createTcpServer(int port, String localIPAddress, Class requestClass, boolean engage)
	{
		TcpServer tcpServer = new TcpServer(this, requestClass, port, localIPAddress);

		addSubSystem(tcpServer, engage);

		return tcpServer;
	}

	/**
	 * Convenience method to create and add a TCP server .
	 *
	 * @param port the port on which the TCP server will listen to.
	 * @param localIPAddress the local address to which the TCP server will be bound.
	 * @param requestClass the requesthandler class.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 * @param engage boolean value indicating if the created TCP server should be engaged.
	 *
	 * @return the created TCP server.
	 *
	 * @see com.teletalk.jserver.tcp.TcpServer
	 */
	public TcpServer createTcpServer(int port, String localIPAddress, Class requestClass, Object[] requestClassCreationParams, boolean engage)
	{
		TcpServer tcpServer = new TcpServer(this, requestClass, requestClassCreationParams, port, localIPAddress);

		addSubSystem(tcpServer, engage);

		return tcpServer;
	}

	/**
	 * Convenience method to create and add a TCP server. The created TCP server will NOT
	 * be engaged as a result of this method call.
	 *
	 * @param name the name of the TCP server.
	 * @param port the port on which the TCP server will listen to.
	 * @param localIPAddress the local address to which the TCP server will be bound.
	 * @param requestClass the requesthandler class.
	 *
	 * @return the created TCP server.
	 *
	 * @see com.teletalk.jserver.tcp.TcpServer
	 */
	public TcpServer createTcpServer(String name, int port, String localIPAddress, Class requestClass)
	{
		return createTcpServer(name, port, localIPAddress, requestClass, false);
	}

	/**
	 * Convenience method to create and add a TCP server. The created TCP server will NOT
	 * be engaged as a result of this method call.
	 *
	 * @param name the name of the TCP server.
	 * @param port the port on which the TCP server will listen to.
	 * @param localIPAddress the local address to which the TCP server will be bound.
	 * @param requestClass the requesthandler class.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 *
	 * @return the created TCP server.
	 *
	 * @see com.teletalk.jserver.tcp.TcpServer
	 */
	public TcpServer createTcpServer(String name, int port, String localIPAddress, Class requestClass, Object[] requestClassCreationParams)
	{
		return createTcpServer(name, port, localIPAddress, requestClass, requestClassCreationParams, false);
	}

	/**
	 * Convenience method to create and add a TCP server .
	 *
	 * @param name the name of the TCP server.
	 * @param port the port on which the TCP server will listen to.
	 * @param localIPAddress the local address to which the TCP server will be bound.
	 * @param requestClass the requesthandler class.
	 * @param engage boolean value indicating if the created TCP server should be engaged.
	 *
	 * @return the created TCP server.
	 *
	 * @see com.teletalk.jserver.tcp.TcpServer
	 */
	public TcpServer createTcpServer(String name, int port, String localIPAddress, Class requestClass, boolean engage)
	{
		TcpServer tcpServer = new TcpServer(this, name, requestClass, port, localIPAddress);

		addSubSystem(tcpServer, engage);

		return tcpServer;
	}

	/**
	 * Convenience method to create and add a TCP server .
	 *
	 * @param name the name of the TCP server.
	 * @param port the port on which the TCP server will listen to.
	 * @param localIPAddress the local address to which the TCP server will be bound.
	 * @param requestClass the requesthandler class.
	 * @param requestClassCreationParams the parameters that are to be used when creating instances of the requesthandler class.
	 * @param engage boolean value indicating if the created TCP server should be engaged.
	 *
	 * @return the created TCP server.
	 *
	 * @see com.teletalk.jserver.tcp.TcpServer
	 */
	public TcpServer createTcpServer(String name, int port, String localIPAddress, Class requestClass, Object[] requestClassCreationParams, boolean engage)
	{
		TcpServer tcpServer = new TcpServer(this, name, requestClass, requestClassCreationParams, port, localIPAddress);

		addSubSystem(tcpServer, engage);

		return tcpServer;
	}

	/**
	 * Method containing the code for the thread which supervises all SubSystems and interpret messages put in
	 * the message queue.
	 * Applications should not call this function directly.
	 *
	 * @see #engage()
	 */
	public void run()
	{
      long lastCheck = System.currentTimeMillis();
		long waitTime = checkInterval.longValue();

		while(canRun)
		{
			if(lastCheck < (System.currentTimeMillis() - waitTime))
			{
				try
				{
					SubSystem subsys;
					List allSubSystems = this.getSubSystemTree();

					for(int i=0; i<allSubSystems.size(); i++)
					{
						subsys = (SubSystem)allSubSystems.get(i);

						if(!subsys.checkStatus())
						{
							subsys.error("Statuscheck failed!");
							logError("Statuscheck failed for subsystem " + subsys + "!");
						}
					}
				}
				catch(Exception e)
				{
					logError("Error while checking subsystems.", e);
				}
				lastCheck = System.currentTimeMillis() - 33;
				waitTime = checkInterval.longValue();
			}
			else waitTime = checkInterval.longValue() - (System.currentTimeMillis() - lastCheck);

			if(waitTime <= 0) waitTime = 1;
         
         try
         {
            if(!canRun) break;
            Thread.sleep(waitTime);
         }
         catch(InterruptedException ie)
         {
            if(!canRun) break;
         }

			if(getRestartCount() > 0 && (System.currentTimeMillis() - lastRestart) > (2*checkInterval.longValue()))
			{
				if(super.isDebugMode()) logDebug( "Resetting restartCounter. Previous value: " + getRestartCount() + ".");
				setRestartCount(0);
			}
		}
	}
   
   
   /* ### DYNAMIC COMPONENT METHODS BEGIN ### */
   
   
   /**
    * Gets the core component/system with the specified alias.
    * 
    * @see JServerConstants
    */
   public Object getCoreComponent(final String coreComponentAlias)
   {
      if (CONFIGURATION_MANAGER_ALIAS.equals(coreComponentAlias)) return this.getPropertyManager();
      else if (EVENT_QUEUE_ALIAS.equals(coreComponentAlias)) return this.getEventQueue();
      else if (LOG_MANAGER_ALIAS.equals(coreComponentAlias)) return this.getLogManager();
      else if (DEFAULT_CRITICAL_ERROR_LOGGER_ALIAS.equals(coreComponentAlias)) return this.getDefaultCriticalErrorLogger();
      else if (DEFAULT_FILE_LOGGER_ALIAS.equals(coreComponentAlias)) return this.getDefaultFileLogger();
      else if (LOAD_MANAGER_ALIAS.equals(coreComponentAlias)) return this.getLoadManager();
      else if (MAIN_HTTP_SERVER_ALIAS.equals(coreComponentAlias)) return this.getMainHttpServer();
      else if (PERIODIC_ACTION_MANAGER_ALIAS.equals(coreComponentAlias)) return this.getPeriodicActionManager();
      else if (RMI_ADMINISTRATION_ALIAS.equals(coreComponentAlias)) return this.getAdministration();
      else if (RMI_MANAGER_ALIAS.equals(coreComponentAlias)) return this.getRmiManager();
      else if (SNS_CLIENT_MANAGER_ALIAS.equals(coreComponentAlias)) return this.getSnsClientManager();
      else if (SNS_MANAGER_ALIAS.equals(coreComponentAlias)) return this.getSnsManager();
      else if (SPRING_APPLICATION_CONTEXT_ALIAS.equals(coreComponentAlias)) return this.getSpringApplicationContext();
      else if (STATISTICS_MANAGER_ALIAS.equals(coreComponentAlias)) return this.getStatisticsManager();
      else return null;
   }
   
   /**
    * Sets the core component/system with the specified alias.
    * 
    * @see JServerConstants
    */
   public void setCoreComponent(final String coreComponentAlias, final Object coreComponent)
   {
      if (DEFAULT_CRITICAL_ERROR_LOGGER_ALIAS.equals(coreComponentAlias)) this.setDefaultCriticalErrorLogger((CriticalErrorAppenderComponent) coreComponent);
      else if (DEFAULT_FILE_LOGGER_ALIAS.equals(coreComponentAlias)) this.setDefaultFileLogger((FileAppenderComponent) coreComponent);
      else if (LOAD_MANAGER_ALIAS.equals(coreComponentAlias)) this.setLoadManager((LoadManager) coreComponent);
      else if (MAIN_HTTP_SERVER_ALIAS.equals(coreComponentAlias)) this.setMainHttpServer((HttpServer) coreComponent);
      else if (PERIODIC_ACTION_MANAGER_ALIAS.equals(coreComponentAlias)) this.setPeriodicActionManager((PeriodicActionManager) coreComponent);
      else if (RMI_ADMINISTRATION_ALIAS.equals(coreComponentAlias)) this.setAdministration((Administration) coreComponent);
      else if (RMI_MANAGER_ALIAS.equals(coreComponentAlias)) this.setRmiManager((RmiManager) coreComponent);
      else if (SNS_CLIENT_MANAGER_ALIAS.equals(coreComponentAlias)) this.setSnsClientManager((SnsClientManager) coreComponent);
      else if (SNS_MANAGER_ALIAS.equals(coreComponentAlias)) this.setSnsManager((SnsManager) coreComponent);
      else if (SPRING_APPLICATION_CONTEXT_ALIAS.equals(coreComponentAlias)) this.setSpringApplicationContext((SpringApplicationContext) coreComponent);
      else if (STATISTICS_MANAGER_ALIAS.equals(coreComponentAlias)) this.setStatisticsManager((StatisticsManager) coreComponent);
   }
   
   /**
    * Instantiates dynamic components specified in configuration file. This method will not create dynamic core systems - this is handled by instantiateDynamicCoreComponent(String)
    * and instantiateDynamicCoreComponents().
    */
   private void instantiateDynamicComponents()
   {
      try
      {
         PropertyManager pm = this.getPropertyManager();
         
         if( pm != null )
         {
            Map loadedProperties = pm.getAllLoadedPropertiesAsMaps();
            
            if( loadedProperties != null )
            {
               String[] componentNames = (String[])loadedProperties.keySet().toArray(new String[0]);
               String componentName;
               Map componentLoadedProperties;
               
               // Sort componentNames to make sure that components higher up in the hierarchy comes before components further down, 
               // i.e. JServer.system1 comes before JServer.system1.component1.
               Arrays.sort(componentNames);
               
               for(int n=0; n<componentNames.length; n++)
               {
                  componentName = componentNames[n];
                  componentLoadedProperties = (Map)loadedProperties.get(componentName);
                  
                  // Don't instantiate core systems here
                  if( (componentName != null) && (componentLoadedProperties != null) && !pm.isDynamicCoreSystem(componentName) )
                  {
                     instantiateDynamicComponent(componentName, componentLoadedProperties, true);
                  }
               }
            }
         }            
      }
      catch(Exception e)
      {
         logError("Error while initializing dynamic components!", e);
         System.out.println("Error while initializing dynamic components!");
         e.printStackTrace();
      }
   }
   
   /**
    * Instantiates a dynamic core components specified in configuration file.
    */
   private SubComponent instantiateDynamicCoreComponent(final String dynamicCoreComponentId)
   {
      try
      {
         PropertyManager pm = this.getPropertyManager();
         
         if( pm != null )
         {
            Map loadedProperties = pm.getAllLoadedPropertiesAsMaps();
            
            if( loadedProperties != null )
            {
               if( pm.isDynamicCoreSystemRegistered(dynamicCoreComponentId) )
               {
                  String componentName = pm.getDynamicCoreSystemName(dynamicCoreComponentId);
                  return instantiateDynamicComponent(componentName, (Map)loadedProperties.get(componentName), false);
               }
            }
         }
      }
      catch(Exception e)
      {
         logError("Error while initializing dynamic default components!", e);
         System.out.println("Error while initializing dynamic default components!");
         e.printStackTrace();
      }
      
      return null;
   }
     
   /**
    * Instantiates dynamic components specified in configuration file.
    */
   private void instantiateDynamicCoreComponents()
   {
      try
      {
         PropertyManager pm = this.getPropertyManager();
         
         if( pm != null )
         {
            Map loadedProperties = pm.getAllLoadedPropertiesAsMaps();
            
            if( loadedProperties != null )
            {
               this.setDynamicCoreComponent(PropertyManager.DEFAULT_FILE_LOGGER_ALIAS, pm, loadedProperties);
               this.setDynamicCoreComponent(PropertyManager.DEFAULT_CRITICAL_ERROR_LOGGER_ALIAS, pm, loadedProperties);
               this.setDynamicCoreComponent(PropertyManager.MAIN_HTTP_SERVER_ALIAS, pm, loadedProperties);
               if( this.setDynamicCoreComponent(PropertyManager.RMI_MANAGER_ALIAS, pm, loadedProperties) )
               {
                  // Initialize the rmi properties for rmi codebase and host. It is imperative that these properties get initialized as soon as possible!
                  this.getRmiManager().initRmiProperties();
               }
               this.setDynamicCoreComponent(PropertyManager.PERIODIC_ACTION_MANAGER_ALIAS, pm, loadedProperties);
               this.setDynamicCoreComponent(PropertyManager.PERIODIC_ACTION_MANAGER_ALIAS, pm, loadedProperties);
               this.setDynamicCoreComponent(PropertyManager.LOAD_MANAGER_ALIAS, pm, loadedProperties);
               this.setDynamicCoreComponent(PropertyManager.STATISTICS_MANAGER_ALIAS, pm, loadedProperties);
               this.setDynamicCoreComponent(PropertyManager.SPRING_APPLICATION_CONTEXT_ALIAS, pm, loadedProperties);
               this.setDynamicCoreComponent(PropertyManager.SNS_CLIENT_MANAGER_ALIAS, pm, loadedProperties);
               this.setDynamicCoreComponent(PropertyManager.SNS_MANAGER_ALIAS, pm, loadedProperties);
               
               if( (this.administration == null) && pm.isDynamicCoreSystemRegistered(PropertyManager.RMI_ADMINISTRATION_ALIAS) )
               {
                  try
                  {
                     this.administration = new Administration();
                     logInfo("Administration created");
                  }
                  catch(Exception e)
                  {
                     e.printStackTrace();
                     logError("Error while creating Administration - " + e);
                  }
               }
            }
         }
      }
      catch(Exception e)
      {
         logError("Error while initializing dynamic default components!", e);
         System.out.println("Error while initializing dynamic default components!");
         e.printStackTrace();
      }
   }
   
   /**
    * Sets a dynamic core component.
    */
   private boolean setDynamicCoreComponent(final String coreComponentAlias, final PropertyManager pm, final Map loadedProperties) throws Exception
   {
      if( (this.getCoreComponent(coreComponentAlias) == null) && pm.isDynamicCoreSystemRegistered(coreComponentAlias) )
      {
         String dynamicComponentName = pm.getDynamicCoreSystemName(coreComponentAlias);
         Object dynamicComponent = instantiateDynamicComponent(dynamicComponentName, (Map)loadedProperties.get(dynamicComponentName), false);
         if( dynamicComponent != null )
         {
            this.setCoreComponent(coreComponentAlias, dynamicComponent);
            return true;
         }
      }
      return false;
   }
      
   /**
    * Instantiates a dynamic component.
    */
   private SubComponent instantiateDynamicComponent(final String componentName, final Map componentLoadedProperties, final boolean addComponent) throws Exception
   {
      boolean componentExists = true;
      SubComponent currentComponent = this;
      SubComponent nextComponent = null;
      SubComponent parentComponent = null;
      Property implementationClassNameProperty = null;
      String implementationClassName = null;
      Class implementationClass = null;
      Constructor componentConstructor = null;
      Object[] constructorParams = null;
      Object component = null;
      
      String[] nameChain = parseNames(componentName);
      if( (nameChain != null) && (nameChain.length > 1) )
      {
         for(int i=1; i<nameChain.length; i++)
         {
            nextComponent = currentComponent.getSubComponent(nameChain[i]);
                                                   
            if( nextComponent == null )
            {
               if( i == (nameChain.length - 1) ) // Make sure that parent is the component before the last element
               {
                  parentComponent = currentComponent;
               } 
                              
               componentExists = false;
               break;
            }
            else
            {
               currentComponent = nextComponent;
            }
         }
                        
         implementationClassNameProperty = (Property)componentLoadedProperties.get(SubComponent.IMPLEMENTATION_CLASS_NAME_PROPERTY);
         if( implementationClassNameProperty != null ) implementationClassName = implementationClassNameProperty.getValueAsString();
                        
         // If component doesn't already exsist, has a parent and an implementation class property...
         if( (!componentExists) && (parentComponent != null) && (implementationClassName != null) )
         {
            logInfo("Attempting to initialize dynamic component '" + componentName + "'.");
                           
            try
            {
               implementationClass = Class.forName(implementationClassName);
            }
            catch(ClassNotFoundException cnfe)
            {
               logError("Error while initializing dynamic component '" + componentName + "' - unable to load class '" + implementationClassName + "'!");
            }
            catch(Throwable t)
            {
               logError("Error while initializing dynamic component '" + componentName + "' - error while loading class '" + implementationClassName + "'!", t);
               if( t instanceof OutOfMemoryError) throw (OutOfMemoryError)t;
            }
                           
            String lastComponentName = nameChain[nameChain.length-1];
                        
            // Get constructor for JServer/SubSystem/SubComponent, String
            constructorParams = new Object[]{parentComponent, lastComponentName};
            componentConstructor = ReflectionUtils.findConstructor(implementationClass, constructorParams, false);
                           
            // Get constructor for SubSystem, String
            /*if( componentConstructor == null )
            { 
               constructorParams = new Object[]{parentComponent, lastComponentName};
               componentConstructor = ReflectionUtils.findConstructor(implementationClass, constructorParams, false);
            }*/
                           
            // Get constructor for String
            if( componentConstructor == null )
            { 
               constructorParams = new Object[]{lastComponentName};
               componentConstructor = ReflectionUtils.findConstructor(implementationClass, constructorParams, false);
            }
            
            // Get constructor for JServer/SubSystem/SubComponent
            if( componentConstructor == null )
            { 
               constructorParams = new Object[]{parentComponent};
               componentConstructor = ReflectionUtils.findConstructor(implementationClass, constructorParams, false);
            }
                           
            if( componentConstructor != null )
            {
               try
               {
                  component = componentConstructor.newInstance(constructorParams);
                                 
                  if( (parentComponent instanceof SubSystem) && (component instanceof SubSystem) )
                  { 
                     if( addComponent )
                     {
                        ((SubSystem)parentComponent).addSubSystem((SubSystem)component, true);
                     }
                     
                     return (SubSystem)component;
                  }
                  /*else if( (!(parentComponent instanceof SubSystem)) && (component instanceof SubSystem) )
                  { 
                     logError("Error while initializing dynamic component '" + componentName + "' - Cannot add SubSystem under SubComponent!");
                  }*/
                  else if( component instanceof SubComponent )
                  { 
                     if( addComponent )
                     {
                        parentComponent.addSubComponent((SubComponent)component, true);
                     }
                     
                     return (SubComponent)component;
                  }
                  else
                  { 
                     logError("Error while initializing dynamic component '" + componentName + "' - Implementation class not instance of SubSystem or SubComponent (" + implementationClass + ")!");
                  } 
               }
               catch(Throwable t)
               {
                  logError("Error while initializing dynamic component '" + componentName + "' - error while loading class '" + implementationClassName + "'.", t);
                  if( t instanceof OutOfMemoryError) throw (OutOfMemoryError)t;
               }
            }
            else
            {
               logError("Error while initializing dynamic component '" + componentName + "' - unable to find matching constructor for class '" + implementationClassName + "'. " + 
                           "Implementation class must have a constructor that takes one of the following sets of parameters: [String], [SubComponent, String] or [SubSystem, String].");
            }
         }
         // If component doesn't already exsist, has an implementation class property but no parent...
         else if( (!componentExists) && (parentComponent == null) && (implementationClassNameProperty != null) )
         {
            logError("Error while initializing dynamic components - unable to find parent component for '" + componentName + "'!");
         }
      }
      
      return null;
   }
   
   
   /* ### DYNAMIC COMPONENT METHODS END ### */
   

	/**
	 * Handles an error detected in a SubSystem.
	 *
	 * @param subsys the SubSystem in which an error occurred.
	 */
	public void handleSubSystemError(SubSystem subsys)
	{
      if( isDebugMode() ) logDebug("Perparing to handle error in subsystem " + subsys + ".");  
      
		if(subsys != null)
		{
			if(subsys.getStatus() == ERROR)
			{
				if(subsys.restartable())
				{
					if(super.isDebugMode()) logDebug("Restarting subsystem - " + subsys);
					subsys.reinitialize();
				}
				else
				{
					try
					{
						subsys.criticalError();
					}
					catch(Exception e)
					{
						logInfo("Error while shutting down crashed subsystem " + subsys + " - " + e);
					}
				}
			}
			else logInfo("Skipping reinitialization of subsystem " + subsys + ". Status no longer ERROR.");
		}
	}

	/**
	 * Handles an error detected in the mainthread (in JServer).
	 *
	 * @param exception an exception that occured in the mainthread.
	 */
	public void handleMainThreadError(Throwable exception)
	{
		logError("Error in topsystemthread - Restarting.", exception);

		stopThread();

		if(getRestartCount() < 10)
		{
			setLastRestart(System.currentTimeMillis());
			incrementRestartCount();

			startThread();
		}
		else logCriticalError("Critical error in JServer main thread! Unable to restart!", exception, JServerConstants.LOG_MESSAGE_ID_CRITICAL_JSERVER_ERROR);
	}

	/**
	 * Calculates the value for the (private)property activeThreadCount.
	 */
	public void calculateProperties()
	{
		activeThreadCount.setValue(threadGroup.activeCount());
	}
	
	/**
	 * Gets the RmiAdapter associated with this JServer.
	 *
	 * @return a RmiAdapter (JServerRmiAdapter) object.
	 *
	 * @see com.teletalk.jserver.rmi.adapter.JServerRmiAdapter
	 */
	public RmiAdapter getRmiAdapter()
	{
		if(rmiAdapter == null)
		{
			try
			{
				setRmiAdapter(new JServerRmiAdapter((JServer)this));
			}
			catch(RemoteException e)
			{
				logError("Unable to create JServerRmiAdapter", e);
			}
		}

		return rmiAdapter;
	}
  
   /**
    * Main method, used to run default configuration JServer, possibly with dynamic components. 
    * The name of the server can be specified through the parameter "-name".<br>
    * <br>
    * The parameters may consist of the following:<br>
    * <br>
    * <code>-name &lt;name&gt;</code> - The name of the server.<br>
    * <code>-empty</code> - Creates an empty server, i.e. no default components are added.<br>
    * <code>-noDefaultAppenders</code> - Creates a default configuration server, adds no appenders to the LogManager.<br>
    * <code>-conf &lt;config file&gt;</code> - Uses the specified configuration file.<br>
    * <code>-confClass &lt;config class&gt;</code> - Uses the specified class as PersistentPropertyStorage implementation in the PropertyManager.<br>
    * 
    * @param args array with application arguments.
    */
   public static void main(String args[]) throws Exception
   {
      createServer(args);
   }
   
   /**
    * Create a default configuration JServer, based on the specified set of parametes.<br>
    * <br>
    * The parameters may consist of the following:<br>
    * <br>
    * <code>-name &lt;name&gt;</code> - The name of the server.<br>
    * <code>-empty</code> - Creates an empty server, i.e. no default components are added.<br>
    * <code>-noDefaultAppenders</code> - Creates a default configuration server, adds no appenders to the LogManager.<br>
    * <code>-conf &lt;config file&gt;</code> - Uses the specified configuration file.<br>
    * <code>-confClass &lt;config class&gt;</code> - Uses the specified class as PersistentPropertyStorage implementation in the PropertyManager.<br>
    * 
    * @param args array with application arguments.
    * 
    * @since 2.0.1 (20041213)
    */
   public static JServer createServer(final String args[]) throws Exception
   {
      return createServer(args, true);
   }
   
   /**
    * Create a default configuration JServer, based on the specified set of parametes.<br>
    * <br>
    * The parameters may consist of the following:<br>
    * <br>
    * <code>-name &lt;name&gt;</code> - The name of the server.<br>
    * <code>-empty</code> - Creates an empty server, i.e. no default components are added.<br>
    * <code>-noDefaultAppenders</code> - Creates a default configuration server, adds no appenders to the LogManager.<br>
    * <code>-conf &lt;config file&gt;</code> - Uses the specified configuration file.<br>
    * <code>-confClass &lt;config class&gt;</code> - Uses the specified class as PersistentPropertyStorage implementation in the PropertyManager.<br>
    * 
    * @param args array with application arguments.
    * @param startServer flag indicating if the server should be started by this method call.
    * 
    * @since 2.0.1 (20041213)
    */
   public static JServer createServer(final String args[], final boolean startServer) throws Exception
   {
      String name = JServerConstants.JSERVER_TOP_SYSTEM_ALIAS;
      boolean empty = false;
      boolean createDefaultLoggers = true;
      String conf = null;
      PersistentPropertyStorage persistentPropertyStorage = null;
      
      //Check if a name for the server has been specified as a parameter
      if( (args != null) && (args.length > 1) )
      {
         for(int i=0; i<args.length; i++)
         {
            if(args[i].equalsIgnoreCase("-name"))
            { 
               name = args[i+1];
               i++;
            }
            else if(args[i].equalsIgnoreCase("-empty"))
            {
               empty = true;
            }
            else if(args[i].equalsIgnoreCase("-noDefaultAppenders"))
            {
               createDefaultLoggers = false;
            }
            else if(args[i].equalsIgnoreCase("-conf"))
            { 
               conf = args[i+1];
               i++;
            }
            else if(args[i].equalsIgnoreCase("-confClass"))
            { 
               String confClass = args[i+1];
               i++;
               persistentPropertyStorage= (PersistentPropertyStorage)Class.forName(confClass).newInstance();
            }
         }
      }
      
      System.out.println("Initializing " + name + "! (" + JServer.getVersionString() + ")");
      
      JServer server;
      
      if( (conf != null) || (persistentPropertyStorage != null) )
      {
         if (persistentPropertyStorage == null)
         {
            persistentPropertyStorage = PropertyManager.guessPersistentPropertyStorage(conf);
         }

         if( conf != null )
         {
            persistentPropertyStorage.setPersistentPropertyStorageFile(conf);
         }

         server = new JServer(name, persistentPropertyStorage, !empty, createDefaultLoggers);
      }
      else
      {
         if (empty)
         {
            server = JServer.createEmptyJServer();
         }
         else
         {
            server = new JServer(name);
         }
      }
      
      if( startServer ) server.startJServer();
      
      System.out.println(name + " initialized!");
      
      return server;
   }
   
   /**
    * Called by JService to start the server.
    * 
    * @param args array with application arguments.
    */
   public static void startService(final String args[]) throws Exception
   {
      createServer(args);
   }
   
   /**
    * Called by JService to stop the server.
    */
   public static void stopService()
   {
      if(topSystem != null) topSystem.stopJServer();
   }
}
