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
package com.teletalk.jserver.spring;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;

import org.springframework.beans.BeansException;
import org.springframework.beans.MutablePropertyValues;
import org.springframework.beans.PropertyValue;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.beans.factory.support.AbstractBeanDefinition;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.NoSuchMessageException;
import org.springframework.context.support.FileSystemXmlApplicationContext;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.io.Resource;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.JServerConstants;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.event.StatusEvent;
import com.teletalk.jserver.event.StatusEventListener;
import com.teletalk.jserver.property.StringProperty;

/**
 * Spring application context subcomponent wrapper class. This component contains a property (<code>applicationContextPath</code>) 
 * for the path of the spring application context file, default spring.xml.<br>
 * <br>
 * SubComponent subclasses defined in the application context will be given the name of the bean (id or name) and registered under 
 * the top subsystem (JServer). If however the name of the bean is of a dot-separated format, SpringApplicationContext will attempt for find  
 * a matching name chain in the server and add the component under the appropriate parent. Components will be engaged automatically 
 * after creation.<br>
 * <br>
 * All core systems in JServer will be made available as beans in a parent context of the context created by this component. See 
 * the alias constants in {@link JServerConstants} for information about the names under which the core componente are registered.  
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1
 */
public class SpringApplicationContext extends SubComponent implements StatusEventListener
{
   /** The name of the (Spring) property to be used to indicate if a component is to be started when created (when added). */
   public static final String ENGAGE_ON_CREATION_PROPERTY = "engageOnCreation"; 
   
   
   /**
    * Custom implemetation of FileSystemXmlApplicationContext to intercept onRefresh().
    */
   private static class CustomFileSystemXmlApplicationContext extends FileSystemXmlApplicationContext
   {
      private final SpringApplicationContext springApplicationContext;
      
      public CustomFileSystemXmlApplicationContext(SpringApplicationContext springApplicationContext, String[] configLocations, boolean refresh, ApplicationContext parent)
      {
         super(configLocations, refresh, parent);
         
         this.springApplicationContext = springApplicationContext;
      }
      
      protected void onRefresh() throws BeansException
      {
         this.springApplicationContext.doOnRefresh();
      }
   }
   
   
   /**
    * This class is used to initialize and start subsysem/subcomponent classes in the spring application context. 
    */
   private static class InternalBeanFactoryPostProcessor implements BeanFactoryPostProcessor, BeanPostProcessor
   {
      private final SpringApplicationContext springApplicationContext;
      
      
      /**
       * Creates a new InternalBeanFactoryPostProcessor.
       */
      public InternalBeanFactoryPostProcessor(SpringApplicationContext springApplicationContext)
      {
         this.springApplicationContext = springApplicationContext;
      }
      
      /**
       */
      public Object postProcessAfterInitialization(final Object bean, final String beanName) throws BeansException
      {
         return bean;
      }
      
      /**
       * Processes a bean before initialized. This implementation initializes any SubComponentBeanProxy associated with the bean. 
       */
      public Object postProcessBeforeInitialization(final Object bean, final String beanName) throws BeansException
      {
         // Check if a SubComponentBeanProxy is registered for the specified bean...
         SubComponentBeanProxy subComponentBeanProxy = (SubComponentBeanProxy)this.springApplicationContext.subComponentBeanProxies.get(beanName);
         if( subComponentBeanProxy != null )
         {
            // ...and if so - set the bean in the proxy
            subComponentBeanProxy.setBean(bean);
         }

         return bean;
      }
        
      public void postProcessBeanFactory(final ConfigurableListableBeanFactory beanFactory) throws BeansException
      {
         beanFactory.addBeanPostProcessor(this);
      }
   }
   
   
   private final StringProperty applicationContextPath;
   
   private FileSystemXmlApplicationContext applicationContext;
   
   final HashMap subComponentBeanProxies;
   
   private final HashSet engageOnCreationBeans = new HashSet();
   
   private String[] subComponentBeanNames;
   
   
   /**
    * Creates a new SpringApplicationContext.
    */
   public SpringApplicationContext(final SubComponent parent)
   {
      super(parent, "SpringApplicationContext");
      
      this.applicationContextPath = new StringProperty(this, "applicationContextPath", "spring.xml", StringProperty.MODIFIABLE_NO_RESTART);
      this.applicationContextPath.setDescription("The path of the Spring XML application context bean definition file to be used.");
      super.addProperty(this.applicationContextPath);
      
      this.applicationContext = null;
      
      this.subComponentBeanProxies = new HashMap();
   }
   
