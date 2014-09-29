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

/**
 * 
 * @author Tobias Löfstrand
 */
public class TestUtils
{
   public static String TEST_SERVER_NAME = "JUnitTestServer";
   
   
   /**
    */
   public static void deleteTestFiles()
   {
      // Delete config and log files
      new File("jserver.xml").delete();
      
      File[] files = new File(".").listFiles();
      if( files != null )
      {
         for(int i=0; i<files.length; i++)
         {
            if( files[i].getName().toLowerCase().endsWith(".log") )
            {
               files[i].delete();
            }
         }
      }
   }
}
