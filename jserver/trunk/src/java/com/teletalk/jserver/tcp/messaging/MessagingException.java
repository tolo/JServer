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

import java.io.Serializable;

import com.teletalk.jserver.util.JavaBugUtils;

/**
 * Base class for all exceptions thrown by MessagingManager when dispatching messages. 
 *  
 * @author Tobias Löfstrand
 * 
 * @since 1.3
 */
public class MessagingException extends RuntimeException
{
   static final long serialVersionUID = -1053969256028778820L;
   
	private interface GetCauseImplementation
	{
		public Throwable getCause();
	}
	
	private class GetCauseImplementation_v_1_3 implements GetCauseImplementation, Serializable
	{
      static final long serialVersionUID = -1934572039128949325L;
      
		private final Throwable cause;
		
		public GetCauseImplementation_v_1_3(Throwable cause)
		{
			this.cause = cause;
		}
		
		public Throwable getCause()
		{
			return cause;
		}
	}
	
	private class GetCauseImplementation_v_1_4 implements GetCauseImplementation, Serializable
	{
      static final long serialVersionUID = -6444626654348609816L;
      
		private final Throwable cause;
		
		public GetCauseImplementation_v_1_4(MessagingException messagingException, Throwable cause)
		{
			if( cause != null ) messagingException.initCause(cause);
			this.cause = cause;
		}
		
		public Throwable getCause()
		{
			return cause;
		}
	}
	
	private final GetCauseImplementation getCauseImplementation;
	
   /**
    * Creates a new MessagingException.
    */
   public MessagingException()
   {
		this(null, null);      
   }

   /**
    * @param message
    */
   public MessagingException(String message)
   {
      this(message, null);
   }
 
   /**
    * @param cause
    */
   public MessagingException(Throwable cause)
   {
      this(null, cause);
   }
   
   /**
    * @param message
  	 * @param cause
    */
   public MessagingException(String message, Throwable cause)
   {
      super(message);
      
		// Attempt to use the java 1.4 way of handling cause
		if( JavaBugUtils.isJavaVersionOrHigher(1,4) )
		{
			this.getCauseImplementation = new GetCauseImplementation_v_1_4(this, cause);
		}
		else 
		{		
			this.getCauseImplementation = new GetCauseImplementation_v_1_3(cause);
		}
   }

   /**
    * 
    */
   public Throwable getCause()
   {
		return this.getCauseImplementation.getCause();
   }
}