   /**
    * Initializes this SpringApplicationContext.
    */
   protected void doInitialize()
   {
      super.doInitialize();
      
      // Register a status event listener on JServer to receive a notification when it is enabled. The reason of this is 
      // to delay intialization of the application context util any dynamically added SubComponentBeanProxy objects have 
      // been added.
      JServer jserver = JServer.getJServer();
      if( (jserver != null) && (jserver.getStatus() != JServerConstants.ENABLED) && (super.getEventQueue() != null) )
      {
         super.getEventQueue().registerStatusEventListener(this);
      }
      else this.initApplicationContext();
   }
   
   /**
    * Shuts down this SpringApplicationContext.
    */
   protected void doShutDown()
   {
      super.doShutDown();
   }
   
   /**
    * Initializes the application context.
    */
   protected void initApplicationContext()
   {
      try
      {
         InternalBeanFactoryPostProcessor internalBeanFactoryPostProcessor = new InternalBeanFactoryPostProcessor(this);
         if( this.applicationContext == null )
         {
            JServer jserver = JServer.getJServer();
            GenericApplicationContext genericApplicationContext = null;
            if( jserver != null )
            {
               // Create a parent application context containing all core systems
               genericApplicationContext = new GenericApplicationContext();
               for(int i=0; i<JServerConstants.coreComponentNames.length; i++)
               {
                  Object coreComponent = jserver.getCoreComponent(JServerConstants.coreComponentNames[i]);
                  if( coreComponent != null ) genericApplicationContext.getBeanFactory().registerSingleton(JServerConstants.coreComponentNames[i], coreComponent);
               }
               genericApplicationContext.refresh();
            }
            
            this.applicationContext = new CustomFileSystemXmlApplicationContext(this, new String[]{this.getApplicationContextPath()}, false, genericApplicationContext);
            this.applicationContext.addBeanFactoryPostProcessor(internalBeanFactoryPostProcessor);
         }
         this.applicationContext.refresh();
         
         
         String[] subComponentBeanNames = this.subComponentBeanNames; //internalBeanFactoryPostProcessor.getSubComponentBeanNames();
         if( (subComponentBeanNames != null) && (subComponentBeanNames.length > 0) )
         {
            SubComponent subComponent;
            
            if( this.isDebugMode() ) this.logDebug("Engaging SubComponents beans.");
                        
            // ### Engage subcomponents
            for (int i = 0; i < subComponentBeanNames.length; i++)
            {
               subComponent = ((SubComponent)this.getBean(subComponentBeanNames[i]));
               if( (subComponent != null) && !subComponent.isEnabled() && !subComponent.isInitializing() )
               {
                  if( this.isDebugMode() ) this.logDebug("Engaging " + subComponent + ".");
                  subComponent.engage();
                  try{
                  subComponent.waitForEnabled(100); // Wait a little while for the component to engage
                  }catch(Exception e){}
               }
            }
         }
      }
      catch(Throwable t)
      {
         logError("Error initializing application context!", t);
         super.error(t.toString());
      }
   }
   
   /**
    * Processes the beans before instantiation as occurred.
    */
   protected void doOnRefresh() throws BeansException
   {
      if( this.isDebugMode() ) this.logDebug("Post processing bean factory.");
      
      final ConfigurableListableBeanFactory beanFactory = this.applicationContext.getBeanFactory();
      
      JServer jserver = JServer.getJServer();
      if( jserver != null )
      {
         ArrayList subComponentBeanNamsList = new ArrayList();
         
         String[] beanNames = beanFactory.getBeanDefinitionNames();
         BeanDefinition beanDef;
         boolean isSubComponent;
         
         for (int i = 0; i < beanNames.length; i++)
         {
            try
            {
               beanDef = beanFactory.getBeanDefinition(beanNames[i]);
               
               // Check if this bean definition is a subcomponent...
               if ((beanDef != null) && (beanDef instanceof AbstractBeanDefinition))
               {
                  AbstractBeanDefinition abstractBeanDefinition = (AbstractBeanDefinition) beanDef;
                  Class beanClass = Class.forName(abstractBeanDefinition.getBeanClassName());
                  isSubComponent = SubComponent.class.isAssignableFrom(beanClass);
                  
                  if (isSubComponent)
                  {
                     if (EngageOnCreationBean.class.isAssignableFrom(beanClass))
                     {
                        this.engageOnCreationBeans.add(beanNames[i]);
                     }
                     else
                     {
                        String initMethodName = abstractBeanDefinition.getInitMethodName();
                        // ... Or - check if the init method is set to engage or engageOnCreation
                        if ("engage".equals(initMethodName) || ENGAGE_ON_CREATION_PROPERTY.equals(initMethodName))
                        {
                           if (this.isDebugMode())
                           {
                              this.logDebug("Found init method '" + initMethodName + "' for component " + beanNames[i] + ".");
                           }

                           this.engageOnCreationBeans.add(beanNames[i]);
                           abstractBeanDefinition.setInitMethodName(null);
                        }
                     }

                     subComponentBeanNamsList.add(beanNames[i]);
                  }

                  if (beanDef.getPropertyValues() != null)
                  {
                     if (isSubComponent) this.checkEngageOnCreation(beanDef, beanNames[i]);
                  }
               }
            }
            catch (ClassNotFoundException cnfe)
            {
               throw new BeanInitializationException("Unable to find class for bean " + beanNames[i] + "!", cnfe);
            }
         }
         
         this.subComponentBeanNames = (String[])subComponentBeanNamsList.toArray(new String[0]);
         Arrays.sort(subComponentBeanNames); // Sort names to get correct tree structure for creation/initialization (when . separated names are used)

         // Register SubComponent beans as components in the JServer
         for (int i = 0; i < this.subComponentBeanNames.length; i++)
         {
            final Object bean = beanFactory.getBean(this.subComponentBeanNames[i]);
            this.registerSubComponent(jserver, (SubComponent)bean, this.subComponentBeanNames[i], engageOnCreationBeans.contains(this.subComponentBeanNames[i]));
         }
         
         if( this.isDebugMode() ) this.logDebug(subComponentBeanNames.length + " SubComponents found in spring configuration file. Preparing to initialize.");
      }
   }
   
