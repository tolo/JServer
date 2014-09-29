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
package com.teletalk.jadmin.proxy.messages;

import com.teletalk.jadmin.proxy.SubComponentProxy;

/** 
 * Message class used to change the state of a remote subcomponent.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public class SubComponentControlMessage
{
	/*public static final int ENABLE			= 0;
	public static final int DISABLE			= 1;*/
   
   public static final int ENGAGE          = 2;
   public static final int SHUTDOWN    = 3;
   public static final int REINITIALIZE   = 4;
   public static final int KILL                  = 5;
	
	public int control;
	public SubComponentProxy proxy;
	
	/**
	 * Creates a new SubComponentControlMessage.
	 */
	public SubComponentControlMessage(SubComponentProxy proxy, int control)
	{
		this.proxy = proxy;
		this.control = control;	
	}
}
