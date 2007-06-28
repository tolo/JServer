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

import java.beans.PropertyDescriptor;
import java.io.File;
import java.net.URL;
import java.util.HashSet;

import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;

import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.log.ManagedLoggerComponent;
import com.teletalk.jserver.property.BooleanProperty;
import com.teletalk.jserver.property.NumberProperty;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.property.StringProperty;

/**
 * SubComponent to be used as a proxy for a bean (typically contained in a Spring application context, although this is not necessary) 
 * and thus making it possible to expose the properties of that bean though JServer properties.<br>
 * <br>
 * The bean property types that may be controlled though this class are all primitive types and corresponding wrapper classes as well
 * as the following classes:<br>
 * <ul>
 *    <li>java.lang.String</li>
 *    <li>java.io.File</li>
 *    <li>java.net.URL</li>
 * </ul>
 * <br> 
 * <br>The bean reference is set by calling the method {@link #setBean(Object)}.
 * 
 * @since 2.1 (20050817)
 * 
 * @author Tobias Löfstrand
 */
public class SubComponentBeanProxy extends ManagedLoggerComponent
{
   private final HashSet beanPropertyNames;
   
   private BeanWrapper beanWrapper;
   
   
   private StringProperty beanName;
   
   private BooleanProperty useJServerPropertyFile;
   
   
   /**
    * Creates a SubComponentBeanProxy.
    */
   public SubComponentBeanProxy(String name)
   {
      this(null, name);
   }

   /**
    * Creates a SubComponentBeanProxy.
    */
   public SubComponentBeanProxy(SubComponent parent, String name)
   {
      super(parent, name);
      
      this.beanName = new StringProperty(this, "beanName", name, StringProperty.NOT_MODIFIABLE, true);
      super.addProperty(this.beanName);
      
      this.useJServerPropertyFile = new BooleanProperty(this, "useJServerPropertyFile", true, BooleanProperty.NOT_MODIFIABLE, true);
      super.addProperty(this.useJServerPropertyFile);
      
            
      this.beanPropertyNames = new HashSet();
   }
   
   /**
    */
   public String getBeanName()
   {
      return beanName.stringValue();
   }
   
   /**
    * Sets the bean for which this object it to be a proxy.
    */
   public void setBean(final Object bean)
   {
      if( this.beanWrapper != null) return; // Already set
      
      this.beanWrapper = new BeanWrapperImpl(bean);
                  
      PropertyDescriptor[] propertyDescriptors = this.beanWrapper.getPropertyDescriptors();
      String propertyName;
      Property property;
      Object value;
      Class valueClass;
            
      for(int i=0; i<propertyDescriptors.length; i++)
      {
         if(propertyDescriptors[i] != null)
         {
            propertyName = propertyDescriptors[i].getName();

            if( (propertyDescriptors[i].getReadMethod() != null) && (propertyDescriptors[i].getWriteMethod() != null) )
            {
               valueClass = this.beanWrapper.getPropertyType(propertyName);
               value = this.beanWrapper.getPropertyValue(propertyName);
               property = null;
                           
               if( (String.class.isAssignableFrom(valueClass)) || (File.class.isAssignableFrom(valueClass)) || (URL.class.isAssignableFrom(valueClass)) )
               {
                  String stringValue = null;
                  if( value != null ) stringValue = String.valueOf(value);
                  property = new StringProperty(this, propertyName, stringValue, StringProperty.MODIFIABLE_NO_RESTART, useJServerPropertyFile.booleanValue());
               }
               else if( Boolean.TYPE.isAssignableFrom(valueClass) || Boolean.class.isAssignableFrom(valueClass) )
               {
                  boolean booleanValue = false;
                  if( value != null ) booleanValue = ((Boolean)value).booleanValue();
                  property = new BooleanProperty(this, propertyName, booleanValue, BooleanProperty.MODIFIABLE_NO_RESTART, useJServerPropertyFile.booleanValue());
               }
               else if( Byte.TYPE.isAssignableFrom(valueClass) || Short.TYPE.isAssignableFrom(valueClass) || Integer.TYPE.isAssignableFrom(valueClass) || Long.TYPE.isAssignableFrom(valueClass) || 
                     Byte.class.isAssignableFrom(valueClass) || Short.class.isAssignableFrom(valueClass) || Integer.class.isAssignableFrom(valueClass) || Long.class.isAssignableFrom(valueClass) )
               {
                  long longValue = 0;
                  if( value != null ) longValue = ((Number)value).longValue();
                  property = new NumberProperty(this, propertyName, longValue, NumberProperty.MODIFIABLE_NO_RESTART, useJServerPropertyFile.booleanValue());
               }
               else if( Float.TYPE.isAssignableFrom(valueClass) || Double.TYPE.isAssignableFrom(valueClass) ||  
                     Float.class.isAssignableFrom(valueClass) || Double.class.isAssignableFrom(valueClass) )
               {
                  double doubleValue = 0;
                  if( value != null ) doubleValue = ((Number)value).doubleValue();
                  property = new NumberProperty(this, propertyName, doubleValue, NumberProperty.MODIFIABLE_NO_RESTART, useJServerPropertyFile.booleanValue());
               }
               
               if( property != null )
               {
                  this.beanPropertyNames.add(propertyName);
                  super.addProperty(property);
                  super.initializeProperty(property);
               }
            }
         }
      }
   }
   
   /**
    * Called when a property has been initialized with a value from persistent storage.
    */
   public void propertyInitialized(Property property)
   {
      if( this.beanPropertyNames.contains(property.getName()) ) 
      {
         if( this.beanWrapper != null ) this.beanWrapper.setPropertyValue(property.getName(), property.getValueAsObject());
      }
      super.propertyInitialized(property);
   }

   /**
    * Called when the value of a property has been modified.
    */
   public void propertyModified(Property property)
   {
      if( this.beanPropertyNames.contains(property.getName()) ) 
      {
         if( this.beanWrapper != null ) this.beanWrapper.setPropertyValue(property.getName(), property.getValueAsObject());
      }
      super.propertyModified(property);
   }
}
