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

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.net.SocketException;

import com.teletalk.jserver.queue.QueueSystemMetaData;
import com.teletalk.jserver.queue.command.Command;
import com.teletalk.jserver.queue.command.DefaultConnectCommand;
import com.teletalk.jserver.queue.command.DefaultConnectResponse;
import com.teletalk.jserver.queue.command.DefaultDisconnectCommand;
import com.teletalk.jserver.queue.command.QueueSystemCommand;
import com.teletalk.jserver.queue.command.QueueSystemSynchronizationRequest;
import com.teletalk.jserver.queue.command.QueueSystemSynchronizationResponse;
import com.teletalk.jserver.tcp.TcpEndPoint;
import com.teletalk.jserver.tcp.TcpEndPointIdentifier;

/**
 * This class symbolizes an actual connection to another queue system and is responsible for receiving commands from
 * that queue system.
 * 
 * @author Tobias Löfstrand
 * @since Beta 1
 */
public final class DefaultQueueSystemEndPoint extends TcpEndPoint
{

   /*
    * Synchronization summary: The lock of this object is used by the superclass and in disconnect methods. ...The lock
    * of this object is primarily used for serializing access to the proxy reference
    */

   private SenderThread senderThread;

   private DefaultQueueSystemEndPointImpl impl; // Internal implementation class (client or server)

   DefaultQueueSystemCollaborationManager qcManager; // Reference to a DefaultQueueSystemCollaborationManager

   volatile DefaultQueueSystemEndPointProxy proxy = null; // The associated DefaultQueueSystemEndPointProxy

   /**
    * Creates a new DefaultQueueSystemEndPoint.
    * 
    * @param qcManager reference to a DefaultQueueSystemCollaborationManager.
    */
   public DefaultQueueSystemEndPoint(DefaultQueueSystemCollaborationManager qcManager)
   {
      super(qcManager);
      this.qcManager = qcManager;
      senderThread = new SenderThread(this.getName() + ".SenderThread");
   }

   /**
    * Creates a new DefaultQueueSystemEndPoint.
    * 
    * @param qcManager reference to a DefaultQueueSystemCollaborationManager.
    * @param name the name of this DefaultQueueSystemEndPoint.
    */
   public DefaultQueueSystemEndPoint(DefaultQueueSystemCollaborationManager qcManager, String name)
   {
      super(qcManager, name);
      this.qcManager = qcManager;
      senderThread = new SenderThread(this.getName() + ".SenderThread");
   }

   /**
    * Sets the DefaultQueueSystemEndPointProxy associated with this DefaultQueueSystemEndPoint.
    * 
    * @param proxy a DefaultQueueSystemEndPointProxy.
    */
   synchronized void setProxy(DefaultQueueSystemEndPointProxy proxy)
   {
      this.proxy = proxy;
   }

   /**
    * Gets a String describing this TcpEndPoint.
    * 
    * @return a String describing this TcpEndPoint.
    */
   public String getDescription()
   {
      if (this.isActive())
      {
         String remoteQueueSystemName = null;

         if (proxy != null) remoteQueueSystemName = proxy.getRemoteQueueSystemName();
         else
         {
            TcpEndPointIdentifier addr = this.getRemoteAddress();
            remoteQueueSystemName = (addr != null) ? addr.getAddressAsString() : "unknown";
         }

         if (!isConnected()) return "DefaultQueueSystemEndPoint: Connecting to " + remoteQueueSystemName + ".";
         else if (isConnected() && !isLinkEstablished()) return "DefaultQueueSystemEndPoint: Connected to " + remoteQueueSystemName + ".";
         else
         {
            return "DefaultQueueSystemEndPoint: Link established with " + remoteQueueSystemName + ".";
         }
      }
      else return "DefaultQueueSystemEndPoint: Inactive";
   }

   /**
    * Sends a message to the queue system to which this DefaultQueueSystemEndPoint object is connected.
    * 
    * @param message a message to be sent.
    * @exception IOException if an error occurs while writing to a socket.
    */
   protected void sendCommand(Object message) throws IOException
   {
      writeObject(message);
   }

