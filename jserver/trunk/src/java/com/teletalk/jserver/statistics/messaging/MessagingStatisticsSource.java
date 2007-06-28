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
package com.teletalk.jserver.statistics.messaging;

import com.teletalk.jserver.statistics.AverageMaxStatisticsSource;
import com.teletalk.jserver.statistics.StatisticsEntry;
import com.teletalk.jserver.statistics.StatisticsSource;
import com.teletalk.jserver.tcp.messaging.MessageHeader;
import com.teletalk.jserver.tcp.messaging.MessagingEndPoint;
import com.teletalk.jserver.tcp.messaging.rpc.MessagingRpcInterface;
import com.teletalk.jserver.tcp.messaging.rpc.RemoteProcedureCall;

/**
 * Statistics source for messaging statistics, i.e sent data size , request-response time and received data size. This object 
 * contains three {@link com.teletalk.jserver.statistics.AverageMaxStatisticsSource} objects at the top level, one for each of the before mentioned 
 * statistics types. These AverageMaxStatisticsSource objects may in turn contain nested AverageMaxStatisticsSource objects for 
 * destination name and message type. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0.2
 */
public class MessagingStatisticsSource implements StatisticsSource
{
   static final long serialVersionUID = -7476534198137934334L;

   /** The name of the sent data size statistics sounce*/
   public static final String SENT_SOURCE_NAME = "Sent (data)";
   
   /** The name of the request-response time statistics sounce*/
   public static final String SEND_RECEIVE_SOURCE_NAME = "Request-response (time)";
     
   /** The name of the received data size statistics sounce*/
   public static final String RECEIVED_SOURCE_NAME = "Received (data)";
   
   
   /** The name of the average size statistics entry. */
   public static final String AVERAGE_SIZE_ENTRY_NAME = "avg size";
   
   /** The name of the maximum size statistics entry. */
   public static final String MAX_SIZE_ENTRY_NAME = "max size";
   
   /** The name of the average time statistics entry. */
   public static final String AVERAGE_TIME_ENTRY_NAME = "avg time";
   
   /** The name of the maximum time statistics entry. */
   public static final String MAX_TIME_ENTRY_NAME = "max time";
   
   
   private static final String[] SourceNames = new String[]{SENT_SOURCE_NAME, SEND_RECEIVE_SOURCE_NAME, RECEIVED_SOURCE_NAME};
   
   
   private final AverageMaxStatisticsSource sentStatistics;
   
   private final AverageMaxStatisticsSource requestResponseStatistics;
   
   private final AverageMaxStatisticsSource receivedStatistics;
   
   /**
    * Creates a new MessagingStatisticsSource.
    */
   public MessagingStatisticsSource()
   {
      this.sentStatistics = new AverageMaxStatisticsSource(true, AVERAGE_SIZE_ENTRY_NAME, MAX_SIZE_ENTRY_NAME);
      this.sentStatistics.setSuffix(" bytes");
      this.requestResponseStatistics = new AverageMaxStatisticsSource(true, AVERAGE_TIME_ENTRY_NAME, MAX_TIME_ENTRY_NAME);
      this.requestResponseStatistics.setSuffix(" ms");
      this.receivedStatistics = new AverageMaxStatisticsSource(true, AVERAGE_SIZE_ENTRY_NAME, MAX_SIZE_ENTRY_NAME);
      this.receivedStatistics.setSuffix(" bytes");
   }
   
   
   /* ### STATISTICSSOURCE METHODS BEGIN ### */
   
   
   /**
    * Gets the names of the StatisticsSource object contained in this StatisticsSource object.
    */
   public String[] getStatisticsSourceNames()
   {
      return SourceNames;
   }
   
   /**
    * Gets the StatisticsSource object with the specified name contained in this StatisticsSource object.
    */
   public StatisticsSource getStatisticsSource(String name)
   {
      if(  SENT_SOURCE_NAME.equals(name) ) return sentStatistics;
      else if(  SEND_RECEIVE_SOURCE_NAME.equals(name) ) return requestResponseStatistics;
      else if(  RECEIVED_SOURCE_NAME.equals(name) ) return receivedStatistics;
      else return null;
   }
   
   /**
    * Gets the names of the {@link StatisticsEntry} object contained in this StatisticsSource object.
    */
   public String[] getStatisticsEntryNames()
   {
      return null;
   }
   
   /**
    * Gets the {@link StatisticsEntry} object with the specified name contained in this StatisticsSource object.
    */
   public StatisticsEntry getStatisticsEntry(String name)
   {
      return null;
   }
   
