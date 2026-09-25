package com.openbravo.pos.reports;

/** Current and elapsed-matched previous values for one dashboard period. */
public final class SalesSummaryComparison {

	private final SalesSummary current;
	private final SalesSummary previous;
	private final LoyaltyRedemptionSummary loyaltyCurrent;
	private final LoyaltyRedemptionSummary loyaltyPrevious;

	public SalesSummaryComparison(SalesSummary current, SalesSummary previous) {
		this(current, previous, new LoyaltyRedemptionSummary(0, 0.0), new LoyaltyRedemptionSummary(0, 0.0));
	}

	public SalesSummaryComparison(SalesSummary current, SalesSummary previous, LoyaltyRedemptionSummary loyaltyCurrent,
			LoyaltyRedemptionSummary loyaltyPrevious) {
		this.current = current;
		this.previous = previous;
		this.loyaltyCurrent = loyaltyCurrent;
		this.loyaltyPrevious = loyaltyPrevious;
	}

	public SalesSummary getCurrent() {
		return current;
	}

	public SalesSummary getPrevious() {
		return previous;
	}

	public LoyaltyRedemptionSummary getLoyaltyCurrent() {
		return loyaltyCurrent;
	}

	public LoyaltyRedemptionSummary getLoyaltyPrevious() {
		return loyaltyPrevious;
	}

	public boolean hasPreviousSales() {
		return previous.getGrossSales() != 0.0 || previous.getRefunds() != 0.0;
	}

	public double netDeltaPercent() {
		if (!hasPreviousSales()) {
			return 0.0;
		}
		return (current.getNetSales() - previous.getNetSales()) / Math.abs(previous.getNetSales()) * 100.0;
	}
}
