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
import java.util.ArrayList;
import java.util.HashMap;

import com.teletalk.jadmin.proxy.messages.VectorPropertyUpdateMessage;
import com.teletalk.jserver.rmi.remote.RemotePropertyData;
import com.teletalk.jserver.rmi.remote.RemoteVectorProperty;

/**
 * Proxy class for remote VectorProperty objects.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.0
 */
public class VectorPropertyProxy extends PropertyProxy
{
	private interface ListenerNotification
	{
		public void notifyListener(VectorPropertyProxyListener l);
	}
	
	private static long updateInterval = 5*1000;
	private static boolean autoUpdateEnabled = true;
	
	private HashMap operations;	private String[][] itemStrings;	//private long lastUpdate;	private boolean dirty;	private ArrayList listeners;
	
	/**
	 * Creates a new VectorPropertyProxy.
	 */	public VectorPropertyProxy(JServerProxy jserverProxy, SubComponentProxy parent, RemotePropertyData data)
	{
		super(jserverProxy, parent, data);				this.operations = null;
		this.itemStrings = null;
		//this.lastUpdate = System.currentTimeMillis();		this.dirty = true;
		this.listeners = new ArrayList();
	}	
	/**
	 * Gets the RMI interface for the remote VectorProperty.
	 */
	public final RemoteVectorProperty getRemoteVectorProperty()	{		return (RemoteVectorProperty)this.getRemoteObject();	}
	/**
	 * Gets the interval in milliseconds at which this VectorPropertyProxy should update its contents from the server.
	 */
	public static long getUpdateInterval()
	{
		return updateInterval;	
	}
	
	/**
	 * Sets the interval in milliseconds at which this VectorPropertyProxy should update its contents from the server.
	 */
	public static void setUpdateInterval(long interval)
	{
		if(interval > 0)
			updateInterval = interval;
	}
	
	/**
	 * Checks if automatic updating of the contents of this VectorPropertyProxy is enabled.
	 */
	public static boolean isAutoUpdateEnabled()
	{
		return autoUpdateEnabled;
	}
	
	/**
	 * Sets the flag indicating if automatic updating of the contents of this VectorPropertyProxy is enabled.
	 */
	public static void setAutoUpdateEnabled(boolean enabled)
	{
		autoUpdateEnabled = enabled;
	}
	
	/**
	 * Forces an update of the contents of this VectorPropertyProxy.
	 */
	public synchronized void forceUpdate()
	{
		dirty = true;
		update();
	}
	
	/**
	 * Updates the contents of this VectorPropertyProxy if needed.
	 */
	public void update()
	{
		try
		{
			boolean updateOps = false;
				
			HashMap newOps = getRemoteVectorProperty().getExternalOperations();
		
			if( (operations == null) || (newOps == null) ) updateOps = true;
			else if(!operations.equals(newOps)) updateOps = true;		   

			if(updateOps) operations = newOps;
				
			if(dirty)
			{
				this.itemStrings = getRemoteVectorProperty().getItemsAsStrings();
			}
			
			if(dirty || updateOps)
			{
				final boolean _dirty = dirty;
				final boolean _updateOps = updateOps;
				
				final ListenerNotification l = new ListenerNotification()
				{
					public void notifyListener(VectorPropertyProxyListener l)
					{
						if(_dirty) l.itemsUpdated(getItemsAsStrings());
						if(_updateOps) l.operationsUpdated(getExternalOperations());
					}
				};
				this.notifyListeners(l);
			}
			
			dirty = false;
			//this.lastUpdate = System.currentTimeMillis();
			
			super.propertyValueChanged(getRemoteVectorProperty().getValueAsString());
			jserverProxy.proxyUpdated(this);
		}
		catch(Exception e)
		{
         e.printStackTrace();
			jserverProxy.recordMessage("Error while updating vectorproperty (" + this.getFullName() + ") - " + e.toString() + ".");	
		}
	}
	
	/**
	 * Mehtod to notify all listeners of this object of a certain event.
	 */
	private synchronized void notifyListeners(final ListenerNotification notification)
	{
		if(notification != null)
		{
			//ArrayList v = (ArrayList)listeners.clone();
			boolean error;
			Exception lastException;

			for(int i=0; i<listeners.size(); i++)
			{
				VectorPropertyProxyListener l = (VectorPropertyProxyListener)listeners.get(i);

				if(l != null)
				{
					error = true;
					lastException = null;
					for(int q=0; (q<3) && error; q++) // Make a maximum of 3 attempts before removing the listener
					{
						try
						{
							notification.notifyListener(l);
							error = false;
						}
						catch(Exception e)
						{
							lastException = e;
						}
					}
				
					if(error)
					{
						System.out.println("Error occured while notifying listener of " + this.getFullName() + ". Removing listener! Stack trace is: ");
						lastException.printStackTrace();
						listeners.remove(l);
					}
				}
			}
		}
	}
	
	/**
	 * Registers a VectorPropertyProxyListener with this VectorPropertyProxy.
	 */
	public synchronized void registerVectorPropertyProxyListener(VectorPropertyProxyListener listener)
	{
		if(!listeners.contains(listener))
		{
			listeners.add(listener);
			
			listener.itemsUpdated(getItemsAsStrings());
			listener.operationsUpdated(getExternalOperations());
		}
	}
	
