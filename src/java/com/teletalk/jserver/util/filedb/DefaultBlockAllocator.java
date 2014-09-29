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

import java.util.Arrays;

/**
 * This class manages block allocations for DataFile/ BlockFile objects.
 * 
 * @see DefaultDataFile
 * @see BlockFile
 * @see DataFile
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1
 */
public class DefaultBlockAllocator implements BlockAllocator
{
	private boolean[] occupiedBlocksArray;
			
	private int numberOfOccupiedBlocks;
	private int firstFreeBlockSearchIndex;
	
	private int initialBlocks;

	/**
	 * Creates a new DefaultBlockAllocator.
	 * 
	 * @param initialBlockCapacity the initial number of blocks this DefaultBlockAllocator should make room for.
	 * @param occupiedIndices an integer array containing the indices of the blocks that are intially allocated (occupied).
	 */
	public DefaultBlockAllocator(int initialBlockCapacity, int[] occupiedIndices) 
	{
		this.numberOfOccupiedBlocks = 0;
		this.firstFreeBlockSearchIndex = 0;
		
		this.initialBlocks = initialBlockCapacity;
		this.occupiedBlocksArray = new boolean[ Math.max(this.initialBlocks, occupiedIndices.length) ];
		
		//Make sure all blocks are marked as vacant
		Arrays.fill(occupiedBlocksArray, false);
				
		final int occupiedIndicesSize = occupiedIndices.length;
		int index;
		
		for(int i=0; i<occupiedIndicesSize; i++)
		{
			index = occupiedIndices[i];
			
			this.occupiedBlocksArray[index] = true;
			this.numberOfOccupiedBlocks++;
		}
	}
		
	/**
	 * Get the indices of all the blocks that are allocated (occipied).
	 * 
	 * @return an array of integer indices.
	 */
	public final int[] getAllocatedBlocks()
	{
		final int[] occupiedIncices = new int[numberOfOccupiedBlocks];
		int occupiedIncicesCounter = 0;
		
		for(int i=0; i<occupiedBlocksArray.length; i++)
		{
			if(occupiedBlocksArray[i])
			{
				occupiedIncices[occupiedIncicesCounter++] = i;
			}
		}
		
		return occupiedIncices;
	}
	
	/**
	 * Sets the size of this BlockAllocator, i.e. the maximum number of blocks 
	 * that can be allocated.
	 * 
	 * @param size the new size of this BlockAllocator.
	 */
	public final void setSize(final int size)
	{
		if(size != occupiedBlocksArray.length)
		{
			boolean[] newBooleanArray = new boolean[size];
				
			if(size > occupiedBlocksArray.length)
			{
				System.arraycopy(occupiedBlocksArray, 0, newBooleanArray, 0, occupiedBlocksArray.length);
				Arrays.fill(newBooleanArray, occupiedBlocksArray.length, size, false);
			}
			else
			{
				System.arraycopy(occupiedBlocksArray, 0, newBooleanArray, 0, size);
			}
				
			occupiedBlocksArray = newBooleanArray;
		}
	}
	
	/**
	 * Gets the total number of blocks in this DefaultBlockAllocator (allocated and deallocated).
	 * 
	 * @return the total number of blocks in this DefaultBlockAllocator (allocated and deallocated).
	 */
	public final int getNumberOfBlocks()
	{
		return occupiedBlocksArray.length;
	}
	
	/**
	 * Gets the number of allocated blocks in this DefaultBlockAllocator.
	 * 
	 * @return the number of allocated blocks in this DefaultBlockAllocator.
	 */
	public final int getNumberOfAllocatedBlocks()
	{
		return numberOfOccupiedBlocks;
	}
	
	/**
	 * Checks if the specified blocknumber is allocated.
	 * 
	 * @param blockNumber a block number (index).
	 * 
	 * @return <code>true</code> if the specified block is allocated, otherwise <code>false</code>.
	 */
	public boolean isAllocated(int blockNumber)
	{
		return this.occupiedBlocksArray[blockNumber];
	}
	
	/**
	 * Gets the space currently used by this BlockAllocator, i.e. the number of 
	 * blocks between (inclusive) block 0 and the highest allocated block number. 
	 * The formal defenition of the return value of this method is <code>highest allocated block number + 1</code>.
	 * 
	 * @return the number of blocks in use.
	 */
	public int getSpaceInUse()
	{
		int highestVacant = -1;
		
		for(int i=(this.occupiedBlocksArray.length-1);  i >=0; i--)
		{
			if(this.occupiedBlocksArray[i]) //If allocated
				break;
			else
				highestVacant = i;
		}
		
		if(highestVacant >= 0)
		{
			return highestVacant;
		}	
		else return occupiedBlocksArray.length;
	}
	
