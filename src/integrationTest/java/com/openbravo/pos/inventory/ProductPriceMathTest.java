package com.openbravo.pos.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Locale;

import javax.swing.JTextField;

import org.junit.jupiter.api.Test;

import com.openbravo.format.Formats;

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

	@Test
	public void parsesKeyboardDecimalPointWithCommaLocale() throws Exception {
		Locale originalLocale = Locale.getDefault();
		try {
			Locale.setDefault(Locale.GERMANY);
			Formats.setCurrencyPattern(null);
			Formats.setDoublePattern(null);
			assertEquals(12.62, ProductPriceMath.parseCurrency("12.62").doubleValue(), 0.0001);
		} finally {
			Locale.setDefault(originalLocale);
			Formats.setCurrencyPattern(null);
			Formats.setDoublePattern(null);
		}
	}

	@Test
	public void numberFieldConvertsKeyboardDecimalPoint() {
		Locale originalLocale = Locale.getDefault();
		try {
			Locale.setDefault(Locale.GERMANY);
			JTextField field = ProductFormLayout.numberField(true);
			field.setText("12.62");
			assertEquals("12,62", field.getText());
		} finally {
			Locale.setDefault(originalLocale);
		}
	}
}
