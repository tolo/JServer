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
package com.teletalk.jadmin.proxy;

import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import com.teletalk.jadmin.JAdmin;
import com.teletalk.jadmin.proxy.messages.AppenderComponentGetLogsRequest;
import com.teletalk.jadmin.proxy.messages.AppenderComponentGetLogsResponse;
import com.teletalk.jadmin.proxy.messages.LogRequestMessage;
import com.teletalk.jadmin.proxy.messages.LogRequestResultMessage;
import com.teletalk.jadmin.proxy.messages.LoggerRequestMessage;
import com.teletalk.jadmin.proxy.messages.LoggerRequestResultMessage;
import com.teletalk.jadmin.proxy.messages.ModifyPropertyMessage;
import com.teletalk.jadmin.proxy.messages.SubComponentControlMessage;
import com.teletalk.jadmin.proxy.messages.VectorPropertyUpdateMessage;
import com.teletalk.jserver.JServerConstants;
import com.teletalk.jserver.log.LogData;
import com.teletalk.jserver.log.LogFilter;
import com.teletalk.jserver.rmi.remote.RemoteAppenderComponent;
import com.teletalk.jserver.rmi.remote.RemoteEvent;
import com.teletalk.jserver.rmi.remote.RemoteJServer;
import com.teletalk.jserver.rmi.remote.RemoteLogEvent;
import com.teletalk.jserver.rmi.remote.RemoteLogger;
import com.teletalk.jserver.rmi.remote.RemoteLoggingEvent;
import com.teletalk.jserver.rmi.remote.RemoteObject;
import com.teletalk.jserver.rmi.remote.RemoteProperty;
import com.teletalk.jserver.rmi.remote.RemotePropertyData;
import com.teletalk.jserver.rmi.remote.RemotePropertyEvent;
import com.teletalk.jserver.rmi.remote.RemoteStatusEvent;
import com.teletalk.jserver.rmi.remote.RemoteStructureEvent;
import com.teletalk.jserver.rmi.remote.RemoteSubComponent;
import com.teletalk.jserver.rmi.remote.RemoteSubComponentData;
import com.teletalk.jserver.rmi.remote.RemoteSubSystem;
import com.teletalk.jserver.rmi.remote.RemoteSubSystemData;
import com.teletalk.jserver.rmi.remote.RemoteVectorPropertyEvent;
import com.teletalk.jserver.util.MessageQueue;

/**
 * This class implements a proxy object for a remote JServer. Just like in on the server side, this object 
 * is the top element in the server component hierarchy. This class handles the most of the communication 
 * between the administration tool and the server, including as receiving events from the server.
 * 
 * @author Tobias Löfstrand
 * 
 * @since 1.0
 */
public class JServerProxy extends SubSystemProxy implements Runnable 
{
	private boolean canRun;
	private boolean initialized;
	//private int numberOfRestarts;
	//private long lastRestart;
	private Thread thread;
	
	private final RemoteJServer remoteJServer;
	
	private JAdmin admin;
	
	private MessageQueue msgQueue;
	
	private ArrayList loggers; // For version 1.x compatability
   private ArrayList appenderComponents;
	private ArrayList vectorProperties;
	
	private RemoteSubSystemData systemTreeData;
	
	/**
	 * Creates a new JServerProxy.
	 */
	public JServerProxy(JAdmin admin, RemoteJServer remoteJServer, RemoteSubSystemData data) throws RemoteException
	{
		super(data);

		this.admin = admin;
		this.remoteJServer = remoteJServer;
		this.remoteObject = remoteJServer;  //Set the remote object refence in subclass RemoteObjectProxy. Needed for method getRemoteObject().
		this.systemTreeData = data;
		
		this.jserverProxy = this;
				
		this.loggers = new ArrayList();
      this.appenderComponents = new ArrayList();
		this.vectorProperties = new ArrayList();
		
		this.msgQueue = new MessageQueue();
		
		this.initialized = false;
	}
			
