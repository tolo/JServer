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
package com.teletalk.jserver.queue.legacy;

import java.net.Socket;
import java.util.List;

import org.apache.log4j.Level;

import com.teletalk.jserver.comm.EndPointIdentifier;
import com.teletalk.jserver.log.ComponentLogger;
import com.teletalk.jserver.log.LoggableObject;
import com.teletalk.jserver.queue.QueueManager;
import com.teletalk.jserver.queue.QueueSystemCollaborationManager;
import com.teletalk.jserver.queue.QueueSystemMetaData;
import com.teletalk.jserver.queue.RemoteQueueSystem;
import com.teletalk.jserver.queue.command.Command;
import com.teletalk.jserver.queue.command.DefaultStatusCommand;
import com.teletalk.jserver.queue.command.QueueItemResponse;
import com.teletalk.jserver.queue.command.QueueItemTransferRequest;
import com.teletalk.jserver.queue.command.QueueSystemCommand;
import com.teletalk.jserver.queue.command.QueueSystemSynchronizationRequest;
import com.teletalk.jserver.queue.command.QueueSystemSynchronizationResponse;
import com.teletalk.jserver.tcp.TcpEndPointIdentifier;
import com.teletalk.jserver.util.MessageQueue;

/**
 * This class serves as a proxy class for DefaultQueueSystemEndPoint. All outgoing messages to remote queue systems goes
 * thought objects of this class. The messages get put in a messagequeue before they are dispathced in a sycnhronized
 * fashion.
 * 
 * @see DefaultQueueSystemEndPoint
 * @author Tobias Löfstrand
 * @since Beta 1
 */
public final class DefaultQueueSystemEndPointProxy extends LoggableObject implements RemoteQueueSystem
{

   /*
    * Synchronization summary: All methods dealing with outgoing commands locks on field queue. All methods dealing with
    * the state of the proxy locks on the proxy (as well as get/set endpoint methods). All methods dealing with meta
    * data uses the metaDataLock.
    */

   /**
    * Constant for the state CONNECTING - indicating that this DefaultQueueSystemEndPointProxy is currently connecting
    * to another queue system.
    */
   public static final byte CONNECTING = 0;

   /**
    * Constant for the state NEGOTIATING - indicating that this DefaultQueueSystemEndPointProxy is currently connected
    * to another queue system.
    */
   public static final byte NEGOTIATING = 1;

   /** Constant for the state SYNCHRONIZING. */
   public static final byte SYNCHRONIZING = 2;

   /**
    * Constant for the state LINK_ESTABLISHED - indicating that this DefaultQueueSystemEndPointProxy has established a
    * link (successfully completed handshaking) to another queue system.
    */
   public static final byte LINK_ESTABLISHED = 3;

   /** Constant for the state ERROR - indicating that an error has occurred in this DefaultQueueSystemEndPointProxy. */
   public static final byte ERROR = 4;

   /**
    * Constant for the state RECONNECTING - indicating that this DefaultQueueSystemEndPointProxy is currently
    * reconnecting to the queue system it was previously connected to.
    */
   public static final byte RECONNECTING = 5;

   /** Constant for the state DESTROYED - indicating that this DefaultQueueSystemEndPointProxy is destroyed. */
   public static final byte DESTROYED = 6;

   /** Array containing the names for the different states. */
   public static final String[] statusNames = { "connecting", "negotiating", "synchronizing queue systems", "link established", "error", "reconnecting", "destroyed"};

   private byte initialized = 0; // Flag used to indicate to the associated DefaultQueueSystemEndPoint that this object

   // is initialized (1). If an error occured, this flag will be set to -1.

   private DefaultQueueSystemEndPointProxyImpl impl; // mode implementation (client or server)

   private volatile byte state;

   private DefaultQueueSystemCollaborationManager qcManager; // Reference to a DefaultQueueSystemCollaborationManager

   // //Should be final, but cannot because of bug in javac
   // in JDK1.2. Bug should be fixed until 1.3.

   private volatile boolean reConnectionFlag = false; // Flag indicating if a reconnection attempt has been made

   private volatile long lastConnectAttempt;

   private volatile long lastStatusMessageTime;

   private volatile int stausMessageCheckFailureCounter;

   private volatile TcpEndPointIdentifier address;

   private volatile QueueSystemMetaData remoteQueueSystemMetaData;

   private Object metaDataLock = new Object();

   private volatile DefaultQueueSystemEndPoint endPoint;

   private MessageQueue queue; // Messagequeue for outgoing messages //Should be final, but cannot because of bug in

   // javac in JDK1.2. Bug should be fixed until 1.3.

   private boolean tooManyStausMessageChecksFailed = false; // Boolean flag inidcating if too many status checks have

   // failed.

   private QueueSystemMetaData thisQueueSystemQueueStatusData; // Used for holding the status of the queue related data

   // of this queue system

   private int expectedRemoteInQueueLength = 0;

   /**
    * Creates a new (client) DefaultQueueSystemEndPointProxy which will try to connect a DefaultQueueSystemEndPoint
    * object to the specified address and port.
    * 
    * @param qcManager reference to the DefaultQueueSystemCollaborationManager.
    * @param address the address which the DefaultQueueSystemEndPoint object will be connected to.
    */
   public DefaultQueueSystemEndPointProxy(DefaultQueueSystemCollaborationManager qcManager, TcpEndPointIdentifier address) throws NullPointerException
   {
      this.qcManager = qcManager;
      this.address = address;
      this.remoteQueueSystemMetaData = null;

      queue = new MessageQueue();
      this.thisQueueSystemQueueStatusData = new QueueSystemMetaData();

      init();
      state = CONNECTING;

      this.performConnect();
   }

