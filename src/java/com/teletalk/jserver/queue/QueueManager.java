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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.JServerUtilities;
import com.teletalk.jserver.StatusTransitionException;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.comm.EndPointIdentifier;
import com.teletalk.jserver.property.NumberProperty;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.queue.command.MultiQueueItemTransferRequest;
import com.teletalk.jserver.queue.command.MultiQueueItemTransferResponse;
import com.teletalk.jserver.queue.command.QueueControllerCommand;
import com.teletalk.jserver.queue.command.QueueItemCancellationRequest;
import com.teletalk.jserver.queue.command.QueueItemCompletionResponse;
import com.teletalk.jserver.queue.command.QueueItemQuery;
import com.teletalk.jserver.queue.command.QueueItemRelocationRequest;
import com.teletalk.jserver.queue.command.QueueItemResponse;
import com.teletalk.jserver.queue.command.QueueItemTransferRequest;
import com.teletalk.jserver.queue.command.QueueItemTransferResponse;
import com.teletalk.jserver.queue.command.QueueSystemCommand;
import com.teletalk.jserver.queue.command.QueueSystemSynchronizationRequest;
import com.teletalk.jserver.queue.command.QueueSystemSynchronizationResponse;
import com.teletalk.jserver.queue.legacy.DefaultQueueSystemCollaborationManager;
import com.teletalk.jserver.queue.legacy.DefaultQueueSystemEndPointProxy;

/**
 * The mainclass of the queue system architecture. This class functions as a façade to the rest of the queue system
 * classes and provides a number of convenience methods. It also performs various maintenance work on the queues, like
 * for instance monitoring the time passed since a certain QueueItem was put in the out queue.<br>
 * <br>
 * QueueManager uses the class DefaultQueueSystemCollaborationManager to handle communication with other queue systems.
 * If another communication mechanism is required it is possible to set the collaboration manager to be used through the
 * method {@link #setQueueCollaborationManager(QueueSystemCollaborationManager)}.<br>
 * <br>
 * The behaviour of the QueueManager can be modified by setting a number of flags. The flags can be set by the following
 * methods:<br>
 * <br>
 * <ul>
 * <li>{@link #setCheckOutQueueItemAge(boolean)}</li>
 * <li>{@link #setOutQueueItemAgeLimit(long)}</li>
 * <li>{@link #setInQueueMaxSize(int)}</li>
 * <li>{@link #setRemoveAbortedItems(boolean)}</li>
 * <li>{@link #setRemoveCompletedOutQueueItems(boolean)}</li>
 * </ul>
 * <br>
 * It is strongly recommended that these flags are set directly after creating a QueueManager object, preferably in the
 * constructor of the queue controller.
 * 
 * @see DefaultQueueSystemCollaborationManager
 * @author Tobias Löfstrand
 * @since Beta 1
 */
public final class QueueManager extends SubSystem implements QueueOwner
{

   /** The default name of the in queue component. */
   public final static String defaultInQueueName = "in-queue";

   /** The default name of the out queue component. */
   public final static String defaultOutQueueName = "out-queue";

   /**
    * Name of external operation for cancelling items in the in queue through the administration tool (through
    * VectorProperty "items").
    */
   public final static String inQueueCancelOperationName = "inQueueCancel";

   /**
    * Name of external operation for relocating items in the in queue through the administration tool (through
    * VectorProperty "items").
    */
   public final static String inQueueRelocateOperationName = "inQueueRelocate";

   /**
    * Name of external operation for forcing items in the in queue to status QUEUED through the administration tool
    * (through VectorProperty "items").
    */
   public final static String inQueueForceQueuedOperationName = "inQueueForceQueued";

   /**
    * Name of external operation for deleting items in the in queue through the administration tool (through
    * VectorProperty "items").
    */
   public final static String inQueueDeleteOperationName = "inQueueDelete";

   /**
    * Name of external operation for cancelling items in the out queue through the administration tool (through
    * VectorProperty "items").
    */
   public final static String outQueueCancelOperationName = "outQueueCancel";

   /**
    * Name of external operation for relocating items in the out queue through the administration tool (through
    * VectorProperty "items").
    */
   public final static String outQueueRelocateOperationName = "outQueueRelocate";

   /**
    * Name of external operation for forcing items in the out queue to be completed (success), through the
    * administration tool (through VectorProperty "items").
    */
   public final static String outQueueForceCompleteSuccessOperationName = "outQueueForceCompleteSuccess";

   /**
    * Name of external operation for forcing items in the out queue to be completed (failure), through the
    * administration tool (through VectorProperty "items").
    */
   public final static String outQueueForceCompleteFailureOperationName = "outQueueForceCompleteFailure";

   /**
    * Name of external operation for forcing items in the out queue to be completed (cancelled), through the
    * administration tool (through VectorProperty "items").
    */
   public final static String outQueueForceCompleteCancelOperationName = "outQueueForceCompleteCancel";

   /**
    * Name of external operation for forcing items in the out queue to be completed (relocation required), through the
    * administration tool (through VectorProperty "items").
    */
   public final static String outQueueForceRelocationOperationName = "outQueueForceRelocation";

   /**
    * Name of external operation for deleting items in the out queue through the administration tool (through
    * VectorProperty "items").
    */
   public final static String outQueueDeleteOperationName = "outQueueDelete";

   private final QueueController controller;

   private QueueManagerImpl impl;

   QueueSystemCollaborationManager collaborationManager;

   Queue inQueue;

   Queue outQueue;

   private long uniqueIdCounter;

   private final Object uniqueIdCounterMonitor = new Object(); // Synchronizing object used when incrementing id counter

   // Only for optimization of unique id generation
   private final StringBuffer JServerName;

   private final int JServerNameAppendIndex;

   
   /* ### PROPERTIES ### */
   
   // The iterval at which checks are made by the thread of this SubSystem
   NumberProperty checkInterval;

   // The max time a queue item can be in the state DISPATCHED before the controller is warned
   NumberProperty outItemWarningAgeLimit; 

   // The maximum number of items that the in queue can hold
   NumberProperty inQueueMaxSize;

   // The max time an unsent response can stay in the unsentCompletionResponses map before it's removed
   NumberProperty unsentResponseMaxAge;
   
   NumberProperty recentCompletionResponseCacheSize;
   

   // Map that holds all completion response that failed to be returned to the sender of the QueueItem
   private final HashMap unsentCompletionResponses; 

   private final LinkedList recentCompletionResponseCache;

   private final ArrayList trustedThreads;

   private final QueueSystemMetaData metaData;
   

   /* ### BEHAVIOUR FLAGS ### */
   
   boolean inOutQueueAutoModeEnabled = true;

   boolean removeCompletedOutQueueItems = true;

   boolean removeAbortedItems = true;

   boolean checkOutQueueItemAge = true;

   private final Object queueItemsLock = this;
      

   /**
    * Private constructor.
    * 
    * @param parent the parent of this QueueManager.
    */
   QueueManager(final SubSystem parent, final QueueController controller)
   {
      super(parent, "QueueManager");

      this.controller = controller;

      if (this.controller instanceof InOutQueueController) this.impl = new QueueManagerInOutQueueImpl(this, (InOutQueueController) this.controller);
      else if (this.controller instanceof InQueueController) this.impl = new QueueManagerInQueueImpl(this, (InQueueController) this.controller);
      else this.impl = new QueueManagerOutQueueImpl(this, (OutQueueController) this.controller);

      metaData = new QueueSystemMetaData();

      this.unsentCompletionResponses = new HashMap();
      this.recentCompletionResponseCache = new LinkedList();

      this.trustedThreads = new ArrayList();

      JServer jServer = JServer.getJServer();
      if (jServer != null) JServerName = new StringBuffer(jServer.getName());
      else JServerName = new StringBuffer("JServer");
      JServerNameAppendIndex = JServerName.length();

      checkInterval = new NumberProperty(this, "checkInterval", 10 * 1000, NumberProperty.MODIFIABLE_NO_RESTART);
      checkInterval.setDescription("Interval in milliseconds at which checks a made.");
      super.addProperty(checkInterval);
      
      if (this.controller instanceof OutQueueController)
      {
         outItemWarningAgeLimit = new NumberProperty(this, "outitemWarningAge", 10 * 60 * 1000, NumberProperty.MODIFIABLE_NO_RESTART);
         outItemWarningAgeLimit.setDescription("The maximum age in milliseconds an item in the out queue can have before a warning is sent to the queue controller.");
         super.addProperty(outItemWarningAgeLimit);
      }
      if (this.controller instanceof InQueueController)
      {
         inQueueMaxSize = new NumberProperty(this, "maxInQueueSize", -1, NumberProperty.MODIFIABLE_NO_RESTART);
         inQueueMaxSize.setDescription("The maximum size of the in queue, -1 if the in queue has no maximum size.");
         super.addProperty(inQueueMaxSize);
         unsentResponseMaxAge = new NumberProperty(this, "unsentResponseMaxAge", 12, NumberProperty.MODIFIABLE_NO_RESTART);
         unsentResponseMaxAge.setDescription("The maximum time (in hours) to keep an unsent response.");
         super.addProperty(unsentResponseMaxAge);
         recentCompletionResponseCacheSize = new NumberProperty(this, "recentCompletionResponseCacheSize", 100, NumberProperty.MODIFIABLE_NO_RESTART);
         recentCompletionResponseCacheSize.setDescription("The maximum number of completion response to cache. This cache is used to minimize the risk of lost completion responses.");
         super.addProperty(recentCompletionResponseCacheSize);
      }
   }

   /**
    * Creates a QueueManager with an in queue. The default implementation of a QueueSystemCollaboraionManager
    * (DefaultQueueSystemCollaborationManager) will be used.
    * 
    * @param parent the parent of this QueueManager.
    * @param inController the InQueueController associated with this QueueManager.
    */
   public QueueManager(final SubSystem parent, final InQueueController inController)
   {
      this(parent, inController, null, true);
   }

   /**
    * Creates a QueueManager with the specified in-queue. The default implementation of a QueueSystemCollaboraionManager
    * (DefaultQueueSystemCollaborationManager) will be used. Finally, this queueManager will assume ownership of the
    * specified queue.
    * 
    * @param parent the parent of this QueueManager.
    * @param inController the InQueueController associated with this QueueManager.
    * @param inQueue the in queue to be used in this QueueManager.
    * @param addDefaultExternalQueueOperations flag indicating if the default external operations (cancel and relocate)
    *           should be added to the in queue.
    */
   public QueueManager(final SubSystem parent, final InQueueController inController, Queue inQueue, final boolean addDefaultExternalQueueOperations)
   {
      this(parent, (QueueController) inController);

      this.setInQueue((inQueue == null) ? new Queue(this, QueueManager.defaultInQueueName) : inQueue, addDefaultExternalQueueOperations);
   }

   /**
    * Creates a QueueManager with an out queue. The default implementation of a QueueSystemCollaboraionManager
    * (DefaultQueueSystemCollaborationManager) will be used.
    * 
    * @param parent the parent of this QueueManager.
    * @param outController the OutQueueController associated with this QueueManager.
    */
   public QueueManager(final SubSystem parent, final OutQueueController outController)
   {
      this(parent, outController, null, true);
   }

   /**
    * Creates a QueueManager with the specified out queue. The default implementation of a
    * QueueSystemCollaboraionManager (DefaultQueueSystemCollaborationManager) will be used. Finally, this queueManager
    * will assume ownership of the specified queue.
    * 
    * @param parent the parent of this QueueManager.
    * @param outController the OutQueueController associated with this QueueManager.
    * @param outQueue the out queue to be used in this QueueManager.
    * @param addDefaultExternalQueueOperations flag indicating if the default external operation (cancel) should be
    *           added to the out queue.
    */
   public QueueManager(final SubSystem parent, final OutQueueController outController, Queue outQueue, final boolean addDefaultExternalQueueOperations)
   {
      this(parent, (QueueController) outController);

      this.setOutQueue((outQueue == null) ? new Queue(this, QueueManager.defaultOutQueueName) : outQueue, addDefaultExternalQueueOperations);
   }

   /**
    * Creates a QueueManager with an in-queue and an out-queue. The default implementation of a
    * QueueSystemCollaboraionManager (DefaultQueueSystemCollaborationManager) will be used.
    * 
    * @param parent the parent of this QueueManager.
    * @param inOutController the InOutQueueController associated with this QueueManager.
    * @param inOutQueueAutoModeEnabled flag indicating if automatic parent/child handling should be used. If this flag
    *           is set to<code>true</code> the QueueManager will keep track of child items in the out queue belonging
    *           to an item in the in queue, and make sure that a respons is sent when all children are completed.
    */
   public QueueManager(final SubSystem parent, final InOutQueueController inOutController, final boolean inOutQueueAutoModeEnabled)
   {
      this(parent, inOutController, null, null, true, inOutQueueAutoModeEnabled);
   }

