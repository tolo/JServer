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
package com.teletalk.jserver.tcp.http.xmlrpc;

import org.apache.xmlrpc.XmlRpcHandlerMapping;
import org.apache.xmlrpc.XmlRpcRequestProcessor;
import org.apache.xmlrpc.XmlRpcResponseProcessor;
import org.apache.xmlrpc.XmlRpcWorker;

/**
 * Custom XML-RPC worker class that exposes the request and response processors.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.1 build 690
 */
public class XmlRpcProcessor extends XmlRpcWorker
{
   /**
    * Creates a new XmlRpcProcessor.
    */
   public XmlRpcProcessor(XmlRpcHandlerMapping mapping)
   {
      super(mapping);
   }
   
   /**
    * Gets the request processor.
    */
   public XmlRpcRequestProcessor getXmlRpcRequestProcessor()
   {
      return super.requestProcessor; 
   }
   
   /**
    * Gets the response processor.
    */
   public XmlRpcResponseProcessor getXmlRpcResponseProcessor()
   {
      return super.responseProcessor; 
   }
}
