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
		if ("Avetset".equals(brand) || "Massana".equals(brand) || "Petrus".equals(brand) || "Dusen".equals(brand)
				|| "Señoretta".equals(brand) || "Egatex".equals(brand) || "Soy".equals(brand) || "Muslher".equals(brand)
				|| "Abanderado".equals(brand) || "Playtex".equals(brand) || "Focenza".equals(brand)
				|| "Punto Blanco".equals(brand) || "Ruipérez".equals(brand) || "Intimalia".equals(brand)
				|| "Intimissimi".equals(brand) || "Naiara".equals(brand) || "Babidu".equals(brand)
				|| "Punt Nou".equals(brand) || "Rodfer".equals(brand) || "Pocholina".equals(brand)
				|| "Selmark".equals(brand) || "Omsa".equals(brand) || "Filodoro".equals(brand)
				|| "Cotonella".equals(brand) || "Dim".equals(brand) || "Mariola Playbra".equals(brand)
				|| "Ocean".equals(brand) || "Princesa".equals(brand) || "Berkshire".equals(brand)
				|| "Ejecutivo".equals(brand) || "Novedades Marcos".equals(brand)
				|| "Babysanex".equals(brand) || "Sisi".equals(brand) || "Set".equals(brand)
				|| "Selene".equals(brand)) {
			return before(value, '-');
		}
		if ("Gisela".equals(brand)) {
			return value.indexOf('-') >= 0 ? before(value, '-') : before(value, ' ');
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
