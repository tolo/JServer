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
package com.teletalk.jserver.pool;

/**
 * Interface for factory classes used for creation, validation and finalization of objects to be contained in an {@link ObjectPool}. 
 * 
 * @see ObjectPool
 * 
 * @since 2.0 Build 757
 * 
 * @author Tobias Löfstrand
 */
public interface PoolObjectFactory
{
	/** 
	 * Method to create an object.
	 * 
	 * @return the newly created object.
	 */
	public Object createObject() throws Exception;
	
	/**
	 * Method to validates an object.
	 * 
	 * @param obj the object to validate.
    * @param cleanUpValidation flag indicating if this method was invoked from the clean up method of the pool (<code>true<code>).
	 * 
	 * @return <code>true</code> if the object passed the validation, otherwise <code>false</code>.
	 */
	public boolean validateObject(Object obj, boolean cleanUpValidation);
	
	/**
	 * Method to finalize an object.
	 * 
	 * @param obj an object to be finalized.
	 */
	public void finalizeObject(Object obj);
}
