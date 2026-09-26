package com.openbravo.pos.panels;

import java.util.Collections;
import java.util.Date;

import org.junit.jupiter.api.Test;

import com.openbravo.pos.sales.TaxesLogic;
import com.openbravo.pos.ticket.ProductInfoExt;
import com.openbravo.pos.ticket.TaxInfo;

import static org.assertj.core.api.Assertions.assertThat;

public class JProductFinderPriceTest {

	@Test
	public void displaysSellPriceIncludingApplicableTax() {
		ProductInfoExt product = new ProductInfoExt();
		product.setPriceSell(10.0);
		product.setTaxCategoryID("standard");
		TaxInfo tax = new TaxInfo("tax", "Standard", "standard", new Date(0L), null, null, 0.21, false, null);
		TaxesLogic taxesLogic = new TaxesLogic(Collections.singletonList(tax));

		String displayedPrice = JProductFinder.formatPrice(product, taxesLogic, new Date(), null);

		assertThat(displayedPrice).isEqualTo(product.printPriceSellTax(tax));
	}

	@Test
	public void keepsNetSellPriceWhenTaxInclusiveDisplayIsNotRequested() {
		ProductInfoExt product = new ProductInfoExt();
		product.setPriceSell(10.0);

		assertThat(JProductFinder.formatPrice(product, null, new Date(), null)).isEqualTo(product.printPriceSell());
	}
}
