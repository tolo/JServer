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

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.util.StringTokenizer;

import com.teletalk.jserver.JServer;

/**	
 * Class for representing an HTTP response that will be sent from 
 * the server to a connected client as a result to a request.
 * 
 * @author Tobias Löfstrand
 * 
 * @since The beginning
 * 
 * @see com.teletalk.jserver.tcp.http.HttpMessage
 * @see com.teletalk.jserver.tcp.http.HttpRequest
 */
public class HttpResponse extends HttpMessage
{
	/** The buffersize used when reading body data from an inputstream and writing it to the outputstream of the response. Default value is 10K. */
	public static int maxBodyDataBufferSize = 10240;

	private int code;
	private String description;
	private String httpVersion;
	private InputStream bodyInputStream = null;
	private long bodyLength;
	private byte[] body;

	// Flag keeping control of wether or not the headers has been written
	private boolean headersWritten = false;
	
   /**   
    * Create a new 200 response without body and no default headers.
    * 
    * @since 2.0
    */
   public HttpResponse()
   {
      this("1.1", 200, "OK", false);
   }
   
   /**   
    * Create a new HttpResponse from an input stream. The specified input stream will also be used for reading the 
    * body of the response, if the content-length header was set. If this behaviour is not desired, <br>
    * <br>
    * Note that this constructor set the default headers (Server, Date, Connection).
    * 
    * @param inputStream the input stream to read the response from.
    */
   public HttpResponse(InputStream inputStream) throws IOException
   {
      this(inputStream, true);
   }
   
	/**	
	 * Create a new HttpResponse from an input stream. The specified input stream will also be used for reading the 
	 * body of the response, if the content-length header was set. If this behaviour is not desired, <br>
    * <br>
    * Note that this constructor set the default headers (Server, Date, Connection).
	 * 
	 * @param inputStream the input stream to read the response from.
	 */
	public HttpResponse(InputStream inputStream, boolean createBufferedStream) throws IOException
	{
		if( createBufferedStream && !(inputStream instanceof BufferedInputStream) )
		{
			inputStream = new BufferedInputStream(inputStream);
		}
				
		super.readMessage(inputStream);
		
		// Set bodyInputStream to inputStream (the remaining data in parameter inputStream is body data)
		this.bodyInputStream = inputStream;
		try
		{
			this.bodyLength = Long.parseLong( super.getHeaderSingleValue(CONTENT_LENGTH_HEADER_KEY) );
		}
		catch(NumberFormatException nfe)
		{
			this.bodyLength = 0;
		}
		
		this.body = new byte[0];
	}

	/**	
	 * Create a new response without body.<br>
    * <br>
    * Note that this constructor set the default headers (Server, Date, Connection).
	 * 
	 * @param code The 3 digit response code (e.g. 200 or 404).
	 * @param description The description of the response (i.e. "Ok" for a 200 response or "Not Found" for a 404 response).
	 */
	public HttpResponse(final int code, final String description)
	{
		this("1.1", code, description);
	}
   
   /**   
    * Create a new response without body.<br>
    * <br>
    * Note that this constructor set the default headers (Server, Date, Connection).
    * 
    * @param httpVersion string containing the HTTP httpVersion to be sent in the response (for instance <code>1.1</code>).
    * @param code The 3 digit response code (e.g. 200 or 404).
    * @param description The description of the response (i.e. "Ok" for a 200 response or "Not Found" for a 404 response).
    */
   public HttpResponse(final String httpVersion, final int code, final String description)
   {
      this(httpVersion, code, description, true);
   }

	/**	
	 * Create a new response without body.
	 * 
	 * @param httpVersion string containing the HTTP httpVersion to be sent in the response (for instance <code>1.1</code>).
	 * @param code The 3 digit response code (e.g. 200 or 404).
	 * @param description The description of the response (i.e. "Ok" for a 200 response or "Not Found" for a 404 response).
    * @param setDefaultHeaders flag indicating if default headers should be set (Server, Date, Connection).
	 */
	public HttpResponse(final String httpVersion, final int code, final String description, final boolean setDefaultHeaders)
	{
		// Make sure code is in valid range
		this.code = (100 <= code && code <= 599) ? code : 500;

		// Make sure description is not empty
		this.description = ((description != null) && (description.length() > 0)) ? description : "x";

		this.httpVersion = "HTTP/" + httpVersion;

		this.body = new byte[0];
		
      if( setDefaultHeaders )
      {
         this.setDefaultHeaders();
      }
	}
	
