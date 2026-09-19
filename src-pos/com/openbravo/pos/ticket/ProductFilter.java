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

package com.openbravo.pos.ticket;

import com.openbravo.data.loader.SerializerWrite;
import com.openbravo.pos.forms.AppLocal;

import com.openbravo.pos.forms.AppView;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;
import java.util.List;
import javax.swing.event.EventListenerList;
import com.openbravo.data.loader.QBFCompareEnum;
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.ListQBFModelNumber;
import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.loader.Datas;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.data.loader.SerializerWriteBasic;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.reports.ReportEditorCreator;

public class ProductFilter extends javax.swing.JPanel implements ReportEditorCreator {

	private SentenceList m_sentcat;
	private ComboBoxValModel m_CategoryModel;
	private SentenceList m_sentbrand;
	private ComboBoxValModel m_BrandModel;

	protected EventListenerList listeners = new EventListenerList();

	/** Creates new form JQBFProduct */
	public ProductFilter() {

		initComponents();

		ActionListener apply = new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fireFilterApplied();
			}
		};
		m_jBarcode.addActionListener(apply);
		m_jReference.addActionListener(apply);
		m_jName.addActionListener(apply);
	}

	public void addActionListener(ActionListener l) {
		listeners.add(ActionListener.class, l);
	}

	public void removeActionListener(ActionListener l) {
		listeners.remove(ActionListener.class, l);
	}

	protected void fireFilterApplied() {
		EventListener[] l = listeners.getListeners(ActionListener.class);
		ActionEvent e = null;
		for (int i = 0; i < l.length; i++) {
			if (e == null) {
				e = new ActionEvent(this, ActionEvent.ACTION_PERFORMED, "APPLY");
			}
			((ActionListener) l[i]).actionPerformed(e);
		}
	}

	public void init(AppView app) {

		DataLogicSales dlSales = app.getBean(DataLogicSales.class);

		// El modelo de categorias
		m_sentcat = dlSales.getCategoriesList();
		m_CategoryModel = new ComboBoxValModel();
		m_sentbrand = dlSales.getBrandsList();
		m_BrandModel = new ComboBoxValModel();

		m_jCboReference.setModel(ListQBFModelNumber.getMandatoryString());
		m_jCboName.setModel(ListQBFModelNumber.getMandatoryString());
	}

	public void activate() throws BasicException {

		List catlist = m_sentcat.list();
		catlist.add(0, null);
		m_CategoryModel = new ComboBoxValModel(catlist);
		m_jCategory.setModel(m_CategoryModel);

		List brandlist = m_sentbrand.list();
		brandlist.add(0, null);
		m_BrandModel = new ComboBoxValModel(brandlist);
		m_jBrand.setModel(m_BrandModel);
		reset();
	}

	public void reset() {
		m_jBarcode.setText(null);
		m_jReference.setText(null);
		m_jName.setText(null);
		m_jCategory.setSelectedItem(null);
		m_jBrand.setSelectedItem(null);
	}

	public SerializerWrite getSerializerWrite() {
		return new SerializerWriteBasic(new Datas[]{Datas.OBJECT, Datas.STRING, Datas.OBJECT, Datas.DOUBLE,
				Datas.OBJECT, Datas.DOUBLE, Datas.OBJECT, Datas.STRING, Datas.OBJECT, Datas.STRING, Datas.OBJECT,
				Datas.STRING, Datas.OBJECT, Datas.STRING});
	}

	public Component getComponent() {
		return this;
	}

	public String getBarcode() {
		String barcode = m_jBarcode.getText();
		return barcode == null ? "" : barcode.trim();
	}

	public Object createValue() throws BasicException {

		if (getBarcode().equals("")) {
			String sName = m_jName.getText();
			Object nameCompare = (sName == null || sName.equals(""))
					? QBFCompareEnum.COMP_NONE
					: m_jCboName.getSelectedItem();
			String sReference = m_jReference.getText();
			Object referenceCompare = (sReference == null || sReference.equals(""))
					? QBFCompareEnum.COMP_NONE
					: m_jCboReference.getSelectedItem();
			// Filtro por formulario
			return new Object[]{nameCompare, sName, QBFCompareEnum.COMP_NONE, null, QBFCompareEnum.COMP_NONE, null,
					m_CategoryModel.getSelectedKey() == null ? QBFCompareEnum.COMP_NONE : 0,
					m_CategoryModel.getSelectedKey(), QBFCompareEnum.COMP_NONE, null,
					m_BrandModel.getSelectedItem() == null ? QBFCompareEnum.COMP_NONE : QBFCompareEnum.COMP_EQUALS,
					m_BrandModel.getSelectedItem(), referenceCompare, sReference};
		} else {
			// Filtro por codigo de barras.
			return new Object[]{QBFCompareEnum.COMP_NONE, null, QBFCompareEnum.COMP_NONE, null,
					QBFCompareEnum.COMP_NONE, null, QBFCompareEnum.COMP_NONE, null, QBFCompareEnum.COMP_BLOOKUP,
					getBarcode(), QBFCompareEnum.COMP_NONE, null, QBFCompareEnum.COMP_NONE, null};
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
		jPanel2 = new javax.swing.JPanel(new GridBagLayout());
		jPanel1 = new javax.swing.JPanel(new GridBagLayout());
		jLabel5 = new javax.swing.JLabel(AppLocal.getIntString("label.prodbarcode"));
		jLabel1 = new javax.swing.JLabel(AppLocal.getIntString("label.prodcategory"));
		jLabel2 = new javax.swing.JLabel(AppLocal.getIntString("label.prodname"));
		jLabel6 = new javax.swing.JLabel(AppLocal.getIntString("label.prodbrand"));
		jLabel7 = new javax.swing.JLabel(AppLocal.getIntString("label.prodref"));
		m_jBarcode = new javax.swing.JTextField();
		m_jCboReference = new javax.swing.JComboBox();
		m_jReference = new javax.swing.JTextField();
		m_jCboName = new javax.swing.JComboBox();
		m_jName = new javax.swing.JTextField();
		m_jCategory = new javax.swing.JComboBox();
		m_jBrand = new javax.swing.JComboBox();

		jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder(AppLocal.getIntString("label.bybarcode")));
		jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(AppLocal.getIntString("label.productfilters")));
		m_jBarcode.setPreferredSize(new java.awt.Dimension(260, 28));
		m_jCboReference.setPreferredSize(new java.awt.Dimension(150, 28));
		m_jCboName.setPreferredSize(new java.awt.Dimension(150, 28));
		m_jCategory.setPreferredSize(new java.awt.Dimension(280, 28));
		m_jBrand.setPreferredSize(new java.awt.Dimension(280, 28));

		GridBagConstraints barcodeLabel = new GridBagConstraints();
		barcodeLabel.gridx = 0;
		barcodeLabel.gridy = 0;
		barcodeLabel.anchor = GridBagConstraints.WEST;
		barcodeLabel.insets = new Insets(6, 8, 6, 8);
		jPanel2.add(jLabel5, barcodeLabel);
		GridBagConstraints barcodeField = new GridBagConstraints();
		barcodeField.gridx = 1;
		barcodeField.gridy = 0;
		barcodeField.weightx = 1.0;
		barcodeField.fill = GridBagConstraints.HORIZONTAL;
		barcodeField.insets = new Insets(6, 0, 6, 8);
		jPanel2.add(m_jBarcode, barcodeField);

		addFormRow(jPanel1, jLabel7, m_jCboReference, m_jReference, 0);
		addFormRow(jPanel1, jLabel2, m_jCboName, m_jName, 1);
		addFormRow(jPanel1, jLabel1, m_jCategory, null, 2);
		addFormRow(jPanel1, jLabel6, m_jBrand, null, 3);

		setLayout(new BorderLayout(0, 6));
		add(jPanel2, BorderLayout.NORTH);
		add(jPanel1, BorderLayout.CENTER);
	}// </editor-fold>//GEN-END:initComponents

	private void addFormRow(javax.swing.JPanel panel, javax.swing.JLabel label, javax.swing.JComboBox selector,
			javax.swing.JTextField field, int row) {
		GridBagConstraints labelConstraints = new GridBagConstraints();
		labelConstraints.gridx = 0;
		labelConstraints.gridy = row;
		labelConstraints.anchor = GridBagConstraints.WEST;
		labelConstraints.insets = new Insets(4, 8, 4, 8);
		panel.add(label, labelConstraints);

		GridBagConstraints selectorConstraints = new GridBagConstraints();
		selectorConstraints.gridx = 1;
		selectorConstraints.gridy = row;
		selectorConstraints.fill = GridBagConstraints.HORIZONTAL;
		selectorConstraints.weightx = field == null ? 1.0 : 0.0;
		selectorConstraints.gridwidth = field == null ? 2 : 1;
		selectorConstraints.insets = new Insets(4, 0, 4, 8);
		panel.add(selector, selectorConstraints);

		if (field != null) {
			GridBagConstraints fieldConstraints = new GridBagConstraints();
			fieldConstraints.gridx = 2;
			fieldConstraints.gridy = row;
			fieldConstraints.weightx = 1.0;
			fieldConstraints.fill = GridBagConstraints.HORIZONTAL;
			fieldConstraints.insets = new Insets(4, 0, 4, 8);
			panel.add(field, fieldConstraints);
		}
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel5;
	private javax.swing.JLabel jLabel6;
	private javax.swing.JLabel jLabel7;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JTextField m_jBarcode;
	private javax.swing.JComboBox m_jBrand;
	private javax.swing.JComboBox m_jCategory;
	private javax.swing.JComboBox m_jCboName;
	private javax.swing.JTextField m_jName;
	private javax.swing.JComboBox m_jCboReference;
	private javax.swing.JTextField m_jReference;
	// End of variables declaration//GEN-END:variables

}
