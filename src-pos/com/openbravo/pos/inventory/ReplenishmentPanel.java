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
import com.openbravo.pos.sales.TicketsEditor;
import com.openbravo.pos.sales.JPanelTicketSales;
import com.openbravo.pos.ticket.UserInfo;
import com.openbravo.pos.ticket.ProductInfoExt;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import javax.swing.JFileChooser;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.ButtonGroup;
import javax.swing.Box;
import javax.swing.JList;
import javax.swing.JComboBox;
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
import javax.swing.plaf.basic.BasicSplitPaneUI;

public class ReplenishmentPanel extends JPanel implements JPanelView, BeanFactoryApp {
	private AppView app;
	private DataLogicReplenishment data;
	private DataLogicSales sales;
	private DataLogicCustomers customers;
	private final DefaultTableModel model = new DefaultTableModel(
			new Object[]{AppLocal.getIntString("Replenishment.Type"), AppLocal.getIntString("Replenishment.Product"),
					AppLocal.getIntString("Replenishment.CustomerColumn"), AppLocal.getIntString("Replenishment.Note"),
					AppLocal.getIntString("Replenishment.Status"), AppLocal.getIntString("Replenishment.Added")},
			0) {
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
	private JLabel selectedProduct, selectedCustomer, formStatusLabel, withoutCodeLabel, manualDescriptionLabel,
			creatorLabel;
	private JScrollPane manualDescriptionScroll;
	private JToggleButton formPending, formOrdered, formReceived;
	private JToggleButton allStatus;
	private JTextField search;
	private CustomerInfo formCustomer;
	private ProductInfoExt formProduct;
	private ReplenishmentEntry editingEntry;
	private String formStatus = "PENDING";
	private String selectedStatus = "ALL";
	private String selectedType = "ALL";
	private final Map<String, JToggleButton> statusButtons = new HashMap<>();
	private JLabel queueFilterLabel;

	@Override
	public void init(AppView app) throws BeanFactoryException {
		this.app = app;
		data = app.getBean(DataLogicReplenishment.class);
		sales = app.getBean(DataLogicSales.class);
		customers = app.getBean(DataLogicCustomers.class);
		build();
	}
	private void build() {
		setLayout(new BorderLayout(6, 6));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		JPanel header = new JPanel(new BorderLayout(8, 4));
		queueFilterLabel = new JLabel(" ");
		queueFilterLabel.setForeground(java.awt.Color.GRAY);
		header.add(queueFilterLabel, BorderLayout.CENTER);
		JButton newRequest = new JButton("+ " + AppLocal.getIntString("Replenishment.NewRequest"));
		newRequest.setFont(newRequest.getFont().deriveFont(java.awt.Font.BOLD));
		newRequest.setMargin(new Insets(6, 12, 6, 12));
		JPanel filters = new JPanel(new BorderLayout(8, 4));
		JPanel statusToggles = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		ButtonGroup group = new ButtonGroup();
		String[] statuses = {"ALL", "PENDING", "ORDERED", "RECEIVED"};
		String[] labels = {"Replenishment.All", "Replenishment.Pending", "Replenishment.Ordered",
				"Replenishment.Received"};
		for (int i = 0; i < statuses.length; i++) {
			final String status = statuses[i];
			JToggleButton button = new JToggleButton(AppLocal.getIntString(labels[i]));
			button.setMargin(new Insets(6, 10, 6, 10));
			button.setSelected(i == 0);
			if (i == 0)
				allStatus = button;
			statusButtons.put(status, button);
			group.add(button);
			statusToggles.add(button);
			button.addActionListener(e -> {
				selectedStatus = status;
				selectedType = "ALL";
				queueFilterLabel.setText(" ");
				load("", false);
			});
		}
		search = new JTextField();
		installPlaceholder(search, "Replenishment.SearchPlaceholder");
		filters.add(statusToggles, BorderLayout.WEST);
		filters.add(search, BorderLayout.CENTER);
		filters.add(newRequest, BorderLayout.EAST);
		JPanel north = new JPanel(new BorderLayout());
		north.add(header, BorderLayout.NORTH);
		north.add(filters, BorderLayout.SOUTH);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && table.getSelectedRow() >= 0)
				loadEntry(entries.get(table.getSelectedRow()));
		});
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			@Override
			public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
					boolean focused, int row, int column) {
				java.awt.Component component = super.getTableCellRendererComponent(table, value, selected, focused, row,
						column);
				if (!selected)
					component.setBackground(row < table.getRowCount()
							&& AppLocal.getIntString("Replenishment.Orders").equals(table.getValueAt(row, 0))
									? new java.awt.Color(255, 244, 214)
									: new java.awt.Color(235, 245, 255));
				if (!selected && column == 4) {
					String state = String.valueOf(value);
					component.setBackground(state.equals(AppLocal.getIntString("Replenishment.Received"))
							? new java.awt.Color(230, 240, 230)
							: state.equals(AppLocal.getIntString("Replenishment.Ordered"))
									? new java.awt.Color(227, 237, 248)
									: new java.awt.Color(253, 241, 216));
					component.setForeground(state.equals(AppLocal.getIntString("Replenishment.Received"))
							? new java.awt.Color(44, 110, 44)
							: state.equals(AppLocal.getIntString("Replenishment.Ordered"))
									? new java.awt.Color(10, 92, 158)
									: new java.awt.Color(138, 90, 0));
				}
				return component;
			}
		});
		JPanel list = new JPanel(new BorderLayout());
		JScrollPane tableScroll = new JScrollPane(table);
		tableScroll.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
		list.add(tableScroll, BorderLayout.CENTER);
		JPanel actions = new JPanel(new BorderLayout());
		actions.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
		JPanel tableActions = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton ordered = new JButton(AppLocal.getIntString("Replenishment.MarkOrdered")),
				received = new JButton(AppLocal.getIntString("Replenishment.MarkReceived")),
				reopen = new JButton(AppLocal.getIntString("Replenishment.Reopen"));
		JButton delete = new JButton(AppLocal.getIntString("Replenishment.Delete"));
		JButton export = new JButton(AppLocal.getIntString("Replenishment.Export"));
		tableActions.add(ordered);
		tableActions.add(received);
		tableActions.add(reopen);
		tableActions.add(delete);
		actions.add(tableActions, BorderLayout.WEST);
		actions.add(export, BorderLayout.EAST);
		list.add(actions, BorderLayout.SOUTH);
		ordered.setMargin(new Insets(6, 10, 6, 10));
		received.setMargin(new Insets(6, 10, 6, 10));
		reopen.setMargin(new Insets(6, 10, 6, 10));
		delete.setMargin(new Insets(6, 10, 6, 10));
		javax.swing.table.TableColumnModel columns = table.getColumnModel();
		columns.getColumn(0).setPreferredWidth(85);
		columns.getColumn(0).setMaxWidth(105);
		columns.getColumn(2).setPreferredWidth(120);
		columns.getColumn(2).setMaxWidth(170);
		columns.getColumn(4).setPreferredWidth(90);
		columns.getColumn(4).setMaxWidth(110);
		columns.getColumn(5).setPreferredWidth(170);
		columns.getColumn(5).setMaxWidth(220);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		JPanel left = new JPanel(new BorderLayout());
		left.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
		left.add(north, BorderLayout.NORTH);
		left.add(list, BorderLayout.CENTER);
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, createForm());
		split.setResizeWeight(1.0);
		split.setDividerLocation(0.72);
		split.setDividerSize(2);
		split.setBorder(null);
		if (split.getUI() instanceof BasicSplitPaneUI) {
			BasicSplitPaneUI splitUI = (BasicSplitPaneUI) split.getUI();
			splitUI.getDivider().setBackground(new java.awt.Color(210, 210, 210));
		}
		add(split, BorderLayout.CENTER);
		search.addActionListener(
				e -> load(search.getText().equals(AppLocal.getIntString("Replenishment.SearchPlaceholder"))
						? ""
						: search.getText(), false));
		javax.swing.Timer filterTimer = new javax.swing.Timer(250,
				e -> load(search.getText().equals(AppLocal.getIntString("Replenishment.SearchPlaceholder"))
						? ""
						: search.getText(), false));
		filterTimer.setRepeats(false);
		search.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				filterTimer.restart();
			}
			public void removeUpdate(DocumentEvent e) {
				filterTimer.restart();
			}
			public void changedUpdate(DocumentEvent e) {
				filterTimer.restart();
			}
		});
		newRequest.addActionListener(e -> clearForm());
		ordered.addActionListener(e -> change("ORDERED"));
		received.addActionListener(e -> change("RECEIVED"));
		reopen.addActionListener(e -> change("PENDING"));
		delete.addActionListener(e -> deleteSelected());
		export.addActionListener(e -> exportVisibleEntries());
		javax.swing.SwingUtilities.invokeLater(() -> formCode.requestFocusInWindow());
	}
	private void deleteSelected() {
		int row = table.getSelectedRow();
		if (row < 0)
			return;
		ReplenishmentEntry entry = entries.get(row);
		int answer = JOptionPane.showConfirmDialog(this, AppLocal.getIntString("Replenishment.ConfirmDelete"),
				AppLocal.getIntString("Replenishment.Delete"), JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
		if (answer != JOptionPane.YES_OPTION)
			return;
		try {
			data.delete(entry.id);
			clearForm();
			load("", false);
		} catch (BasicException e) {
			JOptionPane.showMessageDialog(this, e.getMessage());
		}
	}
	private void exportVisibleEntries() {
		if (entries == null || entries.isEmpty()) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.NothingToExport"));
			return;
		}
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(new File(String.format(AppLocal.getIntString("Replenishment.ExportFileName"),
				new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()))));
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return;
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(chooser.getSelectedFile()))) {
			writer.write("producte\tnom client\ttelefon\tnota\tpersona");
			writer.newLine();
			for (ReplenishmentEntry entry : entries) {
				String phone = "";
				if (entry.customerId != null) {
					CustomerInfo customer = customers.getCustomer(entry.customerId);
					if (customer != null && customer.getPhone() != null)
						phone = customer.getPhone();
				}
				writer.write(exportValue(entry.displayName()));
				writer.write('\t');
				writer.write(exportValue(entry.customerName));
				writer.write('\t');
				writer.write(exportValue(phone));
				writer.write('\t');
				writer.write(exportValue(entry.note));
				writer.write('\t');
				writer.write(exportValue(creatorName(entry.createdBy)));
				writer.newLine();
			}
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.ExportComplete"));
		} catch (Exception ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage());
		}
	}
	private String exportValue(String value) {
		return value == null ? "" : value.replace('\t', ' ').replace('\r', ' ').replace('\n', ' ');
	}
	private UserInfo selectedSeller() {
		try {
			TicketsEditor editor = app.getBean(JPanelTicketSales.class);
			UserInfo seller = ((com.openbravo.pos.sales.JPanelTicket) editor).getSelectedSeller();
			if (seller != null)
				return seller;
		} catch (BeanFactoryException ignored) {
		}
		UserInfo fallback = app.getAppUserView().getUser().getTicketUserInfo();
		return fallback == null ? app.getAppUserView().getUser().getUserInfo() : fallback;
	}
	private String creatorName(String creatorId) {
		UserInfo seller = selectedSeller();
		return seller != null && seller.getId().equals(creatorId) ? seller.getName() : creatorId;
	}
	private void installPlaceholder(JTextField field, String key) {
		final String hint = AppLocal.getIntString(key);
		field.setText(hint);
		field.setForeground(java.awt.Color.GRAY);
		field.addFocusListener(new java.awt.event.FocusAdapter() {
			@Override
			public void focusGained(java.awt.event.FocusEvent e) {
				if (hint.equals(field.getText())) {
					field.setText("");
					field.setForeground(java.awt.Color.BLACK);
				}
			}
			@Override
			public void focusLost(java.awt.event.FocusEvent e) {
				if (field.getText().trim().isEmpty()) {
					field.setText(hint);
					field.setForeground(java.awt.Color.GRAY);
				}
			}
		});
	}
	private JPanel createForm() {
		JPanel form = new JPanel(new BorderLayout(5, 5));
		form.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
		form.setPreferredSize(new java.awt.Dimension(380, 0));
		form.setMinimumSize(new java.awt.Dimension(320, 0));
		JPanel fields = new JPanel(new GridBagLayout());
		GridBagConstraints outer = new GridBagConstraints();
		outer.gridx = 0;
		outer.weightx = 1;
		outer.fill = GridBagConstraints.HORIZONTAL;
		outer.anchor = GridBagConstraints.NORTHWEST;
		outer.insets = new Insets(8, 4, 8, 4);
		JPanel productGroup = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2, 4, 2, 4);
		formCode = new JTextField();
		formDescription = new JTextArea(3, 20);
		((JTextArea) formDescription).setRows(3);
		((JTextArea) formDescription).setLineWrap(true);
		((JTextArea) formDescription).setWrapStyleWord(true);
		c.gridy = 0;
		productGroup.add(new JLabel(AppLocal.getIntString("Replenishment.Scan")), c);
		c.gridy = 1;
		productGroup.add(formCode, c);
		JPanel productButtons = new JPanel(new FlowLayout(FlowLayout.LEFT));
		JButton findProduct = new JButton(AppLocal.getIntString("Replenishment.SearchProduct"));
		findProduct.setMargin(new Insets(6, 10, 6, 10));
		productButtons.add(findProduct);
		c.gridy = 2;
		productGroup.add(productButtons, c);
		selectedProduct = new JLabel(AppLocal.getIntString("Replenishment.NoProductSelected"));
		c.gridy = 3;
		productGroup.add(selectedProduct, c);
		withoutCodeLabel = new JLabel(AppLocal.getIntString("Replenishment.WithoutCode"), JLabel.CENTER);
		withoutCodeLabel.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
		c.gridy = 4;
		productGroup.add(withoutCodeLabel, c);
		manualDescriptionLabel = new JLabel(AppLocal.getIntString("Replenishment.Description"));
		c.gridy = 5;
		productGroup.add(manualDescriptionLabel, c);
		manualDescriptionScroll = new JScrollPane((JTextArea) formDescription);
		c.gridy = 6;
		productGroup.add(manualDescriptionScroll, c);
		JPanel customerGroup = new JPanel(new BorderLayout(6, 0));
		selectedCustomer = new JLabel(AppLocal.getIntString("Replenishment.NoCustomer"));
		JButton customer = new JButton(AppLocal.getIntString("Replenishment.Customer"));
		customer.setMargin(new Insets(6, 10, 6, 10));
		customerGroup.add(selectedCustomer, BorderLayout.CENTER);
		customerGroup.add(customer, BorderLayout.EAST);
		formNoteArea = new JTextArea(4, 20);
		formNoteArea.setLineWrap(true);
		formNoteArea.setWrapStyleWord(true);
		JScrollPane noteScroll = new JScrollPane(formNoteArea);
		noteScroll.setPreferredSize(new java.awt.Dimension(260, 100));
		JPanel noteGroup = new JPanel(new BorderLayout());
		noteGroup.add(noteScroll, BorderLayout.CENTER);
		formStatusLabel = new JLabel(AppLocal.getIntString("Replenishment.Status"));
		JPanel statusButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		ButtonGroup statusGroup = new ButtonGroup();
		formPending = new JToggleButton(AppLocal.getIntString("Replenishment.FormPending"));
		formOrdered = new JToggleButton(AppLocal.getIntString("Replenishment.FormOrdered"));
		formReceived = new JToggleButton(AppLocal.getIntString("Replenishment.FormReceived"));
		formPending.setMargin(new Insets(6, 10, 6, 10));
		formOrdered.setMargin(new Insets(6, 10, 6, 10));
		formReceived.setMargin(new Insets(6, 10, 6, 10));
		formPending.setSelected(true);
		statusGroup.add(formPending);
		statusGroup.add(formOrdered);
		statusGroup.add(formReceived);
		statusButtons.add(formPending);
		statusButtons.add(formOrdered);
		statusButtons.add(formReceived);
		formPending.addActionListener(e -> formStatus = "PENDING");
		formOrdered.addActionListener(e -> formStatus = "ORDERED");
		formReceived.addActionListener(e -> formStatus = "RECEIVED");
		JPanel customerSection = new JPanel(new BorderLayout(0, 2));
		customerSection.add(new JLabel(AppLocal.getIntString("Replenishment.CustomerColumn")), BorderLayout.NORTH);
		customerSection.add(customerGroup, BorderLayout.CENTER);
		JPanel noteSection = new JPanel(new BorderLayout(0, 2));
		noteSection.add(new JLabel(AppLocal.getIntString("Replenishment.Note")), BorderLayout.NORTH);
		noteSection.add(noteGroup, BorderLayout.CENTER);
		JPanel statusSection = new JPanel(new BorderLayout(0, 2));
		statusSection.add(formStatusLabel, BorderLayout.NORTH);
		statusSection.add(statusButtons, BorderLayout.CENTER);
		creatorLabel = new JLabel(AppLocal.getIntString("Replenishment.CreatedBy") + ": " + selectedSeller().getName());
		outer.gridy = 0;
		fields.add(new JLabel(AppLocal.getIntString("Replenishment.Product")), outer);
		outer.gridy = 1;
		fields.add(productGroup, outer);
		outer.gridy = 2;
		fields.add(customerSection, outer);
		outer.gridy = 3;
		outer.weighty = 1;
		outer.fill = GridBagConstraints.BOTH;
		fields.add(noteSection, outer);
		outer.gridy = 4;
		outer.weighty = 0;
		outer.fill = GridBagConstraints.HORIZONTAL;
		fields.add(statusSection, outer);
		outer.gridy = 5;
		fields.add(creatorLabel, outer);
		JButton save = new JButton(AppLocal.getIntString("Replenishment.Save"));
		save.setFont(save.getFont().deriveFont(java.awt.Font.BOLD, 16f));
		save.setMargin(new Insets(6, 12, 6, 12));
		JButton cancel = new JButton(AppLocal.getIntString("Button.Cancel"));
		cancel.setMargin(new Insets(6, 12, 6, 12));
		JPanel formActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		formActions.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
		formActions.add(cancel);
		formActions.add(save);
		form.add(fields, BorderLayout.CENTER);
		form.add(formActions, BorderLayout.SOUTH);
		formCode.addActionListener(e -> findFormProduct());
		findProduct.addActionListener(e -> {
			formProduct = JProductFinder.showMessage(this, sales);
			showSelectedProduct();
		});
		customer.addActionListener(e -> chooseFormCustomer());
		save.addActionListener(e -> saveForm());
		cancel.addActionListener(e -> clearForm());
		return form;
	}
	private void findFormProduct() {
		try {
			formProduct = sales.getProductInfoByCode(formCode.getText().trim());
			if (formProduct == null)
				formProduct = JProductFinder.showMessage(this, sales);
			showSelectedProduct();
		} catch (BasicException e) {
			JOptionPane.showMessageDialog(this, e.getMessage());
		}
	}
	private void showSelectedProduct() {
		boolean manual = formProduct == null;
		if (manual) {
			selectedProduct.setText(AppLocal.getIntString("Replenishment.UnknownProduct"));
			formDescription.setEditable(true);
		} else {
			selectedProduct.setText("<html><b>" + formProduct.getName() + "</b><br><font color='#777777'>"
					+ formProduct.getReference() + " · EAN " + formProduct.getCode() + "</font></html>");
			formDescription.setText(formProduct.getName());
			formDescription.setEditable(false);
		}
		withoutCodeLabel.setVisible(manual);
		manualDescriptionLabel.setVisible(manual);
		manualDescriptionScroll.setVisible(manual);
		manualDescriptionScroll.getParent().revalidate();
		manualDescriptionScroll.getParent().repaint();
	}
	private void chooseFormCustomer() {
		JCustomerFinder finder = JCustomerFinder.getCustomerFinder(this, customers);
		finder.search(null);
		finder.setVisible(true);
		formCustomer = finder.getSelectedCustomer();
		selectedCustomer.setText(formCustomer == null
				? AppLocal.getIntString("Replenishment.NoCustomer")
				: "<html><b>" + formCustomer.getName() + "</b></html>");
	}
	private void saveForm() {
		if (formProduct == null && formCode.getText().trim().isEmpty() && formDescription.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.SelectProduct"));
			return;
		}
		ReplenishmentEntry e = editingEntry == null ? new ReplenishmentEntry() : editingEntry;
		if (formProduct != null) {
			e.productId = formProduct.getID();
			e.reference = formProduct.getReference();
			e.name = formProduct.getName();
			e.ean = formProduct.getCode();
			e.manualDescription = null;
			e.manualEan = null;
		} else {
			e.productId = null;
			e.manualEan = formCode.getText().trim();
			e.manualDescription = formDescription.getText().trim();
		}
		e.note = formNoteArea.getText().trim();
		if (formCustomer != null) {
			e.customerId = formCustomer.getId();
			e.customerName = formCustomer.getName();
		} else {
			e.customerId = null;
			e.customerName = null;
		}
		e.status = formStatus;
		if (editingEntry == null)
			e.createdBy = selectedSeller().getId();
		try {
			if (editingEntry == null)
				data.add(e);
			else
				data.update(e, selectedSeller().getId());
			clearForm();
			load("", false);
		} catch (BasicException ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage());
		}
	}
	private void loadEntry(ReplenishmentEntry entry) {
		editingEntry = entry;
		formStatus = entry.status;
		creatorLabel.setText(AppLocal.getIntString("Replenishment.CreatedBy") + ": " + entry.createdBy);
		formCode.setText(entry.ean == null ? "" : entry.ean);
		formProduct = entry.productId == null ? null : new ProductInfoExt();
		if (formProduct != null) {
			formProduct.setID(entry.productId);
			formProduct.setReference(entry.reference);
			formProduct.setName(entry.name);
			formProduct.setCode(entry.ean);
		}
		formDescription.setText(entry.productId == null
				? (entry.manualDescription == null ? "" : entry.manualDescription)
				: entry.name);
		formDescription.setEditable(entry.productId == null);
		formNoteArea.setText(entry.note == null ? "" : entry.note);
		formCustomer = entry.customerId == null ? null : new CustomerInfo(entry.customerId);
		if (formCustomer != null)
			formCustomer.setName(entry.customerName);
		selectedProduct.setText(entry.productId == null
				? AppLocal.getIntString("Replenishment.UnknownProduct")
				: "<html><b>" + entry.name + "</b><br><font color='#777777'>" + entry.reference + " · EAN " + entry.ean
						+ "</font></html>");
		selectedCustomer.setText(entry.customerName == null
				? AppLocal.getIntString("Replenishment.NoCustomer")
				: "<html><b>" + entry.customerName + "</b></html>");
		showSelectedProduct();
	}
	private void clearForm() {
		editingEntry = null;
		formStatus = "PENDING";
		creatorLabel.setText(AppLocal.getIntString("Replenishment.CreatedBy") + ": " + selectedSeller().getName());
		formCode.setText("");
		formDescription.setText("");
		formDescription.setEditable(true);
		formNoteArea.setText("");
		formCustomer = null;
		formProduct = null;
		showSelectedProduct();
		selectedProduct.setText(AppLocal.getIntString("Replenishment.NoProductSelected"));
		selectedCustomer.setText(AppLocal.getIntString("Replenishment.NoCustomer"));
		table.clearSelection();
		formCode.requestFocusInWindow();
	}
	private void load(String search, boolean received) {
		try {
			entries = data.list(search, selectedType, selectedStatus);
			model.setRowCount(0);
			DateFormat f = new SimpleDateFormat("dd/MM/yy - HH:mm");
			for (ReplenishmentEntry e : entries)
				model.addRow(new Object[]{
						e.customerId == null
								? AppLocal.getIntString("Replenishment.Replenishment")
								: AppLocal.getIntString("Replenishment.Orders"),
						e.displayName(), e.customerName == null ? "--" : e.customerName, e.note == null ? "--" : e.note,
						statusLabel(e.status), f.format(e.createdAt) + " / " + e.createdBy});
		} catch (BasicException e) {
			JOptionPane.showMessageDialog(this, e.getMessage());
		}
	}
	private String statusLabel(String status) {
		return "RECEIVED".equals(status)
				? AppLocal.getIntString("Replenishment.Received")
				: "ORDERED".equals(status)
						? AppLocal.getIntString("Replenishment.Ordered")
						: AppLocal.getIntString("Replenishment.Pending");
	}
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
	@Override
	public String getTitle() {
		return AppLocal.getIntString("Replenishment.ScreenTitle");
	}
	@Override
	public void activate() throws BasicException {
		search.setText("");
		selectedStatus = "ALL";
		selectedType = "ALL";
		queueFilterLabel.setText(" ");
		allStatus.setSelected(true);
		if (!hasDraftContent())
			creatorLabel.setText(AppLocal.getIntString("Replenishment.CreatedBy") + ": " + selectedSeller().getName());
		load("", false);
	}

	/** Open the list for one of the landing screen's waiting-work figures. */
	public void showQueue(String filter) {
		selectedType = "ENCARGO".equals(filter) ? "ENCARGO" : "ALL";
		selectedStatus = "ENCARGO".equals(filter) ? "OPEN" : filter;
		statusButtons.get("ENCARGO".equals(filter) ? "ALL" : filter).setSelected(true);
		queueFilterLabel.setText("ENCARGO".equals(filter) ? AppLocal.getIntString("stock.welcome.customers") : " ");
		load("", false);
	}

	/** Start a manual request from a code with no matching product. */
	public void startManualEntry(String code) {
		clearForm();
		formCode.setText(code);
		formDescription.requestFocusInWindow();
	}

	/** Locate an already-open replenishment entry without adding a duplicate. */
	public void showProductOnList(String code) {
		search.setText(code);
		load(code, false);
	}
	private boolean hasDraftContent() {
		return formProduct != null || !formCode.getText().trim().isEmpty()
				|| !formDescription.getText().trim().isEmpty() || !formNoteArea.getText().trim().isEmpty()
				|| formCustomer != null;
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

}
