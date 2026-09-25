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

package com.openbravo.pos.payment;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.format.Formats;
import com.openbravo.pos.customers.CustomerInfoExt;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.util.RoundUtils;
import com.openbravo.beans.JNumberEvent;
import com.openbravo.beans.JNumberEventListener;
import com.openbravo.beans.JNumberKeys;
import javax.swing.JButton;
import javax.swing.JTextField;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.AbstractDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.DocumentFilter;
import java.util.logging.Logger;

/**
 *
 * @author adrianromero
 */
public class JPaymentCashPos extends javax.swing.JPanel implements JPaymentInterface {

	private static final Logger LOGGER = Logger.getLogger(JPaymentCashPos.class.getName());

	private JPaymentNotifier m_notifier;

	private double m_dPaid;
	private double m_dTotal;
	private JButton m_jPartialPayment;

	/** Creates new form JPaymentCash */
	public JPaymentCashPos(JPaymentNotifier notifier, DataLogicSystem dlSystem) {

		m_notifier = notifier;

		initComponents();
		addPartialPaymentButton();

		m_jKeys.addJNumberEventListener(new JNumberEventListener() {
			@Override
			public void keyPerformed(JNumberEvent event) {
				if (event.getKey() == '\u007f') {
					m_jTendered.setText(null);
				} else {
					m_jTendered.replaceSelection(Character.toString(event.getKey()));
				}
			}
		});
		m_jKeys.setNumbersOnly(true);
		((AbstractDocument) m_jTendered.getDocument()).setDocumentFilter(new NumericFilter());
		m_jTendered.getDocument().addDocumentListener(new DocumentListener() {
			@Override
			public void insertUpdate(DocumentEvent event) {
				printState();
			}

			@Override
			public void removeUpdate(DocumentEvent event) {
				printState();
			}

			@Override
			public void changedUpdate(DocumentEvent event) {
				printState();
			}
		});
	}

