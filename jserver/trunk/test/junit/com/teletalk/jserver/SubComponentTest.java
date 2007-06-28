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
package com.teletalk.jserver;

import java.io.File;
import java.net.URI;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.property.XmlConfigurationFile;

/**
 * 
 */
public class SubComponentTest extends TestCase
{
   private static final Log logger = LogFactory.getLog(SubComponentTest.class);
   
   
   private static class TestSubComponent extends SubComponent
   {
      public TestSubComponent()
      {
         super();
      }
      public TestSubComponent(String name)
      {
         super(name);
      }
      public TestSubComponent(SubComponent parent, String name)
      {
         super(parent, name);
      }
   }
   
   public void testAddRemove()
   {
      logger.info("BEGIN testAddRemove.");
      
      String confFile = null;
      try{
         confFile = new File(
               new URI(this.getClass().getResource("SubComponentTest.xml").toExternalForm()))
               .getAbsolutePath();
      }catch(Exception e){e.printStackTrace();}
      
      if( confFile == null )
      {
         super.fail("Configuration file SubComponentTest.xml not found!");
      }
      
      JServer jserver = new JServer(TestUtils.TEST_SERVER_NAME, new XmlConfigurationFile(confFile), false, false);
      
      SubComponent a = new TestSubComponent();
      a.setCascadeEngageAndShutDown(true);
      SubComponent b = new TestSubComponent("B");
      b.setCascadeEngageAndShutDown(true);
      SubComponent c = new TestSubComponent("Wrong");
      c.setCascadeEngageAndShutDown(true);
            
      jserver.addSubComponent(a);
      
      a.rename("A");
      a.engage();
            
      a.addSubComponent(b);
      b.engage();
            
      b.addSubComponent(c, "C");
      c.engage();
      
      if( !(TestUtils.TEST_SERVER_NAME + ".A.B.C").equals(c.getFullName()) )
      {
         super.fail("Invalid full name for component C - " + c.getFullName() + "!");
      }
      if( b.getLogLevel() != Level.WARN_INT )
      {
         super.fail("Invalid log level component B - " + b.getLogLevel() + "!");
      }
      if( c.getLogLevel() != Level.WARN_INT )
      {
         super.fail("Invalid log level component C - " + c.getLogLevel() + "!");
      }
      
      b.rename("X", true);
      
      if( !(TestUtils.TEST_SERVER_NAME + ".A.X.C").equals(c.getFullName()) )
      {
         super.fail("Invalid full name for component C - " + c.getFullName() + "!");
      }
      if( b.getLogLevel() != Level.DEBUG_INT )
      {
         super.fail("Invalid log level component X - " + b.getLogLevel() + "!");
      }
      if( c.getLogLevel() != Level.DEBUG_INT )
      {
         super.fail("Invalid log level component C - " + c.getLogLevel() + "!");
      }
      
      jserver.destroyJServer(30000);
      jserver = null;
      
      logger.info("END testAddRemove.");
   }
}
