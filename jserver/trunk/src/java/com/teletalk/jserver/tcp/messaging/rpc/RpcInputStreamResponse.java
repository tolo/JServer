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

import java.io.InputStream;

import com.teletalk.jserver.tcp.messaging.Message;

/**
 * RPC Input stream wrapper class for sending a java.io.InputStream in a RCP method call.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0.1 (20040924)
 * 
 * @deprecated as of 2.1 (20050912), replaced by {@link com.teletalk.jserver.tcp.messaging.rpc.RpcInputStream}
 */
public class RpcInputStreamResponse extends RpcInputStream
{
   public static final String RPC_INPUTSTREAM_RESPONSE_HEADER_KEY = "com.teletalk.jserver.tcp.messaging.rpc.RpcInputStreamResponse";
      
   /**
    * Creates a RpcInputStreamResponse for return of a java.io.InputStream object in a RCP method call on the server side. 
    */
   public RpcInputStreamResponse(final InputStream inputStream, final long dataLength)
   {
      super(inputStream, dataLength);
   }
   
   /**
    * Creates a RpcInputStreamResponse for return of a java.io.InputStream object in a RCP method call on the client side.
    */
   public RpcInputStreamResponse(final Message message)
   {
      super(message);
   }
}
