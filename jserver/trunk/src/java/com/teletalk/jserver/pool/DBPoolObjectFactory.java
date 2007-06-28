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
package com.teletalk.jserver.pool;

import java.sql.Connection;

import org.apache.log4j.Level;

/**
 * Factory class for creation, validation and finalization of objects to be contained in an {@link DBConnectionPool}. 
 * This is used as default object factory for {@link DBConnectionPool}.
 * 
 * @see DBConnectionPool
 * 
 * @since 2.0 Build 757
 * 
 * @author Tobias Löfstrand
 */
public class DBPoolObjectFactory implements PoolObjectFactory
{
   protected final DBConnectionPool dbConnectionPool;
   
   /**
    * Creates a new DBPoolObjectFactory. 
    */
   public DBPoolObjectFactory(final DBConnectionPool dbConnectionPool)
   {
      if( dbConnectionPool == null ) throw new RuntimeException("DBConnectionPool may not be null!");
      this.dbConnectionPool = dbConnectionPool;
   }
   
	/**
	 * Creates a new Connection object.
	 * 
	 * @return a newly created Connection object.
	 */
   public Object createObject() throws Exception
   {
      return this.dbConnectionPool.createConnection();
   }
   
	/**
	 * Validates a Connection object.
	 * 
	 * @param obj the Connection object to validate.
	 * 
	 * @return true if the Connection object is open, otherwise false.
	 */
   public boolean validateObject(final Object obj, boolean cleanUpValidation)
   {
		try
		{
			Connection connection = (Connection)obj;
			
			boolean connectionOk = !connection.isClosed();
			if( connectionOk && (dbConnectionPool.getConnectionValidationQuery() != null) )
			{
				connection.createStatement().execute(dbConnectionPool.getConnectionValidationQuery());
			}
			
			return connectionOk;
		}
		catch(Exception e)
		{
			if( this.dbConnectionPool.isDebugMode() ) dbConnectionPool.log(Level.DEBUG, dbConnectionPool.getFullName() + ".DBPoolObjectFactory", "Error in validateObject()!", e);
			return false;
		}
   }
   
	/**
	 * Finalizes a Connection object by closing it.
	 * 
	 * @param obj an Connection object to be finalized.
	 */
   public void finalizeObject(final Object obj)
   {
		final boolean debugModeFlag = this.dbConnectionPool.isDebugMode();
		
		Runnable connectionCloser = new Runnable()
		{
			public void run()
			{
				try
				{
					((Connection)obj).close();
				}
				catch(Exception e)
				{
					if( debugModeFlag ) dbConnectionPool.log(Level.DEBUG, dbConnectionPool.getFullName() + ".DBPoolObjectFactory", "Error in finalizeObject()!", e);
				}
			}
		};
		
		// Execute in separate thread
		this.dbConnectionPool.fireGlobalEvent(connectionCloser);
   }
}
