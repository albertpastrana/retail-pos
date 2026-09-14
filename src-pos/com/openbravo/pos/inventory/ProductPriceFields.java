package com.openbravo.pos.inventory;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.SQLException;
import java.util.List;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import com.openbravo.basic.BasicException;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.ticket.ProductInfoExt;

/**
 * Factory cost, tax, economic cost, and the till price. The till dialog owns
 * this block; the product editor uses the same numbers through
 * {@link ProductPriceMath} and extra markup fields of its own.
 */
public final class ProductPriceFields {

	public interface TaxRateLookup {
		double rate(TaxCategoryInfo category);
	}

	public final JTextField buy = ProductFormLayout.numberField(true, 12);
	public final JTextField secondary = ProductFormLayout.numberField(false, 12);
	public final JTextField sellTax = ProductFormLayout.numberField(true, 12);
	public final JLabel margin = new JLabel();
	public final JComboBox tax = new JComboBox();
	public final JLabel offer = new JLabel();
	public boolean reportlock;
	public Double pricesell;

	private boolean sellOverridden;
	private final PriceRule priceRule;
	private final TaxRegime regime;
	private final TaxRateLookup taxRates;

	public ProductPriceFields(ProductInfoExt product, List taxCategories, String preferredTaxId,
			PriceRuleService priceRuleService, TaxRegime regime, TaxRateLookup taxRates) throws BasicException {
		this.regime = regime;
		this.taxRates = taxRates;
		margin.setHorizontalAlignment(SwingConstants.RIGHT);
		margin.setEnabled(false);
		String brand = product.getProperty("catalog.brand", "").trim();
		try {
			priceRule = priceRuleService.findForBrand(brand);
		} catch (SQLException e) {
			throw new BasicException(AppLocal.getIntString("message.pricerules.loaderror"), e);
		}

		TaxCategoryInfo selected = null;
		for (Object item : taxCategories) {
			TaxCategoryInfo available = (TaxCategoryInfo) item;
			tax.addItem(available);
			if (preferredTaxId != null && preferredTaxId.equals(available.getID())) {
				selected = available;
			}
		}
		tax.setSelectedItem(selected != null ? selected : (taxCategories.isEmpty() ? null : taxCategories.get(0)));

		boolean priceAvailable = Boolean.parseBoolean(product.getProperty("catalog.price.available", "false"));
		if (priceAvailable) {
			buy.setText(ProductPriceMath.formatCurrency(Double.valueOf(product.getPriceBuy())));
		}

		ProductFormLayout.onEdit(buy, new Runnable() {
			public void run() {
				onBuyOrTaxChanged();
			}
		});
		ProductFormLayout.onEdit(sellTax, new Runnable() {
			public void run() {
				onSellTaxEdited();
			}
		});
		tax.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				onBuyOrTaxChanged();
			}
		});

		offerFromCost();
		refreshGrossFromNet();
		refreshSecondary();
		refreshMargin();
	}

	public String secondaryLabel() {
		return ProductPriceMath.secondaryLabel(regime);
	}

	public JPanel priceBlock() {
		JPanel block = new JPanel(new BorderLayout());
		block.add(sellTax, BorderLayout.NORTH);
		block.add(margin, BorderLayout.CENTER);
		return block;
	}

	public Double readCommercialMargin() {
		return ProductPriceMath.commercialMargin(ProductPriceMath.parsePositiveCurrency(buy.getText(), false),
				ProductPriceMath.parsePositiveCurrency(sellTax.getText(), false), taxRate(), regime);
	}

	public void setCommercialMargin(double commercialMargin) {
		Double gross = ProductPriceMath.grossFromCommercialMargin(
				ProductPriceMath.parsePositiveCurrency(buy.getText(), false), commercialMargin, taxRate(), regime);
		if (gross != null) {
			sellTax.setText(ProductPriceMath.formatCurrency(gross));
		}
	}

	private void onBuyOrTaxChanged() {
		if (!sellOverridden) {
			offerFromCost();
		}
		refreshGrossFromNet();
		refreshSecondary();
		refreshMargin();
	}

	private void onSellTaxEdited() {
		if (!reportlock) {
			sellOverridden = true;
			reportlock = true;
			pricesell = ProductPriceMath.netFromGross(ProductPriceMath.parsePositiveCurrency(sellTax.getText(), false),
					taxRate());
			reportlock = false;
		}
		refreshSecondary();
		refreshMargin();
	}

	private void refreshGrossFromNet() {
		if (!reportlock) {
			reportlock = true;
			sellTax.setText(ProductPriceMath.formatCurrency(ProductPriceMath.grossFromNet(pricesell, taxRate())));
			reportlock = false;
		}
	}

	private void refreshMargin() {
		Double commercialMargin = readCommercialMargin();
		margin.setText(commercialMargin == null
				? ""
				: AppLocal.getIntString("message.import.margin", Formats.PERCENT.formatValue(commercialMargin)));
	}

	private void offerFromCost() {
		Double factory = ProductPriceMath.parsePositiveCurrency(buy.getText(), false);
		if (factory == null || tax.getSelectedItem() == null) {
			pricesell = null;
			offer.setText(AppLocal.getIntString("message.pricerule.entercost"));
			return;
		}
		pricesell = ProductPriceMath.offerNetFromFactory(factory, taxRate(), priceRule, regime);
		Double gross = ProductPriceMath.offerGrossFromFactory(factory, taxRate(), priceRule, regime);
		offer.setText(AppLocal.getIntString("message.pricerule.offer", ProductPriceMath.formatCurrency(gross),
				Formats.PERCENT.formatValue(Double.valueOf(priceRule.getMarkupPercent() / 100.0))));
	}

	private void refreshSecondary() {
		Double factory = ProductPriceMath.parsePositiveCurrency(buy.getText(), false);
		Double taxRate = tax.getSelectedItem() == null ? null : Double.valueOf(taxRate());
		secondary.setText(
				ProductPriceMath.formatCurrency(ProductPriceMath.secondary(regime, factory, pricesell, taxRate)));
	}

	private double taxRate() {
		return taxRates.rate((TaxCategoryInfo) tax.getSelectedItem());
	}
}
