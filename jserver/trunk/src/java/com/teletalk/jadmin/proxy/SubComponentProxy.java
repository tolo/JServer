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
import java.util.List;

import com.teletalk.jadmin.proxy.messages.SubComponentControlMessage;
import com.teletalk.jserver.rmi.remote.RemoteAppenderComponentData;
import com.teletalk.jserver.rmi.remote.RemoteLoggerData;
import com.teletalk.jserver.rmi.remote.RemotePropertyData;
import com.teletalk.jserver.rmi.remote.RemoteSubComponent;
import com.teletalk.jserver.rmi.remote.RemoteSubComponentData;

/**
 * Proxy class for remote SubComponent objects.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.0
 */
public class SubComponentProxy extends RemoteObjectProxy implements com.teletalk.jserver.JServerConstants
{
	protected SubComponentProxy parent;
	
	protected ArrayList subComponents;
	protected ArrayList properties;
	
	private int status;
   
   private boolean isAppenderComponent = false;
	
	/**
	 * Creates a new top level SubComponentProxy.
	 */
	protected SubComponentProxy(final RemoteSubComponentData data)
	{
		super(null, data.getName(), data.getName());
		
		this.status = data.getStatus();
		
		subComponents = new ArrayList(); 
		properties = new ArrayList();
	}
	
	/**
	 * Creates a new SubComponentProxy.
	 */
	public SubComponentProxy(final JServerProxy jserverProxy, final SubComponentProxy parent, final RemoteSubComponentData data)	{
		super(jserverProxy, (parent != null) ? (parent.getFullName() + "." + data.getName()) : data.getName(), data.getName());
		
		this.status = data.getStatus();
		
		subComponents = new ArrayList(); 
		properties = new ArrayList();
	}
   
   /**
    * @return Returns the isAppenderComponent.
    */
   public boolean isAppenderComponent()
   {
      return this.isAppenderComponent;
   }
   /**
    * @param isAppenderComponent The isAppenderComponent to set.
    */
   public void setAppenderComponent(boolean isAppenderComponent)
   {
      this.isAppenderComponent = isAppenderComponent;
   }

	/**
	 * Gets the RMI interface for the remote subcomponent.
	 */
	public final RemoteSubComponent getRemoteSubComponent()	{		return (RemoteSubComponent)this.getRemoteObject();	}	
	/**
	 * Called when a RMI interface reference is obtained.
	 */
	protected void remoteObjectReferenceObtained() throws RemoteException
	{
		RemoteSubComponent remoteSubComponent = getRemoteSubComponent();				
		if(remoteSubComponent != null)
		{			this.setStatus(remoteSubComponent.getStatus());		}	
	}
	
	/**
	 * Creates proxy objects for all the child SubComponentProxy objects of this SubComponentProxy.
	 */
	void buildSubComponentProxies(final RemoteSubComponentData[] remoteSubComponentDataArray)
	{
		RemoteSubComponentData remoteSubComponentData  = null;
		SubComponentProxy subComponentProxy = null;
		ArrayList subComponentsCopy = new ArrayList(subComponents);
		
		boolean remove;
		
		//Remove old
		for(int i=0; i<subComponentsCopy.size(); i++)
		{
			subComponentProxy = (SubComponentProxy)subComponentsCopy.get(i);
			if(subComponentProxy != null)
			{
				remove = true;
			
				for(int q=0; (remoteSubComponentDataArray != null) && (q<remoteSubComponentDataArray.length); q++)
				{
					remoteSubComponentData = (RemoteSubComponentData)remoteSubComponentDataArray[q];
					
					if( (remoteSubComponentData != null) && remoteSubComponentData.getName().equals(subComponentProxy.getName()) )
					{
						remove = false;
						break;
					}
				}
			
				if(remove)
					removeSubComponentProxy(subComponentProxy);
			}
		}
		
		boolean add;
		
		//Add new and update...
		for(int i=0; (remoteSubComponentDataArray != null) && (i<remoteSubComponentDataArray.length); i++)
		{
			remoteSubComponentData = (RemoteSubComponentData)remoteSubComponentDataArray[i];
			if(remoteSubComponentData != null)
			{
				add = true;
			
				for(int q=0; q<subComponents.size(); q++)
				{
					subComponentProxy  = (SubComponentProxy)subComponents.get(q);
					
					if( (subComponentProxy != null) && remoteSubComponentData.getName().equals(subComponentProxy.getName()) )
					{
						add = false;
						break;
					}
				}
			
				if(add)
				{
					if(remoteSubComponentData instanceof RemoteLoggerData) // Version 1
               {
						subComponentProxy = new LoggerProxy(jserverProxy, this, (RemoteLoggerData)remoteSubComponentData);
               }
               else if(remoteSubComponentData instanceof RemoteAppenderComponentData) // Version 2
               {
                  subComponentProxy = new SubComponentProxy(jserverProxy, this, remoteSubComponentData);
                  subComponentProxy.setAppenderComponent(true);
               }
					else
               {
						subComponentProxy = new SubComponentProxy(jserverProxy, this, remoteSubComponentData);
               }
						
					addSubComponentProxy(subComponentProxy);
				}
			
				subComponentProxy.setStatus(remoteSubComponentData.getStatus());
				subComponentProxy.buildSubComponentProxies(remoteSubComponentData.getSubComponents());
				subComponentProxy.buildPropertyProxies(remoteSubComponentData.getProperties());
			
				jserverProxy.proxyUpdated(subComponentProxy);
			}
		}
	}
	
