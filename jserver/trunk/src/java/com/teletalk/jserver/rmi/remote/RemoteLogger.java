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

import com.teletalk.jserver.log.LogFilter;

/**
 * Remote interface for interacting with a LoggerRmiAdapter.
 *  * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 * 
 * @deprecated as of 2.0. This class only exists to enable backwards compatability in JAdmin.
 */
public interface RemoteLogger extends RemoteSubComponent
{
	/**
	 * Returns the names of all logs used by this RemoteLogger. 
	 * 
	 * @return Vector containing logs.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public List getAvailableLogs() throws RemoteException;
	
	/**
	 * Gets LogMessages from the given log that passes the given filter. 
	 * 
	 * @param log the name of the log.
	 * @param filter a LogFilter.
	 * 
	 * @return Vector containing LogMessages.
	 * 
	 * @see com.teletalk.jserver.log.LogFilter
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public List getLogMessages(String log, LogFilter filter) throws RemoteException;
}
