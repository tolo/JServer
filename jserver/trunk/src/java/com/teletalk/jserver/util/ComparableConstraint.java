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
 * The class ComparableConstraint is a Constraint to be used when filtering comparable values.
 * 
 * @see Filter
 * @see Constraint
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class ComparableConstraint extends Constraint
{
	static final long serialVersionUID = 8249157244671903871L;
	
	public final static ConstraintOperator EQUALS = new ConstraintOperator("equals", "doesn't equal");
	public final static ConstraintOperator LESSER_THAN = new ConstraintOperator("lesser than", "not lesser than");
	public final static ConstraintOperator LESSER_OR_EQUAL_TO = new ConstraintOperator("lesser or equal to", "not lesser or equal to");
	public final static ConstraintOperator GREATER_THAN = new ConstraintOperator("greater than", "not greater than");
	public final static ConstraintOperator GREATER_OR_EQUAL_TO = new ConstraintOperator("greater or equal to", "not greater or equal to");
	
	public final static Object[] constraintOperators = new Object[]{EQUALS, LESSER_THAN, LESSER_OR_EQUAL_TO, GREATER_THAN, GREATER_OR_EQUAL_TO};
		
	private final Comparable constraintOperand;
		
	/**
	 * Constructs a new ComparableConstraint.
	 * 
	 * @param constraintOperator the operator for this ComparableConstraint.
	 * @param constraintOperand the operand for this ComparableConstraint.
	 */
	public ComparableConstraint(ConstraintOperator constraintOperator, Comparable constraintOperand)
	{
		super(constraintOperator, false);
		this.constraintOperand = constraintOperand;
	}
	
	/**
	 * Constructs a new ComparableConstraint.
	 * 
	 * @param constraintOperator the operator for this ComparableConstraint.
	 * @param constraintOperand the operand for this ComparableConstraint.
	 * @param not indicating whether or not and logical NOT should be used in conjunction with the operator when filtering.
	 */
	public ComparableConstraint(ConstraintOperator constraintOperator, Comparable constraintOperand, boolean not)
	{
		super(constraintOperator, not);
		this.constraintOperand = constraintOperand;
	}
	
	/**
	 * Checkes if the object specified in paramterer obj conforms to the
	 * condition specified by constraintOperator and constraintOperand in
	 * this ComparableConstraint.
	 * 
	 * @return true if the object specefied by the parameter obj passes the check,
	 * otherwise false. This method also returns true if the parameter obj doesn't
	 * implement the interface java.lang.Comparable or is of a different class than that of
	 * constraintOperand.
	 * 
	 * @see Constraint#constraintOperator
	 */
	public boolean filter(Object obj)
	{
		if(obj instanceof Comparable) 
		{
			boolean result;
			final int compareResult = ((Comparable)obj).compareTo(constraintOperand);
		
			if(constraintOperator.equals(EQUALS))
			{
				result = (compareResult == 0);
			}
			else if(constraintOperator.equals(LESSER_THAN))
			{
				result = (compareResult < 0);
			}
			else if(constraintOperator.equals(LESSER_OR_EQUAL_TO))
			{
				result = (compareResult <= 0);
			}
			else if(constraintOperator.equals(GREATER_THAN))
			{
				result = (compareResult > 0);
			}
			else if(constraintOperator.equals(GREATER_OR_EQUAL_TO))
			{
				result = (compareResult >= 0);
			}
			else result = true;
			
			if(not) return !result;
			else return result;
		}
		return true;
	}
	
	/**
	 * Returns the constraint operand used by this ComparableConstraint.
	 * 
	 * @return the constraint operand.
	 */
	public Comparable getConstraintOperand()
	{
		return constraintOperand;	
	}
	
	/**
	* Returns a String describing this ComparableConstraint.
	* 
	* @return a string representation of the ComparableConstraint.
	*/
	public String toString()
	{
		return constraintOperator.toString(not) + " '" + constraintOperand + "'";
	}
	
	/**
	 * Returns the available ConstraintOperators in this class. 
	 */
	public static Object[] getConstraintOperators()
	{
		return constraintOperators;
	}
	
	/**
	 * Returns a ConstraintOperator with a specific name. 
	 * 
	 * @param name the name of the operator to look for.
	 * @param notFlag indicating if the specified name is a notName.
	 */
	public static ConstraintOperator getConstraintOperator(String name, boolean notFlag)
	{
		Object[] ops = getConstraintOperators();
		
		for(int i=0; i<ops.length; i++)
		{
			if(((ConstraintOperator)ops[i]).toString(notFlag).equals(name)) return ((ConstraintOperator)ops[i]);
		}
		
		return null;
	}
	
	/**
	* Compares two ComparableConstraints.
	* 
	* @return true if the constraintOperands, constraintOperator and notflags in the two ComparableConstraints are equal.
	*/
	public boolean equals(Object obj)
	{
		if(obj instanceof ComparableConstraint)
		{
			ComparableConstraint c = (ComparableConstraint)obj;
			return ((Object)this.constraintOperand).equals(c.constraintOperand) && (this.constraintOperator.equals(c.constraintOperator)) && (this.not == c.not);
		} 
		else return false;
	}
}
