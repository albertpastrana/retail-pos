//    Openbravo POS is a point of sales application designed for touch screens.
//    Copyright (C) 2008-2009 Openbravo, S.L.
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

package com.openbravo.pos.config;

import com.openbravo.data.user.DirtyManager;
import com.openbravo.pos.forms.AppConfig;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.payment.ExternalVoucher;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JTextField;

/** Configuration page for the manual external-terminal card flow. */
public class JPanelConfigPayment extends javax.swing.JPanel implements PanelConfig {

	private final DirtyManager dirty = new DirtyManager();
	private final JTextField externalVoucherName = new JTextField(22);

	public JPanelConfigPayment() {
		setBorder(javax.swing.BorderFactory.createTitledBorder(AppLocal.getIntString("Label.Payment")));
		setLayout(new java.awt.GridBagLayout());
		java.awt.GridBagConstraints c = new java.awt.GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.anchor = java.awt.GridBagConstraints.LINE_START;
		c.insets = new java.awt.Insets(8, 8, 8, 8);
		add(new JLabel(AppLocal.getIntString("message.paymentmanualterminal")), c);
		c.gridy = 1;
		c.gridwidth = 1;
		add(new JLabel(AppLocal.getIntString("label.externalvoucher.name")), c);
		c.gridx = 1;
		add(externalVoucherName, c);
		externalVoucherName.getDocument().addDocumentListener(dirty);
	}

	public boolean hasChanged() {
		return dirty.isDirty();
	}

	public Component getConfigComponent() {
		return this;
	}

	public void loadProperties(AppConfig config) {
		externalVoucherName.setText(ExternalVoucher.name(config.getProperty(ExternalVoucher.NAME_KEY)));
		dirty.setDirty(false);
	}

	public void saveProperties(AppConfig config) {
		config.setProperty(ExternalVoucher.NAME_KEY,
				dirty.isDirty() ? ExternalVoucher.name(externalVoucherName.getText()) : null);
		// Remove settings from installations that used the deleted integrations.
		config.setProperty("payment.magcardreader", null);
		config.setProperty("payment.gateway", null);
		config.setProperty("payment.testmode", null);
	}

	@Override
	public void markSaved() {
		dirty.setDirty(false);
	}
}
