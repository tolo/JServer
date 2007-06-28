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
package com.teletalk.jserver.rmi.remote;

import java.util.List;

/**
 * Transfer structure class containing information about a subsystem. This class is used to transfer 
 * subsystem data to the administration tool 
 * 
 * @see com.teletalk.jserver.SubSystem
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 2
 */
public class RemoteSubSystemData extends RemoteSubComponentData
{
	static final long serialVersionUID = 4932500809144673272L;
	
	final RemoteSubSystemData[] subSystems;	
	
	/**
	 * Creates a new RemoteSubSystemData object.
	 * 
	 * @param name the name of the subsystem.
	 * @param status the current status of the subsystem.
	 * @param subSystems a list of child subsystems of the subsystem.
	 * @param subComponents a list of child subcomponents of the subsystem.
	 * @param properties a list of properties owned by the subsystem.
	 */
	public RemoteSubSystemData(String name, int status, List subSystems, List subComponents, List properties)
   //public RemoteSubSystemData(String name, int status, List subComponents, List properties)
	{
		super(name, status, subComponents, properties);
		
		this.subSystems = (subSystems != null) ? (RemoteSubSystemData[])subSystems.toArray(new RemoteSubSystemData[]{}) : null;
	}
	
	/**
	 * Gets an array of all child subsystems of the subsystem this object represents.
	 * 
	 * @return an array of RemoteSubSystemData objects.
	 */
	public final RemoteSubSystemData[] getSubSystems()
	{
		return subSystems;
	}
   
   /**
    * Gets a string representation of this object.
    */
   public String toString()
   {
      int childSubSystems = (this.subSystems != null) ? this.subSystems.length : 0;
      int childComponents = (super.subComponents != null) ? super.subComponents.length : 0; 
      int properties = (this.properties != null) ? this.properties.length : 0;
      return "RemoteSubComponentData[name: " + name + ", status: " + status + ", no of subsystems: " + childSubSystems + ", no of subcomponents: " + childComponents + ", no of properties: " + properties + "]";
   }
}
