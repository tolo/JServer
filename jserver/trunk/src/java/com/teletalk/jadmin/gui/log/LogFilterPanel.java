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
import java.awt.Frame;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.BevelBorder;

import com.teletalk.jserver.log.LogFilter;
import com.teletalk.jserver.util.Constraint;

/**
 * Class responsible for the panel when log filter rules are displayed and handled.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class LogFilterPanel extends JPanel implements ActionListener
{
	/**
    * 
    */
   private static final long serialVersionUID = 1L;

   private final LogPanel logPanel;
	
	private final JList filterList;
	private final DefaultListModel listModel;
	
	private final JButton addB;
	private final JButton removeB;
	
	private final HashMap constraints;
	private final HashMap fields;
	private final LogFilter filter;

	/**
	 * Creates a new LogFilterPanel.
	 */	
	public LogFilterPanel(final LogPanel logPanel)
	{
		super(new BorderLayout());
		
		this.logPanel = logPanel;
		
		constraints = new HashMap();
		fields = new HashMap();
		filter = logPanel.getLogFilter();//new LogFilter();
		
		final JPanel listPanel = new JPanel(new BorderLayout());
		listPanel.setBorder(BorderFactory.createTitledBorder("View logmessages where..."));
			
		listModel = new DefaultListModel();
		filterList = new JList(listModel);
		filterList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		filterList.setPrototypeCellValue("If field 'Origin' doesn't contain 'FinkelfantomFinkelfantomFinkelfantom'");
		filterList.setFont(new Font("SansSerif", Font.PLAIN, 10));
 		JScrollPane scrollPane = new JScrollPane(filterList);
		
		JPanel rigthPanel = new JPanel(new BorderLayout());
		rigthPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
			
			JPanel rigthInnerPanel = new JPanel();
			rigthInnerPanel.setLayout(new BoxLayout(rigthInnerPanel, BoxLayout.Y_AXIS));
		
			addB = new JButton("Add");
			addB.addActionListener(this);
			addB.setAlignmentX(CENTER_ALIGNMENT);
			removeB = new JButton("Remove");
			removeB.addActionListener(this);
			removeB.setAlignmentX(CENTER_ALIGNMENT);
			
			rigthInnerPanel.add(addB);
			rigthInnerPanel.add(Box.createRigidArea(new Dimension(0, 5)));
			rigthInnerPanel.add(removeB);
			
		rigthPanel.add(BorderLayout.WEST, Box.createHorizontalStrut(5));
		rigthPanel.add(BorderLayout.EAST, Box.createHorizontalStrut(5));
		rigthPanel.add(BorderLayout.SOUTH, Box.createVerticalStrut(5));
		rigthPanel.add(BorderLayout.NORTH, Box.createVerticalStrut(5));
		rigthPanel.add(BorderLayout.CENTER, rigthInnerPanel);
		
		listPanel.add(BorderLayout.CENTER, scrollPane);
		listPanel.add(BorderLayout.EAST, rigthPanel);
		
		add(BorderLayout.WEST, Box.createHorizontalStrut(10));
		add(BorderLayout.EAST, Box.createHorizontalStrut(10));
		add(BorderLayout.SOUTH, Box.createVerticalStrut(10));
		add(BorderLayout.NORTH, Box.createVerticalStrut(10));
		add(BorderLayout.CENTER, listPanel);
	}
	
	/**
	 * Action event handler method.
	 */
	public void actionPerformed(ActionEvent event)
	{
		if(event.getSource() == addB)
		{
			Frame owner = JFrame.getFrames()[0];
			FilterDialog dlg = new FilterDialog(owner);
						Dimension dlgSize = dlg.getPreferredSize();
			Dimension frmSize = owner.getSize();
			Point loc = owner.getLocation();
			dlg.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
			
			dlg.setVisible(true);
			
			if(dlg.closedOk())
			{
				String constraintString = dlg.getFilterString();
				
				if(!constraints.containsKey(constraintString))
				{
					Constraint constraint = dlg.getConstraint();
					String field = dlg.getField();
					
					constraints.put(constraintString, constraint);
					fields.put(constraintString, field);
					filter.addConstraint(field, constraint);
					
					listModel.addElement(constraintString);
					
					logPanel.logFilterChanged(filter);
				}
			}
		}
		else if(event.getSource() == removeB)
		{
			if(filterList.getSelectedIndex() >= 0)
			{
				int selIndex = filterList.getSelectedIndex();
				String selString = (String)filterList.getSelectedValue();
				
				if(selString != null)
				{
					Constraint constraint = (Constraint)constraints.remove(selString);
					String field = (String)fields.remove(selString);
					filter.removeConstraint(field, constraint);
					
					listModel.remove(selIndex);
					
					logPanel.logFilterChanged(filter);
				}
			}
		}
	}
}
