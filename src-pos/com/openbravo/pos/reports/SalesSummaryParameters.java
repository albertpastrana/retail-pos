package com.openbravo.pos.reports;

import java.util.Date;

/** The half-open time range used by the sales summary query. */
public final class SalesSummaryParameters {

	private final Date startInclusive;
	private final Date endExclusive;

	public SalesSummaryParameters(Date startInclusive, Date endExclusive) {
		if (startInclusive == null || endExclusive == null || !startInclusive.before(endExclusive)) {
			throw new IllegalArgumentException("The report period must have a start before its end");
		}
		this.startInclusive = new Date(startInclusive.getTime());
		this.endExclusive = new Date(endExclusive.getTime());
	}

	public Date getStartInclusive() {
		return new Date(startInclusive.getTime());
	}

	public Date getEndExclusive() {
		return new Date(endExclusive.getTime());
	}
}
