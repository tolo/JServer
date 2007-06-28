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
 TODO: DestinationMetaDataListener.
 TODO: Improve MetaDataValueMatcher mechanism.
 TODO: Clear named receiver field in header i dispatchMessageInternal?
 TODO: Prevent proxied messages from beeing send back to the sender (if the sender contains the same named receiver name as the message).
 */
package com.teletalk.jserver.tcp.messaging;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Level;

import com.teletalk.jserver.StatusTransitionException;
import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.load.LoadManager;
import com.teletalk.jserver.load.LoadValue;
import com.teletalk.jserver.net.acl.AccessControlListHandler;
import com.teletalk.jserver.net.sns.Service;
import com.teletalk.jserver.net.sns.client.ServiceListener;
import com.teletalk.jserver.net.sns.client.ServiceProvider;
import com.teletalk.jserver.net.sns.client.SnsClientManager;
import com.teletalk.jserver.pool.ThreadPool;
import com.teletalk.jserver.property.BooleanProperty;
import com.teletalk.jserver.property.MultiStringProperty;
import com.teletalk.jserver.property.NumberProperty;
import com.teletalk.jserver.property.Property;
import com.teletalk.jserver.statistics.StatisticsManager;
import com.teletalk.jserver.statistics.messaging.MessagingStatisticsSource;
import com.teletalk.jserver.tcp.TcpCommunicationManager;
import com.teletalk.jserver.tcp.TcpEndPoint;
import com.teletalk.jserver.tcp.TcpEndPointGroup;
import com.teletalk.jserver.tcp.TcpEndPointIdentifier;
import com.teletalk.jserver.tcp.messaging.admin.ServerAdministrationHandler;
import com.teletalk.jserver.tcp.messaging.command.MetaDataUpdateCommand;
import com.teletalk.jserver.tcp.messaging.rpc.MessagingRpcInterface;
import com.teletalk.jserver.util.StringUtils;

/**
 * The MessagingManager is the main class of the messaging framework, designed to facilitate message based communication
 * between two or more servers. This class supports sending and receving messages to remote messaging systems, both 
 * synchronously and asynchronously. Messages may basically be dispatched in four different ways: 
 * <ul>
 * <li>As a serialized object.</li>
 * <li>As a byte array.</li>
 * <li>By reading the message from an input stream.</li>
 * <li>As an remote procedure call.</li> 
 * </ul>
 * <br>
 * To this end there are different versions of the <code>dispatchMessage</code> and <code>dispatchMessageAsync</code>
 * methods available. When sending synchronous messages (dispatchMessage), the calling thread will be blocked until a
 * response is received or a timeout occurs. The timeout can be specified either in the metod call or through the 
 * property "<b>responseTimeout </b>" (which can be set using the method {@link #setResponseMessageTimeOut(long)}).
 * If a response is received in time, it will be returned in the return value of the method call as a {@link Message}
 * object. Otherwise an exception ({@link ResponseTimeOutException}if a timeout occurrs or
 * {@link MessageDispatchFailedException}if the message failed to dispatch) will be thrown.<br>
 * <br>
 * A more convenient way to dispatch messages is to use {@link MessageDispatcher message dispatchers}. Message dispatchers 
 * may be obtained through methods like {@link #getMessageDispatcher(Destination)} or {@link #getMessageDispatcher(String)}.<br>
 * <br>
 * The most high level way of implementing communication via the MessagingManager is to use RPC based communication. This is 
 * possible through the use of {@link MessagingRpcInterface} objects, obtainable through methods like {@link #getMessagingRpcInterface(Destination)} 
 * or {@link #getMessagingRpcInterface(String)}. MessagingRpcInterface also support the use of dynamic proxy classes 
 * (see {@link MessagingRpcInterface#createProxy(Class)} and similar methods) for an even more high level and transparant 
 * implementation.<br>
 * <br>
 * Each message that is sent is accompanied with a header, represented by the class {@link MessageHeader}. The fields
 * in this header that may be interesting to users are <b>message type </b> and <b>description </b>. <br>
 * Message type, set using the method {@link MessageHeader#setMessageType(int)}, is a user defined integer value
 * identifying the type of the message. This value is meant to be used as a means for receivers of messages to identify
 * the type of the received message before examining the body of the message. <br>
 * The desctiption field, set using the method {@link MessageHeader#setDescription(String)}, of MessageHeader is simply
 * a String used to facilitate human identification of the message and logging. <br>
 * <br>
 * Each remote messaging system is represented by the class {@link Destination}, which is a subclass of
 * {@link TcpEndPointGroup}. This class contains various meta data about the remote messaging system and keeps track on
 * all connections it. <br>
 * The MessagingManager can create one or more connections to each remote messaging system, determined by the property "
 * <b>maximum connections/destination </b>" which can be set by calling the method
 * {@link #setConnectionsPerDestination(int)}. MessagingManager will attempt to create connections to remote messaging
 * systems specified by the addresses in the property " <b>client side destinations </b>". Addresses can also be added
 * by using the method {@link #addDestination(TcpEndPointIdentifier)}.<br>
 * <br>
 * Server side addresses, i.e. the addresses on which the MessagingManager will listen for connect requests from other
 * messaging systems, are specified by the property " <b>server addresses </b>" inherited from TcpCommunicationManager.
 * New addresses can be added by using the method
 * {@link TcpCommunicationManager#addServerAddress(TcpEndPointIdentifier)}.<br>
 * <br>
 * For receiving incomming messages, {@link MessageReceiver message receivers} may be registered with a name in the 
 * MessagingManager, using for instance the methods {@link #registerMessageReceiver(MessageReceiver, String)} or 
 * {@link #registerMessageReceiverComponent(MessageReceiverComponent)}.
 * a {@link MessageReceiver}can be associated with a MessagingManager. When registering a message receiver in this fashion, it 
 * will be notified of all incomming messages bound for that receiver name.<br>
 * A default message receiver, that will receive all incomming messages not handled by message receivers registered with a name, 
 * may be registered using the method {@link #setDefaultMessageReceiver(MessageReceiver)}.<br>
 * <br>
 * To send a response to an incomming message, simply pass the header of that message as a parameter when calling one of
 * the dispatchMessage- or dispatchMessageAsync-methods (after modifying the message type and description fields in the
 * header). These methods will automatically set the " <b>response to </b>" field to the value of the <b>message id </b>
 * field, so that the response will be correctly associated with the original message in the sending messaging system.
 * <br>
 * <br>
 * Since version 2.0, this class contains a property called {@link #reportLoad}. This property indicates if a load
 * value should automatically be set for each thread handling an incomming message. If set to <code>true</code>, the
 * load value will be set to 1, using the method {@link com.teletalk.jserver.load.LoadValue#setThreadLoad(int)}, when
 * the message handling thread begins execution. When execution is completed, the load value will be reset using the
 * method {@link com.teletalk.jserver.load.LoadValue#resetThreadLoad()}.<br>
 * <br>
 * <br>
 * <span style="font-family:courier;font-size:14px"><b>Properties:</b><br></span>
 * <ul> 
 * <span style="font-family:courier;font-size:11px">
 * <li><b>remoteAddresses</b> - Addresses for the destinations that this MessagingManager should attempt to create connections to.</li>
 * <li><b>maximumConnections/destination</b> - The maximum number of client endpoints that are to be created for each destination.</li>
 * <li><b>responseTimeout</b> - Maximum time(ms) to wait for a response to a message sent to a remote messaging system.</li>
 * <li><b>asynchMessageDispatchTimeout</b> - Maximum time(ms) to wait for an asynchronous message to get dispatched.</li>
 * <li><b>messageReadTimeout</b> - The timeout used to detect stalled/aborted reading of incomming messages by a message receiver.</li>
 * <li><b>checkInteval</b> - The interval in milliseconds at which checks are performed on connections and meta data update commands are dispatched. Min value is > 500.</li>
 * <li><b>reportLoad</b> - Boolean value indicating if a load value should be should be set automatically for each thread handling an incomming message.</li>
 * <li><b>enableServerAdministration</b> - Boolean value indicating if remote server administration of this server should be enabled though this system.</li>
 * <li><b>statisticsEnabled</b> - Boolean value indicating if statistics should be enabled.</li>
 * <li><b>proxyingEnabled</b> - Boolean value indicating if proxying should be enabled.</li>
 * <li><b>remoteServiceNames</b> - The names of the services that this MessagingManager is to connect to. Addresses for the service names will be fetched from an SNS.</li>  
 * <li><b>localServiceNames</b> - The names of the services provided by this MessagingManager, used for registratration in an SNS.</li> 
 * </span>
 * </ul>
 * 
 * @since 1.2
 * 
 * @author Tobias Löfstrand
 */
public class MessagingManager extends AbstractMessagingManager implements ServiceListener, ServiceProvider
{
   /** The default number of connections per destination (2). */
   public static final int DEFAULT_CONNECTIONS_PER_DESTINATION = 2;

   /** The default timeout used when waiting for a response to a message (10000ms). */
   public static final int DEFAULT_RESPONSE_TIMEOUT = 10 * 1000;
   
   /** The default timeout used when waiting for a asynchronous message to be dispatched (2000ms). @since 2.1.2 (20060208) */
   public static final int DEFAULT_ASYNCH_MESSAGE_DISPATCH_TIMEOUT = 2 * 1000;

   /**
    * Meta data key for server load (fetched from {@link com.teletalk.jserver.load.LoadManager}). The value of this
    * field is <code>ServerLoad</code>.
    * 
    * @since 2.0, build 757.
    */
   public static final String SERVER_LOAD_METADATA_KEY = "ServerLoad";

   /**
    * Message header/meta data key used to store the name of a target named receiver or the names of available named
    * receivers.
    */
   public static final String NAMED_MESSAGE_RECEIVER_METADATA_KEY = "com.teletalk.jserver.tcp.messaging.NamedMessageReceiver";
   
   /**
    * Message header/meta data key used to to store the sender id of the sending/receiving server for proxied messages.
    * 
    * @since 2.0.3 (20050412)
    */
   public static final String PROXY_SENDER_ID_METADATA_KEY = "com.teletalk.jserver.tcp.messaging.proxy.senderId";
   
   /**
    * Message header/meta data key used to to store the message id set by the sending/receiving server for proxied messages.
    * 
    * @since 2.0.3 (20050412)
    */
   public static final String PROXY_MESSAGE_ID_METADATA_KEY = "com.teletalk.jserver.tcp.messaging.proxy.messageId";
   
   /**
    * Message header/meta data key used to to store the server name of the sending/receiving server for proxied messages.
    * 
    * @since 2.1.2 (20060323)
    */
   public static final String PROXY_SERVER_NAME_METADATA_KEY = "com.teletalk.jserver.tcp.messaging.proxy.serverName";
   
   /**
    * @since 2.1.1 (20060109)
    */
   public static final String PROXYING_ENABLED_METADATA_KEY = "com.teletalk.jserver.tcp.messaging.proxy.enabled";

   
   
   private static final ThreadLocal CurrentMessage = new ThreadLocal();
   
   private static final String STATISTICS_BASE_NAME = "Messaging statistics";
   
   
   private final ThreadLocal threadResponseMessageTimeout = new ThreadLocal();
       

   /* ### "HANDLERS" ### */
   /** The "strategy" for selecting destinations and endpoints for dispatching of messages. */
   private EndPointSelectionStrategy endPointSelectionStrategy;
   
   private MessageDispatchHandler messageDispatchHandler;
   
   private MessageReceiverHandler messageReceiverHandler;
   
   private MessageProcessor messageProcessor;
   
   /** @since 2.0.1 (20040924) */
   private ServerAdministrationHandler serverAdministrationHandler;
   
   /** @since 2.0.2 (20050331)  */
   private AccessControlListHandler accessControlListHandler = null;
   
   
   
   /** Meta data. */
   private HashMap messagingSystemMetaData;

   private boolean messagingSystemMetaDataDirtyFlag;
   
   
   /**
    * The {@link MessageReceiver} associated with this MessagingManager. When accessing this field a lock on
    * {@link #getMessageReceiverMonitor()}must be obtained.
    */
   protected MessageReceiver defaultMessageReceiver;

   
   /** @since 2.1.2 (20060228) */
   protected boolean useMessageHandlerPool = true;
   
   /** @since 2.1.2 (20060310) */
   protected boolean useFixedMessageHandlerPoolSize = false;
   
   /** The thread pool containing {@link MessageWorker}objects for handling of incomming messages. */
   protected ThreadPool messageHandlerPool;

   /** The initial size of the pool that holds the MessageWorker objects (Defaultvalue = 10). */
   private int messageHandlerPoolSize = 10;

   
   /** @since 1.3.1, build 670 */
   private int objectMessageBodyResetLimit = 0;
      
   /** @since 2.0.1 (20050215) */
   private boolean saveResponseMessageInCurrentThread = false;
   
   
   /** @since 2.0.1 (20050304) */
   private volatile MessagingStatisticsSource statistics = null;
   private String statisticsName = null;
   
   /** Cache of addresses for remote services.
    * @since 2.1 (20050517) */
   private final ArrayList remoteServiceAddresses;
   
   
   // ### MONITORS
   
   /**
    * Main monitor used by this system for access to named receivers and meta data
    */
   private final Object mainMonitor;
   
   /**
    * Monitor used by the thread of this system for waitning for the next check. This monitor may be used to wake up
    * that thread
    */
   private final Object checkThreadWaitMonitor;
   
   
   /* ### PROPERTIES BEGIN ### */
   
   /** Property for the maximum number of client endpoints that are to be created for each remote destination. */
   protected final NumberProperty connectionsPerDestination;

   /** Property for the maximum time(ms) to wait for a response to a message sent to a remote messaging system. */
   protected final NumberProperty responseMessageTimeOut;

   /** Property for the maximum time(ms) to wait for an asynchronous message to get dispatched. */
   protected final NumberProperty asynchMessageDispatchTimeOut;

   /** Property for the timeout used to detect stalled/aborted reading of incomming messages by a message receiver. */
   protected final NumberProperty messageReadTimeout;

