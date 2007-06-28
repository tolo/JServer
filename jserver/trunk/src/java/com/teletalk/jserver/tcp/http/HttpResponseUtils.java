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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.FileNameMap;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.List;

import com.teletalk.jserver.JServer;
import com.teletalk.jserver.JServerUtilities;
import com.teletalk.jserver.SubComponent;
import com.teletalk.jserver.SubSystem;
import com.teletalk.jserver.tcp.TcpConnection;
import com.teletalk.jserver.util.SystemInfo;

/**
 * Provides convenient utility methods realted to sending HttpResponses.
 * 
 * @author Tobias Löfstrand
 *
 * @since 2.1 (20050525)
 */
public class HttpResponseUtils
{
   /** 
    * A static reference to a file name map. When loaded, this class will atempt to use the 
    * sun.net.www.MimeTable to load a filename map. If that class isn't available a default filename map, 
    * consisting of the most common types, will be created.
    */
   protected static FileNameMap fileNameMap = null;
   
   static 
   {
      try
      {
         // Attempt to use the mime table of the sun reference implementation to load a filename map. If
         // another Java implementation is currently used a ClassNotFoundException will be thrown and 
         // caught.
         Class clazz = Class.forName("sun.net.www.MimeTable");
         fileNameMap = (FileNameMap)clazz.getMethod("loadTable", null).invoke(null, null);
      }
      catch(ClassNotFoundException cnfe)
      {
         JServerUtilities.logWarning("com.teletalk.jserver.tcp.http.HttpResponseUtils", "Class sun.net.www.MimeTable not available - using internal MIME filename map insted.", cnfe);
      }
      catch(Exception e)
      {
         JServerUtilities.logWarning("com.teletalk.jserver.tcp.http.HttpResponseUtils", "Error loading MIME table from sun.net.www.MimeTable - using internal MIME filename map insted.", e);
      }
      
      if(fileNameMap == null)
      {
         final HashMap _extensionContentTypeMap = new HashMap();  
                        
         _extensionContentTypeMap.put("gif", "image/gif");
         _extensionContentTypeMap.put("jpe", "image/jpeg");
         _extensionContentTypeMap.put("jpg", "image/jpeg");
         _extensionContentTypeMap.put("jpeg", "image/jpeg");
         _extensionContentTypeMap.put("tif", "image/tiff");
         _extensionContentTypeMap.put("tiff", "image/tiff");
         _extensionContentTypeMap.put("bmp", "image/bmp");
         _extensionContentTypeMap.put("log", "text/plain");
         _extensionContentTypeMap.put("jsr", "text/plain");
         _extensionContentTypeMap.put("tpl", "text/plain");
         _extensionContentTypeMap.put("java", "text/plain");
         _extensionContentTypeMap.put("ini", "text/plain");
         _extensionContentTypeMap.put("bat", "text/plain");
         _extensionContentTypeMap.put("txt", "text/plain");
         _extensionContentTypeMap.put("text", "text/plain");
         _extensionContentTypeMap.put("c", "text/plain");
         _extensionContentTypeMap.put("cc", "text/plain");
         _extensionContentTypeMap.put("c++", "text/plain");
         _extensionContentTypeMap.put("cpp", "text/plain");
         _extensionContentTypeMap.put("h", "text/plain");
         _extensionContentTypeMap.put("hpp", "text/plain");
         _extensionContentTypeMap.put("htm", "text/html");
         _extensionContentTypeMap.put("html", "text/html");
         _extensionContentTypeMap.put("xml", "text/xml");
         _extensionContentTypeMap.put("zip", "application/zip");
         _extensionContentTypeMap.put("jar", "application/zip");
         
         fileNameMap = new FileNameMap() 
            {
               private final HashMap extensionContentTypeMap = _extensionContentTypeMap;
                                                            
               public String getContentTypeFor(String fileName)
               {
                  int lastDotIndex = fileName.lastIndexOf('.');
                  
                  if( (lastDotIndex >= 0) && (lastDotIndex < (fileName.length() - 1) ) )
                  {
                     String fileNameExtenstion = (fileName.substring(lastDotIndex+1)).toLowerCase();
                     
                     return (String)extensionContentTypeMap.get(fileNameExtenstion);
                  }
                  return null;
               }
            };
      }
   }
   
   /**
    * Gets the content type for the specified file. This method uses the static FileNameMap member (@see #fileNameMap) to 
    * get the conten type for a certain file. If no type could be found, "application/octet-stream" will be returned. <br>
    * <br>
    * Subclasses can override this method to provide a better implementation.
    * 
    * @param file the name of the file to get the contenttype for.
    * 
    * @return the content type.
    */
   public static String getContentType(String file)
   {
      String foundContentType = null;
      file = file.trim();
      
      if(fileNameMap != null) foundContentType = fileNameMap.getContentTypeFor(file);
      
      if(foundContentType != null) return foundContentType;
      else return "application/octet-stream";
   }
   
