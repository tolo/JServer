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
import java.util.HashSet;
import java.util.Set;

import com.teletalk.jserver.util.EqualsUtils;
import com.teletalk.jserver.util.StringUtils;

/**
 * Class representing a service available though an SNS.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20050502)
 */
public class Service implements Externalizable
{
   static final long serialVersionUID = 8401399368546602911L;
   
   /** The version number of the serialized data of this class. */
   public static final byte SERIAL_DATA_VERSION = 0x01;
   
   
   private String name;
   
   private Set addresses;
   
   /**
    * Creates a new Service.
    */
   public Service()
   {
      this(null, null);
   }
   
   /**
    * Creates a new Service.
    */
   public Service(String name)
   {
      this(name, new HashSet());
   }
   
   /**
    * Creates a new Service.
    */
   public Service(String name, Set servers)
   {
      this.name = name;
      this.addresses = servers;
   }
   
   /**
    * Gets the name of the service.
    */
   public String getName()
   {
      return name;
   }
   /**
    * Sets the name of the service.
    */
   public void setName(String id)
   {
      this.name = id;
   }
   
   /**
    * Gets the addresses available for the service in the SNS.
    */
   public Set getAddresses()
   {
      return addresses;
   }
   /**
    * Sets the addresses available for the service in the SNS.
    */
   public void setAddresses(Set servers)
   {
      this.addresses = servers;
   }
   /**
    * Adds an address available for the service in the SNS.
    */
   public void addAddresses(Set servers)
   {
      if( servers == null ) return;
      if( this.addresses == null ) this.setAddresses(servers);
      else this.addresses.addAll(servers);
   }
   
   
   /**
    * Indicates whether some other object is "equal to" this one.
    */
   public boolean equals(Object obj)
   {
      if( obj instanceof Service )
      {
         Service otherService = (Service)obj;
         return EqualsUtils.equals(this.name, otherService.name) && EqualsUtils.equals(this.addresses, otherService.addresses);
      }
      return false;
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
      if( includeClassName ) return "Service(" + this.name + " - " + StringUtils.toString(this.addresses) + ")"; 
      else return this.name + " - " + StringUtils.toString(this.addresses);
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
      this.addresses = (Set)in.readObject();
   }
   
   /**
    * Serialization method.
    */
   public void writeExternal(ObjectOutput out) throws IOException
   {
      // Write serial version
      out.writeByte(SERIAL_DATA_VERSION);
      
      out.writeObject(this.name);
      out.writeObject(this.addresses);
   }
}
