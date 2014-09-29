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
package com.teletalk.jserver.util.filedb;

import java.io.File;
import java.io.RandomAccessFile;
import java.io.Serializable;

import junit.framework.TestCase;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.teletalk.jserver.util.FileDeletor;
import com.teletalk.jserver.util.filedb.DefaultDataFile;
import com.teletalk.jserver.util.filedb.LowLevelFileDB;

/**
 * 
 * @author Tobias Löfstrand
 */
public class LowLevelFileDBTest extends TestCase
{
   private static final String BASE_PATH = ""; //"I:\\Temp\\";
   
   
   private static final Log logger = LogFactory.getLog(LowLevelFileDBTest.class);
   
   private static int failCount = 0;
   
   
   private static void resetFailCount()
   {
      synchronized (LowLevelFileDBTest.class)
      {
         failCount = 0;
      }
   }
   
   private static void incrementFailCount()
   {
      synchronized (LowLevelFileDBTest.class)
      {
         failCount++;
      }
   }
   
   private static int getFailCount()
   {
      synchronized (LowLevelFileDBTest.class)
      {
         return failCount;
      }
   }
   
   
   /* ### TEST METHODS ### */

   
   /**
    * testUpdateAndGetPartial
    */
   public void testUpdateAndGetPartial()
   {
      logger.info("BEGIN testUpdateAndGetPartial.");
      
      testUpdateAndGetPartialInternal(5, false);
      testUpdateAndGetPartialInternal(10, false);
      testUpdateAndGetPartialInternal(15, false);
      testUpdateAndGetPartialInternal(20, false);
      testUpdateAndGetPartialInternal(5, true);
      testUpdateAndGetPartialInternal(10, true);
      testUpdateAndGetPartialInternal(15, true);
      testUpdateAndGetPartialInternal(20, true);
      
      logger.info("END testUpdateAndGetPartial.");
   }
   /**
    */
   private void testUpdateAndGetPartialInternal(final int allocationUnitSize, final boolean useDataFileChecksums)
   {
      String fileNameBase = BASE_PATH + "fileDBPartial";

      try
      {
         new File(fileNameBase + ".idx").delete();
         new File(fileNameBase + ".dat").delete();
         
         int n = 10;
         int blockSize = (allocationUnitSize + DefaultDataFile.BLOCK_HEADER_SIZE + DefaultDataFile.BLOCK_FOOTER_SIZE);
         
         LowLevelFileDB lowLevelFileDB = new LowLevelFileDB("LowLevelFileDB", fileNameBase, blockSize,  
            1, 1, true, useDataFileChecksums, LowLevelFileDB.READ_WRITE_MODE);
         
         String key = "key";
         String dataString = "DATA123456";
         byte[] data = dataString.getBytes();
         lowLevelFileDB.insertItem(key, new byte[n*data.length]);
         
         for(int i=0; i<n; i++)
         {
            lowLevelFileDB.updatePartialItem(key, data, i * data.length);
         }
                  
         String dataRead = new String(lowLevelFileDB.getItem(key));
         if( !dataRead.startsWith(dataString) ) super.fail("Invalid data returned by getItem! " + dataRead);
         for(int i=0; i<n; i++)
         {
            dataRead = new String(lowLevelFileDB.getPartialItem(key, i * data.length, data.length));
            if( !dataRead.equals(dataString) ) super.fail("Invalid data at index " + i + ". dataRead: " + dataRead + ".");
         }
         
         lowLevelFileDB.closeFileDB();
         
         
         // Reopen and test again
         lowLevelFileDB = new LowLevelFileDB("LowLevelFileDB", fileNameBase, blockSize,  
               1, 1, true, useDataFileChecksums, LowLevelFileDB.READ_WRITE_MODE);
         
         dataRead = new String(lowLevelFileDB.getItem(key));
         if( !dataRead.startsWith(dataString) ) super.fail("Invalid data returned by getItem! " + dataRead);
         for(int i=0; i<n; i++)
         {
            dataRead = new String(lowLevelFileDB.getPartialItem(key, i * data.length, data.length));
            if( !dataRead.equals(dataString) ) super.fail("Invalid data at index " + i + ". dataRead: " + dataRead + ".");
         }
         
         
         lowLevelFileDB.closeFileDB();
         
         new File(fileNameBase + ".idx").delete();
         new File(fileNameBase + ".dat").delete();
      }
      catch (Exception e) 
      {
         e.printStackTrace();
         super.fail("Error - " + e);
      }
   }
   
   
   /**
    * testAppend
    */
   public void testAppend()
   {
      logger.info("BEGIN testAppend.");
      
      testAppendInternal(5, false);
      testAppendInternal(10, false);
      testAppendInternal(15, false);
      testAppendInternal(20, false);
      testAppendInternal(5, true);
      testAppendInternal(10, true);
      testAppendInternal(15, true);
      testAppendInternal(20, true);
      
      logger.info("END testAppend.");
   }
   
