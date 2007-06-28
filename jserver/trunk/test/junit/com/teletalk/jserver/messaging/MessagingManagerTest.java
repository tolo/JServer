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
package com.teletalk.jserver.messaging;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.TestUtils;
import com.teletalk.jserver.tcp.TcpEndPointIdentifier;
import com.teletalk.jserver.tcp.messaging.Message;
import com.teletalk.jserver.tcp.messaging.MessageHeader;
import com.teletalk.jserver.tcp.messaging.MessageReceiver;
import com.teletalk.jserver.tcp.messaging.MessagingManager;
import com.teletalk.jserver.util.Future;
import com.teletalk.jserver.util.MessageQueueTest;

/**
 * 
 * @author Tobias Löfstrand
 */
public class MessagingManagerTest extends TestCase
{
   private static final Log logger = LogFactory.getLog(MessagingManagerTest.class);
   
   
   private static final int NO_OF_TESTS = 5;
   
   private static int testCount = 0;
   
   
   
   private static JServer server = null;
   
   
   private static final String RECEIVER_NAME = "receiver";
   
   private static final String PROXIED_RECEIVER_NAME = "proxied";

   
   public static MessagingManager messagingManagerS1;
   
   private static MessagingManager messagingManagerR1;
   
   private static MessagingManager messagingManagerR2;
   
   private static MessagingManager messagingManagerR3;
   
   
   private static int failCount = 0;
   
   public static void resetFailCount()
   {
      synchronized (MessageQueueTest.class)
      {
         failCount = 0;
      }
   }
   
   public static void incrementFailCount()
   {
      synchronized (MessageQueueTest.class)
      {
         failCount++;
      }
   }
   
   public static int getFailCount()
   {
      synchronized (MessageQueueTest.class)
      {
         return failCount;
      }
   }
   
   //private int receivedResponseCounter = 0;
   
   //private final Object receivedResponseMonitor = new Object();
   
   private static Future[] receivedResponseMonitors;
   
   
   
   /**
    */
   protected void setUp() throws Exception
   {
      super.setUp();
      
      if( server == null )
      {
         logger.info("MessagingManagerTest.setUp() - Creating " + TestUtils.TEST_SERVER_NAME + "! (" + JServer.getVersionString() + ")");
         
         server = JServer.createEmptyJServer(TestUtils.TEST_SERVER_NAME, false);
         server.createDefaultFileLogger();
                  
         logger.info("QueueManagerTest.setUp() - JServer name:  " + JServer.getJServer().getName());
                  
         messagingManagerR1 = new MessagingManager(server, "MessagingManagerR1");
         InternalMessageReceiver internalMessageReceiver1 = new InternalMessageReceiver(messagingManagerR1);
         messagingManagerR1.setDefaultMessageReceiver(internalMessageReceiver1);
         messagingManagerR1.registerMessageReceiver(internalMessageReceiver1, RECEIVER_NAME);
         messagingManagerR1.addServerAddress("localhost", 11231);
         //messagingManagerR1.setLogLevel(Level.DEBUG);
         server.addSubSystem(messagingManagerR1);
         
         messagingManagerR2 = new MessagingManager(server, "MessagingManagerR2");
         InternalMessageReceiver internalMessageReceiver2 = new InternalMessageReceiver(messagingManagerR2);
         messagingManagerR2.setDefaultMessageReceiver(internalMessageReceiver2);
         messagingManagerR2.registerMessageReceiver(internalMessageReceiver2, RECEIVER_NAME);
         messagingManagerR2.addServerAddress("localhost", 11232);
         messagingManagerR2.setProxyingEnabled(true); // Enable proxying
         //messagingManagerR2.setLogLevel(Level.DEBUG);
         server.addSubSystem(messagingManagerR2);
         
         messagingManagerR3 = new MessagingManager(server, "MessagingManagerR3");
         messagingManagerR3.registerMessageReceiver(new InternalMessageReceiver(messagingManagerR3), PROXIED_RECEIVER_NAME);
         messagingManagerR3.addServerAddress("localhost", 11233);
         messagingManagerR3.addDestination("localhost", 11232); // MessagingManagerR2
         //messagingManagerR2.setLogLevel(Level.DEBUG);
         server.addSubSystem(messagingManagerR3);
         
         messagingManagerS1 = new MessagingManager(server, "MessagingManagerS1");
         messagingManagerS1.setDefaultMessageReceiver(new AsynchResponseReciver());
         messagingManagerS1.setResponseMessageTimeOut(2000);
         messagingManagerS1.addDestination("localhost", 11231);
         messagingManagerS1.addDestination("localhost", 11232);
         messagingManagerS1.setConnectionsPerDestination(2);
         //messagingManagerS1.setLogLevel(Level.DEBUG);
         server.addSubSystem(messagingManagerS1);
         
         server.startJServer();
         
         messagingManagerS1.waitForEnabled(10000);
         messagingManagerR1.waitForEnabled(10000);
         messagingManagerR2.waitForEnabled(10000);
         messagingManagerR3.waitForEnabled(10000);
         
         logger.info("MessagingManagerTest.setUp() - " + TestUtils.TEST_SERVER_NAME + " started!");
      }      
   }

