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
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

/** Non-blocking product sales report with local filtering and grouping. */
public final class JPanelProductSales extends JPanel implements JPanelView, BeanFactoryApp {

	private AppView app;
	private final ProductSalesRepository repository = new ProductSalesRepository();
	private ReportPeriodSelector period;
	private JTable table;
	private final ProductSalesTableModel model = new ProductSalesTableModel();
	private JComboBox<Choice> category;
	private JComboBox<Choice> brand;
	private JComboBox<String> grouping;
	private JTextField search;
	private JLabel summary;
	private JLabel manualNote;
	private JButton expand;
	private JButton collapse;
	private Map<String, ProductSalesRepository.Category> categories = Collections.emptyMap();
	private List<ProductSalesRow> loaded = Collections.emptyList();
	private List<String> brands = Collections.emptyList();
	private final Map<String, Boolean> collapsed = new HashMap<>();
	private int request;

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
		category = new JComboBox<>();
		brand = new JComboBox<>();
		category.setPreferredSize(new java.awt.Dimension(235, 48));
		brand.setPreferredSize(new java.awt.Dimension(190, 48));
		category.addItem(new Choice(null, label("allCategories")));
		brand.addItem(new Choice(null, label("allBrands")));
		category.addActionListener(e -> refresh());
		brand.addActionListener(e -> refresh());
		search = new JTextField(17);
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
		grouping = new JComboBox<>(new String[]{label("none"), label("byCategory"), label("byBrand")});
		grouping.addActionListener(e -> {
			collapsed.clear();
			refresh();
		});
		JPanel filters = new JPanel(new GridLayout(2, 1));
		filters.setOpaque(false);
		JPanel first = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
		first.setOpaque(false);
		first.add(new JLabel(label("search")));
		first.add(search);
		first.add(new JLabel(AppLocal.getIntString("label.reportcategory")));
		first.add(category);
		JPanel second = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 4));
		second.setOpaque(false);
		second.add(new JLabel(label("brand")));
		second.add(brand);
		second.add(new JLabel(label("groupBy")));
		second.add(grouping);
		filters.add(first);
		filters.add(second);

		expand = new JButton(label("expand"));
		collapse = new JButton(label("collapse"));
		expand.addActionListener(e -> {
			collapsed.clear();
			refresh();
		});
		collapse.addActionListener(e -> {
			for (ProductSalesView.Line line : model.view.lines) {
				if (line.group) {
					collapsed.put(line.title, true);
				}
			}
			refresh();
		});
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		actions.setOpaque(false);
		actions.add(expand);
		actions.add(collapse);
		summary = new JLabel();
		summary.setFont(RetailPOSTheme.MANROPE_BOLD.deriveFont(17f));
		manualNote = new JLabel();
		manualNote.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		manualNote.setForeground(RetailPOSColors.warning());
		JPanel overview = new JPanel(new GridLayout(3, 1, 0, 4));
		overview.setOpaque(false);
		overview.add(summary);
		overview.add(manualNote);
		overview.add(actions);
		JPanel controls = new JPanel(new BorderLayout(0, 8));
		controls.setOpaque(false);
		controls.add(period, BorderLayout.NORTH);
		controls.add(filters, BorderLayout.CENTER);
		controls.add(overview, BorderLayout.SOUTH);

		table = new JTable(model);
		table.setRowHeight(40);
		table.setFillsViewportHeight(true);
		table.getTableHeader().setReorderingAllowed(false);
		table.getTableHeader().setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(13f));
		table.setFont(RetailPOSTheme.MANROPE_MEDIUM.deriveFont(14f));
		table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
		table.getColumnModel().getColumn(0).setPreferredWidth(120);
		table.getColumnModel().getColumn(1).setPreferredWidth(400);
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
		setLayout(new BorderLayout(0, 16));
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

	private void toggleGroup(int row) {
		if (row < 0 || row >= model.view.lines.size()) {
			return;
		}
		ProductSalesView.Line line = model.view.lines.get(row);
		if (line.group) {
			collapsed.put(line.title, !collapsed.getOrDefault(line.title, false));
			refresh();
		}
	}

	private void resizeProductColumn(int width) {
		int fixed = 0;
		for (int i : new int[]{0, 2, 3, 4}) {
			fixed += table.getColumnModel().getColumn(i).getPreferredWidth();
		}
		table.getColumnModel().getColumn(1).setPreferredWidth(Math.max(260, width - fixed));
		table.revalidate();
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
					return new ReportData(repository.load(connection, dates), repository.categories(connection),
							repository.brands(connection));
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
					brands = data.brands;
					populateFilters();
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

	private void populateFilters() {
		String previousCategory = selected(category);
		String previousBrand = selected(brand);
		category.removeAllItems();
		category.addItem(new Choice(null, label("allCategories")));
		List<ProductSalesRepository.Category> ordered = new ArrayList<>(categories.values());
		ordered.sort(Comparator.comparing(c -> ProductSalesRepository.categoryPath(c.id, categories)));
		for (ProductSalesRepository.Category item : ordered) {
			category.addItem(new Choice(item.id, ProductSalesRepository.categoryPath(item.id, categories)));
		}
		brand.removeAllItems();
		brand.addItem(new Choice(null, label("allBrands")));
		for (String value : brands) {
			brand.addItem(new Choice(value, value));
		}
		reselect(category, previousCategory);
		reselect(brand, previousBrand);
	}

	private static void reselect(JComboBox<Choice> box, String value) {
		for (int i = 0; i < box.getItemCount(); i++) {
			if (java.util.Objects.equals(box.getItemAt(i).id, value)) {
				box.setSelectedIndex(i);
				return;
			}
		}
		box.setSelectedIndex(0);
	}

	private static String selected(JComboBox<Choice> box) {
		Choice choice = (Choice) box.getSelectedItem();
		return choice == null ? null : choice.id;
	}

	private void refresh() {
		ProductSalesView view = ProductSalesView.build(loaded, categories, selected(category), selected(brand),
				search.getText(), ProductSalesView.Grouping.values()[grouping.getSelectedIndex()], collapsed,
				label("uncategorized"), label("unbranded"), label("manual"), label("total"));
		model.setView(view);
		summary.setText(AppLocal.getIntString("reports.product.summary", Formats.CURRENCY.formatValue(view.amount),
				Formats.DOUBLE.formatValue(view.units), Integer.toString(view.productCount)));
		manualNote.setText(view.manualLines > 0
				? AppLocal.getIntString("reports.product.manualNote", Formats.CURRENCY.formatValue(view.manualAmount),
						Long.toString(view.manualLines))
				: view.productCount == 0 ? label("noResults") : " ");
		manualNote.setForeground(view.manualLines > 0 ? RetailPOSColors.warning() : RetailPOSColors.inkMuted());
		expand.setVisible(grouping.getSelectedIndex() != 0);
		collapse.setVisible(grouping.getSelectedIndex() != 0);
		table.getColumnModel().getColumn(2).setMinWidth(grouping.getSelectedIndex() == 1 ? 0 : 150);
		table.getColumnModel().getColumn(2).setMaxWidth(grouping.getSelectedIndex() == 1 ? 0 : Integer.MAX_VALUE);
		table.getColumnModel().getColumn(2).setPreferredWidth(grouping.getSelectedIndex() == 1 ? 0 : 280);
		resizeProductColumn(table.getParent() == null ? 990 : table.getParent().getWidth());
	}

	private static String label(String key) {
		return AppLocal.getIntString("reports.product." + key);
	}

	private static final class ReportData {
		final List<ProductSalesRow> rows;
		final Map<String, ProductSalesRepository.Category> categories;
		final List<String> brands;

		ReportData(List<ProductSalesRow> rows, Map<String, ProductSalesRepository.Category> categories,
				List<String> brands) {
			this.rows = rows;
			this.categories = categories;
			this.brands = brands;
		}
	}

	private static final class Choice {
		final String id;
		final String title;

		Choice(String id, String title) {
			this.id = id;
			this.title = title;
		}

		@Override
		public String toString() {
			return title;
		}
	}

	private final class ProductSalesTableModel extends AbstractTableModel {
		private static final long serialVersionUID = 1L;
		private ProductSalesView view = ProductSalesView.build(Collections.emptyList(), Collections.emptyMap(), null,
				null, "", ProductSalesView.Grouping.NONE, Collections.emptyMap(), "", "", "", "");

		void setView(ProductSalesView view) {
			this.view = view;
			fireTableDataChanged();
		}

		@Override
		public String getColumnName(int column) {
			String[] keys = {"label.reportreference", "label.reportproduct", "label.reportcategory",
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
				return column == 1
						? (line.group ? (collapsed.getOrDefault(line.title, false) ? "▸ " : "▾ ") : "") + line.title
								+ (line.group ? " · " + Formats.DOUBLE.formatValue(line.share) + "%" : "")
						: "";
			}
			if (line.product.isManual()) {
				return column == 1 ? label("manual") : column == 2 ? label("noProduct") : "";
			}
			switch (column) {
				case 0 :
					return line.product.getReference();
				case 1 :
					return line.product.getProductName();
				case 2 :
					return line.product.getCategoryName();
				default :
					return "";
			}
		}
	}

	private final class Cell extends DefaultTableCellRenderer {
		private final int column;

		Cell(int column) {
			this.column = column;
			setBorder(BorderFactory.createEmptyBorder(0, 8, 0, 8));
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean selected, boolean focus,
				int row, int col) {
			super.getTableCellRendererComponent(table, value, selected, focus, row, col);
			ProductSalesView.Line line = model.view.lines.get(row);
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
			if (column == 2 && line.product != null && !line.product.isManual()) {
				String path = ProductSalesRepository.categoryPath(line.product.getCategoryId(), categories);
				int last = path.lastIndexOf(" › ");
				if (last >= 0) {
					setText("<html><span style='color:" + color(RetailPOSColors.inkMuted()) + "'>"
							+ html(path.substring(0, last + 3)) + "</span><b>" + html(path.substring(last + 3))
							+ "</b></html>");
				} else {
					setText(path);
				}
			}
			return this;
		}
	}

	private static String color(java.awt.Color color) {
		return String.format("#%02x%02x%02x", color.getRed(), color.getGreen(), color.getBlue());
	}

	private static String html(String value) {
		return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}
}
