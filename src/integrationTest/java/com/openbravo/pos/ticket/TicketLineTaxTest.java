package com.openbravo.pos.ticket;

import java.util.Date;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class TicketLineTaxTest {

	private static final TaxInfo STANDARD_TAX = new TaxInfo("001", "Tax Standard", "001", new Date(0L), null, null,
			0.21, false, null);

	@Test
	public void calculatesGrossValueFromNetPriceAndTax() {
		TicketLineInfo line = new TicketLineInfo("Product", "001", 2.0, 10.0, STANDARD_TAX);

		assertThat(line.getSubValue()).isEqualTo(20.0);
		assertThat(line.getTax()).isEqualTo(4.2);
		assertThat(line.getValue()).isEqualTo(24.2);
		assertThat(line.getPriceTax()).isEqualTo(12.1);
	}

	@Test
	public void setsNetPriceFromGrossPrice() {
		TicketLineInfo line = new TicketLineInfo("Product", "001", 1.0, 0.0, STANDARD_TAX);

		line.setPriceTax(12.1);

		assertThat(line.getPrice()).isEqualTo(10.0);
		assertThat(line.getValue()).isEqualTo(12.1);
	}

	@Test
	public void keepsRefundTaxAndValueNegative() {
		TicketLineInfo line = new TicketLineInfo("Product", "001", -1.0, 10.0, STANDARD_TAX);

		assertThat(line.getSubValue()).isEqualTo(-10.0);
		assertThat(line.getTax()).isEqualTo(-2.1);
		assertThat(line.getValue()).isEqualTo(-12.1);
	}
}
