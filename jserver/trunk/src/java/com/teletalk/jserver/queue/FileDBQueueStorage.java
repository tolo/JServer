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
package com.teletalk.jserver.queue;

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.property.NumberProperty;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.property.StringProperty;
import com.teletalk.jserver.util.MutableByteArrayInputStream;
import com.teletalk.jserver.util.NoHeadersObjectInputStream;
import com.teletalk.jserver.util.NoHeadersObjectOutputStream;
import com.teletalk.jserver.util.filedb.LowLevelFileDB;

/**
 * This class implements a queue storage mechanism that uses a low level file database to store queue items. 
 * 
 * @see com.teletalk.jserver.queue.Queue
 * @see com.teletalk.jserver.util.filedb.LowLevelFileDB
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.02
 */
public final class FileDBQueueStorage extends SubComponent implements QueueStorage
{
   private static interface FileDBAction
   {
      public Object performAction() throws Exception;
   }
   
   
	/** The base name of data base files. */
	public static final String dbFileBaseName = "queueitems";
	
	private final static int MAX_ATTEMPTS = 3; //Maximum number of attempts that should be made to carry out a certain operation
	private final static int ATTEMPT_DELAY = 100; //Attempt delay in milliseconds
	
	//private LowLevelFileDB[] files;
	LowLevelFileDB fileDB; //Only one for now
	
	//Serialization
	private final int noOfSerializers = 10; //10 for now
	private int currentSerializer = 0;
	private final DataOutputStream dataOutput[] = new DataOutputStream[noOfSerializers];
	private final NoHeadersObjectOutputStream[] objectOutput = new NoHeadersObjectOutputStream[noOfSerializers]; 
	private final ByteArrayOutputStream[] byteOutput = new ByteArrayOutputStream[noOfSerializers];  
	
	//Deserialization
	private MutableByteArrayInputStream byteInput;
	private DataInputStream dataInput;
	private NoHeadersObjectInputStream objectInput;
	
	private StringProperty dbFilePath;
	private NumberProperty fileDBBlockSize;
	private NumberProperty fileDBNoOfDataBlocks;
	private NumberProperty fileDBNoOfIndexBlocks;
   private NumberProperty flushInterval;
	
	private boolean useAlternativeObjectOutputStreamResetMethod = false;
   
   private long writeFlushCounter = 0;
   
	/**
	 * Creates a new SimpleFileQueueStorage object.
	 * 
	 * @param parent the parent of this SimpleFileQueueStorage.
	 */
	public FileDBQueueStorage(SubComponent parent)
	{
		this(parent, "FileDBQueueStorage");
	}
	
	/**
	 * Creates a new SimpleFileQueueStorage object.
	 * 
	 * @param parent the parent of this SimpleFileQueueStorage.
	 * @param name the name of this SimpleFileQueueStorage.
	 */
	public FileDBQueueStorage(SubComponent parent, String name)
	{
		super(parent, name);
		
		dbFilePath = new StringProperty(this, "fileDatabaseDirectory", "." + File.separator + parent.getName() + ".storage" + File.separator, StringProperty.MODIFIABLE_OWNER_RESTART);
		dbFilePath.setDescription("The directory where the file database will be stored.");
		
		fileDBBlockSize = new NumberProperty(this, "dataFileBlockSize", 2048, NumberProperty.MODIFIABLE_OWNER_RESTART);
		fileDBBlockSize.setDescription("The block size of the data file of the file database.");
			
		fileDBNoOfDataBlocks = new NumberProperty(this, "dataFileInitialBlockCount", 1000, NumberProperty.MODIFIABLE_OWNER_RESTART);
		fileDBNoOfDataBlocks.setDescription("The initial number of blocks in the data file.");
		
		fileDBNoOfIndexBlocks = new NumberProperty(this, "indexFileInitialBlockCount", 1000, NumberProperty.MODIFIABLE_OWNER_RESTART);
		fileDBNoOfIndexBlocks.setDescription("The initial number of blocks in the index file.");
      
      flushInterval = new NumberProperty(this, "flushInterval", 100, NumberProperty.MODIFIABLE_NO_RESTART);
      flushInterval.setDescription("The file database flush interval.");
			
		addProperty(fileDBBlockSize);
		addProperty(fileDBNoOfDataBlocks);
		addProperty(fileDBNoOfIndexBlocks);
		addProperty(dbFilePath);
      addProperty(flushInterval);
		
		//Set alternative reset method if the version of the VM is 1.3
		if(com.teletalk.jserver.util.JavaBugUtils.isUsingJava1_3_0())
		{
			this.setUseAlternativeObjectOutputStreamResetMethod(true);
			logInfo("Defaulting flag for alternative object output stream reset method to true, because Java 2 version 1.3 is used.");
		}
	}
	
