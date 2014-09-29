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
package com.teletalk.jserver.tcp.messaging;

import java.io.DataInputStream;
import java.io.EOFException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import com.teletalk.jserver.util.InputStreamer;
import com.teletalk.jserver.util.NoHeadersObjectInputStream;

/**
 * Input stream class that handles all reading of data from the socket of a {@link MessagingEndPoint}. 
 * Instances of this class can be associated with a {@link Message} object when a {@link MessageHeader} 
 * is received in a MessagingEndPoint. The purpose of this association is to impose a limit on the number of bytes 
 * that can be read ({@link MessageHeader#getBodyLength()}) from the 
 * stream by a user, attempting to read the message body.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.2
 */
public class MessagingEndPointInputStream extends FilterInputStream implements InputStreamer
{
	private volatile long readProgress;
	
	private volatile long maxReadCount;
	private volatile long readCount; // Bytes read since the last time setCurrentMessage was called
	
	private Message message;
	
	private final DataInputStream dataInput;
	private final InputStream defaultInputStream;
   
   private ObjectInputStream currentContextObjectInputStream;
	
	/**
	 * Creates a new MessagingEndPointInputStream connected to the specified InputStream.
	 * 
	 * @param inputStream the InputStream to read data from.
	 */
	public MessagingEndPointInputStream(final InputStream inputStream)
	{
		super(inputStream);
		this.dataInput = new DataInputStream(this);
						
		this.readProgress = 0;
		
		this.maxReadCount = 0;
		this.readCount = 0;
				
		this.message = null;
		
		this.defaultInputStream = inputStream;
      
      this.currentContextObjectInputStream = null;
	}
	
	/**
	 * Gets the read progress value. This value is incremented every time a byte is read using one of 
	 * the read (or skip) methods of this class.
	 * 
	 * @return the read progress value.
	 */
	public long getReadProgress()
	{
		return this.readProgress;
	}
	
	/**
	 * Sets the message that currently is to be associated with this MessagingEndPointInputStream.
	 * 
	 * @param message the message that currently is to be associated with this MessagingEndPointInputStream.
	 */
	protected void setCurrentMessage(final Message message)
	{
		if( message != null )
		{
			this.maxReadCount = message.getHeader().getBodyLength();
			this.readCount = 0;
		}
		this.message = message;
	}
	
	/**
	 * Gets the message that currently is to be associated with this MessagingEndPointInputStream.
	 * 
	 * @since 1.3.1
	 */
	public Message getCurrentMessage()
	{
		return message;
	}
	
	/**
	 * Removes the association between the current message and this MessagingEndPointInputStream.
	 */
	protected void resetCurrentMessage()
	{
		this.message = null;
	}
	
	/**
	 * Gets the total number of bytes that can be read from this MessagingEndPointInputStream for the current 
	 * message.
	 * 
	 * @return the total number of bytes that can be read for the current message.
	 */
	public long getMaxReadCount()
	{
		return this.maxReadCount;
	}
			
	/**
	 * Gets the number of bytes that has been read from this MessagingEndPointInputStream for the current 
	 * message.
	 * 
	 * @return the number of bytes that has been read for the current message.
	 */
	public long getReadCount()
	{
		return readCount;
	}
	
	/**
	 * Gets the stream that this MessagingEndPointInputStream reads data from. 
	 * 
	 * @since 1.3.1, build 670.
	 */
	protected InputStream getStream()
	{
		return super.in;
	}

	/**
	 * Sets the stream that this MessagingEndPointInputStream is to read data from. If parameter <code>inputStream</code> is
    * <code>null</code>, the stream will be reset to stream that was initially associated with this object. 
	 * 
	 * @since 1.3.1, build 670.
	 */
	protected void setStream(final InputStream inputStream)
	{
		if( inputStream != null )
		{
			super.in = inputStream;
		}
		else
		{ 
			super.in = this.defaultInputStream;
		}
	}

