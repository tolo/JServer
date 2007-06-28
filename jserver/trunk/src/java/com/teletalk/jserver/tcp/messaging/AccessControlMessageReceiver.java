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
 * Message receiver interface that enables identification of messages for access control list checks.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0.2 (20050331)
 * 
 * @see MessagingManager
 * @see com.teletalk.jserver.net.acl.AccessControlListHandler
 */
public interface AccessControlMessageReceiver extends MessageReceiver
{
   /**
    * Called to identify a operation (message) prior to access control check. The returned string will be used to perform a lookup in the 
    * access control list to se if the operation is allowed to be executed from the address it was send from. If the message cannot be 
    * identified or if access check is disabled, this implementation should return <code>null</code>. In other words, 
    * <b>access checks will only be performed for non-null return values</b>.<br> 
    * <br>
    * <i>Note:</i>If the message body needs to be parsed to identify the message, the method {@link Message#setMessageBodyCachingEnabled(boolean)} 
    * should be called to cache the message, i.e. to make it available when {@link MessageReceiver#messageReceived(Message)} is called.<br>
    * <br>
    * Subclasses that wish to enable the use the access control list and  should override this method and return string identification of 
    * incomming messages.
    */
   public String identify(final Message message) throws Exception;
}
