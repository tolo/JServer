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

/*
 TODO: Cache Method objects for increased performance, possibly build util class for this.
 */
package com.teletalk.jserver.tcp.messaging.rpc;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Level;

import com.teletalk.jserver.JServerUtilities;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.tcp.messaging.Message;
import com.teletalk.jserver.tcp.messaging.MessageDispatchFailedException;
import com.teletalk.jserver.tcp.messaging.MessageHeader;
import com.teletalk.jserver.tcp.messaging.MessagingManager;
import com.teletalk.jserver.util.ReflectionUtils;
import com.teletalk.jserver.util.ThrowableUtils;
import com.teletalk.jserver.util.exception.InvocationExceptionTranslator;

/**
 * Class providing the server side interface for messaging based RPC communication. Objects of this class uses 
 * itself as default handler, which means that if no matching handler is found for an incomming RPC message 
 * attempts will be made to invoke the method on this object.<br>
 * <br>
 * Handlers are registered with a name, which is used when invoking a method in the client side. This means 
 * that if a handler is registered under the name <code>"myHandler"</code>, the client must specify the method name 
 * parameter as <code>"myHandler.myMethod"</code> to invoke the method <code>"myMethod"</code> in the handler
 * object. Only the last dot separator is used to determine the method name, which means that handlers names may contain dot themselves.<br>
 * <br>
 * Use {@link #handleRpcMessage(Message)} or {@link #executeMethodCall(String, Object[])} to execute 
 * RPC methods.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.1 Build 690
 */
public class RpcHandler
{
   private static final ThreadLocal currenRpcResponseHeader = new ThreadLocal();
   
   /**
    * Gets the header that will be used for sending a response to RPC message currently beeing processed by the current thread. 
    *  
    * @since 2.1.6 (20070508)
    */
   public static MessageHeader getCurrenRpcResponseHeader()
   {
      return (MessageHeader)currenRpcResponseHeader.get();
   }
         
   
   private final HashMap handlerMap;
   
   private Object defaultHandler;
   
   private MessagingManager messagingManager;
   
   private InvocationExceptionTranslator invocationExceptionTranslator;   
   
   /**
    * Creates a new RpcHandler associated with the specied MessagingManager.
    * 
    * @since 2.1 (20050422)
    */
   public RpcHandler()
   {
      this(null);
   }
   
   /**
    * Creates a new RpcHandler associated with the specied MessagingManager.
    * 
    * @param messagingManager the MessagingManager which will be used for sending RPC responses. If this 
    * object is not going to be used for sending responses (i.e. if the methods {@link #handleRpcMessage(Message)} or 
    * {@link #dispatchRemoteProcedureCallResponse(MessageHeader, RemoteProcedureCallResponse)} are 
    * not to be used), <code>null</code> may be specified.
    */
   public RpcHandler(final MessagingManager messagingManager)
   {
      this.handlerMap = new HashMap();
      
      this.defaultHandler = this;
      
      this.messagingManager = messagingManager;
   }
      
   /**
    * Gets the {@link MessagingManager} associated with this RpcHandler.
    * 
    * @since 2.0 Build 757.
    */
   public MessagingManager getMessagingManager()
   {
      return messagingManager;
   }
   
   /**
    * Sets the {@link MessagingManager} associated with this RpcHandler.
    *  
    * @since 2.1 (20050422)
    */
   public void setMessagingManager(MessagingManager messagingManager)
   {
      this.messagingManager = messagingManager;
   }

   /**
    * Gets the exception translator that should be used to translate exceptions that occur duing method invocations.
    * 
    * @since 2.1
    */
   public InvocationExceptionTranslator getMethodInvocationExceptionTranslator()
   {
      return invocationExceptionTranslator;
   }
   
   /**
    * Sets the exception translator that should be used to translate exceptions that occur duing method invocations.
    * 
    * @param invocationExceptionTranslator the exception translator.
    * 
    * @since 2.1
    */
   public void setMethodInvocationExceptionTranslator(final InvocationExceptionTranslator invocationExceptionTranslator)
   {
      this.invocationExceptionTranslator = invocationExceptionTranslator;
   }
   
