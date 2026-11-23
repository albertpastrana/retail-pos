package com.openbravo.pos.customers;

import com.openbravo.basic.BasicException;
import com.formdev.flatlaf.FlatClientProperties;
import com.openbravo.data.gui.JConfirmationDialog;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppUserView;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.forms.JPrincipalApp;
import com.openbravo.pos.forms.JPanelView;
import com.openbravo.pos.forms.JRootApp;
import com.openbravo.pos.inventory.DataLogicReplenishment;
import com.openbravo.pos.inventory.ReplenishmentEntry;
import com.openbravo.pos.inventory.ReplenishmentPanel;
import com.openbravo.pos.sales.JPanelTicketEdits;
import com.openbravo.pos.sales.JPanelTicketSales;
import com.openbravo.pos.ticket.FindTicketsInfo;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Rectangle;
import java.text.Collator;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JToggleButton;
import javax.swing.JViewport;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.Scrollable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.Timer;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableRowSorter;
import javax.swing.RowSorter;

public class CustomersPanel extends JPanel implements JPanelView, BeanFactoryApp {
	private static String text(String key, Object... args) {
		return AppLocal.getIntString("customers.screen." + key, args);
	}
	private static JToggleButton filter(String label) {
		JToggleButton button = new JToggleButton(label);
		button.putClientProperty(FlatClientProperties.STYLE,
				"arc: 10; background: " + RetailPOSColors.toHex(RetailPOSColors.surface100()) + "; selectedBackground: "
						+ RetailPOSColors.toHex(RetailPOSColors.surface200()) + "; selectedForeground: "
						+ RetailPOSColors.toHex(RetailPOSColors.ink()));
		button.setMargin(new Insets(8, 12, 8, 12));
		button.setHorizontalTextPosition(SwingConstants.LEFT);
		button.setIconTextGap(8);
		button.setFont(RetailPOSTheme.MANROPE_MEDIUM);
		return button;
	}
	private static void updateFilterStyle(JToggleButton button) {
		button.setFont(button.isSelected() ? RetailPOSTheme.MANROPE_SEMIBOLD : RetailPOSTheme.MANROPE_MEDIUM);
	}
	private static final class CountIcon implements Icon {
		private final String count;
		CountIcon(long count) {
			this.count = Long.toString(count);
		}
		public int getIconWidth() {
			FontMetrics metrics = new JLabel().getFontMetrics(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(12f));
			return metrics.stringWidth(count);
		}
		public int getIconHeight() {
			return new JLabel().getFontMetrics(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(12f)).getHeight();
		}
		public void paintIcon(Component component, Graphics graphics, int x, int y) {
			Graphics copy = graphics.create();
			copy.setFont(RetailPOSTheme.PLEX_MONO_REGULAR.deriveFont(12f));
			copy.setColor(RetailPOSColors.inkMuted());
			copy.drawString(count, x, y + copy.getFontMetrics().getAscent());
			copy.dispose();
		}
	}
	private static final DecimalFormat MONEY = new DecimalFormat("#,##0.00",
			DecimalFormatSymbols.getInstance(Locale.forLanguageTag("ca")));
	private final CustomersTableModel model = new CustomersTableModel();
	private final JTable table = new JTable(model) {
		private int preferredColumnsWidth() {
			int width = 0;
			for (int column = 0; column < getColumnCount(); column++)
				width += getColumnModel().getColumn(column).getPreferredWidth();
			return width;
		}
		@Override
		public boolean getScrollableTracksViewportWidth() {
			return getParent() instanceof JViewport viewport && viewport.getWidth() >= preferredColumnsWidth();
		}
		@Override
		public void doLayout() {
			super.doLayout();
			int extra = getParent() instanceof JViewport viewport
					? Math.max(0, viewport.getWidth() - preferredColumnsWidth())
					: 0;
			for (int column = 0; column < getColumnCount(); column++) {
				javax.swing.table.TableColumn cell = getColumnModel().getColumn(column);
				cell.setWidth(cell.getPreferredWidth() + (column == 0 ? extra : 0));
			}
		}
		@Override
		public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
			Component cell = super.prepareRenderer(renderer, row, column);
			if (!isRowSelected(row))
				cell.setBackground(RetailPOSColors.surface100());
			return cell;
		}
	};
	private final TableRowSorter<CustomersTableModel> sorter = new TableRowSorter<CustomersTableModel>(model);
	private final JTextField search = new JTextField();
	private final JToggleButton debtOnly = filter(text("debtFilter"));
	private final JToggleButton orderOnly = filter(text("orderFilter"));
	private final JToggleButton inactive = filter(text("inactiveFilter"));
	private final JTextField name = new JTextField();
	private final JTextField phone = new JTextField();
	private final JTextField maxDebt = new JTextField();
	private final JTextArea notes = new JTextArea(3, 20);
	private final JLabel detailTitle = new JLabel();
	private final JLabel detailStatus = new JLabel();
	private final JLabel debtValue = new JLabel();
	private final JLabel orderValue = new JLabel();
	private final JLabel lastValue = new JLabel();
	private final JButton newSale = new JButton(text("newSale"));
	private final JButton newOrder = new JButton();
	private final JTabbedPane tabs = new JTabbedPane();
	private final JPanel summary = new JPanel(new BorderLayout(0, 12));
	private final JPanel ordersPanel = new JPanel(new BorderLayout(0, 12));
	private final TicketsModel ticketsModel = new TicketsModel();
	private final JTable ticketsTable = ticketTable(ticketsModel);
	private final TicketsModel recentModel = new TicketsModel();
	private final JTable recentTable = ticketTable(recentModel);
	private final Map<String, List<FindTicketsInfo>> ticketHistory = new HashMap<>();
	private final Map<String, List<ReplenishmentEntry>> orderHistory = new HashMap<>();
	private List<CustomerInfoExt> allCustomersSnapshot;
	private long loadSequence;
	private final JLabel state = new JLabel(" ");
	private final JLabel count = new JLabel();
	private final JLabel listCount = new JLabel();
	private final JLabel totalDebt = new JLabel();
	private final JButton save = new JButton(text("save"));
	private final JButton cancel = new JButton(text("cancel"));
	private final JButton archive = new JButton(text("archive"));
	private final JButton newCustomer = new JButton(text("newCustomer"));
	private DataLogicCustomers customers;
	private DataLogicSales sales;
	private DataLogicReplenishment replenishment;
	private AppView app;
	private CustomerInfoExt selected;
	private boolean creating;
	private boolean saving;
	private boolean loading;
	private boolean resettingFilters;
	private String originalName = "", originalPhone = "", originalNotes = "";
	private boolean originalVisible;
	private double originalMaxDebt;
	private final Timer searchTimer;

	public CustomersPanel() {
		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 18, 8));
		add(buildHeader(), BorderLayout.NORTH);
		add(buildBody(), BorderLayout.CENTER);
		search.putClientProperty(FlatClientProperties.PLACEHOLDER_TEXT, text("search"));
		search.putClientProperty(FlatClientProperties.STYLE, "focusWidth: 1");
		searchTimer = new Timer(200, e -> loadCustomers());
		searchTimer.setRepeats(false);
		search.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				searchTimer.restart();
				updateActions();
			}
			public void removeUpdate(DocumentEvent e) {
				searchTimer.restart();
				updateActions();
			}
			public void changedUpdate(DocumentEvent e) {
				searchTimer.restart();
				updateActions();
			}
		});
		DocumentListener dirty = new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				updateActions();
			}
			public void removeUpdate(DocumentEvent e) {
				updateActions();
			}
			public void changedUpdate(DocumentEvent e) {
				updateActions();
			}
		};
		name.getDocument().addDocumentListener(dirty);
		phone.getDocument().addDocumentListener(dirty);
		maxDebt.getDocument().addDocumentListener(dirty);
		notes.getDocument().addDocumentListener(dirty);
		phone.setFont(RetailPOSTheme.PLEX_MONO_REGULAR);
		maxDebt.setFont(RetailPOSTheme.PLEX_MONO_REGULAR);
		newCustomer.addActionListener(e -> startNewCustomer());
		debtOnly.addActionListener(e -> {
			updateFilterStyle(debtOnly);
			if (!resettingFilters)
				loadCustomers();
		});
		orderOnly.addActionListener(e -> {
			updateFilterStyle(orderOnly);
			if (!resettingFilters)
				loadCustomers();
		});
		inactive.addActionListener(e -> {
			updateFilterStyle(inactive);
			if (!resettingFilters)
				loadCustomers();
		});
		save.addActionListener(e -> saveCustomer());
		cancel.addActionListener(e -> cancelEdit());
		archive.addActionListener(e -> toggleArchive());
		newSale.addActionListener(e -> {
			if (selected != null) {
				String id = selected.getId();
				openSalesTask(JPanelTicketSales.class, () -> JPanelTicketSales.requestCustomer(id));
			}
		});
		newOrder.addActionListener(e -> {
			if (selected == null || app == null)
				return;
			String customerId = selected.getId();
			AppUserView destination = app.getAppUserView();
			destination.showTask(ReplenishmentPanel.class.getName(), ReplenishmentPanel.class);
			if (destination instanceof JPrincipalApp principal
					&& principal.isCurrentTask(ReplenishmentPanel.class.getName())) {
				try {
					app.getBean(ReplenishmentPanel.class).startCustomerOrder(customerId);
				} catch (BasicException | BeanFactoryException ex) {
					JOptionPane.showMessageDialog(this, text("loadError"), getTitle(), JOptionPane.WARNING_MESSAGE);
				}
			}
		});
		installActions();
		setDetailEnabled(false);
	}

	private JPanel buildHeader() {
		JPanel header = new JPanel(new BorderLayout(18, 0));
		header.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, RetailPOSColors.border()));
		count.setForeground(RetailPOSColors.inkMuted());
		header.add(count, BorderLayout.WEST);
		JPanel debt = new JPanel(new FlowLayout(FlowLayout.TRAILING, 8, 0));
		debt.add(new JLabel(text("totalDebt")));
		totalDebt.setFont(totalDebt.getFont().deriveFont(Font.BOLD, 20f));
		totalDebt.setForeground(RetailPOSColors.ink());
		debt.add(totalDebt);
		header.add(debt, BorderLayout.EAST);
		return header;
	}

	private JPanel buildBody() {
		JPanel left = new JPanel(new BorderLayout(0, 6));
		JPanel tools = new JPanel(new BorderLayout(0, 4));
		JPanel searchRow = new JPanel(new BorderLayout(8, 0));
		searchRow.add(search, BorderLayout.CENTER);
		newCustomer.setMargin(new Insets(9, 14, 9, 14));
		newCustomer.setBackground(RetailPOSColors.surface100());
		searchRow.add(newCustomer, BorderLayout.EAST);
		tools.add(searchRow, BorderLayout.NORTH);
		JPanel filters = new JPanel(new FlowLayout(FlowLayout.LEADING, 8, 0));
		filters.add(debtOnly);
		filters.add(orderOnly);
		filters.add(inactive);
		tools.add(filters, BorderLayout.SOUTH);
		left.add(tools, BorderLayout.NORTH);
		table.setRowSorter(sorter);
		sorter.setComparator(0, Collator.getInstance(new Locale("ca")));
		sorter.setSortKeys(Collections.singletonList(new RowSorter.SortKey(0, javax.swing.SortOrder.ASCENDING)));
		table.setAutoCreateRowSorter(false);
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(table.getFontMetrics(table.getFont()).getHeight() + 16);
		table.setShowVerticalLines(false);
		table.setShowHorizontalLines(true);
		table.setGridColor(RetailPOSColors.border());
		table.setBackground(RetailPOSColors.surface100());
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		updateColumnWidths();
		table.setFillsViewportHeight(true);
		table.getSelectionModel().addListSelectionListener(e -> {
			if (!e.getValueIsAdjusting() && !loading)
				selectionChanged();
		});
		table.getColumnModel().getColumn(4).setCellRenderer(new DebtRenderer());
		table.getColumnModel().getColumn(0).setCellRenderer(new NameRenderer());
		table.getColumnModel().getColumn(2).setCellRenderer(new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
					boolean focused, int row, int column) {
				super.getTableCellRendererComponent(source, value, selected, focused, row, column);
				setHorizontalAlignment(SwingConstants.CENTER);
				setText(value instanceof Number count && count.longValue() > 0 ? count.toString() : "");
				setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
				return this;
			}
		});
		table.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
					boolean focused, int row, int column) {
				super.getTableCellRendererComponent(source, value, selected, focused, row, column);
				setHorizontalAlignment(SwingConstants.LEFT);
				setText(value instanceof java.util.Date date ? relative(date) : "");
				setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
				return this;
			}
		});
		DefaultTableCellRenderer phoneRenderer = new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r,
					int c) {
				super.getTableCellRendererComponent(t, v, sel, focus, r, c);
				setFont(RetailPOSTheme.PLEX_MONO_REGULAR);
				setHorizontalAlignment(SwingConstants.LEFT);
				setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
				return this;
			}
		};
		table.getColumnModel().getColumn(1).setCellRenderer(phoneRenderer);
		TableCellRenderer defaultHeader = table.getTableHeader().getDefaultRenderer();
		for (int column = 0; column < table.getColumnCount(); column++) {
			int alignment = column == 4
					? SwingConstants.RIGHT
					: column == 2 ? SwingConstants.CENTER : SwingConstants.LEFT;
			table.getColumnModel().getColumn(column)
					.setHeaderRenderer((source, value, selected, focused, row, viewColumn) -> {
						Component component = defaultHeader.getTableCellRendererComponent(source, value, selected,
								focused, row, viewColumn);
						if (component instanceof JLabel label) {
							label.setHorizontalAlignment(alignment);
							label.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
							label.setForeground(RetailPOSColors.inkMuted());
							label.setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
						}
						return component;
					});
		}
		JScrollPane tableScroll = new JScrollPane(table);
		tableScroll.getViewport().setBackground(RetailPOSColors.surface100());
		left.add(tableScroll, BorderLayout.CENTER);
		JPanel footer = new JPanel(new BorderLayout());
		footer.setBorder(BorderFactory.createEmptyBorder(5, 8, 5, 8));
		footer.add(listCount, BorderLayout.WEST);
		footer.add(new JLabel(text("shortcuts")), BorderLayout.EAST);
		left.add(footer, BorderLayout.SOUTH);
		search.requestFocusInWindow();

		JPanel detail = new JPanel(new BorderLayout());
		detail.setBorder(BorderFactory.createLineBorder(RetailPOSColors.border()));
		JPanel detailHead = new JPanel(new BorderLayout(8, 8));
		detailHead.setBorder(BorderFactory.createEmptyBorder(14, 14, 12, 14));
		detailTitle.setFont(detailTitle.getFont().deriveFont(Font.BOLD, 20f));
		detailStatus.setForeground(RetailPOSColors.inkMuted());
		detailStatus.setFont(RetailPOSTheme.PLEX_MONO_REGULAR);
		JPanel identity = new JPanel(new GridLayout(2, 1));
		identity.add(detailTitle);
		identity.add(detailStatus);
		detailHead.add(identity, BorderLayout.CENTER);
		RetailPOSColors.primaryButton(newSale);
		newSale.setMargin(new Insets(12, 12, 12, 12));
		detailHead.add(newSale, BorderLayout.EAST);
		JPanel metrics = new JPanel(new GridLayout(1, 3, 8, 0));
		metrics.add(metric(text("debt"), debtValue));
		metrics.add(metric(text("pendingMetric"), orderValue));
		metrics.add(metric(text("lastPurchase"), lastValue));
		lastValue.setFont(RetailPOSTheme.MANROPE_SEMIBOLD.deriveFont(18f));
		detailHead.add(metrics, BorderLayout.SOUTH);
		detail.add(detailHead, BorderLayout.NORTH);
		JPanel content = new ScrollableForm();
		content.setBorder(BorderFactory.createEmptyBorder(6, 14, 8, 14));
		GridBagConstraints f = new GridBagConstraints();
		f.gridx = 0;
		f.weightx = 1;
		f.fill = GridBagConstraints.HORIZONTAL;
		f.anchor = GridBagConstraints.NORTHWEST;
		f.insets = new Insets(0, 0, 5, 0);
		addField(content, f, 0, text("name"), name);
		JPanel contact = new JPanel(new GridLayout(1, 2, 8, 0));
		contact.add(fieldWithLabel(text("phone"), phone));
		contact.add(fieldWithLabel(text("allowedDebt"), maxDebt));
		f.gridy = 2;
		content.add(contact, f);
		f.gridy = 3;
		content.add(fieldCaption(text("notes")), f);
		f.gridy = 4;
		JScrollPane notesScroll = new JScrollPane(notes);
		notesScroll.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		// Preserve the three content-derived rows when GridBagLayout narrows the form.
		notesScroll.setMinimumSize(new Dimension(0, notesScroll.getPreferredSize().height));
		content.add(notesScroll, f);
		f.gridy = 5;
		content.add(buildActions(), f);
		f.gridy = 6;
		content.add(state, f);
		JScrollPane detailScroll = new JScrollPane(content, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		detailScroll.setBorder(null);
		JScrollPane summaryScroll = new JScrollPane(summary);
		summaryScroll.setBorder(null);
		tabs.addTab(text("summary"), summaryScroll);
		configureTickets(recentTable);
		configureTickets(ticketsTable);
		JPanel purchases = new JPanel(new BorderLayout(0, 8));
		purchases.add(sectionLabel("purchaseHistory"), BorderLayout.NORTH);
		JScrollPane purchasesScroll = new JScrollPane(ticketsTable);
		purchasesScroll.setBorder(null);
		purchasesScroll.getViewport().setBackground(RetailPOSColors.surface100());
		purchases.add(purchasesScroll, BorderLayout.CENTER);
		JLabel openHint = new JLabel(text("openTicketHint"));
		openHint.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		openHint.setForeground(RetailPOSColors.inkMuted());
		openHint.setBorder(BorderFactory.createEmptyBorder(4, 0, 0, 0));
		purchases.add(openHint, BorderLayout.SOUTH);
		tabs.addTab(text("purchases"), purchases);
		JPanel orderTab = new JPanel(new BorderLayout(0, 8));
		JScrollPane ordersScroll = new JScrollPane(ordersPanel);
		ordersScroll.setBorder(null);
		orderTab.add(ordersScroll, BorderLayout.CENTER);
		orderTab.add(newOrder, BorderLayout.SOUTH);
		tabs.addTab(text("orders"), orderTab);
		tabs.addTab(text("data"), detailScroll);
		tabs.addChangeListener(e -> {
			if (tabs.getSelectedIndex() == 3)
				SwingUtilities.invokeLater(() -> {
					if (tabs.getSelectedIndex() == 3 && name.isEnabled()) {
						detailScroll.getViewport().setViewPosition(new java.awt.Point(0, 0));
						name.requestFocusInWindow();
					}
				});
		});
		tabs.setBorder(BorderFactory.createEmptyBorder(0, 14, 8, 14));
		detail.add(tabs, BorderLayout.CENTER);
		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left, detail);
		// Reserve a usable detail width at the till; share additional space.
		Dimension detailMinimum = detail.getMinimumSize();
		detail.setMinimumSize(new Dimension(Math.max(detailMinimum.width, com.formdev.flatlaf.util.UIScale.scale(440)),
				detailMinimum.height));
		split.setResizeWeight(0.6);
		return panelWith(split);
	}
	private void updateColumnWidths() {
		for (int column = 0; column < table.getColumnCount(); column++) {
			int width = table.getTableHeader().getFontMetrics(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f))
					.stringWidth(table.getColumnName(column)) + 16;
			Font font = column == 1
					? RetailPOSTheme.PLEX_MONO_REGULAR
					: column == 4 ? RetailPOSTheme.PLEX_MONO_SEMIBOLD : table.getFont();
			if (column == 0) {
				for (int row = 0; row < model.getRowCount(); row++)
					width = Math.max(width,
							table.getFontMetrics(font).stringWidth(model.getValueAt(row, column).toString()) + 16);
			} else {
				String example = switch (column) {
					case 1 -> "000 000 000";
					case 2 -> "99";
					case 3 -> text("daysAgo", 100);
					default -> MONEY.format(9999.99);
				};
				width = Math.max(width, table.getFontMetrics(font).stringWidth(example) + 16);
			}
			javax.swing.table.TableColumn cell = table.getColumnModel().getColumn(column);
			cell.setMinWidth(0);
			cell.setMaxWidth(column == 0 ? Integer.MAX_VALUE : width);
			cell.setPreferredWidth(width);
			cell.setMinWidth(width);
		}
		table.revalidate();
	}

	private JPanel panelWith(Component component) {
		JPanel p = new JPanel(new BorderLayout());
		p.add(component);
		return p;
	}
	private void addField(JPanel p, GridBagConstraints c, int row, String label, JComponent field) {
		c.gridy = row;
		p.add(fieldCaption(label), c);
		c.gridy = row + 1;
		p.add(field, c);
	}
	private JLabel fieldCaption(String label) {
		JLabel caption = new JLabel(label);
		caption.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		caption.setForeground(RetailPOSColors.inkMuted());
		return caption;
	}
	private JPanel fieldWithLabel(String label, JComponent field) {
		JPanel panel = new JPanel(new BorderLayout(0, 5));
		panel.add(fieldCaption(label), BorderLayout.NORTH);
		panel.add(field, BorderLayout.CENTER);
		return panel;
	}
	private JPanel metric(String caption, JLabel value) {
		JPanel p = new JPanel(new BorderLayout(0, 2));
		p.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(RetailPOSColors.border()),
				BorderFactory.createEmptyBorder(6, 10, 6, 10)));
		JLabel label = new JLabel(caption);
		label.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		label.setForeground(RetailPOSColors.inkMuted());
		p.add(label, BorderLayout.NORTH);
		value.setFont(RetailPOSTheme.PLEX_MONO_SEMIBOLD.deriveFont(18f));
		value.setVerticalAlignment(SwingConstants.TOP);
		p.add(value, BorderLayout.CENTER);
		return p;
	}
	private JLabel sectionLabel(String key) {
		JLabel label = new JLabel(text(key).toUpperCase(Locale.getDefault()));
		label.setFont(RetailPOSTheme.MANROPE_SEMIBOLD.deriveFont(12f));
		label.setForeground(RetailPOSColors.inkMuted());
		return label;
	}
	private JPanel buildActions() {
		JPanel actions = new JPanel(new BorderLayout(8, 0));
		JPanel edit = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
		save.setMargin(new Insets(8, 10, 8, 10));
		cancel.setMargin(new Insets(8, 10, 8, 10));
		RetailPOSColors.primaryButton(save);
		RetailPOSTheme.touchHeight(save, 48);
		RetailPOSTheme.touchHeight(cancel, 48);
		edit.add(save);
		edit.add(javax.swing.Box.createHorizontalStrut(8));
		edit.add(cancel);
		actions.add(edit, BorderLayout.WEST);
		archive.setBorderPainted(false);
		archive.setContentAreaFilled(false);
		archive.setBorder(BorderFactory.createEmptyBorder(8, 0, 8, 0));
		archive.setHorizontalAlignment(SwingConstants.RIGHT);
		archive.setForeground(RetailPOSColors.dangerText());
		actions.add(archive, BorderLayout.EAST);
		return actions;
	}
	private static JTable ticketTable(TicketsModel model) {
		return new JTable(model) {
			private int preferredColumnsWidth() {
				int width = 0;
				for (int column = 0; column < getColumnCount(); column++)
					width += getColumnModel().getColumn(column).getPreferredWidth();
				return width;
			}
			@Override
			public boolean getScrollableTracksViewportWidth() {
				return getParent() instanceof JViewport viewport && viewport.getWidth() >= preferredColumnsWidth();
			}
			@Override
			public void doLayout() {
				super.doLayout();
				int extra = getParent() instanceof JViewport viewport
						? Math.max(0, viewport.getWidth() - preferredColumnsWidth())
						: 0;
				for (int column = 0; column < getColumnCount(); column++) {
					javax.swing.table.TableColumn cell = getColumnModel().getColumn(column);
					cell.setWidth(cell.getPreferredWidth() + (column == 2 ? extra : 0));
				}
			}
			@Override
			public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
				Component cell = super.prepareRenderer(renderer, row, column);
				if (!isRowSelected(row))
					cell.setBackground(RetailPOSColors.surface100());
				return cell;
			}
		};
	}
	private void configureTickets(JTable list) {
		list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		list.setRowHeight(list.getFontMetrics(list.getFont()).getHeight() + 16);
		list.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		list.setBackground(RetailPOSColors.surface100());
		list.setShowVerticalLines(false);
		list.setShowHorizontalLines(true);
		list.setGridColor(RetailPOSColors.border());
		list.setFillsViewportHeight(true);
		String[] examples = {"00/00/0000", "#000000", AppLocal.getIntString("label.paymentmixed"), "0.000,00 €"};
		for (int column = 0; column < examples.length; column++) {
			final int index = column;
			Font font = column == 2 ? RetailPOSTheme.MANROPE_MEDIUM : RetailPOSTheme.PLEX_MONO_REGULAR;
			int width = Math.max(list.getFontMetrics(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f))
					.stringWidth(list.getColumnName(column)), list.getFontMetrics(font).stringWidth(examples[column]))
					+ 16;
			javax.swing.table.TableColumn cell = list.getColumnModel().getColumn(column);
			cell.setPreferredWidth(width);
			if (column != 2) {
				cell.setMinWidth(width);
				cell.setMaxWidth(width);
			}
			cell.setCellRenderer(new DefaultTableCellRenderer() {
				@Override
				public Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
						boolean focused, int row, int viewColumn) {
					super.getTableCellRendererComponent(source, value, selected, focused, row, viewColumn);
					setHorizontalAlignment(index == 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
					setFont(index == 2 ? RetailPOSTheme.MANROPE_MEDIUM : RetailPOSTheme.PLEX_MONO_REGULAR);
					setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
					return this;
				}
			});
			cell.setHeaderRenderer(new DefaultTableCellRenderer() {
				@Override
				public Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
						boolean focused, int row, int viewColumn) {
					super.getTableCellRendererComponent(source, value, selected, focused, row, viewColumn);
					setHorizontalAlignment(index == 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
					setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
					setForeground(RetailPOSColors.inkMuted());
					setBackground(RetailPOSColors.surface100());
					setBorder(BorderFactory.createCompoundBorder(
							BorderFactory.createMatteBorder(0, 0, 1, 0, RetailPOSColors.border()),
							BorderFactory.createEmptyBorder(0, 4, 0, 4)));
					return this;
				}
			});
		}
		list.getInputMap(WHEN_FOCUSED).put(javax.swing.KeyStroke.getKeyStroke("ENTER"), "openTicket");
		list.getActionMap().put("openTicket", new AbstractAction() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				openTicket(list);
			}
		});
		list.addMouseListener(new java.awt.event.MouseAdapter() {
			public void mouseClicked(java.awt.event.MouseEvent e) {
				if (e.getClickCount() == 2)
					openTicket(list);
			}
		});
	}
	private void openTicket(JTable list) {
		if (list.getSelectedRow() < 0)
			return;
		FindTicketsInfo ticket = ((TicketsModel) list.getModel()).tickets.get(list.getSelectedRow());
		openSalesTask(JPanelTicketEdits.class,
				() -> JPanelTicketEdits.requestReceiptBarcode("T" + ticket.getTicketId()));
	}
	private void openSalesTask(Class<? extends JPanelView> view, Runnable prepare) {
		if (app == null || app.getAppUserView() == null)
			return;
		AppUserView current = app.getAppUserView();
		if (current.getUser().isAdministrationMode()) {
			if (!(app instanceof JRootApp root))
				return;
			// Switching modes deactivates this view and handles unsaved changes.
			prepare.run();
			root.openSalesView();
			if (app.getAppUserView() == current) {
				clearSalesRequest(view);
				return;
			}
		} else {
			if (!confirmDiscard())
				return;
			prepare.run();
		}
		AppUserView destination = app.getAppUserView();
		if (destination != null) {
			destination.showTask(view.getName(), view);
			if (destination instanceof JPrincipalApp principal) {
				try {
					if (!principal.isCurrentTask(view.getName()))
						return;
					if (view == JPanelTicketSales.class)
						app.getBean(JPanelTicketSales.class).applyPendingCustomer();
					else
						app.getBean(JPanelTicketEdits.class).openRequestedReceipt();
				} catch (BasicException | BeanFactoryException ex) {
					JOptionPane.showMessageDialog(this, AppLocal.getIntString("customer.loaderror"), getTitle(),
							JOptionPane.WARNING_MESSAGE);
				} finally {
					clearSalesRequest(view);
				}
			}
		} else
			clearSalesRequest(view);
	}
	private void clearSalesRequest(Class<? extends JPanelView> view) {
		if (view == JPanelTicketSales.class)
			JPanelTicketSales.requestCustomer(null);
		else
			JPanelTicketEdits.requestReceiptBarcode(null);
	}
	private void populateActivity(CustomerInfoExt customer) {
		List<FindTicketsInfo> tickets = ticketHistory.getOrDefault(customer.getId(), Collections.emptyList());
		List<ReplenishmentEntry> orders = orderHistory.getOrDefault(customer.getId(), Collections.emptyList());
		long pending = orders.stream().filter(o -> !"DELIVERED".equals(o.status)).count();
		orderValue.setText(Long.toString(pending));
		lastValue.setText(tickets.isEmpty() ? text("none") : relative(tickets.get(0).getDate()));
		ticketsModel.setTickets(tickets);
		if (tickets.isEmpty())
			ticketsTable.clearSelection();
		else
			ticketsTable.setRowSelectionInterval(0, 0);
		recentModel.setTickets(tickets.subList(0, Math.min(3, tickets.size())));
		JPanel recent = new JPanel(new GridBagLayout());
		GridBagConstraints cell = new GridBagConstraints();
		cell.gridx = 0;
		cell.weightx = 1;
		cell.anchor = GridBagConstraints.NORTHWEST;
		cell.fill = GridBagConstraints.HORIZONTAL;
		cell.insets = new Insets(0, 0, 5, 0);
		recent.add(sectionLabel("recentPurchases"), cell);
		JScrollPane recentScroll = new JScrollPane(recentTable);
		recentScroll.setBorder(null);
		recentScroll.setPreferredSize(new Dimension(recentTable.getPreferredSize().width,
				recentTable.getTableHeader().getPreferredSize().height
						+ recentTable.getRowHeight() * recentModel.getRowCount() + 3));
		cell.gridy = 1;
		recent.add(tickets.isEmpty() ? new JLabel(text("noPurchases")) : recentScroll, cell);
		cell.gridy = 2;
		cell.weighty = 1;
		recent.add(new JPanel(), cell);
		JPanel pendingPanel = orderCards(orders, true);
		summary.removeAll();
		summary.add(pendingPanel, BorderLayout.NORTH);
		summary.add(recent, BorderLayout.CENTER);
		ordersPanel.removeAll();
		ordersPanel.add(orderCards(orders, false), BorderLayout.NORTH);
		newOrder.setText(text("newOrder", customer.getName()));
		summary.revalidate();
		ordersPanel.revalidate();
		summary.repaint();
		ordersPanel.repaint();
		// An old card may extend below the new panel height. Repaint the viewport
		// too so its exposed area cannot retain pixels from the previous client.
		if (summary.getParent() instanceof JViewport viewport)
			viewport.repaint();
		if (ordersPanel.getParent() instanceof JViewport viewport)
			viewport.repaint();
	}
	private JPanel orderCards(List<ReplenishmentEntry> orders, boolean pendingOnly) {
		JPanel list = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.insets = new Insets(0, 0, 8, 0);
		list.add(sectionLabel(pendingOnly ? "pendingOrders" : "orders"), c);
		int row = 1;
		for (ReplenishmentEntry order : orders) {
			if (pendingOnly && "DELIVERED".equals(order.status))
				continue;
			c.gridy = row++;
			JPanel card = new JPanel(new BorderLayout(8, 4));
			card.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(RetailPOSColors.border()),
					BorderFactory.createEmptyBorder(8, 10, 8, 10)));
			String statusText = "● " + text("status." + order.status);
			if ("RECEIVED".equals(order.status) && order.receivedAt != null)
				statusText += " · " + ReplenishmentPanel.pickupWait(order.receivedAt);
			JLabel status = new JLabel(statusText);
			status.setForeground("RECEIVED".equals(order.status) || "DELIVERED".equals(order.status)
					? RetailPOSColors.success()
					: RetailPOSColors.warning());
			card.add(new JLabel(order.productId == null
					? value(order.manualDescription)
					: ReplenishmentPanel.displayProductName(order.name, order.reference)), BorderLayout.NORTH);
			card.add(status, BorderLayout.CENTER);
			list.add(card, c);
		}
		if (row == 1) {
			c.gridy = row;
			list.add(new JLabel(text("noOrders")), c);
		}
		return list;
	}
	private static String relative(java.util.Date date) {
		long days = Math.max(0, ChronoUnit.DAYS.between(date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate(),
				LocalDate.now()));
		if (days == 0)
			return text("today");
		if (days == 1)
			return text("yesterday");
		return text("daysAgo", days);
	}
	private static class TicketsModel extends AbstractTableModel {
		private List<FindTicketsInfo> tickets = Collections.emptyList();
		void setTickets(List<FindTicketsInfo> values) {
			tickets = values;
			fireTableDataChanged();
		}
		public int getRowCount() {
			return tickets.size();
		}
		public int getColumnCount() {
			return 4;
		}
		public String getColumnName(int column) {
			return text(new String[]{"date", "ticket", "payment", "amount"}[column]);
		}
		public Object getValueAt(int row, int column) {
			FindTicketsInfo ticket = tickets.get(row);
			return switch (column) {
				case 0 -> new SimpleDateFormat("dd/MM/yy").format(ticket.getDate());
				case 1 -> "#" + ticket.getTicketId();
				case 2 -> ticket.hasMultiplePayments()
						? AppLocal.getIntString("label.paymentmixed")
						: ticket.getPayment() == null
								? ""
								: com.openbravo.pos.payment.ExternalVoucher.PAYMENT.equals(ticket.getPayment())
										? com.openbravo.pos.payment.ExternalVoucher.name(ticket.getPaymentLabel())
										: AppLocal.getIntString("transpayment." + ticket.getPayment());
				default -> MONEY.format(ticket.getTotal()) + " €";
			};
		}
	}
	private static class ScrollableForm extends JPanel implements Scrollable {
		ScrollableForm() {
			super(new GridBagLayout());
		}
		public Dimension getPreferredScrollableViewportSize() {
			return getPreferredSize();
		}
		public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
			return getFontMetrics(getFont()).getHeight();
		}
		public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
			return visibleRect.height - getScrollableUnitIncrement(visibleRect, orientation, direction);
		}
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}
	}

	private void installActions() {
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("control N"), "new");
		getActionMap().put("new", new AbstractAction() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				startNewCustomer();
			}
		});
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("control S"), "save");
		getActionMap().put("save", new AbstractAction() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				saveCustomer();
			}
		});
		getInputMap(WHEN_IN_FOCUSED_WINDOW).put(javax.swing.KeyStroke.getKeyStroke("ESCAPE"), "cancel");
		getActionMap().put("cancel", new AbstractAction() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				cancelEdit();
			}
		});
		table.getInputMap(WHEN_FOCUSED).put(javax.swing.KeyStroke.getKeyStroke("ENTER"), "edit");
		table.getActionMap().put("edit", new AbstractAction() {
			public void actionPerformed(java.awt.event.ActionEvent e) {
				tabs.setSelectedIndex(3);
				name.requestFocusInWindow();
			}
		});
	}

	public void init(AppView app) throws BeanFactoryException {
		this.app = app;
		customers = app.getBean(DataLogicCustomers.class);
		sales = app.getBean(DataLogicSales.class);
		replenishment = app.getBean(DataLogicReplenishment.class);
	}
	public Object getBean() {
		return this;
	}
	public String getTitle() {
		return AppLocal.getIntString("Menu.CustomersManagement");
	}
	public JComponent getComponent() {
		return this;
	}
	public void activate() throws BasicException {
		resetFilters();
		allCustomersSnapshot = null;
		loadCustomers();
		SwingUtilities.invokeLater(() -> search.requestFocusInWindow());
	}

	private void resetFilters() {
		resettingFilters = true;
		search.setText("");
		debtOnly.setSelected(false);
		orderOnly.setSelected(false);
		inactive.setSelected(false);
		resettingFilters = false;
		searchTimer.stop();
	}
	public boolean deactivate() {
		return !saving && confirmDiscard();
	}

	private void loadCustomers() {
		if (customers == null)
			return;
		loading = true;
		final long request = ++loadSequence;
		final String query = search.getText();
		final boolean debt = debtOnly.isSelected(), showInactive = inactive.isSelected(),
				withOrders = orderOnly.isSelected();
		final List<CustomerInfoExt> cachedAll = allCustomersSnapshot;
		final Map<String, List<FindTicketsInfo>> cachedTickets = cachedAll == null
				? Collections.emptyMap()
				: new HashMap<>(ticketHistory);
		final Map<String, List<ReplenishmentEntry>> cachedOrders = cachedAll == null
				? Collections.emptyMap()
				: new HashMap<>(orderHistory);
		new SwingWorker<List<CustomerInfoExt>, Void>() {
			private List<CustomerInfoExt> all;
			private Map<String, List<FindTicketsInfo>> histories = cachedTickets;
			private Map<String, List<ReplenishmentEntry>> requests = cachedOrders;
			protected List<CustomerInfoExt> doInBackground() throws Exception {
				all = cachedAll;
				if (all == null) {
					all = customers.searchCustomerSummaries("", false, true);
					requests = new HashMap<>();
					for (ReplenishmentEntry order : replenishment.list("", "ENCARGO", "ALL"))
						requests.computeIfAbsent(order.customerId, id -> new ArrayList<>()).add(order);
					histories = sales.getCustomerTicketHistory();
				}
				List<CustomerInfoExt> result = customers.searchCustomerSummaries(query, debt, showInactive);
				if (withOrders)
					result.removeIf(customer -> requests.getOrDefault(customer.getId(), Collections.emptyList())
							.stream().noneMatch(order -> !"DELIVERED".equals(order.status)));
				return result;
			}
			protected void done() {
				if (request != loadSequence)
					return;
				try {
					List<CustomerInfoExt> result = get();
					allCustomersSnapshot = all;
					ticketHistory.clear();
					ticketHistory.putAll(histories);
					orderHistory.clear();
					orderHistory.putAll(requests);
					model.setCustomers(result);
					updateColumnWidths();
					int active = 0;
					double total = 0;
					for (CustomerInfoExt c : all) {
						if (c.isVisible())
							active++;
						if (c.getCurdebt() != null)
							total += c.getCurdebt();
					}
					count.setText(text("activeCount", active));
					long debts = all.stream().filter(c -> c.getCurdebt() != null && c.getCurdebt() > 0).count();
					long pending = all.stream().filter(c -> requests.getOrDefault(c.getId(), Collections.emptyList())
							.stream().anyMatch(o -> !"DELIVERED".equals(o.status))).count();
					debtOnly.setIcon(new CountIcon(debts));
					orderOnly.setIcon(new CountIcon(pending));
					totalDebt.setText(MONEY.format(total) + " €");
					totalDebt.setForeground(total > 0 ? RetailPOSColors.dangerText() : RetailPOSColors.ink());
					listCount.setText(text("listCount", result.size(), all.size()));
					if (result.isEmpty()) {
						if (creating || isDirty())
							table.clearSelection();
						else
							clearDetail();
					} else if (creating || isDirty()) {
						if (selected == null || !selectId(selected.getId()))
							table.clearSelection();
					} else {
						if (selected == null || !selectId(selected.getId()))
							table.setRowSelectionInterval(0, 0);
						showCustomer(model.customers.get(table.convertRowIndexToModel(table.getSelectedRow())));
					}
				} catch (Exception e) {
					state.setForeground(RetailPOSColors.dangerText());
					state.setText(text("loadError"));
				} finally {
					loading = false;
				}
			}
		}.execute();
	}

	private void selectionChanged() {
		int view = table.getSelectedRow();
		if (view < 0)
			return;
		if (saving) {
			loading = true;
			if (selected == null || !selectId(selected.getId()))
				table.clearSelection();
			loading = false;
			return;
		}
		if (!confirmDiscard()) {
			loading = true;
			if (selected == null || !selectId(selected.getId()))
				table.clearSelection();
			loading = false;
			return;
		}
		showCustomer(model.customers.get(table.convertRowIndexToModel(view)));
	}
	private boolean selectId(String id) {
		for (int i = 0; i < model.customers.size(); i++)
			if (model.customers.get(i).getId().equals(id)) {
				int v = table.convertRowIndexToView(i);
				table.setRowSelectionInterval(v, v);
				return true;
			}
		return false;
	}
	private void showCustomer(CustomerInfoExt c) {
		selected = c;
		creating = false;
		name.setText(value(c.getName()));
		phone.setText(value(c.getPhone()));
		notes.setText(value(c.getNotes()));
		originalName = name.getText();
		originalPhone = phone.getText();
		originalMaxDebt = value(c.getMaxdebt());
		maxDebt.setText(MONEY.format(originalMaxDebt));
		originalNotes = notes.getText();
		originalVisible = c.isVisible();
		detailTitle.setText(originalName);
		detailStatus.setText(value(c.getPhone()) + (c.isVisible() ? "" : " · " + text("archived")));
		debtValue.setText(MONEY.format(c.getCurdebt() == null ? 0 : c.getCurdebt()) + " €");
		debtValue.setForeground(
				c.getCurdebt() != null && c.getCurdebt() > 0 ? RetailPOSColors.dangerText() : RetailPOSColors.ink());
		populateActivity(c);
		archive.setText(c.isVisible() ? text("archive") : text("restore"));
		state.setForeground(RetailPOSColors.inkMuted());
		state.setText(" ");
		setDetailEnabled(true);
	}
	private String value(String s) {
		return s == null ? "" : s;
	}
	private void startNewCustomer() {
		if (saving || !confirmDiscard())
			return;
		selected = null;
		creating = true;
		name.setText("");
		phone.setText("");
		maxDebt.setText(MONEY.format(100.0));
		notes.setText("");
		originalName = originalPhone = originalNotes = "";
		originalMaxDebt = 100.0;
		originalVisible = true;
		detailTitle.setText(text("newCustomer"));
		detailStatus.setText("");
		debtValue.setText("0,00 €");
		orderValue.setText("0");
		lastValue.setText(text("none"));
		ticketsModel.setTickets(Collections.emptyList());
		ticketsTable.clearSelection();
		recentModel.setTickets(Collections.emptyList());
		summary.removeAll();
		ordersPanel.removeAll();
		debtValue.setForeground(RetailPOSColors.ink());
		archive.setText("");
		archive.setVisible(false);
		setDetailEnabled(true);
		tabs.setSelectedIndex(3);
		name.requestFocusInWindow();
	}
	private boolean isDirty() {
		return !name.getText().equals(originalName)
				|| !CustomerInfo.normalizePhone(phone.getText()).equals(CustomerInfo.normalizePhone(originalPhone))
				|| !isMaxDebtUnchanged() || !notes.getText().equals(originalNotes)
				|| (selected != null && !creating && originalVisible != selected.isVisible());
	}
	private boolean isMaxDebtUnchanged() {
		try {
			return Double.compare(parseMaxDebt(), originalMaxDebt) == 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	private double parseMaxDebt() {
		String text = maxDebt.getText().trim().replace(" ", "");
		if (text.matches("\\d{1,3}(\\.\\d{3})+(,\\d+)?"))
			text = text.replace(".", "");
		text = text.replace(',', '.');
		if (text.isEmpty())
			return 0.0;
		double value = Double.parseDouble(text);
		if (!Double.isFinite(value) || value < 0.0)
			throw new NumberFormatException();
		return value;
	}
	private double value(Double value) {
		return value == null ? 0.0 : value.doubleValue();
	}
	private boolean confirmDiscard() {
		if (saving)
			return false;
		if (!isDirty())
			return true;
		String who = creating ? text("newCustomer") : originalName;
		int answer = JConfirmationDialog.show(this, text("discardPrompt", who), text("discardTitle"),
				AppLocal.getIntString("customer.continue"), AppLocal.getIntString("customer.discard"), false);
		return answer == JOptionPane.YES_OPTION;
	}
	private void cancelEdit() {
		if (saving)
			return;
		if (creating) {
			clearDetail();
			return;
		}
		if (selected != null)
			showCustomer(selected);
	}
	private void saveCustomer() {
		if (saving || !isDirty())
			return;
		final boolean wasCreating = creating;
		final String n = name.getText().trim(), p = CustomerInfo.normalizePhone(phone.getText()), nt = notes.getText();
		final double md;
		try {
			md = parseMaxDebt();
		} catch (NumberFormatException e) {
			state.setForeground(RetailPOSColors.dangerText());
			state.setText(text("invalidDebt"));
			return;
		}
		if (n.isEmpty()) {
			state.setForeground(RetailPOSColors.dangerText());
			state.setText(text("requiredName"));
			return;
		}
		saving = true;
		setDetailEnabled(false);
		save.setEnabled(false);
		cancel.setEnabled(false);
		new SwingWorker<CustomerInfoExt, Void>() {
			protected CustomerInfoExt doInBackground() throws Exception {
				if (creating)
					return customers.createCustomer(n, p, nt, md);
				selected.setName(n);
				selected.setPhone(p);
				selected.setMaxdebt(md);
				selected.setNotes(nt);
				customers.updateCustomer(selected);
				return selected;
			}
			protected void done() {
				saving = false;
				try {
					selected = get();
					creating = false;
					showCustomer(selected);
					state.setForeground(RetailPOSColors.success());
					state.setText(text(wasCreating ? "created" : "saved") + " · "
							+ new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date()));
					allCustomersSnapshot = null;
					loadCustomers();
				} catch (Exception e) {
					setDetailEnabled(true);
					state.setForeground(RetailPOSColors.dangerText());
					state.setText(text("saveError"));
				}
			}
		}.execute();
	}
	private void toggleArchive() {
		if (saving || selected == null || creating)
			return;
		int answer = JConfirmationDialog.show(this, text("archivePrompt"), text("archiveTitle"),
				AppLocal.getIntString("customer.cancel"),
				AppLocal.getIntString(selected.isVisible() ? "customer.archive" : "customer.restore"), true);
		if (answer != JOptionPane.YES_OPTION)
			return;
		final boolean visible = !selected.isVisible();
		new SwingWorker<Void, Void>() {
			protected Void doInBackground() throws Exception {
				selected.setVisible(visible);
				customers.updateCustomer(selected);
				return null;
			}
			protected void done() {
				try {
					state.setForeground(RetailPOSColors.success());
					state.setText(text(visible ? "restored" : "archivedSuccess") + " · "
							+ new java.text.SimpleDateFormat("HH:mm").format(new java.util.Date()));
					allCustomersSnapshot = null;
					loadCustomers();
				} catch (Exception e) {
					state.setForeground(RetailPOSColors.dangerText());
					state.setText(text("archiveError"));
				}
			}
		}.execute();
	}
	private void clearDetail() {
		selected = null;
		creating = false;
		name.setText("");
		phone.setText("");
		maxDebt.setText("");
		notes.setText("");
		originalName = originalPhone = originalNotes = "";
		originalMaxDebt = 0.0;
		detailTitle.setText("");
		detailStatus.setText(text("selectCustomer"));
		debtValue.setText("");
		orderValue.setText("");
		lastValue.setText("");
		ticketsModel.setTickets(Collections.emptyList());
		ticketsTable.clearSelection();
		recentModel.setTickets(Collections.emptyList());
		summary.removeAll();
		ordersPanel.removeAll();
		summary.revalidate();
		summary.repaint();
		ordersPanel.revalidate();
		ordersPanel.repaint();
		archive.setVisible(false);
		setDetailEnabled(false);
	}
	private void updateActions() {
		save.setEnabled(!saving && (creating || selected != null) && isDirty());
		cancel.setEnabled(!saving && (creating || (selected != null && isDirty())));
	}
	private void setDetailEnabled(boolean enabled) {
		name.setEnabled(enabled);
		phone.setEnabled(enabled);
		maxDebt.setEnabled(enabled);
		notes.setEnabled(enabled);
		updateActions();
		archive.setEnabled(enabled && !creating && !saving);
		archive.setVisible(enabled && !creating);
		newSale.setEnabled(enabled && !creating && selected != null && selected.isVisible() && !saving);
		newOrder.setEnabled(enabled && !creating && selected != null && selected.isVisible() && !saving);
	}

	private class CustomersTableModel extends AbstractTableModel {
		private List<CustomerInfoExt> customers = Collections.emptyList();
		public void setCustomers(List<CustomerInfoExt> c) {
			customers = c;
			fireTableDataChanged();
		}
		public int getRowCount() {
			return customers.size();
		}
		public int getColumnCount() {
			return 5;
		}
		public String getColumnName(int c) {
			return text(new String[]{"name", "phone", "orders", "lastPurchase", "debt"}[c]);
		}
		public Object getValueAt(int r, int c) {
			CustomerInfoExt x = customers.get(r);
			return switch (c) {
				case 0 -> x.getName() + (x.isVisible() ? "" : "  (" + text("archived") + ")");
				case 1 -> x.getPhone();
				case 2 -> orderHistory.getOrDefault(x.getId(), Collections.emptyList()).stream()
						.filter(o -> !"DELIVERED".equals(o.status)).count();
				case 3 -> ticketHistory.getOrDefault(x.getId(), Collections.emptyList()).isEmpty()
						? null
						: ticketHistory.get(x.getId()).get(0).getDate();
				default -> x.getCurdebt() == null ? 0.0 : x.getCurdebt();
			};
		}
		public Class<?> getColumnClass(int c) {
			return switch (c) {
				case 2 -> Long.class;
				case 3 -> java.util.Date.class;
				case 4 -> Double.class;
				default -> String.class;
			};
		}
	}
	private static class DebtRenderer extends DefaultTableCellRenderer {
		DebtRenderer() {
			setHorizontalAlignment(SwingConstants.RIGHT);
			setFont(RetailPOSTheme.PLEX_MONO_SEMIBOLD);
			setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
		}
		public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
			super.getTableCellRendererComponent(t, v, sel, focus, r, c);
			setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
			double d = v == null ? 0 : ((Number) v).doubleValue();
			setText(MONEY.format(d));
			if (!sel)
				setForeground(d > 0 ? RetailPOSColors.dangerText() : RetailPOSColors.inkMuted());
			return this;
		}
	}
	private static class NameRenderer extends DefaultTableCellRenderer {
		NameRenderer() {
			setHorizontalAlignment(SwingConstants.LEFT);
			setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
		}
		public Component getTableCellRendererComponent(JTable t, Object v, boolean sel, boolean focus, int r, int c) {
			super.getTableCellRendererComponent(t, v, sel, focus, r, c);
			setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
			if (!sel && v != null && v.toString().endsWith("(arxivada)"))
				setForeground(RetailPOSColors.inkMuted());
			return this;
		}
	}
}
