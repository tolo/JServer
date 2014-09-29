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
package com.teletalk.jadmin.gui.util;

import java.applet.AudioClip;

import javax.swing.Icon;
import javax.swing.JLabel;

import com.teletalk.jadmin.gui.AdminMainPanel;

/**
 * JLabel subclass that can be "turned on", that is display an alternate icon 
 * for a certain number of seconds.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class Indicator extends JLabel implements Runnable
{
	/**
    * 
    */
   private static final long serialVersionUID = 1L;
   private final Thread thread;
	private final AudioClip sound;
	private final long onTime;
		
	private boolean isOn = false;
	private long switchBackTime;
	
	/**
	 * Creates a new Indicator.
	 */
	public Indicator(Icon enabledIcon, Icon disabledIcon, AudioClip sound)
	{
		this(enabledIcon, disabledIcon, sound, 10*1000);
	}
	
	/**
	 * Creates a new Indicator.
	 */
	public Indicator(Icon enabledIcon, Icon disabledIcon, AudioClip sound, long onTime)
	{
		super(enabledIcon);
		setIcon(enabledIcon);
		setDisabledIcon(disabledIcon);
		this.sound = sound;
		setEnabled(false);
		
		this.onTime = onTime;
		
		thread = new Thread(this, "Indicatorthread");
		thread.setDaemon(true);
		thread.start();
	}
	
	/**
	 * Turns this indicator on, that is it begins to display the alternate icon.
	 */
	public synchronized void on()
	{
		this.isOn = true;
		this.switchBackTime = System.currentTimeMillis() + this.onTime;
		notify();
	}
	
	/**
	 * Thread method used wait and switch the icon back to the standard one, after 
	 * the indicator has been turned on.
	 */
	public void run()
	{
		long sleepTime;
		
		while(true)
		{
			try
			{
				synchronized(this)
				{
					while( !this.isOn ) wait();
				}
				this.isOn = false;
				
				setEnabled(true);
				if(AdminMainPanel.soundsEnabled) sound.play();
				
				while( (sleepTime = switchBackTime - System.currentTimeMillis()) > 0 )
				{
					Thread.sleep(sleepTime);
				}
				
				setEnabled(false);
			}
			catch (Exception e){}
		}
	}
}
