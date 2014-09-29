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
import java.awt.Frame;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.border.BevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.teletalk.jadmin.gui.AdminMainPanel;

/**
 * 
 */
public class LoggingMainPanel extends JPanel implements ChangeListener
{
   /**
    * 
    */
   private static final long serialVersionUID = 1L;
   private AdminMainPanel mainPanel;
   private Frame mainFrame;
   private JTabbedPane tabControl;
   
   private LiveLogSubPanel liveLogPanel;
   private SavedLogSubPanel savedLogPanel;
   
   /**
    * Creates a new LoggingMainPanel.
    */
   public LoggingMainPanel(AdminMainPanel mainPanel)
   {
      super(new BorderLayout());

      this.mainPanel = mainPanel;
      this.mainFrame = mainPanel.getMainFrame();
            
      tabControl = new JTabbedPane();
      tabControl.setBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED));
      tabControl.addChangeListener(this);
      
      liveLogPanel = new LiveLogSubPanel(this.mainPanel);
      savedLogPanel = new SavedLogSubPanel(this.mainPanel);
            
      tabControl.addTab("Live log", null, liveLogPanel, "View live log");
      tabControl.addTab("Saved logs", null, savedLogPanel, "View saved logs");
      
      add(BorderLayout.WEST, Box.createHorizontalStrut(10));
      add(BorderLayout.EAST, Box.createHorizontalStrut(10));
      add(BorderLayout.SOUTH, Box.createVerticalStrut(10));
      add(BorderLayout.NORTH, Box.createVerticalStrut(10));
      add(BorderLayout.CENTER, tabControl);
   }
   
   /**
    * Called when the state of the tab control is changed.
    */
   public void stateChanged(ChangeEvent e) 
   {
     if(savedLogPanel != null)
     {
        savedLogPanel.appenderComboSelectionChanged();
     }
   }
   
   /**
    * Destroys this panel and the panels contained in it.
    */
   public void destroy()
   {
      liveLogPanel.destroy();
      savedLogPanel.destroy();
   }
   
   /**
    * Called to process an event received from the server.
    */   
   public void handleEvent(final Object event)
   {
      liveLogPanel.handleEvent(event);
      savedLogPanel.handleEvent(event);
   }
   
   /**
    * Called to initialize the combo boxes holding loggers and logs (on the saved log panel).
    */
   public void initAppenderCombos()
   {
      savedLogPanel.initAppenderCombos();
   }

   /**
    * Gets the main frame.
    */
   public Frame getMainFrame()
   {
      return mainFrame; 
   }
   
   /**
    * Gets then main panel of the JAdmin GUI.
    */
   public AdminMainPanel getMainPanel()
   {
      return mainPanel; 
   }
}
