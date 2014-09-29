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
package com.teletalk.jserver.tcp;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;

import com.teletalk.jserver.pool.PoolWorker;
import com.teletalk.jserver.util.NoHeadersObjectOutputStream;

/**
 * The class TcpConnection is an abstract class which primary function is to serve as a simple  
 * serverside tcp connection handler. This class is meant to be used with the class <code>TcpServer</code> to 
 * handle incomming tcp connections. Although it is also capable to function as a clientside 
 * tcp handler, {@link TcpEndPoint} combined with {@link TcpCommunicationManager} are better equipped 
 * to serve that purpose.
 * 
 * @see TcpServer
 * 
 * @author Tobias Löfstrand
 * 
 * @since 2.0.1
 */
public abstract class TcpConnection extends PoolWorker
{
   /**	The data of the TcpConnection. */
	protected TcpConnectionData connectionData = null;
		
	/** 
	 * The input stream object used by this class to read data from the socket. This field will be <code>null</code> 
	 * until one of the <code>readXXX</code> metods or the method {@link #initInputStream()} is called.<br>
	 * <br>
	 * Note that this field replaces the field <b><code>bufferedInput</code></b> found in
	 * earlier versions.
	 * 
	 * @since 1.2
	 */
	protected InputStream inputStream = null;
	
	/**	A reader for text input. */
	protected BufferedReader textReader = null;
	
	/**	An ObjectInputStream for reading objects. */
	protected ObjectInputStream objectReader = null;
	
	/** 
	 * The output stream object used by this class to write data to the socket. This field will be <code>null</code> 
	 * until one of the <code>writeXXX</code> metods or the method {@link #initOutputStream()} is called.
	 * 
	 * @since 1.2
	 */
	protected OutputStream outputStream = null;
		
	/**	A writer for text output. */
	protected PrintWriter textWriter = null;
	
	/**	An ObjectOutputStream for writing objects. */
	protected ObjectOutputStream objectWriter = null;
	
	/** The object used as lock when reading. */
	protected final Object readerLock = new Object();
	
	/** The object used as lock when writing. */
	protected final Object writerLock = new Object();
	
	// For the bugfix related to the problem with memory leakage in ObjectOutputStream and ObjectInputStream
	private int resetCount = 1;  //...Note that the actual value is set in TcpServer
	
	private boolean useAlternativeResetMethod = false; //For Java 2SE 1.3
	
	private int objectsWritten = 0;
	//private int objectsRead = 0;
	
	private TcpEndPointIdentifier localTcpEndPointIdentifier = null;
		
	private Socket socket;
	
	/**
	 * Constructs a new TcpConnection.
	 */
	public TcpConnection()
	{
		super();
	}
	
	/**
	 * Constructs a new TcpConnection.
	 * 
	 * @param name the name of the TcpConnection.
	 */
	public TcpConnection(String name)
	{
		super(name);
	}
   
   /**
    * Returns the data stored in this TcpConnection.
    * 
    * @return the data.
    * 
    * @see TcpConnectionData
    */
   public TcpConnectionData getTcpConnectionData()
   {
      return connectionData;
   }
	
	/**
	 * Sets the data stored in this TcpConnection.
	 * 
	 * @param data a TcpConnectionData object.
	 * 
	 * @exception ClassCastException if there was an error casting data to TcpConnectionData.
	 */
	protected void setData(final Object data) throws ClassCastException
	{
		connectionData = (TcpConnectionData)data;
		setSocket(connectionData.getSocket());
	}
	
	/**
	 * Sets the interval at which the object output streams of this TcpConnection will be reset. The value 
	 * specified by parameter <code>resetInterval</code> indicates how many object 
	 * writes ({@link #writeObject(Object)}) that have to be made before the streams are reset.
	 * <br><br>
	 * If the value of <code>resetInterval</code> is 0 (zero), stream reset is disabled.
	 * <br><br>
	 * The defaultvalue of this setting is 1.
	 * 
	 * @param resetInterval indicates how many object writes that have to be made before the object output stream is reset.
	 */
	public void setObjectStreamResetInterval(final int resetInterval)
	{
		if(resetInterval >= 0) this.resetCount = resetInterval;
	}
	