   /**
    * Disconnects this DefaultQueueSystemEndPoint from the remote queue system it is connected to.
    */
   public synchronized void disconnect()
   {
      if (this.proxy != null) this.proxy.logInfo("Disconnecting endpoint.");
      else logInfo("Disconnecting.");

      try
      {
         sendCommand(new DefaultDisconnectCommand());
      }
      catch (Exception e)
      {
      }

      try
      {
         Thread.sleep(100);
      }
      catch (Exception e)
      {
      }

      doDisconnect();
   }

   /**
    */
   private void doDisconnect()
   {
      super.disconnect();
   }

   /**
    * Called when a disconnection command is received from the other end.
    */
   public synchronized void disconnectionCommandReceived()
   {
      if (this.proxy != null) this.proxy.logInfo("Disconnection command received.");
      else logInfo("Disconnection command received.");

      Thread.yield();

      doDisconnect();
   }

   /**
    * Resets various data.
    */
   private void resetData()
   {
      proxy = null;
   }

   /**
    * Clean up method for this DefaultQueueSystemEndPoint.
    */
   public void cleanUp()
   {
      String currentName = this.getName(); // Get the name of the endpoint before calling super.cleanUp(), because the
                                             // latter resets the name...
      super.cleanUp();

      if (senderThread.isInitialized()) // If the senderThread is still processing, interrupt it
      {
         senderThread.interrupt();
         try
         {
            senderThread.waitForDeinitialization(1000);
         }
         catch (InterruptedException e)
         {
         }
      }

      if (proxy != null)
      {
         this.proxy.logInfo("Endpoint disconnected.");
         proxy.endPointDisconnected(this);
         proxy = null;
      }
      else
      {
         this.rename(currentName); // Rename before logging
         logInfo("Disconnected.");
      }
      this.rename(getBaseName()); // Reset the name
   }

   /**
    * Destroys this DefaultQueueSystemEndPoint. This method is called when the DefaultQueueSystemEndPoint can no longer
    * be reused and is to be killed.
    */
   public void destroy()
   {
      super.destroy();

      if (senderThread != null) senderThread.destroy();
      senderThread = null;

      if (proxy != null)
      {
         proxy.endPointError(this);
         proxy = null;
      }

      qcManager = null;
   }

   /**
    * Method that validates the state of this DefaultQueueSystemEndPoint, to see if it can be reused.
    * 
    * @return <code>true</code> if validation was successful, otherwise <code>false</code>.
    */
   public boolean validate()
   {
      return (senderThread.check() && !senderThread.isInitialized());
   }

   /**
    * Fatal error in sender thread.
    */
   private final void senderThreadFatalError()
   {
      fatalError();
   }

   /**
    * Clean up method for the sender thread.
    */
   private void senderThreadCleanUp()
   {
      if (isConnected())
      {
         doDisconnect();
         thread.interrupt();
      }
   }

   /**
    * Checks this DefaultQueueSystemEndPoint for errors.
    * 
    * @return <code>true</code> if everything is OK, otherwise <code>false</code>.
    */
   public boolean check()
   {
      if (super.check())
      {
         return senderThread.check();
      }
      else return false;
   }

   /**
    * Client side initialization method run after attempts has been made to connect this endpoint. This method is
    * responsible for performing handshaking procedures if a connection has been successfully established. This involvs
    * sending information about this queue system to that of the remote endpoint which this object is connected to, as
    * well as receiving information about the remote queue system.
    * 
    * @param connectionSuccessful boolean flag indicating if connection was successful.
    * @return <code>true</code> if handshaking was successful, otherwise <code>false</code>.
    */
   protected boolean initClientSide(boolean connectionSuccessful)
   {
      resetData();

      impl = new ClientSideImpl();

      proxy = (DefaultQueueSystemEndPointProxy) connectionData.getCustomData(); // Get reference to the proxy, which
                                                                                 // was transferred through the tcp
                                                                                 // connection data

      if (proxy == null)
      {
         logError("Invalid initialization of Endpoint! Custom data didn't contain reference to DefaultQueueSystemEndPointProxy!");
         return false;
      }

      if (!proxy.waitForInit()) // Wait for proxy to get initialized and check if proxy initialization failed
      {
         logError("Initialization of Endpoint failed!");
         return false;
      }

      proxy.endPointConnected(connectionSuccessful, this);

      if (connectionSuccessful)
      {
         return impl.performPostConnectionNegotiation();
      }
      else return false;
   }

