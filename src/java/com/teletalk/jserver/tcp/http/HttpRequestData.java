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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.StringTokenizer;

/**   
 * Class for representing the data contained in a Http request. Depending on the type of request the data was extracted 
 * from, an object of this class will contain different types of objects. If for instance data was extracted from a multipart 
 * POST request a HttpRequestData object will contain string keys mapped with objects of the class {@link MessageBodyPart}. In all other 
 * cases a HttpRequestData object will contain string keys mapped with string values.<br>
 * <br>
 * <i>Note: </i>POST requests without Content-Length header field specified are currently not handled.
 *   
 * @author Tobias Löfstrand
 * 
 * @since Beta 2
 */
public final class HttpRequestData
{
   private final HashMap requestData = new HashMap();
   private final String multiPartBoundary;
   private final String characterEncoding;
   
   /**
    * Creates a empty HttpRequestData object.
    */
   public HttpRequestData() 
   {
      this.multiPartBoundary = null;
      this.characterEncoding = HttpConstants.DEFAULT_CHARACTER_ENCODING;
   }
   
   /**
    * Creates a HttpRequestData object from a String. This constructor is used 
    * when the request is of type GET and the query string was given 
    * in the path of the request.
    * 
    * @param queryString the string which holds the query string.
    */
   public HttpRequestData(final String queryString)
   {
      this(queryString, HttpConstants.DEFAULT_CHARACTER_ENCODING);
   }
   
   /**
    * Creates a HttpRequestData object from a String. This constructor is used 
    * when the request is of type GET and the query string was given 
    * in the path of the request.
    * 
    * @param queryString the string which holds the query string.
    */
   public HttpRequestData(final String queryString, final String characterEncoding)
   {
      this.multiPartBoundary = null;
      this.characterEncoding = (characterEncoding != null) ? characterEncoding : HttpConstants.DEFAULT_CHARACTER_ENCODING;
      
      if( queryString != null ) this.parseQueryString(queryString);
   }
   
   /**
    * Creates a HttpRequestData object from a reader. This constructor is used when the request 
    * is of type POST and a query string (content type <code>application/x-www-form-urlencoded</code>) is specified in 
    * the body of the request. However, this constructor is also used for all content types other than multipart, in which case 
    * a the body data will simply be read as bytes and wrapped in a <code>MessageBodyPart</code> object.
    * 
    * @param queryString the query string.
    * @param requestReader The reader used for the data from the client.
    * @param contentLength The length of the data in the body (always given in a POST request in header "Content-Length").
    * @param contentType the content type of the body data.
    * 
    * @see MessageBodyPart
    */
   public HttpRequestData(final String queryString, final InputStream requestReader, final int contentLength, final String contentType) throws IOException
   {
      this(queryString, requestReader, contentType, HttpConstants.DEFAULT_CHARACTER_ENCODING, contentLength);
   }
   
   /**
    * Creates a HttpRequestData object from a reader. This constructor is used when the request 
    * is of type POST and a query string (content type <code>application/x-www-form-urlencoded</code>) is specified in 
    * the body of the request. However, this constructor is also used for all content types other than multipart, in which case 
    * a the body data will simply be read as bytes and wrapped in a <code>MessageBodyPart</code> object.
    * 
    * @param queryString the query string.
    * @param requestReader The reader used for the data from the client.
    * @param contentLength The length of the data in the body (always given in a POST request in header "Content-Length").
    * @param contentType the content type of the body data.
    * 
    * @see MessageBodyPart
    */
   public HttpRequestData(final String queryString, final InputStream requestReader, final String contentType, final String characterEncoding, final int contentLength) throws IOException
   {
      this.multiPartBoundary = null;
      this.characterEncoding = (characterEncoding != null) ? characterEncoding : HttpConstants.DEFAULT_CHARACTER_ENCODING;
      
      // Parse request path query string, if any
      if( (queryString != null) && (queryString.trim().length() >0) )
      {
         parseQueryString(queryString);
      }
      
      byte[] buffer = readBodyData(requestReader, contentLength);
      
      if(contentType.equalsIgnoreCase(HttpConstants.CONTENT_TYPE_WWW_FORM_URLENCODED)) // If application/x-www-form-urlencoded
      {
         parseQueryString(new String(buffer, this.characterEncoding));
      }
      else
      {
         // Check if the content type contains the string "urlencoded", and if so parse is as if it was application/x-www-form-urlencoded
         if(contentType.toLowerCase().indexOf(HttpConstants.CONTENT_TYPE_URLENCODED) >= 0) parseQueryString(new String(buffer, this.characterEncoding));
         else // Else create a MessageBodyPart object to wrap around the unknown body data
         {
            MessageBodyPart bodyData = new MessageBodyPart(HttpConstants.UNKNOWN_REQUEST_DATA_NAME);
            bodyData.setData(buffer);
            
            addData(HttpConstants.UNKNOWN_REQUEST_DATA_NAME, bodyData);
         }
      }
      
      buffer = null;
   }
   
