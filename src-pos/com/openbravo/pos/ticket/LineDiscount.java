package com.openbravo.pos.ticket;

import com.openbravo.format.Formats;

/**
 * Percentage off a ticket line, either typed at the till or taken from a
 * product marked for sale. The catalogue price stays the list price.
 */
public final class LineDiscount {

	private LineDiscount() {
	}

	public static boolean shouldApplyCatalogSale(double listPrice, double linePrice, double salePercent) {
		if (salePercent <= 0.0) {
			return false;
		}
		return Math.abs(Math.abs(linePrice) - Math.abs(listPrice)) < 0.000001;
	}

	public static void applyPercent(TicketLineInfo line, double percentage) {
		String basePriceValue = line.getProperty("discount.line.baseprice");
		String baseName = line.getProperty("discount.line.basename");
		double basePrice = basePriceValue == null ? line.getPrice() : Double.parseDouble(basePriceValue);

		if (baseName == null) {
			baseName = line.getProductName();
			line.setProperty("discount.line.basename", baseName);
			line.setProperty("discount.line.baseprice", Double.toString(basePrice));
		}

		line.setPrice(basePrice * (1.0 - percentage / 100.0));
		line.setProperty("discount.line.percent", Double.toString(percentage));
		line.setProperty("product.name", nameWithDiscount(baseName, formatPercentage(percentage)));
	}

	public static String formatPercentage(double percentage) {
		return Formats.PERCENT.formatValue(new Double(percentage / 100.0));
	}

	static String nameWithDiscount(String baseName, String percentLabel) {
		String suffix = " (-" + percentLabel + ")";
		int open = baseName.lastIndexOf(" [");
		if (open >= 0 && baseName.endsWith("]")) {
			return baseName.substring(0, open) + suffix + baseName.substring(open);
		}
		return baseName + suffix;
	}
}
