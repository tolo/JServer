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
package com.teletalk.jserver.pool;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;

/**
 * Class used by ObjectPool as a container for pool objects. If a ReferenceQueue is used a weak rerefence 
 * to the object will be stored here.
 * 
 * @see ObjectPool
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class PoolObjectContainer 
{
	private Object object;
	private final Integer hashcode;
	private WeakReference reference;
	private long lastAccess;
	
	/**
	 * Creates a new PoolObject.
	 * 
	 * @param object an object that is to be contained in this PoolObject.
	 * @param refQueue a reference to the ReferenceQueue used to reclaim lost references. May be <code>null</code>.
	 */
	public PoolObjectContainer(Object object, ReferenceQueue refQueue)
	{
		this.object = object;
		this.hashcode = new Integer(System.identityHashCode(object));
		if( refQueue != null ) reference = new WeakReference(object, refQueue);
		else reference = null;
		this.lastAccess = System.currentTimeMillis();
	}
	
	/**
	 * Returns the object contained in this PoolObject.
	 * 
	 * @return an object.
	 */
	public Object getObject()
	{
		return object;	
	}
	
	/**
	 * Returns the hashcode of the object contained in this PoolObject.
	 * 
	 * @return an Integer object containing a hashcode.
	 */
	public Integer getObjectHashcode()
	{
		return hashcode;
	}
	
	/**
	 * Returns the weak reference to the object contained in this PoolObject.
	 * 
	 * @return a WeakReference object.
	 */
	public WeakReference getReference()
	{
		return reference;
	}
	
	/**
	 * Returns the object that the weak reference in this PoolObject refers to.
	 * 
	 * @return an Object.
	 */
	public Object getReferenceObject()
	{
		if( reference != null ) return reference.get();
		else return null;
	}
	
	/**
	 * Returns the time when this object was last accessed.
	 * 
	 * @return long value.
	 */
	public long getLastAccess()
	{
		return lastAccess;	
	}
	
	/**
	 * Marks this PoolObject as checked out and clears its reference to the object 
	 * (If a weak reference exists).
	 * 
	 * @return an Object.
	 */
	public Object checkOut()
	{
		Object obj = object;
		if( this.reference != null ) object = null;
		lastAccess = System.currentTimeMillis();
		
		return obj;
	}
	
	/**
	 * Returns the object to this PoolObject.
	 * 
	 * @param obj an Object.
	 */
	public void checkIn(final Object obj)
	{
	   if( this.reference != null ) object = obj;
		lastAccess = System.currentTimeMillis();
	}
	
	/**
	 * Checks if the object contained in this PoolObject is the same as the one the weak reference is
	 * referring to.
	 */
	public boolean validate()
	{
		if( reference != null ) return (object != null) ? object == reference.get() : true;
		else return true;
	}
	
	/**
	 * Destroys this PoolObject.
	 * 
	 * @return the Object contained in this PoolObject.
	 */
	public Object destroy()
	{
		Object obj = object;
		object = null;
		if( reference != null ) reference.clear();
		reference = null;
		
		return obj;
	}
	
	/**
	 * Gets a string representation of this PoolObject.
	 * 
	 * @return a string representation of this PoolObject.
	 */
	public String toString()
	{
		if(object != null) return "PoolObject for " + object.toString() + ".";
		else return "PoolObject for object with hashcode " + hashcode + ".";
	}
}
