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

import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import com.teletalk.jadmin.gui.AdminMainPanel;
import com.teletalk.jadmin.proxy.JServerProxy;
import com.teletalk.jadmin.proxy.PropertyProxy;
import com.teletalk.jadmin.proxy.RemoteObjectProxy;

/**
 * This class is responsible for handing the GUI of the server component tree on the overview page.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */public final class AdminTree extends JScrollPane
{
   private static final long serialVersionUID = 1L;
   
   
   private final JTree tree;
	private DefaultMutableTreeNode root;
	private AdminTreeModel treeModel;		private final AdminTreeCellRenderer cellRenderer;
		private final JServerProxy proxy;
		/**
	 * Creates a new AdminTree.
	 */
	public AdminTree(final JServerProxy proxy, final AdminMainPanel mainPanel)
	{
		super();

		this.proxy = proxy;
					root = new DefaultMutableTreeNode(proxy);		treeModel = new AdminTreeModel(root);		
		final AdminTree thisAdminTree = this;
		
		tree = new JTree(treeModel)
		{
			/**
          * 
          */
         private static final long serialVersionUID = 1L;

         /**
	 		 * Overridden to enable tool tips for nodes.
			 */
			public String getToolTipText(MouseEvent event)
			{
				return thisAdminTree.getToolTipText(event);
			}
		};
				//tree.addKeyListener(mainPanel);
		mainPanel.setKeyActions(tree);
		
		cellRenderer = new AdminTreeCellRenderer();		
		tree.setCellRenderer(cellRenderer);

		tree.setRowHeight(-1);
				
		getViewport().add(tree);
		
		// Register tree in tooltip manager to make it possible to display tooltips for renderer components (KeyValueLabels)
		ToolTipManager.sharedInstance().registerComponent(tree);	}
	
	/**
	 * Clears all elements from the tree.
	 */
	public DefaultMutableTreeNode clearTree()
	{
		root = new DefaultMutableTreeNode(proxy);
		treeModel = new AdminTreeModel(root);
		tree.setModel(treeModel);
		tree.repaint();				return root;
	}
	
	/**
	 * Gets the JTree components used for the component tree.
	 */
	public JTree getTree()
	{
		return tree;	
	}	
	/**
	 * Gets the root node.
	 */	public DefaultMutableTreeNode getRootNode()	{		return root;	}

	/**
	 * Updates the tree.
	 */
	public void update()
	{
		validate();
		repaint();
		tree.validate();
		tree.repaint();
	}

	/**
	 * Makes the specified path visible in the tree and selects the item at the end of the path.
	 */
	public void select(final TreePath path)
	{
		try
		{
			tree.makeVisible(path);
			tree.setSelectionPath(path);
			tree.validate();
			tree.repaint();
		}
		catch(Exception e)
		{	
			System.err.println("AdminTree:select - " + e);
		}
	}
	
	/**
	 * Makes the specified path visible in the tree.
	 */
	public void makeVisible(final TreePath path)
	{
		tree.makeVisible(path);
	}
	
	/**
	 * Called to indicate that the specified node has changed it's characteristics.
	 */
	public void nodeChanged(final DefaultMutableTreeNode node)
	{
		treeModel.nodeChanged(node);
	}		/**
	 * Called to indicate that the specified node has changed and is to be updated with the specified RemoteObjectProxy data.
	 */
	public void nodeChanged(final DefaultMutableTreeNode node, final RemoteObjectProxy data)
	{
		treeModel.nodeHasChanged(node, data);
	}

	/**
	 * Adds a mouse listener to the tree.
	 */
	public void addTreeMouseListener(final MouseListener ml)
	{
		tree.addMouseListener(ml);
	}

	/**
	 * Gets the node at the specified position.
	 */
	public DefaultMutableTreeNode getNodeAt(final int x, final int y)
	{
		TreePath selPath = tree.getPathForLocation(x, y);
		
		if(selPath != null)
		{
		    return (DefaultMutableTreeNode)selPath.getLastPathComponent();
		}
		return null;
	}
	
	/**
	 * Gets the data of the node at the specified position.
	 */
	public RemoteObjectProxy getNodeDataAt(int x, int y)
	{
		 DefaultMutableTreeNode node = getNodeAt(x, y);
		
		if(node != null)
			return (RemoteObjectProxy)node.getUserObject();
		else return null;
	}
	
	/**
	 * Gets the selected node.
	 */
	public DefaultMutableTreeNode getSelectedNode()
	{
		TreePath selPath = tree.getSelectionPath();

		if(selPath != null)
		{
		    return (DefaultMutableTreeNode)selPath.getLastPathComponent();
		}
		return null;
	}
	
	/**
	 * Gets the data of the selected node.
	 */
	public RemoteObjectProxy getSelectedNodeData()
	{
		DefaultMutableTreeNode node = getSelectedNode();		
		if(node != null)
			return (RemoteObjectProxy)(node.getUserObject());		else			return null;
	}		/**
	 * Creates a node for the specified data and adds it to the tree under the specified parent node.
	 */	public DefaultMutableTreeNode addNode(final RemoteObjectProxy data, final DefaultMutableTreeNode parent)	{		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(data, true);
		
		treeModel.insertNodeInto(newNode, parent, parent.getChildCount());
		
		return newNode;	}
	
	/**
	 * Creates a node for the specified data and adds it to the tree under the specified parent node. The node will be added at the specified index under the parent.
	 */	public DefaultMutableTreeNode addNode(final RemoteObjectProxy data, final DefaultMutableTreeNode parent, final int index)	{		DefaultMutableTreeNode newNode = new DefaultMutableTreeNode(data, true);
		
		treeModel.insertNodeInto(newNode, parent, index);
		
		return newNode;	}		/**
	 * Creates a node for the specified data and adds it to the tree under the root node.
	 */	public DefaultMutableTreeNode addNode(final RemoteObjectProxy data)	{		return addNode(data, root);	}
	
	/**
	 * Creates a node for the specified data and adds it to the tree under the root node, at the specified index.
	 */	public DefaultMutableTreeNode addNode(final RemoteObjectProxy data, final int index)	{		return addNode(data, root, index);	}		/**
	 * Adds the specified node under the root node of the tree.
	 */	public void addNode(final DefaultMutableTreeNode node)	{		treeModel.insertNodeInto(node, root, root.getChildCount());
	}
	
	/**
	 * Adds the specified node at the specified index under the root node of the tree.
	 */	public void addNode(final DefaultMutableTreeNode node, final int index)	{		treeModel.insertNodeInto(node, root, index);
	}		/**
	 * Adds the specified node under the specified parent node in the tree.
	 */	public void addNode(final DefaultMutableTreeNode node, final DefaultMutableTreeNode parent)	{		treeModel.insertNodeInto(node, parent, parent.getChildCount());
	}		/**
	 * Adds the specified node to the specified index under the specified parent node in the tree.
	 */	public void addNode(DefaultMutableTreeNode node, DefaultMutableTreeNode parent, int index)	{		treeModel.insertNodeInto(node, parent, index);
	}
	
	/**
	 * Removes a node from the tree.
	 */
	public void removeNode(final DefaultMutableTreeNode node, final DefaultMutableTreeNode parent)
	{
		try
		{
			treeModel.removeNodeFromParent(node);
			treeModel.nodeChanged(parent);
		}
		catch(Exception e)
		{	
			System.err.println("AdminTree:removeNode - " + e);
		}
	}
	
	/**
	 * Removes a node from the tree.
	 */
	public void removeNode(final DefaultMutableTreeNode node)
	{
		try
		{
			treeModel.removeNodeFromParent(root);
			treeModel.nodeChanged(root);
		}
		catch(Exception e)
		{	
			System.err.println("AdminTree:removeNode - " + e);
		}
	}
	
	/**
	 * Overridden to enable tool tips for nodes.
	 */
	public String getToolTipText(MouseEvent event)
	{
		DefaultMutableTreeNode node = this.getNodeAt(event.getX(),event.getY());
		
		if( node != null )
		{
			RemoteObjectProxy userObject = (RemoteObjectProxy)node.getUserObject();
			
			if( (userObject != null) && (userObject instanceof PropertyProxy) )
			{
				if(userObject instanceof PropertyProxy)
				{
					PropertyProxy property = (PropertyProxy)userObject;
					String descr = property.getDescription();
					
					if(property.getType() != PropertyProxy.VECTOR_TYPE)
					{
						int mode = property.getModificationMode();
						
						switch( mode )
						{
							case PropertyProxy.MODIFIABLE_NO_RESTART:
							{
								return descr;
							}
							case PropertyProxy.MODIFIABLE_SERVER_RESTART:
							{
								if(descr != null) return descr;
								else return "Double-click to modify. Warning! Changing this property will trigger a server restart!";
							}
							case PropertyProxy.MODIFIABLE_OWNER_RESTART:
							{
								if(descr != null) return descr;
								else return "Double-click to modify. Warning! Changing this property will trigger a property owner restart!";
							}
							case PropertyProxy.NOT_MODIFIABLE:
							{
								return descr;
							}
						}
					}
					else
					{
						if(descr != null) return descr;
						else return "Double-click to view/modify.";
					}
				}
			}
		}
		
		return null;
	}
}
