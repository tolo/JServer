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
import java.io.ObjectOutputStream;
import java.io.OutputStream;

/**
 * Class used to fix a bug in the class ObjectOutputStream (<a href=http://developer.java.sun.com/developer/bugParade/bugs/4332184.html>bug id 4332184</a>).
 * This class only differns from ObjectOutputStream in that it doesn't write any stream headers.<br>
 * <br>
 * The workaround description for the bug is following:<br>
 * <br>
 * "To get around the memory usage problem, you can instantiate 
 * a new ObjectOutputStream from time to time instead of using 
 * the same ObjectOutputStream for the life of an 
 * application.  However, if you try to use a new 
 * ObjectOutputStream, this causes a StreamCorruptedException 
 * because the newly created ObjectOutpuStream flushes a 
 * stream header out when it is constructed.  
 * 
 * To work around this, you must extend ObjectOutputStream and 
 * override the writeStreamHeader() method, giving this method 
 * a null implementation.  Now you can instantiate and use 
 * this new subclass of ObjectOutputStream to send objects 
 * without currupting the stream.
 * 
 * The first ObjectOutputStream instantiated must be of the 
 * type java.io.ObjectOutputStream to send the correct header 
 * information to the corresponding ObjectInputStream.  Then, 
 * all following instantiation must be of the class that 
 * extends ObjectOutputStream. "
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1 (build 399)
 */
public final class NoHeadersObjectOutputStream extends ObjectOutputStream
{
	/**
	 * Provide a way for subclasses that are completely reimplementing ObjectOutputStream to not 
	 * have to allocate private data just used by this implementation of ObjectOutputStream. 
	 * If there is a security manager installed, this method first calls the security manager's checkPermission 
	 * method with a SerializablePermission("enableSubclassImplementation") permission to ensure it's ok to enable subclassing.
	 * 
	 * @exception IOException if not called by a subclass.
	 * @exception SecurityException if a security manager exists and its checkPermission method denies enabling subclassing.
	 */
	protected NoHeadersObjectOutputStream() throws IOException, SecurityException
	{
		super();
	}
	
	/**
	 * Creates an ObjectOutputStream that writes to the specified OutputStream.
	 * 
	 * @exception IOException any exception thrown by the underlying OutputStream.
	 */
	public NoHeadersObjectOutputStream(OutputStream out) throws IOException
	{
		super(out);
	}
	
	/**
	 * Null implementation.
	 */
	protected void writeStreamHeader() throws IOException
	{
	}
}
