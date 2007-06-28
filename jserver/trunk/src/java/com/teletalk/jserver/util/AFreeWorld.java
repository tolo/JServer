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
package com.teletalk.jserver.util;

import java.io.FileDescriptor;
import java.net.InetAddress;
import java.security.Permission;

/**
 * Class implementing a security manager that allows everything.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public final class AFreeWorld extends SecurityManager
{
   public void checkPermission(Permission perm)
   {}

   public void checkPermission(Permission perm, Object context)
   {}

   public void checkCreateClassLoader()
   {}

   public void checkAccess(Thread t)
   {}

   public void checkAccess(ThreadGroup g)
   {}

   public void checkExit(int status)
   {}

   public void checkExec(String cmd)
   {}

   public void checkLink(String lib)
   {}

   public void checkRead(FileDescriptor fd)
   {}

   public void checkRead(String file)
   {}

   public void checkRead(String file, Object context)
   {}

   public void checkWrite(FileDescriptor fd)
   {}

   public void checkWrite(String file)
   {}

   public void checkDelete(String file)
   {}

   public void checkConnect(String host, int port)
   {}

   public void checkConnect(String host, int port, Object context)
   {}

   public void checkListen(int port)
   {}

   public void checkAccept(String host, int port)
   {}

   public void checkMulticast(InetAddress maddr)
   {}

   public void checkMulticast(InetAddress maddr, byte ttl)
   {}

   public void checkPropertiesAccess()
   {}

   public void checkPropertyAccess(String key)
   {}

   public boolean checkTopLevelWindow(Object window)
   {
      return true;
   }

   public void checkPrintJobAccess()
   {}

   public void checkSystemClipboardAccess()
   {}

   public void checkAwtEventQueueAccess()
   {}

   public void checkPackageAccess(String pkg)
   {}

   public void checkSetFactory()
   {}

   public void checkMemberAccess(Class clazz, int which)
   {}

   public void checkPackageDefinition(String arg0)
   {}

   public void checkSecurityAccess(String arg0)
   {}

}