	/**
	 * Reads a single byte from the stream. The value byte is returned as an int in the range 0 to 255. If no byte is available because the 
	 * end of the stream has been reached, the value -1 is returned. -1 is also returned if this MessagingEndPointInputStream is currently 
	 * associated with a message, and the maximum number of possible bytes have already been read (as returned by the method 
	 * {@link #getMaxReadCount()}).<br>
	 * <br>
	 * This method blocks until input data is available, the end of the stream is detected, or an exception is thrown. 
	 * 
	 * @return the read byte or -1 the maximum number of possible bytes have already been read.
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public int read() throws IOException 
	{
      if( (message == null) || (readCount < maxReadCount) )
		{
			int result = 0;
			
			if( message != null )
			{
				try
				{
               result = in.read();
					
					if(result >= 0)
					{
						this.readCount++;
						this.readProgress++;
					}
				}
				catch(Throwable t)
				{
					this.message.handleError(t);
				}
			}
			else
			{
				result = in.read();
			}
									
			if(result < 0)
			{
				if(this.message != null) this.message.signalReadCompletion();
				throw new EOFException("Unexpected end of file!");
			}
			else if( (this.message != null) && (readCount == maxReadCount) ) this.message.signalReadCompletion();

			return result;
		}
		else
			return -1;
    }

	/**
	 * Reads an array of bytes from the stream. The number of bytes actually read is returned as an integer. This method 
	 * blocks until <code>b.length</code> number of bytes have been read, end of file is detected or an exception is thrown.<br>
	 * <br>
	 * If this MessagingEndPointInputStream is currently associated with a message, the maximum number of bytes that can be 
	 * read will be <code>{@link #getMaxReadCount()} - {@link #getReadCount()}</code>.
	 * 
	 * @param b the buffer into which the data is read.
	 * 
	 * @return the total number of bytes read into the buffer, or -1 is there is no more data because the end of the stream has been reached.
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public int read(final byte b[]) throws IOException 
	{
		return read(b, 0, b.length);
	}

	/**
	 * Reads an array of bytes from the stream. The number of bytes actually read is returned as an integer. This method 
	 * blocks until <code>len</code> number of bytes have been read, end of file is detected or an exception is thrown.<br>
	 * <br>
	 * If this MessagingEndPointInputStream is currently associated with a message, the maximum number of bytes that can be 
	 * read will be <code>{@link #getMaxReadCount()} - {@link #getReadCount()}</code>.
	 * 
	 * @param b the buffer into which the data is read.
	 * @param off the start offset in array b at which the data is written.
	 * @param len the maximum number of bytes to read.
	 * 
	 * @return the total number of bytes read into the buffer, or -1 is there is no more data because the end of the stream has been reached.
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public int read(final byte b[], final int off, int len) throws IOException 
	{
      if( (message == null) || (readCount < maxReadCount) )
		{
			int result = 0;
			
			if( message != null )
			{
				// Check max bytes that can be read if this stream is currently associated with a message
				len = (int)Math.min(len, (maxReadCount - readCount));
				
				try
				{
					result = in.read(b, off, len);
					
					if(result >= 0)
					{
						this.readCount += result;
						this.readProgress += result;
					}
				}
				catch(Throwable t)
				{
					this.message.handleError(t);
				}
			}
			else
			{
				result = in.read(b, off, len);
			}
									
			if(result < 0)
			{
				if( message != null ) this.message.signalReadCompletion();
				throw new EOFException("Unexpected end of file!");
			}
			else if( ( message != null ) && (readCount == maxReadCount) ) this.message.signalReadCompletion();
			
			return result;
		}
		else 
			return -1;
    }
    
	/**
	 * Attempts to skip over and discard n bytes of data from this input stream.
	 * 
	 * @param n the number of bytes to be skipped.
	 * 
	 * @return the actual number of bytes skipped.
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public long skip(long n) throws IOException 
	{
      if( (message == null) || (readCount < maxReadCount) )
		{
			long result = 0;
			
			if( message != null )
			{
				n = (int)Math.min(n, (maxReadCount - readCount));
				
				try
				{
               result = in.skip(n);
					
					if(result >= 0)
					{
						this.readCount += result;
						this.readProgress += result;
					}
				}
				catch(Throwable t)
				{
					this.message.handleError(t);
				}
			}
			else
			{
				result = in.skip(n);
			}
												
			if(result < 0)
			{
				if( message != null ) this.message.signalReadCompletion();
				throw new EOFException("Unexpected end of file!");
			}
			else if( ( message != null ) && (readCount == maxReadCount) ) this.message.signalReadCompletion();
		
			return result;
		}
		else 
			return -1;
    }
	
	/**
	 * Skips all the remaining data in this stream (up to {@link #getMaxReadCount()} bytes) for the current message and signals read completion.
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public void skipAll() throws IOException
	{
		if( message != null )
		{
			long leftToSkip = maxReadCount-readCount;
			long result = 0;
					
			while(leftToSkip > 0)
			{
				try
				{
               result = in.skip(leftToSkip);
					
					leftToSkip -= result;
					this.readCount += result;
					this.readProgress += result;
				}
				catch(Throwable t)
				{
					this.message.handleError(t);
				}
			}
		
			this.message.signalReadCompletion();
		}
	}
	
	/**
	 * Marks the current position in this input stream. This method is not implemented.
	 * 
	 * @param readlimit the maximum limit of bytes that can be read before the mark position becomes invalid.
	 */
	public void mark(int readlimit) 
	{
	}