	/**
	 * Gets the interval at which the object input streams of this TcpConnection will be reset. The value 
	 * specified by parameter <code>resetInterval</code> indicates how many object 
	 * writes ({@link #writeObject(Object)}) that have to be made before the streams are reset.
	 * <br><br>
	 * If the value of <code>resetInterval</code> is 0 (zero), stream reset is disabled.
	 * <br><br>
	 * The defaultvalue of this setting is 1.
	 * 
	 * @return the number of object writes that have to be made before the object output stream is reset.
	 */
	public int getObjectStreamResetInterval()
	{
		return resetCount;
	}
	
	/**
	 * Sets the flag indicating if an alternative method should be used when resetting the object output stream 
	 * of this TcpConnection object. This alternative reset method involvs creating a 
	 * new ObjectOutputStream object (specifically a {@link com.teletalk.jserver.util.NoHeadersObjectOutputStream} object) to reset 
	 * the stream.<br>
	 * <br>
	 * The foremost purpose of this alternative reset method is to correct a bug introduced in Java 2 version 1.3, which makes impossible 
	 * to reset an ObjectOutputStream object and consequently leads to a memory leak.
	 * 
	 * @param useAlternativeResetMethod boolean flag indicating if the alternativ reset method should be used (<code>true</code>), or not (<code>false</code>).
	 */
	public void setUseAlternativeResetMethod(final boolean useAlternativeResetMethod)
	{
		this.useAlternativeResetMethod = useAlternativeResetMethod;
	}
	
	/**
	 * Checks if an alternative method will be used when resetting the object output stream 
	 * of this TcpConnection object. This alternative reset method involvs creating a 
	 * new ObjectOutputStream object (specifically a {@link com.teletalk.jserver.util.NoHeadersObjectOutputStream} object) to reset 
	 * the stream.<br>
	 * <br>
	 * The foremost purpose of this alternative reset method is to correct a bug introduced in Java 2 version 1.3, which makes impossible 
	 * to reset an ObjectOutputStream object and consequently leads to a memory leak.
	 * 
	 * @return <code>true</code> if the alternative reset method is currently used, otherwise <code>false</code>. 
	 */
	public boolean isUsingAlternativeResetMethod()
	{
		return useAlternativeResetMethod;
	}
	
	/**
	 * Method to manually reset the object output stream of this TcpConnection.
	 */
	public void resetObjectOutputStream() throws IOException
	{
		synchronized(writerLock)
		{
			doResetObjectOutputStream();
		}
	}
		
	// Internal method to reset the object output stream
	private void doResetObjectOutputStream() throws IOException
	{
		if(objectWriter == null) return;
		
		objectWriter.reset();

		if(useAlternativeResetMethod)
		{
			objectWriter = null;
			this.initObjectOutputStream(false);
		}
	}
		
	/**
	 * Gets the Socket associated with this TcpConnection.
	 * 
	 * @return a Socket object.
	 */
	public Socket getSocket()
	{
		return socket;
	}
	
	/**
	 * Sets the Socket of this TcpConnection.
	 * 
	 * @param socket a Socket object.
	 */
	protected void setSocket(Socket socket)
	{
		this.socket = socket;
	}
	
	/**
	 * Initializes the input stream (the field {@link #inputStream}) used to read data from the socket. All other 
	 * input streams reading data from the socket should be connected to that stream.<br>
	 * <br>
	 * Subclasses may override this method to create a custom implementation for the low level socket input stream.
	 * 
	 * @throws IOException if an error occurs while creating the stream.
	 *  
	 * @since 1.2
	 */
	protected void initInputStream() throws IOException
	{
		this.inputStream = new BufferedInputStream(getSocket().getInputStream());
	}
	
	/**
	 * Initializes the output stream (the field {@link #outputStream}) used to write data to the socket. All other 
	 * output streams writing data to the socket should be connected to that stream.<br>
	 * <br>
	 * Subclasses may override this method to create a custom implementation for the low level socket output stream.
	 * 
	 * @throws IOException if an error occurs while creating the stream.
	 * 
	 * @since 1.2
	 */
	protected void initOutputStream() throws IOException
	{
		this.outputStream = this.getSocket().getOutputStream();
	}
	
