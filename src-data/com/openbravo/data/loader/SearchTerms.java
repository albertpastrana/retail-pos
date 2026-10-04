package com.openbravo.data.loader;

import java.util.Arrays;

/** Splits free-text searches into the terms that must all match. */
public final class SearchTerms {

	private SearchTerms() {
	}

	public static String[] split(String value) {
		if (value == null || value.trim().isEmpty())
			return new String[0];
		return Arrays.stream(value.trim().split("\\s+")).filter(term -> !term.isEmpty()).toArray(String[]::new);
	}
}
