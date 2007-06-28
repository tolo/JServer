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
import java.util.Map;

/**
 * Remote interface for interacting with a JServerRmiHost object.
 * 
 * @see com.teletalk.jserver.rmi.JServerRmiHost
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public interface RemoteJServerRmiHost extends Remote
{
	/**	The name of the remote object. */
	public String remoteObjectName = "JServerRmiHost";
   
   /**
    * Method to connect a RmiClient to a JServer.
    * 
    * @param client a RemoteRmiClient object.
    * @param credentials the credentials needed to authenticate the client in the server (may be null). 
    * 
    * @return a RemoteJServerRmiInterface.
    * 
    * @exception RemoteException if there was an error during remote access of this method.
    * 
    * @since 2.0
    */
   public RemoteJServerRmiInterface connect(RemoteRmiClient client, Map credentials) throws RemoteException;
   
   
	
	/**
	 * Method to connect a RmiClient to a JServerRmiHost.
	 * 
	 * @param client a RemoteRmiClient object.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public RemoteJServer connect(RemoteRmiClient client) throws RemoteException;
	
	/**
	 * Method to disconnect a RmiClient from a JServerRmiHost.
	 * 
	 * @param client a RemoteRmiClient object.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public void disconnect(RemoteRmiClient client) throws RemoteException;
	
	/**
	 * Method to get the <code>java.rmi.server.codebase</code> property used by the remote server.
	 * 
	 * @return the value of the <code>java.rmi.server.codebase property</code>.
    * 
    * @deprecated as of 2.0 (due to bad spelling :))
	 */
	public String gerRmiCodeBase() throws RemoteException;
   
   /**
    * Method to get the <code>java.rmi.server.codebase</code> property used by the remote server.
    * 
    * @return the value of the <code>java.rmi.server.codebase property</code>.
    * 
    * @since 2.0
    */
   //public String getRmiCodeBase() throws RemoteException;
	
	/**
	 * Performs an alive check.
	 */
	public void aliveCheck() throws RemoteException;
      
   /**
    * 
    * @since 2.0
    */
   public int getProtocolVersion() throws RemoteException;
}
