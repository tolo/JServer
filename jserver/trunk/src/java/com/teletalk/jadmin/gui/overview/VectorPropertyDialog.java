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
package com.teletalk.jadmin.gui.overview;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputMethodEvent;
import java.awt.event.InputMethodListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;

import com.teletalk.jadmin.gui.util.GuiUtils;
import com.teletalk.jadmin.proxy.VectorPropertyProxy;
import com.teletalk.jadmin.proxy.VectorPropertyProxyListener;

/**
 * Class implementing the dialog box for viewing the contents of a VectorProperty.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class VectorPropertyDialog extends JDialog implements MouseListener, ActionListener, WindowListener, InputMethodListener, VectorPropertyProxyListener
{
	/**
    * 
    */
   private static final long serialVersionUID = 1L;
   private static final Font plainFont = new Font("SansSerif", Font.PLAIN, 11);
	private static final Font boldFont = new Font("SansSerif", Font.BOLD, 11);
	
	private final JButton done;
	
	private final VectorPropertyProxy vProxy;
	
	final JList itemsList;
	final DefaultListModel listModel;
	
	private final HashMap operationButtons;
	private final JPanel operationButtonsPanel;
	
	private VectorPropertyItemDialog vectorPropertyItemDialog = null;
	
	private String[][] items;
	
	private final ButtonGroup bg;
	private final JRadioButton autoButton;
	private final JRadioButton manualButton;
	
	private final CardLayout updateExtraCardLayout;
	private final JPanel updateExtraCardPanel;
	private final JPanel autoUpdatePanel;
	private final JPanel manualUpdatePanel;
	private final JTextField updateInterval;
	private final JButton updateButton;
	private final JButton setButton;
	
	private final Frame ownerFrame;
	
	/**
	 * Creates a new VectorPropertyDialog.
	 */
	public VectorPropertyDialog(final Frame owner, final VectorPropertyProxy vProxy)
	{
		super(owner, vProxy.getName(), true);
		
		this.ownerFrame = owner;
		
		operationButtons = new HashMap();
		this.vProxy = vProxy;
				
		JPanel mainPanel = (JPanel)this.getContentPane();
		mainPanel.setLayout(new BorderLayout());

			JPanel topPanel = new JPanel(new FlowLayout(FlowLayout.LEFT))
				{
					/**
                * 
                */
               private static final long serialVersionUID = 1L;

               public Insets getInsets()
					{
						return new Insets(2,2,2,2);
					}
				};
			topPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
			
				//Autorefresh på/av, manuell refresh
				JLabel updateLabel = new JLabel("Update mode:");
				
				bg = new ButtonGroup();
				
				boolean autoUpdate = VectorPropertyProxy.isAutoUpdateEnabled();
				
				autoButton = new JRadioButton("Auto", autoUpdate);
				manualButton = new JRadioButton("Manual", !autoUpdate);
				autoButton.setFocusPainted(false);
				manualButton.setFocusPainted(false);
				
				autoButton.addActionListener(this);
				manualButton.addActionListener(this);
						
				bg.add(autoButton);
				bg.add(manualButton);
				
				//Cardlayout
				updateExtraCardLayout = new CardLayout();
				updateExtraCardPanel = new JPanel(updateExtraCardLayout);
					
				autoUpdatePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
				
				updateInterval = new JTextField(String.valueOf(VectorPropertyProxy.getUpdateInterval()), 7);
				updateInterval.addInputMethodListener(this);
				
				setButton = new JButton("Set");
				setButton.addActionListener(this);
				
				autoUpdatePanel.add(new JLabel("Update interval:"));
				autoUpdatePanel.add(updateInterval);
				autoUpdatePanel.add(setButton);
			
				manualUpdatePanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
				
				updateButton = new JButton("Update");
				updateButton.addActionListener(this);
				
				manualUpdatePanel.add(updateButton);
			
				updateExtraCardLayout.addLayoutComponent(autoUpdatePanel, "A");
				updateExtraCardLayout.addLayoutComponent(manualUpdatePanel, "M");
		
				updateExtraCardPanel.add(autoUpdatePanel, "A");
				updateExtraCardPanel.add(manualUpdatePanel, "M");
		
				if(autoUpdate)
					updateExtraCardLayout.show(updateExtraCardPanel, "A");
				else
					updateExtraCardLayout.show(updateExtraCardPanel, "M");
			
			topPanel.add(updateLabel);//, BorderLayout.CENTER);
			topPanel.add(autoButton);
			topPanel.add(manualButton);
			topPanel.add(updateExtraCardPanel);
			
			JPanel mainCenterPanel = new JPanel(new BorderLayout(3,3));
		
				JPanel operationsPanel = new JPanel(new BorderLayout());
				
					operationButtonsPanel = new JPanel()
						{
							/**
                      * 
                      */
                     private static final long serialVersionUID = 1L;

                     public Insets getInsets()
							{
								return new Insets(5,5,5,5);
							}
						};
					operationButtonsPanel.setLayout(new BoxLayout(operationButtonsPanel, BoxLayout.Y_AXIS));
					operationButtonsPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
				
					//opLabel = new JLabel("Operations:");
					//opLabel.setAlignmentX(0.5f);
					//opLabel.setBorder(BorderFactory.createEtchedBorder());
				
					//operationButtonsPanel.add(opLabel);
					
					JScrollPane opScroll = new JScrollPane(operationButtonsPanel, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
					opScroll.setMinimumSize(new Dimension(100, opScroll.getMinimumSize().height));
					
				operationsPanel.add(new JLabel(" Operations:"), BorderLayout.NORTH);
				operationsPanel.add(opScroll, BorderLayout.CENTER);
				
				//JPanel itemsPanel = new JPanel(new BorderLayout());
		
					listModel = new DefaultListModel();
					itemsList = new JList(listModel);
					itemsList.setVisibleRowCount(6);
					itemsList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
					itemsList.setPrototypeCellValue("TestServer 123456 TestServer 123456 TestServer 123456 TestServer");
					itemsList.addMouseListener(this);
					itemsList.setFont(new Font("SansSerif", Font.PLAIN, 10));
					
					JScrollPane listScroll = new JScrollPane(itemsList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);

			mainCenterPanel.add(operationsPanel, BorderLayout.WEST);
			mainCenterPanel.add(listScroll, BorderLayout.CENTER);
		
			JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			southPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		
			done = new JButton("Done");
			done.addActionListener(this);
		
			southPanel.add(done);
			
		mainPanel.add(topPanel, BorderLayout.NORTH);
		mainPanel.add(mainCenterPanel, BorderLayout.CENTER);
		mainPanel.add(southPanel, BorderLayout.SOUTH);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			
		pack();
		//setSize(600, 450);
	
		synchronized(vProxy)
		{
			vProxy.registerVectorPropertyProxyListener(this);
			vProxy.forceUpdate();
		}
		addWindowListener(this);
	}
	
	/**
	 * Disposes this dialog box.
	 */
	public void dispose()
	{
		if(vectorPropertyItemDialog != null)
		{
			vectorPropertyItemDialog.dispose();
			vectorPropertyItemDialog = null;
		}
		super.dispose();
	}
	
	/**
	 * Action event handler method.
	 */
	public void actionPerformed(ActionEvent event)
	{
		Object source = event.getSource();
		
		if(source == done)
		{
			dispose();
		}
		else if(source == autoButton)
		{
			VectorPropertyProxy.setAutoUpdateEnabled(true);
			updateExtraCardLayout.show(updateExtraCardPanel, "A");
		}
		else if(source == manualButton)
		{
			VectorPropertyProxy.setAutoUpdateEnabled(false);
			updateExtraCardLayout.show(updateExtraCardPanel, "M");
		}
		else if(source == updateButton)
		{
			try
			{
				vProxy.forceUpdate();
			}
			catch(Exception e)
			{
				GuiUtils.showErrorMessage("Error while updating! (" + e + ")");
			}
		}
		else if(source == setButton)
		{
			try
			{
				updateInterval.setBackground(Color.white);
				VectorPropertyProxy.setUpdateInterval(Long.parseLong(updateInterval.getText()));
			}
			catch(Exception e)
			{
				GuiUtils.showErrorMessage("Unacceptable value for update interval");
			}
		}
		else
		{
			for(Enumeration e = Collections.enumeration(operationButtons.keySet()); e.hasMoreElements();)
			{
				String internalName = (String)e.nextElement();
				JButton b = (JButton)operationButtons.get(internalName);
				
				if(b == source)
				{
					String[] selKeys = getSelectedItemKeys();
					
					if(selKeys.length > 0)
					{
						if( JOptionPane.showConfirmDialog(this, "Are you sure you want to perform the operation '" + b.getText() + "' on the selected " + 
																			 ( (selKeys.length == 1) ? "item" : (selKeys.length + " items") ) + "?", "Confirm operation", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) 
																				== JOptionPane.YES_OPTION )
						{
							try
							{
								if(selKeys != null && (selKeys.length > 0))
									vProxy.externalOperationCalled(internalName, selKeys);
							}
							catch(Exception ex)
							{
								GuiUtils.showErrorMessage("Error executing selected operation on VectorProperty! (" + ex + ")");	
							}
						}
					}
				}
			}
		}
	}
	
	/**
	 * Gets the keys for the selected items.
	 */
	public synchronized String[] getSelectedItemKeys()
	{
		int[] selIndices = itemsList.getSelectedIndices();
		String[] selKeys = new String[selIndices.length];
		
		for(int i=0; i<selIndices.length; i++)
		{
			selKeys[i] = items[ selIndices[i] ][0];
		}
		
		return selKeys;
	}
	
		
	/**
	 * Text input change event handler method for the update interval input field.
	 */
	public void inputMethodTextChanged(final InputMethodEvent event)
	{
		updateInterval.setBackground(new Color(255, 128, 128));
	}
	
	public void caretPositionChanged(InputMethodEvent event){}

	/**
	 * Window close event handler method.
	 */
	public void windowClosed(WindowEvent e)
	{
		vProxy.unregisterVectorPropertyProxyListener(this);
	}

	public void windowActivated(WindowEvent e){}
	public void windowClosing(WindowEvent e){}
	public void windowDeactivated(WindowEvent e){}
	public void windowDeiconified(WindowEvent e){}
	public void windowIconified(WindowEvent e){}
	public void windowOpened(WindowEvent e){}
	
	/**
	 * Gets the index of the specified key.
	 */
	int findIndexOf(final String key)
	{
		for(int i=0; i<listModel.size(); i++)
		{
			if( ((String)listModel.get(i)).startsWith(key) ) return i;
		}
		
		return -1;
	}
	
	/**
	 * Makes a string of the specified key and description.
	 */
	String itemToString(final String key, final String description)
	{
		return key + " : " + description;
	}
	
	/**
	 * Called when items are added to the VectorProperty.
	 */
	public void itemAdded(final String[] keys, final String[] descriptions)
	{
		Runnable task = new Runnable()
			{
				public void run()
				{
					synchronized(listModel)
					{
						listModel.ensureCapacity(listModel.size() + keys.length);
		
						for(int i=0; i<keys.length; i++)
						{
							listModel.addElement(itemToString(keys[i], descriptions[i]));
						};
					}
				}
			};
			
		//Make the event-dispatching thread (in Swing/AWT) execute the code implemented in task
		SwingUtilities.invokeLater(task);
	}
	
	/**
	 * Called when items are removed from the VectorProperty.
	 */
	public void itemRemoved(final String[] keys, final String[] descriptions)
	{
		Runnable task = new Runnable()
			{
				public void run()
				{
					synchronized(listModel)
					{
						int index;
		
						for(int i=0; i<keys.length; i++)
						{
							index = findIndexOf(keys[i]);
		
							if(index >= 0)
							{
								listModel.remove(index);
							}
						}
					}
				}
			};
			
		//Make the event-dispatching thread (in Swing/AWT) execute the code implemented in task
		SwingUtilities.invokeLater(task);
	}
	
	/**
	 * Called when items have been modified in the VectorProperty.
	 */
	public synchronized void itemModified(final String[] keys, final String[] descriptions)
	{
		Runnable task = new Runnable()
			{
				public void run()
				{
					synchronized(listModel)
					{
						int index;
		
						for(int i=0; i<keys.length; i++)
						{
							index = findIndexOf(keys[i]);
		
							if(index >= 0)
							{
								listModel.set(index, itemToString(keys[i], descriptions[i]));
							}
						}
					}
				}
			};
			
		//Make the event-dispatching thread (in Swing/AWT) execute the code implemented in task
		SwingUtilities.invokeLater(task);
	}
	
	/**
	 * Clears all items from the item list.
	 */
	public synchronized void clearItems()
	{
		Runnable task = new Runnable()
			{
				public void run()
				{
					synchronized(listModel)
					{
						listModel.clear();
					}
				}
			};
			
		//Make the event-dispatching thread (in Swing/AWT) execute the code implemented in task
		SwingUtilities.invokeLater(task);
	}
	
	/**
	 * Calles when the entiere item list should be updated.
	 */
	public synchronized void itemsUpdated(final String[][] items)
	{
		this.items = items;
		
		Runnable task = new Runnable()
			{
				public void run()
				{
					synchronized(listModel)
					{
						listModel.clear();
						String protoTypeString = "";
						String tmpStr;
		
						for(int i=0; i<items.length; i++)
						{
							tmpStr = itemToString(items[i][0], items[i][1]); //items[i][0] + " : " + items[i][1];
							listModel.addElement(tmpStr);
							if(tmpStr.length() > protoTypeString.length()) protoTypeString = tmpStr;
						}
		
						if(protoTypeString != null) itemsList.setPrototypeCellValue(protoTypeString + "          ");
					}
				}
			};
			
		//Make the event-dispatching thread (in Swing/AWT) execute the code implemented in task
		SwingUtilities.invokeLater(task);
	}
	
	/**
	 * Called when the VectorPropery operations have been updated from server.
	 */
	public void operationsUpdated(final HashMap operations)
	{
		operationButtonsPanel.removeAll();
		operationButtons.clear();
		
		operationButtonsPanel.add(Box.createVerticalStrut(5));
		
		// Sort operations according to display name
		final ArrayList displayNames = new ArrayList();
		final HashMap displayNameToOperation = new HashMap();
		String displayName;
		for(Enumeration e = Collections.enumeration(operations.keySet()); e.hasMoreElements();)
		{
			String internalName = (String)e.nextElement();
			displayName = (String)operations.get(internalName);
			displayNames.add(displayName);
			displayNameToOperation.put(displayName, internalName);
		}

		Collections.sort(displayNames);
		
		String operationName;
		int maxWidth = 100;
		
		for(int i=0; i<displayNames.size(); i++)
		{
			displayName = (String)displayNames.get(i);
			operationName = (String)displayNameToOperation.get(displayName);
			
			if(operationName != null)
			{
				JButton b = new JButton(displayName);
				b.setBorderPainted(false);
				b.setFocusPainted(false);
				b.setContentAreaFilled(false);
				b.setAlignmentX(0.5f);
				b.addActionListener(this);
				b.addMouseListener(this);
				b.setFont(boldFont);
				b.setForeground(Color.black);
				b.setMargin(new Insets(2,2,2,2));
				operationButtons.put(operationName, b);
				operationButtonsPanel.add(b);
				operationButtonsPanel.add(Box.createVerticalStrut(5));
				
				if(b.getPreferredSize().width > maxWidth)
				{
					maxWidth = b.getPreferredSize().width;
				}
				
				b.setFont(plainFont);
			}
		}

		operationButtonsPanel.add(Box.createHorizontalStrut(maxWidth));
		
		operationButtonsPanel.validate();
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
				int index = itemsList.getSelectedIndex();
				
				if( (index >= 0) && (index < items.length) )
				{
					if(vectorPropertyItemDialog != null)
					{
						vectorPropertyItemDialog.dispose();
						vectorPropertyItemDialog = null;
					}
					
					vectorPropertyItemDialog = new VectorPropertyItemDialog(ownerFrame, items[index][0], items[index][1]);
				
					Dimension dlgSize = vectorPropertyItemDialog.getPreferredSize();
					Dimension frmSize = ownerFrame.getSize();
					Point loc = ownerFrame.getLocation();
					vectorPropertyItemDialog.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
					vectorPropertyItemDialog.setSize(dlgSize);
					vectorPropertyItemDialog.setVisible(true);
					
					vectorPropertyItemDialog = null;
				}
			}
		}
		catch(Exception e)
		{
		}
	}
	
	/**
	 * Mouse press envent handler method.
	 */
	public void mousePressed(MouseEvent evt)
	{
		Object source = evt.getSource();
		
		for(Enumeration e = Collections.enumeration(operationButtons.keySet()); e.hasMoreElements();)
		{
			String internalName = (String)e.nextElement();
			JButton b = (JButton)operationButtons.get(internalName);
				
			if(b != null)
			{
				if(b == source)
				{
					b.setFont(boldFont);
					b.setForeground(Color.red);
				}
				else
				{
					if(b.getFont() != plainFont)
					{
						b.setFont(plainFont);
					}
				}
			}
		}
	}
	
	/**
	 * Mouse release event handler method.
	 */
	public void mouseReleased(MouseEvent evt)
	{
		for(Enumeration e = Collections.enumeration(operationButtons.keySet()); e.hasMoreElements();)
		{
			String internalName = (String)e.nextElement();
			JButton b = (JButton)operationButtons.get(internalName);
				
			if(b != null)
			{
				if(b.getForeground().equals(Color.red))
				{
					b.setFont(boldFont);
					b.setForeground(Color.black);
				}
			}
		}
	}
	
	/**
	 * Mouse entered event handler method.
	 */
	public void mouseEntered(MouseEvent evt)
	{
		Object source = evt.getSource();
		
		for(Enumeration e = Collections.enumeration(operationButtons.keySet()); e.hasMoreElements();)
		{
			String internalName = (String)e.nextElement();
			JButton b = (JButton)operationButtons.get(internalName);
				
			if(b != null)
			{
				if(b == source)
				{
					b.setFont(boldFont);
				}
				else
				{
					if(b.getFont() != plainFont)
					{
						b.setFont(plainFont);
					}
				}
			}
		}
	}
	
	/**
	 * Mouse exited event handler method.
	 */
	public void mouseExited(MouseEvent evt)
	{
		for(Enumeration e = Collections.enumeration(operationButtons.keySet()); e.hasMoreElements();)
		{
			String internalName = (String)e.nextElement();
			JButton b = (JButton)operationButtons.get(internalName);

			if(b != null)
			{
				if(b.getFont() != plainFont)
				{
					b.setFont(plainFont);
				}
			}
		}
	}
}
