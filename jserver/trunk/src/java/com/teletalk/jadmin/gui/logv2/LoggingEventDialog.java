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
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.Iterator;
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

import org.apache.log4j.Level;
import org.apache.log4j.spi.LoggingEvent;
import org.apache.log4j.spi.ThrowableInformation;

import com.teletalk.jadmin.gui.util.ResourceHandler;
import com.teletalk.jserver.JServerConstants;

/**
 * This class handles the dialog box for displaying a log message.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public class LoggingEventDialog extends JDialog implements ActionListener
{
   private static final long serialVersionUID = 1L; // Just to remove warning...
   

   private final LogTable logTable;
   
   LoggingEvent loggingEvent;
   
   Hashtable mdcContext = null;
   
	private SimpleDateFormat dateFormat;
	
   private JLabel levelIconLabel;
   private JLabel levelLabel;
   
   private JLabel dateLabel;
   
   private JLabel originLabel;
   
	private JTextArea logMessageText;
   
   private JTextArea ndcText;
   
   private JTextArea mdcText;
	
	private JButton done;
   
   private JButton nextButton;
   private JButton previousButton;
   
	
	/**
	 * Creates a new LogMessageDialog.
	 */
	public LoggingEventDialog(final Frame owner, final LogTable logTable, final LoggingEvent loggingEvent)
	{
		super(owner, true);
      
      this.logTable = logTable;
       
      /*Object o = loggingEvent.getMDC(JServerConstants.LOG_MESSAGE_ID_KEY);
		super.setTitle("Logging event" + ((o != null) ? (" (ID: " + o + ")") : "") );*/
		
		this.loggingEvent = loggingEvent;
		
		dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS z");
		
		JPanel mainPanel = (JPanel)this.getContentPane();
		mainPanel.setLayout(new BorderLayout());
		
		JPanel infoPanel = new JPanel();
		infoPanel.setLayout(new BorderLayout(1, 2)); //3,3));
			
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
				
				levelIconLabel = new JLabel("Level: ");
				levelLabel = new JLabel("");
				levelLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
				levelLabel.setForeground(Color.black);
				
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
								
				dateLabel = new JLabel("");
				
				dateLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
				dateLabel.setForeground(Color.black);
				
			datePanel.add(dateTitleLabel);
			datePanel.add(dateLabel);
						
			//upperInfoPanel.add(Box.createVerticalStrut(3), BorderLayout.NORTH);
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
      originLabel = new JLabel("");
				
		originLabel.setFont(new Font("SansSerif", Font.PLAIN, 11));
		originLabel.setForeground(Color.black);
					
		originPanel.add(originTitleLabel);
		originPanel.add(originLabel);

      JPanel upperInfoWrapperPanel = new JPanel(new BorderLayout());
      upperInfoWrapperPanel.add(Box.createVerticalStrut(3), BorderLayout.NORTH);
      upperInfoWrapperPanel.add(BorderLayout.CENTER, upperInfoPanel);
      upperInfoWrapperPanel.add(BorderLayout.EAST, Box.createHorizontalStrut(2));
      upperInfoWrapperPanel.add(BorderLayout.WEST, Box.createHorizontalStrut(2));
            
		infoPanel.add(upperInfoWrapperPanel, BorderLayout.NORTH);
		infoPanel.add(BorderLayout.EAST, Box.createHorizontalStrut(1));
		infoPanel.add(BorderLayout.WEST, Box.createHorizontalStrut(1));
		infoPanel.add(originScroll, BorderLayout.CENTER);
		
      GridBagLayout messageLayout = new GridBagLayout();
      GridBagConstraints constraints = new GridBagConstraints();
      JPanel messagePanel = new JPanel(messageLayout);
      
		logMessageText = new JTextArea("", 5, 35);
		logMessageText.setLineWrap(true);
		logMessageText.setWrapStyleWord(true);
		logMessageText.setEditable(false);
		JScrollPane logScroll = new JScrollPane(logMessageText);
		logScroll.setBorder(BorderFactory.createTitledBorder("Message"));
      
      constraints.fill = GridBagConstraints.BOTH;
      constraints.weightx = 1.0;
      constraints.weighty = 5.0;
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      messageLayout.setConstraints(logScroll, constraints);
      messagePanel.add(logScroll);
      
      ndcText = new JTextArea("", 2, 35);
      ndcText.setLineWrap(true);
      ndcText.setWrapStyleWord(true);
      ndcText.setEditable(false);
      JScrollPane ndcTextScroll = new JScrollPane(ndcText);
      ndcTextScroll.setBorder(BorderFactory.createTitledBorder("Nested Diagnostic Context"));
      
      constraints.fill = GridBagConstraints.BOTH;
      constraints.weightx = 1.0;
      constraints.weighty = 2.0;
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      messageLayout.setConstraints(ndcTextScroll, constraints);
      messagePanel.add(ndcTextScroll);
      
      mdcText= new JTextArea("", 2, 35);
      mdcText.setLineWrap(true);
      mdcText.setWrapStyleWord(true);
      mdcText.setEditable(false);
      JScrollPane mdcTextScroll = new JScrollPane(mdcText);
      mdcTextScroll.setBorder(BorderFactory.createTitledBorder("Mapped Diagnostic Context"));
      
      constraints.fill = GridBagConstraints.BOTH;
      constraints.weightx = 1.0;
      constraints.weighty = 2.0;
      constraints.gridwidth = GridBagConstraints.REMAINDER;
      messageLayout.setConstraints(mdcTextScroll, constraints);
      messagePanel.add(mdcTextScroll);
				
		JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		southPanel.setBorder(BorderFactory.createBevelBorder(BevelBorder.LOWERED));
		
		done = new JButton("Done");
		done.addActionListener(this);
      
      nextButton = new JButton("  Next  ");
      nextButton.addActionListener(this);
      
      previousButton = new JButton("Previous");
      previousButton.addActionListener(this);
		
		southPanel.add(previousButton);
      southPanel.add(Box.createHorizontalStrut(15));
      southPanel.add(done);
      southPanel.add(Box.createHorizontalStrut(15));
      southPanel.add(nextButton);
		
		mainPanel.add(infoPanel, BorderLayout.NORTH);
		//mainPanel.add(logScroll, BorderLayout.CENTER);
      mainPanel.add(messagePanel, BorderLayout.CENTER);
		mainPanel.add(southPanel, BorderLayout.SOUTH);
		
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
      initData();
	}
   
   /**
    */
   private void initData()
   {
      Object o = loggingEvent.getMDC(JServerConstants.LOG_MESSAGE_ID_KEY);
      super.setTitle("Logging event" + ((o != null) ? (" (ID: " + o + ")") : "") );
      
      levelLabel.setText( loggingEvent.getLevel().toString() );
      
      String prefix = "/images/loglevel/";
            
      try
      {
         if(loggingEvent.getLevel().toInt() == Level.DEBUG_INT) levelIconLabel.setIcon(ResourceHandler.getImageIcon(prefix + "debug.gif"));
         else if(loggingEvent.getLevel().toInt() == Level.INFO_INT) levelIconLabel.setIcon(ResourceHandler.getImageIcon(prefix + "info.gif"));
         else if(loggingEvent.getLevel().toInt() == Level.WARN_INT) levelIconLabel.setIcon(ResourceHandler.getImageIcon(prefix + "warning.gif"));
         else if(loggingEvent.getLevel().toInt() == Level.ERROR_INT) levelIconLabel.setIcon(ResourceHandler.getImageIcon(prefix + "error.gif"));
         else if(loggingEvent.getLevel().toInt() == Level.FATAL_INT) levelIconLabel.setIcon(ResourceHandler.getImageIcon(prefix + "critical.gif"));
      }
      catch(Exception e)
      {
         //levelIconLabel.setText("Level: " + logMessage.level);
      }
      
      dateLabel.setText( dateFormat.format(new Date(loggingEvent.timeStamp)) );
      
      if( logTable.is211OrLater() && (loggingEvent.getLoggerName() != null) )
      {
         originLabel.setText(loggingEvent.getLoggerName());
      }
      else if(loggingEvent.getThreadName() != null)
      {
         originLabel.setText(loggingEvent.getThreadName());
      }
      else
      {
         originLabel.setText("<no origin>");
      }
      
      String messageText = "";
      
      if(loggingEvent.getRenderedMessage() != null)
      {
         StringTokenizer t = new StringTokenizer(loggingEvent.getRenderedMessage());
      
         messageText += t.nextToken();
         while(t.hasMoreTokens()) messageText += " " + t.nextToken();
      }
      else
      {
         messageText = "<No message>";
      }
      
      ThrowableInformation ti = loggingEvent.getThrowableInformation();
      if( ti != null )
      {
         String[] stackTrace = ti.getThrowableStrRep();
         if( stackTrace != null )
         {
            StringBuffer stackTraceBuffer = new StringBuffer(stackTrace.length*5);
            for(int i=0; i<stackTrace.length; i++)
            {
               stackTraceBuffer.append(stackTrace[i]);
            }
            
            String lineSep = System.getProperty("line.separator");
            if( lineSep == null ) lineSep = "\r\n";
            
            messageText += lineSep + lineSep + "Stack trace: " + stackTraceBuffer;
         }
      }
      
      logMessageText.setText(messageText);
      
      ndcText.setText(loggingEvent.getNDC());
      
      // Ugly workaround to the fact that there is now way of getting the MCD from a loggingevent!
      // TODO: When a new version of LoggingEvent with a way to get the MDC comes out - change this:  
      final LoggingEventDialog dialog = this;
      dialog.mdcContext = null;
      
      try
      {
         AccessController.doPrivileged(new PrivilegedAction() 
         {
            public Object run() 
            {
               Class c = dialog.loggingEvent.getClass(); 
               try 
                {
                  Field[] f = c.getDeclaredFields();
                  int mods;
                  boolean accessible;
                  
                  for(int i=0; i<f.length; i++)
                  {
                     mods = f[i].getModifiers();
                     if( !Modifier.isStatic(mods) )
                     {
                        // Attempt to get any Hashtable in LoggingEvent as the mdc context.....
                        if( Hashtable.class.isAssignableFrom(f[i].getType())  )
                        {
                           accessible = f[i].isAccessible();
                           if( !accessible ) f[i].setAccessible(true);
                           dialog.mdcContext = (Hashtable)f[i].get(dialog.loggingEvent);
                           if( !accessible ) f[i].setAccessible(false);
                           
                           // .....but preferrably the field named "mdcCopy" 
                           if( "mdcCopy".equals(f[i].getName())  )
                           {
                              break;
                           }  
                        }
                     }
                  }
                }
                catch(Exception ex){ex.printStackTrace();}
                
                return null;
               }
            });
      }catch(Exception e){e.printStackTrace();}
      
      if( dialog.mdcContext != null )
      {
         Iterator mdcKeys = dialog.mdcContext.keySet().iterator();
         //StringBuffer result = new StringBuffer();
         StringWriter stringWriter = new StringWriter();
         PrintWriter writer = new PrintWriter(stringWriter);
         
         Object key;
         
         while(mdcKeys.hasNext())
         {
            key = mdcKeys.next();
            if( key != null )
            {
               writer.println(key + " = " + dialog.mdcContext.get(key));
            }
         }
         writer.flush();
         
         mdcText.setText(stringWriter.getBuffer().toString());
      }
      
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
      else if(event.getSource() == previousButton)
      {
         this.loggingEvent = this.logTable.getPreviousLoggingEvent();
         this.initData();
      }
      else if(event.getSource() == nextButton)
      {
         this.loggingEvent = this.logTable.getNextLoggingEvent();
         this.initData();
      }
	}
}