	/**	
	 * Create a new response that after instantiation can be sent to the connected client.<br>
    * <br>
    * Note that this constructor set the default headers (Server, Date, Connection).
	 * 
	 * @param code The 3 digit response code (e.g. 200 or 404).
	 * @param description The description of the response (i.e. "Ok" for a 200 response or "Not Found" for a 404 response).
	 * @param body The body of the response, presented by the client according to the header variable "Content-Type".
	 */
	public HttpResponse(final int code, final String description, final byte[] body)
	{
		this(code, description);
		this.setBody(body);
	}

	/**	
	 * Create a new response that after instantiation can be sent to the connected client.<br>
    * <br>
    * Note that this constructor set the default headers (Server, Date, Connection).
	 * 
	 * @param httpVersion string containing the HTTP httpVersion to be sent in the response (for instance <code>1.1</code>).
	 * @param code The 3 digit response code (e.g. 200 or 404).
	 * @param description The description of the response (i.e. "Ok" for a 200 response or "Not Found" for a 404 response).
	 * @param body The body of the response, presented by the client according to the header variable "Content-Type".
	 */
	public HttpResponse(final String httpVersion, final int code, final String description, final byte[] body)
	{
		this(httpVersion, code, description);
		this.setBody(body);
	}

	/**	
	 * Create a new response that after instantiation can be sent to the connected client.<br>
    * <br>
    * Note that this constructor set the default headers (Server, Date, Connection).
	 * 
	 * @param code The 3 digit response code (e.g. 200 or 404).
	 * @param description The description of the response (i.e. "Ok" for a 200 response or "Not Found" for a 404 response).
	 * @param body The body of the response, presented by the client according to the header variable "Content-Type".
	 */
	public HttpResponse(final int code, final String description, final String body)
	{
		this(code, description);
		this.setBody(body);
	}

	/**	
	 * Create a new response that after instantiation can be sent to the connected client.<br>
    * <br>
    * Note that this constructor set the default headers (Server, Date, Connection).
	 * 
	 * @param httpVersion string containing the HTTP httpVersion to be sent in the response (for instance <code>1.1</code>).
	 * @param code The 3 digit response code (e.g. 200 or 404).
	 * @param description The description of the response (i.e. "Ok" for a 200 response or "Not Found" for a 404 response).
	 * @param body The body of the response, presented by the client according to the header variable "Content-Type".
	 */
	public HttpResponse(final String httpVersion, final int code, final String description, final String body)
	{
		this(httpVersion, code, description);
		this.setBody(body);
	}

	/**	
	 * Create a new response that after instantiation can be sent to the connected client.<br>
    * <br>
    * Note that this constructor set the default headers (Server, Date, Connection).
	 * 
	 * @param code The 3 digit response code (e.g. 200 or 404). 
	 * @param description The description of the response (i.e. "Ok" for a 200 response or "Not Found" for a 404 response) 
	 * @param is The inputstream from wich a response will be read, presented by the client according to the header variable "Content-Type"
	 * @param bodyLength the size of the data (body) that is to be returned in this response, i.e. the number of bytes that are to be read from the specified input stream.
	 */
	public HttpResponse(final int code, final String description, final InputStream is, final long bodyLength)
	{
		this(code, description);
		this.setBody(is, bodyLength);
	}

	/**	
	 * Create a new response that after instantiation can be sent to the connected client.<br>
    * <br>
    * Note that this constructor set the default headers (Server, Date, Connection).
	 * 
	 * @param httpVersion string containing the HTTP httpVersion to be sent in the response (for instance <code>1.1</code>).
	 * @param code The 3 digit response code (e.g. 200 or 404). 
	 * @param description The description of the response (i.e. "Ok" for a 200 response or "Not Found" for a 404 response) 
	 * @param is The inputstream from wich a response will be read, presented by the client according to the header variable "Content-Type"
	 * @param bodyLength the size of the data (body) that is to be returned in this response, i.e. the number of bytes that are to be read from the specified input stream.
	 */
	public HttpResponse(final String httpVersion, final int code, final String description, final InputStream is, final long bodyLength)
	{
		this(httpVersion, code, description);
		this.setBody(is, bodyLength);
	}
	
