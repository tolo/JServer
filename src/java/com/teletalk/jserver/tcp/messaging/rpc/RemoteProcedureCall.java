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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

import com.teletalk.jserver.tcp.messaging.Message;
import com.teletalk.jserver.tcp.messaging.MessageHeader;
import com.teletalk.jserver.util.StringUtils;

/**
 * This class represents the message body of an RCP method call. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.1 Build 690
 */
public class RemoteProcedureCall implements Externalizable
{
   /** The serial version id of this class. */
   static final long serialVersionUID = 5238355879163162973L;
   
   /** The version number of the serialized data of this class. */
   public static final byte SERIAL_DATA_VERSION = 0x01;  
   
   
   private String methodName;
   
   private Object[] params;
   
   private boolean outParameterModeEnabled;
   
   /**
    * Default no arg constructor for serialization (Externalizable).
    */
   public RemoteProcedureCall()
   {
      this.methodName = null; 
      this.params = null;
      this.outParameterModeEnabled = false;
   }
   
   /**
    * Creates a new RemoteProcedureCall.
    */
   public RemoteProcedureCall(final String methodName, final Object[] params)
   {
      this(methodName, params, false);
   }
   
   /**
    * Creates a new RemoteProcedureCall.
    */
   public RemoteProcedureCall(final String methodName, final Object[] params, final boolean outParameterModeEnabled)
   {
      this.methodName = methodName; 
      this.params = params;
      this.outParameterModeEnabled = outParameterModeEnabled;
   }
   
   /**
    * Gets the method name.
    */
   public String getMethodName()
   {
      return methodName;
   }

   /**
    * Sets the method name.
    */
   public void setMethodName(String string)
   {
      methodName = string;
   }

   /**
    * Gets the params.
    */
   public Object[] getParams()
   {
      return params;
   }

   /**
    * Sets the params.
    */
   public void setParams(Object[] objects)
   {
      params = objects;
   }
   
   /**
    * Gets the flag indicating if out parameters are enabled.
    */
   public boolean isOutParameterModeEnabled()
   {
      return outParameterModeEnabled;
   }

   /**
    * Sets the flag indicating if out parameters are enabled.
    */
   public void setOutParameterModeEnabled(boolean outParameterModeEnabled)
   {
      this.outParameterModeEnabled = outParameterModeEnabled;
   }
   
   /**
    * Serialization (Externalizable) method.
    * 
    * @param out the stream on which to serialize objects of this class. 
    */
   public void writeExternal(final ObjectOutput out) throws IOException
   {
      // Write command version
      out.writeByte(SERIAL_DATA_VERSION);
      
      out.writeObject(this.methodName);
      out.writeObject(this.params);
      out.writeBoolean(this.outParameterModeEnabled);
   }
   
   /**
    * Deserialization (Externalizable) method.
    * 
    * @param in the stream from which to deserialize objects of this class.
    */
   public void readExternal(final ObjectInput in) throws IOException, ClassNotFoundException
   {
      // Read command version
      in.readByte();
      
      this.methodName = (String)in.readObject();
      this.params = (Object[])in.readObject();
      this.outParameterModeEnabled = in.readBoolean();
   }
   
   /**
    * Checks if the specified message contains a remote procedure call. This is accomplished by checking the type of the message header. 
    * 
    * @param message the {@link Message} in which to check for the existence of a RemoteProcedureCall.
    * 
    * @return <code>true</code> if the specified message contains a RemoteProcedureCall, otherwise <code>false</code>.
    * 
    * @since 2.0 Build 762
    */
   public static boolean isRemoteProcedureCall(final Message message)
   {
      return (message.getHeader().getHeaderType() == MessageHeader.RPC_HEADER);
   }
   
   /**
    * Attempts to read the body of the specified message as a RemoteProcedureCall (if the message is a RPC message).
    * 
    * @param message the {@link Message} to read the RemoteProcedureCall from.
    * @param cacheBody flag indicating if the body of the message should be cached, and thus be available through future calls to {@link Message#getBodyAsObject()}. 
    * 
    * @return a RemoteProcedureCall object or <code>null</code> if the message didn't contain a RemoteProcedureCall.
    * 
    * @throws IOException if an error occurrs while reading the message body.
    * 
    * @since 2.0 Build 762
    */
   public static RemoteProcedureCall getRemoteProcedureCall(final Message message, final boolean cacheBody) throws IOException
   {
      MessageHeader header = message.getHeader();
      if( header.getHeaderType() == MessageHeader.RPC_HEADER )
      {
         if( cacheBody && !message.isMessageBodyCachingEnabled() ) message.setMessageBodyCachingEnabled(true);
         return (RemoteProcedureCall)message.getBodyAsObject();
      }
      else return null;
   }
   
   /**
    * Attempts to get the RPC method name from the header of the specified message, by checking for a {MessagingRpcInterface#RPC_METHOD_NAME} custom 
    * header field in it.
    * 
    * @param message the {@link Message} to read the RPC method name from.
    * 
    * @return the RPC method name or <code>null</code> if the message header didn't contain a {MessagingRpcInterface#RPC_METHOD_NAME} custom header.
    * 
    * @since 2.0 Build 762
    */
   public static String getRPCMethodName(final Message message)
   {
      return getRPCMethodName(message.getHeader());
   }
   
   /**
    * Attempts to get the RPC method name from the specified header, by checking for a {MessagingRpcInterface#RPC_METHOD_NAME} custom 
    * header field in it.
    * 
    * @param header the {@link MessageHeader} to read the RPC method name from.
    * 
    * @return the RPC method name or <code>null</code> if the message header didn't contain a {MessagingRpcInterface#RPC_METHOD_NAME} custom header.
    * 
    * @since 2.0.1 (20050304)
    */
   public static String getRPCMethodName(final MessageHeader header)
   {
      return (String)header.getCustomHeaderField(MessagingRpcInterface.RPC_METHOD_NAME);
   }
   
   /**
    * Attempts to read the body of the specified message as a RemoteProcedureCall (if the message is a RPC message) 
    * and get the RPC method parameters. This method enables caching of the message body and thus makes it 
    * available through future calls to {@link Message#getBodyAsObject()}.
    * 
    * @param message the {@link Message} to read the RemoteProcedureCall from.
    * 
    * @return the RPC method parameters or <code>null</code> if the message didn't contain a RemoteProcedureCall.
    * 
    * @throws IOException if an error occurrs while reading the message body.
    * 
    * @since 2.0 Build 762
    */
   public static Object[] getRPCParams(final Message message) throws IOException
   {
      RemoteProcedureCall remoteProcedureCall = getRemoteProcedureCall(message, true);
      if( remoteProcedureCall != null )
      {
         return remoteProcedureCall.getParams();
      }
      else return null;
   }

   /**
    * Gets a string representation of this object.
    * 
    * @since 2.1.3 (20060324)
    */
   public String toString()
   {
      return "RemoteProcedureCall(method: " + this.methodName + ", params: " + StringUtils.limitStringLength(StringUtils.toString(this.params), 128) + ", outParameterModeEnabled: " + outParameterModeEnabled + ")";
   }
}
