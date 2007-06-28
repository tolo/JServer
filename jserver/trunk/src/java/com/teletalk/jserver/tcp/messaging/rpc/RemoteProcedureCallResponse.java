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

import com.teletalk.jserver.util.StringUtils;

/**
 * This class represents the message body of an RCP method call response.
 *  
 * @author Tobias Löfstrand
 * 
 * @since 1.3.1 Build 690
 */
public class RemoteProcedureCallResponse implements Externalizable
{
   /** The serial version id of this class. */
   static final long serialVersionUID = -8203876475813192055L;
   
   /** The version number of the serialized data of this class. */
   public static final byte SERIAL_DATA_VERSION = 0x01;  
   
   
   private RpcException rpcException;
   
   private Object returnValue;
   
   private Object[] params;
   
   
   /**
    * Default no arg constructor for serialization (Externalizable).
    */
   public RemoteProcedureCallResponse()
   {
      this.returnValue = null; 
      this.params = null;
   }
   
   /**
    * Creates a new RemoteProcedureCallResponse, representing an exception that is to be thrown.
    */
   public RemoteProcedureCallResponse(final RpcException rpcException)
   {
      this.rpcException = rpcException;
      this.returnValue = null; 
      this.params = null;
   }
   
   /**
    * Creates a new RemoteProcedureCallResponse, representing an normal method return.
    */
   public RemoteProcedureCallResponse(Object returnValue, Object[] outParams)
   {
      this.rpcException = null;
      this.returnValue = returnValue; 
      this.params = outParams;
   }
   
   /**
    * Gets the RpcException. Null is returned if no exception is to be thrown.
    */
   public RpcException getRpcException()
   {
      return rpcException;
   }
   
   /**
    * Sets the RpcException. Null is returned if no exception is to be thrown.
    */
   public void setRpcException(final RpcException rpcException)
   {
      this.rpcException = rpcException; 
   }
   
   /**
    * Gets the return value.
    */
   public Object getReturnValue()
   {
      return returnValue;
   }

   /**
    * Sets the return value.
    */
   public void setReturnValue(Object returnValue)
   {
      this.returnValue = returnValue;
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
    * Serialization (Externalizable) method.
    * 
    * @param out the stream on which to serialize objects of this class. 
    */
   public void writeExternal(final ObjectOutput out) throws IOException
   {
      // Write command version
      out.writeByte(SERIAL_DATA_VERSION);
      
      out.writeObject(this.rpcException);
      out.writeObject(this.returnValue);
      out.writeObject(this.params);
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
      
      this.rpcException = (RpcException)in.readObject();
      this.returnValue = in.readObject();
      this.params = (Object[])in.readObject();
   }
   
   /**
    * Gets a string representation of this object.
    * 
    * @since 2.1.3 (20060324)
    */
   public String toString()
   {
      return "RemoteProcedureCallResponse(returnValue: " + this.returnValue + ", rpcException: " + rpcException + ", params(out): " + StringUtils.limitStringLength(StringUtils.toString(this.params), 128) + ")";
   }
}
