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

import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggingEvent;

import com.teletalk.jserver.JServerConstants;
import com.teletalk.jserver.rmi.remote.RemoteLoggingEvent;

/**
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0
 */
public class RmiAppender extends AppenderSkeleton
{
   private final RemoteRmiClientProxy remoteRmiClientProxy;
   
   private final String associatedLoggerName;
   
   
   /**
    */
   public RmiAppender(final RemoteRmiClientProxy remoteRmiClientProxy, final String name, final String associatedLoggerName)
   {
      this.remoteRmiClientProxy = remoteRmiClientProxy;
            
      super.setName("RmiAppender(" + name + ")");
      
      this.associatedLoggerName = associatedLoggerName;
   }
   
   /**
    */
   protected void append(final LoggingEvent event)
   {
      if( !this.remoteRmiClientProxy.isDestroyed() )
      {
         if( (this.associatedLoggerName == null) || // View all loggers
               this.associatedLoggerName.equals(event.getLoggerName()) ) // Only create RemoteLoggingEvent for events from the associated logger
         {
            this.remoteRmiClientProxy.queueMessage(new RemoteLoggingEvent(event));
         }
      }
      else
      {
         if( this.associatedLoggerName == null ) // View all loggers
         {
            Logger.getRootLogger().removeAppender(this); // Remove from root logger...
            Logger.getLogger(JServerConstants.JSERVER_TOP_SYSTEM_ALIAS).removeAppender(this); // ...and JServer logger (not additive)
         }
         else Logger.getLogger(associatedLoggerName).removeAppender(this);
      }
   }
   
   /**
    */
   public void close()
   {
   }
   
   /**
    */
   public boolean requiresLayout()
   {
      return false;
   }
}