   /**
    * Creates a new (server) DefaultQueueSystemEndPointProxy which will be associated with the specified
    * DefaultQueueSystemEndPoint.
    * 
    * @param qcManager reference to the DefaultQueueSystemCollaborationManager.
    * @param socket a socket object.
    */
   public DefaultQueueSystemEndPointProxy(DefaultQueueSystemCollaborationManager qcManager, Socket socket, TcpEndPointIdentifier acceptAddess) throws NullPointerException
   {
      try
      {
         this.qcManager = qcManager;

         queue = new MessageQueue();

         this.address = null;
         this.remoteQueueSystemMetaData = null;

         state = NEGOTIATING;

         init();

         impl = new ServerImpl();

         endPoint = qcManager.initServerSideQueueSystemEndPoint(socket, acceptAddess, this);
         if (endPoint != null) logInfo("Connection request received from " + endPoint.getRemoteAddress() + ".");
         else throw new NullPointerException("DefaultQueueSystemCollaborationManager.initClientSideEndPoint(TcpEndPointIdentifier, DefaultQueueSystemEndPointProxy) returned null!");

         this.thisQueueSystemQueueStatusData = new QueueSystemMetaData();
         initialized = 1; // Initialization complete
      }
      finally
      {
         if (initialized == 0) initialized = -1; // If not initialized - set initialized to -1.
         synchronized (this)
         {
            notifyAll();
         }
      }
   }
   
   /**
    * Gets the logger that will be used by this object to dispatch logging events. Get the logger of the DefaultQueueSystemCollaborationManager.
    */
   protected ComponentLogger getLogger()
   {
      return this.qcManager.getLogger();
   }
   
   /**
    * Creates a logging event. This method overrides the super class implementation and adds the name and description of this proxy to the log message.
    * 
    * @since 2.1.1 (20060118)
    */
   protected void doLog(final String callerFQN, final Level level, final String origin, final Object msg, final Throwable throwable, final boolean useLogMessageId, final int logMessageId)
   {
      super.doLog(callerFQN, level, origin, this.toString() + " - " + msg, throwable, useLogMessageId, logMessageId);
   }

   private synchronized void performConnect() throws NullPointerException
   {
      try
      {
         initialized = 0;

         reConnectionFlag = true; // Set the reConnectionFlag so that no further reconnection attempts are made if the
         // first connect attempt should fail

         impl = new ClientImpl();

         endPoint = qcManager.initClientSideQueueSystemEndPoint(address, this);
         if (endPoint != null) logInfo("Connecting...");
         else throw new NullPointerException("DefaultQueueSystemCollaborationManager.initClientSideEndPoint(TcpEndPointIdentifier, DefaultQueueSystemEndPointProxy) returned null!");

         initialized = 1; // Initialization complete
      }
      finally
      {
         if (initialized == 0) initialized = -1; // If not initialized - set initialized to -1.

         notifyAll();
      }
   }

   /**
    * Internal initialization method.
    */
   private void init()
   {
      lastStatusMessageTime = System.currentTimeMillis();
      stausMessageCheckFailureCounter = 0;
      reConnectionFlag = false;
      lastConnectAttempt = System.currentTimeMillis();
   }

   /**
    * Called by DefaultQueueSystemEndPoint to wait for init of this object.
    */
   synchronized boolean waitForInit()
   {
      try
      {
         while (this.initialized == 0)
            wait();
      }
      catch (InterruptedException ie)
      {
      }

      return (this.initialized == 1);
   }

   QueueManager getQueueManager()
   {
      return this.qcManager.getQueueManager();
   }

   /**
    * Reconnects this DefaultQueueSystemEndPointProxy to the address and port to which it was connected before.
    * 
    * @return true if reconnetion was possible, otherwise false.
    */
   protected synchronized boolean reConnect()
   {
      if ((!reConnectionFlag) && (!isDestroyed()))
      {
         setCurrentState(RECONNECTING);

         lastStatusMessageTime = System.currentTimeMillis();
         stausMessageCheckFailureCounter = 0;
         lastConnectAttempt = System.currentTimeMillis();

         this.performConnect();
         return true;
      }
      else return false;
   }

   /**
    * Marks this DefaultQueueSystemEndPointProxy as destroyed and closes the link to the remote queue system it is
    * connected to.
    */
   public synchronized void destroy()
   {
      if (endPoint != null) endPoint.disconnect();
      endPoint = null;

      this.cancelAllCommands();
      
      setCurrentState(DESTROYED);
   }

   /**
    * Sets the DefaultQueueSystemEndPoint for this DefaultQueueSystemEndPointProxy. This method is used when handling
    * connection collisions.
    * 
    * @param newEndPoint a DefaultQueueSystemEndPoint object.
    */
   synchronized void setEndPoint(DefaultQueueSystemEndPoint newEndPoint)
   {
      if (this.endPoint != null)
      {
         this.endPoint.disconnect(); // Set the proxy reference of the endpoint to null to prevent it from accessing
         // this proxy any more
      }

      this.endPoint = newEndPoint;
      this.endPoint.setProxy(this);

      init();

      setCurrentState(SYNCHRONIZING);
      logInfo("Link established with remote queue system. Attempting to synchronize queue systems.");
   }

   /**
    * Gets the DefaultQueueSystemEndPoint object associated with this DefaultQueueSystemEndPointProxy.
    * 
    * @return a DefaultQueueSystemEndPoint object.
    */
   public synchronized DefaultQueueSystemEndPoint getEndPoint()
   {
      return endPoint;
   }

   /**
    * Gets a reference to the QueueSystemCollaborationManager associated with this DefaultQueueSystemEndPointProxy.
    * 
    * @return a DefaultQueueSystemCollaborationManager object.
    */
   public QueueSystemCollaborationManager getQueueSystemCollaborationManager()
   {
      return qcManager;
   }

