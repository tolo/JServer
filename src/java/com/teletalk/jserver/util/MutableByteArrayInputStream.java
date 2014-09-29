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
import java.io.ObjectStreamConstants;

/**
 * InputStream class that reads data from a byte array. The byte array may be specified in the constructor or 
 * using one of the <code>setByteArray</code> ({@link #setByteArray(byte[])} or {@link #setByteArray(byte[], int, int )}) methods. 
 * The <code>setByteArray</code> methods may be called any number of times to set the current byte array to read data 
 * from, making it possible to reuse objects of this class.<br>
 * <br>
 * MutableByteArrayInputStream is also capable of returning a <code>TC_RESET</code> code (see java.io.ObjectStreamConstants) when one of the 
 * read methods is called after a new byte array has been specified. This functionality is provided to make it possible to force the reset of any 
 * NoHeadersObjectInputStream that is connected to this MutableByteArrayInputStream. A flag (which has the default value of <code>false</code>) is 
 * provided to indicate if this functionality is enabled or disabled. This flag can be accessed by the methods {#link #setReturnObjectStreamResetCode(boolean)} 
 * and {@link #isReturningObjectStreamResetCode()}.
 * 
 * @see NoHeadersObjectInputStream
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1 Build 545
 */
public class MutableByteArrayInputStream extends InputStream
{
	private boolean returnObjectStreamResetCode = false;
	private boolean objectStreamResetCodeReturned = false;
	
	private byte[] byteArray = null;	private int byteDataMarkOffset;	private int byteDataMaxPos;		private int currentByteArrayPos;	
	/**
	 * Creates a new MutableByteArrayInputStream without any byte array to read data from.
	 */	public MutableByteArrayInputStream()
	{
		this(null, 0, 0);
	}	
	/**
	 * Creates a new MutableByteArrayInputStream that will initially read data from the specified byte array. 
	 * Data will be read beginning at index 0 and ending at index <code>byteArray.length - 1</code>.<br>
	 * <br>
	 * Note that no copy will be performed on the specified byte array.
	 * 
	 * @param byteArray the new byte array.
	 */	public MutableByteArrayInputStream(byte[] byteArray)
	{
		this(byteArray, 0, byteArray.length);
	}
	
	/**
	 * Creates a new MutableByteArrayInputStream that will initially read data from the specified byte array. 
	 * Data will be read beginning at the index specified by parameter <code>offset</code> and ending at 
	 * index <code>length - 1</code>.<br>
	 * <br>
	 * Note that no copy will be performed on the specified byte array.
	 * 
	 * @param byteArray the new byte array.
	 * @param offset the offset (index) at which this stream is to begin reading at.
	 * @param length the maximum number of bytes that this stream is to read from the byte array.
	 */	public MutableByteArrayInputStream(byte[] byteArray, int offset, int length)
	{
		super();				this.byteArray = byteArray;		this.byteDataMarkOffset = offset;		this.byteDataMaxPos = length;				this.currentByteArrayPos = offset;	}
	/**
	 * Sets the byte array from which the stream is to read data. 
	 * Data will be read beginning at index 0 and ending at index <code>byteArray.length - 1</code>.<br>
	 * <br>
	 * Note that no copy will be performed on the specified byte array.
	 * 
	 * @param byteArray the new byte array.
	 */
	public void setByteArray(byte[] byteArray)
	{
		this.setByteArray(byteArray, 0, byteArray.length);
	}
		/**
	 * Sets the byte array from which the stream is to read data. 
	 * Data will be read beginning at the index specified by parameter <code>offset</code> and ending at 
	 * index <code>length - 1</code>.<br>
	 * <br>
	 * Note that no copy will be performed on the specified byte array.
	 * 
	 * @param byteArray the new byte array.
	 * @param offset the offset (index) at which this stream is to begin reading at.
	 * @param length the maximum number of bytes that this stream is to read from the byte array.
	 */
	public void setByteArray(byte[] byteArray, int offset, int length)
	{
		this.byteArray = byteArray;		this.byteDataMarkOffset = offset;		this.byteDataMaxPos = length;				this.currentByteArrayPos = offset;		
		// Set the objectStreamResetCodeReturned flag to false, indicating that a TC_RESET code should be 
		// returned in the next read if the returnObjectStreamResetCode is set to true.		this.objectStreamResetCodeReturned = false; 
	}	
	/**
	 * Gets the current byte array.
	 * 
	 * @return the current byte array.
	 */	public byte[] getByteArray()	{		return this.byteArray;	}
	
