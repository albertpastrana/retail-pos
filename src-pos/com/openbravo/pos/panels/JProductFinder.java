package com.openbravo.pos.panels;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import java.util.Date;
import java.util.List;
import java.util.Map;

import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingConstants;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;

import com.openbravo.basic.BasicException;
import com.openbravo.data.user.ListProvider;
import com.openbravo.data.user.ListProviderCreator;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.sales.TaxesLogic;
import com.openbravo.pos.customers.CustomerInfoExt;
import com.openbravo.pos.ticket.ProductFilterSales;
import com.openbravo.pos.ticket.CategoryInfo;
import com.openbravo.pos.ticket.ProductInfoExt;
import com.openbravo.pos.ticket.TaxInfo;

public class JProductFinder extends JDialog {
	private static final java.util.logging.Logger LOGGER = java.util.logging.Logger
			.getLogger(JProductFinder.class.getName());
	private ProductInfoExt m_ReturnProduct;
	private ListProvider lpr;
	private TaxesLogic taxesLogic;
	private Date ticketDate;
	private CustomerInfoExt customer;
	private boolean showTaxInclusivePrice;
	private Map<String, Double> productStocks = java.util.Collections.emptyMap();

	public static final int PRODUCT_ALL = 0;
	public static final int PRODUCT_NORMAL = 1;
	public static final int PRODUCT_AUXILIAR = 2;

	private JProductFinder(Frame parent, boolean modal) {
		super(parent, modal);
	}
	private JProductFinder(Dialog parent, boolean modal) {
		super(parent, modal);
	}

	private ProductInfoExt init(DataLogicSales dlSales, int productsType, String actionKey) {
		return init(dlSales, productsType, actionKey, null, null, null);
	}

	private ProductInfoExt init(DataLogicSales dlSales, int productsType, String actionKey, TaxesLogic taxesLogic,
			Date ticketDate, CustomerInfoExt customer) {
		this.taxesLogic = taxesLogic;
		this.ticketDate = ticketDate;
		this.customer = customer;
		this.showTaxInclusivePrice = taxesLogic != null;
		try {
			this.productStocks = dlSales.getProductStocks();
		} catch (BasicException exception) {
			LOGGER.log(java.util.logging.Level.WARNING, "event=product_search_stock_failed", exception);
			this.productStocks = java.util.Collections.emptyMap();
		}
		initComponents();
		jcmdOK.setText(AppLocal.getIntString(actionKey));
		ProductFilterSales filter = new ProductFilterSales();
		try {
			filter.setCategories(dlSales.getCategoriesList().list());
		} catch (BasicException exception) {
			LOGGER.log(java.util.logging.Level.WARNING, "event=product_search_categories_failed", exception);
			filter.setCategories(java.util.Collections.<CategoryInfo>emptyList());
		}
		filter.activate();
		filter.addActionListener(event -> executeSearch());
		m_jProductSelect.add(filter, BorderLayout.CENTER);
		switch (productsType) {
			case PRODUCT_NORMAL :
				lpr = new ListProviderCreator(dlSales.getProductListNormal(), filter);
				break;
			case PRODUCT_AUXILIAR :
				lpr = new ListProviderCreator(dlSales.getProductListAuxiliar(), filter);
				break;
			default :
				lpr = new ListProviderCreator(dlSales.getProductList(), filter);
				break;
		}
		pack();
		m_jSplitPane.setDividerLocation(0.4);
		setLocationRelativeTo(getOwner());
		getRootPane().setDefaultButton(jcmdOK);
		m_ReturnProduct = null;
		setVisible(true);
		return m_ReturnProduct;
	}

	private static Window getWindow(Component parent) {
		if (parent == null)
			return new JFrame();
		if (parent instanceof Frame || parent instanceof Dialog)
			return (Window) parent;
		return getWindow(parent.getParent());
	}

	public static ProductInfoExt showMessage(Component parent, DataLogicSales dlSales) {
		return showMessage(parent, dlSales, PRODUCT_ALL);
	}

	public static ProductInfoExt showMessage(Component parent, DataLogicSales dlSales, String actionKey) {
		return showMessage(parent, dlSales, PRODUCT_ALL, actionKey);
	}

	public static ProductInfoExt showMessage(Component parent, DataLogicSales dlSales, String actionKey,
			TaxesLogic taxesLogic, Date ticketDate, CustomerInfoExt customer) {
		Window window = getWindow(parent);
		JProductFinder finder = window instanceof Frame
				? new JProductFinder((Frame) window, true)
				: new JProductFinder((Dialog) window, true);
		return finder.init(dlSales, PRODUCT_ALL, actionKey, taxesLogic, ticketDate, customer);
	}

