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
package com.teletalk.jserver.tcp.messaging.admin.web.jetty;

import java.io.IOException;

import org.mortbay.io.Buffer;
import org.mortbay.io.ByteArrayBuffer;
import org.mortbay.io.EndPoint;
import org.mortbay.jetty.AbstractConnector;

/**
 * Connector implementation to be used with a single end point. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1.6 (20070503)
 */
public class SingleEndPointConnector extends AbstractConnector
{
   private final EndPoint endp;
   
   public SingleEndPointConnector(final EndPoint endPoint)
   {
      setPort(1);
      this.endp = endPoint;
   }

   public Object getConnection()
   {
      return endp;
   }

   protected Buffer newBuffer(int size)
   {
      return new ByteArrayBuffer(size);
   }

   protected void accept(int acceptorID) throws IOException, InterruptedException
   {
      throw new IOException("This implementation cannot accept new connections!");
   }

   public void open() throws IOException
   {
   }

   public void close() throws IOException
   {
   }

   public int getLocalPort()
   {
      return -1;
   }
}
