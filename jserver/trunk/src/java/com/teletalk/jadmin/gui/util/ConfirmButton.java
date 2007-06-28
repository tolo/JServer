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
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JPanel;

import com.teletalk.jadmin.gui.AdminMainPanel;

/**
 * The class implements a panel that contains two buttons, one standard button and one <code>AnimButtom</code>. When 
 * the standard button is pressed, the AnimButton is enabled. When the AnimButton is pressed, an event is sent to all registered 
 * <code>ConfirmButtonListener</code> objects.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class ConfirmButton extends JPanel implements ActionListener, Runnable
{
	/**
    * 
    */
   private static final long serialVersionUID = 1L;
   private final JCheckBox enableButton;
	private final AnimButton confirmationButton;
	
	private long enableTimeout = 0;
	private Thread timeoutThread;
	
	private ArrayList listeners;
	
	/**
	 * Creates a new ConfirmButton.
	 */
	public ConfirmButton(String enableImagesPrefix, String enableImagesSuffix, String confirmImagesPrefix, String confirmImagesSuffix, int nAnimIcons)
	{
		super();
		
		this.listeners = new ArrayList();
		
		setLayout(new GridLayout(1, 1, 1, 1));
		
		JPanel innerPanel = new JPanel()
			{
				/**
             * 
             */
            private static final long serialVersionUID = 1L;

            public Insets getInsets()
				{
					return new Insets(0, 0, 0, 0);
				}
			};
		innerPanel.setLayout(new BoxLayout(innerPanel, BoxLayout.X_AXIS));
		
		innerPanel.setBorder(BorderFactory.createEtchedBorder());
		
		enableButton = new JCheckBox( ResourceHandler.getImageIcon(enableImagesPrefix + "normal" + enableImagesSuffix) );		enableButton.setAlignmentY(CENTER_ALIGNMENT);
		
		enableButton.setPressedIcon( ResourceHandler.getImageIcon(enableImagesPrefix + "pressed" + enableImagesSuffix) );
		enableButton.setSelectedIcon( ResourceHandler.getImageIcon(enableImagesPrefix + "selected" + enableImagesSuffix) );		enableButton.setDisabledIcon( ResourceHandler.getImageIcon(enableImagesPrefix + "disabled" + enableImagesSuffix) );		enableButton.setRolloverIcon( ResourceHandler.getImageIcon(enableImagesPrefix + "rollover" + enableImagesSuffix) );
		enableButton.setRolloverSelectedIcon( ResourceHandler.getImageIcon(enableImagesPrefix + "rollover_selected" + enableImagesSuffix) );		
		enableButton.setMargin(new Insets(0, 0, 0, 0));
				
		ImageIcon animNormal = ResourceHandler.getImageIcon(confirmImagesPrefix + "normal" + confirmImagesSuffix);		ImageIcon animDisabled = ResourceHandler.getImageIcon(confirmImagesPrefix + "disabled" + confirmImagesSuffix);
		ImageIcon animPressed = ResourceHandler.getImageIcon(confirmImagesPrefix + "pressed" + confirmImagesSuffix);		ImageIcon animRollover = ResourceHandler.getImageIcon(confirmImagesPrefix + "rollover" + confirmImagesSuffix);
				
		confirmationButton = new AnimButton(	animNormal	, animDisabled, animPressed,																		animRollover, confirmImagesPrefix, confirmImagesSuffix,	nAnimIcons);		
		confirmationButton.setMargin(new Insets(0, 0, 0, 0));
		confirmationButton.setAlignmentY(CENTER_ALIGNMENT);
		
		innerPanel.add(Box.createHorizontalStrut(3));
		innerPanel.add(enableButton);
		innerPanel.add(confirmationButton);
		innerPanel.add(Box.createHorizontalStrut(3));
		
		add(innerPanel);
		
		enableButton.addActionListener(this);
		confirmationButton.addActionListener(this);
		
		this.setMaximumSize(this.getMinimumSize());
	}
	
	/**
	 * Gets the insets of this panel.
	 */
	public Insets getInsets()
	{
		return new Insets(0, 0, 0, 0);
	}
	
	/**
	 * Adds a ConfirmButtonListener.
	 */
	public void addConfirmButtonListener(final ConfirmButtonListener listener)
	{
		synchronized(listeners)
		{
			if(!listeners.contains(listener))
				listeners.add(listener);
		}
	}
	
	/**
	 * Removes the specified ConfirmButtonListener.
	 */
	public void removeConfirmButtonListener(final ConfirmButtonListener listener)
	{
		synchronized(listeners)
		{
			listeners.remove(listener);
		}
	}
	
	/**
	 * Action event handler method.
	 */
	public void actionPerformed(final ActionEvent event)
	{
		if(event.getSource() == enableButton)
		{
			if(enableButton.isSelected())
			{
				if(confirmationButton.enableButton())
				{
					if(enableTimeout > 0)
					{
						timeoutThread = new Thread(this);
						timeoutThread.setDaemon(true);
						timeoutThread.start();
					}
				}
				else enableButton.setSelected(false);
			}
			else
			{
				if(!confirmationButton.disableButton())
					enableButton.setSelected(true);
			}
		}
		else if(event.getSource() == confirmationButton)
		{
			AudioClip pushSound = ResourceHandler.getAudioClip("/sounds/button_push2.wav");
			if(pushSound != null && AdminMainPanel.soundsEnabled)
				pushSound.play();
			
			ConfirmButtonListener[] listenerArray;
			
			synchronized(listeners)
			{
				listenerArray = (ConfirmButtonListener[])listeners.toArray(new ConfirmButtonListener[0]);
			}
			
			for(int i=0; i<listenerArray.length; i++)
			{
				try
				{
					listenerArray[i].confimButtonPressed(this);
				}
				catch(Exception e){}
			}
		}
	}
	
	/**
	 * Marks this ConfirmButton as enabled or disabled.
	 */
	public void setEnabled(boolean enabled)
	{
		enableButton.setEnabled(enabled);
	}
	
	/**
	 * Gets the enable button.
	 */
	public AbstractButton getEnableButton()
	{
		return enableButton;	
	}
	
	/**
	 * Gets the <code>AnimButton</code>.
	 */
	public AnimButton getConfirmationButton()
	{
		return confirmationButton;
	}
	
	/**
	 * Sets the timeout in milliseconds to be used when waiting for the anim button to change state.
	 */
	public void setEnableTimeout(long enableTimeout)
	{
		this.enableTimeout = enableTimeout;
	}
	
	/**
	 * Thread method used to wait for the anim button to change state.
	 */
	public void run()
	{
		try
		{
			confirmationButton.waitForStateChange(enableTimeout);
						
			if(confirmationButton.isEnabled()) confirmationButton.disableButton();
			if(enableButton.isSelected()) enableButton.setSelected(false);
		}
		catch(InterruptedException e){}
	}
}
