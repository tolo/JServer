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

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.StringTokenizer;

/**
 * This class is used to identify an address of a communication endpoint, consisting of an ip address and a port number. 
 * An ip-address value of null indicates a local host address.
 * <br><br>
 * <i>Note: This implementation is unsynchronized.</i>
 *  
 * @author Tobias Löfstrand
 * 
 * @since 1.1 Build 545
 */
public class IpAndPortEndPointIdentifier implements EndPointIdentifier
{
	static final long serialVersionUID = 4094144523907408034L;
	
	/**
	 * Interface used to provide subclasses with a means to specify how to create new 
	 * instances when parsing from strings or numbers using the methods {@link #parseIpAndPortEndPointIdentifier(String)} or 
	 * {@link #parseIpAndPortEndPointIdentifier(long)}.
	 * 
	 * @author Tobias Löfstrand
	 * 
	 * @since 1.1 Build 545
	 */
	public interface IpAndPortEndPointIdentifierFactory
	{
		public IpAndPortEndPointIdentifier create(String address, int port);
	}

	/** String constant for any/all local addresses (0.0.0.0). @since 2.0.1 (20041215) */
   public static final String anyLocalHostString = "0.0.0.0";
   /** String constant for localhost (loopback). */
	public static final String localHostString = "localhost";
   /** String constant for localhost (loopback). */
	public static final String localHostAddressString = "127.0.0.1";
	
	private static final HashMap internMap = new HashMap();
	
	private transient InetAddress inetAddress;
	private final String address;
	private final int port;
	private transient String addressString;
	
	/**
	 * The IpAndPortEndPointIdentifierParsingConstructor object used to create new instances when parsing from strings or numbers.
	 */
	private static final IpAndPortEndPointIdentifierFactory DefaultFactory = new IpAndPortEndPointIdentifierFactory()
		 {
			public IpAndPortEndPointIdentifier create(String address, int port)
			{
				return new IpAndPortEndPointIdentifier(address, port);
			}
		 };
	
	/**
	 * Gets the IpAndPortEndPointIdentifierFactory object used to create new instances when parsing from strings or numbers.
	 * 
	 * @return the IpAndPortEndPointIdentifierFactory object used to create new instances when parsing from strings or numbers.
	 */
	public static IpAndPortEndPointIdentifierFactory getIpAndPortEndPointIdentifierFactory()
	{
		return DefaultFactory;
	}
   
   /**
    * Gets the IpAndPortEndPointIdentifierFactory object used to create new instances when parsing from strings or numbers.
    * 
    * @return the IpAndPortEndPointIdentifierFactory object used to create new instances when parsing from strings or numbers.
    */
   /*public static IpAndPortEndPointIdentifierFactory getIpAndPortEndPointIdentifierParsingConstructor()
   {
      return factory;
   }*/
	
	/**
	 * Creates a new IpAndPortEndPointIdentifier representing a local host address.
	 * 
	 * @param port the port component of this IpAndPortEndPointIdentifier.
	 */
	public IpAndPortEndPointIdentifier(final int port)
	{
		this.inetAddress = null;
		this.address = null;
		this.port = port;
	}
	
	/**
	 * Creates a new IpAndPortEndPointIdentifier.
	 * 
	 * @param inetAddress the ip address component of this IpAndPortEndPointIdentifier.
	 * @param port the port component of this IpAndPortEndPointIdentifier.
	 */
	public IpAndPortEndPointIdentifier(final InetAddress inetAddress, final int port)
	{
		this((inetAddress != null) ? inetAddress.getHostAddress(): null, port);
	}
	
	/**
	 * Creates a new IpAndPortEndPointIdentifier.
	 * 
	 * @param address the ip address component of this IpAndPortEndPointIdentifier.
	 * @param port the port component of this IpAndPortEndPointIdentifier.
	 */
	public IpAndPortEndPointIdentifier(final String address, final int port)
	{
		this.inetAddress = null;
		
      if( (address != null) && address.trim().equals(anyLocalHostString) ) this.address = null;
		else this.address = address;
		
		this.port = port;
	}
	
	/**
	 * Creates a new IpAndPortEndPointIdentifier based on the data of another one.
	 * 
	 * @param template Another IpAndPortEndPointIdentifier to extract data from.
	 */
	public IpAndPortEndPointIdentifier(final IpAndPortEndPointIdentifier template)
	{
		this(template.getAddress(), template.getPort());
	}
	
	/**
	 * Gets (the address of) this IpAndPortEndPointIdentifier as a string. The returned string will have the following format:
	 * <code>host:port</code>, e.g. <code>195.84.29.17:80</code>.
	 * 
	 * @return this IpAndPortEndPointIdentifier as a string.
	 */
	public final String getAddressAsString()
	{
		if(addressString == null)
		{
			if(address != null) addressString = address + ":" + port;
			else addressString = anyLocalHostString + ":" + port;
		}
		
		return addressString;
	}
		