	/**
	 * Starts the thread of this JServerProxy.
	 */
	public void start()
	{
		this.buildTree();
		this.initialized = true;
		
		canRun = true;
					
		thread = new Thread(this, "JServerProxy Thread");
		thread.start();
	}
	
	/**
	 * Stops the thread of this JServerProxy.
	 */
	private void stop()
	{
		canRun = false;
		initialized = false;
			
		if(thread != null)
			thread.interrupt();
		thread = null;
			
		synchronized(this)
		{
			notifyAll();
		}
	}
		
	/**
	 * Destroys this JServerProxy.
	 */
	public void destroy()
	{
		this.stop();
		
		List p = getProperties();
		
		for(int i=0; i<p.size(); i++)
			removePropertyProxy( (PropertyProxy)p.get(i) );
		
		List sc = getSubComponents();
		
		for(int i=0; i<sc.size(); i++)
			removeSubComponentProxy( (SubComponentProxy)sc.get(i) );
		
		List s = getSubSystems();
		
		for(int i=0; i<s.size(); i++)
			removeSubSystemProxy( (SubSystemProxy)s.get(i) );
		
		loggers.clear();
		vectorProperties.clear();
	}
	
	/**
	 * Writes a message to the event log of JAdmin.
	 */
	void recordMessage(String msg)
	{
		admin.recordMessage(msg);
	}
	
	/**
	 * Called when a new proxy object has been added.
	 */
	void proxyAdded(final RemoteObjectProxy proxy, final RemoteObjectProxy parent)
	{
		admin.nodeAdded(proxy, parent);
	}
	
	/**
	 * Called when a proxy object has been removed.
	 */
	void proxyRemoved(final RemoteObjectProxy proxy)
	{
		admin.nodeRemoved(proxy);
	}
	
	/**
	 * Called when a proxy object has been updated.
	 */
	void proxyUpdated(final RemoteObjectProxy proxy)
	{
		admin.nodeUpdated(proxy);
	}
	
