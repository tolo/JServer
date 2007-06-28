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
package com.teletalk.jserver.log;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.apache.log4j.spi.LoggingEvent;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.JServerConstants;
import com.teletalk.jserver.SubComponent;

/**
 * Logger class to be used as logger for subcomponents. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0
 */
public class ComponentLogger extends Logger
{
   private volatile boolean ownerNull;
   
   private SubComponent owner;

   /**
    * Creates a new ComponentLogger.
    */
   public ComponentLogger(String name, SubComponent owner)
   {
      super(name);
      this.owner = owner;
      this.ownerNull = (owner == null);
   }
   
   /**
    * Gets the top system logger ({@link JServerConstants#JSERVER_TOP_SYSTEM_ALIAS}).
    */
   public static ComponentLogger getTopSystemLogger()
   {
      ComponentLogger componentLogger = (ComponentLogger)ComponentLogger.getLogger(JServerConstants.JSERVER_TOP_SYSTEM_ALIAS, ComponentLoggerFactory.getDefaultFactory());
      componentLogger.setAdditivity(false);
      return componentLogger;
   }
   
   /**
    * Gets the owner of this logger.
    */
   private void getOwner()
   {
      synchronized(this)
      {
         if( owner == null )
         {
            JServer jserver = JServer.getJServer();
            if( jserver != null )
            {
               owner = jserver.findSubComponent(super.getName());
            }
            
            this.ownerNull = (owner == null);
         }
      } 
   }
   
   /**
    * Forces the specified level.
    */
   public void forceLevel(Level level)
   {
      super.setLevel(level);
   }
   
   /**
    * Sets the specified level.
    */
   public void setLevel(Level level)
   {
     if( ownerNull ) this.getOwner();      
     
      if( !ownerNull ) owner.setLogLevel(level);
      else super.setLevel(level);
   }
   
   /**
    * Sets the specified level.
    */
   public void setPriority(Priority priority)
   {
      if( ownerNull ) this.getOwner();
      
      if( !ownerNull ) owner.setLogLevel(priority.toInt());
      else super.setPriority(priority);
   }
   
   /**
    * Logs a LoggingEvent.
    */
   public void log(final LoggingEvent loggingEvent) 
   {
      if (super.repository.isDisabled(loggingEvent.getLevel().toInt()) )
      {
         return;
      }
      if (loggingEvent.getLevel().isGreaterOrEqual(super.getEffectiveLevel()))
      {
         super.callAppenders(loggingEvent);
      }
   }
}
