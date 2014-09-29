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

import com.teletalk.jserver.log.LogMessage;

/**
 * Class representing a remote log event.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 * 
 * @deprecated as of 2.0. This class only exists to enable backwards compatability in JAdmin.
 */
public class RemoteLogEvent extends RemoteEvent
{
   static final long serialVersionUID = -4173257464063948213L;

	/**
	 * Constructs a new RemoteLogEvent.
	 * 
	 * @param logMsg a LogMessage that is to be contained in this RemoteLogEvent.
	 * 
	 * @see com.teletalk.jserver.log.LogMessage
	 */
	public RemoteLogEvent(LogMessage logMsg)
	{
		super(logMsg);
	}
	
	/**
	 * Gets the LogMessage contained in this RemoteLogEvent.
	 * 
	 * @return a LogMessage.
	 * 
	 * @see com.teletalk.jserver.log.LogMessage
	 */
	public LogMessage getLogMessage()
	{
		return (LogMessage)getParam();
	}

	/**
	 * Returns a String object representing of this RemoteLogEvent.
	 * 
	 * @return a String representation of this RemoteLogEvent.
	 */
	public String toString()
	{
		return "RemoteLogEvent(" + getLogMessage().toString();
	}
}