	/**
	 * Sets the value of the flag indicating if this stream should return a <code>TC_RESET</code> code (see java.io.ObjectStreamConstants) 
	 * every time a new byte array is specified (see the <code>setByteArray</code> methods). If this flag is set to <code>true</code> 
	 * a <code>TC_RESET</code> code will be returned as the first element of data the first time any of <code>read</code> methods are called after 
	 * a new byte array has been set. <br>
	 * <br>
	 * The default value of this flag is <code>false</code>. 
	 * This flag is provided to make it possible to force the reset of any NoHeadersObjectInputStream that is connected to this MutableByteArrayInputStream.
	 * 
	 * @param returnObjectStreamResetCode boolean flag indicating if <code>TC_RESET</code> codes should be returned by this stream.
	 * 
	 * @see NoHeadersObjectInputStream
	 */
	public void setReturnObjectStreamResetCode(boolean returnObjectStreamResetCode)
	{
		this.returnObjectStreamResetCode = returnObjectStreamResetCode;
	}
	
	/**
	 * Gets the value of the flag indicating if this stream should return a <code>TC_RESET</code> code (see java.io.ObjectStreamConstants) 
	 * every time a new byte array is specified (see the <code>setByteArray</code> methods). If this flag is set to <code>true</code> 
	 * a <code>TC_RESET</code> code will be returned as the first element of data the first time any of <code>read</code> methods are called after 
	 * a new byte array has been set. <br>
	 * <br>
	 * The default value of this flag is <code>false</code>.
	 * This flag is provided to make it possible to force the reset of any NoHeadersObjectInputStream that is connected to this MutableByteArrayInputStream.
	 * 
	 * @return the value of the boolean flag indicating if <code>TC_RESET</code> codes should be returned by this stream.
	 * 
	 * @see NoHeadersObjectInputStream
	 */
	public boolean isReturningObjectStreamResetCode()
	{
		return returnObjectStreamResetCode;
	}	/**
	 * Reads the next byte of data from this input stream. The value byte is returned as an int in the range 0 to 255. 
	 * If the maximum number of bytes that can be read from the byte array have already been read, -1 is returned.<br>
	 * <br>
	 * This read method cannot block.
	 * 
	 * @return the next byte of data, or -1 if the end of the stream has been reached.
	 * 
	 * @throws IOException if no byte array is currently specified.
	 */
	public int read() throws IOException	{
		this.check();		
		if(returnObjectStreamResetCode && !objectStreamResetCodeReturned)		{			objectStreamResetCodeReturned = true;
			return ObjectStreamConstants.TC_RESET;		}
		else 		{
			return (this.currentByteArrayPos < this.byteDataMaxPos) ? (this.byteArray[this.currentByteArrayPos++] & 0xFF) : -1;		}	}
	
