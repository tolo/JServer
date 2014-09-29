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

import com.teletalk.jserver.tcp.messaging.EndPointSelectionStrategy;
import com.teletalk.jserver.tcp.messaging.MessagingEndPoint;

/**
 * 
 * @author Tobias Löfstrand
 */
public class DefaultEndPointSelectionStrategyTest
{
   /**
    */
   public static boolean testDefaultEndPointSelectionStrategy()
   {
      EndpointGetterThread endpointGetterThreads[] = new EndpointGetterThread[10];
      
      //int n = 10;
      //int counter = 0;
      
      MessagingManagerTest.resetFailCount();
                  
      /*for(int q=0; q<n; q++)
      {*/
         try
         {
            for(int i=0; i<endpointGetterThreads.length; i++)
            {
               //endpointGetterThreads[i] = new EndpointGetterThread(i, counter, MessagingManagerTest.messagingManagerS1.getEndPointSelectionStrategy());
               endpointGetterThreads[i] = new EndpointGetterThread(i, MessagingManagerTest.messagingManagerS1.getEndPointSelectionStrategy());
               endpointGetterThreads[i].setDaemon(true);
               //counter++;
            }
            
            for(int i=0; i<endpointGetterThreads.length; i++)
            {
               endpointGetterThreads[i].start();
            }
            
            for(int i=0; i<endpointGetterThreads.length; i++)
            {
               endpointGetterThreads[i].join(10000);
               endpointGetterThreads[i].interrupt();
            }
            
            Thread.sleep(100);
         }
         catch(Exception e)
         {
            MessagingManagerTest.messagingManagerS1.logError("Errorbritt!", e);
            e.printStackTrace();
            MessagingManagerTest.incrementFailCount();
         }
      /*}*/
      
      if( MessagingManagerTest.getFailCount() > 0 ) return false;
      return true;
   }
   
   /**
    */
   private static class EndpointGetterThread extends Thread
   {
      //private int threadId;
      
      private int id;
      
      private EndPointSelectionStrategy endPointSelectionStrategy;
      
      
      //public EndpointGetterThread(int threadId, int id, EndPointSelectionStrategy endPointSelectionStrategy)
      public EndpointGetterThread(int id, EndPointSelectionStrategy endPointSelectionStrategy)
      {
         //this.threadId = threadId;
         this.id = id;
         this.endPointSelectionStrategy = endPointSelectionStrategy;
      }
      
      public void run()
      {
         MessagingEndPoint endPoint = null;
         String prefix;
         
         for(int q=0; q<10; q++)
         {
            prefix = id + "(" + q + ")";
            try
            {
               MessagingManagerTest.messagingManagerS1.logInfo(prefix + " : getting endpoint.");
               
               if( (q % 2) == 0 )
               {
                  endPoint = this.endPointSelectionStrategy.getEndPoint(100);
                  if( endPoint == null) 
                  {
                     MessagingManagerTest.messagingManagerS1.logError(prefix + " : timeout while getting endpoint!");
                     MessagingManagerTest.incrementFailCount();
                  }
               }
               else
               {
                  endPoint = this.endPointSelectionStrategy.getEndPoint("mupp", 50);
                  if( endPoint != null )
                  {
                     MessagingManagerTest.messagingManagerS1.logError(prefix + " : error - shouldn't be returned by call getEndPoint(\"mupp\", 100)!");
                     MessagingManagerTest.incrementFailCount();
                  }
               }
            }
            catch(Exception e)
            {
               MessagingManagerTest.messagingManagerS1.logError(prefix + " : error while getting endpoint!", e);
               MessagingManagerTest.incrementFailCount();
            }
            finally
            {
               if( endPoint != null ) this.endPointSelectionStrategy.endPointReady(endPoint);
               MessagingManagerTest.messagingManagerS1.logInfo(prefix + " : done getting endpoint.");
            }
         }
      }
   }
}
