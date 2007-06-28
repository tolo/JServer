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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Byte array output stream implementation that can "spill over" the buffer to a file when the buffer reaches a certain size, 
 * to conserve memory.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1.1 (20060202)
 */
public class SpillOverByteArrayOutputStream extends OutputStream
{
   private static class DirectAccessByteArrayOutputStream extends ByteArrayOutputStream
   {
      public DirectAccessByteArrayOutputStream(final int size)
      {
         super(size);
      }

      public synchronized byte[] toByteArray(final boolean copy)
      {
         if( copy ) return super.toByteArray();
         else return super.buf;
      }
   }
   
   
   private static final int WRITE_TO_BUFFER_SIZE = 8192;
   
   /** Default memory buffer initial size - 8192 bytes. */
   public static final int DEFAULT_MEMORY_BUFFER_INITIAL_SIZE = 8192;
   
   /** Default spill over limit - 1MB. */
   public static final int DEFAULT_SPILL_OVER_LIMIT = 1024*1024;
   
   
   private final int spillOverLimit;
   
   private final int memoryBufferInitialSize;
   
   private DirectAccessByteArrayOutputStream byteArrayOutputStream = null;
   
   
   private long bytesWritten = 0;
   
   private File spillOverFile = null;
   
   private boolean spillOverFailed = false;
   
   private BufferedOutputStream spillOverFileOutputStream = null;
   
   
   /**
    * Creates a new SpillOverByteArrayOutputStream widh a spill set to {@link #DEFAULT_SPILL_OVER_LIMIT}.
    */
   public SpillOverByteArrayOutputStream()
   {
      this(DEFAULT_MEMORY_BUFFER_INITIAL_SIZE, DEFAULT_SPILL_OVER_LIMIT);
   }
   
   /**
    * Creates a new SpillOverByteArrayOutputStream widh a spill set to {@link #DEFAULT_SPILL_OVER_LIMIT}.
    */
   public SpillOverByteArrayOutputStream(final int memoryBufferInitialSize)
   {
      this(memoryBufferInitialSize, DEFAULT_SPILL_OVER_LIMIT);
   }

   /**
    * Creates a new SpillOverByteArrayOutputStream.
    */
   public SpillOverByteArrayOutputStream(final int memoryBufferInitialSize, final int spillOverLimit)
   {
      this.memoryBufferInitialSize = memoryBufferInitialSize;
      this.spillOverLimit = spillOverLimit;
   }
   

   /**
    * Returns the current size of the buffer (written bytes).
    */
   public long size() 
   {
      return this.bytesWritten;
   }
   
   /**
    * Gets the spill over limit.
    */
   public int getSpillOverLimit()
   {
      return spillOverLimit;
   }
   
   /**
    * Gets the spill over file, if created. 
    */
   public File getSpillOverFile()
   {
      return this.spillOverFile;
   }
   
   /**
    * Checks if spill over has been performed. 
    */
   public boolean hasSpilledOver()
   {
      return this.spillOverFileOutputStream != null;
   }
   
   /**
    * Check if spill over is to be performed.
    */
   private void checkSpillOver(final int writeSize)
   {
      if( (this.spillOverFile == null) && (!this.spillOverFailed) && ((this.bytesWritten + writeSize) > this.spillOverLimit) )
      {
         spillOver();
      }
   }
   
   /**
    * Performs spill over.
    */
   private void spillOver()
   {
      try
      {
         this.spillOverFile = File.createTempFile("SpillOver" + Long.toHexString((long)System.identityHashCode(Thread.currentThread()) + (long)System.identityHashCode(this)), ".tmp");
         
         this.spillOverFileOutputStream = new BufferedOutputStream(new FileOutputStream(this.spillOverFile));
         if( this.byteArrayOutputStream != null )
         {
            this.byteArrayOutputStream.writeTo(this.spillOverFileOutputStream);
            this.byteArrayOutputStream = null;
         }
      }
      catch(Exception e)
      {
         this.spillOverFileOutputStream = null;
         this.spillOverFailed = true;
         
         Class commonsLoggingLogClass = null;
         try{
         commonsLoggingLogClass = Class.forName("org.apache.commons.logging.LogFactory");
         }catch(Throwable t){}
         
         if( commonsLoggingLogClass != null )
         {
            org.apache.commons.logging.LogFactory.getLog(this.getClass()).error("Error (" + e + ") creating spill over file!", e);
         }
         else
         {
            System.out.println("Error (" + e + ") creating spill over file!");
            e.printStackTrace();
         }
      }
   }
   
