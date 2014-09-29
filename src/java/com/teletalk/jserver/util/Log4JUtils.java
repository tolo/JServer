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
package com.teletalk.jserver.util;

import java.io.File;
import java.util.ArrayList;
import java.util.Enumeration;

import org.apache.log4j.FileAppender;
import org.apache.log4j.Logger;

import com.teletalk.jserver.log.LogData;

/**
 * Log4J utility class.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0.1 (20041227)
 */
public class Log4JUtils
{
   /**
    * Gets all registered log4j file appenders.
    */
   public static ArrayList getLog4JFileAppenders()
   {
      ArrayList appenders = new ArrayList();
      
      try
      {
         Enumeration loggers = org.apache.log4j.LogManager.getCurrentLoggers();
                  
         Logger logger;
         Object appender;
         while(loggers.hasMoreElements())
         {
            logger = (Logger)loggers.nextElement();

            if( logger != null )
            {
               Enumeration appenderEnum = logger.getAllAppenders();
               while(appenderEnum.hasMoreElements())
               {
                  appender = appenderEnum.nextElement();

                  if( (appender != null) && (appender instanceof FileAppender) )
                  {
                     appenders.add( appender );
                  }
               }
            }
         }
         
         logger = Logger.getRootLogger();
         Enumeration appenderEnum = logger.getAllAppenders();
         while(appenderEnum.hasMoreElements())
         {
            appender = appenderEnum.nextElement();

            if( (appender != null) && (appender instanceof FileAppender) )
            {
               if( !appenders.contains(appender) ) appenders.add( appender );
            }
         }
         
      }catch(Exception e){}
      
      return appenders;
   }
   
   /**
    * Finds the file appender with the specified name.
    */
   public static FileAppender findLog4JFileAppender(final String name)
   {
      try
      {
         Enumeration loggers = org.apache.log4j.LogManager.getCurrentLoggers();
                  
         Logger logger;
         Object appender;
         while(loggers.hasMoreElements())
         {
            logger = (Logger)loggers.nextElement();

            if( logger != null )
            {
               Enumeration appenderEnum = logger.getAllAppenders();
               while(appenderEnum.hasMoreElements())
               {
                  appender = appenderEnum.nextElement();

                  if( (appender != null) && (appender instanceof FileAppender) )
                  {
                     FileAppender fileAppender = (FileAppender)appender;
                     if( name.equals(fileAppender.getName()) ) return fileAppender;
                  }
               }
            }
         }
         
         logger = Logger.getRootLogger();
         Enumeration appenderEnum = logger.getAllAppenders();
         while(appenderEnum.hasMoreElements())
         {
            appender = appenderEnum.nextElement();

            if( (appender != null) && (appender instanceof FileAppender) )
            {
               FileAppender fileAppender = (FileAppender)appender;
               if( name.equals(fileAppender.getName()) ) return fileAppender;
            }
         }
         
      }catch(Exception e){}
      
      return null;
   }
   
   /**
    * Gets the base path of the specified file appender.
    */
   public static String getLog4JFileAppenderBasePath(final FileAppender appender)
   {
      File logFile = new File(appender.getFile()).getAbsoluteFile();
      File parentDir = logFile.getParentFile();
      
      return parentDir.getAbsolutePath();
   }
     
   /**
    * Gets a list of {@link LogData} objects representing the log files of the specified file appender.
    */
   public static ArrayList getLog4JFileAppenderLogs(final FileAppender appender)
   {
      ArrayList logData = new ArrayList();
      
      String appenderName = appender.getName();
      File logFile = new File(appender.getFile()).getAbsoluteFile();
      File parentDir = logFile.getParentFile();
      
      String[] files = parentDir.list();
      for (int i=0; i<files.length; i++)
      {
         if( files[i].startsWith(logFile.getName()) )
         {
            logData.add(new LogData(appenderName, files[i], new File(parentDir.getAbsolutePath(), files[i]).length()));
         }
      }
            
      return logData;
   }
}
