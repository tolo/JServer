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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * FileInputStream implementation that deletes the file when garbage collected.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.1.2 (20060316)
 */
public class DeleteOnFinalizeFileInputStream extends FileInputStream
{
   private final File file;
   

   /**
    * Creates a DeleteOnFinalizeFileInputStream.
    */
   public DeleteOnFinalizeFileInputStream(File file) throws FileNotFoundException
   {
      super(file);
      
      this.file = file;
   }

   /**
    * Finalizes this DeleteOnFinalizeFileInputStream and deletes the associated file.
    */
   protected void finalize() throws IOException
   {
      FileDeletor.delete(this.file);
      
      super.finalize();
   }
}
