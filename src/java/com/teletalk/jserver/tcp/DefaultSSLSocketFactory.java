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
import java.net.InetAddress;
import java.net.Socket;

/**
 * Convenience implementation of the {@link SocketFactory} interface that delegates creation of SSL client sockets 
 * to the default SSL socket factory, obtained using the class <code>javax.net.ssl.SSLSocketFactory</code>.
 * 
 * @see SocketFactory
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.2
 */
public class DefaultSSLSocketFactory implements SocketFactory
{
   /**
    * Creates an unconnected socket.
    * 
    * @throws IOException if a connection cannot be established.
    */
   public Socket createSocket() throws IOException
   {
      return javax.net.ssl.SSLSocketFactory.getDefault().createSocket();
   }
   
   /**
    * Creates a socket connected to the specified host and port
    * 
    * @param host the server host.
    * @param port the server port.
    * 
    * @throws IOException if a connection cannot be established.
    */
   public Socket createSocket(InetAddress host, int port) throws IOException
   {
      return javax.net.ssl.SSLSocketFactory.getDefault().createSocket(host, port);
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
      return DefaultSocketFactory.createSocket(host, port, connectTimeOut, this);
   }
   
   public Socket createSocket(InetAddress host, int port, InetAddress localAddress, int localPort) throws IOException
   {
      return javax.net.ssl.SSLSocketFactory.getDefault().createSocket(host, port, localAddress, localPort);
   }
   
   public Socket createSocket(InetAddress host, int port, InetAddress localAddress, int localPort, int connectTimeOut) throws IOException
   {
      return DefaultSocketFactory.createSocket(host, port, localAddress, localPort, connectTimeOut, this);
   }
}
