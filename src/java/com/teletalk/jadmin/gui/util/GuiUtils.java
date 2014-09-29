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
import java.awt.Component;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.border.Border;

/**
 * Utility class containing useful GUI methods.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class GuiUtils
{
	/**
	 * Creates a bevelled border.
	 */
	public static Border createCompoundBevelBorder()
	{
		Border raisedbevel = BorderFactory.createRaisedBevelBorder();
		Border loweredbevel = BorderFactory.createLoweredBevelBorder();
		
		return BorderFactory.createCompoundBorder(raisedbevel, loweredbevel);
	}
	
	/**
	 * Creates a bevelled border with a title.
	 */
	public static Border createTitleCompoundBevelBorder(String title)
	{
		Border raisedbevel = BorderFactory.createRaisedBevelBorder();
		Border loweredbevel = BorderFactory.createLoweredBevelBorder();
		
		return BorderFactory.createTitledBorder(BorderFactory.createCompoundBorder(raisedbevel, loweredbevel), title);
	}
	
	/**
	 * Displays an error message in a dialog box.
	 */
	public static void showErrorMessage(final String errMsg)
	{
		showErrorMessage(null, errMsg, null);
	}
	
	/**
	 * Displays an error message in a dialog box with a title.
	 */
	public static void showErrorMessage(final String errMsg, final String title)
	{
		showErrorMessage(null, errMsg, title);
	}
	
	/**
	 * Displays an error message in a dialog box.
	 */
	public static void showErrorMessage(final Component dialogParent, final String errMsg)
	{
		showErrorMessage(dialogParent, errMsg, null);
	}
	
	/**
	 * Displays an error message in a dialog box with a title.
	 */
	public static void showErrorMessage(final Component dialogParent, String errMsg, String title)
	{
		if(title == null) title = "Error";
		
		char[] newStr = errMsg.toCharArray();
		int charCount = 0;
			
		//Remove linebreaks from the error message
		for(int i=0; i<newStr.length; i++)
		{
			if(newStr[i] == '\r' || newStr[i] == '\n' || newStr[i] == '\t' || newStr[i] == '\f')
			{
				// Skip all following linebreaks...
				for(; ((i+1)<newStr.length) && (newStr[i+1] == '\r' || newStr[i+1] == '\n' || newStr[i+1] == '\t' || newStr[i+1] == '\f'); i++);
					
				// Replace with space, but only if not first character
				if( (charCount > 0) )
				{
					newStr[charCount] = ' ';
					charCount++;
				}
			}
			else
			{
				newStr[charCount] = newStr[i];
				charCount++;
			}
		}
		errMsg = new String(newStr, 0, charCount);
		
		StringTokenizer t = new StringTokenizer(errMsg);
		String result = "";
		String nextToken;
		int lineLength = 0;

		for(int i=0; t.hasMoreTokens(); i++)
		{
			nextToken = t.nextToken();
			lineLength += nextToken.length();
						
			if( (i == 12) || (lineLength > 80) )
			{
				result += "\n";
				i = 0;
				lineLength = 0;
			}
			result += nextToken + " ";
		}
		
		JOptionPane pane = new JOptionPane(result, JOptionPane.ERROR_MESSAGE);
		pane.setIcon(ResourceHandler.getImageIcon("/images/tiger.gif"));
		
		JDialog dialog = pane.createDialog(dialogParent, title);
		dialog.pack();
		
		AudioClip sound = ResourceHandler.getAudioClip("/sounds/warning.wav");
		if( sound != null ) sound.play();
		
		dialog.setVisible(true);
	}
}
