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
 TODO: Set lock object. 
 */
package com.teletalk.jserver.util.filedb;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.UTFDataFormatException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import com.teletalk.jserver.JServerUtilities;
import com.teletalk.jserver.util.MutableByteArrayInputStream;
import com.teletalk.jserver.util.NoHeadersObjectInputStream;
import com.teletalk.jserver.util.NoHeadersObjectOutputStream;
import com.teletalk.jserver.util.primitive.IntList;

/**
 * This class implements a low level file database, in which it is possible to store data associated with keys. 
 * LowLevelFileDB uses two different files for storing necessary information - one for storing the data and another 
 * for storing the keys and what index in the data file they map to.<br>
 * <br>
 * The data and index files are handled by instances of the class <code>DefaultDataFile</code> by default.<br>
 * <br>
 * Note: All IOExceptions that are throws as a result of calling one of the methods of an instance of 
 * this class must be treated as fatal. Before the database can be used again it must be closed and 
 * reopened.
 * 
 * @see DefaultDataFile
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.02
 */
public final class LowLevelFileDB implements FileDBConstants
{
	/**
	 * Comparator used for sorting keys according to timestamp.
	 */
	private static final Comparator indexFileIndexComparator = new Comparator()
	{
		/**
		 * Compares two objects for order according to priority. Returns a negative integer, zero, or a 
		 * positive integer as the first argument is less than, equal to, or greater than the second.
		 * 
		 * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
		 */
		public int compare(final Object o1, final Object o2)	
		{
			return (int) ( ( (FileDBIndexInformation) ((Map.Entry)o1).getValue() ).getIndexFileIndex() - ( (FileDBIndexInformation) ((Map.Entry)o2).getValue() ).getIndexFileIndex() );
		}

		/**
		 * Checks if this object is equal to that specified by parameter obj.
		 */
		public boolean equals(Object obj)
		{
			return obj == this;
		}
	};
	
	/**
	 * Comparator used for sorting keys according to timestamp.
	 */
	private static final Comparator timeStampComparator = new Comparator()
	{
		/**
		 * Compares two objects for order according to priority. Returns a negative integer, zero, or a 
		 * positive integer as the first argument is less than, equal to, or greater than the second.
		 * 
		 * @return a negative integer, zero, or a positive integer as the first argument is less than, equal to, or greater than the second.
		 */
		public int compare(final Object o1, final Object o2)	
		{
			return (int) ( ( (FileDBIndexInformation) ((Map.Entry)o1).getValue() ).getTimeStamp() - ( (FileDBIndexInformation) ((Map.Entry)o2).getValue() ).getTimeStamp() );
		}

		/**
		 * Checks if this object is equal to that specified by parameter obj.
		 */
		public boolean equals(Object obj)
		{
			return obj == this;
		}
	};
   
   private static final Object SERIALIZATION_ERROR = new Object();
   
	
	private boolean closed = false;
   
   private final boolean readOnlyMode;
	
	private final String fullName;
	private boolean fullNameContainsFileNameBase = false;
	
	private final String fileNameBase;
	
	private final HashMap wireKeysToIndices;
	
	private final DataFile indexFile;
	private final DataFile dataFile;
	
	private boolean debugMode = false;
	
	//private final static int maxAttempts = 3; //Maximum number of attempts that should be made to carry out a certain operation (create index/data file)
	//private final static int attemptDelay = 1000; //Attempt delay in milliseconds
	
	private boolean useAlternativeObjectOutputStreamResetMethod = false;
	
	//Item serialization
	private NoHeadersObjectOutputStream objectOutput; 
	private final ByteArrayOutputStream byteOutput;  
	
	//Item deserialization
	private final MutableByteArrayInputStream byteInput;
	private NoHeadersObjectInputStream objectInput;
	
	
	/**
	 * Creates a new LowLevelFileDB. <br>
	 * <br>
	 *  The following default values will be set: <br>
	 * <br>
	 *  &middot; Data file block size: 1k.<br>
	 *  &middot; Initial data file block count: 100.<br>
	 *  &middot; Index file block size: 128 bytes (of which 128 - the block header size bytes are reserved for key/index information). <br>
	 *  &middot; Intial index file block count: 100<br>
	 * <br>
	 * The index file will use CRC32 checksums but the datafile will not.
	 * 
	 * @param fileNameBase the base name (path) used when creating the data and index files.
	 * 
	 * @exception  IOException if an error occurred while creating files.
	 */
	public LowLevelFileDB(String fileNameBase) throws IOException
	{
		this(fileNameBase, 1024, 100, 100, true, false, READ_WRITE_MODE);
	}
	
	/**
	 * Creates a new LowLevelFileDB. <br>
	 * The index file block size will be defaulted to 128 (of which 128 - the block header size bytes are reserved for key/index information).
	 * The index file will use CRC32 checksums but the datafile will not.
	 * 
	 * @param fileNameBase the base name (path) used when creating the data and index files.
	 * @param dataBlockSize the size in bytes of the blocks in the data file.
	 * @param initialDataBlocks the initial number of blocks in the data file.
	 * @param initialIndexBlocks the initial number of blocks in the index file.
	 * 
	 * @exception  IOException if an error occurred while creating files.
	 */
	public LowLevelFileDB(String fileNameBase, int dataBlockSize, int initialDataBlocks, int initialIndexBlocks) throws IOException
	{
		this(fileNameBase, dataBlockSize, initialDataBlocks, initialIndexBlocks, true, false, READ_WRITE_MODE);
	}
	
	/**
	 * Creates a new LowLevelFileDB. <br>
	 * The index file block size will be defaulted to 128 (of which 128 - the block header size bytes are reserved for key/index information).
	 * The index file will use CRC32 checksums but the datafile will not.
	 * 
	 * @param fileNameBase the base name (path) used when creating the data and index files.
	 * @param dataBlockSize the size in bytes of the blocks in the data file.
	 * @param initialDataBlocks the initial number of blocks in the data file.
	 * @param initialIndexBlocks the initial number of blocks in the index file.
	 * 
	 * @exception  IOException if an error occurred while creating files.
	 */
	public LowLevelFileDB(String fileNameBase, int dataBlockSize, int initialDataBlocks, int initialIndexBlocks, String fileAccessMode) throws IOException
	{
		this(fileNameBase, dataBlockSize, initialDataBlocks, initialIndexBlocks, true, false, fileAccessMode);
	}
	
	/**
	 * Creates a new LowLevelFileDB.<br>
	 * The index file block size will be defaulted to 128 (of which 128 - the block header size bytes are reserved for key/index information).
	 * 
	 * @param fileNameBase the base name (path) used when creating the data and index files.
	 * @param dataBlockSize the size in bytes of the blocks in the data file.
	 * @param initialDataBlocks the initial number of blocks in the data file.
	 * @param initialIndexBlocks the initial number of blocks in the index file.
	 * @param useIndexChecksum boolean flag indicating if checksums are to be used for information in the index file.
	 * @param useDataChecksum boolean flag indicating if checksums are to be used for information in the data file.
	 * 
	 * @exception  IOException if an error occurred while creating files.
	 */
	public LowLevelFileDB(String fileNameBase, int dataBlockSize, int initialDataBlocks, int initialIndexBlocks, boolean useIndexChecksum, boolean useDataChecksum) throws IOException
	{
		this(fileNameBase, dataBlockSize, initialDataBlocks, initialIndexBlocks, useIndexChecksum, useDataChecksum, READ_WRITE_MODE);
	}
	
	/**
	 * Creates a new LowLevelFileDB.<br>
	 * The index file block size will be defaulted to 128 (of which 128 - the block header size bytes are reserved for key/index information).
	 * 
	 * @param fileNameBase the base name (path) used when creating the data and index files.
	 * @param dataBlockSize the size in bytes of the blocks in the data file.
	 * @param initialDataBlocks the initial number of blocks in the data file.
	 * @param initialIndexBlocks the initial number of blocks in the index file.
	 * @param useIndexChecksum boolean flag indicating if checksums are to be used for information in the index file.
	 * @param useDataChecksum boolean flag indicating if checksums are to be used for information in the data file.
	 * 
	 * @exception  IOException if an error occurred while creating files.
	 */
	public LowLevelFileDB(String fileNameBase, int dataBlockSize, int initialDataBlocks, int initialIndexBlocks, boolean useIndexChecksum, boolean useDataChecksum, String fileAccessMode) throws IOException
	{
		this("LowLevelFileDB(" + fileNameBase + ")", fileNameBase, dataBlockSize, initialDataBlocks, initialIndexBlocks, useIndexChecksum, useDataChecksum, fileAccessMode);
		this.fullNameContainsFileNameBase = true;
	}
	
	/**
	 * Creates a new LowLevelFileDB. <br>
	 * <br>
	 *  The following default values will be set: <br>
	 * <br>
	 *  &middot; Data file block size: 1k.<br>
	 *  &middot; Initial data file block count: 100.<br>
	 *  &middot; Index file block size: 128 bytes (of which 128 - the block header size bytes are reserved for key/index information). <br>
	 *  &middot; Intial index file block count: 100<br>
	 * <br>
	 * The index file will use CRC32 checksums but the datafile will not.
	 * 
	 * @param fullName the full name of this LowLevelFileDB object (used for logging).
	 * @param fileNameBase the base name (path) used when creating the data and index files.
	 * 
	 * @exception  IOException if an error occurred while creating files.
	 */
	public LowLevelFileDB(String fullName, String fileNameBase) throws IOException
	{
		this(fullName, fileNameBase, 1024, 100, 100, true, false, READ_WRITE_MODE);
	}
   
   /**
    * Creates a new LowLevelFileDB. <br>
    * <br>
    *  The following default values will be set: <br>
    * <br>
    *  &middot; Data file block size: 1k.<br>
    *  &middot; Initial data file block count: 100.<br>
    *  &middot; Index file block size: 128 bytes (of which 128 - the block header size bytes are reserved for key/index information). <br>
    *  &middot; Intial index file block count: 100<br>
    * <br>
    * The index file will use CRC32 checksums but the datafile will not.
    * 
    * @param fullName the full name of this LowLevelFileDB object (used for logging).
    * @param fileNameBase the base name (path) used when creating the data and index files.
    * 
    * @exception  IOException if an error occurred while creating files.
    */
   public LowLevelFileDB(String fullName, String fileNameBase, String fileAccessMode) throws IOException
   {
      this(fullName, fileNameBase, 1024, 100, 100, true, false, fileAccessMode);
   }
	
