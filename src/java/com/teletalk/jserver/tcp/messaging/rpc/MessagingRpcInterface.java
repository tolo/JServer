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
 TODO: Document behaviour with null parameters (no guarantee which method is invoked if several match...)
*/
package com.teletalk.jserver.tcp.messaging.rpc;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.security.AccessController;
import java.security.PrivilegedExceptionAction;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import com.teletalk.jserver.tcp.messaging.Message;
import com.teletalk.jserver.tcp.messaging.MessageDispatchFailedException;
import com.teletalk.jserver.tcp.messaging.MessageDispatcher;
import com.teletalk.jserver.tcp.messaging.MessageHeader;
import com.teletalk.jserver.tcp.messaging.MessagingException;
import com.teletalk.jserver.tcp.messaging.ResponseTimeOutException;

/**
 * Class providing a client side interface for messaging based RPC communication. RPC methods are 
 * invoked by specifying a method name and parameters. The method name may be the name of the method 
 * to invoke (i.e. <code>"myMethod"</code>) or the method name prefixed by a handler name (i.e. <code>"myHandler.myMethod"</code>), 
 * in which case the handler name must be separated from the method name by a dot ('.'). Only the last dot separator is used to determine the 
 * method name, which means that handlers names may contain dot themselves.<br>
 * <br>
 * Parameters used in method calls may be used as out or in/out parameters by setting the flag <code>outParameterModeEnabled</code> through 
 * the method {@link #setOutParameterModeEnabled(boolean)}. This means that any changes in the parameter object on the server 
 * side will be reflected on the client side, after the call has completed.
 * 
 * @see RcpMessageReceiver
 * @see RpcHandler
 *   
 * @author Tobias Löfstrand
 * 
 * @since 1.3.1 Build 690
 */
public class MessagingRpcInterface implements InvocationHandler
{
   /** Default message type id for RCP messages. */
   public static final int RPC_MESSAGE_TYPE_ID = -1000;
   
   /**  @since 2.0 Build 756 */
   public static final String RPC_METHOD_NAME = "com.teletalk.jserver.tcp.messaging.rpc.methodName";
   
   
   private static final ThreadLocal ContextMessageHeader = new ThreadLocal();
   
   /**
    * Sets the thread local message header to be used when dispatching messages in the current thread.
    */
   public static void setContextMessageHeader(MessageHeader messageHeader)
   {
      ContextMessageHeader.set(messageHeader);
   }
   
   /**
    * Gets the thread local message header to be used when dispatching messages in the current thread.
    */
   public static MessageHeader getContextMessageHeader()
   {
      return (MessageHeader)ContextMessageHeader.get();
   }
   
   
   private final MessageDispatcher messageDispatcher;
   
   private boolean outParameterModeEnabled;
   
   private final HashMap proxyHandlerNameMap = new HashMap();
   
   private final HashSet proxyThrowRealException = new HashSet();
   
   
   /**
    * Creates a new MessagingRpcInterface using the specified MessageDispatcher for dispatching 
    * of rcp messages.
    * 
    * @param messageDispatcher a reference to a MessageDispatcher used for dispatching messages.
    */
   public MessagingRpcInterface(final MessageDispatcher messageDispatcher)
   {
      this.messageDispatcher = messageDispatcher;
      this.outParameterModeEnabled = false;
   }
   
   /**
    * Gets the MessageDispatcher used by this MessagingRpcInterface to dispatch RPC messages.
    */
   public MessageDispatcher getMessageDispatcher()
   {
      return messageDispatcher;
   }
   
   /**
    * Gets the value of the flag, indicating if out parameter mode is enabled. If out parameter mode is enabled, any parameter specified 
    * in a RCP method call could be used as an out parameter. This means that any changes in the parameter object on the server side will 
    * be reflected on the client side, after the call has completed.
    */
   public boolean isOutParameterModeEnabled()
   {
      return outParameterModeEnabled;
   }

   /**
    * Sets the value of the flag, indicating if out parameter mode is enabled. If out parameter mode is enabled, any parameter specified 
    * in a RCP method call could be used as an out parameter. This means that any changes in the parameter object on the server side will 
    * be reflected on the client side, after the call has completed.
    */
   public void setOutParameterModeEnabled(boolean outParametersEnabled)
   {
      this.outParameterModeEnabled = outParametersEnabled;
   }
   
   /**
    * Gets a MessageHeader instance suitable for sending RPC messages. This method will first attempt to get 
    * any message header set for the current thread, using the method {@link #getContextMessageHeader()}. If no such header is set, 
    * the method {@link MessageDispatcher#createPrototypeMessageHeaderInstance()} will be invoked to create an instance of the 
    * prototype header (or an empty one if no such header is set).
    * 
    * @since 2.1 (20050912)
    */
   public MessageHeader getRpcMessageHeader()
   {
      MessageHeader header = getContextMessageHeader();
      
      if( header != null ) return header;
      else return this.messageDispatcher.createPrototypeMessageHeaderInstance();
   }
   
