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
package com.teletalk.jserver.property;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.JServerUtilities;
import com.teletalk.jserver.event.EventQueue;
import com.teletalk.jserver.event.VectorPropertyEvent;
import com.teletalk.jserver.rmi.adapter.RmiAdapter;
import com.teletalk.jserver.rmi.adapter.VectorPropertyRmiAdapter;

/**
 * This class is used for presentation of a list of items that implement the interface {@link VectorPropertyItem}. The
 * actual value of this property, which is an integer, is used to indicate the size of the list. Every item in the list
 * is associated with a unique key, consisting of a <code>String</code> value. This is used to uniquely identify an
 * item.<BR>
 * <BR>
 * VectorProperty is a property class that differs a bit from the other property classes in this package. The most
 * important difference is that it is by definition unmodifiable (that is, if you don't count external operations as
 * modifications) and is strictly ment to be used for presentation of a list of items. Also, this property class doesn't
 * quite follow the pattern of it's base class {@link Property} in that it doesn't support validation or modification
 * notifications of it's value, since the value cannot be modified externally (administraiton tool) anyway.<BR>
 * <BR>
 * In addition, this class supports the existence of so called external operations. External operations makes it
 * possible to manipulate a VectorProperty externally, through for instance the administration tool. <BR>
 * <BR>
 * The primary purpose of this property is to make it possible to display the contents of large lists in the
 * administration tool. Consequently this property has no persistent state.<BR>
 * 
 * @see VectorPropertyItem
 * @see VectorPropertyOwner
 * @author Tobias Löfstrand
 * @since Beta
 */
public class VectorProperty extends Property
{

   /**
    * A specialized iterator class for VectorProperty objects.
    * 
    * @see VectorProperty
    * @author Tobias Löfstrand
    * @since Beta
    */
   public static final class VectorPropertyIterator implements Iterator
   {

      private final Object[] items;

      private int currentIndex = -1;

      private final VectorProperty vectorProperty;

      /**
       * Creates a new VectorPropertyIterator for the specified VectorProperty object.
       * 
       * @param vectorProperty the VectorProperty object for which this iterator is to be created.
       */
      public VectorPropertyIterator(VectorProperty vectorProperty)
      {
         this.vectorProperty = vectorProperty;

         items = vectorProperty.getItems();
      }

      /**
       * Creates a new VectorPropertyIterator for the specified VectorProperty object.
       * 
       * @param vectorProperty the VectorProperty object for which this iterator is to be created.
       */
      public VectorPropertyIterator(VectorProperty vectorProperty, Comparator comparator)
      {
         this.vectorProperty = vectorProperty;

         Object[] itemArray = vectorProperty.getItems();
         Arrays.sort(itemArray, comparator);

         this.items = itemArray;
      }

      /**
       * Checkes if there are more objects in this iterator.
       * 
       * @return true if there are more objects in this iterator, otherwise false.
       */
      public boolean hasNext()
      {
         return (currentIndex + 1) < items.length;
      }

      /**
       * Gets the next object in the iterator.
       * 
       * @return the next object in the iterator, null i there is none.
       */
      public Object next()
      {
         if (!hasNext())
         {
            return null;
         }
         else
         {
            currentIndex++;
            return items[currentIndex];
         }
      }

      /**
       * Gets the items in the iterator.
       * 
       * @return array containing the items.
       */
      public Object[] getItems()
      {
         return (Object[]) items.clone();
      }

      /**
       * Gets the remaining items in the iterator.
       * 
       * @return array containing the remaining items.
       */
      public Object[] getRemainingItems()
      {
         int beginIndex;
         int length;

         if (hasNext()) beginIndex = (currentIndex + 1);
         else return new Object[0];

         length = items.length - beginIndex;

         Object[] remainingItems = new Object[length];
         System.arraycopy(items, beginIndex, remainingItems, 0, length);

         return remainingItems;
      }

      /**
       * Removes the item at the current position of the iterator.
       */
      public void remove()
      {
         vectorProperty.remove((VectorPropertyItem) items[currentIndex]);
      }

