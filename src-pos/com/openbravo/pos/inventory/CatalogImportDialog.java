package com.openbravo.pos.inventory;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import com.openbravo.basic.BasicException;
import com.openbravo.format.Formats;
import com.openbravo.pos.customers.CustomerInfoExt;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.sales.TaxesLogic;
import com.openbravo.pos.ticket.CategoryInfo;
import com.openbravo.pos.ticket.ProductInfoExt;

public final class CatalogImportDialog {

	public enum Copy {
		RECEIPT, STOCK
	}

	private final Component parent;
	private final AppView app;
	private final DataLogicSales dlSales;
	private final TaxesLogic taxeslogic;
	private final Date taxDate;
	private final CustomerInfoExt customer;
	private final List taxCategories;
	private final String defaultTaxCategoryId;
	private final PriceRuleService priceRuleService;
	private final TaxRegime priceTaxRegime;
	private final Copy copy;
	private boolean cancelled;

	public CatalogImportDialog(Component parent, AppView app, DataLogicSales dlSales, TaxesLogic taxeslogic,
			Date taxDate, CustomerInfoExt customer, List taxCategories, String defaultTaxCategoryId,
			PriceRuleService priceRuleService, TaxRegime priceTaxRegime, Copy copy) {
		this.parent = parent;
		this.app = app;
		this.dlSales = dlSales;
		this.taxeslogic = taxeslogic;
		this.taxDate = taxDate;
		this.customer = customer;
		this.taxCategories = taxCategories;
		this.defaultTaxCategoryId = defaultTaxCategoryId;
		this.priceRuleService = priceRuleService;
		this.priceTaxRegime = priceTaxRegime;
		this.copy = copy;
	}

	public static CatalogImportDialog forStock(Component parent, AppView app, DataLogicSales dlSales)
			throws BasicException {
		PriceRuleService priceRules = new PriceRuleService(app.getSession());
		TaxRegime regime;
		try {
			regime = priceRules.getTaxRegime();
		} catch (java.sql.SQLException e) {
			throw new BasicException(AppLocal.getIntString("message.pricerules.loaderror"), e);
		}
		return new CatalogImportDialog(parent, app, dlSales, new TaxesLogic(dlSales.getTaxList().list()), new Date(),
				null, dlSales.getTaxCategoriesList().list(), "001", priceRules, regime, Copy.STOCK);
	}

	public boolean wasCancelled() {
		return cancelled;
	}

	public ProductInfoExt importIfAbsent(String code) throws BasicException {
		cancelled = false;
		ProductInfoExt product = dlSales.getProductInfoByCode(code);
		if (product != null) {
			return product;
		}
		String productsPath = app.getProperties().getProperty("catalog.import.products");
		String categoriesPath = app.getProperties().getProperty("catalog.import.categories");
		ProductInfoExt catalogProduct = dlSales.getCatalogProductByCode(code, productsPath, categoriesPath);
		List<ProductInfoExt> family = catalogProduct == null ? new ArrayList<ProductInfoExt>()
				: dlSales.getCatalogProductFamily(code, productsPath, categoriesPath);
		if (family.size() > 1) {
			List<ProductInfoExt> editedFamily = editProductFamilyForImport(code, family);
			if (editedFamily == null) {
				cancelled = true;
				return null;
			}
			dlSales.importProducts(editedFamily, categoriesPath);
			for (ProductInfoExt imported : editedFamily) {
				applyImportedStock(imported);
			}
			return dlSales.getProductInfoByCode(code);
		}
		ProductInfoExt editedProduct = editProductForImport(code, catalogProduct);
		if (editedProduct == null) {
			cancelled = true;
			return null;
		}
		product = dlSales.importProduct(editedProduct, editedProduct.getProperty("catalog.brand"), categoriesPath);
		applyImportedStock(editedProduct);
		return product;
	}

	private String confirmLabel(boolean family) {
		if (copy == Copy.STOCK) {
			return AppLocal.getIntString(family ? "button.createfamily" : "button.createproduct");
		}
		return AppLocal.getIntString(family ? "button.addfamilytoreceipt" : "button.addtoreceipt");
	}

