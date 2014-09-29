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
package com.teletalk.jserver.tcp;

import com.teletalk.jserver.property.IpAndPortProperty;
import com.teletalk.jserver.property.PropertyOwner;

/**
 * Property class for handling one or more TcpEndPointIdentifier values.
 * 
 * @see TcpEndPointIdentifier
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 2
 */
public final class TcpEndPointIdentifierProperty extends IpAndPortProperty
{
	/**
	 * Creates a new TcpEndPointIdentifierProperty object.
	 * 
	 * @param owner the owner of this TcpEndPointIdentifierProperty.
	 * @param name the name of this TcpEndPointIdentifierProperty.
	 * @param addressString the default value of this TcpEndPointIdentifierProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 */
	public TcpEndPointIdentifierProperty(PropertyOwner owner, String name, String addressString, int modificationMode)
	{
		super(owner, name, addressString, modificationMode, TcpEndPointIdentifier.getIpAndPortEndPointIdentifierParsingConstructor());
	}
	
	/**
	 * Creates a new TcpEndPointIdentifierProperty object.
	 * 
	 * @param owner the owner of this TcpEndPointIdentifierProperty.
	 * @param name the name of this TcpEndPointIdentifierProperty.
	 * @param addressString the default value of this TcpEndPointIdentifierProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
	 */
	public TcpEndPointIdentifierProperty(PropertyOwner owner, String name, String addressString, int modificationMode, boolean persistent)
	{
		super(owner, name, addressString, modificationMode, persistent, TcpEndPointIdentifier.getIpAndPortEndPointIdentifierParsingConstructor());
	}
	
	/**
	 * Creates a new TcpEndPointIdentifierProperty object that isn't modifiable.
	 * 
	 * @param owner the owner of this TcpEndPointIdentifierProperty.
	 * @param name the name of this TcpEndPointIdentifierProperty.
	 * @param addressString the default value of this TcpEndPointIdentifierProperty.
	 */
	public TcpEndPointIdentifierProperty(PropertyOwner owner, String name, String addressString)
	{
		super(owner, name, addressString, TcpEndPointIdentifier.getIpAndPortEndPointIdentifierParsingConstructor());
	}
	
	/**
	 * Creates a new TcpEndPointIdentifierProperty object.
	 * 
	 * @param owner the owner of this TcpEndPointIdentifierProperty.
	 * @param name the name of this TcpEndPointIdentifierProperty.
	 * @param address the default value of this TcpEndPointIdentifierProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 */
	public TcpEndPointIdentifierProperty(PropertyOwner owner, String name, TcpEndPointIdentifier address, int modificationMode)
	{
		super(owner, name, address, modificationMode, TcpEndPointIdentifier.getIpAndPortEndPointIdentifierParsingConstructor());
	}
	
	/**
	 * Creates a new TcpEndPointIdentifierProperty object.
	 * 
	 * @param owner the owner of this TcpEndPointIdentifierProperty.
	 * @param name the name of this TcpEndPointIdentifierProperty.
	 * @param address the default value of this TcpEndPointIdentifierProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
	 */
	public TcpEndPointIdentifierProperty(PropertyOwner owner, String name, TcpEndPointIdentifier address, int modificationMode, boolean persistent)
	{
		super(owner, name, address, modificationMode, persistent, TcpEndPointIdentifier.getIpAndPortEndPointIdentifierParsingConstructor());
	}
	
	/**
	 * Creates a new TcpEndPointIdentifierProperty object that isn't modifiable.
	 * 
	 * @param owner the owner of this TcpEndPointIdentifierProperty.
	 * @param name the name of this TcpEndPointIdentifierProperty.
	 * @param address the default value of this TcpEndPointIdentifierProperty.
	 */
	public TcpEndPointIdentifierProperty(PropertyOwner owner, String name, TcpEndPointIdentifier address)
	{
		super(owner, name, address, TcpEndPointIdentifier.getIpAndPortEndPointIdentifierParsingConstructor());
	}
	
	/**
	 * Creates a new TcpEndPointIdentifierProperty object.
	 * 
	 * @param owner the owner of this TcpEndPointIdentifierProperty.
	 * @param name the name of this TcpEndPointIdentifierProperty.
	 * @param addressArray the default value of this TcpEndPointIdentifierProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 */
	public TcpEndPointIdentifierProperty(PropertyOwner owner, String name, TcpEndPointIdentifier[] addressArray, int modificationMode)
	{
		super(owner, name, addressArray, modificationMode, TcpEndPointIdentifier.getIpAndPortEndPointIdentifierParsingConstructor());
	}
	
	/**
	 * Creates a new TcpEndPointIdentifierProperty object.
	 * 
	 * @param owner the owner of this TcpEndPointIdentifierProperty.
	 * @param name the name of this TcpEndPointIdentifierProperty.
	 * @param addressArray the default value of this TcpEndPointIdentifierProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
	 */
	public TcpEndPointIdentifierProperty(PropertyOwner owner, String name, TcpEndPointIdentifier[] addressArray, int modificationMode, boolean persistent)
	{
		super(owner, name, addressArray, modificationMode, persistent, TcpEndPointIdentifier.getIpAndPortEndPointIdentifierParsingConstructor());
	}
	
	/**
	 * Creates a new TcpEndPointIdentifierProperty object that isn't modifiable.
	 * 
	 * @param owner the owner of this TcpEndPointIdentifierProperty.
	 * @param name the name of this TcpEndPointIdentifierProperty.
	 * @param addressArray the default value of this TcpEndPointIdentifierProperty.
	 */
	public TcpEndPointIdentifierProperty(PropertyOwner owner, String name, TcpEndPointIdentifier[] addressArray)
	{
		super(owner, name, addressArray, TcpEndPointIdentifier.getIpAndPortEndPointIdentifierParsingConstructor());
	}

	/**
	 * Gets all the addresses in this TcpEndPointIdentifierProperty.
	 * 
	 * @return an array of TcpEndPointIdentifier objects.
	 */
	public synchronized TcpEndPointIdentifier[] getAddresses()
	{
		return (TcpEndPointIdentifier[])super.values.toArray(new TcpEndPointIdentifier[0]);
	}
	
	/**
	 * Gets the address in this IpAndPortProperty at the specified index.
	 * 
	 * @return the address in this IpAndPortProperty at the specified index.
	 */
	public synchronized TcpEndPointIdentifier getAddress(int index)
	{
		return (TcpEndPointIdentifier)super.getValue(index);
	}
}
