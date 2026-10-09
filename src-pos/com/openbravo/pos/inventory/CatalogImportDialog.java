package com.openbravo.pos.inventory;

import java.awt.BorderLayout;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.RenderingHints;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.Icon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
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
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.DefaultCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.border.Border;

import com.openbravo.pos.theme.RetailPOSColors;

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
		// Receiving only adds the scanned product to the receipt. The selected
		// family variants are still created in the catalogue for later scans.
		List<ProductInfoExt> family = familyForImport(code, catalogProduct, dlSales);
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
				ProductInfoExt scannedVariant = receivingVariant(editedFamily, code);
				if (scannedVariant != null)
					return importedByExactCode(scannedVariant.getCode());
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

	static List<ProductInfoExt> familyForImport(String code, ProductInfoExt catalogProduct, DataLogicSales dlSales)
			throws BasicException {
		return familyForImport(code, catalogProduct, dlSales, null, null);
	}

	static List<ProductInfoExt> familyForImport(String code, ProductInfoExt catalogProduct, DataLogicSales dlSales,
			String productsPath, String categoriesPath) throws BasicException {
		return catalogProduct == null
				? new ArrayList<ProductInfoExt>()
				: dlSales.getCatalogProductFamily(code, productsPath, categoriesPath);
	}

	static ProductInfoExt receivingVariant(List<ProductInfoExt> importedFamily, String code) {
		for (ProductInfoExt variant : importedFamily)
			if (code.equals(variant.getCode()) || ("0" + code).equals(variant.getCode())
					|| ("00" + code).equals(variant.getCode()))
				return variant;
		return null;
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
		// The checkbox follows the cashier from variant to variant.
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
		model.stockChanged = new java.util.function.BiConsumer<Integer, String>() {
			@Override
			public void accept(Integer row, String value) {
				if (editor.current == variants.get(row.intValue()))
					editor.stock.setText(value);
			}
		};
		final JButton confirm = touchButton(confirmLabel(true));
		confirm.setBackground(RetailPOSColors.brand());
		confirm.setForeground(RetailPOSColors.onBrand());
		final JLabel status = new JLabel();
		final Runnable refreshStatus = new Runnable() {
			@Override
			public void run() {
				int count = 0;
				for (VariantImportState variant : variants)
					if (variant.selected)
						count++;
				VariantImportState scanned = null;
				for (VariantImportState variant : variants)
					if (variant.scanned)
						scanned = variant;
				boolean priced = scanned != null
						&& ProductPriceMath.parsePositiveCurrency(scanned.grossPrice, false) != null;
				confirm.setEnabled(priced);
				confirm.setBackground(priced ? RetailPOSColors.brand() : RetailPOSColors.surface200());
				confirm.setForeground(priced ? RetailPOSColors.onBrand() : RetailPOSColors.inkMuted());
				boolean samePrice = priced;
				for (VariantImportState variant : variants)
					if (samePrice && variant.selected && !scanned.grossPrice.equals(variant.grossPrice))
						samePrice = false;
				status.setText(AppLocal.getIntString(
						!priced
								? "label.variants.status.missing"
								: samePrice ? "label.variants.status.sameprice" : "label.variants.status.ready",
						Integer.valueOf(count), scanned == null ? "" : scanned.grossPrice));
				editor.refreshPriceHint();
			}
		};
		model.onChange = refreshStatus;
		editor.onChange = new Runnable() {
			@Override
			public void run() {
				int row = variants.indexOf(editor.current);
				if (row >= 0)
					model.fireTableRowsUpdated(row, row);
				refreshStatus.run();
			}
		};
		final boolean[] applyingFamilyPrice = new boolean[]{false};
		editor.priceChangeListener = new Runnable() {
			@Override
			public void run() {
				if (!applyPriceModel.isSelected() || applyingFamilyPrice[0] || editor.prices.reportlock) {
					refreshStatus.run();
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
					model.fireTableRowsUpdated(0, variants.size() - 1);
					refreshStatus.run();
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
		table.setRowHeight(48);
		table.setShowGrid(false);
		table.setIntercellSpacing(new Dimension(0, 0));
		table.setBackground(RetailPOSColors.surface100());
		table.getTableHeader().setBackground(RetailPOSColors.surface100());
		table.getTableHeader().setForeground(RetailPOSColors.inkMuted());
		table.getTableHeader().setFont(table.getTableHeader().getFont().deriveFont(13f));
		table.getColumnModel().getColumn(0).setMaxWidth(48);
		table.getColumnModel().getColumn(1).setPreferredWidth(245);
		table.getColumnModel().getColumn(2).setPreferredWidth(85);
		table.getColumnModel().getColumn(3).setPreferredWidth(82);
		JTextField rowStock = ProductFormLayout.numberField(true, 5);
		rowStock.setHorizontalAlignment(SwingConstants.CENTER);
		rowStock.setBackground(RetailPOSColors.surface200());
		rowStock.putClientProperty("JComponent.roundRect", Boolean.TRUE);
		table.getColumnModel().getColumn(3).setCellEditor(new DefaultCellEditor(rowStock) {
			@Override
			public Component getTableCellEditorComponent(JTable source, Object value, boolean selected, int row,
					int column) {
				Component field = super.getTableCellEditorComponent(source, value, selected, row, column);
				SwingUtilities.invokeLater(() -> {
					if (source.isEditing() && source.getEditingRow() == row && source.getEditingColumn() == column)
						rowStock.selectAll();
				});
				return field;
			}
		});
		JCheckBox rowCheckEditor = variantCheckBox();
		rowCheckEditor.setHorizontalAlignment(SwingConstants.CENTER);
		table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(rowCheckEditor));
		table.getColumnModel().getColumn(0).setCellRenderer(new TableCellRenderer() {
			private final JPanel cell = new JPanel(new BorderLayout());
			private final JCheckBox check = variantCheckBox();
			{
				check.setHorizontalAlignment(SwingConstants.CENTER);
				check.setOpaque(false);
				cell.add(check, BorderLayout.CENTER);
			}
			@Override
			public Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
					boolean focused, int row, int column) {
				check.setSelected(Boolean.TRUE.equals(value));
				cell.setBackground(variantRowBackground(variants.get(row), selected, row));
				cell.setBorder(variantRowBorder(variants.get(row), 0, 3));
				return cell;
			}
		});
		DefaultTableCellRenderer rowRenderer = new DefaultTableCellRenderer() {
			@Override
			public Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
					boolean focused, int row, int column) {
				super.getTableCellRendererComponent(source, value, selected, focused, row, column);
				VariantImportState variant = variants.get(row);
				setBackground(variantRowBackground(variant, selected, row));
				setForeground(
						column == 2 && variant.scanned && AppLocal.getIntString("label.variants.missing").equals(value)
								? RetailPOSColors.dangerText()
								: RetailPOSColors.ink());
				setBorder(variantRowBorder(variant, column, 3));
				setHorizontalAlignment(column == 1 ? SwingConstants.LEFT : SwingConstants.CENTER);
				return this;
			}
		};
		for (int column = 1; column < 3; column++)
			table.getColumnModel().getColumn(column).setCellRenderer(rowRenderer);
		table.getColumnModel().getColumn(3).setCellRenderer(new TableCellRenderer() {
			private final JPanel cell = new JPanel(new BorderLayout());
			private final JPanel well = new JPanel(new BorderLayout()) {
				@Override
				protected void paintComponent(Graphics graphics) {
					Graphics2D pen = (Graphics2D) graphics.create();
					try {
						pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
						pen.setColor(RetailPOSColors.surface200());
						pen.fillRoundRect(1, 1, getWidth() - 2, getHeight() - 2, 8, 8);
						pen.setColor(RetailPOSColors.border());
						pen.drawRoundRect(1, 1, getWidth() - 3, getHeight() - 3, 8, 8);
					} finally {
						pen.dispose();
					}
				}
			};
			private final JLabel valueLabel = new JLabel("", SwingConstants.CENTER);
			{
				well.setOpaque(false);
				well.add(valueLabel, BorderLayout.CENTER);
				cell.add(well, BorderLayout.CENTER);
			}
			@Override
			public Component getTableCellRendererComponent(JTable source, Object value, boolean selected,
					boolean focused, int row, int column) {
				cell.setBackground(variantRowBackground(variants.get(row), selected, row));
				cell.setBorder(BorderFactory.createCompoundBorder(variantRowBorder(variants.get(row), 3, 3),
						BorderFactory.createEmptyBorder(4, 8, 4, 8)));
				valueLabel.setForeground(RetailPOSColors.ink());
				valueLabel.setFont(source.getFont());
				valueLabel.setText(value.toString());
				return cell;
			}
		});

		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent event) {
				int selected = table.getSelectedRow();
				if (!event.getValueIsAdjusting() && selected >= 0) {
					if (table.isEditing())
						table.getCellEditor().stopCellEditing();
					editor.load(variants.get(selected));
				}
			}
		});
		table.setRowSelectionInterval(scannedRow, scannedRow);
		JPanel detail = editor.getPanel(applyPriceModel);
		focusImportField(editor.prices.sellTax);

		JButton selectAll = touchButton(AppLocal.getIntString("button.variants.all"));
		selectAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				model.selectAll();
			}
		});
		JButton scannedOnly = touchButton(AppLocal.getIntString("button.variants.scanned"));
		scannedOnly.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				model.selectScannedOnly();
			}
		});
		JPanel listButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
		listButtons.add(selectAll);
		listButtons.add(scannedOnly);

		JPanel variantsPanel = new JPanel(new BorderLayout(0, 6));
		variantsPanel.add(listButtons, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(table);
		// Give the variant list another 48px touch row of vertical space.
		scroll.setPreferredSize(new Dimension(460, 440));
		scroll.setBorder(BorderFactory.createLineBorder(RetailPOSColors.border()));
		scroll.getViewport().setBackground(RetailPOSColors.surface100());
		variantsPanel.add(scroll, BorderLayout.CENTER);

		JPanel body = new JPanel(new GridBagLayout());
		GridBagConstraints placement = new GridBagConstraints();
		placement.gridx = 0;
		placement.weightx = 0.55;
		placement.weighty = 1;
		placement.fill = GridBagConstraints.BOTH;
		body.add(variantsPanel, placement);
		placement.gridx = 1;
		placement.weightx = 0;
		body.add(Box.createHorizontalStrut(32), placement);
		placement.gridx = 2;
		placement.weightx = 0.45;
		body.add(detail, placement);

		JPanel content = new JPanel(new BorderLayout(0, 16));
		JPanel header = new JPanel(new BorderLayout(16, 0));
		JPanel intro = new JPanel(new BorderLayout(0, 4));
		JLabel heading = new JLabel(AppLocal.getIntString("title.importproductfamily"));
		heading.setFont(heading.getFont().deriveFont(Font.BOLD, 20f));
		intro.add(heading, BorderLayout.NORTH);
		intro.add(buildFamilyImportMessage(), BorderLayout.SOUTH);
		header.add(intro, BorderLayout.CENTER);
		JLabel ean = new JLabel("<html><div style='text-align:right'><small>"
				+ AppLocal.getIntString("label.variants.scannedean") + "</small><br>" + code + "</div></html>");
		ean.setHorizontalAlignment(SwingConstants.RIGHT);
		header.add(ean, BorderLayout.EAST);
		content.add(header, BorderLayout.NORTH);
		content.add(body, BorderLayout.CENTER);
		JButton skip = touchButton(AppLocal.getIntString("button.skipitem"));
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
		actions.add(skip);
		actions.add(confirm);
		JPanel footer = new JPanel(new BorderLayout(12, 0));
		status.setFont(status.getFont().deriveFont(13f));
		status.setForeground(RetailPOSColors.inkMuted());
		footer.setBorder(BorderFactory.createCompoundBorder(
				BorderFactory.createMatteBorder(1, 0, 0, 0, RetailPOSColors.border()),
				BorderFactory.createEmptyBorder(16, 0, 0, 0)));
		footer.add(status, BorderLayout.WEST);
		footer.add(actions, BorderLayout.EAST);
		content.add(footer, BorderLayout.SOUTH);
		refreshStatus.run();
		content.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(RetailPOSColors.border()),
				BorderFactory.createEmptyBorder(24, 32, 24, 32)));
		styleFamilySurface(content);
		LOGGER.log(Level.INFO, "event=catalog_family_dialog_ready code=\"{0}\" variants={1}",
				new Object[]{code, family.size()});

		String title = AppLocal.getIntString("title.importproductfamily");
		Window owner = SwingUtilities.getWindowAncestor(parent);
		final JDialog dialog = new JDialog(owner, AppLocal.APP_NAME, java.awt.Dialog.ModalityType.APPLICATION_MODAL);
		dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
		dialog.getContentPane().add(content);
		dialog.getRootPane().setDefaultButton(null);
		dialog.addWindowListener(new java.awt.event.WindowAdapter() {
			@Override
			public void windowOpened(java.awt.event.WindowEvent event) {
				focusPriceField(editor.prices.sellTax);
			}
		});
		dialog.getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW).put(KeyStroke.getKeyStroke("ESCAPE"),
				"skipVariantImport");
		dialog.getRootPane().getActionMap().put("skipVariantImport", new javax.swing.AbstractAction() {
			@Override
			public void actionPerformed(ActionEvent event) {
				dialog.dispose();
			}
		});
		final boolean[] accepted = {false};
		confirm.addActionListener(event -> {
			if (table.isEditing())
				table.getCellEditor().stopCellEditing();
			accepted[0] = true;
			dialog.setVisible(false);
		});
		skip.addActionListener(event -> dialog.dispose());
		dialog.pack();
		dialog.setLocationRelativeTo(parent);
		while (true) {
			dialog.setVisible(true);
			if (!accepted[0]) {
				dialog.dispose();
				return null;
			}
			accepted[0] = false;
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
				dialog.dispose();
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

	private JButton touchButton(String label) {
		JButton button = new JButton(label);
		Dimension size = button.getPreferredSize();
		button.setPreferredSize(new Dimension(size.width + 16, Math.max(48, size.height)));
		return button;
	}

	private void styleFamilySurface(JComponent component) {
		if (component instanceof JPanel)
			component.setBackground(RetailPOSColors.surface100());
		for (Component child : component.getComponents())
			if (child instanceof JComponent)
				styleFamilySurface((JComponent) child);
	}

	private Color variantRowBackground(VariantImportState variant, boolean selected, int row) {
		if (variant.scanned || row % 2 != 0)
			return RetailPOSColors.surface200();
		return selected ? RetailPOSColors.brandSubtle() : RetailPOSColors.surface100();
	}

	private Border variantRowBorder(VariantImportState variant, int column, int lastColumn) {
		return BorderFactory.createCompoundBorder(
				variant.scanned
						? BorderFactory.createMatteBorder(2, column == 0 ? 2 : 0, 2, column == lastColumn ? 2 : 0,
								RetailPOSColors.borderStrong())
						: BorderFactory.createEmptyBorder(2, column == 0 ? 2 : 0, 2, column == lastColumn ? 2 : 0),
				BorderFactory.createEmptyBorder(0, column == 1 ? 8 : 0, 0, 0));
	}

	private JCheckBox variantCheckBox() {
		JCheckBox check = new JCheckBox();
		check.setIcon(new VariantCheckIcon(false));
		check.setSelectedIcon(new VariantCheckIcon(true));
		return check;
	}

	private static final class VariantCheckIcon implements Icon {
		private final boolean checked;

		private VariantCheckIcon(boolean checked) {
			this.checked = checked;
		}

		@Override
		public int getIconWidth() {
			return 22;
		}

		@Override
		public int getIconHeight() {
			return 22;
		}

		@Override
		public void paintIcon(Component component, Graphics graphics, int x, int y) {
			Graphics2D pen = (Graphics2D) graphics.create();
			try {
				pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
				pen.setColor(checked ? RetailPOSColors.ink() : RetailPOSColors.surface100());
				pen.fillRoundRect(x + 1, y + 1, 20, 20, 3, 3);
				if (checked) {
					pen.setColor(RetailPOSColors.surface100());
					pen.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
					pen.drawLine(x + 5, y + 11, x + 9, y + 15);
					pen.drawLine(x + 9, y + 15, x + 17, y + 6);
				} else {
					pen.setColor(RetailPOSColors.border());
					pen.drawRoundRect(x + 1, y + 1, 20, 20, 3, 3);
				}
			} finally {
				pen.dispose();
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
		private String stock = "0";

		private VariantImportState(ProductInfoExt product, boolean scanned) {
			this.product = product;
			this.scanned = scanned;
			this.name = product.getName();
			this.reference = product.getReference();
			if (Boolean.parseBoolean(product.getProperty("catalog.price.available", "false"))) {
				this.buy = ProductPriceMath.formatCurrency(Double.valueOf(Math.max(0.0, product.getPriceBuy())));
			} else {
				this.buy = ProductPriceMath.formatCurrency(Double.valueOf(0.0));
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
		private final JLabel summaryName = new JLabel();
		private final JLabel summaryReference = new JLabel();
		private final JLabel priceHint = new JLabel();
		private final JPanel identity = new JPanel(new BorderLayout());
		private JPanel summaryCard;
		private JPanel editCard;
		private final JButton editIdentity = new JButton();
		private boolean editingIdentity;
		private VariantImportState current;
		private boolean loading;
		private Runnable priceChangeListener;
		private Runnable categoryChangeListener;
		private Runnable onChange;
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
			editIdentity.addActionListener(event -> {
				editingIdentity = !editingIdentity;
				identity.removeAll();
				identity.add(editingIdentity ? editCard : summaryCard, BorderLayout.CENTER);
				identity.revalidate();
				identity.repaint();
				editIdentity.setText("<html><u>"
						+ AppLocal.getIntString(
								editingIdentity ? "button.variants.closeidentity" : "button.variants.editidentity")
						+ "</u></html>");
				Window window = SwingUtilities.getWindowAncestor(identity);
				if (window != null)
					window.pack();
				if (editingIdentity)
					focusPriceField(name);
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
				prices.buy.setText(variant.buy);
				Double grossPrice = ProductPriceMath.parseCurrency(variant.grossPrice);
				if (grossPrice != null) {
					prices.setGrossPrice(grossPrice.doubleValue());
				} else {
					prices.sellTax.setText("");
				}
				stock.setText(variant.stock);
				headingLabel.setText(AppLocal.getIntString(
						variant.scanned ? "label.variants.scannedheading" : "label.variants.variantheading"));
				updateNameSummary();
				summaryReference.setText(reference.getText());
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
			int fallback = 0;
			for (int i = 0; i < prices.tax.getItemCount(); i++) {
				TaxCategoryInfo tax = (TaxCategoryInfo) prices.tax.getItemAt(i);
				if (tax.getID().equals(defaultTaxCategoryId))
					fallback = i;
				if (tax.getID().equals(taxId)) {
					prices.tax.setSelectedIndex(i);
					return;
				}
			}
			if (prices.tax.getItemCount() > 0)
				prices.tax.setSelectedIndex(fallback);
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
			updateNameSummary();
			summaryReference.setText(variant.reference);
		}

		private void updateNameSummary() {
			String title = name.getText();
			if (title.endsWith(" [" + reference.getText() + "]"))
				title = title.substring(0, title.length() - reference.getText().length() - 3);
			summaryName.setText(title);
			summaryName.setToolTipText(name.getText());
		}

		private JPanel getPanel(JToggleButton.ToggleButtonModel applyPriceModel) {
			if (panel != null)
				return panel;
			JCheckBox applyPrice = variantCheckBox();
			applyPrice.setText(AppLocal.getIntString("label.variants.applyprice"));
			applyPrice.setOpaque(false);
			applyPrice.setModel(applyPriceModel);
			applyPrice.addActionListener(event -> {
				if (applyPrice.isSelected() && priceChangeListener != null)
					priceChangeListener.run();
			});
			panel = buildPanel(applyPrice);
			return panel;
		}

		private JPanel buildPanel(JCheckBox applyPrice) {
			headingLabel.setFont(headingLabel.getFont().deriveFont(Font.BOLD, 12f));
			headingLabel.setForeground(RetailPOSColors.inkMuted());
			summaryCard = new JPanel(new java.awt.GridLayout(2, 1));
			summaryName.setPreferredSize(new Dimension(350, 24));
			summaryName.setFont(summaryName.getFont().deriveFont(Font.BOLD, 16f));
			summaryReference.setFont(summaryReference.getFont().deriveFont(13f));
			summaryReference.setForeground(RetailPOSColors.inkMuted());
			summaryCard.add(summaryName);
			summaryCard.add(summaryReference);
			editCard = new JPanel(new GridBagLayout());
			name.setPreferredSize(new Dimension(350, 48));
			reference.setPreferredSize(new Dimension(350, 48));
			ProductFormLayout.addFullRow(editCard, 0, new JLabel(AppLocal.getIntString("label.prodname")));
			ProductFormLayout.addFullRow(editCard, 1, name);
			ProductFormLayout.addFullRow(editCard, 2, new JLabel(AppLocal.getIntString("label.prodref")));
			ProductFormLayout.addFullRow(editCard, 3, reference);
			identity.add(summaryCard, BorderLayout.CENTER);
			editIdentity.setText("<html><u>" + AppLocal.getIntString("button.variants.editidentity") + "</u></html>");
			editIdentity.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
			editIdentity.setBorderPainted(false);
			editIdentity.setContentAreaFilled(false);
			editIdentity.setHorizontalAlignment(SwingConstants.LEFT);
			editIdentity.setPreferredSize(new Dimension(editIdentity.getPreferredSize().width, 48));
			editIdentity.setFont(editIdentity.getFont().deriveFont(Font.BOLD, 15f));
			editIdentity.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			JPanel header = new JPanel(new BorderLayout(0, 4));
			header.add(headingLabel, BorderLayout.NORTH);
			header.add(identity, BorderLayout.CENTER);
			header.add(editIdentity, BorderLayout.SOUTH);

			JPanel fields = new JPanel(new GridBagLayout());
			category.setPreferredSize(new Dimension(350, 48));
			prices.sellTax.setPreferredSize(new Dimension(150, 48));
			prices.sellTax.setFont(prices.sellTax.getFont().deriveFont(Font.BOLD, 26f));
			prices.sellTax.putClientProperty("JTextField.placeholderText", ProductPriceMath.formatCurrency(0.0));
			stock.setPreferredSize(new Dimension(72, 48));
			stock.setFont(stock.getFont().deriveFont(Font.BOLD, 26f));
			ProductFormLayout.addFullRow(fields, 0, new JLabel(AppLocal.getIntString("label.prodcategory")));
			ProductFormLayout.addFullRow(fields, 1, category);
			JPanel priceStock = new JPanel(new java.awt.GridLayout(1, 2, 12, 0));
			JPanel pricePanel = new JPanel(new BorderLayout(0, 4));
			JLabel priceLabel = new JLabel("<html><body style='width: 145px'>"
					+ AppLocal.getIntString("label.prodpriceselltax") + "</body></html>");
			pricePanel.add(priceLabel, BorderLayout.NORTH);
			pricePanel.add(prices.sellTax, BorderLayout.CENTER);
			pricePanel.add(priceHint, BorderLayout.SOUTH);
			JPanel stockPanel = new JPanel(new BorderLayout(0, 4));
			JLabel stockLabel = new JLabel(AppLocal.getIntString("label.variants.stock"));
			stockLabel.setPreferredSize(
					new Dimension(stockLabel.getPreferredSize().width, priceLabel.getPreferredSize().height));
			stockPanel.add(stockLabel, BorderLayout.NORTH);
			JPanel stepper = new JPanel(new BorderLayout());
			JButton minus = touchButton("−");
			JButton plus = touchButton("+");
			minus.setBackground(RetailPOSColors.surface200());
			plus.setBackground(RetailPOSColors.surface200());
			minus.addActionListener(event -> stepStock(-1));
			plus.addActionListener(event -> stepStock(1));
			stock.setHorizontalAlignment(SwingConstants.CENTER);
			stepper.add(minus, BorderLayout.WEST);
			stepper.add(stock, BorderLayout.CENTER);
			stepper.add(plus, BorderLayout.EAST);
			JPanel stockControl = new JPanel(new BorderLayout());
			stockControl.add(stepper, BorderLayout.NORTH);
			stockPanel.add(stockControl, BorderLayout.CENTER);
			priceStock.add(pricePanel);
			priceStock.add(stockPanel);
			ProductFormLayout.addFullRow(fields, 2, priceStock);
			applyPrice.setPreferredSize(new Dimension(applyPrice.getPreferredSize().width, 48));
			ProductFormLayout.addFullRow(fields, 3, applyPrice);
			JPanel result = new JPanel(new BorderLayout(0, 16));
			result.add(header, BorderLayout.NORTH);
			result.add(ProductFormLayout.topAligned(fields), BorderLayout.CENTER);
			return result;
		}

		private void refreshPriceHint() {
			boolean missing = ProductPriceMath.parsePositiveCurrency(prices.sellTax.getText(), false) == null;
			priceHint.setText(missing ? AppLocal.getIntString("label.variants.pricehint") : " ");
			priceHint.setForeground(RetailPOSColors.dangerText());
		}

		private void stepStock(int delta) {
			try {
				Double value = (Double) Formats.DOUBLE.parseValue(stock.getText());
				stock.setText(
						Formats.DOUBLE.formatValue(Double.valueOf(Math.max(0, (value == null ? 0 : value) + delta))));
			} catch (BasicException ignored) {
				stock.setText("0");
			}
		}

		private void changed() {
			if (!loading && current != null) {
				save(current);
				if (onChange != null)
					onChange.run();
			}
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
		private Runnable onChange;
		private java.util.function.BiConsumer<Integer, String> stockChanged;
		private final String[] columns = {"", AppLocal.getIntString("label.variant"),
				AppLocal.getIntString("label.variants.price"), AppLocal.getIntString("label.variants.stock")};

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
			return (column == 0 && !variants.get(row).scanned) || column == 3;
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
					return variant.grossPrice == null || variant.grossPrice.trim().isEmpty()
							? variant.scanned ? AppLocal.getIntString("label.variants.missing") : "–"
							: variant.grossPrice;
				case 3 :
					return variant.stock;
				default :
					return "";
			}
		}

		@Override
		public void setValueAt(Object value, int row, int column) {
			if (column == 0 && !variants.get(row).scanned) {
				variants.get(row).selected = Boolean.TRUE.equals(value);
				fireTableRowsUpdated(row, row);
			} else if (column == 3) {
				variants.get(row).stock = value.toString();
				stockChanged.accept(Integer.valueOf(row), value.toString());
				fireTableRowsUpdated(row, row);
			}
			if (onChange != null)
				onChange.run();
		}

		private void selectAll() {
			for (VariantImportState variant : variants) {
				variant.selected = true;
			}
			fireTableRowsUpdated(0, variants.size() - 1);
			if (onChange != null)
				onChange.run();
		}

		private void selectScannedOnly() {
			for (VariantImportState variant : variants) {
				variant.selected = variant.scanned;
			}
			fireTableRowsUpdated(0, variants.size() - 1);
			if (onChange != null)
				onChange.run();
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
