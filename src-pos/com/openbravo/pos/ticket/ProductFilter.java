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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.EventListener;
import java.util.List;
import javax.swing.event.EventListenerList;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
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
	private javax.swing.Timer filterTimer;

	protected EventListenerList listeners = new EventListenerList();

	/** Creates new form JQBFProduct */
	public ProductFilter() {

		initComponents();

		filterTimer = new javax.swing.Timer(300, new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				fireFilterApplied();
			}
		});
		filterTimer.setRepeats(false);
		DocumentListener textChanged = new DocumentListener() {
			public void insertUpdate(DocumentEvent e) { scheduleFilter(); }
			public void removeUpdate(DocumentEvent e) { scheduleFilter(); }
			public void changedUpdate(DocumentEvent e) { scheduleFilter(); }
		};
		m_jBarcode.getDocument().addDocumentListener(textChanged);
		m_jReference.getDocument().addDocumentListener(textChanged);
		m_jName.getDocument().addDocumentListener(textChanged);
		m_jFamily.getDocument().addDocumentListener(textChanged);
		m_jCategory.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) { fireFilterApplied(); }
		});
		m_jBrand.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) { fireFilterApplied(); }
		});
	}

	private void scheduleFilter() {
		if (filterTimer != null) {
			filterTimer.restart();
		}
	}

	private QBFCompareEnum qbfComparator(Object value) {
		return value instanceof QBFCompareEnum ? (QBFCompareEnum) value : QBFCompareEnum.COMP_NONE;
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
		m_jFamily.setText(null);
		m_jCategory.setSelectedItem(null);
		m_jBrand.setSelectedItem(null);
	}

	public SerializerWrite getSerializerWrite() {
		return new SerializerWriteBasic(new Datas[]{Datas.OBJECT, Datas.STRING, Datas.OBJECT, Datas.DOUBLE,
				Datas.OBJECT, Datas.DOUBLE, Datas.OBJECT, Datas.STRING, Datas.OBJECT, Datas.STRING, Datas.OBJECT,
				Datas.STRING, Datas.OBJECT, Datas.STRING, Datas.OBJECT, Datas.STRING});
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
					: qbfComparator(m_jCboName.getSelectedItem());
			String sReference = m_jReference.getText();
			Object referenceCompare = (sReference == null || sReference.equals(""))
					? QBFCompareEnum.COMP_NONE
					: qbfComparator(m_jCboReference.getSelectedItem());
			String sFamily = m_jFamily.getText();
			Object familyCompare = (sFamily == null || sFamily.equals(""))
					? QBFCompareEnum.COMP_NONE : QBFCompareEnum.COMP_CONTAINS;
			// Filtro por formulario
			return new Object[]{nameCompare, sName, QBFCompareEnum.COMP_NONE, null, QBFCompareEnum.COMP_NONE, null,
					m_CategoryModel.getSelectedKey() == null ? QBFCompareEnum.COMP_NONE : QBFCompareEnum.COMP_EQUALS,
					m_CategoryModel.getSelectedKey(), QBFCompareEnum.COMP_NONE, null,
					m_BrandModel.getSelectedItem() == null ? QBFCompareEnum.COMP_NONE : QBFCompareEnum.COMP_EQUALS,
					m_BrandModel.getSelectedItem(), referenceCompare, sReference, familyCompare, sFamily};
		} else {
			// Filtro por codigo de barras.
			return new Object[]{QBFCompareEnum.COMP_NONE, null, QBFCompareEnum.COMP_NONE, null,
					QBFCompareEnum.COMP_NONE, null, QBFCompareEnum.COMP_NONE, null, QBFCompareEnum.COMP_BLOOKUP,
				getBarcode(), QBFCompareEnum.COMP_NONE, null, QBFCompareEnum.COMP_NONE, null,
				QBFCompareEnum.COMP_NONE, null};
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
		jPanel1 = new javax.swing.JPanel(new java.awt.GridLayout(2, 3, 8, 8));
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
		m_jFamily = new javax.swing.JTextField();

		jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder(AppLocal.getIntString("label.productfilters")));
		jPanel1.add(buildComboFieldCell(jLabel2, m_jCboName, m_jName));
		jPanel1.add(buildCell(jLabel1, m_jCategory));
		jPanel1.add(buildCell(jLabel6, m_jBrand));
		jPanel1.add(buildComboFieldCell(jLabel7, m_jCboReference, m_jReference));
		jPanel1.add(buildCell(jLabel5, m_jBarcode));
		jPanel1.add(buildCell(new javax.swing.JLabel("Família"), m_jFamily));

		setLayout(new BorderLayout(0, 6));
		add(jPanel1, BorderLayout.CENTER);
	}// </editor-fold>//GEN-END:initComponents

	private javax.swing.JPanel buildCell(javax.swing.JLabel label, javax.swing.JComponent field) {
		javax.swing.JPanel cellPanel = new javax.swing.JPanel(new BorderLayout(0, 2));
		cellPanel.setOpaque(false);
		cellPanel.add(label, BorderLayout.NORTH);
		cellPanel.add(field, BorderLayout.CENTER);
		return cellPanel;
	}

	private javax.swing.JPanel buildComboFieldCell(javax.swing.JLabel label, javax.swing.JComboBox combo,
			javax.swing.JTextField field) {
		javax.swing.JPanel row = new javax.swing.JPanel(new BorderLayout(4, 0));
		row.setOpaque(false);
		row.add(combo, BorderLayout.WEST);
		row.add(field, BorderLayout.CENTER);
		return buildCell(label, row);
	}

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JLabel jLabel1;
	private javax.swing.JLabel jLabel2;
	private javax.swing.JLabel jLabel5;
	private javax.swing.JLabel jLabel6;
	private javax.swing.JLabel jLabel7;
	private javax.swing.JPanel jPanel1;
	private javax.swing.JTextField m_jBarcode;
	private javax.swing.JComboBox m_jBrand;
	private javax.swing.JComboBox m_jCategory;
	private javax.swing.JComboBox m_jCboName;
	private javax.swing.JTextField m_jName;
	private javax.swing.JComboBox m_jCboReference;
	private javax.swing.JTextField m_jReference;
	private javax.swing.JTextField m_jFamily;
	// End of variables declaration//GEN-END:variables

}
