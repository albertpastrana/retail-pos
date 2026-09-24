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

package com.openbravo.pos.sales;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.openbravo.beans.JNumberKeys;
import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.format.Formats;
import com.openbravo.pos.printer.*;

import com.openbravo.pos.forms.JPanelView;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.panels.JProductFinder;
import com.openbravo.pos.payment.JPaymentSelect;
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.ListKeyed;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.data.loader.Transaction;
import com.openbravo.pos.customers.CustomerInfoExt;
import com.openbravo.pos.customers.DataLogicCustomers;
import com.openbravo.pos.customers.JCustomerFinder;
import com.openbravo.pos.customers.CustomerSheet;
import com.openbravo.pos.scripting.ScriptEngine;
import com.openbravo.pos.scripting.ScriptException;
import com.openbravo.pos.scripting.ScriptFactory;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.forms.SupervisorAuthorization;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.ticket.UserInfo;
import com.openbravo.pos.sales.shared.JTicketsBagShared;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.forms.LogContext;
import com.openbravo.pos.inventory.CatalogImportDialog;
import com.openbravo.pos.inventory.TaxCategoryInfo;
import com.openbravo.pos.inventory.PriceRuleService;
import com.openbravo.pos.inventory.TaxRegime;
import com.openbravo.pos.payment.JPaymentSelectReceipt;
import com.openbravo.pos.payment.JPaymentSelectRefund;
import com.openbravo.pos.ticket.LineDiscount;
import com.openbravo.pos.ticket.ProductInfoExt;
import com.openbravo.pos.ticket.TaxInfo;
import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.TicketLineInfo;
import com.openbravo.pos.ticket.LoyaltyStamps;
import com.openbravo.pos.util.TillButtons;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author adrianromero
 */
public abstract class JPanelTicket extends JPanel implements JPanelView, BeanFactoryApp, TicketsEditor {

	private static final Logger LOGGER = Logger.getLogger(JPanelTicket.class.getName());

	// Variable numerica
	private final static int NUMBERZERO = 0;
	private final static int NUMBERVALID = 1;

	private final static int NUMBER_INPUTZERO = 0;
	private final static int NUMBER_INPUTZERODEC = 1;
	private final static int NUMBER_INPUTINT = 2;
	private final static int NUMBER_INPUTDEC = 3;
	private final static int NUMBER_PORZERO = 4;
	private final static int NUMBER_PORZERODEC = 5;
	private final static int NUMBER_PORINT = 6;
	private final static int NUMBER_PORDEC = 7;

	protected JTicketLines m_ticketlines;

	// private Template m_tempLine;
	private TicketParser m_TTP;

	protected TicketInfo m_oTicket;
	protected Object m_oTicketExt;

	// Estas tres variables forman el estado...
	private int m_iNumberStatus;
	private int m_iNumberStatusInput;
	private int m_iNumberStatusPor;
	private boolean m_bProductImportUnknown;
	private transient Runnable ticketChangeListener;

	private JTicketsBag m_ticketsbag;

	private SentenceList senttax;
	private ListKeyed taxcollection;
	// private ComboBoxValModel m_TaxModel;

	private SentenceList senttaxcategories;
	private ListKeyed taxcategoriescollection;
	private ComboBoxValModel taxcategoriesmodel;
	private boolean taxesincluded;

	private TaxesLogic taxeslogic;

	// private ScriptObject scriptobjinst;
	protected JPanelButtons m_jbtnconfig;

	protected AppView m_App;
	protected DataLogicSystem dlSystem;
	protected DataLogicSales dlSales;
	protected DataLogicCustomers dlCustomers;
	private PriceRuleService priceRuleService;
	private TaxRegime priceTaxRegime = TaxRegime.EQUIVALENCE_SURCHARGE;

	private JPaymentSelect paymentdialogreceipt;
	private JPaymentSelect paymentdialogrefund;

	/** Creates new form JTicketView */
	public JPanelTicket() {

		initComponents();
	}

	public void init(AppView app) throws BeanFactoryException {
		long started = System.nanoTime();
		LOGGER.log(Level.INFO, "event=sales_panel_init_start panel={0}", getClass().getName());

		m_App = app;
		dlSystem = m_App.getBean(DataLogicSystem.class);
		dlSales = m_App.getBean(DataLogicSales.class);
		dlCustomers = m_App.getBean(DataLogicCustomers.class);
		priceRuleService = new PriceRuleService(m_App.getSession());

		m_ticketsbag = getJTicketsBag();
		LOGGER.log(Level.INFO, "event=sales_panel_init_bag panel={0} duration_ms={1}",
				new Object[]{getClass().getName(), elapsedMillis(started)});
		m_jPanelBag.add(m_ticketsbag.getBagComponent(), BorderLayout.LINE_START);
		add(m_ticketsbag.getNullComponent(), "null");

		m_ticketlines = new JTicketLines();
		LOGGER.log(Level.INFO, "event=sales_panel_init_ticket_lines panel={0} duration_ms={1}",
				new Object[]{getClass().getName(), elapsedMillis(started)});
		m_jPanelCentral.add(m_ticketlines, java.awt.BorderLayout.CENTER);

		m_TTP = new TicketParser(m_App.getDeviceTicket(), dlSystem);

		// Core till actions are owned by the application; layouts and other templates
		// remain database-backed resources.
		m_jbtnconfig = new JPanelButtons(this);
		m_jButtonsExt.add(m_jbtnconfig);

		alignOptionsRowHeight();

		// El panel de los productos o de las lineas...
		catcontainer.add(getSouthComponent(), BorderLayout.CENTER);
		LOGGER.log(Level.INFO, "event=sales_panel_init_south panel={0} duration_ms={1}",
				new Object[]{getClass().getName(), elapsedMillis(started)});

		// El modelo de impuestos
		senttax = dlSales.getTaxList();
		senttaxcategories = dlSales.getTaxCategoriesList();

		taxcategoriesmodel = new ComboBoxValModel();

		// ponemos a cero el estado
		stateToZero();

		// inicializamos
		m_oTicket = null;
		m_oTicketExt = null;
		LOGGER.log(Level.INFO, "event=sales_panel_init_done panel={0} duration_ms={1}",
				new Object[]{getClass().getName(), elapsedMillis(started)});
	}

	// The row above the receipt mixes named buttons with the seller ones, so every
	// control in it takes the height of the tallest one.
	private void alignOptionsRowHeight() {

		List<JComponent> controls = new ArrayList<JComponent>();
		collectButtons(m_jOptions, controls);

		int height = 0;
		for (JComponent control : controls) {
			height = Math.max(height, control.getPreferredSize().height);
		}
		for (JComponent control : controls) {
			control.setPreferredSize(new Dimension(control.getPreferredSize().width, height));
		}
	}

	private static long elapsedMillis(long started) {
		return (System.nanoTime() - started) / 1_000_000L;
	}

	private static void collectButtons(Container parent, List<JComponent> buttons) {

		for (Component child : parent.getComponents()) {
			if (child instanceof AbstractButton) {
				buttons.add((JComponent) child);
			} else if (child instanceof Container) {
				collectButtons((Container) child, buttons);
			}
		}
	}

	public Object getBean() {
		return this;
	}

	public JComponent getComponent() {
		return this;
	}

	public void activate() throws BasicException {
		try {
			priceTaxRegime = priceRuleService.getTaxRegime();
		} catch (java.sql.SQLException e) {
			throw new BasicException(AppLocal.getIntString("message.pricerules.loaderror"), e);
		}

		paymentdialogreceipt = JPaymentSelectReceipt.getDialog(this);
		paymentdialogreceipt.init(m_App);
		paymentdialogrefund = JPaymentSelectRefund.getDialog(this);
		paymentdialogrefund.init(m_App);

		// The till sells at one tax category with prices already including taxes.
		taxesincluded = "true".equals(m_jbtnconfig.getProperty("taxesincluded"));

		java.util.List<TaxInfo> taxlist = senttax.list();
		taxcollection = new ListKeyed<TaxInfo>(taxlist);
		java.util.List<TaxCategoryInfo> taxcategorieslist = senttaxcategories.list();
		taxcategoriescollection = new ListKeyed<TaxCategoryInfo>(taxcategorieslist);

		taxcategoriesmodel = new ComboBoxValModel(taxcategorieslist);

		String taxesid = m_jbtnconfig.getProperty("taxcategoryid");
		if (taxesid == null) {
			if (!taxcategorieslist.isEmpty()) {
				taxcategoriesmodel.setSelectedItem(taxcategorieslist.get(0));
			}
		} else {
			taxcategoriesmodel.setSelectedKey(taxesid);
		}

		taxeslogic = new TaxesLogic(taxlist);

		// Authorization for buttons
		m_jDelete.setEnabled(m_App.getAppUserView().getUser().hasPermission("sales.EditLines"));
		m_jNumberKeys.setMinusEnabled(m_App.getAppUserView().getUser().hasPermission("sales.EditLines"));
		m_jNumberKeys.setEqualsEnabled(m_App.getAppUserView().getUser().hasPermission("sales.Total"));
		m_jbtnconfig.setPermissions(m_App.getAppUserView().getUser());

		// The line column is a GridLayout, which keeps a slot for a hidden child,
		// so buttons the role cannot use come out of the panel instead.
		if (!m_App.getAppUserView().getUser().hasPermission("button.discount")) {
			jPanel2.remove(m_jDiscountLine);
		}
		if (!m_App.getAppUserView().getUser().hasPermission("button.discount.total")) {
			jPanel2.remove(m_jDiscountTotal);
		}
		if (LoyaltyStamps.isEnabled(m_App.getProperties().getProperty(LoyaltyStamps.ENABLED_KEY))
				&& m_App.getAppUserView().getUser().hasPermission("sales.EditLines")) {
			String loyaltyName = LoyaltyStamps.name(m_App.getProperties().getProperty(LoyaltyStamps.NAME_KEY));
			m_jLoyaltyRedemption.setToolTipText(AppLocal.getIntString("button.loyaltyredemption", loyaltyName));
			m_jLoyaltyRedemption.setText(loyaltyName);
		} else {
			jPanel2.remove(m_jLoyaltyRedemption);
		}
		if (!m_App.getAppUserView().getUser().hasPermission("sales.EditLines")) {
			jPanel2.remove(m_jEditLine);
		}

		m_ticketsbag.activate();
	}

