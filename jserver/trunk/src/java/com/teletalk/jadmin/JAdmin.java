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
package com.teletalk.jadmin;

import java.awt.Frame;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JOptionPane;

import com.teletalk.jadmin.gui.AdminMainPanel;
import com.teletalk.jadmin.proxy.JServerProxy;
import com.teletalk.jadmin.proxy.RemoteObjectProxy;
import com.teletalk.jadmin.proxy.SubComponentProxy;
import com.teletalk.jadmin.proxy.SubSystemProxy;
import com.teletalk.jserver.rmi.client.Administrator;
import com.teletalk.jserver.rmi.client.AdministratorOwner;
import com.teletalk.jserver.rmi.remote.RemoteEvent;
import com.teletalk.jserver.rmi.remote.RemoteJServer;
import com.teletalk.jserver.rmi.remote.RemoteSubSystemData;

/**
 * JAdmin is the core class of the administration tool. This class handles communication between the server proxy
 * components and the graphical components of the administration tool.
 * 
 * @author Tobias Löfstrand
 * 
 * @since The very beginning
 * 
 * @version 2.0.1
 */
public final class JAdmin extends ThreadGroup implements AdministratorOwner
{
   /** The name of this application. */
   final static String appName = "JAdmin";

   /** The major version number. */
   final static short appVersionMajor = 2;

   /** The minor version number. */
   final static short appVersionMinor = 2;

   /** The micro version number. */
   final static short appVersionMicro = 0;

   /** The codename of this build. */
   final static String versionCodeName = "Aquarius";

   /** The type of this build (e.g. Debug or Release). */
   final static String buildType = (JAdmin.class.getPackage().getImplementationTitle() != null) ? JAdmin.class.getPackage().getImplementationTitle().trim() : "";

   // final static String buildType = (JAdmin.class.getPackage().getImplementationVersion() != null) ?
   // JAdmin.class.getPackage().getImplementationVersion() : ""; //"Debug";
   /** The build number. */
   final static short build = 763;

   /** The build id. */
   final static String buildId = (JAdmin.class.getPackage().getImplementationVersion() != null) ? JAdmin.class.getPackage().getImplementationVersion().trim() : "";

   /** Sub build name. */
   final static String buildExtra = null; // "RC" "Beta"

   private static final String appVersion = appVersionMajor + "." + appVersionMinor + "." + appVersionMicro;

   public static final String longVersionString; // = (appName + " " + appVersion + " (" + versionCodeName + " Build "
                                                   // + buildId + buildExtra + ( (buildType.length() > 0) ? " " : "") +
                                                   // buildType + ")").trim();

   public static final String versionString = appName + " " + appVersion;

   static
   {
      String buildString = null;
      if (buildId != null) buildString = "Build " + buildId;

      String extraString = "";
      if ((versionCodeName != null) || (buildString != null) || (buildExtra != null) || (buildType != null))
      {
         extraString = " (";

         if (versionCodeName != null) extraString += versionCodeName;

         if (buildString != null) extraString += ((versionCodeName != null) ? " " : "") + buildString;

         if (buildExtra != null) extraString += (((versionCodeName != null) || (buildString != null)) ? " " : "") + buildExtra;

         if (buildType != null) extraString += (((versionCodeName != null) || (buildString != null) || (buildExtra != null)) ? " " : "") + buildType;

         extraString += ")";
      }

      longVersionString = appName + " " + appVersionMajor + "." + appVersionMinor + "." + appVersionMicro + extraString;
   }

   private AdminTopGui adminGui;

   private Frame mainFrame;

   private AdminMainPanel adminMainPanel;

   private Administrator administrator;

   private JServerProxy proxy;

   private String id;

   private volatile boolean connected = false;

   /**
    * Creates a new JAdmin
    */
   public JAdmin(final AdminTopGui adminGui) throws Exception
   {
      super("JAdmin");

      if (System.getSecurityManager() == null)
      {
         System.setSecurityManager(new com.teletalk.jserver.util.AFreeWorld());
      }

      this.adminGui = adminGui;
      this.mainFrame = this.adminGui.getMainFrame();
      this.adminMainPanel = new AdminMainPanel(this, this.mainFrame);

      try
      {
         id = "JAdmin@" + InetAddress.getLocalHost().getHostAddress();
      }
      catch (Exception e)
      {
         id = "JAdmin@" + hashCode();
      }

      proxy = null;
   }