	/**
	 * Creates a new IpAndPortEndPointIdentifier object from information parsed from the string specified by parameter <code>stringAddress</code>. The format of 
	 * the string should be that of strings returned from calls to the method {@link #getAddressAsString()}.
	 * 
	 * @param stringAddress a String representation of a IpAndPortEndPointIdentifier object.
    * @param factory the factory to be used for creation of an IpAndPortEndPointIdentifier object.
	 * 
	 * @return a IpAndPortEndPointIdentifier object, or <code>null</code> if the parsing was unsuccessful.
	 */
	public static final IpAndPortEndPointIdentifier parseIpAndPortEndPointIdentifier(final String stringAddress, final IpAndPortEndPointIdentifierFactory factory)
	{
		String parsedAddress = null;
		int portNumber = -1;
		
		try
		{
			if(stringAddress != null)
			{
				StringTokenizer tokenizer = new StringTokenizer(stringAddress.trim(), ":", true);
				String token;
				
				if(tokenizer.hasMoreTokens())
				{
					token = tokenizer.nextToken().trim();
					
					if(token.equals(":") && tokenizer.hasMoreTokens())
					{
						token = tokenizer.nextToken().trim();
						portNumber = Integer.parseInt(token);
					}
					else if(tokenizer.hasMoreTokens())
					{
						parsedAddress = token;
						if(tokenizer.hasMoreTokens())
						{
							token = tokenizer.nextToken().trim();
							if(token.equals(":") && tokenizer.hasMoreTokens())
							{
								token = tokenizer.nextToken().trim();
								portNumber = Integer.parseInt(token);
							}
						}
					}
				}
			}
		}
		catch(Exception e){}
		
		if(portNumber >= 0) return factory.create(parsedAddress, portNumber);
		else return null;
	}
	
	/**
	 * Creates a new IpAndPortEndPointIdentifier object from information parsed from the string specified by parameter <code>stringAddress</code>. The format of 
	 * the string should be that of strings returned from calls to the method {@link #getAddressAsString()}.
	 * 
	 * @param stringAddress a String representation of a IpAndPortEndPointIdentifier object.
	 * 
	 * @return a IpAndPortEndPointIdentifier object.
	 */
	public static final IpAndPortEndPointIdentifier parseIpAndPortEndPointIdentifier(final String stringAddress)
	{
		return parseIpAndPortEndPointIdentifier(stringAddress, DefaultFactory);
	}
   
   /**
    * Generates a <code>long</code> representation of the specified InetAddress and port. The returned long is created by
    * setting the high 32 bits of the long to the long representation of the ip address and the high 16 bits of the low 32 bits to 
    * the port number.
    */
   public static final long getAddressAsLong(final InetAddress inetAddress, final int port)
   {
      byte[] address;
      long tmp;
      long result = 0;
      
      if(inetAddress != null)
      {
         address = inetAddress.getAddress();

         for(int i=0; i<4; i++)
         {
            tmp = (address[i] + 256) & 0xFF; // Convert signed byte value to unsigned byte value (contained in long)
            result += tmp << 8*(7-i);
         }
      }
      result += (((long)port) << 16);
      
      return result;
   }
	
	/**
	 * Gets (the address of) this IpAndPortEndPointIdentifier as a <code>long</code> (64-bit integer). The returned long is created by
	 * setting the high 32 bits of the long to the long representation of the ip address and the high 16 bits of the low 32 bits to 
	 * the port number.
	 * 
	 * @return this IpAndPortEndPointIdentifier as a long.
	 */
	public final long getAddressAsLong()
	{
      return getAddressAsLong(this.getInetAddress(), this.getPort());
	}
	
	/**
	 * Creates a new IpAndPortEndPointIdentifier object from information parsed from the <code>long</code> specified by parameter <code>longAddress</code>. The format of 
	 * the long should be that of longs returned from calls to the method {@link #getAddressAsLong()}.
	 * 
	 * @param longAddress a <code>long</code> representation of a IpAndPortEndPointIdentifier object.
    * @param factory the factory to be used for creation of an IpAndPortEndPointIdentifier object. 
	 * 
	 * @return a IpAndPortEndPointIdentifier object.
	 */
	public static IpAndPortEndPointIdentifier parseIpAndPortEndPointIdentifier(final long longAddress, final IpAndPortEndPointIdentifierFactory factory)
	{
		String parsedAddress = null;
		int portNumber = -1;
		int tmp;
      
		if( ((int)(longAddress >>> 32)) != 0 )
		{
         StringBuffer addressBuffer = new StringBuffer();
		
			for(int i=0; i<4; i++)
			{
				tmp = (int)(longAddress >>> 8*(7-i));
				tmp = tmp & 0x000000FF;
				addressBuffer.append(tmp);
				if(i<3) addressBuffer.append(".");
			}
			
			parsedAddress = addressBuffer.toString();
		}
		
		portNumber = (int)((longAddress & 0x00000000FFFF0000L) >>> 16);
			
		if(portNumber >= 0) return factory.create(parsedAddress, portNumber);
		else return null;
	}
	