   /**
    * Creates a QueueManager with the specified out queue. The default implementation of a
    * QueueSystemCollaboraionManager (DefaultQueueSystemCollaborationManager) will be used. Finally, this queueManager
    * will assume ownership of the specified queues.
    * 
    * @param parent the parent of this QueueManager.
    * @param inOutController the OutQueueController associated with this QueueManager.
    * @param inQueue the in queue to be used in this QueueManager.
    * @param outQueue the out queue to be used in this QueueManager.
    * @param addDefaultExternalQueueOperations flag indicating if the default external operations (cancel and relocate)
    *           should be added to the queues.
    * @param inOutQueueAutoModeEnabled flag indicating if automatic parent/child handling should be used. If this flag
    *           is set to<code>true</code> the QueueManager will keep track of child items in the out queue belonging
    *           to an item in the in queue, and make sure that a respons is sent when all children are completed.
    */
   public QueueManager(final SubSystem parent, final InOutQueueController inOutController, Queue inQueue, Queue outQueue, final boolean addDefaultExternalQueueOperations,
         final boolean inOutQueueAutoModeEnabled)
   {
      this(parent, (QueueController) inOutController);

      this.setInQueue((inQueue == null) ? new Queue(this, QueueManager.defaultInQueueName) : inQueue, addDefaultExternalQueueOperations);

      this.setOutQueue((outQueue == null) ? new Queue(this, QueueManager.defaultOutQueueName) : outQueue, addDefaultExternalQueueOperations);

      this.inOutQueueAutoModeEnabled = inOutQueueAutoModeEnabled;
   }
   
   /**
    * Gets the current QueueManagerImpl, i.e the object imlementing the mode specific logic of the queue manager.
    * 
    * @since 2.1.2 (20060224)
    */
   public QueueManagerImpl getQueueManagerImpl()
   {
      return impl;
   }

   /**
    * Sets the current QueueManagerImpl, i.e the object imlementing the mode specific logic of the queue manager.
    * 
    * @since 2.1.2 (20060224)
    */
   public void setQueueManagerImpl(QueueManagerImpl impl)
   {
      this.impl = impl;
   }

   /**
    * Initialization method for this QueueManager. This method initializes the queues, restores them from persistent
    * storage and engages the QueueSystemCollaborationManager (if the QueueManager isn't reinitializing). The
    * QueueManager will wait for the QueueSystemCollaborationManager to be enabled before completing initialization.
    */
   protected void doInitialize()
   {
      try
      {
         super.initProperties();

         // Attempt to get old property "check interval"
         super.initFromConfiguredProperty(this.checkInterval, "check interval", false, true);
         // Attempt to get old property "outitem warning age(ms)" to be used if "outitem warning age" property is
         // missing
         super.initFromConfiguredProperty(this.outItemWarningAgeLimit, "outitem warning age(ms)", false, true);
         // Attempt to get old property "max in queue size"
         super.initFromConfiguredProperty(this.inQueueMaxSize, "max in queue size", false, true);
         // Attempt to get old property "unsent response max age" to be used if "unsent response max age" property is
         // missing
         super.initFromConfiguredProperty(this.unsentResponseMaxAge, "unsent response max age(hrs)", false, true);

         if( !super.isReinitializing() )
         {
            this.trustCurrentThread();
   
            uniqueIdCounter = System.currentTimeMillis() + 1000;
   
            if (super.statusTransitionTimeout < 30 * 60 * 1000)
            {
               super.statusTransitionTimeout = 30 * 60 * 1000; // Set status transition timeout to 30 minutes while
               // restoring queues
            }
   
            if ( inQueue != null )
            {
               // Enable in queue
               if ( !inQueue.isEnabled() && !inQueue.engage() ) throw new StatusTransitionException("Failed to engage in queue!");
            }
   
            if ( outQueue != null )
            {
               // Enable out queue
               if ( !outQueue.isEnabled() && !outQueue.engage() ) throw new StatusTransitionException("Failed to engage out queue!");
   
               if (outQueue.size() > 0)
               {
                  // Increment unique id counter
                  uniqueIdCounter += outQueue.size();
               }
            }
   
            ArrayList remoteQueueSystemsToSynchronizeWith = new ArrayList();
            ArrayList responsesToExecute = new ArrayList();
   
            // Check the in queue
            if (inQueue != null)
            {
               if (inQueue.size() > 0)
               {
                  remoteQueueSystemsToSynchronizeWith.addAll(impl.performInQueueCheck());
               }
            }
   
            // Check the out queue
            if (outQueue != null)
            {
               if (outQueue.size() > 0)
               {
                  ArrayList availableReceivers = impl.performOutQueueCheck(responsesToExecute);
   
                  for (int i = 0; i < availableReceivers.size(); i++)
                  {
                     Object o = availableReceivers.get(i);
                     if (o != null)
                     {
                        if (!remoteQueueSystemsToSynchronizeWith.contains(o)) remoteQueueSystemsToSynchronizeWith.add(o);
                     }
                  }
               }
            }
            
            // If no CollaborationManager is set - create the default one
            if( this.collaborationManager == null )
            {
               this.collaborationManager = new DefaultQueueSystemCollaborationManager(this);
               addSubSystem((DefaultQueueSystemCollaborationManager) collaborationManager);
            }
   
            if (collaborationManager instanceof SubComponent)
            {
               SubComponent collaborationManagerComponent = (SubComponent)collaborationManager;
   
               if ((collaborationManagerComponent.getStatus() != ENABLED) && (collaborationManagerComponent.getStatus() != INITIALIZING))
               {
                  // Attempt to engage the collaborationmanager
                  collaborationManagerComponent.engage();
                  try
                  {
                     collaborationManagerComponent.waitForEnabled(30000);
                  }
                  catch (InterruptedException e)
                  {
                  }
   
                  if (collaborationManagerComponent.getStatus() != ENABLED) throw new StatusTransitionException("Unable to start collaborationmanager!");
               }
            }
   
            // Synchronize queues with remote queue systems
            this.collaborationManager.performStartupSynchronization(remoteQueueSystemsToSynchronizeWith);
            
            // Execute any completion responses that were created during out queue check
            QueueItemResponse response;
   
            for (int i = 0; i < responsesToExecute.size(); i++)
            {
               try
               {
                  response = (QueueItemResponse) responsesToExecute.get(i);
                  if (response != null)
                  {
                     try
                     {
                        response.execute(this);
                     }
                     catch (Exception e)
                     {
                        logError("Error occurred while executing response during start up (" + response.toString() + ").", e);
                     }
                  }
               }
               catch (Exception e)
               {
                  logError("Error occurred while executing response during initialization.", e);
               }
            }
   
            // Notify controller
            if (inQueue != null)
            {
               if (inQueue.size() > 0)
               {
                  impl.recoveredItemsInInQueue();
               }
            }
   
            // Notify controller
            if (outQueue != null)
            {
               if (outQueue.size() > 0)
               {
                  impl.recoveredItemsInOutQueue();
               }
            }
         }

         super.doInitialize();
      }
      finally
      {
         this.distrustCurrentThread();
      }
   }

   /**
    * Shutdown method for this QueueManager. This method disables the queues and shuts down the
    * QueueSystemCollaborationManager (if the QueueManager isn't reinitializing).
    */
   protected void doShutDown()
   {
      if( !super.isReinitializing() )
      {
         if (collaborationManager instanceof SubComponent)
         {
            SubComponent collaborationManagerComponent = (SubComponent)collaborationManager;
            
            if ( (collaborationManagerComponent.getStatus() != DOWN) && (collaborationManagerComponent.getStatus() != SHUTTING_DOWN)
                  && (collaborationManagerComponent.getStatus() != CRITICAL_ERROR) )
            {
               collaborationManagerComponent.shutDown();
               
               try
               {
                  collaborationManagerComponent.waitForDown(30000);
               }
               catch (InterruptedException e){}
            }
         }
   
         if (inQueue != null) inQueue.shutDown();
   
         if (outQueue != null) outQueue.shutDown();
      }

      super.doShutDown();
   }

   /**
    * Validates a modification of a property's value.
    * 
    * @param property The property to be validated.
    * @return boolean value indicating if the property passed (true) validation or not (false).
    * @see com.teletalk.jserver.property.PropertyOwner
    */
   public boolean validatePropertyModification(Property property)
   {
      if (property == checkInterval) return checkInterval.longValue() > 1000;
      else if (property == outItemWarningAgeLimit) return outItemWarningAgeLimit.longValue() > 1000;
      else if (property == inQueueMaxSize) return inQueueMaxSize.intValue() >= -1;
      else if (property == unsentResponseMaxAge) return (unsentResponseMaxAge.intValue() > 0);
      else return super.validatePropertyModification(property);
   }

   /**
    * Gets the object that is used for synchronization of thread access to queue items and queues.
    * 
    * @since 2.1.2 (20060207)
    */
   public Object getQueueItemsLock()
   {
      return queueItemsLock;
   }

   /**
    * Called to see if the status was ok when calling a certain method. This is only used for facade (user) methods in this object. 
    */
   private void checkAccess()
   {
      if (getStatus() != ENABLED) // If the QueueManager is not ENABLED...
      {
         if (!isCurrentThreadTrusted()) // ...and the current thread is not trusted...
         {
            String msgStr = "Method called in illegal state (" + getStatusName() + ")!" + JServerUtilities.getStackTrace();
            logWarning(msgStr);
            throw new RuntimeException(msgStr);
         }
      }
   }

   /**
    */
   void addTrustedThread(Thread t)
   {
      synchronized (this.trustedThreads)
      {
         if (!trustedThreads.contains(t)) trustedThreads.add(t);
      }
   }

   /**
    */
   void trustCurrentThread()
   {
      addTrustedThread(Thread.currentThread());
   }

   /**
    */
   void removeTrustedThread(Thread t)
   {
      synchronized (this.trustedThreads)
      {
         trustedThreads.remove(t);
      }
   }

   /**
    */
   void distrustCurrentThread()
   {
      removeTrustedThread(Thread.currentThread());
   }

   /**
    */
   boolean isTrustedThread(Thread t)
   {
      synchronized (this.trustedThreads)
      {
         return trustedThreads.contains(t);
      }
   }

   /**
    */
   boolean isCurrentThreadTrusted()
   {
      return isTrustedThread(Thread.currentThread());
   }

   /**
    */
   HashMap getUnsentCompletionResponses()
   {
      synchronized (this.unsentCompletionResponses)
      {
         return (HashMap) unsentCompletionResponses.clone();
      }
   }
   
   /**
    */
   List getUnsentCompletionResponsesForAddress(final EndPointIdentifier address)
   {
      ArrayList completionResponsesForAddress = new ArrayList();
      QueueItemCompletionResponse response;
      String id;
      
      synchronized (this.unsentCompletionResponses)
      {
         Iterator it = this.unsentCompletionResponses.keySet().iterator();
         while(it.hasNext())
         {
            id = (String)it.next();
            response = (QueueItemCompletionResponse)this.unsentCompletionResponses.get(id);
            if( (response != null) && address.equals(response.getAddress()) )
            {
               completionResponsesForAddress.add(response);
               it.remove();
            }
         }
      }
      
      return completionResponsesForAddress;
   }

   /**
    */
   void addUnsentCompletionResponse(final QueueItemCompletionResponse response)
   {
      synchronized (this.unsentCompletionResponses)
      {
         unsentCompletionResponses.put(response.getItemId(), response);
      }
   }

   /**
    */
   QueueItemCompletionResponse getUnsentCompletionResponse(final String itemId)
   {
      synchronized (this.unsentCompletionResponses)
      {
         return (QueueItemCompletionResponse)unsentCompletionResponses.remove(itemId);
      }
   }
   
   /**
    */
   void addRecentCompletionResponse(final QueueSystemCommand response)
   {
      final int cacheSize = this.recentCompletionResponseCacheSize.intValue();
      
      synchronized(this.recentCompletionResponseCache)
      {
         if( cacheSize > 0)
         {
            this.recentCompletionResponseCache.addLast(response);
         
            while( this.recentCompletionResponseCache.size() > cacheSize ) this.recentCompletionResponseCache.removeFirst();
         }
         else if( this.recentCompletionResponseCache.size() > 0 )
         {
            this.recentCompletionResponseCache.clear();
         }
      }
   }
   
   /**
    */
   QueueItemCompletionResponse getRecentCompletionResponse(final String itemId)
   {
      synchronized (this.recentCompletionResponseCache)
      {
         Iterator it = this.recentCompletionResponseCache.iterator();
         QueueItemCompletionResponse response;
         
         while(it.hasNext())
         {
            response = (QueueItemCompletionResponse)it.next();
            if( (response != null) && itemId.equals(response.getItemId()) )
            {
               it.remove();
               return response;
            }
         }
      }
      
      return null;
   }
   
   /**
    */
   List getRecentCompletionResponsesForAddress(final EndPointIdentifier address)
   {
      ArrayList completionResponsesForAddress = new ArrayList();
      QueueItemCompletionResponse response;
      
      synchronized (this.recentCompletionResponseCache)
      {
         Iterator it = this.recentCompletionResponseCache.iterator();
         while(it.hasNext())
         {
            response = (QueueItemCompletionResponse)it.next();
            if( (response != null) && address.equals(response.getAddress()) )
            {
               completionResponsesForAddress.add(response);
               it.remove();
            }
         }
      }
      
      return completionResponsesForAddress;
   }
   
