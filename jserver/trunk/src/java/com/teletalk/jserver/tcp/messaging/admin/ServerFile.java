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
 * Class representing a file on the server.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1.6 (20070503)
 */
public class ServerFile implements Externalizable
{
   static final long serialVersionUID = -3226419162417119133L;
   
   /** The version number of the serialized data of this class. */
   public static final byte SERIAL_DATA_VERSION = 0x01;
   

   private ServerFileInfo serverFileInfo;
      
   private byte[] fileData;
   
   
   public ServerFile()
   {
      this(null, null);
   }
   
   public ServerFile(ServerFileInfo serverFileInfo, byte[] fileData)
   {
      this.serverFileInfo = serverFileInfo;
      this.fileData = fileData;
   }
   

   public byte[] getFileData()
   {
      return fileData;
   }

   public void setFileData(byte[] fileData)
   {
      this.fileData = fileData;
   }

   public ServerFileInfo getServerFileInfo()
   {
      return serverFileInfo;
   }

   public void setServerFileInfo(ServerFileInfo serverFileInfo)
   {
      this.serverFileInfo = serverFileInfo;
   }

   public String toString()
   {
      return "ServerFileInfo(" + this.serverFileInfo + ", data length: " + ((this.fileData != null) ? 
               Integer.toString(this.fileData.length) : "0") + ")";
   }
   
   
   /* ### EXTERNALIZABLE METHODS ### */
   
   
   /**
    * Deserialization method.
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      // Read (consume) serial version
      in.readByte();
      
      this.serverFileInfo = (ServerFileInfo)in.readObject();
      this.fileData = (byte[])in.readObject();
   }
   
   /**
    * Serialization method.
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      // Write serial version
      out.writeByte(SERIAL_DATA_VERSION);
      
      out.writeObject(this.serverFileInfo);
      out.writeObject(this.fileData);
   }
}
