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

import java.util.List;

/**
 * Interface for classes that provide services that are to be registered in an SNS.  
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20050502)
 * 
 * @see com.teletalk.jserver.net.sns.client.SnsClientManager
 */
public interface ServiceProvider
{
   /**
    * Gets a list containing the service names (java.lang.String objects) provided by this service provider.
    */
   public List getProvidedServices();
   
   /**
    * Gets the addresses (list of java.lang.String objects) for the service with the specified name.
    */
   public List getProvidedServiceAddresses(String service);
}
