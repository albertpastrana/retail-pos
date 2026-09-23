//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2008-2009 Openbravo, S.L.
//    http://www.openbravo.com/product/pos
package com.openbravo.pos.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.WindowConstants;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;

import com.openbravo.basic.BasicException;
import com.openbravo.beans.JCalendarDialog;
import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.gui.ListQBFModelNumber;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.loader.QBFCompareEnum;
import com.openbravo.data.user.EditorCreator;
import com.openbravo.data.user.ListProvider;
import com.openbravo.data.user.ListProviderCreator;
import com.openbravo.format.Formats;
import com.openbravo.pos.customers.DataLogicCustomers;
import com.openbravo.pos.customers.JCustomerFinder;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.inventory.TaxCategoryInfo;
import com.openbravo.pos.ticket.FindTicketsInfo;

public class JTicketsFinder extends JDialog implements EditorCreator {

	private static final java.util.logging.Logger LOGGER = java.util.logging.Logger
			.getLogger(JTicketsFinder.class.getName());

	private ListProvider lpr;
	private DataLogicSales dlSales;
	private DataLogicCustomers dlCustomers;
	private ComboBoxValModel userModel;
	private FindTicketsInfo selectedTicket;
	private JTable ticketTable;
	private JScrollPane resultScrollPane;
	private JTextField ticketIdField;
	private JComboBox<String> ticketTypeCombo;
	private JTextField startDateField;
	private JTextField endDateField;
	private JTextField customerField;
	private JComboBox userCombo;
	private JComboBox moneyCompareCombo;
	private JTextField moneyField;
	private JButton selectButton;
	private Timer searchTimer;

	private JTicketsFinder(Frame parent, boolean modal) { super(parent, modal); }
	private JTicketsFinder(Dialog parent, boolean modal) { super(parent, modal); }

	public static JTicketsFinder getReceiptFinder(Component parent, DataLogicSales dlSales,
			DataLogicCustomers dlCustomers) {
		Window window = getWindow(parent);
		JTicketsFinder finder = window instanceof Frame
				? new JTicketsFinder((Frame) window, true)
				: new JTicketsFinder((Dialog) window, true);
		finder.init(dlSales, dlCustomers);
		finder.applyComponentOrientation(parent.getComponentOrientation());
		return finder;
	}

	public FindTicketsInfo getSelectedCustomer() {
		return selectedTicket;
	}

	private void init(DataLogicSales sales, DataLogicCustomers customers) {
		dlSales = sales;
		dlCustomers = customers;
		initComponents();
		lpr = new ListProviderCreator(dlSales.getTicketsList(), this);
		initCombos();
		defaultValues();
		installSearchListeners();
		setLocationRelativeTo(getOwner());
	}

	private void initCombos() {
		ticketTypeCombo.setModel(new DefaultComboBoxModel<>(new String[]{
				AppLocal.getIntString("label.sales"), AppLocal.getIntString("label.refunds"),
				AppLocal.getIntString("label.all")}));
		moneyCompareCombo.setModel(ListQBFModelNumber.getMandatoryNumber());

		try {
			List users = dlSales.getUserList().list();
			users.add(0, null);
			userModel = new ComboBoxValModel(users);
			userCombo.setModel(userModel);
		} catch (BasicException exception) {
			LOGGER.log(java.util.logging.Level.WARNING, "event=ticket_users_load_failed", exception);
			userModel = new ComboBoxValModel(Collections.emptyList());
			userCombo.setModel(userModel);
		}
	}

	private void defaultValues() {
		selectedTicket = null;
		ticketTable.setModel(new TicketTableModel(Collections.emptyList()));
		showTicketMessage("message.ticketfilter");
		selectButton.setEnabled(false);
		userCombo.setSelectedItem(null);
		ticketIdField.setText("");
		ticketTypeCombo.setSelectedIndex(0);
		moneyCompareCombo.setSelectedItem(((ListQBFModelNumber) moneyCompareCombo.getModel()).getElementAt(0));
		moneyField.setText("");
		startDateField.setText("");
		endDateField.setText("");
		customerField.setText("");
	}