	public boolean deactivate() {

		return m_ticketsbag.deactivate();
	}

	protected abstract JTicketsBag getJTicketsBag();

	protected abstract Component getSouthComponent();

	protected abstract void resetSouthComponent();

	public void setActiveTicket(TicketInfo oTicket, Object oTicketExt) {

		m_oTicket = oTicket;
		m_oTicketExt = oTicketExt;

		if (m_oTicket != null) {
			// Asign preeliminary properties to the receipt
			m_oTicket.setUserIfAbsent(m_App.getAppUserView().getUser().getTicketUserInfo());
			m_oTicket.setActiveCash(m_App.getActiveCashIndex());
			m_oTicket.setDate(new Date()); // Set the edition date.
			applyLoyaltyConfig(m_oTicket);
		}

		executeEvent(m_oTicket, m_oTicketExt, "ticket.show");

		refreshTicket();
	}

	public TicketInfo getActiveTicket() {
		return m_oTicket;
	}

	public UserInfo getSelectedSeller() {
		if (m_ticketsbag instanceof JTicketsBagShared) {
			return ((JTicketsBagShared) m_ticketsbag).getSelectedSeller();
		}
		if (m_oTicket != null && m_oTicket.getUser() != null) {
			return m_oTicket.getUser();
		}
		return m_App.getAppUserView().getUser().getSelectedTicketUser();
	}

	public void setTicketChangeListener(Runnable listener) {
		ticketChangeListener = listener;
	}

	private void notifyTicketChanged() {
		if (ticketChangeListener != null) {
			ticketChangeListener.run();
		}
	}

	private void refreshTicket() {
		updateCustomerButton();

		CardLayout cl = (CardLayout) (getLayout());

		if (m_oTicket == null) {
			m_ticketlines.clearTicketLines();

			m_jSubtotalEuros.setText(null);
			m_jTaxesEuros.setText(null);
			m_jTotalEuros.setText(null);
			updateLoyaltyLabel();

			stateToZero();

			// Muestro el panel de nulos.
			cl.show(this, "null");
			resetSouthComponent();

		} else {
			if (m_oTicket.getTicketType() == TicketInfo.RECEIPT_REFUND) {
				m_jList.setVisible(false);
				m_jEditLine.setVisible(false);
			}

			// Refresh ticket taxes
			for (TicketLineInfo line : m_oTicket.getLines()) {
				line.setTaxInfo(taxeslogic.getTaxInfo(line.getProductTaxCategoryID(), m_oTicket.getDate(),
						m_oTicket.getCustomer()));
			}

			// Limpiamos todas las filas y anadimos las del ticket actual
			m_ticketlines.clearTicketLines();

			for (int i = 0; i < m_oTicket.getLinesCount(); i++) {
				m_ticketlines.addTicketLine(m_oTicket.getLine(i));
			}
			printPartialTotals();
			stateToZero();

			// Muestro el panel de tickets.
			cl.show(this, "ticket");
			resetSouthComponent();

			// activo el tecleador...
			m_jKeyFactory.setText(null);
			java.awt.EventQueue.invokeLater(new Runnable() {
				public void run() {
					m_jKeyFactory.requestFocus();
				}
			});
		}
	}

	private void updateCustomerButton() {
		String label = AppLocal.getIntString("buttonlabel.customer");
		if (m_oTicket != null && m_oTicket.getCustomer() != null) {
			CustomerInfoExt customer = m_oTicket.getCustomer();
			label = customer.getName();
			if (customer.getCurdebt() != null && customer.getCurdebt().doubleValue() > 0.0) {
				label += " " + Formats.CURRENCY.formatValue(customer.getCurdebt());
			}
		}
		TillButtons.labelUnderIcon(btnCustomer, label);
	}

	private void printPartialTotals() {

		if (m_oTicket.getLinesCount() == 0) {
			m_jSubtotalEuros.setText(null);
			m_jTaxesEuros.setText(null);
			m_jTotalEuros.setText(null);
		} else {
			m_jSubtotalEuros.setText(m_oTicket.printSubTotal());
			m_jTaxesEuros.setText(m_oTicket.printTax());
			m_jTotalEuros.setText(m_oTicket.printTotal());
		}
		updateLoyaltyLabel();
	}

	private void updateLoyaltyLabel() {
		if (m_App == null || !LoyaltyStamps.isEnabled(m_App.getProperties().getProperty(LoyaltyStamps.ENABLED_KEY))) {
			m_jPanLoyalty.setVisible(false);
			return;
		}
		m_jPanLoyalty.setVisible(true);
		if (m_oTicket == null || m_oTicket.getLinesCount() == 0) {
			m_jLoyalty.setText(" ");
			return;
		}
		String loyaltyName = LoyaltyStamps.name(m_App.getProperties().getProperty(LoyaltyStamps.NAME_KEY));
		String notice = LoyaltyStamps.hasTotalDiscount(m_oTicket)
				? AppLocal.getIntString("label.loyalty.totaldiscount", loyaltyName)
				: AppLocal.getIntString("label.loyalty.thispurchase",
						Integer.valueOf(LoyaltyStamps.stampsEarned(m_oTicket)), loyaltyName);
		m_jLoyalty.setText("<html><div align=\"center\" style='width: 170px'>" + notice + "</div></html>");
	}

	private void paintTicketLine(int index, TicketLineInfo oLine) {

		if (executeEventAndRefresh("ticket.setline", new ScriptArg("index", index),
				new ScriptArg("line", oLine)) == null) {

			m_oTicket.setLine(index, oLine);
			m_ticketlines.setTicketLine(index, oLine);
			m_ticketlines.setSelectedIndex(index);

			visorTicketLine(oLine); // Y al visor tambien...
			printPartialTotals();
			stateToZero();

			// event receipt
			executeEventAndRefresh("ticket.change");
		}
	}

	private void addTicketLine(ProductInfoExt oProduct, double dMul, double dPrice) {

		TaxInfo tax = taxeslogic.getTaxInfo(oProduct.getTaxCategoryID(), m_oTicket.getDate(), m_oTicket.getCustomer());

		TicketLineInfo line = new TicketLineInfo(oProduct, dMul, dPrice, tax,
				(java.util.Properties) (oProduct.getProperties().clone()));
		if (LineDiscount.shouldApplyCatalogSale(oProduct.getPriceSell(), dPrice, oProduct.getSalePercent())) {
			LineDiscount.applyPercent(line, oProduct.getSalePercent());
		}
		addTicketLine(line);
	}

	protected void addTicketLine(TicketLineInfo oLine) {

		if (executeEventAndRefresh("ticket.addline", new ScriptArg("line", oLine)) == null) {

			TicketLineInfo oVisorLine = oLine;

			if (oLine.isProductCom()) {
				// Comentario entonces donde se pueda
				int i = m_ticketlines.getSelectedIndex();

				// me salto el primer producto normal...
				if (i >= 0 && !m_oTicket.getLine(i).isProductCom()) {
					i++;
				}

				// me salto todos los productos auxiliares...
				while (i >= 0 && i < m_oTicket.getLinesCount() && m_oTicket.getLine(i).isProductCom()) {
					i++;
				}

				if (i >= 0) {
					m_oTicket.insertLine(i, oLine);
					m_ticketlines.insertTicketLine(i, oLine); // Pintamos la linea en la vista...
				} else {
					Toolkit.getDefaultToolkit().beep();
				}
			} else {
				int i = findSameProductLine(oLine);
				if (i >= 0) {
					// Ya esta el producto en el ticket, solo aumentamos las unidades.
					TicketLineInfo oExisting = m_oTicket.getLine(i);
					oExisting.setMultiply(oExisting.getMultiply() + oLine.getMultiply());
					m_oTicket.setLine(i, oExisting);
					m_ticketlines.setTicketLine(i, oExisting);
					m_ticketlines.setSelectedIndex(i);
					oVisorLine = oExisting;
				} else {
					// Producto normal, entonces al finalnewline.getMultiply()
					m_oTicket.addLine(oLine);
					m_ticketlines.addTicketLine(oLine); // Pintamos la linea en la vista...
				}
			}

			visorTicketLine(oVisorLine);
			printPartialTotals();
			stateToZero();

			// event receipt
			executeEventAndRefresh("ticket.change");
		}
	}

	private int findSameProductLine(TicketLineInfo oLine) {

		if (oLine.getProductID() == null) {
			return -1; // productos sin referencia, siempre en una linea nueva
		}

		for (int i = 0; i < m_oTicket.getLinesCount(); i++) {
			TicketLineInfo line = m_oTicket.getLine(i);
			if (!line.isProductCom() && oLine.getProductID().equals(line.getProductID())
					&& line.getPrice() == oLine.getPrice() && (line.getMultiply() > 0.0) == (oLine.getMultiply() > 0.0)
					&& !hasComments(i)) {
				return i;
			}
		}
		return -1;
	}

	private boolean hasComments(int i) {
		// Los productos auxiliares de debajo solo describen las unidades ya vendidas.
		return i + 1 < m_oTicket.getLinesCount() && m_oTicket.getLine(i + 1).isProductCom();
	}

	private void removeTicketLine(int i) {

		if (executeEventAndRefresh("ticket.removeline", new ScriptArg("index", i)) == null) {

			if (m_oTicket.getLine(i).isProductCom()) {
				// Es un producto auxiliar, lo borro y santas pascuas.
				m_oTicket.removeLine(i);
				m_ticketlines.removeTicketLine(i);
			} else {
				// Es un producto normal, lo borro.
				m_oTicket.removeLine(i);
				m_ticketlines.removeTicketLine(i);
				// Y todos lo auxiliaries que hubiera debajo.
				while (i < m_oTicket.getLinesCount() && m_oTicket.getLine(i).isProductCom()) {
					m_oTicket.removeLine(i);
					m_ticketlines.removeTicketLine(i);
				}
			}

			visorTicketLine(null); // borro el visor
			printPartialTotals(); // pinto los totales parciales...
			stateToZero(); // Pongo a cero

			// event receipt
			executeEventAndRefresh("ticket.change");
		}
	}

