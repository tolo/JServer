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

import java.util.HashMap;
import java.util.Map;

import com.teletalk.jserver.load.LoadManager;
import com.teletalk.jserver.util.Future;

/**
 * Default message dispatch implementation. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20050517)
 */
public class DefaultMessageDispatchHandler implements MessageDispatchHandler
{
   private MessagingManager messagingManager;
   
   private final HashMap pendingSynchronousMessages;
   private final Object synchMessageMonitor;
   
   /** Message id counter. */
   private long uniqueIdCounter = 0;
   
   /**
    * Creates a DefaultMessageDispatchHandler.
    */
   public DefaultMessageDispatchHandler()
   {
      this.pendingSynchronousMessages = new HashMap();
      this.synchMessageMonitor = this;
   }
      
   /**
    * Gets the MessagingManager associated with this handler.
    */
   public MessagingManager getMessagingManager()
   {
      return messagingManager;
   }
   
   /**
    * Sets the MessagingManager associated with this handler.
    */
   public void setMessagingManager(MessagingManager messagingManager)
   {
      this.messagingManager = messagingManager;
   }
   
   /**
    * Called by the associated MessagingManager to initialize this handler. This method is called when the MessagingManager 
    * is starting up, after {@link #setMessagingManager(MessagingManager)} has been called.
    */
   public void initialize()
   {
   }
   
   /**
    * Called by the associated MessagingManager to shut down this handler. This method is called when the MessagingManager 
    * is shutting down. 
    */
   public void shutDown()
   {
      synchronized(this.synchMessageMonitor)
      {
         this.pendingSynchronousMessages.clear();
      }
   }
      
   /**
    * Internal method to create a new unique id for a message.
    */
   private long createUniqueId()
   {
      synchronized(this.synchMessageMonitor)
      {
         long unqueId = uniqueIdCounter++;
         if (unqueId == MessageHeader.UNDEFINED) unqueId = uniqueIdCounter++;
      
         return unqueId;
      }
   }

   /**
    * Creates a future object for waiting for a response to a synchronous message.
    */
   private Future createFuture(final long id)
   {
      synchronized(this.synchMessageMonitor)
      {
         Future future = new Future();
         this.pendingSynchronousMessages.put(new Long(id), future);
   
         return future;
      }
   }

   /**
    * Removes a future object used for waiting for a response to a synchronous message.
    */
   private void removeFuture(final long id)
   {
      synchronized(this.synchMessageMonitor)
      {
         this.pendingSynchronousMessages.remove(new Long(id));
      }
   }
   
   /**
    * Checks if the specified message is a response to a message previously dispatched through 
    * {@link #dispatchMessage(MessageHeader, MessageWriter, MessageDispatcherProperties, boolean)}, 
    * and if so notify the thread that is waiting for the response.
    * 
    * @return <code>true</code> if the specified message was a response to a message send through this object, otherwise <code>false</code>.
    */
   public boolean responseReceived(final Message message) throws Exception
   {
      long responseToId = message.getHeader().getResponseToId();
      Future future;

      synchronized(this.synchMessageMonitor)
      {
         future = (Future) this.pendingSynchronousMessages.get(new Long(responseToId));
      }
      
      if (future != null) // If a client is waiting for a reply...
      {
         synchronized (future)
         {
            future.setValue(message); // ...set the future...
            return !future.isCancelled();
         }
      }
      else
      {
         return false;
      }
   }
   
