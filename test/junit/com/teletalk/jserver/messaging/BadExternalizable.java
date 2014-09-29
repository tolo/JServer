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

import java.io.Externalizable;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;

/**
 * 
 * @author Tobias Löfstrand
 */
public class BadExternalizable implements Externalizable
{
   private static final long serialVersionUID = -8718169694931308035L;
   

   private long id;
   
   private String mupp;

   
   /**
    */
   public BadExternalizable()
   {
   }

   /**
    */
   public BadExternalizable(long id, String mupp)
   {
      this.id = id;
      this.mupp = mupp;
   }

   public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException
   {
      this.id = in.readInt(); // Read int instead of long
      this.mupp = (String)in.readObject();
   }

   public void writeExternal(ObjectOutput out) throws IOException
   {
      out.writeLong(this.id);
      out.writeObject(this.mupp);
   }

   public String toString()
   {
      return "BadExternalizable(" + id + ", " + mupp + ")";
   }
}
