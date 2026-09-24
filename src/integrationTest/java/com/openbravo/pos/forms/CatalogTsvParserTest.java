package com.openbravo.pos.forms;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class CatalogTsvParserTest {

	@Test
	public void removesCsvFieldQuotesAndUnescapesQuotes() {
		assertThat(CatalogTsvParser.parse("id\t\"Product \"\"name\"\"\"\tBrand")).containsExactly("id",
				"Product \"name\"", "Brand");
	}

	@Test
	public void preservesEmptyAndUnquotedFields() {
		assertThat(CatalogTsvParser.parse("id\t\tplain\t")).containsExactly("id", "", "plain", "");
	}
}
