package com.openbravo.pos.reports;

/** One payment method's sales for a selected period. */
public final class PaymentSalesRow {

	private final String paymentType;
	private final double sales;
	private final double refunds;
	private final String label;

	public PaymentSalesRow(String paymentType, double sales, double refunds) {
		this(paymentType, sales, refunds, null);
	}

	public PaymentSalesRow(String paymentType, double sales, double refunds, String label) {
		this.paymentType = paymentType;
		this.sales = sales;
		this.refunds = refunds;
		this.label = label;
	}

	public String getLabel() {
		return label;
	}

	public String getPaymentType() {
		return paymentType;
	}

	public double getSales() {
		return sales;
	}

	public double getRefunds() {
		return refunds;
	}

	public double getNet() {
		return sales + refunds;
	}
}