	public void activate(CustomerInfoExt customerext, double dTotal, String transID) {

		m_dTotal = dTotal;
		LOGGER.info("event=cash_payment_activated total=" + dTotal);

		m_jTendered.setText(null);
		java.awt.EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				m_jTendered.requestFocusInWindow();
			}
		});

		printState();
	}

	public PaymentInfo executePayment() {
		LOGGER.info("event=cash_payment_confirmed total=" + m_dTotal + " paid=" + m_dPaid);
		if (m_dPaid - m_dTotal >= 0.0) {
			// pago completo
			return new PaymentInfoCash(m_dTotal, m_dPaid);
		} else {
			// pago parcial
			return new PaymentInfoCash(m_dPaid, m_dPaid);
		}
	}

	public Component getComponent() {
		return this;
	}

	private void printState() {

		Double value;
		try {
			value = (Double) Formats.CURRENCY.parseValue(m_jTendered.getText());
		} catch (Exception exception) {
			value = null;
		}
		if (value == null || value == 0.0) {
			m_dPaid = m_dTotal;
		} else {
			m_dPaid = value;
		}

		int iCompare = RoundUtils.compare(m_dPaid, m_dTotal);

		m_jChangeEuros.setText(Formats.CURRENCY.formatValue(iCompare > 0 ? new Double(m_dPaid - m_dTotal) : 0.0));

		boolean partial = m_dPaid > 0.0 && iCompare < 0;
		m_jPartialPayment.setVisible(partial);
		if (partial) {
			m_jPartialPayment.setText(
					AppLocal.getIntString("button.partialpayment", Formats.CURRENCY.formatValue(new Double(m_dPaid))));
		}
		jPanel4.revalidate();

		m_notifier.setStatus(m_dPaid > 0.0, iCompare >= 0);
	}

	private void addPartialPaymentButton() {
		m_jPartialPayment = new JButton();
		m_jPartialPayment.setVisible(false);
		m_jPartialPayment.setFocusPainted(false);
		m_jPartialPayment.setFocusable(false);
		m_jPartialPayment.setMargin(new Insets(8, 12, 8, 12));
		m_jPartialPayment.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				m_notifier.addSelectedPayment();
			}
		});
		java.awt.GridBagConstraints buttonConstraints = new java.awt.GridBagConstraints();
		buttonConstraints.gridx = 0;
		buttonConstraints.gridy = 2;
		buttonConstraints.gridwidth = 2;
		buttonConstraints.insets = new Insets(6, 6, 6, 6);
		jPanel4.add(m_jPartialPayment, buttonConstraints);
		java.awt.GridBagConstraints fillerConstraints = new java.awt.GridBagConstraints();
		fillerConstraints.gridy = 3;
		fillerConstraints.weighty = 1.0;
		fillerConstraints.fill = java.awt.GridBagConstraints.VERTICAL;
		jPanel4.add(javax.swing.Box.createGlue(), fillerConstraints);
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
				if (Character.isDigit(character) || character == '.' || character == ',') {
					value.append(character);
				}
			}
			return value.toString();
		}
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
		jPanel4 = new javax.swing.JPanel();
		m_jChangeEuros = new javax.swing.JLabel();
		jLabel6 = new javax.swing.JLabel();
		jLabel8 = new javax.swing.JLabel();
		jPanel2 = new javax.swing.JPanel();
		m_jKeys = new JNumberKeys();
		m_jTendered = new JTextField();

		setLayout(new java.awt.BorderLayout());

		jPanel5.setLayout(new java.awt.BorderLayout());

		jPanel4.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 16, 16, 16));
		jPanel4.setLayout(new java.awt.GridBagLayout());
		java.awt.GridBagConstraints formConstraints = new java.awt.GridBagConstraints();
		formConstraints.insets = new java.awt.Insets(6, 6, 6, 6);
		formConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;

		m_jChangeEuros.setBackground(m_jTendered.getBackground());
		m_jChangeEuros.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		m_jChangeEuros.setBorder(m_jTendered.getBorder());
		m_jChangeEuros.setOpaque(true);
		formConstraints.gridx = 1;
		formConstraints.gridy = 1;
		formConstraints.weightx = 1.0;
		jPanel4.add(m_jChangeEuros, formConstraints);

		jLabel6.setText(AppLocal.getIntString("Label.ChangeCash")); // NOI18N
		formConstraints = new java.awt.GridBagConstraints();
		formConstraints.gridx = 0;
		formConstraints.gridy = 1;
		formConstraints.anchor = java.awt.GridBagConstraints.LINE_END;
		formConstraints.insets = new java.awt.Insets(6, 6, 6, 6);
		jPanel4.add(jLabel6, formConstraints);

		jLabel8.setText(AppLocal.getIntString("Label.InputCash")); // NOI18N
		formConstraints.gridy = 0;
		jPanel4.add(jLabel8, formConstraints);

		m_jTendered.setHorizontalAlignment(javax.swing.JTextField.RIGHT);
		formConstraints = new java.awt.GridBagConstraints();
		formConstraints.gridx = 1;
		formConstraints.gridy = 0;
		formConstraints.weightx = 1.0;
		formConstraints.fill = java.awt.GridBagConstraints.HORIZONTAL;
		formConstraints.insets = new java.awt.Insets(6, 6, 6, 6);
		jPanel4.add(m_jTendered, formConstraints);

		jPanel5.add(jPanel4, java.awt.BorderLayout.CENTER);

		add(jPanel5, java.awt.BorderLayout.CENTER);

		jPanel2.setLayout(new java.awt.BorderLayout());

		jPanel2.add(m_jKeys, java.awt.BorderLayout.NORTH);

		add(jPanel2, java.awt.BorderLayout.LINE_END);
	}// </editor-fold>//GEN-END:initComponents

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JLabel jLabel6;
	private javax.swing.JLabel jLabel8;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JPanel jPanel4;
	private javax.swing.JPanel jPanel5;
	private javax.swing.JLabel m_jChangeEuros;
	private JNumberKeys m_jKeys;
	private javax.swing.JTextField m_jTendered;
	// End of variables declaration//GEN-END:variables

}
