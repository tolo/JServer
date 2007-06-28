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

import java.io.IOException;

/**
 * Factory class for BlockFile objects.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.1
 */
public abstract class BlockFileFactory
{
	private static final BlockFileFactory defaultBlockFileFactory = new BlockFileFactory()
		{
			public BlockFile createBlockFile(DataIO dataIO, int blockSize, int blockDataOffset) throws IOException
			{
				return new DefaultBlockFile(dataIO, blockSize, blockDataOffset);
			}
		};
	
	/**
	 * Gets the default BlockFileFactory (which creates DefaultBlockFile objects).
	 * 
	 * @return the default BlockFileFactory.
	 */
	public static BlockFileFactory getDefaultFactory()
	{
		return defaultBlockFileFactory;
	}

	/**
	 * Creates a new BlockFile object.
	 * 
	 * @param dataIO the DataIO object to be used as data storage by the created BlockFile object.
	 * @param blockSize the size of the blocks.
	 * @param blockDataOffset the number of bytes before the first block in the created BlockFile object.
	 * 
	 * @return a new BlockFile object.
	 */
	public abstract BlockFile createBlockFile(DataIO dataIO, int blockSize, int blockDataOffset) throws IOException;
}
