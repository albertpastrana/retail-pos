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

import com.openbravo.data.user.DirtyManager;
import java.awt.CardLayout;
import java.awt.Component;
import com.openbravo.pos.forms.AppConfig;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.util.ReportUtils;
import com.openbravo.pos.util.StringParser;

/**
 *
 * @author adrianromero
 */
public class JPanelConfigGeneral extends javax.swing.JPanel implements PanelConfig {

	private DirtyManager dirty = new DirtyManager();

	private ParametersConfig printer1printerparams;

	private ParametersConfig printer2printerparams;

	private ParametersConfig printer3printerparams;

	/** Creates new form JPanelConfigGeneral */
	public JPanelConfigGeneral() {

		initComponents();

		String[] printernames = ReportUtils.getPrintNames();

		jtxtMachineHostname.getDocument().addDocumentListener(dirty);
		jcboMachineScreenmode.addActionListener(dirty);
		jcboTicketsBag.addActionListener(dirty);

		jcboMachineDisplay.addActionListener(dirty);
		jcboConnDisplay.addActionListener(dirty);
		jcboSerialDisplay.addActionListener(dirty);

		jcboMachinePrinter.addActionListener(dirty);
		jcboConnPrinter.addActionListener(dirty);
		jcboSerialPrinter.addActionListener(dirty);

		printer1printerparams = new ParametersPrinter(printernames);
		printer1printerparams.addDirtyManager(dirty);
		m_jPrinterParams1.add(printer1printerparams.getComponent(), "printer");

		jcboMachinePrinter2.addActionListener(dirty);
		jcboConnPrinter2.addActionListener(dirty);
		jcboSerialPrinter2.addActionListener(dirty);

		printer2printerparams = new ParametersPrinter(printernames);
		printer2printerparams.addDirtyManager(dirty);
		m_jPrinterParams2.add(printer2printerparams.getComponent(), "printer");

		jcboMachinePrinter3.addActionListener(dirty);
		jcboConnPrinter3.addActionListener(dirty);
		jcboSerialPrinter3.addActionListener(dirty);

		printer3printerparams = new ParametersPrinter(printernames);
		printer3printerparams.addDirtyManager(dirty);
		m_jPrinterParams3.add(printer3printerparams.getComponent(), "printer");

		jcboMachineScanner.addActionListener(dirty);
		jcboSerialScanner.addActionListener(dirty);

		cboPrinters.addActionListener(dirty);

		jcboMachineScreenmode.addItem("window");
		jcboMachineScreenmode.addItem("fullscreen");

		jcboTicketsBag.addItem("simple");
		jcboTicketsBag.addItem("standard");

		// Printer 1
		jcboMachinePrinter.addItem("screen");
		jcboMachinePrinter.addItem("printer");
		jcboMachinePrinter.addItem("epson");
		jcboMachinePrinter.addItem("tmu220");
		jcboMachinePrinter.addItem("star");
		jcboMachinePrinter.addItem("ithaca");
		jcboMachinePrinter.addItem("surepos");
		jcboMachinePrinter.addItem("plain");
		jcboMachinePrinter.addItem("Not defined");

		jcboConnPrinter.addItem("file");

		// Printer 2
		jcboMachinePrinter2.addItem("screen");
		jcboMachinePrinter2.addItem("printer");
		jcboMachinePrinter2.addItem("epson");
		jcboMachinePrinter2.addItem("tmu220");
		jcboMachinePrinter2.addItem("star");
		jcboMachinePrinter2.addItem("ithaca");
		jcboMachinePrinter2.addItem("surepos");
		jcboMachinePrinter2.addItem("plain");
		jcboMachinePrinter2.addItem("Not defined");

		jcboConnPrinter2.addItem("file");

		// Printer 3
		jcboMachinePrinter3.addItem("screen");
		jcboMachinePrinter3.addItem("printer");
		jcboMachinePrinter3.addItem("epson");
		jcboMachinePrinter3.addItem("tmu220");
		jcboMachinePrinter3.addItem("star");
		jcboMachinePrinter3.addItem("ithaca");
		jcboMachinePrinter3.addItem("surepos");
		jcboMachinePrinter3.addItem("plain");
		jcboMachinePrinter3.addItem("Not defined");

		jcboConnPrinter3.addItem("file");

		// Display
		jcboMachineDisplay.addItem("screen");
		jcboMachineDisplay.addItem("window");
		jcboMachineDisplay.addItem("epson");
		jcboMachineDisplay.addItem("ld200");
		jcboMachineDisplay.addItem("surepos");
		jcboMachineDisplay.addItem("Not defined");

		jcboConnDisplay.addItem("file");

		// Scanner
		jcboMachineScanner.addItem("Not defined");

		// Printers
		cboPrinters.addItem("(Default)");
		cboPrinters.addItem("(Show dialog)");
		for (String name : printernames) {
			cboPrinters.addItem(name);
		}
	}

