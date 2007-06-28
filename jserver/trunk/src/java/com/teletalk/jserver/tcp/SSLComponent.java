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
package com.teletalk.jserver.tcp;

import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.Provider;
import java.security.Security;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

import com.teletalk.jserver.StatusTransitionException;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.property.BooleanProperty;
import com.teletalk.jserver.property.StringProperty;

/**
 * Sub component class used for setting up an SSLContext based on configuration data specified though 
 * the properties of this class.  
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.1 build 690
 */
public class SSLComponent extends SubComponent
{
   private final StringProperty securityProtocol; 
   
   private final StringProperty securityProviderClass; // instead of securityProviderName
   
   private final StringProperty securityProviderName; // instead of securityProviderClass
   
   
   private final StringProperty keyManagerFactoryAlgorithmName;
   
   private final StringProperty keyStoreType;
   
   private final StringProperty keyStoreName;
   
   private final StringProperty keyStorePassword;
   
   private final StringProperty keyManagerClass;
   
   
   private final StringProperty trustManagerFactoryAlgorithmName;
   
   private final StringProperty trustStoreType;
   
   private final StringProperty trustStoreName;
   
   private final StringProperty trustStorePassword;
   
   private final StringProperty trustManagerClass;
   
   private final BooleanProperty useNaiveTrustManager;
   
   
   private SSLContext context;
   
   /**
    * Creates a new SSLComponent.
    * 
    * @param name the name of this SSLComponent
    */
   public SSLComponent(String name)
   {
      this(null, name);
   }   
   
   /**
    * Creates a new SSLComponent.
    * 
    * @param parent the parent SSLComponent
    * @param name the name of this SSLComponent
    */
   public SSLComponent(SubComponent parent, String name)
   {
      super(parent, name);
 
      this.securityProtocol = new StringProperty(this, "securityProtocol", "", StringProperty.MODIFIABLE_OWNER_RESTART);
      this.securityProtocol.setDescription("The name of the security protocol to use, for instance SSL or TLS.");
      super.addProperty(this.securityProtocol);
      this.securityProviderClass = new StringProperty(this, "securityProviderClass", "", StringProperty.MODIFIABLE_OWNER_RESTART);
      this.securityProviderClass.setDescription("The fully qualified name of an security provider class. Note that his property, if specified, takes precedence over the property securityProviderName. This property is optional.");
      super.addProperty(this.securityProviderClass);
      this.securityProviderName = new StringProperty(this, "securityProviderName", "", StringProperty.MODIFIABLE_OWNER_RESTART);
      this.securityProviderName.setDescription("The name of the security provider to use. If not specified, the default (or other matching provider) is used. This property is only checked if securityProviderClass is not set.");
      super.addProperty(this.securityProviderName);
      
      this.keyManagerFactoryAlgorithmName = new StringProperty(this, "keyManagerFactoryAlgorithmName", "", StringProperty.MODIFIABLE_OWNER_RESTART);
      this.keyManagerFactoryAlgorithmName.setDescription("The name of the key management algorithm to be used by the key manager. If not set, the default algorithm will be used.");
      super.addProperty(this.keyManagerFactoryAlgorithmName);
      this.keyStoreType = new StringProperty(this, "keyStoreType", "", StringProperty.MODIFIABLE_OWNER_RESTART);
      this.keyStoreType.setDescription("The type of key store to use, for instance jks or pkcs12. If not specified, the default type will be used.");
      super.addProperty(this.keyStoreType);
      this.keyStoreName = new StringProperty(this, "keyStoreName", "", StringProperty.MODIFIABLE_OWNER_RESTART);
      this.keyStoreName.setDescription("The name (file name) of the key store to be used. This property must be specified if a key manager is to be used.");
      super.addProperty(this.keyStoreName);
      this.keyStorePassword = new StringProperty(this, "keyStorePassword", "", StringProperty.MODIFIABLE_OWNER_RESTART);
      this.keyStorePassword.setDescription("The password set for the key store.");
      super.addProperty(this.keyStorePassword);
      this.keyManagerClass = new StringProperty(this, "keyManagerClass", "", StringProperty.MODIFIABLE_OWNER_RESTART);
      this.keyManagerClass.setDescription("The fully qualified name of a class to be used as a key manager.");
      super.addProperty(this.keyManagerClass);
      
      this.trustManagerFactoryAlgorithmName = new StringProperty(this, "trustManagerFactoryAlgorithmName", "", StringProperty.MODIFIABLE_OWNER_RESTART);
      this.trustManagerFactoryAlgorithmName.setDescription("The name of the trust management algorithm to be used by the trust manager. If not set, the default algorithm will be used.");
      super.addProperty(this.trustManagerFactoryAlgorithmName);
      this.trustStoreType = new StringProperty(this, "trustStoreType", "", StringProperty.MODIFIABLE_OWNER_RESTART);
      this.trustStoreType.setDescription("The type of trust store to use, for instance jks or pkcs12. If not specified, the default type will be used.");
      super.addProperty(this.trustStoreType);
      this.trustStoreName = new StringProperty(this, "trustStoreName", "", StringProperty.MODIFIABLE_OWNER_RESTART);
      this.trustStoreName.setDescription("The name (file name) of the trust store to be used. This property must be specified if a trust manager is to be used.");
      super.addProperty(this.trustStoreName);
      this.trustStorePassword = new StringProperty(this, "trustStorePassword", "", StringProperty.MODIFIABLE_OWNER_RESTART);
      this.trustStorePassword.setDescription("The password set for the trust store.");
      super.addProperty(this.trustStorePassword);      
      this.trustManagerClass = new StringProperty(this, "trustManagerClass", "", StringProperty.MODIFIABLE_OWNER_RESTART);
      this.trustManagerClass.setDescription("The fully qualified name of a class to be used as a trust manager.");
      super.addProperty(this.trustManagerClass);
      this.useNaiveTrustManager = new BooleanProperty(this, "useNaiveTrustManager", false, BooleanProperty.MODIFIABLE_OWNER_RESTART);
      this.useNaiveTrustManager.setDescription("Boolean value indicating if a naive trust manager is to be used, i.e. a trust manager that allows everything. If this property is set to true, all other trust manager related properties are disregarded.");
      super.addProperty(this.useNaiveTrustManager);
   }
   