      /**
       * Gets the size of the iterator.
       * 
       * @return the number of items in the iterator.
       */
      public int size()
      {
         return items.length;
      }
   }

   /** The actual vector object of this VectorProperty. */
   private final ArrayList items; // Ska man kunna ange ett godtyckligt List objekt...

   private final HashMap itemMap;

   private final HashMap externalOperations;

   private final VectorPropertyOwner vectorPropertyOwner;

   private volatile boolean vectorPropertyEventsEnabled = true;

   // private int modficationEventCounter = 0;

   private final Object lock;

   /**
    * Creates a new VectorProperty object with a VectorPropertyOwner parent.
    * 
    * @param parent the owner of this VectorProperty.
    * @param name the name of this VectorProperty.
    */
   public VectorProperty(PropertyOwner parent, String name)
   {
      this(parent, name, null, null);
   }
   
   /**
    * Creates a new VectorProperty object with a VectorPropertyOwner parent.
    * 
    * @param parent the owner of this VectorProperty.
    * @param name the name of this VectorProperty.
    * 
    * @since 2.1.2 (20060207)
    */
   public VectorProperty(PropertyOwner parent, String name, Object lock)
   {
      this(parent, name, null, lock);
   }

   /**
    * Creates a new VectorProperty object with a VectorPropertyOwner parent.
    * 
    * @param parent the owner of this VectorProperty.
    * @param name the name of this VectorProperty.
    * @param newItems a Vector of objects.
    */
   public VectorProperty(PropertyOwner parent, String name, List newItems)
   {
      this(parent, name, newItems, null);
   }

   /**
    * Creates a new VectorProperty object with a VectorPropertyOwner parent.
    * 
    * @param parent the owner of this VectorProperty.
    * @param name the name of this VectorProperty.
    * @param newItems a Vector of objects.
    * 
    * @since 2.1.2 (20060207)
    */
   public VectorProperty(PropertyOwner parent, String name, List newItems, Object lock)
   {
      super(parent, name, null);
      
      items = new ArrayList();
      itemMap = new HashMap();
      externalOperations = new HashMap();

      if( newItems != null )
      {
         for (int i = 0; i < newItems.size(); i++)
         {
            VectorPropertyItem item = (VectorPropertyItem) newItems.get(i);
            items.add(item);
            itemMap.put(item.getKey(), item);
         }
      }

      if (parent instanceof VectorPropertyOwner) this.vectorPropertyOwner = (VectorPropertyOwner) parent;
      else this.vectorPropertyOwner = null;

      if( lock == null ) this.lock = this;
      else this.lock = lock;
   }

   /**
    * Gets the object that is used for synchronization of thread access in this object.
    * 
    * @since 2.1.2 (20060207)
    */
   public Object getLock()
   {
      return lock;
   }

   /**
    * Sets the value of the flag indicating if VectorPropertyEvents should be dispatched every time an item is added,
    * modified or removed. If this flag is set to <code>false</code> only normal PropertyEvents will be dispatched,
    * which only will notify listeners of how many items are in this VectorProperty.<BR>
    * <BR>
    * The default value of this flag is <code>true</code>.
    * 
    * @param enabled the new value of the flag.
    * @see com.teletalk.jserver.event.VectorPropertyEvent
    */
   public void setVectorPropertyEventsEnabled(boolean enabled)
   {
      this.vectorPropertyEventsEnabled = enabled;
   }

   /**
    * Gets the value of the flag indicating if VectorPropertyEvents should be dispatched every time an item is added,
    * modified or removed. If this flag is set to <code>false</code> only normal PropertyEvents will be dispatched,
    * which only will notify listeners of how many items are in this VectorProperty.<BR>
    * <BR>
    * The default value of this flag is <code>true</code>.
    * 
    * @return the value of the flag.
    * @see com.teletalk.jserver.event.VectorPropertyEvent
    */
   public boolean isVectorPropertyEventsEnabled()
   {
      return vectorPropertyEventsEnabled;
   }

