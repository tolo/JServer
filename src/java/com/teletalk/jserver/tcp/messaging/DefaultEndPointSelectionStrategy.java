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
package com.teletalk.jserver.tcp.messaging;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.teletalk.jserver.util.MessageQueue;

/**
 * Default strategy for selecting endpoints for dispatching of messages. 
 * 
 * @since 1.3
 * 
 * @author Tobias Löfstrand
 */
public class DefaultEndPointSelectionStrategy implements EndPointSelectionStrategy
{
   private final Object endPointSelectionStrategyLock;
   
   /** Queue for available endpoints. */
   private final MessageQueue endPointQueue;
   
   private final HashMap destinationEndPointMap;

   private int notifyNeeded = 0;

   private boolean enabled = true;

   private MessagingManager messagingManager;
   
   
   /**
    * Creates a new DefaultEndPointSelectionStrategy. Note that this object will be in an inconsistent state until setMessagingManager is called.
    */
   public DefaultEndPointSelectionStrategy()
   {
      this.destinationEndPointMap = new HashMap();
      
      this.endPointQueue = new MessageQueue();
      this.endPointSelectionStrategyLock = this.endPointQueue.getLock();
   }

   /**
    * Sets the messaging manager associated with this strategy.
    */
   public void setMessagingManager(MessagingManager messagingManager)
   {
      this.messagingManager = messagingManager;
   }

   /**
    * Enables this EndPointSelectionStrategy.
    */
   public void initialize()
   {
      synchronized (this.endPointSelectionStrategyLock)
      {
         this.enabled = true;
         this.endPointQueue.setBlockingModeEnabled(true);
      }
   }

   /**
    * Disables this EndPointSelectionStrategy.
    */
   public void shutDown()
   {
      synchronized (this.endPointSelectionStrategyLock)
      {
         this.endPointQueue.clear();
         this.destinationEndPointMap.clear();
         this.notifyNeeded = 0;
         this.enabled = false;
         this.endPointQueue.setBlockingModeEnabled(false);
      }
   }

   /**
    * Called when an endpoint is marked as available for message dispapatch.<br>
    * <br>
    * Note: This method must never be called with a lock held on {@link MessagingManager#getEndpointGroupsLock()}.
    */
   public void endPointReady(final MessagingEndPoint endPoint)
   {
      endPoint.setLastReadyTime(System.currentTimeMillis());
      
      if (endPoint == null) return;

      synchronized (this.endPointSelectionStrategyLock)
      {
         final Destination destination = endPoint.getDestination();

         if (destination != null)
         {
            // Add to destination endpoint queue
            ArrayList availableDestinationEndPoints = (ArrayList) this.destinationEndPointMap.get(destination.getKey());

            if (availableDestinationEndPoints == null)
            {
               availableDestinationEndPoints = new ArrayList();
               this.destinationEndPointMap.put(destination.getKey(), availableDestinationEndPoints);
            }

            // Add at the end of list of destination endpoints
            if( !availableDestinationEndPoints.contains(endPoint) ) availableDestinationEndPoints.add(endPoint);
            
            // Add to global endpoint queue
            if( !this.endPointQueue.contains(endPoint) ) this.endPointQueue.putMsg(endPoint);
            
            /*if( this.messagingManager.isDebugMode() )
            {
               this.messagingManager.logDebug("DefaultEndPointSelectionStrategy - Marking endpoint as available: " + endPoint + ". " +
                  "Available endpoints for destination " + destination + ": " + availableDestinationEndPoints.size() + ". Global endpoint queue size: " + this.endPointQueue.size() + ".");
            }*/
            
            if (notifyNeeded != 0)
            { 
               this.endPointSelectionStrategyLock.notify(); // Wake up one thread
            }
         }
      }
   }

   /**
    * Called when an endpoint is destroyed and can no longer be used for dispatching messages.<br>
    * <br>
    * Note: This method must never be called with a lock held on {@link MessagingManager#getEndpointGroupsLock()}.
    */
   public void endPointDestroyed(final MessagingEndPoint endPoint)
   {
      if (endPoint == null) return;

      synchronized (this.endPointSelectionStrategyLock)
      {
         // Remove from global endpoint queue
         this.endPointQueue.remove(endPoint);

         final Destination destination = endPoint.getDestination();

         if (destination != null)
         {
            // Remove from destination endpoint queue
            final ArrayList availableDestinationEndPoints = (ArrayList) this.destinationEndPointMap.get(destination.getKey());

            if (availableDestinationEndPoints != null)
            {
               availableDestinationEndPoints.remove(endPoint);
               if (availableDestinationEndPoints.size() == 0)
               {
                  this.destinationEndPointMap.remove(destination.getKey());
               }
            }
         }
      }
   }

