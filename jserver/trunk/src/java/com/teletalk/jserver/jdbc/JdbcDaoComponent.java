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

import java.sql.Connection;

import javax.sql.DataSource;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.jdbc.CannotGetJdbcConnectionException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.SQLExceptionTranslator;

import com.teletalk.jserver.SubComponent;

/**
 * SubComponent base class providing support similar to the Spring class <code>org.springframework.jdbc.core.support.JdbcDaoSupport</code> 
 * (this class actually borrows the implementation of that class).  
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1
 */
public class JdbcDaoComponent extends SubComponent implements InitializingBean
{
   private JdbcTemplate jdbcTemplate; 
      
   /**
    */
   public JdbcDaoComponent()
   {
      this(null, "JdbcDaoComponent");
   }
   
   /**
    */
   public JdbcDaoComponent(String name)
   {
      this(null, name);
   }
   
   /**
    */
   public JdbcDaoComponent(SubComponent parent, String name)
   {
      super(parent, name);
   }
   
   
   /* ### METHODS COPIED FROM JDBCDAOSupport ### */
   

   /**
    * Set the JDBC DataSource to be used by this DAO and initializes the internal JdbcTemplate.
    */
   public final void setDataSource(DataSource dataSource) 
   {
      this.jdbcTemplate = createJdbcTemplate(dataSource);
   }
   
   /**
    * Return the JDBC DataSource used by this DAO.
    */
   public final DataSource getDataSource() 
   {
      return (this.jdbcTemplate != null ? this.jdbcTemplate.getDataSource() : null);
   }

   /**
    * Create a JdbcTemplate for the given DataSource. This method is called by {@link #setDataSource(DataSource)} 
    * when a JdbcTemplate is to be created for the DataSource.
    */
   protected JdbcTemplate createJdbcTemplate(DataSource dataSource) 
   {
      return new JdbcTemplate(dataSource);
   }

   /**
    * Set the JdbcTemplate for this DAO explicitly,
    * as an alternative to specifying a DataSource.
    */
   public final void setJdbcTemplate(JdbcTemplate jdbcTemplate) 
   {
      this.jdbcTemplate = jdbcTemplate;
   }

   /**
    * Return the JdbcTemplate for this DAO,
    * pre-initialized with the DataSource or set explicitly.
    */
   public final JdbcTemplate getJdbcTemplate() 
   {
     return jdbcTemplate;
   }

   public final void afterPropertiesSet() throws Exception 
   {
      if (this.jdbcTemplate == null) 
      {
         throw new IllegalArgumentException("dataSource or jdbcTemplate is required");
      }
      initDao();
   }

   /**
    * Subclasses method for DAO initialization.
    */
   protected void initDao() throws Exception 
   {
   }

   /**
    * Get a JDBC Connection, either from the current transaction or a new one.
    */
   protected final Connection getConnection() throws CannotGetJdbcConnectionException 
   {
      return DataSourceUtils.getConnection(getDataSource());
   }

   /**
    * Return the SQLExceptionTranslator of this DAO's JdbcTemplate, for translating SQLExceptions in custom JDBC access code.
    */
   protected final SQLExceptionTranslator getExceptionTranslator() 
   {
      return this.jdbcTemplate.getExceptionTranslator();
   }

   /**
    * Close the given JDBC Connection if necessary, created via this bean's
    * DataSource, if it isn't bound to the thread.
    */
   protected final void closeConnectionIfNecessary(Connection con) 
   {
      DataSourceUtils.releaseConnection(con, getDataSource());
   }
}