   /**
    * Sets new values for parameters specified when making an remote procedure call, if the flag {@link #isOutParameterModeEnabled() outParameterModeEnabled} is set to true.   
    * 
    * @param paramList the parameters specified when the call was made. 
    * @param outParamValues the modified parameters.
    * 
    * @since 2.0 (2004101213XX)
    */
   protected void setOutParameters(final Object[] paramList, final Object[] outParamValues) throws Exception
   {
      AccessController.doPrivileged(new PrivilegedExceptionAction()
      {
         public Object run() throws Exception
         {
            Field[] fields;
            int modifiers;
            boolean accessible;
            Class currentClass;
            
            for(int i=0; i<paramList.length; i++)
            {
               if( paramList[i] != null )
               {
                  currentClass = paramList[i].getClass();
                  if( (currentClass != null) && currentClass.isArray() )
                  {
                     System.arraycopy(outParamValues[i], 0, paramList[i], 0, ((Object[])paramList[i]).length); 
                  }
                  else
                  {
                     while( (currentClass != null) && (currentClass != Object.class) )
                     {
                        fields = paramList[i].getClass().getDeclaredFields();

                        for(int f=0; f<fields.length; f++)
                        {
                           modifiers = fields[f].getModifiers();
                           if( (!Modifier.isFinal(modifiers)) && (!Modifier.isStatic(modifiers)) ) // Not final or static
                           {
                              accessible = fields[f].isAccessible();
                              fields[f].setAccessible(true);
                              fields[f].set(paramList[i], fields[f].get(outParamValues[i]));
                              fields[f].setAccessible(accessible);
                           }
                        }
                        
                        currentClass = currentClass.getSuperclass();
                     }
                  }
               }
            }
            
            return null;
         }
      });
   }
   
   
   /**
    * Called when an error occurs while invoking a remote method to create an appropriate RpcException object. Subclasses may 
    * override this method to create custom RpcException objects.
    *  
    * @param methodName the name of the RCP method that was invoked. 
    * @param throwable the error that occurred.
    * 
    * @return an RpcException object.
    * 
    * @since 2.0 (2004101213XX)
    */
   protected RpcException handleError(final String methodName, final Throwable throwable)
   {
      if(throwable instanceof ResponseTimeOutException)
      {
         return new RpcException(RpcException.RESPONSE_TIMEOUT, throwable.getMessage(), throwable);
      }
      else if(throwable instanceof MessageDispatchFailedException)
      {
         MessageHeader responseHeader = ((MessageDispatchFailedException)throwable).getResponseHeader();
         
         if( (responseHeader != null) && responseHeader.isAccessDeniedHeader() )
         {
            return new RpcException(RpcException.ACCESS_DENIED, throwable.getMessage(), throwable);
         }
         else
         {
            return new RpcException(RpcException.MESSAGE_DISPATCH_FAILURE, throwable.getMessage(), throwable);
         }
      }
      else
      {
         messageDispatcher.getMessagingManager().logError("Error while invoking RPC method '" + methodName + "'!", throwable);
         return new RpcException(RpcException.MISC_INTERNAL_ERROR, throwable.getMessage(), throwable);
      }
   }
   
   /**
    * Invokes an RPC method, using that parameters specified in the parameter list.
    *  
    * @throws RpcException if an RCP error occurs.
    */
   private Object invokeInternal(final MessageHeader header, final String methodName, final Object params) throws RpcException
   {
      RpcException rpcException = null;
      
      RpcInputStream rpcInputStream = null;
      RemoteProcedureCall rpcMsg = null;
      Object[] paramList = null;
      if( params instanceof RpcInputStream ) rpcInputStream = (RpcInputStream)params;
      else
      {
         paramList = (Object[])params;
         if( (paramList != null) && (paramList.length == 1) && (paramList[0] instanceof RpcInputStream) ) rpcInputStream = (RpcInputStream)paramList[0];
         else rpcMsg = new RemoteProcedureCall(methodName, paramList, this.outParameterModeEnabled);
      }
      
      // Reset fields (since header may be reused)
      header.resetMessageRoutingFields();
      header.removeCustomHeaderField(RpcInputStream.RPC_INPUTSTREAM_HEADER_KEY);
      
      if( header.getMessageType() == -1 )
      {
         header.setMessageType(RPC_MESSAGE_TYPE_ID);
      }
      if( header.getHeaderType() == MessageHeader.STANDARD_HEADER )
      {
         header.setHeaderType(MessageHeader.RPC_HEADER);
      }
      header.setCustomHeaderField(RPC_METHOD_NAME, methodName);
      
      try
      {
         Message response;
         
         if( rpcInputStream != null )
         {
            header.setCustomHeaderField(RpcInputStream.RPC_INPUTSTREAM_HEADER_KEY, null);
            response = this.messageDispatcher.dispatchMessage(header, rpcInputStream.getInputStream(), rpcInputStream.getDataLength());
         }
         else response = this.messageDispatcher.dispatchMessage(header, rpcMsg);
         
         if( response == null ) // If response == null, assume message is asych...
         {
            return null;
         }
         else if( response.getHeader().hasCustomHeaderField(RpcInputStream.RPC_INPUTSTREAM_HEADER_KEY) || 
                     response.getHeader().hasCustomHeaderField(RpcInputStreamResponse.RPC_INPUTSTREAM_RESPONSE_HEADER_KEY) )
         {
            return new RpcInputStreamResponse(response); // This is necessary to maintain backwards compatability
         }
         else
         {
	         Object responseObject = response.getBodyAsObject();
	         
	         if( responseObject instanceof RemoteProcedureCallResponse ) 
	         {
	            RemoteProcedureCallResponse rpcResponse = (RemoteProcedureCallResponse)responseObject;
	            
	            if( rpcResponse.getRpcException() != null )
	            {
	               rpcException = rpcResponse.getRpcException();
	            }
	            else
	            {
                  // Set out parameters is applicable
	               final Object[] outParams = rpcResponse.getParams();
	               if( this.outParameterModeEnabled && (outParams != null) )
	               {
	                  this.setOutParameters(paramList, outParams);
	               }
	               
	               return rpcResponse.getReturnValue();
	            }
	         }
	         else if( responseObject instanceof RpcException )
	         {
	            rpcException = (RpcException)responseObject;
	         }
	         else
	         {
	            rpcException = new RpcException(RpcException.MISC_INTERNAL_ERROR, "Unknown response: " + responseObject + "!");
	         }
         }
      }
      catch(Throwable t)
      {
         throw handleError(methodName, t);
      }
      
      if( rpcException != null ) throw rpcException;
      else throw new RpcException(RpcException.MISC_INTERNAL_ERROR, "Unknown internal error!"); // This should never happen
   }
   
