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
package com.teletalk.jserver.rmi.client;

import java.lang.reflect.Constructor;
import java.rmi.RemoteException;
import java.rmi.server.RMIClassLoader;
import java.util.ArrayList;
import java.util.List;

import com.teletalk.jserver.rmi.remote.RemoteAdministration;
import com.teletalk.jserver.rmi.remote.RemoteAdministrator;
import com.teletalk.jserver.rmi.remote.RemoteEvent;
import com.teletalk.jserver.rmi.remote.RemoteEventListener;
import com.teletalk.jserver.rmi.remote.RemoteJServer;
import com.teletalk.jserver.util.ReflectionUtils;

/**
 * The Administrator class is a client side class that extends RmiClient with logic for getting custom administration panels.
 * 
 * @author Tobias Löfstrand
 * 
 * @since Alpha
 */
public class Administrator extends RmiClient implements RemoteAdministrator, Runnable
{
   static final long serialVersionUID = 6583721590363823871L;
   
	private RemoteAdministration remoteAdm;
	
	private AdministratorOwner owner;  //Reference to an AdministratorOwner, 
	
	private Thread thread;
	
	private boolean canRun = true;
	
	private List customAdministrationPanels;  // A List of custom administration panels loaded from the server.
	private boolean customAdministrationPanelsLoaded = false;
	
	/**
	 * Creates a new Administrator with specified id.
	 * 
	 * @exception RemoteException if there was an error when creating this Administrator object.
	 */
	public Administrator(final String id, final AdministratorOwner owner) throws RemoteException
	{
		super(id);
		this.owner = owner;
		
		this.customAdministrationPanels = new ArrayList();
	}
	
	/**
	 * Run when this Administrator is connecting to the server.
	 * 
	 * @exception Exception if there was an error connecting to the remote server.
	 */
	protected void doConnect() throws Exception
	{
		super.doConnect();

		remoteAdm = remoteServer.getAdministration();
		//CustomAdministrationPanel.remoteServer = this.remoteServer;
		
        CustomAdministrationPanel.setRemoteJServer(this.remoteServer);
        CustomAdministrationPanel.setAdministrator(this);
		
		startThread();
	}
	
	/**
	 * Disconnects this Administrator from the server.
	 */
	public void disconnect()
	{
      destroyCustomAdministrationPanels();
      
      killThread();
							   
		remoteAdm = null;
		super.disconnect();	
		CustomAdministrationPanel.remoteServer = null;
	}
	
	/**
	 * Called by the server to disconnect this Administrator.
	 */
	public void disconnectedFromServer()
	{
      destroyCustomAdministrationPanels();
      
      super.disconnectedFromServer();
		remoteAdm = null;

		owner.administratorDisconnectedFromServer();
		CustomAdministrationPanel.remoteServer = null;
	}
	
	//Destroys all current administrationpanels
	private synchronized void destroyCustomAdministrationPanels()
	{
		if(customAdministrationPanels != null)
		{
         CustomAdministrationPanel panel;

			//Destroy custom adminsitration panels
			for(int i=0; i<customAdministrationPanels.size(); i++)
			{
            panel = (CustomAdministrationPanel)customAdministrationPanels.get(i);

            if( panel != null )
            {
               owner.recordMessage("Destroying panel " + panel.getTitle() + ".");
               panel.destroyPanel();
            }
			}
			customAdministrationPanels.clear();
		}
		customAdministrationPanels = null;
	}
	
	/**
	 * Returns a reference to the RemoteJServer object.
	 * 
	 * @return a RemoteJServer object.
	 * 
	 * @see com.teletalk.jserver.rmi.remote.RemoteJServer
	 */
	public final RemoteJServer getRemoteJServer()
	{
		return remoteServer;
	}
   
   /**
    * 
    * @since 2.1.1 (20051010)
    */
   public boolean isRemoteJServerVersionOrLater(final int majorVersion, final int minorVersion, final int microVersion) throws RemoteException
   {
      int serverMajorVersion = this.remoteServer.getJServerVersionMajor();
      int serverMinorVersion = this.remoteServer.getJServerVersionMinor();
      int serverMicroVersion = this.remoteServer.getJServerVersionMicro();
      
      if( serverMajorVersion > majorVersion ) return true;
      else if( serverMajorVersion == majorVersion )
      {
         if( serverMinorVersion > minorVersion ) return true;
         else if( serverMinorVersion == minorVersion )
         {
            if( serverMicroVersion >= microVersion ) return true;
         }
      }
         
      return false;
   }
	
