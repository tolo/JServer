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
package com.teletalk.jserver.net.admin.remote;

/**
 * Remote interface for interacting with a SubSystem object.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1.4 (20060503)
 */
public interface RemoteSubSystem extends RemoteSubComponent
{
	/**
	 * Checkes to see if the SubSystem is a keysystem or not.
	 * 
	 * @return booleam indicating if the SubSystem is a keysystem or not.
	 */
	public boolean isKeySystem();
	
	/**
	 * Checkes to see if the SubSystem is at the top of the system hierchy.
	 * 
	 * @return booleam indicating if the SubSystem is the topsystem.
	 */
	public boolean isTopsystem();
	
	/**
	 * Returns the amount of time that has elapsed since the SubSystem was started.
	 * 
	 * @return long indicating the time in milliseconds.
	 */
	public long getUpTime();
}
