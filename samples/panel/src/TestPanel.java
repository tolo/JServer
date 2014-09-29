import java.awt.*;
import javax.swing.*;

import com.teletalk.jserver.rmi.adapter.*;
import com.teletalk.jserver.rmi.client.*;
import com.teletalk.jserver.rmi.remote.*;

public class TestPanel extends CustomAdministrationPanel implements RemoteEventListener
{
	private JLabel eventLabel;
	
	public TestPanel(RemoteTestSystem rs, String s)
	{
		super(new BorderLayout());
				
		JPanel mainPanel = new JPanel();		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));				JLabel label;
		JLabel iconLabel;		
		try
		{
			label = new JLabel("Msg from server : " + rs.getTestMessage());
			label.setHorizontalAlignment(SwingConstants.CENTER);
			
			iconLabel = new JLabel("This image is loaded via server:");
			iconLabel.setHorizontalAlignment(SwingConstants.CENTER);
			iconLabel.setVerticalTextPosition(SwingConstants.TOP);
			iconLabel.setHorizontalTextPosition(SwingConstants.CENTER);
									
			//Eftersom denna klass är laddad från servern kommer metoden Class.getResource() att returnera en URL
			//till en resurs på servern. Denna resurs laddas sedan över till administrationsverktyget på samma sätt.
			ImageIcon icon = new ImageIcon(this.getClass().getResource("jserver_small.jpg"));
			
			if(icon != null)
			{
				iconLabel.setIcon(icon);
			}
			else
				iconLabel.setText("Unable to load picture - " + this.getClass().getResource("jserver_small.jpg"));
		}
		catch(Exception e)
		{
            e.printStackTrace();
			label = new JLabel("Communication error!");
			iconLabel = null;
		}
		
		label.setAlignmentX(CENTER_ALIGNMENT);
		iconLabel.setAlignmentX(CENTER_ALIGNMENT);
						
		mainPanel.add(Box.createGlue());
		mainPanel.add(label);
		if(iconLabel != null)
		{
			mainPanel.add(Box.createVerticalStrut(25));
			mainPanel.add(iconLabel);
		}
						
		eventLabel = new JLabel();
		eventLabel.setAlignmentX(CENTER_ALIGNMENT);
		
		mainPanel.add(Box.createVerticalStrut(25));
		mainPanel.add(eventLabel);
		
		mainPanel.add(Box.createGlue());
		
		add(BorderLayout.CENTER, mainPanel);
	}
	
	public String getTitle()
	{
		return "En liten testpanel";
	}
	
	protected void destroyPanel()
	{
		System.out.println("TestPanel destroyed");
	}
	
	public void remoteEventReceived(RemoteEvent event)
	{
		if( event instanceof RemoteTestEvent )
		{
			eventLabel.setText("Event received: " + event.toString());
			System.out.println("Event received: " + event.toString());
		}
	}
}