	/**
	 * Called when reading a HTTP message to parse the start line (request or status line).
	 * 
	 * @param startLine the HTTP message start line.
	 */
	protected void parseMessageStartLine(final String startLine) throws IOException
	{
		//Create a reader and a tokenizer on the first line
		StringTokenizer st = new StringTokenizer(startLine, HttpConstants.SP, false);
		      
		// First comes the httpVersion
		String _version = st.nextToken().toUpperCase();
		
		if( _version != null ) _version = _version.trim();
		this.httpVersion = _version;
										
		// Then the code
		this.code = Integer.parseInt( st.nextToken().trim() );
		
		// Description
		this.description = "";
		while(st.hasMoreTokens()) 
		{
		   this.description += st.nextToken();
         if( st.hasMoreTokens() ) this.description += HttpConstants.SP;
		}
	}
   
   /**
    * Sets default headers (Server, Date, Connection).
    * 
    * @since 2.0
    */
   public void setDefaultHeaders()
   {
      super.setHeader(SERVER_HEADER_KEY, JServer.getVersionString());
      super.setHeader(DATE_HEADER_KEY, HttpMessage.createHTTPDateString());
      super.setHeader(CONNECTION_HEADER_KEY, CONNECTION_CLOSE_HEADER_VALUE);
      super.setHeader(CONTENT_LENGTH_HEADER_KEY, Long.toString(0));      
   }
   
   /**
    * @return Returns the code.
    * 
    * @since 2.0
    */
   public int getCode()
   {
      return this.code;
   }
   
   /**
    * @param code The code to set.
    * 
    * @since 2.0
    */
   public void setCode(int code)
   {
      this.code = code;
   }
   
   /**
    * @return Returns the description.
    * 
    * @since 2.0
    */
   public String getDescription()
   {
      return this.description;
   }
   
   /**
    * @param description The description to set.
    * 
    * @since 2.0
    */
   public void setDescription(String description)
   {
      this.description = description;
   }
   
   /**
    * @return Returns the httpVersion.
    * 
    * @since 2.0
    */
   public String getHttpVersion()
   {
      return this.httpVersion;
   }
   
   /**
    * @param httpVersion The httpVersion to set.
    * 
    * @since 2.0
    */
   public void setHttpVersion(String httpVersion)
   {
      this.httpVersion = httpVersion;
   }
	
	/**	
	 * Set the body of the response. This method will convert the specified string into bytes 
	 * using the default encoding specified by the field {@link HttpConstants#DEFAULT_CHARACTER_ENCODING}.<br>
	 * <br>
	 * This method will also set the Content-length header of this response.
	 * 
	 * @param body The body of the response.
	 * 
	 * @since 1.3.1 build 690
	 */
	public void setBody(final byte[] body)
	{
		this.body = body;
		
		super.setHeader(CONTENT_LENGTH_HEADER_KEY, Long.toString(this.length()));
	}
	
	/**	
	 * Set the body of the response. This method will convert the specified string into bytes 
	 * using the default encoding specified by the field {@link HttpConstants#DEFAULT_CHARACTER_ENCODING}.<br>
	 * <br>
	 * This method will also set the Content-length header of this response.
	 * 
	 * @param body The body of the response.
	 */
	public void setBody(final String body)
	{
		try
		{
			this.setBody(body.getBytes(DEFAULT_CHARACTER_ENCODING));
		}
		catch(UnsupportedEncodingException uee)
		{
			this.setBody(body.getBytes());
		}
	}
	
	/**	
	 * Set the body of the response. <br>
	 * <br>
	 * This method will also set the Content-length header of this response.
	 * 
	 * @param body The body of the response.
	 * @param bodyEncoding the character encoding that will be used to convert the body string to byte data.
	 * 
	 * @since 1.13
	 */
	public void setBody(final String body, final String bodyEncoding) throws UnsupportedEncodingException
	{
		this.setBody(body.getBytes(bodyEncoding));
	}

	/**	
	 * Reads the body of the response from an inputstream.<br>
	 * <br>
	 * This method will also set the Content-length header of this response.
	 * 
	 * @param body the inputstream from which the body of the response will be read.
	 * @param bodyLength the size of the data (body) that is to be returned in this response, i.e. the number of bytes that are to be read from the specified input stream.
	 */
	public void setBody(final InputStream body, final long bodyLength)
	{
		this.bodyInputStream = body;
		this.bodyLength = bodyLength;
		
		super.setHeader(CONTENT_LENGTH_HEADER_KEY, Long.toString(this.length()));
	}
	