	/**
	 * Creates a new LowLevelFileDB. <br>
	 * The index file block size will be defaulted to 128 (of which 128 - the block header size bytes are reserved for key/index information).
	 * The index file will use CRC32 checksums but the datafile will not.
	 * 
	 * @param fullName the full name of this LowLevelFileDB object (used for logging).
	 * @param fileNameBase the base name (path) used when creating the data and index files.
	 * @param dataBlockSize the size in bytes of the blocks in the data file.
	 * @param initialDataBlocks the initial number of blocks in the data file.
	 * @param initialIndexBlocks the initial number of blocks in the index file.
	 * 
	 * @exception  IOException if an error occurred while creating files.
	 */
	public LowLevelFileDB(String fullName, String fileNameBase, int dataBlockSize, int initialDataBlocks, int initialIndexBlocks) throws IOException
	{
		this(fullName, fileNameBase, dataBlockSize, initialDataBlocks, initialIndexBlocks, true, false, READ_WRITE_MODE);
	}
	
	/**
	 * Creates a new LowLevelFileDB. <br>
	 * The index file block size will be defaulted to 128 (of which 128 - the block header size bytes are reserved for key/index information).
	 * The index file will use CRC32 checksums but the datafile will not.
	 * 
	 * @param fullName the full name of this LowLevelFileDB object (used for logging).
	 * @param fileNameBase the base name (path) used when creating the data and index files.
	 * @param dataBlockSize the size in bytes of the blocks in the data file.
	 * @param initialDataBlocks the initial number of blocks in the data file.
	 * @param initialIndexBlocks the initial number of blocks in the index file.
	 * 
	 * @exception  IOException if an error occurred while creating files.
	 */
	public LowLevelFileDB(String fullName, String fileNameBase, int dataBlockSize, int initialDataBlocks, int initialIndexBlocks, String fileAccessMode) throws IOException
	{
		this(fullName, fileNameBase, dataBlockSize, initialDataBlocks, initialIndexBlocks, true, false, fileAccessMode);
	}
	
	/**
	 * Creates a new LowLevelFileDB.<br>
	 * The index file block size will be defaulted to 128 (of which 128 - the block header size bytes are reserved for key/index information).
	 * 
	 * @param fullName the full name of this LowLevelFileDB object (used for logging).
	 * @param fileNameBase the base name (path) used when creating the data and index files.
	 * @param dataBlockSize the size in bytes of the blocks in the data file.
	 * @param initialDataBlocks the initial number of blocks in the data file.
	 * @param initialIndexBlocks the initial number of blocks in the index file.
	 * @param useIndexChecksum boolean flag indicating if checksums are to be used for information in the index file.
	 * @param useDataChecksum boolean flag indicating if checksums are to be used for information in the data file.
	 * 
	 * @exception  IOException if an error occurred while creating files.
	 */
	public LowLevelFileDB(String fullName, String fileNameBase, int dataBlockSize, int initialDataBlocks, int initialIndexBlocks, boolean useIndexChecksum, boolean useDataChecksum) throws IOException
	{
		this(fullName, fileNameBase, dataBlockSize, initialDataBlocks, 128, initialIndexBlocks, useIndexChecksum, useDataChecksum, READ_WRITE_MODE);
	}
	
	/**
	 * Creates a new LowLevelFileDB.<br>
	 * The index file block size will be defaulted to 128 (of which 112 bytes are reserved for the key).
	 * 
	 * @param fullName the full name of this LowLevelFileDB object (used for logging).
	 * @param fileNameBase the base name (path) used when creating the data and index files.
	 * @param dataBlockSize the size in bytes of the blocks in the data file.
	 * @param initialDataBlocks the initial number of blocks in the data file.
	 * @param initialIndexBlocks the initial number of blocks in the index file.
	 * @param useIndexChecksum boolean flag indicating if checksums are to be used for information in the index file.
	 * @param useDataChecksum boolean flag indicating if checksums are to be used for information in the data file.
	 * 
	 * @exception  IOException if an error occurred while creating files.
	 */
	public LowLevelFileDB(String fullName, String fileNameBase, int dataBlockSize, int initialDataBlocks, int initialIndexBlocks, boolean useIndexChecksum, boolean useDataChecksum, String fileAccessMode) throws IOException
	{
		this(fullName, fileNameBase, dataBlockSize, initialDataBlocks, 128, initialIndexBlocks, useIndexChecksum, useDataChecksum, fileAccessMode);
	}
	
	/**
	 * Creates a new LowLevelFileDB.
	 * 
	 * @param fullName the full name of this LowLevelFileDB object (used for logging).
	 * @param fileNameBase the base name (path) used when creating the data and index files.
	 * @param dataBlockSize the siz in bytese of the blocks in the data file.
	 * @param initialDataBlocks the initial number of blocks in the data file.
	 * @param indexBlockSize the size in bytes of the blocks in the index file.
	 * @param initialIndexBlocks the initial number of blocks in the index file.
	 * @param useIndexChecksum boolean flag indicating if checksums are to be used for information in the index file.
	 * @param useDataChecksum boolean flag indicating if checksums are to be used for information in the data file.
	 * 
	 * @exception  IOException if an error occurred while creating files.
	 */
	public LowLevelFileDB(String fullName, String fileNameBase, int dataBlockSize, int initialDataBlocks, int indexBlockSize, int initialIndexBlocks, boolean useIndexChecksum, boolean useDataChecksum, String fileAccessMode) throws IOException
	{
		this(fullName, fileNameBase, 
				new DefaultDataFile(fullName + ".IndexFile", fileNameBase + ".idx", fileAccessMode, indexBlockSize, initialIndexBlocks, useIndexChecksum),
				new DefaultDataFile(fullName + ".DataFile", fileNameBase + ".dat", fileAccessMode, dataBlockSize, initialDataBlocks, useDataChecksum));
	}
	
	/**
	 * Creates a new LowLevelFileDB. <br>
	 * <br>
	 *  The following default values will be set: <br>
	 * <br>
	 *  &middot; Data file block size: 1k.<br>
	 *  &middot; Initial data file block count: 100.<br>
	 *  &middot; Index file block size: 128 bytes (of which 112 bytes are reserved for the key) <br>
	 *  &middot; Intial index file block count: 100<br>
	 * <br>
	 * The index file will use CRC32 checksums but the datafile will not.
	 * 
	 * @param fileNameBase the base name (path) used when creating the data and index files.
	 * 
	 * @exception  IOException if an error occurred while creating files.
	 */
	public LowLevelFileDB(String fileNameBase, DataFile indexFile, DataFile dataFile) throws IOException
	{
		this("LowLevelFileDB(" + fileNameBase + ")", fileNameBase, indexFile, dataFile);
		this.fullNameContainsFileNameBase = true;
	}
	
	/**
	 * Creates a new LowLevelFileDB. <br>
	 * <br>
	 *  The following default values will be set: <br>
	 * <br>
	 *  &middot; Data file block size: 1k.<br>
	 *  &middot; Initial data file block count: 100.<br>
	 *  &middot; Index file block size: 128 bytes (of which 112 bytes are reserved for the key) <br>
	 *  &middot; Intial index file block count: 100<br>
	 * <br>
	 * The index file will use CRC32 checksums but the datafile will not.
	 * 
	 * @param fileNameBase the base name (path) used when creating the data and index files.
	 * 
	 * @exception  IOException if an error occurred while creating files.
	 */
	public LowLevelFileDB(String fullName, String fileNameBase, DataFile indexFile, DataFile dataFile) throws IOException
	{
		this.fullName = fullName;
		
		this.fileNameBase = fileNameBase;
		
		this.wireKeysToIndices = new HashMap();
		
		this.indexFile = indexFile;
		this.dataFile = dataFile;
      
      this.readOnlyMode = this.indexFile.getBlockFile().getDataIO().isReadOnly();
      		
		restoreIndices();
		
		//Set alternative reset method if the version of the VM is 1.3
		if(com.teletalk.jserver.util.JavaBugUtils.isUsingJava1_3_0())
		{
			this.setUseAlternativeObjectOutputStreamResetMethod(true);
			JServerUtilities.logInfo(fullName, "Defaulting flag for alternative object output stream reset method to true, because Java 2 version 1.3 is used.");
		}
		
      if( this.readOnlyMode )
      {
         this.byteOutput = null;
         this.objectOutput = null;
      }
      else
      {
         this.byteOutput = new ByteArrayOutputStream();
         this.objectOutput = new NoHeadersObjectOutputStream(byteOutput);
      }
		
		this.byteInput = new MutableByteArrayInputStream();
		this.objectInput = new NoHeadersObjectInputStream(this.byteInput);
		
		this.byteInput.setReturnObjectStreamResetCode(true);
	}
   
   /**
    * Gets the {@link DataFile} used for storage of data in this file database.
    * 
    * @since 2.1.3 (20060330)
    */
   public DataFile getDataFile()
   {
      return dataFile;
   }

   /**
    * Gets the {@link DataFile} used for storage of indices (keys) in this file database.
    * 
    * @since 2.1.3 (20060330)
    */
   public DataFile getIndexFile()
   {
      return indexFile;
   }
   
   /**
    * Gets the object that is used for synchronization of thread access in this object.
    * 
    * @since 2.1.5 (20070426)
    */
   public Object getLock()
   {
      return this;
   }

   /**
    * Checks if this file database is closed.
    * 
    * @since 2.1.3 (20060330)
    */
   public boolean isClosed()
   {
      return closed;
   }

   /**
    * Checks if this file database is opened in read only mode.
    * 
    * @since 2.1.3 (20060330)
    */
   public boolean isReadOnlyMode()
   {
      return readOnlyMode;
   }
   