   /**
    * Sets the RPC handlers, removing the previous handler mappings.
    * 
    * @param handlers the new handler mappings (i.e. String to Object mappings). 
    * 
    * @since 2.1
    */
   public void setHandlers(final Map handlers)
   {
      synchronized(this.handlerMap)
      {
         this.handlerMap.clear();
         this.handlerMap.putAll(handlers);
      }
   }
   
   /**
    * Gets the RPC handlers.
    * 
    * @since 2.1.5 (20070301)
    */
   public Map getHandlers()
   {
      synchronized(this.handlerMap)
      {
         return (Map)this.handlerMap.clone();
      }
   }
   
   /**
    * Registers a handler for RPC methods.
    * 
    * @param name the name of the handler.
    * @param handler the handler.
    * 
    * @throws NullPointerException if name is null.
    */
   public void addHandler(final String name, final Object handler)
   {
      if( name == null ) throw new NullPointerException("Handler name may not be null!");
      
      synchronized(this.handlerMap)
      {
         this.handlerMap.put(name, handler);
      }
   }
   
   /**
    * Removes a handler for RPC methods.
    * 
    * @param name the name of the handler to remove.
    */
   public Object removeHandler(final String name)
   {
      synchronized(this.handlerMap)
      {
         return this.handlerMap.remove(name);
      }
   }
   
   /**
    * Gets a handler for RPC methods.
    * 
    * @param name the name of the handler to get.
    */
   public Object getHandler(final String name)
   {
      synchronized(this.handlerMap)
      {
         return this.handlerMap.get(name);
      }
   }
   
   /**
    * Gets the default RPC method handler.
    */
   public Object getDefaultHandler()
   {
      return defaultHandler;
   }

   /**
    * Sets the default RPC method handler.
    */
   public void setDefaultHandler(final Object defaultHandler)
   {
      this.defaultHandler = defaultHandler;
   }
   
   /**
    * 
    * @since 2.1.3 (20060502)
    */
   public String[] getHandlerNames()
   {
      synchronized(this.handlerMap)
      {
         return (String[])this.handlerMap.keySet().toArray(new String[0]);
      }
   }
   