   /**
    * Server side initialization method run after a connection request has been accepted. This method is responsible for
    * performing handshaking procedures, which involves receiving information about the queue system to which this
    * endpoint is connected, as well as sending information about this queue system.
    * 
    * @return <code>true</code> if handshaking was successful, otherwise <code>false</code>.
    */
   protected boolean initServerSide()
   {
      resetData();

      impl = new ServerSideImpl();

      proxy = (DefaultQueueSystemEndPointProxy) connectionData.getCustomData(); // Get reference to the proxy, which
                                                                                 // was transferred through the
                                                                                 // connection data

      if (proxy == null)
      {
         logError("Invalid initialization of Endpoint! Custom data didn't contain reference to DefaultQueueSystemEndPointProxy!");
         return false;
      }

      if (!proxy.waitForInit()) // Wait for proxy to get initialized and check if proxy initialization failed
      {
         logError("Initialization of Endpoint failed!");
         return false;
      }

      proxy.endPointConnected(true, this);

      return impl.performPostConnectionNegotiation();
   }

   /**
    * Client side end point implementation.
    */
   protected final void runClientSideImpl()
   {
      runCommonImpl();
   }

   /**
    * Server side end point implementation.
    */
   protected final void runServerSideImpl()
   {
      runCommonImpl();
   }

   /**
    * Common implementation method.
    */
   private final void runCommonImpl()
   {
      if (isConnected() && isLinkEstablished())
      {
         this.rename(getBaseName() + "(" + proxy.getRemoteQueueSystemName() + ")");

         senderThread.initialize(); // Wake up the sender thread
         receiveCommands(); // Read and execute incomming commands
      }
   }

   /**
    * Method for receiving commands.
    */
   private final void receiveCommands()
   {
      Command command = null;

      // Read and execute incomming commands
      while (isConnected())
      {
         try
         {
            command = (Command) readObject();

            if ((command != null) && (isConnected()))
            {
               try
               {
                  command.execute(proxy);
                  command = null;
               }
               catch (Exception e)
               {
                  if (proxy != null) this.proxy.logError("Error while executing command (" + command + ").", e);
                  else logError("Error while executing command (" + command + ").", e);
               }
            }
         }
         catch (ClassCastException cce)
         {
            if (proxy != null) this.proxy.logError("Received object was not a subclass of com.teletalk.jserver.queue.command.Command! Exception is: " + cce);
            else logError("Received object was not a subclass of com.teletalk.jserver.queue.command.Command! Exception is: " + cce);
         }
         catch (InvalidObjectException ioe)
         {
            if (proxy != null) this.proxy.logError("Unable to receive command (" + command + ")", ioe);
            else logError("Unable to receive command (" + command + ")", ioe);
         }
         catch (InvalidClassException ice)
         {
            if (proxy != null) this.proxy.logError("Unable to receive command (" + command + ")", ice);
            else logError("Unable to receive command (" + command + ")", ice);
         }
         catch (ClassNotFoundException cnfe)
         {
            if (proxy != null) this.proxy.logError("Unable to receive command (" + command + ")", cnfe);
            else logError("Unable to receive command (" + command + ")", cnfe);
         }
         catch (IOException ioe)
         {
            if (isConnected())
            {
               if (proxy != null)
               {
                  if (super.isDebugMode()) this.proxy.logError("Fatal communication error (" + ioe + ") while receiving command! Aborting!", ioe);
                  else this.proxy.logError("Fatal communication error (" + ioe + ") while receiving command! Aborting!");
               }
               else
               {
                  if (super.isDebugMode()) logError("Fatal communication error (" + ioe + ") while receiving command! Aborting!", ioe);
                  else logError("Fatal communication error (" + ioe + ") while receiving command! Aborting!");
               }

               if (proxy != null)
               {
                  proxy.endPointError(this);
                  // proxy = null;
               }
            }
            return;
         }
         catch (Exception e)
         {
            if (isConnected())
            {
               if (proxy != null) this.proxy.logError("Fatal error while receiving command! Aborting!", e);
               else logError("Fatal error while receiving command! Aborting!", e);

               if (proxy != null)
               {
                  proxy.endPointError(this);
                  // proxy = null;
               }
            }
            return;
         }
      }
   }