	/**
	 * Gets the response body, if set.
	 * 
	 * @since 1.2
	 */
	public byte[] getBody()
	{
		return this.body;
	}
	
	/**
	 * Gets the response body input stream, if set.
	 * 
	 * @since 1.2
	 */
	public InputStream getBodyInputStream()
	{
		return this.bodyInputStream;
	}
	
	/** 
	 * The length of the response body. 
	 * 
	 * @return Number of bytes in the assigned body part
	 */
	public long length()
	{
		if(this.bodyInputStream != null) return this.bodyLength;
		else return body.length;
	}
			
	/**
	 * Sends the entire response to the client using the stream 
	 * given as argument. <br>
	 * After this method has been invoked, no more data can be sent to the client.
	 * 
	 * @param os The outputstream on which to write the response.
	 * 
	 * @throws java.io.IOException If communication errors should occur.
	 */
	public void send(final OutputStream os) throws java.io.IOException
	{
		this.sendResponseLineAndHeaders(os);
		this.sendBody(os);
	}
	
	/**
	 * Gets the response message (excluding the body) as a string formatted according to the HTTP specification.
	 * 
	 * @return the response message as a string.
	 * 
	 * @since 1.2
	 */
	public String getResponseMessage()
	{
		String formattedHeaders = super.formatHeaders();
		StringBuffer rspLine = new StringBuffer();
		  
		rspLine.append(this.httpVersion);
		rspLine.append(" ");
		rspLine.append(Integer.toString(this.code));
		rspLine.append(" ");
		rspLine.append(this.description);
		rspLine.append("\r\n");
				  
		return rspLine.toString() + formattedHeaders + "\r\n";
	}
	
	/**	
	 * The representation of the HttpResponse. The returned string will not contain the body of the 
	 * request, if any.
	 * 
	 * @return Textual description of the response.
	 * 
	 * @since 1.2
	 */
   public String toString() 
   {
		return this.getResponseMessage();
   }

	/** 
	 * After the headers has been sent to the client, this method is 
	 * used to iteratively send response data to the client. 
	 * 
	 * @param os The outputstream on which to write the data.
	 * 
	 * @throws java.io.IOException If communication errors should occur.
	 */
	private void sendBody(OutputStream os) throws java.io.IOException
	{
		if( (this.bodyInputStream != null) && (this.bodyLength >= 0) ) //If a bodyInputstream has been specified
		{
			if( !(os instanceof BufferedOutputStream) )
			{
				os = new BufferedOutputStream(os);
			}
			
			byte[] buffer = new byte[maxBodyDataBufferSize];
			int read = -1;
			long bytesLeftToRead = this.bodyLength;

			while( (bytesLeftToRead > 0) && ((read = this.bodyInputStream.read(buffer, 0, buffer.length)) >= 0) )
			{
				os.write(buffer, 0, read);
				bytesLeftToRead -= read;
			}
         
         try{
         this.bodyInputStream.close();
         }catch (Exception e) {}
		}
		else
		{
			os.write(body);
		}
		
		os.flush();
	}

	/**	
	 * Send the response line and headers to the client. After the headers 
	 * has been sent, only body data can be sent to the client. 
	 * 
	 * @param os The outputstream on which to write the data.
	 * 
	 * @throws java.io.IOException If communication errors should occur.
	 */
	protected void sendResponseLineAndHeaders(final OutputStream os) throws java.io.IOException
	{
		if (!this.headersWritten)
		{
			this.sendResponseLine(os);
			super.sendHeaders(os);
			this.headersWritten = true;
		}
	}
		
	/**	
	 * Send the response first line (e.g. "HTTP/1.1 200 Ok").
	 * 
	 * @throws java.io.IOException If communication errors should occur.
	 */
	private void sendResponseLine(final OutputStream os) throws java.io.IOException
	{
		StringBuffer sb = new StringBuffer();

		sb.append(this.httpVersion);
		sb.append(" ");
		sb.append(Integer.toString(this.code));
		sb.append(" ");
		sb.append(this.description);
		sb.append("\r\n");

		os.write(sb.toString().getBytes(DEFAULT_CHARACTER_ENCODING));
	}
}
