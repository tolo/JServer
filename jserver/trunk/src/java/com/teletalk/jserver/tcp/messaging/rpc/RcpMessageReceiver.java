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
package com.teletalk.jserver.tcp.messaging.rpc;

import com.teletalk.jserver.tcp.messaging.MessagingManager;

/**
 * Message receiver implementation used for RPC communication. This class can be used as the 
 * server side end of RPC based communication. Objects of this class uses itself as default handler, which 
 * means that if no matching handler is found for an incomming RPC message attempts will be made to 
 * invoke the method on this object.<br>
 * <br>
 * Handlers are registered with a name, which is used when invoking a method in the client side. This means 
 * that if a handler is registered under the name <code>"myHandler"</code>, the client must specify the method name 
 * parameter as <code>"myHandler.myMethod"</code> to invoke the method <code>"myMethod"</code> in the handler
 * object.<br>
 * <br>
 * This class uses an {@link RpcHandler} object to handle the actual RPC logic.
 * 
 * @see RpcHandler
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.1 Build 690
 * 
 * @deprecated as of 2.1.4 (20060605), replaced by {@link com.teletalk.jserver.tcp.messaging.rpc.RpcMessageReceiver} due to bad spelling.
 */
public class RcpMessageReceiver extends RpcMessageReceiver
{
   /**
    * Creates a new RcpMessageReceiver.
    *  
    * @since 2.1 (20050823).
    */
   public RcpMessageReceiver()
   {
      this(null); 
   }
      
   /**
    * Creates a new RcpMessageReceiver.
    * 
    * @param messagingManager the MessagingManager in which this object will be used as an RPC message 
    *  
    * @since 2.1.
    */
   public RcpMessageReceiver(final MessagingManager messagingManager)
   {
      super(messagingManager);
   }
}
