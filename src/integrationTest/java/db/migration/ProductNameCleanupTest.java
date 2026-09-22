package db.migration;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;

import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;

public class ProductNameCleanupTest {

	@Test
	public void removesOuterQuotesAndUnescapesInnerQuotes() {
		assertEquals("SLIP \"FREE MAN\"", V38__clean_quoted_product_names.normalize("\"SLIP \"\"FREE MAN\"\"\""));
	}

	@Test
	public void leavesNormalNamesUnchanged() {
		assertEquals("SLIP \"FREE MAN\"", V38__clean_quoted_product_names.normalize("SLIP \"FREE MAN\""));
	}

	@Test
	public void migratesExistingNamesAndIsIdempotent() throws Exception {
		try (Connection connection = DriverManager.getConnection("jdbc:derby:memory:quotedProductNames;create=true")) {
			connection.createStatement().execute("CREATE TABLE PRODUCTS (ID VARCHAR(255), NAME VARCHAR(255))");
			connection.createStatement()
					.executeUpdate("INSERT INTO PRODUCTS VALUES ('product-1', '\"SLIP \"\"FREE MAN\"\"\"')");
			Context context = new Context() {
				@Override
				public org.flywaydb.core.api.configuration.Configuration getConfiguration() {
					return null;
				}

				@Override
				public Connection getConnection() {
					return connection;
				}
			};
			V38__clean_quoted_product_names migration = new V38__clean_quoted_product_names();
			migration.migrate(context);
			migration.migrate(context);
			assertEquals("SLIP \"FREE MAN\"", queryName(connection));
		}
	}

	private String queryName(Connection connection) throws Exception {
		try (java.sql.ResultSet result = connection.createStatement().executeQuery("SELECT NAME FROM PRODUCTS")) {
			result.next();
			return result.getString(1);
		}
	}
}