   // Method used to signal event when a single item is added, removed or modified
   private void dispatchModificationEvent(byte eventType, VectorPropertyItem item)
   {
      if (this.vectorPropertyEventsEnabled)
      {
         if (JServer.getJServer() != null)
         {
            EventQueue eq = JServer.getJServer().getEventQueue();
            if (eq != null)
            {
               if (item != null) eq.queueEvent(new VectorPropertyEvent(this.owner, this, eventType, item.getKey(), item.getDescription()));
               else eq.queueEvent(new VectorPropertyEvent(this.owner, this, eventType));
            }
         }
      }
      else
      // Else dispatch a normal property modification event
      {
         super.modified();
      }
   }

   private void dispatchModificationEvent(byte eventType, VectorPropertyItem[] items)
   {
      if (this.vectorPropertyEventsEnabled)
      {
         if (JServer.getJServer() != null)
         {
            EventQueue eq = JServer.getJServer().getEventQueue();
            if (eq != null)
            {
               String[] keys = new String[items.length];
               String[] descriptions = new String[items.length];

               for (int i = 0; i < items.length; i++)
               {
                  keys[i] = items[i].getKey();
                  descriptions[i] = items[i].getDescription();
               }
               eq.queueEvent(new VectorPropertyEvent(this.owner, this, eventType, keys, descriptions));
            }
         }
      }
      else
      // Else dispatch a normal property modification event
      {
         super.modified();
      }
   }

   /*
    * private void dispatchModificationEvent() { if( ((modficationEventCounter++) % 10) == 0 ) //Only dispatch
    * modification event every 10:th modification super.modified(); }
    */

   /**
    * Gets the value of this property as a String.
    * 
    * @return String representation of the value.
    */
   public String getValueAsString()
   {
      return String.valueOf(size());
   }

   /**
    * Gets the value of this BooleanProperty as an Object.
    * 
    * @return the value as an Object.
    */
   public Object getValueAsObject()
   {
      return new Integer(size());
   }

   /**
    * Returns the number of objects stored in the vector of this VectorProperty.
    * 
    * @return the size of the vector.
    */
   public final int size()
   {
      synchronized (this.getLock())
      {
         return items.size();
      }
   }

   /**
    * Not applicable.
    * 
    * @return false.
    */
   public boolean setValueAsString(String strVal)
   {
      return false;
   }

   /**
    * Not applicable.
    * 
    * @return false.
    */
   public boolean setValueAsObject(Object value)
   {
      return false;
   }

   /**
    * Gets an iterator for this VectorProperty.
    * 
    * @return a VectorPropertyIterator object.
    */
   public VectorPropertyIterator iterator()
   {
      synchronized (this.getLock())
      {
         return new VectorPropertyIterator(this);
      }
   }

   /**
    * Gets an iterator for this VectorProperty. The returned iterator will iterate through the items according to the
    * order induced by the comparator specified in parameter <code>comparator</code>.
    * 
    * @param comparator a Comparator object that will be used when determining the order of the elements in the
    *           iterator.
    * @return a VectorPropertyIterator object.
    */
   public VectorPropertyIterator iterator(Comparator comparator)
   {
      synchronized (this.getLock())
      {
         return new VectorPropertyIterator(this, comparator);
      }
   }

   /**
    * Adds an object to this VectorProperty.
    * 
    * @param item an item to be added.
    * @return <code>true</code> if the item was successfully added, otherwise <code>false</code>.
    */
   public boolean add(VectorPropertyItem item)
   {
      return doAdd(item, true);
   }

   private boolean doAdd(VectorPropertyItem item, boolean dispatchEvent)
   {
      synchronized (this.getLock())
      {
         String key = item.getKey();

         if (!itemMap.containsKey(key))
         {
            items.add(item);
            itemMap.put(item.getKey(), item);

            // dispatchModificationEvent();
            if (dispatchEvent) dispatchModificationEvent(VECTORPROPERTY_VALUE_ADDED, item);

            return true;
         }
         return false;
      }
   }

