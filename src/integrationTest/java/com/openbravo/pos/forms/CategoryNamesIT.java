package com.openbravo.pos.forms;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.flywaydb.core.api.configuration.Configuration;
import org.flywaydb.core.api.migration.Context;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import db.migration.V12__category_name_unique_per_parent;

public class CategoryNamesIT {

	@Test
	public void shortensChildNamesAgainstTheirParent() throws Exception {
		Connection connection = migrated("flywayCategoryRenameIT");
		try {
			insertCategory(connection, "roba", "Samarretes Dona", null);
			insertCategory(connection, "curta", "Samarretes Dona / Màniga curta", "roba");
			insertCategory(connection, "llarga", "Samarretes Dona / Màniga llarga", "roba");
			insertCategory(connection, "kept", "Tirants", "roba");

			rerunMigration(connection);

			assertEquals("Samarretes Dona", nameOf(connection, "roba"));
			assertEquals("Màniga curta", nameOf(connection, "curta"));
			assertEquals("Màniga llarga", nameOf(connection, "llarga"));
			assertEquals("Tirants", nameOf(connection, "kept"));
		} finally {
			connection.close();
		}
	}

	@Test
	public void shortensGrandchildrenAgainstTheShortenedParent() throws Exception {
		Connection connection = migrated("flywayCategoryDeepRenameIT");
		try {
			insertCategory(connection, "pijames", "Pijames", null);
			insertCategory(connection, "hivern", "Pijames / Hivern", "pijames");
			insertCategory(connection, "classics", "Hivern / Clàssics", "hivern");

			rerunMigration(connection);

			assertEquals("Hivern", nameOf(connection, "hivern"));
			assertEquals("Clàssics", nameOf(connection, "classics"));
		} finally {
			connection.close();
		}
	}

	@Test
	public void allowsTheSameNameUnderDifferentParents() throws Exception {
		Connection connection = migrated("flywayCategorySiblingsIT");
		try {
			insertCategory(connection, "dona", "Samarretes Dona", null);
			insertCategory(connection, "home", "Samarretes Home", null);
			insertCategory(connection, "dona-curta", "Màniga curta", "dona");
			insertCategory(connection, "home-curta", "Màniga curta", "home");
		} finally {
			connection.close();
		}
	}

	@Test
	public void rejectsTheSameNameTwiceUnderOneParent() throws Exception {
		Connection connection = migrated("flywayCategoryDuplicateIT");
		try {
			insertCategory(connection, "dona", "Samarretes Dona", null);
			insertCategory(connection, "curta", "Màniga curta", "dona");
			try {
				insertCategory(connection, "curta-again", "Màniga curta", "dona");
				fail("Expected the unique index to reject a repeated sibling name");
			} catch (SQLException expected) {
				assertEquals("23505", expected.getSQLState());
			}
		} finally {
			connection.close();
		}
	}

	/**
	 * The baseline seeds two root categories, which Derby only accepts because the
	 * unique index covers (PARENTID, NAME) and not PARENTID alone.
	 */
	@Test
	public void keepsBothSeededRootCategories() throws Exception {
		Connection connection = migrated("flywayCategorySeedIT");
		try {
			assertEquals("Category Standard", nameOf(connection, "000"));
			assertEquals("Vals regal", nameOf(connection, "gift-vouchers"));
		} finally {
			connection.close();
		}
	}

	private static Connection migrated(String database) throws SQLException {
		String url = "jdbc:derby:memory:" + database + ";create=true";
		DatabaseMigrator.migrate(url, null, null);
		return DriverManager.getConnection(url);
	}

	/**
	 * Flyway has already run the migration on this schema, so call it again
	 * directly to shorten names inserted afterwards. Doing so also proves the
	 * migration is safe to repeat.
	 */
	private static void rerunMigration(final Connection connection) throws Exception {
		new V12__category_name_unique_per_parent().migrate(new Context() {
			public Configuration getConfiguration() {
				return null;
			}

			public Connection getConnection() {
				return connection;
			}
		});
	}

	private static void insertCategory(Connection connection, String id, String name, String parent)
			throws SQLException {
		PreparedStatement statement = connection
				.prepareStatement("INSERT INTO CATEGORIES(ID, NAME, PARENTID) VALUES (?, ?, ?)");
		try {
			statement.setString(1, id);
			statement.setString(2, name);
			statement.setString(3, parent);
			statement.execute();
		} finally {
			statement.close();
		}
	}

	private static String nameOf(Connection connection, String id) throws SQLException {
		PreparedStatement statement = connection.prepareStatement("SELECT NAME FROM CATEGORIES WHERE ID = ?");
		try {
			statement.setString(1, id);
			ResultSet rows = statement.executeQuery();
			try {
				rows.next();
				return rows.getString(1);
			} finally {
				rows.close();
			}
		} finally {
			statement.close();
		}
	}
}
