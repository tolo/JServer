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
package com.teletalk.jserver.net.acl;

/**
 * Interface for classes implementing mechanisms for performing (network)interface based access checks on operations/messages. 
 * Interfaces for which access control checks are to be peformed on are represented as simple strings. These strings may for instance 
 * be associated with ip-adresses as value ids (see {@link com.teletalk.jserver.property.IpAndPortProperty} or 
 * {@link com.teletalk.jserver.property.MultiValueProperty}.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0.2 (20050331)
 */
public interface AccessControlListHandler
{
   /**
    * Checks if access control is enabled or not.
    */
   public boolean isAccessControlCheckEnabled();
   
   /**
    * Checks if the specified interfaces has a specific operation permission. If the access control 
    * is disabled (@see {@link #isAccessControlCheckEnabled()}) this method should always return <code>true</code>. 
    * 
    * @param interfaceName the name of the interface. 
    * @param operationId the id of a permission.
    * 
    * @return <code>true</code> if the interface has permission, otherwise <code>false</code>. This method also 
    * returns <code>true</code> is access control check is disabled.
    */
   public boolean checkAccess(String interfaceName, String operationId);
   
   /**
    * Checks if the specified interface has administration permission.
    * 
    * @since 2.1 (20050427)
    * 
    * @return <code>true</code> if the interface has permission, otherwise <code>false</code>. This method also 
    * returns <code>true</code> is access control check is disabled.
    */
   public boolean hasServerAdministrationAccess(String interfaceName);
}