   /**
    * Gets the current state of this DefaultQueueSystemEndPointProxy.
    * 
    * @return the current state of this DefaultQueueSystemEndPointProxy as a byte value.
    */
   public byte getCurrentState()
   {
      return state;
   }

   /**
    * Sets the current state of this DefaultQueueSystemEndPointProxy.
    * 
    * @param newState the new state of this DefaultQueueSystemEndPointProxy as a byte value.
    */
   protected synchronized void setCurrentState(byte newState)
   {
      this.state = newState;
      notify();
   }

   /**
    */
   public boolean isNegotiationCompleted()
   {
      synchronized (this)
      {
         return (this.getCurrentState() == DefaultQueueSystemEndPointProxy.LINK_ESTABLISHED) || (this.getCurrentState() == DefaultQueueSystemEndPointProxy.SYNCHRONIZING);
      }
   }

   /**
    * Checks if this DefaultQueueSystemEndPointProxy has established a link with a remote queue system. A link is
    * established when all post connection negotiation is done and this queue system is registered in the remote queue
    * system.
    * 
    * @return true if this DefaultQueueSystemEndPointProxy has established a link with a remote queue system, otherwise
    *         false.
    */
   public boolean isLinkEstablished()
   {
      return (this.getCurrentState() == DefaultQueueSystemEndPointProxy.LINK_ESTABLISHED);
   }

   /**
    * Makes the calling thread wait until this DefaultQueueSystemEndPointProxy has established a link, the specified
    * maxwait time has ellapsed, or until the DefaultQueueSystemEndPointProxy is destroyed.
    * 
    * @param maxWait the maximum wait time in milliseconds.
    * @exception InterruptedException if the current thread was interrupted while waiting.
    */
   public boolean waitForLinkEstablished(long maxWait) throws InterruptedException
   {
      long waitStart = System.currentTimeMillis();
      long waitTime;

      synchronized (this)
      {
         while ((this.getCurrentState() != DefaultQueueSystemEndPointProxy.LINK_ESTABLISHED) && (this.getCurrentState() != DefaultQueueSystemEndPointProxy.DESTROYED))
         {
            waitTime = maxWait - (System.currentTimeMillis() - waitStart);

            if (waitTime > 0)
            {
               wait(waitTime);
            }
            else break;
         }
      }

      return (this.getCurrentState() == DefaultQueueSystemEndPointProxy.LINK_ESTABLISHED);
   }

   /**
    * Makes the calling thread wait until this DefaultQueueSystemEndPointProxy has established a link, or until its is
    * destroyed.
    * 
    * @exception InterruptedException if the current thread was interrupted while waiting.
    */
   public synchronized boolean waitForLinkEstablished() throws InterruptedException
   {
      while ((this.getCurrentState() != DefaultQueueSystemEndPointProxy.LINK_ESTABLISHED) && (this.getCurrentState() != DefaultQueueSystemEndPointProxy.DESTROYED))
      {
         wait();
      }

      return (this.getCurrentState() == DefaultQueueSystemEndPointProxy.LINK_ESTABLISHED);
   }

   /**
    * Checks if this DefaultQueueSystemEndPointProxy is destroyed, i.e. if the link is down to the remote queue system.
    * The link is considered to be down when attempts to restore it (reconnect) has been made.
    * 
    * @return true if the link is down, otherwise false.
    */
   public boolean isDestroyed()
   {
      return (state == DESTROYED);
   }

   /**
    * Checkes if this DefaultQueueSystemEndPointProxy is connected. Connected in this case means that a network
    * connection has been established to a remote queue system, but no post connection negotiation has been done yet.
    * 
    * @return true if this DefaultQueueSystemEndPointProxy is connected, otherwise false.
    */
   public boolean isConnected()
   {
      return endPoint != null;
   }

   /**
    * Checkes if the connection to another queue system failed.
    * 
    * @return true if the connection to another queue system failed, otherwise false.
    */
   public boolean hasConnectionFailed()
   {
      return (this.getCurrentState() == ERROR);
   }

   /**
    * Checkes if this DefaultQueueSystemEndPointProxy failed to reconnect the other end.
    * 
    * @return true if this DefaultQueueSystemEndPointProxy failed to reconnect the other end, otherwise false.
    */
   public boolean hasReconnectionFailed()
   {
      return (reConnectionFlag && hasConnectionFailed());
   }

   /**
    * Gets the time of the last connect attempt.
    * 
    * @return the time of the last connect attempt in milliseconds.
    */
   public long getLastConnectAttemptTime()
   {
      return this.lastConnectAttempt;
   }

   /**
    * Sends a message to another queue system.
    * 
    * @param command a message to be sent.
    */
   public void dispatchCommand(QueueSystemCommand command)
   {
      if (!this.isDestroyed())
      {
         if (command instanceof QueueItemTransferRequest) // Increase expected remote in queue length
         {
            this.incrementExpectedRemoteInQueueLength();
         }
         queue.putMsg(command);
      }
      else // Cancel command if destroyed
      {
         qcManager.commandDeliveryReport(command, false);
      }
   }

   /**
    * Sends a message to another queue system urgently(puts it at the top of the messagequeue of this
    * DefaultQueueSystemEndPointProxy).
    * 
    * @param command a message to be sent.
    */
   public void dispatchUrgentCommand(QueueSystemCommand command)
   {
      if (!this.isDestroyed())
      {
         if (command instanceof QueueItemTransferRequest) // Increase expected remote in queue length
         {
            this.incrementExpectedRemoteInQueueLength();
         }
         queue.putUrgentMsg(command);
      }
      else // Cancel command if destroyed
      {
         qcManager.commandDeliveryReport(command, false);
      }
   }