	/**
	 * Enables this FileDBQueueStorage by creating the storage directory and the LowLevelFileDB object.
	 */
	public void doInitialize()
	{
		super.doInitialize();
      
      // Attempt to get old property "file database directory"
      super.initFromConfiguredProperty(this.dbFilePath, "file database directory", false, true);
      // Attempt to get old property "data file block size"
      super.initFromConfiguredProperty(this.fileDBBlockSize, "data file block size", false, true);
      // Attempt to get old property "data file initial block count"
      super.initFromConfiguredProperty(this.fileDBNoOfDataBlocks, "data file initial block count", false, true);
      // Attempt to get old property "index file initial block count"
      super.initFromConfiguredProperty(this.fileDBNoOfIndexBlocks, "index file initial block count", false, true);
      
      // Attempt to get old property "data file block size (bytes)" to be used if "data file block size" property is missing
      super.initFromConfiguredProperty(this.fileDBBlockSize, "data file block size (bytes)", false, true);
		
		if(this.isUsingAlternativeObjectOutputStreamResetMethod())
			logWarning("Note: this FileDBQueueStorage uses an alternative method for resetting the object output streams used when writing QueueItems to disk. This alternative method may be inefficient.");
		
		new File(dbFilePath.stringValue()).mkdirs();
		
		try
		{
			//Initialize streams needed for object serialization
			for(int i=0; i<noOfSerializers; i++)
			{
				this.byteOutput[i] = new ByteArrayOutputStream();
				this.dataOutput[i] = new DataOutputStream(byteOutput[i]);
				this.objectOutput[i] = new NoHeadersObjectOutputStream(byteOutput[i]);
			}
			
			//Initialize streams needed for object deserialization
			this.byteInput = new MutableByteArrayInputStream();
			this.dataInput = new DataInputStream(byteInput);
			this.objectInput = new NoHeadersObjectInputStream(byteInput);
		}
		catch(Exception e)
		{
			logError("Error occurred while trying to create streams needed for object serialization!", e);
			shutDown();
		}
		
		boolean done = false;
		Exception error = null;
		
		for(int attempt = 0; !done && (attempt <MAX_ATTEMPTS); attempt++)
		{
			try
			{
            this.fileDB = new LowLevelFileDB(this.getFullName() + ".LowLevelFileDB", dbFilePath.stringValue() +dbFileBaseName, fileDBBlockSize.intValue(), fileDBNoOfDataBlocks.intValue(), fileDBNoOfIndexBlocks.intValue(), LowLevelFileDB.READ_WRITE_MODE);
				done = true;
				error = null;
			}
			catch(Exception e)
			{
				done = false;
				error = e;
				if(attempt < (MAX_ATTEMPTS-1)) //Check if this is not the last attempt
            {
					logError("Error while creating file database - Retrying (attempt " + (attempt + 1) + " of " + MAX_ATTEMPTS + ").", e);
            }
				
				try{
					Thread.sleep(ATTEMPT_DELAY);
				}catch(InterruptedException ie){}
			}	
		}
		
		if(error != null)
		{
			logError("All attempts to create file database failed! Last exception is: " + error + ". Disabling FileDBQueueStorage object.");
			super.error("Failed to create file database - " + error + ".");
		}
		else
		{
         this.fileDB.setDebugMode(super.isDebugMode());
			
			// Set the actual value for data file block size
			this.fileDBBlockSize.setNotificationMode(false);
			this.fileDBBlockSize.setForceMode(true);
			this.fileDBBlockSize.setValue( this.fileDB.getDataFileBlockSize() );
			this.fileDBBlockSize.setForceMode(false);
			this.fileDBBlockSize.setNotificationMode(true);
		}
	}
   
	/**
	 * Disables this FileDBQueueStorage by closing the file database.
	 */
	public void doShutDown()
	{
		super.doShutDown();
		
		try
		{
			for(int i=0; i<noOfSerializers; i++)
			{
				byteOutput[i] = null;
				dataOutput[i] = null;
				objectOutput[i] = null;
			}
			
			this.byteInput = null;
			this.dataInput = null;
			this.objectInput = null;
		}
		catch(Exception e)
		{
			
		}
		
		if(fileDB != null) fileDB.closeFileDB();
		fileDB = null;
	}

