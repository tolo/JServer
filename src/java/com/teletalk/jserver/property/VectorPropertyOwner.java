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
package com.teletalk.jserver.property;

/**
 * Interface for owners of VectorProperty objects.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta
 */
public interface VectorPropertyOwner extends PropertyOwner
{
	/**
	 * This method is called when an external operation is called on the associated VectorProperty object.
	 * 
	 * @param operationName the internal name of the operation that was called.
	 * @param keys an array containing keys that was selected for the operation call.
	 */
	public void externalOperationCalled(String operationName, String[] keys);
}
