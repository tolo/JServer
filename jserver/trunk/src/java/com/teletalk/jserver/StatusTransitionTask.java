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
 * Class used to represent a status transition in a SubComponent or SubSystem. 
 * 
 * @author Tobias Löfstrand
 *  
 * @since 2.0
 */
public class StatusTransitionTask implements Runnable
{
   public final static String StatusTransitionTaskThreadName = "StatusTransitionTask"; 
   
   
   private final SubComponent targetComponent;
   
   private final int statusTransitionType;
   
   private final String reason;
   
   private final boolean interrupt;
   
   /**
    * Creates a new StatusTransitionTask.
    */
   public StatusTransitionTask(SubComponent targetComponent, int stateTransitionType)
   {
      this(targetComponent, stateTransitionType, null, false);
   }
   
   /**
    * Creates a new StatusTransitionTask.
    */
   public StatusTransitionTask(SubComponent targetComponent, int stateTransitionType, String reason)
   {
      this(targetComponent, stateTransitionType, reason, false);
   }
   
   /**
    * Creates a new StatusTransitionTask.
    */
   public StatusTransitionTask(SubComponent targetComponent, int stateTransitionType, boolean interrupt)
   {
      this(targetComponent, stateTransitionType, null, interrupt);
   }
   
   /**
    * Creates a new StatusTransitionTask.
    */
   public StatusTransitionTask(SubComponent targetComponent, int stateTransitionType, String reason, boolean interrupt)
   {
      if( (stateTransitionType < JServerConstants.STATUS_TRANSITION_TYPE_ENGAGE) || (stateTransitionType > JServerConstants.STATUS_TRANSITION_TYPE_CRITICAL_ERROR) )
      {
         throw new RuntimeException("Invalid stateTransitionType:  " + stateTransitionType + "!");
      }
      
      this.targetComponent = targetComponent;
      this.statusTransitionType = stateTransitionType;
      this.reason = reason;
      this.interrupt = interrupt;
   }
   
   /**
    * Gets the status transition type.
    */
   public int getStatusTransitionType()
   {
      return this.statusTransitionType;
   }
   
   /**
    * Gets the status transition reason.
    */
   public String getReason()
   {
      return this.reason;
   }
   
   /**
    * Gets flag indicating if this status transition may interrupt a status transition already in process.
    */
   public boolean isInterrupt()
   {
      return this.interrupt;
   }
   
   /**
    * Translates the status transition type to a string.
    */
   public String translateStatusTransitionType()
   {
      return JServerConstants.statusTransitionTypeNames[statusTransitionType];
   }
   
   /**
    * The famous run method. This method only invokes {@link #execute()}.
    */
   public void run()
   {
      this.execute();
   }
   
   /**
    * Executes the status transition.
    */
   public boolean execute()
   {
      Thread currentThread = Thread.currentThread();
      String threadOldName = currentThread.getName();
      //currentThread.setName(this.targetComponent.getFullName() + "." + StatusTransitionTaskThreadName);
      currentThread.setName(this.targetComponent.getFullName());
      try
      {
         return this.targetComponent.executeStatusTransition(this);
      }
      finally
      {
         currentThread.setName(threadOldName);
      }
   }

   /**
    * Gets a string representation of this object.
    */
   public String toString()
   {
      if( this.targetComponent != null ) return "StatusTransitionTask(" + this.targetComponent.getFullName() + ", " + JServerConstants.statusTransitionTypeNames[this.statusTransitionType] + ")";
      else return "StatusTransitionTask(" + JServerConstants.statusTransitionTypeNames[this.statusTransitionType] + ")";
   }
}
