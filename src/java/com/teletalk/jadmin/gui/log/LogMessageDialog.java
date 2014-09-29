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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.SimpleDateFormat;
import java.util.StringTokenizer;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.BevelBorder;

import com.teletalk.jadmin.gui.util.ResourceHandler;
import com.teletalk.jserver.log.LogMessage;

/**
 * This class handles the dialog box for displaying a log message.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public class LogMessageDialog extends JDialog implements ActionListener
{
	/**
    * 
    */
   private static final long serialVersionUID = 1L;

   private SimpleDateFormat dateFormat;
	
	private JTextArea logMessageText;
	
	private JButton done;
	
	/**
	 * Creates a new LogMessageDialog.
	 */
	public LogMessageDialog(final Frame owner, final LogMessage logMessage)
	{
		super(owner, "Log message" + 
				( (logMessage.getLogMessageId() != LogMessage.LOG_MESSAGE_ID_UNSPECIFIED) ? (" (ID: " + logMessage.getLogMessageId() + ")") : ""), 
				true);
		
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzzz");
		
		JPanel mainPanel = (JPanel)this.getContentPane();
		mainPanel.setLayout(new BorderLayout());
		
		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BorderLayout(3,3));
			
			JPanel upperInfoPanel = new JPanel();
			upperInfoPanel.setLayout(new BorderLayout(3,0));
			
			JPanel levelPanel = new JPanel(new FlowLayout(FlowLayout.LEFT))
				{
					/**
                * 
                */
               private static final long serialVersionUID = 1L;

               public Insets getInsets()
					{
						return new Insets(1,1,1,1);
					}
				};
			levelPanel.setBorder(BorderFactory.createEtchedBorder());
				
				JLabel levelIconLabel = new JLabel("Level: ");
				JLabel levelLabel = new JLabel(String.valueOf(logMessage.level));
				levelLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
				levelLabel.setForeground(Color.black);
	
				String prefix = "/images/loglevel/";
				
				try
				{
					if(logMessage.level == LogMessage.DEBUG_LEVEL) levelIconLabel.setIcon(ResourceHandler.getImageIcon(prefix + "debug.gif"));
					else if(logMessage.level == LogMessage.INFO_LEVEL) levelIconLabel.setIcon(ResourceHandler.getImageIcon(prefix + "info.gif"));
					else if(logMessage.level == LogMessage.WARNING_LEVEL) levelIconLabel.setIcon(ResourceHandler.getImageIcon(prefix + "warning.gif"));
					else if(logMessage.level == LogMessage.ERROR_LEVEL) levelIconLabel.setIcon(ResourceHandler.getImageIcon(prefix + "error.gif"));
					else if(logMessage.level == LogMessage.CRITICAL_ERROR_LEVEL) levelIconLabel.setIcon(ResourceHandler.getImageIcon(prefix + "critical.gif"));
				}
				catch(Exception e)
				{
					//levelIconLabel.setText("Level: " + logMessage.level);
				}
				
			levelPanel.add(levelIconLabel);
			levelPanel.add(levelLabel);
			
			JPanel datePanel = new JPanel(new FlowLayout(FlowLayout.LEFT))
				{
					/**
                * 
                */
               private static final long serialVersionUID = 1L;

               public Insets getInsets()
					{
						return new Insets(1,1,1,1);
					}
				};
			datePanel.setBorder(BorderFactory.createEtchedBorder());
				
				JLabel dateTitleLabel = new JLabel("Time: ");
				JLabel dateLabel;
				
				if(logMessage.time != null)
					dateLabel = new JLabel(dateFormat.format(logMessage.time));	
				else
					dateLabel = new JLabel("<no date>");
				
				dateLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
				dateLabel.setForeground(Color.black);
				
			datePanel.add(dateTitleLabel);
			datePanel.add(dateLabel);
						
			upperInfoPanel.add(Box.createVerticalStrut(3), BorderLayout.NORTH);
			upperInfoPanel.add(levelPanel, BorderLayout.WEST);
			upperInfoPanel.add(datePanel, BorderLayout.CENTER);
		
		JPanel originPanel = new JPanel(new FlowLayout(FlowLayout.LEFT))
			{
				/**
             * 
             */
            private static final long serialVersionUID = 1L;

            public Insets getInsets()
				{
					return new Insets(1,1,1,1);
				}
			};
		
		JScrollPane originScroll = new JScrollPane(originPanel, JScrollPane.VERTICAL_SCROLLBAR_NEVER, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
		originScroll.setBorder(BorderFactory.createEtchedBorder());
		
		JLabel originTitleLabel = new JLabel("Origin: ");
		JLabel originLabel;
		
		if(logMessage.origin != null)
			originLabel = new JLabel(logMessage.origin);
		else
			originLabel = new JLabel("<no origin>");
				
		originLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
		originLabel.setForeground(Color.black);
					
		originPanel.add(originTitleLabel);
		originPanel.add(originLabel);

		infoPanel.add(upperInfoPanel, BorderLayout.NORTH);
		infoPanel.add(Box.createHorizontalStrut(3), BorderLayout.EAST);
		infoPanel.add(Box.createHorizontalStrut(3), BorderLayout.WEST);
		infoPanel.add(originScroll, BorderLayout.CENTER);
		
		String result = "";
		
		if(logMessage.msg != null)
		{
			StringTokenizer t = new StringTokenizer(logMessage.msg);
		
			result += t.nextToken();
			while(t.hasMoreTokens())
				result += " " + t.nextToken();
		}
		else
			result = "<No message>";
		
		logMessageText = new JTextArea(result, 5, 25);
		logMessageText.setLineWrap(true);
		logMessageText.setWrapStyleWord(true);
		logMessageText.setEditable(false);
		JScrollPane logScroll = new JScrollPane(logMessageText);
		logScroll.setBorder(BorderFactory.createTitledBorder("Message"));
				
		JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		southPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		
		done = new JButton("Done");
		done.addActionListener(this);
		
		southPanel.add(done);
		
		mainPanel.add(infoPanel, BorderLayout.NORTH);
		mainPanel.add(logScroll, BorderLayout.CENTER);
		mainPanel.add(southPanel, BorderLayout.SOUTH);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		pack();
	}
	
	/**
	 * Action event handler method.
	 */
	public void actionPerformed(ActionEvent event)
	{
		if(event.getSource() == done)
		{
			dispose();
		}
	}
}
