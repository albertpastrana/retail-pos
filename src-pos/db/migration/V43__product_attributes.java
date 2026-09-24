package db.migration;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/** Stores optional case-insensitive key/value characteristics per product. */
public class V43__product_attributes extends BaseJavaMigration {
	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		try (Statement statement = connection.createStatement()) {
			statement.execute("CREATE TABLE PRODUCT_ATTRIBUTES (" + "ID VARCHAR(255) NOT NULL, "
					+ "PRODUCT_ID VARCHAR(255) NOT NULL, " + "ATTRIBUTE_KEY VARCHAR(255) NOT NULL, "
					+ "ATTRIBUTE_KEY_NORMALIZED VARCHAR(255) NOT NULL, " + "ATTRIBUTE_VALUE VARCHAR(255), "
					+ "ATTRIBUTE_VALUE_NORMALIZED VARCHAR(255), " + "PRIMARY KEY (ID), "
					+ "CONSTRAINT PRODUCT_ATTRIBUTES_PRODUCT FOREIGN KEY (PRODUCT_ID) REFERENCES PRODUCTS(ID) ON DELETE CASCADE"
					+ ")");
			statement.execute("CREATE UNIQUE INDEX PRODUCT_ATTRIBUTES_KEY_INX "
					+ "ON PRODUCT_ATTRIBUTES(PRODUCT_ID, ATTRIBUTE_KEY_NORMALIZED)");
			statement.execute("CREATE INDEX PRODUCT_ATTRIBUTES_SUGGESTION_INX "
					+ "ON PRODUCT_ATTRIBUTES(ATTRIBUTE_KEY_NORMALIZED, ATTRIBUTE_VALUE_NORMALIZED)");

			String database = connection.getMetaData().getDatabaseProductName();
			if (database.toLowerCase().contains("mysql")) {
				statement.execute("ALTER TABLE PRODUCTS DROP FOREIGN KEY PRODUCTS_ATTRSET_FK");
				statement.execute("ALTER TABLE STOCKDIARY DROP FOREIGN KEY STOCKDIARY_ATTSETINST");
				statement.execute("ALTER TABLE STOCKCURRENT DROP FOREIGN KEY STOCKCURRENT_ATTSETINST");
				statement.execute("ALTER TABLE TICKETLINES DROP FOREIGN KEY TICKETLINES_ATTSETINST");
			} else {
				statement.execute("ALTER TABLE PRODUCTS DROP CONSTRAINT PRODUCTS_ATTRSET_FK");
				statement.execute("ALTER TABLE STOCKDIARY DROP CONSTRAINT STOCKDIARY_ATTSETINST");
				statement.execute("ALTER TABLE STOCKCURRENT DROP CONSTRAINT STOCKCURRENT_ATTSETINST");
				statement.execute("ALTER TABLE TICKETLINES DROP CONSTRAINT TICKETLINES_ATTSETINST");
				dropStockCurrentIndex(connection, statement);
			}
			statement.execute("ALTER TABLE PRODUCTS DROP COLUMN ATTRIBUTESET_ID");
			statement.execute("ALTER TABLE STOCKDIARY DROP COLUMN ATTRIBUTESETINSTANCE_ID");
			statement.execute("ALTER TABLE STOCKCURRENT DROP COLUMN ATTRIBUTESETINSTANCE_ID");
			statement.execute("ALTER TABLE TICKETLINES DROP COLUMN ATTRIBUTESETINSTANCE_ID");
			statement.execute("DROP TABLE ATTRIBUTEINSTANCE");
			statement.execute("DROP TABLE ATTRIBUTESETINSTANCE");
			statement.execute("DROP TABLE ATTRIBUTEUSE");
			statement.execute("DROP TABLE ATTRIBUTEVALUE");
			statement.execute("DROP TABLE ATTRIBUTESET");
			statement.execute("DROP TABLE ATTRIBUTE");
		}
	}

	private static void dropStockCurrentIndex(Connection connection, Statement statement) throws Exception {
		if (indexExists(connection, "STOCKCURRENT_INX")) {
			statement.execute("DROP INDEX STOCKCURRENT_INX");
		}
	}

	private static boolean indexExists(Connection connection, String indexName) throws Exception {
		try (ResultSet indexes = connection.getMetaData().getIndexInfo(null, null, "STOCKCURRENT", false, false)) {
			while (indexes.next()) {
				String name = indexes.getString("INDEX_NAME");
				if (indexName.equalsIgnoreCase(name)) {
					return true;
				}
			}
		}
		return false;
	}

}