   /**
    * Property for the interval in milliseconds at which checks are performed on connections and meta data update
    * commands are dispatched.
    */
   protected final NumberProperty checkInteval;

   /**
    * Flag indicating if a load value should be should be set automatically for each thread handling an incomming
    * message.
    * 
    * @since 2.0 Build 757
    */
   protected final BooleanProperty reportLoad;
   
   /**
    * Flag indicating if server administration should be enabled through a {@link ServerAdministrationHandler}.
    * 
    * @since 2.0.1 (20040925)
    */
   protected final BooleanProperty enableServerAdministration;
   
   /**
    * Flag indicating if statistics should be enabled for this MessagingManager.
    * 
    * @since 2.0.1 (20050304)
    */
   protected final BooleanProperty statisticsEnabled;
   
   /**
    * Flag indicating if proxying should be enabled for this MessagingManager, i.e. if this messaging manager 
    * may be used as a proxy for message receivers in all other connected messaging managers. If this flag is enabled, 
    * this messaging manager will route messages with no local matching message receiver to other connected 
    * message receivers when the requested message receiver exitst.
    * 
    * @since 2.0.1 (20050304)
    */
   protected final BooleanProperty proxyingEnabled;
   
   /**
    * The names of the services that this MessagingManager is to connect to. Addresses for the service 
    * names will be fetched from an SNS.
    * 
    * @since 2.1 (20050428)
    * 
    * @see com.teletalk.jserver.net.sns.client.SnsClientManager
    */
   protected final MultiStringProperty remoteServiceNames; 
   
   /**
    * The names of the services provided by this MessagingManager, used for registratration in an SNS.
    * 
    * @since 2.1 (20050428)
    * 
    * @see com.teletalk.jserver.net.sns.client.SnsClientManager
    */
   protected final MultiStringProperty localServiceNames; 
   
   protected final BooleanProperty exposeMessageReceiversAsServices;
   
   /**
    * @since 2.1.1 (20060109)
    */
   protected final BooleanProperty useProxiedMessageReceivers;
   
   
   /* ### PROPERTIES END ### */
   
   
   /**
    * Creates a MessagingManager without any client side or server side addresses specified. <br>
    * <br>
    * The created MessagingManager will be named "MessagingManager".
    *  
    * @since 2.1 (20050422)
    */
   public MessagingManager()
   {
      this(null, "MessagingManager", DEFAULT_CONNECTIONS_PER_DESTINATION, DEFAULT_RESPONSE_TIMEOUT);
   }

   /**
    * Creates a MessagingManager without any client side or server side addresses specified. <br>
    * <br>
    * The created MessagingManager will be named "MessagingManager".
    * 
    * @param parent the parent of this MessagingManager.
    */
   public MessagingManager(SubSystem parent)
   {
      this(parent, "MessagingManager", DEFAULT_CONNECTIONS_PER_DESTINATION, DEFAULT_RESPONSE_TIMEOUT);
   }

   /**
    * Creates a MessagingManager with a single client and server address specified. <br>
    * <br>
    * The created MessagingManager will be named "MessagingManager".
    * 
    * @param parent the parent of this MessagingManager.
    * @param clientAddress client side address (address to connect to). May be <code>null</code>.
    * @param serverAddress server side address (local address to listen on). May be <code>null</code>.
    */
   public MessagingManager(SubSystem parent, TcpEndPointIdentifier clientAddress, TcpEndPointIdentifier serverAddress)
   {
      this(parent, "MessagingManager", clientAddress, serverAddress, DEFAULT_CONNECTIONS_PER_DESTINATION,
            DEFAULT_RESPONSE_TIMEOUT);
   }

   /**
    * Creates a MessagingManager with a several client and server address specified. <br>
    * <br>
    * The created MessagingManager will be named "MessagingManager".
    * 
    * @param parent the parent of this MessagingManager.
    * @param clientAddresses client side addresses (addresses to connect to). May be <code>null</code>.
    * @param serverAddresses server side addresses (local addresses to listen on). May be <code>null</code>.
    */
   public MessagingManager(SubSystem parent, TcpEndPointIdentifier[] clientAddresses,
         TcpEndPointIdentifier[] serverAddresses)
   {
      this(parent, "MessagingManager", clientAddresses, serverAddresses, DEFAULT_CONNECTIONS_PER_DESTINATION,
            DEFAULT_RESPONSE_TIMEOUT);
   }
   
   /**
    * Creates a MessagingManager without any client side or server side addresses specified.
    * 
    * @param name the name that will be given to this MessagingManager.
    */
   public MessagingManager(String name)
   {
      this(null, name, DEFAULT_CONNECTIONS_PER_DESTINATION, DEFAULT_RESPONSE_TIMEOUT);
   }

   /**
    * Creates a MessagingManager without any client side or server side addresses specified.
    * 
    * @param parent the parent of this MessagingManager.
    * @param name the name that will be given to this MessagingManager.
    */
   public MessagingManager(SubSystem parent, String name)
   {
      this(parent, name, DEFAULT_CONNECTIONS_PER_DESTINATION, DEFAULT_RESPONSE_TIMEOUT);
   }

   /**
    * Creates a MessagingManager with a single client and server address specified.
    * 
    * @param parent the parent of this MessagingManager.
    * @param name the name that will be given to this MessagingManager.
    * @param clientAddress client side address (address to connect to). May be <code>null</code>.
    * @param serverAddress server side address (local address to listen on). May be <code>null</code>.
    */
   public MessagingManager(SubSystem parent, String name, TcpEndPointIdentifier clientAddress,
         TcpEndPointIdentifier serverAddress)
   {
      this(parent, name, clientAddress, serverAddress, DEFAULT_CONNECTIONS_PER_DESTINATION, DEFAULT_RESPONSE_TIMEOUT);
   }

   /**
    * Creates a MessagingManager with a several client and server address specified.
    * 
    * @param parent the parent of this MessagingManager.
    * @param name the name that will be given to this MessagingManager.
    * @param clientAddresses client side addresses (addresses to connect to). May be <code>null</code>.
    * @param serverAddresses server side addresses (local addresses to listen on). May be <code>null</code>.
    */
   public MessagingManager(SubSystem parent, String name, TcpEndPointIdentifier[] clientAddresses,
         TcpEndPointIdentifier[] serverAddresses)
   {
      this(parent, name, clientAddresses, serverAddresses, DEFAULT_CONNECTIONS_PER_DESTINATION,
            DEFAULT_RESPONSE_TIMEOUT);
   }

   /**
    * Creates a MessagingManager without any client side or server side addresses specified. <br>
    * <br>
    * The created MessagingManager will be named "MessagingManager".
    * 
    * @param parent the parent of this MessagingManager.
    * @param connectionsPerDestination default number of connections that will be created for client side destinations.
    * @param responseMessageTimeOut the default message response timeout used when sending synchronous messages.
    */
   public MessagingManager(SubSystem parent, int connectionsPerDestination, long responseMessageTimeOut)
   {
      this(parent, "MessagingManager", connectionsPerDestination, responseMessageTimeOut);
   }

   /**
    * Creates a MessagingManager with a single client and server address specified. <br>
    * <br>
    * The created MessagingManager will be named "MessagingManager".
    * 
    * @param parent the parent of this MessagingManager.
    * @param clientAddress client side address (address to connect to). May be <code>null</code>.
    * @param serverAddress server side address (local address to listen on). May be <code>null</code>.
    * @param connectionsPerDestination default number of connections that will be created for client side destinations.
    * @param responseMessageTimeOut the default message response timeout used when sending synchronous messages.
    */
   public MessagingManager(SubSystem parent, TcpEndPointIdentifier clientAddress, TcpEndPointIdentifier serverAddress,
         int connectionsPerDestination, long responseMessageTimeOut)
   {
      this(parent, "MessagingManager", clientAddress, serverAddress, connectionsPerDestination, responseMessageTimeOut);
   }

   /**
    * Creates a MessagingManager with a several client and server address specified. <br>
    * <br>
    * The created MessagingManager will be named "MessagingManager".
    * 
    * @param parent the parent of this MessagingManager.
    * @param clientAddresses client side addresses (addresses to connect to). May be <code>null</code>.
    * @param serverAddresses server side addresses (local addresses to listen on). May be <code>null</code>.
    * @param connectionsPerDestination default number of connections that will be created for client side destinations.
    * @param responseMessageTimeOut the default message response timeout used when sending synchronous messages.
    */
   public MessagingManager(SubSystem parent, TcpEndPointIdentifier[] clientAddresses,
         TcpEndPointIdentifier[] serverAddresses, int connectionsPerDestination, long responseMessageTimeOut)
   {
      this(parent, "MessagingManager", clientAddresses, serverAddresses, connectionsPerDestination,
            responseMessageTimeOut);
   }

   /**
    * Creates a MessagingManager without any client side or server side addresses specified.
    * 
    * @param parent the parent of this MessagingManager.
    * @param connectionsPerDestination default number of connections that will be created for client side destinations.
    * @param responseMessageTimeOut the default message response timeout used when sending synchronous messages.
    */
   public MessagingManager(SubSystem parent, String name, int connectionsPerDestination, long responseMessageTimeOut)
   {
      this(parent, name, new TcpEndPointIdentifier[0], new TcpEndPointIdentifier[0], connectionsPerDestination,
            responseMessageTimeOut);
   }

   /**
    * Creates a MessagingManager with a single client and server address specified.
    * 
    * @param parent the parent of this MessagingManager.
    * @param clientAddress client side address (address to connect to). May be <code>null</code>.
    * @param serverAddress server side address (local address to listen on). May be <code>null</code>.
    * @param connectionsPerDestination default number of connections that will be created for client side destinations.
    * @param responseMessageTimeOut the default message response timeout used when sending synchronous messages.
    */
   public MessagingManager(SubSystem parent, String name, TcpEndPointIdentifier clientAddress,
         TcpEndPointIdentifier serverAddress, int connectionsPerDestination, long responseMessageTimeOut)
   {
      this(parent, name, ((clientAddress != null) ? new TcpEndPointIdentifier[] { clientAddress } : null),
            ((serverAddress != null) ? new TcpEndPointIdentifier[] { serverAddress } : null),
            connectionsPerDestination, responseMessageTimeOut);
   }

   /**
    * Creates a MessagingManager with a several client and server address specified.
    * 
    * @param parent the parent of this MessagingManager.
    * @param clientAddresses client side addresses (addresses to connect to). May be <code>null</code>.
    * @param serverAddresses server side addresses (local addresses to listen on). May be <code>null</code>.
    * @param connectionsPerDestination default number of connections that will be created for client side destinations.
    * @param responseMessageTimeOut the default message response timeout used when sending synchronous messages.
    */
   public MessagingManager(SubSystem parent, String name, TcpEndPointIdentifier[] clientAddresses,
         TcpEndPointIdentifier[] serverAddresses, int connectionsPerDestination, long responseMessageTimeOut)
   {
      super(parent, name, clientAddresses, serverAddresses);

      // Check if parent subsystem inplements MessageReceiver - and if so set it as such
      if (parent instanceof MessageReceiver)
      {
         this.defaultMessageReceiver = (MessageReceiver) parent;
      }
      
      this.messagingSystemMetaData = new HashMap();
      this.messagingSystemMetaDataDirtyFlag = false;
      
      this.remoteServiceAddresses = new ArrayList();
      
      
      /* ### INIT DEFAULT HANDLERS ### */
      
      // Create and set default endpoint selection strategy
      this.setEndPointSelectionStrategy(new DefaultEndPointSelectionStrategy());
      
      // Create and set default message dispatch handler
      this.setMessageDispatchHandler(new DefaultMessageDispatchHandler());

      // Create and set default message receiver handler
      this.setMessageReceiverHandler(new DefaultMessageReceiverHandler());
      
      // Create and set default message processor
      this.setMessageProcessor(new DefaultMessageProcessor());
            
      // Create and set a ServerAdministrationHandler
      this.serverAdministrationHandler = new ServerAdministrationHandler(this);
      

      /* ### INIT PROPERTIES ### */
      
      this.connectionsPerDestination = new NumberProperty(this, "connectionsPerDestination",
            ((connectionsPerDestination > 0) && (connectionsPerDestination <= 1000)) ? connectionsPerDestination
                  : DEFAULT_CONNECTIONS_PER_DESTINATION, Property.MODIFIABLE_NO_RESTART);
      this.connectionsPerDestination.setDescription("The maximum number of client endpoints that are to be created for each destination.");
      addProperty(this.connectionsPerDestination);

      this.responseMessageTimeOut = new NumberProperty(this, "responseTimeout", responseMessageTimeOut,
            Property.MODIFIABLE_NO_RESTART);
      this.responseMessageTimeOut.setDescription("Maximum time(ms) to wait for a response to a message sent to a remote messaging system.");
      addProperty(this.responseMessageTimeOut);
      
      this.asynchMessageDispatchTimeOut = new NumberProperty(this, "asynchMessageDispatchTimeout", DEFAULT_ASYNCH_MESSAGE_DISPATCH_TIMEOUT, 
            Property.MODIFIABLE_NO_RESTART);
      this.asynchMessageDispatchTimeOut.setDescription("Maximum time(ms) to wait for an asynchronous message to get dispatched.");
      addProperty(this.asynchMessageDispatchTimeOut);

      this.messageReadTimeout = new NumberProperty(this, "messageReadTimeout", 30 * 1000,
            Property.MODIFIABLE_NO_RESTART);
      this.messageReadTimeout.setDescription("The timeout in milliseconds used to detect stalled/aborted reading of incomming messages by a message receiver.");
      addProperty(this.messageReadTimeout);

      this.checkInteval = new NumberProperty(this, "checkInteval", 30 * 1000, NumberProperty.MODIFIABLE_NO_RESTART);
      this.checkInteval.setDescription("The interval in milliseconds at which checks are performed on connections and meta data update commands are dispatched. Min value is > 500.");
      addProperty(this.checkInteval);

      this.reportLoad = new BooleanProperty(this, "reportLoad", true, BooleanProperty.MODIFIABLE_NO_RESTART);
      this.reportLoad.setDescription("Boolean value indicating if a load value should be should be set automatically for each thread handling an incomming message.");
      addProperty(this.reportLoad);
      
      this.enableServerAdministration = new BooleanProperty(this, "enableServerAdministration", true, BooleanProperty.MODIFIABLE_NO_RESTART);
      this.enableServerAdministration.setDescription("Boolean value indicating if remote server administration of this server should be enabled though this system.");
      addProperty(this.enableServerAdministration);
      
      this.statisticsEnabled = new BooleanProperty(this, "statisticsEnabled", false, BooleanProperty.MODIFIABLE_NO_RESTART);
      this.statisticsEnabled.setDescription("Boolean value indicating if statistics should be enabled.");
      addProperty(this.statisticsEnabled);
      
      this.proxyingEnabled = new BooleanProperty(this, "proxyingEnabled", false, BooleanProperty.MODIFIABLE_NO_RESTART);
      this.proxyingEnabled.setDescription("Boolean value indicating if proxying, i.e. if the MessagingManager should act as a proxy for all connected " +
            "servers (or actually the registered named receivers of them), should be enabled.");
      addProperty(this.proxyingEnabled);
      
      this.remoteServiceNames = new MultiStringProperty(this, "remoteServiceNames", "", MultiStringProperty.MODIFIABLE_NO_RESTART);
      this.remoteServiceNames.setDescription("The names of the services that this MessagingManager is to connect to. " +
            "Addresses for the service names will be fetched from an SNS.");
      addProperty(this.remoteServiceNames);
      
      this.localServiceNames = new MultiStringProperty(this, "localServiceNames", "", MultiStringProperty.MODIFIABLE_NO_RESTART);
      this.localServiceNames.setDescription("The names of the services provided by this MessagingManager, used for registratration in an SNS.");
      addProperty(this.localServiceNames);
      
      this.exposeMessageReceiversAsServices = new BooleanProperty(this, "exposeMessageReceiversAsServices", true, BooleanProperty.MODIFIABLE_NO_RESTART);
      this.exposeMessageReceiversAsServices.setDescription("Boolean value indicating if message receivers should be exposes as SNS services.");
      addProperty(this.exposeMessageReceiversAsServices);
      
      this.useProxiedMessageReceivers = new BooleanProperty(this, "useProxiedMessageReceivers", true, BooleanProperty.MODIFIABLE_NO_RESTART);
      this.useProxiedMessageReceivers.setDescription("Boolean value indicating if message receivers available via a proxy should be used for message dispatch. " + 
            "This property will affect the destinations returned by the methods getDestinations(String) and getDestinations(Map).");
      addProperty(this.useProxiedMessageReceivers);

      
      /* ### INIT MONITORS ### */
      this.mainMonitor = new Object(); //this.namedMessageReceivers;
      this.checkThreadWaitMonitor = new Object();
   }
   
   
   /* ### INIT/SHUTDOWN/PROPERTY METHODS BEGIN ### */
   

