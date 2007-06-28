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
package com.teletalk.jserver.periodic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.BevelBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.teletalk.jserver.rmi.client.CustomAdministrationPanel;

/**
 * Custom administration panel class for JAdmin.<br>
 * <br>
 * Note: Not currently in use.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.0
 * 
 * @since 1.3.2
 */
public final class ActionsInfoPanel extends CustomAdministrationPanel implements ActionListener, ListSelectionListener, Runnable
{
   static final long serialVersionUID = 1104448141193542254L;

	private RemotePeriodicActionManager remotePeriodicActionManager;
	
	private String[][] actions;
	
	private JList actionList;
	private DefaultListModel actionListData;
	private JTextArea actionDescription;
	private JScrollPane descriptionScroll;
	
	private JButton executeButton;
	//private JButton reloadButton;
	private boolean canRun = true;
	private Thread thread;
	
	/**
	 * Creates a new ActionsInfoPanel.
	 */
	public ActionsInfoPanel(RemotePeriodicActionManager remotePeriodicActionManager) throws Exception
	{
		super(new FlowLayout());
		
		this.remotePeriodicActionManager = remotePeriodicActionManager;
		
		this.actions = remotePeriodicActionManager.getActionDescriptions();
				
		JPanel mainPanel = new JPanel(new BorderLayout());
		mainPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
		
			JPanel actionsPanel = new JPanel();
			actionsPanel.setLayout(new BoxLayout(actionsPanel, BoxLayout.Y_AXIS));
			actionsPanel.setBorder(BorderFactory.createTitledBorder("Actions"));
				
				actionListData = new DefaultListModel();
				for(int i=0; i<this.actions.length; i++)
				{
					actionListData.addElement(this.actions[i]);
				}
				
				actionList = new JList(this.actionListData);
				actionList.setCellRenderer(new ActionListCellRenderer());
				actionList.setVisibleRowCount(5);
				actionList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				actionList.setPrototypeCellValue("TestServer 123456 TestServer 123456 TestServer 123456");
				actionList.addListSelectionListener(this);
 				JScrollPane listScroll = new JScrollPane(actionList);
				
				actionDescription = new JTextArea("", 9, 45);
				actionDescription.setFont(new Font("SansSerif", Font.PLAIN, 10));
				actionDescription.setLineWrap(false);
				actionDescription.setWrapStyleWord(false);
				actionDescription.setEditable(false);
				descriptionScroll = new JScrollPane(actionDescription);
				descriptionScroll.setBorder(BorderFactory.createTitledBorder("Description"));
			
				if(actions.length > 0) 
				{
					actionList.setSelectedIndex(0);
					actionDescription.setText(actions[0][2]);
					actionDescription.setCaretPosition(0);
					
					actionList.updateUI();
				}

			actionsPanel.add(Box.createRigidArea(new Dimension(475, 10)));
			actionsPanel.add(listScroll);
			actionsPanel.add(Box.createRigidArea(new Dimension(475, 10)));
			actionsPanel.add(descriptionScroll);
			
			
		mainPanel.add(Box.createHorizontalStrut(5), BorderLayout.EAST);
		mainPanel.add(Box.createHorizontalStrut(5), BorderLayout.WEST);
		mainPanel.add(Box.createVerticalStrut(5), BorderLayout.NORTH);
		mainPanel.add(Box.createVerticalStrut(5), BorderLayout.SOUTH);
		mainPanel.add(actionsPanel, BorderLayout.CENTER);
		
		JPanel borderPanel = new JPanel();
		borderPanel.setLayout(new BoxLayout(borderPanel, BoxLayout.Y_AXIS));
		
		borderPanel.add(Box.createVerticalGlue());
		
		JPanel p5 = new JPanel();
		p5.add(mainPanel);
		borderPanel.add(p5);
		borderPanel.add(Box.createVerticalStrut(5));
		
		JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		executeButton = new JButton("Execute");
		executeButton.addActionListener(this);
		southPanel.add(executeButton);
		
		borderPanel.add(southPanel);
		borderPanel.add(Box.createVerticalGlue());
				
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		
		add(borderPanel);
		
		thread = new Thread(this);
		thread.setDaemon(true);
		thread.start();
	}
	
	/**
	 * Gets the title of the panel.
	 */
	public String getTitle()
	{
		return "Actions";
	}
	
