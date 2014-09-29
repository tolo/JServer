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
package com.teletalk.jadmin.proxy;

import java.util.HashMap;

/**
 * Listner interface for remote VectorProperty modifications.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.0
 */
public interface VectorPropertyProxyListener
{
	/**
	 * Called when one or more items are added.
	 */
	public void itemAdded(String[] keys, String[] descriptions);
	
	/**
	 * Called when one or more items are removed.
	 */
	public void itemRemoved(String[] keys, String[] descriptions);
	
	/**
	 * Called when one or more items are modified.
	 */
	public void itemModified(String[] keys, String[] descriptions);
	
	/**
	 * Called to notify that all items should be cleared.
	 */
	public void clearItems();
		
	/**
	 * Called when the the entire contents of the VectorPropertyProxy have been updated with data from the server.
	 */
	public void itemsUpdated(String[][] items);
	
	/**
	 * Called when the operations have been updated.
	 */
	public void operationsUpdated(HashMap operations);
}
