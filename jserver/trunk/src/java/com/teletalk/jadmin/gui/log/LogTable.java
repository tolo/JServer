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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Graphics;
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import com.teletalk.jadmin.gui.util.GuiUtils;
import com.teletalk.jadmin.gui.util.ResourceHandler;
import com.teletalk.jserver.log.LogMessage;

/**
 * This class implements a table for displaying log messages.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public final class LogTable extends JPanel implements MouseListener
{
	/**
    * 
    */
   private static final long serialVersionUID = 1L;
   private final JTable table;
	private final LogTableModel model;
	
	private LogMessageDialog logMessageDialog = null;
	
	public boolean enabled = false;
	
	/** 
	 * Creates a new LogTable.
	 */
	public LogTable()
	{
		super(new BorderLayout());
		
		setBorder(GuiUtils.createTitleCompoundBevelBorder(""));
		
		model = new LogTableModel();
		table = new JTable(model);

		//table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
				
		table.setShowHorizontalLines(false);
		table.setShowVerticalLines(false);
		
		LogLevelCellRenderer cell0Renderer = new LogLevelCellRenderer();
		table.setDefaultRenderer(table.getColumnClass(0), cell0Renderer);
    
		initColumnSizes();
		
		JScrollPane scrollPane = new JScrollPane(table);
		
		add(BorderLayout.CENTER, scrollPane);
		
		table.addMouseListener(this);
		
		table.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
	}

	/**
	 * Initializes the column sizes of the table.
	 */	
	private void initColumnSizes() 	{
		//GetTableHeader...se ändringar till v 1.3
		TableColumn column = null;		//JTableHeader header;
		/*Component comp = null;
		int headerWidth = 0;
		int cellWidth = 0;
		LogMessage dummy = new LogMessage(LogMessage.INFO_LEVEL, new Date(), "SubSystem Test", "Jag är en lite kaffekanna!");		Object[] dummyValues = {dummy, dummy.getTimeString(), dummy.origin, dummy.msg};*/					  
		for(int i=0; i<4; i++)
		{			column = table.getColumnModel().getColumn(i);

			/* Genererar nullpointerexception i Java 2 v 1.3			comp = column.getHeaderRenderer().getTableCellRendererComponent(null, column.getHeaderValue(), false, false, 0, i);
			headerWidth = comp.getPreferredSize().width;
			*/			/*comp = new JLabel(LogTableModel.columnNames[i]);			headerWidth = comp.getPreferredSize().width;
			comp = table.getDefaultRenderer(model.getColumnClass(i)).getTableCellRendererComponent(table, dummyValues[i], false, false, 0, i);
			cellWidth = comp.getPreferredSize().width;*/

			//XXX: Before Swing 1.1 Beta 2, use setMinWidth instead.
			//column.setPreferredWidth(Math.max(headerWidth, cellWidth));			if(i==0) column.setPreferredWidth(7);
			else if(i==1) column.setPreferredWidth(100);			else column.setPreferredWidth(200);
		}
	}

	/**
	 * Destroys this table.
	 */	
	public void destroy()
	{
		try{		if(logMessageDialog != null) 		{
			logMessageDialog.dispose();			logMessageDialog = null;		}		}catch(Exception e){}
	}

	/**
	 * Sets the title of this table.
	 */
	public void setTitle(String newTitle)
	{
		setBorder(GuiUtils.createTitleCompoundBevelBorder(" " + newTitle + " "));
	}
	
	/**
	 * Called when a new log message is received.
	 */
	public void newLogMessage(LogMessage logMsg)
	{
		model.addLogMessage(logMsg);
	}
	
	/**
	 * Clears the table.
	 */
	public void clear()
	{
		model.setData(new ArrayList());
		validate();
	}
	
	/**
	 * Fills the table with the log messages in the specified list.
	 */
	public void newLog(final java.util.List log)
	{
		model.setData(log);
		validate();
	}
	
	/**
	 * Mouse click event handler method.
	 */
	public void mouseClicked(final MouseEvent e)
	{
		if(e.getClickCount() == 2)
		{
			int row = table.getSelectedRow();
			
			if(row>=0)
			{	
				LogMessage logMsg = (LogMessage)model.getValueAt(row, 0);
			
				if(logMsg != null)
				{
					Frame owner = JFrame.getFrames()[0];					
					try{					if(logMessageDialog != null) 					{
						logMessageDialog.dispose();						logMessageDialog = null;					}					}catch(Exception ex){}
										
					logMessageDialog = new LogMessageDialog(owner, logMsg);
								Dimension dlgSize = new Dimension(400, 300);//dlg.getPreferredSize();
					Dimension frmSize = owner.getSize();
					Point loc = owner.getLocation();
					logMessageDialog.setLocation((frmSize.width - dlgSize.width) / 2 + loc.x, (frmSize.height - dlgSize.height) / 2 + loc.y);
					logMessageDialog.setSize(dlgSize);
			
					logMessageDialog.setVisible(true);
					
					logMessageDialog = null;
				}
			}
		}
	}
	public void mousePressed(MouseEvent e){}
	public void mouseReleased(MouseEvent e){}
	public void mouseEntered(MouseEvent e){}
	public void mouseExited(MouseEvent e){}
	
	/**
	 * The table model that is used for the log messages table.
	 */
	final static class LogTableModel extends AbstractTableModel
	{
		/**
       * 
       */
      private static final long serialVersionUID = 1L;
      public static final String[] columnNames = {	"Level", 
												"Date",
												"Origin",
												"Message"};
		private java.util.List data = null;
		
		private SimpleDateFormat dateFormat;
	
		/**
		 * Creates a new LogTableModel.
		 */
		public LogTableModel()
		{
			super();
			
			data = new ArrayList();
			
			dateFormat = (SimpleDateFormat)DateFormat.getDateTimeInstance();
			dateFormat.applyPattern("yyyy-MM-dd HH:mm:ss.SSS");
		}
		
		/**
		 * Sets the data of the model.
		 */
		public void setData(final java.util.List data)
		{
			this.data = data;
			fireTableDataChanged();
		}
		
		/**
		 * Adds a log message to the data in the model.
		 */
		public void addLogMessage(LogMessage logMsg)
		{
			data.add(logMsg);
			fireTableRowsInserted(getRowCount(), getRowCount());
		}
		
		/**
		 * Gets the number of columns in the table.
		 */
		public int getColumnCount() 
		{
			return columnNames.length;
	    }
	
		/**
		 * Gets the number of rows in the table.
		 */
		public int getRowCount() 
		{
			if(data!=null) return data.size();
			else return 0;
	    }
	
	    /**
	     * Gets the name of the column with the specified index.
	     */
		public String getColumnName(final int col) 
		{
			return columnNames[col];
	    }
	
	    /**
	     * Gets the value in the specified cell.
	     */
		public Object getValueAt(final int row, final int col) 
		{
			try
			{
				if(row < data.size())
				{
					LogMessage logMsg = (LogMessage)data.get(row);
				
					if(col == 0) return logMsg;
					else if(col == 1) return dateFormat.format(logMsg.getTime());
					else if(col == 2) return logMsg.origin;
					else if(col == 3) return logMsg.msg;
				}
				return null;
			}
			catch(ArrayIndexOutOfBoundsException e)
			{
				return null;
			}
	    }
	}
	
	/**
	 * The class responsible for rendering the cells of the log message table.
	 */
	final static class LogLevelCellRenderer extends DefaultTableCellRenderer
	{
		/**
       * 
       */
      private static final long serialVersionUID = 1L;
      private final String[] logLevelNames= {"Debug", "Info", "Warning", "Error", "Critical error"};
		private final String prefix = "/images/loglevel/";
	
		/**
		 * Creates a new LogLevelCellRenderer.
		 */		
		public LogLevelCellRenderer()
		{
			super();
		}
	
		/**
		 * Gets the component for rendering the cell at the specified row and column.
		 */
		public Component getTableCellRendererComponent(final JTable table, Object value, final boolean isSelected, boolean hasFocus, int row, int column)
		{
			if(value instanceof LogMessage)
			{
				LogMessage logMsg = (LogMessage)value;
				JLabel label = new JLabel()
					{
						/**
                   * 
                   */
                  private static final long serialVersionUID = 1L;

                  public void paint(Graphics g) 
						{
							Color bColor = table.getSelectionBackground();
							
							if(isSelected)
							{
								g.setColor(bColor);
								g.fillRect(0, 0, getWidth(), getHeight());
							}
							
							super.paint(g);
						}
					};
						
				label.setHorizontalAlignment(SwingConstants.CENTER);
				
				try
				{
					if(logMsg.level == LogMessage.DEBUG_LEVEL) label.setIcon(ResourceHandler.getImageIcon(prefix + "debug.gif"));
					else if(logMsg.level == LogMessage.INFO_LEVEL) label.setIcon(ResourceHandler.getImageIcon(prefix + "info.gif"));
					else if(logMsg.level == LogMessage.WARNING_LEVEL) label.setIcon(ResourceHandler.getImageIcon(prefix + "warning.gif"));
					else if(logMsg.level == LogMessage.ERROR_LEVEL) label.setIcon(ResourceHandler.getImageIcon(prefix + "error.gif"));
					else if(logMsg.level == LogMessage.CRITICAL_ERROR_LEVEL) label.setIcon(ResourceHandler.getImageIcon(prefix + "critical.gif"));
					else label.setText("" + logMsg.level);
				}
				catch(Exception e)
				{
					label.setText("" + logMsg.level);
				}
					
				String logLevel;
				
				if(logMsg.level >= logLevelNames.length)
					logLevel = Integer.toString(logMsg.level);
				else
					logLevel = logLevelNames[logMsg.level];
				
				label.setToolTipText("Loglevel " + logLevel);
				
				return label;
			}
			else
			{
				return super.getTableCellRendererComponent(table, value, isSelected, false, row, column);
			}
		}
	}
}