   /**
    * Returns the last time (as reported by the file system) that either the index file or the data file was modified. 
    * 
    * @since 2.1.5 (20061211)
    * 
    * @see DataFile#getLastModified()
    */
   public long getLastModified()
   {
      return Math.max(this.indexFile.getLastModified(), this.dataFile.getLastModified());
   }
   
   /**
    * Gets last time that a write was performed either to the index file or the data file. 
    * 
    * @since 2.1.5 (20061211)
    * 
    * @see DataFile#getLastWrite()
    */
   public long getLastWrite()
   {
      return Math.max(this.indexFile.getLastWrite(), this.dataFile.getLastWrite());
   }
   
   /**
    * Checks if either the index file or the data file has been modified externally, i.e. through another interface or application.<br>
    * <br>
    * <b>Note:</b> Care should be exercised when using this method in write mode and when the storage is 
    * located on a different computer.
    * 
    * @since 2.1.3 (20060330)
    */
   public boolean isModifiedExternally()
   {
      return this.indexFile.isModifiedExternally() || this.dataFile.isModifiedExternally(); 
   }

	/**
	 * Sets the flag indicating if an alternative method should be used when resetting the object output streams 
	 * used when serializing objects. This alternative reset method involvs creating a 
	 * new ObjectOutputStream object (specifically a {@link com.teletalk.jserver.util.NoHeadersObjectOutputStream} object) to reset 
	 * the stream.<br>
	 * <br>
	 * The foremost purpose of this alternative reset method is to correct a bug introduced in Java 2 version 1.3, which makes impossible 
	 * to reset an ObjectOutputStream object and consequently leads to a memory leak.<br>
	 * 
	 * @param useAlternativeResetMethod boolean flag indicating if the alternativ reset method should be used (<code>true</code>), or not (<code>false</code>).
	 */
	public final void setUseAlternativeObjectOutputStreamResetMethod(boolean useAlternativeResetMethod)
	{
		this.useAlternativeObjectOutputStreamResetMethod = useAlternativeResetMethod;
	}
	
	/**
	 * Checks if an alternative method will be used when resetting the object output streams 
	 * used when serializing objects. This alternative reset method involvs creating a 
	 * new ObjectOutputStream object (specifically a {@link com.teletalk.jserver.util.NoHeadersObjectOutputStream} object) to reset 
	 * the stream.<br>
	 * <br>
	 * The foremost purpose of this alternative reset method is to correct a bug introduced in Java 2 version 1.3, which makes impossible 
	 * to reset an ObjectOutputStream object and consequently leads to a memory leak.<br>
	 * 
	 * @return <code>true</code> if the alternative reset method is currently used, otherwise <code>false</code>. 
	 */
	public final boolean isUsingAlternativeObjectOutputStreamResetMethod()
	{
		return useAlternativeObjectOutputStreamResetMethod;
	}
	
	/**
	 * Gets the full name of this LowLevelFileDB.
	 * 
	 * @return a string containing the full name of this LowLevelFileDB.
	 */
	public String getFullName()
	{
		return this.fullName;
	}
	
	/**
	 * Gets the file name base used by this LowLevelFileDB when creating the index and data files.
	 * 
	 * @return a string containing the file name base.
	 */
	public String getFileNameBase()
	{
		return this.fileNameBase;
	}
	
	/**
	 * Gets the number of items in this LowLevelFileDB.
	 * 
	 * @return the number of items in this LowLevelFileDB.
	 */
	public synchronized int size()
	{
		return wireKeysToIndices.size();
	}
	
	/**
	 * Gets the actual block size used by the underlying data file.
	 * 
	 * @return the actual block size used by the underlying data file.
	 */
	public int getDataFileBlockSize()
	{
		return this.dataFile.getBlockFile().getBlockSize();
	}
	
	/**
	 * Gets the actual block size used by the underlying index file.
	 * 
	 * @return the actual block size used by the underlying index file.
	 */
	public int getIndexFileBlockSize()
	{
		return this.indexFile.getBlockFile().getBlockSize();
	}
	
	/**
	 * Sets the flag indicating if debug mode is enabled or not.
	 * 
	 * @param enabled <code>true</code> if debug mode should be enabled, otherwise <code>false</code>.
	 */
	public void setDebugMode(boolean enabled)
	{
		this.debugMode = enabled;
	}
	
	/**
	 * Gets the value of the flag indicating if debug mode is enabled or not.
	 * 
	 * @return <code>true</code> if debug mode is enabled, otherwise <code>false</code>.
	 */
	public boolean getDebugMode()
	{
		return this.debugMode;
	}
   
	/**
	 * Restores key/data file index mappings from index file.
	 */
	private void restoreIndices() throws IOException
	{
		final int[] indexFileStartBlocks = this.indexFile.getDataStartBlocks();
		final int[] dataFileStartBlocks = this.dataFile.getDataStartBlocks();

      final ArrayList keysWithoutValues = new ArrayList();
      final IntList dataFileStartBlocksWithIndexMapping = new IntList(dataFileStartBlocks.length);

		byte[][] allIndexItemData = this.indexFile.getAllData(); // Get all index items
		byte[] indexItemData;
		int indexItemIndex;

      int dataStartBlockIndex;
      // Check index file item start blocks and create key to data mappings
		for(int i=0; i<indexFileStartBlocks.length; i++)
		{
			indexItemIndex = indexFileStartBlocks[i];
			indexItemData = allIndexItemData[i];
			
			try
			{
				// Restore key to data mapping (FileDBIndexInformation)
            FileDBIndexInformation indexInfo = this.restoreKeyToIndexMapping(indexItemData, indexItemIndex);

            if( Arrays.binarySearch(dataFileStartBlocks, indexInfo.getDataFileIndex()) < 0 )
            {
               // Data file start block of index info not found
               keysWithoutValues.add(indexInfo);
               this.wireKeysToIndices.remove(indexInfo.getKey()); // Remove key to data mapping 
            }
            else if( !this.readOnlyMode )
            {
               dataStartBlockIndex = dataFileStartBlocksWithIndexMapping.binarySearch(indexInfo.getDataFileIndex());
               
               if( dataStartBlockIndex < 0 ) // If not already added
               {
                  dataStartBlockIndex = (-dataStartBlockIndex) -1; // If not found, return value of binarySearch is (-(insertion point) - 1)
                  dataFileStartBlocksWithIndexMapping.add(dataStartBlockIndex, indexInfo.getDataFileIndex());
               }
            }
			}
			catch(Exception e)
			{
				JServerUtilities.logError(fullName, "Error while restoring key/index mapping from index file (index file index: " + indexItemIndex + ")!", e);
			}
		}
      
      if( !this.readOnlyMode )
      {
         StringBuffer unreferencedDataFileBlocksBuffer = new StringBuffer();
         StringBuffer keysWithNoDataStringBuffer = new StringBuffer();
         
         // Check for unreferenced data blocks and remove
         for(int i=0; i<dataFileStartBlocks.length; i++)
         {
            if( dataFileStartBlocksWithIndexMapping.binarySearch(dataFileStartBlocks[i]) < 0 ) // Unreferenced data start block
            {
               if( unreferencedDataFileBlocksBuffer.length() > 0 ) unreferencedDataFileBlocksBuffer.append(", ");
               unreferencedDataFileBlocksBuffer.append(String.valueOf(dataFileStartBlocks[i]));
               
               this.dataFile.deleteItemData(dataFileStartBlocks[i]);
            }
         }
          
         // Remove keys mapped to nonexistend data
         FileDBIndexInformation fileDBIndexInformation;
         for(int i=0; i<keysWithoutValues.size(); i++)
         {
            fileDBIndexInformation = (FileDBIndexInformation)keysWithoutValues.get(i);
            
            if( i > 0 ) keysWithNoDataStringBuffer.append(", ");
            keysWithNoDataStringBuffer.append(fileDBIndexInformation.getKey() + "(" + fileDBIndexInformation.getIndexFileIndex() + ")");
                        
            this.indexFile.deleteItemData(fileDBIndexInformation.getIndexFileIndex());
         }
   		
   		if( unreferencedDataFileBlocksBuffer.length() > 0 )
   		{
   			JServerUtilities.logWarning(fullName, "Unreferenced blocks detected in data file (data found, but no matching key was present). The following data blocks have been removed: " + unreferencedDataFileBlocksBuffer.toString() +".");
   		}
         if( keysWithoutValues.size() > 0 )
         {
            JServerUtilities.logWarning(fullName, "Keys with no data detected in index file (index information found, but no data was present). The following keys (and index file block) have been removed: " + keysWithNoDataStringBuffer.toString() +".");
         }
      }
	}

	/**
	 * Closes this LowLevelFileDB. A closed LowLevelFileDB cannot be reused and a new one must be created in its place.
	 */
	public synchronized void closeFileDB()
	{
		closed = true;	
		
		try
		{
			this.indexFile.close();
		}
		catch(Exception e)
		{
			JServerUtilities.logWarning(fullName, "An error occurred while closing index file.", e);
		}
		try
		{
			this.dataFile.close();
		}
		catch(Exception e)
		{
			JServerUtilities.logWarning(fullName, "An error occurred while closing data file.", e);
		}
		this.wireKeysToIndices.clear();
	}
	
	/**
	 * Closes this LowLevelFileDB and deletes the index and data files. A closed LowLevelFileDB cannot be 
	 * reused and a new one must be created in its place.
	 */
	public synchronized void deleteFileDB()
	{
		try
		{
			this.closeFileDB();
		
			new File(fileNameBase + ".idx").delete();
			new File(fileNameBase + ".dat").delete();
		}
		catch(Exception e)
		{
			JServerUtilities.logWarning(fullName, "An error occurred while deleting file db.", e);
		}
	}
   
   /**
    * Flushes any buffered data to the underlying files used by this object..
    * 
    * @since 2.1.3 (20060330)
    */
   public synchronized void flush() throws IOException
   {
      if(debugMode) JServerUtilities.logDebug(fullName, "Flushing.");
      
      this.indexFile.flush();
      this.dataFile.flush();
   }
	
   
   
   /* ### ITEM INSERT METHODS ### */
   
   
   
