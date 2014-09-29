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
package com.teletalk.jserver.util.exception;

/**
 * RuntimeException subclass that contains an error code.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1
 */
public class ErrorCodeException extends RuntimeException
{
   static final long serialVersionUID = -1823390412577600992L;

   private final long errorCode;
   
   /**
    * Creates a new ErrorCodeException using 0 as error code.
    * 
    * @param message the error message.
    */
   public ErrorCodeException(String message)
   {
      this(0, message);
   }
   
   /**
    * Creates a new ErrorCodeException.
    * 
    * @param errorCode the error code of this ErrorCodeException.
    * @param message the error message.
    */
   public ErrorCodeException(long errorCode, String message)
   {
      super(message);
      
      this.errorCode = errorCode;
   }

   /**
    * Creates a new ErrorCodeException.
    * 
    * @param errorCode the error code of this ErrorCodeException.
    * @param message the error message.
    * @param cause the cause of the exception.
    */
   public ErrorCodeException(long errorCode, String message, Throwable cause)
   {
      super(message, cause);
      
      this.errorCode = errorCode;
   }
   
   /**
    * Gets the error code of this ErrorCodeException.
    */
   public long getErrorCode()
   {
      return errorCode;
   }
   
   /**
    * Gets a String representation of this ErrorCodeException.
    */
   public String toString()
   {
      return this.getClass().getName() + " (" + this.errorCode + " - " + super.getLocalizedMessage() + ")";
   }
}
