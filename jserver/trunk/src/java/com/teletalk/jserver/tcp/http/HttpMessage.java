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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.SocketException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.StringTokenizer;
import java.util.TimeZone;

import com.teletalk.jserver.JServerUtilities;

/**	
 * Abstract base class for representation of HTTP messages. This class handles reading 
 * of a HTTP message from a stream and mapping of header key/values.
 * <i>Note:</i> All header keys are case insensitive.
 *   
 * @author Tobias Löfstrand
 * 
 * @since The beginning
 * 
 * @see com.teletalk.jserver.tcp.http.HttpRequest
 * @see com.teletalk.jserver.tcp.http.HttpResponse
 */
public abstract class HttpMessage implements HttpConstants
{
   // Dictionary for header data
   private HashMap headerData = new HashMap();

   // Dictionary for mapping lowercase keys to actual keys
   private HashMap headerKeys = new HashMap();

   // Flag indicating if the client closed the connection
   private boolean connectionClosed = false;

   /**
    * Initializes this HTTP message (start line and headers) with data from an input stream. This method calls 
    * {@link #parseMessageStartLine(String)} to parse the message start line (request or status line).
    * 
    * @param messageReader the input stream to read the message from.
    */
   protected void readMessage(final InputStream messageReader) throws IOException
   {
      byte byteBuffer[] = new byte[2048]; //Temporary byte buffer for the message

      int requestEndIndex = 0;
      int i = 0;

      // Attempt to read first byte to be able to detect if the client closes the connection
      try
      {
         byteBuffer[i] = (byte) messageReader.read();
         i++;
      }
      catch (SocketException se)
      {
         this.connectionClosed = true;
         return;
      }

      // Read message (until CRLF CRLF)
      for (;((byteBuffer[i] = (byte) messageReader.read()) >= 0); i++)
      {
         if (i >= (byteBuffer.length - 1)) //Allocate more space for message
         {
            byte[] newByteBuffer = new byte[byteBuffer.length * 2];
            System.arraycopy(byteBuffer, 0, newByteBuffer, 0, byteBuffer.length);
            byteBuffer = newByteBuffer;
         }

         if (i > 3)
         {
            if ((byteBuffer[i - 3] == '\r')
               && (byteBuffer[i - 2] == '\n')
               && (byteBuffer[i - 1] == '\r')
               && (byteBuffer[i] == '\n'))
            {
               requestEndIndex = i - 4;
               break;
            }
         }
      }

      if (byteBuffer[0] < 0)
      {
         this.connectionClosed = true;
      }
      else if (requestEndIndex > 0)
      {
         StringTokenizer st;

         String requestString = new String(byteBuffer, 0, requestEndIndex + 1, DEFAULT_CHARACTER_ENCODING).trim();
         byteBuffer = null;

         BufferedReader requestStringReader = new BufferedReader(new StringReader(requestString));
         String startLine = requestStringReader.readLine();
         
         if(startLine != null)
         {
            // Parse message start line (request or status line)
            this.parseMessageStartLine(startLine);

            // Parse request headers
            boolean endOfHeaders = false;
            
            String line = null;
            String currentHeaderKey = null;
            String currentHeaderValue = null;
            String lineFirstChar = "";
            boolean lineEmpty;
              
            // Read first line
	         line = requestStringReader.readLine();
                                    
            while(!endOfHeaders)
            {
					if( line == null )
               {
               	endOfHeaders = true;
               	lineEmpty = true;
               }
					else
					{
						lineEmpty = (line.trim().length() == 0);
						lineFirstChar = String.valueOf(line.charAt(0));
					}	
													
					// Parse new header
					if( !lineEmpty && (currentHeaderKey == null) )
					{
						st = new StringTokenizer(line, HttpConstants.HEADER_KEY_VALUE_SEPARATOR, false);
							
	              	currentHeaderKey = st.nextToken();
	           		currentHeaderValue = st.nextToken().trim();
	
	               while (st.hasMoreTokens())
	               {
	                  currentHeaderValue = currentHeaderValue + HttpConstants.HEADER_KEY_VALUE_SEPARATOR + st.nextToken().trim();
	               }
					}
					// Check if multi line header value
					else if( !lineEmpty && (lineFirstChar.equals(HttpConstants.SP) || lineFirstChar.equals(HttpConstants.HT)) )
					{
						currentHeaderValue = currentHeaderValue + lineFirstChar + line.trim();
					}
					// Add current header
					else if( currentHeaderKey != null )
					{
						// Add current header...
						this.addHeader(currentHeaderKey.trim(), currentHeaderValue);
						
						// ...reset current key...
						currentHeaderKey = null;
						
						// ...and jump to beginning of loop, since line already contains an unparsed header
						continue;
					}
					
					// Read new line
					line = requestStringReader.readLine();
            }
         }
      }
   }

   /**
    * Called when reading a HTTP message to parse the start line (request or status line). This method is provided 
    * for subclasses that wish to extract information from the start line.<br>
    * <br>
    * This implementation is empty.
    * 
    * @param startLine the HTTP message start line.
    */
   protected void parseMessageStartLine(String startLine) throws IOException
   {
   }

