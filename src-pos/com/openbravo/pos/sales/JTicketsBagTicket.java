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

package com.openbravo.pos.sales;

import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.TicketLineInfo;
import com.openbravo.pos.ticket.LoyaltyStamps;

import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import javax.swing.*;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.SupervisorAuthorization;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.printer.*;
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.JMessageDialog;
import com.openbravo.format.Formats;
import com.openbravo.pos.customers.DataLogicCustomers;
import com.openbravo.pos.scripting.ScriptEngine;
import com.openbravo.pos.scripting.ScriptException;
import com.openbravo.pos.scripting.ScriptFactory;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.panels.JTicketsFinder;
import com.openbravo.pos.ticket.FindTicketsInfo;
import com.openbravo.beans.JNumberEvent;
import com.openbravo.beans.JNumberEventListener;
import com.openbravo.beans.JNumberKeys;

public class JTicketsBagTicket extends JTicketsBag {

	private DataLogicSystem m_dlSystem = null;
	protected DataLogicCustomers dlCustomers = null;

	private DeviceTicket m_TP;
	private TicketParser m_TTP;
	private TicketParser m_TTP2;

	private TicketInfo m_ticket;
	private TicketInfo m_ticketCopy;

	private JTicketsBagTicketBag m_TicketsBagTicketBag;

	private JPanelTicketEdits m_panelticketedit;

	private static final int RECENT_TICKETS = 10;
	private JTable m_jRecentTickets;

	/** Creates new form JTicketsBagTicket */
	public JTicketsBagTicket(AppView app, JPanelTicketEdits panelticket) {

		super(app, panelticket);
		m_panelticketedit = panelticket;
		m_dlSystem = m_App.getBean(DataLogicSystem.class);
		dlCustomers = m_App.getBean(DataLogicCustomers.class);

		// Inicializo la impresora...
		m_TP = new DeviceTicket();

		// Inicializo el parser de documentos de ticket
		m_TTP = new TicketParser(m_TP, m_dlSystem); // para visualizar el ticket
		m_TTP2 = new TicketParser(m_App.getDeviceTicket(), m_dlSystem); // para imprimir el ticket

		initComponents();
		initRecentTickets();

		m_TicketsBagTicketBag = new JTicketsBagTicketBag(this);

		m_jTicketEditor.addEditorKeys(m_jKeys);

		// Este deviceticket solo tiene una impresora, la de pantalla
		m_jPanelTicket.add(m_TP.getDevicePrinter("1").getPrinterComponent(), BorderLayout.CENTER);
	}

	public void activate() {

		// precondicion es que no tenemos ticket activado ni ticket en el panel

		m_ticket = null;
		m_ticketCopy = null;

		printTicket();

		m_jTicketEditor.reset();
		m_jTicketEditor.activate();

		m_panelticketedit.setActiveTicket(null, null);

		jrbSales.setSelected(true);
		loadRecentTickets();

		m_jEdit.setVisible(m_App.getAppUserView().getUser().hasPermission("sales.EditTicket"));
		m_jRefund.setVisible(m_App.getAppUserView().getUser().hasPermission("sales.RefundTicket"));
		m_jPrint.setVisible(m_App.getAppUserView().getUser().hasPermission("sales.PrintTicket"));
		m_jGiftPrint.setVisible(m_App.getAppUserView().getUser().hasPermission("sales.PrintTicket"));

		// postcondicion es que tenemos ticket activado aqui y ticket en el panel
	}

	public boolean deactivate() {

		// precondicion es que tenemos ticket activado aqui y ticket en el panel
		m_ticket = null;
		m_ticketCopy = null;
		return true;
		// postcondicion es que no tenemos ticket activado ni ticket en el panel
	}

	public void deleteTicket() {

		if (m_ticketCopy != null) {
			// Para editar borramos el ticket anterior
			try {
				m_dlSales.deleteTicket(m_ticketCopy, m_App.getInventoryLocation());
			} catch (BasicException eData) {
				MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.nosaveticket"),
						eData);
				msg.show(this);
			}
		}

