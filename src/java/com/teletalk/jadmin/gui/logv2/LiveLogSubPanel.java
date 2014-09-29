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
package com.teletalk.jadmin.gui.logv2;

import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.apache.log4j.spi.LoggingEvent;

import com.teletalk.jadmin.gui.AdminMainPanel;
import com.teletalk.jserver.JServerConstants;
import com.teletalk.jserver.rmi.remote.RemoteLoggingEvent;
import com.teletalk.jserver.util.MessageQueue;

/**
 * 
 * @since 2.0
 */
public class LiveLogSubPanel extends JPanel implements Runnable, ActionListener, MouseListener
{
   /**
    * 
    */
   private static final long serialVersionUID = 1L;


   private boolean is211OrLater;
   
   
   protected AdminMainPanel mainPanel;
   protected Frame mainFrame;
   
   private boolean canRun; 
   
   private final JCheckBox viewAllCheckBox;
   private final JLabel viewAllLogsLabel;
   private final JButton clearButton;
      
   private final JList availableLoggersList;
   private final DefaultListModel availableLoggersModel;
   
   private final JButton addButton;
   private final JButton removeButton;
   
   private final JList selectedLoggersList;
   private final DefaultListModel selectedLoggersModel;
   
   private LogTable currentLogTable;
   
   private final MessageQueue queuedLogMessages;
   private int maxSize;
   private int overshoot;
   
   private int rejectedLogmessages;

   private Thread thread;
   