   /**
    */
   protected void tearDown() throws Exception
   {
      if( testCount >= NO_OF_TESTS )
      {
         logger.info("MessagingManagerTest.tearDown() - destroying server.");
         
         super.tearDown();
         
         server.destroyJServer(30000);
         server = null;
         
         logger.info("MessagingManagerTest.tearDown() - server destroyed.");
      }
   }
   
   
   /* ### TEST METHODS ### */
   
   
   /**
    * Test case dispatchConcurrent.
    */
   public void testDispatchConcurrent()
   {
      logger.info("BEGIN testDispatchConcurrent.");
      
      testCount++;
      resetFailCount();
      
      server.logInfo("Starting test testDispatchConcurrent(1/3)!");
      dispatchConcurrentInternal(true, false, null);
      server.logInfo("Test testDispatchConcurrent(1/3) complete!");
      
      server.logInfo("Starting test testDispatchConcurrent(2/3)!");
      dispatchConcurrentInternal(false, false, null);
      server.logInfo("Test testDispatchConcurren(2/3) complete!");
      
      server.logInfo("Starting test testDispatchConcurrent(3/3)!");
      dispatchConcurrentInternal(false, false, RECEIVER_NAME);
      server.logInfo("Test testDispatchConcurren(3/3) complete!");
      
      logger.info("END testDispatchConcurrent.");
   }
   
   /**
    * Test case dispatchConcurrentAsynch.
    */
   public void testDispatchConcurrentAsynch()
   {
      logger.info("BEGIN dispatchConcurrentAsynch.");
            
      testCount++;
      resetFailCount();
           
      dispatchConcurrentInternal(true, true, null);
            
      logger.info("END dispatchConcurrentAsynch.");
   }
   
   /**
    * Test case dispatchViaProxy.
    */
   public void testDispatchViaProxy()
   {
      logger.info("BEGIN testDispatchViaProxy.");
            
      testCount++;
      resetFailCount();
            
      dispatchConcurrentInternal(false, false, PROXIED_RECEIVER_NAME);
      
      logger.info("END testDispatchViaProxy.");
   }
   
   /**
    * Test case defaultEndPointSelectionStrategy.
    */
   public void testDefaultEndPointSelectionStrategy()
   {
      logger.info("BEGIN testDefaultEndPointSelectionStrategy.");
            
      testCount++;
      resetFailCount();
            
      if( !DefaultEndPointSelectionStrategyTest.testDefaultEndPointSelectionStrategy() )
      {
         super.fail("Failure testDefaultEndPointSelectionStrategy! FailCount = " + getFailCount() + ". See log file for details.") ;
      }
      
      logger.info("END testDefaultEndPointSelectionStrategy.");
   }
   
   /**
    * Test case badData.
    */
   public void testBadData()
   {
      logger.info("BEGIN testBadData.");
            
      testCount++;
      resetFailCount();
      
      server.logInfo("testBadData - dispatching broken object messages.");
      dispatchConcurrentInternal(false, false, null, true, false, 1, 2); // Bad data
      server.logInfo("testBadData - broken object messages dispatched.");
      
      server.logInfo("testBadData - dispatching normal object messages.");
      dispatchConcurrentInternal(false, false, null, 1, 2);
      server.logInfo("testBadData - normal object messages dispatched.");

      logger.info("END testBadData.");
   }
   
   
   /* ### INTERNAL TEST METHODS ### */
   
   
   /**
    */
   private void dispatchConcurrentInternal(final boolean anyDestination, final boolean asynch, final String receiverName)
   {
      dispatchConcurrentInternal(anyDestination, asynch, receiverName, false, true);
   }

   /**
    */
   private void dispatchConcurrentInternal(final boolean anyDestination, final boolean asynch, final String receiverName, final int repetitions, final int nThreads)
   {
      dispatchConcurrentInternal(anyDestination, asynch, receiverName, false, true, repetitions, nThreads);
   }   
   
   /**
    */
   private void dispatchConcurrentInternal(final boolean anyDestination, final boolean asynch, final String receiverName, final boolean badData, final boolean checkFailCount)
   {
      dispatchConcurrentInternal(anyDestination, asynch, receiverName, false, true, 10, 10);
   }
   
