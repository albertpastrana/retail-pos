package com.openbravo.pos.reports;

/** Paired sales totals in day or month buckets, for the selected period. */
final class SalesTrend {
	final String[] labels;
	final double[] current;
	final double[] previous;
	final double[] previousElapsed;
	final boolean[] reached;
	int partialMonthIndex = -1;

	SalesTrend(String[] labels) {
		this.labels = labels;
		this.current = new double[labels.length];
		this.previous = new double[labels.length];
		this.previousElapsed = new double[labels.length];
		this.reached = new boolean[labels.length];
	}
}
