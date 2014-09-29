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
package com.teletalk.jserver.comm;

/**
 * Interface for classes that identifies (the address of) some sort of endpoint used in communication.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public interface EndPointIdentifier extends java.io.Serializable
{
	/**
	 * Gets (the address of) this EndPointIdentifier as a string.
	 * 
	 * @return this EndPointIdentifier as a string.
	 */
	public String getAddressAsString();
	
	/**
	 * Attempts to get an already existing EndPointIdentifier instance that contains the same information as 
	 * <code>this</code> object. If no such object exists, <code>this</code> object will be returned.
	 * 
	 * @return the shared EndPointIdentifier instance.
	 */
	public EndPointIdentifier getSharedInstance();
}
