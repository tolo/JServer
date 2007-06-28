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

/**
 * Remote interface for interacting with a JServerRmiAdapter object.
 * 
 * @see com.teletalk.jserver.rmi.adapter.JServerRmiAdapter
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public interface RemoteJServer extends RemoteSubSystem
{
	/**
	 * Restarts the server.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public void restartServer() throws RemoteException;

	/**
	 * Shuts down the server.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public void shutDownServer() throws RemoteException;
	
	/**
	 * Kills the server.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public void killServer() throws RemoteException;
	
	/**
	 * Returns a remote reference to a Administration object.
	 * 
	 * @return an RemoteAdministration object.
	 * 
	 * @see com.teletalk.jserver.rmi.Administration
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public RemoteAdministration getAdministration() throws RemoteException;
	
	/**
	 * Gets a tree of objects containing information about the components of this server.
	 * 
	 * @return a RemoteSubSystemData object.
	 */
	public RemoteSubSystemData getSystemTreeData() throws RemoteException;
	
	/**
	 * Performs a search for a Property in this server and returns a RemoteProperty interface to it.
	 * 
	 * @param fullName the name of the Property.
	 * 
	 * @return a RemoteProperty interface or null if no property was found.
	 */
	public RemoteProperty findRemoteProperty(String fullName) throws RemoteException;
	
	/**
	 * Performs a search for a SubComponent in this server and returns a RemoteSubComponent interface to it.
	 * 
	 * @param fullName the name of the SubComponent.
	 * 
	 * @return a RemoteSubComponent interface or null if no subcomponent was found.
	 */
	public RemoteSubComponent findRemoteSubComponent(String fullName) throws RemoteException;
	
	/**
	 * Performs a search for a SubSystem in this server and returns a RemoteSubSystem interface to it.
	 * 
	 * @param fullName the name of the SubSystem.
	 * 
	 * @return a RemoteSubSystem interface or null if no subsystem was found.
	 */
	public RemoteSubSystem findRemoteSubSystem(String fullName, boolean createDefalt) throws RemoteException;
	
	/**
	 * Gets the major version number of the remote JServer (the JServer API). If the version is 1.23 then major version is 1.
	 *
	 * @return the major version number.
	 * 
	 * @since 1.13 Build 601
	 */
	public short getJServerVersionMajor() throws RemoteException;

	/**
	 * Gets the minor version number of the remote JServer (the JServer API). If the version is 1.23 then minor version is 2.
	 *
	 * @return the minor version number.
	 * 
	 * @since 1.13 Build 601
	 */
	public short getJServerVersionMinor() throws RemoteException;
	
	/**
	 * Gets the micro version number of the remote JServer (the JServer API). If the version is 1.23 then micro version is 3.
	 *
	 * @return the micro version number.
	 * 
	 * @since 1.13 Build 601
	 */
	public short getJServerVersionMicro() throws RemoteException;
		
	/**
	 * Gets the codename of the remote JServer (the JServer API)
	 *
	 * @return the codename of the remote JServer.
	 * 
	 * @since 1.13 Build 601
    * 
    * @deprecated as of 2.0
	 */
	public String getJServerVersionCodeName() throws RemoteException;
	
	/**
	 * Gets the type of build (e.g. Beta, Final, Debug and so on) of the remote JServer (the JServer API).
	 *
	 * @return the type of build.
	 * 
	 * @since 1.13 Build 601
	 */
	public String getJServerBuildType() throws RemoteException;
	
	/**
	 * Gets the build number of the remote JServer (the JServer API).
	 *
	 * @return the build number.
	 * 
	 * @since 1.13 Build 601
    * 
    * @deprecated as of 2.0
	 */
	public short getJServerBuild() throws RemoteException;

	/**
	 * Gets a string containing version information about the remote JServer (the JServer API).
	 *
	 * @return a string containing version information.
	 * 
	 * @since 1.13 Build 601
	 */
	public String getJServerVersionString() throws RemoteException;

	/**
	 * Gets version information about the remote server (not the JServer version!).
	 *
	 * @return a string containing version information about the remote server.
	 * 
	 * @since 1.13 Build 601
	 */
	public String getServerVersion() throws RemoteException;
}
