package com.openbravo.pos.inventory;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CatalogVariantModel {

	private static final Pattern YSABEL_MODEL = Pattern.compile("^(\\d{5})");

	private CatalogVariantModel() {
	}

	public static String fromReference(String reference, String brand) {
		if (reference == null) {
			return "";
		}
		String value = reference.trim();
		if ("Avetset".equals(brand) || "Massana".equals(brand) || "Abanderado".equals(brand)
				|| "Playtex".equals(brand)) {
			return before(value, '-');
		}
		if ("Gisela".equals(brand)) {
			return before(value, ' ');
		}
		if ("Ysabel Mora".equals(brand)) {
			Matcher match = YSABEL_MODEL.matcher(value);
			return match.find() ? match.group(1) : value;
		}
		return value;
	}

	private static String before(String value, char separator) {
		int position = value.indexOf(separator);
		return position < 0 ? value : value.substring(0, position);
	}
}
