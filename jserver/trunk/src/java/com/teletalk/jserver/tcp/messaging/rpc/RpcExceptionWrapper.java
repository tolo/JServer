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

/**
 * Wrapper class for RpcException used to make is possible to throw RpcExceptions as RuntimeExceptions. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0.1 (20050211)
 */
public class RpcExceptionWrapper extends RuntimeException
{
   static final long serialVersionUID = 2549275290766148638L;

   private final RpcException rpcException;
   
   /**
    * Creates a new RpcExceptionWrapper that wraps the specified RpcException. The cause of this exception will be the cause of the RpcException.
    */
   public RpcExceptionWrapper(RpcException rpcException)
   {
      super(rpcException.getCause());
      
      this.rpcException = rpcException;
   }
   
   /**
    * Gets the wrapped RpcException.
    */
   public RpcException getRpcException()
   {
      return rpcException;
   }
   
   /* ### DELEGATE CONVENICENCE METHODS ### */
   
   /**
    * Gets the error code of this RpcExceptionWrapper. The values -1000 to -2000 are reserved for internal error codes.
    */
   public long getErrorCode()
   {
      return rpcException.getErrorCode();
   }
   
   /**
    * Checks if this exception represents an internal error, i.e. has a code between -1000 and -2000.
    */
   public boolean isInternalError()
   {
      return rpcException.isInternalError();  
   }
   
   /**
    * Gets a String representation of this RpcExceptionWrapper.
    */
   public String toString()
   {
      return this.getClass().getName() + " (" + this.getErrorCode() +": " + this.rpcException.getMessage() + ")";
   }
}
