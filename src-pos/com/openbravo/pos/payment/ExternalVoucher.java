package com.openbravo.pos.payment;

import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.util.RoundUtils;

/** Shared name and validation for an externally redeemed, non-cash tender. */
public final class ExternalVoucher {
	public static final String PAYMENT = "externalvoucher";
	public static final String NAME_KEY = "payment.externalvoucher.name";

	private ExternalVoucher() {
	}

	public static String name(String configured) {
		return configured == null || configured.trim().isEmpty()
				? AppLocal.getIntString("transpayment.externalvoucher")
				: configured.trim();
	}

	public static boolean valid(double amount, double remaining) {
		return Double.isFinite(amount) && RoundUtils.compare(amount, 0.0) > 0 && Double.isFinite(remaining)
				&& RoundUtils.compare(amount, remaining) <= 0;
	}
}
