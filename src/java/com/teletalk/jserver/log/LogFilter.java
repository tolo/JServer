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
import java.util.Date;

import com.teletalk.jserver.util.Constraint;
import com.teletalk.jserver.util.Filter;

/**
 * A filter class for Filtering LogMessages. This class uses an instance of class Filter for
 * each field in class LogMessage. It is possible to specify which logical operator (AND or OR)
 * that should be used between fields when filtering. This can either be done in the constructor
 * or by using the method {@link #setOperator(boolean operator)}.
 * 
 * @see com.teletalk.jserver.util.Filter
 * @see LogMessage
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 * 
 * @deprecated as of 2.0. This class only exists to enable backwards compatability in JAdmin.
 */
public final class LogFilter implements Serializable
{
	static final long serialVersionUID = -3024327571652173528L;
	
	/** Constant used to get a logical AND function of this LogFiler. */
	public static final boolean AND_OPERATOR = Filter.AND_OPERATOR;
	
	/** Constant used to get a logical OR function of this LogFiler. */
	public static final boolean OR_OPERATOR = Filter.OR_OPERATOR;
	
	/** Constant for the LogMessage field level. */
	public final static String LEVEL_FIELD = "level";
	
	/** Constant for the LogMessage field time. */
	public final static String TIME_FIELD = "time";
	
	/** Constant for the LogMessage field origin. */
	public final static String ORIGIN_FIELD = "origin";
	
	/** Constant for the LogMessage field msg. */
	public final static String MSG_FIELD = "msg";
	
	//private Hashtable filters;
	private final Filter levelFilter;
	private final Filter timeFilter;
	private final Filter originFilter;
	private final Filter msgFilter;
	
	private boolean operator;
	
	/**
	 * Constructs a new LogFiler with a logical AND operator between fields.
	 */
	public LogFilter()
	{
		this(AND_OPERATOR);
	}
	
	/**
	 * Constructs a new LogFiler.
	 * 
	 * @param operator the logical operator to be used between fields when filtering.
	 */
	public LogFilter(boolean operator)
	{
		this.operator = operator;
			
		levelFilter = new Filter();
		timeFilter = new Filter();
		originFilter = new Filter();
		msgFilter = new Filter();
	}
	
	/**
	 * Adds a filtering contraint to the specified field.
	 * 
	 * @param field the field which the constraint should apply to.
	 * @param constraint the contraint.
	 * 
	 * @see com.teletalk.jserver.util.Constraint
	 */
	public synchronized void addConstraint(String field, Constraint constraint)
	{
		if(field.equals(LEVEL_FIELD))
			levelFilter.addConstraint(constraint);
		else if(field.equals(TIME_FIELD))
			timeFilter.addConstraint(constraint);
		else if(field.equals(ORIGIN_FIELD))
			originFilter.addConstraint(constraint);
		else if(field.equals(MSG_FIELD))
			msgFilter.addConstraint(constraint);
		else
			throw new RuntimeException("Unknown field!");
	}
		
	/**
	 * Removes a filtering contraint from the specified field.
	 * 
	 * @param field the field which has the constraint.
	 * @param constraint the contraint.
	 * 
	 * @see com.teletalk.jserver.util.Constraint
	 */
	public synchronized void removeConstraint(String field, Constraint constraint)
	{
		if(field.equals(LEVEL_FIELD))
			levelFilter.removeConstraint(constraint);
		else if(field.equals(TIME_FIELD))
			timeFilter.removeConstraint(constraint);
		else if(field.equals(ORIGIN_FIELD))
			originFilter.removeConstraint(constraint);
		else if(field.equals(MSG_FIELD))
			msgFilter.removeConstraint(constraint);
		else
			throw new RuntimeException("Unknown field!");
	}
	
	/**
	 * Tests if a LogMessage passes this filter.
	 * 
	 * @param logMsg the LogMessage to be tested.
	 * 
	 * @return true if the LogMessage passes the filer, otherwise false.
	 * 
	 * @see LogMessage
	 */
	public synchronized boolean filterLogMessage(LogMessage logMsg)
	{
		return filterLogMessage(new Integer(logMsg.level), logMsg.time, logMsg.origin, logMsg.msg);
	}
	
	/**
	 * Tests if a LogMessage passes this filter.
	 * 
	 * @param level the level field of a LogMessage.
	 * @param time the time field of a LogMessage.
	 * @param origin the origin field of a LogMessage.
	 * @param msg the msg field of a LogMessage.
	 * 
	 * @return true if the LogMessage passes the filer, otherwise false.
	 * 
	 * @see LogMessage
	 */
	public synchronized boolean filterLogMessage(Integer level, Date time, String origin, String msg)
	{
		boolean result;
		
		if(operator == AND_OPERATOR)
		{
			result = true;

			if(!levelFilter.filterObject(level)) return false;
			
			if(!timeFilter.filterObject(time)) return false;
			
			if(!originFilter.filterObject(origin)) return false;
			
			if(!msgFilter.filterObject(msg)) return false;
		}
		else
		{
			result = false;
			
			if(levelFilter.filterObject(level)) return true;
			
			if(timeFilter.filterObject(time)) return true;
			
			if(originFilter.filterObject(origin)) return true;
			
			if(msgFilter.filterObject(msg)) return true;
		}
		
		return result;
	}
	
	/**
	 * Sets the logical operator to be used between fields when filtering.
	 * 
	 * @param operator the operator.
	 */
	public synchronized void setOperator(boolean operator)
	{
		levelFilter.setOperator(operator);
		timeFilter.setOperator(operator);
		originFilter.setOperator(operator);
		msgFilter.setOperator(operator);
		
		this.operator = operator;
	}
	
	/**
	 * Returns the names of the fields in class LogMessage.
	 * 
	 * @return String array.
	 */
	public static String[] getFields()
	{
		return new String[]{LEVEL_FIELD, TIME_FIELD, ORIGIN_FIELD, MSG_FIELD};	
	}
	
	/**
	 * Returns a String representation of this LogFiler.
	 * 
	 * @return String representation.
	 */
	public String toString()
	{
		return "LogFilter ( levelFilter: " + levelFilter + ", timeFilter: " + timeFilter + ", originFilter: " + originFilter + ", msgFilter: " + msgFilter + ")";
	}
}
