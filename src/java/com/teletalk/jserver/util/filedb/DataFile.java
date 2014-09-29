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
 * A DataFile combines the functionality of a <code>BlockFile</code> and a <code>BlockAllocator</code> 
 * with the added support for writing data that spans over several blocks.<br>
 * <br>
 * The DataFile is responsible for determining which block size should be used. This can for instance be accomplished by 
 * maintaining a file header which contains this information. DataFile is also responsible for checking the consistency of 
 * an already existing file.
 *  
 * @see BlockFile
 * @see BlockAllocator
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1
 */
public interface DataFile
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
    * Gets the time that a write was last performed through this object. 
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
	 * Gets the BlockFile instance used by this DataFile.
	 * 
	 * @return a BlockFile object.
	 */
	public BlockFile getBlockFile();
	
	/**
	 * Gets the BlockAllocator instance used by this DataFile.
	 * 
	 * @return a BlockAllocator object.
	 */
	public BlockAllocator getBlockAllocator();
		
	/**
	 * Gets the indices of all blocks that are the first block of a certain 
	 * item that was inserted into the file.
	 * 
	 * @return an array of start block indices.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public int[] getDataStartBlocks() throws IOException;
	
	/**
	 * Gets all the data stored in this DataFile as a byte matrix (an array of byte arrays, on for each item).
	 * 
	 * @return a byte matrix containing all all the data.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public byte[][] getAllData() throws IOException;
	
   /**
    * Gets the size of the data of the item with the specified start block.
    * 
    * @return the data size.
    * 
    * @exception IOException if an I/O error occurs.
    * 
    * @since 2.1.3 (20060329)
    */
   public int getItemDataSize(int dataStartBlock) throws IOException;
   
   /**
    * Gets a part of the data of the item with the specified start block.
    * 
    * @param dataStartBlock the start block of the item to get.
    * @param offset the offset in the data.
    * @param length the number of bytes to read.
    * 
    * @return the data for the item with the specified start block.
    * 
    * @exception IOException if an I/O error occurs.
    * 
    * @since 2.1.3 (20060329)
    */
   public byte[] getPartialItemData(final int dataStartBlock, final int offset, final int length) throws IOException;
   
	/**
	 * Gets the data for the item with the specified start block.
	 * 
	 * @param dataStartBlock the start block of the item to get.
	 * 
	 * @return the data for the item with the specified start block.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public byte[] getItemData(int dataStartBlock) throws IOException;
	
	/**
	 * Gets the data for the items with the specified start blocks. The 
	 * data will be returned as a byte matrix (an array of byte arrays, on for each item).
	 * 
	 * @param dataStartBlocks the start blocks of the items to get.
	 * 
	 * @return the data for the items with the specified start blocks.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public byte[][] getMultipleItemData(int[] dataStartBlocks) throws IOException;
	
	/**
	 * Inserts an item into this DataFile.
	 * 
	 * @param data the data of the item to insert.
	 * 
	 * @return the index of the first block that the item data was inserted to.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public int insertItemData(byte[] data) throws IOException;
   
   /**
    * Inserts a blank item (blank space) into this DefaultDataFile.
    * 
    * @param itemSize the number of bytes of blank space to insert.
    * 
    * @return the index of the first block that the item data was inserted to.
    * 
    * @exception IOException if an I/O error occurs.
    */
   public int insertBlankItemData(final int itemSize) throws IOException;
	
	/**
	 * Inserts multiple items into this DataFile.
	 * 
	 * @param data the data of the items to insert.
	 * 
	 * @return the indices of the first blocks that the items were inserted to.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public int[] insertMultipleItemData(byte[][] data) throws IOException;
	
	/**
	 * Updates the data for a item in this DataFile.
	 * 
	 * @param dataStartBlock the start block of the item to update.
	 * @param data the new data for the item.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void updateItemData(int dataStartBlock, byte[] data) throws IOException;
	
	/**
	 * Updates the data for a item in this DataFile partially.
	 * 
	 * @param dataStartBlock the start block of the item to update.
	 * @param itemDataOffset the offset in the existing data where update should begin.
	 * @param partialData the data to be used for the partial update.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void updatePartialItemData(int dataStartBlock, int itemDataOffset, byte[] partialData) throws IOException;
	
   /**
    * Appends data to the data of the item with the specified start block. 
    * 
    * @param dataStartBlock the start block of the item to update.
    * @param appendData the data to be appended.
    * 
    * @exception IOException if an I/O error occurs.
    * 
    *  @since 2.1.3 (20060329)
    */
   public void appendItemData(int dataStartBlock, byte[] appendData) throws IOException;
   
   /**
    * Appends blank data to the item with the specified start block.
    * 
    * @param dataStartBlock the start block of the item to update.
    * @param appendDataSize the number of bytes of blank space to insert.
    * 
    * @exception IOException if an I/O error occurs.
    * 
    *  @since 2.1.3 (20060329)
    */
   public void appendBlankItemData(int dataStartBlock, int appendDataSize) throws IOException;
   
   /**
    * Removes data at the end of an item in this DefaultDataFile.
    * 
    * @param dataStartBlock the start block of the item to remove data in.
    * @param removeSize the number of bytes of data to remove.
    * 
    * @exception IOException if an I/O error occurs.
    * 
    * @since 2.1.3 (20060403)
    */
   public void deletePartialItemData(int dataStartBlock, int removeSize) throws IOException;
   
	/**
	 * Deletes an item from this DataFile.
	 * 
	 * @param dataStartBlock the start block of the item to delete.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void deleteItemData(int dataStartBlock) throws IOException;
	
	/**
	 * Deletes all data in this DataFile.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void clearAllBlocks() throws IOException;
	
	/**
	 * Closes this DataFile.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void close() throws IOException;
}
