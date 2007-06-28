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

import java.util.ArrayList;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.teletalk.jserver.pool.ObjectPool;
import com.teletalk.jserver.pool.PoolObjectFactory;
import com.teletalk.jserver.util.MessageQueueTest;

/**
 * 
 * @author Tobias Löfstrand
 */
public class ObjectPoolTest extends TestCase
{
   private static final Log logger = LogFactory.getLog(ObjectPoolTest.class);
   
   
   public static final int N_REPETITIONS = 1000;
   
   private static int failCount = 0;
   
   
   private static void resetFailCount()
   {
      synchronized (ObjectPoolTest.class)
      {
         failCount = 0;
      }
   }
   
   private static void incrementFailCount()
   {
      synchronized (ObjectPoolTest.class)
      {
         failCount++;
      }
   }
   
   private static int getFailCount()
   {
      synchronized (ObjectPoolTest.class)
      {
         return failCount;
      }
   }
   
   
   /* ### TEST METHODS ### */
   
   
   /**
    */
   public void testCheckOut()
   {
      logger.info("BEGIN testCheckOut.");
      
      int poolSize = 10;
      
      ObjectPool pool = new ObjectPool(null, "ObjectPool", poolSize);
      pool.setPoolObjectFactory(new TestPoolObjectFactory());
      pool.engage();
      
      ArrayList checkedOut = new ArrayList(); // Keep references to checked out objects
      
      for(int i=0; i<poolSize; i++)
      {
         checkedOut.add(pool.checkOut());
      }
      
      if( pool.checkOutIfAny() != null )
      {
         super.fail("Method checkOutIfAny() should return null!");
      }
      if( pool.checkOut(false) != null )
      {
         super.fail("Method checkOut(false) should return null!");
      }
      if( pool.checkOutWait(10) != null )
      {
         super.fail("Method checkOutWait(10) should return null!");
      }
            
      // Return one object
      pool.checkIn(checkedOut.get(0));
      
      Object object = pool.checkOutIfAny();
      if( object == null )
      {
         super.fail("Method checkOutIfAny() should return an object!"); 
      }
      pool.checkIn(object);
      
      object = pool.checkOutWait(10);
      if( object == null )
      {
         super.fail("Method checkOutWait(10) should return an object!");
      }
      pool.checkIn(object);
      
      object = pool.checkOut(false);
      if( object == null )
      {
         super.fail("Method checkOut(false) should return an object!");
      }
      pool.checkIn(object);
      
      object = pool.checkOutIfAny();
      if( object == null )
      {
         super.fail("Method checkOutIfAny() should return an object!");
      }
      // Don't check in - pool should be empty before final test
      
      if( pool.checkOut() == null )
      {
         super.fail("Method checkOut() should return an object!");
      }
      
      logger.info("END testCheckOut.");
   }
   
   /**
    */
   public void testMultiThreadCheckOut()
   {
      logger.info("BEGIN testMultiThreadCheckOut.");
      
      runMultiThreadTest("testMultiThreadCheckOut", 0);
      
      logger.info("END testMultiThreadCheckOut.");
   }
   
   /**
    */
   public void testMultiThreadCheckOutWait()
   {
      logger.info("BEGIN testMultiThreadCheckOutWait.");
      
      runMultiThreadTest("testMultiThreadCheckOutWait", 1);
      
      logger.info("END testMultiThreadCheckOutWait.");
   }
   
   
   /* ### ### */
   
   
   private void runMultiThreadTest(final String testName, final int mode)
   {
      resetFailCount();
      
      int n = 10;
      
      CheckOutCheckInThread checkOutCheckInThread[] = new CheckOutCheckInThread[n];
            
      ObjectPool pool = new ObjectPool(null, "ObjectPool", 10);
      pool.setPoolObjectFactory(new TestPoolObjectFactory());
      pool.engage();
         
      for(int i=0; i<n; i++)
      {
         checkOutCheckInThread[i] = new CheckOutCheckInThread(i, pool, mode);
         checkOutCheckInThread[i].setDaemon(true);
      }
      
      for(int i=0; i<n; i++)
      {
         checkOutCheckInThread[i].start();
      }
      
      long beginWait = System.currentTimeMillis();
      long waitTime;
      for(int i=0; i<n; i++)
      {
         waitTime = 10000 - (System.currentTimeMillis() - beginWait);
         if( waitTime > 0 )
         {
            try{
            checkOutCheckInThread[i].join(waitTime);
            }catch (Exception e){}
         }
         checkOutCheckInThread[i].interrupt();
      }
         
      if( getFailCount() > 0 )
      {
         super.fail("Error in " + testName + " - failCount: " + getFailCount() + ".");
      }
   }
   
   
   private static class TestPoolObjectFactory implements PoolObjectFactory
   {
      private int counter = 0;
      
      public Object createObject() throws Exception
      {
         synchronized(this)
         {
            return "Object" + (counter++);
         }
      }

      public void finalizeObject(Object obj){}

      public boolean validateObject(Object obj, boolean cleanUpValidation)
      {
         return true;
      }
   }
   
   
   /**
    */
   private static class CheckOutCheckInThread extends Thread
   {
      private int mode;
      
      private int threadId;
      
      private ObjectPool pool;
      
      public CheckOutCheckInThread(int threadId, ObjectPool pool, int mode)
      {
         this.threadId = threadId;
         this.pool = pool;
         this.mode = mode;
      }
      
      public void run()
      {
         int i = 0;
         Object object = null;
         
         for(; (i<N_REPETITIONS) && (!Thread.currentThread().isInterrupted()); i++)
         {
            try
            {
               if( this.mode == 0 )
               {
                  if( (i%2) == 0 )
                  {
                     object = pool.checkOut(false);
                        
                     if( object == null )
                     {
                        if( ObjectPoolTest.logger.isDebugEnabled() ) ObjectPoolTest.logger.debug("CheckOutCheckInThread" + threadId + ": checkOut(false) returned null!");
                        incrementFailCount();
                     }
                  }
                  else
                  {
                     object = pool.checkOutIfAny();
                     
                     if( object == null )
                     {
                        if( ObjectPoolTest.logger.isDebugEnabled() ) ObjectPoolTest.logger.debug("CheckOutCheckInThread" + threadId + ": checkOut(false) returned null!");
                        incrementFailCount();
                     }
                  }
               }
               else
               {
                  if( (i%2) == 0 )
                  {
                     object = pool.checkOutWait(100);
                     
                     if( object == null )
                     {
                        if( ObjectPoolTest.logger.isDebugEnabled() ) ObjectPoolTest.logger.debug("CheckOutCheckInThread" + threadId + ": checkOutWait(100) returned null!");
                        incrementFailCount();
                     }
                  }
                  else
                  {
                     object = pool.checkOutWait();
                     
                     if( object == null )
                     {
                        if( ObjectPoolTest.logger.isDebugEnabled() ) ObjectPoolTest.logger.debug("CheckOutCheckInThread" + threadId + ": checkOutWait(1000) returned null!");
                        incrementFailCount();
                     }
                  }
               }
            }
            catch(Exception e)
            {
               ObjectPoolTest.logger.error("CheckOutCheckInThread" + threadId + ": error (" + i + ")!", e);
               incrementFailCount();
            }
            finally
            {
               if( object != null ) pool.checkIn(object);
            }
         }
         
         if( i < N_REPETITIONS )
         {
            ObjectPoolTest.logger.error("CheckOutCheckInThread" + threadId + " interrupted before completion!");
            incrementFailCount();
         }
      }
   }
}
