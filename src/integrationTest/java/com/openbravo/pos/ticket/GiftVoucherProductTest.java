package com.openbravo.pos.ticket;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.Properties;

import org.junit.Test;

public class GiftVoucherProductTest {

	private static final TaxInfo TAX = new TaxInfo("001", "IVA", "001", null, null, null, 0.21, false, null);

	@Test
	public void voucherFlagSurvivesMovingProductToAnotherCategory() {
		ProductInfoExt product = product("another-category", true);

		TicketLineInfo line = new TicketLineInfo(product, 1.0, 10.0, TAX, new Properties());

		assertTrue(line.isGiftVoucher());
	}

	@Test
	public void explicitFalseDoesNotTurnEveryProductInLegacyCategoryIntoVoucher() {
		ProductInfoExt product = product("gift-vouchers", false);

		TicketLineInfo line = new TicketLineInfo(product, 1.0, 10.0, TAX, new Properties());

		assertFalse(line.isGiftVoucher());
	}

	@Test
	public void oldTicketLinesStillRecognizeVoucherCategory() {
		Properties attributes = new Properties();
		attributes.setProperty("product.categoryid", "gift-vouchers");

		assertTrue(new TicketLineInfo("old-product", 1.0, 10.0, TAX, attributes).isGiftVoucher());
	}

	private static ProductInfoExt product(String category, boolean voucher) {
		ProductInfoExt product = new ProductInfoExt();
		product.setID("product");
		product.setName("Val regal");
		product.setCategoryID(category);
		product.setTaxCategoryID("001");
		product.setVoucher(voucher);
		return product;
	}
}