   /**
    * Creates a response to an "/alive" request. The response will be "200 OK" if all subsystems are ok (no subsystem has the status CRITICAL_ERROR), otherwise 
    * a 500 response will be returned.
    * 
    * @exception IOException if there was an error writing to the socket.
    */
   public static HttpResponse createAliveResponse(final TcpConnection connection) throws IOException
   {
      HttpResponse rsp;
      boolean criticalError = false;
      String errorSubSystems = null;
      
      // Check subsystems in server for CRITICAL_ERROR
      final JServer jserver = JServer.getJServer();
      if(jserver != null)
      {
         final List subsystems = jserver.getSubComponentTree();
         if(subsystems != null)
         {
            SubComponent s;
            
            for(int i=0; i<subsystems.size(); i++)
            {
               s = (SubComponent)subsystems.get(i);
               if(s != null)
               {
                  if(s.getStatus() == SubSystem.CRITICAL_ERROR)
                  {
                     // Critical error in subsystem
                     if( !criticalError )
                     {
                        criticalError = true;
                        errorSubSystems = s.getFullName();
                     }
                     else
                     {
                        errorSubSystems += ", " + s.getFullName();
                     }
                  }
               }
            }
         }
      }
      
      if( criticalError )
      {
         if( connection.isDebugEnabled() ) connection.logDebug("Sending failed alive response to " + connection.getRemoteAddress() + ".");
         rsp = new HttpResponse(500, "Server error", "Critical error in the following subsystem(s): " + errorSubSystems);
      }
      else
      {
         if( connection.isDebugEnabled() ) connection.logDebug("Sending alive response to " + connection.getRemoteAddress() + ".");
         rsp = new HttpResponse(200, "OK", "Kahuna");
      }
      
      rsp.setContentType("text/html");
            
      return rsp;
   }
   
   /**
    * Creates a response to an "/version" request. The response contains information about the version of this server, as 
    * returned by the {@link JServer#getServerVersion()}.
    * 
    * @exception IOException if there was an error writing to the socket.
    */
   public static HttpResponse createVersionResponse(final TcpConnection connection) throws IOException
   {
      return createVersionResponse(false, connection);
   }
      
   /**
    * Creates a response to an "/version" request. The response contains information about the version of this server, as 
    * returned by the {@link JServer#getServerVersion()}. If parameter <code>longVersion</code> is <code>true</code> the 
    * version of the JServer and information about the current Java VM is also returned.
    * 
    * @param longVersion flag indicating if the version of the JServer and information about the current Java VM should also be returned.
    * 
    * @exception IOException if there was an error writing to the socket.
    */
   public static HttpResponse createVersionResponse(final boolean longVersion, final TcpConnection connection) throws IOException
   {
      if( connection.isDebugEnabled() ) connection.logDebug("Sending long version response to " + connection.getRemoteAddress() + ".");

      HttpResponse rsp;
      
      JServer jServer = JServer.getJServer();
      
      if( jServer != null )
      {
         if(longVersion)
         {
            rsp = new HttpResponse(200, "OK", jServer.getServerVersion() + ", " + JServer.getVersionString() + ", " + jServer.getJavaVMInfo());
         }
         else
         {
            rsp = new HttpResponse(200, "OK", jServer.getServerVersion());
         }
         rsp.setContentType("text/html");
      }
      else
      {
         rsp = new HttpResponse(200, "OK", "Unknown");
      }
            
      return rsp;
   }
   
   /**
    * Creates a response to an "/name" request. The response contains information about the name of this server.
    * 
    * @exception IOException if there was an error writing to the socket.
    */
   public static HttpResponse createNameResponse(final TcpConnection connection) throws IOException
   {
      if( connection.isDebugEnabled() ) connection.logDebug("Sending name response to " + connection.getRemoteAddress() + ".");
      
      HttpResponse rsp;
      
      if( JServer.getJServer() != null ) rsp = new HttpResponse(200, "OK", JServer.getJServer().getName());
      else rsp = new HttpResponse(200, "OK", "Unknown");
      rsp.setContentType("text/html");
            
      return rsp;
   }
   