	public static ProductInfoExt showMessage(Component parent, DataLogicSales dlSales, int productsType) {
		return showMessage(parent, dlSales, productsType, "button.selectproduct");
	}

	private static ProductInfoExt showMessage(Component parent, DataLogicSales dlSales, int productsType,
			String actionKey) {
		Window window = getWindow(parent);
		JProductFinder finder = window instanceof Frame
				? new JProductFinder((Frame) window, true)
				: new JProductFinder((Dialog) window, true);
		return finder.init(dlSales, productsType, actionKey);
	}

	private static class ProductTableModel extends AbstractTableModel {
		private final List<ProductInfoExt> products;
		private final Map<String, Double> productStocks;
		private final TaxesLogic taxesLogic;
		private final Date ticketDate;
		private final CustomerInfoExt customer;
		private final boolean showTaxInclusivePrice;
		private final String[] columns;

		ProductTableModel(List<ProductInfoExt> products, Map<String, Double> productStocks, TaxesLogic taxesLogic,
				Date ticketDate, CustomerInfoExt customer, boolean showTaxInclusivePrice) {
			this.products = products;
			this.productStocks = productStocks;
			this.taxesLogic = taxesLogic;
			this.ticketDate = ticketDate;
			this.customer = customer;
			this.showTaxInclusivePrice = showTaxInclusivePrice;
			columns = new String[]{AppLocal.getIntString("label.prodref"), AppLocal.getIntString("label.prodname"),
					AppLocal.getIntString("label.productfinder.stock"),
					AppLocal.getIntString(showTaxInclusivePrice ? "label.productfinder.pvp" : "label.price")};
		}

		@Override
		public int getRowCount() {
			return products.size();
		}
		@Override
		public int getColumnCount() {
			return columns.length;
		}
		@Override
		public String getColumnName(int column) {
			return columns[column];
		}
		@Override
		public Object getValueAt(int row, int column) {
			ProductInfoExt product = products.get(row);
			switch (column) {
				case 0 :
					return product.getReference();
				case 1 :
					return product.getName();
				case 2 :
					return Formats.DOUBLE.formatValue(productStocks.getOrDefault(product.getID(), 0.0));
				case 3 :
					return formatPrice(product, showTaxInclusivePrice ? taxesLogic : null, ticketDate, customer);
				default :
					return "";
			}
		}
	}

	static String formatPrice(ProductInfoExt product, TaxesLogic taxesLogic, Date ticketDate,
			CustomerInfoExt customer) {
		if (taxesLogic == null) {
			return Formats.CURRENCY.formatValue(Double.valueOf(product.getPriceSell()));
		}
		TaxInfo tax = taxesLogic.getTaxInfo(product.getTaxCategoryID(), ticketDate, customer);
		return tax == null ? product.printPriceSell() : product.printPriceSellTax(tax);
	}

