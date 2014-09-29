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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.commons.logging.impl.Log4JLogger;
import org.apache.log4j.Logger;

import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.property.MultiStringProperty;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.property.StringProperty;

/**
 * Subcomponent class for controlling the log level of one or more loggers that aren't defined through a subcomponent. The log level of 
 * this component is used to set the log level of the target loggers. 
 * 
 * @since 2.1 (20050902)
 * 
 * @author Tobias Löfstrand
 */
public class ManagedLoggerComponent extends SubComponent
{
   protected MultiStringProperty managedLoggerNames;
   
   
   /**
    * Creates a new LoggerControlComponent.
    */
   public ManagedLoggerComponent()
   {
      this(null, null);
   }
   
   /**
    * Creates a new LoggerControlComponent.
    */
   public ManagedLoggerComponent(String name)
   {
      this(null, name);
   }
   
   /**
    * Creates a new LoggerControlComponent.
    */
   public ManagedLoggerComponent(SubComponent parent, String name)
   {
      this(parent, name, null);
   }
   
   /**
    * Creates a new LoggerControlComponent.
    */
   public ManagedLoggerComponent(SubComponent parent, String name, String logger)
   {
      super(parent, name);
      
      this.managedLoggerNames = new MultiStringProperty(this, "managedLoggerNames", logger, StringProperty.MODIFIABLE_NO_RESTART);
      this.managedLoggerNames.setDescription("The names of the loggers that this component is to manage the log level for. The names specified here may be the complete name of a logger or " +
            "a part of the logger chain - for example com.test.logger or com.test.");
      super.addProperty(this.managedLoggerNames);
   }
      
   /**
    * Updates the loggers matching the names of the managedLoggerNames property with the log level of this component.
    */
   protected void updateLoggers()
   {
      if( this.managedLoggerNames.size() > 0 )
      {
         String[] loggerNameStrings = this.managedLoggerNames.getStringValues();
         
         for (int i = 0; i < loggerNameStrings.length; i++)
         {
            if( (loggerNameStrings[i] != null) && (loggerNameStrings[i].trim().length() > 0) )
            {
               Log log = LogFactory.getLog(loggerNameStrings[i]);
               if( log instanceof Log4JLogger )
               {
                  Logger logger = ((Log4JLogger)log).getLogger();
                  logger.setLevel(super.logLevel.getLevel());
               }
            }
         }
      }
   }

   /**
    * Called when the value of a property has been modified.
    */
   public void propertyModified(final Property property)
   {
      if( (property == this.managedLoggerNames) || (property == super.logLevel) )
      {
         this.updateLoggers();
      }
      super.propertyModified(property);
   }

   /**
    * Initializes this ManagedLoggerComponent.
    */
   protected void doInitialize()
   {
      super.doInitialize();
      
      this.updateLoggers();
   }
}
