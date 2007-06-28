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
package com.teletalk.jserver.net.admin.remote;

/**
 * Remote interface for interacting with a JServer object.
 *  
 * @author Tobias Löfstrand
 * 
 * @since 2.1.4 (20060503)
 */
public interface RemoteJServer
{
	/**
	 * Restarts the server.
	 */
	public void restartJServer();

	/**
	 * Shuts down the server.
	 */
	public void stopJServer();
	
	/**
	 * Kills the server.
	 */
	public void killJServer();
	
	/**
	 * Gets the major version number of the remote JServer (the JServer API). If the version is 1.23 then major version is 1.
	 *
	 * @return the major version number.
	 */
	public short getAppVersionMajor();

	/**
	 * Gets the minor version number of the remote JServer (the JServer API). If the version is 1.23 then minor version is 2.
	 *
	 * @return the minor version number.
	 */
	public short getAppVersionMinor();
	
	/**
	 * Gets the micro version number of the remote JServer (the JServer API). If the version is 1.23 then micro version is 3.
	 *
	 * @return the micro version number.
	 */
	public short getAppVersionMicro();
	
	/**
	 * Gets the type of build (e.g. Beta, Final, Debug and so on) of the remote JServer (the JServer API).
	 *
	 * @return the type of build.
	 */
	public String getBuildType();

	/**
	 * Gets a string containing version information about the remote JServer (the JServer API).
	 *
	 * @return a string containing version information.
	 */
	public String getVersionString();

	/**
	 * Gets version information about the remote server (not the JServer version!).
	 *
	 * @return a string containing version information about the remote server.
	 */
	public String getServerVersion();
}
