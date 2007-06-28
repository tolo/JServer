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
 * Transfer structure class containing information about an appender component. This class is used to transfer 
 * appender component data to the administration tool. 
 * 
 * @see com.teletalk.jserver.log.AppenderComponent
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0
 */
public class RemoteAppenderComponentData extends RemoteSubComponentData
{
   static final long serialVersionUID = -232348660993343236L;
   
   /**
    * Creates a new RemoteAppenderComponentData object.
    * 
    * @param name the name of the logger.
    * @param status the current status of the logger.
    * @param subComponents a list of child subcomponents of the subcomponent.
    * @param properties a list of properties owned by the subcomponent.
    */
   public RemoteAppenderComponentData(String name, int status, List subComponents, List properties)
   {
      super(name, status, subComponents, properties);
   }
}
