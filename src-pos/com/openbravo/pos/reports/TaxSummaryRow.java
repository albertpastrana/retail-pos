package com.openbravo.pos.reports;

/** One tax's taxable base, tax amount, and total for a selected period. */
public final class TaxSummaryRow {

	private final String taxName;
	private final double taxRate;
	private final double taxableBase;
	private final double taxAmount;

	public TaxSummaryRow(String taxName, double taxRate, double taxableBase, double taxAmount) {
		this.taxName = taxName;
		this.taxRate = taxRate;
		this.taxableBase = taxableBase;
		this.taxAmount = taxAmount;
	}

	public String getTaxName() {
		return taxName;
	}

	public double getTaxRate() {
		return taxRate;
	}

	public double getTaxableBase() {
		return taxableBase;
	}

	public double getTaxAmount() {
		return taxAmount;
	}

	public double getTotal() {
		return taxableBase + taxAmount;
	}
}
