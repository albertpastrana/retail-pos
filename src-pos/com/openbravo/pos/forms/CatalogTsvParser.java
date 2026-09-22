package com.openbravo.pos.forms;

import java.util.ArrayList;
import java.util.List;

/**
 * Parses the tab-separated catalog files, including CSV-style quoted fields.
 */
public final class CatalogTsvParser {

	private CatalogTsvParser() {
	}

	public static String[] parse(String line) {
		List<String> fields = new ArrayList<String>();
		StringBuilder field = new StringBuilder();
		boolean quoted = false;
		boolean fieldStart = true;

		for (int i = 0; i < line.length(); i++) {
			char character = line.charAt(i);
			if (quoted) {
				if (character == '"') {
					if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
						field.append('"');
						i++;
					} else {
						quoted = false;
					}
				} else {
					field.append(character);
				}
			} else if (fieldStart && character == '"') {
				quoted = true;
				fieldStart = false;
			} else if (character == '\t') {
				fields.add(field.toString());
				field.setLength(0);
				fieldStart = true;
			} else {
				field.append(character);
				fieldStart = false;
			}
		}
		fields.add(field.toString());
		return fields.toArray(new String[fields.size()]);
	}
}
