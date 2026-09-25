package com.openbravo.pos.ticket;

import java.util.Properties;

/** Shared store-level loyalty settings. */
public final class LoyaltySettings {

	public static final String ENABLED_KEY = "loyalty.enabled";
	public static final String NAME_KEY = "loyalty.name";
	public static final String ELIGIBLE_SPEND_PER_STAMP_KEY = "loyalty.eligible_spend_per_stamp";
	public static final String REDEMPTION_VALUE_KEY = "loyalty.redemption_value";

	public static final String DEFAULT_NAME = "";
	public static final double DEFAULT_ELIGIBLE_SPEND_PER_STAMP = 10.0;
	public static final double DEFAULT_REDEMPTION_VALUE = 5.0;

	private final boolean enabled;
	private final String name;
	private final double eligibleSpendPerStamp;
	private final double redemptionValue;

	public LoyaltySettings(boolean enabled, String name, double eligibleSpendPerStamp, double redemptionValue) {
		this.enabled = enabled;
		this.name = name == null ? "" : name.trim();
		this.eligibleSpendPerStamp = positiveOrDefault(eligibleSpendPerStamp, DEFAULT_ELIGIBLE_SPEND_PER_STAMP);
		this.redemptionValue = positiveOrDefault(redemptionValue, DEFAULT_REDEMPTION_VALUE);
	}

	public static LoyaltySettings defaults() {
		return new LoyaltySettings(false, DEFAULT_NAME, DEFAULT_ELIGIBLE_SPEND_PER_STAMP, DEFAULT_REDEMPTION_VALUE);
	}

	public static LoyaltySettings fromProperties(Properties properties) {
		return new LoyaltySettings(Boolean.parseBoolean(properties.getProperty(ENABLED_KEY, "false")),
				properties.getProperty(NAME_KEY, DEFAULT_NAME),
				parse(properties.getProperty(ELIGIBLE_SPEND_PER_STAMP_KEY), DEFAULT_ELIGIBLE_SPEND_PER_STAMP),
				parse(properties.getProperty(REDEMPTION_VALUE_KEY), DEFAULT_REDEMPTION_VALUE));
	}

	private static double parse(String value, double defaultValue) {
		try {
			return positiveOrDefault(Double.parseDouble(value), defaultValue);
		} catch (RuntimeException e) {
			return defaultValue;
		}
	}

	private static double positiveOrDefault(double value, double defaultValue) {
		return Double.isFinite(value) && value > 0.0 ? value : defaultValue;
	}

	public boolean isEnabled() {
		return enabled;
	}

	public String getName() {
		return name;
	}

	public double getEligibleSpendPerStamp() {
		return eligibleSpendPerStamp;
	}

	public double getRedemptionValue() {
		return redemptionValue;
	}
}