   /**
    * Creates a new LogPanel.
    */   
   public LiveLogSubPanel(AdminMainPanel mainPanel)
   {
      super(new BorderLayout());
      
      try
      {
         this.is211OrLater = mainPanel.getJAdmin().getAdministrator().isRemoteJServerVersionOrLater(2, 1, 1);
      }
      catch(Exception e)
      {
         e.printStackTrace();
         this.is211OrLater = true;
      }
      

      this.mainPanel = mainPanel;
      this.mainFrame = mainPanel.getMainFrame();
      
      JPanel liveLogUpperPanel = new JPanel(new BorderLayout());
      
      
      JPanel viewAllCheckBoxPanel = new JPanel(new BorderLayout());
      viewAllLogsLabel = new JLabel("View all logs:");
      viewAllLogsLabel.addMouseListener(this);
      viewAllCheckBoxPanel.add(viewAllLogsLabel);
      
      viewAllCheckBox = new JCheckBox();
      viewAllCheckBox.setSelected(false);
      viewAllCheckBox.setToolTipText("View all logs");
      viewAllCheckBox.addActionListener(this);
      
      clearButton = new JButton("Clear");
      clearButton.setToolTipText("Clear all log events");
      clearButton.addActionListener(this);
      
      viewAllCheckBoxPanel.add(BorderLayout.WEST, viewAllCheckBox);
      viewAllCheckBoxPanel.add(BorderLayout.EAST, clearButton);
      
      JPanel viewSelectedPanel = new JPanel();
      viewSelectedPanel.setLayout(new BoxLayout(viewSelectedPanel, BoxLayout.X_AXIS));
      
      
      JPanel availableLoggersPanel = new JPanel(new BorderLayout());
            
      availableLoggersModel = new DefaultListModel();
      availableLoggersList = new JList(availableLoggersModel);
      availableLoggersList.setFont(new Font("SansSerif", Font.PLAIN, 11));
      availableLoggersList.setVisibleRowCount(4);
      availableLoggersList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      availableLoggersList.setPrototypeCellValue("MyLittleServer.MySystem.MyComponent.MyComponent.MyComponent.MyComponent");
      JScrollPane availableLoggersListScroll = new JScrollPane(availableLoggersList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      
      availableLoggersPanel.add(BorderLayout.NORTH, new JLabel("  Available loggers"));
      availableLoggersPanel.add(BorderLayout.CENTER, availableLoggersListScroll);
      availableLoggersPanel.add(BorderLayout.WEST, Box.createHorizontalStrut(5));
      availableLoggersPanel.add(BorderLayout.EAST, Box.createHorizontalStrut(5));
      
      JPanel buttonPanel = new JPanel();
      buttonPanel.setLayout(new BoxLayout(buttonPanel, BoxLayout.Y_AXIS));
      
      addButton = new JButton(">");
      addButton.addActionListener(this);
      removeButton = new JButton("<");
      removeButton.addActionListener(this);
      
      buttonPanel.add(Box.createVerticalStrut(10));
      buttonPanel.add(addButton);
      buttonPanel.add(Box.createVerticalStrut(10));
      buttonPanel.add(removeButton);
      
      JPanel selectedLoggersPanel = new JPanel(new BorderLayout());
            
      selectedLoggersModel = new DefaultListModel();
      selectedLoggersList = new JList(selectedLoggersModel);
      selectedLoggersList.setFont(new Font("SansSerif", Font.PLAIN, 11));
      selectedLoggersList.setVisibleRowCount(4);
      selectedLoggersList.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      selectedLoggersList.setPrototypeCellValue("MyLittleServer.MySystem.MyComponent.MyComponent.MyComponent.MyComponent");
      JScrollPane selectedLoggersListScroll = new JScrollPane(selectedLoggersList, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
      
      selectedLoggersPanel.add(BorderLayout.NORTH, new JLabel("  Selected loggers"));
      selectedLoggersPanel.add(BorderLayout.CENTER, selectedLoggersListScroll);
      selectedLoggersPanel.add(BorderLayout.WEST, Box.createHorizontalStrut(5));
      selectedLoggersPanel.add(BorderLayout.EAST, Box.createHorizontalStrut(5));
      
      viewSelectedPanel.add(availableLoggersPanel);
      viewSelectedPanel.add(buttonPanel);
      viewSelectedPanel.add(selectedLoggersPanel);
      
      
      //JPanel filterPanel = new JPanel(new FlowLayout());
      
      
      /*filterPanel.add(new JLabel(""));
      filterPanel*/
      
      liveLogUpperPanel.add(BorderLayout.NORTH, viewAllCheckBoxPanel);
      liveLogUpperPanel.add(BorderLayout.CENTER, viewSelectedPanel);
      //liveLogUpperPanel.add(BorderLayout.SOUTH, filterPanel);
      

      currentLogTable = new LogTable(this.is211OrLater);
      
      add(BorderLayout.NORTH, liveLogUpperPanel);
      add(BorderLayout.CENTER, currentLogTable);
      
      this.maxSize = 1000;
      this.overshoot = 50;
      
      queuedLogMessages = new MessageQueue();
      
      this.initLoggers();
      
      start();
   }
   
   private void initLoggers()
   {
      try
      {
         String[] loggers = this.mainPanel.getJAdmin().getAdministrator().listLoggers();
         for(int i=0; i<loggers.length; i++)
         {
            if( is211OrLater ) availableLoggersModel.addElement(loggers[i]);
            else if( !loggers[i].equalsIgnoreCase(JServerConstants.JSERVER_TOP_SYSTEM_ALIAS) ) availableLoggersModel.addElement(loggers[i]);
            
         }
         
         this.initLogMode();
      }
      catch(Exception e)
      {
         e.printStackTrace();
      }
   }
   
   private void initLogMode()
   {
      boolean selected = viewAllCheckBox.isSelected();
      
      this.availableLoggersList.setEnabled(!selected);
      this.selectedLoggersList.setEnabled(!selected);
      this.addButton.setEnabled(!selected);
      this.removeButton.setEnabled(!selected);
      
      try
      {
         if( selected )
         {
            // Unregister selected appenders and add appender for JServer
            mainPanel.getJAdmin().getAdministrator().removeAllAppenders();
            if( is211OrLater )
            {
               mainPanel.getJAdmin().getAdministrator().addAppender(null); // Root logger
            }
            else
            {
               mainPanel.getJAdmin().getAdministrator().addAppender(JServerConstants.JSERVER_TOP_SYSTEM_ALIAS);
            }
         }
         else
         {
            // Register selected appenders and unregister appender for JServer
            int size = this.selectedLoggersModel.getSize();
            for(int i=0; i<size; i++)
            {
               mainPanel.getJAdmin().getAdministrator().addAppender(this.selectedLoggersModel.get(i).toString());
            }
            if( is211OrLater )
            {
               mainPanel.getJAdmin().getAdministrator().removeAppender(null); // Root logger
            }
            else
            {
               mainPanel.getJAdmin().getAdministrator().removeAppender(JServerConstants.JSERVER_TOP_SYSTEM_ALIAS);
            }
         }
      }
      catch(Exception e){e.printStackTrace();}
   }
   
   /**
    * Action event handler method.
    */
   public void actionPerformed(final ActionEvent event)
   {
      try
      {
         if(event.getSource() == viewAllCheckBox)
         {
            this.initLogMode();
         }
         else if(event.getSource() == addButton)
         {
            int[] selIndices = availableLoggersList.getSelectedIndices();
            for(int i=0; i<selIndices.length; i++)
            {
               if( selIndices[i] < availableLoggersModel.getSize() )
               {
                  String loggerName = this.availableLoggersModel.get(selIndices[i]).toString();
                  mainPanel.getJAdmin().getAdministrator().addAppender(loggerName);
                  this.availableLoggersModel.remove(selIndices[i]);
                  this.selectedLoggersModel.addElement(loggerName);
               }
            }
         }
         else if(event.getSource() == removeButton)
         {
            int[] selIndices = selectedLoggersList.getSelectedIndices();
            for(int i=0; i<selIndices.length; i++)
            {
               if( selIndices[i] < selectedLoggersModel.getSize() )
               {
                  String loggerName = this.selectedLoggersModel.get(selIndices[i]).toString();
                  mainPanel.getJAdmin().getAdministrator().removeAppender(loggerName);
                  this.selectedLoggersModel.remove(selIndices[i]);
                  this.availableLoggersModel.addElement(loggerName);
               }
            }
         }
         else if(event.getSource() == clearButton)
         {
            currentLogTable.clear();
         }
      }
      catch(Exception e){e.printStackTrace();}
   }
   
   /**
    * Start the internal thread.
    */
   private void start()
   {
      canRun = true;
      thread = new Thread(this, "LiveLogSubPanel thread");
      thread.setDaemon(true);
      thread.start();
   }
   
   /**
    * Destroys the thread of this LiveLogSubPanel.
    */
   public void destroy()
   {
      canRun = true;
      thread.interrupt();
      
      if(currentLogTable != null) 
      {
         currentLogTable.destroy();
         currentLogTable = null;
      }
   }
   
   /**
    * Called to process an event received from the server.
    */
   public void handleEvent(Object event)
   {
      if(event instanceof RemoteLoggingEvent)
      {
         if(queuedLogMessages.size() < 1000)
         {
            if(rejectedLogmessages > 0)
            {
               this.mainPanel.writeToEventLog("Warning! Overflow in messagequeue for logmessages (current log panel)! " + rejectedLogmessages + " messages rejected!");
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
    * Called when a new RemoteLogEvent is received by the LogBuffer.
    * 
    * @param event the RemoteLogEvent.
    * 
    * @see com.teletalk.jserver.rmi.RemoteLogEvent
    */
   private synchronized void processLogEvent(RemoteLoggingEvent event)
   {
      final LoggingEvent loggingEvent = event.getLoggingEvent(); 
      
      if( currentLogTable.getRowCount() > (maxSize + overshoot))
      {
         currentLogTable.trimFirst(overshoot);
      }
      
      currentLogTable.newLoggingEvent(loggingEvent);
   }
   
   /**
    * The thread method of this LogBuffer. Checkes the queue for new LogEvents.
    */
   public void run()
   {
      RemoteLoggingEvent event;

      while(canRun)
      {        
         try
         {
            event = (RemoteLoggingEvent)queuedLogMessages.getMsg();
            processLogEvent(event);
         }catch(Exception e)  {}
      }
   }
   
   /**
    * 
    */
   public void mouseClicked(MouseEvent e)
   {
      if(e.getSource() == viewAllLogsLabel)
      {
         viewAllCheckBox.setSelected(!viewAllCheckBox.isSelected());
         this.initLogMode();
      }
   }
   
   public void mouseEntered(MouseEvent e){}
   
   public void mouseExited(MouseEvent e){}
   
   public void mousePressed(MouseEvent e){}
   
   public void mouseReleased(MouseEvent e){}
}
