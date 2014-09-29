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
import java.rmi.server.RemoteServer;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.JServerUtilities;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.rmi.RmiManager;
import com.teletalk.jserver.rmi.remote.RemoteAdministration;
import com.teletalk.jserver.rmi.remote.RemoteJServer;
import com.teletalk.jserver.rmi.remote.RemoteProperty;
import com.teletalk.jserver.rmi.remote.RemoteSubComponent;
import com.teletalk.jserver.rmi.remote.RemoteSubSystem;
import com.teletalk.jserver.rmi.remote.RemoteSubSystemData;

/**
 * Adapter for remote interaction with a JServer object.
 * 
 * @see com.teletalk.jserver.JServer
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public class JServerRmiAdapter extends SubSystemRmiAdapter implements RemoteJServer
{
   static final long serialVersionUID = 8510159049056532233L;

	/** Reference to a JServer object. */
	protected JServer adaptee;
	
	/**
	 * Constructs a new JServerRmiAdapter object.
	 * 
	 * @param adaptee a JServer adaptee object.
	 * 
	 * @exception RemoteException if there was an error creating this JServerRmiAdapter.
	 */
	public JServerRmiAdapter(JServer adaptee) throws RemoteException
	{
		super(adaptee);
		this.adaptee = adaptee;
	}
	
	/**
	 * Checks if rmi method calls should be logged.
	 * 
	 * @return <code>true</code> if rmi method calls should be logged, otherwise <code>false</code>.
	 */
	public static final boolean isRmiMethodCallLoggingEnabled()
	{
		RmiManager rm = RmiManager.getRmiManager();
		
		return (( rm != null) ? rm.isRmiMethodCallLoggingEnabled() : false );
	}
	
	/**
	 * Method for logging of a method call.
	 * 
	 * @param origin the log origin.
	 * @param methodName the name of the method to create a log entry for.
	 */
	public static final void logMethodCall(final String origin, final String methodName)
	{
		if( isRmiMethodCallLoggingEnabled() )
		{
			try{
			JServerUtilities.logDebug(origin, "Method " + methodName + " called by " + RemoteServer.getClientHost() + ".");
			}catch(Exception e){}
		}
	}
	
	/**
	 * Restarts the server.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public void restartServer() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".JServerRmiAdapter", "restartServer()");

			adaptee.restartJServer();
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
	 * Shuts down the server.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public void shutDownServer() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".JServerRmiAdapter", "shutDownServer()");

			adaptee.stopJServer(true);
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
	 * Kills the server.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public void killServer() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".JServerRmiAdapter", "killServer()");

			adaptee.killJServer();
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
	 * Returns a remote reference to a Administration object.
	 * 
	 * @return an RemoteAdministration object.
	 * 
	 * @see com.teletalk.jserver.rmi.Administration
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public RemoteAdministration getAdministration() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".JServerRmiAdapter", "getAdministration()");
			
			RemoteAdministration radm = adaptee.getAdministration();
			return radm;
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
	 * Gets a tree of objects containing information about the components of this server.
	 * 
	 * @return a RemoteSubSystemData object.
	 */
	public RemoteSubSystemData getSystemTreeData() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".JServerRmiAdapter", "getSystemTreeData()");

         return (RemoteSubSystemData)getRemoteSubComponentData(adaptee);
         //return new RemoteSubSystemData(adaptee.getName(), adaptee.getStatus(), adaptee.getSubSystemData(), adaptee.getSubComponentData(), adaptee.getPropertyData());
			//return adaptee.getSystemTreeData();
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
	 * Performs a search for a Property in this server and returns a RemoteProperty interface to it.
	 * 
	 * @param fullName the name of the Property.
	 * 
	 * @return a RemoteProperty interface or null if no property was found.
	 */
	public RemoteProperty findRemoteProperty(String fullName) throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".JServerRmiAdapter", "findRemoteProperty(\"" +fullName + "\")");
			
			Property p = adaptee.findProperty(fullName);

			if(p != null)
				return (PropertyRmiAdapter)p.getRmiAdapter();	
			else	
				return null;
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
	 * Performs a search for a SubComponent in this server and returns a RemoteSubComponent interface to it.
	 * 
	 * @param fullName the name of the SubComponent.
	 * 
	 * @return a RemoteSubComponent interface or null if no subcomponent was found.
	 */
	public RemoteSubComponent findRemoteSubComponent(String fullName) throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".JServerRmiAdapter", "findRemoteSubComponent(\"" +fullName + "\")");
			
			SubComponent s = adaptee.findSubComponent(fullName);

			if(s != null)
				return (SubComponentRmiAdapter)s.getRmiAdapter();
			else
				return null;
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
	 * Performs a search for a SubSystem in this server and returns a RemoteSubSystem interface to it.
	 * 
	 * @param fullName the name of the SubSystem.
	 * 
	 * @return a RemoteSubSystem interface or null if no subsystem was found.
	 */
	public RemoteSubSystem findRemoteSubSystem(String fullName, boolean createDefault) throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".JServerRmiAdapter", "findRemoteSubSystem(\"" +fullName + "\")");
			
			SubSystem s = adaptee.findSubSystem(fullName);

			if(s != null && !createDefault)
				return (SubSystemRmiAdapter)s.getRmiAdapter();
			else if(s != null && createDefault)
				return new SubSystemRmiAdapter(s);
			else
				return null;
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
	 * Gets the major version number of the remote JServer (the JServer API). If the version is 1.23 then major version is 1.
	 *
	 * @return the major version number.
	 * 
	 * @since 1.13 Build 601
	 */
	public short getJServerVersionMajor()
	{
		return JServer.getAppVersionMajor();
	}

	/**
	 * Gets the minor version number of the remote JServer (the JServer API). If the version is 1.23 then minor version is 2.
	 *
	 * @return the minor version number.
	 * 
	 * @since 1.13 Build 601
	 */
	public short getJServerVersionMinor()
	{
		return JServer.getAppVersionMinor();
	}

	/**
	 * Gets the micro version number of the remote JServer (the JServer API). If the version is 1.23 then micro version is 3.
	 *
	 * @return the micro version number.
	 * 
	 * @since 1.13 Build 601
	 */
	public short getJServerVersionMicro()
	{
		return JServer.getAppVersionMicro();
	}
	
	/**
	 * Gets the codename of the remote JServer (the JServer API)
	 *
	 * @return the codename of the remote JServer.
	 * 
	 * @since 1.13 Build 601
    * 
    * @deprecated as of 2.0
	 */
	public String getJServerVersionCodeName()
	{
		return "";
	}

	/**
	 * Gets the type of build (e.g. Beta, Final, Debug and so on) of the remote JServer (the JServer API).
	 *
	 * @return the type of build.
	 * 
	 * @since 1.13 Build 601
	 */
	public String getJServerBuildType()
	{
		return JServer.getBuildType();
	}

	/**
	 * Gets the build number of the remote JServer (the JServer API).
	 *
	 * @return the build number.
	 * 
	 * @since 1.13 Build 601
	 */
	public short getJServerBuild()
	{
		return 1000;
	}

	/**
	 * Gets a string containing version information about the remote JServer (the JServer API).
	 *
	 * @return a string containing version information.
	 * 
	 * @since 1.13 Build 601
	 */
	public String getJServerVersionString()
	{
		return JServer.getVersionString();
	}

	/**
	 * Gets version information about the remote server (not the JServer version!).
	 *
	 * @return a string containing version information about the remote server.
	 * 
	 * @since 1.13 Build 601
	 */
	public String getServerVersion()
	{
		return this.adaptee.getServerVersion();
	}
}
