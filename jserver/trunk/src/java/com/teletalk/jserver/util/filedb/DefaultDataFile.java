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

/*
 TODO: Use file locks?
 TODO: Make it possible to reload file. 
 */
package com.teletalk.jserver.util.filedb;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.zip.CRC32;

import com.teletalk.jserver.JServerUtilities;
import com.teletalk.jserver.util.primitive.IntList;

/**
 * The DefaultDataFile is a direct implementation of the DataFile interface. DefaultDataFile uses file headers which among 
 * other things contain information about the size of the blocks, or to be precise - the amount of data that can be stored in 
 * each block. Since DefaultDataFile uses headers for each block in the file, the amount of data that can be stored in each 
 * block is of course less than the actual block size.<br>
 * <br>
 * The headers of each block contains information that for instance indicates if the block is occupied or not and which, if any, 
 * block is the next in the chain. Both the headers and that data contained in the blocks can have a checksum, which 
 * will be checked during the consistency check that is performed when an instance of this class is created. <br>
 * <br>
 * Note: All IOExceptions that are throws as a result of calling one of the methods of an instance of 
 * this class must be treated as fatal. Before the file can be used again it must be closed and 
 * reopened.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1
 */
public class DefaultDataFile implements DataFile, FileDBConstants
{
	//
	//		The headers of the data file contains following information:
	//
	//		* Short (2 bytes) for data file version
	//		* Integer (4 bytes) for allocation unit size
	//		* Boolean (1 byte) flag indicating if checksums are used for data in this data file
	//		* Long (8 bytes) for header checksum
	//		

	//		Each block consists of following parts:
	//
	//		Header:
	//			* Boolean (1 byte) flag indicating if the allocation unit is occupied or not
	//			* Boolean (1 byte) flag indicating if the allocation unit is the start block of a data structure
	//			* Integer (4 bytes) for data length
	//			* Integer (4 bytes) for pointer (allocation unit index)  to next allocation unit
	//			* Long (8 bytes) for header checksum
	//		
	//		Data block:
	//			* Length determined by constructor parameter blockSize (default 1k), minus the size of header and footer
	//
	//		Footer:
	//			* Long (8 bytes) for data block checksum
	//
	
	private static final int NULL_LINK = -1;
	
	/** The size of the block header. */
	public static final int BLOCK_HEADER_SIZE = 1+1+4+4+8;
	private static final int BLOCK_HEADER_OCCUPIED_FLAG_OFFSET = 0;
	private static final int BLOCK_HEADER_START_BLOCK_FLAG_OFFSET = 1;
	private static final int BLOCK_HEADER_DATA_LENGTH_OFFSET = 2;
	private static final int BLOCK_HEADER_NEXT_BLOCK_POINTER_OFFSET = 6;
	private static final int BLOCK_HEADER_CHECKSUM_OFFSET = 10;
	private static final int BLOCK_HEADER_CHECKSUM_SIZE = 8;
   	
	private static final int DATA_CHECKSUM_SIZE = 8;
	/** The size of the block footer. */
	public static final int BLOCK_FOOTER_SIZE = DATA_CHECKSUM_SIZE;
   
   /** @since 2.1.3 (20060403) */
   public static final int BLOCK_OVERHEAD = BLOCK_HEADER_SIZE + BLOCK_FOOTER_SIZE;
   
	
	private final String fullName;
	
	//Data file header information:
	/** The size of the file header. */
	public static final int DATA_FILE_HEADER_SIZE = 2+4+1+8;
	private final short version;
	private final int allocationUnitSize; //The size of the data stored in each block
	private final boolean useDataChecksum;

	private final int blockSize; //The size of a block (with headers)
	private final int initialCapacity;
	   
	private final BlockAllocator blockAllocator;
	
	private final IntList dataStartBlocks; // This list is always sorted
   private final ArrayList dataChainsArray; // Index matched against dataStartBlocks.
   private final IntList itemDataSizes; // Index matched against dataStartBlocks.
	
	private final BlockFile blockFile;
	private final String fileName;
	private final boolean readOnlyMode;
	
	private final CRC32 checksumCalculator = new CRC32();
	
	//Buffers for optimization
	private final byte[] deallocatedBlockHeaderBuffer;
	private byte[] formattedBlocksBuffer = null;
	private final static int maxBufferSize = 10;
	
	// For ensureSize
	private final int initialCapacityIncrementFactor;

	/**
	 * Creates a new DefaultDataFile that uses a block size of 1k (1024 bytes), no data checksums and an initial capacity of 100 blocks. 
	 * This constructor will create a DefaultDataFile that uses a default block file (by using {@link BlockFileFactory#getDefaultFactory()}) with a 
	 * {@link DataIOFile} object, and and a default block allocator (by using {@link BlockAllocatorFactory#getDefaultFactory()}).
	 *  
	 * @param fileName the full path of the file this DataFile will use.
	 * @param fileAccessMode the file access mode (see {@link FileDBConstants} for details).
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public DefaultDataFile(String fileName, String fileAccessMode) throws IOException
	{
		this(fileName, fileAccessMode, 1024, 100, false);
	}
	
	/**
	 * Creates a new DefaultDataFile that uses a default block file (by using {@link BlockFileFactory#getDefaultFactory()}) with a 
	 * {@link DataIOFile} object, and and a default block allocator (by using {@link BlockAllocatorFactory#getDefaultFactory()}).
	 *  
	 * @param fileName the full path of the file this DataFile will use.
	 * @param fileAccessMode the file access mode (see {@link FileDBConstants} for details).
	 * @param blockSize the size of each block in the file in bytes.
	 * @param initialCapacity the initial block capacity.
	 * @param useDataChecksum boolean flag indicating if checksums are to be used for data.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public DefaultDataFile(String fileName, String fileAccessMode, int blockSize, int initialCapacity, boolean useDataChecksum) throws IOException
	{
		this("FileDBDataFile", fileName, fileAccessMode, blockSize, initialCapacity, useDataChecksum);
	}
	
	/**
	 * Creates a new DefaultDataFile that uses a default block file (by using {@link BlockFileFactory#getDefaultFactory()}) with a 
	 * {@link DataIOFile} object, and and a default block allocator (by using {@link BlockAllocatorFactory#getDefaultFactory()}).
	 *  
	 * @param fullName the full name that will be give to this component (and used for logging).
	 * @param fileName the full path of the file this DataFile will use.
	 * @param fileAccessMode the file access mode (see {@link FileDBConstants} for details).
	 * @param blockSize the size of each block in the file in bytes.
	 * @param initialCapacity the initial block capacity.
	 * @param useDataChecksum boolean flag indicating if checksums are to be used for data.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public DefaultDataFile(String fullName, String fileName, String fileAccessMode, int blockSize, int initialCapacity, boolean useDataChecksum) throws IOException
	{
		this(fullName, blockSize, initialCapacity, useDataChecksum, new DataIOFile(fileName, fileAccessMode),  BlockFileFactory.getDefaultFactory(),  BlockAllocatorFactory.getDefaultFactory());
	}
	
	/**
	 * Creates a new DefaultDataFile that uses a block size of 1k (1024 bytes), no data checksums and an initial capacity of 100 blocks. 
	 *  
	 * @param fullName the full name that will be give to this component (and used for logging).
	 * @param dataIO the DataIO object to be used when creating a BlockFile object.
	 * @param blockFileFactory the BlockFileFactory object used to create a BlockFile object.
	 * @param blockAllocatorFactory the BlockAllocatorFactory object to create a BlockAllocator object.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public DefaultDataFile(String fullName, DataIO dataIO, BlockFileFactory blockFileFactory , BlockAllocatorFactory blockAllocatorFactory) throws IOException
	{
		this(fullName, 1024, 100, false, dataIO,  blockFileFactory,  blockAllocatorFactory);
	}
				
	/**
	 * Creates a new DefaultDataFile.
	 *  
	 * @param fullName the full name that will be give to this component (and used for logging).
	 * @param blockSize the size of each block in the file in bytes.
	 * @param initialCapacity the initial block capacity.
	 * @param useDataChecksum boolean flag indicating if checksums are to be used for data.
	 * @param dataIO the DataIO object to be used when creating a BlockFile object.
	 * @param blockFileFactory the BlockFileFactory object used to create a BlockFile object.
	 * @param blockAllocatorFactory the BlockAllocatorFactory object to create a BlockAllocator object.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public DefaultDataFile(String fullName, int blockSize, int initialCapacity, boolean useDataChecksum, DataIO dataIO, BlockFileFactory blockFileFactory , BlockAllocatorFactory blockAllocatorFactory) throws IOException
	{
		this.fullName = fullName;
		this.fileName = dataIO.getName();
		this.readOnlyMode = dataIO.isReadOnly();
		
		//Validate blockSize
		if(blockSize <= (BLOCK_HEADER_SIZE + BLOCK_FOOTER_SIZE)) blockSize = 1024;
		
		//Validate initialblocks
		if(initialCapacity < 0) initialCapacity = 100;
		this.initialCapacity = initialCapacity;
	
		boolean newFile = false;
		
      
		// READ / WRITE FILE HEADERS
		if( !dataIO.isNew() && (dataIO.length() >= DATA_FILE_HEADER_SIZE) ) //File exists
		{
			//Read header from blockfile
			dataIO.setFilePointer(0L);
			this.version = dataIO.readShort(); // Read version
			this.allocationUnitSize = dataIO.readInt(); // Read allocation unit size
			this.useDataChecksum = dataIO.readBoolean(); //Read boolean flag indicating if checksums will be used for data
			long checkSum = dataIO.readLong(); // Read file header checksum
			
			//Calculate the checksum for restored header
			byte[] headerBuf = dataFileHeaderFieldsToBytes();
			CRC32 crc32 = new CRC32();
			crc32.update(headerBuf);

			//Compare the calculated checksum for the headers with the one read from file
			if(crc32.getValue() != checkSum)
			{
				throw new IOException("Headers of " + fullName + " corrupted! Calculated checksum: " + crc32.getValue() + ", checksum read from file: " + checkSum + ".");
			}
			
			this.blockSize = (allocationUnitSize + BLOCK_HEADER_SIZE + BLOCK_FOOTER_SIZE); 
			
			newFile = false;
		}
		else //New file
		{
			//If read only mode and the file doesn't exist (or if it doesn't contain any file headers), 
			//throw an exception. 
			if(this.readOnlyMode)
			{
				if(!dataIO.isNew()) throw new IOException("Insufficient headers found in '" + this.fileName + "'!");
				else throw new FileNotFoundException("'" + this.fileName + "' not found!");
			}
			
			//Write headers
			this.version = 1;
			this.allocationUnitSize = blockSize - (BLOCK_HEADER_SIZE + BLOCK_FOOTER_SIZE);
			this.useDataChecksum = useDataChecksum;
			
			//Calculate the checksum for the header
			byte[] headerBuf = dataFileHeaderFieldsToBytes();
			CRC32 crc32 = new CRC32();
			crc32.update(headerBuf);
			
			//Write file header to blockfile
			dataIO.setFilePointer(0L);
			dataIO.writeShort(version); //Write version
			dataIO.writeInt(allocationUnitSize); //Write blocksize
			dataIO.writeBoolean(useDataChecksum); //Write boolean flag indicating if checksums will be used for data
			dataIO.writeLong(crc32.getValue()); //Write header checksum

			this.blockSize = blockSize;
					
			newFile = true;
		}
      
		
		// INITIALIZE THE BLOCKFILE
		this.blockFile = blockFileFactory.createBlockFile(dataIO, this.blockSize, DATA_FILE_HEADER_SIZE);
		
      
		// CREATE A BUFFER THAT REPRESENTS THE HEADER OF DELETED BLOCKS
		this.deallocatedBlockHeaderBuffer = new byte[BLOCK_HEADER_SIZE];
		formatBlockHeader(false, false, -1, NULL_LINK, this.deallocatedBlockHeaderBuffer, 0);

      
		// INITIALIZE FILE
		if(newFile)
		{
		   this.dataChainsArray = new ArrayList(this.initialCapacity);
			final int listSize = this.initialCapacity/5 + 10;
			this.dataStartBlocks = new IntList(listSize, listSize);
         this.itemDataSizes = new IntList(listSize, listSize);
						
			this.initNewFile();
			this.blockAllocator = blockAllocatorFactory.createBlockAllocator(this.initialCapacity, new int[0]);
		}
		else
		{
			int blockCapacity = this.blockFile.getBlockCapacity();
         if(!this.readOnlyMode) 
         {
   			if(blockCapacity < this.initialCapacity)
   			{
   				blockCapacity = this.initialCapacity;
   				this.blockFile.setBlockCapacity(blockCapacity);
   			}
         }

         this.dataChainsArray = new ArrayList(blockCapacity);
			final int listSize = blockCapacity/5 + 10;
			this.dataStartBlocks = new IntList(listSize, listSize);
         this.itemDataSizes = new IntList(listSize, listSize);
			
			int[] occupiedIndices = this.initExistingFile();
			
			if(!this.readOnlyMode) this.blockAllocator = blockAllocatorFactory.createBlockAllocator(blockCapacity, occupiedIndices);
			else this.blockAllocator = null;
		}
		
		// Calculate an appropriate initial capacity increment factor (a maximum of 1000 and a minimum of 1)
      if( this.initialCapacity < 10 ) this.initialCapacityIncrementFactor = 1;
      else this.initialCapacityIncrementFactor = Math.min((int)Math.ceil(this.initialCapacity/10), 1000);
	}
	
   
   
   /* ### INIT METHODS ### */
   
   
   