	private void initComponents() {
		jPanel2 = new javax.swing.JPanel(new BorderLayout());
		m_jProductSelect = new javax.swing.JPanel(new BorderLayout());
		jPanel5 = new javax.swing.JPanel(new BorderLayout());
		jScrollPane1 = new JScrollPane();
		jTableProducts = new JTable();
		jPanel1 = new javax.swing.JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));
		jcmdOK = new javax.swing.JButton();
		jcmdCancel = new javax.swing.JButton();

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setTitle(AppLocal.getIntString("form.productfinder"));
		jTableProducts.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		jTableProducts.setAutoCreateRowSorter(true);
		jTableProducts.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		jTableProducts.setFillsViewportHeight(true);
		jTableProducts.setRowHeight(32);
		jTableProducts.setModel(new ProductTableModel(java.util.Collections.emptyList(), productStocks, taxesLogic,
				ticketDate, customer, showTaxInclusivePrice));
		jTableProducts
				.setPreferredScrollableViewportSize(new java.awt.Dimension(900, jTableProducts.getRowHeight() * 8));
		configureColumnAlignments();
		jTableProducts.getSelectionModel().addListSelectionListener(event -> jcmdOK.setEnabled(hasSelectableProduct()));
		jTableProducts.addMouseListener(new java.awt.event.MouseAdapter() {
			@Override
			public void mouseClicked(java.awt.event.MouseEvent event) {
				if (event.getClickCount() == 2)
					selectProduct();
			}
		});
		jScrollPane1.setViewportView(jTableProducts);
		jScrollPane1.setPreferredSize(new java.awt.Dimension(900, 300));
		jPanel5.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		jPanel5.add(jScrollPane1, BorderLayout.CENTER);
		showProductMessage("message.productfilter");

		m_jSplitPane = new javax.swing.JSplitPane(javax.swing.JSplitPane.VERTICAL_SPLIT, m_jProductSelect, jPanel5);
		m_jSplitPane.setResizeWeight(0.4);
		m_jSplitPane.setContinuousLayout(true);
		jPanel2.add(m_jSplitPane, BorderLayout.CENTER);

		jcmdOK.setText(AppLocal.getIntString("Button.OK"));
		jcmdOK.setEnabled(false);
		jcmdOK.setMargin(new java.awt.Insets(8, 16, 8, 16));
		jcmdOK.addActionListener(event -> selectProduct());
		jcmdCancel.setText(AppLocal.getIntString("button.cancelselection"));
		jcmdCancel.setMargin(new java.awt.Insets(8, 16, 8, 16));
		jcmdCancel.addActionListener(event -> dispose());
		jPanel1.add(jcmdCancel);
		jPanel1.add(jcmdOK);
		jPanel2.add(jPanel1, BorderLayout.SOUTH);
		getContentPane().add(jPanel2, BorderLayout.CENTER);
	}

	private void selectProduct() {
		int selectedRow = jTableProducts.getSelectedRow();
		if (selectedRow >= 0) {
			int modelRow = jTableProducts.convertRowIndexToModel(selectedRow);
			ProductTableModel model = (ProductTableModel) jTableProducts.getModel();
			m_ReturnProduct = model.products.get(modelRow);
			dispose();
		}
	}

	private boolean hasSelectableProduct() {
		return jTableProducts.getModel() instanceof ProductTableModel
				&& !((ProductTableModel) jTableProducts.getModel()).products.isEmpty()
				&& jTableProducts.getSelectedRow() >= 0;
	}

	private void executeSearch() {
		try {
			List<ProductInfoExt> products = lpr.loadData();
			jTableProducts.setModel(new ProductTableModel(products, productStocks, taxesLogic, ticketDate, customer,
					showTaxInclusivePrice));
			configureColumnAlignments();
			if (products.isEmpty()) {
				showProductMessage("message.productfilter.empty");
				jcmdOK.setEnabled(false);
			} else {
				jScrollPane1.setViewportView(jTableProducts);
				setColumnWidths();
				jTableProducts.setRowSelectionInterval(0, 0);
			}
		} catch (BasicException exception) {
			LOGGER.log(java.util.logging.Level.WARNING, "event=product_search_failed", exception);
		}
	}

	private void showProductMessage(String messageKey) {
		javax.swing.JLabel message = new javax.swing.JLabel(AppLocal.getIntString(messageKey),
				javax.swing.SwingConstants.CENTER);
		jScrollPane1.setViewportView(message);
	}

	private void setColumnWidths() {
		int width = jTableProducts.getPreferredScrollableViewportSize().width;
		int referenceWidth = 160;
		int stockWidth = 70;
		int priceWidth = 150;
		jTableProducts.getColumnModel().getColumn(0).setPreferredWidth(referenceWidth);
		jTableProducts.getColumnModel().getColumn(1)
				.setPreferredWidth(Math.max(100, width - referenceWidth - stockWidth - priceWidth));
		jTableProducts.getColumnModel().getColumn(2).setPreferredWidth(stockWidth);
		jTableProducts.getColumnModel().getColumn(3).setPreferredWidth(priceWidth);
	}

	private void configureColumnAlignments() {
		int[] alignments = {SwingConstants.LEFT, SwingConstants.CENTER, SwingConstants.RIGHT, SwingConstants.RIGHT};
		for (int column = 0; column < alignments.length; column++) {
			DefaultTableCellRenderer renderer = new DefaultTableCellRenderer();
			renderer.setHorizontalAlignment(alignments[column]);
			jTableProducts.getColumnModel().getColumn(column).setCellRenderer(renderer);
			DefaultTableCellRenderer header = new DefaultTableCellRenderer();
			header.setHorizontalAlignment(alignments[column]);
			jTableProducts.getColumnModel().getColumn(column).setHeaderRenderer(header);
		}
	}

	private javax.swing.JTable jTableProducts;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JPanel jPanel5;
	private javax.swing.JScrollPane jScrollPane1;
	private javax.swing.JButton jcmdCancel;
	private javax.swing.JButton jcmdOK;
	private javax.swing.JPanel m_jProductSelect;
	private javax.swing.JSplitPane m_jSplitPane;
}