   /**
    * Gets the contents of the cache for recent completion responses.
    * 
    * @since 2.1.2 (20060313)
    */
   public List getRecentCompletionResponses()
   {
      synchronized(this.recentCompletionResponseCache)
      {
         return new ArrayList(this.recentCompletionResponseCache);
      }
   }

   /**
    * This method is called when a external operation is called in one of the queues (from for instance the
    * administration tool).
    * 
    * @param operationName the name of the operation that was called.
    * @param ids indices of items in a queue for which the operation was called.
    */
   public void externalQueueOperationCalled(final String operationName, final String[] ids)
   {
      if (operationName.equals(inQueueCancelOperationName))
      {
         for (int i = 0; i < ids.length; i++)
         {
            if (isDebugMode()) logDebug("External operation called (in queue cancel) for item " + ids[i] + ".");
            QueueItem item = inQueue.get(ids[i]);
            if (item != null) cancelInItem(item);
         }
      }
      else if (operationName.equals(inQueueRelocateOperationName))
      {
         for (int i = 0; i < ids.length; i++)
         {
            if (isDebugMode()) logDebug("External operation called (in queue relocate) for item " + ids[i] + ".");
            QueueItem item = inQueue.get(ids[i]);
            if (item != null) relocateInItem(item);
         }
      }
      else if (operationName.equals(inQueueForceQueuedOperationName))
      {
         for (int i = 0; i < ids.length; i++)
         {
            if (isDebugMode()) logDebug("External operation called (in queue force queued) for item " + ids[i] + ".");
            QueueItem item = inQueue.get(ids[i]);
            if (item != null) item.setStatus(QueueItem.QUEUED);
         }
      }
      else if (operationName.equals(inQueueDeleteOperationName))
      {
         for (int i = 0; i < ids.length; i++)
         {
            if (isDebugMode()) logDebug("External operation called (in queue delete) for item " + ids[i] + ".");
            inQueue.remove(ids[i]);
         }
      }
      else if (operationName.equals(outQueueCancelOperationName))
      {
         for (int i = 0; i < ids.length; i++)
         {
            if (isDebugMode()) logDebug("External operation called (out queue cancel) for item " + ids[i] + ".");
            QueueItem item = outQueue.get(ids[i]);
            if (item != null) cancelOutItem(item);
         }
      }
      else if (operationName.equals(outQueueRelocateOperationName))
      {
         for (int i = 0; i < ids.length; i++)
         {
            if (isDebugMode()) logDebug("External operation called (out queue relocate) for item " + ids[i] + ".");
            QueueItem item = outQueue.get(ids[i]);
            if (item != null) this.relocateOutItem(item);
         }
      }
      else if (operationName.equals(outQueueForceCompleteSuccessOperationName))
      {
         for (int i = 0; i < ids.length; i++)
         {
            if (isDebugMode()) logDebug("External operation called (out queue force complete success) for item " + ids[i] + ".");
            QueueItem item = outQueue.get(ids[i]);
            if (item != null) forceOutItemDone(item, QueueItemResponse.QUEUE_ITEM_DONE_SUCCESS);
         }
      }
      else if (operationName.equals(outQueueForceCompleteFailureOperationName))
      {
         for (int i = 0; i < ids.length; i++)
         {
            if (isDebugMode()) logDebug("External operation called (out queue force complete failure) for item " + ids[i] + ".");
            QueueItem item = outQueue.get(ids[i]);
            if (item != null) forceOutItemDone(item, QueueItemResponse.QUEUE_ITEM_DONE_FAILURE);
         }
      }
      else if (operationName.equals(outQueueForceCompleteCancelOperationName))
      {
         for (int i = 0; i < ids.length; i++)
         {
            if (isDebugMode()) logDebug("External operation called (out queue force complete cancel) for item " + ids[i] + ".");
            QueueItem item = outQueue.get(ids[i]);
            if (item != null) forceOutItemDone(item, QueueItemResponse.QUEUE_ITEM_DONE_CANCELLED);
         }
      }
      else if (operationName.equals(outQueueForceRelocationOperationName))
      {
         for (int i = 0; i < ids.length; i++)
         {
            if (isDebugMode()) logDebug("External operation called (out queue force relocation) for item " + ids[i] + ".");
            QueueItem item = outQueue.get(ids[i]);
            if (item != null) forceOutItemDone(item, QueueItemResponse.QUEUE_ITEM_RELOCATION_REQUIRED);
         }
      }
      else if (operationName.equals(outQueueDeleteOperationName))
      {
         for (int i = 0; i < ids.length; i++)
         {
            if (isDebugMode()) logDebug("External operation called (out queue delete) for item " + ids[i] + ".");
            outQueue.remove(ids[i]);
         }
      }
      else
      {
         controller.customExternalQueueOperationCalled(operationName, ids);
      }
   }

   /**
    * Gets the QueueController object used by this QueueManager.
    * 
    * @return the QueueController object used by this QueueManager.
    */
   public QueueController getQueueController()
   {
      return this.controller;
   }

   /**
    * Sets the DefaultQueueSystemCollaborationManager for this QueueManager.
    * 
    * @param newCollaborationManager a DefaultQueueSystemCollaborationManager object.
    */
   public void setQueueCollaborationManager(final QueueSystemCollaborationManager newCollaborationManager)
   {
      if (this.collaborationManager != null)
      {
         if (this.collaborationManager instanceof SubSystem)
         {
            removeSubSystem(((SubSystem) this.collaborationManager), true);
         }
         this.collaborationManager = null;
      }

      this.collaborationManager = newCollaborationManager;

      if (newCollaborationManager instanceof SubSystem)
      {
         addSubComponent((SubSystem) newCollaborationManager);
      }
   }

   /**
    * Sets the flag for removal of completed items from the out queue.<br>
    * <br>
    * This flag is set to <strong><code>true</code></strong> by default.
    * 
    * @param remove boolean value indicating if completed items should be removed or not.
    */
   public void setRemoveCompletedOutQueueItems(final boolean remove)
   {
      this.removeCompletedOutQueueItems = remove;
   }

   /**
    * Returns the value of the flag for removal of completed items.<br>
    * <br>
    * This flag is set to <strong><code>true</code></strong> by default.
    * 
    * @return boolean value indicating the value of the flag.
    */
   public boolean canRemoveCompletedOutQueueItems()
   {
      return this.removeCompletedOutQueueItems;
   }

   /**
    * Sets the flag for removal of aborted in queue items. A QueueItem is aborted when the QueueManager is unable to
    * send an acknowledgement to the sender of that QueueItem that the item has been received by this queue system.<br>
    * <br>
    * This flag is set to <strong><code>true</code></strong> by default.
    * 
    * @param removeAbortedItems boolean value indicating if aborted items should be removed or not.
    */
   public void setRemoveAbortedItems(final boolean removeAbortedItems)
   {
      this.removeAbortedItems = removeAbortedItems;
   }

   /**
    * Returns the value of the flag for removal of aborted items. A QueueItem is aborted when the QueueManager is unable
    * to send an acknowledgement to the sender of that QueueItem that the item has been received by this queue system.<br>
    * <br>
    * This flag is set to <strong><code>true</code></strong> by default.
    * 
    * @return boolean value indicating the value of the flag.
    */
   public boolean canRemoveAbortedItems()
   {
      return this.removeAbortedItems;
   }

   /**
    * Sets the flag for queueitem age checking.<br>
    * <br>
    * This flag is set to <strong><code>true</code></strong> by default.
    * 
    * @param checkOutQueueItemAge boolean value indicating if the age of queueitems should be monitored.
    */
   public void setCheckOutQueueItemAge(final boolean checkOutQueueItemAge)
   {
      this.checkOutQueueItemAge = checkOutQueueItemAge;
   }

   /**
    * Gets the value of the flag for queueitem age checking.<br>
    * <br>
    * This flag is set to <strong><code>true</code></strong> by default.
    * 
    * @return boolean value indicating the value of the flag.
    */
   public boolean mayCheckOutQueueItemAge()
   {
      return checkOutQueueItemAge;
   }

   /**
    * Sets the amount of time (in milliseconds) that has to pass after an outgoing QueueItem has been dispatched until a
    * warning is sent to the QueueController.
    * 
    * @param ageLimit time in milliseconds before warning is sent.
    * @see InOutQueueController#outItemAgeWarning(QueueItem)
    * @see OutQueueController#outItemAgeWarning(QueueItem)
    */
   public void setOutQueueItemAgeLimit(final long ageLimit)
   {
      outItemWarningAgeLimit.setValue(ageLimit);
   }

   /**
    * Gets the amount of time (in milliseconds) that has to pass after an outgoing QueueItem has been dispatched until a
    * warning is sent to the QueueController.
    * 
    * @return the time in milliseconds before warning is sent.
    * @see InOutQueueController#outItemAgeWarning(QueueItem)
    * @see OutQueueController#outItemAgeWarning(QueueItem)
    */
   public long getOutQueueItemAgeLimit()
   {
      return outItemWarningAgeLimit.longValue();
   }

   /**
    * Sets the flag indicating if automatic parent/child handling should be used. If this flag is set to
    * <code>true</code> the QueueManager will keep track of child items in the out queue belonging to an item in the
    * in queue, and make sure that a respons is sent when all children are completed. Additionally the QueueManager will
    * also make sure that calls to the methods cancelInItem and relocateInItem will have have the same effect on child
    * items in the out queue.
    * 
    * @param inOutQueueAutoModeEnabled boolean flag indicating if auto mode should be enabled (<code>true</code>) or
    *           not (<code>false</code>).
    */
   public void setInOutQueueAutoModeEnabled(final boolean inOutQueueAutoModeEnabled)
   {
      this.inOutQueueAutoModeEnabled = inOutQueueAutoModeEnabled;
   }

   /**
    * Gets the flag indicating if automatic parent/child handling should be used. If this flag is set to
    * <code>true</code> the QueueManager will keep track of child items in the out queue belonging to an item in the
    * in queue, and make sure that a respons is sent when all children are completed. Additionally the QueueManager will
    * also make sure that calls to the methods cancelInItem and relocateInItem will have have the same effect on child
    * items in the out queue.
    * 
    * @return a boolean flag indicating if auto mode should be enabled (<code>true</code>) or not (<code>false</code>).
    */
   public boolean isInOutQueueAutoModeEnabled()
   {
      return this.inOutQueueAutoModeEnabled;
   }

   /**
    * Sets the maximum size of the in queue.
    * 
    * @param maxSize the maximum size of the in queue.
    */
   public void setInQueueMaxSize(final int maxSize)
   {
      if (inQueueMaxSize != null)
      {
         inQueueMaxSize.setValue(maxSize);
      }
   }

   /**
    * Increments the the maximum size of the in queue.
    * 
    * @param increment the amount to increment the maximum size of the in queue with.
    */
   public void incrementInQueueMaxSize(final int increment)
   {
      if (inQueueMaxSize != null)
      {
         inQueueMaxSize.increment(increment);
      }
   }

   /**
    * Gets the maximum size of the in queue.
    * 
    * @return the maximum size of the in queue.
    */
   public int getInQueueMaxSize()
   {
      if (inQueueMaxSize != null)
      {
         return inQueueMaxSize.intValue();
      }
      else return -1;
   }
   
   /**
    * Gets the recent completions cache size.
    * 
    * @since 2.1.2 (20060310)
    */   
   public int getRecentCompletionResponseCacheSize()
   {
      return recentCompletionResponseCacheSize.intValue();
   }

   /**
    * Sets the recent completions cache size.
    * 
    * @since 2.1.2 (20060310)
    */
   public void setRecentCompletionResponseCacheSize(int recentCompletionResponseCacheSize)
   {
      this.recentCompletionResponseCacheSize.setValue(recentCompletionResponseCacheSize);
   }

   /**
    * Gets the unsent completion responses max age.
    * 
    * @since 2.1.2 (20060310)
    */
   public int getUnsentResponseMaxAge()
   {
      return unsentResponseMaxAge.intValue();
   }

   /**
    * Sets the unsent completion responses max age.
    * 
    * @since 2.1.2 (20060310)
    */
   public void setUnsentResponseMaxAge(int unsentResponseMaxAge)
   {
      this.unsentResponseMaxAge.setValue(unsentResponseMaxAge);
   }

   /**
    * Gets a unique id that is to be used when creating new QueueItem objects.
    * 
    * @return a unique id in form of String.
    */
   public String getUniqueId()
   {
      synchronized (uniqueIdCounterMonitor)
      {
         uniqueIdCounter++;
         return JServerName.replace(JServerNameAppendIndex, JServerName.length(), Long.toHexString(uniqueIdCounter)).toString();
      }
   }
   
   /**
    * Returns the in-queue of this QueueManager.
    * 
    * @return the in-queue of this QueueManage, null if there is none.
    */
   public Queue getInQueue()
   {
      return inQueue;
   }
   
   /**
    * @since 2.1.5 (20070329)
    */
   public void createInQueue()
   {
      createInQueue(true);
   }
   
