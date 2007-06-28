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
package com.teletalk.jserver.tcp.messaging.rpc;

import java.io.FilterInputStream;
import java.io.InputStream;

import com.teletalk.jserver.tcp.messaging.Message;

/**
 * RPC Input stream wrapper class for sending a java.io.InputStream in a RCP method call, either as a return value or 
 * a parameter. Methods using this mechanism should declare java.io.InputStream as the type of the return value/parameter.<br>
 * <br>
 * To use this class, simply create an instance of it by passing the input stream to be sent along with the data length in the constructor. 
 * After that, the object may be returned directly as a response object or used to dispatch an RPC call through {@link MessagingRpcInterface}. 
 * Note that when using this class, no other return values or parameters may be sent in the message, as this class uses the 
 * whole body of the message. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20050912)
 */
public class RpcInputStream extends FilterInputStream
{
   public static final String RPC_INPUTSTREAM_HEADER_KEY = "com.teletalk.jserver.tcp.messaging.rpc.RpcInputStream";
   
   
   private InputStream inputStream;
   
   private long dataLength;
   
   private Message message;
   
   
   /**
    * Creates an RpcInputStream for sending of a java.io.InputStream object in a RCP method call on the caller side. 
    */
   public RpcInputStream(final InputStream inputStream, final long dataLength)
   {
      super(inputStream);
      
      this.inputStream = inputStream;
      this.dataLength = dataLength;
      this.message = null;
   }
   
   /**
    * Creates an RpcInputStream for reading a java.io.InputStream object in a RCP method call on the receiver side.
    */
   public RpcInputStream(final Message message)
   {
      this(message.getBodyAsStream(), message.getHeader().getBodyLength());
      this.message = message;
   }
   
   /**
    * Gets the size in bytes of the data to read from the stream.
    */
   public long getDataLength()
   {
      return dataLength;
   }
   
   /**
    * Sets the size in bytes of the data to read from the stream.
    */
   public void setDataLength(long dataLength)
   {
      this.dataLength = dataLength;
   }
   
   /**
    * Gets the actual InputStream.
    */
   public InputStream getInputStream()
   {
      return inputStream;
   }
   
   /**
    * Sets the actual InputStream.
    */
   public void setInputStream(InputStream inputStream)
   {
      this.inputStream = inputStream;
   }
   
   /**
    * Gets the associated {@link Message} (client side only).
    */
   public Message getMessage()
   {
      return message;
   }
   
   /**
    * Sets the associated {@link Message} (client side only).
    */
   public void setMessage(Message message)
   {
      this.message = message;
   }

   /**
    * Gets a string representation of this object.
    * 
    * @since 2.1 (20051014)
    */
   public String toString()
   {
      return "RpcInputStream(dataLength: " + this.dataLength + ")";
   }
}