	private String familyMessageKey() {
		return copy == Copy.STOCK ? "message.importproductfamily.stock" : "message.importproductfamily";
	}

	private String importMessageKey(boolean fromCatalog) {
		if (copy == Copy.STOCK) {
			return fromCatalog ? "message.importproduct.stock" : "message.importproduct.unknown.stock";
		}
		return fromCatalog ? "message.importproduct" : "message.importproduct.unknown";
	}

	private List<ProductInfoExt> editProductFamilyForImport(String code, List<ProductInfoExt> family)
			throws BasicException {
		// One model shared by the checkbox each variant card draws under its price,
		// so the choice follows the cashier from card to card.
		final JToggleButton.ToggleButtonModel applyPriceModel = new JToggleButton.ToggleButtonModel();
		applyPriceModel.setSelected(true);
		final List<VariantImportEditor> editors = new ArrayList<VariantImportEditor>();
		int scannedRow = 0;
		for (int i = 0; i < family.size(); i++) {
			ProductInfoExt variant = family.get(i);
			boolean scanned = code.equals(variant.getCode()) || ("0" + code).equals(variant.getCode())
					|| ("00" + code).equals(variant.getCode());
			if (scanned) {
				scannedRow = i;
			}
			editors.add(new VariantImportEditor(variant, scanned, applyPriceModel));
		}

		final VariantTableModel model = new VariantTableModel(editors);
		final JTable table = new JTable(model);
		final boolean[] applyingFamilyPrice = new boolean[] { false };
		for (final VariantImportEditor source : editors) {
			source.priceChangeListener = new Runnable() {
				@Override
				public void run() {
					if (!applyPriceModel.isSelected() || applyingFamilyPrice[0] || source.prices.reportlock) {
						return;
					}
					Double margin = source.prices.readCommercialMargin();
					if (margin == null) {
						return;
					}
					applyingFamilyPrice[0] = true;
					try {
						for (VariantImportEditor target : editors) {
							if (target != source && target.selected) {
								target.prices.setCommercialMargin(margin.doubleValue());
							}
						}
					} finally {
						applyingFamilyPrice[0] = false;
					}
				}
			};
		}
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(24);
		table.getColumnModel().getColumn(0).setMaxWidth(42);
		table.getColumnModel().getColumn(1).setPreferredWidth(190);
		table.getColumnModel().getColumn(2).setPreferredWidth(90);
		table.getColumnModel().getColumn(3).setPreferredWidth(90);

		final JPanel cards = new JPanel(new CardLayout());
		for (VariantImportEditor editor : editors) {
			cards.add(editor.panel, editor.product.getCode());
		}
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent event) {
				int selected = table.getSelectedRow();
				if (!event.getValueIsAdjusting() && selected >= 0) {
					((CardLayout) cards.getLayout()).show(cards, editors.get(selected).product.getCode());
					focusPriceField(editors.get(selected).stock);
				}
			}
		});
		table.setRowSelectionInterval(scannedRow, scannedRow);
		focusImportField(editors.get(scannedRow).stock);

		JButton selectAll = new JButton(AppLocal.getIntString("button.variants.all"));
		selectAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				model.selectAll();
			}
		});
		JButton scannedOnly = new JButton(AppLocal.getIntString("button.variants.scanned"));
		scannedOnly.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				model.selectScannedOnly();
			}
		});
		JPanel listButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		listButtons.add(selectAll);
		listButtons.add(scannedOnly);

		JPanel variantsPanel = new JPanel(new BorderLayout(0, 6));
		variantsPanel.add(listButtons, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(table);
		scroll.setPreferredSize(new Dimension(420, 330));
		variantsPanel.add(scroll, BorderLayout.CENTER);

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, variantsPanel, cards);
		split.setResizeWeight(0.5);
		split.setBorder(null);

		JPanel content = new JPanel(new BorderLayout(0, 10));
		content.add(buildFamilyImportMessage(), BorderLayout.NORTH);
		content.add(split, BorderLayout.CENTER);

		String title = AppLocal.getIntString("title.importproductfamily");
		Object[] options = new Object[] { confirmLabel(true), AppLocal.getIntString("button.skipitem") };
		while (true) {
			int result = JOptionPane.showOptionDialog(parent, content, title, JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
			if (result != 0) {
				return null;
			}
			List<ProductInfoExt> selected = new ArrayList<ProductInfoExt>();
			for (int i = 0; i < editors.size(); i++) {
				VariantImportEditor editor = editors.get(i);
				if (!editor.selected) {
					continue;
				}
				ProductInfoExt product = editor.buildProduct();
				if (product == null) {
					table.setRowSelectionInterval(i, i);
					String message = editor.stockInvalid ? AppLocal.getIntString("message.stockaddpositive")
							: AppLocal.getIntString("message.importproductrequired");
					JOptionPane.showMessageDialog(parent, message, title, JOptionPane.WARNING_MESSAGE);
					selected.clear();
					break;
				}
				selected.add(product);
			}
			if (!selected.isEmpty()) {
				return selected;
			}
		}
	}

	private JLabel buildFamilyImportMessage() {
		return new JLabel(AppLocal.getIntString(familyMessageKey()));
	}

	private ProductInfoExt editProductForImport(String code, ProductInfoExt catalogProduct) throws BasicException {
		ProductInfoExt availableProduct = catalogProduct;
		if (availableProduct == null) {
			availableProduct = new ProductInfoExt();
			availableProduct.setCode(code);
			availableProduct.setReference(code);
			availableProduct.setName("");
		}
		final JTextField name = new JTextField(availableProduct.getName(), 32);
		name.setCaretPosition(0);
		String initialReference = availableProduct.getReference();
		if (initialReference == null || initialReference.trim().isEmpty()) {
			initialReference = availableProduct.getCode();
		}
		final JTextField reference = new JTextField(initialReference, 16);
		reference.setCaretPosition(0);
		final JComboBox<CategoryInfo> category = createImportCategoryCombo(availableProduct);
		final ProductPriceFields prices = createImportPrices(availableProduct);
		final JTextField stock = ProductFormLayout.numberField(true);

		// What the item is, then what it costs and sells for, then how many. The
		// price rule goes above the price it suggests, and each read-only number
		// under the field it comes from.
		JPanel fields = new JPanel(new GridBagLayout());
		ProductFormLayout.addRow(fields, 0, AppLocal.getIntString("label.prodname") + ":", name);
		ProductFormLayout.addRow(fields, 1, AppLocal.getIntString("label.prodcategory") + ":", category);
		ProductFormLayout.addRow(fields, 2, AppLocal.getIntString("label.prodref") + ":", reference);
		ProductFormLayout.addRow(fields, 3, AppLocal.getIntString("label.prodpricebuy") + ":", prices.buy);
		ProductFormLayout.addRow(fields, 4, prices.secondaryLabel() + ":", prices.secondary);
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = ProductFormLayout.ROW_INSETS;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridx = 1;
		constraints.gridy = 5;
		fields.add(prices.offer, constraints);
		ProductFormLayout.addRow(fields, 6, AppLocal.getIntString("label.prodpriceselltax") + ":", prices.priceBlock());
		ProductFormLayout.addRow(fields, 7, AppLocal.getIntString("label.taxcategory") + ":", prices.tax);
		ProductFormLayout.addRow(fields, 8, AppLocal.getIntString("label.prodstock") + ":", stock);

		JPanel content = new JPanel(new BorderLayout(0, 12));
		content.add(buildImportMessage(code, catalogProduct != null), BorderLayout.NORTH);
		content.add(ProductFormLayout.topAligned(fields), BorderLayout.CENTER);
		focusImportField(stock);

		String title = AppLocal.getIntString("title.importproduct");
		Object[] options = new Object[] { confirmLabel(false), AppLocal.getIntString("button.skipitem") };
		while (true) {
			int result = JOptionPane.showOptionDialog(parent, content, title, JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
			if (result != 0) {
				return null;
			}

			if (hasInvalidImportStock(stock.getText())) {
				JOptionPane.showMessageDialog(parent, AppLocal.getIntString("message.stockaddpositive"), title,
						JOptionPane.WARNING_MESSAGE);
				continue;
			}
			ProductInfoExt edited = buildEditedProduct(availableProduct, catalogProduct, reference, name, category,
					prices, stock);
			if (edited == null) {
				JOptionPane.showMessageDialog(parent, AppLocal.getIntString("message.importproductrequired"), title,
						JOptionPane.WARNING_MESSAGE);
				continue;
			}
			return edited;
		}
	}

	private JLabel buildImportMessage(String code, boolean fromCatalog) {
		String message = AppLocal.getIntString(importMessageKey(fromCatalog), code);
		return new JLabel("<html><body style='width: 320px'>" + message + "</body></html>");
	}

	// JOptionPane grabs focus for its default button, so the caret only lands on
	// the field once the dialog is on screen.
	private void focusImportField(final JComponent field) {
		field.addAncestorListener(new AncestorListener() {
			@Override
			public void ancestorAdded(AncestorEvent event) {
				focusPriceField(field);
			}

			@Override
			public void ancestorRemoved(AncestorEvent event) {
			}

			@Override
			public void ancestorMoved(AncestorEvent event) {
			}
		});
	}

	private void focusPriceField(final JComponent field) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				field.requestFocusInWindow();
				if (field instanceof JTextField) {
					((JTextField) field).selectAll();
				}
			}
		});
	}

	private ProductInfoExt buildEditedProduct(ProductInfoExt availableProduct, ProductInfoExt catalogProduct,
			JTextField reference, JTextField name, JComboBox<CategoryInfo> category, ProductPriceFields prices,
			JTextField stock) {
		Double buy = ProductPriceMath.parsePositiveCurrency(prices.buy.getText(), true);
		Double sell = prices.pricesell;
		CategoryInfo selected = (CategoryInfo) category.getSelectedItem();
		TaxCategoryInfo tax = (TaxCategoryInfo) prices.tax.getSelectedItem();
		if (name.getText().trim().isEmpty() || selected == null || tax == null || buy == null || sell == null
				|| hasInvalidImportStock(stock.getText())) {
			return null;
		}

		ProductInfoExt edited = new ProductInfoExt();
		edited.setID(catalogProduct == null ? java.util.UUID.randomUUID().toString() : catalogProduct.getID());
		edited.setCode(availableProduct.getCode());
		String editedReference = reference.getText().trim();
		edited.setReference(editedReference.isEmpty() ? edited.getCode() : editedReference);
		edited.setName(name.getText().trim());
		edited.setCategoryID(selected.getID());
		edited.setTaxCategoryID(tax.getID());
		edited.setPriceBuy(buy.doubleValue());
		edited.setPriceSell(sell.doubleValue());
		String brand = availableProduct.getProperty("catalog.brand", "").trim();
		if (!brand.isEmpty()) {
			edited.setProperty("catalog.brand", brand);
		}
		assignImportStock(edited, stock.getText());
		return edited;
	}

	private boolean hasInvalidImportStock(String text) {
		if (text == null || text.trim().isEmpty()) {
			return false;
		}
		try {
			Double units = (Double) Formats.DOUBLE.parseValue(text);
			return units == null || units.doubleValue() < 0.0;
		} catch (BasicException e) {
			return true;
		}
	}

	private void assignImportStock(ProductInfoExt product, String text) {
		if (text == null || text.trim().isEmpty()) {
			return;
		}
		try {
			Double units = (Double) Formats.DOUBLE.parseValue(text);
			if (units != null && units.doubleValue() > 0.0) {
				product.setProperty("import.stock", Double.toString(units.doubleValue()));
			}
		} catch (BasicException e) {
			return;
		}
	}

	private void applyImportedStock(ProductInfoExt product) throws BasicException {
		String raw = product.getProperty("import.stock");
		if (raw == null || raw.isEmpty()) {
			return;
		}
		double units = Double.parseDouble(raw);
		ProductInfoExt saved = dlSales.getProductInfoByCode(product.getCode());
		if (units > 0.0 && saved != null) {
			dlSales.addProductStock(app.getInventoryLocation(), saved.getID(), units,
					Double.valueOf(product.getPriceBuy()));
		}
	}

	private ProductPriceFields createImportPrices(ProductInfoExt product) throws BasicException {
		String preferred = product.getTaxCategoryID();
		if (preferred == null || preferred.isEmpty()) {
			preferred = defaultTaxCategoryId;
		}
		return new ProductPriceFields(product, taxCategories, preferred, priceRuleService, priceTaxRegime,
				new ProductPriceFields.TaxRateLookup() {
					public double rate(TaxCategoryInfo category) {
						return taxeslogic.getTaxRate(category, taxDate, customer);
					}
				});
	}

	private JComboBox<CategoryInfo> createImportCategoryCombo(ProductInfoExt catalogProduct) throws BasicException {
		JComboBox<CategoryInfo> category = new JComboBox<CategoryInfo>();
		category.addItem(null);
		CategoryInfo selectedCategory = null;
		java.util.List categories = dlSales.getCategoriesList().list();
		for (Object item : categories) {
			CategoryInfo availableCategory = (CategoryInfo) item;
			category.addItem(availableCategory);
			if (catalogProduct != null && availableCategory.getID().equals(catalogProduct.getCategoryID())) {
				selectedCategory = availableCategory;
			}
		}
		if (catalogProduct != null && catalogProduct.getCategoryID() != null
				&& !catalogProduct.getCategoryID().isEmpty() && selectedCategory == null) {
			selectedCategory = new CategoryInfo(catalogProduct.getCategoryID(),
					catalogProduct.getProperty("catalog.category.name", catalogProduct.getCategoryID()), null);
			category.addItem(selectedCategory);
		}
		category.setSelectedItem(selectedCategory);
		return category;
	}

	private final class VariantImportEditor {
		private final ProductInfoExt product;
		private final boolean scanned;
		private boolean selected = true;
		private final JTextField name;
		private final JTextField reference;
		private final JComboBox<CategoryInfo> category;
		private final ProductPriceFields prices;
		private final JTextField stock;
		private final JPanel panel;
		private Runnable changeListener;
		private Runnable priceChangeListener;
		private boolean stockInvalid;

		private VariantImportEditor(ProductInfoExt product, boolean scanned,
				JToggleButton.ToggleButtonModel applyPriceModel) throws BasicException {
			this.product = product;
			this.scanned = scanned;
			name = new JTextField(product.getName(), 24);
			name.setCaretPosition(0);
			String initialReference = product.getReference();
			if (initialReference == null || initialReference.trim().isEmpty()) {
				initialReference = product.getCode();
			}
			reference = new JTextField(initialReference, 16);
			reference.setCaretPosition(0);
			category = createImportCategoryCombo(product);
			prices = createImportPrices(product);
			stock = ProductFormLayout.numberField(true);
			panel = buildPanel(applyPriceModel);
			DocumentListener changed = new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent event) {
					changed();
				}

				@Override
				public void removeUpdate(DocumentEvent event) {
					changed();
				}

				@Override
				public void changedUpdate(DocumentEvent event) {
					changed();
				}
			};
			name.getDocument().addDocumentListener(changed);
			prices.buy.getDocument().addDocumentListener(changed);
			prices.sellTax.getDocument().addDocumentListener(changed);
			prices.sellTax.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent event) {
					priceChanged();
				}

				@Override
				public void removeUpdate(DocumentEvent event) {
					priceChanged();
				}

				@Override
				public void changedUpdate(DocumentEvent event) {
					priceChanged();
				}
			});
		}

		private JPanel buildPanel(JToggleButton.ToggleButtonModel applyPriceModel) {
			JPanel fields = new JPanel(new GridBagLayout());
			ProductFormLayout.addRow(fields, 0, AppLocal.getIntString("label.prodname") + ":", name);
			ProductFormLayout.addRow(fields, 1, AppLocal.getIntString("label.prodcategory") + ":", category);
			ProductFormLayout.addRow(fields, 2, AppLocal.getIntString("label.prodref") + ":", reference);
			ProductFormLayout.addRow(fields, 3, AppLocal.getIntString("label.prodpricebuy") + ":", prices.buy);
			ProductFormLayout.addRow(fields, 4, prices.secondaryLabel() + ":", prices.secondary);
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.insets = ProductFormLayout.ROW_INSETS;
			constraints.anchor = GridBagConstraints.WEST;
			constraints.fill = GridBagConstraints.HORIZONTAL;
			constraints.gridx = 1;
			constraints.gridy = 5;
			fields.add(prices.offer, constraints);
			ProductFormLayout.addRow(fields, 6, AppLocal.getIntString("label.prodpriceselltax") + ":",
					prices.priceBlock());

			JCheckBox applyPrice = new JCheckBox(AppLocal.getIntString("label.variants.applyprice"));
			applyPrice.setModel(applyPriceModel);
			applyPrice.setToolTipText(AppLocal.getIntString("label.variants.applyprice.hint"));
			constraints.gridy = 7;
			fields.add(applyPrice, constraints);

			ProductFormLayout.addRow(fields, 8, AppLocal.getIntString("label.taxcategory") + ":", prices.tax);
			ProductFormLayout.addRow(fields, 9, AppLocal.getIntString("label.prodstock") + ":", stock);

			JLabel headingLabel = new JLabel("<html><b>" + variantLabel(product, scanned) + "</b></html>");
			JLabel codeLabel = new JLabel(product.getCode());
			codeLabel.setEnabled(false);
			JPanel header = new JPanel(new BorderLayout());
			header.add(headingLabel, BorderLayout.NORTH);
			header.add(codeLabel, BorderLayout.CENTER);

			JPanel result = new JPanel(new BorderLayout(0, 8));
			result.add(header, BorderLayout.NORTH);
			result.add(ProductFormLayout.topAligned(fields), BorderLayout.CENTER);
			return result;
		}

		private void changed() {
			if (changeListener != null) {
				changeListener.run();
			}
		}

		private void priceChanged() {
			if (priceChangeListener != null) {
				priceChangeListener.run();
			}
		}

		private ProductInfoExt buildProduct() {
			stockInvalid = hasInvalidImportStock(stock.getText());
			if (stockInvalid) {
				return null;
			}
			return buildEditedProduct(product, product, reference, name, category, prices, stock);
		}
	}

	private final class VariantTableModel extends AbstractTableModel {
		private final List<VariantImportEditor> editors;
		private final String[] columns = { "", AppLocal.getIntString("label.variant"),
				AppLocal.getIntString("label.variants.cost"), AppLocal.getIntString("label.variants.price") };

		private VariantTableModel(List<VariantImportEditor> editors) {
			this.editors = editors;
			for (int i = 0; i < editors.size(); i++) {
				final int row = i;
				editors.get(i).changeListener = new Runnable() {
					@Override
					public void run() {
						fireTableRowsUpdated(row, row);
					}
				};
			}
		}

		@Override
		public int getRowCount() {
			return editors.size();
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
		public Class<?> getColumnClass(int column) {
			return column == 0 ? Boolean.class : String.class;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return column == 0 && !editors.get(row).scanned;
		}

		@Override
		public Object getValueAt(int row, int column) {
			VariantImportEditor editor = editors.get(row);
			switch (column) {
			case 0:
				return Boolean.valueOf(editor.selected);
			case 1:
				return variantLabel(editor.product, editor.scanned);
			case 2:
				return editor.prices.buy.getText();
			case 3:
				return editor.prices.sellTax.getText();
			default:
				return "";
			}
		}

		@Override
		public void setValueAt(Object value, int row, int column) {
			if (column == 0 && !editors.get(row).scanned) {
				editors.get(row).selected = Boolean.TRUE.equals(value);
				fireTableRowsUpdated(row, row);
			}
		}

		private void selectAll() {
			for (VariantImportEditor editor : editors) {
				editor.selected = true;
			}
			fireTableDataChanged();
		}

		private void selectScannedOnly() {
			for (VariantImportEditor editor : editors) {
				editor.selected = editor.scanned;
			}
			fireTableDataChanged();
		}
	}

	private String variantLabel(ProductInfoExt product, boolean scanned) {
		return variantLabel(product) + (scanned ? " " + AppLocal.getIntString("label.scanned") : "");
	}

	private String variantLabel(ProductInfoExt product) {
		String name = product.getName();
		int separator = name == null ? -1 : name.indexOf(" — ");
		if (separator >= 0) {
			int reference = name.lastIndexOf(" [");
			return name.substring(separator + 3, reference > separator ? reference : name.length());
		}
		return product.getReference();
	}
}