	/**
	 * Inserts an item into the LowLevelFileDB. The object specified by parameter <code>item</code> 
	 * will be serialized to a byte array and the current time (System.currentTimeMillis()) will be used for time stamp.<br>
    * <br>
    * If the item already exists, it will be updated.
	 * 
	 * @param key the key that is to be associated with the item.
	 * @param item the item to insert.
	 * 
	 * @exception IOException if an error occurs while performing file I/O.
	 */
	public synchronized void insertItem(final String key, final Object item) throws IOException
	{
		this.insertItem(key, this.serializeObject(item), System.currentTimeMillis());
	}
	
	/**
	 * Inserts an item into the LowLevelFileDB. The object specified by parameter <code>item</code> 
	 * will be serialized to a byte array.<br>
    * <br>
    * If the item already exists, it will be updated.
	 * 
	 * @param key the key that is to be associated with the item.
	 * @param item the item to insert.
	 * @param timeStamp the time stamp that is to be associated with the item.
	 * 
	 * @exception IOException if an error occurs while performing file I/O.
	 */
	public synchronized void insertItem(final String key, final Object item, long timeStamp) throws IOException
	{
		this.insertItem(key, this.serializeObject(item), timeStamp);
	}
	
	/**
	 * Inserts an item into the LowLevelFileDB. The current time (System.currentTimeMillis()) will be used for time stamp. <br>
    * <br>
    * If the item already exists, it will be updated.
	 * 
	 * @param key the key that is to be associated with the item.
	 * @param itemData the byte representation of the item to insert.
	 * 
	 * @exception IOException if an error occurs while performing file I/O.
	 */
	public synchronized void insertItem(final String key, final byte[] itemData) throws IOException
	{
		this.insertItem(key, itemData, System.currentTimeMillis());
	}
   
   /**
    * Inserts an item into the LowLevelFileDB.<br>
    * <br>
    * If the item already exists, it will be updated.
    * 
    * @param key the key that is to be associated with the item.
    * @param itemData the byte representation of the item to insert.
    * @param timeStamp the time stamp that is to be associated with the item.
    * 
    * @exception IOException if an error occurs while performing file I/O.
    */
   public synchronized void insertItem(final String key, final byte[] itemData, long timeStamp) throws IOException
   {
      if( this.closed) throw new IOException("File database closed!");
      if( this.readOnlyMode ) throw new IOException("Cannot insert item in read only mode!");
      
      if(timeStamp <= 0) timeStamp = System.currentTimeMillis();
      
      if(key == null) throw new RuntimeException("Null key specified!");

      FileDBIndexInformation indexInfo = (FileDBIndexInformation)wireKeysToIndices.get(key);
      
      if( indexInfo != null ) //Update
      {
         if(debugMode) JServerUtilities.logDebug(fullName, "InsertItem - Item with key " + key +" already exists - updating.");
         
         this.updateItemTimestamp(key, timeStamp);
         this.updateItemInternal(indexInfo, itemData);
      }
      else //Insert
      {
         FileDBIndexInformation fileDBIndexInformation = this.insertItemInternal(key, itemData, itemData.length, timeStamp);
         
         if(debugMode) 
         {
            JServerUtilities.logDebug(fullName, "InsertItem - Item with key: " + key +", data length: " + itemData.length + 
                                       ", timeStamp: " + timeStamp + " inserted. Index file index is: " + fileDBIndexInformation.getIndexFileIndex() + 
                                       ", data file index is " + fileDBIndexInformation.getDataFileIndex() + ".");
         }
      }
   }
   
   /**
    * Inserts a blank item into the LowLevelFileDB.<br>
    * <br>
    * If the item already exists, it will be updated.
    * 
    * @param key the key that is to be associated with the item.
    * @param itemSize the number of bytes of blank data to insert.
    * 
    * @exception IOException if an error occurs while performing file I/O.
    * 
    * @since 2.1.3 (20060331)
    */
   public synchronized void insertBlankItem(final String key, final int itemSize) throws IOException
   {
      this.insertBlankItem(key, itemSize, System.currentTimeMillis());
   }
   
   /**
    * Inserts a blank item into the LowLevelFileDB.<br>
    * <br>
    * If the item already exists, it will be updated.
    * 
    * @param key the key that is to be associated with the item.
    * @param itemSize the number of bytes of blank data to insert.
    * @param timeStamp the time stamp that is to be associated with the item.
    * 
    * @exception IOException if an error occurs while performing file I/O.
    * 
    * @since 2.1.3 (20060331)
    */
   public synchronized void insertBlankItem(final String key, final int itemSize, long timeStamp) throws IOException
   {
      if( itemSize < 0 ) throw new IOException("Cannot insert blank data with size < 0!");
      if( this.closed) throw new IOException("File database closed!");
      if( this.readOnlyMode ) throw new IOException("Cannot insert item in read only mode!");
      
      if(timeStamp <= 0) timeStamp = System.currentTimeMillis();
      
      if(key == null) throw new RuntimeException("Null key specified!");

      FileDBIndexInformation indexInfo = (FileDBIndexInformation)wireKeysToIndices.get(key);
      
      if( indexInfo != null ) //Update
      {
         if(debugMode) JServerUtilities.logDebug(fullName, "InsertItem - Item with key " + key +" already exists - updating.");
         
         this.updateItemTimestamp(key, timeStamp);
         this.updateItemInternal(indexInfo, new byte[itemSize]);
      }
      else //Insert
      {
         FileDBIndexInformation fileDBIndexInformation = this.insertItemInternal(key, null, itemSize, timeStamp);
         
         if(debugMode) 
         {
            JServerUtilities.logDebug(fullName, "InsertItem - Item with key: " + key +", data length: " + itemSize + 
                                       ", timeStamp: " + timeStamp + " inserted. Index file index is: " + fileDBIndexInformation.getIndexFileIndex() + 
                                       ", data file index is " + fileDBIndexInformation.getDataFileIndex() + ".");
         }
      }
   }
	
	/**
	 * Inserts several items into the LowLevelFileDB. The objects specified by parameter <code>items</code> 
	 * will be serialized to byte arrays and the current time (System.currentTimeMillis()) will be used for time stamp.
	 * 
	 * @param keys the keys that are to be associated with the items.
	 * @param items the items to insert.
	 * 
	 * @exception IOException if an error occurs while performing file I/O.
	 */
	public synchronized void insertMultipleItems(final String[] keys, final Object[] items) throws IOException
	{
		final long[] timeStamps = new long[keys.length];
		final byte[][] itemData = new byte[keys.length][];
		
		for(int i=0; i<keys.length; i++)
		{
			timeStamps[i] = System.currentTimeMillis();
			itemData[i] = this.serializeObject(items[i]);
		}
		
		this.insertMultipleItems(keys, itemData, timeStamps);
	}
	
	/**
	 * Inserts several items into the LowLevelFileDB. The objects specified by parameter <code>items</code> 
	 * will be serialized to byte arrays.
	 * 
	 * @param keys the keys that are to be associated with the items.
	 * @param items the items to insert.
	 * @param timeStamps the time stamps that are to be associated with the items.
	 * 
	 * @exception IOException if an error occurs while performing file I/O.
	 */
	public synchronized void insertMultipleItems(final String[] keys, final Object[] items, final long[] timeStamps) throws IOException
	{
		final byte[][] itemData = new byte[keys.length][];
		
		for(int i=0; i<keys.length; i++)
		{
			itemData[i] = this.serializeObject(items[i]);
		}
		
		this.insertMultipleItems(keys, itemData, timeStamps);
	}
	
	/**
	 * Inserts several items into the LowLevelFileDB. The current time (System.currentTimeMillis()) will be used for time stamp.
	 * 
	 * @param keys the keys that are to be associated with the items.
	 * @param itemData the byte representation of the items to insert.
	 * 
	 * @exception IOException if an error occurs while performing file I/O.
	 */
	public synchronized void insertMultipleItems(final String[] keys, final byte[][] itemData) throws IOException
	{
		final long[] timeStamps = new long[keys.length];
				
		for(int i=0; i<keys.length; i++)
		{
			timeStamps[i] = System.currentTimeMillis();
		}
		
		this.insertMultipleItems(keys, itemData, timeStamps);
	}
	
	/**
	 * Inserts several items into the LowLevelFileDB. 
	 * 
	 * @param keys the keys that are to be associated with the items.
	 * @param itemData the byte representation of the items to insert.
	 * @param timeStamps the time stamps that are to be associated with the items.
	 * 
	 * @exception IOException if an error occurs while performing file I/O.
	 */
	public synchronized void insertMultipleItems(final String[] keys, final byte[][] itemData, long[] timeStamps) throws IOException
	{
		if( this.closed ) throw new IOException("File database closed!");
      if( this.readOnlyMode ) throw new IOException("Cannot insert items in read only mode!");
		
		for(int i=0; i<timeStamps.length; i++)
		{
			if(timeStamps[i] <= 0) timeStamps[i] = System.currentTimeMillis();
		}
						
		if(keys == null) throw new RuntimeException("Null keys specified!");
		
		//Check if any of the keys already exists, and if so - delete
		for(int  i=0; i<keys.length; i++)
		{
			if(this.wireKeysToIndices.containsKey(keys[i])) //Delete
			{
				if(debugMode) JServerUtilities.logDebug(fullName, "InsertMultipleItems - Item with key " + keys[i] +" already exists - deleting.");
				this.deleteItem(keys[i]);
			}
		}
		
		//Write to data file
		final int[] dataFileIndices = dataFile.insertMultipleItemData(itemData);
		
		//Write to index file
		final FileDBIndexInformation[] fileDBIndexInformation = this.createKeyToIndexMappings(keys, dataFileIndices, timeStamps);
			
      if(debugMode) 
      {
         for(int i=0; i<keys.length; i++)
         {
				JServerUtilities.logDebug(fullName, "InsertMultipleItems - Item with key: " + keys[i] +", data length: " + itemData[i].length + 
										", timeStamp: " + timeStamps[i] + " inserted. Index file index is: " + fileDBIndexInformation[i].getIndexFileIndex() + 
                              ", data file index is " + fileDBIndexInformation[i].getDataFileIndex() + ".");
			}
		}
	}
   
   
	