   /**
    * Gets the main panel in the GUI.
    * 
    * @return the main panel (an {@link AdminMainPanel} object).
    */
   public AdminMainPanel getAdminMainPanel()
   {
      return adminMainPanel;
   }
   

   /*
    * ########################### AdministrationOwner Methods begin ############################
    */

   /**
    * Run when the Administrator gets disconnected from the Server.
    */
   public void administratorDisconnectedFromServer()
   {
      this.disconnectedFromServer();
   }

   /**
    * Method for receiving events from the server.
    * 
    * @param event the event.
    */
   public void receiveEvent(RemoteEvent event)
   {
      if (proxy != null) proxy.receiveEvent(event);
   }

   /**
    * Records a message to a log.
    * 
    * @param msg a message.
    */
   public void recordMessage(String msg)
   {
      if (mainFrame != null && connected) adminMainPanel.writeToEventLog(msg);
   }

   /**
    * Gets the main frame of this application.
    * 
    * @return the main frame.
    */
   public Frame getMainFrame()
   {
      return mainFrame;
   }

   /**
    * Gets the name of this application.
    * 
    * @return the name of the administraion application implementing this interface.
    * @since 1.13 Build 601
    */
   public String getAdministrionApplicationName()
   {
      return appName;
   }

   /**
    * Gets the major version of this application. If the version is 1.23 then major version is 1.
    * 
    * @return the major version number.
    * @since 1.13 Build 601
    */
   public short getAdministrionApplicationVersionMajor()
   {
      return appVersionMajor;
   }

   /**
    * Gets the minor version of this application. If the version is 1.23 then minor version is 2.
    * 
    * @return the minor version number.
    * @since 1.13 Build 601
    */
   public short getAdministrionApplicationVersionMinor()
   {
      return appVersionMinor;
   }

   /**
    * Gets the micro version of this application. If the version is 1.23 then micro version is 3.
    * 
    * @return the micro version number.
    * @since 1.13 Build 601
    */
   public short getAdministrionApplicationVersionMicro()
   {
      return appVersionMicro;
   }

   /**
    * Gets a string containing version information about of this application.
    * 
    * @return the name of the administraion application implementing this interface.
    * @since 1.13 Build 601
    */
   public String getAdministrionApplicationVersionString()
   {
      return longVersionString;
   }

   /**
    * Gets the build of this application.
    * 
    * @return the name of the administraion application implementing this interface.
    * @since 1.13 Build 601
    * @deprecated as of 2.0.1
    */
   public String getAdministrionApplicationBuild()
   {
      return String.valueOf(build);
   }

   /*
    * ########################### AdministrationOwner Methods end ############################
    */

   /**
    * @return Returns the administrator.
    */
   public Administrator getAdministrator()
   {
      return this.administrator;
   }

   /**
    * Gets the JServerProxy object representing the server which the administration tool is currently connected to.
    * 
    * @return a {@link JServerProxy} object.
    */
   public final JServerProxy getJServerProxy()
   {
      return proxy;
   }