   /**
    * Gets the first available endpoint. This method will wait a maximum of <code>timeOut</code> milliseconds for an
    * available endpoint.<br>
    * <br>
    * Note: This method must never be called with a lock held on {@link MessagingManager#getEndpointGroupsLock()}.
    */
   public MessagingEndPoint getEndPoint(final long timeOut) throws InterruptedException
   {
      synchronized (this.endPointSelectionStrategyLock)
      {
         //this.messagingManager.logDebug("DefaultEndPointSelectionStrategy - " + this.endPointQueue.size() + " available endpoints.");
         
         return (MessagingEndPoint) this.endPointQueue.getMsg(timeOut);
      }
   }

   /**
    * Gets the first available endpoint for the specified destination. This method will wait a maximum of
    * <code>timeOut</code> milliseconds for an available endpoint.<br>
    * <br>
    * Note: This method must never be called with a lock held on {@link MessagingManager#getEndpointGroupsLock()}.
    */
   public MessagingEndPoint getEndPoint(final Destination destination, long timeOut) throws InterruptedException
   {
      final long beginWait = System.currentTimeMillis();
      long waitTime = timeOut;
      MessagingEndPoint endPoint = null;

      synchronized (this.endPointSelectionStrategyLock)
      {
         // If a specific destination was requested...
         if (destination != null)
         {
            while ((endPoint == null) && (waitTime > 0) && this.enabled)
            {
               waitTime = timeOut - (System.currentTimeMillis() - beginWait);

               if (waitTime > 0)
               {
                  // ...get the first available enpoint for that destination...
                  endPoint = this.getFirstMessagingEndPoint(destination);

                  if (endPoint == null)
                  {
                     // Wait for an endpoint to be ready (endPointReady called)
                     waitTime = timeOut - (System.currentTimeMillis() - beginWait);

                     if (waitTime > 0)
                     {
                        //if( this.messagingManager.isDebugMode() ) this.messagingManager.logDebug("DefaultEndPointSelectionStrategy - waiting for available endpoint in destination " + destination + ". Wait time: " + waitTime + ".");
                        
                        notifyNeeded++;
                        try{
                           this.endPointSelectionStrategyLock.wait(waitTime);
                        }finally{
                        notifyNeeded--;
                        }
                     }
                  }
                  /*else
                  {
                     if( this.messagingManager.isDebugMode() ) this.messagingManager.logDebug("DefaultEndPointSelectionStrategy - returning available endpoint: " + endPoint + ".");
                  }*/
               }
            }
         }
      }

      return endPoint;
   }

   /**
    * Gets the first available endpoint for the specified named receiver. This method will wait a maximum of
    * <code>timeOut</code> milliseconds for an available endpoint.<br>
    * <br>
    * Note: This method must never be called with a lock held on {@link MessagingManager#getEndpointGroupsLock()}.
    */
   public MessagingEndPoint getEndPoint(final String namedReceiver, long timeOut) throws InterruptedException
   {
      return getEndPointInternal(namedReceiver, null, timeOut);
   }

   /**
    * Gets the first available endpoint for the specified metadata. This method will wait a maximum of
    * <code>timeOut</code> milliseconds for an available endpoint.<br>
    * <br>
    * Note: This method must never be called with a lock held on {@link MessagingManager#getEndpointGroupsLock()}.
    */
   public MessagingEndPoint getEndPoint(final Map metaData, long timeOut) throws InterruptedException
   {
      return getEndPointInternal(null, metaData, timeOut);
   }

