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
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;
import com.openbravo.pos.util.PosIcons;
import java.awt.BorderLayout;
import java.awt.CardLayout;
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
import java.sql.SQLException;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFileChooser;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.ButtonGroup;
import javax.swing.Box;
import javax.swing.JList;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import com.openbravo.data.gui.JConfirmationDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.text.JTextComponent;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.KeyStroke;
import javax.swing.AbstractAction;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.plaf.basic.BasicSplitPaneUI;

public class ReplenishmentPanel extends JPanel implements JPanelView, BeanFactoryApp {
	private static final Logger logger = Logger.getLogger(ReplenishmentPanel.class.getName());
	private static final Pattern CONSTRAINT = Pattern.compile("(?i)constraint [\"']([a-z0-9_]+)[\"']");
	private AppView app;
	private DataLogicReplenishment data;
	private DataLogicSales sales;
	private DataLogicCustomers customers;
	private final DefaultTableModel model = new DefaultTableModel(new Object[]{"",
			AppLocal.getIntString("Replenishment.Type"), AppLocal.getIntString("Replenishment.Product"),
			AppLocal.getIntString("Replenishment.CustomerColumn"), AppLocal.getIntString("Replenishment.Status"),
			AppLocal.getIntString("Replenishment.Added")}, 0) {
		@Override
		public boolean isCellEditable(int row, int column) {
			return false;
		}
		@Override
		public Class<?> getColumnClass(int column) {
			return column == 0 ? Boolean.class : String.class;
		}
	};
	private final JTable table = new JTable(model) {
		@Override
		protected void processMouseEvent(java.awt.event.MouseEvent event) {
			if (event.getID() == java.awt.event.MouseEvent.MOUSE_PRESSED && columnAtPoint(event.getPoint()) == 0) {
				int row = rowAtPoint(event.getPoint());
				if (row >= 0) {
					if (isRowSelected(row))
						removeRowSelectionInterval(row, row);
					else
						addRowSelectionInterval(row, row);
					requestFocusInWindow();
				}
				return;
			}
			if (event.getID() == java.awt.event.MouseEvent.MOUSE_RELEASED && columnAtPoint(event.getPoint()) == 0)
				return;
			super.processMouseEvent(event);
		}
		@Override
		public void doLayout() {
			super.doLayout();
			resizeRows();
		}
		@Override
		public String getToolTipText(java.awt.event.MouseEvent event) {
			int row = rowAtPoint(event.getPoint());
			int column = columnAtPoint(event.getPoint());
			if (row >= 0 && entries != null && (column == 2 || column == 3)) {
				ReplenishmentEntry entry = entries.get(convertRowIndexToModel(row));
				return column == 2
						? entry.productId == null
								? entry.displayName()
								: (displayReference(entry.name, entry.reference) + " "
										+ displayProductName(entry.name, entry.reference)).trim()
						: entry.customerName;
			}
			return super.getToolTipText(event);
		}
	};
	private JButton delivered, findProduct;
	private JButton customerButton;
	private JPanel productButtons;
	private JLabel scanLabel;
	private boolean loading;
	private List<ReplenishmentEntry> entries;
	private JTextField formCode;
	private JTextComponent formDescription;
	private JTextArea formNoteArea;
	private JLabel formStatusLabel, withoutCodeLabel, manualDescriptionLabel, creatorLabel;
	private JLabel productCodes;
	private JTextArea formDeliveryHint;
	private JButton save;
	private JPanel formStatusButtons;
	private JTextArea selectedProduct, selectedCustomer;
	private JScrollPane manualDescriptionScroll;
	private JToggleButton formPending, formOrdered, formReceived, formDelivered;
	private JToggleButton allStatus;
	private JTextField search;
	private CustomerInfo formCustomer;
	private ProductInfoExt formProduct;
	private ReplenishmentEntry editingEntry;
	private String formStatus = "PENDING";
	private String selectedStatus = "ALL";
	private String selectedType = "ALL";
	private String displayedSearch = "";
	private javax.swing.Timer filterTimer;
	private final Map<String, JToggleButton> statusButtons = new HashMap<>();
	private JLabel queueFilterLabel;
	private JPanel detailCards;
	private JLabel selectionSummary;
	private JButton pendingAction, orderedAction, receivedAction, deleteAction;

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
		queueFilterLabel.setForeground(RetailPOSColors.inkMuted());
		header.add(queueFilterLabel, BorderLayout.CENTER);
		JButton newRequest = new JButton("+ " + AppLocal.getIntString("Replenishment.NewRequest"));
		newRequest.setMargin(new Insets(6, 12, 6, 12));
		JPanel filters = new JPanel(new BorderLayout(8, 4));
		JPanel statusToggles = new JPanel(new GridLayout(1, 5, 4, 0));
		ButtonGroup group = new ButtonGroup();
		String[] statuses = {"ALL", "PENDING", "ORDERED", "RECEIVED", "DELIVERED"};
		String[] labels = {"Replenishment.All", "Replenishment.Pending", "Replenishment.Ordered",
				"Replenishment.Received", "Replenishment.Delivered"};
		for (int i = 0; i < statuses.length; i++) {
			final String status = statuses[i];
			JToggleButton button = new JToggleButton(AppLocal.getIntString(labels[i]));
			styleFilterToggle(button);
			button.setMargin(new Insets(6, 2, 6, 2));
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
				clearSelectionForFilter();
				load("", false);
			});
		}
		search = new JTextField();
		search.putClientProperty("JTextField.placeholderText",
				AppLocal.getIntString("Replenishment.SearchPlaceholder"));
		search.putClientProperty("JTextField.leadingIcon", PosIcons.search());
		styleInput(search);
		JPanel searchRow = new JPanel(new BorderLayout(8, 0));
		searchRow.add(search, BorderLayout.CENTER);
		searchRow.add(newRequest, BorderLayout.EAST);
		filters.add(statusToggles, BorderLayout.NORTH);
		filters.add(searchRow, BorderLayout.CENTER);
		JPanel north = new JPanel(new BorderLayout());
		north.add(header, BorderLayout.NORTH);
		north.add(filters, BorderLayout.SOUTH);
		table.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
		table.setRowHeight(48);
		table.setShowHorizontalLines(true);
		table.setShowVerticalLines(false);
		table.setGridColor(RetailPOSColors.border());
		table.setIntercellSpacing(new java.awt.Dimension(0, 1));
		table.getTableHeader().setDefaultRenderer((t, value, selected, focused, row, column) -> {
			JLabel headerLabel = new JLabel(column == 0
					? table.getRowCount() > 0 && table.getSelectedRowCount() == table.getRowCount() ? "☑" : "☐"
					: valueOrEmpty((String) value));
			headerLabel.setFont(t.getTableHeader().getFont());
			headerLabel.setForeground(RetailPOSColors.ink());
			headerLabel.setBackground(RetailPOSColors.surface100());
			headerLabel.setOpaque(true);
			headerLabel.setHorizontalAlignment(JLabel.LEFT);
			headerLabel.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
			return headerLabel;
		});
		table.getTableHeader().addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent event) {
				if (table.columnAtPoint(event.getPoint()) != 0)
					return;
				if (table.getSelectedRowCount() == table.getRowCount())
					clearSelectedRows();
				else if (table.getRowCount() > 0)
					table.setRowSelectionInterval(0, table.getRowCount() - 1);
			}
		});
		table.getInputMap().put(KeyStroke.getKeyStroke("SPACE"), "toggleRequest");
		table.getActionMap().put("toggleRequest", new AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent event) {
				int row = table.getSelectionModel().getLeadSelectionIndex();
				if (row >= 0 && row < table.getRowCount()) {
					if (table.isRowSelected(row))
						table.removeRowSelectionInterval(row, row);
					else
						table.addRowSelectionInterval(row, row);
				}
			}
		});
		table.getInputMap().put(KeyStroke.getKeyStroke("ESCAPE"), "clearRequests");
		table.getActionMap().put("clearRequests", new AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent event) {
				clearSelectedRows();
			}
		});
		table.getSelectionModel().addListSelectionListener(e -> {
			if (!loading && !e.getValueIsAdjusting()) {
				for (int row = 0; row < model.getRowCount(); row++)
					model.setValueAt(table.isRowSelected(row), row, 0);
				table.getTableHeader().repaint();
				if (table.getSelectedRowCount() == 1 && entries != null) {
					ReplenishmentEntry selectedEntry = entries
							.get(table.convertRowIndexToModel(table.getSelectedRow()));
					if (editingEntry == null || !editingEntry.id.equals(selectedEntry.id))
						loadEntry(selectedEntry);
				}
				updateSelectionView();
			}
		});
		table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
			{
				setVerticalAlignment(JLabel.TOP);
			}
			@Override
			public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
					boolean focused, int row, int column) {
				java.awt.Component component = super.getTableCellRendererComponent(table, value, selected, focused, row,
						column);
				setFont(RetailPOSTheme.MANROPE_MEDIUM);
				component.setBackground(selected ? RetailPOSColors.surface200() : RetailPOSColors.surface100());
				component.setForeground(RetailPOSColors.ink());
				setBorder(
						BorderFactory
								.createCompoundBorder(
										selected
												? BorderFactory.createMatteBorder(1, 0, 1, 0,
														RetailPOSColors.borderStrong())
												: BorderFactory.createEmptyBorder(1, 0, 1, 0),
										BorderFactory.createEmptyBorder(8, 8, 0, 0)));
				return component;
			}
		});
		table.getColumnModel().getColumn(0).setCellRenderer((grid, value, selected, focused, row, column) -> {
			JCheckBox check = new JCheckBox();
			check.setHorizontalAlignment(JLabel.CENTER);
			check.setSelected(Boolean.TRUE.equals(value));
			check.setOpaque(true);
			check.setBackground(selected ? RetailPOSColors.surface200() : RetailPOSColors.surface100());
			check.setForeground(RetailPOSColors.ink());
			check.setBorderPainted(true);
			check.setBorder(
					BorderFactory
							.createCompoundBorder(
									selected
											? BorderFactory.createMatteBorder(1, 0, 1, 0,
													RetailPOSColors.borderStrong())
											: BorderFactory.createEmptyBorder(1, 0, 1, 0),
									BorderFactory.createEmptyBorder(0, 4, 0, 4)));
			return check;
		});
		table.getColumnModel().getColumn(4).setCellRenderer(new StatusBadgeRenderer());
		DefaultTableCellRenderer wrappingRenderer = new DefaultTableCellRenderer() {
			@Override
			public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
					boolean focused, int row, int column) {
				JTextArea name = new JTextArea(
						preview(valueOrEmpty((String) value), table.getColumnModel().getColumn(column).getWidth()));
				name.setFont(RetailPOSTheme.MANROPE_MEDIUM);
				// The two-line preview already contains explicit line breaks.
				name.setLineWrap(false);
				name.setBackground(selected ? RetailPOSColors.surface200() : RetailPOSColors.surface100());
				name.setForeground(RetailPOSColors.ink());
				name.setBorder(
						BorderFactory
								.createCompoundBorder(
										selected
												? BorderFactory.createMatteBorder(1, 0, 1, 0,
														RetailPOSColors.borderStrong())
												: BorderFactory.createEmptyBorder(1, 0, 1, 0),
										BorderFactory.createEmptyBorder(8, 8, 0, 4)));
				return name;
			}
		};
		table.getColumnModel().getColumn(2).setCellRenderer(new ProductRenderer());
		table.getColumnModel().getColumn(3).setCellRenderer(wrappingRenderer);
		JPanel list = new JPanel(new BorderLayout());
		JScrollPane tableScroll = new JScrollPane(table);
		tableScroll.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
		list.add(tableScroll, BorderLayout.CENTER);
		JPanel actions = new JPanel(new GridBagLayout());
		actions.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
		JButton pending = new JButton(AppLocal.getIntString("Replenishment.FormPending"));
		JButton ordered = new JButton(AppLocal.getIntString("Replenishment.FormOrdered"));
		JButton received = new JButton(AppLocal.getIntString("Replenishment.FormReceived"));
		delivered = new JButton(AppLocal.getIntString("Replenishment.FormDelivered"));
		JButton delete = new JButton(AppLocal.getIntString("Replenishment.Delete"));
		pendingAction = pending;
		orderedAction = ordered;
		receivedAction = received;
		deleteAction = delete;
		delete.setBackground(RetailPOSColors.surface100());
		delete.setForeground(RetailPOSColors.dangerText());
		delete.putClientProperty("FlatLaf.style",
				"background: " + RetailPOSColors.toHex(RetailPOSColors.surface100()) + "; foreground: "
						+ RetailPOSColors.toHex(RetailPOSColors.dangerText()) + "; borderColor: "
						+ RetailPOSColors.toHex(RetailPOSColors.danger()));
		JButton export = new JButton(AppLocal.getIntString("Replenishment.Export"));
		for (JButton button : new JButton[]{pending, ordered, received, delivered, delete, export}) {
			button.setFont(button.getFont().deriveFont(13f));
			button.setMargin(new Insets(6, 6, 6, 6));
		}
		GridBagConstraints actionCell = new GridBagConstraints();
		actionCell.gridx = 0;
		actionCell.gridy = 0;
		actionCell.fill = GridBagConstraints.VERTICAL;
		actionCell.insets = new Insets(0, 2, 0, 2);
		for (java.awt.Component action : new java.awt.Component[]{
				new JLabel(AppLocal.getIntString("Replenishment.MarkAs")), pending, ordered, received, delivered,
				Box.createHorizontalStrut(8), delete}) {
			actions.add(action, actionCell);
			actionCell.gridx++;
		}
		actionCell.weightx = 1;
		actionCell.fill = GridBagConstraints.HORIZONTAL;
		actions.add(Box.createHorizontalGlue(), actionCell);
		actionCell.gridx++;
		actionCell.weightx = 0;
		actionCell.fill = GridBagConstraints.VERTICAL;
		actions.add(export, actionCell);
		list.add(actions, BorderLayout.SOUTH);
		delivered.setEnabled(false);
		delivered.setToolTipText(AppLocal.getIntString("Replenishment.DeliveryHint"));
		javax.swing.table.TableColumnModel columns = table.getColumnModel();
		columns.getColumn(0).setMinWidth(48);
		columns.getColumn(0).setPreferredWidth(48);
		columns.getColumn(0).setMaxWidth(48);
		java.awt.FontMetrics typeMetrics = table.getFontMetrics(RetailPOSTheme.MANROPE_MEDIUM);
		int typeWidth = Math.max(typeMetrics.stringWidth(AppLocal.getIntString("Replenishment.Replenishment")),
				typeMetrics.stringWidth(AppLocal.getIntString("Replenishment.Order"))) + 16;
		columns.getColumn(1).setMinWidth(typeWidth);
		columns.getColumn(1).setPreferredWidth(typeWidth);
		columns.getColumn(1).setMaxWidth(typeWidth);
		columns.getColumn(2).setMinWidth(115);
		columns.getColumn(2).setPreferredWidth(260);
		columns.getColumn(3).setMinWidth(155);
		columns.getColumn(3).setPreferredWidth(200);
		columns.getColumn(3).setMaxWidth(210);
		columns.getColumn(4).setPreferredWidth(105);
		columns.getColumn(4).setMinWidth(95);
		columns.getColumn(4).setMaxWidth(125);
		int dateWidth = tabularDateWidth(table.getFontMetrics(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f)),
				"00/00/00-00:00") + 16;
		columns.getColumn(5).setMinWidth(dateWidth);
		columns.getColumn(5).setPreferredWidth(dateWidth);
		columns.getColumn(5).setMaxWidth(dateWidth);
		table.getColumnModel().getColumn(5).setCellRenderer(new DefaultTableCellRenderer() {
			@Override
			public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
					boolean focused, int row, int column) {
				super.getTableCellRendererComponent(table, value, selected, focused, row, column);
				setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
				setVerticalAlignment(JLabel.TOP);
				setBackground(selected ? RetailPOSColors.surface200() : RetailPOSColors.surface100());
				setForeground(RetailPOSColors.ink());
				setBorder(
						BorderFactory
								.createCompoundBorder(
										selected
												? BorderFactory.createMatteBorder(1, 0, 1, 0,
														RetailPOSColors.borderStrong())
												: BorderFactory.createEmptyBorder(1, 0, 1, 0),
										BorderFactory.createEmptyBorder(8, 8, 0, 0)));
				return this;
			}
			@Override
			protected void paintComponent(java.awt.Graphics graphics) {
				java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
				g.setColor(getBackground());
				g.fillRect(0, 0, getWidth(), getHeight());
				g.setFont(getFont());
				g.setColor(getForeground());
				java.awt.FontMetrics metrics = g.getFontMetrics();
				int digitWidth = widestDigit(metrics);
				int x = getInsets().left;
				int y = getInsets().top + metrics.getAscent();
				for (char character : getText().toCharArray()) {
					int width = metrics.charWidth(character);
					int advance = Character.isDigit(character) ? digitWidth : width;
					g.drawString(String.valueOf(character), x + (advance - width) / 2, y);
					x += advance;
				}
				g.dispose();
			}
		});
		table.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		table.getColumnModel().addColumnModelListener(new javax.swing.event.TableColumnModelListener() {
			@Override
			public void columnMarginChanged(javax.swing.event.ChangeEvent event) {
				resizeRows();
			}
			@Override
			public void columnAdded(javax.swing.event.TableColumnModelEvent event) {
			}
			@Override
			public void columnRemoved(javax.swing.event.TableColumnModelEvent event) {
			}
			@Override
			public void columnMoved(javax.swing.event.TableColumnModelEvent event) {
			}
			@Override
			public void columnSelectionChanged(javax.swing.event.ListSelectionEvent event) {
			}
		});
		table.addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent event) {
				resizeRows();
			}
		});
		JPanel left = new JPanel(new BorderLayout());
		left.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 8));
		left.add(north, BorderLayout.NORTH);
		left.add(list, BorderLayout.CENTER);
		detailCards = new JPanel(new CardLayout());
		detailCards.add(createForm(), "form");
		JPanel selectionPanel = new JPanel(new BorderLayout(8, 8));
		selectionPanel.setBorder(BorderFactory.createEmptyBorder(20, 16, 20, 16));
		selectionSummary = new JLabel();
		selectionPanel.add(selectionSummary, BorderLayout.NORTH);
		JButton clearSelection = new JButton(AppLocal.getIntString("Replenishment.ClearSelection"));
		clearSelection.addActionListener(e -> clearSelectedRows());
		selectionPanel.add(clearSelection, BorderLayout.SOUTH);
		detailCards.add(selectionPanel, "selection");
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, detailCards);
		split.setResizeWeight(1.0);
		split.setDividerLocation(0.72);
		split.setDividerSize(2);
		split.setBorder(null);
		if (split.getUI() instanceof BasicSplitPaneUI) {
			BasicSplitPaneUI splitUI = (BasicSplitPaneUI) split.getUI();
			splitUI.getDivider().setBackground(RetailPOSColors.border());
		}
		add(split, BorderLayout.CENTER);
		search.addActionListener(e -> {
			clearSelectionForFilter();
			load(search.getText(), false);
		});
		filterTimer = new javax.swing.Timer(250, e -> {
			clearSelectionForFilter();
			load(search.getText(), false);
		});
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
		pending.addActionListener(e -> change("PENDING"));
		delivered.addActionListener(e -> change("DELIVERED"));
		delete.addActionListener(e -> deleteSelected());
		export.addActionListener(e -> exportVisibleEntries());
		javax.swing.SwingUtilities.invokeLater(() -> formCode.requestFocusInWindow());
	}
	private void deleteSelected() {
		List<String> ids = selectedIds();
		if (ids.isEmpty())
			return;
		if (ids.size() > 1 && save.isEnabled()) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.UnsavedBulkEdits"));
			return;
		}
		int answer = JConfirmationDialog.show(this,
				ids.size() == 1
						? AppLocal.getIntString("Replenishment.ConfirmDelete")
						: String.format(AppLocal.getIntString("Replenishment.ConfirmDeleteMultiple"), ids.size()),
				AppLocal.getIntString("Replenishment.Delete"), AppLocal.getIntString("Button.Cancel"),
				AppLocal.getIntString("Replenishment.Delete"), true);
		if (answer != JOptionPane.YES_OPTION)
			return;
		try {
			data.deleteAll(ids);
			clearForm();
			load(search.getText(), false);
		} catch (BasicException e) {
			JOptionPane.showMessageDialog(this, e.getMessage());
		}
	}
	private void exportVisibleEntries() {
		if (filterTimer.isRunning()) {
			filterTimer.stop();
			if (!load(search.getText(), false))
				return;
		}
		if (entries == null || entries.isEmpty()) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.NothingToExport"));
			return;
		}
		List<ReplenishmentEntry> exported;
		try {
			exported = data.listForExport(displayedSearch, selectedType, selectedStatus);
		} catch (BasicException ex) {
			JOptionPane.showMessageDialog(this, ex.getMessage());
			return;
		}
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(new File(String.format(AppLocal.getIntString("Replenishment.ExportFileName"),
				new SimpleDateFormat("yyyy-MM-dd").format(new java.util.Date()))));
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
			return;
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(chooser.getSelectedFile()))) {
			writer.write("producte\tnom clienta\ttelefon\tnota\tpersona");
			writer.newLine();
			for (ReplenishmentEntry entry : exported) {
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
				writer.write(exportValue(creatorName(entry)));
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
	private String selectedSellerName() {
		UserInfo seller = selectedSeller();
		return seller == null ? "" : valueOrEmpty(seller.getName());
	}
	private static void styleInput(JTextComponent input) {
		input.setBackground(RetailPOSColors.surface200());
		input.setForeground(RetailPOSColors.ink());
		if (input instanceof JTextField) {
			input.putClientProperty("FlatLaf.style",
					"arc: 6; borderColor: " + RetailPOSColors.toHex(RetailPOSColors.border()) + "; focusedBorderColor: "
							+ RetailPOSColors.toHex(RetailPOSColors.borderStrong()));
		}
	}
	private static void styleAreaScroll(JScrollPane scroll, JTextArea area) {
		scroll.setBorder(BorderFactory.createLineBorder(RetailPOSColors.border()));
		area.addFocusListener(new java.awt.event.FocusAdapter() {
			@Override
			public void focusGained(java.awt.event.FocusEvent event) {
				scroll.setBorder(BorderFactory.createLineBorder(RetailPOSColors.borderStrong()));
			}
			@Override
			public void focusLost(java.awt.event.FocusEvent event) {
				scroll.setBorder(BorderFactory.createLineBorder(RetailPOSColors.border()));
			}
		});
	}
	private static void styleStatusToggle(JToggleButton button) {
		button.putClientProperty("FlatLaf.style",
				"selectedBackground: " + RetailPOSColors.toHex(RetailPOSColors.surface200()) + "; selectedForeground: "
						+ RetailPOSColors.toHex(RetailPOSColors.ink()) + "; selectedBorderColor: "
						+ RetailPOSColors.toHex(RetailPOSColors.borderStrong()));
	}
	private static void styleFilterToggle(JToggleButton button) {
		button.putClientProperty("FlatLaf.style",
				"selectedBackground: " + RetailPOSColors.toHex(RetailPOSColors.brand()) + "; selectedForeground: "
						+ RetailPOSColors.toHex(RetailPOSColors.onBrand()) + "; selectedBorderColor: "
						+ RetailPOSColors.toHex(RetailPOSColors.brandStrong()));
	}
	private static int widestDigit(java.awt.FontMetrics metrics) {
		int widest = 0;
		for (char digit = '0'; digit <= '9'; digit++)
			widest = Math.max(widest, metrics.charWidth(digit));
		return widest;
	}
	private static int tabularDateWidth(java.awt.FontMetrics metrics, String date) {
		int width = 0;
		int digitWidth = widestDigit(metrics);
		for (char character : date.toCharArray())
			width += Character.isDigit(character) ? digitWidth : metrics.charWidth(character);
		return width;
	}
	private static JTextArea hintArea() {
		JTextArea hint = new JTextArea();
		hint.setEditable(false);
		hint.setFocusable(false);
		hint.setLineWrap(true);
		hint.setWrapStyleWord(true);
		hint.setRows(2);
		hint.setColumns(1);
		hint.setOpaque(false);
		hint.setForeground(RetailPOSColors.inkMuted());
		return hint;
	}
	private JPanel createForm() {
		JPanel form = new JPanel(new BorderLayout(5, 5));
		form.setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 0));
		// Two status buttons per row fit translated labels without a horizontal
		// scrollbar.
		form.setPreferredSize(new java.awt.Dimension(320, 0));
		form.setMinimumSize(new java.awt.Dimension(300, 0));
		JPanel fields = new FormFields();
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
		styleInput(formCode);
		formCode.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f));
		formDescription = new JTextArea(3, 20);
		styleInput(formDescription);
		((JTextArea) formDescription).setRows(3);
		((JTextArea) formDescription).setLineWrap(true);
		((JTextArea) formDescription).setWrapStyleWord(true);
		c.gridy = 0;
		scanLabel = new JLabel(AppLocal.getIntString("Replenishment.Scan"));
		productGroup.add(scanLabel, c);
		c.gridy = 1;
		productGroup.add(formCode, c);
		formCode.setMinimumSize(new java.awt.Dimension(0, 48));
		formCode.setPreferredSize(new java.awt.Dimension(0, 48));
		productButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		findProduct = new JButton(AppLocal.getIntString("Replenishment.SearchProduct"));
		findProduct.setMargin(new Insets(6, 10, 6, 10));
		productButtons.add(findProduct);
		c.gridy = 2;
		productGroup.add(productButtons, c);
		selectedProduct = new JTextArea(AppLocal.getIntString("Replenishment.NoProductSelected"));
		selectedProduct.setEditable(false);
		selectedProduct.setFocusable(false);
		selectedProduct.setLineWrap(true);
		selectedProduct.setWrapStyleWord(true);
		selectedProduct.setOpaque(false);
		selectedProduct.setColumns(1);
		c.gridy = 3;
		productGroup.add(selectedProduct, c);
		productCodes = new JLabel();
		productCodes.setForeground(RetailPOSColors.inkMuted());
		productCodes.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f));
		c.gridy = 4;
		productGroup.add(productCodes, c);
		withoutCodeLabel = new JLabel(AppLocal.getIntString("Replenishment.WithoutCode"), JLabel.CENTER);
		withoutCodeLabel.setForeground(RetailPOSColors.inkMuted());
		withoutCodeLabel.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, RetailPOSColors.border()),
				BorderFactory.createEmptyBorder(8, 0, 8, 0)));
		c.gridy = 5;
		productGroup.add(withoutCodeLabel, c);
		manualDescriptionLabel = new JLabel(AppLocal.getIntString("Replenishment.Description"));
		c.gridy = 6;
		productGroup.add(manualDescriptionLabel, c);
		manualDescriptionScroll = new JScrollPane((JTextArea) formDescription);
		styleAreaScroll(manualDescriptionScroll, (JTextArea) formDescription);
		manualDescriptionScroll.setMinimumSize(new java.awt.Dimension(0, 58));
		manualDescriptionScroll.setPreferredSize(new java.awt.Dimension(0, 64));
		c.gridy = 7;
		productGroup.add(manualDescriptionScroll, c);
		JPanel customerGroup = new JPanel(new BorderLayout(6, 0));
		selectedCustomer = new JTextArea(AppLocal.getIntString("Replenishment.NoCustomer"));
		selectedCustomer.setEditable(false);
		selectedCustomer.setFocusable(false);
		selectedCustomer.setLineWrap(true);
		selectedCustomer.setWrapStyleWord(true);
		selectedCustomer.setOpaque(false);
		selectedCustomer.setColumns(1);
		customerButton = new JButton(AppLocal.getIntString("Replenishment.Customer"));
		customerButton.setMargin(new Insets(6, 10, 6, 10));
		customerGroup.add(selectedCustomer, BorderLayout.CENTER);
		customerGroup.add(customerButton, BorderLayout.EAST);
		formNoteArea = new JTextArea(4, 20);
		styleInput(formNoteArea);
		formNoteArea.setLineWrap(true);
		formNoteArea.setWrapStyleWord(true);
		JScrollPane noteScroll = new JScrollPane(formNoteArea);
		styleAreaScroll(noteScroll, formNoteArea);
		noteScroll.setPreferredSize(new java.awt.Dimension(260, 100));
		JPanel noteGroup = new JPanel(new BorderLayout());
		noteGroup.add(noteScroll, BorderLayout.CENTER);
		formStatusLabel = sectionLabel("Replenishment.Status");
		formStatusButtons = new JPanel(new GridLayout(0, 2, 4, 4));
		ButtonGroup statusGroup = new ButtonGroup();
		formPending = new JToggleButton(AppLocal.getIntString("Replenishment.FormPending"));
		formOrdered = new JToggleButton(AppLocal.getIntString("Replenishment.FormOrdered"));
		formReceived = new JToggleButton(AppLocal.getIntString("Replenishment.FormReceived"));
		formDelivered = new JToggleButton(AppLocal.getIntString("Replenishment.FormDelivered"));
		for (JToggleButton button : new JToggleButton[]{formPending, formOrdered, formReceived, formDelivered})
			styleStatusToggle(button);
		formPending.setMargin(new Insets(6, 10, 6, 10));
		formOrdered.setMargin(new Insets(6, 10, 6, 10));
		formReceived.setMargin(new Insets(6, 10, 6, 10));
		formDelivered.setMargin(new Insets(6, 10, 6, 10));
		formPending.setSelected(true);
		formDelivered.setEnabled(false);
		formDelivered.setToolTipText(AppLocal.getIntString("Replenishment.DeliveryHint"));
		selectFormStatus();
		statusGroup.add(formPending);
		statusGroup.add(formOrdered);
		statusGroup.add(formReceived);
		statusGroup.add(formDelivered);
		formStatusButtons.add(formPending);
		formStatusButtons.add(formOrdered);
		formStatusButtons.add(formReceived);
		formStatusButtons.add(formDelivered);
		formDeliveryHint = hintArea();
		formDeliveryHint.setText(AppLocal.getIntString("Replenishment.DeliveryHint"));
		formDeliveryHint.setVisible(false);
		formPending.addActionListener(e -> setFormStatus("PENDING"));
		formOrdered.addActionListener(e -> setFormStatus("ORDERED"));
		formReceived.addActionListener(e -> setFormStatus("RECEIVED"));
		formDelivered.addActionListener(e -> setFormStatus("DELIVERED"));
		JPanel customerSection = new JPanel(new BorderLayout(0, 2));
		customerSection.add(sectionLabel("Replenishment.CustomerColumn"), BorderLayout.NORTH);
		customerSection.add(customerGroup, BorderLayout.CENTER);
		JPanel noteSection = new JPanel(new BorderLayout(0, 2));
		noteSection.add(sectionLabel("Replenishment.Note"), BorderLayout.NORTH);
		noteSection.add(noteGroup, BorderLayout.CENTER);
		JPanel statusSection = new JPanel(new BorderLayout(0, 2));
		statusSection.add(formStatusLabel, BorderLayout.NORTH);
		JPanel statusControls = new JPanel(new BorderLayout(0, 4));
		statusControls.add(formStatusButtons, BorderLayout.NORTH);
		statusControls.add(formDeliveryHint, BorderLayout.CENTER);
		statusSection.add(statusControls, BorderLayout.CENTER);
		creatorLabel = new JLabel(AppLocal.getIntString("Replenishment.CreatedBy") + ": " + selectedSellerName());
		outer.gridy = 0;
		fields.add(sectionLabel("Replenishment.Product"), outer);
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
		save = new JButton(AppLocal.getIntString("Replenishment.Save"));
		RetailPOSColors.primaryButton(save);
		save.setPreferredSize(new java.awt.Dimension(save.getPreferredSize().width + 24, 48));
		save.setMargin(new Insets(6, 12, 6, 12));
		JButton cancel = new JButton(AppLocal.getIntString("Button.Cancel"));
		cancel.setMargin(new Insets(6, 12, 6, 12));
		JPanel formActions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		formActions.setBorder(BorderFactory.createEmptyBorder(8, 0, 0, 0));
		formActions.add(cancel);
		formActions.add(save);
		JScrollPane fieldsScroll = new JScrollPane(fields);
		fieldsScroll.setBorder(BorderFactory.createEmptyBorder());
		fieldsScroll.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		fieldsScroll.getVerticalScrollBar().setUnitIncrement(16);
		form.add(fieldsScroll, BorderLayout.CENTER);
		form.add(formActions, BorderLayout.SOUTH);
		formCode.addActionListener(e -> findFormProduct());
		formNoteArea.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				updateSaveState();
			}
			public void removeUpdate(DocumentEvent e) {
				updateSaveState();
			}
			public void changedUpdate(DocumentEvent e) {
				updateSaveState();
			}
		});
		findProduct.addActionListener(e -> {
			if (editingEntry != null)
				return;
			formProduct = JProductFinder.showMessage(this, sales);
			showSelectedProduct();
		});
		customerButton.addActionListener(e -> chooseFormCustomer());
		save.addActionListener(e -> saveForm());
		cancel.addActionListener(e -> clearForm());
		updateFormDeliveredEnabled();
		updateSaveState();
		return form;
	}
	private static JLabel sectionLabel(String key) {
		JLabel label = new JLabel(AppLocal.getIntString(key));
		label.setFont(label.getFont().deriveFont(java.awt.Font.BOLD, 13f));
		return label;
	}
	private void findFormProduct() {
		if (editingEntry != null)
			return;
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
			productCodes.setText("");
			formDescription.setEditable(true);
		} else {
			selectedProduct.setText(displayProductName(formProduct.getName(), formProduct.getReference()));
			productCodes.setText(displayReference(formProduct.getName(), formProduct.getReference()) + " · EAN "
					+ valueOrEmpty(formProduct.getCode()));
			formDescription.setText(valueOrEmpty(formProduct.getName()));
			formDescription.setEditable(false);
		}
		if (editingEntry != null)
			formDescription.setEditable(false);
		withoutCodeLabel.setVisible(manual && editingEntry == null);
		manualDescriptionLabel.setVisible(manual && editingEntry == null);
		manualDescriptionScroll.setVisible(manual && editingEntry == null);
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
				: customerDisplayName(formCustomer));
		updateCustomerButton();
		updateFormDeliveredEnabled();
		updateSaveState();
	}
	private void updateSaveState() {
		if (save == null)
			return;
		boolean dirty = editingEntry == null
				|| !formNoteArea.getText().trim().equals(valueOrEmpty(editingEntry.note)) || !java.util.Objects
						.equals(formCustomer == null ? null : formCustomer.getId(), editingEntry.customerId)
				|| !formStatus.equals(editingEntry.status);
		save.setEnabled(dirty);
		if (dirty) {
			RetailPOSColors.primaryButton(save);
		} else {
			save.putClientProperty("FlatLaf.style", null);
			save.setOpaque(false);
			save.setBackground(RetailPOSColors.surface100());
			save.setForeground(RetailPOSColors.ink());
		}
	}
	private void updateCustomerButton() {
		customerButton.setText(AppLocal
				.getIntString(formCustomer == null ? "Replenishment.Customer" : "Replenishment.ChangeCustomer"));
	}
	private void saveForm() {
		if (editingEntry == null && formProduct == null && formCode.getText().trim().isEmpty()
				&& formDescription.getText().trim().isEmpty()) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.SelectProduct"));
			return;
		}
		String customerId = formCustomer == null ? null : formCustomer.getId();
		if (editingEntry != null && "DELIVERED".equals(formStatus) && customerId == null) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.KeepDeliveredCustomer"));
			return;
		}
		try {
			if (editingEntry == null) {
				ReplenishmentEntry e = new ReplenishmentEntry();
				if (formProduct != null) {
					e.productId = formProduct.getID();
					e.reference = formProduct.getReference();
					e.name = formProduct.getName();
					e.ean = formProduct.getCode();
				} else {
					e.manualEan = formCode.getText().trim();
					e.manualDescription = formDescription.getText().trim();
				}
				e.note = formNoteArea.getText().trim();
				e.customerId = customerId;
				e.customerName = formCustomer == null ? null : customerDisplayName(formCustomer);
				e.createdBy = selectedSeller() == null ? null : selectedSeller().getId();
				ReplenishmentEntry saved = data.add(e);
				if (!"PENDING".equals(formStatus))
					data.updateStatus(saved.id, formStatus, app.getAppUserView().getUser().getId());
				clearForm();
				load("", false);
			} else {
				String id = editingEntry.id;
				data.updateDetailsAndStatus(id, formNoteArea.getText().trim(), customerId,
						formCustomer == null ? null : customerDisplayName(formCustomer), formStatus,
						app.getAppUserView().getUser().getId());
				load("", false, id);
			}
		} catch (BasicException ex) {
			logStatusFailure("save", formStatus, editingEntry == null ? null : editingEntry.id, ex);
			String message = DataLogicReplenishment.STORE_REPLENISHMENT_EXISTS.equals(ex.getMessage())
					? AppLocal.getIntString("Replenishment.StoreAlreadyExists")
					: AppLocal.getIntString("Replenishment.StatusUpdateFailed");
			JOptionPane.showMessageDialog(this, message);
		}
	}
	private void loadEntry(ReplenishmentEntry entry) {
		editingEntry = entry;
		formStatus = entry.status;
		creatorLabel.setText(AppLocal.getIntString("Replenishment.CreatedBy") + ": " + creatorName(entry));
		formCode.setText(entry.productId == null ? valueOrEmpty(entry.manualEan) : valueOrEmpty(entry.ean));
		formCode.setEditable(false);
		findProduct.setEnabled(false);
		productButtons.setVisible(false);
		scanLabel.setVisible(false);
		formCode.setVisible(false);
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
		selectedCustomer.setText(entry.customerId == null
				? AppLocal.getIntString("Replenishment.NoCustomer")
				: customerDisplayName(formCustomer, entry.customerName));
		updateCustomerButton();
		showSelectedProduct();
		if (entry.productId == null)
			selectedProduct.setText(valueOrEmpty(entry.manualDescription));
		if (entry.productId == null)
			productCodes.setText(valueOrEmpty(entry.manualEan));
		selectFormStatus();
		updateFormDeliveredEnabled();
		updateDeliveredEnabled();
		updateSaveState();
	}
	private void clearForm() {
		editingEntry = null;
		formStatus = "PENDING";
		creatorLabel.setText(AppLocal.getIntString("Replenishment.CreatedBy") + ": " + selectedSellerName());
		formCode.setText("");
		formDescription.setText("");
		formDescription.setEditable(true);
		formNoteArea.setText("");
		formCustomer = null;
		formProduct = null;
		formCode.setEditable(true);
		formCode.setVisible(true);
		findProduct.setEnabled(true);
		productButtons.setVisible(true);
		scanLabel.setVisible(true);
		selectFormStatus();
		updateFormDeliveredEnabled();
		showSelectedProduct();
		selectedProduct.setText(AppLocal.getIntString("Replenishment.NoProductSelected"));
		selectedCustomer.setText(AppLocal.getIntString("Replenishment.NoCustomer"));
		updateCustomerButton();
		table.clearSelection();
		updateDeliveredEnabled();
		updateSaveState();
		formCode.requestFocusInWindow();
	}
	private boolean load(String search, boolean received) {
		return load(search, received, editingEntry == null ? null : editingEntry.id, false);
	}
	private boolean load(String search, boolean received, String selectId) {
		return load(search, received, selectId, true);
	}
	private boolean load(String search, boolean received, String selectId, boolean reloadForm) {
		try {
			List<ReplenishmentEntry> refreshed = data.list(search, selectedType, selectedStatus);
			List<ReplenishmentEntry> all = "ALL".equals(selectedStatus)
					? refreshed
					: data.list(search, selectedType, "ALL");
			for (Map.Entry<String, JToggleButton> filter : statusButtons.entrySet()) {
				String key = filter.getKey();
				String labelKey = "ALL".equals(key)
						? "Replenishment.All"
						: "Replenishment." + key.charAt(0) + key.substring(1).toLowerCase(java.util.Locale.ROOT);
				long count = all.stream().filter(e -> "ALL".equals(key) || key.equals(e.status)).count();
				filter.getValue().setText(AppLocal.getIntString(labelKey) + " (" + count + ")");
			}
			loading = true;
			table.clearSelection();
			entries = refreshed;
			model.setRowCount(0);
			DateFormat f = new SimpleDateFormat("dd/MM/yy-HH:mm");
			int selectedRow = -1;
			for (ReplenishmentEntry e : entries) {
				if (e.id.equals(selectId))
					selectedRow = model.getRowCount();
				model.addRow(new Object[]{false,
						e.customerId == null
								? AppLocal.getIntString("Replenishment.Replenishment")
								: AppLocal.getIntString("Replenishment.Order"),
						e.displayName(), valueOrEmpty(e.customerName), statusLabel(e.status), f.format(e.createdAt)});
			}
			resizeRows();
			displayedSearch = search;
			if (selectedRow >= 0) {
				table.setRowSelectionInterval(selectedRow, selectedRow);
				if (reloadForm)
					loadEntry(entries.get(selectedRow));
			} else {
				if (editingEntry != null)
					clearForm();
			}
			loading = false;
			updateSelectionView();
			return true;
		} catch (BasicException e) {
			loading = false;
			table.clearSelection();
			model.setRowCount(0);
			entries = null;
			JOptionPane.showMessageDialog(this, e.getMessage());
			return false;
		}
	}
	private String statusLabel(String status) {
		return "DELIVERED".equals(status)
				? AppLocal.getIntString("Replenishment.FormDelivered")
				: "RECEIVED".equals(status)
						? AppLocal.getIntString("Replenishment.FormReceived")
						: "ORDERED".equals(status)
								? AppLocal.getIntString("Replenishment.FormOrdered")
								: AppLocal.getIntString("Replenishment.FormPending");
	}
	private static java.awt.Color statusColor(String state) {
		return "DELIVERED".equals(state)
				? RetailPOSColors.surface200()
				: "RECEIVED".equals(state)
						? RetailPOSColors.success()
						: "ORDERED".equals(state) ? RetailPOSColors.info() : RetailPOSColors.warning();
	}
	private static java.awt.Color badgeFill(String state) {
		java.awt.Color surface = RetailPOSColors.surface100();
		java.awt.Color accent = statusColor(state);
		return new java.awt.Color((surface.getRed() * 3 + accent.getRed()) / 4,
				(surface.getGreen() * 3 + accent.getGreen()) / 4, (surface.getBlue() * 3 + accent.getBlue()) / 4);
	}
	private void setFormStatus(String status) {
		formStatus = status;
		selectFormStatus();
		updateSaveState();
	}
	private void resizeRows() {
		int productWidth = table.getColumnModel().getColumn(2).getWidth();
		int customerWidth = table.getColumnModel().getColumn(3).getWidth();
		if (productWidth <= 0 || customerWidth <= 0 || entries == null)
			return;
		for (int row = 0; row < table.getRowCount(); row++) {
			ReplenishmentEntry entry = entries.get(table.convertRowIndexToModel(row));
			String productName = entry.productId == null
					? entry.displayName()
					: displayProductName(entry.name, entry.reference);
			int productHeight = wrappedHeight(productName, productWidth);
			if (entry.productId != null && !displayReference(entry.name, entry.reference).isBlank())
				productHeight += table.getFontMetrics(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f)).getHeight();
			int height = Math.max(48, Math.max(productHeight, wrappedHeight(entry.customerName, customerWidth)) + 16);
			if (table.getRowHeight(row) != height)
				table.setRowHeight(row, height);
		}
	}
	private static String displayProductName(String name, String reference) {
		String text = valueOrEmpty(name).trim();
		String code = valueOrEmpty(reference).trim();
		if (!code.isEmpty() && text.endsWith(" [" + code + "]"))
			return text.substring(0, text.length() - code.length() - 3).stripTrailing();
		return text;
	}
	private static String displayReference(String name, String reference) {
		String code = valueOrEmpty(reference).trim();
		String displayName = displayProductName(name, reference);
		int separator = displayName.lastIndexOf(" — ");
		if (separator >= 0) {
			String variant = displayName.substring(separator + 3).trim();
			if (!variant.isEmpty() && code.endsWith(" " + variant))
				return code.substring(0, code.length() - variant.length() - 1);
		}
		return code;
	}
	private static int wrappedHeight(String text, int columnWidth) {
		String visible = preview(text, columnWidth);
		int lines = (int) visible.chars().filter(ch -> ch == '\n').count() + 1;
		return lines * new JLabel().getFontMetrics(RetailPOSTheme.MANROPE_MEDIUM).getHeight();
	}
	private static String preview(String text, int columnWidth) {
		java.awt.FontMetrics metrics = new JLabel().getFontMetrics(RetailPOSTheme.MANROPE_MEDIUM);
		int width = Math.max(1, columnWidth - 20);
		StringBuilder visible = new StringBuilder();
		StringBuilder line = new StringBuilder();
		int lines = 1;
		for (String word : valueOrEmpty(text).trim().split("\\s+")) {
			if (line.length() == 0 && metrics.stringWidth(word) > width) {
				StringBuilder shortened = new StringBuilder(word);
				while (shortened.length() > 0 && metrics.stringWidth(shortened + "…") > width)
					shortened.deleteCharAt(shortened.length() - 1);
				return visible.append(shortened).append('…').toString();
			}
			String candidate = line.length() == 0 ? word : line + " " + word;
			if (line.length() > 0 && metrics.stringWidth(candidate) > width) {
				if (lines == 2) {
					while (line.length() > 0 && metrics.stringWidth(line + "…") > width)
						line.deleteCharAt(line.length() - 1);
					return visible.append(line).append('…').toString();
				}
				visible.append(line).append('\n');
				line.setLength(0);
				lines++;
			}
			line.append(line.length() == 0 ? word : " " + word);
		}
		return visible.append(line).toString();
	}
	private final class ProductRenderer extends JPanel implements javax.swing.table.TableCellRenderer {
		private final JLabel reference = new JLabel();
		private final JTextArea name = new JTextArea();
		ProductRenderer() {
			super(new BorderLayout());
			reference.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(13f));
			name.setFont(RetailPOSTheme.MANROPE_MEDIUM);
			name.setLineWrap(false);
			// The renderer panel provides the cell padding for both lines.
			name.setMargin(new Insets(0, 0, 0, 0));
			name.setBorder(BorderFactory.createEmptyBorder());
			name.setEditable(false);
			name.setFocusable(false);
			add(reference, BorderLayout.NORTH);
			add(name, BorderLayout.CENTER);
		}
		@Override
		public void paint(java.awt.Graphics graphics) {
			doLayout();
			super.paint(graphics);
		}
		@Override
		public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
				boolean focused, int row, int column) {
			ReplenishmentEntry entry = entries.get(table.convertRowIndexToModel(row));
			java.awt.Color background = selected ? RetailPOSColors.surface200() : RetailPOSColors.surface100();
			setBackground(background);
			name.setBackground(background);
			name.setForeground(RetailPOSColors.ink());
			reference.setBackground(background);
			reference.setForeground(RetailPOSColors.inkMuted());
			reference.setText(displayReference(entry.name, entry.reference));
			reference.setVisible(entry.productId != null && !reference.getText().isBlank());
			name.setText(preview(
					entry.productId == null ? entry.displayName() : displayProductName(entry.name, entry.reference),
					table.getColumnModel().getColumn(column).getWidth()));
			setBorder(
					BorderFactory
							.createCompoundBorder(
									selected
											? BorderFactory.createMatteBorder(1, 0, 1, 0,
													RetailPOSColors.borderStrong())
											: BorderFactory.createEmptyBorder(1, 0, 1, 0),
									BorderFactory.createEmptyBorder(8, 8, 0, 4)));
			return this;
		}
	}
	private final class StatusBadgeRenderer extends DefaultTableCellRenderer {
		private java.awt.Color badgeColor;
		private java.awt.Color fillColor;
		@Override
		public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
				boolean focused, int row, int column) {
			super.getTableCellRendererComponent(table, value, selected, focused, row, column);
			String state = entries.get(table.convertRowIndexToModel(row)).status;
			badgeColor = statusColor(state);
			fillColor = badgeFill(state);
			setBackground(selected ? RetailPOSColors.surface200() : RetailPOSColors.surface100());
			setForeground("DELIVERED".equals(state) ? RetailPOSColors.inkMuted() : RetailPOSColors.ink());
			setVerticalAlignment(JLabel.TOP);
			setBorder(
					BorderFactory
							.createCompoundBorder(
									selected
											? BorderFactory.createMatteBorder(1, 0, 1, 0,
													RetailPOSColors.borderStrong())
											: BorderFactory.createEmptyBorder(1, 0, 1, 0),
									BorderFactory.createEmptyBorder(0, 12, 0, 4)));
			setOpaque(false);
			return this;
		}
		@Override
		protected void paintComponent(java.awt.Graphics graphics) {
			java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
			g.setColor(getBackground());
			g.fillRect(0, 0, getWidth(), getHeight());
			g.setColor(fillColor);
			int badgeWidth = Math.min(getWidth() - 8, getFontMetrics(getFont()).stringWidth(getText()) + 24);
			int badgeHeight = 28;
			int badgeY = 4;
			g.fillRoundRect(4, badgeY, badgeWidth, badgeHeight, badgeHeight, badgeHeight);
			g.setColor(badgeColor);
			g.drawRoundRect(4, badgeY, badgeWidth, badgeHeight, badgeHeight, badgeHeight);
			g.setColor(getForeground());
			java.awt.FontMetrics metrics = g.getFontMetrics(getFont());
			g.drawString(getText(), 16, badgeY + (badgeHeight - metrics.getHeight()) / 2 + metrics.getAscent());
			g.dispose();
		}
	}
	private static String valueOrEmpty(String value) {
		return value == null ? "" : value;
	}
	private void selectFormStatus() {
		formPending.setSelected("PENDING".equals(formStatus));
		formOrdered.setSelected("ORDERED".equals(formStatus));
		formReceived.setSelected("RECEIVED".equals(formStatus));
		formDelivered.setSelected("DELIVERED".equals(formStatus));
	}
	private void updateFormDeliveredEnabled() {
		formDelivered.setVisible(editingEntry != null);
		formDelivered.setEnabled(editingEntry != null && formCustomer != null
				&& ("RECEIVED".equals(editingEntry.status) || "DELIVERED".equals(editingEntry.status)));
		formDeliveryHint.setVisible(
				editingEntry != null && !formDelivered.isEnabled() && !"DELIVERED".equals(editingEntry.status));
		formStatusButtons.revalidate();
	}
	private static String customerDisplayName(CustomerInfo customer) {
		return customerDisplayName(customer, customer == null ? null : customer.getName());
	}
	private static String customerDisplayName(CustomerInfo customer, String name) {
		if (name != null && !name.isBlank())
			return name;
		return valueOrEmpty(customer == null ? null : customer.getId());
	}
	private String creatorName(ReplenishmentEntry entry) {
		if (entry.creatorName != null && !entry.creatorName.isBlank())
			return entry.creatorName;
		UserInfo seller = selectedSeller();
		return seller != null && seller.getId() != null && seller.getId().equals(entry.createdBy)
				? seller.getName()
				: "--";
	}
	/**
	 * Follow the viewport width while allowing the right form to scroll vertically.
	 */
	private static class FormFields extends JPanel implements javax.swing.Scrollable {
		FormFields() {
			super(new GridBagLayout());
		}
		@Override
		public java.awt.Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}
		@Override
		public int getScrollableUnitIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
			return 16;
		}
		@Override
		public int getScrollableBlockIncrement(java.awt.Rectangle visibleRect, int orientation, int direction) {
			return visibleRect.height - 16;
		}
		@Override
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}
		@Override
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
	}
	private void updateDeliveredEnabled() {
		if (delivered != null)
			delivered.setEnabled(
					selectedEntries().stream().anyMatch(e -> "RECEIVED".equals(e.status) && e.customerId != null));
	}
	private void change(String status) {
		List<ReplenishmentEntry> selected = selectedEntries();
		if (selected.isEmpty())
			return;
		if (selected.size() > 1 && save.isEnabled()) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.UnsavedBulkEdits"));
			return;
		}
		List<String> ids = new ArrayList<>();
		for (ReplenishmentEntry entry : selected)
			if (!"DELIVERED".equals(status) || ("RECEIVED".equals(entry.status) && entry.customerId != null))
				ids.add(entry.id);
		int skipped = selected.size() - ids.size();
		if (ids.isEmpty()) {
			JOptionPane.showMessageDialog(this,
					String.format(AppLocal.getIntString("Replenishment.SkippedDelivery"), skipped));
			return;
		}
		try {
			data.updateStatuses(ids, status, app.getAppUserView().getUser().getId());
			load(search.getText(), false, selected.size() == 1 ? ids.get(0) : null);
			if (selected.size() > 1 || skipped > 0)
				JOptionPane.showMessageDialog(this, String.format(AppLocal.getIntString("Replenishment.BulkResult"),
						ids.size(), statusLabel(status), skipped));
		} catch (BasicException e) {
			logStatusFailure("change", status, null, e);
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("Replenishment.StatusUpdateFailed"));
		}
	}
	private List<ReplenishmentEntry> selectedEntries() {
		List<ReplenishmentEntry> selected = new ArrayList<>();
		if (entries != null)
			for (int row : table.getSelectedRows())
				selected.add(entries.get(table.convertRowIndexToModel(row)));
		return selected;
	}
	private List<String> selectedIds() {
		List<String> ids = new ArrayList<>();
		for (ReplenishmentEntry entry : selectedEntries())
			ids.add(entry.id);
		return ids;
	}
	private void clearSelectedRows() {
		table.clearSelection();
	}
	private void clearSelectionForFilter() {
		if (table.getSelectedRowCount() > 1 && !save.isEnabled()) {
			clearForm();
			return;
		}
		clearSelectedRows();
	}
	private void updateSelectionView() {
		int count = table.getSelectedRowCount();
		if (detailCards == null)
			return;
		((CardLayout) detailCards.getLayout()).show(detailCards, count > 1 ? "selection" : "form");
		selectionSummary.setText(String.format(AppLocal.getIntString("Replenishment.SelectedCount"), count));
		pendingAction.setText(count > 1
				? AppLocal.getIntString("Replenishment.FormPending") + " (" + count + ")"
				: AppLocal.getIntString("Replenishment.FormPending"));
		orderedAction.setText(count > 1
				? AppLocal.getIntString("Replenishment.FormOrdered") + " (" + count + ")"
				: AppLocal.getIntString("Replenishment.FormOrdered"));
		receivedAction.setText(count > 1
				? AppLocal.getIntString("Replenishment.FormReceived") + " (" + count + ")"
				: AppLocal.getIntString("Replenishment.FormReceived"));
		deleteAction.setText(count > 1
				? AppLocal.getIntString("Replenishment.Delete") + " (" + count + ")"
				: AppLocal.getIntString("Replenishment.Delete"));
		delivered.setText(count > 1
				? AppLocal.getIntString("Replenishment.FormDelivered") + " (" + count + ")"
				: AppLocal.getIntString("Replenishment.FormDelivered"));
		updateDeliveredEnabled();
	}
	static void logStatusFailure(String action, String status, String id, BasicException failure) {
		Throwable cause = failure;
		while (cause.getCause() != null)
			cause = cause.getCause();
		String detail = "";
		if (cause instanceof SQLException) {
			SQLException sql = (SQLException) cause;
			detail = " sqlState=" + sql.getSQLState() + " errorCode=" + sql.getErrorCode();
			Matcher constraint = CONSTRAINT.matcher(sql.getMessage() == null ? "" : sql.getMessage());
			if (constraint.find())
				detail += " constraint=" + constraint.group(1);
		}
		logger.warning("event=replenishment_status_update_failed action=" + action + " status=" + status + " entryId="
				+ id + " cause=" + cause.getClass().getSimpleName() + detail);
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
			creatorLabel.setText(AppLocal.getIntString("Replenishment.CreatedBy") + ": " + selectedSellerName());
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

	/**
	 * Prepare a missing delivery-note item with the supplier recorded in its note.
	 */
	public void startManualEntry(String code, String note) {
		startManualEntry(code);
		formNoteArea.setText(note);
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
