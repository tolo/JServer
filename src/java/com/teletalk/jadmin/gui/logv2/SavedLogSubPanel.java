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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.border.TitledBorder;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import com.teletalk.jadmin.gui.AdminMainPanel;
import com.teletalk.jadmin.proxy.SubComponentProxy;
import com.teletalk.jadmin.proxy.messages.AppenderComponentGetLogsRequest;
import com.teletalk.jadmin.proxy.messages.AppenderComponentGetLogsResponse;
import com.teletalk.jserver.log.LogData;
import com.teletalk.jserver.rmi.remote.RemoteAppenderComponent;
import com.teletalk.jserver.rmi.remote.RemoteInputStreamClient;
 
/**
 * 
 */
public class SavedLogSubPanel extends JPanel implements Runnable, ActionListener
{
   /**
    * 
    */
   private static final long serialVersionUID = 1L;
   protected final AdminMainPanel mainPanel;
   protected final Frame mainFrame;
   
   private boolean initialized;
   
   private boolean canRun; 
   
   private Thread thread;
      
   private final JButton getLog;
   
   private final HashMap appenders;
   
   private final JComboBox appendersCombo;
   private final JComboBox logCombo;
   
   //private final JLabel getLogStandby;
   
   private final JTextPane logText;
   private DefaultStyledDocument logTextDocument;
   private final JScrollPane logTextScroll;
   private final TitledBorder logTextBorder;
   
   private final SimpleAttributeSet logTextAttributes;
   
   private AppenderComponentGetLogsRequest currentAppenderComponentGetLogsRequest;
   
   private SubComponentProxy currentAppenderComponent = null;
   private String currentLogName = null;
   private final Object getLogMonitor = new Object();
   
   private boolean appenderCombosInitialized = false;
   
