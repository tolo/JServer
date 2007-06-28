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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.teletalk.jserver.comm.IpAndPortEndPointIdentifier;

/**
 * Property class for handling one or more ip/port values. The values are by default separated by the characters ',' or ';'.
 * 
 * @see MultiValueProperty
 * @see Property
 * @see IpAndPortEndPointIdentifier
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1 Build 545
 */
public class IpAndPortProperty extends MultiValueProperty
{
	/** The default delimiter characters (',', ';'). */
	public static final String DefaulDelimeterChars = ",;";
   
   /**  @since 2.0 */
   public static final String AssociatedAddressSeparator = "/";
   
   
   private final HashMap associatedAddresses = new HashMap();
   	
	private final IpAndPortEndPointIdentifier.IpAndPortEndPointIdentifierFactory endPointFactory;
	
	/**
	 * Creates a new IpAndPortProperty object.
	 * 
	 * @param owner the owner of this IpAndPortProperty.
	 * @param name the name of this IpAndPortProperty.
	 * @param addressString the default value of this IpAndPortProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 */
	public IpAndPortProperty(PropertyOwner owner, String name, String addressString, int modificationMode)
	{
		super(owner, name, IpAndPortProperty.DefaulDelimeterChars, (String)null, modificationMode);
		
		this.endPointFactory = IpAndPortEndPointIdentifier.getIpAndPortEndPointIdentifierFactory();
		this.setValueAsString(addressString);
		super.defaultValue = addressString;
	}
	
	/**
	 * Creates a new IpAndPortProperty object.
	 * 
	 * @param owner the owner of this IpAndPortProperty.
	 * @param name the name of this IpAndPortProperty.
	 * @param addressString the default value of this IpAndPortProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 */
	public IpAndPortProperty(PropertyOwner owner, String name, String addressString, int modificationMode, IpAndPortEndPointIdentifier.IpAndPortEndPointIdentifierFactory parsingConstructor)
	{
		super(owner, name, IpAndPortProperty.DefaulDelimeterChars, (String)null, modificationMode);
		
		this.endPointFactory = parsingConstructor;
		this.setValueAsString(addressString);
		super.defaultValue = addressString;
	}
	
	/**
	 * Creates a new IpAndPortProperty object.
	 * 
	 * @param owner the owner of this IpAndPortProperty.
	 * @param name the name of this IpAndPortProperty.
	 * @param addressString the default value of this IpAndPortProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
	 */
	public IpAndPortProperty(PropertyOwner owner, String name, String addressString, int modificationMode, boolean persistent)
	{
		super(owner, name, IpAndPortProperty.DefaulDelimeterChars, (String)null, modificationMode, persistent);
		
		this.endPointFactory = IpAndPortEndPointIdentifier.getIpAndPortEndPointIdentifierFactory();
		this.setValueAsString(addressString);
		super.defaultValue = addressString;
	}
	
	/**
	 * Creates a new IpAndPortProperty object.
	 * 
	 * @param owner the owner of this IpAndPortProperty.
	 * @param name the name of this IpAndPortProperty.
	 * @param addressString the default value of this IpAndPortProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
	 */
	public IpAndPortProperty(PropertyOwner owner, String name, String addressString, int modificationMode, boolean persistent, IpAndPortEndPointIdentifier.IpAndPortEndPointIdentifierFactory parsingConstructor)
	{
		super(owner, name, IpAndPortProperty.DefaulDelimeterChars, (String)null, modificationMode, persistent);
		
		this.endPointFactory = parsingConstructor;
		this.setValueAsString(addressString);
		super.defaultValue = addressString;
	}
	
	/**
	 * Creates a new IpAndPortProperty object that isn't modifiable.
	 * 
	 * @param owner the owner of this IpAndPortProperty.
	 * @param name the name of this IpAndPortProperty.
	 * @param addressString the default value of this IpAndPortProperty.
	 */
	public IpAndPortProperty(PropertyOwner owner, String name, String addressString)
	{
		super(owner, name, IpAndPortProperty.DefaulDelimeterChars, (String)null);
		
		this.endPointFactory = IpAndPortEndPointIdentifier.getIpAndPortEndPointIdentifierFactory();
		this.setValueAsString(addressString);
		super.defaultValue = addressString;
	}
	
	/**
	 * Creates a new IpAndPortProperty object that isn't modifiable.
	 * 
	 * @param owner the owner of this IpAndPortProperty.
	 * @param name the name of this IpAndPortProperty.
	 * @param addressString the default value of this IpAndPortProperty.
	 */
	public IpAndPortProperty(PropertyOwner owner, String name, String addressString, IpAndPortEndPointIdentifier.IpAndPortEndPointIdentifierFactory parsingConstructor)
	{
		super(owner, name, IpAndPortProperty.DefaulDelimeterChars, (String)null);
		
		this.endPointFactory = parsingConstructor;
		this.setValueAsString(addressString);
		super.defaultValue = addressString;
	}
	