   /**
    * Creates a HttpRequestData object from a reader. This constructor is used when the request 
    * is of type POST and the content type is <code>multipart/form-data</code>. Everty part of the message body will be 
    * encapsulated in a {@link MessageBodyPart MessageBodyPart} object.
    * 
    * @param queryString the query string.
    * @param requestReader The reader used for the data from the client.
    * @param contentLength The length of the data in the body (always given in a POST request in header "Content-Length").
    * @param contentType the content type of the body data.
    * @param multiPartBoundary the boundary used to separate different parts of the message body.
    * 
    * @see MessageBodyPart
    */
   public HttpRequestData(final String queryString, final InputStream requestReader, final int contentLength, final String contentType, final String multiPartBoundary) throws IOException
   {
      this(queryString, requestReader, contentLength, contentType, multiPartBoundary, HttpConstants.DEFAULT_CHARACTER_ENCODING);
   }
   
   /**
    * Creates a HttpRequestData object from a reader. This constructor is used when the request 
    * is of type POST and the content type is <code>multipart/form-data</code>. Everty part of the message body will be 
    * encapsulated in a {@link MessageBodyPart MessageBodyPart} object.
    * 
    * @param queryString the query string.
    * @param requestReader The reader used for the data from the client.
    * @param contentLength The length of the data in the body (always given in a POST request in header "Content-Length").
    * @param contentType the content type of the body data.
    * @param multiPartBoundary the boundary used to separate different parts of the message body.
    * 
    * @see MessageBodyPart
    */
   public HttpRequestData(final String queryString, final InputStream requestReader, final int contentLength, final String contentType, final String multiPartBoundary, final String characterEncoding) throws IOException
   {
      this.multiPartBoundary = multiPartBoundary;
      this.characterEncoding = (characterEncoding != null) ? characterEncoding : HttpConstants.DEFAULT_CHARACTER_ENCODING;
      
      // Parse request path query string, if any
      if( (queryString != null) && (queryString.trim().length() >0) )
      {
         parseQueryString(new String(queryString));
      }
      
      byte[] buffer = readBodyData(requestReader, contentLength);

      parseMultiPartData(buffer);
      
      buffer = null;
   }
   
   /**
    * Internal method to read body data from the request.
    */
   private byte[] readBodyData(InputStream requestReader, int contentLength) throws IOException
   {
      byte[] buffer = new byte[contentLength];
      
      int read = requestReader.read(buffer, 0, contentLength);

      if(read < contentLength)
      {
         try{
         Thread.sleep(100);
         }catch(Exception e){}
         
         for(int i=read; i<contentLength;)
         {
            read = requestReader.read(buffer, i, contentLength-i);

            if(read == -1)
            {
               throw new IOException("Error while reading request body - unexpected EOF!");
            }
            i += read;
         }
      }
      
      return buffer;
   }
   