   /**
    * Method for sending commands.
    */
   private final void sendCommands()
   {
      Command command = null;

      // Get outgoing messages from proxy and dispatch them
      while (isConnected())
      {
         try
         {
            command = proxy.getNextOutoingCommand();
         }
         catch (InterruptedException ie)
         {
         }

         if (isConnected())
         {
            if (command == null)
            {
               if (proxy != null) this.proxy.logWarning("Unable to send command. Null command!");
               else logWarning("Unable to send command. Null command!");
               continue;
            }

            try
            {
               this.sendCommand(command);

               if (command instanceof QueueSystemCommand) qcManager.commandDeliveryReport((QueueSystemCommand) command, true);
            }
            catch (InvalidClassException ice)
            {
               if (proxy != null) this.proxy.logError("Unable to send command (" + command + ")", ice);
               else logError("Unable to send command (" + command + ")", ice);

               if (command instanceof QueueSystemCommand) qcManager.commandDeliveryReport((QueueSystemCommand) command, false);
            }
            catch (NotSerializableException nse)
            {
               if (proxy != null) this.proxy.logError("Unable to send command (" + command + ")", nse);
               else logError("Unable to send command (" + command + ")", nse);

               if (command instanceof QueueSystemCommand) qcManager.commandDeliveryReport((QueueSystemCommand) command, false);
            }
            catch (IOException ioe)
            {
               // Put command back in queue, so it can be dispatched again if this end point gets reconnected
               if ((command != null) && (command instanceof QueueSystemCommand) && (proxy != null))
               {
                  proxy.dispatchUrgentCommand((QueueSystemCommand) command);
               }

               if (isConnected())
               {
                  if (proxy != null) this.proxy.logError("Communication error (" + ioe + ") while sending command (" + command + ")! Aborting!");
                  else logError("Communication error (" + ioe + ") while sending command (" + command + ")! Aborting!");

                  if (proxy != null)
                  {
                     proxy.endPointError(this);
                     // proxy = null;
                  }
               }

               return;
            }
            catch (Throwable e)
            {
               // Put command back in queue, so it can be dispatched again if this end point gets reconnected
               if ((command != null) && (command instanceof QueueSystemCommand) && (proxy != null))
               {
                  proxy.dispatchUrgentCommand((QueueSystemCommand) command);
               }

               if (isConnected())
               {
                  if (proxy != null) this.proxy.logError("Error while sending command (" + command + ")! Aborting!", e);
                  else logError("Error while sending command (" + command + ")! Aborting!", e);

                  if (proxy != null)
                  {
                     proxy.endPointError(this);
                     // proxy = null;
                  }
               }
               
               if( e instanceof Error ) throw (Error)e;

               return;
            }
            finally
            {
               command = null;
            }
         }
         else if ((command != null) && (command instanceof QueueSystemCommand))
         {
            // Put command back in queue, so it can be dispatched again if this end point gets reconnected
            if (proxy != null) proxy.dispatchUrgentCommand((QueueSystemCommand) command);
         }
      }
   }

   /**
    * Internal mode implementation (client or server) interface.
    */
   private interface DefaultQueueSystemEndPointImpl
   {

