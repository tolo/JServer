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
package com.teletalk.jserver.net.sns.client;

import com.teletalk.jserver.net.sns.server.SnsServer;
import com.teletalk.jserver.tcp.messaging.Destination;
import com.teletalk.jserver.tcp.messaging.MessageHeader;
import com.teletalk.jserver.tcp.messaging.MessagingManager;
import com.teletalk.jserver.tcp.messaging.admin.ServerAdministrationClient;
import com.teletalk.jserver.tcp.messaging.rpc.MessagingRpcInterface;

/**
 * Client implementation of RPC based messaging remote administration of JServer via an SNS.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1.4 (20060511)
 */
public class SnsServerAdministrationClient extends ServerAdministrationClient
{
   /**
    * Creates an SnsServerAdministrationClient.
    */
   public SnsServerAdministrationClient(MessagingManager messagingManager, Destination remoteServer, SnsServer currentSns)
   {
      super(messagingManager, remoteServer);
      this.initMessagingRpcInterface(currentSns);
   }

   /**
    * Creates an SnsServerAdministrationClient.
    */
   public SnsServerAdministrationClient(MessagingRpcInterface messagingRpcInterface, String rpcHandlerName, SnsServer currentSns)
   {
      super(messagingRpcInterface, rpcHandlerName);
      this.initMessagingRpcInterface(currentSns);
   }

   /**
    * Creates an SnsServerAdministrationClient.
    */
   public SnsServerAdministrationClient(MessagingRpcInterface messagingRpcInterface, SnsServer currentSns)
   {
      super(messagingRpcInterface);
      this.initMessagingRpcInterface(currentSns);
   }
   
   private void initMessagingRpcInterface(final SnsServer currentSns)
   {
      int snsServerInterfaceVersion = 1;
      try{
         snsServerInterfaceVersion = currentSns.getInterfaceVersion();
      }catch(Exception e){}
      
      if( snsServerInterfaceVersion < 2 )
      {
         MessageHeader prototypeHeader = super.getMessagingRpcInterface().getMessageDispatcher().getPrototypeMessageHeader();
         if( prototypeHeader == null ) prototypeHeader = new MessageHeader();
         prototypeHeader.setHeaderType(MessageHeader.RPC_HEADER);
         this.messagingRpcInterface.getMessageDispatcher().setPrototypeMessageHeader(prototypeHeader);
      }
   }
}