	public boolean hasChanged() {
		return dirty.isDirty();
	}

	public Component getConfigComponent() {
		return this;
	}

	public void loadProperties(AppConfig config) {

		jtxtMachineHostname.setText(config.getProperty("machine.hostname"));

		jcboMachineScreenmode.setSelectedItem(config.getProperty("machine.screenmode"));
		jcboTicketsBag.setSelectedItem(config.getProperty("machine.ticketsbag"));

		StringParser p = new StringParser(config.getProperty("machine.printer"));
		String sparam = p.nextToken(':');
		if ("file".equals(sparam)) {
			jcboMachinePrinter.setSelectedItem("epson");
			jcboConnPrinter.setSelectedItem(sparam);
			jcboSerialPrinter.setSelectedItem(p.nextToken(','));
		} else if ("printer".equals(sparam)) {
			jcboMachinePrinter.setSelectedItem(sparam);
			printer1printerparams.setParameters(p);
		} else {
			jcboMachinePrinter.setSelectedItem(sparam);
			jcboConnPrinter.setSelectedItem(p.nextToken(','));
			jcboSerialPrinter.setSelectedItem(p.nextToken(','));
		}

		p = new StringParser(config.getProperty("machine.printer.2"));
		sparam = p.nextToken(':');
		if ("file".equals(sparam)) {
			jcboMachinePrinter2.setSelectedItem("epson");
			jcboConnPrinter2.setSelectedItem(sparam);
			jcboSerialPrinter2.setSelectedItem(p.nextToken(','));
		} else if ("printer".equals(sparam)) {
			jcboMachinePrinter2.setSelectedItem(sparam);
			printer2printerparams.setParameters(p);
		} else {
			jcboMachinePrinter2.setSelectedItem(sparam);
			jcboConnPrinter2.setSelectedItem(p.nextToken(','));
			jcboSerialPrinter2.setSelectedItem(p.nextToken(','));
		}

		p = new StringParser(config.getProperty("machine.printer.3"));
		sparam = p.nextToken(':');
		if ("file".equals(sparam)) {
			jcboMachinePrinter3.setSelectedItem("epson");
			jcboConnPrinter3.setSelectedItem(sparam);
			jcboSerialPrinter3.setSelectedItem(p.nextToken(','));
		} else if ("printer".equals(sparam)) {
			jcboMachinePrinter3.setSelectedItem(sparam);
			printer3printerparams.setParameters(p);
		} else {
			jcboMachinePrinter3.setSelectedItem(sparam);
			jcboConnPrinter3.setSelectedItem(p.nextToken(','));
			jcboSerialPrinter3.setSelectedItem(p.nextToken(','));
		}

		p = new StringParser(config.getProperty("machine.display"));
		sparam = p.nextToken(':');
		if ("file".equals(sparam)) {
			jcboMachineDisplay.setSelectedItem("epson");
			jcboConnDisplay.setSelectedItem(sparam);
			jcboSerialDisplay.setSelectedItem(p.nextToken(','));
		} else {
			jcboMachineDisplay.setSelectedItem(sparam);
			jcboConnDisplay.setSelectedItem(p.nextToken(','));
			jcboSerialDisplay.setSelectedItem(p.nextToken(','));
		}

		p = new StringParser(config.getProperty("machine.scanner"));
		sparam = p.nextToken(':');
		jcboMachineScanner.setSelectedItem(sparam);

		cboPrinters.setSelectedItem(config.getProperty("machine.printername"));

		dirty.setDirty(false);
	}

