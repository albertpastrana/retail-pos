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

package com.openbravo.pos.config;

import java.awt.Component;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import com.openbravo.basic.BasicException;
import com.openbravo.data.user.DirtyManager;
import com.openbravo.pos.forms.AppConfig;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DatabaseBackup;
import com.openbravo.pos.util.AltEncrypter;
import com.openbravo.pos.util.DirectoryEvent;

/**
 *
 * @author adrianromero
 */
public class JPanelConfigDatabase extends javax.swing.JPanel implements PanelConfig {

	private DirtyManager dirty = new DirtyManager();

	/** Creates new form JPanelConfigDatabase */
	public JPanelConfigDatabase() {

		initComponents();

		jtxtDbDriverLib.getDocument().addDocumentListener(dirty);
		jtxtDbDriver.getDocument().addDocumentListener(dirty);
		jtxtDbURL.getDocument().addDocumentListener(dirty);
		jtxtDbPassword.getDocument().addDocumentListener(dirty);
		jtxtDbUser.getDocument().addDocumentListener(dirty);

		jbtnDbDriverLib.addActionListener(new DirectoryEvent(jtxtDbDriverLib));

		jtxtBackupDir.getDocument().addDocumentListener(dirty);
		jchkBackupDaily.addActionListener(dirty);

		jbtnBackupDir.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				JFileChooser fc = new JFileChooser();
				fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
				String current = jtxtBackupDir.getText();
				if (current != null && !current.trim().isEmpty()) {
					File currentDir = new File(current.trim());
					if (currentDir.exists()) {
						fc.setSelectedFile(currentDir);
					}
				}
				if (fc.showOpenDialog(JPanelConfigDatabase.this) == JFileChooser.APPROVE_OPTION) {
					jtxtBackupDir.setText(fc.getSelectedFile().getAbsolutePath());
				}
			}
		});

		jbtnBackupNow.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				performBackupNow();
			}
		});
	}

	public boolean hasChanged() {
		return dirty.isDirty();
	}

	public Component getConfigComponent() {
		return this;
	}

	public void loadProperties(AppConfig config) {

		jtxtDbDriverLib.setText(config.getProperty("db.driverlib"));
		jtxtDbDriver.setText(config.getProperty("db.driver"));
		jtxtDbURL.setText(config.getProperty("db.URL"));

		String sDBUser = config.getProperty("db.user");
		String sDBPassword = config.getProperty("db.password");
		if (sDBUser != null && sDBPassword != null && sDBPassword.startsWith("crypt:")) {
			// La clave esta encriptada.
			AltEncrypter cypher = new AltEncrypter("cypherkey" + sDBUser);
			sDBPassword = cypher.decrypt(sDBPassword.substring(6));
		}
		jtxtDbUser.setText(sDBUser);
		jtxtDbPassword.setText(sDBPassword);

		jchkBackupDaily.setSelected("true".equalsIgnoreCase(config.getProperty("backup.daily")));
		jtxtBackupDir.setText(config.getProperty("backup.dir"));

		dirty.setDirty(false);
	}

	public void saveProperties(AppConfig config) {

		config.setProperty("db.driverlib", jtxtDbDriverLib.getText());
		config.setProperty("db.driver", jtxtDbDriver.getText());
		config.setProperty("db.URL", jtxtDbURL.getText());
		config.setProperty("db.user", jtxtDbUser.getText());
		AltEncrypter cypher = new AltEncrypter("cypherkey" + jtxtDbUser.getText());
		config.setProperty("db.password", "crypt:" + cypher.encrypt(new String(jtxtDbPassword.getPassword())));

		config.setProperty("backup.daily", Boolean.toString(jchkBackupDaily.isSelected()));
		config.setProperty("backup.dir", jtxtBackupDir.getText());

		dirty.setDirty(false);
	}

	private void performBackupNow() {
		String dir = jtxtBackupDir.getText();
		if (dir == null || dir.trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("message.backupnodir"),
					AppLocal.getIntString("message.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}

		java.awt.Cursor oldCursor = getCursor();
		setCursor(java.awt.Cursor.getPredefinedCursor(java.awt.Cursor.WAIT_CURSOR));
		try {
			AppConfig currentConfig = new AppConfig(
					new File(System.getProperty("user.home"), AppLocal.APP_ID + ".properties"));
			currentConfig.load();
			saveProperties(currentConfig);

			File backupFile = DatabaseBackup.backup(currentConfig);
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("message.backupok", backupFile.getAbsolutePath()),
					AppLocal.getIntString("message.title"), JOptionPane.INFORMATION_MESSAGE);
		} catch (BasicException ex) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("message.backupfailed", ex.getMessage()),
					AppLocal.getIntString("message.title"), JOptionPane.ERROR_MESSAGE);
		} finally {
			setCursor(oldCursor);
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated
	// Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		jPanel1 = new javax.swing.JPanel();
		jLabel18 = new javax.swing.JLabel();
		jtxtDbDriverLib = new javax.swing.JTextField();
		jbtnDbDriverLib = new javax.swing.JButton();
		jLabel1 = new javax.swing.JLabel();
		jtxtDbDriver = new javax.swing.JTextField();
		jLabel2 = new javax.swing.JLabel();
		jtxtDbURL = new javax.swing.JTextField();
		jLabel3 = new javax.swing.JLabel();
		jtxtDbUser = new javax.swing.JTextField();
		jLabel4 = new javax.swing.JLabel();
		jtxtDbPassword = new javax.swing.JPasswordField();

		jPanelBackup = new javax.swing.JPanel();
		jchkBackupDaily = new javax.swing.JCheckBox();
		jLabelBackupDir = new javax.swing.JLabel();
		jtxtBackupDir = new javax.swing.JTextField();
		jbtnBackupDir = new javax.swing.JButton();
		jbtnBackupNow = new javax.swing.JButton();

		jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(AppLocal.getIntString("Label.Database"))); // NOI18N

		jLabel18.setText(AppLocal.getIntString("label.dbdriverlib")); // NOI18N

		jbtnDbDriverLib
				.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/fileopen.png"))); // NOI18N

		jLabel1.setText(AppLocal.getIntString("Label.DbDriver")); // NOI18N

		jLabel2.setText(AppLocal.getIntString("Label.DbURL")); // NOI18N

		jLabel3.setText(AppLocal.getIntString("Label.DbUser")); // NOI18N

		jLabel4.setText(AppLocal.getIntString("Label.DbPassword")); // NOI18N

		javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
		jPanel1.setLayout(jPanel1Layout);
		jPanel1Layout
				.setHorizontalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(jPanel1Layout.createSequentialGroup().addContainerGap()
								.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
										.addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 130,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addComponent(jLabel1).addComponent(jLabel2).addComponent(jLabel3)
										.addComponent(jLabel4))
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
										.addGroup(jPanel1Layout.createSequentialGroup().addGroup(jPanel1Layout
												.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
												.addComponent(jtxtDbURL, javax.swing.GroupLayout.PREFERRED_SIZE, 328,
														javax.swing.GroupLayout.PREFERRED_SIZE)
												.addComponent(jtxtDbDriverLib, javax.swing.GroupLayout.PREFERRED_SIZE,
														328, javax.swing.GroupLayout.PREFERRED_SIZE)
												.addComponent(jtxtDbDriver, javax.swing.GroupLayout.PREFERRED_SIZE, 165,
														javax.swing.GroupLayout.PREFERRED_SIZE))
												.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
												.addComponent(jbtnDbDriverLib))
										.addComponent(jtxtDbPassword, javax.swing.GroupLayout.PREFERRED_SIZE, 165,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addComponent(jtxtDbUser, javax.swing.GroupLayout.PREFERRED_SIZE, 165,
												javax.swing.GroupLayout.PREFERRED_SIZE))
								.addContainerGap(131, Short.MAX_VALUE)));
		jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel1Layout.createSequentialGroup().addContainerGap().addGroup(jPanel1Layout
						.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(jPanel1Layout.createSequentialGroup().addGroup(jPanel1Layout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE).addComponent(jLabel18)
								.addComponent(jtxtDbDriverLib, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
										.addComponent(jLabel1).addComponent(jtxtDbDriver,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE)))
						.addComponent(jbtnDbDriverLib))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jLabel2).addComponent(jtxtDbURL, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jLabel3).addComponent(jtxtDbUser, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jLabel4).addComponent(jtxtDbPassword,
										javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.PREFERRED_SIZE))
						.addContainerGap(14, Short.MAX_VALUE)));

		jPanelBackup.setBorder(javax.swing.BorderFactory.createTitledBorder(AppLocal.getIntString("label.backup"))); // NOI18N

		jchkBackupDaily.setText(AppLocal.getIntString("label.backupdaily")); // NOI18N

		jLabelBackupDir.setText(AppLocal.getIntString("label.backupdir")); // NOI18N

		jbtnBackupDir.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/fileopen.png"))); // NOI18N

		jbtnBackupNow.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/ark2.png"))); // NOI18N
		jbtnBackupNow.setText(AppLocal.getIntString("button.backupnow")); // NOI18N

		javax.swing.GroupLayout jPanelBackupLayout = new javax.swing.GroupLayout(jPanelBackup);
		jPanelBackup.setLayout(jPanelBackupLayout);
		jPanelBackupLayout.setHorizontalGroup(jPanelBackupLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanelBackupLayout.createSequentialGroup().addContainerGap()
						.addGroup(jPanelBackupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(jchkBackupDaily)
								.addGroup(jPanelBackupLayout.createSequentialGroup()
										.addComponent(jLabelBackupDir, javax.swing.GroupLayout.PREFERRED_SIZE, 130,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jtxtBackupDir, javax.swing.GroupLayout.PREFERRED_SIZE, 328,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jbtnBackupDir)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addComponent(jbtnBackupNow)))
						.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		jPanelBackupLayout.setVerticalGroup(jPanelBackupLayout
				.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanelBackupLayout.createSequentialGroup().addContainerGap().addComponent(jchkBackupDaily)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(jPanelBackupLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jLabelBackupDir)
								.addComponent(jtxtBackupDir, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(jbtnBackupDir).addComponent(jbtnBackupNow))
						.addContainerGap(14, Short.MAX_VALUE)));

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
						.addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addComponent(jPanel1, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
								.addComponent(jPanelBackup, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
						.addContainerGap()));
		layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
						.addComponent(jPanel1, javax.swing.GroupLayout.PREFERRED_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addComponent(jPanelBackup, javax.swing.GroupLayout.PREFERRED_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addContainerGap(14, Short.MAX_VALUE)));
	}// </editor-fold>//GEN-END:initComponents

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel18;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JLabel jLabel4;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JButton jbtnDbDriverLib;
	private javax.swing.JTextField jtxtDbDriver;
	private javax.swing.JTextField jtxtDbDriverLib;
	private javax.swing.JPasswordField jtxtDbPassword;
	private javax.swing.JTextField jtxtDbURL;
	private javax.swing.JTextField jtxtDbUser;

	private javax.swing.JPanel jPanelBackup;
	private javax.swing.JCheckBox jchkBackupDaily;
	private javax.swing.JLabel jLabelBackupDir;
	private javax.swing.JTextField jtxtBackupDir;
	private javax.swing.JButton jbtnBackupDir;
	private javax.swing.JButton jbtnBackupNow;
	// End of variables declaration//GEN-END:variables

}