   /* ### ITEM UPDATE/APPEND METHODS ### */
   
   
	
	/**
	 * Updates an item in this LowLevelFileDB with new data. The object specified by parameter <code>item</code> 
	 * will be serialized to a byte array.<br>
    * <br>
    * If the item doesn't exists, it will be inserted.
	 * 
	 * @param key the key of the item to update.
	 * @param item the new item that will replace the already existing one.
	 * 
	 * @exception IOException if an error occurs while performing file I/O.
	 */
	public synchronized void updateItem(final String key, final Object item) throws IOException
	{
		this.updateItem(key, this.serializeObject(item));
	}
	
	/**
	 * Updates an item in this LowLevelFileDB with new data.<br>
    * <br>
    * If the item doesn't exists, it will be inserted.
	 * 
	 * @param key the key of the item to update.
	 * @param itemData the byte representation of the new item that will replace the already existing one.
	 * 
	 * @exception IOException if an error occurs while performing file I/O.
	 */
	public synchronized void updateItem(final String key, final byte[] itemData) throws IOException
	{
		if( this.closed ) throw new IOException("File database closed!");
      if( this.readOnlyMode ) throw new IOException("Cannot update item in read only mode!");
		
		FileDBIndexInformation indexInfo = (FileDBIndexInformation)wireKeysToIndices.get(key);
		
		if(indexInfo != null)
		{
         this.updateItemInternal(indexInfo, itemData);
			
			if(debugMode) JServerUtilities.logDebug(fullName, "UpdateItem - Item with key: " + key +", data length: " + itemData.length + " successfully updated.");
		}
		else // Insert
		{
         if(debugMode) JServerUtilities.logDebug(fullName, "UpdateItem - Item with key " + key +" not found - inserting.");
         
         FileDBIndexInformation fileDBIndexInformation = this.insertItemInternal(key, itemData, itemData.length, System.currentTimeMillis());
         
         if(debugMode) 
         {
            JServerUtilities.logDebug(fullName, "UpdateItem - Item with key: " + key +", data length: " + itemData.length + 
                                       ", timeStamp: " + fileDBIndexInformation.getTimeStamp() + " inserted. Index file index is: " + fileDBIndexInformation.getIndexFileIndex() + 
                                       ", data file index is " + fileDBIndexInformation.getDataFileIndex() + ".");
         }
		}
	}
	
	/**
	 * Performs a partial update on an item in this LowLevelFileDB. 
	 * 
	 * @param key the key of the item to update partially.
	 * @param partialItemData the byte representation of the partial data that will be inserted into the already existing data block.
	 * @param itemDataOffset the offset in the existing data block at which the partialItemData byte array will be inserted.
	 * 
	 * @return <code>true</code> if an item with the specified key was found in this LowLevelFileDB, otherwise <code>false</code>.
	 * 
	 * @exception IOException if an error occurs while performing file I/O.
	 */
	public synchronized boolean updatePartialItem(final String key, final byte[] partialItemData, final int itemDataOffset) throws IOException
	{
		if( this.closed ) throw new IOException("File database closed!");
      if( this.readOnlyMode ) throw new IOException("Cannot update item in read only mode!");
		
		FileDBIndexInformation indexInfo = (FileDBIndexInformation)wireKeysToIndices.get(key);
		
		if(indexInfo != null)
		{
			dataFile.updatePartialItemData(indexInfo.getDataFileIndex(), itemDataOffset, partialItemData);
			
			if(debugMode) JServerUtilities.logDebug(fullName, "UpdatePartialItem - Item with key: " + key +", partial item data length: " + partialItemData.length + ", itemDataOffset: " + itemDataOffset + " successfully updated.");
				
			return true;
		}
		else
		{
			if(debugMode) JServerUtilities.logDebug(fullName, "UpdatePartialItem - Failed to update item with key: " + key +", partial item data length: " + partialItemData.length + ", itemDataOffset: " + itemDataOffset + ". Key not found.");
			
			return false;
		}
	}
   
   /**
    * Appends data to an item in this LowLevelFileDB.<br>
    * <br>
    * If the item doesn't exists, it will be inserted.
    * 
    * @param key the key of the item to append data to.
    * @param appendData the data to be appended.
    * 
    * @exception IOException if an I/O error occurs.
    * 
    *  @since 2.1.3 (20060330)
    */
   public synchronized void appendItem(final String key, final Object appendData) throws IOException
   {
      this.appendItem(key, this.serializeObject(appendData));
   }
   
   /**
    * Appends data to an item in this LowLevelFileDB.<br>
    * <br>
    * If the item doesn't exists, it will be inserted.
    * 
    * @param key the key of the item to append data to.
    * @param appendData the data to be appended.
    * 
    * @exception IOException if an I/O error occurs.
    * 
    *  @since 2.1.3 (20060329)
    */
   public synchronized void appendItem(final String key, final byte[] appendData) throws IOException
   {
      if( this.closed ) throw new IOException("File database closed!");
      if( this.readOnlyMode ) throw new IOException("Cannot append item in read only mode!");
      
      FileDBIndexInformation indexInfo = (FileDBIndexInformation)wireKeysToIndices.get(key);
      
      if( indexInfo != null )
      {
         dataFile.appendItemData(indexInfo.getDataFileIndex(), appendData);
         
         if(debugMode) JServerUtilities.logDebug(fullName, "AppendItem - Item with key: " + key +", append data length: " + appendData.length + " successfully updated.");
      }
      else
      {
         if(debugMode) JServerUtilities.logDebug(fullName, "AppendItem - Item with key " + key +" not found - inserting.");
         
         FileDBIndexInformation fileDBIndexInformation = this.insertItemInternal(key, appendData, appendData.length, System.currentTimeMillis());
         
         if(debugMode) 
         {
            JServerUtilities.logDebug(fullName, "AppendItem - Item with key: " + key +", data length: " + appendData.length + 
                                       ", timeStamp: " + fileDBIndexInformation.getTimeStamp() + " inserted. Index file index is: " + fileDBIndexInformation.getIndexFileIndex() + 
                                       ", data file index is " + fileDBIndexInformation.getDataFileIndex() + ".");
         }
      }
   }
   
   /**
    * Appends blank data to an item in this LowLevelFileDB.<br>
    * <br>
    * If the item doesn't exists, it will be inserted.
    * 
    * @param key the key of the item to append data to.
    * @param appendDataSize the number of bytes to append.
    * 
    * @exception IOException if an I/O error occurs.
    * 
    *  @since 2.1.3 (20060331)
    */
   public synchronized void appendItem(final String key, final int appendDataSize) throws IOException
   {
      if( appendDataSize < 0 ) throw new IOException("Cannot append blank data with size < 0!");
      if( this.closed ) throw new IOException("File database closed!");
      if( this.readOnlyMode ) throw new IOException("Cannot append item in read only mode!");
      
      FileDBIndexInformation indexInfo = (FileDBIndexInformation)wireKeysToIndices.get(key);
      
      if(indexInfo != null)
      {
         dataFile.appendBlankItemData(indexInfo.getDataFileIndex(), appendDataSize);
         
         if(debugMode) JServerUtilities.logDebug(fullName, "AppendBlankItem - Item with key: " + key +", append data length: " + appendDataSize + " successfully updated.");
      }
      else
      {
         if(debugMode) JServerUtilities.logDebug(fullName, "AppendBlankItem - Item with key " + key +" not found - inserting.");
         
         FileDBIndexInformation fileDBIndexInformation = this.insertItemInternal(key, null, appendDataSize, System.currentTimeMillis());
         
         if(debugMode) 
         {
            JServerUtilities.logDebug(fullName, "AppendBlankItem - Item with key: " + key +", data length: " + appendDataSize + 
                                       ", timeStamp: " + fileDBIndexInformation.getTimeStamp() + " inserted. Index file index is: " + fileDBIndexInformation.getIndexFileIndex() + 
                                       ", data file index is " + fileDBIndexInformation.getDataFileIndex() + ".");
         }
      }
   }
   
   
   
   /* ### ITEM DELETE METHODS ### */
   
   
   
   /**
    * Removes data at the end of an item in this LowLevelFileDB.
    * 
    * @param key the key of the item to remove data from.
    * @param removeSize the number of bytes of data to remove.
    * 
    * @return <code>true</code> if an item with the specified key was found in this LowLevelFileDB, otherwise <code>false</code>.
    * 
    * @exception IOException if an error occurs while performing file I/O.
    * 
    * @since 2.1.3 (20060403)
    */
   public synchronized boolean deletePartialItem(final String key, final int removeSize) throws IOException
   {
      if( this.closed ) throw new IOException("File database closed!");
      if( this.readOnlyMode ) throw new IOException("Cannot partially delete item in read only mode!");
      
      FileDBIndexInformation indexInfo = (FileDBIndexInformation)wireKeysToIndices.get(key);
      
      if(indexInfo != null)
      {
         dataFile.deletePartialItemData(indexInfo.getDataFileIndex(), removeSize);
         
         if(debugMode) JServerUtilities.logDebug(fullName, "RemovePartialItem - Item with key: " + key +", remove size: " + removeSize + " successfully deleted partially.");
                  
         return true;
      }
      else
      {
         if(debugMode) JServerUtilities.logDebug(fullName, "RemovePartialItem - Failed to partially delete item with key: " + key +". Key NOT found.");
         
         return false;
      }
   }
   
