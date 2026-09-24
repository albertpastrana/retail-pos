//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2007-2009 Openbravo, S.L.
//    http://www.openbravo.com/product/pos
//
//    This file is part of Openbravo POS.
//
//    Openbravo POS is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.
//
//    Openbravo POS is distributed in the hope that it will be useful,
//    but WITHOUT ANY WARRANTY; without even the implied warranty of
//    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
//    GNU General Public License for more details.
//
//    You should have received a copy of the GNU General Public License
//    along with Openbravo POS.  If not, see <http://www.gnu.org/licenses/>.

package com.openbravo.pos.sales;

import com.openbravo.basic.BasicException;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.Window;
import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import com.openbravo.beans.JNumberEvent;
import com.openbravo.beans.JNumberEventListener;
import com.openbravo.beans.JNumberKeys;
import com.openbravo.format.Formats;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.SupervisorAuthorization;
import com.openbravo.pos.ticket.TicketLineInfo;

/**
 *
 * @author adrianromero
 */
public class JProductLineEdit extends javax.swing.JDialog {

	private TicketLineInfo returnLine;
	private TicketLineInfo m_oLine;
	private boolean m_bunitsok;
	private boolean m_bpriceok;
	private boolean m_updatingFields;

	/** Creates new form JProductLineEdit */
	private JProductLineEdit(java.awt.Frame parent, boolean modal) {
		super(parent, modal);
	}
	/** Creates new form JProductLineEdit */
	private JProductLineEdit(java.awt.Dialog parent, boolean modal) {
		super(parent, modal);
	}

	private TicketLineInfo init(AppView app, TicketLineInfo oLine) throws BasicException {
		// Inicializo los componentes
		initComponents();

		if (oLine.getTaxInfo() == null) {
			throw new BasicException(AppLocal.getIntString("message.cannotcalculatetaxes"));
		}

		m_oLine = new TicketLineInfo(oLine);
		m_bunitsok = true;
		m_bpriceok = true;

		m_jName.setEnabled(m_oLine.getProductID() == null
				&& app.getAppUserView().getUser().hasPermission("com.openbravo.pos.sales.JPanelTicketEdits"));
		m_jPrice.setEnabled(app.getAppUserView().getUser().hasPermission("com.openbravo.pos.sales.JPanelTicketEdits"));
		m_jPriceTax
				.setEnabled(app.getAppUserView().getUser().hasPermission("com.openbravo.pos.sales.JPanelTicketEdits"));

		m_jName.setText(m_oLine.getProperty("product.name"));
		m_jUnits.setText(Double.toString(oLine.getMultiply()));
		m_jPrice.setText(Double.toString(oLine.getPrice()));
		m_jPriceTax.setText(Double.toString(oLine.getPriceTax()));
		m_jTaxrate.setText(oLine.getTaxInfo().getName());
		m_jName.setColumns(24);
		m_jUnits.setColumns(12);
		m_jPrice.setColumns(12);
		m_jPriceTax.setColumns(12);
		m_jUnits.setHorizontalAlignment(JTextField.RIGHT);
		m_jPrice.setHorizontalAlignment(JTextField.RIGHT);
		m_jPriceTax.setHorizontalAlignment(JTextField.RIGHT);

		((AbstractDocument) m_jUnits.getDocument()).setDocumentFilter(new NumericFilter());
		((AbstractDocument) m_jPrice.getDocument()).setDocumentFilter(new NumericFilter());
		((AbstractDocument) m_jPriceTax.getDocument()).setDocumentFilter(new NumericFilter());
		m_jName.getDocument().addDocumentListener(new FieldListener() {
			@Override
			public void update() {
				m_oLine.setProperty("product.name", m_jName.getText());
			}
		});
		m_jUnits.getDocument().addDocumentListener(new FieldListener() {
			@Override
			public void update() {
				Double value = parse(m_jUnits, Formats.DOUBLE);
				m_bunitsok = value != null && value != 0.0;
				if (m_bunitsok) {
					m_oLine.setMultiply(value);
				}
				printTotals();
			}
		});
		m_jPrice.getDocument().addDocumentListener(new FieldListener() {
			@Override
			public void update() {
				if (m_updatingFields) {
					return;
				}
				Double value = parse(m_jPrice, Formats.CURRENCY);
				m_bpriceok = value != null && value != 0.0;
				if (m_bpriceok) {
					m_oLine.setPrice(value);
					m_updatingFields = true;
					try {
						m_jPriceTax.setText(Formats.CURRENCY.formatValue(m_oLine.getPriceTax()));
					} finally {
						m_updatingFields = false;
					}
				}
				printTotals();
			}
		});
		m_jPriceTax.getDocument().addDocumentListener(new FieldListener() {
			@Override
			public void update() {
				if (m_updatingFields) {
					return;
				}
				Double value = parse(m_jPriceTax, Formats.CURRENCY);
				m_bpriceok = value != null && value != 0.0;
				if (m_bpriceok) {
					m_oLine.setPriceTax(value);
					m_updatingFields = true;
					try {
						m_jPrice.setText(Formats.CURRENCY.formatValue(m_oLine.getPrice()));
					} finally {
						m_updatingFields = false;
					}
				}
				printTotals();
			}
		});

		m_jKeys.addJNumberEventListener(new JNumberEventListener() {
			@Override
			public void keyPerformed(JNumberEvent event) {
				JTextField field = getFocusedNumericField();
				if (field == null) {
					return;
				}
				if (event.getKey() == '\u007f') {
					field.setText(null);
				} else {
					field.replaceSelection(Character.toString(event.getKey()));
				}
			}
		});
		m_jKeys.setNumbersOnly(true);
		m_jKeys.setDotVisible(true);

		if (m_jName.isEnabled()) {
			m_jName.requestFocusInWindow();
		} else {
			m_jUnits.requestFocusInWindow();
		}

		printTotals();

		getRootPane().setDefaultButton(m_jButtonOK);
		returnLine = null;
		setVisible(true);

		return returnLine;
	}