	/**
	 * Method for receiving events from the server.
	 * 
	 * @param event the event.
	 */
	public void receiveEvent(final RemoteEvent event)
	{
		if(initialized)
		{
			try
			{
				if( (event instanceof RemoteLoggingEvent) || (event instanceof RemoteLogEvent) )
				{
					admin.getAdminMainPanel().handleEvent(event);
				}
				else if(event instanceof RemotePropertyEvent)
				{
					RemotePropertyEvent r = (RemotePropertyEvent)event;

					RemoteObjectProxy proxy = findProxy(r.getPropertyName());

               if( (proxy != null) && (proxy instanceof PropertyProxy) )
   				{
						//RemotePropertyEvent r = (RemotePropertyEvent)event;
						((PropertyProxy)proxy).propertyValueChanged(r.getValue()); 
						
						if( (event instanceof RemoteVectorPropertyEvent) && (proxy instanceof VectorPropertyProxy) )
						{
							RemoteVectorPropertyEvent rvpe = (RemoteVectorPropertyEvent)r;
							
							if(rvpe.isAddEvent())
							{
								((VectorPropertyProxy)proxy).itemAdded(rvpe.getItemKeys(), rvpe.getItemDescriptions());
							}
							else if(rvpe.isModificationEvent())
							{
								((VectorPropertyProxy)proxy).itemModified(rvpe.getItemKeys(), rvpe.getItemDescriptions());
							}
							else if(rvpe.isRemoveEvent())
							{
								((VectorPropertyProxy)proxy).itemRemoved(rvpe.getItemKeys(), rvpe.getItemDescriptions());
							}
							else if(rvpe.isClearEvent())
							{
								((VectorPropertyProxy)proxy).clearItems();
							}
						}

						proxyUpdated(proxy);
					}
				}
				else if(event instanceof RemoteStatusEvent)
				{
					RemoteStatusEvent rse = (RemoteStatusEvent)event;

					RemoteObjectProxy proxy = findProxy(rse.getSourceName());
					
   				if(proxy != null)
   				{
   						if(proxy instanceof SubComponentProxy)
   						{
   							((SubComponentProxy)proxy).setStatus(rse.getStatus());
   								
   							recordMessage(proxy.getFullName() + " status changed to " + rse.getStatusAsString());
   
   							proxyUpdated(proxy);
   							
   							if(proxy == this)
   								admin.topSystemStateChanged();
   						}
               }
               
				}
				else if(event instanceof RemoteStructureEvent)
				{
					RemoteStructureEvent rse = (RemoteStructureEvent)event;

					if(rse.getStructureModification() == RemoteStructureEvent.ADDED)
					{
						SubComponentProxy parent = null;
						RemoteObjectProxy proxy = findProxy(rse.getSourceName());
						
						if(proxy != null && proxy instanceof SubComponentProxy)
                  {
							parent = (SubComponentProxy)proxy;
                  }
						
						if(parent != null)
						{
   							Object remoteObjectData = rse.getRemoteTargetData();
   							
   							if(remoteObjectData != null)
   							{
   								if(remoteObjectData instanceof RemotePropertyData)
   								{
   									PropertyProxy propertyProxy = new PropertyProxy(this, parent, (RemotePropertyData)remoteObjectData);
   									parent.addPropertyProxy(propertyProxy);
   									
   									recordMessage(propertyProxy.getFullName() + " added");
   								}
   								else if(remoteObjectData instanceof RemoteSubSystemData)
   								{
   									SubSystemProxy subSystemProxy;
   									if(parent instanceof SubSystemProxy)
   										subSystemProxy = new SubSystemProxy(this, (SubSystemProxy)parent, (RemoteSubSystemData)remoteObjectData);
   									else
   										subSystemProxy = new SubSystemProxy(this, null, (RemoteSubSystemData)remoteObjectData);
   									((SubSystemProxy)parent).addSubSystemProxy(subSystemProxy);
   									subSystemProxy.buildSubSystemProxies(((RemoteSubSystemData)remoteObjectData).getSubSystems());
   									subSystemProxy.buildSubComponentProxies(((RemoteSubSystemData)remoteObjectData).getSubComponents());
   									subSystemProxy.buildPropertyProxies(((RemoteSubSystemData)remoteObjectData).getProperties());
   									
   									recordMessage("SubSystem " + subSystemProxy.getFullName() + " added");
   								}
   								else if(remoteObjectData instanceof RemoteSubComponentData)
   								{
   									SubComponentProxy subComponentProxy = new SubComponentProxy(this, parent, (RemoteSubComponentData)remoteObjectData);
   									parent.addSubComponentProxy(subComponentProxy);
   									subComponentProxy.buildSubComponentProxies(((RemoteSubComponentData)remoteObjectData).getSubComponents());
   									subComponentProxy.buildPropertyProxies(((RemoteSubComponentData)remoteObjectData).getProperties());
   									
   									recordMessage("SubComponent " + subComponentProxy.getFullName() + " added");
   								}
   							}
						}
					}
					else
					{
						SubComponentProxy parent = null;
						RemoteObjectProxy proxy = findProxy(rse.getSourceName());
						
						if(proxy != null && proxy instanceof SubComponentProxy)
                  {
							parent = (SubComponentProxy)proxy;
                  }

						if(parent != null)
						{
                     proxy = findProxy(rse.getSourceName() + "." + rse.getTargetName());
                     							
							if(proxy != null)
							{
   								if(proxy instanceof SubSystemProxy)
   									((SubSystemProxy)parent).removeSubSystemProxy((SubSystemProxy)proxy);
   								else if(proxy instanceof SubComponentProxy)
   									parent.removeSubComponentProxy((SubComponentProxy)proxy);
   								else
   									parent.removePropertyProxy((PropertyProxy)proxy);
							
								recordMessage(rse.getSourceName() + "." + rse.getTargetName() + " removed");
							}
						}
					}
				}
			}
			catch(Exception e)
			{
				recordMessage("Error while receiving event (" + e + ")");
				e.printStackTrace();
			}
		}
	}
	
