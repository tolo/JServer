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

import java.io.IOException;
import java.io.InvalidClassException;
import java.io.InvalidObjectException;
import java.io.NotSerializableException;
import java.io.OutputStream;

import com.teletalk.jserver.util.Streamable;

/**
 * Message writer implementation for sending an object message body.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.2
 */
public final class ObjectMessageWriter implements MessageWriter
{
	private final Object objectMessageBody;
	
	/**
	 * Creates a new ObjectMessageWriter for writing the specified object message body.
	 * 
	 * @param objectMessageBody the message body to dispatch.
	 */
	public ObjectMessageWriter(final Object objectMessageBody)
	{
		this.objectMessageBody = objectMessageBody;
	}
	
	/**
	 * Called to write a message (header and body) to an endpoint.
	 * 
	 * @param header the header of the message that is to be dispatched.
    * @param endPoint the endpoint on which the message is to be dispatched on.
    * @param endPointOutputStream the output stream of the endpoint on which the message is to be written to.
	 */
	public void writeMessage(MessageHeader header, MessagingEndPoint endPoint, OutputStream endPointOutputStream) throws IOException
	{
      // DISPATCH MESSAGE FOR PROTOCOL VERSION >= 4
      if( endPoint.getDestination().getProtocolVersion() >= 4 )
      {
         // Perform in-memory serialization of body to calculate data length
         if( serializeObjectMessageBody(header, endPoint, objectMessageBody) )
         {
            boolean isStreamableBody = ( (this.objectMessageBody instanceof Streamable) && (endPoint.getDestination().getProtocolVersion() >= 4) );
            
            endPoint.dispatchHeader(header);
            
            if(endPoint.isDebugMode()) endPoint.logDebug("Sending object message with header " + header + ".");
                        
            if( isStreamableBody )
            {
               endPoint.streamableSerializerByteStream.writeTo(endPointOutputStream);
               endPoint.resetStreamableSerializer();
            }
            else
            {
               endPoint.objectSerializerByteStream.writeTo(endPointOutputStream);
               endPoint.resetObjectSerializer(true, true);
            }
				
				if(endPoint.isDebugMode()) endPoint.logDebug("Done sending object message with header " + header + ".");
         }
         else
         {
            throw new MessageDispatchFailedException("Failed to serialize message body!", false);
         }
      }
      // DISPATCH MESSAGE FOR PROTOCOL VERSION <= 3
      else
      {
         // Perform in-memory serialization of body to calculate data length
			if( serializeObjectMessageBody(header, endPoint, objectMessageBody) )
			{
            endPoint.resetObjectSerializer(false, true); // Write a reset code to end of the body
            byte[] messageBodyBytes = endPoint.objectSerializerByteStream.toByteArray();
            endPoint.resetObjectSerializer(false, true); // Reset the object serializer stream...
            endPoint.resetObjectSerializer(true, false); // ...but clear the reset code from the byte array stream

				// Write header
				endPoint.dispatchHeader(header);
            endPoint.resetObjectSerializer(true, true);
            
            if(endPoint.isDebugMode()) endPoint.logDebug("Sending object message with header " + header + ".");

				// Write body data from the ByteArrayOutputStream that was used for serialization of the 
				// body object (in the method serializeObject).
            endPointOutputStream.write(messageBodyBytes);
            												
				if(endPoint.isDebugMode()) endPoint.logDebug("Done sending object message with header " + header + ".");
			}
			else
			{
				throw new MessageDispatchFailedException("Failed to serialize message body!", false);
			}
      }
	}
	
	/**
	 * Gets a description of the message body (for debug).
	 */
	public String getDescription()
	{
		if( this.objectMessageBody != null ) return this.objectMessageBody.toString();
		else return null;
	}
   
   /**
    * Internal convenience method to write a object message body (and catch a few exceptions).
    */
   private static boolean serializeObjectMessageBody(final MessageHeader header, final MessagingEndPoint endPoint, final Object body)
   {
      try
      {
         //if(super.getDebugMode()) logDebug("Serializing object message body with header " + header + ".");
         
         // Perform in-memory serialization to calculate body data length
         final long dataLength = endPoint.serializeObject(body);
         header.setBodyLength(dataLength); //Set new data length
         
         return true;
      }
      catch(InvalidClassException ice)
      {
         endPoint.logError("Unable to serialize object body for message with header " + header +  " - encountered invalid class during serialization.", ice);
      }
      catch(InvalidObjectException ivoe)
      {
         endPoint.logError("Unable to serialize object body for message with header " + header +  " - encountered invalid object during serialization.", ivoe);
      }
      catch(NotSerializableException nse)
      {
         endPoint.logError("Unable to serialize object body for message with header " + header +  " - object not serializable.", nse);
      }
      catch(Exception e)
      {
         endPoint.logError("Unable to serialize object body for message with header " + header +  ".", e);
      }
      
      return false;
   }
}