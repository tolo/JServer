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
package com.teletalk.jserver.rmi.remote;

import java.rmi.Remote;
import java.rmi.RemoteException;

import org.apache.log4j.spi.Filter;


/**
 * 
 * @since 2.0
 */
public interface RemoteJServerRmiInterface extends Remote
{
   public RemoteJServer getRemoteJServer() throws RemoteException;
   
   public void disconnect() throws RemoteException;

   /**
    * Method to get the <code>java.rmi.server.codebase</code> property used by the remote server.
    * 
    * @return the value of the <code>java.rmi.server.codebase property</code>.
    */
   public String getRmiCodeBase() throws RemoteException;
   
   /**
    * Performs an alive check.
    */
   public void aliveCheck() throws RemoteException;
      
   //public int getProtocolVersion() throws RemoteException;
   
   
   public String[] listLoggers() throws RemoteException;
      
   public void addAppender(String loggerName) throws RemoteException;
   
   public void removeAppender(String loggerName) throws RemoteException;
   
   public void removeAllAppenders() throws RemoteException;
   
   public void addAppenderFilter(String loggerName, Filter filter) throws RemoteException;
   
   //public void removeAppenderFilter(String loggerName, Filter filter) throws RemoteException;
   
   public void clearAppenderFilters(String loggerName) throws RemoteException;
}