	/**
	 * Creates a new IpAndPortProperty object.
	 * 
	 * @param owner the owner of this IpAndPortProperty.
	 * @param name the name of this IpAndPortProperty.
	 * @param address the default value of this IpAndPortProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 */
	public IpAndPortProperty(PropertyOwner owner, String name, IpAndPortEndPointIdentifier address, int modificationMode)
	{
		super(owner, name, IpAndPortProperty.DefaulDelimeterChars, ((address != null) ? new IpAndPortEndPointIdentifier[]{address} : null), modificationMode);
		
		this.endPointFactory = IpAndPortEndPointIdentifier.getIpAndPortEndPointIdentifierFactory();
	}
	
	/**
	 * Creates a new IpAndPortProperty object.
	 * 
	 * @param owner the owner of this IpAndPortProperty.
	 * @param name the name of this IpAndPortProperty.
	 * @param address the default value of this IpAndPortProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 */
	public IpAndPortProperty(PropertyOwner owner, String name, IpAndPortEndPointIdentifier address, int modificationMode, IpAndPortEndPointIdentifier.IpAndPortEndPointIdentifierFactory parsingConstructor)
	{
		super(owner, name, IpAndPortProperty.DefaulDelimeterChars, ((address != null) ? new IpAndPortEndPointIdentifier[]{address} : null), modificationMode);
		
		this.endPointFactory = parsingConstructor;
	}
	
	/**
	 * Creates a new IpAndPortProperty object.
	 * 
	 * @param owner the owner of this IpAndPortProperty.
	 * @param name the name of this IpAndPortProperty.
	 * @param address the default value of this IpAndPortProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
	 */
	public IpAndPortProperty(PropertyOwner owner, String name, IpAndPortEndPointIdentifier address, int modificationMode, boolean persistent)
	{
		super(owner, name, IpAndPortProperty.DefaulDelimeterChars, ((address != null) ? new IpAndPortEndPointIdentifier[]{address} : null), modificationMode, persistent);
		
		this.endPointFactory = IpAndPortEndPointIdentifier.getIpAndPortEndPointIdentifierFactory();
	}
	
	/**
	 * Creates a new IpAndPortProperty object.
	 * 
	 * @param owner the owner of this IpAndPortProperty.
	 * @param name the name of this IpAndPortProperty.
	 * @param address the default value of this IpAndPortProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
	 */
	public IpAndPortProperty(PropertyOwner owner, String name, IpAndPortEndPointIdentifier address, int modificationMode, boolean persistent, IpAndPortEndPointIdentifier.IpAndPortEndPointIdentifierFactory parsingConstructor)
	{
		super(owner, name, IpAndPortProperty.DefaulDelimeterChars, ((address != null) ? new IpAndPortEndPointIdentifier[]{address} : null), modificationMode, persistent);
		
		this.endPointFactory = parsingConstructor;
	}
	
	/**
	 * Creates a new IpAndPortProperty object that isn't modifiable.
	 * 
	 * @param owner the owner of this IpAndPortProperty.
	 * @param name the name of this IpAndPortProperty.
	 * @param address the default value of this IpAndPortProperty.
	 */
	public IpAndPortProperty(PropertyOwner owner, String name, IpAndPortEndPointIdentifier address)
	{
		super(owner, name, IpAndPortProperty.DefaulDelimeterChars, ((address != null) ? new IpAndPortEndPointIdentifier[]{address} : null));
		
		this.endPointFactory = IpAndPortEndPointIdentifier.getIpAndPortEndPointIdentifierFactory();
	}
	
	/**
	 * Creates a new IpAndPortProperty object that isn't modifiable.
	 * 
	 * @param owner the owner of this IpAndPortProperty.
	 * @param name the name of this IpAndPortProperty.
	 * @param address the default value of this IpAndPortProperty.
	 */
	public IpAndPortProperty(PropertyOwner owner, String name, IpAndPortEndPointIdentifier address, IpAndPortEndPointIdentifier.IpAndPortEndPointIdentifierFactory parsingConstructor)
	{
		super(owner, name, IpAndPortProperty.DefaulDelimeterChars, ((address != null) ? new IpAndPortEndPointIdentifier[]{address} : null));
		
		this.endPointFactory = parsingConstructor;
	}
	
	/**
	 * Creates a new IpAndPortProperty object.
	 * 
	 * @param owner the owner of this IpAndPortProperty.
	 * @param name the name of this IpAndPortProperty.
	 * @param addressArray the default value of this IpAndPortProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 */
	public IpAndPortProperty(PropertyOwner owner, String name, IpAndPortEndPointIdentifier[] addressArray, int modificationMode)
	{
		super(owner, name, IpAndPortProperty.DefaulDelimeterChars, addressArray, modificationMode);
		
		this.endPointFactory = IpAndPortEndPointIdentifier.getIpAndPortEndPointIdentifierFactory();
	}
	
