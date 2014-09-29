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

import java.sql.CallableStatement;
import java.sql.Connection;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.pool.DBConnectionPool;
import com.teletalk.jserver.pool.DBPoolObjectFactory;

/**
 * Factory class for creation, validation and finalization of objects to be contained in an {@link com.teletalk.jserver.pool.DBConnectionPool}. 
 * This implementation enables the calling of the stored procedure <code>DBMS_APPLICATION_INFO.SET_MODULE</code> for newly created connections.
 * 
 * @see DBConnectionPool
 * 
 * @since 2.0 Build 757
 * 
 * @author Tobias Löfstrand
 */
public class OracleDBPoolObjectFactory extends DBPoolObjectFactory
{
   /** The value to be used as the module name parameter when calling <code>DBMS_APPLICATION_INFO.SET_MODULE</code>. */
   protected String moduleName;
   
   /** The value to be used as the action name parameter when calling <code>DBMS_APPLICATION_INFO.SET_MODULE</code>. */
   protected String actionName;
   
   /**
    * Creates a new OracleDBPoolObjectFactory with moduleName set to the name of the server and actionName set to the full name of the 
    * DBConnectionPool.
    * 
    * @param dbConnectionPool reference to the DBConnectionPool this factory will be associated with.
    */
   public OracleDBPoolObjectFactory(final DBConnectionPool dbConnectionPool)
   {
      super(dbConnectionPool);
      JServer server = JServer.getJServer();
      if( server != null ) this.moduleName = server.getName(); 
      else this.moduleName = "JServer";
      this.actionName = dbConnectionPool.getFullName();
   }
   
   /**
    * Gets value to be used as the module name parameter when calling <code>DBMS_APPLICATION_INFO.SET_MODULE</code>. 
    */
   public String getModuleName()
   {
      return moduleName;
   }
   
   /**
    * Sets value to be used as the module name parameter when calling <code>DBMS_APPLICATION_INFO.SET_MODULE</code>.
    */
   public void setModuleName(String moduleName)
   {
      this.moduleName = moduleName;
   }
   
   /**
    * Gets value to be used as the action name parameter when calling <code>DBMS_APPLICATION_INFO.SET_MODULE</code>.
    */
   public String getActionName()
   {
      return actionName;
   }
   
   /**
    * Sets value to be used as the action name parameter when calling <code>DBMS_APPLICATION_INFO.SET_MODULE</code>.
    */
   public void setActionName(String actionName)
   {
      this.actionName = actionName;
   }
   
   /**
    * Creates a new OracleDBPoolObjectFactory.
    * 
    * @param dbConnectionPool reference to the DBConnectionPool this factory will be associated with.
    */
   public OracleDBPoolObjectFactory(final DBConnectionPool dbConnectionPool, final String moduleName, final String actionName)
   {
      super(dbConnectionPool);
      this.moduleName = moduleName;
      this.actionName = actionName;
   }
   
	/**
	 * Creates a new Connection object and calls the stored procedure <code>DBMS_APPLICATION_INFO.SET_MODULE</code> with {@link #moduleName} and {@link #actionName} as parameters.
	 * 
	 * @return a newly created Connection object.
	 */
   public Object createObject() throws Exception
   {
      Connection connection = this.dbConnectionPool.createConnection();
      
      if(this.dbConnectionPool.isDebugMode()) this.dbConnectionPool.logDebug(this.dbConnectionPool.getFullName() + ".OracleDBPoolObjectFactory", "(createObject) - Calling DBMS_APPLICATION_INFO.SET_MODULE('" + this.moduleName + "', '" + this.actionName +"').");
      try
      {
	      CallableStatement statement =  connection.prepareCall("{call DBMS_APPLICATION_INFO.SET_MODULE(?, ?)}");
	      statement.setString(1, this.moduleName);
	      statement.setString(2, this.actionName);
	      statement.execute();
      }
      catch(Exception e)
      {
         this.dbConnectionPool.logError(this.dbConnectionPool.getFullName() + ".OracleDBPoolObjectFactory", "Failed to execute DBMS_APPLICATION_INFO.SET_MODULE!", e);
      }
      
      return connection;
   }
}
