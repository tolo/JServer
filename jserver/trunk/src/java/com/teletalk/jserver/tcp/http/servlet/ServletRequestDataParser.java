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
package com.teletalk.jserver.tcp.http.servlet;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;

import com.teletalk.jserver.tcp.http.HttpConstants;
import com.teletalk.jserver.tcp.http.HttpRequestData;

/**   
 * Class for parsing request data from a ServletInputStream. This class is capable of parsing GET, HEAD, and POST requests.
 * 
 * @author Tobias Löfstrand
 */
public final class ServletRequestDataParser implements HttpConstants
{
   /**
    * Parses request data from a HttpServletRequest object.
    * 
    * @param request a HttpServletRequest object to parse the request from.
    * 
    * @return a HttpRequestData object.
    * 
    * @throws IOException If there was an error parsing the request data.
    */
   public static HttpRequestData parseRequestData(final HttpServletRequest request) throws IOException
   {
      String method = request.getMethod();
      HttpRequestData requestData = null;       
      
      if( method != null ) method = method.trim();
      else method = "";

      if(method.equalsIgnoreCase("GET") || method.equalsIgnoreCase("HEAD"))
      {
         String queryString = request.getQueryString();
         
         requestData = new HttpRequestData(queryString, request.getCharacterEncoding());
      } 
      else 
      {
         if(method.equalsIgnoreCase("POST")) 
         {
            if(request.getHeader("Content-Length") != null) 
            {
               String cType = request.getHeader(CONTENT_TYPE_HEADER_KEY);
               String cTypeLowerCase = cType.toLowerCase();
               
               //Check if request is multipart
               if(cTypeLowerCase.startsWith(CONTENT_TYPE_MULTIPART))
               {
                  int boundaryIndex = cTypeLowerCase.indexOf(CONTENT_TYPE_MULTIPART_BOUNDARY);
                                                
                  if(boundaryIndex > 0) // Found boundary
                  {
                     int equalsSignIndex = cTypeLowerCase.indexOf(KEY_VALUE_SEPARATOR, boundaryIndex + CONTENT_TYPE_MULTIPART_BOUNDARY.length());
                     
                     if(equalsSignIndex > 0) // Found equals sign
                     {
                        String boundary = cType.substring(equalsSignIndex+1).trim();
                        
                        String sLength = request.getHeader(CONTENT_LENGTH_HEADER_KEY);
                        int cLength = Integer.parseInt(sLength);

                        requestData = new HttpRequestData(request.getQueryString(), request.getInputStream(), cLength, cType, boundary, request.getCharacterEncoding());
                     }
                     else throw new IOException("Unable to parse multipart boundary from request - " + request.toString() + ".");
                  }
                  else throw new IOException("Unable to parse multipart boundary from request - " + request.toString() + ".");
               }
               else
               {
                  String sLength = request.getHeader(CONTENT_LENGTH_HEADER_KEY);
                  int cLength = Integer.parseInt(sLength);
                  requestData = new HttpRequestData(request.getQueryString(), request.getInputStream(), cLength, cType);
               }
            }
            else //Currently, POST requests without Content-Length header field specified are not handled.
            {
               throw new IOException("No 'Content-Length' header specified in the following request: " + request.toString() + ".");
               //requestData = new HttpRequestData();
            }
         }
       else 
       {
            throw new IOException("Unknown request method " + method);
         }
      }
      
      return requestData;
   }
}