	/**
	 * Deletes an item in this LowLevelFileDB.
	 * 
	 * @param key the key of the item to delete.
	 * 
	 * @return <code>true</code> if an item with the specified key was found in this LowLevelFileDB, otherwise <code>false</code>.
	 * 
	 * @exception IOException if an error occurs while performing file I/O.
	 */
	public synchronized boolean deleteItem(final String key) throws IOException
	{
		if( this.closed ) throw new IOException("File database closed!");
      if( this.readOnlyMode ) throw new IOException("Cannot delete item in read only mode!");
		
		FileDBIndexInformation indexInfo = (FileDBIndexInformation)wireKeysToIndices.remove(key);
		
		if(indexInfo != null)
		{
			indexFile.deleteItemData(indexInfo.getIndexFileIndex());
			dataFile.deleteItemData(indexInfo.getDataFileIndex());
			
			if(debugMode) JServerUtilities.logDebug(fullName, "DeleteItem - Item with key: " + key +" at index file index " + indexInfo.getIndexFileIndex() + ", data file index: " + indexInfo.getDataFileIndex() + " sucessfully deleted.");
			
			return true;
		}
		else
		{
			if(debugMode) JServerUtilities.logDebug(fullName, "DeleteItem - Failed to delete item with key: " + key +". Key NOT found.");
			
			return false;
		}
	}
	
	/**
	 * Deletes all items in this LowLevelFileDB.
	 * 
	 * @exception IOException if an error occurs while performing file I/O.
	 */
	public synchronized void deleteAllItems() throws IOException
	{
		if( this.closed ) throw new IOException("File database closed!");
      if( this.readOnlyMode ) throw new IOException("Cannot delete all items in read only mode!");
		
		if(debugMode) JServerUtilities.logDebug(fullName, "DeleteAllItems - Deleting all items.");
		
		this.indexFile.clearAllBlocks();
		this.dataFile.clearAllBlocks();
		
		this.wireKeysToIndices.clear();
	}
	
   
   
   /* ### ITEM GET METHODS ### */
   
   
   
	/**
	 * Gets an item stored in this LowLevelFileDB as an object.
	 * 
	 * @param key the key of the item to get.
	 * 
	 * @return the item as an object or null if no item with the specified key was found.
	 * 
	 * @exception IOException if an error occurs while performing file I/O.
	 * @exception ClassNotFoundException the class of the read object was not found.
	 */
	public synchronized Object getItemAsObject(final String key) throws IOException, ClassNotFoundException
	{
		if(closed) throw new RuntimeException("File database closed!");
		
		final byte[] byteData = getItem(key);
		
		if(byteData != null) return this.deSerializeObject(byteData);
		else return null;
	}
   
   /**
    * Gets an item stored in this LowLevelFileDB a collection of objects. This method may for instance be used if 
    * the method {@link #appendItem(String, Object)} has been used to append objects to an existing item.
    * 
    * @param key the key of the item to get.
    * 
    * @return the items as an object array or null if no item with the specified key was found.
    * 
    * @exception IOException if an error occurs while performing file I/O.
    * @exception ClassNotFoundException the class of the read object was not found.
    */
   public synchronized Object[] getItemAsObjects(final String key) throws IOException, ClassNotFoundException
   {
      return getItemAsObjects(key, false);
   }
   
   /**
    * Gets an item stored in this LowLevelFileDB a collection of objects. This method may for instance be used if 
    * the method {@link #appendItem(String, Object)} has been used to append objects to an existing item.
    * 
    * @param key the key of the item to get.
    * @param ignoreDeserializationError flag indicating if deserialization errors should be ignored. If this flag is true, this method will read as many valid objects as possible.
    * 
    * @return the items as an object array or null if no item with the specified key was found.
    * 
    * @exception IOException if an error occurs while performing file I/O.
    * @exception ClassNotFoundException the class of the read object was not found.
    * 
    * @since 2.1.3 (20060330)
    */
   public synchronized Object[] getItemAsObjects(final String key, final boolean ignoreDeserializationError) throws IOException, ClassNotFoundException
   {
      if(closed) throw new RuntimeException("File database closed!");
      
      final byte[] byteData = getItem(key);
      
      if(byteData != null) return this.deSerializeObjects(byteData, ignoreDeserializationError);
      else return null;
   }
	
   /**
    * Gets items stored in this LowLevelFileDB as an object.
    * 
    * @param keys the keys of the items to get.
    * 
    * @return the items as an object array.
    * 
    * @exception IOException if an error occurs while performing file I/O.
    * @exception ClassNotFoundException the class of the read object was not found.
    */
	public synchronized Object[] getMultipleItemsAsObjects(final String[] keys) throws IOException, ClassNotFoundException
	{
		if(closed) throw new RuntimeException("File database closed!");
		
		byte[][] byteData = this.getMultipleItems(keys);
		final Object[] objects = new Object[byteData.length];
		
		if(byteData != null)
		{
			for(int i=0; i<objects.length; i++)
			{
				objects[i] = this.deSerializeObject(byteData[i]);
				byteData[i] = null;
			}
			byteData = null;
			
			return objects;
		}
		else return new Object[0];
	}
   
   /**
    * Gets the size of an item stored in this LowLevelFileDB as an object.
    * 
    * @return the data size.
    * 
    * @exception IOException if an I/O error occurs.
    * 
    * @since 2.1.3 (20060329)
    */
   public synchronized int getItemSize(final String key) throws IOException
   {
      if(closed) throw new RuntimeException("File database closed!");
      
      FileDBIndexInformation indexInfo = (FileDBIndexInformation)wireKeysToIndices.get(key);
      
      if(indexInfo != null)
      {
         if(debugMode) JServerUtilities.logDebug(fullName, "GetItemSize - Getting size of item with key: " + key +". Data file index is: " + indexInfo.getDataFileIndex() + ".");
         
         return dataFile.getItemDataSize(indexInfo.getDataFileIndex());
      }
      else
      {
         if(debugMode) JServerUtilities.logDebug(fullName, "GetItemSize - Failed to get size of item with key: " + key +". Key NOT found.");
         
         return -1;
      }
   }
	
	/**
	 * Gets an item stored in this LowLevelFileDB.
	 * 
	 * @param key the key of the item to get.
	 * 
	 * @return the item as a byte array  or null if no item with the specified key was found.
	 * 
	 * @exception IOException if an error occurs while performing file I/O.
	 */
	public synchronized byte[] getItem(final String key) throws IOException
	{
		if(closed) throw new RuntimeException("File database closed!");
		
		FileDBIndexInformation indexInfo = (FileDBIndexInformation)wireKeysToIndices.get(key);
		
		if(indexInfo != null)
		{
			if(debugMode) JServerUtilities.logDebug(fullName, "GetItem - Getting item with key: " + key +". Data file index is: " + indexInfo.getDataFileIndex() + ".");
			
			return dataFile.getItemData(indexInfo.getDataFileIndex());
		}
		else
		{
			if(debugMode) JServerUtilities.logDebug(fullName, "GetItem - Failed to get item with key: " + key +". Key NOT found.");
			
			return null;
		}
	}
   
   /**
    * Gets a part of the data of an item stored in this LowLevelFileDB.
    * 
    * @param key the key of the item to get.
    * @param offset the offset in the data.
    * @param length the number of bytes to read.
    * 
    * @return the item as a byte array  or null if no item with the specified key was found.
    * 
    * @exception IOException if an error occurs while performing file I/O.
    * 
    * @since 2.1.3 (20060329)
    */
   public synchronized byte[] getPartialItem(final String key, final int offset, final int length) throws IOException
   {
      if(closed) throw new RuntimeException("File database closed!");
      
      FileDBIndexInformation indexInfo = (FileDBIndexInformation)wireKeysToIndices.get(key);
      
      if(indexInfo != null)
      {
         if(debugMode) JServerUtilities.logDebug(fullName, "GetPartialItem - Getting item with key: " + key +". Data file index is: " + indexInfo.getDataFileIndex() + ".");
         
         return dataFile.getPartialItemData(indexInfo.getDataFileIndex(), offset, length);
      }
      else
      {
         if(debugMode) JServerUtilities.logDebug(fullName, "GetPartialItem - Failed to get item with key: " + key +". Key NOT found.");
         
         return null;
      }
   }
	
   /**
    * Gets items stored in this LowLevelFileDB.
    * 
    * @param keys the keys of the items to get.
    * 
    * @return the items as an byte matrix.
    * 
    * @exception IOException if an error occurs while performing file I/O.
    */
	public synchronized byte[][]getMultipleItems(final String[] keys) throws IOException
	{
		if(closed) throw new RuntimeException("File database closed!");
		
		final IntList dataFileIndices = new IntList(keys.length);
		FileDBIndexInformation indexInfo;
		
		for(int i=0; i<keys.length; i++)
		{
			indexInfo = (FileDBIndexInformation)wireKeysToIndices.get(keys[i]);
			
			if(indexInfo != null)
			{
				if(debugMode) JServerUtilities.logDebug(fullName, "GetMultipleItems - Getting item with key: " + keys[i] +". Data file index is: " + indexInfo.getDataFileIndex() + ".");
				dataFileIndices.add(indexInfo.getDataFileIndex());
			}
			else
			{
				if(debugMode) JServerUtilities.logDebug(fullName, "GetMultipleItems - Cannot get item with key: " + keys[i] +". Key NOT found.");
			}
		}
		
		if(dataFileIndices.size() > 0) return dataFile.getMultipleItemData(dataFileIndices.toArray());
		else return new byte[0][0];
	}
   
   /**
    * Gets all items stored in this LowLevelFileDB. The return value of this method is an Object matrix with 
    * two columns. The first column (Object[x][0]) contains String keys and the other (Object[x][1]) contains 
    * item data in byte[] format.
    * 
    * @return an Object matrix. 
    * 
    * @exception IOException if an error occurs while performing file I/O.
    */
   public synchronized Object[][] getAllItems() throws IOException
   {
      if(debugMode) JServerUtilities.logDebug(fullName, "GetAllItems - Getting all items.");
      
      if(closed) throw new RuntimeException("File database closed!");
      
      return this.getAllItems(getIndexFileIndexSortedKeyToIndexMappings());
   }
   
   /**
    * Gets all items stored in this LowLevelFileDB. The return value of this method is an Object matrix with 
    * two columns. The first column (Object[x][0]) contains String keys and the other (Object[x][1]) contains 
    * item data in byte[] format.
    * 
    * @return an Object matrix. 
    * 
    * @exception IOException if an error occurs while performing file I/O.
    */
   public synchronized Object[][] getAllItemsTimeStampOrdered() throws IOException
   {
      if(debugMode) JServerUtilities.logDebug(fullName, "GetAllItemsTimeStampOrdered - Getting all items.");
      
      if(closed) throw new RuntimeException("File database closed!");
      
      return this.getAllItems(getTimeStamptSortedKeyToIndexMappings());
   }
   