   /**
    * Resets all the {@link StatisticsEntry} objects contained in this object. This call will cascade to all the 
    * StatisticsSource objects contained in this object.
    * 
    * @see StatisticsEntry#reset() 
    */
   public void reset()
   {
      this.sentStatistics.reset();
      this.requestResponseStatistics.reset();
      this.receivedStatistics.reset();
   }
   
   
   /* ### STATISTICSSOURCE METHODS END ### */

   
   /**
    * Update the sent data size statistics.
    */   
   public void updateSentStatistics(final MessagingEndPoint endPoint, final MessageHeader header)
   {
      this.updateStatistics(this.sentStatistics, endPoint, header, header.getBodyLength());
   }
   
   /**
    * Update the request-response time statistics.
    */   
   public void updateRequestResponseStatistics(final MessagingEndPoint endPoint, final MessageHeader header, final long time)
   {
      this.updateStatistics(this.requestResponseStatistics, endPoint, header, time);
   }
   
   /**
    * Update the received data size statistics.
    */   
   public void updateReceivedStatistics(final MessagingEndPoint endPoint, final MessageHeader header)
   {
      this.updateStatistics(this.receivedStatistics, endPoint, header, header.getBodyLength());
   }
   
   /**
    * Internal method to update statistics.
    */   
   private void updateStatistics(final AverageMaxStatisticsSource topStatisticsSource, final MessagingEndPoint endPoint, final MessageHeader header, final long value)
   {
      final int headerType = header.getHeaderType();
      
      if(   (headerType != MessageHeader.META_DATA_UPDATE_HEADER) &&  
            (headerType != MessageHeader.ENDPOINT_CHECK_HEADER) &&
            (headerType != MessageHeader.DISCONNECT_HEADER) &&
            (headerType != MessageHeader.MESSAGE_PROCESSING_ERROR_HEADER) )
      {
         String destination = endPoint.getDestination().getName();
         String messageType = "";
         if( header.getResponseToId() != MessageHeader.UNDEFINED ) messageType = "Response ";
         
         if( headerType == MessageHeader.SERVER_ADMINISTRATION_HEADER )
         {
            messageType += "Admin - " + RemoteProcedureCall.getRPCMethodName(header);
         }
         else if( headerType == MessageHeader.RPC_HEADER )
         {
            if( header.getMessageType() == MessagingRpcInterface.RPC_MESSAGE_TYPE_ID ) messageType += "RPC - " + RemoteProcedureCall.getRPCMethodName(header);
            else messageType += "RPC (" + header.getMessageType() + ") - " + RemoteProcedureCall.getRPCMethodName(header);
         }
         else messageType += "Msg type " + header.getMessageType();
         
         AverageMaxStatisticsSource destinationStatistics = null;
         AverageMaxStatisticsSource messageTypeStatistics = null;
         
         synchronized(topStatisticsSource)
         {
            topStatisticsSource.update(value);
            
   	      destinationStatistics = (AverageMaxStatisticsSource)topStatisticsSource.getStatisticsSource(destination);
   	      if( destinationStatistics == null )
   	      {
   	         destinationStatistics = new AverageMaxStatisticsSource(true, topStatisticsSource.getAverageEntryName(), topStatisticsSource.getMaxEntryName());
               destinationStatistics.setSuffix(topStatisticsSource.getSuffix());
   	         topStatisticsSource.addSource(destination, destinationStatistics);
   	      }
         }
   	     
         synchronized(destinationStatistics)
         {
            destinationStatistics.update(value);
            
   	      messageTypeStatistics = (AverageMaxStatisticsSource)destinationStatistics.getStatisticsSource(messageType);
   	      if( messageTypeStatistics == null )
   	      {
   	         messageTypeStatistics = new AverageMaxStatisticsSource(false, topStatisticsSource.getAverageEntryName(), topStatisticsSource.getMaxEntryName());
               messageTypeStatistics.setSuffix(topStatisticsSource.getSuffix());
   	         destinationStatistics.addSource(messageType, messageTypeStatistics);
   	      }
         }
         
         messageTypeStatistics.update(value);
      }
   }

   /**
    * Gets the {@link AverageMaxStatisticsSource} object representing the top level sent data size statistics.
    */
   public AverageMaxStatisticsSource getSentStatistics()
   {
      return this.sentStatistics;
   }
   
   /**
    * Gets the {@link AverageMaxStatisticsSource} object representing the top level request-response time statistics.
    */
   public AverageMaxStatisticsSource getRequestResponseStatistics()
   {
      return this.requestResponseStatistics;
   }
   
   /**
    * Gets the {@link AverageMaxStatisticsSource} object representing the top level received data size statistics.
    */
   public AverageMaxStatisticsSource getReceivedStatistics()
   {
      return this.receivedStatistics;
   }
}
