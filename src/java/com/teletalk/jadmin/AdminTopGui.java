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
package com.teletalk.jadmin;

import java.awt.Frame;

/**
 * Interface representing the top element in the GUI.
 * 
 * @author Tobias L�fstrand
 * 
 * @since Alpha
 */
public interface AdminTopGui
{
	/**
	 * Gets the main frame of the GUI.
	 * 
	 * @return the main frame.
	 */
	public Frame getMainFrame();
}
