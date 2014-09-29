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

/**
 * Simple thread subclass that enables the setting and getting of an id field.
 * 
 * @since 2.0, build 761.
 * 
 * @author Tobias Löfstrand
 */
public class AbstractIdentifiableThread extends Thread implements IdentifiableThread
{
   private String id;
   
   /**
    * Creates a new JServerThread.
    */
   public AbstractIdentifiableThread()
   {
      super();
   }

   /**
    * Creates a new JServerThread.
    *
    * @param   target   the object whose <code>run</code> method is called.
    */
   public AbstractIdentifiableThread(Runnable target)
   {
      super(target);
   }

   /**
    * Creates a new JServerThread.
    *
    * @param   name   the name of the new thread.
    */
   public AbstractIdentifiableThread(String name)
   {
      super(name);
   }

   /**
    * Creates a new JServerThread.
    * 
    * @param      group     the thread group.
    * @param      target   the object whose <code>run</code> method is called.
    */
   public AbstractIdentifiableThread(ThreadGroup group, Runnable target)
   {
      super(group, target);
   }

   /**
    * Creates a new JServerThread.
    * 
    * @param      target   the object whose <code>run</code> method is called.
    * @param      name     the name of the new thread.
    */
   public AbstractIdentifiableThread(Runnable target, String name)
   {
      super(target, name);
   }

   /**
    * Creates a new JServerThread.
    * 
    * @param      group     the thread group.
    * @param      name     the name of the new thread.
    */
   public AbstractIdentifiableThread(ThreadGroup group, String name)
   {
      super(group, name);
   }

   /**
    * Creates a new JServerThread.
    * 
    * @param      group     the thread group.
    * @param      target   the object whose <code>run</code> method is called.
    * @param      name     the name of the new thread.
    */
   public AbstractIdentifiableThread(ThreadGroup group, Runnable target, String name)
   {
      super(group, target, name);
   }

   /**
    * Creates a new JServerThread.
    * 
    * @param      group    the thread group.
    * @param      target   the object whose <code>run</code> method is called.
    * @param      name     the name of the new thread.
    * @param      stackSize the desired stack size for the new thread, or
    *             zero to indicate that this parameter is to be ignored.
    */
   public AbstractIdentifiableThread(ThreadGroup group, Runnable target, String name, long stackSize)
   {
      super(group, target, name, stackSize);
   }

   /**
    * Gets the id of this thread.
    */
   public String getThreadId()
   {
      return id;
   }
   
   /**
    * Sets the id of this thread.
    */
   public void setThreadId(String id)
   {
      this.id = id;
   }
}
