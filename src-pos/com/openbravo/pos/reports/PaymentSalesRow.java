package com.openbravo.pos.reports;

/** One payment method's sales for a selected period. */
public final class PaymentSalesRow {

	private final String paymentType;
	private final double sales;
	private final double refunds;

	public PaymentSalesRow(String paymentType, double sales, double refunds) {
		this.paymentType = paymentType;
		this.sales = sales;
		this.refunds = refunds;
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
