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
package com.teletalk.jserver.rmi.remote;

import java.io.IOException;
import java.io.InputStream;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;

/**
 * 
 * @since 2.0
 */
public class RemoteInputStreamImpl extends UnicastRemoteObject implements RemoteInputStream
{
   static final long serialVersionUID = 3005348239392489766L;

   private final InputStream inputStream;
   
   /**
    */
   public RemoteInputStreamImpl(final InputStream inputStream) throws RemoteException
   {
      this.inputStream = inputStream;
   }
   
   public int available() throws RemoteException
   {
      try
      {
         return this.inputStream.available();
      }
      catch(IOException ioe)
      {
         throw new RemoteException("Caught IOException!", ioe);
      }
   }
   
   public void close() throws RemoteException
   {
      try
      {
         this.inputStream.close();
      }
      catch(IOException ioe)
      {
         throw new RemoteException("Caught IOException!", ioe);
      }
   }
   
   public void mark(int readlimit) throws RemoteException
   {
      this.inputStream.mark(readlimit);
   }
   
   public boolean markSupported() throws RemoteException
   {
      return this.inputStream.markSupported();
   }

   public int read() throws RemoteException
   {
      try
      {
         return this.inputStream.read();
      }
      catch(IOException ioe)
      {
         throw new RemoteException("Caught IOException!", ioe);
      }
   }
   
   public Object[] read(int len) throws RemoteException
   {
      try
      {
         byte[] b = new byte[len];
         int result = this.inputStream.read(b);
         return new Object[]{new Integer(result), b};
      }
      catch(IOException ioe)
      {
         throw new RemoteException("Caught IOException!", ioe);
      }
   }

   public void reset() throws RemoteException
   {
      try
      {
         this.inputStream.reset();
      }
      catch(IOException ioe)
      {
         throw new RemoteException("Caught IOException!", ioe);
      }
   }

   public long skip(long n) throws RemoteException
   {
      try
      {
         return this.inputStream.skip(n);      
      }
      catch(IOException ioe)
      {
         throw new RemoteException("Caught IOException!", ioe);
      }
   }
}
