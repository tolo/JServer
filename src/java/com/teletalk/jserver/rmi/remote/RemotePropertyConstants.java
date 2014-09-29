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

import com.teletalk.jserver.property.PropertyConstants;

/**
 * Interface containing constants for remote properties.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 2
 */
public interface RemotePropertyConstants extends PropertyConstants
{
	/** Type constant for custom properties. */
	public static final int CUSTOM_TYPE = 0;
	
	/** Type constant for BooleanProperty properties. */
	public static final int BOOLEAN_TYPE = 1;
	
	/** Type constant for DateProperty properties. */
	public static final int DATE_TYPE = 2;
	
	/** Type constant for EnumProperty properties. */
	public static final int ENUM_TYPE = 3;
	
	/** Type constant for NumberProperty properties. */
	public static final int NUMBER_TYPE = 4;
	
	/** Type constant for StringProperty properties. */
	public static final int STRING_TYPE = 5;
	
	/** Type constant for VectorProperty properties. */
	public static final int VECTOR_TYPE = 6;
	
	/** Type constant for MultiValueProperty properties. */
	public static final int MULTIVALUE_TYPE = 7;
}
