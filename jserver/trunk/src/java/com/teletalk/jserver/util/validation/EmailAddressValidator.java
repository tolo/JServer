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

/*
 * TODO: Much more to check
 * TODO: Doesn't handle domain-literals properly
 */
package com.teletalk.jserver.util.validation;

/**
 * Validator class that checks that an E-mail address is valid (as per RFC822).
 * 
 * @since 2.1 (20050425)
 */
public class EmailAddressValidator
{
   private static final String specialsNoDotNoAt = "()<>,;:\\\"[]";

   private static final String specialsNoDot = specialsNoDotNoAt + "@";
   

   /**
    * 
    */
   public static boolean validate(final String addr)
   {
      return validate(addr, false, false);
   }

   /**
    * 
    */
   public static boolean validate(final String addr, final boolean allowEmptyOrBlankString, final boolean allowNull)
   {
      try
      {
         if (addr == null) return allowNull;
         else if (addr.trim().length() == 0) return allowEmptyOrBlankString;

         int i=0;
         int start = 0;
         if (addr.indexOf('"') >= 0) // Validation of addresses containing quoutes is not supported
         {
            return false;  
         }

         // The rest should be "user@domain"
         String user = null;
         String domain = null;

         if ((i = addr.indexOf('@', start)) >= 0)
         {
            if (i == start) return false; // Missing user
            if (i == addr.length() - 1) return false; //Missing domain
            
            user = addr.substring(start, i);
            domain = addr.substring(i + 1);
         }

         if (indexOfAny(addr, " \t\n\r") >= 0) return false; //Illegal whitespace in address
         
         // User part must follow RFC822, no specials except '.'
         if (indexOfAny(user, specialsNoDot) >= 0)
         {
            return false; //Illegal character in local name", addr);
         }

         if (domain != null && domain.indexOf('[') < 0)
         {
            if (indexOfAny(domain, specialsNoDot) >= 0)
            {
               return false; //Illegal character in domain
            }
         }

         // check for . after @
         if (domain.indexOf('.') == -1)
         {
            return false;
         }

         // check that adress doesn't end with .
         if (domain.endsWith("."))
         {
            return false;
         }

         // check for atleast 2 characters after .
         if ((domain.length() - domain.indexOf('.')) < 3)
         {
            return false;
         }

         // check that first char after @ isn't .
         if (domain.startsWith("."))
         {
            return false;
         }

         return true;
      }
      catch (Exception e)
      {
         return false;
      }
   }

   /**
    * Return the first index of any of the characters in "any" in "s",
    * or -1 if none are found.
    *
    * This should be a method on String.
    */
   private static int indexOfAny(String s, String any)
   {
      return indexOfAny(s, any, 0);
   }

   private static int indexOfAny(String s, String any, int start)
   {
      try
      {
         int len = s.length();

         for (int i = start; i < len; i++)
         {
            if (any.indexOf(s.charAt(i)) >= 0)
            {
               return i;
            }
         }

         return -1;
      }
      catch (StringIndexOutOfBoundsException e)
      {
         return -1;
      }
   }
}