	/**
	 * Repositions this stream to the position at the time the mark method was last called on this input stream. This method is not 
	 * implemented and will throw an exception.
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public void reset() throws IOException 
	{
		throw new IOException("mark/reset not supported");
	}

	/**
	 * Tests if this input stream supports the mark and reset methods. The markSupported method of InputStream returns false.
	 * 
	 * @return <code>true</code> if this class supports the mark and reset method; <code>false</code> otherwise.
	 */
	public boolean markSupported() 
	{
		return false;
	}
	
	/**
	 * Returns the number of bytes that can be read (or skipped over) from this input stream without blocking by the next caller of a method for 
	 * this input stream.
	 * 
	 * @return the number of bytes that can be read from this input stream without blocking.
	 * 
	 * @throws IOException if an I/O error occurs.
	 */
	public int available() throws IOException
	{
		int avail = 0;
		
		if( this.message != null )
		{
			try
			{		
				avail = in.available();
				
				if(avail > (maxReadCount - readCount)) return (int)(maxReadCount - readCount);
				else return avail;
			}
			catch(Throwable t)
			{
				this.message.handleError(t);
				return 0;
			}
		}
		else
		{
			return in.available();
		}
	}
   
   /*### OVERRIDDEN/DELEGATE METHODS FROM DATAINPUT - BEGIN ###*/
      
   public ObjectInputStream getContextObjectInputStream() throws IOException
   {
      if( this.currentContextObjectInputStream == null ) this.currentContextObjectInputStream = new NoHeadersObjectInputStream(this);
      return this.currentContextObjectInputStream;
   }
   
   /*### OVERRIDDEN/DELEGATE METHODS FROM DATAINPUT - END ###*/
   
   public void setContextObjectInputStream(ObjectInputStream contextObjectInputStream) throws IOException
   {
      this.currentContextObjectInputStream = contextObjectInputStream;
   }
   
   /*### OVERRIDDEN/DELEGATE METHODS FROM DATAINPUT - BEGIN ###*/

   /**
    * Reads one input byte and returns.<br><br>
    * See the general contract of the <code>readBoolean</code>
    * method of <code>java.io.DataInput</code>.
    *
    * @return the <code>boolean</code> value read.
    * 
    * @exception EOFException if this stream reaches the end before reading all the bytes.
    * @exception IOException if an I/O error occurs.
    */
   public boolean readBoolean() throws IOException
   {
      return dataInput.readBoolean();
   }

   /**
    * Reads and returns one input byte.<br><br>
    * See the general contract of the <code>readByte</code>
    * method of <code>java.io.DataInput</code>.
    *
    * @return the 8-bit value read.
    * 
    * @exception EOFException if this stream reaches the end before reading all the bytes.
    * @exception IOException if an I/O error occurs.
    */
   public byte readByte() throws IOException
   {
      return dataInput.readByte();
   }

   /**
    * Reads an input <code>char</code> and returns the <code>char</code> value.<br><br>
    * See the general contract of the <code>readChar</code>
    * method of <code>java.io.DataInput</code>.
    *
    * @return the Unicode <code>char</code> read.
    * 
    * @exception EOFException if this stream reaches the end before reading all the bytes.
    * @exception IOException if an I/O error occurs.
    */
   public char readChar() throws IOException
   {
      return dataInput.readChar();
   }

   /**
    * Reads eight input bytes and returns
    * a <code>double</code> value.<br><br>
    * See the general contract of the <code>readDouble</code>
    * method of <code>java.io.DataInput</code>.
    *
    * @return the <code>double</code> value read.
    * 
    * @exception EOFException if this stream reaches the end before reading all the bytes.
    * @exception IOException if an I/O error occurs.
    */
   public double readDouble() throws IOException
   {
      return dataInput.readDouble();
   }

   /**
    * Reads four input bytes and returns
    * a <code>float</code> value. <br><br>
    * See the general contract of the <code>readFloat</code>
    * method of <code>java.io.DataInput</code>.
    *
    * @return the <code>float</code> value read.
    * 
    * @exception EOFException if this stream reaches the end before reading all the bytes.
    * @exception IOException if an I/O error occurs.
    */
   public float readFloat() throws IOException
   {
      return dataInput.readFloat();
   }

   /**
    * Reads some bytes from an input
    * stream and stores them into the buffer
    * array <code>b</code>. The number of bytes
    * read is equal
    * to the length of <code>b</code>.<br><br>
    * See the general contract of the <code>readFully(byte[])</code>
    * method of <code>java.io.DataInput</code>.
    *
    * @param b the buffer into which the data is read.
    * 
    * @exception EOFException if this stream reaches the end before reading all the bytes.
    * @exception IOException if an I/O error occurs.
    */
   public void readFully(byte[] b) throws IOException
   {
      dataInput.readFully(b);
   }

