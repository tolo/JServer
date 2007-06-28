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

import java.io.IOException;
import java.io.InputStream;
import java.io.StreamCorruptedException;

/**
 * ObjectInputStream subclass that makes it possible to specify a ClassLoader that will be used 
 * for resolving classes of objects that are read from the stream. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3
 */
public class NoHeadersClassLoaderObjectInputStream extends ClassLoaderObjectInputStream
{
	/**
	 * Provide a way for subclasses that are completely reimplementing ObjectInputStream to not 
	 * have to allocate private data just used by this implementation of ObjectInputStream. 
	 * If there is a security manager installed, this method first calls the security manager's checkPermission 
	 * method with the SerializablePermission("enableSubclassImplementation") permission to ensure it's ok to enable subclassing.
	 * 
	 * @exception IOException if not called by a subclass.
	 * @exception SecurityException if a security manager exists and its checkPermission method denies enabling subclassing.
	 */
	protected NoHeadersClassLoaderObjectInputStream() throws IOException, SecurityException
	{
		super();
	}
   
   /**
    * Create an ObjectInputStream that reads from the specified InputStream. 
    * 
    * @exception IOException if an exception occurred in the underlying stream.
    */
   public NoHeadersClassLoaderObjectInputStream(InputStream in) throws IOException
   {
      this(in, null);
   }
	
	/**
	 * Create an ObjectInputStream that reads from the specified InputStream. 
	 * 
	 * @exception IOException if an exception occurred in the underlying stream.
	 */
	public NoHeadersClassLoaderObjectInputStream(InputStream in, ClassLoader classLoader) throws IOException
	{
		super(in, classLoader);
	}
	
   /**
    * Null implementation.
    */
   protected void readStreamHeader() throws IOException, StreamCorruptedException
   {
   }
}