   /**
    * Handles the specifed {@link Message} object as as an incomming RPC message, i.e. attempts to read 
    * the message body as an {@link RemoteProcedureCall} object. This method handles dispatching of a 
    * RPC response message, also in the event that an error occurs. The header from the incomming message will be used 
    * for the response. This means that the header may be modifiied in RPC methods, by accessing the 
    * message using the method {@link MessagingManager#getCurrentMessage()}.
    * 
    * @param message the received message.
    */
   public void handleRpcMessage(final Message message) throws MessageDispatchFailedException
   {
      RemoteProcedureCallResponse response = null;
      MessageHeader header = message.getHeader();
      Object messageBody = null;
      
      currenRpcResponseHeader.set(header);
      
      try
      {
         if( message.isConsumed() )
         {
            response = new RemoteProcedureCallResponse(new RpcException(RpcException.MISC_INTERNAL_ERROR, "Message consumed before it could be handled by RpcHandler, possibly due to an input stream read timeout."));
         }
         else if( message.hasExpired() )
         {
            response = new RemoteProcedureCallResponse(new RpcException(RpcException.RESPONSE_TIMEOUT, "Message expired before it could be handled."));
         }
         else
         {
            if( header.hasCustomHeaderField(RpcInputStream.RPC_INPUTSTREAM_HEADER_KEY) )
            {
               header.removeCustomHeaderField(RpcInputStream.RPC_INPUTSTREAM_HEADER_KEY); // Remove RPC_INPUTSTREAM_HEADER_KEY so the response message isn't interpreted as a streamed message
               response = this.executeRpc((String)header.getCustomHeaderField(MessagingRpcInterface.RPC_METHOD_NAME), 
                     new RpcInputStream(message));
            }
            else
            {
               messageBody = message.getBodyAsObject();
               response = this.executeRpc((RemoteProcedureCall)messageBody);
            }
         }
      }
      catch(Throwable t) // Really only for the getBodyAsObject call...
      {
         if(t instanceof ClassCastException )
         {
            String actualClassName = "unknown";
            try{
               actualClassName = messageBody.getClass().getName();
            }catch(Throwable t2){}
            
            if( messagingManager != null ) messagingManager.logError(messagingManager.getFullName() + ".RpcHandler", "Class cast error (" + t + ", actual class: " + actualClassName + ") executing RPC! Message: " + message + ".", t);
            else JServerUtilities.logError("RpcHandler", "Class cast error (" + t + ", actual class: " + actualClassName + ") executing RPC! Message: " + message + ".", t);
         }
         else
         {
            if( messagingManager != null ) messagingManager.logError(messagingManager.getFullName() + ".RpcHandler", "Error (" + t + ") executing RPC! Message header: " + message + ".", t);
            else JServerUtilities.logError("RpcHandler", "Error (" + t + ") executing RPC! Message header: " + message + ".", t);
         }
         
         response = new RemoteProcedureCallResponse(new RpcException(RpcException.MISC_INTERNAL_ERROR, t.getMessage()));
      }
      finally
      {
         currenRpcResponseHeader.set(null);
      }
      
      if( message.expectingResponse() )
      {
         if( response.getReturnValue() instanceof RpcInputStream )
         {
            header.setCustomHeaderField(RpcInputStream.RPC_INPUTSTREAM_HEADER_KEY, null);
            RpcInputStream rpcInputStreamResponse = (RpcInputStream)response.getReturnValue();
            this.messagingManager.dispatchMessageAsync(header, rpcInputStreamResponse.getInputStream(), rpcInputStreamResponse.getDataLength());
         }
         else this.dispatchRemoteProcedureCallResponse(header, response);
      }
   }
   
   /**
    * Default RPC message handling implementation to be used by {@link com.teletalk.jserver.tcp.messaging.MessageReceiver} implementations. This implementation 
    * calls the method {@link #handleRpcMessage(Message)} on the specified rpcHandler, if not null. If the rpcHandler is null, this implementation dispatches an appropriate 
    * error message as a response. 
    * 
    * @param message the received message.
    * @param rpcHandler an {@link RpcHandler}, may be null.
    * @param callingComponent the subcomponent making this call. If null, messagingManager will be used for this parameter. 
    * @param messagingManager the {@link MessagingManager} to be used for dispatching a response. 
    */
   public static void defaultHandleRpcMessage(final Message message, final RpcHandler rpcHandler, SubComponent callingComponent, final MessagingManager messagingManager)
   {
      try
      {
         if( callingComponent == null ) callingComponent = messagingManager;
         
         if( callingComponent.isDebugMode() ) callingComponent.logDebug("Invoking RPC method '" + RemoteProcedureCall.getRPCMethodName(message)  + "'. Message header: " + message.getHeader() + ".");
         
         if( rpcHandler != null ) rpcHandler.handleRpcMessage(message);
         else
         {
            callingComponent.logError("RpcHandler not set and messageReceived(Message) not overridden - unable to handle message " + message + "!");
            if( (messagingManager != null) && (message.getHeader().getHeaderType() == MessageHeader.RPC_HEADER) )
            {
               try
               {
                  messagingManager.dispatchMessageAsync(message.getHeader(), RpcHandler.getErrorResponse(RpcException.HANDLER_NOT_FOUND, "RPC Handler not found!"));
               }
               catch(Exception e)
               {
                  if( callingComponent.isDebugMode() ) callingComponent.log(Level.DEBUG, "Error occurred when attempting to return error response for unhandled message " + message + "!", e);
               }
            }
         }
      }
      catch(Exception e)
      {
         callingComponent.logError("Error occurred while handling rpc message " + message + "!", e);
      }
   }
   