	/**
	 * Creates a new IpAndPortEndPointIdentifier object from information parsed from the <code>long</code> specified by parameter <code>longAddress</code>. The format of 
	 * the long should be that of longs returned from calls to the method {@link #getAddressAsLong()}.
	 * 
	 * @param longAddress a <code>long</code> representation of a IpAndPortEndPointIdentifier object.
	 * 
	 * @return a IpAndPortEndPointIdentifier object.
	 */
	public static final IpAndPortEndPointIdentifier parseIpAndPortEndPointIdentifier(final long longAddress)
	{
		return parseIpAndPortEndPointIdentifier(longAddress, DefaultFactory);
	}
	
	/**
	 * Gets the port component of this IpAndPortEndPointIdentifier.
	 * 
	 * @return the port number as an <strong><code>int</code></strong>.
	 */
	public final int getPort()
	{
		return this.port;
	}
	
	/**
	 * Gets the ip address component of this IpAndPortEndPointIdentifier an <code>InetAddress</code> object.
	 * 
	 * @return the ip address as an InetAddress object.
	 */
	public final InetAddress getInetAddress()
	{
		if(inetAddress == null)
		{
			try
			{
				if(address == null)
            {
					inetAddress = new InetSocketAddress(0).getAddress();
            }
            else
            {
					inetAddress = InetAddress.getByName(address);
            }
			}catch(Exception e){}
		}
		return this.inetAddress;
	}
	
	/**
	 * Gets the ip address component of this IpAndPortEndPointIdentifier as a <code>String</code>. A return value of null should be interpreted as "localhost".
	 * 
	 * @return the ip address component of this IpAndPortEndPointIdentifier as a <code>String</code>.
	 */
	public final String getAddress()
	{
		return this.address;
	}
	
	/**
	 * Gets a string representaiton of this IpAndPortEndPointIdentifier. This method is identical to <code>getAddressAsString()</code>.
	 * 
	 * @return this IpAndPortEndPointIdentifier as a string.
	 * 
	 * @see #getAddressAsString()
	 */
	public final String toString()
	{
		return getAddressAsString();
	}
	
	/**
	 * Compares this object with another.
	 * 
	 * @param obj an object to be compared with this.
	 * 
	 * @return true if the object specified by parameter o is an instance of IpAndPortEndPointIdentifier and has the same ip address and port as this object, otherwise false.
	 */
	public boolean equals(Object obj)
	{
		if(obj instanceof IpAndPortEndPointIdentifier)
		{
			final IpAndPortEndPointIdentifier epi = (IpAndPortEndPointIdentifier)obj;
			
         if( (epi.address != null) && !epi.address.equalsIgnoreCase(IpAndPortEndPointIdentifier.anyLocalHostString) )
			{
				return epi.address.equals(this.address) && (epi.port == this.port);
			}
			else
			{
				return ( (this.address == null)
									|| this.address.equalsIgnoreCase(IpAndPortEndPointIdentifier.anyLocalHostString) )
									&& (epi.port == this.port);
			}
		}
		else return false;
	}
	
	/**
	 * Returns a hash code value for the object. 
	 * This method is supported for the benefit of hashtables such as those provided by java.util.Hashtable. 
	 * 
	 * @return a hash code value for this object.
	 */
	public int hashCode()
	{
		return this.getAddressAsString().hashCode();
	}
	
	/**
	 * Attempts to get an already existing IpAndPortEndPointIdentifier instance that contains the same information as 
	 * <code>this</code> object.<br>
	 * <br>
	 * When this method is called, a 
	 * search will be performed in a static table for another IpAndPortEndPointIdentifier that is equal to <code>this</code> object. 
	 * If such an object is found, it will be returned by this method, otherwise <code>this </code> object will be added 
	 * to the table and returned.
	 * 
	 * @return the shared IpAndPortEndPointIdentifier instance.
	 */
	public EndPointIdentifier getSharedInstance()
	{
		synchronized(IpAndPortEndPointIdentifier.internMap)
		{
			IpAndPortEndPointIdentifier singleInstance = (IpAndPortEndPointIdentifier)IpAndPortEndPointIdentifier.internMap.get(this);
			
			if(singleInstance == null)
			{
				singleInstance = this;
				IpAndPortEndPointIdentifier.internMap.put(this, this);
			}
			
			return singleInstance;
		}
	}
}
