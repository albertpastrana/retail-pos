package com.openbravo.pos.ticket;

/**
 * Paper loyalty stamps. The till only counts how many to stamp; the card stays
 * on paper. One stamp per 10 € of eligible spend. A total discount on the
 * receipt makes the whole receipt ineligible. Sale or line discounted lines and
 * gift vouchers do not count; redemptions and any other negative line come off
 * the eligible spend. A receipt can carry several redemptions, one per full
 * card.
 */
public final class LoyaltyStamps {

	public static final double EUROS_PER_STAMP = 10.0;
	public static final double REDEMPTION_EUROS = 5.0;
	public static final String ENABLED_KEY = "loyalty.enabled";
	public static final String NAME_KEY = "loyalty.name";
	public static final String REDEMPTION_PROPERTY = "loyalty.redemption";
	public static final String DEFAULT_NAME = "victorines";

	private LoyaltyStamps() {
	}

	public static boolean isEnabled(String configured) {
		return configured == null || configured.isEmpty() || Boolean.parseBoolean(configured);
	}

	public static String name(String configured) {
		if (configured == null) {
			return DEFAULT_NAME;
		}
		String trimmed = configured.trim();
		return trimmed.isEmpty() ? DEFAULT_NAME : trimmed;
	}

	public static void applyToTicket(TicketInfo ticket, String enabledValue, String nameValue) {
		if (ticket.getProperty(ENABLED_KEY) == null) {
			ticket.setProperty(ENABLED_KEY, Boolean.toString(isEnabled(enabledValue)));
		}
		if (ticket.getProperty(NAME_KEY) == null) {
			ticket.setProperty(NAME_KEY, name(nameValue));
		}
	}

	public static int stampsEarned(TicketInfo ticket) {
		return (int) Math.floor(eligibleEuros(ticket) / EUROS_PER_STAMP);
	}

	public static double eligibleEuros(TicketInfo ticket) {
		if (hasTotalDiscount(ticket)) {
			return 0.0;
		}
		double sum = 0.0;
		for (TicketLineInfo line : ticket.getLines()) {
			if (countsForStamp(line) || reducesStampSpend(line)) {
				sum += line.getValue();
			}
		}
		return Math.max(0.0, roundCents(sum));
	}

	public static boolean hasTotalDiscount(TicketInfo ticket) {
		for (TicketLineInfo line : ticket.getLines()) {
			if ("total".equals(line.getProperty("discount.scope"))) {
				return true;
			}
		}
		return false;
	}

	public static double savingsEuros(TicketInfo ticket) {
		double savings = 0.0;
		for (TicketLineInfo line : ticket.getLines()) {
			if (isRedemption(line) && line.getValue() < 0.0) {
				savings += -line.getValue();
			}
		}
		return roundCents(savings);
	}

	public static void markRedemption(TicketLineInfo line) {
		line.setProperty(REDEMPTION_PROPERTY, "true");
	}

	public static boolean isRedemption(TicketLineInfo line) {
		return "true".equals(line.getProperty(REDEMPTION_PROPERTY));
	}

	static boolean countsForStamp(TicketLineInfo line) {
		if (line.isProductCom() || line.isGiftVoucher()) {
			return false;
		}
		if (line.getProperty("discount.line.percent") != null) {
			return false;
		}
		return line.getValue() > 0.0;
	}

	/**
	 * What the customer no longer pays: redemptions and any other negative line
	 * typed at the till. A gift voucher handed over is a payment, not a discount.
	 */
	static boolean reducesStampSpend(TicketLineInfo line) {
		if (line.isProductCom() || line.isGiftVoucher()) {
			return false;
		}
		return line.getValue() < 0.0;
	}

	private static double roundCents(double euros) {
		return Math.round(euros * 100.0) / 100.0;
	}
}
