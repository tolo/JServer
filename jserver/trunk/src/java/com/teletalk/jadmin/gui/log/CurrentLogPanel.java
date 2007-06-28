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
import java.util.ArrayList;

import javax.swing.JPanel;

import com.teletalk.jadmin.gui.AdminMainPanel;
import com.teletalk.jserver.log.LogFilter;
import com.teletalk.jserver.log.LogMessage;
import com.teletalk.jserver.rmi.remote.RemoteLogEvent;
import com.teletalk.jserver.util.MessageQueue;

/**
 * Class responsible for handling the panel where current log messages are displayed.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class CurrentLogPanel extends LogPanel implements Runnable
{
	/**
    * 
    */
   private static final long serialVersionUID = 1L;

   private boolean canRun;	

	//private JTabbedPane tabControl;
	
	private final LogFilterPanel currentFilterPanel;
	private LogTable currentLogTable;
	
	private final MessageQueue queuedLogMessages;
	private final ArrayList logMessages;
	private final int maxSize;
	private final int overshoot;
	
	private int rejectedLogmessages;

	private Thread thread;
	
	/**
	 * Creates a new CurrentLogPanel.
	 */
	public CurrentLogPanel(final AdminMainPanel mainPanel)
	{
		super(new BorderLayout(), mainPanel);

		this.mainPanel = mainPanel;

			JPanel currentLogUpper = new JPanel(new BorderLayout());
			
			currentFilterPanel = new LogFilterPanel(this);
			Dimension d = currentFilterPanel.getPreferredSize();
			d.height = 150;
			currentFilterPanel.setPreferredSize(d);

			currentLogUpper.add(currentFilterPanel, BorderLayout.CENTER);
		
		currentLogTable = new LogTable();
		
		add(BorderLayout.NORTH, currentLogUpper);
		add(BorderLayout.CENTER, currentLogTable);
		
		this.maxSize = 1000;
		this.overshoot = 50;
		
		queuedLogMessages = new MessageQueue();
		logMessages = new ArrayList((int)(maxSize/10));
		
		start();
	}
	
	/**
	 * Start the internal thread.
	 */
	private void start()
	{
		canRun = true;
		thread = new Thread(this, "CurrentLogPanel thread");
		thread.setDaemon(true);
		thread.start();
	}
	
	/**
	 * Destroys the thread of this CurrentLogPanel.
	 */
	public void destroy()
	{
		canRun = false;
		thread.interrupt();
		
		if(currentLogTable != null) 
		{
			currentLogTable.destroy();
			currentLogTable = null;
		}
	}

	/**
	 * Callled when the log message filter associated with this LogPanel has changed.
	 */
	public synchronized void logFilterChanged(final LogFilter filter)
	{
		currentLogTable.newLog(getFilteredLog(filter));
	}
	
	/**
	 * Called to process an event received from the server.
	 */
   public void handleEvent(Object event)
	{
		if(event instanceof RemoteLogEvent)
		{
			if(queuedLogMessages.size() < 1000)
			{
				if(rejectedLogmessages > 0)
				{
					this.getMainPanel().writeToEventLog("Warning! Overflow in messagequeue for logmessages (current log panel)! " + rejectedLogmessages + " messages rejected!");
					rejectedLogmessages = 0;
				}
				
				queuedLogMessages.putMsg(event);
			}
			else
			{
				rejectedLogmessages++;
			}
		}										 
	}

	/**
	 * Returns the LogMessages in the buffer that passes the supplied filter.
	 * 
	 * @param filter a LogFiler.
	 * 
	 * @return a Vector containing LogMessages.
	 * 
	 * @see com.teletalk.jserver.log.LogFilter
	 * @see com.teletalk.jserver.log.LogMessage
	 */
	private synchronized java.util.List getFilteredLog(final LogFilter filter)
	{
		ArrayList filtered = new ArrayList();
		
		LogMessage[] lArray = (LogMessage[])logMessages.toArray(new LogMessage[]{});
		
		for(int i=0; i<lArray.length; i++)
		{
			if(filter.filterLogMessage(lArray[i]))
				filtered.add(lArray[i]);
		}
		
		return filtered;
	}
	
	/**
	 * Called when a new RemoteLogEvent is received by the LogBuffer.
	 * 
	 * @param event the RemoteLogEvent.
	 * 
	 * @see com.teletalk.jserver.rmi.RemoteLogEvent
	 */
	private synchronized void processLogEvent(RemoteLogEvent event)
	{
		final LogMessage logMsg = event.getLogMessage();
		
		logMessages.add(logMsg);

		if(logMessages.size() > (maxSize + overshoot))
		{
			for(int i=0; i<overshoot; i++)
				logMessages.remove(i);
			
			currentLogTable.newLog(getFilteredLog(this.getLogFilter()));
		}
		else
		{
			if(this.getLogFilter().filterLogMessage(logMsg))
			{
				currentLogTable.newLogMessage(logMsg);
			}
		}
	}
	
	/**
	 * The thread method of this LogBuffer. Checkes the queue for new LogEvents.
	 */
	public void run()
	{
		RemoteLogEvent event;

		while(canRun)
		{			
			try
			{
				event = (RemoteLogEvent)queuedLogMessages.getMsg();
				processLogEvent(event);
			}catch(Exception e)	{}
		}
	}
}
