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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;

import com.teletalk.jadmin.gui.AdminMainPanel;
import com.teletalk.jadmin.proxy.LoggerProxy;
import com.teletalk.jadmin.proxy.messages.LogRequestMessage;
import com.teletalk.jadmin.proxy.messages.LogRequestResultMessage;
import com.teletalk.jadmin.proxy.messages.LoggerRequestMessage;
import com.teletalk.jadmin.proxy.messages.LoggerRequestResultMessage;
import com.teletalk.jserver.log.LogFilter;

/**
 * Class responsible for handling the panel where saved (old) log messages are displayed.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class SavedLogPanel extends LogPanel implements ActionListener
{
	/**
    * 
    */
   private static final long serialVersionUID = 1L;

   private boolean initialized;
	
	private final LogFilterPanel savedFilterPanel;
	private LogTable savedLogTable;
	
	private final JButton getLog;
	
	private final HashMap loggers;
	
	private final JComboBox loggersCombo;
	private final JComboBox logCombo;
	
	private final JLabel getLogStandby;
	
	private LoggerRequestMessage currentLoggerRequestMessage;
	private LogRequestMessage currentLogRequestMessage;
	
	private boolean loggerCombosInitialized = false;
	
	/**
	 * Creates a new SavedLogPanel.
	 */
	public SavedLogPanel(final AdminMainPanel mainPanel)
	{
		super(new BorderLayout(), mainPanel);

		this.mainPanel = mainPanel;
		
		initialized = false;
		
		currentLoggerRequestMessage = null;
		currentLogRequestMessage = null;
		
		loggers = new HashMap();

			JPanel savedLogUpper = new JPanel(new GridLayout(1, 2));
			
				JPanel savedLogUpperLeft = new JPanel(new BorderLayout());
					
					JPanel savedLogUpperLeftInner = new JPanel();
					savedLogUpperLeftInner.setLayout(new BoxLayout(savedLogUpperLeftInner, BoxLayout.Y_AXIS));
					
					loggersCombo = new JComboBox();
					loggersCombo.setFont(new Font("SansSerif", Font.PLAIN, 11));
					logCombo = new JComboBox();
					logCombo.setFont(new Font("SansSerif", Font.PLAIN, 11));
					
					loggersCombo.addActionListener(this);
					
					logCombo.setAlignmentX(CENTER_ALIGNMENT);
					loggersCombo.setAlignmentX(CENTER_ALIGNMENT);
					
					loggersCombo.setBorder(BorderFactory.createTitledBorder("Available loggers"));
					logCombo.setBorder(BorderFactory.createTitledBorder("Available logs"));
					
					getLog = new JButton();
					if(this.getClass().getResource("/images/get_log.gif") != null)
						getLog.setIcon(new ImageIcon(this.getClass().getResource("/images/get_log.gif")));
					if(this.getClass().getResource("/images/get_log_pressed.gif") != null)
						getLog.setPressedIcon(new ImageIcon(this.getClass().getResource("/images/get_log_pressed.gif")));
					
					getLogStandby = new JLabel("Please wait");
					getLogStandby.setVisible(false);
					getLogStandby.setAlignmentX(CENTER_ALIGNMENT);
					
					getLog.setAlignmentX(CENTER_ALIGNMENT);
					getLog.setAlignmentY(CENTER_ALIGNMENT);
					getLog.addActionListener(this);
					getLog.setBorderPainted(false);
					getLog.setFocusPainted(false);
					getLog.setContentAreaFilled(false);
										
					savedLogUpperLeftInner.add(Box.createGlue());
					
					savedLogUpperLeftInner.add(loggersCombo);
					savedLogUpperLeftInner.add(logCombo);
					savedLogUpperLeftInner.add(getLogStandby);

					savedLogUpperLeftInner.add(Box.createGlue());
					
					JPanel getLogButtoPanel = new JPanel();
					getLogButtoPanel.setLayout(new BoxLayout(getLogButtoPanel, BoxLayout.Y_AXIS));

					getLogButtoPanel.add(Box.createGlue());
					getLogButtoPanel.add(getLog);
					getLogButtoPanel.add(Box.createGlue());
					
				savedLogUpperLeft.add(getLogButtoPanel, BorderLayout.WEST);
				savedLogUpperLeft.add(savedLogUpperLeftInner, BorderLayout.CENTER);
					
			savedFilterPanel = new LogFilterPanel(this);
			
			Dimension d = savedFilterPanel.getPreferredSize();
			d.height = 150;
			savedFilterPanel.setPreferredSize(d);

			savedLogUpper.add(savedLogUpperLeft);
			savedLogUpper.add(savedFilterPanel);
			
		savedLogTable = new LogTable();
		
		add(BorderLayout.NORTH, savedLogUpper);
		add(BorderLayout.CENTER, savedLogTable);
		
		validate();
	}
	
	/**
	 * Destroys this SavedLogPanel.
	 */
	public void destroy()
	{
		if(savedLogTable != null)
		{
			savedLogTable.destroy();
			savedLogTable = null;
		}
	}

	/**
	 * Initializes the combo boxes that contans information about available loggers ang logs on the server.
	 */	
	public void initLoggerCombos()
	{
		ArrayList loggerProxies = mainPanel.getJServerProxy().getLoggers();
		LoggerProxy l;
		
		for(int i=0; i<loggerProxies.size(); i++)
		{
			l = (LoggerProxy)loggerProxies.get(i);
			
			loggers.put(l.getName(), l);
			loggersCombo.addItem(l.getName());
		}
		
		if(loggersCombo.getItemCount() > 0)
		{
			loggersCombo.setSelectedIndex(0);
			initialized = true;
			//loggerComboSelectionChanged();
		}
		
		loggersCombo.validate();
		validate();
		
		loggerCombosInitialized = true;
	}
	
	/**
	 * Called when the selection has changed in the loggers and logs combo boxes.
	 */
	public void loggerComboSelectionChanged()
	{
		if(!loggerCombosInitialized) return;
		
		Object selobj = loggers.get(loggersCombo.getSelectedItem());
		LoggerProxy logger;
		
		if(selobj != null && initialized)
		{
			logger = (LoggerProxy)selobj;
			
			if(logCombo.getItemCount() > 0) logCombo.removeAllItems();
						
			currentLoggerRequestMessage = mainPanel.getJServerProxy().getLogs(logger);
			
			logCombo.setEnabled(false);
			getLogStandby.setVisible(true);
		}
		
		loggersCombo.validate();
		logCombo.validate();
		validate();
	}
	
	/**
	 * Downloads the currently selected log (from the selected logger) from the server.
	 */
	public boolean getSelectedLog()
	{
		LogFilter filter = this.getLogFilter();//savedFilterPanel.getLogFilter();
		LoggerProxy selectedLogger = (LoggerProxy)loggers.get(loggersCombo.getSelectedItem());
		String selectedLog = (String)logCombo.getSelectedItem();
		savedLogTable.clear();
		savedLogTable.setTitle("");

		if((selectedLogger != null) && (selectedLog != null))
		{
			currentLogRequestMessage = mainPanel.getJServerProxy().getLog(selectedLogger, selectedLog, filter);
			return true;
		}
		return false;
	}
	
	/**
	 * Action event handler method.
	 */
	public void actionPerformed(ActionEvent event)
	{
		if(event.getSource() == getLog)// && !gettingLog)
		{
			if(getSelectedLog())
			{
				//gettingLog = true;
				getLogStandby.setVisible(true);
			}
		}
		else if(event.getSource() == loggersCombo)
		{
			loggerComboSelectionChanged();
		}
	}
	
	/**
	 * Called to process an event received from the server.
	 */
   public void handleEvent(Object event)
	{
		if(event instanceof LoggerRequestResultMessage)
		{
			LoggerRequestResultMessage lrrm = (LoggerRequestResultMessage)event;

			if(lrrm.request == currentLoggerRequestMessage)
			{
				java.util.List logList = lrrm.logs;
					
				if(logCombo.getItemCount() > 0) logCombo.removeAllItems();
				
				for(int i=0; i<logList.size(); i++)
				{
					logCombo.addItem((String)logList.get(i));
				}
					
				getLogStandby.setVisible(false);
				logCombo.setEnabled(true);
				logCombo.validate();
				validate();
			}
			
		}
		else if(event instanceof LogRequestResultMessage)
		{
			LogRequestResultMessage lrrm = (LogRequestResultMessage)event;

			if(lrrm.request == currentLogRequestMessage)
			{
				java.util.List logmessages = lrrm.logMessages;
				savedLogTable.newLog(logmessages);
				savedLogTable.setTitle(currentLogRequestMessage.logName);
				//gettingLog = false;
				getLogStandby.setVisible(false);
			}
		}
	}
	
	/**
	 * Callled when the log message filter associated with this LogPanel has changed.
	 */
	public void logFilterChanged(LogFilter filter)
	{
		getSelectedLog();
	}
}