	public void saveProperties(AppConfig config) {

		config.setProperty("machine.hostname", jtxtMachineHostname.getText());

		config.setProperty("machine.screenmode", comboValue(jcboMachineScreenmode.getSelectedItem()));
		config.setProperty("machine.ticketsbag", comboValue(jcboTicketsBag.getSelectedItem()));

		String sMachinePrinter = comboValue(jcboMachinePrinter.getSelectedItem());
		if ("epson".equals(sMachinePrinter) || "tmu220".equals(sMachinePrinter) || "star".equals(sMachinePrinter)
				|| "ithaca".equals(sMachinePrinter) || "surepos".equals(sMachinePrinter)) {
			config.setProperty("machine.printer", sMachinePrinter + ":" + comboValue(jcboConnPrinter.getSelectedItem())
					+ "," + comboValue(jcboSerialPrinter.getSelectedItem()));
		} else if ("printer".equals(sMachinePrinter)) {
			config.setProperty("machine.printer", sMachinePrinter + ":" + printer1printerparams.getParameters());
		} else {
			config.setProperty("machine.printer", sMachinePrinter);
		}

		String sMachinePrinter2 = comboValue(jcboMachinePrinter2.getSelectedItem());
		if ("epson".equals(sMachinePrinter2) || "tmu220".equals(sMachinePrinter2) || "star".equals(sMachinePrinter2)
				|| "ithaca".equals(sMachinePrinter2) || "surepos".equals(sMachinePrinter2)) {
			config.setProperty("machine.printer.2",
					sMachinePrinter2 + ":" + comboValue(jcboConnPrinter2.getSelectedItem()) + ","
							+ comboValue(jcboSerialPrinter2.getSelectedItem()));
		} else if ("printer".equals(sMachinePrinter2)) {
			config.setProperty("machine.printer.2", sMachinePrinter2 + ":" + printer2printerparams.getParameters());
		} else {
			config.setProperty("machine.printer.2", sMachinePrinter2);
		}

		String sMachinePrinter3 = comboValue(jcboMachinePrinter3.getSelectedItem());
		if ("epson".equals(sMachinePrinter3) || "tmu220".equals(sMachinePrinter3) || "star".equals(sMachinePrinter3)
				|| "ithaca".equals(sMachinePrinter3) || "surepos".equals(sMachinePrinter3)) {
			config.setProperty("machine.printer.3",
					sMachinePrinter3 + ":" + comboValue(jcboConnPrinter3.getSelectedItem()) + ","
							+ comboValue(jcboSerialPrinter3.getSelectedItem()));
		} else if ("printer".equals(sMachinePrinter3)) {
			config.setProperty("machine.printer.3", sMachinePrinter3 + ":" + printer3printerparams.getParameters());
		} else {
			config.setProperty("machine.printer.3", sMachinePrinter3);
		}

		String sMachineDisplay = comboValue(jcboMachineDisplay.getSelectedItem());
		if ("epson".equals(sMachineDisplay) || "ld200".equals(sMachineDisplay) || "surepos".equals(sMachineDisplay)) {
			config.setProperty("machine.display", sMachineDisplay + ":" + comboValue(jcboConnDisplay.getSelectedItem())
					+ "," + comboValue(jcboSerialDisplay.getSelectedItem()));
		} else {
			config.setProperty("machine.display", sMachineDisplay);
		}

		// El scanner
		config.setProperty("machine.scanner", comboValue(jcboMachineScanner.getSelectedItem()));

		config.setProperty("machine.printername", comboValue(cboPrinters.getSelectedItem()));

		dirty.setDirty(false);
	}

