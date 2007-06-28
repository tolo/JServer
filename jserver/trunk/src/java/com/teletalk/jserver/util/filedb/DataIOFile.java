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

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Adapter class that adapts a RandomAccessFile to the DataIO interface.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1
 */
public class DataIOFile extends RandomAccessFile implements DataIO
{
   private final File file;
	private final boolean newFile;
	private final boolean readOnly;
	private final String fileName;
	
	/**
	 * Creates a new DataIOFile.
	 * 
	 * @param file the File object to be used when creating the RandomAccessFile.
	 * @param mode the file access mode (see {@link FileDBConstants} for details).
	 */
	public DataIOFile(File file, String mode) throws IOException
	{
		super(file, mode.trim());
      
      this.file = file;
		this.newFile = !file.exists();
      this.readOnly = mode.trim().equalsIgnoreCase(FileDBConstants.READ_ONLY_MODE);
      this.fileName = file.getName();
	}
	
	/**
	 * Creates a new DataIOFile.
	 * 
	 * @param name the file name to be used when creating the RandomAccessFile.
	 * @param mode the file access mode (see {@link FileDBConstants} for details).
	 */
	public DataIOFile(String name, String mode) throws IOException
	{
		super(name, mode.trim());
      
      this.file = new File(name);
		this.newFile = !(new File(name).exists());
      this.readOnly = mode.trim().equalsIgnoreCase(FileDBConstants.READ_ONLY_MODE);
      this.fileName = name;
	}
   
   /**
    * Flushes any buffered data to the underlying file.
    * 
    * @since 2.1.3 (20060330)
    */
   public void flush() throws IOException
   {
      super.getFD().sync();
   }
   
   /**
    * Gets the time that the underlying file used by this object was last modified. 
    * 
    * @since 2.1.3 (20060330)
    */
   public long getLastModified()
   {
      return this.file.lastModified();
   }
	
	/**
	 * Sets the current index of the file pointer. This method only calls <code>seek(long)</code> in the 
	 * super class.
	 * 
	 * @param pos the new index of the file pointer.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void setFilePointer(final long pos) throws IOException
	{
		super.seek(pos);
	}
	
	/**
	 * Gets the file name of this DataIOFile.
	 * 
	 * @return the file name of this DataIOFile.
	 */
	public String getFileName()
	{
		return fileName;
	}
	
	/**
	 * Gets the file name of this DataIOFile.
	 * 
	 * @return the file name of this DataIOFile.
	 */
	public String getName()
	{
		return fileName;
	}
	
	/**
	 * Checks if this DataIOFile is new, i.e. if the underlying file didn't exist before this 
	 * DataIOFile object was created.
	 * 
	 * @return <code>true</code> if this DataIOFile is new, otherwise <code>false</code>.
	 */
	public boolean isNew()
	{
		return newFile;
	}
	
	/**
	 * Checks if this DataIOFile is read only.
	 * 
	 * @return <code>true</code> if this DataIOFile is read only, otherwise <code>false</code>.
	 */
	public boolean isReadOnly()
	{
		return readOnly;
	}
	
	/**
	 * Gets a string representation of this DataIOFile.
	 * 
	 * @return a string representation of this DataIOFile.
	 */
	public String toString()
	{
		return "DataIOFile(" + this.fileName + ")";
	}
}
