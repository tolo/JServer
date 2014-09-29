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
package com.teletalk.jserver.tcp;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLSession;

/**
 * A host name verifier that disregards host name mismatches.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.2
 */
public class NaiveHostnameVerifier implements HostnameVerifier
{
   /**
    * Verify that the host name is an acceptable match with the server's authentication scheme. This method always returns true.
    */
   public boolean verify(String hostName, SSLSession s)
   {
      return true;
   }
}