   /**
    * @since 2.1.5 (20070329)
    */
   public void createInQueue(final boolean addDefaultExternalQueueOperations)
   {
      this.setInQueue(new Queue(this, QueueManager.defaultInQueueName), addDefaultExternalQueueOperations);
   }

   /**
    * Sets the in-queue of this QueueManager and adds the default external operations (cancel and relocate) to it. This
    * QueueManager will assume ownership of the specified queue, and for that reason it is recommended that it is
    * created using a constructor that doensn't take a parent as a parameter.
    * 
    * @param newInQueue the new in-queue of this QueueManager.
    */
   public void setInQueue(final Queue newInQueue)
   {
      setInQueue(newInQueue, true);
   }

   /**
    * Sets the in-queue of this QueueManager. This QueueManager will assume ownership of the specified queue, and for
    * that reason it is recommended that it is created using a constructor that doensn't take a parent as a parameter.
    * 
    * @param newInQueue the new in-queue of this QueueManager.
    * @param addDefaultExternalQueueOperations flag indicating if the default external operations (cancel and relocate)
    *           should be added to the in queue.
    */
   public void setInQueue(final Queue newInQueue, final boolean addDefaultExternalQueueOperations)
   {
      if (this.inQueue != null)
      {
         removeSubComponent(inQueue);
         this.inQueue = null;
      }

      this.inQueue = newInQueue;
      if( this.inQueue != null )
      {
         this.inQueue.setQueueManager(this);
   
         // Assume ownership over the queue
         addSubComponent(this.inQueue);
   
         if (addDefaultExternalQueueOperations)
         {
            this.inQueue.addExternalOperation(inQueueCancelOperationName, "Cancel");
            this.inQueue.addExternalOperation(inQueueRelocateOperationName, "Relocate");
            this.inQueue.addExternalOperation(inQueueForceQueuedOperationName, "Force queued");
            this.inQueue.addExternalOperation(inQueueDeleteOperationName, "Delete");
         }
      }
   }

   /**
    * Returns the number of items in the in-queue.
    * 
    * @return the length of the in-queue, -1 if in-queue is null.
    */
   public int getInQueueLength()
   {
      if (inQueue != null) return inQueue.size();
      else return -1;
   }

   /**
    * Returns the out-queue of this QueueManager.
    * 
    * @return the out-queue of this QueueManager, null if there is none.
    */
   public Queue getOutQueue()
   {
      return outQueue;
   }
   
   /**
    * @since 2.1.5 (20070329)
    */
   public void createOutQueue()
   {
      createOutQueue(true);
   }
   
   /**
    * @since 2.1.5 (20070329)
    */
   public void createOutQueue(final boolean addDefaultExternalQueueOperations)
   {
      this.setOutQueue(new Queue(this, QueueManager.defaultOutQueueName), addDefaultExternalQueueOperations);
   }

   /**
    * Sets the out-queue of this QueueManager and adds the default external operation (cancel) to it. This QueueManager
    * will assume ownership of the specified queue, and for that reason it is recommended that it is created using a
    * constructor that doensn't take a parent as a parameter.
    * 
    * @param newOutQueue the new out-queue of this QueueManager.
    */
   public void setOutQueue(final Queue newOutQueue)
   {
      setOutQueue(newOutQueue, true);
   }

   /**
    * Sets the out-queue of this QueueManager. This QueueManager will assume ownership of the specified queue, and for
    * that reason it is recommended that it is created using a constructor that doensn't take a parent as a parameter.
    * 
    * @param newOutQueue the new out-queue of this QueueManager.
    * @param addDefaultExternalQueueOperations flag indicating if the default external operation (cancel) should be
    *           added to the out queue.
    */
   public void setOutQueue(final Queue newOutQueue, final boolean addDefaultExternalQueueOperations)
   {
      if (this.outQueue != null)
      {
         removeSubComponent(outQueue);
         this.outQueue = null;
      }

      this.outQueue = newOutQueue;
      if( this.outQueue != null )
      {
         this.outQueue.setQueueManager(this);
   
         // Assume ownership over the queue
         addSubComponent(this.outQueue);
   
         if (addDefaultExternalQueueOperations)
         {
            this.outQueue.addExternalOperation(outQueueCancelOperationName, "Cancel");
            this.outQueue.addExternalOperation(outQueueRelocateOperationName, "Relocate");
            this.outQueue.addExternalOperation(outQueueForceCompleteSuccessOperationName, "Force success");
            this.outQueue.addExternalOperation(outQueueForceCompleteFailureOperationName, "Force failure");
            this.outQueue.addExternalOperation(outQueueForceCompleteCancelOperationName, "Force cancel");
            this.outQueue.addExternalOperation(outQueueForceRelocationOperationName, "Force relocation");
            this.outQueue.addExternalOperation(outQueueDeleteOperationName, "Delete");
         }
      }
   }

   /**
    * Returns the number of items in the out-queue.
    * 
    * @return the length of the out-queue, -1 if out-queue is null.
    */
   public int getOutQueueLength()
   {
      if (outQueue != null) return outQueue.size();
      else return -1;
   }

   /**
    * Returns the DefaultQueueSystemCollaborationManager associated with this QueueManager.
    * 
    * @return a DefaultQueueSystemCollaborationManager object.
    */
   public QueueSystemCollaborationManager getCollaborationManager()
   {
      return collaborationManager;
   }

   /**
    * Convenience method to get a reference to a remote queue system. This method uses the method
    * {@link QueueSystemCollaborationManager#getRemoteQueueSystem(EndPointIdentifier)} which will create a new
    * RemoteQueueSystem object if the currently is none for the specified address.
    * 
    * @param address an address to a remote queue system.
    * @return a RemoteQueueSystem object, null if an error occured while getting one.
    * @see RemoteQueueSystem
    * @see DefaultQueueSystemEndPointProxy
    */
   public RemoteQueueSystem getRemoteQueueSystem(final EndPointIdentifier address)
   {
      if (isDebugMode()) logDebug("getRemoteQueueSystem - " + address + ".");
      return collaborationManager.getRemoteQueueSystem(address);
   }

   /**
    * Convenience method to get a reference to a remote queue system. This method uses the method
    * {@link QueueSystemCollaborationManager#getRemoteQueueSystem(EndPointIdentifier,boolean)} which returns null if
    * there is no RemoteQueueSystem object matching the specified address if parameter <code>create</code> is set to
    * <code>false</code>.
    * 
    * @param address an address to a remote queue system.
    * @param create boolean flag indicating if a new RemoteQueueSystem object will be created if there currently is none
    *           available for the specified address.
    * @return a RemoteQueueSystem object or <code>null</code> if an error occured while getting one. Null is also
    *         returned if the parameter <code>create</code> was set to <code>false</code> and there were no
    *         RemoteQueueSystem object matching the specified address.
    * @see RemoteQueueSystem
    * @see DefaultQueueSystemEndPointProxy
    */
   public RemoteQueueSystem getRemoteQueueSystem(final EndPointIdentifier address, final boolean create)
   {
      if (isDebugMode()) logDebug("getRemoteQueueSystem - " + address + ", " + create + ".");
      return collaborationManager.getRemoteQueueSystem(address, create);
   }

   /**
    * Convenience method to get a list of all remote queue systems connected to this queue system.
    * 
    * @return a list of RemoteQueueSystem objects.
    */
   public List getRemoteQueueSystems()
   {
      return this.collaborationManager.getRemoteQueueSystems();
   }

   /**
    * Makes the calling thread wait until a link has been established with the remote queue system at the specified
    * addres, or until the reference to it is destroyed.<br>
    * <br>
    * This method uses the method {@link QueueSystemCollaborationManager#getRemoteQueueSystem(EndPointIdentifier)} which
    * will create a new RemoteQueueSystem object if the currently is none for the specified address.
    * 
    * @param address an address to a remote queue system.
    * @exception InterruptedException if the current thread was interrupted while waiting.
    * @return <code>false</code> if there was an error getting a reference to a remote queue system for the specified
    *         address, otherwise <code>true</code>.
    */
   public boolean waitForRemoteQueueSystemLinkEstablished(final EndPointIdentifier address) throws InterruptedException
   {
      if (isDebugMode()) logDebug("waitForRemoteQueueSystemLinkEstablished - " + address + ".");
      final RemoteQueueSystem remoteQS = collaborationManager.getRemoteQueueSystem(address);

      if (remoteQS != null)
      {
         remoteQS.waitForLinkEstablished();
         return true;
      }
      else return false;
   }

   /**
    * Makes the calling thread wait until a link has been established with the remote queue system at the specified
    * addres, the specified maxwait time has ellapsed, or until the reference to it is destroyed.<br>
    * <br>
    * This method uses the method {@link QueueSystemCollaborationManager#getRemoteQueueSystem(EndPointIdentifier)} which
    * will create a new RemoteQueueSystem object if the currently is none for the specified address.
    * 
    * @param address an address to a remote queue system.
    * @param maxWait the maximum wait time in milliseconds.
    * @exception InterruptedException if the current thread was interrupted while waiting.
    * @return <code>false</code> if there was an error getting a reference to a remote queue system for the specified
    *         address, otherwise <code>true</code>.
    */
   public boolean waitForRemoteQueueSystemLinkEstablished(final EndPointIdentifier address, final long maxWait) throws InterruptedException
   {
      if (isDebugMode()) logDebug("waitForRemoteQueueSystemLinkEstablished - " + address + ".");
      final RemoteQueueSystem remoteQS = collaborationManager.getRemoteQueueSystem(address);

      if (remoteQS != null)
      {
         remoteQS.waitForLinkEstablished(maxWait);
         return true;
      }
      else return false;
   }

   /**
    * Convenience method to check if a link is established to a remote queue system at the specified address. A call to
    * this method will NOT result in the creation of a new reference to a remote queue system if there currently is none
    * for the specified address.
    * 
    * @param address an address to a remote queue system.
    * @return <code>true</code> if a link is established, otherwise <code>false</code>. False is also returned if
    *         there was an error getting a reference to the queue system at the specified address or if no link was
    *         established with it.
    */
   public boolean isLinkEstablishedWithRemoteQueueSystem(final EndPointIdentifier address)
   {
      if (isDebugMode()) logDebug("isLinkEstablishedWithRemoteQueueSystem - " + address + ".");
      final RemoteQueueSystem remoteQS = collaborationManager.getRemoteQueueSystem(address, false);

      if (remoteQS != null)
      {
         return remoteQS.isLinkEstablished();
      }
      else return false;
   }

   /**
    * Convenience metod to get the expected length of the in-queue of a remote queue system. A call to this method will
    * NOT result in the creation of a new reference to a remote queue system if there currently is none for the
    * specified address.
    * 
    * @param address the address to a remote queue system.
    * @return the expected length of the in-queue of a remote queue system, -1 if there was an error getting a reference
    *         to the queue system at the specified address or if no link was established with it.
    */
   public int getExpectedRemoteInQueueLength(final EndPointIdentifier address)
   {
      if (isDebugMode()) logDebug("getExpectedRemoteInQueueLength - " + address + ".");
      final RemoteQueueSystem remoteQS = collaborationManager.getRemoteQueueSystem(address, false);

      if (remoteQS != null)
      {
         return remoteQS.getExpectedRemoteInQueueLength();
      }
      else return -1;
   }

   /**
    * Convenience metod to get the length of the in-queue of a remote queue system. A call to this method will NOT
    * result in the creation of a new reference to a remote queue system if there currently is none for the specified
    * address.
    * 
    * @param address the address to a remote queue system.
    * @return the length of the in-queue of a remote queue system, -1 if there was an error getting a reference to the
    *         queue system at the specified address or if no link was established with it.
    */
   public int getRemoteInQueueLength(final EndPointIdentifier address)
   {
      if (isDebugMode()) logDebug("getRemoteInQueueLength - " + address + ".");
      final RemoteQueueSystem remoteQS = collaborationManager.getRemoteQueueSystem(address, false);

      if (remoteQS != null)
      {
         return remoteQS.getRemoteInQueueLength();
      }
      else return -1;
   }

   /**
    * Convenience metod to get the maximum length of the in-queue of a remote queue system. A call to this method will
    * NOT result in the creation of a new reference to a remote queue system if there currently is none for the
    * specified address.
    * 
    * @param address the address to a remote queue system.
    * @return the maximum length of the in-queue of a remote queue system, -1 if there was an error getting a reference
    *         to the queue system at the specified address or if no link was established with it.
    */
   public int getRemoteInQueueMaxLength(final EndPointIdentifier address)
   {
      if (isDebugMode()) logDebug("getRemoteInQueueMaxLength - " + address + ".");
      final RemoteQueueSystem remoteQS = collaborationManager.getRemoteQueueSystem(address, false);

      if (remoteQS != null)
      {
         return remoteQS.getRemoteInQueueMaxLength();
      }
      else return -1;
   }

