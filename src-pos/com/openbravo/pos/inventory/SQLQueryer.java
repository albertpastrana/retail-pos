package com.openbravo.pos.inventory;

import java.sql.*;
import javax.swing.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.openbravo.data.loader.Session;

public class SQLQueryer {
	private static final Logger LOGGER = Logger.getLogger(SQLQueryer.class.getName());
	Connection conn = null;
	Statement stmt = null;
	ResultSet rs = null;

	public SQLQueryer(Session s, String query) {
		conn = null;
		stmt = null;
		rs = null;

		try {
			if (s == null) {
				throw new SQLException("A database session is required");
			}

			stmt = s.getConnection().createStatement();

			rs = stmt.executeQuery(query);

			ResultSetMetaData rsmd = rs.getMetaData();

			rs.last();
			int length = rs.getRow();
			rs.beforeFirst();

			if (length > 1) {
				JOptionPane.showMessageDialog(null, "ERROR: La consulta SQL ha retornat més d'un resultat");
			}
			rs.next();
		} catch (Exception e) {
			LOGGER.log(Level.WARNING, "event=sql_query_failed", e);
		}
	}

	public String getAttribute(String attr) throws SQLException {
		return rs.getString(attr);
	}
}