   /**
    * Initialization method for this MessagingManager.
    */
   protected void doInitialize()
   {
      long oldTimeOut = super.statusTransitionTimeout;
      
      try
      {
         super.statusTransitionTimeout = 15 * 60 * 1000;

         if (this.messageHandlerPool == null)
         {
            if( this.useMessageHandlerPool )
            {
               this.messageHandlerPool = new ThreadPool(this, "MessageHandlerPool", messageHandlerPoolSize,
                     MessageWorker.class, new Object[] { this });
               super.addSubComponent(this.messageHandlerPool, true);
            }
         }
         
         // Attempt to get old property "maximum connections/destination"
         super.initFromConfiguredProperty(this.connectionsPerDestination, "maximum connections/destination", false, true);
         // Attempt to get old property "response timeout"
         super.initFromConfiguredProperty(this.responseMessageTimeOut, "response timeout", false, true);
         // Attempt to get old property "asynch message dispatch timeout"
         super.initFromConfiguredProperty(this.asynchMessageDispatchTimeOut, "asynch message dispatch timeout", false, true);
         // Attempt to get old property "message read timeout"
         super.initFromConfiguredProperty(this.messageReadTimeout, "message read timeout", false, true);
         // Attempt to get old property "check inteval"
         super.initFromConfiguredProperty(this.checkInteval, "check inteval", false, true);
         // Attempt to get old property "report load"
         super.initFromConfiguredProperty(this.reportLoad, "report load", false, true);

         if (!super.isReinitializing())
         {
            synchronized (this.mainMonitor)
            {
               MessageReceiverComponent messageReceiverComponent;
               
               // Reinitialize all MessageReceiverComponents in case the MessagingManager has been moved or MessageReceiverComponent
               // have been registered and started before start up
               MessageReceiver[] messageReceivers = this.getMessageReceiverObjects();
               if( messageReceivers != null )
               {
                  if( super.isDebugMode() && (messageReceivers.length > 0) ) logDebug(messageReceivers.length + " message receivers registered - attempting to initialize.");
                  
                  for (int i = 0; i < messageReceivers.length; i++)
                  {
                     if( messageReceivers[i] instanceof MessageReceiverComponent )
                     {
                        messageReceiverComponent = ((MessageReceiverComponent)messageReceivers[i]);
                        
                        if( !messageReceiverComponent.isInitializing() && !messageReceiverComponent.isEnabled() )
                        {
                           if( super.isDebugMode() ) logDebug("Initializing message receiver component " + messageReceiverComponent + ".");
                           messageReceiverComponent.engage();
                        }
                        else if( super.isDebugMode() ) logDebug("Skipping initialization of message receiver component " + messageReceiverComponent + ".");
                     }
                  }
               }
            }
            
            this.endPointSelectionStrategy.initialize();
            this.messageDispatchHandler.initialize();
            this.messageReceiverHandler.initialize();
            
            this.initStatistics();
         }
         
         // Call super.doInitialize() which will make sure end point pools etc are created
         super.doInitialize();
            
         if( !super.isReinitializing() )
         {
            if( this.remoteServiceNames.size() > 0 ) // Register as listener in SNS client manager...
            {
               SnsClientManager snsClient = SnsClientManager.getSnsClientManager(true);
               snsClient.addServiceListener(this); // This method call will result in a call to servicesUpdated, i.e. initialization of SNS service addresses 
            }
            
            if( this.registerAsServiceProvider() )
            {
               SnsClientManager snsClient = SnsClientManager.getSnsClientManager(true);
               snsClient.addServiceProvider(this);
            }
         }
         
         // Initialize proxying enabled meta data
         if( this.proxyingEnabled.booleanValue() ) this.setMetaData(PROXYING_ENABLED_METADATA_KEY, new Boolean(true));
      }
      catch (Exception e)
      {
         logError("Error while engaging!", e);
         throw new StatusTransitionException("Error while engaging", e);
      }
      finally
      {
         super.statusTransitionTimeout = oldTimeOut;
      }
   }

   /**
    * Shut down method fot this MessagingManager.
    */
   protected void doShutDown()
   {
      super.doShutDown(); // Destroys all endpoints and endpoint grops (destinations)

      if (!isReinitializing())
      {
         synchronized(super.getEndpointGroupsLock())
         {
            this.clientSideDestinations.clear(); // Clear client destinations
         }
         this.endPointSelectionStrategy.shutDown();
         this.messageDispatchHandler.shutDown();
         this.messageReceiverHandler.shutDown();
         this.destroyMessageHandlerPool();
      }
   }

   /**
    * Destroys the message handler pool.
    */
   private final void destroyMessageHandlerPool()
   {
      if (this.messageHandlerPool != null)
      {
         try
         {
            this.messageHandlerPool.shutDown();
            this.messageHandlerPool.waitForDown(10000);
         }catch (Exception e){}
         try
         {
            removeSubComponent(this.messageHandlerPool);
            this.messageHandlerPool = null;
         }catch (Exception e){}
      }
   }
   
   
   /**
    * Checks if this messaging manager is to be registered as an sns service provider.
    */
   private boolean registerAsServiceProvider()
   {
      if( (this.serverAddresses.size() > 0) && // Register as provider in SNS client manager...
            ( (this.localServiceNames.size() > 0) ||  // ...if local services are set...
               (this.exposeMessageReceiversAsServices.booleanValue() && (this.getMessageReceiverNames().length > 0)) // ...or if message receivers are to be exported as services 
            ) 
        )
      {
         return true;
      }
      else return false;
   }
   

   /**
    * Called when a property owne by this MessagingManager is modified.
    * 
    * @param property the property that was modified.
    */
   public void propertyModified(final Property property)
   {
      if (property == this.connectionsPerDestination)
      {
         synchronized (checkThreadWaitMonitor)
         {
            // Used to force a check to be made ahead of schedule, so that each destination will have the correct
            // number of connections.
            this.checkThreadWaitMonitor.notify();
         }
      }
      else if (property == this.statisticsEnabled)
      {
         this.initStatistics();
      }
      else if(property == this.proxyingEnabled)
      {
         this.updateNamedReceiversMetaData();
         
         if( this.proxyingEnabled.booleanValue() ) this.setMetaData(PROXYING_ENABLED_METADATA_KEY, new Boolean(true));
         else this.setMetaData(PROXYING_ENABLED_METADATA_KEY, null);
      }
      else if(property == this.remoteServiceNames)
      {
         if( this.isEnabled() ) 
         {
            if( this.remoteServiceNames.size() > 0 )
            {
               SnsClientManager snsClient = SnsClientManager.getSnsClientManager(true);
               snsClient.addServiceListener(this);
            }
            else
            {
               SnsClientManager snsClient = SnsClientManager.getSnsClientManager();
               if( snsClient != null ) snsClient.removeServiceListener(this);
            }
         }
      }
      else if( (property == this.localServiceNames) || (property == super.serverAddresses) )
      {
         if( this.isEnabled() ) 
         {
            if( this.registerAsServiceProvider() )
            {
               SnsClientManager snsClient = SnsClientManager.getSnsClientManager(true);
               snsClient.addServiceProvider(this);
            }
            else
            {
               SnsClientManager snsClient = SnsClientManager.getSnsClientManager();
               if( snsClient != null ) snsClient.removeServiceProvider(this);
            }
         }
      }
      
      super.propertyModified(property);
   }

   /**
    * Validates a modification of a property's value.
    * 
    * @param property The property to be validated.
    * 
    * @return boolean value indicating if the property passed (true) validation or not (false).
    */
   public boolean validatePropertyModification(final Property property)
   {
      if (property == this.connectionsPerDestination) return ((this.connectionsPerDestination.intValue() > 0) && (this.connectionsPerDestination.intValue() <= 1000));

      else if (property == this.checkInteval) return (this.checkInteval.longValue() > 500);

      else if (property == this.responseMessageTimeOut) return (this.responseMessageTimeOut.longValue() > 0);

      else if (property == this.asynchMessageDispatchTimeOut) return (this.asynchMessageDispatchTimeOut.longValue() > 0);

      else return super.validatePropertyModification(property);
   }

   /**
    * Called to check if a property can be modified. This method makes it possible to modify the
    * remoteDestinationAddresses, localServiceNames and remoteServiceNames regardless of the state of this
    * 
    * @param property The property to be checked.
    * 
    * @return true if the property can be modified, otherwise false.
    */
   public boolean propertyModificationAllowed(Property property)
   {
      if (property == localServiceNames) return true;
      else if (property == remoteServiceNames) return true;
      else return super.propertyModificationAllowed(property);
   }
   
   
   /* ### INIT/SHUTDOWN/PROPERTY METHODS END ### */
   
   
   /* ### MISC INTERNAL/SUPERCLASS METHODS BEGIN ### */
   
   
   /**
    * Internal method that sets the addresses of the remote messaging systems that this  
    * MessagingManager should connect to. This method calls {@link AbstractMessagingManager#doUpdateRemoteDestinations(List)} 
    * using the addresses of the <code>remoteDestinationAddresses</code> property as parameter. 
    */
   protected void updateRemoteDestinations()
   {
      ArrayList allAddresses = new ArrayList(this.remoteDestinationAddresses.getAddressList());
      
      synchronized(this.mainMonitor)
      {
         allAddresses.addAll(this.remoteServiceAddresses);
      }      
      
      this.doUpdateRemoteDestinations(allAddresses);
   }   
   
   /**
    * Initializes the statistics for this messaging manager.
    */
   private void initStatistics()
   {
      if( this.statisticsEnabled.booleanValue() )
      {
         StatisticsManager statisticsManager = StatisticsManager.getStatisticsManager(true);
         
         this.statisticsName = STATISTICS_BASE_NAME + " - " + this.getName();
         this.statistics = new MessagingStatisticsSource();
         statisticsManager.addStatisticsSource(this.statisticsName, this.statistics);
      }
      else
      {
         StatisticsManager statisticsManager = StatisticsManager.getStatisticsManager();
         
         if( statisticsManager != null )
         {
            if( this.statisticsName != null )
            {
               statisticsManager.removeStatisticsSource(this.statisticsName);
            }
            this.statisticsName = null;
            this.statistics = null;
         }
      }
   }
   
