package db.migration;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;

/**
 * Metadata lookups restricted to the schema being migrated. Asking the driver
 * for every schema finds leftovers in backup schemas and skips work the live
 * schema needs.
 */
final class SchemaObjects {

	private SchemaObjects() {
	}

	static boolean tableExists(Connection connection, String table) throws Exception {
		return actualTableName(connection, table) != null;
	}

	static boolean indexExists(Connection connection, String table, String index) throws Exception {
		String actualTable = actualTableName(connection, table);
		if (actualTable == null) {
			return false;
		}
		DatabaseMetaData metadata = connection.getMetaData();
		try (ResultSet indexes = metadata.getIndexInfo(connection.getCatalog(), connection.getSchema(), actualTable,
				false, true)) {
			while (indexes.next()) {
				if (index.equalsIgnoreCase(indexes.getString("INDEX_NAME"))) {
					return true;
				}
			}
			return false;
		}
	}

	private static String actualTableName(Connection connection, String table) throws Exception {
		DatabaseMetaData metadata = connection.getMetaData();
		try (ResultSet tables = metadata.getTables(connection.getCatalog(), connection.getSchema(), null,
				new String[]{"TABLE"})) {
			while (tables.next()) {
				String candidate = tables.getString("TABLE_NAME");
				if (table.equalsIgnoreCase(candidate)) {
					return candidate;
				}
			}
			return null;
		}
	}

	static boolean columnExists(Connection connection, String table, String column) throws Exception {
		DatabaseMetaData metadata = connection.getMetaData();
		try (ResultSet columns = metadata.getColumns(connection.getCatalog(), connection.getSchema(), null, null)) {
			while (columns.next()) {
				if (table.equalsIgnoreCase(columns.getString("TABLE_NAME"))
						&& column.equalsIgnoreCase(columns.getString("COLUMN_NAME"))) {
					return true;
				}
			}
			return false;
		}
	}
}