   /**
    * Gets all items stored in this LowLevelFileDB as objects. The return value of this method is an Object matrix with 
    * two columns. The first column (Object[x][0]) contains String keys and the other (Object[x][1]) contains 
    * item data in Object format.
    * 
    * @return an Object matrix. 
    * 
    * @exception IOException if an error occurs while performing file I/O.
    */
   public synchronized Object[][] getAllItemsAsObjects() throws IOException, ClassNotFoundException
   {
      if(debugMode) JServerUtilities.logDebug(fullName, "GetAllItemsAsObjects - Getting all items.");
      
      if(closed) throw new RuntimeException("File database closed!");
      
      return this.getAllItemsAsObjects(getIndexFileIndexSortedKeyToIndexMappings());
   }
   
   /**
    * Gets all items stored in this LowLevelFileDB as objects. The return value of this method is an Object matrix with 
    * two columns. The first column (Object[x][0]) contains String keys and the other (Object[x][1]) contains 
    * item data in Object format.
    * 
    * @return an Object matrix. 
    * 
    * @exception IOException if an error occurs while performing file I/O.
    */
   public synchronized Object[][] getAllItemsAsObjectsTimeStampOrdered() throws IOException, ClassNotFoundException
   {
      if(debugMode) JServerUtilities.logDebug(fullName, "GetAllItemsAsObjectsTimeStampOrdered - Getting all items.");
      
      if(closed) throw new RuntimeException("File database closed!");
      
      return this.getAllItemsAsObjects(getTimeStamptSortedKeyToIndexMappings());
   }
   
   
   
   /* ### MISC ITEM METHODS ### */
	
   
   
   /**
    * Checks if the item with the specified key exists in this file database.
    * 
    * @param key the key of the item to check existency for.
    * 
    * @since 2.1.3 (20060330)
    */
   public synchronized boolean containsItem(final String key)
   {
      return this.wireKeysToIndices.containsKey(key);
   }
   
   /**
    * Gets the timestamp of a stored item,
    * 
    * @param key the key of the item to get.
    * 
    * @return the timestamp or -1 if the items wasn't found.
    * 
    * @since 2.1.3 (20060330)
    */
   public synchronized long getItemTimestamp(final String key)
   {
      FileDBIndexInformation indexInfo = (FileDBIndexInformation)this.wireKeysToIndices.get(key);
      
      if(indexInfo != null) return indexInfo.getTimeStamp();
      else return -1;
   }
   
   /**
    * Updates the timestamp of a stored item,
    * 
    * @param key the key of the item to update.
    * @param timestamp the new timestamp.
    * 
    * @return true if the item was found and updated, otherwise false.
    * 
    * @since 2.1.3 (20060330)
    */
   public synchronized boolean updateItemTimestamp(final String key, final long timestamp) throws IOException
   {
      FileDBIndexInformation indexInfo = (FileDBIndexInformation)this.wireKeysToIndices.get(key);
      
      if(indexInfo != null)
      {
         indexInfo.setTimeStamp(timestamp);
         
         this.updateKeyToIndexMapping(indexInfo);
         
         return true;
      }
      else 
      {
         return false;
      }
   }
   
   /**
    * Gets all the keys in this LowLevelFileDB.
    * 
    * @return a String array containing the keys.
    */
   public synchronized String[] getKeys()
   {
      if(debugMode) JServerUtilities.logDebug(fullName, "GetKeys - Getting keys.");
      
      if(closed) throw new RuntimeException("File database closed!");
            
      return this.getKeys(getIndexFileIndexSortedKeyToIndexMappings());
   }
   
   /**
    * Gets all the keys in this LowLevelFileDB, sorted according to time stamp.
    * 
    * @return a String array containing the keys.
    */
   public synchronized String[] getKeysTimeStampOrdered()
   {
      if(debugMode) JServerUtilities.logDebug(fullName, "GetKeysTimeStampOrdered - Getting keys.");
      
      if(closed) throw new RuntimeException("File database closed!");
         
      return this.getKeys(getTimeStamptSortedKeyToIndexMappings());
   }
   
   
   
   /* ### MISC METHODS ### */
   
   
   
   /**
    * Returns a string representation of this LowLevelFileDB.
    * 
    * @return a string representation of this LowLevelFileDB.
    */
   public String toString()
   {
      String fName;
      
      if(this.fullNameContainsFileNameBase) fName = getFullName();
      else fName = getFullName() + "(" + this.fileNameBase + ")";
      
      return "[" + fName + ", " + this.size() + " items]";
   }
   
   /**
    * Resets the object input stream used for deserializing objects.
    * 
    * @since 2.1.3 (20060331)
    */
   public synchronized void resetObjectDeserializer() throws IOException
   {
      this.objectInput = new NoHeadersObjectInputStream(this.byteInput);
   }
   
   
   
   /* ### INTERNAL KEY/DATA MAPPING METHODS ### */
   
   
   
   /**
    * Internal method to insert an item.
    */
   private FileDBIndexInformation insertItemInternal(final String key, final byte[] itemData, final int dataSize, long timeStamp) throws IOException
   {
      // Write to data file
      final int dataFileIndex;
      if( itemData != null ) dataFileIndex = this.dataFile.insertItemData(itemData);
      else dataFileIndex = this.dataFile.insertBlankItemData(dataSize);
   
      // Create index and write to index file
      return this.createKeyToIndexMapping(key, dataFileIndex, timeStamp);
   }
   
   /**
    * Internal method to update an item.
    */
   public void updateItemInternal(final FileDBIndexInformation indexInfo, final byte[] itemData) throws IOException
   {
      this.dataFile.updateItemData(indexInfo.getDataFileIndex(), itemData);
   }
   
   /**
    * Formats a key to data file index mapping to a byte array.
    */
   private byte[] formatKeyToIndexMappingData(final String key, final int dataFileIndex, final long timeStamp)
   {
      final char[] stringChars = key.toCharArray();
      final int strlen = stringChars.length;
      int utfLength = 0;
      int c;
      int i, u;

      //Calculate length of UTF string
      for (i=0 ; i < strlen ; i++) 
      {
          c = stringChars[i];
          if ((c >= 0x0001) && (c <= 0x007F)) utfLength++;
          else if (c > 0x07FF) utfLength += 3;
          else utfLength += 2;
      }

      //4 bytes (32 bit integer) for string length, 4 bytes for the dataFileIndex and 8 bytes for the timestamp
      final byte[] indexInfoBytes = new byte[utfLength + 4 + 4 + 8]; 

      indexInfoBytes[0] = (byte)((utfLength >>> 24));
      indexInfoBytes[1] = (byte)((utfLength >>> 16));
      indexInfoBytes[2] = (byte)((utfLength >>> 8));
      indexInfoBytes[3] = (byte)((utfLength >>> 0));
      
      //Convert the string to UTF bytes and fill the indexInfoBytes array
      for (i=0, u=4 ; i<strlen ; i++) 
      {
         c = stringChars[i];
         
         if ((c >= 0x0001) && (c <= 0x007F)) 
         {
            indexInfoBytes[u++] = (byte)c;
          } 
         else if (c > 0x07FF) 
         {
            indexInfoBytes[u++] = (byte)(0xE0 | ((c >> 12) & 0x0F));
            indexInfoBytes[u++] = (byte)(0x80 | ((c >>  6) & 0x3F));
            indexInfoBytes[u++] = (byte)(0x80 | ((c >>  0) & 0x3F));
          } 
         else 
         {
            indexInfoBytes[u++] = (byte)(0xC0 | ((c >>  6) & 0x1F));
            indexInfoBytes[u++] = (byte)(0x80 | ((c >>  0) & 0x3F));
         }
      }
      
      //Convert the data file index integer to bytes
      indexInfoBytes[u++] = (byte)((dataFileIndex >>> 24));
      indexInfoBytes[u++] = (byte)((dataFileIndex >>> 16));
      indexInfoBytes[u++] = (byte)((dataFileIndex >>> 8));
      indexInfoBytes[u++] = (byte)((dataFileIndex >>> 0));
      
      //Convert the data file index integer to bytes
      indexInfoBytes[u++] = (byte)((timeStamp >>> 56));
      indexInfoBytes[u++] = (byte)((timeStamp >>> 48));
      indexInfoBytes[u++] = (byte)((timeStamp >>> 40));
      indexInfoBytes[u++] = (byte)((timeStamp >>> 32));
      indexInfoBytes[u++] = (byte)((timeStamp >>> 24));
      indexInfoBytes[u++] = (byte)((timeStamp >>> 16));
      indexInfoBytes[u++] = (byte)((timeStamp >>> 8));
      indexInfoBytes[u++] = (byte)((timeStamp >>> 0));
      
      return indexInfoBytes;
   }
   
   /**
    * Creates a FileDBIndexInformation record (key/data file index mapping) and writes it to the index file.
    */
   private FileDBIndexInformation createKeyToIndexMapping(final String key, final int dataFileIndex, final long timeStamp) throws IOException
   {
      final int indexFileIndex = this.indexFile.insertItemData(formatKeyToIndexMappingData(key, dataFileIndex, timeStamp));
      
      final FileDBIndexInformation fileDBIndexInformation = new FileDBIndexInformation(key, indexFileIndex, dataFileIndex, timeStamp); 
      
      // Map key with indices
      this.wireKeysToIndices.put(key, fileDBIndexInformation);
      
      return fileDBIndexInformation;
   }

   /**
    * Updates an FileDBIndexInformation record (key/data file index mapping).
    */
   private void updateKeyToIndexMapping(final FileDBIndexInformation fileDBIndexInformation) throws IOException
   {
      this.indexFile.updateItemData( fileDBIndexInformation.getIndexFileIndex(), 
            this.formatKeyToIndexMappingData( fileDBIndexInformation.getKey(), fileDBIndexInformation.getDataFileIndex(), fileDBIndexInformation.getTimeStamp() ) );
   }
   
