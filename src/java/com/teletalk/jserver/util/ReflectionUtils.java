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

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

/**
 * 
 * @author Tobias Löfstrand
 * 
 */
public class ReflectionUtils
{
   /**
    * Finds the method in the specified target class.
    * 
    * @param target the class in which a search for a matching method should be performed.
    * @param methodName
    * @param params the parameters to find a matching method for.
    * 
    * @return a matching Constructor object or null if none was found. Null is also returned if target or params are null.
    * 
    * @exception NoSuchMethodException if there was an error finding a method in the specified target class matching the sprecfied parameters.
    */
   public static final Method findDeclaredMethod(final Class target, final String methodName, final Object[] params) throws NoSuchMethodException
   {
      return findMethodInternal(target, methodName, params, true, true, false);
   }
   
   /**
    * Finds the method in the specified target class.
    * 
    * @param target the class in which a search for a matching method should be performed.
    * @param methodName
    * @param params the parameters to find a matching method for.
    * @param throwException flag indicating if a NoSuchMethodException is to be thrown if a method isn't found.
    * 
    * @return a matching Method object or null if none was found. Null is also returned if target or params are null.
    * 
    * @exception NoSuchMethodException if there was an error finding a method in the specified target class matching the sprecfied parameters.
    */
   public static final Method findDeclaredMethod(final Class target, final String methodName, final Object[] params, final boolean throwException) throws NoSuchMethodException
   {
      return findMethodInternal(target, methodName, params, true, throwException, false);
   }
   
	/**
	 * Finds the method in the specified target class.
	 * 
	 * @param target the class in which a search for a matching method should be performed.
	 * @param methodName
	 * @param params the parameters to find a matching method for.
	 * @param throwException flag indicating if a NoSuchMethodException is to be thrown if a method isn't found.
	 * 
	 * 
	 * @return a matching Method object or null if none was found. Null is also returned if target or params are null.
	 * 
	 * @exception NoSuchMethodException if there was an error finding a method in the specified target class matching the sprecfied parameters.
	 */
	public static final Method findDeclaredMethod(final Class target, final String methodName, final Object[] params, final boolean throwException, final boolean findExactMatch) throws NoSuchMethodException
	{
		return findMethodInternal(target, methodName, params, true, throwException, findExactMatch);
	}
   
   /**
    * Finds the method in the specified target class.
    * 
    * @param target the class in which a search for a matching method should be performed.
    * @param methodName
    * @param params the parameters to find a matching method for.
    * 
    * @return a matching Constructor object or null if none was found. Null is also returned if target or params are null.
    * 
    * @exception NoSuchMethodException if there was an error finding a method in the specified target class matching the sprecfied parameters.
    */
   public static final Method findMethod(final Class target, final String methodName, final Object[] params) throws NoSuchMethodException
   {
      return findMethodInternal(target, methodName, params, false, true, false);
   }
   
   /**
    * Finds the method in the specified target class.
    * 
    * @param target the class in which a search for a matching method should be performed.
    * @param methodName
    * @param params the parameters to find a matching method for.
    * @param throwException flag indicating if a NoSuchMethodException is to be thrown if a method isn't found.
    * 
    * @return a matching Method object or null if none was found. Null is also returned if target or params are null.
    * 
    * @exception NoSuchMethodException if there was an error finding a method in the specified target class matching the sprecfied parameters.
    */
   public static final Method findMethod(final Class target, final String methodName, final Object[] params, final boolean throwException) throws NoSuchMethodException
   {
      return findMethodInternal(target, methodName, params, false, throwException, false);
   }
   
	/**
	 * Finds the method in the specified target class.
	 * 
	 * @param target the class in which a search for a matching method should be performed.
	 * @param methodName
	 * @param params the parameters to find a matching method for.
	 * @param throwException flag indicating if a NoSuchMethodException is to be thrown if a method isn't found.
	 * 
	 * @return a matching Method object or null if none was found. Null is also returned if target or params are null.
	 * 
	 * @exception NoSuchMethodException if there was an error finding a method in the specified target class matching the sprecfied parameters.
	 */
	public static final Method findMethod(final Class target, final String methodName, final Object[] params, final boolean throwException, final boolean nullParamsMatch) throws NoSuchMethodException
	{
		return findMethodInternal(target, methodName, params, false, throwException, nullParamsMatch);
	}
   
   /**
    * Finds the method in the specified target class.
    * 
    * @param target the class in which a search for a matching method should be performed.
    * @param methodName
    * @param params the parameters to find a matching method for.
    * @param throwException flag indicating if a NoSuchMethodException is to be thrown if a method isn't found.
    * 
    * @return a matching Method object or null if none was found. Null is also returned if target or params are null.
    * 
    * @exception NoSuchMethodException if there was an error finding a method in the specified target class matching the sprecfied parameters.
    */
   private static final Method findMethodInternal(final Class target, final String methodName, final Object[] params, final boolean onlyDeclared, final boolean throwException, final boolean nullParamsMatch) throws NoSuchMethodException
   {
      if (target != null )
      {
         Class[] methodParameterTypes;
         Method[] objectClassMethods = onlyDeclared ? target.getDeclaredMethods() : target.getMethods();
         boolean match;

         for (int m = 0; m < objectClassMethods.length; m++)
         {
            methodParameterTypes = objectClassMethods[m].getParameterTypes();
            
            if( objectClassMethods[m].getName().equals(methodName) )
            {
               match = matchParameters(params, methodParameterTypes, nullParamsMatch);
   
               if(match)
               {
                  return objectClassMethods[m];
               }
            }
         }

         if( throwException )
         {
            String paramString = "[";
            String param;
            if( params != null )
            {
               for (int i = 0; i < params.length; i++)
               {
      				param = (params[i] != null) ? params[i].toString() : "null";
                  if (i < (params.length - 1)) paramString += param + ", ";
                  else paramString += param;
               }
            }
            paramString += "]";

          throw new NoSuchMethodException("Unable to find a matching method for class " + target.getName() + ". Specified parameters were: " + paramString + ".");
         }
         else return null;
      }
      else
      {
         if ( throwException && (target == null) ) throw new NoSuchMethodException("Unable to find a matching method! Target class = null!");
         else return null;
      }
   }