      /**
       * Performs handshaking (post connection negotioation).
       */
      public boolean performPostConnectionNegotiation();
   }

   /**
    * Internal clientside implementation.
    */
   private final class ClientSideImpl implements DefaultQueueSystemEndPointImpl
   {

      /**
       * Performs handshaking (post connection negotioation).
       */
      public boolean performPostConnectionNegotiation()
      {
         Object msg;
         QueueSystemMetaData remoteQueueSystemMetaData = null;
         boolean success = false;
         boolean errorOccurred = false;
         String errorString = "Error while waiting for DefaultConnectResponse! Aborting!";
         boolean hasInQueue = (proxy.getQueueManager().getInQueue() != null);
         boolean hasOutQueue = (proxy.getQueueManager().getOutQueue() != null);

         try
         {
            // Send connect command
            sendCommand(new DefaultConnectCommand(qcManager.getQueueSystemMetaData(), hasInQueue, hasOutQueue));

            // Wait for connect respons/reply
            msg = readObject();

            if (msg instanceof DefaultConnectResponse)
            {
               errorString = "Error while negotiating! Aborting!";

               DefaultConnectResponse command = (DefaultConnectResponse) msg;

               remoteQueueSystemMetaData = command.getQueueSystemMetaData();
               boolean rqsHasInQueue = command.hasInQueue();
               boolean rqsHasOutQueue = command.hasOutQueue();

               boolean establishLinkResult = false;

               if (proxy != null) establishLinkResult = proxy.establishLink(remoteQueueSystemMetaData, DefaultQueueSystemEndPoint.this);

               if (establishLinkResult) // If response contains meta data, the connect attempt was successful.
               {
                  errorString = "Error while synchronizing! Aborting!";

                  boolean synchSuccess = false;

                  if (rqsHasInQueue && hasOutQueue) // If the remote queue system has an in queue and this queue
                                                      // system has an out queue...
                  {
                     // ...synchronize the out queue of this queue system with the in queue of the remote queue system
                     sendCommand(proxy.initiateQueueSystemSynchronization(DefaultQueueSystemEndPoint.this));

                     // Read response
                     msg = readObject();
                     if (msg instanceof QueueSystemSynchronizationResponse)
                     {
                        proxy.queueSystemSynchronizationResponseReceived((QueueSystemSynchronizationResponse) msg, DefaultQueueSystemEndPoint.this);
                     }
                     else
                     {
                        if (proxy != null) proxy.logError("Expected QueueSystemSynchronizationResponse but got " + (msg != null ? msg.getClass().getName() : "null")
                              + " instead. Aborting!");
                        else logError("Expected QueueSystemSynchronizationResponse but got " + (msg != null ? msg.getClass().getName() : "null") + " instead. Aborting!");

                        errorOccurred = true;
                     }
                  }

                  if (!errorOccurred && rqsHasOutQueue && hasInQueue) // If no errrors occurred and the remote queue
                                                                        // system has an out queue and this queue system
                                                                        // has an in queue...
                  {
                     // ...read request for synchronization of the in queue of this queue system with the out queue of
                     // the remote queue system
                     msg = readObject();

                     if (msg instanceof QueueSystemSynchronizationRequest)
                     {
                        QueueSystemSynchronizationResponse response = proxy.queueSystemSynchronizationRequestReceived((QueueSystemSynchronizationRequest) msg, DefaultQueueSystemEndPoint.this);

                        // Send response
                        sendCommand(response);

                        synchSuccess = true;
                     }
                     else
                     {
                        if (proxy != null) proxy.logError("Expected QueueSystemSynchronizationRequest but got " + (msg != null ? msg.getClass().getName() : "null")
                              + " instead. Aborting!");
                        else logError("Expected QueueSystemSynchronizationRequest but got " + (msg != null ? msg.getClass().getName() : "null") + " instead. Aborting!");
                     }
                  }
                  else if (!errorOccurred) synchSuccess = true;

                  if (synchSuccess)
                  {
                     proxy.linkEstablished(DefaultQueueSystemEndPoint.this);
                     success = true;
                  }
               }
            }
            else
            {
               if (proxy != null) proxy.logError("Expected DefaultConnectResponse but got " + (msg != null ? msg.getClass().getName() : "null") + " instead. Aborting!");
               else logError("Expected DefaultConnectResponse but got " + (msg != null ? msg.getClass().getName() : "null") + " instead. Aborting!");
            }
         }
         catch (SocketException se)
         {
            if (isConnected())
            {
               if (proxy != null) proxy.logError("Communication error (" + se + ") while waiting for DefaultConnectResponse! Aborting!");
               else logError("Communication error (" + se + ") while waiting for DefaultConnectResponse! Aborting!");
            }
         }
         catch (Exception e)
         {
            if (isConnected())
            {
               if (proxy != null) proxy.logError(errorString, e);
               else logError(errorString, e);
            }
         }
         return success;
      }
   }