	private void installSearchListeners() {
		searchTimer = new Timer(300, event -> executeSearch());
		searchTimer.setRepeats(false);
		addDebouncedListener(ticketIdField);
		addDebouncedListener(startDateField);
		addDebouncedListener(endDateField);
		addDebouncedListener(customerField);
		addDebouncedListener(moneyField);
		addDebouncedListener(ticketTypeCombo);
		addDebouncedListener(userCombo);
		addDebouncedListener(moneyCompareCombo);
	}

	private void addDebouncedListener(JComboBox component) {
		component.addActionListener(event -> searchTimer.restart());
	}

	private void addDebouncedListener(JTextField field) {
		field.getDocument().addDocumentListener(new DocumentListener() {
			@Override public void insertUpdate(DocumentEvent event) { searchTimer.restart(); }
			@Override public void removeUpdate(DocumentEvent event) { searchTimer.restart(); }
			@Override public void changedUpdate(DocumentEvent event) { searchTimer.restart(); }
		});
	}

	public void executeSearch() {
		try {
			List<FindTicketsInfo> tickets = lpr.loadData();
			ticketTable.setModel(new TicketTableModel(tickets));
			if (tickets.isEmpty()) {
				showTicketMessage("message.ticketfilter.empty");
				selectButton.setEnabled(false);
			} else {
				resultScrollPane.setViewportView(ticketTable);
				setColumnWidths();
				ticketTable.setRowSelectionInterval(0, 0);
			}
		} catch (BasicException exception) {
			LOGGER.log(java.util.logging.Level.WARNING, "event=ticket_search_failed", exception);
		}
	}

	@Override
	public Object createValue() throws BasicException {
		Object[] filter = new Object[14];
		String ticketId = ticketIdField.getText();
		filter[0] = ticketId.isEmpty() ? QBFCompareEnum.COMP_NONE : QBFCompareEnum.COMP_EQUALS;
		filter[1] = ticketId.isEmpty() ? null : Formats.INT.parseValue(ticketId);

		if (ticketTypeCombo.getSelectedIndex() == 2) {
			filter[2] = QBFCompareEnum.COMP_DISTINCT;
			filter[3] = 2;
		} else {
			filter[2] = QBFCompareEnum.COMP_EQUALS;
			filter[3] = ticketTypeCombo.getSelectedIndex();
		}

		filter[5] = Formats.CURRENCY.parseValue(moneyField.getText());
		filter[4] = filter[5] == null ? QBFCompareEnum.COMP_NONE : moneyCompareCombo.getSelectedItem();
		Date startDate = (Date) Formats.TIMESTAMP.parseValue(startDateField.getText());
		Date endDate = (Date) Formats.TIMESTAMP.parseValue(endDateField.getText());
		filter[6] = startDate == null ? QBFCompareEnum.COMP_NONE : QBFCompareEnum.COMP_GREATEROREQUALS;
		filter[7] = startDate;
		filter[8] = endDate == null ? QBFCompareEnum.COMP_NONE : QBFCompareEnum.COMP_LESS;
		filter[9] = endDate;

		Object user = userCombo.getSelectedItem();
		filter[10] = user == null ? QBFCompareEnum.COMP_NONE : QBFCompareEnum.COMP_EQUALS;
		filter[11] = user == null ? null : ((TaxCategoryInfo) user).getName();
		String customer = customerField.getText();
		filter[12] = customer.isEmpty() ? QBFCompareEnum.COMP_NONE : QBFCompareEnum.COMP_CONTAINS;
		filter[13] = customer.isEmpty() ? null : customer;
		return filter;
	}

