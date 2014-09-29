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

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;

import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.property.BooleanProperty;
import com.teletalk.jserver.property.CalculatedPropertyOwner;
import com.teletalk.jserver.property.NumberProperty;
import com.teletalk.jserver.property.Property;

/**
 * This class implements a pool of objects. It can be used to increase 
 * performance by building up a buffer of objects of any type that later on can be
 * checked out (and in) at a low performance cost.<br>
 * <br>
 * This class can be used either by specifying a {@link PoolObjectFactory} or overriding certain key methods. These 
 * methods are:<br>
 * <pre>
 * public abstract Object createObject()
 * public abstract boolean validateObject(Object obj)
 * public abstract void finalizeObject(Object obj)
 * </pre>
 * <br>
 * Note that if no {@link PoolObjectFactory} is specified, all three methods must be overridden.<br>
 * <br>
 * The pool is self maintaining and monitoring of pool objects is performed periodically. If the flag 
 * {@link #isReferenceQueueEnabled() referenceQueueEnabled} is set to true, the pool will maintain weak references 
 * to pool objects instead of stong ones. When a weak reference is lost the object associated with it is automatically 
 * reclaimed and a new one is created in it's place. This is  accomplished through a technique called <i>Reference Objects</i>. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public class ObjectPool extends SubComponent implements CalculatedPropertyOwner
{
   private static final long DEFAULT_CLEAN_UP_INTERVAL = 60*1000;
   
   private static final int DEFAULT_MIN_SIZE = 0;
   
   private static final int DEFAULT_MAX_SIZE = 10;
   
   /** NumberProperty for the maximum size of the pool. @since 2.0 */
	protected NumberProperty minSize;
   
   /** NumberProperty for the maximum size of the pool. */
	protected NumberProperty maxSize;
			
	/** NumberProperty for object expirationtime. Object expiration will be checked if this value is > 0. */
	protected NumberProperty objectExpirationTime;
	
	/** NumberProperty for the interval between cleanups. */
	protected NumberProperty cleanUpInterval;

   /* STATISTICAL PROPERTIES */
   
   /** BooleanProperty indicating if statistics is enabled or not. */
   protected BooleanProperty statisticsEnabled;

   /** NumberProperty for the total checkouts. */
   protected NumberProperty totalCheckouts;
   
   /** NumberProperty for average checkout time. */
   protected NumberProperty averageCheckoutTime;
   
      /** NumberProperty for number of checked out objects. */
   protected NumberProperty checkedOutObjects;
   
   /** NumberProperty for for number of checked in objects. */
   protected NumberProperty checkedInObjects;
   
   /** 
    * BooleanProperty indicating if a refecence queue should be used to keep track of "lost" objects. 
    * If this property is set to true, weak references to pool objects will be maintained, which may have a negative effect performance.
    * @since 2.1.1 (20060111) 
    */
   protected BooleanProperty referenceQueueEnabled;
 
	/** Vector containing all checked in objects. */
	protected final LinkedList checkedIn;
	
	/** Hashmap containing weak references to checked out objects. */
	protected final HashMap checkedOut;
	
	/** ReferenceQueue used to reclaim lost references. This field is <code>null</code> if the flag {@link #isReferenceQueueEnabled() referenceQueueEnabled} is false. */
	protected ReferenceQueue refQueue;
	
	
   /* CODE LEVEL PROPERTIES */
   
	/** @since 2.0 */
	private PoolObjectFactory poolObjectFactory = null;
		
	private boolean checkOutMayCreateObject = true;
 	
	/** The poolcleaner. */
	protected PoolCleaner poolCleaner = null;
	
	private int maxAddNewObjectAttempts = 5;
	
	private boolean refillPoolDuringCleanUp = true;
   
   /* STATISTICAL FIELDS */
   
   /** Counter for number of checkouts. */
   private long noOfCheckOuts = 0;
   
   /** Counter for total checkout time. */ 
   private long totalCheckOutTime = 0;
   
   
	/**
	 * Creates an object pool where the pool objects never expire. The min size of the pool will be set to maximum size.
	 * 
	 * @param parent the parent of this pool.
	 * @param name name of the pool.
	 * @param maxSize default value for the maximum size of the pool.
	 * @param cleanUpInterval time in milliseconds between cleanups, used by the associated PoolCleaner.
	 * 
	 * @see com.teletalk.jserver.pool.PoolCleaner
	 * 
	 * @exception IllegalArgumentException maxSize or cleanUpInterval was <= 0.
	 */
   public ObjectPool(SubComponent parent, String name, int maxSize, long cleanUpInterval) throws IllegalArgumentException
	{
	   this(parent, name, DEFAULT_MIN_SIZE, maxSize, cleanUpInterval, 0);
	}
	
	/**
	 * Creates an object pool where the pool objects never expire.
	 * 
	 * @param parent the parent of this pool.
	 * @param name name of the pool.
	 * @param minSize default value for the minumum size of the pool.
	 * @param maxSize default value for the maximum size of the pool.
	 * @param cleanUpInterval time in milliseconds between cleanups, used by the associated PoolCleaner.
	 * 
	 * @see com.teletalk.jserver.pool.PoolCleaner
	 * 
	 * @exception IllegalArgumentException maxSize or cleanUpInterval was <= 0.
	 * 
	 * @since 2.0
	 */
   public ObjectPool(SubComponent parent, String name, int minSize, int maxSize, long cleanUpInterval) throws IllegalArgumentException
	{
	   this(parent, name, minSize, maxSize, cleanUpInterval, 0);
	}
	
	/**
	 * Creates an object pool with a 60 second clean interval and with pool objects that never expire. The min size of the pool will be set to maximum size.
	 * 
	 * @param parent the parent of this pool.
	 * @param name name of the pool.
	 * @param maxSize default value for the maximum size of the pool. If the value of this property is == 0, the pool is growable.
	 * 
	 * @see com.teletalk.jserver.pool.PoolCleaner
	 * 
	 * @exception IllegalArgumentException maxSize was <= 0.
	 */
   public ObjectPool(SubComponent parent, String name, int maxSize) throws IllegalArgumentException
	{
	   this(parent, name, DEFAULT_MIN_SIZE, maxSize, DEFAULT_CLEAN_UP_INTERVAL, 0);
	}
	
	/**
	 * Creates an object pool with a 60 second clean interval. The min size will be set to 0 and the max size to 10.
	 * 
	 * @param parent the parent of this pool.
	 * @param name name of the pool.
	 * @param objectExpirationTime default value for the time in milliseconds that has to pass after an object has been created/checked in 
	 * til it is considered to be expired. Object expiration will be checked if this value is > 0, i.e. a value of 0 disables object expiration checking.
	 * 
	 * @see com.teletalk.jserver.pool.PoolCleaner
	 * 
	 * @exception IllegalArgumentException objectExpirationTime was <= 0.
	 */
   public ObjectPool(SubComponent parent, String name, long objectExpirationTime) throws IllegalArgumentException
	{
		this(parent, name, DEFAULT_MIN_SIZE, DEFAULT_MAX_SIZE, DEFAULT_CLEAN_UP_INTERVAL, objectExpirationTime);
	}
	
	/**
	 * Creates a new object pool. The min size will be set to 0 and the max size to 10.
	 * 
	 * @param parent the parent of this pool.
	 * @param name name of the pool.
	 * @param cleanUpInterval time in milliseconds between cleanups, used by the associated PoolCleaner.
	 * @param objectExpirationTime default value for the time in milliseconds that has to pass after an object has been created/checked in 
	 * til it is considered to be expired. Object expiration will be checked if this value is > 0, i.e. a value of 0 disables object expiration checking.
	 * @see com.teletalk.jserver.pool.PoolCleaner
	 * 
	 * @exception IllegalArgumentException objectExpirationTime or cleanUpInterval was <= 0.
	 */
   public ObjectPool(SubComponent parent, String name, long cleanUpInterval, long objectExpirationTime) throws IllegalArgumentException
	{
	   this(parent, name, DEFAULT_MIN_SIZE, DEFAULT_MAX_SIZE, cleanUpInterval, objectExpirationTime);
	}
	
	/**
	 * Creates a new object pool.
	 * 
	 * @param parent the parent of this pool.
	 * @param name name of the pool.
	 * @param maxSize default value for the maximum size of the pool. If the value of this property is == 0, the pool is growable.
	 * @param cleanUpInterval time in milliseconds between cleanups, used by the associated PoolCleaner.
	 * @param objectExpirationTime default value for the time in milliseconds that has to pass after an object has been created/checked in 
	 * til it is considered to be expired. Object expiration will be checked if this value is > 0, i.e. a value of 0 disables object expiration checking.
	 * 
	 * @see com.teletalk.jserver.pool.PoolCleaner
	 * 
	 * @exception IllegalArgumentException maxSize or cleanUpInterval was <= 0.
	 */
   public ObjectPool(SubComponent parent, String name, int maxSize, long cleanUpInterval, long objectExpirationTime) throws IllegalArgumentException
	{
		this(parent, name, maxSize, maxSize, cleanUpInterval, objectExpirationTime);
	}
	
	/**
	 * Creates a new object pool.
	 * 
	 * @param parent the parent of this pool.
	 * @param name name of the pool.
	 * @param minSize default value for the minumum size of the pool. The value of this property will only be used if max size if set.
	 * @param maxSize default value for the maximum size of the pool. If the value of this property is == 0, the pool is growable. 
	 * @param cleanUpInterval time in milliseconds between cleanups, used by the associated PoolCleaner. 
	 * @param objectExpirationTime default value for the time in milliseconds that has to pass after an object has been created/checked in 
	 * til it is considered to be expired. Object expiration will be checked if this value is > 0, i.e. a value of 0 disables object expiration checking.
	 * 
	 * @see com.teletalk.jserver.pool.PoolCleaner
	 * 
	 * @exception IllegalArgumentException minSize or objectExpirationTime was < 0.
	 * @exception IllegalArgumentException maxSize or cleanUpInterval was <= 0.
	 * 
	 * @since 2.0
	 */
   public ObjectPool(final SubComponent parent, final String name, final int minSize, final int maxSize, final long cleanUpInterval, final long objectExpirationTime) throws IllegalArgumentException
	{
      this(parent, name, minSize, maxSize, cleanUpInterval, objectExpirationTime, null);
	}

	/**
	 * Creates a new object pool.
	 * 
	 * @param parent the parent of this pool.
	 * @param name name of the pool.
	 * @param minSize default value for the minumum size of the pool. The value of this property will only be used if max size if set.
	 * @param maxSize default value for the maximum size of the pool. If the value of this property is == 0, the pool is growable. 
	 * @param cleanUpInterval time in milliseconds between cleanups, used by the associated PoolCleaner. 
	 * @param objectExpirationTime default value for the time in milliseconds that has to pass after an object has been created/checked in 
	 * til it is considered to be expired. Object expiration will be checked if this value is > 0, i.e. a value of 0 disables object expiration checking.
	 * @param poolObjectFactory the PoolObjectFactory to be used for creation, validation and finalization of pool objects. 
	 * 
	 * @see com.teletalk.jserver.pool.PoolCleaner
	 * 
	 * @exception IllegalArgumentException minSize or objectExpirationTime was < 0.
	 * @exception IllegalArgumentException maxSize or cleanUpInterval was <= 0.
	 * 
	 * @since 2.0
	 */
   public ObjectPool(final SubComponent parent, final String name, final int minSize, final int maxSize, final long cleanUpInterval, final long objectExpirationTime, final PoolObjectFactory poolObjectFactory) throws IllegalArgumentException
	{
		super(parent, name);
		
		if(maxSize <= 0) throw new IllegalArgumentException("Max size of pool must be > 0.");
		if(minSize < 0) throw new IllegalArgumentException("Min size of pool must be >= 0.");
		if(cleanUpInterval <= 0) throw new IllegalArgumentException("Cleanup interval must be > 0.");
		if(objectExpirationTime < 0) throw new IllegalArgumentException("Objectexpirationtime must be >= 0.");
		
		this.minSize = new NumberProperty(this, "minSize", minSize, Property.MODIFIABLE_NO_RESTART);
		this.minSize.setDescription("The minimum size of the pool. The value of this property will only be used if max size if set.");
		super.addProperty(this.minSize);
		
		this.maxSize = new NumberProperty(this, "maxSize", maxSize, Property.MODIFIABLE_NO_RESTART);
		this.maxSize.setDescription("The maximum size of the pool. If the value of this property is == 0, the pool is growable.");
		super.addProperty(this.maxSize);
						
		this.objectExpirationTime = new NumberProperty(this, "objectExpirationTime", objectExpirationTime, Property.MODIFIABLE_NO_RESTART);
		this.objectExpirationTime.setDescription("Expiration time of objects in the pool. Object expiration will be checked if this value is > 0, i.e. a value of 0 disables object expiration checking");
		super.addProperty(this.objectExpirationTime);
		
		this.cleanUpInterval = new NumberProperty(this, "cleanupInterval", cleanUpInterval, Property.MODIFIABLE_NO_RESTART);
		this.cleanUpInterval.setDescription("The interval in milliseconds at which this pool is cleaned by the PoolCleaner.");
		super.addProperty(this.cleanUpInterval);
      
      this.statisticsEnabled = new BooleanProperty(this, "statisticsEnabled", false, Property.MODIFIABLE_NO_RESTART);
      this.statisticsEnabled.setDescription("Flag indicating if statistical information should be calculated. This flag dictates if certain statistical properties will be visible.");
      super.addProperty(this.statisticsEnabled);
      
      /* Statistical properties */
      this.averageCheckoutTime = new NumberProperty(this, "averageCheckoutTime", 0);
      this.averageCheckoutTime.setDescription("The avarage time in milliseconds between check out and check in (statisticsEnabled must be 'true').");
      this.totalCheckouts = new NumberProperty(this, "totalCheckouts", 0);
      this.totalCheckouts.setDescription("Total number of checkouts (statisticsEnabled must be 'true').");
      this.checkedOutObjects = new NumberProperty(this, "checkedOutItems", 0);
      this.checkedOutObjects.setDescription("The number of checked out objects (statisticsEnabled must be 'true').");
      this.checkedInObjects = new NumberProperty(this, "checkedInItems", 0);
      this.checkedInObjects.setDescription("The number of checked in objects (statisticsEnabled must be 'true').");
      
      this.referenceQueueEnabled = new BooleanProperty(this, "referenceQueueEnabled", false, Property.MODIFIABLE_OWNER_RESTART);
      this.referenceQueueEnabled.setDescription("Flag indicating if a refecence queue should be used to keep track of \"lost\" objects. If this property is set to true, weak references to pool objects will be maintained, which may have a negative effect performance.");
      super.addProperty(this.referenceQueueEnabled);
		
		this.checkedIn = new LinkedList();
		this.checkedOut = new HashMap();
				
		//refQueue = new ReferenceQueue();
		this.refQueue = null;
      		
		this.poolCleaner = new PoolCleaner(this, cleanUpInterval);
		this.poolCleaner.start();
		
		this.poolObjectFactory = poolObjectFactory;
	}
	
	
	/* ### GETTERS/SETTERS FOR INTERNAL FIELDS BEGIN  ### */
   
	
	/**
	 * Gets the PoolObjectFactory.
    * 
	 * @since 2.0
	 */
   public PoolObjectFactory getPoolObjectFactory()
   {
      return poolObjectFactory;
   }
   
   /**
    * Sets the PoolObjectFactory.
    * 
    * @param poolObjectFactory
    * 
    * @since 2.0
    */
   public void setPoolObjectFactory(PoolObjectFactory poolObjectFactory)
   {
      this.poolObjectFactory = poolObjectFactory;
   }
   
   /**
    * @return Returns the checkOutMayCreateObject.
    * 
    * @since 2.0
    */
   public boolean isCheckOutMayCreateObject()
   {
      return checkOutMayCreateObject;
   }
   
   /**
    * @param checkOutMayCreateObject The checkOutMayCreateObject to set.
    * 
    * @since 2.0
    */
   public void setCheckOutMayCreateObject(boolean checkOutMayCreateObject)
   {
      this.checkOutMayCreateObject = checkOutMayCreateObject;
   }
   
   /**
    * Checks if the Reference Queue is enabled, i.e. if weak references to queue objects are maintained.  
    * 
    * @since 2.0
    */
   public boolean isReferenceQueueEnabled()
   {
      return this.referenceQueueEnabled.booleanValue();
   }
   
   /**
    * Enables or disables the use of the Reference Queue, i.e. if weak references to queue objects are to be maintained.
    * 
    * @since 2.0
    */
   public void setReferenceQueueEnabled(boolean referenceQueueEnabled)
   {
      this.referenceQueueEnabled.setValue(referenceQueueEnabled);
   }
	
	/**
	 * Sets the maximum number of consecutive attempts (retries)  that should be made to create a new object in the pool when an error occurs. The default value 
	 * of this setting is 5.
	 * 
	 * @param maxAddNewObjectAttempts the new value for maximum number of consecutive object creation attempts.
	 */
	public void setMaxAddNewObjectAttempts(int maxAddNewObjectAttempts)
	{
		this.maxAddNewObjectAttempts = maxAddNewObjectAttempts; 
	}
	
	/**
	 * Gets the maximum number of consecutive attempts (retries) that should be made to create a new object in the pool when an error occurs. The default value 
	 * of this setting is 5.
	 * 
	 * @return the maximum number of consecutive object creation attempts.
	 */
	public int getMaxAddNewObjectAttempts()
	{
		return this.maxAddNewObjectAttempts;
	}
	
	/**
	 * Sets the value of the flag indicating if the pool should be filled up to its max size during 
	 * clean up. This may be necessary if for instance objects are reported as bad during clean up 
	 * and need to be replaced.
	 *  
	 * @param refillPoolDuringCleanUp the new value of the flag.
	 */
	public void setRefillPoolDuringCleanUp(boolean refillPoolDuringCleanUp)
	{
		this.refillPoolDuringCleanUp = refillPoolDuringCleanUp;
	}
	
	/**
	 * Gets the value of the flag indicating if the pool should be filled up to its max size during 
	 * clean up. This may be necessary if for instance objects are reported as bad during clean up 
	 * and need to be replaced.
	 * 
	 * @return the value of the flag (boolean).
	 */
	public boolean canRefillPoolDuringCleanUp()
	{
		return this.refillPoolDuringCleanUp;
	}
	
	/**
	 * Gets the cleanUpInterval.
	 * 
	 * @since 2.0
	 */
	public final long getCleanUpInterval()
	{
		return this.poolCleaner.getCleanUpInterval();
	}
	
	/**
	 * Sets the cleanUpInterval.
	 * 
	 * @param cleanUpInterval the cleanUpInterval.
	 */
	public final void setCleanUpInterval(int cleanUpInterval)
	{
		poolCleaner.setCleanUpInterval(cleanUpInterval);
	}
	
	
	/* ### GETTERS/SETTERS FOR INTERNAL FIELDS END ### */
	
	/* ### GETTERS/SETTERS FOR PROPERTIES BEGIN ### */
	
	
	/**
	 * Gets the minimum size of the pool.
	 * 
	 * @return the minSize.
	 * 
	 * @since 2.0
	 */
	public final int getMinSize()
	{
		return this.minSize.intValue();
	}
	
	/**
	 * Sets the minimum size of the pool.
	 *  
	 * @since 2.0
	 */
	public final void setMinSize(int minSize)
	{
		this.minSize.setValue(minSize);
	}
	
	/**
	 * Gets the maximum size of the pool.
	 * 
	 * @return the maxSize.
	 */
	public final int getMaxSize()
	{
		return this.maxSize.intValue();
	}
	
	/**
	 * Sets the maximum size of the pool.
	 */
	public final void setMaxSize(int maxSize)
	{
		this.maxSize.setValue(maxSize);
	}
	
	/**
	 * Gets the object expiration time, i.e. the time in milliseconds that has to pass after an object 
	 * has been created/checked in til it is considered to be expired. Object expiration will be checked if this value is > 0.
	 * 
	 * @return the objectExpirationTime in milliseconds.
	 */
	public long getObjectExpirationTime()
	{
		return this.objectExpirationTime.longValue();
	}
	
	/**
	 * Sets the object expiration time, i.e. the time in milliseconds that has to pass after an object 
	 * has been created/checked in til it is considered to be expired. Object expiration will be checked if this value is > 0.
	 * 
	 * @param objectExpirationTime the objectExpirationTime in milliseconds.
	 */
	public void setObjectExpirationTime(long objectExpirationTime)
	{
		this.objectExpirationTime.setValue(objectExpirationTime);
	}
	
	/**
	 * Returns the number of checked out objects.
	 * 
	 * @return the number of checked out objects.
	 */
	public synchronized final int getCheckedOutSize()
	{
		return checkedOut.size();
	}
	
	/**
	 * Returns the number of checked in objects.
	 * 
	 * @return the number of checked in objects.
	 */
	public synchronized final int getCheckedInSize()
	{
		return checkedIn.size();
	}
	
	/**
	 * Returns the total number of objects in the pool (checked out and checked in).
	 * 
	 * @return the total number of objects in the pool.
	 */
	public synchronized final int getPoolSize()
	{
		return this.checkedOut.size() + this.checkedIn.size();
	}
	
	/**
	 * Calculates the average checked out time.
	 * 
	 * @return the average checked out time
	 */
	public final int getAverageCheckedOutTime()
	{
		synchronized(statisticsEnabled)
		{
			if(totalCheckOutTime > 0 && noOfCheckOuts > 0) return (int)(totalCheckOutTime / noOfCheckOuts);
			else return 0;
		}
	}
	
	
	/* ### GETTERS/SETTERS FOR PROPERTIES END ### */
	
	/* ### CHECK POOL UTILITY METHODS BEGIN ### */
	
	
	/**
	 * Resizes the pool to the correspond to the minSize and maxSize properties.
	 */
	protected synchronized void resize()
	{
		final int minSize = this.minSize.intValue();
		final int maxSize = this.maxSize.intValue();
		final int poolSize = this.getPoolSize();
		PoolObjectContainer poolObj;
		int itemsToRemove;
		//int errCount = 0;
				
		if( minSize > poolSize )
		{
			this.fillPool(minSize - poolSize);
		}
		else if( poolSize > maxSize )
		{
			itemsToRemove = poolSize - maxSize;
			
			int removed = 0;
			int checkedInSize = this.checkedIn.size();
			
			// Remove checked in objects
		   for(; (removed < itemsToRemove) && (removed < checkedInSize); removed++)
			{
				poolObj = (PoolObjectContainer)this.checkedIn.removeFirst();
					
				if(poolObj != null)
				{
					this.finalizeObject(poolObj.destroy());
					poolObj = null;
				}
			}
		   
		   // Remove checked out objects
		   if( removed < itemsToRemove )
		   {
				Iterator it = checkedOut.keySet().iterator();
				Object hashcode = null;
										
				for(;(removed < itemsToRemove) && it.hasNext(); removed++)
				{
					hashcode = it.next();
					poolObj = (PoolObjectContainer)checkedOut.get(hashcode);
					
					if( poolObj != null )
					{
						poolObj.destroy();
						poolObj = null;				
					}
				}
		   }
			
			// Expedite garbage collection 
			System.runFinalization();
			System.gc();
		}
		if(isDebugMode()) logDebug("Pool resized - size: " + this.getPoolSize() + ", minSize: " + minSize + ", maxSize: " + maxSize + ".");
	}
	
	/**
	 * Adds <code>fillCount</code> number of new objects to the pool.
	 * 
	 * @param fillCount size number of new objects to create.
	 */
	public synchronized final void fillPool(final int fillCount)
	{
		int errCount = 0;
      int actualfillCount = 0;
      boolean errorOccurred = false;
      boolean maxAttemptsExceeded = false;
		
		if(this.isDebugMode()) logDebug("Adding " + fillCount + " objects to the pool.");
		
		for(int i=0; i<fillCount; i++)
		{
			try
			{
				this.doAddNewObject();
				errCount = 0;
            actualfillCount++;            
			}
			catch(Exception e)
			{
            errorOccurred = true;
				errCount++;
				logError("Error when creating pool object (error count: " +errCount + ")", e);
				
				if( (errCount >= maxAddNewObjectAttempts) && (maxAddNewObjectAttempts >= 0) )
				{
               maxAttemptsExceeded = true;
					logError(maxAddNewObjectAttempts + " consecutive pool object creation attempts! Unable to add pool objects!");
					break;
				}
			}
		}      
      
      if( errorOccurred && !maxAttemptsExceeded )
      {
         logInfo("Errors occurred while filling pool - " + actualfillCount + " added. Requested fill size: " + fillCount + ".");
      }
		
		// Notify threads that are waiting in one of the check out methods
		this.notifyAll();
	}
	
	/**
	 * Waits for all checked out objects to be returned to the pool.
	 * 
	 * @param waitTime maximum wait time in milliseconds.
	 */
	public synchronized final void waitForCheckedOutObjects(long waitTime)
	{
		long waitStart = System.currentTimeMillis();
		long remainingWaitTime;
			
		while(checkedOut.size() > 0)
		{
			remainingWaitTime = waitTime - (System.currentTimeMillis() - waitStart);
								
			if(remainingWaitTime > 0)
			{
				try
				{
					this.wait(remainingWaitTime);
				}
				catch(InterruptedException ie)
				{
					logWarning("Interrupted while waiting for checked out objects to be returned to the pool.");
					break;
				}
			}
			else break;
		}
	}
	
	/**
	 * Waits for all checked out objects to be returned to the pool then drains the pool.
	 * 
	 * @param waitTime maximum wait time in milliseconds.
	 */
	public synchronized final void drainPool(long waitTime)
	{
		waitForCheckedOutObjects(waitTime);
		drainPool();
	}
		
	/**
	 * Drains the pool.
	 */
	public synchronized final void drainPool()
	{
		PoolObjectContainer poolObj;
		Object obj;
		
		while(!checkedIn.isEmpty())
		{
			poolObj = ((PoolObjectContainer)checkedIn.removeFirst());
			obj = poolObj.destroy();
			poolObj = null;
			finalizeObject(obj);
			obj = null;
		}
		
		this.checkedIn.clear();
		
		Iterator it = checkedOut.keySet().iterator();
		Object hashcode = null;
								
		while(it.hasNext())
		{
			hashcode = it.next();
			poolObj = (PoolObjectContainer)checkedOut.get(hashcode);
			
			if( poolObj != null )
			{
				poolObj.destroy();
				poolObj = null;				
			}
		}
		
		this.checkedOut.clear();
	}
	
	/**
	 * Adds a new object to the pool. The actual creation of the new object is done in {@link #createObject()}.
	 */
	protected synchronized final void addNewObject() throws Exception
	{
		if(doAddNewObject())
		{
			// Notify threads that are waiting in one of the check out methods
			this.notifyAll();
		}
	}
	
	private boolean doAddNewObject() throws Exception
	{
		final Object obj = createObject();
				
		if(obj != null)
		{
			checkedIn.add(new PoolObjectContainer(obj, this.refQueue));
			if(isDebugMode()) logDebug("New object added: " + obj.toString());
			return true;
		}
		else
		{
			if(isDebugMode()) logDebug("Failed to create new object, createObject returned null.");
			return false;
		}
	}
	
	/**
	 * Internal method to create a new object and mark it as checked out.
	 */
	private final Object createCheckedOutObject() throws Exception
	{
		final Object obj = createObject();
		
		if(obj == null) 
		{
			logWarning("CreateObject returned a null object.");
			return null;
		}

		if(isDebugMode()) logDebug("Returning new object: " + obj.toString());

		PoolObjectContainer poolObj = new PoolObjectContainer(obj, this.refQueue);
		poolObj.checkOut();
		checkedOut.put(poolObj.getObjectHashcode(), poolObj);

		poolObj = null;
		
		//Increment counter for number of checkouts
		if(statisticsEnabled.booleanValue()) noOfCheckOuts++;

		return obj;
	}
	
	/**
	 * Internal method to check if a new object can be created.
	 */
	private final boolean canCreateNewPoolObject()
	{
	   return (this.getPoolSize() < this.getMaxSize());
	   //return (getMaxSize() == 0) || (this.getPoolSize() < this.getMaxSize());
	}
	
	
	/* ### CHECK POOL UTILITY METHODS END ### */
	
	/* ### CHECK OUT/CHECK IN METHODS BEGIN ### */
	
	
	/**
	 * Internal method to check out the first valid object.
	 */
	private final Object checkOutFirstValid() throws Exception
	{
		Object obj = null;
		PoolObjectContainer poolObj;

		while(!checkedIn.isEmpty())	
		{
			poolObj = (PoolObjectContainer)checkedIn.removeFirst();

			if(validatePoolObject(poolObj, false))
			{
				checkedOut.put(poolObj.getObjectHashcode(), poolObj);
				if(isDebugMode()) logDebug("Object checked out: " + poolObj.getObject().toString());
				obj = poolObj.checkOut();

				//Increment counter for number of checkouts
				if(statisticsEnabled.booleanValue()) noOfCheckOuts++;

				poolObj = null;
				break;
			}
			else
			{
				if(isDebugMode()) logDebug("Object failed validation: " +	poolObj.getObject().toString());
				badPoolObject(poolObj);
			}
		}

		// If there where no valid objects in the pool
		if(obj == null)
		{
			// Check if the current pool size is less than the max size 
			if( this.canCreateNewPoolObject() )
			{
				obj = this.createCheckedOutObject(); // Create a new checked out object
			}
			else if(isDebugMode()) logDebug("No valid objects found in pool!");
		}
		
		return obj;
	}
	
	/**
	 * Gets an object from the pool. If the pool is empty or there are no valid items, this method will block the calling 
	 * thread until a valid item can be produced. If the pool is in dynamic mode (object expiration time > 0) and there are no checked out or 
	 * checked in objects, a new object will be created and returned.
	 * 
	 * @return an object, null if the pool is disabled. Null is also returned if there was an error during check out or if the calling thread was interrupted.
	 */
	public synchronized final Object checkOutWait()
	{
		return this.checkOutWait(-1L);
	}
	
	/**
	 * Gets an object from the pool. If the pool is empty or there are no valid items, this method will block the calling 
	 * thread until a valid item can be produced or the specified <code>waitTime </code> ellapses. If the pool is in dynamic mode 
	 * (object expiration time > 0) and there are no checked out or checked in objects, a new object will be created and returned.
	 * 
	 * @param waitTime maximum wait time in milliseconds.
	 * 
	 * @return an object, null if the pool is disabled. Null is also returned if there was an error during check out or if the calling thread was interrupted.
	 */
   public synchronized final Object checkOutWait(final long waitTime)
   {
      Object obj = null;
      final long waitStart = System.currentTimeMillis();
            
      if(isEnabled())
      {
         try
         {
            obj = this.checkOutFirstValid();
            
            // If checkOutFirstValid returned null and there are checked out objects -> wait for checked out objects to get checked in
            if( (obj == null) && (this.getCheckedOutSize() > 0) )
            {
               long remainingWaitTime = 1; // Set remainingWaitTime to 1 as default (for unlimited wait time...) 
               
               while( (obj == null) && (remainingWaitTime > 0) ) // Break loop if wait time is exceeded (remainingWaitTime <= 0)
               {
                  if(waitTime < 0) // Wait forever
                  {
                     while( checkedIn.isEmpty() )
                     {
                        this.wait();
                     }
                     obj = this.checkOutFirstValid();
                  }
                  else // Limited wait
                  {
                     if( checkedIn.isEmpty() )
                     {
                        remainingWaitTime = waitTime - (System.currentTimeMillis() - waitStart);
                        if( remainingWaitTime > 0 ) this.wait(remainingWaitTime);
                     }
                     remainingWaitTime = waitTime - (System.currentTimeMillis() - waitStart);
                     
                     obj = this.checkOutFirstValid();
                  }
               }
            }
         }
         catch(InterruptedException e)
         {
            logWarning("Interrupted during check out", e);
            obj = null;
         }
         catch(Exception e)
         {
            logError("Error during check out", e);
            obj = null;
         }
      }

      return obj;
   }
	
	/**
	 * Gets an object from the pool. If the pool is empty or if there are no valid items, null will be returned.
	 * 
	 * @return an object, null if the pool is disabled, empty or if it contained no valid items. Null is also returned if there was an error during check out.
	 */
	public synchronized final Object checkOutIfAny()
	{
		Object obj = null;
		
		if(isEnabled())
		{
			try
			{
				obj = checkOutFirstValid();
			}
			catch(Exception e)
			{
				logError("Error during check out", e);
				obj = null;
			}
		}
		
		return obj;
	}
	
	/**
	 * Gets an object from the pool. If the pool is empty or if there are no valid items, a new object 
	 * will be created and returned only if the flag {@link #isCheckOutMayCreateObject() checkOutMayCreateObject} is set to <code>true</code>. 
	 * When this object is eventually checked in it will not be added to the pool.
	 * 
	 * @return an object, null if the pool is disabled. Null is also returned if there was an error during check out or the pool was 
	 * empty and the flag {@link #isCheckOutMayCreateObject() checkOutMayCreateObject} is set to <code>false</code>.
	 */
	public final Object checkOut() 
	{
	   return this.checkOut(this.isCheckOutMayCreateObject());
	}
	
	/**
	 * Gets an object from the pool. If the pool is empty or if there are no valid items, a new object 
	 * will be created and returned only if the flag <code>mayCreateObject</code> is set to <code>true</code>. 
	 * When this object is eventually checked in it will not be added to the pool.
	 * 
	 * @return an object, null if the pool is disabled. Null is also returned if there was an error during check out or the pool was 
	 * empty and the flag <code>mayCreateObject</code> is set to <code>false</code>.
	 * 
	 * @since 2.0
	 */
	public synchronized final Object checkOut(boolean mayCreateObject) 
	{
		Object obj = null;
		//PoolObjectContainer poolObj;
		
		if(isEnabled())
		{
			try
			{
				obj = checkOutFirstValid();
				
				if( (obj == null) && mayCreateObject )
				{
					// Create new object even if pool is empty...
				   try
					{
			         // Increment counter for number of checkouts
			         obj = createObject();
			         if(statisticsEnabled.booleanValue()) noOfCheckOuts++;
				      
				      if(isDebugMode()) logDebug("Returning new object: " + obj.toString());
					}
					catch(Exception e)
					{
						logError("Error while checking out! Unable to create new pool object", e);
						obj = null;
					}
				}
			}
			catch(Exception e)
			{
				logError("Error during check out", e);
				obj = null;
			}
		}
		
		return obj;
	}
	
	/**
	 * Puts an object back in to the pool.
	 * 
	 * @param obj an object to check in.
	 */
	public synchronized final void checkIn(final Object obj)
	{
		if( obj != null )
		{
			PoolObjectContainer poolObj;
			Integer hashCode = new Integer(System.identityHashCode(obj));
			poolObj = (PoolObjectContainer)checkedOut.remove(hashCode);
			
			if(isEnabled())
			{
				if(statisticsEnabled.booleanValue() && poolObj != null)
				{
					totalCheckOutTime += (System.currentTimeMillis() - poolObj.getLastAccess());
				}
				
				//if((getMaxSize() == 0) || (this.checkedIn.size() < this.getMaxSize()))
				if( this.checkedIn.size() < this.getMaxSize() )
				{
					//if(poolObj != null)
               if( (poolObj != null) && validatePoolObject(poolObj, false) )
					{
						poolObj.checkIn(obj);
						checkedIn.add(poolObj);
						if(isDebugMode()) logDebug("Object returned to pool: " + obj.toString());
						
						// Notify threads that are waiting in one of the check out methods
						this.notifyAll();
					}
					else
					{
						if(isDebugMode()) logDebug("Object not returned to pool: " + obj.toString());
						finalizeObject(obj);
					}
				}
				else
				{
					if(isDebugMode()) logDebug("Object not returned to pool (pool full): " + obj.toString());
					finalizeObject(obj);
				}
			}
			else if(poolObj != null) finalizeObject(poolObj.destroy());
			else finalizeObject(obj);
			
			poolObj = null;
			hashCode = null;
		}
	}
	
	/**
	 * Called to indicate that the checked in PoolObject specified by parameter poolObj was bad and is to be removed.
	 * 
	 * @param poolObj a bad PoolObject.
	 */
	protected synchronized final void badPoolObject(PoolObjectContainer poolObj) //, boolean createNew)
	{
		if(this.isDebugMode()) logDebug("Reporting checked in object " +((poolObj.getObject() != null) ? poolObj.getObject().toString() : "null") + " as bad.");
		
		checkedIn.remove(poolObj);
		finalizeObject(poolObj.destroy());
	}
	
	/**
	 * Called to indicate that the checked out object specified by parameter obj was bad and a new one must be created.
	 * 
	 * @param obj a bad object.
	 */
	public synchronized final void badObject(Object obj)
	{
		PoolObjectContainer poolObj;
		
		if(isDebugMode()) logDebug("Bad object reported: " + obj.toString());
		poolObj = (PoolObjectContainer)checkedOut.remove(new Integer(System.identityHashCode(obj)));
		
		if(poolObj != null)
		{
			finalizeObject(poolObj.destroy());
			
			try
			{
				addNewObject();
			}
			catch(Exception e)
			{
				logError("Error while reporting bad object - unable to create new", e);
			}
		}
		else finalizeObject(obj);
	}
	
	/**
	 * Called to indicate that the object specified by parameter obj was bad and a new one
	 * is required.
	 * 
	 * @param obj a bad object.
	 * 
	 * @return a new object.
	 */
	public synchronized final Object badObjectGetNew(Object obj)
	{
		badObject(obj);
		return checkOut();
	}
	
	
	/* ### CHECK OUT/CHECK IN METHODS END ### */
	
	
	/* ### RECYCLING & VALIDATION METHODS BEGIN ### */
	
	
	/**
	 * Method for cleaning up the pool. This method is used by the PoolCleaner to run periodic clean ups of the pool. 
    * The interval of the clean ups are determined by the property {@link #getCleanUpInterval() object expirationtime}.  
	 * 
	 * @see PoolCleaner
	 */
	protected final void cleanUp()
	{
		//boolean cleanUpSucceded = true;
		//long now = System.currentTimeMillis();
		int createNew = 0;
		Object obj;
		PoolObjectContainer poolObj;
				
		//if(getDebugMode()) logDebug("Clean up!");

		synchronized(this)
		{
			PoolObjectContainer[] objectArray = (PoolObjectContainer[])checkedIn.toArray(new PoolObjectContainer[]{});
			
			int currentSize = objectArray.length + checkedOut.size();
						
			//Check if there are to few objects in the pool...
			if( this.refillPoolDuringCleanUp && (this.getMinSize() > 0) && (currentSize < this.getMinSize()) ) 
			{
				if(this.isDebugMode()) logDebug("Clean up - too few objects in pool! Min size: " + this.getMinSize(), ", current size: " + currentSize + ". Adding new objects.");
				createNew += (this.getMinSize() - currentSize);
			}
			
			for(int i=0; i<objectArray.length; i++)
			{
				poolObj = objectArray[i];
				if(poolObj != null)
				{
					obj = poolObj.getObject();
				
					if(obj != null && !validatePoolObject(poolObj, true))
					{
						if(this.isDebugMode()) logDebug("Clean up - object " + obj.toString() + " failed validation!");
						badPoolObject(poolObj);
						poolObj = null;
						
						if( this.refillPoolDuringCleanUp ) createNew++;
						//if( this.refillPoolDuringCleanUp && (getMaxSize() > 0) ) createNew++; // Only create new object to replace the bad one if max size is > 0
					}
				}
				objectArray[i] = null;
			}
			objectArray = null;
		}
		
		if( refQueue != null )
		{
			// Check for lost references
			createNew += this.checkForLostReferences();
		}
		
		if( this.refillPoolDuringCleanUp )
		{
			if(createNew > 0)
			{
				if(this.isDebugMode()) logDebug("Clean up - refilling pool with " + createNew + " objects.");
				this.fillPool(createNew);
			}
		}
		
		obj = null;
		poolObj = null;
	}
	
	private synchronized int checkForLostReferences()
	{
		PoolObjectContainer poolObj, tmpPoolObj;
		Iterator it;
		Reference ref;
		Object tmpHashcode, hashcode;
		int createNew = 0;
		
		ref = refQueue.poll();
		
		if(ref!=null)
		{
			while(ref!=null)
			{
				try
				{
					it = checkedOut.keySet().iterator();

					poolObj = null;
					hashcode = null;
						
					while(it.hasNext())
					{
						tmpHashcode = it.next();
						tmpPoolObj = (PoolObjectContainer)checkedOut.get(tmpHashcode);
							
						if(tmpPoolObj.getReference() == ref)
						{
							poolObj = tmpPoolObj;
							hashcode = tmpHashcode;
						}
					}
				}
				catch(Exception e)
				{
					if(isDebugMode()) logDebug("Clean up - got exception while iterating through unreferenced checkedIn: " + e);
					break;
				}

				if(poolObj!=null)
				{
					checkedOut.remove(hashcode);
					poolObj.destroy();
					createNew++;
				}
						
				ref = refQueue.poll();
			}
		}
		
		if( (createNew > 0) && isDebugMode() )
		{
			if(createNew == 1) logDebug("Clean up - unreferenced checked out object was reclaimed by garbage collector. Creating a new one.");
			else logDebug("Clean up - " + createNew + " unreferenced checked out objects was reclaimed by garbage collector. Creating replacement objects.");
		}
		
		return createNew;
	}
	
	/**
	 * Method to validate a PoolObject by among other things checking object expiration and validating
	 * the object containied in the PoolObject.
	 * 
	 * @param poolObj the PoolObject to validate.
    * @param cleanUpValidation flag indicating if this method was invoked from the {@link #cleanUp() clean up} method of this pool (<code>true<code>).
	 * 
	 * @return true if the object passed the validation, otherwise false.
	 */
	protected final boolean validatePoolObject(final PoolObjectContainer poolObj, final boolean cleanUpValidation)
	{
		if(poolObj.validate() && validateObject(poolObj.getObject(), cleanUpValidation))
		{
			if(objectExpirationTime.longValue() > 0)
			{
				boolean result = !((System.currentTimeMillis() - poolObj.getLastAccess()) > objectExpirationTime.longValue());
				if(!result) if(isDebugMode()) logDebug("Object expired: " + poolObj.toString());

				return result;
			}
			else return true;
		}
		else
		{
			logInfo("Object failed validation: " + poolObj.toString());
			return false;
		}
	}
	
	
	/* ### RECYCLING & VALIDATION METHODS END ### */
	
	/* ### SUB CLASS INTERFACE BEGIN ### */
		
	
	/**
	 * Method to create an object.
	 * 
	 * @return the newly created object.
	 */
	public Object createObject() throws Exception
	{
	   if( this.poolObjectFactory != null ) return this.poolObjectFactory.createObject();
	   else throw new RuntimeException("PoolObjectFactory not set! If the method createObject() is not overridden by a subclass, a PoolObjectFactory must be specified for the pool.");
	}
   
   /**
    * Method to validate an object.
    * 
    * @param obj the object to validate.
    * @param cleanUpValidation flag indicating if this method was invoked from the {@link #cleanUp() clean up} method of this pool (<code>true<code>).
    * 
    * @return true if the object passed the validation, otherwise false.
    */
   public boolean validateObject(Object obj, boolean cleanUpValidation)
   {
      if( this.poolObjectFactory != null ) return this.poolObjectFactory.validateObject(obj, cleanUpValidation);
      else throw new RuntimeException("PoolObjectFactory not set! If the method validateObject(Object) is not overridden by a subclass, a PoolObjectFactory must be specified for the pool.");
   }
	
	/**
	 * Method to finalize an object.
	 * 
	 * @param obj an object to be finalized.
	 */
	public void finalizeObject(Object obj)
	{
	   if( this.poolObjectFactory != null ) this.poolObjectFactory.finalizeObject(obj);
	   else throw new RuntimeException("PoolObjectFactory not set! If the method finalizeObject(Object) is not overridden by a subclass, a PoolObjectFactory must be specified for the pool.");
	}
	
	
	/* ### SUB CLASS INTERFACE END ### */
	
	/* ### MISC SUBCOMPOMENT / PROPERTY HANDLING METHODS BEGIN ### */
	
	
	/**
	 * Calculates properties that needs to be calculated.
	 */
	public void calculateProperties()
	{
		if(statisticsEnabled.booleanValue())
		{
         this.averageCheckoutTime.setValue(this.getAverageCheckedOutTime());
         this.totalCheckouts.setValue(this.noOfCheckOuts);
         this.checkedOutObjects.setValue(this.getCheckedOutSize());
         this.checkedInObjects.setValue(this.getCheckedInSize());
		}
	}
	
	/**
	 * Called when a property owned by this ObjectPool has changed.
	 * 
	 * @param property the property that has changed.
	 */
	public void propertyModified(Property property)
	{
		if( property == maxSize ) 
      {
			if( isEnabled() ) 
			{
			   if( maxSize.intValue() < minSize.intValue() )
			   {
			      boolean oldNotificationMode = minSize.getNotificationMode();
			      minSize.setNotificationMode(false);
			      minSize.setValue(maxSize.intValue());
			      minSize.setNotificationMode(oldNotificationMode);
			   }
			   this.resize();
			}
      }
      else if( property == minSize ) 
      {
			if( isEnabled() ) 
			{
			   if( maxSize.intValue() < minSize.intValue() )
			   {
			      boolean oldNotificationMode = maxSize.getNotificationMode();
			      maxSize.setNotificationMode(false);
			      maxSize.setValue(minSize.intValue());
			      maxSize.setNotificationMode(oldNotificationMode);
			   }
			   this.resize();
			}
      }
		else if(property == cleanUpInterval)
      { 
			setCleanUpInterval(cleanUpInterval.getValueAsNumber().intValue());
      }
		else if(property == statisticsEnabled)
		{
         this.updateStatisticsMode();
		}
		super.propertyModified(property);
	}
   
   /**
    */
   private void updateStatisticsMode()
   {
      if(statisticsEnabled.booleanValue())
      {
         synchronized(statisticsEnabled)
         {
            noOfCheckOuts = 0;
            totalCheckOutTime = 0;
         }
            
         super.addProperty(this.averageCheckoutTime);
         super.addProperty(this.totalCheckouts);
         super.addProperty(this.checkedOutObjects);
         super.addProperty(this.checkedInObjects);
      }
      else
      {
         super.removeProperty(this.averageCheckoutTime);
         super.removeProperty(this.totalCheckouts);
         super.removeProperty(this.checkedOutObjects);
         super.removeProperty(this.checkedInObjects);
      }
   }
	
	/**
	 * Validates a modification of a property's value. Subclasses can override this
	 * method to provide more specialized behaviour. This implementation validates various ObjectPool 
	 * properties like maximum poolsize. For that reason it is recommended that subclasses that override this method 
	 * calls the superclass implementation in an appropriate manner.
	 * 
	 * @param property The property to be validated.
	 * 
	 * @return boolean value indicating if the property passed (true) validation or not (false).
	 * 
	 * @see com.teletalk.jserver.property.PropertyOwner
	 */
	public boolean validatePropertyModification(Property property)
	{
		//if(property == maxSize) return maxSize.longValue() >= 0;
	   if(property == maxSize) return maxSize.longValue() > 0;
	   else if(property == minSize) return minSize.longValue() >= 0;
		else if(property == cleanUpInterval) return cleanUpInterval.longValue() >= 1000;
		else return super.validatePropertyModification(property);
	}
	
	/**
	 * Enables and fills the pool. The pool is filled to
	 * maxSize, if maxSize is larger than 0.
	 */
	protected void doInitialize()
	{
		super.doInitialize();
      
      // Attempt to get old property "min size"
      super.initFromConfiguredProperty(this.minSize, "min size", false, true);
      // Attempt to get old property "max size"
      super.initFromConfiguredProperty(this.maxSize, "max size", false, true);
      // Attempt to get old property "object expirationtime(ms)" to be used if "object expirationtime" property is missing
      super.initFromConfiguredProperty(this.objectExpirationTime, "object expirationtime(ms)", false, true);
      // Attempt to get old property "cleanup interval(ms)" to be used if "cleanup interval" property is missing
      super.initFromConfiguredProperty(this.cleanUpInterval, "cleanup interval(ms)", false, true);
      // Attempt to get old property "statistics enabled"
      super.initFromConfiguredProperty(this.statisticsEnabled, "statistics enabled", false, true);
		
		if( this.isReferenceQueueEnabled() ) refQueue = new ReferenceQueue();
		else refQueue = null;
      
      this.updateStatisticsMode();

		if(poolCleaner == null)
		{
			poolCleaner = new PoolCleaner(this, cleanUpInterval.longValue());
			poolCleaner.start();
		}

		if(maxSize.intValue() > 0)
      {
			//this.fillPool(maxSize.intValue());
			this.fillPool(minSize.intValue());
      }
	}
	
	/**
	 * Disables and drains the pool.
	 */
	protected void doShutDown()
	{
		super.doShutDown();
		
		refQueue = null;

		drainPool(1500);
		poolCleaner.kill();
		poolCleaner = null;
		
		// Expedite garbage collection 
		System.runFinalization();
		System.gc();
	}

	
	/* ### MISC SUBCOMPOMENT / PROPERTY HANDLING METHODS END ### */
}
