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
package com.teletalk.jserver.tcp.messaging.support;

import java.io.IOException;

import com.teletalk.jserver.tcp.messaging.Message;
import com.teletalk.jserver.tcp.messaging.MessageHeader;
import com.teletalk.jserver.tcp.messaging.MessagingEndPoint;

/**
 * Message implementation to be used in environments where a messaging manager is to be used from different class loaders.
 *  
 * @author Tobias Löfstrand
 * 
 * @since 2.1.2 (20060303)
 * 
 * @see com.teletalk.jserver.tcp.messaging.support.ClassLoaderAwareMessageDispatcher
 */
public class ClassLoaderAwareMessage extends Message
{
   private ClassLoader classLoader;

	/**
	 * Creates a new ClassLoaderAwareMessage.
	 * 
	 * @param header the header of the message.
	 * @param endPoint the endpoint on which the message was received and from which the message body is to be read.
	 */
   public ClassLoaderAwareMessage(MessageHeader header, MessagingEndPoint endPoint)
   {
      super(header, endPoint);
      
      this.classLoader = null;
   }
   
   /**
    * Sets the class loader that should be used for resolving classes when reading object message bodies, 
    * using the method {@link #getBodyAsObject()}. 
    * 
    * @param classLoader the class loader to be used for resolving classes.
    */
   public void setClassLoader(ClassLoader classLoader)
   {
		this.classLoader = classLoader;
   }

	/**
	 * Gets the message body as an object (by attempting to deserialize the message body data).
	 * This method uses the class loader set by the method {@link #setClassLoader(ClassLoader)} for resolving classes.<br>
	 * <br>
	 * <i>Note: </i> calling this method will "consume" the message body, which means that 
	 * repeated calls to this method, and the other methods for getting the message body, will 
	 * return <code>null</code>.
	 * 
	 * @return the message body as an object deserialized from the message body data.
	 */
   public synchronized Object getBodyAsObject() throws IOException
   {
      return super.getBodyAsObject(this.classLoader);
   }
}