   /**
    * Method to unregister the specified endpoint group.
    * 
    * @param destination a Destination.
    * 
    * @since 2.1 (20050429)
    */
   protected void unregisterGroup(final TcpEndPointGroup destination, final boolean forceUnregister)
   {
      synchronized (this.getEndpointGroupsLock())
      {
         super.unregisterGroup(destination, forceUnregister);
               
         // Call notification method
         if ( !((Destination)destination).isError() ) // Only log and call destinationLinkLost if error flag is not set (i.e. unregisterGroupInternal
            // was not called due to a reconnect attempt)
         {
            if( ((Destination)destination).isAllEndPointsDisconnected() ) logInfo("Destination disconnected: " + destination + ".");
            else logInfo("Link to destination lost - " + destination + ".");
            this.destinationLinkLost((Destination)destination);
         }
      }
      
      if( this.proxyingEnabled.booleanValue() )
      {
         // update named receivers meta data
         this.updateNamedReceiversMetaData();
      }
   }
   
   
   /* ### MISC INTERNAL/SUPERCLASS METHODS END ### */
   
   
   /* ### META DATA METHODS BEGIN ### */
   
   
   /**
    * Stores a meta data value associated with the specified key. Meta data will be shared will all associated messaging
    * systems, in which it can be accessed through the Destination object (through the method
    * {@link Destination#getDestinationMetaData(String)}representing this messaging system. <br>
    * <br>
    * The specified value must be serializable, or a RuntimeException will be thrown.
    * 
    * @param key the key of the meta data.
    * @param value the meta data value. Must be serializable.
    */
   public void setMetaData(final String key, final Object value)
   {
      if ( (value != null) && !(value instanceof Serializable)) throw new RuntimeException("Meta data value must be serializable!");

      synchronized (this.mainMonitor)
      {
         this.messagingSystemMetaData.put(key, value);
         this.messagingSystemMetaDataDirtyFlag = true;
      }
      synchronized (checkThreadWaitMonitor)
      {
         // Used to force a check to be made ahead of schedule, so the meta data will be dispatched as soon as possible to remote destinations 
         this.checkThreadWaitMonitor.notify();
      }
   }

   /**
    * Gets the meta data value with the specified key.
    * 
    * @param key a meta data key.
    * 
    * @return the meta data value matching the specified key, or <code>null</code> if none was found.
    */
   public Object getMetaData(final String key)
   {
      synchronized (this.mainMonitor)
      {
         return this.messagingSystemMetaData.get(key);
      }
   }

   /**
    * Gets all the meta data contained in this MessagingManager.
    * 
    * @return all the meta data contained in this MessagingManager.
    */
   public HashMap getMetaData()
   {
      synchronized (this.mainMonitor)
      {
         return (HashMap) this.messagingSystemMetaData.clone();
      }
   }
   
   /**
    * Called to get the meta data that will be shared with the specified destination.<br>
    * <br>
    * Overridden to assure correct locking.
    * 
    * @return the meta data that will be shared with the specified destination. 
    * 
    * @since 2.1.2 (20060206)
    */
   public HashMap getMetaData(final Destination destination)
   {
      synchronized (this.mainMonitor)
      {
        return super.getMetaData(destination); 
      }
   }
   
   
   /* ### META DATA METHODS END ### */
   
   
   /* ### PROPERTY ETC GETTERS/SETTERS BEGIN ### */

   
   /**
    * Gets the message, if any, that is currently beeing processes by the current thread.
    * 
    * @since 2.0 Build 758
    */
   public static Message getCurrentMessage()
   {
      return (Message)CurrentMessage.get();
   }
   
   /**
    * Resets ThreadLocal variable holding the message, if any, that is currently beeing processes by the current thread.
    * 
    * @since 2.0.1 (20050215)
    */
   public static void resetCurrentMessage()
   {
      CurrentMessage.set(null);
   }
   
   /**
    * Sets the message, if any, that is currently beeing processes by the current thread.
    * 
    * @since 2.0.1 (20050215)
    */
   public static void setCurrentMessage(Message message)
   {
      CurrentMessage.set(message);
   }
   
   /**
    * Gets the flag indicating if response messages should be stored in the current thread (ThreadLocal), making them 
    * accessible by calls to {@link #getCurrentMessage()}.
    * 
    * @since 2.0.1 (20050215)
    */
   public boolean isSaveResponseMessageInCurrentThread()
   {
      return saveResponseMessageInCurrentThread;
   }
   
   /**
    * Sets the flag indicating if response messages should be stored in the current thread (ThreadLocal), making them 
    * accessible by calls to {@link #getCurrentMessage()}.
    * 
    * @since 2.0.1 (20050215)
    */
   public void setSaveResponseMessageInCurrentThread(boolean saveResponseMessageInCurrentThread)
   {
      this.saveResponseMessageInCurrentThread = saveResponseMessageInCurrentThread;
   }
   
   /**
    * Gets the object used for synchronized access of message receivers.
    * 
    * @since 1.3.2
    */
   protected Object getMessageReceiverMonitor()
   {
      return this.mainMonitor;
   }

   /**
    * Sets the current value of the setting for number of (client) connections that are to be created for each remote
    * messaging system.
    * 
    * @param connectionsPerDestination the current value of the setting for number of connections that are to be created
    *                   for each BLiP.
    */
   public final void setConnectionsPerDestination(int connectionsPerDestination)
   {
      this.connectionsPerDestination.setValue(connectionsPerDestination);
   }

   /**
    * Gets the current value of the setting for number of (client) connections that are to be created for each remote
    * messaging system.
    * 
    * @return the current value of the setting for number of connections that are to be created for each BLiP.
    */
   public final int getConnectionsPerDestination()
   {
      return connectionsPerDestination.intValue();
   }

   /**
    * Sets the default timeout used when waiting on a reponse for a specific message to be received (used in the
    * <code>dispatchXXXMessage</code> methods).
    * 
    * @param responseMessageTimeOut the new timeout value.
    */
   public final void setResponseMessageTimeOut(long responseMessageTimeOut)
   {
      this.responseMessageTimeOut.setValue(responseMessageTimeOut);
   }

   /**
    * Gets the default timeout used when waiting on a reponse for a specific message to be received (used in the
    * <code>dispatchXXXMessage</code> methods).
    * 
    * @return the current timeout.
    */
   public final long getResponseMessageTimeOut()
   {
      return responseMessageTimeOut.longValue();
   }

   /**
    * Sets the thread local default timeout used when waiting on a reponse for a specific message to be received (used
    * in the <code>dispatchXXXMessage</code> methods and RPC). If this value is set is will be used instead of the value
    * specified by the property {@link #setResponseMessageTimeOut(long) responseMessageTimeOut}.
    * 
    * @param responseMessageTimeOut the new timeout value. A value of 0 or less will reset the thread local response
    *                   message timeout.
    * 
    * @since 2.0 Build 757
    */
   public final void setThreadResponseMessageTimeOut(long responseMessageTimeOut)
   {
      if (responseMessageTimeOut > 0) this.threadResponseMessageTimeout.set(new Long(responseMessageTimeOut));
      else this.threadResponseMessageTimeout.set(null);
   }

   /**
    * Gets the thread local default timeout used when waiting on a reponse for a specific message to be received (used
    * in the <code>dispatchXXXMessage</code> methods and RPC). If this value is set is will be used instead of the value
    * specified by the property {@link #setResponseMessageTimeOut(long) responseMessageTimeOut}.
    * 
    * @return the current timeout. -1 will be returned if the timeout isn't set.
    * 
    * @since 2.0 Build 757
    */
   public final long getThreadResponseMessageTimeOut()
   {
      Long timeout = (Long) this.threadResponseMessageTimeout.get();
      if (timeout != null) return timeout.longValue();
      else return -1;
   }

   /**
    * Sets the default timeout used when dispatching asynchronous messages (used in the
    * <code>dispatchXXXMessageAsynch</code> methods).
    * 
    * @param asynchMessageDispatchTimeOut the new timeout value.
    */
   public final void setAsynchMessageDispatchTimeOut(long asynchMessageDispatchTimeOut)
   {
      this.asynchMessageDispatchTimeOut.setValue(asynchMessageDispatchTimeOut);
   }

   /**
    * Gets the default timeout used when dispatching asynchronous messages (used in the
    * <code>dispatchXXXMessageAsynch</code> methods).
    * 
    * @return the current timeout.
    */
   public final long getAsynchMessageDispatchTimeOut()
   {
      return asynchMessageDispatchTimeOut.longValue();
   }

   /**
    * Sets the timeout value in milliseconds for reading of the body of a received message.
    */
   public final void setMessageReadTimeout(long messageReadTimeout)
   {
      this.messageReadTimeout.setValue(messageReadTimeout);
   }

   /**
    * Gets the timeout value in milliseconds for reading of the body of a received message.
    */
   public final long getMessageReadTimeout()
   {
      return this.messageReadTimeout.longValue();
   }

   /**
    * Gets the maximum number of bytes that can be written in form of object messages, before a object serialization
    * reset code is written. A value of 0 means that the stream will be reset after each message is written. The default
    * value is 0, which means that the stream will be reset after every message is written.
    * 
    * @since 1.3.1, build 670.
    */
   public int getObjectMessageBodyResetLimit()
   {
      return this.objectMessageBodyResetLimit;
   }

   /**
    * Sets the maximum number of bytes that can be written in form of object messages, before a object serialization
    * reset code is written. A value of 0 means that the stream will be reset after each message is written. The default
    * value is 0, which means that the stream will be reset after every message is written.<br>
    * <br>
    * <b><i>NOTE</i></b>: Object stream resets are currently not used since the implementation doesn't work in all cases.
    * 
    * @since 1.3.1, build 670
    * 
    * @deprecated temporarily as of 2.0 build 761.
    */
   public void setObjectMessageBodyResetLimit(int objectMessageBodyResetLimit)
   {
      this.objectMessageBodyResetLimit = objectMessageBodyResetLimit;
   }
   
   /**
    * Checks if a thread pool is to be used for asynchronous handling of incomming messages (default true).
    * 
    * @since 2.1.2 (20060228)
    * 
    * @see #messageReceived(Message)
    */
   public boolean isUseMessageHandlerPool()
   {
      return useMessageHandlerPool;
   }

   /**
    * Sets the flag indicating if a thread pool is to be used for asynchronous handling of incomming messages (default true).
    * This method must be called before the MessagingManager is initialized.
    * 
    * @since 2.1.2 (20060228)
    * 
    * @see #messageReceived(Message)
    */
   public void setUseMessageHandlerPool(boolean useMessageHandlerPool)
   {
      this.useMessageHandlerPool = useMessageHandlerPool;
   }
   
   /**
    * Checks if a fixed number of threads should be used for asynchronous handling of incomming messages (default false).
    * 
    * @since 2.1.2 (20060310)
    * 
    * @see #messageReceived(Message)
    */   
   public boolean isUseFixedMessageHandlerPoolSize()
   {
      return useFixedMessageHandlerPoolSize;
   }

   /**
    * Sets the flag indicating if a fixed number of threads should be used for asynchronous handling of incomming messages (default false).
    * 
    * @since 2.1.2 (20060310)
    * 
    * @see #messageReceived(Message)
    */   
   public void setUseFixedMessageHandlerPoolSize(boolean useFixedMessageHandlerPoolSize)
   {
      this.useFixedMessageHandlerPoolSize = useFixedMessageHandlerPoolSize;
   }

   /**
    * Gets the max size of the message handler thread pool.
    * 
    * @since 1.3.1, build 670.
    */
   public int getMessageHandlerPoolSize()
   {
      return messageHandlerPoolSize;
   }

   /**
    * Sets the max size of the message handler thread pool.
    * 
    * @since 1.3.1, build 670.
    */
   public void setMessageHandlerPoolSize(int messageHandlerPoolSize)
   {
      this.messageHandlerPoolSize = messageHandlerPoolSize;
      if ((this.messageHandlerPool != null) && this.isEnabled())
      {
         this.messageHandlerPool.setMaxSize(this.messageHandlerPoolSize);
      }
   }
   
   /**
    * Checks if statistics is enabled for this MessagingManager.
    * 
    * @since 2.0.1 (20050304)
    */
   public boolean isStatisticsEnabled()
   {
      return statisticsEnabled.booleanValue();
   }
   
   /**
    * Sets the flag indicating if statistics is to be enabled for this MessagingManager. 
    * 
    * @since 2.0.1 (20050304)
    */
   public void setStatisticsEnabled(boolean statisticsEnabled)
   {
      this.statisticsEnabled.setValue(statisticsEnabled);
   }
   
   /**
    * Gets the flag indicating if proxying, i.e. relaying of messages, should be enabled for this MessagingManager.
    * 
    * @since 2.1 (20050413)
    */
   public boolean isProxyingEnabled()
   {
      return proxyingEnabled.booleanValue();
   }
   
   /**
    * Sets the flag indicating if proxying, i.e. relaying of messages, should be enabled for this MessagingManager.
    * 
    * @since 2.1 (20050413)
    */
   public void setProxyingEnabled(boolean proxyingEnabled)
   {
      this.proxyingEnabled.setValue(proxyingEnabled);
   }
   
   /**
    * Gets the exposeMessageReceiversAsServices flag.
    * 
    * @since 2.1.2 (20060215)
    */   
   public boolean getExposeMessageReceiversAsServices()
   {
      return exposeMessageReceiversAsServices.booleanValue();
   }

   /**
    * Sets the exposeMessageReceiversAsServices flag.
    * 
    * @since 2.1.2 (20060215)
    */
   public void setExposeMessageReceiversAsServices(boolean exposeMessageReceiversAsServices)
   {
      this.exposeMessageReceiversAsServices.setValue(exposeMessageReceiversAsServices);
   }

   /**
    * Gets the useProxiedMessageReceivers flag.
    * 
    * @since 2.1.2 (20060215)
    */ 
   public boolean getUseProxiedMessageReceivers()
   {
      return useProxiedMessageReceivers.booleanValue();
   }

   /**
    * Sets the useProxiedMessageReceivers flag.
    * 
    * @since 2.1.2 (20060215)
    */
   public void setUseProxiedMessageReceivers(boolean useProxiedMessageReceivers)
   {
      this.useProxiedMessageReceivers.setValue(useProxiedMessageReceivers);
   }

   /**
    * Gets the interval (ms) at which periodic checks are performed in this MessagingManager. The actual check interval value is 
    * stored in the property <code>checkInteval</code>.
    * 
    * @since 2.1 (20050502)
    */
   public long getCheckInteval()
   {
      return checkInteval.longValue();
   }
   
   /**
    * Sets the interval (ms) at which periodic checks are performed in this MessagingManager. The actual check interval value is 
    * stored in the property <code>checkInteval</code>.
    * 
    * @since 2.1 (20050502)
    */
   public void setCheckInteval(long checkInteval)
   {
      this.checkInteval.setValue(checkInteval);
   }
   