	/**
	 * Unregisters a VectorPropertyProxyListener with this VectorPropertyProxy.
	 */
	public synchronized void unregisterVectorPropertyProxyListener(final VectorPropertyProxyListener listener)
	{
		listeners.remove(listener);
	}
	
	/**
	 * Called to notify all the listeners of this VectorPropertyProxy that one or more items have been added.
	 */
	public void itemAdded(final String[] keys, final String[] descriptions)
	{
		final ListenerNotification l = new ListenerNotification()
			{
				public void notifyListener(VectorPropertyProxyListener l)
				{
					l.itemAdded(keys, descriptions);
				}
			};
		
		this.notifyListeners(l);
	}
	
	/**
	 * Called to notify all the listeners of this VectorPropertyProxy that one or more items have been removed.
	 */
	public void itemRemoved(final String[] keys, final String[] descriptions)
	{
		final ListenerNotification l = new ListenerNotification()
			{
				public void notifyListener(VectorPropertyProxyListener l)
				{
					l.itemRemoved(keys, descriptions);
				}
			};
		
		this.notifyListeners(l);
	}
	
	/**
	 * Called to notify all the listeners of this VectorPropertyProxy that one or more items have been modified.
	 */
	public void itemModified(final String[] keys, final String[] descriptions)
	{
		final ListenerNotification l = new ListenerNotification()
			{
				public void notifyListener(VectorPropertyProxyListener l)
				{
					l.itemModified(keys, descriptions);
				}
			};
		
		this.notifyListeners(l);
	}
	
	/**
	 * Called to notify all the listeners of this VectorPropertyProxy that the contents should be cleared.
	 */
	public void clearItems()
	{
		final ListenerNotification l = new ListenerNotification()
			{
				public void notifyListener(VectorPropertyProxyListener l)
				{
					l.clearItems();
				}
			};
		
		this.notifyListeners(l);
	}
	
	/**
	 * Called when an update value for the property is received from the server.
	 */
	public void propertyValueChanged(final String value) 
	{
		super.propertyValueChanged(value);		dirty = true;
    }
	
	/**
	 * Checks if the remote VectorProperty is empty.
	 */
	public boolean isEmpty() throws RemoteException
	{
		return this.getRemoteVectorProperty().isEmpty();
	}
	
	/**
	 * Returns the number of objects stored in the vector of this VectorProperty.
	 * 
	 * @return the size of the vector.
	 */
	public int size() throws RemoteException
	{
		return this.getRemoteVectorProperty().size();
	}
	
	/**
	 * Returns the object at the given index.
	 * 
	 * @param i index for an object.
	 * 
	 * @return the object at given index.
	 */
	public Object get(int i) throws RemoteException
	{
		return this.getRemoteVectorProperty().get(i);
	}
	
	/**
	 * Returns the object with the given key.
	 * 
	 * @param key a key of an item in the remote vectorproperty.
	 * 
	 * @return the object at given index.
	 */
	public Object get(String key) throws RemoteException
	{
		return this.getRemoteVectorProperty().get(key);
	}

	/**
	 * Checks if an object with the specified unique index is contained in the remote vectorproperty.
	 * 
	 * @param uniqueIndex unique index for an object.
	 * 
	 * @return true if an object with the specified unique index is contained in this vector, otherwise false.
	 */
	public boolean containsKey(String key) throws RemoteException
	{
		return this.getRemoteVectorProperty().containsKey(key);
	}
	
	/**
	 * Checks if the specified object is contained in the remote vectorproperty.
	 */
	public boolean contains(Object item) throws RemoteException
	{
		return this.getRemoteVectorProperty().contains(item);
	}
	
	/**
	 * Returns a HashMap containing unique indices matched with
	 * string representaions of the actual objects stored in the remote vectorproperty.
	 * 
	 * @return HashMap containing unique indices matched with
	 * string representaions of the actual objects stored in the remote vectorproperty.
	 * 
	 * @exception RemoteException if there was an error during remote access of this method.
	 */
	public String[][] getItemsAsStrings()
	{
		if(itemStrings == null)
			forceUpdate();
		
		if(itemStrings != null)
			return (String[][])itemStrings.clone();
		else
			return new String[0][0];
	}
	
	/**
	 * Gets all external operations for the associated VectorProperty.
	 * 
	 * @param name the internal name of the operation.
	 * 
	 * @return hashtable containing internal name/display name mappings of the operations.
	 */
	public HashMap getExternalOperations()
	{
		if(operations == null)
			forceUpdate();
		
		if(operations != null)
			return (HashMap)operations.clone();
		else
			return new HashMap();
	}
	
	/**
	 * This method is called when an external operation is called on the associated VectorProperty object.
	 * 
	 * @param operationName the internal name of the operation that was called.
	 * @param uniqueIndices an array containing unique indices that was selected for the operation call.
	 * 
	 * @exception Exception if there was an error during remote access of this method or some other error.
	 */
	public void externalOperationCalled(String operationName, String[] keys) throws Exception
	{
		getRemoteVectorProperty().externalOperationCalled(operationName, keys);
		this.jserverProxy.putMsg(new VectorPropertyUpdateMessage());
	}
}
