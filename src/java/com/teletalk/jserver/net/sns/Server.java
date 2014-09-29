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
package com.teletalk.jserver.net.sns;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.HashMap;
import java.util.List;

/**
 * Class representing a server registered in the SNS.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20050502)
 */
public class Server implements Externalizable
{
   static final long serialVersionUID = -1331283297421511430L;
   
   /** The version number of the serialized data of this class. */
   public static final byte SERIAL_DATA_VERSION = 0x01;
   
   private static final long LINK_LOST_TIME_NOT_SET = -1;
   
   
   private String name;
   
   private String snsClientAddress;
   
   private List services;
   
   private HashMap metaData;
   
   private long linkLostTime;
   
   
   /**
    * Creates a new Server.
    */
   public Server()
   {
      this(null, null, null, null);
   }
   
   /**
    * Creates a new Server.
    */
   public Server(String name)
   {
      this(name, null, null, null);
   }
   
   /**
    * Creates a new Server.
    */
   public Server(String name, String snsClientAddress, List services, HashMap metaData)
   {
      this.name = name;
      this.snsClientAddress = snsClientAddress;
      this.services = services;
      this.metaData = metaData;
      this.linkLostTime = LINK_LOST_TIME_NOT_SET;
   }
   
   /**
    * Gets the name of the server
    */
   public String getName()
   {
      return name;
   }
   /**
    * Sets the name of the server
    */
   public void setName(String name)
   {
      this.name = name;
   }
   
   /**
    * Gets the administration proxy rpc handler name, i.e. the RPC handler name which an  
    * administration proxy is registered with in the SNS for this server. The string returned by this method consists of 
    * {@link SnsConstants#SNS_ADMINISTRATION_PROXY_RPC_HANDLER_PREFIX} followed the name of the server. 
    * 
    * @see com.teletalk.jserver.tcp.messaging.admin.ServerAdministrationInterface
    */
   public String getAdministrationProxyRpcHandlerName()
   {
      return SnsConstants.SNS_ADMINISTRATION_PROXY_RPC_HANDLER_PREFIX + name;
   }
   
   /**
    * Gets the address of the sns client of this server.
    */
   public String getSnsClientAddress()
   {
      return snsClientAddress;
   }
   /**
    * Sets the address of the sns client of this server.
    */
   public void setSnsClientAddress(String address)
   {
      this.snsClientAddress = address;
   }
   
   /**
    * Checks if this server has the service with the specified name.
    */
   public boolean hasService(String serviceName)
   {
      if( (this.services != null) && (serviceName != null) )
      {
         for(int i=0; i<this.services.size(); i++)
         {
            if( serviceName.equals( ((Service)this.services.get(i)).getName() ) ) return true;
         }
      }
      return false;
   }
   
   /**
    * Gets the services of this server.
    */
   public List getServices()
   {
      return services;
   }
   /**
    * Sets the services of this server.
    */
   public void setServices(List services)
   {
      this.services = services;
   }

   /**
    * Gets the (messaging) meta data of this server.
    */
   public HashMap getMetaData()
   {
      return metaData;
   }
   /**
    * Sets the (messaging) meta data of this server.
    */
   public void setMetaData(HashMap metaData)
   {
      this.metaData = metaData;
   }
   
   /**
    * Gets the time when the SNS lost the link to the server.
    */
   public long getLinkLostTime()
   {
      return linkLostTime;
   }
   /**
    * Resets the link lost time, indicating that there again exists a link between the SNS and the server.
    */
   public void resetLinkLostTime()
   {
      this.linkLostTime = LINK_LOST_TIME_NOT_SET;
   }
   /**
    * Sets the time when the SNS lost the link to the server.
    */
   public void setLinkLostTime(long linkLostTime)
   {
      this.linkLostTime = linkLostTime;
   }
   /**
    * Checks if the SNS has lost the link to the server.
    */
   public boolean isLinkLost()
   {
      return (this.linkLostTime != LINK_LOST_TIME_NOT_SET);
   }

   
   /**
    * Returns a string representation of the object.
    */
   public String toString()
   {
      return toString(true);
   }
   
   /**
    * Returns a string representation of the object.
    */
   public String toString(boolean includeClassName)
   {
      if( includeClassName ) return "Server(" + name + "/" + snsClientAddress + ")"; 
      else return name + "/" + snsClientAddress;
   }
   
   
   /* ### EXTERNALIZABLE METHODS ### */
   
   
   /**
    * Deserialization method.
    */
   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      // Read (consume) serial version
      in.readByte();
      
      this.name = (String)in.readObject();
      this.snsClientAddress = (String)in.readObject();
      this.services = (List)in.readObject();
      this.metaData = (HashMap)in.readObject();
      this.linkLostTime = in.readLong();
   }
   
   /**
    * Serialization method.
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      // Write serial version
      out.writeByte(SERIAL_DATA_VERSION);
      
      out.writeObject(this.name);
      out.writeObject(this.snsClientAddress);
      out.writeObject(this.services);
      out.writeObject(this.metaData);
      out.writeLong(this.linkLostTime);
   }
}
