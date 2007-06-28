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

/*
	TODO: getMultipartReader()
	TODO: PUT support
*/
package com.teletalk.jserver.tcp.http;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.StringTokenizer;

/**	
 * Class for representing an HTTP request sent by a client to the server. This class currently handles 
 * GET, HEAD and POST (also multipart) requests.
 * 
 * @author Tobias Löfstrand
 * 
 * @since The beginning
 * 
 * @see com.teletalk.jserver.tcp.http.HttpMessage
 * @see com.teletalk.jserver.tcp.http.HttpResponse
 */
public class HttpRequest extends HttpMessage
{
	private String method = null; // e.g. "GET"
	private String path = null; // e.g. "/index.html"
	private String queryString = null; // e.g. "p1=1&p2=2"
	private String version = null; // e.g. "HTTP/1.0"
	
	private HttpRequestData requestData = null;

	private final InputStream requestReader;

	private boolean requestBodyRead = false;

	/**
	 * Parse a request instance from the data received on the socket 
	 * to a connected client.
	 * 
	 * @param requestReader a buffered reader to read the request from.
	 * 
	 * @throws java.io.IOException If communication errors should occur.
	 */
	public HttpRequest(InputStream requestReader) throws IOException
	{
		if( !(requestReader instanceof BufferedInputStream) )
		{
			requestReader = new BufferedInputStream(requestReader);
		}
		this.requestReader = requestReader;

		super.readMessage(this.requestReader);
	}

	/**
	* Create a new request that can be sent to an HTTP server.
	* 
	* @param method The method for the request (i.e. "GET", "HEAD" or "POST", etc).
	* @param path The path for the request (i.e. "/resource").
	*/
	public HttpRequest(String method, String path)
	{
		this.requestReader = null;

		this.method = method.toUpperCase().trim();
		this.path = path;
		this.version = "HTTP/1.0";

		this.requestData = null;
	}

	/**
	 * Create a new request that can be sent to an HTTP server.
	 * 
	 * @param method The method for the request (i.e. "GET", "HEAD" or "POST", etc).
	 * @param path The path for the request (i.e. "/resource").
	 * @param version The version of the request (i.e. "HTTP/1.0").
	 */
	public HttpRequest(String method, String path, String version)
	{
		this.requestReader = null;

		this.method = method.toUpperCase().trim();
		this.path = path;
		this.version = version;

		this.requestData = null;
	}
		
	/**
	 * Called when reading a HTTP message to parse the start line (request or status line).
	 * 
	 * @param startLine the HTTP message start line.
	 */
	protected void parseMessageStartLine(final String startLine) throws IOException
	{
		// Create a reader and a tokenizer on the first line
		StringTokenizer st = new StringTokenizer(startLine, HttpConstants.SP, false);

		// First comes the method (i.e. GET, HEAD or POST)
		method = st.nextToken().toUpperCase();

		if (method != null)
			method = method.trim();
		else
			method = "";

		if (!method.equals(REQUEST_METHOD_GET)
			&& !method.equals(REQUEST_METHOD_HEAD)
			&& !method.equals(REQUEST_METHOD_POST))
			throw new IOException(
				"Unable to handle request method '" + method + "'.");

		// Then the path
		this.path = st.nextToken();

		// Make sure that unquoted spaces doesn't mess it up
		while (st.countTokens() > 1)
			this.path = this.path + "%20" + st.nextToken();

		int index;
		
		// Get query string
		if ((index = this.path.indexOf('?')) != -1)
		{
			this.queryString = this.path.substring(index + 1);
			this.path = this.path.substring(0, index);
		}

		// Last in this line comes the version (e.g. HTTP/1.0)
		this.version = st.nextToken();
	}

