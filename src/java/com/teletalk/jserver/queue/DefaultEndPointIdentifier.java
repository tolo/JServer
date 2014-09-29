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

import java.net.InetAddress;

import com.teletalk.jserver.queue.legacy.DefaultQueueSystemCollaborationManager;
import com.teletalk.jserver.tcp.TcpEndPointIdentifier;

/**
 * This class is used to identify a TCP address, specifically for use in a queue system.<br>
 * <br>
 * Note: This class is part of the legacy queue system collaboration implementation (DefaultQueueSystemCollaborationManager).
 *  
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public final class DefaultEndPointIdentifier extends TcpEndPointIdentifier
{
	static final long serialVersionUID = 5711207016808672510L;
	
	/**
	 * Creates a new DefaultEndPointIdentifier which the <code>port</code> field initialized with the default queue system collaboration port.
	 * 
	 * @param address the ip address component of this DefaultEndPointIdentifier.
	 */
	public DefaultEndPointIdentifier(InetAddress address)
	{
		super(address, DefaultQueueSystemCollaborationManager.DEFAULT_QUEUESYSTEM_COLLABORATION_PORT);
	}
	
	/**
	 * Creates a new DefaultEndPointIdentifier which the <code>port</code> field initialized with the default queue system collaboration port.
	 * 
	 * @param address the ip address component of this DefaultEndPointIdentifier.
	 */
	public DefaultEndPointIdentifier(String address)
	{
		super(address, DefaultQueueSystemCollaborationManager.DEFAULT_QUEUESYSTEM_COLLABORATION_PORT);
	}
	
	/**
	 * Creates a new DefaultEndPointIdentifier.
	 * 
	 * @param address the ip address component of this DefaultEndPointIdentifier.
	 * @param port the port component of this DefaultEndPointIdentifier.
	 */
	public DefaultEndPointIdentifier(InetAddress address, int port)
	{
		super(address, port);
	}
	
	/**
	 * Creates a new DefaultEndPointIdentifier.
	 * 
	 * @param address the ip address component of this DefaultEndPointIdentifier.
	 * @param port the port component of this DefaultEndPointIdentifier.
	 */
	public DefaultEndPointIdentifier(String address, int port)
	{
		super(address, port);
	}
}