   /**
    * Initializes a new file.
    */
   private void initNewFile() throws IOException
   {
      this.blockFile.setBlockCapacity(this.initialCapacity); //Set the initial size of the blockfile

      for(int i=0; i<this.initialCapacity; i++)
      {
         this.blockFile.writePartialBlock(i, 0, this.deallocatedBlockHeaderBuffer, 0, this.deallocatedBlockHeaderBuffer.length);
      }
   }
   
   /**
    * Initializes an existing file.
    */
   private int[] initExistingFile() throws IOException
   {
      final int blockCapacity = this.blockFile.getBlockCapacity();
      if(blockCapacity == 0) return new int[0];
      
      long checksum;
      RandomAccessFile invalidDataFile = null;
      boolean invalidDataFileError = false;
      byte[] invalidData;
      boolean hasInvalidBlocks = false;
      final StringBuffer invalidBlocksBuffer = new StringBuffer();

      final IntList occupiedBlocks = new IntList(Math.max(blockCapacity/5, 100), Math.max(blockCapacity/5, 100));
      final IntList unreferencedBlocks = new IntList(Math.max(blockCapacity/5, 100), Math.max(blockCapacity/5, 100));
            
      int dataStartBlocksAddIndex;
      int[] initialDataChainsArray = new int[blockCapacity];
      Arrays.fill(initialDataChainsArray, NULL_LINK);
      final IntList initialDataChains = new IntList(initialDataChainsArray);

      
      // Read all block headers (read all data since it is more efficient)
      final byte[] blockHeaderBytes = new byte[BLOCK_HEADER_SIZE * blockCapacity];
            
      int bufferBlocks = blockCapacity / 10; 
      for(int i=1; ((bufferBlocks * this.blockSize) > (10*1024*1024)); i++) bufferBlocks = blockCapacity / (10 * i); // Limit buffer size to a maximum of 10 MB
      if( bufferBlocks < 1 ) bufferBlocks = 1;
      
      final byte[] buffer = new byte[bufferBlocks * this.blockSize]; // Buffer length is always evenly dividable by this.blockSize 
       
      int blockOffset = 0;
      int readSize;
      int readBlocks;

      this.blockFile.setFilePointer(this.blockFile.getBlockStartFP(0));
      while( blockOffset < blockCapacity )
      {
         readSize = Math.min(buffer.length, (blockCapacity - blockOffset)*this.blockSize);
         readBlocks = (readSize / this.blockSize);
         this.blockFile.read(buffer, 0, readSize);
         
         for(int i=0; i<readBlocks; i++)
         {
            System.arraycopy(buffer, i*this.blockSize, blockHeaderBytes, (blockOffset+i)*BLOCK_HEADER_SIZE, BLOCK_HEADER_SIZE);
         }
         
         blockOffset += readBlocks;
      }
      
      
      // Read and check block headers
      int offset = 0;
      for(int i=0; i<blockCapacity; i++)
      {
         offset = i * BLOCK_HEADER_SIZE;
            
         checksum = this.parseLongFromByteArray(blockHeaderBytes, BLOCK_HEADER_CHECKSUM_OFFSET + offset);
         CRC32 crc32 = new CRC32();
         crc32.update(blockHeaderBytes, offset, BLOCK_HEADER_CHECKSUM_OFFSET); // length of header - checksum is BLOCK_HEADER_CHECKSUM_OFFSET...

         if(checksum == crc32.getValue()) //Headers ok 
         {
            if(this.parseBooleanFromByteArray(blockHeaderBytes, BLOCK_HEADER_OCCUPIED_FLAG_OFFSET + offset)) //Read occupied flag
            {
               occupiedBlocks.addSorted(i); //Add to end of occupied blocks list
               
               if(this.parseBooleanFromByteArray(blockHeaderBytes, BLOCK_HEADER_START_BLOCK_FLAG_OFFSET + offset)) //Read block start index flag
               {
                  dataStartBlocksAddIndex = this.dataStartBlocks.addSorted(i);
                  // Register size of block data in itemDataSizes (index matched to dataStartBlocks)  
                  this.itemDataSizes.add(dataStartBlocksAddIndex, this.getBlockDataLength(blockHeaderBytes, offset));
                  this.dataChainsArray.add(dataStartBlocksAddIndex, null); // Add empty element - it will be filled later
               }
               else
               {
                  unreferencedBlocks.addSorted(i); //If not start block - add to list for unreferenced blocks...
               }
               
               //Set pointer to next block in the initialDataChains list
               initialDataChains.set(i, this.parseIntFromByteArray(blockHeaderBytes, BLOCK_HEADER_NEXT_BLOCK_POINTER_OFFSET+ offset));
            }
         }
         else if ( (!invalidDataFileError) && (!this.readOnlyMode) ) //Try to write invalid block to file for invalid blocks...
         {
            String logString = "{" + i + ": expected checksum: " + crc32.getValue() + ", read checksum: " + checksum + ", size: " + 
               this.parseIntFromByteArray(blockHeaderBytes, BLOCK_HEADER_DATA_LENGTH_OFFSET + offset) + "}";
            if(hasInvalidBlocks) invalidBlocksBuffer.append(", ");
            invalidBlocksBuffer.append(logString);
               
            hasInvalidBlocks = true;
                                    
            if(invalidDataFile == null)
            {
               try
               {
                  invalidDataFile = createInvalidDataFile();
               }
               catch(IOException ioe)
               {
                  JServerUtilities.logWarning(fullName, "Error creating file for invalid data (" + fileName +".invalid" + ")!", ioe);
                  invalidDataFileError = true;
                  continue;
               }
            }
                  
            invalidData = this.blockFile.readBlock(i); //Read block
            invalidDataFile.write(invalidData); //Write block to invalid data file
                  
            invalidData = null;
                  
            //Mark block as vacant:
            this.blockFile.writePartialBlock(i, 0, this.deallocatedBlockHeaderBuffer, 0, this.deallocatedBlockHeaderBuffer.length); 
         }
      }
      
      if(hasInvalidBlocks)
      {
         JServerUtilities.logWarning(fullName, "Invalid blocks detected while checking file consistency. The following blocks were invalid: " + invalidBlocksBuffer.toString() +".");
      }

      //Check data chains
      this.checkDataChains(initialDataChains, occupiedBlocks, unreferencedBlocks);

      if( !this.readOnlyMode ) //Only delete invalid/unreferenced blocks in read/write mode
      {
         //Delete unreferenced blocks
         int index;

         for(int i=0; i<unreferencedBlocks.size(); i++)
         {
            index = unreferencedBlocks.get(i);
            if( index >= 0 )
            {
               occupiedBlocks.removeValueSorted(index); //Remove from occupied blocks list
               this.blockFile.writePartialBlock(index, 0, this.deallocatedBlockHeaderBuffer, 0, this.deallocatedBlockHeaderBuffer.length); //Mark block as deleted in file
            }
         }
      }
      
      return occupiedBlocks.toArray(); 
   }
   
