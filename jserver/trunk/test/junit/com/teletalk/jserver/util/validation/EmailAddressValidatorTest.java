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

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.teletalk.jserver.pool.ObjectPoolTest;
import com.teletalk.jserver.util.validation.EmailAddressValidator;

/**
 * 
 * @since 2.1 (20050425)
 */
public class EmailAddressValidatorTest extends TestCase
{
   private static final Log logger = LogFactory.getLog(ObjectPoolTest.class);
   
   /**
    */
   public void testValidate()
   {
      logger.info("BEGIN testValidate.");
      
      if( !EmailAddressValidator.validate("tobe@kahuna.se") )
      {
         super.fail("\"tobe@kahuna.se\" didn't pass validation!");
      }
      if( !EmailAddressValidator.validate("tobe.kahuna@mupp.kahuna.se") )
      {
         super.fail("\"tobe.kahuna@mupp.kahuna.se\" didn't pass validation!");
      }
      
      if( EmailAddressValidator.validate("") )
      {
         super.fail("\"\" passed validation!");
      }
      if( EmailAddressValidator.validate(" ") )
      {
         super.fail("\" \" passed validation!");
      }
      if( EmailAddressValidator.validate("tobe.kahuna.se") )
      {
         super.fail("\"tobe.kahuna.se\" passed validation!");
      }
      if( EmailAddressValidator.validate("tobe@se") )
      {
         super.fail("\"tobe@se\" passed validation!");
      }
      if( EmailAddressValidator.validate("@kahuna.se") )
      {
         super.fail("\"@kahuna.se\" passed validation!");
      }
      
      logger.info("END testValidate.");
   }
}
