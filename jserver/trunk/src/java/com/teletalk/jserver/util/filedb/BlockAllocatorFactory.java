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
 * Factory class for BlockAllocatorFactory objects.
 * 
 * @see BlockAllocator
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1
 */
public abstract class BlockAllocatorFactory
{
	private static final BlockAllocatorFactory defaultBlockAllocatorFactory = new BlockAllocatorFactory()
		{
			public BlockAllocator createBlockAllocator(int initialBlockCapacity, int[] occupiedIndices)
			{
				return new DefaultBlockAllocator(initialBlockCapacity, occupiedIndices);
			}
		};
	
	/**
	 * Gets the default BlockAllocatorFactory (which creates DefaultBlockAllocator objects).
	 * 
	 * @return the default BlockAllocatorFactory.
	 */
	public static BlockAllocatorFactory getDefaultFactory()
	{
		return defaultBlockAllocatorFactory;
	}
	
	/**
	 * Creates a new BlockAllocator object.
	 * 
	 * @param initialBlockCapacity the initial number of blocks the created BlockAllocator should make room for.
	 * @param occupiedIndices an integer array containing the indices of the blocks that intially are to be marked as allocated (occupied).
	 * 
	 * @return a new BlockAllocator object.
	 */
	public abstract BlockAllocator createBlockAllocator(int initialBlockCapacity, int[] occupiedIndices);
}