   /**
    * Checks the consistency of all data chains.
    */
   private void checkDataChains(final IntList initialDataChains, final IntList occupiedBlocks, final IntList unreferencedBlocks) throws IOException
   {
      final int[] initialDataStartBlocks = this.dataStartBlocks.toArray();
      int[] dataChain;
      int dataStartBlock;
      int dataStartBlockIndex;
               
      //Check the chains of data
      for(int i=0; i<initialDataStartBlocks.length; i++)
      {
         dataStartBlock = initialDataStartBlocks[i];

         if(dataStartBlock >= 0)
         {
            dataChain = this.getInitialItemDataChain(initialDataChains, dataStartBlock);

            dataStartBlockIndex = this.dataStartBlocks.binarySearch(dataStartBlock);
            
            if( !this.checkChain(dataStartBlockIndex, occupiedBlocks, dataChain) ) //Chain had invalid blocks
            {
               // Delete start block - the following blocks will be deleted later as they are still marked as unreferenced
               if( dataStartBlockIndex >= 0 )
               {
                  this.dataStartBlocks.remove(dataStartBlockIndex); //Remove from start blocks list
                  this.itemDataSizes.remove(dataStartBlockIndex); // ...and item data sizes
               }
               
               if( !this.readOnlyMode ) this.blockFile.writePartialBlock(dataStartBlock, 0, this.deallocatedBlockHeaderBuffer, 0, this.deallocatedBlockHeaderBuffer.length); //Mark block as deleted in file
            }
            else //Chain was ok 
            {
               // Store data chain
               this.dataChainsArray.set(dataStartBlockIndex, dataChain);
                              
               // Remove indices from unreferencedBlocks list
               for(int b=0; b<dataChain.length; b++)
               {
                  unreferencedBlocks.removeValueSorted(dataChain[b]);
               }
            }
         }
      }
   }
   
   /**
    * Gets a data chain and returns the chain in a new int array.
    */
   private final int[] getInitialItemDataChain(final IntList initialDataChains, final int chainStartBlock)
   {
      int numberOfBlocks = 0;
      int currentBlock = chainStartBlock;
      int nextBlock;
                    
      //Count blocks
      while( currentBlock != NULL_LINK )
      {
         if( currentBlock < initialDataChains.size() )
         {
            numberOfBlocks++;
            nextBlock = initialDataChains.get(currentBlock);
            if( currentBlock != nextBlock ) currentBlock = nextBlock;
            else currentBlock = NULL_LINK; // Circular link!
         }
         else currentBlock = NULL_LINK; // Invalid block number (shound only happen during init)
         
         if( numberOfBlocks > initialDataChains.size() ) // Sanity check - circular references detected!
         {
            return null;
         }
      }
      
      final int[] blockIndices = new int[numberOfBlocks];
      
      this.getInitialItemDataChain(initialDataChains, chainStartBlock, blockIndices, 0);
      
      return blockIndices;
   }
   
   /**
    * Gets a data chain and copies the chain into the specified data chain int array.
    */
   private final int getInitialItemDataChain(final IntList initialDataChains, final int chainStartBlock, final int[] dataChain, final int dataChainOffset)
   {
      int currentBlock = chainStartBlock;
      int blockCount = 0;
      
      for(int i=0; (currentBlock != NULL_LINK) && (i<dataChain.length); i++)
      {
         dataChain[dataChainOffset + blockCount] = currentBlock;
         currentBlock = initialDataChains.get(currentBlock);
         blockCount++;
      }
      
      return blockCount;
   }
   
   /**
    * Checks the consistency of a data chain.
    */
   private boolean checkChain(final int dataStartBlockIndex, final IntList occupiedBlocks, final int[] dataChain)
   {
      if( (dataStartBlockIndex < 0) || (dataChain == null) ) return false;
      
      int dataSize = this.itemDataSizes.get(dataStartBlockIndex);

      if( dataSize > (dataChain.length * this.allocationUnitSize) )
      {
         // Data size recorded start block is larger than the capacity of the data chain 
         return false;
      }
            
      for(int i=0; i<(dataChain.length); i++)
      {
         // Search for index in the occupiedBlocks list. If it isn't there it's an invalid block...
         if( occupiedBlocks.binarySearch(dataChain[i]) < 0 )
         {
            return false;
         }
      }
      return true;
   }
   
   /**
    * Creates a new RandomAccessFile used for storing invalid blocks.
    */
   private RandomAccessFile createInvalidDataFile() throws IOException
   {
      RandomAccessFile invalidDataFile = new RandomAccessFile(fileName +".invalid" + Long.toHexString(System.currentTimeMillis()), "rw");
      
      final byte[] headerBuf = dataFileHeaderFieldsToBytes();
      CRC32 crc32 = new CRC32();
      crc32.update(headerBuf);
      
      invalidDataFile.seek(0L);
      invalidDataFile.write(headerBuf); //Write header fields
      invalidDataFile.writeLong(crc32.getValue()); //Write header checksum
            
      return invalidDataFile;
   }
   
	/**
	 * Converts the data header fields to a byte array.
	 */
	private byte[] dataFileHeaderFieldsToBytes()
	{
		final byte[] headerBuf = new byte[DATA_FILE_HEADER_SIZE - 8]; //-8 for the checksum
		
		headerBuf[0] = (byte)((this.version >>> 8));
		headerBuf[1] = (byte)((this.version >>> 0));
		headerBuf[2] = (byte)((this.allocationUnitSize >>> 24));
		headerBuf[3] = (byte)((this.allocationUnitSize >>> 16));
		headerBuf[4] = (byte)((this.allocationUnitSize >>> 8));
		headerBuf[5] = (byte)((this.allocationUnitSize >>> 0));
		headerBuf[6] = (byte)((this.useDataChecksum ? 1 : 0));
		
		return headerBuf;
	}

   
   
   /* ### DATAFILE METHODS ### */
   
   
   
   /**
    * Flushes any buffered data to the underlying file used by this object..
    * 
    * @since 2.1.3 (20060330)
    */
   public void flush() throws IOException
   {
      this.blockFile.flush();
   }
   
   /**
    * Gets the time that the underlying file used by this object was last modified. 
    * 
    * @since 2.1.3 (20060330)
    */
   public long getLastModified()
   {
      return this.blockFile.getLastModified();
   }
   
   /**
    * Gets the time that a write was last performed on the block file through this object. 
    * 
    * @since 2.1.3 (20060330)
    */
   public long getLastWrite()
   {
      return this.blockFile.getLastWrite();
   }
   
   /**
    * Checks if the underlying file has been modified externally, i.e. through another interface or application.<br>
    * <br>
    * <b>Note:</b> Care should be exercised when using this method in write mode and when the storage is 
    * located on a different computer.
    * 
    * @since 2.1.3 (20060330)
    */
   public boolean isModifiedExternally()
   {
      return this.blockFile.isModifiedExternally();
   }
   
	/**
	 * Gets the BlockFile instance used by this DefaultDataFile.
	 * 
	 * @return a BlockFile object.
	 */
	public BlockFile getBlockFile()
	{
		return this.blockFile;
	}
	