	/**
	 * Puts a message in the message queue of this JServerProxy.
	 */
	public void putMsg(Object msg)
	{
		msgQueue.putMsg(msg);	
	}
	
	/**
	 * Puts a message at the beginning of the message queue of this JServerProxy.
	 */
	public void putUrgentMsg(Object msg)
	{
		msgQueue.putUrgentMsg(msg);	
	}
	
	/**
	 * The thread method of this JServerProxy. This method perform periodic updates and performs communication with the server.
	 */
	public void run()
	{
		long buildInterval = 60*1000;
		long nextBuild = System.currentTimeMillis() + buildInterval;
		long nextVectorPropertyUpdate = System.currentTimeMillis() + VectorPropertyProxy.getUpdateInterval();
		long waitTime = 9*1000;
		
		Object msg;

		while(canRun)
		{
			try
			{				
				if(vectorProperties.size() > 0)
				{
					if((System.currentTimeMillis() > nextVectorPropertyUpdate) && VectorPropertyProxy.isAutoUpdateEnabled())
					{
						ArrayList vps = (ArrayList)vectorProperties.clone();
						VectorPropertyProxy vp;
						
						for(int i=0; i<vps.size(); i++)
						{
							vp = (VectorPropertyProxy)vps.get(i);
							vp.forceUpdate();
						}
									
						nextVectorPropertyUpdate = System.currentTimeMillis() + VectorPropertyProxy.getUpdateInterval();
					}
				}
				else
				{
					nextVectorPropertyUpdate = Long.MAX_VALUE;	
				}
				
				long currTime = System.currentTimeMillis();

				if(currTime > nextBuild)
				{
					reBuildTree();
					nextBuild = currTime + buildInterval;
				}
				
				if(!VectorPropertyProxy.isAutoUpdateEnabled())
					waitTime = nextBuild - currTime;
				else
					waitTime = Math.min(nextBuild - currTime, nextVectorPropertyUpdate - currTime);
								
				if(waitTime <= 0) waitTime = 1;

				try
				{
					msgQueue.waitForData(waitTime);
				}
				catch(InterruptedException ie){}

				msg = msgQueue.getMsgIfAny();

				if(msg != null)
				{
					if(msg instanceof VectorPropertyUpdateMessage)
					{
						nextVectorPropertyUpdate = System.currentTimeMillis() - 1;
					}
               else if(msg instanceof AppenderComponentGetLogsRequest)
               {
                  AppenderComponentGetLogsRequest appenderComponentGetLogsRequest = (AppenderComponentGetLogsRequest)msg;
                  
                  RemoteObject ro = appenderComponentGetLogsRequest.getAppenderComponent().getRemoteObject();
                  
                  if( (ro != null) && (ro instanceof RemoteAppenderComponent) )
                  {
                     LogData[] logs = ((RemoteAppenderComponent)ro).getLogs();
                     recordMessage("Received logs for " + appenderComponentGetLogsRequest.getAppenderComponent().getName());

                     admin.getAdminMainPanel().handleEvent(new AppenderComponentGetLogsResponse(appenderComponentGetLogsRequest, logs));
                  }
                  else recordMessage("Unable to get logs for remote appender component (" + appenderComponentGetLogsRequest.getAppenderComponent().getName() + ")! Remote object is null or not appender component!");
               }
               else if(msg instanceof LoggerRequestMessage)
					{
						LoggerRequestMessage lrm = (LoggerRequestMessage)msg;

						RemoteObject ro = lrm.logger.getRemoteObject();
						if(ro != null)
						{
							List logs = ((RemoteLogger)ro).getAvailableLogs();
							recordMessage("Received LoggerRequestMessage for " + lrm.logger.getName());

                     admin.getAdminMainPanel().handleEvent(new LoggerRequestResultMessage(lrm, logs));
						}
						else recordMessage("Unable to get available logs for remote logger (" + lrm.logger.getName() + ")! Remote logger is null!");
					}
               else if(msg instanceof LogRequestMessage)
					{
						final LogRequestMessage lrm = (LogRequestMessage)msg;
						
						//Dispatch request in separate thread, as it may take a while to complete...
						final Thread fireAndForget = new Thread("LogRequestMessage thread")
						{
							public void run()
							{
								try
								{
									RemoteObject ro = lrm.logger.getRemoteObject();
									if(ro != null)
									{
										List logMessages = ((RemoteLogger)ro).getLogMessages(lrm.logName, lrm.filter);
										recordMessage("Received LogRequestMessage for " + lrm.logger.getName() + "(" + lrm.logName + ")");

										admin.getAdminMainPanel().handleEvent(new LogRequestResultMessage(lrm, logMessages));
									}
									else
										recordMessage("Unable to get logmessages in log '" + lrm.logName + "' from remote logger (" + lrm.logger.getName() + ")! Remote logger is null!");
								}
								catch(Exception e)
								{
									recordMessage("Error while getting logmessages - " + e + "! ");
								}
							}
						};
					
						fireAndForget.setDaemon(true);
						fireAndForget.start();
					}
					else if(msg instanceof ModifyPropertyMessage)
					{
						ModifyPropertyMessage mpm = (ModifyPropertyMessage)msg;

						RemoteProperty rp = mpm.proxy.getRemoteProperty();
						if(rp != null)
						{
							if(!rp.setValue(mpm.newValue)) recordMessage("Failed to set value for " + mpm.proxy.getFullName());
						}
						else
                  {
							recordMessage("Unable to modify value of property '" + mpm.proxy.getFullName() + "'! Remote property is null!");
                  }
					}
					else if(msg instanceof SubComponentControlMessage)
					{
                  SubComponentControlMessage sscm = (SubComponentControlMessage)msg;
						
						//RemoteSubSystem rs = sscm.proxy.getRemoteSubSubSystem();
                  RemoteSubComponent rsc = sscm.proxy.getRemoteSubComponent();
                  
                  boolean result = false;
						
						if(sscm.control == SubComponentControlMessage.ENGAGE)
						{
							if(rsc != null)
							{
                        if( this.admin.getAdministrator().getCurrentProtocolVersion() == 1 ) // JServer version 1.X
                        {
                           if( rsc instanceof RemoteSubSystem )
                           {
                              RemoteSubSystem rss = (RemoteSubSystem)rsc;
                              result = rss.engage();
                           }
                           else
                           {
                              result = rsc.enable();
                           }
                        }
                        else
                        {
                           result = rsc.engage();
                        }
                        
                        if( !result ) recordMessage("Failed to engage " + sscm.proxy.getFullName());
							}
							else recordMessage("Failed to engage " + sscm.proxy.getFullName() + "! Remote object is null!");
						}
						else if(sscm.control == SubComponentControlMessage.SHUTDOWN)
						{
							if(sscm.proxy == this)
							{
								remoteJServer.shutDownServer();
								admin.disconnectedFromServer();
							}
							else
							{
								if(rsc != null)
								{
                           if( this.admin.getAdministrator().getCurrentProtocolVersion() == 1 ) // JServer version 1.X
                           {
                              if( rsc instanceof RemoteSubSystem )
                              {
                                 RemoteSubSystem rss = (RemoteSubSystem)rsc;
                                 result = rss.shutDown();
                              }
                              else
                              {
                                 result = rsc.disable();
                              }
                           }
                           else
                           {
                              result = rsc.shutDown();
                           }
                           
                           if( !result ) recordMessage("Failed to shut down " + sscm.proxy.getFullName());
								}
								else recordMessage("Failed to shut down " + sscm.proxy.getFullName() + "! Remote object is null!");
							}
						}
						else if(sscm.control == SubComponentControlMessage.REINITIALIZE)
						{
							if(sscm.proxy == this)
							{
								remoteJServer.restartServer();
								admin.disconnectedFromServer();
							}
							else
							{
                        if(rsc != null)
								{
                           if( this.admin.getAdministrator().getCurrentProtocolVersion() == 1 ) // JServer version 1.X
                           {
                              if( rsc instanceof RemoteSubSystem )
                              {
                                 RemoteSubSystem rss = (RemoteSubSystem)rsc;
                                 result = rss.reinitialize();
                              }
                              else
                              {
	                             result = rsc.disable();
	                             if( result ) rsc.enable();
                              }
                           }
                           else
                           {
                              result = rsc.reinitialize();
                           }
                           
                           if( !result ) recordMessage("Failed to reinitialize " + sscm.proxy.getFullName());
								}
								else recordMessage("Failed to reinitialize " + sscm.proxy.getFullName() + "! Remote object is null!");
							}
						}
						else if(sscm.control == SubComponentControlMessage.KILL)
						{
							if(sscm.proxy == this)
							{
								remoteJServer.killServer();
								admin.disconnectedFromServer();
							}
						}
					}
				}
			}
			catch(RemoteException e)
			{
			   e.printStackTrace();
				if(canRun && !checkServerConnection())
				{
					canRun = false;
					recordMessage("Fatal error while communicating with server - " + e + ". Disconnecting from server!");
					admin.disconnectedFromServer();
				}
				else
					recordMessage("Error while communicating with server - " + e + ".");
			}
			catch(Exception e)
			{
				if(canRun && !checkServerConnection())
				{
					canRun = false;
					recordMessage("Fatal error while communicating with server - " + e + ". Disconnecting from server!");
					admin.disconnectedFromServer();
				}
				else
					recordMessage("Error while communicating with server - " + e + ".");
				
				e.printStackTrace();
			}
		}
		
		stop();
	}
	