	/**
	 * Called to initialize the <code>java.io.ObjectInputStream</code> object (used in the method {@link #readObject()}) of this TcpConnection.
	 * 
	 * @since 1.3
	 */
	protected void initObjectInputStream() throws IOException
	{
		this.objectReader = new ObjectInputStream(this.inputStream);
	}
	
	/**
	 * Called to initialize the <code>java.io.ObjectOutputStream</code> object (used in the method {@link #writeObject(Object)}) of this TcpConnection.
	 * 
	 * @since 1.3
	 */
	protected void initObjectOutputStream(final boolean writeHeaders) throws IOException
	{
		if(writeHeaders) this.objectWriter = new ObjectOutputStream(this.outputStream);
		else this.objectWriter = new NoHeadersObjectOutputStream(this.outputStream);
	}
	
	/**
	 * Called to initialize the <code>java.io.BufferedReader</code> object (used in the method {@link #readLine()}) of this TcpConnection.
	 * 
	 * @since 1.3
	 */
	protected void initTextReader()
	{
		this.textReader = new BufferedReader(new InputStreamReader(this.inputStream));
	}
	
	/**
	 * Called to initialize the <code>java.io.PrintWriter</code> object (used in the method {@link #writeLine(String)}) of this TcpConnection.
	 * 
	 * @since 1.3
	 */
	protected void initTextWriter()
	{
		this.textWriter = new PrintWriter(this.outputStream);
	}
	
	/**
	 * Gets the ip address of the remote computer connected to this TcpConnection.
	 * 
	 * @return InetAddress object, <code>null</code> if this TcpConnection isn't connected.
	 */
	public InetAddress getRemoteInetAddress()
	{
      if( this.getRemoteAddress() != null ) return this.getRemoteAddress().getInetAddress(); 
		else return null;
	}
	
	/**
	 * Gets the remote port to which this TcpConnection is connected.
	 * 
	 * @return a port number, -1 if this TcpConnection isn't connected.
	 */
	public int getRemotePort()
	{
      if( this.getRemoteAddress() != null ) return this.getRemoteAddress().getPort(); 
		else return -1;
	}
	
	/**
	 * Gets the local ip address to which this TcpConnection is bound.
	 * 
	 * @return InetAddress object, <code>null</code> if this TcpConnection isn't connected.
	 */
	public InetAddress getLocalInetAddress()
	{
		if(getSocket() != null) return getSocket().getLocalAddress();
		else return null;
	}
	
	/**
	 * Gets the local port to which this TcpConnection is bound.
	 * 
	 * @return a port number, -1 if this TcpConnection isn't connected.
	 */
	public int getLocalPort()
	{
		if(getSocket() != null) return getSocket().getLocalPort();
		else return -1;
	}
	
	/**
	 * Gets the local address to which this TcpConnection is bound.
	 * 
	 * @return a TcpEndPointIdentifier object, <code>null</code> if this TcpConnection isn't connected.
	 */
	public TcpEndPointIdentifier getLocalAddress()
	{
		if(getSocket() != null)
		{
			if(localTcpEndPointIdentifier == null) localTcpEndPointIdentifier = new TcpEndPointIdentifier(this.getLocalInetAddress(), this.getLocalPort());
		
			return localTcpEndPointIdentifier;
		}
		else return null;
	}
		
	/**
	 * Gets the address of the remote computer connected to this TcpConnection.
	 * 
	 * @return a TcpEndPointIdentifier object, <code>null</code> if this TcpConnection isn't connected.
	 */
	public TcpEndPointIdentifier getRemoteAddress()
	{
      if( this.connectionData.getRemoteAddress() != null ) return this.connectionData.getRemoteAddress();
      else if( this.getSocket() != null ) return new TcpEndPointIdentifier(this.getSocket().getInetAddress(), this.getSocket().getPort());
      else return null;
	}
   
   /**
    * Gets the address of the server socket on which the socket was accepted.
    * 
    * @since 2.0.1 (20041111).
    */
   public TcpEndPointIdentifier getAcceptAddess()
   {
      return this.connectionData.getAcceptAddess();
   }
	