	/**
	 * Gets the BlockAllocator instance used by this DefaultDataFile.
	 * 
	 * @return a BlockAllocator object.
	 */
	public BlockAllocator getBlockAllocator()
	{
		return this.blockAllocator;
	}
	
	/**
	 * Gets the indices of all blocks that are the first block of a certain 
	 * item that was inserted into the file.
	 * 
	 * @return an array of start block indices.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public int[] getDataStartBlocks() throws IOException
	{
		return this.dataStartBlocks.toArray();
	}

	/**
	 * Gets all the data stored in this DefaultDataFile as a byte matrix (an array of byte arrays, on for each item).
	 * 
	 * @return a byte matrix containing all all the data.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public byte[][] getAllData() throws IOException
	{
		return this.getMultipleItemData(this.getDataStartBlocks());
	}
   
   /**
    * Gets the size of the data of the item with the specified start block.
    * 
    * @return the data size.
    * 
    * @exception IOException if an I/O error occurs.
    * 
    * @since 2.1.3 (20060329)
    */
   public synchronized int getItemDataSize(final int dataStartBlock) throws IOException
   {
      final int dataStartBlocksIndex = this.validateDataStartBlock(dataStartBlock);
            
      return this.itemDataSizes.get(dataStartBlocksIndex);
   }
   
	/**
	 * Gets the data for the item with the specified start block.
	 * 
	 * @param dataStartBlock the start block of the item to get.
	 * 
	 * @return the data for the item with the specified start block.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public byte[] getItemData(final int dataStartBlock) throws IOException
	{
      final int dataStartBlocksIndex = this.validateDataStartBlock(dataStartBlock);
      
      final int itemDataSize = this.itemDataSizes.get(dataStartBlocksIndex);
      final int[] itemDataChain = this.getItemDataChain(dataStartBlocksIndex);
		byte[] blockData = null;

		if(itemDataChain.length == 1) blockData = this.blockFile.readBlock(itemDataChain[0]);
		else blockData = this.blockFile.readBlocks(itemDataChain);

		return this.extractItemData(blockData, itemDataSize);
	}
   
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
   public byte[] getPartialItemData(final int dataStartBlock, final int offset, final int length) throws IOException
   {
      if( length < 1) return new byte[0];
      else
      {
         final int dataStartBlocksIndex = this.validateDataStartBlock(dataStartBlock);
         
         final int itemDataSize = this.itemDataSizes.get(dataStartBlocksIndex);
         if( (offset + length) > itemDataSize ) throw new IOException("Index out of bounds - sum of specified offset (" + offset + ") and length (" + length + ") is larger than item data size(" + itemDataSize + ")!");
         
         final int numberOfBlocksToSkip = this.calculateBlocksNeeded( ((int)(offset / this.allocationUnitSize)) * this.allocationUnitSize);
         final int numberOfBlocksAffected = this.calculateBlocksNeeded( (offset % this.allocationUnitSize) + length);
         final int startBlockOffset = (offset % this.allocationUnitSize);
         
         final int[] itemDataChain = this.getPartialItemDataChain(dataStartBlocksIndex, numberOfBlocksToSkip, numberOfBlocksAffected);
         byte[] blockData;
         
         if(itemDataChain.length == 1) blockData = blockFile.readBlock(itemDataChain[0]);
         else blockData = blockFile.readBlocks(itemDataChain);
         
         // Extract data (remove headers and footers)
         blockData = this.extractItemData(blockData, itemDataChain.length * this.allocationUnitSize);
         
         byte[] partialItemData = new byte[length];
         System.arraycopy(blockData, startBlockOffset, partialItemData, 0, length);
         
         return partialItemData;
      }
   }
	
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
	public byte[][] getMultipleItemData(final int[] dataStartBlocks) throws IOException
	{
      if( dataStartBlocks == null ) throw new IOException("No dataStartBlocks specified!");

      final int[] dataStartBlockIndices = new int[dataStartBlocks.length];
      for(int i=0; i<dataStartBlocks.length; i++) dataStartBlockIndices[i] = this.validateDataStartBlock(dataStartBlocks[i]); 
      
      final int[] itemDataLengths = new int[dataStartBlocks.length];
      for (int i = 0; i<dataStartBlocks.length; i++) itemDataLengths[i] = this.itemDataSizes.get(dataStartBlockIndices[i]);
      
		final int[] itemDataChains = this.getItemDataChains(dataStartBlockIndices);

		final byte[] blockData = blockFile.readBlocks(itemDataChains);
	
		return this.extractMultipleItemData(blockData, dataStartBlocks.length, itemDataLengths);
	}
   
   /**
    * Inserts an item into this DefaultDataFile.
    * 
    * @param data the data of the item to insert.
    * 
    * @return the index of the first block that the item data was inserted to.
    * 
    * @exception IOException if an I/O error occurs.
    */
   public int insertItemData(final byte[] data) throws IOException
   {
      return this.insertItemDataInternal(data, data.length);
   }
   
   /**
    * Inserts a blank item (blank space) into this DefaultDataFile.
    * 
    * @param itemSize the number of bytes of blank space to insert.
    * 
    * @return the index of the first block that the item data was inserted to.
    * 
    * @exception IOException if an I/O error occurs.
    */
   public int insertBlankItemData(final int itemSize) throws IOException
   {
      return this.insertItemDataInternal(null, itemSize);
   }

	/**
	 * Inserts an item into this DefaultDataFile.
	 */
	private int insertItemDataInternal(byte[] data, final int dataSize) throws IOException
	{
      if( dataSize < 0 ) throw new IOException("Invalid dataSize (" + dataSize + ")!");
      if( this.readOnlyMode ) throw new IOException("Cannot insert data in read only mode!");
      
		int blocksNeeded = this.calculateBlocksNeeded(dataSize);
      if( blocksNeeded == 0 ) blocksNeeded = 1; // At least one block must be created

		final int[] allocatedBlocks = this.allocateDataChain(blocksNeeded);
		
      if( this.useDataChecksum && (data == null) ) data = new byte[dataSize]; // If data checksums are used, data must be written the normal way...
      
      if( data != null )
      {
   		final byte[] formattedBlockData = this.formatSingleItemDataBlocks(data, allocatedBlocks);
   
   		if(allocatedBlocks.length == 1)
         {
   			this.blockFile.writeBlock(allocatedBlocks[0], formattedBlockData, 0);
         }
   		else
         {
   			this.blockFile.writeBlocks(allocatedBlocks, formattedBlockData, 0);
         }
      }
      else // Blank space
      {
         // Write block headers only
         final byte[] blockHeader = new byte[BLOCK_HEADER_SIZE];

         for(int i=0; i<allocatedBlocks.length; i++)
         {
            this.formatBlockHeader(true, (i==0), (i==0) ? dataSize : -1, 
                  (i < (allocatedBlocks.length - 1)) ? allocatedBlocks[i+1] : NULL_LINK, 
                        blockHeader, 0);
            
            this.blockFile.writePartialBlock(allocatedBlocks[i], 0, blockHeader, 0, BLOCK_HEADER_SIZE);
         }
      }
      
      // Add block index to the start blocks list
      int dataStartBlocksAddIndex = this.dataStartBlocks.addSorted(allocatedBlocks[0]);
      
      // Store data chain
      this.dataChainsArray.add(dataStartBlocksAddIndex, allocatedBlocks);

      // Register size of block data in itemDataSizes (index matched to dataStartBlocks)  
      this.itemDataSizes.add(dataStartBlocksAddIndex, dataSize);
		
		return allocatedBlocks[0];
	}
   	
