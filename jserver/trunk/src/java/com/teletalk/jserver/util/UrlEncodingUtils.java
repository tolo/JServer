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

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;

/**
 * Class providing URL encoding 
 * 
 * @author Tobias Löfstrand
 *
 * @since 2.1 (20050613)
 */
public class UrlEncodingUtils
{
   public static final String DEFAULT_CHARACTER_ENCODING = "ISO-8859-1";
   
   /**
    * URL encodes the specified string using the ISO-8859-1 encoding if applicable, otherwise the system 
    * default encoding will be used.
    */
   public static String encode(final String string)
   {
      return encode(string, null);
   }
   
   /**
    * URL encodes the specified string with the specified encoding. If parameter encoding is null, the ISO-8859-1 encoding 
    * will be used if applicable, otherwise the system default encoding will be used.
    * 
    * @since 2.0 (20050531)
    */
   public static String encode(final String string, String encoding)
   {
      try
      {
         if( encoding == null ) encoding = DEFAULT_CHARACTER_ENCODING;
         
         return URLEncoder.encode(string, encoding);
      }
      catch(UnsupportedEncodingException uee)
      {
         return URLEncoder.encode(string);
      }  
   }
   
   /**
    * URL decodes the specified string using the ISO-8859-1 encoding if applicable, otherwise the system 
    * default encoding will be used.
    * 
    * @since 2.0 (20041015)
    */
   public static String decode(final String string)
   {
      return decode(string, null);
   }
   
   /**
    * URL decodes the specified string with the specified encoding. If parameter encoding is null, the ISO-8859-1 encoding 
    * will be used if applicable, otherwise the system default encoding will be used.
    * 
    * @since 2.0 (20050531)
    */
   public static String decode(final String string, String encoding)
   {
      try
      {
         if( encoding == null ) encoding = DEFAULT_CHARACTER_ENCODING;
         
         return URLDecoder.decode(string, encoding);
      }
      catch(UnsupportedEncodingException uee)
      {
         return URLDecoder.decode(string);
      }  
   }
}
