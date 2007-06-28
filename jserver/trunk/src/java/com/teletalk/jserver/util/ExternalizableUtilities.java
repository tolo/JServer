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
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Utility class that provides a uniform way of serializing String objects 
 * when the <code>java.io.Externalizable</code> is used.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1
 */
public final class ExternalizableUtilities
{
  /**
   * Reads the externalizable version (major and minor) for a class from the specified ObjectInput interface and compares it to the version specified by 
   * parameter expectedMajorVersion and expectedMinorVersion. If the major versions doesn't match (major version in stream is larger than <code>expectedMajorVersion</code>), an IOException will be thrown.
   * 
   * @param objectIn the ObjectInput interface to read the version from.
   * @param expectedMajorVersion the expected major version.
   * @param expectedMinorVersion the expected minor version.
   * 
   * @return a short array containing the major (index 0) and minor (index 1) versions.
   * 
   * @throws IOException if an I/O error occurs or if the expected major version and the major version in the stream doesn't match. 
   */
  public static short[] readExternalVersion(final ObjectInput objectIn, final short expectedMajorVersion, final short expectedMinorVersion) throws IOException
  {
    final short[] version = new short[2];
    
    // Read version in stream
    version[0] = objectIn.readShort();
    version[1] = objectIn.readShort();
    
    //if( version[0] >expectedMajorVersion ) throw new IOException("Unsupported major version of " + JServerUtilities.getCallerClassName() + ". Expected: " + expectedMajorVersion + "." + expectedMinorVersion + ", Got: " + version[0] + "." + version[1] + ".");
      
    return version;
  }
  
  /**
   * Writes the externalizable version of a class.
   * 
   * @param majorVersion the major version.
   * @param minorVersion the minor version.
   * 
   * @throws IOException if an I/O error occurs.
   */
  public static void writeExternalVersion(final ObjectOutput objectOut, final short majorVersion, final short minorVersion) throws IOException
  {
    objectOut.writeShort(majorVersion);
    objectOut.writeShort(minorVersion);
  }
  
  /**
   * Reads (deserializes) a string from the specified ObjectInput interface.
   * 
   * @param objectIn the ObjectInput interface to read a string from.
   * 
   * @return the read String object (may be <code>null</code>).
   * 
   * @throws IOException if an I/O error occurs.
   * @throws ClassNotFoundException If the class for an object being restored cannot be found.
   */
  public static String readString(final ObjectInput objectIn) throws IOException, ClassNotFoundException
  {
    return (String)objectIn.readObject();
  }
  
  /**
   * Writes (serializes) a string using the specified ObjectOutput interface.
   * 
   * @param objectOut the ObjectOutput interface to write a string to.
   * @param str the String object to write (<code>null</code> permitted).
   * 
   * @throws IOException if an I/O error occurs.
   */
  public static void writeString(final ObjectOutput objectOut, final String str) throws IOException
  {
    objectOut.writeObject(str);
  }
}
