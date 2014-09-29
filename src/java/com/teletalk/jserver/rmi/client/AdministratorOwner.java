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
package com.teletalk.jserver.rmi.client;

import java.awt.Frame;

import com.teletalk.jserver.rmi.remote.RemoteEvent;

/**
 * Interface used by classes that own an Administrator object.
 * 
 * @see Administrator
 *  
 * @author Tobias Löfstrand
 * 
 * @since Beta
 */
public interface AdministratorOwner
{
	/**
	 * Run when the Administrator gets disconnected from the Server.
	 */
	public void administratorDisconnectedFromServer();
	
	/**
	 * Method for receiving events from the server.
	 * 
	 * @param event the received event.
	 */
	public void receiveEvent(RemoteEvent event);
	
	/**
	 * Records a message to a log (the eventlog in JAdmin).
	 * 
	 * @param msg a message.
	 */
	public void recordMessage(String msg);
	
	/**
	 * Gets the main frame used by the administration client. 
	 * Non-gui implementations may return <code>null</code>.¨
	 * 
	 * @return a java.awt.Frame object representing the main frame of the administration application.
	 */
	public Frame getMainFrame();
	
	/**
	 * Gets the name of the administration application implementing this interface, for instance "<b>JAdmin</b>".
	 * 
	 * @return the name of the administraion application implementing this interface.
	 * 
	 * @since 1.13 Build 601
	 */
	public String getAdministrionApplicationName();
	
	/**
	 * Gets the major version of the administration application implementing this interface. If the version is 1.23 then major version is 1.
	 * 
	 * @return the major version number.
	 * 
	 * @since 1.13 Build 601
	 */
	public short getAdministrionApplicationVersionMajor();
	
	/**
	 * Gets the minor version of the administration application implementing this interface. If the version is 1.23 then minor version is 2.
	 * 
	 * @return the minor version number.
	 * 
	 * @since 1.13 Build 601
	 */
	public short getAdministrionApplicationVersionMinor();
	
	/**
	 * Gets the micro version of the administration application implementing this interface. If the version is 1.23 then micro version is 3.
	 * 
	 * @return the micro version number.
	 * 
	 * @since 1.13 Build 601
	 */
	public short getAdministrionApplicationVersionMicro();
	
	/**
	 * Gets a string containing version information about the administration application implementing this interface.
	 * 
	 * @return the name of the administraion application implementing this interface.
	 * 
	 * @since 1.13 Build 601
	 */
	public String getAdministrionApplicationVersionString();
	
	/**
	 * Gets the build of the administration application implementing this interface, for instance "<b>601</b>".
	 * 
	 * @return the name of the administraion application implementing this interface.
	 * 
	 * @since 1.13 Build 601
    * 
    * @deprecated as of 2.0.1
	 */
	public String getAdministrionApplicationBuild();
}
