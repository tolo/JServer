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
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.SQLException;

import javax.sql.DataSource;

import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.pool.DBConnectionPool;
import com.teletalk.jserver.pool.DBPoolObjectFactory;

/**
 * Adaptation of the class {@link com.teletalk.jserver.pool.DBConnectionPool} implementing the J2EE datasource interface,  
 * <code>javax.sql.DataSource</code>. This class creates wrapper objects of the class {@link PooledConnection} for the 
 * connection objects, to enable the returning of the connection to the pool when the close method is called.<br>
 * <br>
 * Note: If using Spring, consider using {@link PooledSmartDataSource} instead of this class.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1
 */
public class PooledDataSource extends DBConnectionPool implements DataSource
{
   private static class ConnectionProxyInvocationHandler implements InvocationHandler
   {
      private final DBConnectionPool dbConnectionPool;
      private final Connection connection;
      
      public ConnectionProxyInvocationHandler(final DBConnectionPool dbConnectionPool, final Connection connection)
      {
         this.dbConnectionPool = dbConnectionPool;
         this.connection = connection;
      }
      
	   public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable
	   {
	      if( "close".equals(method.getName()) )
	      {
	         dbConnectionPool.returnConnection(connection);
	         return null;
	      }
	      else if( "getTargetConnection".equals(method.getName()) )
	      {
	         return this.connection;
	      }
	      else
	      {
				try 
				{
				   return method.invoke(this.connection, args);
				}
				catch (InvocationTargetException ite) 
				{
					throw ite.getTargetException();
				}
	      }
	   }
   }
   
   private static class PooledConnectionFactory extends DBPoolObjectFactory
   {
      public PooledConnectionFactory(DBConnectionPool dbConnectionPool)
      {
         super(dbConnectionPool);
      }
      public Object createObject() throws Exception
      {
         return (Connection)Proxy.newProxyInstance(
   				PooledConnection.class.getClassLoader(),
   				new Class[]{PooledConnection.class},
   				new ConnectionProxyInvocationHandler(super.dbConnectionPool, super.dbConnectionPool.createConnection()));
      }
   }
   
   /**
    */
   public PooledDataSource()
   {
      this(null);
   }
   
   /**
    */
   public PooledDataSource(SubComponent parent)
   {
      this(parent, "PooledDataSource");
   }
   
   /**
    */
   public PooledDataSource(SubComponent parent, String name)
   {
      super(parent, name);
      super.setPoolObjectFactory(new PooledConnectionFactory(this));
   }
   
   /**
    */
   public PooledDataSource(SubComponent parent, String name, String driverName, String dbURL)
   {
      super(parent, name, driverName, dbURL);
      super.setPoolObjectFactory(new PooledConnectionFactory(this));
   }
   
   /**
    */
   public PooledDataSource(SubComponent parent, String name, String driverName, String dbURL, String user, String password, int minSize, int maxSize, long cleanInterval, long expirationTime)
   {
      super(parent, name, driverName, dbURL, user, password, minSize, maxSize, cleanInterval, expirationTime);
      super.setPoolObjectFactory(new PooledConnectionFactory(this));
   }
   
   
   /* ### METHODS FROM javax.sql.DataSource ### */
   
      
   /*public Connection getConnection() throws SQLException
   {
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