	/**
	 * Parses the parameters of this http request, if it has not already been read.
	 * 
	 * @exception IOException if an error occurs whie reading the request.
	 */
	public void readRequestBody() throws IOException
	{
		if (!requestBodyRead && (requestReader != null))
		{
			requestBodyRead = true;

			if (method.equals(REQUEST_METHOD_GET) || method.equals(REQUEST_METHOD_HEAD))
			{
				if (this.queryString != null)
				{
					this.requestData = new HttpRequestData(this.queryString);
				}
				else
				{
					this.requestData = new HttpRequestData();
				}
			}
			else if (this.method.equals(REQUEST_METHOD_POST))
			{
				//Parse arguments from the body of the request
				if (hasHeader(CONTENT_LENGTH_HEADER_KEY))
				{
					String cType =
						getHeaderSingleValue(CONTENT_TYPE_HEADER_KEY);
					String cTypeLowerCase = cType.toLowerCase();

					//Check if request is multipart
					if (cTypeLowerCase.startsWith(CONTENT_TYPE_MULTIPART))
					{
						int boundaryIndex =
							cTypeLowerCase.indexOf(
								CONTENT_TYPE_MULTIPART_BOUNDARY);

						if (boundaryIndex > 0) // Found boundary
						{
							int equalsSignIndex =
								cTypeLowerCase.indexOf(
									KEY_VALUE_SEPARATOR,
									boundaryIndex
										+ CONTENT_TYPE_MULTIPART_BOUNDARY
											.length());

							if (equalsSignIndex > 0) // Found equals sign
							{
								String boundary =
									cType.substring(equalsSignIndex + 1).trim();

								String sLength =
									getHeaderSingleValue(CONTENT_LENGTH_HEADER_KEY);
								int cLength = Integer.parseInt(sLength);

								try
								{
									this.requestData =
										new HttpRequestData(this.queryString, 
											requestReader,
											cLength,
											cType,
											boundary);
								}
								catch (IOException e)
								{
									throw new IOException(
										"Error while parsing request ("
											+ toString()
											+ ") - "
											+ e.toString()
											+ ".");
								}
							}
							else
								throw new IOException(
									"Unable to parse multipart boundary from request - "
										+ toString()
										+ ".");
						}
						else
							throw new IOException(
								"Unable to parse multipart boundary from request - "
									+ toString()
									+ ".");
					}
					else //Not multipart (for instance application/x-www-form-urlencoded)
						{
						String sLength =
							getHeaderSingleValue(CONTENT_LENGTH_HEADER_KEY);
						int cLength = Integer.parseInt(sLength);
						this.requestData =
							new HttpRequestData(this.queryString, requestReader, cLength, cType);
					}
				}
				else //Currently, POST requests without Content-Length header field specified are not handled.
				{
					throw new IOException(
						"No 'Content-Length' header specified in the following request: "
							+ this.toString()
							+ ".");
					//this.requestData = new HttpRequestData();
				}
			}
			else
			{
				this.requestData = new HttpRequestData();
			}

			// Consume any extra CRLFs in request caused by buggy client implementations:
			while (requestReader.available() > 0)
				requestReader.read();
		}
	}
	
	/**
	 * Tests if this is a valid http request, e.g if at least the method, path and version fields are specified.
	 * 
	 * @return <code>true</code> if this is a valid http request, otherwise <code>false</code>.
	 */
	public boolean isValid()
	{
		return (this.method != null)
			&& (this.path != null)
			&& (this.version != null);
	}

	/**	
	 * Get the method for this request. 
	 * 
	 * @return The method for the request
	 */
	public String getMethod()
	{
		return this.method;
	}

	/**	
	 * Get the path for this request (excluding the query string). 
	 * 
	 * @return the path for the request.
	 */
	public String getPath()
	{
		return this.path;
	}

	/**	
	 * Get the query string of this request, or <code>null</code> if the request didn't contain any query string. 
	 * 
	 * @return the query string of this request, or <code>null</code> if the request didn't contain any query string. 
	 */
	public String getQueryString()
	{
		return this.queryString;
	}

	/**	
	 * Get the version for this request. 
	 * 
	 * @return The version for the request.
	 */
	public String getVersion()
	{
		return this.version;
	}
	
	/**	
	 * Parses and gets the request data (parameters) for this request. 
	 * 
	 * @return the request data for this request.
	 */
	public HttpRequestData getRequestData()
	{
		if (requestData == null)
		{
			try
			{
				this.readRequestBody();
			}
			catch (IOException e)
			{
				StringWriter strWriter = new StringWriter();
				e.printStackTrace(new PrintWriter(strWriter));

				throw new RuntimeException(strWriter.toString());
			}
		}

		return requestData;
	}

	/**
	 * Gets the inputstream used to read this request.
	 * 
	 * @return the inputstream used to read this request.
	 */
	public InputStream getRequestInputStream()
	{
		return this.requestReader;
	}

	/**
	 * Gets the request message (excluding the body) as a string formatted according to the HTTP specification.
	 * 
	 * @return the request message as a string.
	 * 
	 * @since 1.2
	 */
	public String getRequestMessage()
	{
		String formattedHeaders = super.formatHeaders();
		StringBuffer reqLine = new StringBuffer();

		reqLine.append(this.method);
		reqLine.append(" ");
		reqLine.append(this.path);
		if (this.queryString != null)
		{
			reqLine.append("?");
			reqLine.append(this.queryString);
		}
		reqLine.append(" ");
		reqLine.append(this.version);
		reqLine.append("\r\n");

		return reqLine.toString() + formattedHeaders + "\r\n";
	}

	/**	
	 * The representation of the HttpRequest. The returned string will not contain the body of the 
	 * request, if any.
	 * 
	 * @return Textual description of the request.
	 */
	public String toString()
	{
		return this.getRequestMessage();
	}
}