   /**
    * Invokes an RPC method with an input stream as parameter. 
    * 
    * @param header the header of the RPC message to be dispatched.
    * @param methodName the name of the RCP method. The format of this parameter is <code>handlerName.methodName</code>, but 
    * <code>handlerName.</code> may be omitted if the default (unnamed) hander is to be used.
    * @param rpcInputStream the input stream.
    * 
    * @throws RpcException if an RCP error occurs. 
    * 
    * @since 2.1 (20050912)
    */
   public Object invoke(final MessageHeader header, final String methodName, final RpcInputStream rpcInputStream) throws RpcException
   {
      return this.invokeInternal(header, methodName, rpcInputStream);
   }
   
   /**
    * Invokes an RPC method, using that parameters specified in the parameter list.
    * 
    * @param header the header of the RPC message to be dispatched.
    * @param methodName the name of the RCP method. The format of this parameter is <code>handlerName.methodName</code>, but 
    * <code>handlerName.</code> may be omitted if the default (unnamed) hander is to be used.
    * @param paramList the parameter list.
    * 
    * @throws RpcException if an RCP error occurs. 
    */
   public Object invokeWithParamList(final MessageHeader header, final String methodName, final Serializable[] paramList) throws RpcException
   {
      return this.invokeInternal(header, methodName, paramList);
   }
      
   /**
    * Invokes an RPC method, using that parameters specified in the parameter list.
    * 
    * @param header the header of the RPC message to be dispatched.
    * @param methodName the name of the RCP method. The format of this parameter is <code>handlerName.methodName</code>, but 
    * <code>handlerName.</code> may be omitted if the default (unnamed) hander is to be used.
    * @param paramList the parameter list.
    * 
    * @throws RpcException if an RCP error occurs. 
    */
	public Object invokeWithParamList(final MessageHeader header, final String methodName, final List paramList) throws RpcException
	{
		return this.invokeInternal(header, methodName, (Serializable[])paramList.toArray(new Serializable[0]));
	}
   
   /**
    * Invokes an no-arg RPC method. 
    * 
    * @param header the header of the RPC message to be dispatched.
    * @param methodName the name of the RCP method. The format of this parameter is <code>handlerName.methodName</code>, but 
    * <code>handlerName.</code> may be omitted if the default (unnamed) hander is to be used.
    * 
    * @throws RpcException if an RCP error occurs. 
    */
   public Object invoke(final MessageHeader header, final String methodName) throws RpcException
   {
      return this.invokeInternal(header, methodName, new Serializable[]{});
   }
   
   /**
    * Invokes an RPC method with one parameter. 
    * 
    * @param header the header of the RPC message to be dispatched.
    * @param methodName the name of the RCP method. The format of this parameter is <code>handlerName.methodName</code>, but 
    * <code>handlerName.</code> may be omitted if the default (unnamed) hander is to be used.
    * @param param1 param 1.
    * 
    * @throws RpcException if an RCP error occurs. 
    */
   public Object invoke(final MessageHeader header, final String methodName, final Serializable param1) throws RpcException
   {
      return this.invokeInternal(header, methodName, new Serializable[]{param1});
   }
   
