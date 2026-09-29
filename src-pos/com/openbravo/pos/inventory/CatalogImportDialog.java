package com.openbravo.pos.inventory;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.KeyStroke;
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

	private static final Logger LOGGER = Logger.getLogger(CatalogImportDialog.class.getName());

	public enum Copy {
		RECEIPT, STOCK, RECEIVING
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
	private List<CategoryInfo> importCategories;
	private boolean cancelled;
	private boolean unknown;

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
		return forStockOrReceiving(parent, app, dlSales, Copy.STOCK);
	}

	public static CatalogImportDialog forReceiving(Component parent, AppView app, DataLogicSales dlSales)
			throws BasicException {
		return forStockOrReceiving(parent, app, dlSales, Copy.RECEIVING);
	}

	private static CatalogImportDialog forStockOrReceiving(Component parent, AppView app, DataLogicSales dlSales,
			Copy copy) throws BasicException {
		PriceRuleService priceRules = new PriceRuleService(app.getSession());
		TaxRegime regime;
		try {
			regime = priceRules.getTaxRegime();
		} catch (java.sql.SQLException e) {
			throw new BasicException(AppLocal.getIntString("message.pricerules.loaderror"), e);
		}
		return new CatalogImportDialog(parent, app, dlSales, new TaxesLogic(dlSales.getTaxList().list()), new Date(),
				null, dlSales.getTaxCategoriesList().list(), "001", priceRules, regime, copy);
	}

	public boolean wasCancelled() {
		return cancelled;
	}

	public boolean wasUnknown() {
		return unknown;
	}

	public ProductInfoExt importIfAbsent(String code) throws BasicException {
		cancelled = false;
		unknown = false;
		LOGGER.log(Level.INFO, "event=catalog_import_start copy={0} code=\"{1}\"", new Object[]{copy, code});
		ProductInfoExt product = dlSales.getProductInfoByCode(code);
		// An alias found by the sales lookup is not an unknown catalogue product.
		// A receiving scan accepts only PRODUCTS.CODE and must not import it again.
		if (copy == Copy.RECEIVING && product != null && !code.equals(product.getCode())) {
			unknown = true;
			return null;
		}
		if (product != null) {
			LOGGER.log(Level.INFO, "event=catalog_import_existing_product code=\"{0}\"", code);
			return product;
		}
		ProductInfoExt catalogProduct = dlSales.getCatalogProductByCode(code, null, null);
		if (catalogProduct == null && copy != Copy.STOCK) {
			unknown = true;
			LOGGER.log(Level.INFO, "event=catalog_fallback_miss code=\"{0}\"", code);
			return null;
		}
		if (catalogProduct == null) {
			LOGGER.log(Level.INFO, "event=catalog_fallback_miss code=\"{0}\" mode=stock", code);
		} else {
			LOGGER.log(Level.INFO, "event=catalog_fallback_match code=\"{0}\" family=\"{1}\"",
					new Object[]{code, catalogProduct.getFamily()});
		}
		// Receiving only creates the scanned product. Family import can update the
		// prices of variants already in the catalogue; that is a separate job.
		List<ProductInfoExt> family = catalogProduct == null || copy == Copy.RECEIVING
				? new ArrayList<ProductInfoExt>()
				: dlSales.getCatalogProductFamily(code, null, null);
		if (family.size() > 1) {
			LOGGER.log(Level.INFO, "event=catalog_fallback_family_match code=\"{0}\" family=\"{1}\" variants={2}",
					new Object[]{code, catalogProduct.getFamily(), family.size()});
			List<ProductInfoExt> editedFamily = editProductFamilyForImport(code, family);
			if (editedFamily == null) {
				cancelled = true;
				LOGGER.log(Level.INFO, "event=catalog_import_cancelled code=\"{0}\" reason=family_edit", code);
				return null;
			}
			dlSales.importProducts(editedFamily, null);
			for (ProductInfoExt imported : editedFamily) {
				applyImportedStock(imported);
			}
			LOGGER.log(Level.INFO, "event=catalog_import_family_success code=\"{0}\" variants={1}",
					new Object[]{code, editedFamily.size()});
			if (copy == Copy.RECEIVING) {
				for (ProductInfoExt variant : editedFamily)
					if (code.equals(variant.getCode()) || ("0" + code).equals(variant.getCode())
							|| ("00" + code).equals(variant.getCode()))
						return importedByExactCode(variant.getCode());
			}
			return dlSales.getProductInfoByCode(code);
		}
		ProductInfoExt editedProduct = editProductForImport(code, catalogProduct);
		if (editedProduct == null) {
			cancelled = true;
			LOGGER.log(Level.INFO, "event=catalog_import_cancelled code=\"{0}\" reason=product_edit", code);
			return null;
		}
		product = dlSales.importProduct(editedProduct, editedProduct.getProperty("catalog.brand"), null);
		applyImportedStock(editedProduct);
		LOGGER.log(Level.INFO, "event=catalog_import_success code=\"{0}\" family=\"{1}\"",
				new Object[]{code, editedProduct.getFamily()});
		return copy == Copy.RECEIVING ? importedByExactCode(editedProduct.getCode()) : product;
	}

	private ProductInfoExt importedByExactCode(String code) throws BasicException {
		try (java.sql.PreparedStatement find = app.getSession().getConnection()
				.prepareStatement("SELECT ID FROM PRODUCTS WHERE CODE=?")) {
			find.setString(1, code);
			try (java.sql.ResultSet rows = find.executeQuery()) {
				if (rows.next())
					return dlSales.getProductInfo(rows.getString(1));
			}
		} catch (java.sql.SQLException e) {
			throw new BasicException("Cannot locate imported barcode " + code, e);
		}
		throw new BasicException("Cannot locate imported barcode " + code);
	}

	private String confirmLabel(boolean family) {
		if (copy == Copy.RECEIVING)
			return AppLocal.getIntString(family ? "receiving.importFamily" : "receiving.importProduct");
		if (copy != Copy.RECEIPT) {
			return AppLocal.getIntString(family ? "button.createfamily" : "button.createproduct");
		}
		return AppLocal.getIntString(family ? "button.addfamilytoreceipt" : "button.addtoreceipt");
	}

	private String familyMessageKey() {
		if (copy == Copy.RECEIVING)
			return "receiving.importFamilyHint";
		return copy != Copy.RECEIPT ? "message.importproductfamily.stock" : "message.importproductfamily";
	}

	private String importMessageKey(boolean fromCatalog) {
		if (copy == Copy.RECEIVING)
			return "receiving.importHint";
		if (copy != Copy.RECEIPT) {
			return fromCatalog ? "message.importproduct.stock" : "message.importproduct.unknown.stock";
		}
		return fromCatalog ? "message.importproduct" : "message.importproduct.unknown";
	}

	private List<ProductInfoExt> editProductFamilyForImport(String code, List<ProductInfoExt> family)
			throws BasicException {
		LOGGER.log(Level.INFO, "event=catalog_family_editor_start code=\"{0}\" variants={1}",
				new Object[]{code, family.size()});
		// One model shared by the checkbox each variant card draws under its price,
		// so the choice follows the cashier from card to card.
		final JToggleButton.ToggleButtonModel applyPriceModel = new JToggleButton.ToggleButtonModel();
		applyPriceModel.setSelected(true);
		final List<VariantImportState> variants = new ArrayList<VariantImportState>();
		int scannedRow = 0;
		for (int i = 0; i < family.size(); i++) {
			ProductInfoExt variant = family.get(i);
			boolean scanned = code.equals(variant.getCode()) || ("0" + code).equals(variant.getCode())
					|| ("00" + code).equals(variant.getCode());
			if (scanned) {
				scannedRow = i;
			}
			variants.add(new VariantImportState(variant, scanned));
		}

		final VariantImportEditor editor = new VariantImportEditor(variants.get(scannedRow));
		editor.load(variants.get(scannedRow));
		final VariantTableModel model = new VariantTableModel(variants);
		final JTable table = new JTable(model);
		final boolean[] applyingFamilyPrice = new boolean[]{false};
		editor.priceChangeListener = new Runnable() {
			@Override
			public void run() {
				if (!applyPriceModel.isSelected() || applyingFamilyPrice[0] || editor.prices.reportlock) {
					return;
				}
				Double gross = ProductPriceMath.parsePositiveCurrency(editor.prices.sellTax.getText(), false);
				if (gross == null) {
					return;
				}
				applyingFamilyPrice[0] = true;
				try {
					editor.save(editor.current);
					for (VariantImportState target : variants) {
						if (target != editor.current && target.selected) {
							target.grossPrice = editor.prices.sellTax.getText();
						}
					}
					model.fireTableDataChanged();
				} finally {
					applyingFamilyPrice[0] = false;
				}
			}
		};
		final boolean[] applyingFamilyCategory = new boolean[]{false};
		editor.categoryChangeListener = new Runnable() {
			@Override
			public void run() {
				if (applyingFamilyCategory[0]) {
					return;
				}
				CategoryInfo selectedCategory = (CategoryInfo) editor.category.getSelectedItem();
				String categoryId = selectedCategory == null ? null : selectedCategory.getID();
				applyingFamilyCategory[0] = true;
				try {
					editor.save(editor.current);
					for (VariantImportState target : variants) {
						if (target != editor.current && target.selected) {
							target.categoryId = categoryId;
							target.category = selectedCategory;
						}
					}
				} finally {
					applyingFamilyCategory[0] = false;
				}
			}
		};
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(28);
		table.getColumnModel().getColumn(0).setMaxWidth(42);
		table.getColumnModel().getColumn(1).setPreferredWidth(190);
		table.getColumnModel().getColumn(2).setPreferredWidth(90);
		table.getColumnModel().getColumn(3).setPreferredWidth(90);

		final JPanel cards = new JPanel(new CardLayout());
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent event) {
				int selected = table.getSelectedRow();
				if (!event.getValueIsAdjusting() && selected >= 0) {
					editor.load(variants.get(selected));
					focusPriceField(editor.stock);
				}
			}
		});
		table.setRowSelectionInterval(scannedRow, scannedRow);
		cards.add(editor.getPanel(applyPriceModel), "editor");
		focusImportField(editor.stock);

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
		enlargeDialogFont(content);
		LOGGER.log(Level.INFO, "event=catalog_family_dialog_ready code=\"{0}\" variants={1}",
				new Object[]{code, family.size()});

		String title = AppLocal.getIntString("title.importproductfamily");
		Object[] options = new Object[]{confirmLabel(true), AppLocal.getIntString("button.skipitem")};
		while (true) {
			int result = showImportOptionDialog(content, title, options);
			if (result != 0) {
				return null;
			}
			List<ProductInfoExt> selected = new ArrayList<ProductInfoExt>();
			for (int i = 0; i < variants.size(); i++) {
				VariantImportState variant = variants.get(i);
				if (!variant.selected) {
					continue;
				}
				editor.load(variant);
				ProductInfoExt product = editor.buildProduct();
				if (product == null) {
					table.setRowSelectionInterval(i, i);
					String message = editor.stockInvalid
							? AppLocal.getIntString("message.stockaddpositive")
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
		if (copy != Copy.RECEIVING)
			ProductFormLayout.addRow(fields, 8, AppLocal.getIntString("label.prodstock") + ":", stock);

		JPanel content = new JPanel(new BorderLayout(0, 12));
		content.add(buildImportMessage(code, catalogProduct != null), BorderLayout.NORTH);
		content.add(ProductFormLayout.topAligned(fields), BorderLayout.CENTER);
		focusImportField(stock);
		enlargeDialogFont(content);

		String title = AppLocal.getIntString("title.importproduct");
		Object[] options = new Object[]{confirmLabel(false), AppLocal.getIntString("button.skipitem")};
		while (true) {
			int result = showImportOptionDialog(content, title, options);
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

	private int showImportOptionDialog(JComponent content, String title, Object[] options) {
		final JOptionPane optionPane = new JOptionPane(content, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION,
				null, options, null);
		final JDialog dialog = optionPane.createDialog(parent, title);
		dialog.getRootPane().setDefaultButton(null);
		dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ENTER"),
				"ignoreEnter");
		dialog.getRootPane().getActionMap().put("ignoreEnter", new javax.swing.AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent event) {
			}
		});
		dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ctrl ENTER"),
				"confirmImport");
		dialog.getRootPane().getActionMap().put("confirmImport", new javax.swing.AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent event) {
				optionPane.setValue(options[0]);
				dialog.dispose();
			}
		});
		dialog.setVisible(true);
		Object value = optionPane.getValue();
		return options[0].equals(value) ? 0 : 1;
	}

	private void enlargeDialogFont(JComponent component) {
		Font font = component.getFont();
		if (font != null) {
			component.setFont(font.deriveFont(16f));
		}
		if (component instanceof JTable) {
			JTable table = (JTable) component;
			if (table.getTableHeader() != null && table.getTableHeader().getFont() != null) {
				table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(16f));
			}
		}
		for (Component child : component.getComponents()) {
			if (child instanceof JComponent) {
				enlargeDialogFont((JComponent) child);
			}
		}
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
		edited.setFamily(availableProduct.getFamily());
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
		if (copy == Copy.RECEIVING)
			return;
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
		for (CategoryInfo availableCategory : getImportCategories()) {
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

	private List<CategoryInfo> getImportCategories() throws BasicException {
		if (importCategories == null) {
			List<CategoryInfo> loaded = new ArrayList<CategoryInfo>();
			for (Object item : dlSales.getCategoriesList().list()) {
				loaded.add((CategoryInfo) item);
			}
			importCategories = loaded;
		}
		return importCategories;
	}

	private final class VariantImportState {
		private final ProductInfoExt product;
		private final boolean scanned;
		private boolean selected = true;
		private String name;
		private String reference;
		private CategoryInfo category;
		private String categoryId;
		private String buy;
		private String grossPrice;
		private String taxId;
		private String stock = "";

		private VariantImportState(ProductInfoExt product, boolean scanned) {
			this.product = product;
			this.scanned = scanned;
			this.name = product.getName();
			this.reference = product.getReference();
			if (Boolean.parseBoolean(product.getProperty("catalog.price.available", "false"))) {
				this.buy = ProductPriceMath.formatCurrency(Double.valueOf(product.getPriceBuy()));
			}
			if (product.getPriceSell() > 0.0) {
				this.grossPrice = ProductPriceMath.formatCurrency(Double.valueOf(product.getPriceSell()));
			}
		}
	}

	private final class VariantImportEditor {
		private final JTextField name = new JTextField(24);
		private final JTextField reference = new JTextField(16);
		private final JComboBox<CategoryInfo> category;
		private final ProductPriceFields prices;
		private final JTextField stock = ProductFormLayout.numberField(true);
		private JPanel panel;
		private final JLabel headingLabel = new JLabel();
		private final JLabel codeLabel = new JLabel();
		private VariantImportState current;
		private boolean loading;
		private Runnable priceChangeListener;
		private Runnable categoryChangeListener;
		private boolean stockInvalid;

		private VariantImportEditor(VariantImportState initial) throws BasicException {
			category = createImportCategoryCombo(initial.product);
			prices = createImportPrices(initial.product);
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
			reference.getDocument().addDocumentListener(changed);
			prices.buy.getDocument().addDocumentListener(changed);
			prices.sellTax.getDocument().addDocumentListener(changed);
			stock.getDocument().addDocumentListener(changed);
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
			category.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					if (!loading && categoryChangeListener != null)
						categoryChangeListener.run();
				}
			});
		}

		private void load(VariantImportState variant) {
			loading = true;
			try {
				current = variant;
				name.setText(variant.name == null ? variant.product.getName() : variant.name);
				String initialReference = variant.reference;
				if (initialReference == null || initialReference.trim().isEmpty())
					initialReference = variant.product.getCode();
				reference.setText(initialReference);
				selectCategory(variant.category == null ? variant.product.getCategoryID() : variant.category.getID());
				String preferredTax = variant.taxId == null ? variant.product.getTaxCategoryID() : variant.taxId;
				if (preferredTax == null || preferredTax.isEmpty())
					preferredTax = defaultTaxCategoryId;
				selectTax(preferredTax);
				if (variant.buy != null) {
					prices.buy.setText(variant.buy);
				} else if (Boolean.parseBoolean(variant.product.getProperty("catalog.price.available", "false"))) {
					prices.buy.setText(ProductPriceMath.formatCurrency(Double.valueOf(variant.product.getPriceBuy())));
				} else {
					prices.buy.setText("");
				}
				if (variant.grossPrice != null) {
					prices.setGrossPrice(ProductPriceMath.parseCurrency(variant.grossPrice));
				} else if (!Boolean.parseBoolean(variant.product.getProperty("catalog.price.available", "false"))) {
					prices.sellTax.setText("");
				}
				stock.setText(variant.stock);
				headingLabel.setText("<html><b>" + variantLabel(variant.product, variant.scanned) + "</b></html>");
				codeLabel.setText(variant.product.getCode());
			} finally {
				loading = false;
			}
		}

		private void selectCategory(String categoryId) {
			for (int i = 0; i < category.getItemCount(); i++) {
				CategoryInfo candidate = category.getItemAt(i);
				if (candidate != null && candidate.getID().equals(categoryId)) {
					category.setSelectedIndex(i);
					return;
				}
			}
			category.setSelectedItem(null);
		}

		private void selectTax(String taxId) {
			for (int i = 0; i < prices.tax.getItemCount(); i++) {
				TaxCategoryInfo tax = (TaxCategoryInfo) prices.tax.getItemAt(i);
				if (taxId.equals(tax.getID())) {
					prices.tax.setSelectedIndex(i);
					return;
				}
			}
		}

		private void save(VariantImportState variant) {
			if (variant == null)
				return;
			variant.name = name.getText();
			variant.reference = reference.getText();
			variant.category = (CategoryInfo) category.getSelectedItem();
			variant.categoryId = variant.category == null ? null : variant.category.getID();
			variant.buy = prices.buy.getText();
			variant.grossPrice = prices.sellTax.getText();
			TaxCategoryInfo tax = (TaxCategoryInfo) prices.tax.getSelectedItem();
			variant.taxId = tax == null ? null : tax.getID();
			variant.stock = stock.getText();
		}

		private JPanel getPanel(JToggleButton.ToggleButtonModel applyPriceModel) {
			if (panel != null)
				return panel;
			JCheckBox applyPrice = new JCheckBox(AppLocal.getIntString("label.variants.applyprice"));
			applyPrice.setModel(applyPriceModel);
			applyPrice.setToolTipText(AppLocal.getIntString("label.variants.applyprice.hint"));
			panel = buildPanel(applyPrice);
			return panel;
		}

		private JPanel buildPanel(JCheckBox applyPrice) {
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

			constraints.gridy = 7;
			fields.add(applyPrice, constraints);

			ProductFormLayout.addRow(fields, 8, AppLocal.getIntString("label.taxcategory") + ":", prices.tax);
			if (copy != Copy.RECEIVING)
				ProductFormLayout.addRow(fields, 9, AppLocal.getIntString("label.prodstock") + ":", stock);

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
			if (!loading && current != null)
				save(current);
		}

		private void priceChanged() {
			if (!loading && priceChangeListener != null) {
				priceChangeListener.run();
			}
		}

		private ProductInfoExt buildProduct() {
			save(current);
			stockInvalid = hasInvalidImportStock(stock.getText());
			if (stockInvalid) {
				return null;
			}
			return buildEditedProduct(current.product, current.product, reference, name, category, prices, stock);
		}
	}

	private final class VariantTableModel extends AbstractTableModel {
		private final List<VariantImportState> variants;
		private final String[] columns = {"", AppLocal.getIntString("label.variant"),
				AppLocal.getIntString("label.variants.cost"), AppLocal.getIntString("label.variants.price")};

		private VariantTableModel(List<VariantImportState> variants) {
			this.variants = variants;
		}

		@Override
		public int getRowCount() {
			return variants.size();
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
			return column == 0 && !variants.get(row).scanned;
		}

		@Override
		public Object getValueAt(int row, int column) {
			VariantImportState variant = variants.get(row);
			switch (column) {
				case 0 :
					return Boolean.valueOf(variant.selected);
				case 1 :
					return variantLabel(variant.product, variant.scanned);
				case 2 :
					return variant.buy == null ? "" : variant.buy;
				case 3 :
					return variant.grossPrice == null ? "" : variant.grossPrice;
				default :
					return "";
			}
		}

		@Override
		public void setValueAt(Object value, int row, int column) {
			if (column == 0 && !variants.get(row).scanned) {
				variants.get(row).selected = Boolean.TRUE.equals(value);
				fireTableRowsUpdated(row, row);
			}
		}

		private void selectAll() {
			for (VariantImportState variant : variants) {
				variant.selected = true;
			}
			fireTableDataChanged();
		}

		private void selectScannedOnly() {
			for (VariantImportState variant : variants) {
				variant.selected = variant.scanned;
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