	/**
	 * Reads up to <code>len</code> bytes of data into an array of bytes from this input stream. If the maximum number of bytes that can be 
	 * read from the byte array have already been read, -1 is returned. Otherwise this stream attempts to read the smaller of <code>len</code> 
	 * and the number of bytes left to read in the byte array.<br>
	 * <br>
	 * This read method cannot block.
	 * 
	 * @return the total number of bytes read into the byte array, or -1 if there is no more data because the end of the stream has been reached.
	 * 
	 * @throws IOException if no byte array is currently specified.
	 */	public int read(byte b[], int off, int len) throws IOException	{
		this.check();		
		int readLength = len; /*readLength is used to indicate how many bytes that are to be read from the byte array.
																This variable differs from len only when the object stream reset code is returned.*/		
		if(b == null) throw new NullPointerException("Byte array b is null!");		else if(len <= 0) return 0;
		else if ((off < 0) || (off > b.length) || (len < 0) || ((off + len) > b.length) || ((off + len) < 0)) throw new IndexOutOfBoundsException();		else if(this.currentByteArrayPos >= this.byteDataMaxPos) return -1;
				
		if((this.currentByteArrayPos + readLength) > this.byteDataMaxPos) readLength = this.byteDataMaxPos - this.currentByteArrayPos;
						
		if(returnObjectStreamResetCode && !objectStreamResetCodeReturned)		{			objectStreamResetCodeReturned = true;
			b[off++] = ObjectStreamConstants.TC_RESET;
			readLength--;		}		
		System.arraycopy(this.byteArray, this.currentByteArrayPos, b, off, readLength);
		this.currentByteArrayPos += readLength;		
		return len;	}
	
	/**
	 * Skips <code>n</code> bytes of input from this input stream. Fewer bytes might be skipped if the end of the input stream is reached. The actual 
	 * number of bytes to be skipped is equal to the smaller of <code>n</code> and the number of bytes left to read in the byte array.<br>
	 * 
	 * @param n the number of bytes to be skipped.
	 * 
	 * @return the actual number of bytes skipped.
	 * 
	 * @throws IOException if no byte array is currently specified.
	 */
	public long skip(long n) throws IOException 	{
		this.check();		
		if(this.currentByteArrayPos + n > this.byteDataMaxPos) 		{
			n = this.byteDataMaxPos - this.currentByteArrayPos;
		}
		if(n < 0) 		{
			return 0;
		}
		this.currentByteArrayPos += n;
		return n;
    }
	
	/**
	 * Returns the number of bytes that can be read from this input stream without blocking. The value returned is 
	 * the remaining number of bytes that can be read from the byte array.
	 * 
	 * @return the number of bytes that can be read from the input stream without blocking.
	 * 
	 * @throws IOException if no byte array is currently specified.
	 */	public int available() throws IOException	{
		this.check();
		return this.byteDataMaxPos - this.currentByteArrayPos;	}	
	/**
	 * Tests if MutableByteArrayInputStream supports mark/reset.
	 * 
	 * @return <code>true</code> if this MutableByteArrayInputStream supports mark/reset otherwise <code>false</code>.
	 */	public boolean markSupported() 	{
		return true;
	}	
	/**
	 * Set the current marked position in the stream. MutableByteArrayInputStream objects are marked at position zero by default when 
	 * constructed. When this method is called the mark position is set to the current position in the byte array at which bytes are to be read from. 
	 * When resetting the stream (using the <code>reset</code> method), the current position is set to the mark position.
	 * 
	 * @param readAheadLimit not used.
	 * 
	 * @see #reset()
	 */	public void mark(int readAheadLimit) 	{
		try
		{			this.check();
			this.byteDataMarkOffset = this.currentByteArrayPos;		}		catch(IOException ioe){}
	}
	/**
	 * Resets the buffer to the marked position. MutableByteArrayInputStream objects are marked at position zero by default when 
	 * constructed, but may be marked at another position by calling the method <code>mark</code>.
	 * 
	 * @see #mark(int)
	 * 
	 * @throws IOException if no byte array is currently specified.
	 */
	public void reset() throws IOException	{
		this.check();
		this.currentByteArrayPos = this.byteDataMarkOffset;	}

	/**
	 * Closes this input stream and releases any system resources associated with the stream. 
	 */	public void close() throws IOException 	{
		this.byteArray = null;	 
	}
	/**
	 * Checks if a byte array has been specified.
	 * 
	 * @throws IOException if no byte array is currently specified.
	 */
	private void check() throws IOException	{		if(this.byteArray == null) throw new IOException("Byte array is not set!");	}}
