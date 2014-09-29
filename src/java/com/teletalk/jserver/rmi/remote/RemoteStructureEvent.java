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

/**
 * RemoteEvent for properties.
 * 
 * @see com.teletalk.jserver.rmi.remote.RemoteEvent
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public final class RemoteStructureEvent extends RemoteEvent
{
	static final long serialVersionUID = -325984708175003344L;
	
	/** Boolean constant indicating that the target of a RemoteStructureEvent was added to the source. */
	public static final boolean ADDED = true;
	
	/** Boolean constant indicating that the target of a RemoteStructureEvent was removed from the source. */
	public static final boolean REMOVED = false;
	
	private final String sourceName;
	private final String targetName;
	private final Object remoteObjectData;
	private final boolean structureModification;
	
	/**
	 * Constructs a new RemoteStructureEvent.
	 * 
	 * @param sourceName the name of the source component that generated this event.
	 * @param targetName the name of the target component, i.e. the component that was added or removed.
	 * @param remoteObjectData a data object containing information about the target.
	 * @param structureModification boolean flag indicating if the target component was added or removed.
	 */
	public RemoteStructureEvent(String sourceName, String targetName, Object remoteObjectData, boolean structureModification)
	{
		super(sourceName);
		
		this.sourceName = sourceName;
		this.targetName = targetName;
		this.remoteObjectData = remoteObjectData;
		this.structureModification = structureModification;
	}
	
	/**
	 * Returns the name of the Property associated with this RemoteStructureEvent.
	 * 
	 * @return String representation of the name.
	 */
	public String getSourceName()
	{
		return sourceName;
	}
	
	/**
	 * Returns the name of the Property associated with this RemoteStructureEvent.
	 * 
	 * @return String representation of the name.
	 */
	public String getTargetName()
	{
		return targetName;
	}
	
	/**
	 * Gets a data object containing information about the target.
	 * 
	 * @return a data object containing information about the target.
	 */
	public Object getRemoteTargetData()
	{
		return remoteObjectData;
	}
	
	/**
	 * Gets a boolean flag indicating if the target component was added or removed.
	 * 
	 * @return a boolean flag indicating if the target component was added or removed.
	 */
	public boolean getStructureModification()
	{
		return structureModification;
	}
	
	/**
	 * Returns a String object representing of this RemoteStructureEvent.
	 * 
	 * @return a String representation of this RemotePropertyEvent.
	 */	
	public String toString()
	{
		return "RemoteStructureEvent(" + getSourceName() + ", " + getTargetName() + ", " + getStructureModification() + ")";	
	}
}