	/**
	 * Creates proxy objects for all the child PropertyProxy objects of this SubComponentProxy.
	 */
	void buildPropertyProxies(final RemotePropertyData[] remotePropertyDataArray)
	{
		RemotePropertyData remotePropertyData  = null;
		PropertyProxy propertyProxy = null;
		ArrayList propertiesCopy = new ArrayList(properties);
		
		boolean remove;
		
		//Remove old
		for(int i=0; i<propertiesCopy.size(); i++)
		{
			propertyProxy = (PropertyProxy)propertiesCopy.get(i);
			if(propertyProxy != null)
			{
				remove = true;
					
				for(int q=0; (remotePropertyDataArray != null) && (q<remotePropertyDataArray.length); q++)
				{
					remotePropertyData = (RemotePropertyData)remotePropertyDataArray[q];
					if( (remotePropertyData != null) && remotePropertyData.getName().equals(propertyProxy.getName()) )
					{
						remove = false;
						break;
					}
				}
					
				if(remove)
					removePropertyProxy(propertyProxy);
			}
		}
		
		boolean add;
		
		//Add new and update...
		for(int i=0; (remotePropertyDataArray != null) && (i<remotePropertyDataArray.length); i++)
		{
			remotePropertyData = (RemotePropertyData)remotePropertyDataArray[i];
			if(remotePropertyData != null)
			{
				add = true;
					
				for(int q=0; q<properties.size(); q++)
				{
					propertyProxy  = (PropertyProxy)properties.get(q);

					if( (propertyProxy != null) && remotePropertyData.getName().equals(propertyProxy.getName()) )
					{
						add = false;
						break;
					}
				}
					
				if(add)
				{
					if( remotePropertyData.getType() == RemotePropertyData.VECTOR_TYPE )
					{
						propertyProxy = new VectorPropertyProxy(jserverProxy, this, remotePropertyData);
						jserverProxy.addVectorProperty((VectorPropertyProxy)propertyProxy);
					}
					else
						propertyProxy = new PropertyProxy(jserverProxy, this, remotePropertyData);
						
					addPropertyProxy(propertyProxy);
				}
				else
					propertyProxy.update(remotePropertyData);
					
				jserverProxy.proxyUpdated(propertyProxy);
			}
		}
	}
	
	/**
	 * Adds a child SubComponentProxy object to this SubComponentProxy.
	 */
	public void addSubComponentProxy(final SubComponentProxy proxy)
	{
      if( proxy == null ) return;
      
      synchronized(subComponents)
      {
         boolean alreadyExists = false;
         SubComponentProxy existingPropertyProxy;
         
         for(int i=0; i<this.subComponents.size(); i++)
         {
            existingPropertyProxy = (SubComponentProxy)this.subComponents.get(i); 
            
            if( (existingPropertyProxy != null) && (existingPropertyProxy.getName().equals(proxy.getName())) )
            {
               alreadyExists = true;
               break;
            }
         }
         
         if( !alreadyExists )
         {
      		subComponents.add(proxy);
      		
      		if(proxy instanceof LoggerProxy) jserverProxy.addLogger((LoggerProxy)proxy);
            if(proxy.isAppenderComponent()) jserverProxy.registerAppenderComponent(proxy);
      		
      		jserverProxy.proxyAdded(proxy, this);
         }
      }
	}
	