   /**
    * Get all values associated with a specific key.
    * 
    * @param key The key for which values should be returned.
    * 
    * @return An array of objects or an empty array if the key could not be found.
    */
   public Object[] getValues(String key) 
   {
      if(hasKey(key)) 
      {
         ArrayList vect = (ArrayList)requestData.get(key);
         Object[] objs = vect.toArray();
                  
         return objs;
      } 
      else 
      {
         return new Object[]{};
      }
   }
   
   /**
    * Convenience method to get all values associated with a specific key as String objects.
    * 
    * @param key The key for which values should be returned.
    * 
    * @return An array of objects or an empty array if the key could not be found.
    */
   public String[] getStringValues(String key) 
   {
      if(hasKey(key)) 
      {
         ArrayList vect = (ArrayList)requestData.get(key);
         String[] stringValues = new String[vect.size()];
         
         for(int i=0; i<vect.size(); i++) stringValues[i] = convertValueToString(vect.get(i));

         return stringValues;
      } 
      else 
      {
         return new String[]{};
      }
   }
   
   /**
    * Convenience method to get all values associated with a specific key as MessageBodyPart objects.
    * 
    * @param key The key for which values should be returned.
    * 
    * @return An array of objects or an empty array if the key could not be found.
    */
   public MessageBodyPart[] getMessageBodyParts(String key)
   {
      if(hasKey(key)) 
      {
         ArrayList vect = (ArrayList)requestData.get(key);
                  
         return (MessageBodyPart[])vect.toArray(new MessageBodyPart[]{});
      } 
      else 
      {
         return new MessageBodyPart[]{};
      }
   }
      
   /**
    * Get the first value associated with the specified key. 
    * 
    * @param key The key for which the first value should be returned.
    * 
    * @return the value as an object. If the key could not be found <tt>null</tt> is returned.
    */
   public Object getSingleValue(String key) 
   {
      if(this.hasKey(key)) 
      {
         ArrayList vect = (ArrayList) this.requestData.get(key);
         
         if (vect.size() > 0) 
            return vect.get(0);
         else 
            return null;
      } 
      else 
         return null;
   }
   
   /**
    * Convenience method to get the first value associated with the specified key as a String object. If the first value that matches 
    * the key is a String object, it will simply be returned unchanged. Otherwise if the value is a MessageBodyPart the 
    * method {@link MessageBodyPart#getDataAsString()} will be called to generate a String representation of tha value. 
    * For all other types of values String objects will be created with the method <code>Object.toString()</code>.
    * 
    * @param key The key for which the first value should be returned.
    * 
    * @return the value as a String object. If the key could not be found <tt>null</tt> is returned.
    */
   public String getSingleStringValue(String key)
   {
      Object val = getSingleValue(key);
      
      return convertValueToString(val);
   }
   
   /**
    * Gets a string representation of the specified value.
    */
   private String convertValueToString(final Object val)
   {
      if(val != null)
      {
         if(val instanceof MessageBodyPart)
         {
            return ((MessageBodyPart)val).getDataAsString();
         }
         else if(val instanceof String)
         {
            return (String)val;
         }
         else
         {
            return val.toString();
         }
      }
      else
         return null;
   }
   
   /**
    * Convenience method to get the first value associated with the specified key as a MessageBodyPart object. 
    * 
    * @param key The key for which the first value should be returned.
    * 
    * @return the value as a MessageBodyPart object. If the key could not be found or the matching value 
    * was not an instance of MessageBodyPart, <code>null</code> is returned.
    */
   public MessageBodyPart getSingleMessageBodyPart(String key)
   {
      if(this.hasKey(key)) 
      {
         ArrayList vect = (ArrayList) this.requestData.get(key);
         
         if (vect.size() > 0) 
         {
            Object obj = vect.get(0);
            
            if( (obj != null) && (obj instanceof MessageBodyPart) )
               return (MessageBodyPart)obj;
            else 
               return null;
         }
         else 
            return null;
      } 
      else 
         return null;
   }
   
   /**
    * Gets the number of key/value mappings.
    * 
    * @return the number of key/value mappings.
    */   
   public int getItemCount()
   {
      return requestData.size(); 
   }
      
