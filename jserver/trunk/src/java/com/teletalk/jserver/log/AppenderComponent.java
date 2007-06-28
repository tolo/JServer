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

import java.io.InputStream;
import java.rmi.RemoteException;

import org.apache.log4j.Appender;
import org.apache.log4j.AppenderSkeleton;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.spi.ErrorHandler;
import org.apache.log4j.spi.Filter;
import org.apache.log4j.spi.LoggingEvent;

import com.teletalk.jserver.JServerConstants;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.property.LogLevelProperty;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.rmi.adapter.AppenderComponentRmiAdapter;
import com.teletalk.jserver.rmi.adapter.RmiAdapter;

/**
 * Abstract base class for all appender components that are to be used in conjunction with a {@link com.teletalk.jserver.log.LogManager}. This 
 * class replaces the 1.X class Logger. 
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0
 */
public abstract class AppenderComponent extends SubComponent implements Appender
{
   private int errorCount = 0;
   
   private long lastErrorTime;
   
   private final InternalAppenderImpl internalAppenderImpl = new InternalAppenderImpl(this);
   
   /** The threshold log level. This property determines the minimum log level of logging events that may be logged through this component. */
   protected final LogLevelProperty logLevelThreshold;
   
   /**
    * Creates a new AppenderComponent.
    * 
    * @param name the name of this AppenderComponent.
    */
   public AppenderComponent(String name)
   {
      this(null, name);
   }
   
   /**
    * Creates a new AppenderComponent.
    * 
    * @param parent the parent component (usually the LogManager).
    * @param name the name of this AppenderComponent.
    */
   public AppenderComponent(SubComponent parent, String name)
   {
      this(parent, name, Level.ALL);
   }
   
   /**
    * Creates a new AppenderComponent.
    * 
    * @param parent the parent component (usually the LogManager).
    * @param name the name of this AppenderComponent.
    * @param logLevelThreshold the default log level threshold.
    */
   public AppenderComponent(SubComponent parent, String name, Level logLevelThreshold)
   {
      super(parent, name);
      
      this.logLevelThreshold = new LogLevelProperty(this, "logLevelThreshold", logLevelThreshold, Property.MODIFIABLE_NO_RESTART);
      this.logLevelThreshold.setDescription("The log level threshold of this appender component (not to be confused with the log level of this component!). " +
            "This property determines the minimum log level of logging events that may be logged through this component.");
      super.addProperty(this.logLevelThreshold);
      
      internalAppenderImpl.setThreshold(this.logLevelThreshold.getLevel());
   }
   
   /**
    * Enables this AppenderComponent.
    */
   public synchronized void doInitialize()
   {
      super.doInitialize();
      
      // Attempt to get old property "log level threshold"
      super.initFromConfiguredProperty(this.logLevelThreshold, "log level threshold", false, true);
      
      internalAppenderImpl.setThreshold(this.logLevelThreshold.getLevel());
   }
   
   /**
    *  Called when a property owned by this AppenderComponent is modified.
    * 
    * @param property the property that was modified.
    */
   public void propertyModified(final Property property)
   {
      super.propertyModified(property);
      
      if( property == this.logLevelThreshold )
      {
         internalAppenderImpl.setThreshold(this.logLevelThreshold.getLevel());
      }
   }
   
   /**
    * Returns a AppenderComponentRmiAdapter that makes this AppenderComponent RMI enabled.
    * 
    * @return a RmiAdapter (AppenderComponentRmiAdapter) object.
    * 
    * @see AppenderComponentRmiAdapter
    */
   public RmiAdapter getRmiAdapter()
   {
      if(rmiAdapter == null)
      {
         try
         {
            setRmiAdapter(new AppenderComponentRmiAdapter(this));
         }
         catch(RemoteException e)
         {
            logError("Unable to create AppenderComponentRmiAdapter", e);
         }
      }
            
      return rmiAdapter;   
   }
   
   /**
    * Gets the log level threshold.
    */
   public Level getLogLevelThreshold()
   {
      return this.logLevelThreshold.getLevel();
   }
   
   /**
    * Sets the log level threshold.
    */
   public void setLogLevelThreshold(Level logLevelThreshold)
   {
      this.logLevelThreshold.setLevel(logLevelThreshold);
   }
   
   /**
    * Called to flush any bufferd data in this AppenderComponent. This method is provided exclusively for subclasses. 
    */
   public void flushBuffers()
   {
   }
   
   /**
    * Returns the names of all logfiles used by this AppenderComponent. Subclasses should override this method to return 
    * a list of available logs (previous and current) for AppenderComponent.
    * 
    * @return String array containing filenames.
    */
   public LogData[] getLogs()
   {
      return null;
   }
   
