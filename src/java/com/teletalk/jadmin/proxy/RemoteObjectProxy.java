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
package com.teletalk.jadmin.proxy;

import java.rmi.RemoteException;

import com.teletalk.jserver.rmi.remote.RemoteObject;

/**
 * Abstract base class for all classes implementing proxies for remote objects in a server.
 * 
 * @author Tobias Löfstrand
 * @since 1.0
 */
public abstract class RemoteObjectProxy
{

   private boolean failedToGetRemoteObject = false;

   protected RemoteObject remoteObject;

   protected JServerProxy jserverProxy;

   protected final String fullName;

   protected final String name;

   protected String displayName;

   protected String displayValue;

   /**
    * Creates a new RemoteObjectProxy.
    */
   public RemoteObjectProxy(JServerProxy jserverProxy, String fullName, String name, String displayValue)
   {
      this.jserverProxy = jserverProxy;
      this.fullName = fullName;
      this.name = name;
      setDisplayName(name);
      setDisplayValue(displayValue);
   }

   /**
    * Creates a new RemoteObjectProxy.
    */
   public RemoteObjectProxy(JServerProxy jserverProxy, String fullName, String name)
   {
      this.jserverProxy = jserverProxy;
      this.fullName = fullName;
      this.name = name;
      setDisplayName(name);
      setDisplayValue(null);
   }

   /**
    * Gets the full name of this proxy.
    */
   public final String getFullName()
   {
      return fullName;
   }

   /**
    * Gets the name of this proxy.
    */
   public final String getName()
   {
      return name;
   }

   /**
    * Gets the name for this proxy that will be used for displaying purposes.
    */
   public final String getDisplayName()
   {
      return displayName;
   }

   /**
    * Sets the name for this proxy that will be used for displaying purposes.
    */
   public final void setDisplayName(final String displayName)
   {
      if (displayName != null)
      {
         if (displayName.length() > 60) this.displayName = displayName.substring(0, 57) + "...";
         else this.displayName = displayName;
      }
      else this.displayName = "";
   }

   /**
    * Gets the value for this proxy that will be used for displaying purposes.
    */
   public final String getDisplayValue()
   {
      return displayValue;
   }

   /**
    * Sets the value for this proxy that will be used for displaying purposes.
    */
   public final void setDisplayValue(String displayValue)
   {
      if (displayValue != null)
      {
         if (displayValue.length() > 60) this.displayValue = displayValue.substring(0, 57) + "...";
         else this.displayValue = displayValue;
      }
      else this.displayValue = "";
   }

   /**
    * Gets the RMI interface used to communicate with the server object that this object serves as a proxy for.
    */
   protected final RemoteObject getRemoteObject()
   {
      try
      {
         if (remoteObject == null && !failedToGetRemoteObject)
         {
            remoteObject = jserverProxy.findRemoteObject(this);

            try
            {
               if (remoteObject != null) remoteObjectReferenceObtained();
            }
            catch (Exception e)
            {
            }
         }

         return remoteObject;
      }
      catch (RemoteException re)
      {
         if (jserverProxy != null) jserverProxy.recordMessage("Failed to get remote object for " + getFullName() + "!");

         failedToGetRemoteObject = true;
         return null;
      }
   }

   /**
    * Called when a RMI interface reference is obtained.
    */
   protected abstract void remoteObjectReferenceObtained() throws RemoteException;

   /**
    * Generates a string representation of this object.
    */
   public String toString()
   {
      return this.getClass() + "(" + this.getDisplayName() + " = " + this.getDisplayValue() + ")";
   }
}
