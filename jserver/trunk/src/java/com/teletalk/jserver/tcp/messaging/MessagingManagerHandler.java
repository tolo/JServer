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
package com.teletalk.jserver.tcp.messaging;

/**
 * Base interface for handler classes used with a {@link MessagingManager}.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20050517)
 */
public interface MessagingManagerHandler
{
   /**
    * Sets the MessagingManager associated with this handler.
    */
   public void setMessagingManager(MessagingManager messagingManager);
   
   /**
    * Called by the associated MessagingManager to initialize this handler. This method is called when the MessagingManager 
    * is starting up, after {@link #setMessagingManager(MessagingManager)} has been called.
    */
   public void initialize();
   
   /**
    * Called by the associated MessagingManager to shut down this handler. This method is called when the MessagingManager 
    * is shutting down. 
    */
   public void shutDown();
}
