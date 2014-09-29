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

import java.io.DataInput;
import java.io.IOException;
import java.io.ObjectInputStream;

/**
 * Interface representing the source from which data needed to deserialize a {@link com.teletalk.jserver.util.Streamable} object will be read.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.1, build 670.
 */
public interface InputStreamer extends DataInput
{
   /**
    * Gets the <code>java.io.ObjectInputStream</code> that will be used to deserialize java objects within the context of the current 
    * {@link com.teletalk.jserver.util.Streamable} object. 
    */
   public ObjectInputStream getContextObjectInputStream() throws IOException;
}
