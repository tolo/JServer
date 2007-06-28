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
package com.teletalk.jserver.util.validation;

import java.util.regex.Pattern;

/**
 * 
 * @since 2.1 (20050425)
 */
public class PhoneNumberValidator
{
   private static final Pattern pattern;
   
   static
   {
      String whiteSpace = "[ |" + '\u00A0' + "]";
      pattern = Pattern.compile("^(\\+[" + whiteSpace + "]{0,1}|\\d+)(((\\d[" + whiteSpace + 
            "]{0,1})*-([" + whiteSpace + "]{0,1}\\d)*\\d+)|((\\d[" + whiteSpace + "]{0,1}\\d|\\d)*))$");
      // Basically: ^(\+[ ]{0,1}|\d+)(((\d[ ]{0,1})*-([ ]{0,1}\d)*)|((\d[ ]{0,1}\d|\d)*))$
   }
   
   
   /**
    * 
    * @param number
    */
   public static boolean validate(final String number)
   {
      return validate(number, false, false);
   }
   
   /**
    * 
    * @param number
    * @param allowEmptyOrBlankString
    * @param allowNull
    */
   public static boolean validate(final String number, final boolean allowEmptyOrBlankString, final boolean allowNull)
   {
      if (number == null) return allowNull;
      else if (number.trim().length() == 0) return allowEmptyOrBlankString;
       
      return pattern.matcher(number).matches();
   }
}
