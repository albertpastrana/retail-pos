package com.openbravo.pos.reports;

/** One customer's debt position. */
public final class CustomerDebtRow {

	private final String searchKey;
	private final String customerName;
	private final double currentDebt;
	private final double maximumDebt;
	private final double remainingCredit;

	public CustomerDebtRow(String searchKey, String customerName, double currentDebt, double maximumDebt,
			double remainingCredit) {
		this.searchKey = searchKey;
		this.customerName = customerName;
		this.currentDebt = currentDebt;
		this.maximumDebt = maximumDebt;
		this.remainingCredit = remainingCredit;
	}

	public String getSearchKey() {
		return searchKey;
	}

	public String getCustomerName() {
		return customerName;
	}

	public double getCurrentDebt() {
		return currentDebt;
	}

	public double getMaximumDebt() {
		return maximumDebt;
	}

	public double getRemainingCredit() {
		return remainingCredit;
	}
}
