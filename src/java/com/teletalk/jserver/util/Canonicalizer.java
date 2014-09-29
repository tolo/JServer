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
package com.teletalk.jserver.util;

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.TreeMap;

/**
 * Utility class used for obtaining canonical instances of objects. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1 (20050919)
 */
public class Canonicalizer
{
   private static final Comparator defaultComparator = new Comparator()
   {
      public int compare(final Object o1, final Object o2)
      {
         if( (o1 instanceof Comparable) && (o2 instanceof Comparable) )
         {
            return ((Comparable)o1).compareTo(o2);
         }
         else
         {
            return System.identityHashCode(o1) - System.identityHashCode(o2);
         }
      }
   };
   
   
   private final TreeMap canonicalObjects;
   
   
   /**
    * Creates a new Canonicalizer.
    */
   public Canonicalizer()
   {
      this(null);
   }
   
   /**
    * Creates a new Canonicalizer.
    */
   public Canonicalizer(Collection canonicalObjects)
   {
      this(canonicalObjects, null);
   }
   
   /**
    * Creates a new Canonicalizer.
    */
   public Canonicalizer(Collection canonicalObjects, Comparator comparator)
   {
      if( comparator == null ) comparator = defaultComparator;
      this.canonicalObjects = new TreeMap(comparator);
      
      if( canonicalObjects != null )
      {
         Iterator it = canonicalObjects.iterator();
         Object next;
         while(it.hasNext())
         {
            next = it.next();
            if( next != null ) this.canonicalObjects.put(next, next);
         }
      }
   }
   
   
   /**
    * Clears the cached canonical objects.
    */
   public synchronized void clear()
   {
      this.canonicalObjects.clear();
   }
   
   /**
    * Gets the cached canonical objects.
    */
   public synchronized Object[] getCanonicalObjects()
   {
      return canonicalObjects.values().toArray();
   }
   
   /**
    * Gets the cached canonical objects.
    */
   public synchronized Object[] getCanonicalObjects(Object[] o)
   {
      return canonicalObjects.values().toArray(o);
   }
   
   
   /**
    * Attempts to get an already existing object that is equal to the object specified by parameter <code>object</code>. If 
    * no such object exists, the specified object will be stored in this Canonicalizer and returned back as the return value.<br>
    */
   public synchronized Object canonicalize(final Object object)
   {
      Object canonicalInstance = this.canonicalObjects.get(object);
      
      if(canonicalInstance == null)
      {
         canonicalInstance = object;
         this.canonicalObjects.put(object, object);
      }
      
      return canonicalInstance;
   }
}