	private ProductInfoExt getInputProduct() {
		ProductInfoExt oProduct = new ProductInfoExt(); // Es un ticket
		oProduct.setReference(null);
		oProduct.setCode(null);
		oProduct.setName("");
		oProduct.setTaxCategoryID(((TaxCategoryInfo) taxcategoriesmodel.getSelectedItem()).getID());

		oProduct.setPriceSell(includeTaxes(oProduct.getTaxCategoryID(), getInputValue()));

		return oProduct;
	}

	private double includeTaxes(String tcid, double dValue) {
		if (taxesincluded) {
			TaxInfo tax = taxeslogic.getTaxInfo(tcid, m_oTicket.getDate(), m_oTicket.getCustomer());
			double dTaxRate = tax == null ? 0.0 : tax.getRate();
			return dValue / (1.0 + dTaxRate);
		} else {
			return dValue;
		}
	}

	private double getInputValue() {
		try {
			return Double.parseDouble(m_jPrice.getText());
		} catch (NumberFormatException e) {
			return 0.0;
		}
	}

	private double getPorValue() {
		try {
			return Double.parseDouble(m_jPor.getText().substring(1));
		} catch (NumberFormatException e) {
			return 1.0;
		} catch (StringIndexOutOfBoundsException e) {
			return 1.0;
		}
	}

	private void stateToZero() {
		m_jPor.setText("");
		m_jPrice.setText("");
		m_jKeyFactory.setText(null);
		m_jScanStatus.setText(" ");

		m_iNumberStatus = NUMBER_INPUTZERO;
		m_iNumberStatusInput = NUMBERZERO;
		m_iNumberStatusPor = NUMBERZERO;
	}

	private void incProductByCode(String sCode) {
		// precondicion: sCode != null

		try {
			ProductInfoExt oProduct = findOrImportProduct(sCode);
			if (oProduct == null) {
				productNotFound(sCode);
			} else {
				// Se anade directamente una unidad con el precio y todo
				incProduct(oProduct);
			}
		} catch (BasicException eData) {
			stateToZero();
			new MessageInf(eData).show(this);
		}
	}

	private void incProductByCodePrice(String sCode, double dPriceSell) {
		// precondicion: sCode != null

		try {
			ProductInfoExt oProduct = findOrImportProduct(sCode);
			if (oProduct == null) {
				productNotFound(sCode);
			} else {
				// Se anade directamente una unidad con el precio y todo
				if (taxesincluded) {
					// debemos quitarle los impuestos ya que el precio es con iva incluido...
					TaxInfo tax = taxeslogic.getTaxInfo(oProduct.getTaxCategoryID(), m_oTicket.getDate(),
							m_oTicket.getCustomer());
					addTicketLine(oProduct, 1.0, dPriceSell / (1.0 + tax.getRate()));
				} else {
					addTicketLine(oProduct, 1.0, dPriceSell);
				}
			}
		} catch (BasicException eData) {
			stateToZero();
			new MessageInf(eData).show(this);
		}
	}

	private ProductInfoExt findOrImportProduct(String code) throws BasicException {
		LOGGER.log(Level.INFO, "event=sale_product_lookup_start code=\"{0}\"", code);
		CatalogImportDialog dialog = new CatalogImportDialog(this, m_App, dlSales, taxeslogic, m_oTicket.getDate(),
				m_oTicket.getCustomer(), senttaxcategories.list(), (String) taxcategoriesmodel.getSelectedKey(),
				priceRuleService, priceTaxRegime, CatalogImportDialog.Copy.RECEIPT);
		ProductInfoExt product = dialog.importIfAbsent(code);
		m_bProductImportUnknown = dialog.wasUnknown();
		LOGGER.log(Level.INFO, "event=sale_product_lookup_result code=\"{0}\" result={1} family={2}", new Object[]{code,
				product == null ? "missing" : "found", product != null && product.getFamily() != null});
		return product;
	}

	private void productNotFound(String code) {
		stateToZero();
		if (m_bProductImportUnknown) {
			Toolkit.getDefaultToolkit().beep();
			String logCode = code.replace("\\", "\\\\").replace("\"", "\\\"").replace("\r", "\\r").replace("\n", "\\n")
					.replace("\t", "\\t");
			LOGGER.log(Level.INFO, "event=unknown_barcode code=\"{0}\"", logCode);
			// HTML so the notice wraps to the keypad width instead of being cut off.
			m_jScanStatus.setText("<html><div align=\"center\">"
					+ AppLocal.getIntString("message.unknownbarcode.logged", htmlEscape(code)) + "</div></html>");
		}
	}

	private static String htmlEscape(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
	}

	private void incProduct(ProductInfoExt prod) {
		incProduct(1.0, prod);
	}

	private void incProduct(double dPor, ProductInfoExt prod) {
		// precondicion: prod != null
		addTicketLine(prod, dPor, prod.getPriceSell());
	}

	private Double getDiscountPercentage() {
		if (m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERZERO) {
			double percentage = getInputValue();
			if (percentage > 0.0 && percentage <= 100.0) {
				return new Double(percentage);
			}
		}

		Toolkit.getDefaultToolkit().beep();
		new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.invaliddiscount")).show(this);
		stateToZero();
		return null;
	}

	private String formatDiscountPercentage(double percentage) {
		return LineDiscount.formatPercentage(percentage);
	}

	private void applyLineDiscount() {
		if (!m_App.getAppUserView().getUser().hasPermission("button.discount")) {
			Toolkit.getDefaultToolkit().beep();
			stateToZero();
			return;
		}
		if (!SupervisorAuthorization.authorize(this, m_App, AppLocal.getIntString("message.authorizediscount"))) {
			stateToZero();
			return;
		}

		Double percentage = getDiscountPercentage();
		if (percentage == null) {
			return;
		}

		int index = m_oTicket.getLinesCount() - 1;
		while (index >= 0 && (m_oTicket.getLine(index).isProductCom()
				|| "total".equals(m_oTicket.getLine(index).getProperty("discount.scope")))) {
			index--;
		}

		if (index < 0) {
			Toolkit.getDefaultToolkit().beep();
			stateToZero();
			return;
		}

		TicketLineInfo line = new TicketLineInfo(m_oTicket.getLine(index));
		LineDiscount.applyPercent(line, percentage.doubleValue());
		paintTicketLine(index, line);
	}

	private void applyTotalDiscount() {
		if (!m_App.getAppUserView().getUser().hasPermission("button.discount.total")) {
			Toolkit.getDefaultToolkit().beep();
			stateToZero();
			return;
		}
		if (!SupervisorAuthorization.authorize(this, m_App, AppLocal.getIntString("message.authorizediscount"))) {
			stateToZero();
			return;
		}

		Double percentage = getDiscountPercentage();
		if (percentage == null) {
			return;
		}

		for (int i = m_oTicket.getLinesCount() - 1; i >= 0; i--) {
			if ("total".equals(m_oTicket.getLine(i).getProperty("discount.scope"))) {
				removeTicketLine(i);
			}
		}

		Map<String, Double> subtotals = new LinkedHashMap<String, Double>();
		Map<String, TicketLineInfo> taxLines = new LinkedHashMap<String, TicketLineInfo>();
		for (int i = 0; i < m_oTicket.getLinesCount(); i++) {
			TicketLineInfo line = m_oTicket.getLine(i);
			if (!line.isProductCom()) {
				String taxId = line.getTaxInfo().getId();
				Double subtotal = subtotals.get(taxId);
				subtotals.put(taxId,
						new Double((subtotal == null ? 0.0 : subtotal.doubleValue()) + line.getSubValue()));
				taxLines.put(taxId, line);
			}
		}

		String discountName = AppLocal.getIntString("button.discounttotal") + " "
				+ formatDiscountPercentage(percentage.doubleValue());
		for (Map.Entry<String, Double> entry : subtotals.entrySet()) {
			if (entry.getValue().doubleValue() != 0.0) {
				TicketLineInfo taxLine = taxLines.get(entry.getKey());
				TicketLineInfo discountLine = new TicketLineInfo(discountName, taxLine.getProductTaxCategoryID(), 1.0,
						-entry.getValue().doubleValue() * percentage.doubleValue() / 100.0, taxLine.getTaxInfo());
				discountLine.setProperty("discount.scope", "total");
				discountLine.setProperty("discount.percent", Double.toString(percentage.doubleValue()));
				addTicketLine(discountLine);
			}
		}
	}

	private void applyLoyaltyRedemption() {
		// A full card can be redeemed as many times as the receipt can absorb: what
		// is left to pay has to cover this redemption.
		if (m_oTicket.getTotal() < LoyaltyStamps.REDEMPTION_EUROS) {
			Toolkit.getDefaultToolkit().beep();
			new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.loyalty.minimumtotal")).show(this);
			return;
		}

		TaxCategoryInfo taxCategory = (TaxCategoryInfo) taxcategoriesmodel.getSelectedItem();
		if (taxCategory == null) {
			Toolkit.getDefaultToolkit().beep();
			return;
		}

		TaxInfo tax = taxeslogic.getTaxInfo(taxCategory.getID(), m_oTicket.getDate(), m_oTicket.getCustomer());
		String loyaltyName = LoyaltyStamps.name(m_App.getProperties().getProperty(LoyaltyStamps.NAME_KEY));
		TicketLineInfo redemption = new TicketLineInfo(AppLocal.getIntString("button.loyaltyredemption", loyaltyName),
				taxCategory.getID(), 1.0, includeTaxes(taxCategory.getID(), -LoyaltyStamps.REDEMPTION_EUROS), tax);
		LoyaltyStamps.markRedemption(redemption);
		addTicketLine(redemption);
	}