   /**
    * Invokes an RPC method with two parameters. 
    * 
    * @param header the header of the RPC message to be dispatched.
    * @param methodName the name of the RCP method. The format of this parameter is <code>handlerName.methodName</code>, but 
    * <code>handlerName.</code> may be omitted if the default (unnamed) hander is to be used.
    * @param param1 param 1.
    * @param param2 param 2.
    * 
    * @throws RpcException if an RCP error occurs. 
    */
   public Object invoke(final MessageHeader header, final String methodName, final Serializable param1, final Serializable param2) throws RpcException
   {
      return this.invokeInternal(header, methodName, new Serializable[]{param1, param2});
   }

   /**
    * Invokes an RPC method with three parameters. 
    * 
    * @param header the header of the RPC message to be dispatched.
    * @param methodName the name of the RCP method. The format of this parameter is <code>handlerName.methodName</code>, but 
    * <code>handlerName.</code> may be omitted if the default (unnamed) hander is to be used.
    * @param param1 param 1.
    * @param param2 param 2.
    * @param param3 param 3.
    * 
    * @throws RpcException if an RCP error occurs. 
    */   
   public Object invoke(final MessageHeader header, final String methodName, final Serializable param1, final Serializable param2, final Serializable param3) throws RpcException
   {
      return this.invokeInternal(header, methodName, new Serializable[]{param1, param2, param3});
   }
   
   /**
    * Invokes an RPC method with four parameters. 
    * 
    * @param header the header of the RPC message to be dispatched.
    * @param methodName the name of the RCP method. The format of this parameter is <code>handlerName.methodName</code>, but 
    * <code>handlerName.</code> may be omitted if the default (unnamed) hander is to be used.
    * @param param1 param 1.
    * @param param2 param 2.
    * @param param3 param 3.
    * @param param4 param 4.
    * 
    * @throws RpcException if an RCP error occurs. 
    */   
   public Object invoke(final MessageHeader header, final String methodName, final Serializable param1, final Serializable param2, final Serializable param3, final Serializable param4) throws RpcException
   {
      return this.invokeInternal(header, methodName, new Serializable[]{param1, param2, param3, param4});
   }
   
   /**
    * Invokes an RPC method with five parameters. 
    * 
    * @param header the header of the RPC message to be dispatched.
    * @param methodName the name of the RCP method. The format of this parameter is <code>handlerName.methodName</code>, but 
    * <code>handlerName.</code> may be omitted if the default (unnamed) hander is to be used.
    * @param param1 param 1. 
    * @param param2 param 2.
    * @param param3 param 3.
    * @param param4 param 4.
    * @param param5 param 5.
    * 
    * @throws RpcException if an RCP error occurs. 
    */
   public Object invoke(final MessageHeader header, final String methodName, final Serializable param1, final Serializable param2, final Serializable param3, final Serializable param4, final Serializable param5) throws RpcException
   {
      return this.invokeInternal(header, methodName, new Serializable[]{param1, param2, param3, param4, param5});
   }
   
   
   
   /**
    * Invokes an RPC method with an input stream as parameter. This method will use the 
    * message header returned by the {@link #getRpcMessageHeader()}.
    * 
    * @param methodName the name of the RCP method. The format of this parameter is <code>handlerName.methodName</code>, but 
    * <code>handlerName.</code> may be omitted if the default (unnamed) hander is to be used.
    * @param rpcInputStream the input stream.
    * 
    * @throws RpcException if an RCP error occurs. 
    */
   public Object invoke(final String methodName, final RpcInputStream rpcInputStream) throws RpcException
   {
      return this.invokeInternal(this.getRpcMessageHeader(), methodName, rpcInputStream);
   }
   
   
   /**
    * Invokes an RPC method, using that parameters specified in the parameter list. This method will use the 
    * message header returned by the {@link #getRpcMessageHeader()}.
    * 
    * @param methodName the name of the RCP method. The format of this parameter is <code>handlerName.methodName</code>, but 
    * <code>handlerName.</code> may be omitted if the default (unnamed) hander is to be used.
    * @param paramList the parameter list.
    * 
    * @throws RpcException if an RCP error occurs. 
    * 
    * @since 2.1 (20050912)
    */
   public Object invokeWithParamList(final String methodName, final Serializable[] paramList) throws RpcException
   {
      return this.invokeInternal(this.getRpcMessageHeader(), methodName, paramList);
   }
   
   /**
    * Invokes an RPC method, using that parameters specified in the parameter list. This method will use the 
    * message header returned by the {@link #getRpcMessageHeader()}.
    * 
    * @param methodName the name of the RCP method. The format of this parameter is <code>handlerName.methodName</code>, but 
    * <code>handlerName.</code> may be omitted if the default (unnamed) hander is to be used.
    * @param paramList the parameter list.
    * 
    * @throws RpcException if an RCP error occurs. 
    */
	public Object invokeWithParamList(final String methodName, final List paramList) throws RpcException
	{
		return this.invokeWithParamList(methodName, (Serializable[])paramList.toArray(new Serializable[0]));
	}
   
   /**
    * Invokes an no-arg RPC method. This method will use the 
    * message header returned by the {@link #getRpcMessageHeader()}.
    * 
    * @param methodName the name of the RCP method.
    * 
    * @throws RpcException if an RCP error occurs. 
    */
   public Object invoke(final String methodName) throws RpcException
   {
      return this.invokeWithParamList(methodName, new Serializable[]{});
   }
   
