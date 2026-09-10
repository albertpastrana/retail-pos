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
import java.util.Date;

import com.openbravo.beans.JNumberKeys;
import com.openbravo.data.gui.ComboBoxValModel;
import com.openbravo.data.gui.MessageInf;
import com.openbravo.format.Formats;
import com.openbravo.pos.printer.*;

import com.openbravo.pos.forms.JPanelView;
import com.openbravo.pos.forms.AppView;
import com.openbravo.pos.forms.AppLocal;
import com.openbravo.pos.panels.JProductFinder;
import com.openbravo.pos.scale.ScaleException;
import com.openbravo.pos.payment.JPaymentSelect;
import com.openbravo.basic.BasicException;
import com.openbravo.data.gui.ListKeyed;
import com.openbravo.data.loader.SentenceList;
import com.openbravo.pos.customers.CustomerInfoExt;
import com.openbravo.pos.customers.DataLogicCustomers;
import com.openbravo.pos.customers.JCustomerFinder;
import com.openbravo.pos.scripting.ScriptEngine;
import com.openbravo.pos.scripting.ScriptException;
import com.openbravo.pos.scripting.ScriptFactory;
import com.openbravo.pos.forms.DataLogicSystem;
import com.openbravo.pos.forms.DataLogicSales;
import com.openbravo.pos.forms.BeanFactoryApp;
import com.openbravo.pos.forms.BeanFactoryException;
import com.openbravo.pos.inventory.TaxCategoryInfo;
import com.openbravo.pos.inventory.PriceRule;
import com.openbravo.pos.inventory.PriceRuleService;
import com.openbravo.pos.inventory.TaxRegime;
import com.openbravo.pos.payment.JPaymentSelectReceipt;
import com.openbravo.pos.payment.JPaymentSelectRefund;
import com.openbravo.pos.ticket.CategoryInfo;
import com.openbravo.pos.ticket.ProductInfoExt;
import com.openbravo.pos.ticket.TaxInfo;
import com.openbravo.pos.ticket.TicketInfo;
import com.openbravo.pos.ticket.TicketLineInfo;
import com.openbravo.pos.util.JRPrinterAWT300;
import com.openbravo.pos.util.ReportUtils;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.ResourceBundle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.print.PrintService;
import javax.swing.event.AncestorEvent;
import javax.swing.event.AncestorListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;
import net.sf.jasperreports.engine.JasperCompileManager;
import net.sf.jasperreports.engine.JasperFillManager;
import net.sf.jasperreports.engine.JasperPrint;
import net.sf.jasperreports.engine.JasperReport;
import net.sf.jasperreports.engine.data.JRMapArrayDataSource;
import net.sf.jasperreports.engine.design.JasperDesign;
import net.sf.jasperreports.engine.xml.JRXmlLoader;

/**
 *
 * @author adrianromero
 */
public abstract class JPanelTicket extends JPanel implements JPanelView, BeanFactoryApp, TicketsEditor {

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
	private boolean m_bProductImportCancelled;

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

		m_App = app;
		dlSystem = (DataLogicSystem) m_App.getBean("com.openbravo.pos.forms.DataLogicSystem");
		dlSales = (DataLogicSales) m_App.getBean("com.openbravo.pos.forms.DataLogicSales");
		dlCustomers = (DataLogicCustomers) m_App.getBean("com.openbravo.pos.customers.DataLogicCustomers");
		priceRuleService = new PriceRuleService(m_App.getSession());

		m_ticketsbag = getJTicketsBag();
		m_jPanelBag.add(m_ticketsbag.getBagComponent(), BorderLayout.LINE_START);
		add(m_ticketsbag.getNullComponent(), "null");

		m_ticketlines = new JTicketLines(dlSystem.getResourceAsXML("Ticket.Line"));
		m_jPanelCentral.add(m_ticketlines, java.awt.BorderLayout.CENTER);

		m_TTP = new TicketParser(m_App.getDeviceTicket(), dlSystem);

		// Los botones configurables...
		m_jbtnconfig = new JPanelButtons("Ticket.Buttons", this);
		m_jButtonsExt.add(m_jbtnconfig);

		// El panel de los productos o de las lineas...
		catcontainer.add(getSouthComponent(), BorderLayout.CENTER);

		// El modelo de impuestos
		senttax = dlSales.getTaxList();
		senttaxcategories = dlSales.getTaxCategoriesList();

		taxcategoriesmodel = new ComboBoxValModel();

		// ponemos a cero el estado
		stateToZero();

		// inicializamos
		m_oTicket = null;
		m_oTicketExt = null;
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