	/**
	 * Creates a new IpAndPortProperty object.
	 * 
	 * @param owner the owner of this IpAndPortProperty.
	 * @param name the name of this IpAndPortProperty.
	 * @param addressArray the default value of this IpAndPortProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 */
	public IpAndPortProperty(PropertyOwner owner, String name, IpAndPortEndPointIdentifier[] addressArray, int modificationMode, IpAndPortEndPointIdentifier.IpAndPortEndPointIdentifierFactory parsingConstructor)
	{
		super(owner, name, IpAndPortProperty.DefaulDelimeterChars, addressArray, modificationMode);
		
		this.endPointFactory = parsingConstructor;
	}
	
	/**
	 * Creates a new IpAndPortProperty object.
	 * 
	 * @param owner the owner of this IpAndPortProperty.
	 * @param name the name of this IpAndPortProperty.
	 * @param addressArray the default value of this IpAndPortProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
	 */
	public IpAndPortProperty(PropertyOwner owner, String name, IpAndPortEndPointIdentifier[] addressArray, int modificationMode, boolean persistent)
	{
		super(owner, name, IpAndPortProperty.DefaulDelimeterChars, addressArray, modificationMode, persistent);
		
		this.endPointFactory = IpAndPortEndPointIdentifier.getIpAndPortEndPointIdentifierFactory();
	}
	
	/**
	 * Creates a new IpAndPortProperty object.
	 * 
	 * @param owner the owner of this IpAndPortProperty.
	 * @param name the name of this IpAndPortProperty.
	 * @param addressArray the default value of this IpAndPortProperty.
	 * @param modificationMode the modificationMode of this Property. See the class PropertyConstants for valid values. If an invalid value is specified, modificationMode will be set to NOT_MODIFIABLE.
	 * @param persistent flag indicating whether or not this Property is persistent, i.e. saved and read from a property file.
	 */
	public IpAndPortProperty(PropertyOwner owner, String name, IpAndPortEndPointIdentifier[] addressArray, int modificationMode, boolean persistent, IpAndPortEndPointIdentifier.IpAndPortEndPointIdentifierFactory parsingConstructor)
	{
		super(owner, name, IpAndPortProperty.DefaulDelimeterChars, addressArray, modificationMode, persistent);
		
		this.endPointFactory = parsingConstructor;
	}
	
	/**
	 * Creates a new IpAndPortProperty object that isn't modifiable.
	 * 
	 * @param owner the owner of this IpAndPortProperty.
	 * @param name the name of this IpAndPortProperty.
	 * @param addressArray the default value of this IpAndPortProperty.
	 */
	public IpAndPortProperty(PropertyOwner owner, String name, IpAndPortEndPointIdentifier[] addressArray)
	{
		super(owner, name, IpAndPortProperty.DefaulDelimeterChars, addressArray);
		
		this.endPointFactory = IpAndPortEndPointIdentifier.getIpAndPortEndPointIdentifierFactory();
	}
	
	/**
	 * Creates a new IpAndPortProperty object that isn't modifiable.
	 * 
	 * @param owner the owner of this IpAndPortProperty.
	 * @param name the name of this IpAndPortProperty.
	 * @param addressArray the default value of this IpAndPortProperty.
	 */
	public IpAndPortProperty(PropertyOwner owner, String name, IpAndPortEndPointIdentifier[] addressArray, IpAndPortEndPointIdentifier.IpAndPortEndPointIdentifierFactory parsingConstructor)
	{
		super(owner, name, IpAndPortProperty.DefaulDelimeterChars, addressArray);
		
		this.endPointFactory = parsingConstructor;
	}

	/**
	 * Gets the number of addresses in this IpAndPortProperty.
	 * 
	 * @return the number of addresses in this IpAndPortProperty.
	 */
	public int getNumberOfAddresses()
	{
		return super.size();
	}
	
	/**
	 * Adds an address to this IpAndPortProperty.
	 * 
	 * @param address an address to add.
	 * 
	 * @return <code>true</code> if the address was successfully added, otherwise <code>false</code>.
	 */
	public boolean addAddress(IpAndPortEndPointIdentifier address)
	{
		return super.addValue(address);
	}
	
	/**
	 * Removes an address from this IpAndPortProperty.
	 * 
	 * @param address an address to remove.
	 * 
	 * @return <code>true</code> if the address was successfully removed, otherwise <code>false</code>.
	 */
	public boolean removeAddress(IpAndPortEndPointIdentifier address)
	{
		return super.removeValue(address);
	}
	
