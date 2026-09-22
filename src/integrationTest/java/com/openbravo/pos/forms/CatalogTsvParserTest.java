package com.openbravo.pos.forms;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;

import org.junit.jupiter.api.Test;

public class CatalogTsvParserTest {

	@Test
	public void removesCsvFieldQuotesAndUnescapesQuotes() {
		assertArrayEquals(new String[]{"id", "Product \"name\"", "Brand"},
				CatalogTsvParser.parse("id\t\"Product \"\"name\"\"\"\tBrand"));
	}

	@Test
	public void preservesEmptyAndUnquotedFields() {
		assertArrayEquals(new String[]{"id", "", "plain", ""}, CatalogTsvParser.parse("id\t\tplain\t"));
	}
}
