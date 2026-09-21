//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2007-2009 Openbravo, S.L.
//    http://www.openbravo.com/product/pos
//
//    This file is part of Openbravo POS.
//
//    Openbravo POS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Openbravo POS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with Openbravo POS.  If not, see <http://www.gnu.org/licenses/>.

package com.openbravo.pos.forms;

import com.openbravo.pos.config.JFrmConfig;
import java.awt.BorderLayout;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.rmi.RemoteException;
import javax.swing.JFrame;
import com.openbravo.pos.instance.AppMessage;
import com.openbravo.pos.instance.InstanceManager;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;

/**
 *
 * @author adrianromero
 */
public class JRootFrame extends javax.swing.JFrame implements AppMessage {

	private static Logger logger = Logger.getLogger("com.openbravo.pos.forms.JRootFrame");

	private static final String WINDOW_X = "machine.window.x";
	private static final String WINDOW_Y = "machine.window.y";
	private static final String WINDOW_WIDTH = "machine.window.width";
	private static final String WINDOW_HEIGHT = "machine.window.height";
	private static final String WINDOW_MAXIMIZED = "machine.window.maximized";

	// Share of the desktop the window takes the first time it runs.
	private static final double DEFAULT_SCREEN_SHARE = 0.9;

	// Gestor de que haya solo una instancia corriendo en cada maquina.
	private InstanceManager m_instmanager = null;

	private JRootApp m_rootapp;
	private AppConfig m_props;

	// Read from the shutdown hook thread, written on the event thread.
	private volatile Rectangle restorebounds;
	private volatile boolean maximized;

	/** Creates new form JRootFrame */
	public JRootFrame() {

		initComponents();
	}

	public void initFrame(AppConfig props) {

		m_props = props;

		m_rootapp = new JRootApp();

		if (m_rootapp.initApp(m_props)) {

			if ("true".equals(props.getProperty("machine.uniqueinstance"))) {
				// Register the running application
				try {
					m_instmanager = new InstanceManager(this);
				} catch (Exception e) {
				}
			}

			// Show the application
			add(m_rootapp, BorderLayout.CENTER);

			try {
				this.setIconImage(
						ImageIO.read(JRootFrame.class.getResourceAsStream("/com/openbravo/images/favicon.png")));
			} catch (IOException e) {
			}
			setTitle(AppLocal.getBaseTitle());
			pack();
			restoreGeometry();

			setVisible(true);
		} else {
			new JFrmConfig(props).setVisible(true); // Show the configuration window.
		}
	}

	private void restoreGeometry() {

		Rectangle bounds = savedBounds();
		setBounds(bounds == null ? defaultBounds() : bounds);
		rememberGeometry();

		if ("true".equals(m_props.getProperty(WINDOW_MAXIMIZED))) {
			setExtendedState(JFrame.MAXIMIZED_BOTH);
		}

		addComponentListener(new java.awt.event.ComponentAdapter() {
			public void componentResized(java.awt.event.ComponentEvent evt) {
				rememberGeometry();
			}
			public void componentMoved(java.awt.event.ComponentEvent evt) {
				rememberGeometry();
			}
		});

		// Cmd+Q on macOS exits without ever closing the window, so the save
		// hangs off the exit itself rather than off any close handler.
		Runtime.getRuntime().addShutdownHook(new Thread() {
			public void run() {
				saveGeometry();
			}
		});
	}

	private void rememberGeometry() {

		maximized = (getExtendedState() & JFrame.MAXIMIZED_BOTH) == JFrame.MAXIMIZED_BOTH;

		// Maximized bounds are the desktop, not a size worth restoring to.
		if (!maximized) {
			restorebounds = getBounds();
		}
	}

	private void saveGeometry() {

		m_props.setProperty(WINDOW_X, Integer.toString(restorebounds.x));
		m_props.setProperty(WINDOW_Y, Integer.toString(restorebounds.y));
		m_props.setProperty(WINDOW_WIDTH, Integer.toString(restorebounds.width));
		m_props.setProperty(WINDOW_HEIGHT, Integer.toString(restorebounds.height));
		m_props.setProperty(WINDOW_MAXIMIZED, Boolean.toString(maximized));

		try {
			ConfigurationStore.save(m_props);
		} catch (IOException e) {
			logger.log(Level.WARNING, "Cannot save the window geometry", e);
		}
	}

	private Rectangle savedBounds() {

		try {
			Rectangle bounds = new Rectangle(Integer.parseInt(m_props.getProperty(WINDOW_X)),
					Integer.parseInt(m_props.getProperty(WINDOW_Y)),
					Integer.parseInt(m_props.getProperty(WINDOW_WIDTH)),
					Integer.parseInt(m_props.getProperty(WINDOW_HEIGHT)));
			return onSomeScreen(bounds) ? bounds : null;
		} catch (NumberFormatException e) {
			// Nothing stored yet, or the file was edited by hand.
			return null;
		}
	}

	private static Rectangle defaultBounds() {

		Rectangle desktop = GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds();
		int width = (int) (desktop.width * DEFAULT_SCREEN_SHARE);
		int height = (int) (desktop.height * DEFAULT_SCREEN_SHARE);

		return new Rectangle(desktop.x + (desktop.width - width) / 2, desktop.y + (desktop.height - height) / 2, width,
				height);
	}

	// A monitor that has been unplugged since the last run would put the window
	// where nobody can reach it.
	private static boolean onSomeScreen(Rectangle bounds) {

		for (GraphicsDevice device : GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) {
			if (device.getDefaultConfiguration().getBounds().intersects(bounds)) {
				return true;
			}
		}
		return false;
	}

	public void restoreWindow() throws RemoteException {
		java.awt.EventQueue.invokeLater(new Runnable() {
			public void run() {
				if (getExtendedState() == JFrame.ICONIFIED) {
					setExtendedState(JFrame.NORMAL);
				}
				requestFocus();
			}
		});
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated
	// Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		setDefaultCloseOperation(javax.swing.WindowConstants.DO_NOTHING_ON_CLOSE);
		addWindowListener(new java.awt.event.WindowAdapter() {
			public void windowClosed(java.awt.event.WindowEvent evt) {
				formWindowClosed(evt);
			}
			public void windowClosing(java.awt.event.WindowEvent evt) {
				formWindowClosing(evt);
			}
		});
	}// </editor-fold>//GEN-END:initComponents

	private void formWindowClosing(java.awt.event.WindowEvent evt) {// GEN-FIRST:event_formWindowClosing

		m_rootapp.tryToClose();

	}// GEN-LAST:event_formWindowClosing

	private void formWindowClosed(java.awt.event.WindowEvent evt) {// GEN-FIRST:event_formWindowClosed

		System.exit(0);

	}// GEN-LAST:event_formWindowClosed

	// Variables declaration - do not modify//GEN-BEGIN:variables
	// End of variables declaration//GEN-END:variables

}