   /**
    * Invokes an RPC method with one parameter. This method will use the 
    * message header returned by the {@link #getRpcMessageHeader()}.
    * 
    * @param methodName the name of the RCP method. The format of this parameter is <code>handlerName.methodName</code>, but 
    * <code>handlerName.</code> may be omitted if the default (unnamed) hander is to be used.
    * @param param1 param 1.
    * 
    * @throws RpcException if an RCP error occurs. 
    */
   public Object invoke(final String methodName, final Serializable param1) throws RpcException
   {
      return this.invokeWithParamList(methodName, new Serializable[]{param1});
   }
   
   /**
    * Invokes an RPC method with two parameters. This method will use the 
    * message header returned by the {@link #getRpcMessageHeader()}.
    * 
    * @param methodName the name of the RCP method. The format of this parameter is <code>handlerName.methodName</code>, but 
    * <code>handlerName.</code> may be omitted if the default (unnamed) hander is to be used.
    * @param param1 param 1.
    * @param param2 param 2.
    * 
    * @throws RpcException if an RCP error occurs. 
    */
   public Object invoke(final String methodName, final Serializable param1, final Serializable param2) throws RpcException
   {
      return this.invokeWithParamList(methodName, new Serializable[]{param1, param2});
   }
   
   /**
    * Invokes an RPC method with three parameters. This method will use the 
    * message header returned by the {@link #getRpcMessageHeader()}.
    * 
    * @param methodName the name of the RCP method. The format of this parameter is <code>handlerName.methodName</code>, but 
    * <code>handlerName.</code> may be omitted if the default (unnamed) hander is to be used.
    * @param param1 param 1.
    * @param param2 param 2.
    * @param param3 param 3.
    * 
    * @throws RpcException if an RCP error occurs. 
    */
   public Object invoke(final String methodName, final Serializable param1, final Serializable param2, final Serializable param3) throws RpcException
   {
      return this.invokeWithParamList(methodName, new Serializable[]{param1, param2, param3});
   }
   
   /**
    * Invokes an RPC method with four parameters. This method will use the 
    * message header returned by the {@link #getRpcMessageHeader()}.
    * 
    * @param methodName the name of the RCP method. The format of this parameter is <code>handlerName.methodName</code>, but 
    * <code>handlerName.</code> may be omitted if the default (unnamed) hander is to be used.
    * @param param1 param 1.
    * @param param2 param 2.
    * @param param3 param 3.
    * @param param4 param 4.
    * 
    * @throws RpcException if an RCP error occurs. 
    */
   public Object invoke(final String methodName, final Serializable param1, final Serializable param2, final Serializable param3, final Serializable param4) throws RpcException
   {
      return this.invokeWithParamList(methodName, new Serializable[]{param1, param2, param3, param4});
   }
   
   /**
    * Invokes an RPC method with five parameters. This method will use the 
    * message header returned by the {@link #getRpcMessageHeader()}.
    * 
    * @param methodName the name of the RCP method. The format of this parameter is <code>handlerName.methodName</code>, but 
    * <code>handlerName.</code> may be omitted if the default (unnamed) hander is to be used.
    * @param param1 param 1.
    * @param param2 param 2.
    * @param param3 param 3.
    * @param param4 param 4.
    * @param param5 param 5.
    * 
    * @throws RpcException if an RCP error occurs. 
    */
   public Object invoke(final String methodName, final Serializable param1, final Serializable param2, final Serializable param3, final Serializable param4, final Serializable param5) throws RpcException
   {
      return this.invokeWithParamList(methodName, new Serializable[]{param1, param2, param3, param4, param5});
   }
   
   
   /* ### PROXY RELATED METHODS ### */
   
   
   /**
    * Creates an instance of a proxy class, implementing the specified interface, that translates method invocations to RPC calls. The RPC calls 
    * will be handled by the default handler on the server side.<br>
    * <br>
    * Note: Make sure that interface classes are designed to be fully serializable (parameters, return values and exceptions), since all method calls  
    * will translated to messages that will be serialized to a byte stream.<br>
    * <br>
    * Calling this method has the exact same effect as calling {@link #createProxy(ClassLoader, Class[], String, boolean) createProxy(null, new Class[]&#125;interfaceClass&#125;, null, true)}. 
    * 
    * @param interfaceClass the Class object representing the interface which the proxy instance is to implement. 
    * 
    * @return an RPC proxy instance for the specified interface.
    * 
    * @throws NullPointerException if interfaceClass is null.
    * @throws IllegalArgumentException if a proxy class could not be created for parameter interfaceClass. See java.lang.reflect.Proxy for details.
    * 
    * @see java.lang.reflect.Proxy
    *  
    * @since 2.0.1 (20050210)
    */
   public Object createProxy(final Class interfaceClass) throws NullPointerException, IllegalArgumentException
   {
      return createProxy(null, new Class[]{interfaceClass}, null, true);
   }
     
