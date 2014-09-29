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
package com.teletalk.jserver.queue.command;

import com.teletalk.jserver.queue.RemoteQueueSystem;
import com.teletalk.jserver.queue.legacy.DefaultQueueSystemEndPoint;
import com.teletalk.jserver.queue.legacy.DefaultQueueSystemEndPointProxy;

/**
 * Command class used for DefaultQueueSystemEndPoint disconnection.<br>
 * <br>
 * Note: This class is part of the legacy queue system collaboration implementation (DefaultQueueSystemCollaborationManager).
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public final class DefaultDisconnectCommand extends Command
{
	/** The serial version id of this class. */
	static final long serialVersionUID = 3102587782464108038L;
																						 
	public void execute(RemoteQueueSystem remoteQueueSystem)
	{
		DefaultQueueSystemEndPoint endPoint =  ((DefaultQueueSystemEndPointProxy)remoteQueueSystem).getEndPoint();
		
		if(endPoint != null)
			endPoint.disconnectionCommandReceived();
	}
	
	/**
	 * Gets a string representation of this DefaultQueueSystemEndPoint.
	 * 
	 * @return a string representation of this DefaultQueueSystemEndPoint.
	 */
	public String toString()
	{
		return this.getClass().getName();
	}
}
