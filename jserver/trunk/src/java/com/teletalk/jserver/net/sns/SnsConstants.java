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
package com.teletalk.jserver.net.sns;

/**
 * SNS related constants.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20050502)
 */
public class SnsConstants
{
   /**
    * Constant used by SNS clients for the messaging meta data key of the service of the client (java.util.List object). 
    * This constant is also used by the SNSManager to determine if a destination is an SNS client. This means that this 
    * key should always be present in the meta data of the client.   
    */
   public static final String SNS_SERVICES_META_DATA_KEY = "SnsServices";

   /**
    * Prefix used along with the name of a server (SNS client) when registering an administration proxy 
    * ({@link com.teletalk.jserver.tcp.messaging.admin.ServerAdministrationInterface}) for that server 
    * as an RPC handler in the message receiver of the SNSManager (see {@link #SNS_SERVER_MESSAGE_HANDLER_NAME}). 
    */
   public static final String SNS_ADMINISTRATION_PROXY_RPC_HANDLER_PREFIX = "adminProxy.";
   
   /**
    * The name with wich the SNSManager registers a RPC enabled message receiver, implementing the methods of the interface {@link com.teletalk.jserver.net.sns.server.SnsServer}.  
    */
   public static final String SNS_SERVER_MESSAGE_HANDLER_NAME = "sns.server";
   
   /**
    * The name with wich the SNSClientManager registers a RPC enabled message receiver, implementing the methods of the interface {@link com.teletalk.jserver.net.sns.client.SnsClient}.  
    */
   public static final String SNS_CLIENT_MESSAGE_HANDLER_NAME = "sns.client";   
}