   /**
    * Checks if the a subcomponent bean should be engaged on creation.
    */
   protected void checkEngageOnCreation(final BeanDefinition beanDef, final String beanName)
   {
      MutablePropertyValues mutablePropertyValues = beanDef.getPropertyValues();
      
      if( mutablePropertyValues != null )
      {
         // ### Get SubComponent bean definitions and check for ENGAGE_ON_CREATION_PROPERTY. If the 
         //  ENGAGE_ON_CREATION_PROPERTY is found, the component will be instantiated and started at once.
         PropertyValue propertyValue = mutablePropertyValues.getPropertyValue(ENGAGE_ON_CREATION_PROPERTY);
         if( propertyValue != null )
         {
            if( this.isDebugMode() ) this.logDebug("Found property '" + ENGAGE_ON_CREATION_PROPERTY + "' for component " + beanName + ".");
            
            // Add name to engageOnCreationBeans set if engageOnCreation property is set to true...
            try{
               if( Boolean.valueOf(String.valueOf(propertyValue.getValue())).booleanValue() ) this.engageOnCreationBeans.add(beanName);
               else this.engageOnCreationBeans.remove(beanName); // Make is possible to override interface EngageOnCreationBean
            }
            catch(Throwable e)
            {
               this.logWarning("Invalid value for property " + ENGAGE_ON_CREATION_PROPERTY + " on bean " + beanName + ": " + propertyValue.getValue() + ".");
            }
            beanDef.getPropertyValues().removePropertyValue(propertyValue); // ...and remove property definition
         }
      }
   }
         
   /**
    * Registers a SubComponent bean in the JServer hierarchy.
    */
   protected void registerSubComponent(final JServer jserver, final SubComponent component, final String beanName, final boolean engageOnCreation) throws BeansException
   {
      if( component != null )
      {
         if( this.isDebugMode() )
         {
            if( engageOnCreation ) this.logDebug("Registering and starting component " + beanName + ", class: " + component.getClass().getName() + ".");
            else this.logDebug("Registering component " + beanName + ", class: " + component.getClass().getName() + ".");
         }
         
         SubComponent parent = null;
         String[] nameComponents; // i.e. mupp.mupp.mupp.....
             
         nameComponents = JServer.parseNames(beanName);
         String componentName = null;
         if( (nameComponents != null) && (nameComponents.length > 0) ) componentName = nameComponents[nameComponents.length-1]; // Name of component is last element
         
         if( componentName == null )
         {
            this.logError("Error while registering component - Unable to parse component name for bean " + beanName + "!");
         }
         else if( component instanceof SubComponentBeanProxy ) // SubComponentBeanProxy created as bean in application context...
         {
            if( component.getParent() != this )
            {
               if( this.isDebugMode() ) this.logDebug("Adding SubComponentBeanProxy " + beanName + " to " + this.getFullName() + ".");
               this.addSubComponent(component, componentName, engageOnCreation);
            }
         }
         else if( component.getParent() == null ) // Component not added...
         {
            if( jserver.findSubComponent(nameComponents) == null ) // Check that component doesn't already exists
            {
               if( nameComponents.length == 1 ) // If only a single name has been specified (i.e. no dot-separated name)...
               {
                  parent = jserver; // ...use JServer as parent component
               }
               else
               {
                  // Find parent
                  parent = jserver.findSubComponent(nameComponents, nameComponents.length-1);
                  if( (parent == null) && !(JSERVER_TOP_SYSTEM_ALIAS.equals(nameComponents[0]) || jserver.getName().equals(nameComponents[0])) )
                  {
                     String[] newNameComponents = new String[nameComponents.length + 1];
                     System.arraycopy(nameComponents, 0, newNameComponents, 1, nameComponents.length);
                     newNameComponents[0] = JSERVER_TOP_SYSTEM_ALIAS;
                     nameComponents = newNameComponents;
                     
                     parent = jserver.findSubComponent(nameComponents, nameComponents.length-1);
                  }
               }
               
               // Add sub component (and rename to bean name)
               if( parent != null )
               {
                  if( this.isDebugMode() ) this.logDebug("Adding component " + componentName + " to " + parent.getFullName() + ".");
                  parent.addSubComponent(component, componentName, engageOnCreation);
               }
               else this.logError("Error while registering component - unable to find parent component for '" + beanName + "'!");
            }
            else this.logError("Error while registering components - component '" + beanName + "' already exists!");
         }
         else // Component already added - rename component
         {
            if( this.isDebugMode() ) this.logDebug("Renaming component from " + component.getFullName() + " to " + beanName + ".");
            component.rename(componentName, true);
         }
      }
   }
   
