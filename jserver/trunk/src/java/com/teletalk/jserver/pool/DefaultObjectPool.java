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

import java.lang.reflect.Constructor;

import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.util.ReflectionUtils;

/**
 * A default implementation of ObjectPool that makes it possible to create a pool of objects
 * by specifying the classname and the constructor parameters. This is done in the constructor
 * when creating an instance of DefaultObjectPool.<br>
 * <br>
 * <i>Note:</i> As an alternative to using this class, consider using a {@link PoolObjectFactory} with {@link ObjectPool}.
 * 
 * @see ObjectPool
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class DefaultObjectPool extends ObjectPool
{
	private final Class objectClass;
	private Constructor objectClassConstructor;
	private Object[] objectClassConstructorParams;
	
	/**
	 * Creates a new default object pool where the pool objects never expire. The min size of the pool will be set to maximum size. 
	 * 
	 * @param parent the parent of this pool.
	 * @param name name of the pool.
	 * @param objectClass the Class of which objects will be created to fill the pool.
	 * @param objectClassConstructorParams the contructorparameters to be used when creating objects.
	 * @param maxSize the maximum maxSize of the pool.
	 * @param cleanUpInterval time in milliseconds between cleanups, used by the associated PoolCleaner.
	 * 
	 * @exception IllegalArgumentException maxSize or cleanUpInterval was <= 0.
	 * @exception NoSuchMethodException if there was an error finding a constructor in the specified objectclass matching the sprecfied contructor parameters.
	 */
	public DefaultObjectPool(SubComponent parent, String name, Class objectClass, Object[] objectClassConstructorParams, int maxSize, long cleanUpInterval) throws IllegalArgumentException, NoSuchMethodException
	{
		super(parent, name, maxSize, cleanUpInterval);
		
		this.objectClass = objectClass;
		this.objectClassConstructorParams = objectClassConstructorParams;
		
		this.objectClassConstructor = ReflectionUtils.findConstructor(objectClass, objectClassConstructorParams);
	}
	
	/**
	 * Creates a new default object pool where the pool objects never expire. The min size of the pool will be set to maximum size. 
	 * 
	 * @param parent the parent of this pool.
	 * @param name name of the pool.
	 * @param objectClass the Class of which objects will be created to fill the pool.
	 * @param objectClassConstructorParams the contructorparameters to be used when creating objects.
	 * @param minSize the minimum maxSize of the pool.
	 * @param maxSize the maximum maxSize of the pool.
	 * @param cleanUpInterval time in milliseconds between cleanups, used by the associated PoolCleaner.
	 * 
	 * @exception IllegalArgumentException maxSize or cleanUpInterval was <= 0.
	 * @exception NoSuchMethodException if there was an error finding a constructor in the specified objectclass matching the sprecfied contructor parameters.
	 * 
	 * @since 2.0 Build 757
	 */
	public DefaultObjectPool(SubComponent parent, String name, Class objectClass, Object[] objectClassConstructorParams, int minSize, int maxSize, long cleanUpInterval) throws IllegalArgumentException, NoSuchMethodException
	{
		super(parent, name, minSize, maxSize, cleanUpInterval);
		
		this.objectClass = objectClass;
		this.objectClassConstructorParams = objectClassConstructorParams;
		
		this.objectClassConstructor = ReflectionUtils.findConstructor(objectClass, objectClassConstructorParams);
	}
	
	/**
	 * Creates a default object pool where each object in it has an expirationtime. The min size will be set to 0 and the max size to 10.
	 * 
	 * @param parent the parent of this pool.
	 * @param name name of the pool.
	 * @param objectClass the Class of which objects will be created to fill the pool.
	 * @param objectClassConstructorParams the contructorparameters to be used when creating objects.
	 * @param cleanUpInterval time in milliseconds between cleanups, used by the associated PoolCleaner.
	 * @param objectExpirationTime time in milliseconds that has to pass after an object has been created
	 * 
	 * @exception IllegalArgumentException objectExpirationTime or cleanUpInterval was <= 0.
	 * @exception NoSuchMethodException if there was an error finding a constructor in the specified objectclass matching the sprecfied contructor parameters.
	 */
	public DefaultObjectPool(SubComponent parent, String name, Class objectClass, Object[] objectClassConstructorParams, long cleanUpInterval, long objectExpirationTime) throws IllegalArgumentException, NoSuchMethodException
	{
		super(parent, name, cleanUpInterval, objectExpirationTime);
		
		this.objectClass = objectClass;
		this.objectClassConstructorParams = objectClassConstructorParams;
		
		this.objectClassConstructor = ReflectionUtils.findConstructor(objectClass, objectClassConstructorParams);
	}
	
	/**
	 * Creates a new default object pool. The default constructor of objectClass will be used when creating objects. The min size of the pool will be set to maximum size.
	 * 
	 * @param parent the parent of this pool.
	 * @param name name of the pool.
	 * @param objectClass the Class of which objects will be created to fill the pool.
	 * @param maxSize the maximum maxSize of the pool.
	 * @param cleanUpInterval time in milliseconds between cleanups, used by the associated PoolCleaner.
	 * 
	 * @exception IllegalArgumentException maxSize or cleanUpInterval was <= 0.
	 */
	public DefaultObjectPool(SubComponent parent, String name, Class objectClass, int maxSize, long cleanUpInterval) throws IllegalArgumentException
	{
		super(parent, name, maxSize, cleanUpInterval);
		
		this.objectClass = objectClass;
		this.objectClassConstructor = null;
	}
	
	/**
	 * Creates a new default object pool. The default constructor of objectClass will be used when creating objects. The min size of the pool will be set to maximum size.
	 * 
	 * @param parent the parent of this pool.
	 * @param name name of the pool.
	 * @param objectClass the Class of which objects will be created to fill the pool.
	 * @param minSize the minimum maxSize of the pool.
	 * @param maxSize the maximum maxSize of the pool.
	 * @param cleanUpInterval time in milliseconds between cleanups, used by the associated PoolCleaner.
	 * 
	 * @exception IllegalArgumentException maxSize or cleanUpInterval was <= 0.
	 */
	public DefaultObjectPool(SubComponent parent, String name, Class objectClass, int minSize, int maxSize, long cleanUpInterval) throws IllegalArgumentException
	{
		super(parent, name, minSize, maxSize, cleanUpInterval);
		
		this.objectClass = objectClass;
		this.objectClassConstructor = null;
	}
	
	/**
	 * Creates an objectpool with dynamic size and where each object in it has an expirationtime. 
	 * The defaultconstructor of objectClass will be used when creating objects. The min size will be set to 0 and the max size to 10.
	 * 
	 * @param parent the parent of this pool.
	 * @param name name of the pool.
	 * @param objectClass the Class of which objects will be created to fill the pool.
	 * @param cleanUpInterval time in milliseconds between cleanups, used by the associated PoolCleaner.
	 * @param objectExpirationTime time in milliseconds that has to pass after an object has been created
	 * 
	 * @exception IllegalArgumentException objectExpirationTime or cleanUpInterval was <= 0.
	 */
	public DefaultObjectPool(SubComponent parent, String name, Class objectClass, long cleanUpInterval, long objectExpirationTime) throws IllegalArgumentException
	{
		super(parent, name, cleanUpInterval, objectExpirationTime);
		
		this.objectClass = objectClass;
		this.objectClassConstructor = null;
	}
	
	/**
	 * Creates a fixed size pool with a cleaninterval of 60 seconds. The defaultconstructor of objectClass will be used when creating objects. 
	 * The min size of the pool will be set to maximum size.
	 * 
	 * @param parent the parent of this pool.
	 * @param name name of the pool.
	 * @param objectClass the Class of which objects will be created to fill the pool.
	 * @param maxSize the maximum maxSize of the pool.
	 * 
	 * @exception IllegalArgumentException maxSize was <= 0.
	 */
	public DefaultObjectPool(SubComponent parent, String name, Class objectClass, int maxSize) throws IllegalArgumentException
	{
		this(parent, name, objectClass, maxSize, 60*1000);
	}
	
	/**
	 * Creates a fixed size pool with a cleaninterval of 60 seconds. The defaultconstructor of objectClass will be used when creating objects. 
	 * The min size of the pool will be set to maximum size.
	 * 
	 * @param parent the parent of this pool.
	 * @param name name of the pool.
	 * @param objectClass the Class of which objects will be created to fill the pool.
	 * @param minSize the minimum maxSize of the pool.
	 * @param maxSize the maximum maxSize of the pool.
	 * 
	 * @exception IllegalArgumentException maxSize was <= 0.
	 */
	public DefaultObjectPool(SubComponent parent, String name, Class objectClass, int minSize, int maxSize) throws IllegalArgumentException
	{
		this(parent, name, objectClass, minSize, maxSize, 60*1000);
	}
	
	/**
	 * Creates an objectpool with a cleaninterval of 60 seconds, dynamic size and where each object in it has an expirationtime. 
	 * The defaultconstructor of objectClass will be used when creating objects. The min size will be set to 0 and the max size to 10.
	 * 
	 * @param parent the parent of this pool.
	 * @param name name of the pool.
	 * @param objectClass the Class of which objects will be created to fill the pool.
	 * @param objectExpirationTime time in milliseconds that has to pass after an object has been created
	 * 
	 * @exception IllegalArgumentException objectExpirationTime was <= 0.
	 */
	public DefaultObjectPool(SubComponent parent, String name, Class objectClass, long objectExpirationTime) throws IllegalArgumentException
	{
		this(parent, name, objectClass, 60*1000, objectExpirationTime);
	}
	
	/**
	 * Creates an object.
	 * 
	 * @return the newly created object.
	 */
	public Object createObject() throws Exception
	{
		if(objectClassConstructor != null)
		{
			return objectClassConstructor.newInstance(objectClassConstructorParams);
		}
		else
		{
			return objectClass.newInstance();
		}
	}
	
	/**
	 * Validates an object.
	 * 
	 * @param obj the object to validate.
	 * 
	 * @return true if the object isn't null, otherwise false.
	 */
	public boolean validateObject(Object obj, boolean cleanUpValidation)
	{
		return obj != null;
	}
	
	/**
	 * Finalizes an object.
	 * 
	 * @param obj an object to be finalized.
	 */
	public void finalizeObject(Object obj)
	{
		obj = null;
	}
}