	/**
	 * Inserts multiple items into this DefaultDataFile.
	 * 
	 * @param data the data of the items to insert.
	 * 
	 * @return the indices of the first blocks that the items were inserted to.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public int[] insertMultipleItemData(final byte[][] data) throws IOException
	{
      if( this.readOnlyMode ) throw new IOException("Cannot insert data in read only mode!");
      
		final int[] blocksNeeded = new int[data.length];
		final int[] startBlocks = new int[data.length];
		int totalBlocksNeeded = 0;
		
		for(int i=0; i<data.length; i++)
		{
			blocksNeeded[i] = this.calculateBlocksNeeded(data[i].length);
         if( blocksNeeded[i] == 0 ) blocksNeeded[i] = 1; // At least one block
			totalBlocksNeeded += blocksNeeded[i];
		}
				
		//Allocate blocks
		final int[] allocatedBlocks = allocateDataChains(blocksNeeded, totalBlocksNeeded);
		
		final byte[] formattedBlockData = formatMultipleItemDataBlocks(data, blocksNeeded, allocatedBlocks);
		
		blockFile.writeBlocks(allocatedBlocks, formattedBlockData, 0);
		      
		//Find start blocks...
		int allocatedBlocksOffset = 0;
      int dataStartBlocksAddIndex;
      int[] currentItemBlocks;
		for(int i=0; i<blocksNeeded.length; i++)
		{
			startBlocks[i] = allocatedBlocks[allocatedBlocksOffset];
         
			// Add block index to the start blocks list
         dataStartBlocksAddIndex = this.dataStartBlocks.addSorted(startBlocks[i]);
         
         // Store data chain
         currentItemBlocks = new int[blocksNeeded[i]];
         System.arraycopy(allocatedBlocks, allocatedBlocksOffset, currentItemBlocks, 0, currentItemBlocks.length);
         this.dataChainsArray.add(dataStartBlocksAddIndex, currentItemBlocks);

			// Register size of block data in itemDataSizes (index matched to dataStartBlocks)  
         this.itemDataSizes.add(dataStartBlocksAddIndex, data[i].length);
         
			allocatedBlocksOffset += blocksNeeded[i];
		}
				
		return startBlocks;
	}
	
	/**
	 * Updates the data for a item in this DefaultDataFile.
	 * 
	 * @param dataStartBlock the start block of the item to update.
	 * @param data the new data for the item.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void updateItemData(final int dataStartBlock, final byte[] data) throws IOException
	{
      final int dataStartBlocksIndex = this.validateDataStartBlock(dataStartBlock);
      if( this.readOnlyMode ) throw new IOException("Cannot update data in read only mode!");
      
      if( dataStartBlocksIndex < 0 ) throw new IOException("Data start block " + dataStartBlock + " doesn't exist!");
         
      final int[] currentDataChain = this.getItemDataChain(dataStartBlocksIndex);
		int blocksNeeded = calculateBlocksNeeded(data.length);
      if( blocksNeeded == 0 ) blocksNeeded = 1; // At least one block
      
		//Less data
		if(blocksNeeded < currentDataChain.length)
		{
			final int[] updateDataBlockIndices = new int[blocksNeeded];
			System.arraycopy(currentDataChain, 0, updateDataBlockIndices, 0, blocksNeeded);

			//Write new data
			final byte[] formattedBlockData = formatSingleItemDataBlocks(data, updateDataBlockIndices);
			blockFile.writeBlocks(updateDataBlockIndices, formattedBlockData, 0);
			
			//Remove blocks not needed
			int[] deallocatedBlocks = this.deallocateDataChain(dataStartBlocksIndex, currentDataChain.length - blocksNeeded);

         // Rewrite block headers for deallocated blocks
         for(int i=0; i<deallocatedBlocks.length; i++)
         {
            this.blockFile.writePartialBlock(deallocatedBlocks[i], 0, this.deallocatedBlockHeaderBuffer, 0, this.deallocatedBlockHeaderBuffer.length);
         }         
		}
		//More data
		else if(blocksNeeded > currentDataChain.length)
		{
			//Allocate a new chain
			final int[] newBlockIndices = this.allocateDataChain(blocksNeeded - currentDataChain.length);
			
			//Construct a new array containing the indices
			final int[] newDataChain = new int[blocksNeeded];
			System.arraycopy(currentDataChain, 0, newDataChain, 0, currentDataChain.length);
			System.arraycopy(newBlockIndices, 0, newDataChain, currentDataChain.length, newBlockIndices.length);
			
         // Update data chain
         this.dataChainsArray.set(dataStartBlocksIndex, newDataChain);
         
			//Write new data
			final byte[] formattedBlockData = formatSingleItemDataBlocks(data, newDataChain);
			blockFile.writeBlocks(newDataChain, formattedBlockData, 0);
		}
		//Same amount of data
		else
		{
			final byte[] formattedBlockData = formatSingleItemDataBlocks(data, currentDataChain); //, 0, allocatedBlocks.length);
			blockFile.writeBlocks(currentDataChain, formattedBlockData, 0);
		}
      
      // Update saved size of data
      this.itemDataSizes.set(dataStartBlocksIndex, data.length);
	}
	
	/**
	 * Updates the data for a item in this DefaultDataFile partially.
	 * 
	 * @param dataStartBlock the start block of the item to update.
	 * @param itemDataOffset the offset in the existing data where update should begin.
	 * @param partialData the data to be used for the partial update.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void updatePartialItemData(final int dataStartBlock, final int itemDataOffset, final byte[] partialData) throws IOException
	{
      if( partialData.length > 0 )
      {
         final int dataStartBlocksIndex = this.validateDataStartBlock(dataStartBlock);
         if( this.readOnlyMode ) throw new IOException("Cannot update data in read only mode!");
         
         final int itemDataSize = this.itemDataSizes.get(dataStartBlocksIndex);
         if( (itemDataOffset + partialData.length) > itemDataSize ) throw new IOException("Index out of bounds - sum of specified offset (" + itemDataOffset + ") and data length (" + partialData.length + ") is larger than item data size(" + itemDataSize + ")!");
         
   		final int numberOfBlocksToSkip = this.calculateBlocksNeeded( ((int)(itemDataOffset / this.allocationUnitSize)) * this.allocationUnitSize); //-1;
   		final int numberOfBlocksAffected = this.calculateBlocksNeeded( (itemDataOffset % this.allocationUnitSize) + partialData.length );
   		final int startBlockOffset = (itemDataOffset % this.allocationUnitSize);
   
         final int[] itemDataChain = this.getPartialItemDataChain(dataStartBlocksIndex, numberOfBlocksToSkip, numberOfBlocksAffected);
   
         final int lastBlockSpace = this.allocationUnitSize - startBlockOffset;
         final int lastBlockUpdateLength = (partialData.length > lastBlockSpace) ? lastBlockSpace : partialData.length;
         
   		if( this.useDataChecksum ) //If data checksums are used the whole block needs updating...
   		{
   			final byte[] formattedPartialBlockData = this.blockFile.readBlocks(itemDataChain); //Read old blocks from file
   
   			int partialDataOffset = 0;
            int partialDataCopyLength;
   			int currentBlockDataSize;
   			int formattedPartialBlockDataOffset;
            			
   			//Format updated data
   			for(int i=0; i<itemDataChain.length; i++)
   			{
               currentBlockDataSize = ((partialData.length - partialDataOffset) > this.allocationUnitSize) ? this.allocationUnitSize : (partialData.length - partialDataOffset);
               
               if( i == 0 )
               {
                  partialDataCopyLength = lastBlockUpdateLength;
                  formattedPartialBlockDataOffset = BLOCK_HEADER_SIZE + startBlockOffset;
               }
               else
               {
                  partialDataCopyLength = currentBlockDataSize;
                  formattedPartialBlockDataOffset = BLOCK_HEADER_SIZE + (i * this.blockSize);
               }
   
   				System.arraycopy(partialData, partialDataOffset, formattedPartialBlockData, formattedPartialBlockDataOffset, partialDataCopyLength); //Copy partial data in to old data array
   				
               this.formatBlockDataChecksum(formattedPartialBlockData, i*this.blockSize, currentBlockDataSize);
   				
   				partialDataOffset += partialDataCopyLength;
   			}
   			
   			this.blockFile.writeBlocks(itemDataChain, formattedPartialBlockData, 0);
   		}
   		else //...otherwise, only update a portion of the block
   		{
   			int dataToWrite;
   			int partialDataOffset = 0;
   			
   			for(int i=0; i<itemDataChain.length; i++)
   			{
   				dataToWrite = (partialData.length - partialDataOffset) > this.allocationUnitSize ? this.allocationUnitSize : (partialData.length - partialDataOffset);
               				
   				if(i == 0) //If first block
   				{
                  if( dataToWrite > (this.allocationUnitSize - startBlockOffset) ) dataToWrite = this.allocationUnitSize - startBlockOffset; 
   					this.blockFile.writePartialBlock(itemDataChain[0], BLOCK_HEADER_SIZE + startBlockOffset, partialData, 0, dataToWrite);
   				}
   				else
   				{
   					this.blockFile.writePartialBlock(itemDataChain[i], BLOCK_HEADER_SIZE, partialData, partialDataOffset, dataToWrite);
   				}
   				
   				partialDataOffset += dataToWrite;
   			}
   		}
      }
	}
   
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
   public void appendItemData(final int dataStartBlock, final byte[] appendData) throws IOException
   {
      this.appendItemDataInternal(dataStartBlock, appendData, appendData.length);
   }
   
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
   public void appendBlankItemData(final int dataStartBlock, final int appendDataSize) throws IOException
   {
      if( appendDataSize < 0 ) throw new IOException("Invalid appendDataSize (" + appendDataSize + ")!");
      
      this.appendItemDataInternal(dataStartBlock, null, appendDataSize);
   }
   
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
   private void appendItemDataInternal(final int dataStartBlock, byte[] appendData, final int dataSize) throws IOException
   {
      if( dataSize > 0 )
      {
         final int dataStartBlocksIndex = this.validateDataStartBlock(dataStartBlock);
         if( this.readOnlyMode ) throw new IOException("Cannot update data in read only mode!");
         
         if( dataStartBlocksIndex < 0 ) throw new IOException("Data start block " + dataStartBlock + " doesn't exist!");
         
         final int itemDataSize = this.itemDataSizes.get(dataStartBlocksIndex);
         final int newItemDataSize = itemDataSize + dataSize;
         
         final int lastBlockOffset = (itemDataSize > 0) ? (itemDataSize % this.allocationUnitSize) : 0;
         
         final int[] currentDataChain = this.getItemDataChain(dataStartBlocksIndex);
         final int numberOfNewBlocksRequired = this.calculateBlocksNeeded(newItemDataSize) - currentDataChain.length;
         final int lastBlockNumber = currentDataChain[currentDataChain.length-1];
         int startBlockNextBlock = (currentDataChain.length > 1) ? currentDataChain[1] : NULL_LINK;
         
         int appendDataOffset = 0;
         
         if( this.useDataChecksum && (appendData == null) ) appendData = new byte[dataSize]; // If data checksums are used, data must be written the normal way...
         
         // Update last block
         if( (appendData != null) && (lastBlockOffset > 0) )
         {
            final int lastBlockSpace = this.allocationUnitSize - lastBlockOffset;
            final int lastBlockAppendLength = (appendData.length > lastBlockSpace) ? lastBlockSpace : appendData.length;
            final int formattedLastBlockDataOffset = BLOCK_HEADER_SIZE + lastBlockOffset;
            
            if( this.useDataChecksum ) //If data checksums are used the whole block needs updating...
            {
               final byte[] formattedLastBlockData = blockFile.readBlock(lastBlockNumber); //Read last block from file
               
               System.arraycopy(appendData, 0, formattedLastBlockData, formattedLastBlockDataOffset, lastBlockAppendLength); //Copy partial data in to old data array
               
               this.formatBlockDataChecksum(formattedLastBlockData, 0, lastBlockAppendLength);
               
               this.blockFile.writeBlock(lastBlockNumber, formattedLastBlockData, 0);
            }
            else //...otherwise, only update a portion of the block
            {
               this.blockFile.writePartialBlock(lastBlockNumber, formattedLastBlockDataOffset, appendData, 0, lastBlockAppendLength);
            }
            
            appendDataOffset += lastBlockAppendLength;
         }
         
         // Write new blocks
         if( numberOfNewBlocksRequired > 0 )
         {
            // Allocate a new chain
            final int[] appendDataChain = this.allocateDataChain(numberOfNewBlocksRequired);
            
            // Link the chains together and update data chain
            int[] newDataChain = new int[currentDataChain.length + appendDataChain.length];
            System.arraycopy(currentDataChain, 0, newDataChain, 0, currentDataChain.length);
            System.arraycopy(appendDataChain, 0, newDataChain, currentDataChain.length, appendDataChain.length);
            this.dataChainsArray.set(dataStartBlocksIndex, newDataChain);
            
            if( appendData != null )
            {
               // Format new blocks
               final byte[] formattedBlockData = this.getFormattedBlocksBuffer(appendDataChain.length);
               for(int i=0; i<appendDataChain.length; i++)
               {
                  formatBlock(formattedBlockData, i * this.blockSize, appendData, appendDataOffset, false, 
                                    (i < (appendDataChain.length - 1)) ? appendDataChain[i+1] : NULL_LINK);
                  
                  appendDataOffset += this.allocationUnitSize;
               }
               
               // Write new block data
               this.blockFile.writeBlocks(appendDataChain, formattedBlockData, 0);
            }
            else // Blank space
            {
               // Write block headers only
               final byte[] blockHeader = new byte[BLOCK_HEADER_SIZE];
   
               for(int i=0; i<appendDataChain.length; i++)
               {
                  this.formatBlockHeader(true, false, -1, 
                        (i < (appendDataChain.length - 1)) ? appendDataChain[i+1] : NULL_LINK, 
                              blockHeader, 0);
                  
                  this.blockFile.writePartialBlock(appendDataChain[i], 0, blockHeader, 0, BLOCK_HEADER_SIZE);
               }
            }
            
            
            // Update last block of original data to include link to first block of appended data (but only if the number of original 
            // data blocks are more than one - otherwise the link will be written when the data start block is updated below)
            if( currentDataChain.length > 1 )
            {
               final byte[] newLastBlockHeader = new byte[BLOCK_HEADER_SIZE];
               this.formatBlockHeader(true, false, -1, appendDataChain[0], newLastBlockHeader, 0); 
               this.blockFile.writePartialBlock(lastBlockNumber, 0, newLastBlockHeader, 0, BLOCK_HEADER_SIZE);
            }
            else // currentDataChain.length == 1 
            {
               // If there only existed one block prior to the append - set the next block field of the data start block header to 
               // the first block number of the appended data. The data start block header will be rewritten below.
               startBlockNextBlock = appendDataChain[0];
            }
         }
               
         // Update first block with new size of data
         final byte[] newStartBlockHeader = new byte[BLOCK_HEADER_SIZE];
         this.formatBlockHeader(true, true, newItemDataSize, startBlockNextBlock, 
               newStartBlockHeader, 0); 
         this.blockFile.writePartialBlock(currentDataChain[0], 0, newStartBlockHeader, 0, BLOCK_HEADER_SIZE); 
         
         // Update saved size of data
         this.itemDataSizes.set(dataStartBlocksIndex, newItemDataSize);
      }
   }
   
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
   public void deletePartialItemData(final int dataStartBlock, final int removeSize) throws IOException
   {
      final int dataStartBlocksIndex = this.validateDataStartBlock(dataStartBlock);
      if( this.readOnlyMode ) throw new IOException("Cannot update data in read only mode!");
      
      if( dataStartBlocksIndex < 0 ) throw new IOException("Data start block " + dataStartBlock + " doesn't exist!");
      
      int[] currentDataChain = this.getItemDataChain(dataStartBlocksIndex);
      final int itemDataSize = this.itemDataSizes.get(dataStartBlocksIndex);
      final int newItemDataSize = itemDataSize - removeSize;

      if( newItemDataSize < 0 ) throw new IOException("Remove size (" + removeSize + ") is larger than item size (" + itemDataSize + ")!");
      
      int newNumberOfBlocks = currentDataChain.length;
      if( currentDataChain.length > 1 )
      {
         int numberOfBlocksToRemove = 0;
         numberOfBlocksToRemove = currentDataChain.length - this.calculateBlocksNeeded(newItemDataSize);
         if( numberOfBlocksToRemove == currentDataChain.length ) numberOfBlocksToRemove = currentDataChain.length - 1; // Always leave one block...

         if( numberOfBlocksToRemove > 0 )
         {
            newNumberOfBlocks = currentDataChain.length - numberOfBlocksToRemove;
            
            // Update new next block link of the new last block (but only if the number of  data blocks are more 
            // than one - otherwise the link will be written when the data start block is updated below)
            if( newNumberOfBlocks > 1 )
            {
               final byte[] newLastBlockHeader = new byte[BLOCK_HEADER_SIZE];
               this.formatBlockHeader(true, false, -1, NULL_LINK, newLastBlockHeader, 0); 
               this.blockFile.writePartialBlock(currentDataChain[newNumberOfBlocks-1], 0, newLastBlockHeader, 0, BLOCK_HEADER_SIZE);
            }
            
            // Deallocate
            int[] deallocatedBlocks = this.deallocateDataChain(dataStartBlocksIndex, numberOfBlocksToRemove);
            
            // Rewrite block headers for deallocated blocks
            for(int i=0; i<deallocatedBlocks.length; i++)
            {
               this.blockFile.writePartialBlock(deallocatedBlocks[i], 0, this.deallocatedBlockHeaderBuffer, 0, this.deallocatedBlockHeaderBuffer.length);
            }
         }
      }
            
      int startBlockNextBlock = (newNumberOfBlocks > 1) ? currentDataChain[1] : NULL_LINK;
      
      // Update first block with new size of data
      final byte[] newStartBlockHeader = new byte[BLOCK_HEADER_SIZE];
      this.formatBlockHeader(true, true, newItemDataSize, startBlockNextBlock, 
            newStartBlockHeader, 0); 
      this.blockFile.writePartialBlock(currentDataChain[0], 0, newStartBlockHeader, 0, BLOCK_HEADER_SIZE); 
      
      // Update saved size of data
      this.itemDataSizes.set(dataStartBlocksIndex, newItemDataSize);
   }
	
	/**
	 * Deletes an item from this DefaultDataFile.
	 * 
	 * @param dataStartBlock the start block of the item to delete.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void deleteItemData(final int dataStartBlock) throws IOException
	{
      final int dataStartBlockIndex = this.validateDataStartBlock(dataStartBlock);
      if( this.readOnlyMode ) throw new IOException("Cannot delete data in read only mode!");
      
      // Deallocate
      final int[] itemDataChain = this.deallocateItemDataChain(dataStartBlockIndex);

      // Rewrite block headers for deallocated blocks
		for(int i=0; i<itemDataChain.length; i++)
		{
			this.blockFile.writePartialBlock(itemDataChain[i], 0, this.deallocatedBlockHeaderBuffer, 0, this.deallocatedBlockHeaderBuffer.length);
		}
		
      if( dataStartBlockIndex >= 0 )
      {
         this.dataStartBlocks.remove(dataStartBlockIndex); // Remove from start blocks list
         this.itemDataSizes.remove(dataStartBlockIndex); // ...and item data sizes
         this.dataChainsArray.remove(dataStartBlockIndex);
      }
	}

	/**
	 * Deletes all data in this DefaultDataFile.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void clearAllBlocks() throws IOException
	{
      if( this.readOnlyMode ) throw new IOException("Cannot clear data in read only mode!");
      
		this.blockAllocator.deallocateAllBlocks(); //Deallocate all blocks in blockAllocator
		this.blockAllocator.setSize(this.initialCapacity);
      
		this.dataChainsArray.clear();      
		this.dataStartBlocks.clear(); // Clear data start blocks list
      this.itemDataSizes.clear();  // Clear item data sizes list
		
		this.initNewFile();
	}
	
	/**
	 * Closes this DefaultDataFile.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public final void close() throws IOException
	{
		this.blockFile.close();
	}
   

   
   /* ### DATA CHAIN RELATED UTILITY METHODS ### */
      

   
	/**
	 * Gets multiple data chains.
	 */
   private final int[] getItemDataChains(final int[] chainStartBlockIndices)
	{
      IntList allDataChains = new IntList();
      int[] currentDataChain;
      for(int i=0; i<chainStartBlockIndices.length; i++)
      {
         currentDataChain = getItemDataChain(chainStartBlockIndices[i]);
         allDataChains.addAll(currentDataChain);
      }
      return allDataChains.toArray();
	}
	