	/**
	 * Destroys the panel.
	 */
	protected void destroyPanel()
	{
		canRun = false;
		thread.interrupt();
	}
			
	/**
	 * Action event handler method.
	 */
	public void actionPerformed(ActionEvent event)
	{
		if(event.getSource() == this.executeButton)
		{
			try
			{
				int index = actionList.getSelectedIndex();
				if( (index < this.actions.length) && (index >= 0) )
				{
					if( JOptionPane.showConfirmDialog(this, "Are you sure you want to execute action '" + this.actions[index][0] + "'?", "Confirm", JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION )
					{
						this.remotePeriodicActionManager.executeAction(this.actions[index][0]);
					}
				}		
			}
			catch(Exception e)
			{
				JOptionPane.showMessageDialog(this, "Error while attempting to execute action (" + e + ")!", "Error", JOptionPane.ERROR_MESSAGE);
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Value changed handler method for the action list.
	 */
	public void valueChanged(ListSelectionEvent e)
	{
		synchronized(this.actions)
		{
			int index = actionList.getSelectedIndex();
			if( index < this.actions.length )
			{
				actionDescription.setText( this.actions[index][2] );
				actionDescription.setCaretPosition(0);
			}
		}
	}
	
	/**
	 * Cell renderer for the list cells.
	 */
	private class ActionListCellRenderer extends JPanel implements ListCellRenderer 
	{
      static final long serialVersionUID = 1001380332345404512L;
      
		private JLabel leftLabel;
		private JLabel rightLabel;
		
		/**
		 */
		public ActionListCellRenderer()
		{
			super();
			
			this.setLayout(new BorderLayout(0,0));

			Font f = new Font("SansSerif", Font.PLAIN, 10);
			
			this.leftLabel = new JLabel("");
			this.leftLabel.setOpaque(false);
			this.leftLabel.setForeground(Color.black);
			this.leftLabel.setFont(f);
			this.rightLabel = new JLabel("");
			this.rightLabel.setOpaque(false);
			this.rightLabel.setForeground(Color.black);
			this.rightLabel.setFont(f);
			
			JPanel leftPanel = new JPanel();
			leftPanel.setOpaque(false);
			leftPanel.setLayout(new FlowLayout(FlowLayout.LEFT, 0, 0));
			leftPanel.add(this.leftLabel);
			
			this.add(leftPanel, BorderLayout.CENTER);
			this.add(this.rightLabel, BorderLayout.EAST);
		}
		
		/**
		 */
		public Component getListCellRendererComponent(
													  JList list,
													  Object value,            // value to display
													  int index,               // cell index
													  boolean isSelected,      // is the cell selected
													  boolean cellHasFocus)    // the list and the cell have the focus
		{
			if( actions.length > 0 )
			{
				leftLabel.setText(actions[index][0]);
				rightLabel.setText(" " + actions[index][1] + " ");
			
				if (isSelected) 
				{
					setBackground(list.getSelectionBackground());
				}
				else 
				{
					setBackground(list.getBackground());
				}
			}
			return this;
		}
	}
	
	/**
	 * The famous thread method.
	 */
	public void run()
	{
		while(canRun)
		{
			try
			{
				Thread.sleep(1000);
								
				synchronized(this.actions)
				{
					final String[][] newActions = remotePeriodicActionManager.getActionDescriptions();
						
					boolean update = true;
					
					if( (newActions != null) && (this.actions != null) && (newActions.length == this.actions.length) )
					{
						update = false;
						for(int i=0; (i<newActions.length) && (i<this.actions.length); i++)
						{
							if( ( (newActions[i][0] == null) || !newActions[i][0].equals(this.actions[i][0])) ) // Different name
							{
								update = true;
								break;
							}
						}
					}
					
					this.actions = newActions;
					
					if(update)
					{
						this.actionListData.clear();
						if( this.actions != null )
						{
							for(int i=0; i<this.actions.length; i++)
							{
								this.actionListData.addElement(this.actions[i]);
							}
						}
					}
					this.actionList.updateUI();
				
					int index = actionList.getSelectedIndex();
					if( (index >= 0) && (index < this.actions.length) )
					{
						if( !this.actions[index][2].equals(actionDescription.getText()) )
						{
							actionDescription.setText( this.actions[index][2] );
							actionDescription.setCaretPosition(0);
						}
					}
				}
			}
			catch(Exception e)
			{
				if(!canRun) break;
			}
		}
	}
}