   /**
    */
   private void testAppendInternal(final int allocationUnitSize, final boolean useDataFileChecksums)
   {
      String fileNameBase = BASE_PATH + "fileDBAppend";
      
      try
      {
         new File(fileNameBase + ".idx").delete();
         new File(fileNameBase + ".dat").delete();
         
         final int n = 10;
         final int blockSize = (allocationUnitSize + DefaultDataFile.BLOCK_HEADER_SIZE + DefaultDataFile.BLOCK_FOOTER_SIZE);
         
         LowLevelFileDB lowLevelFileDB = new LowLevelFileDB("LowLevelFileDB", fileNameBase, blockSize, 
               1, 1, true, useDataFileChecksums, LowLevelFileDB.READ_WRITE_MODE);
         
         String key = "key";
         String dataString = "DATA123456";
         byte[] data = dataString.getBytes();
         lowLevelFileDB.insertItem(key, data);
         
         for(int i=1; i<n; i++)
         {
            lowLevelFileDB.appendItem(key, data);
         }
         
         
         if( !lowLevelFileDB.containsItem(key) ) super.fail("Item not found!");
         String dataRead = new String(lowLevelFileDB.getItem(key));
         if( !dataRead.startsWith(dataString + dataString) ) super.fail("Invalid data returned by getItem! " + dataRead);
         for(int i=0; i<n; i++)
         {
            dataRead = new String(lowLevelFileDB.getPartialItem(key, i * data.length, data.length));
            if( !dataRead.equals(dataString) ) super.fail("Invalid data at index " + i + ". dataRead: " + dataRead + ".");
         }
         
         lowLevelFileDB.closeFileDB();
         
         
         // Reopen and test again
         lowLevelFileDB = new LowLevelFileDB("LowLevelFileDB", fileNameBase, blockSize, 
               1, 1, true, useDataFileChecksums, LowLevelFileDB.READ_ONLY_MODE);
         
         if( !lowLevelFileDB.containsItem(key) ) super.fail("Item not found!");
         dataRead = new String(lowLevelFileDB.getItem(key));
         if( !dataRead.startsWith(dataString + dataString) ) super.fail("Invalid data returned by getItem! " + dataRead);
         for(int i=0; i<n; i++)
         {
            dataRead = new String(lowLevelFileDB.getPartialItem(key, i * data.length, data.length));
            if( !dataRead.equals(dataString) ) super.fail("Invalid data at index " + i + ". dataRead: " + dataRead + ".");
         }
         
         
         lowLevelFileDB.closeFileDB();
         
         new File(fileNameBase + ".idx").delete();
         new File(fileNameBase + ".dat").delete();
      }
      catch (Exception e) 
      {
         e.printStackTrace();
         super.fail("Error - " + e);
      }
   }
   
