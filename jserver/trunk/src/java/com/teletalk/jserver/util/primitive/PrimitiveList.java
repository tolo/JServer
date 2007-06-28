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
package com.teletalk.jserver.util.primitive;

import java.io.Serializable;

/**
 * Abstract base class for classes implementing a list of primitive values.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1 Build 545
 */
public abstract class PrimitiveList implements Serializable
{
   static final long serialVersionUID = -5719734771970705185L;
	
   
	private final int initialCapacity;
	
	private final int capacityIncrement;
	
	private int primitiveArrayLength;
		
	private int valueCount;
   
			
	/**
	 * Creates a new PrimitiveList.
	 * 
	 * @param initialCapacity the initial capacity of the list.
	 * @param capacityIncrement the number of cells by which the list will grow when necessary.
	 */
	protected PrimitiveList(final int initialCapacity, final int capacityIncrement)
	{
		if(initialCapacity < 0) throw new IllegalArgumentException("Negative initialCapacity specified!");
		if(capacityIncrement < 0) throw new IllegalArgumentException("Negative capacityIncrement specified!");
		
		this.initialCapacity = initialCapacity;
		this.capacityIncrement = (capacityIncrement > 0) ? capacityIncrement : 1;
				
		this.valueCount = 0;
		
		final Object primitiveArray = this.newArrary(initialCapacity);
		this.primitiveArrayLength = initialCapacity;
		
		this.setPrimitiveArray(primitiveArray);
	}
	
	/**
	 * Creates a new PrimitiveList.
	 * 
	 * @param array an array to fill the list with.
	 * @param arrayLength the initial capacity of the list.
	 * @param capacityIncrement the number of cells by which the list will grow when necessary.
	 */
	protected PrimitiveList(final Object array, final int arrayLength, final int capacityIncrement)
	{
		if(capacityIncrement < 0) throw new IllegalArgumentException("Negative capacityIncrement specified!");
		
		this.initialCapacity = arrayLength;
		this.capacityIncrement = (capacityIncrement > 0) ? capacityIncrement : 1;;
				
		this.valueCount = arrayLength;
		this.primitiveArrayLength = arrayLength;
		
		this.setPrimitiveArray(array);
	}
	
	/**
	 * Gets the initial capacity of this list.
	 * 
	 * @return the initial capacity.
	 */
	protected final int getInitialCapacity()
	{
		return this.initialCapacity;
	}
	
	/**
	 * Gets the capacity increment (the number of cells by which the list will grow when 
	 * necessary) of this list 
	 * 
	 * @return the capacity increment.
	 */
	protected final int getCapacityIncrement()
	{
		return this.capacityIncrement;
	}
	
	/**
	 * Gets the current number of elements in this list.
	 * 
	 * @return the number of elements in the list.
	 */
	public final int size()
	{
		return this.valueCount;
	}

	/**
	 * Gets the primitive array used to store the elements of the list.
	 * 
	 * @return the primitive array.
	 */
	protected abstract Object getPrimitiveArray();
	
	/**
	 * Sets the primitive array used to store the elements of the list.
	 * 
	 * @param primitiveArray a new primitive array.
	 */
	protected abstract void setPrimitiveArray(Object primitiveArray);
	
	/**
	 * Tests if the specified index is within the bounds of this list.
	 * 
	 * @exception IndexOutOfBoundsException if the specified index is < 0 or >= the size of this list.
	 */
	protected final void boundsCheck(final int index) throws IndexOutOfBoundsException
	{
		if( (index < 0) || (index >= this.valueCount) ) throw new IndexOutOfBoundsException("Index: " + index + ", size: " + this.valueCount + ".");
	}
	
	/**
	 * Makes sure there is room in this list to add the specified number of values.
	 * 
	 * @param numberOfValues the number of values to make room for.
	 * 
	 * @return the index at which values should be added.
	 */
	protected final int prepareAdd(final int numberOfValues)
	{
		if(numberOfValues == 0) return -1;
		
		final int addIndex = this.valueCount;
		ensureCapacity(this.valueCount + numberOfValues);  //Ensure capacity
		this.valueCount += numberOfValues; //Increase counter for number of values
		
		return addIndex;
	}
	
