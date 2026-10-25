package com.openbravo.pos.reports;

import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.JPanelView;
import com.openbravo.pos.theme.RetailPOSColors;
import com.openbravo.pos.theme.RetailPOSTheme;
import com.openbravo.pos.util.PosIcons;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.NumberFormat;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.Box;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.JToggleButton;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;

/** Non-blocking product sales report with local filtering and grouping. */
public final class JPanelProductSales extends JPanel implements JPanelView, BeanFactoryApp {

	private AppView app;
	private final ProductSalesRepository repository = new ProductSalesRepository();
	private ReportPeriodSelector period;
	private JTable table;
	private final ProductSalesTableModel model = new ProductSalesTableModel();
	private JToggleButton[] grouping;
	private int groupIndex = 1;
	private JTextField search;
	private JLabel amountValue;
	private JLabel unitsValue;
	private JLabel productsValue;
	private JLabel manualNote;
	private JPanel actions;
	private JButton expand;
	private JButton collapse;
	private Map<String, ProductSalesRepository.Category> categories = Collections.emptyMap();
	private List<ProductSalesRow> loaded = Collections.emptyList();
	private final Map<String, Boolean> collapsed = new HashMap<>();
	private int request;
	private int sortColumn = 4;
	private boolean descending = true;

	public JPanelProductSales() {
		initComponents();
	}

	@Override
	public void init(AppView app) throws BeanFactoryException {
		this.app = app;
	}

	@Override
	public Object getBean() {
		return this;
	}

	@Override
	public javax.swing.JComponent getComponent() {
		return this;
	}

	@Override
	public String getTitle() {
		return AppLocal.getIntString("Menu.ProductSalesSummary");
	}

	@Override
	public void activate() throws BasicException {
		groupIndex = 1;
		grouping[1].setSelected(true);
		// A compact overview on entry; open only the category the owner wants to
		// inspect.
		collapsed.clear();
		period.reset();
		loadRows();
	}

	@Override
	public boolean deactivate() {
		request++;
		return true;
	}