   /**
    */
   private void dispatchConcurrentInternal(final boolean anyDestination, final boolean asynch, final String receiverName, final boolean badData, final boolean checkFailCount, final int repetitions, final int nThreads)
   {
      SenderThread senderThreads[] = new SenderThread[nThreads];
      receivedResponseMonitors = new Future[senderThreads.length];
      for(int i=0; i<senderThreads.length; i++)
      {
         receivedResponseMonitors[i] = new Future();
      }
      
      int n = repetitions;
      int counter = 0;
      //int expectedResponsCount = n * senderThreads.length;
      
      resetFailCount();
      
      for(int q=0; q<n; q++)
      {
         try
         {
            for(int i=0; i<senderThreads.length; i++)
            {
               senderThreads[i] = new SenderThread(i, counter, anyDestination, asynch, receiverName, badData);
               senderThreads[i].setDaemon(true);
               counter++;
            }
            
            for(int i=0; i<senderThreads.length; i++)
            {
               senderThreads[i].start();
            }
            
            for(int i=0; i<senderThreads.length; i++)
            {
               senderThreads[i].join(10000);
            }
            
            Thread.sleep(100);
         }
         catch(Exception e)
         {
            messagingManagerS1.logError("Errorbritt!", e);
            e.printStackTrace();
            incrementFailCount();
         }
      }
      
      if( checkFailCount && (getFailCount() > 0) )
      {
         super.fail("Failure! FailCount = " + getFailCount() + ". See log file for details.") ;
      }
   }
   
   /**
    */
   public static void main(String[] args)
   {
      junit.textui.TestRunner.run(MessagingManagerTest.class);
   }
   
   private static class InternalMessageReceiver implements MessageReceiver
   {
      private final MessagingManager messagingManager; 
      
      public InternalMessageReceiver(MessagingManager messagingManager)
      {
         this.messagingManager = messagingManager;
      }
      
      public void messageReceived(Message message)
   	{
         MessageHeader header = null;
         
   		try
   		{
   		   header = message.getHeader();
   		   
            this.messagingManager.logInfo("Received request : " + message.getBodyAsObject() + ". Header: " + header);
   										
   			Object response = new String("ResponseBritt"); 
   			
            this.messagingManager.logInfo("Sending response : " + response + ".");
   			
            this.messagingManager.dispatchMessageAsync(header, response);
   		}
   		catch(Exception e)
   		{
            this.messagingManager.logError("Error while dispatching response with header: " + header + "!", e);
   		}
   	}
   }
   
   private static class SenderThread extends Thread
   {
      private int threadId;
      
      private int id;
      
      private boolean anyDestination;
      
      private boolean asynch;
      
      private String receiverName;
      
      private boolean badData;
      
      
      public SenderThread(int threadId, int id, boolean anyDestination, boolean asynch, String receiverName, boolean badData)
      {
         this.threadId = threadId;
         this.id = id;
         this.anyDestination = anyDestination;
         this.asynch = asynch;
         this.receiverName = receiverName;
         this.badData = badData;
      }
      
      public void run()
      {
         long requestTime;
         
         try
         {
            MessageHeader header;
            Object command;
            Message response;
            
            header = new MessageHeader(id);
            header.setCustomHeaderField("threadId", new Integer(this.threadId));
            
            if( this.badData ) command = new BadExternalizable(id, "RequestBritt");
            else command = new String("RequestBritt");
            
            messagingManagerS1.logInfo("Sending request " + id + " : " + command + ".");
            
            requestTime = System.currentTimeMillis();
            if( asynch )
            {
               if( this.receiverName != null ) messagingManagerS1.dispatchMessageAsync(header, command, receiverName);
               else if( anyDestination ) messagingManagerS1.dispatchMessageAsync(header, command);
               else messagingManagerS1.dispatchMessageAsync(header, command, new TcpEndPointIdentifier("localhost", 11231));
               
               if( receivedResponseMonitors[threadId].getValue(10000) == null )
               {
                  incrementFailCount();
               }
            }
            else
            {
               if( this.receiverName != null ) response = messagingManagerS1.dispatchMessage(header, command, receiverName);
               else if( anyDestination ) response = messagingManagerS1.dispatchMessage(header, command);
               else response = messagingManagerS1.dispatchMessage(header, command, new TcpEndPointIdentifier("localhost", 11231));
               
               messagingManagerS1.logInfo("Received response for request " + id + " after " + (System.currentTimeMillis() - requestTime) + " ms : " + response.getBodyAsObject() + ". Header: " + response.getHeader());
            }
         }
         catch(Exception e)
         {
            messagingManagerS1.logError("Error while dispatching request " + id + "!", e);
            incrementFailCount();
         }
      }
   }
   
   private static class AsynchResponseReciver implements MessageReceiver 
   {
      public void messageReceived(Message message)
   	{
         MessageHeader header = null;
         
   		try
   		{
   		   header = message.getHeader();
   		   Integer threadId = (Integer)header.getCustomHeaderField("threadId");
   		      		      		   
   		   receivedResponseMonitors[threadId.intValue()].setValue(threadId);
   		      		   
   		   messagingManagerS1.logInfo("Received resonse : " + message.getBodyAsObject() + ". Header: " + header);
   		}
   		catch(Exception e)
   		{
   		   messagingManagerS1.logError("Error while receiving response with header: " + header + "!", e);
   		}
   	}
   }
}