   /**
    * Gets the names of the services provided by this MessagingManager. These names will be reported to all connected SNS:s.
    * 
    * @since 2.1 (20050504)
    */
   public String[] getLocalServiceNames()
   {
      return (String[])localServiceNames.getValues(new String[0]);
   }
   
   /**
    * Sets the names of the services provided by this MessagingManager. These names will be reported to all connected SNS:s.
    * 
    * @since 2.1 (20050504)
    */
   public void setLocalServiceNames(String[] localServiceNames)
   {
      this.localServiceNames.setValue(localServiceNames);
   }
   
   /**
    * Adds the name of a service provided by this MessagingManager. This name will be reported to all connected SNS:s.
    * 
    * @since 2.1 (20050504)
    */
   public void addLocalServiceName(String localServiceName)
   {
      synchronized(this.localServiceNames)
      {
         if( !this.localServiceNames.getValuesAsList().contains(localServiceName) )
         {
            this.localServiceNames.addValue(localServiceName);
         }
      }
   }
   
   /**
    * Gets the names of the services that this MessagingManager is to connect to, with the help of an SNS.
    * 
    * @since 2.1 (20050504)
    */
   public String[] getRemoteServiceNames()
   {
      return (String[])remoteServiceNames.getValues(new String[0]);
   }
   
   /**
    * Sets the names of the services that this MessagingManager is to connect to, with the help of an SNS.
    * 
    * @since 2.1 (20050504)
    */
   public void setRemoteServiceNames(String[] remoteServiceNames)
   {
      this.remoteServiceNames.setValue(remoteServiceNames);
   }
   
   /**
    * Adds the name of a services that this MessagingManager is to connect to, with the help of an SNS.
    * 
    * @since 2.1 (20050504)
    */
   public void addRemoteServiceName(String remoteServiceName)
   {
      synchronized(this.remoteServiceNames)
      {
         if( !this.remoteServiceNames.getValuesAsList().contains(remoteServiceName) )
         {
            this.remoteServiceNames.addValue(remoteServiceName);
         }
      }
   }

   /**
    * Get the current number of remote messaging systems (destinations) this MessagingManager is communicating with.
    * 
    * @return the current number of remote messaging systems this MessagingManager is communicating with.
    */
   public final int getNumberOfDestinations()
   {
      synchronized(super.getEndpointGroupsLock())
      {
         return super.endpointGroups.size();
      }
   }
   
   /**
    * Gets the name (id) of the specified remote address. The address
    * is matched against the <code>remoteAdresses</code> property of this system. 
    * This name is used in access control list checks.  
    *  
    * @param address the address to find the associated interface name for.
    * 
    * @return the associated name or <code>null</code> if none was found.
    * 
    * @since 2.0.2 (20050331)
    * 
    * @see com.teletalk.jserver.property.MultiValueProperty#getValueId(Object)
    * @see AccessControlListHandler
    */
   public String getRemoteAddressName(final TcpEndPointIdentifier address)
   {
      return this.remoteDestinationAddresses.getValueId(address);
   }
   
   /**
    * Sets the name (id) of the specified remote address. The specified name is set as the   
    * address id (value id) of the specified address in the <code>remoteAdresses</code> property of this system. 
    * This name is used in access control list checks.  
    * 
    * @param address the address to find the associated interface name for.
    * @param name the name to be associated with the address.
    * 
    * @since 2.0.2 (20050331)
    * 
    * @see com.teletalk.jserver.property.MultiValueProperty#setValueId(Object, String)
    * @see AccessControlListHandler
    */
   public void setRemoteAddressName(final TcpEndPointIdentifier address, final String name)
   {
      this.remoteDestinationAddresses.setValueId(address, name);
   }   
   
   /**
    * Gets the name (id) of the specified server (local) address. The address
    * is matched against the <code>localAddresses</code> property of this system. 
    * This name is used in access control list checks.  
    *  
    * @param address the address to find the associated interface name for.
    * 
    * @return the associated name or <code>null</code> if none was found.
    * 
    * @since 2.0.2 (20050331)
    * 
    * @see com.teletalk.jserver.property.MultiValueProperty#getValueId(Object)
    * @see AccessControlListHandler
    */
   public String getServerAddressName(final TcpEndPointIdentifier address)
   {
      return super.serverAddresses.getValueId(address);
   }
   
   /**
    * Sets the name (id) of the specified server (local) address. The specified name is set as the   
    * address id (value id) of the specified address in the <code>localAddresses</code> property of this system. 
    * This name is used in access control list checks.  
    * 
    * @param address the address to find the associated interface name for.
    * @param name the name to be associated with the address.
    *  
    * @since 2.0.2 (20050331)
    * 
    * @see com.teletalk.jserver.property.MultiValueProperty#setValueId(Object, String)
    * @see AccessControlListHandler
    */
   public void setServerAddressName(final TcpEndPointIdentifier address, final String name)
   {
      super.serverAddresses.setValueId(address, name);
   }

   /**
    * Gets the interface name (configured address id) matching the {@link Message#getDestinationAddress() destination address} or the 
    * accept (listen) address of the specified message.
    * The address is matched against the <code>remote adresses</code> property for destination adress and the <code>localAddresses</code> 
    * property for accept address. 
    * 
    * @param message a message for which to check for the an interace name. 
    * 
    * @return the associated interface name or <code>null</code> if none was found.
    * 
    * @since 2.0.2 (20050331)
    * 
    * @see #getRemoteAddressName(TcpEndPointIdentifier)
    * @see #getServerAddressName(TcpEndPointIdentifier)
    */
   public String getInterfaceName(final Message message)
   {
      // First, attempt to get id for configured remote address... 
      String addressId = this.getRemoteAddressName(message.getDestinationAddress());
      // ...otherwise attempt to get id for configured the accept address of the destination (the server address the client connected to)
      if( addressId == null ) addressId = this.getServerAddressName(message.getDestination().getAcceptAddress());
      
      return addressId;
   }
   
   /**
    * Gets the names of all message receivers in all connected remote messaging systems.
    * 
    * @since 2.0.3 (20050412)
    */
   public String[] getRemoteMessageReceiverNames()
   {
      Destination[] destinations = this.getDestinations();
      HashSet names = new HashSet();
      
      for (int i=0; i<destinations.length; i++)
      {
         if( destinations[i] != null ) names.addAll( Arrays.asList(destinations[i].getNamedReceivers()) );
      }
      
      return (String[])names.toArray(new String[0]);
   }
   
   /**
    * Checks if the specified receiver name exists as a receiver name is any of the available destinations.
    * 
    * @param receiverName the name of a remote message receiver.
    * 
    * @since 2.0.3 (20050413)
    */
   public boolean isRemoteMessageReceiverAvailable(final String receiverName)
   {
      Destination[] destinations = this.getDestinations();
            
      for (int i=0; i<destinations.length; i++)
      {
         if( destinations[i] != null )
         {
            if( Arrays.asList(destinations[i].getNamedReceivers()).contains(receiverName) ) return true;
         }
      }
      
      return false;
   }
   
   
   /* ### PROPERTY ETC GETTERS/SETTERS END ### */
   
   
   /* ### HANDLER GETTERS/SETTERS BEGIN ### */
   
   
   /**
    * Get the current {@link EndPointSelectionStrategy}, responsible for selection of endpoints/destinations for message dispatch.
    */
   public EndPointSelectionStrategy getEndPointSelectionStrategy()
   {
      return endPointSelectionStrategy;
   }

   /**
    * Set the current {@link EndPointSelectionStrategy}, responsible for selection of endpoints/destinations for message dispatch.
    */
   public void setEndPointSelectionStrategy(EndPointSelectionStrategy endPointSelectionStrategy)
   {
      if (endPointSelectionStrategy == null) return;

      this.endPointSelectionStrategy = endPointSelectionStrategy;
      this.endPointSelectionStrategy.setMessagingManager(this);
   }
   
   /**
    * Gets the current {@link MessageDispatchHandler} implementation, responsible for performing message dispatch.
    * 
    * @since 2.1 (20050517)
    */
   public MessageDispatchHandler getMessageDispatchHandler()
   {
      return messageDispatchHandler;
   }
   
   /**
    * Sets the current {@link MessageDispatchHandler} implementation, responsible for performing message dispatch.
    * 
    * @since 2.1 (20050517)
    */
   public void setMessageDispatchHandler(MessageDispatchHandler messageDispatchHandler)
   {
      if (messageDispatchHandler == null) return;
      
      this.messageDispatchHandler = messageDispatchHandler;
      this.messageDispatchHandler.setMessagingManager(this);
   }
   
   /**
    * Gets the current {@link MessageReceiverHandler} implementation, resposible for management of {@link MessageReceiver message receivers}. 
    * 
    * @since 2.1 (20050517)
    */
   public MessageReceiverHandler getMessageReceiverHandler()
   {
      return messageReceiverHandler;
   }
   
   /**
    * Sets the current {@link MessageReceiverHandler} implementation, resposible for management of {@link MessageReceiver message receivers}.
    * 
    * @since 2.1 (20050517)
    */
   public void setMessageReceiverHandler(MessageReceiverHandler messageReceiverHandler)
   {
      if (messageReceiverHandler == null) return;
      
      this.messageReceiverHandler = messageReceiverHandler;
      this.messageReceiverHandler.setMessagingManager(this);
   }
   
   /**
    * Gets the current {@link MessageProcessor} implementation, resposible for handling of incomming messages.
    * 
    * @since 2.1 (20050517)
    */
   public MessageProcessor getMessageProcessor()
   {
      return messageProcessor;
   }
   
   /**
    * Sets the current {@link MessageProcessor} implementation, resposible for handling of incomming messages.
    * 
    * @since 2.1 (20050517)
    */
   public void setMessageProcessor(MessageProcessor messageProcessor)
   {
      if (messageProcessor == null) return;
      
      this.messageProcessor = messageProcessor;
      this.messageProcessor.setMessagingManager(this);
   }
   
   /**
    * return Returns the serverAdministrationHandler.
    * 
    * @since 2.0.1 (20040925)
    */
   public ServerAdministrationHandler getServerAdministrationHandler()
   {
      return serverAdministrationHandler;
   }
   
   /**
    * @param serverAdministrationHandler The serverAdministrationHandler to set.
    * 
    * @since 2.0.1 (20040925)
    */
   public void setServerAdministrationHandler(ServerAdministrationHandler serverAdministrationHandler)
   {
      this.serverAdministrationHandler = serverAdministrationHandler;
   }
   
   /**
    * Gets the {@link AccessControlListHandler} used by this MessagingManager.
    * 
    * @since 2.0.2 (20050331)
    */
   public AccessControlListHandler getAccessControlListHandler()
   {
      return accessControlListHandler;
   }
   
   /**
    * Gets the {@link AccessControlListHandler} to be used by this MessagingManager.
    * 
    * @since 2.0.2 (20050331)
    */
   public void setAccessControlListHandler(AccessControlListHandler accessControlListHandler)
   {
      this.accessControlListHandler = accessControlListHandler;
   }
   
   
   /* ### HANDLER GETTERS/SETTERS END ### */
   
   
   /* ### SERVICEPROVIDER/SERVICEPROVIDER METHODS BEGIN ### */
   
   
   /**
    * SNS service provider method for getting a list containing the names of the services  
    * provided by this MessagingManager. The names of the services are specified through the property <code>localServiceNames</code>.
    * 
    * @since 2.1
    */
   public List getProvidedServices()
   {
      HashSet services = new HashSet();
      
      if( this.exposeMessageReceiversAsServices.booleanValue() )
      {
         services.addAll( Arrays.asList(this.getMessageReceiverNames()) );
      }
      services.addAll(this.localServiceNames.getValuesAsList());
      
      return new ArrayList(services);
   }
   
   /**
    * SNS service provider method used to fetch the addresses provided by this MessagingManager.
    * 
    * @since 2.1
    */
   public List getProvidedServiceAddresses(final String serviceName)
   {
      if( this.getProvidedServices().contains(serviceName) )
      {
         return getAddressesInternal();
      }
      return null;
   }
   
   /**
    */
   private List getAddressesInternal()
   {
      TcpEndPointIdentifier[] localAddresses = this.serverAddresses.getAddresses();
      ArrayList localAddressStrings = new ArrayList();
      for (int i=0; i<localAddresses.length; i++)
      {
         if (localAddresses[i] != null) localAddressStrings.add(localAddresses[i].toString());
      }
      
      return localAddressStrings; 
   }
   
   /**
    * SNS listener method called when SNS services have been updated.
    */
   public void servicesUpdated(final HashMap services)
   {
      List localServiceAddresses = getAddressesInternal(); // Get local addresses
      
      List remoteServiceNameList = this.remoteServiceNames.getValuesAsList();
      Service service;
      ArrayList newRemoteServiceAddresses = new ArrayList();
      
      // Iterate through service used by this MessagingManager
      for(int i=0; i<remoteServiceNameList.size(); i++)
      {
         service = (Service)services.get(remoteServiceNameList.get(i));
         if( (service != null) && (service.getAddresses() != null) )
         {
            // Add addresses for service 
            Set serviceAddresses = service.getAddresses();
            Iterator it = serviceAddresses.iterator();
            
            while (it.hasNext())
            {
               String element = (String)it.next();
               if( (element != null) && !localServiceAddresses.contains(element) ) // Dont't add local addresses
               {
                  newRemoteServiceAddresses.add(TcpEndPointIdentifier.parseTcpEndPointIdentifier(element));
               }
            }
         }
      }
            
      synchronized(this.mainMonitor)
      {
         this.remoteServiceAddresses.clear();
         this.remoteServiceAddresses.addAll(newRemoteServiceAddresses);
      }
      
      if( this.isDebugMode() ) super.logDebug("Remote service addresses updated: " + StringUtils.toString(newRemoteServiceAddresses) + ".");
      
      // Set new addresses
      this.updateRemoteDestinations();
   }
   
   
   /* ### SERVICEPROVIDER/SERVICEPROVIDER METHODS END ### */
   
   
   /* ### MESSAGE RECEIVER METHODS BEGIN ### */
   
   
   /**
    * Sets the default MessageReceiver that will be notified of all unhandled incomming messages. This means that the
    * specified message receiver will be notified of all messages except for those that are handled by named message
    * receivers and those that are responses to messages sent from this MessagingManager.
    * 
    * @param messageReceiver the new MessageReceiver.
    */
   public void setDefaultMessageReceiver(MessageReceiver messageReceiver)
   {
      synchronized (this.mainMonitor)
      {
         this.defaultMessageReceiver = messageReceiver;
      }
   }

