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
package com.teletalk.jserver.queue;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.log4j.Level;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.TestUtils;
import com.teletalk.jserver.messaging.MessagingManagerTest;
import com.teletalk.jserver.util.FileDeletor;
import com.teletalk.jserver.util.MessageQueueTest;

/**
 * 
 * @author Tobias Löfstrand
 */
public class QueueManagerTest extends TestCase
{
   private static final Log logger = LogFactory.getLog(MessagingManagerTest.class);
   
   
   private static final int NO_OF_TESTS = 1;
   
   private static int testCount = 0;
   
   
   
   private static JServer server = null;
      
   
   public static final int N_ITEMS = 1000;
      
   
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
   
   /**
    */
   protected void setUp() throws Exception
   {
      super.setUp();
      
      if( server == null )
      {
         logger.info("QueueManagerTest.setUp() - Creating " + TestUtils.TEST_SERVER_NAME + "! (" + JServer.getVersionString() + ")");
         
         //server = JServer.createEmptyJServer(TestUtils.TEST_SERVER_NAME, false);
         //server.createDefaultFileLogger();
         server = new JServer(TestUtils.TEST_SERVER_NAME);
                           
         server.startJServer();
         
         server.waitForEnabled(1000);
         
         logger.info("QueueManagerTest.setUp() - " + TestUtils.TEST_SERVER_NAME + " started!");
      }
   }
      
   /**
    */
   protected void tearDown() throws Exception
   {
      if( testCount >= NO_OF_TESTS )
      {
         logger.info("QueueManagerTest.tearDown() - destroying server.");
         
         super.tearDown();
         
         server.destroyJServer(30000);
         server = null;
         
         logger.info("QueueManagerTest.tearDown() - server destroyed.");
      }
   }
   
   
   /* ### TEST METHODS ### */
   
   
   /**
    */
   public void testQueueManagerCrash()
   {
      logger.info("BEGIN testQueueManagerCrash.");
      
      testCount++;
      
      FileDeletor.delete(ReceiverController.QUEUE_ITEM_IN_QUEUE_PATH);
      FileDeletor.delete(RelayController.QUEUE_ITEM_IN_QUEUE_PATH);
      FileDeletor.delete(RelayController.QUEUE_ITEM_OUT_QUEUE_PATH);
      FileDeletor.delete(SendController.QUEUE_ITEM_OUT_QUEUE_PATH);
      
      ReceiverController receiverController = new ReceiverController(server);
      server.addSubSystem(receiverController);
      //receiverController.setLogLevel(Level.DEBUG);      
      RelayController relayController = new RelayController(server);
      server.addSubSystem(relayController);
      //relayController.setLogLevel(Level.DEBUG);
      SendController sendController = new SendController(server);
      server.addSubSystem(sendController);
      
      try
      {
         receiverController.engage();
         try{
         receiverController.waitForEnabled(10000);
         }catch(Exception e){}
         
         relayController.engage();
         try{
         relayController.waitForEnabled(10000);
         }catch(Exception e){}
         
         sendController.engage();
         try{
         sendController.waitForEnabled(10000);
         }catch(Exception e){}
         
         for(int i=0; i<(N_ITEMS/2); i++)
         {
            sendController.queueJob(new TestQueueItemData("Test" + i));
         }
         
         sendController.waitForCompletionEvent((N_ITEMS/4), 30000);
         
         relayController.shutDown();
         try{
         relayController.waitForDown(1000);
         }catch (Exception e) {}
         relayController.queueMessagingManager.shutDown();
         try{
         relayController.queueMessagingManager.waitForDown(10000);
         }catch (Exception e) {}
         relayController.getQueueManager().shutDown();
         try{
         relayController.getQueueManager().waitForDown(10000);
         }catch (Exception e) {}
         
         relayController.engage();
         try{
         relayController.waitForEnabled(10000);
         }catch(Exception e){}
         
         for(int i=0; i<(N_ITEMS/2); i++)
         {
            sendController.queueJob(new TestQueueItemData("Test" + i));
         }
         
         if( !sendController.waitForCompletionEvent(N_ITEMS, 300000) )
         {
            super.fail("To few items completed (" + sendController.getCompleteCount() + ")!");
         }
         if( getFailCount() > 0 )
         {
            super.fail("Error in testQueueManagerCrash - failCount: " + getFailCount() + ".");
         }
         if( receiverController.getProcessedItemsCount() > (N_ITEMS + 10) )
         {
            super.fail("To many items processed: " + receiverController.getProcessedItemsCount() + ".");
         }
         if( !receiverController.checkCompletionResponseCache() )
         {
            super.fail("To many responses in cache (" + receiverController.getActualCompletionResponseCacheSize() + ")!");
         }
      }
      finally
      {
         receiverController.shutDown();
         relayController.shutDown();
         sendController.shutDown();
         try{
            receiverController.waitForDown(10000);
         }catch(InterruptedException ie){}
         try{
            relayController.waitForDown(10000);
         }catch(InterruptedException ie){}
         try{
            sendController.waitForDown(10000);
         }catch(InterruptedException ie){}
         
         server.removeSubSystem(receiverController);
         server.removeSubSystem(relayController);
         server.removeSubSystem(sendController);
         
         FileDeletor.delete(ReceiverController.QUEUE_ITEM_IN_QUEUE_PATH);
         FileDeletor.delete(RelayController.QUEUE_ITEM_IN_QUEUE_PATH);
         FileDeletor.delete(RelayController.QUEUE_ITEM_OUT_QUEUE_PATH);
         FileDeletor.delete(SendController.QUEUE_ITEM_OUT_QUEUE_PATH);
      }
      
      logger.info("END testQueueManagerCrash.");
   }
}
