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
package com.teletalk.jserver.rmi;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Appender;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.Filter;

import com.teletalk.jserver.JServerConstants;
import com.teletalk.jserver.rmi.remote.RemoteEvent;
import com.teletalk.jserver.rmi.remote.RemoteLoggingEvent;
import com.teletalk.jserver.rmi.remote.RemoteRmiClient;
import com.teletalk.jserver.util.MessageQueueThread;

/**
 * Server side representation of a remote rmi client.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0
 */
public class RemoteRmiClientProxy extends MessageQueueThread implements RemoteRmiClient
{
   private JServerRmiHost parent;
   
   private RemoteRmiClient remoteRmiClient;
   
   private JServerRmiInterface jServerRmiInterface;
   
   private final String id;
   
   
   private final int protocolVersion;
   
   //private final ArrayList appenders;
   private final HashMap appenders;
   
   private boolean destroyed = false;
   
   /**
    * Creates a new RemoteRmiClientProxy.
    */
   public RemoteRmiClientProxy(final JServerRmiHost parent, final RemoteRmiClient remoteRmiClient, final int protocolVersion) throws RemoteException
   {
      super(parent.getFullName() + ".RemoteRmiClientProxy(" + remoteRmiClient.identify() + ")");
      
      this.parent = parent;
      this.remoteRmiClient = remoteRmiClient;
      this.jServerRmiInterface = new JServerRmiInterface(this);
      
      this.id = remoteRmiClient.identify();
      
      this.protocolVersion = protocolVersion;
      //this.appenders = new ArrayList();
      this.appenders = new HashMap();
   }
   
   /**
    */
   public void destroy()
   {
      super.destroy();
          
      destroyed = true;
      this.parent = null;
      this.remoteRmiClient = null;
      this.jServerRmiInterface = null;
      
      // Remove appenders
      this.removeAllAppenders();
   }
   
   /**
    * @return Returns the destroyed.
    */
   public boolean isDestroyed()
   {
      return this.destroyed;
   }
   
   /**
    * @return Returns the parent.
    */
   public JServerRmiHost getParent()
   {
      return parent;
   }
   
   /**
    * @return Returns the remoteRmiClient.
    */
   public RemoteRmiClient getRemoteRmiClient()
   {
      return remoteRmiClient;
   }
   
   /**
    * @return Returns the jServerRmiInterface.
    */
   public JServerRmiInterface getJServerRmiInterface()
   {
      return this.jServerRmiInterface;
   }
   
   public void disconnect()
   {
      this.parent.disconnectClient(this);
   }
   
   public String[] listLoggers() throws RemoteException
   {
      Enumeration loggerEnum = LogManager.getCurrentLoggers();
      ArrayList loggerNames = new ArrayList();
      Logger logger;
      
      while(loggerEnum.hasMoreElements())
      {
         logger = (Logger)loggerEnum.nextElement();
         if( logger != null )
         {
            loggerNames.add(logger.getName());
         }
      }
      
      Collections.sort(loggerNames);
      
      return (String[])loggerNames.toArray(new String[0]);
   }
   
   public void addAppender(final String loggerName)
   {
      synchronized(this.appenders)
      {
         if( !this.appenders.containsKey(loggerName) )
         {
            RmiAppender rmiAppender = new RmiAppender(this, this.id + "/" + loggerName, loggerName);
            
            if( loggerName == null ) // View all loggers
            {
               Logger.getRootLogger().addAppender(rmiAppender); // Add to root logger...
               Logger.getLogger(JServerConstants.JSERVER_TOP_SYSTEM_ALIAS).addAppender(rmiAppender); // ...and JServer logger (not additive)
            }
            else Logger.getLogger(loggerName).addAppender(rmiAppender);
            
            this.appenders.put(loggerName, rmiAppender);
         }
      }
   }
   
   public void removeAppender(final String loggerName)
   {
      synchronized(this.appenders)
      {
         RmiAppender rmiAppender = (RmiAppender)this.appenders.get(loggerName);
         
         if( rmiAppender != null )
         {
            if( loggerName == null ) // View all loggers
            {
               Logger.getRootLogger().removeAppender(rmiAppender); // Remove from root logger...
               Logger.getLogger(JServerConstants.JSERVER_TOP_SYSTEM_ALIAS).removeAppender(rmiAppender); // ...and JServer logger (not additive)
            }
            else Logger.getLogger(loggerName).removeAppender(rmiAppender);
            
            this.appenders.remove(loggerName);
         }
      }
   }
   
   public void removeAllAppenders()
   {
      synchronized(this.appenders)
      {
         Iterator loggerNames = this.appenders.keySet().iterator();
         String loggerName;
         
         while(loggerNames.hasNext())
         {
            loggerName = (String)loggerNames.next();
            Logger.getLogger(loggerName).removeAppender((Appender)this.appenders.get(loggerName));
         }
         
         appenders.clear();
      }
   }
   
