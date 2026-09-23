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
import javax.swing.*;
import java.awt.image.*;
import java.awt.*;
import java.awt.event.*;

import com.openbravo.pos.forms.*;
import com.openbravo.data.loader.*;
import java.sql.*;

import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.format.Formats;
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.data.loader.LocalRes;
import com.openbravo.data.loader.SentenceFind;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.data.user.BrowsableEditableData;
import com.openbravo.data.user.DirtyListener;
import com.openbravo.data.user.DirtyManager;
import com.openbravo.data.user.EditorRecord;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.sales.TaxesLogic;
import com.openbravo.pos.theme.RetailPOSColors;
import java.util.Date;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;

/**
 *
 * @author adrianromero
 */
public class ProductsEditor extends JPanel implements EditorRecord {
	private static final java.util.logging.Logger LOGGER = java.util.logging.Logger
			.getLogger(ProductsEditor.class.getName());
	private SentenceList m_sentcat;
	private ComboBoxValModel m_CategoryModel;

	private SentenceList taxcatsent;
	private ComboBoxValModel taxcatmodel;

	private SentenceList attsent;
	private ComboBoxValModel attmodel;

	private SentenceList taxsent;
	private TaxesLogic taxeslogic;

	private SentenceFind loadimage;

	private AppView m_App;
	private DataLogicSales m_dlSales;
	private DirtyManager m_dirty;
	private BrowsableEditableData m_bd;

	private Object m_id;
	private Object pricesell;
	private boolean priceselllock = false;
	private PriceRuleService priceRuleService;
	private TaxRegime priceTaxRegime = TaxRegime.EQUIVALENCE_SURCHARGE;

	private boolean reportlock = false;
	private double m_pendingFactory = 0.0;
	private double m_pendingWholesale = 0.0;
	private Object[] m_purchaseCost;

	public Session session = null;

	/** Creates new form JEditProduct */
	public ProductsEditor(AppView app, DataLogicSales dlSales, DirtyManager dirty) {

		m_App = app;
		m_dlSales = dlSales;
		m_dirty = dirty;

		initComponents();

		try {
			Method method = dlSales.getClass().getDeclaredMethod("getSession");
			method.setAccessible(true);
			session = Session.class.cast(method.invoke(dlSales));
		} catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
			// field will always exist, and we override accessibility
			// if getting session fails, we handle the null later
		}
		if (session != null) {
			priceRuleService = new PriceRuleService(session);
		}

		loadimage = dlSales.getProductImage();

		// The taxes sentence
		taxsent = dlSales.getTaxList();

		// The categories model
		m_sentcat = dlSales.getCategoriesList();
		m_CategoryModel = new ComboBoxValModel();

		// The taxes model
		taxcatsent = dlSales.getTaxCategoriesList();
		taxcatmodel = new ComboBoxValModel();

		// The attributes model
		attsent = dlSales.getAttributeSetList();
		attmodel = new ComboBoxValModel();