	/**
	 * Finds the first free block.
	 */
	private final int findFirstFreeBlock()
	{
		int firstFreeBlock = -1;
				
		if(numberOfOccupiedBlocks < occupiedBlocksArray.length)
		{
			for(int i=0; i<occupiedBlocksArray.length; i++)
			{
				if(firstFreeBlockSearchIndex >= occupiedBlocksArray.length)
					firstFreeBlockSearchIndex = 0;
				
				if(!occupiedBlocksArray[firstFreeBlockSearchIndex]) //If vacant...
				{
					firstFreeBlock = firstFreeBlockSearchIndex++;
					break;
				}
				else
					firstFreeBlockSearchIndex++;
			}
		}
		
		return firstFreeBlock;
	}
	
	/**
	 * Allocates the first available block. If there are no blocks available 
	 * this method returns -1.
	 * 
	 * @return the index of the allocated block or -1 if there were no blocks available.
	 */
	public final int allocateBlock() 
	{
		int firstFreeBlock = this.findFirstFreeBlock();
		
		if(firstFreeBlock >= 0) this.allocateBlock(firstFreeBlock);
		return firstFreeBlock;
	}
	
	/**
	 * Marks the specified block as allocated.
	 * 
	 * @param blockNumber the block to mark as allocated.
	 */
	public final void allocateBlock(final int blockNumber)
	{
		if(!occupiedBlocksArray[blockNumber]) //If vacant
		{
			occupiedBlocksArray[blockNumber] = true;
			numberOfOccupiedBlocks++;
		}
	}
	
	/**
	 * Allocates the first n available blocks (where <code>n=numberOfBlocks</code>). 
	 * The returned array will contain the indices of the allocated blocks and the length
	 * of it will be equal to parameter <code>numberOfBlocks</code> or the number of 
	 * blocks that could be allocated.
	 * 
	 * @param numberOfBlocks the number of blocks to allocate.
	 * 
	 * @return an <code>int</code> array containing the indices of the allocated blocks. The length 
	 * of the array will be equal to parameter <code>numberOfBlocks</code> or the number of 
	 * blocks that could be allocated.
	 */
	public final int[] allocateBlocks(final int numberOfBlocks)
	{
		int[] blocks = new int[numberOfBlocks];
		int numberOfFreeBlocksFound = 0;
		int firstFreeBlock;

		// Find free blocks
		while(numberOfFreeBlocksFound < numberOfBlocks)
		{
			firstFreeBlock = this.findFirstFreeBlock();
			
			if(firstFreeBlock >= 0)
			{
				this.allocateBlock(firstFreeBlock); //Allocate
				blocks[numberOfFreeBlocksFound++] = firstFreeBlock;
			}
			else break;
		}

		// If the required number of blocks could not be allocated...
		if(numberOfFreeBlocksFound < numberOfBlocks)
		{
			int[] _blocks = new int[numberOfFreeBlocksFound];
			System.arraycopy(blocks, 0, _blocks, 0, _blocks.length);
			blocks = _blocks;
		}
		
		return blocks;
	}

	/**
	 * Marks the specified block as deallocated (vacant).
	 * 
	 * @param blockNumber the block to mark as deallocated.
	 */
	public final void deallocateBlock(final int blockNumber)
	{
		if(occupiedBlocksArray[blockNumber]) //If allocated
		{
			occupiedBlocksArray[blockNumber] = false;
			numberOfOccupiedBlocks--;
		}
	}
	
	/**
	 * Marks several blocks as deallocated (vacant).
	 * 
	 * @param blockIndices the array containing the indices of the blocks to mark as deallocated.
	 */
	public final void deallocateBlocks(final int[] blockIndices)
	{
		deallocateBlocks(blockIndices, 0, blockIndices.length);
	}

	/**
	 * Marks several blocks as deallocated (vacant).
	 * 
	 * @param blockIndices the array containing the indices of the blocks to mark as deallocated.
	 * @param blockIndicesOffset the offset in the <code>blockIndices</code> array.
	 * @param numberOfBlocks the number of blocks to mark as deallocated.
	 */
	public final void deallocateBlocks(final int[] blockIndices, final int blockIndicesOffset, final int numberOfBlocks)
	{
		for(int i=0; i<numberOfBlocks; i++)
		{
			if(occupiedBlocksArray[blockIndices[i+blockIndicesOffset]]) //If allocated
			{
				occupiedBlocksArray[blockIndices[i+blockIndicesOffset]] = false;
				numberOfOccupiedBlocks--;
			}
		}
	}
	
	/**
	 * Deallocates all blocks in this BlockAllocator.
	 */
	public final void deallocateAllBlocks()
	{
		occupiedBlocksArray = new boolean[initialBlocks];
		
		//Make sure all blocks are marked as vacant
		for(int i=0; i<initialBlocks; i++) occupiedBlocksArray[i] = false;
		
		firstFreeBlockSearchIndex = 0;
		numberOfOccupiedBlocks = 0;
	}
}