   /**
    * Gets the next outgoing command. Called by DefaultQueueSystemEndPoint.
    * 
    * @return the next outgoing command.
    */
   Command getNextOutoingCommand() throws InterruptedException
   {
      Command command = (Command) queue.getMsg();

      // Piggyback meta data
      if (command instanceof QueueItemResponse)
      {
         synchronized (this.thisQueueSystemQueueStatusData)
         {
            QueueSystemMetaData queueSystemMetaData = this.qcManager.getQueueSystemMetaData();
            this.thisQueueSystemQueueStatusData.setInQueueBlocked(queueSystemMetaData.isInQueueBlocked());
            this.thisQueueSystemQueueStatusData.setInQueueLength(queueSystemMetaData.getInQueueLength());
            this.thisQueueSystemQueueStatusData.setinQueueMaxLength(queueSystemMetaData.getinQueueMaxLength());
         }

         ((QueueItemResponse) command).setQueueStatus(this.thisQueueSystemQueueStatusData);
      }
      return command;
   }

   /**
    * Cancels a command which is in the messagequeue of this DefaultQueueSystemEndPointProxy.
    * 
    * @param command a message to be cancelled.
    * @return true if the specified message was in the messagequeue, otherwise false.
    */
   public boolean cancelCommand(QueueSystemCommand command)
   {
      return this.cancelCommand(command, false);
   }

   /**
    * Cancels a command which is in the messagequeue of this DefaultQueueSystemEndPointProxy.
    * 
    * @param command a message to be cancelled.
    * @return true if the specified message was in the messagequeue, otherwise false.
    */
   public boolean cancelCommand(QueueSystemCommand command, boolean markAsSuccessful)
   {
      synchronized (queue) // Lock on the message queue
      {
         boolean cancelSuccess = queue.remove(command);

         qcManager.commandDeliveryReport(command, markAsSuccessful);

         return cancelSuccess;
      }
   }
   
   /**
    * Gets all the pending commands to the remote queue system.
    * 
    * @since 2.1.2 (20060227)
    */
   public List getPendningCommands()
   {
      return this.queue.getQueueAsList();
   }

   /**
    * Cancels all messages in the messagequeue of this DefaultQueueSystemEndPointProxy.
    */
   public void cancelAllCommands()
   {
      QueueSystemCommand queueCommand;
      Object message;

      synchronized (queue) // Lock on the message queue
      {
         while (queue.containsData())
         {
            message = queue.getMsgIfAny();

            if (message instanceof QueueSystemCommand)
            {
               queueCommand = (QueueSystemCommand) message;

               if (queueCommand != null) qcManager.commandDeliveryReport(queueCommand, false);
               else break;
            }
         }
      }
   }

   /**
    * Gets the expected length of the in queue of the remote queue system represented by this object.<br>
    * <br>
    * This method uses an internal counter of how many items have been dispatched to the remote queue system to
    * determine the expected size of the remote in queue. This counter will be updated with the actual size every time a
    * status message is received.
    * 
    * @return the length of the remote in queue or -1 if no link has been established with the remote queue system this
    *         object represents.
    */
   public int getExpectedRemoteInQueueLength()
   {
      if (this.getCurrentState() == DefaultQueueSystemEndPointProxy.LINK_ESTABLISHED)
      {
         synchronized (metaDataLock)
         {
            return this.expectedRemoteInQueueLength;
         }
      }
      else return -1;
   }

   /**
    * Increments the counter for expected remote in queue length.<br>
    * <br>
    * This method uses an internal counter of how many items have been dispatched to the remote queue system to
    * determine the expected size of the remote in queue. This counter will be updated with the actual size every time a
    * status message is received.
    */
   public void incrementExpectedRemoteInQueueLength()
   {
      synchronized (metaDataLock)
      {
         this.expectedRemoteInQueueLength++;
      }
   }

   /**
    * Sets the value of the counter for expected remote in queue length.<br>
    * <br>
    * This method uses an internal counter of how many items have been dispatched to the remote queue system to
    * determine the expected size of the remote in queue. This counter will be updated with the actual size every time a
    * status message is received.
    * 
    * @param length the new value.
    */
   public void setExpectedRemoteInQueueLength(int length)
   {
      synchronized (metaDataLock)
      {
         this.expectedRemoteInQueueLength = length;
      }
   }

   /**
    * Gets the length of the in queue of the remote queue system this DefaultQueueSystemEndPointProxy is connected to.
    * 
    * @return the length of the remote in queue or -1 if there is no link established with the remote queue system.
    */
   public int getRemoteInQueueLength()
   {
      if (this.getCurrentState() == DefaultQueueSystemEndPointProxy.LINK_ESTABLISHED)
      {
         return getRemoteQueueSystemMetaData().getInQueueLength();
      }
      else return -1;
   }

   /**
    * Gets the maximum length of the in queue of the remote queue system represented by this object.
    * 
    * @return the maximum length of the remote in queue or -1 if there is no link established with the remote queue
    *         system.
    */
   public int getRemoteInQueueMaxLength()
   {
      if (this.getCurrentState() == DefaultQueueSystemEndPointProxy.LINK_ESTABLISHED)
      {
         return getRemoteQueueSystemMetaData().getinQueueMaxLength();
      }
      else return -1;
   }

