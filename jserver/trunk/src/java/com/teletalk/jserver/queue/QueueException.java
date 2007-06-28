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
package com.teletalk.jserver.queue;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Thrown to indicate that a queue related error has occured.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public class QueueException extends RuntimeException
{
   static final long serialVersionUID = -4660973223670276451L;

	/**
	 * Createas a new QueueException without any message.
	 */
	public QueueException()
	{
		super();
	}
	
	/**
	 * Createas a new QueueException.
	 * 
	 * @param msg the message of the exception.
	 */
	public QueueException(String msg)
	{
		super(msg);
	}
	
	/**
	 * Createas a new QueueException.
	 * 
	 * @param msg the message of the exception.
	 * @param error an exception that should be included in the message of this QueueException.
	 */
	public QueueException(String msg, Exception error)
	{
		super(msg + " - " + getStackTrace(error));
	}
	
	/**
	 * Gets the stacktrace from an exeception.
	 * 
	 * @param error an exception.
	 */
	public static String getStackTrace(Throwable error)
	{
		StringWriter strWriter = new StringWriter();
		error.printStackTrace(new PrintWriter(strWriter));
		
		return strWriter.toString();
	}
}
