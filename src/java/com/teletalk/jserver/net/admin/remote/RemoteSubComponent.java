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
 * Remote interface for interacting with a SubComponent object.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1.4 (20060503)
 */
public interface RemoteSubComponent
{
	/**
	 * Returns the name of the SubComponent.
	 * 
	 * @return a String containing the name.
	 */
	public String getName();
		
	/**
	 * Returns the full name of the SubComponent.
	 * 
	 * @return a String containing the full name.
	 */
	public String getFullName();
		
	/**
	 * Returns the status of the SubComponent.
	 * 
	 * @return int indicating the status.
	 */
	public int getStatus();

	/**
	 * Indicates whether or not the SubComponent is enabled.
	 * 
	 * @return boolean value inidcating if the state of the SubComponent is ENABLED.
	 */
	public boolean isEnabled();
	
   /**
    * Engages the SubSystem.
    */
   public boolean engage();
   
   /**
    * Reinitilizes the SubSystem.
    */
   public boolean reinitialize();
   
   /**
    * Shuts down the SubSystem.
    */
   public boolean shutDown();
}
