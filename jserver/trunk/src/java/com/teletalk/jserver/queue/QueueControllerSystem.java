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
package com.teletalk.jserver.queue;

import com.teletalk.jserver.StatusTransitionException;
import com.teletalk.jserver.SubSystem;

/**
 * Base class for queue controller systems.
 * 
 * @since 2.1.5 (20070329)
 */
public abstract class QueueControllerSystem extends SubSystem implements QueueController
{
   /** Reference to the associated QueueManager object. */
   protected QueueManager queueManager;

   /**
    * Flag indicating if this queue controller system should engage the QueueManager when reinitializing itself. This
    * flag is used in the <code>doIntialize()</code> method of this queue controller system. The default value of this
    * flag is <code><strong>false</strong></code>.
    */
   protected boolean engageQueueManagerIfReinitializing = false;

   /**
    * Flag indicating if this queue controller system should shut down the QueueManager when reinitializing itself. This
    * flag is used in the <code>doShutDown()</code> method of this queue controller system. The default value of this
    * flag is <code><strong>false</strong></code>.
    */
   protected boolean shutDownQueueManagerIfReinitializing = false;
   
   /**
    * Flag indicating if this queue controller system should engage the QueueManager when in {@link #doInitialize()}. The default value of this
    * flag is <code><strong>true</strong></code>.
    */
   protected boolean engageQueueManager = true;

   /**
    * Flag indicating if this queue controller system should shut down the QueueManager when in {@link #doShutDown()}. The default value of this
    * flag is <code><strong>true</strong></code>.
    */
   protected boolean shutDownQueueManager = true;

   /**
    * Creates an QueueControllerSystem.
    * 
    * @param parent the parent of this InOutQueueControllerSystem.
    * @param name the name of this InOutQueueControllerSystem.
    */
   protected QueueControllerSystem(SubSystem parent, String name)
   {
      super(parent, name);

      queueManager = new QueueManager(this, this);
      
      addSubSystem(queueManager);
   }

   /**
    * Initialization method for this queue controller system. Engages the QueueManager if this SubSystem is not
    * currently reinitializing (depending on the value of the flag {@link #engageQueueManagerIfReinitializing}. This
    * method will wait a maximum of 40 seconds for the QueueManager to engage before engaging this SubSystem.
    */
   protected void doInitialize()
   {
      if (this.isEngageQueueManager() && (this.isEngageQueueManagerIfReinitializing() || !super.isReinitializing()) )
      {
         queueManager.engage();
         try
         {
            queueManager.waitForEnabled(40000);
         }
         catch (InterruptedException e)
         {
            throw new StatusTransitionException("Interrupted while waiting for QueueManager to engage", e);
         }
      }

      super.doInitialize();
   }

   /**
    * Shut down method for this queue controller system. Shuts down the QueueManager if this SubSystem is not currently
    * reinitializing. (depending on the value of the flag {@link #engageQueueManagerIfReinitializing}. This method will
    * wait a maximum of 40 seconds for the QueueManager to shut down before shutting down this SubSystem.
    */
   protected void doShutDown()
   {
      if (this.isShutDownQueueManager() && (this.isShutDownQueueManagerIfReinitializing() || !super.isReinitializing()) )
      {
         queueManager.shutDown();
         try
         {
            queueManager.waitForDown(40000);
         }
         catch (InterruptedException e)
         {
            throw new StatusTransitionException("Interrupted while waiting for QueueManager to shut down", e);
         }
      }

      super.doShutDown();
   }
   
   /**
    * Blocks the calling thread until the QueueManager becomes engaged. This method will not break on error in the
    * QueueManager (ERROR or CRITICAL_ERROR).
    * 
    * @return true if the specified status was reached, otherwise false.
    */
   public boolean waitForQueueManagerToEngage() throws InterruptedException
   {
      while (queueManager.getStatus() != ENABLED)
      {
         queueManager.waitForEnabled(1000);
      }

      return queueManager.getStatus() == ENABLED;
   }

   /**
    * Blocks the calling thread until the QueueManager becomes engaged.
    * 
    * @param breakOnError flag indicating if this method should return if the QueueManager goes into ERROR or
    *           CRITICAL_ERROR.
    * @return true if the specified status was reached, otherwise false.
    */
   public boolean waitForQueueManagerToEngage(boolean breakOnError) throws InterruptedException
   {
      while (queueManager.getStatus() != ENABLED)
      {
         queueManager.waitForEnabled(1000, breakOnError);
      }

      return queueManager.getStatus() == ENABLED;
   }

   /**
    * Blocks the calling thread until the QueueManager becomes engaged or the specified maxTime has ellapsed. This
    * method will not break on error in the QueueManager (ERROR or CRITICAL_ERROR).
    * 
    * @param maxWait the maximum wait time in milliseconds.
    * @return true if the specified status was reached, otherwise false.
    */
   public boolean waitForQueueManagerToEngage(long maxWait) throws InterruptedException
   {
      long waitStart = System.currentTimeMillis();
      long waitTime;

      while (queueManager.getStatus() != ENABLED)
      {
         waitTime = maxWait - (System.currentTimeMillis() - waitStart);

         if (waitTime > 0) queueManager.waitForEnabled(waitTime);
         else break;
      }

      return queueManager.getStatus() == ENABLED;
   }

   /**
    * Blocks the calling thread until the QueueManager becomes engaged or the specified maxTime has ellapsed.
    * 
    * @param maxWait the maximum wait time in milliseconds.
    * @param breakOnError flag indicating if this method should return if the QueueManager goes into ERROR or
    *           CRITICAL_ERROR.
    * @return true if the specified status was reached, otherwise false.
    */
   public boolean waitForQueueManagerToEngage(long maxWait, boolean breakOnError) throws InterruptedException
   {
      long waitStart = System.currentTimeMillis();
      long waitTime;

      while ((queueManager.getStatus() != ENABLED) && (!breakOnError || (breakOnError && (queueManager.getStatus() != ERROR) && (queueManager.getStatus() != CRITICAL_ERROR))))
      {
         waitTime = maxWait - (System.currentTimeMillis() - waitStart);

         if (waitTime > 0) queueManager.waitForEnabled(waitTime, breakOnError);
         else break;
      }

      return queueManager.getStatus() == ENABLED;
   }

   public boolean isEngageQueueManager()
   {
      return engageQueueManager;
   }

   public void setEngageQueueManager(boolean engageQueueManager)
   {
      this.engageQueueManager = engageQueueManager;
   }

   public boolean isEngageQueueManagerIfReinitializing()
   {
      return engageQueueManagerIfReinitializing;
   }

   public void setEngageQueueManagerIfReinitializing(boolean engageQueueManagerIfReinitializing)
   {
      this.engageQueueManagerIfReinitializing = engageQueueManagerIfReinitializing;
   }

   public boolean isShutDownQueueManagerIfReinitializing()
   {
      return shutDownQueueManagerIfReinitializing;
   }

   public void setShutDownQueueManagerIfReinitializing(boolean shutDownQueueManagerIfReinitializing)
   {
      this.shutDownQueueManagerIfReinitializing = shutDownQueueManagerIfReinitializing;
   }

   public boolean isShutDownQueueManager()
   {
      return shutDownQueueManager;
   }

   public void setShutDownQueueManager(boolean shutDownQueueManager)
   {
      this.shutDownQueueManager = shutDownQueueManager;
   }
}
