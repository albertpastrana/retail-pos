package com.openbravo.pos.reports;

/** One product and location that is below its configured safety stock. */
public final class LowStockRow {

	private final String reference;
	private final String productName;
	private final String categoryName;
	private final String locationName;
	private final double currentUnits;
	private final double minimumUnits;
	private final double maximumUnits;

	public LowStockRow(String reference, String productName, String categoryName, String locationName,
			double currentUnits, double minimumUnits, double maximumUnits) {
		this.reference = reference;
		this.productName = productName;
		this.categoryName = categoryName;
		this.locationName = locationName;
		this.currentUnits = currentUnits;
		this.minimumUnits = minimumUnits;
		this.maximumUnits = maximumUnits;
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

	public String getLocationName() {
		return locationName;
	}

	public double getCurrentUnits() {
		return currentUnits;
	}

	public double getMinimumUnits() {
		return minimumUnits;
	}

	public double getMaximumUnits() {
		return maximumUnits;
	}

	public double getUnitsToOrder() {
		double target = maximumUnits > 0.0 ? maximumUnits : minimumUnits;
		return Math.max(0.0, target - currentUnits);
	}
}
