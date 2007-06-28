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
package com.teletalk.jserver.jboss;

import java.io.File;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.property.XmlConfigurationFile;

/**
 * Class for JBoss MBean integration of a JServer.
 * 
 * @author Tobias Löfstrand
 *  
 * @since 2.1 (20061215)
 */
public class JServerJBossService implements JServerJBossServiceMBean
{
   private JServer jserver;
   
   private String configFilePath = null;
   
   
   /**
    * Gets the config file (jserver.xml) path.
    */
   public String getConfigFilePath()
   {
      return configFilePath;
   }
   
   /**
    * Sets the config file (jserver.xml) path.
    */
   public void setConfigFilePath(String configFilePath)
   {
      this.configFilePath = configFilePath;
   }
   
   
   /**
    * Starts the JServer.
    */
   public void start() throws Exception
   {
      if( (configFilePath == null) || (configFilePath.trim().length() == 0) )
      {
         try
         {
            configFilePath = new File(new File(super.getClass().getResource("/").toURI()), "jserver.xml").getAbsolutePath();
         }
         catch (Exception e){}
      }
      
      jserver = new JServer("JServer", new XmlConfigurationFile(configFilePath));
      jserver.startJServer();
   }
   
   /**
    * Stops the JServer.
    */
   public void stop()
   {
      jserver.stopJServer();
      try
      {
         jserver.waitForDown(10000);
      }
      catch (Exception e) {}
      jserver = null;
   }
}
