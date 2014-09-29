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
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.teletalk.jserver.util.NoHeadersObjectOutputStream;

/**
 * Utility class used by SimpleFileQueueStorage to store and update QueueItems.
 * 
 * @see com.teletalk.jserver.queue.SimpleFileQueueStorage
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public final class SimpleFileQueueStorageFileWriter
{
	private final ByteArrayOutputStream byteOutput;
	private NoHeadersObjectOutputStream objectOutput; 
	
	private final static int maxAttempts = 3; //Maximum number of attempts that should be made to carry out a certain operation
	private final static int attemptDelay = 100; //Attempt delay in milliseconds
	
	private SimpleFileQueueStorage parent;
	
	private final String fullName;
	
	/**
	 * Creates a new SimpleFileQueueStorageFileWriter.
	 * 
	 * @exception  IOException if an error occurred while creating necessary.
	 */
	public SimpleFileQueueStorageFileWriter(SimpleFileQueueStorage parent) throws IOException
	{
		this.parent = parent;
		fullName = parent.getFullName() + "SimpleFileQueueStorageFileWriter";
		
		this.byteOutput = new ByteArrayOutputStream();
		this.objectOutput = new NoHeadersObjectOutputStream(byteOutput);
	}
	
	/**
	 * Stores a QueueItem as a single file.
	 * 
	 * @param item a QueueItem to be stored.
	 * @param fileName the name of the file used to store the QueueItem.
	 * 
	 * @exception IOException if an error occurs while opening the output file, or when writing to it.
	 */
	public void store(final QueueItem item, final String fileName) throws IOException
	{
		boolean done = false;
		IOException error = null;
		
		File queueItemFile = new File(fileName);
		File oldQueueItemFile = null;
		
		if(queueItemFile.exists())
		{
			oldQueueItemFile = new File(fileName + SimpleFileQueueStorage.oldItemFileSuffix);
			queueItemFile.renameTo(oldQueueItemFile);
		}
		
		for(int attempt = 0; !done && (attempt <maxAttempts); attempt++)
		{
			try
			{
				doStore(item, fileName);
				done = true;
				error = null;
			}
			catch(IOException e)
			{
				done = false;
				error = e;
				parent.logError(fullName, "Error while storing QueueItem (" + item.toString() + ") to '" + fileName + "' - Retrying (attempt " + (attempt + 1) + " of " + maxAttempts + "). Exception is: " + e);
				
				try
				{
					Thread.sleep(attemptDelay);
				}catch(InterruptedException ie){}
			}
		}
		
		if(error != null)
		{
			if(oldQueueItemFile != null)
				oldQueueItemFile.renameTo(queueItemFile);
			throw error;
		}
		else
		{
			if(oldQueueItemFile != null)
				oldQueueItemFile.delete();
		}
	}
	
	private void doStore(final QueueItem item, final String fileName) throws IOException
	{
		RandomAccessFile queueItemFile = null;
		
		try
		{
			queueItemFile = new RandomAccessFile(fileName, "rw");
			
			byteOutput.reset();
			
			//this.objectOutput = new ObjectOutputStream(byteOutput);
			objectOutput.writeObject(item);
			objectOutput.flush();

			queueItemFile.writeShort(item.getStatus());

			queueItemFile.write(byteOutput.toByteArray());
			//If overwriting...
			queueItemFile.setLength(byteOutput.size() + 2); //2 for the status short...

			resetObjectOutputStream();
		}
		finally
		{
			if(queueItemFile != null)
			{
				try
				{
					queueItemFile.close();
				}catch(Exception ex1){}
			}
			queueItemFile = null;
		
			//objectOutput = null;
		}
	}
	
	private void resetObjectOutputStream() throws IOException
	{
		objectOutput.reset();
		
		if(parent.isUsingAlternativeObjectOutputStreamResetMethod())
		{
			objectOutput = null;
			objectOutput = new NoHeadersObjectOutputStream(byteOutput);
		}
	}
	
	/**
	 * Updates a stored QueueItem.
	 * 
	 * @param item a QueueItem to be updated.
	 * @param fileName the name of the file used to update the QueueItem.
	 * 
	 * @exception IOException if an error occurs while opening the output file, or when writing to it.
	 */
	public void updateStatus(QueueItem item, final String fileName) throws IOException
	{
		boolean done = false;
		IOException error = null;
		
		File queueItemFile = new File(fileName);
		
		if(queueItemFile.exists())
		{
			for(int attempt = 0; !done && (attempt <maxAttempts); attempt++)
			{
				try
				{
					doUpdateStatus(item, fileName);
					done = true;
					error = null;
				}
				catch(IOException e)
				{
					done = false;
					error = e;
					parent.logError(fullName, "Error while updating QueueItem (" + item.toString() + ") to '" + fileName + "' - Retrying (attempt " + (attempt + 1) + " of " + maxAttempts + "). Exception is: " + e);
					
					try
					{
						Thread.sleep(attemptDelay);
					}catch(InterruptedException ie){}
				}
			}
		
			if(error != null)
				throw error;
		}
		else
		{
			parent.logWarning("Trying to update status of QueueItem " + item + ", but no persistent version of the object exists. Storing item.");
			store(item, fileName);
		}
	}
	
	private void doUpdateStatus(QueueItem item, final String fileName) throws IOException
	{
		RandomAccessFile queueItemFile = null;
		
		try
		{
			queueItemFile = new RandomAccessFile(fileName, "rw");
			queueItemFile.writeShort(item.getStatus());
		}
		finally
		{
			if(queueItemFile != null)
			{
				try
				{
					queueItemFile.close();
				}catch(Exception ex1){}
			}
			queueItemFile = null;
		}
	}
	
	/**
	 * Validates this SimpleFileQueueStorageFileWriter.
	 * 
	 * @return true if this SimpleFileQueueStorageFileWriter was ok, otherwise false.
	 */
	public boolean validate() 
	{
		return (byteOutput != null);
	}
	
	/**
	 * Destroys this SimpleFileQueueStorageFileWriter by closing the streams.
	 */
	public void destroy() 
	{
		try
		{
			this.objectOutput.close();
		}
		catch(Exception e){}
		try
		{
			this.byteOutput.close();
		}
		catch(Exception e){}
	}
}