   /**
    * Notification of a status modification. This implementation calls {@link #initApplicationContext()} to initialize the 
    * application context when an event is received indicating that the JServer is put in the state ENABLED, i.e. when all initialization 
    * is completed (including initialization of dynamic components such as {@link SubComponentBeanProxy} objets). 
    */
   public void statusChanged(final StatusEvent e)
   {
      JServer jserver = JServer.getJServer();
      if( (jserver != null) && (e.getSource() == jserver) && (e.getStatus() == JServerConstants.ENABLED) )
      { 
         this.initApplicationContext();
      }
   }

   /**
    * Called when a new child SubComponent or SubSystem is added to this component. 
    * This implementation registeres dynamically added {@link SubComponentBeanProxy} objects.
    */
   protected void subComponentAdded(final SubComponent component)
   {
      if( component instanceof SubComponentBeanProxy )
      {
         if( !component.isEnabled() ) component.engage();
         this.subComponentBeanProxies.put(((SubComponentBeanProxy)component).getBeanName(), component);
      }
   }
   
   /**
    * Gets the application context path.
    */
   public String getApplicationContextPath()
   {
      return applicationContextPath.stringValue();
   }
   
   /**
    * Sets the application context path.
    */
   public void setApplicationContextPath(String applicationContextPath)
   {
      this.applicationContextPath.setValue(applicationContextPath);
   }

   /**
    * Gets the Spring ApplicationContext.
    */
   public ApplicationContext getApplicationContext()
   {
      return applicationContext;
   }
   
   
   /* ### DELEGATE METHODS ### */
   
   
   public boolean containsBean(String name)
   {
      return applicationContext.containsBean(name);
   }
   public boolean containsBeanDefinition(String beanName)
   {
      return applicationContext.containsBeanDefinition(beanName);
   }
   public String[] getAliases(String name) throws NoSuchBeanDefinitionException
   {
      return applicationContext.getAliases(name);
   }
   public Object getBean(String name) throws BeansException
   {
      return applicationContext.getBean(name);
   }
   public Object getBean(String name, Class requiredType) throws BeansException
   {
      return applicationContext.getBean(name, requiredType);
   }
   public int getBeanDefinitionCount()
   {
      return applicationContext.getBeanDefinitionCount();
   }
   public String[] getBeanDefinitionNames()
   {
      return applicationContext.getBeanDefinitionNames();
   }
   /*public String[] getBeanDefinitionNames(Class type)
   {
      return applicationContext.getBeanDefinitionNames(type);
   }*/
   public Map getBeansOfType(Class type, boolean includePrototypes, boolean includeFactoryBeans) throws BeansException
   {
      return applicationContext.getBeansOfType(type, includePrototypes, includeFactoryBeans);
   }
   public String getDisplayName()
   {
      return applicationContext.getDisplayName();
   }
   public String getMessage(String code, Object[] args, String defaultMessage, Locale locale)
   {
      return applicationContext.getMessage(code, args, defaultMessage, locale);
   }
   public String getMessage(String code, Object[] args, Locale locale) throws NoSuchMessageException
   {
      return applicationContext.getMessage(code, args, locale);
   }
   public String getMessage(MessageSourceResolvable resolvable, Locale locale) throws NoSuchMessageException
   {
      return applicationContext.getMessage(resolvable, locale);
   }
   public BeanFactory getParentBeanFactory()
   {
      return applicationContext.getParentBeanFactory();
   }
   public Resource getResource(String location)
   {
      return applicationContext.getResource(location);
   }
   public long getStartupDate()
   {
      return applicationContext.getStartupDate();
   }
   public boolean isSingleton(String name) throws NoSuchBeanDefinitionException
   {
      return applicationContext.isSingleton(name);
   }
   public void publishEvent(ApplicationEvent event)
   {
      applicationContext.publishEvent(event);
   }
}
