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
package com.teletalk.jadmin.proxy.messages;

import com.teletalk.jserver.log.LogData;

/**
 * 
 */
public class AppenderComponentGetLogsResponse
{
   private final AppenderComponentGetLogsRequest appenderComponentGetLogsRequest;
   
   private final LogData[] logs;
   
   public AppenderComponentGetLogsResponse(AppenderComponentGetLogsRequest appenderComponentGetLogsRequest, LogData[] logs)
   {
      this.appenderComponentGetLogsRequest = appenderComponentGetLogsRequest;
      this.logs = logs;
   }
   
   /**
    * @return Returns the appenderComponentGetLogsRequest.
    */
   public AppenderComponentGetLogsRequest getAppenderComponentGetLogsRequest()
   {
      return this.appenderComponentGetLogsRequest;
   }
   
   /**
    * @return Returns the logs.
    */
   public LogData[] getLogs()
   {
      return this.logs;
   }
}
