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
package com.teletalk.jserver.queue.messaging;

import com.teletalk.jserver.comm.EndPointIdentifier;
import com.teletalk.jserver.util.EqualsUtils;

/**
 * This class is used to identify a remote messaging queue system.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1.2
 */
public class MessagingQueueSystemEndPointIdentifier implements EndPointIdentifier
{
   static final long serialVersionUID = -5784655430837015373L;

   
   //private static final Canonicalizer canonicalizer = new Canonicalizer();
   
   
   private String receiverName;
   
   private String serverName;
      
   
   /**
    * Creates a new MessagingQueueSystemEndPointIdentifier.
    */
   public MessagingQueueSystemEndPointIdentifier(final String receiverName)
   {
      this(receiverName, null);
   }
      
   /**
    * Creates a new MessagingQueueSystemEndPointIdentifier.
    */
   public MessagingQueueSystemEndPointIdentifier(final String receiverName, final String serverName)//final TcpEndPointIdentifier address)
   {
      this.receiverName = receiverName;
      this.serverName = serverName;
   }
   
   
   /**
    * Gets the receiver name set in this MessagingQueueSystemEndPointIdentifier. 
    */
   public String getReceiverName()
   {
      return receiverName;
   }

   /**
    * Sets the receiver name in this MessagingQueueSystemEndPointIdentifier.
    */
   public void setReceiverName(String receiverName)
   {
      this.receiverName = receiverName;
   }
   
   /**
    * Gets the name of the server associated with this MessagingQueueSystemEndPointIdentifier.
    */
   public String getServerName()
   {
      return serverName;
   }

   /**
    * Sets the name of the server associated with this MessagingQueueSystemEndPointIdentifier.
    */
   public void setServerName(String serverName)
   {
      this.serverName = serverName;
   }

   /**
    * Gets (the address of) this EndPointIdentifier as a string.
    */
   public String getAddressAsString()
   {
      return this.toString();
   }

   
   /**
    * Attempts to get an already existing EndPointIdentifier instance that contains the same information as this object.
    * This implementation returns this.
    */
   public EndPointIdentifier getSharedInstance()
   {
      return this;//(EndPointIdentifier)canonicalizer.canonicalize(this);
   }

   /**
    * Gets a string representation of this object.
    */
   public String toString()
   {
      if( this.serverName == null ) return this.receiverName;
      else if( this.receiverName == null ) return this.serverName;
      else return this.receiverName + "@" + this.serverName;
   }
   
   /**
    * Compares this object with another.
    */
   public boolean equals(Object obj)
   {
      if(obj instanceof MessagingQueueSystemEndPointIdentifier)
      {
         MessagingQueueSystemEndPointIdentifier otherMessagingQueueSystemEndPointIdentifier = (MessagingQueueSystemEndPointIdentifier)obj;
         
         if( this.serverName != null )
         {
            return EqualsUtils.equals(this.serverName, otherMessagingQueueSystemEndPointIdentifier.serverName) && 
               EqualsUtils.equals(this.receiverName, otherMessagingQueueSystemEndPointIdentifier.receiverName);
         }
         else if( otherMessagingQueueSystemEndPointIdentifier.serverName == null )
         {
            return EqualsUtils.equals(this.receiverName, otherMessagingQueueSystemEndPointIdentifier.receiverName);
         }
      }
      return false;
   }
}