	/**
	 * Checks if the administration tool still has contact with the server.
	 */
	public boolean checkServerConnection()
	{
		try
		{
			remoteJServer.getFullName();
			return true;
		}
		catch(Exception e)
		{
			return false;
		}
	}
	
	/**
	 * Finds the remote object that matches the specified proxy object.
	 */
	public RemoteObject findRemoteObject(final RemoteObjectProxy proxy) throws RemoteException
	{
		if(proxy instanceof SubSystemProxy)
		{
			try
			{
				return remoteJServer.findRemoteSubSystem(proxy.getFullName(), false);
			}catch(Exception e)
			{
				recordMessage("Failed to get remote object for " + getFullName() + "! Attempting to get a default remote object.");
				return remoteJServer.findRemoteSubSystem(proxy.getFullName(), true);
			}
		}
		else if(proxy instanceof SubComponentProxy)
		{
			return remoteJServer.findRemoteSubComponent(proxy.getFullName());
		}
		else if(proxy instanceof PropertyProxy)
		{
			return remoteJServer.findRemoteProperty(proxy.getFullName());
		}
		else
			return null;
	}

	/**
	 * Finds the remote object with the specified name.
	 */
	private RemoteObjectProxy findProxy(final String fullName)
	{
		RemoteObjectProxy proxy = null;
		RemoteObjectProxy p = null;
		String[] names = parseNames(fullName);
		
		if( names.length > 1 ) //&& names[0].equals(getName()))
		{
			proxy = this;
				
			for(int i=1; i<names.length; i++)
			{
				if(proxy instanceof SubSystemProxy)
				{
					p = ((SubSystemProxy)proxy).getSubSystemProxy(names[i]);
					if(p == null)
					{
						p = ((SubSystemProxy)proxy).getSubComponentProxy(names[i]);
						if(p == null)
						{
							proxy = ((SubSystemProxy)proxy).getPropertyProxy(names[i]);
							break;
						}
						else proxy = p;
					}
					else proxy = p;
				}
				else if(proxy instanceof SubComponentProxy)
				{
					p = ((SubComponentProxy)proxy).getSubComponentProxy(names[i]);
					if(p == null)
					{
						proxy = ((SubComponentProxy)proxy).getPropertyProxy(names[i]);
						break;
					}
					else proxy = p;
				}
			}
		}
		else if(names.length == 1)
		{
			if( fullName.equals(getName()) || fullName.equals(JServerConstants.JSERVER_TOP_SYSTEM_ALIAS) ) proxy = this;
		}
		return proxy;
	}
	
