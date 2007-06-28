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
 * Utility class for equality comparison.
 * 
 * @author Tobias Löfstrand
 *  
 * @since 2.1
 */
public class EqualsUtils
{
   /**
    * Compares two objects for equality.
    */
   public static boolean equals(final Object o1, final Object o2)
   {
      if( (o1 == null) && (o2 == null) ) return true;
      else if( (o1 != null) && (o2 != null) ) return o1.equals(o2);
      
      return false;
   }
}
