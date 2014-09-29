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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import com.teletalk.jadmin.proxy.PropertyProxy;

/**
 * Class implementing the dialog box for editing / viewing a property.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class PropertyDialog extends JDialog implements ActionListener
{
	/**
    * 
    */
   private static final long serialVersionUID = 1L;
   private JButton ok;
	private JButton cancel;
	
	private InputPanel inputPanel;
	
	private PropertyProxy property;
		
	/**
	 * Creates a new PropertyDialog.
	 */
	public PropertyDialog(final Frame owner, final PropertyProxy property) throws Exception
	{
		super(owner, "Change property", true);
		
		this.property = property;
	
		JPanel mainPanel = (JPanel)this.getContentPane();
		mainPanel.setLayout(new BorderLayout(3,3));
		
		JPanel propertyPanel = new JPanel(new BorderLayout());
		propertyPanel.setBorder(BorderFactory.createEtchedBorder());
		
				String description = property.getDescription();
				
				if(property.isModifiable())
				{
					int type = property.getType();
				
					if(type == PropertyProxy.BOOLEAN_TYPE)
					{
						inputPanel = new BooleanInputPanel(this, property);
						if(description == null)
							description = "Boolean property";
					}
					else if(type == PropertyProxy.DATE_TYPE)
					{
						inputPanel = new StringInputPanel(this, property);
						if(description == null)
							description = "Date property";
					}
					else if(type == PropertyProxy.ENUM_TYPE)
					{
						inputPanel = new ChoiceInputPanel(this, property);
						if(description == null)
							description = "Enumeration property";
					}
					else if(type == PropertyProxy.NUMBER_TYPE)
					{
						inputPanel = new StringInputPanel(this, property);
						if(description == null)
							description = "Number property";
					}
					else if(type == PropertyProxy.MULTIVALUE_TYPE)
					{
						String delims = (String)property.getMetaData(PropertyProxy.DELIMETER_CHARS_KEY);

						if(delims != null) inputPanel = new MultiValueInputPanel(this, property, delims);
						else inputPanel = new StringInputPanel(this, property);
						
						if(description == null)
							description = "Multi value property";
					}
					else if(type == PropertyProxy.STRING_TYPE || type == PropertyProxy.CUSTOM_TYPE)
					{
						inputPanel = new StringInputPanel(this, property);
						if(description == null)
							description = "String property";
					}
					else
					{
						throw new RuntimeException("Invalid property type - " + type + "!");	
					}
				}
				else
				{
					inputPanel = new UnmodifiablePropertyPanel(this, property);
					if(description == null)
						description = "Unmodifiable property";
				}

				JPanel propertyInnerPanel = new JPanel();
				propertyInnerPanel.setLayout(new BoxLayout(propertyInnerPanel, BoxLayout.Y_AXIS));
				
				JPanel namePanel = new JPanel(new BorderLayout());
				namePanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Property:"));

				JPanel nameCompoundPanel = new JPanel();
				nameCompoundPanel.setLayout(new BoxLayout(nameCompoundPanel, BoxLayout.Y_AXIS));

					JPanel nameInnerPanel1 = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
						
						JTextField nameLabel = new JTextField(property.getName(), 25);
						nameLabel.setFont(new Font("SansSerif", Font.BOLD, 11));
						nameLabel.setForeground(Color.black);
						nameLabel.setScrollOffset(0);
						nameLabel.setBackground(this.getBackground());
						nameLabel.setEditable(false);
						nameLabel.setBorder(BorderFactory.createEmptyBorder());
						
						JLabel nameLabelLabel = new JLabel("Name: ");
				
					nameInnerPanel1.add(nameLabelLabel);
					nameInnerPanel1.add(Box.createHorizontalStrut(5));
					nameInnerPanel1.add(nameLabel);
					
					nameInnerPanel1.setAlignmentY(0.0f);
					
					JPanel nameInnerPanel2 = new JPanel(new BorderLayout());
					
						JPanel ownerTextPanel = new JPanel();
						ownerTextPanel.setLayout(new BoxLayout(ownerTextPanel, BoxLayout.Y_AXIS));
					
						JTextArea ownerText = new JTextArea(property.getParent().getFullName(), 2, 25);
						ownerText.setFont(new Font("SansSerif", Font.PLAIN, 11));
						ownerText.setForeground(Color.black);
						ownerText.setBackground(this.getBackground());
						ownerText.setLineWrap(true);
						ownerText.setWrapStyleWord(true);
						ownerText.setEditable(false);
						ownerText.setBorder(BorderFactory.createEmptyBorder());
					
						JScrollPane ownerScroll = new JScrollPane(ownerText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
						ownerScroll.setBorder(BorderFactory.createEmptyBorder());
						
						ownerTextPanel.add(ownerScroll);
						ownerTextPanel.add(Box.createGlue());
							
						JPanel ownerLabelPanel = new JPanel();
						ownerLabelPanel.setLayout(new BoxLayout(ownerLabelPanel, BoxLayout.Y_AXIS));
						JLabel ownerLabel = new JLabel("Owner: ");
						
						ownerLabelPanel.add(ownerLabel);
						ownerLabelPanel.add(Box.createGlue());
					
					nameInnerPanel2.add(ownerLabelPanel, BorderLayout.WEST);
					nameInnerPanel2.add(ownerTextPanel, BorderLayout.CENTER);
										
					nameInnerPanel2.setAlignmentY(0.0f);
					
				nameCompoundPanel.add(nameInnerPanel1);
				nameCompoundPanel.add(nameInnerPanel2);
						
				namePanel.add(Box.createHorizontalStrut(3), BorderLayout.EAST);
				namePanel.add(Box.createHorizontalStrut(3), BorderLayout.WEST);
				namePanel.add(nameCompoundPanel, BorderLayout.CENTER);
				
				JPanel descriptionPanel = new JPanel(new BorderLayout());
				
					JPanel descriptionTextPanel = new JPanel();
					descriptionTextPanel.setLayout(new BoxLayout(descriptionTextPanel, BoxLayout.Y_AXIS));
					
					JTextArea descriptionText = new JTextArea(description, 3, 40);
					descriptionText.setFont(new Font("SansSerif", Font.PLAIN, 11));
					descriptionText.setForeground(Color.black);
					descriptionText.setBackground(this.getBackground());
					descriptionText.setLineWrap(true);
					descriptionText.setWrapStyleWord(true);
					descriptionText.setEditable(false);
					descriptionText.setBorder(BorderFactory.createEmptyBorder());

					JScrollPane descriptionScroll = new JScrollPane(descriptionText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);

					descriptionScroll.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Description:"));
					descriptionScroll.setAlignmentY(1.0f);
				
					descriptionTextPanel.add(descriptionScroll);
					descriptionTextPanel.add(Box.createGlue());
					
					JPanel descriptionLabelPanel = new JPanel();
					descriptionLabelPanel.setLayout(new BoxLayout(descriptionLabelPanel, BoxLayout.Y_AXIS));
					JLabel descriptionLabel = new JLabel("Description:  ");
					
					descriptionLabelPanel.add(descriptionLabel);
					descriptionLabelPanel.add(Box.createGlue());

				descriptionPanel.add(descriptionTextPanel, BorderLayout.CENTER);
		
			JPanel inputBorderPanel = new JPanel(new BorderLayout());
			inputBorderPanel.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), "Value:"));
		
			inputBorderPanel.add(inputPanel, BorderLayout.CENTER);
			
			propertyInnerPanel.add(namePanel);
			propertyInnerPanel.add(inputBorderPanel);
			propertyInnerPanel.add(descriptionPanel);
									
		propertyPanel.add(Box.createVerticalStrut(5), BorderLayout.NORTH);
		propertyPanel.add(Box.createHorizontalStrut(5), BorderLayout.EAST);
		propertyPanel.add(Box.createHorizontalStrut(5), BorderLayout.WEST);
		propertyPanel.add(Box.createVerticalStrut(5), BorderLayout.SOUTH);
		propertyPanel.add(propertyInnerPanel, BorderLayout.CENTER);
		
		JPanel propertyPanel2 = new JPanel(new BorderLayout());
		
		propertyPanel2.add(Box.createVerticalStrut(5), BorderLayout.NORTH);
		propertyPanel2.add(Box.createHorizontalStrut(5), BorderLayout.EAST);
		propertyPanel2.add(Box.createHorizontalStrut(5), BorderLayout.WEST);
		propertyPanel2.add(Box.createVerticalStrut(5), BorderLayout.SOUTH);
		propertyPanel2.add(propertyPanel, BorderLayout.CENTER);
			
		JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		southPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		
		if(property.isModifiable())
		{
			ok = new JButton("  Ok  ");
			ok.addActionListener(this);
			southPanel.add(ok);
			
			cancel = new JButton("Cancel");
			cancel.addActionListener(this);
			southPanel.add(cancel);
		}
		else
		{
			ok = new JButton("Done");
			ok.addActionListener(this);
			southPanel.add(ok);	
		}
		
		mainPanel.add(propertyPanel2, BorderLayout.CENTER);
		mainPanel.add(southPanel, BorderLayout.SOUTH);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setResizable(false);
		
		pack();
	}
	
	/**
	 * Action event handler method.
	 */
	public void actionPerformed(final ActionEvent event)
	{
		if(event.getSource() == ok)
		{
			doOk();
		}
		else if(event.getSource() == cancel)
		{
			dispose();
		}
	}
	
	/**
	 * Perform ok button click logic.
	 */
	void doOk()
	{
		try
		{
			if(property.isModifiable())
				property.setPropertyValue(inputPanel.getValue());
			dispose();
		}
		catch(Exception e)
		{
			JOptionPane.showMessageDialog(this, "Unable to set property (" + e + ")", "Error", JOptionPane.ERROR_MESSAGE);
			e.printStackTrace();
		}
	}
	
	/*#################################################################
	######################### INPUT PANEL CLASSES #########################
	#################################################################*/
	
	/**
	 * Abstract base class for the classes implementing input/view GUI for different types of properties.
	 */
	private static abstract class InputPanel extends JPanel
	{
		private final Insets panelInsets = new Insets(5, 5, 5, 5);
		
		protected PropertyDialog propertyDialog;
		
		/**
		 * Creates a new InputPanel.
		 */
		public InputPanel(final PropertyDialog propertyDialog)
		{
			this.propertyDialog = propertyDialog;
		}
		
		/**
		 * Gets the insets of the panel.
		 */
		public final Insets getInsets()
		{
			return panelInsets;
		}
		
		/**
		 * Gets the new value.
		 */
		public abstract String getValue();
	}
	
	/*#################################################################
	###################### MULTI VALUE INPUT PANEL CLASS #####################
	#################################################################*/
	
	/**
	 * Class implementing a multi value input panel (for MultiValueProperty objects).
	 */
	private static class MultiValueInputPanel extends InputPanel implements ActionListener //implements MouseListener
	{
		/**
       * 
       */
      private static final long serialVersionUID = 1L;

      private static final int TABLE_WIDTH = 150;
		
		private JTable table;
		JScrollPane scrollPane;
		private MultiValueTableModel model;
		private String delims;
		
		private JButton addButton;
		private JButton deleteButton;
		private JTextField newValueField;
				
		/**
		 * Creates a new MultiValueInputPanel.
		 */
		public MultiValueInputPanel(final PropertyDialog propertyDialog, final PropertyProxy property, final String delims)
		{
			super(propertyDialog);
			
			this.setLayout(new BorderLayout());
			
			this.delims = delims;
			final ArrayList values = new ArrayList();
			
			// Values...
			final StringTokenizer tokenizer = new StringTokenizer(property.getValueAsString(), delims);
			
			try
			{
				while(tokenizer.hasMoreTokens())
				{
					values.add(tokenizer.nextToken());
				}
			}
			catch(Exception e)
			{
				e.printStackTrace();
			}
			
			//Font plainFont = new Font("SansSerif", Font.PLAIN, 10);
					
			// Panel
			JPanel mainPanel = new JPanel(new BorderLayout(5,5));
			
				model = new MultiValueTableModel(values, this);
				table = new JTable(model);
			
				table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
				table.getTableHeader().setReorderingAllowed(false);
		
				MultiValueCellRenderer cell0Renderer = new MultiValueCellRenderer();
				table.setDefaultRenderer(table.getColumnClass(0), cell0Renderer);
			
				scrollPane = new JScrollPane(table);
				table.setPreferredScrollableViewportSize(new Dimension(TABLE_WIDTH, 50));
												   
    			initColumnSizes();
				
				JPanel newValuePanel = new JPanel(new BorderLayout());
				newValuePanel.setBorder(BorderFactory.createEtchedBorder());
				
					JLabel newValueLabel = new JLabel(" New value: ");
					newValueLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
					newValueLabel.setForeground(Color.black);
					newValueField = new JTextField(20);
				
				newValuePanel.add(BorderLayout.WEST, newValueLabel);
				newValuePanel.add(BorderLayout.CENTER, newValueField);
				
			mainPanel.add(BorderLayout.CENTER, scrollPane);
			mainPanel.add(BorderLayout.SOUTH, newValuePanel);
			
			JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			
			addButton = new JButton(" Add  ");
			//addButton.setFont(plainFont);
			addButton.addActionListener(this);
			deleteButton = new JButton("Delete");
			//deleteButton.setFont(plainFont);
			deleteButton.addActionListener(this);
			
			southPanel.add(addButton);
			southPanel.add(Box.createHorizontalStrut(20));
			southPanel.add(deleteButton);
								
			add(BorderLayout.CENTER, mainPanel);
			add(BorderLayout.SOUTH, southPanel);
		}
		
		/**
		 * Gets the delimeter separated multivalue
		 */
		public String getValue()
		{
			java.util.List data = model.getValues();
			StringBuffer valueString = new StringBuffer();
			
			for(int i=0; i<data.size(); i++)
			{
				try
				{
					table.getCellEditor(i, 1).stopCellEditing(); // Stop cell editing in case the user edited a cell and didn't press return to stop editing...
				}
				catch(Exception e){}

				valueString.append(data.get(i));
				if(i < (data.size() - 1)) // If not last
					valueString.append(delims.charAt(0)); //Add first delim char
			}
			
			return valueString.toString();
		}
		
		/**
		 * Initializes the coulmn sizes used in the table on this panel.
		 */
		private void initColumnSizes() 		{
			TableColumn column = null;			Component comp = null;
			int headerWidth = 0;
			int cellWidth = 0;
			column = table.getColumnModel().getColumn(0);				
			comp = new JLabel(MultiValueTableModel.columnNames[0]);			headerWidth = comp.getPreferredSize().width;				
			comp = table.getDefaultRenderer(model.getColumnClass(0)).getTableCellRendererComponent(table, new Integer(12345), false, false, 0, 0);
			cellWidth = comp.getPreferredSize().width;
				
			int colum1Width = Math.max(headerWidth, cellWidth);
			column.setPreferredWidth((int)(colum1Width * 1.5));			column.setMaxWidth((int)(colum1Width * 2));
		}
		
		/**
		 * Scrolls to the bottom of the table.
		 */
		public void scrollToBottom()
		{
			Runnable r = new Runnable()
				{
					public void run()
					{
						scrollPane.getVerticalScrollBar().setValue(scrollPane.getVerticalScrollBar().getMaximum());
					}
				};
			
			SwingUtilities.invokeLater(r);
		}
		
		/**
		 * Action event handler method.
		 */
		public void actionPerformed(final ActionEvent event)
		{
			if(event.getSource() == addButton)
			{
				String newVal = this.newValueField.getText();
				
				if( (newVal != null) && !newVal.equalsIgnoreCase("") )
				{
					this.model.add(newVal);
				}
			}
			else if(event.getSource() == deleteButton)
			{
				int[] selectedRows = table.getSelectedRows();
				this.model.remove(selectedRows);
			}
		}
		
		/**
		 * The table model implementation.
		 */
		private static final class MultiValueTableModel extends AbstractTableModel
		{
			/**
          * 
          */
         private static final long serialVersionUID = 1L;
         public static String[] columnNames = {"Index", "Value"};
			private ArrayList data = null;
			private MultiValueInputPanel inputPanel;

			/**
			 * Creates a new MultiValueTableModel.
			 */
			public MultiValueTableModel(ArrayList data, MultiValueInputPanel inputPanel)
			{
				super();
				
				this.data = data;
				this.inputPanel = inputPanel;
			}
			
			/**
			 * Adds a new value to the model.
			 */
			public void add(final String value)
			{
				this.data.add(value);
				super.fireTableDataChanged();
				inputPanel.scrollToBottom();
			}
			
			/**
			 * Removes the values at the specified indices from the model.
			 */
			public void remove(final int[] indices)
			{
				ArrayList newData = (ArrayList)((ArrayList)data).clone();
				
				for(int i=0; i<indices.length; i++)
				{
					if( indices[i] < data.size() )
						newData.remove(data.get(indices[i]));
				}
				
				this.data = newData;
				
				super.fireTableDataChanged();
			}
			
			/**
			 * Gets the values in the model.
			 */
			public java.util.List getValues()
			{
				return data;
			}
			
			/**
			 * Gets the column count.
			 */
			public int getColumnCount() 
			{
				return columnNames.length;
		    }

			/**
			 * Gets the row count.
			 */
			public int getRowCount() 
			{
				if(data!=null) return data.size(); // + 1; // +1 for new input
				else return 0;
		    }

		    /**
		     * Gets the name of the column at the specified index.
		     */
			public String getColumnName(final int index) 
			{
				return columnNames[index];
		    }
			
			/**
			 * Checks if the specified cell is editable.
			 */
			public boolean isCellEditable(int row, int col)
			{ 
				if(col == 0) return false;
				else return true; 
			}

		    /**
		     * Gets the value for the specified cell.
		     */
			public Object getValueAt(int row, int col) 
			{
				try
				{
					if(col == 0) return new Integer(row);
					else if(row < data.size())
					{
						if(col == 1) return data.get(row);
						else return null;
					}
					else return null;
				}
				catch(ArrayIndexOutOfBoundsException e)
				{
					return null;
				}
		    }
			
			/**
			 * Sets the value in the specified cell.
			 */
			public void setValueAt(Object aValue, int rowIndex, int columnIndex)
			{
				if(columnIndex == 1)
				{
					if(rowIndex < data.size())
					{
						data.set(rowIndex, aValue);
						super.fireTableDataChanged();
					}
				}
			}
		}
	
		/**
		 * Table cell renderer class.
		 */
		private static final class MultiValueCellRenderer extends DefaultTableCellRenderer
		{
			/**
          * 
          */
         private static final long serialVersionUID = 1L;

         /**
			 * Creates a new MultiValueCellRenderer.
			 */
			public MultiValueCellRenderer()
			{
				super();
			}

			/**
			 * Gets the renderer component for the specified cell.
			 */
			public Component getTableCellRendererComponent(final JTable table, Object value, final boolean isSelected, boolean hasFocus, int row, int column)
			{
				if(column == 0)
				{
					JLabel label = new JLabel(value.toString());
					
					label.setOpaque(true);
					label.setBackground(Color.lightGray);
					label.setForeground(Color.black);
										
					label.setHorizontalAlignment(JLabel.CENTER);
					
					return label;
				}
				else
				{
					return super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
				}
			}
		}

	}
	
	/*#################################################################
	######################## CHOICE INPUT PANEL CLASS ######################
	#################################################################*/
	
	/**
	 * Input panel class for enum/multiple choice values.
	 */
	private static final class ChoiceInputPanel extends InputPanel
	{
		/**
       * 
       */
      private static final long serialVersionUID = 1L;
      private final JComboBox enums;
		
		/**
		 * Creates a new ChoiceInputPanel.
		 */
		public ChoiceInputPanel(final PropertyDialog propertyDialog, final PropertyProxy property)
		{
			super(propertyDialog);
			
			this.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 3));
				
			Object[] enumObjects = property.getEnumerations();
			
			if(enumObjects == null)
				throw new RuntimeException("Unable to initialize EnumProperty - failed to get enumeration objects!");	
			
			int selIndex = -1;
			String selString = property.getValueAsString();
			
			for(int i=0; i<enumObjects.length; i++)
			{
				if(enumObjects[i].toString().equals(selString))
				{
					selIndex = i;
					break;
				}
			}
			
			if(selIndex < 0)
				throw new RuntimeException("Unable to initialize EnumProperty - failed to get selected index!");	
			
			enums = new JComboBox(enumObjects);
			enums.setMaximumRowCount(3);
			enums.setSelectedIndex(selIndex);

			add(enums);
		}
		
		/**
		 * Gets the new value.
		 */
		public String getValue()
		{
			return enums.getSelectedItem().toString();
		}
	}
	
	/*#################################################################
	######################## BOOLEAN INPUT PANEL CLASS ######################
	#################################################################*/
	
	/**
	 * Input panel class for input of boolean values.
	 */
	private static final class BooleanInputPanel extends InputPanel
	{
		/**
       * 
       */
      private static final long serialVersionUID = 1L;
      private final ButtonGroup bg;
		private final JRadioButton trueButton;
		private final JRadioButton falseButton;
		
		/**
		 * Creates BooleanInputPanel.
		 */
		public BooleanInputPanel(final PropertyDialog propertyDialog, final PropertyProxy property)
		{
			super(propertyDialog);
			
			this.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 3));
			bg = new ButtonGroup();
			
			boolean isTrue = false;
			
			isTrue = property.getValueAsString().equalsIgnoreCase("true");
			
			trueButton = new JRadioButton("true", isTrue);
			falseButton = new JRadioButton("false", !isTrue);
			trueButton.setFocusPainted(false);
			falseButton.setFocusPainted(false);
						
			bg.add(trueButton);
			bg.add(falseButton);

			add(trueButton);
			add(falseButton);
		}
		
		/**
		 * Gets the new value.
		 */
		public String getValue()
		{
			if(trueButton.isSelected())
				return "true";
			else
				return "false";
		}
	}
	
	/*#################################################################
	######################## STRING INPUT PANEL CLASS ######################
	#################################################################*/
	
	/**
	 * Input panel class for string values.
	 */
	private static final class StringInputPanel extends InputPanel implements KeyListener
	{
		/**
       * 
       */
      private static final long serialVersionUID = 1L;
      private final JTextArea value;
		
		public StringInputPanel(final PropertyDialog propertyDialog, final PropertyProxy property)
		{
			super(propertyDialog);
			
			this.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 3));
			
			value = new JTextArea(property.getValueAsString(), 1, 35);
			value.setFont(new Font("SansSerif", Font.PLAIN, 11));
								
			JScrollPane scroll = new JScrollPane(value, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			
			add(scroll);
			
			this.addKeyListener(this);
			value.addKeyListener(this);
			scroll.addKeyListener(this);
		}
		
		/**
		 * Gets the new value.
		 */
		public String getValue()
		{
			return value.getText();
		}
		
		/**
		 * Key press event handling method.
		 */
		public void keyPressed(final KeyEvent e)
		{
			if(e.getKeyCode() == KeyEvent.VK_ENTER)
			{
				super.propertyDialog.doOk();
				e.consume();
			}
		}

		public void keyReleased(KeyEvent e){}
		public void keyTyped(KeyEvent e){}
	}
	
	/*#################################################################
	################# UNMODIFIABLE PROPERTY INPUT PANEL CLASS ##################
	#################################################################*/
	
	/**
	 * Input panel class for unmodifiable properties.
	 */
	private static final class UnmodifiablePropertyPanel extends InputPanel
	{
		/**
       * 
       */
      private static final long serialVersionUID = 1L;
      private final JTextArea value;
		
		/**
		 * Creates a new UnmodifiablePropertyPanel.
		 */
		public UnmodifiablePropertyPanel(final PropertyDialog propertyDialog, final PropertyProxy property)
		{
			super(propertyDialog);
			
			this.setLayout(new FlowLayout(FlowLayout.CENTER, 3, 3));
			
			value = new JTextArea(property.getValueAsString(), 1, 35);
			value.setFont(new Font("SansSerif", Font.PLAIN, 11));
			value.setEditable(false);
								
			JScrollPane scroll = new JScrollPane(value, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
			
			add(scroll);
		}
		
		/**
		 * Gets the new value.
		 */
		public String getValue()
		{
			return value.getText();
		}
	}
}