   /**
    * Convenience metod to check if the in queue of a remote queue system is blocked or not. A call to this method will
    * NOT result in the creation of a new reference to a remote queue system if there currently is none for the
    * specified address.
    * 
    * @param address the address to a remote queue system.
    * @return <code>true</code> if the remote in queue is blocked, otherwise <code>false</code>. False is also
    *         returned if there was an error getting a reference to the queue system at the specified address or if no
    *         link was established with it.
    */
   public boolean isRemoteInQueueBlocked(final EndPointIdentifier address)
   {
      if (isDebugMode()) logDebug("isRemoteInQueueBlocked - " + address + ".");
      final RemoteQueueSystem remoteQS = collaborationManager.getRemoteQueueSystem(address, false);

      if (remoteQS != null)
      {
         return remoteQS.isRemoteInQueueBlocked();
      }
      else return false;
   }

   /**
    * Convenience metod to check if the in queue of a remote queue system is full or not. A call to this method will NOT
    * result in the creation of a new reference to a remote queue system if there currently is none for the specified
    * address.
    * 
    * @param address the address to a remote queue system.
    * @return <code>true</code> if the remote in queue is full, otherwise <code>false</code>. False is also
    *         returned if there was an error getting a reference to the queue system at the specified address. Note that
    *         the return value of this method will be <code>true</code> if there is no link established with the
    *         remote queue system.
    */
   public boolean isRemoteInQueueFull(final EndPointIdentifier address)
   {
      if (isDebugMode()) logDebug("isRemoteInQueueFull - " + address + ".");
      final RemoteQueueSystem remoteQS = collaborationManager.getRemoteQueueSystem(address, false);

      if (remoteQS != null)
      {
         return remoteQS.isRemoteInQueueFull();
      }
      else return false;
   }

   /**
    * Convenience metod to destroy (disconnect) a reference to a queue of a remote queue system. A call to this method
    * will NOT result in the creation of a new reference to a remote queue system if there currently is none for the
    * specified address.
    * 
    * @param address the address to a remote queue system.
    * @return <code>true</code> if a reference to a remote queue system at the specified address was available,
    *         otherwise <code>false</code>.
    */
   public boolean destroyRemoteQueueSystem(final EndPointIdentifier address)
   {
      if (isDebugMode()) logDebug("destroyRemoteQueueSystem - " + address + ".");
      final RemoteQueueSystem remoteQS = collaborationManager.getRemoteQueueSystem(address, false);

      if (remoteQS != null)
      {
         remoteQS.destroy();
         return true;
      }
      else return false;
   }

   /**
    * Returns the a metadata object associated with this QueueManager.
    * 
    * @return a QueueSystemMetaData object.
    */
   public QueueSystemMetaData getQueueSystemMetaData()
   {
      if (inQueue != null)
      {
         metaData.setInQueueBlocked(!inQueue.isEnabled());
         metaData.setInQueueLength(getInQueueLength());
         metaData.setinQueueMaxLength(getInQueueMaxSize());
      }
      return metaData;
   }

   /**
    * Convenience method to get the QueueItem in the in queue matching the specified item id.
    * 
    * @param itemId a String uniquely identifying a QueueItem.
    * @return a QueueItem object.
    */
   public QueueItem getInItemWithId(final String itemId)
   {
      if (isDebugMode()) logDebug("getInItemWithId - " + itemId + ".");
      if (inQueue != null) return inQueue.get(itemId);
      else return null;
   }

   /**
    * Convenience method to get the QueueItem in the out queue matching the specified item id.
    * 
    * @param itemId a String uniquely identifying a QueueItem.
    * @return a QueueItem object.
    */
   public QueueItem getOutItemWithId(final String itemId)
   {
      if (isDebugMode()) logDebug("getOutItemWithId - " + itemId + ".");
      if (outQueue != null) return outQueue.get(itemId);
      else return null;
   }

   /**
    * Convenience method to get the parent of the specified QueueItem.
    * 
    * @param outItem a QueueItem to get the parent for.
    * @return the parent of the specified QueueItem, null if there was no in queue or if the parent was not found in the
    *         in queue.
    */
   public QueueItem getOutItemParent(final QueueItem outItem)
   {
      if (isDebugMode()) logDebug("getOutItemParent - " + outItem + ".");
      if (inQueue != null) return inQueue.get(outItem.getParentId());
      else return null;
   }

   /**
    * Gets the child QueueItems of the specified QueueItem.
    * 
    * @param inItem a QueueItem to get the children of.
    * @return the child QueueItems or null if there was no out queue. If the specified QueueItem has no children and
    *         empty array will be returned.
    */
   public QueueItem[] getInItemChildren(final QueueItem inItem)
   {
      if (isDebugMode()) logDebug("getInItemChildren - " + inItem + ".");
      if (outQueue != null) return outQueue.getWithParentId(inItem.getId());
      else return null;
   }

   /**
    * Gets incoming QueueItems that have been received from the specified address. If parameter <code>address</code>
    * is <code>null</code>, all items in the in queue without an address will be retuned.
    * 
    * @param address the address of a remote queue system.
    */
   public QueueItem[] getInItemsWithAddress(final EndPointIdentifier address)
   {
      if (isDebugMode()) logDebug("getInItemsWithAddress - " + address + ".");
      return getItemsWithAddress(inQueue, address);
   }

   /**
    * Gets incoming QueueItems that have been received from the specified address. If parameter <code>address</code>
    * is <code>null</code>, all items in the in queue without an address will be retuned.
    * 
    * @param address the address of a remote queue system.
    */
   public ArrayList getInItemsWithAddressAsList(final EndPointIdentifier address)
   {
      if (isDebugMode()) logDebug("getInItemsWithAddressAsList - " + address + ".");
      return getItemsWithAddressAsList(inQueue, address);
   }

   /**
    * Gets outgoing QueueItems that have been or are beeing dispatched to the specified address. If parameter
    * <code>address</code> is <code>null</code>, all items in the out queue without an address will be retuned.
    * 
    * @param address the address of a remote queue system.
    */
   public QueueItem[] getOutItemsWithAddress(final EndPointIdentifier address)
   {
      if (isDebugMode()) logDebug("getOutItemsWithAddress - " + address + ".");
      return getItemsWithAddress(outQueue, address);
   }

   /**
    * Gets outgoing QueueItems that have been or are beeing dispatched to the specified address. If parameter
    * <code>address</code> is <code>null</code>, all items in the out queue without an address will be retuned.
    * 
    * @param address the address of a remote queue system.
    */
   public ArrayList getOutItemsWithAddressAsList(final EndPointIdentifier address)
   {
      if (isDebugMode()) logDebug("getOutItemsWithAddressAsList - " + address + ".");
      return getItemsWithAddressAsList(outQueue, address);
   }

   /**
    */
   QueueItem[] getItemsWithAddress(final Queue queue, final EndPointIdentifier address)
   {
      return (QueueItem[]) this.getItemsWithAddressAsList(queue, address).toArray(new QueueItem[] {});
   }

   /**
    */
   ArrayList getItemsWithAddressAsList(final Queue queue, final EndPointIdentifier address)
   {
      QueueItem item;
      final ArrayList v = new ArrayList();

      if (queue != null)
      {
         final Iterator it = queue.iterator();

         while (it.hasNext())
         {
            item = (QueueItem) it.next();

            if (item != null)
            {
               if ((address == null) && (item.getSenderReceiverAddress() == null))
               {
                  v.add(item);
               }
               else if ((item.getSenderReceiverAddress() != null) && item.getSenderReceiverAddress().equals(address))
               {
                  v.add(item);
               }
            }
         }
      }

      return v;
   }

   /**
    * Convenience method to attach response data to a QueueItem object. This method makes sure that the persistent state
    * of the QueueItem gets updated after the response data object has been attached. This method will detect if the
    * specified QueueItem is in the in or out queue.
    * 
    * @param item a QueueItem to wich the response data will be attached.
    * @param responseData a response data object.
    * @see com.teletalk.jserver.queue.Queue#updatePersistentStorage(QueueItem)
    * @see com.teletalk.jserver.queue.QueueItem#setResponseData(Object)
    */
   public void attachResponseData(final QueueItem item, final Object responseData) throws QueueStorageException
   {
      if (isDebugMode()) logDebug("attachResponseData - " + item + ".");
      checkAccess(); // Check if it is ok to call this method right now...

      if (item == null)
      {
         logWarning("Got null item in attachResponseData(QueueItem item, Object responseData).");
         return;
      }

      synchronized (this.getQueueItemsLock())
      {
         item.setResponseData(responseData);

         Queue q = item.getQueue();
         if (q != null)
         {
            q.updatePersistentStorage(item);
         }
      }
   }

   /**
    * Creates a QueueItem and places it in the out-queue.
    * 
    * @param itemData an object to be wrapped in a QueueItem.
    * @return the newly created QueueItem.
    * @exception QueueStorageException if an error occurs when the QueueStorage object associated with the out-queue
    *               tries to store the QueueItem.
    */
   public QueueItem createOutgoingQueueItem(final QueueItemData itemData) throws QueueStorageException
   {
      if (isDebugMode()) logDebug("createOutgoingQueueItem - QueueItemData itemData");

      checkAccess(); // Check if it is ok to call this method right now...

      return impl.createOutgoingQueueItem(itemData, null, true);
   }

   /**
    * Creates a QueueItem with a parent QueueItem and places it in the out-queue. The childcount of the specified parent
    * will be incremented by one as a result of this method call.
    * 
    * @param itemData an object to be wrapped in a QueueItem.
    * @param parentItem a QueueItem that will be the parent of the newly created QueueItem.
    * @return the newly created QueueItem.
    * @exception QueueStorageException if an error occurs when the QueueStorage object associated with the out-queue
    *               tries to store the QueueItem.
    */
   public QueueItem createOutgoingQueueItem(final QueueItemData itemData, final QueueItem parentItem) throws QueueStorageException
   {
      if (isDebugMode()) logDebug("createOutgoingQueueItem - QueueItemData itemData, " + parentItem + ".");

      checkAccess(); // Check if it is ok to call this method right now...

      return impl.createOutgoingQueueItem(itemData, parentItem, true);
   }

   /**
    * Creates several QueueItems and places them in the out-queue.
    * 
    * @param itemData an array of object to be wrapped in QueueItems.
    * @return an array of newly created QueueItems.
    * @exception QueueStorageException if an error occurs when the QueueStorage object associated with the out-queue
    *               tries to store the QueueItems.
    */
   public QueueItem[] createOutgoingQueueItems(final QueueItemData[] itemData) throws QueueStorageException
   {
      if (isDebugMode()) logDebug("createOutgoingQueueItems - QueueItemData[] itemData.");

      checkAccess(); // Check if it is ok to call this method right now...

      return impl.createOutgoingQueueItems(itemData, null, true);
   }

   /**
    * Creates several QueueItems associated with a parent QueueItem and places them in the out-queue. The childcount of
    * the specified parent will be incremented by the length of the parameter <code>itemData</code> as a result of
    * this method call.
    * 
    * @param itemData an array of object to be wrapped in QueueItems.
    * @param parentItem a QueueItem that will be the parent of the newly created QueueItems.
    * @return an array of newly created QueueItems.
    * @exception QueueStorageException if an error occurs when the QueueStorage object associated with the out-queue
    *               tries to store the QueueItems.
    */
   public QueueItem[] createOutgoingQueueItems(final QueueItemData[] itemData, final QueueItem parentItem) throws QueueStorageException
   {
      if (isDebugMode()) logDebug("createOutgoingQueueItems - QueueItemData[] itemData, " + parentItem + ".");

      checkAccess(); // Check if it is ok to call this method right now...

      return impl.createOutgoingQueueItems(itemData, parentItem, true);
   }

   /**
    * Dispatches a QueueItem to a specific remote queue system. If the specified QueueItem isn't already in the out
    * queue it will be added to it. Also, if the specified QueueItem is located in the in queue, a new QueueItem object
    * will be created in the out queue and initialized with the data from the item in the in queue. Note that this
    * method requires that the senderReceiverAddress field of the QueueItem object is set. <br>
    * <br>
    * This method will increment the dispatch counter of the queue item.
    * 
    * @param qItem the QueueItem to be dispatched.
    * @exception QueueStorageException if an error occurs when the QueueStorage object associated with the out-queue
    *               tries to store the QueueItem.
    */
   public void dispatchQueueItem(final QueueItem qItem) throws QueueStorageException
   {
      if (qItem == null)
      {
         logWarning("Got null item in dispatchQueueItem(QueueItem qItem).");
         return;
      }

      // Använd impl implementation i nästa build...
      EndPointIdentifier address = qItem.getSenderReceiverAddress();

      if (address == null) throw new QueueException("The specified QueueItem object contains no address!");

      dispatchQueueItem(qItem, address);
   }

   /**
    * Dispatches a QueueItem to a specific remote queue system. If the specified QueueItem isn't already in the out
    * queue it will be added to it. Also, if the specified QueueItem is located in the in queue, a new QueueItem object
    * will be created in the out queue and initialized with the data from the item in the in queue. The newly created
    * out queue item will then be dispatched to the specified address.<br>
    * <br>
    * This method will increment the dispatch counter of the queue item.
    * 
    * @param qItem the QueueItem to be dispatched.
    * @param address the address to a remote queue system.
    * @exception QueueStorageException if an error occurs when the QueueStorage object associated with the out-queue
    *               tries to store the QueueItem.
    */
   public void dispatchQueueItem(final QueueItem qItem, final EndPointIdentifier address) throws QueueStorageException
   {
      if (isDebugMode()) logDebug("dispatchQueueItem - " + qItem + ", " + address + ".");

      checkAccess(); // Check if it is ok to call this method right now...

      if (qItem == null)
      {
         logWarning("Got null item in dispatchQueueItem(QueueItem qItem, EndPointIdentifier address).");
         return;
      }

      impl.dispatchQueueItem(qItem, address);
   }