   /**
    * Enables this SSLComponent and initiates an SSLContext based on the property values of this object.
    */
   protected void doInitialize()
   {
      super.doInitialize();
      
      String securityProtocolStr = this.getSecurityProtocol();
      if( securityProtocolStr != null ) securityProtocolStr = securityProtocolStr.trim(); 
      
      if( (securityProtocolStr != null) && (securityProtocolStr.length() > 0) )
      {

         try
         {
            // PROVIDER & CONTEXT INSTANCE
            
            String securityProviderClassStr = this.getSecurityProviderClass();
            if( securityProviderClassStr != null ) securityProviderClassStr = securityProviderClassStr.trim();
            
            String securityProviderNameStr = this.getSecurityProviderName();
            if( securityProviderNameStr != null ) securityProviderNameStr = securityProviderNameStr.trim();
            
            if( (securityProviderClassStr != null) && (securityProviderClassStr.length() > 0) )
            {
               Provider securityProvider = (Provider)Class.forName(securityProviderClassStr).newInstance();
               Security.addProvider(securityProvider); 
               
               context = SSLContext.getInstance(securityProtocolStr, securityProvider);
            }
            else if( (securityProviderNameStr != null) && (securityProviderNameStr.length() > 0) )
            {
               context = SSLContext.getInstance(securityProtocolStr, securityProviderNameStr);
            }
            else
            {
               context = SSLContext.getInstance(securityProtocolStr);
            }
            
            // KEY MANAGER
            KeyManagerFactory keyManagerFactory = null;
            KeyManager customKeyManager = null;
            KeyManager[] keyManagers = null;
            
            String keyManagerClassStr = this.getKeyManagerClass();
            if( keyManagerClassStr != null ) keyManagerClassStr = keyManagerClassStr.trim();
            
            if( (keyManagerClassStr != null) && (keyManagerClassStr.length() > 0) )
            {
               customKeyManager = (KeyManager)Class.forName(keyManagerClassStr).newInstance(); 
            }
                                    
            String keyManagerFactoryAlgorithmNameStr = this.getKeyManagerFactoryAlgorithmName();
            if( keyManagerFactoryAlgorithmNameStr != null ) keyManagerFactoryAlgorithmNameStr = keyManagerFactoryAlgorithmNameStr.trim();
            
            if( (keyManagerFactoryAlgorithmNameStr != null) && (keyManagerFactoryAlgorithmNameStr.length() > 0) )
            {
               keyManagerFactory = KeyManagerFactory.getInstance(keyManagerFactoryAlgorithmNameStr);
            }
            else if( customKeyManager == null )
            {
               keyManagerFactory = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            }
            else
            {
               keyManagers = new KeyManager[]{customKeyManager};
            }
            
            String keyStoreNameStr = this.getKeyStoreName();
            if( keyStoreNameStr != null ) keyStoreNameStr = keyStoreNameStr.trim();
               
            String keyStorePasswordStr = this.getKeyStorePassword();
            if( keyStorePasswordStr != null ) keyStorePasswordStr = keyStorePasswordStr.trim();
            
            if( (keyManagerFactory != null) && (keyStoreNameStr != null) && (keyStoreNameStr.length() > 0) )
            {
               KeyStore keyStore = null;
               
               String keyStoreTypeStr = this.getKeyStoreType();
               if( keyStoreTypeStr != null ) keyStoreTypeStr = keyStoreTypeStr.trim();
               
               if( (keyStoreTypeStr != null) && (keyStoreTypeStr.length() > 0) )
               {
                  keyStore = KeyStore.getInstance(keyStoreTypeStr);
               }
               else
               {
                  keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
               }
               
               keyStore.load(new FileInputStream(keyStoreNameStr), keyStorePasswordStr.toCharArray());
               
               keyManagerFactory.init(keyStore, keyStorePasswordStr.toCharArray());
               
               KeyManager[] factoryKeyManagers = keyManagerFactory.getKeyManagers();
               
               if( factoryKeyManagers != null )
               {
                  if( keyManagers != null )
                  {
                     KeyManager[] newKeyManagers = new KeyManager[keyManagers.length + factoryKeyManagers.length];
                     System.arraycopy(keyManagers, 0, newKeyManagers, 0, keyManagers.length);
                     System.arraycopy(factoryKeyManagers, 0, newKeyManagers, keyManagers.length, factoryKeyManagers.length);
                  }
                  else
                  {
                     keyManagers = factoryKeyManagers;
                  }
               }
            }
            
            // TRUST MANAGER
                     
            TrustManagerFactory trustManagerFactory = null;
            TrustManager customTrustManager = null;
            TrustManager[] trustManagers = null;            
            
            if( this.useNaiveTrustManager.booleanValue() )
            {
               trustManagers = new TrustManager[]{new NaiveX509TrustManager()}; 
            }
            else
            {
               String trustManagerClassStr = this.getTrustManagerClass();
               if( trustManagerClassStr != null ) trustManagerClassStr = trustManagerClassStr.trim();
               
               if( (trustManagerClassStr != null) && (trustManagerClassStr.length() > 0) )
               {
                  customTrustManager = (TrustManager)Class.forName(trustManagerClassStr).newInstance(); 
               }
                                       
               String trustManagerFactoryAlgorithmNameStr = this.getTrustManagerFactoryAlgorithmName();
               if( trustManagerFactoryAlgorithmNameStr != null ) trustManagerFactoryAlgorithmNameStr = trustManagerFactoryAlgorithmNameStr.trim();
               
               if( (trustManagerFactoryAlgorithmNameStr != null) && (trustManagerFactoryAlgorithmNameStr.length() > 0) )
               {
                  trustManagerFactory = TrustManagerFactory.getInstance(trustManagerFactoryAlgorithmNameStr);
               }
               else if( customTrustManager == null )
               {
                  trustManagerFactory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
               }
               else
               {
                  trustManagers = new TrustManager[]{customTrustManager};
               }
               
               String trustStoreNameStr = this.getTrustStoreName();
               if( trustStoreNameStr != null ) trustStoreNameStr = trustStoreNameStr.trim();
               
               String trustStorePasswordStr = this.getTrustStorePassword();
               if( trustStorePasswordStr != null ) trustStorePasswordStr = trustStorePasswordStr.trim();
               
               if( (trustManagerFactory != null) && (trustStoreNameStr != null) && (trustStoreNameStr.length() > 0) )
               {
                  KeyStore trustStore = null;
                  
                  String trustStoreTypeStr = this.getTrustStoreType();
                  if( trustStoreTypeStr != null ) trustStoreTypeStr = trustStoreTypeStr.trim();
                  
                  if( (trustStoreTypeStr != null) && (trustStoreTypeStr.length() > 0) )
                  {
                     trustStore = KeyStore.getInstance(trustStoreTypeStr);
                  }
                  else
                  {
                     trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
                  }
                  
                  trustStore.load(new FileInputStream(trustStoreNameStr), trustStorePasswordStr.toCharArray());
                  
                  trustManagerFactory.init(trustStore);
                  
                  TrustManager[] factoryTrustManagers = trustManagerFactory.getTrustManagers();
                  
                  if( factoryTrustManagers != null )
                  {
                     if( trustManagers != null )
                     {
                        TrustManager[] newTrustManagers = new TrustManager[trustManagers.length + factoryTrustManagers.length];
                        System.arraycopy(trustManagers, 0, newTrustManagers, 0, trustManagers.length);
                        System.arraycopy(factoryTrustManagers, 0, newTrustManagers, trustManagers.length, factoryTrustManagers.length);
                     }
                     else
                     {
                        trustManagers = factoryTrustManagers;
                     }
                  }
               }
            }

            // INIT CONTEXT
            
            this.context.init(keyManagers, trustManagers, null);
         }
         catch(Exception e)
         {
            throw new StatusTransitionException("Failed to initialize SSLContext!", e);
         }
      }
      else
      {
         this.context = null;
      }
   }
   