   /**
    * Adds several objects to this VectorProperty.
    * 
    * @param vItems a collection of items to be added.
    */
   public void addAll(VectorPropertyItem[] vItems)
   {
      int i;

      for (i = 0; i < vItems.length; i++)
      {
         doAdd(vItems[i], false);
      }

      if (i > 0)
      {
         dispatchModificationEvent(VECTORPROPERTY_VALUE_ADDED, vItems);
         // modified();
      }
   }

   /**
    * Fires an event (VectorPropertyEvent) indicating that the specified item was modified.
    * 
    * @param item the item to fire and event for.
    * @see com.teletalk.jserver.event.VectorPropertyEvent
    */
   public void fireItemModified(VectorPropertyItem item)
   {
      if (this.vectorPropertyEventsEnabled) // This is necessary to prevent modified() form beeing called when a item
                                             // modified event is fired when vectorPropertyEventsEnabled is false
      {
         dispatchModificationEvent(VECTORPROPERTY_VALUE_MODIFIED, item);
      }
   }

   /**
    * Replaces the item with the key specified by <code>item.getKey()</code>. If an item with the specified key is
    * not found, the new item will be added.
    * 
    * @param item the item to replace the old one with.
    * @return the item that was replaced, or<code>null </code> is none was found matching the key of the specified
    *         item.
    */
   public VectorPropertyItem set(VectorPropertyItem item)
   {
      synchronized (this.getLock())
      {
         VectorPropertyItem oldItem = (VectorPropertyItem) itemMap.get(item.getKey());

         if (oldItem != null)
         {
            int index = items.indexOf(oldItem);
            if (index >= 0)
            {
               items.set(index, item);
               itemMap.put(item.getKey(), item);

               // dispatchModificationEvent();
               dispatchModificationEvent(VECTORPROPERTY_VALUE_MODIFIED, item);
            }
            else this.add(item);
         }
         else this.add(item);

         return oldItem;
      }
   }

   /**
    * Clears this VectorProperty so that it contains no items.
    */
   public void clear()
   {
      synchronized (this.getLock())
      {
         items.clear();
         itemMap.clear();
      }
      dispatchModificationEvent(VECTORPROPERTY_CLEAR, (VectorPropertyItem) null);
      // modified();
   }

   /**
    * Removes an object from this VectorProperty.
    * 
    * @param index the index of the object to be removed.
    * @return the removed item or null if none was found.
    */
   public VectorPropertyItem remove(int index)
   {
      synchronized (this.getLock())
      {
         VectorPropertyItem item = (VectorPropertyItem) items.remove(index);

         if (item != null)
         {
            itemMap.remove(item.getKey());
            dispatchModificationEvent(VECTORPROPERTY_VALUE_REMOVED, item);
            // dispatchModificationEvent();
         }

         return item;
      }
   }

   /**
    * Removes an object from this VectorProperty.
    * 
    * @param key the key of the object to be removed.
    * @return the removed item or null if none was found.
    */
   public VectorPropertyItem remove(String key)
   {
      synchronized (this.getLock())
      {
         VectorPropertyItem item = (VectorPropertyItem) itemMap.remove(key);

         if (item != null)
         {
            items.remove(item);
            // dispatchModificationEvent();
            dispatchModificationEvent(VECTORPROPERTY_VALUE_REMOVED, item);
         }

         return item;
      }
   }

   /**
    * Removes all occurances of the object specified by parameter <code>item</code>.
    * 
    * @param item the object to be removed.
    * @return true if the object was found and removed, otherwise false.
    */
   public boolean remove(VectorPropertyItem item)
   {
      synchronized (this.getLock())
      {
         boolean removed = items.remove(item);

         if (removed)
         {
            itemMap.remove(item.getKey());
            // dispatchModificationEvent();
            dispatchModificationEvent(VECTORPROPERTY_VALUE_REMOVED, item);
         }

         return removed;
      }
   }

   /**
    * Removes the first object in this VectorProperty.
    * 
    * @return the removed item or null if the vector was empty.
    */
   public Object removeFirst()
   {
      return remove(0);
   }

   /**
    * Returns the item at the given index.
    * 
    * @param i index for an object.
    * @return the item at given index.
    */
   public Object get(int i)
   {
      synchronized (this.getLock())
      {
         return items.get(i);
      }
   }

