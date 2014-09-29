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
package com.teletalk.jserver.util;

import java.util.HashMap;
import java.util.Iterator;

import com.teletalk.jserver.JServerConstants;
import com.teletalk.jserver.periodic.PeriodicAction;
import com.teletalk.jserver.property.BooleanProperty;
import com.teletalk.jserver.property.MapProperty;
import com.teletalk.jserver.property.NumberProperty;

/**
 * Periodic action class for checking free space on drives.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.2
 */
public class DiskSpaceCheckAction extends PeriodicAction
{
   protected final MapProperty pathsToCheck;
   
   protected final NumberProperty lowDiskSpaceErrorCode;
   
   protected final BooleanProperty diskSpaceCheckErrorsCritical;

   /**
    * Creates a new DiskSpaceCheckAction with the specified name
    * 
    * @param name the name of the DiskSpaceCheckAction. 
    */
   public DiskSpaceCheckAction(String name)
   {
      super(name);
      
      this.pathsToCheck = new MapProperty(this, "pathsToCheck", null, MapProperty.MODIFIABLE_NO_RESTART);
      this.pathsToCheck.setDescription("The drives (path) to check and corresponding minimum disk free space in MB.");
      super.addProperty(this.pathsToCheck);
      
      this.lowDiskSpaceErrorCode = new NumberProperty(this, "lowDiskSpaceErrorCode", JServerConstants.LOG_MESSAGE_ID_LOW_DISK_SPACE, NumberProperty.MODIFIABLE_NO_RESTART);
      this.lowDiskSpaceErrorCode.setDescription("The critical error code that will be logged when a drive/path has too low disk space free.");
      super.addProperty(this.lowDiskSpaceErrorCode);
      
      this.diskSpaceCheckErrorsCritical = new BooleanProperty(this, "diskSpaceCheckErrorsCritical", false, BooleanProperty.MODIFIABLE_NO_RESTART);
      this.diskSpaceCheckErrorsCritical.setDescription("Boolean property indicating if disk space check errors should be logged as critical.");
      super.addProperty(this.diskSpaceCheckErrorsCritical);
   }
   
   /**
    * Enables this DiskSpaceCheckAction.
    */
   protected void doInitialize()
   {
      super.doInitialize();
      
      // Attempt to get old property "drives"
      super.initFromConfiguredProperty(this.pathsToCheck, "drives", false, true);
   }

   /**
    *  Executes this DiskSpaceCheckAction.
    */
   public boolean execute() throws Exception
   {
      HashMap drivesAndSpace = this.pathsToCheck.getMappings();
      
      if( drivesAndSpace != null )
      {
         Iterator drivesIt = drivesAndSpace.keySet().iterator();
         String path;
         String minDiskFreeSpaceString;
         long minDiskFreeSpace;
         long diskFreeSpace;
         
         while(drivesIt.hasNext())
         {
            path = (String)drivesIt.next();
         
            if( path != null )
            {
               try
               {
                  super.setActionStatus("Checking drive " + path + ".");
                  
                  minDiskFreeSpaceString = (String)drivesAndSpace.get(path);
                  if( minDiskFreeSpaceString != null )
                  {
                     minDiskFreeSpace = Long.parseLong( minDiskFreeSpaceString.trim() );
                  }
                  else
                  {
                     minDiskFreeSpace = 1;
                  }
                  
                  if( (path.trim().length() == 1) && Character.isLetter(path.trim().charAt(0)) )
                  {
                     path += ":\\";
                  }
                  
                  diskFreeSpace = SystemInfo.getDiskFreeSpace(path);
                  
                  if( super.isDebugMode() ) logDebug("Space free for path '" + path +"' - " + (diskFreeSpace/(1024*1024)) + " MB (minDiskFreeSpace: " + minDiskFreeSpace + " MB).");
                  
                  if( diskFreeSpace < (minDiskFreeSpace*1024*1024) )
                  {
                     logCriticalError("Low disk space (" + (diskFreeSpace/(1024*1024)) + " MB) path '" + path +"' (alert limit is " + minDiskFreeSpace +" MB)!", this.lowDiskSpaceErrorCode.intValue());
                  }
               }
               catch(Exception e)
               {
                  if( this.diskSpaceCheckErrorsCritical.booleanValue() )
                  {
                     logCriticalError("Failed to check free disk space for path '" + path +"'!", e, JServerConstants.LOG_MESSAGE_ID_DISK_SPACE_CHECK_ERROR);
                  }
                  else
                  {
                     logError("Failed to check free disk space for path '" + path +"'!", e);
                  }
               } 
            }
         }
      }
 
      return true;
   }
}
