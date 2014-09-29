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
import java.io.InputStream;

import org.mortbay.io.EndPoint;
import org.mortbay.io.bio.StreamEndPoint;
import org.mortbay.jetty.Connector;
import org.mortbay.jetty.Handler;
import org.mortbay.jetty.HttpConnection;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.handler.ContextHandlerCollection;
import org.mortbay.jetty.handler.DefaultHandler;
import org.mortbay.jetty.handler.HandlerCollection;
import org.mortbay.jetty.webapp.WebAppContext;

import com.teletalk.jserver.StatusTransitionException;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.net.admin.AdministrationManager;
import com.teletalk.jserver.property.StringProperty;
import com.teletalk.jserver.tcp.messaging.Message;
import com.teletalk.jserver.tcp.messaging.MessagingManager;
import com.teletalk.jserver.tcp.messaging.admin.web.CustomAdministrationWebAppInterface;
import com.teletalk.jserver.tcp.messaging.rpc.RpcInputStream;
import com.teletalk.jserver.util.SpillOverByteArrayOutputStream;

/**
 * Jetty based implementation of web/HTTP based custom (server specific) administration of the server.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1.6 (20070503)
 */
public class CustomAdministrationWebAppHandler extends SubComponent implements CustomAdministrationWebAppInterface
{
   private Server server = null;
   
   private WebAppContext adminCustomContext = null;
   
   private final StringProperty customAdministrationWar;
   
   
   public CustomAdministrationWebAppHandler()
   {
      this(null, "AdministrationWebAppHandler");
   }
      
   public CustomAdministrationWebAppHandler(SubComponent parent, String name)
   {
      super(parent, name);
      
      this.customAdministrationWar = new StringProperty(this, "customAdministrationWar", "./webapps/adminCustom/", StringProperty.MODIFIABLE_OWNER_RESTART);
      this.customAdministrationWar.setDescription("The WAR (file or expanded directory) containing the custom administration web application.");
      super.addProperty(this.customAdministrationWar);
      
      AdministrationManager.getAdministrationManager().addAdministrationHandler(
               CustomAdministrationWebAppInterface.ADMINISTRATION_HANDLER_NAME, this);     
   }
   
   protected void doInitialize()
   {
      super.doInitialize();
      
      try
      {
         if( this.server == null) this.server = new Server();
         
         HandlerCollection handlers = new HandlerCollection();
         ContextHandlerCollection contexts = new ContextHandlerCollection();
         handlers.setHandlers(new Handler[]{contexts,new DefaultHandler()});
         server.setHandler(handlers);
         
         this.adminCustomContext = new WebAppContext();
         this.adminCustomContext.setWar(this.customAdministrationWar.stringValue());
         this.adminCustomContext.setContextPath("/adminCustom");
         this.adminCustomContext.setParentLoaderPriority(true);
         contexts.addHandler(this.adminCustomContext);
         if (this.adminCustomContext.isStarted()) this.adminCustomContext.start();

         if( !this.server.isStarted() ) this.server.start();
      }
      catch (Exception e) 
      {
         super.logError("Error creating AdministrationWebAppHandler!", e);
         throw new StatusTransitionException("Error creating AdministrationWebAppHandler!", e);
      } 
   }
   
   public InputStream handle(final InputStream requestInputStream) throws IOException
   {
      Message message = MessagingManager.getCurrentMessage();
      String virtualContextPath = (String)message.getHeader().getCustomHeaderField(CustomAdministrationWebAppInterface.VIRTUAL_CONTEXT_PATH_HEADER_KEY);
      
      try
      {
         SpillOverByteArrayOutputStream outputStream = new SpillOverByteArrayOutputStream();
         
         EndPoint endPoint = new StreamEndPoint(requestInputStream, outputStream);
         Connector connector = new SingleEndPointConnector(endPoint);
         connector.setServer(this.server);
         connector.start();
         
         HttpConnection httpConnection = new VirtualHttpConnection(connector, endPoint, server, this.adminCustomContext, virtualContextPath);
         httpConnection.handle();
         
         return new RpcInputStream(outputStream.getInputStream(), outputStream.size());
      }
      catch (Exception e) 
      {
         throw new IOException("Error handling request (" + e + ")!", e);
      }
   }
}
