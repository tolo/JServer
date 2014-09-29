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

import java.net.InetAddress;

import com.teletalk.jserver.comm.IpAndPortEndPointIdentifier;

/**
 * This class is used to identify a TCP/IP address, consisting of an ip address (InetAddress) and a port number. An ip-address value 
 * of null indicates a local host address.
 *  
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public class TcpEndPointIdentifier extends IpAndPortEndPointIdentifier
{
	static final long serialVersionUID = 6425695716214512935L;
	
	/**
	 * The IpAndPortEndPointIdentifierParsingConstructor object used to create new instances when parsing from strings or numbers.
	 */
	private static final IpAndPortEndPointIdentifierFactory parsingConstructor = new IpAndPortEndPointIdentifierFactory()
		{
			public IpAndPortEndPointIdentifier create(String address, int port)
			{
				return new TcpEndPointIdentifier(address, port);
			}
		};
	
	/**
	 * Gets the IpAndPortEndPointIdentifierParsingConstructor object used to create new instances when parsing from strings or numbers.
	 * 
	 * @return the IpAndPortEndPointIdentifierParsingConstructor object used to create new instances when parsing from strings or numbers.
	 */
	public static IpAndPortEndPointIdentifierFactory getIpAndPortEndPointIdentifierParsingConstructor()
	{
		return parsingConstructor;
	}
			
	/**
	 * Creates a new TcpEndPointIdentifier representing a local host address.
	 * 
	 * @param port the port component of this TcpEndPointIdentifier.
	 */
	public TcpEndPointIdentifier(final int port)
	{
		super(port);
	}
	
	/**
	 * Creates a new TcpEndPointIdentifier.
	 * 
	 * @param inetAddress the ip address component of this TcpEndPointIdentifier.
	 * @param port the port component of this TcpEndPointIdentifier.
	 */
	public TcpEndPointIdentifier(final InetAddress inetAddress, final int port)
	{
		super(inetAddress, port);
	}
	
	/**
	 * Creates a new TcpEndPointIdentifier.
	 * 
	 * @param address the ip address component of this TcpEndPointIdentifier.
	 * @param port the port component of this TcpEndPointIdentifier.
	 */
	public TcpEndPointIdentifier(final String address, final int port)
	{
		super(address, port);
	}
			
	/**
	 * Creates a new TcpEndPointIdentifier object from information parsed from the string specified by parameter <code>stringAddress</code>. The format of 
	 * the string should be that of strings returned from calls to the method {@link #getAddressAsString()}.
	 * <br><br>
	 * This method is provided only for convenience as it simply uses the super class implementation and casts the 
	 * result into the appropriate type.
	 * 
	 * @param stringAddress a String representation of a TcpEndPointIdentifier object.
	 * 
	 * @return a TcpEndPointIdentifier object.
	 */
	public static final TcpEndPointIdentifier parseTcpEndPointIdentifier(final String stringAddress)
	{
		return (TcpEndPointIdentifier)IpAndPortEndPointIdentifier.parseIpAndPortEndPointIdentifier(stringAddress, parsingConstructor);
	}
	
	/**
	 * Creates a new TcpEndPointIdentifier object from information parsed from the <code>long</code> specified by parameter <code>longAddress</code>. The format of 
	 * the long should be that of longs returned from calls to the method {@link #getAddressAsLong()}.
	 * <br><br>
	 * This method is provided only for convenience as it simply uses the super class implementation and casts the 
	 * result into the appropriate type.
	 * 
	 * @param longAddress a <code>long</code> representation of a TcpEndPointIdentifier object.
	 * 
	 * @return a TcpEndPointIdentifier object.
	 */
	public static final TcpEndPointIdentifier parseTcpEndPointIdentifier(final long longAddress)
	{
		return (TcpEndPointIdentifier)IpAndPortEndPointIdentifier.parseIpAndPortEndPointIdentifier(longAddress, parsingConstructor);
	}
	
	/**
	 * Compares this object with another.
	 * 
	 * @param obj an object to be compared with this.
	 * 
	 * @return true if the object specified by parameter o is an instance of TcpEndPointIdentifier and has the same ip address and port as this object, otherwise false.
	 */
	public boolean equals(Object obj)
	{
		if(obj instanceof TcpEndPointIdentifier)
		{
			return super.equals(obj);
		}
		else return false;
	}
}
