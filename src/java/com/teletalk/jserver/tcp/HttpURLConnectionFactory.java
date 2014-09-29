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

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * Factory for creating custom HttpURLConnection objects.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.1 Build 690
 */
public interface HttpURLConnectionFactory
{
   /**
    * Creates an HttpURLConnection object for the specified URL.
    * 
    * @param url the URL to create an HttpURLConnection.
    */
   public HttpURLConnection getHttpURLConnection(URL url) throws IOException; 
}
