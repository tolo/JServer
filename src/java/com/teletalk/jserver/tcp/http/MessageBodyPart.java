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
package com.teletalk.jserver.tcp.http;

import java.util.StringTokenizer;

/**   
 * Class for representing a part of a message body in a multipart/form-data POST request.
 *   
 * @author Tobias Löfstrand
 * 
 * @since Beta 1
 */
public final class MessageBodyPart extends HttpMessage
{
   private String name = null;
   private byte[] multiPartData = null;
   private String stringData = null;
   private String defaultCharacterEncoding;
   
   /**
    * Constructs a new MessageBodyPart object.
    */
   public MessageBodyPart()
   {
      this(null);
   }
   
   /**
    * Constructs a new MessageBodyPart object.
    */
   public MessageBodyPart(final String name)
   {
      this(name, null);
   }
   
   /**
    * Constructs a new MessageBodyPart object.
    */
   public MessageBodyPart(final String name, final String defaultCharacterEncoding)
   {
      this.name = name;
      if( defaultCharacterEncoding != null ) this.defaultCharacterEncoding = defaultCharacterEncoding;
      else this.defaultCharacterEncoding = HttpConstants.DEFAULT_CHARACTER_ENCODING;
   }
   
   /**
    * Sets the name of this MessageBodyPart.
    * 
    * @param name the name of this MessageBodyPart.
    */
   public void setName(String name)
   {
      this.name = name; 
   }
   
   /**
    * Gets the name of this MessageBodyPart.
    * 
    * @return name the name of this MessageBodyPart.
    */
   public String getName()
   {
      return this.name;
   }
   
   /**
    * Sets the byte data of this MessageBodyPart.
    * 
    * @param multiPartData byte data.
    */
   public void setData(byte[] multiPartData)
   {
      this.multiPartData = multiPartData; 
   }
   
   /**
    * Returns the data stored in this MessageBodyPart.
    * 
    * @return the data in a byte array.
    */
   public byte[] getData()
   {
      return this.multiPartData;
   }
   
   /**
    * Gets a string representation of the data stored in this MessageBodyPart, using the ISO-Latin1 (8859-1) encoding. 
    * 
    * @return a string representation of the data stored in this MessageBodyPart.
    */
   public String getDataAsString()
   {
      return this.getDataAsString(this.getCharacterEncoding());
   }
   
   /**
    * Gets a string representation of the data stored in this MessageBodyPart, using the ISO-Latin1 (8859-1) encoding. 
    * 
    * @param characterEncoding the character encoding that will be used to create a string from the byte data.
    * 
    * @return a string representation of the data stored in this MessageBodyPart.
    */
   public String getDataAsString(final String characterEncoding)
   {
      if(stringData == null)
      {
         try
         {
            stringData = new String(multiPartData, characterEncoding);
         }
         catch(java.io.UnsupportedEncodingException e)
         {
            stringData = new String(multiPartData);
         }
      }
      return stringData;
   }
   
   /**
    * Gets the content type of the data stored in this MessageBodyPart.
    * 
    * @return the content type of the data stored in this MessageBodyPart, null if none was found.
    */
   public String getContentType()
   {
      return this.getHeaderSingleValue(CONTENT_TYPE_HEADER_KEY);
   }
   
   /**
    * Gets the character encoding for this multipart block. This method always returns the default character encoding (8859-1) for now.
    * 
    * @return the character encoding for this multipart block. 
    */
   public String getCharacterEncoding()
   {
      return defaultCharacterEncoding;
   }
   
   /**
    * Method to get the value of the filename header field for this multipart block.
    * 
    * @return the value of the filename header field for this multipart block.
    */
   public String getFileName()
   {
      String contentDisp = this.getHeaderSingleValue(CONTENT_DISPOSITION_HEADER_KEY);
      
      if(contentDisp != null)
      {
         StringTokenizer tokenizer = new StringTokenizer(contentDisp, ";");
         String token;
         
         while( tokenizer.hasMoreTokens() )
         {
            token = tokenizer.nextToken();
            if( token != null ) 
            {
               token = token.trim();
               
               // Test if file name key
               if( token.toLowerCase().startsWith(HttpConstants.CONTENT_TYPE_MULTIPART_FILENAME.toLowerCase()) )
               {
                  int valueBeginIndex = token.indexOf('='); // Get index of equals sign
                  if( (valueBeginIndex > 0) && (token.length() > (valueBeginIndex+1)) )
                  {
                     String fileName = token.substring(valueBeginIndex+1).trim();
                     if(fileName.charAt(0) == '\"') fileName = fileName.substring(1);
                     if(fileName.charAt(fileName.length()-1) == '\"') fileName = fileName.substring(0, fileName.length()-1);
                     
                     return fileName;
                  }
               }
            }
         }
         
         return "";
      }
      
      return null;
   }
   
   /**   
    * Gets a String representation of this MessageBodyPart.
    * 
    * @return The textual representation of this MessageBodyPart.
    */
   public String toString() 
   {
      StringBuffer sb = new StringBuffer();
      String fileName = getFileName();
      String cT = getContentType();
      String lineSep = System.getProperty("line.separator");
      if(lineSep == null) lineSep = "\r\n";
                  
      sb.append("MessageBodyPart(");
      if(name != null)
      {
         sb.append("Name: ");
         sb.append(name);
      }
      if(multiPartData != null)
      {
         sb.append(", Data length: ");
         sb.append(multiPartData.length);
      }
      if(fileName != null)
      {
         sb.append(", File name: ");
         sb.append(fileName); 
      }
      sb.append(")");
      sb.append(lineSep);
      sb.append("[Headers]");
      sb.append(lineSep);
      sb.append(super.toString());
      sb.append(lineSep);
      if((multiPartData != null) && (multiPartData.length > 0))
      {
         if((cT == null) || ((cT != null) && ((cT.startsWith("text")) || (cT.startsWith("TEXT"))) ) )
         {
            sb.append("[Data (first 1000)]");
            sb.append(lineSep);
            
            if(multiPartData.length >= 1000)
            {
               try
               {
                  sb.append(new String(multiPartData, 0, 1000, this.defaultCharacterEncoding));
               }
               catch (Exception e) 
               {
                  sb.append(new String(multiPartData, 0, 1000));
               }
            }
            else
            {
               try
               {
                  sb.append(new String(multiPartData, 0, multiPartData.length, this.defaultCharacterEncoding));
               }
               catch (Exception e) 
               {
                  sb.append(new String(multiPartData, 0, multiPartData.length));
               }
            }
            //sb.append(lineSep);
         }
      }
      
      return sb.toString();
   }
}
