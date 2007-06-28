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

import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * Utility class for generating string representations of objects of different types of common classes.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0 (20041222)
 */
public class StringUtils
{
   /** @since 2.1 (20051014) */
   public static final String DEFAULT_TRUNCATION_INDICATOR_STRING = "...";
   
   
   /**
	 * Generates a string representation of the specified objects this method will always retuns a String object (never null).
	 */
	public static String toString(final Object object)
	{
		if( object instanceof String[] ) return StringUtils.toString((String[])object);
		else if( object instanceof Object[] ) return StringUtils.toString((Object[])object);
		else if( object instanceof Map ) return StringUtils.toString((Map)object);
		else if( object instanceof Collection ) return StringUtils.toString((Collection)object);
		else if( object instanceof Iterator ) return StringUtils.toString((Iterator)object);
      else if( (object != null) && object.getClass().isArray() && object.getClass().getComponentType().isPrimitive() ) // Primitive array
      {
         int length = Array.getLength(object);
         StringBuffer result = new StringBuffer("[");
         
         for(int i=0; i<length; i++) 
         {
            result.append(Array.get(object, i));
            if( i < (length - 1) ) result.append(", ");
         }
         
         result.append("]");
         
         return result.toString();
      }
		else if( object != null )
      {
         String toStringString = object.toString();
         if( toStringString != null ) return toStringString;
      }
		return "";
	}
	
	/**
	 * Generates a string representation of the specified string array.
	 */
	public static String toString(final String[] strings)
	{
		StringBuffer result = new StringBuffer("[");
		
		for(int i=0; (strings != null) && (i<strings.length); i++) 
		{
			result.append(strings[i]);
			if( i < (strings.length - 1) ) result.append(", ");
		}
		
		result.append("]");
		
		return result.toString();
	}
	
	/**
	 * Generates a string representation of the specified object array.
	 */
	public static String toString(final Object[] objects)
	{
		StringBuffer result = new StringBuffer("[");
		
		for(int i=0; (objects != null) && (i<objects.length); i++) 
		{
			result.append(StringUtils.toString(objects[i]));
			if( i < (objects.length - 1) ) result.append(", ");
		}
		
		result.append("]");
		
		return result.toString();
	}
		
	/**
	 * Generates a string representation of the specified map.
	 */
	public static String toString(final Map map)
	{
		StringBuffer result = new StringBuffer("{");
		
		if( map != null )
		{
			Iterator it = map.keySet().iterator();
			
			Object key;
			
			while(it.hasNext())
			{
				key = it.next();
				result.append(StringUtils.toString(key));
				result.append("=");
				result.append(StringUtils.toString(map.get(key)));
				if( it.hasNext() ) result.append(", ");
			}
		}
		
		result.append("}");
			
		return result.toString();
	}
	
	/**
	 * Generates a string representation of the specified collection.
	 */
	public static String toString(final Collection collection)
	{
		return StringUtils.toString(collection.iterator());
	}
	
	/**
	 * Generates a string representation of the specified iterator.
	 */
	public static String toString(final Iterator it)
	{
		StringBuffer result = new StringBuffer("[");
		
		if( it != null )
		{
			while(it.hasNext())
			{
				result.append(StringUtils.toString(it.next()));
				if( it.hasNext() ) result.append(", ");
			}
		}
		
		result.append("]");
			
		return result.toString();
	}
   
   /**
    * Limits the length of the specified string.
    * 
    * @param source the soruce string.
    * @param maxLength an integer indicating the maximum length of the returned string.
    * 
    * @return the resulting string.
    * 
    * @since 2.1 (20051014)
    */
   public static String limitStringLength(final String source, final int maxLength)
   {
     return limitStringLength(source, maxLength, true);
   }
   
   /**
    * Limits the length of the specified string.
    * 
    * @param source the soruce string.
    * @param maxLength an integer indicating the maximum length of the returned string.
    * @param truncateAtEnd flag indicating if truncation should be performed at the end (true) or in the beginning (false).
    * 
    * @since 2.1 (20051014)
    */
   public static String limitStringLength(final String source, final int maxLength, final boolean truncateAtEnd)
   {
      return limitStringLength(source, maxLength, truncateAtEnd, DEFAULT_TRUNCATION_INDICATOR_STRING);
   }
   
   /**
    * Limits the length of the specified string.
    * 
    * @param source the soruce string.
    * @param maxLength an integer indicating the maximum length of the returned string.
    * @param truncateAtEnd flag indicating if truncation should be performed at the end (true) or in the beginning (false).
    * @param truncationIndicatorString the string indicating truncation.
    * 
    * @return the resulting string.
    * 
    * @since 2.1 (20051014)
    */
   public static String limitStringLength(final String source, int maxLength, final boolean truncateAtEnd, String truncationIndicatorString)
   {
      if( (source != null) && (maxLength > 0) )
      {
         if( maxLength == 1 )
         {
            return source.substring(0, Math.min( source.length(), maxLength) );
         }
         else if( source.length() > maxLength )
         {
            if( truncationIndicatorString.length() >= source.length() ) truncationIndicatorString = "";
            else maxLength = maxLength - truncationIndicatorString.length();
            
            if( truncateAtEnd ) return source.substring(0, maxLength) + truncationIndicatorString;
            else return  truncationIndicatorString + source.substring(source.length() - maxLength, source.length());
         }
         else return source;
      }
      else return "";
   }
}
