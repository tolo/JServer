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
package com.teletalk.jserver.event;

import com.teletalk.jserver.JServerConstants;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.rmi.remote.RemoteEvent;
import com.teletalk.jserver.rmi.remote.RemoteStatusEvent;

/**
 * An eventclass for statuschanges in SubSystems and SubComponents.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta
 */
public final class StatusEvent extends Event
{
	private final int status;
	
	/**
	 * Constructs a new StatusEvent.
	 * 
	 * @param source the source of the event.
	 * @param status the new statusvalue associated with source.
	 */
	public StatusEvent(SubComponent source, int status)
	{
		super(source, true);
		this.status = status;
	}
	
	/**
	 * Gets the Property object associated with this PropertyEvent.
	 * 
	 * @return the associated Property object.
	 */
	public SubComponent getSourceComponent()
	{
		return (SubComponent)super.getSource();
	}
	
	/**
	 * Gets the statusvalue associated with the source of this event.
	 * 
	 * @return a statusvalue.
	 */
	public int getStatus()
	{
		return status;
	}
	
	public void notifyListener(final Object listener)
	{
		if(listener instanceof StatusEventListener)
			((StatusEventListener)listener).statusChanged(this);
		else super.notifyListener(listener);
	}
	
	public RemoteEvent createRemoteEvent()
	{
		return new RemoteStatusEvent(getSourceComponent().getFullName(), getStatus(), JServerConstants.statusNames[getStatus()]);
	}
}