   /**
    * Gets the active output stream.
    */
   private OutputStream getActiveOutputStream()
   {
      if( this.hasSpilledOver() ) return this.spillOverFileOutputStream;
      else
      {
         if( this.byteArrayOutputStream == null ) this.byteArrayOutputStream = new DirectAccessByteArrayOutputStream(memoryBufferInitialSize);
         return this.byteArrayOutputStream;
      }
   }
   
   /**
    * Writes the contents of this stream to the specified output stream.
    */
   public void writeTo(final OutputStream out) throws IOException
   {
      if( this.hasSpilledOver() )
      {
         this.spillOverFileOutputStream.flush();
         
         BufferedInputStream input = new BufferedInputStream(new FileInputStream(this.spillOverFile));
         StreamCopyUtils.copy(input, out, this.bytesWritten);
         input.close();
      }
      else if( this.byteArrayOutputStream != null ) this.byteArrayOutputStream.writeTo(out);
   }
   
   /**
    * Gets an input stream for reading the contents of this SpillOverByteArrayOutputStream.
    * 
    * @since 2.1.6 (20070504)
    */
   public InputStream getInputStream() throws IOException
   {
      if( this.hasSpilledOver() )
      {
         this.spillOverFileOutputStream.flush();
         return new BufferedInputStream(new FileInputStream(this.spillOverFile));
      }
      else if( this.byteArrayOutputStream != null ) return new ByteArrayInputStream(this.byteArrayOutputStream.toByteArray(false));
      else return null;
   }
   
   /**
    * Gets the contents of this buffer as a byte array. Note that it is strongly recommended to use writeTo instead of this method, 
    * when possible.
    */
   public byte[] toByteArray() throws IOException
   {
      if( this.hasSpilledOver() )
      {
         this.spillOverFileOutputStream.flush();
         
         BufferedInputStream input = new BufferedInputStream(new FileInputStream(this.spillOverFile));
         DirectAccessByteArrayOutputStream toByteArrayStream = new DirectAccessByteArrayOutputStream(WRITE_TO_BUFFER_SIZE);
         StreamCopyUtils.copy(input, toByteArrayStream, this.bytesWritten);
         
         return toByteArrayStream.toByteArray(false);
      }
      else if( this.byteArrayOutputStream != null ) return this.byteArrayOutputStream.toByteArray(true);
      else return new byte[0];
   }
   
   /**
    * Reset the stream, cleans up resources and deletes any spill over file that has been created.
    */
   public void reset()
   {
      this.byteArrayOutputStream = null;
      this.bytesWritten = 0;
      this.spillOverFailed = false;
      if( this.spillOverFileOutputStream != null )
      {
         try{
            this.spillOverFileOutputStream.close();
         }catch (Exception e){}
         Thread.yield();
      }
      this.spillOverFileOutputStream = null;
      if( this.spillOverFile != null )
      {
         if( this.spillOverFile.delete() ) this.spillOverFile = null; 
      }
   }
   
   
   /* ##### OVERRIDDEN METHODS: ##### */
   
   
   /**
    * Writes the specified byte to this output stream.
    */
   public void write(final int b) throws IOException
   {
      this.checkSpillOver(1);
      
      this.getActiveOutputStream().write(b);
      
      this.bytesWritten++;
   }
   
   /**
    *  Writes the specified byte array to this output stream.
    */
   public void write(byte[] b) throws IOException
   {
      write(b, 0, b.length);
   }
   
   /**
    * Writes <code>len</code> bytes from the specified byte array to this output stream.
    */
   public void write(final byte[] b, final int off, final int len) throws IOException
   {
      this.checkSpillOver(len);
            
      this.getActiveOutputStream().write(b, off, len);
      
      this.bytesWritten += len;
   }

   /**
    * Calling this method has the same effect as calling {@link #reset()}.
    */
   public void close() throws IOException
   {
      reset();
   }

   /**
    * Called when this object is to be deleted.
    */
   protected void finalize() throws Throwable
   {
      if( this.spillOverFile != null ) this.spillOverFile.delete();
      this.spillOverFile = null;
      super.finalize();
   }
}