   /**
    * Executes an rpc method.
    * 
    * @param remoteProcedureCall the {@link RemoteProcedureCall} to execute.
    */
   public RemoteProcedureCallResponse executeRpc(final RemoteProcedureCall remoteProcedureCall)
   {
      RemoteProcedureCallResponse response = null;
      
      try
      {
         Object returnValue = this.executeMethodCall(remoteProcedureCall.getMethodName(), remoteProcedureCall.getParams());
         
         response = new RemoteProcedureCallResponse(returnValue, remoteProcedureCall.isOutParameterModeEnabled() ? remoteProcedureCall.getParams() : null);
      }
      catch(RpcException rpce)
      {
         response = new RemoteProcedureCallResponse(rpce);
      }
      catch(Throwable t)
      {
         if( messagingManager != null ) messagingManager.logError(messagingManager.getFullName() + ".RpcHandler", "Error executing RPC!", t);
         else JServerUtilities.logError("RpcHandler", "Error executing RPC!", t);
         
         String msg = ThrowableUtils.getDescription(t);
                           
         response = new RemoteProcedureCallResponse(new RpcException(RpcException.MISC_INTERNAL_ERROR, msg));
      }
      
      return response;
   }
   
   
   /**
    * Executes an rpc method.
    * 
    * @since 2.1 (20050912)
    */
   public RemoteProcedureCallResponse executeRpc(final String methodName, final RpcInputStream rpcInputStream)
   {
      RemoteProcedureCallResponse response = null;
      
      try
      {
         Object returnValue = this.executeMethodCall(methodName, new Object[]{rpcInputStream});
         
         response = new RemoteProcedureCallResponse(returnValue, null);
      }
      catch(RpcException rpce)
      {
         response = new RemoteProcedureCallResponse(rpce);
      }
      catch(Throwable t)
      {
         if( messagingManager != null ) messagingManager.logError(messagingManager.getFullName() + ".RpcHandler", "Error executing RPC!", t);
         else JServerUtilities.logError("RpcHandler", "Error executing RPC!", t);
         
         String msg = ThrowableUtils.getDescription(t);
                           
         response = new RemoteProcedureCallResponse(new RpcException(RpcException.MISC_INTERNAL_ERROR, msg));
      }
      
      return response;
   }
   
   /**
    * Dispatches a response to a RPC method call.
    * 
    * @param header the header of the response.
    * @param response the {@link RemoteProcedureCallResponse} to send.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to lack of connected endpoints).
    */
   public void dispatchRemoteProcedureCallResponse(final MessageHeader header, final RemoteProcedureCallResponse response) throws MessageDispatchFailedException
   {
      this.messagingManager.dispatchMessageAsync(header, response);
   }
   
   /**
    * Dispatches an error response to a RPC method call.
    * 
    * @param header the header of the response.
    * @param description a description of the error.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to lack of connected endpoints).
    * 
    * @since 2.0 Build 762
    */
   public void dispatchRemoteProcedureCallErrorResponse(final MessageHeader header, final String description) throws MessageDispatchFailedException
   {
      this.messagingManager.dispatchMessageAsync(header, getErrorResponse(description));
   }
   
   /**
    * Dispatches an error response to a RPC method call.
    * 
    * @param header the header of the response.
    * @param responseCode the error code to return, possibly one of the constants defined in {@link RpcException}.
    * @param description a description of the error.
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to lack of connected endpoints).
    * 
    * @since 2.0 Build 762
    */
   public void dispatchRemoteProcedureCallErrorResponse(final MessageHeader header, final long responseCode, final String description) throws MessageDispatchFailedException
   {
      this.messagingManager.dispatchMessageAsync(header, getErrorResponse(responseCode, description));
   }
   
