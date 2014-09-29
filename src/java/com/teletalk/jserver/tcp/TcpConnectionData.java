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

import java.net.Socket;

/**
 * Data transfer buffer for initialization of TcpConnection objects.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public class TcpConnectionData
{
	/** Constant intentifying a server side TcpConnection. */
	public static final boolean SERVER_SIDE_TYPE = true;
	
	/** Constant intentifying a client side TcpConnection. */
	public static final boolean CLIENT_SIDE_TYPE = false;
		
	/** A Socket object (for server side TcpConnectionData objects). */
	private final Socket socket;
	
	/** A TcpEndPointIdentifier object denoting a remote address (for client side TcpConnectionData objects). */
	private TcpEndPointIdentifier remoteAddress;
   
   private TcpEndPointIdentifier acceptAddess;   
	
	private final boolean type;
	
	private final Object customData; 

	/**
	 * Constructs a server side TcpConnectionData object.
	 * 
	 * @param socket a Socket object.
	 */
	public TcpConnectionData(Socket socket)
	{
		this.socket = socket;
		this.remoteAddress = null;
		this.type = SERVER_SIDE_TYPE;
		this.customData = null;
	}
	
	/**
	 * Constructs a server side TcpConnectionData object.
	 * 
	 * @param socket a Socket object.
	 * @param customData a custom data object to be transferred.
	 */
	public TcpConnectionData(Socket socket, Object customData)
	{
		this.socket = socket;
		this.remoteAddress = null;
		this.type = SERVER_SIDE_TYPE;
		this.customData = customData;
	}
   
   /**
    * Constructs a server side TcpConnectionData object.
    * 
    * @param socket a Socket object.
    * @param customData a custom data object to be transferred.
    * @param acceptAddress the address of the server socket on which the socket was accepted.
    * 
    * @since 2.0.1 (20041111).
    */
   public TcpConnectionData(Socket socket, Object customData, TcpEndPointIdentifier acceptAddress)
   {
      this.socket = socket;
      this.remoteAddress = null;
      this.type = SERVER_SIDE_TYPE;
      this.customData = customData;
      this.acceptAddess = acceptAddress;
   }
	
	/**
	 * Constructs a TcpConnectionData object with client side or server side mode.
	 * 
	 * @param socket a Socket object.
	 * @param customData a custom data object to be transferred.
	 * @param type the type (client side or server side) of this TcpConnectionData.
	 */
	public TcpConnectionData(Socket socket, Object customData, boolean type)
	{
		this.socket = socket;
		this.remoteAddress = null;
		this.type = SERVER_SIDE_TYPE;
		this.customData = customData;
	}
	
	/**
	 * Constructs a client side TcpConnectionData object.
	 * 
	 * @param remoteAddress a TcpEndPointIdentifier denoting a remote address to connect to.
	 */
	public TcpConnectionData(TcpEndPointIdentifier remoteAddress)
	{
		this.socket = null;
		this.remoteAddress = remoteAddress;
		this.type = CLIENT_SIDE_TYPE;
		this.customData = null;
	}
	
	/**
	 * Constructs a client side TcpConnectionData object.
	 * 
	 * @param remoteAddress a TcpEndPointIdentifier denoting a remote address to connect to.
	 * @param customData a custom data object to be transferred.
	 */
	public TcpConnectionData(TcpEndPointIdentifier remoteAddress, Object customData)
	{
		this.socket = null;
		this.remoteAddress = remoteAddress;
		this.type = CLIENT_SIDE_TYPE;
		this.customData = customData;
	}
	
	/**
	 * Gets the socket object contained in this TcpConnectionData object.
	 * 
	 * @return the socket object, null if this is a client side TcpConnectionData.
	 */
	public final Socket getSocket()
	{
		return socket;
	}
	
	/**
	 * Gets the TcpEndPointIdentifier contained in this TcpConnectionData, which denotes a remote address to connect to (clientside) or the address the socket is conneted to (server side).
	 * 
	 * @return the TcpEndPointIdentifier object, null if this is a server side TcpConnectionData;
	 */
	public final TcpEndPointIdentifier getRemoteAddress()
	{
		if( (this.remoteAddress == null) && (this.socket != null) )
		{
			this.remoteAddress = new TcpEndPointIdentifier(socket.getInetAddress(), socket.getPort());
		}
		return this.remoteAddress;
	}
   
   /**
    * Gets the address of the server socket on which the socket was accepted.
    * 
    * @since 2.0.1 (20041111).
    */
   public TcpEndPointIdentifier getAcceptAddess()
   {
      return acceptAddess;
   }
   
   /**
    * Sets the address of the server socket on which the socket was accepted.
    * 
    * @since 2.0.1 (20041111).
    */
   protected void setAcceptAddess(TcpEndPointIdentifier acceptAddess)
   {
      this.acceptAddess = acceptAddess;
   }
   
	/**
	 * Gets the type of this TcpConnectionData.
	 * 
	 * @return the type of this TcpConnectionData.
	 */
	public final boolean getType()
	{
		return this.type;
	}
	
	/**
	 * Convenience method to check if this is a server side TcpConnectionData. 
	 * 
	 * @return true if this is a server side TcpConnectionData, otherwise false.
	 */
	public final boolean isServerSide()
	{
		return (type == SERVER_SIDE_TYPE);
	}
	
	/**
	 * Convenience method to check if this is a client side TcpConnectionData. 
	 * 
	 * @return true if this is a client side TcpConnectionData, otherwise false.
	 */	
	public final boolean isClientSide()
	{
		return (type == CLIENT_SIDE_TYPE);
	}
	
	/**
	 * Gets the custom data object contained in this TcpConnectionData object.
	 * 
	 * @return the custom data object.
	 */
	public final Object getCustomData()
	{
		return this.customData;
	}
}
