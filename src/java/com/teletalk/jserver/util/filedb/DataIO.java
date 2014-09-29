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

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

/**
 * Interface for a random access data storage.
 *  
 * @author Tobias Löfstrand
 * 
 * @since 1.1
 */
public interface DataIO extends DataInput, DataOutput
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
	 * @param pos the new index of the file pointer.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void setFilePointer(long pos) throws IOException;
	
	/**
	 * Gets the name of this DataIO.
	 * 
	 * @return the name as a string.
	 */
	public String getName();
	
	/**
	 * Checks if this DataIO is new, i.e. if the underlying data storage was created when this 
	 * DataIO was created.
	 * 
	 * @return <code>true</code> if this DataIO is new, otherwise <code>false</code>.
	 */
	public boolean isNew();
	
	/**
	 * Checks if this DataIO is read only.
	 * 
	 * @return <code>true</code> if this DataIO is read only, otherwise <code>false</code>.
	 */
	public boolean isReadOnly();
	
	/**
	 * Gets the length of this DataIO, i.e. the number of bytes it contains.
	 * 
	 * @return the length of this DataIO, i.e. the number of bytes it contains.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public long length() throws IOException;
			
	/**
	 * Sets the length of this DataIO, i.e. the number of bytes it should be able to contains.
	 * 
	 * @param newLength the new length of this DataIO.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void setLength(long newLength) throws IOException;

	/**
	 * Closes this DataIO.
	 * 
	 * @exception IOException if an I/O error occurs.
	 */
	public void close() throws IOException;
}
