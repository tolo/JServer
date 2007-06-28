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
package com.teletalk.jserver;

/**
 * A message class used to signal that an error has occurred in a SubSystem.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class SubSystemErrorTask implements Runnable
{
	/**	The sender of the message. */
	public SubSystem subsys;
	
	/**
	 * Constructs a new Exitmessage.
	 * 
	 * @param subsys the sender.
	 */
	public SubSystemErrorTask(SubSystem subsys)
	{
		this.subsys = subsys;
	}
	
	/**
	 * Compares this object against the specified object. The result is true if and only if the argument is not null and is a 
	 * ExitMessage object that contains the same subsys and exitcode as this object
	 * 
	 * @param obj the object to compare with.
	 * 
	 * @return true if the objects are the same; false otherwise
	 */
	public final boolean equals(Object obj)
	{
		if((obj != null) && (obj instanceof SubSystemErrorTask))
		{
			return ((SubSystemErrorTask)obj).subsys == subsys; // && ((ExitMessage)obj).exitCode == exitCode);
		}
		else return false;
	}
   
   /**
    * 
    * @since 2.0
    */
   public void run()
   {
      JServer jserver = JServer.getJServer();
      if( jserver != null )
      {
         jserver.handleSubSystemError(this.subsys);
      }
   }
}
