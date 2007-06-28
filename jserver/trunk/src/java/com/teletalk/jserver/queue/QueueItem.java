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
package com.teletalk.jserver.queue;

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.text.SimpleDateFormat;
import java.util.Date;

import com.teletalk.jserver.comm.EndPointIdentifier;
import com.teletalk.jserver.property.VectorPropertyItem;

/**
 * This class wraps around data objects that are to be placed in to a Queue. The data objects must 
 * implement <code>QueueItemData</code>, or one of its subinterfaces.
 *  
 * @see Queue
 * @see QueueItemData
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public final class QueueItem implements Externalizable, Cloneable, VectorPropertyItem
{
	/** The serial version id of this class. */
	static final long serialVersionUID = -7483676924457524444L;
	
	private static final byte SERIAL_VERSION = 1;
	
	/**	Constant for the state QUEUED - signifying a QueueItem that has been put in the in queue. */
	public static final short QUEUED						= 0;
	
	/**	Constant for the state CHECKED_OUT - signifying a QueueItem that has been checked out from the in queue. */
	public static final short CHECKED_OUT			= 1;
	
	/**	Constant for the state DISPATCHING - signifying an outgoing QueueItem that is beeing dispatched to another queue system. */
	public static final short DISPATCHING				= 2;
	
	/**	Constant for the state DISPATCHED - signifying an outgoing QueueItem that has been dispatched to another queue system. */
	public static final short DISPATCHED				= 3;
	
	/**	Constant for the state DISPATCH_FAILED - signifying an outgoing QueueItem that failed to dispatched to another queue system. */
	public static final short DISPATCH_FAILED		= 4; 
	
	/**	Constant for the state DISPATCH_FAILED_QUEUE_FULL - signifying an outgoing QueueItem that failed to dispatched to another queue system, because the in queue of that queue system was full. */
	public static final short DISPATCH_FAILED_QUEUE_FULL		= 5; 

	/**	Constant for the state DONE_SUCCESS - signifying a QueueItem that was completed successfully. */
	public static final short DONE_SUCCESS			= 6;
	
	/**	Constant for the state DONE_FAILURE - signifying a QueueItem that was completed due to failure. */
	public static final short DONE_FAILURE			= 7;
	
	/**	Constant for the state DONE_CANCELLED - signifying a QueueItem that was completed due to cancellation. */
	public static final short DONE_CANCELLED		= 8;
	
	/**	Constant for the state DONE_CANCELLED - signifying a QueueItem that requires relocation. */
	public static final short RELOCATION_REQUIRED		= 9;
	
	private static final short MIN_STATUS = 0;
	
	private static final short MAX_STATUS = 9;
	
	/** Array containing the names for the different statuslevels. */
	public static final String[] statusNames = {	"Queued", 
																		"Checked out", 
																		"Dispatching", 
																		"Dispatched", 
																		"Dispatch failed", 
																		"Dispatch failed (queue full)", 
																		"Done (Success)", 
																		"Done (Failure)", 
																		"Done (Cancelled)", 
																		"Relocation required"};
	
	private static ThreadLocal dateFormatThreadLocal = new ThreadLocal();
	
	private QueueItemData itemData;
	private String id; 
	
	private volatile short status = QUEUED;
			
	private String parentId;
	
	/** Set by DefaultQueueSystemEndPoint. */
	private volatile EndPointIdentifier senderReceiverAddress;
	private volatile long sendReceiveTime = 0;
				
	private volatile transient int childCount = 0;
	private volatile transient int completedChildCount = 0;
	
	private volatile int dispatchCount = 0;
	
	private volatile transient int ageLimitWarningCount = 0;
	
	private volatile transient boolean recoveredFromPersistentStorage = false;
	
	private volatile Object responseData = null;
	
	private transient Queue parentQueue; //Transient reference to a parent Queue
	
	/**
	 * Default no arg constructor (used for deserialization).
	 */
	public QueueItem(){}
	
	/**
	 * Creates a new QueueItem that wraps around the specified data object.
	 * 
	 * @param itemData the data object which this QueueItem is to wrap around.
	 * @param id the unique id of this QueueItem.
	 */
	public QueueItem(QueueItemData itemData, String id)
	{
		this(itemData, id, null);
	}
	
	/**
	 * Creates a new QueueItem that wraps around the specified data object and has a parent.
	 * 
	 * @param itemData the data object which this QueueItem is to wrap around.
	 * @param id the unique id of this QueueItem.
	 * @param parentId string identifying the parent of this QueueItem.
	 */
	public QueueItem(QueueItemData itemData, String id, String parentId)
	{
		this.itemData = itemData;
		this.id = id;
		
		this.senderReceiverAddress = null;
		this.sendReceiveTime = -1;
		
		this.parentId = parentId;
	}
	
	/**
	 * Gets the id of this QueueItem.
	 * 
	 * @return the id of this QueueItem.
	 */
	public String getId()
	{
		return id;	
	}
	
	/**
	 * Gets the item contained in this QueueItem.
	 * 
	 * @return the item contained in this QueueItem.
	 */
	public QueueItemData getItemData()
	{
		return this.itemData;	
	}
	
	/**
	 * Attaches a custom (response) data object to this QueueItem.
	 * 
	 * @param responseData a custom (response) data object.
	 */
	public void setResponseData(Object responseData)
	{
		this.responseData = responseData;
	}
	
	/**
	 * Gets the custom (response) data object stored in this QueueItem.
	 * 
	 * @return the custom (response) data object stored in this QueueItem.
	 */
	public Object getResponseData()
	{
		return this.responseData;
	}
	
	/**
	 * Gets the status of this QueueItem.
	 * 
	 * @return the status of this QueueItem.
	 */
	public short getStatus()
	{
		return status;	
	}
	
	/**
	 * Sets the status of this QueueItem. If this QueueItem is currently in a Queue a call to this method will be redirected to 
	 * {@link Queue#changeStatus(QueueItem item, short newStatus)}.
	 * 
	 * @param status the new status value for this QueueItem.
	 */
	public void setStatus(short status)
	{
		if(this.status != status)
		{
			if(parentQueue != null)
			{
				parentQueue.changeStatus(this, status);
			}
			else this.status = status;
		}
	}
	
	/**
	 * Sets the status of this QueueItem. Warning! Note that the only thing this method does is to change the status 
	 * value of this object. The preferred way to change the status of a QueueItem is to call the 
	 * method {@link Queue#changeStatus(QueueItem item, short newStatus)}. Doing this makes sure 
	 * that the status change is properly reflected in the persistent state of the item and makes the Queue aware of the status change.
	 * 
	 * @param status the new status value for this QueueItem.
	 */
	public void forceStatus(short status)
	{
		this.status = status;
		if(parentQueue != null)
		{
			parentQueue.queueVector.fireItemModified(this);
		}
	}
	
	/**
	 * Gets a string identifying the parent of this QueueItem.
	 * 
	 * @return a string identifying the parent of this QueueItem, null if there is none.
	 */
	public String getParentId()
	{
		return parentId;
	}
	
	/**
	 * Sets the address which this QueueItem was received from or is to be dispatched to.
	 * 
	 * @param senderReceiverAddress a InetAddress object.
	 */
	public void setSenderReceiverAddress(EndPointIdentifier senderReceiverAddress)
	{
		if(senderReceiverAddress != null) this.senderReceiverAddress = senderReceiverAddress.getSharedInstance();
		else this.senderReceiverAddress = null;
		if(parentQueue != null)
		{
			parentQueue.queueVector.fireItemModified(this);
		}
	}
	
	/**
	 * Gets the address which this QueueItem was received from or is to be dispatched to.
	 * 
	 * @return the address which this QueueItem was received from or is to be dispatched to.
	 */
	public EndPointIdentifier getSenderReceiverAddress()
	{
		return senderReceiverAddress;	
	}
	
	/**
	 * Sets the time when this QueueItem was received or dispatched (the number of milliseconds since January 1, 1970, 00:00:00 GMT).
	 * 
	 * @param sendReceiveTime time in milliseconds. 
	 */
	public void setSendReceiveTime(long sendReceiveTime)
	{
		this.sendReceiveTime = sendReceiveTime;
		if(parentQueue != null)
		{
			parentQueue.queueVector.fireItemModified(this);
		}
	}
	
	/**
	 * Gets the time when this QueueItem was received or dispatched (the number of milliseconds since January 1, 1970, 00:00:00 GMT).
	 * 
	 * @return the time when this QueueItem was received or dispatched.
	 */
	public long getSendReceiveTime()
	{
		return sendReceiveTime;	
	}
	
	/**
	 * Increases the counter for child items with one(1).
	 */
	public void incrementChildCount()
	{
		childCount++;
	}
	
	/**
	 * Decreases the counter for child items with one(1), but only if childCount > 0.
	 */
	public void decrementChildCount()
	{
		if(childCount > 0) childCount--;
	}
	
	/**
	 * Gets the number of child items this QueueItem has.
	 * 
	 * @return the number of child items this QueueItem has.
	 */
	public int getChildCount()
	{
		return childCount;
	}
	
	/**
	 * Sets the counter for child items for this QueueItem.
	 * 
	 * @param count the new value for the child items counter.
	 */
	public void setChildCount(int count)
	{
		this.childCount = count;
	}
	
	/**
	 * Increases the counter for completed child items with one(1).
	 */
	public void incrementCompletedChildCount()	
	{
		completedChildCount++;
	}
	
	/**
	 * Decreases the counter for completed child items with one(1), but only if completedChildCount > 0.
	 */
	public void decrementCompletedChildCount()
	{
		if(completedChildCount > 0) completedChildCount--;
	}
	
	/**
	 * Gets the number of completed child items this QueueItem has.
	 * 
	 * @return the number of completed child items this QueueItem has.
	 */
	public int getCompletedChildCount()
	{
		return completedChildCount;
	}
	
	/**
	 * Sets the counter for completed child items for this QueueItem.
	 * 
	 * @param count the new value for the completed child items counter.
	 */
	public void setCompletedChildCount(int count)
	{
		this.completedChildCount = count;
	}
	
	/**
	 * Convenience method to check if all children of this QueueItem are completed.
	 * 
	 * @return true if all children are completed, otherwise false.
	 */
	public boolean areAllChildrenCompleted()
	{
		return completedChildCount == childCount;
	}
	
	/**
	 * Checks if this QueueItem is completed (DONE_SUCCESS, DONE_FAILURE or DONE_CANCELLED).
	 * 
	 * @return true is status is DONE_SUCCESS, DONE_FAILURE or DONE_CANCELLED.
	 */
	public boolean isCompleted()
	{
		return (status == DONE_SUCCESS || status == DONE_FAILURE || status == DONE_CANCELLED);
	}
	
	/**
	 * Returns a string representation of this QueueItem.
	 * 
	 * @return a string representation of this QueueItem.
	 */
	public String toString()
	{
		return getDescription();
	}
	
	/**
	 * Gets the number of times this QueueItem has been dispatched.
	 * 
	 * @return the number of time this QueueItem has been dispatched.
	 */
	public int getDispatchCount()
	{
		return dispatchCount;
	}
	
	/**
	 * Sets the counter for the number of times this QueueItem has been dispatched.
	 * 
	 * @param count the new value for the dispatch counter.
	 */
	public void setDispatchCount(int count)
	{
		this.dispatchCount = count;
		if(parentQueue != null)
		{
			parentQueue.queueVector.fireItemModified(this);
		}
	}
	
	/**
	 * Increases the counter for the number of times this QueueItem has been dispatched with one(1).
	 */
	public void incrementDispatchCount()
	{
		dispatchCount++;
	}
	
	/**
	 * Decreases the counter for the number of times this QueueItem has been dispatched with one(1), but only if dispatchCount > 0.
	 */
	public void decrementDispatchCount()
	{
		if(dispatchCount > 0) dispatchCount--;
	}
	
	/**
	 * Gets the number of times this QueueItem has received age limit warnings.
	 * 
	 * @return the number of times this QueueItem has received age limit warnings.
	 */
	public int getAgeLimitWarningCount()
	{
		return ageLimitWarningCount;
	}
	
	/**
	 * Sets the counter for the number of times this QueueItem has received age limit warnings.
	 * 
	 * @param count the new value for the age limit warning counter.
	 */
	public void setAgeLimitWarningCount(int count)
	{
		this.ageLimitWarningCount = count;
	}
	
	/**
	 * Increases the counter for the number of times this QueueItem has received age limit warnings with one(1).
	 */
	public void incrementAgeLimitWarningCount()
	{
		ageLimitWarningCount++;
	}
	
	/**
	 * Sets the value of the flag indicating if this QueueItem has been restored from persistent storage.
	 * 
	 * @param recoveredFromPersistentStorage the new value of the flag indicating if this QueueItem has been restored from persistent storage.
	 */
	public void setRecoveredFromPersistentStorage(boolean recoveredFromPersistentStorage)
	{
		this.recoveredFromPersistentStorage = recoveredFromPersistentStorage;	
	}
	
	/**
	 * Returns the value of the flag indicating if this QueueItem has been restored from persistent storage.
	 * 
	 * @return the value of the flag indicating if this QueueItem has been restored from persistent storage.
	 */
	public boolean isRecoveredFromPersistentStorage()
	{
		return this.recoveredFromPersistentStorage;	
	}
	
	/**
	 * Compares this object against the specified object. The result is true if and only if the argument is not null and is a 
	 * QueueItem object that has the same id and contains the same item as this object.
	 * 
	 * @param obj the object to compare with.
	 * 
	 * @return true if the objects are the same; false otherwise
	 */
	public boolean equals(Object obj)
	{
		if(obj instanceof QueueItem)
		{
			QueueItem q = (QueueItem)obj;
			if( q.id.equals(this.id) && q.itemData.equals(this.itemData) ) return true;
		}
			
		return false;
	}
	
	/**
	 * Fires an event (VectorPropertyEvent) indicating that this QueueItem was modified.
	 * 
	 * @see com.teletalk.jserver.event.VectorPropertyEvent
	 */
	public void fireItemModified()
	{
		if(parentQueue != null)
		{
			parentQueue.fireItemModified(this);
		}
	}
	
	/**
	 * Gets a key (the item id) uniquely identifying this QueueItem.
	 * 
	 * @return a key (the item id) uniquely identifying this QueueItem.
	 */
	public String getKey()
	{
		return id;
	}
	
	/**
	 * Gets a description of this QueueItem.
	 * 
	 * @return a description of this QueueItem.
	 */
	public String getDescription()
	{
		StringBuffer descr = new StringBuffer();
		
		descr.append("QueueItem Id: ");
		descr.append(id);
		descr.append(", Status: ");
		descr.append(statusNames[this.getStatus()]);
		
		final EndPointIdentifier srAddr = this.getSenderReceiverAddress();
		
		if(srAddr != null)
		{
			descr.append(", Address: ");
			descr.append(srAddr);
		}
		if(parentId != null)
		{
			descr.append(", Parent-id: ");
			descr.append(parentId);
		}
		
		long srTime = this.getSendReceiveTime();
			
		if(srTime > 0)
		{
			//descr += ", "
			if(dateFormatThreadLocal.get() == null)
			{
				SimpleDateFormat dateFormat = (SimpleDateFormat)SimpleDateFormat.getDateTimeInstance();
				dateFormat.applyPattern("yyyy-MM-dd HH:mm:ss");
				dateFormatThreadLocal.set(dateFormat);
			}
			
			descr.append(", ");
			descr.append( ((SimpleDateFormat)dateFormatThreadLocal.get()).format(new Date(srTime)));
		}
		
		descr.append(", Dispatch count: ");
		descr.append(this.getDispatchCount());

		return ((itemData != null) ? itemData.getDescription() : "<empty>") + " [" + descr.toString() + "]";
	}
	
	/**
	 * Performs a shallow copy of this object.
	 * 
	 * @return a new QueueItem object.
	 */
	public Object clone() throws CloneNotSupportedException
	{
		return super.clone();
	}
	
	/**
	 * Sets the queue to which this QueueItem belongs.
	 * 
	 * @param queue the queue to which this QueueItem is to belong.
	 */
	public void setQueue(Queue queue)
	{
		this.parentQueue = queue;
	}
	
	/**
	 * Gets the Queue to which this QueueItem belongs.
	 * 
	 * @return a Queue object.
	 */
	public Queue getQueue()
	{
		return this.parentQueue;
	}
	
	/**
	 * Checks that the specified status value is valid.
	 * 
	 * @param statusValue a statusvalue to validate.
	 * 
	 * @return <code>true</code> if the status value was valid, otherwise <code>false</code>.
	 */
	public static boolean validateStatusValue(final short statusValue)
	{
		return (statusValue >= MIN_STATUS) && (statusValue <= MAX_STATUS);
	}
	
	/**
	 * Concatenates the <code>toString</code> descriptions of several QueueItems.
	 * 
	 * @return the concatenated toString descriptions.
	 */
	public static String concatToString(QueueItem[] queueItems)
	{
		if( (queueItems == null) || (queueItems.length == 0) ) 
		{
			return "";
		}
		else
		{
			StringBuffer str = new StringBuffer();
				
			for(int i=0; i<queueItems.length; i++)
			{
				str.append(queueItems[i].toString());
				if(i < (queueItems.length - 1)) str.append(", ");
			}
			
			return str.toString();
		}
	}
	
	/**
	 * Concatenates the ids of several QueueItems.
	 * 
	 * @return the concatenated ids.
	 */
	public static String concatIds(String[] ids)
	{
		if( (ids == null) || (ids.length == 0) ) 
		{
			return "";
		}
		else
		{
			StringBuffer str = new StringBuffer();
				
			for(int i=0; i<ids.length; i++)
			{
				str.append(ids);
				if(i < (ids.length - 1)) str.append(", ");
			}
			
			return str.toString();
		}
	}
	
	/**
	 * Concatenates the ids of several QueueItems.
	 * 
	 * @return the concatenated ids.
	 */
	public static String concatIds(QueueItem[] queueItems)
	{
		if( (queueItems == null) || (queueItems.length == 0) ) 
		{
			return "";
		}
		else
		{
			StringBuffer str = new StringBuffer();
				
			for(int i=0; i<queueItems.length; i++)
			{
				str.append(queueItems[i].getId());
				if(i < (queueItems.length - 1)) str.append(", ");
			}
			
			return str.toString();
		}
	}
	
	public void writeExternal(ObjectOutput out) throws IOException
	{
		out.writeByte(SERIAL_VERSION); // Write QueueItem stream version
		
		out.writeObject(this.itemData); // Write item data
		out.writeUTF(this.id); // Write queue item id
		out.writeShort(this.status); // Write queue item status
		
		out.writeObject(this.parentId); // Write parent id
		out.writeObject(this.senderReceiverAddress); // Write sender/receiver address
		out.writeLong(this.sendReceiveTime); // Write send / receive time
		
		out.writeInt(this.dispatchCount); // Write dispatch count
		out.writeObject(this.responseData); // Write attached response data
	}
	
	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
	{
		in.readByte(); // Read (consume) QueueItem stream version
		
		this.itemData = (QueueItemData)in.readObject(); // Read item data
		this.id = in.readUTF(); // Read queue item id
		this.status = in.readShort(); // Read queue item status
		
		this.parentId = (String)in.readObject(); // Read parent id
		if(this.parentId != null) this.parentId = this.parentId.intern();
		this.senderReceiverAddress = ((EndPointIdentifier)in.readObject()); // Read sender / receiver address
		if(this.senderReceiverAddress != null) this.senderReceiverAddress = this.senderReceiverAddress.getSharedInstance();
		this.sendReceiveTime = in.readLong(); // Read send / receive time
		
		this.dispatchCount = in.readInt(); // Read dispatch count
		this.responseData = in.readObject(); // Read attached response data
	}
}
