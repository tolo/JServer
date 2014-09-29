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
package com.teletalk.jserver.tcp.messaging;

import java.util.Map;

/**
 * Configuration class for {@link MessageDispatcher}.
 * 
 * @see MessageDispatcher
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.1 Build 690
 */
public class MessageDispatcherProperties
{
	private Destination destination;
		
	private String namedReceiver;
   
   private Map destinationMetaDataConstraints;
	
   private long timeout;
   
	private boolean asynch;

   /**
    * Copy constructor.
    */
   public MessageDispatcherProperties(MessageDispatcherProperties copy)
   {
      if( copy != null )
      {
         this.destination = copy.destination;
         this.namedReceiver = copy.namedReceiver;
         this.destinationMetaDataConstraints = copy.destinationMetaDataConstraints;
         this.timeout = copy.timeout;
         this.asynch = copy.asynch;
      }
      else
      {
         this.destination = null;
         this.namedReceiver = null;
         this.destinationMetaDataConstraints = null;
         this.timeout = -1;
         this.asynch = false;
      }
   }

   /**
    * Creates a new MessageDispatcher with default values (all values null, except timeout (-1) and asych (false)).
    */
   public MessageDispatcherProperties()
   {
      this((String)null);
   }
   
   /**
    * Creates a new MessageDispatcher.
    * 
    * @param namedReceiver the name of a named receiver to which messages are to be dispatched.
    */
   public MessageDispatcherProperties(String namedReceiver)
   {
      this.destination = null;
      this.namedReceiver = namedReceiver;
      this.destinationMetaDataConstraints = null;
      this.timeout = -1;
      this.asynch = false;
   }
   
   /**
    * Creates a new MessageDispatcher.
    * 
    * @param destination a remote destination to dispatch messages to.
    * 
    * @since 2.0 Build 757
    */
   public MessageDispatcherProperties(Destination destination)
   {
      this.destination = destination;
      this.namedReceiver = null;
      this.destinationMetaDataConstraints = null;
      this.timeout = -1;
      this.asynch = false;
   }
   
   /**
    * Creates a new MessageDispatcher.
    * 
    * @param namedReceiver the name of a named receiver.
    * @param timeout the timeout for synchronous messages. if timeout <= 0, the default timeout of {@link MessagingManager} will be used.
    * @param asynch flag indicating if the message should be asynchronous or not.
    */
   public MessageDispatcherProperties(String namedReceiver, long timeout, boolean asynch)
   {
      this.destination = null;
      this.namedReceiver = namedReceiver;
      this.destinationMetaDataConstraints = null;
      this.timeout = timeout;
      this.asynch = asynch;
   }
   
   /**
    * Creates a new MessageDispatcher.
    * 
    * @param destination a remote destination to dispatch messages to. 
    * @param namedReceiver the name of a named receiver.
    */
   public MessageDispatcherProperties(Destination destination, String namedReceiver)
   {
      this.destination = destination;
      this.namedReceiver = namedReceiver;
      this.destinationMetaDataConstraints = null;
      this.timeout = -1;
      this.asynch = false;
   }
   
   /**
    * Creates a new MessageDispatcher.
    * 
    * @param destination a remote destination to dispatch messages to. 
    * @param namedReceiver the name of a named receiver.
    * @param timeout the timeout for synchronous messages. if timeout <= 0, the default timeout of {@link MessagingManager} will be used.
    * @param asynch flag indicating if the message should be asynchronous or not.
    */
   public MessageDispatcherProperties(Destination destination, String namedReceiver, long timeout, boolean asynch)
   {
      this.destination = destination;
      this.namedReceiver = namedReceiver;
      this.destinationMetaDataConstraints = null;
      this.timeout = timeout;
      this.asynch = asynch;
   }
   
   /**
    * Gets the destination setting.
    */
   public Destination getDestination()
   {
      return destination;
   }

   /**
    * Sets the destination setting.
    */
   public void setDestination(Destination destination)
   {
      this.destination = destination;
   }

   /**
    * Gets the named receiver setting.
    */
   public String getNamedReceiver()
   {
      return namedReceiver;
   }

   /**
    * Sets the named receiver setting.
    */
   public void setNamedReceiver(String namedReceiver)
   {
      this.namedReceiver = namedReceiver;
   }

   /**
    * Gets the destination meta data constraints setting.
    */
   public Map getDestinationMetaDataConstraints()
   {
      return destinationMetaDataConstraints;
   }

   /**
    * Sets the destination meta data constraints setting.
    */
   public void setDestinationMetaDataConstraints(Map destinationMetaDataConstraints)
   {
      this.destinationMetaDataConstraints = destinationMetaDataConstraints;
   }

   /**
    * Gets the timeout setting for synchronous messages. if timeout <= 0, the default timeout of {@link MessagingManager} will be used.
    */
   public long getTimeout()
   {
      return timeout;
   }

   /**
    * Sets the timeout setting for synchronous messages. if timeout <= 0, the default timeout of {@link MessagingManager} will be used.
    */
   public void setTimeout(long timeout)
   {
      this.timeout = timeout;
   }
   
   /**
    * Gets the asynch flag.
    */
   public boolean isAsynch()
   {
      return asynch;
   }

   /**
    * Sets the asynch flag.
    */
   public void setAsynch(boolean asynch)
   {
      this.asynch = asynch;
   }
   
   /**
    * Gets a string representation of this object.
    */
   public String toString()
   {
      return "MessageDispatcherProperties[destination: " + destination + ", namedReceiver: " + namedReceiver + ", destinationMetaDataConstraints: " + destinationMetaDataConstraints + ", timeout: " + timeout + ", asynch: " + asynch + "] ";
   }
   
   /**
    * Indicates whether some other object is "equal to" this one.
    */
   public boolean equals(final Object obj)
   {
      if( obj instanceof MessageDispatcherProperties )
      {
         MessageDispatcherProperties otherMessageDispatcherProperties = (MessageDispatcherProperties)obj;
         
	      return 
	      	compareObject(this.destination, otherMessageDispatcherProperties.destination) &&
	      	compareObject(this.namedReceiver, otherMessageDispatcherProperties.namedReceiver) && 
	      	compareObject(this.destinationMetaDataConstraints, otherMessageDispatcherProperties.destinationMetaDataConstraints) &&
	      	(this.timeout == otherMessageDispatcherProperties.timeout) &&
	      	(this.asynch == otherMessageDispatcherProperties.asynch);
      }
      return false;
   }
   
   private static boolean compareObject(Object o1, Object o2)
   {
      if( (o1 == null) && (o2 == null) ) return true;
      else if ( (o1 != null) && o1.equals(o2) ) return true;
      return false;
   }
}
