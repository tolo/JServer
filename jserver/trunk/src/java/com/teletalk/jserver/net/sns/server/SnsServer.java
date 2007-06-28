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
package com.teletalk.jserver.net.sns.server;

import java.util.List;

import com.teletalk.jserver.net.sns.Service;

/**
 * The inteface of an SNS server which is available for remote (RPC) access. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20050502)
 */
public interface SnsServer
{
   /** 
    * The version of the interface.<br>
    * <br>
    * <b>Version 2</b>:<br>
    * * Routing of administration messages expanded to allow full access to components in a remote server.<br> 
    * <br>
    */
   public static final int INTERFACE_VERSION = 2;
   
   /**
    * Gets the version of this interface.
    *  
    * @since 2.1.3 (20060324)
    */
   public int getInterfaceVersion();
   
   /**
    * Gets a list of all servers connected to the SNS server.
    */
   public List getServers();
   
   /**
    * Gets a list of all servers connected to the SNS server, containing the service with the specified name.
    */
   public List getServers(String serviceName);
   
   /**
    * Gets a list of all available services in the SNS server.
    */
   public List getServices();
   
   /**
    * Gets the service with the specified name from the SNS server.
    */
   public Service getService(String serviceName);
}
