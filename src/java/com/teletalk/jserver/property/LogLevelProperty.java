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
package com.teletalk.jserver.property;

import org.apache.log4j.Level;

/**
 * Enumeration property class for handling of a log level value.  
 *
 * @author Tobias Löfstrand
 *  
 * @since 2.0
 */
public class LogLevelProperty extends EnumProperty
{
   private static final boolean traceEnabled;
   
   private static final int ALL_LEVEL_INDEX;
   private static final int TRACE_LEVEL_INDEX;
   private static final int DEBUG_LEVEL_INDEX;
   private static final int INFO_LEVEL_INDEX;
   private static final int WARNING_LEVEL_INDEX;
   private static final int ERROR_LEVEL_INDEX;
   private static final int FATAL_LEVEL_INDEX;
   private static final int OFF_LEVEL_INDEX;
   
   /** Array containing the names for the different statuslevels. */
   public static final String[] logLevels;
      
   
   static
   {
      boolean traceEnabledTmp = true;
      try
      {
         Level.TRACE.toInt();
      }
      catch (Throwable t){ traceEnabledTmp = false; }
      traceEnabled = traceEnabledTmp;
      
      if( traceEnabled )
      {
         logLevels = new String[]{"All", "Trace", "Debug", "Info", "Warning", "Error", "Fatal", "Off"};
         
         ALL_LEVEL_INDEX = 0;
         TRACE_LEVEL_INDEX = 1;
         DEBUG_LEVEL_INDEX = 2;
         INFO_LEVEL_INDEX = 3;
         WARNING_LEVEL_INDEX = 4;
         ERROR_LEVEL_INDEX = 5;
         FATAL_LEVEL_INDEX = 6;
         OFF_LEVEL_INDEX = 7;
      }
      else
      {
         logLevels = new String[]{"All", "Debug", "Info", "Warning", "Error", "Fatal", "Off"};
         
         TRACE_LEVEL_INDEX = -1;
         
         ALL_LEVEL_INDEX = 0;
         DEBUG_LEVEL_INDEX = 1;
         INFO_LEVEL_INDEX = 2;
         WARNING_LEVEL_INDEX = 3;
         ERROR_LEVEL_INDEX = 4;
         FATAL_LEVEL_INDEX = 5;
         OFF_LEVEL_INDEX = 6;
      }
   }
   
   /**
    * Creates a new LogLevelProperty.
    */
   public LogLevelProperty(PropertyOwner owner, String name, Level initialValue, int modificationMode)
   {
      super(owner, name, levelToIndex(initialValue), logLevels, modificationMode);
   }

   /**
    * Creates a new LogLevelProperty.
    */
   public LogLevelProperty(PropertyOwner owner, String name, Level initialValue, int modificationMode, boolean persistent)
   {
      super(owner, name, levelToIndex(initialValue), logLevels, modificationMode, persistent);
   }

   /**
    * Creates a new LogLevelProperty.
    */
   public LogLevelProperty(PropertyOwner owner, String name, Level initialValue)
   {
      super(owner, name, levelToIndex(initialValue), logLevels);
   }
   
   /**
    * Gets the current value of this LogLevelProperty as a Level object.
    */
   public Level getLevel()
   {
      return indexToLevel(super.getIndex());
   }
   
   /**
    * Gets the current value of this LogLevelProperty as a Level integer value.
    */
   public int getLevelInt()
   {
      return indexToLevelInt(super.getIndex());
   }
   
   /**
    * Sets the value of this EnumProperty as an Object, after first checking if the owner allows modification
    * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.<br>
    * <br>
    * This method will attempt to convert the object passed in parameter enumValue to a Level object. If that is not possible, it will 
    * attempt to convert it to a string value, by calling the method toString on the object.
    * 
    * @param value the new value of this EnumProperty.
    * 
    * @return true if the value was successfully set, otherwise false.
    */
   public boolean setValueAsObject(final Object value)
   {
      if( value instanceof Level )
      {
         return super.setValue(levelToIndex((Level)value));
      }
      else return super.setValueAsObject(value);
   }
   
   /**
    * Sets the value of this EnumProperty as a Level object, after first checking if the owner allows modification
    * of it and validates it ok. If the value was succesfully set the owner will be notified of the change.
    * 
    * @param enumValue the new value of this EnumProperty.
    * 
    * @return true if the value was successfully set, otherwise false.
    */
   public boolean setLevel(final Level enumValue)
   {
      return this.setValueAsObject(enumValue);
   }
   
   /* ##### LOGGING ULILITY METHODS AND CLASSES BEGINS #####*/
   
   private static Level indexToLevel(final int levelIndex)
   {
      if( levelIndex == ALL_LEVEL_INDEX ) return Level.ALL;   
      else if( levelIndex == TRACE_LEVEL_INDEX ) return Level.TRACE;
      else if( levelIndex == DEBUG_LEVEL_INDEX ) return Level.DEBUG;
      else if( levelIndex == INFO_LEVEL_INDEX ) return Level.INFO;
      else if( levelIndex == WARNING_LEVEL_INDEX ) return Level.WARN;
      else if( levelIndex == ERROR_LEVEL_INDEX ) return Level.ERROR;
      else if( levelIndex == FATAL_LEVEL_INDEX ) return Level.FATAL;
      else if( levelIndex == OFF_LEVEL_INDEX ) return Level.OFF;
      else return Level.DEBUG;
   }
   
   private static int indexToLevelInt(final int levelIndex)
   {
      if( levelIndex == ALL_LEVEL_INDEX ) return Level.ALL_INT;   
      else if( levelIndex == TRACE_LEVEL_INDEX ) return Level.TRACE_INT;
      else if( levelIndex == DEBUG_LEVEL_INDEX ) return Level.DEBUG_INT;
      else if( levelIndex == INFO_LEVEL_INDEX ) return Level.INFO_INT;
      else if( levelIndex == WARNING_LEVEL_INDEX ) return Level.WARN_INT;
      else if( levelIndex == ERROR_LEVEL_INDEX ) return Level.ERROR_INT;
      else if( levelIndex == FATAL_LEVEL_INDEX ) return Level.FATAL_INT;
      else if( levelIndex == OFF_LEVEL_INDEX ) return Level.OFF_INT;
      else return Level.DEBUG_INT;
   }
   
   private static int levelToIndex(final Level level)
   {
      if( level.toInt() == Level.ALL_INT ) return ALL_LEVEL_INDEX;
      else if( traceEnabled && (level.toInt() == Level.TRACE_INT) ) return TRACE_LEVEL_INDEX;
      else if( level.toInt() == Level.DEBUG_INT ) return DEBUG_LEVEL_INDEX;
      else if( level.toInt() == Level.INFO_INT ) return INFO_LEVEL_INDEX;
      else if( level.toInt() == Level.WARN_INT ) return WARNING_LEVEL_INDEX;
      else if( level.toInt() == Level.ERROR_INT ) return ERROR_LEVEL_INDEX;
      else if( level.toInt() == Level.FATAL_INT ) return FATAL_LEVEL_INDEX;
      else if( level.toInt() == Level.OFF_INT ) return OFF_LEVEL_INDEX;
      else return DEBUG_LEVEL_INDEX;
   }
   
   /* ##### LOGGING ULILITY METHODS AND CLASSES ENDS #####*/
}
