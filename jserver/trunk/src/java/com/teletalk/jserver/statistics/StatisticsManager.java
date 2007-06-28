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

/*
  TODO: Spring AOP support.
  TODO: Automatic reset?
  TODO: Generate tree
  TODO: Replace use of singleton in standalone mode
 */
package com.teletalk.jserver.statistics;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.SubComponent;

/**
 * Class implementing a runtime statistics manager.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0.2
 */
public class StatisticsManager extends SubComponent
{
   private static StatisticsManager singletonStatisticsManager = null; // ONLY used in standalone mode, i.e. no JServer main system

   
   private final CompositeStatisticsSupport compositeStatisticsSupport;
   
   
   /**
    * Singleton method for getting the StatisticsManager. This method will not create a new StatisticsManager if 
    * none exists.
    * 
    * @return the StatisticsManager or <code>null</code> if none has been created. 
    */
   public static StatisticsManager getStatisticsManager()
   {
      return getStatisticsManager(false, null);
   }
   
   /**
    * Singleton method for getting the StatisticsManager. This method will create a new StatisticsManager if 
    * none exists and the parameter <code>create</code> is <code>true</code>.
    * 
    * @param create indicating if a new StatisticsManager should be created if none exists.
    * 
    * @return the StatisticsManager or <code>null</code> if none has been created. 
    */
   public static StatisticsManager getStatisticsManager(boolean create)
   {
      return getStatisticsManager(create, null);
   }
   
   /**
    * Singleton method for getting the StatisticsManager. This method will create a new StatisticsManager if 
    * none exists and the parameter <code>create</code> is <code>true</code>.
    * 
    * @param create indicating if a new StatisticsManager should be created if none exists.
    * @param name the name of the new StatisticsManager, or null to use the default.
    * 
    * @return the StatisticsManager or <code>null</code> if none has been created. 
    */
   public static StatisticsManager getStatisticsManager(boolean create, final String name)
   {
      synchronized(StatisticsManager.class)
      {
         JServer jserver = JServer.getJServer();
         StatisticsManager statisticsManager = StatisticsManager.singletonStatisticsManager;
         if( jserver != null ) statisticsManager = jserver.getStatisticsManager();
         
         if( (statisticsManager == null) && create )
         {
            statisticsManager = new StatisticsManager(JServer.getJServer(), name);
            
            if( jserver != null ) jserver.setStatisticsManager(statisticsManager);
            else
            {
               StatisticsManager.singletonStatisticsManager = statisticsManager;
               StatisticsManager.singletonStatisticsManager.engage();
            }
         }
         
         return statisticsManager;
      }
   }
   
   /**
    * Creates a new StatisticsManager and sets it as a singleton (if one doesn't exist already). 
    * Consider using {@link #getStatisticsManager()}, {@link #getStatisticsManager(boolean)} or 
    * {@link #getStatisticsManager(boolean, String)} instead of creating a StatisticsManager directly.
    */
   public StatisticsManager(SubComponent parent)
   {
      this(parent, "StatisticsManager");
   }
   
   /**
    * Creates a new StatisticsManager and sets it as a singleton (if one doesn't exist already). 
    * Consider using {@link #getStatisticsManager()}, {@link #getStatisticsManager(boolean)} or 
    * {@link #getStatisticsManager(boolean, String)} instead of creating a StatisticsManager directly.
    */
   public StatisticsManager(SubComponent parent, String name)
   {
      super(parent, (name != null) ? name : "StatisticsManager");
                  
      this.compositeStatisticsSupport = new CompositeStatisticsSupport();
   }
   
   /**
    * Gets the root {@link StatisticsSource}.
    */
   public StatisticsSource getRootStatisticsSource()
   {
      return this.compositeStatisticsSupport;
   }
   
   /**
    * Gets the names of the {@link StatisticsSource} object contained in the root StatisticsSource object.
    */
   public String[] getStatisticsSourceNames()
   {
      return compositeStatisticsSupport.getStatisticsSourceNames();
   }
   
   /**
    * Gets the {@link StatisticsSource} object with the specified name contained in the root StatisticsSource object.
    */
   public StatisticsSource getStatisticsSource(String name)
   {
      return this.compositeStatisticsSupport.getStatisticsSource(name);
   }
   
   /**
    * Adds a {@link StatisticsSource} object to the root StatisticsSource.
    */
   public void addStatisticsSource(String name, StatisticsSource source)
   {
      this.compositeStatisticsSupport.addStatisticsSource(name, source);
   }
   
   /**
    * Removes a {@link StatisticsSource} object from the root StatisticsSource.
    */
   public StatisticsSource removeStatisticsSource(String name)
   {
      return this.compositeStatisticsSupport.removeStatisticsSource(name);
   }
   
   /**
    * Resets all the {@link StatisticsEntry} objects contained in the root StatisticsSource. This call will cascade to all the 
    * StatisticsSource objects contained in the root StatisticsSource.
    * 
    * @see StatisticsSource#reset()
    */
   public void reset()
   {
      compositeStatisticsSupport.reset();
   }
   
   /**
    * Finds the {@link StatisticsSource} or {@link StatisticsEntry} with the specified name path.
    */
   public Object find(final String[] path)
   {
      StatisticsSource parentSource = this.compositeStatisticsSupport;
      Object found = null;
            
      if( (path != null) && (path.length > 0) )
      {
         for(int i=0; (i<path.length) && (parentSource != null); i++)
         {
            found = parentSource.getStatisticsSource(path[i]);
            parentSource = (StatisticsSource)found;
            
            if( i == (path.length-1) ) // Last element
            {
               if( parentSource != null ) return parentSource; 
               else return parentSource.getStatisticsEntry(path[i]);
            }
         }
      }
      
      return null;
   }
   
   /**
    * Traverses the tree of {@link StatisticsSource} and {@link StatisticsEntry} from the root StatisticsSource 
    * and notifies the specified observer.  
    */
   public void observe(StatisticsObserver observer)
   {
      this.doObserve(observer, this.compositeStatisticsSupport, new String[0]);
   }
   
   /**
    * Internal method to traverse the tree of {@link StatisticsSource} and {@link StatisticsEntry} from the root StatisticsSource 
    * and notifies the specified observer.
    */
   protected void doObserve(final StatisticsObserver observer, final StatisticsSource parentSource, final String[] path)
   {
      String[] sourceNames = parentSource.getStatisticsSourceNames();
      String[] entryNames = parentSource.getStatisticsEntryNames();
      StatisticsSource source;
      StatisticsEntry entry;
      String[] newPath = new String[path.length + 1];
      System.arraycopy(path, 0, newPath, 0, path.length);
      
      if( sourceNames != null )
      {
         for(int i=0; i<sourceNames.length; i++)
         {
            source = parentSource.getStatisticsSource(sourceNames[i]);
            if( source != null )
            {
               newPath[newPath.length-1] = sourceNames[i];
               observer.inStatisticsSource(newPath, source);
               
               doObserve(observer, source, newPath);
            }
         }
      }
      
      if( entryNames != null )
      {
         for(int i=0; i<entryNames.length; i++)
         {
            entry = parentSource.getStatisticsEntry(entryNames[i]);
            if( entry != null )
            {
               newPath[newPath.length-1] = entryNames[i];
               observer.inStatisticsEntry(newPath, entry);
            }
         }
      }
   }
}