   /**
    * Disables this SSLComponent.
    */
   protected void doShutDown()
   {
      super.doShutDown();
      
      this.context = null;
   }
   
   /**
    * Gets the SSLContext created when this sub component was enabled.
    */
   public SSLContext getSSLContext()
   {
      return context;
   }
   
   /* ##### GETTERS/SETTERS FOR PROPERTIES ##### */
  
   /**
    * Gets the value of the security protocol property.
    */
   public String getSecurityProtocol()
   {
      return this.securityProtocol.stringValue();
   }

   /**
    * Sets the value of the security protocol property.
    */
   public void setSecurityProtocol(String securityProtocol)
   {
      this.securityProtocol.setValue(securityProtocol);
   }
   
   /**
    * Gets the value of the security provider class property. If this value is specified, the security provider name 
    * property will be disregarded.
    */
   public String getSecurityProviderClass()
   {
      return securityProviderClass.stringValue();
   }

   /**
    * Sets the value of the security provider class property. If this value is specified, the security provider name 
    * property will be disregarded.
    */
   public void setSecurityProviderClass(String securityProviderClass)
   {
      this.securityProviderClass.setValue(securityProviderClass);
   }

   /**
    * Gets the value of the security provider name property. If the security provider class is specified, this  
    * property will be disregarded.
    */
   public String getSecurityProviderName()
   {
      return securityProviderName.stringValue();
   }

