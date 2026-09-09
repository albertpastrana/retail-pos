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

package com.openbravo.pos.panels;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.forms.JPanelView;
import com.openbravo.pos.printer.DeviceTicket;
import com.openbravo.pos.printer.TicketParser;
import com.openbravo.pos.printer.TicketPrinterException;
import com.openbravo.pos.scripting.ScriptEngine;
import com.openbravo.pos.scripting.ScriptException;
import com.openbravo.pos.scripting.ScriptFactory;

public class JPanelClosedCash extends JPanel implements JPanelView, BeanFactoryApp {

	// Stores closing twice a day burn two entries per date.
	private static final int RECENT_CLOSINGS = 20;

	private AppView m_App;
	private DataLogicSystem m_dlSystem;

	private DeviceTicket m_TP;
	private TicketParser m_TTPpreview;
	private TicketParser m_TTPprinter;

	private PaymentsModel m_payments;

	private JList m_jClosings;
	private JButton m_jPrint;
	private JPanel m_jPanelTicket;

	public JPanelClosedCash() {
		initComponents();
	}

	public void init(AppView app) throws BeanFactoryException {

		m_App = app;
		m_dlSystem = (DataLogicSystem) m_App.getBean("com.openbravo.pos.forms.DataLogicSystem");

		// Este deviceticket solo tiene una impresora, la de pantalla
		m_TP = new DeviceTicket();
		m_TTPpreview = new TicketParser(m_TP, m_dlSystem);
		m_TTPprinter = new TicketParser(m_App.getDeviceTicket(), m_dlSystem);

		m_jPanelTicket.add(m_TP.getDevicePrinter("1").getPrinterComponent(), BorderLayout.CENTER);
	}

	public Object getBean() {
		return this;
	}

	public JComponent getComponent() {
		return this;
	}

	public String getTitle() {
		return AppLocal.getIntString("Menu.Closing");
	}

	public void activate() throws BasicException {
		loadClosings();
		showClosing(null);
	}

	public boolean deactivate() {
		return true;
	}

	private void loadClosings() {

		DefaultListModel model = new DefaultListModel();
		try {
			List closings = m_dlSystem.listClosedCash(RECENT_CLOSINGS);
			for (int i = 0; i < closings.size(); i++) {
				model.addElement(closings.get(i));
			}
		} catch (BasicException e) {
			MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotloadticket"),
					e);
			msg.show(this);
		}
		m_jClosings.setModel(model);
	}

	private void showClosing(ClosedCashInfo closed) {

		m_TP.getDevicePrinter("1").reset();

		if (closed == null) {
			m_payments = null;
			m_jPrint.setEnabled(false);
			return;
		}

		try {
			m_payments = PaymentsModel.loadInstance(m_App, closed);
		} catch (BasicException e) {
			m_payments = null;
			m_jPrint.setEnabled(false);
			MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotloadticket"),
					e);
			msg.show(this);
			return;
		}

		m_jPrint.setEnabled(true);
		printPayments(m_TTPpreview);
	}

	private void printPayments(TicketParser parser) {

		String sresource = m_dlSystem.getResourceAsXML("Printer.CloseCash");
		if (sresource == null) {
			MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotprintticket"));
			msg.show(this);
		} else {
			try {
				ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);
				script.put("payments", m_payments);
				parser.printTicket(script.eval(sresource).toString());
			} catch (ScriptException e) {
				MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
						AppLocal.getIntString("message.cannotprintticket"), e);
				msg.show(this);
			} catch (TicketPrinterException e) {
				MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
						AppLocal.getIntString("message.cannotprintticket"), e);
				msg.show(this);
			}
		}
	}

	private void initComponents() {

		setLayout(new BorderLayout());

		m_jClosings = new JList();
		m_jClosings.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_jClosings.setFocusable(false);
		m_jClosings.setRequestFocusEnabled(false);
		m_jClosings.addMouseListener(new MouseAdapter() {
			public void mouseClicked(MouseEvent evt) {
				showClosing((ClosedCashInfo) m_jClosings.getSelectedValue());
			}
		});

		JScrollPane scroll = new JScrollPane(m_jClosings);
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(35, 35));

		JPanel listpanel = new JPanel(new BorderLayout());
		listpanel.setBorder(BorderFactory.createTitledBorder(AppLocal.getIntString("label.recentclosings")));
		listpanel.setPreferredSize(new Dimension(460, 0));
		listpanel.add(scroll, BorderLayout.CENTER);
		add(listpanel, BorderLayout.WEST);

		m_jPrint = new JButton(AppLocal.getIntString("Button.PrintCash"));
		m_jPrint.setEnabled(false);
		m_jPrint.setFocusPainted(false);
		m_jPrint.setFocusable(false);
		m_jPrint.setMargin(new java.awt.Insets(8, 14, 8, 14));
		m_jPrint.setRequestFocusEnabled(false);
		m_jPrint.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				printPayments(m_TTPprinter);
			}
		});

		JPanel buttons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		buttons.add(m_jPrint);

		m_jPanelTicket = new JPanel(new BorderLayout());

		JPanel ticketpanel = new JPanel(new BorderLayout());
		ticketpanel.add(buttons, BorderLayout.NORTH);
		ticketpanel.add(m_jPanelTicket, BorderLayout.CENTER);
		add(ticketpanel, BorderLayout.CENTER);
	}
}
