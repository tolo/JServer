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
package com.teletalk.jserver;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;

import com.teletalk.jserver.event.EventQueue;
import com.teletalk.jserver.event.PropertyEvent;
import com.teletalk.jserver.event.StatusEvent;
import com.teletalk.jserver.event.StructureEvent;
import com.teletalk.jserver.log.ComponentLogger;
import com.teletalk.jserver.log.ComponentLoggerFactory;
import com.teletalk.jserver.log.LogManager;
import com.teletalk.jserver.log.LoggableObject;
import com.teletalk.jserver.property.EnumProperty;
import com.teletalk.jserver.property.LogLevelProperty;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.property.PropertyManager;
import com.teletalk.jserver.property.PropertyOwner;
import com.teletalk.jserver.property.StringProperty;
import com.teletalk.jserver.rmi.adapter.RmiAdapter;
import com.teletalk.jserver.rmi.adapter.SubComponentRmiAdapter;
import com.teletalk.jserver.rmi.adapter.SubSystemRmiAdapter;

/**
 * The SubComponent class is the smallest component building block in the JServer
 * architecture. It provides a wide range of useful functions that among other
 * things allows it to interact with the rest of the system.<br>
 * <br>
 * One of the more notable things about this class is it's ability to 
 * host properties and manage them. Properties are added and removed using the {@link #addProperty(Property)} 
 * and {@link #removeProperty(Property)} methods. This class also provides functionality for reading 
 * properties from persistent storage, which is done using the {@link #initProperties()} 
 * method. If the value of the flag <code>propertiesInitialized</code> is false that metod is called when 
 * this SubComponent is enabled (or more exactly in the {@link #engage()} method).<br>
 * <br>
 * All SubComponents have a parent SubComponent and can also have zero or
 * more child SubComponents. Properties are added and removed using the {@link #addSubComponent(SubComponent)} 
 * and {@link #removeSubComponent(SubComponent)} methods.<br>
 * <br>
 * A SubComponent has a specific state at any given time. This state, or status level, may be one of the status level defined in
 * &middot; {@link JServerConstants}:<br>
 * 
 * &middot; {@link JServerConstants#ENABLED}<br>
 * &middot; {@link JServerConstants#INITIALIZING}<br>
 * &middot; {@link JServerConstants#CREATED}<br>
 * &middot; {@link JServerConstants#REINITIALIZING}<br>
 * &middot; {@link JServerConstants#SHUTTING_DOWN}<br>
 * &middot; {@link JServerConstants#DOWN}<br>
 * &middot; {@link JServerConstants#ERROR}<br>
 * &middot; {@link JServerConstants#CRITICAL_ERROR}<br>
 * &middot; {@link JServerConstants#DESTROYED}<br>
 * <br>
 * 
 * SubComponent contains a set of methods for controlling the state of the component, and for each of these methods there exists a 
 * doXXX method in which subclasses may handle the state change. The methods are as follows: 
 * The state of a component may be controlled the following methods:<br>
 * 
 * &middot; {@link #engage()} - Starts the component and makes the component enter the state {@link JServerConstants#INITIALIZING} 
 * and finally {@link JServerConstants#ENABLED}. This state change may be handled by subclasses in the method {@link #doInitialize()}.<br>
 * 
 * &middot; {@link #reinitialize()} - Triggers a restart of the component (shut down and engage), setting it in the state {@link JServerConstants#REINITIALIZING} 
 * and finally {@link JServerConstants#ENABLED}. This state change may be handled by subclasses in the methods 
 * {@link SubComponent#doShutDown()} and {@link #doInitialize()}.<br>
 * 
 * &middot; {@link #shutDown()} - Shuts down the component, setting it in the state {@link JServerConstants#SHUTTING_DOWN} 
 * and finally {@link JServerConstants#DOWN}. This state change may be handled by subclasses in the method {@link #doShutDown()}.<br>
 * 
 * &middot; {@link #error()}/{@link #error(String)} - Signals that an error has occurred in the component and sets the component in the state 
 * {@link JServerConstants#ERROR}. This state change may be handled by subclasses in the method {@link #doError()}.<br>
 * 
 * &middot; {@link #criticalError()}/{@link #criticalError(String)} - Signals that an error has occurred in the component and sets the component in the state 
 * {@link JServerConstants#ERROR}. This state change may be handled by subclasses in the method {@link #doCriticalError()}.<br>
 *  
 *  
 * @see com.teletalk.jserver.property
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public class SubComponent extends LoggableObject implements JServerConstants, PropertyOwner
{
   /** Type constant for arrays of SubComponent objects. */
   private static final SubComponent[] componentArrayType = new SubComponent[]{};
   
   /** Type constant for arrays of Property objects. */
   private static final Property[] propertyArrayType = new Property[]{};

   
   /** Name of property used for implementation class for this component. */
   public static final String IMPLEMENTATION_CLASS_NAME_PROPERTY = "implementationClass";
   
   
   /** The parent of this component. */
   protected volatile SubComponent parent;
   
   /** The name of this components. */
   protected volatile String name;
   
   /** String containing the full name of this subcomponent. This member is only used for optimization and applications should never access it directly. */
   String fullName = null;
      
   
   protected final Object statusMonitor = new Object(); // Monitor used for waiting on status changes
   
   protected final Object statusTransitionExecutionMonitor = new Object(); // Monitor used for execution of status transitions
   
   protected boolean asynchronousStatusChanges;
   
   protected Thread currentStatusTransitionExecutionThread = null;
   
   protected StatusTransitionTask currentStatusTransitionTask = null;
   
   protected volatile boolean statusTransitionInterrupted = true;
   
   protected volatile long statusTransitionBeginTime = -1;
   
   /** The maximum time a status transition is allowed to take in this subsystem. Defaultvalue is 300 seconds.*/
   protected volatile long statusTransitionTimeout = 300*1000;
   

   /** EnumProperty that holds the status. */
   protected EnumProperty status;
   
   /** */
   protected LogLevelProperty logLevel;
   
   /**    A StringProperty for holding a String describing the reason of the last error that occured in this component. */
   protected StringProperty errorReason;
   
   
   /** Flag indicating if properties have been loaded from persistent storage or not. */
   protected volatile boolean propertiesInitialized = false;
   
   
   /** ArrayList containing all properties in this component. */
   protected ArrayList properties;
   
   /** ArrayList containing all child components of this component. */
   protected ArrayList childComponents;
   
   
   /** The associated adapter for RMI communication. */
   protected RmiAdapter rmiAdapter;
   
   
   private volatile boolean destroyed = false; // TODO: Remove and replace with status
   
   /** Internal lock used by component. */
   protected final Object componentLock = new Object();
   
   
   private boolean cascadeEngageAndShutDown = false;
      
   
   protected ComponentLogger componentLogger;
      
   protected ComponentLoggerFactory componentLoggerFactory = new ComponentLoggerFactory(this);
   
   
   /**
    * Creates a new component with an anonymous name. This constructor is only provided for temporary use.
    *  
    * @since 2.1
    */
   protected SubComponent()
   {
      this(null, null, false);
   }
   
   /**
    * Creates a new component with specified name.
    * 
    * @param name the name of this component
    */
   protected SubComponent(final String name)
   {
      this(null, name, false);
   }
   
   /**
    * Creates a new component with specified parent and name.
    * 
    * @param parent the parent component
    * @param name the name of this component
    */
   protected SubComponent(final SubComponent parent, final String name)
   {
      this(parent, name, false);
   }
   
   /**
    * Creates a new component with specified parent and name.
    * 
    * @param parent the parent component
    * @param name the name of this component
    */
   protected SubComponent(final SubComponent parent, final String name, boolean asynchronousStatusChanges)
   {
      this.parent = parent;
      this.name = validateName(this.getClass().getName(), name).intern();
      this.asynchronousStatusChanges = asynchronousStatusChanges;
      
      this.childComponents = new ArrayList();
      
      this.properties = new ArrayList();
            
      this.status = new EnumProperty(this, "status", CREATED, statusNames);
      this.status.setDescription("The status of this component.");
            
      this.logLevel = new LogLevelProperty(this, "logLevel", Level.INFO, Property.MODIFIABLE_NO_RESTART);
      this.logLevel.setDescription("The log level of this component.");
      
      this.errorReason = new StringProperty(this, "errorReason", "");
      this.errorReason.setDescription("The last reson of an error in this subsystem.");
      
      addProperty(this.status);
      addProperty(this.logLevel);
   }
   
   /* ##### METHODS FROM LOGGABLEOBJECT BEGINS #####*/
   
   /**
    * Returns the log4j logger used by this component.
    * 
    * @since 2.0
    */
   public ComponentLogger getLogger()
   {
      synchronized(this.componentLock)
      {
         if( this.componentLogger == null )
         {
            if( this.parent != null )
            {
               this.componentLogger = (ComponentLogger)ComponentLogger.getLogger( this.getNormalizedFullName() , this.componentLoggerFactory);
               this.componentLogger.forceLevel( this.logLevel.getLevel() ); // indexToLevel(this.logLevel.intValue()) );
            }
         }
         
         if( this.componentLogger != null ) return this.componentLogger;
      }
      
      return ComponentLogger.getTopSystemLogger();
   }
   
   
   /* ##### METHODS FROM LOGGABLEOBJECT ENDS #####*/
   
   
   /* ##### MISC UTILITY METHODS BEGINS #####*/
   
   /**
    * Registers this component as a dynamic component. 
    * 
    * @since 2.1 (20050825)
    */
   public void setDynamic(final String implementationClass)
   {
      this.getPropertyManager().addImplementationClassProperty(this.name, implementationClass);
   }
      
   /**
    * Validates a name that is to be given to a component.
    * 
    * @param componentClassName the class of the component.
    * @param requestedName the requested name.
    * 
    * @return the validated name.
    */
   public static String validateName(final String componentClassName, final String requestedName)
   {
      String newName = requestedName;
      
      if( (newName == null) || (newName.equals("")) )
      {
         if( componentClassName == null ) newName = "Component" + Long.toHexString((long)(System.currentTimeMillis()*Math.random() + System.nanoTime()));
         else if(componentClassName.lastIndexOf(".") > 0) newName = componentClassName.substring(componentClassName.lastIndexOf(".")+1);
         else newName = componentClassName;
      }
      else
      {
         newName = newName.trim();
         boolean invalidName = false;
         
         if(newName.length() > 100)
         {
            newName = newName.substring(0, 100);
            invalidName = true;
         }
         
         char[] illegalChars = {'.', '\r', '\n', '\t', '\f'};
         
         for(int i=0; i<illegalChars.length; i++)
         {
            if(newName.indexOf(illegalChars[i]) >= 0)
            {
               newName = newName.replace(illegalChars[i], '_');
               invalidName = true;
            }
         }
         
         if( invalidName )
         {
            JServerUtilities.logWarning(newName, "Name validation failed (requested name contained illegal characters)! Requested name: " + requestedName + ", new name: " + newName + ".");
         }
      }
      
      return newName;
   }
   
   /**
    * Marks this component as removed.
    */
   protected final void remove()
   {
      synchronized(componentLock)
      {
         parent = null;
      }
   }
   
   /**
    * Checks if this component is removed.
    * 
    * @return true if this component is removed, otherwise false.
    */
   public boolean hasParent()
   {
      synchronized(componentLock)
      {
         return this.parent != null;
      }
   }
   
   /**
    * Dispatches an event to the EventQueue of this server. This method may be used to queue both 
    * {@link com.teletalk.jserver.event.Event} objects and <code>java.lang.Runnable</code> objects.
    * 
    * @param event an event to be dispatched.
    * 
    * @return <code>true</code> if the event was successfully queued, otherwise <code>false</code>.
    */
   public final boolean fireGlobalEvent(final Object event)
   {
      EventQueue eventQueue = this.getEventQueue();
      if(eventQueue != null)
      {
         return eventQueue.queueEvent(event);
      }
      return false;
   }
   
   /**
    * Gets the flag indicating if status changes are to be executed asynchronously (true) or synchronously (false).
    * 
    * @since 2.0
    */
   protected boolean isAsynchronousStatusChanges()
   {
      return this.asynchronousStatusChanges;
   }
   
   /**
    * Sets the flag indicating if status changes are to be executed asynchronously (true) or synchronously (false).
    * 
    * @since 2.0
    */
   protected void setAsynchronousStatusChanges(boolean asynchronousStatusChanges)
   {
      this.asynchronousStatusChanges = asynchronousStatusChanges;
   }
   
   /**
    * Sets the error reason of this component.
    */
   protected void setErrorReason(final String errorReason)
   {
      if( (errorReason != null) && (errorReason.trim().length() > 0) )
      {
         if( !this.hasProperty(this.errorReason) ) this.addProperty(this.errorReason);
         this.errorReason.setValue(errorReason);
      }
      else 
      {
         if( this.hasProperty(this.errorReason) ) this.removeProperty(this.errorReason);
         this.errorReason.setValue("");
      }
   }
   
   /**
    * Gets the flag indicating if engage and shut down operation should cascade to child components.
    * 
    * @since 2.1 (20050427)
    */
   public boolean isCascadeEngageAndShutDown()
   {
      return cascadeEngageAndShutDown;
   }
   
   /**
    * Sets the flag indicating if engage and shut down operation should cascade to child components.
    * 
    * @since 2.1 (20050427)
    */
   public void setCascadeEngageAndShutDown(boolean cascadeEngageAndShutDown)
   {
      this.cascadeEngageAndShutDown = cascadeEngageAndShutDown;
   }
   
   /* ##### MISC UTILITY METHODS ENDS #####*/
   
   
   /* ##### GETTER METHODS BEGINS #####*/
      
   /**
    * Returns the name of this component.
    * 
    * @return a String containing the name.
    */
   public final String getName()
   {
      return name;
   }
   
   /**
    * Returns the normalized full name of this component. The full name consists of
    * the result of calling the getFullName() method in the parent component, a "."
    * and finally the name of this component. The result will then be {@link JServer#normalizeFullName(String) normalized}. For instance:
    * <code>JServer.MySystem.MyComponent</code>
    * 
    * @return a String containing the full name.
    * 
    * @since 2.0
    */
   public final String getNormalizedFullName()
   {
      JServer jServer = JServer.getJServer();
      if( jServer != null ) return jServer.normalizeFullName(this.getFullName());
      else return this.getFullName();
   }
   
   /**
    * Returns the full name of this component. The full name consists of
    * the result of calling the getFullName() method in the parent component, a "."
    * and finally the name of this component. For instance:
    * <code>MyServer.MySystem.MyComponent</code>
    * 
    * @return a String containing the full name.
    */
   public final String getFullName()
   {
      final String parentFullName;
      if(parent != null) parentFullName = parent.getFullName();
      else parentFullName = "";

      synchronized(componentLock)
      {
         if(this.fullName == null)
         {
            if( (parentFullName == null) || (parentFullName.trim().length() == 0) ) this.fullName = getName();
            else this.fullName = parentFullName + "." + getName();
            
            this.fullName = this.fullName.intern();
         }
         
         return this.fullName;
      }
   }
   
   /**
    * Gets the parent of this component.
    * 
    * @return a component object.
    */
   public final SubComponent getParent()
   {
      return parent;
   }
   
   /**
    * Returns a string representation of this component, by
    * calling the getFullName() method.
    * 
    * @return a String representation of this component.
    * 
    * @see #getFullName()
    */
   public String toString()
   {
      return getFullName();
   }
   
   /**
    * Gets the threadgroup which most threads created in this
    * subcomponent should be added to. This method only asks
    * its parent for the threadgroup. The topsystem should
    * override this method to return the actual threadgroup object.
    * 
    * @return a ThreadGroup object, null if this component has no parent.
    */
   public ThreadGroup getThreadGroup()
   {
      if(parent != null) return parent.getThreadGroup(); 
      else if(JServer.getJServer() != null) return JServer.getJServer().getThreadGroup();
      else return null; 
   }
   
   /**
    * Returns the PropertyManager from the parent. This method can be overridden by subclasses to retreive a 
    * reference to a PropertyManager in another way.
    * 
    * @return a PropertyManager object, null if this component has no parent.
    * 
    * @see com.teletalk.jserver.property.PropertyManager
    */
   public PropertyManager getPropertyManager()
   {
      if(parent != null) return parent.getPropertyManager();
      else if(JServer.getJServer() != null) return JServer.getJServer().getPropertyManager();      
      else return null;
   }
   
   /**
    * Returns the EventQueue from the parent. This method can be overridden by subclasses to retreive a 
    * reference to a EventQueue in another way.
    * 
    * @return a EventQueue object, null if this component has no parent.
    * 
    * @see com.teletalk.jserver.event.EventQueue
    */
   public EventQueue getEventQueue()
   {
      if(parent != null) return parent.getEventQueue();
      else if(JServer.getJServer() != null) return JServer.getJServer().getEventQueue();
      else return null;
   }
   
   /**
    * Returns the LogManager from the parent. This method can be overridden by subclasses to retreive a 
    * reference to a LogManager in another way.
    * 
    * @return a LogManager object, null if this component has no parent.
    * 
    * @see com.teletalk.jserver.log.LogManager
    */
   public LogManager getLogManager()
   {
      if(parent != null) return parent.getLogManager();
      else if(JServer.getJServer() != null) return JServer.getJServer().getLogManager();
      else return null;
   }
   
   /* ##### GETTER METHODS ENDS #####*/
   
   
   /* ##### CHILD COMPONENT METHODS BEGINS #####*/
   
   /**
    * Adds a child component to this component. If a component with the same name already exists, it 
    * will be removed and replaced.<br>
    * <br>
    * This method will assume ownership of the specified child component. If a parent already exists it will be replaced 
    * by <code>this</code> and the target component will be removed from the original parent.<br>
    * <br> 
    * <i><b>Note:</b> since version 2.0 both SubComponents and SubSystems are supported by this method.</i>
    * 
    * @param childComponent the child component to be added.
    */
   public final void addSubComponent(SubComponent childComponent)
   {
      this.addSubComponent(childComponent, null, false);
   }
   
   /**
    * Adds a child component to this component. If a component with the same name already exists, it 
    * will be removed and replaced.<br>
    * <br>
    * This method will assume ownership of the specified child component. If a parent already exists it will be replaced 
    * by <code>this</code> and the target component will be removed from the original parent.<br>
    * <br> 
    * <i><b>Note:</b> since version 2.0 both SubComponents and SubSystems are supported by this method.</i>
    * 
    * @param childComponent the child component to be added.
    * @param engage boolean value indicating if the component should be engaged.
    */
   public final void addSubComponent(SubComponent childComponent, boolean engage)
   {
      this.addSubComponent(childComponent, null, engage);
   }
   
   /**
    * Adds a child component to this component. If a component with the same name already exists, it 
    * will be removed and replaced.<br>
    * <br>
    * This method will assume ownership of the component and rename it. If a parent already exists it will be replaced 
    * by <code>this</code> and the target component will be removed from the original parent.<br>
    * <br> 
    * <i><b>Note:</b> since version 2.0 both SubComponents and SubSystems are supported by this method.</i>
    *  
    * @param childComponent the child component to be added.
    * @param newName a new name for the component that is to be added.
    */
   public final void addSubComponent(SubComponent childComponent, String newName)
   {
      this.addSubComponent(childComponent, newName, false);
   }
   
   /**
    * Adds a child component to this component. If a component with the same name already exists, it 
    * will be removed and replaced.<br>
    * <br>
    * This method will assume ownership of the component and rename it. If a parent already exists it will be replaced 
    * by <code>this</code> and the target component will be removed from the original parent.<br>
    * <br> 
    * <i><b>Note:</b> since version 2.0 both SubComponents and SubSystems are supported by this method.</i>
    *  
    * @param childComponent the child component to be added.
    * @param newName a new name for the component that is to be added.
    * @param engage boolean value indicating if the component should be engaged.
    */
   public final void addSubComponent(final SubComponent childComponent, final String newName, final boolean engage)
   {
      addSubComponent(childComponent, newName, engage, false);
   }
   
   /**
    * Adds a child component to this component. 
    */
   private final void addSubComponent(final SubComponent childComponent, final String newName, final boolean engage, final boolean silent)
   {
      if( (childComponent == null) || (childComponent == this) )
      {
         logWarning("Cannot add null component or component to itself!");
         return;
      }
      
      //if(this.isDebugMode()) logDebug("Adding component '" + childComponent.getName() + "'.");
      logInfo("Adding component '" + childComponent.getName() + "'.");
            
      SubComponent it;
      SubComponent found = null;
      boolean alreadyExists = false;
      String oldName = null;
      boolean componentRenamed = false;
      boolean newParent = false;
         
      synchronized(this.childComponents)
      {
         // Check if this subcomponent already contains a subcomponent with the same name 
         // as the one that we are trying to add...
         for(int i=0; i<childComponents.size(); i++)
         {
            it = (SubComponent)childComponents.get(i);
                  
            if(it.getName().equals(childComponent.getName()))
            {
               found = it;
               break;
            }
         }
         
         // If found one with same name...
         if( (found != null) && (found != childComponent) )
         {
            logWarning("Warning while trying to add component " + childComponent + ": Found component with same name! Replacing!");
            removeSubComponent(found, true); // Remove it
            found = null;
         }
         else if( (found != null) && (found == childComponent) ) // Already added
         {
            alreadyExists = true;
            logWarning("Warning while trying to add component " + childComponent + ": component already added!");
         }
      
         if( !alreadyExists ) childComponents.add(childComponent);
      }

      // Assume ownership of subcomponent
      synchronized(childComponent.componentLock)
      {
         // If the subcomponent already belongs to another subcomponent than this...
         if( (childComponent.parent != null) && (childComponent.parent != this) )
         {
            // ...remove it from that subcomponent...
            childComponent.parent.removeSubComponent(childComponent);
         }
         
         newParent = childComponent.parent != this; // Check if the component is added to a new parent (for instance if it was created without parent or moved to a new parent).
         
         childComponent.parent = this;
         //childComponent.removed = false;
         if(newName != null)
         {
            oldName = childComponent.getName();
            childComponent.name = validateName(this.getClass().getName(), newName);
            
            if( !newName.equals(oldName) ) componentRenamed = true; // Renamed
         }
         
         // Reset fullname, propertiesInitialized etc in subcomponent - but only if the componet is added to a new parent or renamed
         if( newParent || componentRenamed )
         {
            // Reset propertiesInitialized flag!
            childComponent.propertiesInitialized = false;
            // Reset full name
            childComponent.fullName = null;
            
            if( childComponent.componentLogger != null )
            {
               childComponent.componentLogger.removeAllAppenders();
               childComponent.componentLogger = null;
            }
         }
      }
      
      // Reset fullname and propertiesInitialized in all children of target - but only if the componet is added to a new parent or renamed
      if( newParent || componentRenamed )
      {
         List targetChildren = childComponent.getSubComponentTree();
         SubComponent child;
         if( targetChildren != null )
         {
            for(int i=0; i<targetChildren.size(); i++)
            {
               child = (SubComponent)targetChildren.get(i);
               if( child != null )
               {
                  synchronized(child.componentLock)
                  {
                     child.propertiesInitialized = false;
                     child.fullName = null;
                     if( child.isEnabled() || child.isInitializing() )
                     {
                        child.shutDown(); // Stop (and start) child to make it aware of new hierarchy 
                        if( this.cascadeEngageAndShutDown )
                        {
                           // If cascadeEngageAndShutDown is true - restart child automatically. Otherwise, the 
                           // responsability of this is left to the implementation of doInitialize() in the class of childComponent.
                           try{ child.waitForDown(2500); }catch(InterruptedException ie){}
                           child.engage();
                        }
                     }
                  }
               }
            }
         }
      }
      
      if( componentRenamed )
      {
         logInfo("Added component renamed from " + oldName + " to " + newName + ".");
      }
            
      if( alreadyExists && componentRenamed ) // If the component already exists, but has been renamed...
      {
         removeSubComponent(childComponent); // ...remove it
      }
      
      if( !alreadyExists || componentRenamed )
      {
         fireGlobalEvent(new StructureEvent(this, childComponent, StructureEvent.ADDED));

         if( childComponent.isEnabled() || childComponent.isInitializing() ) // If already started...
         {
            // Restart to make the component aware of the new owner
            childComponent.shutDown();
            childComponent.engage(); // ...A simple reinitialize call won't initialize the properties
         }
         else if( engage && !childComponent.isEnabled() && !childComponent.isInitializing() )  childComponent.engage();
         
         this.subComponentAdded(childComponent);
      }
   }
   
   /**
    * Renames this component.
    * 
    * @param newName a new name for the component that is to be added.
    * 
    * @since 2.1 (20050427)
    */
   public final void rename(final String newName)
   {
      rename(newName, false);
   }
   
   /**
    * Renames this component.
    * 
    * @param newName a new name for the component that is to be added.
    * @param restart flag indicating if the component should be restarted after it has been renamed.
    * 
    * @since 2.1 (20050427)
    */
   public final void rename(final String newName, final boolean restart)
   {
      if( this.parent != null )
      {
         SubComponent parent = this.parent;
         parent.removeSubComponent(this, restart, true);
         parent.addSubComponent(this, newName, restart, true);
      }
      else
      {
         String oldName = null;
         boolean isJServer = (JServer.getJServer() == this);
            
         synchronized(this.componentLock)
         {
            if(newName != null)
            {
               oldName = this.getName();
               this.name = validateName(this.getClass().getName(), newName);
            }
            
            // Reset propertiesInitialized flag! (We don't have to reset propertiesInitialized if rename is called on the JServer object...)
            if( !isJServer ) this.propertiesInitialized = false;
            // Reset full name
            this.fullName = null;
            
            if( this.componentLogger != null )
            {
               this.componentLogger.removeAllAppenders();
               this.componentLogger = null;
            }
         }
         
         // Reset fullname and propertiesInitialized in all children of target
         List children = this.getSubComponentTree();
         SubComponent child;
         if( children != null )
         {
            for(int i=0; i<children.size(); i++)
            {
               child = (SubComponent)children.get(i);
               if( child != null )
               {
                  synchronized(child.componentLock)
                  {
                     // We don't have to reset propertiesInitialized if rename is called on the JServer object...
                     if( !isJServer ) child.propertiesInitialized = false;
                     child.fullName = null;
                  }
               }
            }
         }
         
         if( (newName != null) && !(newName.equals(oldName)) )
         {
            if( !isJServer ) logInfo("Component renamed from " + oldName + " to " + newName + ".");
         }
         
         if( restart )
         {
            this.shutDown();
            this.engage(); // A simple reinitialize call won't initialize the properties
         }
      }
   }
   
   /**
    * Adds the specified list of child components to this component.<br>
    * <br> 
    * <i><b>Note:</b> since version 2.0 both SubComponents and SubSystems are supported by this method.</i>
    * 
    * @param componentList a List containing the child components to be added.
    */
   public final void addSubComponents(final List componentList)
   {
      SubComponent sc;
                                    
      for(int i=0; i<componentList.size(); i++)
      {
         sc = (SubComponent)componentList.get(i);
            
         addSubComponent(sc);
      }
   }
   
   /**
    * Removes a child component from this component.<br>
    * <br> 
    * <i><b>Note:</b> since version 2.0 both SubComponents and SubSystems are supported by this method.</i>
    * 
    * @param component the child component to be removed.
    */
   public final boolean removeSubComponent(SubComponent component)
   {
      return removeSubComponent(component, false);
   }
   
   /**
    * Removes a child component from this component.<br>
    * <br> 
    * <i><b>Note:</b> since version 2.0 both SubComponents and SubSystems are supported by this method.</i>
    * 
    * @param component the child component to be removed.
    */
   public final boolean removeSubComponent(final SubComponent component, final boolean shutDown)
   {
      return removeSubComponent(component, shutDown, false);
   }
   
   /**
    * Removes a child component from this component.<br>
    */
   public final boolean removeSubComponent(final SubComponent component, final boolean shutDown, final boolean silent)
   {
      if( component == null ) return false;
      
      boolean componentRemoved = false;
      
      synchronized(this.childComponents)
      {
         if( component.parent != this )
         {
            if(!silent) logWarning("Unable to remove component '" + component.getName() + "'! It is not a child of this component.");
         }
         else if(this.childComponents.contains(component))
         {
            if( !silent && this.isDebugMode() ) logDebug("Removing component '" + component.getName() + "'.");
            this.childComponents.remove(component);
            component.remove();
            
            componentRemoved = true;
         }
      }
      
      if( componentRemoved )
      {
         this.subComponentRemoved(component);
         fireGlobalEvent(new StructureEvent(this, component, StructureEvent.REMOVED));
         
         if( shutDown && (component.getStatus() != DOWN) && (component.getStatus() != SHUTTING_DOWN) && (component.getStatus() != CREATED) ) component.shutDown();
      }
      
      return componentRemoved;
   }
   
   /**
    * Removes the specified list of child components from this component.<br>
    * <br> 
    * <i><b>Note:</b> since version 2.0 both SubComponents and SubSystems are supported by this method.</i>
    * 
    * @param subComponentList a List containing the child components to be removed.
    */
   public final void removeSubComponents(final List subComponentList)
   {
      for(int i=0; i<subComponentList.size(); i++)
      {
         this.removeSubComponent((SubComponent)subComponentList.get(i));
      }
   }
   
   /**
    * Removes all child components from this component.<br>
    * <br> 
    * <i><b>Note:</b> since version 2.0 both SubComponents and SubSystems are supported by this method.</i>
    * 
    * @since 2.0 Build 757
    */
   public final void removeSubComponents()
   {
      for(int i=0; i<this.childComponents.size(); i++)
      {
         this.removeSubComponent((SubComponent)this.childComponents.get(i));
      }
   }
   
   /**
    * Called when a new child SubComponent or SubSystem is added to this component. This 
    * method is provided as a convenience for subclasses that wish to be notified when a child component is 
    * added. The subComponentAdded method is provided as a simpler alternative to registering a 
    * {@link com.teletalk.jserver.event.StructureEventListener}. This method is especially useful when dynamic 
    * components are used.
    * 
    * @param component the added component.
    * 
    * @since 2.0 Build 755
    */
   protected void subComponentAdded(final SubComponent component)
   {
   }
   
   /**
    * Called when a child SubComponent or SubSystem is removed from this component. This 
    * method is provided as a convenience for subclasses that wish to be notified when a child component is 
    * removed. The subComponentRemoved method is provided as a simpler alternative to registering a 
    * {@link com.teletalk.jserver.event.StructureEventListener}. 
    * 
    * @param component the removed component.
    * 
    * @since 2.0 Build 755
    */
   protected void subComponentRemoved(final SubComponent component)
   {
   }
      
   /**
    * Checks if this component owns (is the parent of) the component specifieid by parameter <code>component.</code><br>
    * <br> 
    * <i><b>Note:</b> since version 2.0 both SubComponents and SubSystems are supported by this method.</i>
    * 
    * @return <code>true</code> if this component owns the specified SubCompoent, otherwise <code>false</code>.
    */
   public final boolean hasSubComponent(final SubComponent component)
   {
      synchronized(childComponents)
      {
         return childComponents.contains(component);
      }
   }
      
   /**
    * Returns the child components of this component.<br>
    * <br> 
    * <i><b>Note:</b> since version 2.0 both SubComponents and SubSystems are returned by this method.</i>
    * 
    * @return a Vector containing the child Components.
    */
   public final List getSubComponents()
   {
      synchronized(childComponents)
      {
         return (List)childComponents.clone();
      }
   }
   
   /**
    * Gets the component with the specified name contained in this component.<br>
    * <br> 
    * <i><b>Note:</b> since version 2.0 both SubComponents and SubSystems are returned by this method.</i>
    * 
    * @param name the name of the component.
    * 
    * @return a component or null if none was found.
    */
   public final SubComponent getSubComponent(final String name)
   {
      SubComponent[] sArray;
      
      synchronized(childComponents)
      {
         sArray = (SubComponent[])childComponents.toArray(SubComponent.componentArrayType);
      }
            
      for(int i=0; i<sArray.length; i++)
      {
         if(sArray[i].getName().equals(name) || sArray[i].getFullName().equals(name)) return sArray[i];
      }
      
      return null;
   }
   
   /**
    * Returns the tree of all components (SubSystems and SubComponents) under this component as a list.
    * 
    * @return a List containing Components.
    */
   public List getSubComponentTree()
   {
      ArrayList recursively = new ArrayList();
      
      SubComponent[] childArray;
      
      synchronized(childComponents)
      {
         childArray = (SubComponent[])childComponents.toArray(SubComponent.componentArrayType);
      }
      
      for(int i=0; i<childArray.length; i++)
      {
         if(childArray[i] != null)
         {
            recursively.add(childArray[i]);
            recursively.addAll(childArray[i].getSubComponentTree());
         }
      }
      
      return recursively;
   }
   
   /* ##### CHILD COMPONENT METHODS ENDS #####*/
   
   
   /* ##### PROPERTY METHODS BEGINS #####*/
   
   /**
    * Adds a property to this component. If a Property with the same name already exists, it will 
    * be removed and replaced.<br>
    * <br>
    * This method will assume ownership of the Property. If a parent already exists it will be 
    * replaced by <code>this</code> and the target Property will be removed from the original parent.
    * 
    * @param property the Property to be added.
    */
   public final void addProperty(final Property property)
   {
      this.addProperty(property, null);
   }
   
   /**
    * Adds a property to this component. If a Property with the same name already exists, it will 
    * be removed and replaced.<br>
    * <br>
    * This method will assume ownership of the Property and rename it. If a parent already exists it will be 
    * replaced by <code>this</code> and the target Property will be removed from the original parent.
    * 
    * @param property the Property to be added.
    * @param newName a new name for the target Property.
    */
   public final void addProperty(final Property property, final String newName)
   {
      if(property == null) return;
      
      if(this.isDebugMode()) logDebug("Adding property '" + property.getName() + "'.");
            
      Property it;
      Property found = null;
      boolean alreadyExists = false;
      //boolean propertyAdded = false;
      
      synchronized(properties)
      {
         for(int i=0; i<properties.size(); i++)
         {
            it = (Property)properties.get(i);
                  
            if(it.getName().equals(property.getName()))
            {
               found = it;
               break;
            }
         }

         if(found != null)
         {
            if(found != property)
            {
               logWarning("Warning while trying to add property '" + property.getName() + "': Found property with same name! Removing!");
               this.removeProperty(found);
               found = null;
            }
            else
            {
               alreadyExists = true;
               logWarning("Warning while trying to add property '" + property.getName() + "': property already added!");
            }
         }
         
         if(!alreadyExists)
         {
            properties.add(property);
            //propertyAdded = true;
         }
      }
      
      // Assume ownership of property
      synchronized(property)
      {
         PropertyOwner owner = property.getPropertyOwner();
         if((owner != null) && (owner instanceof SubComponent) && (owner != this) )//!alreadyExists)
         {
            ((SubComponent)owner).removeProperty(property);
         }
      
         if(newName == null)
         {
            property.ownerShipAssumed(this);
         }
         else
         {
            property.ownerShipAssumed(this, newName);
         }
      }
      
      if( !alreadyExists ) fireGlobalEvent(new StructureEvent(this, property, StructureEvent.ADDED));
   }
   
   /**
    * Removes a property from this component.
    * 
    * @param property the Property to be removed.
    */
   public final void removeProperty(final Property property)
   {
      if(property == null) return;
      
      boolean propertyRemoved = false;
      
      synchronized(properties)
      {
         if(properties.contains(property))
         {
            if(this.isDebugMode()) logDebug("Removing property '" + property.getName() + "'.");
            properties.remove(property);
            property.remove();
            propertyRemoved = true;
         }
         else if(this.isDebugMode()) logDebug("Removing property '" + property.getName() + "' not a child.");
      }
      
      if( propertyRemoved ) fireGlobalEvent(new StructureEvent(this, property, StructureEvent.REMOVED));
   }
   
   /**
    * Removes a list of properties from this component.
    * 
    * @param propertiesList the properties to be removed.
    * 
    * @since 2.0 Build 757
    */
   public final void removeProperties(final List propertiesList)
   {
      if( propertiesList == null ) return;
      
      for(int i=0; i<propertiesList.size(); i++)
      {
         this.removeProperty((Property)propertiesList.get(i));
      }
   }
   
   /**
    * Removes all properties from this component.
    * 
    * @since 2.0 Build 757
    */
   public final void removeProperties()
   {
      for(int i=0; i<this.properties.size(); i++)
      {
         this.removeProperty((Property)this.properties.get(i));
      }
   }
   
   /**
    * Checks if this component owns (is the parent of) the Property specifieid by parameter <code>property.</code>
    * 
    * @return <code>true</code> if this component owns the specified Property, otherwise <code>false</code>.
    */
   public final boolean hasProperty(final Property property)
   {
      synchronized(properties)
      {
         return properties.contains(property);
      }
   }
   
   /**
    * Returns the properties contained in this component.
    * 
    * @return a List containing the properties.
    */
   public final List getProperties()
   {
      synchronized(properties)
      {
         return (List)properties.clone();
      }
   }
   
   /**
    * Returns the persistet properties contained in this component.
    * 
    * @return a Vector containing the properties.
    */
   public final List getPersistentProperties()
   {
      ArrayList persistentProperties = new ArrayList();
      Property[] pArray;
      
      synchronized(properties)
      {
         pArray = (Property[])properties.toArray(SubComponent.propertyArrayType);
      }
      
      for(int i=0; i<pArray.length; i++)
      {
         if(pArray[i].isPersistent()) persistentProperties.add(pArray[i]);
      }
      
      return persistentProperties;
   }
   
   /**
    * Gets the property with the specified name contained in this component.
    * 
    * @param name the name of the Property.
    * 
    * @return a Property or null if none was found.
    */
   public final Property getProperty(String name)
   {
      Property[] pArray;
      
      synchronized(properties)
      {
         pArray = (Property[])properties.toArray(SubComponent.propertyArrayType);
      }
      
      for(int i=0; i<pArray.length; i++)
      {
         if(pArray[i].getName().equals(name) || pArray[i].getFullName().equals(name))
         {
            return pArray[i];
         }
      }
      
      return null;
   }
   
   /* ##### PROPERTY METHODS ENDS #####*/
   
   
   /* ##### PROPERTY RELATED METHODS BEGINS #####*/
   
   /**
    * Gets an object representing the property with the specified name found in the configuration file. 
    * Note, that this method only returns an object if the property was actually present in the configuration 
    * file when starting this server, otherwise null is returned.
    *  
    * @param name the name of the persistent property.
    * 
    * @return a Property object or null if none was found.
    * 
    * @since 2.0.1 (20041215)
    */
   public final Property getConfiguredProperty(final String name)
   {
      final HashMap persistentProperties = this.getPropertiesFromPersistentStorage();
      Property persistentProperty = null;
      
      if( persistentProperties != null )
      {
         persistentProperty = (Property)persistentProperties.get(name);
      }
      
      return persistentProperty;
   }
   
   /**
    * Initializes a property with the data found in the configuration file for a property with the name specified by 
    * parameter <code>configuredPropertyName</code>, which may for instance be an alternate name of the property.
    * This method will overwrite any previously initialized value of the specified property.
    *  
    * @param property the property to initialize.
    * @param configuredPropertyName the name of the cofigured property to be used as the source.
    * 
    * @return <code>true</code> if the property was initialized, otherwise <code>false</code>.
    * 
    * @since 2.0.1 (20041215)
    */
   public final boolean initFromConfiguredProperty(final Property property, final String configuredPropertyName)
   {
      return initFromConfiguredProperty(property, configuredPropertyName, true);
   }
   
   /**
    * Initializes a property with the data found in the configuration file for a property with the name specified by 
    * parameter <code>configuredPropertyName</code>, which may for instance be an alternate name of the property. 
    *  
    * @param property the property to initialize.
    * @param configuredPropertyName the name of the cofigured property to be used as the source.
    * @param overwrite flag indicating if the value of the property should be overwritten even if it has already been initialized.
    * 
    * @return <code>true</code> if the property was initialized, otherwise <code>false</code>.
    * 
    * @since 2.0.1 (20041215)
    */
   public final boolean initFromConfiguredProperty(final Property property, final String configuredPropertyName, final boolean overwrite)
   {
      return initFromConfiguredProperty(property, configuredPropertyName, true, false);
   }
   
   /**
    * Initializes a property with the data found in the configuration file for a property with the name specified by 
    * parameter <code>configuredPropertyName</code>, which may for instance be an alternate name of the property.<br>
    * <br>
    * This method will call the method {@link #propertyInitialized(Property)} after the property has been initialized. 
    *  
    * @param property the property to initialize.
    * @param configuredPropertyName the name of the cofigured property to be used as the source.
    * @param overwrite flag indicating if the value of the property should be overwritten even if it has already been initialized.
    * @param removeOld flag indicating if the property named by configuredPropertyName should be removed from the configuration file. 
    * 
    * @return <code>true</code> if the property was initialized, otherwise <code>false</code>.
    * 
    * @since 2.0.1 (20041215)
    */
   public final boolean initFromConfiguredProperty(final Property property, final String configuredPropertyName, final boolean overwrite, final boolean removeOld)
   {
      Property configuredProperty = this.getConfiguredProperty(configuredPropertyName);
      
      if( configuredProperty != null )
      {
         if( !property.isInitialized() || overwrite )
         {
            property.setNotificationMode(false);
            property.initProperty(configuredProperty);
            property.setNotificationMode(true);
            
            this.propertyInitialized(property);
            
            if( removeOld )
            {
               // Remove the old property
               if( this.getPropertyManager() != null ) this.getPropertyManager().removePersistentProperty(configuredProperty, this.getFullName());
            }
                           
            return true;
         } 
      }
      return false;
   }   
   
   /**
    * Gets the properties loaded from persistent storage (PropertyManager) 
    * associated with this component.
    * 
    * @return a HashMap containing names of properties mapped with Property objects.
    * 
    * @see com.teletalk.jserver.property.PropertyManager
    */
   public final HashMap getPropertiesFromPersistentStorage()
   {
      PropertyManager pm = getPropertyManager();
      
      if(pm != null)
      {
         final Map persistentProperties = pm.getLoadedPropertiesAsMap(this.getFullName());
         if(persistentProperties != null)
         {
            if(persistentProperties instanceof HashMap) return (HashMap)persistentProperties;
            else return new HashMap(persistentProperties);
         }
      }
      return new HashMap();
   }
   
   /**
    * Initializes the properties in this component with their persistent counterparts, if any, that are restored 
    * from persistent storage by the PropertyManager. This method will call {@link Property#initProperty(Property)} for all 
    * the properties contained in this component.<br>
    * <br>
    * A call to this method will not result in calls to the {@link #propertyModified(Property property)} method, 
    * however the method {@link #validatePropertyModification(Property property)}  
    * will be called to make it possible to validate the values read from persistent storage. Also, the method 
    * {@link #propertyInitialized(Property)} is called to make it possible for subclasses to receive a notification 
    * <br><br>
    * By default, this method is called when initializing a component for the first time.
    * 
    * @see #doInitialize()
    * @see #propertiesInitialized
    * @see com.teletalk.jserver.property.PropertyManager
    */
   public final void initProperties()
   {
      if(isDebugMode()) logDebug("Initializing properties");

      HashMap persistentProperties = getPropertiesFromPersistentStorage();
      Property subcomponentProperty = null;
      Property persistentProperty = null;
                  
      synchronized(properties)
      {
         // Make sure that old debug mode property is initialized, if present in ...
         ArrayList propertiesCopy = (ArrayList)properties.clone();
         
         boolean debugMode = isDebugMode();
         
         for(int i=0; i<propertiesCopy.size(); i++)
         {
            subcomponentProperty = (Property)propertiesCopy.get(i);
               
            persistentProperty = null;
            
            if(persistentProperties != null)
            {
               persistentProperty = (Property)persistentProperties.get(subcomponentProperty.getName());
            }
            
            if(persistentProperty == null)
            {
               if( debugMode ) logDebug("Skipping initializing of property " + subcomponentProperty.getName() + "! No persistent counterpart found!");
            }
            else
            {
               if( debugMode ) logDebug("Initializing property " + subcomponentProperty.getName() + " with value '" + persistentProperty.getValueAsString() + "'.");

               subcomponentProperty.setNotificationMode(false);
               subcomponentProperty.initProperty(persistentProperty);
               subcomponentProperty.setNotificationMode(true);
               
               this.propertyInitialized(subcomponentProperty);
            }
         }
         
         // Attempt to get old property "log level" to be used if "logLevel" property is missing
         this.initFromConfiguredProperty(this.logLevel, "log level", false, true);
      
         propertiesInitialized = true;
      }
      
      if(isDebugMode()) logDebug("Done initializing properties");
   }
   
   /**
    * Initializes a single property with its persistent counterparts from the PropertyManager.<br>
    * <br>
    * This method is provided for implementations where properties are added after a component 
    * has been initialized, i.e. after the {@link #initProperties()} method has been called.<br>
    * <br>
    * A call to this method will not result in calls to the {@link #propertyModified(Property property)} method, 
    * however the method {@link #validatePropertyModification(Property property)}  
    * will be called to make it possible to validate the values read from persistent storage. Also, the method 
    * {@link #propertyInitialized(Property)} is called to make it possible for subclasses to receive a notification 
    * 
    * @param property the Property to initialize with a value from persistent property.
    */
   public final void initializeProperty(final Property property)
   {
      if(isDebugMode()) logDebug("Initializing propertiy " + property.getName());

      HashMap persistentProperties = getPropertiesFromPersistentStorage();
      Property persistentProperty = null;
      
      if(persistentProperties != null)
      {
         persistentProperty = (Property)persistentProperties.get(property.getName());
      }
            
      if(persistentProperty == null)
      {
         if(isDebugMode()) logDebug("Skipping initializing of property " + property.getName() + "! No persistent counterpart found!");
      }
      else
      {
         if(isDebugMode()) logDebug("Initializing property " + property.getName() + " with value '" + persistentProperty.getValueAsString() + "'.");
         property.setNotificationMode(false);
         property.initProperty(persistentProperty);
         property.setNotificationMode(true);
               
         this.propertyInitialized(property);
      }
   }
   
   /**
    * Checks whether or not properties are initialized from persistend storage.
    * 
    * @return the value of the propertiesInitialized flag.
    */
   public final boolean propertiesInitializedFromPersistentStorage()
   {
      return propertiesInitialized;
   }
   
   /**
    * Sets the flag indicating whether or not properties are initialized from persistend storage.
    * 
    * @since 2.1.1 (20060112)
    */
   public final void setPropertiesInitializedFromPersistentStorage(boolean propertiesInitialized)
   {
      this.propertiesInitialized = propertiesInitialized;;
   }
   
   /**
    * Called when the value of a property has been modified. Subclasses can override this
    * method to provide more specialized behaviour. This implementation fires propertyevents
    * (and statusevents) when a property is modified and handles property invoked restarts. <br>
    * <br>
    * It is strongly recommended that subclasses that overload this method call the superclass implementation<br>
    * <br>
    * <i>Note:</i> This method is only invoked when a property is modified during runtime. To intercept the initialization of a property, 
    * override the method {@link #propertyInitialized(Property)}.
    * 
    * @param property the Property that was changed.
    * 
    * @see PropertyOwner
    * @see PropertyEvent
    * @see StatusEvent
    */
   public void propertyModified(final Property property)
   {
      if( property == this.logLevel )
      {
         this.getLogger().forceLevel( this.logLevel.getLevel() );
      }
      
      if(property == status)
      {
         fireGlobalEvent(new StatusEvent(this, this.getStatus()));
      }
      
      boolean isPropertyInitializing = property.isInitializing();
      
      fireGlobalEvent(new PropertyEvent(this, property, isPropertyInitializing));
      
      if(!isPropertyInitializing) // If this metod was not called due to an initialization of a property's value
      {
         final int restartRequirementMode = property.getModificationMode();
      
         if(restartRequirementMode == Property.MODIFIABLE_OWNER_RESTART)
         {
            propertyModifiedRestartNeeded();
         }
         else if(restartRequirementMode == Property.MODIFIABLE_SERVER_RESTART)
         {
            JServer topSystem = JServer.getJServer();
            if(topSystem != null)
            {
               topSystem.restartJServer();
            }
         }
      }
   }
   
   /**
    * Called when a property has been initialized with a value from persistent storage. Subclasses can override this
    * method to provide more specialized behaviour. This implementation only fires a property event.<br>
    * <br>
    * It is strongly recommended that subclasses that overload this method call the superclass implementation<br>
    * <br>
    * 
    * @param property the Property that was initialized.
    * 
    * @see #initProperties()
    */
   public void propertyInitialized(Property property)
   {
      if( property == this.logLevel )
      {
         this.getLogger().forceLevel( this.logLevel.getLevel() );
      }
      
      fireGlobalEvent(new PropertyEvent(this, property, true));
   }
   
   /**
    * Called to check if a property can be modified. Subclasses can override this
    * method to provide more specialized behaviour. This implementation always returns
    * true.
    * 
    * @param property The property to be checked.
    * 
    * @return true if the property can be modified, otherwise false.
    *
    * @see PropertyOwner
    */
   public boolean propertyModificationAllowed(final Property property)
   {
      return true;
   }
   
   /**
    * Validates a modification of a property's value. The property specified by parameter <code>property</code> will 
    * be given the value to be validated before this method is called, and will revert to the old one if this method returns <code>false</code>.
    * <br><br>
    * Subclasses can override this method to provide more specialized behaviour. This implementation always returns
    * <code>true</code>.
    * 
    * @param property The property to be validated.
    * 
    * @return boolean value indicating if the property passed (true) validation or not (false).
    * 
    * @see PropertyOwner
    */
   public boolean validatePropertyModification(Property property)
   {
      return true;
   }
   
   /**
    * Called when a propertymodification requires a restart of the owner. This implementation restarts 
    * the component by disabling and then enabling it.
    */
   protected void propertyModifiedRestartNeeded()
   {
      if(isEnabled())
      {
         reinitialize();
      }
   }
   
   /* ##### PROPERTY RELATED METHODS ENDS #####*/
   
   
   /* ##### LOG LEVEL METHODS BEGINS #####*/

   
   /**
    * Checks if log events with debug level can be logged in this component.<br>
    * <br>
    * Note: Calling this method has the same effect as calling {@link LoggableObject#isDebugEnabled()}.
    * 
    * @return <code>true</code> if log events with debug level can be logged in this component, otherwise <code>false</code>.
    * 
    * @see LoggableObject#isDebugEnabled()
    */
   public final boolean isDebugMode()
   {
      return super.isDebugEnabled();  
   }
   
   /**
    * Checks if a log message with the specified log level currently can be logged in this component. This method will return 
    * <code>true</code> if parameter level is greater or equal to the current log level of this component.<br>
    * <br>
    * Note: Calling this method has the same effect as calling {@link #canLog(Level) canLog(Level.toLevel(level))}.
    * 
    * @param level the log level. Note that the value of this parameter is interpreted as a Log4J log level integer value.
    * 
    * @return <code>true</code> if a log message with the the specified level currently can be logged in this component, otherwise <code>false</code>.
    * 
    * @since 1.3
    */
   public boolean canLog(final int level) 
   {
      return this.canLog(Level.toLevel(level));
   }
   
   /**
    * Checks if a log message with the specified log level currently can be logged in this component. This method will return 
    * <code>true</code> if parameter level is greater or equal to the current log level of this component.<br>
    * <br>
    * Note: Calling this method has the same effect as calling {@link LoggableObject#isLogLevelEnabled(Level)}.
    * 
    * @param level the log level.
    * 
    * @return <code>true</code> if a log message with the the specified level currently can be logged in this component, otherwise <code>false</code>.
    * 
    * @since 2.0
    * 
    * @see LoggableObject#isLogLevelEnabled(Level)
    */
   public boolean canLog(final Level level) 
   {
      return super.isLogLevelEnabled(level);
   }
   
   /**
    * Gets the current log level of this component. The value is one of the Level constants defined in the Log4J class Level.<br>
    * <br>
    * Note: This method overrides the super class implementation only for binary compatability reasons.
    * 
    * @return the log level as a Level object.
    * 
    * @since 2.0
    */
   public final Level getLevel()
   {
      return super.getLevel();
   }
   
   /**
    * Gets the current log level of this component. The value is one of the integer constants defined in the Log4J class Level/Priority
    * 
    * @return the log level.
    * 
    * @since 1.3
    */
   public final int getLogLevel()
   {
      return this.logLevel.getLevelInt();
   }
   
   /**
    * Sets the current log level of this component.
    * 
    * @param level
    * 
    * @since 2.0
    */
   public final void setLogLevel(final Level level)
   {
      this.logLevel.setValue( level );
   }
   
   /**
    * Sets the current log level of this component. The value must be one of the integer constants defined in the Log4J class Level/Priority.
    * 
    * @param level the log level. Note that the value of this parameter is interpreted as a Log4J log level integer value.
    * 
    * @since 1.3
    */
   public final void setLogLevel(int level)
   {
      this.setLogLevel(Level.toLevel(level));
   }
   
   /* ##### LOG LEVEL METHODS ENDS #####*/
   

   /* ##### RMI METHODS BEGINS #####*/   
   
   /**
    * Sets the RmiAdapter for this component.
    * 
    * @param rmiAdapter a RmiAdapter object.
    */
   public void setRmiAdapter(RmiAdapter rmiAdapter)
   {
      synchronized(this.componentLock)
      {
         if( (this instanceof SubSystem) && !(rmiAdapter instanceof SubSystemRmiAdapter) ) 
         {
            throw new RuntimeException("Unable to set rmi adapter! Specified adapter object is not an instance of SubSystemRmiAdapter");
         }
         if( (this instanceof SubComponent) && !(rmiAdapter instanceof SubComponentRmiAdapter) ) 
         {
            throw new RuntimeException("Unable to set rmi adapter! Specified adapter object is not an instance of SubComponentRmiAdapter");
         }
         else
         {
            this.rmiAdapter = rmiAdapter;
         }
      }
   }

   /**
    * Gets the RmiAdapter associated with this component.
    * 
    * @return a RmiAdapter (SubComponentRmiAdapter) object.
    * 
    * @see com.teletalk.jserver.rmi.adapter.SubComponentRmiAdapter
    */
   public RmiAdapter getRmiAdapter()
   {
      synchronized(this.componentLock)
      {
         if(rmiAdapter == null)
         {
            try
            {
               if(this instanceof SubSystem )
               {
                  setRmiAdapter(new SubSystemRmiAdapter((SubSystem)this));
               }
               else
               {
                  setRmiAdapter(new SubComponentRmiAdapter(this));
               }
            }
            catch(RemoteException e)
            {
               logError("Unable to create RmiAdapter", e);
            }
         }
               
         return rmiAdapter;
      }
   }
   
   /* ##### RMI METHODS ENDS #####*/
   
   
   /* ##### STATUS RELATED METHODS BEGINS #####*/
   
   /**
    * Returns the status of this component.
    * 
    * @return int indicating the status.
    */
   public final int getStatus()
   {
      return status.getIndex();
   }
   
   /**
    * Internal method to set the current status and log. <b><i>Note: </i></b> subclasses should never calls this method directly under normal 
    * cicrumstances. Use the one of the status change methods such as {@link #engage()} or {@link #shutDown()} instead.   
    */
   protected void setStatus(final int newStatus)
   {
      status.setValue(newStatus);
      synchronized( this.statusMonitor )
      {
         // Notify on status object (to wake up threads in waitForStatus etc)
         this.statusMonitor.notifyAll();
      }
       
      logInfo(statusNames[newStatus]);
   }
   
   /**
    * Returns the status name for the current status of this component.
    * 
    * @return a string representation of the current status.
    */
   public final String getStatusName()
   {
      return statusNames[status.getIndex()];
   }
   
   /**
    * Indicates whether or not this component is enabled (running).
    * 
    * @return boolean value inidcating if the status of this component is ENABLED.
    */
   public final boolean isEnabled()
   {
      return this.getStatus() == ENABLED;
   }
   
   /**
    * Indicates whether or not this component is enabled (running).
    * 
    * @return boolean value inidcating if the status of this component is INITIALIZING.
    * 
    * @since 2.1 (20050524)
    */
   public final boolean isInitializing()
   {
      return this.getStatus() == INITIALIZING;
   }   
   
   /**
    * Method to check if this component is currently reinitializing.
    * 
    * @return true if this component is currently reinitializing, otherwise false.
    */
   public final boolean isReinitializing()
   {
      return this.getStatus() == REINITIALIZING;
   }
   
   /**
    * Makes the calling thread wait until this component has changed status to
    * DOWN or the given maxWait time has ellapsed. This method will not break on error (ERROR or CRITICAL_ERROR).
    * 
    * @param maxWait the maximum wait time in milliseconds.
    * 
    * @return true if the specified status was reached, otherwise false.
    */
   public final boolean waitForDown(long maxWait) throws InterruptedException
   {
      return waitForStatus(DOWN, maxWait, false);
   }
   
   /**
    * Makes the calling thread wait until this component has changed status to
    * DOWN or the given maxWait time has ellapsed.
    * 
    * @param maxWait the maximum wait time in milliseconds.
    * @param breakOnError flag indicating if this method should return if this component goes into ERROR or CRITICAL_ERROR.
    * 
    * @return true if the specified status was reached, otherwise false.
    */
   public final boolean waitForDown(long maxWait, boolean breakOnError) throws InterruptedException
   {
      return waitForStatus(DOWN, maxWait, breakOnError);
   }
   
   /**
    * Makes the calling thread wait until this component has changed status to
    * ENABLED or the given maxWait time has ellapsed. This method will not break on error (ERROR or CRITICAL_ERROR).
    * 
    * @param maxWait the maximum wait time in milliseconds.
    * 
    * @return true if the specified status was reached, otherwise false.
    */
   public final boolean waitForEnabled(long maxWait) throws InterruptedException
   {
      return waitForStatus(ENABLED, maxWait, false);
   }
   
   /**
    * Makes the calling thread wait until this component has changed status to
    * ENABLED or the given maxWait time has ellapsed.
    * 
    * @param maxWait the maximum wait time in milliseconds.
    * @param breakOnError flag indicating if this method should return if this component goes into ERROR or CRITICAL_ERROR.
    * 
    * @return true if the specified status was reached, otherwise false.
    */
   public final boolean waitForEnabled(long maxWait, boolean breakOnError) throws InterruptedException
   {
      return waitForStatus(ENABLED, maxWait, breakOnError);
   }

   /**
    * Makes the calling thread wait until this component has changed status to
    * the specified status or the given maxWait time has ellapsed. This method will not break on error (ERROR or CRITICAL_ERROR).
    * 
    * @param waitStatus the status to wait for.
    * @param maxWait the maximum wait time in milliseconds.
    * 
    * @return true if the specified status was reached, otherwise false.
    */
   public final boolean waitForStatus(int waitStatus, long maxWait) throws InterruptedException
   {
      return this.waitForStatus(waitStatus, maxWait, false);
   }
   
   /**
    * Makes the calling thread wait until this component has changed status to
    * the specified status or the given maxWait time has ellapsed.
    * 
    * @param waitStatus the status to wait for.
    * @param maxWait the maximum wait time in milliseconds.
    * @param breakOnError flag indicating if this method should return if this component goes into ERROR or CRITICAL_ERROR.
    * 
    * @return true if the specified status was reached, otherwise false.
    */
   public final boolean waitForStatus(int waitStatus, long maxWait, boolean breakOnError) throws InterruptedException
   {
      if( (this.getStatus() != waitStatus) && (waitStatus >= 0) && (waitStatus < statusNames.length) )
      {
         synchronized(this.statusTransitionExecutionMonitor)
         {
            if( this.currentStatusTransitionExecutionThread == Thread.currentThread() )
            {
               logWarning("Current thread is already changing status! Cannot wait for status " + statusNames[waitStatus] + "!");
               return false;
            }
         }
         
         long waitStart = System.currentTimeMillis();
         long waitTime;
         
         while( (getStatus() != waitStatus) && (!breakOnError || (breakOnError && (getStatus() != ERROR) && (getStatus() != CRITICAL_ERROR) )) )
         {
            waitTime = maxWait - (System.currentTimeMillis() - waitStart);
                     
            if(waitTime > 0)
            {
               synchronized( this.statusMonitor )
               {
                  this.statusMonitor.wait(waitTime);
               }
            }
            else break;
         }
      }
      else
      {
         return true;
      }

      return getStatus() == waitStatus;
   }
   
   /**
    * Gets the maximum state transition time.
    * 
    * @since 2.0
    */
   protected long getMaximumStatusTransitionTime()
   {
      return this.statusTransitionTimeout;
   }
   
   /**
    * Checks the status of this component. Returns false if the component is in an invalid or erroneous 
    * state, otherwise true.
    * 
    * @return this implementation always returns true.
    */
   protected boolean checkStatus()
   {
      boolean result = true;
      boolean isOkAndEnabled = false; // This flag is used so the doCheck method can be called without owning the componentLock.
      
      synchronized(componentLock)
      {
         long lastStatusChange = this.statusTransitionBeginTime;
               
         if(lastStatusChange > 0)  //Check if a status transition has taken an abnormally long time
         {
            long abnormallyLongTime = this.getMaximumStatusTransitionTime();
            
            if( (System.currentTimeMillis() - lastStatusChange) > abnormallyLongTime )
            {
               int statusTransitionType = this.currentStatusTransitionTask.getStatusTransitionType();
                                                                        
               if( statusTransitionType == STATUS_TRANSITION_TYPE_SHUT_DOWN )
               {
                  setStatus(DOWN);
               }
               else if( statusTransitionType == STATUS_TRANSITION_TYPE_ERROR )
               {
                  setStatus(ERROR);                  
               }
               else if( statusTransitionType == STATUS_TRANSITION_TYPE_CRITICAL_ERROR ) 
               {
                  setStatus(CRITICAL_ERROR);
               }
               else
               {
                  result = false;
               }
               
               logError("Error while executing status transition '" + JServerConstants.statusTransitionTypeNames[statusTransitionType] + "' - status transition took to long time! Current status is " + statusNames[this.getStatus()] + ".");
            }
         }
      
         if(result)
         {
            if(getStatus() == ENABLED)
            {
               isOkAndEnabled = true; // This flag is used so the doCheck method can be called without owning the componentLock.
            }
         }
      }
      
      if( isOkAndEnabled ) // This flag is used so the doCheck method can be called without owning the componentLock.
      {
         // Perform user defined check
         result = doCheck();
      }
      
      return result;
   }
   
   /**
    * Method called when the JServer performs a status check on this component. This method is provided exclusively for 
    * subclasses that may benifit from periodic checks. If the check was successful the return value of this method should be 
    * <code>true</code>, otherwise <code>false</code>. This implementation simply returns <code>true</code>.<br>
    * <br>
    * Note that this method only will be called if the status of the component is ENABLED.
    * 
    * @return <code>true</code> if the check was successful, otherwise <code>false</code>.
    */
   protected boolean doCheck()
   {
      return true;
   }
      
   /**
    * Checks if a state transition is valid.
    * 
    * @since 2.0
    */
   private boolean checkStatusTransition(final int statusTransitionType)
   {
      final int oldStatus = getStatus();
      boolean success = false;
      
      if(statusTransitionType == STATUS_TRANSITION_TYPE_ENGAGE)
      {
         success =     ((oldStatus == CREATED) || 
                                 (oldStatus == ERROR) || 
                                 (oldStatus == CRITICAL_ERROR) || 
                                 (oldStatus == DOWN));
      }
      else if(statusTransitionType == STATUS_TRANSITION_TYPE_REINITIALIZE)
      {
         success = oldStatus != REINITIALIZING;
      }
      else if(statusTransitionType == STATUS_TRANSITION_TYPE_SHUT_DOWN)
      {
         success = oldStatus != SHUTTING_DOWN && oldStatus != DOWN;
      }
      else if(statusTransitionType == STATUS_TRANSITION_TYPE_ERROR)
      {
         success = oldStatus != ERROR;
      }
      else if(statusTransitionType == STATUS_TRANSITION_TYPE_CRITICAL_ERROR)
      {
         success = oldStatus != CRITICAL_ERROR;
      }
            
      return success;
   }
   
   /**
    * Initializes a stauts transition.
    * 
    * @since 2.0
    */
   protected final boolean initiateStatusTransition(final StatusTransitionTask statusTransitionTask)
   {
      boolean success = false;
      int statusTransitionType = statusTransitionTask.getStatusTransitionType();

      if( this.checkStatusTransition(statusTransitionType) ) // Preliminary check...(a check will also be performed in executeStateTransition)
      {
         this.doInitiateStatusTransition(statusTransitionTask);
         
         boolean eventQueued = false;
         
         if( asynchronousStatusChanges ) // Attempt to queue status transition task in event queue when performing asynchronous status changes 
         { 
            EventQueue eventQueue = this.getEventQueue();
            // Check if event queue is available (and make sure that the event queue doesn't queue status change events to itself!)
            if( (eventQueue != null) && (this != eventQueue) && eventQueue.isEnabled() && !JServer.isServerShuttingDown() )
            {
               eventQueued = eventQueue.queueEvent(statusTransitionTask);
               success = eventQueued;
            }
         }
         
         if( !eventQueued )
         {
            if( asynchronousStatusChanges )
            {
               if( super.isDebugEnabled() ) logDebug("Event queue isn't available - executing status transition (" + statusTransitionTask + ") in separate thread.");
               
               // Fallback if event queue isn't available: execute in a separate thread 
               final Thread fireAndForget = new Thread("StatusTransitionTask(" + this.getFullName() + ")")
               {
                  public void run()
                  {        
                     statusTransitionTask.execute();
                  }
               };
   
               fireAndForget.setDaemon(true);
               fireAndForget.start();
               success = true;
            }
            else // Sequential status change
            {
               success = statusTransitionTask.execute();
            }
         }
      }
      
      return success;
   }
   
   /**
    * Method for subclass handling of status transition initialization.
    * 
    * @since 2.0
    */
   protected void doInitiateStatusTransition(final StatusTransitionTask statusTransitionTask)
   {
      int statusTransitionType = statusTransitionTask.getStatusTransitionType();
      
      if( (statusTransitionType == JServerConstants.STATUS_TRANSITION_TYPE_ERROR) || 
            (statusTransitionType == JServerConstants.STATUS_TRANSITION_TYPE_CRITICAL_ERROR) )
      {
         synchronized(this.statusTransitionExecutionMonitor)
         {
            if( this.currentStatusTransitionExecutionThread == Thread.currentThread() )
            {
               throw new StatusTransitionException();
            }
         }
      }
   }
   
   /**
    * Executes a status transition.
    * 
    * @since 2.0
    */
   protected final boolean executeStatusTransition(final StatusTransitionTask statusTransitionTask)
   {
      int statusTransitionType = statusTransitionTask.getStatusTransitionType();
      boolean interrupt = statusTransitionTask.isInterrupt();
   
      try
      {
         synchronized(this.statusTransitionExecutionMonitor)
         {
            if( this.currentStatusTransitionExecutionThread == Thread.currentThread() )
            {
               logWarning("Current thread already changing status! Status change aborted!", new Throwable());
               return false;
            }
            else
            {
               if( interrupt && (this.currentStatusTransitionExecutionThread != null) )
               {
                  this.statusTransitionInterrupted = true;
                  this.currentStatusTransitionExecutionThread.interrupt();
               }
               // Wait for current status change to finish
               while(this.currentStatusTransitionExecutionThread != null) this.statusTransitionExecutionMonitor.wait();
               
               this.statusTransitionInterrupted = false;
               this.currentStatusTransitionExecutionThread = Thread.currentThread();
               this.currentStatusTransitionTask = statusTransitionTask;
               this.statusTransitionBeginTime = System.currentTimeMillis();
            }
         }
         
         if( this.checkStatusTransition(statusTransitionType) )
         {
            return doExecuteStatusTransition(statusTransitionTask);
         }
         else
         {
            if( isDebugMode() ) logDebug("Unable to execute status transition '" + JServerConstants.statusTransitionTypeNames[statusTransitionType] + "' while in status " + statusNames[getStatus()] + "!");
            return false;
         }
      }
      catch(Throwable t)
      {
         if( destroyed ) return false;
         
         String errorStr = "Error (" + t + ") while performing status change '" + JServerConstants.statusTransitionTypeNames[statusTransitionType] + "'!";

         this.setErrorReason(errorStr);
         
         logError(errorStr, t);
                        
         if( statusTransitionType == STATUS_TRANSITION_TYPE_ERROR )
         {
            this.setStatus(ERROR);
         }
         else if( statusTransitionType == STATUS_TRANSITION_TYPE_CRITICAL_ERROR )
         {
            this.setStatus(CRITICAL_ERROR);
         }
         else if( statusTransitionType == STATUS_TRANSITION_TYPE_SHUT_DOWN )
         {
            this.setStatus(DOWN);
         }
         else
         {
            doExecuteStatusTransition(new StatusTransitionTask(this, STATUS_TRANSITION_TYPE_ERROR, this.errorReason.stringValue(), false));
         }

         if(t instanceof Error) throw (Error)t;
         else return false;
      }
      finally
      {
         synchronized(this.statusTransitionExecutionMonitor)
         {
            this.currentStatusTransitionExecutionThread = null;
            this.currentStatusTransitionTask = null;
            this.statusTransitionBeginTime = -1;
            this.statusTransitionExecutionMonitor.notifyAll();
         }
      }
   }
   
   /**
    * Subclass method for handling a status transition.
    * 
    * @since 2.0
    */
   protected boolean doExecuteStatusTransition(final StatusTransitionTask statusTransitionTask)
   {
      int statusTransitionType = statusTransitionTask.getStatusTransitionType();
      
      this.setErrorReason(statusTransitionTask.getReason());
      
      switch(statusTransitionType)
      {
         case STATUS_TRANSITION_TYPE_ENGAGE:
         {
            this.setStatus(INITIALIZING);
            
            if(!propertiesInitialized) initProperties(); // Initialize properties
            this.doInitialize();

            if( this.statusTransitionInterrupted || Thread.interrupted() ) return false;
                  
            this.setStatus(ENABLED); //Set status to ENABLE when done initializing
            
            return true;            
         }
         case STATUS_TRANSITION_TYPE_REINITIALIZE:
         {
            this.setStatus(REINITIALIZING);
            
            try
            {
               this.doShutDown();
            }
            catch(Exception e)
            {
               logError("Got exception while shutting down during reinitialization!", e);
            }
            
            if( this.statusTransitionInterrupted || Thread.interrupted() ) return false;
            
            if(!propertiesInitialized) initProperties(); // Initialize properties
            this.doInitialize();
            
            this.setStatus(ENABLED);

            return true;
         }
         case STATUS_TRANSITION_TYPE_SHUT_DOWN:
         {
            this.setStatus(SHUTTING_DOWN);
                        
            this.doShutDown();
                  
            if( this.statusTransitionInterrupted || Thread.interrupted() ) return false;             

            this.setStatus(DOWN);
            
            return true;
         }
         case STATUS_TRANSITION_TYPE_ERROR:
         {
            this.setStatus(ERROR);
            
            this.doError();
            
            return true;
         }
         case STATUS_TRANSITION_TYPE_CRITICAL_ERROR:
         {
            this.setStatus(CRITICAL_ERROR);
            
            this.doCriticalError();
            
            return true;
         }
      }
      
      return false;      
   }
   
   /**
    * Engages this component by changing it's status to <code>INITIALIZING</code>, which will result in a call to the 
    * method <code>doInitialize()</code>. After the return of that method call the status of the component 
    * will be <code>ENABLED</code>.<br>
    * <br>
    * Invoking this method will result in that the method {@link SubComponent#initProperties()} is called, if the <tt>propertiesInitialized</tt>
    * is set to <tt>false</tt>. If a subclass does not want this to happen it should set the <tt>propertiesInitialized</tt> flag to <tt>true</tt> in it's
    * constructors or before this method is called. <br>
    * <br>
    * <b>Note:</b> If the flag {@link #isAsynchronousStatusChanges() asynchronousStatusChanges} is set to true (default is false for 
    * SubComponents and true for SubSystems) this method is asynchronous - It only initiates the engaging of this component and 
    * returns before it is completed (i.e. it is executed in another thread).
    * 
    * @return true if  the component was successfully engaged, otherwise false. When asynchronous status transitions is enabled (SubSystem) the return value 
    * only indicated is the transition was successfully initiated.
    * 
    * @see #doInitialize()
    */
   public final boolean engage()
   {
      if( this.isDebugMode() ) logDebug("Attempting to engage.");
      return this.initiateStatusTransition(new StatusTransitionTask(this, STATUS_TRANSITION_TYPE_ENGAGE));
   }
   
   /**
    * Shuts down this component by setting it's status to <code>SHUTTING_DOWN</code>, which will result in a call to the 
    * method <code>doShutDown()</code>. After the return of that method call the status of the component 
    * will be <code>DOWN</code>.<br>
    * <br>
    * <b>Note:</b> If the flag {@link #isAsynchronousStatusChanges() asynchronousStatusChanges} is set to true (default is false for 
    * SubComponents and true for SubSystems) this method is asynchronous - It only initiates the engaging of this component and 
    * returns before it is completed (i.e. it is executed in another thread).
    * 
    * @return true if the component was successfully shut down, otherwise false. When asynchronous status transitions is enabled (SubSystem) the return value 
    * only indicated is the transition was successfully initiated.
    * 
    * @see #doShutDown()
    */
   public final boolean shutDown()
   {
      if( this.isDebugMode() ) logDebug("Attempting to shut down.");
      return this.initiateStatusTransition(new StatusTransitionTask(this, STATUS_TRANSITION_TYPE_SHUT_DOWN));
   }
   
   /**
    * Reinitializes this component by setting it's status to <code>REINITIALIZING</code>, which will result in a
    * method call to <code>doShutDown()</code> followed by a call to <code>doInitialize()</code>. After the return of the latter method call 
    * the status of the component will be <code>ENABLED</code>.<br>
    * <br>
    * <b>Note:</b> If the flag {@link #isAsynchronousStatusChanges() asynchronousStatusChanges} is set to true (default is false for 
    * SubComponents and true for SubSystems) this method is asynchronous - It only initiates the engaging of this component and 
    * returns before it is completed (i.e. it is executed in another thread).
    * 
    * @return true if the component was successfully reinitialized, otherwise false. When asynchronous status transitions is enabled (SubSystem) the return value 
    * only indicated is the transition was successfully initiated.
    * 
    * @see #doShutDown()
    * @see #doInitialize()
    */
   public final boolean reinitialize()
   {
      if( this.isDebugMode() ) logDebug("Attempting to reinitialize.");
      return this.initiateStatusTransition(new StatusTransitionTask(this, STATUS_TRANSITION_TYPE_REINITIALIZE));
   }
   
   /**
    * Signal that an error has occured in this component and sets status to <code>ERROR</code>, which will
    * result in a call to the method <code>doError()</code>. If this method is invoked from inside one of 
    * the status methods (doXXX) a {@link StatusTransitionException} will be thrown. That exception should NOT 
    * be caught in the status method, but if it is it should be rethrown.
    * 
    * @return true if an error was successfully signalled, otherwise false. When asynchronous status transitions is enabled (SubSystem) the return value 
    * only indicated is the transition was successfully initiated.
    * 
    * @see #doError()
    */
   public final boolean error()
   {
      if( this.isDebugMode() ) logDebug("Attempting to go to status error.");
      return this.initiateStatusTransition(new StatusTransitionTask(this, STATUS_TRANSITION_TYPE_ERROR, this.errorReason.stringValue()));
   }
   
   /**
    * Signal that an error has occured in this component and sets status to <code>ERROR</code>, which will
    * result in a call to the method <code>doError()</code>. If this method is invoked from inside one of 
    * the status methods (doXXX) a {@link StatusTransitionException} will be thrown. That exception should NOT 
    * be caught in the status method, but if it is it should be rethrown.
    * 
    * @param reason the reason for the error.
    * 
    * @return true if an error was successfully signalled, otherwise false. When asynchronous status transitions is enabled (SubSystem) the return value 
    * only indicated is the transition was successfully initiated.
    * 
    * @see #doError()
    */
   public boolean error(final String reason)
   {
      if( this.isDebugMode() ) logDebug("Attempting to go to status error.");
      return this.initiateStatusTransition(new StatusTransitionTask(this, STATUS_TRANSITION_TYPE_ERROR, reason));
   }
   
   /**
    * Signals that a critical error has occurred within this component by setting the status 
    * to CRITICAL_ERROR, which will result in a call to the method <tt>doCriticalError()</tt>.
    * 
    * @return true if an critical error was successfully signalled, otherwise false. When asynchronous status transitions is enabled (SubSystem) the return value 
    * only indicated is the transition was successfully initiated.
    * 
    * @see #doCriticalError()
    */
   public final boolean criticalError()
   {
      if( this.isDebugMode() ) logDebug("Attempting to go to status critical error.");
      return this.initiateStatusTransition(new StatusTransitionTask(this, STATUS_TRANSITION_TYPE_CRITICAL_ERROR, this.errorReason.stringValue()));
   }
   
   /**
    * Signals that a critical error has occurred within this component by setting the status 
    * to CRITICAL_ERROR, which will result in a call to the method <tt>doCriticalError()</tt>.
    *
    * @param reason the reason for the error.
    * 
    * @return true if an critical error was successfully signalled, otherwise false. When asynchronous status transitions is enabled (SubSystem) the return value 
    * only indicated is the transition was successfully initiated.
    * 
    * @see #doCriticalError()
    */
   public boolean criticalError(final String reason)
   {
      if( this.isDebugMode() ) logDebug("Attempting to go to status critical error.");
      return this.initiateStatusTransition(new StatusTransitionTask(this, STATUS_TRANSITION_TYPE_CRITICAL_ERROR, reason));
   }
   
   /**
    * Destroys this component and all child components. This method is used by JServer to destroy this component when 
    * shutting down the server. Applications should never call this method directly, however subclasses may 
    * override this method but should always call the superclass implementation.
    */
   protected void destroy()
   {
      this.setStatus(DESTROYED);
      
      synchronized(componentLock)
      {
         destroyed = true;
      
         for(int i=0; i<childComponents.size(); i++)
         {
            ((SubComponent)childComponents.get(i)).destroy();
         }
      
         this.childComponents.clear();
      
         this.properties.clear();   
      
         try{
         java.rmi.server.UnicastRemoteObject.unexportObject(this.rmiAdapter, true);
         }catch(Exception e){}
      
         this.rmiAdapter = null;
      
         remove();
      }
   }
   
   /* ##### STATUS RELATED METHODS ENDS #####*/
   
   
   /* ##### OVERRIDABLE STATUS METHODS BEGINS #####*/
   
   /**
   * Statusmethod for the status INITIALIZING. Subclasses can override this method to provide code to 
   * be executed  when initializing (engaging) a subsystem.<br>
   * <br>
   * The thread of this subsystem will be started after the return of this method.<br>
   * <br>
   * The appropriate way for subclass implementations to signal that an error has occurred in this method is to throw a 
   * {@link StatusTransitionException}.
   * 
   * @see #engage()
   */
   protected void doInitialize()
   {
      // Cascade engage 
      if( this.cascadeEngageAndShutDown )
      {
         List targetChildren = this.getSubComponents();
         SubComponent child;
         if( targetChildren != null )
         {
            for(int i=0; i<targetChildren.size(); i++)
            {
               child = (SubComponent)targetChildren.get(i);
               if( child != null ) child.engage();
            }
         }
      }
   }
   
   /**
   * Statusmethod for the status SHUTTING_DOWN. Subclasses can override this method to provide code to 
   * be executed  when shutting down a subsystem.<br>
   * <br>
   * This implementation is empty.<br>
   * <br>
   * The appropriate way for subclass implementations to signal that an error has occurred in this method is to throw a 
   * {@link StatusTransitionException}.
   * 
   * @see #shutDown()
   */
   protected void doShutDown()
   {
      // Cascade shut down 
      if( this.cascadeEngageAndShutDown )
      {
         List targetChildren = this.getSubComponents();
         SubComponent child;
         if( targetChildren != null )
         {
            for(int i=0; i<targetChildren.size(); i++)
            {
               child = (SubComponent)targetChildren.get(i);
               if( child != null ) child.shutDown();
            }
         }
      }
   }
      
   /**
   * Statusmethod for the status ERROR. Subclasses can override this method to provide code to 
   * be executed  when an error has occurred in this subsystem.<br>
   * <br>
   * This implementation calls the method {@link #doShutDown()} to make sure the subsystem is shut down.<br>
   * <br>
   * The appropriate way for subclass implementations to signal that an error has occurred in this method is to throw a 
   * {@link StatusTransitionException}.
   * 
   * @see #error(String)
   */
   protected void doError()
   {
      doShutDown();
   }
   
   /**
   * Statusmethod for the status CRITICAL_ERROR. Subclasses can override this method to provide code to 
   * be executed  when a critical error has occurred in this subsystem.<br>
   * <br>
   * This implementation calls the method {@link #doShutDown()} to make sure the subsystem is shut down.<br>
   * <br>
   * The appropriate way for subclass implementations to signal that an error has occurred in this method is to throw a 
   * {@link StatusTransitionException}.
   * 
   * @see #criticalError()
   */
   protected void doCriticalError()
   {
      doShutDown();
   }
   
   /* ##### OVERRIDABLE STATUS METHODS ENDS #####*/
}
