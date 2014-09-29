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

/**
 * 
 * @since 2.0
 */
public class RemoteInputStreamClient extends InputStream
{
   private final RemoteInputStream remoteInputStream;
   
   /**
    * 
    */
   public RemoteInputStreamClient(final RemoteInputStream remoteInputStream)
   {
      super();
      
      this.remoteInputStream = remoteInputStream;
   }
   
   public int read() throws IOException
   {
      return this.remoteInputStream.read();
   }

   public int read(byte b[]) throws IOException
   {
      return this.read(b, 0, b.length);
   }

   public int read(byte b[], int off, int len) throws IOException
   {
      Object[] returnValue = (Object[])this.remoteInputStream.read(len);
      int result = ((Integer)returnValue[0]).intValue();
      byte[] readBytes = ((byte[])returnValue[1]);

      if( result > 0 )
      {
         System.arraycopy(readBytes, 0, b, off, result);
      }
      
      return result;
   }
   
   public long skip(long n) throws IOException
   {
      return this.remoteInputStream.skip(n);
   }
   
   public int available() throws IOException
   {
      return this.remoteInputStream.available();
   }
   public void close() throws IOException
   {
      this.remoteInputStream.close();
   }

   public void mark(int readlimit)
   {
      try
      {
         this.remoteInputStream.mark(readlimit);
      }
      catch(Exception e){}
   }

   public void reset() throws IOException
   {
      this.remoteInputStream.reset();
   }

   public boolean markSupported()
   {
      try
      {
         return this.remoteInputStream.markSupported();
      }
      catch(Exception e)
      {
         return false;
      }
   }
}
