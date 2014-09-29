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
 * Exception class for RCP errors. This class contains a field to be used for RCP specific error codes.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.1 Build 690
 */
public class RpcException extends Exception
{
   static final long serialVersionUID = 2213699570697819371L;

   /*private static final ThreadLocal LastRpcException = new ThreadLocal();
   
   public static void setLastRpcException(RpcException rpcException)
   {
      LastRpcException.set(rpcException);
   }
   
   public static RpcException getLastRpcException()
   {
      return (RpcException)LastRpcException.get();
   }*/
   
   
   public static final long MISC_INTERNAL_ERROR = -1000;
   
   public static final long RESPONSE_TIMEOUT = -1001;
   
   public static final long MESSAGE_DISPATCH_FAILURE = -1002;
   
   public static final long HANDLER_NOT_FOUND = -1003;
   
   public static final long METHOD_NOT_FOUND = -1004;
   
   public static final long JAVA_LANG_OBJECT_METHOD_CALLED = -1005;
   
   public static final long METHOD_INVOCATION_ERROR = -1006;
      
   public static final long ACCESS_DENIED = -1100;
   
   
   private final long errorCode;
   
   /**
    * Creates a new RpcException using 0 as error code.
    * 
    * @param message the error message.
    */
   public RpcException(String message)
   {
      this(0, message);
   }
   
   /**
    * Creates a new RpcException.
    * 
    * @param errorCode the error code of this RpcException. The values -1000 to -2000 are reserved for internal error codes.
    * @param message the error message.
    */
   public RpcException(long errorCode, String message)
   {
      super(message);
      
      this.errorCode = errorCode;
   }

   /**
    * Creates a new RpcException.
    * 
    * @param errorCode the error code of this RpcException. The values -1000 to -2000 are reserved for internal error codes.
    * @param message the error message.
    * @param cause the cause of the exception.
    */
   public RpcException(long errorCode, String message, Throwable cause)
   {
      super(message, cause);
      
      this.errorCode = errorCode;
   }
   
   /**
    * Gets the error code of this RpcException. The values -1000 to -2000 are reserved for internal error codes.
    */
   public long getErrorCode()
   {
      return errorCode;
   }
   
   /**
    * Checks if this exception represents an internal error, i.e. not user defined (code between -1000 and -2000).
    * 
    * @since 1.3.2.
    */
   public boolean isInternalError()
   {
      return ( (this.errorCode >= -2000) && (this.errorCode <= -1000) );  
   }
   
   /**
    * Gets a String representation of this RpcException.
    */
   public String toString()
   {
      return this.getClass().getName() + " (" + this.errorCode +"): " + super.getMessage() + ")";
   }
}
