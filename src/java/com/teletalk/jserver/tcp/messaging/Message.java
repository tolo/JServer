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
package com.teletalk.jserver.tcp.messaging;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;

import com.teletalk.jserver.tcp.TcpEndPointIdentifier;
import com.teletalk.jserver.util.NoHeadersClassLoaderObjectInputStream;
import com.teletalk.jserver.util.NoHeadersObjectInputStream;
import com.teletalk.jserver.util.Streamable;

/**
 * This class represents an incomming message received from a remote messaging system. It contains the 
 * message header (accessible through the method {@link #getHeader()} and provides methods for getting 
 * the message body in a number of different ways (as an object, byte array and input stream).
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.2
 */
public class Message
{
	/**
	 * This class is used in the method getBodyAsStream() to be able to return a proxy object to the 
	 * input stream of the endpoint. This to prevent direct access to that stream from clients.
	 */
	private final static class MessagingEndPointInputStreamProxy extends InputStream
	{
		private final MessagingEndPointInputStream inputStream;
		private final Message message;
		
		public MessagingEndPointInputStreamProxy(final MessagingEndPointInputStream inputStream, final Message message)
		{
			this.inputStream = inputStream;
			this.message = message;
		}
		
		public int read() throws IOException 
		{
			synchronized(message)
			{
				if( !message.readCompleted )
				{
					return this.inputStream.read();
				}
				else return -1;
			}
		}
		
		public int read(final byte b[]) throws IOException 
		{
			return read(b, 0, b.length);
		}
		
		public int read(final byte b[], final int off, int len) throws IOException 
		{
			synchronized(message)
			{
				if( !message.readCompleted )
				{
					return this.inputStream.read(b, off, len);
				}
				else return -1;
			}
		}
		
		public int available() throws IOException
		{
			synchronized(message)
			{
				if( !message.readCompleted )
				{
					return this.inputStream.available();
				}
				else return 0;
			}
		}

		public synchronized long skip(final long n) throws IOException 
		{
			synchronized(message)
			{
				if( !message.readCompleted )
				{
					return this.inputStream.skip(n);
				}
				else return 0;
			}
		}

		public void close() throws IOException
		{
			synchronized(message)
			{
				if( !message.readCompleted )
				{
					this.message.signalReadCompletion();
				}
			}
		}

		public void mark(int readlimit) 
		{
		}

		public void reset() throws IOException 
		{
			throw new IOException("mark/reset not supported");
		}

		public boolean markSupported() 
		{
			return false;
		}
	}
		
	private final MessageHeader header;
	private MessagingEndPoint endPoint; // The endpoint from which the message will be read
	
	private Throwable error;
	
	private boolean consumed;
	boolean readCompleted;
	
   /** @since 1.3.1 */
	private final long receiveTime;
   
   /** @since 1.3.2 */
   private boolean messageBodyCachingEnabled;
   
   private byte[] cachedMessageBody;
   
   private Thread messageHandlerThread;
   
	
	/**
	 * Creates a new Message, using the specified message as a template. This constructor is provied to facilitate 
	 * wrapper class implementations.
	 * 
	 * @param message .
	 */
	protected Message(Message message)
	{
		this.header = message.header;
		this.endPoint = message.endPoint;
		
		this.consumed = message.consumed;
		this.readCompleted = message.readCompleted;
		
		this.receiveTime = System.currentTimeMillis();
      
      this.messageBodyCachingEnabled = message.messageBodyCachingEnabled;
      this.cachedMessageBody = message.cachedMessageBody;
	}
	
	/**
	 * Creates a new Message.
	 * 
	 * @param header the header of the message.
	 * @param endPoint the endpoint on which the message was received and from which the message body is to be read.
	 */
	protected Message(final MessageHeader header, final MessagingEndPoint endPoint)
	{
		this.header = header;
		this.endPoint = endPoint;
		this.error = null;
		
		this.consumed = false;
		this.readCompleted = false;
		
		this.receiveTime = System.currentTimeMillis();
      
      this.messageBodyCachingEnabled = false;
      this.cachedMessageBody = null;
	}
   
   /**
    * Gets the thread that is assigned to handle this message.
    * 
    * @since 2.1.5 (20070402)
    */
   public Thread getMessageHandlerThread()
   {
      return messageHandlerThread;
   }