	/**
	 * Gets a string representation of this TcpConnection.
	 * 
	 * @return a string representation of this TcpConnection.
	 */
	public String toString()
	{
		return this.getName() + "@" + this.getRemoteAddress();
	}
	
	/**
	 * Reads a line from the inputstream of the Socket.
	 * 
	 * @return a String containing the contents of the line, not including any line-termination characters, or null if the end of the stream has been reached
	 * 
	 * @exception IOException if there was an error reading from the socket.
	 */
	public String readLine() throws IOException
	{
		synchronized(readerLock)
		{	
			if(this.inputStream == null) initInputStream();
			
			if(this.textReader == null) initTextReader();

			return this.textReader.readLine();
		}
	}
	
	/**
	 * Reads an Object from the inputstream of the Socket.
	 * 
	 * @return an Object.
	 * 
	 * @exception IOException if there was an error reading from the socket.
	 * @exception ClassNotFoundException if no class was found for the object that was read from the socket.
	 */
	public Object readObject() throws IOException, ClassNotFoundException
	{
		synchronized(readerLock)
		{
			if(this.inputStream == null) initInputStream();
			
			if(this.objectReader == null) initObjectInputStream();
			
			return objectReader.readObject();	
		}
	}
	
	/**
	 * Writes a String to the outputstream of the Socket.
	 * 
	 * @param str a String to be written.
	 * 
	 * @exception IOException if there was an error writing to the socket.
	 */
	public void writeLine(final String str) throws IOException
	{
		synchronized(writerLock)
		{
			if(this.outputStream == null) initOutputStream();

			if(this.textWriter == null) initTextWriter();
		
			this.textWriter.println(str);
			this.textWriter.flush();
		}
	}
	
	/**
	 * Writes an Object to the outputstream of the Socket.
	 * 
	 * @param obj an Object to be written.
	 * 
	 * @exception IOException if there was an error writing to the socket.
	 */
	public void writeObject(final Object obj) throws IOException
	{
		synchronized(writerLock)
		{
			if(this.outputStream == null) initOutputStream();
			
			if(this.objectWriter == null) initObjectOutputStream(true);
				
			this.objectWriter.writeObject(obj);

			// Reset object output stream
			if( (this.resetCount > 0) && (((++this.objectsWritten) % this.resetCount) == 0) )
			{
				doResetObjectOutputStream();
			}
		}
	}
	
	/**
	 * Closes the Socket associated with this TcpConnection.
	 */
	public void closeConnection()
	{
		// Close output streams
		try{
			if(this.textWriter != null) this.textWriter.close();
		}catch(Exception e){}
		try{
			if(this.objectWriter != null)	this.objectWriter.close();
		}catch(Exception e){}
		try{
			if(this.outputStream != null) this.outputStream.close();
		}catch(Exception e){}
		
		// Close input streams
		try{
			if(this.inputStream != null) this.inputStream.close();
		}catch(Exception e){}
		try{
			if(this.textReader != null) this.textReader.close();
		}catch(Exception e){}
		try{
			if(this.objectReader != null) this.objectReader.close();
		}catch(Exception e){}

		// Close socket
		try{
			if(getSocket() != null) getSocket().close();
		}catch(Exception e){}
      
      if( this.isDebugMode() ) logDebug("Connection closed.");
	}
	
	/**
	 * Resets various data.
	 */
	private void resetConnection()
	{
		inputStream = null;
		outputStream = null;
		textReader = null;
		textWriter = null;
		objectReader = null;
		objectWriter = null;
		connectionData = null;
		setSocket(null);
		
		localTcpEndPointIdentifier = null;
	}
	
	/**
	 * Performs default clean up by closing the connection. If subclasses override this method they <strong>MUST</strong> call the superclass 
	 * implementation or at least call the method <code>closeConnection()</code>.
	 */
	protected void cleanUp()
	{
		closeConnection();
		resetConnection();
	}
	
	/**
	 * Destroys this TcpConnection.
	 */
	protected void destroy()
	{
		closeConnection();
		resetConnection();
	}
}
