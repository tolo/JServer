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
 * Transfer structure class containing information about a logger. This class is used to transfer 
 * logger data to the administration tool 
 *  * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 2
 *  
 * @deprecated as of 2.0. This class only exists to enable backwards compatability in JAdmin.
 */
public class RemoteLoggerData extends RemoteSubComponentData
{
	static final long serialVersionUID = -232348660993343236L;
	
	/**
	 * Creates a new RemoteLoggerData object.
	 * 
	 * @param name the name of the logger.
	 * @param status the current status of the logger.
	 * @param subComponents a list of child subcomponents of the subcomponent.
	 * @param properties a list of properties owned by the subcomponent.
	 */
	public RemoteLoggerData(String name, int status, List subComponents, List properties)
	{
		super(name, status, subComponents, properties);
	}
}