   /**
    * Add another value to a key. If the key already exist, it will
    * have multiple values after the invocation of this method. If value is null
    * it will not be added.
    * 
    * @param key The key to map.
    * @param value The value to which the key should map.
    */
   public void addData(String key, Object value) 
   {
      if(this.hasKey(key)) 
      {
         ArrayList vect = (ArrayList) this.requestData.get(key);
         if(value != null) vect.add(value);
      } 
      else 
      {
         this.setData(key, value);
      }
   }
      
   /**
    * Set a value to a key. If the key already exist, the old value
    * is simply replaced with the new.
    * 
    * @param key The key to map.
    * @param value The value to which the key should map.
    */
   public void setData(String key, Object value) 
   {
      ArrayList vect = new ArrayList();
      vect.add(value);
            
      this.requestData.put(key, vect);
   }
      
   /**
    * Check if a key is defined in this querystring.
    * 
    * @param key The key to map.
    * 
    * @return <tt>true</tt> if this querystring holds a mapping of the key, otherwise <tt>false</tt>.
    */
   public boolean hasKey(String key) 
   {
      return this.requestData.containsKey(key);
   }
      
   /**   
    * Produce an enumeration of all keys in this querystring.
    * 
    * @return An Enumeration of all keys in this querystring.
    */
   public Enumeration keys() 
   {
      return Collections.enumeration(requestData.keySet());
   }
   
   /**
    * Returns a hashtable containging all key/value mappings.
    * 
    * @return a hashtable containging all key/value mappings.
    */
   public HashMap getMappings()
   {
      return (HashMap)requestData.clone();
   }
            
   /**   
    * Represent this HttpRequestData as a String.
    * 
    * @return a string representation of this HttpRequestData.
    */
   public String toString() 
   {
      String key;
      Object[] values;
      StringBuffer sb = new StringBuffer();
      Enumeration keyEnum = keys();
      String lineSep = System.getProperty("line.separator");
      if(lineSep == null) lineSep = "\r\n";
      String separatorLine;
      
      if(multiPartBoundary != null) //Data is multipart (using MessageBodyPart objects)
      {
         sb.append("HttpRequestData(Multi part):" + lineSep);
         //separatorLine = this.multiPartBoundary + lineSep;
         separatorLine = "----------" + lineSep;
      }
      else
      {
         sb.append("HttpRequestData(Query string):" + lineSep);
         separatorLine = "----------" + lineSep;
      }
      
      while(keyEnum.hasMoreElements()) 
      {
         sb.append(separatorLine);
         
         key = (String) keyEnum.nextElement();
         values = getValues(key);
         
         if(multiPartBoundary == null) //Data is not multipart (using MessageBodyPart objects)
            sb.append(key + " : " + lineSep);
         
         if(values != null)
         {
            for(int i=0; i<values.length; i++) 
            {
               sb.append(values[i] + lineSep);
            }
         }
         else sb.append("null");
         
         sb.append(lineSep);
      }
      return sb.toString();
   }
      
   //--------------------------------------------------
   //------------------ Parsing methods ----------------
   //--------------------------------------------------
   
   /**
    * Parse a string that holds a query string and insert the values
    * found into the private dictionary.
    * 
    * @param queryString the string which holds the query string.
    */
   private void parseQueryString(final String queryString) 
   {
      int index;
      String keyValuePair, value;
      StringTokenizer st;
      st = new StringTokenizer(queryString, "&", false);
            
      // As long as there are more pairs of key and value
      while (st.hasMoreTokens()) 
      {
         keyValuePair = st.nextToken();
         index = keyValuePair.indexOf('=');
                  
         if(index != -1) 
         {
            // Add the value to the key
                        
            if(index < keyValuePair.length() - 1) 
            {
               value = HttpMessage.decode(keyValuePair.substring(index + 1));
            } 
            else
            {
               value = "";
            }
                     
            addData(HttpMessage.decode(keyValuePair.substring(0, index)), value);
         }
         else
         {
            // Add the key with no value
            addData(HttpMessage.decode(keyValuePair), null);
         }
      }
   }
   
