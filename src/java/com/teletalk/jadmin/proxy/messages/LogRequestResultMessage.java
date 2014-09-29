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
package com.teletalk.jadmin.proxy.messages;

import java.util.List;

/** 
 * Message response class for the request class LogRequestMessage.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class LogRequestResultMessage
{
	public LogRequestMessage request;
	public List logMessages;
	
	/**
	 * Creates a new LogRequestResultMessage.
	 */
	public LogRequestResultMessage(LogRequestMessage request, List logMessages)
	{
		this.request = request;
		this.logMessages = logMessages;
	}
}
