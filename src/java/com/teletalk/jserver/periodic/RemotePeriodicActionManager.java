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
package com.teletalk.jserver.periodic;

import java.rmi.RemoteException;

import com.teletalk.jserver.rmi.remote.RemoteSubSystem;

/**
 * RMI interface to make it possible to call methods in the class PeriodicActionManager from the custom 
 * administration panel <code>ActionsInfoPanel</code>.<br>
 * <br>
 * Note: Not currently in use.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.2
 */
public interface RemotePeriodicActionManager extends RemoteSubSystem
{
	/**
	 * Gets a String matrix containing name, status and description for each registered action.
	 * 
	 * @return a String matrix containing name, status and description for each registered action.
	 */
	public String[][] getActionDescriptions() throws RemoteException;
	
	/**
	 * Exceutes the action with the specified name if it is not already executing.
	 * 
	 * @param actionName the name of the action to execute.
	 */
	public void executeAction(String actionName) throws RemoteException;
}
