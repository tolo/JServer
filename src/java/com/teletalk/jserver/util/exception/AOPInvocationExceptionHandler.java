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
package com.teletalk.jserver.util.exception;

import java.lang.reflect.Method;

import org.aopalliance.intercept.MethodInterceptor;
import org.aopalliance.intercept.MethodInvocation;

/**
 * Exception translation class implemented as an AOP method interceptor, enabling translation of exceptions 
 * occurring in adviced method calls. To use this class, either extend it and implement the method 
 * {@link #translate(Object, Method, Object[], Throwable)}, or create an instance of this class an set an  
 * {@link InvocationExceptionTranslator} using the method {@link #setInvocationExceptionTranslator(InvocationExceptionTranslator)}.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1
 */
public class AOPInvocationExceptionHandler implements MethodInterceptor, InvocationExceptionTranslator
{
   private InvocationExceptionTranslator invocationExceptionTranslator;
   
   /**
    * Creates a new AOPInvocationExceptionHandler.
    */
   public AOPInvocationExceptionHandler()
   {
      this.setInvocationExceptionTranslator(this);
   }
   
   /**
    * Translates an exception. This implementation simply returns parameter exception.
    */
   public Throwable translate(Object target, Method method, Object[] args, Throwable exception)
   {
      return exception;
   }   
   
   /**
    * Gets the associated {@link InvocationExceptionTranslator}. The default InvocationExceptionTranslator is <code>this</code>.
    */
   public InvocationExceptionTranslator getInvocationExceptionTranslator()
   {
      return invocationExceptionTranslator;
   }
   
   /**
    * Sets the associated {@link InvocationExceptionTranslator}. The default InvocationExceptionTranslator is <code>this</code>.
    */
   public void setInvocationExceptionTranslator(InvocationExceptionTranslator invocationExceptionTranslator)
   {
      this.invocationExceptionTranslator = invocationExceptionTranslator;
   }
   
   /**
    * AOP method interception method. This method catches any exception that may occur during the method invocation and 
    * calls the method {@link InvocationExceptionTranslator#translate(Object, Method, Object[], Throwable)} to translate the exception.
    */
   public Object invoke(final MethodInvocation methodInvocation) throws Throwable
   {
      try
      {
         return methodInvocation.proceed();
      }
      catch(Throwable t)
      {
         throw this.invocationExceptionTranslator.translate(methodInvocation.getThis(), methodInvocation.getMethod(), methodInvocation.getArguments(), t);
      }
   }
}
