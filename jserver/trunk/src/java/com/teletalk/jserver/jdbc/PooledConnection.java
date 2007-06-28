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

/**
 * Connection wrapper interface for creating proxies for pooled connections. This interface is used by {@link PooledDataSource}
 * to make sure that a connection is returned to the pool when the {@link #close()} is called. This interface contains a method        
 * that makes it possible to get the underlying connection.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1
 */
public interface PooledConnection extends Connection
{
   public Connection getTargetConnection();
}
