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
package com.teletalk.jadmin.gui;

import java.applet.AudioClip;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextPane;
import javax.swing.JToolBar;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.Border;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;

import com.teletalk.jadmin.JAdmin;
import com.teletalk.jadmin.gui.logv2.LoggingMainPanel;
import com.teletalk.jadmin.gui.overview.OverviewPanel;
import com.teletalk.jadmin.gui.util.GuiUtils;
import com.teletalk.jadmin.gui.util.Indicator;
import com.teletalk.jadmin.gui.util.ResourceHandler;
import com.teletalk.jadmin.proxy.JServerProxy;
import com.teletalk.jserver.log.LogMessage;
import com.teletalk.jserver.rmi.client.CustomAdministrationPanel;
import com.teletalk.jserver.rmi.remote.RemoteLogEvent;
import com.teletalk.jserver.rmi.remote.RemoteLoggingEvent;

/**
 * The main panel of the JAdmin GUI.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class AdminMainPanel extends JPanel implements  ActionListener //, KeyListener //, ChangeListener
{
   private static final long serialVersionUID = 1L; // Dummy serial uid
   
   
	private static final boolean STATE_DISCONNECTED = false;
	private static final boolean STATE_CONNECTED = true;
	
	volatile boolean currentState = STATE_DISCONNECTED;
	
	public static boolean soundsEnabled = true;
	
	private JAdmin admin;	
		
	private OverviewPanel overviewPanel;
	private com.teletalk.jadmin.gui.log.LoggingMainPanel logPanelVersion1;
   private LoggingMainPanel logPanelVersion2;
	
	private JToolBar toolbar;
	JTabbedPane tabControl = null;
	private Indicator masterAlarm;
	
	private Box imagePanel;
	private JLabel imageLabel;
	
	private JButton connect;
	private JButton disconnect;
	private JCheckBox soundButton;
	
	MutableAttributeSet eventLogDateAttributes;
	MutableAttributeSet eventLogTextAttributes;
	
	JTextPane eventLog;
	DefaultStyledDocument eventLogDocument;
	JScrollPane eventLogScroll;
	
	SimpleDateFormat dateFormat;
	
	final Frame mainFrame;
	
	private final AudioClip connectSound;
	
	private ConnectDialog connectDialog = null;
	
	private AbstractAction connectAction;
	private AbstractAction disconnectAction;

	/**
	 * Creates a new AdminMainPanel
	 */
	public AdminMainPanel(JAdmin admin, Frame mainFrame) throws Exception
	{
		this.admin = admin;
		this.mainFrame = mainFrame;
		
		connectSound = ResourceHandler.getAudioClip("/sounds/connect.wav");
						
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		setLocation(screenSize.width/2 - WIDTH/2, screenSize.height/2 - HEIGHT/2);
		setSize(WIDTH, HEIGHT);
		
		toolbar = new JToolBar();
		toolbar.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		
		connect = new JButton(new ImageIcon(this.getClass().getResource("/images/connect_normal.gif")));
		connect.setRolloverIcon(new ImageIcon(this.getClass().getResource("/images/connect_over.gif")));
		connect.setPressedIcon(new ImageIcon(this.getClass().getResource("/images/connect_normal.gif")));
		connect.setDisabledIcon(new ImageIcon(this.getClass().getResource("/images/connect_disabled.gif")));
		connect.setBorderPainted(false);
		connect.setContentAreaFilled(false);
		connect.setFocusPainted(false);
		connect.setRolloverEnabled(true);
		connect.setToolTipText("Connect to server");
				
		disconnect = new JButton(new ImageIcon(this.getClass().getResource("/images/disconnect_normal.gif")));
		disconnect.setRolloverIcon(new ImageIcon(this.getClass().getResource("/images/disconnect_over.gif")));
		disconnect.setPressedIcon(new ImageIcon(this.getClass().getResource("/images/disconnect_normal.gif")));
		disconnect.setDisabledIcon(new ImageIcon(this.getClass().getResource("/images/disconnect_disabled.gif")));
		disconnect.setBorderPainted(false);
		disconnect.setContentAreaFilled(false);
		disconnect.setFocusPainted(false);
		disconnect.setRolloverEnabled(true);
		disconnect.setToolTipText("Disconnect from server");
		
		soundButton = new JCheckBox();
		soundButton.setIcon(new ImageIcon(this.getClass().getResource("/images/soundDisabled.gif")));
		soundButton.setSelectedIcon(new ImageIcon(this.getClass().getResource("/images/sound.gif")));
		soundButton.setSelected(true);
		soundButton.setToolTipText("Click to disable sounds");
				
		connect.addActionListener(this);
		//connect.addChangeListener(this);
		disconnect.addActionListener(this);
		soundButton.addActionListener(this);
			
		toolbar.setFloatable(false);
		toolbar.add(connect);
		toolbar.add(disconnect);
		toolbar.add(Box.createGlue());
		toolbar.add(soundButton);
			
				
		imageLabel = new JLabel(new ImageIcon(mainFrame.getClass().getResource("/images/JAdmin.gif")));
		Border b1 = BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), BorderFactory.createEtchedBorder());
		Border b2 = BorderFactory.createCompoundBorder(BorderFactory.createLoweredBevelBorder(), b1);
		imageLabel.setBorder(b2);
		imageLabel.setAlignmentX(CENTER_ALIGNMENT);
		imageLabel.setAlignmentY(CENTER_ALIGNMENT);

		JPanel southPanel = new JPanel(new BorderLayout());
		southPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		
		// Make sure StyleConstants is loaded and initialized (seems to be a bug in swing or the JVM)
		((Class)StyleConstants.class).getClass();
		((Class)StyleConstants.FontConstants.class).getClass();
		
		eventLogDateAttributes = new SimpleAttributeSet();
		StyleConstants.FontConstants.setFontFamily(eventLogDateAttributes, "SansSerif");
		StyleConstants.FontConstants.setFontSize(eventLogDateAttributes, 11);
		StyleConstants.FontConstants.setForeground(eventLogDateAttributes, Color.black);
		StyleConstants.FontConstants.setBold(eventLogDateAttributes, true);
		
		eventLogTextAttributes = new SimpleAttributeSet();
		StyleConstants.FontConstants.setFontFamily(eventLogTextAttributes, "SansSerif");
		StyleConstants.FontConstants.setFontSize(eventLogTextAttributes, 10);
		StyleConstants.FontConstants.setForeground(eventLogTextAttributes, Color.darkGray);
		StyleConstants.FontConstants.setBold(eventLogTextAttributes, false);
		
		eventLogDocument = new DefaultStyledDocument();
		eventLog = new JTextPane();
		eventLog.setStyledDocument(eventLogDocument);
		eventLog.setDocument(eventLogDocument);
		eventLog.setEditable(false);
		eventLog.setPreferredSize(new Dimension(eventLog.getPreferredSize().width, 70));
		eventLog.setMaximumSize(new Dimension(eventLog.getPreferredSize().width, 70));
		
		eventLogScroll = new JScrollPane(eventLog);
		eventLogScroll.setBorder(BorderFactory.createTitledBorder("Eventlog"));
		
		eventLogScroll.setPreferredSize(new Dimension(eventLogScroll.getPreferredSize().width, 75));
		eventLogScroll.setMaximumSize(new Dimension(eventLogScroll.getPreferredSize().width, 75));
		
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		AudioClip masterAlarmSound = ResourceHandler.getAudioClip("/sounds/masteralarm.wav");
				
		masterAlarm = new Indicator(new ImageIcon(this.getClass().getResource("/images/masteralarm_on.gif")),
									new ImageIcon(this.getClass().getResource("/images/masteralarm_off.gif")), masterAlarmSound);
		
		southPanel.add(eventLogScroll, BorderLayout.CENTER);
		southPanel.add(masterAlarm, BorderLayout.EAST);
		
		this.setLayout(new BorderLayout());
				
		showDisconnected(true);
		
		overviewPanel = new OverviewPanel(this);
		
		this.add(BorderLayout.NORTH, toolbar);
		this.add(BorderLayout.SOUTH, southPanel);
		
		connectAction = new AbstractAction() {
				/**
          * 
          */
         private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) 
				{
					doConnect();
			 	}
		};
		
		disconnectAction = new AbstractAction() {
				/**
          * 
          */
         private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) 
				{
					doDisconnect();
				}
		};
				
		setKeyActions(this);
		setKeyActions(toolbar);
		setKeyActions(overviewPanel);
		setKeyActions(southPanel);
		setKeyActions(eventLog);
	}
	
	/**
	 * Sets the connect and disconnect key actions.
	 */
	public void setKeyActions(JComponent component)
	{
		component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "connectAction");
		component.getActionMap().put("connectAction",  connectAction);

		component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "disconnectAction");
		component.getActionMap().put("disconnectAction",  disconnectAction);	 
	}
	
	/**
	 * Gets the main frame of this application.
	 */
	public Frame getMainFrame()
	{
		return this.mainFrame;	
	}
		
	/**
	 * Displays the GUI components needed when connected to server.
	 */
	private synchronized void showConnected()
	{
		if(connectSound != null && AdminMainPanel.soundsEnabled) connectSound.play();
		try
		{
			imagePanel.remove(imageLabel);
			this.remove(imagePanel);
			this.revalidate();
		}catch(Exception e){e.printStackTrace();}

		imagePanel = null;
						
		tabControl = new JTabbedPane();
		overviewPanel.initialize(getJServerProxy());
		
      tabControl.addTab("Overview", null, overviewPanel, "Overview of server");
      
      if( this.admin.getAdministrator().getCurrentProtocolVersion() > 1 )
      {
         logPanelVersion2 = new LoggingMainPanel(this);
         tabControl.addTab("Logging", null, logPanelVersion2, "View live and saved logs");
      }
      else
      {
         logPanelVersion1 = new com.teletalk.jadmin.gui.log.LoggingMainPanel(this);
         tabControl.addTab("Logging", null, logPanelVersion1, "View live and saved logs");
      }
      
		this.add(tabControl, BorderLayout.CENTER);
		
		writeToEventLog("Connected!");
		//connect.setRolloverEnabled(false);
		connect.setEnabled(false);		
		disconnect.setEnabled(true);
		disconnect.setRolloverEnabled(true);
		disconnect.getModel().setRollover(false);
						
		this.validateTree();
		
		// Set state to connected
		AdminMainPanel.this.currentState = STATE_CONNECTED;
	}
	
	/**
	 * Displays the GUI components needed when disconnected from server.
	 */
	synchronized void showDisconnected(boolean isInitializing)
	{
		if(isInitializing) writeToEventLog("Initializing " + JAdmin.longVersionString);
		else writeToEventLog("Disconnected!");
		
		connect.setEnabled(true);
		connect.setRolloverEnabled(true);
		connect.getModel().setRollover(false);
		disconnect.setEnabled(false);
		
		try
		{
			overviewPanel.destroy();
			tabControl.removeAll();
			this.remove(tabControl);
			this.revalidate();
			
		}catch(Exception e){}
		
      if(logPanelVersion1 != null) 
      {
         logPanelVersion1.destroy();
         logPanelVersion1 = null;
      }   
      
      if(logPanelVersion2 != null) 
      {
         logPanelVersion2.destroy();
         logPanelVersion2 = null;
      }
      
		tabControl = null; 
		
		imagePanel = new Box(BoxLayout.Y_AXIS);
		imagePanel.add(Box.createGlue());
		imagePanel.add(imageLabel);
		imagePanel.add(Box.createGlue());
		
		this.add(imagePanel, BorderLayout.CENTER);

		this.getMainFrame().setSize(this.getMainFrame().getSize());
		
		//imagePanel.addKeyListener(this);
		
		this.requestFocus();
		
		// Set state to disconnected
		this.currentState = STATE_DISCONNECTED;
	}
	
	/**
	 * Called to indicate that a connection to the server is beeing established.
	 */
	public void connectingToServer()
	{
		this.showConnected();
	}
	
	/**
	 * Called to indicate that a connection to the server has been established.
	 */
	public void connectedToServer()
	{
		if(logPanelVersion1 != null) logPanelVersion1.initLoggerCombos();
      if(logPanelVersion2 != null) logPanelVersion2.initAppenderCombos();
		overviewPanel.connectedToServer();
	}
	
	/**
	 * Called to indicate that the custom administration panels have been loaded.
	 */
	public void administrationPanelsLoaded(final java.util.List customPanels)
	{
		Runnable connectedToServerImpl = new Runnable()
			{
				public void run()
				{
					try
					{
						//java.util.List customPanels = admin.getCustomAdministrationPanels();
						synchronized(AdminMainPanel.this)
						{
							if( AdminMainPanel.this.currentState == STATE_CONNECTED )
							{
								if( (customPanels != null) && (customPanels.size() > 0) )
								{
									for(int i=0; i<customPanels.size(); i++)
									{
										CustomAdministrationPanel customPanel = (CustomAdministrationPanel)customPanels.get(i);
										String title = customPanel.getTitle();
												
										if(title != null) tabControl.addTab(title, null, customPanel, title);
										else tabControl.addTab("Custom panel " + i, null, customPanel, "Custom panel " + i);
									}
								}
							}
						}
					}
					catch(Throwable e)
					{
						e.printStackTrace();
						JOptionPane.showMessageDialog(mainFrame, "Error while adding custom adminstration panels - " + e, "Error", JOptionPane.ERROR_MESSAGE);
					}
					
					AdminMainPanel.this.getMainFrame().setSize(AdminMainPanel.this.getMainFrame().getSize());
				}
			};
		
		//Make the event-dispatching thread (in Swing/AWT) execute the code implemented in connectedToServerImpl 
		SwingUtilities.invokeLater(connectedToServerImpl);
	}
			
	/**
	 * Called to indicate that the administration tool no longer is connected to a server.
	 */
	public final void disconnectedFromServer()
	{
		Runnable disconnectedFromServerImpl = new Runnable()
			{
				public void run()
				{
					showDisconnected(false);
				}
			};
		
		//Make the event-dispatching thread (in Swing/AWT) execute the code implemented in disconnectedFromServer, in case
		//this method is called by another thread.
		SwingUtilities.invokeLater(disconnectedFromServerImpl);
	}
	
	/**
	 * Action event handler method.
	 */
	public void actionPerformed(final ActionEvent event)
	{
		if(event.getSource() == connect)
		{
			this.doConnect();
		}
		else if(event.getSource() == disconnect)
		{
			this.doDisconnect();
		}
		else if(event.getSource() == soundButton)
		{
			soundsEnabled = soundButton.isSelected();
			if(soundsEnabled) soundButton.setToolTipText("Click to disable sounds");
			else soundButton.setToolTipText("Click to enable sounds");
		}
	}
	
	/**
	 * Connects to the server.
	 */
	synchronized void doConnect()
	{
		if( (connectDialog == null) && (this.currentState != STATE_CONNECTED) ) // If a connect dialog is not already displayed		{			try
			{				connectDialog = new ConnectDialog(admin);
											Dimension dlgSize = connectDialog.getPreferredSize();
				Dimension frmSize = mainFrame.getSize();
				Point loc = mainFrame.getLocation();
				connectDialog.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
							
				connectDialog.setVisible(true);
						
				if(connectDialog.closedOk())
				{
					String host = connectDialog.getHost();
					int port = connectDialog.getPort();
					String server = connectDialog.getServer();
							
					if(!server.equals(""))
					{
						try
						{
							connect.setEnabled(false);
							writeToEventLog("Connecting...");
							
							admin.connect(host, port, server);
						}
						catch(Exception e)
						{
							GuiUtils.showErrorMessage(mainFrame, "Failed to connect to server - " + e, "Connection faliure");
							admin.disconnect();
							showDisconnected(false);
							e.printStackTrace();
						}
					}
					else JOptionPane.showMessageDialog(mainFrame, "Invalid servername", "Error", JOptionPane.ERROR_MESSAGE);
				}
			}
			finally
			{
				connectDialog = null;
			}
		}
	}
	
	/**
	 * Disconnects from the server.
	 */
	synchronized void doDisconnect()
	{
		if(this.currentState != STATE_DISCONNECTED)
		{
			disconnect.setEnabled(false);
			try
			{
				writeToEventLog("Disconnecting...");
				admin.disconnect();
			}
			catch(Exception e){}
			showDisconnected(false);
		}
	}
	
	/**
	 * Gets the parent JAdmin object.
	 * 
	 * @return the JAdmin object.
	 */
	public JAdmin getJAdmin()
	{
		return admin;
	}
	
	/**
	 * Convenience method to gets the JServerProxy representing the remote JServer.
	 * 
	 * @return the JServerProxy.
	 */
	public final JServerProxy getJServerProxy()
	{
		return admin.getJServerProxy();
	}
	
	/**
	 * Gets the overview panel.
	 * 
	 * @return an OverviewPanel object implementing the overview panel GUI.
	 */
	public final OverviewPanel getAdministrationPanel()
	{
		return overviewPanel;
	}
	
	/**
	 * Gets the logging panel.
	 * 
	 * @return a LoggingMainPanel object implementing GUI for the logging panels.
	 */
	/*public final LoggingMainPanel getLoggingMainPanel()
	{
		return logPanelVersion1;	
	}*/
	
	/**
	 * Called when a new log event is received from the server.
	 */
	public void handleEvent(final Object event)
	{
      if( event instanceof RemoteLoggingEvent )
      {
         LoggingEvent loggingEvent = ((RemoteLoggingEvent)event).getLoggingEvent();
         if( loggingEvent.getLevel().toInt() == Level.FATAL_INT )
         {
            writeToEventLog("Critical error in " + loggingEvent.getThreadName());
            triggerMasterAlarm();
         }
      }
      else if( event instanceof RemoteLogEvent )
      {
         LogMessage logMessage = ((RemoteLogEvent)event).getLogMessage();
   		if(logMessage.level == LogMessage.CRITICAL_ERROR_LEVEL)
   		{
   			writeToEventLog("Critical error in " + logMessage.origin);
   			triggerMasterAlarm();
   		}
      }
      
      if( this.logPanelVersion1 != null ) this.logPanelVersion1.handleEvent(event);
      if( this.logPanelVersion2 != null ) this.logPanelVersion2.handleEvent(event);
	}
	
	/**
	 * Method to trigger the master alarm indicator.
	 */
	public void triggerMasterAlarm()
	{
		masterAlarm.on();	
	}
	
	/**
	 * Writes a message to the event log at the bottom of the GUI.
	 */
	public synchronized final void writeToEventLog(final String event)
	{
		Runnable writeToEventLogImpl = new Runnable()
			{
				public void run()
				{
					if(eventLogDocument.getLength() > 25000)
					{
						eventLog.setText("");
					}
		
					try
					{
						if(eventLogDocument.getLength() < 1)
                  {
							eventLogDocument.insertString(eventLogDocument.getLength(), dateFormat.format(new Date()) + " - ", eventLogDateAttributes);
                  }
						else
                  {
							eventLogDocument.insertString(eventLogDocument.getLength(), System.getProperty("line.separator") + dateFormat.format(new Date()) + " - ", eventLogDateAttributes);
                  }
						eventLogDocument.insertString(eventLogDocument.getLength(), event, eventLogTextAttributes);
					}
					catch(Exception e){e.printStackTrace();}
		
					//eventLogScroll.revalidate();
					eventLogScroll.getVerticalScrollBar().setValue(eventLogScroll.getVerticalScrollBar().getMaximum());
				}
			};
		
		//Make the event-dispatching thread (in Swing/AWT) execute the code implemented in disconnectedFromServer, in case
		//this method is called by another thread.
		SwingUtilities.invokeLater(writeToEventLogImpl);
	}
	
	// -----------------------------------
	// ---- Key event handling methods ----
	// -----------------------------------
	
	/**
	 * Event handler method for handling key press events.
	 */
	/*public void keyPressed(KeyEvent e)
	{
System.out.println("AdminMainPanel - keyPressed - event " + e);
		if(e.getKeyCode() == KeyEvent.VK_ESCAPE) this.doDisconnect();
		else if(e.getKeyCode() == KeyEvent.VK_ENTER) this.doConnect();
		e.consume();
	}

	public void keyTyped(KeyEvent e){}
	public void keyReleased(KeyEvent e){}*/
}
