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
package com.teletalk.jserver.rmi.remote;

import java.rmi.RemoteException;
import java.util.List;

import com.teletalk.jserver.JServerConstants;

/**
 * Remote interface for interacting with a SubComponentRmiAdapter object.
 * 
 * @see com.teletalk.jserver.rmi.adapter.SubComponentRmiAdapter
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public interface RemoteSubComponent extends RemoteObject
{
	/**
	 * Returns the name of the SubComponent.
	 * 
	 * @return a String containing the name.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public String getName() throws RemoteException;
		
	/**
	 * Returns the full name of the SubComponent.
	 * 
	 * @return a String containing the full name.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public String getFullName() throws RemoteException;
		
	/**
	 * Returns the status of the SubComponent.
	 * 
	 * @return int indicating the status.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public int getStatus() throws RemoteException;

	/**
	 * Indicates whether or not the SubComponent is enabled.
	 * 
	 * @return boolean value inidcating if the state of the SubComponent is ENABLED.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public boolean isEnabled() throws RemoteException;
	
   /**
    * Engages the SubSystem.
    * 
    * @exception RemoteException if there was an error during remote access of this method.
    * 
    * @since 2.0
    */
   public boolean engage() throws RemoteException;
   
   /**
    * Reinitilizes the SubSystem.
    * 
    * @exception RemoteException if there was an error during remote access of this method.
    * 
    * @since 2.0
    */
   public boolean reinitialize() throws RemoteException;
   
   /**
    * Shuts down the SubSystem.
    * 
    * @exception RemoteException if there was an error during remote access of this method.
    * 
    * @since 2.0
    */
   public boolean shutDown() throws RemoteException;
   
	/**
	 * Enables the SubComponent.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
    * 
    * @deprecated as of 2.0
	 */
	public boolean enable() throws RemoteException;

	/**
	 * Disables the SubComponent.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
    * 
    * @deprecated as of 2.0
	 */
	public boolean disable() throws RemoteException;
	
	/**
	 * Returns the properties contained in the SubComponent.
	 * 
	 * @return a Vector containing RemoteProperty objects.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */	
	public List getProperties() throws RemoteException;

	/**
	 * Gets the property with the specified name contained in this SubComponent.
	 * 
	 * @param name the name of the Property.
	 * 
	 * @return a RemoteProperty or null if none was found.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public RemoteProperty getProperty(String name) throws RemoteException;
		
	/**
	 * Returns the child SubComponents of the SubComponent.
	 * 
	 * @return a Vector containing RemoteSubComponent objects.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public List getSubComponents() throws RemoteException;
	
	/**
	 * Gets the SubComponent with the specified name contained in this SubComponent.
	 * 
	 * @param name the name of the SubComponent.
	 * 
	 * @return a RemoteSubComponent or null if none was found.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public RemoteSubComponent getSubComponent(String name) throws RemoteException;

	/**
	 * Returns a recursive listing of all child SubComponents of the SubComponent.
	 * 
	 * @return a Vector containing RemoteSubComponent objects.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public List getSubComponentsRecursively() throws RemoteException;
	
	/**
	 * Convenience method to send a logmessage to the logmanager.
	 * 
	 * @param level the loglevel.
	 * @param msg the message.
	 * 
	 * @see com.teletalk.jserver.log.LogManager
	 */
	public void log(int level, String msg) throws RemoteException;
	
	/**
	 * Convenience method to send a logmessage to the logmanager.
	 * 
	 * @param level the loglevel.
	 * @param origin of the LogMessage.
	 * @param msg the message.
	 * 
	 * @see com.teletalk.jserver.log.LogManager
	 */
	public void log(int level, String origin, String msg) throws RemoteException;
	
	/**
	 * Convenience method to send a logmessage to the logmanager.
	 * 
	 * @param level the loglevel.
	 * @param time the time of the log.
	 * @param origin of the LogMessage.
	 * @param msg the message.
	 * 
	 * @see com.teletalk.jserver.log.LogManager
	 */
	public void log(int level, java.util.Date time, String origin, String msg) throws RemoteException;
	
	/**
	 * Convenience method to send a debug LogMessage to the LogManager, if the debugMode property of this SubComponent is set to <code>true</code>.
	 * 
	 * @param msg the message.
	 */
	public void logDebug(String msg) throws RemoteException;
	
	/**
	 * Convenience method to send a debug LogMessage to the LogManager, if the debugMode property of this SubComponent is set to <code>true</code>.
	 * 
	 * @param origin of the LogMessage.
	 * @param msg the message.
	 */
	public void logDebug(String origin, String msg) throws RemoteException;
	
	/**
	 * Convenience method to send an information LogMessage to the LogManager
	 * 
	 * @param msg the message.
	 */
	public void logInfo(String msg) throws RemoteException;
	
	/**
	 * Convenience method to send an information LogMessage to the LogManager.
	 * 
	 * @param origin of the LogMessage.
	 * @param msg the message.
	 */
	public void logInfo(String origin, String msg) throws RemoteException;
	
	/**
	 * Convenience method to send a warning LogMessage to the LogManager.
	 * 
	 * @param msg the message.
	 */
	public void logWarning(String msg) throws RemoteException;
	
	/**
	 * Convenience method to send a warning LogMessage to the LogManager.
	 * 
	 * @param origin of the LogMessage.
	 * @param msg the message.
	 */
	public void logWarning(String origin, String msg) throws RemoteException;
	
	/**
	 * Convenience method to send a warning LogMessage to the LogManager.
	 * 
	 * @param msg the message.
	 * @param error an error to be logged.
	 */
	public void logWarning(String msg, Throwable error) throws RemoteException;
	
	/**
	 * Convenience method to send a warning LogMessage to the LogManager. This method prints a stacktrace from 
	 * the parameter error and adds it to the logmessage.
	 * 
	 * @param origin of the LogMessage.
	 * @param msg the message.
	 * @param error an error to be logged.
	 */
	public void logWarning(String origin, String msg, Throwable error) throws RemoteException;
	
	/**
	 * Convenience method to send an errorlogmessage to the logmanager. The level of the logmessage is set to ERROR_LEVEL.
	 * 
	 * @param msg a description of the error.
	 * 
	 * @see com.teletalk.jserver.log.LogManager
	 */
	public void logError(String msg) throws RemoteException;
	
	/**
	 * Convenience method to send an errorlogmessage to the logmanager. The level of the logmessage is set to ERROR_LEVEL.
	 * 
	 * @param origin of the LogMessage.
	 * @param msg a description of the error.
	 * 
	 * @see com.teletalk.jserver.log.LogManager
	 */
	public void logError(String origin, String msg) throws RemoteException;
	
	/**
	 * Convenience method to send an errorlogmessage to the logmanager. This method prints a stacktrace from 
	 * the parameter error and adds it to the logmessage. The level of the logmessage is set to ERROR_LEVEL.
	 * 
	 * @param msg a msg of the error.
	 * @param error an error to be logged.
	 * 
	 * @see com.teletalk.jserver.log.LogManager
	 */
	public void logError(String msg, Throwable error) throws RemoteException;
	
	/**
	 * Convenience method to send an errorlogmessage to the logmanager. This method prints a stacktrace from 
	 * the parameter error and adds it to the logmessage. The level of the logmessage is set to ERROR_LEVEL.
	 * 
	 * @param origin of the LogMessage.
	 * @param msg a description of the error.
	 * @param error an error to be logged.
	 * 
	 * @see com.teletalk.jserver.log.LogManager
	 */
	public void logError(String origin, String msg, Throwable error) throws RemoteException;
	
	/**
	 * Convenience method to send an errorlogmessage to the logmanager. This method prints a stacktrace from 
	 * the parameter error and adds it to the logmessage.
	 * 
	 * @param level the loglevel.
	 * @param msg a description of the error.
	 * @param error an error to be logged.
	 * 
	 * @see com.teletalk.jserver.log.LogManager
	 */
	public void logError(int level, String msg, Throwable error) throws RemoteException;
	
	/**
	 * Convenience method to send an errorlogmessage to the logmanager. This method prints a stacktrace from 
	 * the parameter error and adds it to the logmessage.
	 * 
	 * @param level the loglevel.
	 * @param origin of the LogMessage.
	 * @param msg a description of the error.
	 * @param error an error to be logged.
	 * 
	 * @see com.teletalk.jserver.log.LogManager
	 */
	public void logError(int level, String origin, String msg, Throwable error) throws RemoteException;
	
	/**
	 * Convenience method to send a critical error LogMessage to the LogManager.
	 * 
	 * @param msg the message.
    * 
    * @deprecated replaced by {@link #logCriticalError(String, int)}
	 */
	public void logCriticalError(String msg) throws RemoteException;
   
   /**
    * Convenience method to log a critical error message (org.apache.log4j.Level.FATAL).
    * 
    * @param msg the message to log.
    * @param logMessageId the id associated with the log message. This id will be stored in the MDC as an java.lang.Integer object 
    * under the key {@link JServerConstants#LOG_MESSAGE_ID_KEY}. The range 4095-65535 is reserved for JServer.
    */
   public void logCriticalError(String msg, int logMessageId) throws RemoteException;
	
	/**
	 * Convenience method to send a critical error LogMessage to the LogManager.
	 * 
	 * @param origin of the LogMessage.
	 * @param msg the message.
    * 
    * @deprecated replaced by {@link #logCriticalError(String, String, int)}
	 */
	public void logCriticalError(String origin, String msg) throws RemoteException;
   
   /**
    * Convenience method to log a critical error message (org.apache.log4j.Level.FATAL).
    * 
    * @param origin the origin of the logging event. The thread name will be set to this parameter when creating the logging event.
    * @param msg the message to log.
    * @param logMessageId the id associated with the log message. This id will be stored in the MDC as an java.lang.Integer object 
    * under the key {@link JServerConstants#LOG_MESSAGE_ID_KEY}. The range 4095-65535 is reserved for JServer.
    */
   public void logCriticalError(String origin, String msg, int logMessageId) throws RemoteException;
	
	/**
	 * Convenience method to send a critical error LogMessage to the LogManager. This method prints a stacktrace from 
	 * the parameter error and adds it to the logmessage.
	 * 
	 * @param msg the message.
	 * @param error an error to be logged.
    * 
    * @deprecated replaced by {@link #logCriticalError(String, Throwable, int)}
	 */
	public void logCriticalError(String msg, Throwable error) throws RemoteException;
   
   /**
    * Convenience method to log a critical error message (org.apache.log4j.Level.FATAL).
    * 
    * @param msg the message to log.
    * @param throwable a throwable to be associated with the logging event. This parameter may be <code>null</code>.
    * @param logMessageId the id associated with the log message. This id will be stored in the MDC as an java.lang.Integer object 
    * under the key {@link JServerConstants#LOG_MESSAGE_ID_KEY}. The range 4095-65535 is reserved for JServer.
    */
   public void logCriticalError(String msg, Throwable throwable, int logMessageId) throws RemoteException;
	
	/**
	 * Convenience method to send a critical error LogMessage to the LogManager. This method prints a stacktrace from 
	 * the parameter error and adds it to the logmessage.
	 * 
	 * @param origin of the LogMessage.
	 * @param msg the message.
	 * @param error an error to be logged.
    * 
    * @deprecated replaced by {@link #logCriticalError(String, String, Throwable, int)}
	 */
	public void logCriticalError(String origin, String msg, Throwable error) throws RemoteException;
   
   /**
    * Convenience method to log a critical error message (org.apache.log4j.Level.FATAL).
    * 
    * @param origin the origin of the logging event. The thread name will be set to this parameter when creating the logging event.
    * @param msg the message to log.
    * @param throwable a throwable to be associated with the logging event. This parameter may be <code>null</code>.
    * @param logMessageId the id associated with the log message. This id will be stored in the MDC as an java.lang.Integer object 
    * under the key {@link JServerConstants#LOG_MESSAGE_ID_KEY}. The range 4095-65535 is reserved for JServer.
    */
   public void logCriticalError(String origin, String msg, Throwable throwable, int logMessageId) throws RemoteException;
}