	private void closeCurrentTicket() {
		try (LogContext.Scope ignored = LogContext.beginOperation()) {
			LOGGER.info("event=ticket_close_start ticket=" + m_oTicket.getId() + " type=" + ticketTypeName(m_oTicket)
					+ " total=" + m_oTicket.getTotal() + " lines=" + m_oTicket.getLinesCount());
			if (m_oTicket.getLinesCount() > 0) {
				if (!m_ticketsbag.preparePayment()) {
					LOGGER.info("event=ticket_payment_cancelled reason=prepare_payment_rejected");
					return;
				}
				if (closeTicket(m_oTicket, m_oTicketExt)) {
					LOGGER.info("event=ticket_close_success");
					m_ticketsbag.deleteTicket();
				} else {
					LOGGER.warning("event=ticket_close_failed");
					m_ticketsbag.cancelPayment();
					refreshTicket();
				}
			} else {
				LOGGER.info("event=ticket_close_ignored reason=empty_ticket");
				Toolkit.getDefaultToolkit().beep();
			}
		}
	}

	protected void buttonTransition(ProductInfoExt prod) {
		// precondicion: prod != null

		typeNumberInput(); // a quantity typed in the code field applies to the button too

		if (m_iNumberStatusInput == NUMBERZERO && m_iNumberStatusPor == NUMBERZERO) {
			incProduct(prod);
		} else if (m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERZERO) {
			incProduct(getInputValue(), prod);
		} else {
			Toolkit.getDefaultToolkit().beep();
		}
	}

	// Keys that run an action instead of being part of a code.
	private static boolean isActionKey(char cTrans) {
		return cTrans == '\n' || cTrans == '=' || cTrans == ' ' || readsNumberInput(cTrans);
	}

	// The code field holds whatever was typed or scanned. The keys that work on a
	// number instead of a code read it from there, so a price or a quantity can be
	// typed into the same field and then applied with an operator.
	private static boolean readsNumberInput(char cTrans) {
		return cTrans == '*' || cTrans == '+' || cTrans == '-' || cTrans == '\u00a7'
				|| cTrans == JNumberKeys.KEY_DISCOUNT_TOTAL || cTrans == JNumberKeys.KEY_DISCOUNT_LINE;
	}

	// Once a * has been typed the digits are the quantity, which the price display
	// owns, and no longer part of a code.
	private boolean isTypingQuantity() {
		return m_iNumberStatus == NUMBER_PORZERO || m_iNumberStatus == NUMBER_PORZERODEC
				|| m_iNumberStatus == NUMBER_PORINT || m_iNumberStatus == NUMBER_PORDEC;
	}

	private static boolean isNumberChar(char cTrans) {
		return (cTrans >= '0' && cTrans <= '9') || cTrans == '.';
	}

	private void typeNumberInput() {

		if (m_iNumberStatus != NUMBER_INPUTZERO) {
			return; // the price display already holds a number
		}

		String sTyped = m_jKeyFactory.getText();
		if (sTyped == null) {
			return;
		}
		for (int i = 0; i < sTyped.length(); i++) {
			if (isNumberChar(sTyped.charAt(i))) {
				stateTransition(sTyped.charAt(i));
			}
		}
	}

	private void stateTransition(char cTrans) {

		if (readsNumberInput(cTrans)) {
			typeNumberInput();
		}

		if (cTrans == JNumberKeys.KEY_DISCOUNT_TOTAL) {
			applyTotalDiscount();
			return;
		} else if (cTrans == JNumberKeys.KEY_DISCOUNT_LINE) {
			applyLineDiscount();
			return;
		}

		if (cTrans == '\n') {
			// Codigo de barras introducido
			String sCode = m_jKeyFactory.getText();
			if (sCode != null && sCode.length() > 0) {
				if (sCode.length() == 13 && sCode.startsWith("250")) {
					// barcode of the other machine
					ProductInfoExt oProduct = new ProductInfoExt(); // Es un ticket
					oProduct.setReference(null); // para que no se grabe
					oProduct.setCode(sCode);
					oProduct.setName("Ticket " + sCode.substring(3, 7));
					oProduct.setPriceSell(Double.parseDouble(sCode.substring(7, 12)) / 100);
					oProduct.setTaxCategoryID(((TaxCategoryInfo) taxcategoriesmodel.getSelectedItem()).getID());
					// Se anade directamente una unidad con el precio y todo
					addTicketLine(oProduct, 1.0, includeTaxes(oProduct.getTaxCategoryID(), oProduct.getPriceSell()));
				} else if (sCode.length() == 13 && sCode.startsWith("210")) {
					// barcode of a weigth product
					incProductByCodePrice(sCode.substring(0, 7), Double.parseDouble(sCode.substring(7, 12)) / 100);
				} else {
					incProductByCode(sCode);
				}
			} else {
				closeCurrentTicket();
			}
		} else {
			// otro caracter
			// Esto es para el los productos normales...
			if (cTrans == '\u007f') {
				stateToZero();

			} else if ((cTrans == '0') && (m_iNumberStatus == NUMBER_INPUTZERO)) {
				m_jPrice.setText("0");
			} else if ((cTrans == '1' || cTrans == '2' || cTrans == '3' || cTrans == '4' || cTrans == '5'
					|| cTrans == '6' || cTrans == '7' || cTrans == '8' || cTrans == '9')
					&& (m_iNumberStatus == NUMBER_INPUTZERO)) {
				// Un numero entero
				m_jPrice.setText(Character.toString(cTrans));
				m_iNumberStatus = NUMBER_INPUTINT;
				m_iNumberStatusInput = NUMBERVALID;
			} else if ((cTrans == '0' || cTrans == '1' || cTrans == '2' || cTrans == '3' || cTrans == '4'
					|| cTrans == '5' || cTrans == '6' || cTrans == '7' || cTrans == '8' || cTrans == '9')
					&& (m_iNumberStatus == NUMBER_INPUTINT)) {
				// Un numero entero
				m_jPrice.setText(m_jPrice.getText() + cTrans);

			} else if (cTrans == '.' && m_iNumberStatus == NUMBER_INPUTZERO) {
				m_jPrice.setText("0.");
				m_iNumberStatus = NUMBER_INPUTZERODEC;
			} else if (cTrans == '.' && m_iNumberStatus == NUMBER_INPUTINT) {
				m_jPrice.setText(m_jPrice.getText() + ".");
				m_iNumberStatus = NUMBER_INPUTDEC;

			} else if ((cTrans == '0')
					&& (m_iNumberStatus == NUMBER_INPUTZERODEC || m_iNumberStatus == NUMBER_INPUTDEC)) {
				// Un numero decimal
				m_jPrice.setText(m_jPrice.getText() + cTrans);
			} else if ((cTrans == '1' || cTrans == '2' || cTrans == '3' || cTrans == '4' || cTrans == '5'
					|| cTrans == '6' || cTrans == '7' || cTrans == '8' || cTrans == '9')
					&& (m_iNumberStatus == NUMBER_INPUTZERODEC || m_iNumberStatus == NUMBER_INPUTDEC)) {
				// Un numero decimal
				m_jPrice.setText(m_jPrice.getText() + cTrans);
				m_iNumberStatus = NUMBER_INPUTDEC;
				m_iNumberStatusInput = NUMBERVALID;

			} else if (cTrans == '*' && (m_iNumberStatus == NUMBER_INPUTINT || m_iNumberStatus == NUMBER_INPUTDEC)) {
				m_jPor.setText("x");
				m_iNumberStatus = NUMBER_PORZERO;
			} else if (cTrans == '*'
					&& (m_iNumberStatus == NUMBER_INPUTZERO || m_iNumberStatus == NUMBER_INPUTZERODEC)) {
				m_jPrice.setText("0");
				m_jPor.setText("x");
				m_iNumberStatus = NUMBER_PORZERO;

			} else if ((cTrans == '0') && (m_iNumberStatus == NUMBER_PORZERO)) {
				m_jPor.setText("x0");
			} else if ((cTrans == '1' || cTrans == '2' || cTrans == '3' || cTrans == '4' || cTrans == '5'
					|| cTrans == '6' || cTrans == '7' || cTrans == '8' || cTrans == '9')
					&& (m_iNumberStatus == NUMBER_PORZERO)) {
				// Un numero entero
				m_jPor.setText("x" + Character.toString(cTrans));
				m_iNumberStatus = NUMBER_PORINT;
				m_iNumberStatusPor = NUMBERVALID;
			} else if ((cTrans == '0' || cTrans == '1' || cTrans == '2' || cTrans == '3' || cTrans == '4'
					|| cTrans == '5' || cTrans == '6' || cTrans == '7' || cTrans == '8' || cTrans == '9')
					&& (m_iNumberStatus == NUMBER_PORINT)) {
				// Un numero entero
				m_jPor.setText(m_jPor.getText() + cTrans);

			} else if (cTrans == '.' && m_iNumberStatus == NUMBER_PORZERO) {
				m_jPor.setText("x0.");
				m_iNumberStatus = NUMBER_PORZERODEC;
			} else if (cTrans == '.' && m_iNumberStatus == NUMBER_PORINT) {
				m_jPor.setText(m_jPor.getText() + ".");
				m_iNumberStatus = NUMBER_PORDEC;

			} else if ((cTrans == '0') && (m_iNumberStatus == NUMBER_PORZERODEC || m_iNumberStatus == NUMBER_PORDEC)) {
				// Un numero decimal
				m_jPor.setText(m_jPor.getText() + cTrans);
			} else if ((cTrans == '1' || cTrans == '2' || cTrans == '3' || cTrans == '4' || cTrans == '5'
					|| cTrans == '6' || cTrans == '7' || cTrans == '8' || cTrans == '9')
					&& (m_iNumberStatus == NUMBER_PORZERODEC || m_iNumberStatus == NUMBER_PORDEC)) {
				// Un numero decimal
				m_jPor.setText(m_jPor.getText() + cTrans);
				m_iNumberStatus = NUMBER_PORDEC;
				m_iNumberStatusPor = NUMBERVALID;

			} else if (cTrans == '+' && m_iNumberStatusInput == NUMBERZERO && m_iNumberStatusPor == NUMBERZERO) {
				int i = m_ticketlines.getSelectedIndex();
				if (i < 0) {
					Toolkit.getDefaultToolkit().beep();
				} else {
					TicketLineInfo newline = new TicketLineInfo(m_oTicket.getLine(i));
					// If it's a refund + button means one unit less
					if (m_oTicket.getTicketType() == TicketInfo.RECEIPT_REFUND) {
						newline.setMultiply(newline.getMultiply() - 1.0);
						paintTicketLine(i, newline);
					} else {
						// add one unit to the selected line
						newline.setMultiply(newline.getMultiply() + 1.0);
						paintTicketLine(i, newline);
					}
				}

				// Delete one product of the selected line
			} else if (cTrans == '-' && m_iNumberStatusInput == NUMBERZERO && m_iNumberStatusPor == NUMBERZERO
					&& m_App.getAppUserView().getUser().hasPermission("sales.EditLines")) {

				int i = m_ticketlines.getSelectedIndex();
				if (i < 0) {
					Toolkit.getDefaultToolkit().beep();
				} else {
					TicketLineInfo newline = new TicketLineInfo(m_oTicket.getLine(i));
					// If it's a refund - button means one unit more
					if (m_oTicket.getTicketType() == TicketInfo.RECEIPT_REFUND) {
						newline.setMultiply(newline.getMultiply() + 1.0);
						if (newline.getMultiply() >= 0) {
							removeTicketLine(i);
						} else {
							paintTicketLine(i, newline);
						}
					} else {
						// substract one unit to the selected line
						newline.setMultiply(newline.getMultiply() - 1.0);
						if (newline.getMultiply() <= 0.0) {
							removeTicketLine(i); // elimino la linea
						} else {
							paintTicketLine(i, newline);
						}
					}
				}

				// Set n products to the selected line
			} else if (cTrans == '+' && m_iNumberStatusInput == NUMBERZERO && m_iNumberStatusPor == NUMBERVALID) {
				int i = m_ticketlines.getSelectedIndex();
				if (i < 0) {
					Toolkit.getDefaultToolkit().beep();
				} else {
					double dPor = getPorValue();
					TicketLineInfo newline = new TicketLineInfo(m_oTicket.getLine(i));
					if (m_oTicket.getTicketType() == TicketInfo.RECEIPT_REFUND) {
						newline.setMultiply(-dPor);
						newline.setPrice(Math.abs(newline.getPrice()));
						paintTicketLine(i, newline);
					} else {
						newline.setMultiply(dPor);
						newline.setPrice(Math.abs(newline.getPrice()));
						paintTicketLine(i, newline);
					}
				}

				// Set n negative products to the selected line
			} else if (cTrans == '-' && m_iNumberStatusInput == NUMBERZERO && m_iNumberStatusPor == NUMBERVALID
					&& m_App.getAppUserView().getUser().hasPermission("sales.EditLines")) {

				int i = m_ticketlines.getSelectedIndex();
				if (i < 0) {
					Toolkit.getDefaultToolkit().beep();
				} else {
					double dPor = getPorValue();
					TicketLineInfo newline = new TicketLineInfo(m_oTicket.getLine(i));
					if (m_oTicket.getTicketType() == TicketInfo.RECEIPT_NORMAL) {
						newline.setMultiply(dPor);
						newline.setPrice(-Math.abs(newline.getPrice()));
						paintTicketLine(i, newline);
					}
				}

				// Anadimos 1 producto
			} else if (cTrans == '+' && m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERZERO
					&& m_App.getAppUserView().getUser().hasPermission("sales.EditLines")) {
				ProductInfoExt product = getInputProduct();
				addTicketLine(product, 1.0, product.getPriceSell());

				// Anadimos 1 producto con precio negativo
			} else if (cTrans == '-' && m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERZERO
					&& m_App.getAppUserView().getUser().hasPermission("sales.EditLines")) {
				ProductInfoExt product = getInputProduct();
				addTicketLine(product, 1.0, -product.getPriceSell());

				// Anadimos n productos
			} else if (cTrans == '+' && m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERVALID
					&& m_App.getAppUserView().getUser().hasPermission("sales.EditLines")) {
				ProductInfoExt product = getInputProduct();
				addTicketLine(product, getPorValue(), product.getPriceSell());

				// Anadimos n productos con precio negativo ?
			} else if (cTrans == '-' && m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERVALID
					&& m_App.getAppUserView().getUser().hasPermission("sales.EditLines")) {
				ProductInfoExt product = getInputProduct();
				addTicketLine(product, getPorValue(), -product.getPriceSell());

				// Totals() Igual;
			} else if (cTrans == ' ' || cTrans == '=') {
				closeCurrentTicket();
			}
		}
	}