   /**
    * Creates a new LogPanel.
    */   
   public SavedLogSubPanel(AdminMainPanel mainPanel)
   {
      super(new BorderLayout());
         
      this.mainPanel = mainPanel;
      this.mainFrame = mainPanel.getMainFrame();
      
      initialized = false;

      appenders = new HashMap();

         JPanel savedLogUpper = new JPanel();
         savedLogUpper.setLayout(new BoxLayout(savedLogUpper, BoxLayout.X_AXIS));
               
               appendersCombo = new JComboBox();
               appendersCombo.setFont(new Font("SansSerif", Font.PLAIN, 11));
               logCombo = new JComboBox();
               logCombo.setFont(new Font("SansSerif", Font.PLAIN, 11));
               
               appendersCombo.addActionListener(this);
               
               logCombo.setAlignmentX(CENTER_ALIGNMENT);
               appendersCombo.setAlignmentX(CENTER_ALIGNMENT);
               
               appendersCombo.setBorder(BorderFactory.createTitledBorder("Available appender components"));
               logCombo.setBorder(BorderFactory.createTitledBorder("Available logs"));
               
               getLog = new JButton("Get log");
               /*if(this.getClass().getResource("/images/get_log.gif") != null)
                  getLog.setIcon(new ImageIcon(this.getClass().getResource("/images/get_log.gif")));
               if(this.getClass().getResource("/images/get_log_pressed.gif") != null)
                  getLog.setPressedIcon(new ImageIcon(this.getClass().getResource("/images/get_log_pressed.gif")));*/
               JPanel getLogPanel = new JPanel();
               getLogPanel.setLayout(new BoxLayout(getLogPanel, BoxLayout.Y_AXIS));
               getLogPanel.setBorder(BorderFactory.createEmptyBorder(0, 3, 3, 2));
               getLogPanel.add(Box.createVerticalGlue());
               getLogPanel.add(getLog);
               
               /*getLogStandby = new JLabel("Please wait");
               //getLogStandby.setVisible(false);
               getLogStandby.setAlignmentX(CENTER_ALIGNMENT);*/
               
               /*getLog.setAlignmentX(CENTER_ALIGNMENT);
               getLog.setAlignmentY(BOTTOM_ALIGNMENT);*/
               getLog.addActionListener(this);
               /*getLog.setBorderPainted(false);
               getLog.setFocusPainted(false);
               getLog.setContentAreaFilled(false);*/
               
               savedLogUpper.add(appendersCombo);
               savedLogUpper.add(logCombo);
               savedLogUpper.add(getLogPanel);
               //savedLogUpper.add(getLogStandby);
                              
            logText = new JTextPane()
            	{
			         /**
                   * 
                   */
                  private static final long serialVersionUID = 1L;

                  public void setSize(Dimension d)
			         {
			            if (d.width < getParent().getSize().width) d.width = getParent().getSize().width;
			            super.setSize(d);
			         }
			
			         public boolean getScrollableTracksViewportWidth()
			         {
			            return false;
			         }
			      };
			      
            logText.setEditable(false);
            logTextScroll = new JScrollPane(logText,  JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            logTextBorder = BorderFactory.createTitledBorder("<Select a log>");
            logTextScroll.setBorder(logTextBorder);
            
            logTextAttributes = new SimpleAttributeSet();
            StyleConstants.FontConstants.setFontFamily(logTextAttributes, "Courier");
            StyleConstants.FontConstants.setFontSize(logTextAttributes, 11);
            StyleConstants.FontConstants.setForeground(logTextAttributes, Color.darkGray);
            StyleConstants.FontConstants.setBold(logTextAttributes, false);
            
      add(BorderLayout.NORTH, savedLogUpper);
      add(BorderLayout.CENTER, logTextScroll);
      
      validate();
      
      logTextBorder.setTitle("");
      //getLogStandby.setVisible(false);
      
      start();
   }
   
   /**
    * Start the internal thread.
    */
   private void start()
   {
      canRun = true;
      thread = new Thread(this, "SavedLogSubPanel thread");
      thread.setDaemon(true);
      thread.start();
   }
   
   /**
    * Destroys the thread of this panel.
    */
   public void destroy()
   {
      canRun = false;
      thread.interrupt();
   }
   
   /**
    * Called to process an event received from the server.
    */
   public void handleEvent(Object event)
   {
      if(event instanceof AppenderComponentGetLogsResponse)
      {
         AppenderComponentGetLogsResponse response = (AppenderComponentGetLogsResponse)event;

         if(response.getAppenderComponentGetLogsRequest() == currentAppenderComponentGetLogsRequest)
         {
            LogData[] logList = response.getLogs();
               
            if(logCombo.getItemCount() > 0) logCombo.removeAllItems();
            
            for(int i=0; i<logList.length; i++)
            {
               logCombo.addItem(logList[i].getLogName());
            }
               
            //getLogStandby.setVisible(false);
            logCombo.setEnabled(true);
            logCombo.validate();
            validate();
         }
      }
   }
   
   /**
    * Initializes the combo boxes that contans information about available appenders ang logs on the server.
    */   
   public void initAppenderCombos()
   {
      ArrayList appenderComponents = mainPanel.getJServerProxy().getAppenderComponents();
      SubComponentProxy appenderComponent;
      
      for(int i=0; i<appenderComponents.size(); i++)
      {
         appenderComponent = (SubComponentProxy)appenderComponents.get(i);
         
         appenders.put(appenderComponent.getName(), appenderComponent);
         appendersCombo.addItem(appenderComponent.getName());
      }
      
      if(appendersCombo.getItemCount() > 0)
      {
         appendersCombo.setSelectedIndex(0);
         initialized = true;
         //appenderComboSelectionChanged();
      }
      
      appendersCombo.validate();
      validate();
      
      appenderCombosInitialized = true;
   }
   
   /**
    * Called when the selection has changed in the appenders and logs combo boxes.
    */
   public void appenderComboSelectionChanged()
   {
      if(!appenderCombosInitialized) return;
      
      Object selobj = appenders.get(appendersCombo.getSelectedItem());
      SubComponentProxy appenderComponent;
      
      if(selobj != null && initialized)
      {
         appenderComponent = (SubComponentProxy)selobj;
         
         if(logCombo.getItemCount() > 0) logCombo.removeAllItems();
                  
         currentAppenderComponentGetLogsRequest = mainPanel.getJServerProxy().getAppenderComponentLogs(appenderComponent);
         
         logCombo.setEnabled(false);
         //getLogStandby.setVisible(true);
      }
      
      appendersCombo.validate();
      logCombo.validate();
      validate();
   }
   
   /**
    * Downloads the currently selected log (from the selected appender) from the server.
    */
   public boolean getSelectedLog()
   {
      SubComponentProxy selectedAppenderComponent = (SubComponentProxy)appenders.get(appendersCombo.getSelectedItem());
      String selectedLog = (String)logCombo.getSelectedItem();
      
      if((selectedAppenderComponent != null) && (selectedLog != null))
      {
         synchronized(this.getLogMonitor)
         {
            this.currentAppenderComponent = selectedAppenderComponent;
            this.currentLogName = selectedLog;
            
            getLogMonitor.notify();
         }
      }
      return false;
   }
   
   /**
    * Action event handler method.
    */
   public void actionPerformed(ActionEvent event)
   {
      if( event.getSource() == getLog )
      {
         if(getSelectedLog())
         {
            //getLogStandby.setVisible(true);
         }
      }
      else if( event.getSource() == appendersCombo )
      {
         appenderComboSelectionChanged();
      }
   }
   
   /**
    * The thread method of this LogBuffer. Checkes the queue for new LogEvents.
    */
   public void run()
   {
      String currentTransferLogName = null;
      SubComponentProxy currentTransferAppenderComponent = null;
      
      while(canRun)
      {        
         try
         {
            synchronized(this.getLogMonitor)
            {
               while( this.currentLogName == null ) this.getLogMonitor.wait();
            }
            
            currentTransferLogName = this.currentLogName;
            currentTransferAppenderComponent = this.currentAppenderComponent;
            
            if( currentTransferAppenderComponent != null )
            {
               RemoteAppenderComponent component = (RemoteAppenderComponent)currentTransferAppenderComponent.getRemoteSubComponent();
               if( component != null )
               {
                  RemoteInputStreamClient remoteInputStreamClient = new RemoteInputStreamClient(component.getLogAsStream(currentTransferLogName));
                  BufferedReader reader = new BufferedReader(new InputStreamReader(remoteInputStreamClient));
                  
                  this.logTextDocument = new DefaultStyledDocument();
                  this.logText.setStyledDocument(this.logTextDocument);
                  this.logTextBorder.setTitle(currentTransferLogName);
                  
                  String line;
                  StringBuffer textBuffer = new StringBuffer(1000);

                  for(int i=1; ((line = reader.readLine()) != null); i++)
                  {
                     textBuffer.append(line);
                     textBuffer.append(System.getProperty("line.separator"));
                     
                     if( (i %10) == 0 )
                     {
                        logTextDocument.insertString(logTextDocument.getLength(), textBuffer.toString(), logTextAttributes);
                        logTextScroll.getVerticalScrollBar().setValue(logTextScroll.getVerticalScrollBar().getMaximum());
                        textBuffer.delete(0, textBuffer.length());
                        
                        synchronized(this.getLogMonitor)
                        {
                           if( (currentTransferLogName != this.currentLogName) || 
                           (currentTransferAppenderComponent != this.currentAppenderComponent)  )
                           {
                              break;
                           }
                        }
                     }
                  }
                  
                  try{Thread.sleep(100);}catch(Exception e){}
                  
                  logTextScroll.getVerticalScrollBar().setValue(logTextScroll.getVerticalScrollBar().getMaximum());
                  
                  try{ remoteInputStreamClient.close();}catch(Exception e){}
               }
            }
         }
         catch(Exception e){}
         
         //getLogStandby.setVisible(false);
         
         currentTransferLogName = null;
         currentTransferAppenderComponent = null;
         
         synchronized(this.getLogMonitor)
         {
            this.currentLogName = null;
            this.currentAppenderComponent = null;
         }
      }
   }
}