	/**
	 * Gets custom administrationpanels from the server to which this Administrator is connected.
	 * 
	 * @return a list of CustomAdministrationPanel objects.
	 */
	public synchronized final List getCustomAdministrationPanels() throws Exception
	{
		if(!customAdministrationPanelsLoaded)
		{
			this.customAdministrationPanelsLoaded = true;
			
			CustomAdministrationPanel panel;
			  
			String remoteCodeBase = (super.currentProtocolVersion > 1) ? super.jServerRmiInterface.getRmiCodeBase() : super.remoteHost.gerRmiCodeBase();
	
			owner.recordMessage("Getting custom administration panels. Remote codebase is " + remoteCodeBase + ".");
					
			List classNames = remoteAdm.getAdmPanelClassNames();
			String className = "<n/a>";
			List params;
	
			if(classNames.size() > 0)
			{
				Class[] admPanelClasses = new Class[classNames.size()];
						
				//Load admin panel classes
				for(int i=0; i<classNames.size(); i++)
				{
					try
					{
						className = (String)classNames.get(i);
						if(className == null) continue;
						
						owner.recordMessage("Attempting to load class " + className + ".");
						admPanelClasses[i] = RMIClassLoader.loadClass(remoteCodeBase, className);
					}
					catch(Exception e)
					{
						owner.recordMessage("Unable load class " + className + " - " + e);
						e.printStackTrace();
					}
				}
			
				//Get constructor parameters
				try
				{
					owner.recordMessage("Getting constructor parameters for custom administration panels.");
					params = remoteAdm.getAdmPanelConstructorParameters();
				}
				catch(Exception e)
				{
					owner.recordMessage("Error while getting constructor parameters for custom administration panels: " + e + ".");
					return new ArrayList();
				}
			
				//Create admin panel objects
				for(int i=0; i<admPanelClasses.length; i++)
				{
					Class c = admPanelClasses[i];
					if(c == null) continue;
					try
					{
						Object[] classParams = (Object[]) params.get(i);
						
						Constructor con = findConstructor(c, classParams);
						if(con != null)
						{
							owner.recordMessage("Creating " + c.getName() + ".");
							
							panel = (CustomAdministrationPanel)con.newInstance(classParams);
                            //panel.setRemoteJServer(this.remoteServer);
                            //panel.setAdministrator(this);
																					
							if( panel instanceof RemoteEventListener )
							{
								owner.recordMessage("Registering " + c.getName() + " as a remote event listener.");
								super.registerRemoteEventListener( (RemoteEventListener)panel );
							}
							
							this.customAdministrationPanels.add(panel);
						}
						else
						{
							StringBuffer errMsg;
							
							if(classParams.length > 0)
							{
								errMsg = new StringBuffer("Unable to find matching constructor for class " + c.getName() + ". Specified constructor parameter type are: ");
													
								for(int q=0; q<classParams.length; q++)
								{
									if(q > 0) errMsg.append(", ");
									errMsg.append(classParams[q].getClass().getName());
								}
							
								errMsg.append(".");
							}
							else
								errMsg = new StringBuffer("Unable to find no-arg constructor for class " + c.getName() + ".");
							
							owner.recordMessage(errMsg.toString());
						}
	
					}
					catch(Exception e)
					{
						owner.recordMessage("Error creating instance of class " + c.getName() + " - " + e);
						e.printStackTrace();
					}
				}
			}
			else
			{
				owner.recordMessage("No custom administration panels registered in remote server.");
			}
		}
		
		return this.customAdministrationPanels;
	}
	
	/**
	 * Find the constructor for objectClass that matches constructorParams.
	 */
	private Constructor findConstructor(Class objectClass, Object[] constructorParams) throws Exception
	{
      if(constructorParams != null)
		{
         return ReflectionUtils.findConstructor(objectClass, constructorParams);
		}
		
		return null;
	}
	
	/**
	 * Checked if this Administrator is connected to the server.
	 * 
	 * @return true if this object is connected to the sever, otherwise false.
	 */
	public final boolean isConnected()
	{
		return (remoteHost != null) &&
         ( (super.currentProtocolVersion == 1) ||
            ((super.currentProtocolVersion > 1) && (super.jServerRmiInterface != null)) );
	}
	
	/**
	 * Method for receiving events from the server.
	 * 
	 * @param event the event.
	 */
	public final void receiveEvent(final RemoteEvent event)
	{
		super.receiveEvent(event);
		owner.receiveEvent(event);
	}
	
	/**
	 * Starts the thread of this Administrator.
	 */
	private final void startThread()
	{
		canRun = true;
		thread = new Thread(this, "Alive check thread");
		thread.setDaemon(true);
		thread.start();
	}
	
	/**
	 * Stops the thread of this Administrator.
	 */
	private final void killThread()
	{
		canRun = false;
			
		if(thread != null)
			thread.interrupt();
		thread = null;
			
		synchronized(this)
		{
			notifyAll();
		}
	}
	
	/**
	 * The thread method of this Administrator. This thred performs regular alive 
	 * checks agains the server.
	 */
	public final void run()
	{
		while(canRun)
		{
			try
			{
				Thread.sleep(1000);
			}
			catch(InterruptedException ie)
			{
				if(!canRun) break;
			}
			
			if(canRun)
			{
				try
				{
                if( super.currentProtocolVersion > 1 ) super.jServerRmiInterface.aliveCheck();
                else remoteHost.aliveCheck();
				}
				catch(Exception e1)
				{
					try
					{
						super.disconnect();	
					}
					finally
					{
						owner.administratorDisconnectedFromServer();
						remoteAdm = null;
						CustomAdministrationPanel.remoteServer = null;
						canRun = false;
					}
				}
			}
		}
	}
}
