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
import java.rmi.server.UnicastRemoteObject;

import org.apache.log4j.spi.Filter;

import com.teletalk.jserver.rmi.remote.RemoteJServer;
import com.teletalk.jserver.rmi.remote.RemoteJServerRmiInterface;

/**
 * Server side RMI object representing the client's view of the server... 
 * 
 */
public class JServerRmiInterface extends UnicastRemoteObject implements RemoteJServerRmiInterface
{
   static final long serialVersionUID = 4656213019791015543L;

   private final RemoteRmiClientProxy remoteRmiClientProxy;
   
   /**
    * 
    * @param remoteRmiClientProxy
    * @throws RemoteException
    */
   public JServerRmiInterface(RemoteRmiClientProxy remoteRmiClientProxy) throws RemoteException
   {
      super(RmiManager.getRmiManager().getExportAddresses()[0].getPort());
      
      this.remoteRmiClientProxy = remoteRmiClientProxy;
   }
   
   /* (non-Javadoc)
    * @see com.teletalk.jserver.rmi.remote.RemoteJServerRmiInterface#getRemoteJServer()
    */
   public RemoteJServer getRemoteJServer() throws RemoteException
   {
      return this.remoteRmiClientProxy.getParent().getTopsystemAdapter();
   }
   
   /* (non-Javadoc)
    * @see com.teletalk.jserver.rmi.remote.RemoteJServerRmiInterface#disconnect()
    */
   public void disconnect() throws RemoteException
   {
      this.remoteRmiClientProxy.disconnect();
   }
   
   /**
    * Method to get the <code>java.rmi.server.codebase</code> property used by this server.
    * 
    * @return the value of the <code>java.rmi.server.codebase property</code>.
    * 
    * @since 2.0
    */ 
   public String getRmiCodeBase() throws RemoteException
   {
      return System.getProperty("java.rmi.server.codebase");
   }
   
   /**
    * Performs an alive check.
    */
   public void aliveCheck() throws RemoteException
   {
      try
      {
         synchronized(this)
         {
            wait(5*1000);  // Wait 5 seconds per alive check
         }
      }
      catch(Exception e){}
   }
   
   /**
    */
   /*public int getProtocolVersion()
   {
      return JServerRmiHost.PROTOCOL_VERSION;
   }*/
   
   
   public String[] listLoggers() throws RemoteException
   {
      return this.remoteRmiClientProxy.listLoggers();
   }
     
   /* (non-Javadoc)
    * @see com.teletalk.jserver.rmi.remote.RemoteJServerRmiInterface#addAppender(java.lang.String)
    */
   public void addAppender(String loggerName) throws RemoteException
   {
      this.remoteRmiClientProxy.addAppender(loggerName);
   }
   
   /* (non-Javadoc)
    * @see com.teletalk.jserver.rmi.remote.RemoteJServerRmiInterface#removeAppender(java.lang.String)
    */
   public void removeAppender(String loggerName) throws RemoteException
   {
      this.remoteRmiClientProxy.removeAppender(loggerName); 
   }
   
   /**
    * 
    * @throws RemoteException
    */
   public void removeAllAppenders() throws RemoteException
   {
      this.remoteRmiClientProxy.removeAllAppenders();
   }
   
   /* (non-Javadoc)
    * @see com.teletalk.jserver.rmi.remote.RemoteJServerRmiInterface#addAppenderFilter(java.lang.String, org.apache.log4j.spi.Filter)
    */
   public void addAppenderFilter(String loggerName, Filter filter) throws RemoteException
   {
      this.remoteRmiClientProxy.addAppenderFilter(loggerName, filter);
   }
   
   /* (non-Javadoc)
    * @see com.teletalk.jserver.rmi.remote.RemoteJServerRmiInterface#removeAppenderFilter(java.lang.String, org.apache.log4j.spi.Filter)
    */
   /*public void removeAppenderFilter(String loggerName, Filter filter) throws RemoteException
   {
      this.remoteRmiClientProxy.removeAppenderFilter(loggerName, filter);
   }*/
   
   /* (non-Javadoc)
    * @see com.teletalk.jserver.rmi.remote.RemoteJServerRmiInterface#clearAppenderFilters(java.lang.String)
    */
   public void clearAppenderFilters(String loggerName) throws RemoteException
   {
      this.remoteRmiClientProxy.clearAppenderFilters(loggerName);
   }
}
