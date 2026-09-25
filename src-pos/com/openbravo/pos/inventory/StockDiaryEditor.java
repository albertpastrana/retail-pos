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

package com.openbravo.pos.inventory;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Date;
import java.util.UUID;
import com.openbravo.beans.DateUtils;
import com.openbravo.beans.JCalendarDialog;
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.format.Formats;
import com.openbravo.data.user.DirtyManager;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.pos.catalog.CatalogSelector;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.catalog.JCatalog;
import com.openbravo.pos.panels.JProductFinder;
import com.openbravo.pos.ticket.ProductInfoExt;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;

/**
 *
 * @author adrianromero
 */
public class StockDiaryEditor extends javax.swing.JPanel implements EditorRecord {

	private CatalogSelector m_cat;

	private String m_sID;

	private String productid;
	private String productref;
	private String productcode;
	private String productname;
	private String attsetid;
	private String attsetinstid;
	private String attsetinstdesc;

	private ComboBoxValModel m_ReasonModel;

	private SentenceList m_sentlocations;
	private ComboBoxValModel m_LocationsModel;

	private AppView m_App;
	private DataLogicSales m_dlSales;

	/** Creates new form StockDiaryEditor */
	public StockDiaryEditor(AppView app, DirtyManager dirty) {

		m_App = app;
		m_dlSales = m_App.getBean(DataLogicSales.class);

		initComponents();

		m_cat = new JCatalog(m_dlSales);
		m_cat.addActionListener(new CatalogListener());
		add(m_cat.getComponent(), BorderLayout.SOUTH);

		// El modelo de locales
		m_sentlocations = m_dlSales.getLocationsList();
		m_LocationsModel = new ComboBoxValModel();

		m_ReasonModel = new ComboBoxValModel();
		m_ReasonModel.add(MovementReason.IN_PURCHASE);
		m_ReasonModel.add(MovementReason.IN_REFUND);
		m_ReasonModel.add(MovementReason.IN_MOVEMENT);
		m_ReasonModel.add(MovementReason.OUT_SALE);
		m_ReasonModel.add(MovementReason.OUT_REFUND);
		m_ReasonModel.add(MovementReason.OUT_BREAK);
		m_ReasonModel.add(MovementReason.OUT_MOVEMENT);
		m_jreason.setModel(m_ReasonModel);

		m_jdate.getDocument().addDocumentListener(dirty);
		m_jreason.addActionListener(dirty);
		m_jLocation.addActionListener(dirty);
		jproduct.getDocument().addDocumentListener(dirty);
		jattributes.getDocument().addDocumentListener(dirty);
		m_junits.getDocument().addDocumentListener(dirty);
		m_jprice.getDocument().addDocumentListener(dirty);

		writeValueEOF();
	}

	public void activate() throws BasicException {
		m_cat.loadCatalog();

		m_LocationsModel = new ComboBoxValModel(m_sentlocations.list());
		m_jLocation.setModel(m_LocationsModel); // para que lo refresque
	}

	public void refresh() {
	}

	public void writeValueEOF() {
		m_sID = null;
		m_jdate.setText(null);
		m_ReasonModel.setSelectedKey(null);
		m_LocationsModel.setSelectedKey(m_App.getInventoryLocation());
		productid = null;
		productref = null;
		productcode = null;
		productname = null;
		m_jreference.setText(null);
		m_jcodebar.setText(null);
		jproduct.setText(null);
		attsetid = null;
		attsetinstid = null;
		attsetinstdesc = null;
		jattributes.setText(null);
		m_junits.setText(null);
		m_jprice.setText(null);
		m_jdate.setEnabled(false);
		m_jbtndate.setEnabled(false);
		m_jreason.setEnabled(false);
		m_jreference.setEnabled(false);
		m_jEnter1.setEnabled(false);
		m_jcodebar.setEnabled(false);
		m_jEnter.setEnabled(false);
		m_jLocation.setEnabled(false);
		jproduct.setEnabled(false);
		jEditProduct.setEnabled(false);
		jattributes.setEnabled(false);
		jEditAttributes.setEnabled(false);
		m_junits.setEnabled(false);
		m_jprice.setEnabled(false);
		m_cat.setComponentEnabled(false);
	}

