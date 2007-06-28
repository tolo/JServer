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

import java.util.StringTokenizer;

/**
 * Utility class containing useful methods when dealing with bugs and features of different versions of Java Runtime Environments. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1
 */
public class JavaBugUtils
{
	private static final int majorVersion;
	private static final int minorVersion;
	private static final int microVersion;
	
	static
	{
		String javaVersion = System.getProperty("java.version");
		
		if( javaVersion != null ) javaVersion = javaVersion.trim();
		else javaVersion = "";
		
		int endIndex = 0;
		
		for(int i=0; i<javaVersion.length(); i++)
		{
			if( !Character.isDigit(javaVersion.charAt(i)) && (javaVersion.charAt(i) != '.') )
			{
				endIndex = i;
				break;
			}
		}
		
		javaVersion = javaVersion.substring(0, endIndex);
		
		StringTokenizer tokenizer = new StringTokenizer(javaVersion, ".");

		if(tokenizer.hasMoreTokens()) majorVersion = Integer.parseInt(tokenizer.nextToken());
		else majorVersion = -1;
		
		if(tokenizer.hasMoreTokens()) minorVersion = Integer.parseInt(tokenizer.nextToken());
		else minorVersion = -1;
		
		if(tokenizer.hasMoreTokens()) microVersion = Integer.parseInt(tokenizer.nextToken());
		else microVersion = -1;
	}
	
	/**
	 * Checks if the current java version is 1.3.0 (and vendor is Sun Microsystems).
	 * 
	 * @return <code>true</code> if the current java version is 1.3.0 (and vendor is Sun Microsystems), otherwise <code>false</code>.
	 */
	public static boolean isUsingJava1_3_0()
	{
		return isJavaVersion(1, 3, 0);
	}
	
	/**
	 * Checks if the version of the current Java Runtime Environment matches the specified major and minor version numbers.
	 * 
	 * @param major the major version.
	 * @param minor the minor version.
	 * 
	 * @since 1.2
	 */
	public static boolean isJavaVersion(final int major, final int minor)
	{
		return (major == JavaBugUtils.majorVersion) && (minor == JavaBugUtils.minorVersion);
	}
	
	/**
	 * Checks if the version of the current Java Runtime Environment matches the specified major, minor and micro version numbers.
	 * 
	 * @param major the major version.
	 * @param minor the minor version.
	 * @param micro the micro version.
	 * 
	 * @since 1.2
	 */
	public static boolean isJavaVersion(final int major, final int minor, final int micro)
	{
		return isJavaVersion(major, minor) && (micro == JavaBugUtils.microVersion);
	}
	
	/**
	 * Checks if the version of the current Java Runtime Environment is greater or equal to the specified major and minor version numbers.
	 * 
	 * @param major the major version.
	 * @param minor the minor version.
	 * 
	 * @since 1.2
	 */
	public static boolean isJavaVersionOrHigher(final int major, final int minor)
	{
		if(major == JavaBugUtils.majorVersion) 
		{
			if( minor <= 0 ) return true;
			else return (JavaBugUtils.minorVersion > -1) && (JavaBugUtils.minorVersion >= minor);
		}
		else return (JavaBugUtils.majorVersion > -1) && (JavaBugUtils.majorVersion >= major);
	}
	
	/**
	 * Checks if the version of the current Java Runtime Environment is greater or equal to the specified major, minor and micro version numbers.
	 * 
	 * @param major the major version.
	 * @param minor the minor version.
	 * @param micro the micro version.
	 * 
	 * @since 1.2
	 */
	public static boolean isJavaVersionOrHigher(final int major, final int minor, final int micro)
	{
		if( isJavaVersionOrHigher(major, minor) ) 
		{
			if( micro <= 0 ) return true;
			else return (JavaBugUtils.microVersion >= -1) && (JavaBugUtils.microVersion >= micro);
		}
		else return false;
	}
}
