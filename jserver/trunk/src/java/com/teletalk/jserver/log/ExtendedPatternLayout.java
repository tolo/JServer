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

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.PatternLayout;
import org.apache.log4j.helpers.FormattingInfo;
import org.apache.log4j.helpers.PatternConverter;
import org.apache.log4j.helpers.PatternParser;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

/**
 * PatternLayout subclass that enables inclusion of stack trace in formatted logging events by using the formatting 
 * string %S.
 *
 * @author Tobias Löfstrand
 *
 * @since 2.0 
 */
public class ExtendedPatternLayout extends PatternLayout
{
   /**
    * Creates a new ExtendedPatternLayout.
    */
   public ExtendedPatternLayout()
   {
      super();
   }

   /**
    * Creates a new ExtendedPatternLayout.
    */
   public ExtendedPatternLayout(String pattern)
   {
      super(pattern);
   }

   /**
    * Returns PatternParser used to parse the conversion string. Subclasses 
    * may override this to return a subclass of PatternParser which recognize 
    * custom conversion characters.
    */
   protected PatternParser createPatternParser(final String pattern)
   {
      return new ExtendedPatternParser(pattern);
   }
   
   /**
    * Produces a formatted string as specified by the conversion pattern.
    */
   public String format(LoggingEvent event)
   {
      return super.format(event);
   }
   
   /**
    * The PatternLayout handles the throwable contained within LoggingEvents. Thus, it returns <code>false</code>.
    */
   public boolean ignoresThrowable()
   {
      return false;
   }
   
   /**
    * Internal PatternParser implementation.
    */
   private static class ExtendedPatternParser extends PatternParser
   {
      /**
       * @param pattern
       */
      public ExtendedPatternParser(String pattern)
      {
         super(pattern);
      }
      
      /**
       * 
       */
      protected void finalizeConverter(char c)
      {
         if( c == 'S' ) 
         {
            ThrowablePatternConverter throwablePatternConverter = new ThrowablePatternConverter(super.formattingInfo);
            currentLiteral.setLength(0);
            addConverter(throwablePatternConverter);  
         }
         else
         {
            super.finalizeConverter(c);
         }
      }
   }
   
   /**
    */
   private static class ThrowablePatternConverter extends PatternConverter 
   {
      public ThrowablePatternConverter(FormattingInfo formattingInfo) 
      {
        super(formattingInfo);
      }

      public String convert(LoggingEvent event) 
      {
         ThrowableInformation throwableInformation = event.getThrowableInformation();
         if(throwableInformation != null)
         {
            Throwable error = throwableInformation.getThrowable();
            if( error != null )
            {
               StringWriter strWriter = new StringWriter();
               error.printStackTrace(new PrintWriter(strWriter));
                              
               return strWriter.toString().replaceAll("\\s+", " ");
            }
         }
         
         return "";
      }
   }
}
