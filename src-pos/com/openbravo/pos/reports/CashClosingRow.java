package com.openbravo.pos.reports;

import java.util.Date;

/** One cash/session period and its signed payment totals. */
public final class CashClosingRow {

	private final String money;
	private final String host;
	private final int sequence;
	private final Date dateStart;
	private final Date dateEnd;
	private final int receiptCount;
	private final double grossPayments;
	private final double refunds;

	public CashClosingRow(String money, String host, int sequence, Date dateStart, Date dateEnd, int receiptCount,
			double grossPayments, double refunds) {
		this.money = money;
		this.host = host;
		this.sequence = sequence;
		this.dateStart = copy(dateStart);
		this.dateEnd = copy(dateEnd);
		this.receiptCount = receiptCount;
		this.grossPayments = grossPayments;
		this.refunds = refunds;
	}

	public String getMoney() {
		return money;
	}

	public String getHost() {
		return host;
	}

	public int getSequence() {
		return sequence;
	}

	public Date getDateStart() {
		return copy(dateStart);
	}

	public Date getDateEnd() {
		return copy(dateEnd);
	}

	public boolean isOpen() {
		return dateEnd == null;
	}

	public int getReceiptCount() {
		return receiptCount;
	}

	public double getGrossPayments() {
		return grossPayments;
	}

	public double getRefunds() {
		return refunds;
	}

	public double getNetPayments() {
		return grossPayments + refunds;
	}

	private static Date copy(Date value) {
		return value == null ? null : new Date(value.getTime());
	}
}
