//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2008-2009 Openbravo, S.L.
//
//    This program is free software: you can redistribute it and/or modify
//    it under the terms of the GNU General Public License as published by
//    the Free Software Foundation, either version 3 of the License, or
//    (at your option) any later version.

package com.openbravo.pos.payment;

import com.openbravo.basic.BasicException;
import com.openbravo.format.Formats;
import com.openbravo.pos.customers.CustomerInfoExt;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.util.RoundUtils;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class JPaymentPaper extends JPanel implements JPaymentInterface {

	private static final Color ERROR_COLOR = new Color(0xB0, 0x00, 0x20);

	private final JPaymentNotifier notifier;
	private final DataLogicSales dlSales;
	private final JTextField code = new JTextField(18);
	private final JLabel balanceCaption = new JLabel(AppLocal.getIntString("label.voucherbalance"));
	private final JLabel balance = new JLabel();
	private final JLabel amountCaption = new JLabel(AppLocal.getIntString("label.voucheramount"));
	private final JLabel amount = new JLabel();
	private final JLabel message = new JLabel();
	private final JButton use = new JButton(AppLocal.getIntString("button.usevoucher"));
	private final Color messageColor = message.getForeground();

	private double total;
	private double paymentAmount;
	private double remainingBalance;
	private String voucherCode;

	public JPaymentPaper(JPaymentNotifier notifier, AppView app) {
		this.notifier = notifier;
		this.dlSales = (DataLogicSales) app.getBean("com.openbravo.pos.forms.DataLogicSales");
		initComponents();
	}

	public void activate(CustomerInfoExt customerext, double dTotal, String transID) {
		total = dTotal;
		paymentAmount = 0.0;
		remainingBalance = 0.0;
		voucherCode = null;
		code.setText(null);
		showDetails(false);
		message.setForeground(messageColor);
		message.setText(AppLocal.getIntString("message.voucherentercode"));
		notifier.setStatus(false, false);
		code.requestFocusInWindow();
	}

	public PaymentInfo executePayment() {
		String enteredCode = code.getText() == null ? "" : code.getText().trim().toUpperCase();
		if (voucherCode == null || !voucherCode.equals(enteredCode)) {
			validateCode();
		}
		return voucherCode == null ? null
				: new PaymentInfoTicket(paymentAmount, "paperin", voucherCode, remainingBalance);
	}

	public Component getComponent() {
		return this;
	}

	private void validateCode() {
		voucherCode = null;
		paymentAmount = 0.0;
		remainingBalance = 0.0;

		String enteredCode = code.getText() == null ? "" : code.getText().trim().toUpperCase();
		if (enteredCode.length() == 0) {
			showError("message.voucherentercode");
			return;
		}
		if (notifier.isVoucherSelected(enteredCode)) {
			showError("message.voucheralreadyselected");
			return;
		}

		try {
			GiftVoucherInfo voucher = dlSales.findGiftVoucher(enteredCode);
			if (voucher == null) {
				showError("message.vouchernotfound");
				return;
			}
			if (voucher.getBalance() <= 0.0) {
				showError("message.voucherempty");
				return;
			}

			voucherCode = voucher.getCode();
			paymentAmount = RoundUtils.round(Math.min(total, voucher.getBalance()));
			remainingBalance = RoundUtils.round(voucher.getBalance() - paymentAmount);
			code.setText(voucherCode);
			balance.setText(Formats.CURRENCY.formatValue(new Double(voucher.getBalance())));
			amount.setText(Formats.CURRENCY.formatValue(new Double(paymentAmount)));
			message.setForeground(messageColor);
			message.setText(paymentAmount >= total
					? AppLocal.getIntString("message.vouchercoversall")
					: AppLocal.getIntString("message.voucherpartial",
							Formats.CURRENCY.formatValue(new Double(total - paymentAmount))));
			showDetails(true);
			notifier.setStatus(paymentAmount > 0.0, paymentAmount >= total);
		} catch (BasicException e) {
			showError("message.voucherreaderror");
		}
	}

	private void showError(String key) {
		Toolkit.getDefaultToolkit().beep();
		showDetails(false);
		message.setForeground(ERROR_COLOR);
		message.setText(AppLocal.getIntString(key));
		notifier.setStatus(false, false);
	}

	private void showDetails(boolean visible) {
		if (!visible) {
			balance.setText(null);
			amount.setText(null);
		}
		balanceCaption.setVisible(visible);
		balance.setVisible(visible);
		amountCaption.setVisible(visible);
		amount.setVisible(visible);
		use.setVisible(visible);
	}

	private void initComponents() {
		setLayout(new BorderLayout());

		JPanel fields = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(8, 8, 8, 8);
		c.anchor = GridBagConstraints.LINE_START;

		c.gridx = 0;
		c.gridy = 0;
		fields.add(new JLabel(AppLocal.getIntString("label.vouchercode")), c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1.0;
		fields.add(code, c);

		JButton validate = new JButton(AppLocal.getIntString("button.validate"));
		validate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/apply.png")));
		validate.addActionListener(evt -> validateCode());
		c.gridx = 2;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0.0;
		fields.add(validate, c);

		c.gridx = 0;
		c.gridy = 1;
		fields.add(balanceCaption, c);
		c.gridx = 1;
		fields.add(balance, c);

		c.gridx = 0;
		c.gridy = 2;
		fields.add(amountCaption, c);
		c.gridx = 1;
		fields.add(amount, c);

		c.gridx = 0;
		c.gridy = 3;
		c.gridwidth = 3;
		fields.add(message, c);

		use.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/knotes.png")));
		use.addActionListener(evt -> notifier.addSelectedPayment());
		c.gridy = 4;
		fields.add(use, c);

		code.addActionListener(evt -> validateCode());
		showDetails(false);
		add(fields, BorderLayout.NORTH);
	}
}