	private void initComponents() {
		setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
		setTitle(AppLocal.getIntString("form.tickettitle"));

		ticketIdField = new JTextField();
		ticketTypeCombo = new JComboBox<>();
		startDateField = new JTextField();
		endDateField = new JTextField();
		customerField = new JTextField();
		userCombo = new JComboBox();
		moneyCompareCombo = new JComboBox();
		moneyField = new JTextField();
		JPanel filterPanel = new JPanel(new GridBagLayout());
		filterPanel.setBorder(BorderFactory.createEmptyBorder(12, 18, 8, 18));

		addFilterRow(filterPanel, 0, AppLocal.getIntString("label.ticketid"), ticketIdField, ticketTypeCombo,
				AppLocal.getIntString("label.sales"));
		addDateRow(filterPanel, 1, AppLocal.getIntString("Label.StartDate"), startDateField,
				createDateButton(startDateField));
		addDateRow(filterPanel, 2, AppLocal.getIntString("Label.EndDate"), endDateField,
				createDateButton(endDateField));
		addCustomerRow(filterPanel, 3);
		addSingleRow(filterPanel, 4, AppLocal.getIntString("label.user"), userCombo);
		addFilterRow(filterPanel, 5, AppLocal.getIntString("label.totalcash"), moneyCompareCombo, moneyField,
				AppLocal.getIntString("label.totalcash"));

		ticketTable = new JTable(new TicketTableModel(Collections.emptyList()));
		ticketTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		ticketTable.setAutoCreateRowSorter(true);
		ticketTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		ticketTable.setRowHeight(36);
		resultScrollPane = new JScrollPane(ticketTable);
		resultScrollPane.setPreferredSize(new java.awt.Dimension(800, 350));

		JPanel buttons = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
		selectButton = new JButton(AppLocal.getIntString("button.selectticket"));
		selectButton.setEnabled(false);
		selectButton.addActionListener(event -> selectTicket());
		JButton cancelButton = new JButton(AppLocal.getIntString("button.cancelticket"));
		cancelButton.addActionListener(event -> dispose());
		buttons.add(cancelButton);
		buttons.add(selectButton);

		ticketTable.getSelectionModel().addListSelectionListener(event ->
				selectButton.setEnabled(ticketTable.getSelectedRow() >= 0
						&& ticketTable.getModel().getRowCount() > 0));
		ticketTable.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override public void mouseClicked(java.awt.event.MouseEvent event) {
				if (event.getClickCount() == 2) selectTicket();
			}
		});

		JPanel content = new JPanel(new BorderLayout());
		content.add(filterPanel, BorderLayout.NORTH);
		content.add(resultScrollPane, BorderLayout.CENTER);
		content.add(buttons, BorderLayout.SOUTH);
		setContentPane(content);
		showTicketMessage("message.ticketfilter");
		pack();
	}

	private void addFilterRow(JPanel panel, int row, String label, Component first, Component second,
			String secondLabel) {
		GridBagConstraints labelConstraints = constraints(0, row, 0, 0, GridBagConstraints.LINE_END);
		GridBagConstraints firstConstraints = constraints(1, row, 1, 1, GridBagConstraints.LINE_START);
		GridBagConstraints secondConstraints = constraints(2, row, 1, 1, GridBagConstraints.LINE_START);
		panel.add(new JLabel(label), labelConstraints);
		panel.add(first, firstConstraints);
		panel.add(second, secondConstraints);
	}

	private void addDateRow(JPanel panel, int row, String label, JTextField field, JButton button) {
		panel.add(new JLabel(label), constraints(0, row, 0, 0, GridBagConstraints.LINE_END));
		panel.add(field, constraints(1, row, 1, 1, GridBagConstraints.LINE_START));
		panel.add(button, constraints(2, row, 0, 0, GridBagConstraints.LINE_START));
	}

	private void addCustomerRow(JPanel panel, int row) {
		panel.add(new JLabel(AppLocal.getIntString("label.customer")), constraints(0, row, 0, 0,
				GridBagConstraints.LINE_END));
		panel.add(customerField, constraints(1, row, 1, 1, GridBagConstraints.LINE_START));
		JButton button = new JButton(AppLocal.getIntString("button.selectcustomer"));
		button.addActionListener(event -> selectCustomer());
		panel.add(button, constraints(2, row, 0, 0, GridBagConstraints.LINE_START));
	}

	private void addSingleRow(JPanel panel, int row, String label, Component component) {
		GridBagConstraints value = constraints(1, row, 1, 1, GridBagConstraints.LINE_START);
		value.gridwidth = 2;
		panel.add(new JLabel(label), constraints(0, row, 0, 0, GridBagConstraints.LINE_END));
		panel.add(component, value);
	}

	private GridBagConstraints constraints(int x, int y, double weightx, double weighty, int anchor) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = x;
		constraints.gridy = y;
		constraints.weightx = weightx;
		constraints.weighty = weighty;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = anchor;
		constraints.insets = new Insets(5, 8, 5, 8);
		return constraints;
	}

	private JButton createDateButton(JTextField field) {
		JButton button = new JButton("...");
		button.addActionListener(event -> selectDate(field));
		return button;
	}

	private void selectDate(JTextField field) {
		Date date;
		try {
			date = (Date) Formats.TIMESTAMP.parseValue(field.getText());
		} catch (BasicException exception) {
			date = null;
		}
		date = JCalendarDialog.showCalendarTimeHours(this, date);
		if (date != null) field.setText(Formats.TIMESTAMP.formatValue(date));
	}

	private void selectCustomer() {
		JCustomerFinder finder = JCustomerFinder.getCustomerFinder(this, dlCustomers);
		finder.search(null);
		finder.setVisible(true);
		try {
			customerField.setText(finder.getSelectedCustomer() == null ? ""
					: dlSales.loadCustomerExt(finder.getSelectedCustomer().getId()).toString());
		} catch (BasicException exception) {
			new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotfindcustomer"), exception)
					.show(this);
		}
	}

	private void selectTicket() {
		int row = ticketTable.getSelectedRow();
		if (row >= 0) {
			selectedTicket = ((TicketTableModel) ticketTable.getModel()).tickets
					.get(ticketTable.convertRowIndexToModel(row));
			dispose();
		}
	}

	private void showTicketMessage(String messageKey) {
		resultScrollPane.setViewportView(new JLabel(AppLocal.getIntString(messageKey), SwingConstants.CENTER));
	}

	private void setColumnWidths() {
		int width = resultScrollPane.getPreferredSize().width;
		ticketTable.getColumnModel().getColumn(0).setPreferredWidth(width / 8);
		ticketTable.getColumnModel().getColumn(1).setPreferredWidth(width / 4);
		ticketTable.getColumnModel().getColumn(2).setPreferredWidth(width * 3 / 8);
		ticketTable.getColumnModel().getColumn(3).setPreferredWidth(width / 8);
		ticketTable.getColumnModel().getColumn(4).setPreferredWidth(width / 8);
	}

	private static Window getWindow(Component parent) {
		if (parent == null) return new JFrame();
		if (parent instanceof Frame || parent instanceof Dialog) return (Window) parent;
		return getWindow(parent.getParent());
	}

	private static class TicketTableModel extends AbstractTableModel {
		private final List<FindTicketsInfo> tickets;
		private final String[] columns = {AppLocal.getIntString("label.ticketid"),
				AppLocal.getIntString("label.date"), AppLocal.getIntString("label.customer"),
				AppLocal.getIntString("label.totalcash"), AppLocal.getIntString("label.user")};

		TicketTableModel(List<FindTicketsInfo> tickets) { this.tickets = tickets; }
		@Override public int getRowCount() { return tickets.size(); }
		@Override public int getColumnCount() { return columns.length; }
		@Override public String getColumnName(int column) { return columns[column]; }
		@Override public Object getValueAt(int row, int column) {
			FindTicketsInfo ticket = tickets.get(row);
			switch (column) {
				case 0: return ticket.getTicketId();
				case 1: return Formats.TIMESTAMP.formatValue(ticket.getDate());
				case 2: return ticket.getCustomer() == null ? "" : ticket.getCustomer();
				case 3: return Formats.CURRENCY.formatValue(ticket.getTotal());
				case 4: return ticket.getName() == null ? "" : ticket.getName();
				default: return "";
			}
		}
	}
}
