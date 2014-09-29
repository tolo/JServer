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

import java.io.Serializable;

/**
 * Baseclass for all remote events.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta
 */
public class RemoteEvent implements Serializable
{
	static final long serialVersionUID = -5247804408075609477L;

	/** Event parameter. */
	protected final Object param;
	
	/**
	 * Constructs a new RemoteEvent.
	 * 
	 * @param param an event parameter.
	 */
	public RemoteEvent(Object param)
	{
		this.param = param;
	}
	
	/**
	 * Returns the parameter of this RemoteEvent.
	 * 
	 * @return the parameter.
	 */
	public final Object getParam()
	{
		return param;	
	}
	
	/**
	 * Returns a String object representing of this RemoteEvent.
	 * 
	 * @return a String representation of this RemoteEvent.
	 */
	public String toString()
	{
		return "RemoteEvent(" + ( (param != null) ? param.toString() : null) + ")";
	}
}
