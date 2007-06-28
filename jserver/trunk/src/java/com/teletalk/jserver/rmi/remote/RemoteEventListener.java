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

/**
 * Listener interface used for receiving remote events from a server. RemoteEventListener objects 
 * must be registered registered in a {@link com.teletalk.jserver.rmi.client.RmiClient} (using the method 
 * {@link com.teletalk.jserver.rmi.client.RmiClient#registerRemoteEventListener(RemoteEventListener)}) object 
 * to be able to receive events.<br>
 * <br>
 * If this interface is implemented in a subclass of {@link com.teletalk.jserver.rmi.client.CustomAdministrationPanel},  
 * event listener registration will be performed automatically by <b>JAdmin</b> when the panel class is loaded.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.13 Build 600
 */
public interface RemoteEventListener
{
	/**
	 * Listener method called when a remote event is received from the server.
	 * 
	 * @param event the received event object.
	 */
	public void remoteEventReceived(RemoteEvent event);
}