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
 * Listener interface for receiving incomming messages from remote messaging systems. A MessageReceiver is 
 * registered with a receiver name using one of the methods {@link MessagingManager#registerMessageReceiver(MessageReceiver, String)}.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.2
 * 
 * @see MessagingManager
 */
public interface MessageReceiver
{
	/**
	 * Called by the MessagingManager to notify this MessageReceiver that a new message has arrived 
	 * from a remote messaging system. The implementation of this method is free to handle the message in a 
	 * separate thread. This means that this method may return before the message is fully read. Note though, that 
	 * a timeout will occur if no read progress of the message is made duing the timeout specified by a property of 
	 * {@link MessagingManager}, accessible by the method {@link MessagingManager#getMessageReadTimeout()}.<br>
	 * <br>
	 * If this implementation wishes to ignore the message, it should call the method {@link Message#signalReadCompletion()}.
	 * 
	 * @param message the received message.
	 */
	public void messageReceived(Message message);
}
