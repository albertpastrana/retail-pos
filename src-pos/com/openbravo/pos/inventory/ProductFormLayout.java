package com.openbravo.pos.inventory;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

/**
 * One labelled row per field, pinned to the top of whatever panel it sits in.
 * The till dialog and the product editor share this so a price looks the same in
 * both places.
 */
public final class ProductFormLayout {

	public static final Insets ROW_INSETS = new Insets(4, 4, 4, 4);

	private ProductFormLayout() {
	}

	public static void addRow(JPanel panel, int row, String label, JComponent field) {
		addRow(panel, row, new JLabel(label), field);
	}

	public static void addRow(JPanel panel, int row, JComponent label, JComponent field) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = ROW_INSETS;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.gridy = row;
		panel.add(label, constraints);
		constraints.gridx = 1;
		constraints.weightx = 1.0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		panel.add(field, constraints);
	}

	public static void addFullRow(JPanel panel, int row, JComponent field) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = ROW_INSETS;
		constraints.anchor = GridBagConstraints.WEST;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.gridy = row;
		constraints.gridwidth = 2;
		constraints.weightx = 1.0;
		panel.add(field, constraints);
	}

	// Prices, codes and checkboxes keep the width they ask for: the row around
	// them stretches instead, and carries whatever reads with the field.
	public static JPanel inline(JComponent... parts) {
		JPanel row = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
		row.setOpaque(false);
		for (JComponent part : parts) {
			row.add(part);
		}
		return row;
	}

	public static JTextField numberField(boolean editable) {
		return numberField(editable, 8);
	}

	public static JTextField numberField(boolean editable, int columns) {
		JTextField field = new JTextField(columns);
		field.setHorizontalAlignment(JTextField.RIGHT);
		if (!editable) {
			field.setEditable(false);
			field.setFocusable(false);
		}
		return field;
	}

	// GridBagLayout centres its rows in whatever height it gets, so the form needs
	// a north slot to stay pinned to the top.
	public static JPanel topAligned(JComponent fields) {
		JPanel holder = new JPanel(new BorderLayout());
		holder.add(fields, BorderLayout.NORTH);
		return holder;
	}

	public static JScrollPane scrollable(JComponent content) {
		JScrollPane scroll = new JScrollPane(content);
		scroll.setBorder(null);
		scroll.getVerticalScrollBar().setUnitIncrement(16);
		return scroll;
	}

	public static void onEdit(JTextField field, final Runnable action) {
		field.getDocument().addDocumentListener(new DocumentListener() {
			public void insertUpdate(DocumentEvent e) {
				action.run();
			}

			public void removeUpdate(DocumentEvent e) {
				action.run();
			}

			public void changedUpdate(DocumentEvent e) {
				action.run();
			}
		});
	}
}
