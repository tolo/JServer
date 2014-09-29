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

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import org.mortbay.io.EndPoint;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.HttpException;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.webapp.WebAppContext;
import org.mortbay.util.URIUtil;

/**
 * HttpConnection implementation representing a virtual HttpConnection, used for executing a HTTP request received 
 * via messaging in a specific web application context.  
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1.6 (20070503)
 */
public class VirtualHttpConnection extends HttpConnection
{
   private final WebAppContext context;
   private final String virtualContextPath;
   
   public VirtualHttpConnection(Connector connector, EndPoint endpoint, Server server, WebAppContext context, String virtualContextPath)
   {
      super(connector, endpoint, server);
      this.context = context;
      this.virtualContextPath = virtualContextPath;
   }
   
   protected void handleRequest() throws IOException
   {
      try
      {
         String info = URIUtil.canonicalPath(super._uri.getDecodedPath());
         if (info==null) throw new HttpException(400);
         
         info = info.substring(this.virtualContextPath.length());
         super._request.setPathInfo(info);

         HttpServletRequest request = super._request;
         if( this.virtualContextPath != null )
         {
            request = new VirtualHttpServletRequestWrapper(super._request, this.virtualContextPath);
         }

         this.context.handle(info, request, _response, Handler.FORWARD);
      }
      catch (Exception e) 
      {
         throw new IOException("Error handling request (" + e + ")!", e);
      }
   }
}