   /**
    * Creates an instance of a proxy class, implementing the specified interface, that translates method invocations to RPC calls. The RPC calls 
    * will be handled by a server side handler registered under the name specified by parameter <code>remoteHandlerName</code>.<br>
    * <br>
    * Note: Make sure that interface classes are designed to be fully serializable (parameters, return values and exceptions), since all method calls  
    * will translated to messages that will be serialized to a byte stream.<br>
    * <br>
    * Calling this method has the exact same effect as calling {@link #createProxy(ClassLoader, Class[], String, boolean) createProxy(null, new Class[]&#125;interfaceClass&#125;, remoteHandlerName, true)}.
    * 
    * @param interfaceClass the Class object representing the interface which the proxy instance is to implement.
    * @param remoteHandlerName the name of the server side handler to which RPC calls should be sent. If null, the default handler is used.
    * 
    * @return an RPC proxy instance for the specified interface.
    * 
    * @throws NullPointerException if interfaceClass is null.
    * @throws IllegalArgumentException if a proxy class could not be created for parameter interfaceClass. See java.lang.reflect.Proxy for details.
    * 
    * @see java.lang.reflect.Proxy
    * 
    * @since 2.0.1 (20050210)
    */
   public Object createProxy(final Class interfaceClass, final String remoteHandlerName) throws NullPointerException, IllegalArgumentException
   {
      return createProxy(null, new Class[]{interfaceClass}, remoteHandlerName, true);
   }
   
   /**
    * Creates an instance of a proxy class, implementing the specified interface, that translates method invocations to RPC calls. The RPC calls 
    * will be handled by a server side handler registered under the name specified by parameter <code>remoteHandlerName</code>.<br>
    * <br>
    * Note: Make sure that interface classes are designed to be fully serializable (parameters, return values and exceptions), since all method calls  
    * will translated to messages that will be serialized to a byte stream.<br>
    * <br>
    * Calling this method has the exact same effect as calling {@link #createProxy(ClassLoader, Class[], String, boolean) createProxy(null, new Class[]&#125;interfaceClass&#125;, remoteHandlerName, throwRealException)}.
    * 
    * @param interfaceClass the Class object representing the interface which the proxy instance is to implement.
    * @param remoteHandlerName the name of the server side handler to which RPC calls should be sent. If null, the default handler is used.
    * @param throwRealException flag indicating if the actual exceptions that occur should be thown in method calls, whenever possible. If this flag is set 
    * to false, a {@link RpcExceptionWrapper} will be thrown (due to the fact that {@link RpcException} is a checked exception). If however the interface defines 
    * {@link RpcException} in the throws clause of a method, that exception will be thrown regardless of the state of this flag.
    * 
    * @return an RPC proxy instance for the specified interface.
    * 
    * @throws NullPointerException if interfaceClass is null.
    * @throws IllegalArgumentException if a proxy class could not be created for parameter interfaceClass. See java.lang.reflect.Proxy for details.
    * 
    * @see java.lang.reflect.Proxy
    * 
    * @since 2.0.1 (20050210)
    */
   public Object createProxy(final Class interfaceClass, final String remoteHandlerName, final boolean throwRealException) throws NullPointerException, IllegalArgumentException
   {
      return createProxy(null, new Class[]{interfaceClass}, remoteHandlerName, throwRealException);
   }
  
   /**
    * Creates an instance of a proxy class, implementing the specified interfaces, that translates method invocations to RPC calls. The RPC calls 
    * will be handled by the default handler on the server side.<br>
    * <br>
    * Note: Make sure that interface classes are designed to be fully serializable (parameters, return values and exceptions), since all method calls  
    * will translated to messages that will be serialized to a byte stream.<br>
    * <br>
    * Calling this method has the exact same effect as calling {@link #createProxy(ClassLoader, Class[], String, boolean) createProxy(null, interfaceClasses, null, true)}.
    * 
    * @param interfaceClasses the Class objects representing the interfaces which the proxy instance is to implement.
    * 
    * @return an RPC proxy instance for the specified interfaces.
    * 
    * @throws NullPointerException if interfaceClasses is null.
    * @throws IllegalArgumentException if a proxy class could not be created for parameter interfaceClass. See java.lang.reflect.Proxy for details.
    * 
    * @see java.lang.reflect.Proxy
    * 
    * @since 2.0.1 (20050210)
    */
   public Object createProxy(final Class[] interfaceClasses) throws NullPointerException, IllegalArgumentException
   {
      return createProxy(null, interfaceClasses, null, true);
   }
   