	/**
	 * Makes sure there is room in this list to add the specified number of values at the specified index.
	 * 
	 * @param numberOfValues the number of values to make room for.
	 * @param addIndex the index at which values are to be added.
	 */
	protected final void prepareAdd(final int numberOfValues, final int addIndex) throws IndexOutOfBoundsException
	{
		if(numberOfValues == 0) return;
		this.valueCount++; //Allow insert (add) at one index higher that the current size.
		this.boundsCheck(addIndex);
		this.valueCount--;
				
		ensureCapacity(this.valueCount + numberOfValues); //Ensure capacity
		this.valueCount += numberOfValues; //Increase counter for number of values

		final Object primitiveArray = this.getPrimitiveArray();
						
		if(addIndex < (this.valueCount-1)) //If expandIndex is not last index - shift array to make room for new value
			System.arraycopy(primitiveArray, addIndex, primitiveArray, addIndex+numberOfValues, (this.valueCount -1- addIndex));
	}
		
	/**
	 * Removes the value at the specified index.
	 * 
	 * @param removeIndex the index at which to remove a value.
	 */
	protected final void removeValueAtIndex(final int removeIndex) throws IndexOutOfBoundsException
	{
		this.boundsCheck(removeIndex);
		
		final Object primitiveArray = this.getPrimitiveArray();
		
		final int moveCount = this.valueCount - removeIndex - 1;
		
		if(moveCount > 0) //Shift array/shrink it
      {
			System.arraycopy(primitiveArray, removeIndex + 1, primitiveArray, removeIndex, moveCount);
      }
		
		if( (this.primitiveArrayLength - this.valueCount) > this.capacityIncrement ) //If there are too much unused space...
      {
		   trimToSize(); //...trim array
      }
		
		this.valueCount--;
	}
	
	/**
	 * Grows this list up to the specified capacity.
	 * 
	 * @param requiredCapacity the capacity to grow this list to.
	 */
	protected final void grow(int requiredCapacity)
	{
      final int increment = Math.max(this.capacityIncrement, this.primitiveArrayLength / 10);
		requiredCapacity = Math.max(requiredCapacity, this.primitiveArrayLength + increment);
      
		final Object primitiveArray = this.getPrimitiveArray();
        
		final Object expandedArray = this.newArrary(requiredCapacity);
		System.arraycopy(primitiveArray, 0, expandedArray, 0, this.valueCount);
						
		this.setPrimitiveArray(expandedArray);
		
		this.primitiveArrayLength = requiredCapacity;
	}
	
	/**
	 * Ensures that this list is capable of holding the specified capacity.
	 * 
	 * @param requiredCapacity the number of elements that this list should be capable of holding.
	 */
	public final void ensureCapacity(final int requiredCapacity) 
	{
	   if(requiredCapacity > this.primitiveArrayLength)
	   {
	      this.grow(requiredCapacity);
	   }
    }
	
	/**
	 * Sets the current size of this list. If parameter <code>newSize</code> is larger 
	 * than the current size, the list will be expanded to newSize and the newly created 
	 * values (elements) will be initialized using the method {@link #resetValues(int, int)}. If 
	 * newSize is less than the current size, the list will be trimmed to newSize and all values 
	 * with an index >= newSize will be discarded.
	 * 
	 * @param newSize the new size of this list.
	 */
	public final void setSize(final int newSize) 
	{
		if(newSize > this.primitiveArrayLength)
		{
			grow(newSize);
		} 
				
		if(newSize > this.valueCount) //Set "new values" to default value
      {
			this.resetValues(this.valueCount, newSize);
      }
		
		this.valueCount = newSize;
		
		if( (this.primitiveArrayLength - this.valueCount) > this.capacityIncrement ) //If there are too much unused space...
      {
			trimToSize(); //...trim array
      }
	}
		
	/**
	 * Trims the array that is used by this list to hold elements to the current size.
	 */
	public final void trimToSize()
	{
		if(this.valueCount == this.primitiveArrayLength) return;
		
		final Object primitiveArray = this.getPrimitiveArray();
        
		final Object newArray = this.newArrary(this.valueCount);
		System.arraycopy(primitiveArray, 0, newArray, 0, this.valueCount);
		
		this.setPrimitiveArray(newArray);
		
		this.primitiveArrayLength = this.valueCount;
	}
	
	/**
	 * Clears (removes) all values in this list.
	 */
	public final void clear()
	{
		final Object clearedArray = this.newArrary(this.initialCapacity);
		this.setPrimitiveArray(clearedArray);
				
		this.primitiveArrayLength = this.initialCapacity;
		this.valueCount = 0;
	}
	
	/**
	 * Resets all the elements between the specified indices to their 
	 * default values.
	 * 
	 * @param fromIndex the index of the first element to reset (inclusive).
	 * @param toIndex the index after the last index to reset. (exclusive)
	 */
	protected abstract void resetValues(int fromIndex, int toIndex);
   
   /**
    * Creates a new primitive array with the specified length.
    * 
    * @since 2.1.3 (20060404)
    */
   protected abstract Object newArrary(final int size);
}
