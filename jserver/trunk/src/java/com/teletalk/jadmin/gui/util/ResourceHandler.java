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

import java.applet.Applet;
import java.applet.AudioClip;
import java.net.URL;
import java.util.HashMap;

import javax.swing.ImageIcon;

/**
 * This class is responsible for getting and caching resources such as ImageIcons.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class ResourceHandler
{
	public static HashMap resouces = new HashMap();
	
	/**
	 * Gets the ImageIcon resource with the specified name.
	 */
	public static ImageIcon getImageIcon(final String name)
	{
		ImageIcon icon = (ImageIcon)resouces.get(name);

		if(icon == null)
		{
			synchronized(resouces)
			{
				if(resouces.containsKey(name))
				{
					icon = (ImageIcon)resouces.get(name);
				}
				else
				{
					URL url = ((Class)ResourceHandler.class).getResource(name);
					
					if(url != null)
					{
						icon = new ImageIcon(url);
						resouces.put(name, icon);
					}
					else
						icon = null;
				}
			}
		}
		
		return icon;
	}
	
	/**
	 * Gets the AudioClip resource with the specified name.
	 */
	public static AudioClip getAudioClip(final String name)
	{
		AudioClip sound = (AudioClip)resouces.get(name);

		if(sound == null)
		{
			synchronized(resouces)
			{
				if(resouces.containsKey(name))
				{
					sound = (AudioClip)resouces.get(name);
				}
				else
				{
					URL url = ((Class)ResourceHandler.class).getResource(name);
					
					if(url != null)
					{
						sound = Applet.newAudioClip(((Class)ResourceHandler.class).getResource(name));
						resouces.put(name, sound);
					}
					else
						sound = null;
				}
			}
		}
		
		return sound;
	}
}
