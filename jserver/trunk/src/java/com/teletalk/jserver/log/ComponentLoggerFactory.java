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

import org.apache.log4j.Logger;
import org.apache.log4j.spi.LoggerFactory;

import com.teletalk.jserver.SubComponent;

/**
 * Factory class for creation of ComponentLogger objects.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0
 */
public class ComponentLoggerFactory implements LoggerFactory
{
   private static final ComponentLoggerFactory defaultComponentLoggerFactory = new ComponentLoggerFactory(null);
   
   /**
    * Gets the default factory.
    */
   public static ComponentLoggerFactory getDefaultFactory() 
   {
      return defaultComponentLoggerFactory;
   }
   
   private final SubComponent owner;
     
   /**
    * Creates a new ComponentLoggerFactory.
    */
   public ComponentLoggerFactory(final SubComponent owner)
   {
      this.owner = owner;
   }
   
   /**
    * Creates a new logger instance.
    */
   public Logger makeNewLoggerInstance(final String name)
   {
      return new ComponentLogger(name, owner); 
   }
}
