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

import java.io.Serializable;

/**
 * Class representing a log of an {@link AppenderComponent}.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0 Build 757
 */
public class LogData implements Serializable
{
	/** The serial version id of this class. */
   static final long serialVersionUID = 7511077356190660539L;
	
   
   private String appenderName;
   
   private String logName;
   
   private long logSize;
   
   /**
    * Creates a new LogData.
    */
   public LogData(String appenderName, String logName, long logSize)
   {
      this.appenderName = appenderName;
      this.logName = logName;
      this.logSize = logSize;
   }
   
   /**
    * @return Returns the appenderName.
    */
   public String getAppenderName()
   {
      return appenderName;
   }
   
   /**
    * @param appenderName The appenderName to set.
    */
   public void setAppenderName(String appenderName)
   {
      this.appenderName = appenderName;
   }
   
   /**
    * Gets the name of the log.
    */
   public String getLogName()
   {
      return logName;
   }
   
   /**
    * Sets the name of the log.
    */
   public void setLogName(String logName)
   {
      this.logName = logName;
   }
   
   /**
    * Gets the size of the log.
    */
   public long getLogSize()
   {
      return logSize;
   }
   
   /**
    * Sets the size of the log.
    */
   public void setLogSize(long logSize)
   {
      this.logSize = logSize;
   }
   
   /**
    * Gets a string representation of this object.
    */
   public String toString()
   {
      return "LogData[appender name: " + this.appenderName + ", log name: " + this.logName + ", size: " + this.logSize + "]";
   }
}