   /**
    * Creates a QueueItem, places it in the out-queue and dispatches it to a specific remote QueueManager.
    * 
    * @param itemData an object to be wrapped in a QueueItem.
    * @param address the address to a remote QueueManager.
    * @return the newly created QueueItem.
    * @exception QueueStorageException if an error occurs when the QueueStorage object associated with the out-queue
    *               tries to store the QueueItem.
    */
   public QueueItem dispatchQueueItem(final QueueItemData itemData, final EndPointIdentifier address) throws QueueStorageException
   {
      if (isDebugMode()) logDebug("dispatchQueueItem - " + itemData + ", " + address + ".");

      checkAccess(); // Check if it is ok to call this method right now...

      QueueItem qItem = impl.createOutgoingQueueItem(itemData, null, false);
      dispatchQueueItem(qItem, address);

      return qItem;
   }

   /**
    * Creates a QueueItem with a parent QueueItem, places it in the out-queue and dispatches it to a specific remote
    * QueueManager.
    * 
    * @param itemData an object to be wrapped in a QueueItem.
    * @param parentItem a QueueItem that will be the parent of the newly created QueueItem.
    * @param address the address to a remote QueueManager.
    * @return the newly created QueueItem.
    * @exception QueueStorageException if an error occurs when the QueueStorage object associated with the out-queue
    *               tries to store the QueueItem.
    */
   public QueueItem dispatchQueueItem(final QueueItemData itemData, final QueueItem parentItem, final EndPointIdentifier address) throws QueueStorageException
   {
      if (isDebugMode()) logDebug("dispatchQueueItem - " + itemData + ", " + parentItem + ", " + address + ".");

      checkAccess(); // Check if it is ok to call this method right now...

      QueueItem qItem = impl.createOutgoingQueueItem(itemData, parentItem, false);
      dispatchQueueItem(qItem, address);

      return qItem;
   }

   /**
    * Dispatches multiple QueueItems to a specific remote queue system. If the specified QueueItems aren't already in
    * the out queue it will be added to it. Also, if the specified QueueItems are located in the in queue, new QueueItem
    * objects will be created in the out queue and initialized with the data from the items in the in queue. The newly
    * created out queue items will then be dispatched to the specified address.<br>
    * <br>
    * This method will increment the dispatch counter of the queue items.
    * 
    * @param qItems the QueueItems to be dispatched.
    * @param address the address to a remote queue system.
    * @exception QueueStorageException if an error occurs when the QueueStorage object associated with the out-queue
    *               tries to store the QueueItem.
    */
   public void dispatchQueueItems(final QueueItem[] qItems, final EndPointIdentifier address) throws QueueStorageException
   {
      if (isDebugMode()) logDebug("dispatchQueueItem - " + QueueItem.concatToString(qItems) + ", " + address + ".");

      checkAccess(); // Check if it is ok to call this method right now...

      if (qItems == null)
      {
         logWarning("Got null items in dispatchQueueItem(QueueItem qItem, EndPointIdentifier address).");
         return;
      }

      impl.dispatchQueueItems(qItems, address);
   }

   /**
    * Creates several QueueItems, places them in the out-queue and finally dispatches them to a specific remote
    * QueueManager.
    * 
    * @param itemData an array of object to be wrapped in QueueItems.
    * @param address the address to a remote QueueManager.
    * @return an array of newly created QueueItems.
    * @exception QueueStorageException if an error occurs when the QueueStorage object associated with the out-queue
    *               tries to store the QueueItem.
    */
   public QueueItem[] dispatchQueueItems(final QueueItemData[] itemData, final EndPointIdentifier address) throws QueueStorageException
   {
      if (isDebugMode()) logDebug("dispatchQueueItems - QueueItemData[] itemData, " + address + ".");

      checkAccess(); // Check if it is ok to call this method right now...

      QueueItem[] qItems = impl.createOutgoingQueueItems(itemData, null, false);

      impl.dispatchQueueItems(qItems, address);

      return qItems;
   }

   /**
    * Creates several QueueItems associated with a parent QueueItem, places them in the out-queue and finally dispatches
    * them to a specific remote QueueManager.
    * 
    * @param itemData an array of object to be wrapped in QueueItems.
    * @param parentItem a QueueItem that will be the parent of the newly created QueueItems.
    * @param address the address to a remote QueueManager.
    * @return an array of newly created QueueItems.
    * @exception QueueStorageException if an error occurs when the QueueStorage object associated with the out-queue
    *               tries to store the QueueItem.
    */
   public QueueItem[] dispatchQueueItems(final QueueItemData[] itemData, final QueueItem parentItem, final EndPointIdentifier address) throws QueueStorageException
   {
      if (isDebugMode()) logDebug("dispatchQueueItems - QueueItemData[] itemData, " + parentItem + ", " + address + ".");

      checkAccess(); // Check if it is ok to call this method right now...

      QueueItem[] qItems = impl.createOutgoingQueueItems(itemData, parentItem, false);

      impl.dispatchQueueItems(qItems, address);

      return qItems;
   }

   /**
    * Dispatches a response (receipt) for a QueueItem. The destination to which the response will be sent is extracted
    * from the QueueItem.
    * 
    * @param item the QueueItem associated with the response.
    * @param responseType the type of the response.
    * @see com.teletalk.jserver.queue.command.QueueItemTransferResponse
    */
   void dispatchQueueItemTransferResponse(final QueueItem item, final byte responseType)
   {
      if (isDebugMode()) logDebug("dispatchQueueItemTransferResponse - " + item + ", " + responseType + ".");

      dispatchCommand(new QueueItemTransferResponse(item.getSenderReceiverAddress(), item, responseType));
   }

   /**
    * Dispatches a response (receipt) for a QueueItem. The destination to which the response will be sent is extracted
    * from the QueueItem.
    * 
    * @param items the QueueItems associated with the response.
    * @param responseType the type of the response.
    * @see com.teletalk.jserver.queue.command.QueueItemTransferResponse
    */
   void dispatchQueueItemTransferResponse(final QueueItem[] items, final byte responseType)
   {
      if (isDebugMode()) logDebug("dispatchQueueItemTransferResponse - " + QueueItem.concatIds(items) + ", " + responseType + ".");

      dispatchCommand(new MultiQueueItemTransferResponse(items[0].getSenderReceiverAddress(), items, responseType));
   }

   /**
    * Dispatches a response (receipt) for a QueueItem. The destination to which the response will be sent is extracted
    * from the QueueItem.
    * 
    * @param item the QueueItem associated with the response.
    * @param responseType the type of the response.
    * @see com.teletalk.jserver.queue.command.QueueItemCompletionResponse
    */
   void dispatchQueueItemCompletionResponse(final QueueItem item, final byte responseType)
   {
      if (isDebugMode()) logDebug("dispatchQueueItemCompletionResponse - " + item + ", " + responseType + ".");

      dispatchCommand(new QueueItemCompletionResponse(item.getSenderReceiverAddress(), item, responseType));
   }

   /**
    * Dispatches a response (receipt) with custom response data for a QueueItem. The destination to which the response
    * will be sent is extracted from the QueueItem.
    * 
    * @param item the QueueItem associated with the response.
    * @param responseType the type of the response.
    * @param responseData an object to be attached to the responsemessage, containing custom receipt data.
    * @see com.teletalk.jserver.queue.command.QueueItemCompletionResponse
    */
   void dispatchQueueItemCompletionResponse(final QueueItem item, final byte responseType, final Object responseData)
   {
      if (isDebugMode()) logDebug("dispatchQueueItemCompletionResponse - " + item + ", " + responseType + ", Object responseData.");

      dispatchCommand(new QueueItemCompletionResponse(item.getSenderReceiverAddress(), item, responseType, responseData));
   }

   /**
    * Dispatches a query on a QueueItem dispatched to another queue system. The query will make sure that any unsent
    * completion responses for the specified QueueItem in the remote queue system are returned.
    * 
    * @param outQueueItem the QueueItem that a query is to be dispatched for.
    */
   public void dispatchQueueItemQuery(final QueueItem outQueueItem)
   {
      if (isDebugMode()) logDebug("dispatchQueueItemQuery - " + outQueueItem + ".");

      impl.dispatchQueueItemQuery(outQueueItem);
   }

   /**
    * Method to send a response for a QueueItem in the in queue that was completed successfully. This method will remove
    * the QueueItem from the in queue and it's children in the out queue, if any.
    * 
    * @param inItem a QueueItem to be marked as completed successfully.
    */
   public void inItemDoneSuccess(final QueueItem inItem)
   {
      if (isDebugMode()) logDebug("inItemDoneSuccess - " + inItem + ".");

      inItemDone(inItem, QueueItemCompletionResponse.QUEUE_ITEM_DONE_SUCCESS);
   }

   /**
    * Method to send a response for a QueueItem in the in queue that was completed successfully. This method will remove
    * the QueueItem from the in queue and it's children in the out queue, if any.
    * 
    * @param inItem a QueueItem to be marked as completed successfully.
    * @param responseData data to be returned with the response.
    */
   public void inItemDoneSuccess(final QueueItem inItem, final Object responseData)
   {
      if (isDebugMode()) logDebug("inItemDoneSuccess - " + inItem + ", Object responseData.");

      inItemDone(inItem, QueueItemCompletionResponse.QUEUE_ITEM_DONE_SUCCESS, responseData);
   }

   /**
    * Method to send a response for a QueueItem in the in queue that was completed with errors. This method will remove
    * the QueueItem from the in queue and it's children in the out queue, if any.
    * 
    * @param inItem a QueueItem to be marked as completed with errors.
    */
   public void inItemDoneFailure(final QueueItem inItem)
   {
      if (isDebugMode()) logDebug("inItemDoneFailure - " + inItem + ".");

      inItemDone(inItem, QueueItemCompletionResponse.QUEUE_ITEM_DONE_FAILURE);
   }

   /**
    * Method to send a response for a QueueItem in the in queue that was completed with errors. This method will remove
    * the QueueItem from the in queue and it's children in the out queue, if any.
    * 
    * @param inItem a QueueItem to be marked as completed with errors.
    * @param responseData data to be returned with the response.
    */
   public void inItemDoneFailure(final QueueItem inItem, final Object responseData)
   {
      if (isDebugMode()) logDebug("inItemDoneFailure - " + inItem + ", Object responseData.");

      inItemDone(inItem, QueueItemCompletionResponse.QUEUE_ITEM_DONE_FAILURE, responseData);
   }

   /**
    * Method to send a response for a QueueItem in the in queue that was cancelled. This method will remove the
    * QueueItem from the in queue and it's children in the out queue, if any.<BR>
    * <BR>
    * <i>Note!</i> If the queue system is in in/out queue mode it is advisable not to use this method, as it doesn't
    * perform any actual cancellation. To cancel a QueueItem instead use the methods
    * {@link #cancelInItem(QueueItem) cancelInItem} or {@link #cancelOutItem(QueueItem) cancelOutItem}.
    * 
    * @param inItem a QueueItem to be marked as cancelled.
    */
   public void inItemDoneCancelled(final QueueItem inItem)
   {
      if (isDebugMode()) logDebug("inItemDoneCancelled - " + inItem + ".");

      inItemDone(inItem, QueueItemCompletionResponse.QUEUE_ITEM_DONE_CANCELLED);
   }

   /**
    * Method to send a response for a QueueItem in the in queue that was cancelled. This method will remove the
    * QueueItem from the in queue and it's children in the out queue, if any.<BR>
    * <BR>
    * <i>Note!</i> If the queue system is in in/out queue mode it is advisable not to use this method, as it doesn't
    * perform any actual cancellation. To cancel a QueueItem instead use the methods
    * {@link #cancelInItem(QueueItem) relocateInItem} or {@link #cancelOutItem(QueueItem) cancelOutItem}.
    * 
    * @param inItem a QueueItem to be marked as cancelled.
    * @param responseData data to be returned with the response.
    */
   public void inItemDoneCancelled(final QueueItem inItem, final Object responseData)
   {
      if (isDebugMode()) logDebug("inItemDoneCancelled - " + inItem + ", Object responseData.");

      inItemDone(inItem, QueueItemCompletionResponse.QUEUE_ITEM_DONE_CANCELLED, responseData);
   }

   /**
    * Method to send a response for a QueueItem in the in queue that requires relocation. This method will remove the
    * QueueItem from the in queue and it's children in the out queue, if any.<BR>
    * <BR>
    * <i>Note!</i> If the queue system is in in/out queue mode it is advisable not to use this method, as it doesn't
    * perform any actual relocation functionality. To relocate a QueueItem instead use the method
    * {@link #relocateInItem(QueueItem)}.
    * 
    * @param inItem a QueueItem that requires relocation.
    */
   public void inItemRelocationRequired(final QueueItem inItem)
   {
      if (isDebugMode()) logDebug("inItemRelocationRequired - " + inItem + ".");

      inItemDone(inItem, QueueItemCompletionResponse.QUEUE_ITEM_RELOCATION_REQUIRED);
   }