	/**
	 * Gets all the addresses in this IpAndPortProperty.
	 * 
	 * @return an array of IpAndPortEndPointIdentifier objects.
	 */
	public synchronized IpAndPortEndPointIdentifier[] getAllAddresses()
	{
		return (IpAndPortEndPointIdentifier[])super.values.toArray(new IpAndPortEndPointIdentifier[0]);
	}
	
	/**
	 * Gets all the addresses in this IpAndPortProperty.
	 * 
	 * @return a list of IpAndPortEndPointIdentifier objects.
	 */
	public synchronized List getAddressList()
	{
		return new ArrayList(super.values);
	}
	
	/**
	 * Gets the address in this IpAndPortProperty at the specified index.
	 * 
	 * @return .
	 */
	public IpAndPortEndPointIdentifier get(int index)
	{
		return (IpAndPortEndPointIdentifier)super.getValue(index);
	}
   
   /**
    * Gets the associated address for the specified configured address. The associated address is the address specified after the separating "/" 
    * character in the configuration file, for example 10.1.1.1:1111/10.1.1.1:2222. The associated address may also be set using the method 
    * {@link #setAssociatedAddress(IpAndPortEndPointIdentifier, IpAndPortEndPointIdentifier)}.
    * 
    * @param address the address (one of the addresses contained in this property object) to get the associated address for.
    * 
    * @return the associated address. 
    * 
    * @since 2.0
    */
   public IpAndPortEndPointIdentifier getAssociatedAddress(final IpAndPortEndPointIdentifier address)
   {
      synchronized(this.associatedAddresses)
      {
         return (IpAndPortEndPointIdentifier)this.associatedAddresses.get(address);
      }
   }
   
   /**
    * Sets the associated address for the specified configured address. The associated address is the address specified after the separating "/" 
    * character in the configuration file, for example 10.1.1.1:1111/10.1.1.1:2222. The associated address may also be set using the method 
    * {@link #setAssociatedAddress(IpAndPortEndPointIdentifier, IpAndPortEndPointIdentifier)}.
    * 
    * @param address the address (one of the addresses contained in this property object) to get the associated address for.
    * @param associatedAddress the associated address.
    *
    * @return the previous associated address, or <code>null</code> if there wasn't any.
    *  
    * @since 2.0
    */
   public IpAndPortEndPointIdentifier setAssociatedAddress(final IpAndPortEndPointIdentifier address, final IpAndPortEndPointIdentifier associatedAddress)
   {
      synchronized(this.associatedAddresses)
      {
         return (IpAndPortEndPointIdentifier)this.associatedAddresses.put(address, associatedAddress);
      }
   }
	
	/**
	 * Parses a string value into an IpAndPortEndPointIdentifier object that is to be inserted in this MultiValueProperty.
	 * 
	 * @param valueAsString the value to be parsed into an IpAndPortEndPointIdentifier object.
	 * 
	 * @return the parsed IpAndPortEndPointIdentifier object or <code>null</code> if an object couldn't be parsed from the specified string.
	 */
	protected Object parseValue(String valueAsString)
	{
	   int separatorIndex = valueAsString.indexOf(AssociatedAddressSeparator);
      String associatedAddressString = null;
      
      if( separatorIndex > 0 )
      {
         associatedAddressString = valueAsString.substring(separatorIndex + 1);
         valueAsString = valueAsString.substring(0, separatorIndex);
      }
      
      IpAndPortEndPointIdentifier ipAndPortEndPointIdentifier = IpAndPortEndPointIdentifier.parseIpAndPortEndPointIdentifier(valueAsString, this.endPointFactory);
      
      if( associatedAddressString != null )
      {
         synchronized(this.associatedAddresses)
         {
            this.associatedAddresses.put(ipAndPortEndPointIdentifier, IpAndPortEndPointIdentifier.parseIpAndPortEndPointIdentifier(associatedAddressString, this.endPointFactory));
         }
      }
      
      return ipAndPortEndPointIdentifier;
	}
	
	/**
	 * Formats an IpAndPortEndPointIdentifier object value into a string that is to be used when generating a 
	 * string representation of the value of this MultiValueProperty.
	 * 
	 * @param value the value to be formatted.
	 * 
	 * @return the IpAndPortEndPointIdentifier object value as a string.
	 */
	protected String formatValue(Object value)
	{
      if( value == null ) return "";
      
      IpAndPortEndPointIdentifier associatedAddress = null;
      synchronized(this.associatedAddresses)
      {
         associatedAddress = (IpAndPortEndPointIdentifier)this.associatedAddresses.get(value);
      }
      
      if( associatedAddress != null )
      {
         return ((IpAndPortEndPointIdentifier)value).getAddressAsString() + AssociatedAddressSeparator + associatedAddress.getAddressAsString();
      }
      else
      {
   		return ((IpAndPortEndPointIdentifier)value).getAddressAsString();
      }
	}
}
