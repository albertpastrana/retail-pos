package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.pos.customers.CustomerInfo;
import com.openbravo.pos.customers.DataLogicCustomers;
import com.openbravo.pos.customers.JCustomerFinder;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.forms.JPanelView;
import com.openbravo.pos.panels.JProductFinder;
import com.openbravo.pos.ticket.ProductInfoExt;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.ButtonGroup;
import javax.swing.Box;
import javax.swing.JList;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class ReplenishmentPanel extends JPanel implements JPanelView, BeanFactoryApp {
	private AppView app;
	private DataLogicReplenishment data;
	private DataLogicSales sales;
	private DataLogicCustomers customers;
	private final DefaultTableModel model = new DefaultTableModel(
			new Object[]{AppLocal.getIntString("Replenishment.Type"), AppLocal.getIntString("Replenishment.Product"), AppLocal.getIntString("Replenishment.CustomerColumn"), AppLocal.getIntString("Replenishment.Note"), AppLocal.getIntString("Replenishment.Status"), AppLocal.getIntString("Replenishment.Added")}, 0) {
		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
	};
	private final JTable table = new JTable(model);
	private List<ReplenishmentEntry> entries;
	private JTextField formCode, formNote;
	private JTextComponent formDescription;
	private JTextArea formNoteArea;
	private JLabel selectedProduct, selectedCustomer;
	private CustomerInfo formCustomer;
	private ProductInfoExt formProduct;
	private String selectedStatus = "ALL";

	@Override
	public void init(AppView app) throws BeanFactoryException {
		this.app = app;
		data = (DataLogicReplenishment) app.getBean("com.openbravo.pos.inventory.DataLogicReplenishment");
		sales = (DataLogicSales) app.getBean("com.openbravo.pos.forms.DataLogicSales");
		customers = (DataLogicCustomers) app.getBean("com.openbravo.pos.customers.DataLogicCustomers");
		build();
	}
	private void build() {
		setLayout(new BorderLayout(6, 6));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		JPanel header = new JPanel(new BorderLayout(8, 4));
		JLabel summary = new JLabel(" "); summary.setForeground(java.awt.Color.GRAY); header.add(summary, BorderLayout.CENTER);
		JButton newRequest = new JButton("+ " + AppLocal.getIntString("Replenishment.NewRequest")); newRequest.setFont(newRequest.getFont().deriveFont(java.awt.Font.BOLD));
		JPanel filters = new JPanel(new BorderLayout(8, 4)); JPanel statusToggles = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0)); ButtonGroup group = new ButtonGroup();
		String[] statuses = {"ALL", "PENDING", "ORDERED", "RECEIVED"}; String[] labels = {"Replenishment.All", "Replenishment.Pending", "Replenishment.Ordered", "Replenishment.Received"};
		for (int i = 0; i < statuses.length; i++) { final String status = statuses[i]; JToggleButton button = new JToggleButton(AppLocal.getIntString(labels[i])); button.setSelected(i == 0); group.add(button); statusToggles.add(button); button.addActionListener(e -> { selectedStatus = status; load("", false); }); }
		JTextField search = new JTextField(); installPlaceholder(search, "Replenishment.SearchPlaceholder"); filters.add(statusToggles, BorderLayout.WEST); filters.add(search, BorderLayout.CENTER); filters.add(newRequest, BorderLayout.EAST);
		JPanel north = new JPanel(new BorderLayout()); north.add(header, BorderLayout.NORTH); north.add(filters, BorderLayout.SOUTH);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			@Override public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focused, int row, int column) {
				java.awt.Component component = super.getTableCellRendererComponent(table, value, selected, focused, row, column);
				if (!selected) component.setBackground(row < table.getRowCount() && AppLocal.getIntString("Replenishment.Orders").equals(table.getValueAt(row, 0)) ? new java.awt.Color(255, 244, 214) : new java.awt.Color(235, 245, 255));
				if (!selected && column == 4) { String state = String.valueOf(value); component.setBackground(state.equals(AppLocal.getIntString("Replenishment.Received")) ? new java.awt.Color(230, 240, 230) : state.equals(AppLocal.getIntString("Replenishment.Ordered")) ? new java.awt.Color(227, 237, 248) : new java.awt.Color(253, 241, 216)); component.setForeground(state.equals(AppLocal.getIntString("Replenishment.Received")) ? new java.awt.Color(44, 110, 44) : state.equals(AppLocal.getIntString("Replenishment.Ordered")) ? new java.awt.Color(10, 92, 158) : new java.awt.Color(138, 90, 0)); }
				return component;
			}
		});
		JPanel list = new JPanel(new BorderLayout());
		list.add(new JScrollPane(table), BorderLayout.CENTER);
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		JButton ordered = new JButton(AppLocal.getIntString("Replenishment.MarkOrdered")), received = new JButton(AppLocal.getIntString("Replenishment.MarkReceived")),
				reopen = new JButton(AppLocal.getIntString("Replenishment.Reopen"));
		actions.add(ordered);
		actions.add(received);
		actions.add(reopen);
		list.add(actions, BorderLayout.SOUTH);
		javax.swing.table.TableColumnModel columns = table.getColumnModel(); columns.getColumn(0).setPreferredWidth(85); columns.getColumn(0).setMaxWidth(105); columns.getColumn(2).setPreferredWidth(120); columns.getColumn(2).setMaxWidth(170); columns.getColumn(4).setPreferredWidth(90); columns.getColumn(4).setMaxWidth(110); columns.getColumn(5).setPreferredWidth(170); columns.getColumn(5).setMaxWidth(220); table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		JPanel content = new JPanel(new GridBagLayout()); GridBagConstraints layout = new GridBagConstraints(); layout.insets = new Insets(0, 0, 6, 8); layout.gridx = 0; layout.gridy = 0; layout.weightx = 1; layout.fill = GridBagConstraints.HORIZONTAL; content.add(north, layout); layout.gridy = 1; layout.weighty = 1; layout.fill = GridBagConstraints.BOTH; content.add(list, layout); layout.gridx = 1; layout.gridy = 0; layout.gridheight = 2; layout.weightx = 0; layout.weighty = 1; layout.fill = GridBagConstraints.BOTH; layout.insets = new Insets(0, 0, 0, 0); content.add(createForm(), layout); add(content, BorderLayout.CENTER);
		search.addActionListener(e -> load(search.getText().equals(AppLocal.getIntString("Replenishment.SearchPlaceholder")) ? "" : search.getText(), false));
		javax.swing.Timer filterTimer = new javax.swing.Timer(250, e -> load(search.getText().equals(AppLocal.getIntString("Replenishment.SearchPlaceholder")) ? "" : search.getText(), false)); filterTimer.setRepeats(false);
		search.getDocument().addDocumentListener(new DocumentListener() { public void insertUpdate(DocumentEvent e) { filterTimer.restart(); } public void removeUpdate(DocumentEvent e) { filterTimer.restart(); } public void changedUpdate(DocumentEvent e) { filterTimer.restart(); } });
		newRequest.addActionListener(e -> clearForm());
		ordered.addActionListener(e -> change("ORDERED"));
		received.addActionListener(e -> change("RECEIVED"));
		reopen.addActionListener(e -> change("PENDING"));
	}
	private void installPlaceholder(JTextField field, String key) { final String hint = AppLocal.getIntString(key); field.setText(hint); field.setForeground(java.awt.Color.GRAY); field.addFocusListener(new java.awt.event.FocusAdapter() { @Override public void focusGained(java.awt.event.FocusEvent e) { if (hint.equals(field.getText())) { field.setText(""); field.setForeground(java.awt.Color.BLACK); } } @Override public void focusLost(java.awt.event.FocusEvent e) { if (field.getText().trim().isEmpty()) { field.setText(hint); field.setForeground(java.awt.Color.GRAY); } } }); }
	private JPanel createForm() {
		JPanel form = new JPanel(new BorderLayout(5, 5));
		form.setPreferredSize(new java.awt.Dimension(380, 0));
		form.setMinimumSize(new java.awt.Dimension(320, 0));
		JPanel fields = new JPanel(new GridBagLayout());
		GridBagConstraints outer = new GridBagConstraints(); outer.gridx = 0; outer.weightx = 1; outer.fill = GridBagConstraints.HORIZONTAL; outer.anchor = GridBagConstraints.NORTHWEST; outer.insets = new Insets(8, 4, 8, 4);
		JPanel productGroup = new JPanel(new GridBagLayout()); productGroup.setBorder(BorderFactory.createTitledBorder(AppLocal.getIntString("Replenishment.Product")));
		GridBagConstraints c = new GridBagConstraints(); c.gridx = 0; c.weightx = 1; c.fill = GridBagConstraints.HORIZONTAL; c.insets = new Insets(2, 4, 2, 4);
		formCode = new JTextField(); formDescription = new JTextArea(3, 20); ((JTextArea) formDescription).setLineWrap(true); ((JTextArea) formDescription).setWrapStyleWord(true);
		c.gridy = 0; productGroup.add(new JLabel(AppLocal.getIntString("Replenishment.Scan")), c); c.gridy = 1; productGroup.add(formCode, c);
		JPanel productButtons = new JPanel(new FlowLayout(FlowLayout.LEFT)); JButton findProduct = new JButton(AppLocal.getIntString("Replenishment.SearchProduct")); JButton variants = new JButton(AppLocal.getIntString("Replenishment.Variant")); productButtons.add(findProduct); productButtons.add(variants); c.gridy = 2; productGroup.add(productButtons, c);
		selectedProduct = new JLabel(AppLocal.getIntString("Replenishment.NoProductSelected")); c.gridy = 3; productGroup.add(selectedProduct, c);
		JLabel withoutCode = new JLabel(AppLocal.getIntString("Replenishment.WithoutCode"), JLabel.CENTER); withoutCode.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0)); c.gridy = 4; productGroup.add(withoutCode, c); c.gridy = 5; productGroup.add(new JLabel(AppLocal.getIntString("Replenishment.Description")), c); c.gridy = 6; productGroup.add(new JScrollPane((JTextArea) formDescription), c);
		JPanel customerGroup = new JPanel(new BorderLayout(6, 0)); customerGroup.setBorder(BorderFactory.createTitledBorder(AppLocal.getIntString("Replenishment.CustomerColumn"))); selectedCustomer = new JLabel(AppLocal.getIntString("Replenishment.NoCustomer")); JButton customer = new JButton(AppLocal.getIntString("Replenishment.Customer")); customerGroup.add(selectedCustomer, BorderLayout.CENTER); customerGroup.add(customer, BorderLayout.EAST);
		formNoteArea = new JTextArea(4, 20); formNoteArea.setLineWrap(true); formNoteArea.setWrapStyleWord(true); JScrollPane noteScroll = new JScrollPane(formNoteArea); noteScroll.setPreferredSize(new java.awt.Dimension(260, 100)); JPanel noteGroup = new JPanel(new BorderLayout()); noteGroup.setBorder(BorderFactory.createTitledBorder(AppLocal.getIntString("Replenishment.Note"))); noteGroup.add(noteScroll, BorderLayout.CENTER);
		outer.gridy = 0; fields.add(productGroup, outer); outer.gridy = 1; fields.add(customerGroup, outer); outer.gridy = 2; outer.weighty = 1; outer.fill = GridBagConstraints.BOTH; fields.add(noteGroup, outer);
		JButton save = new JButton(AppLocal.getIntString("Replenishment.Add")); save.setFont(save.getFont().deriveFont(java.awt.Font.BOLD, 16f));
		form.add(fields, BorderLayout.CENTER); form.add(save, BorderLayout.SOUTH);
		formCode.addActionListener(e -> findFormProduct()); findProduct.addActionListener(e -> { formProduct = JProductFinder.showMessage(this, sales); showSelectedProduct(); }); variants.addActionListener(e -> chooseFormVariant()); customer.addActionListener(e -> chooseFormCustomer()); save.addActionListener(e -> saveForm());
		return form;
	}
	private void findFormProduct() { try { formProduct = sales.getProductInfoByCode(formCode.getText().trim()); if (formProduct == null) formProduct = JProductFinder.showMessage(this, sales); showSelectedProduct(); } catch (BasicException e) { JOptionPane.showMessageDialog(this, e.getMessage()); } }
	private void showSelectedProduct() { if (formProduct == null) { selectedProduct.setText(AppLocal.getIntString("Replenishment.UnknownProduct")); formDescription.setEditable(true); } else { selectedProduct.setText("<html><b>" + formProduct.getName() + "</b><br><font color='#777777'>" + formProduct.getReference() + " · EAN " + formProduct.getCode() + "</font></html>"); formDescription.setText(formProduct.getName()); formDescription.setEditable(false); } }
	private void chooseFormVariant() { if (formProduct == null) findFormProduct(); if (formProduct == null) return; try { List<ProductInfoExt> variants = sales.getProductVariants(formProduct.getReference()); javax.swing.JList<ProductInfoExt> list = new javax.swing.JList<ProductInfoExt>(variants.toArray(new ProductInfoExt[variants.size()])); if (JOptionPane.showConfirmDialog(this, new JScrollPane(list), AppLocal.getIntString("Replenishment.Variant"), JOptionPane.OK_CANCEL_OPTION) == JOptionPane.OK_OPTION && list.getSelectedValue() != null) { formProduct = list.getSelectedValue(); showSelectedProduct(); } } catch (BasicException e) { JOptionPane.showMessageDialog(this, e.getMessage()); } }
	private void chooseFormCustomer() { JCustomerFinder finder = JCustomerFinder.getCustomerFinder(this, customers); finder.search(null); finder.setVisible(true); formCustomer = finder.getSelectedCustomer(); selectedCustomer.setText(formCustomer == null ? AppLocal.getIntString("Replenishment.NoCustomer") : "<html><b>" + formCustomer.getName() + "</b></html>"); }
	private void saveForm() { if (formProduct == null && formCode.getText().trim().isEmpty() && formDescription.getText().trim().isEmpty()) { JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.SelectProduct")); return; } ReplenishmentEntry e = new ReplenishmentEntry(); if (formProduct != null) { e.productId = formProduct.getID(); e.reference = formProduct.getReference(); e.name = formProduct.getName(); e.ean = formProduct.getCode(); } else { e.manualEan = formCode.getText().trim(); e.manualDescription = formDescription.getText().trim(); } e.note = formNoteArea.getText().trim(); if (formCustomer != null) { e.customerId = formCustomer.getId(); e.customerName = formCustomer.getName(); } e.createdBy = app.getAppUserView().getUser().getId(); try { data.add(e); clearForm(); load("", false); } catch (BasicException ex) { JOptionPane.showMessageDialog(this, ex.getMessage()); } }
	private void clearForm() { formCode.setText(""); formDescription.setText(""); formDescription.setEditable(true); formNoteArea.setText(""); formCustomer = null; formProduct = null; selectedProduct.setText(AppLocal.getIntString("Replenishment.NoProductSelected")); selectedCustomer.setText(AppLocal.getIntString("Replenishment.NoCustomer")); formCode.requestFocusInWindow(); }
	private void load(String search, boolean received) {
		try {
			entries = data.list(search, "ALL", selectedStatus);
			model.setRowCount(0);
			DateFormat f = new SimpleDateFormat("dd/MM/yy - HH:mm");
			for (ReplenishmentEntry e : entries)
				model.addRow(new Object[]{e.customerId == null ? AppLocal.getIntString("Replenishment.Replenishment") : AppLocal.getIntString("Replenishment.Orders"), e.displayName(), e.customerName == null ? "--" : e.customerName,
						 e.note == null ? "--" : e.note, statusLabel(e.status), f.format(e.createdAt) + " / " + e.createdBy});
		} catch (BasicException e) {
			JOptionPane.showMessageDialog(this, e.getMessage());
		}
	}
	private String statusLabel(String status) { return "RECEIVED".equals(status) ? AppLocal.getIntString("Replenishment.Received") : "ORDERED".equals(status) ? AppLocal.getIntString("Replenishment.Ordered") : AppLocal.getIntString("Replenishment.Pending"); }
	private void change(String status) {
		int row = table.getSelectedRow();
		if (row < 0)
			return;
		try {
			data.updateStatus(entries.get(row).id, status, app.getAppUserView().getUser().getId());
			load("", false);
		} catch (BasicException e) {
			JOptionPane.showMessageDialog(this, e.getMessage());
		}
	}
	private void addEntry() {
		ReplenishmentDialog dialog = new ReplenishmentDialog(this);
		dialog.setVisible(true);
		if (dialog.entry != null) {
			try {
				data.add(dialog.entry);
				load("", false);
			} catch (BasicException e) {
				JOptionPane.showMessageDialog(this, e.getMessage());
			}
		}
	}
	@Override
	public String getTitle() {
		return AppLocal.getIntString("Replenishment.ScreenTitle");
	}
	@Override
	public void activate() throws BasicException {
		load("", false);
	}
	@Override
	public boolean deactivate() {
		return true;
	}
	@Override
	public javax.swing.JComponent getComponent() {
		return this;
	}
	@Override
	public Object getBean() {
		return this;
	}

	public static void showAddDialog(Component parent, AppView app) {
		try {
			ReplenishmentPanel p = new ReplenishmentPanel();
			p.init(app);
			p.addEntry();
		} catch (BeanFactoryException e) {
			JOptionPane.showMessageDialog(parent, e.getMessage());
		}
	}
	private class ReplenishmentDialog extends JDialog {
		private ReplenishmentEntry entry;
		private ProductInfoExt product;
		private final JTextField code = new JTextField();
		private final JTextField description = new JTextField();
		private final JTextField note = new JTextField();
		private CustomerInfo customer;
		ReplenishmentDialog(java.awt.Component parent) {
			super(javax.swing.SwingUtilities.getWindowAncestor(parent), AppLocal.getIntString("Menu.Replenishment.Add"),
					ModalityType.APPLICATION_MODAL);
			setLayout(new BorderLayout(6, 6));
			JPanel fields = new JPanel(new GridLayout(0, 2, 5, 5));
			fields.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
			fields.add(new JLabel(AppLocal.getIntString("Replenishment.Scan")));
			fields.add(code);
			fields.add(new JLabel(AppLocal.getIntString("Replenishment.Product")));
			JPanel productButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
			JButton finder = new JButton(AppLocal.getIntString("Replenishment.SearchProduct")), variants = new JButton(AppLocal.getIntString("Replenishment.Variant"));
			productButtons.add(finder);
			productButtons.add(variants);
			fields.add(productButtons);
			fields.add(new JLabel(AppLocal.getIntString("Replenishment.Description")));
			fields.add(description);
			fields.add(new JLabel(AppLocal.getIntString("Replenishment.Note")));
			fields.add(note);
			JButton customerButton = new JButton(AppLocal.getIntString("Replenishment.Customer"));
			fields.add(new JLabel(AppLocal.getIntString("Replenishment.CustomerColumn")));
			fields.add(customerButton);
			add(fields, BorderLayout.CENTER);
			JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
			JButton cancel = new JButton(AppLocal.getIntString("Button.Cancel")), save = new JButton(AppLocal.getIntString("Replenishment.Add"));
			buttons.add(cancel);
			buttons.add(save);
			add(buttons, BorderLayout.SOUTH);
			finder.addActionListener(e -> product = JProductFinder.showMessage(this, sales));
			variants.addActionListener(e -> chooseVariant());
			code.addActionListener(e -> findProduct());
			customerButton.addActionListener(e -> chooseCustomer());
			cancel.addActionListener(e -> dispose());
			save.addActionListener(e -> save());
			pack();
			setSize(560, 300);
		}
		private void findProduct() {
			try {
				product = sales.getProductInfoByCode(code.getText().trim());
				if (product == null)
					product = JProductFinder.showMessage(this, sales);
			} catch (BasicException e) {
				JOptionPane.showMessageDialog(this, e.getMessage());
			}
		}
		private void chooseCustomer() {
			JCustomerFinder f = JCustomerFinder.getCustomerFinder(this, customers);
			f.search(null);
			f.setVisible(true);
			customer = f.getSelectedCustomer();
		}
		private void chooseVariant() {
			if (product == null) {
				findProduct();
			}
			if (product == null)
				return;
			try {
				List<ProductInfoExt> variants = sales.getProductVariants(product.getReference());
				JList<ProductInfoExt> list = new JList<ProductInfoExt>(
						variants.toArray(new ProductInfoExt[variants.size()]));
				int result = JOptionPane.showConfirmDialog(this, new JScrollPane(list), AppLocal.getIntString("Replenishment.Variant"),
						JOptionPane.OK_CANCEL_OPTION);
				if (result == JOptionPane.OK_OPTION && list.getSelectedValue() != null)
					product = list.getSelectedValue();
			} catch (BasicException e) {
				JOptionPane.showMessageDialog(this, e.getMessage());
			}
		}
		private void save() {
			if (product == null && code.getText().trim().isEmpty() && description.getText().trim().isEmpty()) {
				JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.SelectProduct"));
				return;
			}
			entry = new ReplenishmentEntry();
			if (product != null) {
				entry.productId = product.getID();
				entry.reference = product.getReference();
				entry.name = product.getName();
				entry.ean = product.getCode();
			} else {
				entry.manualEan = code.getText().trim();
				entry.manualDescription = description.getText().trim();
			}
			entry.note = note.getText().trim();
			if (customer != null) {
				entry.customerId = customer.getId();
				entry.customerName = customer.getName();
			}
			entry.createdBy = app.getAppUserView().getUser().getId();
			dispose();
		}
	}
}