   /**
    * testUpdateTimestamp
    */
   public void testUpdateTimestamp()
   {
      logger.info("BEGIN testUpdateTimestamp.");
      
      String fileNameBase = "fileDBTimestamp";
      
      try
      {
         new File(fileNameBase + ".idx").delete();
         new File(fileNameBase + ".dat").delete();
         
         final int allocationUnitSize = 10;
         final int blockSize = (allocationUnitSize + DefaultDataFile.BLOCK_HEADER_SIZE + DefaultDataFile.BLOCK_FOOTER_SIZE);
         
         LowLevelFileDB lowLevelFileDB = new LowLevelFileDB("LowLevelFileDB", fileNameBase, blockSize, 1, 1,  
               LowLevelFileDB.READ_WRITE_MODE);
         
         String key = "key";
         String dataString = "DATA123456";
         byte[] data = dataString.getBytes();
         lowLevelFileDB.insertItem(key, data);
         
         long timestamp = lowLevelFileDB.getItemTimestamp(key);
         
         Thread.sleep(100);
         
         lowLevelFileDB.updateItemTimestamp(key, System.currentTimeMillis());
         
         if( lowLevelFileDB.getItemTimestamp(key) == timestamp )
         {
            super.fail("Timestamp hasn't been updated!");
         }
         
         lowLevelFileDB.closeFileDB();
         
         new File(fileNameBase + ".idx").delete();
         new File(fileNameBase + ".dat").delete();
      }
      catch (Exception e) 
      {
         e.printStackTrace();
         super.fail("Error - " + e);
      }
      
      logger.info("END testUpdateTimestamp.");
   }
   
