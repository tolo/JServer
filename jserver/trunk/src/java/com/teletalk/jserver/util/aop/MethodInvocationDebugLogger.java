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
package com.teletalk.jserver.util.aop;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.teletalk.jserver.util.StringUtils;

/**
 * Spring AOP method interceptor class for logging before and after method invocations. Note that this object will only 
 * log if the log level of the associated logger is set to debug. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20051014)
 */
public class MethodInvocationDebugLogger implements MethodInterceptor
{
   private String loggerName;
   
   private Log logger = null;
   
   private int maxParameterStringRepresentationLength = 100;
   
   private int maxReturnValueStringRepresentationLength = 1000;
   
   
   /**
    */
   public String getLoggerName()
   {
      return loggerName;
   }

   /**
    */
   public void setLoggerName(final String loggerName)
   {
      this.loggerName = loggerName;
      if( (this.loggerName != null) && (this.loggerName.trim().length() > 0) ) this.logger = LogFactory.getLog(this.loggerName);
   }
   
   /**
    */
   public int getMaxParameterStringRepresentationLength()
   {
      return maxParameterStringRepresentationLength;
   }

   /**
    */
   public void setMaxParameterStringRepresentationLength(int maxParameterStringRepresentationLength)
   {
      this.maxParameterStringRepresentationLength = maxParameterStringRepresentationLength;
   }

   /**
    */
   public int getMaxReturnValueStringRepresentationLength()
   {
      return maxReturnValueStringRepresentationLength;
   }

   /**
    */
   public void setMaxReturnValueStringRepresentationLength(int maxReturnValueStringRepresentationLength)
   {
      this.maxReturnValueStringRepresentationLength = maxReturnValueStringRepresentationLength;
   }
   
   
   /* ###### */

   
   /**
    */
   public Object invoke(final MethodInvocation methodInvocation) throws Throwable
   {
      Method method = methodInvocation.getMethod();
      String paramLogString = null;
      long executionStartTime = System.currentTimeMillis();
      Log logger = this.logger;
      String methodName;
      if( logger == null )
      {
         logger = LogFactory.getLog(method.getDeclaringClass());
         methodName = method.getName();
      }
      else methodName = method.getDeclaringClass().getName() + "." + method.getName();
      
      if( logger.isDebugEnabled() )
      {
         paramLogString = "";
         Object[] arguments = methodInvocation.getArguments(); 
         if( arguments != null )
         {
            for (int i= 0; i<arguments.length; i++)
            {
               paramLogString += StringUtils.limitStringLength(StringUtils.toString(arguments[i]), this.maxParameterStringRepresentationLength);
               if( i < (arguments.length - 1) ) paramLogString += ", ";
            }
         }
      
         logger.debug("Executing " + methodName + "(" + paramLogString + ").");
      }
      
      Object returnValue = methodInvocation.proceed();
     
      if( logger.isDebugEnabled() )
      {
         if( (method.getReturnType() == null) || method.getReturnType().equals(Void.TYPE) )
         {
            logger.debug("Done executing " + methodName + "(" + paramLogString + ") after " + (System.currentTimeMillis()-executionStartTime) +  "ms.");
         }
         else
         {
            logger.debug("Done executing " + methodName + "(" + paramLogString + ") after " + (System.currentTimeMillis()-executionStartTime) +  "ms" + 
               " - return value: " + StringUtils.limitStringLength(StringUtils.toString(returnValue), this.maxReturnValueStringRepresentationLength) + ".");
         }
      }
      
      
      return returnValue;
   }
}
