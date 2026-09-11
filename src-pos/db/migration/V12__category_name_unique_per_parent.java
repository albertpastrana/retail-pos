package db.migration;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;

/**
 * Category names only have to be unique among siblings, so a subcategory can be
 * called "Clàssics" instead of "Batins Dona / Clàssics" just to clear the old
 * global unique index.
 *
 * The old index goes first: the names collide with each other until it is gone.
 *
 * Derby compares NULLs as equal in a unique index, so it also rejects two root
 * categories sharing a name; MySQL and PostgreSQL let those through. The editor
 * checks siblings before saving so the rule reads the same on every engine.
 *
 * Numbered after V11 because that gift-voucher migration already shipped.
 */
public class V12__category_name_unique_per_parent extends BaseJavaMigration {

	private static final String OLD_INDEX = "CATEGORIES_NAME_INX";

	private static final String NEW_INDEX = "CATEGORIES_PARENT_NAME_INX";

	private static final String SEPARATOR = " / ";

	@Override
	public void migrate(Context context) throws Exception {
		Connection connection = context.getConnection();
		if (SchemaObjects.indexExists(connection, "CATEGORIES", OLD_INDEX)) {
			try (Statement statement = connection.createStatement()) {
				statement.execute(dropIndexStatement(connection));
			}
		}
		stripParentPrefixes(connection);
		if (!SchemaObjects.indexExists(connection, "CATEGORIES", NEW_INDEX)) {
			try (Statement statement = connection.createStatement()) {
				statement.execute("CREATE UNIQUE INDEX " + NEW_INDEX + " ON CATEGORIES(PARENTID, NAME)");
			}
		}
	}

	private String dropIndexStatement(Connection connection) throws Exception {
		String product = connection.getMetaData().getDatabaseProductName();
		if (product != null && product.toLowerCase(Locale.ROOT).contains("mysql")) {
			return "ALTER TABLE CATEGORIES DROP INDEX " + OLD_INDEX;
		}
		return "DROP INDEX " + OLD_INDEX;
	}

	private void stripParentPrefixes(Connection connection) throws Exception {
		Map<String, String> names = new LinkedHashMap<String, String>();
		Map<String, List<String>> children = new LinkedHashMap<String, List<String>>();
		List<String> roots = new ArrayList<String>();
		readTree(connection, names, children, roots);
		applyRenames(connection, shortenedNames(names, children, roots));
	}

	private void readTree(Connection connection, Map<String, String> names, Map<String, List<String>> children,
			List<String> roots) throws Exception {
		try (Statement statement = connection.createStatement();
				ResultSet rows = statement.executeQuery("SELECT ID, NAME, PARENTID FROM CATEGORIES")) {
			while (rows.next()) {
				String id = rows.getString("ID");
				names.put(id, rows.getString("NAME"));
				String parent = rows.getString("PARENTID");
				if (parent == null) {
					roots.add(id);
				} else {
					if (!children.containsKey(parent)) {
						children.put(parent, new ArrayList<String>());
					}
					children.get(parent).add(id);
				}
			}
		}
	}

	/**
	 * Walks the tree from the roots down so a child is shortened against the name
	 * its parent ends up with, not the one it started with.
	 */
	private Map<String, String> shortenedNames(Map<String, String> names, Map<String, List<String>> children,
			List<String> roots) {
		Map<String, String> renamed = new LinkedHashMap<String, String>();
		Set<String> visited = new HashSet<String>(roots);
		Deque<String> pending = new ArrayDeque<String>(roots);
		while (!pending.isEmpty()) {
			String parent = pending.removeFirst();
			String prefix = names.get(parent) + SEPARATOR;
			for (String child : childrenOf(children, parent)) {
				if (visited.add(child)) {
					String name = names.get(child);
					if (name.startsWith(prefix) && name.length() > prefix.length()) {
						String shortened = name.substring(prefix.length());
						names.put(child, shortened);
						renamed.put(child, shortened);
					}
					pending.addLast(child);
				}
			}
		}
		return renamed;
	}

	private List<String> childrenOf(Map<String, List<String>> children, String parent) {
		List<String> siblings = children.get(parent);
		return siblings == null ? Collections.<String>emptyList() : siblings;
	}

	private void applyRenames(Connection connection, Map<String, String> renamed) throws Exception {
		if (renamed.isEmpty()) {
			return;
		}
		try (PreparedStatement update = connection.prepareStatement("UPDATE CATEGORIES SET NAME = ? WHERE ID = ?")) {
			for (Map.Entry<String, String> entry : renamed.entrySet()) {
				update.setString(1, entry.getValue());
				update.setString(2, entry.getKey());
				update.addBatch();
			}
			update.executeBatch();
		}
	}
}