   /**
    * Returns the item with the specified key.
    * 
    * @param key a key uniquely identifying an item in this VectorProperty.
    * @return the item with the specified key.
    */
   public Object get(String key)
   {
      synchronized (this.getLock())
      {
         return itemMap.get(key);
      }
   }

   /**
    * Returns the items matching the specified keys.
    * 
    * @param keys array of keys uniquely identifying items in this VectorProperty.
    * @return the items matching the specified keys.
    */
   public VectorPropertyItem[] get(String[] keys, Object[] arrayType)
   {
      // VectorPropertyItem[] vItems = new VectorPropertyItem[keys.length];
      ArrayList vItems = new ArrayList(keys.length);

      synchronized (this.getLock())
      {
         for (int i = 0; i < keys.length; i++)
         {
            vItems.add((VectorPropertyItem) itemMap.get(keys[i]));
         }
      }

      return (VectorPropertyItem[]) vItems.toArray(arrayType);
   }

   /**
    * Returns the items matching the specified keys.
    * 
    * @param keys array of keys uniquely identifying items in this VectorProperty.
    * @return the items matching the specified keys.
    */
   public VectorPropertyItem[] get(String[] keys)
   {
      VectorPropertyItem[] vItems = new VectorPropertyItem[keys.length];

      synchronized (this.getLock())
      {
         for (int i = 0; i < keys.length; i++)
         {
            vItems[i] = (VectorPropertyItem) itemMap.get(keys[i]);
         }
      }

      return vItems;
   }

   /**
    * Gets the items contained in this VectorProperty.
    * 
    * @return the items contained in this VectorProperty.
    */
   public VectorPropertyItem[] getItems()
   {
      synchronized (this.getLock())
      {
         return (VectorPropertyItem[]) items.toArray(new VectorPropertyItem[0]);
      }
   }
   
   /**
    * Gets the items contained in this VectorProperty.
    * 
    * @since 2.1.2 (20060224)
    */
   public ArrayList getItemsAsList()
   {
      synchronized (this.getLock())
      {
         return (ArrayList)this.items.clone();
      }
   }

   /**
    * Gets the items contained in this VectorProperty. The runtime type of the returned array is that of the specified
    * array. If the list fits in the specified array, it is returned therein. Otherwise, a new array is allocated with
    * the runtime type of the specified array and the size of this list.
    * 
    * @param array the array into which the elements of the list are to be stored, if it is big enough; otherwise, a new
    *           array of the same runtime type is allocated for this purpose.
    * @return the items contained in this VectorProperty.
    */
   public Object[] getItems(Object[] array)
   {
      synchronized (this.getLock())
      {
         return items.toArray(array);
      }
   }

   /**
    * Returns a matrix containing keys matched with string representaions of the actual objects stored in the vecor.
    * 
    * @return matrix containing keys matched with string representaions of the actual objects stored in the vecor.
    */
   public String[][] getItemsAsStrings()
   {
      // return impl.getItemsAsStrings();
      VectorPropertyItem[] vItems = getItems();
      String[][] strings = new String[vItems.length][2];
      VectorPropertyItem item;

      for (int i = 0; i < vItems.length; i++)
      {
         item = (VectorPropertyItem) vItems[i];
         if (item != null)
         {
            strings[i][0] = item.getKey();
            try
            {
               strings[i][1] = item.getDescription();
            }
            catch (Exception e)
            {
               strings[i][1] = "<Error getting description (" + e + ")!>";
            }
         }
      }

      return strings;
   }

   /**
    * Returns a matrix containing keys matched with string representaions of the actual objects stored in the vecor.
    * Only the items matching the keys specified by parameter <code>keys</code> will be returned.
    * 
    * @param keys the keys of the items to get as strings.
    * @return matrix containing keys matched with string representaions of the actual objects stored in the vecor.
    */
   public String[][] getItemsAsStrings(String[] keys)
   {
      // return impl.getItemsAsStrings(keys);
      VectorPropertyItem[] vItems = get(keys);
      String[][] strings = new String[vItems.length][2];
      VectorPropertyItem item;

      for (int i = 0; i < vItems.length; i++)
      {
         item = (VectorPropertyItem) vItems[i];
         if (item != null)
         {
            strings[i][0] = item.getKey();
            strings[i][1] = item.getDescription();
         }
      }

      return strings;
   }