	private void printTotals() {

		if (m_bunitsok && m_bpriceok) {
			m_jSubtotal.setText(m_oLine.printSubValue());
			m_jTotal.setText(m_oLine.printValue());
			m_jButtonOK.setEnabled(true);
		} else {
			m_jSubtotal.setText(null);
			m_jTotal.setText(null);
			m_jButtonOK.setEnabled(false);
		}
	}

	private Double parse(JTextField field, Formats format) {
		try {
			return (Double) format.parseValue(field.getText());
		} catch (BasicException exception) {
			return null;
		}
	}

	private JTextField getFocusedNumericField() {
		if (m_jUnits.hasFocus()) {
			return m_jUnits;
		}
		if (m_jPrice.hasFocus()) {
			return m_jPrice;
		}
		if (m_jPriceTax.hasFocus()) {
			return m_jPriceTax;
		}
		return null;
	}

	private abstract static class FieldListener implements DocumentListener {
		@Override
		public void insertUpdate(DocumentEvent event) {
			update();
		}

		@Override
		public void removeUpdate(DocumentEvent event) {
			update();
		}

		@Override
		public void changedUpdate(DocumentEvent event) {
			update();
		}

		abstract void update();
	}

	private static class NumericFilter extends DocumentFilter {
		@Override
		public void insertString(FilterBypass bypass, int offset, String text, AttributeSet attributes)
				throws BadLocationException {
			if (text != null) {
				bypass.insertString(offset, numericCharacters(text), attributes);
			}
		}

		@Override
		public void replace(FilterBypass bypass, int offset, int length, String text, AttributeSet attributes)
				throws BadLocationException {
			bypass.replace(offset, length, text == null ? null : numericCharacters(text), attributes);
		}

		private String numericCharacters(String text) {
			StringBuilder value = new StringBuilder(text.length());
			for (int i = 0; i < text.length(); i++) {
				char character = text.charAt(i);
				if (Character.isDigit(character) || character == '.' || character == ',' || character == '-') {
					value.append(character);
				}
			}
			return value.toString();
		}
	}

	private static Window getWindow(Component parent) {
		if (parent == null) {
			return new JFrame();
		} else if (parent instanceof Frame || parent instanceof Dialog) {
			return (Window) parent;
		} else {
			return getWindow(parent.getParent());
		}
	}