	/**
	 * Called when the value of a property has been modified. This implementation makes sure that 
	 * the QueueItem savepath property ends with the system-dependent default name-separator character (File.separator).
	 * 
	 * @param property the Property that was changed.
	 */
	public void propertyModified(Property property)
	{
		if(property == dbFilePath)
		{
			String value = dbFilePath.stringValue(); 
			
			if(!(value.endsWith("/") || value.endsWith("\\")))
			{
				dbFilePath.setNotificationMode(false);
				dbFilePath.setValue(value + File.separator);
				dbFilePath.setNotificationMode(true);
			}
		}
		else if(property == super.logLevel)
		{
			if(fileDB != null) fileDB.setDebugMode(super.isDebugMode()); //fileDB.setDebugMode(super.logLevel.intValue() == LogMessage.DEBUG_LEVEL);
		}
		
		super.propertyModified(property);
	}
	
	/**
	 * Validates a modification of a property's value. This implementation validates 
	 * the QueueItem savepath property (checkes if it is a directory).
	 * 
	 * @param property The property to be validated.
	 * 
	 * @return boolean value indicating if the property passed (true) validation or not (false).
	 */
	public boolean validatePropertyModification(Property property)
	{
		if(property == dbFilePath)
		{
			String value = dbFilePath.stringValue();
		
			if(!(value.endsWith("/") || value.endsWith("\\")))
				value = value + File.separator;
		
			File f = new File(value);
		
			return f.isDirectory() || f.mkdirs();
		}
		else if(property == fileDBBlockSize)
		{
			int value = fileDBBlockSize.intValue();
			
			return (value > 0) && (value < (10*1024*1024));
		}
		else if(property == fileDBNoOfDataBlocks)
		{
			int value = fileDBNoOfDataBlocks.intValue();
			
			return (value >= 0) && (value < (1000*1000));
		}
		else if(property == fileDBNoOfIndexBlocks)
		{
			int value = fileDBNoOfIndexBlocks.intValue();
			
			return (value >= 0) && (value < (1000*1000));
		}
		else return super.validatePropertyModification(property);
	}
	
	/**
	 * Sets the file db file path for the LowLevelFileDB used by this FileDBQueueStorage.
	 */
	public void setDbFilePath(final String dbFilePath)
	{
		this.dbFilePath.setValue(dbFilePath);
	}
	
	/**
	 * Gets the file db file path for the LowLevelFileDB used by this FileDBQueueStorage.
	 */
	public String getDbFilePath()
	{
		return this.dbFilePath.stringValue();
	}
	
	/**
	 * Sets the file db block size for the LowLevelFileDB used by this FileDBQueueStorage.
	 */
	public void setFileDBBlockSize(final int fileDBBlockSize)
	{
		this.fileDBBlockSize.setValue(fileDBBlockSize);
	}
	
	/**
	 * Gets the file db block size for the LowLevelFileDB used by this FileDBQueueStorage.
	 */
	public int getFileDBBlockSize()
	{
		return this.fileDBBlockSize.intValue();
	}
	
	/**
	 * Sets the file db data block count for the LowLevelFileDB used by this FileDBQueueStorage.
	 */
	public void setFileDBNoOfDataBlocks(final int fileDBNoOfDataBlocks)
	{
		this.fileDBNoOfDataBlocks.setValue(fileDBNoOfDataBlocks);
	}
	
	/**
	 * Gets the file db data block count for the LowLevelFileDB used by this FileDBQueueStorage.
	 */
	public int getFileDBNoOfDataBlocks()
	{
		return this.fileDBNoOfDataBlocks.intValue();
	}
	
	/**
	 * Sets the file db index block count for the LowLevelFileDB used by this FileDBQueueStorage.
	 */
	public void setFileDBNoOfIndexBlocks(final int fileDBNoOfIndexBlocks)
	{
		this.fileDBNoOfIndexBlocks.setValue(fileDBNoOfIndexBlocks);
	}
	
	/**
	 * Gets the file db index block count for the LowLevelFileDB used by this FileDBQueueStorage.
	 */
	public int getFileDBNoOfIndexBlocks()
	{
		return this.fileDBNoOfIndexBlocks.intValue();
	}
		
