package com.openbravo.pos.ticket;

import java.util.Properties;

/** Resolves the loyalty settings stored for a till. */
public final class LoyaltyConfiguration {

	private LoyaltyConfiguration() {
	}

	public static void apply(Properties runtimeConfig, LoyaltySettings settings) {
		runtimeConfig.setProperty(LoyaltySettings.ENABLED_KEY, Boolean.toString(settings.isEnabled()));
		runtimeConfig.setProperty(LoyaltySettings.NAME_KEY, settings.getName());
		runtimeConfig.setProperty(LoyaltySettings.ELIGIBLE_SPEND_PER_STAMP_KEY,
				Double.toString(settings.getEligibleSpendPerStamp()));
		runtimeConfig.setProperty(LoyaltySettings.REDEMPTION_VALUE_KEY, Double.toString(settings.getRedemptionValue()));
	}

	/** Applies legacy resource properties while databases are being upgraded. */
	public static void apply(Properties runtimeConfig, Properties databaseConfig) {
		apply(runtimeConfig, LoyaltySettings.fromProperties(databaseConfig));
	}
}
