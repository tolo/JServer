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

import java.util.HashMap;

import com.teletalk.jserver.net.sns.Service;

/**
 * The inteface of an SNS client which is available for remote (RPC) access.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20050502)
 */
public interface SnsClient
{
   /** 
    * The version of the interface.
    */
   public static final int INTERFACE_VERSION = 1;
   
   /**
    * Gets the version of this interface.
    *  
    * @since 2.1.3 (20060324)
    */
   public int getInterfaceVersion();
   
   /**
    * Called by an SNS to perform a complete update of the available services.
    */
   public void serviceRegistryRefresh(HashMap services);
   
   /**
    * Called by an SNS when a service is updated.
    */
   public void serviceUpdated(Service service);
}
