package com.openbravo.pos.reports;

/** Named values shown by the period sales summary. */
public final class SalesSummary {

	private final int receiptCount;
	private final double grossSales;
	private final double refunds;
	private final double tax;
	private final int refundCount;

	public SalesSummary(int receiptCount, double grossSales, double refunds, double tax) {
		this(receiptCount, grossSales, refunds, tax, 0);
	}

	public SalesSummary(int receiptCount, double grossSales, double refunds, double tax, int refundCount) {
		this.receiptCount = receiptCount;
		this.grossSales = grossSales;
		this.refunds = refunds;
		this.tax = tax;
		this.refundCount = refundCount;
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

	public int getRefundCount() {
		return refundCount;
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
