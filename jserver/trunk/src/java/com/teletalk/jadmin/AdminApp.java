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

import java.applet.AudioClip;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.Timer;
import java.util.TimerTask;

import javax.swing.JFrame;
import javax.swing.JPanel;

import com.teletalk.jadmin.gui.util.ResourceHandler;

/**
 * Class used to start JAdmin as a GUI application.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class AdminApp extends JFrame implements AdminTopGui, WindowListener
{
   private static final long serialVersionUID = 1L; // Dummy serial uid
   
	public static int WIDTH = 700;
	public static int HEIGHT = 550;

	private JAdmin admin;
   
   static Timer miscSoundTimer = new Timer();
   
   public static class MiscSoundTask extends TimerTask
   {
      public void run()
      {
         AudioClip sound = ResourceHandler.getAudioClip("/sounds/misc.wav");
         if( sound != null ) sound.play();
         miscSoundTimer.schedule(new MiscSoundTask(), (long)(Math.random() * 30 * 60 * 1000));
      }
   }

	/**
	 * Creates a new AdminApp.
	 */
	public AdminApp() throws Exception
	{
		super(JAdmin.versionString);
		
		java.net.URL iu = this.getClass().getResource("/images/JAdminIcon.gif");
		
		if(iu != null)
			this.setIconImage(getToolkit().getImage(iu));
		
		this.admin =new JAdmin(this);
		
		addWindowListener(this);
		
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(screenSize.width/2 - WIDTH/2, screenSize.height/2 - HEIGHT/2);
		setSize(WIDTH, HEIGHT);
		
		JPanel mainPanel = admin.getAdminMainPanel();
		
		getContentPane().setLayout(new BorderLayout());
		getContentPane().add("Center", mainPanel);
      
      miscSoundTimer.schedule(new MiscSoundTask(), (long)(Math.random() * 30 * 60 * 1000));
	}
	
	/**
	 * The main method of the JAdmin application.
	 * 
	 * @param args the application arguments.
	 */
	public static void main(String args[])
	{
		try
		{
			System.out.println("Initializing " + JAdmin.longVersionString + "...");	
			
			System.out.print("Creating main window...");
			AdminApp app = new AdminApp();
			System.out.println("done!");
			System.out.print("Displaying main window...");
			app.setVisible(true);
			System.out.println("done!");
		}
		catch(Exception e)
		{
			e.printStackTrace();	
		}
	}
	
	/**
	 * Gets the main frame of the GUI.
	 * 
	 * @return the main frame.
	 */
	public Frame getMainFrame()
	{
		return this;
	}
	
	/**
	 * Event handler method called when the window is closed.
	 */
	public void windowClosed(WindowEvent e)
	{
		dispose();
		admin.disconnect();
		System.exit(0);
	}

	/**
	 * Event handler method called when the window is closing.
	 */
	public void windowClosing(WindowEvent e)
	{
		dispose();
		admin.disconnect();
		System.exit(0);
	}

	public void windowActivated(WindowEvent e){}
	public void windowDeactivated(WindowEvent e){}
	public void windowDeiconified(WindowEvent e){}
	public void windowIconified(WindowEvent e){}
	public void windowOpened(WindowEvent e){}
}