   /**
    * Method to send a response for a QueueItem in the in queue that requires relocation. This method will remove the
    * QueueItem from the in queue and it's children in the out queue, if any.<BR>
    * <BR>
    * <i>Note!</i> If the queue system is in in/out queue mode it is advisable not to use this method, as it doesn't
    * perform any actual relocation functionality. To relocate a QueueItem instead use the method
    * {@link #relocateInItem(QueueItem)}.
    * 
    * @param inItem a QueueItem that requires relocation.
    * @param responseData data to be returned with the response.
    */
   public void inItemRelocationRequired(final QueueItem inItem, final Object responseData)
   {
      if (isDebugMode()) logDebug("inItemRelocationRequired - " + inItem + ", Object responseData.");

      inItemDone(inItem, QueueItemCompletionResponse.QUEUE_ITEM_RELOCATION_REQUIRED, responseData);
   }

   /**
    * Method to send a response for a completed QueueItem. The type of response that will be sent depend on the status
    * of the QueueItem, i.e. if the status is DONE_CANCELLED a response indicating that the QueueItem was cancelled will
    * be sent.
    * 
    * @param inItem a QueueItem to send a response for.
    */
   public void inItemDone(final QueueItem inItem)
   {
      if (isDebugMode()) logDebug("inItemDone - " + inItem + ".");

      if (inItem == null)
      {
         logWarning("Got null item in inItemDone(QueueItem inItem).");
         return;
      }

      if (inItem.getStatus() == QueueItem.DONE_CANCELLED) inItemDoneCancelled(inItem);
      else if (inItem.getStatus() == QueueItem.DONE_FAILURE) inItemDoneFailure(inItem);
      else if (inItem.getStatus() == QueueItem.RELOCATION_REQUIRED) inItemRelocationRequired(inItem);
      else inItemDoneSuccess(inItem);
   }

   /**
    * Method to send a response for a completed QueueItem. The type of response that will be sent depend on the status
    * of the QueueItem, i.e. if the status is DONE_CANCELLED a response indicating that the QueueItem was cancelled will
    * be sent.
    * 
    * @param inItem a QueueItem to send a response for.
    * @param responseData data to be returned with the response.
    */
   public void inItemDone(final QueueItem inItem, final Object responseData)
   {
      if (isDebugMode()) logDebug("inItemDone - " + inItem + ", Object responseData.");

      if (inItem == null)
      {
         logWarning("Got null item in inItemDone(QueueItem inItem, Object responseData).");
         return;
      }

      if (inItem.getStatus() == QueueItem.DONE_CANCELLED) inItemDoneCancelled(inItem, responseData);
      else if (inItem.getStatus() == QueueItem.DONE_FAILURE) inItemDoneFailure(inItem, responseData);
      else if (inItem.getStatus() == QueueItem.RELOCATION_REQUIRED) inItemRelocationRequired(inItem, responseData);
      else inItemDoneSuccess(inItem, responseData);
   }

   /**
    */
   void inItemDone(final QueueItem inItem, final byte responseType)
   {
      checkAccess(); // Check if it is ok to call this method right now...

      if (inItem == null)
      {
         logWarning("Got null item in inItemDone(QueueItem inItem, QueueItemCompletionResponse.ResponseType responseType).");
         return;
      }

      impl.inItemDone(inItem, responseType, null);
   }

   /**
    */
   void inItemDone(final QueueItem inItem, final byte responseType, final Object responseData)
   {
      checkAccess(); // Check if it is ok to call this method right now...

      if (inItem == null)
      {
         logWarning("Got null item in inItemDone(QueueItem inItem, byte responseType, Object responseData).");
         return;
      }

      impl.inItemDone(inItem, responseType, responseData);
   }

   /**
    * Checks if an item in the in queue is completed by checking the status of all its children. The in queue item is
    * considered to be completed if all its children are completed.<BR>
    * <BR>
    * This method is obviously only intresting in an in/out queue configuration.
    * 
    * @return If an in/out queue configuration is used and all the children of the specified in queue item are
    *         completed, the return value will be <code>true</code>. If an in queue configuration is used the
    *         returnvalue will be the result of callig the method {@link QueueItem#isCompleted() isCompleted()} on the
    *         specified in queue item. All other situations will result in a return value of <code>false</code>.
    */
   public boolean isInItemCompleted(final QueueItem inItem)
   {
      if (isDebugMode()) logDebug("isInItemCompleted - " + inItem + ".");

      return impl.isInItemCompleted(inItem);
   }

   /**
    * Forces an outgoing QueueItem to be marked as completed by creating a fake completion response which will be
    * processed by the QueueManager in exactly the same way as "normal" completion responses. This makes it possible for
    * the queue controller to force an outgoing item to complete and still use the standard mechanism for handling of
    * completed outgoing items.
    * 
    * @param outItem a QueueItem to mark as completed.
    * @param responseType the type of the faked response.
    * @see com.teletalk.jserver.queue.command.QueueItemCompletionResponse
    */
   public void forceOutItemDone(final QueueItem outItem, final byte responseType)
   {
      if (isDebugMode()) logDebug("forceOutItemDone - " + outItem + ", " + responseType + ".");

      checkAccess(); // Check if it is ok to call this method right now...

      forceOutItemDone(outItem, responseType, null);
   }

   /**
    * Forces an outgoing QueueItem to be marked as completed by creating a fake completion response which will be
    * processed by the QueueManager in exactly the same way as "normal" completion responses. This makes it possible for
    * the queue controller to force an outgoing item to complete and still use the standard mechanism for handling of
    * completed outgoing items.
    * 
    * @param outItem a QueueItem to mark as completed.
    * @param responseType the type of the faked response.
    * @param responseData data to be returned with the faked response.
    * @see com.teletalk.jserver.queue.command.QueueItemCompletionResponse
    */
   public void forceOutItemDone(final QueueItem outItem, final byte responseType, final Object responseData)
   {
      if (isDebugMode()) logDebug("forceOutItemDone - " + outItem + ", " + responseType + ", Object responseData.");

      checkAccess(); // Check if it is ok to call this method right now...

      impl.forceOutItemDone(outItem, responseType, responseData);
   }

   /**
    * Cancels a QueueItem in the in-queue and it's child items, if any and if the flag indicating if in/out queue auto
    * handling is enabled is set to <code>true</code>.
    * 
    * @param inItem the QueueItem to cancel.
    */
   public void cancelInItem(final QueueItem inItem)
   {
      if (isDebugMode()) logDebug("cancelInItem - " + inItem + ".");

      checkAccess(); // Check if it is ok to call this method right now...

      impl.cancelInItem(inItem);
   }

   /**
    * Cancels an item in the out-queue. If the item is dispatched or is beeing dispatched to another queue system, a
    * request to cancel it is dispatched. Otherwise the standard handling for cancelled out items is invoked (see
    * {@link InOutQueueController#outItemDoneCancelled(QueueItem, Object)} or
    * {@link OutQueueController#outItemDoneCancelled(QueueItem, Object)}).
    * 
    * @param outItem the QueueItem to cancel.
    */
   public void cancelOutItem(final QueueItem outItem)
   {
      if (isDebugMode()) logDebug("cancelOutItem - " + outItem + ".");

      checkAccess(); // Check if it is ok to call this method right now...

      impl.cancelOutItem(outItem);
   }

   /**
    * Relocates a QueueItem in the in-queue and cancels it's child items, if any and if the flag indicating if in/out
    * queue auto handling is enabled is set to <code>true</code>.
    * 
    * @param inItem the QueueItem to relocate.
    */
   public void relocateInItem(final QueueItem inItem)
   {
      if (isDebugMode()) logDebug("relocateInItem - " + inItem + ".");

      checkAccess(); // Check if it is ok to call this method right now...

      impl.relocateInItem(inItem);
   }

   /**
    * Relocates a QueueItem in the out-queue. If the item is dispatched or is beeing dispatched to another queue system,
    * a request to relocate it is dispatched. Otherwise the standard handling for relocated out items is invoked (see
    * {@link InOutQueueController#outItemRelocationRequestReceived(QueueItem, Object)} or
    * {@link OutQueueController#outItemRelocationRequestReceived(QueueItem, Object)}).
    * 
    * @param outItem the QueueItem to relocate.
    */
   public void relocateOutItem(final QueueItem outItem)
   {
      if (isDebugMode()) logDebug("relocateOutItem - " + outItem + ".");

      checkAccess(); // Check if it is ok to call this method right now...

      impl.relocateOutItem(outItem);
   }

   /**
    * Dispatches the specified QueueSystemCommand.
    * 
    * @param command a QueueSystemCommand to dispatch.
    * @see com.teletalk.jserver.queue.command.QueueSystemCommand
    */
   public void dispatchCommand(final QueueSystemCommand command)
   {
      if (isDebugMode()) logDebug("dispatchCommand - " + command + ".");

      if (command == null)
      {
         logWarning("Got null command in dispatchCommand(QueueSystemCommand command).");
      }
      else
      {
         collaborationManager.dispatchCommand(command);
      }
   }

   /**
    * Notifies the QueueManager on the result of an attempt to deliver a command to a remote queue system. This method 
    * must always be invoked by the QueueSystemCollaborationManager implementation.
    * 
    * @param command the QueueSystemCommand that was to be delivered.
    * @param success flag indicating if the command was successfully delivered.
    */
   public void commandDeliveryReport(final QueueSystemCommand command, final boolean success)
   {
      if (!success)
      {
         try
         {
            command.abort(this);
         }
         catch (Exception e)
         {
            logError("Error occurred while aborting command due to unsuccessful command delivery.", e);
         }
      }
      else
      {
         if( command instanceof QueueItemCompletionResponse )
         {
            this.addRecentCompletionResponse(command);
         }
      }
   }

   /**
    * Method to notify the QueueManager that a link to a remote queue system was lost. This method will also notify the
    * associated QueueController.
    * 
    * @param remoteQueueSystem the remote queue system to which a link was lost.
    * @see QueueController#linkToRemoteQueueSystemLost(RemoteQueueSystem)
    */
   public void linkToRemoteQueueSystemLost(final RemoteQueueSystem remoteQueueSystem)
   {
      if (isDebugMode()) logDebug("linkToRemoteQueueSystemLost - " + remoteQueueSystem + ".");
      
      synchronized (this.getQueueItemsLock() )
      {
         // Check if there are any outgoing items for the specified remote queue system that are in state DISPATCHING and
         // if so, abort them (mark them as DISPATCH_FAILED).
         // (this can only happen if a transfer request has been dispatched but no response has been received yet)
         if (this.outQueue != null)
         {
            QueueItem[] itemsToAddress = this.getOutItemsWithAddress(remoteQueueSystem.getRemoteQueueSystemAddress());
   
            if (itemsToAddress != null)
            {
               for (int i = 0; i < itemsToAddress.length; i++)
               {
                  if (itemsToAddress[i].getStatus() == QueueItem.DISPATCHING)
                  {
                     this.impl.queueItemTransferRequestAborted(itemsToAddress[i]); // Use standard handling for aborted
                     // transfer requests
                  }
               }
            }
         }
      }

      getQueueController().linkToRemoteQueueSystemLost(remoteQueueSystem); // Notify controller
   }

   /**
    * Method to handle an incoming command.
    * 
    * @param command a QueueSystemCommand to handle.
    */
   public void handleQueueSystemCommand(final QueueSystemCommand command)
   {
      command.execute(this);
   }

   // ------------------------------------------ //
   // --- REQUEST/RESPONSE HANDLER METHODS --- //
   // ------------------------------------------ //

   public QueueSystemSynchronizationRequest initiateQueueSystemSynchronization(final RemoteQueueSystem rqs)
   {
      return impl.initiateQueueSystemSynchronization(rqs);
   }

   /**
    */
   public QueueSystemSynchronizationResponse queueSystemSynchronizationRequestReceived(final RemoteQueueSystem rqs, final QueueSystemSynchronizationRequest synchRequest)
   {
      return impl.queueSystemSynchronizationRequestReceived(rqs, synchRequest);
   }

   /**
    */
   public void queueSystemSynchronizationResponseReceived(final RemoteQueueSystem rqs, final QueueSystemSynchronizationResponse synchResponse)
   {
      impl.queueSystemSynchronizationResponseReceived(rqs, synchResponse);
   }

   /**
    * Method to handle an incoming QueueItemTransferRequest command.
    * 
    * @param request a QueueItemTransferRequest.
    */
   public void queueItemTransferRequestReceived(final QueueItemTransferRequest request)
   {
      if (isDebugMode()) logDebug("queueItemTransferRequestReceived - " + request + ".");
      impl.queueItemTransferRequestReceived(request);
   }

