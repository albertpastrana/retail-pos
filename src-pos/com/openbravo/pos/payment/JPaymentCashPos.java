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
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.format.Formats;
import com.openbravo.pos.customers.CustomerInfoExt;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.scripting.ScriptEngine;
import com.openbravo.pos.scripting.ScriptException;
import com.openbravo.pos.scripting.ScriptFactory;
import com.openbravo.pos.util.RoundUtils;
import com.openbravo.pos.util.ThumbNailBuilder;
import com.openbravo.beans.JNumberEvent;
import com.openbravo.beans.JNumberEventListener;
import com.openbravo.beans.JNumberKeys;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.SwingConstants;
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

		m_jTendered.addPropertyChangeListener("Edition", new RecalculateState());
		m_jKeys.addJNumberEventListener(new JNumberEventListener() {
			@Override
			public void keyPerformed(JNumberEvent event) {
				m_jTendered.transChar(event.getKey());
			}
		});

		String code = dlSystem.getResourceAsXML("payment.cash");
		if (code != null) {
			try {
				ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.BEANSHELL);
				script.put("payment", new ScriptPaymentCash(dlSystem));
				script.eval(code);
			} catch (ScriptException e) {
				MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.cannotexecute"),
						e);
				msg.show(this);
			}
		}

	}

	public void activate(CustomerInfoExt customerext, double dTotal, String transID) {

		m_dTotal = dTotal;
		LOGGER.info("event=cash_payment_activated total=" + dTotal);

		m_jTendered.reset();
		m_jTendered.activate();
		java.awt.EventQueue.invokeLater(new Runnable() {
			@Override
			public void run() {
				m_jKeyFactory.requestFocusInWindow();
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

		Double value = m_jTendered.getDoubleValue();
		if (value == null || value == 0.0) {
			m_dPaid = m_dTotal;
		} else {
			m_dPaid = value;
		}

		int iCompare = RoundUtils.compare(m_dPaid, m_dTotal);

		m_jMoneyEuros.setText(Formats.CURRENCY.formatValue(new Double(m_dPaid)));
		m_jChangeEuros.setText(iCompare > 0 ? Formats.CURRENCY.formatValue(new Double(m_dPaid - m_dTotal)) : null);

		boolean partial = m_dPaid > 0.0 && iCompare < 0;
		m_jPartialPayment.setVisible(partial);
		if (partial) {
			m_jPartialPayment.setText(
					AppLocal.getIntString("button.partialpayment", Formats.CURRENCY.formatValue(new Double(m_dPaid))));
			int available = jPanel4.getWidth() - 40;
			int width = m_jPartialPayment.getPreferredSize().width;
			m_jPartialPayment.setBounds(20, 80, available > 0 ? Math.min(width, available) : width, 36);
		}
		jPanel4.setPreferredSize(new java.awt.Dimension(0, partial ? 140 : 100));
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
		jPanel4.add(m_jPartialPayment);
	}

	private class RecalculateState implements PropertyChangeListener {
		public void propertyChange(PropertyChangeEvent evt) {
			printState();
		}
	}

	public class ScriptPaymentCash {

		private DataLogicSystem dlSystem;
		private ThumbNailBuilder tnbbutton;

		public ScriptPaymentCash(DataLogicSystem dlSystem) {
			this.dlSystem = dlSystem;
			tnbbutton = new ThumbNailBuilder(64, 54, "com/openbravo/images/cash.png");
		}

		public void addButton(String image, double amount) {
			JButton btn = new JButton();
			btn.setIcon(new ImageIcon(tnbbutton.getThumbNailText(dlSystem.getResourceAsImage(image),
					Formats.CURRENCY.formatValue(amount))));
			btn.setFocusPainted(false);
			btn.setFocusable(false);
			btn.setRequestFocusEnabled(false);
			btn.setHorizontalTextPosition(SwingConstants.CENTER);
			btn.setVerticalTextPosition(SwingConstants.BOTTOM);
			btn.setMargin(new Insets(2, 2, 2, 2));
			btn.addActionListener(new AddAmount(amount));
			jPanel6.add(btn);
		}
	}

	private class AddAmount implements ActionListener {
		private double amount;

		public AddAmount(double amount) {
			this.amount = amount;
		}

		public void actionPerformed(ActionEvent e) {
			Double tendered = m_jTendered.getDoubleValue();
			if (tendered == null) {
				m_jTendered.setDoubleValue(amount);
			} else {
				m_jTendered.setDoubleValue(tendered + amount);
			}

			printState();
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
		m_jMoneyEuros = new javax.swing.JLabel();
		jPanel6 = new javax.swing.JPanel();
		jPanel2 = new javax.swing.JPanel();
		jPanel1 = new javax.swing.JPanel();
		m_jKeys = new JNumberKeys();
		jPanel3 = new javax.swing.JPanel();
		m_jTendered = new com.openbravo.editor.JEditorCurrencyPositive();
		m_jKeyFactory = new javax.swing.JTextField();

		setLayout(new java.awt.BorderLayout());

		jPanel5.setLayout(new java.awt.BorderLayout());

		jPanel4.setPreferredSize(new java.awt.Dimension(0, 100));
		jPanel4.setLayout(null);

		m_jChangeEuros.setBackground(com.openbravo.pos.theme.RetailPOSColors.surface100());
		m_jChangeEuros.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		m_jChangeEuros.setBorder(javax.swing.BorderFactory.createCompoundBorder(
				javax.swing.BorderFactory
						.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")),
				javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
		m_jChangeEuros.setOpaque(true);
		m_jChangeEuros.setPreferredSize(new java.awt.Dimension(150, 25));
		jPanel4.add(m_jChangeEuros);
		m_jChangeEuros.setBounds(120, 50, 150, 25);

		jLabel6.setText(AppLocal.getIntString("Label.ChangeCash")); // NOI18N
		jPanel4.add(jLabel6);
		jLabel6.setBounds(20, 50, 100, 15);

		jLabel8.setText(AppLocal.getIntString("Label.InputCash")); // NOI18N
		jPanel4.add(jLabel8);
		jLabel8.setBounds(20, 20, 100, 15);

		m_jMoneyEuros.setBackground(com.openbravo.pos.theme.RetailPOSColors.surface100());
		m_jMoneyEuros.setHorizontalAlignment(javax.swing.SwingConstants.RIGHT);
		m_jMoneyEuros.setBorder(javax.swing.BorderFactory.createCompoundBorder(
				javax.swing.BorderFactory
						.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")),
				javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
		m_jMoneyEuros.setOpaque(true);
		m_jMoneyEuros.setPreferredSize(new java.awt.Dimension(150, 25));
		jPanel4.add(m_jMoneyEuros);
		m_jMoneyEuros.setBounds(120, 20, 150, 25);

		jPanel5.add(jPanel4, java.awt.BorderLayout.NORTH);

		jPanel6.setLayout(new java.awt.GridLayout(0, 4, 8, 8));
		jPanel5.add(jPanel6, java.awt.BorderLayout.CENTER);

		add(jPanel5, java.awt.BorderLayout.CENTER);

		jPanel2.setLayout(new java.awt.BorderLayout());

		jPanel1.setLayout(new javax.swing.BoxLayout(jPanel1, javax.swing.BoxLayout.Y_AXIS));
		jPanel1.add(m_jKeys);

		jPanel3.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		jPanel3.setLayout(new java.awt.BorderLayout());
		jPanel3.add(m_jTendered, java.awt.BorderLayout.CENTER);
		m_jKeyFactory.setPreferredSize(new java.awt.Dimension(0, 0));
		m_jKeyFactory.setBorder(null);
		m_jKeyFactory.addKeyListener(new KeyAdapter() {
			@Override
			public void keyTyped(KeyEvent event) {
				char key = event.getKeyChar();
				if ((key >= '0' && key <= '9') || key == '.' || key == ',') {
					m_jTendered.transChar(key == ',' ? '.' : key);
				} else if (key == '\b' || key == '\u007f') {
					m_jTendered.transChar('\u007f');
				}
				m_jKeyFactory.setText(null);
			}
		});
		jPanel3.add(m_jKeyFactory, java.awt.BorderLayout.SOUTH);

		jPanel1.add(jPanel3);

		jPanel2.add(jPanel1, java.awt.BorderLayout.NORTH);

		add(jPanel2, java.awt.BorderLayout.LINE_END);
	}// </editor-fold>//GEN-END:initComponents

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JLabel jLabel6;
	private javax.swing.JLabel jLabel8;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JPanel jPanel3;
	private javax.swing.JPanel jPanel4;
	private javax.swing.JPanel jPanel5;
	private javax.swing.JPanel jPanel6;
	private javax.swing.JLabel m_jChangeEuros;
	private JNumberKeys m_jKeys;
	private javax.swing.JLabel m_jMoneyEuros;
	private javax.swing.JTextField m_jKeyFactory;
	private com.openbravo.editor.JEditorCurrencyPositive m_jTendered;
	// End of variables declaration//GEN-END:variables

}