   /**
    * Creates an instance of a proxy class, implementing the specified interfaces, that translates method invocations to RPC calls. The RPC calls 
    * will be handled by a server side handler registered under the name specified by parameter <code>remoteHandlerName</code>.<br>
    * <br>
    * Note: Make sure that interface classes are designed to be fully serializable (parameters, return values and exceptions), since all method calls  
    * will translated to messages that will be serialized to a byte stream.<br>
    * <br>
    * Calling this method has the exact same effect as calling {@link #createProxy(ClassLoader, Class[], String, boolean) createProxy(null, interfaceClasses, remoteHandlerName, true)}.
    *  
    * @param interfaceClasses the Class objects representing the interfaces which the proxy instance is to implement.
    * @param remoteHandlerName the name of the server side handler to which RPC calls should be sent. If null, the default handler is used.
    * 
    * @return an RPC proxy instance for the specified interfaces.
    * 
    * @throws NullPointerException if interfaceClasses is null.
    * @throws IllegalArgumentException if a proxy class could not be created for parameter interfaceClass. See java.lang.reflect.Proxy for details.
    * 
    * @see java.lang.reflect.Proxy
    * 
    * @since 2.0.1 (20050210)
    */
   public Object createProxy(final Class[] interfaceClasses, final String remoteHandlerName) throws NullPointerException, IllegalArgumentException
   {
      return createProxy(null, interfaceClasses, remoteHandlerName, true);
   }
   
   /**
    * Creates an instance of a proxy class, implementing the specified interfaces, that translates method invocations to RPC calls. The RPC calls 
    * will be handled by a server side handler registered under the name specified by parameter <code>remoteHandlerName</code>.<br>
    * <br>
    * Note: Make sure that interface classes are designed to be fully serializable (parameters, return values and exceptions), since all method calls  
    * will translated to messages that will be serialized to a byte stream.<br>
    * <br>
    * Calling this method has the exact same effect as calling {@link #createProxy(ClassLoader, Class[], String, boolean) createProxy(null, interfaceClasses, remoteHandlerName, throwRealException)}.
    *  
    * @param interfaceClasses the Class objects representing the interfaces which the proxy instance is to implement.
    * @param remoteHandlerName the name of the server side handler to which RPC calls should be sent. If null, the default handler is used.
    * @param throwRealException flag indicating if the actual exceptions that occur should be thown in method calls, whenever possible. If this flag is set 
    * to false, a {@link RpcExceptionWrapper} will be thrown (due to the fact that {@link RpcException} is a checked exception). If however the interface defines 
    * {@link RpcException} in the throws clause of a method, that exception will be thrown regardless of the state of this flag.
    * 
    * @return an RPC proxy instance for the specified interfaces.
    * 
    * @throws NullPointerException if interfaceClasses is null.
    * @throws IllegalArgumentException if a proxy class could not be created for parameter interfaceClass. See java.lang.reflect.Proxy for details.
    * 
    * @see java.lang.reflect.Proxy
    * 
    * @since 2.0.1 (20050210)
    */
   public Object createProxy(final Class[] interfaceClasses, final String remoteHandlerName, final boolean throwRealException) throws NullPointerException, IllegalArgumentException
   {
      return createProxy(null, interfaceClasses, remoteHandlerName, throwRealException);
   }
   
   /**
    * Creates an instance of a proxy class, implementing the specified interfaces, that translates method invocations to RPC calls. The RPC calls 
    * will be handled by a server side handler registered under the name specified by parameter <code>remoteHandlerName</code>.<br>
    * <br>
    * Note: Make sure that interface classes are designed to be fully serializable (parameters, return values and exceptions), since all method calls  
    * will translated to messages that will be serialized to a byte stream.
    * 
    * @param classLoader the class loader to define the proxy class. If null, the class loader of the first interface class will be used. 
    * @param interfaceClasses the Class objects representing the interfaces which the proxy instance is to implement.
    * @param remoteHandlerName the name of the server side handler to which RPC calls should be sent. If null, the default handler is used.
    * @param throwRealException flag indicating if the actual exceptions that occur should be thown in method calls, whenever possible. If this flag is set 
    * to false, a {@link RpcExceptionWrapper} will be thrown (due to the fact that {@link RpcException} is a checked exception). If however the interface defines 
    * {@link RpcException} (or sub class) in the throws clause of a method, that exception will be thrown regardless of the state of this flag. 
    * 
    * @return an RPC proxy instance for the specified interfaces.
    * 
    * @throws NullPointerException if interfaceClasses is null.
    * @throws IllegalArgumentException if a proxy class could not be created for parameter interfaceClass. See java.lang.reflect.Proxy for details.
    * 
    * @see java.lang.reflect.Proxy
    * 
    * @since 2.0.1 (20050210)
    */
   public Object createProxy(ClassLoader classLoader, final Class[] interfaceClasses, final String remoteHandlerName, final boolean throwRealException) throws NullPointerException, IllegalArgumentException
   {
      if( interfaceClasses.length == 0 ) throw new IllegalArgumentException("Length of argument interfaceClasses is 0!");
      if( classLoader == null ) classLoader = interfaceClasses[0].getClassLoader();
      Object proxy = Proxy.newProxyInstance(classLoader, interfaceClasses, this);
            
      if( remoteHandlerName != null )
      {
         synchronized(proxyHandlerNameMap)
         {
            proxyHandlerNameMap.put(proxy, remoteHandlerName);
         }
      }
      
      if( throwRealException )
      {
         synchronized(proxyThrowRealException)
         {
            proxyThrowRealException.add(proxy);
         }
      }
      
      return proxy;
   }
   
