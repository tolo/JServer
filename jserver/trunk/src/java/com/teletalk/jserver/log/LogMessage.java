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
package com.teletalk.jserver.log;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * A message class for logging.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 * 
 * @deprecated as of 2.0. This class only exists to enable backwards compatability in JAdmin.
 */
public class LogMessage implements Serializable
{
	/** The serial version id of this class. */
	static final long serialVersionUID = -6455805099042721562L;
	
	/** Constant for the loglevel debug. */
	public static final int DEBUG_LEVEL = 0;
	
	/** Constant for the loglevel info. */
	public static final int INFO_LEVEL = 1;
	
	/** Constant for the loglevel warning. */
	public static final int WARNING_LEVEL = 2;
	
	/** Constant for the loglevel error. */
	public static final int ERROR_LEVEL = 3;
	
	/** Constant for the loglevel critical error. This level is reserved for critical errors in subsystems, and particularly for key subsystems. */
	public static final int CRITICAL_ERROR_LEVEL = 4;
			
	/** The loglevel of this LogMessage. <i>Warning: This field will not be publically accessible in comming versions. Please used method {@link #getLevel()} instead.</i>*/
	public final int level;
	
	/** The time when this LogMessage was created. <i>Warning: This field will not be publically accessible in comming versions. Please used method {@link #getTime()} instead.</i>*/
	public final java.util.Date time;
	
	/** The place where this LogMessage originated. <i>Warning: This field will not be publically accessible in comming versions. Please used method {@link #getOrigin()} instead.</i>*/
	public final String origin;
	
	/** The actual message. <i>Warning: This field will not be publically accessible in comming versions. Please used method {@link #getMessage()} instead.</i>*/
	public final String msg;
	
	/**	 */
	private final int logMessageId;
	
	/** The value is used to indicate that a log message id has not been specified. This constant has the value of Integer.MIN_VALUE, which is to be considered a reserved value.  */
	public static final int LOG_MESSAGE_ID_UNSPECIFIED = Integer.MIN_VALUE;
	
	/**
	 * Constructs a new LogMessage. This constructor will set the log message id to {@link #LOG_MESSAGE_ID_UNSPECIFIED}.
	 * 
	 * @param level the loglevel.
	 * @param time the time when this LogMessage was created.
	 * @param origin the place where this LogMessage originated.
	 * @param msg the actual message.
	 */
	public LogMessage(int level, java.util.Date time, String origin, String msg)
	{
		this.level = level;
		this.time = time;		
		this.origin = (origin != null) ? origin.intern() : null;
		this.msg = msg;
		this.logMessageId = LogMessage.LOG_MESSAGE_ID_UNSPECIFIED;
	}
	
	/**
	 * Constructs a new LogMessage.
	 * 
	 * @param level the loglevel.
	 * @param time the time when this LogMessage was created.
	 * @param origin the place where this LogMessage originated.
	 * @param msg the actual message.
	 * @param logMessageId the id associated with this log message. The range 4095-65535 is reserved for JServer.
	 */
	public LogMessage(int level, java.util.Date time, String origin, String msg, int logMessageId)
	{
		this.level = level;
		this.time = time;		
		this.origin = (origin != null) ? origin.intern() : null;
		this.msg = msg;
		this.logMessageId = logMessageId;
	}
	
	/**
	 * Gets the level of this log message (for instance INFO_LEVEL).
	 * 
	 * @return an integer representing the level of this log message.
	 */
	public final int getLevel()
	{
		return level;
	}
	
	/**
	 * Gets the time field of this log message.
	 * 
	 * @return a Data object contatining the time of this log message
	 */
	public final Date getTime()
	{
		return time;
	}
	
	/**
	 * Gets the origin field of this log message. This field could for instance be 
	 * set tp the name of the subsystem from which this log message originated.
	 * 
	 * @return a String describing the origin of this log message.
	 */
	public final String getOrigin()
	{
		return origin;
	}
	
	/**
	 * Gets the actual message contained in this log message.
	 * 
	 * @return the message.
	 */
	public final String getMessage()
	{
		return msg;
	}
	
	/**
	 * Returns a String representing the log time of this LogMessage on the form "yyyy-MM-dd HH:mm:ss".
	 * 
	 * @return String representation of the log time.
	 */
	public final String getTimeString()
	{
		SimpleDateFormat dateFormat = (SimpleDateFormat)DateFormat.getDateTimeInstance();
		dateFormat.applyPattern("yyyy-MM-dd HH:mm:ss");
		
		return dateFormat.format(time);
	}
	
	/**
	 * Gets the id associated with this LogMessage. The range 4095-65535 is reserved for JServer.
	 * 
	 * @return the id associated with this LogMessage. The range 4095-65535 is reserved for JServer.
	 */
	public final int getLogMessageId()
	{
		return this.logMessageId;
	}
	
	/** 
	 * Returns a String representation of this LogMessage.
	 * 
	 * @return a String describing this LogMessage.
	 */
	public String toString()
	{
		if( this.logMessageId == LogMessage.LOG_MESSAGE_ID_UNSPECIFIED )
			return "LogMessage(" + (this.level + ", " + this.getTimeString() + ", " + this.origin + ", " + this.msg) + ")";
		else 
			return "LogMessage(" + (this.level + ", " + this.getTimeString() + ", " + this.origin + ", " + this.msg) +", " + this.logMessageId + ")";
	}
}