	/**
	 * Extracts the name components from the specified full name.
	 */
	private String[] parseNames(String fullName)
	{
		StringTokenizer t = new StringTokenizer(fullName, ".");
		String[] names = new String[t.countTokens()];
		
		for(int i=0; t.hasMoreTokens(); i++)
		{
			names[i] = t.nextToken();
		}
		
		return names;
	}
	
	/**
	 * Gets system tree data from the server and rebuilds the tree of proxy objects.
	 */
	private void reBuildTree()
	{
		try
		{
			systemTreeData = this.remoteJServer.getSystemTreeData();
			buildTree();
		}
		catch(RemoteException e)
		{
			recordMessage(" Error while rebuilding system tree - " + e);
		}
	}
	
	/**
	 * Builds the tree of prox object based on system tree data previously fetched from the server.
	 */
	public void buildTree()
	{
		buildSubSystemProxies(systemTreeData.getSubSystems());
		buildSubComponentProxies(systemTreeData.getSubComponents());
		buildPropertyProxies(systemTreeData.getProperties());

		jserverProxy.proxyUpdated(this);
	}
	
	/**
	 * Restarts the server.
	 */
	public void restartServer()
	{
		msgQueue.putMsg(new SubComponentControlMessage(this, SubComponentControlMessage.REINITIALIZE));
	}

