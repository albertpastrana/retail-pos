package com.openbravo.pos.inventory;

import com.openbravo.basic.BasicException;
import com.openbravo.data.loader.Session;
import com.openbravo.data.user.DirtyManager;
import com.openbravo.pos.forms.AppLocal;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentListener;

/** Edits optional key/value characteristics directly in the product editor. */
public class ProductAttributesPanel extends JPanel {

	private static final Logger LOGGER = Logger.getLogger(ProductAttributesPanel.class.getName());

	private final JPanel rows = new JPanel(new GridBagLayout());
	private final List<AttributeRow> attributeRows = new ArrayList<AttributeRow>();
	private final JButton add = new JButton();
	private Session session;
	private DirtyManager dirty;
	private boolean loading;

	public ProductAttributesPanel() {
		setLayout(new BorderLayout(0, 6));
		setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		add.setText(AppLocal.getIntString("button.addattribute"));
		add.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				addRow(null, null);
				markDirty();
			}
		});
		JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEADING, 4, 0));
		toolbar.add(add);
		add(toolbar, BorderLayout.NORTH);
		add(new JScrollPane(rows), BorderLayout.CENTER);
	}

	public void setSession(Session session, DirtyManager dirty) {
		this.session = session;
		this.dirty = dirty;
	}

	@Override
	public void setEnabled(boolean enabled) {
		super.setEnabled(enabled);
		add.setEnabled(enabled);
		for (AttributeRow row : attributeRows) {
			row.key.setEnabled(enabled);
			row.value.setEnabled(enabled);
			row.remove.setEnabled(enabled);
		}
	}

	public void load(String productId) {
		loading = true;
		int loadedRows = 0;
		try {
			attributeRows.clear();
			rows.removeAll();
			if (productId != null && session != null) {
				try (PreparedStatement statement = session.getConnection().prepareStatement(
						"SELECT ATTRIBUTE_KEY, ATTRIBUTE_VALUE FROM PRODUCT_ATTRIBUTES WHERE PRODUCT_ID = ? ORDER BY ATTRIBUTE_KEY")) {
					statement.setString(1, productId);
					try (ResultSet result = statement.executeQuery()) {
						while (result.next()) {
							addRow(result.getString(1), result.getString(2));
							loadedRows++;
						}
					}
				} catch (SQLException exception) {
					LOGGER.log(Level.WARNING, "event=product_attributes_load_failed product_id=" + productId,
							exception);
				}
			}
			refreshRows();
			LOGGER.info("event=product_attributes_loaded product_id=" + productId + " rows=" + loadedRows);
		} finally {
			loading = false;
		}
	}

	public void save(String productId) throws BasicException {
		if (productId == null || session == null) {
			return;
		}
		List<String[]> values = values();
		LOGGER.info("event=product_attributes_save_start product_id=" + productId + " rows=" + values.size());
		try {
			Connection connection = session.getConnection();
			session.begin();
			try (PreparedStatement delete = connection
					.prepareStatement("DELETE FROM PRODUCT_ATTRIBUTES WHERE PRODUCT_ID = ?")) {
				delete.setString(1, productId);
				int deletedRows = delete.executeUpdate();
				LOGGER.info("event=product_attributes_deleted product_id=" + productId + " rows=" + deletedRows);
			}
			try (PreparedStatement insert = connection.prepareStatement(
					"INSERT INTO PRODUCT_ATTRIBUTES (ID, PRODUCT_ID, ATTRIBUTE_KEY, ATTRIBUTE_KEY_NORMALIZED, "
							+ "ATTRIBUTE_VALUE, ATTRIBUTE_VALUE_NORMALIZED) VALUES (?, ?, ?, ?, ?, ?)")) {
				for (String[] value : values) {
					insert.setString(1, UUID.randomUUID().toString());
					insert.setString(2, productId);
					insert.setString(3, value[0]);
					insert.setString(4, normalize(value[0]));
					if (value[1] == null) {
						insert.setNull(5, java.sql.Types.VARCHAR);
						insert.setNull(6, java.sql.Types.VARCHAR);
					} else {
						insert.setString(5, value[1]);
						insert.setString(6, normalize(value[1]));
					}
					insert.addBatch();
				}
				insert.executeBatch();
			}
			session.commit();
			LOGGER.info("event=product_attributes_save_success product_id=" + productId + " rows=" + values.size());
		} catch (SQLException exception) {
			LOGGER.log(Level.SEVERE,
					"event=product_attributes_save_failed product_id=" + productId + " rows=" + values.size(),
					exception);
			try {
				session.rollback();
				LOGGER.info("event=product_attributes_rollback_success product_id=" + productId);
			} catch (SQLException ignored) {
				LOGGER.log(Level.SEVERE, "event=product_attributes_rollback_failed product_id=" + productId, ignored);
			}
			throw new BasicException(exception);
		}
	}

	private void addRow(String key, String value) {
		AttributeRow row = new AttributeRow(key, value);
		attributeRows.add(row);
		refreshRows();
	}

	private void refreshRows() {
		rows.removeAll();
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(2, 2, 2, 2);
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1.0;
		int rowNumber = 0;
		for (final AttributeRow row : attributeRows) {
			constraints.gridy = rowNumber++;
			constraints.gridx = 0;
			rows.add(row.key, constraints);
			constraints.gridx = 1;
			rows.add(row.value, constraints);
			constraints.gridx = 2;
			constraints.weightx = 0.0;
			rows.add(row.remove, constraints);
			constraints.weightx = 1.0;
		}
		constraints.gridy = rowNumber;
		constraints.gridx = 0;
		constraints.gridwidth = 3;
		constraints.weighty = 1.0;
		constraints.anchor = GridBagConstraints.NORTHWEST;
		rows.add(new JLabel(), constraints);
		rows.revalidate();
		rows.repaint();
	}

	private List<String[]> values() throws BasicException {
		List<String[]> result = new ArrayList<String[]>();
		Set<String> keys = new HashSet<String>();
		for (AttributeRow row : attributeRows) {
			String key = text(row.key);
			if (key.length() == 0) {
				throw new BasicException(AppLocal.getIntString("message.attribute.keyrequired"));
			}
			String normalizedKey = normalize(key);
			if (!keys.add(normalizedKey)) {
				throw new BasicException(AppLocal.getIntString("message.attribute.duplicatekey"));
			}
			String value = text(row.value);
			result.add(new String[]{key, value.length() == 0 ? null : value});
		}
		return result;
	}

	/**
	 * Raw editor values for detecting other-field drafts; never validates or writes
	 * them.
	 */
	List<String> batchSnapshot() {
		List<String> snapshot = new ArrayList<String>();
		for (AttributeRow row : attributeRows) {
			snapshot.add(text(row.key));
			snapshot.add(text(row.value));
		}
		return snapshot;
	}

	private String text(JComboBox<String> combo) {
		Object value = combo.isEditable() ? combo.getEditor().getItem() : combo.getSelectedItem();
		return value == null ? "" : value.toString().trim();
	}

	private String normalize(String value) {
		return value.trim().toLowerCase(Locale.ROOT);
	}

	private void markDirty() {
		if (!loading && dirty != null) {
			dirty.setDirty(true);
		}
	}

	private class AttributeRow {
		private final JComboBox<String> key = combo(suggestions(null));
		private final JComboBox<String> value = combo(Collections.<String>emptyList());
		private final JButton remove = new JButton(AppLocal.getIntString("button.delete"));

		AttributeRow(String keyText, String valueText) {
			if (keyText != null) {
				key.getEditor().setItem(keyText);
				value.setModel(
						new javax.swing.DefaultComboBoxModel<String>(suggestions(keyText).toArray(new String[0])));
				value.setEditable(true);
				value.getEditor().setItem(valueText == null ? "" : valueText);
			}
			key.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					String selectedKey = text(key);
					value.setModel(new javax.swing.DefaultComboBoxModel<String>(
							suggestions(selectedKey).toArray(new String[0])));
					value.setEditable(true);
					markDirty();
				}
			});
			addDirtyListener(key);
			addDirtyListener(value);
			remove.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent event) {
					attributeRows.remove(AttributeRow.this);
					refreshRows();
					markDirty();
				}
			});
		}
	}

	private JComboBox<String> combo(List<String> items) {
		JComboBox<String> combo = new JComboBox<String>(items.toArray(new String[0]));
		combo.setEditable(true);
		return combo;
	}

	private void addDirtyListener(JComboBox<String> combo) {
		if (combo.getEditor().getEditorComponent() instanceof JTextField) {
			((JTextField) combo.getEditor().getEditorComponent()).getDocument()
					.addDocumentListener(new DocumentListener() {
						@Override
						public void insertUpdate(javax.swing.event.DocumentEvent event) {
							markDirty();
						}
						@Override
						public void removeUpdate(javax.swing.event.DocumentEvent event) {
							markDirty();
						}
						@Override
						public void changedUpdate(javax.swing.event.DocumentEvent event) {
							markDirty();
						}
					});
		}
	}

	private List<String> suggestions(String key) {
		List<String> result = new ArrayList<String>();
		if (session == null) {
			return result;
		}
		String sql = key == null || key.trim().length() == 0
				? "SELECT DISTINCT ATTRIBUTE_KEY FROM PRODUCT_ATTRIBUTES ORDER BY ATTRIBUTE_KEY"
				: "SELECT DISTINCT ATTRIBUTE_VALUE FROM PRODUCT_ATTRIBUTES WHERE ATTRIBUTE_KEY_NORMALIZED = ? "
						+ "AND ATTRIBUTE_VALUE IS NOT NULL ORDER BY ATTRIBUTE_VALUE";
		try (PreparedStatement statement = session.getConnection().prepareStatement(sql)) {
			if (key != null && key.trim().length() > 0) {
				statement.setString(1, normalize(key));
			}
			try (ResultSet resultSet = statement.executeQuery()) {
				while (resultSet.next()) {
					result.add(resultSet.getString(1));
				}
			}
		} catch (SQLException ignored) {
			// Suggestions must not prevent editing a product.
		}
		return result;
	}
}