   /**
    * Processes a method invocation on a proxy by making an RPC call using the method name of the 
    * specified Method object and the parameters specified by parameter args. If the proxy class was registered 
    * with a remote handler name (see {@link #createProxy(Class, String)}, the handler name will be prepended to 
    * the method name ("myHandler.myMethod").<br>
    * <br>
    * This method will be invoked on an invocation handler when a method is invoked on a proxy instance that it 
    * is associated with. This implementation simply invokes {@link #invoke(MessageHeader, Object, Method, Object[])}, using the 
    * header retuned by {@link #getRpcMessageHeader()}.<br>
    * <br>
    *
    * @param proxy the proxy instance that the method was invoked on.
    *
    * @param method the <code>Method</code> instance corresponding to
    * the interface method invoked on the proxy instance.
    *
    * @param args an array of objects containing the values of the
    * arguments passed in the method invocation on the proxy instance,
    * or <code>null</code> if interface method takes no arguments.
    *
    * @return the value to return from the method invocation on the
    * proxy instance. 
    *
    * @throws Throwable if an RCP error occurs. 
    * 
    * @see java.lang.reflect.InvocationHandler
    * @see java.lang.reflect.Proxy
    * 
    * @since 2.0.1 (20050210)
    */
   public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
   {
      return invoke(this.getRpcMessageHeader(), proxy, method, args);
   }
   
   /**
    * Processes a method invocation on a proxy by making an RPC call using the method name of the 
    * specified Method object and the parameters specified by parameter args. If the proxy class was registered 
    * with a remote handler name (see {@link #createProxy(Class, String)}, the handler name will be prepended to 
    * the method name ("myHandler.myMethod").<br>
    * <br>
    * This method is provided for alternate dynamic proxy invocation handler implementations or for other implementations that wish 
    * to utilize the invocation and exception handling provided by this method.<br>
    * <br>
    *
    * @param header the header to be used when dispatching the message.
    * @param proxy the proxy instance that the method was invoked on.
    *
    * @param method the <code>Method</code> instance corresponding to
    * the interface method invoked on the proxy instance.
    *
    * @param args an array of objects containing the values of the
    * arguments passed in the method invocation on the proxy instance,
    * or <code>null</code> if interface method takes no arguments.
    *
    * @return the value to return from the method invocation on the
    * proxy instance. 
    *
    * @throws Throwable if an RCP error occurs. 
    * 
    * @see java.lang.reflect.InvocationHandler
    * @see java.lang.reflect.Proxy
    * 
    * @since 2.0.1 (20050210)
    */
   public Object invoke(final MessageHeader header, final Object proxy, final Method method, final Object[] args) throws Throwable
   {
      if( method.getDeclaringClass() != Object.class ) // Don't make RPC calls for java.lang.Object methods! 
      {
         String remoteHandlerName = null;
         synchronized(proxyHandlerNameMap)
         {
            if( this.proxyHandlerNameMap != null )
            {
               remoteHandlerName = (String)this.proxyHandlerNameMap.get(proxy);
            }
         }
         String methodName = method.getName();
         if( remoteHandlerName != null ) methodName = remoteHandlerName + "." + methodName;
         
         try
         {
            return this.invokeInternal(header, methodName, args);
         }
         catch(RpcException rpce)
         {
            Throwable cause = rpce.getCause();
            boolean mayThrowRpcException = false;
            boolean mayThrowRealException = false;
            boolean proxyThrowRealExceptionExabled = false;
            synchronized(proxyThrowRealException)
            {
               proxyThrowRealExceptionExabled = this.proxyThrowRealException.contains(proxy);
            }
            
            // If proxy may throw real exceptions and cause is runtime exception...
            if( proxyThrowRealExceptionExabled && (cause != null) && (cause instanceof RuntimeException) && !(cause instanceof MessagingException) )
            {
               mayThrowRealException = true;
            }
            
            // Check declared exceptions for method
            Class[] exceptionTypes = method.getExceptionTypes();
            if( exceptionTypes != null )
            {
               for(int i=0; i<exceptionTypes.length; i++)
               {
                  // Is RpcException (or subclass) explicitly defined in the throws clause?
                  if( RpcException.class.isAssignableFrom(exceptionTypes[i]) )
                  {
                     mayThrowRpcException = true;
                     break;
                  }
                  // Is cause exception declared for method in interface
                  if( proxyThrowRealExceptionExabled && !mayThrowRealException && 
                        (cause != null) && exceptionTypes[i].isAssignableFrom(cause.getClass()) )
                  {
                     mayThrowRealException = true;
                     break;
                  }
               }
            }
                        
            if( mayThrowRpcException )
            {
               throw rpce;
            }
            else if( mayThrowRealException )
            {
               throw cause;
            }
            else
            {
               throw new RpcExceptionWrapper(rpce);
            }
         }
      }
      else // Redirect java.lang.Object methods to Class object of proxy!
      {
         return method.invoke(proxy.getClass(), args);
      }
   }
}