   /**
    * Gets the default MessageReceiver that will be notified of all unhandled incomming messages.
    * 
    * @return the current MessageReceiver.
    */
   public MessageReceiver getDefaultMessageReceiver()
   {
      synchronized (this.mainMonitor)
      {
         return this.defaultMessageReceiver;
      }
   }
   
   /**
    * Sets the message receiver components of this MessagingManager, by adding/replacing the 
    * current mappings with those in <code>messageReceiverComponents</code>. The method  
    * {@link #registerMessageReceiverComponent(MessageReceiverComponent)} will be used to register the components.
    * 
    * @param messageReceiverComponents List containing MessageReceiverComponent objects.   
    * 
    * @since 2.1 (20050426)
    */
   public void setMessageReceiverComponents(final List messageReceiverComponents)
   {
      if( messageReceiverComponents != null )
      {
         synchronized (this.mainMonitor)
         {
            this.messageReceiverHandler.setMessageReceiverComponents(messageReceiverComponents);
         }
      }
   }
   
   /**
    * Sets the message receivers of this MessagingManager, by adding/replacing the 
    * current mappings with those in <code>messageReceiverMap</code>. The method 
    * {@link #registerMessageReceiver(MessageReceiver, String)} to register the message receivers.
    * 
    * @param messageReceiverMap Map containing name to MessageReceiver mappings.   
    * 
    * @since 2.1 (20050422)
    */
   public void setMessageReceivers(final Map messageReceiverMap)
   {
      if( messageReceiverMap != null )
      {
         synchronized (this.mainMonitor)
         {
            this.messageReceiverHandler.setMessageReceivers(messageReceiverMap);
         }
      }
   }

   /**
    * Registers a {@link MessageReceiverComponent} that will receive incomming messages sent to the receiver name
    * specified by the property for that purpose. This MessagingManager will register the name in the meta data of this
    * system, under the key {@link MessagingManager#NAMED_MESSAGE_RECEIVER_METADATA_KEY}. If a MessageReceiver is
    * already registered for the specified name, it will be removed (and returned).<br>
    * <br>
    * The specified message receiver component will be added as a subcomponent to this MessagingManager and engaged.
    * 
    * @param messageReceiverComponent the MessageReceiverComponent to register.
    * 
    * @return the previously registered MessageReceiver under the specified name, if any.
    * 
    * @since 2.0 Build 757
    */
   public MessageReceiver registerMessageReceiverComponent(final MessageReceiverComponent messageReceiverComponent)
   {
      synchronized (this.mainMonitor)
      {
         return this.messageReceiverHandler.registerMessageReceiverComponent(messageReceiverComponent);
      }
   }

   /**
    * Registers a {@link MessageReceiver} that will receive incomming messages sent to the receiver name specified by
    * parameter <code>name</code>. This MessagingManager will register the name in the meta data of this system,
    * under the key {@link MessagingManager#NAMED_MESSAGE_RECEIVER_METADATA_KEY}. If a MessageReceiver is already
    * registered for the specified name, it will be removed (and returned).
    * 
    * @param messageReceiver the MessageReceiver to register.
    * @param name the name that the MessageReceiver is to be associated with in the context of receiving messages.
    * 
    * @return the previously registered MessageReceiver under the specified name, if any.
    * 
    * @since 2.0 Build 757
    */
   public MessageReceiver registerMessageReceiver(final MessageReceiver messageReceiver, final String name)
   {
      synchronized (this.mainMonitor)
      {
         return this.messageReceiverHandler.registerMessageReceiver(messageReceiver, name);
      }
   }

   /**
    * Re-registers a {@link MessageReceiver}that will receive incomming messages sent to the receiver name specified by
    * parameter <code>newName</code>. The MessageReceiver will at the same time be unregistered from the old name.
    * <br>
    * <br>
    * This MessagingManager will register the new name in the meta data of this system, under the key
    * {@link MessagingManager#NAMED_MESSAGE_RECEIVER_METADATA_KEY}. If a MessageReceiver is already registered for the
    * specified name, it will be removed (and returned).
    * 
    * @param messageReceiver the MessageReceiver to register.
    * @param oldName the old name that the MessageReceiver was associated with in the context of receiving messages.
    * @param newName the name that the MessageReceiver is to be associated with in the context of receiving messages.
    *  
    * @since 2.0 Build 757
    */
   public MessageReceiver reRegisterMessageReceiver(final MessageReceiver messageReceiver, final String oldName,
         final String newName)
   {
      synchronized (this.mainMonitor)
      {
         return this.messageReceiverHandler.reRegisterMessageReceiver(messageReceiver, oldName, newName);
      }
   }
   
   /**
    * Gets {@link MessageReceiver} objects registered with names in this MessagingManager.
    * 
    * @since 2.1 (20050823)
    */
   public Map getMessageReceivers()
   {
      synchronized (this.mainMonitor)
      {
         return this.messageReceiverHandler.getMessageReceivers();
      }
   }
   
   /**
    * Gets {@link MessageReceiver}objects registered with names in this MessagingManager.
    * 
    * @since 2.1 (20050823)
    */
   public MessageReceiver[] getMessageReceiverObjects()
   {
      synchronized (this.mainMonitor)
      {
         return (MessageReceiver[])this.messageReceiverHandler.getMessageReceivers().values().toArray(new MessageReceiver[0]);
      }
   }
   
   /**
    * Gets {@link MessageReceiver}objects registered with names in this MessagingManager.
    * 
    * @since 2.0 Build 757
    * 
    * @deprecated as of 2.1 (20050617), replaced by {@link #getMessageReceiverObjects()}.
    */
   public MessageReceiver[] getRegisteredMessageReceivers()
   {
      return this.getMessageReceiverObjects();
   }
   
   /**
    * Gets the names of all message receivers registered in this MessagingManager.
    * 
    * @since 2.1 (20050617)
    */
   public String[] getMessageReceiverNames()
   {
      synchronized (this.mainMonitor)
      {
         return this.messageReceiverHandler.getMessageReceiverNames();
      }
   }

   
   /**
    * Gets the names of all message receivers registered in this MessagingManager.
    * 
    * @since 2.0.3 (20050412)
    * 
    * @deprecated as of 2.1 (20050617), replaced by {@link #getMessageReceiverNames()}.
    */
   public String[] getRegisteredMessageReceiverNames()
   {
      return this.getMessageReceiverNames();
   }

   /**
    * Gets the {@link MessageReceiver}that is registered with the specified name in this MessagingManager.
    * 
    * @param receiverName the name associated with a registered MessageReceiver.
    * 
    * @return a MessageReceiver registered with the specified name, or <code>null</code> if none was found.
    */
   public MessageReceiver getMessageReceiver(final String receiverName)
   {
      synchronized (this.mainMonitor)
      {
         return this.messageReceiverHandler.getMessageReceiver(receiverName);
      }
   }

   /**
    * Unregisters a MessageReceiver from this MessagingManager so that it will no longer receive messages.
    * 
    * @param receiverName the name of the MessageReceiver to remove.
    * 
    * @since 2.0 Build 757
    */
   public MessageReceiver unregisterMessageReceiver(final String receiverName)
   {
      synchronized (this.mainMonitor)
      {
         return this.messageReceiverHandler.unregisterMessageReceiver(receiverName);
      }
   }
   
   
   /* ### MESSAGE RECEIVER METHODS END ### */
   
   
   /* ### MESSAGE DISPATCH RELATED METHODS BEGIN ### */
   
   
   /**
    * Gets the destinations that matches the specified named receiver. This method overrides the super class 
    * implementation to filter out proxied message receiver destinations when the flag useProxiedMessageReceivers 
    * is set to false.
    * 
    * @since 2.1.1 (20060109)
    */
   public Destination[] getDestinations(final String namedReceiver)
   {
      Destination[] destinations = super.getDestinations(namedReceiver);
      
      return getValidDestinationsForMessageReceiverProxyMode(destinations);
   }
   
   /**
    * Gets the destinations that matches the specified map of meta data. This method overrides the super class 
    * implementation to filter out proxied message receiver destinations when the flag useProxiedMessageReceivers 
    * is set to false.
    * 
    * @since 2.1.1 (20060109)
    */
   public final Destination[] getDestinations(final Map metaData)
   {
      Destination[] destinations = super.getDestinations(metaData);
      
      return getValidDestinationsForMessageReceiverProxyMode(destinations);
   }
   
   /**
    * @since 2.1.1 (20060109)
    */
   private Destination[] getValidDestinationsForMessageReceiverProxyMode(final Destination[] destinations)
   {
      if( (destinations != null) && !this.useProxiedMessageReceivers.booleanValue() ) // If proxied message receivers shouldn't be used
      {
         ArrayList newDestinations = new ArrayList();
         
         for(int i=0; i<destinations.length; i++)
         {
            if( !destinations[i].isProxy() ) newDestinations.add(destinations[i]); // Add if not proxy
         }
         
         return (Destination[])newDestinations.toArray(new Destination[newDestinations.size()]);
      }
      
      return destinations;
   }
   

   /**
    * Factory method for creating a new MessagingRpcInterface. Subclasses may override this method to create custom
    * implementations instead of having to override each of the getMessagingRpcInterface methods.
    * 
    * @param messageDispatcher the MessageDispatcher to be associated with the MessagingRpcInterface.
    * 
    * @since 2.0
    */
   protected MessagingRpcInterface createMessagingRpcInterface(final MessageDispatcher messageDispatcher)
   {
      return new MessagingRpcInterface(messageDispatcher);
   }

   /**
    * Factory method for creating a new MessageDispatcher. Subclasses may override this method to create custom
    * implementations instead of having to override each of the getMessagingRpcInterfaces methods.
    * 
    * @param messageDispatcherProperties the properties to set for the message dispatcher.
    * 
    * @since 2.0
    */
   protected MessageDispatcher createMessageDispatcher(final MessageDispatcherProperties messageDispatcherProperties)
   {
      return new MessageDispatcher(this, messageDispatcherProperties);
   }

   /**
    * Internal method for performing the actual dispatching of a message. <br>
    * <br>
    * This method will automatically set the " <b>response to </b>" field of the header to the value of the " <b>message
    * id </b>" field, if set, before assigning the message a new id.
    * 
    * @param header the header of the message to be dispatched.
    * @param messageDispatchImpl the implementation to be used for dispatching the message body.
    * @param messageDispatcherProperties a {@link MessageDispatcherProperties} object containing properties for message dispatch.
    * @param proxyMessage flag indicating if the message to be dispatched is a proxied message, which means that this method should never wait for a 
    * response even if the message isn't asych.
    * 
    * @return the response message or <code>null</code> if parameter asynch was set to (<code>true</code>).
    * 
    * @throws MessageDispatchFailedException if the message could not be dispatched for some reason (possibly due to lack of connected endpoints).
    * @throws ResponseTimeOutException if timeout occurs while waiting for a response to a synchronus message.
    * 
    * @since 2.0.3 (20050413)
    */
   protected Message dispatchMessage(MessageHeader header, MessageWriter messageDispatchImpl, 
         MessageDispatcherProperties messageDispatcherProperties, boolean proxyMessage)
         throws MessageDispatchFailedException, ResponseTimeOutException
   {
      Message response = null;
      final long startTime = System.currentTimeMillis();
      
      try
      {
         // Dispatch
         response = this.messageDispatchHandler.dispatchMessage(header, messageDispatchImpl, messageDispatcherProperties, proxyMessage);
      }
      finally
      {
         if( (response != null) && !messageDispatcherProperties.isAsynch() )
         {
            // Update statistics
            if( this.statistics != null )
            {
               try{
               this.statistics.updateRequestResponseStatistics(response.getEndPoint(), header, System.currentTimeMillis() - startTime);
               }catch(Throwable t){} // We don't want statistical bugs to ruin the day...
            }
         }
      }
      
      return response;
   }
   
   /**
    * Dispatches a proxied message (request or response).
    * 
    * @param message the incomming message.
    * @param destination the destination to dispatch the message to (if known). 
    * @param namedReceiver the named receiver.
    * 
    * @return <code>true</code> if the message was successfully proxied, otherwise false.
    * 
    * @since 2.1.2 (20060322)
    */
   public boolean dispatchProxiedMessage(final Message message, final Destination destination, final String namedReceiver)
   {
      return this.dispatchProxiedMessage(message, destination, namedReceiver, true);
   }
   
   /**
    * Dispatches a proxied message (request or response).
    * 
    * @param message the incomming message.
    * @param destination the destination to dispatch the message to (if known). 
    * @param namedReceiver the named receiver.
    * @param sendReplyOnError flag indicating if a reply message should be sent automatically to the client if an error occurs while proxying the message.
    * 
    * @return <code>true</code> if the message was successfully proxied, otherwise false.
    * 
    * @since 2.1.2 (20060322)
    */
   public boolean dispatchProxiedMessage(final Message message, final Destination destination, final String namedReceiver, final boolean sendReplyOnError)
   {
      return this.dispatchProxiedMessage(message, destination, namedReceiver, sendReplyOnError, null);
   }
   