   /**
    * Performs the actual invocation of a RPC method call.
    *
    * @param methodName the name of the method to be invoked.
    * @param params the method params.
    * @return the return value of the method call.
    * 
    * @throws RpcException which incapsulates an error that occurred while invoking the method. 
    */
   protected Object executeMethodCall(String methodName, final Object[] params) throws Exception
   {
      Object handler = null;
      Object returnValue = null;
      Object[] methodParams = params;
      
      int dotIndex = methodName.lastIndexOf('.');
      if(dotIndex > 0)
      {
         String handlerName = methodName.substring(0, dotIndex);
         methodName = methodName.substring(dotIndex + 1);
         
         handler = this.getHandler(handlerName);
      }
      
      if( handler == null ) handler = this.defaultHandler;
      
      if( handler != null )
      {
         Method method = ReflectionUtils.findMethod(handler.getClass(), methodName, methodParams, false);
			if( method == null )
			{
				method = ReflectionUtils.findMethod(handler.getClass(), methodName, methodParams, false, true);
			}
			if( method == null )
			{
				// Attempt to encapsulate params into array, in case the called method should take Serializable[] as param 
				// (see MessagingRpcInterface.invoke(MessageHeader, String, Serializable[]).
				methodParams = new Object[]{methodParams};
				method = ReflectionUtils.findMethod(handler.getClass(), methodName, methodParams, false);
			}
			         
         if( method != null )
         {            
            if( method.getDeclaringClass() == Object.class )
            {
               throw new RpcException(RpcException.JAVA_LANG_OBJECT_METHOD_CALLED, "Methods in java.lang.Object may not be invoked!");
            }
				try
				{
               returnValue = method.invoke(handler, methodParams);
				}
				catch(Throwable t)
				{
               if( t instanceof InvocationTargetException )
               {
                  t = t.getCause();
               }
               if( t instanceof RpcException )
               {
                  throw (RpcException)t;
               }
               else
               {
                  if( invocationExceptionTranslator != null )
                  {
                     try{
                     t = invocationExceptionTranslator.translate(handler, method, methodParams, t);
                     }catch(Throwable tt){t = tt;}
                  }
                  
                  if( t != null )
                  {
                     String msg = ThrowableUtils.getDescription(t);
      
                     if( messagingManager != null ) messagingManager.logWarning("Error while executing method '" + methodName + "' in handler '" + handler + "'! Error details: " + msg + ".", t);
      					throw new RpcException(RpcException.METHOD_INVOCATION_ERROR, "Error while executing method '" + methodName + "' in handler '" + handler + "'! Error details: " + msg + ".", t);
                  }
               }
				}
         }
         else
         {
				String paramClassString = "(";
				String param;
            if( params != null )
            {
   				for (int i = 0; i < params.length; i++)
   				{
   					param = (params[i] != null) ? params[i].getClass().getName() : "null";
   					if (i < (params.length - 1)) paramClassString += param + ", ";
   					else paramClassString += param;
   				}
            }
				paramClassString += ")";
            
            if( messagingManager != null ) messagingManager.logWarning("Method '" + methodName + "' matching parameters " + paramClassString + " not found in handler '" + handler + "'!");
            throw new RpcException(RpcException.METHOD_NOT_FOUND, "Method '" + methodName + "' matching parameters " + paramClassString + " not found in handler '" + handler + "'!");
         }
      }
      else
      {
         throw new RpcException(RpcException.HANDLER_NOT_FOUND, "Handler '" + handler + "' not found!");
      }
      
      return returnValue;
   }
   
   /**
    * Constructs a RemoteProcedureCallResponse objects to send as a response to a RPC message that failed for some reason. 
    * This method will send {@link RpcException#MISC_INTERNAL_ERROR} as error code.
    * 
    * @param description a description of the error.
    * 
    * @since 2.0 Build 757
    */
   public static RemoteProcedureCallResponse getErrorResponse(final String description)
   {
      return new RemoteProcedureCallResponse(new RpcException(RpcException.MISC_INTERNAL_ERROR, description));
   }
   
   /**
    * Constructs a RemoteProcedureCallResponse objects to send as a response to a RPC message that failed for some reason.
    * 
    * @param responseCode the error code to return, possibly one of the constants defined in {@link RpcException}.
    * @param description a description of the error.
    * 
    * @since 2.0 Build 757 
    */
   public static RemoteProcedureCallResponse getErrorResponse(final long responseCode, final String description)
   {
      return new RemoteProcedureCallResponse(new RpcException(responseCode, description));
   }
}
