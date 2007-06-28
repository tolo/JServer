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
package com.teletalk.jserver.load;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.property.NumberProperty;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.property.VectorProperty;

/**
 * Handles the monitoring of load in a JServer. The current implementation uses an integer load value ({@link LoadValue}) for each thread to represent 
 * the server load. The sum of the thread load value is calculated periodically and exposed through a property.<br>
 * <br>
 * The {@link com.teletalk.jserver.tcp.messaging.MessagingManager} uses this class to report load to other messaging systems for load balancing 
 * purposes.  
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0 Build 757
 */
public class LoadManager extends SubSystem
{
   private static LoadManager singletonLoadManager = null;  // ONLY used in standalone mode, i.e. no JServer main system
   
   /**
    * Registeres the current thread as a load thread.
    */
   public static boolean registerLoadThread()
   {
      LoadManager loadManager;
      synchronized(LoadManager.class)
      {
         loadManager = LoadManager.singletonLoadManager;
      }
      
      if( loadManager != null )
      {
	      Thread currentThread = Thread.currentThread();
	      LoadThreadContainer container = null;
	      LoadThreadContainer iterationContainer = null;
	      
	      synchronized(loadManager)
	      {
		      Object[] loadTheadContainers = loadManager.loadThreads.getItems();

		      for(int i=0; i<loadTheadContainers.length; i++)
		      {
		         iterationContainer = (LoadThreadContainer)loadTheadContainers[i];
		         if( (iterationContainer != null) && (currentThread.equals(iterationContainer.getThread())) )
		         {
		            container = iterationContainer;
		            break;
		         }
		      }
		      
		      if( container == null )
		      {
			      container = new LoadThreadContainer(currentThread, LoadValue.getThreadLoadValue(false));
			      
			      loadManager.loadThreads.add(container);
		      }
	      }
	      return true;
      }
      return false;
   }
   
   /**
    * Unregisteres the current thread as a load thread.
    */
   public static void unregisterLoadThread()
   {
      unregisterLoadThread(Thread.currentThread());
   }
   
   /**
    * Unregisteres the specified thread as a load thread.
    */
   public static void unregisterLoadThread(final Thread thread)
   {
      LoadManager loadManager;
      synchronized(LoadManager.class)
      {
         loadManager = LoadManager.singletonLoadManager;
      }
      
      if( loadManager != null )
      {
	      LoadThreadContainer container;
	      boolean updateLoad = false;
	      int loadDiff = 0;

	      synchronized(loadManager)
	      {
	         Object[] loadTheadContainers = loadManager.loadThreads.getItems();
	         
	         for(int i=0; i<loadTheadContainers.length; i++)
	         {
	            container = (LoadThreadContainer)loadTheadContainers[i];
	            if( (container != null) && (thread.equals(container.getThread())) )
	            {
	               loadManager.loadThreads.remove(container);
	               LoadValue loadValue = container.getLoadValue();
	               if( loadValue != null )
	               {
	                  loadDiff = -loadValue.getLoad();
	                  updateLoad = true;
	                  container.destroy();
	               }
	               break;
	            }
	         }
	      }
	      
	      if( updateLoad )
	      {
	         loadChanged(loadDiff);
	      }
      }
   }
   
   /**
    * Called when the load has changed in a load thread.
    */
   static void loadChanged(final int diff)
   {
      LoadManager loadManager;
      synchronized(LoadManager.class)
      {
         loadManager = LoadManager.singletonLoadManager;
      }
      
      if( loadManager != null )
      {
         loadManager.updateLoad(diff);
      }
   }
   
   /**
    * Singleton method for getting the LoadManager. This method will not create a new LoadManager if 
    * none exists.
    * 
    * @return the LoadManager or <code>null</code> if none has been created. 
    */
   public static LoadManager getLoadManager()
   {
      return getLoadManager(false, null);
   }
   
   /**
    * Singleton method for getting the LoadManager. This method will create a new LoadManager if 
    * none exists and the parameter <code>create</code> is <code>true</code>.
    * 
    * @param create indicating if a new LoadManager should be created if none exists.
    * 
    * @return the LoadManager or <code>null</code> if none has been created. 
    */
   public static LoadManager getLoadManager(boolean create)
   {
      return getLoadManager(create, null);
   }
   
   /**
    * Singleton method for getting the LoadManager. This method will create a new LoadManager if 
    * none exists and the parameter <code>create</code> is <code>true</code>.
    * 
    * @param create indicating if a new LoadManager should be created if none exists.
    * @param name the name of the new LoadManager, or null to use the default.
    * 
    * @return the LoadManager or <code>null</code> if none has been created. 
    */
   public static LoadManager getLoadManager(boolean create, final String name)
   {
      synchronized(LoadManager.class)
      {
         JServer jserver = JServer.getJServer();
         LoadManager loadManager = LoadManager.singletonLoadManager;
         if( jserver != null ) loadManager = jserver.getLoadManager();
         
         if( (loadManager == null) && create )
         {
            loadManager = new LoadManager(JServer.getJServer(), name);
            
            if( jserver != null ) jserver.setLoadManager(loadManager);
            else
            {
               LoadManager.singletonLoadManager = loadManager;
               LoadManager.singletonLoadManager.engage();
            }
         }
         
         return loadManager;
      }
   }
     
   
   private final VectorProperty loadThreads;
   
   private int currentLoad = 0;
   
   private final NumberProperty loadProperty;
   
   private final NumberProperty loadUpdateInterval;
   
   private final NumberProperty threadCheckInterval;
   
