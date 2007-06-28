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
import java.util.HashMap;

import com.teletalk.jadmin.proxy.messages.ModifyPropertyMessage;
import com.teletalk.jserver.rmi.remote.RemoteProperty;
import com.teletalk.jserver.rmi.remote.RemotePropertyConstants;
import com.teletalk.jserver.rmi.remote.RemotePropertyData;

/**
 * Proxy class for remote Property objects.
 * 
 * @author Tobias Löfstrand
 * @since 1.0
 */
public class PropertyProxy extends RemoteObjectProxy implements RemotePropertyConstants
{
   protected SubComponentProxy parent;

   protected int modificationMode;

   protected HashMap metaData;

   protected int propertyType;

   protected String propertyStringValue;

   /**
    * Creates a new PropertyProxy.
    */
   public PropertyProxy(JServerProxy jserverProxy, SubComponentProxy parent, RemotePropertyData data)
   {
      super(jserverProxy, (parent != null) ? (parent.getFullName() + "." + data.getName()) : data.getName(), data.getName(), data.getValue());

      this.parent = parent;

      propertyStringValue = data.getValue();
      modificationMode = data.getModificationMode();
      metaData = data.getMetaData();
      propertyType = data.getType();
   }

   /**
    * Gets the RMI interface for the remote property.
    */
   public final RemoteProperty getRemoteProperty()
   {
      return (RemoteProperty) this.getRemoteObject();
   }

   /**
    * Called when a RMI interface reference is obtained.
    */
   protected void remoteObjectReferenceObtained() throws RemoteException
   {
      RemoteProperty remoteProperty = getRemoteProperty();

      if (remoteProperty != null)
      {
         propertyValueChanged(remoteProperty.getValueAsString());
         modificationMode = remoteProperty.getModificationMode();
         metaData = remoteProperty.getMetaData();
      }
   }

   /**
    * Gets the parent proxy object of this PropertyProxy.
    */
   public final SubComponentProxy getParent()
   {
      return parent;
   }

   /**
    * Updates the data of this PropertyProxy.
    */
   public synchronized void update(final RemotePropertyData data)
   {
      propertyValueChanged(data.getValue());
      this.modificationMode = data.getModificationMode();
      this.metaData = data.getMetaData();
      this.propertyType = data.getType();
   }

   /**
    * Gets the description of the property.
    */
   public String getDescription()
   {
      if (metaData != null) return (String) metaData.get(PROPERTY_DESCRIPTION_KEY);
      else return null;
   }

   /**
    * Gets the enumerations (for EnumProperty objects).
    */
   public Object[] getEnumerations()
   {
      if (metaData != null) return (Object[]) metaData.get(ENUMERATIONS_KEY);
      else return null;
   }

   /**
    * Gets the property type (for instance <code>RemotePropertyConstants.STRING_TYPE</code>.
    */
   public int getType()
   {
      return propertyType;
   }

   /**
    * Called when an update value for the property is received from the server.
    */
   public void propertyValueChanged(String newValue)
   {
      this.propertyStringValue = newValue;
      this.setDisplayValue(newValue);
   }

   /**
    * Gets the value of the Property as a String.
    * 
    * @return String representation of the value.
    */
   public String getValueAsString()
   {
      return propertyStringValue;
   }

   /**
    * Method to parse a new value for the Property from a String.
    * 
    * @param strVal the String from which the value will be parsed.
    */
   public void setPropertyValue(String strVal)
   {
      jserverProxy.putMsg(new ModifyPropertyMessage(this, strVal));
   }

   /**
    * Gets all the metadata of the property.
    */
   public HashMap getMetaData()
   {
      return metaData;
   }

   /**
    * Gets the metadata with the specified key of the property.
    */
   public Object getMetaData(Object key)
   {
      return metaData.get(key);
   }

   /**
    * Returns the value of the modifiable flag.
    * 
    * @return true if the Property is modifiable, otherwise false.
    */
   public boolean isModifiable()
   {
      return this.modificationMode != NOT_MODIFIABLE;
   }

   /**
    * Returns the modificationMode
    * 
    * @return the value of modificationMode.
    */
   public final int getModificationMode()
   {
      return modificationMode;
   }
}
