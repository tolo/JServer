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

import com.teletalk.jadmin.proxy.LoggerProxy;

/**
 * Message class for requesting a list of loggers.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class LoggerRequestMessage
{
	/** The associated logger proxy object. */
	public LoggerProxy logger;
	
	/**
	 * Creates a new LoggerRequestMessage.
	 */
	public LoggerRequestMessage(LoggerProxy logger)
	{
		this.logger = logger;
	}
}
