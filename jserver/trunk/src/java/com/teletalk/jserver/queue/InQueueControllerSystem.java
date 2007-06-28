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

import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.queue.command.QueueControllerCommand;

/**
 * An abstract SubSystem class that implements the InQueueController interface. This class creates a QueueManager object
 * that has only an in queue and specifies itself as the controller. The created QueueManager is then added as a
 * subsystem to this object.<BR>
 * <BR>
 * This class provides dummy implementations of all the methods in the InQueueController interface and subclasses need
 * only implement the ones that are actually needed for a specific purpose.
 * 
 * @see InQueueController
 * @see QueueManager
 * @author Tobias Löfstrand
 * @since Beta 1
 */
public abstract class InQueueControllerSystem extends QueueControllerSystem implements InQueueController
{
   /**
    * Creates an InQueueControllerSystem with a QueueManager equipped with an in-queue.
    * 
    * @param parent the parent of this InQueueControllerSystem.
    * @param name the name of this InQueueControllerSystem.
    */
   protected InQueueControllerSystem(SubSystem parent, String name)
   {
      super(parent, name);

      queueManager.createInQueue();
   }

   /**
    * Creates an InQueueControllerSystem with a QueueManager equipped with the specified in-queue. The QueueManager will
    * assume ownership of the specified queue, and for that reason it is recommended that it is created using a
    * constructor that doensn't take a parent as a parameter.
    * 
    * @param parent the parent of this InQueueControllerSystem.
    * @param name the name of this InQueueControllerSystem.
    * @param inQueue the in queue to be used in the QueueManager.
    * @param addDefaultExternalQueueOperations flag indicating if the default external operations (cancel and relocate)
    *           should be added to the in queue.
    */
   protected InQueueControllerSystem(SubSystem parent, String name, Queue inQueue, boolean addDefaultExternalQueueOperations)
   {
      super(parent, name);

      if( inQueue != null ) queueManager.setInQueue(inQueue, addDefaultExternalQueueOperations);
      else queueManager.createInQueue(addDefaultExternalQueueOperations);
   }


   public void recoveredItemsInInQueue()
   {
   }

   public void unableToDispatchCompletionResponse(QueueItem inItem)
   {
   }

   public void newInItem(QueueItem inItem)
   {
   }

   /**
    * Called when a request to cancel an item in the in-queue is received. This method is called before
    * {@link #inItemCancellationRequested(QueueItem)} to indicate if the specified QueueItem can be cancelled. If this
    * method returns <code>false</code>, <code>inItemCancellationRequested</code> will not be called and the
    * QueueItem will not be cancelled.<br>
    * <br>
    * This default implementation only returns <code>true</code>.
    * 
    * @return <code>true</code> if the item may be cancelled, otherwise <code>false</code>.
    */
   public boolean canCancelInItem(QueueItem inItem)
   {
      return true;
   }

   public Object inItemCancellationRequested(QueueItem inItem)
   {
      return null;
   }

   /**
    * Called when a request to relocate an item in the in-queue is received. This method is called before
    * {@link #inItemRelocationRequested(QueueItem)} to indicate if the specified QueueItem can be relocated. If this
    * method returns <code>false</code>, <code>inItemRelocationRequested</code> will not be called and the
    * QueueItem will not be relocated. <br>
    * This default implementation only returns <code>true</code>.
    * 
    * @return <code>true</code> if the item may be relocated, otherwise <code>false</code>.
    * @since 1.13 Build 603
    */
   public boolean canRelocateInItem(QueueItem inItem)
   {
      return true;
   }

   public Object inItemRelocationRequested(QueueItem inItem)
   {
      return null;
   }

   public void inItemAborted(QueueItem inItem)
   {
   }

   public void queueControllerCommandReceived(QueueControllerCommand command)
   {
   }

   public void unableToDeliverControllerCommand(QueueControllerCommand command)
   {
   }

   public void customExternalQueueOperationCalled(String operationName, String[] ids)
   {
   }

   public void linkToRemoteQueueSystemLost(RemoteQueueSystem remoteQueueSystem)
   {
   }
}
