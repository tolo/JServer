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

import java.io.File;

/**
 * Class for deleting files and subdirectories in a separate thread.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0 (20041223)
 */
public class FileDeletor extends Thread
{
   public static final long DEFAULT_DELETE_RETRY_TIMEOUT = 60000;
   
   
	private final String fileName;
   
   private final long deleteRetryTimeout;
		
   /**
    * Creates a new FileDeletor. Note that the methos <code>start()</code> must be called to initiate the deletion.
    */
   public FileDeletor(final File file)
   {
      this(file, DEFAULT_DELETE_RETRY_TIMEOUT);
   }
   
	/**
	 * Creates a new FileDeletor. Note that the methos <code>start()</code> must be called to initiate the deletion.
	 */
	public FileDeletor(final File file, final long deleteRetryTimeout)
	{
		this(file.getAbsolutePath(), deleteRetryTimeout);
	}
   
   /**
    * Creates a new FileDeletor. Note that the methos <code>start()</code> must be called to initiate the deletion.
    */
   public FileDeletor(final String fileName)
   {
      this(fileName, DEFAULT_DELETE_RETRY_TIMEOUT);
   }
	
	/**
	 * Creates a new FileDeletor. Note that the methos <code>start()</code> must be called to initiate the deletion.
	 */
	public FileDeletor(final String fileName, final long deleteRetryTimeout)
	{
		super("FileDeletorThread(" + fileName + ")");
	
		super.setDaemon(true);
				
		this.fileName= fileName;
      this.deleteRetryTimeout = deleteRetryTimeout;
	}
	
	/**
	 * Deletes the specified file or directory and any sub directories.  
	 */
	public static void delete(final String fileName)
	{
		delete(fileName, true, DEFAULT_DELETE_RETRY_TIMEOUT);
	}
   
   /**
    * Deletes the specified file or directory and any sub directories.  
    */
   public static boolean delete(final String fileName, final boolean asynch, final long retryTimeout)
   {
      if( asynch )
      {
         new FileDeletor(fileName, retryTimeout).start();
         return true;
      }
      else return deleteTree(fileName, retryTimeout);
   }
	
	/**
	 * Deletes the specified file or directory and any sub directories.
	 */
	public static void delete(final File file)
	{
      delete(file, true, DEFAULT_DELETE_RETRY_TIMEOUT);
	}
   
   /**
    * Deletes the specified file or directory and any sub directories.
    */
   public static boolean delete(final File file, final boolean asynch, final long retryTimeout)
   {
      if( asynch )
      {
         new FileDeletor(file, retryTimeout).start();
         return true;
      }
      else return deleteTree(file.getAbsolutePath(), retryTimeout);
   }
		
	/**
	 * The thread method of the FileDeletor. Deletes the file.
	 */
	public void run()
	{
      Thread.yield();
		
		deleteTree(this.fileName, this.deleteRetryTimeout);
	}
		
	/**
	 * Deletes a tree of files.
	 */
	private static boolean deleteTree(final String dirName, final long retryTimeout)
	{
      boolean deleteSuccess = true;
      
		File dir = new File(dirName);

		if( dir.isDirectory() )
		{
			File file;
				
			String[] dirContents = dir.list();
				
			for(int i=0; i<dirContents.length; i++)
			{
				file = new File(dir.getAbsolutePath() + File.separator + dirContents[i]);
				if(file.isDirectory()) deleteSuccess &= deleteTree(file.getAbsolutePath(), retryTimeout);
				else deleteSuccess &= deleteFile(file.getAbsolutePath(), retryTimeout);
				file = null;
			}
		}
			
		return deleteSuccess &= deleteFile(dirName, retryTimeout);
	}
				
	/**
	 * Deletes a file.
	 */
	private static boolean deleteFile(final String fileName, final long retryTimeout)
	{
		boolean deleteSuccess = false;
		File file;
		final int retryIterations = (retryTimeout > 0) ? 60 : 1; 
      
		for(int i=0; ((i<retryIterations) && (!deleteSuccess)); i++)
		{
			file = new File(fileName);
				
			if(!file.exists()) deleteSuccess = true;
			else deleteSuccess = file.delete();
			
			file = null;
				
			if( !deleteSuccess && (retryTimeout > 0) )
			{
				try{
				Thread.sleep(retryTimeout / retryIterations);
				}catch(InterruptedException ie){}
			}
		}
      
      return deleteSuccess;
	}
}
