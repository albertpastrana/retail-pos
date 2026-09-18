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

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.user.EditorListener;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.data.user.ListProviderCreator;
import com.openbravo.data.user.SaveProvider;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.panels.JPanelTable2;
import com.openbravo.pos.ticket.ProductFilter;

/**
 *
 * @author adrianromero Created on 1 de marzo de 2007, 22:15
 *
 */
public class ProductsPanel extends JPanelTable2 implements EditorListener {

	private ProductsEditor jeditor;
	private ProductFilter jproductfilter;

	private DataLogicSales m_dlSales = null;

	/** Creates a new instance of ProductsPanel2 */
	public ProductsPanel() {
	}

	protected void init() {
		m_dlSales = app.getBean(DataLogicSales.class);

		// el panel del filtro
		jproductfilter = new ProductFilter();
		jproductfilter.init(app);
		jproductfilter.addActionListener(new ReloadActionListener());

		row = m_dlSales.getProductsRow();

		lpr = new ListProviderCreator(m_dlSales.getProductCatQBF(), jproductfilter);

		spr = new SaveProvider(m_dlSales.getProductCatUpdate(), m_dlSales.getProductCatInsert(),
				m_dlSales.getProductCatDelete());

		// el panel del editor
		jeditor = new ProductsEditor(app, m_dlSales, dirty);
	}

	public EditorRecord getEditor() {
		return jeditor;
	}

	@Override
	public Component getFilter() {
		return jproductfilter.getComponent();
	}

	public String getTitle() {
		return AppLocal.getIntString("Menu.Products");
	}

	@Override
	public void activate() throws BasicException {

		jeditor.activate();
		jproductfilter.activate();

		super.activate();
		jeditor.setBrowsableData(bd);
	}

	public void updateValue(Object value) {
	}

	private class ReloadActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			try {
				bd.actionLoad();
			} catch (BasicException eD) {
				MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.cannotloadlists"),
						eD);
				msg.show(ProductsPanel.this);
				return;
			}
			try {
				offerCatalogImport();
			} catch (BasicException eD) {
				new MessageInf(eD).show(ProductsPanel.this);
			}
		}
	}

	private void offerCatalogImport() throws BasicException {
		final String barcode = jproductfilter.getBarcode();
		if (barcode.isEmpty() || m_dlSales.getProductInfoByCode(barcode) != null) {
			return;
		}
		if (CatalogImportDialog.forStock(this, app, m_dlSales).importIfAbsent(barcode) != null) {
			bd.actionLoad();
		}
	}
}