	public void writeValueInsert() {
		m_sID = UUID.randomUUID().toString();
		m_jdate.setText(Formats.TIMESTAMP.formatValue(DateUtils.getTodayMinutes()));
		m_ReasonModel.setSelectedItem(MovementReason.IN_PURCHASE);
		m_LocationsModel.setSelectedKey(m_App.getInventoryLocation());
		productid = null;
		productref = null;
		productcode = null;
		productname = null;
		m_jreference.setText(null);
		m_jcodebar.setText(null);
		jproduct.setText(null);
		attsetid = null;
		attsetinstid = null;
		attsetinstdesc = null;
		jattributes.setText(null);
		m_jcodebar.setText(null);
		m_junits.setText(null);
		m_jprice.setText(null);
		m_jdate.setEnabled(true);
		m_jbtndate.setEnabled(true);
		m_jreason.setEnabled(true);
		m_jreference.setEnabled(true);
		m_jEnter1.setEnabled(true);
		m_jcodebar.setEnabled(true);
		m_jEnter.setEnabled(true);
		m_jLocation.setEnabled(true);
		jproduct.setEnabled(true);
		jEditProduct.setEnabled(true);
		jattributes.setEnabled(true);
		jEditAttributes.setEnabled(true);
		m_junits.setEnabled(true);
		m_jprice.setEnabled(true);
		m_cat.setComponentEnabled(true);
	}

	public void writeValueDelete(Object value) {
		Object[] diary = (Object[]) value;
		m_sID = (String) diary[0];
		m_jdate.setText(Formats.TIMESTAMP.formatValue(diary[1]));
		m_ReasonModel.setSelectedKey(diary[2]);
		m_LocationsModel.setSelectedKey(diary[3]);
		productid = (String) diary[4];
		productref = (String) diary[8];
		productcode = (String) diary[9];
		productname = (String) diary[10];
		m_jreference.setText(productref);
		m_jcodebar.setText(productcode);
		jproduct.setText(productname);
		attsetid = (String) diary[11];
		attsetinstid = (String) diary[5];
		attsetinstdesc = (String) diary[12];
		jattributes.setText(attsetinstdesc);
		m_junits.setText(Formats.DOUBLE.formatValue(signum((Double) diary[6], (Integer) diary[2])));
		m_jprice.setText(Formats.CURRENCY.formatValue(diary[7]));
		m_jdate.setEnabled(false);
		m_jbtndate.setEnabled(false);
		m_jreason.setEnabled(false);
		m_jreference.setEnabled(false);
		m_jEnter1.setEnabled(false);
		m_jcodebar.setEnabled(false);
		m_jEnter.setEnabled(false);
		m_jLocation.setEnabled(false);
		jproduct.setEnabled(false);
		jEditProduct.setEnabled(false);
		jattributes.setEnabled(false);
		jEditAttributes.setEnabled(false);
		m_junits.setEnabled(false);
		m_jprice.setEnabled(false);
		m_cat.setComponentEnabled(false);
	}

	public void writeValueEdit(Object value) {
		Object[] diary = (Object[]) value;
		m_sID = (String) diary[0];
		m_jdate.setText(Formats.TIMESTAMP.formatValue(diary[1]));
		m_ReasonModel.setSelectedKey(diary[2]);
		m_LocationsModel.setSelectedKey(diary[3]);
		productid = (String) diary[4];
		productref = (String) diary[8];
		productcode = (String) diary[9];
		productname = (String) diary[10];
		m_jreference.setText(productref);
		m_jcodebar.setText(productcode);
		jproduct.setText(productname);
		attsetid = (String) diary[11];
		attsetinstid = (String) diary[5];
		attsetinstdesc = (String) diary[12];
		jattributes.setText(attsetinstdesc);
		m_junits.setText(Formats.DOUBLE.formatValue(signum((Double) diary[6], (Integer) diary[2])));
		m_jprice.setText(Formats.CURRENCY.formatValue(diary[7]));
		m_jdate.setEnabled(false);
		m_jbtndate.setEnabled(false);
		m_jreason.setEnabled(false);
		m_jreference.setEnabled(false);
		m_jEnter1.setEnabled(false);
		m_jcodebar.setEnabled(false);
		m_jEnter.setEnabled(false);
		m_jLocation.setEnabled(false);
		jproduct.setEnabled(true);
		jEditProduct.setEnabled(true);
		jattributes.setEnabled(false);
		jEditAttributes.setEnabled(false);
		m_junits.setEnabled(false);
		m_jprice.setEnabled(false);
		m_cat.setComponentEnabled(false);
	}

