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
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableColumnModel;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.gui.TableRendererBasic;
import com.openbravo.editor.JEditorCurrency;
import com.openbravo.editor.JEditorKeys;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.forms.JPanelView;

/**
 * Cash movements of the open till: put money in or take it out, and see what
 * has been moved during this cash session.
 */
public class JPanelPayments extends JPanel implements JPanelView, BeanFactoryApp {

	private static final long serialVersionUID = 1L;

	private static final String[] MOVEMENTHEADERS = { "label.paymentdate", "label.paymentreason",
			"label.paymenttotal" };

	private static final Color COLOR_IN = new Color(0, 128, 0);
	private static final Color COLOR_OUT = new Color(178, 34, 34);

	private AppView m_App;
	private DataLogicSales m_dlSales;

	private List<Object[]> m_movements;
	private AbstractTableModel m_movementsmodel;

	private JLabel m_jCashTotal;
	private JEditorCurrency m_jAmount;
	private JEditorKeys m_jKeys;
	private JTable m_jMovements;
	private JButton m_jDelete;

	/** Creates a new instance of JPanelPayments */
	public JPanelPayments() {

		m_movements = new ArrayList<Object[]>();

		initComponents();
	}

	public void init(AppView app) throws BeanFactoryException {

		m_App = app;
		m_dlSales = (DataLogicSales) app.getBean("com.openbravo.pos.forms.DataLogicSales");
	}

	public Object getBean() {
		return this;
	}

	public JComponent getComponent() {
		return this;
	}

	public String getTitle() {
		return AppLocal.getIntString("Menu.Payments");
	}

	public void activate() throws BasicException {

		m_jAmount.reset();
		loadData();
		m_jAmount.activate();
	}

	public boolean deactivate() {
		return true;
	}

	private void loadData() throws BasicException {

		m_jCashTotal.setText(Formats.CURRENCY.formatValue(new Double(m_dlSales.getCashTotal(m_App.getActiveCashIndex()))));

		m_movements = m_dlSales.getCashMovements(m_App.getActiveCashIndex());
		m_movementsmodel.fireTableDataChanged();
		updateDeleteEnabled();
	}

	private void addMovement(boolean cashin) {

		Double amount = m_jAmount.getDoubleValue();
		if (amount == null || amount.doubleValue() <= 0.0) {
			new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cashamountpositive")).show(this);
			m_jAmount.activate();
			return;
		}

		saveMovement(cashin, amount.doubleValue());
		m_jAmount.reset();
		m_jAmount.activate();
	}

	private void reverseSelected() {

		int row = m_jMovements.getSelectedRow();
		if (row < 0 || row >= m_movements.size()) {
			new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cashmovementselect")).show(this);
			m_jAmount.activate();
			return;
		}

		Object[] selected = m_movements.get(row);
		Double total = (Double) selected[2];
		if (total == null || total.doubleValue() == 0.0) {
			m_jAmount.activate();
			return;
		}

		saveMovement(!"cashin".equals(selected[1]), Math.abs(total.doubleValue()));
		m_jAmount.activate();
	}

	private void saveMovement(boolean cashin, double amount) {

		Object[] movement = new Object[] { UUID.randomUUID().toString(), m_App.getActiveCashIndex(), new Date(),
				UUID.randomUUID().toString(), cashin ? "cashin" : "cashout",
				new Double(cashin ? amount : -amount) };

		try {
			m_dlSales.getPaymentMovementInsert().exec(movement);
			loadData();
		} catch (BasicException e) {
			new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotexecute"), e).show(this);
		}
	}

	private void updateDeleteEnabled() {
		m_jDelete.setEnabled(m_jMovements.getSelectedRow() >= 0);
	}

	private void initComponents() {

		setLayout(new BorderLayout());

		add(createTotalPanel(), BorderLayout.NORTH);
		add(createMovementPanel(), BorderLayout.LINE_START);
		add(createListPanel(), BorderLayout.CENTER);
	}

	private JComponent createTotalPanel() {

		JLabel title = new JLabel(AppLocal.getIntString("label.cashdrawer"));

		m_jCashTotal = new JLabel();
		m_jCashTotal.setFont(m_jCashTotal.getFont().deriveFont(Font.BOLD, 24f));
		m_jCashTotal.setHorizontalAlignment(SwingConstants.LEADING);

		JPanel panel = new JPanel(new BorderLayout(10, 0));
		panel.setBorder(BorderFactory.createEmptyBorder(10, 15, 5, 15));
		panel.add(title, BorderLayout.LINE_START);
		panel.add(m_jCashTotal, BorderLayout.CENTER);
		return panel;
	}

