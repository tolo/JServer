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
package com.teletalk.jserver.tcp.messaging.ejb;

import javax.naming.InitialContext;

import com.teletalk.jserver.StatusTransitionException;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.property.StringProperty;
import com.teletalk.jserver.tcp.messaging.Message;
import com.teletalk.jserver.tcp.messaging.MessageReceiverComponent;

/**
 * Message receiver component implementation that relays RPC calls to an EJB.
 * 
 * @author Tobias Löfstrand
 *  
 * @since 2.1 (20061215)
 */
public class EjbMessageReceiverComponent extends MessageReceiverComponent
{
   private final StringProperty ejbJndiName;
   
   private volatile boolean ejbTargetInitialized = false;
   
      
   /**
    * Creates a new EjbMessageReceiverComponent.
    * 
    * @since 2.1 (20050422)
    */
   public EjbMessageReceiverComponent()
   {
      this(null, "EjbMessageReceiverComponent", null);
   }
   
   /**
    * Creates a new EjbMessageReceiverComponent.
    * 
    * @param name the name which this message handler will be associated with. 
    */
   public EjbMessageReceiverComponent(final String name)
   {
      this(null, name, name);
   }
   
   /**
    * Creates a new EjbMessageReceiverComponent.
    * 
    * @param componentName the name of this component. 
    * @param receiverName the named receiver name which this EjbMessageReceiverComponent will be associated with.
    */
   public EjbMessageReceiverComponent(final String componentName, final String receiverName)
   {
      this(null, componentName, receiverName);
   }
   
   /**
    * Creates a new EjbMessageReceiverComponent.
    * 
    * @param parent the parent component of this EjbMessageReceiverComponent. This parameter may be null.
    * @param componentName the name of this component. 
    * @param receiverName the named receiver name which this EjbMessageReceiverComponent will be associated with.
    */
   public EjbMessageReceiverComponent(final SubComponent parent, final String componentName, final String receiverName)
   {
      this(parent, componentName, receiverName, false);
   }

   /**
    * Creates a new EjbMessageReceiverComponent.
    * 
    * @param parent the parent component of this EjbMessageReceiverComponent. This parameter may be null.
    * @param componentName the name of this component. 
    * @param receiverName the named receiver name which this EjbMessageReceiverComponent will be associated with.
    * @param accessControlCheckEnabled default value of accessControlCheckEnabled property.
    */
   public EjbMessageReceiverComponent(final SubComponent parent, final String componentName, final String receiverName, final boolean accessControlCheckEnabled)
   {
      super(parent, componentName, receiverName, accessControlCheckEnabled);
      
      this.ejbJndiName = new StringProperty(this, "ejbJndiName", "", StringProperty.MODIFIABLE_NO_RESTART);
      this.ejbJndiName.setDescription("The JNDI name of the EJB that this component will relay calls to.");
      super.addProperty(this.ejbJndiName);
   }
   
   /**
    * Called when a property is modified.
    */
   public void propertyModified(Property property)
   {
      if( property == this.ejbJndiName )
      {
         this.ejbTargetInitialized = false; // Reset initialization flag
      }
         
      super.propertyModified(property);
   }
   
   /**
    * Initializes the EJB target by performing an JNDI lookup.
    */
   private synchronized void initEJB()
   {
      try
      {
         if( super.isDebugEnabled() ) logDebug("Initializing default rpc handler - looking up EJB with JNDI name " + this.ejbJndiName.stringValue() + ".");
         
         InitialContext ctx = new InitialContext();
         super.rpcHandler.setDefaultHandler(ctx.lookup(this.ejbJndiName.stringValue()));
         this.ejbTargetInitialized = true;
         
         if( super.isDebugEnabled() ) logDebug("Default rpc handler initialized.");
      }
      catch (Exception e) 
      {
         logError("Error resolving EJB with JNDI name " + this.ejbJndiName.stringValue() + "!", e);
         throw new StatusTransitionException("Error resolving EJB with JNDI name " + this.ejbJndiName.stringValue() + " - " + e + "!");
      }
   }
   
   /**
    * Called when a message is received. This method initializes the EJB target, if not already initialized, before processing the message. 
    */
   public void messageReceived(final Message message)
   {
      if( !ejbTargetInitialized )
      {
         this.initEJB();
      }
      super.messageReceived(message);
   }
}