   /**
    * Creates a response containing information on free disk space for the path specified in 
    * the request. This means that the request must contain the parameter <code>path</code>, 
    * for instance: <br><br>
    * <code>http://myserver:myport/getdiskfreespace?path=C</code>.<br><br>
    * The path must be a valid path that identifies the disk that
    * 
    * @param req the current http request object.
    */
   public static HttpResponse createDiskFreeSpaceResponse(final HttpRequest req, final TcpConnection connection) throws IOException
   {
      long diskFreeSpace = -1;
      
      String path = req.getRequestData().getSingleStringValue("path");
      
      if(path != null)
      {
         path = path.trim();
         
         if( (path.length() == 1) && Character.isLetter(path.charAt(0)) )
         {
            path += ":\\";
         }
         
         diskFreeSpace = SystemInfo.getDiskFreeSpace(path);
      }
      
      if( connection.isDebugEnabled() ) connection.logDebug("Sending disk free space (" + diskFreeSpace + ") response to " + connection.getRemoteAddress() + ".");
      
      HttpResponse rsp;
      
      rsp = new HttpResponse(200, "OK", String.valueOf(diskFreeSpace));
      rsp.setContentType("text/html");
            
      return rsp;
   }
   
   /**
    * Create a redirection response (302 - "See Other").
    * 
    * @param path the redirect path.
    * 
    * @exception IOException if there was an error writing to the socket.
    */
   public static HttpResponse createRedirectResponse(final String path, final TcpConnection connection) throws IOException
   {
      if( connection.isDebugEnabled() ) connection.logDebug("Redirecting " + connection.getRemoteAddress() + " to " + path + ".");
      
      HttpResponse rsp;
      
      rsp = new HttpResponse(302, "See Other", path);
      rsp.addHeader("Location", path);

      return rsp;
   }
   
   /**
    * Creates a 200 - "OK" response with the specified response body and a content type of "text/html".
    * 
    * @param body the body of the respose.
    * 
    * @exception IOException if there was an error writing to the socket.
    */
   public static HttpResponse createOkResponse(final String body, final TcpConnection connection) throws IOException
   {
      HttpResponse rsp;
      
      rsp = new HttpResponse(200, "OK", body);
      rsp.setContentType("text/html");
            
      return rsp;
   }
   
   /**
    * Creates a 200 - "OK" response with the specified response body and content type.
    * 
    * @param body the body of the respose.
    * @param contentType the content type of the body ("text/html" for instance).
    * 
    * @exception IOException if there was an error writing to the socket.
    */
   public static HttpResponse createOkResponse(final String body, final String contentType, final TcpConnection connection) throws IOException
   {
      HttpResponse rsp;
      
      rsp = new HttpResponse(200, "OK", body);
      rsp.setContentType(contentType);
            
      return rsp;
   }
   
   /**
    * Creates a response with the specified response code, response description, response body and a content type of "text/html".
    * 
    * @param responseCode the code of the response.
    * @param responseDescription the description of the response.
    * @param body the body of the respose.
    * 
    * @exception IOException if there was an error writing to the socket.
    */
   public static HttpResponse createResponse(final int responseCode, final String responseDescription, final String body, 
         final TcpConnection connection) throws IOException
   {
      HttpResponse rsp;
      
      rsp = new HttpResponse(responseCode, responseDescription, body);
      rsp.setContentType("text/html");
            
      return rsp;
   }
   
   /**
    * Creates a response with the specified response code, response description, response body and content type.
    * 
    * @param responseCode the code of the response.
    * @param responseDescription the description of the response.
    * @param body the body of the respose.
    * @param contentType the content type of the body ("text/html" for instance).
    * 
    * @exception IOException if there was an error writing to the socket.
    */
   public static HttpResponse createResponse(final int responseCode, final String responseDescription, final String body, final String contentType, 
         final TcpConnection connection) throws IOException
   {
      HttpResponse rsp;
      
      rsp = new HttpResponse(responseCode, responseDescription, body);
      rsp.setContentType(contentType);
            
      return rsp;
   }
   
   /**
    * Creates a 404 - "File not found" response, indicating that the file specified by parameter <code>fileName</code> could not be found.
    * 
    * @param fileName the name of the file that couldn't be found.
    * 
    * @exception IOException if there was an error writing to the socket.
    */
   public static HttpResponse createFileNotFoundResponse(final String fileName, final TcpConnection connection) throws IOException
   {
      HttpResponse rsp;
      
      String msg = "<h1>404 File not found</h1>";
      msg += "File not found (" + fileName + ")";
      rsp = new HttpResponse(404, "File not found", msg);
                        
      rsp.setContentType("text/html");

      return rsp;
   }
   
