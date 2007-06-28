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
package com.teletalk.jserver.rmi;

import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.List;

import com.teletalk.jserver.rmi.client.CustomAdministrationPanel;
import com.teletalk.jserver.rmi.remote.RemoteAdministration;

/**
 * The Administration class is a server side object where it is possible to register classes that are to be used as 
 * custom administration panels in the administration tool.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class Administration extends UnicastRemoteObject implements RemoteAdministration
{
   static final long serialVersionUID = 4098358220704136942L;

	private final ArrayList customAdminClasses;
	private final ArrayList customConstructorParameters;
	
	/**
	 * Creates a new Administration object.
	 * 
	 * @exception RemoteException if there was an error creating the Administration object.
	 */
	public Administration() throws RemoteException
	{
		super(RmiManager.getRmiManager().getExportAddresses()[0].getPort());
		
		customAdminClasses = new ArrayList();
		customConstructorParameters = new ArrayList();
	}
	
	/**
	 * Gets the names of all custom administration panel classes contained in this Administration object.
	 * 
	 * @return a List containing class names.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */	
	public List getAdmPanelClassNames() throws RemoteException
	{
		ArrayList classNames = new ArrayList();
		ArrayList customAdminClassesCopy;
		
		synchronized(this)
		{
			customAdminClassesCopy = new ArrayList(customAdminClasses);;
		}
		
		for(int i=0; i<customAdminClassesCopy.size(); i++)
			classNames.add( ((Class)customAdminClassesCopy.get(i)).getName() );
		
		return classNames;
	}
	
	/**
	 * Gets the constructor parameters for all custom administration classes contained in this Administration object.
	 * 
	 * @return a List containing arrays of constructorparameters.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public synchronized List getAdmPanelConstructorParameters() throws RemoteException
	{
		return (ArrayList)customConstructorParameters.clone();
	}
	
	/**
	 * Adds a custom administrationpanel class to this Administration object. All constructorparameters contained in parameter 
	 * constructorParameters must either implement java.io.Serializable or java.rmi.Remote, or be an instance of any of the classes and 
	 * interfaces in the JServer pagages com.teletalk.jserver.rmi.adapter and com.teletalk.jserver.rmi.remote.
	 * 
	 * @param customAdmPanelClass the administrationpanel class to be added.
	 * @param constructorParameters an array of constructor parameters for creating an object of customAdmPanelClass.
	 * 
	 * @exception IllegalArgumentException if the specified class doesn't inherit CustomAdministrationPanel or if any of the constructor parameters doesn't 
	 * implement java.io.Serializable or java.rmi.Remote.
	 * 
	 * @see com.teletalk.jserver.rmi.client.CustomAdministrationPanel
	 */
	public synchronized void addCustomAdmPanel(Class customAdmPanelClass, Object[] constructorParameters) throws IllegalArgumentException
	{
		if( ((Class)CustomAdministrationPanel.class).isAssignableFrom(customAdmPanelClass) ) 
		{
				customAdminClasses.add(customAdmPanelClass);
					
				for(int i=0; i<constructorParameters.length; i++)
				{
					if( !(constructorParameters[i] instanceof java.io.Serializable || constructorParameters[i] instanceof java.rmi.Remote) )
						throw new IllegalArgumentException("All constructor parameters must implement java.io.Serializable or java.rmi.Remote!");
			   }
					
				customConstructorParameters.add(constructorParameters);
		}
		else 
			throw new IllegalArgumentException("Parameter helpPanelClass must inherit CustomAdministrationPanel!");
	}
}