	/**
	 * Gets a partial data chain.
	 */
   private final int[] getPartialItemDataChain(final int chainStartBlockIndex, int numberOfBlocksToSkip, final int numberOfBlocksToInclude)
	{
      final int[] dataChain = ((int[])this.dataChainsArray.get(chainStartBlockIndex));
      
      if( (numberOfBlocksToSkip == 0) && (numberOfBlocksToInclude >= dataChain.length) )
      {
         return this.getItemDataChain(chainStartBlockIndex);
      }
      else
      {
         IntList partialDataChain = new IntList();
         
         for(int i=0; i<numberOfBlocksToInclude; i++)
         {
            if( (numberOfBlocksToSkip + i) < dataChain.length ) partialDataChain.add( dataChain[numberOfBlocksToSkip + i] );
            else break;
         }
         
         return partialDataChain.toArray();
      }
	}
	
	/**
	 * Gets a data chain and returns the chain in a new int array.
	 */
   private final int[] getItemDataChain(final int chainStartBlockIndex)
	{
      return ((int[])this.dataChainsArray.get(chainStartBlockIndex));
	}

	/**
	 * Allocates multiple new data chains.
	 */
	private final int[] allocateDataChains(final int[] blocksNeeded, final int totalBlocksNeeded) throws IOException
	{
		this.ensureCapacity(this.blockAllocator.getNumberOfAllocatedBlocks() + totalBlocksNeeded); //Ensure capacity
		
		final int[] allocatedBlocks = blockAllocator.allocateBlocks(totalBlocksNeeded);
		if(allocatedBlocks.length != totalBlocksNeeded) throw new IOException("Fatal error! BlockAllocator failed to allocate the required number of blocks! TotalBlocksNeeded: " + totalBlocksNeeded + 
																																				 ", allocated blocks: " + allocatedBlocks.length + ", total allocated blocks: " + this.blockAllocator.getNumberOfAllocatedBlocks() + 
																																				 ", total number of blocks in BlockAllocator: " + this.blockAllocator.getNumberOfBlocks() + "."); // This should never happen...
		
		return allocatedBlocks;
	}
	