   /**
    * Checks if the connection was closed by the client before the request could be read.
    * 
    * @return <code>true</code> if the connection was closed by the client before the request could be read, otherwise <code>false</code>.
    *  
    * @since 1.13 Build 600
    */
   public boolean isClosed()
   {
      return this.connectionClosed;
   }

   /**	
    * Send the headers in the packet on an outputstream.
    *   
    * @param os The outputstream on which to write the data
    * 
    * @throws java.io.IOException If communication errors should occur
    */
   protected void sendHeaders(OutputStream os) throws java.io.IOException
   {
      String formattedHeaders;

      formattedHeaders = this.formatHeaders() + "\r\n";
      os.write(formattedHeaders.getBytes(DEFAULT_CHARACTER_ENCODING));
   }
   
   /**
    * Gets the HashMap used for storage of the headers. No cloning will be performed on the 
    * returned HashMap.
    * 
    * @since 1.3.2 Build 699.
    */
   public HashMap getHeaderData()
   {
      return this.headerData;
   }
   
   /**
    * Sets the HashMap used for storage of the headers.
    * 
    * @since 1.3.2 Build 699.
    */
   public void setHeaderData(final HashMap headerData)
   {
      this.headerData = headerData;
   }

   /**	
    * Set a header key to a specified value. If the key is already
    * defined in this packet, the value is replaced.
    *   
    * @param key The key to set the value for
    * @param value The value to set
    */
   public void setHeader(String key, String value)
   {
      String realKeyName;
      ArrayList vect = new ArrayList();

      key = HttpMessage.decode(key);
      vect.add(HttpMessage.decode(value));

      if ((realKeyName = this.getRealKeyName(key.toLowerCase())) == null)
      {
         // The key does not exist in the hashtable
         realKeyName = key;
         this.headerKeys.put(key.toLowerCase(), realKeyName);
      }
      this.headerData.put(realKeyName, vect);
   }

   /**	
    * Add another value to a header key. If the key is already
    * defined in this packet, the value is appended after the
    * existing value(s).
    *   
    * @param key The key to add the value for.
    * @param value The value to set.
    */
   public void addHeader(String key, String value)
   {
      ArrayList vect;
      String realKeyName;

      //Is this really nessecary? Remove for now....or....
      key = HttpMessage.decode(key);
      value = HttpMessage.decode(value);

      if ((realKeyName = this.getRealKeyName(key.toLowerCase())) == null)
      {
         // The key does not exist in the hashtable
         vect = new ArrayList();
         realKeyName = key;
         this.headerKeys.put(key.toLowerCase(), realKeyName);
      }
      else
      {
         vect = (ArrayList) this.headerData.get(realKeyName);
      }

      vect.add(value);
      this.headerData.put(realKeyName, vect);
   }

   /**	
    * Get all header values for a specified key. If the header key
    * is undefined an array of zero length is returned.
    * 
    * @param key The key to get the value(s) for
    * 
    * @return     The values for that key
    */
   public String[] getHeader(final String key)
   {
      String realKeyName;
      ArrayList headerValues;

      if (this.hasHeader(key))
      {
         realKeyName = getRealKeyName(key);
         headerValues = (ArrayList) this.headerData.get(realKeyName);

         if (headerValues != null)
         {
            return (String[]) headerValues.toArray(new String[headerValues.size()]);
         }
      }
      return new String[0];
   }

   /**	
    * Get the first header value for a specified key. If the header 
    * key is undefined <code>null</code> is returned.
    * 
    * @param key The key to get the value(s) for.
    * 
    * @return The value for that key or <code>null</code> if the key has not been defined.
    */
   public String getHeaderSingleValue(String key)
   {
      String[] allValues;

      allValues = this.getHeader(key);
      if (allValues.length > 0)
      {
         return allValues[0];
      }
      else
      {
         return null;
      }
   }

   /**	
    * Remove a key from the header.
    * 
    * @param key The key to remove.
    */
   public void removeHeader(String key)
   {
      String realKeyName;

      if (this.hasHeader(key))
      {
         realKeyName = this.getRealKeyName(key);

         this.headerKeys.remove(key.toLowerCase());
         this.headerData.remove(realKeyName);
      }
   }

   /**	
    * Check if a key is defined in the header.
    * 
    * @param key The key to check for presence.
    * 
    * @return <code>true</code> if the key is defined, otherwise <code>false</code>.
    */
   public boolean hasHeader(String key)
   {
      return (this.getRealKeyName(key) != null);
   }
   
   /**
    * Formats a header (possibly with several values).
    * 
    * @since 1.3.2 Build 699
    */
   public String formatHeader(final String key, final String[] values)
   {
      StringBuffer sb = new StringBuffer();

      if( (key != null) && (key.length() > 0) )
      {
         if( (values != null) && (values.length > 0) )
         {
            for(int i=0; i<values.length; i++)
            {
               sb.append(key);
               sb.append(": ");
               sb.append(values[i]);
               sb.append(HttpConstants.CRLF);
            }
         }
         else
         {
            sb.append(key);
            sb.append(":");
            sb.append(HttpConstants.CRLF);
         }
      }
      return sb.toString();
   }

