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
package com.teletalk.jserver;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Properties;
import java.util.StringTokenizer;

/**
 * The class loader class used by JServerLauncher.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1
 */
final class JServerClassLoader extends ClassLoader
{
	class ClassPath	{		public boolean isJarFile = false;
		public String classPath;	}	
	private static final String verboseSystemPropertyName = "com.teletalk.jserver.JServerClassLoader.verbose"; //"JServer.classloader.verbose";	private static final boolean verbose;
   		private byte[] classFileBuffer = new byte[10*1024];
		private final ClassPath[] classPathURLs;				static
	{
		final Properties p = System.getProperties();				final Iterator it = p.keySet().iterator();
		String key;
		boolean verboseFound = false;
		
		while(it.hasNext())
		{
			key = (String)it.next();
			
			if(key.trim().equalsIgnoreCase(verboseSystemPropertyName))
			{
				String value = (String)p.get(key);
				
				if( (value != null) && (value.trim().equalsIgnoreCase("true")) )
				{
					verboseFound = true;
				}
			}
		}
		
		verbose = verboseFound; 	}	
	/**
	 */	public JServerClassLoader()
	{		super();					
		String classPath = (String)System.getProperties().get("java.class.path");
		ArrayList urls = new ArrayList();				if(verbose)		{			System.out.println("JServerClassLoader initializing...");			System.out.println("Class path is : " + classPath);
		}				StringTokenizer tokenizer = new StringTokenizer(classPath, ";");
		String token;
		ClassPath classPathInstance;		
		try
		{			while(tokenizer.hasMoreTokens())
			{
				token = tokenizer.nextToken();
				classPathInstance = new ClassPath();
				classPathInstance.isJarFile = false;
				
				if(token != null)
				{
					token = token.trim();
					
					if( token.endsWith("\\") )
					{
						token = token.substring(0, token.length() - 1) + "/";
					}
					else if( token.toLowerCase().endsWith(".jar") || token.toLowerCase().endsWith(".jar") )
					{
						classPathInstance.isJarFile = true;
					}
					else if( !token.endsWith("/") ) 
					{
						token = token + "/";
					}
					
					classPathInstance.classPath = token;
				
					urls.add(classPathInstance);
				}
			}
		}		catch(Exception e)		{			System.out.println("JServerClassLoader: Fatal error while parsing class path!");
			e.printStackTrace();		}				classPathURLs = (ClassPath[])urls.toArray(new ClassPath[]{});
	}	
	/**
	 */	public synchronized Class loadClass(String name) throws ClassNotFoundException
	{
		return this.loadClass(name, false);
	}		/**
	 * Override the super class implementation of loadClass to let this class loader make the first attempt at loading the specified class
	 */
	protected synchronized Class loadClass(String name, boolean resolve) throws ClassNotFoundException	{
		// First, check if the class has already been loaded...
		Class c = findLoadedClass(name);				if(c == null) // ...but if it hasn't, let this class loader attempt to load the class from the class path...		{
			//c = findClass(name);			int classDataLengh = 0;			
			try
			{				classDataLengh = this.loadClassData(name);
			}			catch(IOException ioe)
			{
				classDataLengh = 0;
			}
			
			if(classDataLengh > 0)
			{				if(verbose) System.out.println("JServerClassLoader: class '" + name + "' loaded successfully. Class file size: " + classDataLengh + ".");						c = super.defineClass(name, classFileBuffer, 0, classDataLengh);
			}
							
			if( (c != null) && resolve)
			{				resolveClass(c);			}		}
				if(c == null) // ...and if that fails, run the super class imlementation		{
			if(verbose) System.out.println("JServerClassLoader: Using system class loader to load class '" + name + "'.");		
			c = super.loadClass(name, resolve);		}
		return c;
	}	
	/**
	 * Loads class data.
	 * 
	 * @param name the name of the class to load data for.
	 */	private synchronized int loadClassData(final String name) throws IOException
	{
		//String classFileName = "/" + name;
		String classFileName = name;
		classFileName = classFileName.replace('.', '/');
		classFileName += ".class";
		
		String classUrlString;
		URL classUrl;
		InputStream classInputStream = null;

		// Search class paths
		for(int i=0;  ( (classInputStream == null) && (i < this.classPathURLs.length) ) ; i++)
		{
			if(this.classPathURLs[i].isJarFile)
			{
				classUrlString =  this.classPathURLs[i].classPath + "!/" + classFileName;
			}
			else
			{
				classUrlString = this.classPathURLs[i].classPath + classFileName;
			}

			try
			{
				classUrl = new URL(classUrlString);
			}
			catch(MalformedURLException e)
			{
				if(this.classPathURLs[i].isJarFile)
					classUrl = new URL("jar:" + (new File(classUrlString).toURI().toURL().toExternalForm()));
				else
					classUrl = new File(classUrlString).toURI().toURL();
			}
			
			try
			{
				classInputStream = classUrl.openStream();
				if(verbose) System.out.println("JServerClassLoader: class '" + name + "' found in class path '" + this.classPathURLs[i].classPath + "'");		
			}
			catch(IOException ioe)
			{
				classInputStream = null;
			}
		}
		
		if(classInputStream != null)
		{
			int bytesRead = 0;
			int totalBytesRead = 0;
			int readLength;
		
			while(bytesRead >= 0)
			{
				readLength = classFileBuffer.length - totalBytesRead;
				
				bytesRead = classInputStream.read(classFileBuffer, totalBytesRead, readLength);
				
				if(bytesRead > 0) 
				{
					totalBytesRead += bytesRead;
				
					if(totalBytesRead == classFileBuffer.length) //Grow buffer
					{
						byte[] newCurrentClassFileBuffer = new byte[classFileBuffer.length * 2];
						System.arraycopy(classFileBuffer, 0, newCurrentClassFileBuffer, 0, classFileBuffer.length);
						classFileBuffer = newCurrentClassFileBuffer;
					}
				}
			}
			
			return totalBytesRead;
		}
		else 
		{
			return -1;
		}
	}
}