	/**
	 * Shuts down the server
	 */
	public void shutDownServer()
	{
		msgQueue.putMsg(new SubComponentControlMessage(this, SubComponentControlMessage.SHUTDOWN));
	}
	
	/**
	 * Kills the server without attempting to shut down first.
	 */
	public void killServer()
	{
		msgQueue.putMsg(new SubComponentControlMessage(this, SubComponentControlMessage.KILL));
	}
	
	/**
	 * Registers a vector property proxy for peridic updates in this JServerProxy.
	 */
	void addVectorProperty(final VectorPropertyProxy vProxy)
	{
		if(!vectorProperties.contains(vProxy))
			vectorProperties.add(vProxy);
	}
   
   /**
    * Registers a appender components proxy object with this JServerProxy.
    */
   void registerAppenderComponent(final SubComponentProxy appenderComponent)
   {
      if(!appenderComponents.contains(appenderComponents)) appenderComponents.add(appenderComponent);
   }
   
   /**
    * Gets all registered appender components proxy objects.
    */
   public final ArrayList getAppenderComponents()
   {
      return appenderComponents;
   }
	
	/**
	 * Registers a logger proxy object with this JServerProxy.
	 */
	void addLogger(final LoggerProxy logger)
	{
		if(!loggers.contains(logger)) loggers.add(logger);
	}
	
	/**
	 * Unregisters a logger proxy object with this JServerProxy.
	 */
	void removeLogger(final LoggerProxy logger)
	{
		loggers.remove(logger);
	}

	/**
	 * Gets all registered logger proxy objects.
	 */
	public final ArrayList getLoggers()
	{
		return loggers;
	}
   
   /**
    * Dispatches a request to get all logs for the specified appender component.
    */
   public final AppenderComponentGetLogsRequest getAppenderComponentLogs(final SubComponentProxy appenderComponent)
   {
      AppenderComponentGetLogsRequest msg = new AppenderComponentGetLogsRequest(appenderComponent);
      msgQueue.putMsg(msg);
      
      return msg;
   }
	
	/**
	 * Dispatches a request to get all logs for the specified logger.
	 */
	public final LoggerRequestMessage getLogs(final LoggerProxy logger)
	{
		LoggerRequestMessage msg = new LoggerRequestMessage(logger);
		msgQueue.putMsg(msg);
		
		return msg;
	}
	
	/**
	 * Dispatches a request to get the contents of a specific log from a specific logger.
	 */
	public final LogRequestMessage getLog(final LoggerProxy logger, final String logName, final LogFilter filter)
	{
		LogRequestMessage msg = new LogRequestMessage(logger, logName, filter);
		msgQueue.putMsg(msg);
		
		return msg;
	}
}