	public Object createValue() throws BasicException {
		return new Object[]{m_sID, Formats.TIMESTAMP.parseValue(m_jdate.getText()), m_ReasonModel.getSelectedKey(),
				m_LocationsModel.getSelectedKey(), productid, attsetinstid,
				samesignum((Double) Formats.DOUBLE.parseValue(m_junits.getText()),
						(Integer) m_ReasonModel.getSelectedKey()),
				Formats.CURRENCY.parseValue(m_jprice.getText()), productref, productcode, productname, attsetid,
				attsetinstdesc};
	}

	public Component getComponent() {
		return this;
	}
	// private ProductInfoExt getProduct(String id) {
	// try {
	// return m_dlSales.getProductInfo(id);
	// } catch (BasicException e) {
	// return null;
	// }
	// }

	private Double signum(Double d, Integer i) {
		if (d == null || i == null) {
			return d;
		} else if (i.intValue() < 0) {
			return new Double(-d.doubleValue());
		} else {
			return d;
		}
	}

	private Double samesignum(Double d, Integer i) {

		if (d == null || i == null) {
			return d;
		} else if ((i.intValue() > 0 && d.doubleValue() < 0.0) || (i.intValue() < 0 && d.doubleValue() > 0.0)) {
			return new Double(-d.doubleValue());
		} else {
			return d;
		}
	}

	private void assignProduct(ProductInfoExt prod) {

		if (jproduct.isEnabled()) {
			if (prod == null) {
				productid = null;
				productref = null;
				productcode = null;
				productname = null;
				attsetid = null;
				attsetinstid = null;
				attsetinstdesc = null;
				jproduct.setText(null);
				m_jcodebar.setText(null);
				m_jreference.setText(null);
				jattributes.setText(null);
			} else {
				productid = prod.getID();
				productref = prod.getReference();
				productcode = prod.getCode();
				productname = prod.toString();
				attsetid = null;
				attsetinstid = null;
				attsetinstdesc = null;
				jproduct.setText(productname);
				m_jcodebar.setText(productcode);
				m_jreference.setText(productref);
				jattributes.setText(null);

				// calculo el precio sugerido para la entrada.
				MovementReason reason = (MovementReason) m_ReasonModel.getSelectedItem();
				Double dPrice = reason.getPrice(prod.getPriceBuy(), prod.getPriceSell());
				m_jprice.setText(Formats.CURRENCY.formatValue(dPrice));
			}
		}
	}

	private void assignProductByCode() {
		try {
			ProductInfoExt oProduct = m_dlSales.getProductInfoByCode(m_jcodebar.getText());
			if (oProduct == null) {
				assignProduct(null);
				Toolkit.getDefaultToolkit().beep();
			} else {
				// Se anade directamente una unidad con el precio y todo
				assignProduct(oProduct);
			}
		} catch (BasicException eData) {
			assignProduct(null);
			MessageInf msg = new MessageInf(eData);
			msg.show(this);
		}
	}

	private void assignProductByReference() {
		try {
			ProductInfoExt oProduct = m_dlSales.getProductInfoByReference(m_jreference.getText());
			if (oProduct == null) {
				assignProduct(null);
				Toolkit.getDefaultToolkit().beep();
			} else {
				// Se anade directamente una unidad con el precio y todo
				assignProduct(oProduct);
			}
		} catch (BasicException eData) {
			assignProduct(null);
			MessageInf msg = new MessageInf(eData);
			msg.show(this);
		}
	}

