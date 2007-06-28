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

/**
 * Interface for classes that wish to be notified when services available though SNS:s are updated.  
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20050502)
 * 
 * @see com.teletalk.jserver.net.sns.client.SnsClientManager
 */
public interface ServiceListener
{
   /**
    * Called when SNS services have been updated. 
    * 
    * @param services map containing service name (java.lang.String to {@link com.teletalk.jserver.net.sns.Service} mappings.
    */
   public void servicesUpdated(HashMap services);
}