   /**
    * Dispatches a proxied message (request or response).
    * 
    * @param message the incomming message.
    * @param destination the destination to dispatch the message to (if known). 
    * @param namedReceiver the named receiver.
    * @param sendReplyOnError flag indicating if a reply message should be sent automatically to the client if an error occurs while proxying the message.
    * @param messageWriter a specific MessageWriter to use. May be null.
    * 
    * @return <code>true</code> if the message was successfully proxied, otherwise false.
    * 
    * @since 2.1.2 (20060322)
    */
   public boolean dispatchProxiedMessage(final Message message, final Destination destination, final String namedReceiver, 
         final boolean sendReplyOnError, MessageWriter messageWriter)
   {
      final MessageHeader header = message.getHeader();
      final long senderId = header.getSenderId();
      final long messageId = header.getMessageId();
      Long proxyOriginalSenderId = (Long)header.getCustomHeaderField(MessagingManager.PROXY_SENDER_ID_METADATA_KEY);
      Long proxyOriginalMessageId = (Long)header.getCustomHeaderField(MessagingManager.PROXY_MESSAGE_ID_METADATA_KEY);
      String error = null;
       
      if(   // Case 1 - request - only proxy if message isn't already proxied and only if destination or namedReceiver is specified :
            ((proxyOriginalSenderId == null) && ( (destination != null) || (namedReceiver != null) ) ) 
            // Case 2 - response - only proxy if the message can be sent as a response to the original sender :
            || ((proxyOriginalSenderId != null) && (this.getDestination(proxyOriginalSenderId.longValue()) != null)) )
      {
         try
         {
            // Save id of original sender id and message id (multiple chained proxies not supported). Note that these headers will also be sent back 
            // to the original sender of the message
            header.setCustomHeaderField(MessagingManager.PROXY_SENDER_ID_METADATA_KEY, new Long(senderId));
            header.setCustomHeaderField(MessagingManager.PROXY_MESSAGE_ID_METADATA_KEY, new Long(messageId));
            header.setCustomHeaderField(MessagingManager.PROXY_SERVER_NAME_METADATA_KEY, message.getDestination().getServerName());
                        
            if( proxyOriginalSenderId == null ) // Request 
            {
               header.setSenderId(MessageHeader.UNDEFINED); // Clear sender id to prevent dispatchMessageInternal from thinking this is a response message
            }
            else // A response to a previously proxied message
            {
               header.setSenderId(proxyOriginalSenderId.longValue()); // Response to original sender
               if( proxyOriginalMessageId != null ) header.setMessageId(proxyOriginalMessageId.longValue()); // Response to original message
            }
                        
            // Dispatch proxied message asynchronously (i.e. don't wait for response even if not asynch)
            final MessageHeader headerClone = new MessageHeader(message.getHeader());
            MessageDispatcherProperties messageDispatcherProperties = new MessageDispatcherProperties(destination, namedReceiver, headerClone.getTimeToLive(), headerClone.isAsynch());
            
            if( messageWriter == null )
            {
               messageWriter = new InputStreamMessageWriter(message.getBodyAsStream(), headerClone.getBodyLength());
            }
            
            this.dispatchMessage(headerClone, 
                  messageWriter, 
                  messageDispatcherProperties, true);
            
            return true;
         }
         catch(Exception e)
         {
            this.logWarning("Failed to releay message with header - " + header + " - " + e + ".");
            error = e.toString();
         }
         
         if( sendReplyOnError && !header.isAsynch() ) // Send message processing error response if not async 
         {
            // Revert back to the original sender id and message id of the message before returning it to the sender
            header.setSenderId(senderId);
            header.setMessageId(messageId);
            
            header.setHeaderType(MessageHeader.MESSAGE_PROCESSING_ERROR_HEADER);
            header.setDescription(error);
            this.dispatchMessageAsync(header);
         }
      }
      
      return false;
   }
   
   
   /* ### MESSAGE DISPATCH RELATED METHODS END ### */
   
   /* ### MESSAGE PROCESSING METHODS BEGIN ### */
   

   /**
    * Called, asynchronously, by {@link #messageReceived(Message)} to nofiy a message receiver of a received message.
    * This implementation checks if there is any registered message receiver object that matches the named
    * receiver, if specified, in the headers of the message. If the is such a receiver, it will be notified. Otherwise,
    * if a {@link MessageReceiver} is registered and the message has not yet been handled, it will be notified of the
    * message.
    * 
    * @param message the received message.
    */
   protected void messageReceivedImpl(final Message message) throws Exception
   {
      try
      {
         message.setMessageHandlerThread(Thread.currentThread());
         
         // Report load, if enabled
         if (this.reportLoad.booleanValue()) LoadValue.setThreadLoad(1);
         
         // Set current message in ThreadLocal
         setCurrentMessage(message);
         
         boolean messageHandled = false;
         
         final MessageHeader header = message.getHeader();
         // Get default message receiver
         MessageReceiver currentDefaultMessageReceiver = this.getDefaultMessageReceiver();
         // Get requested named receiver
         String requestedNamedMessageReceiver = (String) header.getCustomHeaderField(MessagingManager.NAMED_MESSAGE_RECEIVER_METADATA_KEY);
         
         
         // Check if this is a proxied response message (i.e. if the PROXY_SENDER_ID_METADATA_KEY header is set and if the 
         // value of the header matches a destination id found in this messaging manager)
         Long proxyOriginalSenderId = (Long)header.getCustomHeaderField(MessagingManager.PROXY_SENDER_ID_METADATA_KEY);
         boolean proxiedResponse = false;

         
         boolean responseMessage = false;
         if( header.getResponseToId() != MessageHeader.UNDEFINED ) // Check if message is a response...
         {
            responseMessage = true;
            proxiedResponse = (proxyOriginalSenderId != null) && (this.getDestination(proxyOriginalSenderId.longValue()) != null);
         }
                  
         
         /* ### PROXIED RESPONSE MESSAGE ### */
         if( proxiedResponse ) // Always handle proxied response messages
         {
            messageHandled = this.messageProcessor.handleProxyMessage(message, requestedNamedMessageReceiver);
         }
         /* ### UNHANDLED RESPONSE MESSAGE ### */
         else if( responseMessage ) // If not proxied - this is an unhandled response message (message that couldn't be matched with a "response waiter")
         {
            // Handle in default message receiver, if any
            if( currentDefaultMessageReceiver != null )
            {
               messageHandled = this.messageProcessor.handleInMessageReceiver(message, currentDefaultMessageReceiver, this.accessControlListHandler);
            }
         }
         /* ### HANDLE SERVER ADMINISTRATION MESSAGE ### */
         else if( (header.getHeaderType() == MessageHeader.SERVER_ADMINISTRATION_HEADER) )
         {
            // Enable proxying of sns client
            /*if( this.isProxyingEnabled() && (requestedNamedMessageReceiver != null) && (requestedNamedMessageReceiver.inde) )
            {
               messageHandled = this.messageProcessor.handleProxyMessage(message, requestedNamedMessageReceiver);
            }
            else*/ if( this.enableServerAdministration.booleanValue() )
            {
               messageHandled = this.messageProcessor.handleServerAdministrationMessage(message, this.accessControlListHandler);
            }
         }
         /* ### HANDLE "NORMAL" MESSAGE ### */
         else
         {
            MessageReceiver namedMessageReceiver = null;
            if ( requestedNamedMessageReceiver != null ) namedMessageReceiver = this.getMessageReceiver(requestedNamedMessageReceiver);
            
            /* ### ATTEMPT TO HANDLE IN NAMED MESSAGE RECEIVER... ### */
            messageHandled = this.messageProcessor.handleInMessageReceiver(message, namedMessageReceiver, this.accessControlListHandler);
            
            /* #### ...OR ATTEMPT TO RELAY TO ANOTHER MESSAGING SYSTEM IF PROXYING ENABLED... ### */
            if( !messageHandled )
            {
               // Handle proxied responses even if proxying isn't enabled...
               if( proxiedResponse || this.proxyingEnabled.booleanValue() )
               {
                  messageHandled = this.messageProcessor.handleProxyMessage(message, requestedNamedMessageReceiver);
               }
            }
            
            /* ### ...OR LET THE DEFAULT MESSAGE RECEIVER HANDLE THE MESSAGE ### */
            if (!messageHandled && (currentDefaultMessageReceiver != null))
            {
               messageHandled = this.messageProcessor.handleInMessageReceiver(message, currentDefaultMessageReceiver, this.accessControlListHandler);
            }
         }
         
         /* ### UNHANDLED MESSAGE ### */
         if (!messageHandled)
         {
            this.messageProcessor.handleUnhandledMessage(message);
         }
      }
      finally
      {
         if (this.reportLoad.booleanValue()) LoadValue.resetThreadLoad();

         // Reset current message in ThreadLocal
         setCurrentMessage(null);
      }
   }

   /**
    * Called when a message is received from a remote messaging system. This method will first attempt to find out if
    * the received message is a response to a message sent from this messaging system, and if so notify the thread that
    * is waiting for the response. Otherwise, this method will assign the message to a {@link MessageWorker}thread
    * object contained in a thread pool. This thread will then invoke the method {@link #messageReceivedImpl(Message)}.<br>
    * <br>
    * Subclasses may override this method to take complete control of the message handling logic, including handling of 
    * response messages to messages sent from this system (handled through a call to the method {@link MessageDispatchHandler#responseReceived(Message)}). 
    * 
    * @param message the received message.
    */
   protected void messageReceived(final Message message)
   {
      // Update statistics
      if( this.statistics != null )
      {
         try{
         this.statistics.updateReceivedStatistics(message.getEndPoint(), message.getHeader());
         }catch(Throwable t){} // We don't want statistical bugs to ruin the day...
      }
      
      try
      {
         this.initIncommingMessage(message);
         
         MessageHeader header = message.getHeader();
         boolean messageHandled = false;
         
         if( header.getResponseToId() != MessageHeader.UNDEFINED ) // Check if message is a response...
         {
            // ...and check if this is a proxied response message (i.e. if the PROXY_SENDER_ID_METADATA_KEY header is set and if the 
            // value of the header matches a destination id found in this messaging manager)
            Long proxyOriginalSenderId = (Long)header.getCustomHeaderField(MessagingManager.PROXY_SENDER_ID_METADATA_KEY);
            boolean proxiedResponse = (proxyOriginalSenderId != null) && (this.getDestination(proxyOriginalSenderId.longValue()) != null);
                        
            if( !proxiedResponse ) // If not a proxied response...
            {
               // ... let the message processor handle it, with the help of the message dispatch handler
               messageHandled = this.messageProcessor.handleResponseMessage(message, this.messageDispatchHandler);
            }
         }
         if( !messageHandled ) // ...otherwise (not a response to a message sent through this messaging manager, or a response that is received 
                                          // too late) - process message in a separate thread 
         {
            if( this.messageHandlerPool != null )
            {
               if( this.useFixedMessageHandlerPoolSize ) this.messageHandlerPool.initializeThreadWait(message);
               else this.messageHandlerPool.initializeThread(message);
            }
            else this.messageReceivedImpl(message); // Execute synchronously
         }
      }
      catch(Throwable t)
      {
         this.messageProcessor.handleMessageReceiverError(message, t);  
         if( t instanceof Error ) throw (Error)t;
      }
   }
   
   
   /* ### MESSAGE PROCESSING METHODS END ### */

   
   /* ### NOTIFICATION/INTERCEPTION METHODS BEGIN ### */
   
   /**
    * Called to initialize an outgoing message (header). This method is provides exclusively for subclasses and is invoked by 
    * {@link #dispatchMessage(MessageHeader, MessageWriter, MessageDispatcherProperties, boolean)}.<br>
    * <br>
    * <b>NOTE:<b> Since this is a notification method called by an internal thread, any overriding subclass implementations 
    * should make sure that this method executes and returns rather swiftly, without any kind of blocking wait.
    * 
    * @since 2.0 (20041223)
    */
   protected void initOutgoingMessage(final MessageHeader header)
   {
   }
   
   /**
    * Called to initialize an incomming message. This method is provides exclusively for subclasses and is invoked by 
    * {@link #messageReceived(Message)}.<br>
    * <br>
    * <b>NOTE:<b> Since this is a notification method called by an internal thread, any overriding subclass implementations 
    * should make sure that this method executes and returns rather swiftly, without any kind of blocking wait.
    * 
    * @since 2.0 (20041223)
    */
   protected void initIncommingMessage(final Message message)
   {
   }
   
   /**
    * Called to initialize an incomming response Message object before returning it to the the receiver. This method is called by 
    * {@link #dispatchMessage(MessageHeader, MessageWriter, MessageDispatcherProperties, boolean)}.
    * This means that if this method throws an exception, the exception will be thrown to the caller of the dispatchMessageXXX  (etc) method.<br>
    * <br>
    * <b>NOTE:<b> Since this is a notification method called by an internal thread, any overriding subclass implementations 
    * should make sure that this method executes and returns rather swiftly, without any kind of blocking wait.
    *  
    * @since 2.0 Build 759
    * 
    * @deprecated as of 2.0 (20041223), replaced by {@link #initResponseMessage(Message)}.
    */
   protected Message initMessage(final Message message)
   {
      return message;
   }
   
   /**
    * Called to initialize an incomming response Message object before returning it to the the receiver. This method is called by 
    * {@link #dispatchMessage(MessageHeader, MessageWriter, MessageDispatcherProperties, boolean)}.
    * This means that if this method throws an exception, the exception will be thrown to the caller of the dispatchMessageXXX  (etc) method.<br>
    * <br>
    * This method invokes the deprecated method {@link #initMessage(Message)} (which is empty) and sets the message 
    * in a ThreadLocal variable, making it accessible through the method {@link #getCurrentMessage()} (only if flag 
    * {@link #isSaveResponseMessageInCurrentThread() saveResponseMessageInCurrentThread} is set to true). <br>
    * <br>
    * <b>NOTE:<b> Since this is a notification method called by an internal thread, any overriding subclass implementations 
    * should make sure that this method executes and returns rather swiftly, without any kind of blocking wait.
    *  
    * @since 2.0 (20041223)
    */
   protected void initResponseMessage(final Message message)
   {
      initMessage(message);
      
      // Set current message in ThreadLocal
      if( this.saveResponseMessageInCurrentThread ) setCurrentMessage(message);
   }
   
   /**
    * Called just before a message is to be dispatched on a certain endpoint.<br>
    * <br>
    * <b>NOTE:<b> Since this is a notification method called by an internal thread, any overriding subclass implementations 
    * should make sure that this method executes and returns rather swiftly, without any kind of blocking wait.
    * 
    * @param header the header of the dispatched message.
    * @param endPoint the endpoint on which the message is to be dispatched.  
    * 
    * @since 2.1.2 (20060224)
    */
   protected void beforeMessageDispatch(final MessageHeader header, final MessagingEndPoint endPoint)
   {
   }
   
