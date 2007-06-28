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

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.Socket;

import com.teletalk.jserver.JServerUtilities;
import com.teletalk.jserver.SubComponent;

/**
 * Utility class used by classes who wish to impose a timeout when connecting TCP Sockets.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class TcpUtilities
{
	private SubComponent parent;
   
   private final String name;
	
	private SocketFactory socketFactory;
   
   private String clientSocketBindAddress = null;
   
   private int clientSocketBindPortBegin = -1;
   
   private int clientSocketBindPortEnd = -1;
   
   
   /**
    * Creates a new TcpUtilities object.
    */
   public TcpUtilities(SubComponent parent)
   {
      this(parent, null, null);
   }
   
   /**
    * Creates a new TcpUtilities object.
    */
   public TcpUtilities(SubComponent parent, SocketFactory socketFactory)
   {
      this(parent, null, socketFactory);
   }
	
	/**
	 * Creates a new TcpUtilities object, named "TcpUtilities".
	 */
	public TcpUtilities()
	{
		this(null, "TcpUtilities", null);
	}
	
	/**
	 * Creates a new TcpUtilities object, named "TcpUtilities".
	 */
	public TcpUtilities(SocketFactory socketFactory)
	{
		this(null, "TcpUtilities", socketFactory);
	}
	
	/**
	 * Creates a new TcpUtilities object.
	 * 
	 * @param name the name of this TcpUtilities object.
	 */
	public TcpUtilities(String name)
	{
		this(null, name, null);
	}
	
	/**
	 * Creates a new TcpUtilities object.
	 * 
	 * @param name the name of this TcpUtilities object.
	 */
	public TcpUtilities(String name, SocketFactory socketFactory)
	{
		this(null, name, socketFactory);
	}
   
   /**
    * Creates a new TcpUtilities object.
    * 
    * @param name the name of this TcpUtilities object.
    */
   protected TcpUtilities(SubComponent parent, String name, SocketFactory socketFactory)
   {
      this.parent = parent;
      this.name = name;
      this.setSocketFactory(socketFactory);
   }
	
	/**
	 * Gets the socket factory responsible for creating the actual client socket objects.
	 * 
	 * @return the current client socket factory.
	 * 
	 * @see SocketFactory
	 * 
	 * @since 1.2
	 */
	public SocketFactory getSocketFactory()
	{
		return this.socketFactory;
	}
	
	/**
	 * Sets the socket factory responsible for creating the actual client socket objects.
	 * 
	 * @param socketFactory the new client socket factory.
	 * 
	 * @see SocketFactory
	 * 
	 * @since 1.2
	 */
	public void setSocketFactory(final SocketFactory socketFactory)
	{
		if( socketFactory == null )
		{
			// Create a default socket factory
			this.socketFactory = new DefaultSocketFactory();
		}
		else this.socketFactory = socketFactory;
	}
   
   /**
    * @return Returns the clientSocketBindAddress.
    * 
    * @since 2.0
    */
   public String getClientSocketBindAddress()
   {
      return this.clientSocketBindAddress;
   }
   
   /**
    * @param clientSocketBindAddress The clientSocketBindAddress to set.
    * 
    * @since 2.0
    */
   public void setClientSocketBindAddress(String clientSocketBindAddress)
   {
      this.clientSocketBindAddress = clientSocketBindAddress;
      if( (this.clientSocketBindAddress != null) && (this.clientSocketBindAddress.trim().length() == 0) ) this.clientSocketBindAddress = null;
   }
   
   /**
    * @return Returns the clientSocketBindPortBegin.
    * 
    * @since 2.0
    */
   public int getClientSocketBindPortBegin()
   {
      return this.clientSocketBindPortBegin;
   }
   
   /**
    * @param clientSocketBindPortBegin The clientSocketBindPortBegin to set.
    * 
    * @since 2.0
    */
   public void setClientSocketBindPortBegin(int clientSocketBindPortBegin)
   {
      this.clientSocketBindPortBegin = clientSocketBindPortBegin;
   }
   
   /**
    * @return Returns the clientSocketBindPortEnd.
    * 
    * @since 2.0
    */
   public int getClientSocketBindPortEnd()
   {
      return this.clientSocketBindPortEnd;
   }
   
   /**
    * @param clientSocketBindPortEnd The clientSocketBindPortEnd to set.
    * 
    * @since 2.0
    */
   public void setClientSocketBindPortEnd(int clientSocketBindPortEnd)
   {
      this.clientSocketBindPortEnd = clientSocketBindPortEnd;
   }
		
	/**
	 * Method for connecting a Socket with the given connect timeout.
	 * 
	 * @param address the address the Socket is to be connected to.
	 * @param port the port on the remote host the Socket is to be connected to.
	 * @param connectTimeOut the max time this method is allowed to block the calling thread in an attemt to 
	 * connect the Socket.
	 * 
	 * @exception IOException if there was an error creating a socket.
	 */
	public Socket createSocket(InetAddress address, int port, long connectTimeOut) throws IOException
	{
		return this.socketFactory.createSocket(address, port, (int)connectTimeOut);
	}
   
   /**
    * Method for connecting a Socket with the given connect timeout.
    * 
    * @param address the address the Socket is to be connected to.
    * @param port the port on the remote host the Socket is to be connected to.
    * @param connectTimeOut the max time this method is allowed to block the calling thread in an attemt to 
    * connect the Socket.
    * 
    * @exception IOException if there was an error creating a socket.
    */
   public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort, long connectTimeOut) throws IOException
   {
      return this.socketFactory.createSocket(address, port, localAddress, localPort, (int)connectTimeOut);
   }
	
	/**
	 * Method for connecting a Socket with the given connect timeout.
	 * 
	 * @param remoteAddress the address the Socket is to be connected to.
	 * @param connectTimeOut the max time this method is allowed to block the calling thread in an attemt to 
	 * connect the Socket.
	 * 
	 * @exception IOException if there was an error creating a socket.
	 */
	public Socket createSocket(TcpEndPointIdentifier remoteAddress, long connectTimeOut) throws IOException
	{
		return this.createSocket(remoteAddress.getInetAddress(), remoteAddress.getPort(), connectTimeOut);
	}
   
   /**
    * Method for connecting a Socket with the given connect timeout.
    * 
    * @param remoteAddress the address the Socket is to be connected to.
    * @param localAddress
    * @param connectTimeOut the max time this method is allowed to block the calling thread in an attemt to 
    * connect the Socket.
    * 
    * @exception IOException if there was an error creating a socket.
    */
   public Socket createSocket(TcpEndPointIdentifier remoteAddress, TcpEndPointIdentifier localAddress, long connectTimeOut) throws IOException
   {
      return this.createSocket(remoteAddress.getInetAddress(), remoteAddress.getPort(), localAddress.getInetAddress(), localAddress.getPort(), connectTimeOut);
   }
	
	/**
	 * Method that makes it possible to make a certain number of attempts at a certail interval to create a socket for 
	 * the specified address.
	 *  
	 * @param address the address the Socket is to be connected to.
	 * @param port the port on the remote host the Socket is to be connected to.
	 * @param connectTimeOut the the interval between the connect attempts (the maximum time a connect attempt is allowed to take).
	 * @param maxAttempts the maximum number of attempts that will be made to create a socket.
	 * @param silent boolean flag indicating if errors/warning should be logged while trying to connect.
	 * 
	 * @return a newly created Socket object, or <code>null</code> if there was an error creating one.
	 */
	public Socket createSocket(InetAddress address, int port, long connectTimeOut, int maxAttempts, boolean silent)
	{
		return createSocket(new TcpEndPointIdentifier(address, port), connectTimeOut, maxAttempts, silent);
	}
   
   /**
    * Method that makes it possible to make a certain number of attempts at a certail interval to create a socket for 
    * the specified address.
    *  
    * @param address the address the Socket is to be connected to.
    * @param port the port on the remote host the Socket is to be connected to.
    * @param connectTimeOut the the interval between the connect attempts (the maximum time a connect attempt is allowed to take).
    * @param maxAttempts the maximum number of attempts that will be made to create a socket.
    * @param silent boolean flag indicating if errors/warning should be logged while trying to connect.
    * 
    * @return a newly created Socket object, or <code>null</code> if there was an error creating one.
    */
   public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort, long connectTimeOut, int maxAttempts, boolean silent)
   {
      return createSocket(new TcpEndPointIdentifier(address, port), new TcpEndPointIdentifier(localAddress, localPort), connectTimeOut, maxAttempts, silent);
   }
   
   /**
    * Method that makes it possible to make a certain number of attempts at a certail interval to create a socket for 
    * the specified address.
    *  
    * @param remoteAddress the address for which to create a socket.
    * @param connectTimeOut the the interval between the connect attempts (the maximum time a connect attempt is allowed to take).
    * @param maxAttempts the maximum number of attempts that will be made to create a socket.
    * @param silent boolean flag indicating if errors/warning should be logged while trying to connect.
    * 
    * @return a newly created Socket object, or <code>null</code> if there was an error creating one.
    */
   public Socket createSocket(TcpEndPointIdentifier remoteAddress, long connectTimeOut, int maxAttempts, boolean silent)
   {
      return createSocket(remoteAddress, null, connectTimeOut, maxAttempts, silent);
   }
	
	/**
	 * Method that makes it possible to make a certain number of attempts at a certail interval to create a socket for 
	 * the specified address.
	 *  
	 * @param remoteAddress the address for which to create a socket.
	 * @param connectTimeOut the the interval between the connect attempts (the maximum time a connect attempt is allowed to take).
	 * @param maxAttempts the maximum number of attempts that will be made to create a socket.
	 * @param silent boolean flag indicating if errors/warning should be logged while trying to connect.
	 * 
	 * @return a newly created Socket object, or <code>null</code> if there was an error creating one.
	 */
	public Socket createSocket(TcpEndPointIdentifier remoteAddress, TcpEndPointIdentifier localAddress, long connectTimeOut, int maxAttempts, boolean silent)
	{
		Socket socket = null;
		long connectStartTime;
		long newConnectAttemptWaitTime;
		
		for(int attempt = 0; (socket == null) && (attempt < maxAttempts); attempt++)
		{
			connectStartTime = System.currentTimeMillis();
			try
			{
            if( localAddress != null )
            {
               if( (this.parent != null) && this.parent.isDebugMode() )
               {
                  this.parent.logDebug("Attempting to create socket for address: " + remoteAddress.getInetAddress() + ", port: " + remoteAddress.getPort() + ", local address: " + 
                        localAddress.getInetAddress() + ", local port: " + localAddress.getPort() + ", connect timeout: " + connectTimeOut + ".");
               }
               
               socket = createSocket(remoteAddress.getInetAddress(), remoteAddress.getPort(), localAddress.getInetAddress(), localAddress.getPort(), connectTimeOut);
            }
            else if( (this.clientSocketBindAddress != null) || (this.clientSocketBindPortBegin > 0) || (this.clientSocketBindPortEnd > 0) )
            {
               boolean bindSuccess = false;
               int beginPort = this.clientSocketBindPortBegin;
               if( beginPort <= 0 ) beginPort = 1; 
               int endPort = this.clientSocketBindPortEnd;
               if( endPort <= 0 ) endPort = 0xFFFF;
               String clientAddress = this.clientSocketBindAddress;
               if( (clientAddress == null) || (clientAddress.trim().length() == 0) ) clientAddress = TcpEndPointIdentifier.anyLocalHostString;
               
               if( (this.parent != null) && this.parent.isDebugMode() )
               {
                  this.parent.logDebug("Attempting to create socket for address: " + remoteAddress.getInetAddress() + ", port: " + remoteAddress.getPort() + ", local address: " + 
                        localAddress.getInetAddress() + ", local port range: " + beginPort + " - " + endPort + ", connect timeout: " + connectTimeOut + ".");
               }
               
               while( !bindSuccess && (beginPort <= endPort) )
               {
                  try
                  {
                     socket = createSocket(remoteAddress.getInetAddress(), remoteAddress.getPort(), InetAddress.getByName(clientAddress), beginPort++, connectTimeOut);
                     bindSuccess = true;
                  }
                  catch(BindException bie)
                  {
                     bindSuccess = false;
                  }
               }
               
               if( socket == null )
               {
                  if( this.parent != null ) this.parent.logWarning("Cannot create socket. Unable to bind - clientAddress: " + clientAddress + ", beginPort: " + beginPort + ", endPort: " + endPort + "!");
                  else JServerUtilities.logWarning(name, "Cannot create socket. Unable to bind - clientAddress: " + clientAddress + ", beginPort: " + beginPort + ", endPort: " + endPort + "!");
                  return null;
               }
            }
            else
            {
               if( (this.parent != null) && this.parent.isDebugMode() )
               {
                  this.parent.logDebug("Attempting to create socket for address: " + remoteAddress.getInetAddress() + ", port: " + remoteAddress.getPort() + ", connect timeout: " + connectTimeOut + ".");
               }
               
               socket = createSocket(remoteAddress.getInetAddress(), remoteAddress.getPort(), connectTimeOut);
            }
			}
			catch(Exception e)
			{
				if(attempt < (maxAttempts-1)) //Check if this is not the last connect attempt
				{
					if(!silent)
               {
                  if( this.parent != null ) this.parent.logWarning("Failed to connect to " + remoteAddress + " (attempt " + (attempt + 1) + " of " + maxAttempts + ")! Retrying. Exception is: " + e + ".");
                  else JServerUtilities.logWarning(name, "Failed to connect to " + remoteAddress + " (attempt " + (attempt + 1) + " of " + maxAttempts + ")! Retrying. Exception is: " + e + ".");
               }

					socket = null;
					newConnectAttemptWaitTime = connectTimeOut - (System.currentTimeMillis() - connectStartTime);
			
					if((newConnectAttemptWaitTime > 0) && (newConnectAttemptWaitTime <= connectTimeOut))
					{
						try
						{
							Thread.sleep(newConnectAttemptWaitTime); //Should it be possible to specify whether or not to sleep the duration of the remaining connectTimeOut time.
						}
						catch(InterruptedException ie)
						{
							if(!silent)
                     {
                        if( this.parent != null ) this.parent.logWarning("Interrupted while trying to connect to " + remoteAddress + ". Aborting.");
                        else JServerUtilities.logWarning(name, "Interrupted while trying to connect to " + remoteAddress + ". Aborting.");
                     }
							return null;
						}
					}
				}
			}
		}
		
		if( (socket == null) && (!silent) )
		{
			if(maxAttempts > 1)
         {
            if( this.parent != null ) this.parent.logWarning(maxAttempts +" consecutive unsuccessful connect attempts! Unable to connect to " + remoteAddress + "!");
            else JServerUtilities.logWarning(name, maxAttempts +" consecutive unsuccessful connect attempts! Unable to connect to " + remoteAddress + "!");
         }
			else if(maxAttempts == 1)
         {
            if( this.parent != null ) this.parent.logWarning("Unable to connect to " + remoteAddress + "!");
            else JServerUtilities.logWarning(name, "Unable to connect to " + remoteAddress + "!");
         }
		}
		return socket;
	}
}