   /**
    * Sets the thread that is assigned to handle this message.
    * 
    *  @since 2.1.5 (20070402)
    */
   public void setMessageHandlerThread(Thread messageHandlerThread)
   {
      this.messageHandlerThread = messageHandlerThread;
   }
	
	/**
    * Checks is this message has expired by checking the time that the message was received and the <code>timeToLive</code> 
    * attribute of the message header.
    * 
    * @return <code>true</code> if this message has expired, otherwise <code>false</code>. This method 
    * will also return <code>false</code> if no <code>timeToLive</code> has been specified in the message header.
    * 
    * @since 1.3.1
	 */
	public boolean hasExpired()
	{
		if( !this.header.isAsynch() && (this.header.getTimeToLive() > 0) )
      {
         return ((System.currentTimeMillis() - this.receiveTime) > this.header.getTimeToLive());
      }
      else
      {
         return false;
      }
	}
	
	/**
    * Gets the time when this message will expire. If the <code>timeToLive</code> field of the 
    * message header hasn't been set, this method will return <code>-1</code>.
    * 
    * @return the time, expressed as milliseconds since January 1, 1970, 00:00:00 GMT, when the message will expire, or <code>-1</code> if 
    * the message won't expire.
    * 
    * @since 1.3.1 
	 */
	public long getExpireTime()
	{
      if( !this.header.isAsynch() && (this.header.getTimeToLive() > 0) )
      {
         return (this.receiveTime + this.header.getTimeToLive());
      }
      else
      {
         return -1;
      }
	}
	
	/**
    * Gets the time when this message was received.
    * 
    * @return the time, expressed as milliseconds since January 1, 1970, 00:00:00 GMT, when the message was received.
    * 
    * @since 1.3.1
	 */
	public long getReceiveTime()
	{
		return this.receiveTime;
	}
		
	/**
	 * Gets the {@link MessagingEndPoint} associated with this Message.
	 * 
	 * @return the {@link MessagingEndPoint} associated with this Message.
	 * 
	 * @since 1.3
	 */
	public MessagingEndPoint getEndPoint()
	{
		return this.endPoint;
	}
				
	/**
	 * Gets the header of this message.
	 * 
	 * @return the header of this message.
	 */
	public MessageHeader getHeader()
	{
		return this.header;
	}
	
	/**
	 * Gets the {@link Destination} object representing the sender of this message.
	 * 
	 * @return the {@link Destination} object representing the sender of this message. 
	 * 
	 * @since 2.0 Build 758
	 */
	public Destination getDestination()
	{
	   return this.endPoint.getDestination();
	}
   
   /**
    * Gets the address which identifies the {@link Destination} (endpoint group) to which the endpoint, on which this message was 
    * received, belongs. In other words, the address returned by this method may be viewed as the sender address of the message.
    * 
    * @return a TcpEndPointIdentifier object representing the address of the {@link Destination} from which this message originated. 
    * 
    * @since 2.0.1
    */
   public TcpEndPointIdentifier getDestinationAddress()
   {
      return this.endPoint.getDestination().getAddress();
   }
	
	/**
	 * Gets the local address to which the endpoint (<b>socket</b>) that this message was received on is bound to. <b>Caution!</b> Do not confuse 
    * this method with {@link #getDestinationAddress()}, which gets the address of the associated destination (endpoint group).
	 * 
	 * @return a TcpEndPointIdentifier object representing the local address.
	 *  
	 * @since 1.3
    * 
    * @deprecated as of 2.0.1, due to possible risk of confusion. Most applications will never need to use this method, and if they do, they can access the endpoint local 
    * address through the endpoint object. 
	 */
	public TcpEndPointIdentifier getLocalAddress()
	{
		return (this.endPoint != null) ? this.endPoint.getLocalAddress() : null;
	}
	
	/**
	 * Gets the address of the remote endpoint (<b>socket</b>) from which this message was sent. <b>Caution!</b> Do not confuse 
    * this method with {@link #getDestinationAddress()}, which gets the address of the associated destination (endpoint group).
	 * 
	 * @return a TcpEndPointIdentifier object object representing the remote address. 
	 * 
	 * @since 1.3
    * 
    * @deprecated as of 2.0.1, due to possible risk of confusion. Most applications will never need to use this method, and if they do, they can access the endpoint remote  
    * address through the endpoint object.
	 */
	public TcpEndPointIdentifier getRemoteAddress()
	{
		return (this.endPoint != null) ? this.endPoint.getRemoteAddress() : null;
	}
	
