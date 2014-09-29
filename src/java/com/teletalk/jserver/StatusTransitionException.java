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
package com.teletalk.jserver;

import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * Thrown to indicate that an error occured during state transition. This exception class is to be used in the 
 * state transition methods of class {@link SubSystem} (doXXX methods) to signal that an error has occurred.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public class StatusTransitionException extends RuntimeException
{
   static final long serialVersionUID = -6374038665434700815L;

	/** String containing a stack trace. */
	protected final String stackTrace;
	
	/**
	 * Creates a new StatusTransitionException with no message.
	 */
	public StatusTransitionException()
	{
		super();
		stackTrace = null;
	}
	
	/**
	 * Creates a new StatusTransitionException.
	 * 
	 * @param msg the message of the exception.
	 */
	public StatusTransitionException(String msg)
	{
		super(msg);
		stackTrace = null;
	}
		
	/**
	 * Creates a new StatusTransitionException.
	 * 
	 * @param msg the message of the exception.
	 * @param error an exception that should be included in the message of this StatusTransitionException.
	 */
	public StatusTransitionException(String msg, Throwable error)
	{
		super(msg + " - " + error + ".");
		stackTrace = getStackTrace(error);
	}
	
	/**
	 * Gets extended information about this StatusTransitionException, possibly a stack trace. If the extendedMessage member is <code>null</code>
	 * this method will return the result of a call to <code>super.getMessage()</code>.
	 * 
	 * @return a string containing a stack trace.
	 */
	public String getExtendedMessage()
	{
		if(stackTrace != null)
			return stackTrace;
		else
			return super.getMessage();
	}
	
	/**
	 * Gets the stacktrace from an exeception.
	 * 
	 * @param error an exception.
	 */
	public static final String getStackTrace(Throwable error)
	{
		StringWriter strWriter = new StringWriter();
		error.printStackTrace(new PrintWriter(strWriter));
		
		return strWriter.toString();
	}
}
