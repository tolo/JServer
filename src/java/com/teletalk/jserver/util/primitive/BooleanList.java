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


/**
 * This class implements a list of primitive boolean values.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1 Build 545
 */
public final class BooleanList extends PrimitiveList
{
   static final long serialVersionUID = -6331382630013483343L;
   
   private boolean[] booleanArray;
	
	private final boolean defaultValue;
	
	/**
	 * Creates a new BooleanList with an initial capacity of 10 and a capacity increment of 10. The default values of 
	 * the elements in this list will be <code>false</code>.
	 */
	public BooleanList()
	{
		this(10, 10);
	}

	/**
	 * Creates a new BooleanList with a capacity increment of 10. The default values of 
	 * the elements in this list will be <code>false</code>.
	 * 
	 * @param initialCapacity the initial capacity of the list.
	 */
	public BooleanList(final int initialCapacity)
	{
		this(initialCapacity, 10);
	}
	
	/**
	 * Creates a new BooleanList. The default values of 
	 * the elements in this list will be <code>false</code>.
	 * 
	 * @param initialCapacity the initial capacity of the list.
	 * @param capacityIncrement the number of cells by which the list will grow when necessary.
	 */
	public BooleanList(final int initialCapacity, final int capacityIncrement)
	{
		this(initialCapacity, capacityIncrement, false);
	}
	
	/**
	 * Creates a new BooleanList. 
	 * 
	 * @param initialCapacity the initial capacity of the list.
	 * @param capacityIncrement the number of cells by which the list will grow when necessary.
	 * @param defaultValue the default value of the elements in this list.
	 */
	public BooleanList(final int initialCapacity, final int capacityIncrement, final boolean defaultValue)
	{
		super(initialCapacity, capacityIncrement);
		this.defaultValue = defaultValue;
	}
	
	/**
	 * Creates a new BooleanList initialized with the specified array and with a capacity increment of 10. The default values of 
	 * the elements in this list will be <code>false</code>.
	 * 
	 * @param array an array to fill the list with.
	 */
	public BooleanList(final boolean[] array)
	{
		this(array, 10);
	}
	
	/**
	 * Creates a new BooleanList initialized with the specified array. The default values of 
	 * the elements in this list will be <code>false</code>.
	 * 
	 * @param array an array to fill the list with.
	 * @param capacityIncrement the number of cells by which the list will grow when necessary.
	 */
	public BooleanList(final boolean[] array, final int capacityIncrement)
	{
		this(array, capacityIncrement, false);
	}
	
	/**
	 * Creates a new BooleanList initialized with the specified array.
	 * 
	 * @param array an array to fill the list with.
	 * @param capacityIncrement the number of cells by which the list will grow when necessary.
	 * @param defaultValue the default value of the elements in this list.
	 */
	public BooleanList(final boolean[] array, final int capacityIncrement, final boolean defaultValue)
	{
		super(array, array.length, capacityIncrement);
		this.defaultValue = defaultValue;
	}
	
	protected Object getPrimitiveArray()
	{
		return booleanArray;
	}
	
	protected void setPrimitiveArray(Object primitiveArray)
	{
		this.booleanArray = (boolean[])primitiveArray;
	}
	
	/**
	 * Adds a value to the end of this list.
	 * 
	 * @param value the value to add.
	 * 
	 * @return the index at which the value was added.
	 */
	public int add(final boolean value)
	{
		final int addIndex = super.prepareAdd(1);
						
		this.booleanArray[addIndex] = value;
				
		return addIndex;
	}
	
	/**
	 * Adds a value to this list at the specified index. The element at the specified index (if any) and all subsequent elements 
	 * will be shifted upward (one will be added to their indices).
	 * 
	 * @param index the index at which to add the value.
	 * @param value the value to add.
	 * 
	 * @exception IndexOutOfBoundsException if index is < 0 or > the current size.
	 */
	public void add(final int index, final boolean value) throws IndexOutOfBoundsException
	{
		super.prepareAdd(1, index);
		
		this.booleanArray[index] = value;
	}
	
	/**
	 * Adds all the values in the specified array to the end of this list.
	 * 
	 * @param values the values to add.
	 * 
	 * @return the index at which the first element of the specified array was added.
	 */
	public int addAll(final boolean[] values)
	{
		final int startIndex = super.prepareAdd(values.length);
		
		System.arraycopy(values, 0, this.booleanArray, startIndex, values.length);
				
		return startIndex;
	}
	