   /**
    * Returns the headers in this HttpMessage as a string formatted according to the HTTP specification 
    * (in a format appropriate for returning in a HTTP request or response).
    * 
    * @return the headers in this HttpMessage as a string formatted according to the HTTP specification.
    * 
    * @since 1.2
    */
   public String formatHeaders()
   {
      String key;
      StringBuffer sb = new StringBuffer();
      ArrayList values;
      Iterator allKeys;

      allKeys = this.headerData.keySet().iterator();
      while (allKeys.hasNext()) // Header keys
      {
         key = (String) allKeys.next();
         if( (key != null) && (key.length() > 0) )
         {
            values = (ArrayList) this.headerData.get(key);
            sb.append( this.formatHeader(key, (String[])values.toArray(new String[0])) );
         }
      }
      return sb.toString();
   }

   /**	
    * Represent this set of headers as a string.
    * 
    * @return The textual representation of this header
    */
   public String toString()
   {
      return formatHeaders();
   }

   /**	
    * Get the key name used for storing data under the key in this packet.
    * 
    * @param key The case insensitive key name.
    * 
    * @return The case sensitive key name under which the values are stored in this packet.
    */
   private String getRealKeyName(String key)
   {
      String realKeyName = null;
      String lowerCaseKey = key.toLowerCase();

      if (this.headerKeys.containsKey(lowerCaseKey))
      {
         realKeyName = (String) this.headerKeys.get(lowerCaseKey);
      }

      return realKeyName;
   }

   /**
    * Converts from a MIME format called "x-www-form-urlencoded" to a String.<BR><BR>
    * To convert to a String, each character is examined in turn:<BR><BR> 
    * <li> The ASCII characters 'a' through 'z', 'A' through 'Z', and '0' through '9' remain the same. <BR>
    * <li> The plus sign '+'is converted into a space character ' '.<BR> 
    * <li> The remaining characters are represented by 3-character strings which begin with the percent sign, "%xy", 
    * where xy is the two-digit hexadecimal representation of the lower 8-bits of the character. 
    * 
    * @param s the string to decode.
    * 
    * @return a decoded string.
    */
   public static String decode(String s)
   {
      if (s == null)
         return s;

      try
      {
         return URLDecoder.decode(s);
      }
      catch (Exception e)
      {
         JServerUtilities.logError("HttpPacket", "Error while decoding", e);
         return s;
      }
   }

   /**
    * Converts a String into a MIME format called "x-www-form-urlencoded" format.<BR><BR> 
    * To convert a String, each character is examined in turn: <BR>
    * <li> The ASCII characters 'a' through 'z', 'A' through 'Z', and '0' through '9' remain the same. <BR>
    * <li> The space character ' ' is converted into a plus sign '+'. 
    * <li> All other characters are converted into the 3-character string "%xy", where xy is the two-digit hexadecimal representation of the lower 8-bits of the character. 
    * 
    * @param s the string to encode.
    * 
    * @return an encoded string.
    */
   public static String encode(String s)
   {
      if (s == null)
         return s;

      return URLEncoder.encode(s);
   }

   /**	
    * Shortcut for setting the header variable "Content-Type". 
    * This variable can also be set using method <code>setHeader(key, value)</code> inherited by HttpMessage. 
    * 
    * @param contentType The MIME content type of this response body.
    */
   public void setContentType(final String contentType)
   {
      this.setHeader(CONTENT_TYPE_HEADER_KEY, contentType);
   }

   /**
    * Gets the size in bytes of the body of this message.
    * 
    * @return the size in bytes of the body of this message.
    * 
    * @since 1.2
    */
   public long getContentLength()
   {
      String sLength = this.getHeaderSingleValue(CONTENT_LENGTH_HEADER_KEY);
      if (sLength != null)
      {
         try
         {
            return Long.parseLong(sLength);
         }
         catch (NumberFormatException nfe)
         {
         }
      }

      return 0;
   }

   /**
    * Creates a string containing a HTTP-date/time stamp (rfc1123) for the current date and time.
    * 
    * @return a string containing a HTTP (rfc1123) formatted date/time.
    * 
    * @since 1.13
    */
   public static String createHTTPDateString()
   {
      return createHTTPDateString(new Date());
   }

   /**
    * Creates a string containing a HTTP-date/time stamp (rfc1123) for the specified <code>Date</code> object.
    * 
    * @param date the date and time to create a date/time string for.
    * 
    * @return a string containing a HTTP (rfc1123) formatted date/time.
    * 
    * @since 1.13
    */
   public static String createHTTPDateString(final Date date)
   {
      SimpleDateFormat dateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss 'GMT'", Locale.US);
      GregorianCalendar cal = new GregorianCalendar(TimeZone.getTimeZone("GMT"));
      cal.setTime(date);
      dateFormat.setCalendar(cal);

      return dateFormat.format(cal.getTime());
   }
}
