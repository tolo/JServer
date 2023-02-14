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

import java.awt.Component;
import java.awt.Font;

import javax.swing.ImageIcon;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreeCellRenderer;

import com.teletalk.jadmin.gui.util.ResourceHandler;
import com.teletalk.jadmin.proxy.JServerProxy;
import com.teletalk.jadmin.proxy.PropertyProxy;
import com.teletalk.jadmin.proxy.RemoteObjectProxy;
import com.teletalk.jadmin.proxy.SubComponentProxy;
import com.teletalk.jadmin.proxy.SubSystemProxy;
import com.teletalk.jserver.JServerConstants;

/**
 * Class responsible for rendering tree cell elements on the overview tree.
 * 
 * @author Tobias L�fstrand
 * 
 * @since Alpha
 */
{
	final ImageIcon topsystemIcon;
	final ImageIcon propertyIcon;
	final ImageIcon vectorPropertyIcon;
   
   final ImageIcon subcomponent_enabled;
   final ImageIcon subcomponent_engaging;
   final ImageIcon subcomponent_created;
   final ImageIcon subcomponent_restarting;
   final ImageIcon subcomponent_shutting_down;
   final ImageIcon subcomponent_down;
   final ImageIcon subcomponent_error;
   final ImageIcon subcomponent_critical_error;
	private final Font stdKeyFont;
	
	private final KeyValueLabel topSystemLabel;
	private final KeyValueLabel subSystemLabel;
	private final KeyValueLabel subComponentLabel;
	private final KeyValueLabel propertyLabel;
		
	/**
	 * Creates a new AdminTreeCellRenderer.
	 */
		this.propertyIcon = loadImage("/images/property/property.gif");
		this.vectorPropertyIcon = loadImage("/images/property/vector_property.gif");
      this.subcomponent_engaging = loadImage("/images/subcomponent/engaging.gif");
      this.subcomponent_created = loadImage("/images/subcomponent/created.gif");
      this.subcomponent_restarting = loadImage("/images/subcomponent/restarting.gif");
      this.subcomponent_shutting_down = loadImage("/images/subcomponent/shutting_down.gif");
      this.subcomponent_down = loadImage("/images/subcomponent/down.gif");
      this.subcomponent_error = loadImage("/images/subcomponent/error.gif");
      this.subcomponent_critical_error = loadImage("/images/subcomponent/critical_error.gif");
		this.stdKeyFont = new Font("SansSerif", Font.BOLD, 11);
		
		this.topSystemLabel = new KeyValueLabel();
		this.topSystemLabel.setKeyFont(stdBoldKeyFont);
		this.topSystemLabel.setValueFont(stdValueFont);
		
		this.subSystemLabel = new KeyValueLabel();
		this.subSystemLabel.setKeyFont(stdBoldKeyFont);
		this.subSystemLabel.setValueFont(stdValueFont);
				
		this.subComponentLabel = new KeyValueLabel();
		this.subComponentLabel.setKeyFont(stdBoldKeyFont);
		this.subComponentLabel.setValueFont(stdValueFont);
				
		this.propertyLabel = new KeyValueLabel();
		this.propertyLabel.setKeyFont(stdKeyFont);
		this.propertyLabel.setValueFont(stdValueFont);
	}
	/**
	 *Loads an image from an URL. 
	 */
	/**
	 * Destroys this object.
	 */

    /**
     * Gets a renderer component for a specific element in the tree.
     */
	public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)
	{
		KeyValueLabel keyValueLabel;
		
		RemoteObjectProxy userObject = (RemoteObjectProxy)((DefaultMutableTreeNode)value).getUserObject();
		if(userObject instanceof JServerProxy)
			keyValueLabel = this.topSystemLabel;
			keyValueLabel.setIcon(topsystemIcon);
		}
		{
			
			SubSystemProxy s = (SubSystemProxy)userObject;
			switch( s.getStatus() )
			{
				case JServerConstants.ENABLED: keyValueLabel.setIcon(subsystem_enabled); break;
				case JServerConstants.INITIALIZING: keyValueLabel.setIcon(subsystem_engaging); break;
				case JServerConstants.CREATED: keyValueLabel.setIcon(subsystem_created); break;
				case JServerConstants.REINITIALIZING: keyValueLabel.setIcon(subsystem_restarting); break;
				case JServerConstants.SHUTTING_DOWN: keyValueLabel.setIcon(subsystem_shutting_down); break;
            case 1: // Old DISABLED
				case JServerConstants.DOWN: keyValueLabel.setIcon(subsystem_down); break;
				case JServerConstants.ERROR: keyValueLabel.setIcon(subsystem_error); break;
				case JServerConstants.CRITICAL_ERROR: keyValueLabel.setIcon(subsystem_critical_error); break;
			}
		}
		else if(userObject instanceof SubComponentProxy)
		{
			
			SubComponentProxy s = (SubComponentProxy)userObject;
			
			switch( s.getStatus() )
			{
            case JServerConstants.ENABLED: keyValueLabel.setIcon(subcomponent_enabled); break;
            case JServerConstants.INITIALIZING: keyValueLabel.setIcon(subcomponent_engaging); break;
            case JServerConstants.CREATED: keyValueLabel.setIcon(subcomponent_created); break;
            case JServerConstants.REINITIALIZING: keyValueLabel.setIcon(subcomponent_restarting); break;
            case JServerConstants.SHUTTING_DOWN: keyValueLabel.setIcon(subcomponent_shutting_down); break;
            case 1: // Old DISABLED
            case JServerConstants.DOWN: keyValueLabel.setIcon(subcomponent_down); break;
            case JServerConstants.ERROR: keyValueLabel.setIcon(subcomponent_error); break;
            case JServerConstants.CRITICAL_ERROR: keyValueLabel.setIcon(subcomponent_critical_error); break;
         }
		}
		{
			
			PropertyProxy property = (PropertyProxy)userObject;
			if(property.getType() != PropertyProxy.VECTOR_TYPE)
			{
				int mode = property.getModificationMode();
				
				switch( mode )
				{
					case PropertyProxy.MODIFIABLE_NO_RESTART:
					{
						keyValueLabel.setIcon(modifiablePropertyIcon);
						
						break;
					}
					case PropertyProxy.MODIFIABLE_SERVER_RESTART:
					{
						keyValueLabel.setIcon(restartPropertyIcon);
						
						break;
					}
					case PropertyProxy.MODIFIABLE_OWNER_RESTART:
					{
						keyValueLabel.setIcon(restartPropertyIcon);
						
						break;
					}
				}
			}
			}
		}
		else
		{
			keyValueLabel = this.propertyLabel;
			keyValueLabel.setIcon(null);
			keyValueLabel.setKeyText(null);
			keyValueLabel.setValueText(null);
			userObject = null;
		}
		
		if( userObject != null )
		{
			keyValueLabel.setKeyText(userObject.getDisplayName());
			keyValueLabel.setValueText(userObject.getDisplayValue());
			keyValueLabel.setSelected(selected);
			keyValueLabel.calculateComponentSize();
		}
				
	}
}