	/**
	 * Checks if the sender of this message expects a response. If protocol version > 3 this method checks if the 
    * message was sent asynchronously (i.e the return value is identical to that of {@link MessageHeader#isAsynch()}), 
    * otherwise the value of the "response to id" field is checked to see if this message is a response message itself.  
	 * 
	 * @return <code>true</code> if sender expects a response, otherwise <code>false</code>.
	 */
	public boolean expectingResponse()
	{
		return this.header.expectingResponse();
	}
	
	/**
	 * Checks if the message body has been consumed, i.e. if one of the methods {@link #getBodyAsByteArray()},
	 * {@link #getBodyAsObject()}, {@link #getBodyAsObject(ClassLoader)} or {@link #getBodyAsStream()} has 
	 * been called.
	 * 
	 * @return <code>true</code> if the message body has been consumed, otherwise <code>false</code>.
	 */
	public synchronized boolean isConsumed()
	{
		return this.consumed;
	}
	
	/**
	 * Checks if the message body has been fully read.
	 * 
	 * @return <code>true</code> if the message body has been fully read, otherwise <code>false</code>.
	 * 
	 * @since 1.3
	 */
	public synchronized boolean isReadCompleted()
	{
		return readCompleted;
	}
	
	/**
	 * Blocks the calling thread until the message body has been read.
	 * 
	 * @param timeOut the maximum time to wait for message body read completion.
	 * 
	 * @return <code>true</code> if reading of message body is completed, otherwise <code>false</code>.
	 * 
	 * @throws InterruptedException if the calling thread was interrupted while waiting for reading of the message body to complete.
	 */
	public synchronized boolean waitForReadCompletion(final long timeOut) throws InterruptedException
	{
		if( !this.readCompleted )
		{
			final long beginWait = System.currentTimeMillis();
			long waitTime = timeOut;
			
			while( (!readCompleted) && (waitTime > 0) )
			{
				this.wait(waitTime);
				waitTime = timeOut - (System.currentTimeMillis() - beginWait);
			}
		}
		
		return this.readCompleted;
	}
	
	/**
	 * Called to signal that reading of the message body is completed. This method is invoked 
	 * automatically by the messaging system under normal circumstances.
	 */
	public synchronized void signalReadCompletion()
	{
		this.readCompleted = true;
		this.notify();
	}
   
   /**
    * Checks if the caching of message body is currently enabled.
    * 
    * @since 2.0  
    */
   public boolean isMessageBodyCachingEnabled()
   {
      return this.messageBodyCachingEnabled;
   }

   /**
    * Sets the flag indicating if message body is to be cached when read, thus making it possible to "read" the message 
    * body several times.
    * 
    * @since 2.0 
    */
   public void setMessageBodyCachingEnabled(boolean messageBodyCachingEnabled)
   {
      this.messageBodyCachingEnabled = messageBodyCachingEnabled;
   }
   
   /**
    * Initializes the cached message body.
    */
   private void initCachedMessageBody() throws IOException
   {
      if( !this.consumed )
      {
         try
         {
            this.consumed = true;
            
            this.cachedMessageBody = new byte[(int)this.header.getBodyLength()];
               
            MessagingEndPointInputStream bodyInputStream = this.endPoint.getMessagingEndPointInputStream();
      
            for(int read=0; read<this.cachedMessageBody.length;)
            {
               read += bodyInputStream.read(this.cachedMessageBody, read, (this.cachedMessageBody.length - read));
            }
         }
         catch(Throwable t)
         {
            this.handleError(t);
         }
         finally
         {
            this.signalReadCompletion();
         }
      }
   }
   
   /**
    * Reads and caches the message body.
    * 
    * @since 2.1.6 (20070503)
    */
   public void readAndCacheBody() throws IOException
   {
      this.setMessageBodyCachingEnabled(true);
      this.initCachedMessageBody();
   }
			