   /**
    * Called when a message has been successfully dispatched.<br>
    * <br>
    * The implementation only updates statistics, if enabled.<br>
    * <br>
    * <b>NOTE:<b> Since this is a notification method called by an internal thread, any overriding subclass implementations 
    * should make sure that this method executes and returns rather swiftly, without any kind of blocking wait.
    * 
    * @param header the header of the dispatched message.
    * @param endPoint the endpoint on which the message was dispatched.  
    * 
    * @since 2.1 (20050517)
    */
   protected void messageDispatched(final MessageHeader header, final MessagingEndPoint endPoint)
   {
      // Update statistics
      if( this.statistics != null )
      {
         try{
         this.statistics.updateSentStatistics(endPoint, header);
         }catch(Throwable t){} // We don't want statistical bugs to ruin the day...
      }
   }
      
   /**
    * Called when the meta data of a remote messaging manager has been updated.<br>
    * <br>
    * <b>NOTE:<b> Since this is a notification method called by an internal thread, any overriding subclass implementations 
    * should make sure that this method executes and returns rather swiftly, without any kind of blocking wait.
    * 
    * @since 2.0.3 (20050412)
    */
   protected void destinationMetaDataUpdated(final Destination destination, final HashMap previousDestinationMetaData)
   {
      if( this.proxyingEnabled.booleanValue() )
      {
         this.updateNamedReceiversMetaData();
      }
   }
   
   /**
    * Called when a link has been established with a new destination.<br>
    * <br>
    * <b>NOTE:<b> Since this is a notification method called by an internal thread, any overriding subclass implementations 
    * should make sure that this method executes and returns rather swiftly, without any kind of blocking wait.
    * 
    * @since 2.1 (20050429) 
    */
   protected void destinationLinkEstablished(final Destination destination)
   {
   }
   
   /**
    * Called when a link with a destination has been lost.<br>
    * <br>
    * <b>NOTE:<b> Since this is a notification method called by an internal thread, any overriding subclass implementations 
    * should make sure that this method executes and returns rather swiftly, without any kind of blocking wait. 
    * 
    * @since 2.1 (20050429)
    */
   protected void destinationLinkLost(final Destination destination)
   {
   }


   /**
    * Called when an endpoint establishes a link.<br>
    * <br>
    * <b>NOTE:<b> Since this is a notification method called by an internal thread, any overriding subclass implementations 
    * should make sure that this method executes and returns rather swiftly, without any kind of blocking wait.
    * 
    * @param endPoint the object representing the endpoint.
    */
   protected void endPointLinkEstablished(final TcpEndPoint endPoint)
   {
      MessagingEndPoint messagingEndPoint = (MessagingEndPoint)endPoint;
      
      this.endPointSelectionStrategy.endPointReady(messagingEndPoint);
      
      if( messagingEndPoint.isFirstEndPointInGroup() )
      {
         logInfo("Link to destination established - " + messagingEndPoint.getDestination() + ".");
         // Call notification method
         this.destinationLinkEstablished(messagingEndPoint.getDestination());
      }
   }

   /**
    * Called when an endpoint gets disconnected. This implementation removes the endpoint from the endpoint queue of
    * this system and the associated destination.<br>
    * <br>
    * <b>NOTE:<b> Since this is a notification method called by an internal thread, any overriding subclass implementations 
    * should make sure that this method executes and returns rather swiftly, without any kind of blocking wait.
    * 
    * @param endPoint the object representing the endpoint.
    */
   protected void endPointDisconnected(final TcpEndPoint endPoint)
   {
      MessagingEndPoint messagingEndPoint = (MessagingEndPoint) endPoint;
      
      // If the other side was so nice as to send disconnect messages...
      if ((messagingEndPoint != null) && (messagingEndPoint.getDestination() != null)
            && (messagingEndPoint.isDisconnectHeaderReceived()))
      {
         boolean logBritt = false;

         synchronized (super.getEndpointGroupsLock())
         {
            if ((messagingEndPoint.getDestination().size() == 0)
                  && (!messagingEndPoint.getDestination().isAllEndPointsDisconnected()))
            {
               logBritt = true;
               messagingEndPoint.getDestination().setAllEndPointsDisconnected(true);
            }
         }

         if (logBritt && this.isDebugMode() ) logDebug("All endpoints disconnected in destination " + messagingEndPoint.getDestination());
      }

      // Notify endpoint selection strategy that an endpoint has been disconnected
      this.endPointSelectionStrategy.endPointDestroyed(messagingEndPoint);
   }
   
   
   /* ### NOTIFICATION/INTERCEPTION METHODS END ### */
   
   
   /* ### CHECK RELATED METHODS BEGIN ### */
   
   
   /**
    * Updates the named receivers meta data.
    * 
    * @since 2.1 (20050412) 
    */
   protected void updateNamedReceiversMetaData()
   {
      String[] remoteMessageReceiverNames = this.getRemoteMessageReceiverNames();
      synchronized(this.mainMonitor)
      {      
         Object oldNamedReceivers = this.getMetaData(MessagingManager.NAMED_MESSAGE_RECEIVER_METADATA_KEY); 
         ArrayList namedReceivers = new ArrayList();
         namedReceivers.addAll(Arrays.asList(this.getMessageReceiverNames()));
         
         if( this.proxyingEnabled.booleanValue() )
         {
            namedReceivers.addAll(Arrays.asList(remoteMessageReceiverNames));
         }

         if( !namedReceivers.equals(oldNamedReceivers) )
         {
            this.setMetaData(MessagingManager.NAMED_MESSAGE_RECEIVER_METADATA_KEY, namedReceivers);
            if( super.isDebugMode() ) logDebug("Name receivers meta data updated: " + StringUtils.toString(namedReceivers) + ".");
         }
      }
   }

   /**
    * Method that makes sure there are a correct number of connections to each BLiP.
    * 
    * @since 2.1 (20050502)
    */
   protected void performClientSideDestinationsCheck()
   {
      int numberOfConnections;
      int connectionsToAdd;
      Destination destination;
      int maxConnections = connectionsPerDestination.intValue();

      // Only perform checks on client side clientSideDestinations
      final Destination[] currentClientSideDestinations = this.getClientSideDestinations();

      for(int i = 0; i<currentClientSideDestinations.length; i++)
      {
         destination = (Destination) currentClientSideDestinations[i];

         if (destination != null)
         {
            synchronized (super.getEndpointGroupsLock())
            {
               numberOfConnections = destination.size();
               String remoteServerName = (String) destination.getDestinationMetaData(SERVER_NAME_METADATA_KEY);
               if (remoteServerName == null) remoteServerName = "messaging system";

               if (numberOfConnections < maxConnections)
               {
                  if ((numberOfConnections <= 0) && !destination.isConnectingFirstEndPoint())
                  {
                     if (destination.isError())
                     {
                        destination.incrementErrorCounter();

                        if ((destination.getErrorCount() % 20) == 0)
                        {
                           logWarning("Unable to contact " + remoteServerName + " at address "
                                 + destination.getAddress() + ". Attempts to establish contact are underway ("
                                 + destination.getErrorCount() + ").");
                        }

                        this.createFirstClientSideMessagingEndPoint(destination.getAddress(), destination);
                     }
                     else
                     {
                        logWarning("No connections to " + remoteServerName + " at address " + destination.getAddress()
                              + " available. Attempting to connect.");
                        this.createFirstClientSideMessagingEndPoint(destination.getAddress(), destination);
                     }
                  }
                  else if (!destination.isConnectingFirstEndPoint())
                  {
                     connectionsToAdd = maxConnections - numberOfConnections;

                     logWarning("Too few connections to " + remoteServerName + " at address "
                           + destination.getAddress() + " available. Creating " + connectionsToAdd
                           + " additional endpoint" + (((connectionsToAdd) > 1) ? "s" : "") + ".");

                     for (int q = 0; q < connectionsToAdd; q++)
                     {
                        createTcpEndPoint(destination.getAddress(), new MessagingEndPointInitData(destination));
                     }
                  }
               }
               else if (numberOfConnections > maxConnections)
               {
                  try
                  {
                     synchronized(destination)
                     {
                        destination.wait(2000); // Wait a while to allow possible erroneous endpoints to
                                                               // be disconnected properly.
                     }
                  }
                  catch (InterruptedException ie){}

                  numberOfConnections = destination.size();

                  if (numberOfConnections > maxConnections)
                  {
                     int removeCount = numberOfConnections - maxConnections;

                     logWarning("Too many connections to " + remoteServerName + " at address "
                           + destination.getAddress() + " available. Removing " + removeCount + " endpoint"
                           + (((removeCount) > 1) ? "s" : "") + ".");

                     List endPointsInGroup = destination.getEndPoints();

                     for (int q = 0; (q < endPointsInGroup.size()) && (q < removeCount); q++)
                     {
                        removeTcpEndPoint(((MessagingEndPoint) endPointsInGroup.get(q)).getKey());
                     }
                  }
               }
            }
         }
      }
   }

   /**
    * Dispatches a meta data update command to all associated messaging systems.
    * 
    * @since 2.1 (20050502)
    */
   protected void dispatchMetaDataUpdateCommand()
   {
      try
      {
         HashMap messagingSystemMetaDataClone = null;

         synchronized (this.mainMonitor)
         {
            if (this.messagingSystemMetaDataDirtyFlag)
            {
               messagingSystemMetaDataClone = this.getMetaData();
               this.messagingSystemMetaDataDirtyFlag = false;
            }
         }

         // Update load
         LoadManager loadManager = LoadManager.getLoadManager();
         if (loadManager != null)
         {
            if( messagingSystemMetaDataClone == null ) messagingSystemMetaDataClone = new HashMap();
            messagingSystemMetaDataClone.put(SERVER_LOAD_METADATA_KEY, new Integer(loadManager.getLoad()));
         }

         final Destination[] destArray = this.getDestinations();
         MetaDataUpdateCommand metaDataUpdateCommand;
         HashMap destinationMetaData;
         HashMap contextualMetaData;

         for (int i = 0; i<destArray.length; i++)
         {
            // Dispatch MetaDataUpdateCommand only if link established
            if( destArray[i].isLinkEstablished() )
            {
               contextualMetaData = this.getContextualMetaData(destArray[i]);
               
               if( contextualMetaData != null )
               {
                  destinationMetaData = new HashMap();
                  if( messagingSystemMetaDataClone != null ) destinationMetaData.putAll(messagingSystemMetaDataClone);
                  destinationMetaData.putAll(contextualMetaData);
               }
               else destinationMetaData = messagingSystemMetaDataClone;
               
               
               if( destinationMetaData != null ) 
               {
                  metaDataUpdateCommand = new MetaDataUpdateCommand(destinationMetaData);
                  
                  if (super.isDebugMode()) logDebug("Dispatching MetaDataUpdateCommand to " + destArray[i] + ".");
                  this.dispatchMessage(metaDataUpdateCommand, new ByteArrayMessageWriter(
                        new byte[0]), destArray[i], null, null, this.asynchMessageDispatchTimeOut.longValue(), true);
               }
            }
         }
      }
      catch (MessageDispatchFailedException mdfe)
      {
         synchronized (this.mainMonitor)
         {
            this.messagingSystemMetaDataDirtyFlag = true;
         }
         if (super.isDebugMode()) log(Level.DEBUG, "Got exception while dispatching meta data update command!", mdfe);
      }
      catch (Exception e)
      {
         synchronized (this.mainMonitor)
         {
            this.messagingSystemMetaDataDirtyFlag = true;
         }
         logWarning("Got exception while dispatching meta data update command!", e);
      }
   }
   
   /**
    * Performs a periodic check of this MessagingManager. The interval of the periodic checks is determined by the 
    * property {@link #getCheckInteval() checkInteval}.<br>
    * <br>
    * This implementation handles endpoint and destination checks as well as meta data updates to other messaging systems.<br>
    * <br> 
    * Subclasses may override this method, but should always call this implementation to ensure proper functionality.
    * 
    * @since 2.1 (20050502)
    */
   protected void performPeriodicCheck() throws Exception
   {
      if (super.isDebugMode()) logDebug("Performing endpoint check.");
      super.performConnectionCheck();

      try{
      // Sleep a while to let any bad endpoints disconnect properly berfore performing the destination check
      Thread.sleep(500);
      }catch (InterruptedException e){}

      if (super.isDebugMode()) logDebug("Performing client side destination check.");
      this.performClientSideDestinationsCheck();
      
      if( this.proxyingEnabled.booleanValue() )
      {
         // update named receivers meta data
         this.updateNamedReceiversMetaData();
      }

      // Notify remote messaging systems of updated meta data
      this.dispatchMetaDataUpdateCommand();
   }

   /**
    * Thread implementation for this MessagingManager. Performs checks on connections on a regular basis. This method
    * also regulates the number of (client side) connections to each remote messaging system.
    */
   public final void run()
   {
      long waitTime = checkInteval.longValue();
      long lastCheck = 0;

      while (canRun)
      {
         try
         {
            synchronized (checkThreadWaitMonitor)
            {
               this.checkThreadWaitMonitor.wait(waitTime);
            }
            
            if( (System.currentTimeMillis() - lastCheck) < 500 ) Thread.sleep(500); // Prevent to frequent checks 
         }
         catch (InterruptedException e)
         {
            if (!canRun) continue;
         }
         
         lastCheck = System.currentTimeMillis() - 33;

         try
         {
            this.performPeriodicCheck();
         }
         catch (Exception e)
         {
            logWarning("Got exception while performing periodic check!", e);
         }

         // Calculate the new wait time after the checks have been performed
         waitTime = checkInteval.longValue() - (System.currentTimeMillis() - lastCheck);
         
         if (waitTime <= 0) waitTime = 1;
      }
   }
   
   /* ### CHECK RELATED METHODS END ### */
}