   /**
    * Sets the value of the security provider name property. If the security provider class is specified, this  
    * property will be disregarded.
    */
   public void setSecurityProviderName(String securityProviderName)
   {
      this.securityProviderName.setValue(securityProviderName);
   }

   /**
    * Gets the value of the key manager factory algorithm property.
    */
   public String getKeyManagerFactoryAlgorithmName()
   {
      return keyManagerFactoryAlgorithmName.stringValue();
   }

   /**
    * Sets the value of the key manager factory algorithm property.
    */
   public void setKeyManagerFactoryAlgorithmName(String keyManagerFactoryAlgorithmName)
   {
      this.keyManagerFactoryAlgorithmName.setValue(keyManagerFactoryAlgorithmName);
   }

   /**
    * Gets the value of the key store type property.
    */
   public String getKeyStoreType()
   {
      return keyStoreType.stringValue();
   }

   /**
    * Sets the value of the key store type property.
    */
   public void setKeyStoreType(String keyStoreType)
   {
      this.keyStoreType.setValue(keyStoreType);
   }

   /**
    * Gets the value of the key store name property.
    */
   public String getKeyStoreName()
   {
      return keyStoreName.stringValue();
   }

   /**
    * Sets the value of the key store name property.
    */
   public void setKeyStoreName(String keyStoreName)
   {
      this.keyStoreName.setValue(keyStoreName);
   }

