package com.openbravo.pos.reports;

/** One product's sales for a selected period. */
public final class ProductSalesRow {

	private final String reference;
	private final String productName;
	private final String categoryName;
	private final double units;
	private final double amount;

	public ProductSalesRow(String reference, String productName, String categoryName, double units, double amount) {
		this.reference = reference;
		this.productName = productName;
		this.categoryName = categoryName;
		this.units = units;
		this.amount = amount;
	}

	public String getReference() {
		return reference;
	}

	public String getProductName() {
		return productName;
	}

	public String getCategoryName() {
		return categoryName;
	}

	public double getUnits() {
		return units;
	}

	public double getAmount() {
		return amount;
	}
}