		// The till sells at one tax category with the prices already including
		// taxes, both taken from Ticket.Buttons instead of on-screen controls.
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
			m_oTicket.setUserIfAbsent(m_App.getAppUserView().getUser().getUserInfo());
			m_oTicket.setActiveCash(m_App.getActiveCashIndex());
			m_oTicket.setDate(new Date()); // Set the edition date.
		}

		executeEvent(m_oTicket, m_oTicketExt, "ticket.show");

		refreshTicket();
	}

	public TicketInfo getActiveTicket() {
		return m_oTicket;
	}

	private void refreshTicket() {

		CardLayout cl = (CardLayout) (getLayout());

		if (m_oTicket == null) {
			m_jTicketId.setText(null);
			m_ticketlines.clearTicketLines();

			m_jSubtotalEuros.setText(null);
			m_jTaxesEuros.setText(null);
			m_jTotalEuros.setText(null);

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

			// The ticket name
			m_jTicketId.setText(m_oTicket.getName(m_oTicketExt));

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

		addTicketLine(new TicketLineInfo(oProduct, dMul, dPrice, tax,
				(java.util.Properties) (oProduct.getProperties().clone())));
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

		m_iNumberStatus = NUMBER_INPUTZERO;
		m_iNumberStatusInput = NUMBERZERO;
		m_iNumberStatusPor = NUMBERZERO;
	}

	private void incProductByCode(String sCode) {
		// precondicion: sCode != null

		try {
			ProductInfoExt oProduct = findOrImportProduct(sCode);
			if (oProduct == null) {
				if (!m_bProductImportCancelled) {
					Toolkit.getDefaultToolkit().beep();
					new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.noproduct", sCode))
							.show(this);
				}
				stateToZero();
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
				if (!m_bProductImportCancelled) {
					Toolkit.getDefaultToolkit().beep();
					new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.noproduct", sCode))
							.show(this);
				}
				stateToZero();
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
		m_bProductImportCancelled = false;
		ProductInfoExt product = dlSales.getProductInfoByCode(code);
		if (product == null) {
			String productsPath = m_App.getProperties().getProperty("catalog.import.products");
			String categoriesPath = m_App.getProperties().getProperty("catalog.import.categories");
			ProductInfoExt catalogProduct = dlSales.getCatalogProductByCode(code, productsPath, categoriesPath);
			List<ProductInfoExt> family = catalogProduct == null ? new ArrayList<ProductInfoExt>()
					: dlSales.getCatalogProductFamily(code, productsPath, categoriesPath);
			if (family.size() > 1) {
				List<ProductInfoExt> editedFamily = editProductFamilyForImport(code, family);
				if (editedFamily == null) {
					m_bProductImportCancelled = true;
					return null;
				}
				dlSales.importProducts(editedFamily, categoriesPath);
				return dlSales.getProductInfoByCode(code);
			}
			ProductInfoExt editedProduct = editProductForImport(code, catalogProduct);
			if (editedProduct == null) {
				m_bProductImportCancelled = true;
				return null;
			}
			product = dlSales.importProduct(editedProduct, editedProduct.getProperty("catalog.brand"), categoriesPath);
		}
		return product;
	}

	private List<ProductInfoExt> editProductFamilyForImport(String code, List<ProductInfoExt> family)
			throws BasicException {
		// One model shared by the checkbox each variant card draws under its price,
		// so the choice follows the cashier from card to card.
		final JToggleButton.ToggleButtonModel applyPriceModel = new JToggleButton.ToggleButtonModel();
		applyPriceModel.setSelected(true);
		final List<VariantImportEditor> editors = new ArrayList<VariantImportEditor>();
		int scannedRow = 0;
		for (int i = 0; i < family.size(); i++) {
			ProductInfoExt variant = family.get(i);
			boolean scanned = code.equals(variant.getCode()) || ("0" + code).equals(variant.getCode())
					|| ("00" + code).equals(variant.getCode());
			if (scanned) {
				scannedRow = i;
			}
			editors.add(new VariantImportEditor(variant, scanned, applyPriceModel));
		}

		final VariantTableModel model = new VariantTableModel(editors);
		final JTable table = new JTable(model);
		final boolean[] applyingFamilyPrice = new boolean[] { false };
		for (final VariantImportEditor source : editors) {
			source.priceChangeListener = new Runnable() {
				@Override
				public void run() {
					if (!applyPriceModel.isSelected() || applyingFamilyPrice[0] || source.prices.reportlock) {
						return;
					}
					Double margin = source.prices.readCommercialMargin();
					if (margin == null) {
						return;
					}
					applyingFamilyPrice[0] = true;
					try {
						for (VariantImportEditor target : editors) {
							if (target != source && target.selected) {
								target.prices.setCommercialMargin(margin.doubleValue());
							}
						}
					} finally {
						applyingFamilyPrice[0] = false;
					}
				}
			};
		}
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.setRowHeight(24);
		table.getColumnModel().getColumn(0).setMaxWidth(42);
		table.getColumnModel().getColumn(1).setPreferredWidth(190);
		table.getColumnModel().getColumn(2).setPreferredWidth(90);
		table.getColumnModel().getColumn(3).setPreferredWidth(90);

		final JPanel cards = new JPanel(new CardLayout());
		for (VariantImportEditor editor : editors) {
			cards.add(editor.panel, editor.product.getCode());
		}
		table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
			@Override
			public void valueChanged(ListSelectionEvent event) {
				int selected = table.getSelectedRow();
				if (!event.getValueIsAdjusting() && selected >= 0) {
					((CardLayout) cards.getLayout()).show(cards, editors.get(selected).product.getCode());
					focusPriceField(editors.get(selected).prices.sellTax);
				}
			}
		});
		table.setRowSelectionInterval(scannedRow, scannedRow);
		focusImportField(editors.get(scannedRow).prices.sellTax);

		JButton selectAll = new JButton(AppLocal.getIntString("button.variants.all"));
		selectAll.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				model.selectAll();
			}
		});
		JButton scannedOnly = new JButton(AppLocal.getIntString("button.variants.scanned"));
		scannedOnly.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				model.selectScannedOnly();
			}
		});
		JPanel listButtons = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
		listButtons.add(selectAll);
		listButtons.add(scannedOnly);

		JPanel variantsPanel = new JPanel(new BorderLayout(0, 6));
		variantsPanel.add(listButtons, BorderLayout.NORTH);
		JScrollPane scroll = new JScrollPane(table);
		scroll.setPreferredSize(new Dimension(420, 330));
		variantsPanel.add(scroll, BorderLayout.CENTER);

		JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, variantsPanel, cards);
		split.setResizeWeight(0.5);
		split.setBorder(null);

		JPanel content = new JPanel(new BorderLayout(0, 10));
		content.add(buildFamilyImportMessage(), BorderLayout.NORTH);
		content.add(split, BorderLayout.CENTER);

		String title = AppLocal.getIntString("title.importproductfamily");
		Object[] options = new Object[] { AppLocal.getIntString("button.addfamilytoreceipt"),
				AppLocal.getIntString("button.skipitem") };
		while (true) {
			int result = JOptionPane.showOptionDialog(this, content, title, JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
			if (result != 0) {
				return null;
			}
			List<ProductInfoExt> selected = new ArrayList<ProductInfoExt>();
			for (int i = 0; i < editors.size(); i++) {
				VariantImportEditor editor = editors.get(i);
				if (!editor.selected) {
					continue;
				}
				ProductInfoExt product = editor.buildProduct();
				if (product == null) {
					table.setRowSelectionInterval(i, i);
					JOptionPane.showMessageDialog(this, AppLocal.getIntString("message.importproductrequired"), title,
							JOptionPane.WARNING_MESSAGE);
					selected.clear();
					break;
				}
				selected.add(product);
			}
			if (!selected.isEmpty()) {
				return selected;
			}
		}
	}

	private JLabel buildFamilyImportMessage() {
		return new JLabel(AppLocal.getIntString("message.importproductfamily"));
	}

	private ProductInfoExt editProductForImport(String code, ProductInfoExt catalogProduct) throws BasicException {
		ProductInfoExt availableProduct = catalogProduct;
		if (availableProduct == null) {
			availableProduct = new ProductInfoExt();
			availableProduct.setCode(code);
			availableProduct.setReference(code);
			availableProduct.setName("");
		}
		final JTextField name = new JTextField(availableProduct.getName(), 32);
		name.setCaretPosition(0);
		String initialReference = availableProduct.getReference();
		if (initialReference == null || initialReference.trim().isEmpty()) {
			initialReference = availableProduct.getCode();
		}
		final JTextField reference = new JTextField(initialReference, 16);
		reference.setCaretPosition(0);
		final JComboBox<CategoryInfo> category = createImportCategoryCombo(availableProduct);
		final ImportPriceFields prices = new ImportPriceFields(availableProduct);

		JPanel fields = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.insets = new Insets(3, 4, 3, 4);
		constraints.anchor = GridBagConstraints.NORTHWEST;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		addImportField(fields, constraints, 0, AppLocal.getIntString("label.prodref"), reference);
		addImportField(fields, constraints, 1, AppLocal.getIntString("label.prodname"), name);
		addImportField(fields, constraints, 2, AppLocal.getIntString("label.prodcategory"), category);
		addImportField(fields, constraints, 3, AppLocal.getIntString("label.taxcategory"), prices.tax);
		addImportField(fields, constraints, 4, AppLocal.getIntString("label.prodpricebuy"), prices.buy);
		addImportField(fields, constraints, 5, prices.secondaryLabel(), prices.secondary);
		addImportField(fields, constraints, 6, AppLocal.getIntString("label.prodpriceselltax"), prices.priceBlock());
		constraints.gridx = 1;
		constraints.gridy = 7;
		fields.add(prices.offer, constraints);

		JPanel content = new JPanel(new BorderLayout(0, 12));
		content.add(buildImportMessage(code, catalogProduct != null), BorderLayout.NORTH);
		content.add(topAligned(fields), BorderLayout.CENTER);
		focusImportField(name.getText().trim().isEmpty() ? name : prices.sellTax);

		String title = AppLocal.getIntString("title.importproduct");
		Object[] options = new Object[] { AppLocal.getIntString("button.addtoreceipt"),
				AppLocal.getIntString("button.skipitem") };
		while (true) {
			int result = JOptionPane.showOptionDialog(this, content, title, JOptionPane.OK_CANCEL_OPTION,
					JOptionPane.PLAIN_MESSAGE, null, options, options[0]);
			if (result != 0) {
				return null;
			}

			ProductInfoExt edited = buildEditedProduct(availableProduct, catalogProduct, reference, name, category,
					prices);
			if (edited == null) {
				JOptionPane.showMessageDialog(this, AppLocal.getIntString("message.importproductrequired"), title,
						JOptionPane.WARNING_MESSAGE);
				continue;
			}
			return edited;
		}
	}

	private JLabel buildImportMessage(String code, boolean fromCatalog) {
		String message = AppLocal.getIntString(fromCatalog ? "message.importproduct" : "message.importproduct.unknown",
				code);
		return new JLabel("<html><body style='width: 320px'>" + message + "</body></html>");
	}

	// JOptionPane grabs focus for its default button, so the caret only lands on
	// the field once the dialog is on screen.
	private void focusImportField(final JComponent field) {
		field.addAncestorListener(new AncestorListener() {
			@Override
			public void ancestorAdded(AncestorEvent event) {
				focusPriceField(field);
			}

			@Override
			public void ancestorRemoved(AncestorEvent event) {
			}

			@Override
			public void ancestorMoved(AncestorEvent event) {
			}
		});
	}

	private void focusPriceField(final JComponent field) {
		SwingUtilities.invokeLater(new Runnable() {
			@Override
			public void run() {
				field.requestFocusInWindow();
				if (field instanceof JTextField) {
					((JTextField) field).selectAll();
				}
			}
		});
	}

	private ProductInfoExt buildEditedProduct(ProductInfoExt availableProduct, ProductInfoExt catalogProduct,
			JTextField reference, JTextField name, JComboBox<CategoryInfo> category, ImportPriceFields prices) {
		Double buy = readImportCurrency(prices.buy.getText(), true);
		Double sell = prices.pricesell;
		CategoryInfo selected = (CategoryInfo) category.getSelectedItem();
		TaxCategoryInfo tax = (TaxCategoryInfo) prices.tax.getSelectedItem();
		if (name.getText().trim().isEmpty() || selected == null || tax == null || buy == null || sell == null) {
			return null;
		}

		ProductInfoExt edited = new ProductInfoExt();
		edited.setID(catalogProduct == null ? java.util.UUID.randomUUID().toString() : catalogProduct.getID());
		edited.setCode(availableProduct.getCode());
		String editedReference = reference.getText().trim();
		edited.setReference(editedReference.isEmpty() ? edited.getCode() : editedReference);
		edited.setName(name.getText().trim());
		edited.setCategoryID(selected.getID());
		edited.setTaxCategoryID(tax.getID());
		edited.setPriceBuy(buy.doubleValue());
		edited.setPriceSell(sell.doubleValue());
		String brand = availableProduct.getProperty("catalog.brand", "").trim();
		if (!brand.isEmpty()) {
			edited.setProperty("catalog.brand", brand);
		}
		return edited;
	}

	private JComboBox<CategoryInfo> createImportCategoryCombo(ProductInfoExt catalogProduct) throws BasicException {
		JComboBox<CategoryInfo> category = new JComboBox<CategoryInfo>();
		category.addItem(null);
		CategoryInfo selectedCategory = null;
		java.util.List categories = dlSales.getCategoriesList().list();
		for (Object item : categories) {
			CategoryInfo availableCategory = (CategoryInfo) item;
			category.addItem(availableCategory);
			if (catalogProduct != null && availableCategory.getID().equals(catalogProduct.getCategoryID())) {
				selectedCategory = availableCategory;
			}
		}
		if (catalogProduct != null && catalogProduct.getCategoryID() != null
				&& !catalogProduct.getCategoryID().isEmpty() && selectedCategory == null) {
			selectedCategory = new CategoryInfo(catalogProduct.getCategoryID(),
					catalogProduct.getProperty("catalog.category.name", catalogProduct.getCategoryID()), null);
			category.addItem(selectedCategory);
		}
		category.setSelectedItem(selectedCategory);
		return category;
	}

	private void addImportField(JPanel panel, GridBagConstraints constraints, int row, String label, JComponent field) {
		constraints.gridx = 0;
		constraints.gridy = row;
		constraints.weightx = 0.0;
		panel.add(new JLabel(label + ":"), constraints);
		constraints.gridx = 1;
		constraints.weightx = 1.0;
		panel.add(field, constraints);
	}

	// GridBagLayout centres its rows in whatever height it gets, so the form needs
	// a north slot to stay pinned to the top of the dialog.
	private JPanel topAligned(JComponent fields) {
		JPanel holder = new JPanel(new BorderLayout());
		holder.add(fields, BorderLayout.NORTH);
		return holder;
	}

	private final class VariantImportEditor {
		private final ProductInfoExt product;
		private final boolean scanned;
		private boolean selected = true;
		private final JTextField name;
		private final JTextField reference;
		private final JComboBox<CategoryInfo> category;
		private final ImportPriceFields prices;
		private final JPanel panel;
		private Runnable changeListener;
		private Runnable priceChangeListener;

		private VariantImportEditor(ProductInfoExt product, boolean scanned,
				JToggleButton.ToggleButtonModel applyPriceModel) throws BasicException {
			this.product = product;
			this.scanned = scanned;
			name = new JTextField(product.getName(), 24);
			name.setCaretPosition(0);
			String initialReference = product.getReference();
			if (initialReference == null || initialReference.trim().isEmpty()) {
				initialReference = product.getCode();
			}
			reference = new JTextField(initialReference, 16);
			reference.setCaretPosition(0);
			category = createImportCategoryCombo(product);
			prices = new ImportPriceFields(product);
			panel = buildPanel(applyPriceModel);
			DocumentListener changed = new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent event) {
					changed();
				}

				@Override
				public void removeUpdate(DocumentEvent event) {
					changed();
				}

				@Override
				public void changedUpdate(DocumentEvent event) {
					changed();
				}
			};
			name.getDocument().addDocumentListener(changed);
			prices.buy.getDocument().addDocumentListener(changed);
			prices.sellTax.getDocument().addDocumentListener(changed);
			prices.sellTax.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent event) {
					priceChanged();
				}

				@Override
				public void removeUpdate(DocumentEvent event) {
					priceChanged();
				}

				@Override
				public void changedUpdate(DocumentEvent event) {
					priceChanged();
				}
			});
		}

		private JPanel buildPanel(JToggleButton.ToggleButtonModel applyPriceModel) {
			JPanel fields = new JPanel(new GridBagLayout());
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.insets = new Insets(3, 4, 3, 4);
			constraints.anchor = GridBagConstraints.NORTHWEST;
			constraints.fill = GridBagConstraints.HORIZONTAL;
			addImportField(fields, constraints, 0, AppLocal.getIntString("label.prodref"), reference);
			addImportField(fields, constraints, 1, AppLocal.getIntString("label.prodname"), name);
			addImportField(fields, constraints, 2, AppLocal.getIntString("label.prodcategory"), category);
			addImportField(fields, constraints, 3, AppLocal.getIntString("label.taxcategory"), prices.tax);
			addImportField(fields, constraints, 4, AppLocal.getIntString("label.prodpricebuy"), prices.buy);
			addImportField(fields, constraints, 5, prices.secondaryLabel(), prices.secondary);
			addImportField(fields, constraints, 6, AppLocal.getIntString("label.prodpriceselltax"),
					prices.priceBlock());

			JCheckBox applyPrice = new JCheckBox(AppLocal.getIntString("label.variants.applyprice"));
			applyPrice.setModel(applyPriceModel);
			applyPrice.setToolTipText(AppLocal.getIntString("label.variants.applyprice.hint"));
			constraints.gridx = 1;
			constraints.gridy = 7;
			fields.add(applyPrice, constraints);
			constraints.gridy = 8;
			fields.add(prices.offer, constraints);

			String heading = variantLabel(product) + (scanned ? " · " + AppLocal.getIntString("label.scanned") : "");
			JLabel headingLabel = new JLabel("<html><b>" + heading + "</b></html>");
			JLabel codeLabel = new JLabel(product.getCode());
			codeLabel.setEnabled(false);
			JPanel header = new JPanel(new BorderLayout());
			header.add(headingLabel, BorderLayout.NORTH);
			header.add(codeLabel, BorderLayout.CENTER);

			JPanel result = new JPanel(new BorderLayout(0, 8));
			result.add(header, BorderLayout.NORTH);
			result.add(topAligned(fields), BorderLayout.CENTER);
			return result;
		}

		private void changed() {
			if (changeListener != null) {
				changeListener.run();
			}
		}

		private void priceChanged() {
			if (priceChangeListener != null) {
				priceChangeListener.run();
			}
		}

		private ProductInfoExt buildProduct() {
			return buildEditedProduct(product, product, reference, name, category, prices);
		}
	}

	private final class VariantTableModel extends AbstractTableModel {
		private final List<VariantImportEditor> editors;
		private final String[] columns = { "", AppLocal.getIntString("label.variant"),
				AppLocal.getIntString("label.variants.cost"), AppLocal.getIntString("label.variants.price") };

		private VariantTableModel(List<VariantImportEditor> editors) {
			this.editors = editors;
			for (int i = 0; i < editors.size(); i++) {
				final int row = i;
				editors.get(i).changeListener = new Runnable() {
					@Override
					public void run() {
						fireTableRowsUpdated(row, row);
					}
				};
			}
		}

		@Override
		public int getRowCount() {
			return editors.size();
		}

		@Override
		public int getColumnCount() {
			return columns.length;
		}

		@Override
		public String getColumnName(int column) {
			return columns[column];
		}

		@Override
		public Class<?> getColumnClass(int column) {
			return column == 0 ? Boolean.class : String.class;
		}

		@Override
		public boolean isCellEditable(int row, int column) {
			return column == 0 && !editors.get(row).scanned;
		}

		@Override
		public Object getValueAt(int row, int column) {
			VariantImportEditor editor = editors.get(row);
			switch (column) {
			case 0:
				return Boolean.valueOf(editor.selected);
			case 1:
				return variantLabel(editor.product);
			case 2:
				return editor.prices.buy.getText();
			case 3:
				return editor.prices.sellTax.getText();
			default:
				return "";
			}
		}

		@Override
		public void setValueAt(Object value, int row, int column) {
			if (column == 0 && !editors.get(row).scanned) {
				editors.get(row).selected = Boolean.TRUE.equals(value);
				fireTableRowsUpdated(row, row);
			}
		}

		private void selectAll() {
			for (VariantImportEditor editor : editors) {
				editor.selected = true;
			}
			fireTableDataChanged();
		}

		private void selectScannedOnly() {
			for (VariantImportEditor editor : editors) {
				editor.selected = editor.scanned;
			}
			fireTableDataChanged();
		}
	}

	private String variantLabel(ProductInfoExt product) {
		String name = product.getName();
		int separator = name == null ? -1 : name.indexOf(" — ");
		if (separator >= 0) {
			int reference = name.lastIndexOf(" [");
			return name.substring(separator + 3, reference > separator ? reference : name.length());
		}
		return product.getReference();
	}

	// The cashier only ever sees the retail price: the net price and the markup on
	// cost stay internal, and the margin beside the price is the one on retail.
	private final class ImportPriceFields {
		private final JTextField buy = new JTextField(12);
		private final JTextField secondary = new JTextField(12);
		private final JTextField sellTax = new JTextField(12);
		private final JLabel margin = new JLabel();
		private final JComboBox<TaxCategoryInfo> tax = new JComboBox<TaxCategoryInfo>();
		private final JLabel offer = new JLabel();
		private boolean reportlock;
		private boolean sellOverridden;
		private Double pricesell;
		private final PriceRule priceRule;

		private ImportPriceFields(ProductInfoExt product) throws BasicException {
			buy.setHorizontalAlignment(JTextField.RIGHT);
			secondary.setHorizontalAlignment(JTextField.RIGHT);
			secondary.setEditable(false);
			secondary.setFocusable(false);
			sellTax.setHorizontalAlignment(JTextField.RIGHT);
			margin.setHorizontalAlignment(SwingConstants.RIGHT);
			margin.setEnabled(false);
			String brand = product.getProperty("catalog.brand", "").trim();
			try {
				priceRule = priceRuleService.findForBrand(brand);
			} catch (java.sql.SQLException e) {
				throw new BasicException(AppLocal.getIntString("message.pricerules.loaderror"), e);
			}

			java.util.List categories = senttaxcategories.list();
			TaxCategoryInfo selected = null;
			String preferred = product.getTaxCategoryID();
			if (preferred == null || preferred.isEmpty()) {
				preferred = (String) taxcategoriesmodel.getSelectedKey();
			}
			for (Object item : categories) {
				TaxCategoryInfo available = (TaxCategoryInfo) item;
				tax.addItem(available);
				if (preferred != null && preferred.equals(available.getID())) {
					selected = available;
				}
			}
			tax.setSelectedItem(selected != null ? selected : (categories.isEmpty() ? null : categories.get(0)));

			boolean priceAvailable = Boolean.parseBoolean(product.getProperty("catalog.price.available", "false"));
			if (priceAvailable) {
				buy.setText(Formats.CURRENCY.formatValue(Double.valueOf(product.getPriceBuy())));
			}

			buy.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent e) {
					onBuyOrTaxChanged();
				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					onBuyOrTaxChanged();
				}

				@Override
				public void changedUpdate(DocumentEvent e) {
					onBuyOrTaxChanged();
				}
			});
			sellTax.getDocument().addDocumentListener(new DocumentListener() {
				@Override
				public void insertUpdate(DocumentEvent e) {
					onSellTaxEdited();
				}

				@Override
				public void removeUpdate(DocumentEvent e) {
					onSellTaxEdited();
				}

				@Override
				public void changedUpdate(DocumentEvent e) {
					onSellTaxEdited();
				}
			});
			tax.addActionListener(new ActionListener() {
				@Override
				public void actionPerformed(ActionEvent e) {
					onBuyOrTaxChanged();
				}
			});

			offerFromCost();
			calculatePriceSellTax();
			calculateSecondary();
			calculateMargin();
		}

		private void onBuyOrTaxChanged() {
			if (!sellOverridden) {
				offerFromCost();
			}
			calculatePriceSellTax();
			calculateSecondary();
			calculateMargin();
		}

		private void onSellTaxEdited() {
			if (!reportlock) {
				sellOverridden = true;
				reportlock = true;
				Double dPriceSellTax = readImportCurrency(sellTax.getText(), false);
				if (dPriceSellTax == null) {
					setPriceSell(null);
				} else {
					setPriceSell(Double.valueOf(dPriceSellTax.doubleValue() / (1.0 + taxRate())));
				}
				reportlock = false;
			}
			calculateSecondary();
			calculateMargin();
		}

		private void calculatePriceSellTax() {
			if (!reportlock) {
				reportlock = true;
				if (pricesell == null) {
					sellTax.setText(null);
				} else {
					sellTax.setText(
							Formats.CURRENCY.formatValue(Double.valueOf(pricesell.doubleValue() * (1.0 + taxRate()))));
				}
				reportlock = false;
			}
		}

		private void calculateMargin() {
			Double commercialMargin = readCommercialMargin();
			margin.setText(commercialMargin == null ? ""
					: AppLocal.getIntString("message.import.margin", Formats.PERCENT.formatValue(commercialMargin)));
		}

		private Double readCommercialMargin() {
			Double factoryPrice = readImportCurrency(buy.getText(), false);
			Double gross = readImportCurrency(sellTax.getText(), false);
			if (factoryPrice == null || gross == null || factoryPrice.doubleValue() <= 0.0
					|| gross.doubleValue() <= 0.0) {
				return null;
			}
			double cost = PriceRuleService.calculateGrossCostBasis(factoryPrice.doubleValue(), taxRate(),
					priceTaxRegime);
			return Double.valueOf((gross.doubleValue() - cost) / gross.doubleValue());
		}

		private void setCommercialMargin(double commercialMargin) {
			Double factoryPrice = readImportCurrency(buy.getText(), false);
			if (factoryPrice == null || factoryPrice.doubleValue() <= 0.0 || commercialMargin >= 1.0) {
				return;
			}
			double cost = PriceRuleService.calculateGrossCostBasis(factoryPrice.doubleValue(), taxRate(),
					priceTaxRegime);
			sellTax.setText(Formats.CURRENCY.formatValue(Double.valueOf(cost / (1.0 - commercialMargin))));
		}

		private void offerFromCost() {
			Double cost = readImportCurrency(buy.getText(), false);
			if (cost == null || cost.doubleValue() <= 0.0 || tax.getSelectedItem() == null) {
				setPriceSell(null);
				offer.setText(AppLocal.getIntString("message.pricerule.entercost"));
				return;
			}
			double gross = PriceRuleService.calculateGross(cost.doubleValue(), taxRate(), priceRule, priceTaxRegime);
			setPriceSell(Double.valueOf(gross / (1.0 + taxRate())));
			offer.setText(AppLocal.getIntString("message.pricerule.offer",
					Formats.CURRENCY.formatValue(Double.valueOf(gross)),
					Formats.PERCENT.formatValue(Double.valueOf(priceRule.getMarkupPercent() / 100.0))));
		}

		private void setPriceSell(Double value) {
			pricesell = value;
		}

		private JPanel priceBlock() {
			JPanel block = new JPanel(new BorderLayout());
			block.add(sellTax, BorderLayout.NORTH);
			block.add(margin, BorderLayout.CENTER);
			return block;
		}

		private String secondaryLabel() {
			return AppLocal.getIntString(priceTaxRegime == TaxRegime.EQUIVALENCE_SURCHARGE ? "label.prodpriceeconomic"
					: "label.prodpricesell");
		}

		private void calculateSecondary() {
			Double factoryPrice = readImportCurrency(buy.getText(), false);
			if (priceTaxRegime == TaxRegime.NORMAL) {
				secondary.setText(Formats.CURRENCY.formatValue(pricesell));
			} else if (factoryPrice == null || tax.getSelectedItem() == null) {
				secondary.setText(null);
			} else {
				double economicCost = PriceRuleService.calculateEconomicCost(factoryPrice.doubleValue(), taxRate(),
						priceTaxRegime);
				secondary.setText(Formats.CURRENCY.formatValue(Double.valueOf(economicCost)));
			}
		}

		private double taxRate() {
			return taxeslogic.getTaxRate((TaxCategoryInfo) tax.getSelectedItem(), m_oTicket.getDate(),
					m_oTicket.getCustomer());
		}
	}

	private Double readImportCurrency(String value, boolean emptyIsZero) {
		if (value.trim().isEmpty()) {
			return emptyIsZero ? Double.valueOf(0.0) : null;
		}
		try {
			Double parsed = (Double) Formats.CURRENCY.parseValue(value);
			return parsed.doubleValue() < 0.0 ? null : parsed;
		} catch (BasicException e) {
			return null;
		}
	}

	private void incProduct(ProductInfoExt prod) {

		if (prod.isScale() && m_App.getDeviceScale().existsScale()) {
			try {
				Double value = m_App.getDeviceScale().readWeight();
				if (value != null) {
					incProduct(value.doubleValue(), prod);
				}
			} catch (ScaleException e) {
				Toolkit.getDefaultToolkit().beep();
				new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.noweight"), e).show(this);
				stateToZero();
			}
		} else {
			// No es un producto que se pese o no hay balanza
			incProduct(1.0, prod);
		}
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
		return Formats.PERCENT.formatValue(new Double(percentage / 100.0));
	}

	private void applyLineDiscount() {
		if (!m_App.getAppUserView().getUser().hasPermission("button.discount")) {
			Toolkit.getDefaultToolkit().beep();
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
		String basePriceValue = line.getProperty("discount.line.baseprice");
		String baseName = line.getProperty("discount.line.basename");
		double basePrice = basePriceValue == null ? line.getPrice() : Double.parseDouble(basePriceValue);

		if (baseName == null) {
			baseName = line.getProductName();
			line.setProperty("discount.line.basename", baseName);
			line.setProperty("discount.line.baseprice", Double.toString(basePrice));
		}

		line.setPrice(basePrice * (1.0 - percentage.doubleValue() / 100.0));
		line.setProperty("discount.line.percent", Double.toString(percentage.doubleValue()));
		line.setProperty("product.name", baseName + " (-" + formatDiscountPercentage(percentage.doubleValue()) + ")");
		paintTicketLine(index, line);
	}

	private void applyTotalDiscount() {
		if (!m_App.getAppUserView().getUser().hasPermission("button.discount.total")) {
			Toolkit.getDefaultToolkit().beep();
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

	private void closeCurrentTicket() {
		if (m_oTicket.getLinesCount() > 0) {
			if (closeTicket(m_oTicket, m_oTicketExt)) {
				m_ticketsbag.deleteTicket();
			} else {
				refreshTicket();
			}
		} else {
			Toolkit.getDefaultToolkit().beep();
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
				if (sCode.startsWith("c")) {
					// barcode of a customers card
					try {
						CustomerInfoExt newcustomer = dlSales.findCustomerExt(sCode);
						if (newcustomer == null) {
							Toolkit.getDefaultToolkit().beep();
							new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.nocustomer"))
									.show(this);
						} else {
							m_oTicket.setCustomer(newcustomer);
							m_jTicketId.setText(m_oTicket.getName(m_oTicketExt));
						}
					} catch (BasicException e) {
						Toolkit.getDefaultToolkit().beep();
						new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.nocustomer"), e)
								.show(this);
					}
					stateToZero();
				} else if (sCode.length() == 13 && sCode.startsWith("250")) {
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

			} else if (cTrans == '\u00a7' && m_iNumberStatusInput == NUMBERVALID && m_iNumberStatusPor == NUMBERZERO) {
				// Scale button pressed and a number typed as a price
				if (m_App.getDeviceScale().existsScale()
						&& m_App.getAppUserView().getUser().hasPermission("sales.EditLines")) {
					try {
						Double value = m_App.getDeviceScale().readWeight();
						if (value != null) {
							ProductInfoExt product = getInputProduct();
							addTicketLine(product, value.doubleValue(), product.getPriceSell());
						}
					} catch (ScaleException e) {
						Toolkit.getDefaultToolkit().beep();
						new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.noweight"), e).show(this);
						stateToZero();
					}
				} else {
					// No existe la balanza;
					Toolkit.getDefaultToolkit().beep();
				}
			} else if (cTrans == '\u00a7' && m_iNumberStatusInput == NUMBERZERO && m_iNumberStatusPor == NUMBERZERO) {
				// Scale button pressed and no number typed.
				int i = m_ticketlines.getSelectedIndex();
				if (i < 0) {
					Toolkit.getDefaultToolkit().beep();
				} else if (m_App.getDeviceScale().existsScale()) {
					try {
						Double value = m_App.getDeviceScale().readWeight();
						if (value != null) {
							TicketLineInfo newline = new TicketLineInfo(m_oTicket.getLine(i));
							newline.setMultiply(value.doubleValue());
							newline.setPrice(Math.abs(newline.getPrice()));
							paintTicketLine(i, newline);
						}
					} catch (ScaleException e) {
						// Error de pesada.
						Toolkit.getDefaultToolkit().beep();
						new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.noweight"), e).show(this);
						stateToZero();
					}
				} else {
					// No existe la balanza;
					Toolkit.getDefaultToolkit().beep();
				}

				// Add one product more to the selected line
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

					if (paymentdialog.showDialog(ticket.getTotal(), ticket.getCustomer())) {

						// assign the payments selected and calculate taxes.
						ticket.setPayments(paymentdialog.getSelectedPayments());

						// Asigno los valores definitivos del ticket...
						ticket.setUserIfAbsent(m_App.getAppUserView().getUser().getUserInfo());
						ticket.setActiveCash(m_App.getActiveCashIndex());
						ticket.setDate(new Date()); // Le pongo la fecha de cobro

						if (executeEvent(ticket, ticketext, "ticket.save") == null) {
							// Save the receipt and assign a receipt number
							boolean saved = true;
							try {
								dlSales.saveTicket(ticket, m_App.getInventoryLocation());
							} catch (BasicException eData) {
								MessageInf msg = new MessageInf(MessageInf.SGN_NOTICE,
										AppLocal.getIntString("message.nosaveticket"), eData);
								msg.show(this);
								saved = false;
							}

							if (saved) {
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
				MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
						AppLocal.getIntString("message.cannotcalculatetaxes"));
				msg.show(this);
				resultok = false;
			}

			// reset the payment info
			m_oTicket.resetTaxes();
			m_oTicket.resetPayments();
		}

		// cancelled the ticket.total script
		// or canceled the payment dialog
		// or canceled the ticket.close script
		return resultok;
	}

	private void printTicket(String sresourcename, TicketInfo ticket, Object ticketext) {

		String sresource = dlSystem.getResourceAsXML(sresourcename);
		if (sresource == null) {
			MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotprintticket"));
			msg.show(JPanelTicket.this);
		} else {
			try {
				ScriptEngine script = ScriptFactory.getScriptEngine(ScriptFactory.VELOCITY);
				script.put("taxes", taxcollection);
				script.put("taxeslogic", taxeslogic);
				script.put("ticket", ticket);
				script.put("place", ticketext);
				m_TTP.printTicket(script.eval(sresource).toString());
			} catch (ScriptException e) {
				MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
						AppLocal.getIntString("message.cannotprintticket"), e);
				msg.show(JPanelTicket.this);
			} catch (TicketPrinterException e) {
				MessageInf msg = new MessageInf(MessageInf.SGN_WARNING,
						AppLocal.getIntString("message.cannotprintticket"), e);
				msg.show(JPanelTicket.this);
			}
		}
	}

	private void printReport(String resourcefile, TicketInfo ticket, Object ticketext) {

		try {

			JasperReport jr;

			InputStream in = getClass().getResourceAsStream(resourcefile + ".ser");
			if (in == null) {
				// read and compile the report
				JasperDesign jd = JRXmlLoader.load(getClass().getResourceAsStream(resourcefile + ".jrxml"));
				jr = JasperCompileManager.compileReport(jd);
			} else {
				// read the compiled reporte
				ObjectInputStream oin = new ObjectInputStream(in);
				jr = (JasperReport) oin.readObject();
				oin.close();
			}

			// Construyo el mapa de los parametros.
			Map reportparams = new HashMap();
			// reportparams.put("ARG", params);
			try {
				reportparams.put("REPORT_RESOURCE_BUNDLE", ResourceBundle.getBundle(resourcefile + ".properties"));
			} catch (MissingResourceException e) {
			}
			reportparams.put("TAXESLOGIC", taxeslogic);

			Map reportfields = new HashMap();
			reportfields.put("TICKET", ticket);
			reportfields.put("PLACE", ticketext);

			JasperPrint jp = JasperFillManager.fillReport(jr, reportparams,
					new JRMapArrayDataSource(new Object[] { reportfields }));

			PrintService service = ReportUtils
					.getPrintService(m_App.getProperties().getProperty("machine.printername"));

			JRPrinterAWT300.printPages(jp, 0, jp.getPages().size() - 1, service);

		} catch (Exception e) {
			MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotloadreport"),
					e);
			msg.show(this);
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

	private Object executeEventAndRefresh(String eventkey, ScriptArg... args) {

		String resource = m_jbtnconfig.getEvent(eventkey);
		if (resource == null) {
			return null;
		} else {
			ScriptObject scr = new ScriptObject(m_oTicket, m_oTicketExt);
			scr.setSelectedIndex(m_ticketlines.getSelectedIndex());
			Object result = evalScript(scr, resource, args);
			refreshTicket();
			setSelectedIndex(scr.getSelectedIndex());
			return result;
		}
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

		public void printReport(String resourcefile) {
			JPanelTicket.this.printReport(resourcefile, ticket, ticketext);
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
		m_jTicketId = new javax.swing.JLabel();
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
		m_jEnter = new javax.swing.JButton();
		m_jKeyFactory = new javax.swing.JTextField();
		catcontainer = new javax.swing.JPanel();

		setBackground(new java.awt.Color(255, 204, 153));
		setLayout(new java.awt.CardLayout());

		m_jPanContainer.setLayout(new java.awt.BorderLayout());

		m_jOptions.setLayout(new java.awt.BorderLayout());

		m_jTicketId.setBackground(java.awt.Color.white);
		m_jTicketId.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
		m_jTicketId.setBorder(javax.swing.BorderFactory.createCompoundBorder(
				javax.swing.BorderFactory
						.createLineBorder(javax.swing.UIManager.getDefaults().getColor("Button.darkShadow")),
				javax.swing.BorderFactory.createEmptyBorder(1, 4, 1, 4)));
		m_jTicketId.setOpaque(true);
		m_jTicketId.setPreferredSize(new java.awt.Dimension(160, 25));
		m_jTicketId.setRequestFocusEnabled(false);
		m_jButtons.add(m_jTicketId);

		btnCustomer.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/kuser.png"))); // NOI18N
		btnCustomer.setToolTipText(AppLocal.getIntString("tooltiptext.customer")); // NOI18N
		btnCustomer.setFocusPainted(false);
		btnCustomer.setFocusable(false);
		btnCustomer.setMargin(new java.awt.Insets(8, 14, 8, 14));
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

		m_jDelete.setIcon(
				new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/locationbar_erase.png"))); // NOI18N
		m_jDelete.setFocusPainted(false);
		m_jDelete.setFocusable(false);
		m_jDelete.setMargin(new java.awt.Insets(8, 14, 8, 14));
		m_jDelete.setRequestFocusEnabled(false);
		m_jDelete.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jDeleteActionPerformed(evt);
			}
		});
		jPanel2.add(m_jDelete);

		m_jList.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/search22.png"))); // NOI18N
		m_jList.setFocusPainted(false);
		m_jList.setFocusable(false);
		m_jList.setMargin(new java.awt.Insets(8, 14, 8, 14));
		m_jList.setRequestFocusEnabled(false);
		m_jList.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jListActionPerformed(evt);
			}
		});
		jPanel2.add(m_jList);

		m_jDiscountLine.setIcon(
				new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/discount_line22.png"))); // NOI18N
		m_jDiscountLine.setToolTipText(AppLocal.getIntString("button.discountline")); // NOI18N
		m_jDiscountLine.setFocusPainted(false);
		m_jDiscountLine.setFocusable(false);
		m_jDiscountLine.setMargin(new java.awt.Insets(8, 14, 8, 14));
		m_jDiscountLine.setRequestFocusEnabled(false);
		m_jDiscountLine.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jDiscountLineActionPerformed(evt);
			}
		});
		jPanel2.add(m_jDiscountLine);

		m_jDiscountTotal.setIcon(
				new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/discount_total22.png"))); // NOI18N
		m_jDiscountTotal.setToolTipText(AppLocal.getIntString("button.discounttotal")); // NOI18N
		m_jDiscountTotal.setFocusPainted(false);
		m_jDiscountTotal.setFocusable(false);
		m_jDiscountTotal.setMargin(new java.awt.Insets(8, 14, 8, 14));
		m_jDiscountTotal.setRequestFocusEnabled(false);
		m_jDiscountTotal.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				m_jDiscountTotalActionPerformed(evt);
			}
		});
		jPanel2.add(m_jDiscountTotal);

		m_jEditLine.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/color_line.png"))); // NOI18N
		m_jEditLine.setFocusPainted(false);
		m_jEditLine.setFocusable(false);
		m_jEditLine.setMargin(new java.awt.Insets(8, 14, 8, 14));
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

		m_jTotalEuros.setBackground(java.awt.Color.white);
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

		m_jSubtotalEuros.setBackground(java.awt.Color.white);
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

		m_jTaxesEuros.setBackground(java.awt.Color.white);
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

		m_jEnter.setIcon(new javax.swing.ImageIcon(getClass().getResource("/com/openbravo/images/barcode.png"))); // NOI18N
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

		ProductInfoExt prod = JProductFinder.showMessage(JPanelTicket.this, dlSales);
		if (prod != null) {
			buttonTransition(prod);
		}

	}// GEN-LAST:event_m_jListActionPerformed

	private void btnCustomerActionPerformed(java.awt.event.ActionEvent evt) {// GEN-FIRST:event_btnCustomerActionPerformed

		JCustomerFinder finder = JCustomerFinder.getCustomerFinder(this, dlCustomers);
		finder.search(m_oTicket.getCustomer());
		finder.setVisible(true);

		try {
			m_oTicket.setCustomer(finder.getSelectedCustomer() == null ? null
					: dlSales.loadCustomerExt(finder.getSelectedCustomer().getId()));
		} catch (BasicException e) {
			MessageInf msg = new MessageInf(MessageInf.SGN_WARNING, AppLocal.getIntString("message.cannotfindcustomer"),
					e);
			msg.show(this);
		}

		refreshTicket();

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
	private javax.swing.JPanel m_jPanTicket;
	private javax.swing.JPanel m_jPanTotals;
	private javax.swing.JPanel m_jPanelBag;
	private javax.swing.JPanel m_jPanelCentral;
	private javax.swing.JPanel m_jPanelScripts;
	private javax.swing.JLabel m_jPor;
	private javax.swing.JLabel m_jPrice;
	private javax.swing.JLabel m_jSubtotalEuros;
	private javax.swing.JLabel m_jTaxesEuros;
	private javax.swing.JLabel m_jTicketId;
	private javax.swing.JLabel m_jTotalEuros;
	// End of variables declaration//GEN-END:variables

}
