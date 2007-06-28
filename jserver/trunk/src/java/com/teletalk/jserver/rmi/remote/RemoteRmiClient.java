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

/**
 * Remote interface for interacting with a RmiClient object.
 * 
 * @see com.teletalk.jserver.rmi.client.RmiClient
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public interface RemoteRmiClient extends Remote
{
	/**
	 * Returns true if this client is alive.
	 * 
	 * @return true if this client is alive, otherwise false.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public boolean isAlive() throws RemoteException;
	
	/**
	 * Disconnects the client from the server.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public void disconnectedFromServer() throws RemoteException;
	
	/**
	 * Identifies this client.
	 * 
	 * @return String object indentifying this client.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */	
	public String identify() throws RemoteException;
	
	/**
	 * Checkes whether or not this client wants to receive events.
	 * 
	 * @return boolean value.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public boolean receiveEvents() throws RemoteException;
	
	/**
	 * Method for receiving events from the server.
	 * 
	 * @param event the event.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public void receiveEvent(RemoteEvent event) throws RemoteException;
   
   /**
    * Gets the protocol version.
    * 
    * @exception RemoteException if there was an error during remote access of this method.
    * 
    * @since 2.0
    */
   public int getProtocolVersion() throws RemoteException;
}