   /**
    * Parses multipart data and creates MessageBodyPart objects for every part in the message body.
    * 
    * @param buffer the message body of the request.
    */
   public void parseMultiPartData(final byte[] buffer) throws IOException
   {
      if(buffer.length == 0) return; // No data
      
      final byte[] multiPartBoundaryBeginArray = ("--" + this.multiPartBoundary).getBytes();
      
      this.parseMultiPartData(buffer, 0, multiPartBoundaryBeginArray, null);
   }
      
   /**
    * Parses multipart data and creates MessageBodyPart objects for every part in the message body.
    */
   private void parseMultiPartData(final byte[] buffer, final int offset, final byte[] multiPartBoundaryBeginArray, String defaultName) throws IOException
   {
      String line;
      String lowerCaseLine;
      String name;
      int unknownCounter = 0;
      int nameValueIndex;
      int nameValueEndIndex;
      String key;
      String val;
      StringTokenizer st;
      MessageBodyPart bodyPart;
      int boundaryIndex;
      int currentIndex = offset;

      // Find first multipart boundary
      boundaryIndex = findNextMultiPartBoundary(buffer, currentIndex, multiPartBoundaryBeginArray);
      if(boundaryIndex < 0) throw new IOException("Unable to find first multipart boundary!"); // No boundary found
      else boundaryIndex = boundaryIndex + multiPartBoundaryBeginArray.length + 2; //Set boundaryIndex to end of boundary (including the CRLF)

      currentIndex = boundaryIndex;

      // Iterate through buffer until last multipart boundary is reached
      while(currentIndex < (buffer.length - multiPartBoundaryBeginArray.length))
      {
         name = null;
         bodyPart = new MessageBodyPart(null, this.characterEncoding);

         //Read headers (until CRLF CRLF)
         while((line = readHeaderLineCRLF(buffer, currentIndex)).length() > 0)
         {
            currentIndex += (line.length() + 2); // Increment currentIndex with length of line and +2 for CRLF
            lowerCaseLine = line.toLowerCase();
               
            // Find name of field
            if(lowerCaseLine.startsWith(HttpConstants.CONTENT_DISPOSITION_HEADER_KEY.toLowerCase()))
            {
               nameValueIndex = lowerCaseLine.indexOf(HttpConstants.CONTENT_TYPE_MULTIPART_NAME.toLowerCase()); //Find beginning of name key
               if(nameValueIndex >= 0)
               {
                  nameValueIndex = lowerCaseLine.indexOf("\"", nameValueIndex + HttpConstants.CONTENT_TYPE_MULTIPART_NAME.length()) + 1; //Find beginning of name value, i.e. first "-sign and add one.
                  if(nameValueIndex > 0)
                  {
                     nameValueEndIndex = line.indexOf("\"", nameValueIndex);
                     name = line.substring(nameValueIndex, nameValueEndIndex);
                  }
               }
            }
               
            st = new StringTokenizer(line, ":", false);
            key = st.nextToken();
            
            if(st.hasMoreTokens())
            {
               val = st.nextToken();
         
               while (st.hasMoreTokens()) 
               {
                  val = val + ":" + st.nextToken();
               }
            }
            else val = "";
            
            bodyPart.addHeader(key.trim(), val.trim());
         }

         currentIndex += 2; // Increment currentIndex with + 2 for CRLF
            
         if(name == null)
         {
            if(defaultName == null)
               name = HttpConstants.UNKNOWN_REQUEST_DATA_NAME + (unknownCounter++);
            else
               name = defaultName;
         }
            
         bodyPart.setName(name);
         
         // Find next multipart boundary
         boundaryIndex = findNextMultiPartBoundary(buffer, currentIndex, multiPartBoundaryBeginArray);
         if(boundaryIndex < 0) throw new IOException("Unexpected end of multipart data. Unable to find next multipart boundary.");

         String cType = bodyPart.getHeaderSingleValue(HttpConstants.CONTENT_TYPE_HEADER_KEY);
         String cTypeLowerCase = (cType != null) ? cType.toLowerCase() : "";
                  
         // Check if content type of multipart block is multi part (nested multi part...)
         if( cTypeLowerCase.startsWith(HttpConstants.CONTENT_TYPE_MULTIPART) )
         {
               int nestedMultiPartBoundaryIndex = cTypeLowerCase.indexOf(HttpConstants.CONTENT_TYPE_MULTIPART_BOUNDARY);
                                                
               if(nestedMultiPartBoundaryIndex > 0) // Found boundary
               {
                  int equalsSignIndex = cTypeLowerCase.indexOf(HttpConstants.KEY_VALUE_SEPARATOR, nestedMultiPartBoundaryIndex + HttpConstants.CONTENT_TYPE_MULTIPART_BOUNDARY.length());
                     
                  if(equalsSignIndex > 0) // Found equals sign
                  {
                     String nestedMultiPartBoundary = cType.substring(equalsSignIndex+1).trim();
                     byte[] nestedMultiPartBoundaryBeginArray = ("--" + nestedMultiPartBoundary).getBytes();

                     // Parse nested multipart block
                     this.parseMultiPartData(buffer, currentIndex, nestedMultiPartBoundaryBeginArray, name);
                  }
                  else throw new IOException("Unable to parse nested multipart boundary from multipart request data");
               }
               else throw new IOException("Unable to parse nested multipart boundary from multipart request data.");
         }
         else // Multi part block had other content type than multipart
         {
            byte[] bodyPartData = new byte[(boundaryIndex - 2) - currentIndex]; // - 2 for CRLF
            System.arraycopy(buffer, currentIndex, bodyPartData, 0, bodyPartData.length);
            bodyPart.setData(bodyPartData);

            addData(name, bodyPart);
         }
         
         boundaryIndex = boundaryIndex + multiPartBoundaryBeginArray.length + 2; //Set boundaryIndex to end of boundary (including the CRLF)
         currentIndex = boundaryIndex; //Set current index to end of next multipart boundary
      }
   }
   
