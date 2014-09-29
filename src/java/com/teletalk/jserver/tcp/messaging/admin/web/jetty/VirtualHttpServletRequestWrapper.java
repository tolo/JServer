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
package com.teletalk.jserver.tcp.messaging.admin.web.jetty;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;

/**
 * Servlet request wrapper with the ability to expose a virtual context path.
 * 
 * @author Tobias L�fstrand
 * 
 * @since 2.1.6 (20070503)
 */
public class VirtualHttpServletRequestWrapper extends HttpServletRequestWrapper
{
   private String virtualContextPath;

   public VirtualHttpServletRequestWrapper(HttpServletRequest request, String virtualContextPath)
   {
      super(request);
      this.virtualContextPath = virtualContextPath;
   }
   
   public String getContextPath()
   {
      return virtualContextPath;
   }
}