	private class CatalogListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			assignProduct((ProductInfoExt) e.getSource());
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

		jPanel1 = new javax.swing.JPanel();
		jLabel1 = new javax.swing.JLabel();
		m_jdate = new javax.swing.JTextField();
		jLabel2 = new javax.swing.JLabel();
		jLabel3 = new javax.swing.JLabel();
		jattributes = new javax.swing.JTextField();
		m_jreason = new javax.swing.JComboBox();
		jEditAttributes = new javax.swing.JButton();
		m_jbtndate = new javax.swing.JButton();
		jLabel4 = new javax.swing.JLabel();
		jLabel5 = new javax.swing.JLabel();
		m_junits = new javax.swing.JTextField();
		m_jprice = new javax.swing.JTextField();
		m_jcodebar = new javax.swing.JTextField();
		m_jEnter = new javax.swing.JButton();
		m_jreference = new javax.swing.JTextField();
		m_jEnter1 = new javax.swing.JButton();
		jLabel6 = new javax.swing.JLabel();
		jLabel7 = new javax.swing.JLabel();
		m_jLocation = new javax.swing.JComboBox();
		jLabel8 = new javax.swing.JLabel();
		jproduct = new javax.swing.JTextField();
		jEditProduct = new javax.swing.JButton();
		jLabel9 = new javax.swing.JLabel();

		setLayout(new java.awt.BorderLayout());

		jLabel1.setText(AppLocal.getIntString("label.stockdate")); // NOI18N

		jLabel2.setText(AppLocal.getIntString("label.stockreason")); // NOI18N

		jLabel3.setText(AppLocal.getIntString("label.stockproduct")); // NOI18N

		jattributes.setEditable(false);

