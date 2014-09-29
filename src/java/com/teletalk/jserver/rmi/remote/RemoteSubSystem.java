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

/**
 * Remote interface for interacting with a SubSystemRmiAdapter object.
 * 
 * @see com.teletalk.jserver.rmi.adapter.SubSystemRmiAdapter
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public interface RemoteSubSystem extends RemoteSubComponent
{
	/**
	 * Makes the calling thread wait until the SubSystem has changed state to
	 * DOWN or the given maxWait time has ellapsed.
	 * 
	 * @param maxWait the maximum wait time in milliseconds.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public void waitForDown(long maxWait) throws RemoteException, InterruptedException;
	
	/**
	 * Makes the calling thread wait until the SubSystem has changed state to
	 * parameter status or the given maxWait time has ellapsed.
	 * 
	 * @param status the status to wait for.
	 * @param maxWait the maximum wait time in milliseconds.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public void waitForStatus(int status, long maxWait) throws RemoteException, InterruptedException;
	
	/**
	 * Checkes to see if the SubSystem is a keysystem or not.
	 * 
	 * @return booleam indicating if the SubSystem is a keysystem or not.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public boolean isKeySystem() throws RemoteException;
	
	/**
	 * Checkes to see if the SubSystem is at the top of the system hierchy.
	 * 
	 * @return booleam indicating if the SubSystem is the topsystem.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public boolean isTopsystem() throws RemoteException;
	
	/**
	 * Returns the amount of time that has elapsed since the SubSystem was started.
	 * 
	 * @return long indicating the time in milliseconds.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public long getUpTime() throws RemoteException;
	
	/**
	 * Engages the SubSystem.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	//public boolean engage() throws RemoteException;
	
	/**
	 * Reinitilizes the SubSystem.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	//public boolean reinitialize() throws RemoteException;
	
	/**
	 * Shuts down the SubSystem.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	//public boolean shutDown() throws RemoteException;
	
	/**
	 * Enables the SubSystem.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	//public boolean enable() throws RemoteException;

	/**
	 * Disables the SubSystem.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	//public boolean disable() throws RemoteException;
	
	/**
	 * Returns the child SubComponents and SubSystems of
	 * the SubSystem.
	 * 
	 * @return Vector containing the RemoteSubComponent and RemoteSubSystems.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public List getAllSubcomponents() throws RemoteException;
	
	/**
	 * Returns a recursive listing of all child SubComponents and SubSystems of
	 * the SubSystem.
	 * 
	 * @return Vector containing the RemoteSubComponent and RemoteSubSystems.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public List getAllSubcomponentsRecursively() throws RemoteException;
		
	/**
	 * Returns the child SubSystems of the SubSystem.
	 * 
	 * @return Vector containing RemoteSubSystems.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public List getSubSystems() throws RemoteException;
	
	/**
	 * Gets the SubSystems with the specified name contained in this SubSystems.
	 * 
	 * @param name the name of the SubSystems.
	 * 
	 * @return a RemoteSubSystem or null if none was found.
	 */
	public RemoteSubSystem getSubSystem(String name) throws RemoteException;

	/**
	 * Returns a recursive listing of all child SubSystem of the SubSystem.
	 * 
	 * @return a Vector containing RemoteSubSystems.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public List getSubSystemsRecursively() throws RemoteException;
}
