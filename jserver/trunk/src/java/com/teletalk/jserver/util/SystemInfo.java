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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

/**
 * This class contains methods for accessing information about the file system.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.13 Build 600
 */
public final class SystemInfo
{
	/** 
	 * Flag indicating if the necessary native library has been successfully loaded. If this flag is <code>false</code> it will not be possible to get useful information from 
	 * any of the methods of this class.
	 */
	public static final boolean libraryLoaded;
	
	private static final String SystemInfoLibraryName = "SystemInfo";
	private static final String SystemInfoWin32LibraryName = SystemInfoLibraryName + ".dll";
	
	static
	{
		boolean loadSuccess = false;
		
		try
		{
			// Attempt to load library
			System.loadLibrary(SystemInfoLibraryName);
			loadSuccess = true;
		}
		catch(Exception e){}
		catch(UnsatisfiedLinkError err){}
				
		if(!loadSuccess)
		{
			// Attempt to get FileSystemInfo.dll as a resource
			try
			{
				final BufferedInputStream input = new BufferedInputStream(SystemInfo.class.getResourceAsStream("/" + SystemInfoWin32LibraryName));
				final BufferedOutputStream output = new BufferedOutputStream(new FileOutputStream(SystemInfoWin32LibraryName));
				
				int read = 0;
				final byte[] buffer = new byte[1024];
				
				while( read > -1 )
				{
					read = input.read(buffer);
					if( read > -1 )
					{
						output.write(buffer, 0, read);
					}
				} 
				input.close();
				output.flush();
				output.close();
				
				// Attempt to load library
				System.loadLibrary(SystemInfoLibraryName);
				loadSuccess = true;
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			catch(UnsatisfiedLinkError err)
			{
				err.printStackTrace();
			}
		}
		
		libraryLoaded = loadSuccess;
	}
	
	/**
	 * Gets free disk space for the disk indentified by the specified path.
	 * 
	 * @param path a path of the disk to get free space for (for instance <code>C:\</code>).
	 * 
	 * @return the free disk space in bytes, or -1 if free disk space could not be gotten. 
	 * This method also returns -1 if the necessary native library could not be loaded.
	 */
	public static long getDiskFreeSpace(final String path)
	{
		if( libraryLoaded )
		{
			return getDiskFreeSpaceNative(path);
		}
		else return -1;
	}
	
	/**
	 * Native method to get free disk space.
	 */
	private static native long getDiskFreeSpaceNative(final String path);
}
