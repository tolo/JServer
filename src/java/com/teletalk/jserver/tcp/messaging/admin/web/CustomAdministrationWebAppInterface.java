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
package com.teletalk.jserver.tcp.messaging.admin.web;

import java.io.IOException;
import java.io.InputStream;

/**
 * Interface for web/HTTP based custom (server specific) administration of the server.
 *   
 * @author Tobias Löfstrand
 * 
 * @since 2.1.6 (20070503)
 */
public interface CustomAdministrationWebAppInterface
{
   public static final String ADMINISTRATION_HANDLER_NAME = "webadmin";
   
   public static final String VIRTUAL_CONTEXT_PATH_HEADER_KEY = "virtualContextPath";
      
   
   /**
    * Handles a custom administration HTTP request. A virtual context path may be specified in a custom header field in 
    * the message, under the key {@link #VIRTUAL_CONTEXT_PATH_HEADER_KEY}.
    */
   public InputStream handle(final InputStream httpRequestInputStream) throws IOException;
}
