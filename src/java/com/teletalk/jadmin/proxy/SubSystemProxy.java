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

import java.util.ArrayList;
import java.util.List;

import com.teletalk.jserver.rmi.remote.RemoteSubSystem;
import com.teletalk.jserver.rmi.remote.RemoteSubSystemData;

/**
 * Proxy class for remote SubSystem objects.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.0
 */
public class SubSystemProxy extends SubComponentProxy
{
	protected ArrayList subsystems;
	
	/**
	 * Creates a new top level SubSystemProxy.
	 */
	protected SubSystemProxy(RemoteSubSystemData data)
	{
		super(data);			 
		
		subsystems = new ArrayList(); 
	}
	
	/**
	 * Creates a new SubSystemProxy.
	 */
	public SubSystemProxy(JServerProxy jserverProxy, SubSystemProxy parent, RemoteSubSystemData data)	{
		super(jserverProxy, parent, data);
		
		subsystems = new ArrayList(); 
	}
	
	/**
	 * Gets the RMI interface for the remote subsystem.
	 */
	public final RemoteSubSystem getRemoteSubSubSystem()	{		return (RemoteSubSystem)this.getRemoteObject();	}
	
	/**
	 * Creates proxy objects for all the child SubSystemProxy objects of this SubSystemProxy.
	 */
	void buildSubSystemProxies(final RemoteSubSystemData[] remoteSubSystemDataArray)
	{
		RemoteSubSystemData remoteSubSystemData;
		SubSystemProxy subSystemProxy = null;
		ArrayList subsystemsCopy = new ArrayList(subsystems);
		
		boolean remove;
		
		//Remove old
		for(int i=0; i<subsystemsCopy.size(); i++)
		{
			subSystemProxy = (SubSystemProxy)subsystemsCopy.get(i);
			if(subSystemProxy != null)
			{
				remove = true;
			
				for(int q=0; (remoteSubSystemDataArray != null) && (q<remoteSubSystemDataArray.length); q++)
				{
					remoteSubSystemData = (RemoteSubSystemData)remoteSubSystemDataArray[q];

					if( (remoteSubSystemData != null) && remoteSubSystemData.getName().equals(subSystemProxy.getName()) )
					{
						remove = false;
						break;
					}
				}
			
				if(remove)
					removeSubSystemProxy(subSystemProxy);
			}
		}
		
		boolean add;
		
		//Add new and update...
		for(int i=0; (remoteSubSystemDataArray != null) && (i<remoteSubSystemDataArray.length); i++)
		{
			remoteSubSystemData = (RemoteSubSystemData)remoteSubSystemDataArray[i];
			if(remoteSubSystemData != null)
			{
				add = true;
							
				for(int q=0; q<subsystems.size(); q++)
				{
					subSystemProxy  = (SubSystemProxy)subsystems.get(q);
					
					if( (subSystemProxy != null) && remoteSubSystemData.getName().equals(subSystemProxy.getName()) )
					{
						add = false;
						break;
					}
				}
			
				if(add)
				{
					subSystemProxy = new SubSystemProxy(jserverProxy, this, remoteSubSystemData);
					addSubSystemProxy(subSystemProxy);
				}
			
				subSystemProxy.setStatus(remoteSubSystemData.getStatus());
				subSystemProxy.buildSubSystemProxies(remoteSubSystemData.getSubSystems());
				subSystemProxy.buildSubComponentProxies(remoteSubSystemData.getSubComponents());
				subSystemProxy.buildPropertyProxies(remoteSubSystemData.getProperties());
			
				jserverProxy.proxyUpdated(subSystemProxy);
			}
		}
	}
	
	/**
	 * Adds a child SubSystemProxy object to this SubSystemProxy.
	 */
	public void addSubSystemProxy(final SubSystemProxy proxy)
	{
      if( proxy == null ) return;
      
      synchronized(subsystems)
      {
         boolean alreadyExists = false;
         SubSystemProxy existingPropertyProxy;
         
         for(int i=0; i<this.subsystems.size(); i++)
         {
            existingPropertyProxy = (SubSystemProxy)this.subsystems.get(i); 
            
            if( (existingPropertyProxy != null) && (existingPropertyProxy.getName().equals(proxy.getName())) )
            {
               alreadyExists = true;
               break;
            }
         }
         
         if( !alreadyExists )
         {
      		subsystems.add(proxy);
      		
      		jserverProxy.proxyAdded(proxy, this);
         }
      }
	}
	
	/**
	 * Removes a child SubSystemProxy object from this SubSystemProxy.
	 */
	public void removeSubSystemProxy(final SubSystemProxy proxy)
	{
		List p = proxy.getProperties();
		
		for(int i=0; i<p.size(); i++)
      {
			proxy.removePropertyProxy( (PropertyProxy)p.get(i) );
      }
		
		List sc = proxy.getSubComponents();
		
		for(int i=0; i<sc.size(); i++)
      {
			proxy.removeSubComponentProxy( (SubComponentProxy)sc.get(i) );
      }
		
		List s = proxy.getSubSystems();
		
		for(int i=0; i<s.size(); i++)
      {
			proxy.removeSubSystemProxy( (SubSystemProxy)s.get(i) );
      }
		
      synchronized(subsystems)
      {
         subsystems.remove(proxy);
		
         jserverProxy.proxyRemoved(proxy);
      }
	}
   
   /**
    * Removes a child SubSystemProxy object from this SubSystemProxy.
    */
   public void removeSubSystemProxy(final String name)
   {
      SubSystemProxy proxy = this.getSubSystemProxy(name);
      this.removeSubSystemProxy(proxy);
   }
	
	/**
	 * Gets all the child SubSystemProxy objects of this SubSystemProxy.
	 */
	public List getSubSystems()
	{
      synchronized(subsystems)
      {
         return (List)subsystems.clone();
      }
	}
		
	/**
	 * Gets the SubSystemProxy with the specified name contained in this SubSystemProxy.
	 * 
	 * @param name the name of the SubSystemProxy.
	 * 
	 * @return a SubSystemProxy or null if none was found.
	 */
	public SubSystemProxy getSubSystemProxy(String name)
	{
		List v = this.getSubSystems();
		
		for(int i=0; i<v.size(); i++)
		{
			SubSystemProxy s = (SubSystemProxy)v.get(i);
			
			if(s.getName().equals(name) || s.getFullName().equals(name))
				return s;
		}
		
		return null;
	}
}