   /**
    * Checks if the in queue of the remote queue system represented by this object is expected to be full or not.<br>
    * <br>
    * This method uses an internal counter of how many items have been dispatched to the remote queue system to
    * determine the expected size of the remote in queue. This counter will be updated with the actual size every time a
    * status message is received.
    * 
    * @return <code>true</code> if the remote in queue is full, otherwise <code>false</code>. Note that the return
    *         value of this method will be <code>true</code> if there is no link established with the remote queue
    *         system.
    */
   public boolean isExpectedRemoteInQueueFull()
   {
      int max = this.getRemoteInQueueMaxLength();

      if (this.getCurrentState() == DefaultQueueSystemEndPointProxy.LINK_ESTABLISHED)
      {
         if (max >= 0) return (this.getExpectedRemoteInQueueLength() >= max);
         else return false;
      }
      else return true;
   }

   /**
    * Checks if the in queue of the remote queue system represented by this object is full or not.
    * 
    * @return <code>true</code> if the remote in queue is full, otherwise <code>false</code>. Note that the return
    *         value of this method will be <code>true</code> if there is no link established with the remote queue
    *         system.
    */
   public boolean isRemoteInQueueFull()
   {
      int max = this.getRemoteInQueueMaxLength();

      if (this.getCurrentState() == DefaultQueueSystemEndPointProxy.LINK_ESTABLISHED)
      {
         if (max >= 0) return (this.getRemoteInQueueLength() >= max);
         else return false;
      }
      else return true;
   }

   /**
    * Checks if the in queue of the remote queue system represented by this object is blocked or not.
    * 
    * @return <code>true</code> if the remote in queue is blocked, otherwise <code>false</code>. Note that the
    *         return value of this method will be <code>false</code> if there is no link established with the remote
    *         queue system.
    */
   public boolean isRemoteInQueueBlocked()
   {
      if (this.getCurrentState() == DefaultQueueSystemEndPointProxy.LINK_ESTABLISHED)
      {
         return getRemoteQueueSystemMetaData().isInQueueBlocked();
      }
      else return false;
   }

   /**
    * Gets the address of the remote queue system this DefaultQueueSystemEndPointProxy is supposed to represent.
    * 
    * @return an InetAddress object.
    */
   public EndPointIdentifier getRemoteQueueSystemAddress()
   {
      return address;
   }

   /**
    * Gets the name of the server which this DefaultQueueSystemEndPointProxy is connected to.
    * 
    * @return the name of the server which this DefaultQueueSystemEndPointProxy is connected to, or <code>null</code>
    *         if the metadata object didn't contain one.
    */
   public String getRemoteServerName()
   {
      String name = "n/a";

      QueueSystemMetaData md = getRemoteQueueSystemMetaData();
      if (md != null)
      {
         String n = (String) md.getExtraData(DefaultQueueSystemCollaborationManager.SERVER_NAME_METADATA_KEY);
         if (n != null)
         {
            name = n;
         }
      }

      return name;
   }

   /**
    * Gets a string representing the queue system this DefaultQueueSystemEndPointProxy is connected to, by combining the
    * results from {@link #getRemoteQueueSystemAddress()} and {@link #getRemoteServerName()}.
    * 
    * @return a string representing the remote queue system.
    */
   public String getRemoteQueueSystemName()
   {
      String remoteServerName = getRemoteServerName();

      if (remoteServerName != null) return remoteServerName + " at " + getRemoteQueueSystemAddress().getAddressAsString();
      else if (getRemoteQueueSystemAddress() != null) return getRemoteQueueSystemAddress().getAddressAsString();
      else return null;
   }

   /**
    * Gets a metadata object from the remote queue system to which this DefaultQueueSystemEndPointProxy object is
    * connected.
    * 
    * @return a QueueSystemMetaData object.
    * @see QueueSystemMetaData
    */
   public QueueSystemMetaData getRemoteQueueSystemMetaData()
   {
      synchronized (metaDataLock)
      {
         return this.remoteQueueSystemMetaData;
      }
   }

   /**
    * Sends a status message to the remote queue system at the other end.
    * 
    * @param statusCommand the status message to be sent.
    */
   public void sendStatusCommand(DefaultStatusCommand statusCommand)
   {
      if (this.getCurrentState() == DefaultQueueSystemEndPointProxy.LINK_ESTABLISHED)
      {
         queue.putUrgentMsg(statusCommand);
      }
   }

   /**
    * Called when a status command, containing metadata, is received from the remote queue system to which this
    * DefaultQueueSystemEndPointProxy object is connected. This method is also called when a link is first established
    * to the queue system.
    * 
    * @param metaData a QueueSystemMetaData object.
    * @see QueueSystemMetaData
    */
   public void remoteQueueSystemMetaDataUpdated(QueueSystemMetaData metaData)
   {
      synchronized (metaDataLock)
      {
         lastStatusMessageTime = System.currentTimeMillis();

         if (metaData != null)
         {
            if ((this.remoteQueueSystemMetaData != null) && (metaData.getExtraData() == null)) // If piggybacked meta
            // data (which only
            // contains queue
            // status...)
            {
               this.remoteQueueSystemMetaData.setInQueueBlocked(metaData.isInQueueBlocked());
               this.remoteQueueSystemMetaData.setInQueueLength(metaData.getInQueueLength());
               this.remoteQueueSystemMetaData.setinQueueMaxLength(metaData.getinQueueMaxLength());
            }
            else
            {
               this.remoteQueueSystemMetaData = metaData;
            }

            this.expectedRemoteInQueueLength = metaData.getInQueueLength();
         }
      }
   }

   /**
    * Gets the interval at which statusmessages are dispathced from the remote queue system to which this
    * DefaultQueueSystemEndPointProxy object is connected.
    * 
    * @return the interval in milliseconds.
    */
   public long getStatusMessageInterval()
   {
      synchronized (metaDataLock)
      {
         if (remoteQueueSystemMetaData != null)
         {
            Number n = (Number) remoteQueueSystemMetaData.getExtraData(DefaultQueueSystemCollaborationManager.STATUSMESSAGE_INTERVAL_METADATA_KEY);

            if (n != null) return n.longValue();
         }
         return -1;
      }
   }

