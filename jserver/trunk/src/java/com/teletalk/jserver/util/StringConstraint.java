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

/**
 * The class StringConstraint is a Constraint to be used when filtering String values.
 * 
 * @see Filter
 * @see Constraint
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public class StringConstraint extends Constraint
{
	static final long serialVersionUID = 3784831190772049502L;
	
	public final static ConstraintOperator EQUALS = new ConstraintOperator("equals", "doesn't equal");
	public final static ConstraintOperator EQUALS_IGNORE_CASE = new ConstraintOperator("equals (ignore case)", "doesn't equal (ignore case)");
	public final static ConstraintOperator CONTAINS = new ConstraintOperator("contains", "doesn't contain");
	public final static ConstraintOperator CONTAINS_IGNORE_CASE = new ConstraintOperator("contains (ignore case)", "doesn't contain (ignore case)");
	public final static ConstraintOperator STARTS_WITH = new ConstraintOperator("starts with", "doesn't start with");
	public final static ConstraintOperator STARTS_WITH_IGNORE_CASE = new ConstraintOperator("starts with (ignore case)", "doesn't start with (ignore case)");
	
	public final static Object[] constraintOperators = new Object[]{EQUALS, EQUALS_IGNORE_CASE, CONTAINS, CONTAINS_IGNORE_CASE, STARTS_WITH, STARTS_WITH_IGNORE_CASE};
	
	private String constraintString;

	/**
	 * Constructs a new StringConstraint.
	 * 
	 * @param constraintOperator the operator for this StringConstraint.
	 * @param constraintString the constraintString for this StringConstraint.
	 */
	public StringConstraint(ConstraintOperator constraintOperator, String constraintString)
	{
		super(constraintOperator, false);
		//this.constraintOperator = constraintOperator;
		this.constraintString = constraintString;
		//this.not = false;
	}
	
	/**
	 * Constructs a new StringConstraint.
	 * 
	 * @param constraintOperator the operator for this StringConstraint.
	 * @param constraintString the constraintString for this StringConstraint.
	 * @param not indicating whether or not and logical NOT should be used in conjunction with the operator when filtering.
	 */
	public StringConstraint(ConstraintOperator constraintOperator, String constraintString, boolean not)
	{
		super(constraintOperator, not);
		//this.constraintOperator = constraintOperator;
		this.constraintString = constraintString;
		//this.not = not;
	}
	
	/**
	 * Checkes if the object specified in paramterer obj conforms to the
	 * condition specified by constraintOperator and constraintString in
	 * this StringConstraint.
	 * 
	 *  @return true if the object specefied by the parameter obj passes the check,
	 * otherwise false. This method also returns true if the parameter obj isn't
	 * an instance of java.lang.String.
	 */
	public final boolean filter(Object obj)
	{
		if(obj instanceof String)
		{
			boolean result;
			
			if(constraintOperator.equals(EQUALS))
			{
				result = ((String)obj).equals(constraintString);
			}
			else if(constraintOperator.equals(EQUALS_IGNORE_CASE))
			{
				result = ((String)obj).equalsIgnoreCase(constraintString);
			}
			else if(constraintOperator.equals(CONTAINS))
			{
				result = (((String)obj).indexOf(constraintString) >= 0);
			}
			else if(constraintOperator.equals(CONTAINS_IGNORE_CASE))
			{
				result = (((String)obj).toUpperCase().indexOf(constraintString.toUpperCase()) >= 0);
			}
			else if(constraintOperator.equals(STARTS_WITH))
			{
				result = ((String)obj).startsWith(constraintString);
			}
			else if(constraintOperator.equals(STARTS_WITH_IGNORE_CASE))
			{
				result = ((String)obj).toUpperCase().startsWith(constraintString.toUpperCase());
			}
			else result = true;
			
			if(not) return !result;
			else return result;
		}
		return true;
	}
	
	/**
	* Returns a String describing this StringConstraint.
	* 
	* @return a string representation of the StringConstraint.
	*/
	public final String toString()
	{
		return constraintOperator.toString(not) + " '" + constraintString + "'";
	}
	
	/**
	 * Returns the available ConstraintOperators in this class. 
	 */
	public static final Object[] getConstraintOperators()
	{
		return constraintOperators;
	}
	
	/**
	 * Returns a ConstraintOperator with a specific name. 
	 * 
	 * @param name the name of the operator to look for.
	 * @param notFlag indicating if the specified name is a notName.
	 */
	public static final ConstraintOperator getConstraintOperator(String name, boolean notFlag)
	{
		Object[] ops = getConstraintOperators();
		
		for(int i=0; i<ops.length; i++)
		{
			if(((ConstraintOperator)ops[i]).toString(notFlag).equals(name)) return ((ConstraintOperator)ops[i]);
			/*if(((ConstraintOperator)ops[i]).toString(true).equals(name) ||
			   ((ConstraintOperator)ops[i]).toString(false).equals(name)) return ((ConstraintOperator)ops[i]);*/
		}
		
		return null;
	}
	
	/**
	* Compares two StringConstraints.
	* 
	* @return true if the constraintOperands, constraintStrings and notflags in the two StringConstraints are equal.
	*/
	public boolean equals(Object obj)
	{
		if(obj instanceof StringConstraint)
		{
			StringConstraint c = (StringConstraint)obj;
			return ((Object)this.constraintString).equals(c.constraintString) && (this.constraintOperator.equals(c.constraintOperator)) && (this.not == c.not);
		} 
		else return false;
	}
}