   /**
    * Finds the next multipart boundary in the specified buffer.
    */
   private int findNextMultiPartBoundary(final byte[] buffer, int offset, byte[] multiPartBoundaryBeginArray)
   {
      boolean foundNextMultiPart;
      int bufferSearchIndex;
      
      for(; offset<buffer.length; offset++)
      {
         if(buffer[offset] == multiPartBoundaryBeginArray[0])
         {
            bufferSearchIndex = offset + 1;
            foundNextMultiPart = true;
            
            for(int i=1; (i < multiPartBoundaryBeginArray.length); i++, bufferSearchIndex++)
            {
               if( (bufferSearchIndex >= buffer.length) || (buffer[bufferSearchIndex] != multiPartBoundaryBeginArray[i]) )
               {
                  foundNextMultiPart = false;
                  break;
               }
            }
            
            if(foundNextMultiPart) return offset;
            else offset = bufferSearchIndex;
         }
      }
      
      return -1;
   }
   
   /**
    * Reads a headers line terminated by CRLF from the specified buffer (using the ISO-Latin1 (8859-1) encoding).
    */
   private String readHeaderLineCRLF(final byte[] buffer, final int stringBeginIndex)
   {
      int stringEndIndex = stringBeginIndex;
      
      for(; stringEndIndex < buffer.length; stringEndIndex++)
      {
         if(stringEndIndex == (buffer.length - 1) ) // If last index
         {
            break;
         }
         else if( (buffer[stringEndIndex] == '\r') && (buffer[stringEndIndex+1] == '\n') )
         {
            break;
         }
      }
      
      if(stringEndIndex == stringBeginIndex) return "";
      else 
      {
         try
         {
            return new String(buffer, stringBeginIndex, (stringEndIndex - stringBeginIndex), this.characterEncoding);
         }
         catch(java.io.UnsupportedEncodingException e)
         {
            return new String(buffer, stringBeginIndex, (stringEndIndex - stringBeginIndex));
         }
      }
   }
}