   /**
    * Gets the time on which the last status message was received from the remote queue system to which this
    * DefaultQueueSystemEndPointProxy object is connected.
    * 
    * @return the time of the last status message.
    */
   public long getLastStatusMessageTime()
   {
      return lastStatusMessageTime;
   }

   /**
    * Gets the full name of this DefaultQueueSystemEndPointProxy.
    * 
    * @return the full name of this DefaultQueueSystemEndPointProxy.
    */
   /*public String getFullName()
   {
      return qcManager.getFullName() + "." + toString();
   }*/

   /**
    * Gets a string representation for this DefaultQueueSystemEndPointProxy.
    * 
    * @return a string representation for this DefaultQueueSystemEndPointProxy.
    */
   public String toString()
   {
      // TEMP//
      // String id = impl.getId() + "[" + Integer.toHexString(System.identityHashCode(this)) + "]";
      String id = "";
      // TEMP//

      if (address != null) return "DefaultQueueSystemEndPointProxy(Remote queue system: " + getRemoteQueueSystemName() + ", status: "
            + DefaultQueueSystemEndPointProxy.statusNames[this.getCurrentState()] + ")" + id;
      else if (endPoint != null) return "DefaultQueueSystemEndPointProxy(Remote queue system: " + endPoint.getRemoteAddress() + ", status: "
            + DefaultQueueSystemEndPointProxy.statusNames[this.getCurrentState()] + ")" + id;
      else return "DefaultQueueSystemEndPointProxy(Remote queue system: n/a, status: " + DefaultQueueSystemEndPointProxy.statusNames[this.getCurrentState()] + ")" + id;
   }

   /**
    * Gets a key used to uniquely identify this object.
    * 
    * @return a key used to uniquely identify this object.
    */
   public String getKey()
   {
      return address.getAddressAsString();
   }

   /**
    * Checks this DefaultQueueSystemEndPointProxy for errors.
    * 
    * @return <code>true</code> if everything is OK, otherwise <code>false</code>.
    */
   public synchronized boolean check()
   {
      boolean result = true;

      if ((state == CONNECTING) || (state == RECONNECTING))
      {
         if ((endPoint != null) && endPoint.isActive())
         {
            result = endPoint.check();

            if (!result) logWarning("Associated DefaultQueueSystemEndPoint failed check!");
            else
            {
               // Check if we have been in this state abnormally long e.g. if connect took to long / was stalled
               long timeOut = (qcManager.getConnectAttempts() * qcManager.getConnectTimeOut()) * 2;
               if (timeOut < 60 * 1000) timeOut = 60 * 1000;

               if ((System.currentTimeMillis() - lastConnectAttempt) > timeOut)
               {
                  result = false;
                  logError("Connection error! Socket connect took to long / was stalled!");
               }
            }
         }
         else
         {
            endPoint = null;
            result = false;
            logError("Connection error (state=CONNECTING)!");
         }
      }
      else if ((state == NEGOTIATING) || (state == SYNCHRONIZING))
      {
         if ((endPoint != null) && endPoint.isActive())
         {
            result = endPoint.check();

            if (!result) logWarning("Associated DefaultQueueSystemEndPoint failed check!");
            else
            {
               // Check if we have been in this state abnormally long e.g. if connection negotiation / synchronization
               // took to long / was stalled
               long timeOut = (qcManager.getConnectAttempts() * qcManager.getConnectTimeOut()) * 50;
               if (timeOut < (20 * 60 * 1000)) timeOut = 20 * 60 * 1000; // Synchronization can take a very long time
               // if the queues are large...

               if ((System.currentTimeMillis() - lastConnectAttempt) > timeOut)
               {
                  result = false;
                  logError("Connection error! Post connection negotiation took to long / was stalled!");
               }
            }
         }
         else
         {
            endPoint = null;
            result = false;
            if (state == NEGOTIATING) logError("Connection error (state=NEGOTIATING)!");
            else logError("Connection error (state=SYNCHRONIZING)!");
         }
      }
      else if (state == LINK_ESTABLISHED)
      {
         if ((endPoint != null) && endPoint.isActive())
         {
            result = endPoint.check();

            if (!result) logWarning("Associated DefaultQueueSystemEndPoint failed check!");
         }
         else
         {
            endPoint = null;
            result = false;
            logError("Connection error (state=LINK_ESTABLISHED)!");
         }
      }
      else if (state == ERROR)
      {
         result = false;
      }

      return result;
   }

   /**
    * Checks if status messages has been received as expected from the remote queue system this
    * DefaultQueueSystemEndPointProxy is connected to.
    * 
    * @return true if everything is OK, otherwise false. True is also returned if this DefaultQueueSystemEndPointProxy
    *         has not yet been connected.
    */
   public boolean statusMessageCheck()
   {
      boolean result = true;

      if (getCurrentState() == LINK_ESTABLISHED)
      {
         // if(getStatusMessageInterval() > 0)
         // {
         long currentStatusMessageInterval = getStatusMessageInterval();

         if (currentStatusMessageInterval <= 0) // If there was no statusmessageinterval information in the metadata...
            currentStatusMessageInterval = qcManager.getStatusMessageInterval(); // Get the statusmessageinterval of
         // this queue system

         long timeSinceLastStatusMessage = System.currentTimeMillis() - getLastStatusMessageTime();

         if (timeSinceLastStatusMessage > (currentStatusMessageInterval * 5))
         {
            if (!tooManyStausMessageChecksFailed)
            {
               logWarning("Status message timeout - last status message received " + timeSinceLastStatusMessage + " milliseconds ago!");
               stausMessageCheckFailureCounter++;
            }
         }
         else
         {
            tooManyStausMessageChecksFailed = false;
            stausMessageCheckFailureCounter = 0;
         }

         if (stausMessageCheckFailureCounter >= 5)
         {
            logWarning("To many status message timeouts!");
            tooManyStausMessageChecksFailed = true;
            stausMessageCheckFailureCounter = 0;
            result = false;
         }

         // }
         // Otherwise, check timeout...
         /*
          * else if((System.currentTimeMillis() - getLastStatusMessageTime()) > (qcManager.getStatusMessageInterval() *
          * 5)) { logWarning("Connect timeout in associated
          * DefaultQueueSystemEndPoint"); result = false; }
          */
      }
      return result;
   }