   /**
    * Method for performing the actual dispatching of a message. This method handles the dispatching of both synchronous and 
    * asynchronous message. For synchronous messages, this methods will block while waiting for a response message. 
    * The maximum time this method will block is determined by the timeout value of the messageDispatcherProperties  
    * object or the timeouts defined in MessagingManager by {@link MessagingManager#getResponseMessageTimeOut()} or 
    * {@link MessagingManager#getThreadResponseMessageTimeOut()}. When a response message is received, the MessagingManager 
    * will call the {@link #responseReceived(Message)} to notify this object that a response is received.<br>
    * <br>
    * This method automatically sets the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageDispatchImpl the implementation to be used for dispatching the message body.
    * @param messageDispatcherProperties a {@link MessageDispatcherProperties} object containing properties for message dispatch.
    * @param proxyMessage flag indicating if the message to be dispatched is a proxied message, which means that this method should never wait for a 
    * response even if the message isn't asych.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    */
   public Message dispatchMessage(MessageHeader header, MessageWriter messageDispatchImpl, 
         MessageDispatcherProperties messageDispatcherProperties, boolean proxyMessage)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      Destination destination = messageDispatcherProperties.getDestination();
      String namedReceiver = messageDispatcherProperties.getNamedReceiver();
      final Map destinationMetaDataConstraints = messageDispatcherProperties.getDestinationMetaDataConstraints();
      long timeOut = messageDispatcherProperties.getTimeout();
      final boolean asynch = messageDispatcherProperties.isAsynch(); 
      
      EndPointSelectionStrategy endPointSelectionStrategy = messagingManager.getEndPointSelectionStrategy();
      
      long messageId = -1;
      Message response = null;
      Future future = null;
      MessagingEndPoint endPoint = null;
      final long startTime = System.currentTimeMillis();
      boolean messageDispatched = false;

      if (header == null) header = new MessageHeader();
      else
      {
         // Reset obsolete fields (to eliminate confusing log statements)
         header.setBodyLength(MessageHeader.UNDEFINED);
         header.setTimeToLive(MessageHeader.UNDEFINED);
      }

      if (header.getHeaderType() != MessageHeader.META_DATA_UPDATE_HEADER)
      {
         // Piggy-back load
         LoadManager loadManager = LoadManager.getLoadManager();
         if (loadManager != null)
         {
            HashMap partialMetaData = header.getMessagingSystemMetaData();
            if (partialMetaData == null) partialMetaData = new HashMap();
            partialMetaData.put(MessagingManager.SERVER_LOAD_METADATA_KEY, new Integer(loadManager.getLoad()));
            header.setMessagingSystemMetaData(partialMetaData);
         }
      }

      if (timeOut <= 0)
      {
         // Get thread local timeout first, if set
         long timeOutLong = messagingManager.getThreadResponseMessageTimeOut();
         if (timeOutLong >= 0) timeOut = timeOutLong;
      }
      if (timeOut <= 0) timeOut = messagingManager.getResponseMessageTimeOut();

      // Attempt to get destination from sender id field of the header (if the header was received in an incomming
      // message, and a reply is to be sent)
      if ((destination == null) && (header.getSenderId() != MessageHeader.UNDEFINED))
      {
         destination = messagingManager.getDestination(header.getSenderId());
      }

      // If a sender id exists in the header, this is a response message - i.e. set the response to field
      if ( (header.getSenderId() != MessageHeader.UNDEFINED) && (header.getMessageId() != MessageHeader.UNDEFINED) )
      {
         if( destination == null ) // Check if reply destination is valid
         {
            messagingManager.logError("Attempt to dispatch reply message failed - invalid sender id! Message header : " + header + ".");
            throw new MessageDispatchFailedException("Attempt to dispatch reply message failed - invalid sender id! Message header : " + header + ".");            
         }
         
         header.setResponseToId(header.getMessageId());
      }

      // Check if destination meta data constraints have been specified, and a named reciever has been specified - if
      // so, set that field as a custom field in the message header.
      if (namedReceiver != null)
      {
         header.setCustomHeaderField(MessagingManager.NAMED_MESSAGE_RECEIVER_METADATA_KEY, namedReceiver);
      }
      else if (destinationMetaDataConstraints != null)
      {
         namedReceiver = (String) destinationMetaDataConstraints.get(MessagingManager.NAMED_MESSAGE_RECEIVER_METADATA_KEY);
         if (namedReceiver != null)
         {
            header.setCustomHeaderField(MessagingManager.NAMED_MESSAGE_RECEIVER_METADATA_KEY, namedReceiver);
         }
      }

