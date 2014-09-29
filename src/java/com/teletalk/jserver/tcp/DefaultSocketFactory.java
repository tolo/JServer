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
import java.net.ConnectException;
import java.net.InetAddress;
import java.net.Socket;

import com.teletalk.jserver.JServer;

/**
 * Default implementation of the {@link SocketFactory} interface for creation of standard sockets.
 * 
 * @see SocketFactory
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.1 build 690
 */
public class DefaultSocketFactory implements SocketFactory
{
	/**
	 * Creates an unconnected socket.
	 * 
	 * @throws IOException if a connection cannot be established.
	 */
	public Socket createSocket() throws IOException
	{
      return new Socket();
	}
   
	/**
	 * Creates a socket and connects it to the specified host and port
	 * 
	 * @param host the server host.
	 * @param port the server port.
	 * 
	 * @throws IOException if a connection cannot be established.
	 */
	public Socket createSocket(final InetAddress host, final int port) throws IOException
	{
		return new Socket(host, port);
	}
   
	/**
	 * Creates a socket and connects it to the specified host and port, using the specified timeout value. 
	 * A timeout of zero is interpreted as an infinite timeout. The connection will then block until established or an error occurs. 
	 * 
	 * @param host the server host.
	 * @param port the server port.
	 * @param connectTimeOut the timeout value to be used in milliseconds. 
	 * 
	 * @throws IOException if a connection cannot be established.
	 */
   public Socket createSocket(final InetAddress host, final int port, final int connectTimeOut) throws IOException
   {
		return createSocket(host, port, connectTimeOut, this);
   }
   
   public Socket createSocket(InetAddress host, int port, InetAddress localAddress, int localPort) throws IOException
   {
      return new Socket(host, port, localAddress, localPort);
   }
   
   public Socket createSocket(InetAddress host, int port, InetAddress localAddress, int localPort, int connectTimeOut) throws IOException
   {
      return createSocket(host, port, localAddress, localPort, connectTimeOut, this);
   }

	/**
	 * Static method that creates a socket and connects it to the specified host and port, using the specified timeout value and socket factory. 
	 * A timeout of zero is interpreted as an infinite timeout. The connection will then block until established or an error occurs.<br>
	 * <br>
	 * <i>Note:</i> This method uses different implementations for pre 1.4 and post 1.4 VMs.
	 * 
	 * @param host the server host.
	 * @param port the server port.
	 * @param connectTimeOut the timeout value to be used in milliseconds.
    * @param socketFactory the socket factory to use for creating a socket.
	 * 
	 * @throws IOException if a connection cannot be established.
	 */
	public static Socket createSocket(final InetAddress host, final int port, final int connectTimeOut, final SocketFactory socketFactory) throws IOException
	{
      return createSocket(host, port, null, -1, connectTimeOut, socketFactory);
	}
   
	/**
    * Static method that creates a socket and connects it to the specified host and port, using the specified timeout value and socket factory. 
    * A timeout of zero is interpreted as an infinite timeout. The connection will then block until established or an error occurs.<br>
    * <br>
    * <i>Note:</i> This method uses different implementations for pre 1.4 and post 1.4 VMs.
    * 
    * @param host the server host.
    * @param port the server port.
    * @param localAddress the local address to bind to.
    * @param localPort the local port to bind to.
    * @param connectTimeOut the timeout value to be used in milliseconds.
    * @param socketFactory the socket factory to use for creating a socket.
    * 
    * @throws IOException if a connection cannot be established.
    */
   public static Socket createSocket(final InetAddress host, final int port, InetAddress localAddress, int localPort, final int connectTimeOut, final SocketFactory socketFactory) throws IOException
   {
      IOException connectionException = null;
      Socket socket;
      
      SocketConnectorTask connector = new SocketConnectorTask(host, port, localAddress, localPort, socketFactory);
      if( (JServer.getJServer() != null) && (JServer.getJServer().getEventQueue() != null) )
      {
         JServer.getJServer().getEventQueue().queueEvent(connector);
      }
      else
      {
         connector.runInThread(); 
      }
   
      try
      {
         connector.waitForConnectionResult(connectTimeOut);
      }
      catch(InterruptedException e)
      {
         connector.kill();
         throw new IOException("Interupted while waiting for socket to get connected.");
      }
      
      if(!connector.isConnected())
      {
         connectionException = connector.getConnectionException();
         
         if(connectionException != null) throw connectionException;
         else
         {
            connector.kill();
            throw new ConnectException("Connection timed out");
         }
      }
      
      socket = connector.getSocket();
      connector = null;
      
      return socket;
   }
	
   
   /* INTERNAL INTERFACES AND CLASSES */
   
	/**
	 * Internal thread class used to connect a socket.
	 */
   private static final class SocketConnectorTask implements Runnable
	{
		private boolean connected = false;
		private IOException connectionException = null;
		
		private final InetAddress address;
		private final int port;
      private final InetAddress localAddress;
      private final int localPort;
		
		private final SocketFactory socketFactory;
		private Socket socket;
      
      private Thread thread = null;

      public SocketConnectorTask(InetAddress address, int port, InetAddress localAddress, int localPort, SocketFactory socketFactory)
		{
			this.address = address;
			this.port = port;
         this.localAddress = localAddress;
         this.localPort = localPort;
			this.socketFactory = socketFactory;
		}
      
      void runInThread()
      {
         this.thread = new Thread(this);
         this.thread.setDaemon(true);
         this.thread.start();
      }
		
		void kill()
		{
			if( this.thread != null ) this.thread.interrupt();
		}
		
		public void run()
		{
			try
			{
            if( (localAddress == null ) && (localPort < 0) )
            {
               socket = this.socketFactory.createSocket(address, port);
            }
            else
            {
               socket = this.socketFactory.createSocket(address, port, localAddress, localPort);
            }
				connected = true;
			}
			catch(IOException e)
			{
				connectionException = e;
				connected = false;
			}
			finally
			{
				synchronized(this)
				{
					notify();
				}
			}
		}
		
		boolean isConnected()
		{
			return connected;
		}

		synchronized final void waitForConnectionResult(long connectTimeOut) throws InterruptedException
		{
			wait(connectTimeOut);
		}
		
		IOException getConnectionException()
		{
			return connectionException;
		}
		
		Socket getSocket()
		{
			return socket;
		}
	}
}
