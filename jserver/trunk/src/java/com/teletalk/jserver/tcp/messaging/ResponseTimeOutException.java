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

/**
 * Exception class used to indicate that a timeout occurred while waiting for a response to a 
 * message.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.2
 */
public class ResponseTimeOutException extends MessagingException
{
   static final long serialVersionUID = 6588933639118666219L;

	/**
	 * Constructs a new ResponseTimeOutException.
	 */
	public ResponseTimeOutException()
	{
		super();
	}

	/**
	 * Constructs a new ResponseTimeOutException.
	 * 
	 * @param s the detail message.
	 */
	public ResponseTimeOutException(String s)
	{
		super(s);
	}
	
	/**
	 * Constructs a new ResponseTimeOutException.
	 * 
	 * @param cause the cause.
	 */
	public ResponseTimeOutException(Throwable cause)
	{
		super(cause);
	}
	
	/**
	 * Constructs a new ResponseTimeOutException.
	 * 
	 * @param s the detail message.
	 * @param cause the cause.
	 */
	public ResponseTimeOutException(String s, Throwable cause)
	{
		super(s, cause);
	}
}