   /**
    * Gets an input stream for a specific log . Subclasses should override this method to return 
    * a list of available logs (previous and current) for AppenderComponent.
    * 
    * @param log the name of the log, as previously returned by a call to {@link #getLogs()}. 
    * 
    * @return an InputStream for the specified log, or <code>null</code> if this AppenderComponent is 
    * unable to return streams for logs. 
    */
   public InputStream getLogAsStream(final String log)
   {
      return null;
   }
   
   /**
    * Gets the number of times this Logger has failed to log.
    * 
    * @return the number of failures.
    */
   public final int getErrorCount()
   {
      return errorCount;   
   }
   
   /**
    * Sets the number of times this Logger has failed to log.
    * 
    * @param count the new fail count.
    */
   public final void setErrorCount(int count)
   {
      errorCount = count;  
      lastErrorTime = System.currentTimeMillis();
   }
   
   /**
    * Gets the time of the last error.
    * 
    * @return the time of the last error.
    */
   public final long getLastErrorTime()
   {
      return lastErrorTime;
   }
   
   /**
    * Called to record a LoggingEvent to this AppenderComponent.
    * 
    * @param event the LoggingEvent to record.
    * 
    * @throws Exception if an error occurred while attempting to record the event.
    */
   protected abstract void append(LoggingEvent event) throws Exception;
   
   
   /**
    * Indicates whether some other object is "equal to" this one.
    */
   public boolean equals(Object obj)
   {
      if( obj instanceof AppenderComponent )
      {
         return this.getName().equals( ((AppenderComponent)obj).getName() );
      }
      return false;
   }
   
   
   /* ##### FROM APPENDER  ##### */
   

   /**
    * Add a filter to the end of the filter list.
    */
   public void addFilter(final Filter newFilter)
   {
      this.internalAppenderImpl.addFilter(newFilter);
   }

   /**
    * Returns the head Filter.
    */
   public Filter getFilter()
   {
      return internalAppenderImpl.getFilter();
   }

   /**
    * Clear the list of filters by removing all the filters in it.
    */
   public void clearFilters()
   {
      this.internalAppenderImpl.clearFilters();
   }

   /**
    * Release any resources allocated within the appender.
    */
   public void close()
   {
      this.internalAppenderImpl.close();
   }

   /**
    * Log in the appender specific way.
    */
   public void doAppend(final LoggingEvent event)
   {
      this.internalAppenderImpl.doAppend(event);
   }

   /**
    * Set the ErrorHandler for this appender.
    */
   public void setErrorHandler(final ErrorHandler errorHandler)
   {
      this.internalAppenderImpl.setErrorHandler(errorHandler);
   }

   /**
    * Returns the ErrorHandler for this appender.
    */
   public ErrorHandler getErrorHandler()
   {
      return this.internalAppenderImpl.getErrorHandler();
   }

   /**
    * Set the Layout for this appender.
    */
   public void setLayout(final Layout layout)
   {
      this.internalAppenderImpl.setLayout(layout);
   }

   /**
    * Get the Layout for this appender.
    */
   public Layout getLayout()
   {
      return this.internalAppenderImpl.getLayout();
   }

   /**
    * Set the name of this appender.
    */
   public void setName(final String name)
   {
      // TODO: Rename component?
   }

   /**
    * Called to determine if the appender requires a layout. This method returns <code>false</code> as default.
    */
   public boolean requiresLayout()
   {
      return false;
   }
   
   
   /* ##### INTERNAL CLASS InternalAppenderImpl ##### */
   
   
   /**
    * Internal appender skeleton helper class.
    */
   private static class InternalAppenderImpl extends AppenderSkeleton
   {
      private final AppenderComponent appenderComponent;
      
      public InternalAppenderImpl(final AppenderComponent appenderComponent)
      {
         this.appenderComponent = appenderComponent;
      }
      
      /**
       */
      protected void append(LoggingEvent event)
      {
         try
         {
            this.appenderComponent.append(event);
         }
         catch(Exception e)
         {
            if( appenderComponent != null )
            {
               int errorCount = this.appenderComponent.getErrorCount();
               
               if(errorCount >= 5)
               {
                  try
                  {
                     this.appenderComponent.shutDown();
                  }
                  catch(Exception ex){}
   
                  this.appenderComponent.logCriticalError("5 consecutive errors while logging to " + appenderComponent.getName() + "! Logger disabled!", e, JServerConstants.LOG_MESSAGE_ID_LOG_APPENDER_ERROR);
               }
               else
               {
                  errorCount++;
                  appenderComponent.setErrorCount(errorCount);
                  this.appenderComponent.logError("Error occurred while appending!", e);
               }
            }
         }
      }
      
      /**
       */
      public void close()
      {
      }
      
      /**
       */
      public boolean requiresLayout()
      {
         return false;
      }
   }
}
