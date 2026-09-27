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
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.user.EditorListener;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.data.user.ListProviderCreator;
import com.openbravo.data.user.SaveProvider;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.panels.JPanelTable2;
import com.openbravo.pos.sales.TaxesLogic;
import com.openbravo.pos.ticket.ProductFilter;
import com.openbravo.data.user.BrowsableEditableData;

/**
 *
 * @author adrianromero Created on 1 de marzo de 2007, 22:15
 *
 */
public class ProductsPanel extends JPanelTable2 implements EditorListener {

	private ProductsEditor jeditor;
	private ProductFilter jproductfilter;

	private DataLogicSales m_dlSales = null;
	private TaxesLogic taxesLogic;
	private ProductTableNavigator batchNavigator;
	private boolean showingBatchEditor;

	/** Creates a new instance of ProductsPanel2 */
	public ProductsPanel() {
	}

	protected void init() {
		m_dlSales = app.getBean(DataLogicSales.class);
		try {
			taxesLogic = new TaxesLogic(m_dlSales.getTaxList().list());
		} catch (BasicException e) {
			throw new IllegalStateException("Cannot load product taxes", e);
		}
		row = m_dlSales.getProductsRow();

		// el panel del filtro
		jproductfilter = new ProductFilter();
		jproductfilter.init(app);
		jproductfilter.addActionListener(new ReloadActionListener());

		lpr = new ListProviderCreator(m_dlSales.getProductCatQBF(), jproductfilter);

		spr = new SaveProvider(m_dlSales.getProductCatUpdate(), m_dlSales.getProductCatInsert(),
				m_dlSales.getProductCatDelete());

		// el panel del editor
		jeditor = new ProductsEditor(app, m_dlSales, dirty);
		jeditor.setDeleteAction(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				confirmDelete();
			}
		});
	}

	public EditorRecord getEditor() {
		return jeditor;
	}

	@Override
	public Component getFilter() {
		JPanel header = new JPanel(new BorderLayout(0, 6));
		JPanel actionBar = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
		JButton newProduct = new JButton(AppLocal.getIntString("batch.newProduct"));
		newProduct.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (batchNavigator != null && batchNavigator.hasPending()) {
					JOptionPane.showMessageDialog(ProductsPanel.this, AppLocal.getIntString("batch.saveBeforeFilter"));
					return;
				}
				try {
					bd.actionInsert();
					if (batchNavigator != null)
						batchNavigator.clearSelection();
				} catch (BasicException ex) {
					new MessageInf(ex).show(ProductsPanel.this);
				}
			}
		});
		// La pantalla ja mostra "Productes" a la capçalera de l'aplicació
		// (JPrincipalApp usa getTitle()); no cal repetir-ho aqui.
		actionBar.add(newProduct);
		header.add(actionBar, BorderLayout.NORTH);
		header.add(jproductfilter.getComponent(), BorderLayout.CENTER);
		return header;
	}

	@Override
	protected boolean showToolbar() {
		return false;
	}

	@Override
	protected double getSplitResizeWeight(boolean editorKeepsSize) {
		return 0.5;
	}

	@Override
	protected double getSplitDividerLocation() {
		return 0.5;
	}

	@Override
	protected Component getListComponent(BrowsableEditableData data) {
		try {
			batchNavigator = new ProductTableNavigator(data, taxesLogic, m_dlSales, this);
			return batchNavigator;
		} catch (BasicException e) {
			throw new IllegalStateException("Cannot load batch categories", e);
		}
	}

	com.openbravo.data.loader.Session getSession() {
		return app.getSession();
	}

	boolean prepareBatchEdit() throws BasicException {
		if (!dirty.isDirty())
			return true;
		if (bd.getState() != BrowsableEditableData.ST_UPDATE || bd.getIndex() < 0) {
			return bd.actionClosingForm(this);
		}
		Object[] original = (Object[]) bd.getListModel().getElementAt(bd.getIndex());
		ProductBatchDraft draft = jeditor.captureBatchDraft(original);
		ProductBatchDraft.OtherFields decision = ProductBatchDraft.OtherFields.DISCARD;
		if (draft.otherChanged) {
			Object[] options = {AppLocal.getIntString("batch.saveOther"), AppLocal.getIntString("batch.discardOther"),
					AppLocal.getIntString("batch.cancelTransition")};
			int choice = JOptionPane.showOptionDialog(this, AppLocal.getIntString("batch.otherChanges"),
					AppLocal.getIntString("batch.pendingTitle"), JOptionPane.DEFAULT_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, options, options[2]);
			decision = choice == 0
					? ProductBatchDraft.OtherFields.SAVE
					: choice == 1 ? ProductBatchDraft.OtherFields.DISCARD : ProductBatchDraft.OtherFields.CANCEL;
		}
		return draft.enter(decision, () -> {
			jeditor.restoreBatchFields(original);
			try {
				jeditor.saveOtherBatchFields(bd);
			} catch (BasicException ex) {
				jeditor.restoreDraftFields(draft);
				throw ex;
			}
		}, () -> bd.refreshCurrent(), batchNavigator::stageEditorDraft);
	}

	boolean hasDirtyIndividualEditor() {
		return dirty.isDirty();
	}

	void refreshBatchEditor() {
		if (bd != null)
			bd.refreshCurrent();
	}

	void batchStateChanged(boolean expanded) {
		SwingUtilities.invokeLater(() -> {
			if (batchNavigator == null || !(batchNavigator.getParent() instanceof JSplitPane))
				return;
			JSplitPane split = (JSplitPane) batchNavigator.getParent();
			if (expanded == showingBatchEditor)
				return;
			int divider = split.getDividerLocation();
			split.setRightComponent(expanded ? batchNavigator.getBatchEditor() : jeditor);
			split.setDividerLocation(divider);
			showingBatchEditor = expanded;
		});
	}

	@Override
	public boolean deactivate() {
		if (batchNavigator != null && batchNavigator.hasPending()) {
			int answer = JOptionPane.showConfirmDialog(this, AppLocal.getIntString("batch.leave"),
					AppLocal.getIntString("batch.pendingTitle"), JOptionPane.YES_NO_OPTION);
			if (answer != JOptionPane.YES_OPTION)
				return false;
			batchNavigator.discardPending();
		}
		return super.deactivate();
	}

	public String getTitle() {
		return AppLocal.getIntString("Menu.Products");
	}

	@Override
	public void activate() throws BasicException {

		jeditor.activate();
		jproductfilter.activate();

		super.activate();
		batchNavigator.reloadChoices();
		jeditor.setBrowsableData(bd);
	}

	/** Open the existing product editor on a fresh record after navigating here. */
	public void createNewProduct() throws BasicException {
		if (batchNavigator != null && batchNavigator.hasPending()) {
			JOptionPane.showMessageDialog(this, AppLocal.getIntString("batch.saveBeforeFilter"));
			return;
		}
		bd.actionInsert();
		if (batchNavigator != null)
			batchNavigator.clearSelection();
	}

	/** Reuse the same filter and list reload as a barcode typed in this screen. */
	public void showProduct(String barcode) {
		jproductfilter.searchBarcode(barcode);
	}

	public void updateValue(Object value) {
	}

	private class ReloadActionListener implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			if (bd == null) {
				return;
			}
			if (batchNavigator != null && batchNavigator.hasPending()) {
				JOptionPane.showMessageDialog(ProductsPanel.this, AppLocal.getIntString("batch.saveBeforeFilter"));
				return;
			}
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

	private void confirmDelete() {
		String name = jeditor.getProductName();
		Object[] options = {"Eliminar", "Cancel·la"};
		int answer = JOptionPane.showOptionDialog(this,
				"Eliminar «" + name + "» permanentment? Aquesta acció no es pot desfer.", "Eliminar producte",
				JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[1]);
		if (answer == 0) {
			try {
				bd.actionDelete();
			} catch (BasicException e) {
				new MessageInf(e).show(this);
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