	/**
	 * Sets the flag indicating if an alternative method should be used when resetting the object output streams 
	 * used when saving QueueItems to disk. This alternative reset method involvs creating a 
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
	 * used when saving QueueItems to disk. This alternative reset method involvs creating a 
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
    * Performs an operation on the file database. Up to MAX_ATTEMPTS number of attempts will be made. 
    */
   private Object performFileDBAction(final FileDBAction fileDBAction, final String actionDescription, final boolean flush) throws QueueStorageException
   {
      if( super.getStatus() == ERROR )
      {
         logInfo("File database in state ERROR - attempting to reopen.");
         if( !this.engage() )
         {
            logError("Failed to reopen file database!");
            try{
            Thread.sleep(1000);
            }catch (Exception e){}
            throw new QueueStorageException("Failed to reopen file database!");
         }
      }
      else if(!this.isEnabled()) throw new QueueStorageException("FileDBQueueStorage not enabled!");
      
      Exception error = null;
      
      for(int attempt=1; attempt <= MAX_ATTEMPTS; attempt++)
      {
         try
         {
            Object result = fileDBAction.performAction();
            synchronized(this.fileDB.getLock())
            {
               if( flush && ((++writeFlushCounter) % this.flushInterval.longValue()) == 0 )
               {
                  this.fileDB.flush();
               }
            }
            return result;
         }
         catch(Exception e)
         {
            error = e;
            
            if( attempt == (MAX_ATTEMPTS-1) ) // If attempt before last attempt...
            {
               logError("Error while attempting to " + actionDescription + " - Retrying (attempt " + (attempt) + " of " + MAX_ATTEMPTS + "). Reopening file database.", e);
               // try to reopen the file db
               this.shutDown();
               this.engage();
            }
            else if(attempt < (MAX_ATTEMPTS)) // Check if this is not the last attempt (i.e. don't log this message for the last error)
            {
               logError("Error while attempting to " + actionDescription + " - Retrying (attempt " + (attempt) + " of " + MAX_ATTEMPTS + ").", e);
            }
            
            try{
               Thread.sleep(ATTEMPT_DELAY);
            }catch(InterruptedException ie){}
         }
      }
      
      logError("All attempts to " + actionDescription + " failed! Last exception is: " + error + ".");
      throw new QueueStorageException("All attempts to " + actionDescription + " failed!", error);
   }
   
	/**
	 * Adds a QueueItem to persistant storage. This method stores each QueueItem in a file database.
	 * 
	 * @param item the QueueItem to be stored.
	 * 
	 * @exception QueueStorageException if an error occured during the store operation.
	 */
	public void storeQueueItem(final QueueItem item) throws QueueStorageException
	{
      this.performFileDBAction(new FileDBAction(){
         public Object performAction() throws Exception
         {
            fileDB.insertItem(item.getId(), serializeObject(item), item.getSendReceiveTime());
            return null;
         }
      }, "store QueueItem '" + item + "'", true);
	}
	
	/**
	 * Adds several QueueItems to persistant storage. This method stores each QueueItem in a file database.
	 * 
	 * @param items the QueueItems to be stored.
	 * 
	 * @exception QueueStorageException if an error occured during the store operation.
	 */
	public void storeQueueItems(final QueueItem[] items) throws QueueStorageException
	{
      final String[] keys = new String[items.length];
      for(int i=0; i<items.length; i++)
      {
         keys[i] = items[i].getId();
      }
      
      this.performFileDBAction(new FileDBAction(){
         public Object performAction() throws Exception
         {
            final byte[][] itemData = new byte[items.length][];
            final long[] timeStamps = new long[items.length];
            
            for(int i=0; i<items.length; i++)
            {
               itemData[i] = serializeObject(items[i]);
               timeStamps[i] = items[i].getSendReceiveTime();
            }
            
            fileDB.insertMultipleItems(keys, itemData, timeStamps);
            //fileDB.flush();
            return null;
         }
      }, "store QueueItems (" + QueueItem.concatIds(items) + ")", true);
	}
	
	byte[] serializeObject(QueueItem item) throws IOException
	{
		int whichSerializer;
			
		synchronized(byteOutput)
		{
			if(currentSerializer >= noOfSerializers)
				currentSerializer = 0;
			
			whichSerializer = currentSerializer++;
		}
			
		synchronized(byteOutput[whichSerializer])
		{
			byteOutput[whichSerializer].reset();
			
			//Write status
			dataOutput[whichSerializer].writeShort(item.getStatus());
						
			//Write QueueItem object
			objectOutput[whichSerializer].writeObject(item);
			objectOutput[whichSerializer].flush();

			//Return the bytes of the status short and the QueueItem object
			byte[] bytes = byteOutput[whichSerializer].toByteArray();
			
			//Reset the object output stream to clear its stupid strong reference to the written QueueItem object!
			resetObjectOutputStream(whichSerializer);
			
			return bytes;
		}
	}
	