	private JComponent createMovementPanel() {

		m_jAmount = new JEditorCurrency();
		m_jAmount.setPreferredSize(new Dimension(0, 45));

		m_jKeys = new JEditorKeys();
		m_jAmount.addEditorKeys(m_jKeys);

		JButton cashin = actionButton(AppLocal.getIntString("button.cashin"), true);
		JButton cashout = actionButton(AppLocal.getIntString("button.cashout"), false);

		JPanel actions = new JPanel(new GridLayout(1, 2, 10, 0));
		actions.add(cashin);
		actions.add(cashout);

		JPanel panel = new JPanel(new BorderLayout(0, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 15, 15, 10));
		panel.setPreferredSize(new Dimension(380, 0));
		panel.add(m_jAmount, BorderLayout.NORTH);
		panel.add(m_jKeys, BorderLayout.CENTER);
		panel.add(actions, BorderLayout.SOUTH);
		return panel;
	}

	private JButton actionButton(String label, final boolean cashin) {

		JButton button = new JButton(label);
		button.setFocusPainted(false);
		button.setFocusable(false);
		button.setFont(button.getFont().deriveFont(Font.BOLD, 18f));
		button.setPreferredSize(new Dimension(0, 60));
		button.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				addMovement(cashin);
			}
		});
		return button;
	}

	private JComponent createListPanel() {

		m_movementsmodel = new AbstractTableModel() {

			private static final long serialVersionUID = 1L;

			public String getColumnName(int column) {
				return AppLocal.getIntString(MOVEMENTHEADERS[column]);
			}

			public int getRowCount() {
				return m_movements.size();
			}

			public int getColumnCount() {
				return MOVEMENTHEADERS.length;
			}

			public Object getValueAt(int row, int column) {
				return m_movements.get(row)[column];
			}
		};

		m_jMovements = new JTable(m_movementsmodel);
		m_jMovements.setDefaultRenderer(Object.class, new MovementRenderer());
		m_jMovements.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		m_jMovements.setRowHeight(25);
		m_jMovements.getTableHeader().setReorderingAllowed(false);
		m_jMovements.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_jMovements.getSelectionModel().addListSelectionListener(new javax.swing.event.ListSelectionListener() {
			public void valueChanged(javax.swing.event.ListSelectionEvent evt) {
				if (!evt.getValueIsAdjusting()) {
					updateDeleteEnabled();
				}
			}
		});

		TableColumnModel columns = m_jMovements.getColumnModel();
		columns.getColumn(0).setPreferredWidth(160);
		columns.getColumn(1).setPreferredWidth(120);
		columns.getColumn(2).setPreferredWidth(100);

		JScrollPane scroll = new JScrollPane(m_jMovements);
		scroll.getVerticalScrollBar().setPreferredSize(new Dimension(25, 25));

		m_jDelete = new JButton(AppLocal.getIntString("button.cashmovementdelete"));
		m_jDelete.setFocusPainted(false);
		m_jDelete.setFocusable(false);
		m_jDelete.setFont(m_jDelete.getFont().deriveFont(Font.BOLD, 18f));
		m_jDelete.setPreferredSize(new Dimension(0, 60));
		m_jDelete.setEnabled(false);
		m_jDelete.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				reverseSelected();
			}
		});

		JPanel panel = new JPanel(new BorderLayout(0, 10));
		panel.setBorder(BorderFactory.createEmptyBorder(5, 10, 15, 15));
		panel.add(new JLabel(AppLocal.getIntString("label.cashmovements")), BorderLayout.NORTH);
		panel.add(scroll, BorderLayout.CENTER);
		panel.add(m_jDelete, BorderLayout.SOUTH);
		return panel;
	}

	// Money in is green, money out is red, in every column of the row
	private static class MovementRenderer extends TableRendererBasic {

		private static final long serialVersionUID = 1L;

		public MovementRenderer() {
			super(new Formats[] { Formats.TIMESTAMP, new FormatsMovement(), Formats.CURRENCY });
		}

		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
				int row, int column) {

			Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
			if (!isSelected) {
				Double total = (Double) table.getModel().getValueAt(row, 2);
				c.setForeground(total != null && total.doubleValue() < 0.0 ? COLOR_OUT : COLOR_IN);
			}
			return c;
		}
	}

	private static class FormatsMovement extends Formats {

		protected String formatValueInt(Object value) {
			return AppLocal.getIntString("transpayment." + (String) value);
		}

		protected Object parseValueInt(String value) throws ParseException {
			return value;
		}

		public int getAlignment() {
			return SwingConstants.LEFT;
		}
	}
}
