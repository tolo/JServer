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
package com.teletalk.jserver.util.filedb;

import java.io.IOException;

/**
 * A BlockFile is capable of reading and writing data in blocks to a file or other types 
 * of data storage. All blocks have the same size and that size should be determined when creating
 * the BlockFile object and then never changed after that.<br>
 * <br>
 * BlockFile uses an instance of the inerface <code>DataIO</code> to access the data storage 
 * that is to be used.
 * 
 * @see DataIO
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1
 */
public interface BlockFile
{
   /**
    * Flushes any buffered data to the underlying file used by this object..
    * 
    * @since 2.1.3 (20060330)
    */
   public void flush() throws IOException;
   
   /**
    * Gets the time that the underlying file used by this object was last modified. 
    * 
    * @since 2.1.3 (20060330)
    */
   public long getLastModified();
   
   /**
    * Gets the time that a write was last performed on the block file through this object. 
    * 
    * @since 2.1.3 (20060330)
    */
   public long getLastWrite();   
   
   /**
    * Checks if the underlying file has been modified externally, i.e. through another interface or application.<br>
    * <br>
    * <b>Note:</b> Care should be exercised when using this method in write mode and when the storage is 
    * located on a different computer.
    * 
    * @since 2.1.3 (20060330)
    */
   public boolean isModifiedExternally();   
   
	/**
	 * Gets the DataOI object associated with this BlockFile.
	 * 
	 * @return the DataOI object associated with this BlockFile.
	 */
	public DataIO getDataIO();

	/**
	 * Checks if this BlockFile is in read only mode.
	 * 
	 * @return <code>true</code> if this BlockFile is in read only mode, otherwise <code>false</code>.
	 */
	public boolean isReadOnly();
	
	/**
	 * Gets the file header size, i.e. the number of bytes before the first block in this BlockFile.
	 * 
	 * @return the file header size.
	 */
	public int getFileHeaderSize();
	
	/**
	 * Gets the size of the blocks in this BlockFile.
	 * 
	 * @return the size of the blocks in this BlockFile.
	 */
	public int getBlockSize();
	
	/**
	 * Gets the number of blocks that this BlockFile currently has room for.
	 * 
	 * @return the number of blocks that this BlockFile currently has room for.
	 */
	public int getBlockCapacity();
	
	/**
	 * Sets the number of blocks that this BlockFile should have room for.
	 * 
	 * @param blockCapacity the number of blocks that this BlockFile should have room for.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void setBlockCapacity(final int blockCapacity) throws IOException;
	
	/**
	 * Gets the current index of the file pointer.
	 * 
	 * @return the current index of the file pointer.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public long getFilePointer() throws IOException;
	
	/**
	 * Sets the current index of the file pointer.
	 * 
	 * @param fp the new index of the file pointer.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void setFilePointer(long fp) throws IOException;
		
	/**
	 * Gets the index of the first byte of the specified block number.
	 * 
	 * @param blockNumber an index of a block.
	 * 
	 * @return the index of the first byte of the specified block number.
	 */
	public long getBlockStartFP(final int blockNumber);
	
	/**
	 * Reads the data in the block specied by parameter <code>blockNumber</code>.
	 * 
	 * @param blockNumber the index of the block to read.
	 * 
	 * @return the data in the block. The size of the returned byte array will be the current block size.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public byte[] readBlock(int blockNumber) throws IOException;
	
	/**
	 * Reads the data in the blocks specied by parameter <code>blockNumbers</code>.
	 * 
	 * @param blockNumbers the indices of the blocks to read.
	 * 
	 * @return the data in the blocks. The size of the returned byte array will be the current block size * the length of the <code>blockNumbers</code> array.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public byte[] readBlocks(int[] blockNumbers) throws IOException;
	
	/**
	 * Reads only a part of the block specified by parameter <code>blockNumber</code>.
	 * 
	 * @param blockNumber the index of the block to read.
	 * @param blockOffset the offset from the beginning of the block.
	 * @param partialBlockData the byte array into which data will be read.
	 * @param partialBlockDataOffset the offset in the partialBlockData array where the read data is to be copied.
	 * @param partialBlockDataLength the number of bytes of data to read.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void readPartialBlock(int blockNumber, int blockOffset, byte[] partialBlockData, int partialBlockDataOffset, int partialBlockDataLength) throws IOException;
   
   /**
    * Reads only a part of the blocks specified by parameter <code>blockNumbers</code>.
    * 
    * @param blockNumbers the indices of the blocks to read.
    * @param blockOffset the offset from the beginning of the block.
    * @param partialBlockData the byte array into which data will be read.
    * @param partialBlockDataOffset the offset in the partialBlockData array where the read data is to be copied.
    * @param partialBlockDataLength the number of bytes of data to read in each block (i.e. NOT the total length of the data).
    * 
    * @exception IOException if an I/O error occurs.
    */
   public void readPartialBlocks(int[] blockNumbers, int blockOffset, byte[] partialBlockData, int partialBlockDataOffset, int partialBlockDataLength) throws IOException;
		
	/**
	 * Writes a block of data to this BlockFile. The maximum number of bytes that will be written from the 
	 * specified array will be equal to the block size, or less if the specified array doesn't contain that many 
	 * bytes.
	 * 
	 * @param blockNumber the index of the block to write to.
	 * @param blockData the data to be written.
	 * @param blockDataOffset the start offset in the blockData.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void writeBlock(int blockNumber, byte[] blockData, int blockDataOffset) throws IOException;

	/**
	 * Writes multiple blocks of data to this BlockFile. The maximum number of bytes that will be written from the 
	 * specified array will be equal to the block size times the length of the specified block numbers array, or less if the specified array 
	 * doesn't contain that many bytes.
	 * 
	 * @param blockNumbers the indices of the blocks to write to.
	 * @param blockData the data to be written.
	 * @param blockDataOffset the start offset in the blockData.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void writeBlocks(int[] blockNumbers, byte[] blockData, int blockDataOffset) throws IOException;
	
	/**
	 * Writes data to only a part of the block specified by parameter <code>blockNumber</code>.
	 * 
	 * @param blockNumber the index of the block to write to.
	 * @param blockOffset the offset from the beginning of the block where writing will start.
	 * @param partialBlockData the data to be written.
	 * @param partialBlockDataOffset the offset in the partialBlockData array from which to get data to be written.
	 * @param partialBlockDataLength the number of bytes of data to write.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void writePartialBlock(int blockNumber, int blockOffset, byte[] partialBlockData, int partialBlockDataOffset, int partialBlockDataLength) throws IOException;
	
	/**
	 * Reads data from the current file position.
	 * 
	 * @param buf the buffer to read data into.
	 * @param bufOffset the start offset in buf.
	 * @param length the amount of data to read.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void read(final byte[] buf, final int bufOffset, final int length) throws IOException;
	
	/**
	 * Writes data to the current file position.
	 * 
	 * @param buf the buffer to write data from.
	 * @param bufOffset the start offset in buf.
	 * @param length the amount of data to write.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void write(final byte[] buf, final int bufOffset, final int length) throws IOException;
	
	/**
	 * Closes this BlockFile.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void close() throws IOException;
}
