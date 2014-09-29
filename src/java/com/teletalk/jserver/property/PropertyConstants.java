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
 * Class containing constants used by the property classes.
 *  
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public interface PropertyConstants
{
   /**   Modification mode constant indicating that modification mode is not set. */
   public static final int MODIFICATION_MODE_NOT_SET = -1;
   
	/**	Modification mode constant for properties that are modifiable but reqire no restart. */
	public static final int MODIFIABLE_NO_RESTART = 0;
	
	/**	Modification mode constant for properties that are modifiable and reqire an owner restart. */
	public static final int MODIFIABLE_OWNER_RESTART = 1;
	
	/**	Modification mode constant for properties that are modifiable and reqire a server restart. */
	public static final int MODIFIABLE_SERVER_RESTART = 2;
	
	/**	Modification mode constant for properties that aren't modifiable. */
	public static final int NOT_MODIFIABLE = 3;
   
	
	/** Metadata key used for access of the description field of a property.  */
	public static final String PROPERTY_DESCRIPTION_KEY = "description";
	
	/** Metadata key used for access of the parsing description field of a property.  */
	public static final String PROPERTY_PARSINGDESCRIPTION_KEY = "parsingDescription";
	
	/** Metadata key used for access of the enumerations field of a VectorProperty.  */
	public static final String ENUMERATIONS_KEY = "enumerations";
	
	/** Metadata key used for access of the delimiters field of a MultiValueProperty.  */
	public static final String DELIMETER_CHARS_KEY = "delimiters";
	
   
	/** Constant for event type used by VectorPropertyEvent. */
	public static final byte VECTORPROPERTY_VALUE_ADDED = 0;
	
	/** Constant for event type used by VectorPropertyEvent. */
	public static final byte VECTORPROPERTY_VALUE_REMOVED = 1;
	
	/** Constant for event type used by VectorPropertyEvent. */
	public static final byte VECTORPROPERTY_VALUE_MODIFIED = 2;
	
	/** Constant for event type used by VectorPropertyEvent. */
	public static final byte VECTORPROPERTY_CLEAR = 3;
}
