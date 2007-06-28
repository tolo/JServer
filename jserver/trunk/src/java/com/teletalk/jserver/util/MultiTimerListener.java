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
package com.teletalk.jserver.util;

/**
 * Listener interface for classes who wish to receive timer ticks from one or more timers.
 * 
 * @see Timer
 * 
 * @author Tobias L�fstrand
 * 
 * @since Beta 2
 */
public interface MultiTimerListener
{
	/**
	 * Method called when the associated Timer has "ticked".
	 */
	public void timerTick(Timer timer);
}