      // If debug mode and description not set....
      if ((messagingManager.isDebugMode()) && (header.getDescription() == null))
      {
         // ... set it!
         String description = messageDispatchImpl.getDescription();
         if (description != null)
         {
            // Limit description length to 150 chars
            description = (description.length() >= 150) ? description.substring(0, 150) + "..." : description;
         }

         header.setDescription(description);
      }

      header.setAsynch(asynch);

      try
      {
         synchronized (this.synchMessageMonitor)
         {
            // Create unique id for message
            messageId = this.createUniqueId();

            if( !asynch && !proxyMessage )
            {
               // Create a future used to wait for a response to the message
               future = this.createFuture(messageId);
            }
         }

         header.setMessageId(messageId);
         
         // Invoke custom message (header) initialization
         messagingManager.initOutgoingMessage(header);
         
         if( messagingManager.isDebugMode() )
         {
            if (destination != null) messagingManager.logDebug("Attempting to dispatch message with header " + header + " to " + destination.getDescription() + ".");
            else if ( namedReceiver != null ) messagingManager.logDebug("Attempting to dispatch message with header " + header + " to named receiver " + namedReceiver + ".");
            else messagingManager.logDebug("Attempting to dispatch message with header " + header + ".");
         }
         
         while (!messageDispatched && (timeOut > 0))
         {
            try
            {
               endPoint = null;
               
               // Find an endpoint...
               if (destination != null) // ...for a specific destination...
               {
                  endPoint = endPointSelectionStrategy.getEndPoint(destination, timeOut);
               }
               else if (namedReceiver != null) // ...or with a specific named receiver...
               {
                  endPoint = endPointSelectionStrategy.getEndPoint(namedReceiver, timeOut);
               }
               else if (destinationMetaDataConstraints != null) // ...or with a specific set of meta data...
               {
                  endPoint = endPointSelectionStrategy.getEndPoint(destinationMetaDataConstraints, timeOut);
               }
               else // ...or any available endpoint.
               {
                  endPoint = endPointSelectionStrategy.getEndPoint(timeOut);
               }

               if ((endPoint != null) && endPoint.isLinkEstablished())
               {
                  timeOut = timeOut - (System.currentTimeMillis() - startTime);
   
                  if (timeOut > 0)
                  {
                     try
                     {
                        if (!asynch)
                        {
                           header.setTimeToLive(timeOut);
                        }
                        
                        messagingManager.beforeMessageDispatch(header, endPoint);
   
                        // Dispatch message through endpoint
                        endPoint.dispatchMessage(header, messageDispatchImpl);
                        messageDispatched = true;
                        
                        if (messagingManager.isDebugMode()) messagingManager.logDebug("Dispatched message with header: " + header + "!");
                     }
                     catch (MessageDispatchFailedException mdfe)
                     {
                        if (!mdfe.reDispatchPossible())
                        {
                           messagingManager.logWarning("Unable to dispatch message - redispatch not possible! Message header: " + header + ".",
                                 mdfe);
                           throw new MessageDispatchFailedException("Unable to dispatch message! Reason: " + mdfe.toString() + ". Message header: " + header + ".");
                        }
                        else
                        {
                           messagingManager.logWarning("Unable to dispatch message - attempting redispatch! Message header: " + header + ".", mdfe);
                        }
                     }
                  }
               }
               else if (endPoint == null)
               {
                  if (destination != null)
                  {
                     if (messagingManager.isDebugMode())
                     {
                        messagingManager.logDebug("No endpoints available for destination (" + destination
                                 + ")! Unable to dispatch message with header " + header + "!");
                     }
                     throw new MessageDispatchFailedException("No endpoints available for destination (" + destination
                           + ")! Unable to dispatch message with header " + header + "!");
                  }
                  else
                  {
                     if (messagingManager.isDebugMode())
                     {
                        messagingManager.logDebug("No endpoints available! Unable to dispatch message with header " + header + "!");
                     }
                     throw new MessageDispatchFailedException(
                           "No endpoints available! Unable to dispatch message with header " + header + "!");
                  }
               }
            }
            catch (InterruptedException ie)
            {
               messagingManager.logWarning("Unable to dispatch message! Caught InterruptedException while waiting for an available endpoint! Message header: " + header + ".", ie);
               throw new MessageDispatchFailedException("Interrupted while waiting for an available endpoint!", ie);
            }
            finally
            {
               // Make endpoint avaiable for message dispatch again
               if( endPoint != null ) endPointSelectionStrategy.endPointReady(endPoint);
               
               if ( messageDispatched ) messagingManager.messageDispatched(header, endPoint);
            }
         }

         if (messageDispatched)
         {
            if ( !asynch && !proxyMessage )
            {
               // Wait for response
               timeOut = timeOut - (System.currentTimeMillis() - startTime);
               synchronized (future)
               {
                  try
                  {
                     response = (Message) future.getValue(timeOut);
                  }
                  catch (InterruptedException ie)
                  {
                     messagingManager.logWarning("Caught InterruptedException while waiting for a response to message with header "
                           + header + ", dispatched on endpoint " + endPoint + ", " + endPoint.getDestination() + "!", ie);
                     throw new ResponseTimeOutException("Interrupted while waiting for a response to message with header "
                           + header + "!", ie);
                  }
                  finally
                  {
                     future.setCancelled(true);
                  }
               }

               if ((response == null) || (response.isConsumed())) // Timeout
               {
                  if (messagingManager.isDebugMode())
                  {
                     messagingManager.logDebug("Timeout occurred while waiting for a response to message with header " + header
                           + ", dispatched on endpoint " + endPoint + ", " + endPoint.getDestination() + "!");
                  }
                  throw new ResponseTimeOutException("Timeout occurred while waiting for a response to message with header " + header + "!");
               }
               else
               {
                  if( response.getHeader().isAccessDeniedHeader() )
                  {
                     String errorMessage = response.getHeader().getDescription();
                     if( (errorMessage == null) || (errorMessage.trim().length() == 0) ) errorMessage = "";
                     else errorMessage = " - " + errorMessage; 
                     
                     if (messagingManager.isDebugMode())
                     {
                        messagingManager.logDebug("Access denied response to message with header " + header + " received after " + (System.currentTimeMillis() - startTime) + " ms. Response header: " + response.getHeader() + ".");
                     }
                     
                     throw new MessageDispatchFailedException("Access denied response for message with header " + header + errorMessage + ".", response.getHeader());
                  }
                  else if( response.getHeader().isMessageProcessingErrorHeader() )
                  {
                     String errorMessage = response.getHeader().getDescription();
                     if( (errorMessage == null) || (errorMessage.trim().length() == 0) ) errorMessage = "";
                     else errorMessage = " - " + errorMessage;
                     
                     if (messagingManager.isDebugMode())
                     {
                        messagingManager.logDebug("Error occurred in remote message receiver while processing message with header " + header + errorMessage + ". Response received after " + (System.currentTimeMillis() - startTime) + " ms.");
                     }
                     
                     throw new MessageDispatchFailedException("Error occurred in remote message receiver while processing message with header " + header + " - " + errorMessage + ".", response.getHeader());
                  }
                  else // Success
                  {
                     if (messagingManager.isDebugMode())
                     {
                        if( response.getHeader().isAccessDeniedHeader() ) messagingManager.logDebug("Access denied response to message with header " + header + " received after " + (System.currentTimeMillis() - startTime) + " ms. Response header: " + response.getHeader() + ".");
                        else messagingManager.logDebug("Response to message with header " + header + " received after " + (System.currentTimeMillis() - startTime) + " ms. Response header: " + response.getHeader() + ".");
                     }
                     
                     // Call initMessage to enable subclasses to perform optional message initialization before returning the message
                     messagingManager.initResponseMessage(response);
                  }
               }
            }
         }
         else
         {
            if (messagingManager.isDebugMode())
            {
               messagingManager.logDebug("Timeout occurred before message with header " + header + " could be dispatched!");
            }
            throw new MessageDispatchFailedException("Timeout occurred before message with header " + header
                  + " could be dispatched!");
         }
      }
      finally
      {
         if ( !asynch && !proxyMessage )
         {
            // Remove ResponseMessage from map
            this.removeFuture(messageId);
         }
      }

      return response;
   }
}
