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
package com.teletalk.jadmin.proxy;

import com.teletalk.jserver.rmi.remote.RemoteSubComponentData;

/**
 * This class implements a proxy object for a remote Logger object.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.0
 */
public class LoggerProxy extends SubComponentProxy
{
	/**
	 * Creates a new LoggerProxy.
	 */
	public LoggerProxy(JServerProxy jserverProxy, SubComponentProxy parent, RemoteSubComponentData data)	{
		super(jserverProxy, parent, data);
	}
}
