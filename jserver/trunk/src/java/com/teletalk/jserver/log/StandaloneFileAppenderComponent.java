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
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.varia.DenyAllFilter;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.spring.SpringApplicationContext;

/**
 * Standalone file appender component that is meant to be used outside the {@link LogManager}, to log 
 * specific log messages to a separate log file.<br>
 * <br>
 * This component may however be added to the {@link LogManager} in order to expose the log files generated by 
 * this component in administration interfaces. This can be achieved by setting the property 
 * {@link #setAddToLogManager(boolean) addToLogManager)} to true, or, if created in a {@link SpringApplicationContext Spring context}, 
 * by prefixing the bean name with "LogManager.".  
 * 
 * @author Tobias L�fstrand
 * 
 * @since 2.1.1 (20051229)
 */
public class StandaloneFileAppenderComponent extends FileAppenderComponent
{
   public static final String DEFAULT_LAYOUT_PATTERN = "%d %m%n";
   
   
   private boolean addToLogManager = false;
   
   
   /**
    */
   public StandaloneFileAppenderComponent()
   {
      this(null, null);
   }
   
   /**
    */
   public StandaloneFileAppenderComponent(SubComponent parent)
   {
      this(parent, null);
   }
   
   /**
    */
   public StandaloneFileAppenderComponent(SubComponent parent, String name)
   {
      super(parent, name);
      
      this.init();
   }

   /**
    */
   public StandaloneFileAppenderComponent(SubComponent parent, String name, String logFileNamePrefix, String logFileNameSuffix, String basePath, int periodicity)
   {
      super(parent, name, logFileNamePrefix, logFileNameSuffix, basePath, periodicity);
            
      this.init();
   }

   /**
    */
   private void init()
   {
      super.setDefaultLayoutPattern(DEFAULT_LAYOUT_PATTERN);
      super.setAutoFlushMode(true);
      // Create a DenyAllFilter to prevent the StandaloneFileAppenderComponent from receiving general log 
      // messages, if added to a LogManager.
      super.addFilter(new DenyAllFilter());
   }
   
   /**
    * @since 2.1.5 (20070205) 
    */
   public boolean isAddToLogManager()
   {
      return addToLogManager;
   }

   /**
    * @since 2.1.5 (20070205)
    */
   public void setAddToLogManager(boolean addToLogManager)
   {
      this.addToLogManager = addToLogManager;
      
      JServer jserver = JServer.getJServer();
      if( (jserver != null) && (jserver.getLogManager() != null) )
      {
         if( this.addToLogManager ) jserver.getLogManager().addAppender(this);
         else jserver.getLogManager().removeAppender(this); 
      }
   }
   
   /**
    * Writes a log message to the file log.
    * 
    * @deprecated as of 2.1.5 (20070214), replaced by {@link #append(String)}
    */
   public void log(final String message) throws Exception
   {
      this.append(message);
   }
   
   /**
    * Writes a log message to the file log.
    * 
    * @deprecated as of 2.1.5 (20070214), replaced by {@link #append(String)}
    */
   public void log(final long timestamp, final String message) throws Exception
   {
      this.append(timestamp, message);
   }
   
   /**
    * Writes a log message to the file log.
    * 
    * @since 2.1.5 (20070214)
    */
   public void append(final String message) throws Exception
   {
      super.append(new LoggingEvent(StandaloneFileAppenderComponent.class.getName(), 
            super.getLogger(), Level.INFO, message, null));
   }
   
   /**
    * Writes a log message to the file log.
    * 
    * @since 2.1.5 (20070214)
    */
   public void append(final long timestamp, final String message) throws Exception
   {
      super.append(new LoggingEvent(StandaloneFileAppenderComponent.class.getName(), 
            super.getLogger(), timestamp, Level.INFO, message, null));
   }
}
