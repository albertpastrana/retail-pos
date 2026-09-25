package com.openbravo.pos.reports;

/** Redemption count and value for a dashboard period. */
public final class LoyaltyRedemptionSummary {

	private final int count;
	private final double value;

	public LoyaltyRedemptionSummary(int count, double value) {
		this.count = count;
		this.value = value;
	}

	public int getCount() {
		return count;
	}

	public double getValue() {
		return value;
	}
}
