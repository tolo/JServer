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
package com.teletalk.jserver.comm;

/**
 * Interface for endpoints used in communication. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 2
 */
public interface EndPoint
{
	/**
	 * Returns an EndPointIdentifier object identifying this end point.
	 *  
	 * @return an EndPointIdentifier object identifying this end point.
	 */
	public EndPointIdentifier getEndPointIdentifier();
   
   /**
    * Disconnects this EndPoint.
    * 
    * @since 2.0.1
    */
   public void disconnect();
   
   /**
    * Checks if this end point has estabished a link with the remote end.
    * 
    * @return <code>true</code> if a link has been established, otherwise <code>false</code>. 
    * 
    * @since 2.0.1
    */
   public boolean isLinkEstablished();
}
