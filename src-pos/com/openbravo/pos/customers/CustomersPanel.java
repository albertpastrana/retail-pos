package com.openbravo.pos.customers;

import com.openbravo.basic.BasicException;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.JPanelView;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.util.Collections;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;

public class CustomersPanel extends JPanel implements JPanelView, BeanFactoryApp {

	private final CustomersTableModel model = new CustomersTableModel();
	private final JTable table = new JTable(model);
	private final JTextField search = new JTextField();
	private final JTextField name = new JTextField();
	private final JTextField phone = new JTextField();
	private final JTextField debt = new JTextField();
	private final JCheckBox visible = new JCheckBox();
	private final JButton save = new JButton();
	private final JButton newCustomer = new JButton();
	private DataLogicCustomers customers;
	private CustomerInfoExt selected;
	private boolean creating;

	public CustomersPanel() {
		setLayout(new BorderLayout(8, 8));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

		JPanel toolbar = new JPanel(new BorderLayout(8, 0));
		toolbar.add(new JLabel(AppLocal.getIntString("customer.find")), BorderLayout.WEST);
		toolbar.add(search, BorderLayout.CENTER);
		newCustomer.setText(AppLocal.getIntString("customer.new"));
		toolbar.add(newCustomer, BorderLayout.EAST);
		add(toolbar, BorderLayout.NORTH);

		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(28);
		table.setFillsViewportHeight(true);
		table.getSelectionModel().addListSelectionListener(e -> selectCustomer());

		JPanel detail = new JPanel(new BorderLayout(8, 8));
		detail.setBorder(BorderFactory.createTitledBorder(AppLocal.getIntString("customer.title")));
		JPanel fields = new JPanel(new GridLayout(0, 2, 8, 8));
		fields.add(new JLabel(AppLocal.getIntString("customer.name")));
		fields.add(name);
		fields.add(new JLabel(AppLocal.getIntString("customer.phone")));
		fields.add(phone);
		fields.add(new JLabel(AppLocal.getIntString("customer.debt")));
		debt.setEditable(false);
		fields.add(debt);
		fields.add(new JLabel(AppLocal.getIntString("label.visible")));
		fields.add(visible);
		detail.add(fields, BorderLayout.NORTH);
		JPanel detailActions = new JPanel(new FlowLayout(FlowLayout.TRAILING));
		save.setText(AppLocal.getIntString("Button.Save"));
		detailActions.add(save);
		detail.add(detailActions, BorderLayout.SOUTH);

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(table), detail);
		split.setResizeWeight(0.65);
		add(split, BorderLayout.CENTER);

		search.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				loadCustomers();
			}

			public void removeUpdate(DocumentEvent e) {
				loadCustomers();
			}

			public void changedUpdate(DocumentEvent e) {
				loadCustomers();
			}
		});
		newCustomer.addActionListener(e -> startNewCustomer());
		save.addActionListener(e -> saveCustomer());
		setDetailEnabled(false);
	}

	public void init(AppView app) throws BeanFactoryException {
		customers = (DataLogicCustomers) app.getBean("com.openbravo.pos.customers.DataLogicCustomers");
	}

	public Object getBean() {
		return this;
	}

	public String getTitle() {
		return AppLocal.getIntString("Menu.CustomersManagement");
	}

	public void activate() throws BasicException {
		loadCustomers();
	}

	public boolean deactivate() {
		return true;
	}

	public JComponent getComponent() {
		return this;
	}

	private void loadCustomers() {
		if (customers == null) {
			return;
		}
		try {
			List<CustomerInfoExt> result = customers.searchCustomerSummaries(search.getText());
			model.setCustomers(result);
			if (!result.isEmpty()) {
				table.setRowSelectionInterval(0, 0);
			} else {
				clearDetail();
			}
		} catch (BasicException exception) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("customer.finderror"),
					AppLocal.getIntString("customer.title"), JOptionPane.WARNING_MESSAGE);
		}
	}

	private void selectCustomer() {
		int row = table.getSelectedRow();
		if (row < 0 || row >= model.customers.size()) {
			return;
		}
		selected = model.customers.get(row);
		creating = false;
		name.setText(selected.getName());
		phone.setText(selected.getPhone());
		debt.setText(selected.getCurdebt() == null ? "0.00" : selected.getCurdebt().toString());
		visible.setSelected(selected.isVisible());
		setDetailEnabled(true);
	}

	private void startNewCustomer() {
		selected = null;
		creating = true;
		name.setText(null);
		phone.setText(null);
		debt.setText("0.00");
		visible.setSelected(true);
		setDetailEnabled(true);
		name.requestFocusInWindow();
	}

	private void saveCustomer() {
		String customerName = name.getText().trim();
		String customerPhone = CustomerInfo.normalizePhone(phone.getText());
		if (customerName.isEmpty() || customerPhone.isEmpty()) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("customer.required"),
					AppLocal.getIntString("customer.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		try {
			if (creating) {
				selected = customers.createCustomer(customerName, customerPhone);
			} else {
				selected.setName(customerName);
				selected.setPhone(customerPhone);
				selected.setVisible(visible.isSelected());
				customers.updateCustomer(selected);
			}
			creating = false;
			loadCustomers();
		} catch (BasicException exception) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("customer.saveerror"),
					AppLocal.getIntString("customer.title"), JOptionPane.WARNING_MESSAGE);
		}
	}

	private void clearDetail() {
		selected = null;
		creating = false;
		name.setText(null);
		phone.setText(null);
		debt.setText(null);
		visible.setSelected(false);
		setDetailEnabled(false);
	}

	private void setDetailEnabled(boolean enabled) {
		name.setEnabled(enabled);
		phone.setEnabled(enabled);
		visible.setEnabled(enabled);
		save.setEnabled(enabled);
		debt.setEnabled(false);
	}

	private static class CustomersTableModel extends AbstractTableModel {
		private List<CustomerInfoExt> customers = Collections.emptyList();

		public void setCustomers(List<CustomerInfoExt> customers) {
			this.customers = customers;
			fireTableDataChanged();
		}

		public int getRowCount() {
			return customers.size();
		}

		public int getColumnCount() {
			return 3;
		}

		public String getColumnName(int column) {
			if (column == 0) {
				return AppLocal.getIntString("customer.name");
			}
			if (column == 1) {
				return AppLocal.getIntString("customer.phone");
			}
			return AppLocal.getIntString("customer.debt");
		}

		public Object getValueAt(int row, int column) {
			CustomerInfoExt customer = customers.get(row);
			if (column == 0) {
				return customer.getName();
			}
			if (column == 1) {
				return customer.getPhone();
			}
			return customer.getCurdebt() == null ? "0.00" : customer.getCurdebt();
		}
	}
}
