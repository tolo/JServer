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

/**
 * This class contains information about the index file index, data file index and time stamp of  
 * items stored in the LowLevelFileDB. In LowLevelFileDB, keys are mapped to instances of this class.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.02
 * 
 * @see LowLevelFileDB
 */
public final class FileDBIndexInformation
{
   private final String key;
	private final int indexFileIndex;
	private final int dataFileIndex;
	private long timeStamp;
	
	/**
	 * Creates a new FileDBIndexInformation.
	 * 
	 * @param indexFileIndex the index in the index file.
	 * @param dataFileIndex the index in the data file.
	 * @param timeStamp the time stamp in milliseconds.
	 */
	public FileDBIndexInformation(String key, int indexFileIndex, int dataFileIndex, long timeStamp)
	{
      this.key = key;
		this.indexFileIndex = indexFileIndex;
		this.dataFileIndex = dataFileIndex;
		this.timeStamp = timeStamp;
	}
   
	/**
    * Gets the key.
	 */
   public String getKey()
   {
      return key;
   }

   /**
	 * Gets the index file index.
	 * 
	 * @return the index file index.
	 */
	public int getIndexFileIndex()
	{
		return indexFileIndex;
	}
	
	/**
	 * Gets the data file index.
	 * 
	 * @return the data file index.
	 */
	public int getDataFileIndex()
	{
		return dataFileIndex;
	}
	
	/**
	 * Gets the time stamp.
	 * 
	 * @return the time stamp in milliseconds.
	 */
	public long getTimeStamp()
	{
		return timeStamp;
	}
   
   /**
    * Sets the time stamp.
    */   
   public void setTimeStamp(long timeStamp)
   {
      this.timeStamp = timeStamp;
   }
	
	/**
	 * Compares this object with another.
	 * 
	 * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the object specified by parameter <code>o</code>.
	 */
	public int compareTo(Object o)
	{
		return (int)(this.timeStamp - ((FileDBIndexInformation)o).timeStamp);
	}
}
