package com.openbravo.pos.ticket;

import java.util.Properties;

/** Resolves the loyalty settings stored for a till. */
public final class LoyaltyConfiguration {

	private LoyaltyConfiguration() {
	}

	/** Applies only database values; missing values disable loyalty. */
	public static void apply(Properties runtimeConfig, Properties databaseConfig) {
		runtimeConfig.setProperty(LoyaltyStamps.ENABLED_KEY,
				databaseConfig.getProperty(LoyaltyStamps.ENABLED_KEY, "false"));
		runtimeConfig.setProperty(LoyaltyStamps.NAME_KEY, databaseConfig.getProperty(LoyaltyStamps.NAME_KEY, ""));
	}
}
