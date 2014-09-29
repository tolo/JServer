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

import org.apache.log4j.spi.LoggingEvent;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.log.LogManager;


/**
 * 
 * @since 2.0
 */
public class RemoteLoggingEvent extends RemoteEvent
{
   static final long serialVersionUID = 3840458387991454439L;

   /**
    */
   public RemoteLoggingEvent(final LoggingEvent loggingEvent)
   {
      super(loggingEvent);
      
      JServer jServer = JServer.getJServer();
      LogManager logManager = (jServer != null) ? jServer.getLogManager() : null;
      if( logManager != null ) logManager.prepareLoggingEvent(loggingEvent);
      else LogManager.prepareLoggingEvent(loggingEvent, false);
   }
   
   /**
    */
   public LoggingEvent getLoggingEvent()
   {
      return (LoggingEvent)super.param;
   }
   
   /**
    * Returns a String object representing of this RemoteLogEvent.
    * 
    * @return a String representation of this RemoteLogEvent.
    */
   public String toString()
   {
      return "RemoteLoggingEvent(" + getLoggingEvent();
   }
}
