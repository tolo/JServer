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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ImageIcon;
import javax.swing.JCheckBox;

import com.teletalk.jadmin.gui.AdminMainPanel;

/**
 */
public class SwitchButton extends JCheckBox implements ActionListener
{
	/**
    * 
    */
   private static final long serialVersionUID = 1L;
   private String unselectedToolTipText;
	private String selectedToolTipText;
	
	/**
	 */
	public SwitchButton(String imagePrefix, String imageSuffix, String unselectedToolTipText, String selectedToolTipText)
	{
		super(new ImageIcon(Object.class.getResource(imagePrefix + imageSuffix)));
		
		if(this.getClass().getResource(imagePrefix + "_pressed" + imageSuffix) != null)
			setPressedIcon(new ImageIcon(this.getClass().getResource(imagePrefix + "_pressed" + imageSuffix)));
		if(this.getClass().getResource(imagePrefix + "_selected" + imageSuffix) != null)
			setSelectedIcon(new ImageIcon(this.getClass().getResource(imagePrefix + "_selected" + imageSuffix)));
		
		this.unselectedToolTipText = unselectedToolTipText;
		this.selectedToolTipText = selectedToolTipText;
		
		updateToolTipText();
		
		addActionListener(this);
	}
	
	/**
	 */
	public SwitchButton(String imagePrefix, String imageSuffix)
	{
		this(imagePrefix, imageSuffix, null, null);
	}

	/**
	 */
	public final void setUnselectedToolTipText(String text)
	{
		unselectedToolTipText = text;
		
		updateToolTipText();
	}
	
	/**
	 */
	public final void setSelectedToolTipText(String text)
	{
		selectedToolTipText = text;
		
		updateToolTipText();
	}
	
	/**
	 */
	public final void updateToolTipText()
	{
		if(isSelected() && (selectedToolTipText != null))
			setToolTipText(selectedToolTipText);
		else if(unselectedToolTipText != null)
			setToolTipText(unselectedToolTipText);
	}
	
	/**
	 */
	public void actionPerformed(ActionEvent event)
	{
		if(event.getSource() == this)
		{
			updateToolTipText();
			
			/*if(this.getClass().getResource("/sounds/button_push3.wav") != null)
				(Applet.newAudioClip(this.getClass().getResource("/sounds/button_push3.wav"))).play();*/
			AudioClip pushSound = ResourceHandler.getAudioClip("/sounds/button_push3.wav");
			if(pushSound != null && AdminMainPanel.soundsEnabled)
				pushSound.play();
		}
	}
}
