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
package com.teletalk.jserver.util;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * This class serves as a filter for arbitrary objects. The filter is defined by adding constraints of the class
 * <tt>Constraint</tt>, which could be subclassed for desired functionality.
 * 
 * @see Constraint
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class Filter implements Serializable
{
	static final long serialVersionUID = -383135804277784191L;
	
	/** Constant used to get a logical AND function of this Filer. */
	public static final boolean AND_OPERATOR = true;
	
	/** Constant used to get a logical OR function of this Filer. */
	public static final boolean OR_OPERATOR = false;
	
	protected final ArrayList constraints;
	protected boolean operator;
	
	/**
	 * Constructs a new Filer with a logical AND operator between constraints.
	 */
	public Filter()
	{
		this(AND_OPERATOR);
	}
	
	/**
	 * Constructs a new Filer.
	 * 
	 * @param operator the logical operator to be used between constraints when filtering.
	 */
	public Filter(boolean operator)
	{
		this.operator = operator;
		constraints = new ArrayList();
	}
	
	/**
	 * Returns the constraints used by this filter.
	 * 
	 * @return ArrayList containing Constraint objects.
	 * 
	 * @see Constraint
	 */
	public ArrayList getConstraints()
	{
		return (ArrayList)constraints.clone();
	}
	
	/**
	 * Adds a new Constraint to this Filter.
	 * 
	 * @param constraint the Constraint to be added to the filer.
	 * 
	 * @see Constraint
	 */
	public void addConstraint(Constraint constraint)
	{
		if(!constraints.contains(constraint)) 
		{
			constraints.add(constraint);
		}
	}
	
	/**
	 * Removes a Constraint from this Filter.
	 * 
	 * @param constraint the Constraint to be removed from the filer.
	 * 
	 * @see Constraint
	 */
	public boolean removeConstraint(Constraint constraint)
	{
		return constraints.remove(constraint);
	}
	
	/**
	 * Filters an object.
	 * 
	 * @return true if the object passes the filter or if there are no constraints in the filter, otherwise false.
	 */
	public boolean filterObject(Object obj)
	{
		boolean result = false;
		boolean checkResult;
		
		if( (constraints.size() > 0) && (obj != null) )
		{
			if(operator == AND_OPERATOR)
			{
				for(int i=0; i<constraints.size(); i++)
				{
					checkResult = ((Constraint)constraints.get(i)).filter(obj);
					
					if(!checkResult) return false;
					else result = true;
				}
			}
			else
			{
				result = false;
				
				for(int i=0; i<constraints.size(); i++)
				{
					checkResult = ((Constraint)constraints.get(i)).filter(obj);
					
					if(checkResult) return true;
				}
			}
			
			return result;
		}
		else return true;
	}
	
	/**
	 * 
	 */
	public List filterList(final List list)
	{
		ArrayList filteredCollection = new ArrayList();
		boolean result;
		Object obj;
		
		if(constraints.size() > 0)
		{
			if(operator == AND_OPERATOR)
			{		
				for(int i=0; i<list.size(); i++)
				{
					result = false;
					obj = list.get(i);
						
					if(obj != null)
					{
						for(int q=0; q<constraints.size(); q++)
						{
							if( ! ((Constraint)constraints.get(q)).filter(obj) )
							{
								result = false;
								break;
							}
							else
								 result = true;
						}
					}
					
					if(result) filteredCollection.add(obj);
				}
			}
			else
			{
				for(int i=0; i<list.size(); i++)
				{
					result = false;
					obj = list.get(i);
					
					for(int q=0; q<constraints.size(); q++)
					{
						obj = list.get(q);
						
						if(obj != null)
						{
							if(((Constraint)obj).filter(obj))
							{
								result = true;
								break;
							}
						}
					}
					
					if(result) filteredCollection.add(obj);
				}
			}
		}
		
		return filteredCollection;
	}
	
	/**
	 * Sets the logical operator to be used between constraints when filtering.
	 * 
	 * @param operator the operator.
	 */
	public void setOperator(boolean operator)
	{
		this.operator = operator;	
	}
	
	/**
	 * Returns a String representation of this Filer.
	 * 
	 * @return String representation.
	 */
	public String toString()
	{
		return "Filter: " + constraints;
	}
}