   /**
    * Gets the value of the key store password property.
    */
   public String getKeyStorePassword()
   {
      return keyStorePassword.stringValue();
   }

   /**
    * Sets the value of the key store password property.
    */
   public void setKeyStorePassword(String keyStorePassword)
   {
      this.keyStorePassword.setValue(keyStorePassword);
   }
   
   /**
    * Gets the value of the key manager class property.
    */
   public String getKeyManagerClass()
   {
      return keyManagerClass.stringValue();
   }

   /**
    * Sets the value of the key manager class property.
    */
   public void setKeyManagerClass(String keyManagerClass)
   {
      this.keyManagerClass.setValue(keyManagerClass);
   }

   /**
    * Gets the value of the trust manager factory algorithm property.
    */
   public String getTrustManagerFactoryAlgorithmName()
   {
      return trustManagerFactoryAlgorithmName.stringValue();
   }

   /**
    * Sets the value of the trust manager factory algorithm property.
    */
   public void setTrustManagerFactoryAlgorithmName(String trustManagerFactoryAlgorithmName)
   {
      this.trustManagerFactoryAlgorithmName.setValue(trustManagerFactoryAlgorithmName);
   }

   /**
    * Gets the value of the trust store type property.
    */
   public String getTrustStoreType()
   {
      return trustStoreType.stringValue();
   }

   /**
    * Sets the value of the trust store type property.
    */
   public void setTrustStoreType(String trustStoreType)
   {
      this.trustStoreType.setValue(trustStoreType);
   }

   /**
    * Gets the value of the trust store name property.
    */
   public String getTrustStoreName()
   {
      return trustStoreName.stringValue();
   }

   /**
    * Sets the value of the trust store name property.
    */
   public void setTrustStoreName(String trustStoreName)
   {
      this.trustStoreName.setValue(trustStoreName);
   }

   /**
    * Gets the value of the trust store password property.
    */
   public String getTrustStorePassword()
   {
      return trustStorePassword.stringValue();
   }

   /**
    * Sets the value of the trust store password property.
    */
   public void setTrustStorePassword(String trustStorePassword)
   {
      this.trustStorePassword.setValue(trustStorePassword);
   }
   
   /**
    * Gets the value of the trust manager class property.
    */
   public String getTrustManagerClass()
   {
      return trustManagerClass.stringValue();
   }

   /**
    * Sets the value of the trust manager class property.
    */
   public void setTrustManagerClass(String trustManagerClass)
   {
      this.trustManagerClass.setValue(trustManagerClass);
   }
   
   /**
    * Gets the value of the use naive trust manager property. If this property is set to true, a 
    * {@link NaiveX509TrustManager} will be created and used as TrustManager when creating an SSLContext.
    */
   public boolean getUseNaiveTrustManager()
   {
      return useNaiveTrustManager.booleanValue();
   }

   /**
    * Sets the value of the use naive trust manager property. If this property is set to true, a 
    * {@link NaiveX509TrustManager} will be created and used as TrustManager when creating an SSLContext.
    */
   public void setUseNaiveTrustManager(boolean useNaiveTrustManager)
   {
      this.useNaiveTrustManager.setValue(useNaiveTrustManager);
   }
}