	/**
	 * Removes a child SubComponentProxy object from this SubComponentProxy.
	 */
	public void removeSubComponentProxy(final SubComponentProxy proxy)
	{
		List p = proxy.getProperties();
		
		for(int i=0; i<p.size(); i++)
      {
			proxy.removePropertyProxy( (PropertyProxy)p.get(i) );
      }
		
		List s = proxy.getSubComponents();
		
		for(int i=0; i<s.size(); i++)
      {
			proxy.removeSubComponentProxy( (SubComponentProxy)s.get(i) );
      }
		
      synchronized(subComponents)
      {
   		subComponents.remove(proxy);
   		
   		jserverProxy.proxyRemoved(proxy);
      }
	}
   
   /**
    * Removes a child SubComponentProxy object from this SubComponentProxy.
    */
   public void removeSubComponentProxy(final String name)
   {
      synchronized(subComponents)
      {
         SubComponentProxy proxy = this.getSubComponentProxy(name);
         this.removeSubComponentProxy(proxy);
      }
   }
	
	/**
	 * Adds a child PropertyProxy object to this SubComponentProxy.
	 */
	public void addPropertyProxy(final PropertyProxy proxy)
	{
      if( proxy == null ) return;
      
      synchronized(properties)
      {
         boolean alreadyExists = false;
         PropertyProxy existingPropertyProxy;
         
   		for(int i=0; i<this.properties.size(); i++)
         {
   		   existingPropertyProxy = (PropertyProxy)this.properties.get(i); 
            
            if( (existingPropertyProxy != null) && (existingPropertyProxy.getName().equals(proxy.getName())) )
            {
               alreadyExists = true;
               break;
            }
         }
   		
         if( !alreadyExists )
         {
            properties.add(proxy);
            jserverProxy.proxyAdded(proxy, this);
         }
      }
	}
	
	/**
	 * Removes a child PropertyProxy object from this SubComponentProxy.
	 */
	public void removePropertyProxy(final PropertyProxy proxy)
	{
      synchronized(properties)
      {
   		properties.remove(proxy);
   		
   		jserverProxy.proxyRemoved(proxy);
      }
	}
   
   /**
    * Removes a child PropertyProxy object from this SubComponentProxy.
    */
   public void removePropertyProxy(final String name)
   {
      PropertyProxy proxy = this.getPropertyProxy(name);
      this.removePropertyProxy(proxy);
   }
	
	/**
	 * Gets all the child SubComponentProxy objects of this SubComponentProxy.
	 */
	public List getSubComponents()
	{
      synchronized(subComponents)
      {
         return (List)subComponents.clone();
      }
	}
	
	/**
	 * Gets all the child PropertyProxy objects of this SubComponentProxy.
	 */
	public List getProperties()
	{
      synchronized(properties)
      {
         return (List)properties.clone();
      }
	}
	
   /**
    * Engages the remote component.
    */
   public void engage()
   {
      jserverProxy.putMsg(new SubComponentControlMessage(this, SubComponentControlMessage.ENGAGE));
   }
   
   /**
    * Shuts down the remote component.
    */
   public void shutDown()
   {
      jserverProxy.putMsg(new SubComponentControlMessage(this, SubComponentControlMessage.SHUTDOWN));
   }
   
   /**
    * Reinitializes the remote component.
    */
   public void reinitialize()
   {
      jserverProxy.putMsg(new SubComponentControlMessage(this, SubComponentControlMessage.REINITIALIZE));
   }
	
	/**
	 * Sets the status value contained in this component.
	 */
	void setStatus(int status)
	{
		this.status = status;
	}
	
	/**
	 * Gets the status of the component this object represents.
	 */
	public int getStatus()
	{
		return status;	
	}
	
	/**
	 * Gets the PropertyProxy with the specified name contained in this SubComponentProxy.
	 * 
	 * @param name the name of the PropertyProxy.
	 * 
	 * @return a PropertyProxy or null if none was found.
	 */
	public PropertyProxy getPropertyProxy(String name)
	{
		List v = this.getProperties();
		
		for(int i=0; i<v.size(); i++)
		{
			PropertyProxy p = (PropertyProxy)v.get(i);
			
			if(p.getName().equals(name) || p.getFullName().equals(name))
				return p;
		}
		
		return null;
	}
	
	/**
	 * Gets the SubComponentProxy with the specified name contained in this SubComponentProxy.
	 * 
	 * @param name the name of the SubComponentProxy.
	 * 
	 * @return a SubComponentProxy or null if none was found.
	 */
	public SubComponentProxy getSubComponentProxy(String name)
	{
		List v = this.getSubComponents();
		
		for(int i=0; i<v.size(); i++)
		{
			SubComponentProxy s = (SubComponentProxy)v.get(i);
			
			if(s.getName().equals(name) || s.getFullName().equals(name))
				return s;
		}
		
		return null;
	}
}
