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
package com.teletalk.jserver.log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.AppenderAttachable;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import com.teletalk.jserver.JServerConstants;
import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.event.EventQueue;
import com.teletalk.jserver.event.StructureEvent;
import com.teletalk.jserver.event.StructureEventListener;
import com.teletalk.jserver.property.BooleanProperty;
import com.teletalk.jserver.property.LogLevelProperty;
import com.teletalk.jserver.property.NumberProperty;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.util.MessageQueue;

/**
 * The LogManager is a SubSystem that is responsible for the logging function in the server. By itself
 * the LogManager has no ability to log but must instead depend on {@link Logger Loggers} to perform this function. The primary
 * function of the LogManager is to serve as a host for several different types of Loggers and distribute LogMessages to them.
 * This way the rest of the server can use logging in a more cetralized manner.
 * 
 * @see com.teletalk.jserver.SubSystem
 * @see Logger
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class LogManager extends SubSystem implements StructureEventListener, AppenderAttachable
{
   private final MessageQueue msgQueue;
	private final ArrayList appenders;
	
	private int rejectedLoggingEvents = 0;
	   
   private final LogManagerAppender logManagerAppender = new LogManagerAppender(this);
   
   private NumberProperty maxQueueSize;
   
   private BooleanProperty getLocationInformation;
   
   private BooleanProperty addToRootLogger; // Since 2.1 (20050419)
   
   private LogLevelProperty rootLoggerLogLevel; // Since 2.1 (20050419)
   
   
   /**
    * Creates a new LogManager.
    * 
    * @param parent the parent of this LogManager.
    */
   public LogManager(SubSystem parent)
   {
      this(parent, "LogManager");
   }
	
	/**
	 * Creates a new LogManager.
	 * 
	 * @param parent the parent of this LogManager.
	 */
	public LogManager(SubSystem parent, String name)
	{
		super(parent, name);
		
		this.maxQueueSize = new NumberProperty(this, "maxQueueSize", 10000, Property.MODIFIABLE_NO_RESTART);
		this.maxQueueSize.setDescription("The maximum size of the queue that holds log messages that are waiting to be processed. A value of 0 or less means that no maximum limit will be imposed.");
		super.addProperty(this.maxQueueSize);
      
      this.getLocationInformation = new BooleanProperty(this, "getLocationInformation", false, BooleanProperty.MODIFIABLE_NO_RESTART);
      this.getLocationInformation.setDescription("Boolean property indicating if location information should be generated for logging events. Note that generation of location information may have an adverse effect on performance.");
      super.addProperty(this.getLocationInformation);
      
      this.addToRootLogger = new BooleanProperty(this, "addToRootLogger", false, BooleanProperty.MODIFIABLE_NO_RESTART);
      this.addToRootLogger.setDescription("Boolean property indicating if this LogManager should be added as an appender in the root loggger, thus enabling logging through non JServer components.");
      super.addProperty(this.addToRootLogger);
      
      this.rootLoggerLogLevel = new LogLevelProperty(this, "rootLoggerLogLevel", Level.INFO, Property.MODIFIABLE_NO_RESTART);
      this.rootLoggerLogLevel.setDescription("The log level of the root logger.");
      super.addProperty(this.rootLoggerLogLevel);
		
      this.msgQueue = new MessageQueue();
      this.appenders = new ArrayList();
      
      // Register appender
      parent.getLogger().addAppender(this.logManagerAppender);
	}
   
	/**
    * Destroys this LogManager. 
	 */
   protected void destroy()
   {
      try{
      parent.getLogger().removeAppender(this.logManagerAppender);
      }catch (Exception e) {}
      try{
      Logger.getRootLogger().removeAppender(this.logManagerAppender);
      }catch (Exception e) {}
      
      super.destroy();
   }
	
	/**
	 * Engages the LogManager.
	 */
	protected void doInitialize()
	{
		super.doInitialize();
            
      // Attempt to get old property "max queuesize"
      this.initFromConfiguredProperty(this.maxQueueSize, "max queuesize", false, true);
      // Attempt to get old property "get location information"
      this.initFromConfiguredProperty(this.getLocationInformation, "get location information", false, true);

      if( !super.isReinitializing() )
      {
         Appender[] appendersCopy;
      
         synchronized(this)
         {
            appendersCopy = (Appender[])this.appenders.toArray(new Appender[0]);
         }
         
         for(int i=0; i<appendersCopy.length; i++)
         {
            if( appendersCopy[i] instanceof AppenderComponent )
            {
               AppenderComponent appenderComponent = (AppenderComponent)appendersCopy[i];
               
               if( (appenderComponent != null) && (!appenderComponent.isEnabled()) )
               {
                  if(!((AppenderComponent)appenders.get(i)).engage()) logError("Failed to enable " + ((AppenderComponent)appenders.get(i)).getName() + ".");
               }
            }
         }
         
         EventQueue eventQueue = getEventQueue();
            
         if( eventQueue != null )
         {
            eventQueue.registerStructureEventListener(this);
         }
      }
      
      // Register appender
      if( !parent.getLogger().isAttached(this.logManagerAppender) )
      {
         parent.getLogger().addAppender(this.logManagerAppender);
      }
	}
	
	/**
	 * Shuts down the LogManager.
	 */
	protected void doShutDown()
	{
		super.doShutDown();
		
		if( !super.isReinitializing() )
      { 
         try
   		{
   			long waitStart = System.currentTimeMillis();
   			long waitTime;
   						
   			for(int i=0; (i<5) && !msgQueue.isEmpty(); i++)
   			{
   				waitTime = 1000 - (System.currentTimeMillis() - waitStart);
   				
   				if(waitTime > 0)
   				{
   					synchronized(this)
   					{
   						wait(waitTime);
   					}
   				}
   				else break;
   			}
   		}
   		catch(Exception e){}
   
         this.msgQueue.clear();
   		
   		try
   		{
            Appender[] appendersCopy;
         
            synchronized(this)
            {
               appendersCopy = (Appender[])this.appenders.toArray(new Appender[0]);
            }
         
   			for(int i=0; i<appendersCopy.length; i++)
   			{
               if( appendersCopy[i] instanceof AppenderComponent)
               {
      				AppenderComponent appenderComponent = (AppenderComponent)appendersCopy[i];
      				
      				if(appenderComponent != null)
      				{
      					try
                     {
      						if(appenderComponent.isEnabled())
      						{ 
      							appenderComponent.shutDown();
      						}
      					}catch(Exception e){}
      				}
               }
   			}
   		}
   		catch(Exception e){}

         // Unregister appender
         parent.getLogger().removeAppender(this.logManagerAppender);
      }
	}
   
   /**
    * Called when a property has been initialized with a value from persistent storage.
    * 
    * @since 2.1
    */
   public void propertyInitialized(final Property property)
   {
      this.propertyModifiedInternal(property);
      
      super.propertyInitialized(property);
   }
   
   /**
    * Called when the value of a property has been modified.
    * 
    * @since 2.1
    */
   public void propertyModified(final Property property)
   {
      this.propertyModifiedInternal(property);
      
      super.propertyModified(property);
   }
   
   private void propertyModifiedInternal(final Property property)
   {
      if( property == this.addToRootLogger )
      {
         try
         {
            if( this.addToRootLogger.booleanValue() )
            {
               Logger.getRootLogger().setLevel( this.rootLoggerLogLevel.getLevel() );
               Logger.getRootLogger().addAppender(this.logManagerAppender);
            }
            else
            {
               Logger.getRootLogger().removeAppender(this.logManagerAppender);
            }
         }
         catch(Exception e)
         {
            e.printStackTrace();
         }
      }
      if( property == this.rootLoggerLogLevel )
      {
         if( this.addToRootLogger.booleanValue() )
         {
            Logger.getRootLogger().setLevel( this.rootLoggerLogLevel.getLevel() );
         }
      }
   }
      
   /**
    * Notification of a structure modificaion. The primary function of this method is to ensure that dynamically added 
    * loggers are registered properly.
    * 
    * @param structureEvent a StructureEvent object.
    * 
    * @since 1.3.1 Build 691
    */
   public void structureModification(final StructureEvent structureEvent)
   {
      if( structureEvent.getSourceComponent() == this )
      {
         Object target = structureEvent.getTarget();
         if( target instanceof AppenderComponent )
         {
            boolean addAppender = false;
            boolean removeAppender = false;
            
            if( structureEvent.getStructureModification() == StructureEvent.ADDED )
            {
               // Prevent a logger previously added through attachLogger to be added again
               synchronized(this)
               {
                  if( !this.appenders.contains(target) )
                  {
                     addAppender = true;
                  }
               }
            }
            else
            {
               synchronized(this)
               {
                  if( this.appenders.contains(target) )
                  {
                     removeAppender = true;
                  }
               }
            }
            
            if( addAppender )
            {
               this.addAppender((AppenderComponent)target);
            }
            else if( removeAppender )
            {
               this.removeAppender((AppenderComponent)target);
            }
         }
      }
   }
	
	/**
	 * Validates a modification of a property's value. 
	 * 
	 * @param property The property to be validated.
	 * 
	 * @return boolean value indicating if the property passed (true) validation or not (false).
	 * 
	 * @see com.teletalk.jserver.property.PropertyOwner
	 */
	/*public boolean validatePropertyModification(Property property)
	{
		if(property == maxQueueSize) return maxQueueSize.intValue() > 100;
		else return super.validatePropertyModification(property);
	}*/
	
	/**
	 * Sets the maximum number of logmessages that can be waiting in the queue 
	 * of this LogManager. A value of 0 or less means that no maximum limit will be imposed.
	 * 
	 * @param maxLogMessageQueueSize the new maximum value.
	 */
	public void setMaxLogMessageQueueSize(int maxLogMessageQueueSize)
	{
		this.maxQueueSize.setValue(maxLogMessageQueueSize);
	}
	
	/**
	 * Gets the maximum number of logmessages that can be waiting in the queue 
	 * of this LogManager. A value of 0 or less means that no maximum limit will be imposed.
	 * 
	 * @return the maximum value.
	 */
	public int getMaxLogMessageQueueSize()
	{
		return this.maxQueueSize.intValue();
	}
   
   /**
    * Gets the flag indicating if this LogManager should be added as an appender in the root loggger, thus enabling logging through non JServer components.
    * 
    * @since 2.1.6 (20070508)
    */
   public boolean getAddToRootLogger()
   {
      return addToRootLogger.booleanValue();
   }

   /**
    * Sets the flag indicating if this LogManager should be added as an appender in the root loggger, thus enabling logging through non JServer components.
    * 
    * @since 2.1.6 (20070508)
    */
   public void setAddToRootLogger(boolean addToRootLogger)
   {
      this.addToRootLogger.setValue(addToRootLogger);
   }

   /**
    * Gets the flag indicating if location information should be generated for logging events. 
    * Note that generation of location information may have an adverse effect on performance.
    * 
    * @since 2.1.6 (20070508)
    */
   public boolean getGetLocationInformation()
   {
      return getLocationInformation.booleanValue();
   }

   /**
    * Sets the flag indicating if location information should be generated for logging events. 
    * Note that generation of location information may have an adverse effect on performance.
    * 
    * @since 2.1.6 (20070508)
    */
   public void setGetLocationInformation(boolean getLocationInformation)
   {
      this.getLocationInformation.setValue(getLocationInformation);
   }

   /**
    * Gets the maximum size of the queue that holds log messages that are waiting to be processed. 
    * A value of 0 or less means that no maximum limit will be imposed.
    * 
    * @since 2.1.6 (20070508)
    */
   public int getMaxQueueSize()
   {
      return maxQueueSize.intValue();
   }

   /**
    * Sets the maximum size of the queue that holds log messages that are waiting to be processed. 
    * A value of 0 or less means that no maximum limit will be imposed.
    * 
    * @since 2.1.6 (20070508)
    */
   public void setMaxQueueSize(int maxQueueSize)
   {
      this.maxQueueSize.setValue(maxQueueSize);
   }

   /**
    * Gets the log level of the root logger.
    * 
    * @since 2.1.6 (20070508)
    */
   public Level getRootLoggerLogLevel()
   {
      return rootLoggerLogLevel.getLevel();
   }

   /**
    * Sets the log level of the root logger.
    * 
    * @since 2.1.6 (20070508)
    */
   public void setRootLoggerLogLevel(Level rootLoggerLogLevel)
   {
      this.rootLoggerLogLevel.setValue(rootLoggerLogLevel);
   }
   
   
   /* ##### IMPLEMENTED METHODS FROM INTERFACE APPENDERATTACHABLE BEGIN ##### */
   

   /**
    * Adds an appender.
    */
   public void addAppender(final Appender appender)
   {
      if(appender == null) return;

      synchronized(this)
      {
         if( !this.appenders.contains(appender) )
         {
            this.appenders.add(appender);
         }
      }
      
      if( appender instanceof AppenderComponent )
      {
         AppenderComponent appenderComponent = (AppenderComponent)appender;
         if( !super.hasSubComponent(appenderComponent) )
         {
            if( this.isEnabled() ) addSubComponent(appenderComponent, true);
            else addSubComponent(appenderComponent);
         }
      }
      
      synchronized(this)
      {
         notify();
      }
   }
   
   /**
    * Get all previously added appenders as an Enumeration.
    */
   public Enumeration getAllAppenders()
   {
      synchronized(this)
      {
         return Collections.enumeration(this.appenders);
      }
   }
   
   /**
    * Get all previously added appenders as an array.
    */
   public Appender[] getAppenders()
   {
      synchronized(this)
      {
         return (Appender[])this.appenders.toArray(new Appender[0]);
      }
   }
   
   /**
    * Get an appender by name.
    */
   public Appender getAppender(final String name)
   {
      if( name == null ) return null;
      
      synchronized(this)
      {
         int size = this.appenders.size();
         Appender appender;
         
         for(int i = 0; i < size; i++)
         {
            appender = (Appender) this.appenders.get(i);
            if( (appender != null) && name.equals(appender.getName()) ) return appender;
         }
         return null;
      }
   }
   
   /**
    * Returns <code>true</code> if the specified appender is in list of attached attached, <code>false</code> otherwise.
    */
   public boolean isAttached(final Appender appender)
   {
      if( appender == null ) return false;
      
      synchronized(this)
      {
         int size = this.appenders.size();
         Appender iterationAppender;
         
         for(int i = 0; i < size; i++)
         {
            iterationAppender = (Appender) this.appenders.get(i);
            if( appender.equals(iterationAppender) ) return true;
         }
         return false;
      }
   }
   
   /**
    * Remove all previously added appenders.
    */
   public void removeAllAppenders()
   {
      synchronized(this)
      {
         int size = this.appenders.size();
         
         for(int i = 0; i < size; i++)
         {
            this.removeAppender( (Appender) this.appenders.get(i) );
         }
      }
   }
   
   /**
    * Remove the specified appender.
    */
   public void removeAppender(final Appender appender)
   {
      if(appender == null) return;
      boolean removed = false;
      
      synchronized(this)
      {
         removed = this.appenders.remove(appender);
      }
      
      if( removed && (appender instanceof AppenderComponent) )
      {
         super.removeSubComponent((AppenderComponent)appender, true);
      }
      
      synchronized(this)
      {
         notify();
      }
   }
   
   /**
    * Removes the appender with the specified name.  
    */
   public void removeAppender(final String name)
   {
      this.removeAppender(this.getAppender(name));
   }
   
   
   /* ##### IMPLEMENTED METHODS FROM INTERFACE APPENDERATTACHABLE END ##### */
		
   
   /**
    * Puts a LoggingEvent in the queue of this LogManager. The LoggingEvent will be processed and dispatched to appenders 
    * in a separate thread.  
    * 
    * @param event a LoggingEvent to be queued.
    * 
    * @since 2.0
    */
   public void append(final LoggingEvent event)
   {
      final int stat = getStatus();
      
      if( (stat != SHUTTING_DOWN) && (stat != DOWN) && (stat != CRITICAL_ERROR) && (msgQueue != null) )
      {
         synchronized(msgQueue)
         {
            int maxQueueSizeTmp = maxQueueSize.intValue();
            if( (maxQueueSizeTmp <= 0) || (msgQueue.size() < maxQueueSizeTmp) )
            {
               int rejectedLoggingEventsLocal = rejectedLoggingEvents;
               if(rejectedLoggingEvents > 0) rejectedLoggingEvents = 0;
               
               this.prepareLoggingEvent(event);
               
               msgQueue.putMsg(event);
               
               if( rejectedLoggingEventsLocal > 0 ) super.logError("Messagequeue overflow! " + rejectedLoggingEventsLocal + " logmessages rejected!");
            }
            else
            {
               rejectedLoggingEvents++;
            }
         }
      }
   }
   
   /**
    * Prepares the LoggingEvent for beeing processes in another thread, by saving away the NDC, MDC, thread name,  
    * throwable information and location information (if the getLocationInformation property of this system is set to true).   
    * 
    * @param loggingEvent the logging event to prepare.
    * 
    * @since 2.0
    */
   public void prepareLoggingEvent(final LoggingEvent loggingEvent)
   {
      prepareLoggingEvent(loggingEvent, this.getLocationInformation.booleanValue());
   }
   
   /**
    * Prepares the LoggingEvent for beeing processes in another thread, by saving away the NDC, MDC, thread name,  
    * throwable information and location information (if parameter getLocationInformation is true).   
    * 
    * @param loggingEvent the logging event to prepare.
    * @param getLocationInformation flag indicating if location information should be fetched for the logging event.
    * 
    * @since 2.0
    */
   public static void prepareLoggingEvent(final LoggingEvent loggingEvent, final boolean getLocationInformation)
   {
      // Set the NDC and thread name for the calling thread as these LoggingEvent fields were not set at event creation time.
      loggingEvent.getNDC();
      loggingEvent.getThreadName();
      // Get a copy of this thread's MDC.
      loggingEvent.getMDCCopy();
      ThrowableInformation throwableInformation =  loggingEvent.getThrowableInformation();
      if( throwableInformation != null )
      {
         // Make the ThrowableInformation of the LoggingEvent store away the representation of the stack trace.
         throwableInformation.getThrowableStrRep();
      }
      if( getLocationInformation ) 
      {
         loggingEvent.getLocationInformation();
      }
   }
	
	/**
	 * The thread method of this LogManager. It's primary function is to get LoggingEvents from the queue of
	 * this LogManager and distribute them to the appenders.
	 */
   public void run()
   {
      LoggingEvent loggingEvent;
      Appender appender;
      AppenderComponent appenderComponent;
      Appender[] appendersCopy;
      long appenderFlushInterval = 2500; //TODO: Property?
      long waitTime = appenderFlushInterval;
      boolean flushAppenders = true;
      long lastAppenderUpdate = 0;
      
      try
      {
         Thread.sleep(100); //Sleep a while to catch up with loggers
      }
      catch(InterruptedException e){}  
      
      while(canRun)
      {
         loggingEvent = null;

         try
         {
            msgQueue.waitForData(waitTime);
            
            if(msgQueue.isEmpty())
            {
               synchronized(this)
               {
                  notify();
               }
            }
         }
         catch(InterruptedException e)
         {
            if(!canRun) continue;
         }
         
         // Check if appender components are to be flushed
         if((System.currentTimeMillis() - lastAppenderUpdate) > appenderFlushInterval)
         {
            flushAppenders = true;
            waitTime = appenderFlushInterval;
            lastAppenderUpdate = System.currentTimeMillis() - 33;
         }
         else
         {
            flushAppenders = false;
            waitTime = appenderFlushInterval - (System.currentTimeMillis() - lastAppenderUpdate);
            if(waitTime <= 0) waitTime = 1;
         }
         
         loggingEvent = (LoggingEvent)msgQueue.getMsgIfAny();
      
         synchronized(this)
         {
            appendersCopy = (Appender[])this.appenders.toArray(new Appender[0]);
         }
         
         for(int i=0; i<appendersCopy.length; i++)
         {
            appender = appendersCopy[i];
            if( appender == null ) continue;
            
            if( appender instanceof AppenderComponent ) appenderComponent = (AppenderComponent)appender; 
            else appenderComponent = null;
            
            if( flushAppenders )
            {
               try
               {
                  if(appenderComponent.isEnabled())
                  {
                     appenderComponent.flushBuffers();
                  }
               }
               catch(Exception e)
               {
                  logError("Error while flushing " + appenderComponent.getName() + "!", e);
                  if(!canRun) continue;
               }
            }
            
            if(loggingEvent != null)
            {
               try
               {
                  if(appenderComponent.isEnabled())
                  {
                     appender.doAppend(loggingEvent);
                     
                     if( (appenderComponent != null) && (appenderComponent.getErrorCount() > 0) && 
                       ((System.currentTimeMillis() - appenderComponent.getLastErrorTime()) > 30*1000) )
                     {
                        appenderComponent.setErrorCount(0);
                     }
                  }
               }
               catch(Exception e)
               {
                  if( appenderComponent != null )
                  {
                     int errorCount = appenderComponent.getErrorCount();
                     
                     // TODO: add property
                     if(errorCount == 5)
                     {
                        try
                        {
                           appenderComponent.shutDown();
                        }
                        catch(Exception ex){}

                        logCriticalError("5 consecutive errors while logging to " + appenderComponent.getName() + "! Logger disabled!", e, JServerConstants.LOG_MESSAGE_ID_LOG_APPENDER_ERROR);
                     }
                     else if(errorCount < 5)
                     {
                        errorCount++;
                        appenderComponent.setErrorCount(errorCount);
                        logError("Error while logging to " + appender.getName() + "!", e);
                     }
                  }
               }
            }
         }
      } 
   }
   
   /**
    * AppenderSkeleton used for recording logging events, logged via Log4J loggers, to this LogManager.
    */
   private static class LogManagerAppender extends AppenderSkeleton
   {
      private final LogManager logManager;
      
      public LogManagerAppender(final LogManager logManager)
      {
         this.logManager = logManager;
         super.setName("LogManagerAppender");
      }
      
      protected void append(LoggingEvent event)
      {
         logManager.append(event);
      }
      
      public void close()
      {
      }
      
      public boolean requiresLayout()
      {
         return false;
      }
   }
}
