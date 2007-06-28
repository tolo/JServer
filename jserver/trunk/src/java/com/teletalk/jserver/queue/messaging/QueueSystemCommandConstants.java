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
package com.teletalk.jserver.queue.messaging;

import java.util.HashMap;

import com.teletalk.jserver.queue.command.MultiQueueItemTransferRequest;
import com.teletalk.jserver.queue.command.MultiQueueItemTransferResponse;
import com.teletalk.jserver.queue.command.QueueControllerCommand;
import com.teletalk.jserver.queue.command.QueueItemCancellationRequest;
import com.teletalk.jserver.queue.command.QueueItemCompletionResponse;
import com.teletalk.jserver.queue.command.QueueItemQuery;
import com.teletalk.jserver.queue.command.QueueItemRelocationRequest;
import com.teletalk.jserver.queue.command.QueueItemTransferRequest;
import com.teletalk.jserver.queue.command.QueueItemTransferResponse;

/**
 * Constant class for QueueSystemCommand related constants.
 *
 * @author Tobias Löfstrand
 * 
 * @since 2.1.5 (20070402)
 */
public final class QueueSystemCommandConstants
{
   public static final int QUEUE_ITEM_TRANSFER_REQUEST_MESSAGE_TYPE = 1000;
   
   public static final int QUEUE_ITEM_TRANSFER_RESPONSE_MESSAGE_TYPE = 2000;
   
   public static final int MULTI_QUEUE_ITEM_TRANSFER_REQUEST_MESSAGE_TYPE = 1001;
   
   public static final int MULTI_QUEUE_ITEM_TRANSFER_RESPONSE_MESSAGE_TYPE = 2001;
   
        
   public static final int QUEUE_ITEM_CANCELLATION_REQUEST_MESSAGE_TYPE = 1100;
   
   public static final int QUEUE_ITEM_RELOCATION_REQUEST_MESSAGE_TYPE = 1101;
   
   public static final int QUEUE_ITEM_QUERY_MESSAGE_TYPE = 1110;
   
   
   public static final int QUEUE_ITEM_COMPLETION_RESPONSE_MESSAGE_TYPE = 2100;
   
   
   public static final int QUEUE_CONTROLLER_COMMAND_MESSAGE_TYPE = 1500;
   
   
   public static final int QUEUE_SYSTEM_COMMAND_MESSAGE_TYPE = 1999;
   
   
   private static final HashMap queueSystemClassToCode = new HashMap();
   
   static
   {
      queueSystemClassToCode.put(QueueItemTransferRequest.class, new Integer(QUEUE_ITEM_TRANSFER_REQUEST_MESSAGE_TYPE));
      queueSystemClassToCode.put(QueueItemTransferResponse.class, new Integer(QUEUE_ITEM_TRANSFER_RESPONSE_MESSAGE_TYPE));
      queueSystemClassToCode.put(MultiQueueItemTransferRequest.class, new Integer(MULTI_QUEUE_ITEM_TRANSFER_REQUEST_MESSAGE_TYPE));
      queueSystemClassToCode.put(MultiQueueItemTransferResponse.class, new Integer(MULTI_QUEUE_ITEM_TRANSFER_RESPONSE_MESSAGE_TYPE));
      
      queueSystemClassToCode.put(QueueItemCancellationRequest.class, new Integer(QUEUE_ITEM_CANCELLATION_REQUEST_MESSAGE_TYPE));
      queueSystemClassToCode.put(QueueItemRelocationRequest.class, new Integer(QUEUE_ITEM_RELOCATION_REQUEST_MESSAGE_TYPE));
      queueSystemClassToCode.put(QueueItemQuery.class, new Integer(QUEUE_ITEM_QUERY_MESSAGE_TYPE));
      
      queueSystemClassToCode.put(QueueItemCompletionResponse.class, new Integer(QUEUE_ITEM_COMPLETION_RESPONSE_MESSAGE_TYPE));
      
      queueSystemClassToCode.put(QueueControllerCommand.class, new Integer(QUEUE_CONTROLLER_COMMAND_MESSAGE_TYPE));
   }
   
   
   /**
    * Gets the appropriate message type for the specified QueueSystemCommand class. 
    */
   public static int getQueueSystemCommandMessageType(final Class queueSystemCommandClass)
   {
      Integer code = (Integer)queueSystemClassToCode.get(queueSystemCommandClass);
      if( code != null ) return code.intValue();
      else return QUEUE_SYSTEM_COMMAND_MESSAGE_TYPE; // Default to common code for all queue system commands
   }
}