   /*
    * ############################################################ ########### DefaultQueueSystemEndPoint callback
    * methods ########### ############################################################
    */

   private boolean checkEndPoint(DefaultQueueSystemEndPoint ep)
   {
      return (this.endPoint == ep);
   }

   /**
    * Called by the associated DefaultQueueSystemEndPoint when a connection is established. <br>
    * <br>
    * The implementation of this method depends on whether this is a client- or serverside proxy.
    * 
    * @param connectionSuccessful boolean flag indicating if the connection was successfully established.
    */
   synchronized void endPointConnected(boolean connectionSuccessful, DefaultQueueSystemEndPoint ep)
   {
      if (checkEndPoint(ep))
      {
         impl.endPointConnected(connectionSuccessful);
      }
   }

   /**
    * Called by the associated DefaultQueueSystemEndPoint to establish a link with the remote queue system. <br>
    * <br>
    * The implementation of this method depends on whether this is a client- or serverside proxy.
    * 
    * @param metaData a QueueSystemMetaData object, containing information about the remote queue system.
    */
   synchronized boolean establishLink(QueueSystemMetaData metaData, DefaultQueueSystemEndPoint ep)
   {
      if (checkEndPoint(ep))
      {
         return impl.establishLink(metaData);
      }
      else return false;
   }

   /**
    */
   synchronized QueueSystemSynchronizationRequest initiateQueueSystemSynchronization(DefaultQueueSystemEndPoint ep)
   {
      if (checkEndPoint(ep))
      {
         return impl.initiateQueueSystemSynchronization();
      }
      else return null;
   }

   /**
    */
   synchronized QueueSystemSynchronizationResponse queueSystemSynchronizationRequestReceived(QueueSystemSynchronizationRequest synchRequest, DefaultQueueSystemEndPoint ep)
   {
      if (checkEndPoint(ep))
      {
         return impl.queueSystemSynchronizationRequestReceived(synchRequest);
      }
      else return null;
   }

   /**
    */
   synchronized void queueSystemSynchronizationResponseReceived(QueueSystemSynchronizationResponse synchResponse, DefaultQueueSystemEndPoint ep)
   {
      if (checkEndPoint(ep))
      {
         QueueSystemMetaData mData = synchResponse.getMetaData();
         if (mData != null) this.setExpectedRemoteInQueueLength(mData.getInQueueLength()); // Update expected remote
         // in queue length

         impl.queueSystemSynchronizationResponseReceived(synchResponse);
      }
   }

   /**
    */
   synchronized void linkEstablished(DefaultQueueSystemEndPoint ep)
   {
      if (checkEndPoint(ep))
      {
         impl.linkEstablished();
      }
   }

   /**
    * Called when the associated DefaultQueueSystemEndPoint gets disconnected. <br>
    * <br>
    * The implementation of this method depends on whether this is a client- or serverside proxy.
    */
   synchronized void endPointDisconnected(DefaultQueueSystemEndPoint ep)
   {
      if (checkEndPoint(ep))
      {
         impl.endPointDisconnected();
      }
   }

   /**
    * Called when an error occurs in the DefaultQueueSystemEndPoint object associated with this
    * DefaultQueueSystemEndPointProxy. <br>
    * <br>
    * The implementation of this method depends on whether this is a client- or serverside proxy.
    */
   synchronized void endPointError(DefaultQueueSystemEndPoint ep)
   {
      if (checkEndPoint(ep))
      {
         impl.endPointError();
      }
   }

   /**
    * Internal implemetation of DefaultQueueSystemEndPointProxy.
    */
   private class DefaultQueueSystemEndPointProxyImpl
   {

      public String getId()
      {
         return "fel";
      }

      void endPointConnected(boolean connectionSuccessful)
      {
      }

      boolean establishLink(QueueSystemMetaData metaData)
      {
         return false;
      }

      QueueSystemSynchronizationRequest initiateQueueSystemSynchronization()
      {
         lastConnectAttempt = System.currentTimeMillis(); // Update lastConnectAttempt, which is used in check() to
         // determine if connection/negotiation was stalled
         return qcManager.getQueueManager().initiateQueueSystemSynchronization(DefaultQueueSystemEndPointProxy.this);
      }

      QueueSystemSynchronizationResponse queueSystemSynchronizationRequestReceived(QueueSystemSynchronizationRequest synchRequest)
      {
         lastConnectAttempt = System.currentTimeMillis(); // Update lastConnectAttempt, which is used in check() to
         // determine if connection/negotiation was stalled
         return qcManager.getQueueManager().queueSystemSynchronizationRequestReceived(DefaultQueueSystemEndPointProxy.this, synchRequest);
      }

