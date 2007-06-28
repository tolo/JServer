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
 * Interface representing a remote queue system.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public interface RemoteQueueSystem
{
	/**
	 * Marks this RemoteQueueSystem as destroyed and closes the link to the remote queue system it is connected to.
	 */
	public void destroy();
	
	/**
	 * Gets the address of the remote queue system represented by this object.
	 * 
	 * @return the address as an EndPointIdentifier object.
	 */ 
	public EndPointIdentifier getRemoteQueueSystemAddress();
	
	/**
	 * Gets a reference to the QueueSystemCollaborationManager associated with this RemoteQueueSystem.
	 * 
	 * @return a QueueSystemCollaborationManager object.
	 */
	public QueueSystemCollaborationManager getQueueSystemCollaborationManager();

	/**
	 * Method to dispatch a command to the remote queue system this object represents.
	 * 
	 * @param command the command to dispatch.
	 */
	public void dispatchCommand(QueueSystemCommand command);
	
	/**
	 * Cancels a command that is to be delivered.
	 * 
	 * @param command a command to be cancelled.
	 * 
	 * @return true if the specified command was successfully cancelled, otherwise false.
	 */
	public boolean cancelCommand(QueueSystemCommand command);
	
	/**
	 * Cancels a command which that is to be delivered.
	 * 
	 * @param command a command to be cancelled.
	 * 
	 * @return true if the specified command was successfully cancelled, otherwise false.
	 */
	public boolean cancelCommand(QueueSystemCommand command, boolean markAsSuccessful);
	
   /**
    * Gets all the pending commands to the remote queue system.
    * 
    * @since 2.1.2 (20060227)
    */
   public List getPendningCommands();
   
	/**
	 * Gets the length of the in queue of the remote queue system represented by this object.
	 * 
	 * @return the length of the remote in queue or -1 if no link has been established with the remote queue system this object represents.
	 */
	public int getRemoteInQueueLength();
	
	/**
	 * Gets the expected length of the in queue of the remote queue system represented by this object.<br>
	 * <br>
	 * This method uses an internal counter of how many items have been dispatched to the remote queue system to determine the 
	 * expected size of the remote in queue. This counter will be updated with the actual size every time a status message is received.
	 * 
	 * @return the length of the remote in queue or -1 if no link has been established with the remote queue system this object represents.
	 */
	public int getExpectedRemoteInQueueLength();
	
	/**
	 * Increments the counter for expected remote in queue length.<br>
	 * <br>
	 * This method uses an internal counter of how many items have been dispatched to the remote queue system to determine the 
	 * expected size of the remote in queue. This counter will be updated with the actual size every time a status message is received.
	 */
	public void incrementExpectedRemoteInQueueLength();
		
	/**
	 * Sets the value of the counter for expected remote in queue length.<br>
	 * <br>
	 * This method uses an internal counter of how many items have been dispatched to the remote queue system to determine the 
	 * expected size of the remote in queue. This counter will be updated with the actual size every time a status message is received.
	 * 
	 * @param length the new value.
	 */
	public void setExpectedRemoteInQueueLength(int length);

	/**
	 * Makes the calling thread wait until a link has been established, or until this RemoteQueueSystem
	 * is destroyed.
	 * 
	 * @exception InterruptedException if the current thread was interrupted while waiting.
	 */
	public boolean waitForLinkEstablished() throws InterruptedException;
	
	/**
	 * Makes the calling thread wait until this RemoteQueueSystem has established a link, the specified 
	 * maxwait time has ellapsed, or until the RemoteQueueSystem is destroyed.
	 * 
	 * @param maxWait the maximum wait time in milliseconds.
	 * 
	 * @exception InterruptedException if the current thread was interrupted while waiting.
	 */
	public boolean waitForLinkEstablished(long maxWait) throws InterruptedException;
	
	/**
	 * Gets the maximum length of the in queue of the remote queue system represented by this object.
	 * 
	 * @return the maximum length of the remote in queue.
	 */
	public int getRemoteInQueueMaxLength();
	
	/**
	 * Checks if the in queue of the remote queue system represented by this object is expected to be full or not.<br>
	 * <br>
	 * This method uses an internal counter of how many items have been dispatched to the remote queue system to determine the 
	 * expected size of the remote in queue. This counter will be updated with the actual size every time a status message is received.
	 * 
	 * @return <code>true</code> if the remote in queue is full, otherwise <code>false</code>. Note that the return value of this method 
	 * will be <code>true</code> if there is no link established with the remote queue system.
	 */
	public boolean isExpectedRemoteInQueueFull();
	
	/**
	 * Checks if the in queue of the remote queue system represented by this object is full or not.
	 * 
	 * @return true if the remote in queue is full, otherwise false.
	 */
	public boolean isRemoteInQueueFull();
	
	/**
	 * Checks if the in queue of the remote queue system represented by this object is blocked or not.
	 * 
	 * @return true if the remote in queue is blocked, otherwise false.
	 */
	public boolean isRemoteInQueueBlocked();
	
   /**
    * Checks whether or not a connection exists with the remote queue system this object represents. This method should 
    * return true if a connection exists even though final handshaking and synchronization may not be completed. 
    * 
    * @return true if a connection exists, otherwise false.
    * 
    * @since 2.1.2 (20060224)
    */
   public boolean isConnected();
   
	/**
	 * Checks whether or not a link has been established with the remote queue system this object represents.
	 * 
	 * @return true if a link has been established, otherwise false.
	 */
	public boolean isLinkEstablished();

	/**
	 * Gets metadata about this remote queue system.
	 * 
	 * @return a QueueSystemMetaData object.
	 */
	public QueueSystemMetaData getRemoteQueueSystemMetaData();
	
	/**
	 * Called when a status command, containing metadata, is received from the remote queue system this 
	 * object represents. This method should also be called when a link is first established with the queue system.
	 * 
	 * @param metaData a QueueSystemMetaData object.
	 * 
	 * @see QueueSystemMetaData
	 */
	public void remoteQueueSystemMetaDataUpdated(QueueSystemMetaData metaData);
}