	private boolean closeTicket(TicketInfo ticket, Object ticketext) {

		boolean resultok = false;

		if (m_App.getAppUserView().getUser().hasPermission("sales.Total")) {

			try {
				boolean refund = ticket.getTicketType() == TicketInfo.RECEIPT_REFUND;
				if (refund) {
					LOGGER.info("event=refund_flow_start ticket=" + ticket.getId() + " total=" + ticket.getTotal()
							+ " lines=" + ticket.getLinesCount());
				}
				// reset the payment info
				taxeslogic.calculateTaxes(ticket);
				if (ticket.getTotal() >= 0.0) {
					ticket.resetPayments(); // Only reset if is sale
				}

				if (executeEvent(ticket, ticketext, "ticket.total") == null) {

					// Muestro el total
					printTicket("Printer.TicketTotal", ticket, ticketext);

					// Select the Payments information
					JPaymentSelect paymentdialog = ticket.getTicketType() == TicketInfo.RECEIPT_NORMAL
							? paymentdialogreceipt
							: paymentdialogrefund;
					paymentdialog.setPrintSelected("true".equals(m_jbtnconfig.getProperty("printselected", "true")));

					paymentdialog.setTransactionID(ticket.getTransactionID());

					boolean paymentAccepted = paymentdialog.showDialog(ticket.getTotal(), ticket.getCustomer());
					LOGGER.info("event=ticket_payment_result ticket=" + ticket.getId() + " type="
							+ ticketTypeName(ticket) + " accepted=" + paymentAccepted + " total=" + ticket.getTotal());
					if (refund) {
						LOGGER.info("event=refund_payment_dialog_result ticket=" + ticket.getId() + " accepted="
								+ paymentAccepted + " total=" + ticket.getTotal());
					}
					if (paymentAccepted) {

						// assign the payments selected and calculate taxes.
						ticket.setPayments(paymentdialog.getSelectedPayments());
						if (refund) {
							LOGGER.info("event=refund_payment_selected ticket=" + ticket.getId() + " payments="
									+ ticket.getPayments().size());
						}

						// Asigno los valores definitivos del ticket...
						ticket.setUserIfAbsent(m_App.getAppUserView().getUser().getTicketUserInfo());
						if (ticket.getUser() == null) {
							UserInfo selectedSeller = getSelectedSeller();
							if (selectedSeller != null) {
								// Seller sessions assign the selected main-screen seller at checkout.
								ticket.setUser(selectedSeller);
								LOGGER.warning("event=ticket_user_fallback_selected_seller ticket=" + ticket.getId()
										+ " type=" + ticketTypeName(ticket) + " seller=" + selectedSeller.getId());
							}
						}
						ticket.setActiveCash(m_App.getActiveCashIndex());
						ticket.setDate(new Date()); // Le pongo la fecha de cobro

						if (executeEvent(ticket, ticketext, "ticket.save") == null) {
							// Save the receipt and assign a receipt number
							boolean saved = true;
							try {
								if (refund) {
									LOGGER.info("event=refund_persistence_start ticket=" + ticket.getId() + " total="
											+ ticket.getTotal());
								}
								new Transaction<Object>(m_App.getSession()) {
									@Override
									protected Object transact() throws BasicException {
										dlSales.saveTicket(ticket, m_App.getInventoryLocation());
										m_ticketsbag.completePayment();
										return null;
									}
								}.execute();
							} catch (BasicException eData) {
								LOGGER.log(Level.SEVERE,
										"event=" + (refund ? "refund" : "ticket") + "_persistence_failed ticket="
												+ ticket.getId() + " total=" + ticket.getTotal(),
										eData);
								MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE,
										AppLocal.getIntString("message.nosaveticket"), eData);
								msg.show(this);
								saved = false;
							}

							if (saved) {
								LOGGER.info("event=ticket_persistence_success ticket=" + ticket.getId() + " type="
										+ ticketTypeName(ticket) + " ticketNumber=" + ticket.getTicketId() + " total="
										+ ticket.getTotal());
								if (refund) {
									LOGGER.info("event=refund_flow_success ticket=" + ticket.getId() + " total="
											+ ticket.getTotal());
								}
								executeEvent(ticket, ticketext, "ticket.close",
										new ScriptArg("print", paymentdialog.isPrintSelected()));

								// Print receipt.
								printTicket(paymentdialog.isPrintSelected() ? "Printer.Ticket" : "Printer.Ticket2",
										ticket, ticketext);
								resultok = true;
							}
						}
					}
				}
			} catch (TaxesException e) {
				LOGGER.log(Level.SEVERE,
						"event=ticket_close_tax_failed ticket=" + ticket.getId() + " type=" + ticketTypeName(ticket),
						e);
				MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
						AppLocal.getIntString("message.cannotcalculatetaxes"));
				msg.show(this);
				resultok = false;
			} catch (RuntimeException e) {
				LOGGER.log(Level.SEVERE, "event=ticket_close_unexpected_failure ticket=" + ticket.getId() + " type="
						+ ticketTypeName(ticket) + " total=" + ticket.getTotal(), e);
				new MessageInf(MessageInf.SGN_NOTICE, AppLocal.getIntString("message.nosaveticket"), e).show(this);
				resultok = false;
			}

