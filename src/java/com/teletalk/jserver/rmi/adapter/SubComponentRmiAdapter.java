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
package com.teletalk.jserver.rmi.adapter;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;

import com.teletalk.jserver.JServerConstants;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.log.AppenderComponent;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.rmi.RmiManager;
import com.teletalk.jserver.rmi.remote.RemoteAppenderComponentData;
import com.teletalk.jserver.rmi.remote.RemoteProperty;
import com.teletalk.jserver.rmi.remote.RemotePropertyData;
import com.teletalk.jserver.rmi.remote.RemoteSubComponent;
import com.teletalk.jserver.rmi.remote.RemoteSubComponentData;
import com.teletalk.jserver.rmi.remote.RemoteSubSystemData;

/**
 * Adapter for remote interaction with a SubComponent object.
 * 
 * @see com.teletalk.jserver.JServer
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public class SubComponentRmiAdapter extends UnicastRemoteObject implements RemoteSubComponent, RmiAdapter, Cloneable 
{
   static final long serialVersionUID = -1767608923621613565L;
   
	/** Reference to a SubComponent object. */
	protected SubComponent adaptee;
	
	/**
	 * Constructs a new SubComponentRmiAdapter object.
	 * 
	 * @param adaptee a SubComponent adaptee object.
	 * 
	 * @exception RemoteException if there was an error creating this SubComponentRmiAdapter.
	 */
	public SubComponentRmiAdapter(SubComponent adaptee) throws RemoteException
	{
		super(RmiManager.getRmiManager().getExportAddresses()[0].getPort());
		
		this.adaptee = adaptee;
	}
	
	public Object clone()
	{
		try{
		return super.clone();
		}catch(Exception e){e.printStackTrace(); return null;}
	}
	
	/**
	 * Returns the name of the SubComponent.
	 * 
	 * @return a String containing the name.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final String getName() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubComponentRmiAdapter", "getName()");
					
			return adaptee.getName();
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Returns the full name of the SubComponent.
	 * 
	 * @return a String containing the full name.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final String getFullName() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubComponentRmiAdapter", "getFullName()");
					
			return adaptee.getFullName();
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
		
	/**
	 * Returns the status of the SubComponent.
	 * 
	 * @return int indicating the status.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final int getStatus() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubComponentRmiAdapter", "getStatus()");
					
			return adaptee.getStatus();
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}

	/**
	 * Indicates whether or not the SubComponent is enabled.
	 * 
	 * @return boolean value inidcating if the state of the SubComponent is ENABLED.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final boolean isEnabled() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubComponentRmiAdapter", "isEnabled()");
					
			return adaptee.isEnabled();
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
   
   /**
    * Engages the SubSystem.
    * 
    * @exception RemoteException if there was an error during remote access of this method.
    * 
    * @since 2.0
    */
   public final boolean engage() throws RemoteException
   {
      try
      {
         JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubComponentRmiAdapter", "engage()");
         
         return adaptee.engage();
      }
      catch(Throwable t)
      {
         adaptee.logError("Error occurred during remote execution of operation!", t);
         if(t instanceof Error)
            throw (Error)t;
         else
            throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
      }
   }
   
   /**
    * Reinitilizes the SubSystem.
    * 
    * @exception RemoteException if there was an error during remote access of this method.
    * 
    * @since 2.0
    */
   public final boolean reinitialize() throws RemoteException
   {
      try
      {
         JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubComponentRmiAdapter", "reinitialize()");
         
         return adaptee.reinitialize();
      }
      catch(Throwable t)
      {
         adaptee.logError("Error occurred during remote execution of operation!", t);
         if(t instanceof Error)
            throw (Error)t;
         else
            throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
      }
   }
   
   /**
    * Shuts down the SubSystem.
    * 
    * @exception RemoteException if there was an error during remote access of this method.
    * 
    * @since 2.0
    */
   public final boolean shutDown() throws RemoteException
   {
      try
      {
         JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubComponentRmiAdapter", "shutDown()");
         
         return adaptee.shutDown();
      }
      catch(Throwable t)
      {
         adaptee.logError("Error occurred during remote execution of operation!", t);
         if(t instanceof Error)
            throw (Error)t;
         else
            throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
      }
   }
	
	/**
	 * Enables the SubComponent.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
    * 
    * @deprecated as of 2.0
	 */
	public boolean enable() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubComponentRmiAdapter", "enable()");
		
			return adaptee.engage();
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Disables the SubComponent.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
    * 
    * @deprecated as of 2.0
	 */
	public boolean disable() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubComponentRmiAdapter", "disable()");
					
			return adaptee.shutDown();
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Returns the properties contained in the SubComponent.
	 * 
	 * @return a List containing RemoteProperty objects.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */	
	public final List getProperties() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubComponentRmiAdapter", "getProperties()");
			
			ArrayList result = new ArrayList();
			List properties = adaptee.getProperties();
		
			for(int i=0; i<properties.size(); i++)
			{
				result.add(((Property)properties.get(i)).getRmiAdapter());
			}
		
			return result;
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Gets the property with the specified name contained in this SubComponent.
	 * 
	 * @param name the name of the Property.
	 * 
	 * @return a RemoteProperty object or null if none was found.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public RemoteProperty getProperty(String name) throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubComponentRmiAdapter", "getProperty(" + name + ")");
					
			Property p = adaptee.getProperty(name);
			RemoteProperty remote = null;
		
			if(p != null)
			{
				RmiAdapter r = p.getRmiAdapter();
				
				if(remote instanceof RemoteProperty)
					remote = (RemoteProperty)r;
			}
		
			return remote;
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Returns the child SubComponents of the SubComponent.
	 * 
	 * @return a Vector containing RemoteSubComponent objects.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final List getSubComponents() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubComponentRmiAdapter", "getSubComponents()");
					
			ArrayList result = new ArrayList();
			List subcomponents = adaptee.getSubComponents();
		
			for(int i=0; i<subcomponents.size(); i++)
			{
				result.add(((SubComponent)subcomponents.get(i)).getRmiAdapter());
			}

			return result;
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}			
	
	/**
	 * Gets the SubComponent with the specified name contained in this SubComponent.
	 * 
	 * @param name the name of the SubComponent.
	 * 
	 * @return a RemoteSubComponent or null if none was found.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public RemoteSubComponent getSubComponent(String name) throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubComponentRmiAdapter", "getSubComponent(" + name + ")");

			SubComponent s = adaptee.getSubComponent(name);
			RemoteSubComponent remote = null;
		
			if(s != null)
			{
				RmiAdapter r = s.getRmiAdapter();
				
				if(remote instanceof RemoteSubComponent)
					remote = (RemoteSubComponent)r;
			}
		
			return remote;
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
		
	/**
	 * Returns a recursive listing of all child SubComponents of the SubComponent.
	 * 
	 * @return a Vector containing RemoteSubComponent objects.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final List getSubComponentsRecursively() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubComponentRmiAdapter", "getSubComponentsRecursively()");
					
			ArrayList result = new ArrayList();
			List subcomponents = adaptee.getSubComponentTree();
		
			for(int i=0; i<subcomponents.size(); i++)
			{
				result.add(((SubComponent)subcomponents.get(i)).getRmiAdapter());
			}

			return result;
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Convenience method to send a logmessage to the logmanager.
	 * 
	 * @param level the loglevel.
	 * @param msg the message.
	 * 
	 * @see com.teletalk.jserver.log.LogManager
	 */
	public final void log(int level, String msg) throws RemoteException
	{
		try
		{
			adaptee.log(Level.toLevel(level), msg);
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Convenience method to send a logmessage to the logmanager.
	 * 
	 * @param level the loglevel.
	 * @param origin of the LogMessage.
	 * @param msg the message.
	 * 
	 * @see com.teletalk.jserver.log.LogManager
	 */
	public final void log(int level, String origin, String msg) throws RemoteException
	{
		try
		{
			adaptee.log(Level.toLevel(level), origin, msg);
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Convenience method to send a logmessage to the logmanager.
	 * 
	 * @param level the loglevel.
	 * @param time the time of the log.
	 * @param origin of the LogMessage.
	 * @param msg the message.
	 * 
	 * @see com.teletalk.jserver.log.LogManager
    * 
    * @deprecated as of 2.0
	 */
	public final void log(int level, java.util.Date time, String origin, String msg) throws RemoteException
	{
		try
		{
			adaptee.log(Level.toLevel(level), origin, msg);
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Convenience method to send a debug LogMessage to the LogManager, if the debugMode property of this SubComponent is set to <code>true</code>.
	 * 
	 * @param msg the message.
	 */
	public final void logDebug(String msg) throws RemoteException
	{
		try
		{
			adaptee.logDebug(msg);
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Convenience method to send a debug LogMessage to the LogManager, if the debugMode property of this SubComponent is set to <code>true</code>.
	 * 
	 * @param origin of the LogMessage.
	 * @param msg the message.
	 */
	public final void logDebug(String origin, String msg) throws RemoteException
	{
		try
		{
			adaptee.logDebug(origin, msg);
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Convenience method to send an information LogMessage to the LogManager
	 * 
	 * @param msg the message.
	 */
	public final void logInfo(String msg) throws RemoteException
	{
		try
		{
			adaptee.logInfo(msg);
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Convenience method to send an information LogMessage to the LogManager.
	 * 
	 * @param origin of the LogMessage.
	 * @param msg the message.
	 */
	public final void logInfo(String origin, String msg) throws RemoteException
	{
		try
		{
			adaptee.logInfo(origin, msg);
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Convenience method to send a warning LogMessage to the LogManager.
	 * 
	 * @param msg the message.
	 */
	public final void logWarning(String msg) throws RemoteException
	{
		try
		{
			adaptee.logWarning(msg);
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Convenience method to send a warning LogMessage to the LogManager.
	 * 
	 * @param origin of the LogMessage.
	 * @param msg the message.
	 */
	public final void logWarning(String origin, String msg) throws RemoteException
	{
		try
		{
			adaptee.logWarning(origin, msg);
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Convenience method to send a warning LogMessage to the LogManager.
	 * 
	 * @param msg the message.
	 * @param error an error to be logged.
	 */
	public final void logWarning(String msg, Throwable error) throws RemoteException
	{
		try
		{
			adaptee.logWarning(msg, error);
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Convenience method to send a warning LogMessage to the LogManager. This method prints a stacktrace from 
	 * the parameter error and adds it to the logmessage.
	 * 
	 * @param origin of the LogMessage.
	 * @param msg the message.
	 * @param error an error to be logged.
	 */
	public final void logWarning(String origin, String msg, Throwable error) throws RemoteException
	{
		try
		{
			adaptee.logWarning(origin, msg, error);
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Convenience method to send an errorlogmessage to the logmanager. The level of the logmessage is set to ERROR_LEVEL.
	 * 
	 * @param msg a description of the error.
	 * 
	 * @see com.teletalk.jserver.log.LogManager
	 */
	public final void logError(String msg) throws RemoteException
	{
		try
		{
			adaptee.logError(msg);
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Convenience method to send an errorlogmessage to the logmanager. The level of the logmessage is set to ERROR_LEVEL.
	 * 
	 * @param origin of the LogMessage.
	 * @param msg a description of the error.
	 * 
	 * @see com.teletalk.jserver.log.LogManager
	 */
	public final void logError(String origin, String msg) throws RemoteException
	{
		try
		{
			adaptee.logError(origin, msg);
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Convenience method to send an errorlogmessage to the logmanager. This method prints a stacktrace from 
	 * the parameter error and adds it to the logmessage. The level of the logmessage is set to ERROR_LEVEL.
	 * 
	 * @param msg a msg of the error.
	 * @param error an error to be logged.
	 * 
	 * @see com.teletalk.jserver.log.LogManager
	 */
	public final void logError(String msg, Throwable error) throws RemoteException
	{
		try
		{
			adaptee.logError(msg, error);
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
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
	public final void logError(String origin, String msg, Throwable error) throws RemoteException
	{
		try
		{
			adaptee.logError(origin, msg, error);
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
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
	public final void logError(int level, String msg, Throwable error) throws RemoteException
	{
		try
		{
			adaptee.log(Level.toLevel(level), msg, error);
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
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
	public final void logError(int level, String origin, String msg, Throwable error) throws RemoteException
	{
		try
		{
			adaptee.log(Level.toLevel(level), origin, msg, error);
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error)
				throw (Error)t;
			else
				throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
	
	/**
	 * Convenience method to send a critical error LogMessage to the LogManager.
	 * 
	 * @param msg the message.
    * 
    * @deprecated replaced by {@link #logCriticalError(String, int)}
	 */
	public final void logCriticalError(String msg) throws RemoteException
	{
		try
		{
			adaptee.logCriticalError(msg);
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if (t instanceof Error) throw (Error) t;
         else throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
   
   /**
    * Convenience method to log a critical error message (org.apache.log4j.Level.FATAL).
    * 
    * @param msg the message to log.
    * @param logMessageId the id associated with the log message. This id will be stored in the MDC as an java.lang.Integer object 
    * under the key {@link JServerConstants#LOG_MESSAGE_ID_KEY}. The range 4095-65535 is reserved for JServer.
    */
   public void logCriticalError(String msg, int logMessageId) throws RemoteException
   {
      try
      {
         adaptee.logCriticalError(msg, logMessageId);
      }
      catch(Throwable t)
      {
         adaptee.logError("Error occurred during remote execution of operation!", t);
         if (t instanceof Error) throw (Error) t;
         else throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
      }
   }
	
	/**
	 * Convenience method to send a critical error LogMessage to the LogManager.
	 * 
	 * @param origin of the LogMessage.
	 * @param msg the message.
    * 
    * @deprecated replaced by {@link #logCriticalError(String, String, int)}
	 */
	public final void logCriticalError(String origin, String msg) throws RemoteException
	{
		try
		{
			adaptee.logCriticalError(origin, msg);
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if (t instanceof Error) throw (Error) t;
         else throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
   
   /**
    * Convenience method to log a critical error message (org.apache.log4j.Level.FATAL).
    * 
    * @param origin the origin of the logging event. The thread name will be set to this parameter when creating the logging event.
    * @param msg the message to log.
    * @param logMessageId the id associated with the log message. This id will be stored in the MDC as an java.lang.Integer object 
    * under the key {@link JServerConstants#LOG_MESSAGE_ID_KEY}. The range 4095-65535 is reserved for JServer.
    */
   public void logCriticalError(String origin, String msg, int logMessageId) throws RemoteException
   {
      try
      {
         adaptee.logCriticalError(origin, msg, logMessageId);
      }
      catch(Throwable t)
      {
         adaptee.logError("Error occurred during remote execution of operation!", t);
         if (t instanceof Error) throw (Error) t;
         else throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
      }
   }
	
	/**
	 * Convenience method to send a critical error LogMessage to the LogManager. This method prints a stacktrace from 
	 * the parameter error and adds it to the logmessage.
	 * 
	 * @param msg the message.
	 * @param error an error to be logged.
    * 
    * @deprecated replaced by {@link #logCriticalError(String, Throwable, int)}
	 */
	public final void logCriticalError(String msg, Throwable error) throws RemoteException
	{
		try
		{
			adaptee.logCriticalError(msg, error);
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if (t instanceof Error) throw (Error) t;
         else throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
	}
   
   /**
    * Convenience method to log a critical error message (org.apache.log4j.Level.FATAL).
    * 
    * @param msg the message to log.
    * @param throwable a throwable to be associated with the logging event. This parameter may be <code>null</code>.
    * @param logMessageId the id associated with the log message. This id will be stored in the MDC as an java.lang.Integer object 
    * under the key {@link JServerConstants#LOG_MESSAGE_ID_KEY}. The range 4095-65535 is reserved for JServer.
    */
   public void logCriticalError(String msg, Throwable error, int logMessageId) throws RemoteException
   {
      try
      {
         adaptee.logCriticalError(msg, error, logMessageId);
      }
      catch(Throwable t)
      {
         adaptee.logError("Error occurred during remote execution of operation!", t);
         if (t instanceof Error) throw (Error) t;
         else throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
      }
   }
	
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
	public final void logCriticalError(String origin, String msg, Throwable error) throws RemoteException
	{
		try
		{
			adaptee.logCriticalError(origin, msg, error);
		}
		catch(Throwable t)
		{
			adaptee.logError("Error occurred during remote execution of operation!", t);
			if(t instanceof Error) throw (Error)t;
			else throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
		}
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
   public void logCriticalError(String origin, String msg, Throwable error, int logMessageId) throws RemoteException
   {
      try
      {
         adaptee.logCriticalError(origin, msg, error, logMessageId);
      }
      catch(Throwable t)
      {
         adaptee.logError("Error occurred during remote execution of operation!", t);
         if(t instanceof Error) throw (Error)t;
         else throw new RemoteException("Error occurred during remote execution of operation in " + adaptee.getFullName() + "!", t);
      }
   }
   
   /**
    * Generates recursive administration data for the specified component.
    * 
    * @since 2.0
    */
   public static RemoteSubComponentData getRemoteSubComponentData(final SubComponent subComponent)
   {
      if( subComponent == null ) return null;
      
      try
      {
         final List subcomponents = subComponent.getSubComponents();
         boolean isAppenderComponent = (subComponent instanceof AppenderComponent);
         boolean isSubSystem = (subComponent instanceof SubSystem);
         final List properties =  subComponent.getProperties(); 
         
         ArrayList subSystemData = null; // To preserve backwards compatability
         ArrayList subComponentData = null;
         ArrayList propertyData = null;
         
         if(subcomponents.size() > 0)
         {
            SubComponent child;
            RemoteSubComponentData remoteSubComponentData;
         
            for(int i=0; i<subcomponents.size(); i++)
            {
               child = (SubComponent)subcomponents.get(i);

               if( isSubSystem && (child instanceof SubSystem) )
               {
                  if( subSystemData == null ) subSystemData = new ArrayList();
                  
                  remoteSubComponentData = getRemoteSubComponentData(child);
                  if( remoteSubComponentData != null ) subSystemData.add( remoteSubComponentData );
               }
               else
               {
                  if( subComponentData == null ) subComponentData = new ArrayList();
                  
                  remoteSubComponentData = getRemoteSubComponentData(child);
                  if( remoteSubComponentData != null ) subComponentData.add( remoteSubComponentData );
               }
            }
         }
         
         if(properties.size() > 0)
         {
            propertyData = new ArrayList();
            Property p;
            RemotePropertyData rpd;
      
            for(int i=0; i<properties.size(); i++)
            {
               p = (Property)properties.get(i);

               rpd = new RemotePropertyData(p.getName(), p.getValueAsString(), p.getMetaData(), p.getModificationMode(), PropertyRmiAdapter.getType(p));
               propertyData.add(rpd);
            }
         }
         
         if( isAppenderComponent ) return new RemoteAppenderComponentData(subComponent.getName(), subComponent.getStatus(), subComponentData , propertyData);
         else if( isSubSystem ) return new RemoteSubSystemData(subComponent.getName(), subComponent.getStatus(), subSystemData, subComponentData, propertyData);
         else return new RemoteSubComponentData(subComponent.getName(), subComponent.getStatus(), subComponentData , propertyData);
      }
      catch(Exception e)
      {
         subComponent.logError("Error while getting remote component data for " + subComponent.getFullName() + ".", e);
         return null;
      }
   }
}
