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
package com.teletalk.jserver.queue;

import java.util.List;

import com.teletalk.jserver.comm.EndPointIdentifier;
import com.teletalk.jserver.queue.command.QueueSystemCommand;

/**
 * The responsibility of the abstract class QueueSystemCollaborationManager is to handle the network communication 
 * between queuesystems. This class is also responsible for creating <code>RemoteQueueSystem</code> objects to 
 * represent remote queue systems.
 * 
 * @see RemoteQueueSystem
 * @see QueueManager
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1 
 */
public interface QueueSystemCollaborationManager 
{
	/**
	 * Performs an internal check of this QueueSystemCollaborationManager.
	 * 
	 * @return true if this QueueSystemCollaborationManager is OK, otherwise false.
	 */
	public boolean performCheck();
   
   /**
    * Performs start up synchronization.
    * 
    * @param addresses list of all sender/receiver addresses of all queue items.  
    * 
    * @since 2.1 (20051221)
    */
   public void performStartupSynchronization(List addresses);
	
	/**
	 * Gets a RemoteQueueSystem object representing a remote queue system at the
	 * specified address.<br> If there currently is no is no RemoteQueueSystem object available for the address a new one will be created.
	 * 
	 * @param address the address of the remote queue system (Must be an instance of TcpEndPointIdentifier or one of its subclasses).
	 *  
	 * @return a RemoteQueueSystem object, <code>null</code> if the address was invalid or there was an error creating a RemoteQueueSystem object.
	 */
	public RemoteQueueSystem getRemoteQueueSystem(EndPointIdentifier address);
		
	/**
	 * Gets a RemoteQueueSystem object representing a remote queue system at the
	 * specified address.<br>
	 * 
	 * @param address the address of the remote queue system (Must be an instance of TcpEndPointIdentifier or one of its subclasses).
	 * @param create boolean flag indicating if a new RemoteQueueSystem object will be created if there currently is none available for the specified address.
	 *  
	 * @return a RemoteQueueSystem object, <code>null</code> if the address was invalid or there was an error creating a RemoteQueueSystem object.
	 */
	public RemoteQueueSystem getRemoteQueueSystem(EndPointIdentifier address, boolean create);
	
	/**
	 * Gets a list of all remote queue systems connected to this queue system.
	 * 
	 * @return a list of RemoteQueueSystem objects.
	 */
	public List getRemoteQueueSystems();
   
   /**
    * Method to dispatch a command to a remote queue system at the address specified in the command.
    * 
    * @param command the command to dispatch.
    * 
    * @since 2.1 (20051221)
    */
   public void dispatchCommand(QueueSystemCommand command); 
   
	/**
	 * Called to handle an incoming command from a remote queue system. 
	 * 
	 * @param command a command to be handled.
	 */
	public void handleQueueSystemCommand(QueueSystemCommand command);
}