   /**
    * testModifiedExternally
    */
   public void testModifiedExternally()
   {
      logger.info("BEGIN testModifiedExternally.");
      
      String fileNameBase = BASE_PATH + "fileDBTimestamp";
      
      try
      {
         new File(fileNameBase + ".idx").delete();
         new File(fileNameBase + ".dat").delete();
         
         final int allocationUnitSize = 10;
         final int blockSize = (allocationUnitSize + DefaultDataFile.BLOCK_HEADER_SIZE + DefaultDataFile.BLOCK_FOOTER_SIZE);
         
         LowLevelFileDB lowLevelFileDB1 = new LowLevelFileDB("LowLevelFileDB", fileNameBase, blockSize, 1, 1,  
               LowLevelFileDB.READ_WRITE_MODE);
         lowLevelFileDB1.closeFileDB();
                  
         // Make testing work on FAT32
         new File(fileNameBase + ".idx").setLastModified(System.currentTimeMillis() - 60*1000);
         new File(fileNameBase + ".dat").setLastModified(System.currentTimeMillis() - 60*1000);
         
         lowLevelFileDB1 = new LowLevelFileDB("LowLevelFileDB", fileNameBase, blockSize, 1, 1,  
                  LowLevelFileDB.READ_WRITE_MODE);
         
         Thread.sleep(100);
                  
         LowLevelFileDB lowLevelFileDB2 = new LowLevelFileDB("LowLevelFileDB", fileNameBase, blockSize, 1, 1,  
               LowLevelFileDB.READ_ONLY_MODE);
                     
         String key = "key";
         String dataString = "DATA123456";
         byte[] data = dataString.getBytes();
         
         Thread.sleep(250); // Sleep to make sure that the last modified timestamp of the file will be different that the initial timestamp 
         
         lowLevelFileDB1.insertItem(key, data);
         lowLevelFileDB1.getDataFile().flush();
                  
         for(int i=0; i<10; i++) // Allow a maximum of one second for the modification to get detected
         {
            if( !lowLevelFileDB2.isModifiedExternally() )
            {
               Thread.sleep(1000);
               logger.warn("testModifiedExternally - file modification not detected yet (" + i + ")!");               
            }
            else break;
         }
         
         if( !lowLevelFileDB2.isModifiedExternally() )
         {
            super.fail("External modification not detected!");
         }
         
         /*if( lowLevelFileDB1.isModifiedExternally() ) // Will only work when testing against a local NTFS file system
         {
            super.fail("External modification shoult NOT be detected in lowLevelFileDB1!");
         }*/
         
         lowLevelFileDB1.closeFileDB();
         lowLevelFileDB2.closeFileDB();
         
         new File(fileNameBase + ".idx").delete();
         new File(fileNameBase + ".dat").delete();
      }
      catch (Exception e) 
      {
         e.printStackTrace();
         super.fail("Error - " + e);
      }
      
      logger.info("END testModifiedExternally.");
   }
   
   
   /**
    * testInsertAppendGetAndDeleteObject
    */
   public void testInsertAppendGetAndDeleteObject()
   {
      logger.info("BEGIN testInsertAppendGetAndDeleteObject.");
      
      String fileNameBase = BASE_PATH + "fileDBObjects";
      
      try
      {
         new File(fileNameBase + ".idx").delete();
         new File(fileNameBase + ".dat").delete();
         
         final int allocationUnitSize = 10;
         final int blockSize = (allocationUnitSize + DefaultDataFile.BLOCK_HEADER_SIZE + DefaultDataFile.BLOCK_FOOTER_SIZE);
         
         LowLevelFileDB lowLevelFileDB = new LowLevelFileDB("LowLevelFileDB", fileNameBase, blockSize, 1, 1,  
               LowLevelFileDB.READ_WRITE_MODE);
         
         String key = "key";
         TestObject data1 = new TestObject("DATA123456");
         TestObject data2 = new TestObject("DATA654321");
         
         
         // Test insert and get
         lowLevelFileDB.insertItem(key, data1);
         int sizeOfOneSerializedObject = lowLevelFileDB.getItemSize(key);

         Object object = lowLevelFileDB.getItemAsObject(key);
         if( !object.equals(data1) ) super.fail("Data is invalid (" + data1 + "/" + object + ")");
         
         int appendSize = 9; 
         for(int i=0; i<appendSize; i++)
         {
            lowLevelFileDB.appendItem(key, data2);
         }
                  
         Object[] objects = lowLevelFileDB.getItemAsObjects(key);
         
         if( objects == null ) super.fail("Data for items not found!");
         if( objects.length != (appendSize + 1) ) super.fail("Method getItemAsObjects (1) returned incorrect number of items (" + objects.length + ")!");
         if( !objects[0].equals(data1) ) super.fail("Item 1 is invalid (" + objects[0] + ")!");
         if( !objects[1].equals(data2) ) super.fail("Item 2 is invalid (" + objects[1] + ")!");
         if( !objects[appendSize].equals(data2) ) super.fail("Item " + (appendSize + 1) + " is invalid (" + objects[appendSize] + ")!");
         
         
         // Test corrupt data at the end
         int itemSize = lowLevelFileDB.getItemSize(key);
         lowLevelFileDB.updatePartialItem(key, new byte[sizeOfOneSerializedObject/2], itemSize-1-(sizeOfOneSerializedObject/2));
         objects = lowLevelFileDB.getItemAsObjects(key, true);
         if( objects.length != appendSize ) super.fail("Method getItemAsObjects (2) returned incorrect number of items (" + objects.length + ")!");
         
         lowLevelFileDB.closeFileDB();
         
         
         // Reopen and test again
         lowLevelFileDB = new LowLevelFileDB("LowLevelFileDB", fileNameBase, blockSize, 1, 1,  
               LowLevelFileDB.READ_WRITE_MODE);
         
         itemSize = lowLevelFileDB.getItemSize(key);
         lowLevelFileDB.updatePartialItem(key, new byte[sizeOfOneSerializedObject/2], itemSize-1-(sizeOfOneSerializedObject/2));
         objects = lowLevelFileDB.getItemAsObjects(key, true);
         if( objects.length != appendSize ) super.fail("Method getItemAsObjects (2) returned incorrect number of items (" + objects.length + ")!");
         
         
         // Test delete
         lowLevelFileDB.deleteItem(key);
         if( lowLevelFileDB.containsItem(key) ) super.fail("Item should be deleted!");
         
         lowLevelFileDB.closeFileDB();
         
         new File(fileNameBase + ".idx").delete();
         new File(fileNameBase + ".dat").delete();
      }
      catch (Exception e) 
      {
         e.printStackTrace();
         super.fail("Error - " + e);
      }
      
      logger.info("END testInsertAppendGetAndDeleteObject.");
   }
   
   
   /**
    * testInsertAndGetMultiple
    */
   public void testInsertAndGetMultiple()
   {
      logger.info("BEGIN testInsertAndGetMultiple.");
      
      String fileNameBase = BASE_PATH + "fileDBMultiple";
      
      try
      {
         new File(fileNameBase + ".idx").delete();
         new File(fileNameBase + ".dat").delete();
         
         final int allocationUnitSize = 10;
         final int blockSize = (allocationUnitSize + DefaultDataFile.BLOCK_HEADER_SIZE + DefaultDataFile.BLOCK_FOOTER_SIZE);
         
         LowLevelFileDB lowLevelFileDB = new LowLevelFileDB("LowLevelFileDB", fileNameBase, blockSize, 1, 1,  
               LowLevelFileDB.READ_WRITE_MODE);
         
         String key = "key";
         String[] keys = new String[10];
         byte[][] data = new byte[keys.length][];
         String dataString = "DATA123456";
         
         for(int i=0; i<keys.length; i++)
         {
            keys[i] = key + i;
            data[i] = dataString.getBytes();
         }
         
         lowLevelFileDB.insertMultipleItems(keys, data);
         
         byte[][] readData = lowLevelFileDB.getMultipleItems(keys);
         
         if( readData == null ) super.fail("Data for items not found!");
            
         for(int i=0; i<keys.length; i++)
         {
            if( readData[i] == null ) super.fail("Data for item with key '" + key + i + "' wasn't found!");
            if( !dataString.equals(new String(readData[i])) ) super.fail("Data for item with key '" + key + i + "' was invalid (" + new String(readData[i]) + ")!");
         }
                  
         lowLevelFileDB.closeFileDB();
         
         
         // Reopen (read only) and check again
         lowLevelFileDB = new LowLevelFileDB("LowLevelFileDB", fileNameBase, blockSize, 1, 1,  
               LowLevelFileDB.READ_ONLY_MODE);
         
         readData = lowLevelFileDB.getMultipleItems(keys);
         
         if( readData == null ) super.fail("Data for items not found!");
            
         for(int i=0; i<keys.length; i++)
         {
            if( readData[i] == null ) super.fail("Data for item with key '" + key + i + "' wasn't found!");
            if( !dataString.equals(new String(readData[i])) ) super.fail("Data for item with key '" + key + i + "' was invalid (" + new String(readData[i]) + ")!");
         }
                  
         lowLevelFileDB.closeFileDB();
         
         new File(fileNameBase + ".idx").delete();
         new File(fileNameBase + ".dat").delete();
      }
      catch (Exception e) 
      {
         e.printStackTrace();
         super.fail("Error - " + e);
      }
      
      logger.info("END testInsertAndGetMultiple.");
   }
   
   
   /**
    * testBlankSpace
    */
   public void testBlankSpace()
   {
      logger.info("BEGIN testBlankSpace.");
      
      testBlankSpaceInternal(5);
      testBlankSpaceInternal(10);
      testBlankSpaceInternal(15);
      testBlankSpaceInternal(20);
      
      logger.info("END testBlankSpace.");
   }
   /**
    */
   private void testBlankSpaceInternal(final int allocationUnitSize)
   {
      String fileNameBase = BASE_PATH + "fileDBBlank";
      
      try
      {
         new File(fileNameBase + ".idx").delete();
         new File(fileNameBase + ".dat").delete();
         
         final int n = 10;
         final int blockSize = (allocationUnitSize + DefaultDataFile.BLOCK_HEADER_SIZE + DefaultDataFile.BLOCK_FOOTER_SIZE);
         
         LowLevelFileDB lowLevelFileDB = new LowLevelFileDB("LowLevelFileDB", fileNameBase, blockSize, 1, 1,  
               LowLevelFileDB.READ_WRITE_MODE);
         
         String key = "key";
         String dataString = "DATA123456";
         byte[] data = dataString.getBytes(); 
                  
         lowLevelFileDB.insertBlankItem(key + "0", 0); // Test zero size blank space
         
         lowLevelFileDB.insertBlankItem(key, data.length);
         
         for(int i=1; i<n; i++)
         {
            lowLevelFileDB.appendItem(key, data.length);
         }
         
         for(int i=0; i<10; i++)
         {
            lowLevelFileDB.updatePartialItem(key, data, i*data.length);
         }
         
         if( !dataString.equals(new String(lowLevelFileDB.getPartialItem(key, 0, data.length))) ) super.fail("Item 1 invalid!");
         if( !dataString.equals(new String(lowLevelFileDB.getPartialItem(key, (n-1)*data.length, data.length))) ) super.fail("Item 10 invalid!");
         
         lowLevelFileDB.closeFileDB();
         
         // Reopen and check again
         lowLevelFileDB = new LowLevelFileDB("LowLevelFileDB", fileNameBase, blockSize, 1, 1,  
               LowLevelFileDB.READ_ONLY_MODE);
         
         if( !dataString.equals(new String(lowLevelFileDB.getPartialItem(key, 0, data.length))) ) super.fail("Item 1 invalid!");
         if( !dataString.equals(new String(lowLevelFileDB.getPartialItem(key, (n-1)*data.length, data.length))) ) super.fail("Item 10 invalid!");
         
         lowLevelFileDB.closeFileDB();
         
         new File(fileNameBase + ".idx").delete();
         new File(fileNameBase + ".dat").delete();
      }
      catch (Exception e) 
      {
         e.printStackTrace();
         super.fail("Error - " + e);
      }
   }
   
   
   /**
    * testDeletePartial
    */
   public void testDeletePartial()
   {
      logger.info("BEGIN testDeletePartial.");
      
      testDeletePartialInternal(5);
      testDeletePartialInternal(10);
      testDeletePartialInternal(15);
      testDeletePartialInternal(20);
      
      logger.info("END testDeletePartial.");
   }
   /**
    */
   private void testDeletePartialInternal(final int allocationUnitSize)
   {
      String fileNameBase = BASE_PATH + "fileDBPartial";
      
      try
      {
         new File(fileNameBase + ".idx").delete();
         new File(fileNameBase + ".dat").delete();
         
         final int n = 10;
         final int blockSize = (allocationUnitSize + DefaultDataFile.BLOCK_HEADER_SIZE + DefaultDataFile.BLOCK_FOOTER_SIZE);
         
         LowLevelFileDB lowLevelFileDB = new LowLevelFileDB("LowLevelFileDB", fileNameBase, blockSize, 1, 1,  
               LowLevelFileDB.READ_WRITE_MODE);
         
         String key = "key";
         String dataString = "DATA123456";
         byte[] data = dataString.getBytes();
         
         for(int i=0; i<n; i++)
         {
            lowLevelFileDB.appendItem(key, data);
         }
         
         // Delete last
         lowLevelFileDB.deletePartialItem(key, data.length);
         
         if( lowLevelFileDB.getItemSize(key) != ((n-1) * data.length) ) super.fail("Invalid item data size(" + lowLevelFileDB.getItemSize(key) + ")!");
         if( !dataString.equals(new String(lowLevelFileDB.getPartialItem(key, ((n-2)*data.length), data.length))) ) super.fail("Last item is invalid/not found!");

         // Reopen to check again
         lowLevelFileDB = new LowLevelFileDB("LowLevelFileDB", fileNameBase, blockSize, 1, 1,  
               LowLevelFileDB.READ_WRITE_MODE);

         if( lowLevelFileDB.getItemSize(key) != ((n-1) * data.length) ) super.fail("Invalid item data size(" + lowLevelFileDB.getItemSize(key) + ")!");
         if( !dataString.equals(new String(lowLevelFileDB.getPartialItem(key, ((n-2)*data.length), data.length))) ) super.fail("Last item is invalid/not found!");
         
         if( n > 2 )
         {
            // Delete (n-1)/2 last
            int itemsToRemove = ((n-1)/2); 
            
            for(int i=0; i<itemsToRemove; i++)
            {
               lowLevelFileDB.deletePartialItem(key, data.length);
            }
            
            lowLevelFileDB.closeFileDB();
            
            
            // Reopen to check that all is ok
            lowLevelFileDB = new LowLevelFileDB("LowLevelFileDB", fileNameBase, blockSize, 1, 1,  
                  LowLevelFileDB.READ_WRITE_MODE);

            if( lowLevelFileDB.getItemSize(key) != ((n-1-itemsToRemove) * data.length) ) super.fail("Invalid item data size(" + lowLevelFileDB.getItemSize(key) + ")!");
            if( !dataString.equals(new String(lowLevelFileDB.getPartialItem(key, ((n-2-itemsToRemove)*data.length), data.length))) ) super.fail("Last item is invalid/not found!");
         }

         lowLevelFileDB.deleteItem(key);
         lowLevelFileDB.closeFileDB();
         
         new File(fileNameBase + ".idx").delete();
         new File(fileNameBase + ".dat").delete();
      }
      catch (Exception e) 
      {
         e.printStackTrace();
         super.fail("Error - " + e);
      }
   }
   
   
   /**
    * testCorruptData
    */
   public void testCorruptData()
   {
      logger.info("BEGIN testCorruptData.");
      
      String fileNameBase = BASE_PATH + "fileDBCorrupt/fileDBCorrupt";
      new File(fileNameBase).mkdirs();
      
      try
      {
         new File(fileNameBase + ".idx").delete();
         new File(fileNameBase + ".dat").delete();
         
         final int allocationUnitSize = 10;
         final int blockSize = (allocationUnitSize + DefaultDataFile.BLOCK_HEADER_SIZE + DefaultDataFile.BLOCK_FOOTER_SIZE);
         
         LowLevelFileDB lowLevelFileDB = new LowLevelFileDB("LowLevelFileDB", fileNameBase, blockSize, 2, 2,  
               LowLevelFileDB.READ_WRITE_MODE);
         
         String dataString = "DATA123456";
         byte[] data = dataString.getBytes();
         lowLevelFileDB.insertItem("key1", data);
         lowLevelFileDB.insertItem("key2", data);
         lowLevelFileDB.insertItem("key3", data);
         
         lowLevelFileDB.closeFileDB();
         
         
         byte[] corruptHeader = new byte[DefaultDataFile.BLOCK_HEADER_SIZE];
         
         // Destroy header of first data block
         RandomAccessFile randomAccessFile = new RandomAccessFile(fileNameBase + ".dat", "rw");
         randomAccessFile.seek(DefaultDataFile.DATA_FILE_HEADER_SIZE);
         randomAccessFile.write(corruptHeader);
         randomAccessFile.close();
         
         // Destroy header of second index block
         randomAccessFile = new RandomAccessFile(fileNameBase + ".idx", "rw");
         randomAccessFile.seek(DefaultDataFile.DATA_FILE_HEADER_SIZE + lowLevelFileDB.getIndexFileBlockSize());
         randomAccessFile.write(corruptHeader);
         randomAccessFile.close();
         
         
         // Reopen
         lowLevelFileDB = new LowLevelFileDB("LowLevelFileDB", fileNameBase, 20, 2, 2,  
               LowLevelFileDB.READ_WRITE_MODE);
         
         byte[] readData = lowLevelFileDB.getItem("key1");
         if( readData != null ) super.fail("Item with key 'key1' shouldn't exist!");
         readData = lowLevelFileDB.getItem("key2");
         if( readData != null ) super.fail("Item with key 'key2' shouldn't exist!");
         readData = lowLevelFileDB.getItem("key3");
         if( readData == null ) super.fail("Item with key 'key3' wasn't found!");
         if( !dataString.equals(new String(readData)) ) super.fail("Data for item with key 'key3' was invalid!");
                  
         
         lowLevelFileDB.closeFileDB();
         
         FileDeletor.delete("fileDBCorrupt");
         new File(fileNameBase + ".idx").delete();
         new File(fileNameBase + ".dat").delete();
      }
      catch (Exception e) 
      {
         e.printStackTrace();
         super.fail("Error - " + e);
      }
      
      logger.info("END testCorruptData.");
   }
   
   
   /**
    * testReadWriteConcurrently
    */
   public void testReadWriteConcurrently()
   {
      logger.info("BEGIN testReadWriteConcurrently.");
      
      int nConsumerThreads = 10;
      
      String fileNameBase = BASE_PATH + "fileDBConcurrently";
      
      new File(fileNameBase + ".idx").delete();
      new File(fileNameBase + ".dat").delete();
      
      try
      {
         LowLevelFileDB lowLevelFileDB = new LowLevelFileDB("LowLevelFileDB", fileNameBase, 1024, 1, 1,  
               LowLevelFileDB.READ_WRITE_MODE);
         
         lowLevelFileDB.insertItem("initialData", "test");
         
         lowLevelFileDB.closeFileDB();
      }
      catch (Exception e) 
      {
         e.printStackTrace();
         super.fail("Error adding initial data!");
      }
      
      ProducerThread producerThread = new ProducerThread(fileNameBase);
      producerThread.setDaemon(true);
      ConsumerThread consumerThreads[] = new ConsumerThread[nConsumerThreads];
            
      int counter = 0;
      resetFailCount();
         
      for(int i=0; i<nConsumerThreads; i++)
      {
         consumerThreads[i] = new ConsumerThread(i, fileNameBase);
         consumerThreads[i].setDaemon(true);
         counter++;
      }
      
      producerThread.start();
      
      for(int i=0; i<nConsumerThreads; i++)
      {
         consumerThreads[i].start();
      }
      
      long beginWait = System.currentTimeMillis();
      long waitTime;
      
      waitTime = 10000 - (System.currentTimeMillis() - beginWait);
      if( waitTime > 0 )
      {
         try{
         producerThread.join(waitTime);
         }catch (Exception e){}
      }
      producerThread.interrupt();
      
      for(int i=0; i<nConsumerThreads; i++)
      {
         waitTime = 10000 - (System.currentTimeMillis() - beginWait);
         if( waitTime > 0 )
         {
            try{
            consumerThreads[i].join(waitTime);
            }catch (Exception e){}
         }
         consumerThreads[i].interrupt();
      }
       
      if( getFailCount() > 0 )
      {
         super.fail("Error in testReadWriteConcurrently - failCount: " + getFailCount() + ".");
      }
      
      new File(fileNameBase + ".idx").delete();
      new File(fileNameBase + ".dat").delete();
      
      logger.info("END testReadWriteConcurrently.");
   }
   
   
   /* ### INTERNALS ### */
   

   
   /**
    */
   private static class TestObject implements Serializable
   {
      static final long serialVersionUID = 6763187874742941642L;

