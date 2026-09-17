package com.openbravo.pos.customers;

import com.openbravo.basic.BasicException;
import com.openbravo.pos.forms.AppLocal;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridLayout;
import java.awt.Window;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

public class JCustomerFinder extends JDialog {

	private final DataLogicCustomers dlCustomers;
	private final JTextField searchField = new JTextField();
	private final DefaultListModel<CustomerInfo> customersModel = new DefaultListModel<CustomerInfo>();
	private final JList<CustomerInfo> customers = new JList<CustomerInfo>(customersModel);
	private final JButton removeButton = new JButton();
	private CustomerInfo selectedCustomer;
	private boolean removeRequested;

	private JCustomerFinder(Window parent, DataLogicCustomers dlCustomers) {
		super(parent, ModalityType.APPLICATION_MODAL);
		this.dlCustomers = dlCustomers;
		setTitle(AppLocal.getIntString("customer.title"));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout(8, 8));
		setResizable(false);

		JPanel search = new JPanel(new BorderLayout(5, 5));
		search.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		search.add(new JLabel(AppLocal.getIntString("customer.find")), BorderLayout.WEST);
		search.add(searchField, BorderLayout.CENTER);
		JButton searchButton = new JButton(AppLocal.getIntString("customer.search"));
		search.add(searchButton, BorderLayout.EAST);
		add(search, BorderLayout.NORTH);

		customers.setVisibleRowCount(7);
		add(new JScrollPane(customers), BorderLayout.CENTER);

		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton newButton = new JButton(AppLocal.getIntString("customer.new"));
		JButton cancelButton = new JButton(AppLocal.getIntString("Button.Cancel"));
		JButton selectButton = new JButton(AppLocal.getIntString("customer.select"));
		removeButton.setText(AppLocal.getIntString("customer.remove"));
		actions.add(newButton);
		removeButton.setVisible(false);
		actions.add(removeButton);
		actions.add(cancelButton);
		actions.add(selectButton);
		add(actions, BorderLayout.SOUTH);

		searchButton.addActionListener(e -> executeSearch());
		searchField.addActionListener(e -> executeSearch());
		selectButton.addActionListener(e -> selectCustomer());
		customers.addListSelectionListener(e -> selectButton.setEnabled(customers.getSelectedValue() != null));
		customers.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent event) {
				if (event.getClickCount() == 2) {
					selectCustomer();
				}
			}
		});
		newButton.addActionListener(e -> createCustomer());
		removeButton.addActionListener(e -> {
			removeRequested = true;
			selectedCustomer = null;
			dispose();
		});
		cancelButton.addActionListener(e -> dispose());
		selectButton.setEnabled(false);
		pack();
		setSize(620, 430);
		// Centre the modal finder on the sales window that opened it.
		setLocationRelativeTo(parent);
	}

	public static JCustomerFinder getCustomerFinder(Component parent, DataLogicCustomers dlCustomers) {
		Window window = getWindow(parent);
		return new JCustomerFinder(window, dlCustomers);
	}

	public CustomerInfo getSelectedCustomer() {
		return selectedCustomer;
	}

	public boolean isRemoveRequested() {
		return removeRequested;
	}

	public void search(CustomerInfo customer) {
		selectedCustomer = null;
		removeRequested = false;
		searchField.setText(null);
		if (customer != null) {
			searchField.setText(customer.getName());
		}
		if (customer != null) {
			removeButton.setVisible(true);
		}
		customersModel.clear();
		searchField.requestFocusInWindow();
	}

	private void executeSearch() {
		try {
			customersModel.clear();
			List<CustomerInfo> result = dlCustomers.searchCustomers(searchField.getText());
			for (CustomerInfo customer : result) {
				customersModel.addElement(customer);
			}
			if (!result.isEmpty()) {
				customers.setSelectedIndex(0);
			}
		} catch (BasicException exception) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("customer.finderror"),
					AppLocal.getIntString("customer.title"), JOptionPane.WARNING_MESSAGE);
		}
	}

	private void selectCustomer() {
		selectedCustomer = customers.getSelectedValue();
		if (selectedCustomer != null) {
			dispose();
		}
	}

	private void createCustomer() {
		JTextField name = new JTextField();
		JTextField phone = new JTextField();
		JPanel fields = new JPanel(new GridLayout(0, 2, 5, 5));
		fields.add(new JLabel(AppLocal.getIntString("customer.name")));
		fields.add(name);
		fields.add(new JLabel(AppLocal.getIntString("customer.phone")));
		fields.add(phone);
		int result = JOptionPane.showConfirmDialog(this, fields, AppLocal.getIntString("customer.new"),
				JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
		if (result != JOptionPane.OK_OPTION) {
			return;
		}
		String normalizedPhone = CustomerInfo.normalizePhone(phone.getText());
		if (name.getText().trim().isEmpty() || normalizedPhone.isEmpty()) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("customer.required"),
					AppLocal.getIntString("customer.title"), JOptionPane.WARNING_MESSAGE);
			return;
		}
		try {
			List<CustomerInfo> existing = dlCustomers.findCustomersByPhone(normalizedPhone);
			if (!existing.isEmpty()) {
				StringBuilder message = new StringBuilder("A customer already has this phone:\n");
				for (CustomerInfo c : existing)
					message.append(c.getName()).append("\n");
				Object[] options = {"Select existing", "Create anyway", "Cancel"};
				int choice = JOptionPane.showOptionDialog(this, message.toString(),
						AppLocal.getIntString("customer.title"), JOptionPane.DEFAULT_OPTION,
						JOptionPane.WARNING_MESSAGE, null, options, options[0]);
				if (choice == 0) {
					selectedCustomer = existing.get(0);
					dispose();
					return;
				}
				if (choice != 1)
					return;
			}
			CustomerInfoExt created = dlCustomers.createCustomer(name.getText(), normalizedPhone);
			selectedCustomer = created;
			dispose();
		} catch (BasicException exception) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("customer.createerror"),
					AppLocal.getIntString("customer.title"), JOptionPane.WARNING_MESSAGE);
		}
	}

	private static Window getWindow(Component parent) {
		if (parent == null) {
			return null;
		}
		if (parent instanceof Frame || parent instanceof Dialog) {
			return (Window) parent;
		}
		return getWindow(parent.getParent());
	}
}