   /**
    * Internal method for getting an endpoint.
    */
   private MessagingEndPoint getEndPointInternal(final String namedReceiver, final Map metaData, final long timeOut)
         throws InterruptedException
   {
      final long beginWait = System.currentTimeMillis();
      long waitTime = timeOut;
      Destination[] destinations;
      MessagingEndPoint endPoint = null;
      long oldestEndPointReadyTime;
      long endPointReadyTime;
      Destination destination;
      MessagingEndPoint tmpEndPoint;
      boolean allDestinationsHaveLoad;
      int lowestLoad;
      int tmpLoad;

      synchronized (this.endPointSelectionStrategyLock)
      {
         while ((endPoint == null) && (waitTime > 0) && this.enabled)
         {
            waitTime = timeOut - (System.currentTimeMillis() - beginWait);
            
            oldestEndPointReadyTime = Long.MAX_VALUE;

            if (waitTime > 0)
            {
               // Get destinations matching meta data
               if (namedReceiver != null) destinations = messagingManager.getDestinations(namedReceiver);
               else destinations = messagingManager.getDestinations(metaData);

               if ((destinations != null) && (destinations.length > 0))
               {
                  destination = null;
                                    
                  if( destinations.length == 1 ) // If only one destination...
                  {
                     destination = destinations[0]; // ...go a head and use it...
                  }
                  else // ...otherwise, select an appropriate destination (load or with lowest lastReadyTime)
                  {
                     ArrayList sameLoadDestinations = null;
                     allDestinationsHaveLoad = true;
                     lowestLoad = Integer.MAX_VALUE;
                     
                     // Get destination with lowest load
                     for (int i = 0; i<destinations.length; i++)
                     {
                        if( destinations[i] != null )
                        {
   	                     tmpLoad = destinations[i].getLoad();
   	                     if( tmpLoad < 0 )
   	                     {
   	                        allDestinationsHaveLoad = false;
   	                        break;
   	                     }
   	                     else if( tmpLoad == lowestLoad )
   	                     {
   	                        if( sameLoadDestinations == null )
   	                        {
   	                           sameLoadDestinations = new ArrayList();
   	                           sameLoadDestinations.add(destination); // Add previous
   	                        }
   	                        sameLoadDestinations.add(destinations[i]);
   	                     }
   	                     else if( tmpLoad < lowestLoad )
   	                     {
   	                        lowestLoad = tmpLoad;
   	                        destination = destinations[i];
   	                        sameLoadDestinations = null;
   	                     }
                        }
                     }
                     
                     // If several destinations with same load, use logic below on these destinations...
                     if( sameLoadDestinations != null ) destinations = (Destination[])sameLoadDestinations.toArray(new Destination[0]);
                  
                     if( !allDestinationsHaveLoad || (destination == null) || (sameLoadDestinations != null) )
                     {
   	                  // Get destination containing endpoint with lowest lastReadyTime
   	                  for (int i = 0; i < destinations.length; i++)
   	                  {
   	                     tmpEndPoint = this.getFirstMessagingEndPoint(destinations[i], true);
   	                     if (tmpEndPoint != null)
   	                     {
   	                        endPointReadyTime = tmpEndPoint.getLastReadyTime();
   	
   	                        if (endPointReadyTime < oldestEndPointReadyTime)
   	                        {
   	                           oldestEndPointReadyTime = endPointReadyTime;
   	                           destination = destinations[i];
   	                        }
   	                     }
   	                  }
                     }
                  }

                  if (destination != null)
                  {
                     // Get (remove) end point
                     endPoint = this.getFirstMessagingEndPoint(destination, false);
                  }
               }

               if (endPoint == null)
               {
                  // Wait for an endpoint to be ready (endPointReady called)
                  waitTime = timeOut - (System.currentTimeMillis() - beginWait);

                  if (waitTime > 0)
                  {
                     //if( this.messagingManager.isDebugMode() ) this.messagingManager.logDebug("DefaultEndPointSelectionStrategy - waiting for available endpoint with receiver name '" + namedReceiver + "'. Wait time: " + waitTime + ".");
                     
                     notifyNeeded++;
                     try{
                        this.endPointSelectionStrategyLock.wait(waitTime);
                     }finally{
                     notifyNeeded--;
                     }
                  }
               }
               /*else
               {
                  if( this.messagingManager.isDebugMode() ) this.messagingManager.logDebug("DefaultEndPointSelectionStrategy - returning available endpoint: " + endPoint + ".");
               }*/
            }
         }

         return endPoint;
      }
   }

   /**
    * Internal method for getting an endpoint. Must always be called with a lock on endPointQueue.
    */
   private MessagingEndPoint getFirstMessagingEndPoint(final Destination destination) throws InterruptedException
   {
      return getFirstMessagingEndPoint(destination, false);
   }

   /**
    * Internal method for getting an endpoint. Must always be called with a lock on endPointQueue.
    */
   private MessagingEndPoint getFirstMessagingEndPoint(final Destination destination, final boolean peek)
         throws InterruptedException
   {
      MessagingEndPoint endPoint = null;

      if( destination != null )
      {
         // Remove from destination endpoint queue
         final ArrayList availableDestinationEndPoints = (ArrayList) this.destinationEndPointMap.get(destination.getKey());
            
         if ((availableDestinationEndPoints != null) && (availableDestinationEndPoints.size() > 0))
         {
            //if( availableDestinationEndPoints != null ) this.messagingManager.logDebug("DefaultEndPointSelectionStrategy - " + availableDestinationEndPoints.size() + " available endpoint(s) in destination " + destination + ".");
            
            if (peek) // Only look
            {
               // Get first in list of destination endpoints
               endPoint = (MessagingEndPoint) availableDestinationEndPoints.get(0);
            }
            else
            {
               // Remove first in list of destination endpoints
               endPoint = (MessagingEndPoint) availableDestinationEndPoints.remove(0);
   
               // Remove from global endpoint queue
               this.endPointQueue.remove(endPoint);
            }
         }
         /*else if( this.messagingManager.isDebugMode() )
         {
            this.messagingManager.logDebug("DefaultEndPointSelectionStrategy - no available endpoints in destination " + destination + ".");
         }*/
      }

      return endPoint;
   }
}