	/**
	 * Allocates a new data chain.
	 */
	private final int[] allocateDataChain(final int blocksNeeded) throws IOException
	{
		this.ensureCapacity(this.blockAllocator.getNumberOfAllocatedBlocks() + blocksNeeded); //Ensure capacity
		
		final int[] allocatedBlocks = this.blockAllocator.allocateBlocks(blocksNeeded);
		if(allocatedBlocks.length != blocksNeeded) throw new IOException("Fatal error! BlockAllocator failed to allocate the required number of blocks! BlocksNeeded: " + blocksNeeded + 
																																				 ", allocated blocks: " + allocatedBlocks.length + ", total allocated blocks: " + this.blockAllocator.getNumberOfAllocatedBlocks() + 
																																				 ", total number of blocks in BlockAllocator: " + this.blockAllocator.getNumberOfBlocks() + "."); // This should never happen...
		return allocatedBlocks;
	}
	
	/**
	 * Deallocates an item data chain.
	 */
   private final int[] deallocateItemDataChain(final int chainStartBlockIndex)
	{
      final int[] dataChain = ((int[])this.dataChainsArray.get(chainStartBlockIndex));
      
      // Set data chain to null
      this.dataChainsArray.set(chainStartBlockIndex, null);
      
      // Deallocate
      this.blockAllocator.deallocateBlocks(dataChain);
      
      return dataChain;
	}
	
	/**
	 * Deallocates a data chain.
	 */
   private final int[] deallocateDataChain(final int chainStartBlockIndex, final int numberOfBlocksToRemove)
	{
      final int[] dataChain = ((int[])this.dataChainsArray.get(chainStartBlockIndex));
      
      if( numberOfBlocksToRemove >= dataChain.length )
      {
         return this.deallocateItemDataChain(chainStartBlockIndex);
      }
      else
      {
         final int[] blockIndices = new int[numberOfBlocksToRemove];
         
         int beginIndex = dataChain.length - numberOfBlocksToRemove;
         for(int i=0; i<numberOfBlocksToRemove; i++)
         {
            blockIndices[i] = dataChain[beginIndex + i];
         }
         
         // Resize data chain
         int[] newDataChain = new int[dataChain.length - numberOfBlocksToRemove];
         System.arraycopy(dataChain, 0, newDataChain, 0, newDataChain.length);
         this.dataChainsArray.set(chainStartBlockIndex, newDataChain);
         
         // Deallocate
         this.blockAllocator.deallocateBlocks(blockIndices);
         
         return blockIndices;
      }
	}
	
   
   
   /* ### BLOCK EXTRACTION UTILITY METHODS ### */
   
	
	
	/**
	 * Extracts multiple items (removes headers and footers) from the specified data.
	 */
   private byte[][] extractMultipleItemData(final byte[] blockData, final int numberOfItems, final int[] dataLengths)
	{
		final byte[][] data = new byte[numberOfItems][];
		
		final int[] blockDataOffset = new int[]{0};

		for(int i=0; i<numberOfItems; i++)
		{
			data[i] = this.extractItemData(blockData, blockDataOffset, dataLengths[i]);
		}

		return data;
	}
   
   /**
    * Extracts an item (removes headers and footers) from the specified data.
    */
   private byte[] extractItemData(final byte[] blockData, final int dataLength)
   {
      return this.extractItemData(blockData, new int[]{0}, dataLength);
   }
	
	/**
	 * Extracts an item (removes header and footer) from the specified data.
	 * 
	 * Note: blockDataOffset must be array to make it an in/out parameter. Only element 0 is used.
	 */
	private byte[] extractItemData(final byte[] blockData, final int[] blockDataOffset, final int dataLength)
	{
		final byte[] data = new byte[dataLength];
		int dataOffset = 0;
		int blockDataLength;
		
		//Extract data
		while(dataOffset < dataLength)
		{
			blockDataLength = (dataLength - dataOffset) > this.allocationUnitSize ? this.allocationUnitSize : (dataLength - dataOffset);

         // Copy block data (skip block headers) 
			System.arraycopy(blockData, blockDataOffset[0] + BLOCK_HEADER_SIZE, data, dataOffset, blockDataLength);

         dataOffset += blockDataLength;
			blockDataOffset[0] += this.blockSize;
		}

		return data;
	}
   
   /**
    * Gets the data length field from a block data header.
    */
   public int getBlockDataLength(final byte[] blockData, final int offset)
   {
      return this.parseIntFromByteArray(blockData, BLOCK_HEADER_DATA_LENGTH_OFFSET + offset);
   }
	
   
   
   /* ### BLOCK FORMATTING UTILITY METHODS ### */
   
   
   
	/**
	 * Gets the buffer used for formatting blocks.
	 */
	private byte[] getFormattedBlocksBuffer(final int numberOfBlocks)
	{
		byte[] buf;
		
		if( (this.formattedBlocksBuffer == null) || ( numberOfBlocks > (this.formattedBlocksBuffer.length / this.blockSize) ) )
		{
			buf = new byte[numberOfBlocks * this.blockSize];
			
			if(numberOfBlocks <= DefaultDataFile.maxBufferSize) this.formattedBlocksBuffer = buf;
		}
		else
		{
			buf = this.formattedBlocksBuffer;
		}
		
		return buf;
	}
	
	/**
	 * Formats blocks for a single item (adds headers and footers).
	 */
	private byte[] formatSingleItemDataBlocks(final byte[] originalData, final int[] allocatedBlocks) 
	{
		final byte[] formattedBlockData = this.getFormattedBlocksBuffer(allocatedBlocks.length);

		for(int i=0; i<allocatedBlocks.length; i++)
		{
			formatBlock(formattedBlockData, i * this.blockSize, originalData, i * this.allocationUnitSize, (i == 0) ? true : false, 
									(i < (allocatedBlocks.length - 1)) ? allocatedBlocks[i+1] : NULL_LINK);
		}

		return formattedBlockData;
	}
		
	/**
	 * Formats blocks for multiple items (adds headers and footers).
	 */
	private byte[] formatMultipleItemDataBlocks(final byte[][] originalData, final int[] blocksNeeded, final int[] allocatedBlocks)
	{
		final byte[] formattedBlockData = new byte[allocatedBlocks.length * this.blockSize];
		int allocatedBlocksOffset = 0;
		
		for(int originalDataIndex=0; originalDataIndex<originalData.length; originalDataIndex++)
		{
			for(int i=0; i<blocksNeeded[originalDataIndex]; i++)
			{
				formatBlock(formattedBlockData, ((i * this.blockSize) + (allocatedBlocksOffset * this.blockSize)), originalData[originalDataIndex], (i * this.allocationUnitSize), (i == 0) ? true: false, 
										(i < (blocksNeeded[originalDataIndex] - 1)) ? allocatedBlocks[allocatedBlocksOffset + i +1] : NULL_LINK);
			}
			
			allocatedBlocksOffset += blocksNeeded[originalDataIndex];
		}
		
		return formattedBlockData;
	}
		
