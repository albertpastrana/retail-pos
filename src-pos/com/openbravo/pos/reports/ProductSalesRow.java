package com.openbravo.pos.reports;

/** One product's sales for a selected period. */
public final class ProductSalesRow {

	private final String reference;
	private final String productName;
	private final String categoryName;
	private final String categoryId;
	private final String brand;
	private final boolean manual;
	private final long lineCount;
	private final double units;
	private final double amount;

	public ProductSalesRow(String reference, String productName, String categoryName, double units, double amount) {
		this(reference, productName, categoryName, null, "", false, units, amount);
	}

	public ProductSalesRow(String reference, String productName, String categoryName, String categoryId, String brand,
			boolean manual, double units, double amount) {
		this(reference, productName, categoryName, categoryId, brand, manual, units, amount, 1);
	}

	public ProductSalesRow(String reference, String productName, String categoryName, String categoryId, String brand,
			boolean manual, double units, double amount, long lineCount) {
		this.reference = reference;
		this.productName = productName;
		this.categoryName = categoryName;
		this.categoryId = categoryId;
		this.brand = brand;
		this.manual = manual;
		this.lineCount = lineCount;
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

	public String getCategoryId() {
		return categoryId;
	}

	public String getBrand() {
		return brand;
	}

	public boolean isManual() {
		return manual;
	}

	public long getLineCount() {
		return lineCount;
	}

	public double getUnits() {
		return units;
	}

	public double getAmount() {
		return amount;
	}
}
