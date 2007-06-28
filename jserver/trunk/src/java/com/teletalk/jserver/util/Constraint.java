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

/**
 * Abstract baseclass for all Constraints that are to be used with a <tt>Filter</tt>.
 * 
 * @see Filter
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public abstract class Constraint implements Serializable
{
	static final long serialVersionUID = -4512575030428812351L;
	
	/**
	 * The class ConstraintOperator represents an operator used when specifying a Constraint.
	 * 
	 * @see Constraint
	 */
	public static class ConstraintOperator implements Serializable
	{
      static final long serialVersionUID = 3606575019458652226L;
      
		/** The name of the operator. */
		public String name;
		
		/** The name of the operator when used with a logical NOT. */
		public String notName;
		
		/**
		 * Constructs a new ConstraintOperator.
		 * 
		 * @param name the name of this operator.
		 * @param notName the name of this operator when used with a logical NOT. 
		 */
		public ConstraintOperator(String name, String notName)
		{
			this.name = name;
			this.notName = notName;
		}
			
		/**
		* Returns a String describing this ConstraintOperator.
		* 
		* @param not not flag.
		* 
		* @return a string representation of the ConstraintOperator.
		*/
		public final String toString(boolean not)
		{
			if(not)
				return notName;
			else
				return name;
		}
		
		/**
		* Compares two ConstraintOperator.
		* 
		* @return true if the names and notNames of the ConstraintOperators are equal.
		*/
		public boolean equals(Object obj)
		{
			if(obj instanceof ConstraintOperator)
			{
				ConstraintOperator o = (ConstraintOperator)obj;
				return this.name.equals(o.name) && this.notName.equals(o.notName);
			} 
			else return false;
		}
	}
	
	/** The current operator. */
	protected final ConstraintOperator constraintOperator;
	
	/**	Flag indicating if logical NOT is to be used. */
	protected final boolean not;
	
	/**
	 * Internal constructor.
	 * 
	 * @param constraintOperator the operator for this Constraint.
	 * @param not indicating whether or not and logical NOT should be used in conjunction with the operator when filtering.
	 */
	protected Constraint(ConstraintOperator constraintOperator, boolean not)
	{
		this.constraintOperator = constraintOperator;
		this.not = not;
	}
	
	/**
	 * Returns the ConstraintOperator used by this Constraint.
	 * 
	 * @return a ConstraintOperator object.
	 */
	public ConstraintOperator getConstraintOperator()
	{
		return constraintOperator;
	}
	
	/**
	 * Gets the value of the not flag.
	 * 
	 * @return the value of the not flag.
	 */
	public boolean isNot()
	{
		return not;	
	}
	
	/**
	 * Abstract method to check if an object passes this Constraint.
	 * 
	 * @param obj the object to check.
	 */
	public abstract boolean filter(Object obj);
	
	/**
	 * Returns the available ConstraintOperators in this class. Subclasses should override this
	 * method. This implementation returns null.
	 */
	public static Object[] getConstraintOperators()
	{
		//hmmm
		return null;
	}
	
	/**
	 * Returns a ConstraintOperator with a specific name. Subclasses should override this
	 * method. This implementation returns null.
	 * 
	 * @param name the name of the operator to look for.
	 * @param notFlag indicating if the specified name is a notName.
	 * 
	 * @see ConstraintOperator#notName
	 */
	public static ConstraintOperator getConstraintOperator(String name, boolean notFlag)
	{
		//hmmm
		return null;
	}
}