   /**
    * Finds the constructor in the specified target class.
    * 
    * @param target the class in which a search for a matching constructor should be performed.
    * @param params the constructorparameters to find a matching contructor for.
    * 
    * @return a matching Constructor object or null if none was found. Null is also returned if target or constructorParams are null.
    * 
    * @exception NoSuchMethodException if there was an error finding a constructor in the specified target class matching the sprecfied parameters.
    */
   public static final Constructor findConstructor(final Class target, final Object[] params) throws NoSuchMethodException
   {
      return findConstructor(target, params, true);
   }
   
	/**
	 * Finds the constructor in the specified target class.
	 * 
	 * @param target the class in which a search for a matching constructor should be performed.
	 * @param params the parameters to find a matching contructor for.
	 * @param throwException flag indicating if a NoSuchMethodException is to be thrown if a constructor isn't found.
	 * 
	 * @return a matching Constructor object or null if none was found. Null is also returned if target or constructorParams are null.
	 * 
	 * @exception NoSuchMethodException if there was an error finding a constructor in the specified target class matching the sprecfied parameters.
	 */
	public static final Constructor findConstructor(final Class target, final Object[] params, final boolean throwException) throws NoSuchMethodException
	{
		return findConstructor(target, params, throwException, false);
	}
  
   /**
    * Finds the constructor in the specified target class.
    * 
    * @param target the class in which a search for a matching constructor should be performed.
    * @param params the parameters to find a matching contructor for.
    * @param throwException flag indicating if a NoSuchMethodException is to be thrown if a constructor isn't found.
    * 
    * @return a matching Constructor object or null if none was found. Null is also returned if target or constructorParams are null.
    * 
    * @exception NoSuchMethodException if there was an error finding a constructor in the specified target class matching the sprecfied parameters.
    */
   public static final Constructor findConstructor(final Class target, final Object[] params, final boolean throwException, final boolean nullParamsMatch) throws NoSuchMethodException
   {
      if (target != null )
      {
         Class[] constructorParameterTypes;
         Constructor[] objectClassConstructors = target.getDeclaredConstructors();
         boolean match;

         for (int c = 0; c < objectClassConstructors.length; c++)
         {
            constructorParameterTypes = objectClassConstructors[c].getParameterTypes();
            
            match = matchParameters(params, constructorParameterTypes, nullParamsMatch);
            if(match)
            {
               return objectClassConstructors[c];
            }
         }

         if( throwException ) 
         {
   			String paramString = "[";
   			String param;
            if( params != null )
            {
      			for (int i = 0; i < params.length; i++)
      			{
      				param = (params[i] != null) ? params[i].toString() : "null";
      				if (i < (params.length - 1)) paramString += param + ", ";
      				else paramString += param;
      			}
            }
   			paramString += "]";
   
            throw new NoSuchMethodException("Unable to find a matching constructor for class " + target.getName() + ". Specified parameters was: " + paramString + ".");
         }
         else return null;
      }
      else
      {
         if ( throwException && (target == null) ) throw new NoSuchMethodException("Unable to find a matching constructor! Target class = null!");
         else return null;
      }
   }
   
   /**
    */
   private static final boolean matchParameters(final Object[] sourceParams, final Class[] targetParamClasses, final boolean nullParamsMatch)
   {
      boolean match = true;
      
      if( (sourceParams == null) && (targetParamClasses == null) ) return true;
      else if( (sourceParams == null) && (targetParamClasses != null) && (targetParamClasses.length == 0) ) return true;
      else if( (targetParamClasses == null) && (sourceParams != null) && (sourceParams.length == 0) ) return true;
           
      if( (sourceParams != null) && (targetParamClasses != null) )
      {
         // If not same number of params...
         if (targetParamClasses.length != sourceParams.length)
         {
            return false;
         }
   
         for (int p = 0; p < targetParamClasses.length; p++)
         {
            if( targetParamClasses[p].isPrimitive() )
            {
               Class type = null;
   
   				if( sourceParams[p] != null )
   				{
   	            try
   	            {
   	               type = (Class)sourceParams[p].getClass().getDeclaredField("TYPE").get(sourceParams[p]);
   	            }
   	            catch (Exception nsfe)
   	            {
   	               type = null;
   	               match = false;
   	               break;
   	            }
   	
   	            if( !((type != null) && type.isAssignableFrom(targetParamClasses[p])) )
   	            {
   	               match = false;
   	               break;
   	            }
   				}
   				else
   				{
   					match = false;
   					break;
   				}
            }
            else if( (sourceParams[p] == null) && !nullParamsMatch )
            {
   				match = false;
   				break;
            }
            else if( (sourceParams[p] != null) && !targetParamClasses[p].isAssignableFrom(sourceParams[p].getClass()) )
            {
               match = false;
               break;
            }
         }
      }
      else match = false;
		
      return match;
   }
}
