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
package com.teletalk.jserver;

import junit.framework.Test;
import junit.framework.TestSuite;

import com.teletalk.jserver.messaging.MessagingManagerTest;
import com.teletalk.jserver.periodic.PeriodicActionManagerTest;
import com.teletalk.jserver.pool.ObjectPoolTest;
import com.teletalk.jserver.queue.QueueManagerTest;
import com.teletalk.jserver.queue.QueueTest;
import com.teletalk.jserver.tcp.TcpEndPointIdentifierTest;
import com.teletalk.jserver.util.MessageQueueTest;
import com.teletalk.jserver.util.PriorityMessageQueueTest;
import com.teletalk.jserver.util.filedb.LowLevelFileDBTest;
import com.teletalk.jserver.util.validation.EmailAddressValidatorTest;
import com.teletalk.jserver.util.validation.PhoneNumberValidatorTest;

/**
 * 
 * @author Tobias Löfstrand
 */
public class AllTests
{
   /**
    */
   public static void main(String[] args)
   {
      junit.textui.TestRunner.run(AllTests.class);
   }

   /**
    */
   public static Test suite()
   {
      TestUtils.deleteTestFiles();
      
      TestSuite suite = new TestSuite("JServer everything test");
      
      //$JUnit-BEGIN$
      suite.addTestSuite(SubComponentTest.class);
      
      suite.addTestSuite(MessagingManagerTest.class);
      
      suite.addTestSuite(PeriodicActionManagerTest.class);
      
      suite.addTestSuite(ObjectPoolTest.class);
      
      suite.addTestSuite(QueueTest.class);
      suite.addTestSuite(QueueManagerTest.class);
      
      suite.addTestSuite(TcpEndPointIdentifierTest.class);
      
      suite.addTestSuite(MessageQueueTest.class);
      suite.addTestSuite(PriorityMessageQueueTest.class);
      
      suite.addTestSuite(LowLevelFileDBTest.class);
      
      suite.addTestSuite(PhoneNumberValidatorTest.class);
      suite.addTestSuite(EmailAddressValidatorTest.class);
      
      //$JUnit-END$
      
      return suite;
   }
}
