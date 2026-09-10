package com.openbravo.pos.inventory;

public enum TaxRegime {
	EQUIVALENCE_SURCHARGE,
	NORMAL;

	public static TaxRegime fromDatabase(String value) {
		if (NORMAL.name().equals(value)) {
			return NORMAL;
		}
		return EQUIVALENCE_SURCHARGE;
	}
}
