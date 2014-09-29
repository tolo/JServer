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

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import com.teletalk.jadmin.proxy.RemoteObjectProxy;

/**
 * This class is responsible for handling the data model of the server component tree.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */public final class AdminTreeModel extends DefaultTreeModel
{
	/**
    * 
    */
   private static final long serialVersionUID = 1L;

   /**
	 * Creates a new AdminTreeModel.	 */
	public AdminTreeModel(TreeNode newRoot)
	{
		super(newRoot);
	}

	/**
	 * Called when a node in the tree has changed.
	 */
	public void valueForPathChanged(final TreePath path, final Object newValue) 
	{
		DefaultMutableTreeNode aNode = (DefaultMutableTreeNode)path.getLastPathComponent();		if(newValue instanceof RemoteObjectProxy)			nodeHasChanged(aNode, (RemoteObjectProxy)newValue);
    }
	/**
	 * Called when a node in the tree has changed.
	 */
	public void nodeHasChanged(final DefaultMutableTreeNode node, final RemoteObjectProxy newData)
	{
		if( (node != null) && (newData instanceof RemoteObjectProxy) )
      {
			node.setUserObject(newData);      }
		nodeChanged(node);
	}
}