      void queueSystemSynchronizationResponseReceived(QueueSystemSynchronizationResponse synchResponse)
      {
         lastConnectAttempt = System.currentTimeMillis(); // Update lastConnectAttempt, which is used in check() to
         // determine if connection/negotiation was stalled
         qcManager.getQueueManager().queueSystemSynchronizationResponseReceived(DefaultQueueSystemEndPointProxy.this, synchResponse);
      }

      void linkEstablished()
      {
         lastConnectAttempt = System.currentTimeMillis(); // Update lastConnectAttempt, which is used in check() to
         // determine if connection/negotiation was stalled
         lastStatusMessageTime = System.currentTimeMillis(); // Update lastStatusMessageTime so that the
         // statusMessageCheck() doesn't get sad
         setCurrentState(LINK_ESTABLISHED);
         logInfo("Queue system synchronization complete.");
      }

      void endPointDisconnected()
      {
         endPoint = null;
         setCurrentState(DESTROYED);
      }

      void endPointError()
      {
         endPoint = null;
         setCurrentState(ERROR);
      }
   }

   /**
    * Internal clientside implemetation of DefaultQueueSystemEndPointProxy.
    */
   private final class ClientImpl extends DefaultQueueSystemEndPointProxyImpl
   {

      public String getId()
      {
         return "CS";
      }

      void endPointConnected(boolean connectionSuccessful)
      {
         if (connectionSuccessful)
         {
            setCurrentState(NEGOTIATING);

            DefaultQueueSystemEndPoint ep = getEndPoint();

            if (ep != null) // If endpoint is null, then a connection collision probably has occurred...
               logInfo("Connected to " + ep.getRemoteAddress() + ". Attempting to establish link.");
         }
         else
         {
            endPoint = null;
            setCurrentState(ERROR);
            logInfo("Unable to connect to remote queue system.");
         }
      }

      boolean establishLink(QueueSystemMetaData metaData)
      {
         try
         {
            lastConnectAttempt = System.currentTimeMillis(); // Update lastConnectAttempt, which is used in check() to
            // determine if connection/negotiation was stalled

            if (metaData != null)
            {
               remoteQueueSystemMetaDataUpdated(metaData);

               setCurrentState(SYNCHRONIZING);
               logInfo("Link established with remote queue system. Attempting to synchronize queue systems.");

               reConnectionFlag = false; // Set the reConnectionFlag to false so that this proxy can reconnect if the
               // connection should fail later on.
               return true;
            }
            else
            {
               logWarning("Failed to establish link with queue system (connect response received from remote queue system contained no meta data).");
               endPoint = null;
               setCurrentState(DESTROYED);
               return false;
            }
         }
         catch (Throwable e)
         {
            logError("Failed to establish link with queue system - exception occurred.", e);
            endPoint = null;
            setCurrentState(DESTROYED);
            if (e instanceof Error) throw (Error) e;
            else return false;
         }
      }
   }

   /**
    * Internal severside implemetation of DefaultQueueSystemEndPointProxy.
    */
   private final class ServerImpl extends DefaultQueueSystemEndPointProxyImpl
   {

      public String getId()
      {
         return "SS";
      }

      void endPointConnected(boolean connectionSuccessful)
      {
         if (connectionSuccessful)
         {
            setCurrentState(NEGOTIATING);
            logInfo("Connected to " + endPoint.getRemoteAddress() + ". Attempting to establish link.");
         }
         else
         {
            endPoint = null;
            setCurrentState(ERROR);
            logWarning("Unknown error (endPointConnected).");
         }
      }

      boolean establishLink(QueueSystemMetaData metaData)
      {
         try
         {
            lastConnectAttempt = System.currentTimeMillis(); // Update lastConnectAttempt, which is used in check() to
            // determine if connection/negotiation was stalled

            if (metaData != null)
            {
               remoteQueueSystemMetaDataUpdated(metaData); // This metod initializes member fields (for instance the
               // address field) of this object from the metadata.

               synchronized (metaDataLock)
               {
                  address = (TcpEndPointIdentifier) metaData.getExtraData(DefaultQueueSystemCollaborationManager.QUEUESYSTEM_ADDRESS_METADATA_KEY);
               }

               if (address == null)
               {
                  address = (TcpEndPointIdentifier) endPoint.getRemoteAddress();
                  logWarning("Unable to get queue system address from meta data while trying to establish link. Using remote address of endpoint.");
               }

               int result = qcManager.remoteQueueSystemLinkRequest(DefaultQueueSystemEndPointProxy.this); // Try to
               // establish
               // link and
               // check for
               // connection
               // collision

               if (result > 0) // Link sucessfully established
               {
                  setCurrentState(SYNCHRONIZING);
                  logInfo("Link established with remote queue system. Attempting to synchronize queue systems.");

                  reConnectionFlag = false; // Set the reConnectionFlag to false so that this proxy can reconnect if
                  // the connection should fail later on.
                  return true;
               }
               else if (result == 0) // Connection collision occurred but the endpoint of this proxy won the "duel".
               {
                  endPoint = null; // Set endPoint to null so that the call to destroy won't disconnect the endpoint.
                  destroy();
                  return true;
               }
               else
               // Connection collision occurred by the endpoint of this proxy lost the "duel".
               {
                  endPoint = null; // Set endPoint to null so that the call to destroy won't call the disconnect method
                  // in endpoint (because that is unnecessary and generates error logs)
                  destroy();
                  return false;
               }
            }
            else
            {
               logWarning("Failed to establish link with queue system (connect command received from remote queue system contained no meta data).");
               endPoint = null;
               setCurrentState(DESTROYED);
               return false;
            }
         }
         catch (Throwable e)
         {
            logError("Failed to establish link with queue system - exception occurred.", e);
            endPoint = null;
            setCurrentState(DESTROYED);
            if (e instanceof Error) throw (Error) e;
            else return false;
         }
      }
   }
}