      private String value;
      
      private NestedTestObject nestedTestObject;
      
      public TestObject(String value)
      {
         this.value = value;
         this.nestedTestObject = new NestedTestObject(value);
      }
      
      public String toString()
      {
         return "TestObject(" + this.value + ")";
      }
      
      public boolean equals(Object o)
      {
         return ((TestObject)o).value.equals(this.value);
      }
   }
   
   /**
    */
   private static class NestedTestObject implements Serializable
   {
      static final long serialVersionUID = -2904221434133691779L;
      
      private String value;
      
      public NestedTestObject(String value)
      {
         this.value = value;
      }
      
      public String toString()
      {
         return "NestedTestObject(" + this.value + ")";
      }
      
      public boolean equals(Object o)
      {
         return ((NestedTestObject)o).value.equals(this.value);
      }
   }
   
   
   /**
    */
   private static class ProducerThread extends Thread
   {
      public static final int N_ITERATIONS = 200;
      
      private String fileNameBase;
      
      public ProducerThread(String fileNameBase)
      {
         this.fileNameBase = fileNameBase;
      }
      
      public void run()
      {
         try
         {
            LowLevelFileDB lowLevelFileDB = new LowLevelFileDB("LowLevelFileDB", fileNameBase, 1024, 1, 1, LowLevelFileDB.READ_WRITE_MODE);
            
            String key;
            for(int i=0; i<N_ITERATIONS; i++)
            {
               byte[] data = new byte[1024];
               
               key = "item" + i;
               lowLevelFileDB.insertItem(key, data);
               if( (i % 4) == 0 ) lowLevelFileDB.updatePartialItem(key, new byte[10], 0);
               if( (i % 4) == 1 ) lowLevelFileDB.appendItem(key, new byte[1024]);
            }
            
            lowLevelFileDB.closeFileDB();
         }
         catch(Exception e)
         {
            e.printStackTrace();            
            incrementFailCount();
         }
      }
   }
   
   /**
    */
   private static class ConsumerThread extends Thread
   {
      public static final int N_ITERATIONS = 200;
      
      //private int threadId;
      
      private String fileNameBase;
      
      public ConsumerThread(int threadId, String fileNameBase)
      {
         //this.threadId = threadId;
         this.fileNameBase = fileNameBase;
      }
      
      public void run()
      {
         try
         {
            LowLevelFileDB lowLevelFileDB = null;
            
            for(int i=0; i<N_ITERATIONS; i++)
            {
               if( (i % 10) == 0 )
               {
                  lowLevelFileDB = new LowLevelFileDB("LowLevelFileDB", fileNameBase, LowLevelFileDB.READ_ONLY_MODE);
               }
               
               if( lowLevelFileDB.getItem("item" + i) == null )
               {
                  Thread.yield();
               }
            }
            
            lowLevelFileDB.closeFileDB();
         }
         catch(Exception e)
         {
            e.printStackTrace();           
            incrementFailCount();
         }
      }
   }
}