   /**
    * Creates FileDBIndexInformation records (key/data file index mappings) and writes it to the index file.
    */
   private FileDBIndexInformation[] createKeyToIndexMappings(final String[] keys, final int[] dataFileIndices, final long[] timeStamps) throws IOException
   {
      final byte[][] indexInfoBytes = new byte[keys.length][];
      
      for(int i=0; i<keys.length; i++)
      {
         indexInfoBytes[i] = formatKeyToIndexMappingData(keys[i], dataFileIndices[i], timeStamps[i]);
      }
      
      final int[] indexFileIndices = indexFile.insertMultipleItemData(indexInfoBytes);
      
      final FileDBIndexInformation[] fileDBIndexInformation = new FileDBIndexInformation[indexFileIndices.length];
      for(int i=0; i<keys.length; i++)
      {
         fileDBIndexInformation[i] = new FileDBIndexInformation(keys[i], indexFileIndices[i], dataFileIndices[i], timeStamps[i]); 
         
         // Map key with indices
         this.wireKeysToIndices.put(fileDBIndexInformation[i].getKey(), fileDBIndexInformation[i]);
      }
      
      return fileDBIndexInformation;
   }
   
   /**
    * Restores an key/data file index mapping 
    */
   private FileDBIndexInformation restoreKeyToIndexMapping(final byte[] indexInfoBytes, final int indexFileIndex) throws IOException
   {
      int utfLength = 0;
      long tmp;
      int arrayIndex = 0;
      
      for(int i=0; i<4; i++)
      {
         tmp = (indexInfoBytes[arrayIndex++] + 256) & 0xFF;
         utfLength += (tmp << 8*(3-i));
      }

      //Restore UTF key
      char utfString[] = new char[utfLength];
      int count = 0;
      int strlen = 0;
      int c;
      //byte c1, c2, c3;
      int c1, c2, c3;

      while(count < utfLength)
      {
          c1 = (indexInfoBytes[arrayIndex++] & 0xff);
          c = c1 >> 4; 

         if( (c >= 0) && (c <= 7) ) // 0001 - 007F
         {
               count++;
               utfString[strlen++] = (char)c1;
         }
         else if( (c == 12) || (c == 13) ) // 0080 - 07FF
         {
               count += 2;
               if (count > utfLength) 
               {
                  throw new UTFDataFormatException();
               }
               
               c2 = (indexInfoBytes[arrayIndex++] & 0xff);
               if ((c2 & 0xC0) != 0x80) 
               {
                  throw new UTFDataFormatException();
               }
               
               utfString[strlen++] = (char)(((c1 & 0x1F) << 6) | (c2 & 0x3F));
         }
         else if(c == 14) // 0800 - FFFF
         {
               count += 3;
               
               if (count > utfLength) 
               {
                  throw new UTFDataFormatException();
               }
               
               c2 = (indexInfoBytes[arrayIndex++] & 0xff);
               c3 = (indexInfoBytes[arrayIndex++] & 0xff);
               
               if (((c2 & 0xC0) != 0x80) || ((c3 & 0xC0) != 0x80)) 
               {
                  throw new UTFDataFormatException();
               }
               
               utfString[strlen++] = (char)(((c1 & 0x0F) << 12) | ((c2 & 0x3F) << 6) | ((c3 & 0x3F) << 0));
         }
         else
         {
            throw new UTFDataFormatException();
         }
      }
      
      String key = new String(utfString, 0, strlen);

      int dataFileIndex = 0;
      
      //Restore data file index
      for(int i=0; i<4; i++)
      {
         tmp = (indexInfoBytes[arrayIndex++] + 256) & 0xFF;
         dataFileIndex += (tmp << 8*(3-i));
      }
      
      long timeStamp = 0;
      
      //Restore timestamp
      for(int i=0; i<8; i++)
      {
         tmp = (indexInfoBytes[arrayIndex++] + 256) & 0xFF;
         timeStamp += (tmp << 8*(7-i));
      }
      
      FileDBIndexInformation indexInfo = new FileDBIndexInformation(key, indexFileIndex, dataFileIndex, timeStamp);
      
      //Map key with indices
      this.wireKeysToIndices.put(key, indexInfo);
      
      return indexInfo;
   }
   
   
   
   /* ### INTERNAL UTILITY METHODS ### */
   
   
   
	/**
	 * Sorts the mappings in the wireKeysToIndices HashMap according to time stamp.
	 */
	private Map.Entry[] getIndexFileIndexSortedKeyToIndexMappings()
	{
		Map.Entry[] entries = (Map.Entry[])wireKeysToIndices.entrySet().toArray(new Map.Entry[]{});
		Arrays.sort(entries, LowLevelFileDB.indexFileIndexComparator);
		
		return entries;
	}
	
	/**
	 * Sorts the mappings in the wireKeysToIndices HashMap according to time stamp.
	 */
	private Map.Entry[] getTimeStamptSortedKeyToIndexMappings()
	{
		Map.Entry[] entries = (Map.Entry[])wireKeysToIndices.entrySet().toArray(new Map.Entry[]{});
		Arrays.sort(entries, LowLevelFileDB.timeStampComparator);
		
		return entries;
	}
			
	/**
    * Internal method to get the keys and items (byte[]) for a set of map entries.
	 */
	private Object[][] getAllItems(final Map.Entry[] entries) throws IOException
	{
		final IntList dataFileIndices = new IntList(entries.length);
		FileDBIndexInformation indexInfo;
		final Object[][] keysAndItems = new Object[entries.length][2];
				
		for(int i=0; i<entries.length; i++)
		{
			keysAndItems[i][0] = entries[i].getKey();
			indexInfo = (FileDBIndexInformation)wireKeysToIndices.get(keysAndItems[i][0]);
			
			if(indexInfo != null)
			{
				if(debugMode) JServerUtilities.logDebug(fullName, "GetMultipleItems - Getting item with key: " + entries[i].getKey() +". Data file index is: " + indexInfo.getDataFileIndex() + ".");
				dataFileIndices.add(indexInfo.getDataFileIndex());
			}
			else
			{
				if(debugMode) JServerUtilities.logDebug(fullName, "GetMultipleItems - Cannot get item with key: " + entries[i].getKey() +". Key NOT found.");
			}
		}
		
		if(dataFileIndices.size() > 0)
		{
			final byte[][] byteData = dataFile.getMultipleItemData(dataFileIndices.toArray());
			
			for(int i=0; i<byteData.length; i++)
			{
				keysAndItems[i][1] = byteData[i];
			}
			
			return keysAndItems;
		}
		else return new Object[0][0];
	}
	
	/**
    * Internal method to get the keys and items (objects) for a set of map entries.
	 */
	private Object[][] getAllItemsAsObjects(final Map.Entry[] entries) throws IOException, ClassNotFoundException
	{
		final Object[][] allData = this.getAllItems(entries);
		
		if(allData != null)
		{
			for(int i=0; i<allData.length; i++)
			{
				allData[i][1] = this.deSerializeObject((byte[])allData[i][1]); //Replace byte data with deserialized object
			}
			
			return allData;
		}
		else return new Object[0][0];
	}
			
	/**
    * Gets the keys for a set of map entries.
	 */
	private String[] getKeys(final Map.Entry[] entries)
	{
		String[] keys = new String[entries.length];
		
		for(int i=0; i<keys.length; i++)
		{
			keys[i] = (String)entries[i].getKey();
		}
				
		return keys;
	}
	
	/**
	 * Serializes an object.
	 */
	private byte[] serializeObject(final Object object) throws IOException
	{
		byteOutput.reset();
			
		//Write object
      objectOutput.writeObject(object);
      objectOutput.reset(); // Write reset code to make sure the objectInput clears strong references to objects
      objectOutput.flush();

		//Return the bytes of the object
		byte[] bytes = byteOutput.toByteArray();
			
		//Reset the object output stream to clear its stupid strong reference to the written object!
		resetObjectOutputStream();
			
		return bytes;
	}
	
	/**
	 * Resets the objet output stream used for object serialization.
	 */
	private void resetObjectOutputStream() throws IOException
	{
		//this.objectOutput.reset();
		
		if(isUsingAlternativeObjectOutputStreamResetMethod())
		{
			this.objectOutput = null;
			this.objectOutput = new NoHeadersObjectOutputStream(byteOutput);
		}
	}
   
   /**
    * Deserializes an object.
    */
   private Object deSerializeObject(final byte[] data) throws IOException, ClassNotFoundException
   {
      this.byteInput.setByteArray(data);
      
      Object object = this.deSerializeSingleObject(false);
      this.byteInput.reset();
      
      return object;
   }
   
   /**
    * Deserializes a number of objects.
    */
   private Object[] deSerializeObjects(final byte[] data, final boolean ignoreDeserializationError) throws IOException, ClassNotFoundException
   {
      this.byteInput.setByteArray(data);
      
      ArrayList objects = new ArrayList();
      Object object;
      while(this.byteInput.available() > 1 ) // If only one byte available - assume it's a reset code
      {
         object = this.deSerializeSingleObject(ignoreDeserializationError);
         if( object != SERIALIZATION_ERROR ) objects.add(object);
         else break;
      }
      this.byteInput.reset();
      
      return objects.toArray();
   }
	
	/**
	 * Deserializes an object.
	 */
	private Object deSerializeSingleObject(final boolean ignoreDeserializationError) throws IOException, ClassNotFoundException
	{
      try
      {
         return this.objectInput.readObject();
      }
      catch(Throwable t) 
      {
         // Reinitialize objectInput
         this.resetObjectDeserializer();
         
         if( !ignoreDeserializationError )
         {
            if( t instanceof IOException ) throw (IOException)t;
            else if( t instanceof ClassNotFoundException ) throw (ClassNotFoundException)t;
            else if( t instanceof Error ) throw (Error)t;
            else
            {
               IOException ioException = new IOException("Error deserializing object");
               ioException.initCause(t);
               throw ioException;
            }
         }
         else
         {
            JServerUtilities.logWarning(fullName, "Error occurred while deserializing object. Error ignored.", t);
            return SERIALIZATION_ERROR;
         }
      }
	}
}