		m_jRef.getDocument().addDocumentListener(dirty);
		m_jName.getDocument().addDocumentListener(dirty);
		m_jComment.addActionListener(dirty);
		m_jScale.addActionListener(dirty);
		m_jVoucher.addActionListener(dirty);
		m_jCategory.addActionListener(dirty);
		m_jTax.addActionListener(dirty);
		m_jAtt.addActionListener(dirty);
		m_jPriceBuy.getDocument().addDocumentListener(dirty);
		m_jPriceBuyWholesale.getDocument().addDocumentListener(dirty);
		m_jPriceSell.getDocument().addDocumentListener(dirty);
		m_jImage.addPropertyChangeListener("image", dirty);
		m_jstockcost.getDocument().addDocumentListener(dirty);
		m_jstockvolume.getDocument().addDocumentListener(dirty);
		m_jStock.getDocument().addDocumentListener(dirty);
		m_jStockAdd.getDocument().addDocumentListener(dirty);
		m_jStockAdd.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addStock(false);
			}
		});
		m_jStockFactoryButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addStock(false);
			}
		});
		m_jStockWholesaleButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				addStock(true);
			}
		});
		m_jInCatalog.addActionListener(dirty);
		m_jCatalogOrder.getDocument().addDocumentListener(dirty);
		txtAttributes.getDocument().addDocumentListener(dirty);
		m_jSave.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				saveProduct();
			}
		});
		m_dirty.addDirtyListener(new DirtyListener() {
			public void changedDirty(boolean bDirty) {
				m_jSave.setEnabled(bDirty);
			}
		});
		m_jSave.setEnabled(false);

		FieldsManager fm = new FieldsManager();
		m_jPriceBuy.getDocument().addDocumentListener(fm);
		m_jPriceBuyWholesale.getDocument().addDocumentListener(fm);
		m_jPriceSell.getDocument().addDocumentListener(new PriceSellManager());
		m_jTax.addActionListener(fm);

		m_jPriceSellTax.getDocument().addDocumentListener(new PriceTaxManager());
		m_jmargin.getDocument().addDocumentListener(new MarginManager());
		m_jmarginTax.getDocument().addDocumentListener(new MarginTaxManager());

		writeValueEOF();
	}

	public void setBrowsableData(BrowsableEditableData bd) {
		m_bd = bd;
	}

	public void setDeleteAction(ActionListener listener) {
		m_jDelete.addActionListener(listener);
	}

	public String getProductName() {
		return m_jName.getText();
	}

	private void saveProduct() {
		if (m_bd == null) {
			return;
		}
		try {
			m_bd.saveData();
		} catch (BasicException eD) {
			MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE, LocalRes.getIntString("message.nosave"), eD);
			msg.show(this);
		}
	}

	public void activate() throws BasicException {
		if (priceRuleService != null) {
			try {
				priceTaxRegime = priceRuleService.getTaxRegime();
			} catch (SQLException e) {
				throw new BasicException(AppLocal.getIntString("message.pricerules.loaderror"), e);
			}
		}
		jLabelPriceSecondary.setText(ProductPriceMath.secondaryLabel(priceTaxRegime));
		boolean normalAccounting = priceTaxRegime == TaxRegime.NORMAL;
		jLabelMarginNet.setVisible(normalAccounting);
		m_jmargin.setVisible(normalAccounting);
		jLabelWholesaleMarginNet.setVisible(normalAccounting);
		m_jmarginWholesale.setVisible(normalAccounting);

		// Load the taxes logic
		taxeslogic = new TaxesLogic(taxsent.list());

		m_CategoryModel = new ComboBoxValModel(m_sentcat.list());
		m_jCategory.setModel(m_CategoryModel);

		taxcatmodel = new ComboBoxValModel(taxcatsent.list());
		m_jTax.setModel(taxcatmodel);

		attmodel = new ComboBoxValModel(attsent.list());
		attmodel.add(0, null);
		m_jAtt.setModel(attmodel);
	}

	public void refresh() {
	}

	public void writeValueEOF() {

		reportlock = true;
		// Los valores
		m_jTitle.setText(AppLocal.getIntString("label.recordeof"));
		m_id = null;
		m_jStock.setText(null);
		resetStockAdd();
		m_jRef.setText(null);
		m_jCode.setText(null);
		m_jName.setText(null);
		m_jFamily.setText(null);
		m_jComment.setSelected(false);
		m_jScale.setSelected(false);
		m_jVoucher.setSelected(false);
		m_CategoryModel.setSelectedKey(null);
		taxcatmodel.setSelectedKey(null);
		attmodel.setSelectedKey(null);
		m_jPriceBuy.setText(null);
		m_jPriceBuyWholesale.setText(null);
		m_purchaseCost = null;
		setPriceSell(null);
		m_jImage.setImage(null);
		m_jstockcost.setText(null);
		m_jstockvolume.setText(null);
		m_jInCatalog.setSelected(false);
		m_jCatalogOrder.setText(null);
		txtAttributes.setText(null);
		reportlock = false;

		// Los habilitados
		m_jRef.setEnabled(false);
		m_jCode.setEnabled(false);
		m_jName.setEnabled(false);
		m_jFamily.setEnabled(false);
		m_jComment.setEnabled(false);
		m_jScale.setEnabled(false);
		m_jVoucher.setEnabled(false);
		m_jCategory.setEnabled(false);
		m_jTax.setEnabled(false);
		m_jAtt.setEnabled(false);
		m_jPriceBuy.setEnabled(false);
		m_jPriceBuyWholesale.setEnabled(false);
		m_jmarginWholesale.setEnabled(false);
		m_jmarginWholesaleTax.setEnabled(false);
		m_jPriceSell.setEnabled(false);
		m_jPriceSellTax.setEnabled(false);
		m_jmargin.setEnabled(false);
		m_jmarginTax.setEnabled(false);
		m_jImage.setEnabled(false);
		m_jstockcost.setEnabled(false);
		m_jstockvolume.setEnabled(false);
		m_jStock.setEnabled(false);
		m_jStockAdd.setEnabled(false);
		m_jStockFactoryButton.setEnabled(false);
		m_jStockWholesaleButton.setEnabled(false);
		m_jInCatalog.setEnabled(false);
		m_jCatalogOrder.setEnabled(false);
		txtAttributes.setEnabled(false);

		calculateMargin();
		calculateWholesaleMargin();
		calculatePriceSellTax();
		calculateMarginTax();
		calculateMixCost();
	}

	public void writeValueInsert() {

		reportlock = true;
		// Los valores
		m_jTitle.setText(AppLocal.getIntString("label.recordnew"));
		m_id = UUID.randomUUID().toString();
		m_jStock.setText(null);
		resetStockAdd();
		m_jRef.setText(null);
		m_jCode.setText(null);
		m_jName.setText(null);
		m_jFamily.setText(null);
		m_jComment.setSelected(false);
		m_jScale.setSelected(false);
		m_jVoucher.setSelected(false);
		m_CategoryModel.setSelectedKey(null);
		taxcatmodel.setSelectedKey(null);
		attmodel.setSelectedKey(null);
		m_jPriceBuy.setText(null);
		m_jPriceBuyWholesale.setText(null);
		m_purchaseCost = null;
		setPriceSell(null);
		m_jImage.setImage(null);
		m_jstockcost.setText(null);
		m_jstockvolume.setText(null);
		m_jInCatalog.setSelected(true);
		m_jCatalogOrder.setText(null);
		txtAttributes.setText(null);
		reportlock = false;

		// Los habilitados
		m_jRef.setEnabled(true);
		m_jCode.setEnabled(true);
		m_jName.setEnabled(true);
		m_jFamily.setEnabled(false);
		m_jComment.setEnabled(true);
		m_jScale.setEnabled(true);
		m_jVoucher.setEnabled(true);
		m_jCategory.setEnabled(true);
		m_jTax.setEnabled(true);
		m_jAtt.setEnabled(true);
		m_jPriceBuy.setEnabled(true);
		m_jPriceBuyWholesale.setEnabled(true);
		m_jmarginWholesale.setEnabled(true);
		m_jmarginWholesaleTax.setEnabled(true);
		m_jPriceSell.setEnabled(true);
		m_jPriceSellTax.setEnabled(true);
		m_jmargin.setEnabled(true);
		m_jmarginTax.setEnabled(true);
		m_jImage.setEnabled(true);
		m_jstockcost.setEnabled(true);
		m_jstockvolume.setEnabled(true);
		m_jStock.setEnabled(true);
		m_jStockAdd.setEnabled(false);
		m_jStockFactoryButton.setEnabled(false);
		m_jStockWholesaleButton.setEnabled(false);
		m_jInCatalog.setEnabled(true);
		m_jCatalogOrder.setEnabled(false);
		txtAttributes.setEnabled(true);

		calculateMargin();
		calculateWholesaleMargin();
		calculatePriceSellTax();
		calculateMarginTax();
		calculateMixCost();
	}

	public void writeValueDelete(Object value) {

		reportlock = true;
		Object[] myprod = (Object[]) value;
		m_jTitle.setText(Formats.STRING.formatValue(myprod[1]) + " - " + Formats.STRING.formatValue(myprod[3]) + " "
				+ AppLocal.getIntString("label.recorddeleted"));
		m_id = myprod[0];
		m_jStock.setText(Formats.DOUBLE.formatValue(findStock(m_id)));
		resetStockAdd();
		m_jRef.setText(Formats.STRING.formatValue(myprod[1]));
		m_jCode.setText(Formats.STRING.formatValue(myprod[2]));
		m_jName.setText(Formats.STRING.formatValue(myprod[3]));
		m_jFamily.setText(Formats.STRING.formatValue(myprod[18]));
		m_jComment.setSelected(((Boolean) myprod[4]).booleanValue());
		m_jScale.setSelected(((Boolean) myprod[5]).booleanValue());
		m_jVoucher.setSelected(((Boolean) myprod[17]).booleanValue());
		m_jPriceBuy.setText(Formats.CURRENCY.formatValue(myprod[6]));
		m_jPriceBuyWholesale.setText(Formats.CURRENCY.formatValue(findPriceBuyWholesale(m_id)));
		m_purchaseCost = findPurchaseCost(m_id);
		setPriceSell(myprod[7]);
		m_CategoryModel.setSelectedKey(myprod[8]);
		taxcatmodel.setSelectedKey(myprod[9]);
		attmodel.setSelectedKey(myprod[10]);
		m_jImage.setImage(findImage(m_id));
		m_jstockcost.setText(Formats.CURRENCY.formatValue(myprod[12]));
		m_jstockvolume.setText(Formats.DOUBLE.formatValue(myprod[13]));
		m_jInCatalog.setSelected(((Boolean) myprod[14]).booleanValue());
		m_jCatalogOrder.setText(Formats.INT.formatValue(myprod[15]));
		txtAttributes.setText(Formats.BYTEA.formatValue(myprod[16]));
		txtAttributes.setCaretPosition(0);
		reportlock = false;

		// Los habilitados
		m_jRef.setEnabled(false);
		m_jCode.setEnabled(false);
		m_jName.setEnabled(false);
		m_jComment.setEnabled(false);
		m_jScale.setEnabled(false);
		m_jVoucher.setEnabled(false);
		m_jCategory.setEnabled(false);
		m_jTax.setEnabled(false);
		m_jAtt.setEnabled(false);
		m_jPriceBuy.setEnabled(false);
		m_jPriceBuyWholesale.setEnabled(false);
		m_jmarginWholesale.setEnabled(false);
		m_jmarginWholesaleTax.setEnabled(false);
		m_jPriceSell.setEnabled(false);
		m_jPriceSellTax.setEnabled(false);
		m_jmargin.setEnabled(false);
		m_jmarginTax.setEnabled(false);
		m_jImage.setEnabled(false);
		m_jstockcost.setEnabled(false);
		m_jstockvolume.setEnabled(false);
		m_jStock.setEnabled(false);
		m_jStockAdd.setEnabled(false);
		m_jStockFactoryButton.setEnabled(false);
		m_jStockWholesaleButton.setEnabled(false);
		m_jInCatalog.setEnabled(false);
		m_jCatalogOrder.setEnabled(false);
		txtAttributes.setEnabled(false);

		calculateMargin();
		calculateWholesaleMargin();
		calculatePriceSellTax();
		calculateMarginTax();
		calculateMixCost();
	}

	public void writeValueEdit(Object value) {

		reportlock = true;
		Object[] myprod = (Object[]) value;
		m_jTitle.setText(Formats.STRING.formatValue(myprod[1]) + " - " + Formats.STRING.formatValue(myprod[3]));
		m_id = myprod[0];
		m_jStock.setText(Formats.DOUBLE.formatValue(findStock(m_id)));
		resetStockAdd();
		m_jRef.setText(Formats.STRING.formatValue(myprod[1]));
		m_jCode.setText(Formats.STRING.formatValue(myprod[2]));
		m_jName.setText(Formats.STRING.formatValue(myprod[3]));
		m_jFamily.setText(Formats.STRING.formatValue(myprod[18]));
		m_jComment.setSelected(((Boolean) myprod[4]).booleanValue());
		m_jScale.setSelected(((Boolean) myprod[5]).booleanValue());
		m_jVoucher.setSelected(((Boolean) myprod[17]).booleanValue());
		m_jPriceBuy.setText(Formats.CURRENCY.formatValue(myprod[6]));
		m_jPriceBuyWholesale.setText(Formats.CURRENCY.formatValue(findPriceBuyWholesale(m_id)));
		m_purchaseCost = findPurchaseCost(m_id);
		setPriceSell(myprod[7]);
		m_CategoryModel.setSelectedKey(myprod[8]);
		taxcatmodel.setSelectedKey(myprod[9]);
		attmodel.setSelectedKey(myprod[10]);
		m_jImage.setImage(findImage(m_id));
		m_jstockcost.setText(Formats.CURRENCY.formatValue(myprod[12]));
		m_jstockvolume.setText(Formats.DOUBLE.formatValue(myprod[13]));
		m_jInCatalog.setSelected(((Boolean) myprod[14]).booleanValue());
		m_jCatalogOrder.setText(Formats.INT.formatValue(myprod[15]));
		txtAttributes.setText(Formats.BYTEA.formatValue(myprod[16]));
		txtAttributes.setCaretPosition(0);
		reportlock = false;

		// Los habilitados
		m_jRef.setEnabled(true);
		m_jCode.setEnabled(true);
		m_jName.setEnabled(true);
		m_jFamily.setEnabled(false);
		m_jComment.setEnabled(true);
		m_jScale.setEnabled(true);
		m_jVoucher.setEnabled(true);
		m_jCategory.setEnabled(true);
		m_jTax.setEnabled(true);
		m_jAtt.setEnabled(true);
		m_jPriceBuy.setEnabled(true);
		m_jPriceBuyWholesale.setEnabled(true);
		m_jmarginWholesale.setEnabled(true);
		m_jmarginWholesaleTax.setEnabled(true);
		m_jPriceSell.setEnabled(true);
		m_jPriceSellTax.setEnabled(true);
		m_jmargin.setEnabled(true);
		m_jmarginTax.setEnabled(true);
		m_jImage.setEnabled(true);
		m_jstockcost.setEnabled(true);
		m_jstockvolume.setEnabled(true);
		m_jStock.setEnabled(true);
		m_jStockAdd.setEnabled(true);
		m_jStockFactoryButton.setEnabled(true);
		m_jStockWholesaleButton.setEnabled(true);
		m_jInCatalog.setEnabled(true);
		m_jCatalogOrder.setEnabled(m_jInCatalog.isSelected());
		txtAttributes.setEnabled(true);

		calculateMargin();
		calculateWholesaleMargin();
		calculatePriceSellTax();
		calculateMarginTax();
		calculateMixCost();
	}

	public Object createValue() throws BasicException {

		if (m_jStockAdd.getText() != null && m_jStockAdd.getText().trim().length() > 0) {
			throw new BasicException(AppLocal.getIntString("message.stockaddselectsource"));
		}
		Object[] myprod = new Object[24];
		myprod[0] = m_id;
		myprod[1] = m_jRef.getText();
		myprod[2] = m_jCode.getText();
		myprod[3] = m_jName.getText();
		myprod[4] = Boolean.valueOf(m_jComment.isSelected());
		myprod[5] = Boolean.valueOf(m_jScale.isSelected());
		myprod[6] = Formats.CURRENCY.parseValue(m_jPriceBuy.getText());
		myprod[7] = pricesell;
		myprod[8] = m_CategoryModel.getSelectedKey();
		myprod[9] = taxcatmodel.getSelectedKey();
		myprod[10] = attmodel.getSelectedKey();
		myprod[11] = m_jImage.getImage();
		myprod[12] = Formats.CURRENCY.parseValue(m_jstockcost.getText());
		myprod[13] = Formats.DOUBLE.parseValue(m_jstockvolume.getText());
		myprod[14] = Boolean.valueOf(m_jInCatalog.isSelected());
		myprod[15] = Formats.INT.parseValue(m_jCatalogOrder.getText());
		myprod[16] = Formats.BYTEA.parseValue(txtAttributes.getText());
		myprod[17] = Boolean.valueOf(m_jVoucher.isSelected());
		// Keep the editor extras after the products row fields; FAMILY is field 18.
		myprod[18] = m_jFamily.getText();
		myprod[19] = Formats.DOUBLE.parseValue(m_jStock.getText());
		myprod[20] = m_App.getInventoryLocation();
		myprod[21] = m_pendingFactory > 0.0 ? new Double(m_pendingFactory) : null;
		myprod[22] = Formats.CURRENCY.parseValue(m_jPriceBuyWholesale.getText());
		myprod[23] = m_pendingWholesale > 0.0 ? new Double(m_pendingWholesale) : null;

		return myprod;
	}

	public Component getComponent() {
		return this;
	}

	private Double findPriceBuyWholesale(Object id) {
		if (id == null) {
			return null;
		}
		try {
			return m_dlSales.findPriceBuyWholesale((String) id);
		} catch (BasicException e) {
			return null;
		}
	}

	private Object[] findPurchaseCost(Object id) {
		if (id == null) {
			return null;
		}
		try {
			return m_dlSales.findPurchaseCost((String) id);
		} catch (BasicException e) {
			return null;
		}
	}

	private BufferedImage findImage(Object id) {
		try {
			return (BufferedImage) loadimage.find(id);
		} catch (BasicException e) {
			return null;
		}
	}

	private Double findStock(Object id) {
		try {
			return new Double(m_dlSales.findProductStock(m_App.getInventoryLocation(), (String) id, null));
		} catch (BasicException e) {
			return null;
		}
	}

	private void resetStockAdd() {
		m_pendingFactory = 0.0;
		m_pendingWholesale = 0.0;
		m_jStockAdd.setText(null);
		m_jStockAddResult.setText(null);
		calculateMixCost();
	}

	private void takeStockAdd(boolean wholesale) throws BasicException {

		Double units = (Double) Formats.DOUBLE.parseValue(m_jStockAdd.getText());
		if (units != null) {
			if (units.doubleValue() <= 0.0) {
				throw new BasicException(AppLocal.getIntString("message.stockaddpositive"));
			}
			Double total = (Double) Formats.DOUBLE.parseValue(m_jStock.getText());
			double newstock = (total == null ? 0.0 : total.doubleValue()) + units.doubleValue();
			if (wholesale) {
				m_pendingWholesale += units.doubleValue();
			} else {
				m_pendingFactory += units.doubleValue();
			}
			m_jStock.setText(Formats.DOUBLE.formatValue(new Double(newstock)));
			m_jStockAdd.setText(null);
			String source = AppLocal
					.getIntString(wholesale ? "label.prodstocksource.wholesale" : "label.prodstocksource.factory");
			m_jStockAddResult.setText(AppLocal.getIntString("message.stockadded", units, source, new Double(newstock)));
			calculateMixCost();
		}
	}

	private void addStock(boolean wholesale) {
		try {
			if (m_jStockAdd.getText() == null || m_jStockAdd.getText().trim().length() == 0) {
				JOptionPane.showMessageDialog(this, AppLocal.getIntString("message.stockaddpositive"),
						AppLocal.getIntString("label.prodstockadd"), JOptionPane.WARNING_MESSAGE);
				return;
			}
			takeStockAdd(wholesale);
			m_jStockAdd.requestFocusInWindow();
		} catch (BasicException e) {
			JOptionPane.showMessageDialog(this, e.getMessage(), AppLocal.getIntString("label.prodstockadd"),
					JOptionPane.WARNING_MESSAGE);
		}
	}

	private void calculateMargin() {

		if (!reportlock) {
			reportlock = true;
			setMarginFromBuy(m_jmargin, ProductPriceMath.parseCurrency(m_jPriceBuy.getText()));
			reportlock = false;
		}
		calculateSecondaryPrice();
		calculateWholesaleMargin();
		calculateMixCost();
	}

	private void calculateWholesaleMargin() {
		if (!reportlock) {
			reportlock = true;
			setMarginFromBuy(m_jmarginWholesale, ProductPriceMath.parseCurrency(m_jPriceBuyWholesale.getText()));
			setMarginFromSell(m_jmarginWholesaleTax, ProductPriceMath.parseCurrency(m_jPriceBuyWholesale.getText()),
					ProductPriceMath.parseCurrency(m_jPriceSellTax.getText()));
			reportlock = false;
		}
	}

	private void setMarginFromBuy(JTextField field, Double dPriceBuy) {
		setMarginFromSell(field, dPriceBuy, (Double) pricesell);
	}

	private void setMarginFromSell(JTextField field, Double dPriceBuy, Double dPriceSell) {
		field.setText(ProductPriceMath.formatPercent(ProductPriceMath.markup(dPriceBuy, dPriceSell)));
	}

	private void calculateMixCost() {
		if (m_jMixCost == null) {
			return;
		}
		double value = 0.0;
		double units = 0.0;
		if (m_purchaseCost != null) {
			if (m_purchaseCost[0] != null) {
				value += ((Double) m_purchaseCost[0]).doubleValue();
			}
			if (m_purchaseCost[1] != null) {
				units += ((Double) m_purchaseCost[1]).doubleValue();
			}
		}
		Double factory = ProductPriceMath.parseCurrency(m_jPriceBuy.getText());
		Double wholesale = ProductPriceMath.parseCurrency(m_jPriceBuyWholesale.getText());
		if (m_pendingFactory > 0.0 && factory != null) {
			value += m_pendingFactory * factory.doubleValue();
			units += m_pendingFactory;
		}
		if (m_pendingWholesale > 0.0) {
			Double cost = wholesale != null ? wholesale : factory;
			if (cost != null) {
				value += m_pendingWholesale * cost.doubleValue();
				units += m_pendingWholesale;
			}
		}
		Double dPriceSell = (Double) pricesell;
		if (units <= 0.0 || dPriceSell == null) {
			m_jMixCost.setText(null);
			return;
		}
		double average = value / units;
		Double dPriceSellTax = ProductPriceMath.parseCurrency(m_jPriceSellTax.getText());
		String marginTax = dPriceSellTax == null
				? ""
				: Formats.PERCENT.formatValue(new Double(dPriceSellTax.doubleValue() / average - 1.0));
		m_jMixCost.setText(AppLocal.getIntString("label.prodmixcost", Formats.CURRENCY.formatValue(new Double(average)),
				Formats.PERCENT.formatValue(new Double(dPriceSell.doubleValue() / average - 1.0)), marginTax));
	}

	private void calculatePriceSellTax() {

		if (!reportlock) {
			reportlock = true;
			m_jPriceSellTax.setText(
					ProductPriceMath.formatCurrency(ProductPriceMath.grossFromNet((Double) pricesell, taxRate())));
			reportlock = false;
		}
		calculateSecondaryPrice();
	}

	private void calculateSecondaryPrice() {
		if (m_jPriceSecondary == null || taxeslogic == null) {
			return;
		}
		TaxCategoryInfo category = selectedTax();
		Double tax = category == null ? null : Double.valueOf(taxRate());
		m_jPriceSecondary.setText(ProductPriceMath.formatCurrency(ProductPriceMath.secondary(priceTaxRegime,
				ProductPriceMath.parseCurrency(m_jPriceBuy.getText()), (Double) pricesell, tax)));
	}

	private void calculateMarginTax() {

		if (!reportlock) {
			reportlock = true;

			Double dPriceSellTax = ProductPriceMath.parseCurrency(m_jPriceSellTax.getText());
			setMarginFromSell(m_jmarginTax, grossCostBasis(ProductPriceMath.parseCurrency(m_jPriceBuy.getText())),
					dPriceSellTax);
			setMarginFromSell(m_jmarginWholesaleTax, ProductPriceMath.parseCurrency(m_jPriceBuyWholesale.getText()),
					dPriceSellTax);
			reportlock = false;
		}
	}

	private void calculatePriceSellfromMargin() {

		if (!reportlock) {
			reportlock = true;
			setPriceSell(ProductPriceMath.netFromMarkup(ProductPriceMath.parseCurrency(m_jPriceBuy.getText()),
					ProductPriceMath.parsePercent(m_jmargin.getText())));
			reportlock = false;
		}

	}

	private void calculatePriceSellfromPST() {

		if (!reportlock) {
			reportlock = true;
			setPriceSell(ProductPriceMath.netFromGross(ProductPriceMath.parseCurrency(m_jPriceSellTax.getText()),
					taxRate()));
			reportlock = false;
		}
	}

	private void calculatePriceSellfromMarginTax() {

		if (!reportlock) {
			reportlock = true;
			setPriceSell(ProductPriceMath.netFromGrossMarkup(
					grossCostBasis(ProductPriceMath.parseCurrency(m_jPriceBuy.getText())),
					ProductPriceMath.parsePercent(m_jmarginTax.getText()), taxRate()));
			reportlock = false;
		}

	}

	private Double grossCostBasis(Double factoryPrice) {
		Double tax = selectedTax() == null ? null : Double.valueOf(taxRate());
		return ProductPriceMath.grossCostBasis(factoryPrice, tax, priceTaxRegime);
	}

	private TaxCategoryInfo selectedTax() {
		return (TaxCategoryInfo) taxcatmodel.getSelectedItem();
	}

	private double taxRate() {
		if (taxeslogic == null) {
			return 0.0;
		}
		return taxeslogic.getTaxRate(selectedTax(), new Date());
	}

	private void setPriceSell(Object value) {

		if (!priceselllock) {
			priceselllock = true;
			pricesell = value;
			m_jPriceSell.setText(ProductPriceMath.formatCurrency(pricesell));
			priceselllock = false;
		}
	}

	private class PriceSellManager implements DocumentListener {
		public void changedUpdate(DocumentEvent e) {
			if (!priceselllock) {
				priceselllock = true;
				pricesell = ProductPriceMath.parseCurrency(m_jPriceSell.getText());
				priceselllock = false;
			}
			calculateMargin();
			calculatePriceSellTax();
			calculateMarginTax();
		}

		public void insertUpdate(DocumentEvent e) {
			if (!priceselllock) {
				priceselllock = true;
				pricesell = ProductPriceMath.parseCurrency(m_jPriceSell.getText());
				priceselllock = false;
			}
			calculateMargin();
			calculatePriceSellTax();
			calculateMarginTax();
		}

		public void removeUpdate(DocumentEvent e) {
			if (!priceselllock) {
				priceselllock = true;
				pricesell = ProductPriceMath.parseCurrency(m_jPriceSell.getText());
				priceselllock = false;
			}
			calculateMargin();
			calculatePriceSellTax();
			calculateMarginTax();
		}
	}

	private class FieldsManager implements DocumentListener, ActionListener {
		public void changedUpdate(DocumentEvent e) {
			calculateMargin();
			calculatePriceSellTax();
			calculateMarginTax();
		}

		public void insertUpdate(DocumentEvent e) {
			calculateMargin();
			calculatePriceSellTax();
			calculateMarginTax();
		}

		public void removeUpdate(DocumentEvent e) {
			calculateMargin();
			calculatePriceSellTax();
			calculateMarginTax();
		}

		public void actionPerformed(ActionEvent e) {
			calculateMargin();
			calculatePriceSellTax();
			calculateMarginTax();
		}
	}

	private class PriceTaxManager implements DocumentListener {
		public void changedUpdate(DocumentEvent e) {
			calculatePriceSellfromPST();
			calculateMargin();
			calculateMarginTax();
		}

		public void insertUpdate(DocumentEvent e) {
			calculatePriceSellfromPST();
			calculateMargin();
			calculateMarginTax();
		}

		public void removeUpdate(DocumentEvent e) {
			calculatePriceSellfromPST();
			calculateMargin();
			calculateMarginTax();
		}
	}

	private class MarginManager implements DocumentListener {
		public void changedUpdate(DocumentEvent e) {
			calculatePriceSellfromMargin();
			calculatePriceSellTax();
			calculateMarginTax();
		}

		public void insertUpdate(DocumentEvent e) {
			calculatePriceSellfromMargin();
			calculatePriceSellTax();
			calculateMarginTax();
		}

		public void removeUpdate(DocumentEvent e) {
			calculatePriceSellfromMargin();
			calculatePriceSellTax();
			calculateMarginTax();
		}
	}

	private class MarginTaxManager implements DocumentListener {
		public void changedUpdate(DocumentEvent e) {
			calculatePriceSellfromMarginTax();
			calculatePriceSellTax();
			calculateMargin();
		}

		public void insertUpdate(DocumentEvent e) {
			calculatePriceSellfromMarginTax();
			calculatePriceSellTax();
			calculateMargin();
		}

		public void removeUpdate(DocumentEvent e) {
			calculatePriceSellfromMarginTax();
			calculatePriceSellTax();
			calculateMargin();
		}
	}

	public void setBarcodeAndRef(String code) {
		m_jCode.setText(code);
		m_jRef.setText(code);
	}

	public String getCode() {
		return m_jCode.getText();
	}

	private final void showBarcodeGen() {
		new BFrame(this);
	}

	/**
	 * The fields read as one label per row, the same order and wording the sales
	 * screen uses when it asks for a product it has just scanned. Each tab holds
	 * one job: what the shop sells, what it buys and keeps in stock, and how the
	 * product shows up in the catalogue.
	 */
	private void initComponents() {

		m_jTitle = new javax.swing.JLabel();
		m_jTitle.setFont(new Font("SansSerif", Font.BOLD, 16));

		m_jRef = new javax.swing.JTextField(14);
		m_jName = new javax.swing.JTextField(32);
		m_jFamily = new javax.swing.JTextField(32);
		m_jCode = new javax.swing.JTextField(14);
		m_jImage = new com.openbravo.data.gui.JImageEditor();
		m_jImage.setPreferredSize(new Dimension(200, 180));

		m_jCategory = new javax.swing.JComboBox();
		m_jTax = new javax.swing.JComboBox();
		m_jAtt = new javax.swing.JComboBox();

		m_jPriceBuy = ProductFormLayout.numberField(true);
		m_jPriceSecondary = ProductFormLayout.numberField(false);
		m_jPriceSellTax = ProductFormLayout.numberField(true);
		// Never shown: the calculations pass the net price around, but the shop only
		// ever types the factory price or the price with VAT.
		m_jPriceSell = ProductFormLayout.numberField(true);
		m_jmargin = ProductFormLayout.numberField(true);
		m_jmarginTax = ProductFormLayout.numberField(true);
		m_jPriceBuyWholesale = ProductFormLayout.numberField(true);
		m_jmarginWholesale = ProductFormLayout.numberField(false);
		m_jmarginWholesaleTax = ProductFormLayout.numberField(false);
		m_jStock = ProductFormLayout.numberField(true);
		m_jStockAdd = ProductFormLayout.numberField(true);
		m_jstockcost = ProductFormLayout.numberField(true);
		m_jstockvolume = ProductFormLayout.numberField(true);
		m_jCatalogOrder = ProductFormLayout.numberField(true);

		jLabelPriceSecondary = new javax.swing.JLabel(AppLocal.getIntString("label.prodpriceeconomic"));
		jLabelMarginNet = new javax.swing.JLabel(AppLocal.getIntString("label.prodmarginwithouttax"));
		jLabelMarginTax = new javax.swing.JLabel(AppLocal.getIntString("label.prodmarginwithtax"));
		jLabelWholesaleMarginNet = new javax.swing.JLabel(AppLocal.getIntString("label.prodmarginwithouttax"));
		jLabelWholesaleMarginTax = new javax.swing.JLabel(AppLocal.getIntString("label.prodmarginwithtax"));

		m_jStockFactoryButton = new javax.swing.JButton(AppLocal.getIntString("button.stockaddfactory"));
		m_jStockWholesaleButton = new javax.swing.JButton(AppLocal.getIntString("button.stockaddwholesale"));
		m_jStockAddResult = new javax.swing.JLabel();
		m_jMixCost = new javax.swing.JLabel();

		m_jInCatalog = new javax.swing.JCheckBox();
		m_jInCatalog.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent evt) {
				catalogOrderFollowsCatalog();
			}
		});
		m_jComment = new javax.swing.JCheckBox();
		m_jScale = new javax.swing.JCheckBox();
		m_jVoucher = new javax.swing.JCheckBox();

		txtAttributes = new javax.swing.JTextArea();
		txtAttributes.setFont(new Font("DialogInput", Font.PLAIN, 12));

		m_jSave = new javax.swing.JButton(AppLocal.getIntString("Button.Save"));
		RetailPOSColors.primaryButton(m_jSave);
		m_jDelete = new javax.swing.JButton("Eliminar");
		m_jDelete.setForeground(RetailPOSColors.danger());

		m_jCode.addKeyListener(new KeyListener() {
			public void keyPressed(KeyEvent ke) {
			}

			public void keyReleased(KeyEvent ke) {
			}

			public void keyTyped(KeyEvent ke) {
				if (m_jCode.getText().length() == 13) {
					m_jRef.setText(m_jCode.getText());
				}
			}
		});

		jTabbedPane1 = new javax.swing.JTabbedPane();
		jTabbedPane1.addTab(AppLocal.getIntString("label.prodgeneral"),
				ProductFormLayout.scrollable(buildGeneralTab()));
		jTabbedPane1.addTab(AppLocal.getIntString("label.prodpurchasestock"),
				ProductFormLayout.scrollable(buildStockTab()));
		jTabbedPane1.addTab(AppLocal.getIntString("label.prodcatalog"),
				ProductFormLayout.scrollable(buildCatalogTab()));
		jTabbedPane1.addTab(AppLocal.getIntString("label.properties"), buildPropertiesTab());

		setLayout(new BorderLayout(0, 8));
		setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));
		add(buildHeader(), BorderLayout.NORTH);
		add(jTabbedPane1, BorderLayout.CENTER);
		add(buildActions(), BorderLayout.SOUTH);
	}

	private JComponent buildHeader() {
		JPanel header = new JPanel(new GridBagLayout());
		GridBagConstraints title = new GridBagConstraints();
		title.insets = ProductFormLayout.ROW_INSETS;
		title.anchor = GridBagConstraints.WEST;
		title.fill = GridBagConstraints.HORIZONTAL;
		title.gridwidth = 2;
		title.weightx = 1.0;
		header.add(m_jTitle, title);
		ProductFormLayout.addRow(header, 1, AppLocal.getIntString("label.prodref"), ProductFormLayout.inline(m_jRef));
		ProductFormLayout.addRow(header, 2, AppLocal.getIntString("label.prodname"), m_jName);
		ProductFormLayout.addRow(header, 3, "Família", m_jFamily);
		return header;
	}

	private JComponent buildGeneralTab() {
		JPanel fields = new JPanel(new GridBagLayout());
		ProductFormLayout.addRow(fields, 0, AppLocal.getIntString("label.prodbarcode"),
				ProductFormLayout.inline(m_jCode, buildOtherCodesButton(), buildLabelButton()));
		ProductFormLayout.addRow(fields, 1, AppLocal.getIntString("label.prodcategory"), m_jCategory);
		ProductFormLayout.addRow(fields, 2, AppLocal.getIntString("label.taxcategory"), m_jTax);
		ProductFormLayout.addRow(fields, 3, AppLocal.getIntString("label.prodpricebuy"),
				ProductFormLayout.inline(m_jPriceBuy, jLabelMarginNet, m_jmargin));
		ProductFormLayout.addRow(fields, 4, jLabelPriceSecondary, ProductFormLayout.inline(m_jPriceSecondary));
		ProductFormLayout.addRow(fields, 5, AppLocal.getIntString("label.prodpriceselltax"),
				ProductFormLayout.inline(m_jPriceSellTax, jLabelMarginTax, m_jmarginTax));
		ProductFormLayout.addRow(fields, 6, AppLocal.getIntString("label.attributes"), m_jAtt);
		ProductFormLayout.addRow(fields, 7, AppLocal.getIntString("label.prodvoucher"),
				ProductFormLayout.inline(m_jVoucher));

		JPanel tab = new JPanel(new BorderLayout(12, 0));
		tab.add(ProductFormLayout.topAligned(fields), BorderLayout.CENTER);
		tab.add(ProductFormLayout.topAligned(m_jImage), BorderLayout.EAST);
		return tab;
	}

	private JComponent buildStockTab() {
		JPanel fields = new JPanel(new GridBagLayout());
		ProductFormLayout.addRow(fields, 0, AppLocal.getIntString("label.prodpricebuywholesale"),
				ProductFormLayout.inline(m_jPriceBuyWholesale, jLabelWholesaleMarginNet, m_jmarginWholesale,
						jLabelWholesaleMarginTax, m_jmarginWholesaleTax));
		ProductFormLayout.addRow(fields, 1, AppLocal.getIntString("label.prodstockcurrent"),
				ProductFormLayout.inline(m_jStock));
		ProductFormLayout.addRow(fields, 2, AppLocal.getIntString("label.prodstockadd"),
				ProductFormLayout.inline(m_jStockAdd, m_jStockFactoryButton, m_jStockWholesaleButton));
		ProductFormLayout.addFullRow(fields, 3, m_jStockAddResult);
		ProductFormLayout.addFullRow(fields, 4, m_jMixCost);
		return ProductFormLayout.topAligned(fields);
	}

	private JComponent buildCatalogTab() {
		JPanel fields = new JPanel(new GridBagLayout());
		ProductFormLayout.addRow(fields, 0, AppLocal.getIntString("label.prodincatalog"),
				ProductFormLayout.inline(m_jInCatalog));
		ProductFormLayout.addRow(fields, 1, AppLocal.getIntString("label.prodorder"),
				ProductFormLayout.inline(m_jCatalogOrder));
		ProductFormLayout.addRow(fields, 2, AppLocal.getIntString("label.prodaux"),
				ProductFormLayout.inline(m_jComment));
		ProductFormLayout.addRow(fields, 3, AppLocal.getIntString("label.prodscale"),
				ProductFormLayout.inline(m_jScale));
		ProductFormLayout.addRow(fields, 4, AppLocal.getIntString("label.prodstockcost"),
				ProductFormLayout.inline(m_jstockcost));
		ProductFormLayout.addRow(fields, 5, AppLocal.getIntString("label.prodstockvol"),
				ProductFormLayout.inline(m_jstockvolume));
		return ProductFormLayout.topAligned(fields);
	}

	private JComponent buildPropertiesTab() {
		JPanel properties = new JPanel(new BorderLayout());
		properties.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		properties.add(new JScrollPane(txtAttributes), BorderLayout.CENTER);
		return properties;
	}

	private JComponent buildActions() {
		JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
		actions.add(m_jDelete);
		actions.add(m_jSave);
		return actions;
	}

	private JButton buildLabelButton() {
		JButton label = new JButton(AppLocal.getIntString("button.productlabel"));
		label.setToolTipText(AppLocal.getIntString("tooltip.productlabel"));
		label.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				showBarcodeGen();
			}
		});
		return label;
	}

	private JButton buildOtherCodesButton() {
		JButton othercodes = new JButton(AppLocal.getIntString("button.productbarcodes"));
		othercodes.setToolTipText(AppLocal.getIntString("tooltip.productbarcodes"));
		othercodes.addActionListener(new ActionListener() {
			int num = 15;
			JTextField[] fields;

			public void actionPerformed(ActionEvent ae) {
				new JFrame() {
					{
						fields = new JTextField[num];
						JLabel[] labels = new JLabel[num];
						JButton submit = new JButton("Afegeix");
						try {
							Session s = AppViewConnection.createSession(m_App.getProperties());

							setLayout(new GridLayout(num + 1, 2));
							setPreferredSize(new Dimension(230, 25 * (num + 1)));
							for (int n = 0; n < num; n++) {
								fields[n] = new JTextField();
								labels[n] = new JLabel(String.valueOf(n + 1), JLabel.CENTER);
								fields[n].setPreferredSize(new Dimension(200, 25));
								labels[n].setPreferredSize(new Dimension(30, 25));
								add(labels[n]);
								add(fields[n]);
							}
							add(new JLabel());
							add(submit);
							// check product exists in database
							String statement = "SELECT * FROM PRODUCTS WHERE PRODUCTS.CODE ='" + m_jCode.getText()
									+ "'";
							PreparedStatement ps = s.getConnection().prepareStatement(statement);
							try (ResultSet rs = ps.executeQuery()) {
								if (!rs.next()) {
									javax.swing.JOptionPane.showMessageDialog(null,
											"Desa el producte abans d'afegir més codis de barres");
								} else {
									submit.addActionListener(new ActionListener() {
										public void actionPerformed(ActionEvent ae) {
											try {
												Session s = AppViewConnection.createSession(m_App.getProperties());
												String statement;
												PreparedStatement ps;
												ResultSet rs;

												// check for duplication attempt
												statement = "SELECT BARCODE_TABLE.Code,PRODUCTS.NAME FROM BARCODE_TABLE, PRODUCTS WHERE BARCODE_TABLE.PID = PRODUCTS.ID AND PRODUCTS.ID != '"
														+ m_id + "'";
												ps = s.getConnection().prepareStatement(statement);
												ps.execute();
												rs = ps.getResultSet();

												String code;
												while (rs.next()) {
													code = rs.getString(1);
													for (int n = 0; n < num; n++) {
														Matcher m = Pattern
																.compile("0{0,2}" + fields[n].getText() + "{0,1}")
																.matcher(code);
														if (m.matches()) {
															javax.swing.JOptionPane.showMessageDialog(null, code
																	+ " is a duplicate. It is already assigned to '"
																	+ rs.getString(2) + "'");
															return;
														}
													}
												}
												// check local codes against products.code
												statement = "SELECT Products.Code,PRODUCTS.NAME FROM PRODUCTS";
												ps = s.getConnection().prepareStatement(statement);
												ps.execute();
												rs = ps.getResultSet();

												while (rs.next()) {
													code = rs.getString(1);
													for (int n = 0; n < num; n++) {
														Matcher m = Pattern
																.compile("0{0,2}" + fields[n].getText() + "{0,1}")
																.matcher(code);
														if (m.matches()) {
															javax.swing.JOptionPane.showMessageDialog(null, code
																	+ " is a duplicate. It is already assigned to '"
																	+ rs.getString(2) + "'");
															return;
														}
													}
												}
												// check for 2 same entries on form
												for (int n = 0; n < num; n++) {
													for (int m = 0; m < num; m++) {
														if (n != m && !fields[m].getText().equals("")
																&& fields[m].getText().equals(fields[n].getText())) {
															javax.swing.JOptionPane.showMessageDialog(null, fields[n]
																	.getText()
																	+ " is a duplicate. It has been entered more than once on this form");
															return;
														}
													}
												}

												// delete old
												statement = "DELETE FROM BARCODE_TABLE WHERE pid = '" + m_id + "'";
												ps = s.getConnection().prepareStatement(statement);
												ps = s.getConnection().prepareStatement(statement);
												ps.execute();

												// insert new
												for (int p = 0; p < num; p++) {
													if (!"".equals(fields[p].getText())) {
														statement = "INSERT INTO BARCODE_TABLE (pid, code) VALUES ('"
																+ m_id + "','" + fields[p].getText() + "')";
														ps = s.getConnection().prepareStatement(statement);
														ps.execute();
													}
												}
											} catch (Exception e) {
												LOGGER.log(java.util.logging.Level.WARNING,
														"event=product_barcodes_save_failed", e);
											}
											hid();
										}
									});

									statement = "SELECT barcode_table.code FROM barcode_table,products WHERE barcode_table.pid = products.ID AND products.ID = '"
											+ m_id + "'";

									ps = s.getConnection().prepareStatement(statement);
									int count = 0;
									try (ResultSet loaded = ps.executeQuery()) {
										while (loaded.next()) {
											if (count > num - 1) {
												break;
											}
											fields[count++].setText(loaded.getString(1));
										}
									}

									pack();
									setLocationRelativeTo(null);
									setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
									setVisible(true);
								}
							}
						} catch (Exception e) {
							LOGGER.log(java.util.logging.Level.WARNING, "event=product_barcodes_load_failed", e);
						}
					}

					public void hid() {
						setVisible(false);
					}
				};
			}
		});
		return othercodes;
	}

	private void catalogOrderFollowsCatalog() {

		if (m_jInCatalog.isSelected()) {
			m_jCatalogOrder.setEnabled(true);
		} else {
			m_jCatalogOrder.setEnabled(false);
			m_jCatalogOrder.setText(null);
		}

	}

	private javax.swing.JLabel jLabelPriceSecondary;
	private javax.swing.JLabel jLabelMarginNet;
	private javax.swing.JLabel jLabelMarginTax;
	private javax.swing.JLabel jLabelWholesaleMarginNet;
	private javax.swing.JLabel jLabelWholesaleMarginTax;
	private javax.swing.JTabbedPane jTabbedPane1;
	private javax.swing.JComboBox m_jAtt;
	private javax.swing.JTextField m_jCatalogOrder;
	private javax.swing.JComboBox m_jCategory;
	private javax.swing.JTextField m_jCode;
	private javax.swing.JCheckBox m_jComment;
	private com.openbravo.data.gui.JImageEditor m_jImage;
	private javax.swing.JCheckBox m_jInCatalog;
	private javax.swing.JTextField m_jName;
	private javax.swing.JTextField m_jFamily;
	private javax.swing.JTextField m_jPriceBuy;
	private javax.swing.JTextField m_jPriceBuyWholesale;
	private javax.swing.JTextField m_jPriceSecondary;
	private javax.swing.JTextField m_jPriceSell;
	private javax.swing.JTextField m_jPriceSellTax;
	private javax.swing.JTextField m_jRef;
	private javax.swing.JButton m_jSave;
	private javax.swing.JButton m_jDelete;
	private javax.swing.JCheckBox m_jScale;
	private javax.swing.JCheckBox m_jVoucher;
	private javax.swing.JTextField m_jStock;
	private javax.swing.JTextField m_jStockAdd;
	private javax.swing.JButton m_jStockFactoryButton;
	private javax.swing.JButton m_jStockWholesaleButton;
	private javax.swing.JLabel m_jStockAddResult;
	private javax.swing.JLabel m_jMixCost;
	private javax.swing.JComboBox m_jTax;
	private javax.swing.JLabel m_jTitle;
	private javax.swing.JTextField m_jmargin;
	private javax.swing.JTextField m_jmarginTax;
	private javax.swing.JTextField m_jmarginWholesale;
	private javax.swing.JTextField m_jmarginWholesaleTax;
	private javax.swing.JTextField m_jstockcost;
	private javax.swing.JTextField m_jstockvolume;
	private javax.swing.JTextArea txtAttributes;

}
