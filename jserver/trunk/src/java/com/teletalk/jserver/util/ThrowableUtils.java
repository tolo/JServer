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

/**
 * Utility class with useful methods associated with Throwable objects.
 * 
 * @since 2.0 (20041011)
 */
public class ThrowableUtils
{
   private static final int THROWABLE_CAUSE_SANITY_DEPTH = 100;
   
   /**
    * Gets a descriptive string of the specified Throwable, including any cause Throwables.<br>
    * <br>
    * This will include a maximum of 100 causes.
    * 
    * @param throwable a Throwable.
    */
   public static String getDescription(final Throwable throwable)
   {
      return getDescription(throwable, THROWABLE_CAUSE_SANITY_DEPTH, " - Cause: ");
   }
   
   /**
    * Gets a descriptive string of the specified Throwable, including any cause Throwables.
    * 
    * @param throwable a Throwable.
    * @param maxCauseDepth the maximum number of causes to include.
    */
   public static String getDescription(final Throwable throwable, final int maxCauseDepth)
   {
      return getDescription(throwable, maxCauseDepth, " - Cause: ");
   }
   
   /**
    * Gets a descriptive string of the specified Throwable, including any cause Throwables.
    * 
    * @param throwable a Throwable.
    * @param separatorString a string separating each cause.
    */
   public static String getDescription(final Throwable throwable, final String separatorString)
   {
      return getDescription(throwable, THROWABLE_CAUSE_SANITY_DEPTH, separatorString);
   }
   
   /**
    * Gets a descriptive string of the specified Throwable, including any cause Throwables.
    * 
    * @param throwable a Throwable.
    * @param maxCauseDepth the maximum number of causes to include.
    * @param separatorString a string separating each cause.
    */
   public static String getDescription(final Throwable throwable, final int maxCauseDepth, final String separatorString)
   {
      if( throwable == null ) return "null";
      
      StringBuffer msg = new StringBuffer(throwable.toString());
      Throwable cause = throwable.getCause();
      
      for(int i=0; (i<maxCauseDepth) && (cause != null); i++)
      {
         msg.append(separatorString); 
         msg.append(cause.toString());
         cause = cause.getCause();
      }
      
      return msg.toString();
   }
}
