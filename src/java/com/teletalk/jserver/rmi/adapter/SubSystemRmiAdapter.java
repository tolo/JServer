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
import java.util.ArrayList;
import java.util.List;

import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.rmi.remote.RemoteSubSystem;

/**
 * Adapter for remote interaction with a SubSystem object.
 * 
 * @see com.teletalk.jserver.SubSystem
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public class SubSystemRmiAdapter extends SubComponentRmiAdapter implements RemoteSubSystem
{
   static final long serialVersionUID = 7878255540870433861L;
   
	/** Reference to a SubSystem object. */
	protected SubSystem adaptee;
	
	/**
	 * Constructs a new SubSystemRmiAdapter object.
	 * 
	 * @param adaptee a SubSystem adaptee object.
	 * 
	 * @exception RemoteException if there was an error creating this SubSystemRmiAdapter.
	 */
	public SubSystemRmiAdapter(SubSystem adaptee) throws RemoteException
	{
		super(adaptee);
		this.adaptee = adaptee;
	}
	
	/**
	 * Makes the calling thread wait until the SubSystem has changed state to
	 * DOWN or the given maxWait time has ellapsed.
	 * 
	 * @param maxWait the maximum wait time in milliseconds.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final void waitForDown(long maxWait) throws RemoteException, InterruptedException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubSystemRmiAdapter", "waitForDown(" + maxWait + ")");
			
			adaptee.waitForDown(maxWait);
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
	 * Makes the calling thread wait until the SubSystem has changed state to
	 * parameter status or the given maxWait time has ellapsed.
	 * 
	 * @param status the status to wait for.
	 * @param maxWait the maximum wait time in milliseconds.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public void waitForStatus(int status, long maxWait) throws RemoteException, InterruptedException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubSystemRmiAdapter", "waitForStatus(" + status + ", " + maxWait +  ")");
			
			adaptee.waitForStatus(status, maxWait);
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
	 * Checkes to see if the SubSystem is a keysystem or not.
	 * 
	 * @return booleam indicating if the SubSystem is a keysystem or not.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final boolean isKeySystem() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubSystemRmiAdapter", "isKeySystem()");
			
			return adaptee.isKeySystem();
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
	 * Checkes to see if the SubSystem is at the top of the system hierchy.
	 * 
	 * @return booleam indicating if the SubSystem is the topsystem.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final boolean isTopsystem() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubSystemRmiAdapter", "isTopsystem()");
			
			return adaptee.isTopsystem();	
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
	 * Returns the amount of time that has elapsed since the SubSystem was started.
	 * 
	 * @return long indicating the time in milliseconds.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final long getUpTime() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubSystemRmiAdapter", "getUpTime()");
			
			return adaptee.getUpTime();
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
	 * Returns the child SubComponents and SubSystems of
	 * the SubSystem.
	 * 
	 * @return Vector containing the RemoteSubComponent and RemoteSubSystems.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final List getAllSubcomponents() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubSystemRmiAdapter", "getAllSubcomponents()");
					
			ArrayList all = new ArrayList();
		
			//all.addAll(getSubSystems());
		
			all.addAll(getSubComponents());
		
			return all;
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
	 * Returns a recursive listing of all child SubComponents and SubSystems of
	 * the SubSystem.
	 * 
	 * @return Vector containing the RemoteSubComponent and RemoteSubSystems.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final List getAllSubcomponentsRecursively() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubSystemRmiAdapter", "getAllSubcomponentsRecursively()");
					
			ArrayList result = new ArrayList();
			List recursively = adaptee.getSubComponentTree();
			SubComponent s;
		
			for(int i=0; i<recursively.size(); i++)
			{
				s = (SubComponent)recursively.get(i);
				
				result.add(s.getRmiAdapter());
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
	 * Returns the child SubSystems of the SubSystem.
	 * 
	 * @return Vector containing RemoteSubSystems.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final List getSubSystems() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubSystemRmiAdapter", "getSubSystems()");
					
			ArrayList result = new ArrayList();
			List subs = adaptee.getSubSystems();
					
			for(int i=0; i<subs.size(); i++)
			{
				result.add(((SubSystem)subs.get(i)).getRmiAdapter());
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
	 * Gets the SubSystems with the specified name contained in this SubSystems.
	 * 
	 * @param name the name of the SubSystems.
	 * 
	 * @return a RemoteSubSystem or null if none was found.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public RemoteSubSystem getSubSystem(String name) throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubSystemRmiAdapter", "getSubSystem(" + name +")");
					
			SubSystem s = adaptee.getSubSystem(name);
			RemoteSubSystem remote = null;
		
			if(s != null)
			{
				RmiAdapter r = s.getRmiAdapter();
				
				if(remote instanceof RemoteSubSystem)
					remote = (RemoteSubSystem)r;
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
	 * Returns a recursive listing of all child SubSystem of the SubSystem.
	 * 
	 * @return a Vector containing RemoteSubSystems.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public final List getSubSystemsRecursively() throws RemoteException
	{
		try
		{
			JServerRmiAdapter.logMethodCall(adaptee.getFullName() + ".SubSystemRmiAdapter", "getSubSystemsRecursively()");			
		
			ArrayList result = new ArrayList();
			List recursively = adaptee.getSubSystems();
					
			for(int i=0; i<recursively.size(); i++)
			{
				result.add(((SubSystem)recursively.get(i)).getRmiAdapter());
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
}
