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

import java.security.cert.X509Certificate;

import javax.net.ssl.X509TrustManager;

/**
 * Trust manager implementation that allows everything.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.2
 */
public class NaiveX509TrustManager implements X509TrustManager
{
   /**
    * Return an array of certificate authority certificates which are trusted for authenticating peers.
    */
   public X509Certificate[] getAcceptedIssuers() 
   {
       return new X509Certificate[0];
   }
   
   /**
    * Checks certificate chain can be trusted. This method allows all chains.
    */
   public void checkClientTrusted(X509Certificate[] certs, String authType) 
   {
   }
   
   /**
    * Checks certificate chain can be trusted. This method allows all chains.
    */
   public void checkServerTrusted(X509Certificate[] certs, String authType) 
   {
   }
}