	/**
	 * Formats a single block (adds header and footer).
	 */
	private void formatBlock(final byte[] formattedBlockData, final int formattedBlockDataOffset, final byte[] originalData, final int originalDataOffset, final boolean firstBlockInChain, final int nextBlock)
	{
		//Format header
		if( firstBlockInChain ) this.formatBlockHeader(true, firstBlockInChain, originalData.length, nextBlock, formattedBlockData, formattedBlockDataOffset);
      else this.formatBlockHeader(true, firstBlockInChain, -1, nextBlock, formattedBlockData, formattedBlockDataOffset);
		
		//Format allocation unit data and footer
		final int allocationUnitDataLength = ((originalData.length - originalDataOffset) >= this.allocationUnitSize) ? this.allocationUnitSize : (originalData.length - originalDataOffset);

		//Copy data into block
		System.arraycopy(originalData, originalDataOffset, formattedBlockData, formattedBlockDataOffset + BLOCK_HEADER_SIZE, allocationUnitDataLength);
			
		//Format footer
		if(this.useDataChecksum) //If checksums are to be used for data...
		{
			this.formatBlockDataChecksum(formattedBlockData, formattedBlockDataOffset, allocationUnitDataLength);
		}
	}
	
	/**
	 * Adds a data checksum to a block.
	 */
	private void formatBlockDataChecksum(final byte[] formattedBlockData, final int formattedBlockDataOffset, final int allocationUnitDataLength)
	{
		checksumCalculator.reset();
		checksumCalculator.update(formattedBlockData, formattedBlockDataOffset + BLOCK_HEADER_SIZE, allocationUnitDataLength);
				
		final long dataChecksum = checksumCalculator.getValue();
		final int checkSumOffset = formattedBlockDataOffset + (this.blockSize - DATA_CHECKSUM_SIZE);
		
		formattedBlockData[checkSumOffset + 0] = (byte)((dataChecksum >>> 56));
		formattedBlockData[checkSumOffset + 1] = (byte)((dataChecksum >>> 48));
		formattedBlockData[checkSumOffset + 2] = (byte)((dataChecksum >>> 40));
		formattedBlockData[checkSumOffset + 3] = (byte)((dataChecksum >>> 32));
		formattedBlockData[checkSumOffset + 4] = (byte)((dataChecksum >>> 24));
		formattedBlockData[checkSumOffset + 5] = (byte)((dataChecksum >>> 16));
		formattedBlockData[checkSumOffset + 6] = (byte)((dataChecksum >>>  8));
		formattedBlockData[checkSumOffset + 7] = (byte)((dataChecksum >>>  0));
	}
	
	/**
	 * Adds a header to a block.
	 */
	private void formatBlockHeader(final boolean occupied, final boolean startBlock, final int dataLength, final int nextBlockPointer, final byte[] buffer, final int bufferOffset)
	{
		buffer[bufferOffset + 0] = (byte)((occupied ? 1 : 0));
		buffer[bufferOffset + 1] = (byte)((startBlock ? 1 : 0));
		buffer[bufferOffset + 2] = (byte)((dataLength >>> 24));
		buffer[bufferOffset + 3] = (byte)((dataLength >>> 16));
		buffer[bufferOffset + 4] = (byte)((dataLength >>>  8));
		buffer[bufferOffset + 5] = (byte)((dataLength >>>  0));
		buffer[bufferOffset + 6] = (byte)((nextBlockPointer >>> 24));
		buffer[bufferOffset + 7] = (byte)((nextBlockPointer >>> 16));
		buffer[bufferOffset + 8] = (byte)((nextBlockPointer >>> 8));
		buffer[bufferOffset + 9] = (byte)((nextBlockPointer >>> 0));
		
		checksumCalculator.reset();
		checksumCalculator.update(buffer, bufferOffset, BLOCK_HEADER_SIZE - BLOCK_HEADER_CHECKSUM_SIZE);
		final long checksum = checksumCalculator.getValue();
		
		buffer[bufferOffset + 10] = (byte)((checksum >>> 56));
		buffer[bufferOffset + 11] = (byte)((checksum >>> 48));
		buffer[bufferOffset + 12] = (byte)((checksum >>> 40));
		buffer[bufferOffset + 13] = (byte)((checksum >>> 32));
		buffer[bufferOffset + 14] = (byte)((checksum >>> 24));
		buffer[bufferOffset + 15] = (byte)((checksum >>> 16));
		buffer[bufferOffset + 16] = (byte)((checksum >>>  8));
		buffer[bufferOffset + 17] = (byte)((checksum >>>  0));
	}
	
	/**
	 * Calculates the capacity increment factor used when growing a file.
	 */
	private int getCapacityIncrementFactor()
	{
      return Math.max( this.initialCapacityIncrementFactor, (this.blockAllocator.getNumberOfBlocks()/10) );      
	}
	
	/**
	 * Ensures that the file can hold the specified capacity.
	 */
	private void ensureCapacity(final int numberOfRequiredBlocks) throws IOException
	{
		if(this.readOnlyMode) throw new RuntimeException("Cannot grow file in read only mode!");
		
      final int currentNoOfBlocks = this.blockAllocator.getNumberOfBlocks();

		if(numberOfRequiredBlocks > currentNoOfBlocks)
		{
			int noOfBlocks = Math.max(numberOfRequiredBlocks, currentNoOfBlocks + this.getCapacityIncrementFactor() );

			this.blockAllocator.setSize(noOfBlocks);
			
			this.blockFile.setBlockCapacity(noOfBlocks);
			
			//Write empty blockheader only for the blocks 
			if(noOfBlocks > numberOfRequiredBlocks)
			{
				for(int i=numberOfRequiredBlocks; i<noOfBlocks; i++)
				{
					this.blockFile.writePartialBlock(i, 0, this.deallocatedBlockHeaderBuffer, 0, this.deallocatedBlockHeaderBuffer.length);
				}
			}
		}
		else if( (numberOfRequiredBlocks*2) < (currentNoOfBlocks) ) // While we're at it, check if there is too much unused space...
		{
			// To be fully effective, the skrinking should include a total reallocation of all the data, which how ever may be quite a costly maneuver...
			int minRequiredSize = Math.max(this.initialCapacity, Math.min(currentNoOfBlocks, numberOfRequiredBlocks + this.getCapacityIncrementFactor() ) );
						
			if(minRequiredSize < currentNoOfBlocks) // Shrink! But not lower than initialCapacity
			{
				int shrinkToSize = Math.max(minRequiredSize, this.blockAllocator.getSpaceInUse());
				
				if(shrinkToSize < currentNoOfBlocks) // If it is possible to shrink, i.e. there is unused space at the end of the allocation table/array
				{
					this.blockAllocator.setSize(shrinkToSize);
			
					this.blockFile.setBlockCapacity(shrinkToSize);
				}
			}
		}
	}
   
   
   
   /* ### MISC UTILITY METHODS ### */
   
   
   
   /**
    * Validates the existency of a data start block.
    */
   private int validateDataStartBlock(final int dataStartBlock) throws IOException
   {
      if( dataStartBlock < 0 ) throw new IOException("Invalid dataStartBlock (" + dataStartBlock + ")!");
      
      final int dataStartBlocksIndex = this.dataStartBlocks.binarySearch(dataStartBlock);
      if( dataStartBlocksIndex < 0 ) throw new IOException("Data start block " + dataStartBlock + " doesn't exist!");
      
      return dataStartBlocksIndex;
   }
   
   
   /**
    * Calculates the blocks needed to hold the specified data length.
    */
   private final int calculateBlocksNeeded(final int dataLength)
   {
      return (int)Math.ceil((double)dataLength / (double)this.allocationUnitSize);
   }
   
   /**
    * Parses a long from a byte array.
    */
   private long parseLongFromByteArray(final byte[] byteArray, final int byteArrayOffset)
   {
      long result = 0;
      long tmp;
      
      for(int i=0; i<8; i++)
      {
         tmp = (byteArray[byteArrayOffset + i] + 256) & 0xFF;
         result += (tmp << 8*(7-i));
      }
      
      return result;
   }
   
   /**
    * Parses an int from a byte array.
    */
   private int parseIntFromByteArray(final byte[] byteArray, final int byteArrayOffset)
   {
      int result = 0;
      int tmp;
      
      for(int i=0; i<4; i++)
      {
         tmp = (byteArray[byteArrayOffset + i] + 256) & 0xFF;
         result += (tmp << 8*(3-i));
      }
      
      return result;
   }
   
   /**
    * Parses a boolen from a byte array.
    */
   private boolean parseBooleanFromByteArray(final byte[] byteArray, final int byteArrayOffset)
   {
      return byteArray[byteArrayOffset] != 0;
   }
}
