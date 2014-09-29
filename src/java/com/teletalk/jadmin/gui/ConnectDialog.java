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

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.KeyStroke;
import javax.swing.ListSelectionModel;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.teletalk.jadmin.JAdmin;
import com.teletalk.jadmin.gui.util.GuiUtils;

/**
 * This class implements the dialog box used when connecting to a server.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public class ConnectDialog extends JDialog implements ActionListener, ChangeListener, MouseListener
{
   private static final long serialVersionUID = 1L; // Dummy serial uid
   
   
   private static String lastRmiRegistryAddress = "";
	private static String lastRmiRegistryPort = "";
	private static String lastServerName = "";
	
	private static final String WIZARD_PAGE_1 = "W1";
	private static final String WIZARD_PAGE_2 = "W2";
		
	private boolean endOk = false;
	
	private JTextField whost;
	private JTextField wport;
	
	private JTextField host;
	private JTextField port;
	private JTextField server;
	
	private JButton listServers;
	private JButton wizardStart;
	private JButton connectToSelected;
	private JButton w_cancel1;
	private JButton w_cancel2;
	
	private JButton connect;
	private JButton m_cancel;
	
	private JList serverList;
	private DefaultListModel listModel;
	
	JTabbedPane tabControl = null;
	
	JPanel wizardPanels;
	private CardLayout wizardLayout;
	
	JPanel manualConnectPanel;
	
	private JAdmin admin;	
	
	private String selectedHost;
	private int selectedPort;
	private String selectedServer;
	
	String currentWizardPage = WIZARD_PAGE_1;
	
	//private long lastReturnKeyPressed = 0;
	
	private AbstractAction enterAction;
	private AbstractAction backSpaceAction;
	private AbstractAction escapeAction;
	
	/**
	 * Creates a new ConnectDialog.
	 */
	public ConnectDialog(final JAdmin admin)
	{
		super(admin.getMainFrame(), "Connect to JServer", true);
		
		enterAction = new AbstractAction() {
				/**
          * 
          */
         private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) 
				{
					if(tabControl.getSelectedComponent() == wizardPanels) //  Wizard
					{
						if(currentWizardPage.equals(WIZARD_PAGE_1)) listServers();
						else connectToSelected();
					}
					else manualConnect();
				}
		};

		backSpaceAction = new AbstractAction() {
				/**
          * 
          */
         private static final long serialVersionUID = 1L;

            public void actionPerformed(ActionEvent e) 
				{
					if(tabControl.getSelectedComponent() == wizardPanels) //  Wizard
					{
						if(currentWizardPage.equals(WIZARD_PAGE_2)) // Go back
						{
							goToWizardStart();
						}
					}
				}
		};
		
		escapeAction = new AbstractAction() {
					/**
          * 
          */
         private static final long serialVersionUID = 1L;

               public void actionPerformed(ActionEvent e) 
					{
						if(tabControl.getSelectedComponent() == wizardPanels) //  Wizard
						{
							if(currentWizardPage.equals(WIZARD_PAGE_2)) // Go back
							{
								goToWizardStart();
							}
							else // Go to manual tab
							{
								tabControl.setSelectedComponent(manualConnectPanel);
							}
						}
						else // Go to wizard tab
						{
							tabControl.setSelectedComponent(wizardPanels);
						}
					}
			};
	
				
		selectedHost = "";
		selectedPort = -1;
		selectedServer = "";
		
		this.admin = admin;
	
		JPanel mainPanel = (JPanel)this.getContentPane();
		mainPanel.setLayout(new BorderLayout());
		this.setKeyActions(mainPanel);
		
		tabControl = new JTabbedPane();
		this.setKeyActions(tabControl);
		tabControl.addChangeListener(this); // Add this dialog as a change listener to the tab control
		
		wizardLayout = new CardLayout();
		wizardPanels = new JPanel(wizardLayout);
		this.setKeyActions(wizardPanels);
		
		//Connect wizard page 1
		JPanel wizardPanel1 = new JPanel();
		this.setKeyActions(wizardPanel1);
		wizardPanel1.setLayout(new BorderLayout());
		
			JPanel wizardMainPanel = new JPanel();
			wizardMainPanel.setLayout(new BoxLayout(wizardMainPanel, BoxLayout.Y_AXIS));
		
				JPanel wizardBorderPanel = new JPanel();
				wizardBorderPanel.setLayout(new BorderLayout());
				
					JPanel wizardInnerPanel = new JPanel();
					wizardInnerPanel.setLayout(new GridLayout(2, 2, 10, 10));
								
					JLabel whostLabel = new JLabel("Host (leave blank for localhost): ");
					whost = new JTextField(lastRmiRegistryAddress, 15);
					this.setKeyActions(whost);
														
					wizardInnerPanel.add(whostLabel);
					wizardInnerPanel.add(whost);
					
					JLabel wportLabel = new JLabel("Port (leave blank for default): ");
					wport = new JTextField(lastRmiRegistryPort, 15);
					this.setKeyActions(wport);
									
					wizardInnerPanel.add(wportLabel);
					wizardInnerPanel.add(wport);
				
				wizardBorderPanel.add(wizardInnerPanel, BorderLayout.CENTER);
				wizardBorderPanel.add(Box.createVerticalStrut(10), BorderLayout.NORTH);
				wizardBorderPanel.add(Box.createHorizontalStrut(10), BorderLayout.EAST);
				wizardBorderPanel.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
				wizardBorderPanel.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);
				
				JPanel wizardInnerPanel3 = new JPanel();	
				wizardInnerPanel3.add(wizardBorderPanel);
		
			wizardMainPanel.add(Box.createGlue());
			wizardMainPanel.add(wizardInnerPanel3);
			wizardMainPanel.add(Box.createGlue());
			
			JPanel wizardSouthPanel1 = new JPanel(new FlowLayout(FlowLayout.CENTER));
			wizardSouthPanel1.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		
			listServers = new JButton("Next");
			listServers.setDefaultCapable(true);
			listServers.addActionListener(this);
			super.rootPane.setDefaultButton(listServers);
			w_cancel1 = new JButton("Cancel");
			w_cancel1.addActionListener(this);
					
			wizardSouthPanel1.add(listServers);
			wizardSouthPanel1.add(w_cancel1);
			
		wizardPanel1.add(wizardMainPanel, BorderLayout.CENTER);
		wizardPanel1.add(wizardSouthPanel1, BorderLayout.SOUTH);
		
		//Connect wizard page 2
		JPanel wizardPanel2 = new JPanel();
		this.setKeyActions(wizardPanel2);
		wizardPanel2.setLayout(new BorderLayout());
		
			wizardMainPanel = new JPanel();
			wizardMainPanel.setLayout(new BoxLayout(wizardMainPanel, BoxLayout.Y_AXIS));
		
				wizardBorderPanel = new JPanel();
				wizardBorderPanel.setLayout(new BorderLayout());
				
					wizardInnerPanel = new JPanel();	
					wizardInnerPanel.setLayout(new BorderLayout());
								
					listModel = new DefaultListModel();
					serverList = new JList(listModel);
					this.setKeyActions(serverList);
					serverList.setVisibleRowCount(4);
					serverList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
					serverList.setPrototypeCellValue("TestServer 123456 TestServer 123456 TestServer 123456");
					serverList.addMouseListener(this);
 					JScrollPane listScroll = new JScrollPane(serverList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
					
					JLabel serversLabel = new JLabel("Found servers: ");
					
					wizardInnerPanel.add(serversLabel, BorderLayout.NORTH);
					wizardInnerPanel.add(listScroll, BorderLayout.CENTER);
				
				wizardBorderPanel.add(wizardInnerPanel, BorderLayout.CENTER);
				wizardBorderPanel.add(Box.createVerticalStrut(10), BorderLayout.NORTH);
				wizardBorderPanel.add(Box.createHorizontalStrut(10), BorderLayout.EAST);
				wizardBorderPanel.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
				wizardBorderPanel.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);
				
				wizardInnerPanel3 = new JPanel();
				wizardInnerPanel3.add(wizardBorderPanel);
		
			wizardMainPanel.add(Box.createGlue());
			wizardMainPanel.add(wizardInnerPanel3);
			wizardMainPanel.add(Box.createGlue());
			
			JPanel wizardSouthPanel2 = new JPanel(new FlowLayout(FlowLayout.CENTER));
			wizardSouthPanel2.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		
			wizardStart = new JButton("Previous");
			wizardStart.addActionListener(this);
			connectToSelected = new JButton("Connect");
			connectToSelected.setDefaultCapable(true);
			connectToSelected.addActionListener(this);
			
			w_cancel2 = new JButton("Cancel");
			w_cancel2.addActionListener(this);
					
			wizardSouthPanel2.add(wizardStart);
			wizardSouthPanel2.add(connectToSelected);
			wizardSouthPanel2.add(w_cancel2);
			
		wizardPanel2.add(wizardMainPanel, BorderLayout.CENTER);
		wizardPanel2.add(wizardSouthPanel2, BorderLayout.SOUTH);
		
		wizardLayout.addLayoutComponent(wizardPanel1, WIZARD_PAGE_1);
		wizardLayout.addLayoutComponent(wizardPanel2, WIZARD_PAGE_2);
		
		wizardPanels.add(wizardPanel1, WIZARD_PAGE_1);
		wizardPanels.add(wizardPanel2, WIZARD_PAGE_2);
		
		wizardLayout.show(wizardPanels, WIZARD_PAGE_1);
				
		//Manual connect panel
		manualConnectPanel = new JPanel();
		this.setKeyActions(manualConnectPanel);
		manualConnectPanel.setLayout(new BorderLayout());
		
			JPanel manualConnectMainPanel = new JPanel();
			manualConnectMainPanel.setLayout(new BoxLayout(manualConnectMainPanel, BoxLayout.Y_AXIS));
		
				JPanel manualConnectBorderPanel = new JPanel();
				manualConnectBorderPanel.setLayout(new BorderLayout());
				
					JPanel manualConnectInnerPanel = new JPanel();
					manualConnectInnerPanel.setLayout(new GridLayout(3, 2, 10, 10));
								
					JLabel hostLabel = new JLabel("Host (leave blank for localhost): ");
					host = new JTextField(lastRmiRegistryAddress, 15);
					this.setKeyActions(host);
				
					manualConnectInnerPanel.add(hostLabel);
					manualConnectInnerPanel.add(host);
				
					JLabel portLabel = new JLabel("Port (leave blank for default): ");
					port = new JTextField(lastRmiRegistryPort, 15);
					this.setKeyActions(port);
				
					manualConnectInnerPanel.add(portLabel);
					manualConnectInnerPanel.add(port);
				
					JLabel serverLabel = new JLabel("Servername: ");
					server = new JTextField(lastServerName, 15);
					this.setKeyActions(server);
				
					manualConnectInnerPanel.add(serverLabel);
					manualConnectInnerPanel.add(server);
					
				manualConnectBorderPanel.add(manualConnectInnerPanel, BorderLayout.CENTER);
				manualConnectBorderPanel.add(Box.createVerticalStrut(10), BorderLayout.NORTH);
				manualConnectBorderPanel.add(Box.createHorizontalStrut(10), BorderLayout.EAST);
				manualConnectBorderPanel.add(Box.createHorizontalStrut(10), BorderLayout.WEST);
				manualConnectBorderPanel.add(Box.createVerticalStrut(10), BorderLayout.SOUTH);
				
				JPanel manualConnectInnerPanel3 = new JPanel();	
				manualConnectInnerPanel3.add(manualConnectBorderPanel);
		
			manualConnectMainPanel.add(Box.createGlue());
			manualConnectMainPanel.add(manualConnectInnerPanel3);
			manualConnectMainPanel.add(Box.createGlue());
			
			JPanel manualConnectSouthPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			manualConnectSouthPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		
			connect = new JButton("Connect");
			connect.setDefaultCapable(true);
			connect.addActionListener(this);
			m_cancel = new JButton("Cancel");
			m_cancel.addActionListener(this);
		
			manualConnectSouthPanel.add(connect);
			manualConnectSouthPanel.add(m_cancel);
			
		manualConnectPanel.add(manualConnectMainPanel, BorderLayout.CENTER);
		manualConnectPanel.add(manualConnectSouthPanel, BorderLayout.SOUTH);
		
		tabControl.addTab("Browse", null, wizardPanels, "Browse the RMI registry for a server");
		tabControl.addTab("Manual", null, manualConnectPanel, "Manual connect");
				
		mainPanel.add(tabControl, BorderLayout.CENTER);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		pack();
		
		// To solve stupid java awt focus bug/problem
		addWindowListener( new WindowAdapter() 
			{ 
				public void windowOpened( WindowEvent e ) 
				{
					//whost.getFocusTraversalPolicy().getDefaultComponent(whost).requestFocus();
					whost.requestDefaultFocus();
               whost.requestFocus();
				} 
			} 
		);
	}
	
	/**
	 */
	private void setKeyActions(JComponent component)
	{
		component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0), "enterAction");
		component.getActionMap().put("enterAction",  enterAction);
		
		component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_BACK_SPACE, 0), "backSpaceAction");
		component.getActionMap().put("backSpaceAction",  backSpaceAction);
		
		component.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "escapeAction");
		component.getActionMap().put("escapeAction",  escapeAction);
	}
	
	/**
	 * Called when the active tab is changed.
	 */
	public void stateChanged(ChangeEvent e)
	{
		if(e.getSource() == this.tabControl)
		{
			if(this.tabControl.getSelectedComponent() == this.wizardPanels)
			{
				if(this.currentWizardPage.equals(WIZARD_PAGE_1))
				{
					super.rootPane.setDefaultButton(this.listServers);
                    //whost.getFocusTraversalPolicy().getDefaultComponent(whost).requestFocus();
                    whost.requestDefaultFocus();
                    whost.requestFocus();
				}
				else
				{
					super.rootPane.setDefaultButton(this.connectToSelected);
                    //serverList.getFocusTraversalPolicy().getDefaultComponent(serverList).requestFocus();
                    serverList.requestDefaultFocus();
                    serverList.requestFocus();
				}
			}
			else
			{
				super.rootPane.setDefaultButton(this.connect);
                //host.getFocusTraversalPolicy().getDefaultComponent(host).requestFocus();
                host.requestDefaultFocus();
                host.requestFocus();
			}
		}
	}
	
	/**
	 * Action event handler method.
	 */
	public void actionPerformed(ActionEvent event)
	{
		Object src = event.getSource();
		
		if(src == connect) //  Manual mode - connect
		{
			this.manualConnect();
		}
		else if(src == listServers) // Wizard mode - list servers 
		{
			this.listServers();
		}
		else if(src == connectToSelected) // Wizard mode - connect
		{
			this.connectToSelected();
		}
		else if(src == wizardStart)
		{
			this.goToWizardStart();
		}
		else if(src == m_cancel || src == w_cancel1 || src == w_cancel2)
		{
			dispose();
		}
	}
	
	/**
	 * Goes to the wizard start page.
	 */
	void goToWizardStart()
	{
		wizardLayout.show(wizardPanels, WIZARD_PAGE_1);
		this.currentWizardPage = WIZARD_PAGE_1;
		super.rootPane.setDefaultButton(this.listServers);
        //whost.getFocusTraversalPolicy().getDefaultComponent(whost).requestFocus();
        whost.requestDefaultFocus();
        whost.requestFocus();
	}
	
	/**
	 * Validates and saves the data for a manual connect to a server and disposes the dialog box.
	 */
	void manualConnect()
	{
		boolean ok = true;
		String errStr = "?";
			
		selectedHost = host.getText();
		lastRmiRegistryAddress = selectedHost;
			
		if(!port.getText().equals(""))
		{
			try
			{
				selectedPort = Integer.parseInt(port.getText());
				lastRmiRegistryPort = port.getText();
			}
			catch(NumberFormatException e)
			{
				ok = false;
				errStr = "Invalid port value!";
			}
		}
			
		selectedServer = server.getText();
			
		if(selectedServer.equals(""))
		{
			ok = false;
			errStr = "No servername specified!";
		}
		else
			lastServerName = selectedServer;
									   
		if(ok)
		{
			endOk = true;
			dispose();
		}
		else
		{
			JOptionPane.showMessageDialog(this, errStr, "Input error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * Lists the servers on the specified in the host text field.
	 */
	void listServers()
	{
		boolean ok = true;
			
		selectedHost = whost.getText();
		lastRmiRegistryAddress = whost.getText();
			
		try
		{
			if(wport.getText().equals("")) 
				selectedPort = -1;
			else 
			{
				selectedPort = Integer.parseInt(wport.getText());
			}
			lastRmiRegistryPort = wport.getText();
		}
		catch(NumberFormatException e)
		{
			ok = false;
		}
			
		if(!ok)
		{
			GuiUtils.showErrorMessage(this, "Invalid port value!");
		}
		else
		{
			listModel.clear();

			try
			{
				java.util.List servers = null;
					
				if(!selectedHost.equals("") && selectedPort >= 0)
					servers = admin.listRemoteServers(selectedHost, selectedPort);
				else if(!selectedHost.equals("") && selectedPort < 0)
					servers = admin.listRemoteServers(selectedHost);
				else if(selectedHost.equals("") && selectedPort >= 0)
					servers = admin.listRemoteServers(selectedPort);
				else
					servers = admin.listRemoteServers();

				if(servers != null)
				{
					int i=0;
						
					for(i=0; i < servers.size(); i++)
					{
						listModel.addElement(servers.get(i));
					}
						
					if(i > 0)
						serverList.setSelectedIndex(0);
				}
					
				this.rootPane.setDefaultButton(this.connectToSelected);
					
				wizardLayout.show(wizardPanels, WIZARD_PAGE_2);
				serverList.requestFocus();
				this.currentWizardPage = WIZARD_PAGE_2;
			}
			catch(RemoteException e)
			{
				GuiUtils.showErrorMessage(this, "Unable to contact RMI registry at specified host and port (" + e + ")", "RMI faliure");
			}
			catch(Exception e)
			{
				GuiUtils.showErrorMessage(this, "Failed to list servers (" + e + ")");
			}
		}
	}
	
	/**
	 * Validates and saves the data for a wizard connect to a server and disposes the dialog box.
	 */
	void connectToSelected()
	{
		if(serverList.getSelectedIndex() >= 0)
		{
			String selString = (String)serverList.getSelectedValue();
				
			if(selString != null)
			{
				selectedServer = selString;
				lastServerName = selectedServer;	
					
				endOk = true;
				dispose();
			}
			else
			{
				GuiUtils.showErrorMessage(this, "No server selected", "Selection error");
			}
		}
		else
		{
			GuiUtils.showErrorMessage(this, "No server selected", "Selection error");
		}
	}

	/**
	 * Mouse click event handler method.
	 */
	public void mouseClicked(MouseEvent evt)
	{
		try
		{
			if(evt.getModifiers() == MouseEvent.BUTTON1_MASK && evt.getClickCount() == 2)
			{
				connectToSelected();
			}
		}
		catch(Exception e)
		{
		}
	}
	
	// MOUSE EVENTS
	public void mousePressed(MouseEvent e){}
	public void mouseReleased(MouseEvent e){}
	public void mouseEntered(MouseEvent evt){}
	public void mouseExited(MouseEvent evt)	{}
		
	/**
	 * Checks if the dialog box was closed ok.
	 */
	public boolean closedOk()
	{
		return endOk;	
	}
	
	/**
	 * Gets the selected host.
	 */
	public String getHost()
	{
		return selectedHost;	
	}
	
	/**
	 * Gets the selected port.
	 */
	public int getPort()
	{
		return selectedPort;
	}
	
	/**
	 * Gets the selected server name.
	 */
	public String getServer()
	{
		return selectedServer;	
	}
}
