package com.openbravo.pos.ticket;

/**
 * Paper loyalty stamps. The till only counts how many to stamp; the card stays
 * on paper. One stamp per 10 currency units of eligible spend. A total discount
 * on the receipt makes the whole receipt ineligible. Sale or line discounted
 * lines and gift vouchers do not count; redemptions and any other negative line
 * come off the eligible spend. A receipt can carry several redemptions, one per
 * full card.
 */
public final class LoyaltyStamps {

	public static final String ENABLED_KEY = LoyaltySettings.ENABLED_KEY;
	public static final String NAME_KEY = LoyaltySettings.NAME_KEY;
	public static final String ELIGIBLE_SPEND_PER_STAMP_KEY = LoyaltySettings.ELIGIBLE_SPEND_PER_STAMP_KEY;
	public static final String REDEMPTION_VALUE_KEY = LoyaltySettings.REDEMPTION_VALUE_KEY;
	public static final String REDEMPTION_PROPERTY = "loyalty.redemption";
	public static final String DEFAULT_NAME = "victorines";

	private LoyaltyStamps() {
	}

	public static boolean isEnabled(String configured) {
		return Boolean.parseBoolean(configured);
	}

	public static String name(String configured) {
		return configured == null ? "" : configured.trim();
	}

	public static void applyToTicket(TicketInfo ticket, String enabledValue, String nameValue, String spendPerStamp,
			String redemptionValue) {
		if (ticket.getProperty(ENABLED_KEY) == null) {
			ticket.setProperty(ENABLED_KEY, Boolean.toString(isEnabled(enabledValue)));
		}
		if (ticket.getProperty(NAME_KEY) == null) {
			ticket.setProperty(NAME_KEY, name(nameValue));
		}
		if (ticket.getProperty(ELIGIBLE_SPEND_PER_STAMP_KEY) == null) {
			ticket.setProperty(ELIGIBLE_SPEND_PER_STAMP_KEY,
					Double.toString(positiveValue(spendPerStamp, LoyaltySettings.DEFAULT_ELIGIBLE_SPEND_PER_STAMP)));
		}
		if (ticket.getProperty(REDEMPTION_VALUE_KEY) == null) {
			ticket.setProperty(REDEMPTION_VALUE_KEY,
					Double.toString(positiveValue(redemptionValue, LoyaltySettings.DEFAULT_REDEMPTION_VALUE)));
		}
	}

	public static void applyToTicket(TicketInfo ticket, String enabledValue, String nameValue) {
		applyToTicket(ticket, enabledValue, nameValue, null, null);
	}

	public static int stampsEarned(TicketInfo ticket) {
		return (int) Math.floor(eligibleEuros(ticket) / positiveValue(ticket.getProperty(ELIGIBLE_SPEND_PER_STAMP_KEY),
				LoyaltySettings.DEFAULT_ELIGIBLE_SPEND_PER_STAMP));
	}

	public static double redemptionValue(TicketInfo ticket) {
		return positiveValue(ticket.getProperty(REDEMPTION_VALUE_KEY), LoyaltySettings.DEFAULT_REDEMPTION_VALUE);
	}

	public static double redemptionValue(String value) {
		return positiveValue(value, LoyaltySettings.DEFAULT_REDEMPTION_VALUE);
	}

	private static double positiveValue(String value, double defaultValue) {
		try {
			double parsed = Double.parseDouble(value);
			return Double.isFinite(parsed) && parsed > 0.0 ? parsed : defaultValue;
		} catch (RuntimeException e) {
			return defaultValue;
		}
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