	public static TicketLineInfo showMessage(Component parent, AppView app, TicketLineInfo oLine)
			throws BasicException {

		Window window = getWindow(parent);

		JProductLineEdit myMsg;
		if (window instanceof Frame) {
			myMsg = new JProductLineEdit((Frame) window, true);
		} else {
			myMsg = new JProductLineEdit((Dialog) window, true);
		}
		TicketLineInfo result = myMsg.init(app, oLine);
		if (result != null && app.getAppUserView().getUser().isSellerSession()
				&& (result.getPrice() != oLine.getPrice() || result.getPriceTax() != oLine.getPriceTax())
				&& !SupervisorAuthorization.authorize(parent, app, AppLocal.getIntString("message.authorizedprice"))) {
			return null;
		}
		return result;
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the Form Editor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated
	// Code">//GEN-BEGIN:initComponents
	private void initComponents() {

		jPanel5 = new javax.swing.JPanel();
		jPanel2 = new javax.swing.JPanel();
		jLabel1 = new javax.swing.JLabel();
		jLabel2 = new javax.swing.JLabel();
		jLabel3 = new javax.swing.JLabel();
		jLabel4 = new javax.swing.JLabel();
		m_jName = new javax.swing.JTextField();
		m_jUnits = new javax.swing.JTextField();
		m_jPrice = new javax.swing.JTextField();
		m_jPriceTax = new javax.swing.JTextField();
		m_jTaxrate = new javax.swing.JLabel();
		jLabel5 = new javax.swing.JLabel();
		jLabel6 = new javax.swing.JLabel();
		m_jTotal = new javax.swing.JLabel();
		jLabel7 = new javax.swing.JLabel();
		m_jSubtotal = new javax.swing.JLabel();
		jPanel1 = new javax.swing.JPanel();
		m_jButtonOK = new javax.swing.JButton();
		m_jButtonCancel = new javax.swing.JButton();
		jPanel3 = new javax.swing.JPanel();
		jPanel4 = new javax.swing.JPanel();
		m_jKeys = new com.openbravo.beans.JNumberKeys();

		setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
		setTitle(AppLocal.getIntString("label.editline")); // NOI18N

		jPanel5.setLayout(new java.awt.BorderLayout());

		jLabel1.setText(AppLocal.getIntString("label.price")); // NOI18N
		jLabel2.setText(AppLocal.getIntString("label.units")); // NOI18N
		jLabel3.setText(AppLocal.getIntString("label.pricetax")); // NOI18N
		jLabel4.setText(AppLocal.getIntString("label.item")); // NOI18N
		jLabel5.setText(AppLocal.getIntString("label.tax")); // NOI18N
		jLabel6.setText(AppLocal.getIntString("label.totalcash")); // NOI18N
		jLabel7.setText(AppLocal.getIntString("label.subtotalcash")); // NOI18N

		m_jTaxrate.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		m_jTaxrate.setBorder(javax.swing.BorderFactory.createCompoundBorder(
				javax.swing.BorderFactory
						.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")),
				javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
		m_jTaxrate.setOpaque(true);
		m_jTaxrate.setBackground(javax.swing.UIManager.getDefaults().getColor("TextField.disabledBackground"));
		m_jTotal.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		m_jSubtotal.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		m_jTotal.setBorder(m_jTaxrate.getBorder());
		m_jSubtotal.setBorder(m_jTaxrate.getBorder());
		m_jTotal.setOpaque(true);
		m_jSubtotal.setOpaque(true);
		m_jTotal.setBackground(m_jTaxrate.getBackground());
		m_jSubtotal.setBackground(m_jTaxrate.getBackground());

		jPanel1.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT));

		m_jButtonOK.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/button_ok.png"))); // NOI18N
		m_jButtonOK.setText(AppLocal.getIntString("button.savechanges")); // NOI18N
		m_jButtonOK.setFocusPainted(false);
		m_jButtonOK.setFocusable(false);
		m_jButtonOK.setMargin(new java.awt.Insets(8, 16, 8, 16));
		m_jButtonOK.setRequestFocusEnabled(false);
		m_jButtonOK.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jButtonOKActionPerformed(evt);
			}
		});
		m_jButtonCancel
				.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/button_cancel.png"))); // NOI18N
		m_jButtonCancel.setText(AppLocal.getIntString("button.canceledit")); // NOI18N
		m_jButtonCancel.setFocusPainted(false);
		m_jButtonCancel.setFocusable(false);
		m_jButtonCancel.setMargin(new java.awt.Insets(8, 16, 8, 16));
		m_jButtonCancel.setRequestFocusEnabled(false);
		m_jButtonCancel.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jButtonCancelActionPerformed(evt);
			}
		});
		jPanel1.add(m_jButtonCancel);
		jPanel1.add(m_jButtonOK);

		getContentPane().add(jPanel5, java.awt.BorderLayout.CENTER);

		jPanel3.setLayout(new java.awt.BorderLayout());

		jPanel4.setLayout(new javax.swing.BoxLayout(jPanel4, javax.swing.BoxLayout.Y_AXIS));
		jPanel4.add(m_jKeys);

		getContentPane().add(jPanel3, java.awt.BorderLayout.EAST);

		configureLayout();
		pack();
		setLocationRelativeTo(getOwner());
	}// </editor-fold>//GEN-END:initComponents

	private void configureLayout() {
		jPanel2.removeAll();
		jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 20, 16, 20));
		jPanel2.setLayout(new java.awt.GridBagLayout());

		addFieldRow(jLabel4, m_jName, 0);
		addFieldRow(jLabel2, m_jUnits, 1);
		addFieldRow(jLabel1, m_jPrice, 2);
		addFieldRow(jLabel3, m_jPriceTax, 3);
		addFieldRow(jLabel5, m_jTaxrate, 4);
		addFieldRow(jLabel7, m_jSubtotal, 5);
		addFieldRow(jLabel6, m_jTotal, 6);

		jPanel5.removeAll();
		jPanel5.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 0, 12, 0));
		jPanel5.add(jPanel2, java.awt.BorderLayout.CENTER);
		jPanel5.add(jPanel1, java.awt.BorderLayout.SOUTH);

		jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 0, 16, 20));
		jPanel4.removeAll();
		jPanel4.setBorder(null);
		jPanel4.add(m_jKeys);
		jPanel3.removeAll();
		jPanel3.add(jPanel4, java.awt.BorderLayout.CENTER);
	}

	private void addFieldRow(javax.swing.JLabel label, java.awt.Component field, int row) {
		java.awt.GridBagConstraints labelConstraints = new java.awt.GridBagConstraints();
		labelConstraints.gridx = 0;
		labelConstraints.gridy = row;
		labelConstraints.anchor = java.awt.GridBagConstraints.LINE_START;
		labelConstraints.insets = new java.awt.Insets(5, 0, 5, 12);
		jPanel2.add(label, labelConstraints);

		java.awt.GridBagConstraints fieldConstraints = new java.awt.GridBagConstraints();
		fieldConstraints.gridx = 1;
		fieldConstraints.gridy = row;
		fieldConstraints.weightx = 1.0;
		fieldConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		fieldConstraints.insets = new java.awt.Insets(5, 0, 5, 0);
		jPanel2.add(field, fieldConstraints);
	}

	private void m_jButtonCancelActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jButtonCancelActionPerformed

		dispose();

	}// GEN-LAST:event_m_jButtonCancelActionPerformed

	private void m_jButtonOKActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jButtonOKActionPerformed

		returnLine = m_oLine;

		dispose();

	}// GEN-LAST:event_m_jButtonOKActionPerformed

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JLabel jLabel4;
	private javax.swing.JLabel jLabel5;
	private javax.swing.JLabel jLabel6;
	private javax.swing.JLabel jLabel7;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JPanel jPanel3;
	private javax.swing.JPanel jPanel4;
	private javax.swing.JPanel jPanel5;
	private javax.swing.JButton m_jButtonCancel;
	private javax.swing.JButton m_jButtonOK;
	private com.openbravo.beans.JNumberKeys m_jKeys;
	private javax.swing.JTextField m_jName;
	private javax.swing.JTextField m_jPrice;
	private javax.swing.JTextField m_jPriceTax;
	private javax.swing.JLabel m_jSubtotal;
	private javax.swing.JLabel m_jTaxrate;
	private javax.swing.JLabel m_jTotal;
	private javax.swing.JTextField m_jUnits;
	// End of variables declaration//GEN-END:variables

}
