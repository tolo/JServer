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
package com.teletalk.jserver.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * Utility class for copying data from an input stream to an output stream.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1.1 (20060202)
 */
public class StreamCopyUtils
{
   private static final int COPY_BUFFER_SIZE = 8192;
   
   
   /**
    * Copies data from the input stream to the output stream.
    */
   public static long copy(final InputStream input, final OutputStream output) throws IOException
   {
      return copy(input, output, new byte[COPY_BUFFER_SIZE]);
   }
   
   /**
    * Copies data from the input stream to the output stream.
    */
   public static long copy(final InputStream input, final OutputStream output, final byte[] copyBuffer) throws IOException
   {
      int readBytes = -1;
      long bytesCopied = 0;
      
      while ((readBytes = input.read(copyBuffer)) != -1) 
      {
         output.write(copyBuffer, 0, readBytes);
         bytesCopied += readBytes;
      }
      output.flush();
      
      return bytesCopied; 
   }
   
   /**
    * Copies <code>dataLength</code> bytes from the input stream to the output stream.
    */
   public static void copy(final InputStream input, final OutputStream output, final long dataLength) throws IOException
   {
      copy(input, output, dataLength, new byte[COPY_BUFFER_SIZE]);
   }
   
   /**
    * Copies <code>dataLength</code> bytes from the input stream to the output stream.
    */
   public static void copy(final InputStream input, final OutputStream output, final long dataLength, final byte[] copyBuffer) throws IOException
   {
      int readBytes = 0;
      long dataLeftToWrite = dataLength;
      int copyCount;
      int bufferSize = copyBuffer.length;
                  
      while(dataLeftToWrite > 0)
      {
         copyCount = (dataLeftToWrite >= bufferSize) ? bufferSize : (int)dataLeftToWrite;

         readBytes = input.read(copyBuffer, 0, copyCount);
         if(readBytes < 0) throw new IOException("Error occurred while reading data from input stream! Got unexpected end of file!");
         
         output.write(copyBuffer, 0, readBytes);
            
         dataLeftToWrite -= readBytes;
      }
      
      output.flush();
   }
}
