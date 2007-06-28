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
package com.teletalk.jserver.statistics;

/**
 * A System.out implementation of StatisticsObserver.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0.2
 */
public class SystemOutStatisticsObserver implements StatisticsObserver
{
   private static String toPathString(final String[] namePath)
   {
      StringBuffer pathString = new StringBuffer();
      
      for(int i=0; i<namePath.length; i++)
      {
         pathString.append(namePath[i]);
         if( i < (namePath.length - 1) ) pathString.append("/");
      }
      
      return pathString.toString();
   }
   
   /**
    * Called to observe a {@link StatisticsSource}.
    */
   public void inStatisticsSource(String[] namePath, StatisticsSource statisticsSource)
   {
      System.out.println(toPathString(namePath));
   }

   /**
    * Called to observe a {@link StatisticsEntry}.
    */
   public void inStatisticsEntry(String[] namePath, StatisticsEntry statisticsEntry)
   {
      System.out.println(toPathString(namePath) + ": " + statisticsEntry.getValue());
   }
}