	/**
	 * Gets the message body as a raw byte array.<br>
	 * <br>
	 * <i>Note: </i> calling this method will "consume" the message body, which means that 
	 * repeated calls to this method, and the other methods for getting the message body, will 
	 * return <code>null</code>.
	 * 
	 * @return the message body as a raw byte array.
    * 
    * @throws IOException if an error occurrs while reading the message body.
	 */
	public synchronized byte[] getBodyAsByteArray() throws IOException
	{
		if( this.messageBodyCachingEnabled )
      {
         this.initCachedMessageBody();
         
         return this.cachedMessageBody;
      }
      else if( !this.consumed && !this.readCompleted )
		{
         try
         {
   			this.consumed = true;
   			final byte[] byteArrayMessageBody = new byte[(int)this.header.getBodyLength()];
   			
   			MessagingEndPointInputStream bodyInputStream = this.endPoint.getMessagingEndPointInputStream();
   
   			for(int read=0; read<byteArrayMessageBody.length;)
   			{
   				read += bodyInputStream.read(byteArrayMessageBody, read, (byteArrayMessageBody.length - read));
   			}
   						
   			return byteArrayMessageBody;
         }
         catch(Throwable t)
         {
            this.handleError(t, false);
         }
         finally
         {
            this.signalReadCompletion();
         }
		}
		return null;
	}
	
	/**
	 * Gets the message body as an object (by attempting to deserialize the message body data).<br>
	 * <br>
	 * <i>Note: </i> calling this method will "consume" the message body, which means that 
	 * repeated calls to this method, and the other methods for getting the message body, will 
	 * return <code>null</code>.
	 * 
	 * @return the message body as an object deserialized from the message body data.
    * 
    * @throws IOException if an error occurrs while reading the message body.
	 */
	public synchronized Object getBodyAsObject() throws IOException
	{
		return this.getBodyAsObject(null);
	}
	
	/**
	 * Gets the message body as an object (by attempting to deserialize the message body data). The specified 
	 * ClassLoader will be used to resolve the class of the read object.<br>
	 * <br>
	 * <i>Note: </i> calling this method will "consume" the message body, which means that 
	 * repeated calls to this method, and the other methods for getting the message body, will 
	 * return <code>null</code>.
	 * 
	 * @param classLoader the ClassLoader that is to be used to resolve the class of the read object.
	 * 
	 * @return the message body as an object deserialized from the message body data.
    * 
    * @throws IOException if an error occurrs while reading the message body.
	 * 
	 * @since 1.3
	 */
	public synchronized Object getBodyAsObject(final ClassLoader classLoader) throws IOException
	{
      if( this.messageBodyCachingEnabled )
      {
         this.initCachedMessageBody();
         
         try
         {
            ObjectInputStream objectInput;
            
            if( classLoader != null )
            {
               objectInput = new NoHeadersClassLoaderObjectInputStream(new ByteArrayInputStream(this.cachedMessageBody), classLoader);
            }
            else
            {
               objectInput = new NoHeadersObjectInputStream(new ByteArrayInputStream(this.cachedMessageBody));
            }
            
            return objectInput.readObject();
         }
         catch(ClassNotFoundException ncfe)
         {
            throw new IOException(ncfe.toString());
         }
      }
      else if( !this.consumed && !this.readCompleted )
		{
         try
         {
				this.consumed = true;
				
				if( classLoader != null ) return this.endPoint.readObject(classLoader);
				else return this.endPoint.readObject();
			}
			catch(Throwable t)
			{
				this.handleError(t, false);
			}
			finally
			{
				this.signalReadCompletion();
			}
		}
		return null;
	}
		
   /**
    * Gets the message body as an {@link Streamable} object, by attempting to deserialize the message body data using the Streamable object 
    * specified by parameter <code>streamable</code>.<br>
    * <br>
    * <i>Note: </i> calling this method will "consume" the message body, which means that 
    * repeated calls to this method, and the other methods for getting the message body, will 
    * return <code>false</code>.
    * 
    * @return <code>true</code> if the streamable deserialized from the message body, otherwise <code>false</code>. 
    * 
    * @throws IOException if an error occurrs while reading the message body.
    * 
    * @since 1.3.1, build 670.
    */
	public synchronized boolean getBodyAsStreamable(final Streamable streamable) throws IOException
	{
      if( this.messageBodyCachingEnabled )
      {
         this.initCachedMessageBody();
         
         MessagingEndPointInputStream input = new MessagingEndPointInputStream(new ByteArrayInputStream(this.cachedMessageBody));
         streamable.read(input);
         return true; 
      }
      else if( !this.consumed && !this.readCompleted )
      {
         try
         {
            streamable.read(this.endPoint.getMessagingEndPointInputStream());
            return true;
         }
         catch(Throwable t)
         {
            this.handleError(t, false);
         }
         finally
         {
            this.signalReadCompletion();
         }
      }
      return false;
	}
	
