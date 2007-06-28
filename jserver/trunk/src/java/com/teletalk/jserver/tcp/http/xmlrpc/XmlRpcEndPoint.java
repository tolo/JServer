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

import com.teletalk.jserver.tcp.http.HttpEndPoint;

/**
 * End point implementation for XML-RPC over HTTP communication.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.1 build 690
 */
public class XmlRpcEndPoint extends HttpEndPoint
{
	private final XmlRpcProcessor xmlRpcWorker;
	
	/**
	 * Creates  a new XmlRpcEndPoint.
	 * 
	 * @param commManager reference to a parent XmlRpcCommunicationManager.
	 */
	public XmlRpcEndPoint(final XmlRpcCommunicationManager commManager)
	{
		this(commManager, "XmlRpcEndPoint");
	}
	
	/**
	 * Creates  a new XmlRpcEndPoint.
	 * 
	 * @param commManager reference to a parent XmlRpcCommunicationManager.
	 * @param name the name of this XmlRpcEndPoint.
	 */
	public XmlRpcEndPoint(final XmlRpcCommunicationManager commManager, final String name)
	{
		super(commManager, name);
		
		this.xmlRpcWorker = new XmlRpcProcessor(commManager.getHandlerMapping());
	}
	
   /**
    * Gets the associated XmlRpcProcessor.
    */
   public XmlRpcProcessor getXmlRpcProcessor()
   {
      return xmlRpcWorker;
   }
}
