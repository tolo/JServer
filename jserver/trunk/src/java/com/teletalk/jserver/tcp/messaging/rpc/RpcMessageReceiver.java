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

import java.util.Map;

import com.teletalk.jserver.tcp.messaging.Message;
import com.teletalk.jserver.tcp.messaging.MessagingManager;
import com.teletalk.jserver.tcp.messaging.MessagingManagerAwareMessageReceiver;

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
 * @since 2.1.4 (20060605) (replaces RcpMessageReceiver introduced in version 1.3.1 Build 690).
 */
public class RpcMessageReceiver implements MessagingManagerAwareMessageReceiver
{
   private MessagingManager messagingManager;
   
   private final RpcHandler rpcHandler;
   
   /**
    * Creates a new RpcMessageReceiver.
    *  
    * @since 2.1 (20050823).
    */
   public RpcMessageReceiver()
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
   public RpcMessageReceiver(final MessagingManager messagingManager)
   {
      this.messagingManager = messagingManager;
      
      this.rpcHandler = new RpcHandler(messagingManager);
      this.rpcHandler.setDefaultHandler(this);
   }
   
   /**
    * Gets the associated MessagingManager.
    * 
    * @since 2.1 (20050823).
    */
   public MessagingManager getMessagingManager()
   {
      return messagingManager;
   }
   
   /**
    * Sets the associated MessagingManager.
    * 
    * @since 2.1 (20050823).
    */
   public void setMessagingManager(MessagingManager messagingManager)
   {
      this.messagingManager = messagingManager;
      
      this.rpcHandler.setMessagingManager(this.messagingManager);
   }
   
   /**
    * Gets the {@link RpcHandler} object used by this RcpMessageReceiver.
    */
   public RpcHandler getRpcHandler()
   {
      return rpcHandler;
   }
   
   /**
    * Called by the MessagingManager to notify this MessageReceiver that a new message has arrived 
    * from a remote messaging system. The implementation of this method will attempt to handle the message 
    * as an incomming RPC message ({@link RemoteProcedureCall}).
    * 
    * @param message the received message.
    */
   public void messageReceived(final Message message)
   {
      RpcHandler.defaultHandleRpcMessage(message, this.rpcHandler, this.messagingManager, this.messagingManager);
   }
   
   
   /* ### DELEGATE METHODS ### */
   
   
   /**
    * Registers a handler for RPC methods.
    * 
    * @param name the name of the handler.
    * @param handler the handler.
    */
   public void addHandler(final String name, final Object handler)
   {
      this.rpcHandler.addHandler(name, handler);
   }
   
   /**
    * Gets a handler for RPC methods.
    * 
    * @param name the name of the handler to get.
    */
   public Object getHandler(String name)
   {
      return this.rpcHandler.getHandler(name);
   }

   /**
    * Removes a handler for RPC methods.
    * 
    * @param name the name of the handler to remove.
    */
   public Object removeHandler(String name)
   {
      return this.rpcHandler.removeHandler(name);
   }

   /**
    * Gets the default RPC method handler.
    */
   public Object getDefaultHandler()
   {
      return this.rpcHandler.getDefaultHandler();
   }

   /**
    * Sets the default RPC method handler.
    */
   public void setDefaultHandler(Object object)
   {
      this.rpcHandler.setDefaultHandler(object);
   }
   
   /**
    * Convenience to set the RCP handler mappings in the {@link RpcHandler} of this component, removing the previous handler mappings.
    * One of the reasons why this method exists here is to facilitate configuration when using Spring.
    * 
    * @param handlers the new handler mappings (i.e. String to Object mappings). 
    * 
    * @since 2.1.5 (20070301)
    * 
    * @see RpcHandler#setHandlers(Map)
    */
   public void setRpcHandlerMappings(final Map handlers)
   {
      this.rpcHandler.setHandlers(handlers); 
   }
   
   /**
    * Convenience to get the RPC handler mappings in the {@link RpcHandler} of this component.
    * One of the reasons why this method exists here is to facilitate configuration when using Spring.
    * 
    * @since 2.1.5 (20070301)
    * 
    * @see RpcHandler#setHandlers(Map)
    */
   public Map getRpcHandlerMappings()
   {
      return this.rpcHandler.getHandlers();
   }
}