   /**
    * Creates a 200 - "OK" response with the specified file in the response body. If parameter <code>file</code> is 
    * null, doesn't exist or if it is a directory a 404 - "File not found" response will be sent (if parameter <code>sendFileNotFound</code> 
    * is set to <code>true</code>).
    * 
    * @param file a file to be sent in a response.
    * @param sendFileNotFound flag indicating if a 404 - "File not found" response should be sent if the specified file does not exist. If this is 
    * set to false, null will be returned if the file wasn't found.
    * 
    * @return <code>true</code> if the file existed, otherwise <code>false</code>.
    * 
    * @exception IOException if there was an error writing to the socket.
    */
   public static HttpResponse createFileResponse(final File file, final boolean sendFileNotFound, final TcpConnection connection) throws IOException
   {
      if(file!= null && file.exists() && !file.isDirectory()) 
      {
         FileInputStream in = new FileInputStream(file);
         String fullPath = file.getCanonicalPath();
         
         if( connection.isDebugEnabled() ) connection.logDebug("Sending file " + fullPath + " to " + connection.getRemoteAddress() + " (length: " + file.length() + ").");
         
         return createInputStreamResponse(in, getContentType(fullPath), file.length(), connection);
      }
      else
      {
         if(sendFileNotFound)
         {
            if( connection.isDebugEnabled() ) connection.logDebug("File (" + file + ") requested by " + connection.getRemoteAddress() + " not found!");         
            
            return createFileNotFoundResponse(file.getCanonicalPath(), connection);
         }
         return null;
      }
   }
   
   /**
    * Creates a 200 - "OK" response with the specified file in the response body. If parameter <code>file</code> is 
    * null, doesn't exist or if it is a directory a 404 - "File not found" response will be sent.
    * 
    * @param file a file to be sent in a response.
    * 
    * @exception IOException if there was an error writing to the socket.
    */
   public static HttpResponse createFileResponse(final File file, final TcpConnection connection) throws IOException
   {
      return createFileResponse(file, true, connection);
   }
   
   /**
    * Creates a 200 - "OK" response with the specified resource in the response body. If parameter <code>resourceName</code> is 
    * null, doesn't exist or if it is a directory a 404 - "File not found" response will be sent (if parameter <code>sendFileNotFound</code> 
    * is set to <code>true</code>). This method uses Class.getResource(String) to serach for resources.
    * 
    * @param resourceName name of a resource to be sent in a response.
    * @param sendFileNotFound flag indicating if a 404 - "File not found" response should be sent if the specified resource does not exist. If this is 
    * set to false, null will be returned if the file wasn't found.
    * 
    * @return <code>true</code> if the resource existed, otherwise <code>false</code>.
    * 
    * @exception IOException if there was an error writing to the socket.
    */
   public static HttpResponse createResourceResponse(String resourceName, final boolean sendFileNotFound, 
         final TcpConnection connection) throws IOException
   {
      if( (resourceName != null) && !resourceName.startsWith("/")) resourceName = "/" + resourceName;
      
      URL url = HttpResponseUtils.class.getResource(resourceName);
      
      if(url != null)
      {
         return createUrlResponse(url, connection);
      }
      else
      {
         if(sendFileNotFound)
         {
            if( connection.isDebugEnabled() ) connection.logDebug("File (" + resourceName + ") requested by " + connection.getRemoteAddress() + " not found!"); 
            
            return createFileNotFoundResponse(resourceName, connection);
         }
         
         return null;
      }
   }
   
   /**
    * Creates a 200 - "OK" response with the specified resource in the response body. If parameter <code>resourceName</code> is 
    * null, doesn't exist or if it is a directory a 404 - "File not found" response will be sent. This method uses Class.getResource(String) to serach 
    * for resources.
    * 
    * @param resourceName name of a resource to be sent in a response.
    * 
    * @exception IOException if there was an error writing to the socket.
    */
   public static HttpResponse createResourceResponse(final String resourceName, final TcpConnection connection) throws IOException
   {
      return createResourceResponse(resourceName, true, connection);
   }
   
   /**
    * Creates a 200 - "OK" response with the specified url in the response body. 
    * 
    * @param url an URL to be sent in a response.
    * 
    * @exception IOException if there was an error writing to the socket.
    * @exception NullPointerException if parameter url was null.
    */
   public static HttpResponse createUrlResponse(final URL url, final TcpConnection connection) throws IOException, NullPointerException
   {
      String fullPath = url.toString();
      
      URLConnection conn = url.openConnection();
      InputStream in = conn.getInputStream();
      final long length = conn.getContentLength();
      
      if( connection.isDebugEnabled() ) connection.logDebug("Sending " + fullPath + " to " + connection.getRemoteAddress() + " (length: " + length + ").");
      
      return createInputStreamResponse(in, conn.getContentType(), length, connection);
   }
   
   /**
    * Internal method to create a response from an inputstream.
    */
   private static HttpResponse createInputStreamResponse(final InputStream input, final String contentType, final long length, 
         final TcpConnection connection) throws IOException
   {
      HttpResponse rsp = new HttpResponse(200, "OK", input, length);
                        
      if( (contentType != null) && (!contentType.equals("")) ) rsp.setContentType(contentType);
      else rsp.setContentType("text/html");
               
      return rsp;
   }
}