	private String comboValue(Object value) {
		return value == null ? "" : value.toString();
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated
	// Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		jPanel13 = new javax.swing.JPanel();
		jLabel5 = new javax.swing.JLabel();
		jtxtMachineHostname = new javax.swing.JTextField();
		jLabel6 = new javax.swing.JLabel();
		jcboMachineScreenmode = new javax.swing.JComboBox();
		jLabel16 = new javax.swing.JLabel();
		jcboTicketsBag = new javax.swing.JComboBox();
		jLabel15 = new javax.swing.JLabel();
		jcboMachineDisplay = new javax.swing.JComboBox();
		m_jDisplayParams = new javax.swing.JPanel();
		jPanel2 = new javax.swing.JPanel();
		jPanel1 = new javax.swing.JPanel();
		jlblConnDisplay = new javax.swing.JLabel();
		jcboConnDisplay = new javax.swing.JComboBox();
		jlblDisplayPort = new javax.swing.JLabel();
		jcboSerialDisplay = new javax.swing.JComboBox();
		jLabel7 = new javax.swing.JLabel();
		jcboMachinePrinter = new javax.swing.JComboBox();
		m_jPrinterParams1 = new javax.swing.JPanel();
		jPanel5 = new javax.swing.JPanel();
		jPanel6 = new javax.swing.JPanel();
		jlblConnPrinter = new javax.swing.JLabel();
		jcboConnPrinter = new javax.swing.JComboBox();
		jlblPrinterPort = new javax.swing.JLabel();
		jcboSerialPrinter = new javax.swing.JComboBox();
		jLabel18 = new javax.swing.JLabel();
		jcboMachinePrinter2 = new javax.swing.JComboBox();
		jLabel19 = new javax.swing.JLabel();
		m_jPrinterParams2 = new javax.swing.JPanel();
		jPanel7 = new javax.swing.JPanel();
		jPanel8 = new javax.swing.JPanel();
		jlblConnPrinter2 = new javax.swing.JLabel();
		jcboConnPrinter2 = new javax.swing.JComboBox();
		jlblPrinterPort2 = new javax.swing.JLabel();
		jcboSerialPrinter2 = new javax.swing.JComboBox();
		jcboMachinePrinter3 = new javax.swing.JComboBox();
		jLabel26 = new javax.swing.JLabel();
		jcboMachineScanner = new javax.swing.JComboBox();
		jLabel1 = new javax.swing.JLabel();
		cboPrinters = new javax.swing.JComboBox();
		m_jPrinterParams3 = new javax.swing.JPanel();
		jPanel9 = new javax.swing.JPanel();
		jPanel10 = new javax.swing.JPanel();
		jlblConnPrinter3 = new javax.swing.JLabel();
		jcboConnPrinter3 = new javax.swing.JComboBox();
		jlblPrinterPort3 = new javax.swing.JLabel();
		jcboSerialPrinter3 = new javax.swing.JComboBox();
		m_jScannerParams = new javax.swing.JPanel();
		jPanel24 = new javax.swing.JPanel();
		jPanel19 = new javax.swing.JPanel();
		jlblPrinterPort5 = new javax.swing.JLabel();
		jcboSerialScanner = new javax.swing.JComboBox();

		jPanel13.setBorder(javax.swing.BorderFactory.createTitledBorder(AppLocal.getIntString("Label.CashMachine"))); // NOI18N

		jLabel5.setText(AppLocal.getIntString("Label.MachineName")); // NOI18N

		jLabel6.setText(AppLocal.getIntString("Label.MachineScreen")); // NOI18N

		jLabel16.setText(AppLocal.getIntString("Label.Ticketsbag")); // NOI18N

		jLabel15.setText(AppLocal.getIntString("Label.MachineDisplay")); // NOI18N

		jcboMachineDisplay.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jcboMachineDisplayActionPerformed(evt);
			}
		});

		m_jDisplayParams.setLayout(new java.awt.CardLayout());
		m_jDisplayParams.add(jPanel2, "empty");

		jlblConnDisplay.setText(AppLocal.getIntString("label.machinedisplayconn")); // NOI18N

		jlblDisplayPort.setText(AppLocal.getIntString("label.machinedisplayport")); // NOI18N

		jcboSerialDisplay.setEditable(true);

		javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
		jPanel1.setLayout(jPanel1Layout);
		jPanel1Layout
				.setHorizontalGroup(
						jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(jPanel1Layout.createSequentialGroup().addContainerGap()
										.addComponent(jlblConnDisplay, javax.swing.GroupLayout.PREFERRED_SIZE, 100,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jcboConnDisplay, javax.swing.GroupLayout.PREFERRED_SIZE, 90,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jlblDisplayPort, javax.swing.GroupLayout.PREFERRED_SIZE, 100,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jcboSerialDisplay, javax.swing.GroupLayout.PREFERRED_SIZE, 90,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addContainerGap(56, Short.MAX_VALUE)));
		jPanel1Layout.setVerticalGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel1Layout.createSequentialGroup()
						.addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jcboConnDisplay, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(jlblDisplayPort)
								.addComponent(jcboSerialDisplay, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(jlblConnDisplay))
						.addGap(59, 59, 59)));

		m_jDisplayParams.add(jPanel1, "comm");

		jLabel7.setText(AppLocal.getIntString("Label.MachinePrinter")); // NOI18N

		jcboMachinePrinter.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jcboMachinePrinterActionPerformed(evt);
			}
		});

		m_jPrinterParams1.setLayout(new java.awt.CardLayout());
		m_jPrinterParams1.add(jPanel5, "empty");

		jlblConnPrinter.setText(AppLocal.getIntString("label.machinedisplayconn")); // NOI18N

		jlblPrinterPort.setText(AppLocal.getIntString("label.machineprinterport")); // NOI18N

		jcboSerialPrinter.setEditable(true);

		javax.swing.GroupLayout jPanel6Layout = new javax.swing.GroupLayout(jPanel6);
		jPanel6.setLayout(jPanel6Layout);
		jPanel6Layout
				.setHorizontalGroup(
						jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(jPanel6Layout.createSequentialGroup().addContainerGap()
										.addComponent(jlblConnPrinter, javax.swing.GroupLayout.PREFERRED_SIZE, 100,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jcboConnPrinter, javax.swing.GroupLayout.PREFERRED_SIZE, 90,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jlblPrinterPort, javax.swing.GroupLayout.PREFERRED_SIZE, 100,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jcboSerialPrinter, javax.swing.GroupLayout.PREFERRED_SIZE, 90,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addContainerGap(56, Short.MAX_VALUE)));
		jPanel6Layout.setVerticalGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel6Layout.createSequentialGroup()
						.addGroup(jPanel6Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jcboConnPrinter, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(jlblPrinterPort)
								.addComponent(jcboSerialPrinter, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(jlblConnPrinter))
						.addGap(195, 195, 195)));

		m_jPrinterParams1.add(jPanel6, "comm");

		jLabel18.setText(AppLocal.getIntString("Label.MachinePrinter2")); // NOI18N

		jcboMachinePrinter2.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jcboMachinePrinter2ActionPerformed(evt);
			}
		});

		jLabel19.setText(AppLocal.getIntString("Label.MachinePrinter3")); // NOI18N

		m_jPrinterParams2.setLayout(new java.awt.CardLayout());
		m_jPrinterParams2.add(jPanel7, "empty");

		jlblConnPrinter2.setText(AppLocal.getIntString("label.machinedisplayconn")); // NOI18N

		jlblPrinterPort2.setText(AppLocal.getIntString("label.machineprinterport")); // NOI18N

		jcboSerialPrinter2.setEditable(true);

		javax.swing.GroupLayout jPanel8Layout = new javax.swing.GroupLayout(jPanel8);
		jPanel8.setLayout(jPanel8Layout);
		jPanel8Layout
				.setHorizontalGroup(
						jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(jPanel8Layout.createSequentialGroup().addContainerGap()
										.addComponent(jlblConnPrinter2, javax.swing.GroupLayout.PREFERRED_SIZE, 100,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jcboConnPrinter2, javax.swing.GroupLayout.PREFERRED_SIZE, 90,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jlblPrinterPort2, javax.swing.GroupLayout.PREFERRED_SIZE, 100,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jcboSerialPrinter2, javax.swing.GroupLayout.PREFERRED_SIZE, 90,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addContainerGap(56, Short.MAX_VALUE)));
		jPanel8Layout.setVerticalGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel8Layout.createSequentialGroup()
						.addGroup(jPanel8Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jcboConnPrinter2, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(jlblPrinterPort2)
								.addComponent(jcboSerialPrinter2, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(jlblConnPrinter2))
						.addGap(205, 205, 205)));

		m_jPrinterParams2.add(jPanel8, "comm");

		jcboMachinePrinter3.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jcboMachinePrinter3ActionPerformed(evt);
			}
		});

		jLabel26.setText(AppLocal.getIntString("label.scanner")); // NOI18N

		jcboMachineScanner.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jcboMachineScannerActionPerformed(evt);
			}
		});

		jLabel1.setText(AppLocal.getIntString("label.reportsprinter")); // NOI18N

		m_jPrinterParams3.setLayout(new java.awt.CardLayout());
		m_jPrinterParams3.add(jPanel9, "empty");

		jlblConnPrinter3.setText(AppLocal.getIntString("label.machinedisplayconn")); // NOI18N

		jlblPrinterPort3.setText(AppLocal.getIntString("label.machineprinterport")); // NOI18N

		jcboSerialPrinter3.setEditable(true);

		javax.swing.GroupLayout jPanel10Layout = new javax.swing.GroupLayout(jPanel10);
		jPanel10.setLayout(jPanel10Layout);
		jPanel10Layout
				.setHorizontalGroup(
						jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(jPanel10Layout.createSequentialGroup().addContainerGap()
										.addComponent(jlblConnPrinter3, javax.swing.GroupLayout.PREFERRED_SIZE, 100,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jcboConnPrinter3, javax.swing.GroupLayout.PREFERRED_SIZE, 90,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jlblPrinterPort3, javax.swing.GroupLayout.PREFERRED_SIZE, 100,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jcboSerialPrinter3, javax.swing.GroupLayout.PREFERRED_SIZE, 90,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addContainerGap(56, Short.MAX_VALUE)));
		jPanel10Layout.setVerticalGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel10Layout.createSequentialGroup()
						.addGroup(jPanel10Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jcboConnPrinter3, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(jlblPrinterPort3)
								.addComponent(jcboSerialPrinter3, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(jlblConnPrinter3))
						.addGap(125, 125, 125)));

		m_jPrinterParams3.add(jPanel10, "comm");

		m_jScannerParams.setLayout(new java.awt.CardLayout());
		m_jScannerParams.add(jPanel24, "empty");

		jlblPrinterPort5.setText(AppLocal.getIntString("label.machineprinterport")); // NOI18N

		jcboSerialScanner.setEditable(true);

		javax.swing.GroupLayout jPanel19Layout = new javax.swing.GroupLayout(jPanel19);
		jPanel19.setLayout(jPanel19Layout);
		jPanel19Layout
				.setHorizontalGroup(
						jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(jPanel19Layout.createSequentialGroup().addContainerGap()
										.addComponent(jlblPrinterPort5, javax.swing.GroupLayout.PREFERRED_SIZE, 100,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jcboSerialScanner, javax.swing.GroupLayout.PREFERRED_SIZE, 90,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addContainerGap(270, Short.MAX_VALUE)));
		jPanel19Layout.setVerticalGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel19Layout.createSequentialGroup()
						.addGroup(jPanel19Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jcboSerialScanner, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
								.addComponent(jlblPrinterPort5))
						.addGap(235, 235, 235)));

		m_jScannerParams.add(jPanel19, "comm");

		javax.swing.GroupLayout jPanel13Layout = new javax.swing.GroupLayout(jPanel13);
		jPanel13.setLayout(jPanel13Layout);
		jPanel13Layout.setHorizontalGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel13Layout.createSequentialGroup().addContainerGap().addGroup(jPanel13Layout
						.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
						.addGroup(jPanel13Layout.createSequentialGroup()
								.addComponent(jLabel15, javax.swing.GroupLayout.PREFERRED_SIZE, 130,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(jcboMachineDisplay, javax.swing.GroupLayout.PREFERRED_SIZE, 165,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(
										m_jDisplayParams, javax.swing.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE))
						.addGroup(jPanel13Layout.createSequentialGroup()
								.addComponent(jLabel7, javax.swing.GroupLayout.PREFERRED_SIZE, 130,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(jcboMachinePrinter, javax.swing.GroupLayout.PREFERRED_SIZE, 165,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(
										m_jPrinterParams1, javax.swing.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE))
						.addGroup(jPanel13Layout.createSequentialGroup()
								.addComponent(jLabel18, javax.swing.GroupLayout.PREFERRED_SIZE, 130,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(jcboMachinePrinter2, javax.swing.GroupLayout.PREFERRED_SIZE, 165,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(
										m_jPrinterParams2, javax.swing.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE))
						.addGroup(jPanel13Layout.createSequentialGroup()
								.addComponent(jLabel19, javax.swing.GroupLayout.PREFERRED_SIZE, 130,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(jcboMachinePrinter3, javax.swing.GroupLayout.PREFERRED_SIZE, 165,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(
										m_jPrinterParams3, javax.swing.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE))
						.addGroup(jPanel13Layout.createSequentialGroup().addGroup(jPanel13Layout
								.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(jPanel13Layout.createSequentialGroup()
										.addComponent(jLabel26, javax.swing.GroupLayout.PREFERRED_SIZE, 130,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(jcboMachineScanner, javax.swing.GroupLayout.PREFERRED_SIZE, 165,
												javax.swing.GroupLayout.PREFERRED_SIZE))
								.addGroup(jPanel13Layout.createSequentialGroup()
										.addComponent(jLabel1, javax.swing.GroupLayout.PREFERRED_SIZE, 130,
												javax.swing.GroupLayout.PREFERRED_SIZE)
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
										.addComponent(cboPrinters, javax.swing.GroupLayout.PREFERRED_SIZE, 165,
												javax.swing.GroupLayout.PREFERRED_SIZE)))
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED).addComponent(
										m_jScannerParams, javax.swing.GroupLayout.DEFAULT_SIZE, 484, Short.MAX_VALUE))
						.addGroup(jPanel13Layout.createSequentialGroup()
								.addComponent(jLabel5, javax.swing.GroupLayout.PREFERRED_SIZE, 130,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(jtxtMachineHostname, javax.swing.GroupLayout.PREFERRED_SIZE, 165,
										javax.swing.GroupLayout.PREFERRED_SIZE))
						.addGroup(jPanel13Layout.createSequentialGroup()
								.addComponent(jLabel6, javax.swing.GroupLayout.PREFERRED_SIZE, 130,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(jcboMachineScreenmode, javax.swing.GroupLayout.PREFERRED_SIZE, 165,
										javax.swing.GroupLayout.PREFERRED_SIZE))
						.addGroup(jPanel13Layout.createSequentialGroup()
								.addComponent(jLabel16, javax.swing.GroupLayout.PREFERRED_SIZE, 130,
										javax.swing.GroupLayout.PREFERRED_SIZE)
								.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
								.addComponent(jcboTicketsBag, javax.swing.GroupLayout.PREFERRED_SIZE, 165,
										javax.swing.GroupLayout.PREFERRED_SIZE)))
						.addContainerGap()));
		jPanel13Layout.setVerticalGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(jPanel13Layout.createSequentialGroup().addContainerGap()
						.addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jLabel5)
								.addComponent(jtxtMachineHostname, javax.swing.GroupLayout.PREFERRED_SIZE,
										javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
						.addGap(6, 6, 6)
						.addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jLabel6).addComponent(jcboMachineScreenmode,
										javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
								.addComponent(jLabel16).addComponent(jcboTicketsBag,
										javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE,
										javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
										.addComponent(jLabel15).addComponent(jcboMachineDisplay,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE))
								.addComponent(m_jDisplayParams, javax.swing.GroupLayout.PREFERRED_SIZE, 24,
										javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
										.addComponent(jLabel7).addComponent(jcboMachinePrinter,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE))
								.addComponent(m_jPrinterParams1, javax.swing.GroupLayout.PREFERRED_SIZE, 24,
										javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
										.addComponent(jLabel18).addComponent(jcboMachinePrinter2,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE))
								.addComponent(m_jPrinterParams2, javax.swing.GroupLayout.PREFERRED_SIZE, 24,
										javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
										.addComponent(jLabel19).addComponent(jcboMachinePrinter3,
												javax.swing.GroupLayout.PREFERRED_SIZE,
												javax.swing.GroupLayout.DEFAULT_SIZE,
												javax.swing.GroupLayout.PREFERRED_SIZE))
								.addComponent(m_jPrinterParams3, javax.swing.GroupLayout.PREFERRED_SIZE, 24,
										javax.swing.GroupLayout.PREFERRED_SIZE))
						.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
						.addGroup(jPanel13Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
								.addGroup(jPanel13Layout.createSequentialGroup()
										.addGroup(jPanel13Layout
												.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(jLabel26).addComponent(jcboMachineScanner,
														javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE))
										.addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
										.addGroup(jPanel13Layout
												.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
												.addComponent(jLabel1).addComponent(cboPrinters,
														javax.swing.GroupLayout.PREFERRED_SIZE,
														javax.swing.GroupLayout.DEFAULT_SIZE,
														javax.swing.GroupLayout.PREFERRED_SIZE)))
								.addComponent(m_jScannerParams, javax.swing.GroupLayout.PREFERRED_SIZE, 33,
										javax.swing.GroupLayout.PREFERRED_SIZE))
						.addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));

		javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
		this.setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap().addComponent(jPanel13,
						javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
						.addContainerGap()));
		layout.setVerticalGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
				.addGroup(layout.createSequentialGroup().addContainerGap()
						.addComponent(jPanel13, javax.swing.GroupLayout.PREFERRED_SIZE,
								javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
						.addContainerGap(17, Short.MAX_VALUE)));
	}// </editor-fold>//GEN-END:initComponents

	private void jcboMachineScannerActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jcboMachineScannerActionPerformed
		CardLayout cl = (CardLayout) (m_jScannerParams.getLayout());

		if ("scanpal2".equals(jcboMachineScanner.getSelectedItem())) {
			cl.show(m_jScannerParams, "comm");
		} else {
			cl.show(m_jScannerParams, "empty");
		}
	}// GEN-LAST:event_jcboMachineScannerActionPerformed

	private void jcboMachinePrinter3ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jcboMachinePrinter3ActionPerformed
		CardLayout cl = (CardLayout) (m_jPrinterParams3.getLayout());

		if ("epson".equals(jcboMachinePrinter3.getSelectedItem())
				|| "tmu220".equals(jcboMachinePrinter3.getSelectedItem())
				|| "star".equals(jcboMachinePrinter3.getSelectedItem())
				|| "ithaca".equals(jcboMachinePrinter3.getSelectedItem())
				|| "surepos".equals(jcboMachinePrinter3.getSelectedItem())) {
			cl.show(m_jPrinterParams3, "comm");
		} else if ("printer".equals(jcboMachinePrinter3.getSelectedItem())) {
			cl.show(m_jPrinterParams3, "printer");
		} else {
			cl.show(m_jPrinterParams3, "empty");
		}
	}// GEN-LAST:event_jcboMachinePrinter3ActionPerformed

	private void jcboMachinePrinter2ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jcboMachinePrinter2ActionPerformed
		CardLayout cl = (CardLayout) (m_jPrinterParams2.getLayout());

		if ("epson".equals(jcboMachinePrinter2.getSelectedItem())
				|| "tmu220".equals(jcboMachinePrinter2.getSelectedItem())
				|| "star".equals(jcboMachinePrinter2.getSelectedItem())
				|| "ithaca".equals(jcboMachinePrinter2.getSelectedItem())
				|| "surepos".equals(jcboMachinePrinter2.getSelectedItem())) {
			cl.show(m_jPrinterParams2, "comm");
		} else if ("printer".equals(jcboMachinePrinter2.getSelectedItem())) {
			cl.show(m_jPrinterParams2, "printer");
		} else {
			cl.show(m_jPrinterParams2, "empty");
		}
	}// GEN-LAST:event_jcboMachinePrinter2ActionPerformed

	private void jcboMachineDisplayActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jcboMachineDisplayActionPerformed
		CardLayout cl = (CardLayout) (m_jDisplayParams.getLayout());

		if ("epson".equals(jcboMachineDisplay.getSelectedItem()) || "ld200".equals(jcboMachineDisplay.getSelectedItem())
				|| "surepos".equals(jcboMachineDisplay.getSelectedItem())) {
			cl.show(m_jDisplayParams, "comm");
		} else {
			cl.show(m_jDisplayParams, "empty");
		}
	}// GEN-LAST:event_jcboMachineDisplayActionPerformed

	private void jcboMachinePrinterActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jcboMachinePrinterActionPerformed
		CardLayout cl = (CardLayout) (m_jPrinterParams1.getLayout());

		if ("epson".equals(jcboMachinePrinter.getSelectedItem())
				|| "tmu220".equals(jcboMachinePrinter.getSelectedItem())
				|| "star".equals(jcboMachinePrinter.getSelectedItem())
				|| "ithaca".equals(jcboMachinePrinter.getSelectedItem())
				|| "surepos".equals(jcboMachinePrinter.getSelectedItem())) {
			cl.show(m_jPrinterParams1, "comm");
		} else if ("printer".equals(jcboMachinePrinter.getSelectedItem())) {
			cl.show(m_jPrinterParams1, "printer");
		} else {
			cl.show(m_jPrinterParams1, "empty");
		}
	}// GEN-LAST:event_jcboMachinePrinterActionPerformed
		// Variables declaration - do not modify//GEN-BEGIN:variables

	private javax.swing.JComboBox cboPrinters;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel15;
	private javax.swing.JLabel jLabel16;
	private javax.swing.JLabel jLabel18;
	private javax.swing.JLabel jLabel19;
	private javax.swing.JLabel jLabel26;
	private javax.swing.JLabel jLabel5;
	private javax.swing.JLabel jLabel6;
	private javax.swing.JLabel jLabel7;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JPanel jPanel10;
	private javax.swing.JPanel jPanel13;
	private javax.swing.JPanel jPanel19;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JPanel jPanel24;
	private javax.swing.JPanel jPanel5;
	private javax.swing.JPanel jPanel6;
	private javax.swing.JPanel jPanel7;
	private javax.swing.JPanel jPanel8;
	private javax.swing.JPanel jPanel9;
	private javax.swing.JComboBox jcboConnDisplay;
	private javax.swing.JComboBox jcboConnPrinter;
	private javax.swing.JComboBox jcboConnPrinter2;
	private javax.swing.JComboBox jcboConnPrinter3;
	private javax.swing.JComboBox jcboMachineDisplay;
	private javax.swing.JComboBox jcboMachinePrinter;
	private javax.swing.JComboBox jcboMachinePrinter2;
	private javax.swing.JComboBox jcboMachinePrinter3;
	private javax.swing.JComboBox jcboMachineScanner;
	private javax.swing.JComboBox jcboMachineScreenmode;
	private javax.swing.JComboBox jcboSerialDisplay;
	private javax.swing.JComboBox jcboSerialPrinter;
	private javax.swing.JComboBox jcboSerialPrinter2;
	private javax.swing.JComboBox jcboSerialPrinter3;
	private javax.swing.JComboBox jcboSerialScanner;
	private javax.swing.JComboBox jcboTicketsBag;
	private javax.swing.JLabel jlblConnDisplay;
	private javax.swing.JLabel jlblConnPrinter;
	private javax.swing.JLabel jlblConnPrinter2;
	private javax.swing.JLabel jlblConnPrinter3;
	private javax.swing.JLabel jlblDisplayPort;
	private javax.swing.JLabel jlblPrinterPort;
	private javax.swing.JLabel jlblPrinterPort2;
	private javax.swing.JLabel jlblPrinterPort3;
	private javax.swing.JLabel jlblPrinterPort5;
	private javax.swing.JTextField jtxtMachineHostname;
	private javax.swing.JPanel m_jDisplayParams;
	private javax.swing.JPanel m_jPrinterParams1;
	private javax.swing.JPanel m_jPrinterParams2;
	private javax.swing.JPanel m_jPrinterParams3;
	private javax.swing.JPanel m_jScannerParams;
	// End of variables declaration//GEN-END:variables
}