   /* (non-Javadoc)
    * @see com.teletalk.jserver.rmi.remote.RemoteJServerRmiInterface#addAppenderFilter(java.lang.String, org.apache.log4j.spi.Filter)
    */
   public void addAppenderFilter(String loggerName, Filter filter) throws RemoteException
   {
      synchronized(this.appenders)
      {
         RmiAppender rmiAppender = (RmiAppender)this.appenders.get(loggerName);
         
         if( rmiAppender != null )
         {
            rmiAppender.addFilter(filter);
         }
      }
   }
   
   /* (non-Javadoc)
    * @see com.teletalk.jserver.rmi.remote.RemoteJServerRmiInterface#removeAppenderFilter(java.lang.String, org.apache.log4j.spi.Filter)
    */
   /*public void removeAppenderFilter(String loggerName, Filter filter) throws RemoteException
   {
      // FIXME:
      synchronized(this.appenders)
      {
         RmiAppender rmiAppender = (RmiAppender)this.appenders.get(loggerName);
         
         if( rmiAppender != null )
         {
            
            rmiAppender.getFilter(); // (filter);
         }
      }
   }*/
   
   /* (non-Javadoc)
    * @see com.teletalk.jserver.rmi.remote.RemoteJServerRmiInterface#clearAppenderFilters(java.lang.String)
    */
   public void clearAppenderFilters(String loggerName) throws RemoteException
   {
      synchronized(this.appenders)
      {
         RmiAppender rmiAppender = (RmiAppender)this.appenders.get(loggerName);
         
         if( rmiAppender != null )
         {
            rmiAppender.clearFilters();
         }
      }
   }
   
   /**
    */
   protected void handleMessage(final Object message)
   {
      if( message instanceof RemoteEvent )
      {
         RemoteEvent remoteEvent = (RemoteEvent)message;
         
         try
         {
            // Don't debug log RemoteLogEvents
            if( this.parent.getParent().isDebugMode() && !(remoteEvent instanceof RemoteLoggingEvent) )
            {  
               this.parent.getParent().logDebug(this.getName(), "Dispatching event (" + remoteEvent + ").");
            }
            
            this.remoteRmiClient.receiveEvent(remoteEvent);
         }
         catch(Exception e)
         {
            Exception error2 = null;
            
            // Retry once...
            try
            {
               this.remoteRmiClient.receiveEvent(remoteEvent);
            }
            catch(Exception e2)
            {
               error2 = e2;
            }
            
            if(error2 != null) // Another error occurred - disconnect
            {
               this.parent.disconnectClient(this);
               this.parent.getParent().logError(this.getName(), "Fatal error while sending event (" + remoteEvent + ") to client. Disconnecting! ", e);
            }
            else // Single/isolated error
            {
               this.parent.getParent().logError(this.getName(), "Got exception while sending event (" + remoteEvent + ") to client.", e);
            }
         }
      }
   }
   
   /**
    * 
    */
   public boolean equals(final Object obj)
   {
      if( obj instanceof RemoteRmiClientProxy )
      {
         return ( ((RemoteRmiClientProxy)obj).remoteRmiClient == this.remoteRmiClient );
      }
      
      return false;
   }
   
   /* #####  DELEGATE/OVERRIDDEN METHODS FOR RemoteRmiClient ##### */
   
   /**
    * Disconnects the client from the server.
    * 
    * @exception RemoteException if there was an error during remote access of this method.
    */
   public void disconnectedFromServer() throws RemoteException
   {
      remoteRmiClient.disconnectedFromServer();
   }
   
   /**
    * Identifies this client.
    * 
    * @return String object indentifying this client.
    * 
    * @exception RemoteException if there was an error during remote access of this method.
    */   
   public String identify() throws RemoteException
   {
      return this.id;
   }
   
   /**
    * Method for receiving events from the server.
    * 
    * @param event the event.
    * 
    * @exception RemoteException if there was an error during remote access of this method.
    */
   public void receiveEvent(RemoteEvent event) throws RemoteException
   {
      super.queueMessage(event);
   }
   
   /**
    * Checkes whether or not this client wants to receive events.
    * 
    * @return boolean value.
    * 
    * @exception RemoteException if there was an error during remote access of this method.
    */
   public boolean receiveEvents() throws RemoteException
   {
      return remoteRmiClient.receiveEvents();
   }
   
   /**
    * Gets the protocol version.
    * 
    * @exception RemoteException if there was an error during remote access of this method.
    * 
    * @since 2.0
    */
   public int getProtocolVersion() throws RemoteException
   {
      return this.protocolVersion;
   }
}
