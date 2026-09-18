package com.openbravo.pos.reports;

/** Named values shown by the period sales summary. */
public final class SalesSummary {

	private final int receiptCount;
	private final double grossSales;
	private final double refunds;
	private final double tax;

	public SalesSummary(int receiptCount, double grossSales, double refunds, double tax) {
		this.receiptCount = receiptCount;
		this.grossSales = grossSales;
		this.refunds = refunds;
		this.tax = tax;
	}

	public int getReceiptCount() {
		return receiptCount;
	}

	public double getGrossSales() {
		return grossSales;
	}

	public double getRefunds() {
		return refunds;
	}

	public double getNetSales() {
		return grossSales + refunds;
	}

	public double getTax() {
		return tax;
	}

	public double getAverageReceipt() {
		return receiptCount == 0 ? 0.0 : getNetSales() / receiptCount;
	}
}
