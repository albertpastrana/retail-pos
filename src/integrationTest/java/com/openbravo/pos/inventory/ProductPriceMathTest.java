package com.openbravo.pos.inventory;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import org.junit.Test;

public class ProductPriceMathTest {

	@Test
	public void secondaryIsEconomicCostUnderEquivalence() {
		assertEquals(12.62, ProductPriceMath.secondary(TaxRegime.EQUIVALENCE_SURCHARGE, Double.valueOf(10.0),
				Double.valueOf(20.0), Double.valueOf(0.21)).doubleValue(), 0.0001);
	}

	@Test
	public void secondaryIsNetSellUnderNormalAccounting() {
		assertEquals(20.0,
				ProductPriceMath
						.secondary(TaxRegime.NORMAL, Double.valueOf(10.0), Double.valueOf(20.0), Double.valueOf(0.21))
						.doubleValue(),
				0.0001);
	}

	@Test
	public void convertsNetAndGrossThroughVat() {
		assertEquals(12.1, ProductPriceMath.grossFromNet(Double.valueOf(10.0), 0.21).doubleValue(), 0.0001);
		assertEquals(10.0, ProductPriceMath.netFromGross(Double.valueOf(12.1), 0.21).doubleValue(), 0.0001);
	}

	@Test
	public void markupIsSellOverCostMinusOne() {
		assertEquals(1.0, ProductPriceMath.markup(Double.valueOf(10.0), Double.valueOf(20.0)).doubleValue(), 0.0001);
		assertNull(ProductPriceMath.markup(Double.valueOf(0.0), Double.valueOf(20.0)));
	}

	@Test
	public void commercialMarginMatchesRetailFormula() {
		Double margin = ProductPriceMath.commercialMargin(Double.valueOf(10.0), Double.valueOf(19.95), 0.21,
				TaxRegime.EQUIVALENCE_SURCHARGE);
		assertEquals(36.74, margin.doubleValue() * 100.0, 0.02);
	}
}
