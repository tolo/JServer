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

import java.io.IOException;

/**
 * Interface for classes that are capable of seriailizing and deseriailizing their state in a primitive fashion (i.e. not by using the standard 
 * java serialization mechanism). The serialization will be performed in the method {@link #read(InputStreamer)} and the deseriailization 
 * in the method {@link #write(OutputStreamer)}.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.3.1, build 670.
 */
public interface Streamable
{
   /**
    * Deserializates the state of this object from the specified {@link InputStreamer}.
    * 
    * @param input the {@link InputStreamer} to read data from.
    * 
    * @throws IOException if an I/O error occurs.
    */
	public void read(InputStreamer input) throws IOException;
	
   /**
    * Serializates the state of this object to the specified {@link OutputStreamer}.
    * 
    * @param output the {@link OutputStreamer} to write data to.
    * 
    * @throws IOException if an I/O error occurs.
    */
	public void write(OutputStreamer output) throws IOException;	
}