	/**
	 * Gets an InputStream for reading the message body.<br>
	 * <br>
	 * <i>Note: </i> calling this method will "consume" the message body, which means that 
	 * repeated calls to this method, and the other methods for getting the message body, will 
	 * return <code>null</code>.
	 * 
	 * @return an InputStream for reading the message body.
	 */
	public synchronized InputStream getBodyAsStream()
	{
      if( this.messageBodyCachingEnabled )
      {
         try
         {
            this.initCachedMessageBody();
            
            return new ByteArrayInputStream(this.cachedMessageBody);
         }
         catch(IOException ioe)
         {
            return new MessagingEndPointInputStreamProxy(null, this); // Create this object, which only will return -1/EOF.  
         }
      }
		else if( !this.consumed && !this.readCompleted )
		{
			this.consumed = true;
			
			return new MessagingEndPointInputStreamProxy( this.endPoint.getMessagingEndPointInputStream(), this );
		}
		else return null;
	}
	
	/**
	 * Gets the error, if any, that occurred while reading body data.
	 * 
	 * @return the error that occurred while reading body data or <code>null</code> if no error occurred.
	 */
	protected Throwable getError()
	{
		return this.error;
	}
   
   /**
    * Called to indicate that an error occurred while reading body data.
    * 
    * @param t the error that occurred.
    */
   protected void handleError(final Throwable t) throws IOException
   {
      this.handleError(t, true);
   }
		
	/**
	 * Called to indicate that an error occurred while reading body data.
	 * 
	 * @param t the error that occurred.
    * 
    * @since 2.1.2 (20060308)
	 */
	protected void handleError(final Throwable t, final boolean signalReadCompletion) throws IOException
	{
		this.error = t;
      
      // Notify endpoint that a read error has occurred
      this.endPoint.bodyReadErrorOccurred(t);
      
      if( signalReadCompletion ) this.signalReadCompletion();
		
		if( t instanceof IOException )
		{
			if( this.endPoint.isDebugMode() ) this.endPoint.logError("Caught IOException while reading object message body!", t);
         throw (IOException)t;
		}
		else if( t instanceof Exception )
		{
         if( this.endPoint.isDebugMode() ) this.endPoint.logError("Caught Exception while reading object message body!", t);
			throw new IOException("Error occurred while reading object message body from endpoint " + this.endPoint.toString() + "! Nested error is: " + t + ".");
		}
		else if( t instanceof Error )
		{
         if( this.endPoint.isDebugMode() ) this.endPoint.logError("Caught Error while reading object message body!", t);
         throw (Error)t;
		}
		else
		{
			// Should not occurr during normal circumstances. Included for consistency.
         if( this.endPoint.isDebugMode() ) this.endPoint.logError("Caught Throwable while reading object message body!", t);
			throw new IOException("Error occurred while reading object message body from endpoint " + this.endPoint.toString() + "! Nested error is: " + t + ".");
		}
	}
	
   /**
    * Finalizes this message by signalling read completion. 
    */
   protected void finalize() throws Throwable
   {
      this.signalReadCompletion();
      
      super.finalize();
   }
   
   /**
    * Gets a string representation of this Message.
    * 
    * @return a string representation of this BlipCommandHeader.
    */
   public String toString()
   {
      return "Message[" +
                                    "destination: " + this.getDestination() +
                                    ", consumed: " + this.consumed + 
                                    ", readCompleted: " + this.readCompleted + 
                                    ", receiveTime: " + this.receiveTime + 
                                    ", messageBodyCachingEnabled: " + this.messageBodyCachingEnabled +
                                    ", header: " + this.header + "]";
   }
}
