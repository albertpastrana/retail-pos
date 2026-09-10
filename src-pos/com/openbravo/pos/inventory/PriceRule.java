package com.openbravo.pos.inventory;

public final class PriceRule {

	public static final String ROUND_CHARM = "CHARM";
	public static final String ROUND_NONE = "NONE";
	public static final String ROUND_95 = "ALWAYS_95";

	private final String id;
	private final String brand;
	private final double markupPercent;
	private final String rounding;

	public PriceRule(String id, String brand, double markupPercent, String rounding) {
		this.id = id;
		this.brand = brand;
		this.markupPercent = markupPercent;
		this.rounding = rounding;
	}

	public String getId() {
		return id;
	}

	public String getBrand() {
		return brand;
	}

	public double getMarkupPercent() {
		return markupPercent;
	}

	public String getRounding() {
		return rounding;
	}
}
