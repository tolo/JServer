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
package com.teletalk.jserver.periodic;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.teletalk.jserver.periodic.PeriodicAction;
import com.teletalk.jserver.periodic.PeriodicActionManager;

/**
 * 
 * @author Tobias Löfstrand
 */
public class PeriodicActionManagerTest extends TestCase {
   
   private static final Log logger = LogFactory.getLog(PeriodicActionManagerTest.class);

   
   public static final int N_ACTIONS = 200;
   
   public static final int N_REPETITIONS = 150;
   
   private static int actionsComplete = 0;
   
      
   private static void incrementActionsComplete()
   {
      synchronized (PeriodicActionManagerTest.class)
      {
         actionsComplete++;
      }
   }
     
   /**
    */
   public void testAbnormalActionExecution() throws Exception
   {
      logger.info("BEGIN testAbnormalActionExecution.");
      
      PeriodicActionManager pam = new PeriodicActionManager(null);
      pam.actionCheckInterval = 0;
      pam.actionExecutionWaitTime = 1;
      
      AbnormalAction abnormalAction = new AbnormalAction();
      pam.addPeriodicAction(abnormalAction);
      pam.engage();
      
      synchronized (abnormalAction) {
         while(!abnormalAction.running) abnormalAction.wait();
      }
      
      Thread.sleep(100);
      
      assertTrue(abnormalAction.isCriticalErrorDispatched());
      
      synchronized (abnormalAction) {
         abnormalAction.canRun = true;
         abnormalAction.notifyAll();
      }
      
      Thread.sleep(100);
      
      assertTrue(!abnormalAction.isCriticalErrorDispatched());
      
      logger.info("END testAbnormalActionExecution.");
   }
   
   /**
    */
   public void testActionExecutionMultiThreaded() throws Exception
   {
      logger.info("BEGIN testActionExecutionMultiThreaded.");
      
      PeriodicActionManager pam = new PeriodicActionManager(null);
      
      for(int i=0; i<N_ACTIONS; i++) 
      {
         pam.addPeriodicAction(new TestAction(pam, i));
      }
      
      pam.engage();
      pam.waitForEnabled(2000);
      
      PMStressThread[] stressThreads = new PMStressThread[10];
      
      for (int i = 0; i < stressThreads.length; i++) {
         stressThreads[i] = new PMStressThread(pam, i);
         stressThreads[i].start();
      }
      for(int i=0; i<50; i++) 
      {
         Thread.sleep(500);
         synchronized (PeriodicActionManagerTest.class)
         {
            if( actionsComplete == N_ACTIONS ) break;
         }
      }
      for (int i = 0; i < stressThreads.length; i++) {
         synchronized (stressThreads[i]) {
            stressThreads[i].canRun = false;
         }
      }
      
      synchronized (PeriodicActionManagerTest.class)
      {
         if( actionsComplete != N_ACTIONS ) super.fail("All actions didn't finish (actionsComplete: " + actionsComplete + ")!");
      }
      
      logger.info("END testActionExecutionMultiThreaded.");
   }
   
   
   /* ### INTERNAL CLASSES ### */
   
   
   private static class AbnormalAction extends PeriodicAction 
   {
      boolean running = false;
      boolean canRun = false;
      public AbnormalAction()
      {
         super("AbnormalAction");
         super.setPeriodicityType(PeriodicAction.PERIODICITY_INTERVAL);
         super.interval.setForceMode(true);
         super.setInterval(50);
      }
      void actionPreExecuteInternal()
      {
         super.actionPreExecuteInternal();
         super.executionStartTime = 1;
      }
      public boolean execute() throws Exception {
         synchronized (this) {
            running = true;
            notifyAll();
            while(!canRun) this.wait();
         }
         return true;
      }
   }
   
   private static class TestAction extends PeriodicAction 
   {
      private int executeCount = 0;
      private final PeriodicActionManager pam;
      
      public TestAction(PeriodicActionManager pam, int id)
      {
         super("TestAction" + id);
         
         this.pam = pam;
         
         super.setPeriodicityType(PeriodicAction.PERIODICITY_INTERVAL);
         super.interval.setForceMode(true);
         super.setInterval(0);
      }
      public boolean execute() throws Exception {
         if( (executeCount++) >= N_REPETITIONS ) {
            incrementActionsComplete();
            this.pam.removePeriodicAction(this);
         }
         
         return true;
      }
   }
   
   private static class PMStressThread extends Thread
   {
      boolean canRun = true;
      private final PeriodicActionManager pam;
      private final int id;
      
      public PMStressThread(PeriodicActionManager pam, int id) 
      {
         this.pam = pam;
         this.id = id;
         super.setDaemon(true);
      }
      
      public void run()
      {
         while(true)
         {
            synchronized (this) {
               if( !canRun ) break;
            }
            
            DummyAction action = new DummyAction(pam, this.id);
            this.pam.addPeriodicAction(action);
            double random = Math.random();
            if( random < 0.25 ) this.pam.getPeriodicActions();
            else if( random < 0.50) this.pam.getActionDescriptions();
            else if( random < 0.75) this.pam.actionPeriodicityModified(action);
            else this.pam.getPeriodicAction("DummyAction" + this.id);
            
            action.setForcedExecution(true);
            try { Thread.sleep(10); } catch(Exception e){}
         }
      }
   }
   
   private static class DummyAction extends PeriodicAction 
   {
      private final PeriodicActionManager pam;
      public DummyAction(PeriodicActionManager pam, int id)
      {
         super("DummyAction" + id);
         this.pam = pam;
      }
      public boolean execute() throws Exception {
         this.pam.removePeriodicAction(this);
         return true;
      }
   }
}
