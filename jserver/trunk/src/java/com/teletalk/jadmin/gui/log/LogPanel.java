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
package com.teletalk.jadmin.gui.log;

import java.awt.Frame;
import java.awt.LayoutManager;

import javax.swing.JPanel;

import com.teletalk.jadmin.gui.AdminMainPanel;
import com.teletalk.jserver.log.LogFilter;

/**
 * Abstract base class for classes responsible for displaying log messages.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public abstract class LogPanel extends JPanel
{
	protected AdminMainPanel mainPanel;
	protected Frame mainFrame;
	
	private LogFilter filter;

	/**
	 * Creates a new LogPanel.
	 */	
	public LogPanel(AdminMainPanel mainPanel)
	{
		super();
		
		filter = new LogFilter();
		
		this.mainPanel = mainPanel;
		this.mainFrame = mainPanel.getMainFrame();
	}
	
	/**
	 * Creates a new LogPanel.
	 */	
	public LogPanel(LayoutManager layout, AdminMainPanel mainPanel)
	{
		super(layout);
		
		filter = new LogFilter();
		
		this.mainPanel = mainPanel;
		this.mainFrame = mainPanel.getMainFrame();
	}
	
	/**
	 * Destroys this LogPanel.
	 */
	public void destroy()
	{
	}
	
	/**
	 * Gets the main frame.
	 */
	protected Frame getMainFrame()
	{
		return mainFrame;	
	}
	
	/**
	 * Gets the main panel of the JAdmin GUI.
	 */
	public AdminMainPanel getMainPanel()
	{
		return mainPanel;	
	}
	
	/**
	 * Callled when the log message filter associated with this LogPanel has changed.
	 */
	public void logFilterChanged(LogFilter filter)
	{
	}
	
	/**
	 * Called to process an event received from the server.
	 */
   public void processEvent(Object event)
	{
	}
	
	/**
	 * Gets the log message filter associated with this LogPanel.
	 */
	public LogFilter getLogFilter()
	{
		return filter;
	}
}