		jEditAttributes
				.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/colorize16.png"))); // NOI18N
		jEditAttributes.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jEditAttributesActionPerformed(evt);
			}
		});

		m_jbtndate.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/date.png"))); // NOI18N
		m_jbtndate.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jbtndateActionPerformed(evt);
			}
		});

		jLabel4.setText(AppLocal.getIntString("label.units")); // NOI18N

		jLabel5.setText(AppLocal.getIntString("label.price")); // NOI18N

		m_junits.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

		m_jprice.setHorizontalAlignment(javax.swing.JTextField.RIGHT);

		m_jcodebar.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jcodebarActionPerformed(evt);
			}
		});

		m_jEnter.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/apply.png"))); // NOI18N
		m_jEnter.setFocusPainted(false);
		m_jEnter.setFocusable(false);
		m_jEnter.setRequestFocusEnabled(false);
		m_jEnter.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jEnterActionPerformed(evt);
			}
		});

		m_jreference.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jreferenceActionPerformed(evt);
			}
		});

		m_jEnter1.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/apply.png"))); // NOI18N
		m_jEnter1.setFocusPainted(false);
		m_jEnter1.setFocusable(false);
		m_jEnter1.setRequestFocusEnabled(false);
		m_jEnter1.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jEnter1ActionPerformed(evt);
			}
		});

		jLabel6.setText(AppLocal.getIntString("label.prodref")); // NOI18N

		jLabel7.setText(AppLocal.getIntString("label.prodbarcode")); // NOI18N

		jLabel8.setText(AppLocal.getIntString("label.warehouse")); // NOI18N

		jproduct.setEditable(false);

		jEditProduct.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/search.png"))); // NOI18N
		jEditProduct.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				jEditProductActionPerformed(evt);
			}
		});

		jLabel9.setText(AppLocal.getIntString("label.attributes")); // NOI18N

		add(jPanel1, java.awt.BorderLayout.CENTER);
		layoutForm();
	}// </editor-fold>//GEN-END:initComponents

	private void layoutForm() {
		jPanel1.removeAll();
		jPanel1.setLayout(new GridBagLayout());
		GridBagConstraints label = new GridBagConstraints();
		label.anchor = GridBagConstraints.LINE_START;
		label.insets = new Insets(4, 8, 4, 8);
		GridBagConstraints field = new GridBagConstraints();
		field.fill = GridBagConstraints.HORIZONTAL;
		field.weightx = 1.0;
		field.insets = new Insets(4, 0, 4, 8);
		GridBagConstraints button = new GridBagConstraints();
		button.insets = new Insets(4, 0, 4, 8);

		addFormRow(0, jLabel1, m_jdate, m_jbtndate, label, field, button);
		addFormRow(1, jLabel2, m_jreason, null, label, field, button);
		addFormRow(2, jLabel8, m_jLocation, null, label, field, button);
		addFormRow(3, jLabel6, m_jreference, m_jEnter1, label, field, button);
		addFormRow(4, jLabel7, m_jcodebar, m_jEnter, label, field, button);
		addFormRow(5, jLabel3, jproduct, jEditProduct, label, field, button);
		addFormRow(6, jLabel9, jattributes, jEditAttributes, label, field, button);
		addFormRow(7, jLabel4, m_junits, null, label, field, button);
		addFormRow(8, jLabel5, m_jprice, null, label, field, button);
		jPanel1.revalidate();
		jPanel1.repaint();
	}

	private void addFormRow(int row, Component labelComponent, Component fieldComponent, Component buttonComponent,
			GridBagConstraints label, GridBagConstraints field, GridBagConstraints button) {
		label.gridx = 0;
		label.gridy = row;
		label.weightx = 0;
		jPanel1.add(labelComponent, label);
		field.gridx = 1;
		field.gridy = row;
		field.gridwidth = buttonComponent == null ? 2 : 1;
		jPanel1.add(fieldComponent, field);
		if (buttonComponent != null) {
			button.gridx = 2;
			button.gridy = row;
			jPanel1.add(buttonComponent, button);
		}
	}

	private void m_jEnter1ActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jEnter1ActionPerformed

		assignProductByReference();

	}// GEN-LAST:event_m_jEnter1ActionPerformed

	private void m_jreferenceActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jreferenceActionPerformed

		assignProductByReference();

	}// GEN-LAST:event_m_jreferenceActionPerformed

	private void m_jcodebarActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jcodebarActionPerformed

		assignProductByCode();

	}// GEN-LAST:event_m_jcodebarActionPerformed

	private void m_jEnterActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jEnterActionPerformed

		assignProductByCode();

	}// GEN-LAST:event_m_jEnterActionPerformed

	private void jEditAttributesActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jEditAttributesActionPerformed
		new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.productattributesineditor")).show(this);
	}// GEN-LAST:event_jEditAttributesActionPerformed

	private void m_jbtndateActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jbtndateActionPerformed

		Date date;
		try {
			date = (Date) Formats.TIMESTAMP.parseValue(m_jdate.getText());
		} catch (BasicException e) {
			date = null;
		}
		date = JCalendarDialog.showCalendarTime(this, date);
		if (date != null) {
			m_jdate.setText(Formats.TIMESTAMP.formatValue(date));
		}

	}// GEN-LAST:event_m_jbtndateActionPerformed

	private void jEditProductActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_jEditProductActionPerformed

		assignProduct(JProductFinder.showMessage(this, m_dlSales));

	}// GEN-LAST:event_jEditProductActionPerformed

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton jEditAttributes;
	private javax.swing.JButton jEditProduct;
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel3;
	private javax.swing.JLabel jLabel4;
	private javax.swing.JLabel jLabel5;
	private javax.swing.JLabel jLabel6;
	private javax.swing.JLabel jLabel7;
	private javax.swing.JLabel jLabel8;
	private javax.swing.JLabel jLabel9;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JTextField jattributes;
	private javax.swing.JTextField jproduct;
	private javax.swing.JButton m_jEnter;
	private javax.swing.JButton m_jEnter1;
	private javax.swing.JComboBox m_jLocation;
	private javax.swing.JButton m_jbtndate;
	private javax.swing.JTextField m_jcodebar;
	private javax.swing.JTextField m_jdate;
	private javax.swing.JTextField m_jprice;
	private javax.swing.JComboBox m_jreason;
	private javax.swing.JTextField m_jreference;
	private javax.swing.JTextField m_junits;
	// End of variables declaration//GEN-END:variables

}