   /**
    * Reads <code>len</code>
    * bytes from
    * an input stream.<br><br>
    * See the general contract of the <code>readFully(byte[],int,int)</code>
    * method of <code>java.io.DataInput</code>.
    *
    * @param b the buffer into which the data is read.
    * @param off an int specifying the offset into the data.
    * @param len an int specifying the number of bytes to read.
    * 
    * @exception EOFException if this stream reaches the end before reading all the bytes.
    * @exception IOException if an I/O error occurs.
    */
   public void readFully(byte[] b, int off, int len) throws IOException
   {
      dataInput.readFully(b, off, len);
   }

   /**
    * Reads four input bytes and returns an
    * <code>int</code> value. <br><br>
    * See the general contract of the <code>readInt</code>
    * method of <code>java.io.DataInput</code>.
    *
    * @return the <code>int</code> value read.
    * 
    * @exception EOFException if this stream reaches the end before reading all the bytes.
    * @exception IOException if an I/O error occurs.
    */
   public int readInt() throws IOException
   {
      return dataInput.readInt();
   }

   /**
    * Reads the next line of text from the input stream.<br><br>
    * See the general contract of the <code>readLine</code>
    * method of <code>java.io.DataInput</code>.
    *
    * @return the next line of text from the input stream, or <CODE>null</CODE> if the end of file is encountered before a byte can be read.
    *  
    * @exception  IOException  if an I/O error occurs.
    * 
    * @deprecated This method does not properly convert bytes to characters.
    */
   public String readLine() throws IOException
   {
      return dataInput.readLine();
   }

   /**
    * Reads eight input bytes and returns
    * a <code>long</code> value.<br><br>
    * See the general contract of the <code>readLong</code>
    * method of <code>java.io.DataInput</code>.
    *
    * @return the <code>long</code> value read.
    * 
    * @exception EOFException if this stream reaches the end before reading all the bytes.
    * @exception IOException if an I/O error occurs.
    */
   public long readLong() throws IOException
   {
      return dataInput.readLong();
   }

   /**
    * Reads two input bytes and returns
    * a <code>short</code> value.<br><br>
    * See the general contract of the <code>readShort</code>
    * method of <code>java.io.DataInput</code>.
    *
    * @return the 16-bit value read.
    * 
    * @exception EOFException if this stream reaches the end before reading all the bytes.
    * @exception IOException if an I/O error occurs.
    */
   public short readShort() throws IOException
   {
      return dataInput.readShort();
   }

   /**
    * Reads one input byte, zero-extends
    * it to type <code>int</code>, and returns
    * the result, which is therefore in the range
    * <code>0</code>
    * through <code>255</code>.<br><br>
    * See the general contract of the <code>readUnsignedByte</code>
    * method of <code>java.io.DataInput</code>.
    *
    * @return the unsigned 8-bit value read.
    * 
    * @exception EOFException if this stream reaches the end before reading all the bytes.
    * @exception IOException if an I/O error occurs.
    */
   public int readUnsignedByte() throws IOException
   {
      return dataInput.readUnsignedByte();
   }

   /**
    * Reads two input bytes and returns
    * an <code>int</code> value in the range <code>0</code>
    * through <code>65535</code>. <br><br>
    * See the general contract of the <code>readUnsignedShort</code>
    * method of <code>java.io.DataInput</code>.
    *
    * @return the unsigned 16-bit value read.
    * 
    * @exception EOFException if this stream reaches the end before reading all the bytes.
    * @exception IOException if an I/O error occurs.
    */
   public int readUnsignedShort() throws IOException
   {
      return dataInput.readUnsignedShort();
   }

   /**
    * Reads in a string that has been encoded using a modified UTF-8 format.<br><br>
    * See the general contract of the <code>readUTF</code>
    * method of <code>java.io.DataInput</code>.
    *
    * @return a Unicode string.
    * 
    * @exception EOFException if this stream reaches the end before reading all the bytes.
    * @exception IOException if an I/O error occurs.
    */
   public String readUTF() throws IOException
   {
      return dataInput.readUTF();
   }

   /**
    * Makes an attempt to skip over
    * <code>n</code> bytes
    * of data from the input
    * stream, discarding the skipped bytes.<br><br>
    * See the general contract of the <code>skipBytes(int)</code>
    * method of <code>java.io.DataInput</code>.
    *
    * @param n the number of bytes to be skipped.
    * @return the number of bytes actually skipped.
    * 
    * @exception EOFException if this stream reaches the end before reading all the bytes.
    * @exception IOException if an I/O error occurs.
    */
   public int skipBytes(int n) throws IOException
   {
      return dataInput.skipBytes(n);
   }
   
   /*### OVERRIDDEN/DELEGATE METHODS FROM DATAINPUT - END ###*/
}