   /**
    * Creates a new LoadManager and sets it as a singleton (if one doesn't exist already). 
    * Consider using {@link #getLoadManager()}, {@link #getLoadManager(boolean)} or 
    * {@link #getLoadManager(boolean, String)} instead of creating a LoadManager directly.
    */
   public LoadManager(SubSystem parent)
   {
      this(parent, "LoadManager");
   }
   
   /**
    * Creates a new LoadManager and sets it as a singleton (if one doesn't exist already). 
    * Consider using {@link #getLoadManager()}, {@link #getLoadManager(boolean)} or 
    * {@link #getLoadManager(boolean, String)} instead of creating a LoadManager directly.
    */
   public LoadManager(SubSystem parent, String name)
   {
      super(parent, (name != null) ? name : "LoadManager");
            
      this.loadThreads = new VectorProperty(this, "load threads");
      super.addProperty(this.loadThreads);
      
      this.loadProperty = new NumberProperty(this, "load", 0, NumberProperty.NOT_MODIFIABLE, false);
      this.loadProperty.setDescription("The total load in all threads.");
      super.addProperty(this.loadProperty);
      
      this.loadUpdateInterval = new NumberProperty(this, "loadUpdateInterval", 500, NumberProperty.MODIFIABLE_NO_RESTART);
      this.loadUpdateInterval.setDescription("The interval in milliseconds at which the property indicating load will be updated.");
      super.addProperty(this.loadUpdateInterval);
      
      this.threadCheckInterval = new NumberProperty(this, "threadCheckInterval", 30*1000, NumberProperty.MODIFIABLE_NO_RESTART);
      this.threadCheckInterval.setDescription("The interval in milliseconds at which the LoadManager will check for dead threads and reevaluate load.");
      super.addProperty(this.threadCheckInterval);
   }
   
   /**
    * Updates the total load.
    */
   private void updateLoad(int diff)
   {
      synchronized(this)
      {
         this.currentLoad += diff;
      }
   }
   
   /**
    * Sets the total load.
    */
   private void setLoad(int newLoad)
   {
      synchronized(this)
      {
         this.currentLoad = newLoad;
      }
   }
   
   /**
    * Gets the total load.
    */
   public int getLoad()
   {
      synchronized(this)
      {
         return this.currentLoad;
      }
   }
   
	/**
	 * Validates a modification of a property's value. 
	 * 
	 * @param property The property to be validated.
	 * 
	 * @return boolean value indicating if the property passed (true) validation or not (false).
	 * 
	 * @see com.teletalk.jserver.property.PropertyOwner
	 */
	public boolean validatePropertyModification(final Property property)
	{
		if(property == this.loadUpdateInterval) return this.loadUpdateInterval.intValue() >= 100;
		else if(property == this.threadCheckInterval) return this.threadCheckInterval.intValue() >= 1000;
		else return super.validatePropertyModification(property);
	}
   
   /**
    * The thread method of this LoadManager.
    */
   public void run()
   {
      long sleepTime;
      long nextLoadUpdate = System.currentTimeMillis() + this.loadUpdateInterval.longValue();
      long nextThreadCheck = System.currentTimeMillis() + this.threadCheckInterval.longValue();
      boolean performLoadUpdate = false; 
      boolean performThreadCheck = false;
      
      while(super.canRun)
      {
         try
         {
            sleepTime = Math.min(nextLoadUpdate, nextThreadCheck) - System.currentTimeMillis();

            if( sleepTime > 0 ) Thread.sleep(sleepTime);
            else Thread.yield();
            
            if( System.currentTimeMillis() >= nextLoadUpdate )
            {
               performLoadUpdate = true;
               nextLoadUpdate = System.currentTimeMillis() + this.loadUpdateInterval.longValue();
            }
            else performLoadUpdate = false;
            
            if( System.currentTimeMillis() >= nextThreadCheck )
            {
               performThreadCheck = true;
               nextThreadCheck = System.currentTimeMillis() + this.threadCheckInterval.longValue();
            }
            else performThreadCheck = false;
            
            /*if( super.isDebugMode() ) 
            {
               if( performLoadUpdate && performThreadCheck ) super.logDebug("Performing load update and thread check.");
               else if( performLoadUpdate ) super.logDebug("Performing load update.");
               else if( performThreadCheck ) super.logDebug("Performing thread check.");
            }*/
            
            if( performThreadCheck )
            {
               int newLoad = 0;
               //synchronized(this.loadThreads)
               synchronized(this)
      	      {
      		      Object[] loadTheadContainers = this.loadThreads.getItems();
      		      LoadThreadContainer iterationContainer = null;
      		      
      		      for(int i=0; i<loadTheadContainers.length; i++)
      		      {
      		         iterationContainer = (LoadThreadContainer)loadTheadContainers[i];
      		         if( iterationContainer != null )
      		         {
      		            if( (iterationContainer.getThread() != null) && (iterationContainer.getThread().isAlive()) ) 
      		            {
      		               newLoad += iterationContainer.getLoad();
      		            }
      		            else // Thread dead
      		            {
      		               this.loadThreads.remove(iterationContainer);
      		               iterationContainer.destroy();
      		            }
      		         }
      		      }
      		      
      		      this.setLoad(newLoad);
      	      }
            }
            
            if( performThreadCheck || performLoadUpdate )
            {
               this.loadProperty.setValue(this.getLoad());
            }
         }
         catch(InterruptedException ie)
         {
            continue;
         }
         catch(Exception e)
         {
            logError("Error occured in LoadManager thread!", e);
         }
      }
   }
}
