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

/**
 * Classes that implement BlockAllocator are responsible for keeping track on 
 * which blocks are allocated for DataFile/ BlockFile objects.
 * 
 * @see BlockFile
 * @see DataFile
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1
 */
public interface BlockAllocator
{
	/**
	 * Get the indices of all the blocks that are allocated (occipied).
	 * 
	 * @return an array of integer indices.
	 */
	public int[] getAllocatedBlocks();
	
	/**
	 * Sets the size of this BlockAllocator, i.e. the maximum number of blocks 
	 * that can be allocated.
	 * 
	 * @param size the new size of this BlockAllocator.
	 */
	public void setSize(int size);
	
	/**
	 * Gets the total number of blocks in this BlockAllocator (allocated and deallocated).
	 * 
	 * @return the total number of blocks in this BlockAllocator (allocated and deallocated).
	 */
	public int getNumberOfBlocks();
	
	/**
	 * Gets the number of allocated blocks in this BlockAllocator.
	 * 
	 * @return the number of allocated blocks in this BlockAllocator.
	 */
	public int getNumberOfAllocatedBlocks();
	
	/**
	 * Checks if the specified blocknumber is allocated.
	 * 
	 * @param blockNumber a block number (index).
	 * 
	 * @return <code>true</code> if the specified block is allocated, otherwise <code>false</code>.
	 */
	public boolean isAllocated(int blockNumber);
	
	/**
	 * Gets the space currently used by this BlockAllocator, i.e. the number of 
	 * blocks between (inclusive) block 0 and the highest allocated block number. 
	 * The formal defenition of the return value of this method is <code>highest allocated block number + 1</code>.
	 * 
	 * @return the number of blocks in use.
	 */
	public int getSpaceInUse();
	
	/**
	 * Allocates the first available block. If there are no blocks available 
	 * this method returns -1.
	 * 
	 * @return the index of the allocated block or -1 if there were no blocks available.
	 */
	public int allocateBlock();
	
	/**
	 * Marks the specified block as allocated.
	 * 
	 * @param blockNumber the block to mark as allocated.
	 */
	public void allocateBlock(int blockNumber);
	
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
	public int[] allocateBlocks(int numberOfBlocks);
	
	/**
	 * Marks the specified block as deallocated (vacant).
	 * 
	 * @param blockNumber the block to mark as deallocated.
	 */
	public void deallocateBlock(int blockNumber);
	
	/**
	 * Marks several blocks as deallocated (vacant).
	 * 
	 * @param blockIndices the array containing the indices of the blocks to mark as deallocated.
	 */
	public void deallocateBlocks(int[] blockIndices);
	
	/**
	 * Marks several blocks as deallocated (vacant).
	 * 
	 * @param blockIndices the array containing the indices of the blocks to mark as deallocated.
	 * @param blockIndicesOffset the offset in the <code>blockIndices</code> array.
	 * @param numberOfBlocks the number of blocks to mark as deallocated.
	 */
	public void deallocateBlocks(int[] blockIndices, int blockIndicesOffset, int numberOfBlocks);
	
	/**
	 * Deallocates all blocks in this BlockAllocator.
	 */
	public void deallocateAllBlocks();
}