   /**
    * Internal serverside implementation.
    */
   private final class ServerSideImpl implements DefaultQueueSystemEndPointImpl
   {

      /**
       * Performs handshaking (post connection negotioation).
       */
      public boolean performPostConnectionNegotiation()
      {
         Object msg;
         boolean success = false;
         boolean errorOccurred = false;
         String errorString = "Error while waiting for DefaultConnectCommand! Aborting!";
         boolean hasInQueue = (proxy.getQueueManager().getInQueue() != null);
         boolean hasOutQueue = (proxy.getQueueManager().getOutQueue() != null);

         try
         {
            // Wait for connect command
            msg = readObject();

            if (msg instanceof DefaultConnectCommand)
            {
               errorString = "Error while negotiating! Aborting!";
               DefaultConnectCommand command = (DefaultConnectCommand) msg;
               boolean rqsHasInQueue = command.hasInQueue();
               boolean rqsHasOutQueue = command.hasOutQueue();

               QueueSystemMetaData metaData = command.getQueueSystemMetaData();

               boolean establishLinkResult = false;

               if (proxy != null) establishLinkResult = proxy.establishLink(metaData, DefaultQueueSystemEndPoint.this);

               if (establishLinkResult)
               {
                  errorString = "Error while synchronizing! Aborting!";

                  DefaultQueueSystemEndPoint.this.rename(DefaultQueueSystemEndPoint.this.getBaseName() + "(" + proxy.getRemoteQueueSystemName() + ")");

                  // Send connect response
                  DefaultConnectResponse d = new DefaultConnectResponse(qcManager.getQueueSystemMetaData(), (proxy.getQueueManager().getInQueue() != null),
                        (proxy.getQueueManager().getOutQueue() != null));

                  sendCommand(d);

                  boolean synchSuccess = false;

                  if (rqsHasOutQueue && hasInQueue) // If the remote queue system has an out queue and this queue
                                                      // system has an in queue...
                  {
                     // ...read request for synchronization of the in queue of this queue system with the out queue of
                     // the remote queue system
                     msg = readObject();

                     if (msg instanceof QueueSystemSynchronizationRequest)
                     {
                        QueueSystemSynchronizationResponse response = proxy.queueSystemSynchronizationRequestReceived((QueueSystemSynchronizationRequest) msg, DefaultQueueSystemEndPoint.this);

                        // Send response
                        sendCommand(response);
                     }
                     else
                     {
                        if (proxy != null) proxy.logError("Expected QueueSystemSynchronizationRequest but got " + (msg != null ? msg.getClass().getName() : "null")
                              + " instead. Aborting!");
                        else logError("Expected QueueSystemSynchronizationRequest but got " + (msg != null ? msg.getClass().getName() : "null") + " instead. Aborting!");

                        errorOccurred = true;
                     }
                  }

                  if (!errorOccurred && rqsHasInQueue && hasOutQueue) // If no errrors occurred and the remote queue
                                                                        // system has an in queue and this queue system
                                                                        // has an out queue...
                  {
                     // Synchronize the out queue of this queue system with the in queue of the remote queue system
                     sendCommand(proxy.initiateQueueSystemSynchronization(DefaultQueueSystemEndPoint.this));

                     // Read response
                     msg = readObject();

                     if (msg instanceof QueueSystemSynchronizationResponse)
                     {
                        proxy.queueSystemSynchronizationResponseReceived((QueueSystemSynchronizationResponse) msg, DefaultQueueSystemEndPoint.this);
                        synchSuccess = true;
                     }
                     else
                     {
                        if (proxy != null) proxy.logError("Expected QueueSystemSynchronizationResponse but got " + (msg != null ? msg.getClass().getName() : "null")
                              + " instead. Aborting!");
                        else logError("Expected QueueSystemSynchronizationResponse but got " + (msg != null ? msg.getClass().getName() : "null") + " instead. Aborting!");
                     }
                  }
                  else if (!errorOccurred) synchSuccess = true;

                  if (synchSuccess)
                  {
                     proxy.linkEstablished(DefaultQueueSystemEndPoint.this);
                     success = true;
                  }
               }
               else
               {
                  sendCommand(new DefaultConnectResponse());
               }
            }
            else
            {
               if (proxy != null) proxy.logError("Expected DefaultConnectResponse but got " + (msg != null ? msg.getClass().getName() : "null") + " instead. Aborting!");
               else logError("Expected DefaultConnectResponse but got " + (msg != null ? msg.getClass().getName() : "null") + " instead. Aborting!");
            }
         }
         catch (SocketException se)
         {
            if (isConnected())
            {
               if (proxy != null) proxy.logError("Communication error (" + se + ") while waiting for DefaultConnectCommand! Aborting!");
               else logError("Communication error (" + se + ") while waiting for DefaultConnectCommand! Aborting!");
            }
         }
         catch (Exception e)
         {
            if (isConnected())
            {
               if (proxy != null) proxy.logError(errorString, e);
               else logError(errorString, e);
            }
         }

         return success;
      }
   }