   /**
    * Connects this administration tool a remote server.
    * 
    * @exception Exception if there was an error connecting to the remote server.
    */
   public void connect(final String host, final int port, final String server) throws Exception
   {
      if (administrator == null) administrator = new Administrator(id, this);

      System.out.print("Connecting to " + server + "@" + ((host == null || host.equals("")) ? "localhost" : host) + ".");

      if (!host.equals("") && port >= 0) administrator.connect(host, port, server);
      else if (port >= 0) administrator.connect(port, server);
      else if (!host.equals("")) administrator.connect(host, server);
      else administrator.connect(server);

      System.out.print(".");

      RemoteJServer rs = administrator.getRemoteJServer();
      RemoteSubSystemData rsd = administrator.getRemoteJServer().getSystemTreeData();

      System.out.print(".");

      this.proxy = new JServerProxy(this, rs, rsd);
      System.out.print(".");

      // adminMainPanel.showConnected();
      this.adminMainPanel.connectingToServer();
      System.out.print(".");

      this.connected = true;

      proxy.start();
      System.out.print(".");

      this.adminMainPanel.connectedToServer();

      System.out.println("connected!");

      final Thread fireAndForget = new Thread("Connected to server thread")
      {

         public void run()
         {
            try
            {
               adminMainPanel.administrationPanelsLoaded(getCustomAdministrationPanels());
            }
            catch (Throwable e)
            {
               e.printStackTrace();
               JOptionPane.showMessageDialog(mainFrame, "Error while getting custom adminstration panels - " + e, "Error", JOptionPane.ERROR_MESSAGE);
            }
         }
      };

      fireAndForget.setDaemon(true);
      fireAndForget.start();
   }

   /**
    * Disconnects this Administrator from the server.
    */
   public void disconnect()
   {
      connected = false;

      if (proxy != null) proxy.destroy();

      proxy = null;

      if (administrator != null) administrator.disconnect();

      administrator = null;

      System.runFinalization();
      System.gc();
   }

   /**
    * Called by JServerProxy when the connection to the server is fully initialized.
    */
   public void connectedToServer()
   {
      adminMainPanel.connectedToServer();
   }

   /**
    * Called by JServerProxy when the connection to the server is lost.
    */
   public synchronized void disconnectedFromServer()
   {
      if (connected)
      {
         connected = false;
         final Frame frame = mainFrame;
         Thread fireAndForget = new Thread("Disconnection thread (JAdminMainFrame)")
         {

            public void run()
            {
               disconnect();
               adminMainPanel.disconnectedFromServer();
               JOptionPane.showMessageDialog(frame, "Connection to server lost", "Info", JOptionPane.ERROR_MESSAGE);
            }
         };
         fireAndForget.start();
      }
   }

   /**
    */
   public void topSystemStateChanged()
   {
      /*
       * if(connected) adminMainPanel.getAdministrationPanel().topSystemStateChanged();
       */
   }

   /**
    */
   public void nodeAdded(final RemoteObjectProxy node, final RemoteObjectProxy parent)
   {
      if (connected)
      {
         if (node instanceof SubSystemProxy)
         {
            adminMainPanel.getAdministrationPanel().addSubSystemNode(node, (SubSystemProxy) parent);
         }
         else if (node instanceof SubComponentProxy)
         {
            adminMainPanel.getAdministrationPanel().addSubComponentNode(node, (SubComponentProxy) parent);
         }
         else
         {
            adminMainPanel.getAdministrationPanel().addNode(node, parent);
         }
      }
   }

   /**
    */
   public void nodeRemoved(final RemoteObjectProxy node)
   {
      if (connected) adminMainPanel.getAdministrationPanel().removeNode(node.getFullName());
   }

   /**
    */
   public void nodeUpdated(final RemoteObjectProxy node)
   {
      if (connected) adminMainPanel.getAdministrationPanel().nodeChanged(node.getFullName());
   }

   /**
    */
   public final List getCustomAdministrationPanels() throws Exception
   {
      if (administrator != null) return administrator.getCustomAdministrationPanels();
      return new ArrayList();
   }

   /**
    */
   public List listRemoteServers(String host, int port) throws Exception
   {
      if (administrator == null) administrator = new Administrator(id, this);

      return administrator.list(host, port);
   }

   /**
    */
   public List listRemoteServers(String host) throws Exception
   {
      if (administrator == null) administrator = new Administrator(id, this);

      return administrator.list(host);
   }

   /**
    */
   public List listRemoteServers(int port) throws Exception
   {
      if (administrator == null) administrator = new Administrator(id, this);

      return administrator.list(port);
   }

   /**
    */
   public List listRemoteServers() throws Exception
   {
      if (administrator == null) administrator = new Administrator(id, this);

      return administrator.list();
   }
}
