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

/**
 * Constans for http communication.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Beta 2
 */
public interface HttpConstants
{
	//Request methods
	
	/** Request method constant for a HTTP DELETE request. */
	public static final String REQUEST_METHOD_DELETE			= "DELETE";
	/** Request method constant for a HTTP GET request. */
	public static final String REQUEST_METHOD_GET				= "GET";
	/** Request method constant for a HTTP HEAD request. */
	public static final String REQUEST_METHOD_HEAD			= "HEAD";
	/** Request method constant for a HTTP OPTIONS request. */
	public static final String REQUEST_METHOD_OPTIONS		= "OPTIONS";
	/** Request method constant for a HTTP POST request. */
	public static final String REQUEST_METHOD_POST			= "POST";
	/** Request method constant for a HTTP PUT request. */
	public static final String REQUEST_METHOD_PUT				= "PUT";
	/** Request method constant for a HTTP TRACE request. */
	public static final String REQUEST_METHOD_TRACE			= "TRACE";
	
	// Header keys constants
	
	/** Header constant for Cache-Control. */
	public static final String CACHE_CONTROL_HEADER_KEY = "Cache-Control";
	/** Header constant for Content-Type. */
	public static final String CONTENT_TYPE_HEADER_KEY = "Content-Type";
	/** Header constant for Content-Length. */
	public static final String CONTENT_LENGTH_HEADER_KEY = "Content-Length";
	/** Header constant for Content-Disposition. */
	public static final String CONTENT_DISPOSITION_HEADER_KEY = "Content-Disposition";	
	/** Header constant for Connection. */
	public static final String CONNECTION_HEADER_KEY = "Connection";
	/** Header constant for Date. */
	public static final String DATE_HEADER_KEY = "Date";
	/** Header constant for Server. */
	public static final String SERVER_HEADER_KEY = "Server";
	/** Header constant for Host. */
	public static final String HOST_HEADER_KEY = "Host";
   /** Header constant for User-agent. */
   public static final String USER_AGENT_HEADER_KEY = "User-agent";
	
	// Non-standard header constants
	
	/** Non-standard header for client (remote) ip-address. Set by {@link com.teletalk.jserver.tcp.http.proxy.HttpProxy}. */
	public static final String REMOTE_ADDRESS_HEADER_KEY = "X-Remote-Address";
	
	/** Non-standard header for client (remote) port. Set by {@link com.teletalk.jserver.tcp.http.proxy.HttpProxy}. */
	public static final String REMOTE_PORT_HEADER_KEY = "X-Remote-Port";
	
	/** Non-standard header for server (local) ip-address. Set by {@link com.teletalk.jserver.tcp.http.proxy.HttpProxy}. */
	public static final String LOCAL_ADDRESS_HEADER_KEY = "X-Local-Address";
	
	/** Non-standard header for server (local) port. Set by {@link com.teletalk.jserver.tcp.http.proxy.HttpProxy}. */
	public static final String LOCAL_PORT_HEADER_KEY = "X-Local-Port";
	
	// Header value constants
	/** Header value constant "no-cache" for the header key {@link #CACHE_CONTROL_HEADER_KEY Cache-Control}. */
	public static final String CACHE_CONTROL_NO_CACHE_HEADER_VALUE = "no-cache";
	/** Header value constant "Keep-Alive" for the header key {@link #CONNECTION_HEADER_KEY Connection}. */
	public static final String CONNECTION_KEEP_ALIVE_HEADER_VALUE = "Keep-Alive";
	/** Header value constant "close" for the header key {@link #CONNECTION_HEADER_KEY Connection}. */
	public static final String CONNECTION_CLOSE_HEADER_VALUE = "close";
	
	// Value constants.
	/** Content type constant used to identify a multipart content type (i.e. multipart...). */
	public static final String CONTENT_TYPE_MULTIPART = "multipart";
	/** Constant used to find the multipart boundary value in a multipart content type header. */
	public static final String CONTENT_TYPE_MULTIPART_BOUNDARY = "boundary";
	/** Constant used to find the name of a multipart block. */
	public static final String CONTENT_TYPE_MULTIPART_NAME = "name";
	/** Constant used to find the file name of a multipart block. */
	public static final String CONTENT_TYPE_MULTIPART_FILENAME = "filename";
	/** Content type constant for the "application/x-www-form-urlencoded" content type. */	
	public static final String CONTENT_TYPE_WWW_FORM_URLENCODED = "application/x-www-form-urlencoded";
	/** Content type constant for the urlencoded content type. */	
	public static final String CONTENT_TYPE_URLENCODED = "urlencoded";
		
	/** Name used to map request data of unknown type. */
	public static final String UNKNOWN_REQUEST_DATA_NAME = "com.teletalk.jserver.tcp.http.HttpConstants.UNKNOWN_REQUEST_DATA_NAME";
	
	/** Contant for the key value separator in HTTP headers. */
	public static final String HEADER_KEY_VALUE_SEPARATOR = ":";
	
	/** Contant for the key value separator in HTTP header values. */
	public static final String KEY_VALUE_SEPARATOR = "=";
	
	/** Carrige return (CR) constant (\r). */
	public static final String CR = "\r";
	
	/** Line feed (LF) constant (\n). */
	public static final String LF = "\n";
	
	/** Line break (CRLF) constant (\r\n). */
	public static final String CRLF = CR+LF;
	
	/** Space (SP) constant (0x20). */
	public static final String SP = " ";
	
	/** Tab (HT) constant (\t). */
	public static final String HT = "\t";
	
	/** The default character encoding used by the HTTP-classes. Default value is "<b>8859_1</b>".*/
	public static String DEFAULT_CHARACTER_ENCODING = "8859_1";
}
