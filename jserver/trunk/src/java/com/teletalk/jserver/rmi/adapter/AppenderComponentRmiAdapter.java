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
package com.teletalk.jserver.rmi.adapter;

import java.rmi.RemoteException;

import com.teletalk.jserver.log.AppenderComponent;
import com.teletalk.jserver.log.LogData;
import com.teletalk.jserver.rmi.remote.RemoteAppenderComponent;
import com.teletalk.jserver.rmi.remote.RemoteInputStream;
import com.teletalk.jserver.rmi.remote.RemoteInputStreamImpl;

/**
 * Appender component RMI adapter
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0
 */
public class AppenderComponentRmiAdapter extends SubComponentRmiAdapter implements RemoteAppenderComponent
{
   static final long serialVersionUID = -1931846722764595685L;
   
   private final AppenderComponent appenderComponent;
   
   /**
    * Creates a new AppenderComponentRmiAdapter.
    */
   public AppenderComponentRmiAdapter(AppenderComponent appenderComponent) throws RemoteException
   {
      super(appenderComponent);
      
      this.appenderComponent = appenderComponent;      
   }
   
   /**
    * Gets the logs.
    */
   public LogData[] getLogs() throws RemoteException
   {
      return this.appenderComponent.getLogs();
   }

   /**
    * Gets a log as a stream.
    */
   public RemoteInputStream getLogAsStream(String log) throws RemoteException
   {
      return new RemoteInputStreamImpl(this.appenderComponent.getLogAsStream(log));
   }
}