   /**
    * Internal thread used to send commands.
    */
   private final class SenderThread extends Thread
   {

      private boolean initialized = false;

      private boolean canRun = true;

      private final Object initializationMonitor = new Object();

      public SenderThread(String name)
      {
         super(name);
         setDaemon(true);
         start();
      }

      public synchronized void initialize()
      {
         initialized = true;
         synchronized (initializationMonitor)
         {
            initializationMonitor.notify();
         }
      }

      public boolean isInitialized()
      {
         return initialized;
      }

      public synchronized void waitForDeinitialization() throws InterruptedException
      {
         while (initialized)
            wait();
      }

      //return true if deinitialized.
      public synchronized boolean waitForDeinitialization(long maxWait) throws InterruptedException
      {
         long waitStart = System.currentTimeMillis();
         long waitTime;

         while (initialized)
         {
            waitTime = maxWait - (System.currentTimeMillis() - waitStart);

            if (waitTime > 0) wait(waitTime);
            else break;
         }

         return !initialized;
      }

      private synchronized void deinitialized()
      {
         initialized = false;
         notifyAll();
      }

      // Makes the calling thread wait if there is no data.
      private void waitForInitialization()
      {
         try
         {
            while (!initialized)
            {
               synchronized (initializationMonitor)
               {
                  initializationMonitor.wait();
               }
            }
         }
         catch (InterruptedException e)
         {
         }
      }

      public void run()
      {
         try
         {
            while (canRun)
            {
               waitForInitialization();

               if (initialized && canRun)
               {
                  sendCommands();
                  deinitialized();
                  senderThreadCleanUp();
               }
               else deinitialized();
            }
         }
         finally
         {
            if (canRun) senderThreadFatalError();
         }
      }

      public boolean check()
      {
         return canRun && isAlive();
      }

      public void destroy()
      {
         canRun = false;

         try
         {
            this.interrupt();
         }
         catch (Exception e)
         {
         }
      }
   }
}