	/**
	 * Adds all the values in the specified array to this list  at the specified index. The element at the 
	 * specified index (if any) and all subsequent elements  will be shifted upward (values.length will be added to their indices).
	 * 
	 * @param addIndex the index at which to add the values.
	 * @param values the values to add.
	 * 
	 * @exception IndexOutOfBoundsException if index is < 0 or > the current size.
	 */
	public void addAll(final int addIndex, final boolean[] values)
	{
		super.prepareAdd(values.length, addIndex);
		
		System.arraycopy(values, 0, this.booleanArray, addIndex, values.length);
	}
	
	/**
	 * Removes the value at the specified index.
	 * 
	 * @param index the index at which to remove a value.
	 * 
	 * @return the removed value.
	 * 
	 * @exception IndexOutOfBoundsException if index is < 0 or >= the current size.
	 */
	public boolean remove(final int index) throws IndexOutOfBoundsException
	{
		final boolean oldValue = this.booleanArray[index];
		
		super.removeValueAtIndex(index);
		
		return oldValue;
	}
	
	/**
	 * Gets the value at the specified index.
	 * 
	 * @param index of the element to get the value of.
	 * 
	 * @return the value at the specified index.
	 * 
	 * @exception IndexOutOfBoundsException if index is < 0 or >= the current size.
	 */
	public boolean get(final int index) throws IndexOutOfBoundsException
	{
		super.boundsCheck(index);
		
		return this.booleanArray[index];
	}
	
	/**
	 * Sets the value of the element at the specified index.
	 * 
	 * @param index the index of the value to set.
	 * @param value the new value to set.
	 * 
	 * @exception IndexOutOfBoundsException if index is < 0 or >= the current size.
	 */
	public boolean set(final int index, final boolean value) throws IndexOutOfBoundsException
	{
		super.boundsCheck(index);
		
		final boolean oldValue = this.booleanArray[index];
		this.booleanArray[index] = value;
		
		return oldValue;
	}
	
	protected void resetValues(final int fromIndex, final int toIndex)
	{	
		for(int i=fromIndex; i<toIndex; i++)
		{
			this.booleanArray[i] = defaultValue;
		}
	}
   
   protected Object newArrary(final int size)
   {
      return new boolean[size];
   }
	
	/**
	 * Returns all the elements in this list as an array.
	 * 
	 * @return this list as an array.
	 */
	public final boolean[] toArray()
	{
		final int valueCount = super.size();
		final boolean[] all = new boolean[valueCount];
		
		System.arraycopy(this.booleanArray, 0, all, 0, valueCount);
		
		return all;
	}
   
   /**
    * Checks if this lists contains the value specified by parameter <code>key</code>.
    * 
    * @param key the value to search for.
    * 
    * @return <code>true</code> if the specified value was found in this list, otherwise <code>false</code>.
    * 
    * @since 2.0 (20050121)
    */
   public boolean contains(final boolean key)
   {
      for(int i=0; i<booleanArray.length; i++)
      {
         if( booleanArray[i] == key ) return true;
      }
      return false;
   }
	
	/**
	 * Compares the specified object with this list for equality. This method 
	 * returns <code>true</code> if and only if the specified object is of the same 
	 * class as this list, has the same size as this list and all the elelemts are equal and in 
	 * the same order.
	 * 
	 * @param o an object to be compared for equality with this list.
	 * 
	 * @return <code>true</code> if the specified object is equal to this list.
	 */
	public boolean equals(Object o)
	{
		boolean [] otherArray;
		
		if(o instanceof BooleanList) otherArray = ((BooleanList)o).toArray();
		else if(o instanceof boolean[]) otherArray = (boolean[])o;
		else return false;
					
		if(super.size() != otherArray.length) return false;
		
		for(int i=0; i<otherArray.length; i++)
		{
			if(this.booleanArray[i] != otherArray[i]) return false;
		}
		
		return true;
	}
   
   /**
    * Gets a string representation of this list.
    */
   public String toString()
   {
      StringBuffer buf = new StringBuffer();
      buf.append("[");
      
      final int valueCount = super.size();
      for(int i=0; i<valueCount; i++)
      {
         buf.append(String.valueOf(this.booleanArray[i]));
         if( i < (valueCount-1) ) buf.append(",");
      }
      
      buf.append("]");
      return buf.toString();
   }
}
