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
package com.teletalk.jserver.queue; 

import com.teletalk.jserver.queue.QueueItemData;

/**
 * 
 * @author Tobias Löfstrand
 */
public class TestQueueItemData implements QueueItemData
{
   private static final long serialVersionUID = -4360483302489076539L;
   
   
   private final String s;
	
	public TestQueueItemData(String str)
	{
		s = str;
	}
		
	public String getDescription()
	{
		return s;	
	}
	
	public String toString()
	{
		return getDescription();	
	}
}
