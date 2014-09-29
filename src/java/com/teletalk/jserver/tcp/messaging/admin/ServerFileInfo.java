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
package com.teletalk.jserver.tcp.messaging.admin;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * Class containing information about a file on the server.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1.6 (20070503)
 */
public class ServerFileInfo implements Externalizable
{
   static final long serialVersionUID = -2896140891602555419L;

   /** The version number of the serialized data of this class. */
   public static final byte SERIAL_DATA_VERSION = 0x01;
   

   private String fileName;
   
   private boolean directory;
   
   private long lastModified;
   
   
   public ServerFileInfo()
   {
      this.fileName = null;
      this.directory = false;
      this.lastModified = -1;
   }
   
   public ServerFileInfo(String fileName, boolean directory, long lastModified)
   {
      this.fileName = fileName;
      this.directory = directory;
      this.lastModified = lastModified;
   }

   public boolean isDirectory()
   {
      return directory;
   }

   public void setDirectory(boolean directory)
   {
      this.directory = directory;
   }

   public String getFileName()
   {
      return fileName;
   }

   public void setFileName(String fileName)
   {
      this.fileName = fileName;
   }

   public long getLastModified()
   {
      return lastModified;
   }

   public void setLastModified(long lastModified)
   {
      this.lastModified = lastModified;
   }
   
   public String toString()
   {
      return "ServerFileInfo(fileName: " + this.fileName + ", directory: " + this.directory + ", lastModified: " + this.lastModified + ")";
   }
   
   
   /* ### EXTERNALIZABLE METHODS ### */
   
   
   /**
    * Deserialization method.
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      // Read (consume) serial version
      in.readByte();
      
      this.fileName = (String)in.readObject();
      this.directory = in.readBoolean();
      this.lastModified = in.readLong();
   }
   
   /**
    * Serialization method.
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      // Write serial version
      out.writeByte(SERIAL_DATA_VERSION);
      
      out.writeObject(this.fileName);
      out.writeBoolean(this.directory);
      out.writeLong(this.lastModified);
   }
}