	private void resetObjectOutputStream(int whichSerializer) throws IOException
	{
		objectOutput[whichSerializer].reset();
		
		if(isUsingAlternativeObjectOutputStreamResetMethod())
		{
			objectOutput[whichSerializer] = null;
			objectOutput[whichSerializer] = new NoHeadersObjectOutputStream(byteOutput[whichSerializer]);
		}
	}
	
	/**
	 * Updates the persistent state of a previously stored QueueItem.
	 * 
	 * @param item the QueueItem to be updated.
	 * 
	 * @exception QueueStorageException if an error occured during the update operation.
	 */
	public void updateStoredQueueItem(final QueueItem item)  throws QueueStorageException
	{
      this.performFileDBAction(new FileDBAction(){
         public Object performAction() throws Exception
         {
            fileDB.updateItem(item.getId(), serializeObject(item));
            return null;
         }
      }, "update QueueItem '" + item + "'", true);
	}
		
	/**
	 * Method to reflect a change in the state (status value) of a QueueItem object on it's persistent counterpart. This method updates the file used to store the 
	 * specified QueueItem.
	 * 
	 * @param item the QueueItem to be stored.
	 * 
	 * @exception QueueStorageException if an error occured during the store operation.
	 */
	public void updateQueueItemStatus(final QueueItem item) throws QueueStorageException
	{
      this.performFileDBAction(new FileDBAction(){
         public Object performAction() throws Exception
         {
            final byte[] statusBytes = new byte[2];
            final int status = item.getStatus();
            
            //Convert status to bytes (2 bytes for a short)
            statusBytes[0] = (byte)((status >>> 8));
            statusBytes[1] = (byte)((status >>> 0));

            fileDB.updatePartialItem(item.getId(), statusBytes, 0);
            //fileDB.flush();
            return null;
         }
      }, "update status for QueueItem '" + item + "'", true);
	}
	
	/**
	 * Removes a QueueItem from persistant storage.
	 * 
	 * @param item the QueueItem to be removed from persistent storage.
	 */
	public void removeStoredQueueItem(final QueueItem item)
	{
      this.performFileDBAction(new FileDBAction(){
         public Object performAction() throws Exception
         {
            fileDB.deleteItem(item.getId());
            //fileDB.flush();
            return null;
         }
      }, "remove QueueItem '" + item + "'", true);
	}
	
	/**
	 * Restores all stored QueueItem objects.
	 * 
	 * @return a vector containing QueueItem objects restored from persistent storage.
	 * 
	 * @exception QueueStorageException if an error occured during restoration of the stored QueueItem objects.
	 */
	public List restoreQueueFromStorage() throws QueueStorageException
	{
      Object[][] queueItems = (Object[][])this.performFileDBAction(new FileDBAction(){
         public Object performAction() throws Exception
         {
            return fileDB.getAllItemsTimeStampOrdered();
         }
      }, "get items from file database", false);
      		
		//Restore items
      ArrayList restoredQueueItems = new ArrayList();
      QueueItem item = null;
      
		for(int i=0; i<queueItems.length; i++)
		{
			try
			{
				item = restoreQueueItemQueueItem((String)queueItems[i][0], (byte[])queueItems[i][1]);
				queueItems[i][0] = null;
				queueItems[i][1] = null;

				if(item != null)
				{
					restoredQueueItems.add(item);
					item.setRecoveredFromPersistentStorage(true);
				}
			}
			catch(Exception e)
			{
				logError("Unable to restore QueueItem with id '" + queueItems[i][0] + ". Removing item from file db.", e);
				try
				{
					fileDB.deleteItem((String)queueItems[i][0]);
               //fileDB.flush();
				}
				catch(Exception ex)
				{
					logError("Error occurred while deleting bad item " + queueItems[i][0] + ".", ex);
				}
			}
		}
		queueItems = null;

		return restoredQueueItems;
	}
	
	/**
	 */
	private QueueItem restoreQueueItemQueueItem(final String queueItemId, final byte[] itemData) throws Exception
	{
		this.byteInput.setByteArray(itemData);

		this.byteInput.setReturnObjectStreamResetCode(false);
		final short statusShort = this.dataInput.readShort();
		this.byteInput.setReturnObjectStreamResetCode(true);
		final QueueItem qItem = (QueueItem)this.objectInput.readObject();

		if(QueueItem.validateStatusValue(statusShort))
      {
			qItem.forceStatus(statusShort);
      }
		else
      {
			logWarning("Invalid status value detected while restoring QueueItem with id '" +queueItemId + "'. The read status value was " + statusShort + ".");
      }
		
		return qItem;
	}
}