			// reset the payment info
			m_oTicket.resetTaxes();
			m_oTicket.resetPayments();
		} else {
			LOGGER.warning("event=ticket_close_denied ticket=" + ticket.getId() + " reason=missing_permission");
		}

		// cancelled the ticket.total script
		// or canceled the payment dialog
		// or canceled the ticket.close script
		return resultok;
	}

	private static String ticketTypeName(TicketInfo ticket) {
		return ticket.getTicketType() == TicketInfo.RECEIPT_REFUND ? "refund" : "sale";
	}

	private void printTicket(String sresourcename, TicketInfo ticket, Object ticketext) {

		String sresource = dlSystem.getResourceAsXML(sresourcename);
		if (sresource == null) {
			MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotprintticket"));
			msg.show(JPanelTicket.this);
		} else {
			try {
				applyLoyaltyConfig(ticket);
				ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);
				script.put("taxes", taxcollection);
				script.put("taxeslogic", taxeslogic);
				script.put("ticket", ticket);
				script.put("place", ticketext);
				m_TTP.printTicket(script.eval(sresource).toString());
				LOGGER.info("event=ticket_print_success ticket=" + ticket.getId() + " resource=" + sresourcename);
			} catch (ScriptException e) {
				LOGGER.log(Level.WARNING, "event=ticket_print_failed ticket=" + ticket.getId() + " resource="
						+ sresourcename + " reason=template", e);
				MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
						AppLocal.getIntString("message.cannotprintticket"), e);
				msg.show(JPanelTicket.this);
			} catch (TicketPrinterException e) {
				LOGGER.log(Level.WARNING, "event=ticket_print_failed ticket=" + ticket.getId() + " resource="
						+ sresourcename + " reason=device", e);
				MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
						AppLocal.getIntString("message.cannotprintticket"), e);
				msg.show(JPanelTicket.this);
			}
		}
	}

	private void visorTicketLine(TicketLineInfo oLine) {
		if (oLine == null) {
			m_App.getDeviceTicket().getDeviceDisplay().clearVisor();
		} else {
			try {
				ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);
				script.put("ticketline", oLine);
				m_TTP.printTicket(script.eval(dlSystem.getResourceAsXML("Printer.TicketLine")).toString());
			} catch (ScriptException e) {
				MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
						AppLocal.getIntString("message.cannotprintline"), e);
				msg.show(JPanelTicket.this);
			} catch (TicketPrinterException e) {
				MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
						AppLocal.getIntString("message.cannotprintline"), e);
				msg.show(JPanelTicket.this);
			}
		}
	}

	private Object evalScript(ScriptObject scr, String resource, ScriptArg... args) {

		// resource here is guaratied to be not null
		try {
			scr.setSelectedIndex(m_ticketlines.getSelectedIndex());
			return scr.evalScript(dlSystem.getResourceAsXML(resource), args);
		} catch (ScriptException e) {
			MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotexecute"), e);
			msg.show(this);
			return msg;
		}
	}

	public void evalScriptAndRefresh(String resource, ScriptArg... args) {

		if (resource == null) {
			MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotexecute"));
			msg.show(this);
		} else {
			ScriptObject scr = new ScriptObject(m_oTicket, m_oTicketExt);
			scr.setSelectedIndex(m_ticketlines.getSelectedIndex());
			evalScript(scr, resource, args);
			refreshTicket();
			setSelectedIndex(scr.getSelectedIndex());
		}
	}

	public void printTicket(String resource) {
		printTicket(resource, m_oTicket, m_oTicketExt);
	}

	private void applyLoyaltyConfig(TicketInfo ticket) {
		LoyaltyStamps.applyToTicket(ticket, m_App.getProperties().getProperty(LoyaltyStamps.ENABLED_KEY),
				m_App.getProperties().getProperty(LoyaltyStamps.NAME_KEY));
	}

	private Object executeEventAndRefresh(String eventkey, ScriptArg... args) {

		String resource = m_jbtnconfig.getEvent(eventkey);
		Object result;
		if (resource == null) {
			result = null;
		} else {
			ScriptObject scr = new ScriptObject(m_oTicket, m_oTicketExt);
			scr.setSelectedIndex(m_ticketlines.getSelectedIndex());
			result = evalScript(scr, resource, args);
			refreshTicket();
			setSelectedIndex(scr.getSelectedIndex());
		}
		if ("ticket.change".equals(eventkey)) {
			notifyTicketChanged();
		}
		return result;
	}

	private Object executeEvent(TicketInfo ticket, Object ticketext, String eventkey, ScriptArg... args) {

		String resource = m_jbtnconfig.getEvent(eventkey);
		if (resource == null) {
			return null;
		} else {
			ScriptObject scr = new ScriptObject(ticket, ticketext);
			return evalScript(scr, resource, args);
		}
	}

	public String getResourceAsXML(String sresourcename) {
		return dlSystem.getResourceAsXML(sresourcename);
	}

	public BufferedImage getResourceAsImage(String sresourcename) {
		return dlSystem.getResourceAsImage(sresourcename);
	}

	private void setSelectedIndex(int i) {

		if (i >= 0 && i < m_oTicket.getLinesCount()) {
			m_ticketlines.setSelectedIndex(i);
		} else if (m_oTicket.getLinesCount() > 0) {
			m_ticketlines.setSelectedIndex(m_oTicket.getLinesCount() - 1);
		}
	}

	public static class ScriptArg {
		private String key;
		private Object value;

		public ScriptArg(String key, Object value) {
			this.key = key;
			this.value = value;
		}

		public String getKey() {
			return key;
		}

		public Object getValue() {
			return value;
		}
	}

	public class ScriptObject {

		private TicketInfo ticket;
		private Object ticketext;

		private int selectedindex;

		private ScriptObject(TicketInfo ticket, Object ticketext) {
			this.ticket = ticket;
			this.ticketext = ticketext;
		}

		public double getInputValue() {
			if (m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERZERO) {
				return JPanelTicket.this.getInputValue();
			} else {
				return 0.0;
			}
		}

		public int getSelectedIndex() {
			return selectedindex;
		}

		public void setSelectedIndex(int i) {
			selectedindex = i;
		}

		public void printTicket(String sresourcename) {
			JPanelTicket.this.printTicket(sresourcename, ticket, ticketext);
		}

		public Object evalScript(String code, ScriptArg... args) throws ScriptException {

			ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.BEANSHELL);
			script.put("ticket", ticket);
			script.put("place", ticketext);
			script.put("taxes", taxcollection);
			script.put("taxeslogic", taxeslogic);
			script.put("user", m_App.getAppUserView().getUser());
			script.put("sales", this);

			// more arguments
			for (ScriptArg arg : args) {
				script.put(arg.getKey(), arg.getValue());
			}

			return script.eval(code);
		}
	}

	/**
	 * This method is called from within the constructor to initialize the form.
	 * WARNING: Do NOT modify this code. The content of this method is always
	 * regenerated by the FormEditor.
	 */
	// <editor-fold defaultstate="collapsed" desc="Generated
	// Code">//GEN-BEGIN:initComponents
	private void initComponents() {
		java.awt.GridBagConstraints gridBagConstraints;

		m_jPanContainer = new javax.swing.JPanel();
		m_jOptions = new javax.swing.JPanel();
		m_jButtons = new javax.swing.JPanel();
		btnCustomer = new javax.swing.JButton();
		m_jPanelScripts = new javax.swing.JPanel();
		m_jButtonsExt = new javax.swing.JPanel();
		m_jPanelBag = new javax.swing.JPanel();
		m_jPanTicket = new javax.swing.JPanel();
		jPanel5 = new javax.swing.JPanel();
		jPanel2 = new javax.swing.JPanel();
		m_jDelete = new javax.swing.JButton();
		m_jList = new javax.swing.JButton();
		m_jDiscountLine = new javax.swing.JButton();
		m_jDiscountTotal = new javax.swing.JButton();
		m_jLoyaltyRedemption = new javax.swing.JButton();
		m_jEditLine = new javax.swing.JButton();
		m_jPanelCentral = new javax.swing.JPanel();
		jPanel4 = new javax.swing.JPanel();
		m_jPanTotals = new javax.swing.JPanel();
		m_jTotalEuros = new javax.swing.JLabel();
		m_jLblTotalEuros1 = new javax.swing.JLabel();
		m_jSubtotalEuros = new javax.swing.JLabel();
		m_jTaxesEuros = new javax.swing.JLabel();
		m_jLblTotalEuros2 = new javax.swing.JLabel();
		m_jLblTotalEuros3 = new javax.swing.JLabel();
		m_jContEntries = new javax.swing.JPanel();
		m_jPanEntries = new javax.swing.JPanel();
		m_jPanCode = new javax.swing.JPanel();
		m_jLblCode = new javax.swing.JLabel();
		m_jNumberKeys = new com.openbravo.beans.JNumberKeys();
		m_jPanReadout = new javax.swing.JPanel();
		m_jPrice = new javax.swing.JLabel();
		m_jPor = new javax.swing.JLabel();
		m_jPanLoyalty = new javax.swing.JPanel();
		m_jLoyalty = new javax.swing.JLabel();
		m_jPanScanStatus = new javax.swing.JPanel();
		m_jScanStatus = new javax.swing.JLabel();
		m_jEnter = new javax.swing.JButton();
		m_jKeyFactory = new javax.swing.JTextField();
		catcontainer = new javax.swing.JPanel();

		setBackground(new java.awt.Color(255, 204, 153));
		setLayout(new java.awt.CardLayout());

		m_jPanContainer.setLayout(new java.awt.BorderLayout());

		m_jOptions.setLayout(new java.awt.BorderLayout());

		btnCustomer.setIcon(TillButtons.icon("/com/openbravo/images/till/user-circle.png")); // NOI18N
		btnCustomer.setToolTipText(AppLocal.getIntString("tooltiptext.customer")); // NOI18N
		TillButtons.labelUnderIcon(btnCustomer, AppLocal.getIntString("buttonlabel.customer")); // NOI18N
		btnCustomer.setFocusPainted(false);
		btnCustomer.setFocusable(false);
		btnCustomer.setRequestFocusEnabled(false);
		btnCustomer.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				btnCustomerActionPerformed(evt);
			}
		});
		m_jButtons.add(btnCustomer);

		m_jOptions.add(m_jButtons, java.awt.BorderLayout.LINE_START);

		m_jPanelScripts.setLayout(new java.awt.BorderLayout());

		m_jButtonsExt.setLayout(new javax.swing.BoxLayout(m_jButtonsExt, javax.swing.BoxLayout.LINE_AXIS));

		m_jPanelScripts.add(m_jButtonsExt, java.awt.BorderLayout.LINE_END);

		m_jOptions.add(m_jPanelScripts, java.awt.BorderLayout.LINE_END);

		m_jPanelBag.setLayout(new java.awt.BorderLayout());
		m_jOptions.add(m_jPanelBag, java.awt.BorderLayout.CENTER);

		m_jPanContainer.add(m_jOptions, java.awt.BorderLayout.NORTH);

		m_jPanTicket.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		m_jPanTicket.setLayout(new java.awt.BorderLayout());

		jPanel5.setLayout(new java.awt.BorderLayout());

		jPanel2.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 0, 5));
		jPanel2.setLayout(new java.awt.GridLayout(0, 1, 5, 5));

		m_jDelete.setIcon(TillButtons.icon("/com/openbravo/images/till/x-circle.png")); // NOI18N
		TillButtons.labelUnderIcon(m_jDelete, AppLocal.getIntString("buttonlabel.deleteline")); // NOI18N
		m_jDelete.setFocusPainted(false);
		m_jDelete.setFocusable(false);
		m_jDelete.setRequestFocusEnabled(false);
		m_jDelete.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jDeleteActionPerformed(evt);
			}
		});
		jPanel2.add(m_jDelete);

		m_jList.setIcon(TillButtons.icon("/com/openbravo/images/till/magnifying-glass.png")); // NOI18N
		TillButtons.labelUnderIcon(m_jList, AppLocal.getIntString("buttonlabel.findproduct")); // NOI18N
		m_jList.setFocusPainted(false);
		m_jList.setFocusable(false);
		m_jList.setRequestFocusEnabled(false);
		m_jList.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jListActionPerformed(evt);
			}
		});
		jPanel2.add(m_jList);

		m_jDiscountLine.setIcon(TillButtons.icon("/com/openbravo/images/till/percent.png")); // NOI18N
		m_jDiscountLine.setToolTipText(AppLocal.getIntString("button.discountline")); // NOI18N
		TillButtons.labelUnderIcon(m_jDiscountLine, AppLocal.getIntString("buttonlabel.discountline")); // NOI18N
		m_jDiscountLine.setFocusPainted(false);
		m_jDiscountLine.setFocusable(false);
		m_jDiscountLine.setRequestFocusEnabled(false);
		m_jDiscountLine.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jDiscountLineActionPerformed(evt);
			}
		});
		jPanel2.add(m_jDiscountLine);

		m_jDiscountTotal.setIcon(TillButtons.icon("/com/openbravo/images/till/seal-percent.png")); // NOI18N
		m_jDiscountTotal.setToolTipText(AppLocal.getIntString("button.discounttotal")); // NOI18N
		TillButtons.labelUnderIcon(m_jDiscountTotal, AppLocal.getIntString("buttonlabel.discounttotal")); // NOI18N
		m_jDiscountTotal.setFocusPainted(false);
		m_jDiscountTotal.setFocusable(false);
		m_jDiscountTotal.setRequestFocusEnabled(false);
		m_jDiscountTotal.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jDiscountTotalActionPerformed(evt);
			}
		});
		jPanel2.add(m_jDiscountTotal);

		m_jLoyaltyRedemption.setIcon(TillButtons.icon("/com/openbravo/images/till/stamp.png"));
		m_jLoyaltyRedemption
				.setToolTipText(AppLocal.getIntString("button.loyaltyredemption", LoyaltyStamps.DEFAULT_NAME));
		TillButtons.labelUnderIcon(m_jLoyaltyRedemption, LoyaltyStamps.DEFAULT_NAME);
		m_jLoyaltyRedemption.setFocusPainted(false);
		m_jLoyaltyRedemption.setFocusable(false);
		m_jLoyaltyRedemption.setRequestFocusEnabled(false);
		m_jLoyaltyRedemption.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jLoyaltyRedemptionActionPerformed(evt);
			}
		});
		jPanel2.add(m_jLoyaltyRedemption);

		m_jEditLine.setIcon(TillButtons.icon("/com/openbravo/images/till/pencil-simple.png")); // NOI18N
		TillButtons.labelUnderIcon(m_jEditLine, AppLocal.getIntString("buttonlabel.editline")); // NOI18N
		m_jEditLine.setFocusPainted(false);
		m_jEditLine.setFocusable(false);
		m_jEditLine.setRequestFocusEnabled(false);
		m_jEditLine.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jEditLineActionPerformed(evt);
			}
		});
		jPanel2.add(m_jEditLine);

		jPanel5.add(jPanel2, java.awt.BorderLayout.NORTH);

		m_jPanTicket.add(jPanel5, java.awt.BorderLayout.LINE_END);

		m_jPanelCentral.setLayout(new java.awt.BorderLayout());

		jPanel4.setLayout(new java.awt.BorderLayout());

		m_jPanTotals.setLayout(new java.awt.GridBagLayout());

		m_jTotalEuros.setBackground(com.openbravo.pos.theme.RetailPOSColors.surface100());
		m_jTotalEuros.setFont(new java.awt.Font("Dialog", 1, 14));
		m_jTotalEuros.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
		m_jTotalEuros.setBorder(javax.swing.BorderFactory.createCompoundBorder(
				javax.swing.BorderFactory
						.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")),
				javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
		m_jTotalEuros.setOpaque(true);
		m_jTotalEuros.setPreferredSize(new java.awt.Dimension(150, 25));
		m_jTotalEuros.setRequestFocusEnabled(false);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
		m_jPanTotals.add(m_jTotalEuros, gridBagConstraints);

		m_jLblTotalEuros1.setText(AppLocal.getIntString("label.totalcash")); // NOI18N
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 1;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
		m_jPanTotals.add(m_jLblTotalEuros1, gridBagConstraints);

		m_jSubtotalEuros.setBackground(com.openbravo.pos.theme.RetailPOSColors.surface100());
		m_jSubtotalEuros.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
		m_jSubtotalEuros.setBorder(javax.swing.BorderFactory.createCompoundBorder(
				javax.swing.BorderFactory
						.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")),
				javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
		m_jSubtotalEuros.setOpaque(true);
		m_jSubtotalEuros.setPreferredSize(new java.awt.Dimension(150, 25));
		m_jSubtotalEuros.setRequestFocusEnabled(false);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 3;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
		m_jPanTotals.add(m_jSubtotalEuros, gridBagConstraints);

		m_jTaxesEuros.setBackground(com.openbravo.pos.theme.RetailPOSColors.surface100());
		m_jTaxesEuros.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
		m_jTaxesEuros.setBorder(javax.swing.BorderFactory.createCompoundBorder(
				javax.swing.BorderFactory
						.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")),
				javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
		m_jTaxesEuros.setOpaque(true);
		m_jTaxesEuros.setPreferredSize(new java.awt.Dimension(150, 25));
		m_jTaxesEuros.setRequestFocusEnabled(false);
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 1;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
		gridBagConstraints.weightx = 1.0;
		gridBagConstraints.weighty = 1.0;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 5);
		m_jPanTotals.add(m_jTaxesEuros, gridBagConstraints);

		m_jLblTotalEuros2.setText(AppLocal.getIntString("label.taxcash")); // NOI18N
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 0;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
		gridBagConstraints.insets = new java.awt.Insets(5, 0, 0, 0);
		m_jPanTotals.add(m_jLblTotalEuros2, gridBagConstraints);

		m_jLblTotalEuros3.setText(AppLocal.getIntString("label.subtotalcash")); // NOI18N
		gridBagConstraints = new java.awt.GridBagConstraints();
		gridBagConstraints.gridx = 2;
		gridBagConstraints.gridy = 0;
		gridBagConstraints.anchor = java.awt.GridBagConstraints.FIRST_LINE_START;
		gridBagConstraints.insets = new java.awt.Insets(5, 5, 0, 0);
		m_jPanTotals.add(m_jLblTotalEuros3, gridBagConstraints);

		jPanel4.add(m_jPanTotals, java.awt.BorderLayout.LINE_END);

		m_jPanelCentral.add(jPanel4, java.awt.BorderLayout.SOUTH);

		m_jPanTicket.add(m_jPanelCentral, java.awt.BorderLayout.CENTER);

		m_jPanContainer.add(m_jPanTicket, java.awt.BorderLayout.CENTER);

		m_jContEntries.setLayout(new java.awt.BorderLayout());

		m_jPanEntries.setLayout(new javax.swing.BoxLayout(m_jPanEntries, javax.swing.BoxLayout.Y_AXIS));

		m_jPanCode.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 0, 5));
		m_jPanCode.setLayout(new java.awt.BorderLayout(5, 0));
		m_jPanCode.setMaximumSize(new java.awt.Dimension(32767, 40));

		m_jLblCode.setText(AppLocal.getIntString("label.code")); // NOI18N
		m_jLblCode.setLabelFor(m_jKeyFactory);
		m_jPanCode.add(m_jLblCode, java.awt.BorderLayout.LINE_START);

		m_jKeyFactory.setPreferredSize(new java.awt.Dimension(100, 25));
		m_jKeyFactory.addKeyListener(new java.awt.event.KeyAdapter() {
			public void keyTyped(java.awt.event.KeyEvent evt) {
				m_jKeyFactoryKeyTyped(evt);
			}
		});
		m_jPanCode.add(m_jKeyFactory, java.awt.BorderLayout.CENTER);

		m_jEnter.setIcon(TillButtons.icon("/com/openbravo/images/till/barcode.png")); // NOI18N
		m_jEnter.setToolTipText(AppLocal.getIntString("tooltiptext.entercode")); // NOI18N
		m_jEnter.setFocusPainted(false);
		m_jEnter.setFocusable(false);
		m_jEnter.setRequestFocusEnabled(false);
		m_jEnter.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jEnterActionPerformed(evt);
			}
		});
		m_jPanCode.add(m_jEnter, java.awt.BorderLayout.LINE_END);

		m_jPanEntries.add(m_jPanCode);

		m_jNumberKeys.addJNumberEventListener(new com.openbravo.beans.JNumberEventListener() {
			public void keyPerformed(com.openbravo.beans.JNumberEvent evt) {
				m_jNumberKeysKeyPerformed(evt);
			}
		});
		m_jPanEntries.add(m_jNumberKeys);

		// A readout, not a field: the keypad writes here and nothing else does.
		m_jPanReadout.setBorder(javax.swing.BorderFactory.createEmptyBorder(0, 5, 5, 5));
		m_jPanReadout.setLayout(new java.awt.FlowLayout(java.awt.FlowLayout.TRAILING, 8, 0));
		m_jPanReadout.setMaximumSize(new java.awt.Dimension(32767, 40));

		m_jPor.setForeground(java.awt.Color.GRAY);
		m_jPor.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
		m_jPor.setPreferredSize(new java.awt.Dimension(50, 30));
		m_jPor.setRequestFocusEnabled(false);
		m_jPanReadout.add(m_jPor);

		m_jPrice.setFont(new java.awt.Font("Dialog", 1, 18));
		m_jPrice.setHorizontalAlignment(javax.swing.SwingConstants.TRAILING);
		m_jPrice.setPreferredSize(new java.awt.Dimension(130, 30));
		m_jPrice.setRequestFocusEnabled(false);
		m_jPanReadout.add(m_jPrice);

		m_jPanEntries.add(m_jPanReadout);

		// Three wrapped lines of the notice at the width of the keypad column, so the
		// count at the end of the sentence is never the part that gets cut off.
		m_jPanLoyalty.setBorder(javax.swing.BorderFactory.createEmptyBorder(8, 5, 0, 5));
		m_jPanLoyalty.setLayout(new java.awt.BorderLayout());
		m_jPanLoyalty.setPreferredSize(new java.awt.Dimension(180, 76));
		m_jPanLoyalty.setMaximumSize(new java.awt.Dimension(32767, 76));

		m_jLoyalty.setFont(new java.awt.Font("Dialog", 1, 13));
		m_jLoyalty.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		m_jLoyalty.setText(" ");
		m_jLoyalty.setRequestFocusEnabled(false);
		m_jPanLoyalty.add(m_jLoyalty, java.awt.BorderLayout.CENTER);

		m_jPanEntries.add(m_jPanLoyalty);

		// What happened to the last scan, on its own row below the keys so it is not
		// read as part of them. Like the code and readout rows, the row spans the
		// column and keeps its height, so the notice is centred and nothing jumps.
		m_jPanScanStatus.setBorder(javax.swing.BorderFactory.createEmptyBorder(16, 5, 8, 5));
		m_jPanScanStatus.setLayout(new java.awt.BorderLayout());
		m_jPanScanStatus.setPreferredSize(new java.awt.Dimension(100, 60));
		m_jPanScanStatus.setMaximumSize(new java.awt.Dimension(32767, 60));

		m_jScanStatus.setFont(new java.awt.Font("Dialog", 1, 13));
		m_jScanStatus.setForeground(new java.awt.Color(166, 51, 0));
		m_jScanStatus.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		m_jScanStatus.setRequestFocusEnabled(false);
		m_jPanScanStatus.add(m_jScanStatus, java.awt.BorderLayout.CENTER);

		m_jPanEntries.add(m_jPanScanStatus);

		m_jContEntries.add(m_jPanEntries, java.awt.BorderLayout.NORTH);

		m_jPanContainer.add(m_jContEntries, java.awt.BorderLayout.LINE_END);

		catcontainer.setBorder(javax.swing.BorderFactory.createEmptyBorder(5, 5, 5, 5));
		catcontainer.setLayout(new java.awt.BorderLayout());
		m_jPanContainer.add(catcontainer, java.awt.BorderLayout.SOUTH);

		add(m_jPanContainer, "ticket");
	}// </editor-fold>//GEN-END:initComponents

	private void m_jDiscountLineActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jDiscountLineActionPerformed

		stateTransition(JNumberKeys.KEY_DISCOUNT_LINE);

	}// GEN-LAST:event_m_jDiscountLineActionPerformed

	private void m_jDiscountTotalActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jDiscountTotalActionPerformed

		stateTransition(JNumberKeys.KEY_DISCOUNT_TOTAL);

	}// GEN-LAST:event_m_jDiscountTotalActionPerformed

	private void m_jLoyaltyRedemptionActionPerformed(java.awt.event.ActionEvent evt) {

		applyLoyaltyRedemption();

	}

	private void m_jEditLineActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jEditLineActionPerformed

		int i = m_ticketlines.getSelectedIndex();
		if (i < 0) {
			Toolkit.getDefaultToolkit().beep(); // no line selected
		} else {
			try {
				TicketLineInfo newline = JProductLineEdit.showMessage(this, m_App, m_oTicket.getLine(i));
				if (newline != null) {
					paintTicketLine(i, newline);
				}
			} catch (BasicException e) {
				new MessageInf(e).show(this);
			}
		}

	}// GEN-LAST:event_m_jEditLineActionPerformed

	private void m_jEnterActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jEnterActionPerformed

		stateTransition('\n');

	}// GEN-LAST:event_m_jEnterActionPerformed

	private void m_jNumberKeysKeyPerformed(com.openbravo.beans.JNumberEvent evt) {// GEN-FIRST:event_m_jNumberKeysKeyPerformed

		char cKey = evt.getKey();

		if (isNumberChar(cKey) && !isTypingQuantity()) {
			// The on screen keys type into the code field, like the keyboard does.
			m_jKeyFactory.replaceSelection(Character.toString(cKey));
		} else {
			stateTransition(cKey);
		}

	}// GEN-LAST:event_m_jNumberKeysKeyPerformed

	private void m_jKeyFactoryKeyTyped(java.awt.event.KeyEvent evt) {// GEN-FIRST:event_m_jKeyFactoryKeyTyped

		char cTyped = evt.getKeyChar();

		if (isActionKey(cTyped) || (isTypingQuantity() && isNumberChar(cTyped))) {
			// Enter looks the code up and the operators act on the number, so neither
			// belongs in the text. Everything else is typed into the field as usual.
			evt.consume();
			stateTransition(cTyped);
		}

	}// GEN-LAST:event_m_jKeyFactoryKeyTyped

	private void m_jDeleteActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jDeleteActionPerformed

		int i = m_ticketlines.getSelectedIndex();
		if (i < 0) {
			Toolkit.getDefaultToolkit().beep(); // No hay ninguna seleccionada
		} else {
			removeTicketLine(i); // elimino la linea
		}

	}// GEN-LAST:event_m_jDeleteActionPerformed

	private void m_jListActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_m_jListActionPerformed

		ProductInfoExt prod = JProductFinder.showMessage(JPanelTicket.this, dlSales, "button.addproduct");
		if (prod != null) {
			buttonTransition(prod);
		}

	}// GEN-LAST:event_m_jListActionPerformed

	private void btnCustomerActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnCustomerActionPerformed

		try {
			boolean openFinder = m_oTicket.getCustomer() == null;
			if (m_oTicket.getCustomer() != null) {
				int action = CustomerSheet.showDialog(this, m_App, m_oTicket.getCustomer());
				if (action == CustomerSheet.REMOVE) {
					m_oTicket.setCustomer(null);
					openFinder = false;
				} else if (action != CustomerSheet.CHANGE) {
					return;
				} else {
					openFinder = true;
				}
			}
			if (openFinder) {
				JCustomerFinder finder = JCustomerFinder.getCustomerFinder(this, dlCustomers);
				finder.search(m_oTicket.getCustomer());
				finder.setVisible(true);
				if (finder.isRemoveRequested()) {
					m_oTicket.setCustomer(null);
				} else if (finder.getSelectedCustomer() != null) {
					m_oTicket.setCustomer(dlSales.loadCustomerExt(finder.getSelectedCustomer().getId()));
				}
			}
		} catch (BasicException e) {
			MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotfindcustomer"),
					e);
			msg.show(this);
		}

		refreshTicket();
		notifyTicketChanged();

	}// GEN-LAST:event_btnCustomerActionPerformed

	// Variables declaration - do not modify//GEN-BEGIN:variables
	private javax.swing.JButton btnCustomer;
	private javax.swing.JPanel catcontainer;
	private javax.swing.JPanel jPanel2;
	private javax.swing.JPanel jPanel4;
	private javax.swing.JPanel jPanel5;
	private javax.swing.JPanel m_jButtons;
	private javax.swing.JPanel m_jButtonsExt;
	private javax.swing.JPanel m_jContEntries;
	private javax.swing.JButton m_jDelete;
	private javax.swing.JButton m_jDiscountLine;
	private javax.swing.JButton m_jDiscountTotal;
	private javax.swing.JButton m_jLoyaltyRedemption;
	private javax.swing.JButton m_jEditLine;
	private javax.swing.JButton m_jEnter;
	private javax.swing.JTextField m_jKeyFactory;
	private javax.swing.JLabel m_jLblCode;
	private javax.swing.JLabel m_jLblTotalEuros1;
	private javax.swing.JLabel m_jLblTotalEuros2;
	private javax.swing.JLabel m_jLblTotalEuros3;
	private javax.swing.JButton m_jList;
	private com.openbravo.beans.JNumberKeys m_jNumberKeys;
	private javax.swing.JPanel m_jOptions;
	private javax.swing.JPanel m_jPanCode;
	private javax.swing.JPanel m_jPanContainer;
	private javax.swing.JPanel m_jPanEntries;
	private javax.swing.JPanel m_jPanReadout;
	private javax.swing.JPanel m_jPanLoyalty;
	private javax.swing.JPanel m_jPanScanStatus;
	private javax.swing.JPanel m_jPanTicket;
	private javax.swing.JPanel m_jPanTotals;
	private javax.swing.JPanel m_jPanelBag;
	private javax.swing.JPanel m_jPanelCentral;
	private javax.swing.JPanel m_jPanelScripts;
	private javax.swing.JLabel m_jPor;
	private javax.swing.JLabel m_jPrice;
	private javax.swing.JLabel m_jScanStatus;
	private javax.swing.JLabel m_jLoyalty;
	private javax.swing.JLabel m_jSubtotalEuros;
	private javax.swing.JLabel m_jTaxesEuros;
	private javax.swing.JLabel m_jTotalEuros;
	// End of variables declaration//GEN-END:variables

}
