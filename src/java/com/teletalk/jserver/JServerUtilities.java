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

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;

import com.teletalk.jserver.log.ComponentLogger;

/**
 * This class contains various utility methods that are useful for classes that doesn't 
 * inherit SubSystem or SubComponent.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public final class JServerUtilities
{
   /**
    * Convenience method to log a trace (finer-grained than debug) message.
    * 
    * @param message the message to log.
    * 
    * @since 2.1.4 (20060511)
    */
   public static final void logTrace(String message)
   {
      log(Level.TRACE_INT, message);
   }
   
   /**
    * Convenience method to log a trace (finer-grained than debug) message.
    * 
    * @param origin the origin of the log message.
    * @param message the message to log.
    * 
    * @since 2.1.4 (20060511)
    */
   public static final void logTrace(String origin, String message)
   {
      log(Level.TRACE_INT, origin, message);
   }
   
   /**
    * Convenience method to log a debug message.
    * 
    * @param message the message to log.
    * 
    * @since 2.0
    */
   public static final void logDebug(String message)
   {
      log(Level.DEBUG_INT, message);
   }
   
	/**
	 * Convenience method to log a debug message.
	 * 
    * @param origin the origin of the log message.
	 * @param message the message to log.
	 */
	public static final void logDebug(String origin, String message)
	{
		log(Level.DEBUG_INT, origin, message);
	}
   
   /**
    * Convenience method to log an info message.
    * 
    * @param message the message to log.
    * 
    * @since 2.0
    */
   public static final void logInfo(String message)
   {
      log(Level.INFO_INT, message);
   }
	
	/**
	 * Convenience method to log an info message.
	 * 
	 * @param origin the origin of the log message.
    * @param message the message to log.
	 */
	public static final void logInfo(String origin, String message)
	{
		log(Level.INFO_INT, origin, message);
	}
   
   /**
    * Convenience method to log a warning message.
    * 
    * @param message the message to log.
    * 
    * @since 2.0
    */
   public static final void logWarning(String message)
   {
      log(Level.WARN_INT, message);
   }
	
	/**
	 * Convenience method to log a warning message.
	 * 
	 * @param origin the origin of the log message.
    * @param message the message to log.
	 */
	public static final void logWarning(String origin, String message)
	{
		log(Level.WARN_INT, origin, message);
	}
      
   /**
    * Convenience method to log a warning message.
    * 
    * @param message the message to log.
    * @param error an error to be logged.
    * 
    * @since 2.0
    */
   public static final void logWarning(String message, Throwable error)
   {
      log(Level.WARN_INT, message, error);
   }
	
	/**
	 * Convenience method to log a warning message.
	 * 
	 * @param origin the origin of the log message.
    * @param message the message to log.
    * @param error an error to be logged.
	 */
	public static final void logWarning(String origin, String message, Throwable error)
	{
		log(Level.WARN_INT, origin, message, error);
	}
   
   /**
    * Convenience method to log an error message.
    * 
    * @param message the message to log.
    * 
    * @since 2.0
    */
   public static final void logError(String message)
   {
      log(Level.ERROR_INT, message);
   }
	
	/**
	 * Convenience method to log an error message.
	 * 
	 * @param origin the origin of the log message.
    * @param message the message to log.
	 */
	public static final void logError(String origin, String message)
	{
		log(Level.ERROR_INT, origin, message);
	}
   
   /**
    * Convenience method to log an error message.
    * 
    * @param message the message to log.
    * @param error an error to be logged.
    * 
    * @since 2.0
    */
   public static final void logError(String message, Throwable error)
   {
      log(Level.ERROR_INT, message, error);
   }
	
	/**
	 * Convenience method to log an error message.
	 * 
	 * @param origin the origin of the log message.
    * @param message the message to log.
    * @param error an error to be logged.
	 */
	public static final void logError(String origin, String message, Throwable error)
	{
		log(Level.ERROR_INT, origin, message, error);
	}
   
   /**
    * Convenience method to log a fatal error message.
    * 
    * @param message the message to log.
    * 
    * @since 2.0
    */
   public static final void logCriticalError(String message)
   {
      log(Level.FATAL_INT, message);
   }
	
	/**
	 * Convenience method to log a fatal error message.
	 * 
	 * @param origin the origin of the log message.
    * @param message the message to log.
	 */
	public static final void logCriticalError(String origin, String message)
	{
		log(Level.FATAL_INT, origin, message);
	}
   
   /**
    * Convenience method to log a fatal error message.
    * 
    * @param message the message to log.
    * @param logMessageId the id associated with the log message.
    * 
    * @since 2.0
    */
   public static final void logCriticalError(String message, int logMessageId)
   {
      log(Level.FATAL_INT, message, logMessageId);
   }
	
	/**
	 * Convenience method to log a fatal error message.
	 * 
	 * @param origin the origin of the log message.
    * @param message the message to log.
	 * @param logMessageId the id associated with the log message.
	 */
	public static final void logCriticalError(String origin, String message, int logMessageId)
	{
		log(Level.FATAL_INT, origin, message, logMessageId);
	}
   
   /**
    * Convenience method to log a fatal error message.
    * 
    * @param message the message to log.
    * @param error an error to be logged.
    * 
    * @since 2.0
    */
   public static final void logCriticalError(String message, Throwable error)
   {
      log(Level.FATAL_INT, message, error);
   }
	
	/**
	 * Convenience method to log a fatal error message.
	 * 
	 * @param origin the origin of the log message.
    * @param message the message to log.
    * @param error an error to be logged.
	 */
	public static final void logCriticalError(String origin, String message, Throwable error)
	{
		log(Level.FATAL_INT, origin, message, error);
	}
   
   /**
    * Convenience method to log a fatal error message.
    * 
    * @param message the message to log.
    * @param error an error to be logged. 
    * @param logMessageId the id associated with the log message.
    * 
    * @since 2.0
    */
   public static final void logCriticalError(String message, Throwable error, int logMessageId)
   {
      log(Level.FATAL_INT, message, error, logMessageId);
   }
	
	/**
	 * Convenience method to log a fatal error message.
	 * 
	 * @param origin the origin of the log message.
    * @param message the message to log.
    * @param error an error to be logged. 
	 * @param logMessageId the id associated with the log message.
	 */
	public static final void logCriticalError(String origin, String message, Throwable error, int logMessageId)
	{
		log(Level.FATAL_INT, origin, message, error, logMessageId);
	}
   
   /**
    * Convenience method to log a message.
    * 
    * @param level the log level (<i><b>Note</b>: Since version 2.0 this parameter is interpreted as the Log4J level</i>)..
    * @param message the message to log.
    * 
    * @since 2.0
    */
   public static final void log(int level, String message)
   {
      log(level, message, (Throwable)null, JServerConstants.LOG_MESSAGE_ID_UNSPECIFIED);
   }
	
	/**
	 * Convenience method to log a message.
	 * 
	 * @param level the log level (<i><b>Note</b>: Since version 2.0 this parameter is interpreted as the Log4J level</i>)..
	 * @param origin the origin of the log message.
    * @param message the message to log.
	 */
	public static final void log(int level, String origin, String message)
	{
		log(level, origin, message, null, JServerConstants.LOG_MESSAGE_ID_UNSPECIFIED);
	}
  
   /**
    * Convenience method to log a message.
    * 
    * @param level the log level (<i><b>Note</b>: Since version 2.0 this parameter is interpreted as the Log4J level</i>)..
    * @param message the message to log.
    * @param logMessageId the id associated with the log message.
    * 
    * @since 2.0
    */
   public static final void log(int level, String message, int logMessageId)
   {
      log(level, message, (Throwable)null, logMessageId);
   }
	
	/**
	 * Convenience method to log a message.
	 * 
	 * @param level the log level (<i><b>Note</b>: Since version 2.0 this parameter is interpreted as the Log4J level</i>)..
	 * @param origin the origin of the log message.
    * @param message the message to log.
	 * @param logMessageId the id associated with the log message.
	 */
	public static final void log(int level, String origin, String message, int logMessageId)
	{
      log(level, origin, message, null, logMessageId);
	}
   
   /**
    * Convenience method to log a message.
    * 
    * @param level the log level (<i><b>Note</b>: Since version 2.0 this parameter is interpreted as the Log4J level</i>)..
    * @param message the message to log.
    * @param error an error to be logged.
    * 
    * @since 2.0
    */
   public static final void log(int level, String message, Throwable error)
   {
      ComponentLogger.getTopSystemLogger().log(Level.toLevel(level), message, error);
   }
   
   /**
    * Convenience method to log a message.
    * 
    * @param level the log level (<i><b>Note</b>: Since version 2.0 this parameter is interpreted as the Log4J level</i>)..
    * @param origin the origin of the log message.
    * @param message the message to log.
    * @param error an error to be logged.
    * 
    * @since 2.0
    */
   public static final void log(int level, String origin, String message, Throwable error)
   {
      Logger logger;
      
      if( origin != null )
      {
         if( !origin.startsWith(JServerConstants.JSERVER_TOP_SYSTEM_ALIAS + ".") ) origin = JServerConstants.JSERVER_TOP_SYSTEM_ALIAS + "." + origin; 
         logger = Logger.getLogger(origin);
      }
      else logger = ComponentLogger.getTopSystemLogger();
      
      logger.log(Level.toLevel(level), message, error);
   }
   
   /**
    * Convenience method to log a message.
    * 
    * @param level the log level (<i><b>Note</b>: Since version 2.0 this parameter is interpreted as the Log4J level</i>)..
    * @param message the message to log.
    * @param error an error to be logged.
    * @param logMessageId the id associated with the log message.
    * 
    * @since 2.0
    */
   public static final void log(int level, String message, Throwable error, int logMessageId)
   {
      if( logMessageId != JServerConstants.LOG_MESSAGE_ID_UNSPECIFIED ) MDC.put(JServerConstants.LOG_MESSAGE_ID_KEY, new Integer(logMessageId));
      ComponentLogger.getTopSystemLogger().log(Level.toLevel(level), message, error);
      if( logMessageId != JServerConstants.LOG_MESSAGE_ID_UNSPECIFIED ) MDC.remove(JServerConstants.LOG_MESSAGE_ID_KEY);
   }
   
   /**
    * Convenience method to log a message.
    * 
    * @param level the log level (<i><b>Note</b>: Since version 2.0 this parameter is interpreted as the Log4J level</i>)..
    * @param origin the origin of the log message.
    * @param message the message to log.
    * @param error an error to be logged.
    * @param logMessageId the id associated with the log message.
    * 
    * @since 2.0
    */
   public static final void log(int level, String origin, String message, Throwable error, int logMessageId)
   {
      Logger logger;
      
      if( origin != null )
      {
         if( !origin.startsWith(JServerConstants.JSERVER_TOP_SYSTEM_ALIAS + ".") ) origin = JServerConstants.JSERVER_TOP_SYSTEM_ALIAS + "." + origin; 
         logger = Logger.getLogger(origin);
      }
      else logger = ComponentLogger.getTopSystemLogger();
      
      if( logMessageId != JServerConstants.LOG_MESSAGE_ID_UNSPECIFIED ) MDC.put(JServerConstants.LOG_MESSAGE_ID_KEY, new Integer(logMessageId));
      logger.log(Level.toLevel(level), message, error);
      if( logMessageId != JServerConstants.LOG_MESSAGE_ID_UNSPECIFIED ) MDC.remove(JServerConstants.LOG_MESSAGE_ID_KEY);      
   }
	
	/**
	 */
	private static int getNextAtIndex(String str, int beginIndex)
	{
		String lowerCaseString = str.toLowerCase();
		int index = lowerCaseString.indexOf("at", beginIndex);
		
		while(index >= 0)
		{
			if( (index+2) < str.length() )
			{
				char charAfterAt = str.charAt(index+2);
			
				if( Character.isWhitespace(charAfterAt) || (charAfterAt == (char)0xA0) )
				{
					// "at " found!
					break;
				}
				else
				{
					index = lowerCaseString.indexOf("at", index+2);
				}
			}
			else index = -1;
		}
		
		return index;
	}
	
	/**
	 * Gets a stack trace for the current thread.
	 * 
	 * @return a String containing a stack trace.
	 */
	public static String getStackTrace()
	{
		try
		{
			final StringWriter sw = new StringWriter();
			new Throwable().printStackTrace(new PrintWriter(sw));
		
			String callStack = sw.toString();
		
			//Exclude the last method call (this method) from the trace
			int atPos = getNextAtIndex(callStack, 0);
			callStack = callStack.substring(atPos + 3);
			atPos = getNextAtIndex(callStack, 0);
			if(callStack.indexOf("java.lang.Throwable") >= 0) // If still trace inside the dummy java.lang.Throwable object
			{
				callStack = callStack.substring(atPos + 3);
				atPos = getNextAtIndex(callStack, 0);
			}
						
			//return "Stack trace: " + callStack.substring(atPos + 3).trim() + ".";
			return callStack.substring(atPos + 3).trim();
		}
		catch(Exception e)
		{
			return "Error occurred while getting stack trace (" + e + ")";
		}
	}
	
	/**
	 * Gets the specified entry in the current stack trace. The parameter <code>entry</code> specifies 
	 * which entry to get, with 0 beeing the last in the stack trace (or the first to be printed in a call to the method Throwable.printStackTrace()).
	 * 
	 * @param entry the stack trace entry to get.
	 * 
	 * @return the stack trace entry.
	 * 
	 * @since 1.1 Build 561
	 */
	public static String getStackTraceEntry(final int entry)
	{
		String callStack = getStackTrace();
		
		int entryBeginIndex = getNextAtIndex(callStack, 0)+3; // Skip this method call // = callStack.indexOf( "at" ); // Get position of first "at" in stack trace
		int nextAtIndex = 0;
		String stackTraceEntry = null;
		
		for(int i=0; (nextAtIndex >= 0) && (i <= entry); i++)
		{
			nextAtIndex = getNextAtIndex(callStack, entryBeginIndex);//callStack.indexOf( "at ", entryBeginIndex); // Get position of next "at" in stack trace
			
			if(i == entry)
			{
				if(nextAtIndex >= 0)
				{
					stackTraceEntry = callStack.substring(entryBeginIndex, nextAtIndex).trim();
				}
				else
				{
					stackTraceEntry = callStack.substring(entryBeginIndex).trim();
				}
			}
			
			entryBeginIndex = nextAtIndex + 3; // Skip to beginning of next entry
		}
		
		return stackTraceEntry;
	}
	
	/**
	 * Gets the name of the class that called the method that called this method.  <br>
	 * <br>
	 * Example: <br>
	 * <br>
	 * <code>
	 * ...<br>
	 * class C1<br>
	 * {<br>
	 * &nbsp;&nbsp;&nbsp;public static void method1()<br>
	 * &nbsp;&nbsp;&nbsp;{<br>
	 *	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;C2.method2();<br>
	 * &nbsp;&nbsp;&nbsp;}<br>
	 * }<br>
	 * <br>
	 * ...<br>
	 * <br>
	 * class C2<br>
	 * {<br>
	 * &nbsp;&nbsp;&nbsp;public static void method2()<br>
	 * &nbsp;&nbsp;&nbsp;{<br>
	 *	&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;System.out.println(JServerUtilities.getCallerClassName());<br>
	 * &nbsp;&nbsp;&nbsp;}<br>
	 * }<br>
	 * ...<br>
	 * </code>
	 * <br>
	 * The ouput from the above program will be: <code>C1</code><br>
	 * <br>
	 * 
	 * @return the caller class name.
	 * 
	 * @since 1.1 Build 561
	 */
	public static String getCallerClassName()
	{
		String previousStackTraceEntry = getStackTraceEntry(2); // We need to skip this method call too...
		
		if(previousStackTraceEntry != null)
		{
			int dotPos = previousStackTraceEntry.indexOf(".");
			if(dotPos > 0)
				return previousStackTraceEntry.substring(0, dotPos);
			else
				return previousStackTraceEntry;
		}
		else return "Unknown";
	}
	
	/**
	 * Gets the name of the method that called the method that called this method. <br>
	 * <br>
	 * Example: <br>
	 * <br>
	 * <code>
	 * ...<br>
	 * public static void method1()<br>
	 * {<br>
	 *	 &nbsp;&nbsp;&nbsp;method2();<br>
	 * }<br>
	 * <br>
	 * public static void method2()<br>
	 * {<br>
	 *	 &nbsp;&nbsp;&nbsp;System.out.println(JServerUtilities.getCallerMethodName());<br>
	 * }<br>
	 * ...<br>
	 * </code>
	 * <br>
	 * The ouput from the above program will be: <code>method1</code><br>
	 * <br>
	 * 
	 * @return the caller method name.
	 * 
	 * @since 1.1 Build 561
	 */
	public static String getCallerMethodName()
	{
		String previousStackTraceEntry = getStackTraceEntry(2); // We need to skip this method call too...
		
		if(previousStackTraceEntry != null)
		{
			int parenthesisIndex = previousStackTraceEntry.indexOf("(");
			if(parenthesisIndex > 0)
			{
				previousStackTraceEntry = previousStackTraceEntry.substring(0, parenthesisIndex);
				
				int lastDotPos = previousStackTraceEntry.lastIndexOf(".");
				if(lastDotPos > 0)
					return previousStackTraceEntry.substring(lastDotPos+1);
				else
					return previousStackTraceEntry;
			}
			else
				return previousStackTraceEntry;
		}
		else return "Unknown";
	}
}
