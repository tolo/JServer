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
package com.teletalk.jserver.jdbc;

import java.io.PrintWriter;
import java.sql.Connection;
import java.sql.SQLException;

import org.springframework.jdbc.datasource.SmartDataSource;

import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.pool.DBConnectionPool;
import com.teletalk.jserver.spring.EngageOnCreationBean;

/**
 * Adaptation of the class {@link com.teletalk.jserver.pool.DBConnectionPool} implementing the J2EE datasource interface,  
 * <code>javax.sql.DataSource</code>, as well as the Spring interface <code>org.springframework.jdbc.datasource.SmartDataSource</code>. 
 * The latter makes it possible to get a notification when the connection is about to be closed, and return it to the pool instead.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1
 */
public class PooledSmartDataSource extends DBConnectionPool implements SmartDataSource, EngageOnCreationBean
{
   /**
    */
   public PooledSmartDataSource()
   {
      this((SubComponent)null);
   }
   
   /**
    */
   public PooledSmartDataSource(String name)
   {
      this(null, name);
   }
   
   /**
    */
   public PooledSmartDataSource(SubComponent parent)
   {
      this(parent, "PooledSmartDataSource");
   }
   
   /**
    */
   public PooledSmartDataSource(SubComponent parent, String name)
   {
      super(parent, name);
   }
   
   /**
    */
   public PooledSmartDataSource(SubComponent parent, String name, String driverName, String dbURL)
   {
      super(parent, name, driverName, dbURL);
   }
   
   /**
    */
   public PooledSmartDataSource(SubComponent parent, String name, String driverName, String dbURL, String user, String password, int minSize, int maxSize, long cleanInterval, long expirationTime)
   {
      super(parent, name, driverName, dbURL, user, password, minSize, maxSize, cleanInterval, expirationTime);
   }
   
   /* ### METHODS FROM SMARTDATASOURCE ### */
   
   public boolean shouldClose(final Connection connection)
   {
      super.returnConnection(connection);
      return false;
   }
   
   
   /* ### METHODS FROM javax.sql.DataSource ### */
      
   
   /*public Connection getConnection() throws SQLException
   {
      return super.getConnection();
   }*/
   public Connection getConnection(String username, String password) throws SQLException
   {
      return this.getConnection();
   }
   public int getLoginTimeout() throws SQLException
   {
      return 0;
   }
   public void setLoginTimeout(int seconds) throws SQLException
   {
   }
   public PrintWriter getLogWriter() throws SQLException
   {
      return null;
   }
   public void setLogWriter(PrintWriter out) throws SQLException
   {
   }
   
   
   /* ### METHODS FROM java.sql.Wrapper (JDK1.6) ### */
   
   
   public boolean isWrapperFor(final Class interfaceClass) throws SQLException
   {
      return interfaceClass.isInstance(this); 
   }

   public Object unwrap(final Class interfaceClass) throws SQLException
   {
      if( this.isWrapperFor(interfaceClass)) return this;
      else return null;
   }
}