		m_ticket = null;
		m_ticketCopy = null;
		resetToTicket();
	}

	public void canceleditionTicket() {

		m_ticketCopy = null;
		resetToTicket();
	}

	private void resetToTicket() {
		printTicket();
		m_jTicketEditor.reset();
		m_jTicketEditor.activate();
		m_panelticketedit.setActiveTicket(null, null);
		loadRecentTickets();
	}

	private void initRecentTickets() {
		m_jRecentTickets = new JTable(new RecentTicketsTableModel());
		m_jRecentTickets.setFocusable(false);
		m_jRecentTickets.setRequestFocusEnabled(false);
		m_jRecentTickets.setRowHeight(40);
		m_jRecentTickets.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_jRecentTickets.getTableHeader().setReorderingAllowed(false);
		m_jRecentTickets.getColumnModel().getColumn(0).setPreferredWidth(70);
		m_jRecentTickets.getColumnModel().getColumn(1).setPreferredWidth(140);
		m_jRecentTickets.getColumnModel().getColumn(2).setPreferredWidth(110);
		m_jRecentTickets.getColumnModel().getColumn(3).setPreferredWidth(160);
		m_jRecentTickets.getColumnModel().getColumn(4).setPreferredWidth(165);
		DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
		centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
		m_jRecentTickets.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
		DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
		rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
		m_jRecentTickets.getColumnModel().getColumn(2).setCellRenderer(rightRenderer);
		m_jRecentTickets.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent event) {
				if (!event.getValueIsAdjusting()) {
					int row = m_jRecentTickets.getSelectedRow();
					if (row >= 0) {
						FindTicketsInfo selected = ((RecentTicketsTableModel) m_jRecentTickets.getModel())
								.getTicketAt(row);
						readTicket(selected.getTicketId(), selected.getTicketType());
					}
				}
			}
		});

		JScrollPane scroll = new JScrollPane(m_jRecentTickets);
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(35, 35));

		JPanel panel = new JPanel(new BorderLayout());
		panel.setBorder(BorderFactory.createTitledBorder(AppLocal.getIntString("label.recentsales")));
		panel.setPreferredSize(new Dimension(720, 0));
		panel.add(scroll, BorderLayout.CENTER);
		add(panel, BorderLayout.WEST);

		java.awt.event.ActionListener reloadRecent = new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				loadRecentTickets();
			}
		};
		jrbSales.addActionListener(reloadRecent);
		jrbRefunds.addActionListener(reloadRecent);
	}

	private void loadRecentTickets() {
		RecentTicketsTableModel model = (RecentTicketsTableModel) m_jRecentTickets.getModel();
		List<FindTicketsInfo> tickets = new ArrayList<FindTicketsInfo>();
		try {
			tickets = m_dlSales.getRecentTickets(jrbSales.isSelected() ? 0 : 1, RECENT_TICKETS);
		} catch (BasicException e) {
			MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotloadticket"),
					e);
			msg.show(this);
		}
		m_jRecentTickets.clearSelection();
		model.setTickets(tickets);
		if (model.getRowCount() > 0) {
			// Make the normal correction flow immediately actionable.
			m_jRecentTickets.setRowSelectionInterval(0, 0);
		}
	}

	private static class RecentTicketsTableModel extends AbstractTableModel {
		private final String[] columns = {"label.ticketid", "label.date", "label.totalcash", "Empleada", "Clienta"};
		private List<FindTicketsInfo> tickets = new ArrayList<FindTicketsInfo>();
		private final SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yy - HH:mm");

		public void setTickets(List<FindTicketsInfo> tickets) {
			this.tickets = tickets;
			fireTableDataChanged();
		}

		public FindTicketsInfo getTicketAt(int row) {
			return tickets.get(row);
		}

		@Override
		public int getRowCount() {
			return tickets.size();
		}

		@Override
		public int getColumnCount() {
			return columns.length;
		}

		@Override
		public String getColumnName(int column) {
			return column < 3 ? AppLocal.getIntString(columns[column]) : columns[column];
		}

		@Override
		public Object getValueAt(int row, int column) {
			FindTicketsInfo ticket = tickets.get(row);
			switch (column) {
			case 0:
				return "[" + ticket.getTicketId() + "]";
			case 1:
				return dateFormat.format(ticket.getDate());
			case 2:
				return Formats.CURRENCY.formatValue(ticket.getTotal());
			case 3:
				return Formats.STRING.formatValue(ticket.getName());
			case 4:
				return ticket.getCustomer() == null ? "" : ticket.getCustomer();
			default:
				return "";
			}
		}
	}

	protected JComponent getBagComponent() {
		return m_TicketsBagTicketBag;
	}

	protected JComponent getNullComponent() {
		return this;
	}

	private void readTicket(int iTicketid, int iTickettype) {

		try {
			TicketInfo ticket = (iTicketid == -1)
					? m_dlSales.loadTicket(iTickettype, m_jTicketEditor.getValueInteger())
					: m_dlSales.loadTicket(iTickettype, iTicketid);

			if (ticket == null) {
				MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
						AppLocal.getIntString("message.notexiststicket"));
				msg.show(this);
			} else {
				m_ticket = ticket;
				m_ticketCopy = null; // se asigna al pulsar el boton de editar o devolver
				printTicket();
			}

		} catch (BasicException e) {
			MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotloadticket"),
					e);
			msg.show(this);
		}

		m_jTicketEditor.reset();
		m_jTicketEditor.activate();
	}

	private void printTicket() {

		// imprimo m_ticket

		// Cash-state validation is performed when editing starts, after the
		// supervisor authorization can explain why the operation is rejected.
		m_jEdit.setEnabled(m_ticket != null && (m_ticket.getTicketType() == TicketInfo.RECEIPT_NORMAL
				|| m_ticket.getTicketType() == TicketInfo.RECEIPT_REFUND));
		m_jRefund.setEnabled(m_ticket != null && m_ticket.getTicketType() == TicketInfo.RECEIPT_NORMAL);
		m_jPrint.setEnabled(m_ticket != null);
		m_jGiftPrint.setEnabled(m_ticket != null);

		// Este deviceticket solo tiene una impresora, la de pantalla
		m_TP.getDevicePrinter("1").reset();

		if (m_ticket == null) {
			m_jTicketId.setText(null);
		} else {
			m_jTicketId.setText(m_ticket.getName());

			try {
				LoyaltyStamps.applyToTicket(m_ticket, m_App.getProperties().getProperty(LoyaltyStamps.ENABLED_KEY),
						m_App.getProperties().getProperty(LoyaltyStamps.NAME_KEY));
				ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);
				script.put("ticket", m_ticket);
				m_TTP.printTicket(script.eval(m_dlSystem.getResourceAsXML("Printer.TicketPreview")).toString());
			} catch (ScriptException e) {
				MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
						AppLocal.getIntString("message.cannotprintticket"), e);
				msg.show(this);
			} catch (TicketPrinterException eTP) {
				MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
						AppLocal.getIntString("message.cannotprintticket"), eTP);
				msg.show(this);
			}
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
		java.awt.GridBagConstraints gridBagConstraints;

		buttonGroup1 = new javax.swing.ButtonGroup();
		m_jOptions = new javax.swing.JPanel();
		m_jButtons = new javax.swing.JPanel();
		m_jTicketId = new javax.swing.JLabel();
		jButton2 = new javax.swing.JButton();
		m_jEdit = new javax.swing.JButton();
		m_jRefund = new javax.swing.JButton();
		m_jPrint = new javax.swing.JButton();
		m_jGiftPrint = new javax.swing.JButton();
		jPanel2 = new javax.swing.JPanel();
		m_jPanelTicket = new javax.swing.JPanel();
		jPanel3 = new javax.swing.JPanel();
		jPanel4 = new javax.swing.JPanel();
		m_jKeys = new com.openbravo.editor.JEditorKeys();
		m_jNumberKeys = new com.openbravo.beans.JNumberKeys();
		jPanel5 = new javax.swing.JPanel();
		jButton1 = new javax.swing.JButton();
		m_jTicketEditor = new com.openbravo.editor.JEditorIntegerPositive();
		jPanel1 = new javax.swing.JPanel();
		jrbSales = new javax.swing.JRadioButton();
		jrbRefunds = new javax.swing.JRadioButton();

		setLayout(new java.awt.BorderLayout());

		m_jOptions.setLayout(new java.awt.BorderLayout());

		m_jButtons.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));

		m_jTicketId.setBackground(com.openbravo.pos.theme.RetailPOSColors.surface100());
		m_jTicketId.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		m_jTicketId.setBorder(javax.swing.BorderFactory.createCompoundBorder(
				javax.swing.BorderFactory
						.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")),
				javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
		m_jTicketId.setOpaque(true);
		m_jTicketId.setPreferredSize(new java.awt.Dimension(160, 25));
		m_jTicketId.setRequestFocusEnabled(false);
		m_jButtons.add(m_jTicketId);

		jButton2.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/search.png"))); // NOI18N
		jButton2.setText(AppLocal.getIntString("label.search")); // NOI18N
		jButton2.setFocusPainted(false);
		jButton2.setFocusable(false);
		jButton2.setMargin(new java.awt.Insets(8, 14, 8, 14));
		jButton2.setRequestFocusEnabled(false);
		jButton2.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton2ActionPerformed(evt);
			}
		});
		m_jButtons.add(jButton2);

		m_jEdit.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/edit.png"))); // NOI18N
		m_jEdit.setText(AppLocal.getIntString("button.edit")); // NOI18N
		m_jEdit.setFocusPainted(false);
		m_jEdit.setFocusable(false);
		m_jEdit.setMargin(new java.awt.Insets(8, 14, 8, 14));
		m_jEdit.setRequestFocusEnabled(false);
		m_jEdit.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jEditActionPerformed(evt);
			}
		});
		m_jButtons.add(m_jEdit);

		m_jRefund.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/inbox.png"))); // NOI18N
		m_jRefund.setText(AppLocal.getIntString("button.refund")); // NOI18N
		m_jRefund.setFocusPainted(false);
		m_jRefund.setFocusable(false);
		m_jRefund.setMargin(new java.awt.Insets(8, 14, 8, 14));
		m_jRefund.setRequestFocusEnabled(false);
		m_jRefund.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jRefundActionPerformed(evt);
			}
		});
		m_jButtons.add(m_jRefund);

		m_jPrint.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/yast_printer.png"))); // NOI18N
		m_jPrint.setText(AppLocal.getIntString("button.print")); // NOI18N
		m_jPrint.setFocusPainted(false);
		m_jPrint.setFocusable(false);
		m_jPrint.setMargin(new java.awt.Insets(8, 14, 8, 14));
		m_jPrint.setRequestFocusEnabled(false);
		m_jPrint.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jPrintActionPerformed(evt);
			}
		});
		m_jButtons.add(m_jPrint);

		m_jGiftPrint
				.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/yast_printer.png"))); // NOI18N
		m_jGiftPrint.setText(AppLocal.getIntString("button.giftreceipt")); // NOI18N
		m_jGiftPrint.setFocusPainted(false);
		m_jGiftPrint.setFocusable(false);
		m_jGiftPrint.setMargin(new java.awt.Insets(8, 14, 8, 14));
		m_jGiftPrint.setRequestFocusEnabled(false);
		m_jGiftPrint.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jGiftPrintActionPerformed(evt);
			}
		});
		m_jButtons.add(m_jGiftPrint);

		m_jOptions.add(m_jButtons, java.awt.BorderLayout.WEST);

		jPanel2.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.LEFT));
		m_jOptions.add(jPanel2, java.awt.BorderLayout.CENTER);

		add(m_jOptions, java.awt.BorderLayout.NORTH);

		m_jPanelTicket.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		m_jPanelTicket.setLayout(new java.awt.BorderLayout());
		add(m_jPanelTicket, java.awt.BorderLayout.CENTER);

		jPanel3.setLayout(new java.awt.BorderLayout());

		jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.Y_AXIS));

		m_jKeys.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jKeysActionPerformed(evt);
			}
		});
		m_jNumberKeys.setNumbersOnly(false);
		m_jNumberKeys.addJNumberEventListener(new JNumberEventListener() {
			@Override
			public void keyPerformed(JNumberEvent event) {
				if (event.getKey() == '=') {
					readTicket(-1, jrbSales.isSelected() ? 0 : 1);
				} else if (event.getKey() == '\u007f') {
					m_jTicketEditor.reset();
				} else if (event.getKey() >= '0' && event.getKey() <= '9') {
					m_jTicketEditor.transChar(event.getKey());
				}
			}
		});
		jPanel4.add(m_jNumberKeys);

		jPanel5.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		jPanel5.setLayout(new java.awt.GridBagLayout());

		jButton1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/button_ok.png"))); // NOI18N
		jButton1.setFocusPainted(false);
		jButton1.setFocusable(false);
		jButton1.setMargin(new java.awt.Insets(8, 14, 8, 14));
		jButton1.setRequestFocusEnabled(false);
		jButton1.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jButton1ActionPerformed(evt);
			}
		});
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.fill = java.awt.GridBagConstraints.BOTH;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(0, 5, 0, 0);
		jPanel5.add(jButton1, gridBagConstraints);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		jPanel5.add(m_jTicketEditor, gridBagConstraints);

		jPanel4.add(jPanel5);

		jPanel3.add(jPanel4, java.awt.BorderLayout.NORTH);

		buttonGroup1.add(jrbSales);
		jrbSales.setText(AppLocal.getIntString("label.sales")); // NOI18N
		jrbSales.setFocusPainted(false);
		jrbSales.setFocusable(false);
		jrbSales.setRequestFocusEnabled(false);
		jPanel1.add(jrbSales);

		buttonGroup1.add(jrbRefunds);
		jrbRefunds.setText(AppLocal.getIntString("label.refunds")); // NOI18N
		jrbRefunds.setFocusPainted(false);
		jrbRefunds.setFocusable(false);
		jrbRefunds.setRequestFocusEnabled(false);
		jPanel1.add(jrbRefunds);

		jPanel3.add(jPanel1, java.awt.BorderLayout.CENTER);

		add(jPanel3, java.awt.BorderLayout.EAST);
	}// </editor-fold>//GEN-END:initComponents

	private void m_jEditActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jEditActionPerformed
		if (!SupervisorAuthorization.authorize(this, m_App, AppLocal.getIntString("message.authorizedticketedit"))) {
			return;
		}
		try {
			if (!m_dlSales.isCashActive(m_ticket.getActiveCash())) {
				return;
			}
		} catch (BasicException e) {
			return;
		}

		m_ticketCopy = m_ticket;
		m_TicketsBagTicketBag.showEdit();
		m_panelticketedit.showCatalog();
		m_panelticketedit.setActiveTicket(m_ticket.copyTicket(), null);

	}// GEN-LAST:event_m_jEditActionPerformed

	private void m_jPrintActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jPrintActionPerformed

		if (m_ticket != null) {
			try {
				LoyaltyStamps.applyToTicket(m_ticket, m_App.getProperties().getProperty(LoyaltyStamps.ENABLED_KEY),
						m_App.getProperties().getProperty(LoyaltyStamps.NAME_KEY));
				ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);
				script.put("ticket", m_ticket);
				m_TTP2.printTicket(script.eval(m_dlSystem.getResourceAsXML("Printer.TicketPreview")).toString());
			} catch (ScriptException e) {
				JMessageDialog.showMessage(this,
						new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.cannotprint"), e));
			} catch (TicketPrinterException e) {
				JMessageDialog.showMessage(this,
						new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.cannotprint"), e));
			}
		}

	}// GEN-LAST:event_m_jPrintActionPerformed

	private void m_jGiftPrintActionPerformed(java.awt.event.ActionEvent evt) {
		if (m_ticket != null) {
			String sresource = m_dlSystem.getResourceAsXML("Printer.TicketGift");
			if (sresource == null) {
				JMessageDialog.showMessage(this,
						new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.cannotprintticket")));
				return;
			}
			try {
				ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);
				script.put("ticket", m_ticket);
				m_TTP2.printTicket(script.eval(sresource).toString());
			} catch (ScriptException e) {
				JMessageDialog.showMessage(this,
						new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.cannotprint"), e));
			} catch (TicketPrinterException e) {
				JMessageDialog.showMessage(this,
						new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.cannotprint"), e));
			}
		}
	}

	private void m_jRefundActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jRefundActionPerformed
		if (!SupervisorAuthorization.authorize(this, m_App, AppLocal.getIntString("message.authorizedrefund"))) {
			return;
		}

		java.util.List aRefundLines = new ArrayList();

		for (int i = 0; i < m_ticket.getLinesCount(); i++) {
			TicketLineInfo newline = new TicketLineInfo(m_ticket.getLine(i));
			aRefundLines.add(newline);
		}

		m_ticketCopy = null;
		m_TicketsBagTicketBag.showRefund();
		m_panelticketedit.showRefundLines(aRefundLines);

		TicketInfo refundticket = new TicketInfo();
		refundticket.setTicketType(TicketInfo.RECEIPT_REFUND);
		refundticket.setCustomer(m_ticket.getCustomer());
		refundticket.setPayments(m_ticket.getPayments());
		m_panelticketedit.setActiveTicket(refundticket, null);

	}// GEN-LAST:event_m_jRefundActionPerformed

	private void jButton1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton1ActionPerformed

		readTicket(-1, jrbSales.isSelected() ? 0 : 1);

	}// GEN-LAST:event_jButton1ActionPerformed

	private void m_jKeysActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jKeysActionPerformed

		readTicket(-1, jrbSales.isSelected() ? 0 : 1);

	}// GEN-LAST:event_m_jKeysActionPerformed

	private void jButton2ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jButton2ActionPerformed
		JTicketsFinder finder = JTicketsFinder.getReceiptFinder(this, m_dlSales, dlCustomers);
		finder.setVisible(true);
		FindTicketsInfo selectedTicket = finder.getSelectedCustomer();
		if (selectedTicket == null) {
			m_jTicketEditor.reset();
			m_jTicketEditor.activate();
		} else {
			readTicket(selectedTicket.getTicketId(), selectedTicket.getTicketType());
		}
	}// GEN-LAST:event_jButton2ActionPerformed

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.ButtonGroup buttonGroup1;
	private javax.swing.JButton jButton1;
	private javax.swing.JButton jButton2;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JPanel jPanel3;
	private javax.swing.JPanel jPanel4;
	private javax.swing.JPanel jPanel5;
	private javax.swing.JRadioButton jrbRefunds;
	private javax.swing.JRadioButton jrbSales;
	private javax.swing.JPanel m_jButtons;
	private javax.swing.JButton m_jEdit;
	private com.openbravo.editor.JEditorKeys m_jKeys;
	private com.openbravo.beans.JNumberKeys m_jNumberKeys;
	private javax.swing.JPanel m_jOptions;
	private javax.swing.JPanel m_jPanelTicket;
	private javax.swing.JButton m_jPrint;
	private javax.swing.JButton m_jGiftPrint;
	private javax.swing.JButton m_jRefund;
	private com.openbravo.editor.JEditorIntegerPositive m_jTicketEditor;
	private javax.swing.JLabel m_jTicketId;
	// End of variables declaration//GEN-END:variables

}