   /**
    * Method to handle an aborted QueueItemTransferRequest command.
    * 
    * @param request a QueueItemTransferRequest.
    */
   public void queueItemTransferRequestAborted(final QueueItemTransferRequest request)
   {
      // Frågan är om man ska skilja på när ett jobb inte kan skickas och när man får tillbaka ett meddelande
      // om att det inte gick att köa....
      if (isDebugMode()) logDebug("queueItemTransferRequestAborted - " + request + ".");

      impl.queueItemTransferRequestAborted(request.getQueueItem());
   }

   /**
    * Method to handle an incoming MultiQueueItemTransferRequest command.
    * 
    * @param request a MultiQueueItemTransferRequest.
    */
   public void queueItemTransferRequestReceived(final MultiQueueItemTransferRequest request)
   {
      if (isDebugMode()) logDebug("multiQueueItemTransferRequestReceived - " + request + ".");
      impl.multiQueueItemTransferRequestReceived(request);
   }

   /**
    * Method to handle an aborted MultiQueueItemTransferRequest command.
    * 
    * @param request a MultiQueueItemTransferRequest.
    */
   public void queueItemTransferRequestAborted(final MultiQueueItemTransferRequest request)
   {
      // Frågan är om man ska skilja på när ett jobb inte kan skickas och när man får tillbaka ett meddelande
      // om att det inte gick att köa....
      if (isDebugMode()) logDebug("queueItemTransferRequestAborted - " + request + ".");

      QueueItem[] items = request.getQueueItems();
      for (int i = 0; i < items.length; i++) impl.queueItemTransferRequestAborted(items[i]);
   }

   /**
    * Method to handle an incoming QueueItemTransferResponse command that indicates that a QueueItem was transferred
    * successfully.
    * 
    * @param response a QueueItemTransferResponse.
    */
   public void queueItemTransferredResponseReceived(final QueueItemTransferResponse response)
   {
      if (isDebugMode()) logDebug("queueItemTransferredResponseReceived - " + response + ".");

      String outItemId = response.getItemId();
      QueueItem item = this.outQueue.get(outItemId);

      impl.queueItemTransferred(outItemId, item);
   }

   /**
    * Method to handle an incoming QueueItemTransferResponse command that indicates that a QueueItem failed to be
    * transferred.
    * 
    * @param response a QueueItemTransferRequest.
    */
   public void queueItemTransferFailureResponseReceived(final QueueItemTransferResponse response)
   {
      if (isDebugMode()) logDebug("queueItemTransferFailureResponseReceived - " + response + ".");

      String outItemId = response.getItemId();
      QueueItem item = this.outQueue.get(outItemId);

      impl.queueItemTransferFailure(outItemId, item);
   }

   /**
    * Method to handle an incoming QueueItemTransferResponse command that indicates that a QueueItem failed to be
    * transferred because the in queue of the receiving queue system was full.
    * 
    * @param response a QueueItemTransferResponse.
    */
   public void queueItemTransferFailureQueueFullResponseReceived(final QueueItemTransferResponse response)
   {
      if (isDebugMode()) logDebug("queueItemTransferFailureQueueFullResponseReceived - " + response + ".");

      String outItemId = response.getItemId();
      QueueItem item = this.outQueue.get(outItemId);

      impl.queueItemTransferFailureQueueFull(outItemId, item);
   }

   /**
    * Method to handle an aborted QueueItemTransferResponse command.
    * 
    * @param command a QueueItemTransferResponse.
    */
   public void queueItemTransferResponseAborted(final QueueItemTransferResponse command)
   {
      if (isDebugMode()) logDebug("Unable to send QueueItemTransferResponse: " + command.toString() + ".");

      String inItemId = command.getItemId();
      QueueItem item = this.inQueue.get(inItemId);

      impl.queueItemTransferResponseAborted(inItemId, item);
   }

   /**
    * Method to handle an incoming QueueItemTransferResponse command that indicates that a QueueItem was transferred
    * successfully.
    * 
    * @param response a QueueItemTransferResponse.
    */
   public void queueItemTransferredResponseReceived(final MultiQueueItemTransferResponse response)
   {
      if (isDebugMode()) logDebug("queueItemTransferredResponseReceived - " + response + ".");

      String[] outItemIds = response.getItemIds();
      QueueItem[] items = this.outQueue.get(outItemIds);

      for (int i = 0; i < outItemIds.length; i++)
         impl.queueItemTransferred(outItemIds[i], items[i]);
   }

   /**
    * Method to handle an incoming QueueItemTransferResponse command that indicates that a QueueItem failed to be
    * transferred.
    * 
    * @param response a QueueItemTransferRequest.
    */
   public void queueItemTransferFailureResponseReceived(final MultiQueueItemTransferResponse response)
   {
      if (isDebugMode()) logDebug("queueItemTransferFailureResponseReceived - " + response + ".");

      String[] outItemIds = response.getItemIds();
      QueueItem[] items = this.outQueue.get(outItemIds);

      for (int i = 0; i < outItemIds.length; i++)
         impl.queueItemTransferFailure(outItemIds[i], items[i]);
   }

   /**
    * Method to handle an incoming QueueItemTransferResponse command that indicates that a QueueItem failed to be
    * transferred because the in queue of the receiving queue system was full.
    * 
    * @param response a QueueItemTransferResponse.
    */
   public void queueItemTransferFailureQueueFullResponseReceived(final MultiQueueItemTransferResponse response)
   {
      if (isDebugMode()) logDebug("queueItemTransferFailureQueueFullResponseReceived - " + response + ".");

      String[] outItemIds = response.getItemIds();
      QueueItem[] items = this.outQueue.get(outItemIds);

      for (int i = 0; i < outItemIds.length; i++)
         impl.queueItemTransferFailureQueueFull(outItemIds[i], items[i]);
   }

   /**
    * Method to handle an aborted QueueItemTransferResponse command.
    * 
    * @param command a QueueItemTransferResponse.
    */
   public void queueItemTransferResponseAborted(final MultiQueueItemTransferResponse command)
   {
      if (isDebugMode()) logDebug("Unable to send MultiQueueItemTransferResponse: " + command.toString() + ".");

      String[] inItemIds = command.getItemIds();
      QueueItem[] items = this.inQueue.get(inItemIds);

      for (int i = 0; i < inItemIds.length; i++)
         impl.queueItemTransferResponseAborted(inItemIds[i], items[i]);
   }

   /**
    * Method to handle an incoming QueueItemQuery command.
    * 
    * @param query a QueueItemQuery.
    */
   public void queueItemQueryReceived(final QueueItemQuery query)
   {
      if (isDebugMode()) logDebug("queueItemQueryReceived - " + query + ".");

      impl.queueItemQueryReceived(query);
   }

   /**
    * Method to handle an incoming QueueItemCompletionResponse command indicating a successfully completed QueueItem.
    * 
    * @param response a QueueItemCompletionResponse.
    */
   public void queueItemSuccessResponseReceived(final QueueItemCompletionResponse response)
   {
      if (isDebugMode()) logDebug("queueItemSuccessResponseReceived - " + response + ".");
      impl.queueItemCompletedSuccess(response);
   }

   /**
    * Method to handle an incoming QueueItemCompletionResponse command indicating a QueueItem that was completed with
    * errors.
    * 
    * @param response a QueueItemCompletionResponse.
    */
   public void queueItemFailureResponseReceived(final QueueItemCompletionResponse response)
   {
      if (isDebugMode()) logDebug("queueItemFailureResponseReceived - " + response + ".");
      impl.queueItemCompletedFailure(response);
   }

   /**
    * Method to handle an incoming QueueItemCompletionResponse command indicating a cancelled QueueItem.
    * 
    * @param response a QueueItemCompletionResponse.
    */
   public void queueItemCancelledResponseReceived(final QueueItemCompletionResponse response)
   {
      if (isDebugMode()) logDebug("queueItemCancelledResponseReceived - " + response + ".");
      impl.queueItemCompletedCancelled(response);
   }

   /**
    * Method to handle an incoming QueueItemCompletionResponse command indicating a QueueItem that needs relocation.
    * 
    * @param response a QueueItemCompletionResponse.
    */
   // public void queueItemRelocationRequestReceived(QueueItemCompletionResponse response)
   public void queueItemRelocationRequiredResponseReceived(final QueueItemCompletionResponse response)
   {
      if (isDebugMode()) logDebug("queueItemRelocationRequestReceived - " + response + ".");
      impl.queueItemRelocationRequiredResponseReceived(response);
   }

   /**
    * Method to handle an aborted QueueItemCompletionResponse command.
    * 
    * @param command a QueueItemCompletionResponse.
    */
   public void queueItemCompletionResponseAborted(final QueueItemCompletionResponse command)
   {
      if (isDebugMode()) logDebug("queueItemCompletionResponseAborted - " + command + ".");
      impl.queueItemCompletionResponseAborted(command);
   }

   /**
    * Method to handle an incoming QueueItemCancellationRequest command.
    * 
    * @param request a QueueItemCancellationRequest.
    */
   public void queueItemCancellationRequestReceived(final QueueItemCancellationRequest request)
   {
      if (isDebugMode()) logDebug("queueItemCancellationRequestReceived - " + request + ".");
      impl.queueItemCancellationRequest(request);
   }

   /**
    * Method to handle an incoming QueueItemRelocationRequest command indicating that an item in the in queue needs
    * relocation.
    * 
    * @param request a QueueItemRelocationRequest.
    */
   public void queueItemRelocationRequestReceived(final QueueItemRelocationRequest request)
   {
      if (isDebugMode()) logDebug("QueueItemRelocationRequest - " + request + ".");
      impl.queueItemRelocationRequest(request);
   }

   /**
    * Method to handle an aborted QueueItemCancellationRequest command.
    * 
    * @param command a QueueItemCancellationRequest.
    */
   public void queueItemCancellationRequestAborted(final QueueItemCancellationRequest command)
   {
      if (isDebugMode()) logDebug("queueItemCancellationRequestAborted - " + command + ".");
      logWarning("Unable to send " + command.toString() + ".");
   }

   /**
    * Method to handle an incoming QueueControllerCommand command.
    * 
    * @param command a QueueControllerCommand.
    */
   public void queueControllerCommandReceived(final QueueControllerCommand command)
   {
      if (isDebugMode()) logDebug("queueControllerCommandReceived - " + command + ".");
      this.controller.queueControllerCommandReceived(command);
   }

   /**
    * Method to handle an aborted QueueControllerCommand command.
    * 
    * @param command a QueueControllerCommand.
    */
   public void queueControllerCommandAborted(final QueueControllerCommand command)
   {
      if (isDebugMode()) logDebug("queueControllerCommandAborted - " + command + ".");
      controller.unableToDeliverControllerCommand(command);
   }

   /**
    * The thread method of this QueueManager. This method performs various maintenance work on the queues, like for
    * instance monitoring the time passed since a certain QueueItem was put in the out queue.
    */
   public void run()
   {
      // Reset status transition timeout to default
      super.statusTransitionTimeout = DEFAULT_STATUS_TRANSITION_TIMEOUT;
      
      while (canRun)
      {
         long timeOut = checkInterval.longValue();

         try
         {
            Thread.sleep(timeOut);
         }
         catch (Exception e)
         {
            if (!canRun) break;
         }
         
         RemoteQueueSystem remoteQueueSystem;

         // Check if there are any unsent responses that are to be removed
         if (unsentCompletionResponses.size() > 0)
         {
            String[] queueItemIds;
            QueueItemResponse response;

            synchronized (unsentCompletionResponses)
            {
               queueItemIds = (String[]) unsentCompletionResponses.keySet().toArray(new String[] {});
            }

            // unsentResponseMaxAge is hours
            long unsentResponseMaxAgeMs = unsentResponseMaxAge.intValue() * 60 * 60 * 1000;

            for (int i = 0; i<queueItemIds.length; i++)
            {
               if (queueItemIds[i] != null)
               {
                  synchronized (unsentCompletionResponses)
                  {
                     response = (QueueItemResponse) unsentCompletionResponses.get(queueItemIds[i]);
                     if (response != null)
                     {
                        if ((System.currentTimeMillis() - response.getSendTime()) > unsentResponseMaxAgeMs)
                        {
                           logInfo("Removing unsent response (" + response + ") due to old age.");
                           unsentCompletionResponses.remove(queueItemIds[i]);
                        }
                     }
                  }
                  
                  // Check if there is any unsent completion responses for a connected (link established) remote queue system (for some reason...)
                  if (response != null)
                  {
                     remoteQueueSystem = this.getRemoteQueueSystem(response.getAddress(), false);
                     if( (remoteQueueSystem != null) && (remoteQueueSystem.isLinkEstablished()) )
                     {
                        logInfo("Redispatching unsent completion response (" + response + ").");
                        remoteQueueSystem.dispatchCommand(response);
                        unsentCompletionResponses.remove(queueItemIds[i]);
                     }
                  }
               }
            }
         }

         if (!impl.performQueueSystemCheck()) // Perform check on QueueItems that are dispatching here!
         {
            logError("Fatal error in queue system!");
            this.criticalError();
         }

         // Perform check to see if there are any items in the out queue that have been there abnormally long
         impl.performOutItemCheck();
      }
   }
}
