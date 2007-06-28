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

import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Remote interface for the Administration class.
 * 
 * @see com.teletalk.jserver.rmi.Administration
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public interface RemoteAdministration extends Remote
{
	/**
	 * Gets the names of all custom administration panel classes contained in the remote Administration object.
	 * 
	 * @return a List containing class names.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public List getAdmPanelClassNames() throws RemoteException;
	
	/**
	 * Gets the constructor parameters for all custom administration panel classes contained in the remote Administration object.
	 * 
	 * @return a List containing arrays of constructorparameters.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public List getAdmPanelConstructorParameters() throws RemoteException;
}