   /**
    * Checks if this property is using its default value.
    * 
    * @since 2.0 Build 757
    */
   public boolean isUsingDefaultValue()
   {
      return false;
   }

   /**
    * Checks if an object with the specified key is contained in this VectorProperty.
    * 
    * @param key a key uniquely identifying an item in this VectorProperty.
    * @return true if an object with the specified key is contained in this vector, otherwise false.
    */
   public boolean containsKey(String key)
   {
      synchronized (this.getLock())
      {
         return itemMap.containsKey(key);
      }
   }

   /**
    * Checkes if the specified item is a contained in this VectorProperty.
    * 
    * @param item an item to check for.
    * @return true if the specified item is contained in this VectorProperty as determined by the equals method; false
    *         otherwise.
    */
   public boolean contains(VectorPropertyItem item)
   {
      synchronized (this.getLock())
      {
         return containsKey(item.getKey());
      }
   }

   /**
    * Checkes if this VectorProperty is empty.
    * 
    * @return true if the VectorProperty has no items, otherwise false.
    */
   public boolean isEmpty()
   {
      synchronized (this.getLock())
      {
         return items.isEmpty();
      }
   }

   /**
    * Adds an external operation to this VectorProperty.
    * 
    * @param internalName the internal name of the operation.
    * @param displayName the name of the operation used for displaying purposes.
    */
   public void addExternalOperation(String internalName, String displayName)
   {
      synchronized (externalOperations)
      {
         externalOperations.put(internalName, displayName);
      }
   }

   /**
    * Adds an external operation to this VectorProperty. The displayname will be the same as the internal name.
    * 
    * @param name the internal name of the operation.
    */
   public void addExternalOperation(String name)
   {
      synchronized (externalOperations)
      {
         externalOperations.put(name, name);
      }
   }

   /**
    * Removes an external operation from this VectorProperty.
    * 
    * @param name the internal name of the operation.
    */
   public void removeExternalOperation(String name)
   {
      synchronized (externalOperations)
      {
         externalOperations.remove(name);
      }
   }

   /**
    * Gets all external operations for this VectorProperty.
    * 
    * @return HashMap containing internal name/display name mappings of the operations.
    */
   public HashMap getExternalOperations()
   {
      synchronized (externalOperations)
      {
         try
         {
            return (HashMap) externalOperations.clone();
         }
         catch (Exception e) // Should never happen
         {
            return null;
         }
      }
   }

   /**
    * This method is called when an external operation is called on this VectorProperty object.
    * 
    * @param operationName the internal name of the operation that was called.
    * @param keys an array containing keys that was selected for the operation call.
    */
   public void externalOperationCalled(String operationName, String[] keys)
   {
      if (vectorPropertyOwner != null) vectorPropertyOwner.externalOperationCalled(operationName, keys);
   }

   /**
    * Gets the owner of this VectorProperty.
    * 
    * @return a VectorPropertyOwner object.
    */
   public VectorPropertyOwner getVectorPropertyOwner()
   {
      return vectorPropertyOwner;
   }

   /**
    * Gets the RmiAdapter associated with this VectorProperty.
    * 
    * @return a RmiAdapter (VectorPropertyRmiAdapter) object.
    * @see com.teletalk.jserver.rmi.adapter.VectorPropertyRmiAdapter
    */
   public RmiAdapter getRmiAdapter()
   {
      if (rmiAdapter == null)
      {
         try
         {
            setRmiAdapter(new VectorPropertyRmiAdapter(this));
         }
         catch (RemoteException e)
         {
            JServerUtilities.logError(getFullName(), "Unable to create VectorPropertyRmiAdapter", e);
         }
      }

      return rmiAdapter;
   }
}