	private void initComponents() {
		period = new ReportPeriodSelector(this::loadRows);
		search = new JTextField();
		search.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(15f));
		search.setOpaque(false);
		search.setBorder(BorderFactory.createEmptyBorder());
		search.putClientProperty("JTextField.placeholderText", label("searchHint"));
		search.getAccessibleContext().setAccessibleName(label("search"));
		search.setToolTipText(label("searchHint"));
		JPanel searchWell = new JPanel(new BorderLayout(12, 0));
		searchWell.setBackground(RetailPOSColors.surface200());
		javax.swing.border.Border searchBorder = BorderFactory.createCompoundBorder(
				BorderFactory.createLineBorder(RetailPOSColors.border()),
				BorderFactory.createEmptyBorder(0, 12, 0, 12));
		searchWell.setBorder(searchBorder);
		int searchWidth = Math.max(350, search.getFontMetrics(search.getFont()).stringWidth(label("searchHint")) + 80);
		searchWell.setPreferredSize(new java.awt.Dimension(searchWidth, 48));
		searchWell.setMinimumSize(new java.awt.Dimension(searchWidth, 48));
		searchWell.add(new JLabel(PosIcons.search()), BorderLayout.WEST);
		searchWell.add(search, BorderLayout.CENTER);
		search.addFocusListener(new java.awt.event.FocusAdapter() {
			@Override
			public void focusGained(java.awt.event.FocusEvent e) {
				searchWell.setBorder(
						BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(RetailPOSColors.ink(), 2),
								BorderFactory.createEmptyBorder(0, 11, 0, 11)));
			}

			@Override
			public void focusLost(java.awt.event.FocusEvent e) {
				searchWell.setBorder(searchBorder);
			}
		});
		period.addSearchField(searchWell);
		search.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent e) {
				refresh();
			}

			@Override
			public void removeUpdate(DocumentEvent e) {
				refresh();
			}

			@Override
			public void changedUpdate(DocumentEvent e) {
				refresh();
			}
		});
		JPanel groupButtons = new JPanel(new GridLayout(1, 3, 4, 0));
		groupButtons.setOpaque(false);
		grouping = new JToggleButton[3];
		ButtonGroup group = new ButtonGroup();
		String[] groupLabels = {label("none"), label("byCategory"), label("byBrand")};
		for (int i = 0; i < grouping.length; i++) {
			final int index = i;
			JToggleButton button = new ReportPeriodSelector.PresetButton(groupLabels[i]);
			button.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
			button.setPreferredSize(new java.awt.Dimension(button.getPreferredSize().width + 16, 48));
			ReportPeriodSelector.activateOnEnter(button);
			button.addActionListener(e -> {
				groupIndex = index;
				collapsed.clear();
				sortColumn = 4;
				descending = true;
				refresh();
			});
			grouping[i] = button;
			group.add(button);
			groupButtons.add(button);
		}
		grouping[1].setSelected(true);
		expand = new TextActionButton(label("expand"));
		collapse = new TextActionButton(label("collapse"));
		for (JButton button : new JButton[]{expand, collapse}) {
			button.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
			button.setContentAreaFilled(false);
			button.setForeground(RetailPOSColors.ink());
			button.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
			button.addFocusListener(new java.awt.event.FocusAdapter() {
				@Override
				public void focusGained(java.awt.event.FocusEvent e) {
					button.setBorder(
							BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(RetailPOSColors.ink(), 2),
									BorderFactory.createEmptyBorder(0, 6, 0, 6)));
				}

				@Override
				public void focusLost(java.awt.event.FocusEvent e) {
					button.setBorder(BorderFactory.createEmptyBorder(2, 8, 2, 8));
				}
			});
		}
		expand.addActionListener(e -> {
			for (String key : model.view.groupKeys) {
				collapsed.put(key, false);
			}
			refresh();
		});
		collapse.addActionListener(e -> {
			for (String key : model.view.groupKeys) {
				collapsed.put(key, true);
			}
			refresh();
		});
		actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		actions.setOpaque(false);
		actions.add(expand);
		actions.add(collapse);
		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
		toolbar.setOpaque(false);
		JLabel groupLabel = new JLabel(label("groupBy"));
		groupLabel.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		groupLabel.setForeground(RetailPOSColors.inkMuted());
		toolbar.add(groupLabel);
		toolbar.add(groupButtons);
		toolbar.add(actions);
		JPanel figures = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
		figures.setOpaque(false);
		amountValue = figure(figures, "amount");
		unitsValue = figure(figures, "unitsLabel");
		productsValue = figure(figures, "productsLabel");
		manualNote = new JLabel();
		manualNote.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		manualNote.setForeground(RetailPOSColors.warning());
		JPanel overview = new JPanel(new BorderLayout(0, 4));
		overview.setOpaque(false);
		overview.add(figures, BorderLayout.NORTH);
		overview.add(manualNote, BorderLayout.CENTER);
		JPanel controls = new JPanel(new BorderLayout(0, 4));
		controls.setOpaque(false);
		controls.add(period, BorderLayout.NORTH);
		JPanel below = new JPanel(new java.awt.GridBagLayout());
		below.setOpaque(false);
		java.awt.GridBagConstraints belowConstraints = new java.awt.GridBagConstraints();
		belowConstraints.gridx = 0;
		belowConstraints.gridy = 0;
		belowConstraints.weightx = 1;
		belowConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		below.add(overview, belowConstraints);
		belowConstraints.gridy = 1;
		belowConstraints.insets = new java.awt.Insets(8, 0, 0, 0);
		below.add(toolbar, belowConstraints);
		belowConstraints.gridy = 2;
		belowConstraints.weighty = 1;
		belowConstraints.insets = new java.awt.Insets(0, 0, 0, 0);
		belowConstraints.fill = java.awt.GridBagConstraints.BOTH;
		below.add(Box.createGlue(), belowConstraints);
		controls.add(below, BorderLayout.CENTER);

		table = new JTable(model) {
			@Override
			public boolean getScrollableTracksViewportWidth() {
				return getParent() instanceof javax.swing.JViewport
						&& getPreferredSize().width <= getParent().getWidth();
			}

			@Override
			public void doLayout() {
				if (getParent() != null && getParent().getWidth() > 0) {
					resizeProductColumn(getParent().getWidth());
				}
				super.doLayout();
			}
		};
		table.setRowHeight(40);
		table.setFillsViewportHeight(true);
		table.getTableHeader().setReorderingAllowed(false);
		table.getTableHeader().setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		TableCellRenderer header = table.getTableHeader().getDefaultRenderer();
		table.getTableHeader().setDefaultRenderer((t, value, selected, focus, row, column) -> {
			Component component = header.getTableCellRendererComponent(t, value, selected, focus, row, column);
			if (component instanceof JLabel) {
				JLabel label = (JLabel) component;
				label.setHorizontalAlignment(column >= 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
				label.setText(value + (column == sortColumn ? descending ? " ↓" : " ↑" : ""));
			}
			return component;
		});
		table.getTableHeader().addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (groupIndex != 0) {
					return;
				}
				int column = table.getTableHeader().columnAtPoint(e.getPoint());
				if (column >= 0) {
					descending = sortColumn == column ? !descending : column >= 3;
					sortColumn = column;
					refresh();
				}
			}
		});
		table.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(14f));
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.getColumnModel().getColumn(0).setPreferredWidth(400);
		table.getColumnModel().getColumn(1).setPreferredWidth(135);
		table.getColumnModel().getColumn(2).setPreferredWidth(280);
		table.getColumnModel().getColumn(3).setPreferredWidth(65);
		table.getColumnModel().getColumn(4).setPreferredWidth(125);
		for (int column = 0; column < 5; column++) {
			table.getColumnModel().getColumn(column).setCellRenderer(new Cell(column));
		}
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				toggleGroup(table.rowAtPoint(e.getPoint()));
			}
		});
		table.getInputMap().put(javax.swing.KeyStroke.getKeyStroke("ENTER"), "toggleGroup");
		table.getActionMap().put("toggleGroup", new javax.swing.AbstractAction() {
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e) {
				toggleGroup(table.getSelectedRow());
			}
		});
		table.getInputMap(javax.swing.JComponent.WHEN_FOCUSED).put(javax.swing.KeyStroke.getKeyStroke("SPACE"),
				"toggleGroup");
		setLayout(new BorderLayout(0, 4));
		setBackground(RetailPOSColors.surface0());
		setBorder(BorderFactory.createEmptyBorder(0, 24, 24, 24));
		add(controls, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(table);
		scroll.getViewport().addComponentListener(new java.awt.event.ComponentAdapter() {
			@Override
			public void componentResized(java.awt.event.ComponentEvent e) {
				resizeProductColumn(scroll.getViewport().getWidth());
			}
		});
		add(scroll, BorderLayout.CENTER);
		refresh();
	}

	private static JLabel figure(JPanel figures, String key) {
		JPanel cell = new JPanel();
		cell.setOpaque(false);
		cell.setLayout(new BoxLayout(cell, BoxLayout.Y_AXIS));
		if (figures.getComponentCount() > 0) {
			figures.add(Box.createHorizontalStrut(32));
		}
		JLabel caption = new JLabel(label(key).toUpperCase(java.util.Locale.getDefault()));
		caption.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(12f));
		caption.setForeground(RetailPOSColors.inkMuted());
		JLabel value = new JLabel();
		value.setFont(RetailPOSTheme.PLEX_MONO_SEMIBOLD.deriveFont(20f));
		cell.add(caption);
		cell.add(value);
		figures.add(cell);
		return value;
	}

	private static final class TextActionButton extends JButton {
		TextActionButton(String text) {
			super(text);
		}

		@Override
		public java.awt.Dimension getPreferredSize() {
			java.awt.Dimension size = super.getPreferredSize();
			size.width = Math.max(size.width,
					getFontMetrics(getFont()).stringWidth(getText()) + getInsets().left + getInsets().right + 8);
			size.height = Math.max(48, size.height);
			return size;
		}
	}

	private void toggleGroup(int row) {
		if (row < 0 || row >= model.view.lines.size()) {
			return;
		}
		ProductSalesView.Line line = model.view.lines.get(row);
		if (line.group) {
			collapsed.put(line.groupKey, !collapsed.getOrDefault(line.groupKey, true));
			refresh();
			table.getSelectionModel().setSelectionInterval(row, row);
			table.scrollRectToVisible(table.getCellRect(row, 0, true));
		}
	}

	private void resizeProductColumn(int width) {
		int reference = table.getTableHeader().getFontMetrics(table.getTableHeader().getFont())
				.stringWidth(table.getColumnName(1)) + 34;
		for (ProductSalesRow row : loaded) {
			reference = Math.max(reference, table.getFontMetrics(table.getFont()).stringWidth(row.getReference()) + 56);
		}
		TableColumn referenceColumn = table.getColumnModel().getColumn(1);
		int referenceMaximum = Math.min(260, Math.max(160, width - 530));
		int referenceWidth = Math.min(referenceMaximum, Math.max(160, reference));
		referenceColumn.setPreferredWidth(referenceWidth);
		referenceColumn.setWidth(referenceWidth);
		TableColumn categoryColumn = table.getColumnModel().getColumn(2);
		if (groupIndex != 1) {
			int categoryWidth = Math.min(280, Math.max(140, width - referenceWidth - 65 - 125 - 260));
			categoryColumn.setPreferredWidth(categoryWidth);
			categoryColumn.setWidth(categoryWidth);
		}
		int fixed = 0;
		for (int i : new int[]{1, 2, 3, 4}) {
			fixed += table.getColumnModel().getColumn(i).getPreferredWidth();
		}
		int productWidth = Math.max(200, width - fixed);
		table.getColumnModel().getColumn(0).setPreferredWidth(productWidth);
		table.getColumnModel().getColumn(0).setWidth(productWidth);
	}

	private void loadRows() {
		if (app == null) {
			return;
		}
		final int current = ++request;
		final SalesSummaryParameters dates = period.getParameters();
		final long started = ReportLog.start("product_sales");
		new SwingWorker<ReportData, Void>() {
			@Override
			protected ReportData doInBackground() throws Exception {
				return ReportLog.inOperation(() -> {
					Connection connection = app.getSession().getConnection();
					return new ReportData(repository.load(connection, dates), repository.categories(connection));
				});
			}

			@Override
			protected void done() {
				if (current != request) {
					return;
				}
				try {
					ReportData data = get();
					loaded = data.rows;
					categories = data.categories;
					refresh();
					ReportLog.success("product_sales", loaded.size(), started);
				} catch (Exception e) {
					ReportLog.failure("product_sales", started, e);
					new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotloadreport"), e)
							.show(JPanelProductSales.this);
				}
			}
		}.execute();
	}

	private void refresh() {
		List<ProductSalesRow> sorted = new ArrayList<>(loaded);
		if (groupIndex == 0) {
			Comparator<ProductSalesRow> order;
			switch (sortColumn) {
				case 0 :
					order = ProductSalesView.productNameOrder();
					break;
				case 1 :
					order = Comparator.comparing(ProductSalesRow::getReference, String.CASE_INSENSITIVE_ORDER);
					break;
				case 2 :
					order = ProductSalesView.categoryProductOrder(categories);
					break;
				case 3 :
					order = Comparator.comparingDouble(ProductSalesRow::getUnits);
					break;
				default :
					order = Comparator.comparingDouble(ProductSalesRow::getAmount);
			}
			sorted.sort(descending ? order.reversed() : order);
		}
		ProductSalesView view = ProductSalesView.build(sorted, categories, search.getText(),
				ProductSalesView.Grouping.values()[groupIndex], collapsed, label("uncategorized"), label("unbranded"),
				label("manual"), label("total"));
		model.setView(view);
		for (int i = 0; i < view.lines.size(); i++) {
			if (view.lines.get(i).group) {
				table.setRowHeight(i, 56);
			}
		}
		amountValue.setText(Formats.CURRENCY.formatValue(view.amount));
		unitsValue.setText(Formats.DOUBLE.formatValue(view.units));
		productsValue.setText(Integer.toString(view.productCount));
		manualNote.setText(view.manualLines > 0
				? AppLocal.getIntString("reports.product.manualNote", Formats.CURRENCY.formatValue(view.manualAmount),
						Long.toString(view.manualLines))
				: view.productCount == 0 ? label("noResults") : "");
		manualNote.setForeground(view.manualLines > 0 ? RetailPOSColors.warning() : RetailPOSColors.inkMuted());
		manualNote.setVisible(view.manualLines > 0 || view.productCount == 0);
		actions.setVisible(groupIndex != 0);
		TableColumn categoryColumn = table.getColumnModel().getColumn(2);
		categoryColumn.setMinWidth(groupIndex == 1 ? 0 : 140);
		categoryColumn.setMaxWidth(groupIndex == 1 ? 0 : Integer.MAX_VALUE);
		categoryColumn.setPreferredWidth(groupIndex == 1 ? 0 : 280);
		categoryColumn.setWidth(groupIndex == 1 ? 0 : 280);
		resizeProductColumn(table.getParent() == null ? 990 : table.getParent().getWidth());
		table.getTableHeader().repaint();
	}

	/**
	 * Catalogue names may store a trailing reference; it is redundant only on this
	 * report.
	 */
	static String displayName(ProductSalesRow row) {
		return ProductSalesView.displayName(row);
	}

	private static String label(String key) {
		return AppLocal.getIntString("reports.product." + key);
	}

	private static final class ReportData {
		final List<ProductSalesRow> rows;
		final Map<String, ProductSalesRepository.Category> categories;

		ReportData(List<ProductSalesRow> rows, Map<String, ProductSalesRepository.Category> categories) {
			this.rows = rows;
			this.categories = categories;
		}
	}

	private final class ProductSalesTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		private ProductSalesView view = ProductSalesView.build(Collections.emptyList(), Collections.emptyMap(), "",
				ProductSalesView.Grouping.NONE, Collections.emptyMap(), "", "", "", "");

		void setView(ProductSalesView view) {
			this.view = view;
			fireTableDataChanged();
		}

		@Override
		public String getColumnName(int column) {
			String[] keys = {"label.reportproduct", "label.reportreference", "label.reportcategory",
					"reports.product.units", "label.reportamount"};
			return AppLocal.getIntString(keys[column]);
		}

		@Override
		public int getRowCount() {
			return view.lines.size();
		}

		@Override
		public int getColumnCount() {
			return 5;
		}

		@Override
		public Object getValueAt(int row, int column) {
			ProductSalesView.Line line = view.lines.get(row);
			if (column == 3) {
				return Formats.DOUBLE.formatValue(line.units);
			}
			if (column == 4) {
				return Formats.CURRENCY.formatValue(line.amount);
			}
			if (line.product == null) {
				if (column != 0) {
					return "";
				}
				if (!line.group) {
					return line.title;
				}
				String title = line.title.equals(label("manual")) ? manualLabel() : groupTitle(line.title);
				String detail = line.title.equals(label("manual"))
						? AppLocal.getIntString(view.manualLines == 1
								? "reports.product.groupManualCountOne"
								: "reports.product.groupManualCount", Long.toString(view.manualLines))
						: AppLocal.getIntString(line.productCount == 1
								? "reports.product.groupProductCountOne"
								: "reports.product.groupProductCount", Integer.toString(line.productCount));
				return "<html><nobr>" + title + "&nbsp;&nbsp;<font size='-1' color='"
						+ color(RetailPOSColors.inkMuted()) + "'>" + html(detail) + " · "
						+ html(groupPercentage(line.share, java.util.Locale.getDefault())) + "</font></nobr></html>";
			}
			if (line.product.isManual()) {
				return column == 0 ? "<html>" + manualLabel() + "</html>" : "";
			}
			switch (column) {
				case 0 :
					return displayName(line.product);
				case 1 :
					return line.product.getReference();
				case 2 :
					return line.product.getCategoryName();
				default :
					return "";
			}
		}
	}

	private static String manualLabel() {
		return html(label("manual")) + " · <span style='color:" + color(RetailPOSColors.warning()) + "'><b>"
				+ html(label("noProductBadge")) + "</b></span>";
	}

	private static String groupTitle(String path) {
		if (path.contains(" › ")) {
			path = path.substring(path.lastIndexOf(" › ") + 3);
		}
		return "<b>" + html(path) + "</b>";
	}

	static String groupPercentage(double share, java.util.Locale locale) {
		NumberFormat format = NumberFormat.getPercentInstance(locale);
		format.setMinimumFractionDigits(1);
		format.setMaximumFractionDigits(1);
		return format.format(share / 100.0).replace('\u00a0', ' ').replace('\u202f', ' ');
	}

	private final class Cell extends DefaultTableCellRenderer {
		private final int column;
		private boolean groupIndicator;
		private boolean folded;
		private int indent;

		Cell(int column) {
			this.column = column;
			setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus,
				int row, int col) {
			super.getTableCellRendererComponent(table, value, selected, focus, row, col);
			ProductSalesView.Line line = model.view.lines.get(row);
			groupIndicator = line.group && column == 0;
			folded = groupIndicator && collapsed.getOrDefault(line.groupKey, true);
			indent = groupIndex == 1 ? line.depth * 20 : 0;
			setBorder(focus
					? BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(RetailPOSColors.ink(), 2),
							BorderFactory.createEmptyBorder(0, (groupIndicator ? 26 : 6) + indent, 0, 6))
					: BorderFactory.createEmptyBorder(0, (groupIndicator ? 28 : 8) + indent, 0, 8));
			setHorizontalAlignment(column >= 3 ? SwingConstants.RIGHT : SwingConstants.LEFT);
			setFont((column >= 3 ? RetailPOSTheme.PLEX_MONO_REGULAR : RetailPOSTheme.MANROPE_MEDIUM).deriveFont(14f));
			setForeground(line.product != null && line.product.isManual() && column == 2
					? RetailPOSColors.warning()
					: RetailPOSColors.ink());
			if (line.group || line.total) {
				setFont((column >= 3 ? RetailPOSTheme.PLEX_MONO_SEMIBOLD : RetailPOSTheme.MANROPE_BOLD)
						.deriveFont(14f));
			}
			setBackground(selected
					? RetailPOSColors.brandSubtle()
					: line.group || line.total ? RetailPOSColors.surface200() : RetailPOSColors.surface100());
			setToolTipText(line.product == null
					? line.group ? line.title : null
					: line.product.isManual()
							? label("manualExplanation")
							: column == 1
									? line.product.getReference()
									: column == 2
											? ProductSalesRepository.categoryPath(line.product.getCategoryId(),
													categories)
											: null);
			if (column == 2 && line.product != null && !line.product.isManual()) {
				String path = ProductSalesRepository.categoryPath(line.product.getCategoryId(), categories);
				int last = path.lastIndexOf(" › ");
				if (last >= 0) {
					setText("<html><nobr><span style='color:" + color(RetailPOSColors.inkMuted()) + "'>"
							+ html(path.substring(0, last + 3)) + "</span><b>" + html(path.substring(last + 3))
							+ "</b></nobr></html>");
				} else {
					setText(path);
				}
			}
			return this;
		}

		@Override
		protected void paintComponent(java.awt.Graphics graphics) {
			super.paintComponent(graphics);
			if (groupIndicator) {
				java.awt.Graphics2D g = (java.awt.Graphics2D) graphics.create();
				g.setRenderingHint(java.awt.RenderingHints.KEY_ANTIALIASING,
						java.awt.RenderingHints.VALUE_ANTIALIAS_ON);
				g.setColor(RetailPOSColors.ink());
				g.setStroke(
						new java.awt.BasicStroke(2f, java.awt.BasicStroke.CAP_ROUND, java.awt.BasicStroke.JOIN_ROUND));
				int y = getHeight() / 2;
				int x = indent;
				if (folded) {
					g.drawLine(11 + x, y - 6, 17 + x, y);
					g.drawLine(17 + x, y, 11 + x, y + 6);
				} else {
					g.drawLine(8 + x, y - 3, 14 + x, y + 3);
					g.drawLine(14 + x, y + 3, 20 + x, y - 3);
				}
				g.dispose();
			}
		}
	}

	private static String color(java.awt.Color color) {
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}

	private static String html(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
